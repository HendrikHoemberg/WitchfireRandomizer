package dev.hendrikhoemberg.witchfirerandomizer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LegalController {

    @GetMapping("/privacy")
    public String privacy(Model model) {
        model.addAttribute("pageTitle", "Privacy Policy");
        model.addAttribute("activeTab", "privacy");
        return "legal/privacy";
    }

    @GetMapping("/impressum")
    public String impressum(Model model) {
        model.addAttribute("pageTitle", "Impressum / Legal Notice");
        model.addAttribute("activeTab", "impressum");
        return "legal/impressum";
    }
}
