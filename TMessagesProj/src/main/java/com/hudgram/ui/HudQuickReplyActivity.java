package com.hudgram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ScaleStateListAnimator;
import org.telegram.ui.Components.Switch;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.BulletinFactory;

import java.util.ArrayList;

public class HudQuickReplyActivity extends BaseHudSettingsActivity {

    private LinearLayout inputCardView;
    private EditTextBoldCursor labelEdit;
    private EditTextBoldCursor valueEdit;
    private FrameLayout addButton;
    private ImageView addButtonIcon;
    private TextView modeHeaderText;
    private ImageView cancelButton;
    private LinearLayout headerContainer;

    private ArrayList<HudConfig.QuickReplyItem> quickReplies;

    // Edit mode state
    private int editingIndex = -1;

    @Override
    public boolean onFragmentCreate() {
        quickReplies = new ArrayList<>(HudConfig.getQuickReplies());
        return super.onFragmentCreate();
    }

    @Override
    public ActionBar createActionBar(Context context) {
        ActionBar actionBar = super.createActionBar(context);
        ActionBarMenu menu = actionBar.createMenu();

        Switch switchView = new Switch(context);
        switchView.setColors(Theme.key_switchTrack, Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhite);
        switchView.setChecked(HudConfig.quickReplyEnabled, false);
        switchView.setOnCheckedChangeListener((view, isChecked) -> {
            if (HudConfig.quickReplyEnabled != isChecked) {
                HudConfig.toggleQuickReplyEnabled();
                BulletinFactory.of(this).createSimpleBulletin(
                    isChecked ? R.drawable.msg_saved : R.drawable.msg_close,
                    getString(isChecked ? "HudQuickReplyEnabled" : "HudQuickReplyDisabled")
                ).show();
            }
        });

        // Create a menu item wrapper to give the switch a native clickable area and touch selector background
        ActionBarMenuItem menuItem = menu.addItem(1, 0);
        menuItem.removeAllViews();
        menuItem.addView(switchView, LayoutHelper.createFrame(37, 50, Gravity.CENTER));
        menuItem.setOnClickListener(v -> {
            switchView.setChecked(!switchView.isChecked(), true);
        });

        return actionBar;
    }

    @Override
    public View createView(Context context) {
        // === Input card container ===
        inputCardView = new LinearLayout(context);
        inputCardView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        inputCardView.setOrientation(LinearLayout.VERTICAL);
        inputCardView.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
        inputCardView.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(12));

