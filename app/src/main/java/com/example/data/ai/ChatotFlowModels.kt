package com.example.data.ai

enum class WisprContextType(val displayName: String, val iconDesc: String) {
    GENERAL("General", "Clean, balanced text"),
    EMAIL("Email", "Structured greeting, body, and sign-off"),
    SLACK_MESSAGE("Slack / Chat", "Brevity, concise bullets, friendly chat tone"),
    LINKEDIN_POST("LinkedIn Post", "Hook, spacing, insights, engaging call-to-action"),
    MEETING_NOTE("Meeting Note", "Executive summary, decisions, action items"),
    LONG_CONTEXT_SYNC("Deep Meeting / Deposition", "Comprehensive multi-topic analysis with timeline & risks"),
    TASK_LIST("Task List", "Numbered actionable to-do checklist"),
    AI_PROMPT("AI Prompt", "High-yield structured system prompt with constraints"),
    SUPPORT_REPLY("Customer Support", "Empathetic, clear, step-by-step resolution"),
    JOURNAL("Journal Note", "Reflective, organized thoughts and reflections")
}

typealias ChatotContextType = WisprContextType

enum class WisprTone(val displayName: String, val description: String, val promptInstruction: String) {
    AUTO_CLEAN("Auto Clean", "Natural clarity & filler removal", "Clean speech disfluencies, remove filler words (um, uh, you know, like), fix false starts, and maintain natural speaking flow with proper punctuation."),
    FORMAL("Formal", "Executive, polished & precise", "Write in a formal, polished, professional, and grammatically impeccable executive tone. Use authoritative, precise terminology suitable for client memos, legal debriefs, and leadership review."),
    CASUAL("Casual", "Friendly, conversational & natural", "Write in a friendly, natural, approachable, and conversational tone while retaining clarity and removing speech filler words."),
    CONCISE("Concise", "High-density, zero fluff", "Write in an ultra-concise, high-density format with zero fluff or conversational filler. Prioritize bulleted key decisions and direct takeaways."),
    PROFESSIONAL("Professional", "Polished workplace tone", "Write in a clear, courteous, and crisp professional tone for business and client communications.")
}

typealias ChatotTone = WisprTone

enum class WisprTransform(val displayName: String, val promptInstruction: String) {
    POLISH("Polish & Elevate", "Fix grammar, polish syntax, and enhance readability while preserving tone"),
    CONCISE("Make Concise", "Remove fluff and redundancy, keep strictly the core message in half the words"),
    FIX_GRAMMAR("Fix Grammar Only", "Fix only typos, punctuation, and grammar without changing style or vocabulary"),
    BULLETS("Turn into Bullets", "Convert all key points and thoughts into clear, structured bullet points"),
    TO_EMAIL("Convert to Email", "Draft a professional email with subject line, greeting, body paragraphs, and sign-off"),
    TO_TASKS("Extract Action Items", "Extract every task, deliverable, and action item as a formatted checklist with owners and deadlines"),
    TO_LINKEDIN("LinkedIn Post", "Draft a compelling LinkedIn post with an engaging hook, readable line breaks, key takeaways, and hashtags"),
    TO_PROMPT("AI Prompt Builder", "Transform this rough voice thought into an expertly engineered AI prompt with role, context, constraints, and output format"),
    SUMMARIZE("Executive Summary", "Synthesize into a 2-3 sentence executive summary followed by key highlights"),
    LONG_FORM_SYNTHESIS("Extended Deep Synthesis", "Synthesize long-form multi-speaker transcript into complete meeting minutes, decisions, milestones, risks, and follow-up email"),
    TRANSLATE_EN("Translate to English", "Accurately translate any multilingual, Hinglish, or foreign text into natural, idiomatic English")
}

typealias ChatotTransform = WisprTransform

data class StructuredMeetingNotes(
    val title: String = "Meeting Minutes",
    val executiveSummary: String = "",
    val actionItems: List<String> = emptyList(),
    val keyDecisions: List<String> = emptyList(),
    val peopleMentioned: List<String> = emptyList(),
    val datesAndDeadlines: List<String> = emptyList(),
    val timelineHighlights: List<String> = emptyList(),
    val risksAndBlockers: List<String> = emptyList(),
    val followUpEmailDraft: String = ""
)

data class WisprFlowResult(
    val rawTranscript: String = "",
    val cleanText: String = "",
    val formattedText: String = "",
    val toneRewrittenText: String? = null,
    val toneUsed: WisprTone = WisprTone.AUTO_CLEAN,
    val structuredMeeting: StructuredMeetingNotes? = null,
    val builtAiPrompt: String? = null,
    val appliedSnippets: List<String> = emptyList(),
    val selfCorrectionsFound: List<String> = emptyList(),
    val actionItems: List<String> = emptyList(),
    val detectedLanguage: String = "English",
    val isExtendedContext: Boolean = false,
    val tokenCountEstimate: Int = 0,
    val latencyMs: Long = 0L,
    val audioFilePath: String? = null
)

typealias ChatotFlowResult = WisprFlowResult
