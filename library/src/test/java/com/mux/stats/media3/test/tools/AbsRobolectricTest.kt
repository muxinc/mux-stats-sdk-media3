package com.mux.stats.media3.test.tools

import com.mux.stats.media3.test.tools.testdoubles.FakeMuxDevice
import com.mux.stats.sdk.muxstats.MuxStats
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(
  manifest = Config.NONE,
)
@RunWith(RobolectricTestRunner::class)
abstract class AbsRobolectricTest {

  @Before
  fun setUpLogging() {
    // Subclasses may set this to something else as part of setup or testing, this is a good default
    MuxStats.setHostDevice(FakeMuxDevice())
  }
}
