@file:JvmName("ThreadUtils")

package com.bluelinelabs.conductor.internal

import android.os.Looper
import android.util.AndroidRuntimeException
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP

@RestrictTo(LIBRARY_GROUP)
internal fun ensureMainThread() {
  if (Looper.getMainLooper().thread !== Thread.currentThread()) {
    throw CalledFromWrongThreadException("Methods that affect the view hierarchy can can only be called from the main thread.")
  }
}

private class CalledFromWrongThreadException(msg: String?) : AndroidRuntimeException(msg)
