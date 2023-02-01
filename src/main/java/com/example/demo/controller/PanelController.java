package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.model.request.PanelCreationRequest;
import com.example.demo.model.request.PanelSelectionDto;
import com.example.demo.model.response.PaginatedPanelResponse;
import com.example.demo.repository.PanelRepository;
import com.example.demo.service.LedService;
import com.example.demo.service.RepositoryService;
import com.example.demo.utils.FileUtils;
import com.example.demo.utils.OSValidator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@CrossOrigin("*")
@RequestMapping(value = "/panel")
public class PanelController {
    private final RepositoryService repositoryService;
    private LibraryController libraryController;

    private static final Logger logger = LoggerFactory.getLogger(PanelController.class);
    private LedService ledService = new LedService();
    private final PanelRepository panelRepository;

    @GetMapping("")
    public String getPanel(Model model,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "3") int size) {
        try {
            libraryController = new LibraryController(this.repositoryService);
            Pageable paging = PageRequest.of(page - 1, size);
            List<Panel> activePanels = FileUtils.getPanelsList();
            List<Panel> inactivePanels = repositoryService.readPanels();
            inactivePanels.removeAll(activePanels);
            for (Panel ipanel:inactivePanels) {
                ipanel.setStatus(PanelStatus.DEACTIVATED);
                panelRepository.save(ipanel);
            }
            for (Panel ipanel:activePanels) {
                ipanel.setStatus(PanelStatus.ACTIVE);
                panelRepository.save(ipanel);
            }
            ResponseEntity<PaginatedPanelResponse> pagePanel;
            if (keyword == null || keyword.equalsIgnoreCase("")) {
                pagePanel = libraryController.getPanel(paging);
            } else {
                pagePanel = libraryController.getPanelWithFilter(keyword, paging);
                model.addAttribute("keyword", keyword);
            }

            List<Panel> pagedPanels = pagePanel.getBody().getPanelList();
            inactivePanels = repositoryService.readPanels();
            System.out.println("Total activePanels Found : " + activePanels.size());
            model.addAttribute("panelList", activePanels);
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

    @PostMapping("/create")
    public String createPanel(PanelCreationRequest panelCreationRequest, RedirectAttributes redirectAttributes) {
        try {
            panelCreationRequest.setStatus(PanelStatus.ACTIVE.toString());
            System.out.println("createPanel " + panelCreationRequest);
            ResponseEntity<Panel> response = libraryController.createPanel(panelCreationRequest);
            System.out.println(response.getStatusCode());
            redirectAttributes.addFlashAttribute("message", "The Panel has been saved successfully!");
        } catch (Exception e) {
            System.out.println("createPanel " + e.getMessage());
            redirectAttributes.addFlashAttribute("message", e.getMessage());
        }
        return "redirect:";
    }

    @GetMapping("/fetch/{id}")
    @ResponseBody
    public Optional<Panel> fetch(@PathVariable("id") Long id) {
        logger.info("Panel has been fetched. Panel id: " + id);
        return Optional.ofNullable(repositoryService.getPanel(id));
    }

    @PostMapping("/update")
    public String updatePanel(PanelCreationRequest panelCreationRequest, RedirectAttributes redirectAttributes) {
        logger.info("updatePanel: " + panelCreationRequest.toString());
        libraryController = new LibraryController(repositoryService);
        try {
            ResponseEntity<Panel> response = libraryController.updatePanel(panelCreationRequest.getId(), panelCreationRequest);
            logger.info("Panel has been updated. Panel id: " + response.getBody().getId());
            redirectAttributes.addFlashAttribute("message", "The Panel has been updated successfully!");
        } catch (Exception e) {
            logger.info("Panel update failed. ERROR : " + e);
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
    public ResponseEntity handleFileUpload(@RequestParam("file") MultipartFile file,
                                           @RequestParam("panel") String panel) {
        String fileName = file.getOriginalFilename();
        String filePath = "/home/pi/Application/Uploads/" + fileName;
        try {
            if (OSValidator.isWindows()) {
                file.transferTo(new File("D:\\upload\\" + fileName));
            } else {
                file.transferTo(new File(filePath));
                ledService.setFilePath(filePath);
                ledService.setPanelId(panel);
                ledService.run();
            }
        } catch (Exception e) {
            System.out.println("FileUpload Error " + e);
        }
        //TODO.. microcontroller isnt accepting Byte Array of images. Need image to micro controller data type

        //TODO this doesn't work anymore... creating information when uploading image to Panel while checking
//        new InformationController(repositoryService).createInformation(new InformationCreationRequest(name, type, file, filePath, profileId), null);
        return ResponseEntity.ok(filePath + " File uploaded successfully");
    }
}