package com.hexvane.aetherhaven.patrol;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
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

public final class PatrolRoutesWorldFile {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @SerializedName("routes")
    private List<PatrolRouteRecord> routes = new ArrayList<>();

    @Nonnull
    public List<PatrolRouteRecord> getRoutes() {
        if (routes == null) {
            routes = new ArrayList<>();
        }
        return routes;
    }

    @Nonnull
    public static PatrolRoutesWorldFile readOrEmpty(@Nonnull Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            return new PatrolRoutesWorldFile();
        }
        try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            PatrolRoutesWorldFile f = GSON.fromJson(r, PatrolRoutesWorldFile.class);
            return f != null ? f : new PatrolRoutesWorldFile();
        }
    }

    public void writeAtomic(@Nonnull Path path) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
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
