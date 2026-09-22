package com.uncreated.civilized.networking;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.ClientBuildingStore;
import com.uncreated.civilized.core.building.ServerBuildingsStore;
import com.uncreated.civilized.core.dialogue.rewards.GiveItemsToPlayer;
import com.uncreated.civilized.core.dialogue.rewards.RewardActions;
import com.uncreated.civilized.core.quest.ActivatedQuest;
import com.uncreated.civilized.core.quest.attachments.PlayerQuests;
import com.uncreated.civilized.core.settlement.ClientSettlementsStore;
import com.uncreated.civilized.core.settlement.ServerSettlementsStore;
import com.uncreated.civilized.core.settlement.Settlement;
import com.uncreated.civilized.core.villagerinfo.ClientVillagerStore;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.networking.packets.CreateNewBuilding;
import com.uncreated.civilized.networking.packets.PreviewProductionBill;
import com.uncreated.civilized.networking.packets.ProductionBillPreview;
import com.uncreated.civilized.networking.packets.SaveProductionBill;
import com.uncreated.civilized.networking.packets.SetEyeDropperSlotItem;
import com.uncreated.civilized.ui.menu.dialogue.VillagerDialogueScreen;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class PacketRegistry {
   @SubscribeEvent
   public static void register(final RegisterPayloadHandlersEvent event) {
      // Sets the current network version
      final PayloadRegistrar registrar = event.registrar("1");
      // registrar.executesOn(HandlerThread.NETWORK); // All subsequent payloads will register on the network thread

      // synced data stores
      registrar.playBidirectional(
            Building.Packet.SYNC_TYPE,
            Building.Packet.STREAM_CODEC,
            new DirectionalPayloadHandler<>(
                  ClientBuildingStore::receiveSyncFromServer,
                  ServerBuildingsStore::receiveSyncFromClient));

      registrar.playBidirectional(
            Settlement.Packet.SYNC_TYPE,
            Settlement.Packet.STREAM_CODEC,
            new DirectionalPayloadHandler<>(
                  ClientSettlementsStore::receiveSyncFromServer,
                  ServerSettlementsStore::receiveSyncFromClient));

      registrar.playBidirectional(
            VillagerInfo.Packet.SYNC_TYPE,
            VillagerInfo.Packet.STREAM_CODEC,
            new DirectionalPayloadHandler<>(
                  ClientVillagerStore::receiveSyncFromServer,
                  ServerVillagerStore::receiveSyncFromClient));

      // quests
      registrar.playBidirectional(
            ActivatedQuest.TYPE,
            ActivatedQuest.STREAM_CODEC,
            new DirectionalPayloadHandler<>(PlayerQuests::receiveSyncFromServer, PlayerQuests::receiveSyncFromClient));

      // bespoke actions
      registrar.playToServer(
            CreateNewBuilding.TYPE,
            CreateNewBuilding.STREAM_CODEC,
            CreateNewBuilding::serverReceiveCreateNewBuilding);

      registrar.playToServer(
            SetEyeDropperSlotItem.TYPE,
            SetEyeDropperSlotItem.STREAM_CODEC,
            SetEyeDropperSlotItem::serverReceiveSetEyeDropperSlotItem);

      registrar.playToServer(
            GiveItemsToPlayer.TYPE,
            GiveItemsToPlayer.STREAM_CODEC,
            RewardActions::serverGiveItemsToPlayer);

      registrar.playToServer(
            VillagerDialogueScreen.ScreenToggledPacket.TYPE,
            VillagerDialogueScreen.ScreenToggledPacket.STREAM_CODEC,
            VillagerDialogueScreen::serverReceiveShowScreen);

      registrar.playToServer(
            PreviewProductionBill.TYPE,
            PreviewProductionBill.STREAM_CODEC,
            PreviewProductionBill::serverReceivePreviewProductionBill);

      registrar.playToClient(
            ProductionBillPreview.TYPE,
            ProductionBillPreview.STREAM_CODEC,
            ProductionBillPreview::clientReceiveProductionBillPreview);

      registrar.playToServer(
            SaveProductionBill.TYPE,
            SaveProductionBill.STREAM_CODEC,
            SaveProductionBill::serverReceiveSaveProductionBill);
   }
}
