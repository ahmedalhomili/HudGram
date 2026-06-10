package com.hudgram.ui.autoreply;
import com.hudgram.ui.settings.BaseHudSettingsActivity;
import com.hudgram.core.HudConfig;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.ScaleStateListAnimator;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

public class HudAutoReplyMessagesActivity extends BaseHudSettingsActivity {

    private ArrayList<String> messages;

    @Override
    public boolean onFragmentCreate() {
        messages = new ArrayList<>(HudConfig.getAutoReplyMessages());
        return super.onFragmentCreate();
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asShadow(getString("HudAutoReplyNoMessages").replace("لا توجد رسائل بعد.", "").trim()));

        if (messages.isEmpty()) {
            items.add(UItem.asShadow(getString("HudAutoReplyNoMessages")));
        } else {
            for (int i = 0; i < messages.size(); i++) {
                boolean hasDivider = (i < messages.size() - 1);
                UItem uItem = MessageCellFactory.of(100 + i, (i + 1) + ".", messages.get(i));
                uItem.accent = hasDivider;
                items.add(uItem);
            }
            items.add(UItem.asShadow(null));
        }

        // Add button
        UItem addItem = TextSettingsCellFactory.of(1, getString("HudAutoReplyAddMessage"));
        addItem.accent = true;
        items.add(addItem);
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.id == 1) {
            showEditDialog(-1);
        } else if (item.id >= 100) {
            int index = item.id - 100;
            if (index >= 0 && index < messages.size()) {
                showItemOptions(index);
            }
        }
    }

    @Override
    protected boolean onItemLongClick(UItem item, View view, int position, float x, float y) {
        if (item.id >= 100) {
            int index = item.id - 100;
            if (index >= 0 && index < messages.size()) {
                showItemOptions(index);
                return true;
            }
        }
        return false;
    }

    private void showItemOptions(int index) {
        if (index < 0 || index >= messages.size()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(getString("HudAutoReplyEditMessage"));

        String editText = getString("HudAutoReplyEditMessage");
        String deleteText = getString("HudAutoReplyDeleteMessage");

        builder.setItems(new CharSequence[]{editText, deleteText}, (dialog, which) -> {
            if (which == 0) {
                showEditDialog(index);
            } else if (which == 1) {
                showDeleteConfirmation(index);
            }
        });
        showDialog(builder.create());
    }

    private void showEditDialog(int index) {
        final EditTextBoldCursor editText = new EditTextBoldCursor(getParentActivity());
        editText.setBackgroundDrawable(Theme.createEditTextDrawable(getParentActivity(), true));

        boolean isEdit = index >= 0 && index < messages.size();
        String title = isEdit ? getString("HudAutoReplyEditMessage") : getString("HudAutoReplyAddMessage");

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setDialogButtonColorKey(Theme.key_dialogButton);
        builder.setTitle(title);
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), (dialog, which) -> AndroidUtilities.hideKeyboard(editText));

        LinearLayout linearLayout = new LinearLayout(getParentActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        builder.setView(linearLayout);

        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setMaxLines(4);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        editText.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP);
        editText.setHint(getString("HudAutoReplyMentionTextHint"));
        editText.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        editText.setCursorColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setCursorSize(AndroidUtilities.dp(20));
        editText.setCursorWidth(1.5f);
        editText.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4));
        linearLayout.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 24, 6, 24, 0));

        // Character counter
        final TextView charCounter = new TextView(getParentActivity());
        charCounter.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        charCounter.setTextColor(Theme.getColor(Theme.key_dialogTextGray3));
        charCounter.setGravity(LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT);
        linearLayout.addView(charCounter, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 24, 4, 24, 0));

        if (isEdit) {
            editText.setText(messages.get(index));
            editText.setSelection(editText.length());
        }
        charCounter.setText(String.valueOf(editText.length()));

        editText.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                charCounter.setText(String.valueOf(s.length()));
            }
        });

        builder.setPositiveButton(LocaleController.getString(R.string.Save), (dialog, which) -> {
            AndroidUtilities.hideKeyboard(editText);
            String text = editText.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) {
                if (isEdit) {
                    messages.set(index, text);
                    HudConfig.setAutoReplyMessages(messages);
                    listView.adapter.update(true);
                    BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_saved, getString("HudAutoReplyMessageUpdated")).show();
                } else {
                    messages.add(text);
                    HudConfig.setAutoReplyMessages(messages);
                    listView.adapter.update(true);
                    BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_saved, getString("HudAutoReplyMessageAdded")).show();
                }
            }
        });

        final AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(dialog -> AndroidUtilities.runOnUIThread(() -> {
            editText.requestFocus();
            AndroidUtilities.showKeyboard(editText);
        }));
        alertDialog.setOnDismissListener(dialog -> AndroidUtilities.hideKeyboard(editText));
        showDialog(alertDialog);
        alertDialog.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.requestFocus();
    }

    private void showDeleteConfirmation(int index) {
        if (index < 0 || index >= messages.size()) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(getString("HudAutoReplyDeleteMessage"));
        builder.setMessage(getString("HudAutoReplyDeleteConfirm"));
        builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialog, which) -> {
            messages.remove(index);
            HudConfig.setAutoReplyMessages(messages);
            listView.adapter.update(true);
            BulletinFactory.of(this).createSimpleBulletin(R.drawable.msg_delete, getString("HudAutoReplyMessageDeleted")).show();
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
    protected String getActionBarTitle() {
        return getString("HudAutoReplyManageMessages");
    }

    @Override
    protected String getKey() {
        return "arm";
    }

    // === Message cell factory ===
    protected static class MessageCellFactory extends UItem.UItemFactory<MessageCell> {
        static {
            setup(new MessageCellFactory());
        }

        @Override
        public MessageCell createView(Context context, RecyclerListView listView, int currentAccount, int classGuid, Theme.ResourcesProvider resourcesProvider) {
            return new MessageCell(context);
        }

        @Override
        public void bindView(View view, UItem item, boolean divider, UniversalAdapter adapter, UniversalRecyclerView listView) {
            MessageCell cell = (MessageCell) view;
            cell.setData(item.text, item.subtext, item.accent || divider);
        }

        public static UItem of(int id, CharSequence number, CharSequence message) {
            UItem item = UItem.ofFactory(MessageCellFactory.class);
            item.id = id;
            item.text = number;
            item.subtext = message;
            return item;
        }
    }

    protected static class MessageCell extends FrameLayout {
        private final TextView numberView;
        private final TextView messageView;
        private boolean needsDivider;

        public MessageCell(Context context) {
            super(context);

            numberView = new TextView(context);
            numberView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            numberView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText2));
            numberView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
            addView(numberView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT,
                    (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP,
                    21, 12, 21, 0));

            messageView = new TextView(context);
            messageView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            messageView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            messageView.setMaxLines(3);
            messageView.setEllipsize(TextUtils.TruncateAt.END);
            messageView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
            messageView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
            int leftMargin = LocaleController.isRTL ? 21 : 40;
            int rightMargin = LocaleController.isRTL ? 40 : 21;
            addView(messageView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                    (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP,
                    leftMargin, 10, rightMargin, 0));

            setWillNotDraw(false);
        }

        public void setData(CharSequence number, CharSequence message, boolean divider) {
            numberView.setText(number);
            messageView.setText(message);
            needsDivider = divider;
            invalidate();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(
                    MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            );
            int minHeight = AndroidUtilities.dp(56);
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
