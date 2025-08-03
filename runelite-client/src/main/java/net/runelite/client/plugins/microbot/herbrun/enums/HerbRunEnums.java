package net.runelite.client.plugins.microbot.herbrun.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

@Getter
@RequiredArgsConstructor
public enum HerbRunEnums {
    RANARR_WEED("Ranarr Weed", ItemID.RANARR_WEED,32),
    SNAPDRAGON("Snapdragon", ItemID.SNAPDRAGON,62);

    private final String name;
    private final int herbID;
    private final int farmingLevel;

    @Override
    public String toString() {
        return name;
    }

    public boolean hasRequiredLevel() {
        return Rs2Player.getSkillRequirement(Skill.FARMING, this.farmingLevel);
    }
}
