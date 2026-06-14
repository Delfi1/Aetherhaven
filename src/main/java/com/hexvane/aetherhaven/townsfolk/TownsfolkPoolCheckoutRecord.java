package com.hexvane.aetherhaven.townsfolk;

import com.google.gson.annotations.SerializedName;
import javax.annotation.Nonnull;

/** Persisted ledger row: one catalog character checked out into the world. */
public final class TownsfolkPoolCheckoutRecord {
    @SerializedName("characterId")
    private String characterId = "";

    @SerializedName("townId")
    private String townId = "";

    @SerializedName("entityUuid")
    private String entityUuid = "";

    @SerializedName("assignmentKind")
    private String assignmentKind = "";

    @SerializedName("instanceGeneration")
    private int instanceGeneration = 1;

    @SerializedName("activePersonalityId")
    private String activePersonalityId = "";

    public TownsfolkPoolCheckoutRecord() {}

    public TownsfolkPoolCheckoutRecord(
        @Nonnull String characterId,
        @Nonnull String townId,
        @Nonnull String entityUuid,
        @Nonnull String assignmentKind,
        @Nonnull String activePersonalityId
    ) {
        this(characterId, townId, entityUuid, assignmentKind, 1, activePersonalityId);
    }

    public TownsfolkPoolCheckoutRecord(
        @Nonnull String characterId,
        @Nonnull String townId,
        @Nonnull String entityUuid,
        @Nonnull String assignmentKind,
        int instanceGeneration,
        @Nonnull String activePersonalityId
    ) {
        this.characterId = characterId;
        this.townId = townId;
        this.entityUuid = entityUuid;
        this.assignmentKind = assignmentKind;
        this.instanceGeneration = Math.max(1, instanceGeneration);
        this.activePersonalityId = activePersonalityId;
    }

    @Nonnull
    public String getCharacterId() {
        return characterId != null ? characterId : "";
    }

    @Nonnull
    public String getTownId() {
        return townId != null ? townId : "";
    }

    @Nonnull
    public String getEntityUuid() {
        return entityUuid != null ? entityUuid : "";
    }

    @Nonnull
    public String getAssignmentKind() {
        return assignmentKind != null ? assignmentKind : "";
    }

    public int getInstanceGeneration() {
        return instanceGeneration;
    }

    @Nonnull
    public String getActivePersonalityId() {
        return activePersonalityId != null ? activePersonalityId : "";
    }

    public void setEntityUuid(@Nonnull String entityUuid) {
        this.entityUuid = entityUuid;
    }

    public void setAssignmentKind(@Nonnull String assignmentKind) {
        this.assignmentKind = assignmentKind;
    }

    public void setTownId(@Nonnull String townId) {
        this.townId = townId;
    }

    public void setInstanceGeneration(int instanceGeneration) {
        this.instanceGeneration = Math.max(1, instanceGeneration);
    }
}
