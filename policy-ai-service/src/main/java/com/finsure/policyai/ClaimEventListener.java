package com.finsure.policyai;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ClaimEventListener {
  public record Notification(String claimId, String message, Instant at) {}
  private static final String SYSTEM = "Write a short, friendly, plain-language status update (max 3 sentences) for an insurance customer: what this claim stage means and what they should do next. Do not invent facts.";
  private final List<Notification> notifications = new CopyOnWriteArrayList<>();
  private final AiGateway gateway;
  public ClaimEventListener(AiGateway gateway) { this.gateway = gateway; }

  @KafkaListener(topics = "claim-events", groupId = "policy-ai")
  public void onClaimEvent(ConsumerRecord<String, String> rec) {
    String[] p = rec.value().split("\\|", 2);
    String message;
    try { message = gateway.ask("notification", SYSTEM, "Claim status: " + p[0] + (p.length > 1 ? ", policy " + p[1] : ""), null); }
    catch (Exception e) { message = "Your claim #" + rec.key() + " is now " + p[0] + "."; }
    notifications.add(new Notification(rec.key(), message, Instant.now()));
  }
  public List<Notification> notifications() { return notifications; }
}
