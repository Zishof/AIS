package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudFieldDefinition;
import ais.action.master.generic.v2.GenericCrudOperation;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.PenugasanDosenMengajar;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;

/** Parity headless PenugasanDosenMengajarAction: edit-inline dan generate dari jadwal. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class PenugasanDosenMengajarGenericCrudAdapter extends GenericCrudAutoEntityAdapter
        implements GenericCrudCustomActionProvider {
    private static final String GENERATE = "generate_from_schedule";
    private static final String[] INLINE_FIELDS = new String[] {
        "kode", "tanggalSuratTugas", "tmtSuratTugas", "keterangan"
    };

    public PenugasanDosenMengajarGenericCrudAdapter() {
        super(PenugasanDosenMengajar.class, false, null, true);
    }

    public void configure(GenericCrudDefinition definition) {
        definition.setDisplayName("Penugasan Dosen Mengajar");
        definition.setCreateEnabled(false);
        definition.setUpdateEnabled(true);
        definition.setDeleteEnabled(false);
        definition.setImportEnabled(false);
        definition.setDefaultSortProperty("tahunAkademik");
        definition.setDefaultSortAscending(false);
        definition.setDefaultPageSize(25);
        for (int i = 0; i < definition.getFields().size(); i++) {
            GenericCrudFieldDefinition field = (GenericCrudFieldDefinition) definition.getFields().get(i);
            field.setCreateable(false);
            field.setUpdateable(contains(INLINE_FIELDS, field.getProperty()));
        }
    }

    public void beforeSave(Session session, GeneralValueObject target,
            GenericCrudRequestContext context) throws Exception {
        PenugasanDosenMengajar value = (PenugasanDosenMengajar) target;
        Tbmuser user = context == null ? null : context.getUser();
        if (user != null) {
            value.setOleh(user.getUserNama());
            value.setOlehId(user.getUserId());
        }
        super.beforeSave(session, target, context);
    }

    public List getNaturalKeyProperties() {
        List values = new ArrayList();
        values.add("jurusan"); values.add("program"); values.add("tahunAkademik");
        values.add("semester"); values.add("dosen"); return values;
    }

    public void applyReadScope(Criteria criteria, GenericCrudRequestContext context) {
        applyAssignmentScope(criteria, context == null ? null : context.getUser());
    }

    public void applyCountScope(Criteria criteria, GenericCrudRequestContext context) {
        applyAssignmentScope(criteria, context == null ? null : context.getUser());
    }

    public void validateObjectScope(GeneralValueObject object, GenericCrudRequestContext context)
            throws Exception {
        authorize(context);
        if (!(object instanceof PenugasanDosenMengajar)
                || !withinInstitution(((PenugasanDosenMengajar) object).getJurusan(), context.getUser())) {
            throw new GenericCrudException(403, "OBJECT_OUTSIDE_SCOPE",
                    "Penugasan dosen berada di luar jurusan/fakultas/perguruan tinggi role aktif.");
        }
    }

    public List getActions(GenericCrudDefinition definition, GenericCrudRequestContext context) {
        List result = new ArrayList(); Map action = new LinkedHashMap();
        action.put("actionKey", GENERATE);
        action.put("label", "Generate No. SK Berdasarkan Jadwal");
        action.put("requiredPrivilege", GenericCrudOperation.UPDATE);
        action.put("selectionMode", "NONE");
        action.put("enabled", Boolean.valueOf(context != null && context.isCanUpdate()));
        action.put("dangerous", Boolean.FALSE);
        List names = new ArrayList(); names.add("tahunAkademik"); names.add("semester");
        action.put("parameterNames", names);
        List parameters = new ArrayList();
        parameters.add(parameter("tahunAkademik", "Tahun akademik (contoh 2026/2027)"));
        parameters.add(parameter("semester", "Semester (Ganjil/Genap)"));
        action.put("parameters", parameters); result.add(action); return result;
    }

    public GenericCrudResult execute(String actionKey, List selectedIds, Map parameters,
            GenericCrudRequestContext context) throws Exception {
        if (!GENERATE.equals(actionKey)) return GenericCrudResult.error("ACTION_NOT_ALLOWED", "Aksi tidak dikenal.");
        if (context == null || !context.isCanUpdate())
            throw new GenericCrudException(403, "UPDATE_FORBIDDEN", "Hak UPDATE diperlukan untuk generate penugasan.");
        String year = required(parameters, "tahunAkademik", "TAHUN_AKADEMIK_REQUIRED", "Tahun akademik wajib diisi.");
        String semester = required(parameters, "semester", "SEMESTER_REQUIRED", "Semester wajib diisi.");
        if (!Perkuliahan.GANJIL.equalsIgnoreCase(semester) && !Perkuliahan.GENAP.equalsIgnoreCase(semester))
            throw new GenericCrudException(400, "SEMESTER_INVALID", "Semester harus Ganjil atau Genap.");
        Session session = HibernateUtil.currentNativeSession(); Transaction tx = null;
        int created = 0, updated = 0, schedules = 0;
        try {
            tx = session.beginTransaction();
            Criteria query = session.createCriteria(Perkuliahan.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
                    .add(Restrictions.eq("tahunAjaran", year))
                    .add(Restrictions.eq("ganjilGenap", canonicalSemester(semester)));
            List rows = query.list(); schedules = rows.size();
            for (int i = 0; i < rows.size(); i++) {
                Perkuliahan course = (Perkuliahan) rows.get(i);
                if (!withinInstitution(course.getJurusan(), context.getUser())) continue;
                List lecturers = course.populateDosenBuNama();
                for (int d = 0; lecturers != null && d < lecturers.size(); d++) {
                    Dosen lecturer = (Dosen) lecturers.get(d);
                    PenugasanDosenMengajar assignment = find(session, course, lecturer, year,
                            canonicalSemester(semester));
                    Integer sks = course.getMatakuliah() == null ? null : course.getMatakuliah().getSks();
                    if (assignment == null) {
                        assignment = new PenugasanDosenMengajar();
                        assignment.setJurusan(course.getJurusan()); assignment.setProgram(course.getProgram());
                        assignment.setTahunAkademik(year); assignment.setSemester(canonicalSemester(semester));
                        assignment.setNama(year + "-" + canonicalSemester(semester));
                        assignment.setDosen(lecturer); assignment.setSks(sks); stamp(assignment, context.getUser());
                        session.save(assignment); created++;
                    } else if (!same(assignment.getSks(), sks)) {
                        assignment.setSks(sks); stamp(assignment, context.getUser()); session.update(assignment); updated++;
                    }
                }
            }
            session.flush(); tx.commit();
        } catch (Exception failure) {
            try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignored) { }
            throw failure;
        } finally { HibernateUtil.closeSession(); }
        Map data = new LinkedHashMap(); data.put("jadwal", Integer.valueOf(schedules));
        data.put("dibuat", Integer.valueOf(created)); data.put("diperbarui", Integer.valueOf(updated));
        return GenericCrudResult.ok("Generate penugasan dosen selesai.", data);
    }

    private PenugasanDosenMengajar find(Session session, Perkuliahan course, Dosen lecturer,
            String year, String semester) {
        Criteria criteria = session.createCriteria(PenugasanDosenMengajar.class)
                .add(Restrictions.eq("jurusan", course.getJurusan()))
                .add(course.getProgram() == null ? Restrictions.isNull("program") : Restrictions.eq("program", course.getProgram()))
                .add(Restrictions.eq("tahunAkademik", year)).add(Restrictions.eq("semester", semester))
                .add(Restrictions.eq("dosen", lecturer)).setMaxResults(1);
        return (PenugasanDosenMengajar) criteria.uniqueResult();
    }

    private void applyAssignmentScope(Criteria criteria, Tbmuser user) {
        if (criteria == null) return;
        if (user == null) { criteria.add(Restrictions.sqlRestriction("1=0")); return; }
        if (ais.common.Common.getApakahAdmin()) return;
        Jurusan department = user.getJurusan();
        if (department != null) { criteria.add(Restrictions.eq("jurusan", department)); return; }
        Fakultas faculty = user.getFakultas();
        if (faculty != null) {
            criteria.createAlias("jurusan", "scopeJurusan").add(Restrictions.eq("scopeJurusan.fakultas", faculty));
            return;
        }
        if (user.getPerguruanTinggi() != null) {
            criteria.createAlias("jurusan", "scopeJurusan")
                    .createAlias("scopeJurusan.fakultas", "scopeFakultas")
                    .add(Restrictions.eq("scopeFakultas.perguruanTinggi", user.getPerguruanTinggi()));
            return;
        }
        criteria.add(Restrictions.sqlRestriction("1=0"));
    }

    private boolean withinInstitution(Jurusan department, Tbmuser user) {
        if (user == null) return false;
        if (ais.common.Common.getApakahAdmin()) return true;
        try {
            if (department == null) return false;
            if (user.getJurusan() != null) return user.getJurusan().equals(department);
            if (department.getFakultas() == null) return false;
            if (user.getFakultas() != null) return user.getFakultas().equals(department.getFakultas());
            return user.getPerguruanTinggi() != null
                    && user.getPerguruanTinggi().equals(department.getFakultas().getPerguruanTinggi());
        } catch (Exception denied) { return false; }
    }

    private void stamp(PenugasanDosenMengajar value, Tbmuser user) {
        if (user == null) return; value.setOleh(user.getUserNama()); value.setOlehId(user.getUserId());
    }
    private String canonicalSemester(String value) {
        return Perkuliahan.GANJIL.equalsIgnoreCase(value) ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
    }
    private boolean same(Object one, Object two) { return one == null ? two == null : one.equals(two); }
    private boolean contains(String[] values, String key) {
        for (int i = 0; i < values.length; i++) if (values[i].equals(key)) return true; return false;
    }
    private Map parameter(String name, String label) {
        Map value = new LinkedHashMap(); value.put("name", name); value.put("label", label);
        value.put("required", Boolean.TRUE); return value;
    }
    private String required(Map values, String key, String code, String message) throws GenericCrudException {
        String value = values == null || values.get(key) == null ? "" : String.valueOf(values.get(key)).trim();
        if (value.length() == 0) throw new GenericCrudException(400, code, message); return value;
    }
}
