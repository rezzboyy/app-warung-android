package com.warung.app.data.hardware

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

object BluetoothPrinterManager {

    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    fun getPairedPrinters(): List<BluetoothDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        return try {
            adapter.bondedDevices?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun printBytes(macAddress: String, data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        val adapter = BluetoothAdapter.getDefaultAdapter()
            ?: return@withContext Result.failure(Exception("Bluetooth tidak tersedia di perangkat ini."))

        if (!adapter.isEnabled) {
            return@withContext Result.failure(Exception("Bluetooth belum diaktifkan."))
        }

        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null

        try {
            val device = adapter.getRemoteDevice(macAddress)
            adapter.cancelDiscovery()

            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()

            outputStream = socket.outputStream
            outputStream.write(data)
            outputStream.flush()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Gagal mencetak ke printer: ${e.localizedMessage ?: "Koneksi terputus"}"))
        } finally {
            try {
                outputStream?.close()
                socket?.close()
            } catch (_: Exception) { }
        }
    }
}
