package ths.server.model;

import java.time.LocalDateTime;

public class VitalSign {
    public long id;
    public long patientId;
    public LocalDateTime measuredAt;
    public Integer pulse, respiration, systolic, diastolic, spo2;
    public Double temperature;
}
