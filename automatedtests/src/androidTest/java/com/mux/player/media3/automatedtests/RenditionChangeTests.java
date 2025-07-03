package com.mux.player.media3.automatedtests;

import static org.junit.Assert.fail;

import android.util.Log;
import androidx.media3.common.Format;
import com.mux.stats.sdk.core.events.playback.PlayingEvent;
import com.mux.stats.sdk.core.events.playback.RenditionChangeEvent;
import com.mux.stats.sdk.core.model.VideoData;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class RenditionChangeTests extends AdaptiveBitStreamTestBase {

  static final String TAG = "RenditionChangeTests";

  @Before
  public void init() {
    urlToPlay = "http://localhost:5000/hls/google_glass/playlist.m3u8";
    // These video have larger bitrate, make sure we do not cause any
    // rebuffering due to low bandwith
    bandwidthLimitInBitsPerSecond = 12000000;
    super.init();
  }

  @Test
  public void testRenditionChange() {
    try {
      if (!testActivity.waitForPlaybackToStart(waitForPlaybackToStartInMS)) {
        fail("Playback did not start in " + waitForPlaybackToStartInMS + " milliseconds !!!");
      }
      Thread.sleep(PLAY_PERIOD_IN_MS);
      // Switch rendition
      int startingFmtIndex = getSelectedRenditionIndex();
      ArrayList<Format> availableFormats = getAvailableVideoRendition();
      Format startingFmt = availableFormats.get(startingFmtIndex);
      int nextFmtIndex;
      if (startingFmtIndex == availableFormats.size() - 1) {
        nextFmtIndex = startingFmtIndex - 1;
      } else {
        nextFmtIndex = startingFmtIndex + 1;
      }
      Format changedFmt = availableFormats.get(nextFmtIndex);
      switchRenditionToIndex(nextFmtIndex);
      Thread.sleep(PLAY_PERIOD_IN_MS);
      int renditionChangeIndex = 0;
      int playinIndex = networkRequest.getIndexForFirstEvent(PlayingEvent.TYPE);
      JSONArray receivedRenditionChangeEvents = new JSONArray();
      Log.d(TAG, "Received events: " + networkRequest.getReceivedEventNames());
      while (true) {
        renditionChangeIndex = networkRequest
            .getIndexForNextEvent(renditionChangeIndex + 1, RenditionChangeEvent.TYPE);

        Log.w(TAG, "renditionchange time: " + networkRequest.getCreationTimeForEvent(renditionChangeIndex));
        Log.w(TAG, "playing time: " + networkRequest.getCreationTimeForEvent(playinIndex));
        long lastRenditionChangeAt = networkRequest.getCreationTimeForEvent(renditionChangeIndex)
              - networkRequest.getCreationTimeForEvent(playinIndex);
        Log.w(TAG, "last rendition change at: " + lastRenditionChangeAt);

        // TODO: OK, so the server responding with 206/partial content changes the timing of the
        //  rendntionchange events, maybe because it finishes the current segment before changing
        //  renditions. This is good player behavior, and responding 200/OK is good server behavior
        //  but it appears that our test relies on the server responding with 206/partial content
        //  I think just change the logic here. The first rc after the first playing evt is ours
        if (renditionChangeIndex == -1) {
          fail("Failed to find RenditionChangeEvent dispatched after: "
              + PLAY_PERIOD_IN_MS + " ms since playback started, with valid data"
              + ", received events: " + receivedRenditionChangeEvents.length() + ": "
              + receivedRenditionChangeEvents);
        }

        JSONObject jo = networkRequest.getEventForIndex(renditionChangeIndex);
        receivedRenditionChangeEvents.put(jo);
        if (Math.abs(lastRenditionChangeAt - PLAY_PERIOD_IN_MS) < 500) {
          // We found rendition change index we ware looking for, there may be more after,
          // because I dont know how to controll the player bitadaptive settings
          if (!jo.has(VideoData.VIDEO_SOURCE_WIDTH) || !jo.has(VideoData.VIDEO_SOURCE_HEIGHT)) {
            Log.w(TAG,
                "Missing video width and/or video height parameters on Rendition change event, "
                    + " json: " + jo.toString());
            continue;
          }
          break;
        }
      }

      JSONObject jo = networkRequest.getEventForIndex(renditionChangeIndex);
      int videoWidth = jo.getInt(VideoData.VIDEO_SOURCE_WIDTH);
      int videoHeight = jo.getInt(VideoData.VIDEO_SOURCE_HEIGHT);
      if (videoWidth != changedFmt.width && videoHeight != changedFmt.height) {
        fail("Last reported rendition change width and height (" + videoWidth + "x" +
            videoHeight + ") do not match requested format resolution: (" +
            changedFmt.width + "x" + changedFmt.height + ")");
      }
    } catch (Exception e) {
      fail(getExceptionFullTraceAndMessage(e));
    }
  }


}
