package com.qa.testmanagement.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UIController {

    @GetMapping("/index")
    public String index() {
        return "index";
    }

    @GetMapping("/create-testcase")
    public String createTestCase() {
        return "create-testcase";
    }

    @GetMapping("/execution-history")
    public String executionHistory() {
        return "execution-history";
    }

    @GetMapping("/upload-page")
    public String uploadPage() {
        return "upload";
    }

    @GetMapping("/help")
    public String help() {
        return "help";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }
}