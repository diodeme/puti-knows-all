package com.puti.code.server.controller;

import com.puti.code.base.config.AppConfig;
import com.puti.code.repository.tracker.DependencyTracker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
@Slf4j
public class ProjectController {

    @GetMapping("/projects")
    public ResponseEntity<?> listProjects() {
        try (DependencyTracker tracker = new DependencyTracker()) {
            List<Map<String, String>> projects = tracker.listProjects();
            return ResponseEntity.ok(projects);
        } catch (Exception e) {
            log.error("Failed to list projects", e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/config")
    public ResponseEntity<?> getProjectConfig() {
        AppConfig config = AppConfig.getInstance();
        return ResponseEntity.ok(Map.of(
                "project_id", config.getProjectId() != null ? config.getProjectId() : "",
                "branch", config.getBranch() != null ? config.getBranch() : ""
        ));
    }
}
