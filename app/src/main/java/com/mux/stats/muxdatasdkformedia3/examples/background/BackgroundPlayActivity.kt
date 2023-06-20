package com.mux.stats.muxdatasdkformedia3.examples.background

import android.content.ComponentName
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.mux.stats.muxdatasdkformedia3.R
import com.mux.stats.muxdatasdkformedia3.databinding.ActivityBackgroundPlayBinding

class BackgroundPlayActivity : AppCompatActivity() {

  private lateinit var view: ActivityBackgroundPlayBinding
  private var controllerFuture: ListenableFuture<MediaController>? = null
  private val mediaController: MediaController? get() = controllerFuture?.get()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    view = ActivityBackgroundPlayBinding.inflate(layoutInflater)
    setContentView(view.root)
  }

  override fun onStart() {
    super.onStart()

    val sessionToken = SessionToken(this, ComponentName(this, ExamplePlayerService::class.java))
    val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
    this.controllerFuture = controllerFuture
    controllerFuture.addListener(
      {
        val controller = controllerFuture.get()!!
        view.playerView.player = controller
      },
      MoreExecutors.directExecutor()
    )
  }

  override fun onStop() {
    // Also releases
    controllerFuture?.let { MediaController.releaseFuture(it) }

    super.onStop()
  }

}