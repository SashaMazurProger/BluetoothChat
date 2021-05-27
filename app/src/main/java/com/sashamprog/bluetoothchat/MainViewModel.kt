package com.sashamprog.bluetoothchat

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@HiltViewModel
class MainViewModel @Inject constructor(@SerializableChatRepository private val mChatRepository: IChatRepository) :
    ViewModel() {

    private val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var mMessages: ArrayList<Message> = arrayListOf()
    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var uuid: UUID = UUID.fromString(UUID_STRING)

    var boundedDevicePosition: Int? = null
    val messages: MutableLiveData<List<Message>> = MutableLiveData()
    val bondedDevices: MutableLiveData<List<BluetoothDevice>> = MutableLiveData()
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

        val cachedMessages = mChatRepository.loadMessages("test") //TODO test

        if (!cachedMessages.isNullOrEmpty()) {
            mMessages.addAll(cachedMessages)
        }

        messages.value = mMessages
    }

    private fun sendMessage(message: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                outputStream?.write(message.toByteArray(Charset.forName("UTF-8")))
            } catch (e: Exception) {
                e.printStackTrace()
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

    /**
     * For sending messages to remote device
     * */
    private fun setupClient(device: BluetoothDevice?) {
        viewModelScope.launch(Dispatchers.IO) {
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

    /**
     * For listening messages from remote device
     * */
    private fun setupServer() {
        viewModelScope.launch(Dispatchers.IO) {
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

    private fun startListenMessages() {
        outputStream = socket?.outputStream
        listenMessages()
    }

    fun onBluetoothEnabledByRequest() {
        onBluetoothEnabled()
    }

    private fun listenMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            val input = socket?.inputStream
            val buffer = ByteArray(1024)

            while (socket != null && socket!!.isConnected) {
                try {
                    input?.read(buffer)
                    val message = String(buffer, Charset.forName("UTF-8"))
                    launch(Dispatchers.Main) { handleMessage(message) }
                } catch (e: Exception) {
                    e.printStackTrace()
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

        //TODO test
//        if (socket?.remoteDevice != null) {
        viewModelScope.launch(Dispatchers.IO) { mChatRepository.saveMessages("test", mMessages) }
//        }
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
}