package com.mux.stats.muxdatasdkformedia3.examples

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.AdsConfiguration
import com.mux.stats.muxdatasdkformedia3.Constants

/**
 * Helper class for the example Activities that handles managing playback parameters, etc for
 * IMA client-side ads
 */
class ImaClientAdsParamHelper {
  // todo: set data from Intent (ie, deep linking)

  var sourceUrl: String? = null
  var adTagUrl: String? = null
  var title: String? = null
  var envKey: String? = null

  fun createMediaItemBuilder(): MediaItem.Builder {
    return MediaItem.Builder()
      .setUri(Uri.parse(sourceUrlOrDefault()))
      .setAdsConfiguration(
        AdsConfiguration.Builder(Uri.parse(adTagUrlOrDefault())).build()
      )
  }

  fun adTagUrlOrDefault(): String {
    return adTagUrl?.ifEmpty { DEFAULT_AD_TAG_URL } ?: DEFAULT_AD_TAG_URL
  }

  fun sourceUrlOrDefault(): String {
    return sourceUrl?.ifEmpty { DEFAULT_SOURCE_URL } ?: DEFAULT_SOURCE_URL
  }

  fun envKeyOrDefault(): String {
    return envKey?.ifEmpty { Constants.MUX_DATA_ENV_KEY } ?: Constants.MUX_DATA_ENV_KEY
  }

  fun createMediaItem(): MediaItem {
    return createMediaItemBuilder().build()
  }

  fun saveInstanceState(state: Bundle) {
    state.putString("source url", sourceUrl)
    state.putString("ad tag url", adTagUrl)
    state.putString("env key", envKey)
  }

  fun restoreInstanceState(state: Bundle) {
    sourceUrl = state.getString("source url", null)
    adTagUrl = state.getString("ad tag url", null)
    envKey = state.getString("env key", null)
  }

  companion object {
    const val DEFAULT_SOURCE_URL = Constants.VOD_TEST_URL_TEARS_OF_STEEL
    const val DEFAULT_AD_TAG_URL = Constants.AD_TAG_COMPLEX
  }
}

data class ImaAdTagExample(
  val title: String,
  val adTagUrl: String,
)

object ImaAdTags {

