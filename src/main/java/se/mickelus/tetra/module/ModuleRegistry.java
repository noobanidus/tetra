package se.mickelus.tetra.module;

import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se.mickelus.tetra.data.DataManager;
import se.mickelus.tetra.module.data.ModuleData;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class ModuleRegistry {
    private static final Logger logger = LogManager.getLogger();

    public static ModuleRegistry instance;

    private Map<ResourceLocation, BiFunction<ResourceLocation, ModuleData, ItemModule>> moduleConstructors;
    private Map<ResourceLocation, ItemModule> moduleMap;

    public ModuleRegistry() {
        instance = this;

        moduleConstructors = new HashMap<>();
        moduleMap = Collections.emptyMap();

        DataManager.moduleData.onReload(() -> {
            setupModules(DataManager.moduleData.getData());
            logger.debug(moduleMap);
        });
    }

    private void setupModules(Map<ResourceLocation, ModuleData> data) {
        moduleMap = data.entrySet().stream()
                .filter(entry -> validateModuleData(entry.getKey(), entry.getValue()))
                .flatMap(entry -> expandEntry(entry).stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> setupModule(entry.getKey(), entry.getValue())
                ));
    }

    private boolean validateModuleData(ResourceLocation identifier, ModuleData data) {
        if (data == null) {
            logger.warn("Failed to create module from module data '{}': Data is null (probably due to it failing to parse)",
                    identifier);
            return false;
        }

        if (!moduleConstructors.containsKey(data.type)) {
            logger.warn("Failed to create module from module data '{}': Unknown type '{}'", identifier, data.type);
            return false;
        }

        if (data.slots == null || data.slots.length < 1) {
            logger.warn("Failed to create module from module data '{}': Slots field is empty",
                    identifier);
            return false;
        }

        return true;
    }

    // todo: hacky stuff to get multislot modules to work, there has to be another way
    private Collection<Pair<ResourceLocation, ModuleData>> expandEntry(Map.Entry<ResourceLocation, ModuleData> entry) {
        ModuleData moduleData = entry.getValue();
        if (moduleData.slotSuffixes.length > 0) {
            ArrayList<Pair<ResourceLocation, ModuleData>> result = new ArrayList<>(moduleData.slots.length);
            for (int i = 0; i < moduleData.slots.length; i++) {
                ModuleData dataCopy = moduleData.shallowCopy();
                dataCopy.slots = new String[] { moduleData.slots[i] };
                dataCopy.slotSuffixes = new String[] { moduleData.slotSuffixes[i] };

                ResourceLocation suffixedIdentifier = new ResourceLocation(
                        entry.getKey().getNamespace(),
                        entry.getKey().getPath() + moduleData.slotSuffixes[i]);

                result.add(new ImmutablePair<>(suffixedIdentifier, dataCopy));
            }

            return result;
        }
        return Collections.singletonList(new ImmutablePair<>(entry.getKey(), entry.getValue()));
    }

    private ItemModule setupModule(ResourceLocation identifier, ModuleData data) {
        return moduleConstructors.get(data.type).apply(identifier, data);
    }

    public void registerModuleType(ResourceLocation identifier, BiFunction<ResourceLocation, ModuleData, ItemModule> constructor) {
        moduleConstructors.put(identifier, constructor);
    }


    public ItemModule getModule(ResourceLocation identifier) {
        return moduleMap.get(identifier);
    }

    public Collection<ItemModule> getAllModules() {
        return moduleMap.values();
    }
}
