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

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(getString("CommonSettings")));
        items.add(UItem.asCheck(disabledInstantCameraRow, getString("DisableInstantCamera")).slug("disabledInstantCamera").setChecked(HudConfig.disableInstantCamera));
        items.add(UItem.asCheck(askBeforeCallRow, getString("AskBeforeCalling")).slug("askBeforeCall").setChecked(HudConfig.askBeforeCall));
        items.add(UItem.asCheck(openArchiveOnPullRow, getString("OpenArchiveOnPull")).slug("openArchiveOnPull").setChecked(HudConfig.openArchiveOnPull));
        items.add(UItem.asShadow(null));
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
