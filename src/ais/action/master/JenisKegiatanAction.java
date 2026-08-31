package ais.action.master;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.ConstantValues;
import ais.database.dao.DaoFactory;
import ais.database.dao.JenisKegiatanDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.sekolah.KanalPembayaran;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyButtonTabbox;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;
import ais.ui.util.ZkCompat;

/**
 * Controller/action ZK untuk jenis kegiatan. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericCrudAction}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Tabpanel prasyaratTab}, {@code Textbox
 * kode}, {@code Textbox namaKegiatan}, {@code Textbox keterangan}, {@code MyCheckboxConfig aktif}, {@code
 * MyCheckboxConfig digunakanUntukPengecekanNilai}, {@code MyCheckboxConfig digunakanUntukPengecekanKrs}, {@code
 * MyCheckboxConfig digunakanUntukPengecekanUjian}; inisialisasi/lifecycle ({@code initCriteria()}, {@code
 * doAfterCompose()}); pembacaan/pencarian ({@code getEntityClass()}, {@code getWindowTitle()});
 * validasi/perhitungan ({@code petaSmtDariCheckbox()}, {@code checkNamaKegiatan()}); mutasi data ({@code
 * onSave()}); pelaporan/ekspor ({@code createRenderer()}); operasi domain lain ({@code createNewEntity()},
 * {@code onPrasyarat()}, {@code buildFormContent()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericCrudAction
 */
public class JenisKegiatanAction extends GenericCrudAction<JenisKegiatan> {

    private static final long serialVersionUID = 3786091220301468178L;

    // ZK auto-wired extra dari ZUL
    private Tabpanel prasyaratTab;

    // Form fields — direset setiap buildFormContent dipanggil
    private Textbox kode;
    private Textbox namaKegiatan;
    private Textbox keterangan;
    private MyCheckboxConfig aktif;
    private MyCheckboxConfig digunakanUntukPengecekanNilai;
    private MyCheckboxConfig digunakanUntukPengecekanKrs;
    private MyCheckboxConfig digunakanUntukPengecekanUjian;
    private MyCheckboxConfig digunakanSyaratKeaktifan;
    private MyIntbox minSmt;
    private MyIntbox maxSmt;
    private MyDoublebox persenSyaratLogin;
    private MyDoublebox persenSyaratLogin1;
    private MyDoublebox persenSyaratLogin2;
    private Checkbox digunakanSyaratLogin;
    private MyCheckboxConfig bayarHanyaSmtSaatIni;
    private MyCheckboxConfig bayarHanyaSmtSaatIniDanSebelumnya;
    private MyCheckboxConfig bayarHanyaSmtSaatIniDanSebelumnyalagi;
    private MyCheckboxConfig tidakBolehMengangsur;
    private MyCheckboxConfig tagihan_juga_untuk_alumni;
    private MyCheckboxConfig digunakanSyaratCetakSuratBebasAktif;
    private MyCkEditor penjelasanPembayaran;
    private MyDoublebox persenSyaratLogin3;
    private MyDoublebox persenSyaratLogin4;
    private MyCheckboxConfig bayarHanyaSmtSaatIniDanSebelumnyalagi3;
    private MyCheckboxConfig bayarHanyaSmtSaatIniDanSebelumnyalagi4;
    private MyDoublebox persenSyaratLogin5;
    private MyDoublebox persenSyaratLogin6;
    private MyDoublebox persenSyaratLogin7;
    private MyDoublebox persenSyaratLogin8;
    private MyCheckboxConfig bayarHanyaSmtSaatIniDanSebelumnyalagi5;
    private MyCheckboxConfig bayarHanyaSmtSaatIniDanSebelumnyalagi6;
    private MyCheckboxConfig bayarHanyaSmtSaatIniDanSebelumnyalagi7;
    private MyCheckboxConfig bayarHanyaSmtSaatIniDanSebelumnyalagi8;
    private MyCheckboxConfig dendaJikaTerlambat;
    private MyCheckboxConfig nilaiDendaDalamPersen;
    private MyIntbox dendaAkanBerlipatTerlambaHari;
    private MyIntbox maksimalBerlipatTerlambaHari;
    private MyDoublebox defaultProsentaseDenda;
    private MyCheckboxConfig hanyaBerupaAngsuran;
    private MyDoublebox persenSyaratLogin9;
    private MyDoublebox persenSyaratLogin10;
    private MyDoublebox persenSyaratLogin11;
    private MyDoublebox persenSyaratLogin12;
    private MyDoublebox persenSyaratLogin13;
    private MyDoublebox persenSyaratLogin14;
    private MyCheckboxConfig bayarHanyaSmtSaatIniDanSebelumnyalagi9;
    private MyCheckboxConfig bayarHanyaSmtSaatIniDanSebelumnyalagi10;
    private MyCheckboxConfig bayarHanyaSmtSaatIniDanSebelumnyalagi11;
    private MyCheckboxConfig bayarHanyaSmtSaatIniDanSebelumnyalagi12;
    private MyCheckboxConfig bayarHanyaSmtSaatIniDanSebelumnyalagi13;
    private MyCheckboxConfig bayarHanyaSmtSaatIniDanSebelumnyalagi14;
    private MyCheckboxConfig untukBayarSP;
    private org.zkoss.zul.Combobox kanalPembayaran;
    private MyCheckboxConfig hanyaBerupaBukanAngsuran;
    private List<Checkbox> jenjangHarusCheckboxes;
    private List<Checkbox> jenjangBukanCheckboxes;
    private MyCheckboxConfig abaikanNilaiMinus;
    private MyCheckboxConfig dendaDibuatPerProdi;
    private JSONObject dendaPerProdi;
    private Textbox prefixKodePembayaran;
    private Textbox namaBankPembayaran;

    // ======================== Abstract implementations ========================

    @Override
    protected Class<JenisKegiatan> getEntityClass() { return JenisKegiatan.class; }

    @Override
    protected JenisKegiatan createNewEntity() { return new JenisKegiatan(); }

