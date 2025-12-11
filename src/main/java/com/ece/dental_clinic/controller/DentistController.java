package com.ece.dental_clinic.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DentistController {

    @GetMapping("/dentist/home")
    public String dentistHome() {
        return "dentist-home";
    }
}