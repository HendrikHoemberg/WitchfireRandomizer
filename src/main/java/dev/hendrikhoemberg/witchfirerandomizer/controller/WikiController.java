package dev.hendrikhoemberg.witchfirerandomizer.controller;

import dev.hendrikhoemberg.witchfirerandomizer.model.Element;
import dev.hendrikhoemberg.witchfirerandomizer.model.Item;
import dev.hendrikhoemberg.witchfirerandomizer.model.ItemCategory;
import dev.hendrikhoemberg.witchfirerandomizer.repository.ItemRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/wiki")
public class WikiController {

    private final ItemRepository itemRepository;

    public WikiController(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("activeTab", "wiki");
        model.addAttribute("pageTitle", "Item Wiki");
        model.addAttribute("categories", ItemCategory.values());
        model.addAttribute("elements", Element.values());
        model.addAttribute("selectedCategory", ItemCategory.WEAPON);
        model.addAttribute("items", itemRepository.findByCategory(ItemCategory.WEAPON));
        return "wiki/index";
    }

    @GetMapping("/items")
    public String getItems(
            @RequestParam(required = false) ItemCategory category,
            @RequestParam(required = false) Element element,
            @RequestParam(required = false) String search,
            Model model) {
        List<Item> filtered = itemRepository.search(category, element, search);
        model.addAttribute("items", filtered);
        return "wiki/fragments/item-grid :: itemGrid";
    }

    @GetMapping("/item/{id}")
    public String getItemModal(@PathVariable String id, Model model) {
        Item item = itemRepository.findById(id).orElseThrow();
        model.addAttribute("item", item);
        return "wiki/fragments/item-modal :: itemModalContent";
    }
}
