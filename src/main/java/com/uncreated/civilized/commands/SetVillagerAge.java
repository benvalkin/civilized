package com.uncreated.civilized.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.villagerinfo.LifeStage;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.behaviour.InvalidateImportantLocations;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public class SetVillagerAge {

   private static final String ARGUMENT_VILLAGER = "villager";
   private static final String ARGUMENT_AGE = "age_in_days";

   private static final SimpleCommandExceptionType ERROR_NOT_A_VILLAGER =
         new SimpleCommandExceptionType(Component.literal("That isn't a villager."));

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
            Commands.literal("setvillagerage")
                  .requires(source -> source.hasPermission(2))
                  .then(
                        Commands.argument(ARGUMENT_VILLAGER, EntityArgument.entity())
                              .then(
                                    Commands.argument(ARGUMENT_AGE, IntegerArgumentType.integer(0))
                                          .executes(
                                                context -> setAge(
                                                      context.getSource(),
                                                      EntityArgument.getEntity(context, ARGUMENT_VILLAGER),
                                                      IntegerArgumentType.getInteger(context, ARGUMENT_AGE))))));
   }

   private static int setAge(CommandSourceStack source, Entity entity, int ageInDays) throws CommandSyntaxException {

      if (!(entity instanceof CivilizedVillager villager))
         throw ERROR_NOT_A_VILLAGER.create();

      ServerLevel level = source.getLevel();
      VillagerInfo info = villager.getInfo();
      info.setBirthDay(level.getDayTime() / 24000 - ageInDays);

      boolean becameChild = ageInDays < VillagerInfo.GROW_UP_DAYS && !info.isChild();
      if (becameChild)
         info.setLifeStage(LifeStage.CHILD);

      ServerVillagerStore.INSTANCE.setDirty();
      ServerVillagerStore.INSTANCE.replicateChange(info, StoreOperation.UPDATE);

      InvalidateImportantLocations.revalidate(level, villager);
      villager.refreshBrain(level);

      String outcome;
      if (becameChild)
         outcome = "They're now a child.";
      else if (info.isOldEnoughToGrowUp(level.getDayTime()))
         outcome = "They'll grow up tonight.";
      else
         outcome = info.isChild() ? "They're still a child." : "They're still an adult.";

      source.sendSuccess(
            () -> Component.literal(info.getFullName() + " is now " + ageInDays + " days old. " + outcome),
            false);
      return 1;
   }
}
