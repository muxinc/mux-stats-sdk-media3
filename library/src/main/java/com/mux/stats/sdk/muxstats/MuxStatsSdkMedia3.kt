package com.mux.stats.sdk.muxstats

import android.content.Context
import android.view.View
import androidx.media3.common.Player
import com.mux.stats.sdk.core.CustomOptions
import com.mux.stats.sdk.core.model.CustomerData
import com.mux.stats.sdk.muxstats.internal.media3GenericBinding
import com.mux.stats.sdk.muxstats.media3.BuildConfig

class MuxStatsSdkMedia3(
  context: Context,
  envKey: String,
  customerData: CustomerData,
  player: Player,
  playerView: View? = null,
  customOptions: CustomOptions? = null,
) : MuxDataSdk<Player, View>(
  context= context,
  envKey = envKey,
  player = player,
  playerView = playerView,
  customerData = customerData,
  customOptions = customOptions ?: CustomOptions(),
  trackFirstFrame = true,
  playerBinding = media3GenericBinding(),
  device = AndroidDevice(
    ctx = context,
    playerVersion = BuildConfig.MEDIA3_VERSION, /* TODO: Dynamic would be better if possible*/
    muxPluginName = "mux-media3" ,
    muxPluginVersion = BuildConfig.LIB_VERSION,
    playerSoftware = "media3",
  )
) {

}