    @Override
    protected String getWindowTitle() { return "Pendataan Jenis Kegiatan / Pembayaran"; }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(JenisKegiatan.class)
                .add(searchaktif != null && searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"));
        if (order) criteria.addOrder(Order.asc("namaKegiatan"));
        criteria.add(searchnama == null || searchnama.getValue().trim().isEmpty()
                ? Restrictions.sqlRestriction("true")
                : Restrictions.ilike("namaKegiatan", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new JenisKegiatanRenderer();
    }

    // ======================== doAfterCompose ========================

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        Div mainContainer = (Div) comp.getFellow("mainContainer");

        int[] tabAktif = {0};
        MyButtonTabbox btabs = MyButtonTabbox.buat(mainContainer, "100%", tabAktif);

        // Tab 0: Jenis Pembayaran (inline — eager)
        // PERBAIKAN (grid selalu kosong): harus muatZulEager, BUKAN muatZul/Include.
        // Include me-render kontennya tertunda & di ID-space terpisah, sehingga
        // grid/searchnama/searchaktif/paging tidak pernah ter-autowire oleh
        // GenericCrudAction dan onSearchDefault tidak pernah mengisi data.
        Div panel0 = btabs.tambahTab(0, "Jenis Pembayaran");
        MyButtonTabbox.muatZulEager(panel0,
                "/WEB-INF/z/x/y/pages/master/jenis_kegiatan_tab_0.zul");

        // Wire fields from the eager sub-ZUL before continuing
        super.doAfterCompose(comp);

        // Tab 1: Prasyarat Jenis Pembayaran (lazy)
        btabs.tambahTabLazy(1, "Prasyarat Jenis Pembayaran", new MyButtonTabbox.PemuatTab() {
            @Override
            public void muat(Div panel) throws Exception {
                MyWindow w = new MyWindow("", "none", false);
                w.setHeight("100%");
                w.setWidth("100%");
                w.setParent(panel);
                MyInclude iframe = new MyInclude("/pages/master/jenis_kegiatan_prasyarat.zul");
                iframe.setParent(w);
            }
        });

        // Tab 2: Kanal Pembayaran (lazy — include)
        final String srcKanal = "/WEB-INF/z/x/y/pages/master/sekolah/kanal_pembayaran.zul";
        btabs.tambahTabLazy(2, "Kanal Pembayaran", new MyButtonTabbox.PemuatTab() {
            @Override
            public void muat(Div panel) throws Exception {
                MyButtonTabbox.muatZul(panel, srcKanal);
            }
        });

        btabs.pulihkanSeleksi(3);
    }

    // ======================== ZUL event handler untuk tab prasyarat ========================

    public void onPrasyarat(Event event) {
        if (prasyaratTab.getChildren().size() == 0) {
            MyWindow window = new MyWindow("", "none", false);
            window.setHeight("100%");
            window.setWidth("100%");
            window.setParent(prasyaratTab);
            MyInclude iframe = new MyInclude("/pages/master/jenis_kegiatan_prasyarat.zul");
            iframe.setParent(window);
        }
    }

    // ======================== Form content ========================

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    protected void buildFormContent(MyWindow window, final JenisKegiatan jenisKegiatan) throws Exception {
        org.zkoss.zul.Borderlayout borderlayout = new MyBorderlayout();
        // Wajib mengisi penuh tinggi window agar Center (overflow:auto) punya tinggi terbatas
        // sehingga isi form yang panjang bisa di-SCROLL (bukan terpotong di luar window).
        borderlayout.setWidth("100%");
        borderlayout.setHeight("100%");

        // ---- Center: scrollable card ----
        org.zkoss.zul.Center center = new org.zkoss.zul.Center();
        center.setStyle("overflow:auto;padding:12px;background:#f0f4f8;");
        center.setParent(borderlayout);
        ZkCompat.setFlex(center, true);

        org.zkoss.zul.Div cardWrap = new org.zkoss.zul.Div();
        cardWrap.setStyle(FormBuilder.STYLE_CARD_WRAP);
        cardWrap.setParent(center);


        org.zkoss.zul.Grid grid = new org.zkoss.zul.Grid();
        grid.setStyle("border:none;width:100%;");
        grid.setParent(cardWrap);

        Rows rows = new Rows();
        rows.setParent(grid);

        FormBuilder fb = new FormBuilder(rows);

        kode = new Textbox(jenisKegiatan.getKode() == null ? "" : jenisKegiatan.getKode());
        kode.setWidth("100%");

        fb.addRow("Kode Pembayaran *", kode);

        namaKegiatan = new Textbox(jenisKegiatan.getNamaKegiatan() == null ? "" : jenisKegiatan.getNamaKegiatan());
        namaKegiatan.setWidth("100%");
        fb.addRow("Nama Pembayaran *", namaKegiatan);

        if (jenisKegiatan.getId() != null) {
            if ((ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
                    && jenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId()))
                    || (ConstantValues.PENDAFTARAN_MAHASISWA_LAMA != null
                            && jenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_MAHASISWA_LAMA.getId()))
                    || (ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
                            && jenisKegiatan.getId()
                                    .equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId()))
                    || (ConstantValues.PENDAFTARAN_WISUDA != null
                            && jenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_WISUDA.getId()))) {
                kode.setDisabled(true);
                namaKegiatan.setDisabled(true);
            }
        }

        minSmt = new MyIntbox(jenisKegiatan.getMinSmt());
        fb.addRow("Minimal Semester", minSmt);

        maxSmt = new MyIntbox(jenisKegiatan.getMaxSmt());
        fb.addRow("Maksimal Semester", maxSmt);

        untukBayarSP = new MyCheckboxConfig("Khusus untuk semester pendek");
        untukBayarSP.setChecked(jenisKegiatan.getUntukBayarSP());
        fb.addRow("", untukBayarSP);

        keterangan = new Textbox(jenisKegiatan.getKeterangan());
        keterangan.setWidth("100%");
        keterangan.setRows(4);
        fb.addRow("Keterangan Pembayaran", keterangan);

        digunakanUntukPengecekanKrs = new MyCheckboxConfig("Digunakan sebagai syarat pengambilan KRS");
        digunakanUntukPengecekanKrs.setChecked(jenisKegiatan.getDigunakanUntukPengecekanKrs());
        fb.addRow("", digunakanUntukPengecekanKrs);

        digunakanUntukPengecekanUjian = new MyCheckboxConfig("Digunakan sebagai syarat mencetak kartu ujian");
        digunakanUntukPengecekanUjian.setChecked(jenisKegiatan.getDigunakanUntukPengecekanUjian());
        fb.addRow("", digunakanUntukPengecekanUjian);

        digunakanUntukPengecekanNilai = new MyCheckboxConfig("Digunakan sebagai syarat melihat nilai");
        digunakanUntukPengecekanNilai.setChecked(jenisKegiatan.getDigunakanUntukPengecekanNilai());
        fb.addRow("", digunakanUntukPengecekanNilai);

        hanyaBerupaAngsuran = new MyCheckboxConfig("Jenis pembayaran ini harus berupa angsuran");
        hanyaBerupaAngsuran.setChecked(jenisKegiatan.getHanyaBerupaAngsuran());
        fb.addRow("", hanyaBerupaAngsuran);

        final MyFormRow rowJenjangHarus = new MyFormRow();
        rowJenjangHarus.setParent(rows);
        rowJenjangHarus.appendChild(new ais.ui.util.MyLabelConfig("  ↳ Berlaku untuk jenjang"));
        final Vbox vboxJenjangHarus = new Vbox();
        rowJenjangHarus.appendChild(vboxJenjangHarus);
        rowJenjangHarus.setVisible(hanyaBerupaAngsuran.isChecked());
        jenjangHarusCheckboxes = new ArrayList<Checkbox>();
        {
            JSONArray harusArr = new JSONArray();
            JSONObject harusSmtPeta = null;
            try {
                if (jenisKegiatan.getJenjangAngsuranJson() != null) {
                    JSONObject json = new JSONObject(jenisKegiatan.getJenjangAngsuranJson());
                    JSONArray tmp = json.optJSONArray("harus");
                    if (tmp != null) harusArr = tmp;
                    harusSmtPeta = json.optJSONObject("harus_smt");
                }
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/JenisKegiatanAction.java:271");}
            boolean defaultAllChecked = (harusArr.length() == 0);
            @SuppressWarnings("unchecked")
            List<Jenjang> allJenjangs = HibernateUtil.currentSession()
                    .createCriteria(Jenjang.class)
                    .add(Restrictions.eq("aktif", true))
                    .addOrder(Order.asc("kode")).list();
            for (Jenjang j : allJenjangs) {
                final Checkbox cb = new Checkbox(j.getNama() != null ? j.getNama() : String.valueOf(j.getId()));
                final String jid = String.valueOf(j.getId());
                cb.setAttribute("jkode", jid);
                if (defaultAllChecked) {
                    cb.setChecked(true);
                } else {
                    for (int i = 0; i < harusArr.length(); i++) {
                        try { if (jid.equals(harusArr.getString(i))) { cb.setChecked(true); break; } } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/JenisKegiatanAction.java:286");}
                    }
                }
                cb.addEventListener("onCheck", new EventListener() {
                    @Override
                    public void onEvent(Event arg0) throws Exception {}
                });
                // Field "Berlaku di smt" per jenjang: kosong = berlaku semua semester;
                // daftar dipisah koma (contoh 1,2,3,5,6 = berlaku smt tsb, kecuali smt 4).
                final Textbox smtBox = new Textbox();
                smtBox.setWidth("140px");
                smtBox.setTooltiptext("Berlaku di smt (jika kosong artinya berlaku di semua semester)."
                        + " Isi daftar semester dipisah koma, contoh: 1,2,3,5,6 (artinya berlaku di"
                        + " smt 1,2,3,5,6 — kecuali smt 4). Bisa juga khusus angkatan tertentu dengan"
                        + " format TAHUN:SMT, contoh: 2023:1 (angkatan 2023 hanya smt 1) atau"
                        + " 2023:1,2,3,4,5,8,2024:1 (angkatan 2023 smt 1-5 & 8, angkatan 2024 smt 1);"
                        + " angka setelah TAHUN: tetap milik angkatan itu sampai TAHUN: berikutnya."
                        + " 2023: (tanpa smt) berarti angkatan 2023 semua semester.");
                if (harusSmtPeta != null) smtBox.setValue(harusSmtPeta.optString(jid, ""));
                cb.setAttribute("smtbox", smtBox);
                org.zkoss.zul.Hbox barisJenjang = new org.zkoss.zul.Hbox();
                barisJenjang.appendChild(cb);
                barisJenjang.appendChild(smtBox);
                barisJenjang.appendChild(new ais.ui.util.MyLabelConfig(
                        "smt, atau TAHUN:smt utk angkatan tertentu (kosong = semua)"));
                barisJenjang.setParent(vboxJenjangHarus);
                jenjangHarusCheckboxes.add(cb);
            }
        }
        hanyaBerupaAngsuran.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                rowJenjangHarus.setVisible(hanyaBerupaAngsuran.isChecked());
            }
        });

        hanyaBerupaBukanAngsuran = new MyCheckboxConfig("Jenis pembayaran ini bukan berupa angsuran");
        hanyaBerupaBukanAngsuran.setChecked(jenisKegiatan.getHanyaBerupaBukanAngsuran());
        fb.addRow("", hanyaBerupaBukanAngsuran);

        final MyFormRow rowJenjangBukan = new MyFormRow();
        rowJenjangBukan.setParent(rows);
        rowJenjangBukan.appendChild(new ais.ui.util.MyLabelConfig("  ↳ Berlaku untuk jenjang"));
        final Vbox vboxJenjangBukan = new Vbox();
        rowJenjangBukan.appendChild(vboxJenjangBukan);
        rowJenjangBukan.setVisible(hanyaBerupaBukanAngsuran.isChecked());
        jenjangBukanCheckboxes = new ArrayList<Checkbox>();
        {
            JSONArray bukanArr = new JSONArray();
            JSONObject bukanSmtPeta = null;
            try {
                if (jenisKegiatan.getJenjangAngsuranJson() != null) {
                    JSONObject json = new JSONObject(jenisKegiatan.getJenjangAngsuranJson());
                    JSONArray tmp = json.optJSONArray("bukan");
                    if (tmp != null) bukanArr = tmp;
                    bukanSmtPeta = json.optJSONObject("bukan_smt");
                }
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/JenisKegiatanAction.java:322");}
            boolean defaultAllChecked = (bukanArr.length() == 0);
            @SuppressWarnings("unchecked")
            List<Jenjang> allJenjangsB = HibernateUtil.currentSession()
                    .createCriteria(Jenjang.class)
                    .add(Restrictions.eq("aktif", true))
                    .addOrder(Order.asc("kode")).list();
            for (Jenjang j : allJenjangsB) {
                final Checkbox cb = new Checkbox(j.getNama() != null ? j.getNama() : String.valueOf(j.getId()));
                final String jid = String.valueOf(j.getId());
                cb.setAttribute("jkode", jid);
                if (defaultAllChecked) {
                    cb.setChecked(true);
                } else {
                    for (int i = 0; i < bukanArr.length(); i++) {
                        try { if (jid.equals(bukanArr.getString(i))) { cb.setChecked(true); break; } } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/JenisKegiatanAction.java:337");}
                    }
                }
                cb.addEventListener("onCheck", new EventListener() {
                    @Override
                    public void onEvent(Event arg0) throws Exception {}
                });
                // Field "Berlaku di smt" per jenjang — sama seperti grup "harus angsuran".
                final Textbox smtBox = new Textbox();
                smtBox.setWidth("140px");
                smtBox.setTooltiptext("Berlaku di smt (jika kosong artinya berlaku di semua semester)."
                        + " Isi daftar semester dipisah koma, contoh: 1,2,3,5,6 (artinya berlaku di"
                        + " smt 1,2,3,5,6 — kecuali smt 4). Bisa juga khusus angkatan tertentu dengan"
                        + " format TAHUN:SMT, contoh: 2023:1 (angkatan 2023 hanya smt 1) atau"
                        + " 2023:1,2,3,4,5,8,2024:1 (angkatan 2023 smt 1-5 & 8, angkatan 2024 smt 1);"
                        + " angka setelah TAHUN: tetap milik angkatan itu sampai TAHUN: berikutnya."
                        + " 2023: (tanpa smt) berarti angkatan 2023 semua semester.");
                if (bukanSmtPeta != null) smtBox.setValue(bukanSmtPeta.optString(jid, ""));
                cb.setAttribute("smtbox", smtBox);
                org.zkoss.zul.Hbox barisJenjang = new org.zkoss.zul.Hbox();
                barisJenjang.appendChild(cb);
                barisJenjang.appendChild(smtBox);
                barisJenjang.appendChild(new ais.ui.util.MyLabelConfig(
                        "smt, atau TAHUN:smt utk angkatan tertentu (kosong = semua)"));
                barisJenjang.setParent(vboxJenjangBukan);
                jenjangBukanCheckboxes.add(cb);
            }
        }
        hanyaBerupaBukanAngsuran.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                rowJenjangBukan.setVisible(hanyaBerupaBukanAngsuran.isChecked());
            }
        });

        abaikanNilaiMinus = new MyCheckboxConfig(
                "Jika ada tagihan minus, abaikan nilainya untuk menghtung total tagihan");
        abaikanNilaiMinus.setChecked(jenisKegiatan.getAbaikanNilaiMinus());
        fb.addRow("", abaikanNilaiMinus);

        digunakanSyaratKeaktifan = new MyCheckboxConfig(
                "Digunakan sebagai syarat untuk mengaktifkan status mahasiswa");
        digunakanSyaratKeaktifan.setChecked(jenisKegiatan.getDigunakanSyaratKeaktifan());
        fb.addRow("", digunakanSyaratKeaktifan);

        digunakanSyaratLogin = new MyCheckboxConfig("Digunakan sebagai syarat login mahasiswa");
        digunakanSyaratLogin.setChecked(jenisKegiatan.getDigunakanSyaratLogin());
        fb.addRow("", digunakanSyaratLogin);

        final MyFormRow rowSyaratLogin = new MyFormRow();
        rowSyaratLogin.setParent(rows);
        rowSyaratLogin.appendChild(new ais.ui.util.MyLabelConfig(
                "Jika digunakan sebagai syarat login, berapa persen harus membayar ?"));
        rowSyaratLogin.appendChild(persenSyaratLogin = new MyDoublebox(jenisKegiatan.getPersenSyaratLogin()));
        rowSyaratLogin.setVisible(digunakanSyaratLogin.isChecked());

        final MyFormRow rowBayarHanyaSmtSaatIni = new MyFormRow();
        rowBayarHanyaSmtSaatIni.setParent(rows);
        rowBayarHanyaSmtSaatIni.appendChild(new ais.ui.util.MyLabelConfig(""));
        rowBayarHanyaSmtSaatIni.appendChild(bayarHanyaSmtSaatIni = new MyCheckboxConfig(
                "Persyaratan ini hanya khusus untuk semester saat mahasiswa login"));
        bayarHanyaSmtSaatIni.setChecked(jenisKegiatan.getBayarHanyaSmtSaatIni());
        rowBayarHanyaSmtSaatIni.setVisible(digunakanSyaratLogin.isChecked());

        final MyFormRow rowBayarHanyaSmtSaatIniDanSebelumnya = new MyFormRow();
        rowBayarHanyaSmtSaatIniDanSebelumnya.setParent(rows);
        rowBayarHanyaSmtSaatIniDanSebelumnya.appendChild(new ais.ui.util.MyLabelConfig(""));
        rowBayarHanyaSmtSaatIniDanSebelumnya.appendChild(
                bayarHanyaSmtSaatIniDanSebelumnya = new MyCheckboxConfig(
                        "Persyaratan ini juga hanya khusus untuk semester saat ini -1 saat mahasiswa login"));
        bayarHanyaSmtSaatIniDanSebelumnya.setChecked(jenisKegiatan.getBayarHanyaSmtSaatIniDanSebelumnya());
        rowBayarHanyaSmtSaatIniDanSebelumnya.setVisible(digunakanSyaratLogin.isChecked());

        final MyFormRow rowSyaratLogin1 = new MyFormRow();
        rowSyaratLogin1.setParent(rows);
        rowSyaratLogin1.appendChild(new ais.ui.util.MyLabelConfig(
                "Persyaratan ini juga hanya khusus untuk semester saat ini -1, berapa persen harus membayar ?"));
        rowSyaratLogin1.appendChild(persenSyaratLogin1 = new MyDoublebox(jenisKegiatan.getPersenSyaratLogin1()));
        rowSyaratLogin1.setVisible(digunakanSyaratLogin.isChecked());

        final MyFormRow rowBayarHanyaSmtSaatIniDanSebelumnyalagi = new MyFormRow();
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi.setParent(rows);
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi.appendChild(new ais.ui.util.MyLabelConfig(""));
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi.appendChild(
                bayarHanyaSmtSaatIniDanSebelumnyalagi = new MyCheckboxConfig(
                        "Persyaratan ini juga hanya khusus untuk semester saat ini -2 saat mahasiswa login"));
        bayarHanyaSmtSaatIniDanSebelumnyalagi.setChecked(jenisKegiatan.getBayarHanyaSmtSaatIniDanSebelumnyalagi());
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi.setVisible(digunakanSyaratLogin.isChecked());

        final MyFormRow rowSyaratLogin2 = new MyFormRow();
        rowSyaratLogin2.setParent(rows);
        rowSyaratLogin2.appendChild(new ais.ui.util.MyLabelConfig(
                "Persyaratan ini juga hanya khusus untuk semester saat ini -2, berapa persen harus membayar ?"));
        rowSyaratLogin2.appendChild(persenSyaratLogin2 = new MyDoublebox(jenisKegiatan.getPersenSyaratLogin2()));
        rowSyaratLogin2.setVisible(digunakanSyaratLogin.isChecked());

        final MyFormRow rowBayarHanyaSmtSaatIniDanSebelumnyalagi3 = new MyFormRow();
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi3.setParent(rows);
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi3.appendChild(new ais.ui.util.MyLabelConfig(""));
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi3.appendChild(
                bayarHanyaSmtSaatIniDanSebelumnyalagi3 = new MyCheckboxConfig(
                        "Persyaratan ini juga hanya khusus untuk semester saat ini -3 saat mahasiswa login"));
        bayarHanyaSmtSaatIniDanSebelumnyalagi3.setChecked(jenisKegiatan.getBayarHanyaSmtSaatIniDanSebelumnyalagi3());
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi3.setVisible(digunakanSyaratLogin.isChecked());

        final MyFormRow rowSyaratLogin3 = new MyFormRow();
        rowSyaratLogin3.setParent(rows);
        rowSyaratLogin3.appendChild(new ais.ui.util.MyLabelConfig(
                "Persyaratan ini juga hanya khusus untuk semester saat ini -3, berapa persen harus membayar ?"));
        rowSyaratLogin3.appendChild(persenSyaratLogin3 = new MyDoublebox(jenisKegiatan.getPersenSyaratLogin3()));
        rowSyaratLogin3.setVisible(digunakanSyaratLogin.isChecked());

        final MyFormRow rowBayarHanyaSmtSaatIniDanSebelumnyalagi4 = new MyFormRow();
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi4.setParent(rows);
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi4.appendChild(new ais.ui.util.MyLabelConfig(""));
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi4.appendChild(
                bayarHanyaSmtSaatIniDanSebelumnyalagi4 = new MyCheckboxConfig(
                        "Persyaratan ini juga hanya khusus untuk semester saat ini -4 saat mahasiswa login"));
        bayarHanyaSmtSaatIniDanSebelumnyalagi4.setChecked(jenisKegiatan.getBayarHanyaSmtSaatIniDanSebelumnyalagi4());
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi4.setVisible(digunakanSyaratLogin.isChecked());

        final MyFormRow rowSyaratLogin4 = new MyFormRow();
        rowSyaratLogin4.setParent(rows);
        rowSyaratLogin4.appendChild(new ais.ui.util.MyLabelConfig(
                "Persyaratan ini juga hanya khusus untuk semester saat ini -4, berapa persen harus membayar ?"));
        rowSyaratLogin4.appendChild(persenSyaratLogin4 = new MyDoublebox(jenisKegiatan.getPersenSyaratLogin4()));
        rowSyaratLogin4.setVisible(digunakanSyaratLogin.isChecked());

        final MyFormRow rowBayarHanyaSmtSaatIniDanSebelumnyalagi5 = new MyFormRow();
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi5.setParent(rows);
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi5.appendChild(new ais.ui.util.MyLabelConfig(""));
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi5.appendChild(
                bayarHanyaSmtSaatIniDanSebelumnyalagi5 = new MyCheckboxConfig(
                        "Persyaratan ini juga hanya khusus untuk semester saat ini -5 saat mahasiswa login"));
        bayarHanyaSmtSaatIniDanSebelumnyalagi5.setChecked(jenisKegiatan.getBayarHanyaSmtSaatIniDanSebelumnyalagi5());
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi5.setVisible(digunakanSyaratLogin.isChecked());

        final MyFormRow rowSyaratLogin5 = new MyFormRow();
        rowSyaratLogin5.setParent(rows);
        rowSyaratLogin5.appendChild(new ais.ui.util.MyLabelConfig(
                "Persyaratan ini juga hanya khusus untuk semester saat ini -5, berapa persen harus membayar ?"));
        rowSyaratLogin5.appendChild(persenSyaratLogin5 = new MyDoublebox(jenisKegiatan.getPersenSyaratLogin5()));
        rowSyaratLogin5.setVisible(digunakanSyaratLogin.isChecked());

        final MyFormRow rowBayarHanyaSmtSaatIniDanSebelumnyalagi6 = new MyFormRow();
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi6.setParent(rows);
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi6.appendChild(new ais.ui.util.MyLabelConfig(""));
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi6.appendChild(
                bayarHanyaSmtSaatIniDanSebelumnyalagi6 = new MyCheckboxConfig(
                        "Persyaratan ini juga hanya khusus untuk semester saat ini -6 saat mahasiswa login"));
        bayarHanyaSmtSaatIniDanSebelumnyalagi6.setChecked(jenisKegiatan.getBayarHanyaSmtSaatIniDanSebelumnyalagi6());
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi6.setVisible(digunakanSyaratLogin.isChecked());

        final MyFormRow rowSyaratLogin6 = new MyFormRow();
        rowSyaratLogin6.setParent(rows);
        rowSyaratLogin6.appendChild(new ais.ui.util.MyLabelConfig(
                "Persyaratan ini juga hanya khusus untuk semester saat ini -6, berapa persen harus membayar ?"));
        rowSyaratLogin6.appendChild(persenSyaratLogin6 = new MyDoublebox(jenisKegiatan.getPersenSyaratLogin6()));
        rowSyaratLogin6.setVisible(digunakanSyaratLogin.isChecked());

        final MyFormRow rowBayarHanyaSmtSaatIniDanSebelumnyalagi7 = new MyFormRow();
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi7.setParent(rows);
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi7.appendChild(new ais.ui.util.MyLabelConfig(""));
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi7.appendChild(
                bayarHanyaSmtSaatIniDanSebelumnyalagi7 = new MyCheckboxConfig(
                        "Persyaratan ini juga hanya khusus untuk semester saat ini -7 saat mahasiswa login"));
        bayarHanyaSmtSaatIniDanSebelumnyalagi7.setChecked(jenisKegiatan.getBayarHanyaSmtSaatIniDanSebelumnyalagi7());
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi7.setVisible(digunakanSyaratLogin.isChecked());

        final MyFormRow rowSyaratLogin7 = new MyFormRow();
        rowSyaratLogin7.setParent(rows);
        rowSyaratLogin7.appendChild(new ais.ui.util.MyLabelConfig(
                "Persyaratan ini juga hanya khusus untuk semester saat ini -7, berapa persen harus membayar ?"));
        rowSyaratLogin7.appendChild(persenSyaratLogin7 = new MyDoublebox(jenisKegiatan.getPersenSyaratLogin7()));
        rowSyaratLogin7.setVisible(digunakanSyaratLogin.isChecked());

        final MyFormRow rowBayarHanyaSmtSaatIniDanSebelumnyalagi8 = new MyFormRow();
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi8.setParent(rows);
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi8.appendChild(new ais.ui.util.MyLabelConfig(""));
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi8.appendChild(
                bayarHanyaSmtSaatIniDanSebelumnyalagi8 = new MyCheckboxConfig(
                        "Persyaratan ini juga hanya khusus untuk semester saat ini -8 saat mahasiswa login"));
        bayarHanyaSmtSaatIniDanSebelumnyalagi8.setChecked(jenisKegiatan.getBayarHanyaSmtSaatIniDanSebelumnyalagi8());
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi8.setVisible(digunakanSyaratLogin.isChecked());

        final MyFormRow rowSyaratLogin8 = new MyFormRow();
        rowSyaratLogin8.setParent(rows);
        rowSyaratLogin8.appendChild(new ais.ui.util.MyLabelConfig(
                "Persyaratan ini juga hanya khusus untuk semester saat ini -8, berapa persen harus membayar ?"));
        rowSyaratLogin8.appendChild(persenSyaratLogin8 = new MyDoublebox(jenisKegiatan.getPersenSyaratLogin8()));
        rowSyaratLogin8.setVisible(digunakanSyaratLogin.isChecked());

        final MyFormRow rowBayarHanyaSmtSaatIniDanSebelumnyalagi9 = new MyFormRow();
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi9.setParent(rows);
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi9.appendChild(new ais.ui.util.MyLabelConfig(""));
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi9.appendChild(
                bayarHanyaSmtSaatIniDanSebelumnyalagi9 = new MyCheckboxConfig(
                        "Persyaratan ini juga hanya khusus untuk semester saat ini -9 saat mahasiswa login"));
        bayarHanyaSmtSaatIniDanSebelumnyalagi9.setChecked(jenisKegiatan.getBayarHanyaSmtSaatIniDanSebelumnyalagi9());
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi9.setVisible(digunakanSyaratLogin.isChecked());

        final MyFormRow rowSyaratLogin9 = new MyFormRow();
        rowSyaratLogin9.setParent(rows);
        rowSyaratLogin9.appendChild(new ais.ui.util.MyLabelConfig(
                "Persyaratan ini juga hanya khusus untuk semester saat ini -9, berapa persen harus membayar ?"));
        rowSyaratLogin9.appendChild(persenSyaratLogin9 = new MyDoublebox(jenisKegiatan.getPersenSyaratLogin9()));
        rowSyaratLogin9.setVisible(digunakanSyaratLogin.isChecked());

        final MyFormRow rowBayarHanyaSmtSaatIniDanSebelumnyalagi10 = new MyFormRow();
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi10.setParent(rows);
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi10.appendChild(new ais.ui.util.MyLabelConfig(""));
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi10.appendChild(
                bayarHanyaSmtSaatIniDanSebelumnyalagi10 = new MyCheckboxConfig(
                        "Persyaratan ini juga hanya khusus untuk semester saat ini -10 saat mahasiswa login"));
        bayarHanyaSmtSaatIniDanSebelumnyalagi10.setChecked(jenisKegiatan.getBayarHanyaSmtSaatIniDanSebelumnyalagi10());
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi10.setVisible(digunakanSyaratLogin.isChecked());

        final MyFormRow rowSyaratLogin10 = new MyFormRow();
        rowSyaratLogin10.setParent(rows);
        rowSyaratLogin10.appendChild(new ais.ui.util.MyLabelConfig(
                "Persyaratan ini juga hanya khusus untuk semester saat ini -10, berapa persen harus membayar ?"));
        rowSyaratLogin10.appendChild(persenSyaratLogin10 = new MyDoublebox(jenisKegiatan.getPersenSyaratLogin10()));
        rowSyaratLogin10.setVisible(digunakanSyaratLogin.isChecked());

        final MyFormRow rowBayarHanyaSmtSaatIniDanSebelumnyalagi11 = new MyFormRow();
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi11.setParent(rows);
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi11.appendChild(new ais.ui.util.MyLabelConfig(""));
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi11.appendChild(
                bayarHanyaSmtSaatIniDanSebelumnyalagi11 = new MyCheckboxConfig(
                        "Persyaratan ini juga hanya khusus untuk semester saat ini -11 saat mahasiswa login"));
        bayarHanyaSmtSaatIniDanSebelumnyalagi11.setChecked(jenisKegiatan.getBayarHanyaSmtSaatIniDanSebelumnyalagi11());
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi11.setVisible(digunakanSyaratLogin.isChecked());

        final MyFormRow rowSyaratLogin11 = new MyFormRow();
        rowSyaratLogin11.setParent(rows);
        rowSyaratLogin11.appendChild(new ais.ui.util.MyLabelConfig(
                "Persyaratan ini juga hanya khusus untuk semester saat ini -11, berapa persen harus membayar ?"));
        rowSyaratLogin11.appendChild(persenSyaratLogin11 = new MyDoublebox(jenisKegiatan.getPersenSyaratLogin11()));
        rowSyaratLogin11.setVisible(digunakanSyaratLogin.isChecked());

        final MyFormRow rowBayarHanyaSmtSaatIniDanSebelumnyalagi12 = new MyFormRow();
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi12.setParent(rows);
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi12.appendChild(new ais.ui.util.MyLabelConfig(""));
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi12.appendChild(
                bayarHanyaSmtSaatIniDanSebelumnyalagi12 = new MyCheckboxConfig(
                        "Persyaratan ini juga hanya khusus untuk semester saat ini -12 saat mahasiswa login"));
        bayarHanyaSmtSaatIniDanSebelumnyalagi12.setChecked(jenisKegiatan.getBayarHanyaSmtSaatIniDanSebelumnyalagi12());
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi12.setVisible(digunakanSyaratLogin.isChecked());

        final MyFormRow rowSyaratLogin12 = new MyFormRow();
        rowSyaratLogin12.setParent(rows);
        rowSyaratLogin12.appendChild(new ais.ui.util.MyLabelConfig(
                "Persyaratan ini juga hanya khusus untuk semester saat ini -12, berapa persen harus membayar ?"));
        rowSyaratLogin12.appendChild(persenSyaratLogin12 = new MyDoublebox(jenisKegiatan.getPersenSyaratLogin12()));
        rowSyaratLogin12.setVisible(digunakanSyaratLogin.isChecked());

        final MyFormRow rowBayarHanyaSmtSaatIniDanSebelumnyalagi13 = new MyFormRow();
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi13.setParent(rows);
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi13.appendChild(new ais.ui.util.MyLabelConfig(""));
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi13.appendChild(
                bayarHanyaSmtSaatIniDanSebelumnyalagi13 = new MyCheckboxConfig(
                        "Persyaratan ini juga hanya khusus untuk semester saat ini -13 saat mahasiswa login"));
        bayarHanyaSmtSaatIniDanSebelumnyalagi13.setChecked(jenisKegiatan.getBayarHanyaSmtSaatIniDanSebelumnyalagi13());
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi13.setVisible(digunakanSyaratLogin.isChecked());

        final MyFormRow rowSyaratLogin13 = new MyFormRow();
        rowSyaratLogin13.setParent(rows);
        rowSyaratLogin13.appendChild(new ais.ui.util.MyLabelConfig(
                "Persyaratan ini juga hanya khusus untuk semester saat ini -13, berapa persen harus membayar ?"));
        rowSyaratLogin13.appendChild(persenSyaratLogin13 = new MyDoublebox(jenisKegiatan.getPersenSyaratLogin13()));
        rowSyaratLogin13.setVisible(digunakanSyaratLogin.isChecked());

        final MyFormRow rowBayarHanyaSmtSaatIniDanSebelumnyalagi14 = new MyFormRow();
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi14.setParent(rows);
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi14.appendChild(new ais.ui.util.MyLabelConfig(""));
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi14.appendChild(
                bayarHanyaSmtSaatIniDanSebelumnyalagi14 = new MyCheckboxConfig(
                        "Persyaratan ini juga hanya khusus untuk semester saat ini -14 saat mahasiswa login"));
        bayarHanyaSmtSaatIniDanSebelumnyalagi14.setChecked(jenisKegiatan.getBayarHanyaSmtSaatIniDanSebelumnyalagi14());
        rowBayarHanyaSmtSaatIniDanSebelumnyalagi14.setVisible(digunakanSyaratLogin.isChecked());

        final MyFormRow rowSyaratLogin14 = new MyFormRow();
        rowSyaratLogin14.setParent(rows);
        rowSyaratLogin14.appendChild(new ais.ui.util.MyLabelConfig(
                "Persyaratan ini juga hanya khusus untuk semester saat ini -14, berapa persen harus membayar ?"));
        rowSyaratLogin14.appendChild(persenSyaratLogin14 = new MyDoublebox(jenisKegiatan.getPersenSyaratLogin14()));
        rowSyaratLogin14.setVisible(digunakanSyaratLogin.isChecked());

        digunakanSyaratLogin.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                boolean checked = digunakanSyaratLogin.isChecked();
                rowSyaratLogin.setVisible(checked);
                rowBayarHanyaSmtSaatIni.setVisible(checked);
                rowBayarHanyaSmtSaatIniDanSebelumnya.setVisible(checked);
                rowSyaratLogin1.setVisible(checked);
                rowBayarHanyaSmtSaatIniDanSebelumnyalagi.setVisible(checked);
                rowSyaratLogin2.setVisible(checked);
                rowBayarHanyaSmtSaatIniDanSebelumnyalagi3.setVisible(checked);
                rowSyaratLogin3.setVisible(checked);
                rowBayarHanyaSmtSaatIniDanSebelumnyalagi4.setVisible(checked);
                rowSyaratLogin4.setVisible(checked);
                rowBayarHanyaSmtSaatIniDanSebelumnyalagi5.setVisible(checked);
                rowSyaratLogin5.setVisible(checked);
                rowBayarHanyaSmtSaatIniDanSebelumnyalagi6.setVisible(checked);
                rowSyaratLogin6.setVisible(checked);
                rowBayarHanyaSmtSaatIniDanSebelumnyalagi7.setVisible(checked);
                rowSyaratLogin7.setVisible(checked);
                rowBayarHanyaSmtSaatIniDanSebelumnyalagi8.setVisible(checked);
                rowSyaratLogin8.setVisible(checked);
                rowBayarHanyaSmtSaatIniDanSebelumnyalagi9.setVisible(checked);
                rowSyaratLogin9.setVisible(checked);
                rowBayarHanyaSmtSaatIniDanSebelumnyalagi10.setVisible(checked);
                rowSyaratLogin10.setVisible(checked);
                rowBayarHanyaSmtSaatIniDanSebelumnyalagi11.setVisible(checked);
                rowSyaratLogin11.setVisible(checked);
                rowBayarHanyaSmtSaatIniDanSebelumnyalagi12.setVisible(checked);
                rowSyaratLogin12.setVisible(checked);
                rowBayarHanyaSmtSaatIniDanSebelumnyalagi13.setVisible(checked);
                rowSyaratLogin13.setVisible(checked);
                rowBayarHanyaSmtSaatIniDanSebelumnyalagi14.setVisible(checked);
                rowSyaratLogin14.setVisible(checked);
            }
        });

        dendaJikaTerlambat = new MyCheckboxConfig("Dikenakan denda jika terlambat membayar");
        dendaJikaTerlambat.setChecked(jenisKegiatan.getDendaJikaTerlambat());
        fb.addRow("", dendaJikaTerlambat);

        dendaDibuatPerProdi = new MyCheckboxConfig("Denda dibuat per prodi");
        dendaDibuatPerProdi.setChecked(jenisKegiatan.getDendaDibuatPerProdi());
        fb.addRow("", dendaDibuatPerProdi);

        nilaiDendaDalamPersen = new MyCheckboxConfig("Denda dalam persen (jika tidak dipilih dalam nilai fix)");
        nilaiDendaDalamPersen.setChecked(jenisKegiatan.getNilaiDendaDalamPersen());
        fb.addRow("", nilaiDendaDalamPersen);

        dendaAkanBerlipatTerlambaHari = new MyIntbox(jenisKegiatan.getDendaAkanBerlipatTerlambaHari());
        fb.addRow("Denda akan berlipat jika terlambat dalam hari", dendaAkanBerlipatTerlambaHari);

        maksimalBerlipatTerlambaHari = new MyIntbox(jenisKegiatan.getMaksimalBerlipatTerlambaHari());
        fb.addRow("Maksimal jumlah kelipatan", maksimalBerlipatTerlambaHari);

        final MyFormRow rowDenda = new MyFormRow();
        rowDenda.setStyle("border:0px;background: transparent;");
        rowDenda.setParent(rows);
        rowDenda.appendChild(new ais.ui.util.MyLabelConfig("Nilai Denda"));
        rowDenda.appendChild(defaultProsentaseDenda = new MyDoublebox(jenisKegiatan.getDefaultProsentaseDenda()));

        final MyFormRow rowDendaPerProdi = new MyFormRow();
        rowDendaPerProdi.setValign("top");
        rowDendaPerProdi.setStyle("border:0px;background: transparent;");
        rowDendaPerProdi.setParent(rows);
        rowDendaPerProdi.appendChild(new ais.ui.util.MyLabelConfig("Nilai Denda"));
        dendaPerProdi = new JSONObject(jenisKegiatan.getDendaPerProdi());
        Session session = HibernateUtil.currentSession();
        List<Jurusan> jurusans = ConstantValues.simpleList(
                session.createCriteria(Jurusan.class).createAlias("fakultas", "fakultas")
                        .add(Restrictions.eq("fakultas.perguruanTinggi",
                                PerguruanTinggiUtil.getPerguruanTinggi()))
                        .addOrder(Order.asc("fakultas.nama")).addOrder(Order.asc("nama"))
                        .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
                Jurusan.class);

        MyGrid vboxSkala = new MyGrid();
        vboxSkala.setParent(rowDendaPerProdi);

        Columns columnsSkala = new Columns();
        columnsSkala.setParent(vboxSkala);

        MyColumnConfig columnSkala = new MyColumnConfig("Jurusan");
        columnSkala.setParent(columnsSkala);
        columnSkala.setWidth("75%");

        columnSkala = new MyColumnConfig("Denda");
        columnSkala.setParent(columnsSkala);

        Rows rowsSkala = new Rows();
        rowsSkala.setParent(vboxSkala);

        for (final Jurusan jurusan : jurusans) {
            MyFormRow rowSkala = new MyFormRow();
            rowSkala.setStyle("border:0px;background: transparent;");
            rowSkala.setParent(rowsSkala);
            rowSkala.appendChild(new Label(jurusan.getNama()));
            Double dendaP = dendaPerProdi.isNull(jurusan.getId().toString()) ? null
                    : dendaPerProdi.getDouble(jurusan.getId().toString());
            final MyDoublebox defaultTagihan = new MyDoublebox(dendaP);
            defaultTagihan.setWidth("95%");
            rowSkala.appendChild(defaultTagihan);
            defaultTagihan.addEventListener("onChange", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    dendaPerProdi.put(jurusan.getId().toString(), defaultTagihan.getValue());
                }
            });
        }

        EventListener eventListenerDenda = new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                dendaDibuatPerProdi.getParent().setVisible(dendaJikaTerlambat.isChecked());
                nilaiDendaDalamPersen.getParent().setVisible(dendaJikaTerlambat.isChecked());
                dendaAkanBerlipatTerlambaHari.getParent().setVisible(dendaJikaTerlambat.isChecked());
                maksimalBerlipatTerlambaHari.getParent().setVisible(dendaJikaTerlambat.isChecked());
                rowDendaPerProdi.setVisible(dendaDibuatPerProdi.isChecked() && dendaJikaTerlambat.isChecked());
                rowDenda.setVisible(!dendaDibuatPerProdi.isChecked() && dendaJikaTerlambat.isChecked());
            }
        };
        dendaJikaTerlambat.addEventListener("onClick", eventListenerDenda);
        dendaDibuatPerProdi.addEventListener("onClick", eventListenerDenda);
        try {
            eventListenerDenda.onEvent(null);
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }

        aktif = new MyCheckboxConfig("Jenis pembayaran ini aktif");
        aktif.setChecked(jenisKegiatan.getAktif());
        fb.addRow("", aktif);

        tidakBolehMengangsur = new MyCheckboxConfig("Tidak boleh meng-angsur atau klik proses bayar di item biaya");
        tidakBolehMengangsur.setChecked(jenisKegiatan.getTidakBolehMengangsur());
        fb.addRow("", tidakBolehMengangsur);

        tagihan_juga_untuk_alumni = new MyCheckboxConfig("Tagihan juga untuk alumni");
        tagihan_juga_untuk_alumni.setChecked(jenisKegiatan.getTagihanJugaUntukAlumni());
        fb.addRow("", tagihan_juga_untuk_alumni);

        digunakanSyaratCetakSuratBebasAktif = new MyCheckboxConfig(
                "Tagihan juga digunakan sebagai syarat cetak surat bebas aktif");
        digunakanSyaratCetakSuratBebasAktif.setChecked(jenisKegiatan.getDigunakanSyaratCetakSuratBebasAktif());
        fb.addRow("", digunakanSyaratCetakSuratBebasAktif);

        prefixKodePembayaran = new Textbox(jenisKegiatan.getPrefixKodePembayaran());
        prefixKodePembayaran.setWidth("100%");
        fb.addRow("Prefix Kode Pembayaran", prefixKodePembayaran);

        namaBankPembayaran = new Textbox(jenisKegiatan.getNamaBankPembayaran());
        namaBankPembayaran.setWidth("100%");
        namaBankPembayaran.setRows(2);
        fb.addRow("Bank Pembayaran", namaBankPembayaran,
                "Jika Bank Pembayaran mengulang lebih dari satu, pisahkan dengan tanda semikolon (;), "
                        + "contoh : ;A;B;C; dan seterusnya.");

        kanalPembayaran = new org.zkoss.zul.Combobox();
        kanalPembayaran.setWidth("100%");
        kanalPembayaran.setReadonly(true);
        Common.insertComboDanSemua(kanalPembayaran, new String[] { "nama" }, "keterangan",
                KanalPembayaran.class, "Ikuti Kanal Pembayaran Default", Restrictions.eq("aktif", true));
        Common.selectComboItem(kanalPembayaran, jenisKegiatan.getKanalPembayaran());
        fb.addRow("Kanal Pembayaran", kanalPembayaran);

        penjelasanPembayaran = new MyCkEditor();
        penjelasanPembayaran.setWidth("95%");
        penjelasanPembayaran.setHeight("380px");
        penjelasanPembayaran.setValue(jenisKegiatan.getPenjelasanPembayaran());
        fb.addFullRow(penjelasanPembayaran);

        South south = new South();
        ZkCompat.setFlex(south, true);
        south.setStyle(FormBuilder.STYLE_TOOLBAR_AREA);
        south.setParent(borderlayout);

        Toolbar toolbar = new Toolbar();
        toolbar.setStyle("padding:6px 12px;");
        toolbar.setParent(south);

        MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
        cancel.setTooltiptext("Tutup");
        cancel.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                addWindow.setVisible(false);
            }
        });
        cancel.setParent(toolbar);

        MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
        save.setTooltiptext("Simpan");
        save.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                if (onSave(event)) {
                    onSearchDefault(null);
                    addWindow.setVisible(false);
                }
            }
        });
        save.setParent(toolbar);

        borderlayout.setParent(window);
    }

    // ======================== Save logic ========================

    /**
     * Merakit peta "Berlaku di smt" ({@code JSONObject} kunci = id jenjang, nilai = daftar
     * semester dipisah koma) dari deretan checkbox jenjang beserta {@link Textbox}
     * pendampingnya (atribut {@code "smtbox"} pada checkbox). Field ini melengkapi aturan
     * angsuran per-jenjang menjadi per-jenjang <b>per-semester</b>: kosong berarti aturan
     * berlaku di semua semester (kompatibel dengan data lama), sedangkan isian seperti
     * {@code "1,2,3,5,6"} berarti aturan hanya berlaku pada semester tersebut (contoh
     * nyata: S2 wajib angsuran di smt 1-3, namun smt 4 ditagih sekaligus).
     * <p>
     * Isian disanitasi ringan: hanya token angka yang dipertahankan, spasi dibuang,
     * token tidak valid dilewati dengan pencatatan ke ErrorLog (bukan menggagalkan
     * simpan). Hanya jenjang TERCENTANG dengan isian tidak kosong yang masuk peta —
     * peta kosong tidak ikut disimpan ke JSON supaya struktur data lama tetap ramping.
     * Konsumen peta ini: {@code JenisKegiatan.semesterCocokUntukJenjang} yang dipakai
     * {@code modeAngsuranUntukJenjang(Jenjang, Integer)}.
     *
     * @param daftarCheckbox deretan checkbox jenjang grup "harus"/"bukan" angsuran
     * @return peta id-jenjang → daftar semester ternormalisasi (bisa kosong, tidak null)
     */
    private JSONObject petaSmtDariCheckbox(List<Checkbox> daftarCheckbox) {
        JSONObject peta = new JSONObject();
        if (daftarCheckbox == null)
            return peta;
        for (Checkbox cb : daftarCheckbox) {
            try {
                if (!cb.isChecked())
                    continue;
                String jid = (String) cb.getAttribute("jkode");
                Object box = cb.getAttribute("smtbox");
                if (jid == null || !(box instanceof Textbox))
                    continue;
                String isian = ((Textbox) box).getValue();
                if (isian == null || isian.trim().isEmpty())
                    continue;
                // Token yang sah: "N" (semester global), "TAHUN:N" (mulai cakupan angkatan),
                // atau "TAHUN:" (angkatan tsb semua semester). Token rusak dilewati + ErrorLog.
                StringBuilder bersih = new StringBuilder();
                for (String token : isian.split(",")) {
                    String t = token.trim();
                    if (t.isEmpty())
                        continue;
                    try {
                        int posTitikDua = t.indexOf(':');
                        String normal;
                        if (posTitikDua >= 0) {
                            int tahun = Integer.parseInt(t.substring(0, posTitikDua).trim());
                            String smtStr = t.substring(posTitikDua + 1).trim();
                            normal = tahun + ":" + (smtStr.isEmpty() ? "" : String.valueOf(Integer.parseInt(smtStr)));
                        } else {
                            normal = String.valueOf(Integer.parseInt(t));
                        }
                        if (bersih.length() > 0)
                            bersih.append(",");
                        bersih.append(normal);
                    } catch (Exception exNum) {
                        ais.common.ErrorAuditUtil.record(exNum,
                                "JenisKegiatanAction: isian 'Berlaku di smt' tidak valid '" + t
                                        + "' untuk jenjang id=" + jid + " (token dilewati)");
                    }
                }
                if (bersih.length() > 0)
                    peta.put(jid, bersih.toString());
            } catch (Exception e) {
                ais.common.ErrorAuditUtil.record(e, "JenisKegiatanAction: gagal merakit peta smt jenjang");
            }
        }
        return peta;
    }

    public boolean onSave(Event event) throws Exception {
        if (kode.getValue().trim().equals("")) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Kode",
            		"Kolom Kode belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
            		new String[] {
            				"Isi/pilih terlebih dahulu Kode.",
            				"Ulangi proses penyimpanan setelah kolom tersebut terisi."
            		});
            return false;
        }
        if (namaKegiatan.getValue().trim().equals("")) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Nama",
            		"Kolom Nama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
            		new String[] {
            				"Isi/pilih terlebih dahulu Nama.",
            				"Ulangi proses penyimpanan setelah kolom tersebut terisi."
            		});
            return false;
        }
        if (checkNamaKegiatan()) {
            PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Pembayaran",
            		"Nama Jenis Pembayaran sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
            		new String[] {
            				"Gunakan nama jenis pembayaran yang berbeda dari data yang sudah ada.",
            				"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
            		});
            return false;
        }
        JenisKegiatanDao jenisKegiatanDao = DaoFactory.getInstance().getJenisKegiatanDao();
        JenisKegiatan entity = currentEntity;
        if (entity.getId() != null) {
            entity = jenisKegiatanDao.load(entity.getId());
            currentEntity = entity;
        }
        entity.setPrefixKodePembayaran(prefixKodePembayaran.getValue().trim());
        entity.setHanyaBerupaBukanAngsuran(hanyaBerupaBukanAngsuran.isChecked());
        entity.setUntukBayarSP(untukBayarSP.isChecked());
        entity.setNamaKegiatan(namaKegiatan.getValue());
        entity.setDefaultKegiatan(false);
        entity.setAbaikanNilaiMinus(abaikanNilaiMinus.isChecked());
        entity.setKode(kode.getValue());
        entity.setAktif(aktif.isChecked());
        entity.setKeterangan(keterangan.getValue());
        entity.setDigunakanUntukPengecekanKrs(digunakanUntukPengecekanKrs.isChecked());
        entity.setDigunakanUntukPengecekanUjian(digunakanUntukPengecekanUjian.isChecked());
        entity.setDigunakanSyaratKeaktifan(digunakanSyaratKeaktifan.isChecked());
        entity.setDigunakanSyaratLogin(digunakanSyaratLogin.isChecked());
        entity.setPersenSyaratLogin(persenSyaratLogin.getValue());
        entity.setPersenSyaratLogin1(persenSyaratLogin1.getValue());
        entity.setPersenSyaratLogin2(persenSyaratLogin2.getValue());
        entity.setPersenSyaratLogin3(persenSyaratLogin3.getValue());
        entity.setPersenSyaratLogin4(persenSyaratLogin4.getValue());
        entity.setPersenSyaratLogin5(persenSyaratLogin5.getValue());
        entity.setPersenSyaratLogin6(persenSyaratLogin6.getValue());
        entity.setPersenSyaratLogin7(persenSyaratLogin7.getValue());
        entity.setPersenSyaratLogin8(persenSyaratLogin8.getValue());
        entity.setPersenSyaratLogin9(persenSyaratLogin9.getValue());
        entity.setPersenSyaratLogin10(persenSyaratLogin10.getValue());
        entity.setPersenSyaratLogin11(persenSyaratLogin11.getValue());
        entity.setPersenSyaratLogin12(persenSyaratLogin12.getValue());
        entity.setPersenSyaratLogin13(persenSyaratLogin13.getValue());
        entity.setPersenSyaratLogin14(persenSyaratLogin14.getValue());
        entity.setHanyaBerupaAngsuran(hanyaBerupaAngsuran.isChecked());
        {
            JSONObject jenjangJson = new JSONObject();
            // [] berarti berlaku semua jenjang; simpan [] jika semua tercentang
            JSONArray harusArr = new JSONArray();
            if (jenjangHarusCheckboxes != null && !jenjangHarusCheckboxes.isEmpty()) {
                boolean allChecked = true;
                for (Checkbox cb : jenjangHarusCheckboxes) {
                    if (!cb.isChecked()) { allChecked = false; break; }
                }
                if (!allChecked) {
                    for (Checkbox cb : jenjangHarusCheckboxes) {
                        String kode = (String) cb.getAttribute("jkode");
                        if (cb.isChecked() && kode != null) harusArr.put(kode);
                    }
                }
            }
            JSONArray bukanArr = new JSONArray();
            if (jenjangBukanCheckboxes != null && !jenjangBukanCheckboxes.isEmpty()) {
                boolean allChecked = true;
                for (Checkbox cb : jenjangBukanCheckboxes) {
                    if (!cb.isChecked()) { allChecked = false; break; }
                }
                if (!allChecked) {
                    for (Checkbox cb : jenjangBukanCheckboxes) {
                        String kode = (String) cb.getAttribute("jkode");
                        if (cb.isChecked() && kode != null) bukanArr.put(kode);
                    }
                }
            }
            // Peta "Berlaku di smt" per jenjang (kunci = id jenjang, nilai = "1,2,3,5,6").
            // Disimpan untuk SEMUA jenjang tercentang — termasuk saat array jenjang kosong
            // (kosong = semua jenjang) — karena pembatasan semester tetap harus per jenjang.
            JSONObject harusSmt = petaSmtDariCheckbox(jenjangHarusCheckboxes);
            JSONObject bukanSmt = petaSmtDariCheckbox(jenjangBukanCheckboxes);
            try {
                jenjangJson.put("harus", harusArr);
                jenjangJson.put("bukan", bukanArr);
                if (harusSmt.length() > 0) jenjangJson.put("harus_smt", harusSmt);
                if (bukanSmt.length() > 0) jenjangJson.put("bukan_smt", bukanSmt);
            } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/JenisKegiatanAction.java:896");}
            entity.setJenjangAngsuranJson(jenjangJson.toString());
        }
        entity.setMinSmt(minSmt.getValue());
        entity.setMaxSmt(maxSmt.getValue());
        entity.setBayarHanyaSmtSaatIni(bayarHanyaSmtSaatIni.isChecked());
        entity.setBayarHanyaSmtSaatIniDanSebelumnya(bayarHanyaSmtSaatIniDanSebelumnya.isChecked());
        entity.setBayarHanyaSmtSaatIniDanSebelumnyalagi(bayarHanyaSmtSaatIniDanSebelumnyalagi.isChecked());
        entity.setBayarHanyaSmtSaatIniDanSebelumnyalagi3(bayarHanyaSmtSaatIniDanSebelumnyalagi3.isChecked());
        entity.setBayarHanyaSmtSaatIniDanSebelumnyalagi4(bayarHanyaSmtSaatIniDanSebelumnyalagi4.isChecked());
        entity.setBayarHanyaSmtSaatIniDanSebelumnyalagi5(bayarHanyaSmtSaatIniDanSebelumnyalagi5.isChecked());
        entity.setBayarHanyaSmtSaatIniDanSebelumnyalagi6(bayarHanyaSmtSaatIniDanSebelumnyalagi6.isChecked());
        entity.setBayarHanyaSmtSaatIniDanSebelumnyalagi7(bayarHanyaSmtSaatIniDanSebelumnyalagi7.isChecked());
        entity.setBayarHanyaSmtSaatIniDanSebelumnyalagi8(bayarHanyaSmtSaatIniDanSebelumnyalagi8.isChecked());
        entity.setBayarHanyaSmtSaatIniDanSebelumnyalagi9(bayarHanyaSmtSaatIniDanSebelumnyalagi9.isChecked());
        entity.setBayarHanyaSmtSaatIniDanSebelumnyalagi10(bayarHanyaSmtSaatIniDanSebelumnyalagi10.isChecked());
        entity.setBayarHanyaSmtSaatIniDanSebelumnyalagi11(bayarHanyaSmtSaatIniDanSebelumnyalagi11.isChecked());
        entity.setBayarHanyaSmtSaatIniDanSebelumnyalagi12(bayarHanyaSmtSaatIniDanSebelumnyalagi12.isChecked());
        entity.setBayarHanyaSmtSaatIniDanSebelumnyalagi13(bayarHanyaSmtSaatIniDanSebelumnyalagi13.isChecked());
        entity.setBayarHanyaSmtSaatIniDanSebelumnyalagi14(bayarHanyaSmtSaatIniDanSebelumnyalagi14.isChecked());
        entity.setDigunakanUntukPengecekanNilai(digunakanUntukPengecekanNilai.isChecked());
        entity.setTidakBolehMengangsur(tidakBolehMengangsur.isChecked());
        entity.setTagihanJugaUntukAlumni(tagihan_juga_untuk_alumni.isChecked());
        entity.setDigunakanSyaratCetakSuratBebasAktif(digunakanSyaratCetakSuratBebasAktif.isChecked());
        entity.setPenjelasanPembayaran(penjelasanPembayaran.getValue());
        entity.setDendaJikaTerlambat(dendaJikaTerlambat.isChecked());
        entity.setDefaultProsentaseDenda(defaultProsentaseDenda.getValue());
        entity.setNilaiDendaDalamPersen(nilaiDendaDalamPersen.isChecked());
        entity.setDendaAkanBerlipatTerlambaHari(dendaAkanBerlipatTerlambaHari.getValue());
        entity.setMaksimalBerlipatTerlambaHari(maksimalBerlipatTerlambaHari.getValue());
        entity.setKanalPembayaran((KanalPembayaran) (kanalPembayaran.getSelectedItem() == null
                ? null : kanalPembayaran.getSelectedItem().getValue()));
        entity.setDendaDibuatPerProdi(dendaDibuatPerProdi.isChecked());
        entity.setDendaPerProdi(dendaPerProdi.toString());
        entity.setNamaBankPembayaran(namaBankPembayaran.getValue());

        if (entity.getId() != null) {
            jenisKegiatanDao.update(entity);
        } else {
            jenisKegiatanDao.save(entity);
        }

        Common.createDefaultTimer(new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                Common.reloadJenisKegiatans();
            }
        });
        return true;
    }

    public Boolean checkNamaKegiatan() {
        Session session = HibernateUtil.currentSession();
        int count = ((Number) session.createCriteria(JenisKegiatan.class)
                .setProjection(Projections.rowCount())
                .add(Restrictions.eq("namaKegiatan", namaKegiatan.getValue().trim()).ignoreCase())
                .add(currentEntity.getId() == null
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.ne("id", currentEntity.getId()))
                .uniqueResult()).intValue();
        return count != 0;
    }

    // ======================== Renderer ========================

    class JenisKegiatanRenderer extends MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final JenisKegiatan jk = (JenisKegiatan) arg1;

            RevisiHelper.createNewRevisi(JenisKegiatan.class, jk, jk.getKode()).setParent(arg0);

            org.zkoss.zul.Vbox vbox = new org.zkoss.zul.Vbox();
            vbox.setParent(arg0);
            new Label(jk.getNamaKegiatan()).setParent(vbox);
            new MyLabelAgakKecil(jk.getKanalPembayaran() == null ? ""
                    : jk.getKanalPembayaran().getNama()).setParent(vbox);
            if (jk.getPrefixKodePembayaran() != null) {
                vbox.appendChild(new MyLabelAgakKecil(jk.getPrefixKodePembayaran()));
            }
            if (!jk.getNamaBankPembayaran().isEmpty()) {
                vbox.appendChild(new MyLabelAgakKecil(jk.getNamaBankPembayaran()));
            }

            new Label(jk.getDigunakanUntukPengecekanKrs() ? "Ya" : "Tidak").setParent(arg0);
            new Label(jk.getDigunakanUntukPengecekanUjian() ? "Ya" : "Tidak").setParent(arg0);
            new Label(jk.getDigunakanUntukPengecekanNilai() ? "Ya" : "Tidak").setParent(arg0);
            new Label(jk.getDigunakanSyaratKeaktifan() ? "Ya" : "Tidak").setParent(arg0);

            String persen = Common.numberFormat.get().format(jk.getPersenSyaratLogin());
            if (jk.getBayarHanyaSmtSaatIniDanSebelumnya())
                persen += ", " + Common.numberFormat.get().format(jk.getPersenSyaratLogin1());
            if (jk.getBayarHanyaSmtSaatIniDanSebelumnyalagi())
                persen += ", " + Common.numberFormat.get().format(jk.getPersenSyaratLogin2());
            if (jk.getBayarHanyaSmtSaatIniDanSebelumnyalagi3())
                persen += ", " + Common.numberFormat.get().format(jk.getPersenSyaratLogin3());
            if (jk.getBayarHanyaSmtSaatIniDanSebelumnyalagi4())
                persen += ", " + Common.numberFormat.get().format(jk.getPersenSyaratLogin4());
            if (jk.getBayarHanyaSmtSaatIniDanSebelumnyalagi5())
                persen += ", " + Common.numberFormat.get().format(jk.getPersenSyaratLogin5());
            if (jk.getBayarHanyaSmtSaatIniDanSebelumnyalagi6())
                persen += ", " + Common.numberFormat.get().format(jk.getPersenSyaratLogin6());
            if (jk.getBayarHanyaSmtSaatIniDanSebelumnyalagi7())
                persen += ", " + Common.numberFormat.get().format(jk.getPersenSyaratLogin7());
            if (jk.getBayarHanyaSmtSaatIniDanSebelumnyalagi8())
                persen += ", " + Common.numberFormat.get().format(jk.getPersenSyaratLogin8());
            if (jk.getBayarHanyaSmtSaatIniDanSebelumnyalagi9())
                persen += ", " + Common.numberFormat.get().format(jk.getPersenSyaratLogin9());
            if (jk.getBayarHanyaSmtSaatIniDanSebelumnyalagi10())
                persen += ", " + Common.numberFormat.get().format(jk.getPersenSyaratLogin10());
            if (jk.getBayarHanyaSmtSaatIniDanSebelumnyalagi11())
                persen += ", " + Common.numberFormat.get().format(jk.getPersenSyaratLogin11());
            if (jk.getBayarHanyaSmtSaatIniDanSebelumnyalagi12())
                persen += ", " + Common.numberFormat.get().format(jk.getPersenSyaratLogin12());
            if (jk.getBayarHanyaSmtSaatIniDanSebelumnyalagi13())
                persen += ", " + Common.numberFormat.get().format(jk.getPersenSyaratLogin13());
            if (jk.getBayarHanyaSmtSaatIniDanSebelumnyalagi14())
                persen += ", " + Common.numberFormat.get().format(jk.getPersenSyaratLogin14());

            new Label(jk.getDigunakanSyaratLogin() ? "Ya (" + persen + "%)" : "Tidak").setParent(arg0);
            new Label(jk.getKeterangan()).setParent(arg0);
            new Label(jk.getMinSmt() + "").setParent(arg0);
            new Label(jk.getMaxSmt() + "").setParent(arg0);

            final MyCheckboxConfig checkboxLain = new MyCheckboxConfig("Pemb. Lain");
            checkboxLain.setChecked(!jk.getDefaultKegiatan());
            checkboxLain.setParent(arg0);
            checkboxLain.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    jk.setDefaultKegiatan(!checkboxLain.isChecked());
                    Common.refreshSaveOrUpdate(jk);
                }
            });

            boolean nggakBolehHapus = false;
            if (jk.getId() != null) {
                if ((ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
                        && jk.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId()))
                        || (ConstantValues.PENDAFTARAN_MAHASISWA_LAMA != null
                                && jk.getId().equals(ConstantValues.PENDAFTARAN_MAHASISWA_LAMA.getId()))
                        || (ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
                                && jk.getId().equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId()))) {
                    nggakBolehHapus = true;
                }
            }

            final MyCheckboxConfig defaultPembayaran = new MyCheckboxConfig("Default Pemb.");
            defaultPembayaran.setDisabled(!edit);
            defaultPembayaran.setChecked(jk.getDefaultPembayaran());
            defaultPembayaran.setParent(arg0);
            defaultPembayaran.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    jk.setDefaultPembayaran(defaultPembayaran.isChecked());
                    Common.refreshSaveOrUpdate(jk);
                    if (defaultPembayaran.isChecked()) {
                        HibernateUtil.currentSession()
                                .createSQLQuery("update jenis_kegiatan set default_pembayaran=false where id != "
                                        + jk.getId())
                                .executeUpdate();
                        ConstantValues.PENDAFTARAN_MAHASISWA_LAMA = jk;
                    }
                    onSearchDefault(arg0);
                }
            });

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit || nggakBolehHapus);
            checkbox.setChecked(jk.getAktif());
            checkbox.setParent(arg0);
            arg0.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    jk.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(jk);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, jk, JenisKegiatanAction.this).setParent(arg0);
        }
    }
}
