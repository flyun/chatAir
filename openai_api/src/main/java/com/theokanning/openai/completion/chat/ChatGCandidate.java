package com.theokanning.openai.completion.chat;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by flyun on 2023/12/16.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatGCandidate {

    /**
     * see {@link ChatGFinishReason} documentation.
     */
    // 大小写转换有问题（根据setXxx获取首字母大小写），需要加@JsonProperty进行注解
    @JsonProperty("finishReason")
    String finishReason;
    @JsonProperty("index")
    int index;
    @JsonProperty("content")
    ChatGMessage content;

}
