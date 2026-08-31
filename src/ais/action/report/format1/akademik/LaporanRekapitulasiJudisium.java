package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GelombangPendaftaranSidangTugasAkhir;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.JadwalSidangTugasAkhir;
import ais.database.model.Judisium;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Skripsi;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

/**
 * Jendela laporan rekapitulasi yudisium (predikat kelulusan) mahasiswa: menampilkan dashboard
 * HTML (via {@link LaporanSkripsiDashboardUtil}) dan mendukung cetak PDF gaya lama (via
 * {@link Report}). Data dasar diambil dari {@link Skripsi} yang sudah lulus (nilai total {@code >
 * 0}), difilter berdasarkan kombinasi fakultas/prodi, program, angkatan, status mahasiswa,
 * gelombang/jadwal sidang, predikat yudisium, dosen pembimbing/penguji (hingga 7 slot), dan
 * mahasiswa tertentu. Untuk tiap skripsi yang cocok, KRS mahasiswa disinkronkan ulang
 * ({@link Common#singkronkanKrsMahasiswa}, opsional menghitung ulang IP/IPK) untuk mengambil
 * SKS/IPK terkini dan status histori mahasiswa saat kelulusan, lalu predikat dihitung
 * ({@link Common#hitungJudisium}) bila belum tersimpan pada mahasiswa. Pemrosesan data berjalan
 * asinkron di thread terpisah dengan indikator progres (persentase mahasiswa terproses)
 * ditampilkan ke pengguna.
 */
public class LaporanRekapitulasiJudisium extends MyWindow {

    private static final long serialVersionUID = 4766478176972379068L;

    private Combobox fakultas;
    private Combobox jurusan;
    private Intbox angkatan;
    private Combobox status;
    private AmbilDataDosenBanbox searchdosen;
    private AmbilDataMahasiswaBanbox searchmahasiswa;
    private Combobox tahunAkademik;
    private MyCheckboxConfig hitungUlang = new MyCheckboxConfig("Hitung Ulang IP/IPK");
    private Center center;
    private Toolbar toolbar;
    private Combobox program;
    private Combobox gelombang;
    private Combobox judisium;
    private Combobox jadwalSidang;

    @SuppressWarnings("rawtypes")
    private List<Map> maps = null;

