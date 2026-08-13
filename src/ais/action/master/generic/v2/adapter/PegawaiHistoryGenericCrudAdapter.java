package ais.action.master.generic.v2.adapter;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;
import ais.action.master.generic.v2.GenericCrudValueConverter;
import ais.common.CommonPrivilages;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;

/**
 * Port pola bersama helper riwayat pegawai. Hanya dipakai oleh class yang
 * didaftarkan eksplisit di registry; tidak melakukan scanning runtime.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class PegawaiHistoryGenericCrudAdapter extends AbstractGenericCrudEntityAdapter<GeneralValueObject>
        implements GenericCrudScopeAdapter, GenericCrudApprovalAdapter, GenericCrudAttachmentAdapter {
    private final Class ownerClass;

    public PegawaiHistoryGenericCrudAdapter(Class ownerClass) {
        if (ownerClass == null || !GeneralValueObject.class.isAssignableFrom(ownerClass)) {
            throw new IllegalArgumentException("Class riwayat pegawai tidak valid.");
        }
        this.ownerClass = ownerClass;
    }

    public Class getAttachmentOwnerClass() { return ownerClass; }

    public GeneralValueObject createNew(GenericCrudRequestContext context) throws Exception {
        GeneralValueObject value = (GeneralValueObject) ownerClass.newInstance();
        set(value, "setPegawai", Pegawai.class, owner(context));
        set(value, "setStatus", Boolean.class, Boolean.FALSE);
        return value;
    }

    public void validateCreate(Map values, GenericCrudRequestContext context, List errors) throws Exception {
        validate(null, values, context, errors);
    }

    public void validateUpdate(GeneralValueObject current, Map values,
            GenericCrudRequestContext context, List errors) throws Exception {
        validate(current, values, context, errors);
    }

    private void validate(GeneralValueObject current, Map values,
            GenericCrudRequestContext context, List errors) throws Exception {
        if (current != null && status(current) && !context.isCanApprove()) {
            errors.add("_global:Data yang sudah disetujui hanya dapat diubah oleh pemegang privilege APPROVE");
        }
        compareNumbers(values, "tahunMasuk", "tahunLulus", errors);
        compareNumbers(values, "tahunMulai", "tahunSelesai", errors);
        compareDates(values, "mulai", "selesai", errors);
        compareDates(values, "tanggalBerangkat", "tanggalPulang", errors);
    }

    public void applyCreateValues(GeneralValueObject target, Map values,
            GenericCrudRequestContext context) throws Exception {
        super.applyCreateValues(target, safeValues(values), context);
        Pegawai owner = owner(context);
        if (owner != null) set(target, "setPegawai", Pegawai.class, owner);
        set(target, "setStatus", Boolean.class, Boolean.FALSE);
    }

    public void applyUpdateValues(GeneralValueObject target, Map values,
            GenericCrudRequestContext context) throws Exception {
        super.applyUpdateValues(target, safeValues(values), context);
    }

    private Map safeValues(Map values) {
        Map safe = new LinkedHashMap(values);
        safe.remove("status");
        return safe;
    }

    public void beforeSave(Session session, GeneralValueObject target,
            GenericCrudRequestContext context) throws Exception {
        if (pegawai(target) == null) {
            throw new GenericCrudException(400, "PEGAWAI_REQUIRED", "Data Pegawai wajib dipilih.");
        }
    }

    public boolean canDelete(GeneralValueObject target, GenericCrudRequestContext context, List reasons) {
        if (status(target)) {
            reasons.add("Riwayat pegawai yang sudah disetujui tidak dapat dihapus.");
            return false;
        }
        return true;
    }

    public void delete(Session session, GeneralValueObject target,
            GenericCrudRequestContext context) throws Exception {
        session.delete(target);
    }

    public List getNaturalKeyProperties() {
        List result = new ArrayList(); result.add("pegawai"); result.add("id"); return result;
    }

    public GenericCrudResult approve(Serializable id, String reason,
            GenericCrudRequestContext context) throws Exception {
        return setApproval(id, true, context);
    }

    public GenericCrudResult reject(Serializable id, String reason,
            GenericCrudRequestContext context) throws Exception {
        return setApproval(id, false, context);
    }

    private GenericCrudResult setApproval(Serializable id, boolean approved,
            GenericCrudRequestContext context) throws Exception {
        Session session = HibernateUtil.currentNativeSession(); Transaction tx = null;
        try {
            tx = session.beginTransaction();
            GeneralValueObject target = (GeneralValueObject) session.get(ownerClass, id);
            if (target == null) throw new GenericCrudException(404, "ROW_NOT_FOUND", "Riwayat pegawai tidak ditemukan.");
            validateObjectScope(target, context);
            set(target, "setStatus", Boolean.class, Boolean.valueOf(approved));
            session.saveOrUpdate(target); session.flush(); tx.commit();
            CommonPrivilages.saveActivity(getClass(), CommonPrivilages.APPROVE, target,
                    approved ? "Persetujuan riwayat pegawai New UI" : "Pembatalan persetujuan riwayat pegawai New UI");
            return GenericCrudResult.ok(approved ? "Riwayat pegawai disetujui." : "Persetujuan riwayat pegawai dibatalkan.", null);
        } catch (Exception error) {
            try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignored) { }
            throw error;
        } finally { HibernateUtil.closeSession(); }
    }

    public void applyReadScope(Criteria criteria, GenericCrudRequestContext context) throws Exception { applyScope(criteria, context); }
    public void applyCountScope(Criteria criteria, GenericCrudRequestContext context) throws Exception { applyScope(criteria, context); }

    private void applyScope(Criteria criteria, GenericCrudRequestContext context) throws Exception {
        if (context.getUser() == null) { criteria.add(Restrictions.sqlRestriction("1=0")); return; }
        Pegawai owner = owner(context);
        if (owner != null) { criteria.add(Restrictions.eq("pegawai", owner)); return; }
        if (Common.getApakahAdmin()) return;
        Object unit = context.getUser().getSatuanKerja();
        if (unit != null) {
            criteria.createAlias("pegawai", "scopePegawai");
            criteria.add(Restrictions.eq("scopePegawai.satuanKerja", unit));
        } else criteria.add(Restrictions.sqlRestriction("1=0"));
    }

    public void validateObjectScope(GeneralValueObject object,
            GenericCrudRequestContext context) throws Exception {
        if (object == null || !ownerClass.isAssignableFrom(object.getClass()) || context.getUser() == null) deny();
        Pegawai actual = pegawai(object); Pegawai owner = owner(context);
        if (owner != null) { if (!same(owner, actual)) deny(); return; }
        if (Common.getApakahAdmin()) return;
        Object allowedUnit = context.getUser().getSatuanKerja();
        Object actualUnit = actual == null ? null : actual.getSatuanKerja();
        if (allowedUnit == null || !allowedUnit.equals(actualUnit)) deny();
    }

    private Pegawai owner(GenericCrudRequestContext context) {
        try { return context.getUser() == null ? null : context.getUser().ambilPegawai(); }
        catch (Exception unavailable) { return null; }
    }

    private Pegawai pegawai(Object target) {
        try { return (Pegawai) target.getClass().getMethod("getPegawai", new Class[0]).invoke(target, new Object[0]); }
        catch (Exception invalid) { return null; }
    }

    private boolean status(Object target) {
        try { return Boolean.TRUE.equals(target.getClass().getMethod("getStatus", new Class[0]).invoke(target, new Object[0])); }
        catch (Exception invalid) { return false; }
    }

    private void set(Object target, String method, Class type, Object value) throws Exception {
        Method setter = target.getClass().getMethod(method, new Class[] { type });
        setter.invoke(target, new Object[] { value });
    }

    private void compareNumbers(Map values, String start, String end, List errors) {
        if (!values.containsKey(start) || !values.containsKey(end)) return;
        try {
            int first = ((Integer) GenericCrudValueConverter.convert(values.get(start), Integer.class)).intValue();
            int last = ((Integer) GenericCrudValueConverter.convert(values.get(end), Integer.class)).intValue();
            if (last < first) errors.add(end + ":Nilai akhir tidak boleh sebelum nilai awal");
        } catch (Exception ignored) { }
    }

    private void compareDates(Map values, String start, String end, List errors) {
        try {
            java.util.Date first = (java.util.Date) GenericCrudValueConverter.convert(values.get(start), java.util.Date.class);
            java.util.Date last = (java.util.Date) GenericCrudValueConverter.convert(values.get(end), java.util.Date.class);
            if (first != null && last != null && last.before(first)) errors.add(end + ":Tanggal akhir tidak boleh sebelum tanggal awal");
        } catch (Exception ignored) { }
    }

    private boolean same(Pegawai one, Pegawai two) { return one != null && two != null && one.getId() != null && one.getId().equals(two.getId()); }
    private void deny() throws GenericCrudException { throw new GenericCrudException(403, "OBJECT_OUT_OF_SCOPE", "Riwayat berada di luar scope pegawai aktif."); }
}
