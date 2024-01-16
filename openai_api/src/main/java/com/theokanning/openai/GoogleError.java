package com.theokanning.openai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by flyun on 2023/12/17.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleError {

    public GoogleErrorDetails error;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GoogleErrorDetails {
        String message;

        String status;

        String code;
    }
}
