package com.hudgram.translator;

import android.text.TextUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import java.util.ArrayList;

public class Translator {

    public interface TranslateCallBack {
        void onSuccess(TLRPC.TL_textWithEntities translation, String sourceLanguage, String targetLanguage);
        void onError(Throwable t);
    }

    public interface SequentialTranslateCallback {
        void onResult(int index, TLRPC.TL_textWithEntities translation, Throwable error);
    }

    public static void translateSequentially(
        ArrayList<TLRPC.TL_textWithEntities> texts,
        String fl,
        String tl,
        int delayMs,
        SequentialTranslateCallback callback
    ) {
        translateSequentiallyInternal(texts, fl, tl, delayMs, callback, 0);
    }

    private static void translateSequentiallyInternal(
        ArrayList<TLRPC.TL_textWithEntities> texts,
        String fl,
        String tl,
        int delayMs,
        SequentialTranslateCallback callback,
        int index
    ) {
        if (index >= texts.size()) {
            return;
        }
        translate(texts.get(index), null, fl, tl, new TranslateCallBack() {
            @Override
            public void onSuccess(TLRPC.TL_textWithEntities translation, String sourceLanguage, String targetLanguage) {
                if (callback != null) {
                    callback.onResult(index, translation, null);
                }
                AndroidUtilities.runOnUIThread(() -> {
                    translateSequentiallyInternal(texts, fl, tl, delayMs, callback, index + 1);
                }, delayMs);
            }

            @Override
            public void onError(Throwable t) {
                if (callback != null) {
                    callback.onResult(index, null, t);
                }
                AndroidUtilities.runOnUIThread(() -> {
                    translateSequentiallyInternal(texts, fl, tl, delayMs, callback, index + 1);
                }, delayMs);
            }
        });
    }

    public static boolean isLanguageRestricted(String language) {
        if (language == null) return false;
        java.util.HashSet<String> restricted = org.telegram.ui.RestrictedLanguagesSelectActivity.getRestrictedLanguages();
        return restricted.contains(language.toLowerCase());
    }

    public static TLRPC.TL_textWithEntities textWithEntities(String text, ArrayList<TLRPC.MessageEntity> entities) {
        TLRPC.TL_textWithEntities textWithEntities = new TLRPC.TL_textWithEntities();
        textWithEntities.text = text;
        if (entities != null) textWithEntities.entities = entities;
        return textWithEntities;
    }

    public static void translate(String text, ArrayList<TLRPC.MessageEntity> entities, org.telegram.messenger.TranslateController.PollText poll, String fl, String tl, TranslateCallBack translateCallBack) {
        TLRPC.TL_textWithEntities textWithEntities = new TLRPC.TL_textWithEntities();
        textWithEntities.text = text;
        textWithEntities.entities = entities != null ? entities : new ArrayList<>();
        translate(textWithEntities, poll, fl, tl, translateCallBack);
    }

    public static void translate(TLRPC.TL_textWithEntities query, org.telegram.messenger.TranslateController.PollText poll, String fl, String tl, TranslateCallBack translateCallBack) {
        final String defaultLocale = LocaleController.getInstance().getCurrentLocale().getLanguage();
        final String fromLang = normalizeLanguageCode(fl, "auto");
        final String toLang = "app".equalsIgnoreCase(tl) ? normalizeLanguageCode(defaultLocale, "en") : normalizeLanguageCode(tl, defaultLocale);

        if ("telegram".equals(com.hudgram.ui.HudConfig.translationProvider)) {
            int currentAccount = org.telegram.messenger.UserConfig.selectedAccount;
            TLRPC.TL_messages_translateText req = new TLRPC.TL_messages_translateText();
            req.flags |= 2;
            req.text.add(query);
            req.to_lang = toLang;
            org.telegram.tgnet.ConnectionsManager.getInstance(currentAccount).sendRequest(req, (res, err) -> {
                if (err != null) {
                    if (translateCallBack != null) {
                        AndroidUtilities.runOnUIThread(() -> translateCallBack.onError(new Exception(err.text)));
                    }
                } else if (res instanceof TLRPC.TL_messages_translateResult &&
                    !((TLRPC.TL_messages_translateResult) res).result.isEmpty() &&
                    ((TLRPC.TL_messages_translateResult) res).result.get(0) != null
                ) {
                    TLRPC.TL_textWithEntities translatedText = ((TLRPC.TL_messages_translateResult) res).result.get(0);
                    if (translateCallBack != null) {
                        AndroidUtilities.runOnUIThread(() -> translateCallBack.onSuccess(translatedText, fromLang, toLang));
                    }
                } else {
                    if (translateCallBack != null) {
                        AndroidUtilities.runOnUIThread(() -> translateCallBack.onError(new Exception("Unknown translation result")));
                    }
                }
            });
            return;
        }

        Utilities.globalQueue.postRunnable(() -> {
            try {
                // Convert entities to HTML
                String html = com.hudgram.translator.html.HTMLKeeper.entitiesToHtml(query.text, query.entities, true);
                
                // Translate the HTML
                String translatedHtml = translateGoogle(html, fromLang, toLang);
                
                // Convert HTML back to entities
                TLRPC.TL_textWithEntities translatedText = com.hudgram.translator.html.HTMLKeeper.htmlToEntities(translatedHtml, null, true, true);
                
                AndroidUtilities.runOnUIThread(() -> {
                    if (translateCallBack != null) {
                        translateCallBack.onSuccess(translatedText, fromLang, toLang);
                    }
                });
            } catch (Exception e) {
                AndroidUtilities.runOnUIThread(() -> {
                    if (translateCallBack != null) {
                        translateCallBack.onError(e);
                    }
                });
            }
        });
    }

