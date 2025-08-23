package net.runelite.client.plugins.microbot.SimpleFlicker;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SimpleFlickerScript extends Script {

    public static double version = 1.3;

    // Time of last attack animation
    private long lastAttackTime = 0;
    private int lastAnim = -1;

    private static final int TICK_LENGTH = 600;

    public boolean run(SimpleFlickerConfig config) {
        Microbot.enableAutoRunOn = false;

        int attackSpeedTicks = config.weaponAttackSpeed();

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;

                // Skip flicking if we're not in combat
                if (!Rs2Player.isInCombat()) {
                    disableSelectedPrayers(config);
                    return;
                }

                // Detect new attack animation
                int anim = Rs2Player.getAnimation();
                if (anim != -1 && anim != lastAnim) {
                    lastAttackTime = System.currentTimeMillis();
                    lastAnim = anim;
                }

                // Calculate expected swing timing
                long timeSinceLastAttack = System.currentTimeMillis() - lastAttackTime;
                long expectedAttackInterval = (long) attackSpeedTicks * TICK_LENGTH;
                long flickBefore = TICK_LENGTH - (long) Rs2Random.truncatedGauss(30, 80, 4); // ~520–570ms

                // Flick slightly before expected next swing
                if (timeSinceLastAttack >= (expectedAttackInterval - flickBefore)) {
                    flickSelectedPrayers(config);
                    lastAttackTime = System.currentTimeMillis(); // Reset timer
                }

            } catch (Exception ex) {
                System.out.println("SimpleFlicker error: " + ex.getMessage());
            }

            // Always turn off prayers (failsafe)
            disableSelectedPrayers(config);

        }, 0, 10, TimeUnit.MILLISECONDS);

        return true;
    }

    private static void flickSelectedPrayers(SimpleFlickerConfig config) {
        List<Rs2PrayerEnum> prayers = getSelectedPrayers(config);

        // Turn on selected prayers
        prayers.forEach(p -> Rs2Prayer.toggle(p, true));

        // Wait a random short delay (simulate human flick)
        sleep((int) Rs2Random.truncatedGauss(84, 122, 4)); // 40–65ms

        // Turn prayers back off
        prayers.forEach(p -> Rs2Prayer.toggle(p, false));
    }

    private static void disableSelectedPrayers(SimpleFlickerConfig config) {
        getSelectedPrayers(config).forEach(p -> Rs2Prayer.toggle(p, false));
    }

    private static List<Rs2PrayerEnum> getSelectedPrayers(SimpleFlickerConfig config) {
        List<Rs2PrayerEnum> list = new ArrayList<>();

        if (config.usePiety()) list.add(Rs2PrayerEnum.PIETY);
        if (config.useChivalry()) list.add(Rs2PrayerEnum.CHIVALRY);
        if (config.useUltimateStrength()) list.add(Rs2PrayerEnum.ULTIMATE_STRENGTH);
        if (config.useIncredibleReflexes()) list.add(Rs2PrayerEnum.INCREDIBLE_REFLEXES);
        if (config.useMysticMight()) list.add(Rs2PrayerEnum.MYSTIC_MIGHT);
        if (config.useAugury()) list.add(Rs2PrayerEnum.AUGURY);
        if (config.useRigour()) list.add(Rs2PrayerEnum.RIGOUR);
        if (config.useEagleEye()) list.add(Rs2PrayerEnum.EAGLE_EYE);

        return list;
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}
