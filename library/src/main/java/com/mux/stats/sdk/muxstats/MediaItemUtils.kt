@file:JvmName("MediaItemUtils")
package com.mux.stats.sdk.muxstats

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.RequestMetadata
import com.mux.stats.sdk.core.model.CustomerData
import org.json.JSONObject

const val BUNDLE_MUX_CUSTOMER_DATA = "mux_customer_data"

/**
 * Adds the given [CustomerData] to this [RequestMetadata.Builder]. If you have other metadata to
 * add to your request, you can do so before or after calling this method.
 *
 * @see MediaItem.Builder.addMuxMetadata
 */
fun RequestMetadata.Builder.addMuxMetadata(data: CustomerData): RequestMetadata.Builder {
 return this.setExtras(
   Bundle().apply { putBundle(BUNDLE_MUX_CUSTOMER_DATA, BundledCustomerData(data).toBundle()) }
 )
}


/**
 * Adds the given [CustomerData] to this [MediaItem.Builder]. Use this if you don't have any of your
 * own [RequestMetadata] to add.
 *
 * This method will clear any existing RequestMetadata]on the [MediaItem.Builder].
 *
 * @see RequestMetadata.Builder.addMuxMetadata
 */
fun MediaItem.Builder.addMuxMetadata(data: CustomerData): MediaItem.Builder {
  val metadata = RequestMetadata.Builder()
    .addMuxMetadata(data)
    .build()
  this.setRequestMetadata(metadata)
  return this
}

/**
 * Gets the previously-added Mux [CustomerData] from this [MediaItem], if one was added.
 *
 * Returns null if there was no Mux [CustomerData] added.
 */
fun MediaItem.getMuxMetadata(): CustomerData? {
  return runCatching {
    @Suppress("UNNECESSARY_SAFE_CALL") // extras are definitely nullable
    this.requestMetadata?.extras?.getBundle(BUNDLE_MUX_CUSTOMER_DATA)?.let {
      BundledCustomerData(it).data
    }
  }.getOrNull()
}

internal class BundledCustomerData(val data: CustomerData) {

  companion object {
    const val BUNDLE_PLAYER_DATA = "player-data"
    const val BUNDLE_VIDEO_DATA = "video-data"
    const val BUNDLE_VIEW_DATA = "view-data"
    const val BUNDLE_VIEWER_DATA = "viewer-data"
    const val BUNDLE_CUSTOM_DATA = "custom-data"
  }

  constructor(bundle: Bundle) : this(CustomerData()) {
    bundle.getString(BUNDLE_PLAYER_DATA, null)
      ?.let { data.customerPlayerData.replace(JSONObject(it)) }
    bundle.getString(BUNDLE_VIDEO_DATA, null)
      ?.let { data.customerVideoData.replace(JSONObject(it)) }
    bundle.getString(BUNDLE_VIEW_DATA, null)
      ?.let { data.customerViewData.replace(JSONObject(it)) }
    bundle.getString(BUNDLE_VIEWER_DATA, null)
      ?.let { data.customerViewerData.replace(JSONObject(it)) }
    bundle.getString(BUNDLE_CUSTOM_DATA, null)
      ?.let { data.customData.replace(JSONObject(it)) }
  }

  fun toBundle(): Bundle {
    val bundle = Bundle()
    bundle.putString(BUNDLE_PLAYER_DATA, data.customerPlayerData.muxDictionary.toString())
    bundle.putString(BUNDLE_VIDEO_DATA, data.customerVideoData.muxDictionary.toString())
    bundle.putString(BUNDLE_VIEW_DATA, data.customerViewData.muxDictionary.toString())
    bundle.putString(BUNDLE_VIEWER_DATA, data.customerViewerData.muxDictionary.toString())
    bundle.putString(BUNDLE_CUSTOM_DATA, data.customData.muxDictionary.toString())
    return bundle
  }
}
