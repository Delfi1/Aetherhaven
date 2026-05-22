package com.hexvane.aetherhaven.ui;

import com.hexvane.aetherhaven.AetherhavenConstants;
import com.hexvane.aetherhaven.AetherhavenPlugin;
import com.hexvane.aetherhaven.construction.ConstructionCatalog;
import com.hexvane.aetherhaven.construction.ConstructionDefinition;
import com.hexvane.aetherhaven.construction.MaterialRequirement;
import com.hexvane.aetherhaven.construction.PlotMaterialDepositService;
import com.hexvane.aetherhaven.construction.PrefabMaterialsCatalog;
import com.hexvane.aetherhaven.difficulty.EffectiveBuildingCosts;
import com.hexvane.aetherhaven.difficulty.WorldDifficultyState;
import com.hexvane.aetherhaven.economy.GoldCoinPayment;
import com.hexvane.aetherhaven.inventory.BenchAdjacentChestUtil;
import com.hexvane.aetherhaven.inventory.InventoryMaterials;
import com.hexvane.aetherhaven.plot.ManagementBlock;
import com.hexvane.aetherhaven.plot.PlotBlockRotationUtil;
import com.hexvane.aetherhaven.plot.PlotSignBlock;
import com.hexvane.aetherhaven.placement.PlotPlacementOpenHelper;
import com.hexvane.aetherhaven.construction.assembly.PlotAssemblyService;
import com.hexvane.aetherhaven.prefab.PrefabResolveUtil;
import com.hexvane.aetherhaven.production.ProductionCatalog;
import com.hexvane.aetherhaven.town.AetherhavenWorldRegistries;
import com.hexvane.aetherhaven.town.HouseResidentAssignment;
import com.hexvane.aetherhaven.town.WorkplacePlotAssignment;
import com.hexvane.aetherhaven.town.PlotInstance;
import com.hexvane.aetherhaven.town.PlotInstanceState;
import com.hexvane.aetherhaven.town.TownManager;
import com.hexvane.aetherhaven.town.TownMemberRole;
import com.hexvane.aetherhaven.town.TownMembershipActions;
import com.hexvane.aetherhaven.town.TownPlayerLookup;
import com.hexvane.aetherhaven.town.TownRecord;
import com.hexvane.aetherhaven.villager.TownVillagerBinding;
import com.hexvane.aetherhaven.villager.data.VillagerDefinition;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hexvane.aetherhaven.ui.AetherhavenInteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.prefab.selection.buffer.PrefabBufferUtil;
import com.hypixel.hytale.server.core.prefab.selection.buffer.impl.IPrefabBuffer;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PlotConstructionPage extends AetherhavenInteractiveCustomUIPage<PlotConstructionPage.PageData> {
    private static final int BREAK_SETTINGS = 10;
    private static final String MATERIALS_GRID = "#MaterialsScroll #MaterialsGrid";
    private static final int MATERIAL_GRID_COLS = 6;
    private static final String MEMBER_ROWS = "#MemberRows";
    private static final int MAX_MEMBER_ROWS = 24;

    private final Ref<ChunkStore> blockRef;
    @Nonnull
    private final Vector3i blockWorldPos;
    private final boolean managementUi;
    /** 0 = Plot, 1 = Players (management UI only). */
    private int managementTab;
    /** Move-building confirmation modal (management block, completed plot). */
    private boolean moveBuildingConfirmOpen;
    /** Pick-up plot confirmation modal (plot sign, blueprint plot). */
    private boolean pickUpPlotConfirmOpen;
    /** Open the move-building modal on the first {@link #build} (e.g. returning from town needs). */
    private boolean pendingMoveBuildingModal;
    /**
     * {@code append(ui)} must run only once per page instance; repeating it on every {@link #sendUpdate} duplicates the
     * whole tree and breaks selectors (wrong title, orphan "Materials" label, empty tabs).
     */
    private boolean templateAppended;
    /** House management: hide villagers who already have a home assigned on another completed house plot. */
    private boolean hideHouseResidentElsewhereHoused;
    /** Persist house-resident hide toggle per player across page instances. */
    private static final Map<UUID, Boolean> HOUSE_RESIDENT_HIDE_ELSEWHERE_PREF = new LinkedHashMap<>();

    public PlotConstructionPage(
        @Nonnull PlayerRef playerRef,
        @Nonnull Ref<ChunkStore> blockRef,
        @Nonnull Vector3i blockWorldPos,
        boolean managementUi
    ) {
        this(playerRef, blockRef, blockWorldPos, managementUi, 0, false);
    }

    public PlotConstructionPage(
        @Nonnull PlayerRef playerRef,
        @Nonnull Ref<ChunkStore> blockRef,
        @Nonnull Vector3i blockWorldPos,
        boolean managementUi,
        int initialManagementTab,
        boolean openMoveBuildingModalOnFirstBuild
    ) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageData.CODEC);
        this.blockRef = blockRef;
        this.blockWorldPos = blockWorldPos.clone();
        this.managementUi = managementUi;
        this.managementTab = initialManagementTab;
        this.pendingMoveBuildingModal = openMoveBuildingModalOnFirstBuild;
        UUID prefKey = playerRef.getUuid();
        if (prefKey != null) {
            Boolean saved = HOUSE_RESIDENT_HIDE_ELSEWHERE_PREF.get(prefKey);
            if (saved != null) {
                this.hideHouseResidentElsewhereHoused = saved;
            }
        }
    }

    @Override
    public void build(
        @Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store
    ) {
        if (!templateAppended) {
            commandBuilder.append("Aetherhaven/PlotConstructionPage.ui");
            templateAppended = true;
            AetherhavenUiLocalization.applyPlotConstructionPage(commandBuilder);
        }
        if (managementUi && pendingMoveBuildingModal) {
            moveBuildingConfirmOpen = true;
            pendingMoveBuildingModal = false;
        }
        commandBuilder.set(
            "#ShellTitleText.TextSpans",
            managementUi
                ? Message.translation("aetherhaven_ui_town.aetherhaven.ui.plotmanagement.title")
                : Message.translation("aetherhaven_ui_town.aetherhaven.ui.plotconstruction.title")
        );
        boolean plotTabActive = !managementUi || managementTab == 0;
        commandBuilder.set("#ManagementTabStrip.Visible", managementUi);
        commandBuilder.set("#PlotTabContent.Visible", plotTabActive);
        commandBuilder.set("#PlayersTabContent.Visible", managementUi && managementTab == 1);
        commandBuilder.set("#MoveBuildingModal.Visible", managementUi && moveBuildingConfirmOpen);
        commandBuilder.set("#PickUpPlotModal.Visible", !managementUi && pickUpPlotConfirmOpen);

        ConstructionDefinition def = resolveDefinition(store, ref);
        Player player = store.getComponent(ref, Player.getComponentType());
        boolean plotReqBypassCreative = player != null && player.getGameMode() == GameMode.Creative;
        CombinedItemContainer inv = materialCombinedForPlotBlock(store, ref);

        if (def == null) {
            commandBuilder.set(
                "#BuildingTitle.TextSpans",
                managementUi
                    ? Message.translation("aetherhaven_ui_shell.aetherhaven.ui.plotConstruction.buildingTitle")
                    : Message.translation("aetherhaven_ui_shell.aetherhaven.ui.plotConstruction.plotSignTitle")
            );
            commandBuilder.set(
                "#Description.TextSpans",
                Message.translation("aetherhaven_ui_shell.aetherhaven.ui.plotConstruction.noConstruction")
            );
            commandBuilder.set("#VillagerRow.Visible", false);
            commandBuilder.set("#TreasuryRow.Visible", false);
            commandBuilder.set("#HouseResidentRow.Visible", false);
            commandBuilder.set("#WorkplaceAssignRow.Visible", false);
            commandBuilder.set("#MaterialsHeader.Visible", false);
            commandBuilder.set("#MaterialsProgress.Visible", false);
            commandBuilder.set("#MaterialsScroll.Visible", false);
            commandBuilder.set("#PlotActionRow.Visible", false);
            commandBuilder.clear(MATERIALS_GRID);
            commandBuilder.set("#BuildButton.Disabled", true);
            commandBuilder.set("#PickUpPlotButton.Visible", false);
            commandBuilder.set("#TabNeedsButton.Disabled", true);
            commandBuilder.set("#TabMoveButton.Disabled", true);
            if (managementUi) {
                commandBuilder.set("#TabPlotButton.Disabled", managementTab == 0);
                commandBuilder.set("#TabPlayersButton.Disabled", managementTab == 1);
                bindManagementTabEvents(eventBuilder, false);
                if (managementTab == 1) {
                    buildManagementPlayersTab(ref, store, commandBuilder, eventBuilder);
                } else {
                    commandBuilder.clear(MEMBER_ROWS);
                }
            }
            return;
        }

        PlotInstanceState state = resolvePlotState(store, ref);
        boolean completed = state == PlotInstanceState.COMPLETE;
        boolean hideConstructionDetails = managementUi && completed;

        commandBuilder.set("#BuildingTitle.TextSpans", Message.raw(def.getDisplayName()));
        String desc = def.getDescription() != null ? def.getDescription() : "";
        if (completed) {
            if (!hideConstructionDetails) {
                desc = desc.isEmpty() ? "Construction complete." : desc + "\n\nConstruction complete.";
            }
        }
        commandBuilder.set("#Description.TextSpans", Message.raw(desc));
        commandBuilder.set("#Description.Visible", !desc.isBlank());

        commandBuilder.set("#VillagerRow.Visible", false);

        EffectiveBuildingCosts effectiveCosts = resolveEffectiveCosts(store, def);
        List<MaterialRequirement> requiredMaterials = effectiveCosts.getMaterials();
        long goldCost = effectiveCosts.getTreasuryGoldCoinCost();
        PlotInstance blueprintPlot = resolveBlueprintPlot(store, ref);
        TownRecord treasuryTown =
            !managementUi && !completed && goldCost > 0 ? resolveTownForPlotSign(store, ref) : null;
        UUIDComponent ucComp = store.getComponent(ref, UUIDComponent.getComponentType());
        UUID playerUuid = ucComp != null ? ucComp.getUuid() : null;
        boolean treasuryPerm =
            treasuryTown != null && playerUuid != null && treasuryTown.playerCanSpendTreasuryGold(playerUuid);
        long spendableGold =
            treasuryTown != null && inv != null
                ? GoldCoinPayment.totalAvailable(treasuryTown, inv, treasuryPerm)
                : inv != null ? InventoryMaterials.count(inv, AetherhavenConstants.ITEM_GOLD_COIN) : 0L;
        boolean treasuryOk =
            completed
                || goldCost <= 0
                || plotReqBypassCreative
                || (treasuryTown != null
                    && inv != null
                    && GoldCoinPayment.canAfford(treasuryTown, inv, goldCost, treasuryPerm));
        boolean showTreasury = !hideConstructionDetails && goldCost > 0;
        commandBuilder.set("#TreasuryRow.Visible", showTreasury);
        if (showTreasury) {
            commandBuilder.set(
                "#TreasuryLabel.TextSpans",
                Message.translation("aetherhaven_ui_shell.aetherhaven.ui.plotConstruction.treasuryGold")
                    .param("available", String.valueOf(spendableGold))
                    .param("required", String.valueOf(goldCost))
            );
            commandBuilder.set(
                "#TreasuryLabel.Style.TextColor",
                plotReqBypassCreative || spendableGold >= goldCost ? "#3d913f" : "#962f2f"
            );
        }

        boolean showMaterials = !hideConstructionDetails && !requiredMaterials.isEmpty();
        boolean showPlotActions = !managementUi && !completed;
        boolean showDeposit =
            showMaterials && showPlotActions && !plotReqBypassCreative && blueprintPlot != null;
        commandBuilder.set("#MaterialsHeader.Visible", showMaterials);
        commandBuilder.set("#MaterialsScroll.Visible", showMaterials);
        commandBuilder.set("#PlotActionRow.Visible", showPlotActions);
        commandBuilder.set("#DepositMaterialsButton.Visible", showDeposit);
        if (showMaterials && blueprintPlot != null) {
            buildMaterialsGrid(commandBuilder, eventBuilder, blueprintPlot, requiredMaterials, completed, plotReqBypassCreative);
        } else {
            commandBuilder.clear(MATERIALS_GRID);
            commandBuilder.set("#MaterialsProgress.Visible", false);
        }

        boolean matsOk =
            completed
                || plotReqBypassCreative
                || (blueprintPlot != null && PlotMaterialDepositService.allDeposited(blueprintPlot, requiredMaterials));
        boolean canBuild = !managementUi && !completed && matsOk && treasuryOk;
        commandBuilder.set("#BuildButton.Visible", showPlotActions);
        commandBuilder.set("#BuildButton.Disabled", !canBuild);

        boolean canPickupPlot =
            def.getPlotTokenItemId() != null && !def.getPlotTokenItemId().isBlank();
        commandBuilder.set("#PickUpPlotButton.Visible", showPlotActions);
        commandBuilder.set("#PickUpPlotButton.Disabled", !canPickupPlot);

        boolean needsMoveTabsOk = managementUi && completed;
        commandBuilder.set("#TabNeedsButton.Disabled", !needsMoveTabsOk);
        commandBuilder.set("#TabMoveButton.Disabled", !needsMoveTabsOk);
        if (managementUi) {
            commandBuilder.set("#TabPlotButton.Disabled", managementTab == 0);
            commandBuilder.set("#TabPlayersButton.Disabled", managementTab == 1);
        }

        boolean showHouseResident =
            managementUi
                && completed
                && AetherhavenConstants.CONSTRUCTION_PLOT_HOUSE.equals(def.getGameplayConstructionId());

        String gameplayWorkplaceId = "";
        AetherhavenPlugin plugWork = AetherhavenPlugin.get();
        if (plugWork != null) {
            gameplayWorkplaceId = plugWork.getConstructionCatalog().resolveGameplayConstructionId(def.getId());
        }
        boolean showWorkplaceAssign =
            managementUi && completed && ProductionCatalog.isProductionWorkplaceConstruction(gameplayWorkplaceId);

        commandBuilder.set("#HouseResidentRow.Visible", showHouseResident);
        commandBuilder.set("#WorkplaceAssignRow.Visible", showWorkplaceAssign);

        Store<ChunkStore> csMb = blockRef.getStore();
        ManagementBlock mbHouse = csMb.getComponent(blockRef, ManagementBlock.getComponentType());
        UUID plotUuidMgmt = null;
        UUID townUuidMgmt = null;
        if (mbHouse != null && mbHouse.getPlotId() != null && !mbHouse.getPlotId().isBlank()) {
            try {
                plotUuidMgmt = UUID.fromString(mbHouse.getPlotId().trim());
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (mbHouse != null && mbHouse.getTownId() != null && !mbHouse.getTownId().isBlank()) {
            try {
                townUuidMgmt = UUID.fromString(mbHouse.getTownId().trim());
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (showWorkplaceAssign && plotTabActive && plotUuidMgmt != null && townUuidMgmt != null && plugWork != null) {
            commandBuilder.set(
                "#WorkplaceAssignHint.TextSpans",
                Message.translation("aetherhaven_ui_town.aetherhaven.ui.plotconstruction.workplaceAssignHint")
            );
            World worldW = store.getExternalData().getWorld();
            TownManager tmw = AetherhavenWorldRegistries.getOrCreateTownManager(worldW, plugWork);
            TownRecord townW = tmw.getTown(townUuidMgmt);
            ObjectArrayList<DropdownEntryInfo> workEntries = new ObjectArrayList<>();
            String langW = this.playerRef.getLanguage() != null ? this.playerRef.getLanguage() : "en-US";
            String unWork =
                I18nModule.get().getMessage(langW, "aetherhaven_ui_shell.aetherhaven.ui.plotConstruction.houseResidentUnassigned");
            if (unWork == null || unWork.isEmpty()) {
                unWork = "Unassigned";
            }
            workEntries.add(new DropdownEntryInfo(LocalizableString.fromString(unWork), ""));
            String workSelected = "";
            if (townW != null) {
                List<WorkplaceAssignRow> wrows = collectWorkplaceAssignRows(store, townW, plugWork, gameplayWorkplaceId);
                for (WorkplaceAssignRow row : wrows) {
                    workEntries.add(new DropdownEntryInfo(LocalizableString.fromString(row.label()), row.entityUuid().toString()));
                }
                workSelected = findEntityUuidWithJobPlot(store, townW.getTownId(), plotUuidMgmt);
            }
            commandBuilder.set("#WorkplaceAssignDropdown #Input.Entries", workEntries);
            commandBuilder.set("#WorkplaceAssignDropdown #Input.Value", workSelected.isEmpty() ? "" : workSelected);
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#AssignWorkplaceButton",
                new EventData().append("Action", "AssignWorkplace").append("@WorkplaceAssignUuid", "#WorkplaceAssignDropdown #Input.Value"),
                false
            );
        }

        if (showHouseResident && plotTabActive) {
            commandBuilder.set(
                "#HouseResidentHint.TextSpans",
                Message.translation("aetherhaven_ui_shell.aetherhaven.ui.plotConstruction.assignResidentHint")
            );
            commandBuilder.set("#HouseResidentHideElsewhereCheckbox #CheckBox.Value", hideHouseResidentElsewhereHoused);
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#HouseResidentHideElsewhereCheckbox #CheckBox",
                EventData.of("@HouseResidentHideElsewhere", "#HouseResidentHideElsewhereCheckbox #CheckBox.Value"),
                false
            );
            ObjectArrayList<DropdownEntryInfo> resEntries = new ObjectArrayList<>();
            {
                String langU = this.playerRef.getLanguage() != null ? this.playerRef.getLanguage() : "en-US";
                String unLabel =
                    I18nModule.get().getMessage(langU, "aetherhaven_ui_shell.aetherhaven.ui.plotConstruction.houseResidentUnassigned");
                if (unLabel == null || unLabel.isEmpty()) {
                    unLabel = "Unassigned";
                }
                resEntries.add(new DropdownEntryInfo(LocalizableString.fromString(unLabel), ""));
            }
            String selectedValue = "";
            if (plotUuidMgmt != null && townUuidMgmt != null && plugWork != null) {
                World world = store.getExternalData().getWorld();
                TownManager townManager = AetherhavenWorldRegistries.getOrCreateTownManager(world, plugWork);
                TownRecord town = townManager.getTown(townUuidMgmt);
                if (town != null) {
                    PlotInstance pi = town.findPlotById(plotUuidMgmt);
                    UUID cur = pi != null ? pi.getHomeResidentEntityUuid() : null;
                    if (cur != null) {
                        selectedValue = cur.toString();
                    }
                    List<HouseResidentRow> rows =
                        collectHouseResidentRows(store, town, plugWork, plotUuidMgmt, hideHouseResidentElsewhereHoused);
                    for (HouseResidentRow row : rows) {
                        resEntries.add(
                            new DropdownEntryInfo(LocalizableString.fromString(row.label()), row.entityUuid().toString())
                        );
                    }
                }
            }
            commandBuilder.set("#HouseResidentDropdown #Input.Entries", resEntries);
            commandBuilder.set("#HouseResidentDropdown #Input.Value", selectedValue.isEmpty() ? "" : selectedValue);
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#AssignHouseResidentButton",
                new EventData().append("Action", "AssignHouseResident").append("@HouseResidentUuid", "#HouseResidentDropdown #Input.Value"),
                false
            );
        }

        if (showDeposit) {
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#DepositMaterialsButton",
                new EventData().append("Action", "DepositMaterials"),
                false
            );
        }
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#BuildButton",
            new EventData().append("Action", "Build"),
            false
        );
        if (!managementUi && canPickupPlot) {
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#PickUpPlotButton",
                new EventData().append("Action", "BeginPickUpPlot"),
                false
            );
        }

        if (managementUi) {
            bindManagementTabEvents(eventBuilder, needsMoveTabsOk);
            if (managementTab == 1) {
                buildManagementPlayersTab(ref, store, commandBuilder, eventBuilder);
            } else {
                commandBuilder.clear(MEMBER_ROWS);
            }
        }
        if (managementUi && moveBuildingConfirmOpen) {
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#MoveBuildingConfirmButton",
                new EventData().append("Action", "ConfirmMoveBuilding"),
                false
            );
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#MoveBuildingCancelButton",
                new EventData().append("Action", "CancelMoveBuilding"),
                false
            );
        }
        if (!managementUi && pickUpPlotConfirmOpen) {
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#PickUpPlotConfirmButton",
                new EventData().append("Action", "ConfirmPickUpPlot"),
                false
            );
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#PickUpPlotCancelButton",
                new EventData().append("Action", "CancelPickUpPlot"),
                false
            );
        }
    }

    private void bindManagementTabEvents(@Nonnull UIEventBuilder eventBuilder, boolean needsMoveTabsEnabled) {
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#TabPlotButton",
            new EventData().append("Action", "SwitchTabPlot"),
            false
        );
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#TabPlayersButton",
            new EventData().append("Action", "SwitchTabPlayers"),
            false
        );
        if (needsMoveTabsEnabled) {
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TabNeedsButton",
                new EventData().append("Action", "OpenTownNeeds"),
                false
            );
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TabMoveButton",
                new EventData().append("Action", "BeginMoveBuilding"),
                false
            );
        }
    }

    private void buildManagementPlayersTab(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull UIEventBuilder eventBuilder
    ) {
        World world = store.getExternalData().getWorld();
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        UUIDComponent uc = store.getComponent(ref, UUIDComponent.getComponentType());
        if (plugin == null || uc == null) {
            commandBuilder.set(
                "#PlayersHint.TextSpans",
                Message.translation("aetherhaven_ui_shell.aetherhaven.ui.plotConstruction.couldNotLoadMembers")
            );
            commandBuilder.clear(MEMBER_ROWS);
            return;
        }
        TownRecord town = resolveManagementTown(store);
        if (town == null) {
            commandBuilder.set(
                "#PlayersHint.TextSpans",
                Message.translation("aetherhaven_ui_shell.aetherhaven.ui.plotConstruction.townDataMissing")
            );
            commandBuilder.clear(MEMBER_ROWS);
            return;
        }
        UUID viewer = uc.getUuid();
        boolean viewerOwner = town.getOwnerUuid().equals(viewer);
        commandBuilder.set(
            "#PlayersHint.TextSpans",
            viewerOwner
                ? Message.translation("aetherhaven_ui_town.aetherhaven.ui.plotmanagement.playersHint")
                : Message.translation("aetherhaven_ui_town.aetherhaven.ui.plotmanagement.playersHintReadOnly")
        );
        commandBuilder.set("#InviteLabel.Visible", viewerOwner);
        commandBuilder.set("#InvitePlayerInput.Visible", viewerOwner);
        commandBuilder.set("#InviteSendButton.Visible", viewerOwner);
        if (viewerOwner) {
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#InviteSendButton",
                new EventData().append("Action", "InviteMember").append("@InviteName", "#InvitePlayerInput.Value"),
                false
            );
        }

        commandBuilder.clear(MEMBER_ROWS);

        List<UUID> ordered = new ArrayList<>();
        ordered.add(town.getOwnerUuid());
        List<UUID> mem = new ArrayList<>(town.getMemberPlayerUuids());
        mem.sort(Comparator.comparing(u -> TownPlayerLookup.displayNameForUuid(world, u), String.CASE_INSENSITIVE_ORDER));
        ordered.addAll(mem);

        int n = Math.min(ordered.size(), MAX_MEMBER_ROWS);
        for (int i = 0; i < n; i++) {
            UUID pid = ordered.get(i);
            boolean isOwner = pid.equals(town.getOwnerUuid());
            String rowPath = MEMBER_ROWS + "[" + i + "]";
            commandBuilder.append(MEMBER_ROWS, "Aetherhaven/TownMemberRow.ui");
            String display = TownPlayerLookup.displayNameForUuid(world, pid);
            commandBuilder.set(rowPath + " #OpenMemberName #NameLabel.TextSpans", Message.raw(display));
            if (viewerOwner) {
                commandBuilder.set(rowPath + " #OpenMemberName.Disabled", false);
                eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    rowPath + " #OpenMemberName",
                    new EventData().append("Action", "OpenMemberPermissions").append("MemberUuid", pid.toString()),
                    false
                );
            } else {
                commandBuilder.set(rowPath + " #OpenMemberName.Disabled", true);
            }
            if (isOwner) {
                commandBuilder.set(rowPath + " #RoleReadOnly.Visible", true);
                commandBuilder.set(rowPath + " #RoleReadOnly.TextSpans", Message.translation("aetherhaven_ui_town.aetherhaven.ui.plotmanagement.roleOwner"));
                commandBuilder.set(rowPath + " #KickButton.Visible", false);
            } else {
                TownMemberRole role = town.getMemberRoleOrNull(pid);
                String roleName = role != null ? role.name() : TownMemberRole.BOTH.name();
                commandBuilder.set(rowPath + " #RoleReadOnly.Visible", true);
                commandBuilder.set(rowPath + " #RoleReadOnly.TextSpans", Message.raw(roleName));
                if (viewerOwner) {
                    commandBuilder.set(rowPath + " #KickButton.Visible", true);
                    commandBuilder.set(
                        rowPath + " #KickButton.TextSpans",
                        Message.translation("aetherhaven_ui_town.aetherhaven.ui.plotmanagement.kick")
                    );
                    eventBuilder.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        rowPath + " #KickButton",
                        new EventData().append("Action", "KickMember").append("MemberUuid", pid.toString()),
                        false
                    );
                } else {
                    commandBuilder.set(rowPath + " #KickButton.Visible", false);
                }
            }
        }
    }

    @Nullable
    private TownRecord resolveManagementTown(@Nonnull Store<EntityStore> store) {
        if (!managementUi) {
            return null;
        }
        Store<ChunkStore> cs = blockRef.getStore();
        ManagementBlock mb = cs.getComponent(blockRef, ManagementBlock.getComponentType());
        if (mb == null || mb.getTownId() == null || mb.getTownId().isBlank()) {
            return null;
        }
        try {
            UUID tid = UUID.fromString(mb.getTownId().trim());
            AetherhavenPlugin p = AetherhavenPlugin.get();
            if (p == null) {
                return null;
            }
            World world = store.getExternalData().getWorld();
            return AetherhavenWorldRegistries.getOrCreateTownManager(world, p).getTown(tid);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        if (data.houseResidentHideElsewhere != null) {
            hideHouseResidentElsewhereHoused = data.houseResidentHideElsewhere;
            UUID prefKey = playerRef.getUuid();
            if (prefKey != null) {
                HOUSE_RESIDENT_HIDE_ELSEWHERE_PREF.put(prefKey, hideHouseResidentElsewhereHoused);
            }
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder ev = new UIEventBuilder();
            build(ref, cmd, ev, store);
            sendUpdate(cmd, ev, false);
            return;
        }
        if (data.action != null && data.action.equalsIgnoreCase("BeginMoveBuilding")) {
            if (!managementUi) {
                return;
            }
            PlotInstanceState stBegin = resolvePlotState(store, ref);
            if (stBegin != PlotInstanceState.COMPLETE) {
                return;
            }
            moveBuildingConfirmOpen = true;
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder ev = new UIEventBuilder();
            build(ref, cmd, ev, store);
            sendUpdate(cmd, ev, false);
            return;
        }
        if (data.action != null && data.action.equalsIgnoreCase("CancelMoveBuilding")) {
            moveBuildingConfirmOpen = false;
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder ev = new UIEventBuilder();
            build(ref, cmd, ev, store);
            sendUpdate(cmd, ev, false);
            return;
        }
        if (data.action != null && data.action.equalsIgnoreCase("ConfirmMoveBuilding")) {
            if (!managementUi) {
                return;
            }
            moveBuildingConfirmOpen = false;
            PlotInstanceState stMove = resolvePlotState(store, ref);
            if (stMove != PlotInstanceState.COMPLETE) {
                UICommandBuilder cmd = new UICommandBuilder();
                UIEventBuilder ev = new UIEventBuilder();
                build(ref, cmd, ev, store);
                sendUpdate(cmd, ev, false);
                return;
            }
            Store<ChunkStore> csMove = blockRef.getStore();
            ManagementBlock mbMove = csMove.getComponent(blockRef, ManagementBlock.getComponentType());
            if (mbMove == null || mbMove.getPlotId().isBlank() || mbMove.getTownId().isBlank()) {
                return;
            }
            UUID plotIdMove;
            UUID townIdMove;
            try {
                plotIdMove = UUID.fromString(mbMove.getPlotId().trim());
                townIdMove = UUID.fromString(mbMove.getTownId().trim());
            } catch (IllegalArgumentException e) {
                return;
            }
            Player playerMove = store.getComponent(ref, Player.getComponentType());
            if (playerMove != null) {
                PlotPlacementPage placementPage = PlotPlacementOpenHelper.openForMove(ref, store, playerRef, townIdMove, plotIdMove);
                if (placementPage != null) {
                    playerMove.getPageManager().openCustomPage(ref, store, placementPage);
                }
            }
            return;
        }
        if (data.action != null && data.action.equalsIgnoreCase("SwitchTabPlot")) {
            if (!managementUi) {
                return;
            }
            managementTab = 0;
            moveBuildingConfirmOpen = false;
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder ev = new UIEventBuilder();
            build(ref, cmd, ev, store);
            sendUpdate(cmd, ev, false);
            return;
        }
        if (data.action != null && data.action.equalsIgnoreCase("SwitchTabPlayers")) {
            if (!managementUi) {
                return;
            }
            managementTab = 1;
            moveBuildingConfirmOpen = false;
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder ev = new UIEventBuilder();
            build(ref, cmd, ev, store);
            sendUpdate(cmd, ev, false);
            return;
        }
        if (data.action != null && data.action.equalsIgnoreCase("OpenMemberPermissions")) {
            if (!managementUi || data.memberUuid == null) {
                return;
            }
            UUIDComponent uc = store.getComponent(ref, UUIDComponent.getComponentType());
            if (uc == null) {
                return;
            }
            AetherhavenPlugin plugin = AetherhavenPlugin.get();
            if (plugin == null) {
                return;
            }
            TownRecord town = resolveManagementTown(store);
            if (town == null || !town.getOwnerUuid().equals(uc.getUuid())) {
                return;
            }
            UUID targetId;
            try {
                targetId = UUID.fromString(data.memberUuid.trim());
            } catch (IllegalArgumentException e) {
                return;
            }
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                player.getPageManager()
                    .openCustomPage(
                        ref,
                        store,
                        new TownMemberPermissionsPage(playerRef, blockRef, blockWorldPos, town.getTownId(), targetId)
                    );
            }
            return;
        }
        if (data.action != null && data.action.equalsIgnoreCase("KickMember")) {
            if (!managementUi || data.memberUuid == null) {
                return;
            }
            UUIDComponent uc = store.getComponent(ref, UUIDComponent.getComponentType());
            if (uc == null) {
                return;
            }
            AetherhavenPlugin plugin = AetherhavenPlugin.get();
            World world = store.getExternalData().getWorld();
            if (plugin == null) {
                return;
            }
            TownRecord town = resolveManagementTown(store);
            if (town == null || !town.getOwnerUuid().equals(uc.getUuid())) {
                return;
            }
            UUID memberId;
            try {
                memberId = UUID.fromString(data.memberUuid.trim());
            } catch (IllegalArgumentException e) {
                return;
            }
            TownManager tm = AetherhavenWorldRegistries.getOrCreateTownManager(world, plugin);
            Message err = TownMembershipActions.tryKickMemberUuid(world, tm, town, playerRef, memberId);
            if (err != null) {
                playerRef.sendMessage(err);
            }
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder ev = new UIEventBuilder();
            build(ref, cmd, ev, store);
            sendUpdate(cmd, ev, false);
            return;
        }
        if (data.action != null && data.action.equalsIgnoreCase("InviteMember")) {
            if (!managementUi || data.inviteName == null) {
                return;
            }
            UUIDComponent uc = store.getComponent(ref, UUIDComponent.getComponentType());
            if (uc == null) {
                return;
            }
            AetherhavenPlugin plugin = AetherhavenPlugin.get();
            World world = store.getExternalData().getWorld();
            if (plugin == null) {
                return;
            }
            TownRecord town = resolveManagementTown(store);
            if (town == null || !town.getOwnerUuid().equals(uc.getUuid())) {
                return;
            }
            TownManager tm = AetherhavenWorldRegistries.getOrCreateTownManager(world, plugin);
            Message err = TownMembershipActions.tryInviteMember(world, tm, town, uc.getUuid(), playerRef, data.inviteName);
            if (err != null) {
                playerRef.sendMessage(err);
            }
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder ev = new UIEventBuilder();
            build(ref, cmd, ev, store);
            sendUpdate(cmd, ev, false);
            return;
        }
        if (data.action != null && data.action.equalsIgnoreCase("AssignWorkplace")) {
            if (!managementUi) {
                return;
            }
            PlotInstanceState stW = resolvePlotState(store, ref);
            if (stW != PlotInstanceState.COMPLETE) {
                return;
            }
            ConstructionDefinition defW = resolveDefinition(store, ref);
            AetherhavenPlugin pluginW = AetherhavenPlugin.get();
            if (defW == null || pluginW == null) {
                return;
            }
            String gw = pluginW.getConstructionCatalog().resolveGameplayConstructionId(defW.getId());
            if (!ProductionCatalog.isProductionWorkplaceConstruction(gw)) {
                return;
            }
            Store<ChunkStore> csw = blockRef.getStore();
            ManagementBlock mbw = csw.getComponent(blockRef, ManagementBlock.getComponentType());
            if (mbw == null || mbw.getPlotId().isBlank() || mbw.getTownId().isBlank()) {
                return;
            }
            UUID plotIdW;
            UUID townIdW;
            try {
                plotIdW = UUID.fromString(mbw.getPlotId().trim());
                townIdW = UUID.fromString(mbw.getTownId().trim());
            } catch (IllegalArgumentException e) {
                return;
            }
            World worldW = store.getExternalData().getWorld();
            TownManager tmw = AetherhavenWorldRegistries.getOrCreateTownManager(worldW, pluginW);
            TownRecord townW = tmw.getTown(townIdW);
            if (townW == null) {
                return;
            }
            String rawW = data.workplaceAssignUuid;
            if (rawW == null || rawW.isBlank()) {
                PlayerRef prw = store.getComponent(ref, PlayerRef.getComponentType());
                if (prw != null) {
                    prw.sendMessage(Message.translation("aetherhaven_ui_town.aetherhaven.ui.plotconstruction.workplacePickWorker"));
                }
                return;
            }
            UUID npcUuid;
            try {
                npcUuid = UUID.fromString(rawW.trim());
            } catch (IllegalArgumentException e) {
                PlayerRef prw = store.getComponent(ref, PlayerRef.getComponentType());
                if (prw != null) {
                    prw.sendMessage(Message.translation("aetherhaven_ui_town.aetherhaven.ui.plotconstruction.workplaceInvalidWorker"));
                }
                return;
            }
            String err =
                WorkplacePlotAssignment.tryAssignWorker(worldW, pluginW, townW, tmw, plotIdW, npcUuid, worldW.getEntityStore().getStore());
            PlayerRef prw = store.getComponent(ref, PlayerRef.getComponentType());
            if (err != null) {
                if (prw != null) {
                    prw.sendMessage(Message.raw(err));
                }
                return;
            }
            if (prw != null) {
                prw.sendMessage(Message.translation("aetherhaven_ui_town.aetherhaven.ui.plotconstruction.workplaceUpdated"));
            }
            UICommandBuilder cmdw = new UICommandBuilder();
            UIEventBuilder evw = new UIEventBuilder();
            build(ref, cmdw, evw, store);
            sendUpdate(cmdw, evw, false);
            return;
        }
        if (data.action != null && data.action.equalsIgnoreCase("AssignHouseResident")) {
            if (!managementUi) {
                return;
            }
            PlotInstanceState st = resolvePlotState(store, ref);
            if (st != PlotInstanceState.COMPLETE) {
                return;
            }
            ConstructionDefinition def = resolveDefinition(store, ref);
            if (def == null || !AetherhavenConstants.CONSTRUCTION_PLOT_HOUSE.equals(def.getGameplayConstructionId())) {
                return;
            }
            Store<ChunkStore> cs = blockRef.getStore();
            ManagementBlock mb = cs.getComponent(blockRef, ManagementBlock.getComponentType());
            if (mb == null || mb.getPlotId().isBlank() || mb.getTownId().isBlank()) {
                return;
            }
            UUID plotId;
            UUID townId;
            try {
                plotId = UUID.fromString(mb.getPlotId().trim());
                townId = UUID.fromString(mb.getTownId().trim());
            } catch (IllegalArgumentException e) {
                return;
            }
            AetherhavenPlugin plugin = AetherhavenPlugin.get();
            if (plugin == null) {
                return;
            }
            World world = store.getExternalData().getWorld();
            TownManager tm = AetherhavenWorldRegistries.getOrCreateTownManager(world, plugin);
            TownRecord town = tm.getTown(townId);
            if (town == null) {
                return;
            }
            UUID residentUuid = null;
            String raw = data.houseResidentUuid;
            if (raw != null && !raw.isBlank()) {
                try {
                    residentUuid = UUID.fromString(raw.trim());
                } catch (IllegalArgumentException e) {
                    sendBuildError(store, ref, "Invalid villager selection.");
                    return;
                }
            }
            HouseResidentAssignment.assignResident(town, plotId, residentUuid, tm, world, store, plugin.getConstructionCatalog());
            PlayerRef pr = store.getComponent(ref, PlayerRef.getComponentType());
            if (pr != null) {
                pr.sendMessage(
                    residentUuid == null
                        ? Message.translation("aetherhaven_ui_shell.aetherhaven.ui.plotConstruction.clearedHome")
                        : Message.translation("aetherhaven_ui_shell.aetherhaven.ui.plotConstruction.updatedHome")
                );
            }
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder ev = new UIEventBuilder();
            build(ref, cmd, ev, store);
            sendUpdate(cmd, ev, false);
            return;
        }
        if (data.action != null && data.action.equalsIgnoreCase("DepositMaterial")) {
            if (managementUi || data.materialIndex == null || data.materialIndex < 0) {
                return;
            }
            handleDepositMaterial(store, ref, data.materialIndex);
            return;
        }
        if (data.action != null && data.action.equalsIgnoreCase("DepositMaterials")) {
            if (managementUi) {
                return;
            }
            handleDepositAllMaterials(store, ref);
            return;
        }
        if (data.action != null && data.action.equalsIgnoreCase("BeginPickUpPlot")) {
            if (managementUi) {
                return;
            }
            String pickUpErr = validatePickUpPlotAllowed(store, ref);
            if (pickUpErr != null) {
                sendBuildError(store, ref, pickUpErr);
                return;
            }
            pickUpPlotConfirmOpen = true;
            refreshPage(ref, store);
            return;
        }
        if (data.action != null && data.action.equalsIgnoreCase("CancelPickUpPlot")) {
            pickUpPlotConfirmOpen = false;
            refreshPage(ref, store);
            return;
        }
        if (data.action != null && data.action.equalsIgnoreCase("ConfirmPickUpPlot")) {
            if (managementUi) {
                return;
            }
            pickUpPlotConfirmOpen = false;
            String pickUpErr = validatePickUpPlotAllowed(store, ref);
            if (pickUpErr != null) {
                sendBuildError(store, ref, pickUpErr);
                refreshPage(ref, store);
                return;
            }
            executePickUpPlot(store, ref);
            return;
        }
        if (data.action != null && data.action.equalsIgnoreCase("OpenTownNeeds")) {
            if (!managementUi) {
                return;
            }
            moveBuildingConfirmOpen = false;
            PlotInstanceState st = resolvePlotState(store, ref);
            if (st != PlotInstanceState.COMPLETE) {
                return;
            }
            Store<ChunkStore> cs = blockRef.getStore();
            ManagementBlock mb = cs.getComponent(blockRef, ManagementBlock.getComponentType());
            if (mb == null || mb.getTownId().isBlank()) {
                return;
            }
            UUID townUuid;
            try {
                townUuid = UUID.fromString(mb.getTownId().trim());
            } catch (IllegalArgumentException e) {
                return;
            }
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                // openCustomPage replaces this UI; do not call close() or Page.None clears the new page.
                player.getPageManager()
                    .openCustomPage(ref, store, new VillagerNeedsOverviewPage(playerRef, townUuid, blockRef, blockWorldPos));
            }
            return;
        }
        if (data.action == null || !data.action.equalsIgnoreCase("Build")) {
            return;
        }
        if (managementUi) {
            return;
        }
        ConstructionDefinition def = resolveDefinition(store, ref);
        if (def == null) {
            return;
        }
        PlotInstanceState state = resolvePlotState(store, ref);
        if (state == PlotInstanceState.COMPLETE) {
            return;
        }
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        boolean plotReqBypassCreative = player.getGameMode() == GameMode.Creative;
        EffectiveBuildingCosts effectiveCosts = resolveEffectiveCosts(store, def);
        List<MaterialRequirement> requiredMaterials = effectiveCosts.getMaterials();
        PlotInstance blueprintPlot = resolveBlueprintPlot(store, ref);
        CombinedItemContainer inv = materialCombinedForPlotBlock(store, ref);
        if (!plotReqBypassCreative
            && (blueprintPlot == null || !PlotMaterialDepositService.allDeposited(blueprintPlot, requiredMaterials))) {
            return;
        }
        UUIDComponent uc = store.getComponent(ref, UUIDComponent.getComponentType());
        if (uc == null) {
            return;
        }
        Store<ChunkStore> cstore = blockRef.getStore();
        PlotSignBlock plot = cstore.getComponent(blockRef, PlotSignBlock.getComponentType());
        if (plot == null || plot.getPlotId() == null || plot.getPlotId().isBlank()) {
            sendBuildError(store, ref, "This plot sign has no plot id (legacy); replace the sign.");
            return;
        }
        UUID plotId;
        try {
            plotId = UUID.fromString(plot.getPlotId().trim());
        } catch (IllegalArgumentException e) {
            sendBuildError(store, ref, "Invalid plot id on sign.");
            return;
        }
        AetherhavenPlugin pluginEarly = AetherhavenPlugin.get();
        if (pluginEarly == null) {
            return;
        }
        World worldEarly = store.getExternalData().getWorld();
        TownManager tmEarly = AetherhavenWorldRegistries.getOrCreateTownManager(worldEarly, pluginEarly);
        TownRecord townEarly = tmEarly.findTownOwningPlot(plotId);
        PlotInstance plotEarly = townEarly != null ? townEarly.findPlotById(plotId) : null;
        if (townEarly == null || plotEarly == null) {
            sendBuildError(store, ref, "This plot is not registered in your town.");
            return;
        }
        Vector3i logicalSignEarly =
            new Vector3i(plotEarly.getSignX(), plotEarly.getSignY(), plotEarly.getSignZ());
        Rotation yawEarly = plotEarly.resolvePrefabYaw();
        Vector3i anchorEarly = def.resolvePrefabAnchorWorld(logicalSignEarly, yawEarly);
        Path prefabPathEarly = PrefabResolveUtil.resolvePrefabPath(def.getPrefabPath());
        if (prefabPathEarly == null) {
            sendBuildError(store, ref, "Prefab not found for path: " + def.getPrefabPath());
            return;
        }
        long goldCost = effectiveCosts.getTreasuryGoldCoinCost();
        if (goldCost > 0 && !plotReqBypassCreative) {
            AetherhavenPlugin plugPre = AetherhavenPlugin.get();
            World worldPre = store.getExternalData().getWorld();
            if (plugPre == null) {
                return;
            }
            TownManager tmPre = AetherhavenWorldRegistries.getOrCreateTownManager(worldPre, plugPre);
            TownRecord tr = tmPre.findTownOwningPlot(plotId);
            if (tr == null) {
                sendBuildError(store, ref, "No town owns this plot.");
                return;
            }
            boolean allowTreasury = tr.playerCanSpendTreasuryGold(uc.getUuid());
            if (!GoldCoinPayment.canAfford(tr, inv, goldCost, allowTreasury)) {
                sendBuildError(store, ref, "Not enough gold (inventory + town treasury).");
                return;
            }
            if (!GoldCoinPayment.trySpend(tr, inv, goldCost, allowTreasury)) {
                sendBuildError(store, ref, "Could not deduct construction gold.");
                return;
            }
            tmPre.updateTown(tr);
        }

        if (!plotReqBypassCreative && blueprintPlot != null) {
            PlotMaterialDepositService.clearDeposits(blueprintPlot);
            tmEarly.updateTown(townEarly);
        }

        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        if (plugin == null) {
            return;
        }
        World world = store.getExternalData().getWorld();
        Vector3i physicalSignPos = blockWorldPos;
        TownRecord townBuild = townEarly;
        PlotInstance buildPlot = plotEarly;
        Rotation yaw = yawEarly;
        Vector3i anchor = anchorEarly;
        IPrefabBuffer buffer = PrefabBufferUtil.getCached(prefabPathEarly);
        UUID ownerUuid = uc.getUuid();
        world.execute(
            () ->
                PlotAssemblyService.startFromBuildClick(
                    plugin,
                    world,
                    store,
                    townBuild,
                    buildPlot,
                    physicalSignPos,
                    ownerUuid,
                    anchor,
                    yaw,
                    def,
                    buffer
                )
        );
        close();
    }

    /** Player {@link InventoryComponent#EVERYTHING} plus chests in the same volume vanilla benches search. */
    @Nullable
    private CombinedItemContainer materialCombinedForPlotBlock(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        if (store.getComponent(ref, Player.getComponentType()) == null) {
            return null;
        }
        World world = store.getExternalData().getWorld();
        return BenchAdjacentChestUtil.combinedPlayerAndAdjacentChestsForBlock(
            world, store, ref, blockWorldPos.x, blockWorldPos.y, blockWorldPos.z
        );
    }

    private void refreshPage(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder ev = new UIEventBuilder();
        build(ref, cmd, ev, store);
        sendUpdate(cmd, ev, false);
    }

    /** Returns an error message when pick-up is not allowed, or null if it may proceed. */
    @Nullable
    private String validatePickUpPlotAllowed(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        if (managementUi) {
            return null;
        }
        if (resolvePlotState(store, ref) == PlotInstanceState.COMPLETE) {
            return null;
        }
        ConstructionDefinition def = resolveDefinition(store, ref);
        if (def == null) {
            return null;
        }
        String tokenId = def.getPlotTokenItemId();
        if (tokenId == null || tokenId.isBlank()) {
            return null;
        }
        Store<ChunkStore> cstore = blockRef.getStore();
        PlotSignBlock plot = cstore.getComponent(blockRef, PlotSignBlock.getComponentType());
        if (plot == null || plot.getPlotId() == null || plot.getPlotId().isBlank()) {
            return "This plot sign has no plot id (legacy); replace the sign.";
        }
        UUID plotId;
        try {
            plotId = UUID.fromString(plot.getPlotId().trim());
        } catch (IllegalArgumentException e) {
            return "Invalid plot id on sign.";
        }
        UUIDComponent uc = store.getComponent(ref, UUIDComponent.getComponentType());
        if (uc == null) {
            return null;
        }
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        if (plugin == null) {
            return null;
        }
        World world = store.getExternalData().getWorld();
        TownManager tm = AetherhavenWorldRegistries.getOrCreateTownManager(world, plugin);
        TownRecord town = tm.findTownForPlayerInWorld(uc.getUuid());
        if (town == null || !town.getOwnerUuid().equals(uc.getUuid())) {
            return "Only the town owner can pick up this plot.";
        }
        if (town.findPlotById(plotId) == null) {
            return "This plot is not registered in your town.";
        }
        if (store.getComponent(ref, Player.getComponentType()) == null) {
            return null;
        }
        return null;
    }

    private void executePickUpPlot(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        ConstructionDefinition def = resolveDefinition(store, ref);
        if (def == null) {
            return;
        }
        String tokenId = def.getPlotTokenItemId();
        if (tokenId == null || tokenId.isBlank()) {
            return;
        }
        Store<ChunkStore> cstore = blockRef.getStore();
        PlotSignBlock plot = cstore.getComponent(blockRef, PlotSignBlock.getComponentType());
        if (plot == null || plot.getPlotId() == null || plot.getPlotId().isBlank()) {
            return;
        }
        UUID plotId;
        try {
            plotId = UUID.fromString(plot.getPlotId().trim());
        } catch (IllegalArgumentException e) {
            return;
        }
        UUIDComponent uc = store.getComponent(ref, UUIDComponent.getComponentType());
        if (uc == null) {
            return;
        }
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        if (plugin == null) {
            return;
        }
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        World world = store.getExternalData().getWorld();
        TownManager tm = AetherhavenWorldRegistries.getOrCreateTownManager(world, plugin);
        TownRecord town = tm.findTownForPlayerInWorld(uc.getUuid());
        if (town == null) {
            return;
        }
        PlotInstance piPickup = town.findPlotById(plotId);
        List<MaterialRequirement> refunded =
            piPickup != null ? PlotMaterialDepositService.refundAll(piPickup) : List.of();
        if (!town.removePlotInstance(plotId)) {
            sendBuildError(store, ref, "Could not remove plot from town data.");
            return;
        }
        tm.updateTown(town);
        TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
        Vector3d dropPos =
            tc != null
                ? tc.getPosition().clone()
                : new Vector3d(blockWorldPos.x + 0.5, blockWorldPos.y, blockWorldPos.z + 0.5);
        if (!refunded.isEmpty()) {
            PlotMaterialDepositService.refundToPlayer(player, ref, store, refunded, dropPos);
        }
        world.breakBlock(blockWorldPos.x, blockWorldPos.y, blockWorldPos.z, BREAK_SETTINGS);
        if (def.consumesPlotToken()) {
            ItemStack tokenStack = new ItemStack(tokenId, 1);
            ItemStackTransaction giveTx = player.giveItem(tokenStack, ref, store);
            if (!giveTx.succeeded() || !ItemStack.isEmpty(giveTx.getRemainder())) {
                List<ItemStack> tokenOverflow = new ArrayList<>();
                if (!giveTx.succeeded()) {
                    tokenOverflow.add(tokenStack);
                } else {
                    tokenOverflow.add(giveTx.getRemainder());
                }
                PlotMaterialDepositService.refundToPlayer(
                    player,
                    ref,
                    store,
                    tokenOverflow.stream()
                        .map(s -> MaterialRequirement.ofItem(s.getItemId(), s.getQuantity()))
                        .toList(),
                    dropPos
                );
            }
        }
        PlayerRef pr = store.getComponent(ref, PlayerRef.getComponentType());
        if (pr != null) {
            pr.sendMessage(Message.translation("aetherhaven_ui_shell.aetherhaven.ui.plotConstruction.plotRemoved"));
        }
        close();
    }

    private void sendBuildError(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull String text) {
        PlayerRef pr = store.getComponent(ref, PlayerRef.getComponentType());
        if (pr != null) {
            pr.sendMessage(Message.raw(text));
        }
    }

    @Nullable
    private TownRecord resolveTownForPlotSign(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        AetherhavenPlugin p = AetherhavenPlugin.get();
        if (p == null) {
            return null;
        }
        Store<ChunkStore> cs = blockRef.getStore();
        PlotSignBlock plot = cs.getComponent(blockRef, PlotSignBlock.getComponentType());
        if (plot == null || plot.getPlotId() == null || plot.getPlotId().isBlank()) {
            return null;
        }
        UUID plotId;
        try {
            plotId = UUID.fromString(plot.getPlotId().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
        World world = store.getExternalData().getWorld();
        TownManager tm = AetherhavenWorldRegistries.getOrCreateTownManager(world, p);
        return tm.findTownOwningPlot(plotId);
    }

    @Nullable
    private ConstructionDefinition resolveDefinition(@Nonnull Store<EntityStore> entityStore, @Nonnull Ref<EntityStore> playerRef) {
        AetherhavenPlugin p = AetherhavenPlugin.get();
        if (p == null) {
            return null;
        }
        Store<ChunkStore> cs = blockRef.getStore();
        World world = entityStore.getExternalData().getWorld();
        TownManager tm = AetherhavenWorldRegistries.getOrCreateTownManager(world, p);

        if (managementUi) {
            ManagementBlock mb = cs.getComponent(blockRef, ManagementBlock.getComponentType());
            if (mb == null || mb.getTownId().isBlank() || mb.getPlotId().isBlank()) {
                return null;
            }
            try {
                TownRecord town = tm.getTown(UUID.fromString(mb.getTownId().trim()));
                if (town == null) {
                    return null;
                }
                PlotInstance pi = town.findPlotById(UUID.fromString(mb.getPlotId().trim()));
                if (pi == null) {
                    return null;
                }
                return p.getConstructionCatalog().get(pi.getConstructionId());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        PlotSignBlock plot = cs.getComponent(blockRef, PlotSignBlock.getComponentType());
        if (plot == null) {
            return null;
        }
        return p.getConstructionCatalog().get(plot.getConstructionId());
    }

    private void handleDepositAllMaterials(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        ConstructionDefinition def = resolveDefinition(store, ref);
        if (def == null) {
            return;
        }
        PlotInstance plot = resolveBlueprintPlot(store, ref);
        CombinedItemContainer inv = materialCombinedForPlotBlock(store, ref);
        if (plot == null || inv == null) {
            return;
        }
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        if (plugin == null) {
            return;
        }
        EffectiveBuildingCosts costs = resolveEffectiveCosts(store, def);
        for (MaterialRequirement line : costs.getMaterials()) {
            if (line.getCount() > 0) {
                PlotMaterialDepositService.depositFromContainer(plot, line, inv);
            }
        }
        World world = store.getExternalData().getWorld();
        TownManager tm = AetherhavenWorldRegistries.getOrCreateTownManager(world, plugin);
        TownRecord town = resolveTownForPlotSign(store, ref);
        if (town != null) {
            tm.updateTown(town);
        }
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder ev = new UIEventBuilder();
        build(ref, cmd, ev, store);
        sendUpdate(cmd, ev, false);
    }

    @Nonnull
    private void handleDepositMaterial(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, int materialIndex) {
        ConstructionDefinition def = resolveDefinition(store, ref);
        if (def == null) {
            return;
        }
        EffectiveBuildingCosts costs = resolveEffectiveCosts(store, def);
        List<MaterialRequirement> required = costs.getMaterials();
        if (materialIndex < 0 || materialIndex >= required.size()) {
            return;
        }
        PlotInstance plot = resolveBlueprintPlot(store, ref);
        CombinedItemContainer inv = materialCombinedForPlotBlock(store, ref);
        if (plot == null || inv == null) {
            return;
        }
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        if (plugin == null) {
            return;
        }
        MaterialRequirement line = required.get(materialIndex);
        int deposited = PlotMaterialDepositService.depositFromContainer(plot, line, inv);
        if (deposited <= 0) {
            return;
        }
        World world = store.getExternalData().getWorld();
        TownManager tm = AetherhavenWorldRegistries.getOrCreateTownManager(world, plugin);
        TownRecord town = resolveTownForPlotSign(store, ref);
        if (town != null) {
            tm.updateTown(town);
        }
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder ev = new UIEventBuilder();
        build(ref, cmd, ev, store);
        sendUpdate(cmd, ev, false);
    }

    private void buildMaterialsGrid(
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull UIEventBuilder eventBuilder,
        @Nonnull PlotInstance plot,
        @Nonnull List<MaterialRequirement> required,
        boolean completed,
        boolean creativeBypass
    ) {
        commandBuilder.clear(MATERIALS_GRID);
        List<Integer> sourceIndices = new ArrayList<>();
        List<MaterialRequirement> lines = new ArrayList<>();
        for (int i = 0; i < required.size(); i++) {
            MaterialRequirement line = required.get(i);
            if (line.getCount() > 0) {
                sourceIndices.add(i);
                lines.add(line);
            }
        }
        int totalLines = lines.size();
        int ready = 0;
        for (MaterialRequirement line : lines) {
            if (PlotMaterialDepositService.depositedCount(plot, line) >= line.getCount()) {
                ready++;
            }
        }
        if (totalLines == 0) {
            commandBuilder.set("#MaterialsProgress.Visible", false);
            return;
        }
        int numRows = (totalLines + MATERIAL_GRID_COLS - 1) / MATERIAL_GRID_COLS;
        for (int r = 0; r < numRows; r++) {
            commandBuilder.append(MATERIALS_GRID, "Aetherhaven/PlotConstructionMaterialRow.ui");
            String rowBase = MATERIALS_GRID + "[" + r + "]";
            for (int c = 0; c < MATERIAL_GRID_COLS; c++) {
                int idx = r * MATERIAL_GRID_COLS + c;
                if (idx >= totalLines) {
                    break;
                }
                MaterialRequirement line = lines.get(idx);
                int sourceIndex = sourceIndices.get(idx);
                int deposited = PlotMaterialDepositService.depositedCount(plot, line);
                commandBuilder.append(rowBase + " #Strip", "Aetherhaven/PlotConstructionMaterialCell.ui");
                String cell = rowBase + " #Strip[" + c + "]";
                String iconPath = UiMaterialIcons.assetPathFor(line);
                if (iconPath != null && !iconPath.isBlank()) {
                    commandBuilder.set(cell + " #IconBox #Icon.AssetPath", iconPath);
                }
                boolean ok = completed || creativeBypass || deposited >= line.getCount();
                commandBuilder.set(
                    cell + ".TooltipTextSpans",
                    Message.raw(UiMaterialLabels.displayLabelFor(line) + "\n" + deposited + " / " + line.getCount())
                );
                commandBuilder.set(cell + " #Count.TextSpans", Message.raw(deposited + " / " + line.getCount()));
                commandBuilder.set(cell + " #Count.Style.TextColor", ok ? "#3d913f" : "#c8a060");
                if (!completed && !creativeBypass) {
                    eventBuilder.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        cell,
                        new EventData().append("Action", "DepositMaterial").append("MaterialIndex", String.valueOf(sourceIndex)),
                        false
                    );
                }
            }
        }
        boolean showProgress = !completed && !creativeBypass;
        commandBuilder.set("#MaterialsProgress.Visible", showProgress);
        if (showProgress) {
            commandBuilder.set(
                "#MaterialsProgress.TextSpans",
                Message.translation("aetherhaven_ui_shell.aetherhaven.ui.plotConstruction.materialsProgress")
                    .param("ready", String.valueOf(ready))
                    .param("total", String.valueOf(totalLines))
            );
        }
    }

    @Nonnull
    private EffectiveBuildingCosts resolveEffectiveCosts(@Nonnull Store<EntityStore> store, @Nonnull ConstructionDefinition def) {
        AetherhavenPlugin plugin = AetherhavenPlugin.get();
        if (plugin == null) {
            return EffectiveBuildingCosts.forDefinition(def, WorldDifficultyState.normalUntilChosen(), PrefabMaterialsCatalog.empty());
        }
        World world = store.getExternalData().getWorld();
        WorldDifficultyState difficulty = AetherhavenWorldRegistries.getOrLoadWorldDifficulty(world, plugin);
        return EffectiveBuildingCosts.forDefinition(def, difficulty, plugin.getPrefabMaterialsCatalog());
    }

    @Nullable
    private PlotInstance resolveBlueprintPlot(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        AetherhavenPlugin p = AetherhavenPlugin.get();
        if (p == null) {
            return null;
        }
        World world = store.getExternalData().getWorld();
        TownManager tm = AetherhavenWorldRegistries.getOrCreateTownManager(world, p);
        Store<ChunkStore> cs = blockRef.getStore();
        if (managementUi) {
            return null;
        }
        PlotSignBlock plot = cs.getComponent(blockRef, PlotSignBlock.getComponentType());
        if (plot == null || plot.getPlotId() == null || plot.getPlotId().isBlank()) {
            return null;
        }
        UUIDComponent uc = store.getComponent(ref, UUIDComponent.getComponentType());
        if (uc == null) {
            return null;
        }
        TownRecord town = tm.findTownForPlayerInWorld(uc.getUuid());
        if (town == null) {
            return null;
        }
        try {
            return town.findPlotById(UUID.fromString(plot.getPlotId().trim()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private PlotInstanceState resolvePlotState(@Nonnull Store<EntityStore> entityStore, @Nonnull Ref<EntityStore> playerRef) {
        AetherhavenPlugin p = AetherhavenPlugin.get();
        if (p == null) {
            return PlotInstanceState.BLUEPRINTING;
        }
        World world = entityStore.getExternalData().getWorld();
        TownManager tm = AetherhavenWorldRegistries.getOrCreateTownManager(world, p);
        Store<ChunkStore> cs = blockRef.getStore();

        if (managementUi) {
            ManagementBlock mb = cs.getComponent(blockRef, ManagementBlock.getComponentType());
            if (mb == null || mb.getTownId().isBlank() || mb.getPlotId().isBlank()) {
                return PlotInstanceState.COMPLETE;
            }
            try {
                TownRecord town = tm.getTown(UUID.fromString(mb.getTownId().trim()));
                if (town == null) {
                    return PlotInstanceState.COMPLETE;
                }
                PlotInstance pi = town.findPlotById(UUID.fromString(mb.getPlotId().trim()));
                return pi != null ? pi.getState() : PlotInstanceState.COMPLETE;
            } catch (IllegalArgumentException e) {
                return PlotInstanceState.COMPLETE;
            }
        }

        PlotSignBlock plot = cs.getComponent(blockRef, PlotSignBlock.getComponentType());
        if (plot == null || plot.getPlotId() == null || plot.getPlotId().isBlank()) {
            return PlotInstanceState.BLUEPRINTING;
        }
        UUIDComponent uc = entityStore.getComponent(playerRef, UUIDComponent.getComponentType());
        if (uc == null) {
            return PlotInstanceState.BLUEPRINTING;
        }
        TownRecord town = tm.findTownForPlayerInWorld(uc.getUuid());
        if (town == null) {
            return PlotInstanceState.BLUEPRINTING;
        }
        try {
            PlotInstance pi = town.findPlotById(UUID.fromString(plot.getPlotId().trim()));
            return pi != null ? pi.getState() : PlotInstanceState.BLUEPRINTING;
        } catch (IllegalArgumentException e) {
            return PlotInstanceState.BLUEPRINTING;
        }
    }

    private record WorkplaceAssignRow(@Nonnull String label, @Nonnull UUID entityUuid) {}

    @Nonnull
    private static String findEntityUuidWithJobPlot(
        @Nonnull Store<EntityStore> store,
        @Nonnull UUID townId,
        @Nonnull UUID jobPlotId
    ) {
        final String[] holder = new String[] {""};
        Query<EntityStore> q = Query.and(TownVillagerBinding.getComponentType(), UUIDComponent.getComponentType());
        store.forEachChunk(
            q,
            (ArchetypeChunk<EntityStore> archetypeChunk, CommandBuffer<EntityStore> commandBuffer) -> {
                if (!holder[0].isEmpty()) {
                    return;
                }
                for (int i = 0; i < archetypeChunk.size(); i++) {
                    TownVillagerBinding b = archetypeChunk.getComponent(i, TownVillagerBinding.getComponentType());
                    if (b == null || !townId.equals(b.getTownId())) {
                        continue;
                    }
                    UUID jp = b.getJobPlotId();
                    if (jp == null || !jp.equals(jobPlotId)) {
                        continue;
                    }
                    UUIDComponent uc = archetypeChunk.getComponent(i, UUIDComponent.getComponentType());
                    if (uc != null) {
                        holder[0] = uc.getUuid().toString();
                        return;
                    }
                }
            }
        );
        return holder[0];
    }

    private static boolean isHomeResidentElsewhereOnHouse(
        @Nonnull TownRecord town,
        @Nonnull ConstructionCatalog catalog,
        @Nonnull UUID exceptPlotId,
        @Nonnull UUID villagerUuid
    ) {
        for (PlotInstance p : town.getPlotInstances()) {
            if (p.getPlotId().equals(exceptPlotId)) {
                continue;
            }
            if (!AetherhavenConstants.CONSTRUCTION_PLOT_HOUSE.equals(catalog.resolveGameplayConstructionId(p.getConstructionId()))) {
                continue;
            }
            UUID h = p.getHomeResidentEntityUuid();
            if (h != null && h.equals(villagerUuid)) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    private static List<WorkplaceAssignRow> collectWorkplaceAssignRows(
        @Nonnull Store<EntityStore> store,
        @Nonnull TownRecord town,
        @Nonnull AetherhavenPlugin plugin,
        @Nonnull String gameplayWorkplaceId
    ) {
        ComponentType<EntityStore, NPCEntity> npcType = NPCEntity.getComponentType();
        if (npcType == null) {
            return List.of();
        }
        UUID tid = town.getTownId();
        Map<UUID, WorkplaceAssignRow> byUuid = new LinkedHashMap<>();
        Query<EntityStore> q =
            Query.and(TownVillagerBinding.getComponentType(), UUIDComponent.getComponentType(), npcType);
        store.forEachChunk(
            q,
            (ArchetypeChunk<EntityStore> archetypeChunk, CommandBuffer<EntityStore> commandBuffer) -> {
                for (int i = 0; i < archetypeChunk.size(); i++) {
                    TownVillagerBinding b = archetypeChunk.getComponent(i, TownVillagerBinding.getComponentType());
                    if (b == null || !tid.equals(b.getTownId())) {
                        continue;
                    }
                    UUIDComponent uc = archetypeChunk.getComponent(i, UUIDComponent.getComponentType());
                    NPCEntity npc = archetypeChunk.getComponent(i, npcType);
                    if (uc == null || npc == null || npc.getRoleName() == null || npc.getRoleName().isBlank()) {
                        continue;
                    }
                    VillagerDefinition vdef = plugin.getVillagerDefinitionCatalog().byNpcRoleId(npc.getRoleName().trim());
                    if (vdef == null) {
                        continue;
                    }
                    String w = vdef.getWorkConstructionId();
                    if (w == null || !w.equals(gameplayWorkplaceId)) {
                        continue;
                    }
                    UUID u = uc.getUuid();
                    String label = NpcPortraitProvider.displayLabelForRoleId(npc.getRoleName());
                    byUuid.put(u, new WorkplaceAssignRow(label, u));
                }
            }
        );
        List<WorkplaceAssignRow> out = new ArrayList<>(byUuid.values());
        out.sort(Comparator.comparing(WorkplaceAssignRow::label, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    private record HouseResidentRow(@Nonnull String label, @Nonnull UUID entityUuid) {}

    @Nonnull
    private static List<HouseResidentRow> collectHouseResidentRows(
        @Nonnull Store<EntityStore> store,
        @Nonnull TownRecord town,
        @Nullable AetherhavenPlugin plugin,
        @Nonnull UUID currentHousePlotId,
        boolean hideElsewhereHoused
    ) {
        ComponentType<EntityStore, NPCEntity> npcType = NPCEntity.getComponentType();
        if (npcType == null) {
            return List.of();
        }
        UUID tid = town.getTownId();
        Map<UUID, HouseResidentRow> byUuid = new LinkedHashMap<>();
        Query<EntityStore> q =
            Query.and(TownVillagerBinding.getComponentType(), UUIDComponent.getComponentType(), npcType);
        store.forEachChunk(
            q,
            (ArchetypeChunk<EntityStore> archetypeChunk, CommandBuffer<EntityStore> commandBuffer) -> {
                for (int i = 0; i < archetypeChunk.size(); i++) {
                    TownVillagerBinding b = archetypeChunk.getComponent(i, TownVillagerBinding.getComponentType());
                    if (b == null || !tid.equals(b.getTownId()) || TownVillagerBinding.isVisitorKind(b.getKind())) {
                        continue;
                    }
                    UUIDComponent uc = archetypeChunk.getComponent(i, UUIDComponent.getComponentType());
                    NPCEntity npc = archetypeChunk.getComponent(i, npcType);
                    if (uc == null || npc == null || npc.getRoleName() == null) {
                        continue;
                    }
                    UUID u = uc.getUuid();
                    if (hideElsewhereHoused
                        && plugin != null
                        && isHomeResidentElsewhereOnHouse(town, plugin.getConstructionCatalog(), currentHousePlotId, u)) {
                        continue;
                    }
                    String label = NpcPortraitProvider.displayLabelForRoleId(npc.getRoleName());
                    byUuid.put(u, new HouseResidentRow(label, u));
                }
            }
        );
        addHouseFallbackIfMissing(
            byUuid,
            town.getElderEntityUuid(),
            AetherhavenConstants.ELDER_NPC_ROLE_ID,
            hideElsewhereHoused,
            plugin,
            town,
            currentHousePlotId
        );
        addHouseFallbackIfMissing(
            byUuid,
            town.getInnkeeperEntityUuid(),
            AetherhavenConstants.INNKEEPER_NPC_ROLE_ID,
            hideElsewhereHoused,
            plugin,
            town,
            currentHousePlotId
        );
        List<HouseResidentRow> out = new ArrayList<>(byUuid.values());
        out.sort(Comparator.comparing(HouseResidentRow::label, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    private static void addHouseFallbackIfMissing(
        @Nonnull Map<UUID, HouseResidentRow> byUuid,
        @Nullable UUID entityUuid,
        @Nonnull String roleId,
        boolean hideElsewhereHoused,
        @Nullable AetherhavenPlugin plugin,
        @Nonnull TownRecord town,
        @Nonnull UUID currentHousePlotId
    ) {
        if (entityUuid == null || byUuid.containsKey(entityUuid)) {
            return;
        }
        if (hideElsewhereHoused
            && plugin != null
            && isHomeResidentElsewhereOnHouse(town, plugin.getConstructionCatalog(), currentHousePlotId, entityUuid)) {
            return;
        }
        byUuid.put(
            entityUuid,
            new HouseResidentRow(NpcPortraitProvider.displayLabelForRoleId(roleId), entityUuid)
        );
    }

    public static final class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.builder(PageData.class, PageData::new)
            .append(new KeyedCodec<>("Action", Codec.STRING), (d, a) -> d.action = a, d -> d.action)
            .add()
            .append(new KeyedCodec<>("MemberUuid", Codec.STRING), (d, v) -> d.memberUuid = v, d -> d.memberUuid)
            .add()
            .append(new KeyedCodec<>("@InviteName", Codec.STRING), (d, v) -> d.inviteName = v, d -> d.inviteName)
            .add()
            .append(new KeyedCodec<>("@HouseResidentUuid", Codec.STRING), (d, v) -> d.houseResidentUuid = v, d -> d.houseResidentUuid)
            .add()
            .append(
                new KeyedCodec<>("@HouseResidentHideElsewhere", Codec.BOOLEAN),
                (d, v) -> d.houseResidentHideElsewhere = v,
                d -> d.houseResidentHideElsewhere
            )
            .add()
            .append(
                new KeyedCodec<>("@WorkplaceAssignUuid", Codec.STRING),
                (d, v) -> d.workplaceAssignUuid = v,
                d -> d.workplaceAssignUuid
            )
            .add()
            .append(new KeyedCodec<>("MaterialIndex", Codec.INTEGER), (d, v) -> d.materialIndex = v, d -> d.materialIndex)
            .add()
            .build();

        private String action;
        @Nullable
        private String memberUuid;
        @Nullable
        private String inviteName;
        private String houseResidentUuid;
        @Nullable
        private Boolean houseResidentHideElsewhere;
        @Nullable
        private String workplaceAssignUuid;
        @Nullable
        private Integer materialIndex;
    }
}
