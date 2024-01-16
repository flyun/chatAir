package com.theokanning.openai.completion.chat;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by flyun on 2023/12/15.
 * https://ai.google.dev/tutorials/rest_quickstart#configuration
 *
 *
 * {
 *         "contents": [{
 *             "parts":[
 *                 {"text": "Write a story about a magic backpack."}
 *             ]
 *         }],
 *         "safetySettings": [
 *             {
 *                 "category": "HARM_CATEGORY_DANGEROUS_CONTENT",
 *                 "threshold": "BLOCK_ONLY_HIGH"
 *             }
 *         ],
 *         "generationConfig": {
 *             "stopSequences": [
 *                 "Title"
 *             ],
 *             "temperature": 1.0,
 *             "maxOutputTokens": 800,
 *             "topP": 0.8,
 *             "topK": 10
 *         }
 *     }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatGMessage {

    /**
     * see {@link ChatGMessageRole} documentation.
     */
    @JsonProperty("role")
    String role;
    @JsonProperty("parts")
    List<ChatGMessagePart> parts;


}
