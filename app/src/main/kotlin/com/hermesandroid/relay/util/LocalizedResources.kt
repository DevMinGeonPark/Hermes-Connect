package com.hermesandroid.relay.util

import android.content.Context
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat

/** Resolve per-app language for services and retained models as well as activities. */
fun Context.localizedResources(): Resources = ContextCompat.getContextForLanguage(this).resources

fun Context.localizedString(@StringRes id: Int, vararg arguments: Any): String =
    localizedResources().getString(id, *arguments)
