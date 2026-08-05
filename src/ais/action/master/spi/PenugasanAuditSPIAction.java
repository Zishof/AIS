package ais.action.master.spi;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.AmbilDataTbmuserBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.spi.ChecklistAuditSPI;
import ais.database.model.spi.JenisAuditSPI;
import ais.database.model.spi.KriteriaAuditSPI;
import ais.database.model.spi.PenugasanAuditSPI;
import ais.database.model.spi.RencanaAuditTahunanSPI;
import ais.database.model.spi.TemuanAuditSPI;
import ais.database.model.spi.TimAuditSPI;
import ais.ui.util.FormSop;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/**
 * <h2>PenugasanAuditSPIAction &mdash; Pengendali Utama Layar Pelaksanaan Audit (Bagian C)</h2>
 *
 * <p>
 * Kelas ini adalah pengendali ZK sekaligus jembatan ke mesin SOP/Disposisi untuk
 * {@link PenugasanAuditSPI} &mdash; layar tempat staf SPI mengajukan penugasan audit baru,
 * mengelola tim auditornya ({@link TimAuditSPI}), mengisi checklist temuan
 * ({@link #tampilRinci(JenisAuditSPI, PenugasanAuditSPI, boolean)}), dan mengikuti alur
 * persetujuan berjenjang. Kelas ini mengimplementasikan {@code ais.ui.util.FormSop} agar mesin
 * SOP generik yang SUDAH ADA di aplikasi ini ({@code TampilanAlurSopAction}/
 * {@code DisposisiSopAction}) bisa memanggil formulir ini secara REFLEKSI
 * (({@code Class.forName(alurSop.getFormInputan()).newInstance()})) di setiap titik alur
 * persetujuan yang dikonfigurasi admin &mdash; sehingga TIDAK ADA kode routing/approval baru yang
 * perlu ditulis di sini sama sekali; satu-satunya syarat adalah kelas ini memiliki constructor
 * tanpa-argumen (dipakai reflection) dan mengimplementasikan seluruh method kontrak
 * {@code FormSop} dengan benar.
 * </p>
 *
 * <h3>Perbedaan struktural dari template SPMI ({@code HasilSPMIAction})</h3>
 * <p>
 * Kelas ini SENGAJA meniru kerangka besar {@code HasilSPMIAction} (yang sudah production-proven
 * di modul Audit Mutu Internal akademik) karena kebutuhan "dokumen ber-checklist dengan alur
 * persetujuan SOP" memang identik. Namun tiga perbedaan disengaja mengikuti hasil analisis
 * best-practice modul SPI:
 * </p>
 * <ol>
 *   <li><b>Auditee terstruktur.</b> Field {@link PenugasanAuditSPI#getSatuanKerja()} menggantikan
 *       kombinasi Fakultas/Jurusan milik SPMI (yang hanya cocok konteks akademik perguruan tinggi)
 *       &mdash; {@code SatuanKerja} adalah representasi unit organisasi generik yang sudah dipakai
 *       lintas eCampus MAUPUN eSchool di modul lain, sehingga modul SPI otomatis kompatibel dengan
 *       kedua jenis lembaga tanpa kode tambahan.</li>
 *   <li><b>Tim auditor terstruktur, bukan teks bebas.</b> {@link #buildTimAuditPanel} mengelola
 *       baris {@link TimAuditSPI} lewat pencarian pengguna ({@link AmbilDataTbmuserBanbox},
 *       komponen pencarian bertahap yang TIDAK memuat seluruh tabel pengguna ke memori sekaligus
 *       &mdash; penting mengingat tabel pengguna aplikasi ini bisa berisi ribuan baris).</li>
 *   <li><b>Temuan diisi lewat popup terpisah, bukan inline.</b> Berbeda dari SPMI yang menaruh
 *       kotak hasil-temuan langsung di dalam baris grid, {@link #tampilRinci} hanya menampilkan
 *       ringkasan per-checklist dengan tombol "Isi Temuan" yang membuka
 *       {@link TemuanAuditSPIAction#openForChecklist}. Struktur temuan SPI memiliki 5 ruas teks
 *       (Kondisi/Kriteria/Sebab/Akibat/Rekomendasi &mdash; lihat javadoc {@code TemuanAuditSPI})
 *       yang jauh lebih kaya dari temuan SPMI (1 ruas teks + status), sehingga tidak realistis
 *       ditampilkan nyaman sebagai kotak sempit di dalam satu baris tabel.</li>
 * </ol>
 *
 * <h3>Pemilih Unit Kerja memakai pencarian bertahap, bukan combobox biasa</h3>
 * <p>
 * {@link #searchsatuanKerja} dan {@link #satuanKerja} (pada formulir) memakai
 * {@code ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox} &mdash; konvensi baku di 300-an
 * layar lain aplikasi ini untuk memilih {@link SatuanKerja}. Versi awal layar ini memakai
 * {@code Combobox} biasa diisi lewat {@code Common.insertComboDanSemua(...)}, yang memuat SELURUH
 * baris {@link SatuanKerja} ke memori setiap kali layar dibuka &mdash; terbukti membuat layar ini
 * (dan dua layar SPI lain yang memakai pola sama) terasa sangat lambat pada instalasi dengan banyak
 * unit kerja. Picker ini memuat data secara bertahap (pohon hierarki per-level, tabel "sering
 * dipakai" dengan paging sisi server).
 * </p>
 *
 * <h3>Cetak (PDF/Excel/dst.) lewat {@link ais.action.report.format1.spi.LaporanPenugasanAuditSPI}</h3>
 * <p>
 * {@link #cetakData(GeneralValueObject)} dan {@link #cetak(PenugasanAuditSPI)} membangun jendela
 * pratinjau lewat {@code LaporanPenugasanAuditSPI}, yang meniru persis kerangka
 * {@code LaporanHasilSPMI} milik modul SPMI. Karena toolbar pratinjaunya dibangun lewat
 * {@code CommonReport.exportReport} (helper baku aplikasi ini), SATU template Jasper
 * ({@code format1/spi/lembar_kerja_penugasan_audit_spi}) sudah otomatis menyediakan tombol unduh
 * PDF MAUPUN Excel sekaligus &mdash; tidak perlu jalur ekspor terpisah untuk masing-masing format.
 * </p>
 *
 * @author e-Campus SPI Team
 */
