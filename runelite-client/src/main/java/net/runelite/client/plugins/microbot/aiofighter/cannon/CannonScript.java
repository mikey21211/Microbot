package net.runelite.client.plugins.microbot.aiofighter.cannon;

import net.runelite.api.ObjectID;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.aiofighter.AIOFighterConfig;
import net.runelite.client.plugins.microbot.aiofighter.enums.State;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2Cannon;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.Projectile;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class CannonScript extends Script {

    private WorldPoint lastCircleTarget = null;
    private int circleStep = 0;
    private long lastCannonProjectileTime = 0;

    public boolean run(AIOFighterConfig config) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run() || !config.toggleCannon()) return;
                
                if(config.state().equals(State.BANKING) || config.state().equals(State.WALKING))
                    return;
                
               if (Rs2Cannon.repair())
                   return;
               Rs2Cannon.refill();



            } catch(Exception ex) {
                Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
            }
        }, 0, 2000, TimeUnit.MILLISECONDS);
        return true;
    }

    /*private void checkCannonAndCircle() {
        if (!Rs2Player.isInCombat()) return;

        // Find the dwarf cannon object
        TileObject cannon = Rs2GameObject.findObject(new Integer[]{
                ObjectID.DWARF_MULTICANNON,
                ObjectID.DWARF_MULTICANNON_43027
        });

        if (cannon == null) return;

        WorldPoint cannonLocation = Rs2GameObject.getLocation(cannon);
        if (cannonLocation == null) return;

        if (cannonLocation.distanceTo(Rs2Player.getWorldLocation()) > 3)
            return;

        boolean cannonRecentlyFired = System.currentTimeMillis() - lastCannonProjectileTime < 3000;
        if (cannonRecentlyFired) return;

        List<WorldPoint> circlePoints = List.of(
                cannonLocation.dx(2),
                cannonLocation.dx(2).dy(2),
                cannonLocation.dy(2),
                cannonLocation.dx(-2).dy(2),
                cannonLocation.dx(-2),
                cannonLocation.dx(-2).dy(-2),
                cannonLocation.dy(-2),
                cannonLocation.dx(2).dy(-2)
        );

        if (lastCircleTarget == null || Rs2Player.getWorldLocation().equals(lastCircleTarget)) {
            circleStep = (circleStep + 1) % circlePoints.size();
            lastCircleTarget = circlePoints.get(circleStep);
            Rs2Walker.walkTo(lastCircleTarget);
            Microbot.log("Cannon not firing, moving to: " + lastCircleTarget);
        }
    }*/


    @Subscribe
    public void onProjectileMoved(ProjectileMoved event) {
        Projectile projectile = event.getProjectile();

        // ID 53 is common for cannonballs (you may want to confirm in-game)
        if (projectile.getId() == 53) {
            lastCannonProjectileTime = System.currentTimeMillis();
            Microbot.log("Cannon fired!");
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }


}
