package ths.server.model;

public class Prescription {
    public long id;
    public long patientId;
    public long specialistId;
    public String drugName;
    public String dosage;
    public String instructions;
    public int refillsTotal;
    public int refillsUsed;
    public String status; // ACTIVE/EXPIRED/REVOKED
}
