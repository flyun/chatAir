package com.theokanning.openai.completion.chat;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by flyun on 2023/12/16.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatGCompletionResponse {

    @JsonProperty("candidates")
    List<ChatGCandidate> candidates;

    //promptFeedback
}
