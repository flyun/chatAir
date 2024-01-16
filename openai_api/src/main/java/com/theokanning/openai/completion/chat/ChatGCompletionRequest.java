package com.theokanning.openai.completion.chat;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by flyun on 2023/12/15.
 *
 * https://ai.google.dev/tutorials/rest_quickstart#configuration
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatGCompletionRequest {

    @JsonProperty("contents")
    List<ChatGMessage> contents;

    @JsonProperty("generationConfig")
    ChatGGenerationConfig generationConfig;

//    ChatGSafetySetting safetySettings;

}
