This repo contains all the relevant files for a chess board I'm building.

The top level directory contains a chess engine I wrote in Kotlin, serve with Spring Boot, and deploy to AWS with Terraform.
I'd written a chess engine in Java using two dimensional arrays to represent the board early on in college, so I wanted 
to try bitboards this time. The goal is to represent the board as a set of 64 bit numbers since operations with those should 
be much faster than an array. 

Some handwavy math for moving a rook up one square:
* One clock cycle to multiply the first index by the width of the outer array
* One clock cycle to add that to the base address of the array
* One clock cycle to multiply the second index by the data type the inner array contains
* One clock cycle to add that to the address calculated in step 2
* One clock cycle to null out that square
* Repeat above for the target square

With bitboards (ideally)
* One clock cycle to left shift the number representing the rook by 8

`chess_electronics` contains the schematic and PCB design for the circuit board that connects the microcontroller, RFID 
reader, and antennas together. These were created in KiCad.

`microcontroller` contains the code running on the ESP32. The microcontroller is responsible for controlling the 8 separate
8 way switches, driving the RFID reader over UART, and converting the RFID tags into pieces to create the actual game state.

`mobile_app` contains a proof of concept android app that reads the board state from the microcontroller over bluetooth 
and sends that state to the chess engine in AWS over the internet. The ESP32 could do this itself since it has wifi capabilities,
but it'd be a huge pain to reprogram the wifi credentials if I want to take the board to a club.

Here's an MS Paint breakdown of the board itself.

![Chessboard](https://github.com/D-Christopherson/Chess/blob/master/chessboard.jpg)

Eventually I'd like to strap an electromagnet to a pair of linear slides under the board to have the board play against 
a human. This product already exists, but costs between \$500 and \$1100.
The bill of materials for this build so far is:
* ESP32 dev board - $16 for a 3 pack on Amazon
* 8x SN74LV4051AN 8 way switch - $5.52 for 10 on DigiKey (cheaper than buying 8)
* 64x 13.56MHz RF antennas - $32 on DigiKey
* 65x 10nF ceramic capacitor - I bought a variety pack and used 47 nF capcitors after running out of 10nF. These will cost 
\$5-$30 on DigiKey. They just need to be capacitors that don't have polarity and are large enough to not add a ton of impedance 
(10nF is plenty).
* DLP RFID2 - $37.95 on DigiKey. I've read that this chip is pretty outdated and has some firmware bugs. There might be 
something cheaper nowadays. The key is just to make sure it's compatible with the same frequency as the tags/antennas used 
and also has a pin for an external antenna.
* 5x circuit boards (as that's the minimum quantity most factories will make) - ~\$120. I also have a directory in `chess_electronics` 
called `Modular_Antenna` which crams the screw terminals and capacitors into a small board independent of one that would 
house the microcontroller and RFID reader (I have not designed this motherboard yet). This should bring the cost down to 
$30 or so since there's less wasted space. I'm just not sure if it's acceptable to run signal lines so close to other traces.