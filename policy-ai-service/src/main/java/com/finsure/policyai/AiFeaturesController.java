package com.finsure.policyai;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/ai") @CrossOrigin
public class AiFeaturesController {
  public record Text(String text) {}
  public record Ask(String question, String language) {}
  public record Extract(String documentType, String documentText) {}
  public record DocCheck(String claimType, List<String> documents, String language) {}
  public record Compare(String policyA, String policyB, String language) {}
  public record Risk(String claimType, double claimAmount, double sumInsured, int policyAgeMonths, int documentsMissing, int priorClaimsLast12Months) {}
  public record Coverage(double annualIncome, int dependents, double loans, String city, double existingCover, String language) {}
  public record Chat(String message) {}

  private static final String QA = "You answer questions about an insurance policy using ONLY the numbered excerpts provided. Cite excerpt numbers like [1]. If the excerpts do not answer the question, say it is not found in the policy. Never invent terms.";
  private static final String EXTRACT = "Extract fields from the insurance or medical document. Return only a JSON object with keys such as patientName, dates, diagnosis, amounts, hospital, rejectionReason. Use null for anything not present. Never guess.";
  private static final String DOCS = "You check an Indian insurance claim document list. Given the claim type and the documents the customer has, list typical required documents, which are missing, and which look inconsistent. State that the insurer's own list is final.";
  private static final String COMPARE = "Compare two insurance policy wordings. Give a side-by-side summary of exclusions, sub-limits, room-rent caps, waiting periods and co-pay, then the trade-offs. Use only the text given.";
  private static final String RISK = "Explain in plain language, for a customer, why these rule-based indicators raise claim rejection or fraud-review risk and how to reduce it. Do not accuse anyone of fraud.";
  private static final String COVER = "You are a cautious insurance needs advisor for India. Using the figures given and the baseline estimate, recommend a sum insured range, flag coverage gaps, and state the assumptions. This is guidance, not financial advice.";
  private static final String SCAM = "Assess whether this advisor, app, loan offer or message shows common finance scam red flags (guaranteed returns, urgency, OTP or advance-fee requests, unregistered lenders, harassment). You cannot verify SEBI, RBI or IRDAI registration; tell the user to check the official registers. Give a risk level and reasons.";
  private static final String COPILOT = "You are the FinSure claims copilot. Use the tools to look up real claim data before answering. If a claim is not found, say so. You cannot change claims.";

  private final Map<String, List<String>> policies = new HashMap<>();
  private final AiGateway gw; private final ClaimEventListener events; private final String claimsUrl;
  public AiFeaturesController(AiGateway gw, ClaimEventListener events, @Value("${claims.url:http://localhost:8081}") String claimsUrl) {
    this.gw = gw; this.events = events; this.claimsUrl = claimsUrl;
  }

  @PostMapping("/policy/{policyNumber}/ingest")
  public Map<String, Object> ingest(@PathVariable String policyNumber, @RequestBody Text t) {
    List<String> chunks = new ArrayList<>();
    for (String para : t.text().split("\\n\\s*\\n"))
      for (int i = 0; i < para.length(); i += 800) { String c = para.substring(i, Math.min(para.length(), i + 800)).trim(); if (!c.isEmpty()) chunks.add(c); }
    policies.put(policyNumber, chunks);
    return Map.of("policyNumber", policyNumber, "chunks", chunks.size());
  }

  @PostMapping("/policy/{policyNumber}/ask")
  public Map<String, Object> ask(@PathVariable String policyNumber, @RequestBody Ask a) {
    List<String> chunks = policies.getOrDefault(policyNumber, List.of());
    Set<String> words = Arrays.stream(a.question().toLowerCase().split("\\W+")).filter(w -> w.length() > 3).collect(Collectors.toSet());
    List<String> top = chunks.stream().filter(c -> score(c, words) > 0)
        .sorted(Comparator.comparingInt((String c) -> score(c, words)).reversed()).limit(4).toList();
    if (top.isEmpty()) return Map.of("answer", "Not found in the policy. Ingest the policy text first or rephrase the question.", "grounded", false, "sources", List.of());
    StringBuilder ctx = new StringBuilder();
    for (int i = 0; i < top.size(); i++) ctx.append("[").append(i + 1).append("] ").append(top.get(i)).append("\n\n");
    String answer = gw.ask("policy-qa", QA, "Excerpts:\n" + ctx + "Question: " + a.question(), a.language());
    return Map.of("answer", answer, "grounded", true, "sources", top);
  }
  private int score(String chunk, Set<String> words) { String l = chunk.toLowerCase(); return (int) words.stream().filter(l::contains).count(); }

