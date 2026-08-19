package ais.action.master.spmi;

import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.report.Report;
import ais.action.report.format1.akademik.LaporanHasilSPMI;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.DanaTalangan;
import ais.database.model.akunting.UangMuka;
import ais.database.model.file.LampiranLain;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.spmi.ButirMutuSPMI;
import ais.database.model.spmi.HasilSPMI;
import ais.database.model.spmi.HasilTemuanSPMI;
import ais.database.model.spmi.IndikatorSPMI;
import ais.database.model.spmi.JenisSPMI;
import ais.database.model.spmi.SkenarioSPMI;
import ais.database.model.spmi.StandarSPMI;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
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
import ais.ui.util.WaktuUtil;

public class HasilSPMIAction extends BaseSPMIAction implements FormSop {

    private static final long serialVersionUID = 4124140285573733292L;

    // ---- Search fields (additional to BaseSPMIAction's searchnama / searchaktif) ----
    // Note: 'searchnama' and 'searchaktif' are inherited from BaseSPMIAction.
    // The original used 'serachnama'/'serachjenis' (typos) — corrected here.
    private Combobox searchjenis;
    private Combobox searchstatus;
    private Combobox searchfakultas;
    private Combobox searchjurusan;
    private MyDatebox start;
    private MyDatebox end;

    // ---- Form fields ----
    private Textbox  nama;
    private Textbox  keterangan;
    private Textbox  auditorNama;
    private Textbox  auditeeNama;
    private Combobox jenisSPMI;
    private MyDatebox tanggal;
    private Combobox  fakultas;
    private Combobox  jurusan;
    private Combobox  ta;
    private Combobox  semester;
    private Radiogroup status;
    private MyDatebox  tanggalPersetujuanManual;
    private Row        rowDetail;

    // ---- State ----
    public HasilSPMI hasilSPMI;
    private Tbmuser  tbmuser;
    private boolean  persetujuan = false;
    private boolean  setujui     = false;
    private boolean  viewOnly    = false;
    private PerguruanTinggi  perguruanTinggi;
    private DisposisiSop     disposisiSop;
    protected LampiranLain   lainMahasiswa;

    // Instance field — NOT static to avoid cross-request data corruption
    private Map<Long, HasilTemuanSPMI> hasilTemuanSPMIs;

    // Dashboard tab (autowired from ZUL by id "dasborTab")
    protected org.zkoss.zul.Tabpanel dasborTab;

    public static final String[] contents = new String[]{
            "id", "perguruanTinggi", "fakultas", "jurusan", "disposisiSop",
            "tanggal", "ta", "semester", "nama", "keterangan", "aktif",
            "status", "dibuatOleh", "disetujuiOleh",
            "tanggalPembuatan", "tanggalPersetujuan",
            "auditorNama", "auditeeNama"
    };

    public HasilSPMIAction() {
        tbmuser = Common.getCurrentUser();
    }

