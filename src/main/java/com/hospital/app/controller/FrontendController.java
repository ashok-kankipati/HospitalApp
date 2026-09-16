package com.hospital.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Spring serves nested static files, but does not infer a directory index for /app. */
@Controller
public class FrontendController {
    @GetMapping({"/app", "/app/"})
    public String workspace() {
        return "forward:/app/index.html";
    }
}
