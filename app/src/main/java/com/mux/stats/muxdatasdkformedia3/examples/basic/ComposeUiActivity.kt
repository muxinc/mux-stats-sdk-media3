package com.mux.stats.muxdatasdkformedia3.examples.basic

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.common.collect.Queues
import com.mux.stats.muxdatasdkformedia3.Constants
import com.mux.stats.muxdatasdkformedia3.examples.basic.ui.theme.MuxDataSDKForMedia3Theme
import com.mux.stats.sdk.core.model.CustomerData
import com.mux.stats.sdk.core.model.CustomerPlayerData
import com.mux.stats.sdk.core.model.CustomerVideoData
import com.mux.stats.sdk.core.model.CustomerViewData
import com.mux.stats.sdk.muxstats.MuxStatsSdkMedia3
import com.mux.stats.sdk.muxstats.monitorWithMuxData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ComposeUiExampleActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      MuxDataSDKForMedia3Theme {
        // A surface container using the 'background' color from the theme
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          //Greeting("Android")
          VideoSwitchingScreen()
        }
      }
    }
  }
}

private fun monitorPlayer(
  context: Context,
  playerView: View? = null,
  player: ExoPlayer
): MuxStatsSdkMedia3<ExoPlayer> {
  // You can add your own data to a View, which will override any data we collect
  val customerData = CustomerData(
    CustomerPlayerData().apply { },
    CustomerVideoData().apply {
      videoId = "A Custom ID"
    },
    CustomerViewData().apply { }
  )

  return player.monitorWithMuxData(
    context = context,
    envKey = Constants.MUX_DATA_ENV_KEY,
    customerData = customerData,
    playerView = playerView
  )
}

private fun createPlayer(context: Context): ExoPlayer {
  return ExoPlayer.Builder(context)
    .build().apply {
      addListener(object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
          Log.e(javaClass.simpleName, "player error!", error)
          Toast.makeText(context, error.localizedMessage, Toast.LENGTH_SHORT)
            .show()
        }
      })
    }
}

// todo:
//  remember mediaUri by rememberUpdatedState & LaunchedEffect(or DisposableEffect?), change
//    MediaItem when it changes This LaunchedEffect(or DisposableEffect?) sets the MediaItem
//  rememberUpdatedState on the playerProvider, but doesn't need to be key on LaunchedEffect
//  one level up: put an exoplayer + muxStats into a DisposableEffect. (each their own maybe?)
//    When the mediaUri changes, call videoChange (or dispose of the MuxStats)

val videoList = listOf(
  VideoInformation(Uri.parse(Constants.VOD_TEST_URL_DRAGON_WARRIOR_LADY), "Sintel"),
  VideoInformation(Uri.parse(Constants.VOD_TEST_URL_BIG_BUCK_BUNNY), "Big Buck Bunny"),
  VideoInformation(Uri.parse(Constants.VOD_TEST_URL_STEVE), "Old Keynote"),
)

/**
 * A Screen that allows switching between a number of videos
 */
@Composable
fun VideoSwitchingScreen(
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  // set when context or lifecycle owner change, in the DisposableEffect
  val currentPlayer = remember { mutableStateOf<PlayerStatsPair?>(null) }
  // set when a button is clicked to cycle between videos
  val currentVideoIdx = remember { mutableIntStateOf(0) }

  DisposableEffect(key1 = context, key2 = lifecycleOwner) {
    val player = createPlayer(context = context)
    val muxStats = monitorPlayer(context = context, player = player)
    currentPlayer.value = PlayerStatsPair(player, muxStats)

    onDispose {
      Log.d(javaClass.simpleName, "Disposing of player and monitor")

      muxStats.release()
      player.release()
    }
  }

  Column(modifier) {
    val videoInformation = videoList[currentVideoIdx.intValue]

    Row(
      modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
      Text(
        buildAnnotatedString {
          withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) { "Current Video: " }
          videoInformation.title
        }
      )
      Button(onClick = {
        val nextIdx = if (currentVideoIdx.intValue >= videoList.size) {
          0
        } else {
          currentVideoIdx.intValue + 1
        }
        currentVideoIdx.intValue = nextIdx
      }) {
        Text("Change video")
      }

      VideoPlayer(
        info = videoInformation,
        playerProvider = { currentPlayer.value?.player },
        muxStatsProvider = { currentPlayer.value?.muxStatsSdkMedia3 },
        configurePlayerView = { },
        modifier = modifier
          .fillMaxWidth()
          .height(500.dp)
      )
    }
  }
}

@Composable
fun VideoPlayer(
  info: VideoInformation,
  playerProvider: () -> ExoPlayer?,
  muxStatsProvider: () -> MuxStatsSdkMedia3<ExoPlayer>?,
  configurePlayerView: (PlayerView) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier.padding(vertical = 4.dp)
  ) {
    val recentPlayerProvider = rememberUpdatedState(newValue = playerProvider)
    val recentMuxStatsProvider = rememberUpdatedState(newValue = muxStatsProvider)
    val player = recentPlayerProvider.value.invoke()
    val muxStats = recentMuxStatsProvider.value.invoke()

    if (player != null) {
      LaunchedEffect(key1 = info) {
        launch(Dispatchers.Main) {
          player.stop()

          // todo - or reset the muxStats, but you'd have to pass it back up
          muxStats?.videoChange(CustomerVideoData().apply {
            videoSourceUrl = info.toString()
          })

          player.setMediaItem(MediaItem.fromUri(info.uri))
          player.playWhenReady = true
          player.prepare()
        }
      }
      AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
          val view = PlayerView(context)
          configurePlayerView(view)
          view
        },
        update = { view -> view.player = player },
        onReset = { view -> view.player = null },
        // nothing to do here, release player and muxStats when parent leaves composition
        // onRelease = { view -> }
      )
    }
  }
}

data class PlayerStatsPair(
  val player: ExoPlayer,
  val muxStatsSdkMedia3: MuxStatsSdkMedia3<ExoPlayer>
)

data class VideoInformation(
  val uri: Uri,
  val title: String
)

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(
    text = "Hello $name!",
    modifier = modifier
  )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  MuxDataSDKForMedia3Theme {
    Greeting("Android")
  }
}

class PlayerPool(val size: Int) {
  private val deque = Queues.newArrayBlockingQueue<ExoPlayer>(size)

  fun obtain(): ExoPlayer? {
    return deque.poll()
  }

  fun giveBack(player: ExoPlayer): Boolean {
    return deque.offer(player)
  }

  fun releaseAllPlayers() {
    deque.forEach {
      it.stop()
      it.release()
    }
  }
}