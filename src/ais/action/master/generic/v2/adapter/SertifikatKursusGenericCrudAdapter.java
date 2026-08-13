package ais.action.master.generic.v2.adapter;

import java.net.URLEncoder;
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
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.kursus.PesertaKursus;
import ais.database.model.kursus.SertifikatKursus;

/** Review/pencabutan sertifikat; penerbitan tetap otomatis oleh service kursus. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class SertifikatKursusGenericCrudAdapter extends GenericCrudAutoEntityAdapter
        implements GenericCrudCustomActionProvider {
    private static final String VERIFY = "public_verification";
    public SertifikatKursusGenericCrudAdapter() { super(SertifikatKursus.class, false, null, true); }

    public void configure(GenericCrudDefinition definition) {
        definition.setDisplayName("Sertifikat Kursus");
        definition.setCreateEnabled(false); definition.setUpdateEnabled(true);
        definition.setDeleteEnabled(false); definition.setImportEnabled(false);
        definition.setDefaultSortProperty("tanggalTerbit"); definition.setDefaultSortAscending(false);
        definition.setDefaultPageSize(25);
        List fields = definition.getFields();
        for (int i = 0; i < fields.size(); i++) {
            GenericCrudFieldDefinition field = (GenericCrudFieldDefinition) fields.get(i);
            field.setCreateable(false); field.setUpdateable("status".equals(field.getProperty()));
            if ("status".equals(field.getProperty())) {
                field.setEditorType("select");
                field.setEnumValues(new String[] { SertifikatKursus.AKTIF, SertifikatKursus.DICABUT });
            }
        }
    }

    public void beforeSave(Session session, GeneralValueObject target,
            GenericCrudRequestContext context) throws Exception {
        SertifikatKursus value = (SertifikatKursus) target;
        if (!SertifikatKursus.AKTIF.equals(value.getStatus())
                && !SertifikatKursus.DICABUT.equals(value.getStatus()))
            throw new GenericCrudException(400, "CERTIFICATE_STATUS_INVALID", "Status harus Aktif atau Dicabut.");
        Tbmuser user = context == null ? null : context.getUser();
        if (user != null) { value.setOleh(user.getUserNama()); value.setOlehId(user.getUserId()); }
        super.beforeSave(session, target, context);
    }

    public void applyReadScope(Criteria criteria, GenericCrudRequestContext context) {
        applyParticipantScope(criteria, context == null ? null : context.getUser());
    }
    public void applyCountScope(Criteria criteria, GenericCrudRequestContext context) {
        applyParticipantScope(criteria, context == null ? null : context.getUser());
    }
    public void validateObjectScope(GeneralValueObject object, GenericCrudRequestContext context) throws Exception {
        authorize(context);
        if (!(object instanceof SertifikatKursus))
            throw new GenericCrudException(403, "OBJECT_OUTSIDE_SCOPE", "Sertifikat tidak valid.");
        PesertaKursus participant = context.getUser().getPesertaKursus();
        if (ais.common.Common.getApakahAdmin() || participant == null) return;
        SertifikatKursus certificate = (SertifikatKursus) object;
        if (certificate.getPesertaPunyaProdukKursus() == null
                || !participant.equals(certificate.getPesertaPunyaProdukKursus().getPesertaKursus()))
            throw new GenericCrudException(403, "OBJECT_OUTSIDE_SCOPE", "Sertifikat bukan milik peserta aktif.");
    }

    public List getActions(GenericCrudDefinition definition, GenericCrudRequestContext context) {
        List result = new ArrayList(); Map action = new LinkedHashMap();
        action.put("actionKey", VERIFY); action.put("label", "Verifikasi Publik");
        action.put("requiredPrivilege", GenericCrudOperation.READ); action.put("selectionMode", "SINGLE");
        action.put("enabled", Boolean.valueOf(context != null && context.isCanRead()));
        action.put("dangerous", Boolean.FALSE); action.put("parameterNames", new ArrayList());
        result.add(action); return result;
    }

    public GenericCrudResult execute(String actionKey, List selectedIds, Map parameters,
            GenericCrudRequestContext context) throws Exception {
        if (!VERIFY.equals(actionKey)) return GenericCrudResult.error("ACTION_NOT_ALLOWED", "Aksi tidak dikenal.");
        Long id;
        try { id = Long.valueOf(String.valueOf(selectedIds.get(0))); }
        catch (Exception invalid) { throw new GenericCrudException(400, "CERTIFICATE_ID_INVALID", "ID sertifikat tidak valid."); }
        Session session = HibernateUtil.currentNativeSession();
        try {
            SertifikatKursus value = (SertifikatKursus) session.get(SertifikatKursus.class, id);
            if (value == null) throw new GenericCrudException(404, "ROW_NOT_FOUND", "Sertifikat tidak ditemukan.");
            new GenericCrudScopeGuard().validateObject(value, context);
            String root = context.getRequest() == null ? "" : context.getRequest().getContextPath();
            Map data = new LinkedHashMap(); data.put("redirectUrl", root
                    + "/VerifikasiSertifikatKursus?kode=" + URLEncoder.encode(value.getKode(), "UTF-8"));
            return GenericCrudResult.ok("Halaman verifikasi dibuka.", data);
        } finally { HibernateUtil.closeSession(); }
    }

    public List getNaturalKeyProperties() { return new ArrayList(); }
    private void applyParticipantScope(Criteria criteria, Tbmuser user) {
        if (criteria == null) return;
        if (user == null) { criteria.add(Restrictions.sqlRestriction("1=0")); return; }
        if (ais.common.Common.getApakahAdmin()) return;
        PesertaKursus participant = user.getPesertaKursus();
        if (participant != null)
            criteria.createAlias("pesertaPunyaProdukKursus", "scopeEnrollment")
                    .add(Restrictions.eq("scopeEnrollment.pesertaKursus", participant));
        // Pengelola kursus tanpa relasi peserta mengikuti assignment menu/RBAC.
    }
}
