package dev.hendrikhoemberg.witchfirerandomizer.controller;

import dev.hendrikhoemberg.witchfirerandomizer.model.Prophecy;
import dev.hendrikhoemberg.witchfirerandomizer.repository.ArcanaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/wiki/prophecies")
public class PropheciesWikiController {

    private final ArcanaRepository arcanaRepository;

    public PropheciesWikiController(ArcanaRepository arcanaRepository) {
        this.arcanaRepository = arcanaRepository;
    }

    @GetMapping
    public String index(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String location,
            Model model) {
        model.addAttribute("activeTab", "prophecies");
        model.addAttribute("pageTitle", "Prophecies & Omens - Witchfire Companion");
        model.addAttribute("prophecies", filterProphecies(search, location));
        model.addAttribute("locations", arcanaRepository.getAllLocations());
        model.addAttribute("searchQuery", search != null ? search : "");
        model.addAttribute("selectedLocation", location);
        return "wiki/prophecies";
    }

    @GetMapping("/list")
    public String getList(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String location,
            Model model) {
        model.addAttribute("prophecies", filterProphecies(search, location));
        return "wiki/fragments/prophecy-list :: prophecyList";
    }

    private List<Prophecy> filterProphecies(String search, String location) {
        List<Prophecy> all = arcanaRepository.findAllProphecies();
        return all.stream()
                .filter(p -> {
                    if (search == null || search.isBlank()) return true;
                    String s = search.toLowerCase().trim();
                    boolean matchName = p.getName() != null && p.getName().toLowerCase().contains(s);
                    boolean matchDesc = p.getDescription() != null && p.getDescription().toLowerCase().contains(s);
                    boolean matchOmen = p.getOmenName() != null && p.getOmenName().toLowerCase().contains(s);
                    boolean matchOmenEff = p.getOmenEffect() != null && p.getOmenEffect().toLowerCase().contains(s);
                    boolean matchLoc = p.getLocation() != null && p.getLocation().toLowerCase().contains(s);
                    boolean matchType = p.getArcanaType() != null && p.getArcanaType().toLowerCase().contains(s);
                    return matchName || matchDesc || matchOmen || matchOmenEff || matchLoc || matchType;
                })
                .filter(p -> {
                    if (location == null || location.isBlank()) return true;
                    return p.getLocation() != null && p.getLocation().equalsIgnoreCase(location);
                })
                .toList();
    }
}
