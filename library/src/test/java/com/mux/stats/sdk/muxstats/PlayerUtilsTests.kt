package com.mux.stats.sdk.muxstats

import androidx.media3.common.Player
import androidx.media3.common.Player.DISCONTINUITY_REASON_AUTO_TRANSITION
import androidx.media3.common.Player.DISCONTINUITY_REASON_INTERNAL
import androidx.media3.common.Player.DISCONTINUITY_REASON_REMOVE
import androidx.media3.common.Player.DISCONTINUITY_REASON_SEEK
import androidx.media3.common.Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT
import androidx.media3.common.Player.DISCONTINUITY_REASON_SKIP
import androidx.media3.common.Player.DiscontinuityReason
import androidx.media3.common.Player.STATE_IDLE
import com.mux.core_android.test.tools.log
import com.mux.stats.media3.test.tools.AbsRobolectricTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Test

class PlayerUtilsTests : AbsRobolectricTest() {

  @Test
  fun testHandlePositionDiscontinuity() {
    fun testDiscontinuityReason(@DiscontinuityReason reason: Int, shouldCallSeeking: Boolean) {
      val mockCollector = mockk<MuxStateCollector> {
        every { seeking() } just runs
      }

      mockCollector.handlePositionDiscontinuity(reason)

      val times = if (shouldCallSeeking) 1 else 0
      verify(exactly = times) {
        mockCollector.seeking()
      }
    }

    // Cases that start seeking
    testDiscontinuityReason(DISCONTINUITY_REASON_SEEK_ADJUSTMENT, true)
    testDiscontinuityReason(DISCONTINUITY_REASON_SEEK, true)
    // Cases that don't start seeking
    testDiscontinuityReason(DISCONTINUITY_REASON_AUTO_TRANSITION, false)
    testDiscontinuityReason(DISCONTINUITY_REASON_SKIP, false)
    testDiscontinuityReason(DISCONTINUITY_REASON_REMOVE, false)
    testDiscontinuityReason(DISCONTINUITY_REASON_INTERNAL, false)
  }

  @Test
  fun testHandlePlayWhenReadyBecomesTrue() {
    fun doTestCase(@Player.State playerState: Int, sendPlayingAlso: Boolean) {
      log("testHandlePlayWhenReadyBecomesTrue", "Testing player state $playerState")
      val mockStateCollector = mockk<MuxStateCollector> {
        every { play() } just runs
        every { playing() } just runs
      }
      mockStateCollector.handlePlayWhenReady(true, playerState)
      verify { mockStateCollector.play() }
      if (sendPlayingAlso) {
        verify { mockStateCollector.playing() }
      }
    }

    doTestCase(Player.STATE_READY, true)
    doTestCase(Player.STATE_BUFFERING, false)
    doTestCase(Player.STATE_ENDED, false)
    doTestCase(Player.STATE_IDLE, false)
  }

  @Test
  fun testHandlePlayWhenReadyBecomesFalse() {
    fun testFromPlayerState(from: MuxPlayerState) {
      val mockStateCollector = mockk<MuxStateCollector> {
        every { pause() } just runs
        every { muxPlayerState } returns from
      }
      val dummyPlayerState = STATE_IDLE

      mockStateCollector.handlePlayWhenReady(false, dummyPlayerState)

      val times = if (from == MuxPlayerState.PAUSED) 0 else 1
      verify(exactly = times) { mockStateCollector.pause() }
    }

    for (state in MuxPlayerState.values()) {
      log(javaClass.simpleName, "testing handlePlayWhenReadyBecomesFalse from state $state")
      testFromPlayerState(state)
    }
  }

  @Test
  fun testHandleExoPlaybackStateWhilePlayingAds() {
    val mockStateCollector = mockk<MuxStateCollector> {
      every { muxPlayerState } returns MuxPlayerState.PLAYING_ADS
    }
    mockStateCollector.handleExoPlaybackState(0, false) // params not under test
    // no state-changing methods should be called
    verify(exactly = 0) {
      mockStateCollector.buffering()
      mockStateCollector.seeking()
      mockStateCollector.seeked()
      mockStateCollector.playing()
      mockStateCollector.pause()
      mockStateCollector.ended()
    }
  }

  @Test
  fun testHandleExoPlaybackStateBuffering() {
    val mockStateCollector = mockk<MuxStateCollector> {
      every { buffering() } just runs
      every { muxPlayerState } returns MuxPlayerState.INIT // state during call is not under test
    }

    // both cases should call buffering()
    mockStateCollector.handleExoPlaybackState(Player.STATE_BUFFERING, false)
    mockStateCollector.handleExoPlaybackState(Player.STATE_BUFFERING, true)

    verify(exactly = 2) {
      mockStateCollector.buffering()
    }
    // no other state-changing methods should be called
    verify(exactly = 0) {
      mockStateCollector.seeking()
      mockStateCollector.seeked()
      mockStateCollector.playing()
      mockStateCollector.pause()
      mockStateCollector.ended()
    }
  }

