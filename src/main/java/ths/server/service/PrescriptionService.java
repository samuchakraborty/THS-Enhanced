package ths.server.service;

import ths.server.dao.*;
import ths.server.model.Prescription;

import java.util.List;

public class PrescriptionService {
    private final PrescriptionDao rx = new PrescriptionDao();
    private final RefillDao refills = new RefillDao();
    private final AuditDao audit = new AuditDao();

    public long create(long specialistId, long patientId, String drug, String dosage, String instructions, int total) throws Exception {
        Prescription p = new Prescription();
        p.specialistId = specialistId;
        p.patientId = patientId;
        p.drugName = drug;
        p.dosage = dosage;
        p.instructions = instructions;
        p.refillsTotal = total;
        p.refillsUsed = 0;
        p.status = "ACTIVE";
        long id = rx.insert(p);
        audit.log(specialistId, "RX_CREATE", "prescription", "{\"id\":"+id+"}");
        return id;
    }

    public List<Prescription> listMine(long patientId) throws Exception {
        return rx.listByPatient(patientId);
    }

    public long requestRefill(long patientId, long rxId) throws Exception {
        long id = refills.insert(rxId, patientId);
        audit.log(patientId, "REFILL_REQUEST", "prescription", "{\"rxId\":"+rxId+"}");
        return id;
    }

    public void decideRefill(long specialistId, long refillId, String decision, long rxId, String notes) throws Exception {
        refills.decide(refillId, decision, specialistId, notes);
//        if ("APPROVED".equals(decision)) rx.incrementRefillsUsed(rxId);
        audit.log(specialistId, "REFILL_DECISION", "prescription", "{\"refillId\":"+refillId+",\"decision\":\""+decision+"\"}");
    }
  
}

