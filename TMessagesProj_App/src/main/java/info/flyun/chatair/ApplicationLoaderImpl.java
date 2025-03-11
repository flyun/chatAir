package info.flyun.chatair;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.regular.BuildConfig;

public class ApplicationLoaderImpl extends ApplicationLoader {
    @Override
    protected String onGetApplicationId() {
        return BuildConfig.APPLICATION_ID;
    }
    @Override
    protected String onGetFlavor() {
        return BuildConfig.FLAVOR;
    }
}
