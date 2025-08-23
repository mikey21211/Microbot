package net.runelite.client.plugins.microbot.SimpleFlicker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigInformation;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("Test")
@ConfigInformation("<div style='background-color:black;color:yellow;padding:20px;'>" +
        "<center><h2>Instructions</h2>" +
        " <p>Enter whatever code you want.<br /><br />" +
        " <b style='color:red;'>MUST</b> Profit <br />")

public interface SimpleFlickerConfig extends Config {

    @ConfigItem(
            keyName = "usePiety",
            name = "Use Piety",
            description = "Enable Piety flicking"
    )
    default boolean usePiety() {
        return false;
    }

    @ConfigItem(
            keyName = "useChivalry",
            name = "Use Chivalry",
            description = "Enable Chivalry flicking"
    )
    default boolean useChivalry() {
        return false;
    }

    @ConfigItem(
            keyName = "useUltimateStrength",
            name = "Use Ultimate Strength",
            description = "Enable Ultimate Strength flicking"
    )
    default boolean useUltimateStrength() {
        return false;
    }

    @ConfigItem(
            keyName = "useIncredibleReflexes",
            name = "Use Incredible Reflexes",
            description = "Enable Incredible Reflexes flicking"
    )
    default boolean useIncredibleReflexes() {
        return false;
    }

    @ConfigItem(
            keyName = "useMysticMight",
            name = "Use Mystic Might",
            description = "Enable Mystic Might flicking"
    )
    default boolean useMysticMight() {
        return false;
    }

    @ConfigItem(
            keyName = "useAugury",
            name = "Use Augury",
            description = "Enable Augury flicking"
    )
    default boolean useAugury() {
        return false;
    }

    @ConfigItem(
            keyName = "useRigour",
            name = "Use Rigour",
            description = "Enable Rigour flicking"
    )
    default boolean useRigour() {
        return false;
    }

    @ConfigItem(
            keyName = "useEagleEye",
            name = "Use Eagle Eye",
            description = "Enable Eagle Eye flicking"
    )
    default boolean useEagleEye() {
        return false;
    }

    @ConfigItem(
            keyName = "weaponAttackSpeed",
            name = "Weapon Attack Speed (ticks)",
            description = "Enter the weapon's attack speed in ticks (e.g. 4 for whip)"
    )
    default int weaponAttackSpeed() {
        return 4;
    }


}