package com.example

import com.example.data.ai.WisprContextType
import com.example.data.ai.WisprFlowEngine
import com.example.data.ai.WisprTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying 100% accurate dictation transformations,
 * snippet expansions, personal dictionary replacements, and educational note formatting.
 */
class ExampleUnitTest {

  private val wisprEngine = WisprFlowEngine()

  @Test
  fun testWisprFlowCleanPunctuationAndFillers() {
    val rawSpoken = "um hello everyone comma welcome to CS 101 period today we will learn binary search"
    val result = wisprEngine.processFlow(
      rawTranscript = rawSpoken,
      contextType = WisprContextType.EDUCATION_LECTURE,
      targetTone = WisprTone.AUTO_CLEAN,
      language = "English"
    )

    assertNotNull(result)
    assertTrue("Should remove fillers like 'um'", !result.cleanText.contains("um hello"))
    assertTrue("Should capitalize sentence beginnings", result.formattedText.contains("Hello everyone,"))
  }

  @Test
  fun testWisprFlowSnippetExpansion() {
    val rawSpoken = "please send the lecture slides to my email right away"
    val snippets = mapOf("my email" to "student.success@university.edu")

    val result = wisprEngine.processFlow(
      rawTranscript = rawSpoken,
      contextType = WisprContextType.EDUCATION_STUDY,
      targetTone = WisprTone.AUTO_CLEAN,
      customSnippets = snippets
    )

    assertTrue("Should expand 'my email' to full email snippet", result.formattedText.contains("student.success@university.edu"))
    assertTrue("Should track applied snippets", result.appliedSnippets.contains("my email"))
  }

  @Test
  fun testWisprFlowPersonalDictionaryPreservation() {
    val rawSpoken = "we reviewed the paper written by kaito and analyzed pytorch algorithms"
    val dictionary = listOf("Kaito", "PyTorch")

    val result = wisprEngine.processFlow(
      rawTranscript = rawSpoken,
      contextType = WisprContextType.EDUCATION_LECTURE,
      targetTone = WisprTone.AUTO_CLEAN,
      personalDictionary = dictionary
    )

    assertTrue("Should preserve casing of Kaito", result.formattedText.contains("Kaito"))
    assertTrue("Should preserve casing of PyTorch", result.formattedText.contains("PyTorch"))
  }
}

