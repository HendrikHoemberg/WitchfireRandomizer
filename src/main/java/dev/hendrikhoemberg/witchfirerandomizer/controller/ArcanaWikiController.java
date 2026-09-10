package dev.hendrikhoemberg.witchfirerandomizer.controller;

import dev.hendrikhoemberg.witchfirerandomizer.model.Arcana;
import dev.hendrikhoemberg.witchfirerandomizer.model.Element;
import dev.hendrikhoemberg.witchfirerandomizer.model.Prophecy;
import dev.hendrikhoemberg.witchfirerandomizer.repository.ArcanaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Controller
@RequestMapping("/wiki/arcana")
public class ArcanaWikiController {

    private final ArcanaRepository arcanaRepository;

    public ArcanaWikiController(ArcanaRepository arcanaRepository) {
        this.arcanaRepository = arcanaRepository;
    }

    @GetMapping
    public String index(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String prophecyType,
            @RequestParam(required = false) Element element,
            @RequestParam(required = false, defaultValue = "cards") String viewMode,
            Model model) {
        model.addAttribute("activeTab", "arcana");
        model.addAttribute("pageTitle", "Arcana & Prophecies - Witchfire Companion");
        model.addAttribute("cards", arcanaRepository.search(search, prophecyType, element));
        model.addAttribute("prophecies", arcanaRepository.findAllProphecies());
        model.addAttribute("prophecyTypes", arcanaRepository.getAllProphecyTypes());
        model.addAttribute("elements", Element.values());
        model.addAttribute("selectedElement", element);
        model.addAttribute("selectedProphecyType", prophecyType);
        model.addAttribute("searchQuery", search != null ? search : "");
        model.addAttribute("viewMode", viewMode);
        return "wiki/arcana";
    }

    @GetMapping("/cards")
    public String getCards(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String prophecyType,
            @RequestParam(required = false) Element element,
            Model model) {
        List<Arcana> cards = arcanaRepository.search(search, prophecyType, element);
        model.addAttribute("cards", cards);
        return "wiki/fragments/arcana-grid :: arcanaGrid";
    }

    @GetMapping("/card/{id}")
    public String getCardModal(@PathVariable String id, Model model) {
        Arcana card = arcanaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found: " + id));
        model.addAttribute("card", card);
        return "wiki/fragments/arcana-modal :: arcanaModalContent";
    }

    @GetMapping("/prophecies")
    public String getProphecies(Model model) {
        model.addAttribute("prophecies", arcanaRepository.findAllProphecies());
        return "wiki/fragments/prophecy-list :: prophecyList";
    }
}
