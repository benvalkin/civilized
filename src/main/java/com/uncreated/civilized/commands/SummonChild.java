package com.uncreated.civilized.commands;

import java.util.Optional;

import javax.annotation.Nullable;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.util.BuildingUtil;
import com.uncreated.civilized.core.settlement.family.BirthOrganiser;
import com.uncreated.civilized.core.villagerinfo.Gender;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.entity.CivilizedVillager;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * Summons a child into the home the player is standing in. If a couple lives there, they become its parents, otherwise
 * it's an orphan. An age can be given to summon an older child.
 */
public class SummonChild {

   private static final String ARGUMENT_AGE = "age_in_days";

   private static final SimpleCommandExceptionType ERROR_NOT_PLAYER =
         new SimpleCommandExceptionType(Component.literal("This command cannot be called from console."));
   private static final SimpleCommandExceptionType ERROR_NOT_INSIDE_HOME =
         new SimpleCommandExceptionType(Component.literal("You must be inside a family home to use this command."));
   private static final SimpleCommandExceptionType ERROR_ENTITY_SPAWN_ERROR =
         new SimpleCommandExceptionType(Component.literal("Something went wrong while spawning the child."));

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
            Commands.literal("summonchild")
                  .requires(source -> source.hasPermission(2))
                  .executes(context -> summonChild(context.getSource(), 0))
                  .then(
                        Commands.argument(ARGUMENT_AGE, IntegerArgumentType.integer(0, VillagerInfo.GROW_UP_DAYS - 1))
                              .executes(
                                    context -> summonChild(
                                          context.getSource(),
                                          IntegerArgumentType.getInteger(context, ARGUMENT_AGE)))));
   }

   private static int summonChild(CommandSourceStack source, int ageInDays) throws CommandSyntaxException {

      ServerLevel level = source.getLevel();
      if (!source.isPlayer())
         throw ERROR_NOT_PLAYER.create();

      Optional<Building> home =
            ServerBuildingsStore.INSTANCE.findEnclosingBuilding(source.getPlayer().getOnPos(), level)
                  .filter(building -> building.getBuildingType().isFamilyResidence());
      if (home.isEmpty())
         throw ERROR_NOT_INSIDE_HOME.create();

      VillagerInfo mother = findMotherLivingWithPartner(home.get());
      VillagerInfo father = mother == null ? null : ServerVillagerStore.INSTANCE.get(mother.getPartnerId());

      long birthDay = level.getDayTime() / 24000 - ageInDays;
      Optional<CivilizedVillager> child = BirthOrganiser.spawnChild(level, home.get(), mother, father, birthDay);
      if (child.isEmpty())
         throw ERROR_ENTITY_SPAWN_ERROR.create();

      String parents =
            mother == null ? "an orphan" : "the child of " + mother.getFullName() + " and " + father.getFullName();
      source.sendSuccess(
            () -> Component.literal(
                  "Summoned " + child.get().getInfo().getFullName() + ", " + ageInDays + " days old, " + parents + "."),
            false);
      return 1;
   }

   private static @Nullable VillagerInfo findMotherLivingWithPartner(Building home) {
      for (VillagerInfo resident : BuildingUtil.getOccupants(home, ServerVillagerStore.INSTANCE)) {
         if (resident.getGender() != Gender.FEMALE || resident.isChild())
            continue;

         Optional<VillagerInfo> partner = ServerVillagerStore.INSTANCE.find(resident.getPartnerId());
         if (partner.isPresent() && partner.get().isOccupantOf(home))
            return resident;
      }
      return null;
   }
}
