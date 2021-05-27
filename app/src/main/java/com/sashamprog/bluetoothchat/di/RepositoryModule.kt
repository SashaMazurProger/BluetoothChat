package com.sashamprog.bluetoothchat.di

import com.sashamprog.bluetoothchat.ChatRepository
import com.sashamprog.bluetoothchat.IChatRepository
import com.sashamprog.bluetoothchat.SerializableChatRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {

    @SerializableChatRepository
    @Binds
    @Singleton
    fun repository(repository: ChatRepository): IChatRepository
}
