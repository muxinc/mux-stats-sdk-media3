package com.mux.stats.sdk.muxstats

import android.content.Context
import android.view.View
import androidx.media3.common.Player
import com.mux.stats.sdk.core.CustomOptions
import com.mux.stats.sdk.core.model.CustomerData
import com.mux.stats.sdk.core.util.MuxLogger
import com.mux.stats.sdk.muxstats.internal.media3GenericBinding
import com.mux.stats.sdk.muxstats.media3.BuildConfig

/**
 * Monitors a generic [Player] with Mux Data, reporting data about the View to the environment
 * specified by the env key.
 *
 * @param context a Context containing your player. If it's an Activity, screen size is obtained
 * @param envKey your Mux Data Environment Key
 * @param customerData data about you, your customer, and your video
 * @param player the player you wish to observe
 * @param playerView the View showing your video content
 * @param customOptions Options that affect the behavior of the SDK
 * @param playerBinding a [MuxPlayerAdapter.PlayerBinding] that can observe the state of your player
 */
class MuxStatsSdkMedia3(
  context: Context,
  envKey: String,
  customerData: CustomerData,
  player: Player,
  playerView: View? = null,
  customOptions: CustomOptions? = null,
  playerBinding: MuxPlayerAdapter.PlayerBinding<Player> = media3GenericBinding()
) : MuxDataSdk<Player, View>(
  context = context,
  envKey = envKey,
  player = player,
  playerView = playerView,
  customerData = customerData,
  customOptions = customOptions ?: CustomOptions(),
  logLevel = LogcatLevel.VERBOSE,
  trackFirstFrame = true,
  playerBinding = playerBinding,
  device = AndroidDevice(
    ctx = context,
    playerVersion = BuildConfig.MEDIA3_VERSION, /* TODO: Dynamic would be better if possible*/
    muxPluginName = "mux-media3",
    muxPluginVersion = BuildConfig.LIB_VERSION,
    playerSoftware = "media3",
  )
)
