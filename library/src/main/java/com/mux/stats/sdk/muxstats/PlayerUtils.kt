package com.mux.stats.sdk.muxstats

import android.util.Log
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import com.mux.android.util.oneOf
import com.mux.stats.sdk.core.util.MuxLogger

internal const val PLAYER_STATE_POLL_MS = 150L
private const val LOG_TAG = "PlayerUtils"

/**
 * Asynchronously watch player playback position, collecting periodic updates out-of-band from the
 * normal callback flow.
 */
fun MuxStateCollector.watchPlayerPos(player: Player) {
  playerWatcher = MuxStateCollector.PlayerWatcher(
    PLAYER_STATE_POLL_MS,
    this,
    player
  ) { it, _ -> it.currentPosition }
  playerWatcher?.start()
}

/**
 * Returns true if any media track in the given [Tracks] object had a video MIME type
 */
fun Tracks.hasAtLeastOneVideoTrack(): Boolean {
  return groups.map { it.mediaTrackGroup }
    .filter { trackGroup -> trackGroup.length > 0 }
    .map { trackGroup -> trackGroup.getFormat(0) }
    .find { format -> format.sampleMimeType?.contains("video") ?: false }
    .let { foundVideoTrack -> foundVideoTrack != null }
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
 * Handles a change of basic ExoPlayer state
 */
@JvmSynthetic // Hidden from Java callers, since the only ones are external
fun MuxStateCollector.handleExoPlaybackState(
  playbackState: Int, // the @IntDef for player state omitted. Unavailable on all exo versions
  playWhenReady: Boolean
) {
  if (this.muxPlayerState == MuxPlayerState.PLAYING_ADS) {
    // Normal playback events are ignored during ad playback
    return
  }

  when (playbackState) {
    Player.STATE_BUFFERING -> {
      MuxLogger.d(LOG_TAG, "entering BUFFERING")
      buffering()
    }
    Player.STATE_READY -> {
      MuxLogger.d(LOG_TAG, "entering READY")

      // We're done seeking after we get back to STATE_READY
      if(muxPlayerState == MuxPlayerState.SEEKING) {
        // TODO <em> playing() and pause() handle rebuffering, why not also seeking
        seeked(false)
      }

      // If playWhenReady && READY, we're playing or else we're paused
      if (playWhenReady) {
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
