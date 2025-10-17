package ths.server.model;

import java.time.Instant;

public class RefillRequest {
    public long id;
    public long prescriptionId;
    public long patientId;
    public String status; // PENDING/APPROVED/REJECTED
    public Long decisionBy;
    public Instant decisionAt;
    public String notes;
}
