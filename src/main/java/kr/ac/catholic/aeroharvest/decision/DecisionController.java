package kr.ac.catholic.aeroharvest.decision;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/v1")
public class DecisionController {
    private final DecisionService service;

    public DecisionController(DecisionService service) { this.service = service; }

    @GetMapping("/candidates")
    public List<Map<String, Object>> candidates(@RequestParam(required = false) String buildingId) {
        return service.candidates(buildingId);
    }

    @GetMapping("/candidates/{candidateId}")
    public Map<String, Object> candidate(@PathVariable String candidateId) {
        return service.candidate(candidateId);
    }

    @PostMapping("/decisions")
    public Map<String, Object> create(@Valid @RequestBody DecisionCreateRequest request) {
        return service.create(request);
    }

    @GetMapping("/decisions")
    public List<Map<String, Object>> decisions(@RequestParam(required = false) String candidateId,
                                               @RequestParam(required = false) DecisionStatus status) {
        return service.decisions(candidateId, status);
    }

    @GetMapping("/decisions/{decisionId}")
    public Map<String, Object> decision(@PathVariable String decisionId) {
        return service.decision(decisionId);
    }

    @PatchMapping("/decisions/{decisionId}")
    public Map<String, Object> update(@PathVariable String decisionId,
                                      @Valid @RequestBody DecisionUpdateRequest request) {
        return service.update(decisionId, request);
    }

    public enum DecisionStatus { MEASURE, HOLD, REVIEW }

    public record DecisionCreateRequest(
            @NotBlank String candidateId,
            @NotNull DecisionStatus status,
            @NotBlank String reason,
            String assignee,
            OffsetDateTime dueAt
    ) {}

    public record DecisionUpdateRequest(
            DecisionStatus status,
            String reason,
            String assignee,
            OffsetDateTime dueAt,
            @NotBlank String actor
    ) {}
}
