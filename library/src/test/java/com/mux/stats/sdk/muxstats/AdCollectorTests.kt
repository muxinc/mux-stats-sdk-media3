package com.mux.stats.sdk.muxstats

import com.mux.android.util.noneOf
import com.mux.stats.media3.test.tools.AbsRobolectricTest
import com.mux.stats.sdk.core.events.EventBus
import com.mux.stats.sdk.core.events.IEvent
import com.mux.stats.sdk.core.events.playback.AdBreakEndEvent
import com.mux.stats.sdk.core.events.playback.AdErrorEvent
import com.mux.stats.sdk.core.events.playback.AdPauseEvent
import com.mux.stats.sdk.core.events.playback.AdPlayEvent
import com.mux.stats.sdk.core.events.playback.AdPlayingEvent
import com.mux.stats.sdk.core.events.playback.AdRequestEvent
import com.mux.stats.sdk.core.events.playback.AdResponseEvent
import com.mux.stats.sdk.core.events.playback.RebufferEndEvent
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import junit.framework.TestCase.assertTrue
import org.junit.Assert
import org.junit.Test

class AdCollectorTests : AbsRobolectricTest() {

  @Test
  fun testAdPlaybackEventsNotDuringAds() {
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

    // initial condition: some normal stuff happens
    stateCollector.play()
    adCollector.dispatch(AdRequestEvent(null))
    stateCollector.playing()

    // ad sdks sometimes do surprising things when handling unhappy paths
    adCollector.dispatch(AdErrorEvent(null))
    adCollector.dispatch(AdPlayingEvent(null))
    adCollector.dispatch(AdPauseEvent(null))
    adCollector.dispatch(AdBreakEndEvent(null))

    // Maybe it retried
    adCollector.dispatch(AdRequestEvent(null))
    adCollector.dispatch(AdResponseEvent(null))

    assertTrue(
      "Events related to ads playing or ending should be filtered out if no adbreakstart came thru",
      dispatchedEvents.map { it.type }.noneOf(AdCollector.EVENTS_ONLY_IN_ADBREAK)
    )

    assertTrue(
      "Some ad-related events are expected outside of ad breaks",
      dispatchedEvents.map { it.type }
        .containsAll(listOf(
          AdErrorEvent.TYPE, AdRequestEvent.TYPE, AdRequestEvent.TYPE, AdRequestEvent.TYPE
        ))
    )
  }

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

  @Test
  fun testDispatchesAdPlayAndAdPlayingOnlyDuringAdBreaks() {
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

    adCollector.dispatch(AdPlayEvent(null))
    adCollector.dispatch(AdPlayingEvent(null))

    Assert.assertTrue(
      "adplay should not be sent to event bus",
      dispatchedEvents.find { it is AdPlayEvent } == null
    )

    Assert.assertTrue(
      "adplaying should not be sent to event bus",
      dispatchedEvents.find { it is AdPlayingEvent } == null
    )

    dispatchedEvents.clear()
    adCollector.onStartPlayingAds()

    adCollector.dispatch(AdPlayEvent(null))
    adCollector.dispatch(AdPlayingEvent(null))

    Assert.assertTrue(
      "adplay should not be sent to event bus",
      dispatchedEvents.find { it is AdPlayEvent } != null
    )

    Assert.assertTrue(
      "adplaying should not be sent to event bus",
      dispatchedEvents.find { it is AdPlayingEvent } != null
    )

    dispatchedEvents.clear()
    adCollector.onFinishPlayingAds(willPlay = true)

    adCollector.dispatch(AdPlayEvent(null))
    adCollector.dispatch(AdPlayingEvent(null))

    Assert.assertTrue(
      "adplay should not be sent to event bus",
      dispatchedEvents.find { it is AdPlayEvent } == null
    )

    Assert.assertTrue(
      "adplaying should not be sent to event bus",
      dispatchedEvents.find { it is AdPlayingEvent } == null
    )
  }

  @Test
  fun testDispatchesAdErrorBothDuringAndOutsideOfAdBreaks() {
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

    adCollector.dispatch(AdErrorEvent(null))
    Assert.assertTrue(
      "aderror should be sent to event bus before the start of an ad break",
      dispatchedEvents.find { it is AdErrorEvent } != null
    )

    dispatchedEvents.clear()
    adCollector.onStartPlayingAds()

    adCollector.dispatch(AdErrorEvent(null))
    Assert.assertTrue(
      "aderror should be sent to event bus during an ad break",
      dispatchedEvents.find { it is AdErrorEvent } != null
    )

    dispatchedEvents.clear()
    adCollector.onFinishPlayingAds(willPlay = true)

    adCollector.dispatch(AdErrorEvent(null))
    Assert.assertTrue(
      "aderror should be sent to event bus after an ad break is finished",
      dispatchedEvents.find { it is AdErrorEvent } != null
    )
  }

  @Test
  fun testDispatchesAdRequestBothDuringAndOutsideOfAdBreaks() {
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

    adCollector.dispatch(AdRequestEvent(null))
    Assert.assertTrue(
      "adrequest should be sent to event bus before the start of an ad break",
      dispatchedEvents.find { it is AdRequestEvent } != null
    )

    dispatchedEvents.clear()
    adCollector.onStartPlayingAds()

    adCollector.dispatch(AdRequestEvent(null))
    Assert.assertTrue(
      "adrequest should be sent to event bus during an ad break",
      dispatchedEvents.find { it is AdRequestEvent } != null
    )

    dispatchedEvents.clear()
    adCollector.onFinishPlayingAds(willPlay = true)

    adCollector.dispatch(AdRequestEvent(null))
    Assert.assertTrue(
      "adrequest should be sent to event bus  after an ad break is finished",
      dispatchedEvents.find { it is AdRequestEvent } != null
    )
  }

  @Test
  fun testDispatchesAdResponseBothDuringAndOutsideOfAdBreaks() {
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

    adCollector.dispatch(AdResponseEvent(null))
    Assert.assertTrue(
      "adresponse should be sent to event bus before the start of an ad break",
      dispatchedEvents.find { it is AdResponseEvent } != null
    )

    dispatchedEvents.clear()
    adCollector.onStartPlayingAds()

    adCollector.dispatch(AdResponseEvent(null))
    Assert.assertTrue(
      "adresponse should be sent to event bus during an ad break",
      dispatchedEvents.find { it is AdResponseEvent } != null
    )

    dispatchedEvents.clear()
    adCollector.onFinishPlayingAds(willPlay = true)

    adCollector.dispatch(AdResponseEvent(null))
    Assert.assertTrue(
      "adresponse should be sent to event bus after an ad break is finished",
      dispatchedEvents.find { it is AdResponseEvent } != null
    )
  }
}