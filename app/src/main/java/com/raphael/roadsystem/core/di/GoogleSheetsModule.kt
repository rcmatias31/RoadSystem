package com.raphael.roadsystem.core.di

import android.content.Context
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.InputStream
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GoogleSheetsModule {

    @Provides
    @Singleton
    @Named("spreadsheetId")
    fun provideSpreadsheetId(): String = "1F68CS_4CGRMPm4HqJMETBFdj6XM7ObJbiFyvdiuhoDA" // Substituir pelo ID Real da Planilha

    @Provides
    @Singleton
    fun provideSheetsService(@ApplicationContext context: Context): Sheets {
        val assetManager = context.assets
        val inputStream: InputStream = try {
            assetManager.open("service_account.json")
        } catch (e: Exception) {
            throw IllegalStateException(
                "O arquivo 'service_account.json' não foi encontrado na pasta 'assets'. " +
                "Certifique-se de que as credenciais da Service Account do Google Cloud foram adicionadas corretamente.", e
            )
        }
        
        val credentials = try {
            GoogleCredentials.fromStream(inputStream)
                .createScoped(listOf(SheetsScopes.SPREADSHEETS))
        } catch (e: Exception) {
            throw IllegalStateException(
                "Erro ao ler as credenciais de 'service_account.json'. O arquivo pode estar malformado ou incompleto.", e
            )
        }
        
        return Sheets.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            HttpCredentialsAdapter(credentials)
        ).setApplicationName("RoadSystem").build()
    }
}
