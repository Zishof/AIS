package ais.action.master.helper.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.AbstractTreeModel;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Akreditasi;
import ais.database.model.DokumenAkreditasi;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;

public class DokumenAkreditasiTreeModel extends AbstractTreeModel {

    private static final long serialVersionUID = -5115651721345571411L;

    private Akreditasi akreditasi;
    private SatuanKerja satuanKerja;
    private SatuanKerjaTreeModel satuanKerjaTreeModel;
    private AmbilDataSatuanKerjaBanbox ambilDataSatuanKerjaBanbox;
    private Map<String, List<DokumenAkreditasi>> cacheChildren = new HashMap<String, List<DokumenAkreditasi>>();
    private Map<String, Integer> cacheCount = new HashMap<String, Integer>();

    public DokumenAkreditasiTreeModel(Akreditasi akreditasi, AmbilDataSatuanKerjaBanbox ambilDataSatuanKerjaBanbox) {
        super(null);
        this.akreditasi = akreditasi;
        this.ambilDataSatuanKerjaBanbox = ambilDataSatuanKerjaBanbox;
        this.satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
        initDefaultSatuanKerja();
    }

    public DokumenAkreditasiTreeModel(Akreditasi akreditasi) {
        super(null);
        this.akreditasi = akreditasi;
        this.satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
        initDefaultSatuanKerja();
    }

    public DokumenAkreditasiTreeModel() {
        super(null);
        this.satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
        initDefaultSatuanKerja();
    }

    public DokumenAkreditasiTreeModel(DokumenAkreditasi indukDokumenAkreditasi) {
        super(indukDokumenAkreditasi);
        this.satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
        Tbmuser tbmuser = Common.getCurrentUser();
        this.satuanKerja = tbmuser == null ? null : tbmuser.ambilSatuanKerja();
    }

    private void initDefaultSatuanKerja() {
        try {
            PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi();
            this.satuanKerja = pt == null ? null : pt.getSatuanKerja();
        } catch (Exception e) {
            this.satuanKerja = null;
        }
    }

    public void clearCache() {
        cacheChildren.clear();
        cacheCount.clear();
    }

    @SuppressWarnings("unchecked")
    public void getChildDeepSet(Long induk, List<Long> longs) {
        if (induk == null || longs == null) {
            return;
        }
        Session session = HibernateUtil.currentSession();
        List<Long> values = session.createCriteria(DokumenAkreditasi.class)
                .add(Restrictions.eq("induk.id", induk))
                .add(createSatuanKerjaRestriction())
                .setProjection(Projections.property("id"))
                .list();
        for (Long id : values) {
            if (id != null && !longs.contains(id)) {
                longs.add(id);
                getChildDeepSet(id, longs);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public List<DokumenAkreditasi> getChildren(DokumenAkreditasi indukDokumenAkreditasi) {
        String key = indukDokumenAkreditasi == null || indukDokumenAkreditasi.getId() == null ? "root"
                : indukDokumenAkreditasi.getId().toString();
        if (cacheChildren.containsKey(key)) {
            return cacheChildren.get(key);
        }
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(DokumenAkreditasi.class)
                .add(akreditasi == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("akreditasi", akreditasi))
                .add(indukDokumenAkreditasi == null ? Restrictions.isNull("induk") : Restrictions.eq("induk", indukDokumenAkreditasi))
                .add(createSatuanKerjaRestriction())
                .addOrder(Order.asc("nomorUrut"))
                .addOrder(Order.asc("kode"))
                .addOrder(Order.asc("nama"));
        List<DokumenAkreditasi> result = ConstantValues.simpleList(criteria, DokumenAkreditasi.class);
        cacheChildren.put(key, result);
        cacheCount.put(key, Integer.valueOf(result == null ? 0 : result.size()));
        return result;
    }

    public void generateAllChildren(DokumenAkreditasi indukDokumenAkreditasi, Set<DokumenAkreditasi> dokumenAkreditasis) {
        if (dokumenAkreditasis == null) {
            return;
        }
        List<DokumenAkreditasi> children = getChildren(indukDokumenAkreditasi);
        for (DokumenAkreditasi child : children) {
            if (child != null && dokumenAkreditasis.add(child)) {
                generateAllChildren(child, dokumenAkreditasis);
            }
        }
    }

    public Object getChild(Object induk, int index) {
        List<DokumenAkreditasi> dokumenAkreditasis = getChildren((DokumenAkreditasi) induk);
        if (dokumenAkreditasis == null || index < 0 || index >= dokumenAkreditasis.size()) {
            return null;
        }
        return dokumenAkreditasis.get(index);
    }

    public int getChildCount(Object induk) {
        String key = induk == null || ((DokumenAkreditasi) induk).getId() == null ? "root"
                : ((DokumenAkreditasi) induk).getId().toString();
        Integer cached = cacheCount.get(key);
        if (cached != null) {
            return cached.intValue();
        }
        List<DokumenAkreditasi> children = getChildren((DokumenAkreditasi) induk);
        return children == null ? 0 : children.size();
    }

    public boolean isLeaf(Object induk) {
        return getChildCount(induk) == 0;
    }

    private org.hibernate.criterion.Criterion createSatuanKerjaRestriction() {
        SatuanKerja parent = getSelectedSatuanKerja();
        Set<SatuanKerja> satuanKerjas = new HashSet<SatuanKerja>();
        try {
            satuanKerjas.addAll(ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas());
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/util/DokumenAkreditasiTreeModel.java:160");
        }
        if (parent != null) {
            satuanKerjas.clear();
            satuanKerjas.add(parent);
            satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
        } else if (satuanKerja != null) {
            satuanKerjas.add(satuanKerja);
            satuanKerjaTreeModel.getChildsSet(satuanKerja, satuanKerjas);
        }
        if (satuanKerjas == null || satuanKerjas.isEmpty()) {
            return Restrictions.sqlRestriction("1=1");
        }
        return Restrictions.or(Restrictions.isNull("satuanKerja"), Restrictions.in("satuanKerja", satuanKerjas));
    }

    private SatuanKerja getSelectedSatuanKerja() {
        if (ambilDataSatuanKerjaBanbox != null && ambilDataSatuanKerjaBanbox.getAttribute("satuanKerja") != null) {
            try {
                return (SatuanKerja) ambilDataSatuanKerjaBanbox.getAttribute("satuanKerja");
            } catch (Exception e) {
                return null;
            }
        }
        return satuanKerja;
    }
}
