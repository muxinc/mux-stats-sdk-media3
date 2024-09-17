package com.mux.stats.sdk.muxstats

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import com.mux.android.util.oneOf
import com.mux.stats.sdk.core.model.VideoData
import com.mux.stats.sdk.core.util.MuxLogger

internal const val PLAYER_STATE_POLL_MS = 150L
private const val LOG_TAG = "PlayerUtils"

/**
 * Returns true if any media track in the given [Tracks] object had a video MIME type
 */
@OptIn(UnstableApi::class)
fun Tracks.hasAtLeastOneVideoTrack(): Boolean {
  return groups.map { it.mediaTrackGroup }
    .filter { trackGroup -> trackGroup.length > 0 }
//    .map { trackGroup -> trackGroup.getFormat(0) }
    .flatMap {trackGroup -> trackGroup.iterateFormats() }
    .find { format -> format.sampleMimeType?.contains("video") ?: false }
    .let { foundVideoTrack -> foundVideoTrack != null }
}

@OptIn(UnstableApi::class)
fun TrackGroup.iterateFormats(): Iterable<Format> {
  val group = this
  return object: Iterable<Format> {
    override fun iterator(): Iterator<Format> {
      return object: Iterator<Format> {
        private var current: Int = 0
        override fun hasNext(): Boolean {
          return current < group.length
        }
        override fun next(): Format {
          val format = group.getFormat(current)
          current += 1;
          return format
        }
      }
    }
  }
}

/**
 * Maps the formats of the tracks in a [Tracks.Group] to some other type
 */
fun <R> Tracks.Group.mapFormats(block: (Format) -> R): List<R> {
  val retList = mutableListOf<R>()
  for (i in 0 until length) {
    retList.add(block(getTrackFormat(i)))
  }
  return retList
}

// Catches the Collector up to the current play state if the user registers after prepare()
@JvmSynthetic
fun catchUpPlayState(player: Player, collector: MuxStateCollector) {
  MuxLogger.d("PlayerUtils", "catchUpPlayState: Called. pwr is ${player.playWhenReady}")
  MuxLogger.d("PlayerUtils", "catchUpPlayState: Called. state is ${player.playbackState}")
  if (player.playWhenReady) {
    MuxLogger.d("PlayerUtils", "catchUpPlayState: dispatching play")
    // Captures auto-play & late-registration, setting state and sending 'viewstart'
    collector.play()
  }
  // The player will be idle when we are first attached, so we don't need to say we paused
  //  (which is how IDLE is handled during actual playback)
  if (player.playbackState != Player.STATE_IDLE) {
    collector.handleExoPlaybackState(player.playbackState, player.playWhenReady)
  }
}

@JvmSynthetic
fun catchUpStreamData(player: Player, collector: MuxStateCollector) {
  player.currentTimeline.takeIf { it.windowCount > 0 }?.let { tl ->
    val window = Timeline.Window().apply { tl.getWindow(0, this) }
    collector.sourceDurationMs = window.durationMs
  }
  @Suppress("UNNECESSARY_SAFE_CALL")
  player.videoSize?.let {
    collector.sourceWidth = it.width
    collector.sourceHeight = it.height
  }
  player.currentMediaItem?.let { collector.handleMediaItemChanged(it) }
}

/**
 * Handles an ExoPlayer position discontinuity
 */
@JvmSynthetic // Hides from java
fun MuxStateCollector.handlePositionDiscontinuity(reason: Int) {
  when (reason) {
    Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT, Player.DISCONTINUITY_REASON_SEEK -> {
      // Called when seeking starts. Player will move to READY when seeking is over
      seeking()
    }

    else -> {} // ignored
  }
}

/**
 * Handles changes to playWhenReady.
 */
