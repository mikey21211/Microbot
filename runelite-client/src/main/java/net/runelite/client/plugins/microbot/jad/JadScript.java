package net.runelite.client.plugins.microbot.jad;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.reflection.Rs2Reflection;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class JadScript extends Script {
    public static final String VERSION = "1.0.5";

    public static final Map<Integer, Long> jadAttackCooldowns = new HashMap<>();
    public static final Map<Integer, Long> healerAttackCooldowns = new HashMap<>();

    private Rs2NpcModel currentHealerTarget = null;

    public boolean run(JadConfig config) {
        Microbot.enableAutoRunOn = false;

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !super.run()) return;

                long currentTimeMillis = System.currentTimeMillis();

                // Handle Jad Prayer Logic
                var jadNpcs = Rs2Npc.getNpcs("Jad", false); // Try "TzTok-Jad" if this doesn't work
                var jadList = jadNpcs.collect(Collectors.toList());

                if (jadList.isEmpty()) {
                    System.out.println("[JadScript] No Jad NPCs found.");
                }

                for (Rs2NpcModel jadNpc : jadList) {
                    if (jadNpc == null) continue;

                    int npcIndex = jadNpc.getIndex();
                    int animationId = jadNpc.getAnimation();

                    System.out.println("[JadScript] Jad NPC index: " + npcIndex + ", Animation ID: " + animationId);

                    handleJadPrayer(animationId); // Always attempt to switch prayer for now

                    // Log cooldown setting
                    System.out.println("[JadScript] Setting prayer cooldown for Jad " + npcIndex);
                    jadAttackCooldowns.put(npcIndex, currentTimeMillis); // cooldown logic can be added back later
                }

                // Handle Healers
                if (config.shouldAttackHealers()) {
                    handleHealerInteraction(currentTimeMillis);
                }

            } catch (Exception ex) {
                System.out.println("[JadScript] ERROR: " + ex.getMessage());
                ex.printStackTrace();
            }
        }, 0, 10, TimeUnit.MILLISECONDS);

        return true;
    }

    private void handleHealerInteraction(long currentTimeMillis) {
        var localPlayer = Microbot.getClient().getLocalPlayer();

        // 🧹 Reset if current target is dead
        if (currentHealerTarget != null && currentHealerTarget.isDead()) {
            System.out.println("[JadScript] Current healer target is dead. Resetting.");
            currentHealerTarget = null;
        }

        // ⏳ Wait for current healer to aggro
        if (currentHealerTarget != null) {
            if (currentHealerTarget.getInteracting() == localPlayer) {
                System.out.println("[JadScript] Healer " + currentHealerTarget.getIndex() + " has aggro.");
                currentHealerTarget = null; // Ready for next
            } else {
                System.out.println("[JadScript] Waiting for healer " + currentHealerTarget.getIndex() + " to aggro.");
                return; // Don't select next yet
            }
        }

        // 🧠 Get all healers that are alive
        var aliveHealers = Rs2Npc.getNpcs("hurkot", false)
                .filter(npc -> npc != null && !npc.isDead())
                .collect(Collectors.toList());

        // ✅ If all alive healers are aggroed on player, switch back to Jad
        long aggroedCount = aliveHealers.stream()
                .filter(h -> h.getInteracting() == localPlayer)
                .count();

        /*if (!aliveHealers.isEmpty() && aggroedCount == aliveHealers.size()) {
            System.out.println("[JadScript] All healers are aggroed. Attacking Jad.");
            Rs2Npc.interact(Rs2Npc.getNpc("Jad", false), "attack");
            return;
        }*/

        // 🎯 Try to find a new healer to attack
        var healer = aliveHealers.stream()
                .filter(h -> h.getInteracting() != localPlayer)
                .filter(h -> {
                    int id = h.getIndex();
                    boolean cooldownOver = !healerAttackCooldowns.containsKey(id) ||
                            currentTimeMillis - healerAttackCooldowns.get(id) >= 3000;
                    if (!cooldownOver) {
                        System.out.println("[JadScript] Skipping healer " + id + " (cooldown active)");
                    }
                    return cooldownOver;
                })
                .findFirst()
                .orElse(null);

        // 🗡️ Attack that healer if found
        if (healer != null) {
            int id = healer.getIndex();
            System.out.println("[JadScript] Attacking healer " + id);
            if (Rs2Npc.interact(healer, "attack")) {
                healerAttackCooldowns.put(id, currentTimeMillis);
                currentHealerTarget = healer;
            } else {
                System.out.println("[JadScript] Failed to attack healer " + id);
            }
        } else {
            System.out.println("[JadScript] No valid healer found to attack this tick.");
        }
    }


    private void handleJadPrayer(int animationId) {
        if (animationId == 7592 || animationId == 2656) {
            System.out.println("[JadScript] Switching to Protect from Magic");
            Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MAGIC, true);
        } else if (animationId == 7593 || animationId == 2652) {
            System.out.println("[JadScript] Switching to Protect from Missiles");
            Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_RANGE, true);
        } else {
            System.out.println("[JadScript] No matching prayer for animation: " + animationId);
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        jadAttackCooldowns.clear();
        healerAttackCooldowns.clear();
        System.out.println("[JadScript] Shutdown: cooldowns cleared.");
    }
}
