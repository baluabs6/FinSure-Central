package com.finsure.policyai;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
@Component
public class ClaimEventListener {
  private static final Logger log = LoggerFactory.getLogger(ClaimEventListener.class);
  @KafkaListener(topics = "claim-events", groupId = "policy-ai")
  public void onClaimEvent(String event){ log.info("Claim event received: {}", event); /* hook: trigger rejection-reason explainer */ }
}
