package com.uncreated.civilized.entity.behaviour;

import java.util.List;
import java.util.Set;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.logistics.AggregateItemStack;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.util.ContainerHelper;

import lombok.extern.log4j.Log4j2;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.Tags;

@Log4j2
public class EatFood extends StatefulBehaviour {

   private AggregateItemStack food;

   private List<Building> viableFoodBuildings;

   public EatFood(BehaviourState state) {
      super(state);
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel level, CivilizedVillager villager) {

      if (!villager.getHunger().isHungry())
         return false;

      food = ContainerHelper.countItems(villager.getLogisticsInventory(), EatFood::isViableFood);

      if (!food.hasItems()) {
         Set<Building> buildings =
               ServerBuildingsStore.INSTANCE.findForSettlement(villager.getInfo().getSettlementId());
         viableFoodBuildings =
               buildings.stream()
                     .filter(
                           b -> b.getBuildingType().isFoodVendor()
                                 || b.getBuildingId().equals(villager.getInfo().getHomeBuildingId())
                                 || b.getBuildingType().is(BuildingTypes.STOREHOUSE))
                     .sorted((b1, b2) -> {
                        // choose vendors over home
                        if (b1.getBuildingType().isFoodVendor() && b2.getBuildingType().isFoodVendor())
                           return 0;

                        if (b1.getBuildingType().isFoodVendor())
                           return -1;

                        return 1;
                     })
                     .toList();

         if (viableFoodBuildings.isEmpty())
            return false; // can't fetch food if there are no food vendor buildings

         // TODO: go pick up food into the villager's inventory.
         // * create an ItemStockRequirement for 1 food item
         // * Create a TakeToInventory instruction with the requirement
         // * start TakeItemsToInventory behaviour if possible
         // note: we don't currently have a way to select more satiating foods over others. A "preference" system will
         // probably be the next thing we work on.
         return false;
      }

      return true;
   }

   private static boolean isViableFood(ItemStack i) {
      return i.is(Tags.Items.FOODS) && !i.is(Tags.Items.FOODS_RAW_FISH) && !i.is(Tags.Items.FOODS_RAW_MEAT);
   }

   protected void tick(ServerLevel level, CivilizedVillager villager, long gameTime) {
      // TODO: eat the food item:
      // * choose the best (most satiating food) from the villager's logistics inventory
      // * play eating sound + particle effects for a short time (same time as vanilla minecraft)
      // * consume the item
      // * if no longer hungry, stop the activity (it can still restart if the villager is still hungry)
   }
}