public class PenugasanAuditSPIAction extends BaseSPIAction implements FormSop {

    private static final long serialVersionUID = 1L;

    // ---- Search fields ----
    private Combobox searchjenis;
    private Combobox searchstatus;
    private AmbilDataSatuanKerjaBanbox searchsatuanKerja;
    private MyDatebox start;
    private MyDatebox end;

    // ---- Form fields ----
    private Textbox  nama;
    private Textbox  keterangan;
    private Combobox jenisAuditSPI;
    private AmbilDataSatuanKerjaBanbox satuanKerja;
    private Combobox rencanaAuditTahunanSPI;
    private MyDatebox tanggalMulai;
    private MyDatebox tanggalSelesai;
    private Radiogroup status;
    private MyDatebox  tanggalPersetujuanManual;
    private Row        rowDetail;
    private Row        rowTim;

    // ---- State ----
    public PenugasanAuditSPI penugasanAuditSPI;
    private Tbmuser  tbmuser;
    private boolean  persetujuan = false;
    private boolean  setujui     = false;
    private boolean  viewOnly    = false;
    private DisposisiSop disposisiSop;
    protected LampiranLain lainMahasiswa;

    protected org.zkoss.zul.Tabpanel dasborTab;

    public static final String[] contents = new String[]{
            "id", "jenisAuditSPI", "satuanKerja", "rencanaAuditTahunanSPI", "disposisiSop",
            "tanggalMulai", "tanggalSelesai", "nama", "keterangan", "aktif",
            "status", "dibuatOleh", "disetujuiOleh", "tanggalPembuatan", "tanggalPersetujuan"
    };

    public PenugasanAuditSPIAction() {
        tbmuser = Common.getCurrentUser();
    }

    public PenugasanAuditSPIAction(boolean persetujuan) {
        this.persetujuan = persetujuan;
        tbmuser = Common.getCurrentUser();
    }

    // =====================================================================
    // ZK lifecycle
    // =====================================================================

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        Common.initLaguage();

        if (session.getAttribute("usersTemp") == null
                || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
            session.removeAttribute("usersTemp");
            Common.goLogoff();
            return;
        }

