package org.telegram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.PromptBean;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.NumberTextView;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SlideChooseView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by flyun on 2023/9/19.
 */
class PromptListActivity extends BaseFragment {

    private static final int MENU_DELETE = 0;
    private static final int MENU_SHARE = 1;

    private ListAdapter listAdapter;
    private RecyclerListView listView;
    private LinearLayoutManager layoutManager;

    private int rowCount;
    private int promptListHeaderRow;
    private int promptStartRow;
    private int promptEndRow;
    private int promptAddRow;
    private int promptShadowRow;
    private int deleteAllRow;

    private ItemTouchHelper itemTouchHelper;
    private NumberTextView selectedCountTextView;
    private ActionBarMenuItem shareMenuItem;
    private ActionBarMenuItem deleteMenuItem;
    private boolean isJump = false;


    private List<PromptBean> selectedItems = new ArrayList<>();
    private List<PromptBean> promptList = new ArrayList<>();

    private List<PromptBean> internalPromptList = new ArrayList<>();

    public class TextDetailPromptCell extends FrameLayout {

        private final TextView title;
        private final TextView description;
        private PromptBean data;
        boolean drawDivider;

        public TextDetailPromptCell(Context context) {
            super(context);

            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(LinearLayout.VERTICAL);

            title = new TextView(context);
            title.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            linearLayout.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
                    LayoutHelper.WRAP_CONTENT));

            description = new TextView(context);
            description.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            description.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            description.setLineSpacing(AndroidUtilities.dp(2), 1f);
            linearLayout.addView(description, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT
                    , LayoutHelper.WRAP_CONTENT, 0, 0, 0, 1, 0, 0));

//            addView(linearLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,
//            LayoutHelper.WRAP_CONTENT, 0, 62, 8, 48, 9));
            addView(linearLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,
                    LayoutHelper.WRAP_CONTENT, 0, 20, 8, 16, 9));
        }

        public void setData(PromptBean data, boolean drawDivider) {
            this.data = data;
            title.setText(data.title);
            description.setText(data.description);
            this.drawDivider = drawDivider;
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);
            if (drawDivider) {
                canvas.drawRect(AndroidUtilities.dp(20), getMeasuredHeight() - 1,
                        getMeasuredWidth(), getMeasuredHeight(), Theme.dividerPaint);
            }
        }
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        initData();

        updateRows(true);

        return true;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("Prompts",
                R.string.Prompts));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        listAdapter = new ListAdapter(context);

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new RecyclerListView(context) {
            @Override
            protected void dispatchDraw(Canvas canvas) {
                drawSectionBackground(canvas, promptStartRow, promptEndRow, Theme.getColor(Theme
                .key_windowBackgroundWhite));
                super.dispatchDraw(canvas);
            }
        };
        ((DefaultItemAnimator) listView.getItemAnimator()).setDelayAnimations(false);
        ((DefaultItemAnimator) listView.getItemAnimator()).setTranslationInterpolator(CubicBezierInterpolator.DEFAULT);
        listView.setVerticalScrollBarEnabled(false);
        listView.setLayoutManager(layoutManager = new LinearLayoutManager(context,
                LinearLayoutManager.VERTICAL, false));
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,
                LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener((view, position) -> {

            if (position >= promptStartRow && position < promptEndRow) {

                if (!selectedItems.isEmpty()) {
                    listAdapter.toggleSelected(position);
                    return;
                }

                PromptBean bean = promptList.get(position - promptStartRow);
                promptChat(bean);

            } else if (position == promptAddRow) {
            }

        });

