package com.mux.stats.muxdatasdkformedia3

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mux.stats.muxdatasdkformedia3.databinding.ActivityMainBinding
import com.mux.stats.muxdatasdkformedia3.databinding.ListitemExampleBinding
import com.mux.stats.muxdatasdkformedia3.examples.background.BackgroundPlayActivity
import com.mux.stats.muxdatasdkformedia3.examples.basic.BasicPlayerActivity
import com.mux.stats.muxdatasdkformedia3.examples.ima.ImaAdsActivity

class MainActivity : AppCompatActivity() {

  private lateinit var view: ActivityMainBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    view = ActivityMainBinding.inflate(layoutInflater)
    setContentView(view.root)
    view.recycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    view.recycler.adapter = ExampleListAdapter(this, examples())
  }

  private fun examples() = listOf(
    Example(
      title = "Basic playback",
      destination = Intent(this, BasicPlayerActivity::class.java)
    ),
    Example(
      title = "IMA Ads",
      destination = Intent(this, ImaAdsActivity::class.java)
    ),
    Example(
      title = "Live Playback",
      destination = Intent(this, BasicPlayerActivity::class.java).apply {
        putExtra(
          BasicPlayerActivity.EXTRA_URL,
          "https://stream.mux.com/v69RSHhFelSm4701snP22dYz2jICy4E4FUyk02rW4gxRM.m3u8"
        )
      }
    ),
    // TODO: post-beta, add APIs for talking to a `MediaSessionService`
//    Example(
//      title = "Background playback",
//      destination = Intent(this, BackgroundPlayActivity::class.java)
//    ),
  )
}

data class Example(
  val title: String,
  val destination: Intent
)

class ExampleListAdapter(
  private val context: Context,
  private val examples: List<Example>
) : RecyclerView.Adapter<ExampleListAdapter.Holder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
    val viewBinding = ListitemExampleBinding.inflate(
      LayoutInflater.from(context),
      parent,
      false
    )
    return Holder(
      viewBinding = viewBinding,
      itemView = viewBinding.root
    )
  }

  override fun getItemCount(): Int = examples.size

  override fun onBindViewHolder(holder: Holder, position: Int) {
    val example = examples[position]
    holder.viewBinding.exampleName.text = example.title
    holder.itemView.setOnClickListener { context.startActivity(example.destination) }
  }

  class Holder(
    val itemView: View,
    val viewBinding: ListitemExampleBinding
  ) : RecyclerView.ViewHolder(itemView)
}
