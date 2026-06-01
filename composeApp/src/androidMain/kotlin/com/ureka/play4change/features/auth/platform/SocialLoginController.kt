package com.ureka.play4change.features.auth.platform

import android.content.Intent
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Manages Facebook Login for one Activity lifetime.
 *
 * Create one instance in [com.ureka.play4change.MainActivity.onCreate] and store it
 * in [current]. The Activity must forward [onActivityResult] so the Facebook
 * [CallbackManager] can route results back to the pending coroutine.
 */
internal class SocialLoginController {

    internal val callbackManager: CallbackManager = CallbackManager.Factory.create()

    @Volatile private var pendingCont: CancellableContinuation<String?>? = null

    init {
        LoginManager.getInstance().registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    pendingCont?.resume(result.accessToken.token)
                    pendingCont = null
                }
                override fun onCancel() {
                    pendingCont?.resume(null)
                    pendingCont = null
                }
                override fun onError(error: FacebookException) {
                    pendingCont?.resumeWithException(error)
                    pendingCont = null
                }
            }
        )
    }

    /** Called from [com.ureka.play4change.MainActivity.onActivityResult]. */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    suspend fun facebookLogin(): String? = suspendCancellableCoroutine { cont ->
        pendingCont = cont
        cont.invokeOnCancellation { pendingCont = null }
        @Suppress("DEPRECATION")
        LoginManager.getInstance().logInWithReadPermissions(
            ActivityHolder.require(),
            callbackManager,
            listOf("email", "public_profile")
        )
    }

    companion object {
        /** Set by MainActivity; null between Activity instances. */
        @Volatile var current: SocialLoginController? = null
    }
}
