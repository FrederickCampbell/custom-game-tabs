package com.freddy.customgametabs;

import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

/** Persists complete activity layouts while reusing the 2.1.x keys for migration. */
@Singleton
final class ActivityLayoutStore
{
    static final int PRESET_COUNT = 3;

    private static final String PRESET_PREFIX = "freeformPreset";
    private static final String RECOVERY_KEY = "freeformRecovery";
    private static final String WORKING_KEY = "freeformWorking";

    private final ConfigManager configManager;

    @Inject
    ActivityLayoutStore(ConfigManager configManager)
    {
        this.configManager = configManager;
    }

    ActivityLayoutSnapshot loadPreset(int slot)
    {
        return validSlot(slot)
            ? load(presetKey(slot))
            : null;
    }

    void savePreset(int slot, ActivityLayoutSnapshot snapshot)
    {
        if (validSlot(slot) && snapshot != null)
        {
            save(presetKey(slot), snapshot);
        }
    }

    boolean hasPreset(int slot)
    {
        return loadPreset(slot) != null;
    }

    ActivityLayoutSnapshot loadRecovery()
    {
        return load(RECOVERY_KEY);
    }

    void saveRecovery(ActivityLayoutSnapshot snapshot)
    {
        if (snapshot != null)
        {
            save(RECOVERY_KEY, snapshot);
        }
    }

    boolean hasRecovery()
    {
        return loadRecovery() != null;
    }

    ActivityLayoutSnapshot loadWorking()
    {
        return load(WORKING_KEY);
    }

    void saveWorking(ActivityLayoutSnapshot snapshot)
    {
        if (snapshot != null)
        {
            save(WORKING_KEY, snapshot);
        }
    }

    boolean hasWorking()
    {
        return loadWorking() != null;
    }

    void clearAll()
    {
        for (int slot = 1; slot <= PRESET_COUNT; slot++)
        {
            configManager.unsetConfiguration(CustomGameTabsConfig.GROUP, presetKey(slot));
        }
        configManager.unsetConfiguration(CustomGameTabsConfig.GROUP, RECOVERY_KEY);
        configManager.unsetConfiguration(CustomGameTabsConfig.GROUP, WORKING_KEY);
    }

    private ActivityLayoutSnapshot load(String key)
    {
        return ActivityLayoutSnapshot.parse(
            configManager.getConfiguration(CustomGameTabsConfig.GROUP, key)
        );
    }

    private void save(String key, ActivityLayoutSnapshot snapshot)
    {
        configManager.setConfiguration(
            CustomGameTabsConfig.GROUP,
            key,
            snapshot.encode()
        );
    }

    private static String presetKey(int slot)
    {
        return PRESET_PREFIX + slot;
    }

    private static boolean validSlot(int slot)
    {
        return slot >= 1 && slot <= PRESET_COUNT;
    }
}
