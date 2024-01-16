package com.theokanning.openai.completion.chat;

/**
 * Created by flyun on 2023/12/15.
 *
 * https://ai.google.dev/tutorials/rest_quickstart#multi-turn_conversations_chat
 */
public enum ChatGMessageRole {

    MODEL("model"),
    USER("user");

    private final String value;

    ChatGMessageRole(final String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
