package com.uncreated.civilized.mixins;

import java.util.Optional;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingScreenOpener;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.neoforge.registration.attachments.DataAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

@Mixin(SignBlock.class)
public abstract class SignBlockMixin extends BaseEntityBlock implements SimpleWaterloggedBlock {

   public SignBlockMixin(Properties properties) {
      super(properties);
   }

   @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true, remap = false)
   public void useWithOutItem(
         BlockState blockState,
         Level level,
         BlockPos pos,
         Player player,
         BlockHitResult hit,
         CallbackInfoReturnable<InteractionResult> cir) {

      if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
         return;
      }

      if (!(level.getBlockEntity(pos) instanceof SignBlockEntity signEntity))
         return;

      UUID buildingId = signEntity.getData(DataAttachments.LINKED_BUILDING).getBuildingId();
      if (buildingId == null)
         return;

      Optional<Building> building = ServerBuildingsStore.INSTANCE.find(buildingId);
      if (building.isEmpty())
         return;

      // Settlement settlement = ServerSettlementsStore.INSTANCE.get(building.get().getSettlementId());
      //
      // BuildingMenu menu =
      // new BuildingMenu(0, serverPlayer.getInventory(), new SimpleContainer(9), settlement, building.get());
      //
      // var containerSynchronizer = new ContainerSynchronizer() {
      // public void sendInitialData(
      // AbstractContainerMenu p_143448_,
      // NonNullList<ItemStack> p_143449_,
      // ItemStack p_143450_,
      // int[] p_143451_) {
      // serverPlayer.connection.send(
      // new ClientboundContainerSetContentPacket(
      // p_143448_.containerId,
      // p_143448_.incrementStateId(),
      // p_143449_,
      // p_143450_));
      //
      // for (int i = 0; i < p_143451_.length; ++i) {
      // this.broadcastDataValue(p_143448_, i, p_143451_[i]);
      // }
      //
      // }
      //
      // public void sendSlotChange(AbstractContainerMenu p_143441_, int p_143442_, ItemStack p_143443_) {
      // serverPlayer.connection.send(
      // new ClientboundContainerSetSlotPacket(
      // p_143441_.containerId,
      // p_143441_.incrementStateId(),
      // p_143442_,
      // p_143443_));
      // }
      //
      // public void sendCarriedChange(AbstractContainerMenu p_143445_, ItemStack p_143446_) {
      // serverPlayer.connection.send(new ClientboundSetCursorItemPacket(p_143446_.copy()));
      // }
      //
      // public void sendDataChange(AbstractContainerMenu p_143437_, int p_143438_, int p_143439_) {
      // this.broadcastDataValue(p_143437_, p_143438_, p_143439_);
      // }
      //
      // private void broadcastDataValue(AbstractContainerMenu container, int id, int value) {
      // if (serverPlayer.connection.hasChannel(AdvancedContainerSetDataPayload.TYPE)) {
      // serverPlayer.connection
      // .send(new AdvancedContainerSetDataPayload((byte) container.containerId, (short) id, value));
      // } else {
      // serverPlayer.connection.send(new ClientboundContainerSetDataPacket(container.containerId, id, value));
      // }
      // }
      // };
      // var containerListener = new ContainerListener() {
      // public void slotChanged(AbstractContainerMenu p_143466_, int p_143467_, ItemStack p_143468_) {
      // Slot slot = p_143466_.getSlot(p_143467_);
      // if (!(slot instanceof ResultSlot) && slot.container == serverPlayer.getInventory()) {
      // CriteriaTriggers.INVENTORY_CHANGED.trigger(serverPlayer, serverPlayer.getInventory(), p_143468_);
      // }
      //
      // }
      //
      // public void dataChanged(AbstractContainerMenu p_143462_, int p_143463_, int p_143464_) {
      // }
      // };
      //
      // menu.addSlotListener(containerListener);
      // menu.setSynchronizer(containerSynchronizer);
      // serverPlayer.containerMenu = menu;

      BuildingScreenOpener.open(serverPlayer, building.get());
      // building.get());

      // player.openMenu(new MenuProvider() {
      // @Override
      // public Component getDisplayName() {
      // return building.get().getBuildingType().translationDark().withStyle(ChatFormatting.UNDERLINE);
      // }
      //
      // @Override
      // public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
      // return new BuildingMenu(i, inventory, new SimpleContainer(9), settlement, building.get());
      // }
      //
      // @Override
      // public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
      // buffer.writeUUID(settlement.getSettlementId());
      // buffer.writeUUID(building.get().getBuildingId());
      // }
      // });

      cir.setReturnValue(InteractionResult.SUCCESS);
      cir.cancel();
   }
}
