package com.guyshalev.Salt_security.controller;

import com.guyshalev.Salt_security.model.dto.ModelDTO;
import com.guyshalev.Salt_security.model.dto.RequestDTO;
import com.guyshalev.Salt_security.model.dto.ValidationResultDTO;
import com.guyshalev.Salt_security.service.ValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ValidationController {

    private final ValidationService validationService;

    public ValidationController(ValidationService validationService) {
        this.validationService = validationService;
    }

    @PostMapping("/models")
    public ResponseEntity<Void> saveModels(@RequestBody List<ModelDTO> models) {
        validationService.saveModels(models);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/models")
    public ResponseEntity<List<ModelDTO>> getAllModels() {
        List<ModelDTO> models = validationService.getAllModels();
        return ResponseEntity.ok(models);
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidationResultDTO> validateRequest(@RequestBody RequestDTO request) {
        ValidationResultDTO result = validationService.validateRequest(request);
        return ResponseEntity.ok(result);
    }
}