  @Test
  fun testHandleExoPlaybackStateEnded() {
    val mockStateCollector = mockk<MuxStateCollector> {
      every { ended() } just runs
      every { muxPlayerState } returns MuxPlayerState.INIT // state during call is not under test
    }

    // both cases should call ended()
    mockStateCollector.handleExoPlaybackState(Player.STATE_ENDED, false)
    mockStateCollector.handleExoPlaybackState(Player.STATE_ENDED, true)

    verify(exactly = 2) {
      mockStateCollector.ended()
    }
    // no other state-changing methods should be called
    verify(exactly = 0) {
      mockStateCollector.seeking()
      mockStateCollector.seeked()
      mockStateCollector.playing()
      mockStateCollector.pause()
      mockStateCollector.buffering()
    }
  }

  @Test
  fun testPlaybackStateIdle() {
    fun testIdleFromState(from: MuxPlayerState) {
      val mockStateCollector = mockk<MuxStateCollector> {
        every { muxPlayerState } returns from
        every { pause() } just runs
      }

      // Handling is the same for both cases
      mockStateCollector.handleExoPlaybackState(Player.STATE_IDLE, false)
      mockStateCollector.handleExoPlaybackState(Player.STATE_IDLE, true)

      val shouldPause = listOf(MuxPlayerState.PLAYING, MuxPlayerState.PLAY).contains(from)
      val times = if (shouldPause) 2 else 0
      verify(exactly = times) {
        mockStateCollector.pause()
      }
      // no other state-changing methods should be called
      verify(exactly = 0) {
        mockStateCollector.buffering()
        mockStateCollector.seeking()
        mockStateCollector.seeked()
        mockStateCollector.playing()
        mockStateCollector.ended()
      }
    }

    for (state in MuxPlayerState.values()) {
      testIdleFromState(state)
    }
  }

  @Test
  fun testHandleExoPlaybackStateReadyWhileSeekingWhilePlayWhenReady() {
    val mockStateCollector = mockk<MuxStateCollector> {
      every { seeked() } just runs
      every { playing() } just runs
      every { muxPlayerState } returns MuxPlayerState.SEEKING
    }

    mockStateCollector.handleExoPlaybackState(Player.STATE_READY, true)
    verify(exactly = 1) {
      mockStateCollector.seeked()
      mockStateCollector.playing()
    }
    // no state-changing methods should be called
    verify(exactly = 0) {
      mockStateCollector.buffering()
      mockStateCollector.seeking()
      mockStateCollector.pause()
      mockStateCollector.ended()
    }
  }

  @Test
  fun testHandleExoPlaybackStateReadyWhileSeekingWhileNotPlayWhenReady() {
    val mockStateCollector = mockk<MuxStateCollector> {
      every { seeked() } just runs
      every { pause() } just runs
      every { muxPlayerState } returns MuxPlayerState.SEEKING
    }

    mockStateCollector.handleExoPlaybackState(Player.STATE_READY, false)
    verify(exactly = 1) {
      mockStateCollector.seeked()
      mockStateCollector.pause()
    }
    // no state-changing methods should be called
    verify(exactly = 0) {
      mockStateCollector.playing()
      mockStateCollector.buffering()
      mockStateCollector.seeking()
      mockStateCollector.ended()
      mockStateCollector.play()
    }
  }

  @Test
  fun testHandleExoPlaybackStateReadyWhilePlayWhenReady() {
    val mockStateCollector = mockk<MuxStateCollector> {
      every { muxPlayerState } returns MuxPlayerState.INIT // value not part of test
      every { playing() } just runs
    }

    mockStateCollector.handleExoPlaybackState(Player.STATE_READY, true)
    verify(exactly = 1) {
      mockStateCollector.playing()
    }
    // no state-changing methods should be called
    verify(exactly = 0) {
      mockStateCollector.seeked()
      mockStateCollector.pause()
      mockStateCollector.buffering()
      mockStateCollector.seeking()
      mockStateCollector.ended()
      mockStateCollector.play()
    }
  }

  @Test
  fun testHandleExoPlaybackStateReadyWhileNotPlayWhenReady() {
    val mockStateCollector = mockk<MuxStateCollector> {
      every { muxPlayerState } returns MuxPlayerState.INIT // value not part of test
      every { pause() } just runs
    }

    mockStateCollector.handleExoPlaybackState(Player.STATE_READY, false)
    verify(exactly = 1) {
      mockStateCollector.pause()
    }
    // no state-changing methods should be called
    verify(exactly = 0) {
      mockStateCollector.seeked()
      mockStateCollector.playing()
      mockStateCollector.buffering()
      mockStateCollector.seeking()
      mockStateCollector.ended()
      mockStateCollector.play()
    }
  }

  @Test
  fun testHandleExoPlaybackStateReadyWhileNotPlayWhenReadyAlreadyPaused() {
    val mockStateCollector = mockk<MuxStateCollector> {
      every { muxPlayerState } returns MuxPlayerState.PAUSED
    }

    mockStateCollector.handleExoPlaybackState(Player.STATE_READY, false)
    // no state-changing methods should be called
    verify(exactly = 0) {
      mockStateCollector.pause()
      mockStateCollector.seeked()
      mockStateCollector.playing()
      mockStateCollector.buffering()
      mockStateCollector.seeking()
      mockStateCollector.ended()
      mockStateCollector.play()
    }
  }
}