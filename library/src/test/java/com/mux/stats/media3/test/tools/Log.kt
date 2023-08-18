package com.mux.core_android.test.tools

fun log(tag: String = "\t", message: String, ex: Throwable? = null) {
  println("$tag :: $message")
  ex?.let {
    print(it)
    println()
  }
}

