package ais.action.master.dashboard.sirs;
import ais.ui.util.DashboardGridExportHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.FetchMode;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.MyPortallayout;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimplePieModel;
import org.zkoss.zul.SimpleCategoryModel;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.event.PagingEvent;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.DataPasienKeluar;
import ais.database.model.sirs.Pendaftaran;
import ais.database.model.sirs.TempatTidur;
import ais.database.model.sirs.TransaksiMedis;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.HargaJualItem;
import ais.database.model.sirs.BookingRegistrasi;
import ais.database.model.sirs.Resep;
import ais.database.model.sirs.Racikan;
import ais.database.model.sirs.KunjunganDokter;
import ais.database.model.sirs.Pembayaran;
import ais.database.model.sirs.TransaksiMedisDetail;
import ais.database.model.sirs.Tindakan;
import ais.database.model.sirs.DiagnosaPenyakit;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.BiayaTindakanPerKelas;

import ais.ui.util.DashboardModernHtmlUtil;
/**
 * Dashboard SIRS Terpadu - Versi Ultimate Anti LazyInitializationException 
 * Di-maintain oleh tim: Huda & Mang Dadang
 * Menggunakan Alias Left Join & Eager Fetch Mode penuh agar object terbebas dari Proxy.
 */
public class DashboardSirsKomprehensif extends MyPortallayout {

    private static final long serialVersionUID = 1L;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");
    private final SimpleDateFormat shortDate = new SimpleDateFormat("dd/MM/yy");

    public DashboardSirsKomprehensif() {
        super();
        try {
            this.setWidth("100%");
            this.setMaximizedMode("whole");
            this.setStyle("background-color: #f4f7fa; padding: 15px;");
            init();
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/sirs/DashboardSirsKomprehensif.java:74");
        }
    }

    private void init() throws Exception {
        // Toolbar ekspor (Cetak PDF / Ekspor Excel) DITARUH sebagai BARIS HEADER PENUH di atas
        // dua kolom grafik. Kalau ditempel langsung ke portal (pasang()), toolbar jadi satu
        // item flex sempit di kiri sehingga kolom kanan terdorong turun & tampilan tak penuh.
        // Membungkusnya dalam MyPortalchildren lebar 100% membuatnya menempati satu baris penuh.
        MyPortalchildren pcHeader = new MyPortalchildren();
        pcHeader.setWidth("100%");
        pcHeader.setParent(this);
        Toolbar toolbarEkspor = new Toolbar();
        toolbarEkspor.setWidth("100%");
        toolbarEkspor.setStyle("padding:6px 8px;background:#ffffff;border:1px solid #e2e8f0;border-radius:10px;");
        DashboardGridExportHelper.pasangTombol(toolbarEkspor, this, "SIRS Komprehensif");
        toolbarEkspor.setParent(pcHeader);

        MyPortalchildren pcKiri = new MyPortalchildren();
        pcKiri.setWidth("50%");
        pcKiri.setStyle("padding-right: 7px;");
        pcKiri.setParent(this);
        
        MyPortalchildren pcKanan = new MyPortalchildren();
        pcKanan.setWidth("50%");
        pcKanan.setStyle("padding-left: 7px;");
        pcKanan.setParent(this);

        // --- BARIS 1 & 2: GRAFIK STATISTIK (DATA NYATA) ---
        // Tiap grafik membuka session-nya SENDIRI (ditutup di finally) agar galat pada satu
        // grafik TIDAK meracuni transaksi grafik lain (PostgreSQL membatalkan seluruh transaksi
        // bila ada satu statement gagal) — mengikuti pola grid di kelas ini.
        buildChartOkupansi(pcKiri);
        buildChartTrendPendaftaran(pcKanan);
        buildChartKomposisiJenis(pcKiri);
        buildChartTrendPembayaran(pcKanan);
        buildChartPaymentMethod(pcKiri);
        buildChartTrendResepRacikan(pcKanan);
        buildChartBookingMethod(pcKiri);
        buildChartTrendDiagnosa(pcKanan);

        // --- BARIS 3: RESOURCES ---
        buildGridKetersediaanBed(pcKiri);
        buildGridKetersediaanDokter(pcKanan);

        // --- BARIS 4: BIAYA ---
        buildGridBiayaTindakan(pcKiri);
        buildGridRingkasanHarga(pcKanan);

        // --- BARIS 5: PENDAFTARAN ---
        buildGridPendaftaran(pcKiri);
        buildGridBooking(pcKanan);

        // --- BARIS 6: KLINIS ---
        buildGridKunjunganDokter(pcKiri);
        buildGridDiagnosaPenyakit(pcKanan);  

        // --- BARIS 7: FARMASI ---
        buildGridResepTerbaru(pcKiri);
        buildGridRacikan(pcKanan);           

        // --- BARIS 8: KEUANGAN ---
        buildGridPembayaran(pcKiri);       
        buildGridTransaksiMedis(pcKanan);

        // --- BARIS 9: LOGISTIK & HISTORY ---
        buildGridTindakanMedis(pcKiri);
        buildGridStokKritis(pcKanan);
        buildGridMasterTindakan(pcKiri);   
        buildGridPasienKeluar(pcKanan);
    }

    // ========================================================================
    // SECTION 1: CHARTS BUILDERS (Pie & Trend)
    // ========================================================================