    public HasilSPMIAction(boolean persetujuan) {
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

        Common.initFakultasDanJurusan(null, null, searchfakultas, searchjurusan);
        perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

        Common.insertComboDanSemua(searchjenis, "nama", "keterangan",
                JenisSPMI.class, Restrictions.eq("aktif", true));

        if (start != null) start.setReadonly(true);
        if (end != null) end.setReadonly(true);
        Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
        cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) - 6);
        if (start != null) start.setValue(cal.getTime());
        cal = ais.ui.util.WaktuUtil.getCalendar();
        cal.set(Calendar.DATE, cal.get(Calendar.DATE) + 1);
        if (end != null) end.setValue(cal.getTime());

        Comboitem semua = new Comboitem("Semua");
        if (semua != null) { semua.setValue(null); }
        searchstatus.appendChild(semua);
        for (String s : new String[]{HasilSPMI.PENGAJUAN, HasilSPMI.DISETUJU, HasilSPMI.DITOLAK}) {
            Comboitem ci = new Comboitem(s);
            ci.setValue(s);
            searchstatus.appendChild(ci);
        }
        if (searchstatus != null) { searchstatus.setSelectedItem(semua); }
        if (searchstatus != null) { searchstatus.setReadonly(true); }

        if (execution.getParameter("persetujuan") != null) {
            persetujuan = Boolean.parseBoolean(execution.getParameter("persetujuan"));
        }

        initPrivileges();
        if (add != null) {
        add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE) && !persetujuan);
        }

        initPagingListener();

        MyToolbarbuttonConfig cetak = Common.cetakData(HasilSPMI.class, this, contents);
        Common.appendKeToolbar(cetak, add, comp);
        MyToolbarbuttonConfig upload = Common.uploadData(this, HasilSPMI.class, contents);
        if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
        Common.appendKeToolbar(upload, add, comp);

        Common.createDefaultTimer(new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                onSearchDefault(null);
            }
        });

        // Auto-load dashboard on page open (lazy — only if tab exists)
        if (dasborTab != null) {
            Common.createDefaultTimer(new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    onDasbor(null);
                }
            });
        }
    }

    /** Lazy-creates the SPMI dashboard when the Dasbor tab is first selected. */
    public void onDasbor(Event event) throws Exception {
        if (dasborTab != null && dasborTab.getChildren().isEmpty()) {
            DasboardSPMI spmiDasbord = new DasboardSPMI();
            ais.ui.util.BaseDasbordPortal.mountWrapped(spmiDasbord, dasborTab,
                "Dasbor SPMI",
                "Gambaran hasil penjaminan mutu internal (SPMI) secara keseluruhan.");
        }
    }

    // =====================================================================
    // Row renderer
    // =====================================================================

    class HasilSPMIRenderer extends ais.ui.util.MyRowRenderer {
        @Override
        public void render(final Row row, Object obj) throws Exception {
            row.setValign("top");
            final HasilSPMI item = (HasilSPMI) obj;

            Vbox nameVbox;
            (nameVbox = RevisiHelper.createNewRevisi(HasilSPMI.class, item,
                    item.getNama() == null ? "" : item.getNama().trim())).setParent(row);

            Hbox docHbox = new Hbox();
            docHbox.setParent(nameVbox);
            LampiranLain.createDownloadUploadFileLain(docHbox, item.getId(),
                    HasilSPMI.class.getName(), "Dokumen Pengajuan SPMI",
                    false, null, null, false, false, false, true);

            Vbox jenisVbox = new Vbox();
            jenisVbox.setParent(row);
            new Label(item.getJenisSPMI()  == null ? "" : item.getJenisSPMI().getNama()).setParent(jenisVbox);
            new Label(item.getFakultas()   == null ? "" : item.getFakultas().getNama()).setParent(jenisVbox);
            new Label(item.getJurusan()    == null ? "" : item.getJurusan().getNama()).setParent(jenisVbox);

            Vbox taVbox = new Vbox();
            taVbox.setParent(row);
            new Label(item.getTa()).setParent(taVbox);
            new Label(item.getSemester()).setParent(taVbox);

            Vbox pengajuanVbox = new Vbox();
            pengajuanVbox.setParent(row);
            new Label(item.getTanggal() == null ? ""
                    : Common.dateFormat3.get().format(item.getTanggal())).setParent(pengajuanVbox);
            new MyLabelAgakKecil(item.getDibuatOleh() == null ? ""
                    : item.getDibuatOleh().getUserNama()).setParent(pengajuanVbox);

            Vbox persetujuanVbox = new Vbox();
            persetujuanVbox.setParent(row);
            new Label(item.getStatus()).setParent(persetujuanVbox);
            new MyLabelAgakKecil(item.getDisetujuiOleh() == null ? ""
                    : item.getDisetujuiOleh().getUserNama()).setParent(persetujuanVbox);
            new MyLabelAgakKecil(item.getTanggalPersetujuan() == null ? ""
                    : Common.dateFormat3.get().format(item.getTanggalPersetujuan())).setParent(persetujuanVbox);

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
                new Label(ais.common.Common.getBahasaConfig("Tidak aktif")).setParent(row);
            } else if (persetujuan && !item.getStatus().equals(HasilSPMI.DISETUJU)) {
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

            boolean canEdit   = edit   && !persetujuan && !item.getStatus().equals(HasilSPMI.DISETUJU);
            boolean canDelete = delete && !persetujuan && !item.getStatus().equals(HasilSPMI.DISETUJU);
            final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
                    new java.util.ArrayList<org.zkoss.zk.ui.Component>();

            Hbox actionHbox = Common.copyEditDeleteButtons(canEdit, canEdit, canDelete, item, HasilSPMIAction.this);
            aksiButtons.addAll(ais.ui.util.UIHelper.ambilItemAksi(actionHbox));

            MyToolbarbuttonConfig downloadAmi = new MyToolbarbuttonConfig("Unduh AMI", "/img/excel.png");
            downloadAmi.setTooltiptext("Unduh format umum AMI satu XLSX: petunjuk, identitas, satu tabel seluruh indikator, ringkasan, dan referensi pilihan");
            downloadAmi.setOrient("vertical");
            downloadAmi.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    try {
                        byte[] workbook = AmiExcelHelper.exportWorkbook(item);
                        Filedownload.save(workbook, AmiExcelHelper.MIME_XLSX,
                                AmiExcelHelper.fileName(item));
                    } catch (Exception ex) {
                        Common.tampilErrorJikaAdmin(ex);
                        MyMessageboxConfig.show("Format AMI tidak dapat dibuat. " + ex.getMessage()
                                + " Langkah yang dapat dilakukan: (1) pastikan Jenis SPMI sudah dipilih; "
                                + "(2) pastikan master Standar, Indikator, dan Daftar Tilik sudah aktif; "
                                + "(3) coba unduh kembali.", "Peringatan",
                                MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
                    }
                }
            });
            aksiButtons.add(downloadAmi);

            MyToolbarbuttonConfig uploadAmi = new MyToolbarbuttonConfig("Upload AMI", "/img/upload.png");
            uploadAmi.setTooltiptext("Upload format AMI V2 atau format lama V1; ID teknis boleh kosong jika teks indikator dan bukti cocok unik dengan master aktif");
            uploadAmi.setOrient("vertical");
            uploadAmi.setVisible(canEdit);
            uploadAmi.setUpload(Common.ukuranFileUpload());
            uploadAmi.addEventListener("onUpload", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    UploadEvent uploadEvent = (UploadEvent) e;
                    Media media = uploadEvent.getMedia();
                    try {
                        if (media == null || media.getName() == null
                                || !media.getName().toLowerCase().endsWith(".xlsx")) {
                            throw new IllegalArgumentException("File harus berformat Excel Open XML (.xlsx).");
                        }
                        if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media)) return;
                        AmiExcelHelper.ImportResult result = AmiExcelHelper.importWorkbook(item, media.getByteData());
                        Common.refresh(item);
                        MyMessageboxConfig.show(result.message(), "Pemberitahuan",
                                MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
                                new EventListener() {
                                    @Override
                                    public void onEvent(Event event) throws Exception {
                                        onSearchDefault(null);
                                    }
                                });
                    } catch (Exception ex) {
                        Common.tampilErrorJikaAdmin(ex);
                        MyMessageboxConfig.show("Upload format AMI gagal. " + ex.getMessage()
                                + " Tidak ada data yang diproses bila validasi file gagal.", "Peringatan",
                                MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
                    }
                }
            });
            aksiButtons.add(uploadAmi);

            MyToolbarbuttonConfig printBtn = new MyToolbarbuttonConfig("", "/img/print.png");
            printBtn.setTooltiptext("Cetak");
            printBtn.setOrient("vertical");
            printBtn.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    cetak(item);
                }
            });
            aksiButtons.add(printBtn);

            // ── Pelaksanaan SPMI: Sasaran Mutu (PPEPP P-2) ──────────────────
            MyToolbarbuttonConfig sasaranBtn = new MyToolbarbuttonConfig("Sasaran", null);
            sasaranBtn.setTooltiptext("Isi/lihat sasaran mutu pelaksanaan standar (Fase Pelaksanaan PPEPP)");
            sasaranBtn.setOrient("vertical");
            sasaranBtn.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    Long jId = item.getJurusan()   != null ? item.getJurusan().getId()   : null;
                    Long jnId = item.getJenisSPMI() != null ? item.getJenisSPMI().getId() : null;
                    if (jId == null || jnId == null) {
                        MyMessageboxConfig.show("Mohon maaf, Jurusan dan Jenis SPMI belum diisi pada AMI ini. "
                            + "Langkah yang dapat dilakukan: (1) buka form detail AMI dan isi kolom Jurusan serta Jenis SPMI; "
                            + "(2) simpan perubahan pada AMI tersebut; "
                            + "(3) coba buka Sasaran Mutu kembali. "
                            + "Jika masih mengalami kendala, hubungi Administrator atau tim teknis.",
                            "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                        return;
                    }
                    String namaJur   = item.getJurusan().getNama();
                    String namaJenis = item.getJenisSPMI().getNama();
                    boolean ed = edit && !HasilSPMI.DISETUJU.equals(item.getStatus());
                    SasaranMutuSPMIAction.openForHasilSPMI(jId, namaJur,
                        item.getTa(), item.getSemester(), jnId, namaJenis, ed, e.getTarget());
                }
            });
            aksiButtons.add(sasaranBtn);

            // ── Peningkatan SPMI (PPEPP P-5) ────────────────────────────────
            MyToolbarbuttonConfig peningkatanBtn = new MyToolbarbuttonConfig("Peningkatan", null);
            peningkatanBtn.setTooltiptext("Usulkan peningkatan target standar untuk siklus berikutnya (Fase Peningkatan PPEPP)");
            peningkatanBtn.setOrient("vertical");
            peningkatanBtn.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event e) throws Exception {
                    boolean isAdm = edit && delete;
                    PeningkatanSPMIAction.openForHasilSPMI(item, isAdm, e.getTarget());
                }
            });
            aksiButtons.add(peningkatanBtn);

            ais.ui.util.UIHelper.buatBarisAksi(row, 3, aksiButtons);
        }
    }

    // =====================================================================
    // Cetak helpers
    // =====================================================================

    @Override
    public File cetakData(GeneralValueObject generalValueObject) throws Exception {
        HasilSPMI item = (HasilSPMI) generalValueObject;
        LaporanHasilSPMI laporan = buildLaporan(item);
        return Report.generateFileReport(Report.PDF, laporan.generateParameter(),
                "format1/lembar_kerja_ami", ais.ui.util.WaktuUtil.getDate(), null, new Toolbar());
    }

    public static void cetak(HasilSPMI item) throws Exception {
        LaporanHasilSPMI laporan = buildLaporan(item);
        laporan.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
        laporan.onModal();
    }

    private static LaporanHasilSPMI buildLaporan(HasilSPMI item) {
        LaporanHasilSPMI laporan = new LaporanHasilSPMI(item);
        laporan.setTitle("Laporan");
        laporan.setClosable(true);
        laporan.setHeight("90%");
        laporan.setWidth("900px");
        laporan.setVisible(false);
        return laporan;
    }

    // =====================================================================
    // Add / Edit entry points
    // =====================================================================

    public void onAdd(Event event) throws Exception {
        viewOnly = false;
        init(new HasilSPMI());
    }

    @Override
    public void init(GeneralValueObject obj) throws Exception {
        hasilSPMI = (HasilSPMI) obj;
        buildFormWindow(hasilSPMI);
        openAddWindow();
    }

    // =====================================================================
    // FormSop interface — form() builds the detail grid used by SOP workflow
    // =====================================================================

    @SuppressWarnings("deprecation")
    @Override
    public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSopArg,
            final MyToolbarbuttonConfig save, final EventListener setujuiData) throws Exception {

        this.disposisiSop = (this.disposisiSop != null
                && (disposisiSopArg == null || disposisiSopArg.getId() == null))
                        ? this.disposisiSop : disposisiSopArg;
        hasilSPMI = (HasilSPMI) generalValueObject;
        setujui   = false;

        if (hasilSPMI != null && hasilSPMI.getStatus().equals(HasilSPMI.DISETUJU)) {
            setujui = true;
        }

        if (hasilSPMI.getDisposisiSop() != null
                && hasilSPMI.getDisposisiSop().getDisposisiSetuju() != null
                && hasilSPMI.getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null
                && hasilSPMI.getDisposisiSop().getDisposisiSetuju().getSelesai()) {
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

        Tbmuser currentUser = Common.getCurrentUser();

        // -- Judul --
        MyFormRow row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Judul Pengajuan SPMI *"));
        nama = new Textbox(hasilSPMI.getNama());
        row.appendChild((persetujuan || setujui || viewOnly) ? new Label(hasilSPMI.getNama()) : nama);
        nama.setWidth("90%");
        nama.setRows(3);

        // -- Auditor --
        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Nama Auditor/Tim Audit"));
        auditorNama = new Textbox(hasilSPMI.getAuditorNama() == null ? "" : hasilSPMI.getAuditorNama());
        row.appendChild((persetujuan || setujui || viewOnly)
                ? new Label(hasilSPMI.getAuditorNama() == null ? "" : hasilSPMI.getAuditorNama())
                : auditorNama);
        auditorNama.setWidth("90%");
        /* ZK 5.5 belum punya setPlaceholder (baru ada di ZK 6); pakai tooltip. */
        auditorNama.setTooltiptext("Contoh: Dr. Budi Santoso, M.T.");

        // -- Auditee --
        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Nama Auditee (Pihak yang Diaudit)"));
        auditeeNama = new Textbox(hasilSPMI.getAuditeeNama() == null ? "" : hasilSPMI.getAuditeeNama());
        row.appendChild((persetujuan || setujui || viewOnly)
                ? new Label(hasilSPMI.getAuditeeNama() == null ? "" : hasilSPMI.getAuditeeNama())
                : auditeeNama);
        auditeeNama.setWidth("90%");
        auditeeNama.setTooltiptext("Contoh: Kaprodi Sistem Informasi");

        // -- Fakultas / Prodi --
        fakultas = new Combobox();
        jurusan  = new Combobox();
        Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
        Common.selectComboItem(fakultas,
                hasilSPMI.getJurusan() == null ? currentUser.ambilFakultas()
                                               : hasilSPMI.getJurusan().getFakultas());
        row.appendChild((persetujuan || setujui || viewOnly)
                ? new Label(hasilSPMI.getFakultas() == null ? "" : hasilSPMI.getFakultas().getNama())
                : fakultas);
        fakultas.setWidth("90%");

        if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
            Common.insertCombo(jurusan, new String[]{"nama", "kodeEpsbed"}, "jenjang", Jurusan.class,
                    Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
                    CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
        }

        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
        Common.pilihJurusan(jurusan,
                hasilSPMI.getJurusan() == null ? currentUser.ambilJurusan() : hasilSPMI.getJurusan());
        row.appendChild((persetujuan || setujui || viewOnly)
                ? new Label(hasilSPMI.getJurusan() == null ? "" : hasilSPMI.getJurusan().getNama())
                : jurusan);
        jurusan.setWidth("90%");

        // -- Tahun Akademik --
        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
        row.appendChild(ta = new Combobox());
        Common.generateTahunAjaranDanSemua(ta);
        Common.selectComboItem(ta, hasilSPMI.getTa());
        ta.setReadonly(true);

        // -- Semester --
        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
        row.appendChild(semester = new Combobox());
        Comboitem smtGanjil = new org.zkoss.zul.Comboitem();
        smtGanjil.setValue(Perkuliahan.GANJIL);
        smtGanjil.setLabel(Perkuliahan.GANJIL);
        semester.appendChild(smtGanjil);
        Comboitem smtGenap = new MyComboitemConfig();
        smtGenap.setValue(Perkuliahan.GENAP);
        smtGenap.setLabel(Perkuliahan.GENAP);
        semester.appendChild(smtGenap);
        Common.selectComboItem(semester, hasilSPMI.getSemester());
        semester.setReadonly(true);

        // -- Jenis SPMI --
        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pengajuan SPMI *"));
        row.appendChild(jenisSPMI = new Combobox());
        Common.insertCombo(jenisSPMI, "nama", "keterangan",
                JenisSPMI.class, Restrictions.eq("aktif", true));
        jenisSPMI.setReadonly(true);
        Common.selectComboItem(true, jenisSPMI, hasilSPMI.getJenisSPMI());

        // -- Tanggal Pengajuan --
        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pengajuan SPMI *"));
        Hbox tanggalHbox = new Hbox();
        row.appendChild(tanggalHbox);
        tanggal = new MyDatebox(hasilSPMI.getTanggal());
        tanggal.setFormat(Common.dateFormat3.get().toPattern());
        if (persetujuan || setujui || viewOnly) {
            tanggalHbox.appendChild(new Label(Common.dateFormat6.get().format(hasilSPMI.getTanggal())));
        } else {
            tanggal.setParent(tanggalHbox);
        }
        tanggal.setReadonly(true);

        // -- Dokumen --
        lainMahasiswa = null;
        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Dokumen Pengajuan SPMI"));
        Hbox docHbox = new Hbox();
        LampiranLain.createDownloadUploadFileLain(docHbox, hasilSPMI.getId(),
                HasilSPMI.class.getName(), "Dokumen Pengajuan SPMI", false,
                new EventListener() {
                    @Override
                    public void onEvent(Event e) throws Exception {
                        lainMahasiswa = (LampiranLain) e.getData();
                    }
                }, null, false, false, false, !(persetujuan || setujui || viewOnly));
        docHbox.setParent(row);
        Common.initKeterangan(rows,
                "Jika file dokumen pengajuan SPMI lebih dari satu file, zip dulu semua file tersebut");

        // -- Keterangan --
        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
        keterangan = new Textbox(hasilSPMI.getKeterangan() == null ? "" : hasilSPMI.getKeterangan());
        row.appendChild(setujui ? new Label(hasilSPMI.getKeterangan() == null ? "" : hasilSPMI.getKeterangan())
                                : keterangan);
        keterangan.setWidth("90%");
        keterangan.setRows(2);

        // -- Detail grid row --
        rowDetail = new MyFormRow();
        ais.ui.util.ZkCompat.setSpans(rowDetail, "2");
        rowDetail.setParent(rows);

        // When Jenis changes: refresh the detail temuan grid
        EventListener jenisChangeListener = new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                Tbmuser u = Common.getCurrentUser();
                JenisSPMI work = (JenisSPMI) (jenisSPMI.getSelectedItem() == null ? null
                        : jenisSPMI.getSelectedItem().getValue());
                hasilSPMI.setJenisSPMI(work);
                hasilSPMI.setNama(nama.getValue());
                hasilSPMI.setKeterangan(keterangan.getValue());
                hasilSPMI.setTanggal(tanggal.getValue());

                String sts = statusValue();
                if (sts != null && sts.equals(DanaTalangan.DISETUJU)) {
                    hasilSPMI.setDisetujuiOleh(u);
                    hasilSPMI.setTanggalPersetujuan(tanggalPersetujuanManual.getValue());
                } else {
                    hasilSPMI.setDisetujuiOleh(null);
                    hasilSPMI.setTanggalPersetujuan(null);
                }
                hasilSPMI.setStatus(sts);
                Common.clear(rowDetail);
                tampilRinci(work, hasilSPMI, !(persetujuan || setujui || viewOnly)).setParent(rowDetail);
            }
        };
        jenisSPMI.addEventListener("onChange", jenisChangeListener);

        // -- Status Pengajuan (for approver) --
        row = new MyFormRow();
        row.setVisible(persetujuan && !viewOnly && (disposisiSop == null || disposisiSop.getId() == null));
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Status Pengajuan"));
        status = new Radiogroup();
        Radio rPengajuan = new Radio(UangMuka.PENGAJUAN);
        rPengajuan.setAttribute("value", UangMuka.PENGAJUAN);
        rPengajuan.setValue(UangMuka.PENGAJUAN);
        rPengajuan.setVisible(false);
        status.appendChild(rPengajuan);
        Radio rSetuju = new Radio(UangMuka.DISETUJU);
        rSetuju.setAttribute("value", UangMuka.DISETUJU);
        rSetuju.setValue(UangMuka.DISETUJU);
        status.appendChild(rSetuju);
        Radio rTolak = new Radio(UangMuka.DITOLAK);
        rTolak.setAttribute("value", UangMuka.DITOLAK);
        rTolak.setValue(UangMuka.DITOLAK);
        status.appendChild(rTolak);
        status.setWidth("90%");
        Common.selectRadioItem(status, hasilSPMI.getStatus());
        row.appendChild(status);

        grid.setAttribute("eventListenerSetuju", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                if (e != null && e.getTarget() instanceof Checkbox) {
                    Checkbox cb = (Checkbox) e.getTarget();
                    Boolean selesai = (Boolean) cb.getAttribute("checkbox");
                    if (selesai != null && selesai) {
                        Common.selectRadioItem(status, UangMuka.DISETUJU);
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
                    setujuiData.onEvent(new Event("", null, hasilSPMI.getStatus().equals(UangMuka.DISETUJU)));
                }
            });
        }

        if (setujui) {
            row = new MyFormRow();
            row.setParent(rows);
            row.appendChild(new ais.ui.util.MyLabelConfig("Status Pengajuan"));
            row.appendChild(new ais.ui.util.MyLabelConfig(hasilSPMI.getStatus()));
        }

        // -- Tanggal Persetujuan --
        row = new MyFormRow();
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Persetujuan"));
        tanggalPersetujuanManual = new MyDatebox(hasilSPMI.getTanggalPersetujuanManual());
        row.appendChild(new Label(Common.dateFormat1.get().format(
                hasilSPMI.getTanggalPersetujuanManual() == null
                        ? WaktuUtil.getDate() : hasilSPMI.getTanggalPersetujuanManual())));
        tanggalPersetujuanManual.setReadonly(true);
        tanggalPersetujuanManual.addEventListener("onChange", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                if (hasilSPMI != null && hasilSPMI.getId() != null) {
                    hasilSPMI.setTanggalPersetujuanManual(tanggalPersetujuanManual.getValue());
                    Common.refreshUpdate(hasilSPMI);
                }
            }
        });

        jenisChangeListener.onEvent(null);

        // Save button label changes based on approval status
        EventListener saveLabelListener = new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                boolean disetujui = status.getSelectedItem() != null
                        && status.getSelectedItem().getValue().equals(HasilSPMI.DISETUJU);
                if (tanggalPersetujuanManual != null && tanggalPersetujuanManual.getParent() != null) {
                    if (tanggalPersetujuanManual.getValue() == null) {
                        tanggalPersetujuanManual.setValue(WaktuUtil.getDate());
                    }
                    tanggalPersetujuanManual.getParent().setVisible(disetujui);
                }
                if (disetujui) {
                    save.setLabel("Selesaikan dan Setujui Pengajuan SPMI");
                } else {
                    save.setLabel(!persetujuan ? "Simpan dan Cetak" : "Ubah Status Persetujuan dan Cetak");
                }
            }
        };
        status.addEventListener("onClick", saveLabelListener);
        Common.createDefaultTimer(saveLabelListener);

        return grid;
    }

    // =====================================================================
    // Internal popup window builder
    // =====================================================================

    private void buildFormWindow(final HasilSPMI item) throws Exception {
        addWindow.setTitle("Lembar Kerja AMI SPMI");

        if (item.getDibuatOleh() == null) {
            item.setDibuatOleh(tbmuser);
            item.setTanggalPembuatan(new Date());
        }

        MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan dan Cetak", "/img/save.gif");

        Common.clear(addWindow);
        Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
        borderlayout.setParent(addWindow);

        Center center = new Center();
        center.setParent(borderlayout);
        disposisiSop = null;
        center.appendChild(form(item, disposisiSop, save, null));
        ais.ui.util.ZkCompat.setFlex(center, true);

        South south = new South();
        ais.ui.util.ZkCompat.setFlex(south, true);
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
    // tampilRinci — builds the audit finding grid for a JenisSPMI
    // =====================================================================

    @SuppressWarnings("unchecked")
    public static Grid tampilRinci(JenisSPMI jenisSPMI, final HasilSPMI hasilSPMI,
            final boolean editable) throws Exception {

        if (jenisSPMI == null || jenisSPMI.getId() == null) {
            return new Grid();
        }

        Grid grid = new Grid();
        grid.setSclass("dgrid");
        grid.setWidth("100%");
        grid.setHeight("100%");

        Columns columns = new Columns();
        columns.setParent(grid);

        MyColumnConfig col = new MyColumnConfig("No.");
        col.setAlign("center");
        col.setWidth("40px");
        col.setParent(columns);
        new MyColumnConfig("Standar SPMI/Referensi Eksternal").setParent(columns);
        new MyColumnConfig("Pernyataan Ayat Standar/Butir Mutu").setParent(columns);
        new MyColumnConfig("Indikator").setParent(columns);
        new MyColumnConfig("Daftar Tilik/Skenario Pertanyaan/Bukti yang akan diperiksa").setParent(columns);
        new MyColumnConfig("Status Kesiapan Bukti (Auditee)").setParent(columns);
        new MyColumnConfig("Bukti/Link Dokumen (Auditee)").setParent(columns);
        new MyColumnConfig("Catatan Auditee").setParent(columns);
        new MyColumnConfig("Hasil Temuan Audit/Visitasi Lapangan").setParent(columns);
        new MyColumnConfig("Status Temuan\n(O/KTS MYR/KTS MNR/S/LS)*").setParent(columns);
        new MyColumnConfig("Skor AMI\n(1/0)").setParent(columns);
        new MyColumnConfig("Catatan Khusus").setParent(columns);
        new MyColumnConfig("Rekomendasi Auditor").setParent(columns);
        new MyColumnConfig("Tindak Lanjut").setParent(columns);

        Session session = HibernateUtil.currentSession();
        List<StandarSPMI> standarList = ConstantValues.simpleList(
                session.createCriteria(StandarSPMI.class)
                        .add(Restrictions.eq("jenisSPMI", jenisSPMI))
                        .addOrder(Order.asc("nomorUrut")),
                StandarSPMI.class);

        // Instance-local map for this render pass (NOT static!)
        final Map<Long, HasilTemuanSPMI> temuanMap = new HashMap<Long, HasilTemuanSPMI>();

        Rows rows = new Rows();
        rows.setParent(grid);

        // Sequential NO counter and status rekap counters
        int rowNo = 0;
        int cntS = 0, cntKtsMnr = 0, cntKtsMyr = 0, cntO = 0, cntLs = 0, cntBelum = 0;
        int cntBuktiTersedia = 0, cntBuktiSebagian = 0, cntBuktiBelum = 0, cntBuktiKosong = 0;
        Long lastStandarId = -1L;

        for (StandarSPMI standar : standarList) {
            if (!standar.getAktif()) continue;

            List<ButirMutuSPMI> butirList = ConstantValues.simpleList(
                    session.createCriteria(ButirMutuSPMI.class)
                            .add(Restrictions.eq("standarSPMI", standar))
                            .addOrder(Order.asc("nomorUrut")),
                    ButirMutuSPMI.class);

            for (ButirMutuSPMI butir : butirList) {
                if (!butir.getAktif()) continue;

                List<IndikatorSPMI> indikatorList = ConstantValues.simpleList(
                        session.createCriteria(IndikatorSPMI.class)
                                .add(Restrictions.eq("butirMutuSPMI", butir))
                                .addOrder(Order.asc("nomorUrut")),
                        IndikatorSPMI.class);

                for (IndikatorSPMI indikator : indikatorList) {
                    if (!indikator.getAktif()) continue;

                    List<SkenarioSPMI> skenarioList = ConstantValues.simpleList(
                            session.createCriteria(SkenarioSPMI.class)
                                    .add(Restrictions.eq("indikatorSPMI", indikator))
                                    .addOrder(Order.asc("nomorUrut")),
                            SkenarioSPMI.class);

                    for (final SkenarioSPMI skenario : skenarioList) {
                        if (!skenario.getAktif()) continue;

                        rowNo++; // sequential per skenario row

                        HasilTemuanSPMI temuanTemp = (hasilSPMI == null || hasilSPMI.getId() == null) ? null
                                : (HasilTemuanSPMI) ConstantValues.simpleObject(
                                        session.createCriteria(HasilTemuanSPMI.class)
                                                .add(Restrictions.eq("skenarioSPMI", skenario))
                                                .add(Restrictions.eq("hasilSPMI", hasilSPMI))
                                                .setMaxResults(1),
                                        HasilTemuanSPMI.class);

                        final HasilTemuanSPMI temuan = (temuanTemp == null)
                                ? new HasilTemuanSPMI(skenario, hasilSPMI) : temuanTemp;

                        // Accumulate status counts for rekap
                        String st0 = temuan.getStatus();
                        if (HasilTemuanSPMI.KTS_MYR1.equals(st0))      cntKtsMyr++;
                        else if (HasilTemuanSPMI.KTS_MNR1.equals(st0)) cntKtsMnr++;
                        else if (HasilTemuanSPMI.S1.equals(st0))        cntS++;
                        else if (HasilTemuanSPMI.LS1.equals(st0))       cntLs++;
                        else if (HasilTemuanSPMI.O1.equals(st0))        cntO++;
                        else                                             cntBelum++;

                        String kesiapan0 = temuan.getStatusKesiapanBukti();
                        if (HasilTemuanSPMI.BUKTI_TERSEDIA.equals(kesiapan0)) cntBuktiTersedia++;
                        else if (HasilTemuanSPMI.BUKTI_SEBAGIAN.equals(kesiapan0)) cntBuktiSebagian++;
                        else if (HasilTemuanSPMI.BUKTI_BELUM_TERSEDIA.equals(kesiapan0)) cntBuktiBelum++;
                        else cntBuktiKosong++;

                        MyFormRow row = new MyFormRow();
                        row.setValign("top");
                        row.setParent(rows);

                        // NO column: sequential 1,2,3...
                        row.appendChild(new MyLabelAgakKecil(String.valueOf(rowNo)));

                        // Standar column: show name only on first appearance per standar
                        boolean isNewStandar = !standar.getId().equals(lastStandarId);
                        row.appendChild(new MyLabelAgakKecil(isNewStandar ? standar.getNama() : ""));

                        row.appendChild(new MyLabelAgakKecil(butir.getNama()));
                        row.appendChild(new MyLabelAgakKecil(indikator.getNama()));
                        row.appendChild(new MyLabelAgakKecil(skenario.getNama()));

                        final Combobox kesiapanInput = new Combobox();
                        final Textbox buktiInput = new Textbox(temuan.getBuktiAuditee());
                        final Textbox catatanAuditeeInput = new Textbox(temuan.getCatatanAuditee());
                        final Textbox hasilInput   = new Textbox(temuan.getNama());
                        final Combobox statusInput = new Combobox();
                        final Textbox catatanInput = new Textbox(temuan.getKeterangan());
                        final Textbox rekomendasiInput = new Textbox(temuan.getRekomendasi());

                        for (String key : HasilTemuanSPMI.statusKesiapanBuktiData.keySet()) {
                            Comboitem ci = new Comboitem(HasilTemuanSPMI.statusKesiapanBuktiData.get(key));
                            ci.setValue(key);
                            kesiapanInput.appendChild(ci);
                        }
                        Comboitem kesiapanKosong = new Comboitem("Belum diisi");
                        kesiapanKosong.setValue(null);
                        kesiapanInput.appendChild(kesiapanKosong);
                        Common.selectComboItem(kesiapanInput, temuan.getStatusKesiapanBukti());

                        for (String key : HasilTemuanSPMI.statusData.keySet()) {
                            Comboitem ci = new Comboitem(HasilTemuanSPMI.statusData.get(key));
                            ci.setValue(key);
                            statusInput.appendChild(ci);
                        }
                        Comboitem belum = new Comboitem("Belum ditentukan");
                        belum.setValue(null);
                        statusInput.appendChild(belum);
                        Common.selectComboItem(statusInput, temuan.getStatus());

                        EventListener saveTemuan = new EventListener() {
                            @Override
                            public void onEvent(Event e) throws Exception {
                                temuan.setStatusKesiapanBukti(kesiapanInput.getSelectedItem() == null ? null
                                        : (String) kesiapanInput.getSelectedItem().getValue());
                                temuan.setBuktiAuditee(buktiInput.getValue().trim());
                                temuan.setCatatanAuditee(catatanAuditeeInput.getValue().trim());
                                temuan.setNama(hasilInput.getValue().trim());
                                temuan.setStatus(statusInput.getSelectedItem() == null ? null
                                        : (String) statusInput.getSelectedItem().getValue());
                                temuan.setKeterangan(catatanInput.getValue().trim());
                                temuan.setRekomendasi(rekomendasiInput.getValue().trim());
                                temuan.setHasilSPMI(hasilSPMI);
                                temuan.setSkenarioSPMI(skenario);
                                if (hasilSPMI != null && hasilSPMI.getId() != null) {
                                    Common.refreshSaveOrUpdate(temuan);
                                }
                                temuanMap.put(skenario.getId(), temuan);
                            }
                        };

                        temuanMap.put(skenario.getId(), temuan);

                        if (editable) row.appendChild(kesiapanInput);
                        else row.appendChild(new MyLabelAgakKecil(temuan.getStatusKesiapanBukti()));
                        kesiapanInput.setWidth("95%");
                        kesiapanInput.setReadonly(true);

                        if (editable) row.appendChild(buktiInput);
                        else row.appendChild(new MyLabelAgakKecil(temuan.getBuktiAuditee()));
                        buktiInput.setWidth("95%");
                        buktiInput.setRows(3);

                        if (editable) row.appendChild(catatanAuditeeInput);
                        else row.appendChild(new MyLabelAgakKecil(temuan.getCatatanAuditee()));
                        catatanAuditeeInput.setWidth("95%");
                        catatanAuditeeInput.setRows(3);

                        if (editable) {
                            row.appendChild(hasilInput);
                        } else {
                            row.appendChild(new MyLabelAgakKecil(temuan.getNama()));
                        }
                        hasilInput.setWidth("95%");
                        hasilInput.setRows(3);

                        if (editable) {
                            row.appendChild(statusInput);
                        } else {
                            // Color-coded status badge in view mode
                            String stKey = temuan.getStatus();
                            if (stKey == null || stKey.trim().isEmpty()) {
                                row.appendChild(new MyLabelAgakKecil(""));
                            } else {
                                new Html(TindakLanjutSPMIAction.statusBadge(stKey)).setParent(row);
                            }
                        }
                        statusInput.setWidth("95%");
                        statusInput.setReadonly(true);

                        Integer skorAmi = temuan.getSkorAmi();
                        final MyLabelAgakKecil skorAmiOutput = new MyLabelAgakKecil(
                                skorAmi == null ? "" : String.valueOf(skorAmi));
                        row.appendChild(skorAmiOutput);

                        if (editable) {
                            row.appendChild(catatanInput);
                        } else {
                            row.appendChild(new MyLabelAgakKecil(temuan.getKeterangan()));
                        }
                        catatanInput.setWidth("95%");
                        catatanInput.setRows(3);

                        if (editable) row.appendChild(rekomendasiInput);
                        else row.appendChild(new MyLabelAgakKecil(temuan.getRekomendasi()));
                        rekomendasiInput.setWidth("95%");
                        rekomendasiInput.setRows(3);

                        kesiapanInput.addEventListener("onChange", saveTemuan);
                        buktiInput.addEventListener("onChange", saveTemuan);
                        catatanAuditeeInput.addEventListener("onChange", saveTemuan);
                        hasilInput.addEventListener("onChange", saveTemuan);
                        statusInput.addEventListener("onChange", saveTemuan);
                        statusInput.addEventListener("onChange", new EventListener() {
                            @Override
                            public void onEvent(Event e) throws Exception {
                                Integer skor = temuan.getSkorAmi();
                                skorAmiOutput.setValue(skor == null ? "" : String.valueOf(skor));
                            }
                        });
                        catatanInput.addEventListener("onChange", saveTemuan);
                        rekomendasiInput.addEventListener("onChange", saveTemuan);

                        // Kolom Tindak Lanjut — muncul jika temuan sudah tersimpan
                        final Hbox tlHbox = new Hbox();
                        if (temuan.getId() != null) {
                            boolean isKts = HasilTemuanSPMI.KTS_MYR1.equals(temuan.getStatus())
                                         || HasilTemuanSPMI.KTS_MNR1.equals(temuan.getStatus());
                            MyToolbarbuttonConfig tlBtn = new MyToolbarbuttonConfig(
                                    isKts ? "Tindak Lanjut" : "Pantau TL", "/img/edit.gif");
                            tlBtn.setTooltiptext("Kelola tindak lanjut atas temuan ini (fase Pengendalian PPEPP)");
                            tlBtn.addEventListener("onClick", new EventListener() {
                                @Override
                                public void onEvent(Event e) throws Exception {
                                    HasilTemuanSPMI t = (HasilTemuanSPMI) HibernateUtil.currentSession()
                                            .load(HasilTemuanSPMI.class, temuan.getId());
                                    TindakLanjutSPMIAction.openForTemuan(t, e.getTarget());
                                }
                            });
                            tlBtn.setParent(tlHbox);
                        }
                        row.appendChild(tlHbox);

                        lastStandarId = standar.getId();
                    }
                }
            }
        }

        // --- Rekap / summary row at the bottom of the grid ---
        if (rowNo > 0) {
            int cntTerisi = cntS + cntKtsMnr + cntKtsMyr + cntO + cntLs;
            MyFormRow rekapRow = new MyFormRow();
            rekapRow.setStyle("background:#f8fafc;");
            ais.ui.util.ZkCompat.setSpans(rekapRow, "14");
            rekapRow.setParent(rows);

            String rekapHtml =
                "<div style='padding:8px 12px; font-size:11px; color:#334155; line-height:1.8;'>"
                + "<b>Rekapitulasi Temuan AMI:</b>&nbsp;&nbsp;"
                + "<span style='margin-right:10px;'>&#x2705; S (Sesuai): <b style='color:#166534;'>" + cntS + "</b></span>"
                + "<span style='margin-right:10px;'>&#x1F7E1; KTS MNR: <b style='color:#9a3412;'>" + cntKtsMnr + "</b></span>"
                + "<span style='margin-right:10px;'>&#x1F534; KTS MYR: <b style='color:#991b1b;'>" + cntKtsMyr + "</b></span>"
                + "<span style='margin-right:10px;'>&#x1F535; O (Observasi): <b style='color:#1e40af;'>" + cntO + "</b></span>"
                + "<span style='margin-right:10px;'>&#x1F49A; LS (Melebihi Standar): <b style='color:#064e3b;'>" + cntLs + "</b></span>"
                + "<span style='color:#94a3b8;'>Belum diisi: " + cntBelum + "</span>"
                + "&nbsp;&nbsp;|&nbsp;&nbsp;Total skenario: <b>" + rowNo + "</b>"
                + "&nbsp;&nbsp;|&nbsp;&nbsp;Terisi: <b>" + cntTerisi + "/" + rowNo + "</b>"
                + (rowNo > 0 ? " (<b>" + (cntTerisi * 100 / rowNo) + "%</b>)" : "")
                + "<br><b>Skor AMI 2026:</b>&nbsp;&nbsp;Memenuhi: <b style='color:#166534;'>" + (cntS + cntLs) + "</b>"
                + "&nbsp;&nbsp;|&nbsp;&nbsp;Tidak memenuhi: <b style='color:#991b1b;'>" + (cntKtsMnr + cntKtsMyr + cntO) + "</b>"
                + "&nbsp;&nbsp;|&nbsp;&nbsp;Capaian: <b>" + ((cntS + cntLs) * 100 / rowNo) + "%</b>"
                + "<br><b>Kesiapan Bukti:</b>&nbsp;&nbsp;Tersedia: <b style='color:#166534;'>" + cntBuktiTersedia + "</b>"
                + "&nbsp;&nbsp;|&nbsp;&nbsp;Sebagian: <b style='color:#9a3412;'>" + cntBuktiSebagian + "</b>"
                + "&nbsp;&nbsp;|&nbsp;&nbsp;Belum tersedia: <b style='color:#991b1b;'>" + cntBuktiBelum + "</b>"
                + "&nbsp;&nbsp;|&nbsp;&nbsp;Belum diisi: " + cntBuktiKosong
                + "&nbsp;&nbsp;|&nbsp;&nbsp;Kesiapan: <b>" + (cntBuktiTersedia * 100 / rowNo) + "%</b>"
                + "</div>";
            new Html(rekapHtml).setParent(rekapRow);
        }

        // Expose the local map via a grid attribute so onSave can retrieve it
        grid.setAttribute("temuanMap", temuanMap);
        return grid;
    }

    // =====================================================================
    // Save
    // =====================================================================

    public boolean onSave(Event event) throws Exception {
        JenisSPMI jenisSel = (JenisSPMI) (jenisSPMI.getSelectedItem() == null ? null
                : jenisSPMI.getSelectedItem().getValue());

        if (nama.getValue().trim().isEmpty()) {
            MyMessageboxConfig.show("Mohon maaf, Judul Pengajuan SPMI belum diisi. "
                    + "Langkah yang dapat dilakukan: (1) isi kolom Judul Pengajuan SPMI pada form; "
                    + "(2) pastikan teks tidak kosong atau hanya berisi spasi; "
                    + "(3) ulangi proses simpan. "
                    + "Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (ta.getSelectedItem() == null || ta.getSelectedItem().getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, Tahun Akademik Pengajuan SPMI belum dipilih. "
                    + "Langkah yang dapat dilakukan: (1) pilih Tahun Akademik dari daftar pilihan; "
                    + "(2) pastikan daftar Tahun Akademik sudah termuat; "
                    + "(3) ulangi proses simpan. "
                    + "Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (semester.getSelectedItem() == null || semester.getSelectedItem().getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, Semester Pengajuan SPMI belum dipilih. "
                    + "Langkah yang dapat dilakukan: (1) pilih Semester dari daftar pilihan; "
                    + "(2) pastikan Tahun Akademik sudah dipilih sebelum memilih Semester; "
                    + "(3) ulangi proses simpan. "
                    + "Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return false;
        }
        if (jenisSel == null) {
            MyMessageboxConfig.show("Mohon maaf, Jenis Pengajuan SPMI belum dipilih. "
                    + "Langkah yang dapat dilakukan: (1) pilih Jenis Pengajuan SPMI dari daftar pilihan; "
                    + "(2) pastikan daftar Jenis SPMI sudah memuat data; "
                    + "(3) ulangi proses simpan. "
                    + "Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return false;
        }
        if (tanggal.getValue() == null) {
            MyMessageboxConfig.show("Mohon maaf, Tanggal Pengajuan SPMI belum diisi. "
                    + "Langkah yang dapat dilakukan: (1) isi kolom Tanggal Pengajuan pada form; "
                    + "(2) pastikan format tanggal sudah benar; "
                    + "(3) ulangi proses simpan. "
                    + "Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            return false;
        }

        Session session = HibernateUtil.currentSession();
        if (hasilSPMI.getId() != null) {
            hasilSPMI = (HasilSPMI) session.load(HasilSPMI.class, hasilSPMI.getId());
        }

        if (hasilSPMI.getDibuatOleh() == null) {
            hasilSPMI.setDibuatOleh(tbmuser);
            hasilSPMI.setTanggalPembuatan(new Date());
        }
        if (disposisiSop != null && disposisiSop.getId() != null) {
            hasilSPMI.setDisposisiSop(disposisiSop);
        }

        hasilSPMI.setJenisSPMI(jenisSel);
        hasilSPMI.setNama(nama.getValue());
        hasilSPMI.setKeterangan(keterangan.getValue());
        hasilSPMI.setTanggal(tanggal.getValue());
        hasilSPMI.setPerguruanTinggi(perguruanTinggi);
        hasilSPMI.setFakultas((Fakultas) (fakultas.getSelectedItem() == null ? null
                : fakultas.getSelectedItem().getValue()));
        hasilSPMI.setJurusan((Jurusan) (jurusan.getSelectedItem() == null ? null
                : jurusan.getSelectedItem().getValue()));
        hasilSPMI.setTa((String) ta.getSelectedItem().getValue());
        hasilSPMI.setSemester((String) semester.getSelectedItem().getValue());

        String sts = statusValue();
        if (sts != null && sts.equals(DanaTalangan.DISETUJU)) {
            hasilSPMI.setDisetujuiOleh(tbmuser);
            hasilSPMI.setTanggalPersetujuan(tanggalPersetujuanManual.getValue());
        } else {
            hasilSPMI.setDisetujuiOleh(null);
            hasilSPMI.setTanggalPersetujuan(null);
        }
        hasilSPMI.setTanggalPersetujuanManual(tanggalPersetujuanManual.getValue());
        hasilSPMI.setStatus(sts);
        hasilSPMI.setAuditorNama(auditorNama.getValue().trim().isEmpty() ? null : auditorNama.getValue().trim());
        hasilSPMI.setAuditeeNama(auditeeNama.getValue().trim().isEmpty() ? null : auditeeNama.getValue().trim());

        if (hasilSPMI.getId() != null) {
            session.update(hasilSPMI);
        } else {
            hasilSPMI.setDibuatOleh(tbmuser);
            session.save(hasilSPMI);
        }
        session.flush();

        // Retrieve temuanMap from the Grid rendered inside rowDetail (for new records
        // where saveTemuan listeners couldn't persist directly due to missing hasilSPMI.id)
        if (hasilTemuanSPMIs == null && rowDetail != null) {
            /* ZK 5.5: getChildren() mengembalikan List mentah (tanpa generics),
             * jadi iterasi harus lewat Object lalu di-cast manual. */
            for (Object childObj : rowDetail.getChildren()) {
                org.zkoss.zk.ui.Component child = (org.zkoss.zk.ui.Component) childObj;
                if (child instanceof org.zkoss.zul.Grid) {
                    @SuppressWarnings("unchecked")
                    Map<Long, HasilTemuanSPMI> m =
                            (Map<Long, HasilTemuanSPMI>) child.getAttribute("temuanMap");
                    if (m != null) { hasilTemuanSPMIs = m; }
                    break;
                }
            }
        }

        // Persist temuan data — skip new empty rows (no finding + no status)
        if (hasilTemuanSPMIs != null) {
            for (HasilTemuanSPMI temuan : hasilTemuanSPMIs.values()) {
                boolean isNew    = temuan.getId() == null;
                boolean hasNama  = temuan.getNama() != null && !temuan.getNama().trim().isEmpty();
                boolean hasSt    = temuan.getStatus() != null;
                boolean hasCat   = temuan.getKeterangan() != null && !temuan.getKeterangan().trim().isEmpty();
                boolean hasBukti = temuan.getBuktiAuditee() != null && !temuan.getBuktiAuditee().trim().isEmpty();
                boolean hasKesiapan = temuan.getStatusKesiapanBukti() != null;
                boolean hasCatAuditee = temuan.getCatatanAuditee() != null
                        && !temuan.getCatatanAuditee().trim().isEmpty();
                boolean hasRekomendasi = temuan.getRekomendasi() != null
                        && !temuan.getRekomendasi().trim().isEmpty();
                if (isNew && !hasNama && !hasSt && !hasCat && !hasBukti && !hasKesiapan
                        && !hasCatAuditee && !hasRekomendasi) continue; // nothing entered yet
                temuan.setHasilSPMI(hasilSPMI);
                Common.refreshSaveOrUpdate(session, temuan);
            }
        }

        // Persist lampiran reference
        if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
            try {
                Session streamSession = StreamingHibernateUtil.getInstance().currentSession();
                streamSession.refresh(lainMahasiswa);
                lainMahasiswa.setRef(hasilSPMI.getId());
                streamSession.getTransaction().begin();
                streamSession.update(lainMahasiswa);
                streamSession.getTransaction().commit();
                StreamingHibernateUtil.getInstance().closeSession();
            } catch (Exception e) {
                StreamingHibernateUtil.getInstance().rollbackTransaction();
                Common.tampilErrorJikaAdmin(e);
            }
        }

        Common.createDefaultTimer(new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                Common.createDefaultTimer(new EventListener() {
                    @Override
                    public void onEvent(Event e2) throws Exception {
                        cetak(HasilSPMIAction.this.hasilSPMI);
                    }
                }, "Proses cetak", false, 2500);
            }
        });

        return true;
    }

    // =====================================================================
    // Criteria & search
    // =====================================================================

    @Override
    public Criteria initCriteria(boolean order) {
        Session session = HibernateUtil.currentSession();
        Criteria criteria = session.createCriteria(HasilSPMI.class)
                .add((start == null || end == null || start.getValue() == null || end.getValue() == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction(
                        "date(this_.tanggal_pembuatan) between date('"
                        + Common.databaseDateFormat.get().format(start.getValue()) + "') and date('"
                        + Common.databaseDateFormat.get().format(end.getValue()) + "')")))
                .add(searchaktif == null || searchaktif.isChecked()
                        ? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
                        : Restrictions.sqlRestriction("true"))
                .add(searchnama.getValue().trim().isEmpty()
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
                .add(searchjenis.getSelectedItem() == null || searchjenis.getSelectedItem().getValue() == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("jenisSPMI", searchjenis.getSelectedItem().getValue()))
                .add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
                        ? Restrictions.sqlRestriction("true")
                        : Restrictions.eq("status", searchstatus.getSelectedItem().getValue()))
                .add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
                        ? Restrictions.sqlRestriction("true")
                        : CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))
                .add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
                        ? Restrictions.sqlRestriction("true")
                        : CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false));

        if (order) criteria.addOrder(Order.desc("id"));
        return criteria;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSearchDefault(Event event) {
        Common.initPaging(initCriteria(false), paging);
        List<HasilSPMI> data = initCriteria(true)
                .setMaxResults(Common.ROWS_COUNT_ON_PAGE)
                .setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
                .list();
        refreshGridData(data, new HasilSPMIRenderer());
    }

    // =====================================================================
    // FormSop interface
    // =====================================================================

    @Override
    public String istilah() throws Exception {
        return "Pengajuan SPMI";
    }

    @Override
    public DataSop ambil() throws Exception {
        return hasilSPMI;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Class ambilClass() throws Exception {
        return HasilSPMI.class;
    }

    @Override
    public void setPersetujuan(boolean persetujuan) {
        this.persetujuan = persetujuan;
    }

    // =====================================================================
    // Internal helpers
    // =====================================================================

    private String statusValue() {
        return (status == null || status.getSelectedItem() == null)
                ? null : (String) status.getSelectedItem().getValue();
    }
}
