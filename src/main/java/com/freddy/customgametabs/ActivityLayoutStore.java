package com.freddy.customgametabs;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
    // At most five keys. Compare raw text so profile switches and external edits invalidate naturally.
    private final Map<String, String> encodedCache = new HashMap<>();
    private final Map<String, ActivityLayoutSnapshot> snapshotCache = new HashMap<>();

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

    private synchronized ActivityLayoutSnapshot load(String key)
    {
        final String encoded = configManager.getConfiguration(CustomGameTabsConfig.GROUP, key);
        if (!encodedCache.containsKey(key) || !Objects.equals(encodedCache.get(key), encoded))
        {
            encodedCache.put(key, encoded);
            snapshotCache.put(key, ActivityLayoutSnapshot.parse(encoded));
        }
        return snapshotCache.get(key);
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
