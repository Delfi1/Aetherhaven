package com.hexvane.aetherhaven.shopspot;

import com.hexvane.aetherhaven.town.TownRecord;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Resolves look-at HUD hint lang keys for the viewing player and stall state. */
public final class ShopSpotHudHints {
    private static final String MSG = "aetherhaven_shop.aetherhaven.shop.hud";

    private ShopSpotHudHints() {}

    @Nullable
    public static String hintTranslationKey(
        @Nonnull ShopSpotRecord record,
        @Nonnull TownRecord town,
        @Nonnull UUID viewerUuid,
        boolean gameDay
    ) {
        if (!gameDay) {
            return null;
        }
        if (!record.isPlayerControlled()) {
            return MSG + ".hintNpc";
        }
        boolean canSell = town.playerCanUseShopSpots(viewerUuid);
        UUID seller = record.getSellerUuid();
        boolean ownsListing =
            seller != null && record.hasStock() && seller.equals(viewerUuid);
        if (ownsListing) {
            return MSG + ".hintPlayerOwn";
        }
        if (canSell && !record.hasStock()) {
            return MSG + ".hintPlayerList";
        }
        if (record.hasStock()) {
            return MSG + ".hintPlayerBuy";
        }
        return MSG + ".hintPlayerEmpty";
    }
}
