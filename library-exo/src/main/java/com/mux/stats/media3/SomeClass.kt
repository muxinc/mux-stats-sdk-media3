package com.mux.stats.media3

import android.app.Activity
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.mux.stats.sdk.core.model.CustomerData
import com.mux.stats.sdk.muxstats.MuxPlayerAdapter
import com.mux.stats.sdk.muxstats.MuxStateCollector
import com.mux.stats.sdk.muxstats.MuxStatsSdkMedia3
import com.mux.stats.sdk.muxstats.internal.BaseMedia3Binding

class SomeClass : Activity() {
  fun aFunction(c: MuxStateCollector) {
    val s = MuxStatsSdkMedia3(
      this,
      envKey = "",
      customerData = CustomerData(),
      player = ExoPlayer.Builder(this).build(),
      playerView = null,
      customOptions = null,
      playerBinding = PB()
    )

    val binding = BaseMedia3Binding<Player>()
    binding.bindPlayer(
      ExoPlayer.Builder(this).build(),
      c
    )
  }

  fun f(binding: MuxPlayerAdapter.PlayerBinding<in Player>) {

  }

  fun fill(dest: Array<in String>, value: String) { }

  class BPB : MuxPlayerAdapter.PlayerBinding<Player> {
    override fun bindPlayer(player: Player, collector: MuxStateCollector) {
      TODO("Not yet implemented")
    }

    override fun unbindPlayer(player: Player, collector: MuxStateCollector) {
      TODO("Not yet implemented")
    }

  }

  class PB : MuxPlayerAdapter.PlayerBinding<ExoPlayer> {
    override fun bindPlayer(player: ExoPlayer, collector: MuxStateCollector) {
      TODO("Not yet implemented")
    }

    override fun unbindPlayer(player: ExoPlayer, collector: MuxStateCollector) {
      TODO("Not yet implemented")
    }

  }
}