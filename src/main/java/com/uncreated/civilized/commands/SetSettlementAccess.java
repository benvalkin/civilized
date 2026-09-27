package com.uncreated.civilized.commands;

import java.util.Locale;
import java.util.Optional;

import javax.annotation.Nullable;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlement;
import com.uncreated.civilized.core.settlement.entity.LoadedSettlements;
import com.uncreated.civilized.core.settlement.permission.AccessLevel;
import com.uncreated.civilized.core.settlement.permission.ServerSettlementPermissionStore;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Dev command that sets a player's access level in the settlement they're standing in, e.g.
 * {@code /settlementaccess @s governor}. {@code none} takes their access away.
 */
public class SetSettlementAccess {

   private static final String ARGUMENT_PLAYER = "player";
   private static final String NO_ACCESS = "none";

   private static final DynamicCommandExceptionType ERROR_NOT_INSIDE_SETTLEMENT =
         new DynamicCommandExceptionType(
               player -> Component.literal(((ServerPlayer) player).getScoreboardName() + " is not inside a settlement."));

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      RequiredArgumentBuilder<CommandSourceStack, ?> playerArgument =
            Commands.argument(ARGUMENT_PLAYER, EntityArgument.player());

      // one literal per access level gives tab completion without any parsing
      for (AccessLevel accessLevel : AccessLevel.values())
         playerArgument.then(
               Commands.literal(accessLevel.name().toLowerCase(Locale.ROOT))
                     .executes(context -> setAccessLevel(context, accessLevel)));
      playerArgument.then(Commands.literal(NO_ACCESS).executes(context -> setAccessLevel(context, null)));

      LiteralArgumentBuilder<CommandSourceStack> command =
            Commands.literal("settlementaccess").requires(source -> source.hasPermission(2)).then(playerArgument);
      dispatcher.register(command);
   }

   private static int setAccessLevel(CommandContext<CommandSourceStack> context, @Nullable AccessLevel accessLevel)
         throws CommandSyntaxException {
      ServerPlayer player = EntityArgument.getPlayer(context, ARGUMENT_PLAYER);

      Optional<Settlement> settlement =
            LoadedSettlements.findEnclosing(player.blockPosition(), player.serverLevel())
                  .map(LoadedSettlement::getSettlement);
      if (settlement.isEmpty())
         throw ERROR_NOT_INSIDE_SETTLEMENT.create(player);

      ServerSettlementPermissionStore permissions = ServerSettlementPermissionStore.INSTANCE;
      if (accessLevel == null)
         permissions.removeAccess(settlement.get().getSettlementId(), player.getUUID());
      else
         permissions.setAccessLevel(settlement.get().getSettlementId(), player, accessLevel);

      String levelName = accessLevel == null ? NO_ACCESS : accessLevel.name().toLowerCase(Locale.ROOT);
      context.getSource()
            .sendSuccess(
                  () -> Component.literal("Set " + player.getScoreboardName() + "'s access in ")
                        .append(settlement.get().displayNameTranslation())
                        .append(" to " + levelName + "."),
                  true);
      return 1;
   }
}
