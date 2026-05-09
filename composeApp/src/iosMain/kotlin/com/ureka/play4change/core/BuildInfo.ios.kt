package com.ureka.play4change.core

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

/**
 * True for debug/simulator builds, false for release/App Store builds.
 * [Platform.isDebugBinary] is set by the Kotlin/Native linker at compile time:
 * a debug framework link sets it to true; a release framework link sets it to false.
 * This makes the in-app token paste field (guarded by [isDebugBuild]) visible on the
 * simulator without any runtime flag injection.
 * See DECISIONS.md [2026-05-09] [iosMain] — isDebugBuild via Platform.isDebugBinary.
 */
@OptIn(ExperimentalNativeApi::class)
actual val isDebugBuild: Boolean = Platform.isDebugBinary
