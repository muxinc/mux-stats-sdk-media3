package com.mux.stats.sdk.muxstats.bandwidth

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.common.Tracks.Group
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import com.mux.android.util.weak
import com.mux.stats.sdk.core.events.playback.PlaybackEvent
import com.mux.stats.sdk.core.events.playback.RequestCanceled
import com.mux.stats.sdk.core.events.playback.RequestCompleted
import com.mux.stats.sdk.core.events.playback.RequestFailed
import com.mux.stats.sdk.core.model.BandwidthMetricData
import com.mux.stats.sdk.core.model.BandwidthMetricData.Rendition
import com.mux.stats.sdk.core.util.MuxLogger
import com.mux.stats.sdk.muxstats.MuxStateCollector
import com.mux.stats.sdk.muxstats.mapFormats
import java.io.IOException
import java.util.*
import java.util.regex.Pattern

/**
 * Calculate Bandwidth metrics of an HLS or DASH segment. {@link ExoPlayer} will trigger
 * these events in {@link MuxStatsExoPlayer} and will be propagated here for processing, at this
 * point both HLS and DASH segments are processed in same way so all metrics are collected here.
 */
internal open class BandwidthMetrics(
  player: ExoPlayer,
  private val collector: MuxStateCollector
) {
  /** Available qualities. */
  var availableTracks: List<Group>? = null

  private val player by weak(player)

  /**
   * Each segment that started loading is stored here until the segment ceases loading.
   * The segment url is the key value of the map.
   */
  private var loadedSegments = HashMap<Long, BandwidthMetricData>()

  private var currentTimelineWindow = Timeline.Window()

  /**
   * When the segment failed to load an error will be reported to the backend. This also
   * removes the segment that failed to load from the {@link #loadedSegments} hash map.
   *
   * @param loadTaskId, unique segment id.
   * @param e, error that occurred.
   * @return segment that failed to load.
   */
  open fun onLoadError(loadTaskId: Long, e: IOException): BandwidthMetricData {
    var segmentData: BandwidthMetricData? = loadedSegments[loadTaskId]
    if (segmentData == null) {
      segmentData = BandwidthMetricData()
    }
    segmentData.requestError = e.toString()
    System.out.println("for url ${segmentData.requestUrl}: Request Error! $e")
    if (e is HttpDataSource.InvalidResponseCodeException) {
      segmentData.requestErrorCode = e.responseCode
      segmentData.requestErrorText = e.responseMessage
    } else {
      segmentData.requestErrorCode = -1
      segmentData.requestErrorText = e.message
    }
    segmentData.requestResponseEnd = System.currentTimeMillis()
    return segmentData
  }

  /**
   * If the segment is no longer needed this function will be triggered. This can happen if
   * the player stopped the playback and wants to stop all network loading. In that case we will
   * remove the appropriate segment from {@link #loadedSegments}.
   *
   * @param loadTaskId, unique id that represent the loaded segment.
   * @return Canceled segment.
   */
  open fun onLoadCanceled(loadTaskId: Long): BandwidthMetricData {
    var segmentData: BandwidthMetricData? = loadedSegments[loadTaskId]
    if (segmentData == null) {
      segmentData = BandwidthMetricData()
    }
    segmentData.requestCancel = "genericLoadCanceled"
    segmentData.requestResponseEnd = System.currentTimeMillis()
    return segmentData
  }

  @OptIn(UnstableApi::class) // opting-in to the bitrate api
  open fun onLoad(
    loadTaskId: Long, loadStartTimeMs: Long, mediaStartTimeMs: Long, mediaEndTimeMs: Long,
    segmentUrl: String?, dataType: Int, trackType: Int, host: String?, segmentMimeType: String?,
    segmentWidth: Int, segmentHeight: Int
  ): BandwidthMetricData {
    // Populate segment time details.
    synchronized(currentTimelineWindow) {
      player?.let { safePlayer ->
        try {
          safePlayer.currentTimeline
            .getWindow(safePlayer.currentWindowIndex, currentTimelineWindow)
        } catch (e: Exception) {
          // Failed to obtain data, ignore, we will get it on next call
          MuxLogger.exception(e, "BandwidthMetrics", "Failed to get current timeline")
        }
      }
    }
    val segmentData = BandwidthMetricData()
    segmentData.requestStart = loadStartTimeMs
    // request_response_start not available
//    segmentData.requestResponseStart = System.currentTimeMillis()
    segmentData.requestMediaStartTime = mediaStartTimeMs
    if (segmentWidth != 0 && segmentHeight != 0) {
      segmentData.requestVideoWidth = segmentWidth
      segmentData.requestVideoHeight = segmentHeight
    } else {
      segmentData.requestVideoWidth = collector.sourceWidth
      segmentData.requestVideoHeight = collector.sourceHeight
    }
    segmentData.requestUrl = segmentUrl

    if (dataType == C.DATA_TYPE_MANIFEST) {
      // Overall MIME type only applicable to progressive files
      collector.detectMimeType = false
    }

    fillRequestType(segmentData, dataType, trackType, mediaEndTimeMs, mediaStartTimeMs)
    MuxLogger.d("BandwidthMetrics", "onLoad: For request: ${segmentUrl}:"
        + "\nRequest type: ${segmentData.requestType}"
        + "\nMedia duration: ${segmentData.requestMediaDuration}")

    // headers will be picked up when we get onLoadCompleted
    segmentData.requestResponseHeaders = null
    segmentData.requestHostName = host
    segmentData.requestRenditionLists = collector.renditionList
    loadedSegments[loadTaskId] = segmentData
    return segmentData
  }

  @OptIn(UnstableApi::class)
  private fun fillRequestType(
    segmentData: BandwidthMetricData,
    dataType: Int,
    trackType: Int,
    mediaEndTimeMs: Long,
    mediaStartTimeMs: Long
  ) {
    when (dataType) {
      C.DATA_TYPE_MANIFEST -> segmentData.requestType = "manifest"
      C.DATA_TYPE_DRM -> segmentData.requestType = "encryption"
      C.DATA_TYPE_MEDIA_INITIALIZATION -> {
        when (trackType) {
          C.TRACK_TYPE_VIDEO, C.TRACK_TYPE_DEFAULT -> segmentData.requestType = "video_init"
          C.TRACK_TYPE_AUDIO -> segmentData.requestType = "audio_init"
        }
      }
      C.DATA_TYPE_MEDIA -> {
        segmentData.requestMediaDuration = mediaEndTimeMs - mediaStartTimeMs
        when (trackType) {
          // cmaf or plain hls with a video track
          C.TRACK_TYPE_DEFAULT -> segmentData.requestType = "media"
          // dash or cmaf hls audio segments, audio-only hls or dash
          C.TRACK_TYPE_AUDIO -> segmentData.requestType = "audio"
          // dash video
          C.TRACK_TYPE_VIDEO -> segmentData.requestType = "video"
          C.TRACK_TYPE_TEXT -> segmentData.requestType = "subtitle"
        }
      }
    }
  }

  /**
   * Called when a segment starts loading. Set appropriate metrics and store the segment in
   * {@link #loadedSegments}. It will be then sent to the backend once the appropriate one of
   * {@link #onLoadCompleted(Long, String, long, Format)} ,{@link #onLoadError(Long, IOException)}
   * or {@link #onLoadCanceled(Long)}  is called for this segment.
   *
   * @param mediaStartTimeMs, {@link ExoPlayer} reported segment playback start time, this refer
   *                                           to playback position of segment inside the media
   *                                           presentation (DASH or HLS stream).
   * @param mediaEndTimeMs, {@link ExoPlayer} reported playback end time, this refer to playback
   *                                         position of segment inside the media presentation
   *                                         (DASH or HLS stream).
   * @param segmentUrl, url of the segment that is being loaded, used as a unique id for segment
   *                    storage in {@link #loadedSegments} table.
   * @param dataType, type of the segment (manifest, media etc ...)
   * @param host, host associated with this segment.
   * @return new segment.
   */
  open fun onLoadStarted(
    loadTaskId: Long, loadStartTimeMs: Long, mediaStartTimeMs: Long, mediaEndTimeMs: Long,
    segmentUrl: String?, dataType: Int, trackType: Int, host: String?, segmentMimeType: String?,
    segmentWidth: Int, segmentHeight: Int
  ) : BandwidthMetricData {
    val loadData = onLoad(
      loadTaskId = loadTaskId,
      loadStartTimeMs = loadStartTimeMs,
      mediaStartTimeMs = mediaStartTimeMs,
      mediaEndTimeMs = mediaEndTimeMs,
      segmentUrl = segmentUrl,
      dataType = dataType,
      trackType = trackType,
      host = host,
      segmentMimeType = segmentMimeType,
      segmentWidth = segmentWidth,
      segmentHeight = segmentHeight
    )
    return loadData
  }

  /**
   * Called when segment is loaded. This function will retrieve the statistics for this segment
   * from {@link #loadedSegments} and fill out the remaining metrics.
   *
   * @param loadTaskId unique id representing the current segment.
   * @param segmentUrl url related to the segment.
   * @param bytesLoaded number of bytes needed to load the segment.
   * @param trackFormat Media details related to the segment.
   * @return loaded segment.
   */
  @OptIn(UnstableApi::class) // Opting-in to the bitrate apis
  open fun onLoadCompleted(
    loadTaskId: Long,
    segmentUrl: String?,
    bytesLoaded: Long,
    trackFormat: Format?
  ): BandwidthMetricData? {
    val segmentData: BandwidthMetricData = loadedSegments[loadTaskId] ?: return null

    segmentData.requestBytesLoaded = bytesLoaded
    segmentData.requestResponseEnd = System.currentTimeMillis()
    val availableVideoTrackGroups = availableTracks
    if (trackFormat != null && availableVideoTrackGroups != null) {
      availableVideoTrackGroups.onEach { group ->
        for (trackGroupIndex in 0 until group.length) {
          val currentFormat: Format = group.getTrackFormat(trackGroupIndex)
          if (trackFormat.width == currentFormat.width
            && trackFormat.height == currentFormat.height
            && trackFormat.bitrate == currentFormat.bitrate
          ) {
            segmentData.requestCurrentLevel = trackGroupIndex
            MuxLogger.d(
              "BandwidthMetrics", "onLoadCompleted: found rendition idx $trackGroupIndex"
                      + "\nwith format $currentFormat"
            )
          }
        }
      }
    }
    loadedSegments.remove(loadTaskId)
    return segmentData
  }
}

