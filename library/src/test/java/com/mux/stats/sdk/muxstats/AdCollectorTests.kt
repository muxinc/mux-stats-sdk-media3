package com.mux.stats.sdk.muxstats

import com.mux.stats.media3.test.tools.AbsRobolectricTest
import com.mux.stats.sdk.core.events.EventBus
import com.mux.stats.sdk.core.events.IEvent
import com.mux.stats.sdk.core.events.playback.RebufferEndEvent
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.junit.Assert
import org.junit.Test

class AdCollectorTests : AbsRobolectricTest() {
  @Test
  fun testEndsRebufferingWhenAdsStart() {
    val dispatchedEvents = mutableListOf<IEvent>()
    val eventBus = mockk<EventBus> {
      every { addListener(any()) } just runs
      every { removeListener(any()) } just runs
      every { removeAllListeners() } just runs
      every { dispatch(any()) } answers { call ->
        dispatchedEvents += (call.invocation.args.first() as IEvent)
      }
    }
    val stateCollector = MuxStateCollector(mockk<MuxStats>(relaxed = true), eventBus)
    val adCollector = AdCollector.create(stateCollector, eventBus)

    // initial condition: rebuffering started
    stateCollector.play()
    stateCollector.playing()
    stateCollector.buffering()

    // function under test
    adCollector.onStartPlayingAds()

    Assert.assertNotEquals(
      "state should not be REBUFFERING after onStartPlayingAds",
      MuxPlayerState.REBUFFERING, stateCollector.muxPlayerState
    )

    Assert.assertTrue(
      "rebufferend should be sent",
      dispatchedEvents.find { it is RebufferEndEvent } != null
    )

  }
}