package com.raphael.roadsystem.core.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ControllerModule {
    // Controladores pesados (SDKs) foram removidos em favor de implementações leves no Compose.
}
