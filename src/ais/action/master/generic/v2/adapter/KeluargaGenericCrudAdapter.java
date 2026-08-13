package ais.action.master.generic.v2.adapter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;
import ais.database.model.employ.Keluarga;

/**
 * Port native aturan KeluargaPegawaiHelper. Adapter ini sengaja tidak
 * menjalankan composer ZK: seluruh lifecycle dipanggil oleh service New UI.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class KeluargaGenericCrudAdapter extends AbstractGenericCrudEntityAdapter<Keluarga>
        implements GenericCrudScopeAdapter, GenericCrudApprovalAdapter {

    public Keluarga createNew(GenericCrudRequestContext context) throws Exception {
        Keluarga value = new Keluarga();
        value.setPegawai(owner(context));
        value.setStatus(Boolean.FALSE);
        value.setMenikah(Boolean.FALSE);
        return value;
    }

    public void validateCreate(Map values, GenericCrudRequestContext context, List errors) throws Exception {
        validate(null, values, context, errors);
    }

    public void validateUpdate(Keluarga current, Map values,
            GenericCrudRequestContext context, List errors) throws Exception {
        validate(current, values, context, errors);
    }

    private void validate(Keluarga current, Map values,
            GenericCrudRequestContext context, List errors) throws Exception {
        required(values, "hubungan", "Hubungan Keluarga", errors);
        required(values, "nama", "Nama", errors);
        required(values, "jenisKelamin", "Jenis Kelamin", errors);
        required(values, "alamat", "Alamat", errors);
        required(values, "pekerjaan", "Pekerjaan", errors);
        required(values, "tempatLahir", "Tempat Lahir", errors);
        required(values, "tanggalLahir", "Tanggal Lahir", errors);
        required(values, "keteranganTambahan", "Keterangan Tambahan", errors);

        Object relation = values.get("hubungan");
        if (relation != null && !relations().contains(String.valueOf(relation))) {
            errors.add("hubungan:Hubungan Keluarga tidak valid");
        }
        Object gender = values.get("jenisKelamin");
        if (gender != null && !("Laki-laki".equals(gender) || "Perempuan".equals(gender))) {
            errors.add("jenisKelamin:Jenis Kelamin tidak valid");
        }
        if (current != null && current.getStatus().booleanValue() && !context.isCanApprove()) {
            errors.add("_global:Data yang sudah disetujui hanya dapat diubah oleh pemegang privilege APPROVE");
        }
    }

    public void applyCreateValues(Keluarga target, Map values,
            GenericCrudRequestContext context) throws Exception {
        Map safe = withoutApproval(values);
        super.applyCreateValues(target, safe, context);
        Pegawai owner = owner(context);
        if (owner != null) target.setPegawai(owner);
        target.setStatus(Boolean.FALSE);
    }

    public void applyUpdateValues(Keluarga target, Map values,
            GenericCrudRequestContext context) throws Exception {
        super.applyUpdateValues(target, withoutApproval(values), context);
    }

    private Map withoutApproval(Map values) {
        Map safe = new java.util.LinkedHashMap(values);
        safe.remove("status");
        return safe;
    }

    public void beforeSave(Session session, Keluarga target,
            GenericCrudRequestContext context) throws Exception {
        if (target.getPegawai() == null) {
            throw new GenericCrudException(400, "KELUARGA_PEGAWAI_REQUIRED", "Data Pegawai wajib dipilih.");
        }
        if (target.getStatus() == null) target.setStatus(Boolean.FALSE);
    }

    public boolean canDelete(Keluarga target, GenericCrudRequestContext context, List reasons) {
        if (target.getStatus().booleanValue()) {
            reasons.add("Data keluarga yang sudah disetujui tidak dapat dihapus.");
            return false;
        }
        return true;
    }

    public void delete(Session session, Keluarga target,
            GenericCrudRequestContext context) throws Exception {
        session.delete(target);
    }

    public List getNaturalKeyProperties() {
        List keys = new ArrayList();
        keys.add("pegawai");
        keys.add("hubungan");
        keys.add("nama");
        return keys;
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
        Session session = HibernateUtil.currentNativeSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            Keluarga target = (Keluarga) session.get(Keluarga.class, id);
            if (target == null) throw new GenericCrudException(404, "ROW_NOT_FOUND", "Data keluarga tidak ditemukan.");
            validateObjectScope(target, context);
            target.setStatus(Boolean.valueOf(approved));
            session.saveOrUpdate(target);
            session.flush();
            tx.commit();
            CommonPrivilages.saveActivity(getClass(), CommonPrivilages.APPROVE, target,
                    approved ? "Persetujuan keluarga New UI" : "Pembatalan persetujuan keluarga New UI");
            return GenericCrudResult.ok(approved ? "Data keluarga disetujui." : "Persetujuan data keluarga dibatalkan.", null);
        } catch (Exception error) {
            try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignored) { }
            throw error;
        } finally {
            HibernateUtil.closeSession();
        }
    }

    public void applyReadScope(Criteria criteria, GenericCrudRequestContext context) throws Exception {
        applyScope(criteria, context);
    }

    public void applyCountScope(Criteria criteria, GenericCrudRequestContext context) throws Exception {
        applyScope(criteria, context);
    }

    private void applyScope(Criteria criteria, GenericCrudRequestContext context) throws Exception {
        if (context.getUser() == null) {
            criteria.add(Restrictions.sqlRestriction("1=0"));
            return;
        }
        Pegawai employee = owner(context);
        if (employee != null) {
            criteria.add(Restrictions.eq("pegawai", employee));
            return;
        }
        Object unit = context.getUser().getSatuanKerja();
        if (unit != null) {
            criteria.createAlias("pegawai", "scopePegawai");
            criteria.add(Restrictions.eq("scopePegawai.satuanKerja", unit));
        }
    }

    public void validateObjectScope(GeneralValueObject object,
            GenericCrudRequestContext context) throws Exception {
        if (!(object instanceof Keluarga) || context.getUser() == null) deny();
        Keluarga family = (Keluarga) object;
        Pegawai employee = owner(context);
        if (employee != null) {
            if (!same(employee, family.getPegawai())) deny();
            return;
        }
        Object allowedUnit = context.getUser().getSatuanKerja();
        Object actualUnit = family.getPegawai() == null ? null : family.getPegawai().getSatuanKerja();
        if (allowedUnit != null && !allowedUnit.equals(actualUnit)) deny();
    }

    private Pegawai owner(GenericCrudRequestContext context) {
        try { return context.getUser() == null ? null : context.getUser().ambilPegawai(); }
        catch (Exception unavailable) { return null; }
    }

    private boolean same(Pegawai one, Pegawai two) {
        return one != null && two != null && one.getId() != null && one.getId().equals(two.getId());
    }

    private void deny() throws GenericCrudException {
        throw new GenericCrudException(403, "OBJECT_OUT_OF_SCOPE", "Data keluarga berada di luar scope pegawai aktif.");
    }

    private void required(Map values, String key, String label, List errors) {
        if (!values.containsKey(key)) return;
        Object value = values.get(key);
        if (value == null || String.valueOf(value).trim().length() == 0) errors.add(key + ":" + label + " wajib diisi");
    }

    private List relations() {
        List result = new ArrayList();
        result.add(Keluarga.SUAMI);
        result.add(Keluarga.ISTRI);
        result.add(Keluarga.ANAK);
        result.add(Keluarga.MERTUA);
        result.add(Keluarga.ORANG_TUA);
        result.add(Keluarga.SAUDARA);
        return result;
    }
}
