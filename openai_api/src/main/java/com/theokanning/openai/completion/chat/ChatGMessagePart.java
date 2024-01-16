package com.theokanning.openai.completion.chat;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by flyun on 2023/12/15.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatGMessagePart{

    @JsonProperty("text")
    private String text;
    @JsonProperty("inline_data")
    private ChatGMessagePartInnerData inline_data;

}
