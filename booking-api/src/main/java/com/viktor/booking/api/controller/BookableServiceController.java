package com.viktor.booking.api.controller;

import com.viktor.booking.api.dto.BookableServiceCreateRequest;
import com.viktor.booking.api.dto.BookableServiceResponse;
import com.viktor.booking.application.service.BookableServiceService;
import com.viktor.booking.domain.model.BookableService;
import com.viktor.booking.application.exception.BookableServiceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BookableServiceController {

    private final BookableServiceService serviceService;

    public BookableServiceController(
            BookableServiceService serviceService
    ) {
        this.serviceService = serviceService;
    }

    @PostMapping("/api/services")
    public ResponseEntity<BookableServiceResponse> createService(
            @Valid @RequestBody BookableServiceCreateRequest request
    ) {
        BookableService createdService =
                serviceService.createService(
                        request.getName(),
                        request.getDescription(),
                        request.getDurationMinutes()
                );

        return ResponseEntity
                .status(201)
                .body(toResponse(createdService));
    }

    @GetMapping("/api/services")
    public List<BookableServiceResponse> getAllServices() {
        return serviceService.getAllServices()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/api/services/{id}")
    public ResponseEntity<BookableServiceResponse> getServiceById(
            @PathVariable("id") Long id
    ) {
        BookableService service =
                serviceService.getServiceById(id)
                        .orElseThrow(
                                () -> new BookableServiceNotFoundException(id)
                        );

        return ResponseEntity.ok(
                toResponse(service)
        );
    }

    @PutMapping("/api/services/{id}/activate")
    public ResponseEntity<BookableServiceResponse> activateServiceById(
            @PathVariable("id") Long id
    ) {
        BookableService service =
                serviceService.activateServiceById(id)
                        .orElseThrow(
                                () -> new BookableServiceNotFoundException(id)
                        );

        return ResponseEntity.ok(
                toResponse(service)
        );
    }

    @PutMapping("/api/services/{id}/deactivate")
    public ResponseEntity<BookableServiceResponse> deactivateServiceById(
            @PathVariable("id") Long id
    ) {
        BookableService service =
                serviceService.deactivateServiceById(id)
                        .orElseThrow(
                                () -> new BookableServiceNotFoundException(id)
                        );

        return ResponseEntity.ok(
                toResponse(service)
        );
    }

    private BookableServiceResponse toResponse(
            BookableService service
    ) {
        return new BookableServiceResponse(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getDurationMinutes(),
                service.isActive()
        );
    }
}