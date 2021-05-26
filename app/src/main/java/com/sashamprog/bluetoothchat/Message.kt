package com.sashamprog.bluetoothchat

import java.io.Serializable

data class Message(val text: String, val authorName: String, val timeInMillis: Long) : Serializable