  val googleTags: List<ImaAdTagExample> = listOf(
    ImaAdTagExample(
      title = "Single Inline Linear",
      adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/single_ad_samples&sz=640x480&cust_params=sample_ct%3Dlinear&ciu_szs=300x250%2C728x90&gdfp_req=1&output=vast&unviewed_position_start=1&env=vp&impl=s&correlator="
    ),
    ImaAdTagExample(
      title = "Single Skippable Inline",
      adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/single_preroll_skippable&sz=640x480&ciu_szs=300x250%2C728x90&gdfp_req=1&output=vast&unviewed_position_start=1&env=vp&impl=s&correlator="
    ),
    ImaAdTagExample(
      title = "Single Redirect Linear",
      adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/single_ad_samples&sz=640x480&cust_params=sample_ct%3Dredirectlinear&ciu_szs=300x250%2C728x90&gdfp_req=1&output=vast&unviewed_position_start=1&env=vp&impl=s&correlator="
    ),
    ImaAdTagExample(
      title = "Single Redirect Error",
      adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/single_ad_samples&sz=640x480&cust_params=sample_ct%3Dredirecterror&ciu_szs=300x250%2C728x90&gdfp_req=1&output=vast&unviewed_position_start=1&env=vp&impl=s&correlator="
    ),
    ImaAdTagExample(
      title = "Single Redirect Broken (Fallback)",
      adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/single_ad_samples&sz=640x480&cust_params=sample_ct%3Dredirecterror&ciu_szs=300x250%2C728x90&gdfp_req=1&output=vast&unviewed_position_start=1&env=vp&impl=s&nofb=1&correlator="
    ),
    ImaAdTagExample(
      title = "Single VPAID 2.0 Linear",
      adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/single_ad_samples&sz=640x480&cust_params=sample_ct%3Dlinearvpaid2js&ciu_szs=300x250%2C728x90&gdfp_req=1&output=vast&unviewed_position_start=1&env=vp&impl=s&correlator="
    ),
    ImaAdTagExample(
      title = "Single VPAID 2.0 Non-Linear",
      adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/single_ad_samples&sz=640x480&cust_params=sample_ct%3Dnonlinearvpaid2js&ciu_szs=728x90%2C300x250&gdfp_req=1&output=vast&unviewed_position_start=1&env=vp&impl=s&correlator="
    ),
    ImaAdTagExample(
      title = "Single Non-linear Inline",
      adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/nonlinear_ad_samples&sz=480x70&cust_params=sample_ct%3Dnonlinear&ciu_szs=300x250%2C728x90&gdfp_req=1&output=vast&unviewed_position_start=1&env=vp&impl=s&correlator="
    ),
    ImaAdTagExample(
      title = "Single Vertical Inline Linear",
      adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/single_vertical_ad_samples&sz=360x640&gdfp_req=1&output=vast&unviewed_position_start=1&env=vp&impl=s&correlator="
    ),
    ImaAdTagExample(
      title = "VMAP Session Ad Rule Pre-roll",
      adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/vmap_ad_samples&sz=640x480&cust_params=sar%3Da0f2&ciu_szs=300x250&ad_rule=1&gdfp_req=1&output=vmap&unviewed_position_start=1&env=vp&impl=s&correlator="
    ),
    ImaAdTagExample(
      title = "VMAP Pre-roll",
      adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/vmap_ad_samples&sz=640x480&cust_params=sample_ar%3Dpreonly&ciu_szs=300x250%2C728x90&gdfp_req=1&ad_rule=1&output=vmap&unviewed_position_start=1&env=vp&impl=s&correlator="
    ),
    ImaAdTagExample(
      title = "VMAP Pre-roll + Bumper",
      adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/vmap_ad_samples&sz=640x480&cust_params=sample_ar%3Dpreonlybumper&ciu_szs=300x250&gdfp_req=1&ad_rule=1&output=vmap&unviewed_position_start=1&env=vp&impl=s&correlator="
    ),
    ImaAdTagExample(
      title = "VMAP Post-roll",
      adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/vmap_ad_samples&sz=640x480&cust_params=sample_ar%3Dpostonly&ciu_szs=300x250&gdfp_req=1&ad_rule=1&output=vmap&unviewed_position_start=1&env=vp&impl=s&correlator="
    ),
    ImaAdTagExample(
      title = "VMAP Post-roll + Bumper",
      adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/vmap_ad_samples&sz=640x480&cust_params=sample_ar%3Dpostonlybumper&ciu_szs=300x250&gdfp_req=1&ad_rule=1&output=vmap&unviewed_position_start=1&env=vp&impl=s&correlator="
    ),
    ImaAdTagExample(
      title = "VMAP Pre-, Mid-, and Post-rolls, Single Ads",
      adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/vmap_ad_samples&sz=640x480&cust_params=sample_ar%3Dpremidpost&ciu_szs=300x250&gdfp_req=1&ad_rule=1&output=vmap&unviewed_position_start=1&env=vp&impl=s&cmsid=496&vid=short_onecue&correlator="
    ),
    ImaAdTagExample(
      title = "VMAP - Pre-roll Single Ad, Mid-roll Standard Pod with 3 ads, Post-roll Single Ad",
      adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/vmap_ad_samples&sz=640x480&cust_params=sample_ar%3Dpremidpostpod&ciu_szs=300x250&gdfp_req=1&ad_rule=1&output=vmap&unviewed_position_start=1&env=vp&impl=s&cmsid=496&vid=short_onecue&correlator="
    ),
    ImaAdTagExample(
      title = "VMAP - Pre-roll Single Ad, Mid-roll Optimized Pod with 3 Ads, Post-roll Single Ad",
      adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/vmap_ad_samples&sz=640x480&cust_params=sample_ar%3Dpremidpostoptimizedpod&ciu_szs=300x250&gdfp_req=1&ad_rule=1&output=vmap&unviewed_position_start=1&env=vp&impl=s&cmsid=496&vid=short_onecue&correlator="
    ),
    ImaAdTagExample(
      title = "VMAP - Pre-roll Single Ad, Mid-roll Standard Pod with 3 Ads, Post-roll Single Ad (bumpers around all ad breaks)",
      adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/vmap_ad_samples&sz=640x480&cust_params=sample_ar%3Dpremidpostpodbumper&ciu_szs=300x250&gdfp_req=1&ad_rule=1&output=vmap&unviewed_position_start=1&env=vp&impl=s&cmsid=496&vid=short_onecue&correlator="
    ),
    ImaAdTagExample(
      title = "VMAP - Pre-roll Single Ad, Mid-roll Optimized Pod with 3 Ads, Post-roll Single Ad (bumpers around all ad breaks)",
      adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/vmap_ad_samples&sz=640x480&cust_params=sample_ar%3Dpremidpostoptimizedpodbumper&ciu_szs=300x250&gdfp_req=1&ad_rule=1&output=vmap&unviewed_position_start=1&env=vp&impl=s&cmsid=496&vid=short_onecue&correlator="
    ),
    ImaAdTagExample(
      title = "VMAP - Pre-roll Single Ad, Mid-roll Standard Pods with 5 Ads Every 10 Seconds, Post-roll Single Ad",
      adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/vmap_ad_samples&sz=640x480&cust_params=sample_ar%3Dpremidpostlongpod&ciu_szs=300x250&gdfp_req=1&ad_rule=1&output=vmap&unviewed_position_start=1&env=vp&impl=s&cmsid=496&vid=short_onecue&correlator="
    ),
    ImaAdTagExample(
      title = "SIMID Survey Pre-roll",
      adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/simid&description_url=https%3A%2F%2Fdevelopers.google.com%2Finteractive-media-ads&sz=640x480&gdfp_req=1&output=vast&unviewed_position_start=1&env=vp&impl=s&correlator="
    ),
    ImaAdTagExample(
      title = "OM SDK Sample Pre-roll",
      adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/omid_ad_samples&env=vp&gdfp_req=1&output=vast&sz=640x480&description_url=http%3A%2F%2Ftest_site.com%2Fhomepage&vpmute=0&vpa=0&vad_format=linear&url=http%3A%2F%2Ftest_site.com&vpos=preroll&unviewed_position_start=1&correlator="
    ),
  )
}
