package com.uncreated.civilized.core.building.placement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.core.settlement.ServerSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.settlement.SettlementBounds;
import com.uncreated.civilized.core.settlement.permission.ServerSettlementPermissionStore;
import com.uncreated.civilized.core.settlement.util.SettlementUtil;
import com.uncreated.civilized.item.BuildingDeedItem;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Checks whether a building can go where a player wants to put it, apart from its requirements, e.g. that it doesn't
 * overlap another building and is close enough to a settlement. Only the server does this, so these are checked both
 * before showing a building's requirements and again when the player confirms.
 */
public class ServerPlacementChecks {

   /** How far outside a settlement's bounds new buildings can still be placed, in blocks. */
   public static final int SETTLEMENT_EXTENSION_DISTANCE = 32;
   /** The widest a building can be along either horizontal axis, in blocks. */
   public static final int MAX_HORIZONTAL_SIZE = 64;
   /** The tallest a building can be, in blocks. */
   public static final int MAX_HEIGHT = 32;

   /**
    * Whether a new building of this type can be placed at these bounds.
    *
    * @param confirming
    *           whether the player is confirming the bounds, e.g. by right-clicking inside them, rather than having just
    *           finished dragging them out. Valid bounds are {@link PlacementResult.Confirmed} if so, and only
    *           {@link PlacementResult.Accepted} otherwise
    */
   public static PlacementResult checkEstablish(
         ServerPlayer player,
         BuildingType buildingType,
         BuildingBounds bounds,
         boolean confirming) {
      ServerLevel level = player.serverLevel();

      ItemStack deed = player.getMainHandItem();
      if (!(deed.getItem() instanceof BuildingDeedItem deedItem && deedItem.getBuildingType() == buildingType))
         return failure("message.settlement.create_building.failed.no_deed", buildingType.translation());

      Optional<PlacementResult.Failure> boundsFailure = checkBounds(level, buildingType, bounds, null);
      if (boundsFailure.isPresent())
         return boundsFailure.get();

      List<Settlement> dimensionSettlements = ServerSettlementsStore.INSTANCE.findInDimension(level.dimension());
      Optional<Settlement> nearbySettlement = findNearbySettlement(level, bounds);

      if (buildingType.is(BuildingTypes.TOWN_HALL)) {
         if (nearbySettlement.isPresent())
            return failure(
                  "message.settlement.create_building.failed.building_already_exists",
                  nearbySettlement.get().displayNameTranslation(),
                  buildingType.translation());

         if (ServerSettlementsStore.INSTANCE.findFromOwner(player.getUUID()).isPresent())
            return failure("message.settlement.create_building.failed.player_already_has_another_settlement");

         // the new settlement starts out around just its town hall
         SettlementBounds newSettlementBounds = Settlement.calculateBounds(bounds.getCenter(), List.of(bounds));
         Optional<PlacementResult.Failure> overlap =
               checkSettlementOverlap(newSettlementBounds, dimensionSettlements, null);
         if (overlap.isPresent())
            return overlap.get();

         return new PlacementResult.Confirmed(null, confirming);
      }

      if (nearbySettlement.isEmpty())
         return failure("message.settlement.create_building.failed.too_far_from_settlement");

      Settlement settlement = nearbySettlement.get();
      if (!ServerSettlementPermissionStore.INSTANCE.getOrCreate(settlement.getSettlementId())
            .hasCreateBuildingsPermission(player.getUUID()))
         return failure("message.settlement.create_building.failed.no_permission", settlement.displayNameTranslation());

      List<BuildingBounds> settlementBuildings = new ArrayList<>();
      ServerBuildingsStore.INSTANCE.findForSettlement(settlement.getSettlementId())
            .forEach(building -> settlementBuildings.add(building.getBounds()));
      settlementBuildings.add(bounds);

      SettlementBounds newSettlementBounds =
            Settlement.calculateBounds(settlement.getBounds().getOrigin(), settlementBuildings);
      Optional<PlacementResult.Failure> overlap =
            checkSettlementOverlap(newSettlementBounds, dimensionSettlements, settlement);
      if (overlap.isPresent())
         return overlap.get();

      return new PlacementResult.Confirmed(settlement, confirming);
   }

