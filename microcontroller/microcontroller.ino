#include <map>
#include <stdlib.h>
#include <string>
#include "BluetoothSerial.h"
#include "driver/uart.h"
#include "driver/gpio.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/queue.h"

// We're communicating with the RFID reader over UART. I've arbitrarily picked the second set of UART pins on my ESP32.
const int RXD_PIN = 16;
const int TXD_PIN = 17;
#define UART_NUM UART_NUM_2
static const int RX_BUF_SIZE = 4096;

// TODO this is not implemented yet. Sometimes the RFID reader gets into a wonky state and never returns meaningful information again.
// The reset pin can be used to return the reader into a good state without having to cycle power.
const int reset = 12;

// These will control the multiplexers wired to the rf antennas. The output of each will bbe wired together.
const int c1 = 21;
const int c2 = 19;
const int c3 = 18;


// The values to set the switch the multiplexer to each leg. 0,0,0 is y0, 0,0,1 is y1 etc. These are the values for the control pins above.
const int muxChannels[8][3] = {
  { 0, 0, 0 }, { 0, 0, 1 }, { 0, 1, 0 }, { 0, 1, 1 }, { 1, 0, 0 }, { 1, 0, 1 }, { 1, 1, 0 }, { 1, 1, 1 }
};

// The pins on the microcontroller that disable each switch. Even if the antennas aren't connected, the unused switches need to be disabled.
// I'm not sure what the cause is. Perhaps the screw terminals have too much capacitance. This ordering isn't particularly special, it's just 
// the order that the ESP32 dev board I have puts them in.
// INH pops up a lot in circuit diagrams and stands for "inhibit high" as in: supply the pin power and the switch shuts off.
const int muxInh[8] = {
  13, 12, 14, 27, 26, 25, 33, 32
};

QueueHandle_t uart_queue;

// The standard way to refer to pieces is one letter that is lowercase for black and uppercase for white.
// King - K, Queen - Q, Rook - R, Knight - N, Bishop - b, Pawn - p
// Unfortunately the reader seems to sometimes include extra characters or hallucinate additional tags, so I'll have to iterate through each key
// looking for something close.
// TODO - Need tags for additional queens, rooks, bishops, and pawns (both for promotion and the starting set of pieces)
const std::map<std::string, std::string> tagIdToPiece = {
  {"[04FA0D7B5A6F6180D4", "k"},
  {"[04711CE15A6F6180D4", "q"},
  {"[045E1BC95D6F6180D3", "r"},
  {"[047AAD596F6180", "b"},
  {"[04DD6A3B5D6F6180D3", "n"},
  {"[04F389F65A6F6180D4", "p"},

  {"[0440905C5A6F6180D4", "K"},
  {"[04EBE5825C6F6180D2", "Q"},
  {"[043E5DEF5D6F6180D3", "R"},
  {"[04BA0A606F6180", "B"},
  {"[04211AB75A6F6180D4", "N"},
  {"[047113606F6180", "P"}
};

// Wrangling 128 wires to the correct location on the circuit board seems a lot more difficult than just mapping it in software. 
// The first digit will be row, second will be column. To correct the mapping I'll set unique pieces on the first row and swap values with
// the correct square. Then move on to the second row and repeat.
// If I make a second revision of the circuit board, I'll silk screen the multiplexer values next to the screw terminals instead of this.
std::map<int, int> antennaLocationToSquare = {
  {00, 00}, {01, 01}, {02, 02}, {03, 03}, {04, 04}, {05, 05}, {06, 06}, {07, 07},
  {10, 10}, {11, 11}, {12, 12}, {13, 13}, {14, 14}, {15, 15}, {16, 16}, {17, 17},
  {20, 20}, {21, 21}, {22, 22}, {23, 23}, {24, 24}, {25, 25}, {26, 26}, {27, 27},
  {30, 30}, {31, 31}, {32, 32}, {33, 33}, {34, 34}, {35, 35}, {36, 36}, {37, 37},
  {40, 40}, {41, 41}, {42, 42}, {43, 43}, {44, 44}, {45, 45}, {46, 46}, {47, 47},
  {50, 50}, {51, 51}, {52, 52}, {53, 53}, {54, 54}, {55, 55}, {56, 56}, {57, 57},
  {60, 60}, {61, 61}, {62, 62}, {63, 63}, {64, 64}, {65, 65}, {66, 66}, {67, 67},
  {70, 70}, {71, 71}, {72, 72}, {73, 73}, {74, 74}, {75, 75}, {76, 76}, {77, 77},
};

BluetoothSerial SerialBT;

