package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudFieldDefinition;
import ais.action.master.generic.v2.GenericCrudOperation;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailKelompokKegiatanKemahasiswaan;
import ais.database.model.GeneralValueObject;
import ais.database.model.JabatanKegiatanKemahasiswaan;
import ais.database.model.NilaiKegiatanKemahasiswaan;
import ais.database.model.SkalaKegiatanKemahasiswaan;
import ais.database.model.Tbmuser;

/** Matrix angka kredit kegiatan mahasiswa dari NilaiKegiatanKemahasiswaanAction. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class NilaiKegiatanKemahasiswaanGenericCrudAdapter extends GenericCrudAutoEntityAdapter
        implements GenericCrudCustomActionProvider {
    private static final String INITIALIZE = "initialize_score_matrix";

    public NilaiKegiatanKemahasiswaanGenericCrudAdapter() {
        super(NilaiKegiatanKemahasiswaan.class, false, null, true);
    }

    public void configure(GenericCrudDefinition definition) {
        definition.setDisplayName("Nilai Kegiatan Kemahasiswaan");
        definition.setCreateEnabled(false); definition.setUpdateEnabled(true);
        definition.setDeleteEnabled(false); definition.setImportEnabled(false);
        definition.setDefaultSortProperty("kodeUnik"); definition.setDefaultSortAscending(true);
        definition.setDefaultPageSize(50);
        List fields = definition.getFields();
        for (int i = 0; i < fields.size(); i++) {
            GenericCrudFieldDefinition field = (GenericCrudFieldDefinition) fields.get(i);
            field.setCreateable(false); field.setUpdateable("nilai".equals(field.getProperty()));
            field.setTableVisible("id".equals(field.getProperty()) || "detailKelompokKegiatanKemahasiswaan".equals(field.getProperty())
                    || "jabatanKegiatanKemahasiswaan".equals(field.getProperty())
                    || "skalaKegiatanKemahasiswaan".equals(field.getProperty()) || "nilai".equals(field.getProperty()));
        }
    }

    public void beforeSave(Session session, GeneralValueObject target, GenericCrudRequestContext context) throws Exception {
        NilaiKegiatanKemahasiswaan value = (NilaiKegiatanKemahasiswaan) target;
        DetailKelompokKegiatanKemahasiswaan detail = value.getDetailKelompokKegiatanKemahasiswaan();
        SkalaKegiatanKemahasiswaan scale = value.getSkalaKegiatanKemahasiswaan();
        if (detail == null || scale == null || !detail.getSkalaKegiatanKemahasiswaans().contains(scale))
            throw new GenericCrudException(400, "SCORE_MATRIX_INVALID", "Skala tidak terhubung ke rincian kegiatan.");
        Set positions = detail.getJabatanKegiatanKemahasiswaans();
        if (!positions.isEmpty() && (value.getJabatanKegiatanKemahasiswaan() == null
                || !positions.contains(value.getJabatanKegiatanKemahasiswaan())))
            throw new GenericCrudException(400, "SCORE_POSITION_INVALID", "Jabatan tidak terhubung ke rincian kegiatan.");
        if (positions.isEmpty() && value.getJabatanKegiatanKemahasiswaan() != null)
            throw new GenericCrudException(400, "SCORE_POSITION_INVALID", "Rincian ini tidak memakai jabatan.");
        Tbmuser user = context == null ? null : context.getUser();
        if (user != null) { value.setOleh(user.getUserNama()); value.setOlehId(user.getUserId()); }
        super.beforeSave(session, target, context);
    }

    public List getActions(GenericCrudDefinition definition, GenericCrudRequestContext context) {
        List result = new ArrayList(); Map action = new LinkedHashMap();
        action.put("actionKey", INITIALIZE); action.put("label", "Lengkapi Matriks Nilai");
        action.put("requiredPrivilege", GenericCrudOperation.UPDATE); action.put("selectionMode", "NONE");
        action.put("enabled", Boolean.valueOf(context != null && context.isCanUpdate()));
        action.put("dangerous", Boolean.FALSE); action.put("parameterNames", new ArrayList());
        result.add(action); return result;
    }

    public GenericCrudResult execute(String actionKey, List selectedIds, Map parameters,
            GenericCrudRequestContext context) throws Exception {
        if (!INITIALIZE.equals(actionKey)) return GenericCrudResult.error("ACTION_NOT_ALLOWED", "Aksi tidak dikenal.");
        Session session = HibernateUtil.currentNativeSession(); int created = 0;
        try {
            session.beginTransaction();
            List details = session.createCriteria(DetailKelompokKegiatanKemahasiswaan.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
                    .addOrder(Order.asc("nomorUrut")).list();
            for (int i = 0; i < details.size(); i++) {
                DetailKelompokKegiatanKemahasiswaan detail = (DetailKelompokKegiatanKemahasiswaan) details.get(i);
                Set scales = detail.getSkalaKegiatanKemahasiswaans(); Set positions = detail.getJabatanKegiatanKemahasiswaans();
                for (Iterator si = scales.iterator(); si.hasNext();) {
                    SkalaKegiatanKemahasiswaan scale = (SkalaKegiatanKemahasiswaan) si.next();
                    if (!Boolean.TRUE.equals(scale.getAktif())) continue;
                    if (positions.isEmpty()) created += ensure(session, detail, scale, null, context);
                    else for (Iterator pi = positions.iterator(); pi.hasNext();)
                        created += ensure(session, detail, scale, (JabatanKegiatanKemahasiswaan) pi.next(), context);
                }
            }
            session.getTransaction().commit(); Map data = new LinkedHashMap(); data.put("created", Integer.valueOf(created));
            return GenericCrudResult.ok(created + " sel matriks nilai berhasil dilengkapi.", data);
        } catch (Exception failure) {
            if (session.getTransaction() != null && session.getTransaction().isActive()) session.getTransaction().rollback();
            throw failure;
        } finally { HibernateUtil.closeSession(); }
    }

    private int ensure(Session session, DetailKelompokKegiatanKemahasiswaan detail,
            SkalaKegiatanKemahasiswaan scale, JabatanKegiatanKemahasiswaan position,
            GenericCrudRequestContext context) {
        String key = detail.getId() + "-" + scale.getId() + "-" + (position == null ? "" : position.getId());
        Object existing = session.createCriteria(NilaiKegiatanKemahasiswaan.class)
                .add(Restrictions.eq("kodeUnik", key)).setMaxResults(1).uniqueResult();
        if (existing != null) return 0;
        NilaiKegiatanKemahasiswaan value = new NilaiKegiatanKemahasiswaan();
        value.setDetailKelompokKegiatanKemahasiswaan(detail); value.setSkalaKegiatanKemahasiswaan(scale);
        value.setJabatanKegiatanKemahasiswaan(position); value.setNilai(Double.valueOf(0)); value.setKodeUnik(key);
        Tbmuser user = context == null ? null : context.getUser();
        if (user != null) { value.setOleh(user.getUserNama()); value.setOlehId(user.getUserId()); }
        session.save(value); return 1;
    }

    public List getNaturalKeyProperties() { List result = new ArrayList(); result.add("kodeUnik"); return result; }
}
