package com.mux.stats.sdk.muxstats

import com.mux.stats.sdk.muxstats.bandwidth.TrackedHeader
import org.junit.Assert
import org.junit.Test
import java.util.regex.Pattern

class TrackedHeaderTest {

  @Test
  fun testAllowedHeaderString() {
    val headerNameMatches = "real-header"
    val headerNameDoesntMatch = "not-the-right-header"
    val spec = TrackedHeader.ExactlyIgnoreCase("real-header")

    Assert.assertFalse(
      "only exact matches should be allowed",
      spec.matches(headerNameDoesntMatch)
    )
    Assert.assertTrue(
      "only exact matches should be allowed",
      spec.matches(headerNameMatches)
    )
    Assert.assertTrue(
      "matching is case-insensitive",
      spec.matches(headerNameMatches.uppercase())
    )
  }

  @Test
  fun testAllowedHeaderPattern() {
    val headerNameMatches = "x-litix-session-id"
    val headerNameDoesntMatch = "fastcdn-log-tag-id"
    val spec = TrackedHeader.Matching(Pattern.compile("^x-litix.*"))

    Assert.assertFalse(
      "only headers matching the regex are allowed",
      spec.matches(headerNameDoesntMatch)
    )
    Assert.assertTrue(
      "only headers matching the regex are allowed",
      spec.matches(headerNameMatches)
    )
  }
}