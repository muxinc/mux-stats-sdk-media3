package com.mux.stats.sdk.muxstats.internal

import androidx.annotation.OptIn
import androidx.media3.common.Timeline
import androidx.media3.common.Timeline.Window
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.hls.HlsManifest
import com.mux.stats.sdk.core.util.MuxLogger
import com.mux.stats.sdk.muxstats.MuxStateCollector

/*
 * HlsUtils.kt: Utility functions for working with HLS playlists in exoplayer
 */

// lazily-cached check for the HLS extension, which may not be available at runtime
@OptIn(UnstableApi::class) // opting-in to HlsManifest
private val hlsExtensionAvailable: Boolean by lazy {
  try {
    Class.forName(HlsManifest::class.java.canonicalName!!)
    true
  } catch (e: ClassNotFoundException) {
    MuxLogger.w("isHlsExtensionAvailable", "HLS extension not found. Some features may not work")
    false
  } catch (e: LinkageError) {
    MuxLogger.w("isHlsExtensionAvailable", "HLS extension not found. Some features may not work")
    false
  } catch (e: ExceptionInInitializerError) {
    MuxLogger.w("isHlsExtensionAvailable", "HLS extension not found. Some features may not work")
    false
  }
}

/**
 * True when Exoplayer's HLS extension is available at runtime
 */
@JvmSynthetic
internal fun isHlsExtensionAvailable() = hlsExtensionAvailable

/**
 * Add livestream data to a [MuxStateCollector] if the given [Window] represents a live stream
 */
@JvmSynthetic
internal fun MuxStateCollector.populateLiveStreamData(window: Window) {
  if (window.isLive()) {
    hlsManifestNewestTime = window.windowStartTimeMs
    hlsHoldBack = parseManifestTagL(window, "HOLD-BACK")
    hlsPartHoldBack = parseManifestTagL(window, "PART-HOLD-BACK")
    hlsPartTargetDuration = parseManifestTagL(window, "PART-TARGET")
    hlsTargetDuration = parseManifestTagL(window, "EXT-X-TARGETDURATION")
  }
}

/**
 * Parses manifest tags representing a named numerical value, returning the value as a Long
 */
@JvmSynthetic
internal fun parseManifestTagL(currentWindow: Window, tagName: String): Long {
  var value: String = parseManifestTag(currentWindow, tagName)
  value = value.replace(".", "")
  try {
    return value.toLong()
  } catch (e: NumberFormatException) {
    MuxLogger.exception(e, "Manifest Parsing", "Bad number format for value: $value")
  }
  return -1L
}

/**
 * Parses manifest tags representing a named numerical value, returning the value as a string
 */
@OptIn(UnstableApi::class)
@JvmSynthetic
internal fun parseManifestTag(currentWindow: Timeline.Window, tagName: String): String {
  if (!isHlsExtensionAvailable()) {
    return "-1"
  }

  if (currentWindow.manifest != null && tagName.isNotEmpty()) {
    if (currentWindow.manifest is HlsManifest) {
      val manifest = currentWindow.manifest as HlsManifest
      for (tag in manifest.mediaPlaylist.tags) {
        if (tag.contains(tagName)) {
          var value = tag.split(tagName).toTypedArray()[1]
          if (value.contains(",")) {
            value = value.split(",").toTypedArray()[0]
          }
          if (value.startsWith("=") || value.startsWith(":")) {
            value = value.substring(1, value.length)
          }
          return value
        }
      }
    }
  }
  return "-1"
}
