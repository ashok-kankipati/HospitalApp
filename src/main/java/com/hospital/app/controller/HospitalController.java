package com.hospital.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping("/")
public class HospitalController {

    @GetMapping({"", "/", "/index.html"})
    public RedirectView root() {
        return new RedirectView("/app/", true);
    }

    @GetMapping("/app")
    public RedirectView appRoot() {
        return new RedirectView("/app/", true);
    }

    @GetMapping("/api/health")
    public String health() {
        return "Hospital Management System is running!";
    }

    @GetMapping("/api/")
    public String welcome() {
        return "Welcome to Hospital Management System";
    }
}
