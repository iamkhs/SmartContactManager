package com.iamkhs.contactmanager.controller;

import com.iamkhs.contactmanager.entities.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model){
        model.addAttribute("title", "Home");
        return "home";
    }

    @GetMapping("/about")
    public String about(Model model){
        model.addAttribute("title", "About");
        return "about";
    }

    @GetMapping("/login")
    public String login(){
        return "login";
    }


    @GetMapping("/signup")
    public String signup(Model model){
        model.addAttribute("title", "Register");
        model.addAttribute("user", new User());
        return "signup";
    }

}
