package com.sashamprog.bluetoothchat

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.lang.Exception
import javax.inject.Inject

class ChatRepository @Inject constructor(@ApplicationContext private val mContext: Context) :
    IChatRepository {

    override fun saveMessages(remoteDevice: String, messages: List<Message>) {
        try {
            val file = File(mContext.filesDir.path + "messages_$remoteDevice")

            if (!file.exists()) {
                file.createNewFile()
            }

            val outputStream = file.outputStream()
            val objectOutputStream = ObjectOutputStream(outputStream)
            val serializableMessages = arrayListOf<Message>()
            if (messages.isNotEmpty()) serializableMessages.addAll(messages)
            objectOutputStream.writeObject(serializableMessages)
            objectOutputStream.close()
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun loadMessages(remoteDevice: String): List<Message>? {
        return try {
            val file = File(mContext.filesDir.path + "messages_$remoteDevice")

            if (!file.exists()) {
                return null
            }

            val inputStream = file.inputStream()
            val objectInputStream = ObjectInputStream(inputStream)
            val messages = objectInputStream.readObject() as ArrayList<Message>
            objectInputStream.close()
            inputStream.close()
            messages
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

