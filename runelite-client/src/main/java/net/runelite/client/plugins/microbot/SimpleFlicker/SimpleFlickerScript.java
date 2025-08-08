package net.runelite.client.plugins.microbot.SimpleFlicker;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.Notifier;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.bankjs.BanksBankStander.CurrentStatus;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2Cannon;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.InteractOrder;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import javax.inject.Inject;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory.calculateInteractOrder;

public class SimpleFlickerScript extends Script {

    private static final WorldPoint birdhouseLocation1 = new WorldPoint(3763, 3755, 0);
    private static final WorldPoint birdhouseLocation2 = new WorldPoint(3768, 3761, 0);
    private static final WorldPoint birdhouseLocation3 = new WorldPoint(3677, 3882, 0);
    private static final WorldPoint birdhouseLocation4 = new WorldPoint(3679, 3815, 0);
    public static double version = 1.0;
    private boolean initialized;
    @Inject
    private Notifier notifier;

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

    public static boolean test = false;

    public boolean run(SimpleFlickerConfig config) {
        Microbot.enableAutoRunOn = false;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;
                long startTime = System.currentTimeMillis();

                Microbot.log("Hello world");
                //cleanHerbsRun();

                long endTime = System.currentTimeMillis();
                long totalTime = endTime - startTime;
                System.out.println("Total time for loop " + totalTime);

                this.shutdown();

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
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

    boolean isCannonNearbyTest() {
        final int DWARF_CANNON_OBJECT_ID = 6; // Replace this with your logged ID

        return Rs2GameObject.getGameObjects().stream()
                .anyMatch(obj -> obj.getId() == DWARF_CANNON_OBJECT_ID &&
                        obj.getWorldLocation().distanceTo(Rs2Player.getWorldLocation()) <= 5);
    }

    private void cannonPick()
    {
        Rs2Cannon.pickUpCannon();
        Rs2Walker.walkTo(Rs2Bank.getNearestBank().getWorldPoint());
        this.shutdown();
    }


    @Override
    public void shutdown() {
        super.shutdown();
    }}

