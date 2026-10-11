package com.uncreated.civilized.ui.menu.building.townsquare.tabs;

import java.util.List;
import java.util.Optional;

import com.uncreated.civilized.core.building.util.BuildingUtil;
import com.uncreated.civilized.core.settlement.SettlementState;
import com.uncreated.civilized.core.settlement.festival.Festival;
import com.uncreated.civilized.core.villagerinfo.ClientVillagerStore;
import com.uncreated.civilized.item.CurrencyItem;
import com.uncreated.civilized.networking.packets.StartFestival;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.menu.building.ABuildingScreenTab;
import com.uncreated.civilized.ui.menu.building.AlterBuildingPermissions;
import com.uncreated.civilized.ui.menu.building.IBuildingInfoTab;
import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class TownSquareInfoTab extends ABuildingScreenTab implements IBuildingInfoTab {

   private static final int BUTTON_WIDTH = 160;
   private static final int BUTTON_HEIGHT = 18;
   private static final int FESTIVAL_TOP = 45;

   private final Button festivalButton;
   private int visitorCount;
   private Component festivalTooltip = Component.empty();

   public TownSquareInfoTab(ITabHost tabHost, Font font, BuildingScreenContext context) {
      super(
            tabHost,
            font,
            context.settlement()
                  .displayNameTranslationExtended()
                  .withColor(Colors.SETTLEMENT_NAME)
                  .withStyle(ChatFormatting.ITALIC),
            context);

      int festivalCost = Festival.cost(context.settlement().getState());

      festivalButton =
            Button.builder(
                  Component.translatable(
                        "menu.building.town_square.festival.button",
                        CurrencyItem.amountTranslation(festivalCost).withColor(Colors.COIN)),
                  button -> cloaseAndStartFestival())
                  .pos(getX() + (getWidth() - BUTTON_WIDTH) / 2, getBottom() - BUTTON_HEIGHT)
                  .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                  .build();

      visitorCount = countVisitors();
   }

   private SettlementState state() {
      return context.settlement().getState();
   }

   private static long dayTime() {
      return Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getDayTime();
   }

   private void cloaseAndStartFestival() {
      PacketDistributor.sendToServer(new StartFestival(context.building().getBuildingId()));
      if (Minecraft.getInstance().player != null)
         Minecraft.getInstance().player.closeContainer();
   }

   private int countVisitors() {
      return BuildingUtil.getVisitors(context.building(), ClientVillagerStore.INSTANCE).size();
   }

   @Override
   public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderWidget(graphics, mouseX, mouseY, partialTicks);

      graphics.drawString(
            font,
            Component.translatable("menu.building.town_square.visitors.count", visitorCount),
            getX(),
            getY() + 20,
            Colors.MENU_TEXT_DARK,
            false);

      graphics.drawString(
            font,
            Component.translatable("menu.building.town_square.festival.heading").withStyle(ChatFormatting.UNDERLINE),
            getX(),
            getY() + FESTIVAL_TOP,
            Colors.MENU_TEXT_DARK,
            false);
      graphics.drawWordWrap(
            font,
            festivalStatus(),
            getX(),
            getY() + FESTIVAL_TOP + 12,
            width,
            Colors.MENU_TEXT_DARK,
            false);

      updateFestivalButton(); // festival button must be updated every frame
      festivalButton.render(graphics, mouseX, mouseY, partialTicks);
   }

   private Component festivalStatus() {
      SettlementState state = state();
      long dayTime = dayTime();
      long ticksLeft;
      return switch (Festival.phase(state, dayTime)) {
      case GATHERING -> {
         ticksLeft = state.getFestivalStartTime() + Festival.GATHERING_DURATION - dayTime;
         yield Component.translatable("menu.building.town_square.festival.gathering", hoursLeft(ticksLeft));
      }
      case CELEBRATING -> {
         ticksLeft = Festival.endTime(state) - dayTime;
         yield Component.translatable("menu.building.town_square.festival.celebrating", hoursLeft(ticksLeft));
      }
      // includes a festival that's over, but that the server hasn't wrapped up yet
      case NONE -> Festival.isUnderway(state) ? Component.translatable("festival.cannot_start.underway")
            : Component.translatable("menu.building.town_square.festival.description");
      };
   }

   private static long hoursLeft(long ticks) {
      return Math.max(1, (ticks + 999) / 1000);
   }

   private void updateFestivalButton() {
      Optional<Component> whyNot = Festival.whyCannotStart(state(), dayTime(), context.availableCoins());
      boolean hasPermission = AlterBuildingPermissions.hasPermission(context);
      festivalButton.active = hasPermission && whyNot.isEmpty();

      Component tooltip;
      if (!hasPermission)
         tooltip = Component.translatable("menu.building.town_square.festival.no_permission");
      else
         tooltip =
               whyNot.orElseGet(
                     () -> Component.translatable(
                           "menu.building.town_square.festival.button.tooltip",
                           CurrencyItem.amountTranslation(Festival.cost(state()))));

      if (!tooltip.equals(festivalTooltip)) {
         festivalTooltip = tooltip;
         festivalButton.setTooltip(Tooltip.create(tooltip));
      }
   }

   @Override
   public List<? extends GuiEventListener> children() {
      return List.of(festivalButton);
   }

   @Override
   public void refresh() {
      visitorCount = countVisitors();
   }
}
