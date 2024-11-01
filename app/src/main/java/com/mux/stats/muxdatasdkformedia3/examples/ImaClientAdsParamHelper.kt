package com.mux.stats.muxdatasdkformedia3.examples

import android.os.Bundle
import android.view.MenuItem
import androidx.media3.common.MediaItem
import com.mux.stats.muxdatasdkformedia3.Constants

/**
 * Helper class for the example Activities that handles managing playback parameters, etc for
 * IMA client-side ads
 */
class ImaClientAdsParamHelper {
  // todo: set data from Intent (ie, deep linking)

  var playbackToken: String? = null
  var sourceUrl: String? = null
  var adTagUrl: String? = null


  fun createMediaItemBuilder(): MediaItem.Builder {
//    return MediaItems.builderFromMuxPlaybackId(
//      playbackId = playbackIdOrDefault(),
//      minResolution = minRes,
//      maxResolution = maxRes,
//      renditionOrder = renditionOrder,
//      assetStartTime = assetStartTime,
//      assetEndTime = assetEndTime,
//      playbackToken = playbackToken?.ifEmpty { null },
//      drmToken = drmToken?.ifEmpty { null },
//      domain = customDomain?.ifEmpty { null },
//    )
    // todo
    return MediaItem.Builder()
  }

  fun sourceUrlOrDefault(): String {
    return sourceUrl?.ifEmpty { DEFAULT_SOURCE_URL } ?: DEFAULT_SOURCE_URL
  }

  fun createMediaItem(): MediaItem {
    return createMediaItemBuilder().build()
  }

  fun saveInstanceState(state: Bundle) {
    //todo
  }

  fun restoreInstanceState(state: Bundle) {
    // todo
  }

  companion object {
    const val DEFAULT_SOURCE_URL = Constants.VOD_TEST_URL_TEARS_OF_STEEL
  }
}
