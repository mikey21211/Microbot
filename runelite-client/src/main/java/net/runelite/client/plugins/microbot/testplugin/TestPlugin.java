package net.runelite.client.plugins.microbot.testplugin;

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
        name = PluginDescriptor.Default + "Test",
        description = "Microbot test plugin ML",
        tags = {"test", "microbot"},
        enabledByDefault = false
)
@Slf4j
public class TestPlugin extends Plugin {
    @Inject
    private TestConfig config;
    @Provides
    TestConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(TestConfig.class);
    }

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private TestOverlay testOverlay;

    @Inject
    TestScript testScript;


    @Override
    protected void startUp() throws AWTException {
        if (overlayManager != null) {
            overlayManager.add(testOverlay);
            testOverlay.myButton.hookMouseListener();
        }
        testScript.run(config);
    }

    protected void shutDown() {
        testScript.shutdown();
        overlayManager.remove(testOverlay);
        testOverlay.myButton.unhookMouseListener();
    }
}
