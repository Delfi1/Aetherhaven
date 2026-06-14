package com.hexvane.aetherhaven.guild.marker;

import com.hexvane.aetherhaven.construction.PrefabLocalOffset;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import javax.annotation.Nonnull;
import org.joml.Vector3d;
import org.joml.Vector3i;

/** World positions for guild hall adventurer anchors (plot creator locals, POI tool, NPC spawn). */
public final class GuildHallAdventurerSpawnPositions {
    private GuildHallAdventurerSpawnPositions() {}

    /**
     * Converts a prefab-local stand cell ({@code local[1]} is the block the NPC stands on) to a world anchor.
     * Matches {@link com.hexvane.aetherhaven.inn.InnkeeperSpawnService} ({@code wy} without {@code +0.5}).
     */
    @Nonnull
    public static Vector3d fromPrefabLocalStandCell(
        @Nonnull Vector3i prefabAnchorWorld,
        @Nonnull Rotation yaw,
        int localX,
        int localY,
        int localZ
    ) {
        Vector3i d = PrefabLocalOffset.rotate(yaw, localX, localY, localZ);
        return new Vector3d(
            prefabAnchorWorld.x + d.x + 0.5,
            prefabAnchorWorld.y + d.y,
            prefabAnchorWorld.z + d.z + 0.5
        );
    }
}
