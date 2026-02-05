/*
 * Ruta: app/src/main/java/com/empresa/asistencia/di/AppModule.kt
 * Descripción: Módulo de Hilt que provee las dependencias globales (Gson, FaceRecognizer, Repositorio).
 */
package com.empresa.asistencia.di

import android.content.Context
import com.empresa.asistencia.data.EmployeeRepository
import com.empresa.asistencia.domain.FaceRecognizer
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideFaceRecognizer(@ApplicationContext context: Context): FaceRecognizer {
        return FaceRecognizer(context)
    }

    @Provides
    @Singleton
    fun provideEmployeeRepository(
        @ApplicationContext context: Context,
        gson: Gson
    ): EmployeeRepository {
        return EmployeeRepository(context, gson)
    }
}