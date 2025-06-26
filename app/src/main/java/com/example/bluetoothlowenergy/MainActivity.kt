package com.example.bluetoothlowenergy

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
class MainActivity : ComponentActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var scanner: BluetoothLeScanner
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        scanner = bluetoothAdapter.bluetoothLeScanner

        setContent {
            val devices = remember { mutableStateListOf<BluetoothDevice>() }
            val activity = this@MainActivity

            //We are preparing a list of permissions that the app needs:
            val permissions = listOf(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
            )
            val permissionState = rememberMultiplePermissionsState(permissions)

            LaunchedEffect(Unit) {
                permissionState.launchMultiplePermissionRequest()
            }

            if (permissionState.allPermissionsGranted) {
                ScanDeviceList(devices = devices) { device ->
                    activity.connectToDevice(device)
                }
                @SuppressLint("MissingPermission")
                LaunchedEffect(Unit) {
                    scanner.startScan(object : ScanCallback() {
                        override fun onScanResult(callbackType: Int, result: ScanResult?) {
                            result?.device?.let { device ->
                                if (!devices.contains(device) && device.name != null) {
                                    devices.add(device)
                                }
                            }
                        }
                    })
                }
            } else {
                Text("Permissions not granted.")
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun connectToDevice(device: BluetoothDevice) {
        //Think of it like calling the device and waiting for its replies about its services.
        device.connectGatt(this, false, object : BluetoothGattCallback() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int
            ) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices() //When the device successfully connects (STATE_CONNECTED), it starts discovering services available on the device.
                }
            }
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                gatt.services.forEach { service ->
                    Log.d("BLE", "Service: ${service.uuid}")
                    service.characteristics.forEach { char ->
                        Log.d("BLE", "Characteristic: ${char.uuid}")
                        gatt.readCharacteristic(char)//Reading characteristics lets you get the actual data from the device.
                    }
                }
            }
              //This is called when a characteristic’s value has been read successfully.
            //This is where you get actual data sent from the BLE peripheral.
            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                Log.d("BLE", "Read: ${characteristic.uuid} -> ${characteristic.value?.joinToString()}")
            }
        })
    }
}
@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
@Composable
fun ScanDeviceList(devices: List<BluetoothDevice>, onClick: (BluetoothDevice) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(devices) { device ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onClick(device) },
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Name: ${device.name}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Address: ${device.address}", fontSize = 14.sp)
                }
            }
        }
    }
}
