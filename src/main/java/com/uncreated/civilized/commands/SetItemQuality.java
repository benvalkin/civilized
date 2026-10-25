package com.uncreated.civilized.commands;

import org.jetbrains.annotations.Nullable;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.uncreated.civilized.item.quality.ItemQuality;
import com.uncreated.civilized.neoforge.registration.attachments.MyDataComponents;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Sets the quality of the item in the player's main hand, e.g. {@code /setitemquality excellent}. For testing. */
public class SetItemQuality {

   private static final String NO_QUALITY = "none";

   private static final SimpleCommandExceptionType ERROR_EMPTY_HAND =
         new SimpleCommandExceptionType(Component.literal("Hold the item to change in your main hand."));

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      LiteralArgumentBuilder<CommandSourceStack> command =
            Commands.literal("setitemquality").requires(source -> source.hasPermission(2));

      for (ItemQuality quality : ItemQuality.values())
         command.then(
               Commands.literal(quality.getSerializedName())
                     .executes(context -> setQuality(context.getSource(), quality)));
      command.then(Commands.literal(NO_QUALITY).executes(context -> setQuality(context.getSource(), null)));

      dispatcher.register(command);
   }

   private static int setQuality(CommandSourceStack source, @Nullable ItemQuality quality)
         throws CommandSyntaxException {
      ItemStack held = source.getPlayerOrException().getMainHandItem();
      if (held.isEmpty())
         throw ERROR_EMPTY_HAND.create();

      if (quality == null)
         held.remove(MyDataComponents.ITEM_QUALITY);
      else
         held.set(MyDataComponents.ITEM_QUALITY, quality);

      String outcome = quality == null ? "no quality" : quality.getSerializedName() + " quality";
      source.sendSuccess(() -> Component.literal("The held item now has " + outcome + "."), false);
      return 1;
   }
}
