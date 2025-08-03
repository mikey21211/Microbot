package net.runelite.client.plugins.microbot.birdhouseruns;

import net.runelite.api.gameval.ItemID;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.Notifier;
import net.runelite.client.config.Notification;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.bankjs.BanksBankStander.CurrentStatus;
import net.runelite.client.plugins.microbot.birdhouseruns.FornBirdhouseRunsInfo.states;
import net.runelite.client.plugins.microbot.util.Rs2InventorySetup;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.InteractOrder;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import javax.inject.Inject;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.runelite.client.plugins.microbot.birdhouseruns.FornBirdhouseRunsInfo.*;
import static net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory.calculateInteractOrder;

public class FornBirdhouseRunsScript extends Script {
    private static final WorldPoint birdhouseLocation1 = new WorldPoint(3763, 3755, 0);
    private static final WorldPoint birdhouseLocation2 = new WorldPoint(3768, 3761, 0);
    private static final WorldPoint birdhouseLocation3 = new WorldPoint(3677, 3882, 0);
    private static final WorldPoint birdhouseLocation4 = new WorldPoint(3679, 3815, 0);
    public static double version = 1.0;
    private boolean initialized;
    @Inject
    private Notifier notifier;
    private final FornBirdhouseRunsPlugin plugin;
    private final FornBirdhouseRunsConfig config;

    public static long previousItemChange;

    public static CurrentStatus currentStatus = CurrentStatus.FETCH_SUPPLIES;

    public static int itemsProcessed;

    static Integer thirdItemId;
    static Integer fourthItemId;


    static Integer firstItemId;
    public static Integer secondItemId;

    public static boolean isWaitingForPrompt = false;
    private long timeValue;
    private int randomNum;

