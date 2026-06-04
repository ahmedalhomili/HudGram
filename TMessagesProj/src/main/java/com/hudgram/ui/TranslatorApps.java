package com.hudgram.ui;

import org.telegram.messenger.LocaleController;

public class TranslatorApps {

    public static class App {
        public String title;

        public App(String title) {
            this.title = title;
        }
    }

    public static App getTranslatorApp() {
        return new App(LocaleController.getString("GoogleTranslate", org.telegram.messenger.R.string.GoogleTranslate));
    }
}
