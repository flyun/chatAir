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
public class ChatGMessagePartInnerData {
    @JsonProperty("mime_type")
    private String mime_type;
    @JsonProperty("data")
    private String data;

}
