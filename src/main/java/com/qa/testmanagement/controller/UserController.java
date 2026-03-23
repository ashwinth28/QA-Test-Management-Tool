package com.qa.testmanagement.controller;

import com.qa.testmanagement.model.User;
import com.qa.testmanagement.model.UserRole;
import com.qa.testmanagement.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/users")
    public String listUsers(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<User> userPage = userRepository.findAll(pageable);

        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("totalItems", userPage.getTotalElements());

        return "admin/users";
    }

    @GetMapping("/users/new")
    public String newUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", UserRole.values());
        model.addAttribute("action", "Create");
        return "admin/user-form";
    }

    @PostMapping("/users/save")
    public String saveUser(@ModelAttribute User user,
            @RequestParam String password,
            RedirectAttributes redirectAttributes) {
        try {
            // Check if username exists
            if (userRepository.existsByUsername(user.getUsername())) {
                redirectAttributes.addFlashAttribute("error", "Username already exists");
                return "redirect:/admin/users/new";
            }

            // Check if email exists
            if (userRepository.existsByEmail(user.getEmail())) {
                redirectAttributes.addFlashAttribute("error", "Email already exists");
                return "redirect:/admin/users/new";
            }

            // Set user details
            user.setPassword(passwordEncoder.encode(password));
            user.setCreatedAt(LocalDateTime.now());
            user.setEnabled(true);

            userRepository.save(user);

            redirectAttributes.addFlashAttribute("success", "User created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/users/edit/{id}")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + id));
        model.addAttribute("user", user);
        model.addAttribute("roles", UserRole.values());
        model.addAttribute("action", "Edit");
        return "admin/user-form";
    }

    @PostMapping("/users/update")
    public String updateUser(@ModelAttribute User user,
            @RequestParam(required = false) String newPassword,
            RedirectAttributes redirectAttributes) {
        try {
            User existingUser = userRepository.findById(user.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid user Id"));

            // Check if email is changed and already exists
            if (!existingUser.getEmail().equals(user.getEmail()) &&
                    userRepository.existsByEmail(user.getEmail())) {
                redirectAttributes.addFlashAttribute("error", "Email already exists");
                return "redirect:/admin/users/edit/" + user.getId();
            }

            // Update user details
            existingUser.setFullName(user.getFullName());
            existingUser.setEmail(user.getEmail());
            existingUser.setRole(user.getRole());
            existingUser.setEnabled(user.isEnabled());

            // Update password if provided
            if (newPassword != null && !newPassword.isEmpty()) {
                existingUser.setPassword(passwordEncoder.encode(newPassword));
            }

            userRepository.save(existingUser);

            redirectAttributes.addFlashAttribute("success", "User updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/toggle/{id}")
    @ResponseBody
    public String toggleUserStatus(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user Id"));

        // Don't allow disabling yourself
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (user.getUsername().equals(currentUsername)) {
            return "cannot-disable-self";
        }

        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        return user.isEnabled() ? "enabled" : "disabled";
    }

    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            // Don't allow deleting yourself
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid user Id"));

            if (user.getUsername().equals(currentUsername)) {
                redirectAttributes.addFlashAttribute("error", "You cannot delete your own account");
                return "redirect:/admin/users";
            }

            userRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting user: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
}