package com.lastgimbus.the.freebuddy.widget

import android.content.Context
import androidx.annotation.StringRes

object WidgetLabels {
    fun visibleLabel(
        context: Context,
        mode: Int,
        @StringRes fullLabel: Int,
        shortLabel: String?,
    ): String = when (mode) {
        WidgetStorage.LABELS_SHORT -> shortLabel.orEmpty()
        WidgetStorage.LABELS_HIDDEN -> ""
        else -> context.getString(fullLabel)
    }
}
