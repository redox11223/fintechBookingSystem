package com.redox.fintechBookingSystem.catalog;

import com.redox.fintechBookingSystem.catalog.dto.ServiceCategoryRequest;
import com.redox.fintechBookingSystem.catalog.dto.ServiceCategoryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
public class ServiceCategoryController {

  private final ServiceCategoryService categoryService;

  @GetMapping("/{id}")
  public ResponseEntity<ServiceCategoryResponse> getCategory(@PathVariable UUID id){
    var category=categoryService.getCategory(id);
    return ResponseEntity.ok(category);
  }

  @PostMapping
  public ResponseEntity<ServiceCategoryResponse> createCategory(@Valid @RequestBody ServiceCategoryRequest categoryRequest){
    var category=categoryService.registerCategory(categoryRequest);
    URI location=ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(category.id())
            .toUri();
    return ResponseEntity.created(location).body(category);
  }

  @PutMapping("/{id}")
  public ResponseEntity<ServiceCategoryResponse> updateCategory(@PathVariable UUID id,@Valid @RequestBody ServiceCategoryRequest categoryRequest){
    var category=categoryService.updateCategory(id,categoryRequest);
    return ResponseEntity.ok(category);
  }
}