        // === Mode header (Add/Edit indicator) ===
        headerContainer = new LinearLayout(context);
        headerContainer.setOrientation(LinearLayout.HORIZONTAL);
        headerContainer.setGravity(Gravity.CENTER_VERTICAL);
        headerContainer.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(8));

        modeHeaderText = new TextView(context);
        modeHeaderText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        modeHeaderText.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText2));
        modeHeaderText.setText(getString("HudQuickReplyAddNew"));
        modeHeaderText.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        headerContainer.addView(modeHeaderText, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        // Cancel button (only visible in edit mode)
        cancelButton = new ImageView(context);
        cancelButton.setImageResource(R.drawable.msg_close);
        cancelButton.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_windowBackgroundWhiteGrayText2), PorterDuff.Mode.SRC_IN));
        cancelButton.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4));
        cancelButton.setVisibility(View.GONE);
        cancelButton.setOnClickListener(v -> exitEditMode());
        ScaleStateListAnimator.apply(cancelButton, 0.85f, 1.2f);
        headerContainer.addView(cancelButton, LayoutHelper.createLinear(28, 28, Gravity.CENTER_VERTICAL));

        inputCardView.addView(headerContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // === Horizontal row: fields + FAB ===
        LinearLayout fieldsRow = new LinearLayout(context);
        fieldsRow.setOrientation(LinearLayout.HORIZONTAL);
        fieldsRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout fieldsContainer = new LinearLayout(context);
        fieldsContainer.setOrientation(LinearLayout.VERTICAL);

        // Label field with # prefix
        LinearLayout labelRow = new LinearLayout(context);
        labelRow.setOrientation(LinearLayout.HORIZONTAL);
        labelRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView hashPrefix = new TextView(context);
        hashPrefix.setText("#");
        hashPrefix.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        hashPrefix.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueText2));
        hashPrefix.setTypeface(Typeface.DEFAULT_BOLD);
        hashPrefix.setPadding(0, 0, AndroidUtilities.dp(4), 0);
        labelRow.addView(hashPrefix, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        labelEdit = new EditTextBoldCursor(context);
        labelEdit.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        labelEdit.setHintTextColor(getThemedColor(Theme.key_windowBackgroundWhiteHintText));
        labelEdit.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        labelEdit.setBackgroundDrawable(null);
        labelEdit.setLineColors(getThemedColor(Theme.key_windowBackgroundWhiteInputField), getThemedColor(Theme.key_windowBackgroundWhiteInputFieldActivated), getThemedColor(Theme.key_text_RedRegular));
        labelEdit.setPadding(0, 0, 0, AndroidUtilities.dp(6));
        labelEdit.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        labelEdit.setHint(getString("HudQuickReplyLabelHint"));
        labelEdit.setSingleLine(true);
        labelEdit.setCursorColor(getThemedColor(Theme.key_windowBackgroundWhiteInputFieldActivated));
        labelEdit.setCursorWidth(1.5f);
        labelEdit.setFilters(new InputFilter[] {
            new InputFilter.LengthFilter(20),
            (source, start, end, dest, dstart, dend) -> {
                for (int i = start; i < end; i++) {
                    char c = source.charAt(i);
                    if (Character.isWhitespace(c) || c == '#' || c == '@') {
                        return "";
                    }
                }
                return null;
            }
        });
        labelRow.addView(labelEdit, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));
        fieldsContainer.addView(labelRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        valueEdit = new EditTextBoldCursor(context);
        valueEdit.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        valueEdit.setHintTextColor(getThemedColor(Theme.key_windowBackgroundWhiteHintText));
        valueEdit.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        valueEdit.setBackgroundDrawable(null);
        valueEdit.setLineColors(getThemedColor(Theme.key_windowBackgroundWhiteInputField), getThemedColor(Theme.key_windowBackgroundWhiteInputFieldActivated), getThemedColor(Theme.key_text_RedRegular));
        valueEdit.setPadding(0, 0, 0, AndroidUtilities.dp(6));
        valueEdit.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        valueEdit.setHint(getString("HudQuickReplyValueHint"));
        valueEdit.setSingleLine(false);
        valueEdit.setMaxLines(4);
        valueEdit.setCursorColor(getThemedColor(Theme.key_windowBackgroundWhiteInputFieldActivated));
        valueEdit.setCursorWidth(1.5f);

        fieldsContainer.addView(valueEdit, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 0));

        fieldsRow.addView(fieldsContainer, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1.0f));

        // FAB button
        addButton = new FrameLayout(context);
        int fabColor = getThemedColor(Theme.key_featuredStickers_addButton);
        int fabPressedColor = getThemedColor(Theme.key_featuredStickers_addButtonPressed);
        if (fabPressedColor == 0) {
            fabPressedColor = Theme.blendOver(fabColor, 0x1A000000);
        }
        addButton.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(12), fabColor, fabPressedColor));
        ScaleStateListAnimator.apply(addButton, 0.85f, 1.5f);

        addButtonIcon = new ImageView(context);
        addButtonIcon.setImageResource(R.drawable.msg_add);
        addButtonIcon.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_chats_actionIcon), PorterDuff.Mode.SRC_IN));
        addButton.addView(addButtonIcon, LayoutHelper.createFrame(24, 24, Gravity.CENTER));

        fieldsRow.addView(addButton, LayoutHelper.createLinear(44, 44, Gravity.CENTER_VERTICAL, LocaleController.isRTL ? 0 : 12, 0, LocaleController.isRTL ? 12 : 0, 0));

        inputCardView.addView(fieldsRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        addButton.setOnClickListener(v -> {
            String label = labelEdit.getText().toString().trim();
            String value = valueEdit.getText().toString().trim();
            if (TextUtils.isEmpty(label) || TextUtils.isEmpty(value)) {
                BulletinFactory.of(this).createErrorBulletin(getString("HudQuickReplyFillFields")).show();
                return;
            }
            if (label.startsWith("#")) {
                label = label.substring(1).trim();
            }
            if (TextUtils.isEmpty(label)) {
                BulletinFactory.of(this).createErrorBulletin(getString("HudQuickReplyInvalidLabel")).show();
                return;
            }

            if (editingIndex >= 0) {
                // Update existing item
                final String finalLabel = label;
                for (int i = 0; i < quickReplies.size(); i++) {
                    if (i != editingIndex && quickReplies.get(i).label.equalsIgnoreCase(finalLabel)) {
                        BulletinFactory.of(this).createErrorBulletin(getString("HudQuickReplyDuplicate")).show();
                        return;
                    }
                }
                quickReplies.get(editingIndex).label = finalLabel;
                quickReplies.get(editingIndex).value = value;
                HudConfig.setQuickReplies(quickReplies);

                exitEditMode();
                listView.adapter.update(true);

                BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_saved, getString("HudQuickReplyUpdated")).show();
            } else {
                // Add new item
                final String finalLabel = label;
                for (HudConfig.QuickReplyItem item : quickReplies) {
                    if (item.label.equalsIgnoreCase(finalLabel)) {
                        BulletinFactory.of(this).createErrorBulletin(getString("HudQuickReplyDuplicate")).show();
                        return;
                    }
                }

                HudConfig.QuickReplyItem newItem = new HudConfig.QuickReplyItem(label, value);
                quickReplies.add(newItem);
                HudConfig.setQuickReplies(quickReplies);

                labelEdit.setText("");
                valueEdit.setText("");

                listView.adapter.update(true);

                BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_saved, getString("HudQuickReplyAdded")).show();
            }

            // Dismiss keyboard after add/save
            AndroidUtilities.hideKeyboard(labelEdit);
            AndroidUtilities.hideKeyboard(valueEdit);
        });

        return super.createView(context);
    }

    private void enterEditMode(int index) {
        if (index < 0 || index >= quickReplies.size()) return;
        editingIndex = index;
        HudConfig.QuickReplyItem item = quickReplies.get(index);
        labelEdit.setText(item.label);
        valueEdit.setText(item.value);
        labelEdit.requestFocus();
        labelEdit.setSelection(labelEdit.getText().length());

        // Switch icon to check mark (save)
        addButtonIcon.setImageResource(R.drawable.floating_check);
        addButtonIcon.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_chats_actionIcon), PorterDuff.Mode.SRC_IN));

        // Update header
        modeHeaderText.setText(getString("HudQuickReplyEditMode") + ": #" + item.label);
        modeHeaderText.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueText2));
        cancelButton.setVisibility(View.VISIBLE);
    }

    private void exitEditMode() {
        editingIndex = -1;
        labelEdit.setText("");
        valueEdit.setText("");

        // Switch icon back to add
        addButtonIcon.setImageResource(R.drawable.msg_add);
        addButtonIcon.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_chats_actionIcon), PorterDuff.Mode.SRC_IN));

        // Reset header
        modeHeaderText.setText(getString("HudQuickReplyAddNew"));
        modeHeaderText.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText2));
        cancelButton.setVisibility(View.GONE);

        // Dismiss keyboard
        AndroidUtilities.hideKeyboard(labelEdit);
        AndroidUtilities.hideKeyboard(valueEdit);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        // Instructions at top
        items.add(UItem.asShadow(getString("HudQuickReplySettingsInfo")));

        // Input card
        items.add(UItem.asCustom(inputCardView));
        items.add(UItem.asShadow(null));

        // Quick reply items with dividers
        for (int i = 0; i < quickReplies.size(); i++) {
            HudConfig.QuickReplyItem item = quickReplies.get(i);
            boolean hasDivider = (i < quickReplies.size() - 1);
            UItem uItem = QuickReplyCellFactory.of(100 + i, "#" + item.label, item.value);
            uItem.accent = hasDivider;  // reuse accent flag for divider signal
            items.add(uItem);
        }

        if (quickReplies.isEmpty()) {
            items.add(UItem.asShadow(getString("HudQuickReplyEmpty")));
        }
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.id >= 100) {
            int index = item.id - 100;
            if (index >= 0 && index < quickReplies.size()) {
                showItemOptionsDialog(index);
            }
        }
    }

    @Override
    protected boolean onItemLongClick(UItem item, View view, int position, float x, float y) {
        if (item.id >= 100) {
            int index = item.id - 100;
            if (index >= 0 && index < quickReplies.size()) {
                showItemOptionsDialog(index);
                return true;
            }
        }
        return false;
    }

    private void showItemOptionsDialog(int index) {
        if (index < 0 || index >= quickReplies.size()) return;
        HudConfig.QuickReplyItem quickReplyItem = quickReplies.get(index);

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("#" + quickReplyItem.label);

        String editText = getString("HudQuickReplyEdit");
        String deleteText = LocaleController.getString("Delete", R.string.Delete);

        builder.setItems(new CharSequence[]{editText, deleteText}, (dialog, which) -> {
            if (which == 0) {
                // Edit
                enterEditMode(index);
            } else if (which == 1) {
                // Delete
                showDeleteConfirmation(index);
            }
        });
        showDialog(builder.create());
    }

    private void showDeleteConfirmation(int index) {
        if (index < 0 || index >= quickReplies.size()) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(getString("HudQuickReplyDeleteTitle"));
        builder.setMessage(getString("HudQuickReplyDeleteConfirm"));
        builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialog, which) -> {
            quickReplies.remove(index);
            HudConfig.setQuickReplies(quickReplies);

            if (editingIndex == index) {
                exitEditMode();
            } else if (editingIndex > index) {
                editingIndex--;
            }

            listView.adapter.update(true);
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_delete, getString("HudQuickReplyDeleted")).show();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);

        AlertDialog dialog = builder.create();
        showDialog(dialog);

        TextView button = (TextView) dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (button != null) {
            button.setTextColor(getThemedColor(Theme.key_text_RedBold));
        }
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        editingIndex = -1;
    }

    @Override
    protected String getActionBarTitle() {
        return getString("HudQuickReplyTitle");
    }

    @Override
    protected String getKey() {
        return "quickReply";
    }

    // === Custom cell factory for quick reply items ===
    // Shows label prominently with value truncated to 3 lines, with divider support

    protected static class QuickReplyCellFactory extends UItem.UItemFactory<QuickReplyCell> {
        static {
            setup(new QuickReplyCellFactory());
        }

        @Override
        public QuickReplyCell createView(Context context, org.telegram.ui.Components.RecyclerListView listView, int currentAccount, int classGuid, Theme.ResourcesProvider resourcesProvider) {
            return new QuickReplyCell(context);
        }

        @Override
        public void bindView(View view, UItem item, boolean divider, UniversalAdapter adapter, org.telegram.ui.Components.UniversalRecyclerView listView) {
            QuickReplyCell cell = (QuickReplyCell) view;
            cell.setData(item.text, item.subtext, item.accent || divider);
        }

        public static UItem of(int id, CharSequence title, CharSequence subtitle) {
            UItem item = UItem.ofFactory(QuickReplyCellFactory.class);
            item.id = id;
            item.text = title;
            item.subtext = subtitle;
            return item;
        }
    }

    // === Custom view for quick reply list items ===
    protected static class QuickReplyCell extends FrameLayout {

        private final TextView labelView;
        private final TextView valueView;
        private boolean needsDivider;

        public QuickReplyCell(Context context) {
            super(context);

            // Label (e.g. "#hello") - bold, accent color
            labelView = new TextView(context);
            labelView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            labelView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText2));
            labelView.setTypeface(Typeface.DEFAULT_BOLD);
            labelView.setSingleLine(true);
            labelView.setEllipsize(TextUtils.TruncateAt.END);
            labelView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
            addView(labelView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                    (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP,
                    21, 10, 21, 0));

            // Value text - gray, max 3 lines
            valueView = new TextView(context);
            valueView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            valueView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            valueView.setMaxLines(3);
            valueView.setEllipsize(TextUtils.TruncateAt.END);
            valueView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
            valueView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
            addView(valueView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                    (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP,
                    21, 32, 21, 0));

            setWillNotDraw(false);
        }

        public void setData(CharSequence label, CharSequence value, boolean divider) {
            labelView.setText(label);
            valueView.setText(value);
            needsDivider = divider;
            invalidate();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(
                    MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            );
            // Ensure minimum height and add bottom padding
            int minHeight = AndroidUtilities.dp(64);
            int measuredH = getMeasuredHeight();
            int paddedH = measuredH + AndroidUtilities.dp(10);
            if (paddedH < minHeight) paddedH = minHeight;
            setMeasuredDimension(getMeasuredWidth(), paddedH + (needsDivider ? 1 : 0));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (needsDivider && Theme.dividerPaint != null) {
                int startX = LocaleController.isRTL ? 0 : AndroidUtilities.dp(21);
                int endX = getMeasuredWidth() - (LocaleController.isRTL ? AndroidUtilities.dp(21) : 0);
                canvas.drawLine(startX, getMeasuredHeight() - 1, endX, getMeasuredHeight() - 1, Theme.dividerPaint);
            }
        }
    }
}
