package com.mux.stats.sdk.muxstats

import android.util.Log
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import com.mux.android.util.oneOf

internal const val PLAYER_STATE_POLL_MS = 150L

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
 * Handles an ExoPlayer position discontinuity
 */
@JvmSynthetic // Hides from java
fun MuxStateCollector.handlePositionDiscontinuity(reason: Int) {
  when (reason) {
    Player.DISCONTINUITY_REASON_SEEK -> {
      // If they seek while paused, this is how we know the seek is complete
      if (muxPlayerState == MuxPlayerState.PAUSED
        // Seeks on audio-only media are reported this way instead
        || !mediaHasVideoTrack!!
      ) {
        seeked(false)
      } else {
        // Video Case: A Seek Event started
        seeking()
      }
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
      buffering()
      if (playWhenReady) {
        play()
      } else if (muxPlayerState != MuxPlayerState.PAUSED) {
        pause()
      }
    }
    Player.STATE_READY -> {
      if (playWhenReady) {
        if(muxPlayerState == MuxPlayerState.SEEKING) {
          Log.d("STATE", "Was seeking, dispatch seeked")
          seeked(false)
        }
        playing()
      } else if (muxPlayerState != MuxPlayerState.PAUSED) {
        pause()
      }
    }
    Player.STATE_ENDED -> {
      ended()
    }
    Player.STATE_IDLE -> {
      if (muxPlayerState.oneOf(MuxPlayerState.PLAY, MuxPlayerState.PLAYING)) {
        // If we are playing/preparing to play and go idle, the player was stopped
        pause()
      }
    }
  } // when (playbackState)
} // fun handleExoPlaybackState