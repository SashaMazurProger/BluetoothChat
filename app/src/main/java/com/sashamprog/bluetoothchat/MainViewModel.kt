package com.sashamprog.bluetoothchat

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.*

class MainViewModel : ViewModel() {

    private var asyncJobs: MutableList<Job> = mutableListOf()
    private val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    private var mMessages: MutableList<Message> = mutableListOf()
    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var uuid: UUID = UUID.fromString(UUID_STRING)
    val messages: MutableLiveData<List<Message>> = MutableLiveData()
    val bondedDevices: MutableLiveData<List<BluetoothDevice>> = MutableLiveData()
    var boundedDevicePosition: Int? = null

    val bluetoothAddress: MutableLiveData<String> = MutableLiveData("")
    val bluetoothName: MutableLiveData<String> = MutableLiveData("")
    val userMessage: MutableLiveData<String> = MutableLiveData()
    val makeVibrate: MutableLiveData<Any> = MutableLiveData()
    val enableBluetooth: MutableLiveData<Any> = MutableLiveData()
    val toggleWifi: MutableLiveData<Any> = MutableLiveData()


    init {
        if (bluetoothAdapter.isEnabled) {
            onBluetoothEnabled()
        } else {
            enableBluetooth.value = Any()
        }
    }

    private fun sendMessage(message: String) {
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
        bluetoothAddress.value = bluetoothAdapter.address
        bluetoothName.value = bluetoothAdapter.name
        boundedDevicePosition = null
        bondedDevices.value = bluetoothAdapter.bondedDevices?.toList()
        setupServer()
    }

    fun setupClient(device: BluetoothDevice?) {
        //client
        launchAsync {
            asyncAwait {
                socket?.close()
                socket = try {
                    device?.createRfcommSocketToServiceRecord(uuid)
                } catch (e: Exception) {
                    null
                }
                bluetoothAdapter.cancelDiscovery()
                try {
                    socket?.connect()
                    startListenMessages()
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
                    bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, uuid)
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
                        startListenMessages()
                        serverSocket?.close()
                        loop = false
                    }
                }
            }
        }
    }

    private fun startListenMessages() {
        outputStream = socket?.outputStream
        listenMessages()
    }

    fun onBluetoothEnabledByRequest() {
        onBluetoothEnabled()
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
                        launchAsync { handleMessage(message) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun handleMessage(message: String) {
        when {
            message.startsWith(":vibro") -> {
                addMessageToList("Make vibro", true)
                makeVibrate.value = Any()
            }
            message.startsWith(":wifi") -> {
                addMessageToList("Toggle wifi", true)
                toggleWifi.value = Any()
            }
            else -> {
                addMessageToList(message, true)
            }
        }
    }

    private fun addMessageToList(message: String, fromRemoteDevice: Boolean) {

        val authorName = if (fromRemoteDevice) socket?.remoteDevice?.name ?: "Unknown"
        else "${bluetoothName.value ?: ""}(Me)"

        mMessages.add(
            Message(
                message,
                authorName,
                System.currentTimeMillis()
            )
        )
        messages.value = mMessages
    }

    //

    fun onVibrateButtonClicked() {
        addMessageToList("Make vibro", false)
        sendMessage(":vibro")
    }

    fun onWifiButtonClicked() {
        addMessageToList("Toggle wifi", false)
        sendMessage(":wifi")
    }

    fun onConnectButtonClicked() {
        if (!bondedDevices.value.isNullOrEmpty() && boundedDevicePosition != null) {
            val device = bondedDevices.value?.get(boundedDevicePosition!!)
            if (device != null) setupClient(device)
        }
    }

    fun onSendMessageButtonClicked() {
        if (!userMessage.value.isNullOrEmpty()) {
            addMessageToList(userMessage.value!!, false)
            sendMessage(userMessage.value!!)
            userMessage.value = ""
        }
    }

    //

    override fun onCleared() {
        super.onCleared()
        cancelAllAsync()
        socket?.close()
        try {
            outputStream?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val UUID_STRING: String = "a8d52687-55bb-4b1b-b8f5-a1b17cdb0e59"
        const val APP_NAME: String = "BluetoothChat"
    }


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
}