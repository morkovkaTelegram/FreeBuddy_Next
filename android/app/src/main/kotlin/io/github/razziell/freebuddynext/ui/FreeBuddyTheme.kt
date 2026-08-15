package io.github.razziell.freebuddynext.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.razziell.freebuddynext.data.ThemeStore

@Composable
fun FreeBuddyTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    // remember(): ThemeStore builds a new Flow instance per call; without it
    // every recomposition of the theme root restarts the DataStore collector.
    val mode by remember { ThemeStore.themeMode(context) }
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
