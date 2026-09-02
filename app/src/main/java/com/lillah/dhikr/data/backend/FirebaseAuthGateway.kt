package com.lillah.dhikr.data.backend

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.lillah.dhikr.domain.sync.AuthMethod
import com.lillah.dhikr.domain.sync.AuthUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume

/**
 * Firebase Authentication, reached through Google's Credential Manager and the Facebook Login SDK.
 *
 * Both providers end at the same place — a Firebase credential exchanged for a Firebase user — so
 * the rest of the app sees one uid regardless of how somebody signed in, and the backend's
 * security rules have a single identity to authorise against.
 */
class FirebaseAuthGateway(private val context: Context) : AuthGateway {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override val isConfigured = BackendConfig.hasFirebase

    override val availableMethods: List<AuthMethod> = buildList {
        if (BackendConfig.hasGoogleSignIn) add(AuthMethod.Google)
        if (BackendConfig.hasFacebookSignIn) add(AuthMethod.Facebook)
    }

    override fun currentUser(): Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { instance ->
            trySend(instance.currentUser?.toAuthUser())
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signIn(activity: Activity, method: AuthMethod): Result<AuthUser> =
        when (method) {
            AuthMethod.Google -> signInWithGoogle(activity)
            AuthMethod.Facebook -> signInWithFacebook(activity)
        }

    private suspend fun signInWithGoogle(activity: Activity): Result<AuthUser> = runCatching {
        require(BackendConfig.hasGoogleSignIn) { "Google sign-in is not configured in this build." }

        val option = GetGoogleIdOption.Builder()
            // Show every Google account on the device, not only ones already used with this app:
            // a first sign-in has nothing authorised yet, and an empty chooser looks broken.
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BackendConfig.googleWebClientId)
            .build()

        val response = CredentialManager.create(context).getCredential(
            context = activity,
            request = GetCredentialRequest.Builder().addCredentialOption(option).build(),
        )
        val googleCredential = GoogleIdTokenCredential.createFrom(response.credential.data)
        val firebaseCredential =
            GoogleAuthProvider.getCredential(googleCredential.idToken, null)

        auth.signInWithCredential(firebaseCredential).await().user
            ?.toAuthUser(AuthMethod.Google)
            ?: error("Google sign-in returned no user.")
    }.recoverCatching { error ->
        // A deliberate dismissal is not a failure worth showing as an error.
        if (error is GetCredentialCancellationException) throw SignInCancelled else throw error
    }

    private suspend fun signInWithFacebook(activity: Activity): Result<AuthUser> = runCatching {
        require(BackendConfig.hasFacebookSignIn) {
            "Facebook sign-in is not configured in this build."
        }
        ensureFacebookInitialized()

        val accessToken = suspendCancellableCoroutine<String?> { continuation ->
            val callbackManager = CallbackManager.Factory.create()
            val loginManager = LoginManager.getInstance()
            loginManager.registerCallback(
                callbackManager,
                object : FacebookCallback<LoginResult> {
                    override fun onSuccess(result: LoginResult) {
                        if (continuation.isActive) continuation.resume(result.accessToken.token)
                    }

                    override fun onCancel() {
                        if (continuation.isActive) continuation.resume(null)
                    }

                    override fun onError(error: FacebookException) {
                        if (continuation.isActive) continuation.resume(null)
                    }
                },
            )
            loginManager.logIn(
                activity as ActivityResultRegistryOwner,
                callbackManager,
                listOf("email", "public_profile"),
            )
            continuation.invokeOnCancellation { loginManager.unregisterCallback(callbackManager) }
        } ?: throw SignInCancelled

        auth.signInWithCredential(FacebookAuthProvider.getCredential(accessToken)).await().user
            ?.toAuthUser(AuthMethod.Facebook)
            ?: error("Facebook sign-in returned no user.")
    }

    override suspend fun signOut() {
        runCatching { auth.signOut() }
        // Clears the Facebook session too, so a second sign-in shows the account chooser rather
        // than silently reusing the previous one.
        runCatching { if (BackendConfig.hasFacebookSignIn) LoginManager.getInstance().logOut() }
    }

    /**
     * Facebook auto-init is disabled in the manifest so that a build with no Facebook app never
     * starts the SDK with placeholder credentials. It is started here instead, on first use.
     */
    private fun ensureFacebookInitialized() {
        if (FacebookSdk.isInitialized()) return
        FacebookSdk.setApplicationId(BackendConfig.facebookAppId)
        FacebookSdk.setClientToken(BackendConfig.facebookClientToken)
        FacebookSdk.setAutoInitEnabled(true)
        FacebookSdk.fullyInitialize()
    }

    private fun FirebaseUser.toAuthUser(method: AuthMethod? = null) = AuthUser(
        uid = uid,
        displayName = displayName,
        email = email,
        photoUrl = photoUrl?.toString(),
        method = method ?: providerId.toAuthMethod(),
    )

    private fun String?.toAuthMethod(): AuthMethod? = when {
        this == null -> null
        contains("google") -> AuthMethod.Google
        contains("facebook") -> AuthMethod.Facebook
        else -> null
    }
}

object SignInCancelled : Exception("Sign-in was cancelled.")
