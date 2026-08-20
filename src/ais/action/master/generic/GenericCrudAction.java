package ais.action.master.generic;

import java.util.List;

import org.hibernate.Criteria;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.FilterLanjutHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.HeadlessActionContext;
import ais.common.HeadlessBusinessRuleException;
import ais.database.model.GeneralValueObject;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyGrid;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * Base class reusable untuk semua Action CRUD standar di ais.action.master.*.
 *
 * Mengeliminasi ~60% kode duplikat (doBeforeCompose, doAfterCompose,
 * onSearchDefault, onAdd, init, help button) yang berulang di ~200 Action class.
 *
 * Cara pemakaian — subclass WAJIB mengimplementasikan:
 *   getEntityClass()         kelas entitas Hibernate, mis. Agama.class
 *   createNewEntity()        buat instance entitas baru (blank) untuk form Tambah
 *   getWindowTitle()         judul window form tambah/ubah, mis. "Pendataan Agama"
 *   initCriteria(boolean)    bangun Criteria Hibernate; false=COUNT, true=DATA+ORDER
 *   createRenderer()         renderer baris MyGrid utama
 *   buildFormContent()       bangun isi form (Borderlayout + Rows + Toolbar)
 *                            di dalam addWindow yang sudah di-clear
 *
 * Field yang di-wire otomatis ZK dari ZUL (id harus sama):
 *   addWindow, paging, grid, add, searchnama, searchaktif
 *
 * Hook opsional:
 *   getDownloadUploadContents()   kolom export/import; return null = tidak ada
 *   getHelpContent()              HTML panduan modul; default = teks generik
 *   onAfterInit(Component)        init tambahan setelah doAfterCompose selesai
 */
