package ais.action.master.koperasi;

import java.util.Date;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.inventory.Produk;
import ais.database.model.inventory.Toko;
import ais.database.model.koperasi.AturanDiskon;
import ais.database.model.koperasi.JenisAnggotaKoperasi;
import ais.database.model.koperasi.TipeAnggotaKoperasi;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * Pendataan Aturan Diskon / Promo Produk — konversi dari {@code modul/kantin/diskon}.
 *
 * <p>Memakai {@link GenericCrudAction} (boilerplate CRUD, Help, Download/Upload, filter
 * lanjut, spanduk pengantar ditangani base class).</p>
 *
 * <p><b>Pembatasan toko:</b> bila pengguna login sebagai pedagang yang tidak boleh melihat
 * toko lain, daftar &amp; pilihan produk dikunci hanya untuk tokonya (tidak bisa melihat /
 * membuat aturan untuk toko/pedagang lain).</p>
 */
public class AturanDiskonAction extends GenericCrudAction<AturanDiskon> {

    private static final long serialVersionUID = 1L;

    // Form fields
    private org.zkoss.zul.Textbox namaAturan;
    private org.zkoss.zul.Textbox keterangan;
    private Combobox produk;
    private Combobox toko;
    private Combobox jenisAnggota;
    private Combobox tipeAnggota;
    private Checkbox chkSemuaMember;
    private Checkbox chkPotonganLangsung;
    private Checkbox chkBatas1x;
    private MyDoublebox persentase;
    private MyDoublebox maksimalPotongan;
    private MyDoublebox nominal;
    private Datebox tglMulai;
    private Datebox tglSelesai;

    // Cache lingkup toko (untuk pedagang)
    private Toko scopeTokoCache;
    private boolean scopeResolved = false;

