package com.mux.stats.sdk.muxstats.internal

import androidx.media3.common.Player
import androidx.media3.common.Tracks
import com.mux.android.util.oneOf
import com.mux.stats.sdk.muxstats.MuxPlayerState
import com.mux.stats.sdk.muxstats.MuxStateCollector
import java.lang.ref.WeakReference

internal const val PLAYER_STATE_POLL_MS = 150L

internal fun watchPlayerPos(player: WeakReference<Player>, collector: MuxStateCollector) {
  collector.playerWatcher = MuxStateCollector.PlayerWatcher(
    PLAYER_STATE_POLL_MS,
    collector,
    player
  ) { it, _ -> it.currentPosition }
  collector.playerWatcher?.start()
}

/**
 * Returns true if any media track in the given [Tracks] object had a video MIME type
 */
internal fun Tracks.hasAtLeastOneVideoTrack(): Boolean {
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
internal fun MuxStateCollector.handlePositionDiscontinuity(reason: Int) {
  when (reason) {
    Player.DISCONTINUITY_REASON_SEEK -> {
      // If they seek while paused, this is how we know the seek is complete
      if (muxPlayerState == MuxPlayerState.PAUSED
        // Seeks on audio-only media are reported this way instead
        || !mediaHasVideoTrack!!
      ) {
        seeked(false)
      }
    }
    else -> {} // ignored
  }
}

/**
 * Handles a change of basic ExoPlayer state
 */
@JvmSynthetic // Hidden from Java callers, since the only ones are external
internal fun MuxStateCollector.handleExoPlaybackState(
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
