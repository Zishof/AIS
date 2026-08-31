package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
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
import ais.common.IndonesianNumberToWords;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.JadwalSidangTugasAkhir;
import ais.database.model.Judisium;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Skripsi;
import ais.database.model.StatusKeluar;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

/**
 * Jendela laporan rekapitulasi sidang tugas akhir/skripsi ({@link Skripsi}): untuk setiap peran
 * pembimbing/penguji (Pembimbing I/II, Penguji I-V — 7 peran, masing-masing kolom dosen berbeda
 * pada {@link Skripsi}), menghasilkan satu baris data per (skripsi, peran) yang dosennya terisi,
 * difilter fakultas/jurusan/program/angkatan/status mahasiswa/status keluar/dosen/mahasiswa/tahun
 * akademik/semester/status sidang. Data ditampilkan sebagai dashboard HTML
 * ({@link LaporanSkripsiDashboardUtil}) yang dimuat asinkron di thread terpisah (dengan progress
 * bar), atau dapat dicetak sebagai PDF lewat mesin laporan lama ({@link Report}). Bila dibuka
 * untuk satu {@link JadwalSidangTugasAkhir} tertentu, filter TA dikosongkan dan laporan langsung
 * dimuat. Setiap baris data diperkaya dengan snapshot akademik mahasiswa (IPK/IPS, judisium,
 * status aktif historis, dosen PA) lewat {@link #buildMapSidang}.
 */
public class LaporanRekapitulasiSidang extends MyWindow {

    private static final long serialVersionUID = 4766478176972379068L;

    private Combobox fakultas;
    private Combobox jurusan;
    private Intbox angkatan;
    private Combobox status;
    private AmbilDataDosenBanbox searchdosen;
    private AmbilDataMahasiswaBanbox searchmahasiswa;
    private Center center;
    private Toolbar toolbar;
    private JadwalSidangTugasAkhir jadwalSidangTugasAkhir;
    private Combobox program;
    private Combobox statusLulus;
    private Combobox searchTahunAkademik;
    private Combobox searchSemesterAbsensi;
    private Combobox searchsidang;

    @SuppressWarnings("rawtypes")
    private List<Map> maps = null;

