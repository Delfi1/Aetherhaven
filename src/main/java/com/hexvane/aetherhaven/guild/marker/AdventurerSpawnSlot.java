package com.hexvane.aetherhaven.guild.marker;

import javax.annotation.Nonnull;
import org.joml.Vector3d;

/** World spawn position and facing for a guild hall adventurer display slot. */
public record AdventurerSpawnSlot(@Nonnull Vector3d position, float yawRadians) {}
