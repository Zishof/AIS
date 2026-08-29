package ais.action.master.obe;

import ais.action.master.helper.AmbilDataMatakuliahBanbox;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.CommonSearchFilterHelper;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.Matakuliah;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.UIUtil;
import ais.ui.util.ZkCompat;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.metainfo.ComponentInfo;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import java.util.List;
import java.util.Map;

/**
 * Abstract base untuk semua Action OBE. Mengandung boilerplate yang identik
 * di BahanKajian, CapaianLulusan, CapaianPembelajaranLulusan, ProfilLulusan,
 * ProfesiLulusan, dan ReferensiLulusan — sehingga tiap subclass hanya
 * berisi logika yang benar-benar unik.
 */
@SuppressWarnings({"deprecation", "unchecked"})
public abstract class ObeBaseAction extends GenericAutowireComposer
        implements DataCriteria, DataSearchDefault, DataInitDefault {

    private static final long serialVersionUID = 1L;

    // Semua field di bawah di-autowire oleh GenericAutowireComposer dari ZUL (id cocok nama field).
    protected MyWindow    addWindow;
    protected Paging      paging;
    protected MyGrid      grid;
    protected Textbox     searchnama;
    protected Textbox     searchkode;
    protected Combobox    searchfakultas;
    protected Combobox    searchjurusan;
    protected Checkbox    searchaktif;
    protected MyToolbarbuttonConfig add;

    protected PerguruanTinggi perguruanTinggi;
    protected boolean edit;
    protected boolean delete;

    // ── Security ─────────────────────────────────────────────────────────────

    @Override
    public ComponentInfo doBeforeCompose(Page page, Component parent, ComponentInfo compInfo) {
        Common.doCheckSecurity();
        return super.doBeforeCompose(page, parent, compInfo);
    }

    // ── Init umum (dipanggil dari doAfterCompose di tiap subclass) ───────────

    /**
     * Harus dipanggil di awal doAfterCompose subclass, setelah
     * super.doAfterCompose(comp).
     *
     * @param entityClass    kelas entity untuk cetak/upload
     * @param exportContents kolom yang diekspor
     */
    protected void initCommon(Component comp, Class<?> entityClass, String[] exportContents)
            throws Exception {
        super.doAfterCompose(comp);
        Common.initLaguage();
        ObePageHelpHelper.pasangHalamanCrud(comp, getClass());
        perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
        if (searchfakultas != null) {
            Common.initFakultasDanJurusan(null, null, searchfakultas, searchjurusan);
        }
        if (add != null) {
        add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
        add.setTooltiptext("Tambah");
        }
        edit   = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
        delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
        onSearchDefault(null);
        Common.initPaging(paging, new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                onSearchDefault(null);
            }
        });

        MyToolbarbuttonConfig cetak = Common.cetakData(entityClass, this, exportContents);
        if (add != null) {
        add.getParent().appendChild(cetak);
        }
        MyToolbarbuttonConfig upload = Common.uploadData(this, entityClass, exportContents);
        upload.setVisible((add != null && add.isVisible()) && edit && delete);
        if (add != null) {
        add.getParent().appendChild(upload);
        }
    }

    // ── Form-builder helpers ──────────────────────────────────────────────────

    /** Kontainer sederhana untuk hasil buildFormBorderlayout(). */
    protected static final class FormContext {
        public final org.zkoss.zul.Borderlayout borderlayout;
        public final Rows rows;

        FormContext(org.zkoss.zul.Borderlayout bl, Rows r) {
            this.borderlayout = bl;
            this.rows         = r;
        }
    }

    /**
     * Membersihkan addWindow, membuat struktur Borderlayout > Center > Grid
     * dengan dua kolom (30% label / sisanya input), dan mengembalikan
     * FormContext berisi borderlayout dan rows untuk diisi field-field form.
     */
    protected FormContext buildFormBorderlayout(String title) {
        addWindow.setTitle(title);
        Common.clear(addWindow);

        org.zkoss.zul.Borderlayout bl = new MyBorderlayout();
        Center center = new Center();
        center.setParent(bl);
        ZkCompat.setFlex(center, true);

        MyGrid formGrid = new MyGrid();
        formGrid.setWidth("100%");
        // Tidak set height:100% agar konten panjang (mis. banyak Sub-CPMK) tidak
        // terpotong — Center Borderlayout sudah overflow:auto dan akan menggulir.
        formGrid.setParent(center);

        Columns cols = new Columns();
        cols.setParent(formGrid);
        MyColumnConfig c1 = new MyColumnConfig();
        c1.setWidth("30%");
        c1.setParent(cols);
        new MyColumnConfig().setParent(cols);

        Rows rows = new Rows();
        rows.setParent(formGrid);
        return new FormContext(bl, rows);
    }

    /** Menambah satu baris form berlabel ke rows. Mengembalikan Row yang dibuat. */
    protected Row addFormRow(Rows rows, String label, Component field) {
        MyFormRow row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new MyLabelConfig(label));
        row.appendChild(field);
        return row;
    }

    /** Menambah baris Kode, Nama, Fakultas, Prodi yang sama di semua form OBE. */
    protected void addKodeNamaFakultasJurusanRows(Rows rows, Textbox kode, Textbox nama,
            Combobox fakultas, Combobox jurusan, Jurusan entityJurusan,
            String labelKode, String labelNama) {
        Tbmuser user = Common.getCurrentUser();

        addFormRow(rows, labelKode, kode);
        kode.setWidth("90%");

        addFormRow(rows, labelNama, nama);
        nama.setWidth("90%");
        // Nama CPL/entity OBE sering panjang (satu-dua kalimat) → jadikan area teks
        // multiline 5 baris agar seluruh teks terlihat & mudah diedit (bukan 1 baris sempit).
        nama.setMultiline(true);
        nama.setRows(5);

        Common.selectComboItem(fakultas,
                entityJurusan == null ? user.ambilFakultas() : entityJurusan.getFakultas());
        addFormRow(rows, "Fakultas *", fakultas);
        fakultas.setWidth("90%");

        if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
            Common.insertCombo(jurusan, new String[]{"nama", "kodeEpsbed"}, "jenjang", Jurusan.class,
                    Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
                    CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
        }
        Common.pilihJurusan(jurusan, entityJurusan == null ? user.ambilJurusan() : entityJurusan);
        addFormRow(rows, "Prodi *", jurusan);
        jurusan.setWidth("90%");
    }

    /**
     * Menambah baris "Khusus buat matakuliah" ke form. Backward-compat: contextMk = mk.
     */
    protected AmbilDataMatakuliahBanbox addKhususMkRow(Rows rows, Matakuliah mk) {
        return addKhususMkRow(rows, mk, mk);
    }

    /**
     * Menambah baris "Khusus buat matakuliah" ke form dan mengembalikan banbox-nya.
     *
     * <p>{@code initialMk} = nilai awal banbox (null = kosong untuk form tambah baru).
     * {@code contextMk} = MK yang otomatis diisi ketika checkbox "Khusus MK Ini" dicentang.
     *
     * <ul>
     *   <li>UNCHECKED (default saat initialMk==null) → banbox kosong + disabled (berlaku umum).</li>
     *   <li>CHECKED → banbox diisi contextMk + enabled.</li>
     * </ul>
     */
    protected AmbilDataMatakuliahBanbox addKhususMkRow(Rows rows, Matakuliah initialMk, final Matakuliah contextMk) {
        boolean adaMkKhusus;
        try {
            adaMkKhusus = (initialMk != null && initialMk.getId() != null);
        } catch (Exception e) {
            adaMkKhusus = false;
        }

        final AmbilDataMatakuliahBanbox banbox = new AmbilDataMatakuliahBanbox();
        banbox.setReadonly(true);
        banbox.setAttribute("matakuliah", adaMkKhusus ? initialMk : null);
        banbox.setValue(adaMkKhusus ? initialMk.getNama() : "");
        banbox.setWidth("90%");
        banbox.setDisabled(!adaMkKhusus);

        // Checkbox "Khusus MK Ini": CHECKED = terikat ke MK tertentu, banbox aktif+terisi.
        final Checkbox khususMkChk = new Checkbox("Khusus MK Ini");
        khususMkChk.setStyle("margin-left:6px;white-space:nowrap;");
        khususMkChk.setChecked(adaMkKhusus);
        banbox.setAttribute("semuaMkCheckbox", khususMkChk);
        khususMkChk.addEventListener("onCheck", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                if (khususMkChk.isChecked()) {
                    boolean hasCtx = false;
                    try { hasCtx = contextMk != null && contextMk.getId() != null; } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) ObeBaseAction addKhususMkRow");}
                    if (hasCtx) {
                        banbox.setValue(contextMk.getNama());
                        banbox.setAttribute("matakuliah", contextMk);
                    }
                    banbox.setDisabled(false);
                } else {
                    banbox.setValue("");
                    banbox.removeAttribute("matakuliah");
                    banbox.setDisabled(true);
                }
            }
        });

        org.zkoss.zul.Hbox hb = new org.zkoss.zul.Hbox();
        hb.setWidth("100%");
        hb.setAlign("center");
        hb.appendChild(banbox);
        hb.appendChild(khususMkChk);
        addFormRow(rows, "Khusus buat matakuliah", hb);
        return banbox;
    }

    /** @deprecated Diganti addKhususMkRow dua-param; tetap ada untuk backward compat BahanKajian/CapaianLulusan. */
    protected static void terapkanSemuaMatakuliah(AmbilDataMatakuliahBanbox banbox, boolean semua) {
        if (banbox == null) return;
        if (semua) { banbox.setValue(""); banbox.removeAttribute("matakuliah"); }
        banbox.setDisabled(semua);
    }

    /** Menambah baris Keterangan (multiline textarea). */
    protected Textbox addKeteranganRow(Rows rows, String value) {
        Textbox tb = new Textbox(value);
        tb.setWidth("90%");
        tb.setRows(3);
        addFormRow(rows, "Keterangan", tb);
        return tb;
    }

    /**
     * Membuat toolbar Simpan & Batal di bagian South borderlayout.
     * saveListener menerima event onClick tombol Simpan.
     */
    protected void buildSouthToolbar(org.zkoss.zul.Borderlayout bl,
            final EventListener saveListener) {
        South south = new South();
        ZkCompat.setFlex(south, true);
        south.setParent(bl);

        Toolbar toolbar = new Toolbar();
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
        save.addEventListener("onClick", saveListener);
        save.setParent(toolbar);
    }

    /** Memasang borderlayout ke addWindow lalu menampilkan window secara modal. */
    protected void attachAndShow(org.zkoss.zul.Borderlayout bl) {
        bl.setParent(addWindow);
        addWindow.setVisible(true);
        try {
            addWindow.onModal();
        } catch (Exception e) {
            ais.common.Common.tampilErrorJikaAdmin(e);
        }
    }

    // ── Multi-select checkbox grid ────────────────────────────────────────────

    /**
     * Merender ulang grid checkbox multi-pilih di dalam sebuah Row container.
     * items      : daftar pilihan
     * selectedMap: map ID → entity yang sedang dipilih; diperbarui saat user klik
     * columnHeader: label kolom grid
     * labelFn    : fungsi yang menghasilkan teks label tiap checkbox dari entity
     */
    protected <T extends GeneralValueObject> void renderCheckboxGrid(
            Row container, List<T> items, final Map<Long, T> selectedMap,
            String columnHeader, CheckboxLabelFn<T> labelFn) {
        Common.clear(container);

        // Pembungkus vertikal: kotak pencarian/filter di ATAS daftar checkbox.
        org.zkoss.zul.Vbox wrap = new org.zkoss.zul.Vbox();
        wrap.setWidth("100%");
        wrap.setParent(container);

        // Kotak pencarian (live filter). Tidak memakai setPlaceholder (tak didukung
        // di ZK 5.5 build ini) -> pakai tooltiptext.
        final Textbox cari = new Textbox();
        cari.setWidth("99%");
        cari.setTooltiptext("Ketik untuk menyaring daftar di bawah berdasarkan teksnya");
        cari.setParent(wrap);

        MyGrid checkGrid = new MyGrid();
        checkGrid.setParent(wrap);
        Columns cols = new Columns();
        cols.setParent(checkGrid);
        new MyColumnConfig(columnHeader).setParent(cols);
        Rows rows = new Rows();
        rows.setParent(checkGrid);

        // Simpan baris + teks (lowercase) untuk penyaringan cepat tanpa query ulang.
        final java.util.List<Row> barisList = new java.util.ArrayList<Row>();
        final java.util.List<String> teksList = new java.util.ArrayList<String>();

        for (final T item : items) {
            MyFormRow r = new MyFormRow();
            r.setStyle("border:0px;background:transparent;");
            r.setParent(rows);
            final Checkbox cb = new Checkbox(labelFn.label(item));
            cb.setChecked(selectedMap.containsKey(item.getId()));
            cb.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    if (cb.isChecked()) selectedMap.put(item.getId(), item);
                    else              selectedMap.remove(item.getId());
                }
            });
            cb.setParent(r);
            barisList.add(r);
            String lbl = labelFn.label(item);
            teksList.add(lbl == null ? "" : lbl.toLowerCase());
        }

        // Filter langsung saat mengetik (onChanging): baris yang tidak cocok disembunyikan.
        // Status centang TIDAK berubah (tetap tersimpan di selectedMap), sehingga pilihan
        // yang sedang ter-filter-keluar tidak hilang saat disimpan.
        cari.addEventListener("onChanging", new EventListener() {
            @Override
            public void onEvent(Event ev) throws Exception {
                String q = (ev instanceof org.zkoss.zk.ui.event.InputEvent)
                        ? ((org.zkoss.zk.ui.event.InputEvent) ev).getValue()
                        : cari.getValue();
                q = q == null ? "" : q.trim().toLowerCase();
                for (int i = 0; i < barisList.size(); i++) {
                    barisList.get(i).setVisible(q.length() == 0 || teksList.get(i).indexOf(q) >= 0);
                }
            }
        });
    }

    /** Functional interface sederhana untuk menghindari ketergantungan java.util.function. */
    public interface CheckboxLabelFn<T> {
        String label(T item);
    }

    /** Mengonversi kunci Map<Long,?> menjadi String comma-separated. */
    protected static String mapKeysToString(Map<Long, ?> map) {
        if (map == null || map.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Long key : map.keySet()) {
            if (sb.length() > 0) sb.append(',');
            sb.append(key);
        }
        return sb.toString();
    }

    // ── Criteria helpers ─────────────────────────────────────────────────────

    protected boolean isUnselected(Combobox cb) {
        return cb == null || cb.getSelectedItem() == null || cb.getSelectedItem().getValue() == null;
    }

    /**
     * Membangun Criteria dasar untuk pola filter OBE:
     * perguruanTinggi, aktif, nama (ilike), dan opsional fakultas+jurusan.
     *
     * @param hasFakultasJurusan true jika entity punya relasi ke Jurusan (createAlias dibutuhkan)
     * @param orderFields        field yang diurutkan ascending jika order=true; boleh kosong
     */
    protected Criteria buildBaseCriteria(Session session, Class<?> entityClass,
            boolean order, boolean hasFakultasJurusan, String... orderFields) {
        Criteria c = session.createCriteria(entityClass);

        if (hasFakultasJurusan) {
            c.createAlias("jurusan", "jurusan");
            c.add(isUnselected(searchfakultas) ? Restrictions.sqlRestriction("1=1")
                    : CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false));
            c.add(isUnselected(searchjurusan) ? Restrictions.sqlRestriction("1=1")
                    : CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false));
        }

        c.add(Restrictions.eq("perguruanTinggi", perguruanTinggi));
        c.add(searchaktif == null || searchaktif.isChecked()
                ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                : Restrictions.sqlRestriction("true"));

        if (order && orderFields.length > 0) {
            for (String f : orderFields) c.addOrder(Order.asc(f));
        }

        if (searchnama != null && !searchnama.getValue().trim().isEmpty()) {
            c.add(Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
        }
        if (searchkode != null && !searchkode.getValue().trim().isEmpty()) {
            c.add(Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
        }
        return c;
    }

    /** Menjalankan paging + query + render grid. */
    protected void executeSearch(Criteria countCriteria, Criteria dataCriteria, RowRenderer renderer) {
        Common.initPaging(countCriteria, paging);
        List<?> list = dataCriteria
                .setMaxResults(Common.ROWS_COUNT_ON_PAGE)
                .setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
                .list();
        grid.setRowRenderer(renderer);
        grid.setModelCheckMobile(new SimpleListModel(list));
    }

    // ── Validasi umum ─────────────────────────────────────────────────────────

    protected boolean validateNamaRequired(Textbox tb, String entityLabel) throws Exception {
        if (tb.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Nama " + entityLabel + " harus diisi", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        return true;
    }

    protected boolean validateJurusanRequired(Combobox jurusan) throws Exception {
        if (isUnselected(jurusan)) {
            MyMessageboxConfig.show(Common.getBahasaConfig("Jurusan") + " harus diisi", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        return true;
    }

    /** Mengunci jurusan & fakultas jika sudah terpilih (untuk mode external/popup). */
    protected void lockFakultasJurusanIfSelected(Combobox fakultas, Combobox jurusan) {
        if (!isUnselected(jurusan))   jurusan.setDisabled(true);
        if (!isUnselected(fakultas)) fakultas.setDisabled(true);
    }

    // ── Ringkasan teks panjang (Keterangan / daftar) dgn tautan "(baca)" ───────

    /** Panjang ringkas default (karakter) sebelum dipotong + tautan "(baca)". */
    private static final int PANJANG_RINGKAS = 90;

    /**
     * Menampilkan teks panjang (mis. Keterangan) secara ringkas: hanya beberapa
     * kata / 1 kalimat, sisanya disembunyikan di balik tautan "(baca)". Saat
     * di-klik, teks penuh ditampilkan (toggle jadi "(tutup)"). Mengembalikan satu
     * komponen sel (Vbox) sehingga bisa langsung di-setParent(row).
     */
    public static Component ringkasanKeterangan(String teksPenuh) {
        return ringkasanKeterangan(teksPenuh, PANJANG_RINGKAS);
    }

    /**
     * Sel kolom "Nama" pada grid OBE: tetap memakai tautan revisi
     * ({@link ais.action.master.helper.RevisiHelper#createNewRevisi}) sebagai label nama,
     * TAPI bila nama terlalu panjang label dipotong &amp; ditambah tautan "(baca)"/"(tutup)"
     * untuk membuka/menutup teks penuh secara inline — sehingga baris grid tidak menjadi
     * sangat tinggi saat nama panjang. Aman: bila nama pendek, perilakunya sama seperti dulu.
     *
     * @param kelas  kelas entity (untuk riwayat revisi)
     * @param entity entity baris (boleh belum tersimpan)
     * @param nama   teks nama penuh
     */
    public static Component namaCellRingkas(Class<?> kelas,
            ais.database.model.GeneralValueObject entity, String nama) {
        final String teks = nama == null ? "" : nama.trim();
        final String ringkas = potongRingkas(teks, PANJANG_RINGKAS);
        final boolean perluPotong = ringkas.length() < teks.length();

        org.zkoss.zul.Vbox box = ais.action.master.helper.RevisiHelper.createNewRevisi(
                kelas, entity, perluPotong ? ringkas + "…" : teks);

        if (perluPotong) {
            // Anak pertama Vbox dari createNewRevisi adalah A (tautan revisi) atau Label biasa.
            final Component labelKomp = box.getFirstChild();
            final org.zkoss.zul.Toolbarbutton baca = new org.zkoss.zul.Toolbarbutton("(baca)");
            baca.setStyle("color:#2563eb;font-weight:bold;cursor:pointer;");
            box.appendChild(baca);
            final boolean[] terbuka = new boolean[] { false };
            baca.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    terbuka[0] = !terbuka[0];
                    String v = terbuka[0] ? teks : ringkas + "…";
                    if (labelKomp instanceof org.zkoss.zul.A) {
                        ((org.zkoss.zul.A) labelKomp).setLabel(v);
                    } else if (labelKomp instanceof org.zkoss.zul.Label) {
                        ((org.zkoss.zul.Label) labelKomp).setValue(v);
                    }
                    baca.setLabel(terbuka[0] ? "(tutup)" : "(baca)");
                }
            });
        }
        return box;
    }

    public static Component ringkasanKeterangan(final String teksPenuh, int maksKarakter) {
        org.zkoss.zul.Vbox box = new org.zkoss.zul.Vbox();
        box.setWidth("100%");
        final String teks = teksPenuh == null ? "" : teksPenuh.trim();
        if (teks.isEmpty()) {
            return box;
        }
        final String ringkas = potongRingkas(teks, maksKarakter);
        boolean perluPotong = ringkas.length() < teks.length();
        final org.zkoss.zul.Label label = new org.zkoss.zul.Label(perluPotong ? ringkas + "… " : teks);
        label.setMultiline(true);
        label.setParent(box);
        if (perluPotong) {
            final org.zkoss.zul.Toolbarbutton baca = new org.zkoss.zul.Toolbarbutton("(baca)");
            baca.setStyle("color:#2563eb;font-weight:bold;cursor:pointer;");
            baca.setParent(box);
            final boolean[] terbuka = new boolean[] { false };
            baca.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    terbuka[0] = !terbuka[0];
                    label.setValue(terbuka[0] ? teks : ringkas + "… ");
                    baca.setLabel(terbuka[0] ? "(tutup)" : "(baca)");
                }
            });
        }
        return box;
    }

    /** Memotong teks pada akhir kalimat pertama bila ada, jika tidak pada batas kata. */
    private static String potongRingkas(String teks, int maksKarakter) {
        if (teks == null) {
            return "";
        }
        if (teks.length() <= maksKarakter) {
            return teks;
        }
        int batasCari = Math.min(teks.length(), maksKarakter + 40);
        for (int i = 0; i < batasCari; i++) {
            char c = teks.charAt(i);
            if ((c == '.' || c == '!' || c == '?') && i >= 15) {
                return teks.substring(0, i + 1);
            }
        }
        int potong = teks.lastIndexOf(' ', maksKarakter);
        if (potong < maksKarakter / 2) {
            potong = maksKarakter;
        }
        return teks.substring(0, potong);
    }
}
