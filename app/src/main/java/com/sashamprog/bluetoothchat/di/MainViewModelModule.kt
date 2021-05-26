package com.sashamprog.bluetoothchat.di

import com.sashamprog.bluetoothchat.ChatRepository
import com.sashamprog.bluetoothchat.IChatRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import javax.inject.Singleton

@Module
@InstallIn(ViewModelComponent::class)
interface MainViewModelModule {

    @Binds
    fun repository(repository: ChatRepository): IChatRepository
}