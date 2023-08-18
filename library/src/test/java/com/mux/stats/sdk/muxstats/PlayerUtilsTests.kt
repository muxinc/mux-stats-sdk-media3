package com.mux.stats.sdk.muxstats

import androidx.media3.common.Player.DISCONTINUITY_REASON_AUTO_TRANSITION
import androidx.media3.common.Player.DISCONTINUITY_REASON_INTERNAL
import androidx.media3.common.Player.DISCONTINUITY_REASON_REMOVE
import androidx.media3.common.Player.DISCONTINUITY_REASON_SEEK
import androidx.media3.common.Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT
import androidx.media3.common.Player.DISCONTINUITY_REASON_SKIP
import androidx.media3.common.Player.DiscontinuityReason
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
    fun testContinuityReason(@DiscontinuityReason reason: Int, shouldCallSeeking: Boolean) {
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
    testContinuityReason(DISCONTINUITY_REASON_SEEK_ADJUSTMENT, true)
    testContinuityReason(DISCONTINUITY_REASON_SEEK, true)
    // Cases that don't start seeking
    testContinuityReason(DISCONTINUITY_REASON_AUTO_TRANSITION, false)
    testContinuityReason(DISCONTINUITY_REASON_SKIP, false)
    testContinuityReason(DISCONTINUITY_REASON_REMOVE, false)
    testContinuityReason(DISCONTINUITY_REASON_INTERNAL, false)

  }
}