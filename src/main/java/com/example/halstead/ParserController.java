package com.example.halstead;

import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ParserController {

    private final HalsteadAnalyzer analyzer;

    public ParserController(HalsteadAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/analyze")
    public String analyze(@RequestParam("sourceCode") String sourceCode, Model model) {
        HalsteadResult result = analyzer.analyze(sourceCode);
        model.addAttribute("result", result);
        model.addAttribute("sourceCode", sourceCode);
        return "result";
    }
}