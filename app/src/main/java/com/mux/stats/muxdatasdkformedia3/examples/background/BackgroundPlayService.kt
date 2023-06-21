package com.mux.stats.muxdatasdkformedia3.examples.background

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.mux.stats.muxdatasdkformedia3.Constants
import com.mux.stats.sdk.muxstats.MuxStatsSdkMedia3

class BackgroundPlayService : MediaSessionService() {

  private var mediaSession: MediaSession? = null
  private val player: ExoPlayer? get() = mediaSession?.player as? ExoPlayer
  private var muxStats: MuxStatsSdkMedia3<ExoPlayer>? = null

  override fun onCreate() {
    super.onCreate()
    val player = createPlayer()
    muxStats = monitorPlayer(player)
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
        val resolvedMediaItems = mediaItems
          .map { item ->
            val playableVideo = findUrl(item.mediaId)
            val mediaMetadata = playableVideo?.let {
              MediaMetadata.Builder()
                .setTitle(it.title)
                .setDisplayTitle(it.title)
                .build()
            } ?: MediaMetadata.EMPTY
            item.buildUpon()
              .setUri(playableVideo?.url)
              .setMediaMetadata(mediaMetadata)
              .build()
          }
          .toMutableList()
        player?.addMediaItems(resolvedMediaItems)
        return Futures.immediateFuture(resolvedMediaItems)
      } // onAddMediaItems

      override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
      ): ListenableFuture<SessionResult> {
        // TODO: Something here to parse these commands
        return super.onCustomCommand(session, controller, customCommand, args)
      }
    } // object : MediaSession.Callback
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
    return mediaSession
  }

  private fun findUrl(videoId: String): PlayableVideo? {
    return VIDEO_IDS.find { it.id == videoId }
  }

  private fun monitorPlayer(player: ExoPlayer): MuxStatsSdkMedia3<ExoPlayer>? {
    return null // TODO: not null
  }

  private fun createPlayer(): ExoPlayer {
    return ExoPlayer.Builder(this).build()
  }

  data class PlayableVideo(
    val id: String,
    val url: String,
    val title: String,
  )

  companion object {
    val VIDEO_IDS = listOf(
      PlayableVideo("bunny", Constants.VOD_TEST_URL_BIG_BUCK_BUNNY, "Big Buck Bunny"),
      PlayableVideo("steve", Constants.VOD_TEST_URL_STEVE, "Apple Keynote"),
      PlayableVideo("durian", Constants.VOD_TEST_URL_DRAGON_WARRIOR_LADY, "Durian Open Movie Project"),
    )
  }
}