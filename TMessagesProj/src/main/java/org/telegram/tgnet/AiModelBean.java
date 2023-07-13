package org.telegram.tgnet;

/**
 * Created by flyun on 2023/7/12.
 */
public class AiModelBean {

    public final String name;
    public final String aiModel;
    public final boolean isShow;

    public AiModelBean(String name, String aiModel, boolean isShow) {
        this.name = name;
        this.aiModel = aiModel;
        this.isShow = isShow;
    }

    public String getName() {
        return name;
    }

    public String getAiModel() {
        return aiModel;
    }

    public boolean isShow() {
        return isShow;
    }
}
