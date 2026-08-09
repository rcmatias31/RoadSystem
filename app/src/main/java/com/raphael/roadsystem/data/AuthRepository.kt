package com.raphael.roadsystem.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await


/**
 * Repositório responsável pela autenticação do usuário utilizando Firebase e Credential Manager.
 */
class AuthRepository(private val auth: FirebaseAuth) {

    /**
     * Retorna o usuário atualmente logado no Firebase, ou null se não houver um.
     */
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    /**
     * Inicia o fluxo de login com o Google utilizando o Credential Manager.
     * 
     * @param context Contexto da Activity (necessário para o Credential Manager)
     * @param webClientId ID do cliente web configurado no console do Google/Firebase
     */
    suspend fun signInWithGoogle(context: Context, webClientId: String): Result<FirebaseUser?> {
        val credentialManager = CredentialManager.create(context)
        android.util.Log.d("AuthRepository", "signInWithGoogle: webClientId=$webClientId")

        // Configura a opção de login com o Google
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            // Solicita a credencial ao usuário
            val result = credentialManager.getCredential(context = context, request = request)
            
            // Extrai o ID Token do Google
            val credential = result.credential
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val googleIdToken = googleIdTokenCredential.idToken

            // Autentica no Firebase com o token recebido
            val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
            val authResult = auth.signInWithCredential(authCredential).await()
            
            Result.success(authResult.user)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error signing in with Google", e)
            Result.failure(e)
        }
    }

    /**
     * Realiza o logout do usuário.
     */
    fun signOut() {
        auth.signOut()
    }

    suspend fun getFreshToken(): String? {
        return try {
            auth.currentUser?.getIdToken(true)?.await()?.token
        } catch (e: Exception) {
            null
        }
    }
}