// TODO on interrupt reset queue or reader
// This is modified from Espressif's uart event example https://github.com/espressif/esp-idf/blob/v5.5.1/examples/peripherals/uart/uart_events/main/uart_events_example_main.c
// I've only seen these errors pop up when there's something wrong with my circuit and the reader gets into a bad state, but I'm leaving it in
// for visibility in case it happens again in the future.
static void uart_event_task(void* pvParameters) {
  uart_event_t event;
  size_t buffered_size;
  uint8_t* dtmp = (uint8_t*)malloc(1024);
  for (;;) {
    if (xQueueReceive(uart_queue, (void*)&event, (TickType_t)portMAX_DELAY)) {
      bzero(dtmp, 1024);
      switch (event.type) {
        case UART_DATA:
          break;
        case UART_FIFO_OVF:
          //Serial.println("hw fifo overflow");
          uart_flush_input(UART_NUM);
          xQueueReset(uart_queue);
          break;
        case UART_BUFFER_FULL:
          //Serial.println("ring buffer full");
          uart_flush_input(UART_NUM);
          xQueueReset(uart_queue);
          break;
        case UART_BREAK:
          //Serial.println("uart rx break");
          break;
        case UART_PARITY_ERR:
          //Serial.println("uart parity error");
          break;
        case UART_FRAME_ERR:
          //Serial.println("uart frame error");
          break;
        case UART_PATTERN_DET:
          uart_get_buffered_data_len(UART_NUM, &buffered_size);
          //Serial.println("[UART PATTERN DETECTED] pos: %d, buffered size: %d");
          break;
        default:
          //Serial.println("Event type");
          //Serial.println(event.type);
          break;
      }
    }
  }
  free(dtmp);
  dtmp = NULL;
  vTaskDelete(NULL);
}


/**
* Sends a command over UART to the RFID reader. Output will be stored in the data buffer.
*/
void sendUartCommand(uint8_t* data, char* command) {
  // The commands for the RFID reader are written in hex as specified by the docs. But it doesn't want the values in hex,
  // it wants the ASCII representation of the hex. So I'll be printing the commands to a string and sending that.
  int len = sprintf((char*)data, command);
  uart_write_bytes(UART_NUM, data, len);
  delay(1);

  int rxBytes = uart_read_bytes(UART_NUM, data, RX_BUF_SIZE, 200 / portTICK_PERIOD_MS);
  if (rxBytes > 0) {
    data[rxBytes] = 0;
    Serial.print((char*)data);

  } else {
    //Serial.println("No data");
  }
  delay(1);
}

void setup() {
  Serial.begin(115200);
  Serial.println("Booting");

  SerialBT.begin("Chess");  // Bluetooth device name

  // Set the switches to y0
  pinMode(c1, OUTPUT);
  pinMode(c2, OUTPUT);
  pinMode(c3, OUTPUT);
  digitalWrite(c1, LOW);
  digitalWrite(c2, LOW);
  digitalWrite(c3, LOW);

  // Turn all of our switches off
  for (int inhPin : muxInh) {
    pinMode(inhPin, OUTPUT);
    digitalWrite(inhPin, HIGH);
  }

  // UART configuration lifted almost verbatim from https://controllerstech.com/how-to-use-uart-in-esp32-esp-idf/
  const uart_config_t uart_config = {
    .baud_rate = 115200,
    .data_bits = UART_DATA_8_BITS,
    .parity = UART_PARITY_DISABLE,
    .stop_bits = UART_STOP_BITS_1,
    .flow_ctrl = UART_HW_FLOWCTRL_DISABLE,
    .source_clk = UART_SCLK_DEFAULT,
  };


  int result = uart_driver_install(UART_NUM, RX_BUF_SIZE, 0, 10, &uart_queue, 0);
  if (result != ESP_OK) {
    // I'm not sure what I'd even want to do if this happens. Maybe restart the microcontroller? Luckily it hasn't popped up yet.
    //Serial.println("Failed to install UART driver");
  }

  xTaskCreate(uart_event_task, "uart_event_task", 3072, NULL, 12, NULL);

  uart_param_config(UART_NUM, &uart_config);
  ESP_ERROR_CHECK(uart_set_pin(UART_NUM, TXD_PIN, RXD_PIN, UART_PIN_NO_CHANGE, UART_PIN_NO_CHANGE));
  uint8_t* data = (uint8_t*)malloc(RX_BUF_SIZE * 2);
  int len = 0;
  delay(100);

  // I couldn't find out what this does in the docs. The GUI sends it, and it does seem to matter. The GUI can be found
  // here https://www.dlpdesign.com/rfrdr/ and it's a great tool to determine if there's something wrong with my circuit/commands
  // or the reader itself. I picked up a cheap USB to UART converter to test with.
  //Serial.println("Begin Setup");
  sendUartCommand(data, "010A0003041001210000");


  //Serial.println("Retrieving firmware version");
  sendUartCommand(data, "0108000304FE0000");

  // The DLP RFID2 comes with an internal antenna and a pin to connect an external antenna to. I'm using several switches to
  // multiplex to that one pin.
  //Serial.println("Enable external antenna");
  sendUartCommand(data, "01080003042B0000");

  // These three commands were what the GUI sent when selecting the ISO14443A protocol. The latter two seem to help correct
  // weak or slightly malformed signals.
  //Serial.println("ISO14443A setup");
  sendUartCommand(data, "010C00030410002101090000");
  //Serial.println("Sending AGC Toggle");
  sendUartCommand(data, "0109000304F0000000");
  //Serial.println("Sending AM PM Toggle");
  sendUartCommand(data, "0109000304F1FF0000");

  free(data);
}

