package com.uncreated.civilized.commands;

import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.uncreated.civilized.core.StoreOperation;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.core.villagerinfo.skills.VillagerSkill;
import com.uncreated.civilized.core.villagerinfo.skills.VillagerSkills;
import com.uncreated.civilized.entity.CivilizedVillager;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

/**
 * Sets one (or all) of a villager's skills to the start of a level.
 */
public class SetVillagerSkill {

   private static final String ARGUMENT_VILLAGER = "villager";
   private static final String ARGUMENT_LEVEL = "level";
   private static final String ALL_SKILLS = "all";

   private static final SimpleCommandExceptionType ERROR_NOT_A_VILLAGER =
         new SimpleCommandExceptionType(Component.literal("That isn't a villager."));

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      RequiredArgumentBuilder<CommandSourceStack, ?> villagerArgument =
            Commands.argument(ARGUMENT_VILLAGER, EntityArgument.entity());

      for (VillagerSkill skill : VillagerSkill.values())
         villagerArgument.then(skillLiteral(skill.name().toLowerCase(), List.of(skill)));
      villagerArgument.then(skillLiteral(ALL_SKILLS, List.of(VillagerSkill.values())));

      dispatcher.register(
            Commands.literal("setvillagerskill").requires(source -> source.hasPermission(2)).then(villagerArgument));
   }

   private static LiteralArgumentBuilder<CommandSourceStack> skillLiteral(String name, List<VillagerSkill> skills) {
      return Commands.literal(name)
            .then(
                  Commands.argument(ARGUMENT_LEVEL, IntegerArgumentType.integer(0, VillagerSkills.MAX_LEVEL))
                        .executes(
                              context -> setLevel(
                                    context.getSource(),
                                    EntityArgument.getEntity(context, ARGUMENT_VILLAGER),
                                    skills,
                                    IntegerArgumentType.getInteger(context, ARGUMENT_LEVEL))));
   }

   private static int setLevel(CommandSourceStack source, Entity entity, List<VillagerSkill> skills, int level)
         throws CommandSyntaxException {
      if (!(entity instanceof CivilizedVillager villager))
         throw ERROR_NOT_A_VILLAGER.create();

      VillagerInfo info = villager.getInfo();
      for (VillagerSkill skill : skills)
         info.getSkills().setLevel(skill, level);

      ServerVillagerStore.INSTANCE.setDirty();
      ServerVillagerStore.INSTANCE.replicateChange(info, StoreOperation.UPDATE);

      String skillNames = skills.size() == 1 ? skills.getFirst().name().toLowerCase() : "every skill";
      source.sendSuccess(
            () -> Component.literal(info.getFullName() + "'s " + skillNames + " is now level " + level + "."),
            false);
      return 1;
   }
}
