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
import ais.action.master.generic.v2.GenericCrudOperation;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Mahasiswa;
import ais.database.model.sekolah.Siswa;

/** Scope bersama laporan request payment gateway sebagaimana Action ZKoss lama. */
@SuppressWarnings("rawtypes")
public abstract class AbstractPaymentRequestReadOnlyAdapter<T extends GeneralValueObject>
        extends AbstractGenericCrudEntityAdapter<T>
        implements GenericCrudScopeAdapter, GenericCrudCustomActionProvider {
    protected abstract Mahasiswa owner(T target);
    protected abstract Class requestType();
    protected abstract Class detailType();
    protected abstract String detailRequestProperty();
    protected Siswa schoolOwner(T target) { return null; }

    public T createNew(GenericCrudRequestContext context) { return null; }
    public boolean canDelete(T target, GenericCrudRequestContext context, List reasons) {
        reasons.add("Request payment gateway adalah log transaksi read-only."); return false;
    }
    public List getNaturalKeyProperties() { return new ArrayList(); }

    public void applyReadScope(Criteria criteria, GenericCrudRequestContext context) {
        Mahasiswa mahasiswa = currentStudent(context);
        if (mahasiswa != null) criteria.add(Restrictions.eq("mahasiswa", mahasiswa));
        Siswa siswa = currentSchoolStudent(context);
        if (siswa != null) criteria.add(Restrictions.eq("siswa", siswa));
        if (context != null && context.getUser() != null && context.getUser().getOrangTua() != null) {
            List schoolIds = context.getUser().getOrangTua().ambilAnakSiswa();
            List collegeIds = context.getUser().getOrangTua().ambilAnakMahasiswa();
            if (schoolIds != null && !schoolIds.isEmpty()) criteria.add(Restrictions.in("siswa.id", schoolIds));
            if (collegeIds != null && !collegeIds.isEmpty()) criteria.add(Restrictions.in("mahasiswa.id", collegeIds));
        }
    }
    public void applyCountScope(Criteria criteria, GenericCrudRequestContext context) {
        applyReadScope(criteria, context);
    }
    public void validateObjectScope(GeneralValueObject object, GenericCrudRequestContext context)
            throws GenericCrudException {
        Mahasiswa mahasiswa = currentStudent(context);
        if (mahasiswa != null && !sameStudent(mahasiswa, owner((T) object)))
            throw new GenericCrudException(403, "PAYMENT_REQUEST_SCOPE_DENIED",
                    "Request payment gateway bukan milik mahasiswa aktif.");
        Siswa siswa = currentSchoolStudent(context);
        if (siswa != null && !sameSchoolStudent(siswa, schoolOwner((T) object)))
            throw new GenericCrudException(403, "PAYMENT_REQUEST_SCOPE_DENIED",
                    "Request payment gateway bukan milik siswa aktif.");
    }
    private Mahasiswa currentStudent(GenericCrudRequestContext context) {
        return context == null || context.getUser() == null ? null : context.getUser().getMahasiswa();
    }
    private Siswa currentSchoolStudent(GenericCrudRequestContext context) {
        return context == null || context.getUser() == null ? null : context.getUser().getSiswa();
    }
    private boolean sameStudent(Mahasiswa expected, Mahasiswa actual) {
        if (expected == actual) return true;
        if (expected == null || actual == null || expected.getId() == null || actual.getId() == null) return false;
        return expected.getId().equals(actual.getId());
    }
    private boolean sameSchoolStudent(Siswa expected, Siswa actual) {
        if (expected == actual) return true;
        if (expected == null || actual == null || expected.getId() == null || actual.getId() == null) return false;
        return expected.getId().equals(actual.getId());
    }

    public List getActions(GenericCrudDefinition definition, GenericCrudRequestContext context) {
        List result = new ArrayList(); Map action = new LinkedHashMap();
        action.put("actionKey", "view_details"); action.put("label", "Lihat Rincian");
        action.put("requiredPrivilege", GenericCrudOperation.READ); action.put("selectionMode", "SINGLE");
        action.put("enabled", Boolean.valueOf(context != null && context.isCanRead()));
        result.add(action); return result;
    }

    public GenericCrudResult execute(String actionKey, List selectedIds, Map parameters,
            GenericCrudRequestContext context) throws Exception {
        if (!"view_details".equals(actionKey))
            return GenericCrudResult.error("ACTION_NOT_ALLOWED", "Aksi tidak dikenal.");
        Long id;
        try { id = Long.valueOf(String.valueOf(selectedIds.get(0))); }
        catch (Exception invalid) { throw new GenericCrudException(400, "ROW_ID_INVALID", "ID request tidak valid."); }
        Session session = HibernateUtil.currentNativeSession();
        T request = (T) session.get(requestType(), id);
        if (request == null) throw new GenericCrudException(404, "ROW_NOT_FOUND", "Request tidak ditemukan.");
        validateObjectScope(request, context);
        List details = session.createCriteria(detailType())
                .add(Restrictions.eq(detailRequestProperty(), request))
                .add(Restrictions.isNull("idCicilan")).list();
        StringBuilder message = new StringBuilder("Rincian nominal"); double total = 0d;
        List safeDetails = new ArrayList();
        for (int i = 0; i < details.size(); i++) {
            Object detail = details.get(i);
            Object label = invoke(detail, "getKeterangan"); Object amount = invoke(detail, "getNilai");
            if (amount instanceof Number) total += ((Number) amount).doubleValue();
            Map safe = new LinkedHashMap(); safe.put("keterangan", label); safe.put("nilai", amount);
            safeDetails.add(safe);
            message.append(i == 0 ? ": " : "; ").append(label == null ? "-" : label)
                    .append(" = ").append(amount == null ? "0" : amount);
        }
        if (details.isEmpty()) message.append(": tidak ada detail aktif.");
        else message.append(". Total = ").append(total);
        Map data = new LinkedHashMap(); data.put("count", Integer.valueOf(details.size()));
        data.put("total", Double.valueOf(total)); data.put("details", safeDetails);
        return GenericCrudResult.ok(message.toString(), data);
    }
    private Object invoke(Object target, String method) {
        try { return target.getClass().getMethod(method, new Class[0]).invoke(target, new Object[0]); }
        catch (Exception ignored) { return null; }
    }
}