@JvmSynthetic
fun MuxStateCollector.handlePlayWhenReady(
  playWhenReady: Boolean,
  @Player.State playbackState: Int
) {
  MuxLogger.d("PlayerUtils", "handlePlayWhenReady: Called. pwr is $playWhenReady")
  if (playWhenReady) {
    MuxLogger.d("PlayerUtils", "handlePlayWhenReady: dispatching play")
    play()
    if (playbackState == Player.STATE_READY) {
      MuxLogger.d("PlayerUtils", "handlePlayWhenReady: dispatching playing")
      // If we were already READY when playWhenReady is set, then we are definitely also playing
      playing()
    }
  } else if (muxPlayerState != MuxPlayerState.PAUSED) {
    pause()
  }
}

/**
 * Asynchronously watch player playback position, collecting periodic updates out-of-band from the
 * normal callback flow.
 */
@JvmSynthetic
fun MuxStateCollector.watchPlayerPos(player: Player) {
  playerWatcher = MuxStateCollector.PlayerWatcher(
    PLAYER_STATE_POLL_MS,
    this,
    player
  ) { it, _ -> it.currentPosition }
  playerWatcher?.start()
}

/**
 * Handles a change of basic ExoPlayer state
 */
@JvmSynthetic // Hidden from Java callers, since the only ones are external
fun MuxStateCollector.handleExoPlaybackState(
  @Player.State playbackState: Int,
  playWhenReady: Boolean
) {
  if (this.muxPlayerState == MuxPlayerState.PLAYING_ADS) {
    // Normal playback events are ignored during ad playback.
    return
  }

  when (playbackState) {
    Player.STATE_BUFFERING -> {
      MuxLogger.d(LOG_TAG, "entering BUFFERING")
      MuxLogger.d(LOG_TAG, "muxPlayerState is $muxPlayerState")
      buffering()
    }

    Player.STATE_READY -> {
      MuxLogger.d(LOG_TAG, "entering READY")

      // We're done seeking after we get back to STATE_READY
      if (muxPlayerState == MuxPlayerState.SEEKING) {
        // TODO <em> playing() and pause() handle rebuffering, why not also seeking
        seeked()
      }

      // If playWhenReady && READY, we're playing or else we're paused
      if (playWhenReady) {
        MuxLogger.d(LOG_TAG, "entered READY && pwr is true, dispatching playing()")
        playing()
      } else if (muxPlayerState != MuxPlayerState.PAUSED) {
        pause()
      }
    }

    Player.STATE_ENDED -> {
      MuxLogger.d(LOG_TAG, "entering ENDED")
      ended()
    }

    Player.STATE_IDLE -> {
      MuxLogger.d(LOG_TAG, "entering IDLE")
      if (muxPlayerState.oneOf(MuxPlayerState.PLAY, MuxPlayerState.PLAYING)) {
        // If we are playing/preparing to play and go idle, the player was stopped
        pause()
      }
    }
  } // when (playbackState)
} // fun handleExoPlaybackState

@JvmSynthetic
fun MuxStateCollector.handleMediaItemChanged(mediaItem: MediaItem) {
  mediaItem.localConfiguration?.let { localConfig ->
    val sourceUrl = localConfig.uri;
    val sourceDomain = sourceUrl.authority
    val videoData = VideoData().apply {
      videoSourceDomain = sourceDomain
      videoSourceUrl = sourceUrl.toString()
    }
    videoDataChange(videoData)
  }

  // Also pick up data from MediaMetadata
  handleMediaMetadata(mediaItem.mediaMetadata)
}

@JvmSynthetic
fun MuxStateCollector.handleMediaMetadata(mediaMetadata: MediaMetadata) {
  // explicitly make those ! into ?
  val posterUrl: Uri? = mediaMetadata.artworkUri
  val title: CharSequence? = mediaMetadata.title

  val videoData = VideoData().apply {
    posterUrl?.let { videoPosterUrl = it.toString() }
    title?.let { videoTitle = it.toString() }
  }
  videoDataChange(videoData)
}
