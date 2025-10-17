package ths.server.model;

import java.time.LocalDateTime;

public class Booking {
    public long id;
    public long patientId;
    public long specialistId;
    public LocalDateTime startAt;
    public String status;
    public String notes;
}
