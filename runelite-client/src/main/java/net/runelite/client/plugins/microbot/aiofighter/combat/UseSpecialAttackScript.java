package net.runelite.client.plugins.microbot.aiofighter.combat;

import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.aiofighter.AIOFighterConfig;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

import java.util.concurrent.TimeUnit;

public class UseSpecialAttackScript extends Script {

    public boolean run(AIOFighterConfig config) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;
                if (!config.useSpecialAttack()) return;
                //if (Rs2Equipment.isWearingFullGuthan()) return;

                Actor interactingNPC = Microbot.getClient().getLocalPlayer().getInteracting();

                if (Rs2Player.isInteracting() && interactingNPC instanceof NPC)
                {
                    NPC npc = (NPC) interactingNPC;
                    Rs2NpcModel npcModel = new Rs2NpcModel(npc);
                    int health = (int) Rs2Npc.getHealth(npcModel);

                    if (health > 73) {
                        Microbot.getSpecialAttackConfigs().useSpecWeapon();
                    }
                }

            } catch (Exception ex) {
                Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
        return true;
    }

    public void shutdown() {
        super.shutdown();
    }

}