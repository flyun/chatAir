package info.flyun.chatair;

import org.telegram.messenger.ApplicationLoader;

import androidx.multidex.BuildConfig;

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
