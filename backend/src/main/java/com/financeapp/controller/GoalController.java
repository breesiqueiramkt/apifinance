package com.financeapp.controller;

import com.financeapp.dto.GoalDtos.*;
import com.financeapp.security.UserPrincipal;
import com.financeapp.service.GoalService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
@Tag(name = "Metas financeiras")
public class GoalController {

    private final GoalService goalService;

    @GetMapping
    public List<GoalResponse> list(@AuthenticationPrincipal UserPrincipal user) {
        return goalService.list(user.getId());
    }

    @PostMapping
    public ResponseEntity<GoalResponse> create(@AuthenticationPrincipal UserPrincipal user,
                                                 @Valid @RequestBody GoalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(goalService.create(user.getId(), request));
    }

    @PutMapping("/{id}")
    public GoalResponse update(@AuthenticationPrincipal UserPrincipal user,
                                @PathVariable Long id,
                                @Valid @RequestBody GoalRequest request) {
        return goalService.update(user.getId(), id, request);
    }

    @PostMapping("/{id}/contribute")
    public GoalResponse contribute(@AuthenticationPrincipal UserPrincipal user,
                                    @PathVariable Long id,
                                    @Valid @RequestBody ContributeRequest request) {
        return goalService.contribute(user.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id) {
        goalService.delete(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
