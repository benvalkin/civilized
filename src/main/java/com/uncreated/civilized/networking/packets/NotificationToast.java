package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import com.uncreated.civilized.core.notifications.Notification;
import com.uncreated.civilized.core.notifications.Severity;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

/** Shows a notification to a player as a toast in the corner of their screen. */
public record NotificationToast(Component headline, Component detail, Severity severity,
      ItemStack icon) implements CustomPacketPayload {

   public static final Type<NotificationToast> TYPE =
         new Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "notification_toast"));

   public static final StreamCodec<RegistryFriendlyByteBuf, NotificationToast> STREAM_CODEC =
         StreamCodec.composite(
               ComponentSerialization.TRUSTED_STREAM_CODEC,
               NotificationToast::headline,
               ComponentSerialization.TRUSTED_STREAM_CODEC,
               NotificationToast::detail,
               NeoForgeStreamCodecs.enumCodec(Severity.class),
               NotificationToast::severity,
               // optional icon - not every notification needs an icon
               ItemStack.OPTIONAL_STREAM_CODEC,
               NotificationToast::icon,
               NotificationToast::new);

   public static NotificationToast of(Notification notification) {
      return new NotificationToast(
            notification.headline(),
            notification.detail(),
            notification.severity(),
            notification.icon());
   }

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
