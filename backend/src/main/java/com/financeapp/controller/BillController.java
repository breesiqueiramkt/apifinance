package com.financeapp.controller;

import com.financeapp.dto.BillDtos.BillRequest;
import com.financeapp.dto.BillDtos.BillResponse;
import com.financeapp.security.UserPrincipal;
import com.financeapp.service.BillService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
@Tag(name = "Contas futuras")
public class BillController {

    private final BillService billService;

    @GetMapping
    public List<BillResponse> list(@AuthenticationPrincipal UserPrincipal user,
                                    @RequestParam(required = false) Integer nextDays) {
        return billService.list(user.getId(), nextDays);
    }

    @PostMapping
    public ResponseEntity<BillResponse> create(@AuthenticationPrincipal UserPrincipal user,
                                                 @Valid @RequestBody BillRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(billService.create(user.getId(), request));
    }

    @PutMapping("/{id}")
    public BillResponse update(@AuthenticationPrincipal UserPrincipal user,
                                @PathVariable Long id,
                                @Valid @RequestBody BillRequest request) {
        return billService.update(user.getId(), id, request);
    }

    @PostMapping("/{id}/pay")
    public BillResponse markAsPaid(@AuthenticationPrincipal UserPrincipal user,
                                    @PathVariable Long id,
                                    @RequestParam(required = false) Long accountId) {
        return billService.markAsPaid(user.getId(), id, accountId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id) {
        billService.delete(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
