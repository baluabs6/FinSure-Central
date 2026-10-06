package com.finsure.policyai;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/policies") @CrossOrigin
public class ClaimAssistController {
  public record ClaimContext(@NotBlank String policyNumber, @NotBlank String claimantName, @NotBlank String description,
                             @NotBlank String rejectionReason, String policyText) {}
  public record AssistResponse(String result) {}

  private static final String EXPLAIN = """
      You explain insurance claim rejections to retail customers in India in simple language.
      Given the claim details, the insurer's stated rejection reason and optionally the policy wording, explain:
      1) what the reason means, 2) which policy clause likely applies (only if the wording is provided; otherwise say it is unknown),
      3) what documents or steps could resolve it, 4) whether an appeal looks reasonable.
      Never invent clauses, amounts or legal conclusions.""";
  private static final String APPEAL = """
      You draft a formal appeal letter from an insurance customer in India to the insurer's grievance officer.
      Use only the facts provided. State the claim, the rejection reason, the customer's grounds for reconsideration and the requested action.
      Mention that the customer may escalate to the Insurance Ombudsman if unresolved within the regulatory period.
      Use placeholders such as [date] and [contact details] for anything not provided. Do not invent facts.""";

  private final ChatClient explainer, drafter;
  public ClaimAssistController(ChatClient.Builder b) {
    this.explainer = b.defaultSystem(EXPLAIN).build();
    this.drafter = b.defaultSystem(APPEAL).build();
  }

  @PostMapping("/explain-rejection")
  public AssistResponse explain(@Valid @RequestBody ClaimContext c) {
    return new AssistResponse(explainer.prompt().user(render(c)).call().content());
  }

  @PostMapping("/appeal-draft")
  public AssistResponse appeal(@Valid @RequestBody ClaimContext c) {
    return new AssistResponse(drafter.prompt().user(render(c)).call().content());
  }

  private String render(ClaimContext c) {
    return "Policy number: " + c.policyNumber() + "\nClaimant: " + c.claimantName() + "\nClaim description: " + c.description()
        + "\nRejection reason: " + c.rejectionReason()
        + (c.policyText() == null || c.policyText().isBlank() ? "" : "\nPolicy wording:\n" + c.policyText());
  }
}
