package com.hexvane.aetherhaven.difficulty;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Gson root for {@code difficulty.json} per world. */
public final class WorldDifficultyFile {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @com.google.gson.annotations.SerializedName("difficulty")
    @Nullable
    private WorldDifficultyState state;

    @Nullable
    public WorldDifficultyState getState() {
        return state;
    }

    public void setState(@Nullable WorldDifficultyState state) {
        this.state = state;
    }

    public static WorldDifficultyFile readOrEmpty(@Nonnull Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            return new WorldDifficultyFile();
        }
        try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            WorldDifficultyFile f = GSON.fromJson(r, WorldDifficultyFile.class);
            return f != null ? f : new WorldDifficultyFile();
        }
    }

    public void writeAtomic(@Nonnull Path path) throws IOException {
        Path dir = path.getParent();
        if (dir != null) {
            Files.createDirectories(dir);
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
}
