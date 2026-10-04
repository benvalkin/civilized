package com.uncreated.civilized.networking;

import com.uncreated.civilized.client.ClientPacketHandlers;
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
import com.uncreated.civilized.core.settlement.permission.ClientSettlementPermissionStore;
import com.uncreated.civilized.core.settlement.permission.SettlementPermissions;
import com.uncreated.civilized.core.villagerinfo.ClientVillagerStore;
import com.uncreated.civilized.core.villagerinfo.ServerVillagerStore;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.networking.packets.BuildingPlacementRejected;
import com.uncreated.civilized.networking.packets.BuyItem;
import com.uncreated.civilized.networking.packets.CheckUpgradeRequirements;
import com.uncreated.civilized.networking.packets.CreateNewBuilding;
import com.uncreated.civilized.networking.packets.OpenEstablishBuildingScreen;
import com.uncreated.civilized.networking.packets.ResizeBuilding;
import com.uncreated.civilized.networking.packets.RequestRedrawBuilding;
import com.uncreated.civilized.networking.packets.OpenRedrawBoundsScreen;
import com.uncreated.civilized.networking.packets.PreviewProductionBill;
import com.uncreated.civilized.networking.packets.RequestEstablishBuilding;
import com.uncreated.civilized.networking.packets.UpgradeBuilding;
import com.uncreated.civilized.networking.packets.RequirementsChecked;
import com.uncreated.civilized.networking.packets.ProductionBillPreview;
import com.uncreated.civilized.networking.packets.SaveProductionBill;
import com.uncreated.civilized.networking.packets.SellItem;
import com.uncreated.civilized.networking.packets.SetEyeDropperSlotItem;
import com.uncreated.civilized.networking.packets.NotificationToast;
import com.uncreated.civilized.networking.packets.SetSettlementAccessLevel;
import com.uncreated.civilized.networking.packets.SettlementAccessLevelDenied;
import com.uncreated.civilized.networking.packets.TradeSlotUpdated;
import com.uncreated.civilized.networking.packets.VillagerDialogueScreenToggled;

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

      registrar.playToClient(
            SettlementPermissions.Packet.SYNC_TYPE,
            SettlementPermissions.Packet.STREAM_CODEC,
            ClientSettlementPermissionStore::receiveSyncFromServer);

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
            RequestEstablishBuilding.TYPE,
            RequestEstablishBuilding.STREAM_CODEC,
            RequestEstablishBuilding::serverReceiveRequestEstablishBuilding);

      registrar.playToClient(
            BuildingPlacementRejected.TYPE,
            BuildingPlacementRejected.STREAM_CODEC,
            ClientPacketHandlers::receiveBuildingPlacementRejected);

      registrar.playToServer(
            RequestRedrawBuilding.TYPE,
            RequestRedrawBuilding.STREAM_CODEC,
            RequestRedrawBuilding::serverReceiveRequestRedrawBuilding);

      registrar.playToClient(
            OpenRedrawBoundsScreen.TYPE,
            OpenRedrawBoundsScreen.STREAM_CODEC,
            ClientPacketHandlers::openRedrawBoundsScreen);

      registrar.playToServer(
            ResizeBuilding.TYPE,
            ResizeBuilding.STREAM_CODEC,
            ResizeBuilding::serverReceiveResizeBuilding);

      registrar.playToClient(
            OpenEstablishBuildingScreen.TYPE,
            OpenEstablishBuildingScreen.STREAM_CODEC,
            ClientPacketHandlers::openEstablishBuildingScreen);

      registrar.playToServer(
            UpgradeBuilding.TYPE,
            UpgradeBuilding.STREAM_CODEC,
            UpgradeBuilding::serverReceiveUpgradeBuilding);

      registrar.playToServer(
            CheckUpgradeRequirements.TYPE,
            CheckUpgradeRequirements.STREAM_CODEC,
            CheckUpgradeRequirements::serverReceiveCheckUpgradeRequirements);

      registrar.playToClient(
            RequirementsChecked.TYPE,
            RequirementsChecked.STREAM_CODEC,
            ClientPacketHandlers::receiveRequirementsChecked);

      registrar.playToServer(
            SetEyeDropperSlotItem.TYPE,
            SetEyeDropperSlotItem.STREAM_CODEC,
            SetEyeDropperSlotItem::serverReceiveSetEyeDropperSlotItem);

      registrar.playToServer(
            GiveItemsToPlayer.TYPE,
            GiveItemsToPlayer.STREAM_CODEC,
            RewardActions::serverGiveItemsToPlayer);

      registrar.playToServer(
            VillagerDialogueScreenToggled.TYPE,
            VillagerDialogueScreenToggled.STREAM_CODEC,
            VillagerDialogueScreenToggled::serverReceiveVillagerDialogueScreenToggled);

      registrar.playToServer(
            PreviewProductionBill.TYPE,
            PreviewProductionBill.STREAM_CODEC,
            PreviewProductionBill::serverReceivePreviewProductionBill);

      registrar.playToClient(
            ProductionBillPreview.TYPE,
            ProductionBillPreview.STREAM_CODEC,
            ClientPacketHandlers::receiveProductionBillPreview);

      registrar.playToServer(
            SaveProductionBill.TYPE,
            SaveProductionBill.STREAM_CODEC,
            SaveProductionBill::serverReceiveSaveProductionBill);

      registrar.playToServer(
            SetSettlementAccessLevel.TYPE,
            SetSettlementAccessLevel.STREAM_CODEC,
            SetSettlementAccessLevel::serverReceiveSetSettlementAccessLevel);

      registrar.playToClient(
            NotificationToast.TYPE,
            NotificationToast.STREAM_CODEC,
            ClientPacketHandlers::receiveNotificationToast);

      registrar.playToClient(
            SettlementAccessLevelDenied.TYPE,
            SettlementAccessLevelDenied.STREAM_CODEC,
            ClientPacketHandlers::receiveSettlementAccessLevelDenied);

      registrar.playToServer(
            BuyItem.TYPE,
            BuyItem.STREAM_CODEC,
            BuyItem::serverReceiveBuyItem);

      registrar.playToServer(
              SellItem.TYPE,
              SellItem.STREAM_CODEC,
              SellItem::serverReceiveSellItem);

      registrar.playToClient(
              TradeSlotUpdated.TYPE,
              TradeSlotUpdated.STREAM_CODEC,
              ClientPacketHandlers::receiveTradeSlotUpdated);
   }
}
