# Mux Data SDK for media3

Mux Data SDK for AndroidX Media3 is an SDK that can observe a media3 `Player` and report state and
player metadata to [Mux Data](https://www.mux.com/data). This SDK is currently in beta, but reports
all playback events, player startup time, experience score, etc. Additional Video Quality metrics
are planned

## Usage

To use this SDK, you must add it as a dependency to your Android project and then monitor it from
the UI component or Service that manages your `Player`.

### Add dependencies

Add our maven repository

```groovy
repositories {
  maven {
    url "https://muxinc.jfrog.io/artifactory/default-maven-release-local"
  }
}
```

Add a dependency on this SDK

```groovy
api 'com.mux.stats.sdk.muxstats:data-media3:1.2.0'
```

### Monitor your player

After you create your `Player` instance, monitor it with `monitorWithMuxData()`.

```kotlin
// from (for example) a MediaSessionService
override fun onCreate() {
  // ...
  player = createMyExoPlayer() // Whatever player init you do
  mediaSession = createMyMediaSession()
  muxStats = monitorPlayer(player)
}

private fun monitorPlayer(player: Player): MuxStatsSdkMedia3<ExoPlayer> {
  // You can add your own data to a View, which will override any data we collect
  val customerData = CustomerData(
    CustomerPlayerData().apply { },
    CustomerVideoData().apply {
      title = "Mux Data SDK for Media3 Demo"
    },
    CustomerViewData().apply { }
  )

  val muxStats = player.monitorWithMuxData(
    context = this,
    envKey = MUX_DATA_ENV_KEY,
    customerData = customerData,
    playerView = view.playerView
  )
  // (beta-only) If you're playing from a MediaSessionService, you'll need to manually set screen & player size
  muxStats.setPlayerSize(...)
  muxStats.setScreenSize(...)

  return muxStats
}

private fun stopPlaying() {
  player?.let { oldPlayer ->
    oldPlayer.stop()
    oldPlayer.release()
  }
  // Make sure to release() your muxStats whenever the user is done with the player
  muxStats?.release()
}
```

## Development

### Contributing

We welcome [pull requests](https://github.com/muxinc/mux-stats-sdk-media3/pulls)
and [issues](https://github.com/muxinc/mux-stats-sdk-media3/issues)! Our codebase
follows [Kotlin's standard coding conventions](https://kotlinlang.org/docs/coding-conventions.html),
which Android Studio should already be configured for. Our only special rule is that we require an
indent width of 2. Your IDE should pick this up automatically.

### Internal Structure

The SDK for media3 is composed mostly of two objects: `MuxStatsSdkMedia3` is the public interface
for use by app developers integrating the SDK. It leverages the observation logic
in `Media3PlayerBinding`, an internal class that observes a `Player` of any type and forwards events
and player state to a core library whose source code can be
found [here](https://github.com/muxinc/stats-sdk-android).

#### API Surface

The surface of this API is contained in `MuxStatsSdkMeda3`, which extends a common `MuxDataSdk`
object in a common core library. This is the class customers should interact with if/when they need
to customize some aspect of SDK behavior. Internally, `MuxStatsSdkMeda3` manages player interaction
with the underlying data tracking and reporting code in
the [android core](https://github.com/muxinc/stats-sdk-android)

#### Player Bindings

`Meda3PlayerBinding` should handle all interaction with the `Player`. It, in turn, is managed by our
[core android library](https://github.com/muxinc/stats-sdk-android). The `PlayerBinding` is bound
when
the outer `MuxStatsSdkMeda3` object is created and unbound when that object is released.