   /**
    * Whether an existing building can be redrawn to these bounds. Like a new building, it can't overlap other buildings
    * or settlements, and it has to stay within reach of its own settlement.
    *
    * @param confirming
    *           as for {@link #checkEstablish}
    */
   public static PlacementResult checkRedraw(
         ServerPlayer player,
         Building building,
         BuildingBounds bounds,
         boolean confirming) {
      ServerLevel level = player.serverLevel();

      Optional<Settlement> found = ServerSettlementsStore.INSTANCE.find(building.getSettlementId());
      // the bounds are dragged out in the player's level, so it has to be the one the building is in
      if (found.isEmpty() || !building.getDimension().equals(level.dimension()))
         return failure("message.building.placement.validation.invalid_bounds");

      Settlement settlement = found.get();
      if (!ServerSettlementPermissionStore.INSTANCE.getOrCreate(settlement.getSettlementId())
            .hasCreateBuildingsPermission(player.getUUID()))
         return failure("message.building.redraw.failed.no_permission", settlement.displayNameTranslation());

      Optional<PlacementResult.Failure> boundsFailure = checkBounds(level, building.getBuildingType(), bounds, building);
      if (boundsFailure.isPresent())
         return boundsFailure.get();

      // the sign is how players get to the building's menu, so it can't be left outside the building
      if (building.getPrimarySignPos() != null && !bounds.contains(building.getPrimarySignPos()))
         return failure("message.building.redraw.failed.sign_outside");

      List<Settlement> dimensionSettlements = ServerSettlementsStore.INSTANCE.findInDimension(level.dimension());
      Optional<Settlement> nearbySettlement = findNearbySettlement(level, bounds);
      if (nearbySettlement.isEmpty())
         return failure("message.settlement.create_building.failed.too_far_from_settlement");
      if (nearbySettlement.get() != settlement)
         return failure(
               "message.settlement.create_building.failed.too_close_to_other_settlement",
               nearbySettlement.get().displayNameTranslation());

      // the settlement's bounds as they'd be with the building's new bounds instead of its old ones
      List<BuildingBounds> settlementBuildings = new ArrayList<>();
      ServerBuildingsStore.INSTANCE.findForSettlement(settlement.getSettlementId())
            .stream()
            .filter(other -> other != building)
            .forEach(other -> settlementBuildings.add(other.getBounds()));
      settlementBuildings.add(bounds);

      SettlementBounds newSettlementBounds =
            Settlement.calculateBounds(settlement.getBounds().getOrigin(), settlementBuildings);
      Optional<PlacementResult.Failure> overlap =
            checkSettlementOverlap(newSettlementBounds, dimensionSettlements, settlement);
      if (overlap.isPresent())
         return overlap.get();

      return new PlacementResult.Confirmed(settlement, confirming);
   }

   /** The settlement close enough to these bounds for a new building there to join it. */
   public static Optional<Settlement> findNearbySettlement(ServerLevel level, BuildingBounds bounds) {
      return SettlementUtil.findExtendedEncapsulating(
            ServerSettlementsStore.INSTANCE.findInDimension(level.dimension()),
            bounds.getEncapsulatingAABB(),
            SETTLEMENT_EXTENSION_DISTANCE);
   }

   /**
    * Checks that the proposed bounds themselves are a sensible size, don't overlap other buildings, and more.
    *
    * @param existing
    *           if these bounds are for an existing building and are being redrawn, otherwise null for new building
    */
   static Optional<PlacementResult.Failure> checkBounds(
         ServerLevel level,
         BuildingType buildingType,
         BuildingBounds bounds,
         @Nullable Building existing) {

      BlockPos lower = bounds.getLowerCorner();
      BlockPos upper = bounds.getUpperCorner();
      boolean ordered = lower.getX() <= upper.getX() && lower.getY() <= upper.getY() && lower.getZ() <= upper.getZ();
      // bounds come from clients, so they're checked to be what the drag tool would make before anything else
      if (!ordered || !bounds.contains(bounds.getCenter()) || !level.hasChunksAt(lower, upper))
         return Optional.of(failure("message.building.placement.validation.invalid_bounds"));

      int sizeX = upper.getX() - lower.getX() + 1;
      int sizeZ = upper.getZ() - lower.getZ() + 1;
      int height = upper.getY() - lower.getY() + 1;
      if (sizeX > MAX_HORIZONTAL_SIZE || sizeZ > MAX_HORIZONTAL_SIZE || height > MAX_HEIGHT)
         return Optional.of(
               failure("message.building.placement.validation.too_large", MAX_HORIZONTAL_SIZE, MAX_HORIZONTAL_SIZE));

      Optional<Building> overlapping =
            ServerBuildingsStore.INSTANCE.findOverlappingBuilding(bounds, level).filter(other -> other != existing);
      if (overlapping.isPresent())
         return Optional.of(
               failure(
                     "message.building.placement.validation.building_overlapping",
                     overlapping.get().getBuildingType().translation()));

      // worksites are open areas like farms, so they don't need a clear, roofed center like other buildings
      if (!buildingType.isWorksite()) {
         BlockPos center = bounds.getCenter();
         if (!level.getBlockState(center).isAir())
            return Optional
                  .of(failure("message.building.placement.validation.center_obstructed", center.toShortString()));

         if (!buildingType.is(BuildingTypes.TOWN_SQUARE) && level.canSeeSky(center))
            return Optional.of(failure("message.building.placement.validation.center_no_roof", center.toShortString()));
      }

      return Optional.empty();
   }

   /**
    * Checks if the proposed new settlement bounds are overlapping someone else's settlement (i.e. settlements that are
    * not {@code ownSettlement}). If so, you shouldn't be able allowed to place a building there.
    */
   private static Optional<PlacementResult.Failure> checkSettlementOverlap(
         SettlementBounds bounds,
         List<Settlement> settlements,
         @Nullable Settlement ownSettlement) {

      for (Settlement other : settlements) {
         boolean overlappingSomeoneElseSettlement = other != ownSettlement && other.getBounds().isOverlapping(bounds);
         boolean placerHasNoSettlementYet = ownSettlement == null;
         // we might be trying to increase the settlement bounds of a settlement that belongs to someone else where we
         // have permission
         boolean notPartOfSomeoneElseSettlement =
               other != null && ownSettlement != null && !other.getBounds().isOverlapping(ownSettlement.getBounds());
         if (overlappingSomeoneElseSettlement && (placerHasNoSettlementYet || notPartOfSomeoneElseSettlement))
            return Optional.of(
                  failure(
                        "message.settlement.create_building.failed.too_close_to_other_settlement",
                        other.displayNameTranslation()));
      }

      return Optional.empty();
   }

   private static PlacementResult.Failure failure(String translationKey, Object... args) {
      return new PlacementResult.Failure(
            Component.translatable(translationKey, args).withColor(Colors.VALIDATION_ERROR));
   }
}