internal class BandwidthMetricsHls(
  player: ExoPlayer,
  collector: MuxStateCollector
) : BandwidthMetrics(player, collector) {

  override fun onLoadCanceled(loadTaskId: Long): BandwidthMetricData {
    val loadData: BandwidthMetricData = super.onLoadCanceled(loadTaskId)
    loadData.requestCancel = "FragLoadEmergencyAborted"
    return loadData
  }

  @OptIn(UnstableApi::class) // Opting-in to the bitrate APIs
  override fun onLoadCompleted(
    loadTaskId: Long, segmentUrl: String?, bytesLoaded: Long,
    trackFormat: Format?
  ): BandwidthMetricData? {
    val loadData: BandwidthMetricData? =
      super.onLoadCompleted(loadTaskId, segmentUrl, bytesLoaded, trackFormat)
    if (trackFormat != null && loadData != null) {
      if (trackFormat.bitrate > 0) {
        MuxLogger.d(
          "BandwidthMetrics",
          "onLoadCompleted: current track bitrate " + trackFormat.bitrate
        )
        loadData.requestLabeledBitrate = trackFormat.bitrate
      }
    }
    return loadData
  }
}

/**
 * Determine which stream is being parsed (HLS or DASH) and then use an appropriate
 * {@link BandwidthMetric} to parse the stream. The problem with this is that it is not possible
 * to reliably detect the stream type currently being played so this part is not functional.
 * Luckily logic for HLS parsing is same as logic for DASH parsing so for both streams we use
 * {@link BandwidthMetricHls}.
 */
