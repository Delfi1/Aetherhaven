package com.hexvane.aetherhaven.townsfolk;

import com.google.gson.annotations.SerializedName;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TownsfolkPoolCheckoutRecord {
    @SerializedName("characterId")
    private String characterId = "";

    @SerializedName("townId")
    private String townId = "";

    @SerializedName("entityUuid")
    private String entityUuid = "";

    @SerializedName("assignmentKind")
    private String assignmentKind = "";

    @SerializedName("activePersonalityId")
    private String activePersonalityId = "";

    @SerializedName("hired")
    private boolean hired;

    public TownsfolkPoolCheckoutRecord() {}

    public TownsfolkPoolCheckoutRecord(
        @Nonnull String characterId,
        @Nonnull String townId,
        @Nonnull String entityUuid,
        @Nonnull String assignmentKind,
        @Nonnull String activePersonalityId
    ) {
        this(characterId, townId, entityUuid, assignmentKind, activePersonalityId, false);
    }

    public TownsfolkPoolCheckoutRecord(
        @Nonnull String characterId,
        @Nonnull String townId,
        @Nonnull String entityUuid,
        @Nonnull String assignmentKind,
        @Nonnull String activePersonalityId,
        boolean hired
    ) {
        this.characterId = characterId;
        this.townId = townId;
        this.entityUuid = entityUuid;
        this.assignmentKind = assignmentKind;
        this.activePersonalityId = activePersonalityId;
        this.hired = hired;
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

    @Nonnull
    public String getActivePersonalityId() {
        return activePersonalityId != null ? activePersonalityId : "";
    }

    public boolean isHired() {
        return hired;
    }

    public void setHired(boolean hired) {
        this.hired = hired;
    }
}
