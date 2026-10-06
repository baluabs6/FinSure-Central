package com.finsure.claims;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/claims") @CrossOrigin
public class ClaimController {
  public record ClaimRequest(@NotBlank String policyNumber, @NotBlank String claimantName, @NotBlank String description) {}
  public record StatusUpdate(@NotBlank String status, String rejectionReason) {}
  private static final List<String> STAGES = List.of("SUBMITTED", "DOCS_VERIFIED", "UNDER_REVIEW", "APPROVED", "REJECTED", "PAID");

  private final ClaimRepository repo; private final KafkaTemplate<String, String> kafka;
  public ClaimController(ClaimRepository repo, KafkaTemplate<String, String> kafka){ this.repo = repo; this.kafka = kafka; }

  @PostMapping @ResponseStatus(HttpStatus.CREATED)
  public Claim submit(@Valid @RequestBody ClaimRequest r) {
    Claim c = new Claim(); c.setPolicyNumber(r.policyNumber()); c.setClaimantName(r.claimantName()); c.setDescription(r.description());
    c = repo.save(c); publish(c); return c;
  }
  @GetMapping public List<Claim> all(){ return repo.findAll(); }
  @GetMapping("/{id}") public Claim get(@PathVariable Long id){ return repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)); }
  @GetMapping("/stages") public List<String> stages(){ return STAGES; }

  @PutMapping("/{id}/status")
  public Claim update(@PathVariable Long id, @Valid @RequestBody StatusUpdate u) {
    if (!STAGES.contains(u.status())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown status");
    Claim c = get(id); c.setStatus(u.status()); c.setRejectionReason(u.rejectionReason());
    c = repo.save(c); publish(c); return c;
  }
  private void publish(Claim c){ kafka.send("claim-events", String.valueOf(c.getId()), c.getStatus() + "|" + c.getPolicyNumber()); }
}
