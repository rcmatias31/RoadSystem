package com.raphael.roadsystem.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
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
     * @param context Contexto (preferencialmente Activity)
     * @param webClientId ID do cliente web configurado no console do Google/Firebase
     */
    suspend fun signInWithGoogle(context: Context, webClientId: String): Result<FirebaseUser?> {
        val credentialManager = CredentialManager.create(context)
        
        // Credential Manager precisa de uma Activity para mostrar o bottom sheet.
        val activity = context.findActivity() ?: return Result.failure(
            IllegalStateException("Context must be an Activity to show the Google Sign-In prompt")
        )

        android.util.Log.d("AuthRepository", "signInWithGoogle: webClientId=$webClientId")

        // Configura a opção de login com o Google
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        // Adiciona suporte adicional para garantir que o seletor apareça em mais cenários
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .addCredentialOption(signInWithGoogleOption)
            .build()

        return try {
            // Solicita a credencial ao usuário
            val result = credentialManager.getCredential(context = activity, request = request)
            
            // Extrai o ID Token do Google
            val credential = result.credential
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val googleIdToken = googleIdTokenCredential.idToken

            // Autentica no Firebase com o token recebido
            val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
            val authResult = auth.signInWithCredential(authCredential).await()
            
            Result.success(authResult.user)
        } catch (e: GetCredentialException) {
            android.util.Log.e("AuthRepository", "Credential Manager Error: ${e.type}", e)
            if (e is NoCredentialException) {
                android.util.Log.e("AuthRepository", "HINT: Verifique se o SHA-1 do Debug está no Console do Firebase e se o ClienteID está correto.")
            }
            Result.failure(e)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error signing in with Google", e)
            Result.failure(e)
        }
    }

    /**
     * Tenta encontrar a Activity a partir de um Context.
     */
    private fun Context.findActivity(): android.app.Activity? {
        var currentContext = this
        while (currentContext is android.content.ContextWrapper) {
            if (currentContext is android.app.Activity) return currentContext
            currentContext = currentContext.baseContext
        }
        return null
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
