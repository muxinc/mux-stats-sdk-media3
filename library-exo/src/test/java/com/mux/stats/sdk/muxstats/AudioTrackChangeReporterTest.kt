package com.mux.stats.sdk.muxstats

import androidx.media3.common.Format
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
  fun `selected audio format reports the audio portion of a codecs string`() {
    val state = audioFormat(
      codecs = "avc1.640028,mp4a.40.2",
      label = "English",
      language = "en-US",
      bitrate = 128_000,
      channelCount = 2,
    ).toAudioTrackState()

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
    assertEquals("ec-3", audioFormat(codecs = "ec-3").toAudioTrackState().codec)
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

  private fun channelsFor(channelCount: Int): String? =
    audioFormat(codecs = "mp4a.40.2", channelCount = channelCount).toAudioTrackState().channels

  @Test
  fun `unidentifiable codec and non-meaningful fields are omitted`() {
    val state = audioFormat(codecs = null, label = " ", language = "und").toAudioTrackState()

    assertEquals(true, state.enabled)
    assertNull(state.codec)
    assertNull(state.name)
    assertNull(state.language)
    assertNull(state.bitrate)
    assertNull(state.channels)
  }

  @Test
  fun `reporter dispatches initial state, dedupes repeats, and reports changes`() {
    val reportedStates = mutableListOf<AudioTrackState>()
    val reporter = AudioTrackChangeReporter { reportedStates += it }
    val english = audioFormat(codecs = "mp4a.40.2", label = "English", language = "en")
    val spanish = audioFormat(codecs = "mp4a.40.2", label = "Spanish", language = "es")

    reporter.reportAudioInputFormatChanged(english)
    reporter.reportAudioInputFormatChanged(english)
    reporter.reportAudioInputFormatChanged(spanish)

    assertEquals(2, reportedStates.size)
    assertEquals(
      AudioTrackState(enabled = true, codec = "mp4a.40.2", name = "English", language = "en"),
      reportedStates[0]
    )
    assertEquals(
      AudioTrackState(enabled = true, codec = "mp4a.40.2", name = "Spanish", language = "es"),
      reportedStates[1]
    )
  }

  @Test
  fun `disabling with no prior state reports a single disabled state`() {
    val reportedStates = mutableListOf<AudioTrackState>()
    val reporter = AudioTrackChangeReporter { reportedStates += it }

    reporter.reportAudioTrackDisabled()

    assertEquals(listOf(AudioTrackState(enabled = false)), reportedStates)
  }

  @Test
  fun `disabling after an enabled track reports a disabled state`() {
    val reportedStates = mutableListOf<AudioTrackState>()
    val reporter = AudioTrackChangeReporter { reportedStates += it }
    val english = audioFormat(codecs = "mp4a.40.2", label = "English", language = "en")

    reporter.reportAudioInputFormatChanged(english)
    reporter.reportAudioTrackDisabled()

    assertEquals(2, reportedStates.size)
    assertEquals(
      AudioTrackState(enabled = true, codec = "mp4a.40.2", name = "English", language = "en"),
      reportedStates[0]
    )
    assertEquals(AudioTrackState(enabled = false), reportedStates[1])
  }

  @Test
  fun `repeated disabling dedupes to a single disabled state`() {
    val reportedStates = mutableListOf<AudioTrackState>()
    val reporter = AudioTrackChangeReporter { reportedStates += it }

    reporter.reportAudioTrackDisabled()
    reporter.reportAudioTrackDisabled()
    reporter.reportAudioTrackDisabled()

    assertEquals(listOf(AudioTrackState(enabled = false)), reportedStates)
  }

  @Test
  fun `re-enabling after disabling reports the enabled track again`() {
    val reportedStates = mutableListOf<AudioTrackState>()
    val reporter = AudioTrackChangeReporter { reportedStates += it }
    val english = audioFormat(codecs = "mp4a.40.2", label = "English", language = "en")

    reporter.reportAudioInputFormatChanged(english)
    reporter.reportAudioTrackDisabled()
    reporter.reportAudioInputFormatChanged(english)

    assertEquals(3, reportedStates.size)
    assertEquals(AudioTrackState(enabled = false), reportedStates[1])
    assertEquals(reportedStates[0], reportedStates[2])
  }

  @Test
  fun `reset allows a disabled state to be emitted again for a new view`() {
    val reportedStates = mutableListOf<AudioTrackState>()
    val reporter = AudioTrackChangeReporter { reportedStates += it }

    reporter.reportAudioTrackDisabled()
    reporter.reset()
    reporter.reportAudioTrackDisabled()

    assertEquals(
      listOf(AudioTrackState(enabled = false), AudioTrackState(enabled = false)),
      reportedStates
    )
  }

  @Test
  fun `reporter reset allows same state to be emitted for a new view`() {
    val reportedStates = mutableListOf<AudioTrackState>()
    val reporter = AudioTrackChangeReporter { reportedStates += it }
    val format = audioFormat(codecs = "mp4a.40.2", label = "English", language = "en")

    reporter.reportAudioInputFormatChanged(format)
    reporter.reset()
    reporter.reportAudioInputFormatChanged(format)

    assertEquals(2, reportedStates.size)
    assertEquals(reportedStates[0], reportedStates[1])
  }

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
