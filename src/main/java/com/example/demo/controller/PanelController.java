package com.example.demo.controller;

import com.example.demo.model.Information;
import com.example.demo.model.Panel;
import com.example.demo.model.PanelStatus;
import com.example.demo.model.request.PanelCreationRequest;
import com.example.demo.model.response.PaginatedPanelResponse;
import com.example.demo.service.RepositoryService;
import com.example.demo.service.individual.IndividualPanelsService;
import com.example.demo.utils.FileUtils;
import com.example.demo.utils.OSValidator;
import com.pi4j.util.Console;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@CrossOrigin("*")
@RequestMapping(value = "/panel")
public class PanelController {
    private static final Logger logger = LoggerFactory.getLogger(PanelController.class);
    @Autowired
    private final RepositoryService repositoryService;
    @Autowired
    private final IndividualPanelsService individualPanelsService;

    public Console console = new Console();
    @Autowired
    private LibraryController libraryController;


    @GetMapping("")
    public String getPanel(Model model, @RequestParam(required = false) String keyword, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "3") int size) {
//        init(); TODO maybe init.. the panels don't refresh when panel db is truncated
        try {
            Pageable paging = PageRequest.of(page - 1, size);

            ResponseEntity<PaginatedPanelResponse> pagePanel;
            if (keyword == null || keyword.equalsIgnoreCase("")) {
                pagePanel = libraryController.getPanel(paging);
            } else {
                pagePanel = libraryController.getPanelWithFilter(keyword, paging);
                model.addAttribute("keyword", keyword);
            }
            List<Panel> currentActivePanels = repositoryService.getPanelsWithStatus(PanelStatus.ACTIVE);
            List<Panel> pagedPanels = pagePanel.getBody().getPanelList();
            model.addAttribute("panelList", currentActivePanels);
            model.addAttribute("panels", pagedPanels);
            model.addAttribute("profiles", repositoryService.getProfile());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalItems", pagePanel.getBody().getNumberOfItems());
            model.addAttribute("totalPages", pagePanel.getBody().getNumberOfPages());
            model.addAttribute("pageSize", size);
        } catch (Exception e) {
            System.out.println("getPanel ERROR : " + e);
            model.addAttribute("message", e.getMessage());
        }
        model.addAttribute("panelCreationRequest", new PanelCreationRequest());
        return "panel/panel";
    }

    @GetMapping("/fetch/{id}")
    @ResponseBody
    public Optional<Panel> fetch(@PathVariable("id") Long id) {
        return Optional.ofNullable(repositoryService.getPanel(id));
    }

    @PostMapping("/update")
    public String updatePanel(PanelCreationRequest panelCreationRequest, RedirectAttributes redirectAttributes) {
        try {
            ResponseEntity<Panel> response = libraryController.updatePanel(panelCreationRequest.getId(), panelCreationRequest);
            console.println(" ==>> Panel has been updated. Panel id: " + response.getBody().getId());
            redirectAttributes.addFlashAttribute("message", "The Panel has been updated successfully!");
        } catch (Exception e) {
            console.println(" ==>> Panel update failed. ERROR : " + e);
            redirectAttributes.addFlashAttribute("message", e.getMessage());
        }
        return "redirect:";
    }

    @GetMapping("/delete/{id}")
    public String deletePanel(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            repositoryService.deletePanel(id);
            redirectAttributes.addFlashAttribute("message", "The Panel with id=" + id + " has been deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
        }
        return "redirect:../";
    }

    @PostMapping("/upload")
    public ResponseEntity<String> handleFileUpload(@RequestParam("file") MultipartFile file, @RequestParam("panel") String panel) {
        console.clearScreen();
        console.title("<-- handleFileUpload -->", "STARTED");
        console.println("\n[handleFileUpload API TRIGGERED]");
        String fileName = file.getOriginalFilename();
        String filePath = FileUtils.createFileDir(fileName);
        Panel panel1 = repositoryService.getPanel(Long.parseLong(panel));
        try {
            if (OSValidator.isWindows()) {
                file.transferTo(new File("D:\\upload\\" + fileName));
            } else {
                file.transferTo(new File(filePath));
            }
        } catch (Exception e) {
            console.println("FileUpload Error " + e);
        }
        Information info = new Information(0L, fileName, FileUtils.getFileType(fileName), filePath, null, "10", null);
//        String execute = individualPanelsService.execute(info, serialCommunication.getIndexFromDevice(panel1.getDevice()));
        return ResponseEntity.ok("TO BE CHANGED!");
    }

    @GetMapping("/clearScreen")
    @ResponseBody
    public ResponseEntity<String> clearPanel() {
        try {
            individualPanelsService.clearAllScreens();
            return ResponseEntity.ok("Panel Reset completed");
        } catch (Exception e) {
            System.out.println("clearPanel message" + e.getMessage());
            return ResponseEntity.ok("Panel Reset Error");
        }
    }
}