    private static final String[] USER_AGENTS = new String[] {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.3 Safari/605.1.15",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.3.1 Mobile/15E148 Safari/604.1",
        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
    };

    private static String translateGoogle(String text, String sl, String tl) throws Exception {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        // Try Method 1: gtx client (standard Google Translate Extension client)
        try {
            return translateGoogleWithClient(text, sl, tl, "gtx");
        } catch (Exception e1) {
            org.telegram.messenger.FileLog.e("Translator: main client gtx failed, trying dictionary fallback", e1);
            // Try Method 2: dict-chrome-ex client (Chrome Dictionary Extension, highly stable fallback)
            try {
                return translateGoogleWithClient(text, sl, tl, "dict-chrome-ex");
            } catch (Exception e2) {
                org.telegram.messenger.FileLog.e("Translator: dictionary fallback failed, trying GET method", e2);
                // Try Method 3: GET request to gtx (as a last resort, since GET requests sometimes bypass post limits)
                try {
                    return translateGoogleGet(text, sl, tl);
                } catch (Exception e3) {
                    org.telegram.messenger.FileLog.e("Translator: all Google Translate methods failed", e3);
                    throw e3;
                }
            }
        }
    }

    private static String translateGoogleWithClient(String text, String sl, String tl, String client) throws Exception {
        String baseUrl = "dict-chrome-ex".equals(client) 
            ? "https://clients5.google.com/translate_a/t" 
            : "https://translate.googleapis.com/translate_a/single";
        
        String urlStr = baseUrl + "?client=" + client 
            + "&dt=t&ie=UTF-8&oe=UTF-8&sl=" + java.net.URLEncoder.encode(sl, "UTF-8") 
            + "&tl=" + java.net.URLEncoder.encode(tl, "UTF-8");
        
        java.net.URL url = new java.net.URL(urlStr);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        
        // Use a random modern user agent
        String userAgent = USER_AGENTS[(int) (Math.random() * USER_AGENTS.length)];
        conn.setRequestProperty("User-Agent", userAgent);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
        conn.setRequestProperty("Accept", "*/*");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9,ar;q=0.8");
        conn.setRequestProperty("Referer", "https://translate.google.com/");
        conn.setRequestProperty("Connection", "keep-alive");
        
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setDoOutput(true);
        
        String postData = "q=" + java.net.URLEncoder.encode(text, "UTF-8");
        byte[] postDataBytes = postData.getBytes("UTF-8");
        conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
        
        try (java.io.OutputStream outStream = conn.getOutputStream()) {
            outStream.write(postDataBytes);
            outStream.flush();
        }
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("HTTP error code: " + responseCode + " (" + conn.getResponseMessage() + ")");
        }
        
        StringBuilder response = new StringBuilder();
        try (java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }
        
        return parseGoogleJson(response.toString());
    }

    private static String translateGoogleGet(String text, String sl, String tl) throws Exception {
        String urlStr = "https://translate.googleapis.com/translate_a/single?client=gtx"
            + "&dt=t&ie=UTF-8&oe=UTF-8&sl=" + java.net.URLEncoder.encode(sl, "UTF-8") 
            + "&tl=" + java.net.URLEncoder.encode(tl, "UTF-8")
            + "&q=" + java.net.URLEncoder.encode(text, "UTF-8");
        
        java.net.URL url = new java.net.URL(urlStr);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        
        String userAgent = USER_AGENTS[(int) (Math.random() * USER_AGENTS.length)];
        conn.setRequestProperty("User-Agent", userAgent);
        conn.setRequestProperty("Accept", "*/*");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9,ar;q=0.8");
        conn.setRequestProperty("Referer", "https://translate.google.com/");
        
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("GET HTTP error code: " + responseCode + " (" + conn.getResponseMessage() + ")");
        }
        
        StringBuilder response = new StringBuilder();
        try (java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }
        
        return parseGoogleJson(response.toString());
    }

    private static String parseGoogleJson(String jsonStr) throws Exception {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return "";
        }
        
        try {
            org.json.JSONArray responseArray = new org.json.JSONArray(jsonStr);
            if (responseArray.length() == 0) {
                return "";
            }
            
            Object firstElement = responseArray.get(0);
            if (firstElement instanceof org.json.JSONArray) {
                org.json.JSONArray sentencesArray = (org.json.JSONArray) firstElement;
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < sentencesArray.length(); i++) {
                    Object obj = sentencesArray.get(i);
                    if (obj instanceof org.json.JSONArray) {
                        org.json.JSONArray sentence = (org.json.JSONArray) obj;
                        if (!sentence.isNull(0)) {
                            result.append(sentence.getString(0));
                        }
                    } else if (obj instanceof String) {
                        result.append((String) obj);
                    }
                }
                return result.toString();
            } else if (firstElement instanceof String) {
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < responseArray.length(); i++) {
                    Object obj = responseArray.get(i);
                    if (obj instanceof String) {
                        result.append((String) obj);
                    }
                }
                return result.toString();
            }
        } catch (Exception e) {
            org.telegram.messenger.FileLog.e("Translator: failed to parse Google JSON: " + jsonStr, e);
            throw e;
        }
        throw new Exception("Unexpected Google Translate JSON format");
    }

    private static String normalizeLanguageCode(String code, String fallback) {
        if (code == null || code.isEmpty() || "und".equalsIgnoreCase(code) || "app".equalsIgnoreCase(code)) {
            return fallback;
        }
        code = code.replace('_', '-');
        if (code.contains("-") && !code.toLowerCase().startsWith("zh")) {
            int idx = code.indexOf('-');
            if (idx > 0) {
                code = code.substring(0, idx);
            }
        }
        return code.toLowerCase();
    }
}