    /** BOR: perbandingan tempat tidur terisi vs kosong (data nyata dari sirs.tempat_tidur). */
    private void buildChartOkupansi(MyPortalchildren pc) {
        Panel p = createPanel("Analisis Okupansi Bed (BOR)");
        Panelchildren child = createCenteredChild();
        SimplePieModel model = new SimplePieModel();
        Session s = null;
        try {
            s = HibernateUtil.getSessionFactory().openSession();
            long total = hitung(s, TempatTidur.class);
            long terisi = hitung(s, TempatTidur.class, Restrictions.eq("terisi", Boolean.TRUE));
            long kosong = Math.max(0, total - terisi);
            if (total <= 0) {
                model.setValue("Belum ada data", new Double(1));
            } else {
                model.setValue("Terisi", new Double(terisi));
                model.setValue("Kosong", new Double(kosong));
            }
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/sirs/DashboardSirsKomprehensif.java:168");
            model.setValue("Belum ada data", new Double(1));
        } finally {
            tutup(s);
        }
        child.appendChild(DashboardModernHtmlUtil.createAnyChart(model, p.getTitle(), "Perbandingan tempat tidur yang terisi dan yang masih kosong saat ini.", "pie")); p.appendChild(child); pc.appendChild(p);
    }

    /** Metode registrasi: pasien lewat booking vs datang langsung (data nyata). */
    private void buildChartBookingMethod(MyPortalchildren pc) {
        Panel p = createPanel("Metode Registrasi Pasien");
        Panelchildren child = createCenteredChild();
        SimplePieModel model = new SimplePieModel();
        Session s = null;
        try {
            s = HibernateUtil.getSessionFactory().openSession();
            long totalPend = hitung(s, Pendaftaran.class);
            long viaBooking = hitung(s, BookingRegistrasi.class, Restrictions.isNotNull("pendaftaran"));
            long langsung = Math.max(0, totalPend - viaBooking);
            if (totalPend <= 0) {
                model.setValue("Belum ada data", new Double(1));
            } else {
                model.setValue("Via Booking", new Double(viaBooking));
                model.setValue("Datang Langsung", new Double(langsung));
            }
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/sirs/DashboardSirsKomprehensif.java:194");
            model.setValue("Belum ada data", new Double(1));
        } finally {
            tutup(s);
        }
        child.appendChild(DashboardModernHtmlUtil.createAnyChart(model, p.getTitle(), "Perbandingan pasien yang mendaftar lewat booking dan yang datang langsung.", "pie")); p.appendChild(child); pc.appendChild(p);
    }

