package com.uncreated.civilized.commands;

import java.util.Optional;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.util.BuildingUtil;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerNpcRoles;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.InvalidateImportantLocations;
import com.uncreated.civilized.neoforge.registration.entity.EntityRegistry;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;

public class SummonWorker {

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
            Commands.literal("summonworker")
                  .requires(source -> source.hasPermission(2))
                  .executes(context -> summonWorker(context.getSource())));
   }

   private static final SimpleCommandExceptionType ERROR_NOT_PLAYER =
         new SimpleCommandExceptionType(Component.literal("This command cannot be called from console."));
   private static final SimpleCommandExceptionType ERROR_NOT_INSIDE_WORKSITE =
         new SimpleCommandExceptionType(Component.literal("You must be inside a worksite to use this command."));
   private static final SimpleCommandExceptionType ERROR_WORKSITE_FULL =
         new SimpleCommandExceptionType(Component.literal("This worksite has no free worker slots."));
   private static final SimpleCommandExceptionType ERROR_ENTITY_SPAWN_ERROR =
         new SimpleCommandExceptionType(Component.literal("Something went wrong while spawning the villager."));

   private static int summonWorker(CommandSourceStack source) throws CommandSyntaxException {

      ServerLevel level = source.getLevel();
      if (!source.isPlayer())
         throw ERROR_NOT_PLAYER.create();

      Optional<Building> worksite =
            ServerBuildingsStore.INSTANCE.findEnclosingBuilding(source.getPlayer().getOnPos(), level)
                  .filter(building -> building.getBuildingType().isWorkBuilding());
      if (worksite.isEmpty())
         throw ERROR_NOT_INSIDE_WORKSITE.create();

      if (!BuildingUtil.worksiteHasFreeWorkerSlot(worksite.get(), ServerVillagerStore.INSTANCE))
         throw ERROR_WORKSITE_FULL.create();

      BlockPos insidePos = worksite.get().getBounds().findRandomInsideFloorBlock(level);

      CivilizedVillager villager =
            EntityRegistry.CIVILIZED_VILLAGER.get().spawn(level, insidePos, EntitySpawnReason.EVENT);

      if (villager == null) {
         throw ERROR_ENTITY_SPAWN_ERROR.create();
      }

      villager.changeNpcRole(VillagerNpcRoles.WORKER);
      villager.getInfo().setSettlementId(worksite.get().getSettlementId());
      villager.getInfo().setWorkBuildingId(worksite.get().getBuildingId());
      ServerVillagerStore.INSTANCE.setDirty();
      ServerVillagerStore.INSTANCE.replicateChange(villager.getInfo(), StoreOperation.ADD_OR_OVERWRITE);

      // works out their occupation from the worksite and finds them a home
      InvalidateImportantLocations.revalidate(level, villager);
      villager.refreshBrain(level);

      source.sendSuccess(
            () -> Component.literal(
                  "Summoned " + villager.getInfo().getFullName() + " to work at the "
                        + worksite.get().getBuildingType().name() + "."),
            false);
      return 1;
   }
}
