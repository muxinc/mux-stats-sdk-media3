package com.mux.stats.sdk.muxstats.internal

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.hls.HlsManifest
import com.mux.stats.sdk.core.util.MuxLogger

// lazily-cached check for the HLS extension, which may not be available at runtime
@OptIn(UnstableApi::class) // opting-in to HlsManifest
private val hlsExtensionAvailable: Boolean by lazy {
  try {
    Class.forName(HlsManifest::class.java.canonicalName!!)
    true
  } catch (e: ClassNotFoundException) {
    MuxLogger.w("isHlsExtensionAvailable", "HLS extension not found. Some features may not work")
    false
  }
}

@JvmSynthetic
internal fun isHlsExtensionAvailable() = hlsExtensionAvailable