//        listView.setOnItemLongClickListener((view, position) -> {
//            if (position >= promptStartRow && position < promptEndRow) {
//                listAdapter.toggleSelected(position);
//                return true;
//            }
//            return false;
//        });

        ActionBarMenu actionMode = actionBar.createActionMode();
        selectedCountTextView = new NumberTextView(actionMode.getContext());
        selectedCountTextView.setTextSize(18);
        selectedCountTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        selectedCountTextView.setTextColor(Theme.getColor(Theme.key_actionBarActionModeDefaultIcon));
        actionMode.addView(selectedCountTextView, LayoutHelper.createLinear(0,
                LayoutHelper.MATCH_PARENT, 1.0f, 72, 0, 0, 0));
        selectedCountTextView.setOnTouchListener((v, event) -> true);

        shareMenuItem = actionMode.addItemWithWidth(MENU_SHARE, R.drawable.msg_share,
                AndroidUtilities.dp(54));
        deleteMenuItem = actionMode.addItemWithWidth(MENU_DELETE, R.drawable.msg_delete,
                AndroidUtilities.dp(54));

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {

                switch (id) {
                    case -1:
                        if (selectedItems.isEmpty()) {
                            finishFragment();
                        } else {
//                            listAdapter.clearSelected();
                        }
                        break;
                    case MENU_DELETE:
                        break;
                    case MENU_SHARE:
                        break;
                }

            }
        });


        return fragmentView;
    }

    @Override
    public boolean onBackPressed() {
        if (!selectedItems.isEmpty()) {
            listAdapter.clearSelected();
            return false;
        }
        return true;
    }

    private void updateRows(boolean notify) {
        rowCount = 0;
        promptListHeaderRow = rowCount++;
        deleteAllRow = -1;

        if (notify) {
            promptList.clear();
            promptList.addAll(internalPromptList);
        }

        if (!promptList.isEmpty()) {
            promptStartRow = rowCount;
            rowCount += promptList.size();
            promptEndRow = rowCount;
        } else {
            promptStartRow = -1;
            promptEndRow = -1;
        }
        promptAddRow = rowCount++;
        promptShadowRow = rowCount++;

        if (notify && listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private void initData() {

        boolean isZh = "zh".equals(LocaleController.getInstance().getCurrentLocaleInfo().shortName);

        if (isZh) {
            internalPromptList.add(new PromptBean(
                    1,
                    LocaleController.getString(R.string.ModelTranslationTitle),
                    LocaleController.getString(R.string.ModelTranslationDesc),
                    LocaleController.getString(R.string.ModelTranslationContent)));
            internalPromptList.add(new PromptBean(
                    2,
                    LocaleController.getString(R.string.PictureModelTranslationTitle),
                    LocaleController.getString(R.string.PictureModelTranslationDesc),
                    LocaleController.getString(R.string.PictureModelTranslationContent)));
        }

        internalPromptList.add(new PromptBean(
                101,
                LocaleController.getString(R.string.WritingAssistantTitle),
                LocaleController.getString(R.string.WritingAssistantDesc),
                LocaleController.getString(R.string.WritingAssistantContent)
        ));
        internalPromptList.add(new PromptBean(
                102,
                LocaleController.getString(R.string.EmojiWritingTitle),
                LocaleController.getString(R.string.EmojiWritingDesc),
                LocaleController.getString(R.string.EmojiWritingContent)
        ));
        internalPromptList.add(new PromptBean(
                103,
                LocaleController.getString(R.string.ScademicianTitle),
                LocaleController.getString(R.string.ScademicianDesc),
                LocaleController.getString(R.string.ScademicianContent)
        ));
        internalPromptList.add(new PromptBean(
                104,
                LocaleController.getString(R.string.ThesisReplyTitle),
                LocaleController.getString(R.string.ThesisReplyDesc),
                LocaleController.getString(R.string.ThesisReplyContent)
        ));
        internalPromptList.add(new PromptBean(
                105,
                LocaleController.getString(R.string.EnglishTranslatorTitle),
                LocaleController.getString(R.string.EnglishTranslatorDesc),
                LocaleController.getString(R.string.EnglishTranslatorContent)
        ));
        internalPromptList.add(new PromptBean(
                106,
                LocaleController.getString(R.string.StackoverflowAnswerTitle),
                LocaleController.getString(R.string.StackoverflowAnswerDesc),
                LocaleController.getString(R.string.StackoverflowAnswerContent)
        ));
        internalPromptList.add(new PromptBean(
                107,
                LocaleController.getString(R.string.CodeAnythingNowTitle),
                LocaleController.getString(R.string.CodeAnythingNowDesc),
                LocaleController.getString(R.string.CodeAnythingNowContent)
        ));
        internalPromptList.add(new PromptBean(
                108,
                LocaleController.getString(R.string.CodeInterpreterTitle),
                LocaleController.getString(R.string.CodeInterpreterDesc),
                LocaleController.getString(R.string.CodeInterpreterContent)
        ));
        internalPromptList.add(new PromptBean(
                109,
                LocaleController.getString(R.string.SummaryTitle),
                LocaleController.getString(R.string.SummaryDesc),
                LocaleController.getString(R.string.SummaryContent)
        ));
        internalPromptList.add(new PromptBean(
                110,
                LocaleController.getString(R.string.AIAssistedDoctorTitle),
                LocaleController.getString(R.string.AIAssistedDoctorDesc),
                LocaleController.getString(R.string.AIAssistedDoctorContent)
        ));
        internalPromptList.add(new PromptBean(
                111,
                LocaleController.getString(R.string.AIPsychotherapyExperienceTitle),
                LocaleController.getString(R.string.AIPsychotherapyExperienceDesc),
                LocaleController.getString(R.string.AIPsychotherapyExperienceContent)
        ));
        internalPromptList.add(new PromptBean(
                112,
                LocaleController.getString(R.string.AILegalAdvisorTitle),
                LocaleController.getString(R.string.AILegalAdvisorDesc),
                LocaleController.getString(R.string.AILegalAdvisorContent)
        ));
        internalPromptList.add(new PromptBean(
                113,
                LocaleController.getString(R.string.DraftingContractsTitle),
                LocaleController.getString(R.string.DraftingContractsDesc),
                LocaleController.getString(R.string.DraftingContractsContent)
        ));

    }

    private void promptChat(PromptBean promptBean) {
        if (promptBean == null || isJump) return;
        int id = UserConfig.getInstance(currentAccount).getNewUserId();
        UserConfig.getInstance(currentAccount).saveConfig(false);
        TLRPC.User dialogUser = new TLRPC.TL_user();
        dialogUser.status = new TLRPC.TL_userStatusOffline();
        dialogUser.first_name = promptBean.title;
        dialogUser.apply_min_photo = true;
        dialogUser.flags = 33555539;
        dialogUser.id = id;
        dialogUser.phone = String.valueOf(id);
        dialogUser.flags2 |= MessagesController.UPDATE_MASK_CHAT_AIR_PROMPT;
        if (promptBean.getNum() == 2) {
            dialogUser.flags2 |= MessagesController.UPDATE_MASK_CHAT_AIR_AI_MODEL;
            dialogUser.aiModel = 10;
        }
        dialogUser.prompt = promptBean.content;

        //写入内存
        MessagesController.getInstance(currentAccount).putUser(dialogUser, false);

        TLRPC.Dialog newDialog = new TLRPC.TL_dialog();
        newDialog.id = dialogUser.id;
        newDialog.folder_id = 0;
//                newDialog.last_message_date = (int) (System.currentTimeMillis() / 1000);
        newDialog.notify_settings = new TLRPC.TL_peerNotifySettingsEmpty_layer77();

        //写入内存
        MessagesController.getInstance(currentAccount).dialogs_dict.put(dialogUser.id, newDialog);
        MessagesController.getInstance(currentAccount).getAllDialogs().add(newDialog);
        MessagesController.getInstance(currentAccount).sortDialogs(null);

        TLRPC.messages_Dialogs dialogsRes = new TLRPC.TL_messages_dialogs();

        ArrayList<TLRPC.Dialog> dialogs = new ArrayList<>();
        dialogs.add(newDialog);

        ArrayList<TLRPC.User> dialogUsers = new ArrayList<>();
        dialogUsers.add(dialogUser);

        dialogsRes.dialogs = dialogs;
        dialogsRes.users = dialogUsers;

        //写入数据库以及其他数据
        MessagesController.getInstance(currentAccount).processLoadedDialogs(dialogsRes, null, null, 0, 0, 100, 0, false, false, false);

        Bundle args = new Bundle();
        args.putLong("user_id", dialogUser.id);
        isJump = true;
        presentFragment(new ChatActivity(args), true);

        AndroidUtilities.logEvent("promptChat", String.valueOf(promptBean.num));
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private final static int VIEW_TYPE_SHADOW = 0,
                VIEW_TYPE_TEXT_SETTING = 1,
                VIEW_TYPE_HEADER = 2,
                VIEW_TYPE_TEXT_CHECK = 3,
                VIEW_TYPE_INFO = 4,
                VIEW_TYPE_PROMPT_DETAIL = 5,
                VIEW_TYPE_SLIDE_CHOOSER = 6;

        public static final int PAYLOAD_CHECKED_CHANGED = 0;
        public static final int PAYLOAD_SELECTION_CHANGED = 1;
        public static final int PAYLOAD_SELECTION_MODE_CHANGED = 2;

        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;

            setHasStableIds(true);
        }

        public void toggleSelected(int position) {
            if (position < promptStartRow || position >= promptEndRow) {
                return;
            }
            PromptBean info = promptList.get(position - promptStartRow);
            if (selectedItems.contains(info)) {
                selectedItems.remove(info);
            } else {
                selectedItems.add(info);
            }
            notifyItemChanged(position, PAYLOAD_SELECTION_CHANGED);
            checkActionMode();
        }

        public void clearSelected() {
            selectedItems.clear();
            notifyItemRangeChanged(promptStartRow, promptEndRow - promptStartRow,
                    PAYLOAD_SELECTION_CHANGED);
            checkActionMode();
        }

        private void checkActionMode() {
            int selectedCount = selectedItems.size();
            boolean actionModeShowed = actionBar.isActionModeShowed();
            if (selectedCount > 0) {
                selectedCountTextView.setNumber(selectedCount, actionModeShowed);
                if (!actionModeShowed) {
                    actionBar.showActionMode();
                    notifyItemRangeChanged(promptStartRow, promptEndRow - promptStartRow,
                            PAYLOAD_SELECTION_MODE_CHANGED);
                }
            } else if (actionModeShowed) {
                actionBar.hideActionMode();
                notifyItemRangeChanged(promptStartRow, promptEndRow - promptStartRow,
                        PAYLOAD_SELECTION_MODE_CHANGED);
            }
        }


        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

            switch (holder.getItemViewType()) {
                case VIEW_TYPE_SHADOW: {
                    if (position == promptShadowRow) {
                        holder.itemView.setBackgroundDrawable(Theme.getThemedDrawable(mContext,
                                R.drawable.greydivider_bottom,
                                Theme.key_windowBackgroundGrayShadow));
                    } else {
                        holder.itemView.setBackgroundDrawable(Theme.getThemedDrawable(mContext,
                                R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    }
                    break;
                }
                case VIEW_TYPE_TEXT_SETTING: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == promptAddRow) {
                        textCell.setText(LocaleController.getString("AddPrompt",
                                R.string.AddPrompt), deleteAllRow != -1);
                    } else if (position == deleteAllRow) {
                        textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteRedText4));
                        textCell.setText(LocaleController.getString(R.string.DeleteAllProxies),
                                false);
                    }
                    break;
                }
                case VIEW_TYPE_HEADER: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == promptListHeaderRow) {
                        headerCell.setText(LocaleController.getString("promptListHeaderRow",
                                R.string.promptListHeaderRow));
                    }
                    break;
                }
                case VIEW_TYPE_PROMPT_DETAIL: {
                    TextDetailPromptCell cell = (TextDetailPromptCell) holder.itemView;
                    PromptBean info = promptList.get(position - promptStartRow);
                    cell.setData(info, position != promptEndRow - 1);
//                    cell.setChecked(SharedConfig.currentProxy == info);
//                    cell.setItemSelected(selectedItems.contains(promptList.get(position -
//                    promptStartRow)), false);
//                    cell.setSelectionEnabled(!selectedItems.isEmpty(), false);
                    break;
                }
            }
        }

