package ais.action.master.payroll.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;

import ais.common.Common;
import ais.common.CommonDashboardHtmlHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.payroll.CutiDanIzin;
import ais.database.model.payroll.PembayaranGaji;
import ais.database.model.payroll.PembayaranGajiPunyaPegawai;
import ais.database.model.payroll.TransaksiPegawai;
import ais.ui.util.DashboardCache;
import ais.ui.util.DashboardProgress;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Dasbor Analisis Penggajian — satu layar ringkas yang menggabungkan tiga domain payroll:
 * <ol>
 *   <li><b>Pembayaran Gaji</b> (PembayaranGaji / PembayaranGajiPunyaPegawai): nilai gaji,
 *       jumlah slip, pegawai dibayar, status posting, tren bulanan, distribusi cara bayar.</li>
 *   <li><b>Transaksi Pegawai</b> (TransaksiPegawai / JenisTransaksiPegawai): total Debet
 *       (tunjangan/penambah) vs Kredit (potongan/pengurang), rekap per jenis, tren bulanan.</li>
 *   <li><b>Cuti &amp; Izin</b> (CutiDanIzin): jumlah pengajuan, disetujui vs belum, total hari,
 *       distribusi per jenis &amp; status absensi, tren bulanan.</li>
 * </ol>
 *
 * <h3>Optimasi akses cepat</h3>
 * <ul>
 *   <li>Semua angka diambil lewat <b>query agregat</b> (SUM/COUNT/GROUP BY via Hibernate
 *       {@link Projections}) — bukan menarik ribuan baris lalu dihitung di Java.</li>
 *   <li><b>Cache L1</b> ({@link DashboardCache#getIfPresent}/{@link DashboardCache#putL1}):
 *       seluruh data layar per tahun di-cache 90 detik → buka ulang tab instan.</li>
 *   <li><b>Cache L2</b> ({@link DashboardCache#cacheable}): tiap criteria ditandai cacheable
 *       agar dilayani Hibernate second-level/query cache (EhCache).</li>
 *   <li><b>Cache L3</b> ({@link DashboardCache#l3}): tiap blok agregat per-domain di-cache 30
 *       menit dan dipakai bersama semua pengguna.</li>
 *   <li><b>Progress bar sinkron</b> ({@link DashboardProgress}): tiap tahap query dijalankan di
 *       event ZK terpisah sehingga bar bergerak persis seiring data yang benar-benar selesai
 *       dimuat (tidak melompat / tidak mendahului).</li>
 * </ul>
 *
 * <h3>Fokus</h3>
 * Kelas ini parameterik lewat {@link Fokus}:
 * <ul>
 *   <li>{@code SEMUA} (no-arg) — dasbor gabungan tiga domain, dipakai menu utama "Gaji"
 *       (lihat MainDashboardEventHelper.onGaji).</li>
 *   <li>{@code GAJI/TRANSAKSI/CUTI} — dasbor khusus satu halaman payroll. Subkelas tipis
 *       {@link DasborGajiPegawai}, {@link DasborTransaksiPegawai}, {@link DasborCutiDanIzin}
 *       dipasang di bayar_gaji_pegawai.zul, transaksi_pegawai.zul, dan cuti_dan_izin.zul
 *       sehingga tiap halaman hanya menampilkan dasbor datanya sendiri (tidak menampilkan
 *       dasbor yang sama di mana-mana). Halaman khusus juga hanya memuat domain yang relevan
 *       (lebih cepat).</li>
 * </ul>
 */
public class DasborAnalisisPenggajian extends Div {

    private static final long serialVersionUID = 1L;

    private static final String[] BULAN = { "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt",
            "Nov", "Des" };

    /**
     * Fokus dasbor: SEMUA = gabungan tiga domain (dipakai menu utama "Gaji");
     * GAJI / TRANSAKSI / CUTI = dasbor khusus satu halaman payroll, hanya menampilkan
     * data domain yang relevan (tidak menampilkan dasbor yang sama di setiap halaman).
     */
    public enum Fokus { SEMUA, GAJI, TRANSAKSI, CUTI }

    private final Fokus fokus;
    private int filterTahun;
    // Rentang tanggal untuk panel Daftar Cuti & Daftar Perjalanan Dinas (default: 1 tahun).
    private Date filterMulai;
    private Date filterSampai;
    private Combobox cbTahun;
    private PayrollData data;

    // ════════════════════════════════════════════════════════════════════════
    // Data holder
    // ════════════════════════════════════════════════════════════════════════

    private static class PayrollData {
        int year;
        GajiData gaji;
        TransaksiData transaksi;
        CutiData cuti;
    }

    private static class GajiData {
        long totalBatch, batchPosted, totalSlip, pegawaiDibayar;
        double totalNilai;
        double[] nilaiPerBulan = new double[12];
        long[] slipPerBulan = new long[12];
        LinkedHashMap<String, Double> caraBayar = new LinkedHashMap<String, Double>();
    }

    private static class TransaksiData {
        long totalTransaksi;
        double totalDebet, totalKredit;
        double[] debetPerBulan = new double[12];
        double[] kreditPerBulan = new double[12];
        // tiap baris: [0]nama(String) [1]debet(Boolean) [2]nilai(Double) [3]count(Long)
        List<Object[]> perJenis = new ArrayList<Object[]>();
    }

    private static class CutiData {
        long total, disetujui;
        double totalHari;
        long[] perBulan = new long[12];
        LinkedHashMap<String, Long> perJenis = new LinkedHashMap<String, Long>();
        LinkedHashMap<String, Long> perStatus = new LinkedHashMap<String, Long>();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Constructor
    // ════════════════════════════════════════════════════════════════════════

    public DasborAnalisisPenggajian() {
        this(Fokus.SEMUA);
    }

    public DasborAnalisisPenggajian(Fokus fokus) {
        this.fokus = (fokus == null ? Fokus.SEMUA : fokus);
        Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
        this.filterTahun = cal.get(Calendar.YEAR);

        setWidth("100%");
        setStyle("min-height:320px; background:#f1f5f9; padding:12px 14px; box-sizing:border-box; overflow:auto;");

        // Bar awal sederhana; pemuatan nyata ditunda sampai komponen ter-attach (lihat timer).
        appendHtml(this, CommonDashboardHtmlHelper.progressBar(2, "Menyiapkan " + judulFokus(),
                "Memuat komponen dasbor…"));

        try {
            // createDefaultTimer aman dari constructor: handler dijalankan setelah komponen
            // terpasang ke desktop, sehingga echoEvent pada DashboardProgress berfungsi.
            Common.createDefaultTimer(new EventListener() {
                public void onEvent(Event e) throws Exception {
                    loadAndRender();
                }
            });
        } catch (Exception ex) {
            Common.tampilErrorJikaAdmin(ex);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Load orchestration (L1 cek dulu → kalau miss, muat bertahap dengan progress sinkron)
    // ════════════════════════════════════════════════════════════════════════

    private void loadAndRender() {
        final int year = filterTahun;
        final int bm = bulanMulai();
        final int bs = bulanSampai();
        // Rentang bulan ikut serta di key cache supaya ganti rentang -> hitung ulang (bukan data lama).
        final String dashKey = "payroll.dash." + fokus.name() + "." + year + "." + bm + "-" + bs;

        // L1: kalau seluruh data layar masih segar, langsung render (instan, tanpa progress).
        Object hit = DashboardCache.getIfPresent(dashKey);
        if (hit instanceof PayrollData) {
            this.data = (PayrollData) hit;
            renderNow();
            return;
        }

        final PayrollData d = new PayrollData();
        d.year = year;

        // Hanya muat domain yang relevan dengan fokus (halaman khusus = lebih cepat).
        DashboardProgress dp = new DashboardProgress(this, "Memuat " + judulFokus() + " " + year);

        if (fokus == Fokus.SEMUA || fokus == Fokus.GAJI) {
            dp.step("Menghitung ringkasan & tren penggajian", new DashboardProgress.Step() {
                public void run() throws Exception {
                    d.gaji = DashboardCache.l3("payroll.gaji." + year + "." + bm + "-" + bs,
                            new DashboardCache.Loader<GajiData>() {
                                public GajiData load() {
                                    return queryGaji(year, bm, bs);
                                }
                            });
                }
            });
        }
        if (fokus == Fokus.SEMUA || fokus == Fokus.TRANSAKSI) {
            dp.step("Menghitung transaksi pegawai (tunjangan & potongan)", new DashboardProgress.Step() {
                public void run() throws Exception {
                    d.transaksi = DashboardCache.l3("payroll.transaksi." + year,
                            new DashboardCache.Loader<TransaksiData>() {
                                public TransaksiData load() {
                                    return queryTransaksi(year);
                                }
                            });
                }
            });
        }
        if (fokus == Fokus.SEMUA || fokus == Fokus.CUTI) {
            dp.step("Menghitung data cuti & izin", new DashboardProgress.Step() {
                public void run() throws Exception {
                    d.cuti = DashboardCache.l3("payroll.cuti." + year, new DashboardCache.Loader<CutiData>() {
                        public CutiData load() {
                            return queryCuti(year);
                        }
                    });
                }
            });
        }

        dp.onDone(new DashboardProgress.Done() {
            public void run() throws Exception {
                data = d;
                DashboardCache.putL1(dashKey, d); // L1: percepat buka ulang
                renderNow();
            }
        }).start();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Query agregat — Penggajian
    // ════════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private GajiData queryGaji(int year, int bulanMulai, int bulanSampai) {
        GajiData g = new GajiData();
        Session s = HibernateUtil.currentSession();

        // Filter rentang bulan (1-12) sesuai "Rentang Tgl" -> bisa 1/3/6 bulan atau setahun.
        g.totalBatch = num(cache(s.createCriteria(PembayaranGaji.class).add(Restrictions.eq("tahun", year))
                .add(Restrictions.between("bulan", bulanMulai, bulanSampai))
                .setProjection(Projections.rowCount())).uniqueResult());

        g.batchPosted = num(cache(s.createCriteria(PembayaranGaji.class).add(Restrictions.eq("tahun", year))
                .add(Restrictions.between("bulan", bulanMulai, bulanSampai))
                .add(Restrictions.isNotNull("postingHistory")).setProjection(Projections.rowCount())).uniqueResult());

        // Nilai & jumlah slip per bulan (GROUP BY bulan)
        List<Object[]> bulanRows = cache(s.createCriteria(PembayaranGajiPunyaPegawai.class, "x")
                .createAlias("x.pembayaranGaji", "pg").add(Restrictions.eq("pg.tahun", year))
                .add(Restrictions.between("pg.bulan", bulanMulai, bulanSampai))
                .setProjection(Projections.projectionList().add(Projections.groupProperty("pg.bulan"))
                        .add(Projections.sum("x.nilai")).add(Projections.rowCount())))
                .list();
        for (Object[] r : bulanRows) {
            int bulan = (r[0] instanceof Number) ? ((Number) r[0]).intValue() : 0;
            double nilai = dbl(r[1]);
            long cnt = num(r[2]);
            g.totalNilai += nilai;
            g.totalSlip += cnt;
            if (bulan >= 1 && bulan <= 12) {
                g.nilaiPerBulan[bulan - 1] = nilai;
                g.slipPerBulan[bulan - 1] = cnt;
            }
        }

        g.pegawaiDibayar = num(cache(s.createCriteria(PembayaranGajiPunyaPegawai.class, "x")
                .createAlias("x.pembayaranGaji", "pg").add(Restrictions.eq("pg.tahun", year))
                .add(Restrictions.between("pg.bulan", bulanMulai, bulanSampai))
                .setProjection(Projections.countDistinct("x.pegawai"))).uniqueResult());

        // Distribusi nilai gaji per cara pembayaran (LEFT JOIN agar yang null tetap terhitung)
        List<Object[]> caraRows = cache(s.createCriteria(PembayaranGajiPunyaPegawai.class, "x")
                .createAlias("x.pembayaranGaji", "pg")
                .createAlias("pg.caraPembayaranGaji", "cb", Criteria.LEFT_JOIN)
                .add(Restrictions.eq("pg.tahun", year))
                .add(Restrictions.between("pg.bulan", bulanMulai, bulanSampai))
                .setProjection(Projections.projectionList().add(Projections.groupProperty("cb.nama"))
                        .add(Projections.sum("x.nilai"))))
                .list();
        for (Object[] r : caraRows) {
            String nama = r[0] == null ? "(Tanpa cara bayar)" : String.valueOf(r[0]);
            g.caraBayar.put(nama, Double.valueOf(dbl(r[1])));
        }
        return g;
    }

    // ════════════════════════════════════════════════════════════════════════
    // Query agregat — Transaksi Pegawai
    // ════════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private TransaksiData queryTransaksi(int year) {
        TransaksiData t = new TransaksiData();
        Session s = HibernateUtil.currentSession();

        // Rekap per jenis transaksi (GROUP BY nama, jenisTransaksi)
        List<Object[]> jenisRows = cache(s.createCriteria(TransaksiPegawai.class, "t")
                .createAlias("t.jenisTransaksiPegawai", "j", Criteria.LEFT_JOIN)
                .add(Restrictions.eq("t.thn", year))
                .setProjection(Projections.projectionList().add(Projections.groupProperty("j.nama"))
                        .add(Projections.groupProperty("j.jenisTransaksi")).add(Projections.sum("t.nilai"))
                        .add(Projections.rowCount())))
                .list();
        for (Object[] r : jenisRows) {
            String nama = r[0] == null ? "(Tanpa jenis)" : String.valueOf(r[0]);
            int jenis = (r[1] instanceof Number) ? ((Number) r[1]).intValue() : 1;
            boolean debet = jenis != 2; // 1 = Debet (penambah/tunjangan), 2 = Kredit (potongan)
            double nilai = dbl(r[2]);
            long cnt = num(r[3]);
            t.totalTransaksi += cnt;
            if (debet) {
                t.totalDebet += nilai;
            } else {
                t.totalKredit += nilai;
            }
            t.perJenis.add(new Object[] { nama, Boolean.valueOf(debet), Double.valueOf(nilai), Long.valueOf(cnt) });
        }
        // Urutkan per nilai (desc) di Java — menghindari ORDER BY non-grouped di SQL.
        java.util.Collections.sort(t.perJenis, new java.util.Comparator<Object[]>() {
            public int compare(Object[] a, Object[] b) {
                return Double.compare(((Double) b[2]).doubleValue(), ((Double) a[2]).doubleValue());
            }
        });

        // Tren per bulan (GROUP BY bln, jenisTransaksi)
        List<Object[]> bulanRows = cache(s.createCriteria(TransaksiPegawai.class, "t")
                .createAlias("t.jenisTransaksiPegawai", "j", Criteria.LEFT_JOIN)
                .add(Restrictions.eq("t.thn", year))
                .setProjection(Projections.projectionList().add(Projections.groupProperty("t.bln"))
                        .add(Projections.groupProperty("j.jenisTransaksi")).add(Projections.sum("t.nilai"))))
                .list();
        for (Object[] r : bulanRows) {
            int bln = (r[0] instanceof Number) ? ((Number) r[0]).intValue() : 0;
            int jenis = (r[1] instanceof Number) ? ((Number) r[1]).intValue() : 1;
            double nilai = dbl(r[2]);
            if (bln >= 1 && bln <= 12) {
                if (jenis != 2) {
                    t.debetPerBulan[bln - 1] += nilai;
                } else {
                    t.kreditPerBulan[bln - 1] += nilai;
                }
            }
        }
        return t;
    }

    // ════════════════════════════════════════════════════════════════════════
    // Query agregat — Cuti & Izin
    // ════════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private CutiData queryCuti(int year) {
        CutiData c = new CutiData();
        Session s = HibernateUtil.currentSession();
        Date start = startOfYear(year);
        Date end = endOfYear(year);

        Object[] agg = (Object[]) cache(s.createCriteria(CutiDanIzin.class, "c")
                .add(Restrictions.ge("c.mulai", start)).add(Restrictions.le("c.mulai", end))
                .setProjection(Projections.projectionList().add(Projections.rowCount())
                        .add(Projections.sum("c.jumlahHariCuti")))).uniqueResult();
        if (agg != null) {
            c.total = num(agg[0]);
            c.totalHari = dbl(agg[1]);
        }

        c.disetujui = num(cache(s.createCriteria(CutiDanIzin.class, "c").add(Restrictions.ge("c.mulai", start))
                .add(Restrictions.le("c.mulai", end)).add(Restrictions.eq("c.setujui", Boolean.TRUE))
                .setProjection(Projections.rowCount())).uniqueResult());

        // Distribusi per jenis cuti/izin
        List<Object[]> jenisRows = cache(s.createCriteria(CutiDanIzin.class, "c")
                .createAlias("c.jenisCutiDanIzin", "j", Criteria.LEFT_JOIN)
                .add(Restrictions.ge("c.mulai", start)).add(Restrictions.le("c.mulai", end))
                .setProjection(Projections.projectionList().add(Projections.groupProperty("j.nama"))
                        .add(Projections.rowCount())))
                .list();
        for (Object[] r : jenisRows) {
            String nama = r[0] == null ? "(Tanpa jenis)" : String.valueOf(r[0]);
            c.perJenis.put(nama, Long.valueOf(num(r[1])));
        }

        // Distribusi per status absensi
        List<Object[]> statusRows = cache(s.createCriteria(CutiDanIzin.class, "c")
                .createAlias("c.statusabsensi", "sa", Criteria.LEFT_JOIN)
                .add(Restrictions.ge("c.mulai", start)).add(Restrictions.le("c.mulai", end))
                .setProjection(Projections.projectionList().add(Projections.groupProperty("sa.nama"))
                        .add(Projections.rowCount())))
                .list();
        for (Object[] r : statusRows) {
            String nama = r[0] == null ? "(Tanpa status)" : String.valueOf(r[0]);
            c.perStatus.put(nama, Long.valueOf(num(r[1])));
        }

        // Tren per bulan: group by tanggal mulai (payload kecil) lalu bucket per bulan di Java
        List<Object[]> tglRows = cache(s.createCriteria(CutiDanIzin.class, "c").add(Restrictions.ge("c.mulai", start))
                .add(Restrictions.le("c.mulai", end)).setProjection(Projections.projectionList()
                        .add(Projections.groupProperty("c.mulai")).add(Projections.rowCount())))
                .list();
        for (Object[] r : tglRows) {
            if (r[0] instanceof Date) {
                int m = monthIndex((Date) r[0]);
                if (m >= 0 && m < 12) {
                    c.perBulan[m] += num(r[1]);
                }
            }
        }
        return c;
    }

    // ════════════════════════════════════════════════════════════════════════
    // Render
    // ════════════════════════════════════════════════════════════════════════

    private String judulFokus() {
        switch (fokus) {
            case GAJI:      return "Dasbor Pembayaran Gaji";
            case TRANSAKSI: return "Dasbor Transaksi Pegawai";
            case CUTI:      return "Dasbor Cuti & Izin";
            default:        return "Dasbor Analisis Penggajian";
        }
    }

    private void renderNow() {
        try {
            Common.clear(this);
            renderFilterBar();
            if (data == null) {
                appendHtml(this, CommonDashboardHtmlHelper.emptyState("Data dasbor belum tersedia."));
                return;
            }
            appendHtml(this, buildHero());
            Session s = HibernateUtil.currentSession();
            switch (fokus) {
                case GAJI:
                    appendHtml(this, buildGajiSection());
                    appendHtml(this, DasborPenggajianDetailHelper.panelGajiKategori(s, data.year, bulanMulai(), bulanSampai()));
                    appendHtml(this, DasborPenggajianDetailHelper.panelKomponenGajiFormula(s, data.year, bulanMulai(), bulanSampai()));
                    appendHtml(this, DasborPenggajianDetailHelper.panelKehadiranBulanan(s, data.year, bulanMulai(), bulanSampai()));
                    appendHtml(this, buildGajiMonthly());
                    break;
                case TRANSAKSI:
                    appendHtml(this, buildTransaksiSection());
                    appendHtml(this, DasborPenggajianDetailHelper.panelTunjanganHonor(s, data.year));
                    appendHtml(this, DasborPenggajianDetailHelper.panelDaftarPerjalananDinas(s, rentangMulai(), rentangSampai()));
                    appendHtml(this, buildTransaksiMonthly());
                    break;
                case CUTI:
                    appendHtml(this, buildCutiSection());
                    appendHtml(this, DasborPenggajianDetailHelper.panelDaftarCuti(s, rentangMulai(), rentangSampai()));
                    appendHtml(this, buildCutiMonthly());
                    break;
                default:
                    appendHtml(this, buildGajiSection());
                    appendHtml(this, DasborPenggajianDetailHelper.panelGajiKategori(s, data.year, bulanMulai(), bulanSampai()));
                    appendHtml(this, DasborPenggajianDetailHelper.panelKomponenGajiFormula(s, data.year, bulanMulai(), bulanSampai()));
                    appendHtml(this, DasborPenggajianDetailHelper.panelKehadiranBulanan(s, data.year, bulanMulai(), bulanSampai()));
                    appendHtml(this, buildMonthlyTable());
                    appendHtml(this, buildTransaksiSection());
                    appendHtml(this, DasborPenggajianDetailHelper.panelTunjanganHonor(s, data.year));
                    appendHtml(this, DasborPenggajianDetailHelper.panelDaftarPerjalananDinas(s, rentangMulai(), rentangSampai()));
                    appendHtml(this, buildCutiSection());
                    appendHtml(this, DasborPenggajianDetailHelper.panelDaftarCuti(s, rentangMulai(), rentangSampai()));
                    break;
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            appendHtml(this, CommonDashboardHtmlHelper.errorState(judulFokus(), e));
        }
    }

    private void renderFilterBar() {
        Div bar = new Div();
        bar.setStyle("background:#fff; border-radius:10px; padding:10px 14px; box-shadow:0 8px 20px rgba(15,23,42,.06);"
                + " margin-bottom:12px; display:flex; align-items:center; gap:10px; flex-wrap:wrap;");
        bar.setParent(this);

        appendHtml(bar, "<span style='font-size:12px; font-weight:800; color:#475569;'>Tahun Penggajian</span>");

        cbTahun = new Combobox();
        cbTahun.setReadonly(true);
        cbTahun.setWidth("110px");
        cbTahun.setParent(bar);
        Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
        int now = cal.get(Calendar.YEAR);
        for (int y = now; y >= now - 6; y--) {
            Comboitem ci = new Comboitem(String.valueOf(y));
            ci.setValue(Integer.valueOf(y));
            ci.setParent(cbTahun);
            if (y == filterTahun) {
                cbTahun.setSelectedItem(ci);
            }
        }
        if (cbTahun.getSelectedItem() == null) {
            cbTahun.setSelectedIndex(0);
        }

        appendHtml(bar, "<span style='font-size:12px; font-weight:800; color:#475569;'>Rentang Tgl (gaji, cuti &amp; perjalanan)</span>");
        final org.zkoss.zul.Datebox dbMulai = new org.zkoss.zul.Datebox();
        dbMulai.setWidth("130px");
        dbMulai.setValue(filterMulai);
        dbMulai.setParent(bar);
        appendHtml(bar, "<span style='font-size:12px; color:#94a3b8;'>s/d</span>");
        final org.zkoss.zul.Datebox dbSampai = new org.zkoss.zul.Datebox();
        dbSampai.setWidth("130px");
        dbSampai.setValue(filterSampai);
        dbSampai.setParent(bar);

        MyToolbarbuttonConfig btnTampil = new MyToolbarbuttonConfig("Tampilkan", "/img/search.gif");
        btnTampil.setParent(bar);
        btnTampil.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception {
                if (cbTahun.getSelectedItem() != null) {
                    Object v = cbTahun.getSelectedItem().getValue();
                    if (v instanceof Integer) {
                        filterTahun = ((Integer) v).intValue();
                    }
                }
                filterMulai = dbMulai.getValue();
                filterSampai = dbSampai.getValue();
                loadAndRender();
            }
        });

        MyToolbarbuttonConfig btnRefresh = new MyToolbarbuttonConfig("Muat Ulang (Reset Cache)", "/img/refresh.gif");
        btnRefresh.setParent(bar);
        btnRefresh.addEventListener("onClick", new EventListener() {
            public void onEvent(Event e) throws Exception {
                // Invalidasi semua cache payroll → paksa hitung ulang dari database.
                DashboardCache.invalidateContaining("payroll.");
                loadAndRender();
            }
        });

        appendHtml(bar, "<span style='font-size:11px; color:#94a3b8;'>Data di-cache (L1 90 dtk / L3 30 mnt)."
                + " Klik <b>Muat Ulang</b> untuk angka terbaru.</span>");
    }

    private String buildHero() {
        StringBuilder badges = new StringBuilder();
        String sub;
        if (fokus == Fokus.GAJI) {
            GajiData g = data.gaji;
            badges.append(heroBadge(money(g.totalNilai) + " total gaji"));
            badges.append(heroBadge(angka(g.totalSlip) + " slip"));
            badges.append(heroBadge(g.pegawaiDibayar + " pegawai dibayar"));
            badges.append(heroBadge(g.totalBatch + " batch pembayaran"));
            sub = "Ringkasan pembayaran gaji pegawai tahun <b>" + data.year + "</b>.";
        } else if (fokus == Fokus.TRANSAKSI) {
            TransaksiData t = data.transaksi;
            badges.append(heroBadge(money(t.totalDebet) + " tunjangan"));
            badges.append(heroBadge(money(t.totalKredit) + " potongan"));
            badges.append(heroBadge(money(t.totalDebet - t.totalKredit) + " selisih"));
            badges.append(heroBadge(t.totalTransaksi + " transaksi"));
            sub = "Tunjangan (Debet) dan potongan (Kredit) pegawai tahun <b>" + data.year + "</b>.";
        } else if (fokus == Fokus.CUTI) {
            CutiData c = data.cuti;
            badges.append(heroBadge(c.total + " pengajuan"));
            badges.append(heroBadge(c.disetujui + " disetujui"));
            badges.append(heroBadge((c.total - c.disetujui) + " belum disetujui"));
            badges.append(heroBadge(angka((long) c.totalHari) + " hari cuti"));
            sub = "Pengajuan cuti &amp; izin pegawai tahun <b>" + data.year + "</b>.";
        } else {
            GajiData g = data.gaji;
            TransaksiData t = data.transaksi;
            CutiData c = data.cuti;
            badges.append(heroBadge(money(g.totalNilai) + " total gaji"));
            badges.append(heroBadge(g.pegawaiDibayar + " pegawai dibayar"));
            badges.append(heroBadge(t.totalTransaksi + " transaksi pegawai"));
            badges.append(heroBadge(c.total + " pengajuan cuti/izin"));
            sub = "Ringkasan penggajian, transaksi pegawai, serta cuti &amp; izin tahun <b>" + data.year
                + "</b> dalam satu layar.";
        }
        return "<div style='border-radius:12px; padding:18px 22px; margin-bottom:12px;"
                + " background:linear-gradient(135deg,#0f766e 0%,#0ea5e9 100%); color:#fff;"
                + " box-shadow:0 10px 26px rgba(13,148,136,.25);'>"
                + "<div style='font-size:20px; font-weight:900; letter-spacing:-.02em;'>" + judulFokus() + "</div>"
                + "<div style='font-size:12px; opacity:.9; margin-top:4px; line-height:1.5;'>" + sub + "</div>"
                + "<div style='margin-top:12px; display:flex; gap:8px; flex-wrap:wrap;'>" + badges + "</div></div>";
    }

    private String heroBadge(String text) {
        return "<span style='display:inline-block; border-radius:999px; padding:4px 12px; font-size:11px;"
                + " font-weight:700; background:rgba(255,255,255,.2);'>" + CommonDashboardHtmlHelper.escape(text)
                + "</span>";
    }

    // ── Penggajian ──────────────────────────────────────────────────────────

    private String buildGajiSection() {
        GajiData g = data.gaji;
        long belumPosting = g.totalBatch - g.batchPosted;
        String cards = CommonDashboardHtmlHelper.cards(new String[] {
                CommonDashboardHtmlHelper.metricCard("Total Nilai Gaji", money(g.totalNilai),
                        "Akumulasi seluruh slip gaji tahun " + data.year),
                CommonDashboardHtmlHelper.metricCard("Slip Gaji", angka(g.totalSlip),
                        "Jumlah baris pembayaran gaji pegawai"),
                CommonDashboardHtmlHelper.metricCard("Pegawai Dibayar", angka(g.pegawaiDibayar),
                        "Pegawai unik yang menerima gaji"),
                CommonDashboardHtmlHelper.metricCard("Batch Pembayaran", angka(g.totalBatch),
                        g.batchPosted + " sudah posting, " + belumPosting + " belum") });

        StringBuilder cara = new StringBuilder();
        double maxCara = 0;
        for (Double v : g.caraBayar.values()) {
            if (v != null && v.doubleValue() > maxCara) {
                maxCara = v.doubleValue();
            }
        }
        if (g.caraBayar.isEmpty()) {
            cara.append(CommonDashboardHtmlHelper.emptyState("Belum ada data cara pembayaran gaji."));
        } else {
            for (Map.Entry<String, Double> e : g.caraBayar.entrySet()) {
                cara.append(moneyBar(e.getKey(), e.getValue() == null ? 0 : e.getValue().doubleValue(), maxCara));
            }
        }

        return CommonDashboardHtmlHelper.panel("Ringkasan Penggajian", "Kondisi pembayaran gaji tahun berjalan.",
                cards + "<div style='margin-top:6px; font-size:13px; font-weight:800; color:#0f172a;'>"
                        + "Distribusi Nilai Gaji per Cara Pembayaran</div>"
                        + "<div style='margin-top:8px;'>" + cara + "</div>");
    }

    private String buildMonthlyTable() {
        GajiData g = data.gaji;
        TransaksiData t = data.transaksi;
        CutiData c = data.cuti;
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='overflow-x:auto;'>");
        sb.append("<table style='width:100%; border-collapse:collapse; font-size:11px; font-family:Arial,sans-serif;'>");
        sb.append("<tr style='background:#f1f5f9;'>");
        String[] head = { "Bulan", "Nilai Gaji", "Slip", "Transaksi Debet", "Transaksi Kredit", "Cuti/Izin" };
        for (int i = 0; i < head.length; i++) {
            String alignH = i == 0 ? "left" : "right";
            sb.append("<th style='padding:7px 9px; text-align:" + alignH
                    + "; color:#475569; font-weight:800; border-bottom:2px solid #e2e8f0; white-space:nowrap;'>")
                    .append(CommonDashboardHtmlHelper.escape(head[i])).append("</th>");
        }
        sb.append("</tr>");
        for (int m = 0; m < 12; m++) {
            String bg = (m % 2 == 0) ? "#ffffff" : "#f8fafc";
            sb.append("<tr style='background:" + bg + "; border-bottom:1px solid #f1f5f9;'>");
            sb.append(tdL(BULAN[m]));
            sb.append(tdR(g.nilaiPerBulan[m] > 0 ? money(g.nilaiPerBulan[m]) : "-"));
            sb.append(tdR(g.slipPerBulan[m] > 0 ? angka(g.slipPerBulan[m]) : "-"));
            sb.append(tdR(t.debetPerBulan[m] > 0 ? money(t.debetPerBulan[m]) : "-"));
            sb.append(tdR(t.kreditPerBulan[m] > 0 ? money(t.kreditPerBulan[m]) : "-"));
            sb.append(tdR(c.perBulan[m] > 0 ? angka(c.perBulan[m]) : "-"));
            sb.append("</tr>");
        }
        // Baris total
        sb.append("<tr style='background:#0f172a; color:#fff; font-weight:800;'>");
        sb.append("<td style='padding:8px 9px;'>Total</td>");
        sb.append(tdRDark(money(g.totalNilai)));
        sb.append(tdRDark(angka(g.totalSlip)));
        sb.append(tdRDark(money(t.totalDebet)));
        sb.append(tdRDark(money(t.totalKredit)));
        sb.append(tdRDark(angka(c.total)));
        sb.append("</tr>");
        sb.append("</table></div>");

        return CommonDashboardHtmlHelper.panel("Rekap Bulanan Terpadu",
                "Perbandingan nilai gaji, transaksi, dan cuti per bulan dalam satu tabel.", sb.toString());
    }

    private String buildGajiMonthly() {
        GajiData g = data.gaji;
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='overflow-x:auto;'>");
        sb.append("<table style='width:100%; border-collapse:collapse; font-size:11px; font-family:Arial,sans-serif;'>");
        sb.append("<tr style='background:#f1f5f9;'>");
        String[] head = { "Bulan", "Nilai Gaji", "Jumlah Slip" };
        for (int i = 0; i < head.length; i++) {
            sb.append("<th style='padding:7px 9px; text-align:" + (i == 0 ? "left" : "right")
                    + "; color:#475569; font-weight:800; border-bottom:2px solid #e2e8f0; white-space:nowrap;'>")
                    .append(CommonDashboardHtmlHelper.escape(head[i])).append("</th>");
        }
        sb.append("</tr>");
        for (int m = 0; m < 12; m++) {
            sb.append("<tr style='background:" + (m % 2 == 0 ? "#ffffff" : "#f8fafc") + "; border-bottom:1px solid #f1f5f9;'>");
            sb.append(tdL(BULAN[m]));
            sb.append(tdR(g.nilaiPerBulan[m] > 0 ? money(g.nilaiPerBulan[m]) : "-"));
            sb.append(tdR(g.slipPerBulan[m] > 0 ? angka(g.slipPerBulan[m]) : "-"));
            sb.append("</tr>");
        }
        sb.append("<tr style='background:#0f172a; color:#fff; font-weight:800;'>");
        sb.append("<td style='padding:8px 9px;'>Total</td>");
        sb.append(tdRDark(money(g.totalNilai)));
        sb.append(tdRDark(angka(g.totalSlip)));
        sb.append("</tr></table></div>");
        return CommonDashboardHtmlHelper.panel("Tren Pembayaran Gaji per Bulan",
                "Nilai gaji yang dibayarkan dan jumlah slip pada tiap bulan.", sb.toString());
    }

    private String buildTransaksiMonthly() {
        TransaksiData t = data.transaksi;
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='overflow-x:auto;'>");
        sb.append("<table style='width:100%; border-collapse:collapse; font-size:11px; font-family:Arial,sans-serif;'>");
        sb.append("<tr style='background:#f1f5f9;'>");
        String[] head = { "Bulan", "Debet (Tunjangan)", "Kredit (Potongan)", "Selisih" };
        for (int i = 0; i < head.length; i++) {
            sb.append("<th style='padding:7px 9px; text-align:" + (i == 0 ? "left" : "right")
                    + "; color:#475569; font-weight:800; border-bottom:2px solid #e2e8f0; white-space:nowrap;'>")
                    .append(CommonDashboardHtmlHelper.escape(head[i])).append("</th>");
        }
        sb.append("</tr>");
        for (int m = 0; m < 12; m++) {
            double net = t.debetPerBulan[m] - t.kreditPerBulan[m];
            boolean ada = t.debetPerBulan[m] > 0 || t.kreditPerBulan[m] > 0;
            sb.append("<tr style='background:" + (m % 2 == 0 ? "#ffffff" : "#f8fafc") + "; border-bottom:1px solid #f1f5f9;'>");
            sb.append(tdL(BULAN[m]));
            sb.append(tdR(t.debetPerBulan[m] > 0 ? money(t.debetPerBulan[m]) : "-"));
            sb.append(tdR(t.kreditPerBulan[m] > 0 ? money(t.kreditPerBulan[m]) : "-"));
            sb.append(tdR(ada ? money(net) : "-"));
            sb.append("</tr>");
        }
        sb.append("<tr style='background:#0f172a; color:#fff; font-weight:800;'>");
        sb.append("<td style='padding:8px 9px;'>Total</td>");
        sb.append(tdRDark(money(t.totalDebet)));
        sb.append(tdRDark(money(t.totalKredit)));
        sb.append(tdRDark(money(t.totalDebet - t.totalKredit)));
        sb.append("</tr></table></div>");
        return CommonDashboardHtmlHelper.panel("Tren Transaksi per Bulan",
                "Tunjangan (Debet) dan potongan (Kredit) pegawai pada tiap bulan.", sb.toString());
    }

    private String buildCutiMonthly() {
        CutiData c = data.cuti;
        long maxBulan = 0;
        for (int m = 0; m < 12; m++) {
            if (c.perBulan[m] > maxBulan) {
                maxBulan = c.perBulan[m];
            }
        }
        StringBuilder bars = new StringBuilder();
        if (maxBulan == 0) {
            bars.append(CommonDashboardHtmlHelper.emptyState("Belum ada pengajuan cuti/izin bertanggal pada tahun ini."));
        } else {
            for (int m = 0; m < 12; m++) {
                int w = CommonDashboardHtmlHelper.percent(c.perBulan[m], maxBulan);
                bars.append("<div style='display:flex; align-items:center; gap:8px; margin:5px 0;'>")
                        .append("<div style='width:40px; font-size:11px; color:#475569; font-weight:700;'>").append(BULAN[m]).append("</div>")
                        .append("<div style='flex:1; height:14px; background:#e2e8f0; border-radius:999px; overflow:hidden;'>")
                        .append("<div style='height:14px; width:").append(w).append("%; border-radius:999px; background:var(--ais-theme-primary,#2563eb);'></div></div>")
                        .append("<div style='width:42px; text-align:right; font-size:11px; font-weight:800; color:#0f172a;'>")
                        .append(c.perBulan[m] > 0 ? angka(c.perBulan[m]) : "-").append("</div></div>");
            }
        }
        return CommonDashboardHtmlHelper.panel("Tren Cuti & Izin per Bulan",
                "Banyaknya pengajuan cuti/izin pada tiap bulan.", bars.toString());
    }

    // ── Transaksi ────────────────────────────────────────────────────────────

    private String buildTransaksiSection() {
        TransaksiData t = data.transaksi;
        double net = t.totalDebet - t.totalKredit;
        String cards = CommonDashboardHtmlHelper.cards(new String[] {
                CommonDashboardHtmlHelper.metricCard("Debet (Tunjangan/Penambah)", money(t.totalDebet),
                        "Total transaksi yang menambah gaji"),
                CommonDashboardHtmlHelper.metricCard("Kredit (Potongan)", money(t.totalKredit),
                        "Total transaksi yang mengurangi gaji"),
                CommonDashboardHtmlHelper.metricCard("Selisih (Debet - Kredit)", money(net),
                        "Dampak bersih transaksi pegawai"),
                CommonDashboardHtmlHelper.metricCard("Jumlah Transaksi", angka(t.totalTransaksi),
                        "Banyaknya baris transaksi pegawai") });

        StringBuilder jenis = new StringBuilder();
        if (t.perJenis.isEmpty()) {
            jenis.append(CommonDashboardHtmlHelper.emptyState("Belum ada transaksi pegawai pada tahun ini."));
        } else {
            double maxJenis = 0;
            for (Object[] r : t.perJenis) {
                double v = ((Double) r[2]).doubleValue();
                if (v > maxJenis) {
                    maxJenis = v;
                }
            }
            int shown = 0;
            for (Object[] r : t.perJenis) {
                if (shown++ >= 12) {
                    break;
                }
                String nama = (String) r[0];
                boolean debet = ((Boolean) r[1]).booleanValue();
                double nilai = ((Double) r[2]).doubleValue();
                long cnt = ((Long) r[3]).longValue();
                String tag = debet
                        ? "<span style='font-size:9px;font-weight:800;color:#166534;background:#dcfce7;border-radius:999px;padding:1px 7px;'>DEBET</span>"
                        : "<span style='font-size:9px;font-weight:800;color:#991b1b;background:#fee2e2;border-radius:999px;padding:1px 7px;'>KREDIT</span>";
                jenis.append(coloredMoneyBar(CommonDashboardHtmlHelper.escape(nama) + " " + tag
                        + " <span style='color:#94a3b8;'>(" + cnt + "x)</span>", nilai, maxJenis,
                        debet ? "#16a34a" : "#dc2626"));
            }
        }

        return CommonDashboardHtmlHelper.panel("Analisis Transaksi Pegawai",
                "Tunjangan (Debet) dan potongan (Kredit) per jenis transaksi.",
                cards + "<div style='margin-top:6px; font-size:13px; font-weight:800; color:#0f172a;'>"
                        + "Rekap per Jenis Transaksi</div><div style='margin-top:8px;'>" + jenis + "</div>");
    }

    // ── Cuti & Izin ────────────────────────────────────────────────────────────

    private String buildCutiSection() {
        CutiData c = data.cuti;
        long belum = c.total - c.disetujui;
        String cards = CommonDashboardHtmlHelper.cards(new String[] {
                CommonDashboardHtmlHelper.metricCard("Total Pengajuan", angka(c.total),
                        "Cuti & izin diajukan tahun " + data.year),
                CommonDashboardHtmlHelper.metricCard("Disetujui", angka(c.disetujui),
                        persen(c.disetujui, c.total) + "% dari total pengajuan"),
                CommonDashboardHtmlHelper.metricCard("Belum Disetujui", angka(belum),
                        "Menunggu persetujuan atau ditolak"),
                CommonDashboardHtmlHelper.metricCard("Total Hari Cuti", angka((long) c.totalHari),
                        "Akumulasi jumlah hari cuti") });

        String jenis = distribusiBar(c.perJenis, "var(--ais-theme-primary,#2563eb)", "Belum ada data jenis cuti/izin.");
        String status = distribusiBar(c.perStatus, "#7c3aed", "Belum ada data status absensi.");

        String dua = "<div style='display:flex; gap:14px; flex-wrap:wrap;'>"
                + "<div style='flex:1 1 280px; min-width:240px;'>"
                + "<div style='font-size:13px; font-weight:800; color:#0f172a; margin-bottom:6px;'>Per Jenis Cuti/Izin</div>"
                + jenis + "</div>"
                + "<div style='flex:1 1 280px; min-width:240px;'>"
                + "<div style='font-size:13px; font-weight:800; color:#0f172a; margin-bottom:6px;'>Per Status Absensi</div>"
                + status + "</div></div>";

        return CommonDashboardHtmlHelper.panel("Analisis Cuti & Izin",
                "Status persetujuan serta distribusi jenis dan status kehadiran.", cards + dua);
    }

    private String distribusiBar(Map<String, Long> map, String color, String emptyMsg) {
        if (map == null || map.isEmpty()) {
            return CommonDashboardHtmlHelper.emptyState(emptyMsg);
        }
        long max = 0;
        for (Long v : map.values()) {
            if (v != null && v.longValue() > max) {
                max = v.longValue();
            }
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Long> e : map.entrySet()) {
            long v = e.getValue() == null ? 0 : e.getValue().longValue();
            int w = CommonDashboardHtmlHelper.percent(v, max);
            sb.append("<div style='display:flex; align-items:center; gap:8px; margin:6px 0;'>")
                    .append("<div style='width:150px; font-size:11px; color:#475569; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;'>")
                    .append(CommonDashboardHtmlHelper.escape(e.getKey())).append("</div>")
                    .append("<div style='flex:1; height:12px; background:#e2e8f0; border-radius:999px; overflow:hidden;'>")
                    .append("<div style='height:12px; width:").append(w).append("%; border-radius:999px; background:")
                    .append(color).append(";'></div></div>")
                    .append("<div style='width:48px; text-align:right; font-size:11px; font-weight:800; color:#0f172a;'>")
                    .append(v).append("</div></div>");
        }
        return sb.toString();
    }

    // ════════════════════════════════════════════════════════════════════════
    // HTML helpers
    // ════════════════════════════════════════════════════════════════════════

    private String moneyBar(String label, double value, double max) {
        return coloredMoneyBar(CommonDashboardHtmlHelper.escape(label), value, max, "#0ea5e9");
    }

    private String coloredMoneyBar(String labelHtml, double value, double max, String color) {
        int w = CommonDashboardHtmlHelper.percent(value, max);
        return "<div style='display:flex; align-items:center; gap:8px; margin:6px 0;'>"
                + "<div style='width:230px; font-size:11px; color:#475569; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;'>"
                + labelHtml + "</div>"
                + "<div style='flex:1; height:12px; background:#e2e8f0; border-radius:999px; overflow:hidden;'>"
                + "<div style='height:12px; width:" + w + "%; border-radius:999px; background:" + color + ";'></div></div>"
                + "<div style='width:120px; text-align:right; font-size:11px; font-weight:800; color:#0f172a;'>"
                + money(value) + "</div></div>";
    }

    private static String tdL(String v) {
        return "<td style='padding:6px 9px; text-align:left; color:#334155; font-weight:700;'>"
                + CommonDashboardHtmlHelper.escape(v) + "</td>";
    }

    private static String tdR(String v) {
        return "<td style='padding:6px 9px; text-align:right; color:#334155;'>"
                + CommonDashboardHtmlHelper.escape(v) + "</td>";
    }

    private static String tdRDark(String v) {
        return "<td style='padding:8px 9px; text-align:right;'>" + CommonDashboardHtmlHelper.escape(v) + "</td>";
    }

    private void appendHtml(org.zkoss.zk.ui.Component parent, String html) {
        new Html(html).setParent(parent);
    }

    private Criteria cache(Criteria c) {
        return DashboardCache.cacheable(c);
    }

    private static long num(Object o) {
        return (o instanceof Number) ? ((Number) o).longValue() : 0L;
    }

    private static double dbl(Object o) {
        return (o instanceof Number) ? ((Number) o).doubleValue() : 0.0;
    }

    private static int persen(long value, long total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round(value * 100.0 / total);
    }

    private static String money(double v) {
        try {
            return "Rp " + Common.numberFormat.get().format(Math.round(v));
        } catch (Exception e) {
            return "Rp " + (long) v;
        }
    }

    private static String angka(long v) {
        try {
            return Common.numberFormat.get().format(v);
        } catch (Exception e) {
            return String.valueOf(v);
        }
    }

    private static Date startOfYear(int year) {
        Calendar c = ais.ui.util.WaktuUtil.getCalendar();
        c.clear();
        c.set(year, Calendar.JANUARY, 1, 0, 0, 0);
        return c.getTime();
    }

    private static Date endOfYear(int year) {
        Calendar c = ais.ui.util.WaktuUtil.getCalendar();
        c.clear();
        c.set(year, Calendar.DECEMBER, 31, 23, 59, 59);
        return c.getTime();
    }

    /** Awal rentang untuk panel cuti/perjalanan: filter tanggal bila ada, else awal tahun. */
    private Date rentangMulai() {
        return filterMulai != null ? filterMulai : startOfYear(filterTahun);
    }

    /** Akhir rentang untuk panel cuti/perjalanan: filter tanggal bila ada, else akhir tahun. */
    private Date rentangSampai() {
        return filterSampai != null ? filterSampai : endOfYear(filterTahun);
    }

    /** Bulan awal (1-12) dari rentang tanggal; 1 bila tidak diisi. Dipakai memfilter GAJI. */
    private int bulanMulai() {
        if (filterMulai == null) {
            return 1;
        }
        Calendar c = ais.ui.util.WaktuUtil.getCalendar();
        c.setTime(filterMulai);
        return c.get(Calendar.MONTH) + 1;
    }

    /** Bulan akhir (1-12) dari rentang tanggal; 12 bila tidak diisi. Dipakai memfilter GAJI. */
    private int bulanSampai() {
        if (filterSampai == null) {
            return 12;
        }
        Calendar c = ais.ui.util.WaktuUtil.getCalendar();
        c.setTime(filterSampai);
        return c.get(Calendar.MONTH) + 1;
    }

    private static int monthIndex(Date d) {
        Calendar c = ais.ui.util.WaktuUtil.getCalendar();
        c.setTime(d);
        return c.get(Calendar.MONTH);
    }
}
