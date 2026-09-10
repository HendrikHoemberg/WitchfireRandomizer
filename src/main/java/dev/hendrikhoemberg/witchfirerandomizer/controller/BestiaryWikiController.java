package dev.hendrikhoemberg.witchfirerandomizer.controller;

import dev.hendrikhoemberg.witchfirerandomizer.model.Enemy;
import dev.hendrikhoemberg.witchfirerandomizer.repository.EnemyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Controller
@RequestMapping("/wiki/bestiary")
public class BestiaryWikiController {

    private final EnemyRepository enemyRepository;

    public BestiaryWikiController(EnemyRepository enemyRepository) {
        this.enemyRepository = enemyRepository;
    }

    @GetMapping
    public String index(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer gnosis,
            @RequestParam(required = false) String location,
            @RequestParam(required = false, defaultValue = "name") String sort,
            @RequestParam(required = false) String modal,
            Model model) {
        model.addAttribute("activeTab", "bestiary");
        model.addAttribute("pageTitle", "Bestiary & Vulnerabilities - Witchfire Companion");
        model.addAttribute("enemies", enemyRepository.search(search, gnosis, null, location, sort));
        model.addAttribute("locations", enemyRepository.getAllLocations());
        model.addAttribute("gnosisLevels", List.of(0, 1, 2, 3, 4, 5, 6));
        model.addAttribute("selectedGnosis", gnosis);
        model.addAttribute("selectedLocation", location);
        model.addAttribute("sortCriteria", sort);
        model.addAttribute("searchQuery", search != null ? search : "");
        if (modal != null) {
            enemyRepository.findById(modal).ifPresent(e -> {
                model.addAttribute("enemy", e);
                model.addAttribute("initialModalOpen", true);
            });
        }
        return "wiki/bestiary";
    }

    @GetMapping("/enemies")
    public String getEnemies(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer gnosis,
            @RequestParam(required = false) String location,
            @RequestParam(required = false, defaultValue = "name") String sort,
            Model model) {
        List<Enemy> enemies = enemyRepository.search(search, gnosis, null, location, sort);
        model.addAttribute("enemies", enemies);
        return "wiki/fragments/enemy-grid :: enemyGrid";
    }

    @GetMapping("/enemy/{id}")
    public String getEnemyModal(@PathVariable String id, Model model) {
        Enemy enemy = enemyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enemy not found: " + id));
        model.addAttribute("enemy", enemy);
        return "wiki/fragments/enemy-modal :: enemyModalContent";
    }
}
