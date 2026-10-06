package com.finsure.claims;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
public class Claim {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  private String policyNumber, claimantName, description, status = "SUBMITTED", rejectionReason;
  private Instant createdAt = Instant.now();
  public Long getId(){return id;} public String getPolicyNumber(){return policyNumber;} public void setPolicyNumber(String v){policyNumber=v;}
  public String getClaimantName(){return claimantName;} public void setClaimantName(String v){claimantName=v;}
  public String getDescription(){return description;} public void setDescription(String v){description=v;}
  public String getStatus(){return status;} public void setStatus(String v){status=v;}
  public String getRejectionReason(){return rejectionReason;} public void setRejectionReason(String v){rejectionReason=v;}
  public Instant getCreatedAt(){return createdAt;}
}
