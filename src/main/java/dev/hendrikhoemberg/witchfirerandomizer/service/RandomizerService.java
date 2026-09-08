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

        // 1. Primary Weapon
        Weapon primary = resolveSlot(
                "primaryWeapon",
                req,
                ItemCategory.WEAPON,
                Collections.emptySet(),
                Weapon.class
        );
        loadout.setPrimaryWeapon(primary);

        // 2. Secondary Weapon (cannot duplicate primary)
        Set<String> excludeForSecondary = new HashSet<>(req.getExcludedItemIds());
        if (primary != null) {
            excludeForSecondary.add(primary.getId());
        }
        Weapon secondary = resolveSlotWithCustomExclusions(
                "secondaryWeapon",
                req,
                ItemCategory.WEAPON,
                excludeForSecondary,
                Weapon.class
        );
        loadout.setSecondaryWeapon(secondary);

        // 3. Demonic Weapon
        Weapon demonic = resolveSlot(
                "demonicWeapon",
                req,
                ItemCategory.DEMONIC_WEAPON,
                Collections.emptySet(),
                Weapon.class
        );
        loadout.setDemonicWeapon(demonic);

        // 4. Melee Weapon
        MeleeWeapon melee = resolveSlot(
                "meleeWeapon",
                req,
                ItemCategory.MELEE_WEAPON,
                Collections.emptySet(),
                MeleeWeapon.class
        );
        loadout.setMeleeWeapon(melee);

        // 5. Light Spell
        Spell lightSpell = resolveSlot(
                "lightSpell",
                req,
                ItemCategory.LIGHT_SPELL,
                Collections.emptySet(),
                Spell.class
        );
        loadout.setLightSpell(lightSpell);

        // 6. Heavy Spell
        Spell heavySpell = resolveSlot(
                "heavySpell",
                req,
                ItemCategory.HEAVY_SPELL,
                Collections.emptySet(),
                Spell.class
        );
        loadout.setHeavySpell(heavySpell);

        // 7. Relic
        MagicalItem relic = resolveSlot(
                "relic",
                req,
                ItemCategory.RELIC,
                Collections.emptySet(),
                MagicalItem.class
        );
        loadout.setRelic(relic);

        // 8. Fetish
        MagicalItem fetish = resolveSlot(
                "fetish",
                req,
                ItemCategory.FETISH,
                Collections.emptySet(),
                MagicalItem.class
        );
        loadout.setFetish(fetish);

        // 9. Ring
        MagicalItem ring = resolveSlot(
                "ring",
                req,
                ItemCategory.RING,
                Collections.emptySet(),
                MagicalItem.class
        );
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

    private <T extends Item> T resolveSlot(
            String slotName,
            RandomizerRequest req,
            ItemCategory category,
            Set<String> extraExclusions,
            Class<T> type) {
        Set<String> exclusions = new HashSet<>(req.getExcludedItemIds());
        exclusions.addAll(extraExclusions);
        return resolveSlotWithCustomExclusions(slotName, req, category, exclusions, type);
    }

    @SuppressWarnings("unchecked")
    private <T extends Item> T resolveSlotWithCustomExclusions(
            String slotName,
            RandomizerRequest req,
            ItemCategory category,
            Set<String> exclusions,
            Class<T> type) {
        boolean isLocked = Boolean.TRUE.equals(req.getLocks().get(slotName));
        String currentId = req.getCurrentSlotItemIds().get(slotName);

        if (isLocked && currentId != null) {
            Optional<Item> lockedItem = itemRepository.findById(currentId);
            if (lockedItem.isPresent() && type.isInstance(lockedItem.get())) {
                return (T) lockedItem.get();
            }
        }

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

        // Apply candidate pool restriction if any
        if (candidateIds != null && !candidateIds.isEmpty()) {
            candidates = candidates.stream()
                    .filter(i -> candidateIds.contains(i.getId()))
                    .collect(Collectors.toList());
        }

        // Apply exclusions (only if it doesn't eliminate all candidates)
        if (excludedIds != null && !excludedIds.isEmpty()) {
            List<Item> nonExcluded = candidates.stream()
                    .filter(i -> !excludedIds.contains(i.getId()))
                    .collect(Collectors.toList());
            if (!nonExcluded.isEmpty()) {
                candidates = nonExcluded;
            }
        }

        // Apply preferred elements (if any candidate matches)
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

        // Filter exclusions if possible
        if (!req.getExcludedItemIds().isEmpty()) {
            List<Bead> nonExcluded = eligible.stream()
                    .filter(b -> !req.getExcludedItemIds().contains(b.getId()))
                    .collect(Collectors.toList());
            if (nonExcluded.size() >= count) {
                eligible = nonExcluded;
            }
        }

        Collections.shuffle(eligible);
        return eligible.stream().limit(count).collect(Collectors.toList());
    }
}
