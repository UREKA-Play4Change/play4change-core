# ── Kotlin ────────────────────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses, Signature, SourceFile, LineNumberTable
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**

# ── Kotlinx Serialization ─────────────────────────────────────────────────────
-keepattributes RuntimeVisibleAnnotations
-keep @kotlinx.serialization.Serializable class ** { *; }
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# ── Ktor ──────────────────────────────────────────────────────────────────────
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ── Koin ──────────────────────────────────────────────────────────────────────
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# ── Decompose ─────────────────────────────────────────────────────────────────
-keep class com.arkivanov.decompose.** { *; }
-dontwarn com.arkivanov.**

# ── Compose ───────────────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── App models (Serializable data classes sent over the network) ───────────────
-keep class com.ureka.play4change.** { *; }

# ── OkHttp / Okio ────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ── Google Error Prone / Tink ─────────────────────────────────────────────────
-dontwarn com.google.errorprone.annotations.**
