package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.List;
import java.util.UUID;

import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.requirement.RequirementResultData;
import com.uncreated.civilized.core.building.requirement.ServerRequirementChecks;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Asks the server to check the requirements for a building's next level, which it answers with
 * {@link RequirementsChecked}. Used by the upgrade tab, which must already be open on the client while the requirements
 * are checked.
 *
 * @param requestId
 *           sent back with the answer, so that the tab can ignore answers to requests it's since replaced
 */
public record CheckUpgradeRequirements(int requestId, UUID buildingId) implements CustomPacketPayload {

   public static final CustomPacketPayload.Type<CheckUpgradeRequirements> TYPE =
         new CustomPacketPayload.Type<>(
               ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "check_upgrade_requirements"));

   public static final StreamCodec<FriendlyByteBuf, CheckUpgradeRequirements> STREAM_CODEC =
         StreamCodec.ofMember(CheckUpgradeRequirements::encode, CheckUpgradeRequirements::decode);

   private static int lastRequestId = 0;

   /** A new id for a request, so that its answer can be told apart from answers to earlier ones. */
   public static int nextRequestId() {
      return ++lastRequestId;
   }

   public static CheckUpgradeRequirements decode(FriendlyByteBuf buffer) {
      return new CheckUpgradeRequirements(buffer.readVarInt(), buffer.readUUID());
   }

   public void encode(FriendlyByteBuf buffer) {
      buffer.writeVarInt(requestId);
      buffer.writeUUID(buildingId);
   }

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void serverReceiveCheckUpgradeRequirements(CheckUpgradeRequirements packet, IPayloadContext context) {
      ServerPlayer player = (ServerPlayer) context.player();

      // unknown buildings, and buildings already at their highest level, are answered with no results
      List<RequirementResultData> results =
            ServerBuildingsStore.INSTANCE.find(packet.buildingId())
                  .flatMap(building -> ServerRequirementChecks.checkUpgradeToNextLevel(player, building))
                  .orElse(List.of())
                  .stream()
                  .map(RequirementResultData::from)
                  .toList();
      PacketDistributor.sendToPlayer(player, new RequirementsChecked(packet.requestId(), results));
   }
}
