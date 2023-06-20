package com.mux.stats.muxdatasdkformedia3.examples.background

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.mux.stats.sdk.muxstats.MuxStatsSdkMedia3

class ExamplePlayerService : MediaSessionService() {

  private var mediaSession: MediaSession? = null
  private var muxStats: MuxStatsSdkMedia3? = null

  override fun onCreate() {
    super.onCreate()
    val player = createPlayer()
    mediaSession = MediaSession.Builder(this, player)
      .setCallback(createMediaSessionCallback())
      .build()
  }

  override fun onDestroy() {
    mediaSession?.run {
      player.release()
      release()
      mediaSession = null
    }
    super.onDestroy()
  }


  private fun createMediaSessionCallback(): MediaSession.Callback {
    return object : MediaSession.Callback {

    }
  }

  private fun monitorPlayer(player: ExoPlayer): MuxStatsSdkMedia3? {
    return null // TODO: not null
  }

  private fun createPlayer(): ExoPlayer {
    return ExoPlayer.Builder(this).build()
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
    return mediaSession
  }

}