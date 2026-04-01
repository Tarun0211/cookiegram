package com.cookigram.controller;

import com.cookigram.dto.RegistrationDto;
import com.cookigram.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        @RequestParam(value = "registered", required = false) String registered,
                        Model model) {
        if (error != null) model.addAttribute("error", "Invalid username or password. Please try again.");
        if (logout != null) model.addAttribute("success", "You have been logged out successfully.");
        if (registered != null) model.addAttribute("success", "Account created successfully! Please login.");
        return "login";
    }
    
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("registration", new RegistrationDto());
        return "register";
    }
    
    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("registration") RegistrationDto registration,
                               BindingResult result, RedirectAttributes redirectAttributes, Model model) {
        if (result.hasErrors()) return "register";
        if (!registration.getPassword().equals(registration.getConfirmPassword())) {
            model.addAttribute("passwordError", "Passwords do not match");
            return "register";
        }
        if (userService.usernameExists(registration.getUsername())) {
            model.addAttribute("usernameError", "Username already exists");
            return "register";
        }
        if (registration.getEmail() != null && !registration.getEmail().isEmpty()
            && userService.emailExists(registration.getEmail())) {
            model.addAttribute("emailError", "Email already registered");
            return "register";
        }
        userService.registerNewCustomer(registration);
        redirectAttributes.addFlashAttribute("success", "Account created successfully!");
        return "redirect:/login?registered=true";
    }
}