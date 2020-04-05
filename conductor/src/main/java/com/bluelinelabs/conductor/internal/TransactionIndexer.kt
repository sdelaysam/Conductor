package com.bluelinelabs.conductor.internal

import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP

@RestrictTo(LIBRARY_GROUP)
class TransactionIndexer {

    private var currentIndex = 0

    fun nextIndex(): Int {
        return ++currentIndex
    }

    fun saveInstanceState(outState: Bundle) {
        outState.putInt(KEY_INDEX, currentIndex)
    }

    fun restoreInstanceState(savedInstanceState: Bundle) {
        currentIndex = savedInstanceState.getInt(KEY_INDEX)
    }
}

private const val KEY_INDEX = "TransactionIndexer.currentIndex"
