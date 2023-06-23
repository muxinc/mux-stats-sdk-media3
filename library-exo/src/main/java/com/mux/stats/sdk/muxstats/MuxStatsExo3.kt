package com.mux.stats.sdk.muxstats

import android.content.Context
import android.view.View
import androidx.media3.exoplayer.ExoPlayer
import com.mux.stats.sdk.core.CustomOptions
import com.mux.stats.sdk.core.model.CustomerData
import com.mux.stats.sdk.muxstats.media3.BuildConfig

/**
 * Monitors an instance of [ExoPlayer], reporting events and metrics to a Mux Data environment
 *
 * // TODO: Usage examples, show the extensions off
 *
 * @param context a Context containing your player. If it's an Activity, screen size is obtained
 * @param envKey your Mux Data Environment Key
 * @param customerData data about you, your customer, and your video
 * @param player the player you wish to observe
 * @param playerView the View showing your video content
 * @param customOptions Options that affect the behavior of the SDK
 */
class MuxStatsExo3 @JvmOverloads constructor(
  context: Context,
  envKey: String,
  customerData: CustomerData,
  player: ExoPlayer,
  playerView: View? = null,
  customOptions: CustomOptions? = null,
) : MuxDataSdk<ExoPlayer, View>(
  context = context,
  envKey = envKey,
  player = player,
  playerView = playerView,
  customerData = customerData,
  customOptions = customOptions ?: CustomOptions(),
  logLevel = LogcatLevel.VERBOSE,
  trackFirstFrame = true,
  playerBinding = BaseMedia3Binding(), // TODO: Plug in the exo binding
  device = AndroidDevice(
    ctx = context,
    playerVersion = BuildConfig.MEDIA3_VERSION, /* TODO: Dynamic would be better if possible*/
    muxPluginName = "mux-media3",
    muxPluginVersion = BuildConfig.LIB_VERSION,
    playerSoftware = "media3-generic",
  )
)
