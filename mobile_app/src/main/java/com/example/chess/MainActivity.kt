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
import java.util.UUID

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
        // The buttons are going to throw NPEs if I'm not connected to the ESP32, but it's useful
        // to be able to test UI changes without having to turn that on.
        setContent {
            ChessBoard(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                EvaluateResult("", 0, 0)
            )
        }

        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter!!
        val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
        pairedDevices.forEach { device ->
            Log.d("Main", device.name)
            if (device.name == "Chess") {
                val socket = device.createRfcommSocketToServiceRecord(
                    UUID.fromString(
                        device.uuids.first()
                            .toString()
                    )
                )
                setContent {
                    ChessBoard(
                        "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                        EvaluateResult("", 0, 0),
                        socket,
                        resources.getString(R.string.AUTH_TOKEN)
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

