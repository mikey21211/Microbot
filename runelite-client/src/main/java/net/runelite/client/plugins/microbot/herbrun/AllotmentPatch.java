package net.runelite.client.plugins.microbot.herbrun;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.questhelper.helpers.mischelpers.farmruns.FarmingHandler;
import net.runelite.client.plugins.microbot.questhelper.helpers.mischelpers.farmruns.FarmingPatch;
import net.runelite.client.plugins.microbot.util.walker.enums.Allotments;
import net.runelite.client.plugins.microbot.questhelper.helpers.mischelpers.farmruns.CropState;
import java.util.HashMap;
import java.util.Objects;

@Getter
public class AllotmentPatch {
    private final FarmingPatch patch;
    private final String regionName;
    private final CropState prediction;
    private final WorldPoint location;
    private boolean enabled;
    private final HashMap<String, Integer> items = new HashMap<>();

    public AllotmentPatch(FarmingPatch patch, HerbrunConfig config, FarmingHandler farmingHandler) {
        this.patch = patch;
        this.regionName = patch.getRegion().getName();
        this.prediction = farmingHandler.predictPatch(patch);
        this.location = getAllotmentsFromName(regionName).getWorldPoint();
        switch (regionName) {
            case "Ardougne":
//                if (Rs2Bank.hasItem("Ardougne cloak")) {
//                    this.items.put("Ardougne cloak", 1);
//                }
                this.enabled = config.enableArdougne();
                break;
            case "Catherby":
                this.items.put("Camelot teleport", 1);
                this.enabled = config.enableCatherby();
                break;
            case "Civitas illa Fortis":
                this.items.put("Civitas illa fortis teleport", 1);
                this.enabled = config.enableVarlamore();
                break;
            case "Falador":
                this.items.put("Explorer's ring", 1);
                this.enabled = config.enableFalador();
                break;
            case "Farming Guild":
                this.items.put("Skills necklace(", 1);
                this.enabled = config.enableGuild();
                break;
            case "Prifddinas":
                this.enabled = false;
                break;
            case "Kourend":
                this.items.put("Xeric's talisman", 1);
                this.enabled = config.enableHosidius();
                break;
            case "Morytania":
                this.items.put("Ectophial", 1);
                this.enabled = config.enableMorytania();
                break;
            case "Harmony":
                this.enabled = false;
                break;

        }
    }

    /**
     * Gets a Allotments enum value from its string name
     * @param regionName The region name (e.g., "Ardougne")
     * @return The matching Allotments enum value, or NONE if not found
     */
    private static Allotments getAllotmentsFromName(String regionName) {
        for (Allotments allotments : Allotments.values()) {
            if (allotments.getName().equalsIgnoreCase(regionName)) {
                return allotments;
            }
        }
        return Allotments.NONE;
    }

    /*
    public boolean isInRange(int distance) {
        if(Objects.equals(regionName, "Weiss")) {
            return Rs2Player.getWorldLocation().getRegionID() == 11325;

        } else if(Objects.equals(regionName, "Troll Stronghold")) {
            return Rs2Player.getWorldLocation().getRegionID() == 11321;
        } else {
            return Rs2Player.getWorldLocation().distanceTo(location) < distance;
        }
    }*/

    public boolean contains(WorldPoint worldPoint) {
        return location.equals(worldPoint);
    }

    public boolean contains(String regionName) {
        return this.regionName.equals(regionName);
    }

}
