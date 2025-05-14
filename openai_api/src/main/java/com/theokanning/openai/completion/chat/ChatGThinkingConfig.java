package com.theokanning.openai.completion.chat;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;

@Builder
public class ChatGThinkingConfig {
   @JsonProperty("thinkingBudget")
   Integer thinkingBudget;
   @JsonProperty("includeThoughts")
   Boolean includeThoughts;
}
