package com.mux.stats.muxdatasdkformedia3

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.mux.stats.muxdatasdkformedia3.databinding.ActivityPlayerBinding

class PlayerActivity : AppCompatActivity() {

  private lateinit var view: ActivityPlayerBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    view = ActivityPlayerBinding.inflate(layoutInflater)
    setContentView(view.root)
  }
}