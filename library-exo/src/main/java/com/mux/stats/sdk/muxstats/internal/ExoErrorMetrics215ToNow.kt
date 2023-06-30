package com.mux.stats.sdk.muxstats.internal

import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import com.mux.android.util.weak
import com.mux.stats.sdk.core.util.MuxLogger
import com.mux.stats.sdk.muxstats.MuxErrorException
import com.mux.stats.sdk.muxstats.MuxPlayerAdapter
import com.mux.stats.sdk.muxstats.MuxStateCollector

/**
 * Player binding for  exoplayer android metrics
 * This implementation works from 2.15.1 up until now (as of 4/25/2022)
 */
private class ExoErrorMetricsByListener215ToNow : MuxPlayerAdapter.PlayerBinding<ExoPlayer> {
  private var playerListener: Player.Listener? by weak(null)

  override fun bindPlayer(player: ExoPlayer, collector: MuxStateCollector) {
    playerListener = newListener(collector).also { player.addListener(it) }
  }

  override fun unbindPlayer(player: ExoPlayer, collector: MuxStateCollector) {
    collector.playerWatcher?.stop("player unbound")
    collector.playerWatcher= null
    playerListener?.let { player.removeListener(it) }
  }

  private fun newListener(collector: MuxStateCollector) = ErrorPlayerListenerUpTo214(collector)
} // class ErrorPlayerBuListenerUpTo214

private class ErrorPlayerListenerUpTo214(val collector: MuxStateCollector) : Player.Listener {
  override fun onPlayerError(error: PlaybackException) {
    if (error is ExoPlaybackException) {
      collector.handleExoPlaybackException(error.errorCode, error)
    } else {
      val errorMessage = "${error.errorCode}: ${error.message}"
      collector.internalError(MuxErrorException(error.errorCode, errorMessage))
    }
  }
}

/**
 * Generates a player binding for exoplayer error metrics.
 */
@JvmSynthetic
internal fun playerErrorMetrics(): MuxPlayerAdapter.PlayerBinding<ExoPlayer> =
  ExoErrorMetricsByListener215ToNow();

/**
 * Handles fatal [ExoPlaybackException]s from the player, reporting them to the dashboard
 * The error code parameter is required, because different versions of exoplayer offer different
 * granularities in error reporting
 *
 * @param errorCode An error code for this exception. The best is [PlaybackException.errorCode]
 * @param e The Exception thrown. The error code will be overidden with the value of [errorCode]
 */
@OptIn(UnstableApi::class)
@JvmSynthetic
internal fun MuxStateCollector.handleExoPlaybackException(errorCode: Int, e: ExoPlaybackException) {
  if (e.type == ExoPlaybackException.TYPE_RENDERER) {
    val rendererEx = e.rendererException
    // Decoder Init errors are given special messages
    if (rendererEx is MediaCodecRenderer.DecoderInitializationException) {
      if (rendererEx.cause is MediaCodecUtil.DecoderQueryException) {
        internalError(MuxErrorException(errorCode, "Unable to query device decoders"))
      } else if (rendererEx.secureDecoderRequired) {
        internalError(
          MuxErrorException(
            errorCode,
            "No secure decoder for " + rendererEx.mimeType,
            rendererEx.diagnosticInfo
          )
        )
      } else {
        internalError(
          MuxErrorException(
            errorCode,
            "No decoder for " + rendererEx.mimeType,
            rendererEx.diagnosticInfo
          )
        )
      }
    } else {
      // Not a DecoderInitializationException
      internalError(
        MuxErrorException(
          errorCode,
          "${rendererEx.javaClass.canonicalName} - ${rendererEx.message}",
        )
      )
    } // if(rendererEx is..)...else
  } else if (e.type == ExoPlaybackException.TYPE_SOURCE) {
    val error: Exception = e.sourceException
    internalError(
      MuxErrorException(
        errorCode,
        "${error.javaClass.canonicalName} - ${error.message}"
      )
    )
  } else if (e.type == ExoPlaybackException.TYPE_UNEXPECTED) {
    val error: Exception = e.unexpectedException
    internalError(
      MuxErrorException(
        errorCode,
        "${error.javaClass.canonicalName} - ${error.message}"
      )
    )
  } else {
    internalError(MuxErrorException(errorCode, "${e.javaClass.canonicalName} - ${e.message}"))
  }
}
