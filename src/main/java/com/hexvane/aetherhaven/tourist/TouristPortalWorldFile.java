package com.hexvane.aetherhaven.tourist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.hypixel.hytale.logger.HytaleLogger;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TouristPortalWorldFile {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @SerializedName("portals")
    private List<Row> portals = new ArrayList<>();

    @Nonnull
    public List<Row> getPortals() {
        if (portals == null) {
            portals = new ArrayList<>();
        }
        return portals;
    }

    public static final class Row {
        @Nullable
        public String portalId;
        @Nullable
        public String worldName;
        public int blockX;
        public int blockY;
        public int blockZ;
        @Nullable
        public String townId;
        @Nullable
        public String plotId;
        public long plannedDayEpochDay = Long.MIN_VALUE;
        @Nullable
        public List<Long> plannedSpawnEpochMinutes;
        @Nullable
        public List<Long> executedSpawnEpochMinutes;
    }

    @Nonnull
    public static TouristPortalWorldFile readOrEmpty(@Nonnull Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            return new TouristPortalWorldFile();
        }
        try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            TouristPortalWorldFile f = GSON.fromJson(r, TouristPortalWorldFile.class);
            return f != null ? f : new TouristPortalWorldFile();
        }
    }

    public void writeAtomic(@Nonnull Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path tmp = path.resolveSibling(path.getFileName().toString() + ".tmp");
        try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            GSON.toJson(this, w);
        }
        try {
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Nonnull
    public static TouristPortalWorldFile fromRecords(@Nonnull List<TouristPortalRecord> records) {
        TouristPortalWorldFile f = new TouristPortalWorldFile();
        for (TouristPortalRecord r : records) {
            Row row = new Row();
            row.portalId = r.getPortalId().toString();
            row.worldName = r.getWorldName();
            row.blockX = r.getBlockX();
            row.blockY = r.getBlockY();
            row.blockZ = r.getBlockZ();
            row.townId = r.getTownId().toString();
            row.plotId = r.getPlotId().toString();
            row.plannedDayEpochDay = r.getPlannedDayEpochDay();
            row.plannedSpawnEpochMinutes = new ArrayList<>(r.getPlannedSpawnEpochMinutes());
            row.executedSpawnEpochMinutes = new ArrayList<>(r.getExecutedSpawnEpochMinutes());
            f.getPortals().add(row);
        }
        return f;
    }

    @Nonnull
    public static List<TouristPortalRecord> toRecords(@Nonnull TouristPortalWorldFile file) {
        List<TouristPortalRecord> out = new ArrayList<>();
        for (Row row : file.getPortals()) {
            TouristPortalRecord r = rowToRecord(row);
            if (r != null) {
                out.add(r);
            }
        }
        return out;
    }

    @Nullable
    private static TouristPortalRecord rowToRecord(@Nonnull Row row) {
        if (row.portalId == null || row.portalId.isBlank()) {
            return null;
        }
        try {
            TouristPortalRecord r = new TouristPortalRecord();
            r.setPortalId(UUID.fromString(row.portalId.trim()));
            r.setWorldName(row.worldName != null ? row.worldName : "");
            r.setBlockPosition(new org.joml.Vector3i(row.blockX, row.blockY, row.blockZ));
            if (row.townId != null && !row.townId.isBlank()) {
                r.setTownId(UUID.fromString(row.townId.trim()));
            }
            if (row.plotId != null && !row.plotId.isBlank()) {
                r.setPlotId(UUID.fromString(row.plotId.trim()));
            }
            r.setPlannedDayEpochDay(row.plannedDayEpochDay);
            r.getPlannedSpawnEpochMinutes().clear();
            if (row.plannedSpawnEpochMinutes != null) {
                r.getPlannedSpawnEpochMinutes().addAll(row.plannedSpawnEpochMinutes);
            }
            r.getExecutedSpawnEpochMinutes().clear();
            if (row.executedSpawnEpochMinutes != null) {
                r.getExecutedSpawnEpochMinutes().addAll(row.executedSpawnEpochMinutes);
            }
            return r;
        } catch (IllegalArgumentException e) {
            LOGGER.atWarning().withCause(e).log("Skipping invalid tourist portal row %s", row.portalId);
            return null;
        }
    }
}