    /** Konstruktor default: menyiapkan combo fakultas/jurusan dan membangun kerangka jendela; kegagalan pemuatan awal ditangani dengan pesan formal ({@link PesanFormalHelper}) berisi langkah pemulihan bagi pengguna. */
    public LaporanRekapitulasiJudisium() {
        super();
        try {
            initComboboxFakultasJurusan();
            init();
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekapitulasi Judisium", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
            		new String[] {
            			"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
            			"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
            			"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
            		});
        }
    }

    /** Seperti konstruktor default, dengan judul/border/closable jendela yang dapat disesuaikan. */
    public LaporanRekapitulasiJudisium(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        initComboboxFakultasJurusan();
        init();
    }

    /** Menyiapkan combobox fakultas/jurusan berpasangan (jurusan mengikuti pilihan fakultas). */
    private void initComboboxFakultasJurusan() {
        fakultas = new Combobox();
        jurusan = new Combobox();
        Common.initFakultasDanJurusan(fakultas, jurusan, null, null);
    }

    /** Membangun kerangka jendela: panel filter di barat (lihat {@link #buildFilterRows}), area konten dashboard di tengah, dan toolbar ekspor/cetak di utara ({@link CommonReport#exportReport}), lalu menampilkan dashboard kosong awal. */
    @SuppressWarnings("deprecation")
    private void init() throws Exception {
        Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
        borderlayout.setParent(this);

        West west = new West();
        west.setTitle("Filter Laporan");
        west.setCollapsible(true);
        west.setParent(borderlayout);
        ais.ui.util.ZkCompat.setFlex(west, true);
        west.setWidth("360px");

        MyGrid grid = new MyGrid();
        grid.setWidth("100%");
        grid.setHeight("100%");
        grid.setParent(west);

        Columns columns = new Columns();
        columns.setParent(grid);
        MyColumnConfig column = new MyColumnConfig();
        column.setWidth("40%");
        column.setParent(columns);
        column = new MyColumnConfig();
        column.setParent(columns);

        Rows rows = new Rows();
        rows.setParent(grid);
        buildFilterRows(rows);

        center = new Center();
        center.setParent(borderlayout);
        ais.ui.util.ZkCompat.setFlex(center, true);

        North north = new North();
        north.setParent(borderlayout);
        north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            public Map<String, Serializable> generateParameters() throws Exception {
                return generateParameter();
            }
        }, "Laporan_judisium_mahasiswa", null, new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                onCetakLama(arg0);
            }
        }));

        tampilkanDashboardAwal();
    }

    private void buildFilterRows(Rows rows) {
        tahunAkademik = new Combobox();
        tahunAkademik = Common.generateTahunAjaran(tahunAkademik);
        tahunAkademik.setWidth("92%");
        tambahRow(rows, "Tahun Akademik", tahunAkademik);

        tambahRow(rows, "Fakultas", fakultas);
        fakultas.setWidth("92%");
        tambahRow(rows, "Prodi", jurusan);
        jurusan.setWidth("92%");

        program = Common.initPrograms(null);
        program.setWidth("92%");
        program.setReadonly(true);
        tambahRow(rows, "Program", program);

        angkatan = new Intbox();
        angkatan.setWidth("92%");
        tambahRow(rows, "Angkatan", angkatan);

        status = new Combobox();
        Common.insertComboDanSemua(status, new String[] { "nama", "kodeEpsbed" }, StatusMahasiswa.class);
        status.setWidth("92%");
        tambahRow(rows, "Status Mahasiswa", status);

        gelombang = new Combobox();
        Common.insertComboDanSemua(gelombang, new String[] { "nama", "tahunAkademik", "jurusan" }, "keterangan",
                GelombangPendaftaranSidangTugasAkhir.class, Restrictions.eq("aktif", true));
        gelombang.setWidth("92%");
        tambahRow(rows, "Gelombang", gelombang);

        jadwalSidang = new Combobox();
        Common.insertComboDanSemua(jadwalSidang, new String[] { "nama", "tahunAkademik", "jurusan", "mulai", "sampai" },
                "keterangan", JadwalSidangTugasAkhir.class);
        jadwalSidang.setWidth("92%");
        tambahRow(rows, "Jadwal Sidang", jadwalSidang);

        judisium = new Combobox();
        Common.insertComboDanSemua(judisium, new String[] { "nama", "jenjang", "nilaiMulai", "nilaiSampai" },
                "keterangan", Judisium.class, Restrictions.eq("aktif", true));
        judisium.setWidth("92%");
        tambahRow(rows, "Predikat", judisium);

        searchdosen = new AmbilDataDosenBanbox();
        searchdosen.setWidth("92%");
        tambahRow(rows, "Dosen", searchdosen);

        searchmahasiswa = new AmbilDataMahasiswaBanbox();
        searchmahasiswa.setWidth("92%");
        tambahRow(rows, "Mahasiswa", searchmahasiswa);

        MyFormRow cek = new MyFormRow();
        cek.setParent(rows);
        cek.appendChild(new ais.ui.util.MyLabelConfig("Perhitungan"));
        cek.appendChild(hitungUlang);
        hitungUlang.setChecked(true);

        MyFormRow row = new MyFormRow();
        row.setParent(rows);
        ais.ui.util.ZkCompat.setSpans(row, "2");
        MyButtonConfig tampil = new MyButtonConfig("Tampilkan Dashboard");
        tampil.setParent(row);
        tampil.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                onLaporan(event);
            }
        });

        MyButtonConfig cetak = new MyButtonConfig("Cetak PDF Lama");
        cetak.setParent(row);
        cetak.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                onCetakLama(event);
            }
        });
    }

    private void tambahRow(Rows rows, String label, org.zkoss.zk.ui.Component component) {
        MyFormRow row = new MyFormRow();
        row.setValign("top");
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig(label));
        row.appendChild(component);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Map generateParameter() throws Exception {
        Dosen dosen = getSelectedDosen();
        Mahasiswa mahasiswa = getSelectedMahasiswa();
        Map parameters = ais.common.HashMapGenerator.getRand();
        parameters.put("fakultas", getSelectedFakultasId());
        parameters.put("dosen", dosen == null || dosen.getId() == null ? -1L : dosen.getId());
        parameters.put("mahasiswa", mahasiswa == null || mahasiswa.getId() == null ? -1L : mahasiswa.getId());
        parameters.put("program", getSelectedProgram());
        parameters.put("jurusan", getSelectedJurusanId());
        parameters.put("angkatan", angkatan.getValue() == null ? new Integer(-1) : angkatan.getValue());
        parameters.put("status", getSelectedStatusMahasiswaId());
        parameters.put("current_ta", getSelectedTahunAkademik());
        if (maps != null) {
            parameters.put("maps", maps);
        }
        return parameters;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void generateDataDanImageAlbum(Label label) {
        maps = new ArrayList<Map>();
        Judisium selectedJudisium = getSelectedJudisium();
        Dosen dosen = getSelectedDosen();
        Mahasiswa mahasiswa = getSelectedMahasiswa();
        Session session = HibernateUtil.currentSession();
        Criteria c = session.createCriteria(Skripsi.class)
                .setProjection(Projections.property("id"))
                .add(getSelectedJadwalSidang() == null ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.eq("jadwalSidangTugasAkhir", getSelectedJadwalSidang()))
                .add(getSelectedGelombang() == null ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.eq("gelombangPendaftaranSidangTugasAkhir", getSelectedGelombang()))
                .createAlias("gelombangPendaftaranSidangTugasAkhir", "gelombangPendaftaranSidangTugasAkhir")
                .addOrder(Order.asc("gelombangPendaftaranSidangTugasAkhir.mulai"))
                .addOrder(Order.asc("gelombangPendaftaranSidangTugasAkhir.id"))
                .add(getSelectedTahunAkademik() == null ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.eq("tahunAkademik", getSelectedTahunAkademik()))
                .add(Restrictions.eq("lulus", true))
                .add(Restrictions.ge("totalNilai", new Double(0.1)))
                .add(mahasiswa == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("mahasiswa", mahasiswa))
                .createAlias("mahasiswa", "mahasiswa").createAlias("mahasiswa.jurusan", "jurusan")
                .add(angkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.eq("mahasiswa.tahunangkatan", angkatan.getValue()))
                .add(getSelectedFakultasObject() == null ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.eq("jurusan.fakultas", getSelectedFakultasObject()))
                .add(getSelectedJurusanObject() == null ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.eq("mahasiswa.jurusan", getSelectedJurusanObject()))
                .add(getSelectedProgram().equals("-1") ? Restrictions.sqlRestriction("1=1")
                        : Restrictions.eq("mahasiswa.program", getSelectedProgram()));

        if (dosen != null) {
            c.add(buildDosenCriterion(dosen));
        }

        List<Long> skripsis = c.list();
        int size = skripsis == null ? 0 : skripsis.size();
        for (int index = 0; skripsis != null && index < size; index++) {
            Skripsi skripsi = (Skripsi) ConstantValues.ambil(Skripsi.class.getName(), skripsis.get(index));
            if (skripsi == null || skripsi.getMahasiswa() == null) {
                continue;
            }
            Mahasiswa mhs = skripsi.getMahasiswa();
            updateLabel(label, mhs, index, size, null);
            KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mhs, skripsi.getSemester(), null, null,
                    hitungUlang.isChecked());
            HistoryStatusMahasiswa historyStatus = getHistoryStatus(krsMahasiswa);
            if (!cocokStatusMahasiswa(historyStatus)) {
                continue;
            }
            Judisium predikat = mhs.getPredikatKelulusan() == null ? Common.hitungJudisium(mhs, krsMahasiswa)
                    : mhs.getPredikatKelulusan();
            if (selectedJudisium != null
                    && (predikat == null || predikat.getId() == null || !selectedJudisium.getId().equals(predikat.getId()))) {
                continue;
            }
            Map map = new HashMap();
            map.put("gelombang_id", skripsi.getGelombangPendaftaranSidangTugasAkhir() == null ? null
                    : skripsi.getGelombangPendaftaranSidangTugasAkhir().getId());
            map.put("gelombang", skripsi.getGelombangPendaftaranSidangTugasAkhir() == null ? "Tanpa Gelombang"
                    : skripsi.getGelombangPendaftaranSidangTugasAkhir().getNama());
            map.put("fakult", mhs.getJurusan() == null || mhs.getJurusan().getFakultas() == null ? ""
                    : mhs.getJurusan().getFakultas().getNama());
            map.put("nim", mhs.getNim());
            map.put("nama", mhs.getNama());
            map.put("sks", krsMahasiswa == null ? new Integer(0) : krsMahasiswa.getSksYangDiambil());
            map.put("sksk", krsMahasiswa == null ? new Integer(0) : krsMahasiswa.getSksk());
            map.put("ips", krsMahasiswa == null ? new Double(0.0) : krsMahasiswa.getIps());
            map.put("ipk", krsMahasiswa == null ? new Double(0.0) : krsMahasiswa.getIpk());
            map.put("nilai", skripsi.getTotalNilai());
            map.put("ip", skripsi.getTotalIP());
            map.put("huruf", skripsi.getNilaiHuruf());
            map.put("judisium", predikat == null ? "" : predikat.getNama());
            map.put("tanggal_lulus", mhs.getTanggalLulus());
            map.put("status_aktif", historyStatus == null || historyStatus.getStatusMahasiswa() == null ? ""
                    : historyStatus.getStatusMahasiswa().getNama());
            maps.add(map);
        }
        updateLabel(label, null, 0, 0, "");
    }

    public void onLaporan(Event event) throws Exception {
        final Label label = Common.displayLoadBar(new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                renderDashboard();
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    generateDataDanImageAlbum(label);
                } catch (Exception e) {
                    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanRekapitulasiJudisium.java:352");
                    updateLabel(label, null, 0, 0, "Gagal memproses data yudisium.");
                }
            }
        }).start();
    }

    public void onCetakLama(Event event) throws Exception {
        final Label label = Common.displayLoadBar(new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Laporan_judisium_mahasiswa",
                        ais.ui.util.WaktuUtil.getDate(), null, toolbar);
                CommonReport.tampilkanReportPDF(center, file);
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    generateDataDanImageAlbum(label);
                } catch (Exception e) {
                    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanRekapitulasiJudisium.java:374");
                    updateLabel(label, null, 0, 0, "Gagal memproses cetak yudisium.");
                }
            }
        }).start();
    }

    private void renderDashboard() {
        center.getChildren().clear();
        String html = (maps == null || maps.isEmpty())
                ? LaporanSkripsiDashboardUtil.empty("Dashboard Rekapitulasi Yudisium", getFilterInfo())
                : LaporanSkripsiDashboardUtil.renderJudisium(maps, getFilterInfo());
        center.appendChild(new Html(html));
    }

    private void tampilkanDashboardAwal() {
        center.getChildren().clear();
        center.appendChild(new Html(LaporanSkripsiDashboardUtil.empty("Dashboard Rekapitulasi Yudisium", getFilterInfo())));
    }

    private Criterion buildDosenCriterion(Dosen dosen) {
        Criterion cc = Restrictions.or(Restrictions.eq("pembimbing", dosen), Restrictions.eq("ketuaSidang", dosen));
        cc = Restrictions.or(cc, Restrictions.eq("penguji1", dosen));
        cc = Restrictions.or(cc, Restrictions.eq("penguji2", dosen));
        cc = Restrictions.or(cc, Restrictions.eq("penguji3", dosen));
        cc = Restrictions.or(cc, Restrictions.eq("penguji4", dosen));
        cc = Restrictions.or(cc, Restrictions.eq("penguji5", dosen));
        cc = Restrictions.or(cc, Restrictions.eq("pembimbing3", dosen));
        return cc;
    }

    private Dosen getSelectedDosen() {
        Dosen dosen = searchdosen == null ? null : (Dosen) searchdosen.getAttribute("dosen");
        if (dosen == null && searchdosen != null) {
            dosen = (Dosen) searchdosen.getAttribute("myValue");
        }
        return dosen;
    }

    private Mahasiswa getSelectedMahasiswa() {
        return searchmahasiswa == null ? null : (Mahasiswa) searchmahasiswa.getAttribute("mahasiswa");
    }

    private Fakultas getSelectedFakultasObject() {
        return fakultas == null || fakultas.getSelectedItem() == null ? null : (Fakultas) fakultas.getSelectedItem().getValue();
    }

    private Long getSelectedFakultasId() {
        Fakultas f = getSelectedFakultasObject();
        return f == null || f.getId() == null ? -1L : f.getId();
    }

    private Jurusan getSelectedJurusanObject() {
        return jurusan == null || jurusan.getSelectedItem() == null ? null : (Jurusan) jurusan.getSelectedItem().getValue();
    }

    private Long getSelectedJurusanId() {
        Jurusan j = getSelectedJurusanObject();
        return j == null || j.getId() == null ? -1L : j.getId();
    }

    private String getSelectedProgram() {
        return program == null || program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? "-1"
                : String.valueOf(program.getSelectedItem().getValue());
    }

    private String getSelectedTahunAkademik() {
        return tahunAkademik == null || tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null
                ? null : String.valueOf(tahunAkademik.getSelectedItem().getValue());
    }

    private StatusMahasiswa getSelectedStatusMahasiswa() {
        return status == null || status.getSelectedItem() == null || status.getSelectedItem().getValue() == null ? null
                : (StatusMahasiswa) status.getSelectedItem().getValue();
    }

    private Long getSelectedStatusMahasiswaId() {
        StatusMahasiswa s = getSelectedStatusMahasiswa();
        return s == null || s.getId() == null ? -1L : s.getId();
    }

    private boolean cocokStatusMahasiswa(HistoryStatusMahasiswa historyStatus) {
        StatusMahasiswa selected = getSelectedStatusMahasiswa();
        if (selected == null) {
            return true;
        }
        return historyStatus != null && historyStatus.getStatusMahasiswa() != null && selected.getId() != null
                && selected.getId().equals(historyStatus.getStatusMahasiswa().getId());
    }

    private HistoryStatusMahasiswa getHistoryStatus(KrsMahasiswa krsMahasiswa) {
        try {
            return krsMahasiswa == null ? null : ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(krsMahasiswa);
        } catch (Exception e) {
            return null;
        }
    }

    private GelombangPendaftaranSidangTugasAkhir getSelectedGelombang() {
        return gelombang == null || gelombang.getSelectedItem() == null || gelombang.getSelectedItem().getValue() == null
                ? null : (GelombangPendaftaranSidangTugasAkhir) gelombang.getSelectedItem().getValue();
    }

    private JadwalSidangTugasAkhir getSelectedJadwalSidang() {
        return jadwalSidang == null || jadwalSidang.getSelectedItem() == null || jadwalSidang.getSelectedItem().getValue() == null
                ? null : (JadwalSidangTugasAkhir) jadwalSidang.getSelectedItem().getValue();
    }

    private Judisium getSelectedJudisium() {
        return judisium == null || judisium.getSelectedItem() == null || judisium.getSelectedItem().getValue() == null ? null
                : (Judisium) judisium.getSelectedItem().getValue();
    }

    private String getFilterInfo() {
        String ta = getSelectedTahunAkademik() == null ? "Semua TA" : getSelectedTahunAkademik();
        String prodi = getSelectedJurusanObject() == null ? "Semua Prodi" : getSelectedJurusanObject().getNama();
        String gel = getSelectedGelombang() == null ? "Semua Gelombang" : getSelectedGelombang().getNama();
        return ta + " • " + prodi + " • " + gel;
    }

    private void updateLabel(Label label, Mahasiswa mahasiswa, int index, int size, String message) {
        if (label == null) {
            return;
        }
        if (message != null) {
            label.setValue(message);
            return;
        }
        if (mahasiswa != null && size > 0) {
            label.setValue("Memproses data " + mahasiswa + " (" + Common.numberFormat.get().format((index * 100.0) / size)
                    + "%)");
        }
    }
}
