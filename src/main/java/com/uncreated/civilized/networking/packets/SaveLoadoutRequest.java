package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.Optional;

import com.uncreated.civilized.core.settlement.ServerSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.settlement.loadouts.Loadout;
import com.uncreated.civilized.core.settlement.loadouts.ServerLoadoutsStore;
import com.uncreated.civilized.core.settlement.permission.ServerSettlementPermissionStore;
import com.uncreated.civilized.core.settlement.permission.SettlementPermissions;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SaveLoadoutRequest(Loadout loadout) implements CustomPacketPayload {

   public static final Type<SaveLoadoutRequest> TYPE =
         new Type<>(ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "save_loadout"));

   public static final StreamCodec<RegistryFriendlyByteBuf, SaveLoadoutRequest> STREAM_CODEC =
         StreamCodec.ofMember(SaveLoadoutRequest::encode, SaveLoadoutRequest::decode);

   public static SaveLoadoutRequest decode(RegistryFriendlyByteBuf buffer) {
      return new SaveLoadoutRequest(Loadout.STREAM_CODEC.decode(buffer));
   }

   public void encode(RegistryFriendlyByteBuf buffer) {
      Loadout.STREAM_CODEC.encode(buffer, loadout);
   }

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void serverSaveLoadoutRequest(SaveLoadoutRequest packet, IPayloadContext context) {

      Loadout sanitized = packet.loadout().sanitized();

      if (sanitized.displayName().isBlank()) {
         failure(sanitized, context, "Loadout could not be saved - loadout name cannot be blank.");
         return;
      }

      Optional<Settlement> settlement = ServerSettlementsStore.INSTANCE.find(sanitized.settlementId());
      if (settlement.isEmpty()) {
         failure(sanitized, context, "Loadout could not be saved - specified settlement does not exist.");
         return;
      }

      SettlementPermissions permissions =
            ServerSettlementPermissionStore.INSTANCE.getOrCreate(settlement.get().getSettlementId());
      if (!permissions.hasEditBuildingPermission(context.player().getUUID())) {
         failure(
               sanitized,
               context,
               "Loadout could not be saved - you do not have permission to create or edit loadouts in this settlement.");
         return;
      }

      Optional<Loadout> existing = ServerLoadoutsStore.INSTANCE.find(packet.loadout().loadoutId());

      if (existing.isEmpty()) {
         ServerLoadoutsStore.INSTANCE.add(sanitized);
         success(sanitized, context);
      } else {
         existing.get().secureCopyFrom(sanitized);
         ServerLoadoutsStore.INSTANCE.find(packet.loadout().loadoutId());
         ServerLoadoutsStore.INSTANCE.setDirty();
         success(existing.get(), context);
      }
   }

   private static void success(Loadout loadout, IPayloadContext context) {
      context.reply(new LoadoutSavedResult(loadout, true));
   }

   private static void failure(Loadout loadout, IPayloadContext context, String errorMessage) {
      context.reply(new LoadoutSavedResult(loadout, false));
      context.player().displayClientMessage(Component.literal(errorMessage).withColor(Colors.VALIDATION_ERROR), false);
   }
}
