package com.uncreated.civilized.core.building.requirement;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.core.building.requirement.registry.BuildingRequirements;
import com.uncreated.civilized.core.settlement.ServerSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.item.CurrencyItem;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Checks building requirements on the server, both to make sure requirements are properly met, and also to show the
 * client the results.
 */
public class ServerRequirementChecks {

   /**
    * Checks if a new building can be established. New buildings are evaluated with the requirements belonging to the
    * first upgrade level ({@code Building.FIRST_UPGRADE_LEVEL}).
    *
    * @param settlement
    *           the settlement it would join, as found by its placement check, or null if this is a new town hall.
    */
   public static List<IBuildingRequirementResult> checkEstablish(
         ServerPlayer player,
         BuildingType buildingType,
         BuildingBounds bounds,
         @Nullable Settlement settlement) {

      RequirementContext context = ServerRequirementContexts.create(player.serverLevel(), bounds, settlement, player);
      return BuildingRequirements.getBuildingRequirements(buildingType, Building.FIRST_UPGRADE_LEVEL).evaluate(context);
   }

   /**
    * Checks if a new building can be upgraded to its next upgrade level, if one exists. Returns empty if it's already
    * at its highest level.
    */
   public static Optional<List<IBuildingRequirementResult>> checkUpgradeToNextLevel(
         ServerPlayer player,
         Building building) {
      return BuildingRequirements.find(building.getBuildingType(), building.getUpgradeLevel() + 1)
            .map(requirements -> requirements.evaluate(contextFor(player, building, building.getBounds())));
   }

   /**
    * Checks if a building's redrawn new bounds are valid according the building's structural requirements.
    * Prerequisites and costs aren't checked here, as they were already checked when creating/upgrading the building.
    */
   public static List<IBuildingRequirementResult> checkRedrawnBounds(
         ServerPlayer player,
         Building building,
         BuildingBounds newBounds) {

      return BuildingRequirements.getBuildingRequirements(building.getBuildingType(), building.getUpgradeLevel())
            .evaluate(contextFor(player, building, newBounds), EnumSet.of(RequirementKind.STRUCTURAL));
   }

   /**
    * Pays the {@link RequirementKind#COST} requirements for a building to reach a level (typically by subtracting coins
    * from the player's inventory and certain building chests). Only call this once its requirements have been checked
    * and met.
    */
   public static void payCosts(
         BuildingType buildingType,
         int upgradeLevel,
         Settlement settlement,
         ServerPlayer player) {
      for (IBuildingRequirement requirement : BuildingRequirements
            .getBuildingRequirements(buildingType, upgradeLevel)) {
         if (requirement instanceof CurrencyRequirement currency)
            CurrencyItem
                  .debit(ServerRequirementContexts.coinStorage(settlement, player), currency.getRequiredCurrency());
      }
   }

   public static boolean allSatisfied(List<IBuildingRequirementResult> results) {
      return results.stream().allMatch(IBuildingRequirementResult::isSatisfied);
   }

   private static RequirementContext contextFor(ServerPlayer player, Building building, BuildingBounds bounds) {
      // the building may be in another dimension than the player, e.g. if they're looking at its menu remotely
      ServerLevel level = player.server.getLevel(building.getDimension());
      if (level == null)
         level = player.serverLevel();

      Settlement settlement = ServerSettlementsStore.INSTANCE.find(building.getSettlementId()).orElse(null);
      return ServerRequirementContexts.create(level, bounds, settlement, player);
   }
}
