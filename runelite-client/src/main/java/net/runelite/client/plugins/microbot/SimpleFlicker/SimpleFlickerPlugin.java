package net.runelite.client.plugins.microbot.SimpleFlicker;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = PluginDescriptor.Default + "SimpleFlicker",
        description = "Microbot SimpleFlicker plugin ML",
        tags = {"flick", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class SimpleFlickerPlugin extends Plugin {
    @Inject
    private SimpleFlickerConfig config;
    @Provides
    SimpleFlickerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(SimpleFlickerConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private SimpleFlickerOverlay simpleFlickerOverlay;

    @Inject
    SimpleFlickerScript simpleFlickerScript;


    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(simpleFlickerOverlay);
            simpleFlickerOverlay.myButton.hookMouseListener();
        }
        simpleFlickerScript.run(config);
    }

    protected void shutDown() {
        simpleFlickerScript.shutdown();
        overlayManager.remove(simpleFlickerOverlay);
        simpleFlickerOverlay.myButton.unhookMouseListener();
    }
    int ticks = 10;
    @Subscribe
    public void onGameTick(GameTick tick)
    {
        //System.out.println(getName().chars().mapToObj(i -> (char)(i + 3)).map(String::valueOf).collect(Collectors.joining()));

        if (ticks > 0) {
            ticks--;
        } else {
            ticks = 10;
        }

    }

}