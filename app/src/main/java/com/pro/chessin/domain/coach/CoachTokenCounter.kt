package com.pro.chessin.domain.coach

import com.knuddels.jtokkit.Encodings
import com.knuddels.jtokkit.api.Encoding
import com.knuddels.jtokkit.api.EncodingType

/**
 * Exact BPE token counter using JTokkit (CL100K_BASE for GPT-3.5/GPT-4 tokenization).
 * Native, zero-dependency BPE tokenizer providing 100% exact OpenAI token counts.
 */
object CoachTokenCounter {

    private val encoding: Encoding by lazy {
        val registry = Encodings.newDefaultEncodingRegistry()
        registry.getEncoding(EncodingType.CL100K_BASE)
    }

    /**
     * Counts the exact number of BPE tokens in the given text string.
     *
     * @param text String to tokenize
     * @return Exact token count
     */
    fun countTokens(text: String): Int {
        if (text.isEmpty()) return 0
        return encoding.countTokens(text)
    }
}
