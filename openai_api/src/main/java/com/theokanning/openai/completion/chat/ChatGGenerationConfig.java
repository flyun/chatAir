package com.theokanning.openai.completion.chat;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by flyun on 2023/12/16.
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
/**
 * Helper method to construct a [GenerationConfig] in a DSL-like manner.
 *
 * Example Usage:
 * ```
 * generationConfig {
 *   temperature = 0.75f
 *   topP = 0.5f
 *   topK = 30
 *   candidateCount = 4
 *   maxOutputTokens = 300
 *   stopSequences = listOf("in conclusion", "-----", "do you need")
 * }
 * ```
 *
 * Configuration parameters to use for content generation.
 *
 *  temperature The degree of randomness in token selection, typically between 0 and 1
 *  topK The sum of probabilities to collect to during token selection
 *  topP How many tokens to select amongst the highest probabilities
 *  candidateCount The max *unique* responses to return
 *  maxOutputTokens The max tokens to generate per response
 *  stopSequences A list of strings to stop generation on occurrence of
 */
public class ChatGGenerationConfig {


    @JsonProperty("stop_sequences")
    List<String> stop_sequences;
    @JsonProperty("temperature")
    Double temperature;
    @JsonProperty("max_output_tokens")
    Integer max_output_tokens;
    @JsonProperty("candidate_count")
    Integer candidate_count;
    @JsonProperty("topP")
    Float topP;
    @JsonProperty("topK")
    Integer topK;
}
