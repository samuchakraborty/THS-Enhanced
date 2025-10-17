package ths.server.service;

import ths.server.dao.AuditDao;
import ths.server.dao.BookingDao;
import ths.server.model.Booking;

import java.time.LocalDateTime;
import java.util.List;

public class BookingService {
    private final BookingDao dao = new BookingDao();
    private final AuditDao audit = new AuditDao();

    public long create(long patientId, long specialistId, LocalDateTime startAt, String notes) throws Exception {
        Booking b = new Booking();
        b.patientId = patientId;
        b.specialistId = specialistId;
        b.startAt = startAt;
        b.status = "REQUESTED";
        b.notes = notes;
        long id = dao.insert(b);
        audit.log(patientId, "BOOKING_CREATE", "booking", "{\"id\":"+id+"}");
        return id;
    }

    public List<Booking> listMine(long patientId) throws Exception {
        return dao.listByPatient(patientId);
    }
}
