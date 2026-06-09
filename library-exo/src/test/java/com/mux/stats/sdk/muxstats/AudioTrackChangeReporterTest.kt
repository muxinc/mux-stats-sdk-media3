package com.mux.stats.sdk.muxstats

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class AudioTrackChangeReporterTest {

  @Test
  fun `selected audio track reports the audio portion of a codecs string`() {
    val tracks = tracksOf(
      audioGroup(
        format = audioFormat(
          codecs = "avc1.640028,mp4a.40.2",
          label = "English",
          language = "en-US",
          bitrate = 128_000,
          channelCount = 2,
        ),
        selected = true,
      )
    )

    val state = tracks.toAudioTrackState()

    assertEquals(
      AudioTrackState(
        enabled = true,
        codec = "mp4a.40.2",
        name = "English",
        language = "en-us",
        bitrate = "128000",
        channels = "stereo",
      ),
      state
    )
  }

  @Test
  fun `single audio codec is reported as-is`() {
    val state = tracksOf(audioGroup(audioFormat(codecs = "ec-3"), selected = true))
      .toAudioTrackState()

    assertEquals("ec-3", state.codec)
  }

  @Test
  fun `known channel counts are mapped to channel configurations`() {
    assertEquals("mono", channelsFor(channelCount = 1))
    assertEquals("stereo", channelsFor(channelCount = 2))
    assertEquals("5.1", channelsFor(channelCount = 6))
    assertEquals("7.1", channelsFor(channelCount = 8))
  }

  @Test
  fun `unknown channel counts fall back to the raw count`() {
    assertEquals("3", channelsFor(channelCount = 3))
  }

  private fun channelsFor(channelCount: Int): String? = tracksOf(
    audioGroup(audioFormat(codecs = "mp4a.40.2", channelCount = channelCount), selected = true)
  ).toAudioTrackState().channels

  @Test
  fun `missing audio selection normalizes to disabled with cleared metadata`() {
    val tracks = tracksOf(
      audioGroup(
        format = audioFormat(codecs = "mp4a.40.2", label = "English", language = "en"),
        selected = false,
      )
    )

    val state = tracks.toAudioTrackState()

    assertEquals(AudioTrackState(enabled = false), state)
  }

  @Test
  fun `unidentifiable codec and non-meaningful fields are omitted`() {
    val tracks = tracksOf(
      audioGroup(
        format = audioFormat(codecs = null, label = " ", language = "und"),
        selected = true,
      )
    )

    val state = tracks.toAudioTrackState()

    assertEquals(true, state.enabled)
    assertNull(state.codec)
    assertNull(state.name)
    assertNull(state.language)
    assertNull(state.bitrate)
    assertNull(state.channels)
  }

  @Test
  fun `reporter dispatches initial state, dedupes repeats, and reports disable`() {
    val reportedStates = mutableListOf<AudioTrackState>()
    val reporter = AudioTrackChangeReporter { reportedStates += it }
    val selectedTracks = tracksOf(
      audioGroup(
        format = audioFormat(codecs = "mp4a.40.2", label = "English", language = "en"),
        selected = true,
      )
    )
    val disabledTracks = tracksOf(
      audioGroup(
        format = audioFormat(codecs = "mp4a.40.2", label = "English", language = "en"),
        selected = false,
      )
    )

    reporter.reportTracksChanged(selectedTracks)
    reporter.reportTracksChanged(selectedTracks)
    reporter.reportTracksChanged(disabledTracks)

    assertEquals(2, reportedStates.size)
    assertEquals(
      AudioTrackState(
        enabled = true,
        codec = "mp4a.40.2",
        name = "English",
        language = "en",
      ),
      reportedStates[0]
    )
    assertEquals(AudioTrackState(enabled = false), reportedStates[1])
  }

  @Test
  fun `reporter reset allows same state to be emitted for a new view`() {
    val reportedStates = mutableListOf<AudioTrackState>()
    val reporter = AudioTrackChangeReporter { reportedStates += it }
    val selectedTracks = tracksOf(
      audioGroup(
        format = audioFormat(codecs = "mp4a.40.2", label = "English", language = "en"),
        selected = true,
      )
    )

    reporter.reportTracksChanged(selectedTracks)
    reporter.reset()
    reporter.reportTracksChanged(selectedTracks)

    assertEquals(2, reportedStates.size)
    assertEquals(reportedStates[0], reportedStates[1])
  }

  private fun tracksOf(vararg groups: Tracks.Group): Tracks = Tracks(groups.toList())

  private fun audioGroup(format: Format, selected: Boolean): Tracks.Group = Tracks.Group(
    TrackGroup(format),
    false,
    intArrayOf(C.FORMAT_HANDLED),
    booleanArrayOf(selected),
  )

  private fun audioFormat(
    codecs: String?,
    label: String? = null,
    language: String? = null,
    bitrate: Int = Format.NO_VALUE,
    channelCount: Int = Format.NO_VALUE,
  ): Format = Format.Builder()
    .setSampleMimeType(MimeTypesShim.AUDIO_AAC)
    .setCodecs(codecs)
    .setLabel(label)
    .setLanguage(language)
    .setAverageBitrate(bitrate)
    .setChannelCount(channelCount)
    .build()

  // Keeps the test independent of the @UnstableApi MimeTypes constants.
  private object MimeTypesShim {
    const val AUDIO_AAC = "audio/mp4a-latm"
  }
}
