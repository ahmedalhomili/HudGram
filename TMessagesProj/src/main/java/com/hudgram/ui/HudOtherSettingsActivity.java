package com.hudgram.ui;

import android.view.View;

import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

import java.util.ArrayList;

public class HudOtherSettingsActivity extends BaseHudSettingsActivity {

    private final int preferIPv6Row = rowId++;
    private final int nameOrderRow = rowId++;
    private final int idTypeRow = rowId++;

    private CharSequence getNameOrderString() {
        if (HudConfig.nameOrder == 1) {
            return getString("FirstLast");
        } else {
            return getString("LastFirst");
        }
    }

    private CharSequence getIdTypeString() {
        switch (HudConfig.idType) {
            case HudConfig.ID_TYPE_API:
                return getString("IdTypeAPI");
            case HudConfig.ID_TYPE_BOTAPI:
                return getString("IdTypeBOTAPI");
            case HudConfig.ID_TYPE_HIDDEN:
            default:
                return getString("IdTypeHidden");
        }
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(getString("HudSettingsOther")));
        
        items.add(UItem.asCheck(preferIPv6Row, getString("PreferIPv6")).slug("preferIPv6").setChecked(HudConfig.preferIPv6));
        items.add(UItem.asShadow(getString("Connection")));
        
        items.add(TextSettingsCellFactory.of(nameOrderRow, getString("NameOrder"), getNameOrderString()).slug("nameOrder"));
        items.add(UItem.asShadow(null));
        
        items.add(TextSettingsCellFactory.of(idTypeRow, getString("IdType"), getIdTypeString()).slug("idType"));
        items.add(UItem.asShadow(getString("IdTypeAbout")));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        int id = item.id;
        if (id == preferIPv6Row) {
            HudConfig.toggleIPv6();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(HudConfig.preferIPv6);
            }
        } else if (id == nameOrderRow) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(getString("NameOrder"));
            builder.setItems(new CharSequence[]{
                    getString("FirstLast"),
                    getString("LastFirst")
            }, (dialog, which) -> {
                HudConfig.setNameOrder(which == 0 ? 1 : 2);
                item.textValue = getNameOrderString();
                listView.adapter.notifyItemChanged(position, PARTIAL);
                org.telegram.messenger.NotificationCenter.getInstance(currentAccount).postNotificationName(org.telegram.messenger.NotificationCenter.mainUserInfoChanged);
            });
            showDialog(builder.create());
        } else if (id == idTypeRow) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(getString("IdType"));
            builder.setItems(new CharSequence[]{
                    getString("IdTypeHidden"),
                    getString("IdTypeAPI"),
                    getString("IdTypeBOTAPI")
            }, (dialog, which) -> {
                HudConfig.setIdType(which);
                item.textValue = getIdTypeString();
                listView.adapter.notifyItemChanged(position, PARTIAL);
                org.telegram.messenger.NotificationCenter.getInstance(currentAccount).postNotificationName(org.telegram.messenger.NotificationCenter.mainUserInfoChanged);
            });
            showDialog(builder.create());
        }
    }

    @Override
    protected String getActionBarTitle() {
        return getString("HudSettingsOther");
    }

    @Override
    protected String getKey() {
        return "o";
    }
}
