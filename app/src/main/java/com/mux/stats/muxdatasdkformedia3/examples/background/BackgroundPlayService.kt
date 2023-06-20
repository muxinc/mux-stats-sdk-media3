package com.mux.stats.muxdatasdkformedia3.examples.background

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.mux.stats.muxdatasdkformedia3.Constants
import com.mux.stats.sdk.muxstats.MuxStatsSdkMedia3

class BackgroundPlayService : MediaSessionService() {

  private var mediaSession: MediaSession? = null
  private val player: ExoPlayer? get() = mediaSession?.player as? ExoPlayer
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
      override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>
      ): ListenableFuture<MutableList<MediaItem>> {
        Log.d("BackgroundPlayExample", "Adding media items: $mediaItems")
        val resolvedMediaItems = mediaItems
          .map { item ->
            item.buildUpon()
              .setUri(findUrl(item.mediaId))
              .build()
          }
          .toMutableList()
        player?.addMediaItems(resolvedMediaItems)
        return Futures.immediateFuture(resolvedMediaItems)
      }
    }
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
    return mediaSession
  }

  private fun findUrl(videoId: String): String? {
    return VIDEO_IDS.find { it.id == videoId }?.url
  }

  private fun monitorPlayer(player: ExoPlayer): MuxStatsSdkMedia3? {
    return null // TODO: not null
  }

  private fun createPlayer(): ExoPlayer {
    return ExoPlayer.Builder(this).build()
  }

  data class PlayableVideo(
    val id: String,
    val url: String
  )

  companion object {
    val VIDEO_IDS = listOf(
      PlayableVideo("steve", Constants.VOD_TEST_URL_STEVE),
      PlayableVideo("durian", Constants.VOD_TEST_URL_DRAGON_WARRIOR_LADY),
    )
  }
}
