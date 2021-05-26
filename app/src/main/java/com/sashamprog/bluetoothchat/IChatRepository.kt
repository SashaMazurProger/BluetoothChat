package com.sashamprog.bluetoothchat

interface IChatRepository {
    fun saveMessages(remoteDevice: String, messages: List<Message>)
    fun loadMessages(remoteDevice: String): List<Message>?
}