package org.telegram.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

/**
 * Created by flyun on 2023/9/17.
 */
class ChangeCustomModelActivity extends BaseFragment {

    private EditTextBoldCursor firstNameField;
    private View doneButton;

    private long userId = 0;
    private String customModel;

    private Theme.ResourcesProvider resourcesProvider;

    private final static int done_button = 1;

    public ChangeCustomModelActivity(Bundle args, Theme.ResourcesProvider resourcesProvider) {
        super(args);
        this.resourcesProvider = resourcesProvider;
    }

    public ChangeCustomModelActivity(Theme.ResourcesProvider resourcesProvider) {
        this.resourcesProvider = resourcesProvider;
    }

    @Override
    public boolean onFragmentCreate() {
        if (arguments != null) {
            userId = arguments.getLong("user_id", 0);
        }
        if (userId != 0) {
            customModel = arguments.getString("custom_model", "");
        } else {
            customModel = UserConfig.getInstance(currentAccount).customModel;
        }
        return super.onFragmentCreate();
    }

    @Override
    public View createView(Context context) {
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_avatar_actionBarSelectorBlue, resourcesProvider), false);
        actionBar.setItemsColor(Theme.getColor(Theme.key_actionBarDefaultIcon, resourcesProvider), false);
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("ChangeCustomModel", R.string.ChangeCustomModel));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == done_button) {
                    if (firstNameField.getText() != null) {
                        saveCustomModel();
                    }
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        doneButton = menu.addItemWithWidth(done_button, R.drawable.ic_ab_done, AndroidUtilities.dp(56), LocaleController.getString("Done", R.string.Done));

        LinearLayout linearLayout = new LinearLayout(context);
        fragmentView = linearLayout;
        fragmentView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ((LinearLayout) fragmentView).setOrientation(LinearLayout.VERTICAL);
        fragmentView.setOnTouchListener((v, event) -> true);

        firstNameField = new EditTextBoldCursor(context) {
            @Override
            protected Theme.ResourcesProvider getResourcesProvider() {
                return resourcesProvider;
            }
        };
        firstNameField.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        firstNameField.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText, resourcesProvider));
        firstNameField.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        firstNameField.setBackgroundDrawable(null);
        firstNameField.setLineColors(getThemedColor(Theme.key_windowBackgroundWhiteInputField), getThemedColor(Theme.key_windowBackgroundWhiteInputFieldActivated), getThemedColor(Theme.key_windowBackgroundWhiteRedText3));
        firstNameField.setMaxLines(1);
        firstNameField.setLines(1);
        firstNameField.setSingleLine(true);
        firstNameField.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        firstNameField.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
        firstNameField.setHint(LocaleController.getString("CustomModel", R.string.CustomModel));
        firstNameField.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        firstNameField.setCursorSize(AndroidUtilities.dp(20));
        firstNameField.setCursorWidth(1.5f);

        if (!TextUtils.isEmpty(customModel)) {
            firstNameField.setText(customModel);
            firstNameField.setSelection(customModel.length());
        }

        linearLayout.addView(firstNameField, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 36, 24, 24, 24, 0));

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        boolean animations = preferences.getBoolean("view_animations", true);
        if (!animations) {
            firstNameField.requestFocus();
            AndroidUtilities.showKeyboard(firstNameField);
        }
    }

    private void saveCustomModel() {
        if (firstNameField.getText() == null) {
            return;
        }

        String newFirst = firstNameField.getText().toString().replace("\n", "");

        if (userId == 0) {
            if (newFirst.equals(UserConfig.getInstance(currentAccount).customModel)){
                finishFragment();
                return;
            }

            UserConfig.getInstance(currentAccount).customModel = newFirst;
            UserConfig.getInstance(currentAccount).saveConfig(false);
            NotificationCenter.getInstance(currentAccount)
                    .postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_CHAT_AIR_AI_CUSTOM_MODEL);
        } else {

            final TLRPC.UserFull userFull = MessagesController.getInstance(currentAccount).getUserFull(userId);
            final TLRPC.User user = getMessagesController().getUser(userId);
            if (getParentActivity() == null || userFull == null || user == null) {
                return;
            }

            final String customModelTx = firstNameField.getText().toString();

            //传入的值与编辑的值相同则不修改
            if (customModelTx.equals(customModel)){
                finishFragment();
                return;
            }
            ArrayList<TLRPC.User> userArrayList = new ArrayList<>();

            if (!TextUtils.isEmpty(customModelTx)) {
                //更新
                user.flags2 |= MessagesController.UPDATE_MASK_CHAT_AIR_AI_CUSTOM_MODEL;
                user.customModel = customModelTx;
                //如果更新自定义model，则说明model选择也需要更新
                user.flags2 |= MessagesController.UPDATE_MASK_CHAT_AIR_AI_MODEL;
                user.aiModel = 0;
                userArrayList.add(user);

            } else {
                //重置
                //更新内存
                user.flags2 = user.flags2 &~ MessagesController.UPDATE_MASK_CHAT_AIR_AI_CUSTOM_MODEL;
                user.customModel = null;

                //更新数据库
                TLRPC.User updateUser = new TLRPC.TL_user();
                updateUser.id = userId;
                updateUser.flags2 = MessagesController.UPDATE_MASK_CHAT_AIR_PROMPT;
                userArrayList.add(updateUser);

            }

            userFull.user = user;
            getMessagesStorage().updateUsers(userArrayList, false, true, true, true);

            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.userInfoDidLoad, user.id, userFull);

        }

        AndroidUtilities.logEvent("saveCustomModel", "");

        finishFragment();
    }

    @Override
    public Theme.ResourcesProvider getResourceProvider() {
        return resourcesProvider;
    }

    @Override
    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        if (isOpen) {
            AndroidUtilities.runOnUIThread(() -> {
                if (firstNameField != null) {
                    firstNameField.requestFocus();
                    AndroidUtilities.showKeyboard(firstNameField);
                }
            }, 100);
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();

        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundWhite));

        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));

        themeDescriptions.add(new ThemeDescription(firstNameField, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(firstNameField, ThemeDescription.FLAG_HINTTEXTCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteHintText));
        themeDescriptions.add(new ThemeDescription(firstNameField, ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_windowBackgroundWhiteInputField));
        themeDescriptions.add(new ThemeDescription(firstNameField, ThemeDescription.FLAG_BACKGROUNDFILTER | ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, null, null, null, null, Theme.key_windowBackgroundWhiteInputFieldActivated));

        return themeDescriptions;
    }

}
