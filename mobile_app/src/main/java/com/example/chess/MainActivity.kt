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
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestMultiplePermissions.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.INTERNET,
            )
        )
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
                    val body = FormBody.Builder()
                        .add("fen", fen)
                        .add("depth", "8")
                        .build()
                    val request = Request.Builder()
                        .url("https://chess.dakotachristopherson.com/evaluate")
                        .post(body)
                        .build()
                    thread(block = {
                        val response = client.newCall(request).execute()
                        Log.d("Main", response.body.string())
                    }).join()

                }
            }
        }
        enableEdgeToEdge()
        setContent {
            ChessTheme {
                Scaffold( modifier = Modifier.fillMaxSize() ) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
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

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ChessTheme {
        Greeting("Android")
    }
}

