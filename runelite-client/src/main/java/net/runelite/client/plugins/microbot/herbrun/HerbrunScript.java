package net.runelite.client.plugins.microbot.herbrun;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.questhelper.helpers.mischelpers.farmruns.FarmingHandler;
import net.runelite.client.plugins.microbot.questhelper.helpers.mischelpers.farmruns.FarmingPatch;
import net.runelite.client.plugins.microbot.questhelper.helpers.mischelpers.farmruns.FarmingWorld;
import net.runelite.client.plugins.microbot.util.Rs2InventorySetup;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.InteractOrder;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.timetracking.Tab;
import net.runelite.client.plugins.microbot.questhelper.helpers.mischelpers.farmruns.CropState;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Inject;

import static net.runelite.client.plugins.microbot.Microbot.log;
import static net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory.calculateInteractOrder;

@Slf4j
public class HerbrunScript extends Script {
    @Inject
    private ConfigManager configManager;
    @Inject
    private FarmingWorld farmingWorld;
    private FarmingHandler farmingHandler;
    private final HerbrunPlugin plugin;
    private final HerbrunConfig config;
    private HerbPatch currentPatch;
    @Inject
    ClientThread clientThread;
    private boolean initialized = false;

    private long timeValue;
    private int randomNum;

    @Inject
    public HerbrunScript(HerbrunPlugin plugin, HerbrunConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    private final List<HerbPatch> herbPatches = new ArrayList<>();

    public boolean run() {        
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            if (!Microbot.isLoggedIn()) return;
            if (!super.run()) return;
            if (!initialized) {
                initialized = true;
                HerbrunPlugin.status = "Gearing up";
                populateHerbPatches();
                if (herbPatches.isEmpty()) {                                        
                    plugin.reportFinished("No herb patches ready to farm",true);
                    this.shutdown();
                    return;
                }
                var inventorySetup = new Rs2InventorySetup(config.inventorySetup(), mainScheduledFuture);
                if (!inventorySetup.doesInventoryMatch() || !inventorySetup.doesEquipmentMatch()) {
                    Rs2Walker.walkTo(Rs2Bank.getNearestBank().getWorldPoint(), 20);
                    if (!inventorySetup.loadEquipment() || !inventorySetup.loadInventory()) {                        
                        plugin.reportFinished("Failed to load inventory setup",false);
                        return;
                    }
                    Rs2Bank.closeBank();
                }

                log("Will visit " + herbPatches.size() + " herb patches");
            }
            

            if (Rs2Inventory.hasItem("Weeds")) {
                Rs2Inventory.drop("Weeds");
            }
            if (currentPatch == null) getNextPatch();
            if (currentPatch == null) {
                HerbrunPlugin.status = "Finishing up";
                cleanHerbsRun(); //Clean remaining herbs before banking
                if (config.goToBank()) {
                    Rs2Walker.walkTo(Rs2Bank.getNearestBank().getWorldPoint());
                    if (!Rs2Bank.isOpen()) Rs2Bank.openBank();
                    Rs2Bank.depositAll();
                }
                HerbrunPlugin.status = "Finished";
                plugin.reportFinished("Herb run finished",true);
                this.shutdown();
                
            }

            if (!currentPatch.isInRange(10)) {
                HerbrunPlugin.status = "Walking to " + currentPatch.getRegionName();
                Rs2Walker.walkTo(currentPatch.getLocation(), 20);

            }

            HerbrunPlugin.status = "Farming " + currentPatch.getRegionName();
            if (handleHerbPatch()) getNextPatch();


        }, 0, 1000, TimeUnit.MILLISECONDS);