    /** Membuat jendela laporan tanpa jadwal sidang spesifik (mode rekapitulasi umum) dan langsung menyusun tampilan dasar. */
    public LaporanRekapitulasiSidang() {
        super();
        try {
            initComboboxFakultasJurusan();
            init();
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekapitulasi Sidang", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
            		new String[] {
            			"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
            			"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
            			"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
            		});
        }
    }

    /**
     * Membuat jendela laporan terikat ke satu {@code jadwalSidangTugasAkhir}: filter tahun
     * akademik dikosongkan (semua TA) dan laporan langsung dimuat setelah tampilan tersusun.
     *
     * @param jadwalSidangTugasAkhir jadwal sidang yang menjadi cakupan laporan
     */
    public LaporanRekapitulasiSidang(JadwalSidangTugasAkhir jadwalSidangTugasAkhir) {
        super();
        this.jadwalSidangTugasAkhir = jadwalSidangTugasAkhir;
        try {
            initComboboxFakultasJurusan();
            init();
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekapitulasi Sidang", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
            		new String[] {
            			"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
            			"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
            			"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
            		});
        }
    }

    /**
     * Membuat jendela laporan dengan judul, gaya border, dan status dapat-ditutup kustom.
     *
     * @param title    judul jendela
     * @param border   gaya border jendela
     * @param closable apakah jendela dapat ditutup pengguna
     */
    public LaporanRekapitulasiSidang(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        initComboboxFakultasJurusan();
        init();
    }

    /** Menyiapkan kombo fakultas/jurusan berpasangan (jurusan disaring ulang sesuai fakultas terpilih). */
    private void initComboboxFakultasJurusan() {
        fakultas = new Combobox();
        jurusan = new Combobox();
        Common.initFakultasDanJurusan(fakultas, jurusan, null, null);
    }

    /** Menyusun tata letak (panel filter di West, toolbar ekspor di North, area dashboard di Center), menampilkan dashboard kosong awal, dan langsung memuat laporan bila jendela dibuka untuk satu jadwal sidang tertentu. */
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
        }, "Rekap_sidang_mahasiswa", null, new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                onCetakLama(arg0);
            }
        }));

        tampilkanDashboardAwal();
        if (jadwalSidangTugasAkhir != null) {
            onLaporan(null);
        }
    }

    /** Menyusun seluruh baris filter (fakultas, prodi, program, angkatan, status mahasiswa/keluar, dosen, mahasiswa, tahun akademik, semester, status sidang) beserta tombol tampil dashboard dan cetak PDF lama. */
    private void buildFilterRows(Rows rows) {
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

        statusLulus = new Combobox();
        Common.insertComboDanSemua(statusLulus, new String[] { "nama" }, StatusKeluar.class);
        statusLulus.setWidth("92%");
        tambahRow(rows, "Status Keluar", statusLulus);

        searchdosen = new AmbilDataDosenBanbox();
        searchdosen.setWidth("92%");
        tambahRow(rows, "Dosen", searchdosen);

        searchmahasiswa = new AmbilDataMahasiswaBanbox();
        searchmahasiswa.setWidth("92%");
        tambahRow(rows, "Mahasiswa", searchmahasiswa);

        Common.generateTahunAjaranDanSemua(searchTahunAkademik = new Combobox());
        Common.selectComboItem(searchTahunAkademik,
                jadwalSidangTugasAkhir == null ? Common.getCurrentTahunAkademik() : null);
        searchTahunAkademik.setWidth("92%");
        searchTahunAkademik.setReadonly(true);
        tambahRow(rows, "TA", searchTahunAkademik);

        searchSemesterAbsensi = new Combobox();
        org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
        comboitem.setLabel(Perkuliahan.GENAP);
        comboitem.setValue(Perkuliahan.GENAP);
        searchSemesterAbsensi.appendChild(comboitem);
        comboitem = new MyComboitemConfig();
        comboitem.setLabel(Perkuliahan.GANJIL);
        comboitem.setValue(Perkuliahan.GANJIL);
        searchSemesterAbsensi.appendChild(comboitem);
        comboitem = new MyComboitemConfig();
        comboitem.setLabel("Semua");
        comboitem.setValue(null);
        searchSemesterAbsensi.appendChild(comboitem);
        Common.selectComboItem(searchSemesterAbsensi, null);
        searchSemesterAbsensi.setReadonly(true);
        tambahRow(rows, "Semester", searchSemesterAbsensi);

        searchsidang = new Combobox();
        comboitem = new org.zkoss.zul.Comboitem();
        comboitem.setLabel("Sudah sidang");
        comboitem.setValue(new Integer(1));
        searchsidang.appendChild(comboitem);
        comboitem = new MyComboitemConfig();
        comboitem.setLabel("Belum sidang");
        comboitem.setValue(new Integer(0));
        searchsidang.appendChild(comboitem);
        comboitem = new MyComboitemConfig();
        comboitem.setLabel("Semua");
        comboitem.setValue(null);
        searchsidang.appendChild(comboitem);
        searchsidang.setReadonly(true);
        searchsidang.setSelectedItem(comboitem);
        tambahRow(rows, "Sidang", searchsidang);

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

    /** Menambahkan satu baris label+komponen ke {@code rows} (helper tata letak formulir filter). */
    private void tambahRow(Rows rows, String label, org.zkoss.zk.ui.Component component) {
        MyFormRow row = new MyFormRow();
        row.setValign("top");
        row.setParent(rows);
        row.appendChild(new ais.ui.util.MyLabelConfig(label));
        row.appendChild(component);
    }

    /** @return peta parameter laporan (filter terpilih + {@link #maps} bila sudah dihitung) untuk mesin cetak PDF lama. */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Map generateParameter() throws Exception {
        Dosen dosen = getSelectedDosen();
        Mahasiswa mahasiswa = getSelectedMahasiswa();
        Map parameters = ais.common.HashMapGenerator.getRand();
        parameters.put("fakultas", getSelectedFakultasId());
        parameters.put("dosen", dosen == null || dosen.getId() == null ? -1L : dosen.getId());
        parameters.put("mahasiswa", mahasiswa == null || mahasiswa.getId() == null ? -1L : mahasiswa.getId());
        parameters.put("jadwal", jadwalSidangTugasAkhir == null || jadwalSidangTugasAkhir.getId() == null ? -1L : jadwalSidangTugasAkhir.getId());
        parameters.put("program", getSelectedProgram());
        parameters.put("jurusan", getSelectedJurusanId());
        parameters.put("angkatan", angkatan.getValue() == null ? new Integer(-1) : angkatan.getValue());
        parameters.put("status", getSelectedStatusMahasiswaId());
        parameters.put("current_ta", getSelectedTahunAkademik());
        parameters.put("current_semester", getSelectedSemesterLabel());
        if (maps != null) {
            parameters.put("maps", maps);
        }
        return parameters;
    }

    /**
     * Menghitung ulang {@link #maps} (satu baris per skripsi+peran dosen yang terisi): untuk
     * masing-masing dari 7 peran (Pembimbing I/II, Penguji I-V), mengambil id
     * {@link Skripsi} yang cocok dengan seluruh filter formulir dan kolom dosen peran tersebut
     * tidak kosong, lalu untuk tiap skripsi menyinkronkan {@link KrsMahasiswa} terkait, mengambil
     * status historis mahasiswa, melewati baris bila tidak cocok filter status mahasiswa
     * ({@link #cocokStatusMahasiswa}), dan membangun baris data lewat {@link #buildMapSidang}.
     * Progres ditulis ke {@code label} di setiap iterasi.
     *
     * @param label komponen label UI untuk menampilkan progres, boleh {@code null}
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void generateDataDanImageAlbum(Label label) {
        maps = new ArrayList<Map>();
        Dosen dosenPemimbing = getSelectedDosen();
        Mahasiswa mahasiswaFilter = getSelectedMahasiswa();
        Session session = HibernateUtil.currentSession();

        TreeMap<String, String> roles = new TreeMap<String, String>();
        roles.put("01. Pembimbing I", "pembimbing");
        roles.put("02. Pembimbing II", "ketuaSidang");
        roles.put("03. Penguji I", "penguji1");
        roles.put("04. Penguji II", "penguji2");
        roles.put("05. Penguji III", "penguji3");
        roles.put("06. Penguji IV", "penguji4");
        roles.put("07. Penguji V", "penguji5");

        ClassMetadata classMetadata = HibernateUtil.getClassMetadata(Skripsi.class);
        for (String namaPeran : roles.keySet()) {
            String kolomDosen = roles.get(namaPeran);
            Criteria criteria = session.createCriteria(Skripsi.class)
                    .add(jadwalSidangTugasAkhir == null ? Restrictions.sqlRestriction("1=1")
                            : Restrictions.eq("jadwalSidangTugasAkhir", jadwalSidangTugasAkhir))
                    .add(Restrictions.isNotNull(kolomDosen))
                    .createAlias("mahasiswa", "mahasiswa").createAlias("mahasiswa.jurusan", "jurusan")
                    .add(statusLulus.getSelectedItem() == null || statusLulus.getSelectedItem().getValue() == null
                            ? Restrictions.sqlRestriction("1=1")
                            : Restrictions.eq("mahasiswa.statusKeluar", statusLulus.getSelectedItem().getValue()))
                    .add(getSelectedProgram().equals("-1") ? Restrictions.sqlRestriction("1=1")
                            : Restrictions.eq("mahasiswa.program", getSelectedProgram()))
                    .add(getSelectedJurusanObject() == null ? Restrictions.sqlRestriction("1=1")
                            : Restrictions.eq("mahasiswa.jurusan", getSelectedJurusanObject()))
                    .add(getSelectedFakultasObject() == null ? Restrictions.sqlRestriction("1=1")
                            : Restrictions.eq("jurusan.fakultas", getSelectedFakultasObject()))
                    .add(angkatan.getValue() == null || angkatan.getValue().intValue() < 1900
                            ? Restrictions.sqlRestriction("1=1")
                            : Restrictions.eq("mahasiswa.tahunangkatan", angkatan.getValue()))
                    .add(getSelectedTahunAkademik() == null ? Restrictions.sqlRestriction("1=1")
                            : Restrictions.eq("tahunAkademik", getSelectedTahunAkademik()))
                    .add(getSelectedSemesterValue() == null ? Restrictions.sqlRestriction("1=1")
                            : Restrictions.sqlRestriction("this_.semester%2="
                                    + (Perkuliahan.GANJIL.equals(getSelectedSemesterValue()) ? "1" : "0")))
                    .add(searchsidang.getSelectedItem() == null || searchsidang.getSelectedItem().getValue() == null
                            ? Restrictions.sqlRestriction("1=1")
                            : Restrictions.eq("telahSidang", searchsidang.getSelectedItem().getValue()))
                    .add(mahasiswaFilter == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("mahasiswa", mahasiswaFilter))
                    .add(dosenPemimbing == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq(kolomDosen, dosenPemimbing))
                    .addOrder(Order.desc("jadwalSidangTugasAkhir.id")).addOrder(Order.desc("mahasiswa.nim"))
                    .setProjection(Projections.property("id"));

            List<Long> skripsis = criteria.list();
            int size = skripsis == null ? 0 : skripsis.size();
            for (int index = 0; skripsis != null && index < size; index++) {
                Long skripsiId = skripsis.get(index);
                Skripsi skripsi = (Skripsi) ConstantValues.ambil(Skripsi.class.getName(), skripsiId);
                if (skripsi == null || skripsi.getMahasiswa() == null) {
                    continue;
                }
                Mahasiswa mahasiswa = skripsi.getMahasiswa();
                updateLabel(label, mahasiswa, index, size, "Memproses " + namaPeran + " - " + mahasiswa);
                KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, skripsi.getSemester(), null, null);
                HistoryStatusMahasiswa historyStatus = getHistoryStatus(krsMahasiswa);
                if (!cocokStatusMahasiswa(historyStatus)) {
                    continue;
                }
                Map map = buildMapSidang(skripsi, mahasiswa, krsMahasiswa, historyStatus);
                map.put("pembimbing", namaPeran.substring(4));
                try {
                    Dosen dosen = (Dosen) classMetadata.getPropertyValue(skripsi, kolomDosen, EntityMode.POJO);
                    map.put("dosen", dosen == null ? "" : dosen.getNama());
                    map.put("dosen_id", dosen == null || dosen.getId() == null ? -1L : dosen.getId());
                } catch (Exception e) {
                    map.put("dosen", "");
                    map.put("dosen_id", -1L);
                }
                map.put("status", getNilaiPeran(skripsi, kolomDosen));
                maps.add(map);
            }
        }
        updateLabel(label, null, 0, 0, "");
    }

    /** @return nilai yang diberikan pada peran {@code kolomDosen} (pembimbing/ketuaSidang/penguji1-5) untuk {@code skripsi}, atau {@code null} bila peran tidak dikenal. */
    private Object getNilaiPeran(Skripsi skripsi, String kolomDosen) {
        if ("pembimbing".equals(kolomDosen)) {
            return skripsi.getNilaiPembimbing();
        }
        if ("ketuaSidang".equals(kolomDosen)) {
            return skripsi.getNilaiKetuaSidang();
        }
        if ("penguji1".equals(kolomDosen)) {
            return skripsi.getNilaiPenguji1();
        }
        if ("penguji2".equals(kolomDosen)) {
            return skripsi.getNilaiPenguji2();
        }
        if ("penguji3".equals(kolomDosen)) {
            return skripsi.getNilaiPenguji3();
        }
        if ("penguji4".equals(kolomDosen)) {
            return skripsi.getNilaiPenguji4();
        }
        if ("penguji5".equals(kolomDosen)) {
            return skripsi.getNilaiPenguji5();
        }
        return null;
    }

    /**
     * Membangun satu baris data laporan dari {@code skripsi}: menyalin seluruh properti
     * {@link Skripsi} (dan {@link KrsMahasiswa} bila ada) apa adanya lewat
     * {@link Common#insertProperty}, lalu menambahkan turunan akademik (foto lulus, judisium,
     * dosen PA, SKS, IPK/IPS beserta varian pembulatan dan terbilang, mutu), identitas jadwal
     * sidang, nama ketujuh peran dosen, status sidang, dan status aktif historis mahasiswa
     * ({@link #getNamaStatusAktif}).
     *
     * @return peta data satu baris laporan
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Map buildMapSidang(Skripsi skripsi, Mahasiswa mahasiswa, KrsMahasiswa krsMahasiswa,
            HistoryStatusMahasiswa historyStatus) {
        Map map = new HashMap();
        Common.insertProperty(Skripsi.class, skripsi, map, "");
        if (krsMahasiswa != null) {
            Common.insertProperty(KrsMahasiswa.class, krsMahasiswa, map, "krs");
        }
        mahasiswa.putPhotoLulus(map);
        Judisium judisium = krsMahasiswa == null ? null : Common.hitungJudisium(mahasiswa, krsMahasiswa);
        map.put("judisium", judisium == null ? "" : judisium.getNama());
        map.put("judisium_en", judisium == null ? "" : judisium.getNamaen());
        map.put("dosen_pa", krsMahasiswa == null || krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNama());
        map.put("dosen_nidn", krsMahasiswa == null || krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getNidn());
        map.put("dosen_code", krsMahasiswa == null || krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getCode());
        map.put("dosen_nip", krsMahasiswa == null || krsMahasiswa.getDosenPa() == null ? "" : krsMahasiswa.getDosenPa().getMycode());
        map.put("sks", krsMahasiswa == null ? new Integer(0) : krsMahasiswa.getSksk());
        map.put("semester", krsMahasiswa == null ? skripsi.getSemester() : krsMahasiswa.getSemester());
        map.put("sksk", krsMahasiswa == null ? new Integer(0) : krsMahasiswa.getSksk());
        double ipk = krsMahasiswa == null || krsMahasiswa.getIpk() == null ? 0.0 : krsMahasiswa.getIpk().doubleValue();
        double ips = krsMahasiswa == null || krsMahasiswa.getIps() == null ? 0.0 : krsMahasiswa.getIps().doubleValue();
        map.put("ipk", new Double(ipk));
        map.put("ipk_ceil", new Double(Math.ceil(ipk)));
        map.put("ipk_floor", new Double(Math.floor(ipk)));
        map.put("ipk_round", new Long(Math.round(ipk)));
        map.put("ipk_terbilang", IndonesianNumberToWords.convert(Common.numberFormat2.get().format(ipk)));
        map.put("ip", new Double(ips));
        map.put("ip_ceil", new Double(Math.ceil(ips)));
        map.put("ip_floor", new Double(Math.floor(ips)));
        map.put("ip_round", new Double(Math.floor(ips)));
        map.put("mutu", mahasiswa.hitungMutu());
        map.put("nim", mahasiswa.getNim());
        map.put("nama_mhs", mahasiswa.getNama());
        map.put("dosen_id", skripsi.getPembimbing() == null ? -1L : skripsi.getPembimbing().getId());
        map.put("id_jadwal", skripsi.getJadwalSidangTugasAkhir() == null ? -1L : skripsi.getJadwalSidangTugasAkhir().getId());
        map.put("nama", skripsi.getJadwalSidangTugasAkhir() == null ? "" : skripsi.getJadwalSidangTugasAkhir().getNama());
        map.put("mulai", skripsi.getJadwalSidangTugasAkhir() == null ? null : skripsi.getJadwalSidangTugasAkhir().getMulai());
        map.put("sampai", skripsi.getJadwalSidangTugasAkhir() == null ? null : skripsi.getJadwalSidangTugasAkhir().getSampai());
        map.put("dosen1", skripsi.getPembimbing() == null ? null : skripsi.getPembimbing().getNama());
        map.put("dosen2", skripsi.getKetuaSidang() == null ? null : skripsi.getKetuaSidang().getNama());
        map.put("dosen3", skripsi.getPenguji1() == null ? null : skripsi.getPenguji1().getNama());
        map.put("dosen4", skripsi.getPenguji2() == null ? null : skripsi.getPenguji2().getNama());
        map.put("dosen5", skripsi.getPenguji3() == null ? null : skripsi.getPenguji3().getNama());
        map.put("dosen6", skripsi.getPenguji4() == null ? null : skripsi.getPenguji4().getNama());
        map.put("dosen7", skripsi.getPenguji5() == null ? null : skripsi.getPenguji5().getNama());
        map.put("nilaihuruf", skripsi.getNilaiHuruf());
        map.put("totalnilai", skripsi.getTotalNilai());
        map.put("jur", mahasiswa.getJurusan() == null ? null : mahasiswa.getJurusan().getNama());
        map.put("fak", mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null ? null : mahasiswa.getJurusan().getFakultas().getNama());
        map.put("tahunangkatan", mahasiswa.getTahunangkatan());
        map.put("status", skripsi.getNilaiKetuaSidang());
        map.put("judul", skripsi.getJudul());
        map.put("kelamin", mahasiswa.getKelamin());
        map.put("status_sidang", isSudahSidang(skripsi) ? "Sudah" : "Belum");
        map.put("tanggal_sidang", skripsi.getTanggalSidang());
        map.put("awal_bimbingan", skripsi.getAwalBimbingan());
        map.put("akhir_bimbingan", skripsi.getAkhirBimbingan());
        map.put("status_aktif", getNamaStatusAktif(mahasiswa, skripsi, historyStatus));
        return map;
    }

    /**
     * Menampilkan progress bar, lalu menghitung ulang data ({@link #generateDataDanImageAlbum})
     * di thread terpisah dan merender dashboard HTML ({@link #renderDashboard()}) setelah selesai.
     *
     * @param event event pemicu (tidak dipakai)
     */
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
                    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanRekapitulasiSidang.java:476");
                    updateLabel(label, null, 0, 0, "Gagal memproses data rekapitulasi sidang.");
                }
            }
        }).start();
    }

    /**
     * Menampilkan progress bar, menghitung ulang data di thread terpisah, lalu menghasilkan dan
     * menampilkan berkas PDF lewat mesin laporan lama ({@link Report#generateFileReportWithProgress}).
     *
     * @param event event pemicu (tidak dipakai)
     */
    public void onCetakLama(Event event) throws Exception {
        final Label label = Common.displayLoadBar(new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "Rekap_sidang_mahasiswa",
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
                    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanRekapitulasiSidang.java:498");
                    updateLabel(label, null, 0, 0, "Gagal memproses data cetak rekapitulasi sidang.");
                }
            }
        }).start();
    }

    /** Mengosongkan area dashboard dan mengisinya dengan hasil rekap ({@link #maps}) atau tampilan kosong bila belum ada data. */
    private void renderDashboard() {
        center.getChildren().clear();
        String html = (maps == null || maps.isEmpty()) ? LaporanSkripsiDashboardUtil.empty("Dashboard Rekapitulasi Sidang", getFilterInfo())
                : LaporanSkripsiDashboardUtil.renderRekapSidang(maps, getFilterInfo());
        center.appendChild(new Html(html));
    }

    /** Menampilkan dashboard kosong (belum ada data) sebagai tampilan awal sebelum laporan pertama kali dijalankan. */
    private void tampilkanDashboardAwal() {
        center.getChildren().clear();
        center.appendChild(new Html(LaporanSkripsiDashboardUtil.empty("Dashboard Rekapitulasi Sidang", getFilterInfo())));
    }

    private Criterion buildDosenCriterion(Dosen dosen) {
        Criterion criterion = Restrictions.eq("pembimbing", dosen);
        criterion = Restrictions.or(criterion, Restrictions.eq("ketuaSidang", dosen));
        criterion = Restrictions.or(criterion, Restrictions.eq("penguji1", dosen));
        criterion = Restrictions.or(criterion, Restrictions.eq("penguji2", dosen));
        criterion = Restrictions.or(criterion, Restrictions.eq("penguji3", dosen));
        criterion = Restrictions.or(criterion, Restrictions.eq("penguji4", dosen));
        criterion = Restrictions.or(criterion, Restrictions.eq("penguji5", dosen));
        criterion = Restrictions.or(criterion, Restrictions.eq("pembimbing3", dosen));
        return criterion;
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
        return searchTahunAkademik == null || searchTahunAkademik.getSelectedItem() == null
                || searchTahunAkademik.getSelectedItem().getValue() == null ? null
                        : String.valueOf(searchTahunAkademik.getSelectedItem().getValue());
    }

    private Object getSelectedSemesterValue() {
        return searchSemesterAbsensi == null || searchSemesterAbsensi.getSelectedItem() == null ? null
                : searchSemesterAbsensi.getSelectedItem().getValue();
    }

    private String getSelectedSemesterLabel() {
        Object value = getSelectedSemesterValue();
        return value == null ? Perkuliahan.GANJIL : String.valueOf(value);
    }

    private Long getSelectedStatusMahasiswaId() {
        StatusMahasiswa s = getSelectedStatusMahasiswa();
        return s == null || s.getId() == null ? -1L : s.getId();
    }

    private StatusMahasiswa getSelectedStatusMahasiswa() {
        return status == null || status.getSelectedItem() == null || status.getSelectedItem().getValue() == null ? null
                : (StatusMahasiswa) status.getSelectedItem().getValue();
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

    private String getNamaStatusAktif(Mahasiswa mahasiswa, Skripsi skripsi, HistoryStatusMahasiswa historyStatus) {
        if (mahasiswa.getStatusKeluar() != null && mahasiswa.getSemesterLulus() != null && skripsi.getSemester() != null
                && mahasiswa.getSemesterLulus().intValue() <= skripsi.getSemester().intValue()) {
            return mahasiswa.getStatusKeluar().getNama();
        }
        return historyStatus == null || historyStatus.getStatusMahasiswa() == null ? ""
                : historyStatus.getStatusMahasiswa().getNama();
    }

    private boolean isSudahSidang(Skripsi skripsi) {
        return skripsi.getTelahSidang() != null && skripsi.getTelahSidang().intValue() == 1;
    }

    private String getFilterInfo() {
        String ta = getSelectedTahunAkademik() == null ? "Semua TA" : getSelectedTahunAkademik();
        String smt = getSelectedSemesterValue() == null ? "Semua Semester" : String.valueOf(getSelectedSemesterValue());
        String prodi = getSelectedJurusanObject() == null ? "Semua Prodi" : getSelectedJurusanObject().getNama();
        return ta + " • " + smt + " • " + prodi;
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
