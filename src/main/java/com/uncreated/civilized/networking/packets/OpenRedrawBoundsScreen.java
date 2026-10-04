package com.uncreated.civilized.networking.packets;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.List;
import java.util.UUID;

import com.uncreated.civilized.core.building.bounds.BuildingBounds;
import com.uncreated.civilized.core.building.requirement.RequirementResultData;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenRedrawBoundsScreen(UUID buildingId, BuildingBounds bounds, List<RequirementResultData> requirements)
      implements CustomPacketPayload {

   public static final CustomPacketPayload.Type<OpenRedrawBoundsScreen> TYPE =
         new CustomPacketPayload.Type<>(
               ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "open_redraw_bounds_screen"));

   public static final StreamCodec<RegistryFriendlyByteBuf, OpenRedrawBoundsScreen> STREAM_CODEC =
         StreamCodec.composite(
               UUIDUtil.STREAM_CODEC,
               OpenRedrawBoundsScreen::buildingId,
               StreamCodec.<FriendlyByteBuf, BuildingBounds>of((buffer, bounds) -> bounds.encode(buffer), BuildingBounds::decode),
               OpenRedrawBoundsScreen::bounds,
               RequirementResultData.STREAM_CODEC.apply(ByteBufCodecs.list()),
               OpenRedrawBoundsScreen::requirements,
               OpenRedrawBoundsScreen::new);

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
