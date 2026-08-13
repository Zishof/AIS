package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudFieldDefinition;
import ais.action.master.generic.v2.GenericCrudOperation;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;
import ais.action.master.generic.v2.GenericCrudScopeGuard;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;
import ais.database.model.StatusPegawai;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Guru;

/** Daftar pegawai berstatus pensiun; parity untuk PensiunAction/PegawaiPensiunAction. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class PegawaiPensiunGenericCrudAdapter extends GenericCrudAutoEntityAdapter
        implements GenericCrudCustomActionProvider {
    private static final String REACTIVATE = "reactivate_employee";

    public PegawaiPensiunGenericCrudAdapter() {
        super(Pegawai.class, false, null, true);
    }

    public void configure(GenericCrudDefinition definition) {
        definition.setDisplayName("Pegawai Pensiun");
        definition.setLifecycleStatus(GenericCrudDefinition.READ_ONLY);
        definition.setCreateEnabled(false);
        definition.setUpdateEnabled(false);
        definition.setDeleteEnabled(false);
        definition.setImportEnabled(false);
        definition.setDefaultSortProperty("tanggallahir");
        definition.setDefaultSortAscending(true);
        definition.setDefaultPageSize(25);
        String[] columns = { "id", "mycode", "code", "nama", "usiaPensiun", "tanggallahir",
                "tanggalmasuk", "satuanKerja", "statusPegawai", "aktif" };
        List fields = definition.getFields();
        for (int i = 0; i < fields.size(); i++) {
            GenericCrudFieldDefinition field = (GenericCrudFieldDefinition) fields.get(i);
            field.setCreateable(false);
            field.setUpdateable(false);
            field.setTableVisible(contains(columns, field.getProperty()));
            field.setSearchable("mycode".equals(field.getProperty()) || "code".equals(field.getProperty())
                    || "nama".equals(field.getProperty()));
        }
    }

    public void applyDefaultFilters(Criteria criteria, GenericCrudRequestContext context) {
        criteria.add(Restrictions.sqlRestriction("(aktif = true or aktif is null)"));
        criteria.add(Restrictions.sqlRestriction(
                "status_pegawai = (select id from public.status_pegawai where lower(nama) = 'pensiun' order by id limit 1)"));
    }

    public void applyReadScope(Criteria criteria, GenericCrudRequestContext context) {
        applyUnitScope(criteria, context == null ? null : context.getUser());
    }

    public void applyCountScope(Criteria criteria, GenericCrudRequestContext context) {
        applyUnitScope(criteria, context == null ? null : context.getUser());
    }

    public void validateObjectScope(GeneralValueObject object, GenericCrudRequestContext context) throws Exception {
        authorize(context);
        if (!(object instanceof Pegawai))
            throw new GenericCrudException(403, "OBJECT_OUTSIDE_SCOPE", "Pegawai tidak valid.");
        Pegawai employee = (Pegawai) object;
        if (!isRetired(employee))
            throw new GenericCrudException(403, "OBJECT_OUTSIDE_SCOPE", "Pegawai tidak berstatus pensiun.");
        Tbmuser user = context.getUser();
        if (ais.common.Common.getApakahAdmin() || user == null || user.hakAkses() != null
                && Boolean.TRUE.equals(user.hakAkses().getMelihatDataSatkerLain())) return;
        if (user.getSatuanKerja() == null || !user.getSatuanKerja().equals(employee.getSatuanKerja()))
            throw new GenericCrudException(403, "OBJECT_OUTSIDE_SCOPE", "Pegawai berada di luar satuan kerja role aktif.");
    }

    public List getActions(GenericCrudDefinition definition, GenericCrudRequestContext context) {
        List result = new ArrayList();
        Map action = new LinkedHashMap();
        action.put("actionKey", REACTIVATE);
        action.put("label", "Ubah ke Aktif");
        action.put("requiredPrivilege", GenericCrudOperation.UPDATE);
        action.put("selectionMode", "SINGLE");
        action.put("enabled", Boolean.valueOf(context != null && context.isCanUpdate()));
        action.put("dangerous", Boolean.TRUE);
        action.put("parameterNames", new ArrayList());
        result.add(action);
        return result;
    }

    public GenericCrudResult execute(String actionKey, List selectedIds, Map parameters,
            GenericCrudRequestContext context) throws Exception {
        if (!REACTIVATE.equals(actionKey))
            return GenericCrudResult.error("ACTION_NOT_ALLOWED", "Aksi tidak dikenal.");
        Long id;
        try { id = Long.valueOf(String.valueOf(selectedIds.get(0))); }
        catch (Exception invalid) {
            throw new GenericCrudException(400, "EMPLOYEE_ID_INVALID", "ID pegawai tidak valid.");
        }
        Session session = HibernateUtil.currentNativeSession();
        try {
            session.beginTransaction();
            Pegawai employee = (Pegawai) session.get(Pegawai.class, id);
            if (employee == null)
                throw new GenericCrudException(404, "ROW_NOT_FOUND", "Pegawai tidak ditemukan.");
            new GenericCrudScopeGuard().validateObject(employee, context);
            StatusPegawai active = (StatusPegawai) session.createCriteria(StatusPegawai.class)
                    .add(Restrictions.ilike("nama", "Aktif"))
                    .setMaxResults(1).uniqueResult();
            if (active == null)
                throw new GenericCrudException(409, "ACTIVE_STATUS_NOT_FOUND", "Master status pegawai Aktif tidak ditemukan.");
            employee.setStatusPegawai(active);
            Dosen lecturer = employee.getDosen();
            Guru teacher = employee.getGuru();
            if (lecturer != null) { lecturer.setStatusPegawai(active); session.update(lecturer); }
            if (teacher != null) { teacher.setStatusPegawai(active); session.update(teacher); }
            Tbmuser user = context.getUser();
            if (user != null) { employee.setOleh(user.getUserNama()); employee.setOlehId(user.getUserId()); }
            session.update(employee);
            session.getTransaction().commit();
            Map data = new LinkedHashMap(); data.put("id", id);
            return GenericCrudResult.ok("Status pegawai berhasil diubah menjadi Aktif.", data);
        } catch (Exception failure) {
            if (session.getTransaction() != null && session.getTransaction().isActive())
                session.getTransaction().rollback();
            throw failure;
        } finally { HibernateUtil.closeSession(); }
    }

    public List getNaturalKeyProperties() { return new ArrayList(); }

    private void applyUnitScope(Criteria criteria, Tbmuser user) {
        if (criteria == null) return;
        if (user == null) { criteria.add(Restrictions.sqlRestriction("1=0")); return; }
        if (ais.common.Common.getApakahAdmin() || user.hakAkses() != null
                && Boolean.TRUE.equals(user.hakAkses().getMelihatDataSatkerLain())) return;
        if (user.getSatuanKerja() == null) criteria.add(Restrictions.sqlRestriction("1=0"));
        else criteria.add(Restrictions.eq("satuanKerja", user.getSatuanKerja()));
    }

    private boolean isRetired(Pegawai value) {
        return value != null && value.getStatusPegawai() != null
                && "Pensiun".equalsIgnoreCase(value.getStatusPegawai().getNama());
    }

    private boolean contains(String[] values, String value) {
        for (int i = 0; i < values.length; i++) if (values[i].equals(value)) return true;
        return false;
    }
}
