package org.telegram.messenger.forkgram;

import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

/**
 * "Disable Global Sticker Search": keeps the sticker and emoji panels limited
 * to the packs the user has installed.
 *
 * Every request that pulls stickers or custom emoji from the global catalog
 * (keyword search, pack search, emoji-to-sticker lookups, trending packs) is
 * answered locally with an empty result instead of reaching the server, so no
 * screen can show packs the user did not add, whichever screen asked.
 * Telegram's own curated lookups (greeting and premium stickers, whose
 * emoticon contains a star) are left alone.
 */
public final class StickerSearchBlock {

    public static final String KEY = "disableGlobalStickerSearch";

    private static final String STAR = "⭐";

    private StickerSearchBlock() {
    }

    public static boolean isEnabled() {
        return MessagesController.getGlobalMainSettings().getBoolean(KEY, false);
    }

    /** An empty list to hand out instead of trending packs while the option is on. */
    public static <T> ArrayList<T> emptyIfEnabled(ArrayList<T> list) {
        return isEnabled() ? new ArrayList<>() : list;
    }

    /**
     * Returns an empty response for a request that would reach the global
     * sticker catalog, or null when the request should be sent as usual.
     */
    public static TLObject fakeResponse(TLObject request) {
        if (!isGlobalStickerRequest(request) || !isEnabled()) {
            return null;
        }
        if (request instanceof TLRPC.TL_messages_getStickers) {
            final String emoticon = ((TLRPC.TL_messages_getStickers) request).emoticon;
            if (emoticon != null && emoticon.contains(STAR)) {
                return null;
            }
            return new TLRPC.TL_messages_stickers();
        }
        if (request instanceof TLRPC.TL_messages_searchStickers) {
            return new TLRPC.TL_messages_foundStickers();
        }
        if (request instanceof TLRPC.TL_messages_searchCustomEmoji) {
            return new TLRPC.TL_emojiList();
        }
        if (request instanceof TLRPC.TL_messages_searchStickerSets
                || request instanceof TLRPC.TL_messages_searchEmojiStickerSets) {
            return new TLRPC.TL_messages_foundStickerSets();
        }
        return new TLRPC.TL_messages_featuredStickers();
    }

    private static boolean isGlobalStickerRequest(TLObject request) {
        return request instanceof TLRPC.TL_messages_searchStickerSets
                || request instanceof TLRPC.TL_messages_searchEmojiStickerSets
                || request instanceof TLRPC.TL_messages_searchStickers
                || request instanceof TLRPC.TL_messages_searchCustomEmoji
                || request instanceof TLRPC.TL_messages_getStickers
                || request instanceof TLRPC.TL_messages_getFeaturedStickers
                || request instanceof TLRPC.TL_messages_getOldFeaturedStickers
                || request instanceof TLRPC.TL_messages_getFeaturedEmojiStickers;
    }
}
