package com.uncreated.civilized.core.building.entity.behaviour;

import com.uncreated.civilized.core.building.entity.LoadedBuilding;
import com.uncreated.civilized.core.building.production.RecipeProductionMachine;
import com.uncreated.civilized.core.building.production.RecipeProductionSystem;
import com.uncreated.civilized.core.building.production.bills.ProductionType;
import com.uncreated.civilized.core.building.state.artisan.ArtisanHouseState;

import lombok.Getter;
import net.minecraft.server.level.ServerLevel;

public class ArtisanHouseBehaviour extends BuildingBehaviour {

   @Getter
   private RecipeProductionSystem recipeProductionSystem; // null on client

   public ArtisanHouseBehaviour(LoadedBuilding entity) {
      super(entity);

      if (!entity.getLevel().isClientSide()) {
         recipeProductionSystem = new RecipeProductionSystem();
      }
   }

   protected void registerProductionMachines(RecipeProductionSystem recipeProductionSystem) {
      for (ProductionType productionType : getBuilding().getBuildingType().supportedProductionTypes()) {
         RecipeProductionMachine<?> machine = productionType.createRecipeProductionMachine().get();
         recipeProductionSystem.registerMachine(machine.getClass(), machine);
      }
   }

   @Override
   public void start() {

      registerProductionMachines(recipeProductionSystem);

      refreshProductionOrders();
   }

   private void refreshProductionOrders() {
      ArtisanHouseState artisanHouseState = (ArtisanHouseState) getBuilding().getState();
      for (RecipeProductionMachine<?> machine : recipeProductionSystem.registeredMachines()) {
         artisanHouseState.createProductionOrders(machine, (ServerLevel) getEntity().getLevel());
      }
   }

   private static final long PRODUCTION_BILL_REFRESH_INTERVAL = 5 * 20;
   private long refreshedTime = 0;

   @Override
   public void serverTick(ServerLevel level, long gameTime, long dayTime) {
      if (gameTime >= refreshedTime) {
         refreshedTime = gameTime + PRODUCTION_BILL_REFRESH_INTERVAL;
         refreshProductionOrders();
      }
   }
}
