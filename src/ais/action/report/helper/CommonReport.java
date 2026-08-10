package ais.action.report.helper;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Blob;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.util.media.AMedia;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.report.Report;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.file.FileFoto;
import ais.database.model.file.LampiranLain;
import ais.database.model.sirs.Minggu;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import net.sf.jasperreports.engine.JasperCompileManager;

public class CommonReport {

    private static final int DEFAULT_SPREADSHEET_ROWS = 5000;
    private static final int DEFAULT_SPREADSHEET_COLUMNS = 40;
    private static final int PARAMETER_PAGE_SIZE = 20;
    private static final int BUFFER_SIZE = 8192;
    private static final String REPORT_PREFIX = "Report_";

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd");
        }
    };

    private static final ThreadLocal<SimpleDateFormat> DATE_TIME_FORMAT = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
    };

    @SuppressWarnings("rawtypes")
    public static void tampilkanReportPDF(Component parent, File file) throws Exception {
        Map parameters = null;
        tampilkanReportPDF(parent, file, parameters);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void tampilkanReportPDF(Component parent, File file, final Map parameters) throws Exception {
        if (file == null) {
            return;
        }

        Component target = parent;
        if (parent != null && parent instanceof Window) {
            final Window window = (Window) parent;
            Borderlayout borderlayout = new Borderlayout();
            borderlayout.setParent(window);
            borderlayout.setWidth("100%");
            borderlayout.setHeight("100%");

            Center center = new Center();
            ais.ui.util.ZkCompat.setFlex(center, true);
            center.setParent(borderlayout);
            target = center;

            if (parameters != null) {
                North north = new North();
                north.setParent(borderlayout);
                north.setSize("34px");
                north.setSplittable(false);
                north.setCollapsible(false);
                north.appendChild(CommonReport.exportReport(new ParameterListener() {
                    @Override
                    public Map<String, Serializable> generateParameters() throws Exception {
                        return parameters;
                    }
                }, file.getName(), null, null));
            }

            South south = new South();
            south.setSize("38px");
            south.setParent(borderlayout);

            Toolbar toolbar = new Toolbar();
            toolbar.setAlign("end");
            toolbar.setParent(south);

            MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
            button.setTooltiptext("Tutup tampilan laporan");
            button.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    window.detach();
                }
            });
            button.setParent(toolbar);
        }

        Integer desktopHeight = target == null ? null : (Integer) target.getAttribute("desktopHeight");
        if (target instanceof Iframe) {
            Iframe iframe = (Iframe) target;
            Common.clear(iframe);
            iframe.setWidth("100%");
            iframe.setHeight(desktopHeight == null ? "100%" : desktopHeight + "px");
            Report.tampil(file, iframe);
        } else {
            Report.tampil(file, target);
        }
    }

    public static void tampilkanReportXLS(Component parent, File file) throws InterruptedException {
        tampilkanReportXLS(parent, file, null);
    }

    public static void tampilkanReportXLS(Component parent, final File file, Integer row) throws InterruptedException {
        if (file == null) {
            return;
        }

        long size = file.length();
        final String path = Common.getRequestHostWithProtocol() + "/report/" + file.getName();
        System.out.println("report " + path + " size " + size);

        if (parent != null) {
            Common.clear(parent);
        }

        if (size <= 3584) {
            MyMessageboxConfig.show("Data tidak ditemukan", "Peringatan", MyMessageboxConfig.OK,
                    MyMessageboxConfig.EXCLAMATION);
            return;
        }

        Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
        borderlayout.setWidth("100%");
        borderlayout.setHeight("100%");
        borderlayout.setParent(parent);

        North north = new North();
        north.setSize("74px");
        north.setParent(borderlayout);

        Vbox header = new Vbox();
        header.setWidth("100%");
        header.setStyle("padding:8px 10px;background:#f8fafc;border-bottom:1px solid #e5e7eb;");
        header.setParent(north);

        Html info = new Html();
        info.setContent("<div style='font-family:Arial,sans-serif;'>"
                + "<div style='font-weight:bold;font-size:13px;color:#111827;margin-bottom:3px;'>Preview Excel</div>"
                + "<div style='font-size:11px;color:#4b5563;'>Data ditampilkan dahulu agar mudah diperiksa. Gunakan tombol download untuk mengambil file Excel asli.</div>"
                + "</div>");
        info.setParent(header);

        Toolbar toolbar = new Toolbar();
        toolbar.setAlign("end");
        toolbar.setParent(header);

        MyToolbarbuttonConfig download = new MyToolbarbuttonConfig("Download Excel Asli", FileFoto.icon("xls"));
        download.setTooltiptext("Download file Excel asli");
        download.setParent(toolbar);
        download.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                AMedia amedia = new AMedia(file, "application/vnd.ms-excel", "UTF-8");
                Filedownload.save(amedia);
            }
        });

        Center center = new Center();
        ais.ui.util.ZkCompat.setFlex(center, true);
        center.setParent(borderlayout);

        Spreadsheet spreadsheet = new ais.ui.util.MySpreadsheet();
        spreadsheet.setSrc(path);
        spreadsheet.setParent(center);
        spreadsheet.setWidth("100%");
        spreadsheet.setHeight("100%");
        spreadsheet.setVflex("true");
        spreadsheet.setMaxrows(row == null ? DEFAULT_SPREADSHEET_ROWS : row.intValue());
        spreadsheet.setMaxcolumns(DEFAULT_SPREADSHEET_COLUMNS);

        // FIX beban widget zss Spreadsheet (modul SIRS, 21 laporan): ganti dgn Grid ringan
        // berpaginasi via PratinjauXlsxHelper (pola B, sama spt 57 dashboard lain). Book zss
        // tetap hidup (widget disembunyikan bukan detach); tombol "Download Excel Asli" di atas
        // tetap baca langsung dari File asli (bukan dari Book spreadsheet ini) jadi tak terpengaruh.
        ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(spreadsheet);
    }

    public static Combobox generateReportType() {
        Combobox reportType = new Combobox();
        MyComboitemConfig comboitem = new MyComboitemConfig(Report.PDF);
        comboitem.setValue(Report.PDF);
        reportType.appendChild(comboitem);

        comboitem = new MyComboitemConfig(Report.RTF);
        comboitem.setValue(Report.RTF);
        reportType.appendChild(comboitem);

        reportType.setSelectedIndex(0);
        return reportType;
    }

    public static Toolbar exportReport(ParameterListener parameters, String nama) {
        return exportReport(parameters, nama, null, null, true);
    }

    @SuppressWarnings("rawtypes")
    public static Toolbar exportReport(ParameterListener parameters, String nama, List map, EventListener eventListener) {
        return exportReport(parameters, nama, map, eventListener, true);
    }

    @SuppressWarnings("rawtypes")
    private static void tambahTombolExport(Toolbar toolbar, String judul, String iconPath, final String tipeReport,
            final Radiogroup localeCombobox, final ParameterListener parameters, final String nama, final List map,
            boolean isVisible) {
        MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(judul, FileFoto.icon(iconPath));
        button.setVisible(isVisible);
        button.setTooltiptext("Download laporan format " + judul);
        button.setParent(toolbar);
        button.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                Common.createDefaultTimer(new EventListener() {
                    @Override
                    public void onEvent(Event arg0) throws Exception {
                        try {
                            Locale locale = ambilLocale(localeCombobox);
                            Map<String, Serializable> mapParameter = parameters == null ? new HashMap<String, Serializable>()
                                    : parameters.generateParameters();
                            Report.generateDownloadReport(tipeReport, mapParameter, nama, map,
                                    ais.ui.util.WaktuUtil.getDate(), locale);
                        } catch (Exception e) {
                            Common.tampilErrorJikaAdmin(e);
                            PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
                            		new String[] {
                            			"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
                            			"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
                            			"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
                            		});
                        }
                    }
                });
            }
        });
    }

    @SuppressWarnings("rawtypes")
    public static Toolbar exportReport(final ParameterListener parameters, final String nama, final List map,
            final EventListener eventListener, boolean edit) {

        Toolbar toolbar = new Toolbar();
        toolbar.setAlign("end");
        toolbar.setHeight("28px");
        toolbar.setStyle("border:0;background:transparent;padding:2px 4px;");

        final Radiogroup localeCombobox = new Radiogroup();
        MyRadioConfig comboitem = new MyRadioConfig(Common.locale.getCountry());
        comboitem.setAttribute("val", Common.locale);
        localeCombobox.appendChild(comboitem);
        localeCombobox.setSelectedItem(comboitem);
        comboitem.setVisible(false);

        comboitem = new MyRadioConfig(Common.localeEn.getCountry());
        comboitem.setAttribute("val", Common.localeEn);
        localeCombobox.appendChild(comboitem);
        comboitem.setVisible(false);

        localeCombobox.addEventListener("onCheck", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                Locale locale = ambilLocale(localeCombobox);
                System.out.println("locale = " + locale);
                if (eventListener != null) {
                    eventListener.onEvent(new Event("", localeCombobox, locale));
                }
            }
        });
        localeCombobox.setParent(toolbar);

        if (eventListener != null) {
            toolbar.appendChild(new Space());
            toolbar.appendChild(new Space());

            // Tiap tombol laporan bisa di-ON/OFF via Konfigurasi (default ON). Kondisi lama
            // (mis. hanya non-mahasiswa/siswa, atau updateFormatLaporan admin) TETAP dipertahankan
            // dan di-AND dengan konfigurasi ini.
            tambahTombolExport(toolbar, "PDF", "pdf", Report.PDF, localeCombobox, parameters, nama, map,
                    Common.bolehKonfigurasi("report_tombol_pdf"));

            Tbmuser tbmuser = Common.getCurrentUser();
            if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null) {
                tambahTombolExport(toolbar, "XLS", "xls", Report.XLS, localeCombobox, parameters, nama, map,
                        Common.bolehKonfigurasi("report_tombol_xls"));
                tambahTombolExport(toolbar, "DOCX", "docx", Report.DOCX, localeCombobox, parameters, nama, map,
                        Common.bolehKonfigurasi("report_tombol_docx"));
                tambahTombolExport(toolbar, "PPTX", "pptx", Report.PPTX, localeCombobox, parameters, nama, map,
                        Common.bolehKonfigurasi("report_tombol_pptx"));
                tambahTombolExport(toolbar, "ODT", "odt", Report.ODT, localeCombobox, parameters, nama, map, false);
                tambahTombolExport(toolbar, "ODS", "odt", Report.ODS, localeCombobox, parameters, nama, map, false);
                tambahTombolExport(toolbar, "TXT", "txt", Report.TXT, localeCombobox, parameters, nama, map, false);
                tambahTombolExport(toolbar, "CSV", "txt", Report.CSV, localeCombobox, parameters, nama, map, false);
                tambahTombolExport(toolbar, "HTML", "html", Report.HTML, localeCombobox, parameters, nama, map, false);
            }
        }

        Tbmuser tbmuser = Common.getCurrentUser();
        Tbmrole tbmrole = tbmuser == null ? null : tbmuser.hakAkses();
        if (tbmrole != null && tbmrole.getUpdateFormatLaporan() && edit) {
            // Kondisi lama (updateFormatLaporan admin + edit) dipertahankan, lalu di-AND dengan
            // konfigurasi per-tombol (default ON).
            if (Common.bolehKonfigurasi("report_tombol_download")) {
                tambahTombolDownloadJrxml(toolbar, parameters, nama);
            }
            if (Common.bolehKonfigurasi("report_tombol_upload")) {
                tambahTombolUploadJrxml(toolbar, nama, localeCombobox, eventListener);
            }
            if (Common.bolehKonfigurasi("report_tombol_sejarah")) {
                tambahTombolHistory(toolbar, nama);
            }
            if (Common.bolehKonfigurasi("report_tombol_parameter")) {
                tambahTombolParameter(toolbar, parameters, nama);
            }
            // Tombol Reset hanya untuk superadmin: menghapus jrxml kustom dan
            // mengembalikan laporan ke file JRXML bawaan (default).
            if (Common.getApakahAdmin() && Common.bolehKonfigurasi("report_tombol_reset")) {
                tambahTombolResetJrxml(toolbar, nama);
            }
        }
        return toolbar;
    }

    private static void tambahTombolDownloadJrxml(Toolbar toolbar, final ParameterListener parameters, final String nama) {
        MyToolbarbuttonConfig btnDownload = new MyToolbarbuttonConfig("Download", FileFoto.icon("jrxml"));
        btnDownload.setTooltiptext("Download file JRXML yang sedang aktif");
        btnDownload.setParent(toolbar);
        btnDownload.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                try {
                    downloadJrxmlAktif(parameters, nama);
                } catch (Exception e) {
                    Common.tampilErrorJikaAdmin(e);
                    // Pastikan SELALU ada umpan balik ke pengguna (tombol "tidak merespons"
                    // sebelumnya karena error ditelan tampilErrorJikaAdmin yang admin-only).
                    try {
                        MyMessageboxConfig.show(
                                "Gagal mengunduh file JRXML: "
                                        + (e.getMessage() == null ? e.toString() : e.getMessage()),
                                "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
                    } catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/report/helper/CommonReport.java:399");
                    }
                }
            }
        });
    }

    private static void tambahTombolUploadJrxml(Toolbar toolbar, final String nama, final Radiogroup localeCombobox,
            final EventListener eventListener) {
        MyToolbarbuttonConfig fileUpload = new MyToolbarbuttonConfig("Upload", "/img/Button-Upload-icon.png");
        fileUpload.setTooltiptext("Upload file JRXML baru");
        fileUpload.setParent(toolbar);
        fileUpload.setUpload(Common.ukuranFileUpload(500000));
        fileUpload.addEventListener("onUpload", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                handleUploadReport((UploadEvent) event, nama, localeCombobox, eventListener);
            }
        });
    }

    private static void tambahTombolHistory(Toolbar toolbar, final String nama) {
        MyToolbarbuttonConfig btnHistory = new MyToolbarbuttonConfig("Sejarah", "/img/Time-Sandglass-icon.png");
        btnHistory.setTooltiptext("Lihat riwayat upload JRXML");
        btnHistory.setParent(toolbar);
        btnHistory.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                handleHistoryReport(nama);
            }
        });
    }

    private static void tambahTombolParameter(Toolbar toolbar, final ParameterListener parameters, final String nama) {
        MyToolbarbuttonConfig btnParam = new MyToolbarbuttonConfig("Parameter", "/img/User-Interface-Menu-icon.png");
        btnParam.setTooltiptext("Lihat seluruh parameter yang dipakai laporan");
        btnParam.setParent(toolbar);
        btnParam.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                handleParameterReport(parameters, nama);
            }
        });
    }

    private static void tambahTombolResetJrxml(Toolbar toolbar, final String nama) {
        MyToolbarbuttonConfig btnReset = new MyToolbarbuttonConfig("Reset", "/img/svg/refresh.svg");
        btnReset.setTooltiptext("Kembalikan laporan ke file JRXML bawaan (hapus konfigurasi yang diunggah)");
        btnReset.setParent(toolbar);
        btnReset.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                MyMessageboxConfig.show(
                        "Yakin ingin mereset laporan \"" + nama + "\" ke file JRXML bawaan?\n"
                                + "File JRXML yang pernah diunggah akan dihapus dari konfigurasi.",
                        "Konfirmasi Reset",
                        MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
                        MyMessageboxConfig.QUESTION,
                        new EventListener() {
                            @Override
                            public void onEvent(Event event) throws Exception {
                                int i = Integer.parseInt(event.getData().toString());
                                if (i == MyMessageboxConfig.OK) {
                                    try {
                                        handleResetJrxml(nama);
                                        MyMessageboxConfig.show(
                                                "Laporan \"" + nama + "\" berhasil direset ke file JRXML bawaan.",
                                                "Berhasil", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                                    } catch (Exception e) {
                                        Common.tampilErrorJikaAdmin(e);
                                        PesanFormalHelper.tampilkanGagalException(
                                                "reset JRXML laporan " + nama, e,
                                                new String[] {
                                                        "Coba ulangi operasi reset.",
                                                        "Hubungi Administrator jika masalah berlanjut." });
                                    }
                                }
                            }
                        });
            }
        });
    }

    private static void handleResetJrxml(String nama) throws Exception {
        // lampiran_lain ada di streaming DB — wajib pakai StreamingHibernateUtil
        org.hibernate.Session streamSess = null;
        org.hibernate.Session mainSess = null;
        try {
            streamSess = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
            streamSess.beginTransaction();
            streamSess.createSQLQuery(
                    "DELETE FROM lampiran_lain WHERE ref = -10001 AND jenis = :jenis")
                    .setParameter("jenis", REPORT_PREFIX + nama)
                    .executeUpdate();
            streamSess.getTransaction().commit();
        } catch (Exception e) {
            if (streamSess != null && streamSess.getTransaction() != null) {
                try { streamSess.getTransaction().rollback(); } catch (Exception er) {
                    ais.common.ErrorAuditUtil.record(er, "auto-audit(empty-catch) CommonReport:handleResetJrxml:streamRollback");
                }
            }
            throw e;
        } finally {
            if (streamSess != null && streamSess.isOpen()) {
                try { streamSess.close(); } catch (Exception er) {
                    ais.common.ErrorAuditUtil.record(er, "auto-audit(empty-catch) CommonReport:handleResetJrxml:streamClose");
                }
            }
        }
        // Konfigurasi ada di main DB
        try {
            mainSess = HibernateUtil.getSessionFactory().openSession();
            mainSess.beginTransaction();
            mainSess.createSQLQuery(
                    "UPDATE konfigurasi SET info1 = '' WHERE kunci = :kunci")
                    .setParameter("kunci", REPORT_PREFIX + nama)
                    .executeUpdate();
            mainSess.getTransaction().commit();
        } catch (Exception e) {
            if (mainSess != null && mainSess.getTransaction() != null) {
                try { mainSess.getTransaction().rollback(); } catch (Exception er) {
                    ais.common.ErrorAuditUtil.record(er, "auto-audit(empty-catch) CommonReport:handleResetJrxml:mainRollback");
                }
            }
            throw e;
        } finally {
            if (mainSess != null && mainSess.isOpen()) {
                try { mainSess.close(); } catch (Exception er) {
                    ais.common.ErrorAuditUtil.record(er, "auto-audit(empty-catch) CommonReport:handleResetJrxml:mainClose");
                }
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private static void downloadJrxmlAktif(ParameterListener parameters, String nama) throws Exception {
        String namaLaporan = null;
        // generateParameters() bisa melempar (mis. komponen UI parameter sudah berubah
        // setelah laporan tampil). Jangan biarkan itu menggagalkan unduhan: cukup pakai
        // nama dasar laporan bila regenerasi parameter gagal.
        Map parameter = null;
        try {
            parameter = parameters == null ? null : parameters.generateParameters();
        } catch (Throwable t) {
            parameter = null;
        }
        if (parameter != null && parameter.containsKey("nama_laporan") && parameter.get("nama_laporan") != null) {
            namaLaporan = parameter.get("nama_laporan").toString();
        }

        LampiranLain lampiran = LampiranLain.ambil(-10001L, REPORT_PREFIX + nama);
        String keyKonfigurasi = namaLaporan == null ? REPORT_PREFIX + nama : REPORT_PREFIX + namaLaporan;
        String namaDefault = namaLaporan == null ? nama : namaLaporan;
        // getKonfigurasi/getInfo1 bisa null → jangan NPE; pakai nama dasar sebagai fallback.
        ais.database.model.Konfigurasi kf = Common.getKonfigurasi(keyKonfigurasi, Konfigurasi.AKTIF, namaDefault, "", "");
        String fileName = (kf == null || kf.getInfo1() == null || kf.getInfo1().trim().isEmpty()) ? namaDefault
                : kf.getInfo1();

        File fileAktif = new File(lampiran == null ? (Common.ambilREAL_PATH_REPORT() + "/" + fileName + ".jrxml")
                : lampiran.ambilFile().getAbsolutePath());
        File fileAsli = new File(Common.ambilREAL_PATH_REPORT() + "/" + nama + ".jrxml");

        if (fileAktif != null && fileAktif.exists()) {
            Filedownload.save(new AMedia(fileAktif, "application/jrxml", "UTF-8"));
            tampilToastUnduh(fileAktif.getName());
        } else if (fileAsli.exists()) {
            Filedownload.save(new AMedia(fileAsli, "application/jrxml", "UTF-8"));
            tampilToastUnduh(fileAsli.getName());
        } else {
            // FIX permintaan pengguna: "jika file tidak ketemu, ambil dari history upload file JRXML
            // terakhir" -- sebelum menyerah dengan pesan error, coba ambil baris riwayat upload
            // TERBARU (ref=-10002, jenis sama -- lihat handleUploadReport/handleHistoryReport) yang
            // file fisiknya BENAR-BENAR masih ada di disk. Ini menutup celah saat baris "aktif"
            // (ref=-10001) rusak/hilang filenya tapi masih ada versi lama yang valid tersimpan.
            LampiranLain lampiranSejarah = ambilJrxmlTerakhirDariSejarah(REPORT_PREFIX + nama,
                    lampiran == null ? null : lampiran.getId());
            File fileSejarah = null;
            try {
                fileSejarah = lampiranSejarah == null ? null : lampiranSejarah.ambilFile();
            } catch (Exception eSejarah) {
                ais.common.ErrorAuditUtil.record(eSejarah,
                        "auto-audit(empty-catch) src/ais/action/report/helper/CommonReport.java:downloadJrxmlAktif-ambilFileSejarah");
            }
            if (fileSejarah != null && fileSejarah.exists()) {
                Filedownload.save(new AMedia(fileSejarah, "application/jrxml", "UTF-8"));
                tampilToastUnduh("(riwayat) " + fileSejarah.getName());
            } else {
                // Sebutkan path yang dicari agar mudah ditelusuri (mis. laporan ini memang
                // tidak memakai template JRXML, atau nama file laporan berbeda).
                MyMessageboxConfig.show(
                        "File JRXML untuk laporan ini tidak ditemukan sehingga tidak ada yang bisa diunduh."
                                + "\n\nDicari di:\n- " + fileAktif.getAbsolutePath() + "\n- " + fileAsli.getAbsolutePath()
                                + "\n- (riwayat upload terakhir, bila ada)"
                                + "\n\nBila laporan ini tidak menggunakan template JRXML (mis. dibuat dari HTML/Java),"
                                + " penambahan tanda tangan tidak dilakukan lewat JRXML.",
                        "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
            }
        }
    }

    private static void tampilToastUnduh(String namaFile) {
        try {
            String pesan = "Berhasil mengunduh: " + namaFile.replace("'", "\\'").replace("\\", "\\\\");
            Clients.evalJavaScript(
                "(function(){var d=document,t=d.createElement('div');t.setAttribute('style'," +
                "'position:fixed;top:16px;left:50%;transform:translateX(-50%);z-index:2147483647;" +
                "background:#16a34a;color:#fff;padding:8px 18px;border-radius:8px;font-size:13px;" +
                "font-weight:700;box-shadow:0 4px 16px rgba(0,0,0,.25);pointer-events:none;white-space:nowrap;');" +
                "t.textContent='" + pesan + "';d.body.appendChild(t);" +
                "setTimeout(function(){try{d.body.removeChild(t);}catch(e){}},4000);})();"
            );
        } catch (Throwable ignore) {
            ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) CommonReport.tampilToastUnduh");
        }
    }

    /**
     * Fallback bagi {@link #downloadJrxmlAktif(ParameterListener, String)} saat file jrxml AKTIF
     * (ref=-10001) maupun file bawaan tidak ditemukan di disk (mis. server dipindah/file terhapus
     * manual tapi baris DB masih ada, atau baris aktif rusak) -- ambil jrxml UPLOAD TERAKHIR dari
     * riwayat (baris {@code ref=-10002} dengan {@code jenis} yang sama, lihat
     * {@link #handleUploadReport}/{@link #handleHistoryReport}) yang file fisiknya BENAR-BENAR masih
     * ada, diurutkan dari yang paling baru ({@code id} descending). Baris yang sudah terbukti gagal
     * (id pada parameter {@code idYangSudahDicoba}) dilewati agar tidak mengulang percobaan yang sama.
     *
     * @param jenis              {@code REPORT_PREFIX + nama} laporan
     * @param idYangSudahDicoba  id baris {@code ref=-10001} yang sudah dicoba & terbukti filenya
     *                           tidak ada; boleh {@code null}
     * @return baris {@link LampiranLain} riwayat paling baru yang file fisiknya masih ada, atau
     *         {@code null} bila tidak ada satu pun riwayat yang valid
     */
    @SuppressWarnings("unchecked")
    private static LampiranLain ambilJrxmlTerakhirDariSejarah(String jenis, Long idYangSudahDicoba) {
        Session session = null;
        try {
            session = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
            List<LampiranLain> daftar = session.createCriteria(LampiranLain.class)
                    .add(Restrictions.eq("jenis", jenis))
                    .addOrder(Order.desc("id"))
                    .list();
            if (daftar == null) {
                return null;
            }
            for (LampiranLain kandidat : daftar) {
                if (kandidat == null || kandidat.getId() == null) {
                    continue;
                }
                if (idYangSudahDicoba != null && idYangSudahDicoba.equals(kandidat.getId())) {
                    continue;
                }
                try {
                    File f = kandidat.ambilFile();
                    if (f != null && f.exists()) {
                        return kandidat;
                    }
                } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/report/helper/CommonReport.java:ambilJrxmlTerakhirDariSejarah-loop"); }
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e,
                    "CommonReport.ambilJrxmlTerakhirDariSejarah gagal query riwayat jrxml jenis=" + jenis);
        } finally {
            closeOpenSession(session);
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private static void handleUploadReport(UploadEvent uploadEvent, final String nama, final Radiogroup localeCombobox,
            final EventListener eventListener) throws Exception {
        if (uploadEvent == null) {
            return;
        }

        Media media = uploadEvent.getMedia();
        if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media)) {
            return;
        }
        if (media.getName() == null || !media.getName().trim().toLowerCase(Locale.ENGLISH).endsWith(".jrxml")) {
            MyMessageboxConfig.show("File yang Anda upload harus berupa file JRXML", "Peringatan",
                    MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
            return;
        }

        Session session = null;
        Session streamingSession = null;
        InputStream inputStream = null;
        FileOutputStream fileOutputStream1 = null;
        FileOutputStream fileOutputStream2 = null;
        FileInputStream fileInputStream = null;
        Transaction tx = null;
        Transaction txStream = null;

        try {
            Konfigurasi konfigurasi = Common.getKonfigurasi(REPORT_PREFIX + nama, Konfigurasi.AKTIF, nama, "", "");
            String reportName = nama + "_" + Common.getGeneratedBarCode();
            konfigurasi.setInfo1(reportName);

            session = ais.action.report.Report.openNativeSession();
            tx = session.beginTransaction();
            Common.refreshSaveOrUpdate(session, konfigurasi);
            tx.commit();
            tx = null;

            File newFile = new File(Common.ambilREAL_PATH_REPORT() + "/" + reportName + ".jrxml");
            buatFolderJikaPerlu(newFile);

            inputStream = media.getStreamData();
            fileOutputStream1 = new FileOutputStream(newFile);
            copyStream(inputStream, fileOutputStream1);
            closeQuietly(fileOutputStream1);
            fileOutputStream1 = null;
            closeQuietly(inputStream);
            inputStream = null;

            File folder = CommonMedia.getMediaDirectory();
            if (folder != null && !folder.exists()) {
                folder.mkdirs();
            }

            File fileMedia = new File(folder.getAbsolutePath() + "/"
                    + URLEncoder.encode(ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis() + "_" + media.getName(),
                            "UTF-8"));
            buatFolderJikaPerlu(fileMedia);

            fileOutputStream2 = new FileOutputStream(fileMedia);
            fileInputStream = new FileInputStream(newFile);
            IOUtils.copyLarge(fileInputStream, fileOutputStream2);
            closeQuietly(fileInputStream);
            fileInputStream = null;
            closeQuietly(fileOutputStream2);
            fileOutputStream2 = null;

            streamingSession = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
            // Muat lampiran AKTIF DI DALAM transaksi streamingSession agar Large Object (foto)
            // terikat ke koneksi NON-autocommit. Bila memakai LampiranLain.ambil (session lain yang
            // sudah ditutup), Blob-nya berada di koneksi autocommit sehingga save clone memicu
            // "Large Objects may not be used in auto-commit mode".
            txStream = streamingSession.beginTransaction();
            LampiranLain lampiranAktif = (LampiranLain) streamingSession
                    .createQuery("from LampiranLain where ref = :ref and jenis = :jenis")
                    .setParameter("ref", -10001L).setParameter("jenis", REPORT_PREFIX + nama)
                    .setMaxResults(1).uniqueResult();
            if (lampiranAktif != null) {
                // Materialkan byte foto SELAGI transaksi streaming AKTIF (LO wajib non-autocommit),
                // lalu bungkus jadi SerialBlob in-memory agar lepas dari LO asli sebelum di-save.
                byte[] fotoBytes = null;
                try {
                    java.sql.Blob fotoAsli = lampiranAktif.getFoto();
                    if (fotoAsli != null) {
                        fotoBytes = IOUtils.toByteArray(fotoAsli.getBinaryStream());
                    }
                } catch (Exception exFoto) {
                    fotoBytes = null;
                }

                LampiranLain lampiranHistory = (LampiranLain) lampiranAktif.clone();
                lampiranHistory.setRef(-10002L);
                lampiranHistory.setId(null);
                lampiranHistory.setFoto(fotoBytes == null ? null
                        : new javax.sql.rowset.serial.SerialBlob(fotoBytes));
                streamingSession.save(lampiranHistory);

                // PENTING: keluarkan lampiranAktif dari persistence context SEBELUM baris
                // dihapus lewat SQL native di bawah. AuditTimestampInterceptor.instantiate()
                // (lihat ais/database/hibernate/AuditTimestampInterceptor.java) mengembalikan
                // SATU instance canonical per kelas+id dari EntityIdentityMap yang dipakai
                // BERSAMA oleh semua session di JVM ini -- bila instance yang sama sedang
                // termuat/berubah di session/thread lain bersamaan, snapshot dirty-check
                // streamingSession untuk lampiranAktif bisa jadi tidak lagi cocok. Ditambah
                // baris ref=-10001 di bawah ini DIHAPUS via SQL native (di luar sepengetahuan
                // persistence context Hibernate), sehingga kalau lampiranAktif sampai
                // ter-flush sebagai UPDATE saat commit, barisnya sudah tidak ada lagi ->
                // org.hibernate.StaleStateException: "actual row count: 0; expected: 1".
                // evict() melepas lampiranAktif dari session ini shg tidak ikut di-dirty-check
                // / di-flush lagi -- data yang dibutuhkan (fotoBytes, clone-nya) sudah
                // diambil di atas, jadi aman dilepas di sini.
                streamingSession.evict(lampiranAktif);

                streamingSession.createSQLQuery("delete from lampiran_lain where ref = -10001 and jenis = :jenisReport")
                        .setParameter("jenisReport", REPORT_PREFIX + nama).executeUpdate();
            }
            txStream.commit();
            txStream = null;

            LampiranLain lampiranBaru = new LampiranLain();
            lampiranBaru.setRef(-10001L);
            lampiranBaru.setNama(reportName + ".jrxml");
            lampiranBaru.setKeterangan(media.getContentType());
            lampiranBaru.setJenis(REPORT_PREFIX + nama);

            txStream = streamingSession.beginTransaction();
            streamingSession.save(lampiranBaru);
            txStream.commit();
            txStream = null;

            fileInputStream = new FileInputStream(fileMedia);
            Blob blob;
            if (fileMedia.length() > Integer.MAX_VALUE) {
                blob = new javax.sql.rowset.serial.SerialBlob(IOUtils.toByteArray(fileInputStream));
            } else {
                blob = new javax.sql.rowset.serial.SerialBlob(IOUtils.toByteArray(fileInputStream));
            }
            lampiranBaru.setFoto(blob);

            txStream = streamingSession.beginTransaction();
            streamingSession.update(lampiranBaru);
            txStream.commit();
            txStream = null;
            closeQuietly(fileInputStream);
            fileInputStream = null;

            refreshReportSetelahUpload(localeCombobox, eventListener);
        } catch (Exception e) {
            rollbackQuietly(tx);
            rollbackQuietly(txStream);
            Common.tampilErrorJikaAdmin(e);
        } finally {
            closeQuietly(inputStream);
            closeQuietly(fileOutputStream1);
            closeQuietly(fileOutputStream2);
            closeQuietly(fileInputStream);
            closeOpenSession(session);
            closeOpenSession(streamingSession);
        }
    }

    private static void refreshReportSetelahUpload(final Radiogroup localeCombobox, final EventListener eventListener) {
        Common.createDefaultTimer(new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                Locale locale = ambilLocale(localeCombobox);
                System.out.println("locale = " + locale);
                if (eventListener != null) {
                    eventListener.onEvent(new Event("", localeCombobox, locale));
                }
            }
        });
    }

    private static void handleHistoryReport(final String nama) {
        Session streamingSession = null;
        List<LampiranLain> lampirans = new ArrayList<LampiranLain>();
        try {
            streamingSession = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
            @SuppressWarnings("unchecked")
            List<LampiranLain> temp = streamingSession.createCriteria(LampiranLain.class)
                    .add(Restrictions.eq("jenis", REPORT_PREFIX + nama)).addOrder(Order.desc("id")).list();
            if (temp != null) {
                lampirans.addAll(temp);
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
            		new String[] {
            			"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
            			"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
            			"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
            		});
        } finally {
            closeOpenSession(streamingSession);
        }

        try {
            final Window window = new Window("Sejarah upload laporan " + nama, "none", true);
            window.setHeight("95%");
            window.setWidth("650px");
            window.setClosable(true);
            ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

            Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
            borderlayout.setParent(window);
            borderlayout.setWidth("100%");
            borderlayout.setHeight("100%");

            North north = new North();
            north.setSize("70px");
            north.setParent(borderlayout);
            Html info = new Html();
            info.setContent("<div style='font-family:Arial,sans-serif;padding:10px;background:#f8fafc;border-bottom:1px solid #e5e7eb;'>"
                    + "<div style='font-weight:bold;font-size:13px;color:#111827;'>Riwayat format laporan</div>"
                    + "<div style='font-size:11px;color:#4b5563;margin-top:3px;'>Daftar file JRXML yang pernah diupload. Gunakan download untuk mengambil versi yang diperlukan.</div>"
                    + "</div>");
            info.setParent(north);

            Center center = new Center();
            center.setParent(borderlayout);
            ais.ui.util.ZkCompat.setFlex(center, true);

            MyGrid grid = new MyGrid();
            grid.setWidth("100%");
            grid.setHeight("100%");
            grid.setMold("paging");
            grid.setPageSize(20);
            grid.getPagingChild().setMold("os");
            grid.setParent(center);

            Columns columns = new Columns();
            columns.setParent(grid);
            MyColumnConfig column = new MyColumnConfig("Nama File");
            column.setParent(columns);
            column = new MyColumnConfig("Tanggal Upload");
            column.setWidth("160px");
            column.setParent(columns);
            column = new MyColumnConfig("Aksi");
            column.setWidth("110px");
            column.setParent(columns);

            Rows rows = new Rows();
            rows.setParent(grid);
            for (int i = 0; i < lampirans.size(); i++) {
                final LampiranLain lampiran = lampirans.get(i);
                MyFormRow row = new MyFormRow();
                row.setValign("top");
                row.setParent(rows);
                row.appendChild(buatLabelTabel(lampiran == null ? "" : lampiran.getNama()));
                row.appendChild(buatLabelTabel(lampiran == null || lampiran.getTanggal_dirubah() == null ? ""
                        : Common.dateFormat.get().format(lampiran.getTanggal_dirubah())));

                A a = new A("Download");
                a.setStyle("font-size:11px;font-weight:bold;color:#2563eb;text-decoration:none;");
                row.appendChild(a);
                a.addEventListener("onClick", new EventListener() {
                    @Override
                    public void onEvent(Event arg0) throws Exception {
                        downloadLampiranJrxml(lampiran, nama);
                    }
                });
            }

            South south = new South();
            south.setSize("40px");
            south.setParent(borderlayout);

            Toolbar toolbar = new Toolbar();
            toolbar.setAlign("end");
            toolbar.setParent(south);
            MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
            cancel.setTooltiptext("Tutup");
            cancel.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    window.detach();
                }
            });
            cancel.setParent(toolbar);

            window.onModal();
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
            		new String[] {
            			"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
            			"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
            			"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
            		});
        }
    }

    private static void downloadLampiranJrxml(LampiranLain lampiran, String nama) throws Exception {
        File file = new File(lampiran == null ? (Common.ambilREAL_PATH_REPORT() + "/" + nama + ".jrxml")
                : lampiran.ambilFile().getAbsolutePath());
        File fileAsli = new File(Common.ambilREAL_PATH_REPORT() + "/" + nama + ".jrxml");

        if (file != null && file.exists()) {
            Filedownload.save(new AMedia(file, "application/jrxml", "UTF-8"));
        } else if (fileAsli.exists()) {
            Filedownload.save(new AMedia(fileAsli, "application/jrxml", "UTF-8"));
        } else {
            MyMessageboxConfig.show("File JRXML tidak ditemukan", "Peringatan", MyMessageboxConfig.OK,
                    MyMessageboxConfig.EXCLAMATION);
        }
    }

    @SuppressWarnings({ "rawtypes" })
    private static void handleParameterReport(final ParameterListener parameters, final String nama) {
        final Map<String, Serializable> copyMapLocal = new HashMap<String, Serializable>();
        try {
            final Window window = new Window("Parameter untuk laporan " + nama, "none", true);
            window.setHeight("95%");
            window.setWidth("95%");
            window.setClosable(true);
            ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

            Borderlayout borderlayoutUtama = new ais.ui.util.MyBorderlayout();
            borderlayoutUtama.setParent(window);
            borderlayoutUtama.setWidth("100%");
            borderlayoutUtama.setHeight("100%");

            North progressNorth = new North();
            progressNorth.setSize("78px");
            progressNorth.setParent(borderlayoutUtama);

            final Vbox progressBox = new Vbox();
            progressBox.setWidth("100%");
            progressBox.setStyle("padding:10px;background:#f8fafc;border-bottom:1px solid #e5e7eb;");
            progressBox.setParent(progressNorth);

            final Label progressLabel = new Label(ais.common.Common.getBahasaConfig("Menyiapkan tampilan parameter laporan ..."));
            progressLabel.setStyle("font-size:12px;font-weight:bold;color:#111827;margin-bottom:6px;");
            progressLabel.setParent(progressBox);

            final Progressmeter progressmeter = new Progressmeter();
            progressmeter.setWidth("100%");
            progressmeter.setHeight("16px");
            progressmeter.setValue(5);
            progressmeter.setParent(progressBox);

            Center centerUtama = new Center();
            centerUtama.setParent(borderlayoutUtama);
            ais.ui.util.ZkCompat.setFlex(centerUtama, true);

            Tabbox tabbox = new Tabbox();
            tabbox.setParent(centerUtama);
            tabbox.setHeight("100%");
            tabbox.setWidth("100%");

            Tabs tabs = new Tabs();
            tabs.setParent(tabbox);

            MyTabConfig tab1 = new MyTabConfig("Parameter");
            tab1.setParent(tabs);
            MyTabConfig tab2 = new MyTabConfig("Sub Report");
            tab2.setParent(tabs);
            MyTabConfig tab3 = new MyTabConfig("Gambar Pendukung");
            tab3.setParent(tabs);

            Tabpanels tabpanels = new Tabpanels();
            tabpanels.setParent(tabbox);

            Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
            tabpanel1.setParent(tabpanels);
            tabpanel1.setStyle("padding:0;overflow:hidden;");

            Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
            tabpanel2.setParent(tabpanels);
            tabpanel2.setStyle("padding:0;overflow:auto;");
            isiTabInclude(tabpanel2,
                    "Daftar file tambahan yang ikut dipakai laporan. Gunakan bagian ini saat laporan memiliki halaman atau tabel pendukung.",
                    "/pages/master/sub_report.zul?namaStr=" + URLEncoder.encode(nama, "UTF-8"));

            Tabpanel tabpanel3 = new ais.ui.util.MyTabpanel();
            tabpanel3.setParent(tabpanels);
            tabpanel3.setStyle("padding:0;overflow:auto;");
            isiTabInclude(tabpanel3,
                    "Gambar dan logo yang bisa dipakai laporan. Pastikan file pendukung sudah benar sebelum laporan dicetak.",
                    "/pages/master/pendukung_report.zul?namaStr=" + URLEncoder.encode(nama, "UTF-8"));

            Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
            borderlayout.setParent(tabpanel1);
            borderlayout.setWidth("100%");
            borderlayout.setHeight("100%");

            North north = new North();
            north.setParent(borderlayout);
            north.setSize("155px");

            Vbox header = new Vbox();
            header.setWidth("100%");
            header.setStyle("padding:10px;background:#ffffff;border-bottom:1px solid #e5e7eb;");
            header.setParent(north);

            final Html ringkasanHtml = new Html();
            ringkasanHtml.setContent(buatRingkasanParameterHtml(nama, copyMapLocal));
            ringkasanHtml.setParent(header);

            Hbox hbox = new Hbox();
            hbox.setWidth("100%");
            hbox.setStyle("padding-top:8px;gap:6px;");
            hbox.setParent(header);

            final Textbox cari = new Textbox("");
            cari.setParent(hbox);
            cari.setCols(35);
            cari.setTooltiptext("Cari berdasarkan key atau nilai parameter");

            Toolbarbutton toolbarbutton = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
            toolbarbutton.setParent(hbox);

            MyToolbarbuttonConfig btnDownloadParameter = new MyToolbarbuttonConfig("Download Semua Parameter", FileFoto.icon("xls"));
            btnDownloadParameter.setTooltiptext("Download seluruh parameter dalam bentuk Excel");
            btnDownloadParameter.setParent(hbox);
            btnDownloadParameter.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    downloadParameterExcel(copyMapLocal, nama);
                }
            });

            Center center = new Center();
            center.setParent(borderlayout);
            ais.ui.util.ZkCompat.setFlex(center, true);

            MyGrid grid = new MyGrid();
            grid.setWidth("100%");
            grid.setHeight("100%");
            grid.setMold("paging");
            grid.setPageSize(PARAMETER_PAGE_SIZE);
            grid.getPagingChild().setMold("os");
            grid.setParent(center);

            Columns columns = new Columns();
            columns.setParent(grid);
            MyColumnConfig column = new MyColumnConfig("Key");
            column.setParent(columns);
            column.setWidth("28%");
            column = new MyColumnConfig("Nilai");
            column.setParent(columns);

            final Rows rowsCari = new Rows();
            rowsCari.setParent(grid);

            final EventListener cariAkun = new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    tampilkanParameterKeGrid(rowsCari, cari, copyMapLocal, progressmeter, progressLabel, progressBox,
                            false);
                }
            };
            cari.addEventListener("onOK", cariAkun);
            toolbarbutton.addEventListener("onClick", cariAkun);

            South south = new South();
            south.setSize("42px");
            south.setParent(borderlayoutUtama);

            Toolbar toolbar = new Toolbar();
            toolbar.setAlign("end");
            toolbar.setParent(south);

            MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
            cancel.setTooltiptext("Tutup");
            cancel.addEventListener("onClick", new EventListener() {
                @Override
                public void onEvent(Event event) throws Exception {
                    window.detach();
                    copyMapLocal.clear();
                }
            });
            cancel.setParent(toolbar);

            window.onModal();

            Common.createDefaultTimer(new EventListener() {
                @Override
                public void onEvent(Event arg0) throws Exception {
                    try {
                        progressBox.setVisible(true);
                        updateProgress(progressmeter, progressLabel, 15, "Mengambil parameter laporan ...");

                        Map p = parameters == null ? null : parameters.generateParameters();
                        updateProgress(progressmeter, progressLabel, 45, "Menambahkan parameter bawaan laporan ...");
                        if (p != null) {
                            Report.initDefaultParameter(p, Common.locale);
                            salinParameter(p, copyMapLocal);
                        }

                        updateProgress(progressmeter, progressLabel, 70, "Menyusun ringkasan dan tabel ...");
                        ringkasanHtml.setContent(buatRingkasanParameterHtml(nama, copyMapLocal));
                        tampilkanParameterKeGrid(rowsCari, cari, copyMapLocal, progressmeter, progressLabel, progressBox,
                                true);
                    } catch (Exception e) {
                        updateProgress(progressmeter, progressLabel, 100, "Gagal memuat parameter laporan.");
                        Common.tampilErrorJikaAdmin(e);
                    }
                }
            });
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
            		new String[] {
            			"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
            			"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
            			"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
            		});
        }
    }

    private static void isiTabInclude(Tabpanel tabpanel, String deskripsi, String src) {
        Vbox vbox = new Vbox();
        vbox.setWidth("100%");
        vbox.setHeight("100%");
        vbox.setParent(tabpanel);

        Html html = new Html();
        html.setContent("<div style='font-family:Arial,sans-serif;padding:10px;background:#f8fafc;border-bottom:1px solid #e5e7eb;color:#4b5563;font-size:11px;'>"
                + escapeHtml(deskripsi) + "</div>");
        html.setParent(vbox);

        MyInclude include = new MyInclude(src);
        include.setWidth("100%");
        include.setHeight("100%");
        include.setParent(vbox);
    }

    @SuppressWarnings("rawtypes")
    private static void salinParameter(Map sumber, Map<String, Serializable> tujuan) {
        tujuan.clear();
        if (sumber == null) {
            return;
        }
        List<String> keys = new ArrayList<String>();
        for (Object keyObj : sumber.keySet()) {
            if (keyObj != null) {
                keys.add(String.valueOf(keyObj));
            }
        }
        Collections.sort(keys);
        for (String key : keys) {
            Object nilai = sumber.get(key);
            tujuan.put(key, ubahMenjadiSerializable(nilai));
        }
    }

    private static Serializable ubahMenjadiSerializable(Object nilai) {
        if (nilai == null) {
            return "";
        }
        if (nilai instanceof Serializable) {
            return (Serializable) nilai;
        }
        return String.valueOf(nilai);
    }

    private static void tampilkanParameterKeGrid(Rows rowsCari, Textbox cari, Map<String, Serializable> copyMapLocal,
            Progressmeter progressmeter, Label progressLabel, Component progressBox, boolean hideWhenFinish) {
        try {
            Common.clear(rowsCari);
            String keyword = cari == null || cari.getValue() == null ? "" : cari.getValue().trim().toLowerCase(Locale.ENGLISH);
            List<String> keys = new ArrayList<String>(copyMapLocal.keySet());
            Collections.sort(keys);

            // Deteksi parameter yang nilainya bersumber dari KONFIGURASI (boleh disunting).
            // Satu query saja (bukan per-baris) agar ringan & TIDAK meng-insert entri baru.
            final java.util.Set<String> namaKonfig = muatNamaKonfigurasiAman();

            int total = keys.size();
            int selesai = 0;
            updateProgress(progressmeter, progressLabel, 78, "Menampilkan parameter laporan ...");

            for (int i = 0; i < keys.size(); i++) {
                String key = keys.get(i);
                String val = toDisplayText(copyMapLocal.get(key));
                if (keyword.length() == 0 || key.toLowerCase(Locale.ENGLISH).contains(keyword)
                        || val.toLowerCase(Locale.ENGLISH).contains(keyword)) {
                    MyFormRow row = new MyFormRow();
                    row.setValign("top");
                    row.setParent(rowsCari);
                    row.appendChild(buatLabelTabel(key));
                    if (namaKonfig.contains(key)) {
                        // Parameter dari konfigurasi → tampilkan nilai + tombol Edit.
                        row.appendChild(buatSelNilaiKonfigurasi(key, val, copyMapLocal));
                    } else {
                        row.appendChild(buatLabelTabel(val));
                    }
                }
                selesai++;
                if (selesai % 10 == 0 || selesai == total) {
                    int persen = total == 0 ? 100 : 80 + ((selesai * 18) / total);
                    updateProgress(progressmeter, progressLabel, persen, "Memproses parameter " + selesai + " dari " + total
                            + " ...");
                }
            }
            updateProgress(progressmeter, progressLabel, 100, "Parameter selesai dimuat.");
            if (hideWhenFinish && progressBox != null) {
                progressBox.setVisible(false);
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
            		new String[] {
            			"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
            			"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
            			"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
            		});
        }
    }

    private static Label buatLabelTabel(String text) {
        Label label = new MyLabelKecil(text == null ? "" : text);
        label.setMultiline(true);
        label.setStyle("font-size:11px;color:#111827;white-space:normal;line-height:16px;word-break:break-word;");
        return label;
    }

    /** Memuat himpunan nama konfigurasi dengan aman (gagal → himpunan kosong, tidak melempar). */
    private static java.util.Set<String> muatNamaKonfigurasiAman() {
        try {
            return ais.common.KonfigurasiManager.kumpulanNamaKonfigurasi();
        } catch (Throwable t) {
            return new java.util.HashSet<String>();
        }
    }

    /**
     * Membuat sel kolom "Nilai" untuk parameter yang bersumber dari KONFIGURASI: menampilkan
     * nilai + tombol "Edit". Mengklik Edit memunculkan popup penyuntingan; menyimpan akan
     * memperbarui basis data sekaligus cache konfigurasi.
     *
     * @param key          nama/kunci konfigurasi
     * @param val          nilai tampil saat ini
     * @param copyMapLocal peta parameter laporan (di-update agar tampilan ikut segar)
     * @return komponen sel siap dipasang sebagai sel kedua ({@code Nilai}) pada baris.
     */
    private static Component buatSelNilaiKonfigurasi(final String key, String val,
            final Map<String, Serializable> copyMapLocal) {
        Hbox box = new Hbox();
        box.setAlign("center");
        box.setWidth("100%");
        final Label labelNilai = buatLabelTabel(val);
        labelNilai.setHflex("1");
        box.appendChild(labelNilai);
        MyToolbarbuttonConfig btnEdit = new MyToolbarbuttonConfig("Edit", FileFoto.icon("edit"));
        btnEdit.setStyle("color:#2563eb;font-weight:700;flex-shrink:0;");
        btnEdit.setTooltiptext("Sunting nilai konfigurasi \"" + key + "\" (tersimpan ke DB + cache)");
        btnEdit.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception {
                bukaPopupEditKonfigurasi(key, labelNilai, copyMapLocal);
            }
        });
        box.appendChild(btnEdit);
        return box;
    }

    /**
     * Popup penyuntingan satu nilai konfigurasi. Saat disimpan, nilai ditulis ke basis data
     * DAN cache (lewat {@code KonfigurasiManager.simpanKonfigurasi}), lalu tampilan baris &amp;
     * peta parameter di-segarkan. AMAN: kegagalan ditangani, tidak mematikan aplikasi.
     *
     * @param key          nama/kunci konfigurasi
     * @param labelNilai   label nilai pada baris (di-update setelah simpan berhasil)
     * @param copyMapLocal peta parameter laporan (di-update setelah simpan berhasil)
     */
    private static void bukaPopupEditKonfigurasi(final String key, final Label labelNilai,
            final Map<String, Serializable> copyMapLocal) {
        try {
            Konfigurasi k = ais.common.KonfigurasiManager.cariKonfigurasi(key);
            String nilaiSekarang = (k != null && k.getNilai() != null) ? k.getNilai() : labelNilai.getValue();

            final Window win = new Window("Edit Konfigurasi: " + key, "normal", true);
            win.setWidth("540px");
            win.setClosable(true);
            ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(win);

            Vbox vb = new Vbox();
            vb.setWidth("100%");
            vb.setStyle("padding:12px;");
            vb.setParent(win);

            Label info = new MyLabelKecil("Nilai disimpan ke konfigurasi dan langsung diperbarui di cache, "
                    + "sehingga laporan berikutnya memakai nilai terbaru tanpa perlu reset cache.");
            info.setMultiline(true);
            info.setStyle("color:#64748b;font-size:11px;");
            info.setParent(vb);

            final Textbox tb = new Textbox(nilaiSekarang == null ? "" : nilaiSekarang);
            tb.setMultiline(true);
            tb.setRows(5);
            tb.setWidth("100%");
            tb.setParent(vb);

            Hbox foot = new Hbox();
            foot.setStyle("margin-top:10px;");
            foot.setParent(vb);

            MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
            simpan.addEventListener("onClick", new EventListener() {
                public void onEvent(Event e) throws Exception {
                    String baru = tb.getValue();
                    Konfigurasi hasil = ais.common.KonfigurasiManager.simpanKonfigurasi(key, baru);
                    if (hasil != null) {
                        if (labelNilai != null) {
                            labelNilai.setValue(baru);
                        }
                        if (copyMapLocal != null) {
                            copyMapLocal.put(key, baru);
                        }
                        win.detach();
                        MyMessageboxConfig.show("Konfigurasi \"" + key + "\" tersimpan & cache diperbarui.",
                                "Berhasil", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                    } else {
                        MyMessageboxConfig.show("Gagal menyimpan konfigurasi. Silakan coba lagi.",
                                "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
                    }
                }
            });
            simpan.setParent(foot);

            MyToolbarbuttonConfig batal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
            batal.addEventListener("onClick", new EventListener() {
                public void onEvent(Event e) throws Exception {
                    win.detach();
                }
            });
            batal.setParent(foot);

            win.doHighlighted();
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
            		new String[] {
            			"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
            			"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
            			"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
            		});
        }
    }

    private static void updateProgress(Progressmeter meter, Label label, int persen, String status) {
        try {
            int nilai = persen;
            if (nilai < 0) {
                nilai = 0;
            }
            if (nilai > 100) {
                nilai = 100;
            }
            if (meter != null) {
                meter.setValue(nilai);
            }
            if (label != null) {
                label.setValue((status == null ? "Memproses data ..." : status) + " (" + nilai + "%)");
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/CommonReport.java:1198");
        	PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
        		new String[] {
        			"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
        			"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
        			"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
        		});
        }
    }

    private static String buatRingkasanParameterHtml(String nama, Map<String, Serializable> map) {
        int total = map == null ? 0 : map.size();
        int kosong = 0;
        int angka = 0;
        int tanggal = 0;
        int booleanValue = 0;
        int file = 0;
        int teks = 0;

        if (map != null) {
            for (Serializable value : map.values()) {
                if (value == null || String.valueOf(value).trim().length() == 0) {
                    kosong++;
                } else if (value instanceof Number) {
                    angka++;
                } else if (value instanceof Date || value instanceof Calendar) {
                    tanggal++;
                } else if (value instanceof Boolean) {
                    booleanValue++;
                } else if (value instanceof File || value instanceof InputStream || value instanceof Blob) {
                    file++;
                } else {
                    teks++;
                }
            }
        }

        int lengkap = total - kosong;
        int lengkapPersen = total == 0 ? 0 : (lengkap * 100 / total);
        int max = Math.max(1, Math.max(teks, Math.max(angka, Math.max(tanggal, Math.max(booleanValue, file)))));

        StringBuffer sb = new StringBuffer(4096);
        sb.append("<div style='font-family:Arial,sans-serif;width:100%;'>");
        sb.append("<div style='font-weight:bold;font-size:13px;color:#111827;margin-bottom:3px;'>Parameter laporan ")
                .append(escapeHtml(nama)).append("</div>");
        sb.append("<div style='font-size:11px;color:#4b5563;margin-bottom:8px;'>Semua nilai yang dipakai laporan ada di sini. Cari key atau nilai untuk memastikan laporan mengambil data yang benar.</div>");
        sb.append("<div style='display:flex;gap:8px;flex-wrap:wrap;'>");
        appendSummaryCard(sb, "Total", total, "Jumlah parameter yang terbaca");
        appendSummaryCard(sb, "Terisi", lengkap, lengkapPersen + "% sudah memiliki nilai");
        appendSummaryCard(sb, "Kosong", kosong, "Perlu dicek bila laporan belum sesuai");
        appendSummaryCard(sb, "Teks", teks, "Nilai berupa tulisan atau objek umum");
        sb.append("</div>");
        sb.append("<div style='display:flex;gap:12px;margin-top:8px;align-items:flex-start;'>");
        sb.append("<div style='flex:1;min-width:260px;border:1px solid #e5e7eb;border-radius:8px;padding:8px;background:#ffffff;'>");
        sb.append("<div style='font-weight:bold;font-size:11px;color:#111827;margin-bottom:6px;'>Komposisi Nilai</div>");
        appendBar(sb, "Teks/Objek", teks, max);
        appendBar(sb, "Angka", angka, max);
        appendBar(sb, "Tanggal", tanggal, max);
        appendBar(sb, "Ya/Tidak", booleanValue, max);
        appendBar(sb, "File/Stream", file, max);
        sb.append("</div>");
        sb.append("<div style='width:190px;border:1px solid #e5e7eb;border-radius:8px;padding:8px;background:#ffffff;'>");
        sb.append("<div style='font-weight:bold;font-size:11px;color:#111827;margin-bottom:6px;'>Kesiapan</div>");
        sb.append("<div style='height:8px;background:#e5e7eb;border-radius:999px;overflow:hidden;'>");
        sb.append("<div style='height:8px;width:").append(lengkapPersen).append("%;background:#22c55e;'></div>");
        sb.append("</div>");
        sb.append("<div style='font-size:20px;font-weight:bold;color:#111827;margin-top:6px;'>").append(lengkapPersen)
                .append("%</div>");
        sb.append("<div style='font-size:10px;color:#6b7280;'>Parameter terisi</div>");
        sb.append("</div>");
        sb.append("</div>");
        sb.append("</div>");
        return sb.toString();
    }

    private static void appendSummaryCard(StringBuffer sb, String label, int value, String info) {
        sb.append("<div style='border:1px solid #e5e7eb;border-radius:8px;padding:7px 10px;background:#f8fafc;min-width:115px;'>");
        sb.append("<div style='font-size:10px;color:#6b7280;'>").append(escapeHtml(label)).append("</div>");
        sb.append("<div style='font-size:17px;font-weight:bold;color:#111827;'>").append(value).append("</div>");
        sb.append("<div style='font-size:10px;color:#6b7280;'>").append(escapeHtml(info)).append("</div>");
        sb.append("</div>");
    }

    private static void appendBar(StringBuffer sb, String label, int value, int max) {
        int width = max <= 0 ? 0 : (value * 100 / max);
        sb.append("<div style='display:flex;align-items:center;gap:6px;margin:3px 0;'>");
        sb.append("<div style='width:70px;font-size:10px;color:#4b5563;'>").append(escapeHtml(label)).append("</div>");
        sb.append("<div style='flex:1;height:7px;background:#e5e7eb;border-radius:999px;overflow:hidden;'>");
        sb.append("<div style='height:7px;width:").append(width).append("%;background:#3b82f6;'></div>");
        sb.append("</div>");
        sb.append("<div style='width:32px;text-align:right;font-size:10px;color:#111827;'>").append(value).append("</div>");
        sb.append("</div>");
    }

    private static void downloadParameterExcel(Map<String, Serializable> copyMapLocal, String nama) throws Exception {
        String fileName = "Parameter_" + sanitizeFileName(nama) + "_" + ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis()
                + ".xls";
        String html = buildExcelHtml(copyMapLocal, nama);
        AMedia amedia = new AMedia(fileName, "xls", "application/vnd.ms-excel", html);
        Filedownload.save(amedia);
    }

    private static String buildExcelHtml(Map<String, Serializable> copyMapLocal, String nama) {
        StringBuffer sb = new StringBuffer(8192 + (copyMapLocal == null ? 0 : copyMapLocal.size() * 256));
        sb.append("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />");
        sb.append("<style>");
        sb.append("body{font-family:Arial,sans-serif;font-size:11pt;color:#111827;}");
        sb.append("table{border-collapse:collapse;width:100%;}");
        sb.append("th{background:#1f2937;color:white;font-weight:bold;border:1px solid #d1d5db;padding:7px;text-align:left;}");
        sb.append("td{border:1px solid #d1d5db;padding:6px;vertical-align:top;mso-number-format:'\\@';}");
        sb.append(".title{font-size:16pt;font-weight:bold;margin-bottom:4px;}");
        sb.append(".meta{font-size:10pt;color:#4b5563;margin-bottom:12px;}");
        sb.append(".no{width:40px;text-align:center;}");
        sb.append(".key{width:260px;font-weight:bold;}");
        sb.append("</style></head><body>");
        sb.append("<div class='title'>Parameter Laporan ").append(escapeHtml(nama)).append("</div>");
        sb.append("<div class='meta'>Dibuat: ").append(escapeHtml(DATE_TIME_FORMAT.get().format(new Date()))).append(" | Total: ")
                .append(copyMapLocal == null ? 0 : copyMapLocal.size()).append(" parameter</div>");
        sb.append("<table>");
        sb.append("<tr><th class='no'>No</th><th class='key'>Key</th><th>Nilai</th><th>Tipe Data</th></tr>");

        if (copyMapLocal != null && !copyMapLocal.isEmpty()) {
            List<String> keys = new ArrayList<String>(copyMapLocal.keySet());
            Collections.sort(keys);
            for (int i = 0; i < keys.size(); i++) {
                String key = keys.get(i);
                Serializable value = copyMapLocal.get(key);
                sb.append("<tr>");
                sb.append("<td class='no'>").append(i + 1).append("</td>");
                sb.append("<td class='key'>").append(escapeHtml(key)).append("</td>");
                sb.append("<td>").append(escapeHtml(toDisplayText(value))).append("</td>");
                sb.append("<td>").append(escapeHtml(value == null ? "" : value.getClass().getName())).append("</td>");
                sb.append("</tr>");
            }
        } else {
            sb.append("<tr><td colspan='4'>Tidak ada parameter yang bisa ditampilkan.</td></tr>");
        }
        sb.append("</table></body></html>");
        return sb.toString();
    }

    public static File generateFileJasper(String file, String namaAsli) throws Exception {
        if (file == null || file.trim().length() == 0) {
            return null;
        }

        String fileTrim = file.trim();
        if (fileTrim.toLowerCase(Locale.ENGLISH).endsWith(".jasper")) {
            // FIX (root cause ERROR A - FileNotFoundException saat fillJasperReport):
            // sebelumnya baris ini langsung "return new File(fileTrim)" TANPA cek exists()
            // maupun upaya pemulihan apa pun. Kalau berkas .jasper ybs kebetulan hilang
            // secara fisik di server (mis. gagal ditulis saat kompilasi sebelumnya, terhapus
            // proses cleanup lain, atau belum sempat ter-deploy) maka fillJasperReport akan
            // menerima File yang tidak ada dan meledak dengan FileNotFoundException mentah
            // dari JRLoader (mis. laporan tagihan mahasiswa: "tagihan_9AC45F4D191E.jasper").
            // Sekarang: kalau berkas SUDAH ADA, tetap langsung dipakai (perilaku lama, tidak
            // berubah untuk kasus normal). Kalau TIDAK ADA, coba pulihkan lewat jalur yang
            // sama seperti nama laporan biasa di bawah (kompilasi ulang dari .jrxml
            // pendamping bernama sama, lalu fallback ke lampiran tersimpan di database).
            File fileJasperExt = new File(fileTrim);
            if (fileJasperExt.exists()) {
                return fileJasperExt;
            }
            String basePathNoExt = fileTrim.substring(0, fileTrim.length() - ".jasper".length());
            File fileJrxmlExt = new File(basePathNoExt + ".jrxml");
            buatFolderJikaPerlu(fileJasperExt);
            buatFolderJikaPerlu(fileJrxmlExt);
            pulihkanJasperJikaHilang(fileJasperExt, fileJrxmlExt, namaAsli);
            return fileJasperExt;
        }

        String baseFile = fileTrim;
        if (baseFile.toLowerCase(Locale.ENGLISH).endsWith(".jrxml")) {
            baseFile = baseFile.substring(0, baseFile.length() - 6);
        }

        boolean isAbsolutePath = new File(baseFile).isAbsolute();
        File fileJasper = buatFileReport(baseFile, ".jasper", isAbsolutePath);
        File fileJrxml = buatFileReport(baseFile, ".jrxml", isAbsolutePath);
        buatFolderJikaPerlu(fileJasper);
        buatFolderJikaPerlu(fileJrxml);

        pulihkanJasperJikaHilang(fileJasper, fileJrxml, namaAsli);
        return fileJasper;
    }

    /**
     * FIX (root cause ERROR A): dipisah dari {@link #generateFileJasper(String, String)}
     * supaya jalur ".jasper" langsung (sebelumnya early-return tanpa pengecekan apa pun)
     * dan jalur nama-laporan biasa sama-sama mendapat kesempatan pulih kalau berkas
     * .jasper ternyata hilang/gagal ditulis secara fisik -- baik dengan kompilasi ulang
     * dari .jrxml pendamping maupun fallback ke lampiran tersimpan di database
     * (LampiranLain). Aman dipanggil berulang: no-op kalau fileJasper sudah ada.
     */
    private static void pulihkanJasperJikaHilang(File fileJasper, File fileJrxml, String namaAsli) throws Exception {
        if (!fileJasper.exists()) {
            if (fileJrxml.exists()) {
                try {
                    compileReportToFileDenganFallbackUtf8(fileJrxml, fileJasper);
                } catch (Exception e) {
                    gunakanYangAsli(fileJasper, fileJrxml, namaAsli);
                }
            } else {
                gunakanYangAsli(fileJasper, fileJrxml, namaAsli);
            }
        }

        if (!fileJasper.exists()) {
            LampiranLain lampiran = LampiranLain.ambil(-10001L, REPORT_PREFIX + namaAsli);
            if (lampiran != null && lampiran.ambilFile() != null && lampiran.ambilFile().exists()) {
                try {
                    compileReportToFileDenganFallbackUtf8(lampiran.ambilFile(), fileJasper);
                } catch (Exception e) {
                    gunakanYangAsli(fileJasper, fileJrxml, namaAsli);
                }
            } else {
                gunakanYangAsli(fileJasper, fileJrxml, namaAsli);
            }
        }
    }

    private static File buatFileReport(String baseFile, String extension, boolean isAbsolutePath) {
        if (isAbsolutePath) {
            return new File(baseFile + extension);
        }
        return new File(Common.ambilREAL_PATH_REPORT() + "/" + baseFile + extension);
    }

    public static void gunakanYangAsli(File fileJasper, File fileJrxml, String namaAsli) throws Exception {
        LampiranLain lampiran = LampiranLain.ambil(-10001L, REPORT_PREFIX + namaAsli);
        FileOutputStream fileOutputStream = null;
        InputStream fileInputStream = null;
        boolean isCopied = false;

        try {
            if (lampiran != null && fileJrxml != null) {
                File file = lampiran.ambilFile();
                if (file != null && file.exists()) {
                    fileInputStream = new FileInputStream(file);
                } else {
                    java.sql.Blob fotoBlob = lampiran.getCopyDari() != null ? lampiran.getCopyDari().getFoto()
                            : lampiran.getFoto();
                    if (fotoBlob != null) {
                        fileInputStream = fotoBlob.getBinaryStream();
                    }
                }

                if (fileInputStream != null) {
                    buatFolderJikaPerlu(fileJrxml);
                    fileOutputStream = new FileOutputStream(fileJrxml);
                    IOUtils.copyLarge(fileInputStream, fileOutputStream);
                    isCopied = true;
                }
            }
        } finally {
            closeQuietly(fileInputStream);
            closeQuietly(fileOutputStream);
        }

        if (isCopied && fileJrxml != null && fileJrxml.exists()) {
            try {
                buatFolderJikaPerlu(fileJasper);
                compileReportToFileDenganFallbackUtf8(fileJrxml, fileJasper);
            } catch (Exception e) {
                Common.tampilErrorJikaAdmin(e);
                // KE-FIX (JRXML template rusak/bukan XML valid, mis. SAXParseException "Content is
                // not allowed in prolog" karena berkas .jrxml terpotong/corrupt/kosong): baik berkas
                // di disk maupun salinan cadangan dari LampiranLain (yang baru saja disalin ke
                // fileJrxml di atas) sama-sama tetap gagal di-parse sebagai XML walau sudah dicoba
                // dipulihkan lewat compileReportToFileDenganFallbackUtf8 (termasuk percobaan
                // perbaikan BOM/encoding). Ini BUKAN masalah data/filter laporan — pesan generik di
                // bawah menyuruh pengguna mengecek data/filter, yang tidak akan pernah menyelesaikan
                // masalah berkas template yang rusak. Beri diagnosis log yang jelas + pesan yang
                // akurat (arahkan ke admin), tanpa mengubah perilaku fallback yang sudah ada.
                if (adalahMasalahEncodingJrxml(e)) {
                    ais.common.ErrorAuditUtil.record(e,
                            "auto-audit(jrxml-template-corrupt) src/ais/action/report/helper/CommonReport.java template="
                                    + namaAsli);
                    PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Common Report",
                            "Berkas template laporan \"" + namaAsli + "\" rusak atau bukan format XML yang valid (kemungkinan terpotong/corrupt saat disimpan atau diunggah), sehingga tidak dapat dibaca sistem untuk membuat PDF laporan ini.",
                            e,
                            new String[] {
                                    "Ini BUKAN disebabkan oleh data, filter, atau periode yang Bapak/Ibu pilih — mengubahnya tidak akan menyelesaikan kendala ini.",
                                    "Template laporan ini perlu diperbaiki atau diunggah ulang oleh Administrator/Pengembang Sistem.",
                                    "Silakan hubungi Administrator atau laporkan ke Pengembang Sistem, sebutkan nama laporan (\"" + namaAsli + "\") dan sertakan tangkapan layar (screenshot) pesan ini."
                            });
                } else {
                    PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Common Report", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
                    		new String[] {
                    			"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
                    			"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
                    			"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
                    		});
                }
            }
        }
    }

    private static void compileReportToFileDenganFallbackUtf8(File fileJrxml, File fileJasper) throws Exception {
        try {
            JasperCompileManager.compileReportToFile(fileJrxml.getAbsolutePath(), fileJasper.getAbsolutePath());
        } catch (Exception e) {
            if (!adalahMasalahEncodingJrxml(e)) {
                throw e;
            }
            File fileJrxmlUtf8 = buatSalinanJrxmlUtf8(fileJrxml);
            try {
                JasperCompileManager.compileReportToFile(fileJrxmlUtf8.getAbsolutePath(), fileJasper.getAbsolutePath());
                ais.common.ErrorAuditUtil.record(e, "auto-audit(recovered-jrxml-encoding) src/ais/action/report/helper/CommonReport.java");
            } finally {
                if (fileJrxmlUtf8 != null && fileJrxmlUtf8.exists()) {
                    try {
                        fileJrxmlUtf8.delete();
                    } catch (Exception ex) {
                        ais.common.ErrorAuditUtil.record(ex, "auto-audit(delete-temp-jrxml-utf8) src/ais/action/report/helper/CommonReport.java");
                    }
                }
            }
        }
    }

    private static boolean adalahMasalahEncodingJrxml(Throwable e) {
        while (e != null) {
            String teks = (e.getClass().getName() + " " + (e.getMessage() == null ? "" : e.getMessage()))
                    .toLowerCase(Locale.ENGLISH);
            if (teks.contains("saxparseexception") || teks.contains("malformedbytesequenceexception")
                    || teks.contains("invalid byte") || teks.contains("utf-8")) {
                return true;
            }
            e = e.getCause();
        }
        return false;
    }

    private static File buatSalinanJrxmlUtf8(File fileJrxml) throws Exception {
        byte[] bytes = Files.readAllBytes(fileJrxml.toPath());
        String xml;
        try {
            xml = new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            xml = new String(bytes, Charset.forName("Windows-1252"));
        }
        if (xml.length() > 0 && xml.charAt(0) == '\uFEFF') {
            xml = xml.substring(1);
        }
        xml = xml.replaceFirst("(?is)<\\?xml\\s+version=(['\\\"])1\\.0\\1\\s+encoding=(['\\\"])[^'\\\"]+\\2\\s*\\?>",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml = bersihkanKarakterXmlTidakValid(xml);
        File temp = new File(fileJrxml.getParentFile(), fileJrxml.getName() + ".utf8fix.tmp.jrxml");
        Files.write(temp.toPath(), xml.getBytes(StandardCharsets.UTF_8));
        return temp;
    }

    private static String bersihkanKarakterXmlTidakValid(String text) {
        if (text == null || text.length() == 0) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == 0x9 || c == 0xA || c == 0xD || c >= 0x20) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @SuppressWarnings({})
    public static void inputMinggu(Tbmuser tbmuser, Integer mybulan, Integer mytahun) {
        if (tbmuser == null || tbmuser.getUserId() == null || mybulan == null || mytahun == null) {
            return;
        }

        Session session = null;
        try {
            session = HibernateUtil.currentSession();
            session.createSQLQuery("delete from sirs.minggu where tbmuser = :userId")
                    .setParameter("userId", tbmuser.getUserId()).executeUpdate();

            Calendar calendar = buatAwalMinggu(mybulan, mytahun);
            for (int i = 0; i < 10; i++) {
                Minggu minggu = new Minggu();
                minggu.setTbmuser(tbmuser);
                minggu.setTanggalMulai(calendar.getTime());

                calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 5);
                int tgl = calendar.get(Calendar.DATE);
                int bln = calendar.get(Calendar.MONTH);

                if (tgl + 4 > 25 && mybulan.equals(Integer.valueOf(bln + 1))) {
                    calendar.set(Calendar.DATE, 25);
                    minggu.setTanggalSampai(calendar.getTime());
                    session.save(minggu);
                    break;
                }

                minggu.setTanggalSampai(calendar.getTime());
                session.save(minggu);
                calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
            		new String[] {
            			"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
            			"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
            			"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
            		});
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void inputParameterTanggal(Map parameters, Integer mybulan, Integer mytahun) {
        if (parameters == null || mybulan == null || mytahun == null) {
            return;
        }

        for (int i = 0; i < 10; i++) {
            parameters.put("mg" + (i + 1), "1980-01-01");
            parameters.put("mg" + (i + 1) + "_1", "1980-01-01");
        }

        Calendar calendar = buatAwalMinggu(mybulan, mytahun);
        for (int i = 0; i < 10; i++) {
            parameters.put("mg" + (i + 1), DATE_FORMAT.get().format(calendar.getTime()));
            calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 5);
            int tgl = calendar.get(Calendar.DATE);
            int bln = calendar.get(Calendar.MONTH);

            if (tgl + 4 > 25 && mybulan.equals(Integer.valueOf(bln + 1))) {
                calendar.set(Calendar.DATE, 25);
                parameters.put("mg" + (i + 1) + "_1", DATE_FORMAT.get().format(calendar.getTime()));
                break;
            }

            parameters.put("mg" + (i + 1) + "_1", DATE_FORMAT.get().format(calendar.getTime()));
            calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
        }
    }

    public static List<Minggu> getMinggu(Integer mybulan, Integer mytahun) {
        List<Minggu> minggus = new ArrayList<Minggu>();
        if (mybulan == null || mytahun == null) {
            return minggus;
        }

        Calendar calendar = buatAwalMinggu(mybulan, mytahun);
        for (int i = 0; i < 10; i++) {
            Minggu minggu = new Minggu();
            minggu.setTanggalMulai(calendar.getTime());
            calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 5);
            int tgl = calendar.get(Calendar.DATE);
            int bln = calendar.get(Calendar.MONTH);

            if (tgl + 4 > 25 && mybulan.equals(Integer.valueOf(bln + 1))) {
                calendar.set(Calendar.DATE, 25);
                minggu.setTanggalSampai(calendar.getTime());
                minggus.add(minggu);
                break;
            }

            minggu.setTanggalSampai(calendar.getTime());
            minggus.add(minggu);
            calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
        }
        return minggus;
    }

    private static Calendar buatAwalMinggu(Integer mybulan, Integer mytahun) {
        Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
        calendar.set(Calendar.YEAR, mytahun.intValue());
        calendar.set(Calendar.MONTH, mybulan.intValue() - 2);
        calendar.set(Calendar.DATE, 26);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private static Locale ambilLocale(Radiogroup localeCombobox) {
        Locale locale = Common.locale;
        if (localeCombobox != null && localeCombobox.getSelectedItem() != null
                && localeCombobox.getSelectedItem().getAttribute("val") != null) {
            locale = (Locale) localeCombobox.getSelectedItem().getAttribute("val");
        }
        return locale;
    }

    private static void copyStream(InputStream inputStream, FileOutputStream outputStream) throws Exception {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
    }

    private static void buatFolderJikaPerlu(File file) {
        if (file != null && file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
    }

    private static void rollbackQuietly(Transaction tx) {
        if (tx != null) {
            try {
                tx.rollback();
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/CommonReport.java:1564");
            	PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
            		new String[] {
            			"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
            			"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
            			"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
            		});
            }
        }
    }

    private static void closeQuietly(InputStream inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/CommonReport.java:1573");
            	PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
            		new String[] {
            			"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
            			"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
            			"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
            		});
            }
        }
    }

    private static void closeQuietly(FileInputStream inputStream) {
        closeQuietly((InputStream) inputStream);
    }

    private static void closeQuietly(FileOutputStream outputStream) {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/CommonReport.java:1586");
            	PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
            		new String[] {
            			"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
            			"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
            			"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
            		});
            }
        }
    }

    private static void closeOpenSession(Session session) {
        if (session != null) {
            try {
                session.clear();
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/CommonReport.java:1595");
            	PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
            		new String[] {
            			"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
            			"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
            			"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
            		});
            }
            try {
                session.disconnect();
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/CommonReport.java:1599");
            	PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
            		new String[] {
            			"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
            			"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
            			"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
            		});
            }
            try {
                if (session.isOpen()) {
                    session.close();
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/helper/CommonReport.java:1605");
            	PesanFormalHelper.tampilkanGagalException("pemrosesan Common Report", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
            		new String[] {
            			"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
            			"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
            			"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
            		});
            }
        }
    }

    private static String toDisplayText(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Date) {
            return DATE_TIME_FORMAT.get().format((Date) value);
        }
        if (value instanceof Calendar) {
            return DATE_TIME_FORMAT.get().format(((Calendar) value).getTime());
        }
        if (value instanceof File) {
            return ((File) value).getAbsolutePath();
        }
        if (value instanceof InputStream) {
            return "InputStream";
        }
        if (value instanceof Blob) {
            return "Blob";
        }
        return String.valueOf(value);
    }

    private static String sanitizeFileName(String value) {
        if (value == null || value.trim().length() == 0) {
            return "report";
        }
        StringBuffer sb = new StringBuffer(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_'
                    || c == '-') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer(value.length() + 32);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '&') {
                sb.append("&amp;");
            } else if (c == '<') {
                sb.append("&lt;");
            } else if (c == '>') {
                sb.append("&gt;");
            } else if (c == '"') {
                sb.append("&quot;");
            } else if (c == '\'') {
                sb.append("&#39;");
            } else if (c == '\n') {
                sb.append("<br/>");
            } else if (c == '\r') {
                // abaikan, newline sudah ditangani oleh \n
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