void loop() {
  uint8_t* data = (uint8_t*)malloc(RX_BUF_SIZE * 2);
  // This makes me miss Kotlin's initialization functions
  std::string board[8][8];
  for (int i = 0; i < 8; i++) {
    for (int j = 0; j < 8; j++) {
      board[i][j] = "-";
    }
  }

  iterateMuxes(data, board);

  std::string fen = "";
  int emptySquares = 0;
  // Retrieve board state and convert to FEN.
  // I haven't thought about how I'm going to track state like which turn it is or if there's an en-passant square. That might be better
  // to do in the mobile app with a toggle for whose move it is.
  for(int i = 0; i < 8; i++) {
    for (int j = 0; j < 8; j++) {
      Serial.print(board[i][j].c_str());
      if (board[i][j] != "-") {
        if (emptySquares > 0) {
          fen += std::to_string(emptySquares);
          emptySquares = 0;
        }
        fen += board[i][j];
      } else {
        emptySquares++;
      }
    }
    if (emptySquares > 0) {
      fen += std::to_string(emptySquares);
    }
    if (i != 7) {
      fen += "/";
    }
    emptySquares = 0;
    Serial.println();
  }

  // The state I'm not tracking yet. Side to move, castling rights, en-passant square, half move clock, full move clock
  fen += " w KQkq - 0 0";
  Serial.println(fen.c_str());

  if (SerialBT.isReady()) {
    SerialBT.write((uint8_t*)fen.c_str(), fen.length());
    Serial.println("Sent FEN over bluetooth");
  } else {
    Serial.println("Unable to connect via bluetooth");
  }

  free(data);
  return;
}


void setMuxControlPins(int pin) {
  digitalWrite(c1, muxChannels[pin][0]);
  digitalWrite(c2, muxChannels[pin][1]);
  digitalWrite(c3, muxChannels[pin][2]);
  delay(1);
}

void setInhPins(int lowPin) {
  for (int inhPin : muxInh) {
    (inhPin == lowPin) ? digitalWrite(inhPin, LOW) : digitalWrite(inhPin, HIGH);
  }
  delay(1);
}

void iterateMuxes(uint8_t* data, std::string board[8][8]) {
  // Disable transmitter. An engineer on the Texas Instruments forum mentioned it's not a good idea to hot swap antennas.
  // https://e2e.ti.com/support/wireless-connectivity/other-wireless-group/other-wireless/f/other-wireless-technologies-forum/639799/trf7970a-multiplexing-the-dlp-rfid2-external-antenna-output/2370572
  //Serial.println("Disable RF");
  sendUartCommand(data, "010A0003041000010000");
  int i = 0;
  for (int inhPin : muxInh) {
    setInhPins(inhPin);
    //Serial.println("Mux: " + String(inhPin));
    for (int j = 0; j < 8; j++) {
      //Serial.println("Mux pin: y" + String(j));

      // Swap antennas
      setMuxControlPins(j);

      //Serial.println("Enable RF");
      sendUartCommand(data, "010A0003041000210000");

      //Serial.println("Inventory Tags");
      sendUartCommand(data, "0109000304A0010000");

      std::string tag((char*) data);
      for (auto tagToPiece : tagIdToPiece) {
        if (tag.find(tagToPiece.first) != std::string::npos) {
          //Serial.println(tagToPiece.second.c_str());
          int boardLocation = antennaLocationToSquare[i * 10 + j];
          board[boardLocation / 10][boardLocation % 10] = tagToPiece.second.c_str();
        }
      }

      //Serial.println("Disable RF");
      sendUartCommand(data, "010A0003041000010000");
    }
    i++;
  }
}
