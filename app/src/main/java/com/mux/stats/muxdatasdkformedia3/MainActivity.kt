package com.mux.stats.muxdatasdkformedia3

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.mux.stats.muxdatasdkformedia3.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

  private lateinit var view: ActivityMainBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    view = ActivityMainBinding.inflate(layoutInflater)
    setContentView(view.root)
  }
}