        Common.insertComboDanSemua(searchjenis, "nama", "keterangan",
                JenisAuditSPI.class, Restrictions.eq("aktif", true));
        searchsatuanKerja.setEventListener(new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                onSearchDefault(e);
            }
        });

        if (start != null) start.setReadonly(true);
        if (end != null) end.setReadonly(true);
        Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
        cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) - 6);
        if (start != null) start.setValue(cal.getTime());
        cal = ais.ui.util.WaktuUtil.getCalendar();
        cal.set(Calendar.DATE, cal.get(Calendar.DATE) + 1);
        if (end != null) end.setValue(cal.getTime());

        Comboitem semua = new Comboitem("Semua");
        semua.setValue(null);
        searchstatus.appendChild(semua);
        for (String s : new String[]{PenugasanAuditSPI.PENGAJUAN, PenugasanAuditSPI.DISETUJU, PenugasanAuditSPI.DITOLAK}) {
            Comboitem ci = new Comboitem(s);
            ci.setValue(s);
            searchstatus.appendChild(ci);
        }
        searchstatus.setSelectedItem(semua);
        searchstatus.setReadonly(true);

        if (execution.getParameter("persetujuan") != null) {
            persetujuan = Boolean.parseBoolean(execution.getParameter("persetujuan"));
        }

        initPrivileges();
        if (add != null) {
            add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE) && !persetujuan);
        }

        initPagingListener();
        appendCetakUpload(PenugasanAuditSPI.class, contents);

        onSearchDefault(null);

        if (dasborTab != null) {
            Common.createDefaultTimer(new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    onDasbor(null);
                }
            });
        }
    }

    /** Memuat dasbor SPI secara malas (hanya sekali) saat tab Dasbor pertama kali dibuka. */
    public void onDasbor(Event event) throws Exception {
        if (dasborTab != null && dasborTab.getChildren().isEmpty()) {
            DasboardSPI dasbor = new DasboardSPI();
            ais.ui.util.BaseDasbordPortal.mountWrapped(dasbor, dasborTab,
                "Dasbor Satuan Pengawasan Internal",
                "Gambaran keseluruhan pelaksanaan audit internal: kepatuhan, temuan, dan tindak lanjut.");
        }
    }

    // =====================================================================
    // Row renderer
    // =====================================================================

    class PenugasanAuditSPIRenderer extends ais.ui.util.MyRowRenderer {
        @Override
        public void render(final Row row, Object obj) throws Exception {
            row.setValign("top");
            final PenugasanAuditSPI item = (PenugasanAuditSPI) obj;

            Vbox nameVbox;
            (nameVbox = RevisiHelper.createNewRevisi(PenugasanAuditSPI.class, item,
                    item.getNama() == null ? "" : item.getNama().trim())).setParent(row);
            Hbox docHbox = new Hbox();
            docHbox.setParent(nameVbox);
            LampiranLain.createDownloadUploadFileLain(docHbox, item.getId(),
                    PenugasanAuditSPI.class.getName(), "Dokumen Penugasan Audit",
                    false, null, null, false, false, false, true);

            Vbox jenisVbox = new Vbox();
            jenisVbox.setParent(row);
            new Label(item.getJenisAuditSPI() == null ? "" : item.getJenisAuditSPI().getNama()).setParent(jenisVbox);
            new Label(item.getSatuanKerja()  == null ? "" : item.getSatuanKerja().getNama()).setParent(jenisVbox);

            Vbox tglVbox = new Vbox();
            tglVbox.setParent(row);
            new Label(item.getTanggalMulai() == null ? "" : Common.dateFormat3.get().format(item.getTanggalMulai())).setParent(tglVbox);
            new MyLabelAgakKecil(item.getTanggalSelesai() == null ? "s/d berjalan"
                    : "s/d " + Common.dateFormat3.get().format(item.getTanggalSelesai())).setParent(tglVbox);

            Vbox pengajuanVbox = new Vbox();
            pengajuanVbox.setParent(row);
            new Label(Common.dateFormat3.get().format(item.getTanggalPembuatan())).setParent(pengajuanVbox);
            new MyLabelAgakKecil(item.getDibuatOleh() == null ? "" : item.getDibuatOleh().getUserNama()).setParent(pengajuanVbox);

            Vbox persetujuanVbox = new Vbox();
            persetujuanVbox.setParent(row);
            new Label(item.getStatus()).setParent(persetujuanVbox);
            new MyLabelAgakKecil(item.getDisetujuiOleh() == null ? "" : item.getDisetujuiOleh().getUserNama()).setParent(persetujuanVbox);

            Vbox ketVbox = new Vbox();
            ketVbox.setParent(row);
            new MyLabelAgakKecil(Common.simpleString(item.getKeterangan())).setParent(ketVbox);
            if (item.getDisposisiSop() != null) {
                A sopLink = new A();
                sopLink.setParent(ketVbox);
                sopLink.setStyle("font-size:9px;");
                UIClassHelper.applyReadMore(sopLink,
                        "SOP " + item.getDisposisiSop().getKeterangan()
                        + " (" + item.getDisposisiSop().getSop().getNama() + ")");
                sopLink.addEventListener("onClick", new EventListener() {
                    @Override
                    public void onEvent(Event e) throws Exception {
                        TampilanAlurSopAction.prosess(
                                item.getDisposisiSop().getId(), null, null, true, e.getTarget());
                    }
                });
            }

            if (item.getDisposisiSop() != null && !item.getDisposisiSop().getAktif()) {
                new Label(Common.getBahasaConfig("Tidak aktif")).setParent(row);
            } else if (persetujuan && !item.getStatus().equals(PenugasanAuditSPI.DISETUJU)) {
                final MyCheckboxConfig aktifCb = new MyCheckboxConfig("Aktif");
                aktifCb.setChecked(item.getAktif());
                aktifCb.setParent(row);
                aktifCb.addEventListener("onCheck", new EventListener() {
                    @Override
                    public void onEvent(Event e) throws Exception {
                        item.setAktif(aktifCb.isChecked());
                        Common.refreshSaveOrUpdate(item);
                    }
                });
            } else {
                new Label(item.getAktif() ? "Ya" : "Tidak").setParent(row);
            }

            boolean canEdit = edit && !persetujuan && !item.getStatus().equals(PenugasanAuditSPI.DISETUJU);
            Hbox actionHbox = Common.copyEditDeleteButtons(canEdit, canEdit,
                    delete && !persetujuan && !item.getStatus().equals(PenugasanAuditSPI.DISETUJU),
                    item, PenugasanAuditSPIAction.this);
            actionHbox.setParent(row);

            MyToolbarbuttonConfig printBtn = new MyToolbarbuttonConfig("", "/img/print.png");
            printBtn.setTooltiptext("Cetak (PDF/Excel)");
            printBtn.setOrient("vertical");
            printBtn.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    cetak(item);
                }
            });
            printBtn.setParent(actionHbox);
        }
    }

    // =====================================================================
    // Add / Edit entry points
    // =====================================================================

    public void onAdd(Event event) throws Exception {
        viewOnly = false;
        init(new PenugasanAuditSPI());
    }

    @Override
    public void init(GeneralValueObject obj) throws Exception {
        penugasanAuditSPI = (PenugasanAuditSPI) obj;
        buildFormWindow(penugasanAuditSPI);
        openAddWindow();
    }

    // =====================================================================
    // FormSop.form() — builds the detail grid used by the SOP workflow engine
    // =====================================================================

    @SuppressWarnings("deprecation")
    @Override
    public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSopArg,
            final MyToolbarbuttonConfig save, final EventListener setujuiData) throws Exception {

        this.disposisiSop = (this.disposisiSop != null
                && (disposisiSopArg == null || disposisiSopArg.getId() == null))
                        ? this.disposisiSop : disposisiSopArg;
        penugasanAuditSPI = (PenugasanAuditSPI) generalValueObject;
        setujui = false;

        if (penugasanAuditSPI != null && penugasanAuditSPI.getStatus().equals(PenugasanAuditSPI.DISETUJU)) {
            setujui = true;
        }
        if (penugasanAuditSPI.getDisposisiSop() != null
                && penugasanAuditSPI.getDisposisiSop().getDisposisiSetuju() != null
                && penugasanAuditSPI.getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null
                && penugasanAuditSPI.getDisposisiSop().getDisposisiSetuju().getSelesai()) {
            viewOnly = true;
        }

        MyGrid grid = new MyGrid();
        grid.setWidth("100%");
        grid.setHeight("100%");

        Columns columns = new Columns();
        columns.setParent(grid);
        MyColumnConfig labelCol = new MyColumnConfig();
        labelCol.setWidth("30%");
        labelCol.setParent(columns);
        new MyColumnConfig().setParent(columns);

        Rows rows = new Rows();
        rows.setParent(grid);

        // -- Judul --
        MyFormRow row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Judul Penugasan Audit *"));
        nama = new Textbox(penugasanAuditSPI.getNama());
        row.appendChild((persetujuan || setujui || viewOnly) ? new Label(penugasanAuditSPI.getNama()) : nama);
        nama.setWidth("90%");
        nama.setRows(2);
        nama.setTooltiptext("Contoh: Audit Keuangan Fakultas Ekonomi Semester Ganjil 2026");

        // -- Jenis Audit --
        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Audit *"));
        row.appendChild(jenisAuditSPI = new Combobox());
        Common.insertCombo(jenisAuditSPI, "nama", "keterangan",
                JenisAuditSPI.class, Restrictions.eq("aktif", true));
        jenisAuditSPI.setReadonly(true);
        Common.selectComboItem(true, jenisAuditSPI, penugasanAuditSPI.getJenisAuditSPI());

        // -- Unit Kerja (Auditee) --
        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Unit Kerja yang Diaudit (Auditee) *"));
        satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
        if (penugasanAuditSPI.getSatuanKerja() != null) {
            satuanKerja.setValue(penugasanAuditSPI.getSatuanKerja().getNama());
            satuanKerja.setAttribute("satuanKerja", penugasanAuditSPI.getSatuanKerja());
        }
        row.appendChild((persetujuan || setujui || viewOnly)
                ? new Label(penugasanAuditSPI.getSatuanKerja() == null ? "" : penugasanAuditSPI.getSatuanKerja().getNama())
                : satuanKerja);
        satuanKerja.setWidth("90%");

        // -- Dasar Rencana Audit Tahunan (PKPT), opsional --
        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Dasar Rencana Audit Tahunan (PKPT)"));
        rencanaAuditTahunanSPI = new Combobox();
        rencanaAuditTahunanSPI.setReadonly(true);
        populateRencanaCombo(rencanaAuditTahunanSPI);
        Common.selectComboItem(rencanaAuditTahunanSPI, penugasanAuditSPI.getRencanaAuditTahunanSPI());
        row.appendChild((persetujuan || setujui || viewOnly)
                ? new Label(penugasanAuditSPI.getRencanaAuditTahunanSPI() == null ? "(Audit Khusus/Insidental)"
                        : penugasanAuditSPI.getRencanaAuditTahunanSPI().toString())
                : rencanaAuditTahunanSPI);
        rencanaAuditTahunanSPI.setWidth("90%");

        // -- Tanggal Mulai / Selesai --
        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Mulai Pelaksanaan *"));
        Hbox mulaiHbox = new Hbox();
        row.appendChild(mulaiHbox);
        tanggalMulai = new MyDatebox(penugasanAuditSPI.getTanggalMulai());
        tanggalMulai.setFormat(Common.dateFormat3.get().toPattern());
        if (persetujuan || setujui || viewOnly) {
            mulaiHbox.appendChild(new Label(Common.dateFormat6.get().format(penugasanAuditSPI.getTanggalMulai())));
        } else {
            tanggalMulai.setParent(mulaiHbox);
        }
        tanggalMulai.setReadonly(true);

        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Selesai Pelaksanaan"));
        Hbox selesaiHbox = new Hbox();
        row.appendChild(selesaiHbox);
        tanggalSelesai = new MyDatebox(penugasanAuditSPI.getTanggalSelesai());
        tanggalSelesai.setFormat(Common.dateFormat3.get().toPattern());
        if (persetujuan || setujui || viewOnly) {
            selesaiHbox.appendChild(new Label(penugasanAuditSPI.getTanggalSelesai() == null ? "-"
                    : Common.dateFormat6.get().format(penugasanAuditSPI.getTanggalSelesai())));
        } else {
            tanggalSelesai.setParent(selesaiHbox);
        }
        tanggalSelesai.setReadonly(true);

        // -- Dokumen --
        lainMahasiswa = null;
        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Dokumen Penugasan (Surat Tugas)"));
        Hbox docHbox = new Hbox();
        LampiranLain.createDownloadUploadFileLain(docHbox, penugasanAuditSPI.getId(),
                PenugasanAuditSPI.class.getName(), "Dokumen Penugasan Audit", false,
                new EventListener() {
                    @Override
                    public void onEvent(Event e) throws Exception {
                        lainMahasiswa = (LampiranLain) e.getData();
                    }
                }, null, false, false, false, !(persetujuan || setujui || viewOnly));
        docHbox.setParent(row);

        // -- Keterangan --
        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
        keterangan = new Textbox(penugasanAuditSPI.getKeterangan() == null ? "" : penugasanAuditSPI.getKeterangan());
        row.appendChild(setujui ? new Label(penugasanAuditSPI.getKeterangan() == null ? "" : penugasanAuditSPI.getKeterangan())
                                 : keterangan);
        keterangan.setWidth("90%");
        keterangan.setRows(2);

        // -- Tim Audit --
        rowTim = new MyFormRow();
        ZkCompat.setSpans(rowTim, "2");
        rowTim.setParent(rows);
        rebuildTimAuditPanel(!(persetujuan || setujui || viewOnly));

        // -- Detail grid (checklist/temuan) row --
        rowDetail = new MyFormRow();
        ZkCompat.setSpans(rowDetail, "2");
        rowDetail.setParent(rows);

        EventListener jenisChangeListener = new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                JenisAuditSPI work = (JenisAuditSPI) (jenisAuditSPI.getSelectedItem() == null ? null
                        : jenisAuditSPI.getSelectedItem().getValue());
                penugasanAuditSPI.setJenisAuditSPI(work);
                Common.clear(rowDetail);
                tampilRinci(work, penugasanAuditSPI, !(persetujuan || setujui || viewOnly)).setParent(rowDetail);
            }
        };
        jenisAuditSPI.addEventListener("onChange", jenisChangeListener);

        // -- Status Pengajuan (approver) --
        row = new MyFormRow();
        row.setVisible(persetujuan && !viewOnly && (disposisiSop == null || disposisiSop.getId() == null));
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Status Pengajuan"));
        status = new Radiogroup();
        Radio rPengajuan = new Radio(PenugasanAuditSPI.PENGAJUAN);
        rPengajuan.setValue(PenugasanAuditSPI.PENGAJUAN);
        rPengajuan.setVisible(false);
        status.appendChild(rPengajuan);
        Radio rSetuju = new Radio(PenugasanAuditSPI.DISETUJU);
        rSetuju.setValue(PenugasanAuditSPI.DISETUJU);
        status.appendChild(rSetuju);
        Radio rTolak = new Radio(PenugasanAuditSPI.DITOLAK);
        rTolak.setValue(PenugasanAuditSPI.DITOLAK);
        status.appendChild(rTolak);
        status.setWidth("90%");
        Common.selectRadioItem(status, penugasanAuditSPI.getStatus());
        row.appendChild(status);

        grid.setAttribute("eventListenerSetuju", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                if (e != null && e.getTarget() instanceof Checkbox) {
                    Checkbox cb = (Checkbox) e.getTarget();
                    Boolean selesai = (Boolean) cb.getAttribute("checkbox");
                    if (selesai != null && selesai) {
                        Common.selectRadioItem(status, PenugasanAuditSPI.DISETUJU);
                        Common.freeze(status, true);
                    } else {
                        status.setSelectedItem(null);
                        Common.freeze(status, false);
                    }
                }
            }
        });

        if (setujuiData != null) {
            status.addEventListener("onClick", setujuiData);
            Common.createDefaultTimer(new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    setujuiData.onEvent(new Event("", null, penugasanAuditSPI.getStatus().equals(PenugasanAuditSPI.DISETUJU)));
                }
            });
        }

        if (setujui) {
            row = new MyFormRow();
            row.setParent(rows);
            row.appendChild(new ais.ui.util.MyLabelConfig("Status Pengajuan"));
            row.appendChild(new ais.ui.util.MyLabelConfig(penugasanAuditSPI.getStatus()));
        }

        // -- Tanggal Persetujuan --
        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Persetujuan"));
        tanggalPersetujuanManual = new MyDatebox(penugasanAuditSPI.getTanggalPersetujuanManual());
        row.appendChild(new Label(Common.dateFormat1.get().format(
                penugasanAuditSPI.getTanggalPersetujuanManual() == null
                        ? ais.ui.util.WaktuUtil.getDate() : penugasanAuditSPI.getTanggalPersetujuanManual())));
        tanggalPersetujuanManual.setReadonly(true);
        tanggalPersetujuanManual.addEventListener("onChange", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                if (penugasanAuditSPI != null && penugasanAuditSPI.getId() != null) {
                    penugasanAuditSPI.setTanggalPersetujuanManual(tanggalPersetujuanManual.getValue());
                    Common.refreshUpdate(penugasanAuditSPI);
                }
            }
        });

        jenisChangeListener.onEvent(null);

        EventListener saveLabelListener = new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                boolean disetujui = status.getSelectedItem() != null
                        && status.getSelectedItem().getValue().equals(PenugasanAuditSPI.DISETUJU);
                if (tanggalPersetujuanManual != null && tanggalPersetujuanManual.getParent() != null) {
                    if (tanggalPersetujuanManual.getValue() == null) {
                        tanggalPersetujuanManual.setValue(ais.ui.util.WaktuUtil.getDate());
                    }
                    tanggalPersetujuanManual.getParent().setVisible(disetujui);
                }
                if (disetujui) {
                    save.setLabel("Selesaikan dan Setujui Penugasan");
                } else {
                    save.setLabel(!persetujuan ? "Simpan" : "Ubah Status Persetujuan");
                }
            }
        };
        status.addEventListener("onClick", saveLabelListener);
        Common.createDefaultTimer(saveLabelListener);

        return grid;
    }

    // =====================================================================
    // Tim Audit panel — searchable add/remove of team members
    // =====================================================================

    @SuppressWarnings("unchecked")
    private void rebuildTimAuditPanel(final boolean editable) {
        Common.clear(rowTim);
        Div panel = new Div();
        panel.setStyle("width:100%; box-sizing:border-box; padding:6px 0;");
        panel.setParent(rowTim);

        org.zkoss.zul.Label judulTim = new org.zkoss.zul.Label("Tim Audit");
        judulTim.setStyle("font-weight:700; font-size:12px; color:#0f172a;");
        judulTim.setParent(panel);

        if (penugasanAuditSPI.getId() == null) {
            new Html("<div style='padding:6px 0; color:#94a3b8; font-size:11px;'>"
                    + "Simpan penugasan terlebih dahulu sebelum menambahkan anggota tim.</div>").setParent(panel);
            return;
        }

        Session session = HibernateUtil.currentSession();
        List<TimAuditSPI> anggota = session.createCriteria(TimAuditSPI.class)
                .add(Restrictions.eq("penugasanAuditSPI", penugasanAuditSPI))
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .addOrder(Order.asc("peranTim"))
                .list();

        if (anggota.isEmpty()) {
            new Html("<div style='padding:4px 0; color:#94a3b8; font-size:11px;'>Belum ada anggota tim.</div>").setParent(panel);
        } else {
            for (final TimAuditSPI a : anggota) {
                Hbox row = new Hbox();
                row.setStyle("align-items:center; gap:8px; padding:2px 0;");
                row.setParent(panel);
                org.zkoss.zul.Label badge = new org.zkoss.zul.Label(a.getPeranTimLabel());
                badge.setSclass("ais-badge " + (TimAuditSPI.KETUA_TIM.equals(a.getPeranTim()) ? "ais-badge-biru" : "ais-badge-abu"));
                badge.setParent(row);
                new org.zkoss.zul.Label(a.getAnggota() == null ? "-" : a.getAnggota().getUserNama()).setParent(row);
                if (editable) {
                    MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("", "/img/delete.gif");
                    hapus.setTooltiptext("Keluarkan dari tim");
                    hapus.addEventListener("onClick", new EventListener() {
                        @Override
                        public void onEvent(Event e) throws Exception {
                            a.setAktif(false);
                            Common.refreshSaveOrUpdate(a);
                            rebuildTimAuditPanel(editable);
                        }
                    });
                    hapus.setParent(row);
                }
            }
        }

        if (editable) {
            Hbox addRow = new Hbox();
            addRow.setStyle("align-items:center; gap:8px; padding:6px 0 0;");
            addRow.setParent(panel);

            final AmbilDataTbmuserBanbox picker = new AmbilDataTbmuserBanbox();
            picker.setWidth("220px");
            picker.setParent(addRow);

            final Combobox peran = new Combobox();
            peran.setReadonly(true);
            peran.setWidth("140px");
            for (java.util.Map.Entry<String, String> e : TimAuditSPI.PERAN_TIM_DATA.entrySet()) {
                Comboitem ci = new Comboitem(e.getValue());
                ci.setValue(e.getKey());
                peran.appendChild(ci);
            }
            peran.setSelectedIndex(1); // default: Anggota Tim
            peran.setParent(addRow);

            MyToolbarbuttonConfig tambah = new MyToolbarbuttonConfig("Tambah ke Tim", "/img/new.gif");
            tambah.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    Tbmuser dipilih = (Tbmuser) picker.getAttribute("tbmuser");
                    if (dipilih == null) {
                        MyMessageboxConfig.show("Mohon maaf, pengguna anggota tim belum dipilih."
                                + " Langkah yang dapat dilakukan:"
                                + " (1) klik ikon pemilih pengguna dan cari nama yang akan ditambahkan ke Tim Audit;"
                                + " (2) pastikan pengguna sudah terdaftar di sistem;"
                                + " (3) klik tombol Tambah kembali setelah memilih."
                                + " Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                                MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                        return;
                    }
                    TimAuditSPI baru = new TimAuditSPI(penugasanAuditSPI);
                    baru.setAnggota(dipilih);
                    baru.setPeranTim(peran.getSelectedItem() == null ? TimAuditSPI.ANGGOTA_TIM
                            : (String) peran.getSelectedItem().getValue());
                    Common.refreshSaveOrUpdate(baru);
                    rebuildTimAuditPanel(editable);
                }
            });
            tambah.setParent(addRow);
        }
    }

    // =====================================================================
    // Combobox manual: Rencana Audit Tahunan (label gabungan, bukan 1 properti)
    // =====================================================================

    @SuppressWarnings("unchecked")
    private void populateRencanaCombo(Combobox cb) {
        Common.clear(cb);
        Comboitem kosong = new Comboitem("(Audit Khusus/Insidental — tanpa PKPT)");
        kosong.setValue(null);
        cb.appendChild(kosong);

        Session session = HibernateUtil.currentSession();
        List<RencanaAuditTahunanSPI> list = session.createCriteria(RencanaAuditTahunanSPI.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .addOrder(Order.desc("tahun"))
                .list();
        for (RencanaAuditTahunanSPI r : list) {
            Comboitem ci = new Comboitem(r.toString());
            ci.setValue(r);
            cb.appendChild(ci);
        }
    }

    // =====================================================================
    // Internal popup window builder (standalone Tambah, outside SOP flow)
    // =====================================================================

    private void buildFormWindow(final PenugasanAuditSPI item) throws Exception {
        addWindow.setTitle("Penugasan Audit SPI");

        if (item.getDibuatOleh() == null) {
            item.setDibuatOleh(tbmuser);
            item.setTanggalPembuatan(new Date());
        }

        MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");

        Common.clear(addWindow);
        Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
        borderlayout.setParent(addWindow);

        Center center = new Center();
        center.setParent(borderlayout);
        disposisiSop = null;
        center.appendChild(form(item, disposisiSop, save, null));
        ZkCompat.setFlex(center, true);

        South south = new South();
        ZkCompat.setFlex(south, true);
        south.setParent(borderlayout);

        Toolbar toolbar = new Toolbar();
        toolbar.setParent(south);

        MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
        cancel.setTooltiptext("Tutup");
        cancel.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                addWindow.setVisible(false);
            }
        });
        cancel.setParent(toolbar);

        if (item.getId() != null) {
            MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Cetak", "/img/print.png");
            cetak.setTooltiptext("Cetak/unduh laporan (PDF, Excel, dst.)");
            cetak.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    cetak(item);
                }
            });
            cetak.setParent(toolbar);
        }

        save.setTooltiptext("Simpan");
        save.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                if (onSave(e)) {
                    addWindow.setVisible(false);
                    Common.createDefaultTimer(new EventListener() {
                        @Override
                        public void onEvent(Event e2) throws Exception {
                            onSearchDefault(null);
                        }
                    });
                }
            }
        });
        save.setParent(toolbar);

        if (!persetujuan && setujui) {
            save.setVisible(false);
            cancel.setLabel("Tutup");
        }
    }

    // =====================================================================
    // tampilRinci — builds the checklist/finding summary grid for a JenisAuditSPI
    // =====================================================================

    @SuppressWarnings("unchecked")
    public static Grid tampilRinci(JenisAuditSPI jenisAuditSPI, final PenugasanAuditSPI penugasan,
            final boolean editable) throws Exception {

        Grid grid = new Grid();
        grid.setSclass("dgrid");
        grid.setWidth("100%");
        grid.setHeight("100%");

        if (jenisAuditSPI == null || jenisAuditSPI.getId() == null) {
            return grid;
        }
        if (penugasan == null || penugasan.getId() == null) {
            new Html("<div style='padding:14px; color:#94a3b8; font-size:12px;'>"
                    + "Simpan penugasan terlebih dahulu untuk mulai mengisi checklist &amp; temuan.</div>").setParent(grid);
            return grid;
        }

        Columns columns = new Columns();
        columns.setParent(grid);
        MyColumnConfig col = new MyColumnConfig("No.");
        col.setAlign("center");
        col.setWidth("40px");
        col.setParent(columns);
        new MyColumnConfig("Kriteria Audit").setParent(columns);
        new MyColumnConfig("Langkah Uji/Checklist").setParent(columns);
        new MyColumnConfig("Klasifikasi").setParent(columns);
        new MyColumnConfig("").setParent(columns);

        Session session = HibernateUtil.currentSession();
        List<KriteriaAuditSPI> kriteriaList = ConstantValues.simpleList(
                session.createCriteria(KriteriaAuditSPI.class)
                        .add(Restrictions.eq("jenisAuditSPI", jenisAuditSPI))
                        .addOrder(Order.asc("nomorUrut")),
                KriteriaAuditSPI.class);

        Rows rows = new Rows();
        rows.setParent(grid);

        int rowNo = 0;
        int cntKritis = 0, cntMayor = 0, cntMinor = 0, cntObs = 0, cntSesuai = 0, cntBelum = 0;
        Long lastKriteriaId = -1L;

        for (KriteriaAuditSPI kriteria : kriteriaList) {
            if (!kriteria.getAktif()) continue;

            List<ChecklistAuditSPI> checklistList = ConstantValues.simpleList(
                    session.createCriteria(ChecklistAuditSPI.class)
                            .add(Restrictions.eq("kriteriaAuditSPI", kriteria))
                            .addOrder(Order.asc("nomorUrut")),
                    ChecklistAuditSPI.class);

            for (final ChecklistAuditSPI checklist : checklistList) {
                if (!checklist.getAktif()) continue;

                rowNo++;

                TemuanAuditSPI temuan = (TemuanAuditSPI) ConstantValues.simpleObject(
                        session.createCriteria(TemuanAuditSPI.class)
                                .add(Restrictions.eq("checklistAuditSPI", checklist))
                                .add(Restrictions.eq("penugasanAuditSPI", penugasan))
                                .setMaxResults(1),
                        TemuanAuditSPI.class);

                String klas = temuan == null ? null : temuan.getKlasifikasi();
                if (TemuanAuditSPI.KRITIS.equals(klas)) cntKritis++;
                else if (TemuanAuditSPI.MAYOR.equals(klas)) cntMayor++;
                else if (TemuanAuditSPI.MINOR.equals(klas)) cntMinor++;
                else if (TemuanAuditSPI.OBSERVASI.equals(klas)) cntObs++;
                else if (TemuanAuditSPI.SESUAI.equals(klas)) cntSesuai++;
                else cntBelum++;

                MyFormRow row = new MyFormRow();
                row.setValign("top");
                row.setParent(rows);

                row.appendChild(new MyLabelAgakKecil(String.valueOf(rowNo)));

                boolean isNewKriteria = !kriteria.getId().equals(lastKriteriaId);
                row.appendChild(new MyLabelAgakKecil(isNewKriteria ? kriteria.getNama() : ""));
                row.appendChild(new MyLabelAgakKecil(checklist.getNama()));

                if (temuan == null || temuan.getKlasifikasi() == null) {
                    row.appendChild(new MyLabelAgakKecil("Belum diisi"));
                } else {
                    new Html(klasifikasiBadge(temuan.getKlasifikasi())).setParent(row);
                }

                MyToolbarbuttonConfig isiBtn = new MyToolbarbuttonConfig(
                        temuan != null && temuan.getId() != null
                                ? (editable ? "Ubah Temuan" : "Lihat Temuan") : "Isi Temuan",
                        "/img/edit.gif");
                isiBtn.addEventListener("onClick", new EventListener() {
                    @Override
                    public void onEvent(Event e) throws Exception {
                        TemuanAuditSPIAction.openForChecklist(checklist, penugasan, editable, e.getTarget());
                    }
                });
                row.appendChild(isiBtn);

                lastKriteriaId = kriteria.getId();
            }
        }

        if (rowNo > 0) {
            MyFormRow rekapRow = new MyFormRow();
            rekapRow.setStyle("background:#f8fafc;");
            ZkCompat.setSpans(rekapRow, "5");
            rekapRow.setParent(rows);

            String rekapHtml =
                "<div style='padding:8px 12px; font-size:11px; color:#334155; line-height:1.8;'>"
                + "<b>Rekapitulasi Temuan:</b>&nbsp;&nbsp;"
                + "<span style='margin-right:10px;'>&#x1F534; Kritis: <b style='color:#991b1b;'>" + cntKritis + "</b></span>"
                + "<span style='margin-right:10px;'>&#x1F7E0; Mayor: <b style='color:#9a3412;'>" + cntMayor + "</b></span>"
                + "<span style='margin-right:10px;'>&#x1F7E1; Minor: <b style='color:#a06a00;'>" + cntMinor + "</b></span>"
                + "<span style='margin-right:10px;'>&#x1F535; Observasi: <b style='color:#1e40af;'>" + cntObs + "</b></span>"
                + "<span style='margin-right:10px;'>&#x2705; Sesuai: <b style='color:#166534;'>" + cntSesuai + "</b></span>"
                + "<span style='color:#94a3b8;'>Belum diisi: " + cntBelum + "</span>"
                + "&nbsp;&nbsp;|&nbsp;&nbsp;Total checklist: <b>" + rowNo + "</b>"
                + "</div>";
            new Html(rekapHtml).setParent(rekapRow);
        }

        return grid;
    }

    static String klasifikasiBadge(String klas) {
        if (klas == null) return "";
        String bg = TemuanAuditSPI.KRITIS.equals(klas) ? "#fee2e2"
                : TemuanAuditSPI.MAYOR.equals(klas) ? "#ffedd5"
                : TemuanAuditSPI.MINOR.equals(klas) ? "#fef3c7"
                : TemuanAuditSPI.SESUAI.equals(klas) ? "#dcfce7"
                : "#dbeafe";
        String clr = TemuanAuditSPI.KRITIS.equals(klas) ? "#991b1b"
                : TemuanAuditSPI.MAYOR.equals(klas) ? "#9a3412"
                : TemuanAuditSPI.MINOR.equals(klas) ? "#a06a00"
                : TemuanAuditSPI.SESUAI.equals(klas) ? "#166534"
                : "#1e40af";
        String label = TemuanAuditSPI.KLASIFIKASI_DATA.containsKey(klas) ? TemuanAuditSPI.KLASIFIKASI_DATA.get(klas) : klas;
        return "<span style='border-radius:999px; padding:2px 9px; font-size:10px; font-weight:700;"
             + " background:" + bg + "; color:" + clr + ";'>" + label + "</span>";
    }

    // =====================================================================
    // Save
    // =====================================================================

    public boolean onSave(Event event) throws Exception {
        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Judul Penugasan Audit belum diisi."
                    + " Langkah yang dapat dilakukan:"
                    + " (1) isi kolom Judul Penugasan Audit dengan nama atau deskripsi singkat penugasan;"
                    + " (2) pastikan judul tidak kosong dan informatif;"
                    + " (3) ulangi proses simpan."
                    + " Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (jenisAuditSPI.getSelectedItem() == null || jenisAuditSPI.getSelectedItem().getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, Jenis Audit belum dipilih."
                    + " Langkah yang dapat dilakukan:"
                    + " (1) pilih Jenis Audit dari daftar yang tersedia;"
                    + " (2) jika jenis audit yang diinginkan belum ada, tambahkan terlebih dahulu melalui menu Master Jenis Audit SPI;"
                    + " (3) ulangi proses simpan."
                    + " Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return false;
        }
        if (satuanKerja.getAttribute("satuanKerja") == null) {
            MyMessageboxConfig.show("Mohon maaf, Unit Kerja yang Diaudit (Auditee) belum dipilih."
                    + " Langkah yang dapat dilakukan:"
                    + " (1) klik tombol pemilih Unit Kerja dan cari unit yang akan diaudit;"
                    + " (2) pastikan unit kerja yang dituju sudah terdaftar di master data;"
                    + " (3) ulangi proses simpan."
                    + " Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return false;
        }
        if (tanggalMulai.getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, Tanggal Mulai Pelaksanaan Audit belum diisi."
                    + " Langkah yang dapat dilakukan:"
                    + " (1) klik kolom Tanggal Mulai dan pilih tanggal pelaksanaan audit dimulai;"
                    + " (2) pastikan tanggal yang dipilih sesuai dengan rencana penugasan;"
                    + " (3) ulangi proses simpan."
                    + " Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return false;
        }

        Session session = HibernateUtil.currentSession();
        if (penugasanAuditSPI.getId() != null) {
            penugasanAuditSPI = (PenugasanAuditSPI) session.load(PenugasanAuditSPI.class, penugasanAuditSPI.getId());
        }

        if (penugasanAuditSPI.getDibuatOleh() == null) {
            penugasanAuditSPI.setDibuatOleh(tbmuser);
            penugasanAuditSPI.setTanggalPembuatan(new Date());
        }
        if (disposisiSop != null && disposisiSop.getId() != null) {
            penugasanAuditSPI.setDisposisiSop(disposisiSop);
        }

        penugasanAuditSPI.setNama(nama.getValue());
        penugasanAuditSPI.setKeterangan(keterangan.getValue());
        penugasanAuditSPI.setJenisAuditSPI((JenisAuditSPI) jenisAuditSPI.getSelectedItem().getValue());
        penugasanAuditSPI.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
        penugasanAuditSPI.setRencanaAuditTahunanSPI((RencanaAuditTahunanSPI) (rencanaAuditTahunanSPI.getSelectedItem() == null
                ? null : rencanaAuditTahunanSPI.getSelectedItem().getValue()));
        penugasanAuditSPI.setTanggalMulai(tanggalMulai.getValue());
        penugasanAuditSPI.setTanggalSelesai(tanggalSelesai.getValue());

        String sts = statusValue();
        if (sts != null && sts.equals(PenugasanAuditSPI.DISETUJU)) {
            penugasanAuditSPI.setDisetujuiOleh(tbmuser);
            penugasanAuditSPI.setTanggalPersetujuan(tanggalPersetujuanManual.getValue());
        } else {
            penugasanAuditSPI.setDisetujuiOleh(null);
            penugasanAuditSPI.setTanggalPersetujuan(null);
        }
        penugasanAuditSPI.setTanggalPersetujuanManual(tanggalPersetujuanManual.getValue());
        penugasanAuditSPI.setStatus(sts);

        if (penugasanAuditSPI.getId() != null) {
            session.update(penugasanAuditSPI);
        } else {
            penugasanAuditSPI.setDibuatOleh(tbmuser);
            session.save(penugasanAuditSPI);
        }
        session.flush();

        if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
            try {
                Session streamSession = ais.database.hibernate.StreamingHibernateUtil.getInstance().currentSession();
                streamSession.refresh(lainMahasiswa);
                lainMahasiswa.setRef(penugasanAuditSPI.getId());
                streamSession.getTransaction().begin();
                streamSession.update(lainMahasiswa);
                streamSession.getTransaction().commit();
                ais.database.hibernate.StreamingHibernateUtil.getInstance().closeSession();
            } catch (Exception e) {
                ais.database.hibernate.StreamingHibernateUtil.getInstance().rollbackTransaction();
                Common.tampilErrorJikaAdmin(e);
            }
        }

        // Bila baru pertama kali disimpan (belum sempat isi tim/checklist), segarkan
        // panel Tim Audit dan grid checklist supaya keduanya langsung dapat dipakai.
        if (rowTim != null) {
            rebuildTimAuditPanel(!(persetujuan || setujui || viewOnly));
        }
        if (rowDetail != null) {
            Common.clear(rowDetail);
            tampilRinci(penugasanAuditSPI.getJenisAuditSPI(), penugasanAuditSPI,
                    !(persetujuan || setujui || viewOnly)).setParent(rowDetail);
        }

        return true;
    }

    // =====================================================================
    // Criteria & search
    // =====================================================================

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(PenugasanAuditSPI.class)
                .add((start == null || end == null || start.getValue() == null || end.getValue() == null)
                        ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.sqlRestriction("date(this_.tanggal_pembuatan) between date('"
                                + Common.databaseDateFormat.get().format(start.getValue()) + "') and date('"
                                + Common.databaseDateFormat.get().format(end.getValue()) + "')"))
                .add(searchaktif == null || searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"))
                .add(searchnama.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
                .add(searchjenis.getSelectedItem() == null || searchjenis.getSelectedItem().getValue() == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("jenisAuditSPI", searchjenis.getSelectedItem().getValue()))
                .add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("status", searchstatus.getSelectedItem().getValue()))
                .add(searchsatuanKerja.getAttribute("satuanKerja") == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("satuanKerja", searchsatuanKerja.getAttribute("satuanKerja")));

        if (order) criteria.addOrder(Order.desc("id"));
        return criteria;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSearchDefault(Event event) {
        Common.initPaging(initCriteria(false), paging);
        List<PenugasanAuditSPI> data = initCriteria(true)
                .setMaxResults(Common.ROWS_COUNT_ON_PAGE)
                .setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
                .list();
        refreshGridData(data, new PenugasanAuditSPIRenderer());
    }

    // =====================================================================
    // FormSop interface
    // =====================================================================

    @Override
    public String istilah() throws Exception {
        return "Penugasan Audit SPI";
    }

    @Override
    public DataSop ambil() throws Exception {
        return penugasanAuditSPI;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Class ambilClass() throws Exception {
        return PenugasanAuditSPI.class;
    }

    @Override
    public void setPersetujuan(boolean persetujuan) {
        this.persetujuan = persetujuan;
    }

    @Override
    public java.io.File cetakData(GeneralValueObject generalValueObject) throws Exception {
        PenugasanAuditSPI item = (PenugasanAuditSPI) generalValueObject;
        ais.action.report.format1.spi.LaporanPenugasanAuditSPI laporan =
                new ais.action.report.format1.spi.LaporanPenugasanAuditSPI(item);
        return ais.action.report.Report.generateFileReport(ais.action.report.Report.PDF, laporan.generateParameter(),
                "format1/spi/lembar_kerja_penugasan_audit_spi", ais.ui.util.WaktuUtil.getDate(), null, new Toolbar());
    }

    /** Membuka jendela pratinjau/cetak (PDF, Excel, dst.) untuk satu penugasan audit. */
    public static void cetak(PenugasanAuditSPI item) throws Exception {
        ais.action.report.format1.spi.LaporanPenugasanAuditSPI laporan = buildLaporan(item);
        laporan.setParent(org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
        laporan.onModal();
    }

    private static ais.action.report.format1.spi.LaporanPenugasanAuditSPI buildLaporan(PenugasanAuditSPI item) {
        ais.action.report.format1.spi.LaporanPenugasanAuditSPI laporan =
                new ais.action.report.format1.spi.LaporanPenugasanAuditSPI(item);
        laporan.setTitle("Laporan Penugasan Audit SPI");
        laporan.setClosable(true);
        laporan.setHeight("90%");
        laporan.setWidth("900px");
        laporan.setVisible(false);
        return laporan;
    }

    // =====================================================================
    // Internal helpers
    // =====================================================================

    private String statusValue() {
        return (status == null || status.getSelectedItem() == null)
                ? null : (String) status.getSelectedItem().getValue();
    }
}
