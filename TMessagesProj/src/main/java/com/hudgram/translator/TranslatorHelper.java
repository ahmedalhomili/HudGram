package com.hudgram.translator;
import com.hudgram.core.HudConfig;

import java.util.ArrayList;
import org.telegram.tgnet.TLRPC;

public class TranslatorHelper {
    public static final String DIVIDER = "\n\n───────────────────\n\n";
    public static final String LEGACY_DIVIDER = "\n\n--------------------\n\n";

    public static TLRPC.TL_textWithEntities preprocessShowOriginal(TLRPC.TL_textWithEntities source, TLRPC.TL_textWithEntities received) {
        if (com.hudgram.core.HudConfig.showOriginal && source != null && source.text != null && received != null && received.text != null) {
            TLRPC.TL_textWithEntities merged = new TLRPC.TL_textWithEntities();
            merged.text = received.text + DIVIDER + source.text;
            merged.entities = new ArrayList<>(received.entities);
            int shift = received.text.length() + DIVIDER.length();
            if (source.entities != null) {
                for (TLRPC.MessageEntity entity : source.entities) {
                    try {
                        TLRPC.MessageEntity cloned = entity.getClass().getDeclaredConstructor().newInstance();
                        cloned.offset = entity.offset + shift;
                        cloned.length = entity.length;
                        cloned.url = entity.url;
                        cloned.language = entity.language;
                        if (entity instanceof TLRPC.TL_messageEntityCustomEmoji) {
                            ((TLRPC.TL_messageEntityCustomEmoji) cloned).document_id = ((TLRPC.TL_messageEntityCustomEmoji) entity).document_id;
                            ((TLRPC.TL_messageEntityCustomEmoji) cloned).document = ((TLRPC.TL_messageEntityCustomEmoji) entity).document;
                        }
                        merged.entities.add(cloned);
                    } catch (Exception ignore) {}
                }
            }
            return merged;
        }
        return received;
    }

    public static CharSequence formatBilingualText(CharSequence text, int baseColorKey) {
        if (text == null) {
            return null;
        }
        
        int dividerIndex = -1;
        int dividerLength = 0;
        
        String str = text.toString();
        if (str.contains(DIVIDER)) {
            dividerIndex = str.indexOf(DIVIDER);
            dividerLength = DIVIDER.length();
        } else if (str.contains(LEGACY_DIVIDER)) {
            dividerIndex = str.indexOf(LEGACY_DIVIDER);
            dividerLength = LEGACY_DIVIDER.length();
        }
        
        if (dividerIndex != -1) {
            android.text.SpannableStringBuilder builder;
            if (text instanceof android.text.SpannableStringBuilder) {
                builder = (android.text.SpannableStringBuilder) text;
            } else {
                builder = new android.text.SpannableStringBuilder(text);
            }
            
            int dividerStart = dividerIndex;
            int dividerEnd = dividerIndex + dividerLength;
            int originalTextStart = dividerEnd;
            int originalTextEnd = builder.length();
            
            int baseColor = org.telegram.ui.ActionBar.Theme.getColor(baseColorKey);
            
            // Apply 30% opacity to divider (0.3 * 255 = 76.5 -> 0x4D)
            int dividerColor = (baseColor & 0x00FFFFFF) | 0x4D000000; 
            builder.setSpan(new android.text.style.ForegroundColorSpan(dividerColor), dividerStart, dividerEnd, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new android.text.style.RelativeSizeSpan(0.8f), dividerStart, dividerEnd, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            // Apply 60% opacity to original text (0.6 * 255 = 153 -> 0x99)
            int originalColor = (baseColor & 0x00FFFFFF) | 0x99000000;
            builder.setSpan(new android.text.style.ForegroundColorSpan(originalColor), originalTextStart, originalTextEnd, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new android.text.style.RelativeSizeSpan(0.85f), originalTextStart, originalTextEnd, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            return builder;
        }
        return text;
    }
}
