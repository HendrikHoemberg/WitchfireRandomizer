package dev.hendrikhoemberg.witchfirerandomizer.service;

import dev.hendrikhoemberg.witchfirerandomizer.model.*;
import dev.hendrikhoemberg.witchfirerandomizer.repository.ItemRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class RandomizerService {

    private final ItemRepository itemRepository;

    public RandomizerService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public Loadout generateRandomLoadout(RandomizerRequest request) {
        Loadout loadout = new Loadout();
        RandomizerRequest req = request != null ? request : new RandomizerRequest();

        // 1. Primary Weapon (Guaranteed)
        Weapon primary = resolveSlot("primaryWeapon", req, ItemCategory.WEAPON, Collections.emptySet(), Weapon.class, false);
        loadout.setPrimaryWeapon(primary);

        // 2. Secondary Weapon (cannot duplicate primary)
        Set<String> excludeForSecondary = new HashSet<>(req.getExcludedItemIds());
        if (primary != null) {
            excludeForSecondary.add(primary.getId());
        }
        Weapon secondary = resolveSlot("secondaryWeapon", req, ItemCategory.WEAPON, excludeForSecondary, Weapon.class, req.isEmptySlotMode());
        loadout.setSecondaryWeapon(secondary);

        // 3. Demonic Weapon
        Weapon demonic = resolveSlot("demonicWeapon", req, ItemCategory.DEMONIC_WEAPON, Collections.emptySet(), Weapon.class, req.isEmptySlotMode());
        loadout.setDemonicWeapon(demonic);

        // 4. Melee Weapon
        MeleeWeapon melee = resolveSlot("meleeWeapon", req, ItemCategory.MELEE_WEAPON, Collections.emptySet(), MeleeWeapon.class, req.isEmptySlotMode());
        loadout.setMeleeWeapon(melee);

        // 5. Light Spell
        Spell lightSpell = resolveSlot("lightSpell", req, ItemCategory.LIGHT_SPELL, Collections.emptySet(), Spell.class, req.isEmptySlotMode());
        loadout.setLightSpell(lightSpell);

        // 6. Heavy Spell
        Spell heavySpell = resolveSlot("heavySpell", req, ItemCategory.HEAVY_SPELL, Collections.emptySet(), Spell.class, req.isEmptySlotMode());
        loadout.setHeavySpell(heavySpell);

        // 7. Relic
        MagicalItem relic = resolveSlot("relic", req, ItemCategory.RELIC, Collections.emptySet(), MagicalItem.class, req.isEmptySlotMode());
        loadout.setRelic(relic);

        // 8. Fetish
        MagicalItem fetish = resolveSlot("fetish", req, ItemCategory.FETISH, Collections.emptySet(), MagicalItem.class, req.isEmptySlotMode());
        loadout.setFetish(fetish);

        // 9. Ring
        MagicalItem ring = resolveSlot("ring", req, ItemCategory.RING, Collections.emptySet(), MagicalItem.class, req.isEmptySlotMode());
        loadout.setRing(ring);

        // 10. Beads
        loadout.setBeads(resolveBeads(req));

        return loadout;
    }

    public Item rerollSlot(String slotName, RandomizerRequest request) {
        RandomizerRequest req = request != null ? request : new RandomizerRequest();
        return switch (slotName) {
            case "primaryWeapon" -> pickRandomItem(ItemCategory.WEAPON, req.getPreferredElements(), req.getExcludedItemIds(), null);
            case "secondaryWeapon" -> {
                Set<String> excludes = new HashSet<>(req.getExcludedItemIds());
                String primaryId = req.getCurrentSlotItemIds().get("primaryWeapon");
                if (primaryId != null) excludes.add(primaryId);
                yield pickRandomItem(ItemCategory.WEAPON, req.getPreferredElements(), excludes, null);
            }
            case "demonicWeapon" -> pickRandomItem(ItemCategory.DEMONIC_WEAPON, req.getPreferredElements(), req.getExcludedItemIds(), null);
            case "meleeWeapon" -> pickRandomItem(ItemCategory.MELEE_WEAPON, req.getPreferredElements(), req.getExcludedItemIds(), null);
            case "lightSpell" -> pickRandomItem(ItemCategory.LIGHT_SPELL, req.getPreferredElements(), req.getExcludedItemIds(), null);
            case "heavySpell" -> pickRandomItem(ItemCategory.HEAVY_SPELL, req.getPreferredElements(), req.getExcludedItemIds(), null);
            case "relic" -> pickRandomItem(ItemCategory.RELIC, req.getPreferredElements(), req.getExcludedItemIds(), null);
            case "fetish" -> pickRandomItem(ItemCategory.FETISH, req.getPreferredElements(), req.getExcludedItemIds(), null);
            case "ring" -> pickRandomItem(ItemCategory.RING, req.getPreferredElements(), req.getExcludedItemIds(), null);
            default -> null;
        };
    }

    @SuppressWarnings("unchecked")
    private <T extends Item> T resolveSlot(
            String slotName,
            RandomizerRequest req,
            ItemCategory category,
            Set<String> extraExclusions,
            Class<T> type,
            boolean canBeEmpty) {
        boolean isLocked = Boolean.TRUE.equals(req.getLocks().get(slotName));
        String currentId = req.getCurrentSlotItemIds().get(slotName);

        if (isLocked) {
            if (currentId != null && !currentId.isBlank()) {
                Optional<Item> lockedItem = itemRepository.findById(currentId);
                if (lockedItem.isPresent() && type.isInstance(lockedItem.get())) {
                    return (T) lockedItem.get();
                }
            }
            return null;
        }

        if (canBeEmpty && ThreadLocalRandom.current().nextDouble() < 0.35) {
            return null;
        }

        Set<String> exclusions = new HashSet<>(req.getExcludedItemIds());
        exclusions.addAll(extraExclusions);
        Item picked = pickRandomItem(category, req.getPreferredElements(), exclusions, null);
        return type.isInstance(picked) ? (T) picked : null;
    }

    private Item pickRandomItem(
            ItemCategory category,
            Set<Element> preferredElements,
            Set<String> excludedIds,
            Set<String> candidateIds) {
        List<Item> candidates = itemRepository.findByCategory(category);
        if (candidates.isEmpty()) {
            return null;
        }

        if (candidateIds != null && !candidateIds.isEmpty()) {
            candidates = candidates.stream()
                    .filter(i -> candidateIds.contains(i.getId()))
                    .collect(Collectors.toList());
        }

        if (excludedIds != null && !excludedIds.isEmpty()) {
            List<Item> nonExcluded = candidates.stream()
                    .filter(i -> !excludedIds.contains(i.getId()))
                    .collect(Collectors.toList());
            if (!nonExcluded.isEmpty()) {
                candidates = nonExcluded;
            }
        }

        if (preferredElements != null && !preferredElements.isEmpty()) {
            List<Item> preferred = candidates.stream()
                    .filter(i -> i.getElement() != null && preferredElements.contains(i.getElement()))
                    .collect(Collectors.toList());
            if (!preferred.isEmpty()) {
                candidates = preferred;
            }
        }

        int index = ThreadLocalRandom.current().nextInt(candidates.size());
        return candidates.get(index);
    }

    private List<Bead> resolveBeads(RandomizerRequest req) {
        int count = Math.max(1, Math.min(5, req.getBeadSlotCount()));
        List<Bead> eligible = new ArrayList<>(itemRepository.findEligibleBeads(req.getBeadUserStats()));

        Bead[] result = new Bead[count];
        boolean[] isSlotResolved = new boolean[count];
        Set<String> lockedBeadIds = new HashSet<>();

        for (int i = 0; i < count; i++) {
            String slotKey = "bead" + (i + 1);
            boolean isLocked = Boolean.TRUE.equals(req.getLocks().get(slotKey));
            if (isLocked) {
                isSlotResolved[i] = true;
                String currentId = req.getCurrentSlotItemIds().get(slotKey);
                if (currentId != null && !currentId.isBlank()) {
                    Optional<Item> item = itemRepository.findById(currentId);
                    if (item.isPresent() && item.get() instanceof Bead bead) {
                        result[i] = bead;
                        lockedBeadIds.add(bead.getId());
                    } else {
                        result[i] = null;
                    }
                } else {
                    result[i] = null;
                }
            }
        }

        Set<String> allExclusions = new HashSet<>(req.getExcludedItemIds());
        allExclusions.addAll(lockedBeadIds);

        List<Bead> availablePool = eligible.stream()
                .filter(b -> !allExclusions.contains(b.getId()))
                .collect(Collectors.toList());

        if (availablePool.size() < count) {
            availablePool = eligible.stream()
                    .filter(b -> !lockedBeadIds.contains(b.getId()))
                    .collect(Collectors.toList());
        }

        Collections.shuffle(availablePool);
        int poolIndex = 0;

        for (int i = 0; i < count; i++) {
            if (isSlotResolved[i]) {
                continue;
            }
            if (req.isEmptySlotMode() && ThreadLocalRandom.current().nextDouble() < 0.35) {
                result[i] = null;
            } else if (poolIndex < availablePool.size()) {
                result[i] = availablePool.get(poolIndex++);
            } else {
                result[i] = null;
            }
        }

        return Arrays.asList(result);
    }
}
