package com.mux.stats.muxdatasdkformedia3

import android.net.Uri
import androidx.media3.common.MediaItem

fun String.toUri(): Uri = Uri.parse(this)
fun String.toMediaItem() = MediaItem.fromUri(toUri())
