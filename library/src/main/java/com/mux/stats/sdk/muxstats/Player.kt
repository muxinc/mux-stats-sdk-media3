package com.mux.stats.sdk.muxstats

import android.content.Context
import android.view.View
import androidx.media3.common.Player
import com.mux.stats.sdk.core.CustomOptions
import com.mux.stats.sdk.core.model.CustomerData

/**
 * Monitor this Player with Mux Data, reporting data about the View to the environment specified
 * by the env key
 *
 * @param context a Context containing your player. If it's an Activity, screen size is obtained
 * @param envKey your Mux Data Environment Key
 * @param customerData data about you, your customer, and your video
 * @param playerView the View showing your video content
 * @param customOptions Options that affect the behavior of the SDK
 */
fun Player.monitorWithMuxData(
  context: Context,
  envKey: String,
  customerData: CustomerData,
  playerView: View? = null,
  customOptions: CustomOptions? = null
): MuxStatsSdkMedia3 = MuxStatsSdkMedia3(
  context = context,
  envKey = envKey,
  customerData = customerData,
  player = this,
  playerView = playerView,
  customOptions = customOptions
)