    @Inject
    FornBirdhouseRunsScript(FornBirdhouseRunsPlugin plugin, FornBirdhouseRunsConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public boolean run() {
        Microbot.enableAutoRunOn = true;
        botStatus = states.GEARING;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
             
                if (!initialized) {
                    if (Rs2Player.getQuestState(Quest.BONE_VOYAGE) != QuestState.FINISHED) {
                        Microbot.log("You need to finish the quest 'BONE VOYAGE' to use this script");
                        plugin.reportFinished("Birdhouse run failed, you need to finish the quest 'BONE VOYAGE'",false);
                        this.shutdown();
                        return;
                    }
                    initialized = true;
                    
                    boolean hasInventorySetup =  config.inventorySetup()!= null && Rs2InventorySetup.isInventorySetup(config.inventorySetup().getName());
                    if (hasInventorySetup) {
                        var inventorySetup = new Rs2InventorySetup(config.inventorySetup(), mainScheduledFuture);
                        if (!inventorySetup.doesInventoryMatch() || !inventorySetup.doesEquipmentMatch()) {
                            Rs2Walker.walkTo(Rs2Bank.getNearestBank().getWorldPoint(), 20);
                            if (!inventorySetup.loadEquipment() || !inventorySetup.loadInventory()) {
                                Microbot.log("Failed to load inventory setup");
                                plugin.reportFinished("Birdhouse run failed to load inventory setup",false);                                                        
                                this.shutdown();
                                return;
                            }
                            if (Rs2Bank.isOpen()) Rs2Bank.closeBank();
                        }
                    }else{
                        Microbot.log("Failed to load inventory, inventory setup not found:"+ config.inventorySetup());
                        plugin.reportFinished("Birdhouse run failed to load inventory setup",false);                                                        
                        this.shutdown();
                        return;
                    }
                    botStatus = states.TELEPORTING;
                }
                if (!super.run()) return;

                switch (botStatus) {
                    case TELEPORTING:
                        Rs2Walker.walkTo(new WorldPoint(3764, 3869, 1), 5);
                        botStatus = states.VERDANT_TELEPORT;
                        break;
                    case VERDANT_TELEPORT:
                        interactWithObject(30920);
                        sleepUntil(() -> Rs2Widget.findWidget("Mycelium Transportation System") != null);
                        Rs2Widget.clickWidget(39845895);
                        sleepUntil(() -> Rs2Player.distanceTo(birdhouseLocation1) < 20);
                        botStatus = states.DISMANTLE_HOUSE_1;
                        break;
                    case DISMANTLE_HOUSE_1:
                        dismantleBirdhouse(30568, states.BUILD_HOUSE_1);
                        break;
                    case BUILD_HOUSE_1:
                        buildBirdhouse(birdhouseLocation1, states.SEED_HOUSE_1);
                        break;
                    case SEED_HOUSE_1:
                        seedHouse(birdhouseLocation1, states.DISMANTLE_HOUSE_2);
                    case DISMANTLE_HOUSE_2:
                        dismantleBirdhouse(30567, states.BUILD_HOUSE_2);
                        break;
                    case BUILD_HOUSE_2:
                        buildBirdhouse(birdhouseLocation2, states.SEED_HOUSE_2);
                        break;
                    case SEED_HOUSE_2:
                        seedHouse(birdhouseLocation2, states.MUSHROOM_TELEPORT);
                        break;
                    case MUSHROOM_TELEPORT:
                        interactWithObject(30924);
                        sleepUntil(() -> Rs2Widget.findWidget("Mycelium Transportation System") != null);
                        Rs2Widget.clickWidget(39845903);
                        sleepUntil(() -> Rs2Player.distanceTo(birdhouseLocation3) < 20);
                        botStatus = states.DISMANTLE_HOUSE_3;
                        break;
                    case DISMANTLE_HOUSE_3:
                        dismantleBirdhouse(30565, states.BUILD_HOUSE_3);
                        break;
                    case BUILD_HOUSE_3:
                        buildBirdhouse(birdhouseLocation3, states.SEED_HOUSE_3);
                        break;
                    case SEED_HOUSE_3:
                        seedHouse(birdhouseLocation3, states.DISMANTLE_HOUSE_4);
                        break;
                    case DISMANTLE_HOUSE_4:
                        Rs2Walker.walkTo(new WorldPoint(3680, 3813, 0));
                        dismantleBirdhouse(30566, states.BUILD_HOUSE_4);
                        break;
                    case BUILD_HOUSE_4:
                        buildBirdhouse(birdhouseLocation4, states.SEED_HOUSE_4);
                        break;
                    case SEED_HOUSE_4:
                        seedHouse(birdhouseLocation4, states.FINISHING);
                        break;
                    case FINISHING:
                        if (config.goToBank()) {
                            Rs2Walker.walkTo(Rs2Bank.getNearestBank().getWorldPoint());
                            emptyNests();
                            clampedSleepGaussian(760, 222);
                            crushBirdNests();
                            if (!Rs2Bank.isOpen()) Rs2Bank.openBank();
                            Rs2Bank.depositAll();
                        }

                        botStatus = states.FINISHED;
                        notifier.notify(Notification.ON, "Birdhouse run is finished.");
                        plugin.reportFinished("Birdhouse run finished",true);
                        this.shutdown();
                        break;
                    case FINISHED:

                }

            } catch (Exception ex) {
                Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    private void emptyNests() {
        var ids = List.of(
                ItemID.BIRD_NEST_EGG_RED,
                ItemID.BIRD_NEST_EGG_GREEN,
                ItemID.BIRD_NEST_EGG_BLUE,
                ItemID.BIRD_NEST_SEEDS,
                ItemID.BIRD_NEST_RING,
                ItemID.BIRD_NEST_SEEDS_JAN2019,
                ItemID.BIRD_NEST_DECENTSEEDS_JAN2019
        );

        Rs2Inventory.items().forEachOrdered(item -> {
            if (ids.contains(item.getId())) {
                Rs2Inventory.interact(item, "Search");
            }
        });
    }

    @Override
    public void shutdown() {
        super.shutdown();
        initialized = false;
        botStatus = states.TELEPORTING;
    }

    private boolean interactWithObject(int objectId) {
        Rs2GameObject.interact(objectId);
        sleepUntil(Rs2Player::isInteracting);
        sleepUntil(() -> !Rs2Player.isInteracting());
        return true;
    }

    private void seedHouse(WorldPoint worldPoint, states status) {
        Rs2Inventory.use(" seed");
        sleepUntil(Rs2Inventory::isItemSelected);
        Rs2GameObject.interact(worldPoint);
        sleepUntil(() -> Rs2Widget.findWidget("full of seed") != null, 1000);
        botStatus = status;
    }

    private void buildBirdhouse(WorldPoint worldPoint, states status) {
        if (!Rs2Inventory.hasItem("bird house") && Rs2Inventory.hasItem(ItemID.POH_CLOCKWORK_MECHANISM)
                && Rs2Inventory.hasItem(" logs"))
        {
            //Rs2Inventory.use(ItemID.HAMMER);
            //Rs2Inventory.use(" logs");
            Rs2GameObject.interact(worldPoint, "Build");
            Rs2Inventory.waitForInventoryChanges(5000);
            //sleepUntil(Rs2Player::isAnimating);
            botStatus = status;
        }
    }

    private void dismantleBirdhouse(int itemId, states status) {
        Rs2GameObject.interact(itemId, "Empty");
        Rs2Player.waitForXpDrop(Skill.HUNTER);
        botStatus = status;
    }

    private void crushBirdNests() {
        if(Rs2Inventory.contains(ItemID.PESTLE_AND_MORTAR) && Rs2Inventory.contains(ItemID.BIRD_NEST_EMPTY) && Rs2Inventory.count(ItemID.PESTLE_AND_MORTAR) == 1)
        {

            //Get pestle/mortar and move to first open slot in inventory
            Rs2ItemModel pestleMortar = Rs2Inventory.get(ItemID.PESTLE_AND_MORTAR);

            //Waits in a loop until bird nests are no longer in inventory, times out after 30 seconds
            if (Rs2Inventory.hasItem(5075) && Rs2Inventory.hasItem(233)) {
                Rs2Inventory.combineClosest(5075, 233); // Start the grind process
                Rs2Player.waitForAnimation(800);        // Wait for animation to begin

                long startTime = System.currentTimeMillis();
                long timeout = 30000; // 30 seconds

                while (Rs2Inventory.hasItem(5075) && System.currentTimeMillis() - startTime < timeout) {
                    sleep(500); // Check every 0.5 seconds
                }

                clampedSleepGaussian(430, 180);
            }

                //int emptySlot = Rs2Inventory.getFirstEmptySlot();

            /*if (pestleMortar != null && emptySlot != -1) {
                Rs2Inventory.moveItemToSlot(pestleMortar, emptySlot);
            } else {
                Microbot.log("Pestle and Mortar not found or no empty slot available.");
            }*/

            /*
            //Random choice how to tackle inventory
            InteractOrder interactOrderLocal;
            int randomValue = Rs2Random.betweenInclusive(0, 5);
            if (randomValue == 0) {
                interactOrderLocal = InteractOrder.EFFICIENT_ROW;
            } else if (randomValue == 1) {
                interactOrderLocal = InteractOrder.COLUMN;
            } else if (randomValue == 2) {
                interactOrderLocal = InteractOrder.EFFICIENT_COLUMN;
            } else if (randomValue == 3) {
                interactOrderLocal = InteractOrder.ZIGZAG;
            } else if (randomValue == 4) {
                interactOrderLocal = InteractOrder.STANDARD;
            } else { // Covers randomValue == 5 and any other unexpected values due to 'default' in original
                interactOrderLocal = InteractOrder.RANDOM;
            }

            List<Rs2ItemModel> inventoryNests = calculateInteractOrder(Rs2Inventory.items(x -> x.getName().toLowerCase().contains("bird"))
                    .collect(Collectors.toList()), interactOrderLocal);

            if (inventoryNests.isEmpty()) {
                Microbot.log("No nests found in inventory.");
                return;
            }

            // Interact with each slot in the specified order
            for (Rs2ItemModel item : inventoryNests) {

                //Set baseline time, click 'Use' on pestle and mortar
                timeValue = System.currentTimeMillis();
                Rs2Inventory.interact(pestleMortar, "Use");
                randomNum = calculateSleepDuration(0.5);
                if (System.currentTimeMillis()-timeValue<randomNum)
                {
                    sleep((int) (randomNum-(System.currentTimeMillis()-timeValue)));
                }
                else { sleep(Rs2Random.between(14, 28)); }

                //Set baseline time, click 'Use' on nests
                timeValue = System.currentTimeMillis();
                Rs2Inventory.interact(item, "Use");
                randomNum = calculateSleepDuration(0.5);
                if (System.currentTimeMillis()-timeValue<randomNum)
                {
                    sleep((int) (randomNum-(System.currentTimeMillis()-timeValue)));
                }*/

                //else { sleep(Rs2Random.between(14, 28)); }
            }
    }

    private int calculateSleepDuration(double multiplier) {
        // Create a Random object
        Random random = new Random();

        // Calculate the mean (average) of sleepMin and sleepMax, adjusted by sleepTarget
        int sleepMin = 58;
        int sleepMax = 1200;
        int sleepTarget = 440;

        double mean = (sleepMin + sleepMax + sleepTarget) / 3.0;

        // Calculate the standard deviation with added noise
        double noiseFactor = 0.2; // Adjust the noise factor as needed (0.0 to 1.0)
        double stdDeviation = Math.abs(sleepTarget - mean) / 3.0 * (1 + noiseFactor * (random.nextDouble() - 0.5) * 2);

        // Generate a random number following a normal distribution
        int sleepDuration;
        do {
            // Generate a random number using nextGaussian method, scaled by standard deviation
            sleepDuration = (int) Math.round(mean + random.nextGaussian() * stdDeviation);
        } while (sleepDuration < sleepMin || sleepDuration > sleepMax); // Ensure the duration is within the specified range
        if ((int) Math.round(sleepDuration * multiplier) < 60) sleepDuration += ((60-sleepDuration)+Rs2Random.between(11,44));
        return sleepDuration;
    }
}
