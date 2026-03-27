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
class TextTrackChangeReporterTest {

  @Test
  fun `selected text track is normalized with known values`() {
    val tracks = tracksOf(
      textGroup(
        format = textFormat(
          sampleMimeType = "text/vtt",
          label = "English (SDH)",
          language = "en-US",
          roleFlags = C.ROLE_FLAG_CAPTION,
        ),
        selected = true,
      )
    )

    val state = tracks.toTextTrackState()

    assertEquals(
      TextTrackState(
        enabled = true,
        type = "cc",
        format = "webvtt",
        name = "English (SDH)",
        language = "en-us",
      ),
      state
    )
  }

  @Test
  fun `missing text selection normalizes to disabled with cleared metadata`() {
    val tracks = tracksOf(
      textGroup(
        format = textFormat(
          sampleMimeType = "application/cea-608",
          label = "English",
          language = "en",
          roleFlags = C.ROLE_FLAG_SUBTITLE,
        ),
        selected = false,
      )
    )

    val state = tracks.toTextTrackState()

    assertEquals(TextTrackState(enabled = false), state)
  }

  @Test
  fun `unknown format and non-meaningful fields are omitted`() {
    val tracks = tracksOf(
      textGroup(
        format = textFormat(
          sampleMimeType = "application/ttml+xml",
          label = " ",
          language = "und",
          roleFlags = 0,
        ),
        selected = true,
      )
    )

    val state = tracks.toTextTrackState()

    assertEquals(true, state.enabled)
    assertNull(state.type)
    assertNull(state.format)
    assertNull(state.name)
    assertNull(state.language)
  }

  @Test
  fun `known formats are mapped when detectable`() {
    assertEquals(
      "srt",
      tracksOf(textGroup(textFormat(sampleMimeType = "application/x-subrip"), selected = true))
        .toTextTrackState().format
    )
    assertEquals(
      "cea-608",
      tracksOf(textGroup(textFormat(sampleMimeType = "application/cea-608"), selected = true))
        .toTextTrackState().format
    )
    assertEquals(
      "cea-708",
      tracksOf(textGroup(textFormat(sampleMimeType = "application/cea-708"), selected = true))
        .toTextTrackState().format
    )
  }

  @Test
  fun `reporter dispatches initial state, dedupes repeats, and reports disable`() {
    val reportedStates = mutableListOf<TextTrackState>()
    val reporter = TextTrackChangeReporter { reportedStates += it }
    val selectedTracks = tracksOf(
      textGroup(
        format = textFormat(
          sampleMimeType = "text/vtt",
          label = "English",
          language = "en",
          roleFlags = C.ROLE_FLAG_SUBTITLE,
        ),
        selected = true,
      )
    )
    val disabledTracks = tracksOf(
      textGroup(
        format = textFormat(
          sampleMimeType = "text/vtt",
          label = "English",
          language = "en",
          roleFlags = C.ROLE_FLAG_SUBTITLE,
        ),
        selected = false,
      )
    )

    reporter.reportTracksChanged(selectedTracks)
    reporter.reportTracksChanged(selectedTracks)
    reporter.reportTracksChanged(disabledTracks)

    assertEquals(2, reportedStates.size)
    assertEquals(
      TextTrackState(
        enabled = true,
        type = "subtitles",
        format = "webvtt",
        name = "English",
        language = "en",
      ),
      reportedStates[0]
    )
    assertEquals(TextTrackState(enabled = false), reportedStates[1])
  }

  @Test
  fun `reporter reset allows same state to be emitted for a new view`() {
    val reportedStates = mutableListOf<TextTrackState>()
    val reporter = TextTrackChangeReporter { reportedStates += it }
    val selectedTracks = tracksOf(
      textGroup(
        format = textFormat(sampleMimeType = "text/vtt", label = "English", language = "en"),
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

  private fun textGroup(format: Format, selected: Boolean): Tracks.Group = Tracks.Group(
    TrackGroup(format),
    false,
    intArrayOf(C.FORMAT_HANDLED),
    booleanArrayOf(selected),
  )

  private fun textFormat(
    sampleMimeType: String? = "text/vtt",
    codecs: String? = null,
    label: String? = null,
    language: String? = null,
    roleFlags: Int = 0,
  ): Format = Format.Builder()
    .setSampleMimeType(sampleMimeType)
    .setCodecs(codecs)
    .setLabel(label)
    .setLanguage(language)
    .setRoleFlags(roleFlags)
    .build()
}
