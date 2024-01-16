package com.theokanning.openai.completion.chat;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by flyun on 2023/11/13.
 *
 * https://platform.openai.com/docs/guides/vision
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatContentImg {

    @JsonProperty("url")
    String url;

}
