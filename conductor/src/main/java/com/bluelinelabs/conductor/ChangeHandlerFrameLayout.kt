package com.bluelinelabs.conductor

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import com.bluelinelabs.conductor.ControllerChangeHandler.ControllerChangeListener

/**
 * A FrameLayout implementation that can be used to block user interactions while
 * [ControllerChangeHandler]s are performing changes. It is not required to use this
 * ViewGroup, but it can be helpful.
 */
class ChangeHandlerFrameLayout : FrameLayout, ControllerChangeListener {

  private var inProgressTransactionCount = 0

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
    context,
    attrs,
    defStyleAttr
  )

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
    context,
    attrs,
    defStyleAttr,
    defStyleRes
  )

  override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
    return inProgressTransactionCount > 0 || super.onInterceptTouchEvent(ev)
  }

  override fun onChangeStarted(
    to: Controller?,
    from: Controller?,
    isPush: Boolean,
    container: ViewGroup,
    handler: ControllerChangeHandler
  ) {
    inProgressTransactionCount++
  }

  override fun onChangeCompleted(
    to: Controller?,
    from: Controller?,
    isPush: Boolean,
    container: ViewGroup,
    handler: ControllerChangeHandler
  ) {
    inProgressTransactionCount--
  }
}