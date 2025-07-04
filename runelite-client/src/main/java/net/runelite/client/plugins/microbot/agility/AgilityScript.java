package net.runelite.client.plugins.microbot.agility;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Inject;
import net.runelite.api.Skill;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.agility.AgilityPlugin;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.agility.courses.GnomeStrongholdCourse;
import net.runelite.client.plugins.microbot.agility.courses.PrifddinasCourse;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.camera.Rs2Camera;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.grounditem.Rs2GroundItem;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.models.RS2Item;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

public class AgilityScript extends Script
{

	public static String version = "1.2.0";
	final MicroAgilityPlugin plugin;
	final MicroAgilityConfig config;

	WorldPoint startPoint = null;
	WorldPoint centerPoint = null;

	boolean animatingCameraChangedFlag;
	boolean otherCameraChangedFlag;

	int previousObstacle;
	int currentObstacle;

	@Inject
	public AgilityScript(MicroAgilityPlugin plugin, MicroAgilityConfig config)
	{
		this.plugin = plugin;
		this.config = config;
	}

	public boolean run()
	{
		Microbot.enableAutoRunOn = true;
		Rs2Antiban.resetAntibanSettings();
		Rs2Antiban.antibanSetupTemplates.applyAgilitySetup();
		startPoint = plugin.getCourseHandler().getStartPoint();
		centerPoint = plugin.getCourseHandler().getCenterPoint();
		animatingCameraChangedFlag = false;
		//previousObstacle = -1;
		//currentObstacle = -1;

		mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
			try
			{
				if (!Microbot.isLoggedIn())
				{
					return;
				}
				if (!super.run())
				{
					return;
				}
				if (Rs2AntibanSettings.actionCooldownActive)
				{
					return;
				}
				if (startPoint == null)
				{
					Microbot.showMessage("Agility course: " + config.agilityCourse().getTooltip() + " is not supported.");
					sleep(10000);
					return;
				}

				final LocalPoint playerLocation = Microbot.getClient().getLocalPlayer().getLocalLocation();
				final WorldPoint playerWorldLocation = Microbot.getClient().getLocalPlayer().getWorldLocation();

				if (handleFood())
				{
					return;
				}
				if (handleSummerPies())
				{
					return;
				}

				WorldPoint courseCenter = plugin.getCourseHandler().getCenterPoint();

				if (plugin.getCourseHandler().getCurrentObstacleIndex() != 0)
				{
					if (Rs2Player.isMoving() || Rs2Player.isAnimating())
					{
						if(!animatingCameraChangedFlag)
						{
							Microbot.log("Changed During Animation!");
							//Chance to allow the camera change pitch/yaw.
							//Rotate the screen based on random chance
							if(Rs2Random.dicePercentage(28)) { rotateToTargetAngleWithJitter(courseCenter); }
							//Change the screen height based on random chance
							if(Rs2Random.dicePercentage(8)) { rotateCameraPitchWithJitter(); }
							animatingCameraChangedFlag = true;
						}
						return;
					}
				}

				if (lootMarksOfGrace())
				{
					return;
				}

				if (plugin.getCourseHandler() instanceof PrifddinasCourse)
				{
					PrifddinasCourse course = (PrifddinasCourse) plugin.getCourseHandler();
					if (course.handlePortal())
					{
						return;
					}

					if (course.handleWalkToStart(playerWorldLocation, playerLocation))
					{
						return;
					}
				}
				else if (!(plugin.getCourseHandler() instanceof GnomeStrongholdCourse))
				{
					if (plugin.getCourseHandler().handleWalkToStart(playerWorldLocation, playerLocation))
					{
						return;
					}
				}

				final int agilityExp = Microbot.getClient().getSkillExperience(Skill.AGILITY);

				TileObject gameObject = plugin.getCourseHandler().getCurrentObstacle();

				if (gameObject == null)
				{
					//Microbot.log("No agility obstacle found. Report this as a bug if this keeps happening.");
					return;
				}

				if (!Rs2Camera.isTileOnScreen(gameObject))
				{
					Rs2Walker.walkMiniMap(gameObject.getWorldLocation());
					//If tile is not on the screen, high chance to rotate
					if(Rs2Random.dicePercentage(47)) { rotateToTargetAngleWithJitter(gameObject.getWorldLocation()); }
				}

				//***********************Potentially random rotation again, and also enable flag
				if(!otherCameraChangedFlag)
				{
					//Chance to allow the camera change pitch/yaw.
					//Rotate the screen based on random chance
					if(Rs2Random.dicePercentage(12)) { rotateToTargetAngleWithJitter(courseCenter); }
					//Change the screen height based on random chance
					if(Rs2Random.dicePercentage(4)) { rotateCameraPitchWithJitter(); }
					otherCameraChangedFlag = true;
				}

				if (Rs2GameObject.interact(gameObject))
				{
					plugin.getCourseHandler().waitForCompletion(agilityExp, Microbot.getClient().getLocalPlayer().getWorldLocation().getPlane());
					Rs2Antiban.actionCooldown();
					Rs2Antiban.takeMicroBreakByChance();

					//Reset flags, allow yaw/pitch to be changed again
					animatingCameraChangedFlag = false;
					otherCameraChangedFlag = false;
				}

			}
			catch (Exception ex)
			{
				Microbot.log("An error occurred: " + ex.getMessage(), ex);
			}
		}, 0, 100, TimeUnit.MILLISECONDS);
		return true;
	}

	/**
	 * Rotate the screen, centered on the center of the agility course or similar.
	 *
	 * @param target The center of the agility course
	 */
	public void rotateToTargetAngleWithJitter(WorldPoint target) {

		// Calculate yaw (left/right)
		int baseYaw = Rs2Camera.angleToTile(target);
		int correctedYaw = (baseYaw - 90 + 360) % 360;

		// Add Gaussian overshoot/undershoot
		int yawJitter = (int) Rs2Random.gaussRand(0, 30); // 60° std dev
		int targetYaw = (correctedYaw + yawJitter + 360) % 360;

		//Clamp target yaw
		targetYaw = Math.max(0, Math.min(2048, targetYaw));

		// Rotate camera yaw
		Rs2Camera.setAngle(targetYaw, 10); // 0° threshold for stopping
	}

	/**
	 * Rotate the screen pitch, with randomness.
	 */
	public void rotateCameraPitchWithJitter() {

		// Calculate random pitch
		int rawPitch = (int) Rs2Random.gaussRand(266, 38);
		int clampedPitch = Math.max(128, Math.min(383, rawPitch));
		float percentage = (float) (clampedPitch - 128) / (383 - 128);

		//Rotate camera pitch
		Rs2Camera.adjustPitch(percentage);
	}

	public void handleAlch()
	{
		scheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
			if (!config.alchemy())
			{
				return;
			}
			if (plugin.getCourseHandler().getCurrentObstacleIndex() != 0)
			{
				if (Rs2Player.isMoving() || Rs2Player.isAnimating())
				{
					return;
				}
			}

			getAlchItem().ifPresent(item -> Rs2Magic.alch(item, 50, 75));
		}, 0, 300, TimeUnit.MILLISECONDS);
	}

	@Override
	public void shutdown()
	{
		super.shutdown();
	}

	private Optional<String> getAlchItem()
	{
		String itemsInput = config.itemsToAlch().trim();
		if (itemsInput.isEmpty())
		{
			Microbot.log("No items specified for alching or none available.");
			return Optional.empty();
		}

		List<String> itemsToAlch = Arrays.stream(itemsInput.split(","))
			.map(String::trim)
			.map(String::toLowerCase)
			.filter(s -> !s.isEmpty())
			.collect(Collectors.toList());

		if (itemsToAlch.isEmpty())
		{
			Microbot.log("No valid items specified for alching.");
			return Optional.empty();
		}

		for (String itemName : itemsToAlch)
		{
			if (Rs2Inventory.hasItem(itemName))
			{
				return Optional.of(itemName);
			}
		}

		return Optional.empty();
	}

	private boolean lootMarksOfGrace()
	{
		final List<RS2Item> marksOfGrace = AgilityPlugin.getMarksOfGrace();
		if (!marksOfGrace.isEmpty() && !Rs2Inventory.isFull())
		{
			for (RS2Item markOfGraceTile : marksOfGrace)
			{
				if (Microbot.getClient().getTopLevelWorldView().getPlane() != markOfGraceTile.getTile().getPlane())
				{
					continue;
				}
				if (!Rs2GameObject.canReach(markOfGraceTile.getTile().getWorldLocation()))
				{
					continue;
				}
				Rs2GroundItem.loot(markOfGraceTile.getItem().getId());
				Rs2Player.waitForWalking();
				return true;
			}
		}
		return false;
	}

	private boolean handleFood()
	{
		if (Rs2Player.getHealthPercentage() > config.hitpoints())
		{
			return false;
		}

		List<Rs2ItemModel> foodItems = plugin.getInventoryFood();
		if (foodItems.isEmpty())
		{
			return false;
		}
		Rs2ItemModel foodItem = foodItems.get(0);

		Rs2Inventory.interact(foodItem, foodItem.getName().toLowerCase().contains("jug of wine") ? "drink" : "eat");
		Rs2Inventory.waitForInventoryChanges(1800);

		if (Rs2Inventory.contains(ItemID.JUG_EMPTY))
		{
			Rs2Inventory.dropAll(ItemID.JUG_EMPTY);
		}
		return true;
	}

	private boolean handleSummerPies()
	{
		if (plugin.getCourseHandler().getCurrentObstacleIndex() != 0)
		{
			return false;
		}
		if (Rs2Player.getBoostedSkillLevel(Skill.AGILITY) >= (Rs2Player.getRealSkillLevel(Skill.AGILITY) + config.pieThreshold()))
		{
			return false;
		}

		List<Rs2ItemModel> summerPies = plugin.getSummerPies();
		if (summerPies.isEmpty())
		{
			return false;
		}
		Rs2ItemModel summerPie = summerPies.get(0);

		Rs2Inventory.interact(summerPie, "eat");
		Rs2Inventory.waitForInventoryChanges(1800);
		if (Rs2Inventory.contains(ItemID.PIEDISH))
		{
			Rs2Inventory.dropAll(ItemID.PIEDISH);
		}
		return true;
	}

}