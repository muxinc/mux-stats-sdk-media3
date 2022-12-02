package com.mux.stats.muxdatasdkformedia3

import android.net.Uri
import androidx.media3.common.MediaItem

internal fun String.toUri() = Uri.parse(this)
internal fun String.toMediaItem() = MediaItem.fromUri(toUri())
