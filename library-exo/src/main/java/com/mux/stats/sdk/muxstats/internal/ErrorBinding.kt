package com.mux.stats.sdk.muxstats.internal

import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.mediacodec.MediaCodecDecoderException
import androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import com.mux.android.util.weak
import com.mux.stats.sdk.muxstats.MuxErrorException
import com.mux.stats.sdk.muxstats.MuxPlayerAdapter
import com.mux.stats.sdk.muxstats.MuxStateCollector

/**
 * Player binding for  exoplayer android metrics
 * This implementation works from 2.15.1 up until now (as of 4/25/2022)
 */
private class ErrorBindings : MuxPlayerAdapter.PlayerBinding<ExoPlayer> {
  private var playerListener: Player.Listener? by weak(null)
  private var analyticsListener: AnalyticsListener? by weak(null)

  override fun bindPlayer(player: ExoPlayer, collector: MuxStateCollector) {
    playerListener = ErrorPlayerListener(collector).also { player.addListener(it) }
    analyticsListener = ErrorAnalyticsListener(collector).also { player.addAnalyticsListener(it) }
  }

  override fun unbindPlayer(player: ExoPlayer, collector: MuxStateCollector) {
    collector.playerWatcher?.stop("player unbound")
    collector.playerWatcher= null
    playerListener?.let { player.removeListener(it) }
    analyticsListener?.let { player.removeAnalyticsListener(it) }
  }
} // class ErrorPlayerBuListenerUpTo214

@OptIn(UnstableApi::class)
private class ErrorAnalyticsListener(val collector: MuxStateCollector): AnalyticsListener {

  override fun onVideoCodecError(
    eventTime: AnalyticsListener.EventTime,
    videoCodecError: java.lang.Exception
  ) {
    // for the case in question, we are expecting CodecException here, possibly recoverable.
    //  find this in MediaCodecRendered and its event listener impl in ExoPlayerImplInternal

    // We won't get anything on the main error listener until the exception is no longer recoverable
    //  at which point we'd get the non-recoverable CodecException here then the normal error
    //  callback would be called with MediaCodecDecoderException
  }

  override fun onAudioCodecError(
    eventTime: AnalyticsListener.EventTime,
    audioCodecError: java.lang.Exception
  ) {
  }
}

private class ErrorPlayerListener(val collector: MuxStateCollector) : Player.Listener {
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
internal fun createErrorDataBinding(): MuxPlayerAdapter.PlayerBinding<ExoPlayer> =
  ErrorBindings();

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
  when (e.type) {
    ExoPlaybackException.TYPE_RENDERER -> {
      when(val rendererEx = e.rendererException) {
        is MediaCodecRenderer.DecoderInitializationException -> {
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
        }
        is MediaCodecDecoderException -> {
          internalError(
            MuxErrorException(errorCode, "MediaCodec unavailable", rendererEx.diagnosticInfo)
          )
        }
        else -> {
          internalError(
            MuxErrorException(
              errorCode,
              "${rendererEx.javaClass.canonicalName} - ${rendererEx.message}",
            )
          )
        }
      }
    }
    ExoPlaybackException.TYPE_SOURCE -> {
      val error: Exception = e.sourceException
      internalError(
        MuxErrorException(
          errorCode,
          "${error.javaClass.canonicalName} - ${error.message}"
        )
      )
    }
    ExoPlaybackException.TYPE_UNEXPECTED -> {
      val error: Exception = e.unexpectedException
      internalError(
        MuxErrorException(
          errorCode,
          "${error.javaClass.canonicalName} - ${error.message}"
        )
      )
    }
    else -> {
      internalError(MuxErrorException(errorCode, "${e.javaClass.canonicalName} - ${e.message}"))
    }
  }
}
