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
import com.uncreated.civilized.networking.packets.EditProductionBillUpdateState;
import com.uncreated.civilized.networking.packets.RequestBuildingItemManagementScreen;
import com.uncreated.civilized.networking.packets.SetGhostSlotItem;
import com.uncreated.civilized.networking.packets.RequestEditRecipeProductionScreen;
import com.uncreated.civilized.networking.packets.ShowBuildingScreen;
import com.uncreated.civilized.networking.packets.TellProductionBillRecipeAllowed;
import com.uncreated.civilized.ui.menu.building.residence.artisan.crafting.EditCraftingRecipeMenu;
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

      registrar.playToClient(
            ShowBuildingScreen.TYPE,
            ShowBuildingScreen.STREAM_CODEC,
            ShowBuildingScreen::clientReceiveShowBuildingScreen);

      registrar.playToServer(
            RequestBuildingItemManagementScreen.TYPE,
            RequestBuildingItemManagementScreen.STREAM_CODEC,
            RequestBuildingItemManagementScreen::serverReceiveRequestScreen);

      registrar.playToServer(
            SetGhostSlotItem.TYPE,
            SetGhostSlotItem.STREAM_CODEC,
            SetGhostSlotItem::serverReceiveSetGhostSlotItem);

      registrar.playToServer(
            RequestEditRecipeProductionScreen.TYPE,
            RequestEditRecipeProductionScreen.STREAM_CODEC,
            RequestEditRecipeProductionScreen::serverReceiveRequestScreen);

      registrar.playToServer(
            GiveItemsToPlayer.TYPE,
            GiveItemsToPlayer.STREAM_CODEC,
            RewardActions::serverGiveItemsToPlayer);

      registrar.playToServer(
            VillagerDialogueScreen.ScreenToggledPacket.TYPE,
            VillagerDialogueScreen.ScreenToggledPacket.STREAM_CODEC,
            VillagerDialogueScreen::serverReceiveShowScreen);

      registrar.playToServer(
            EditProductionBillUpdateState.TYPE,
            EditProductionBillUpdateState.CODEC,
            EditCraftingRecipeMenu::serverReceiveDesiredProductionBillAmount);

      registrar.playToClient(
            TellProductionBillRecipeAllowed.TYPE,
            TellProductionBillRecipeAllowed.STREAM_CODEC,
            TellProductionBillRecipeAllowed::clientReceiveRecipeAllowed);
   }
}
