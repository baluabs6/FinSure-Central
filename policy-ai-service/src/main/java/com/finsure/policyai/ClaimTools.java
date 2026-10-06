package com.finsure.policyai;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.web.client.RestClient;

public class ClaimTools {
  private final RestClient http;
  public ClaimTools(String baseUrl) { this.http = RestClient.create(baseUrl); }

  @Tool(description = "Get the details and status of one claim by its numeric id")
  public String claimStatus(@ToolParam(description = "claim id") long id) {
    return http.get().uri("/api/claims/{id}", id).retrieve().body(String.class);
  }
  @Tool(description = "List all claims with their statuses")
  public String listClaims() { return http.get().uri("/api/claims").retrieve().body(String.class); }
}
