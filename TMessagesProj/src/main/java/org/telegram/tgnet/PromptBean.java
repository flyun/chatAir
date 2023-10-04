package org.telegram.tgnet;

/**
 * Created by flyun on 2023/9/27.
 */
public class PromptBean {
    public final String title;
    public final String description;
    public final String content;


    public PromptBean(String title, String description, String content) {
        this.title = title;
        this.description = description;
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getContent() {
        return content;
    }
}
