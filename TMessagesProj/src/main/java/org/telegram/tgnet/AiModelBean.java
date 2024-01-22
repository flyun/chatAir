package org.telegram.tgnet;

/**
 * Created by flyun on 2023/7/12.
 */
public class AiModelBean {

    public final String name;
    public final String tips;
    public final String aiModel;
    public final boolean isShow;

    public AiModelBean(String name, String aiModel, boolean isShow) {
        this.name = name;
        this.aiModel = aiModel;
        this.tips = name;
        this.isShow = isShow;
    }
    public AiModelBean(String name, String aiModel, String tips, boolean isShow) {
        this.name = name;
        this.aiModel = aiModel;
        this.tips = tips;
        this.isShow = isShow;
    }

    public String getName() {
        return name;
    }

    public String getAiModel() {
        return aiModel;
    }
    public String getTips() {
        return tips;
    }

    public boolean isShow() {
        return isShow;
    }
}
