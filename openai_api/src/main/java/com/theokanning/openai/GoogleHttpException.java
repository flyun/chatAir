package com.theokanning.openai;

public class GoogleHttpException extends RuntimeException {

    public final String code;

    public final String status;


    public GoogleHttpException(GoogleError error, Throwable parent) {
        super(error.error.message, parent);
        this.code = error.error.code;
        this.status = error.error.status;
    }
}
