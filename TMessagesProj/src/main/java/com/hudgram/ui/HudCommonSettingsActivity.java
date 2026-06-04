package com.hudgram.ui;

import android.view.View;

import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

public class HudCommonSettingsActivity extends BaseHudSettingsActivity {

    private final int disabledInstantCameraRow = rowId++;
    private final int askBeforeCallRow = rowId++;
    private final int openArchiveOnPullRow = rowId++;
    private final int confirmStickersRow = rowId++;
    private final int confirmVoiceMessagesRow = rowId++;
    private final int partialCopyRow = rowId++;

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(getString("CommonSettings")));
        items.add(UItem.asCheck(disabledInstantCameraRow, getString("DisableInstantCamera")).slug("disabledInstantCamera").setChecked(HudConfig.disableInstantCamera));
        items.add(UItem.asCheck(askBeforeCallRow, getString("AskBeforeCalling")).slug("askBeforeCall").setChecked(HudConfig.askBeforeCall));
        items.add(UItem.asCheck(openArchiveOnPullRow, getString("OpenArchiveOnPull")).slug("openArchiveOnPull").setChecked(HudConfig.openArchiveOnPull));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCheck(confirmStickersRow, getString("ConfirmStickers")).slug("confirmStickers").setChecked(HudConfig.confirmStickers));
        items.add(UItem.asShadow(getString("ConfirmStickersAbout")));

        items.add(UItem.asCheck(confirmVoiceMessagesRow, getString("ConfirmVoiceMessages")).slug("confirmVoiceMessages").setChecked(HudConfig.confirmVoiceMessages));
        items.add(UItem.asShadow(getString("ConfirmVoiceMessagesAbout")));

        items.add(UItem.asCheck(partialCopyRow, getString("PartialCopy")).slug("partialCopy").setChecked(HudConfig.partialCopy));
        items.add(UItem.asShadow(getString("PartialCopyAbout")));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        int id = item.id;
        if (id == disabledInstantCameraRow) {
            HudConfig.toggleDisabledInstantCamera();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(HudConfig.disableInstantCamera);
            }
        } else if (id == askBeforeCallRow) {
            HudConfig.toggleAskBeforeCall();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(HudConfig.askBeforeCall);
            }
        } else if (id == openArchiveOnPullRow) {
            HudConfig.toggleOpenArchiveOnPull();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(HudConfig.openArchiveOnPull);
            }
        } else if (id == confirmStickersRow) {
            HudConfig.toggleConfirmStickers();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(HudConfig.confirmStickers);
            }
        } else if (id == confirmVoiceMessagesRow) {
            HudConfig.toggleConfirmVoiceMessages();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(HudConfig.confirmVoiceMessages);
            }
        } else if (id == partialCopyRow) {
            HudConfig.togglePartialCopy();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(HudConfig.partialCopy);
            }
        }
    }

    @Override
    protected String getActionBarTitle() {
        return getString("CommonSettings");
    }

    @Override
    protected String getKey() {
        return "c";
    }
}
