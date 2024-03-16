package com.tostky;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("tostkytracker")
public interface TostkyConfig extends Config
{
	@ConfigItem(
			keyName = "trackCannonBalls",
			name = "Track Cannon Balls",
			description = "Enable tracking of cannon balls"
	)
	default boolean trackCannonBalls()
	{
		return true;
	}
	@ConfigItem(
			keyName = "trackArrows",
			name = "Track Arrows",
			description = "Enable tracking of arrows"
	)
	default boolean trackArrows()
	{
		return true;
	}

	@ConfigItem(
			keyName = "trackBolts",
			name = "Track Bolts",
			description = "Enable tracking of bolts"
	)
	default boolean trackBolts()
	{
		return true;
	}

	@ConfigItem(
			keyName = "trackDarts",
			name = "Track Darts",
			description = "Enable tracking of darts"
	)
	default boolean trackDarts()
	{
		return true;
	}

	@ConfigItem(
			keyName = "trackJavelins",
			name = "Track Javelins",
			description = "Enable tracking of javelins"
	)
	default boolean trackJavelins()
	{
		return true;
	}

	@ConfigItem(
			keyName = "trackThrowingAxes",
			name = "Track Throwing Axes",
			description = "Enable tracking of throwing axes"
	)
	default boolean trackThrowingAxes()
	{
		return true;
	}
	@ConfigItem(
			keyName = "trackThrowingKnives",
			name = "Track Throwing Axes",
			description = "Enable tracking of throwing axes"
	)
	default boolean trackThrowingKnives()
	{
		return true;
	}
	@ConfigItem(
			keyName = "cannonBallCount",
			name = "Cannon Ball Count",
			description = "The current count of cannonballs used",
			hidden = true
	)
	default int cannonBallCount() {
		return 0;
	}

	default void cannonBallCount(int count) { }

	@ConfigItem(
			keyName = "arrowCount",
			name = "Arrow Count",
			description = "The current count of arrows used",
			hidden = true
	)
	default int arrowCount() {
		return 0;
	}

	default void arrowCount(int count) { }

	@ConfigItem(
			keyName = "boltCount",
			name = "Bolt Count",
			description = "The current count of bolts used",
			hidden = true
	)
	default int boltCount() {
		return 0;
	}

	default void boltCount(int count) { }

	@ConfigItem(
			keyName = "dartCount",
			name = "Dart Count",
			description = "The current count of darts used",
			hidden = true
	)
	default int dartCount() {
		return 0;
	}

	default void dartCount(int count) { }

	@ConfigItem(
			keyName = "javelinCount",
			name = "Javelin Count",
			description = "The current count of javelins used",
			hidden = true
	)
	default int javelinCount() {
		return 0;
	}

	default void javelinCount(int count) { }

	@ConfigItem(
			keyName = "throwingAxeCount",
			name = "Throwing Axe Count",
			description = "The current count of throwing axes used",
			hidden = true
	)
	default int throwingAxeCount() {
		return 0;
	}

	default void throwingKnifeCount(int count) { }
	@ConfigItem(
			keyName = " ",
			name = "Throwing Knife Count",
			description = "The current count of throwing knives used",
			hidden = true
	)
	default int throwingKnifeCount() {
		return 0;
	}
	@ConfigItem(
			keyName = "countPickedUpItems",
			name = "Count Picked Up Items",
			description = "Toggle counting of items picked back up towards on the panel"
	)
	default boolean countPickedUpItems() {
		return true; // By default, this setting is enabled
	}

}
	// Add further configuration for different types of ammunition as required.