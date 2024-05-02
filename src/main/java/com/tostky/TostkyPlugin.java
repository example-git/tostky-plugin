package com.tostky;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.swing.SwingUtilities;
import java.util.stream.Collectors;
import java.awt.image.BufferedImage;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.util.ImageUtil;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.GameObject;


@Slf4j
@PluginDescriptor(name = "Tostky Tracker")
public class TostkyPlugin extends Plugin {
	private TostkyPanel panel;
	private final Map<Integer, Integer> previousCounts = new HashMap<>();
	private boolean inBank;
	private boolean cannonPlaced;
	private Map<Integer, Integer> lastInventoryState = new HashMap<>();
	private long lastBankInteractionTime;
	private long lastCannonPlaceTime;

	@Inject
	private Client client;

	@Inject
	private TostkyConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private TostkyOverlay overlay;


	// Implement similar methods for isDart, isJavelin, isThrowingAxe, etc.
	// Getter methods for each ammunition count
	@Getter
	private int cannonBallCount;
	@Getter
	private int arrowCount;
	@Getter
	private int boltCount;
	@Getter
	private int dartCount;
	@Getter
	private int javelinCount;
	@Getter
	private int throwingAxeCount;
	@Getter
	private int throwingKnifeCount;
	@Getter
	private int cballsLeft;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;
	private NavigationButton navButton;

	@Override
	protected void startUp() throws Exception {
		//previousCannonballCount = client.getVar(VarPlayer.CANNON_AMMO);
		// Initialize previousCounts
		updateAmmunitionCountsFromContainer(client.getItemContainer(InventoryID.INVENTORY), previousCounts);
		updateAmmunitionCountsFromContainer(client.getItemContainer(InventoryID.EQUIPMENT), previousCounts);
		panel = injector.getInstance(TostkyPanel.class);
		overlayManager.add(overlay);
		cannonBallCount = config.cannonBallCount();
		panel.updateCannonBallCount(cannonBallCount);
		resetCounts();



		// Create a navigation button for the panel on the sidebar with an icon
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon.png"); // Ensure you have an icon.png in your resources
		navButton = NavigationButton.builder()
				.tooltip("Tostky Tracker")
				.icon(icon)
				.priority(5)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown() throws Exception {
		overlayManager.remove(overlay);
		resetCounts(); // Reset counts if that's your intended behavior on shutdown
		clientToolbar.removeNavigation(navButton);
		cannonBallCount = 0;
		configManager.setConfiguration("tostkytracker", "cannonBallCount", 0);
		panel.updateCannonBallCount(0);
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event) {
		if(event.getGroupId() == InterfaceID.BANK) {
			inBank = true;}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event) {
		if(event.getGroupId() == InterfaceID.BANK) {
			inBank = false;
			lastBankInteractionTime = System.currentTimeMillis(); }
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event) {
		GameObject gameObject = event.getGameObject();
		if ((gameObject.getId() == ObjectID.CANNON_BASE || gameObject.getId() == ObjectID.CANNON_BASE_43029) && !cannonPlaced) {
			cannonPlaced = true;
		}
	}
	private int cannonBallsLoaded = 0;
	private int previousCannonBallCount = -1;

	@Subscribe
	public void onVarbitChanged(VarbitChanged event) {
		if (event.getVarpId() == VarPlayer.CANNON_AMMO) {
			int currentCannonBallCount = event.getValue();

			if (previousCannonBallCount != -1) {
				int cannonBallsUsed = previousCannonBallCount - currentCannonBallCount;
				if (cannonBallsUsed > 0) {
					if (currentCannonBallCount == 0 && cannonBallsUsed == previousCannonBallCount) {
						// Entire ammo refunded at once
						return;
					} else {
						incrementCannonBallCount(cannonBallsUsed);
					}
				} else if (previousCannonBallCount == 1 && currentCannonBallCount == 0) {
					// Last cannonball used
					incrementCannonBallCount(1);
				}
			}

			previousCannonBallCount = currentCannonBallCount;
		}
	}





	private void incrementCannonBallCount(int amount) {
		int currentCount = config.cannonBallCount();
		currentCount += amount;
		configManager.setConfiguration("tostkytracker", "cannonBallCount", currentCount);
		panel.updateCannonBallCount(currentCount);
	}


	public void setCannonBallCount(int count) {
		configManager.setConfiguration("tostkytracker", "cannonBallCount", count);
	}

	public int getCannonBallCount() {
		return config.cannonBallCount();
	}


	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event) {
		GameObject gameObject = event.getGameObject();
		if ((gameObject.getId() == ObjectID.CANNON_BASE || gameObject.getId() == ObjectID.CANNON_BASE_43029) && cannonPlaced) {
			cannonPlaced = false;
		}
	}


