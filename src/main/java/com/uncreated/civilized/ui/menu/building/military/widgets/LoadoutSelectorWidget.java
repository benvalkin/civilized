package com.uncreated.civilized.ui.menu.building.military.widgets;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.uncreated.civilized.core.settlement.loadouts.Loadout;
import com.uncreated.civilized.entity.equipment.CarriedEquipmentSlot;
import com.uncreated.civilized.ui.components.widget.ArrowSelectorWidget;
import com.uncreated.civilized.ui.components.widget.ItemDisplayWidget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class LoadoutSelectorWidget extends AbstractContainerWidget {

   public static final int SELECTOR_HEIGHT = 16;

   private static final int PREVIEW_GAP = 4;
   private static final int SLOT_SPACING = 18;

   private final Font font;
   private final int loadingTextColour;
   private final boolean showPreviewItems;
   private final Consumer<LoadoutChoice> onSelected;

   /** Null until the ListLoadouts packet is received from the server. */
   @Nullable
   private ArrowSelectorWidget<LoadoutChoice> selector;
   private List<ItemDisplayWidget> preview = List.of();

   public LoadoutSelectorWidget(
         int x,
         int y,
         int width,
         Font font,
         int loadingTextColour,
         boolean showPreviewItems,
         Consumer<LoadoutChoice> onSelected) {
      super(x, y, width, heightFor(showPreviewItems), Component.empty());
      this.font = font;
      this.loadingTextColour = loadingTextColour;
      this.showPreviewItems = showPreviewItems;
      this.onSelected = onSelected;
   }

   public static int heightFor(boolean showPreviewItems) {
      if (!showPreviewItems)
         return SELECTOR_HEIGHT;

      return SELECTOR_HEIGHT + PREVIEW_GAP + 16;
   }

   /**
    * @param assignedLoadoutId
    *           the loadout to start on, or null for no loadout
    */
   public void setLoadouts(List<Loadout> loadouts, @Nullable UUID assignedLoadoutId) {
      // no loadout is always the first in the list so that it's always arrow click step away
      List<LoadoutChoice> choices = new ArrayList<>();
      choices.add(LoadoutChoice.noLoadout());
      loadouts.stream()
            .sorted(Comparator.comparing(Loadout::displayName))
            .forEach(l -> choices.add(LoadoutChoice.of(l)));

      selector =
            new ArrowSelectorWidget<>(
                  getX(),
                  getY(),
                  width,
                  SELECTOR_HEIGHT,
                  font,
                  choices,
                  indexOf(choices, assignedLoadoutId),
                  LoadoutSelectorWidget::loadoutName,
                  this::select);
      preview = createPreview(selector.getSelected());
   }

   /**
    * Shows the loadout the soldier actually has, without calling {@code onSelected}: the server may have turned the
    * player's choice down, or someone else may have changed it since.
    */
   public void showAssigned(@Nullable UUID assignedLoadoutId) {
      if (selector == null)
         return;

      selector.forceSetSelectedIndex(indexOf(selector.getValues(), assignedLoadoutId));
      preview = createPreview(selector.getSelected());
   }

   private static int indexOf(List<LoadoutChoice> choices, @Nullable UUID loadoutId) {
      for (int i = 0; i < choices.size(); i++) {
         LoadoutChoice choice = choices.get(i);
         if (choice.getLoadout().isEmpty())
            continue;

         if (choice.getLoadout().get().loadoutId().equals(loadoutId))
            return i;
      }

      return 0;
   }

   private static Component loadoutName(LoadoutChoice choice) {

      if (choice.getLoadout().isPresent())
         return Component.literal(choice.getLoadout().get().displayName());

      return Component.translatable("menu.building.military.soldier.loadout.none");
   }

   private void select(LoadoutChoice choice) {
      preview = createPreview(choice);
      onSelected.accept(choice);
   }

   private List<ItemDisplayWidget> createPreview(LoadoutChoice choice) {
      if (!showPreviewItems || choice.getLoadout().isEmpty())
         return List.of();

      List<ItemDisplayWidget> preview = new ArrayList<>();
      for (CarriedEquipmentSlot slot : Loadout.SUPPORTED_SLOTS) {
         Optional<ItemStack> mostPreferred =
               choice.getLoadout()
                     .get()
                     .getAllowedItems(slot)
                     .flatMap(filter -> filter.items().stream().filter(item -> !item.isEmpty()).findFirst());
         if (mostPreferred.isEmpty())
            continue;

         int x = getX() + 1 + preview.size() * SLOT_SPACING;
         preview.add(new ItemDisplayWidget(x, getY() + SELECTOR_HEIGHT + PREVIEW_GAP, mostPreferred.get()));
      }
      return preview;
   }

   @Override
   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      if (selector == null) {
         graphics.drawString(
               font,
               Component.translatable("gui.misc.loading"),
               getX(),
               getY() + (SELECTOR_HEIGHT - font.lineHeight) / 2 + 1,
               loadingTextColour,
               false);
         return;
      }

      selector.render(graphics, mouseX, mouseY, partialTick);
      preview.forEach(item -> item.render(graphics, mouseX, mouseY, partialTick));
   }

   /** Scrolling over the selector steps through the loadouts, like it does on its own. */
   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      return selector != null && selector.isMouseOver(mouseX, mouseY)
            && selector.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      return selector == null ? List.of() : List.of(selector);
   }

   @Override
   protected int contentHeight() {
      return height;
   }

   @Override
   protected double scrollRate() {
      return 0;
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
   }
}
