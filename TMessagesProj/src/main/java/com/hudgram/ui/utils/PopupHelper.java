package com.hudgram.ui.utils;

import android.content.Context;
import android.view.View;
import java.util.ArrayList;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;

public class PopupHelper {

    public interface OnClickListener {
        void onClick(int index);
    }

    public static void show(ArrayList<String> items, String title, int selectedIndex, Context context, View anchor, OnClickListener listener, Theme.ResourcesProvider resourcesProvider) {
        if (context == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);
        builder.setTitle(title);

        CharSequence[] charSequences = items.toArray(new CharSequence[0]);
        builder.setItems(charSequences, (dialog, which) -> {
            if (listener != null) {
                listener.onClick(which);
            }
        });
        builder.show();
    }
}