	private void handleInventoryChange(ItemContainer newItemContainer) {
		Map<Integer, Integer> currentInventoryState = extractInventoryState(newItemContainer);
		boolean justBanked = System.currentTimeMillis() - lastBankInteractionTime < 2000; // 2-second threshold

		if (!justBanked) {
			// Iterate over the current inventory state
			currentInventoryState.forEach((itemId, currentCount) -> {
				int lastCount = lastInventoryState.getOrDefault(itemId, 0);

				// If the count has increased, consider it a pickup
				if (currentCount > lastCount) {
					int increment = currentCount - lastCount;
					// Logic to handle the increment, e.g., updating counts or UI
					handleItemPickup(itemId, increment);
				}
			});
		}

		// Finally, update the lastInventoryState for the next comparison
		lastInventoryState = new HashMap<>(currentInventoryState);
	}
	private void handleItemPickup(int itemId, int increment) {
		// Implement your logic here to handle item pickups
		// For example, increment tracked counts for the item
	}
	private Map<Integer, Integer> extractInventoryState(ItemContainer itemContainer) {
		Map<Integer, Integer> inventoryState = new HashMap<>();
		for (Item item : itemContainer.getItems()) {
			inventoryState.put(item.getId(), inventoryState.getOrDefault(item.getId(), 0) + item.getQuantity());
		}
		return inventoryState;
	}


	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event) {
		if (event.getContainerId() == InventoryID.INVENTORY.getId() || event.getContainerId() == InventoryID.EQUIPMENT.getId()) {
			Map<Integer, Integer> currentCounts = new HashMap<>();

			// Populate currentCounts with the current state of inventory and equipment
			updateAmmunitionCountsFromContainer(client.getItemContainer(InventoryID.INVENTORY), currentCounts);
			updateAmmunitionCountsFromContainer(client.getItemContainer(InventoryID.EQUIPMENT), currentCounts);

			// Check used items by comparing previous and current counts
			for (Map.Entry<Integer, Integer> entry : previousCounts.entrySet()) {
				int itemId = entry.getKey();
				int prevCount = entry.getValue();
				int currCount = currentCounts.getOrDefault(itemId, 0);

				if (prevCount > currCount) {
					if(inBank) {return;}
						incrementCounter(itemId, prevCount - currCount);
				}
				else if (config.countPickedUpItems() || (prevCount < currCount)) {
					if(!inBank) {
						decrementCounter(itemId, currCount - prevCount);
					}
				}
			}

			// Update previousCounts with the current state for next comparison
			previousCounts.clear();
			previousCounts.putAll(currentCounts);

			// Trigger overlay update to reflect the new counters
			overlay.update();
			panel.updateCounts();
		}
	}





	private void updateAmmunitionCountsFromContainer(ItemContainer container, Map<Integer, Integer> counts) {
		if (container != null) {
			for (Item item : container.getItems()) {
				int quantity = counts.getOrDefault(item.getId(), 0);
				counts.put(item.getId(), quantity + item.getQuantity());
			}
		}
	}

	private void incrementCounter(int itemId, int quantity) {
		if (isCannonBall(itemId)) {
			cannonBallCount += quantity;
		} else if (isArrow(itemId)) {
			arrowCount += quantity;
		} else if (isBolt(itemId)) {
			boltCount += quantity;
		} else if (isDart(itemId)) {
			dartCount += quantity;
		} else if (isJavelin(itemId)) {
			javelinCount += quantity;
		} else if (isThrowingAxe(itemId)) {
			throwingAxeCount += quantity;
		} else if (isThrowingKnife(itemId)) {
			throwingKnifeCount += quantity;
		}
		if (panel != null) {
			panel.updateCounts();
		}
		overlay.update();
	}

	private void decrementCounter(int itemId, int quantity) {
		if (isCannonBall(itemId)) {
			cannonBallCount -= quantity;
		} else if (isArrow(itemId)) {
			arrowCount -= quantity;
		} else if (isBolt(itemId)) {
			boltCount -= quantity;
		} else if (isDart(itemId)) {
			dartCount -= quantity;
		} else if (isJavelin(itemId)) {
			javelinCount -= quantity;
		} else if (isThrowingAxe(itemId)) {
			throwingAxeCount -= quantity;
		} else if (isThrowingKnife(itemId)) {
			throwingKnifeCount -= quantity;
		}
		if (panel != null) {
			panel.updateCounts();
		}
		overlay.update();

	}


		// Extend with additional checks as necessary

	private void decrementOnUsage(Map<Integer, Integer> previousCounts, Map<Integer, Integer> newCounts) {
		for (Integer itemId : previousCounts.keySet()) {
			if (newCounts.containsKey(itemId)) {
				int diff = previousCounts.get(itemId) - newCounts.get(itemId);
				if (diff > 0) {
					// Decrement the appropriate counter based on the item ID
					incrementCounter(itemId, diff);
				}
			}
		}
	}

	// This is a consolidated method to update the ammunition counts based on the item ID.
	// It performs the checks for each ammunition type as previously done directly in the onItemContainerChanged method.
	private void updateAmmunitionCounts(Item item) {
		if (config.trackCannonBalls() && isCannonBall(item.getId())) {
			cannonBallCount += item.getQuantity();
		}
		if (config.trackArrows() && isArrow(item.getId())) {
			arrowCount += item.getQuantity();
		}
		if (config.trackBolts() && isBolt(item.getId())) {
			boltCount += item.getQuantity();
		}
		if (config.trackDarts() && isDart(item.getId())) {
			dartCount += item.getQuantity();
		}
		if (config.trackJavelins() && isJavelin(item.getId())) {
			javelinCount += item.getQuantity();
		}
		if (config.trackThrowingAxes() && isThrowingAxe(item.getId())) {
			throwingAxeCount += item.getQuantity();
		}
		if (config.trackThrowingKnives() && isThrowingKnife(item.getId())) {
			throwingKnifeCount += item.getQuantity();
		}
	}

	private void resetCounts() {
		cannonBallCount = 0;
		arrowCount = 0;
		boltCount = 0;
		dartCount = 0;
		javelinCount = 0;
		throwingAxeCount = 0;
		// Reset other counts as necessary

		// Clear previousCounts to reset state
		previousCounts.clear();

		// Optionally, reinitialize previousCounts to match the current
		// inventory state if you want to immediately start tracking after a reset.
		// Otherwise, previousCounts will naturally repopulate on the next
		// onItemContainerChanged event.
		updateAmmunitionCountsFromContainer(client.getItemContainer(InventoryID.INVENTORY), previousCounts);
		updateAmmunitionCountsFromContainer(client.getItemContainer(InventoryID.EQUIPMENT), previousCounts);

		overlay.update();
		panel.updateCounts();
	}

	@Inject
	private ConfigManager configManager;

	// ...

	// Example for resetting cannon ball count manually
	void resetPersistCannon() {
		configManager.setConfiguration("tostkytracker", "cannonBallCount", 0);
		cannonBallCount = 0;

		// Directly remove Cannonballs count from previousCounts using its specific item ID
		previousCounts.remove(2);  // Cannonballs item ID

		if (panel != null) panel.updateCounts();
		overlay.update();
	}

	void resetPersistArrow() {
		configManager.setConfiguration("tostkytracker", "arrowCount", 0);
		arrowCount = 0;

		// Collecting arrow item IDs to remove based on our ID filtering logic
		Set<Integer> arrowIdsToRemove = previousCounts.keySet().stream()
				.filter(this::isArrow)
				.collect(Collectors.toSet());

		// Removing the IDs from previousCounts
		arrowIdsToRemove.forEach(previousCounts::remove);

		if (panel != null) panel.updateCounts();
		overlay.update();
	}

	void resetPersistBolt() {
		configManager.setConfiguration("tostkytracker", "boltCount", 0);
		boltCount = 0;

		// Similar procedure for Bolts
		Set<Integer> boltIdsToRemove = previousCounts.keySet().stream()
				.filter(this::isBolt)
				.collect(Collectors.toSet());

		boltIdsToRemove.forEach(previousCounts::remove);

		if (panel != null) panel.updateCounts();
		overlay.update();
	}

	void resetPersistDart() {
		configManager.setConfiguration("tostkytracker", "dartCount", 0);
		dartCount = 0;

		// And for Darts
		Set<Integer> dartIdsToRemove = previousCounts.keySet().stream()
				.filter(this::isDart)
				.collect(Collectors.toSet());

		dartIdsToRemove.forEach(previousCounts::remove);

		if (panel != null) panel.updateCounts();
		overlay.update();
	}

	void resetPersistJavelin() {
		configManager.setConfiguration("tostkytracker", "javelinCount", 0);
		javelinCount = 0;

		// Following the pattern for Javelins
		Set<Integer> javelinIdsToRemove = previousCounts.keySet().stream()
				.filter(this::isJavelin)
				.collect(Collectors.toSet());

		javelinIdsToRemove.forEach(previousCounts::remove);

		if (panel != null) panel.updateCounts();
		overlay.update();
	}

	void resetPersistThrowingAxe() {
		configManager.setConfiguration("tostkytracker", "throwingAxeCount", 0);
		throwingAxeCount = 0;

		// Lastly, for Throwing Axes
		Set<Integer> throwingAxeIdsToRemove = previousCounts.keySet().stream()
				.filter(this::isThrowingAxe)
				.collect(Collectors.toSet());

		throwingAxeIdsToRemove.forEach(previousCounts::remove);

		if (panel != null) panel.updateCounts();
		overlay.update();
	}

	void resetPersistThrowingKnife() {
		configManager.setConfiguration("tostkytracker", "throwingKnifeCount", 0);
		throwingAxeCount = 0;

		// Lastly, for Throwing Axes
		Set<Integer> throwingKnifeIdsToRemove = previousCounts.keySet().stream()
				.filter(this::isThrowingKnife)
				.collect(Collectors.toSet());

		throwingKnifeIdsToRemove.forEach(previousCounts::remove);

		if (panel != null) panel.updateCounts();
		overlay.update();
	}

	private boolean isCannonBall(int itemId) {
		// Placeholder: Replace with actual item ID
		return itemId == ItemID.CANNON_BALL; // Use the actual ItemID for Cannonballs
	}

	private boolean isArrow(int itemId) {
		// Example: Replace with actual range or IDs
		return itemId == ItemID.BRONZE_ARROW || itemId == ItemID.IRON_ARROW || itemId == ItemID.STEEL_ARROW || itemId == ItemID.MITHRIL_ARROW || itemId == ItemID.ADAMANT_ARROW || itemId == ItemID.RUNE_ARROW || itemId == ItemID.AMETHYST_ARROW || itemId == ItemID.DRAGON_ARROW || itemId == ItemID.BROAD_ARROWS;
	}

	private boolean isBolt(int itemId) {
		// Example: Replace with actual range or IDs
		return itemId == ItemID.BRONZE_BOLTS || itemId == ItemID.IRON_BOLTS || itemId == ItemID.STEEL_BOLTS || itemId == ItemID.MITHRIL_BOLTS || itemId == ItemID.ADAMANT_BOLTS || itemId == ItemID.RUNITE_BOLTS || itemId == ItemID.RUNITE_BOLTS_P || itemId == ItemID.DRAGON_BOLTS || itemId == ItemID.AMETHYST_BROAD_BOLTS || itemId == ItemID.RUBY_BOLTS_E || itemId == ItemID.EMERALD_BOLTS_E || itemId == ItemID.DIAMOND_BOLTS_E || itemId == ItemID.ONYX_BOLTS_E || itemId == ItemID.BROAD_BOLTS;
	}

	private boolean isDart(int itemId) {
		// Example: Replace with actual range or IDs
		return itemId == ItemID.BRONZE_DART || itemId == ItemID.IRON_DART || itemId == ItemID.STEEL_DART || itemId == ItemID.MITHRIL_DART || itemId == ItemID.ADAMANT_DART || itemId == ItemID.RUNE_DART || itemId == ItemID.DRAGON_DART || itemId == ItemID.AMETHYST_DART;
	}

	private boolean isJavelin(int itemId) {
		// Example: Replace with actual range or IDs
		return itemId == ItemID.BRONZE_JAVELIN || itemId == ItemID.IRON_JAVELIN || itemId == ItemID.STEEL_JAVELIN || itemId == ItemID.MITHRIL_JAVELIN || itemId == ItemID.ADAMANT_JAVELIN || itemId == ItemID.RUNE_JAVELIN || itemId == ItemID.AMETHYST_JAVELIN || itemId == ItemID.DRAGON_JAVELIN;
	}

	private boolean isThrowingAxe(int itemId) {
		// Example: Replace with actual range or IDs
		return itemId == ItemID.BRONZE_THROWNAXE || itemId == ItemID.IRON_THROWNAXE || itemId == ItemID.STEEL_THROWNAXE || itemId == ItemID.MITHRIL_THROWNAXE || itemId == ItemID.ADAMANT_THROWNAXE || itemId == ItemID.RUNE_THROWNAXE || itemId == ItemID.DRAGON_THROWNAXE;
	}

	private boolean isThrowingKnife(int itemId) {
		return itemId == ItemID.BRONZE_KNIFE || itemId == ItemID.IRON_KNIFE || itemId == ItemID.STEEL_KNIFE || itemId == ItemID.MITHRIL_KNIFE || itemId == ItemID.ADAMANT_KNIFE || itemId == ItemID.RUNE_KNIFE || itemId == ItemID.BLACK_KNIFE || itemId == ItemID.DRAGON_KNIFE;
	}
	void resetCannonBallCount() {
		cannonBallCount = 0;
		configManager.setConfiguration("tostkytracker", "cannonBallCount", 0);
		panel.updateCannonBallCount(0);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if (event.getGroup().equals("tostkytracker")) {
			switch (event.getKey()) {
				case "trackCannonBalls":
					if (!config.trackCannonBalls()) {
						cannonBallCount = 0;
						configManager.setConfiguration("tostkytracker", "cannonBallCount", 0);
					}
					break;
				case "trackArrows":
					if (!config.trackArrows()) {
						arrowCount = 0;
						configManager.setConfiguration("tostkytracker", "arrowCount", 0);
					}
					break;
				case "trackBolts":
					if (!config.trackBolts()) {
						boltCount = 0;
						configManager.setConfiguration("tostkytracker", "boltCount", 0);
					}
					break;
				case "trackDarts":
					if (!config.trackDarts()) {
						dartCount = 0;
						configManager.setConfiguration("tostkytracker", "dartCount", 0);
					}
					break;
				case "trackJavelins":
					if (!config.trackJavelins()) {
						javelinCount = 0;
						configManager.setConfiguration("tostkytracker", "javelinCount", 0);
					}
					break;
				case "trackThrowingAxes":
					if (!config.trackThrowingAxes()) {
						throwingAxeCount = 0;
						configManager.setConfiguration("tostkytracker", "throwingAxeCount", 0);
					}
					break;
				case "trackThrowingKnives":
					if (!config.trackThrowingKnives()) {
						throwingAxeCount = 0;
						configManager.setConfiguration("tostkytracker", "throwingKnifeCount", 0);
					}
					break;
			}
			overlay.update();
			panel.updateCounts();
		}

	}

	@Provides
	TostkyConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(TostkyConfig.class);
	}


}
