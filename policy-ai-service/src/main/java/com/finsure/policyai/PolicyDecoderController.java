package com.finsure.policyai;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/policies") @CrossOrigin
public class PolicyDecoderController {
  public record DecodeRequest(@NotBlank String policyText) {}
  public record DecodeResponse(String summary) {}
  private static final String SYSTEM = """
      You are an insurance policy decoder for retail customers in India. From the policy wording, list in plain language:
      1) exclusions, 2) sub-limits and room-rent caps, 3) waiting periods, 4) co-pay clauses, 5) claim-rejection risks.
      Quote clause numbers when present. If something is not in the text, say so; never invent terms.""";
  private final ChatClient chat;
  public PolicyDecoderController(ChatClient.Builder b){ this.chat = b.defaultSystem(SYSTEM).build(); }

  @PostMapping("/decode")
  public DecodeResponse decode(@Valid @RequestBody DecodeRequest r) {
    return new DecodeResponse(chat.prompt().user(r.policyText()).call().content());
  }
}
