package com.mux.stats.sdk.media3_ima

import androidx.media3.common.Player
import com.google.ads.interactivemedia.v3.api.Ad
import com.google.ads.interactivemedia.v3.api.AdError
import com.google.ads.interactivemedia.v3.api.AdError.AdErrorCode
import com.google.ads.interactivemedia.v3.api.AdError.AdErrorType
import com.google.ads.interactivemedia.v3.api.AdErrorEvent
import com.google.ads.interactivemedia.v3.api.AdErrorEvent.AdErrorListener
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

  @Test
  fun testAdErrorsAlwaysForwarded() {
    // This specific test doesn't care about what the SDK internals are doing at all
    // We only want to make sure that the customer's ad listeners are always called
    fun testSpecificCase(incomingError: AdErrorEvent) {
      log(message = "Testing adError with type $incomingError")
      val dummyExoPlayer = mockk<Player>(relaxed = true)
      val dummyMuxStats = mockk<MuxStatsSdkMedia3<Player>>(relaxed = true) {
        every { boundPlayer } returns dummyExoPlayer
      }
      val customerAdErrorListener = mockk<AdErrorListener> {
        every { onAdError(any()) } just runs
      }
      val muxImaListener = MuxImaAdsListener.newListener(
        dummyMuxStats,
        mockk(relaxed = true), // error listener not under test
        customerAdErrorListener
      )

      muxImaListener.onAdError(incomingError)

      verify {
        customerAdErrorListener.onAdError(
          withArg { forwardedError ->
            assertEquals(
              "Error type should be ${incomingError.error.errorType}",
              incomingError.error.errorType,
              forwardedError.error.errorType
            )
            assertEquals(
              "Error code should be ${incomingError.error.errorCode}",
              incomingError.error.errorCode,
              forwardedError.error.errorCode
            )
          }
        )
      } // verify
    } // testSpecificCase

    for (errType in AdErrorType.values()) {
      for (errCode in AdErrorCode.values()) {
        val fakeEvent = object : AdErrorEvent {
          override fun getError(): AdError = AdError(errType, errCode, "fake error")
          override fun getUserRequestContext(): Any = Object()
          override fun toString() = "adError: $error"
        }
        testSpecificCase(fakeEvent)
      }
    }
  }
}