    private Toko scopeToko() {
        if (!scopeResolved) {
            scopeResolved = true;
            try {
                Toko ct = Common.getCurrentToko();
                if (ct != null && ct.getId() != null) {
                    Boolean b = ct.getBolehMelihatTokolain();
                    if (b == null || !b.booleanValue()) {
                        scopeTokoCache = ct;
                    }
                }
            } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/koperasi/AturanDiskonAction.java:86");
            }
        }
        return scopeTokoCache;
    }

    // ======================== Abstract implementations ========================

    @Override
    protected Class<AturanDiskon> getEntityClass() {
        return AturanDiskon.class;
    }

    @Override
    protected AturanDiskon createNewEntity() {
        return new AturanDiskon();
    }

    @Override
    protected String getWindowTitle() {
        return "Pendataan Aturan Diskon";
    }

    @Override
    protected String getIntroTitle() {
        return "Aturan Diskon & Promo";
    }

    @Override
    protected String getIntroDescription() {
        return "Atur potongan harga atau cashback untuk produk tertentu, lengkap dengan sasaran member, "
                + "besaran diskon, dan masa berlakunya. Diskon yang aktif akan otomatis dipakai saat penjualan.";
    }

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(AturanDiskon.class)
                .add(searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"));
        if (order) {
            criteria.addOrder(Order.desc("id"));
        }
        if (!searchnama.getValue().trim().isEmpty()) {
            criteria.add(Restrictions.ilike("namaAturan", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        }
        // Pembatasan toko untuk pedagang.
        Toko st = scopeToko();
        if (st != null) {
            criteria.add(Restrictions.eq("toko", st));
        }
        return criteria;
    }

    @Override
    protected MyRowRenderer createRenderer() {
        return new AturanDiskonRenderer();
    }

    // ======================== Form UI ========================

    @Override
    protected void buildFormContent(MyWindow window, final AturanDiskon ad) throws Exception {
        Toko st = scopeToko();

        org.zkoss.zul.Borderlayout borderlayout = new MyBorderlayout();

        org.zkoss.zul.Center center = new org.zkoss.zul.Center();
        center.setStyle("overflow:auto;padding:12px;background:#f0f4f8;");
        ZkCompat.setFlex(center, true);
        center.setParent(borderlayout);

        Div cardWrap = new Div();
        cardWrap.setStyle(FormBuilder.STYLE_CARD_WRAP);
        cardWrap.setParent(center);

        Grid formGrid = new Grid();
        formGrid.setStyle("border:none;width:100%;");
        formGrid.setParent(cardWrap);

        Rows rows = new Rows();
        rows.setParent(formGrid);

        FormBuilder fb = new FormBuilder(rows);

        namaAturan = new org.zkoss.zul.Textbox(ad.getNamaAturan());
        namaAturan.setWidth("100%");
        fb.addRow("Nama Aturan / Promo *", namaAturan, "Contoh: Promo Akhir Tahun Indomie");

        produk = new Combobox();
        produk.setWidth("100%");
        produk.setReadonly(true);
        if (st != null) {
            Common.insertCombo(produk, "nama", Produk.class, Restrictions.eq("aktif", true),
                    Restrictions.eq("toko", st));
        } else {
            Common.insertCombo(produk, "nama", Produk.class, Restrictions.eq("aktif", true));
        }
        Common.selectComboItem(produk, ad.getProduk());
        fb.addRow("Produk *", produk, "Barang yang mendapat diskon");

        toko = new Combobox();
        toko.setWidth("100%");
        toko.setReadonly(true);
        Common.insertComboDanSemua(toko, "nama", Toko.class, Restrictions.eq("aktif", true));
        if (st != null) {
            Common.selectComboItem(true, toko, st);
            toko.setDisabled(true);
        } else {
            Common.selectComboItem(toko, ad.getToko());
        }
        fb.addRow("Berlaku di Toko", toko, "Kosongkan (Semua) berarti berlaku di seluruh toko");

        fb.addSectionHeader("Sasaran Member");

        chkSemuaMember = new Checkbox();
        chkSemuaMember.setChecked(Boolean.TRUE.equals(ad.getBerlakuSemuaMember()));
        fb.addRow("Berlaku Semua Member", chkSemuaMember, "Centang = semua orang dapat diskon ini");

        jenisAnggota = new Combobox();
        jenisAnggota.setWidth("100%");
        jenisAnggota.setReadonly(true);
        Common.insertComboDanSemua(jenisAnggota, "nama", JenisAnggotaKoperasi.class);
        Common.selectComboItem(jenisAnggota, ad.getJenisAnggota());
        fb.addRow("Khusus Jenis Anggota", jenisAnggota, "Opsional — biarkan Semua bila tidak spesifik");

        tipeAnggota = new Combobox();
        tipeAnggota.setWidth("100%");
        tipeAnggota.setReadonly(true);
        Common.insertComboDanSemua(tipeAnggota, "nama", TipeAnggotaKoperasi.class);
        Common.selectComboItem(tipeAnggota, ad.getTipeAnggota());
        fb.addRow("Khusus Tipe Anggota", tipeAnggota, "Opsional — biarkan Semua bila tidak spesifik");

        fb.addSectionHeader("Besaran Diskon");

        persentase = new MyDoublebox(ad.getPersentase());
        fb.addRow("Diskon Persen (%)", persentase, "Contoh: 10 untuk potongan 10%");

        maksimalPotongan = new MyDoublebox(ad.getMaksimalPotongan());
        fb.addRow("Maksimal Potongan (Rp)", maksimalPotongan, "Batas rupiah bila memakai persen (0 = tanpa batas)");

        nominal = new MyDoublebox(ad.getNominal());
        fb.addRow("Diskon Nominal (Rp)", nominal, "Potongan tetap dalam rupiah (alternatif dari persen)");

        chkPotonganLangsung = new Checkbox();
        chkPotonganLangsung.setChecked(Boolean.TRUE.equals(ad.getPotonganLangsung()));
        fb.addRow("Potong Langsung di Struk", chkPotonganLangsung,
                "Tidak dicentang = diskon masuk ke saldo cashback member");

        chkBatas1x = new Checkbox();
        chkBatas1x.setChecked(Boolean.TRUE.equals(ad.getBerlakuPerHariDanPerToko()));
        fb.addRow("Batasi 1x per Hari per Toko", chkBatas1x, "Cocok untuk subsidi harian agar tidak diborong");

        fb.addSectionHeader("Masa Berlaku");

        tglMulai = new Datebox(ad.getTanggalMulai());
        tglMulai.setFormat("dd-MM-yyyy");
        tglMulai.setWidth("100%");
        fb.addRow("Mulai Berlaku", tglMulai, "Kosongkan = langsung berlaku");

        tglSelesai = new Datebox(ad.getTanggalSelesai());
        tglSelesai.setFormat("dd-MM-yyyy");
        tglSelesai.setWidth("100%");
        fb.addRow("Selesai Berlaku", tglSelesai, "Kosongkan = tanpa batas waktu");

        keterangan = new org.zkoss.zul.Textbox(ad.getKeterangan());
        keterangan.setWidth("100%");
        keterangan.setRows(3);
        fb.addRow("Keterangan", keterangan, "Catatan tambahan bila diperlukan");

        org.zkoss.zul.South south = new org.zkoss.zul.South();
        ZkCompat.setFlex(south, true);
        south.setStyle(FormBuilder.STYLE_TOOLBAR_AREA);
        south.setParent(borderlayout);

        Toolbar toolbar = new Toolbar();
        toolbar.setStyle("padding:6px 12px;");
        toolbar.setParent(south);

        MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
        cancel.setTooltiptext("Tutup tanpa menyimpan");
        cancel.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                addWindow.setVisible(false);
            }
        });
        cancel.setParent(toolbar);

        MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
        save.setTooltiptext("Simpan perubahan");
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

    public boolean onSave(Event event) throws Exception {
        if (namaAturan.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, nama aturan/promo belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Aturan/Promo; (2) pastikan nama tidak kosong; (3) ulangi penyimpanan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (produk.getSelectedItem() == null || produk.getSelectedItem().getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, produk yang akan didiskon belum dipilih. Langkah yang dapat dilakukan: (1) pilih produk dari daftar yang tersedia; (2) ulangi penyimpanan.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }

        Session session = HibernateUtil.currentSession();
        AturanDiskon ad = currentEntity;
        if (ad.getId() != null) {
            ad = (AturanDiskon) session.load(AturanDiskon.class, ad.getId());
            currentEntity = ad;
        }

        ad.setNamaAturan(namaAturan.getValue());
        ad.setKeterangan(keterangan.getValue());
        ad.setProduk((Produk) produk.getSelectedItem().getValue());

        Toko st = scopeToko();
        if (st != null) {
            ad.setToko(st); // pedagang: kunci ke tokonya sendiri
        } else {
            ad.setToko((Toko) (toko.getSelectedItem() == null ? null : toko.getSelectedItem().getValue()));
        }

        ad.setBerlakuSemuaMember(chkSemuaMember.isChecked());
        ad.setJenisAnggota((JenisAnggotaKoperasi) (jenisAnggota.getSelectedItem() == null ? null
                : jenisAnggota.getSelectedItem().getValue()));
        ad.setTipeAnggota((TipeAnggotaKoperasi) (tipeAnggota.getSelectedItem() == null ? null
                : tipeAnggota.getSelectedItem().getValue()));
        ad.setPersentase(persentase.getValue());
        ad.setMaksimalPotongan(maksimalPotongan.getValue());
        ad.setNominal(nominal.getValue());
        ad.setPotonganLangsung(chkPotonganLangsung.isChecked());
        ad.setBerlakuPerHariDanPerToko(chkBatas1x.isChecked());
        ad.setTanggalMulai(tglMulai.getValue());
        ad.setTanggalSelesai(tglSelesai.getValue());
        if (ad.getId() == null) {
            ad.setAktif(true);
        }
        Common.refreshSaveOrUpdate(session, ad);
        return true;
    }

    // ======================== Renderer ========================

    class AturanDiskonRenderer extends ais.ui.util.MyRowRenderer {

        @Override
        public void render(final Row arg0, Object arg1) throws Exception {
            arg0.setValign("top");
            final AturanDiskon ad = (AturanDiskon) arg1;

            RevisiHelper.createNewRevisi(AturanDiskon.class, ad, ad.getNamaAturan()).setParent(arg0);
            new Label(ad.getProduk() == null ? "" : ad.getProduk().getNama()).setParent(arg0);
            new Label(ad.getToko() == null ? "Semua Toko" : ad.getToko().getNama()).setParent(arg0);
            new Label(deskripsiDiskon(ad)).setParent(arg0);
            new Label(deskripsiPeriode(ad)).setParent(arg0);

            final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
            checkbox.setDisabled(!edit);
            checkbox.setChecked(!Boolean.FALSE.equals(ad.getAktif()));
            checkbox.setParent(arg0);
            arg0.setAttribute("checkbox", checkbox);
            checkbox.addEventListener("onCheck", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    ad.setAktif(checkbox.isChecked());
                    Common.refreshSaveOrUpdate(ad);
                }
            });

            Common.copyEditDeleteButtons(edit, delete, ad, AturanDiskonAction.this).setParent(arg0);
        }
    }

    private static String deskripsiDiskon(AturanDiskon ad) {
        StringBuilder sb = new StringBuilder();
        double persen = ad.getPersentase() == null ? 0 : ad.getPersentase();
        double nom = ad.getNominal() == null ? 0 : ad.getNominal();
        if (persen > 0) {
            sb.append(Common.numberFormat.get().format(persen)).append("%");
            double maks = ad.getMaksimalPotongan() == null ? 0 : ad.getMaksimalPotongan();
            if (maks > 0) {
                sb.append(" (maks Rp ").append(Common.numberFormat.get().format(maks)).append(")");
            }
        } else if (nom > 0) {
            sb.append("Rp ").append(Common.numberFormat.get().format(nom));
        } else {
            sb.append("-");
        }
        sb.append(ad.getPotonganLangsung() ? " · potong harga" : " · cashback");
        return sb.toString();
    }

    private static String deskripsiPeriode(AturanDiskon ad) {
        Date m = ad.getTanggalMulai();
        Date s = ad.getTanggalSelesai();
        if (m == null && s == null) {
            return "Selalu";
        }
        java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("dd MMM yyyy");
        String a = m == null ? "…" : f.format(m);
        String b = s == null ? "…" : f.format(s);
        return a + " s/d " + b;
    }

    // ======================== Help content ========================

    @Override
    protected String getHelpContent() {
        return "<div style='font-family:sans-serif;font-size:13px;line-height:1.8;color:#1e293b;'>"
                + "<h3 style='margin-top:0;color:#1d4ed8;border-bottom:2px solid #dbeafe;padding-bottom:6px;'>"
                + "&#127991;&#65039; Panduan &mdash; Aturan Diskon</h3>"
                + "<p>Modul ini mengatur <b>potongan harga / cashback</b> untuk produk tertentu.</p>"
                + "<ul style='padding-left:18px;'>"
                + "<li><b>Diskon Persen</b> atau <b>Diskon Nominal</b> — isi salah satu. Jika pakai persen, "
                + "<b>Maksimal Potongan</b> membatasi rupiahnya.</li>"
                + "<li><b>Potong Langsung di Struk</b> dicentang = harga langsung berkurang; jika tidak, diskon "
                + "masuk sebagai <b>saldo cashback</b> member yang bisa dicairkan.</li>"
                + "<li><b>Sasaran Member</b> — centang Semua Member, atau batasi ke Jenis/Tipe anggota tertentu.</li>"
                + "<li><b>Masa Berlaku</b> — kosongkan agar berlaku terus tanpa batas.</li>"
                + "<li><b>Batasi 1x per Hari per Toko</b> — cocok untuk subsidi harian agar adil.</li>"
                + "</ul>"
                + "<div style='background:#fff7ed;border:1px solid #fed7aa;border-radius:6px;padding:10px 14px;margin-top:8px;'>"
                + "&#9888;&#65039; Bila Anda login sebagai pedagang, aturan otomatis dikunci untuk toko Anda sendiri.</div>"
                + "</div>";
    }
}
