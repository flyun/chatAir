package com.theokanning.openai.completion.chat;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by flyun on 2023/12/16.
 */
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
public class ChatGMessagePartBlob {
    @JsonProperty("mime_type")
    private String mime_type;
    @JsonProperty("data")
    private String data;

    public ChatGMessagePartBlob(String mime_type, String data) {
        super(); // invoke parent class constructor
        this.mime_type = mime_type;
        this.data = data;
    }

    public String getMime_type() {
        return mime_type;
    }

    public void setMime_type(String mime_type) {
        this.mime_type = mime_type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