//        @SuppressWarnings("unchecked")
//        @Override
//        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position,
//        @NonNull List payloads) {
//            if (holder.getItemViewType() == VIEW_TYPE_PROMPT_DETAIL && !payloads.isEmpty()) {
//                TextSettingsCell cell = (TextSettingsCell) holder.itemView;
//                if (payloads.contains(PAYLOAD_SELECTION_CHANGED)) {
//                    cell.setItemSelected(selectedItems.contains(promptList.get(position -
//                    promptStartRow)), true);
//                }
//                if (payloads.contains(PAYLOAD_SELECTION_MODE_CHANGED)) {
//                    cell.setSelectionEnabled(!selectedItems.isEmpty(), true);
//                }
//            } else {
//                super.onBindViewHolder(holder, position, payloads);
//            }
//        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case VIEW_TYPE_SHADOW:
                    view = new ShadowSectionCell(mContext);
                    break;
                case VIEW_TYPE_TEXT_SETTING:
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case VIEW_TYPE_HEADER:
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case VIEW_TYPE_TEXT_CHECK:
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case VIEW_TYPE_INFO:
                    view = new TextInfoPrivacyCell(mContext);
                    view.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider,
                            Theme.key_windowBackgroundGrayShadow));
                    break;
                case VIEW_TYPE_SLIDE_CHOOSER:
                    view = new SlideChooseView(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case VIEW_TYPE_PROMPT_DETAIL:
                default:
                    view = new TextDetailPromptCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public long getItemId(int position) {
            // Random stable ids, could be anything non-repeating
            if (position == promptListHeaderRow) {
                return -1;
            } else if (position == promptAddRow) {
                return -2;
            } else if (position == promptShadowRow) {
                return -3;
//            } else if (position == useProxyRow) {
//                return -4;
//            } else if (position == callsRow) {
//                return -5;
//            } else if (position == connectionsHeaderRow) {
//                return -6;
//            } else if (position == deleteAllRow) {
//                return -8;
//            } else if (position == rotationRow) {
//                return -9;
//            } else if (position == rotationTimeoutRow) {
//                return -10;
//            } else if (position == rotationTimeoutInfoRow) {
//                return -11;
            } else if (position >= promptStartRow && position < promptEndRow) {
                return promptList.get(position - promptStartRow).hashCode();
            } else {
                return -7;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == promptShadowRow) {
                return VIEW_TYPE_SHADOW;
//            } else if (position == proxyAddRow || position == deleteAllRow) {
//                return VIEW_TYPE_TEXT_SETTING;
//            } else if (position == useProxyRow || position == rotationRow || position ==
//            callsRow) {
//                return VIEW_TYPE_TEXT_CHECK;
            } else if (position == promptListHeaderRow) {
                return VIEW_TYPE_HEADER;
//            } else if (position == rotationTimeoutRow) {
//                return VIEW_TYPE_SLIDE_CHOOSER;
            } else if (position >= promptStartRow && position < promptEndRow) {
                return VIEW_TYPE_PROMPT_DETAIL;
            } else {
                return VIEW_TYPE_INFO;
            }
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            return position >= promptStartRow && position < promptEndRow;
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();

        themeDescriptions.add(new ThemeDescription(listView,
                ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{TextSettingsCell.class,
                TextCheckCell.class, HeaderCell.class, TextDetailPromptCell.class}, null, null,
                null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND
                , null, null, null, null, Theme.key_windowBackgroundGray));

        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND,
                null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR,
                null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR
                , null, null, null, null, Theme.key_actionBarDefaultIcon));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR
                , null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar,
                ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null,
                Theme.key_actionBarDefaultSelector));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null
                , null, null, null, Theme.key_listSelector));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{View.class},
                Theme.dividerPaint, null, null, Theme.key_divider));

        themeDescriptions.add(new ThemeDescription(listView,
                ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{ShadowSectionCell.class},
                null, null, null, Theme.key_windowBackgroundGrayShadow));

        themeDescriptions.add(new ThemeDescription(listView, 0,
                new Class[]{TextSettingsCell.class}, new String[]{"textView"}, null, null, null,
                Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0,
                new Class[]{TextSettingsCell.class}, new String[]{"valueTextView"}, null, null,
                null, Theme.key_windowBackgroundWhiteValueText));

        themeDescriptions.add(new ThemeDescription(listView, 0,
                new Class[]{TextDetailPromptCell.class}, new String[]{"textView"}, null, null,
                null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView,
                ThemeDescription.FLAG_TEXTCOLOR | ThemeDescription.FLAG_CHECKTAG | ThemeDescription.FLAG_IMAGECOLOR, new Class[]{TextDetailPromptCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueText6));
        themeDescriptions.add(new ThemeDescription(listView,
                ThemeDescription.FLAG_TEXTCOLOR | ThemeDescription.FLAG_CHECKTAG | ThemeDescription.FLAG_IMAGECOLOR, new Class[]{TextDetailPromptCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new ThemeDescription(listView,
                ThemeDescription.FLAG_TEXTCOLOR | ThemeDescription.FLAG_CHECKTAG | ThemeDescription.FLAG_IMAGECOLOR, new Class[]{TextDetailPromptCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGreenText));
        themeDescriptions.add(new ThemeDescription(listView,
                ThemeDescription.FLAG_TEXTCOLOR | ThemeDescription.FLAG_CHECKTAG | ThemeDescription.FLAG_IMAGECOLOR, new Class[]{TextDetailPromptCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteRedText4));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_IMAGECOLOR,
                new Class[]{TextDetailPromptCell.class}, new String[]{"checkImageView"}, null,
                null, null, Theme.key_windowBackgroundWhiteGrayText3));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{HeaderCell.class},
                new String[]{"textView"}, null, null, null,
                Theme.key_windowBackgroundWhiteBlueHeader));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class},
                new String[]{"textView"}, null, null, null,
                Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class},
                new String[]{"valueTextView"}, null, null, null,
                Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class},
                new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class},
                new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked));

        themeDescriptions.add(new ThemeDescription(listView,
                ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{TextInfoPrivacyCell.class},
                null, null, null, Theme.key_windowBackgroundGrayShadow));
        themeDescriptions.add(new ThemeDescription(listView, 0,
                new Class[]{TextInfoPrivacyCell.class}, new String[]{"textView"}, null, null,
                null, Theme.key_windowBackgroundWhiteGrayText4));

        return themeDescriptions;
    }

}
