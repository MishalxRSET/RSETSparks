package com.example.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.data.AppConfig

/** Build-time config (Blaze plan, version) for screens; provided by RSETSparksApp. */
val LocalAppConfig = staticCompositionLocalOf { AppConfig(isBlaze = false, versionName = "") }
