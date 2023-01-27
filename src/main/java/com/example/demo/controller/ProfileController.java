package com.example.demo.controller;

import com.example.demo.model.Profile;
import com.example.demo.model.request.ProfileCreationRequest;
import com.example.demo.model.response.PaginatedProfileResponse;
import com.example.demo.service.RepositoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@CrossOrigin("*")
@RequestMapping(value = "/profile")
public class ProfileController {
    private final RepositoryService repositoryService;
    private final LibraryController libraryController;

    @GetMapping("")
    public String getProfile(Model model, @RequestParam(required = false) String keyword, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "3") int size) {
        try {
            List<Profile> profileList;
            Pageable paging = PageRequest.of(page - 1, size);
            ResponseEntity<PaginatedProfileResponse> pageProfile;
            if (keyword == null || keyword.equalsIgnoreCase("")) {
                pageProfile = libraryController.getProfile(paging);
            } else {
                pageProfile = libraryController.getProfileWithFilter(keyword, paging);
                model.addAttribute("keyword", keyword);
            }
            profileList = pageProfile.getBody().getProfileList();
            model.addAttribute("profiles", profileList);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalItems", pageProfile.getBody().getNumberOfItems());
            model.addAttribute("totalPages", pageProfile.getBody().getNumberOfPages());
            model.addAttribute("pageSize", size);
            model.addAttribute("profileCreationRequest", new ProfileCreationRequest());
        } catch (Exception e) {
            model.addAttribute("profileCreationRequest", new ProfileCreationRequest());
            model.addAttribute("message", e.getMessage());
        }
        return "profile/profile";
    }

    @PostMapping("/create")
    public String createProfile(ProfileCreationRequest profileCreationRequest, RedirectAttributes redirectAttributes) {
        try {
            ResponseEntity<Profile> response = libraryController.createProfile(profileCreationRequest);
            System.out.println(response.getStatusCode());
            redirectAttributes.addFlashAttribute("message", "The Profile has been saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
        }
        return "redirect:";
    }

    @PostMapping("/update")
    public String updateProfile(ProfileCreationRequest profileCreationRequest, RedirectAttributes redirectAttributes) {
        try {
            ResponseEntity<Profile> response = libraryController.updateProfile(profileCreationRequest.getId(), profileCreationRequest);
            System.out.println(response.getStatusCode());
            redirectAttributes.addFlashAttribute("message", "The Profile has been updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
        }
        return "redirect:";
    }

    @GetMapping("/delete/{id}")
    public String deleteProfile(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            repositoryService.deleteProfile(id);
            redirectAttributes.addFlashAttribute("message", "The Profile with id=" + id + " has been deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
        }
        return "redirect:../";
    }

}
