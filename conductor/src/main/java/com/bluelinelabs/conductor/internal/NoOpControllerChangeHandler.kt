package com.bluelinelabs.conductor.internal

import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.ControllerChangeHandler

class NoOpControllerChangeHandler : ControllerChangeHandler() {

  override fun performChange(
    container: ViewGroup,
    from: View?,
    to: View?,
    isPush: Boolean,
    changeListener: ControllerChangeCompletedListener
  ) {
    changeListener.onChangeCompleted()
  }

  override fun copy(): ControllerChangeHandler = NoOpControllerChangeHandler()

  override fun isReusable() = true
}
