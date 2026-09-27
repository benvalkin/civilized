package com.uncreated.civilized.ui.menu.building.townhall.tabs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.settlement.permission.AccessLevel;
import com.uncreated.civilized.core.settlement.permission.PlayerPermission;
import com.uncreated.civilized.ui.components.IListViewBuilder;
import com.uncreated.civilized.ui.components.ScrollListView;
import com.uncreated.civilized.ui.context.BuildingScreenContext;
import com.uncreated.civilized.ui.menu.building.ABuildingScreenTab;
import com.uncreated.civilized.ui.menu.building.widgets.ManagePermissionWidget;
import com.uncreated.civilized.ui.style.Colors;
import com.uncreated.civilized.ui.tabs.ITabHost;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

public class ManagePermissionsTab extends ABuildingScreenTab {

   private static final int LIST_TOP = 30;
   private static final int ELEMENT_HEIGHT = 20;

   private record PlayerRow(UUID playerId, String name, @Nullable AccessLevel accessLevel) {
   }

   private ScrollListView<PlayerRow, ManagePermissionWidget> scrollView;
   private boolean canManage;

   public ManagePermissionsTab(ITabHost tabHost, Font font, BuildingScreenContext context) {
      super(tabHost, font, Component.translatable("menu.building.town_hall.permissions.tab.heading"), context);
      refresh();
   }

   @Override
   public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderWidget(graphics, mouseX, mouseY, partialTicks);

      Component subHeading =
            Component.translatable(
                  canManage
                        ? "menu.building.town_hall.permissions.heading"
                        : "menu.building.town_hall.permissions.heading.governors_only");
      graphics.drawWordWrap(font, subHeading, getX(), getY() + 15, width, Colors.MENU_TEXT_DARK, false);

      scrollView.render(graphics, mouseX, mouseY, partialTicks);
   }

   @Override
   public List<? extends GuiEventListener> children() {
      List<GuiEventListener> children = new ArrayList<>();
      children.add(scrollView);
      children.addAll(scrollView.children());
      return children;
   }

   @Override
   public void refresh() {
      List<PlayerPermission> permissions = List.copyOf(context.permissions().entries());

      UUID viewerId = Minecraft.getInstance().player.getUUID();
      canManage =
            permissions.stream()
                  .anyMatch(p -> p.playerId().equals(viewerId) && p.accessLevel().isAboveOrEqualTo(AccessLevel.GOVERNOR));

      scrollView = createScrollView(createRows(permissions));
   }

   /** Players with access come first, highest access level first, then online players who have none. */
   private static List<PlayerRow> createRows(List<PlayerPermission> permissions) {
      List<PlayerRow> rows = new ArrayList<>();
      permissions.stream()
            .sorted(
                  Comparator.comparing(PlayerPermission::accessLevel)
                        .reversed()
                        .thenComparing(PlayerPermission::scoreboardName, String.CASE_INSENSITIVE_ORDER))
            .forEach(p -> rows.add(new PlayerRow(p.playerId(), p.scoreboardName(), p.accessLevel())));

      // only online players appear in list at the moment - the client doesn't know who else has ever joined
      Set<UUID> withAccess = permissions.stream().map(PlayerPermission::playerId).collect(Collectors.toSet());
      Minecraft.getInstance()
            .getConnection()
            .getOnlinePlayers()
            .stream()
            .map(PlayerInfo::getProfile)
            .filter(profile -> !withAccess.contains(profile.getId()))
            .sorted(Comparator.comparing(profile -> profile.getName(), String.CASE_INSENSITIVE_ORDER))
            .forEach(profile -> rows.add(new PlayerRow(profile.getId(), profile.getName(), null)));

      return rows;
   }

   private ScrollListView<PlayerRow, ManagePermissionWidget> createScrollView(List<PlayerRow> rows) {
      return new ScrollListView<>(
            getX(),
            getY() + LIST_TOP,
            width,
            height - LIST_TOP,
            ELEMENT_HEIGHT,
            new IListViewBuilder<>() {
               @Override
               public List<PlayerRow> provideModelData() {
                  return rows;
               }

               @Override
               public ManagePermissionWidget buildElementWidgetFromModel(
                     int elementIndex,
                     PlayerRow row,
                     int elementX,
                     int elementY,
                     int elementWidth,
                     int elementHeight,
                     int elementSpacing) {
                  return new ManagePermissionWidget(
                        elementX,
                        elementY,
                        elementWidth,
                        elementHeight,
                        font,
                        context.settlement().getSettlementId(),
                        row.playerId(),
                        row.name(),
                        row.accessLevel(),
                        canManage);
               }
            });
   }
}
