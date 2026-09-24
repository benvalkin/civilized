package com.uncreated.civilized.mixins;

import static com.uncreated.civilized.neoforge.registration.attachments.DataAttachments.LinkedBuilding.FIELD_BUILDING_IS_BUILDING;

import java.util.Optional;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ClientBuildingStore;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.building.signs.SignHelper;
import com.uncreated.civilized.core.settlement.ClientSettlementsStore;
import com.uncreated.civilized.core.villagerinfo.ClientVillagerStore;
import com.uncreated.civilized.neoforge.registration.attachments.DataAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityMixin extends BlockEntity {

   @Shadow
   private SignText frontText;

   public SignBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
      super(type, pos, blockState);
   }

   @Inject(method = "getUpdateTag", at = @At("TAIL"))
   protected void linkBuildingBeforeSendingClientPacket(CallbackInfoReturnable<CompoundTag> cir) {
      if (level == null || level.isClientSide)
         return;

      // on server create update packet
      Optional<Building> building = tryLinkWithEnclosingBuilding((ServerLevel) level);
      if (building.isEmpty())
         return;

      // add extra packet fields to send to client
      CompoundTag tag = cir.getReturnValue();
      tag.putUUID(FIELD_BUILDING_IS_BUILDING, building.get().getBuildingId());
   }

   @Inject(method = "loadAdditional", at = @At("TAIL"))
   protected void onClientReadUpdateTag(CompoundTag tag, HolderLookup.Provider provider, CallbackInfo ci) {

      if (level == null || !level.isClientSide)
         return;

      // on client reading update packet from server
      if (tag.hasUUID(FIELD_BUILDING_IS_BUILDING)) {
         UUID buildingId = tag.getUUID(FIELD_BUILDING_IS_BUILDING);
         setData(DataAttachments.LINKED_BUILDING, new DataAttachments.LinkedBuilding());
         civilizations$setClientText(buildingId);
      } else if (getData(DataAttachments.LINKED_BUILDING).getBuildingId() != null) {
         // if sign previously had a linked building but no longer does, unlink it/clear its text
         frontText = new SignText();
         setData(DataAttachments.LINKED_BUILDING, new DataAttachments.LinkedBuilding());
      }
   }

   @Unique
   private void civilizations$setClientText(UUID buildingId) {
      Optional<Building> building = ClientBuildingStore.INSTANCE.find(buildingId);
      if (building.isEmpty())
         return;

      frontText =
            SignHelper
                  .getBuildingSignText(building.get(), ClientVillagerStore.INSTANCE, ClientSettlementsStore.INSTANCE);
   }

   @Unique
   private Optional<Building> tryLinkWithEnclosingBuilding(ServerLevel level) {

      Optional<Building> enclosingBuilding = ServerBuildingsStore.INSTANCE.findEnclosingBuilding(getBlockPos(), level);
      if (enclosingBuilding.isEmpty())
         return Optional.empty();

      SignBlockEntity existingSign = enclosingBuilding.get().getPrimarySign(level);
      boolean buildingHasAnotherSign = existingSign != null && !existingSign.getBlockPos().equals(getBlockPos());

      if (buildingHasAnotherSign) {

         if (SignHelper.signTextHasSpecialTag(frontText)) {
            // unlink the old sign if this new sign has the special tag
            existingSign.setData(DataAttachments.LINKED_BUILDING, new DataAttachments.LinkedBuilding(null));
            enclosingBuilding.get().setPrimarySignPos(null);
            // clear existing sign text
            existingSign.setText(new SignText(), true);
         } else // otherwise, do not link this sign, as the old one takes precedence
            return Optional.empty();
      } else {
         // blank signs will be given the special tag if there isn't another linked sign
         if (SignHelper.signIsBlank(frontText))
            frontText = SignHelper.createSpecialSignText(); // note: manually setting text does not trigger a block
                                                            // update
      }

      // at this point, the special tag should be set if this sign is intended to be linked.
      if (!SignHelper.signTextHasSpecialTag(frontText))
         return Optional.empty();

      setData(
            DataAttachments.LINKED_BUILDING,
            new DataAttachments.LinkedBuilding(enclosingBuilding.get().getBuildingId()));

      enclosingBuilding.get().setPrimarySignPos(getBlockPos());
      return enclosingBuilding;
   }
}
