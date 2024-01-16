package com.theokanning.openai.completion.chat;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by flyun on 2023/12/16.
 */
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@EqualsAndHashCode(callSuper=false)
public class ChatGMessagePartText {
    @JsonProperty("text")
    String text;

    public ChatGMessagePartText(String text) {
        super();
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
