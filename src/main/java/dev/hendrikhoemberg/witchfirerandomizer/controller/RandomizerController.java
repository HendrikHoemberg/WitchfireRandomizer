package dev.hendrikhoemberg.witchfirerandomizer.controller;

import dev.hendrikhoemberg.witchfirerandomizer.model.*;
import dev.hendrikhoemberg.witchfirerandomizer.repository.ItemRepository;
import dev.hendrikhoemberg.witchfirerandomizer.service.RandomizerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class RandomizerController {

    private final RandomizerService randomizerService;
    private final ItemRepository itemRepository;

    public RandomizerController(RandomizerService randomizerService, ItemRepository itemRepository) {
        this.randomizerService = randomizerService;
        this.itemRepository = itemRepository;
    }

    @GetMapping({"/", "/randomizer"})
    public String index(
            @RequestParam(required = false) String primary,
            @RequestParam(required = false) String secondary,
            @RequestParam(required = false) String demonic,
            @RequestParam(required = false) String melee,
            @RequestParam(required = false) String light,
            @RequestParam(required = false) String heavy,
            @RequestParam(required = false) String relic,
            @RequestParam(required = false) String fetish,
            @RequestParam(required = false) String ring,
            @RequestParam(required = false) String beads,
            Model model) {

        Loadout loadout;
        if (primary != null || melee != null || light != null) {
            loadout = restoreLoadoutFromParams(primary, secondary, demonic, melee, light, heavy, relic, fetish, ring, beads);
        } else {
            loadout = new Loadout();
        }

        model.addAttribute("loadout", loadout);
        model.addAttribute("activeTab", "randomizer");
        model.addAttribute("pageTitle", "Loadout Randomizer");
        model.addAttribute("elements", Element.values());
        model.addAttribute("allCategories", ItemCategory.values());
        model.addAttribute("statRequirements", calculateStatRequirements(loadout.getBeads()));
        model.addAttribute("activeElements", calculateActiveElements(loadout));
        model.addAttribute("allItems", itemRepository.findAll());
        return "randomizer/index";
    }

    @PostMapping("/randomizer/reroll")
    public String reroll(@ModelAttribute RandomizerRequest request, Model model) {
        Loadout loadout = randomizerService.generateRandomLoadout(request);
        model.addAttribute("loadout", loadout);
        model.addAttribute("request", request);
        model.addAttribute("elements", Element.values());
        model.addAttribute("statRequirements", calculateStatRequirements(loadout.getBeads()));
        model.addAttribute("activeElements", calculateActiveElements(loadout));
        return "randomizer/fragments/loadout-grid :: loadoutGrid";
    }

    @PostMapping("/randomizer/clear")
    public String clear(Model model) {
        Loadout loadout = new Loadout();
        model.addAttribute("loadout", loadout);
        model.addAttribute("elements", Element.values());
        model.addAttribute("statRequirements", calculateStatRequirements(Collections.emptyList()));
        model.addAttribute("activeElements", Collections.emptySet());
        return "randomizer/fragments/loadout-grid :: loadoutGrid";
    }

    @PostMapping("/randomizer/reroll-slot")
    public String rerollSlot(
            @RequestParam String slotKey,
            @RequestParam String slotLabel,
            @ModelAttribute RandomizerRequest request,
            Model model) {
        Item item = randomizerService.rerollSlot(slotKey, request);
        model.addAttribute("slotKey", slotKey);
        model.addAttribute("slotLabel", slotLabel);
        model.addAttribute("item", item);
        return "randomizer/fragments/slot-card :: slotCardFragment";
    }

    public static Set<Element> calculateActiveElements(Loadout loadout) {
        Set<Element> active = new HashSet<>();
        if (loadout == null) return active;
        List<Item> items = Arrays.asList(
                loadout.getPrimaryWeapon(),
                loadout.getSecondaryWeapon(),
                loadout.getDemonicWeapon(),
                loadout.getMeleeWeapon(),
                loadout.getLightSpell(),
                loadout.getHeavySpell(),
                loadout.getRelic(),
                loadout.getFetish(),
                loadout.getRing()
        );
        for (Item i : items) {
            if (i != null && i.getElement() != null) {
                active.add(i.getElement());
            }
        }
        return active;
    }

    public static Map<String, Integer> calculateStatRequirements(List<Bead> beads) {
        Map<String, Integer> reqs = new LinkedHashMap<>();
        reqs.put("Flesh", 0);
        reqs.put("Blood", 0);
        reqs.put("Mind", 0);
        reqs.put("Witchery", 0);
        reqs.put("Arsenal", 0);
        reqs.put("Faith", 0);
        if (beads != null) {
            for (Bead bead : beads) {
                if (bead != null && bead.getRequirements() != null) {
                    for (BeadRequirement sr : bead.getRequirements()) {
                        String s = sr.stat();
                        if (s != null && !s.isBlank()) {
                            String statName = s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
                            if (reqs.containsKey(statName)) {
                                reqs.put(statName, Math.max(reqs.get(statName), sr.value()));
                            }
                        }
                    }
                }
            }
        }
        return reqs;
    }

    private Loadout restoreLoadoutFromParams(
            String primary, String secondary, String demonic, String melee,
            String light, String heavy, String relic, String fetish, String ring, String beadsParam) {
        Loadout loadout = new Loadout();
        if (primary != null) itemRepository.findById(primary).filter(i -> i instanceof Weapon).ifPresent(i -> loadout.setPrimaryWeapon((Weapon) i));
        if (secondary != null) itemRepository.findById(secondary).filter(i -> i instanceof Weapon).ifPresent(i -> loadout.setSecondaryWeapon((Weapon) i));
        if (demonic != null) itemRepository.findById(demonic).filter(i -> i instanceof Weapon).ifPresent(i -> loadout.setDemonicWeapon((Weapon) i));
        if (melee != null) itemRepository.findById(melee).filter(i -> i instanceof MeleeWeapon).ifPresent(i -> loadout.setMeleeWeapon((MeleeWeapon) i));
        if (light != null) itemRepository.findById(light).filter(i -> i instanceof Spell).ifPresent(i -> loadout.setLightSpell((Spell) i));
        if (heavy != null) itemRepository.findById(heavy).filter(i -> i instanceof Spell).ifPresent(i -> loadout.setHeavySpell((Spell) i));
        if (relic != null) itemRepository.findById(relic).filter(i -> i instanceof MagicalItem).ifPresent(i -> loadout.setRelic((MagicalItem) i));
        if (fetish != null) itemRepository.findById(fetish).filter(i -> i instanceof MagicalItem).ifPresent(i -> loadout.setFetish((MagicalItem) i));
        if (ring != null) itemRepository.findById(ring).filter(i -> i instanceof MagicalItem).ifPresent(i -> loadout.setRing((MagicalItem) i));

        if (beadsParam != null && !beadsParam.isBlank()) {
            List<Bead> beads = new ArrayList<>();
            for (String bId : beadsParam.split(",")) {
                itemRepository.findById(bId.trim()).filter(i -> i instanceof Bead).ifPresent(i -> beads.add((Bead) i));
            }
            loadout.setBeads(beads);
        }

        // Fill any missing slots with random items
        RandomizerRequest fillReq = new RandomizerRequest();
        Map<String, String> currentMap = new HashMap<>();
        Map<String, Boolean> locks = new HashMap<>();
        if (loadout.getPrimaryWeapon() != null) { currentMap.put("primaryWeapon", loadout.getPrimaryWeapon().getId()); locks.put("primaryWeapon", true); }
        if (loadout.getSecondaryWeapon() != null) { currentMap.put("secondaryWeapon", loadout.getSecondaryWeapon().getId()); locks.put("secondaryWeapon", true); }
        if (loadout.getDemonicWeapon() != null) { currentMap.put("demonicWeapon", loadout.getDemonicWeapon().getId()); locks.put("demonicWeapon", true); }
        if (loadout.getMeleeWeapon() != null) { currentMap.put("meleeWeapon", loadout.getMeleeWeapon().getId()); locks.put("meleeWeapon", true); }
        if (loadout.getLightSpell() != null) { currentMap.put("lightSpell", loadout.getLightSpell().getId()); locks.put("lightSpell", true); }
        if (loadout.getHeavySpell() != null) { currentMap.put("heavySpell", loadout.getHeavySpell().getId()); locks.put("heavySpell", true); }
        if (loadout.getRelic() != null) { currentMap.put("relic", loadout.getRelic().getId()); locks.put("relic", true); }
        if (loadout.getFetish() != null) { currentMap.put("fetish", loadout.getFetish().getId()); locks.put("fetish", true); }
        if (loadout.getRing() != null) { currentMap.put("ring", loadout.getRing().getId()); locks.put("ring", true); }
        fillReq.setCurrentSlotItemIds(currentMap);
        fillReq.setLocks(locks);

        Loadout filled = randomizerService.generateRandomLoadout(fillReq);
        if (loadout.getBeads().isEmpty()) {
            loadout.setBeads(filled.getBeads());
        }
        if (loadout.getPrimaryWeapon() == null) loadout.setPrimaryWeapon(filled.getPrimaryWeapon());
        if (loadout.getSecondaryWeapon() == null) loadout.setSecondaryWeapon(filled.getSecondaryWeapon());
        if (loadout.getDemonicWeapon() == null) loadout.setDemonicWeapon(filled.getDemonicWeapon());
        if (loadout.getMeleeWeapon() == null) loadout.setMeleeWeapon(filled.getMeleeWeapon());
        if (loadout.getLightSpell() == null) loadout.setLightSpell(filled.getLightSpell());
        if (loadout.getHeavySpell() == null) loadout.setHeavySpell(filled.getHeavySpell());
        if (loadout.getRelic() == null) loadout.setRelic(filled.getRelic());
        if (loadout.getFetish() == null) loadout.setFetish(filled.getFetish());
        if (loadout.getRing() == null) loadout.setRing(filled.getRing());

        return loadout;
    }
}