    /** Komposisi kunjungan menurut jenis layanan (Rawat Jalan/Inap/UGD) — data nyata sirs.pendaftaran. */
    @SuppressWarnings("unchecked")
    private void buildChartKomposisiJenis(MyPortalchildren pc) {
        Panel p = createPanel("Komposisi Kunjungan per Jenis Layanan");
        Panelchildren child = createCenteredChild();
        SimplePieModel model = new SimplePieModel();
        boolean ada = false;
        Session s = null;
        try {
            s = HibernateUtil.getSessionFactory().openSession();
            List<Object[]> baris = s.createCriteria(Pendaftaran.class)
                    .setProjection(Projections.projectionList()
                            .add(Projections.groupProperty("jenis"))
                            .add(Projections.rowCount())).list();
            if (baris != null) {
                for (Object[] row : baris) {
                    if (row == null) { continue; }
                    String jenis = row[0] == null ? "(Lainnya)" : String.valueOf(row[0]).trim();
                    if (jenis.isEmpty()) { jenis = "(Lainnya)"; }
                    double n = row[1] instanceof Number ? ((Number) row[1]).doubleValue() : 0;
                    if (n > 0) { model.setValue(jenis, new Double(n)); ada = true; }
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/sirs/DashboardSirsKomprehensif.java:226");
        } finally {
            tutup(s);
        }
        if (!ada) { model.setValue("Belum ada data", new Double(1)); }
        child.appendChild(DashboardModernHtmlUtil.createAnyChart(model, p.getTitle(), "Perbandingan jumlah kunjungan menurut jenis layanan (Rawat Jalan / Rawat Inap / UGD).", "pie")); p.appendChild(child); pc.appendChild(p);
    }

    /** Metode pembayaran: nominal tunai vs non-tunai (data nyata sirs.pembayaran). */
    private void buildChartPaymentMethod(MyPortalchildren pc) {
        Panel p = createPanel("Analisis Metode Pembayaran");
        Panelchildren child = createCenteredChild();
        SimplePieModel model = new SimplePieModel();
        Session s = null;
        try {
            s = HibernateUtil.getSessionFactory().openSession();
            double tunai = jumlah(s, Pembayaran.class, "bayarTunai");
            double nonTunai = jumlah(s, Pembayaran.class, "bayarNonTunai");
            if (tunai <= 0 && nonTunai <= 0) {
                model.setValue("Belum ada data", new Double(1));
            } else {
                model.setValue("Tunai", new Double(tunai));
                model.setValue("Non-Tunai", new Double(nonTunai));
            }
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/sirs/DashboardSirsKomprehensif.java:251");
            model.setValue("Belum ada data", new Double(1));
        } finally {
            tutup(s);
        }
        child.appendChild(DashboardModernHtmlUtil.createAnyChart(model, p.getTitle(), "Perbandingan nominal uang yang diterima secara tunai dan non-tunai.", "pie")); p.appendChild(child); pc.appendChild(p);
    }

    /** Tren pendaftaran & booking 7 hari terakhir (data nyata, hitung per hari). */
    private void buildChartTrendPendaftaran(MyPortalchildren pc) {
        Panel p = createPanel("Grafik Trend Pendaftaran & Booking (7 Hari)");
        Panelchildren child = createCenteredChild();
        SimpleCategoryModel model = new SimpleCategoryModel();
        Session s = null;
        try {
            s = HibernateUtil.getSessionFactory().openSession();
            for (int i = 6; i >= 0; i--) {
                Date start = awalHari(i);
                Date end = tambahHari(start, 1);
                long pend = hitung(s, Pendaftaran.class,
                        Restrictions.ge("tanggalPendaftaran", start), Restrictions.lt("tanggalPendaftaran", end));
                long book = hitung(s, BookingRegistrasi.class,
                        Restrictions.ge("tanggalBookingRegistrasi", start), Restrictions.lt("tanggalBookingRegistrasi", end));
                String label = (i == 0) ? "Hari Ini" : "H-" + i;
                model.setValue("Pendaftaran", label, new Integer((int) pend));
                model.setValue("Booking", label, new Integer((int) book));
            }
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/sirs/DashboardSirsKomprehensif.java:279");
        } finally {
            tutup(s);
        }
        child.appendChild(DashboardModernHtmlUtil.createAnyChart(model, p.getTitle(), "Banyaknya pendaftaran dan booking pasien selama 7 hari terakhir.", "line")); p.appendChild(child); pc.appendChild(p);
    }

    /** Tren pendapatan kasir 4 minggu terakhir (jumlah total_biaya per minggu, dalam juta Rupiah). */
    private void buildChartTrendPembayaran(MyPortalchildren pc) {
        Panel p = createPanel("Grafik Trend Pendapatan / Kasir (Mingguan)");
        Panelchildren child = createCenteredChild();
        SimpleCategoryModel model = new SimpleCategoryModel();
        Session s = null;
        try {
            s = HibernateUtil.getSessionFactory().openSession();
            for (int w = 3; w >= 0; w--) {
                Date start = awalHari(w * 7 + 6);
                Date end = tambahHari(awalHari(w * 7), 1);
                double total = jumlah(s, Pembayaran.class, "totalBiaya",
                        Restrictions.ge("tanggalPembayaran", start), Restrictions.lt("tanggalPembayaran", end));
                String label = (w == 0) ? "Mgg Ini" : "Mgg-" + w;
                model.setValue("Total (juta Rp)", label, new Double(total / 1000000d));
            }
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/sirs/DashboardSirsKomprehensif.java:303");
        } finally {
            tutup(s);
        }
        child.appendChild(DashboardModernHtmlUtil.createAnyChart(model, p.getTitle(), "Total uang yang diterima kasir tiap minggu (dalam juta Rupiah), 4 minggu terakhir.", "bar")); p.appendChild(child); pc.appendChild(p);
    }

    /** Tren resep vs racikan 7 hari terakhir (data nyata, hitung per hari). */
    private void buildChartTrendResepRacikan(MyPortalchildren pc) {
        Panel p = createPanel("Grafik Trend Resep vs Racikan (Apotek)");
        Panelchildren child = createCenteredChild();
        SimpleCategoryModel model = new SimpleCategoryModel();
        Session s = null;
        try {
            s = HibernateUtil.getSessionFactory().openSession();
            for (int i = 6; i >= 0; i--) {
                Date start = awalHari(i);
                Date end = tambahHari(start, 1);
                long resep = hitung(s, Resep.class,
                        Restrictions.ge("tanggal_dirubah", start), Restrictions.lt("tanggal_dirubah", end));
                long racikan = hitung(s, Racikan.class,
                        Restrictions.ge("tanggal_dirubah", start), Restrictions.lt("tanggal_dirubah", end));
                String label = (i == 0) ? "Hari Ini" : "H-" + i;
                model.setValue("Resep Biasa", label, new Integer((int) resep));
                model.setValue("Obat Racikan", label, new Integer((int) racikan));
            }
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/sirs/DashboardSirsKomprehensif.java:330");
        } finally {
            tutup(s);
        }
        child.appendChild(DashboardModernHtmlUtil.createAnyChart(model, p.getTitle(), "Banyaknya resep biasa dan obat racikan yang dibuat selama 7 hari terakhir.", "line")); p.appendChild(child); pc.appendChild(p);
    }

    /** 5 diagnosa terbanyak berdasarkan diagnosa akhir utama (ICD) — data nyata. */
    @SuppressWarnings("unchecked")
    private void buildChartTrendDiagnosa(MyPortalchildren pc) {
        Panel p = createPanel("Trend 5 Diagnosa Penyakit Terbanyak");
        Panelchildren child = createCenteredChild();
        SimpleCategoryModel model = new SimpleCategoryModel();
        boolean ada = false;
        Session s = null;
        try {
            s = HibernateUtil.getSessionFactory().openSession();
            List<Object[]> baris = s.createCriteria(DiagnosaPenyakit.class)
                    .createAlias("diagnosaAkhir1", "icd", Criteria.INNER_JOIN)
                    .setProjection(Projections.projectionList()
                            .add(Projections.groupProperty("icd.nama_indonesia"))
                            .add(Projections.rowCount())).list();
            if (baris == null) {
                baris = new java.util.ArrayList<Object[]>();
            }
            Collections.sort(baris, new Comparator<Object[]>() {
                public int compare(Object[] a, Object[] b) {
                    long ca = (a != null && a.length > 1 && a[1] instanceof Number) ? ((Number) a[1]).longValue() : 0;
                    long cb = (b != null && b.length > 1 && b[1] instanceof Number) ? ((Number) b[1]).longValue() : 0;
                    return (cb < ca) ? -1 : (cb > ca) ? 1 : 0;
                }
            });
            int batas = Math.min(5, baris.size());
            for (int i = 0; i < batas; i++) {
                Object[] row = baris.get(i);
                String nama = (row[0] == null) ? "(Tanpa Nama)" : String.valueOf(row[0]).trim();
                if (nama.isEmpty()) { nama = "(Tanpa Nama)"; }
                double n = row[1] instanceof Number ? ((Number) row[1]).doubleValue() : 0;
                if (n > 0) { model.setValue("Penyakit", nama, new Integer((int) n)); ada = true; }
            }
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/sirs/DashboardSirsKomprehensif.java:371");
        } finally {
            tutup(s);
        }
        if (!ada) { model.setValue("Penyakit", "Belum ada data", new Integer(0)); }
        child.appendChild(DashboardModernHtmlUtil.createAnyChart(model, p.getTitle(), "Lima penyakit yang paling sering ditegakkan sebagai diagnosa akhir utama.", "bar")); p.appendChild(child); pc.appendChild(p);
    }

    private Panelchildren createCenteredChild() {
        Panelchildren child = new Panelchildren();
        child.setStyle("text-align: center; padding: 10px;");
        return child;
    }

    // ---- Helper query ringkas (property-based, tanpa tebak nama kolom) ----

    /** Menutup session grafik dengan aman (clear + disconnect + close), abai bila null. */
    private void tutup(Session s) {
        if (s != null) {
            try { s.clear(); s.disconnect(); s.close(); } catch (Exception ex) { ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/dashboard/sirs/DashboardSirsKomprehensif.java:390"); }
        }
    }

    /** Menghitung jumlah baris suatu entitas dengan kondisi (Criteria rowCount). */
    private long hitung(Session s, Class<?> cls, Criterion... kondisi) {
        Criteria c = s.createCriteria(cls);
        for (int i = 0; i < kondisi.length; i++) {
            c.add(kondisi[i]);
        }
        c.setProjection(Projections.rowCount());
        Number n = (Number) c.uniqueResult();
        return n == null ? 0 : n.longValue();
    }

    /** Menjumlahkan satu properti numerik suatu entitas dengan kondisi (Criteria sum). */
    private double jumlah(Session s, Class<?> cls, String properti, Criterion... kondisi) {
        Criteria c = s.createCriteria(cls);
        for (int i = 0; i < kondisi.length; i++) {
            c.add(kondisi[i]);
        }
        c.setProjection(Projections.sum(properti));
        Object v = c.uniqueResult();
        return v instanceof Number ? ((Number) v).doubleValue() : 0d;
    }

    /** Awal hari (00:00:00) untuk {@code mundurHari} hari yang lalu (0 = hari ini). */
    private Date awalHari(int mundurHari) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -mundurHari);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /** Menambah {@code n} hari ke sebuah tanggal. */
    private Date tambahHari(Date d, int n) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        cal.add(Calendar.DAY_OF_MONTH, n);
        return cal.getTime();
    }

    // ========================================================================
    // SECTION 2: GRIDS DENGAN FULL ALIAS LEFT JOIN UNTUK MENCEGAH LAZY_EXC
    // ========================================================================

    private void buildGridKetersediaanBed(MyPortalchildren pc) {
        buildEngine(pc, new Config<TempatTidur>() {
            public String getTitle() { return "Monitoring Detail Tempat Tidur & Ruangan"; }
            public String[] getHeaders() { return new String[]{"Ruang/Kamar", "Kelas", "Status Bed"}; }
            public Criteria getCriteria(Session s) { 
                return s.createCriteria(TempatTidur.class)
                        .createAlias("kamar", "kmr", Criteria.LEFT_JOIN)
                        .createAlias("kmr.ruang", "rng", Criteria.LEFT_JOIN)
                        .createAlias("kmr.kelasPerawatan", "kls", Criteria.LEFT_JOIN)
                        .setFetchMode("kmr", FetchMode.JOIN)
                        .setFetchMode("rng", FetchMode.JOIN)
                        .setFetchMode("kls", FetchMode.JOIN);
            }
            public void addOrder(Criteria c) { c.addOrder(Order.asc("id")); }
            public void render(Row r, TempatTidur d) {
                String infoRuang = "-";
                String infoKelas = "-";
                if (d.getKamar() != null) {
                    infoRuang = d.getKamar().getNama();
                    if(d.getKamar().getRuang() != null) infoRuang = d.getKamar().getRuang().getNama() + " / " + infoRuang;
                    if(d.getKamar().getKelasPerawatan() != null) infoKelas = d.getKamar().getKelasPerawatan().getNama();
                }
                r.appendChild(new Label(infoRuang));
                r.appendChild(new Label(infoKelas));
                boolean isTerisi = d.getTerisi() != null && d.getTerisi();
                Label lStatus = new Label(isTerisi ? "TERISI" : "KOSONG");
                lStatus.setStyle(isTerisi ? "color:white; background-color:#e74c3c; padding:2px 8px; border-radius:3px; font-weight:bold;" : "color:white; background-color:#2ecc71; padding:2px 8px; border-radius:3px; font-weight:bold;");
                r.appendChild(lStatus);
            }
        });
    }

    private void buildGridKetersediaanDokter(MyPortalchildren pc) {
        buildEngine(pc, new Config<Dokter>() {
            public String getTitle() { return "Informasi Ketersediaan Dokter Aktif"; }
            public String[] getHeaders() { return new String[]{"Kode", "Nama Dokter", "Kategori"}; }
            public Criteria getCriteria(Session s) { return s.createCriteria(Dokter.class); }
            public void addOrder(Criteria c) { c.addOrder(Order.asc("nama")); }
            public void render(Row r, Dokter d) {
                r.appendChild(new Label(d.getKode() != null ? d.getKode() : "-"));
                r.appendChild(new Label(d.getNama() != null ? d.getNama() : "-"));
                r.appendChild(new Label(d.getKategori() != null ? d.getKategori() : "-"));
            }
        });
    }

    private void buildGridBiayaTindakan(MyPortalchildren pc) {
        buildEngine(pc, new Config<BiayaTindakanPerKelas>() {
            public String getTitle() { return "Master Biaya Tindakan Berdasarkan Kelas"; }
            public String[] getHeaders() { return new String[]{"Tindakan", "Kelas Perawatan", "Tarif Biaya"}; }
            public Criteria getCriteria(Session s) { 
                return s.createCriteria(BiayaTindakanPerKelas.class)
                        .createAlias("tarifKhususPunyaTindakan", "tkpt", Criteria.LEFT_JOIN)
                        .createAlias("tkpt.tindakan", "tdk", Criteria.LEFT_JOIN)
                        .createAlias("kelasPerawatan", "kp", Criteria.LEFT_JOIN)
                        .setFetchMode("tkpt", FetchMode.JOIN)
                        .setFetchMode("tdk", FetchMode.JOIN)
                        .setFetchMode("kp", FetchMode.JOIN);
            }
            public void addOrder(Criteria c) { c.addOrder(Order.desc("id")); }
            public void render(Row r, BiayaTindakanPerKelas d) {
                String namaTindakan = "-";
                if (d.getTarifKhususPunyaTindakan() != null && d.getTarifKhususPunyaTindakan().getTindakan() != null) {
                    namaTindakan = d.getTarifKhususPunyaTindakan().getTindakan().getNama();
                }
                r.appendChild(new Label(namaTindakan));
                r.appendChild(new Label(d.getKelasPerawatan() != null ? d.getKelasPerawatan().getNama() : "-"));
                r.appendChild(new Label(d.getBiaya() != null ? String.format("Rp %,.0f", d.getBiaya()) : "Rp 0"));
            }
        });
    }

    private void buildGridPendaftaran(MyPortalchildren pc) {
        buildEngine(pc, new Config<Pendaftaran>() {
            public String getTitle() { return "Rekap Pendaftaran Terkini"; }
            public String[] getHeaders() { return new String[]{"Waktu", "Pasien", "Poli", "Dokter"}; }
            public Criteria getCriteria(Session s) { 
                return s.createCriteria(Pendaftaran.class)
                        .createAlias("pasien", "psn", Criteria.LEFT_JOIN)
                        .createAlias("poly", "pl", Criteria.LEFT_JOIN)
                        .createAlias("dokter", "dk", Criteria.LEFT_JOIN)
                        .setFetchMode("psn", FetchMode.JOIN)
                        .setFetchMode("pl", FetchMode.JOIN)
                        .setFetchMode("dk", FetchMode.JOIN);
            }
            public void addOrder(Criteria c) { c.addOrder(Order.desc("tanggalPendaftaran")); }
            public void render(Row r, Pendaftaran d) {
                r.appendChild(new Label(d.getTanggalPendaftaran() != null ? dateFormat.format(d.getTanggalPendaftaran()) : "-"));
                r.appendChild(new Label(d.getPasien() != null ? d.getPasien().getNama() : "No Name"));
                r.appendChild(new Label(d.getPoly() != null ? d.getPoly().getNama() : "-"));
                r.appendChild(new Label(d.getDokter() != null ? d.getDokter().getNama() : "-"));
            }
        });
    }

    private void buildGridBooking(MyPortalchildren pc) {
        buildEngine(pc, new Config<BookingRegistrasi>() {
            public String getTitle() { return "Rekap Reservasi / Booking"; }
            public String[] getHeaders() { return new String[]{"Tgl Booking", "Pasien", "Poli", "Status"}; }
            public Criteria getCriteria(Session s) { 
                return s.createCriteria(BookingRegistrasi.class)
                        .createAlias("pasien", "psn", Criteria.LEFT_JOIN)
                        .createAlias("poly", "pl", Criteria.LEFT_JOIN)
                        .createAlias("pendaftaran", "pdf", Criteria.LEFT_JOIN)
                        .setFetchMode("psn", FetchMode.JOIN)
                        .setFetchMode("pl", FetchMode.JOIN)
                        .setFetchMode("pdf", FetchMode.JOIN);
            }
            public void addOrder(Criteria c) { c.addOrder(Order.desc("tanggalBookingRegistrasi")); }
            public void render(Row r, BookingRegistrasi d) {
                r.appendChild(new Label(d.getTanggalBookingRegistrasi() != null ? shortDate.format(d.getTanggalBookingRegistrasi()) : "-"));
                r.appendChild(new Label(d.getPasien() != null ? d.getPasien().getNama() : "-"));
                r.appendChild(new Label(d.getPoly() != null ? d.getPoly().getNama() : "-"));
                boolean isCheckedIn = d.getPendaftaran() != null;
                Label l = new Label(isCheckedIn ? "CHECKED-IN" : "WAITING");
                l.setStyle(isCheckedIn ? "color:green; font-weight:bold;" : "color:blue; font-weight:bold;");
                r.appendChild(l);
            }
        });
    }

    private void buildGridPembayaran(MyPortalchildren pc) {
        buildEngine(pc, new Config<Pembayaran>() {
            public String getTitle() { return "Rekap Pembayaran Kasir Terkini"; }
            public String[] getHeaders() { return new String[]{"ID Kasir", "Shift", "Total Biaya", "Status"}; }
            public Criteria getCriteria(Session s) { 
                return s.createCriteria(Pembayaran.class)
                        .createAlias("shift", "shf", Criteria.LEFT_JOIN)
                        .setFetchMode("shf", FetchMode.JOIN);
            }
            public void addOrder(Criteria c) { c.addOrder(Order.desc("id")); }
            public void render(Row r, Pembayaran d) {
                r.appendChild(new Label(d.getId() != null ? "PAY-" + d.getId() : "-"));
                r.appendChild(new Label(d.getShift() != null ? d.getShift().getNama() : "-"));
                Double total = d.getTotalBiaya() != null ? d.getTotalBiaya() : 0.0;
                r.appendChild(new Label(String.format("Rp %,.0f", total)));
                Label lStatus = new Label(d.getLunas() != null && d.getLunas() ? "LUNAS" : "BELUM LUNAS");
                lStatus.setStyle(d.getLunas() != null && d.getLunas() ? "color:white; background-color:#27ae60; padding:2px 5px; border-radius:3px;" : "color:white; background-color:#e74c3c; padding:2px 5px; border-radius:3px;");
                r.appendChild(lStatus);
            }
        });
    }

    private void buildGridDiagnosaPenyakit(MyPortalchildren pc) {
        buildEngine(pc, new Config<DiagnosaPenyakit>() {
            public String getTitle() { return "Rekap Riwayat Diagnosa Pasien"; }
            public String[] getHeaders() { return new String[]{"Tgl/Waktu", "Pasien", "Keterangan/Keluhan"}; }
            public Criteria getCriteria(Session s) { 
                return s.createCriteria(DiagnosaPenyakit.class)
                        .createAlias("pasien", "psn", Criteria.LEFT_JOIN)
                        .setFetchMode("psn", FetchMode.JOIN);
            }
            public void addOrder(Criteria c) { c.addOrder(Order.desc("tanggal")); }
            public void render(Row r, DiagnosaPenyakit d) {
                r.appendChild(new Label(d.getTanggal() != null ? dateFormat.format(d.getTanggal()) : "-"));
                r.appendChild(new Label(d.getPasien() != null ? d.getPasien().getNama() : "-"));
                String info = d.getKeterangan();
                if (info == null || info.trim().isEmpty()) info = d.getKeluhanDiagnosa();
                if (info == null || info.trim().isEmpty()) info = d.getKeluhanPasien();
                r.appendChild(new Label(info != null ? info : "-"));
            }
        });
    }

    private void buildGridResepTerbaru(MyPortalchildren pc) {
        buildEngine(pc, new Config<Resep>() {
            public String getTitle() { return "Rekap Monitoring Resep (Farmasi)"; }
            public String[] getHeaders() { return new String[]{"Kode Resep", "Diagnosa ID", "Waktu Buat"}; }
            public Criteria getCriteria(Session s) { 
                return s.createCriteria(Resep.class)
                        .createAlias("diagnosaPenyakit", "dp", Criteria.LEFT_JOIN)
                        .setFetchMode("dp", FetchMode.JOIN);
            }
            public void addOrder(Criteria c) { c.addOrder(Order.desc("id")); }
            public void render(Row r, Resep d) {
                r.appendChild(new Label(d.getKode() != null ? d.getKode() : "-"));
                String infoDiagnosa = d.getDiagnosaPenyakit() != null ? String.valueOf(d.getDiagnosaPenyakit().getId()) : "-";
                r.appendChild(new Label(infoDiagnosa));
                r.appendChild(new Label(d.getTanggal_dirubah() != null ? dateFormat.format(d.getTanggal_dirubah()) : "-"));
            }
        });
    }

    private void buildGridRacikan(MyPortalchildren pc) {
        buildEngine(pc, new Config<Racikan>() {
            public String getTitle() { return "Rekap Trend Racikan Farmasi"; }
            public String[] getHeaders() { return new String[]{"ID Racikan", "Jenis", "Waktu Diupdate"}; }
            public Criteria getCriteria(Session s) { 
                return s.createCriteria(Racikan.class)
                        .createAlias("variasiDari", "vd", Criteria.LEFT_JOIN)
                        .setFetchMode("vd", FetchMode.JOIN);
            }
            public void addOrder(Criteria c) { c.addOrder(Order.desc("id")); }
            public void render(Row r, Racikan d) {
                r.appendChild(new Label(d.getId() != null ? "RCK-" + d.getId() : "-"));
                r.appendChild(new Label(d.getVariasiDari() != null ? "Variasi Obat" : "Racikan Baru"));
                r.appendChild(new Label(d.getTanggal_dirubah() != null ? dateFormat.format(d.getTanggal_dirubah()) : "-"));
            }
        });
    }

    private void buildGridKunjunganDokter(MyPortalchildren pc) {
        buildEngine(pc, new Config<KunjunganDokter>() {
            public String getTitle() { return "Log Aktivitas Kunjungan Dokter"; }
            public String[] getHeaders() { return new String[]{"Waktu", "Dokter", "Diagnosa ID"}; }
            public Criteria getCriteria(Session s) { 
                return s.createCriteria(KunjunganDokter.class)
                        .createAlias("dokter", "dk", Criteria.LEFT_JOIN)
                        .createAlias("diagnosaPenyakit", "dp", Criteria.LEFT_JOIN)
                        .setFetchMode("dk", FetchMode.JOIN)
                        .setFetchMode("dp", FetchMode.JOIN);
            }
            public void addOrder(Criteria c) { c.addOrder(Order.desc("waktu")); }
            public void render(Row r, KunjunganDokter d) {
                r.appendChild(new Label(d.getWaktu() != null ? dateFormat.format(d.getWaktu()) : "-"));
                r.appendChild(new Label(d.getDokter() != null ? d.getDokter().getNama() : "-"));
                r.appendChild(new Label(d.getDiagnosaPenyakit() != null ? String.valueOf(d.getDiagnosaPenyakit().getId()) : "-"));
            }
        });
    }

    private void buildGridTindakanMedis(MyPortalchildren pc) {
        buildEngine(pc, new Config<TransaksiMedisDetail>() {
            public String getTitle() { return "Pelaksanaan Tindakan Medis"; }
            public String[] getHeaders() { return new String[]{"Mulai", "Tindakan", "Dokter", "Biaya Jasa"}; }
            public Criteria getCriteria(Session s) { 
                return s.createCriteria(TransaksiMedisDetail.class)
                        .createAlias("tindakan", "tdk", Criteria.LEFT_JOIN)
                        .createAlias("dokter", "dk", Criteria.LEFT_JOIN)
                        .setFetchMode("tdk", FetchMode.JOIN)
                        .setFetchMode("dk", FetchMode.JOIN);
            }
            public void addOrder(Criteria c) { c.addOrder(Order.desc("mulai")); }
            public void render(Row r, TransaksiMedisDetail d) {
                r.appendChild(new Label(d.getMulai() != null ? dateFormat.format(d.getMulai()) : "-"));
                r.appendChild(new Label(d.getTindakan() != null ? d.getTindakan().getNama() : "-"));
                r.appendChild(new Label(d.getDokter() != null ? d.getDokter().getNama() : "-"));
                r.appendChild(new Label(d.getAmountJasa() != null ? String.format("%,.0f", d.getAmountJasa()) : "0"));
            }
        });
    }

    private void buildGridTransaksiMedis(MyPortalchildren pc) {
        buildEngine(pc, new Config<TransaksiMedis>() {
            public String getTitle() { return "Rekap Billing Medis"; }
            public String[] getHeaders() { return new String[]{"No Trx", "Tanggal", "Sumber", "Validasi"}; }
            public Criteria getCriteria(Session s) { return s.createCriteria(TransaksiMedis.class); }
            public void addOrder(Criteria c) { c.addOrder(Order.desc("tanggalTransaksi")); }
            public void render(Row r, TransaksiMedis d) {
                r.appendChild(new Label(d.getKode() != null ? d.getKode() : "-"));
                r.appendChild(new Label(d.getTanggalTransaksi() != null ? shortDate.format(d.getTanggalTransaksi()) : "-"));
                r.appendChild(new Label(d.getSumber() != null ? d.getSumber() : "-"));
                Label l = new Label(d.getValidasi() != null && d.getValidasi() ? "VALID" : "PENDING");
                l.setStyle(d.getValidasi() != null && d.getValidasi() ? "color:blue; font-weight:bold;" : "color:orange; font-weight:bold;");
                r.appendChild(l);
            }
        });
    }

    private void buildGridStokKritis(MyPortalchildren pc) {
        buildEngine(pc, new Config<ItemMedis>() {
            public String getTitle() { return "Logistik: Stok Item Kritis (<10)"; }
            public String[] getHeaders() { return new String[]{"Nama Barang", "Satuan", "Batas Min"}; }
            public Criteria getCriteria(Session s) { 
                return s.createCriteria(ItemMedis.class)
                        .createAlias("satuanItem", "si", Criteria.LEFT_JOIN)
                        .setFetchMode("si", FetchMode.JOIN)
                        .add(Restrictions.lt("batasMinimalStok", 10)); 
            }
            public void addOrder(Criteria c) { c.addOrder(Order.asc("nama")); }
            public void render(Row r, ItemMedis d) {
                r.appendChild(new Label(d.getNama() != null ? d.getNama() : "-"));
                r.appendChild(new Label(d.getSatuanItem() != null ? d.getSatuanItem().getNama() : "-"));
                Label l = new Label(d.getBatasMinimalStok() != null ? d.getBatasMinimalStok().toString() : "0");
                l.setStyle("color: #d9534f; font-weight: bold;");
                r.appendChild(l);
            }
        });
    }

    private void buildGridMasterTindakan(MyPortalchildren pc) {
        buildEngine(pc, new Config<Tindakan>() {
            public String getTitle() { return "Katalog Master Tindakan"; }
            public String[] getHeaders() { return new String[]{"Nama Tindakan", "Jenis/Keterangan"}; }
            public Criteria getCriteria(Session s) { return s.createCriteria(Tindakan.class); }
            public void addOrder(Criteria c) { c.addOrder(Order.asc("nama")); }
            public void render(Row r, Tindakan d) {
                r.appendChild(new Label(d.getNama() != null ? d.getNama() : "-"));
                String info = d.getKeteranganLayanan();
                if (info != null && info.length() > 30) info = info.substring(0, 30) + "..."; 
                r.appendChild(new Label(info != null ? info : "-"));
            }
        });
    }

    private void buildGridRingkasanHarga(MyPortalchildren pc) {
        buildEngine(pc, new Config<HargaJualItem>() {
            public String getTitle() { return "Update Harga Jual Obat/Item Terkini"; }
            public String[] getHeaders() { return new String[]{"Item", "Kelas", "Harga"}; }
            public Criteria getCriteria(Session s) { 
                return s.createCriteria(HargaJualItem.class)
                        .createAlias("item", "itm", Criteria.LEFT_JOIN)
                        .createAlias("kelasPerawatan", "kp", Criteria.LEFT_JOIN)
                        .setFetchMode("itm", FetchMode.JOIN)
                        .setFetchMode("kp", FetchMode.JOIN);
            }
            public void addOrder(Criteria c) { c.addOrder(Order.desc("id")); }
            public void render(Row r, HargaJualItem d) {
                r.appendChild(new Label(d.getItem() != null ? d.getItem().getNama() : "-"));
                r.appendChild(new Label(d.getKelasPerawatan() != null ? d.getKelasPerawatan().getNama() : "Global"));
                r.appendChild(new Label(d.getHargaJual() != null ? String.format("Rp %,.0f", d.getHargaJual()) : "Rp 0"));
            }
        });
    }

    private void buildGridPasienKeluar(MyPortalchildren pc) {
        buildEngine(pc, new Config<DataPasienKeluar>() {
            public String getTitle() { return "Pasien Keluar (Checkout/Selesai)"; }
            public String[] getHeaders() { return new String[]{"Tgl Pulang", "Pasien", "Cara Keluar"}; }
            public Criteria getCriteria(Session s) { 
                return s.createCriteria(DataPasienKeluar.class)
                        .createAlias("pendaftaran", "pdf", Criteria.LEFT_JOIN)
                        .createAlias("pdf.pasien", "psn", Criteria.LEFT_JOIN)
                        .createAlias("statusPulang", "sp", Criteria.LEFT_JOIN)
                        .setFetchMode("pdf", FetchMode.JOIN)
                        .setFetchMode("psn", FetchMode.JOIN)
                        .setFetchMode("sp", FetchMode.JOIN);
            }
            public void addOrder(Criteria c) { c.addOrder(Order.desc("tanggalPulang")); }
            public void render(Row r, DataPasienKeluar d) {
                r.appendChild(new Label(d.getTanggalPulang() != null ? shortDate.format(d.getTanggalPulang()) : "-"));
                String pName = (d.getPendaftaran() != null && d.getPendaftaran().getPasien() != null) ? d.getPendaftaran().getPasien().getNama() : "-";
                r.appendChild(new Label(pName));
                r.appendChild(new Label(d.getStatusPulang() != null ? d.getStatusPulang().getNama() : "Selesai"));
            }
        });
    }

    // ========================================================================
    // CORE ENGINE (Paging Server-Side dengan Anti-Crash Renderer)
    // ========================================================================

    private <T> void buildEngine(MyPortalchildren pc, final Config<T> config) {
        Panel panel = createPanel(config.getTitle());
        Panelchildren child = new Panelchildren();
        final Grid grid = new Grid();
        grid.setEmptyMessage("Data tidak tersedia...");
        grid.setMold("paging"); 
        Columns cols = new Columns();
        cols.setStyle("background: #e9ecef;");
        for (String h : config.getHeaders()) {
            Column c = new Column(h);
            c.setStyle("font-weight: bold; font-size: 11px;");
            cols.appendChild(c);
        }
        cols.setParent(grid);
        final Rows rows = new Rows();
        rows.setParent(grid);
        grid.setParent(child);
        
        final Paging paging = new Paging();
        paging.setPageSize(5); 
        paging.setDetailed(true);
        paging.setParent(child);
        
        paging.addEventListener("onPaging", new EventListener() {
            public void onEvent(Event e) throws Exception {
                fetchData(config, rows, paging, ((PagingEvent) e).getActivePage() * paging.getPageSize());
            }
        });
        
        fetchData(config, rows, paging, 0); 
        panel.appendChild(child);
        pc.appendChild(panel);
    }

    private <T> void fetchData(Config<T> config, Rows rows, Paging paging, int offset) {
        Session s = null;
        try {
            s = HibernateUtil.getSessionFactory().openSession();
            
            // 1. Hitung Total Baris
            Criteria cnt = config.getCriteria(s);
            cnt.setProjection(Projections.rowCount());
            Number total = (Number) cnt.uniqueResult();
            paging.setTotalSize(total != null ? total.intValue() : 0);

            // 2. Fetch Data Asli dengan Limit/Offset
            Criteria data = config.getCriteria(s);
            config.addOrder(data);
            data.setFirstResult(offset);
            data.setMaxResults(paging.getPageSize());
            
            @SuppressWarnings("unchecked")
            List<T> list = data.list();
            rows.getChildren().clear();
            
            // 3. Render secara aman per baris
            for (T obj : list) {
                Row r = new Row();
                try {
                    config.render(r, obj);
                } catch (Exception ex) {
                    ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/dashboard/sirs/DashboardSirsKomprehensif.java:842"); // Agar tercatat di log tanpa merusak interface visual 
                    r.getChildren().clear();
                    Label errLbl = new Label("Data Invalid / Sedang Diproses");
                    errLbl.setStyle("color:#d9534f; font-style:italic; font-size: 10px;");
                    r.appendChild(errLbl);
                }
                rows.appendChild(r);
            }
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/dashboard/sirs/DashboardSirsKomprehensif.java:851");
        } finally {
            if (s != null) {
                try { 
                    s.clear(); 
                    s.disconnect(); 
                    s.close(); 
                } catch (Exception ex) { 
                    ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/dashboard/sirs/DashboardSirsKomprehensif.java:859"); 
                }
            }
        }
    }

    private Panel createPanel(String title) {
        Panel p = new Panel();
        p.setTitle(title);
        p.setBorder("normal");
        p.setCollapsible(true);
        p.setStyle("margin-bottom: 12px; border-radius: 8px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); overflow: hidden;");
        return p;
    }

    public interface Config<T> {
        String getTitle();
        String[] getHeaders();
        Criteria getCriteria(Session s);
        void addOrder(Criteria c);
        void render(Row r, T d) throws Exception;
    }
}