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

/**
 * Model pohon ZK ({@link AbstractTreeModel}) untuk hierarki dokumen akreditasi
 * ({@link DokumenAkreditasi}, self-referencing lewat field {@code induk}) pada satu {@link Akreditasi}
 * opsional. Setiap simpul difilter menurut satuan kerja: hanya dokumen milik satuan kerja yang dipilih
 * (atau turunannya, lewat {@link SatuanKerjaTreeModel#getChildsSet}) beserta dokumen tanpa satuan
 * kerja ({@code satuanKerja IS NULL}, dianggap dokumen umum) yang ditampilkan. Satuan kerja acuan
 * diambil, secara berurutan prioritasnya: dari komponen {@link AmbilDataSatuanKerjaBanbox} bila
 * diberikan, lalu satuan kerja default {@link PerguruanTinggiUtil#getPerguruanTinggi()}, atau satuan
 * kerja pengguna yang login (untuk konstruktor simpul turunan).
 *
 * <p>
 * Hasil query anak-anak ({@link #getChildren}) dan jumlah anak ({@link #getChildCount}) di-cache
 * per-kunci simpul ({@code cacheChildren}/{@code cacheCount}) untuk menghindari query berulang saat
 * ZK memanggil method model berkali-kali ketika membangun tampilan pohon; panggil
 * {@link #clearCache()} setelah data dokumen berubah agar pohon dimuat ulang dari database.
 * </p>
 */
public class DokumenAkreditasiTreeModel extends AbstractTreeModel {

    private static final long serialVersionUID = -5115651721345571411L;

    private Akreditasi akreditasi;
    private SatuanKerja satuanKerja;
    private SatuanKerjaTreeModel satuanKerjaTreeModel;
    private AmbilDataSatuanKerjaBanbox ambilDataSatuanKerjaBanbox;
    private Map<String, List<DokumenAkreditasi>> cacheChildren = new HashMap<String, List<DokumenAkreditasi>>();
    private Map<String, Integer> cacheCount = new HashMap<String, Integer>();

    /**
     * Model pohon akar dengan filter akreditasi, satuan kerja acuan diambil dari kombo
     * {@code ambilDataSatuanKerjaBanbox} (bila pengguna memilih satuan kerja lewat komponen tersebut)
     * atau default satuan kerja perguruan tinggi.
     */
    public DokumenAkreditasiTreeModel(Akreditasi akreditasi, AmbilDataSatuanKerjaBanbox ambilDataSatuanKerjaBanbox) {
        super(null);
        this.akreditasi = akreditasi;
        this.ambilDataSatuanKerjaBanbox = ambilDataSatuanKerjaBanbox;
        this.satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
        initDefaultSatuanKerja();
    }

    /** Model pohon akar dengan filter akreditasi, satuan kerja acuan default satuan kerja perguruan tinggi. */
    public DokumenAkreditasiTreeModel(Akreditasi akreditasi) {
        super(null);
        this.akreditasi = akreditasi;
        this.satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
        initDefaultSatuanKerja();
    }

    /** Model pohon akar tanpa filter akreditasi (seluruh dokumen akreditasi), satuan kerja acuan default satuan kerja perguruan tinggi. */
    public DokumenAkreditasiTreeModel() {
        super(null);
        this.satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
        initDefaultSatuanKerja();
    }

    /** Model pohon berakar pada simpul {@code indukDokumenAkreditasi} tertentu (bukan akar sebenarnya), satuan kerja acuan diambil dari satuan kerja milik pengguna yang login. */
    public DokumenAkreditasiTreeModel(DokumenAkreditasi indukDokumenAkreditasi) {
        super(indukDokumenAkreditasi);
        this.satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
        Tbmuser tbmuser = Common.getCurrentUser();
        this.satuanKerja = tbmuser == null ? null : tbmuser.ambilSatuanKerja();
    }

    /** Menetapkan satuan kerja acuan default dari satuan kerja milik {@link PerguruanTinggiUtil#getPerguruanTinggi()}; kegagalan (mis. belum ada data PT) diperlakukan sebagai "tanpa filter satuan kerja" (satuan kerja {@code null}). */
    private void initDefaultSatuanKerja() {
        try {
            PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi();
            this.satuanKerja = pt == null ? null : pt.getSatuanKerja();
        } catch (Exception e) {
            this.satuanKerja = null;
        }
    }

    /** Mengosongkan cache anak-anak/jumlah-anak per simpul; panggil setelah data dokumen akreditasi berubah agar pohon dimuat ulang dari database. */
    public void clearCache() {
        cacheChildren.clear();
        cacheCount.clear();
    }

    /**
     * Mengumpulkan secara rekursif seluruh id {@link DokumenAkreditasi} turunan (langsung maupun tak
     * langsung) dari {@code induk} ke dalam {@code longs}, mengikuti filter satuan kerja yang sama
     * dengan {@link #getChildren}. Tidak melakukan apa-apa bila {@code induk} atau {@code longs}
     * {@code null}.
     *
     * @param induk id dokumen akreditasi induk
     * @param longs list keluaran yang diisi id turunan (unik, tanpa duplikat)
     */
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

    /**
     * Mengambil (dari cache bila tersedia, atau query dan meng-cache) daftar anak langsung dari
     * {@code indukDokumenAkreditasi}, difilter akreditasi (bila diset) dan satuan kerja acuan,
     * diurutkan berdasarkan nomor urut lalu kode lalu nama.
     *
     * @param indukDokumenAkreditasi dokumen induk; {@code null} mengambil dokumen level akar
     * @return daftar dokumen anak yang cocok
     */
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

    /**
     * Mengumpulkan secara rekursif seluruh entitas {@link DokumenAkreditasi} turunan dari
     * {@code indukDokumenAkreditasi} (langsung maupun tak langsung) ke dalam {@code dokumenAkreditasis}.
     * Tidak melakukan apa-apa bila {@code dokumenAkreditasis} {@code null}.
     */
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

    /** Implementasi {@link AbstractTreeModel}: mengembalikan anak ke-{@code index} milik {@code induk}, atau {@code null} bila indeks di luar jangkauan. */
    public Object getChild(Object induk, int index) {
        List<DokumenAkreditasi> dokumenAkreditasis = getChildren((DokumenAkreditasi) induk);
        if (dokumenAkreditasis == null || index < 0 || index >= dokumenAkreditasis.size()) {
            return null;
        }
        return dokumenAkreditasis.get(index);
    }

    /** Implementasi {@link AbstractTreeModel}: jumlah anak milik {@code induk} (memakai cache bila tersedia). */
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

    /** Implementasi {@link AbstractTreeModel}: {@code true} bila {@code induk} tidak memiliki anak. */
    public boolean isLeaf(Object induk) {
        return getChildCount(induk) == 0;
    }

    /** Membangun kriteria Hibernate pembatas satuan kerja: dokumen milik satuan kerja acuan (atau turunannya) ATAU dokumen tanpa satuan kerja (umum). Tanpa satuan kerja acuan sama sekali, tidak membatasi apa pun. */
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

    /** Mengambil satuan kerja acuan aktif: dari atribut komponen {@link AmbilDataSatuanKerjaBanbox} bila diberikan dan sudah dipilih, atau {@code satuanKerja} default kelas. */
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