  @PostMapping("/extract")
  public Map<String, String> extract(@RequestBody Extract e) {
    return Map.of("fields", gw.ask("extract", EXTRACT, "Document type: " + e.documentType() + "\n" + e.documentText(), null));
  }

  @PostMapping("/check-documents")
  public Map<String, String> checkDocuments(@RequestBody DocCheck d) {
    return Map.of("result", gw.ask("doc-check", DOCS, "Claim type: " + d.claimType() + "\nDocuments held: " + String.join("; ", d.documents()), d.language()));
  }

  @PostMapping("/compare")
  public Map<String, String> compare(@RequestBody Compare c) {
    return Map.of("result", gw.ask("compare", COMPARE, "Policy A:\n" + c.policyA() + "\n\nPolicy B:\n" + c.policyB(), c.language()));
  }

  @PostMapping("/risk")
  public Map<String, Object> risk(@RequestBody Risk r) {
    int score = 0; List<String> flags = new ArrayList<>();
    if (r.sumInsured() > 0 && r.claimAmount() > 0.8 * r.sumInsured()) { score += 30; flags.add("Claim amount is above 80% of the sum insured"); }
    if (r.policyAgeMonths() < 12) { score += 25; flags.add("Policy is under 12 months old, so waiting periods may apply"); }
    if (r.documentsMissing() > 0) { score += Math.min(30, 10 * r.documentsMissing()); flags.add(r.documentsMissing() + " required document(s) missing"); }
    if (r.priorClaimsLast12Months() >= 3) { score += 25; flags.add("3 or more claims in the last 12 months"); }
    score = Math.min(100, score);
    String level = score >= 60 ? "HIGH" : score >= 30 ? "MEDIUM" : "LOW";
    String explanation = flags.isEmpty() ? "No risk indicators found." : gw.ask("risk", RISK, "Claim type: " + r.claimType() + "\nIndicators: " + String.join("; ", flags), null);
    return Map.of("score", score, "level", level, "flags", flags, "explanation", explanation);
  }

  @PostMapping("/coverage")
  public Map<String, Object> coverage(@RequestBody Coverage c) {
    double baseline = Math.max(0, 10 * c.annualIncome() + c.loans() - c.existingCover());
    String advice = gw.ask("coverage", COVER, "Annual income: " + c.annualIncome() + "\nDependents: " + c.dependents() + "\nLoans: " + c.loans()
        + "\nCity: " + c.city() + "\nExisting cover: " + c.existingCover() + "\nBaseline additional life cover (10x income + loans - existing): " + baseline, c.language());
    return Map.of("baselineAdditionalCover", baseline, "advice", advice);
  }

  @PostMapping("/scam-check")
  public Map<String, String> scam(@RequestBody Text t) { return Map.of("assessment", gw.ask("scam-check", SCAM, t.text(), null)); }

  @PostMapping("/copilot")
  public Map<String, String> copilot(@RequestBody Chat c) {
    String reply = gw.builder().defaultSystem(COPILOT).defaultTools(new ClaimTools(claimsUrl)).build().prompt().user(gw.mask(c.message())).call().content();
    gw.record("copilot", gw.mask(c.message()), reply);
    return Map.of("reply", reply);
  }

  @GetMapping("/notifications") public List<ClaimEventListener.Notification> notifications() { return events.notifications(); }
  @GetMapping("/audit") public List<AiGateway.Audit> audit() { return gw.audit(); }
}
