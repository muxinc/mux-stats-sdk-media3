package com.mux.stats.sdk.media3_ima

import androidx.media3.common.Player

/**
 * Gets the Ad Tag URL for the AdsConfiguration associated with this [MediaItem], if any
 * Returns null if there's no ads configured, or no media item set on the [Player]
 */
fun Player.getAdTagUrl(): String? =
  currentMediaItem?.localConfiguration?.adsConfiguration?.adTagUri?.toString()
