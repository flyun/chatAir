package com.theokanning.openai.completion.chat;

/**
 * Created by flyun on 2023/12/16.
 */
public enum ChatGFinishReason {

    /** A new and not yet supported value. */
    UNKNOWN("UNKNOWN"),
    /** Reason is unspecified. */
    UNSPECIFIED("UNSPECIFIED"),
    /** Model finished successfully and stopped. */
    STOP("STOP"),
    /** Model hit the token limit. */
    MAX_TOKENS("MAX_TOKENS"),
    /** [SafetySetting]s prevented the model from outputting content. */
    SAFETY("SAFETY"),
    /** Model began looping. */
    RECITATION("RECITATION"),
    /** Model stopped for another reason. */
    OTHER("OTHER");

    private final String value;

    ChatGFinishReason(final String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }


}
