package com.mux.stats.muxdatasdkformedia3

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mux.stats.muxdatasdkformedia3.databinding.ActivityMainBinding
import com.mux.stats.muxdatasdkformedia3.databinding.ListitemExampleBinding
import com.mux.stats.muxdatasdkformedia3.examples.basic.BasicPlayerActivity
import com.mux.stats.muxdatasdkformedia3.examples.basic.ComposeUiExampleActivity
import com.mux.stats.muxdatasdkformedia3.examples.basic.PlayerReuseActivity
import com.mux.stats.muxdatasdkformedia3.examples.ima.ImaClientAdsActivity
import com.mux.stats.muxdatasdkformedia3.examples.ima.ImaServerAdsActivity

class MainActivity : AppCompatActivity() {

  private lateinit var view: ActivityMainBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    view = ActivityMainBinding.inflate(layoutInflater)
    setContentView(view.root)

    ViewCompat.setOnApplyWindowInsetsListener(view.root) { v, insets ->
      val bars = insets.getInsets(
        WindowInsetsCompat.Type.systemBars()
            or WindowInsetsCompat.Type.displayCutout()
      )
      v.updatePadding(
        top = bars.top,
        bottom = bars.bottom,
      )
      WindowInsetsCompat.CONSUMED
    }

    view.recycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    view.recycler.adapter = ExampleListAdapter(this, examples())
  }

  private fun examples() = listOf(
    Example(
      title = "Basic playback",
      destination = Intent(this, BasicPlayerActivity::class.java)
    ),
    Example(
      title = "IMA Ads (CSAI, most common)",
      destination = Intent(this, ImaClientAdsActivity::class.java)
    ),
    Example(
      title = "IMA Ads (DAI/SSAI, less common)",
      destination = Intent(this, ImaServerAdsActivity::class.java)
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
    Example(
      title = "Compose UI With Shared Player",
      destination = Intent(this, ComposeUiExampleActivity::class.java)
    ),
    Example(
      title = "Reusing a Player for multiple MediaItems",
      destination = Intent(this, PlayerReuseActivity::class.java),
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
      root = viewBinding.root
    )
  }

  override fun getItemCount(): Int = examples.size

  override fun onBindViewHolder(holder: Holder, position: Int) {
    val example = examples[position]
    holder.viewBinding.exampleName.text = example.title
    holder.viewBinding.root.setOnClickListener { context.startActivity(example.destination) }
  }

  class Holder(
    root: View,
    val viewBinding: ListitemExampleBinding
  ) : RecyclerView.ViewHolder(root)
}
