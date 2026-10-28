package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.uncreated.civilized.core.dialogue.actions.server.IServerDialogueAction;
import com.uncreated.civilized.core.dialogue.actions.server.ServerDialogueActions;
import com.uncreated.civilized.ui.menu.villager.VillagerMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RunDialogueAction(ResourceLocation actionId) implements CustomPacketPayload {

   private static final Logger LOGGER = LogUtils.getLogger();

   public static final Type<RunDialogueAction> TYPE =
         new Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "run_dialogue_action"));

   public static final StreamCodec<RegistryFriendlyByteBuf, RunDialogueAction> STREAM_CODEC =
         StreamCodec.composite(ResourceLocation.STREAM_CODEC, RunDialogueAction::actionId, RunDialogueAction::new);

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void serverReceiveRunDialogueAction(RunDialogueAction packet, IPayloadContext context) {
      // the villager comes from the menu the player has open, rather than trusting the client to say who it is
      if (!(context.player() instanceof ServerPlayer player)
            || !(player.containerMenu instanceof VillagerMenu menu)
            || !menu.stillValid(player))
         return;

      IServerDialogueAction action = ServerDialogueActions.get(packet.actionId);
      if (action == null) {
         LOGGER.warn("{} asked to run unknown dialogue action {}", player.getName().getString(), packet.actionId);
         return;
      }

      action.run(player, menu.getVillager());
   }
}
