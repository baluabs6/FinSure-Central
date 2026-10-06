package com.finsure.policyai;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class AiGateway {
  public record Audit(Instant at, String feature, String prompt, String output) {}
  private final ChatClient.Builder builder;
  private final List<Audit> audit = new CopyOnWriteArrayList<>();
  public AiGateway(ChatClient.Builder builder) { this.builder = builder; }

  public String ask(String feature, String system, String user, String language) {
    String sys = language == null || language.isBlank() ? system : system + " Respond in " + language + ".";
    String masked = mask(user);
    String out = builder.clone().defaultSystem(sys).build().prompt().user(masked).call().content();
    record(feature, masked, out);
    return out;
  }
  public ChatClient.Builder builder() { return builder.clone(); }
  public String mask(String t) {
    return t.replaceAll("\\b\\d{12}\\b", "[AADHAAR]").replaceAll("\\b[A-Z]{5}\\d{4}[A-Z]\\b", "[PAN]")
        .replaceAll("\\b\\d{10}\\b", "[PHONE]").replaceAll("[\\w.+-]+@[\\w-]+\\.[\\w.]+", "[EMAIL]");
  }
  public void record(String feature, String prompt, String output) { audit.add(new Audit(Instant.now(), feature, prompt, output)); }
  public List<Audit> audit() { return audit; }
}
