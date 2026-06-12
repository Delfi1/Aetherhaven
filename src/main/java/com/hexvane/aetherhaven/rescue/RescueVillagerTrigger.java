package com.hexvane.aetherhaven.rescue;

import javax.annotation.Nonnull;

/** Config for a discoverable villager spawned when a town member breaks a trigger block. */
public record RescueVillagerTrigger(
    @Nonnull String triggerBlockTypeId,
    @Nonnull String rescueQuestId,
    @Nonnull String rescueNpcRoleId,
    @Nonnull String rescueBindingKind,
    @Nonnull String rescueDialogueTreeId,
    int columnScanMinDy,
    int columnScanMaxDy,
    @Nonnull String vanishParticleSystemId,
    @Nonnull String vanishSoundEventId,
    @Nonnull String logLabel
) {}
