package com.uncreated.civilized.commands;

import java.util.Optional;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerNpcRole;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.neoforge.registration.entity.EntityRegistry;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;

public class SummonResident {

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
            (LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) ((LiteralArgumentBuilder) Commands
                  .literal("summonresident")
                  .executes((stack) -> spawnResidentVillager(stack.getSource()))
                  .requires((stack) -> stack.hasPermission(2))))));
   }

   private static final SimpleCommandExceptionType ERROR_NOT_PLAYER =
         new SimpleCommandExceptionType(Component.literal("This command cannot be called from console."));
   private static final SimpleCommandExceptionType ERROR_NOT_INSIDE_BUILDING =
         new SimpleCommandExceptionType(Component.literal("You must be inside a building to use this command."));
   private static final SimpleCommandExceptionType ERROR_ENTITY_SPAWN_ERROR =
         new SimpleCommandExceptionType(Component.literal("Something went wrong while spawning the villager."));

   private static int spawnResidentVillager(CommandSourceStack source) throws CommandSyntaxException {

      ServerLevel level = source.getLevel();
      if (!source.isPlayer())
         throw ERROR_NOT_PLAYER.create();

      Optional<Building> building =
            ServerBuildingsStore.INSTANCE.findEnclosingBuilding(source.getPlayer().getOnPos(), level);
      if (building.isEmpty())
         throw ERROR_NOT_INSIDE_BUILDING.create();

      BlockPos insidePos = building.get().getBounds().findRandomInsideFloorBlock(level);

      CivilizedVillager villager =
            EntityRegistry.CIVILIZED_VILLAGER.get().spawn(level, insidePos, EntitySpawnReason.EVENT);

      if (villager == null) {
         throw ERROR_ENTITY_SPAWN_ERROR.create();
      }

      villager.getInfo().getNpcRoles().add(VillagerNpcRole.WORKER);
      villager.getInfo().setOccupation(building.get().getBuildingType().occupation());
      villager.getInfo().setSettlementId(building.get().getSettlementId());
      villager.getInfo().setHomeBuildingId(building.get().getBuildingId());
      building.get().getOccupantIds().add(villager.getInfo().getVillagerId());
      ServerVillagerStore.INSTANCE.setDirty();
      ServerVillagerStore.INSTANCE.replicateChange(villager.getInfo(), StoreOperation.ADD_OR_OVERWRITE);
      ServerBuildingsStore.INSTANCE.setDirty();
      ServerBuildingsStore.INSTANCE.replicateChange(building.get(), StoreOperation.UPDATE);
      return 1;
   }
}
