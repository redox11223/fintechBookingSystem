package com.redox.fintechBookingSystem.catalog;

import com.redox.fintechBookingSystem.catalog.dto.FinancialServiceRequest;
import com.redox.fintechBookingSystem.catalog.dto.FinancialServiceResponse;
import com.redox.fintechBookingSystem.catalog.dto.FinancialServiceUpdate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/services")
public class FinancialServiceController {
  private final FinancialServiceService financialService;

  @PostMapping
  public ResponseEntity<FinancialServiceResponse> createFinancialService(@Valid @RequestBody FinancialServiceRequest serviceRequest){
    var service=financialService.registerFinancialService(serviceRequest);
    URI location= ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(service.id())
            .toUri();
    return ResponseEntity.created(location).body(service);
  }

  @PutMapping("/{id}")
  public ResponseEntity<FinancialServiceResponse> updateFinancialService(@PathVariable UUID id, @Valid @RequestBody FinancialServiceUpdate serviceUpdate){
    var service=financialService.updateFinancialService(id,serviceUpdate);
    return ResponseEntity.ok(service);
  }

  @GetMapping("/{id}")
  public ResponseEntity<FinancialServiceResponse> getFinancialServiceById(@PathVariable UUID id){
    var service= financialService.getFinancialService(id);
    return ResponseEntity.ok(service);
  }

  @GetMapping
  public ResponseEntity<List<FinancialServiceResponse>> get
}
