package org.telegram.tgnet;

/**
 * Created by flyun on 2023/9/27.
 */
public class PromptBean {
    public final int num;
    public final String title;
    public final String description;
    public final String content;


    public PromptBean(int num, String title, String description, String content) {
        this.num = num;
        this.title = title;
        this.description = description;
        this.content = content;
    }

    public int getNum() {
        return num;
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
