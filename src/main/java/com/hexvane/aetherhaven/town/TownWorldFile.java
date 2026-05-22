package com.hexvane.aetherhaven.town;

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
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

/** Gson root for {@code towns.json} per world. */
public final class TownWorldFile {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @com.google.gson.annotations.SerializedName("towns")
    private List<TownRecord> towns = new ArrayList<>();

    @Nonnull
    public List<TownRecord> getTowns() {
        if (towns == null) {
            towns = new ArrayList<>();
        }
        return towns;
    }

    public static TownWorldFile readOrEmpty(@Nonnull Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            return new TownWorldFile();
        }
        try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            TownWorldFile f = GSON.fromJson(r, TownWorldFile.class);
            return f != null ? f : new TownWorldFile();
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
            replaceDestinationFromTemp(tmp, path);
        } catch (IOException e) {
            if (isReplaceBlockedOnWindows(e)) {
                replaceDestinationFromTemp(tmp, path);
            } else {
                throw e;
            }
        }
    }

    /**
     * Windows / synced folders often reject {@link Files#move} into an existing file; copy-over is more reliable.
     */
    private static void replaceDestinationFromTemp(@Nonnull Path tmp, @Nonnull Path path) throws IOException {
        try {
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException moveFailed) {
            if (!isReplaceBlockedOnWindows(moveFailed)) {
                throw moveFailed;
            }
            Files.copy(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(tmp);
        }
    }

    private static boolean isReplaceBlockedOnWindows(@Nonnull IOException e) {
        return e instanceof java.nio.file.AccessDeniedException
            || (e.getCause() instanceof java.nio.file.AccessDeniedException);
    }
}
