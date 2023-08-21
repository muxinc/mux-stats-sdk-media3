package com.mux.stats.muxdatasdkformedia3.examples.background

import android.content.ComponentName
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.mux.stats.muxdatasdkformedia3.databinding.ActivityBackgroundPlayBinding

class BackgroundPlayActivity : AppCompatActivity() {

  private lateinit var view: ActivityBackgroundPlayBinding
  private var controllerFuture: ListenableFuture<MediaController>? = null
  private val mediaController: MediaController? get() = controllerFuture?.get()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    view = ActivityBackgroundPlayBinding.inflate(layoutInflater)
    setContentView(view.root)
    window.addFlags(View.KEEP_SCREEN_ON)
  }

  override fun onStart() {
    super.onStart()

    val sessionToken = SessionToken(this, ComponentName(this, BackgroundPlayService::class.java))
    val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
    controllerFuture.addListener(
      {
        Log.i("BackgroundPlayExample", "new MediaController created!")
        val controller = controllerFuture.get()!!
        view.playerView.player = controller
        if (!controller.playWhenReady) {
          startPlaying(controller)
        }
      },
      MoreExecutors.directExecutor()
    )
    this.controllerFuture = controllerFuture
  }

  override fun onStop() {
    // Also releases the controller
    controllerFuture?.let { MediaController.releaseFuture(it) }
    controllerFuture = null

    super.onStop()
  }

  private fun startPlaying(controller: MediaController) {
    val mediaItem = MediaItem.Builder().setMediaId("bunny").build()
    controller.setMediaItem(mediaItem)
    controller.prepare()
    controller.playWhenReady = true
  }
}
