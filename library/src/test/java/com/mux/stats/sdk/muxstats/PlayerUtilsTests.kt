package com.mux.stats.sdk.muxstats

import androidx.media3.common.Player.DISCONTINUITY_REASON_AUTO_TRANSITION
import androidx.media3.common.Player.DISCONTINUITY_REASON_INTERNAL
import androidx.media3.common.Player.DISCONTINUITY_REASON_REMOVE
import androidx.media3.common.Player.DISCONTINUITY_REASON_SEEK
import androidx.media3.common.Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT
import androidx.media3.common.Player.DISCONTINUITY_REASON_SKIP
import androidx.media3.common.Player.DiscontinuityReason
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
    val mockStateCollector = mockk<MuxStateCollector> {
      every { play() } just runs
    }
    mockStateCollector.handlePlayWhenReady(true)
    verify { mockStateCollector.play() }
  }

  @Test
  fun testHandlePlayWhenReadyBecomesFalse() {
    fun testFromPlayerState(from: MuxPlayerState) {
      val mockStateCollector = mockk<MuxStateCollector> {
        every { pause() } just runs
        every { muxPlayerState } returns from
      }

      mockStateCollector.handlePlayWhenReady(false)

      val times = if (from == MuxPlayerState.PAUSED) 0 else 1
      verify(exactly = times) { mockStateCollector.pause() }
    }

    for (state in MuxPlayerState.values()) {
      log(javaClass.simpleName, "testing handlePlayWhenReadyBecomesFalse from state $state")
      testFromPlayerState(state)
    }
  }
}