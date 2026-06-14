package com.hexvane.aetherhaven.patrol;

import org.joml.Vector3d;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

@Immutable
public final class PatrolWandNode {
    @Nonnull
    private final UUID id;
    private final double x;
    private final double y;
    private final double z;

    public PatrolWandNode(@Nonnull UUID id, @Nonnull Vector3d pos) {
        this.id = id;
        this.x = pos.x();
        this.y = pos.y();
        this.z = pos.z();
    }

    @Nonnull
    public UUID getId() {
        return id;
    }

    @Nonnull
    public Vector3d getPosition() {
        return new Vector3d(x, y, z);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, x, y, z);
    }

    @Override
    public boolean equals(@Nonnull Object o) {
        if (!(o instanceof PatrolWandNode other)) {
            return false;
        }
        return id.equals(other.id)
            && Double.compare(x, other.x) == 0
            && Double.compare(y, other.y) == 0
            && Double.compare(z, other.z) == 0;
    }
}
