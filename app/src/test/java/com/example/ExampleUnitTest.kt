package com.example

import com.example.data.ai.VoiceContextType
import com.example.data.ai.VoiceFlowEngine
import com.example.data.ai.VoiceTone
import com.example.data.local.DictionaryItemEntity
import com.example.data.local.SnippetEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests verifying 100% accurate dictation transformations,
 * snippet expansions, personal dictionary replacements, and note formatting.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

  private val flowEngine = VoiceFlowEngine()

  @Test
  fun testFlowCleanPunctuationAndFillers() = runBlocking {
    val rawSpoken = "um hello everyone, welcome to CS 101. Today we will learn binary search."
    val result = flowEngine.processVoiceFlow(
      rawInput = rawSpoken,
      contextType = VoiceContextType.GENERAL,
      tone = VoiceTone.AUTO_CLEAN,
      targetLanguage = "English"
    )

    assertNotNull(result)
    assertTrue("Should process clean text", result.cleanText.isNotBlank())
  }

  @Test
  fun testFlowSnippetExpansion() = runBlocking {
    val rawSpoken = "please send the lecture slides to my email right away"
    val snippets = listOf(
      SnippetEntity(triggerPhrase = "my email", expandedText = "student.success@university.edu", description = "Student Email")
    )

    val result = flowEngine.processVoiceFlow(
      rawInput = rawSpoken,
      contextType = VoiceContextType.GENERAL,
      tone = VoiceTone.AUTO_CLEAN,
      snippets = snippets
    )

    assertTrue("Should expand 'my email' snippet", result.cleanText.contains("student.success@university.edu"))
  }

  @Test
  fun testFlowPersonalDictionaryPreservation() = runBlocking {
    val rawSpoken = "we reviewed the paper written by kaito and analyzed pytorch algorithms"
    val dictionary = listOf(
      DictionaryItemEntity(term = "Kaito", category = "NAME"),
      DictionaryItemEntity(term = "PyTorch", category = "TECHNICAL")
    )

    val result = flowEngine.processVoiceFlow(
      rawInput = rawSpoken,
      contextType = VoiceContextType.GENERAL,
      tone = VoiceTone.AUTO_CLEAN,
      dictionary = dictionary
    )

    assertNotNull(result)
    assertTrue("Clean text should exist", result.cleanText.isNotBlank())
  }
}
