package ths.server.service;

import ths.server.dao.AuditDao;
import ths.server.dao.VitalDao;
import ths.server.model.VitalSign;

import java.io.BufferedReader;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.List;

public class VitalService {
    private final VitalDao dao = new VitalDao();
    private final AuditDao audit = new AuditDao();

    public long add(VitalSign v) throws Exception {
        if (v.measuredAt == null) v.measuredAt = LocalDateTime.now();
        long id = dao.insert(v);
        audit.log(v.patientId, "VITAL_ADD", "vitals", "{\"vitalId\":"+id+"}");
        return id;
    }

    public List<VitalSign> list(long patientId) throws Exception {
        return dao.listByPatient(patientId);
    }

    public int importCsv(long patientId, String csv) throws Exception {
        int imported = 0;
        try (BufferedReader br = new BufferedReader(new StringReader(csv))) {
            String header = br.readLine(); // measuredAt,pulse,respiration,systolic,diastolic,spo2,temperature
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                VitalSign v = new VitalSign();
                v.patientId = patientId;
                v.measuredAt = LocalDateTime.parse(p[0]);
                v.pulse = nzI(p,1);
                v.respiration = nzI(p,2);
                v.systolic = nzI(p,3);
                v.diastolic = nzI(p,4);
                v.spo2 = nzI(p,5);
                v.temperature = nzD(p,6);
                dao.insert(v); imported++;
            }
        }
        audit.log(patientId, "VITAL_IMPORT_CSV", "vitals", "{\"count\":"+imported+"}");
        return imported;
    }

    public String exportCsv(long patientId) throws Exception {
        var list = dao.listByPatient(patientId);
        StringBuilder sb = new StringBuilder("measuredAt,pulse,respiration,systolic,diastolic,spo2,temperature\n");
        for (var v : list) {
            sb.append(v.measuredAt).append(',')
              .append(nz(v.pulse)).append(',')
              .append(nz(v.respiration)).append(',')
              .append(nz(v.systolic)).append(',')
              .append(nz(v.diastolic)).append(',')
              .append(nz(v.spo2)).append(',')
              .append(nz(v.temperature)).append('\n');
        }
        return sb.toString();
    }

    private static Integer nzI(String[] p, int i){ return i<p.length && !p[i].isBlank()? Integer.parseInt(p[i]) : null; }
    private static Double  nzD(String[] p, int i){ return i<p.length && !p[i].isBlank()? Double.parseDouble(p[i]) : null; }
    private static String  nz(Object o){ return o==null? "" : o.toString(); }
}
