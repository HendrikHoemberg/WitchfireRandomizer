package dev.hendrikhoemberg.witchfirerandomizer.controller;

import dev.hendrikhoemberg.witchfirerandomizer.model.Element;
import dev.hendrikhoemberg.witchfirerandomizer.model.Item;
import dev.hendrikhoemberg.witchfirerandomizer.model.ItemCategory;
import dev.hendrikhoemberg.witchfirerandomizer.repository.ItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Controller
@RequestMapping("/wiki")
public class WikiController {

    private final ItemRepository itemRepository;

    public WikiController(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @GetMapping
    public String index(
            @RequestParam(required = false) ItemCategory category,
            @RequestParam(required = false) Element element,
            @RequestParam(required = false) Boolean noElement,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "category") String sort,
            Model model) {
        model.addAttribute("activeTab", "wiki");
        model.addAttribute("pageTitle", "Item Wiki");
        model.addAttribute("categories", ItemCategory.values());
        model.addAttribute("elements", Element.values());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedElement", element);
        model.addAttribute("noElement", noElement);
        model.addAttribute("searchQuery", search != null ? search : "");
        model.addAttribute("sortCriteria", sort);

        List<Item> items = itemRepository.search(category, element, noElement, search, sort);
        model.addAttribute("items", items);
        model.addAttribute("groupedItems", groupByCategory(items));
        return "wiki/index";
    }

    @GetMapping("/items")
    public String getItems(
            @RequestParam(required = false) ItemCategory category,
            @RequestParam(required = false) Element element,
            @RequestParam(required = false) Boolean noElement,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "category") String sort,
            Model model) {
        List<Item> filtered = itemRepository.search(category, element, noElement, search, sort);
        model.addAttribute("items", filtered);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("sortCriteria", sort);
        model.addAttribute("groupedItems", groupByCategory(filtered));
        return "wiki/fragments/item-grid :: itemGrid";
    }

    private Map<ItemCategory, List<Item>> groupByCategory(List<Item> items) {
        Map<ItemCategory, List<Item>> grouped = new LinkedHashMap<>();
        for (ItemCategory cat : ItemCategory.values()) {
            List<Item> catItems = items.stream().filter(i -> i.getCategory() == cat).toList();
            if (!catItems.isEmpty()) {
                grouped.put(cat, catItems);
            }
        }
        return grouped;
    }

    @GetMapping("/item/{id}")
    public String getItemModal(@PathVariable String id, Model model) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found: " + id));
        model.addAttribute("item", item);
        return "wiki/fragments/item-modal :: itemModalContent";
    }
}
