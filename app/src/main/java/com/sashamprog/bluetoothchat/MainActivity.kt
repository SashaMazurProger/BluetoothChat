package com.sashamprog.bluetoothchat

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.io.OutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var messageAdapter: ArrayAdapter<String>? = null
    private var messages: MutableList<String> = mutableListOf()
    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private lateinit var uuid: UUID

    private val asyncJobs: MutableList<Job> = mutableListOf()


    fun launchAsync(block: suspend CoroutineScope.() -> Unit): Job {
        val job: Job = GlobalScope.launch(Dispatchers.Main) { block() }
        asyncJobs.add(job)
        job.invokeOnCompletion { asyncJobs.remove(job) }
        return job
    }

    suspend fun <T> async(block: suspend CoroutineScope.() -> T): T =
        withContext(Dispatchers.Default) { block() }

    private fun cancelAllAsync() {
        if (asyncJobs.size > 0) {
            for (i in asyncJobs.lastIndex downTo 0) {
                try {
                    asyncJobs[i].cancel()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    suspend fun <T> asyncAwait(block: suspend CoroutineScope.() -> T): T = async(block)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        uuid = UUID.fromString(getString(R.string.uuid))

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter!!.isEnabled) {
                onBluetoothEnabled()
            } else {
                startActivityForResult(
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                    ENABLE_BLUETOOTH_REQ
                )
            }
        }

        image_view_send.setOnClickListener {
            val message = edit_text_message.text.toString()
            edit_text_message.setText("")
            sendMessage(message)
        }

        button_vibrate.setOnClickListener {
            sendMessage(":vibro")
        }

        button_wifi.setOnClickListener {
            sendMessage(":wifi")
        }

        messageAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        list_view.adapter = messageAdapter
    }

    private fun sendMessage(message: String) {
        messages.add(message)
        val time = SimpleDateFormat("HH:mm:ss").format(Date())
        messageAdapter?.add(
            "$message\n\t${bluetoothAdapter?.name} $time"
        )
        messageAdapter?.notifyDataSetChanged()

        launchAsync {
            asyncAwait {
                try {
                    outputStream?.write(message.toByteArray(Charset.forName("UTF-8")))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun onBluetoothEnabled() {
        text_view_address.text = "Address:${bluetoothAdapter?.address}"
        text_view_name.text = "Name:${bluetoothAdapter?.name}"

        val bondedDevices = bluetoothAdapter?.bondedDevices
        if (bondedDevices != null) {
            val bondedAdapter =
                ArrayAdapter<String>(
                    this,
                    android.R.layout.simple_spinner_item,
                    bondedDevices.map { it.name })
            bondedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner_devices.adapter = bondedAdapter

            button_connect.setOnClickListener {
                val device = bondedDevices.toList()[spinner_devices.selectedItemPosition]
                setupClient(device)
            }
        }

        setupServer()

    }

    private fun setupClient(device: BluetoothDevice?) {
        //client
        launchAsync {
            asyncAwait {
                socket?.close()
                socket = try {
                    device?.createRfcommSocketToServiceRecord(uuid)
                } catch (e: Exception) {
                    null
                }
                bluetoothAdapter?.cancelDiscovery()
                try {
                    socket?.connect()
                    connected()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun setupServer() {
        //server
        launchAsync {
            asyncAwait {
                val serverSocket = try {
                    bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                        getString(R.string.app_name),
                        uuid
                    )
                } catch (e: Exception) {
                    null
                }

                var loop = true
                while (loop) {
                    socket = try {
                        serverSocket?.accept()
                    } catch (e: Exception) {
                        loop = false
                        null
                    }
                    socket?.also {
                        connected()
                        serverSocket?.close()
                        loop = false
                    }
                }
            }
        }
    }

    private fun connected() {
        outputStream = socket?.outputStream
        listenMessages()
    }

    private fun listenMessages() {
        launchAsync {
            asyncAwait {
                val input = socket?.inputStream
                val buffer = ByteArray(1024)

                while (socket != null && socket!!.isConnected) {
                    try {
                        input?.read(buffer)
                        val message = String(buffer, Charset.forName("UTF-8"))
                        launchAsync {
                            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                            messages.add(message)
                            val time = SimpleDateFormat("HH:mm:ss").format(Date())
                            messageAdapter?.add(
                                "$message\n\t${socket?.remoteDevice?.name} $time"
                            )
                            messageAdapter?.notifyDataSetChanged()
                            if (message.startsWith(":vibro")) {
                                val service = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                if (Build.VERSION.SDK_INT >= 26) {
                                    service.vibrate(
                                        VibrationEffect.createOneShot(
                                            1000,
                                            VibrationEffect.DEFAULT_AMPLITUDE
                                        )
                                    )
                                } else {
                                    service.vibrate(1000)
                                }
                            } else if (message.startsWith(":wifi")) {
                                val service =
                                    applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                                    service.isWifiEnabled = !service.isWifiEnabled
                                } else {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Операция недоступна",
                                        Toast.LENGTH_LONG
                                    )
                                        .show()
                                }
                            }
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ENABLE_BLUETOOTH_REQ) {
            if (resultCode == Activity.RESULT_OK) {
                onBluetoothEnabled()
            } else {
                startActivityForResult(
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                    ENABLE_BLUETOOTH_REQ
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelAllAsync()
        socket?.close()
        try {
            outputStream?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val ENABLE_BLUETOOTH_REQ = 10
    }
}
