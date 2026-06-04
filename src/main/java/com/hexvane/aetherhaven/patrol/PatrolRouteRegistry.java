package com.hexvane.aetherhaven.patrol;

import com.hypixel.hytale.server.core.universe.world.World;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** In memory patrol routes for a world. */
public final class PatrolRouteRegistry {
    @Nonnull
    private final World world;
    @Nonnull
    private final List<PatrolRouteRecord> records = new ArrayList<>();

    public PatrolRouteRegistry(@Nonnull World world) {
        this.world = world;
    }

    @Nonnull
    public World getWorld() {
        return world;
    }

    public void replaceAll(@Nonnull List<PatrolRouteRecord> list) {
        records.clear();
        for (PatrolRouteRecord r : list) {
            if (r != null && r.id != null) {
                records.add(r);
            }
        }
    }

    public void upsert(@Nonnull PatrolRouteRecord r) {
        if (r.id == null) {
            return;
        }
        for (int i = 0; i < records.size(); i++) {
            PatrolRouteRecord e = records.get(i);
            if (e != null && r.id.equals(e.id)) {
                records.set(i, r);
                return;
            }
        }
        records.add(r);
    }

    @Nullable
    public PatrolRouteRecord get(@Nonnull UUID id) {
        String sid = id.toString();
        for (PatrolRouteRecord r : records) {
            if (r != null && sid.equals(r.id)) {
                return r;
            }
        }
        return null;
    }

    @Nullable
    public PatrolRouteRecord remove(@Nonnull UUID id) {
        for (int i = 0; i < records.size(); i++) {
            PatrolRouteRecord e = records.get(i);
            if (e != null && id.toString().equals(e.id)) {
                return records.remove(i);
            }
        }
        return null;
    }

    @Nonnull
    public List<PatrolRouteRecord> all() {
        return new ArrayList<>(records);
    }

    @Nonnull
    public List<PatrolRouteRecord> listByTown(@Nonnull UUID townId) {
        String tid = townId.toString();
        List<PatrolRouteRecord> out = new ArrayList<>();
        for (PatrolRouteRecord r : records) {
            if (r != null && tid.equals(r.townId)) {
                out.add(r);
            }
        }
        return out;
    }

    @Nonnull
    public List<PatrolRouteRecord> routesForGuard(@Nonnull UUID guardEntityUuid) {
        String gid = guardEntityUuid.toString();
        List<PatrolRouteRecord> out = new ArrayList<>();
        for (PatrolRouteRecord r : records) {
            if (r != null && gid.equals(r.assignedGuardUuid)) {
                out.add(r);
            }
        }
        out.sort((a, b) -> {
            String na = a != null ? a.safeDisplayName() : "";
            String nb = b != null ? b.safeDisplayName() : "";
            return na.compareToIgnoreCase(nb);
        });
        return out;
    }

    public void clearGuardAssignment(@Nonnull UUID guardEntityUuid) {
        String gid = guardEntityUuid.toString();
        for (PatrolRouteRecord r : records) {
            if (r != null && gid.equals(r.assignedGuardUuid)) {
                r.assignedGuardUuid = null;
            }
        }
    }

    /** @return true when any route assignment was updated */
    public boolean migrateGuardAssignment(@Nonnull UUID oldGuardEntityUuid, @Nonnull UUID newGuardEntityUuid) {
        if (oldGuardEntityUuid.equals(newGuardEntityUuid)) {
            return false;
        }
        String oldId = oldGuardEntityUuid.toString();
        String newId = newGuardEntityUuid.toString();
        boolean changed = false;
        for (PatrolRouteRecord r : records) {
            if (r != null && oldId.equals(r.assignedGuardUuid)) {
                r.assignedGuardUuid = newId;
                changed = true;
            }
        }
        return changed;
    }

    public void clearStaleGuardAssignments(@Nonnull java.util.function.Predicate<UUID> isValidGuard) {
        for (PatrolRouteRecord r : records) {
            if (r == null) {
                continue;
            }
            UUID g = r.getAssignedGuardUuidParsed();
            if (g != null && !isValidGuard.test(g)) {
                r.assignedGuardUuid = null;
            }
        }
    }

    @Nonnull
    public String nextDisplayName(@Nonnull UUID townId) {
        int max = 0;
        String prefix = "Patrol ";
        for (PatrolRouteRecord r : listByTown(townId)) {
            String name = r.safeDisplayName();
            if (name.startsWith(prefix)) {
                try {
                    max = Math.max(max, Integer.parseInt(name.substring(prefix.length()).trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return prefix + (max + 1);
    }

    @Nullable
    public PatrolRouteRecord findRouteNearNode(double x, double y, double z, double radiusSq) {
        PatrolRouteRecord best = null;
        double bestDist = Double.POSITIVE_INFINITY;
        for (PatrolRouteRecord r : records) {
            if (r == null || r.nodes == null) {
                continue;
            }
            for (PatrolRouteNode n : r.nodes) {
                if (n == null) {
                    continue;
                }
                double dx = n.x - x;
                double dy = n.y - y;
                double dz = n.z - z;
                double d2 = dx * dx + dy * dy + dz * dz;
                if (d2 <= radiusSq && d2 < bestDist) {
                    bestDist = d2;
                    best = r;
                }
            }
        }
        return best;
    }
}