internal class BandwidthMetricDispatcher(
  player: ExoPlayer,
  collector: MuxStateCollector,
  private val trackedResponseHeaders: List<TrackedHeader> = listOf()
) {
  private val player: ExoPlayer? by weak(player)
  private val collector: MuxStateCollector? by weak(collector)
  private val bandwidthMetricHls: BandwidthMetricsHls = BandwidthMetricsHls(player, collector)
  private var debugModeOn: Boolean = false
  private var requestSegmentDuration: Long = 1000
  private var lastRequestSentAt: Long = -1
  private var maxNumberOfEventsPerSegmentDuration: Int = 10
  private var numberOfRequestCompletedBeaconsSentPerSegment: Int = 0
  private var numberOfRequestCancelBeaconsSentPerSegment: Int = 0
  private var numberOfRequestFailedBeaconsSentPerSegment: Int = 0

  private fun currentBandwidthMetric(): BandwidthMetricsHls {
    // in the future if bandwidth metrics for dash required a different logic we will implement
    //   it here
    return bandwidthMetricHls
  }

  @Suppress("UNUSED_PARAMETER")
  fun onLoadError(loadTaskId: Long, segmentUrl: String?, e: IOException) {
    if (player == null || collector == null) {
      return
    }
    val loadData: BandwidthMetricData = currentBandwidthMetric().onLoadError(loadTaskId, e)
    dispatch(data = loadData, event = RequestFailed(null))
  }

  @Suppress("UNUSED_PARAMETER")
  fun onLoadCanceled(loadTaskId: Long, segmentUrl: String?, headers: Map<String, List<String>>) {
    if (player == null || collector == null) {
      return
    }
    val loadData: BandwidthMetricData = currentBandwidthMetric().onLoadCanceled(loadTaskId)
    parseHeaders(loadData, headers)
    dispatch(loadData, RequestCanceled(null))
  }

  fun onLoadStarted(
    loadTaskId: Long, loadStartTimeMs: Long, mediaStartTimeMs: Long, mediaEndTimeMs: Long, segmentUrl: String?,
    dataType: Int, trackType: Int, host: String?, segmentMimeType: String?,
    segmentWidth: Int, segmentHeight: Int
  ) {
    if (player == null || collector == null) {
      return
    }
    currentBandwidthMetric().onLoadStarted(
      loadTaskId,
      loadStartTimeMs,
      mediaStartTimeMs,
      mediaEndTimeMs,
      segmentUrl,
      dataType,
      trackType,
      host,
      segmentMimeType,
      segmentWidth,
      segmentHeight
    )
  }

  fun onLoadCompleted(
    loadTaskId: Long, segmentUrl: String?, bytesLoaded: Long, trackFormat: Format?,
    responseHeaders: Map<String, List<String>>
  ) {
    if (player == null || collector == null) {
      return
    }
    val loadData: BandwidthMetricData? = currentBandwidthMetric().onLoadCompleted(
      loadTaskId, segmentUrl, bytesLoaded, trackFormat
    )
    if (loadData != null) {
      parseHeaders(loadData, responseHeaders)
      dispatch(loadData, RequestCompleted(null))
    }
  }

  private fun parseHeaders(
    loadData: BandwidthMetricData,
    responseHeaders: Map<String, List<String>>
  ) {
    val headers: Hashtable<String, String>? = parseHeaders(responseHeaders)
    if (headers != null) {
      loadData.requestId = headers["x-request-id"]
      loadData.requestResponseHeaders = headers
    }
  }

  /**
   * Call when the player's track list changes, so we can report rendition lists on request events
   */
  @OptIn(UnstableApi::class) // Opting-in to the Bitrate APIs
  fun onTracksChanged(tracks: Tracks) {
    MuxLogger.d("BandwidthMetrics", "onTracksChanged: Got ${tracks.groups.size} tracks")
    currentBandwidthMetric().availableTracks =
      tracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }
    if (player == null || collector == null) {
      return
    }
    val renditions = tracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }
      .flatMap {
        it.mapFormats { trackFormat ->
          Rendition().apply {
            bitrate = trackFormat.bitrate.toLong()
            width = trackFormat.width
            height = trackFormat.height
            codec = trackFormat.codecs
            fps = trackFormat.frameRate
            name = trackFormat.width.toString() + "_" +
                    trackFormat.height + "_" +
                    trackFormat.bitrate + "_" + trackFormat.codecs + "_" +
                    trackFormat.frameRate
          }
        }
      }
    collector?.renditionList = renditions
    MuxLogger.d(
      "BandwidthMetrics",
      "onTracksChanged: ended function with renditions: " +
              collector?.renditionList?.map { it.debugString() }
    )
  }

  private fun BandwidthMetricData.Rendition.debugString(): String {
    return "{size: [${width}x$height], ${fps}fps, ${bitrate}bps, name: $name codec $codec}"
  }

  private fun dispatch(data: BandwidthMetricData, event: PlaybackEvent) {
    if (shouldDispatchEvent(data, event)) {
      event.bandwidthMetricData = data
      collector?.dispatcher?.dispatch(event)
    }
  }

  private fun parseHeaders(responseHeaders: Map<String, List<String>>): Hashtable<String, String>? {
    if (responseHeaders.isEmpty()) {
      return null
    }

    val headers: Hashtable<String, String> = Hashtable<String, String>()
    for (headerName in responseHeaders.keys) {
      var headerTracked = false
      synchronized(this) {
        for (trackedHeader in trackedResponseHeaders) {
          if (trackedHeader.matches(headerName)) {
            headerTracked = true
          }
        }
      }
      if (!headerTracked) {
        // Pass this header, we do not need it
        continue
      }

      val headerValues: List<String> = responseHeaders[headerName]!!
      if (headerValues.isEmpty()) {
        headers[headerName] = ""
      } else if (headerValues.size == 1) {
        headers[headerName] = headerValues[0]
      } else if (headerValues.size > 1) {
        // In the case that there is more than one header, we squash
        // it down to a single comma-separated value per RFC 2616
        // https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2
        var headerValue: String = headerValues[0]
        for (i in 1 until headerValues.size) {
          headerValue = headerValue + ", " + headerValues[i]
        }
        headers[headerName] = headerValue
      }
    }
    return headers
  }

  /**
   * Make sure we do not overflow backend with Request events in case we have a broken live stream
   * and player keeps loading manifest or some other short segment not really needed for playback.
   *
   * @param data, all statistics collected for this segment.
   * @param event, event to be dispatched.
   * @return true if number of request completed events do not exceed two request per media
   * segment duration.
   */
  private fun shouldDispatchEvent(data: BandwidthMetricData, event: PlaybackEvent): Boolean {
    requestSegmentDuration =
      if (data.requestMediaDuration == null || data.requestMediaDuration < 1000) {
        1000
      } else {
        data.requestMediaDuration
      }
    val timeDiff: Long = System.currentTimeMillis() - lastRequestSentAt
    if (timeDiff > requestSegmentDuration) {
      // Reset all stats
      lastRequestSentAt = System.currentTimeMillis()
      numberOfRequestCompletedBeaconsSentPerSegment = 0
      numberOfRequestCancelBeaconsSentPerSegment = 0
      numberOfRequestFailedBeaconsSentPerSegment = 0
    }
    if (event is RequestCompleted) {
      numberOfRequestCompletedBeaconsSentPerSegment++
    }
    if (event is RequestCanceled) {
      numberOfRequestCancelBeaconsSentPerSegment++
    }
    if (event is RequestFailed) {
      numberOfRequestFailedBeaconsSentPerSegment++
    }
    if (numberOfRequestCompletedBeaconsSentPerSegment > maxNumberOfEventsPerSegmentDuration
      || numberOfRequestCancelBeaconsSentPerSegment > maxNumberOfEventsPerSegmentDuration
      || numberOfRequestFailedBeaconsSentPerSegment > maxNumberOfEventsPerSegmentDuration
    ) {
      if (debugModeOn) {
        MuxLogger.d(
          "BandwidthMetrics", "Dropping event: " + event.type
                  + "\nnumberOfRequestCompletedBeaconsSentPerSegment: "
                  + numberOfRequestCompletedBeaconsSentPerSegment
                  + "\nnumberOfRequestCancelBeaconsSentPerSegment: "
                  + numberOfRequestCancelBeaconsSentPerSegment
                  + "\nnumberOfRequestFailedBeaconsSentPerSegment: "
                  + numberOfRequestFailedBeaconsSentPerSegment
                  + "\ntimeDiff: " + timeDiff
        )
      }
      return false
    }
    if (debugModeOn) {
      MuxLogger.d(
        "BandwidthMetrics", "All good: " + event.type
                + "\nnumberOfRequestCompletedBeaconsSentPerSegment: "
                + numberOfRequestCompletedBeaconsSentPerSegment
                + "\nnumberOfRequestCancelBeaconsSentPerSegment: "
                + numberOfRequestCancelBeaconsSentPerSegment
                + "\nnumberOfRequestFailedBeaconsSentPerSegment: "
                + numberOfRequestFailedBeaconsSentPerSegment
                + "\ntimeDiff: " + timeDiff
      )
    }
    return true
  } // fun shouldDispatchEvent
} // class BandwidthMetricDispatcher

internal sealed class TrackedHeader {

  abstract fun matches(headerName: String?): Boolean

  class ExactlyIgnoreCase(private val name: String) : TrackedHeader() {
    override fun matches(headerName: String?): Boolean {
      return headerName.contentEquals(name, true)
    }
  }

  class Matching(private val pattern: Pattern) : TrackedHeader() {
    override fun matches(headerName: String?): Boolean {
      return if (headerName != null) {
        val matcher = pattern.matcher(headerName)
        matcher.find()
      } else {
        false
      }
    }
  }
}
