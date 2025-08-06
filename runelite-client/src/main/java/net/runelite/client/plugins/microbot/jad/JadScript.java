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

/*public class JadScript extends Script {
    public static final String VERSION = "1.0.5";
    public static final Map<Integer, Long> npcAttackCooldowns = new HashMap<>();

    public boolean run(JadConfig config) {
        Microbot.enableAutoRunOn = false;
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn() || !super.run()) return;

                var jadNpcs = Rs2Npc.getNpcs("Jad", false);

                for (Rs2NpcModel jadNpc : jadNpcs.collect(Collectors.toList())) {
                    if (jadNpc == null) continue;

                    long currentTimeMillis = System.currentTimeMillis();
                    int npcIndex = jadNpc.getIndex();

                    if (npcAttackCooldowns.containsKey(npcIndex)) {
                        if (currentTimeMillis - npcAttackCooldowns.get(npcIndex) < 4600) {
                            continue;
                        } else {
                            npcAttackCooldowns.remove(npcIndex);
                        }
                    }

                    int npcAnimation = Rs2Reflection.getAnimation(jadNpc);
                    handleJadPrayer(npcAnimation);
                    if (config.shouldAttackHealers()) {
                        handleHealerInteraction();
                        npcAttackCooldowns.put(npcIndex, currentTimeMillis);
                    }
                }
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 10, TimeUnit.MILLISECONDS);
        return true;
    }

    private void handleHealerInteraction() {
        var healer = Rs2Npc.getNpcs("hurkot", false)
                .filter(npc -> npc != null && npc.getInteracting() != Microbot.getClient().getLocalPlayer())
                .findFirst()
                .orElse(null);

        if (healer != null) {
            Rs2Npc.interact(healer, "attack");
        } else {
            var npc = Rs2Player.getInteracting();
            if (npc == null || npc != null && npc.getName().contains("hurkot")) {
                Rs2Npc.interact(Rs2Npc.getNpc("Jad", false), "attack");
            }
        }

    }

    @Override
    public void shutdown() {
        super.shutdown();
        npcAttackCooldowns.clear();
    }

    private void handleJadPrayer(int animationId) {
        if (animationId == 7592 || animationId == 2656) {
            Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MAGIC, true);
        } else if (animationId == 7593 || animationId == 2652) {
            Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_RANGE, true);
        }
    }
}*/

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
                    int animationId = Rs2Reflection.getAnimation(jadNpc);

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

    /*private void handleHealerInteraction(long currentTimeMillis) {
        var localPlayer = Microbot.getClient().getLocalPlayer();

        // If we're waiting on a healer to aggro
        if (currentHealerTarget != null) {
            var targetAggro = currentHealerTarget.getInteracting();
            if (targetAggro == localPlayer) {
                System.out.println("[JadScript] Healer " + currentHealerTarget.getIndex() + " has aggro. Moving on.");
                currentHealerTarget = null; // clear and proceed next tick
            } else {
                System.out.println("[JadScript] Waiting for healer " + currentHealerTarget.getIndex() + " to aggro.");
            }
            return; // Don't proceed to next healer yet
        }

        // Look for a new healer to attack
        var healer = Rs2Npc.getNpcs("hurkot", false)
                .filter(npc -> npc != null && npc.getInteracting() != localPlayer)
                .filter(npc -> {
                    int npcIndex = npc.getIndex();
                    boolean cooldownOver = !healerAttackCooldowns.containsKey(npcIndex)
                            || currentTimeMillis - healerAttackCooldowns.get(npcIndex) >= 3000;
                    if (!cooldownOver) {
                        System.out.println("[JadScript] Skipping healer " + npcIndex + " (cooldown not finished)");
                    }
                    return cooldownOver;
                })
                .findFirst()
                .orElse(null);

        if (healer != null) {
            int npcIndex = healer.getIndex();
            System.out.println("[JadScript] Attacking healer " + npcIndex);
            if (Rs2Npc.interact(healer, "attack")) {
                healerAttackCooldowns.put(npcIndex, currentTimeMillis);
                currentHealerTarget = healer; // ✅ Wait on this guy to aggro
            } else {
                System.out.println("[JadScript] Failed to attack healer " + npcIndex);
            }
        } else {
            var target = Rs2Player.getInteracting();
            if (target == null || target.getName().contains("hurkot")) {
                System.out.println("[JadScript] No valid healer found, attacking Jad.");
                Rs2Npc.interact(Rs2Npc.getNpc("Jad", false), "attack");
            }
        }
    }*/

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

        if (!aliveHealers.isEmpty() && aggroedCount == aliveHealers.size()) {
            System.out.println("[JadScript] All healers are aggroed. Attacking Jad.");
            Rs2Npc.interact(Rs2Npc.getNpc("Jad", false), "attack");
            return;
        }

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
