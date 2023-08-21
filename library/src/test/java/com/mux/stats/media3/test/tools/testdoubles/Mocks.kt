package com.mux.stats.media3.test.tools.testdoubles

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Point
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.View
import com.mux.stats.sdk.muxstats.*
import io.mockk.*
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

const val MOCK_SCREEN_WIDTH = 2400
const val MOCK_SCREEN_HEIGHT = 1080
const val MOCK_INSET_X = 100
const val MOCK_INSET_Y = 100

const val MOCK_PLAYER_WIDTH = 1080
const val MOCK_PLAYER_HEIGHT = 700

/**
 * Mocks an [IPlayerListener], with no mocked methods
 */
fun mockPlayerListener() = mockk<IPlayerListener> {}

/**
 * Mocks an [IDevice] with no mocked methods
 */
fun mockDevice() = mockk<IDevice> {}

/**
 * Mocks an [INetworkRequest] with no mocked methods
 */
fun mockNetworkRequest() = mockk<INetworkRequest> {}

/**
 * Mocks a View of constant size
 */
fun mockView() = mockk<View> {
  every { width } returns MOCK_PLAYER_WIDTH
  every { height } returns MOCK_PLAYER_HEIGHT
  every { id } returns 1
  every { context } returns mockActivity()
}

/**
 * Mocks the path we call to get the size of the screen, and get connection info
 */
@Suppress("DEPRECATION") // Backward-compatible APIs are mocked intentionally
fun mockActivity(
  prefs: SharedPreferences = mockSharedPrefs(),
  pm: PackageManager = mockPackageManager(),
  connMgr: ConnectivityManager = mockConnectivityManager16(NetworkCapabilities.TRANSPORT_CELLULAR)
) = mockk<Activity>(relaxed = true) {
  every { windowManager } returns mockk {
    every { defaultDisplay } returns mockk {
      every { getSize(Point()) } answers {
        arg<Point>(0).apply {
          x = MOCK_SCREEN_WIDTH
          y = MOCK_SCREEN_HEIGHT
        }
      }
    }
  }
  every { getSharedPreferences(any(), any()) } returns prefs
  every { packageManager } returns pm
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    every { getSystemService(ConnectivityManager::class.java) } returns connMgr
  }
  every { getSystemService(Context.CONNECTIVITY_SERVICE) } returns connMgr
  every { packageName } returns "com.mux.core_android.unittests"
}

fun mockPackageManager() = mockk<PackageManager> {
  val fakePkgInfo = PackageInfo()
  fakePkgInfo.packageName = "com.mux.core_android.unittests"
  fakePkgInfo.versionName = "1.0.0"
  every { getPackageInfo(any<String>(), any<Int>()) } returns fakePkgInfo
  // TODO: VersionedPackage is new, but nothing is using it yet
  //every { getPackageInfo(any<VersionedPackage>(), any()) } returns fakePkgInfo
}

/**
 * Mocks a SharedPreferences. Not all types of pref value are supported (as of oct 13 2022)
 */
fun mockSharedPrefs() = mockk<SharedPreferences> {
  val prefStrings = HashMap<String, String>()
  every { edit() } returns mockk {
    val thiz = this

    // putString
    val keySlot = slot<String>()
    val valSlot = slot<String>()
    every { putString(capture(keySlot), capture(valSlot)) } answers {
      prefStrings[keySlot.captured] = valSlot.captured
      thiz
    }

    every { commit() } returns true
    every { apply() } just runs
  }

  // getString
  val keySlot = slot<String>()
  val valSlot = ArrayList<String?>()
  every { getString(capture(keySlot), captureNullable(valSlot)) } answers {
    (prefStrings[keySlot.captured] ?: valSlot[0])
  }
}

/**
 * Mocks a [ConnectivityManager] with methods for getting the active network type using modern SDK
 * APIs
 */
fun mockConnectivityManager23(transportType: Int) = mockk<ConnectivityManager> {
  every { activeNetwork } returns mockk {}
  every { getNetworkCapabilities(any()) } returns mockk {
    every { hasTransport(any()) } answers { firstArg<Int>() == transportType }
  }
}

/**
 * Mocks [ConnectivityManager] with methods for getting the active network type using the older
 * [NetworkInfo]-based APIs
 */
@Suppress("DEPRECATION") // some legacy methods are used for backward compat
fun mockConnectivityManager16(netType: Int) = mockk<ConnectivityManager> {
  every { activeNetworkInfo } returns mockk {
    every { type } answers { netType }
  }
}

/**
 * Mocks [URL], returning the given string as toString and a mocked [HttpURLConnection]
 */
fun mockURL(url: String, conn: HttpURLConnection = mockHttpUrlConnection()): URL = mockk {
  //every { toString() } returns url
  every { openConnection() } returns conn
}

/**
 * Mocks [OutputStream], capturing written data
 */
fun mockOutputStream(byteArraySlot: CapturingSlot<ByteArray> = slot()): OutputStream =
  mockk(relaxed = true) {
    every { write(capture(byteArraySlot)) } just runs
  }

/**
 * Mocks [HttpURLConnection], providing basic response data and input/output streams
 */
fun mockHttpUrlConnection(
  code: Int = 200,
  message: String? = "OK",
  responseHeaders: Map<String, List<String>> = mapOf(),
  input: InputStream = ByteArrayInputStream("hello world".encodeToByteArray()),
  output: OutputStream = mockOutputStream()
): HttpURLConnection =
  mockk(relaxed = true) {
    every { responseCode } returns code
    every { responseMessage } returns message
    every { headerFields } returns responseHeaders
    every { inputStream } returns input
    every { outputStream } returns output
    every { setRequestProperty(any(), any()) } just runs
  }
