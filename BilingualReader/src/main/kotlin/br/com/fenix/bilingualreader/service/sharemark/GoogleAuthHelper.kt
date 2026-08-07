package br.com.fenix.bilingualreader.service.sharemark

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import org.slf4j.LoggerFactory

object GoogleAuthHelper {

    private val mLOGGER = LoggerFactory.getLogger(GoogleAuthHelper::class.java)

    suspend fun signIn(context: Context, activity: Activity, serverClientId: String): GoogleIdTokenCredential? {
        val credentialManager = CredentialManager.create(context)

        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            return parseGoogleIdToken(credentialManager.getCredential(activity, request))
        } catch (e: NoCredentialException) {
            mLOGGER.info("No Google ID credential available, falling back to Sign in with Google")
        } catch (e: GetCredentialException) {
            mLOGGER.warn("GetGoogleIdOption failed, falling back to Sign in with Google", e)
        }

        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(serverClientId).build()
        val fallbackRequest = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        return parseGoogleIdToken(credentialManager.getCredential(activity, fallbackRequest))
    }

    fun currentEmail(): String? =
        FirebaseAuth.getInstance().currentUser?.email

    fun isSignedIn(): Boolean =
        FirebaseAuth.getInstance().currentUser != null

    suspend fun signOut(context: Context) {
        FirebaseAuth.getInstance().signOut()
        try {
            CredentialManager.create(context)
                .clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            mLOGGER.warn("Could not clear credential state", e)
        }
    }

    suspend fun ensureFirebaseAuth(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential).await()
    }

    private fun parseGoogleIdToken(response: GetCredentialResponse): GoogleIdTokenCredential? {
        val credential = response.credential
        return if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            GoogleIdTokenCredential.createFrom(credential.data)
        } else {
            mLOGGER.warn("Credential is not a Google ID token")
            null
        }
    }

}
