package com.mux.stats.sdk.media3_ima

import androidx.media3.common.Player
import com.google.ads.interactivemedia.v3.api.AdEvent
import io.mockk.mockk
import org.junit.Test

class MuxImAdsListenerTests : AbsRobolectricTest()  {

  @Test
  fun testAdEventsAlwaysForwarded() {
    fun testSpecificCase(adEvent: AdEvent) {
      val dummyExoPlayer = mockk<Player>(relaxed = true)

    }
  }
}