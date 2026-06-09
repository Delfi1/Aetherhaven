package com.hexvane.aetherhaven.questboard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hexvane.aetherhaven.questboard.data.QuestBoardDefinitionJson;
import com.hexvane.aetherhaven.questboard.data.QuestBoardFetchEntryJson;
import com.hexvane.aetherhaven.questboard.data.QuestBoardHuntEntryJson;
import com.hexvane.aetherhaven.questboard.data.QuestBoardRaidEntryJson;
import com.hexvane.aetherhaven.questboard.data.QuestBoardQuestTypeWeightJson;
import com.hexvane.aetherhaven.questboard.data.QuestBoardRankTierJson;
import com.hexvane.aetherhaven.questboard.data.QuestBoardVillagerJson;
import com.hypixel.hytale.logger.HytaleLogger;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class QuestBoardCatalog {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String CONFIG_PATH = "Server/Aetherhaven/quest_board.json";

    private final QuestBoardDefinitionJson definition;

    private QuestBoardCatalog(@Nonnull QuestBoardDefinitionJson definition) {
        this.definition = definition;
    }

    @Nonnull
    public static QuestBoardCatalog empty() {
        QuestBoardDefinitionJson def = new QuestBoardDefinitionJson();
        return new QuestBoardCatalog(def);
    }

    @Nonnull
    public static QuestBoardCatalog loadFromAssetPacksOrClasspath(@Nonnull ClassLoader classLoader) {
        Gson gson = new GsonBuilder().create();
        QuestBoardDefinitionJson loaded = null;
        com.hypixel.hytale.server.core.asset.AssetModule module = com.hypixel.hytale.server.core.asset.AssetModule.get();
        if (module != null) {
            for (com.hypixel.hytale.assetstore.AssetPack pack : module.getAssetPacks()) {
                Path file = pack.getRoot().resolve(CONFIG_PATH);
                if (!Files.isRegularFile(file)) {
                    continue;
                }
                try (InputStream in = Files.newInputStream(file)) {
                    loaded = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), QuestBoardDefinitionJson.class);
                } catch (Exception e) {
                    LOGGER.atSevere().withCause(e).log("Failed to load quest board config %s", file);
                }
            }
        }
        if (loaded == null) {
            try (InputStream in = classLoader.getResourceAsStream(CONFIG_PATH)) {
                if (in != null) {
                    loaded = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), QuestBoardDefinitionJson.class);
                }
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e).log("Failed to load quest board config from classpath %s", CONFIG_PATH);
            }
        }
        if (loaded == null) {
            LOGGER.atWarning().log("No quest board config found; using empty defaults");
            return empty();
        }
        if (loaded.schemaVersion() != QuestBoardDefinitionJson.SUPPORTED_SCHEMA_VERSION) {
            LOGGER.atWarning().log(
                "Quest board schemaVersion %s (expected %s)",
                loaded.schemaVersion(),
                QuestBoardDefinitionJson.SUPPORTED_SCHEMA_VERSION
            );
        }
        LOGGER.atInfo().log(
            "Loaded quest board config (%s rank tiers, %s villager roles)",
            loaded.ranksOrEmpty().size(),
            loaded.villagersOrEmpty().size()
        );
        return new QuestBoardCatalog(loaded);
    }

    public int slotCount() {
        return definition.slotCount();
    }

    @Nonnull
    public List<QuestBoardRankTierJson> ranks() {
        return definition.ranksOrEmpty();
    }

    @Nonnull
    public Map<String, QuestBoardVillagerJson> villagers() {
        return definition.villagersOrEmpty();
    }

    @Nullable
    public QuestBoardVillagerJson villager(@Nonnull String npcRoleId) {
        return villagers().get(npcRoleId.trim());
    }

    @Nonnull
    public List<QuestBoardFetchEntryJson> fetchEntriesForRole(@Nonnull String npcRoleId) {
        QuestBoardVillagerJson v = villager(npcRoleId);
        return v != null ? v.fetchEntriesOrEmpty() : List.of();
    }

    @Nonnull
    public List<QuestBoardHuntEntryJson> huntEntriesForRole(@Nonnull String npcRoleId) {
        QuestBoardVillagerJson v = villager(npcRoleId);
        return v != null ? v.huntEntriesOrEmpty() : List.of();
    }

    @Nonnull
    public List<QuestBoardRaidEntryJson> raidEntriesForRole(@Nonnull String npcRoleId) {
        QuestBoardVillagerJson v = villager(npcRoleId);
        return v != null ? v.raidEntriesOrEmpty() : List.of();
    }

    @Nonnull
    public Map<String, QuestBoardQuestTypeWeightJson> questTypes() {
        return definition.questTypesOrEmpty();
    }

    public int questTypeWeight(@Nonnull String typeId) {
        QuestBoardQuestTypeWeightJson w = questTypes().get(typeId.trim());
        return w != null ? w.weight() : 0;
    }

    @Nullable
    public QuestBoardRankTierJson rankTier(@Nonnull String rankId) {
        String id = rankId.trim();
        for (QuestBoardRankTierJson tier : ranks()) {
            if (id.equalsIgnoreCase(tier.idOrEmpty())) {
                return tier;
            }
        }
        return null;
    }

    public int defaultXpRewardForRank(@Nonnull String rankId) {
        QuestBoardRankTierJson tier = rankTier(rankId);
        return tier != null ? tier.xpReward() : 0;
    }

    @Nullable
    public String iconForRank(@Nonnull String rankId) {
        QuestBoardRankTierJson tier = rankTier(rankId);
        return tier != null ? tier.icon() : null;
    }
}