        return true;
    }

    private void populateHerbPatches() {
        this.farmingHandler = new FarmingHandler(Microbot.getClient(), configManager);
        herbPatches.clear();
        clientThread.runOnClientThreadOptional(() -> {
            for (FarmingPatch patch : farmingWorld.getTabs().get(Tab.HERB)) {
                HerbPatch _patch = new HerbPatch(patch, config, farmingHandler);
                if (_patch.getPrediction() != CropState.GROWING && _patch.isEnabled()) herbPatches.add(_patch);
            }
            return true;
        });
    }

    private void getNextPatch() {
        if (currentPatch == null) {
            if (herbPatches.isEmpty()) {
                return;
            }

            // Start with weiss, getNearestBank doesn't like that area!
            currentPatch = herbPatches.stream()
                    .filter(patch -> Objects.equals(patch.getRegionName(), "Weiss"))
                    .findFirst()
                    .orElseGet(() -> herbPatches.stream()
                            .findFirst()
                            .orElse(null));
            herbPatches.remove(currentPatch);
        }
    }

    private boolean handleHerbPatch() {
        if (Rs2Inventory.isFull()) {
            cleanHerbsRun();
            clampedSleepGaussian(495, 130);
            noteCleanedHerbs();
            return false;
        }

        Integer[] ids = {
                ObjectID.MYARM_HERBPATCH,
                ObjectID.FARMING_HERB_PATCH_2,
                ObjectID.FARMING_HERB_PATCH_4,
                ObjectID.FARMING_HERB_PATCH_8,
                ObjectID.FARMING_HERB_PATCH_6,
                ObjectID.FARMING_HERB_PATCH_3,
                ObjectID.FARMING_HERB_PATCH_1,
                ObjectID.FARMING_HERB_PATCH_7,
                ObjectID.MY2ARM_HERBPATCH,
                ObjectID.FARMING_HERB_PATCH_5
        };
        var obj = Rs2GameObject.findObject(ids);
        if (obj == null) return false;
        var state = getHerbPatchState(obj);
        switch (state) {
            case "Empty":
                Rs2Inventory.use("compost");
                Rs2GameObject.interact(obj, "Compost");
                Rs2Player.waitForXpDrop(Skill.FARMING);
                Rs2Inventory.use(" seed");
                Rs2GameObject.interact(obj, "Plant");
                sleepUntil(() -> getHerbPatchState(obj).equals("Growing"));
                return false;
            case "Harvestable":
                Rs2GameObject.interact(obj, "Pick");
                sleepUntil(() -> getHerbPatchState(obj).equals("Empty") || Rs2Inventory.isFull(), 20000);
                return false;
            case "Weeds":
                Rs2GameObject.interact(obj);
                Rs2Player.waitForAnimation(10000);
                return false;
            case "Dead":
                Rs2GameObject.interact(obj, "Clear");
                sleepUntil(() -> getHerbPatchState(obj).equals("Empty"));
                return false;
            default:
                currentPatch = null;
                return true;
        }
    }

    private static String getHerbPatchState(TileObject rs2TileObject) {
        var game_obj = Rs2GameObject.convertToObjectComposition(rs2TileObject, true);
        var varbitValue = Microbot.getVarbitValue(game_obj.getVarbitId());

        if ((varbitValue >= 0 && varbitValue < 3) ||
                (varbitValue >= 60 && varbitValue <= 67) ||
                (varbitValue >= 173 && varbitValue <= 191) ||
                (varbitValue >= 204 && varbitValue <= 219) ||
                (varbitValue >= 221 && varbitValue <= 255)) {
            return "Weeds";
        }

        if ((varbitValue >= 4 && varbitValue <= 7) ||
                (varbitValue >= 11 && varbitValue <= 14) ||
                (varbitValue >= 18 && varbitValue <= 21) ||
                (varbitValue >= 25 && varbitValue <= 28) ||
                (varbitValue >= 32 && varbitValue <= 35) ||
                (varbitValue >= 39 && varbitValue <= 42) ||
                (varbitValue >= 46 && varbitValue <= 49) ||
                (varbitValue >= 53 && varbitValue <= 56) ||
                (varbitValue >= 68 && varbitValue <= 71) ||
                (varbitValue >= 75 && varbitValue <= 78) ||
                (varbitValue >= 82 && varbitValue <= 85) ||
                (varbitValue >= 89 && varbitValue <= 92) ||
                (varbitValue >= 96 && varbitValue <= 99) ||
                (varbitValue >= 103 && varbitValue <= 106) ||
                (varbitValue >= 192 && varbitValue <= 195)) {
            return "Growing";
        }

        if ((varbitValue >= 8 && varbitValue <= 10) ||
                (varbitValue >= 15 && varbitValue <= 17) ||
                (varbitValue >= 22 && varbitValue <= 24) ||
                (varbitValue >= 29 && varbitValue <= 31) ||
                (varbitValue >= 36 && varbitValue <= 38) ||
                (varbitValue >= 43 && varbitValue <= 45) ||
                (varbitValue >= 50 && varbitValue <= 52) ||
                (varbitValue >= 57 && varbitValue <= 59) ||
                (varbitValue >= 72 && varbitValue <= 74) ||
                (varbitValue >= 79 && varbitValue <= 81) ||
                (varbitValue >= 86 && varbitValue <= 88) ||
                (varbitValue >= 93 && varbitValue <= 95) ||
                (varbitValue >= 100 && varbitValue <= 102) ||
                (varbitValue >= 107 && varbitValue <= 109) ||
                (varbitValue >= 196 && varbitValue <= 197)) {
            return "Harvestable";
        }

        if ((varbitValue >= 128 && varbitValue <= 169) ||
                (varbitValue >= 198 && varbitValue <= 200)) {
            return "Diseased";
        }

        if ((varbitValue >= 170 && varbitValue <= 172) ||
                (varbitValue >= 201 && varbitValue <= 203)) {
            return "Dead";
        }

        return "Empty";
    }

    private void cleanHerbsRun() {
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

        List<Rs2ItemModel> herbStack = calculateInteractOrder(Rs2Inventory.items(x -> x.getName().toLowerCase().contains("grimy"))
                .collect(Collectors.toList()), interactOrderLocal);

        if (herbStack.isEmpty()) {
            Microbot.log("No herbs found in inventory.");
            return;
        }

        // Interact with each slot in the specified order
        for (Rs2ItemModel item : herbStack) {
            //Set baseline time, clean herb
            timeValue = System.currentTimeMillis();
            Rs2Inventory.interact(item, "Clean");
            randomNum = calculateSleepDuration(0.44);
            if (System.currentTimeMillis() - timeValue < randomNum) {
                sleep((int) (randomNum - (System.currentTimeMillis() - timeValue)));
            } else {
                sleep(Rs2Random.between(14, 28));
            }
        }
    }

    private void noteCleanedHerbs() {
        var ids = List.of(
                ItemID.GUAM_LEAF,
                ItemID.MARENTILL,
                ItemID.TARROMIN,
                ItemID.HARRALANDER,
                ItemID.RANARR_WEED,
                ItemID.TOADFLAX,
                ItemID.IRIT_LEAF,
                ItemID.AVANTOE,
                ItemID.KWUARM,
                ItemID.HUASCA,
                ItemID.SNAPDRAGON,
                ItemID.CADANTINE,
                ItemID.LANTADYME,
                ItemID.DWARF_WEED,
                ItemID.TORSTOL
        );

        Rs2NpcModel leprechaun = Rs2Npc.getNpc("Tool leprechaun");

        /*Rs2Inventory.items().forEachOrdered(item -> {
            if (ids.contains(item.getId()) && leprechaun != null && !item.isNoted())
            {
                //Rs2ItemModel unNoted = Rs2Inventory.getUnNotedItem("Grimy", false);
                //Rs2Inventory.use(unNoted);
                Rs2Inventory.interact(item, "Use");
                Rs2Npc.interact(leprechaun, "Talk-to");
                Rs2Inventory.waitForInventoryChanges(10000);
            }
        });*/

        // Step 1 & 2: Filter then collect matching items
        List<Rs2ItemModel> matchingItems = Rs2Inventory.items()
                .filter(item -> ids.contains(item.getId()) && leprechaun != null && !item.isNoted())
                .collect(Collectors.toList());

        // Step 3: Pick a random item if list is not empty
        if (!matchingItems.isEmpty())
        {
            Random rand = new Random();
            Rs2ItemModel randomItem = matchingItems.get(rand.nextInt(matchingItems.size()));

            // Step 4: Use the randomly selected item
            Rs2Inventory.interact(randomItem, "Use");
            Rs2Npc.interact(leprechaun, "Talk-to");
            Rs2Inventory.waitForInventoryChanges(10000);
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

    @Override
    public void shutdown() {
        super.shutdown();
        initialized = false;
    }
}
