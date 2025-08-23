package net.runelite.client.plugins.microbot.aiofighter.cannon;

import lombok.Getter;
import net.runelite.api.ObjectID;
import net.runelite.api.TileObject;
import net.runelite.api.World;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.aiofighter.AIOFighterConfig;
import net.runelite.client.plugins.microbot.aiofighter.enums.State;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2Cannon;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.math.Rs2Random;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class CannonScript extends Script {

    // Last target point in the circular path around the cannon
    private WorldPoint lastCircleTarget = null;

    //Used to differentiate monsters after one is ko'd
    private int currentSlayerCount = -1;

    // Current index in the circular path list
    private int circleStep = 0;
    // Timestamp of the last detected cannonball projectile
    private long lastCannonProjectileTime = 0;
    //Cannon rotation time, full rotation
    final private int cannonRotationTime = 5000;

    int minTiles = 4;    // at least 3 tiles away (including diagonals)
    int maxOffset = 6;

    @Getter
    public final int cannonballID = 53;

    public boolean run(AIOFighterConfig config) {
        // Schedule the cannon logic to run every 2 seconds
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                // Skip if not logged in or if script toggle is off
                if (!Microbot.isLoggedIn()) return;
                if (!super.run() || !config.toggleCannon()) return;

                // Skip if player is banking or walking somewhere
                if(config.state().equals(State.BANKING) || config.state().equals(State.WALKING))
                    return;

                // Repair cannon if needed
                if (Rs2Cannon.repair())
                    return;
                // Refill cannon if needed
                Rs2Cannon.refill();

                // Run main logic for checking cannon state and circling
                checkCannonAndCircle();

            } catch(Exception ex) {
                Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
            }
        }, 0, 2000, TimeUnit.MILLISECONDS);
        return true;
    }

    /**
     * Checks if the cannon is present and firing.
     * If not firing, moves the player in a smooth circle around it.
     */
    private void checkCannonAndCircle() {
        boolean cannonRecentlyFired = System.currentTimeMillis() - lastCannonProjectileTime < cannonRotationTime;
        Microbot.log("Cannon recently fired: " + cannonRecentlyFired);
        Microbot.log(System.currentTimeMillis() + " - " + lastCannonProjectileTime + " = " + (System.currentTimeMillis() - lastCannonProjectileTime));

        if (!Rs2Player.isInCombat()) {
            Microbot.log("Player is not in combat. Skipping circling.");
            return;
        }

        TileObject cannon = Rs2GameObject.findObject(new Integer[]{
                ObjectID.DWARF_MULTICANNON,
                ObjectID.DWARF_MULTICANNON_43027
        });

        if (cannon == null) {
            Microbot.log("No cannon found in the game world.");
            return;
        }

        WorldPoint cannonCenter = cannon.getWorldLocation();
        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        int distanceToCannon = cannonCenter.distanceTo(playerLocation);

        //Before we see if the cannon has recently fired, check and see first if we are too far from it.
        checkIfTooFar(cannonCenter, playerLocation, distanceToCannon);

        //Catch all - if the cannon fired recently, won't even touch the complicated code below
        if (cannonRecentlyFired) {
            //Microbot.log("Cannon fired recently. Skipping moving.");
            lastCircleTarget = null; //Reset circle walking, otherwise upon a new monster we won't try to circle
            return;
        }

        //========================================================================================

        //Microbot.log("Found cannon at " + cannonCenter + ", player at " + playerLocation + ", distance: " + distanceToCannon);

        //If too close to cannon
        if (distanceToCannon <= 2) {
            Microbot.log("Too close to cannon, trying to move away...");

            int moveDistance = Rs2Random.between(minTiles, maxOffset);
            double angle = Math.toRadians(Rs2Random.between(0, 359));

            int dx = (int) Math.round(Math.cos(angle) * moveDistance);
            int dy = (int) Math.round(Math.sin(angle) * moveDistance);

            if (dx == 0 && dy == 0) {
                dx = moveDistance; // Ensure we move somewhere
            }

            WorldPoint proposedWalkPoint = cannonCenter.dx(dx).dy(dy);
            WorldPoint walkTarget = null;

            // Check if initial proposed point is walkable
            if (Rs2Walker.canReach(proposedWalkPoint)) {
                walkTarget = proposedWalkPoint;
            } else {
                Microbot.log("Initial moveAwayPoint not reachable: " + proposedWalkPoint + ". Trying fallback directions...");

                int[][] fallbackDirs = {
                        {1, 0}, {-1, 0}, {0, 1}, {0, -1}
                };

                for (int[] dir : fallbackDirs) {
                    WorldPoint fallback = cannonCenter.dx(dir[0] * moveDistance).dy(dir[1] * moveDistance);
                    if (Rs2Walker.canReach(fallback)) {
                        walkTarget = fallback;
                        Microbot.log("Fallback walkable tile found: " + fallback);
                        break;
                    }
                }
            }

            if (walkTarget == null) {
                Microbot.log("No walkable tiles found. Skipping movement.");
                return;
            }

            final WorldPoint finalMoveAwayPoint = walkTarget; // ✅ Final assignment before lambda

            Rs2Walker.walkFastCanvas(finalMoveAwayPoint);

            sleepUntil(() ->
                            !Rs2Player.isMoving() &&
                                    finalMoveAwayPoint.distanceTo(Rs2Player.getWorldLocation()) <= 1,
                    8000
            );

            playerLocation = Rs2Player.getWorldLocation();
            distanceToCannon = cannonCenter.distanceTo(playerLocation);
            //Microbot.log("Moved away from cannon. New location: " + playerLocation + ", distance: " + distanceToCannon);
            return;//Only go to the next bit of code to begin circling if we are far enough away first.
        }

        List<WorldPoint> circlePoints = List.of(
                cannonCenter.dx(2),
                cannonCenter.dx(2).dy(1),
                cannonCenter.dx(2).dy(2),
                cannonCenter.dx(1).dy(2),
                cannonCenter.dy(2),
                cannonCenter.dx(-1).dy(2),
                cannonCenter.dx(-2).dy(2),
                cannonCenter.dx(-2).dy(1),
                cannonCenter.dx(-2),
                cannonCenter.dx(-2).dy(-1),
                cannonCenter.dx(-2).dy(-2),
                cannonCenter.dx(-1).dy(-2),
                cannonCenter.dy(-2),
                cannonCenter.dx(1).dy(-2),
                cannonCenter.dx(2).dy(-2),
                cannonCenter.dx(2).dy(-1)
        );

        //First seed when circling cannon
        if (lastCircleTarget == null) {
            // First seed → pick closest or random
            WorldPoint me = Rs2Player.getWorldLocation();
            int best = Integer.MAX_VALUE;
            WorldPoint fallback = null;
            int bestIdx = -1;

            for (int i = 0; i < circlePoints.size(); i++) {
                WorldPoint p = circlePoints.get(i);
                if (!Rs2Walker.canReach(p)) continue; // ✅ only reachable
                int d = p.distanceTo(me);
                if (d < best) {
                    best = d;
                    fallback = p;
                    bestIdx = i;
                }
            }

            if (fallback != null) {
                lastCircleTarget = fallback;
                circleStep = bestIdx;
                Microbot.log("First time circling. Closest reachable point chosen: "
                        + fallback + " (step=" + circleStep + ")");
            } else {
                Microbot.log("No reachable circle points at all. Skipping this tick.");
                return;
            }
        }

        if (lastCircleTarget == null || Rs2Player.getWorldLocation().equals(lastCircleTarget)) //Advance to next point on list of points circling cannon
        {
            circleStep = (circleStep + 1) % circlePoints.size();
            lastCircleTarget = circlePoints.get(circleStep);

            // ✅ Reachability fallback (applied every step)
            if (!Rs2Walker.canReach(lastCircleTarget)) {
                WorldPoint me = Rs2Player.getWorldLocation();
                WorldPoint fallback = null;
                int best = Integer.MAX_VALUE;

                for (int i = 0; i < circlePoints.size(); i++) {
                    WorldPoint p = circlePoints.get(i);
                    if (!Rs2Walker.canReach(p)) continue;
                    int d = p.distanceTo(me);
                    if (d < best) {
                        best = d;
                        fallback = p;
                        circleStep = i;
                    }
                }
                if (fallback != null) {
                    lastCircleTarget = fallback;
                    Microbot.log("Fallback reachable circle point: "
                            + fallback + " (step=" + circleStep + ")");
                } else {
                    Microbot.log("No reachable circle points; skipping this tick.");
                    return;
                }
            }

            int targetDistance = cannonCenter.distanceTo(lastCircleTarget);
            Microbot.log("Cannon not firing, moving to circle point " + lastCircleTarget
                    + " (distance: " + targetDistance + ", step " + circleStep + ")");
            Rs2Walker.walkFastCanvas(lastCircleTarget);
        }

        else if ((lastCircleTarget != null) && !slayerCountEqual()) //Slayer count check ensures we only enter this function once
        {
            currentSlayerCount = Microbot.getClient().getVarpValue(VarPlayerID.SLAYER_COUNT);
            circleStep = Rs2Random.between(0, circlePoints.size() - 1);//Choose random starting point to circle from
            lastCircleTarget = null; //Reset
        }

    }

    private void checkIfTooFar(WorldPoint pLocation, WorldPoint cCenter, int distanceCannon)
    {
        //If too far from cannon
        if (distanceCannon > 6) {
            Microbot.log("Too far from cannon (>" + 6 + "). Walking closer...");
            Rs2Walker.walkTo(cCenter, 4);
            sleepUntil(() ->
                            !Rs2Player.isMoving() &&
                                    cCenter.distanceTo(Rs2Player.getWorldLocation()) <= 4,
                    8000
            );

            distanceCannon = cCenter.distanceTo(pLocation);
            Microbot.log("Arrived near cannon. New player location: " + pLocation + ", distance: " + distanceCannon);
        }
    }

    private boolean slayerCountEqual()
    {
        return currentSlayerCount == (Microbot.getClient().getVarpValue(VarPlayerID.SLAYER_COUNT));
    }

    /**
     * Listens for any projectile movement in the game.
     * If a cannonball (ID 53) is detected, record the current time.
     */
    @Subscribe
    public void onProjectileFired() {
            lastCannonProjectileTime = System.currentTimeMillis();
            //Microbot.log("Cannon fired!");
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}
