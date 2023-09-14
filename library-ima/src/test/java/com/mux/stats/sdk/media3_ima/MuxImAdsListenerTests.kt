package com.mux.stats.sdk.media3_ima

import androidx.media3.common.Player
import com.google.ads.interactivemedia.v3.api.Ad
import com.google.ads.interactivemedia.v3.api.AdEvent
import com.google.ads.interactivemedia.v3.api.AdEvent.AdEventListener
import com.mux.stats.sdk.muxstats.MuxStatsSdkMedia3
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class MuxImAdsListenerTests : AbsRobolectricTest() {

  @Test
  fun testAdEventsAlwaysForwarded() {
    // This specific test doesn't care about what the SDK internals are doing at all
    // We only want to make sure that the customer's ad listeners are always called
    fun testSpecificCase(incomingAdEvent: AdEvent) {
      log(message = "Testing adEvent with type ${incomingAdEvent.type.name}")
      val dummyExoPlayer = mockk<Player>(relaxed = true)
      val dummyMuxStats = mockk<MuxStatsSdkMedia3<Player>>(relaxed = true) {
        every { boundPlayer } returns dummyExoPlayer
      }
      val customerAdEventListener = mockk<AdEventListener> {
        every { onAdEvent(any()) } just runs
      }
      val muxImaListener = MuxImaAdsListener.newListener(
        dummyMuxStats,
        customerAdEventListener,
        mockk(relaxed = true) // error listener not under test
      )

      muxImaListener.onAdEvent(incomingAdEvent)

      verify {
        customerAdEventListener.onAdEvent(
          withArg { capturedListenerArg ->
            assertEquals(
              "AdEvent ${incomingAdEvent.type.name} should be forwarded to customer",
              incomingAdEvent.type.name,
              capturedListenerArg.type.name,
            )
          })
      } // verify
    } // testSpecificCase

    for (eventType in AdEvent.AdEventType.values()) {
      val fakeEvent = object : AdEvent {
        override fun getAd(): Ad = mockk(relaxed = true)
        override fun getType(): AdEvent.AdEventType = eventType
        override fun getAdData(): MutableMap<String, String> = mockk(relaxed = true)
      }
      testSpecificCase(fakeEvent)
    }
  }
}
