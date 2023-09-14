package com.mux.stats.sdk.media3_ima.testdoubles

import android.net.Uri
import com.mux.stats.sdk.media3_ima.log
import com.mux.stats.sdk.muxstats.INetworkRequest
import com.mux.android.http.beaconAuthority
import com.mux.android.util.logTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.net.URL
import java.util.*

open class FakeNetwork : INetworkRequest {

  private val bgCoroutineScope = CoroutineScope(Dispatchers.Default)

  override fun get(url: URL?) {
    log(logTag(), "GET request to URL $url")
  }

  override fun post(url: URL?, json: JSONObject?, headers: Hashtable<String, String>?) {
    log(
      logTag(),
      "POSTing request to URL $url" +
              "\nWith Headers: $headers" +
              "\nWith Body: $json"
    )
  }

  override fun postWithCompletion(
    domain: String?,
    envKey: String?,
    body: String?,
    headers: Hashtable<String, String>?,
    completion: INetworkRequest.IMuxNetworkRequestsCompletion?
  ) {
    @Suppress("DeferredResultUnused") // we *want* to let the coroutine go w/o management.
    bgCoroutineScope.async {
      delay(75)

      @Suppress("BlockingMethodInNonBlockingContext") // URL() is fine, compiler got fooled
      post(
        url = URL(
          Uri.Builder()
            .scheme("https")
            .authority(
              beaconAuthority(
                envKey ?: "",
                domain ?: ""
              )
            )
            .path("android")
            .build().toString()
        ),
        json = body?.let { JSONObject(it) },
        headers = headers
      )
    }
  }
}
