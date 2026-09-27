package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.UUID;

import com.uncreated.civilized.entity.CivilizedVillager;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Tells the server that a player opened or closed a villager's dialogue screen, so that the villager stops to talk to
 * them, or carries on with what it was doing.
 */
public record VillagerDialogueScreenToggled(UUID entityId, boolean showDialogueScreen)
      implements CustomPacketPayload {

   public static final Type<VillagerDialogueScreenToggled> TYPE =
         new Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "villager_dialogue_screen_toggled"));

   public static final StreamCodec<ByteBuf, VillagerDialogueScreenToggled> STREAM_CODEC =
         StreamCodec.composite(
               UUIDUtil.STREAM_CODEC,
               VillagerDialogueScreenToggled::entityId,
               ByteBufCodecs.BOOL,
               VillagerDialogueScreenToggled::showDialogueScreen,
               VillagerDialogueScreenToggled::new);

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void serverReceiveVillagerDialogueScreenToggled(
         VillagerDialogueScreenToggled packet,
         IPayloadContext context) {

      if (!(context.player().level() instanceof ServerLevel serverLevel))
         return;

      Entity entity = serverLevel.getEntity(packet.entityId());
      if (!(entity instanceof CivilizedVillager villager))
         return;

      if (packet.showDialogueScreen)
         villager.goSpeakToPlayer(context.player());
      else
         villager.stopSpeakingToPlayer();
   }
}
