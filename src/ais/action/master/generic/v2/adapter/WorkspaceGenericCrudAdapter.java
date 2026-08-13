package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudOperation;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;
import ais.action.master.generic.v2.GenericCrudScopeGuard;
import ais.action.master.rab.util.WorkspaceTreeModel;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.rab.Workspace;

/** Parity WorkspaceAction/WorkspaceRevisiAction untuk hierarchy dan lifecycle revisi anggaran. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class WorkspaceGenericCrudAdapter extends AbstractGenericCrudEntityAdapter<Workspace>
        implements GenericCrudScopeAdapter, GenericCrudCustomActionProvider {
    public Workspace createNew(GenericCrudRequestContext context) {
        Workspace value = new Workspace(); value.setParentId(Long.valueOf(0)); value.setRevisi(Integer.valueOf(1));
        return value;
    }
    public void validateCreate(Map values, GenericCrudRequestContext context, List errors) { required(values, errors); }
    public void validateUpdate(Workspace current, Map values, GenericCrudRequestContext context, List errors) { required(values, errors); }
    private void required(Map values, List errors) {
        required(values, errors, "nama", "Nama item anggaran");
        required(values, errors, "tahunWorkspace", "Tahun anggaran");
        required(values, errors, "revisi", "Nomor revisi");
        required(values, errors, "satuanKerja", "Satuan kerja");
    }
    private void required(Map values, List errors, String key, String label) {
        if (!values.containsKey(key) || values.get(key) == null || String.valueOf(values.get(key)).trim().length() == 0)
            errors.add(key + ":" + label + " wajib diisi");
    }
    public void beforeSave(Session session, Workspace target, GenericCrudRequestContext context) throws Exception {
        target.setNama(target.getNama() == null ? null : target.getNama().trim());
        target.setKode(target.getKode() == null ? "" : target.getKode().trim());
        if (target.getNama() == null || target.getNama().length() == 0 || target.getSatuanKerja() == null
                || target.getTahunWorkspace() == null || target.getRevisi() == null)
            throw new GenericCrudException(400, "WORKSPACE_REQUIRED", "Nama, tahun, revisi, dan satuan kerja wajib diisi.");
        if (target.getParentId() == null) target.setParentId(Long.valueOf(0));
        assertNoCycle(session, target);
        target.setHargaTotal(Double.valueOf(target.getVolume().doubleValue() * target.getHargaSatuan().doubleValue()));
    }
    private void assertNoCycle(Session session, Workspace target) throws GenericCrudException {
        Long parent = target.getParentId(); int depth = 0;
        while (parent != null && parent.longValue() != 0L) {
            if (target.getId() != null && target.getId().equals(parent))
                throw new GenericCrudException(409, "WORKSPACE_HIERARCHY_CYCLE", "Induk item tidak boleh menunjuk dirinya atau turunannya.");
            Workspace value = (Workspace) session.get(Workspace.class, parent);
            if (value == null) break; parent = value.getParentId();
            if (++depth > 1000) throw new GenericCrudException(409, "WORKSPACE_HIERARCHY_CYCLE", "Hierarchy anggaran terdeteksi bersiklus.");
        }
    }
    public boolean canDelete(Workspace target, GenericCrudRequestContext context, List reasons) {
        reasons.add("Gunakan aksi hapus revisi terkontrol; item anggaran dapat terikat transaksi dan relasi RAB."); return false;
    }
    public List getNaturalKeyProperties() {
        List result = new ArrayList(); result.add("tahunWorkspace"); result.add("revisi");
        result.add("satuanKerja"); result.add("kode"); result.add("nama"); return result;
    }
    public void applyReadScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void applyCountScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void validateObjectScope(GeneralValueObject object, GenericCrudRequestContext context) { }

    public List getActions(GenericCrudDefinition definition, GenericCrudRequestContext context) {
        List result = new ArrayList(); boolean enabled = context != null && context.isCanCreate() && context.isCanUpdate();
        result.add(action("copy_tree", "Salin Pohon Anggaran", enabled, true));
        result.add(action("next_revision", "Buat Revisi Berikutnya", enabled, false)); return result;
    }
    private Map action(String key, String label, boolean enabled, boolean parameters) {
        Map action = new LinkedHashMap(); action.put("actionKey", key); action.put("label", label);
        action.put("requiredPrivilege", GenericCrudOperation.CREATE); action.put("selectionMode", "SINGLE");
        action.put("enabled", Boolean.valueOf(enabled)); action.put("dangerous", Boolean.FALSE);
        action.put("confirmation", "Salin hierarchy anggaran beserta child ke tujuan yang dipilih?");
        if (parameters) {
            List values = new ArrayList(); values.add(parameter("targetYear", "Tahun tujuan"));
            values.add(parameter("targetRevision", "Revisi tujuan")); action.put("parameters", values);
            List names = new ArrayList(); names.add("targetYear"); names.add("targetRevision"); action.put("parameterNames", names);
        }
        return action;
    }
    private Map parameter(String name, String label) {
        Map value = new LinkedHashMap(); value.put("name", name); value.put("label", label);
        value.put("required", Boolean.TRUE); return value;
    }
    public GenericCrudResult execute(String actionKey, List selectedIds, Map parameters,
            GenericCrudRequestContext context) throws Exception {
        if (!"copy_tree".equals(actionKey) && !"next_revision".equals(actionKey))
            return GenericCrudResult.error("ACTION_NOT_ALLOWED", "Aksi tidak dikenal.");
        if (context == null || !context.isCanUpdate())
            throw new GenericCrudException(403, "UPDATE_REQUIRED", "Hak UPDATE diperlukan untuk menyalin anggaran.");
        Long id; try { id = Long.valueOf(String.valueOf(selectedIds.get(0))); }
        catch (Exception invalid) { throw new GenericCrudException(400, "WORKSPACE_ID_INVALID", "ID Workspace tidak valid."); }
        Session session = HibernateUtil.currentNativeSession(); Workspace source;
        try { source = (Workspace) session.get(Workspace.class, id);
            if (source == null) throw new GenericCrudException(404, "ROW_NOT_FOUND", "Item anggaran sumber tidak ditemukan.");
            new GenericCrudScopeGuard().validateObject(source, context);
            if (source.getParentId() != null && source.getParentId().longValue() > 0L)
                throw new GenericCrudException(409, "WORKSPACE_ROOT_REQUIRED", "Pilih root anggaran untuk menyalin seluruh hierarchy.");
        } finally { HibernateUtil.closeSessionQuietly(session); }
        int year = "copy_tree".equals(actionKey) ? integer(parameters, "targetYear") : source.getTahunWorkspace().intValue();
        int revision = "copy_tree".equals(actionKey) ? integer(parameters, "targetRevision") : nextRevision(source);
        ensureDestinationEmpty(source, year, revision);
        WorkspaceTreeModel.copy(source, source.getSatuanKerja(), source.getSumberDana(), Integer.valueOf(year), Integer.valueOf(revision));
        CommonPrivilages.saveActivity(getClass(), CommonPrivilages.CREATE, source,
                "Salin Workspace New UI ke tahun " + year + " revisi " + revision);
        Map data = new LinkedHashMap(); data.put("year", Integer.valueOf(year)); data.put("revision", Integer.valueOf(revision));
        return GenericCrudResult.ok("Hierarchy anggaran berhasil disalin ke tahun " + year + " revisi " + revision + ".", data);
    }
    private int nextRevision(Workspace source) {
        Session session = HibernateUtil.currentNativeSession();
        try { Number max = (Number) session.createCriteria(Workspace.class)
                .add(Restrictions.eq("tahunWorkspace", source.getTahunWorkspace()))
                .add(Restrictions.eq("satuanKerja", source.getSatuanKerja()))
                .add(source.getSumberDana() == null ? Restrictions.isNull("sumberDana") : Restrictions.eq("sumberDana", source.getSumberDana()))
                .setProjection(Projections.max("revisi")).uniqueResult();
            return max == null ? 1 : max.intValue() + 1;
        } finally { HibernateUtil.closeSessionQuietly(session); }
    }
    private void ensureDestinationEmpty(Workspace source, int year, int revision) throws GenericCrudException {
        Session session = HibernateUtil.currentNativeSession();
        try { Number count = (Number) session.createCriteria(Workspace.class)
                .add(Restrictions.eq("tahunWorkspace", Integer.valueOf(year)))
                .add(Restrictions.eq("revisi", Integer.valueOf(revision)))
                .add(Restrictions.eq("satuanKerja", source.getSatuanKerja()))
                .add(source.getSumberDana() == null ? Restrictions.isNull("sumberDana") : Restrictions.eq("sumberDana", source.getSumberDana()))
                .setProjection(Projections.rowCount()).uniqueResult();
            if (count != null && count.longValue() > 0L)
                throw new GenericCrudException(409, "WORKSPACE_DESTINATION_EXISTS", "Tujuan sudah mempunyai data; pilih revisi lain.");
        } finally { HibernateUtil.closeSessionQuietly(session); }
    }
    private int integer(Map values, String key) throws GenericCrudException {
        try { int value = Integer.parseInt(String.valueOf(values.get(key))); if (value < 1) throw new Exception(); return value; }
        catch (Exception invalid) { throw new GenericCrudException(400, "WORKSPACE_PARAMETER_INVALID", key + " harus berupa angka positif."); }
    }
}
