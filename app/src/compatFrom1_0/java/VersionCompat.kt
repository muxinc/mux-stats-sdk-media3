import android.os.Bundle
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ima.ImaServerSideAdInsertionMediaSource

/*
 * VersionCompat.kt: Functions with different Variants for different media3
 * versions. There are other version of this file for other variants. Look
 * in the source set defs for more details
 */

object VersionCompat {
  @androidx.annotation.OptIn(UnstableApi::class)
  fun adsLoaderStateFromBundle(bundle: Bundle): ImaServerSideAdInsertionMediaSource.AdsLoader.State {
    return ImaServerSideAdInsertionMediaSource.AdsLoader.State.CREATOR.fromBundle(bundle)
  }
}
