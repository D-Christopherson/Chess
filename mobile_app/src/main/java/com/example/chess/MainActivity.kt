package com.example.chess

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.chess.ui.theme.ChessTheme
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onCreate(savedInstanceState: Bundle?) {
        requestMultiplePermissions.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.INTERNET,
            )
        )

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChessBoard("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", EvaluateResult("b2b3", 8, -1))
        }

        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter!!
        val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
        val client = OkHttpClient()
        pairedDevices.forEach { device ->
            Log.d("Main", device.name)
            if (device.name == "Chess") {
                val socket = device.createRfcommSocketToServiceRecord(UUID.fromString(device.uuids.first()
                    .toString()))
                socket.connect()
                while(true) {
                    while (socket.inputStream.available() == 0) {
                        Log.d("Main", "No data")
                        Thread.sleep(500)
                    }
                    Thread.sleep(500)

                    val fen = socket.inputStream.readNBytes(socket.inputStream.available())
                        .decodeToString()
                    Log.d("Main", fen)


                    val body = JSONObject()
                    body.put("fen", fen)
                    body.put("depth", 8)
                    val headers = mutableMapOf<String, String>()
                    headers["Authorization"] = resources.getString(R.string.AUTH_TOKEN)
                    headers["Content-Type"] = "application/json"
                    val request = Request.Builder()
                        .url("https://chess.dakotachristopherson.com/evaluate")
                        .post(body.toString().toRequestBody("application/json".toMediaType()))
                        .headers(headers.toHeaders())
                        .build()
                    thread(block = {
                        val response = client.newCall(request).execute()
                        Log.d("Main", response.body.string())
                        val evaluateResult = Json.decodeFromString<EvaluateResult>(response.body.string())
                        setContent {
                            ChessBoard(fen, evaluateResult)
                        }
                    }).join()
                }
            }
        }
    }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.d("Main", "${it.key} = ${it.value}")
            }
        }
}

@Serializable
data class EvaluateResult(val move: String?, val depth: Int, val evaluation: Int)