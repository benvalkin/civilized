package com.uncreated.civilized.ui.menu.building.military.tabs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.settlement.loadouts.Loadout;
import com.uncreated.civilized.core.villagerinfo.VillagerInfo;
import com.uncreated.civilized.entity.equipment.CarriedEquipmentSlot;
import com.uncreated.civilized.networking.packets.AssignLoadoutRequest;
import com.uncreated.civilized.networking.packets.RequestSettlementLoadouts;
import com.uncreated.civilized.networking.packets.RequestSoldierEquipment;
import com.uncreated.civilized.ui.components.widget.ArrowSelectorWidget;
import com.uncreated.civilized.ui.components.widget.CarriedSlotIconWidget;
import com.uncreated.civilized.ui.components.widget.ItemDisplayWidget;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.menu.building.ABuildingScreenTab;
import com.uncreated.civilized.ui.menu.building.military.ILoadoutListener;
import com.uncreated.civilized.ui.menu.building.military.LoadoutSlotLayout;
import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class ManageSoldierTab extends ABuildingScreenTab implements ILoadoutListener {

   private static final int LABEL_Y = 12;
   private static final int CONTENT_Y = 24;
   private static final int SLOT_SPACING = 18;
   private static final int ROW_SPACING = 19;
   private static final int ICON_RIGHT_MARGIN = CarriedSlotIconWidget.SIZE + 2;
   private static final int CELL_GAP = 2;
   private static final int CELL_WIDTH = ICON_RIGHT_MARGIN + SLOT_SPACING + CELL_GAP;

   /** Where the settings start, to the right of the equipment grid. */
   private static final int SETTINGS_X = 160;
   private static final int SELECTOR_WIDTH = 126;
   private static final int SELECTOR_HEIGHT = 16;
   private static final int PREVIEW_Y = CONTENT_Y + SELECTOR_HEIGHT + 4;

   private static final int BUTTON_WIDTH = 60;
   private static final int BUTTON_HEIGHT = 18;

   private final VillagerInfo soldier;
   private List<CarriedSlotIconWidget> equipmentIcons = new ArrayList<>();
   private final Button backButton;
   private final Button dismissButton;

   private int equipmentRequestId;
   /** Empty until the server answers. */
   private List<ItemDisplayWidget> equipmentItems = new ArrayList<>();
   private boolean equipmentLoading = true;
   /** The soldier is not loaded in a chunk (e.g. too far away) so the server cannot tell what they're carrying. */
   private boolean equipmentUnavailable;

   private int loadoutsRequestId;
   /** Null until the server has listed the settlement's loadouts. */
   @Nullable
   private ArrowSelectorWidget<Optional<Loadout>> loadoutSelector;
   private List<ItemDisplayWidget> loadoutPreview = List.of();

   public ManageSoldierTab(ITabHost tabHost, Font font, BuildingScreenContext context, VillagerInfo soldier) {
      super(
            tabHost,
            font,
            Component.translatable("menu.building.military.soldier.tab.heading", soldier.getFullName()),
            context);
      this.soldier = soldier;

      backButton =
            Button.builder(
                  Component.translatable("gui.misc.button.back"),
                  button -> tabHost.changeTab(new ManageSoldiersTab(tabHost, font, context)))
                  .pos(getX(), getBottom() - BUTTON_HEIGHT)
                  .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                  .build();

      // FUTURE: send a request to dismiss the soldier, once it's decided what dismissing does
      dismissButton =
            Button.builder(Component.translatable("menu.building.military.soldier.button.dismiss"), button -> {
            }).pos(getRight() - BUTTON_WIDTH, getBottom() - BUTTON_HEIGHT).size(BUTTON_WIDTH, BUTTON_HEIGHT).build();
      dismissButton.active = false;

      equipmentRequestId = RequestSoldierEquipment.nextRequestId();
      PacketDistributor.sendToServer(new RequestSoldierEquipment(equipmentRequestId, soldier.getVillagerId()));

      loadoutsRequestId = RequestSettlementLoadouts.nextRequestId();
      PacketDistributor
            .sendToServer(new RequestSettlementLoadouts(loadoutsRequestId, context.settlement().getSettlementId()));
   }

   private int cellX(CarriedEquipmentSlot slot) {
      return getX() + 1 + LoadoutSlotLayout.gridCellFor(slot).column() * CELL_WIDTH;
   }

   private int cellY(CarriedEquipmentSlot slot) {
      return getY() + CONTENT_Y + LoadoutSlotLayout.gridCellFor(slot).row() * ROW_SPACING;
   }

   public void receiveEquipment(int requestId, @Nullable List<ItemStack> carried) {
      if (requestId != equipmentRequestId)
         return;

      equipmentLoading = false;
      equipmentUnavailable = carried == null;
      if (carried == null) {
         equipmentItems = List.of();
         return;
      }

      equipmentIcons = new ArrayList<>();
      equipmentItems = new ArrayList<>();
      for (int i = 0; i < Loadout.SUPPORTED_SLOTS.size(); i++) {
         CarriedEquipmentSlot slot = Loadout.SUPPORTED_SLOTS.get(i);
         ItemStack itemStack = i < carried.size() ? carried.get(i) : ItemStack.EMPTY;
         int x = getX(i);
         int y = getY() + CONTENT_Y;
         equipmentItems.add(new ItemDisplayWidget(x, y, itemStack));
         equipmentIcons.add(
               new CarriedSlotIconWidget(
                     x,
                     y,
                     LoadoutSlotLayout.watermarkIconFor(slot),
                     LoadoutSlotLayout.nameOf(slot)));
      }
   }

   private int getX(int i) {
      return getX() + i * SLOT_SPACING;
   }

   @Override
   public void receiveListLoadouts(int requestId, List<Loadout> results) {
      if (requestId != loadoutsRequestId)
         return;

      // no loadout comes first, so that it's always one step away
      List<Optional<Loadout>> choices = new ArrayList<>();
      choices.add(Optional.empty());
      results.stream().sorted(Comparator.comparing(Loadout::displayName)).map(Optional::of).forEach(choices::add);

      loadoutSelector =
            new ArrowSelectorWidget<>(
                  getX() + SETTINGS_X,
                  getY() + CONTENT_Y,
                  SELECTOR_WIDTH,
                  SELECTOR_HEIGHT,
                  font,
                  choices,
                  indexOfAssignedLoadout(choices),
                  ManageSoldierTab::loadoutName,
                  this::assignLoadout);
      loadoutPreview = createLoadoutPreview(loadoutSelector.getSelected());
   }

   private int indexOfAssignedLoadout(List<Optional<Loadout>> choices) {
      for (int i = 0; i < choices.size(); i++) {
         Optional<Loadout> choice = choices.get(i);
         if (choice.map(loadout -> loadout.loadoutId().equals(soldier.getLoadoutId())).orElse(false))
            return i;
      }
      return 0;
   }

   private static Component loadoutName(Optional<Loadout> loadout) {
      return loadout.<Component> map(l -> Component.literal(l.displayName()))
            .orElse(Component.translatable("menu.building.military.soldier.loadout.none"));
   }

   private void assignLoadout(Optional<Loadout> loadout) {
      loadoutPreview = createLoadoutPreview(loadout);
      PacketDistributor
            .sendToServer(new AssignLoadoutRequest(soldier.getVillagerId(), loadout.map(Loadout::loadoutId)));
   }

   /** The most preferred item of each of the loadout's slots, like the rows in the loadouts list. */
   private List<ItemDisplayWidget> createLoadoutPreview(Optional<Loadout> loadout) {
      if (loadout.isEmpty())
         return List.of();

      List<ItemDisplayWidget> preview = new ArrayList<>();
      for (CarriedEquipmentSlot slot : Loadout.SUPPORTED_SLOTS) {
         Optional<ItemStack> mostPreferred =
               loadout.get()
                     .getAllowedItems(slot)
                     .flatMap(filter -> filter.items().stream().filter(item -> !item.isEmpty()).findFirst());
         if (mostPreferred.isEmpty())
            continue;

         int x = getX() + SETTINGS_X + 1 + preview.size() * SLOT_SPACING;
         preview.add(new ItemDisplayWidget(x, getY() + PREVIEW_Y, mostPreferred.get()));
      }
      return preview;
   }

   @Override
   public void refresh() {
      // the server may have turned the change down, or someone else may have changed it since
      if (loadoutSelector == null)
         return;

      loadoutSelector.forceSetSelectedIndex(indexOfAssignedLoadout(loadoutSelector.getValues()));
      loadoutPreview = createLoadoutPreview(loadoutSelector.getSelected());
   }

   @Override
   public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderWidget(graphics, mouseX, mouseY, partialTicks);

      renderEquipment(graphics, mouseX, mouseY, partialTicks);
      renderLoadout(graphics, mouseX, mouseY, partialTicks);

      backButton.render(graphics, mouseX, mouseY, partialTicks);
      dismissButton.render(graphics, mouseX, mouseY, partialTicks);
   }

   private void renderEquipment(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      graphics.drawString(
            font,
            Component.translatable("menu.building.military.soldier.equipment"),
            getX(),
            getY() + LABEL_Y,
            Colors.MENU_TEXT_DARK,
            false);

      for (int i = 0; i < equipmentItems.size(); i++) {
         ItemDisplayWidget item = equipmentItems.get(i);
         if (!item.getDisplayItem().isEmpty())
            item.render(graphics, mouseX, mouseY, partialTicks);
         else {
            CarriedSlotIconWidget watermarkIcon = equipmentIcons.get(i);
            watermarkIcon.render(graphics, mouseX, mouseY, partialTicks);
         }
      }

      Component status = null;
      if (equipmentLoading)
         status = Component.translatable("gui.misc.loading");
      else if (equipmentUnavailable)
         status = Component.translatable("menu.building.military.soldier.equipment.unavailable");

      if (status != null)
         graphics.drawWordWrap(
               font,
               status,
               getX(),
               getY() + CONTENT_Y + ROW_SPACING * 2 + 4,
               SETTINGS_X - 10,
               Colors.MENU_TEXT_DARK,
               false);
   }

   private void renderLoadout(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      graphics.drawString(
            font,
            Component.translatable("menu.building.military.soldier.loadout"),
            getX() + SETTINGS_X,
            getY() + LABEL_Y,
            Colors.MENU_TEXT_DARK,
            false);

      if (loadoutSelector == null) {
         graphics.drawString(
               font,
               Component.translatable("gui.misc.loading"),
               getX() + SETTINGS_X,
               getY() + CONTENT_Y + (SELECTOR_HEIGHT - font.lineHeight) / 2 + 1,
               Colors.MENU_TEXT_DARK,
               false);
         return;
      }

      loadoutSelector.render(graphics, mouseX, mouseY, partialTicks);
      loadoutPreview.forEach(item -> item.render(graphics, mouseX, mouseY, partialTicks));
   }

   @Override
   public List<? extends GuiEventListener> children() {
      List<GuiEventListener> children = new ArrayList<>(List.of(backButton, dismissButton));
      if (loadoutSelector != null)
         children.add(loadoutSelector);
      return children;
   }
}
