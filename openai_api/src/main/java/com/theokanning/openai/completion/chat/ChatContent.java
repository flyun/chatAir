package com.theokanning.openai.completion.chat;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by flyun on 2023/11/12.
 *
 * https://platform.openai.com/docs/guides/vision
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatContent {

    /**
     * Must be either 'text' or 'image_url'.<br>
     * You may use {@link ChatContentType} enum.
     */
    @JsonProperty("type")
    String type;

    @JsonProperty("text")
    String text;

    @JsonProperty("image_url")
    ChatContentImg image_url;

}