public abstract class GenericCrudAction<T extends GeneralValueObject>
        extends GenericAutowireComposer
        implements DataCriteria, DataSearchDefault, DataInitDefault {

    private static final long serialVersionUID = 1L;

    // ---- ZK auto-wired dari ZUL (id harus cocok) ----
    protected MyWindow addWindow;
    protected Paging paging;
    protected MyGrid grid;
    protected MyToolbarbuttonConfig add;
    protected Textbox searchnama;
    protected Textbox searchkode;
    protected Checkbox searchaktif;

    protected T currentEntity;
    protected boolean edit = false;
    protected boolean delete = false;

    /** Key bantuan halaman (nama berkas zul tanpa ekstensi), ditangkap saat doBeforeCompose. */
    protected String helpKey;

    // ======================== Abstract (wajib di subclass) ========================

    protected abstract Class<T> getEntityClass();

    protected abstract T createNewEntity();

    protected abstract String getWindowTitle();

    public abstract Criteria initCriteria(boolean order);

    protected abstract MyRowRenderer createRenderer();

    /**
     * Bangun konten form di dalam window yang sudah di-clear.
     * Subclass harus menambahkan Borderlayout ke parameter window.
     */
    protected abstract void buildFormContent(MyWindow window, T entity) throws Exception;

    /**
     * Business rule penyimpanan tetap dimiliki subclass Action existing. New UI
     * memanggil kontrak yang sama setelah form headless diisi dari entity target.
     */
    public abstract boolean onSave(Event event) throws Exception;

    /**
     * Jalankan lifecycle simpan existing tanpa compose ZUL. Komponen input tetap
     * dibangun oleh {@link #buildFormContent} sehingga onSave membaca nilai yang
     * sama persis dengan halaman lama, tetapi window tidak pernah dirender.
     */
    public final void executeHeadlessSave(T entity) throws Exception {
        if (entity == null) {
            throw new HeadlessBusinessRuleException("Data yang akan disimpan tidak tersedia.");
        }
        MyWindow previousWindow = addWindow;
        T previousEntity = currentEntity;
        String captured = null;
        boolean accepted = false;
        try {
            currentEntity = entity;
            HeadlessActionContext.enter();
            addWindow = new MyWindow(true);
            buildFormContent(addWindow, entity);
            accepted = onSave(null);
        } finally {
            if (HeadlessActionContext.isActive()) captured = HeadlessActionContext.exit();
            try { if (addWindow != null) addWindow.detach(); } catch (Exception ignored) { }
            addWindow = previousWindow;
            currentEntity = previousEntity;
        }
        if (!accepted) {
            throw new HeadlessBusinessRuleException(captured == null || captured.length() == 0
                    ? "Validasi business rule existing menolak penyimpanan data." : captured);
        }
    }

    // ======================== Template methods (bisa di-override) ========================

    /**
     * Daftar kolom untuk tombol Download/Upload.
     * Return null jika modul tidak memerlukan download/upload.
     */
    protected String[] getDownloadUploadContents() {
        return null;
    }

    /**
     * Konten HTML untuk popup Bantuan.
     * Override di subclass untuk panduan yang spesifik per modul.
     */
    protected String getHelpContent() {
        String moduleName = getWindowTitle()
                .replace("Pendataan ", "").replace("Data ", "").replace("Master ", "");
        return buildDefaultHelpHtml(moduleName);
    }

    /**
     * Hook dipanggil di akhir doAfterCompose, setelah semua inisialisasi standar.
     * Override untuk menambah tombol custom atau inisialisasi tambahan.
     * Memanggil FilterLanjutHelper.setup() agar popup "Pencarian Lebih Lanjut"
     * otomatis tersedia di semua halaman yang memakai GenericCrudAction.
     */
    protected void onAfterInit(Component comp) throws Exception {
        FilterLanjutHelper.setup(comp);
    }

    /**
     * Judul spanduk pengantar di paling atas halaman. Return null = tanpa spanduk.
     * Override di subclass agar setiap modul punya penjelasan singkat untuk end user.
     */
    protected String getIntroTitle() {
        return null;
    }

    /**
     * Kalimat penjelas yang sangat sederhana (untuk end user awam) yang tampil di spanduk pengantar.
     * Hindari istilah teknis; jelaskan manfaat/gunanya dalam 1-2 kalimat.
     */
    protected String getIntroDescription() {
        return null;
    }

    // ======================== ZK lifecycle ========================

    @Override
    public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(
            org.zkoss.zk.ui.Page page,
            org.zkoss.zk.ui.Component parent,
            org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
        Common.doCheckSecurity();
        try {
            if (compInfo != null && compInfo.getPageDefinition() != null) {
                this.helpKey = keyDariPath(compInfo.getPageDefinition().getRequestPath());
            }
        } catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/generic/GenericCrudAction.java:157");
            // key bantuan opsional; jangan ganggu compose
        }
        return super.doBeforeCompose(page, parent, compInfo);
    }

    /** Turunkan key (nama berkas zul tanpa path/ekstensi) dari request path halaman. */
    private static String keyDariPath(String p) {
        if (p == null) {
            return null;
        }
        p = p.trim();
        int q = p.indexOf('?');
        if (q >= 0) {
            p = p.substring(0, q);
        }
        int s = Math.max(p.lastIndexOf('/'), p.lastIndexOf('\\'));
        if (s >= 0) {
            p = p.substring(s + 1);
        }
        if (p.toLowerCase().endsWith(".zul")) {
            p = p.substring(0, p.length() - 4);
        }
        p = p.trim().toLowerCase();
        return p.length() == 0 ? null : p;
    }

    /** Apakah tersedia berkas bantuan bespoke {@code WEB-INF/bantuan/<key>.html}. */
    private boolean bespokeBantuanAda(String key) {
        if (key == null) {
            return false;
        }
        try {
            org.zkoss.zk.ui.Execution ex = org.zkoss.zk.ui.Executions.getCurrent();
            if (ex == null || ex.getDesktop() == null) {
                return false;
            }
            String rp = ex.getDesktop().getWebApp().getRealPath("/WEB-INF/bantuan/" + key + ".html");
            return rp != null && new java.io.File(rp).isFile();
        } catch (Throwable t) {
            return false;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        Common.initLaguage();

        // Spanduk pengantar berisi penjelasan sederhana modul (reuse terpusat).
        ais.ui.util.DashboardUiKit.attachIntro(comp, getIntroTitle(), getIntroDescription());

        // Tombol Tambah: bila ZUL tidak mendeklarasikan id="add", buat otomatis & sisipkan
        // ke area aksi filter — base class tetap berfungsi tanpa NPE dan fungsi Tambah tetap ada.
        ensureAddButton(comp);
        if (add != null) {
            add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
            add.setTooltiptext("Tambah");
        }

        edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
        delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

        String[] downloadCols = getDownloadUploadContents();
        if (downloadCols != null && add != null) {
            Common.appendDownloadUploadButtons(add, getEntityClass(), this, this,
                    (add != null && add.isVisible()) && edit && delete, downloadCols);
        }

        appendHelpButton();

        onSearchDefault(null);

        Common.initPaging(paging, new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                onSearchDefault(null);
            }
        });

        onAfterInit(comp);
    }

    // ======================== CRUD operations ========================

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void onSearchDefault(Event event) {
        // FIX NullPointerException: subclass yg memuat sebagian ZUL-nya secara dinamis lewat
        // Include/MyInclude (mis. JenisKegiatanAction -- tab "Jenis Pembayaran" dimuat via
        // MyButtonTabbox.muatZul) bisa memicu doAfterCompose() composer ini terpanggil LAGI oleh ZK
        // untuk fragment yang baru selesai di-include, SEBELUM field grid (di-@Wire dari fragment
        // tsb) benar-benar terpasang. onSearchDefault() yang dipanggil pada momen prematur itu
        // menabrak grid==null. Lewati saja secara aman di sini -- pemanggilan doAfterCompose
        // berikutnya (setelah grid benar-benar ter-wire) akan mengisi grid dgn normal, jadi
        // fungsi pencarian tetap berjalan seperti biasa, hanya saja tidak crash pada momen prematur.
        if (grid == null) {
            return;
        }
        Common.initPaging(initCriteria(false), paging);
        List<T> data = initCriteria(true)
                .setMaxResults(Common.ROWS_COUNT_ON_PAGE)
                .setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
                .list();
        ListModel strset = new SimpleListModel(data);
        grid.setRowRenderer(createRenderer());
        grid.setModelCheckMobile(strset);
    }

    public void onAdd(Event event) throws Exception {
        init(createNewEntity());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init(GeneralValueObject obj) throws Exception {
        currentEntity = (T) obj;
        String moduleName = getWindowTitle()
                .replace("Pendataan ", "").replace("Data ", "").replace("Master ", "");
        boolean isNew = (obj.getId() == null);
        addWindow.setTitle((isNew ? "Tambah " : "Ubah ") + moduleName);
        Common.clear(addWindow);
        buildFormContent(addWindow, currentEntity);
        addWindow.setVisible(true);
        addWindow.onModal();
    }

    protected void openForm(T entity) throws Exception {
        init(entity);
    }

    // ======================== Tombol Tambah (auto-inject bila ZUL tak menyediakan) ========================

    /**
     * Pastikan tombol "Tambah" tersedia. Bila ZUL tidak mendeklarasikan {@code id="add"}
     * (cukup menyediakan kontainer ber-sclass "ais-crud-filter-actions"), tombol dibuat
     * otomatis dan disisipkan di paling depan area aksi filter.
     */
    private void ensureAddButton(Component comp) {
        if (add != null) {
            return;
        }
        try {
            Component actions = findBySclass(comp, "ais-crud-filter-actions");
            if (actions == null) {
                return;
            }
            MyToolbarbuttonConfig btn = new MyToolbarbuttonConfig("Tambah", "/img/new.gif");
            btn.setTooltiptext("Tambah data baru");
            btn.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    onAdd(e);
                }
            });
            if (actions.getFirstChild() != null) {
                actions.insertBefore(btn, actions.getFirstChild());
            } else {
                actions.appendChild(btn);
            }
            add = btn;
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/generic/GenericCrudAction.java:308");
            // bila gagal, add tetap null — pemanggil sudah menjaga dengan null-check
        }
    }

    /** Cari komponen pertama (DFS) yang sclass-nya mengandung {@code sclass}. */
    private static Component findBySclass(Component root, String sclass) {
        if (root == null) {
            return null;
        }
        if (root instanceof org.zkoss.zk.ui.HtmlBasedComponent) {
            String sc = ((org.zkoss.zk.ui.HtmlBasedComponent) root).getSclass();
            if (sc != null && sc.contains(sclass)) {
                return root;
            }
        }
        for (Object o : root.getChildren()) {
            Component f = findBySclass((Component) o, sclass);
            if (f != null) {
                return f;
            }
        }
        return null;
    }

    // ======================== Help button & window ========================

    protected void appendHelpButton() {
        if (add == null || add.getParent() == null) {
            return;
        }
        // Sakelar induk bantuan_tombol_tampil: bila tidak aktif, tombol Bantuan tidak
        // dibuat sama sekali. Tombol ini dibuat lewat Java sehingga afterCompose()
        // MyToolbarbuttonConfig tidak dipanggil -- pemeriksaannya harus di sini.
        try {
            if (!ais.action.master.helper.BantuanGlobalHook.tombolBantuanAktif()) {
                return;
            }
        } catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/generic/GenericCrudAction.java:appendHelpButton");
            /* gagal baca konfigurasi: pertahankan perilaku lama (tombol tampil) */
        }
        MyToolbarbuttonConfig bantuan = new MyToolbarbuttonConfig(
                "Bantuan", "/img/svg/question-circle.svg");
        bantuan.setTooltiptext("Panduan penggunaan modul ini");
        // Dibuat via Java (afterCompose tidak dipanggil), jadi pasang sclass floating
        // langsung agar seragam dengan tombol Bantuan lain saat fitur mengambang aktif.
        try {
            if (ais.action.master.helper.BantuanGlobalHook.gayaMengambangAktif()) {
                String sc = bantuan.getSclass();
                bantuan.setSclass((sc == null || sc.trim().length() == 0 ? "" : sc + " ") + "kb-fab-inline");
            }
        } catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/generic/GenericCrudAction.java:349");
            /* jangan ganggu render */
        }
        bantuan.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                onBantuan(event);
            }
        });
        bantuan.setParent(add.getParent());
    }

    public void onBantuan(Event event) throws Exception {
        // Utamakan bantuan bespoke (HTML ≥1000 kata) bila tersedia — rendering & tombol
        // Cetak sama persis dengan halaman lain. Introspektif hanya fallback (mis. modul
        // tanpa berkas bantuan).
        if (helpKey != null && bespokeBantuanAda(helpKey)) {
            org.zkoss.zk.ui.Component anchor =
                    (event != null && event.getTarget() instanceof org.zkoss.zk.ui.Component)
                            ? (org.zkoss.zk.ui.Component) event.getTarget()
                            : add;
            ais.action.master.helper.BantuanHelper.tampilkanDariResource(anchor, helpKey, "Bantuan");
            return;
        }

        String moduleName = getWindowTitle()
                .replace("Pendataan ", "").replace("Data ", "").replace("Master ", "");

        final MyWindow helpWindow = new MyWindow("Bantuan — " + moduleName, "none", true);
        helpWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
        helpWindow.setWidth("640px");
        helpWindow.setHeight("500px");

        Borderlayout bl = new MyBorderlayout();
        bl.setParent(helpWindow);

        Center center = new Center();
        center.setStyle("overflow:auto;padding:14px 18px;");
        ZkCompat.setFlex(center, true);
        center.setParent(bl);

        Html html = new Html();
        html.setContent(getHelpContent());
        html.setParent(center);

        South south = new South();
        ZkCompat.setFlex(south, true);
        south.setParent(bl);

        Toolbar toolbar = new Toolbar();
        toolbar.setParent(south);

        MyToolbarbuttonConfig tutup = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
        tutup.setTooltiptext("Tutup");
        tutup.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                helpWindow.setVisible(false);
                helpWindow.detach();
            }
        });
        tutup.setParent(toolbar);

        helpWindow.setVisible(true);
        helpWindow.onModal();
    }

    // ======================== Default help content generator ========================

    private static String buildDefaultHelpHtml(String moduleName) {
        StringBuilder sb = new StringBuilder(2048);
        sb.append("<div style='font-family:sans-serif;font-size:13px;line-height:1.8;color:#1e293b;'>");

        sb.append("<h3 style='margin-top:0;color:#1d4ed8;"
                + "border-bottom:2px solid #dbeafe;padding-bottom:6px;'>");
        sb.append("&#128218; Panduan Penggunaan &mdash; ").append(moduleName).append("</h3>");

        sb.append("<h4 style='color:#0f172a;margin-bottom:4px;'>&#128269; Pencarian Data</h4>");
        sb.append("<ul style='margin-top:0;padding-left:18px;'>");
        sb.append("<li>Isi kolom pencarian yang tersedia di <b>Kartu Filter</b> lalu tekan <b>Enter</b> "
                + "atau klik <b>Cari</b>.</li>");
        sb.append("<li>Centang <b>Tampilkan hanya yang aktif</b> untuk memfilter hanya data aktif.</li>");
        sb.append("<li>Panel filter selalu tampil — tidak perlu menutup/membuka; ubah nilai lalu klik Cari.</li>");
        sb.append("</ul>");

        sb.append("<h4 style='color:#0f172a;margin-bottom:4px;'>&#10133; Menambah Data Baru</h4>");
        sb.append("<ul style='margin-top:0;padding-left:18px;'>");
        sb.append("<li>Klik tombol <b>Tambah</b> di sudut kanan atas Kartu Filter.</li>");
        sb.append("<li>Isi semua kolom yang bertanda <b>*</b> (wajib diisi).</li>");
        sb.append("<li>Klik <b>Simpan</b> untuk menyimpan, atau <b>Batal</b> untuk membatalkan.</li>");
        sb.append("</ul>");

        sb.append("<h4 style='color:#0f172a;margin-bottom:4px;'>&#9998;&#65039; Mengubah Data</h4>");
        sb.append("<ul style='margin-top:0;padding-left:18px;'>");
        sb.append("<li>Klik ikon <b>Edit</b> (pensil) di baris data yang ingin diubah.</li>");
        sb.append("<li>Ubah nilai yang diperlukan, lalu klik <b>Simpan</b>.</li>");
        sb.append("<li>Tombol Edit hanya muncul jika Anda memiliki hak akses <em>Update</em>.</li>");
        sb.append("</ul>");

        sb.append("<h4 style='color:#0f172a;margin-bottom:4px;'>&#128465;&#65039; Menghapus Data</h4>");
        sb.append("<ul style='margin-top:0;padding-left:18px;'>");
        sb.append("<li>Klik ikon <b>Hapus</b> (tempat sampah) lalu konfirmasi pada dialog yang muncul.</li>");
        sb.append("<li>Data yang masih berelasi dengan modul lain <b>tidak dapat dihapus</b>.</li>");
        sb.append("<li>Tombol Hapus hanya muncul jika Anda memiliki hak akses <em>Delete</em>.</li>");
        sb.append("</ul>");

        sb.append("<h4 style='color:#0f172a;margin-bottom:4px;'>&#9989; Status Aktif</h4>");
        sb.append("<ul style='margin-top:0;padding-left:18px;'>");
        sb.append("<li>Centang/hapus centang kolom <b>Aktif</b> langsung di daftar "
                + "untuk mengubah status tanpa membuka form ubah.</li>");
        sb.append("<li>Data tidak aktif tidak akan muncul sebagai pilihan di modul lain.</li>");
        sb.append("</ul>");

        sb.append("<h4 style='color:#0f172a;margin-bottom:4px;'>&#128196; Riwayat Perubahan</h4>");
        sb.append("<ul style='margin-top:0;padding-left:18px;'>");
        sb.append("<li>Klik nama data (biru, bergaris bawah) untuk melihat riwayat perubahan, "
                + "termasuk siapa yang mengubah dan kapan.</li>");
        sb.append("</ul>");

        sb.append("<div style='background:#f0f9ff;border:1px solid #bae6fd;"
                + "border-radius:6px;padding:10px 14px;margin-top:14px;'>");
        sb.append("<b>&#128161; Tips:</b> Gunakan fitur <b>Download</b> untuk mengunduh data ke Excel, "
                + "dan <b>Upload</b> untuk impor data massal. "
                + "Unduh data yang sudah ada terlebih dahulu sebagai contoh format kolom yang benar.</div>");

        sb.append("</div>");
        return sb.toString();
    }
}
