package com.tostky;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.InventoryID;
import net.runelite.api.Client;
import net.runelite.api.Item;
import java.util.Arrays;
import org.apache.commons.lang3.ArrayUtils; // In case you use it for ArrayUtils.contains in getItemCount


public class TostkyOverlay extends Overlay {
    private Client client;

    private final TostkyPlugin plugin;
    private final TostkyConfig config;
    private final PanelComponent panelComponent = new PanelComponent();

    @Inject
    public TostkyOverlay(TostkyPlugin plugin, TostkyConfig config, Client client) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
    }

    private int getItemCount(int... itemIds) {
        ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        int count = 0;

        // Check and sum up from the inventory
        if (inventory != null) {
            count += Arrays.stream(inventory.getItems())
                    .filter(item -> Arrays.stream(itemIds).anyMatch(id -> id == item.getId()))
                    .mapToInt(Item::getQuantity)
                    .sum();
        }

        // Check and sum up from the equipment
        if (equipment != null) {
            count += Arrays.stream(equipment.getItems())
                    .filter(item -> Arrays.stream(itemIds).anyMatch(id -> id == item.getId()))
                    .mapToInt(Item::getQuantity)
                    .sum();
        }

        return count; // Return the combined count from both inventory and equipment
    }

    public void update() {
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
    }

    private static final Logger log = LoggerFactory.getLogger(TostkyPlugin.class);

    @Override
    public Dimension render(Graphics2D graphics) {
        panelComponent.getChildren().clear();

        // Get the current inventory
        ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);

        if (inventory != null || equipment != null) {
            // Example of checking configuration and displaying count for Cannon Balls
            if (config.trackCannonBalls()) {
                int cannonBallCount = getItemCount(ItemID.CANNONBALL);
                if (cannonBallCount > 0) {
                        panelComponent.getChildren().add(LineComponent.builder().left("Cannon Balls:").right(String.valueOf(cannonBallCount)).build());
                }
            }

            // Repeat for Arrows with config check
            if (config.trackArrows()) {
                int arrowCount = getItemCount(ItemID.BRONZE_ARROW, ItemID.IRON_ARROW, ItemID.STEEL_ARROW, ItemID.MITHRIL_ARROW, ItemID.ADAMANT_ARROW, ItemID.RUNE_ARROW, ItemID.AMETHYST_ARROW, ItemID.DRAGON_ARROW);
                if (arrowCount > 0) {
                    panelComponent.getChildren().add(LineComponent.builder().left("Arrows:").right(String.valueOf(arrowCount)).build());
                }
            }
            if (config.trackBolts()) {
                int boltCount = getItemCount(ItemID.BRONZE_BOLTS, ItemID.IRON_BOLTS, ItemID.STEEL_BOLTS, ItemID.MITHRIL_BOLTS, ItemID.ADAMANT_BOLTS, ItemID.RUNITE_BOLTS, ItemID.RUNITE_BOLTS_P, ItemID.DRAGON_BOLTS, ItemID.AMETHYST_BROAD_BOLTS, ItemID.RUBY_BOLTS_E, ItemID.EMERALD_BOLTS_E, ItemID.DIAMOND_BOLTS_E, ItemID.ONYX_BOLTS_E);
                if (boltCount > 0) {
                    panelComponent.getChildren().add(LineComponent.builder().left("Bolts:").right(String.valueOf(boltCount)).build());
                }
            }
            if (config.trackDarts()) {
                int dartCount = getItemCount(ItemID.BRONZE_DART, ItemID.IRON_DART, ItemID.STEEL_DART, ItemID.MITHRIL_DART, ItemID.ADAMANT_DART, ItemID.RUNE_DART, ItemID.DRAGON_DART, ItemID.AMETHYST_DART);
                if (dartCount > 0) {
                    panelComponent.getChildren().add(LineComponent.builder().left("Darts:").right(String.valueOf(dartCount)).build());
                }
            }
            if (config.trackJavelins()) {
                int javelinCount = getItemCount(ItemID.BRONZE_JAVELIN, ItemID.IRON_JAVELIN, ItemID.STEEL_JAVELIN, ItemID.MITHRIL_JAVELIN, ItemID.ADAMANT_JAVELIN, ItemID.RUNE_JAVELIN, ItemID.AMETHYST_JAVELIN, ItemID.DRAGON_JAVELIN);
                if (javelinCount > 0) {
                    panelComponent.getChildren().add(LineComponent.builder().left("Javelins:").right(String.valueOf(javelinCount)).build());
                }
            }
            if (config.trackThrowingAxes()) {
                int axeCount = getItemCount(ItemID.BRONZE_THROWNAXE, ItemID.IRON_THROWNAXE, ItemID.STEEL_THROWNAXE, ItemID.MITHRIL_THROWNAXE, ItemID.ADAMANT_THROWNAXE, ItemID.RUNE_THROWNAXE, ItemID.DRAGON_THROWNAXE);
                if (axeCount > 0) {
                    panelComponent.getChildren().add(LineComponent.builder().left("Axes:").right(String.valueOf(axeCount)).build());
                }
            }
            if (config.trackThrowingKnives()) {
                int knifeCount = getItemCount(ItemID.BRONZE_KNIFE, ItemID.IRON_KNIFE, ItemID.STEEL_KNIFE, ItemID.MITHRIL_KNIFE, ItemID.ADAMANT_KNIFE, ItemID.RUNE_KNIFE, ItemID.BLACK_KNIFE, ItemID.DRAGON_KNIFE);
                if (knifeCount > 0) {
                    panelComponent.getChildren().add(LineComponent.builder().left("Axes:").right(String.valueOf(knifeCount)).build());
                }
            }
        }

        return panelComponent.getChildren().isEmpty() ? null : panelComponent.render(graphics);
    }
}
