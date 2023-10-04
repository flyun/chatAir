package org.telegram.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.AiModelBean;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextDetailCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by flyun on 2023/7/13.
 */
public class AiParametersActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate{

    private RecyclerListView listView;
    private ListAdapter adapter;
    private LinearLayoutManager layoutManager;

    private int aiParametersHeaderRow;
    private int aiModelRow;
    private int customModelRow;
    private int aiModelTipsRow;
    private int temperatureRow;
    private int temperatureTipsRow;
    private int contextRow;
    private int contextTipsRow;
    private int tokenLimitRow;
    private int tokenLimitTipsRow;

    private int defaultHeaderRow;
    private int defaultRow;
    private int defaultSectionRow;

    private int rowCount = 0;

    private int lastModel;

    @Override
    public boolean onFragmentCreate() {

        updateRow(true);
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.updateInterfaces);

        return super.onFragmentCreate();
    }

    private void updateRow(boolean notify) {
        rowCount = 0;
        aiParametersHeaderRow = rowCount++;
        aiModelRow = rowCount++;
        if (UserConfig.getInstance(currentAccount).aiModel == 0) {
            customModelRow = rowCount++;
        } else {
            customModelRow = 0;
        }
        aiModelTipsRow = rowCount++;
        temperatureRow = rowCount++;
        temperatureTipsRow = rowCount++;
        contextRow = rowCount++;
        contextTipsRow = rowCount++;
        tokenLimitRow = rowCount++;
        tokenLimitTipsRow = rowCount++;
        defaultHeaderRow = rowCount++;
        defaultRow = rowCount++;
        defaultSectionRow = rowCount++;

        if (notify && adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onFragmentDestroy() {
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.updateInterfaces);
        super.onFragmentDestroy();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("GlobalAiParametersHeader", R.string.GlobalAiParametersHeader));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new RecyclerListView(context);
        listView.setItemAnimator(null);
        listView.setLayoutAnimation(null);
        listView.setLayoutManager(layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        });
        listView.setVerticalScrollBarEnabled(false);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setAdapter(adapter = new ListAdapter(context));

        lastModel = UserConfig.getInstance(currentAccount).aiModel;
        //点击
        listView.setOnItemClickListener((view, position, x, y) ->{

            boolean enabled = false;
            if (getParentActivity() == null) {
                return;
            }

            if (position == aiModelRow) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("AiModelTitle", R.string.AiModelTitle));

                LinkedHashMap<Integer, AiModelBean> aiModelList
                        = UserConfig.getInstance(currentAccount).aiModelList;
                if (aiModelList != null && !aiModelList.isEmpty()) {

                    ArrayList<Integer> list = new ArrayList<>();
                    CharSequence[] charSequences = new CharSequence[aiModelList.size()];

                    int i = 0;
                    for (Map.Entry<Integer, AiModelBean> entry : aiModelList.entrySet()){
                        if (entry.getValue().isShow) {
                            list.add(entry.getKey());
                            charSequences[i] = entry.getValue().getName();
                            i++;
                        }
                    }

                    //todo 通过listModels接口检测是否具有模型能力

                    builder.setItems(charSequences, (dialog, which) -> {
                        UserConfig.getInstance(currentAccount).aiModel = list.get(which);
                        UserConfig.getInstance(currentAccount).saveConfig(false);


                        if (isNoUpdateCustomModel()) {
                            adapter.notifyItemChanged(position);
                        }else {
                            lastModel = UserConfig.getInstance(currentAccount).aiModel;
                            updateRow(true);
                        }

                        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_USER_PRINT);
                    });
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    showDialog(builder.create());
                }
            } else if (position == customModelRow) {
                ChangeCustomModelActivity fragment = new ChangeCustomModelActivity(getResourceProvider());
                presentFragment(fragment);
            } else if (position == temperatureRow) {

                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("TemperatureTitle", R.string.TemperatureTitle));

                builder.setItems(new CharSequence[]{
                        "0.0" + " ("+ LocaleController.getString("TemperatureLowValue", R.string.TemperatureLowValue)+")",
                        "0.1",
                        "0.2",
                        "0.3",
                        "0.4",
                        "0.5",
                        "0.6",
                        "0.7" + " ("+ LocaleController.getString("Default", R.string.Default)+")",
                        "0.8",
                        "0.9",
                        "1.0",
                        "1.1",
                        "1.2",
                        "1.3",
                        "1.4",
                        "1.5",
                        "1.6",
                        "1.7",
                        "1.8",
                        "1.9",
                        "2.0" + " ("+ LocaleController.getString("TemperatureHighValue", R.string.TemperatureHighValue)+")"
                }, (dialog, which) -> {
                    double type = 0;
                    if (which == 0) {
                        type = 0;
                    } else if (which == 1) {
                        type = 0.1;
                    } else if (which == 2) {
                        type = 0.2;
                    } else if (which == 3) {
                        type = 0.3;
                    } else if (which == 4) {
                        type = 0.4;
                    } else if (which == 5) {
                        type = 0.5;
                    } else if (which == 6) {
                        type = 0.6;
                    } else if (which == 7) {
                        type = 0.7;
                    } else if (which == 8) {
                        type = 0.8;
                    } else if (which == 9) {
                        type = 0.9;
                    } else if (which == 10) {
                        type = 1.0;
                    } else if (which == 11) {
                        type = 1.1;
                    } else if (which == 12) {
                        type = 1.2;
                    } else if (which == 13) {
                        type = 1.3;
                    } else if (which == 14) {
                        type = 1.4;
                    } else if (which == 15) {
                        type = 1.5;
                    } else if (which == 16) {
                        type = 1.6;
                    } else if (which == 17) {
                        type = 1.7;
                    } else if (which == 18) {
                        type = 1.8;
                    } else if (which == 19) {
                        type = 1.9;
                    } else if (which == 20) {
                        type = 2.0;
                    }

                    UserConfig.getInstance(currentAccount).temperature = type;
                    UserConfig.getInstance(currentAccount).saveConfig(false);

                    adapter.notifyItemChanged(position);
                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_USER_PRINT);
                });
                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                showDialog(builder.create());

            } else if (position == contextRow) {

                //todo 优化：通过操作messageList插入删除contextClear来加入提示，注意删除、发送，检查列表的情况
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("ContextTitle", R.string.ContextTitle));

                builder.setItems(new CharSequence[]{
                        "0",
                        "1",
                        "2",
                        "3",
                        "4",
                        "5",
                        "6",
                        "7",
                        "8",
                        "9",
                        "10",
                        "11",
                        "12",
                        "13",
                        "14",
                        "15",
                        "16",
                        "17",
                        "18",
                        "19",
                        "20",
                        "30",
                        "40"
                }, (dialog, which) -> {
                    int type = which;
                    if (which == 21) {
                        type = 30;
                    } else if (which == 22) {
                        type = 40;
                    }

                    UserConfig.getInstance(currentAccount).contextLimit = type;
                    UserConfig.getInstance(currentAccount).saveConfig(false);

                    adapter.notifyItemChanged(position);
                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_USER_PRINT);
                });
                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                showDialog(builder.create());

            } else if (position == tokenLimitRow) {

                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("TokenLimitTitle", R.string.TokenLimitTitle));

                builder.setItems(new CharSequence[]{
                        LocaleController.getString("RepeatDisabled", R.string.RepeatDisabled),
                        "256 " + LocaleController.getString("TokenText", R.string.TokenText),
                        "512 " + LocaleController.getString("TokenText", R.string.TokenText),
                        "1024 " + LocaleController.getString("TokenText", R.string.TokenText),
                        "2048 " + LocaleController.getString("TokenText", R.string.TokenText),
                        "4096 " + LocaleController.getString("TokenText", R.string.TokenText)
                }, (dialog, which) -> {
                    int type = 0;
                    if (which == 0) {
                        type = -100;
                    } else if (which == 1) {
                        type = 256;
                    } else if (which == 2) {
                        type = 512;
                    } else if (which == 3) {
                        type = 1024;
                    } else if (which == 4) {
                        type = 2048;
                    } else if (which == 5) {
                        type = 4096;
                    }

                    UserConfig.getInstance(currentAccount).tokenLimit = type;
                    UserConfig.getInstance(currentAccount).saveConfig(false);

                    adapter.notifyItemChanged(position);
                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_USER_PRINT);
                });
                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                showDialog(builder.create());

            } else if (position == defaultRow) {

                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("ResetAiParameters", R.string.ResetAiParameters));
                builder.setMessage(LocaleController.getString("ResetAllAiParametersTips", R.string.ResetAllAiParametersTips));
                builder.setPositiveButton(LocaleController.getString("Reset", R.string.Reset), (dialogInterface, i) -> {

                    UserConfig.getInstance(currentAccount).aiModel = UserConfig.defaultAiModel;
                    UserConfig.getInstance(currentAccount).temperature = UserConfig.defaultTemperature;
                    UserConfig.getInstance(currentAccount).contextLimit = UserConfig.defaultContextLimit;
                    UserConfig.getInstance(currentAccount).tokenLimit = UserConfig.defaultTokenLimit;
                    UserConfig.getInstance(currentAccount).customModel = UserConfig.defaultCustomModel;
                    UserConfig.getInstance(currentAccount).saveConfig(false);

                    if (isNoUpdateCustomModel()) {
                        adapter.notifyItemChanged(aiModelRow);
                        adapter.notifyItemChanged(temperatureRow);
                        adapter.notifyItemChanged(contextRow);
                        adapter.notifyItemChanged(tokenLimitRow);
                    }else {
                        lastModel = UserConfig.getInstance(currentAccount).aiModel;
                        updateRow(true);
                    }


                    if (getParentActivity() != null) {
                        Toast toast = Toast.makeText(getParentActivity(), LocaleController.getString("ResetAiParametersText", R.string.ResetAiParametersText), Toast.LENGTH_SHORT);
                        toast.show();
                    }
                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_USER_PRINT);
                });
                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                AlertDialog alertDialog = builder.create();
                showDialog(alertDialog);
                TextView button = (TextView) alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                if (button != null) {
                    button.setTextColor(Theme.getColor(Theme.key_dialogTextRed));
                }
            }

        });

        return fragmentView;

    }

    private boolean isNoUpdateCustomModel() {
        if (lastModel == UserConfig.getInstance(currentAccount).aiModel) return true;
        if (lastModel == 0 || UserConfig.getInstance(currentAccount).aiModel == 0) return false;
        return true;
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.updateInterfaces) {
            int mask = (Integer) args[0];
            if ((mask & MessagesController.UPDATE_MASK_CHAT_AIR_AI_CUSTOM_MODEL) != 0) {
                if (adapter != null){
                    adapter.notifyItemChanged(customModelRow);
                }
            }
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private final Context mContext;
        private final static int
                VIEW_TYPE_HEADER = 1,
                VIEW_TYPE_SELECT = 2,
                VIEW_TYPE_DETAIL_TIPS = 3,
                VIEW_TYPE_SHADOW = 4,
                VIEW_TYPE_TEXT_DETAIL = 5;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            return type != VIEW_TYPE_HEADER && type != VIEW_TYPE_SHADOW;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case VIEW_TYPE_HEADER: {
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                }
                case VIEW_TYPE_SELECT: {
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                }
                case VIEW_TYPE_SHADOW: {
                    view = new ShadowSectionCell(mContext);
                    break;
                }
                case VIEW_TYPE_TEXT_DETAIL:
                    view = new TextDetailSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                default: {
                    view = new TextInfoPrivacyCell(mContext);
                    break;
                }

            }
            return new RecyclerListView.Holder(view);

        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case VIEW_TYPE_HEADER: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == aiParametersHeaderRow) {
                        headerCell.setText(LocaleController.getString("GlobalAiParametersHeader", R.string.GlobalAiParametersHeader));
                    } else if (position == defaultHeaderRow) {
                        headerCell.setText(LocaleController.getString("Reset", R.string.Reset));
                    }
                    break;
                }
                case VIEW_TYPE_SELECT:
                    TextSettingsCell textSettingsCell = (TextSettingsCell) holder.itemView;
                    String selectText;
                    String selectValue;

                    if (position == aiModelRow) {
                        selectText = LocaleController.getString("AiModelTitle", R.string.AiModelTitle);

                        int aiModel = UserConfig.getInstance(currentAccount).aiModel;

                        LinkedHashMap<Integer, AiModelBean> aiModelList
                                = UserConfig.getInstance(currentAccount).aiModelList;
                        if (aiModelList != null && aiModelList.containsKey(aiModel)) {
                            AiModelBean aiModelBean = aiModelList.get(aiModel);
                            selectValue = aiModelBean != null? aiModelBean.getName() : "";
                        } else {
                            selectValue = "";
                        }
                    } else if (position == customModelRow){
                        selectText = LocaleController.getString("CustomModel", R.string.CustomModel);
                        selectValue = UserConfig.getInstance(currentAccount).customModel;
                    } else if (position == temperatureRow){
                        selectText = LocaleController.getString("TemperatureTitle", R.string.TemperatureTitle);
                        selectValue = Double.toString(UserConfig.getInstance(currentAccount).temperature);
                    } else if (position == contextRow){
                        selectText = LocaleController.getString("ContextTitle", R.string.ContextTitle);
                        selectValue = Integer.toString(UserConfig.getInstance(currentAccount).contextLimit);
                    } else if (position == tokenLimitRow){
                        selectText = LocaleController.getString("TokenLimitTitle", R.string.TokenLimitTitle);
                        int tokenLimit = UserConfig.getInstance(currentAccount).tokenLimit;
                        if (tokenLimit != -100) {
                            selectValue = Integer.toString(tokenLimit);
                        } else {
                            selectValue = LocaleController.getString("RepeatDisabled", R.string.RepeatDisabled);
                        }

                    } else {
                        selectText = "";
                        selectValue = "";
                    }

                    textSettingsCell.setTextAndValue(selectText, selectValue, false, false);

                    break;

                case VIEW_TYPE_DETAIL_TIPS:
                    TextInfoPrivacyCell textInfoPrivacyCell = (TextInfoPrivacyCell) holder.itemView;
                    textInfoPrivacyCell.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, getThemedColor(Theme.key_windowBackgroundGrayShadow)));
                    String tips;
                    if (position == aiModelTipsRow) {
                        tips = LocaleController.getString("AiModelTips", R.string.AiModelTips);
                    } else if (position == contextTipsRow) {
                        tips = LocaleController.getString("ContextTips", R.string.ContextTips);
                    } else if (position == temperatureTipsRow) {
                        tips = LocaleController.getString("TemperatureTips", R.string.TemperatureTips);
                    } else if (position == tokenLimitTipsRow) {
                        tips = LocaleController.getString("TokenLimitTips", R.string.TokenLimitTips);
                    } else {
                        tips = "";
                    }
                    textInfoPrivacyCell.setText(tips);
                    break;
                case VIEW_TYPE_SHADOW:
                    View sectionCell = holder.itemView;
                    sectionCell.setTag(position);
                    sectionCell.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider, getThemedColor(Theme.key_windowBackgroundGrayShadow)));
                    break;
                case VIEW_TYPE_TEXT_DETAIL:

                    TextDetailSettingsCell settingsCell = (TextDetailSettingsCell) holder.itemView;
                    settingsCell.setMultilineDetail(true);
                    if (position == defaultRow) {
                        settingsCell.setTextAndValue(
                                LocaleController.getString("ResetAiParameters", R.string.ResetAiParameters),
                                LocaleController.getString("ResetAllAiParametersTips", R.string.ResetAllAiParametersTips), false);
                    }
                    break;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == aiParametersHeaderRow || position == defaultHeaderRow) {
                return VIEW_TYPE_HEADER;
            } else if (position == aiModelRow || position == temperatureRow ||
                    position == contextRow || position == tokenLimitRow ||
                    position == customModelRow){
                return VIEW_TYPE_SELECT;
            } else if (position == aiModelTipsRow ||
                    position == temperatureTipsRow || position == contextTipsRow ||
                    position == tokenLimitTipsRow){
                return VIEW_TYPE_DETAIL_TIPS;
            } else if (position == defaultSectionRow){
                return VIEW_TYPE_SHADOW;
            } else if (position == defaultRow){
                return VIEW_TYPE_TEXT_DETAIL;
            } else {
                return 0;
            }
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{HeaderCell.class, TextSettingsCell.class, TextDetailSettingsCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));

        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{HeaderCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{TextSettingsCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextSettingsCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextSettingsCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteValueText));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{TextInfoPrivacyCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextInfoPrivacyCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText4));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextDetailCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextDetailCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextDetailSettingsCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextDetailSettingsCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));


        return themeDescriptions;
    }


    }
