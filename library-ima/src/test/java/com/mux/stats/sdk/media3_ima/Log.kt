package com.mux.stats.sdk.media3_ima

fun log(tag: String = "\t", message: String, ex: Throwable? = null) {
  println("$tag :: $message")
  ex?.let {
    print(it)
    println()
  }
}

