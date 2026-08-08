package com.lastgimbus.the.freebuddy.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lastgimbus.the.freebuddy.data.ThemeStore

@Composable
fun FreeBuddyTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val mode by ThemeStore.themeMode(context)
        .collectAsStateWithLifecycle(initialValue = ThemeStore.ThemeMode.SYSTEM)
    val dark = when (mode) {
        ThemeStore.ThemeMode.LIGHT -> false
        ThemeStore.ThemeMode.DARK -> true
        ThemeStore.ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colors = when {
        Build.VERSION.SDK_INT >= 31 && dark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= 31 -> dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colors, content = content)
}
