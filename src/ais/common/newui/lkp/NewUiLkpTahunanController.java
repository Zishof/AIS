package ais.common.newui.lkp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.newui.NewUiRouteGuard;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AsesorPegawai;
import ais.database.model.Dosen;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.lkp.KegiatanTugasJabatan;
import ais.database.model.lkp.RealisasiKerjaPegawai;
import ais.database.model.lkp.TargetKerjaPegawai;
import ais.database.model.rab.SatuanKerja;

/**
 * Kontrak native dua layar Laporan Kinerja Pegawai (LKP) periode tahunan.
 *
 * <p>Target Kerja Pegawai Tahunan dan Realisasi Kerja Pegawai Tahunan bekerja
 * pada daftar yang sama — {@link TargetKerjaPegawai} untuk kegiatan berperiode
 * {@code Tahunan} — dan hanya berbeda pada apa yang ditonjolkan: yang pertama
 * angka targetnya, yang kedua capaian terhadap target itu. Karena itu keduanya
 * dilayani satu kontrak dengan dua mode.</p>
 *
 * <h3>Penyaringan asesor: bukan sekadar "pegawai ini"</h3>
 * <p>Ini bagian yang paling mudah salah ditiru. Ketika seorang pegawai dipilih,
 * layar ZK <b>tidak</b> menyaring baris menjadi milik pegawai itu saja,
 * melainkan menjadi milik pegawai itu <i>beserta seluruh pegawai yang ia
 * asesmen</i> ({@link AsesorPegawai} yang aktif). Menyaring dengan
 * {@code pegawai = ?} akan menghasilkan daftar yang jauh lebih pendek dan
 * tampak benar — cacat yang hanya ketahuan bila seseorang membandingkan dengan
 * layar lama. Penyaringan yang sama disalin apa adanya ke sini.</p>
 *
 * <h3>Baca saja</h3>
 * <p>Penambahan target menuntut pemilih pohon kegiatan tugas jabatan, dan
 * pencatatan realisasi ditangani action tersendiri yang membawa parameter
 * tambahan per kegiatan. Menandai verifikasi pun kewenangan terpisah. Kontrak
 * ini karena itu menyajikan daftar dan rinciannya saja; menyediakan sebagian
 * dari alur penilaian kinerja lebih berisiko daripada berguna.</p>
 *
 * <p>Fail-closed: mode tak dikenal ditolak, sesi tanpa pengguna ditolak, dan
 * seluruh aksi hanya membaca.</p>
 */
public final class NewUiLkpTahunanController {

    /** Harus sama dengan awalan folder JSP sebelum {@code /uiux/}. */
    private static final String MODULE = "lkp";

    /** Target Kerja Pegawai Tahunan. */
    public static final String MODE_TARGET = "target";
    /** Realisasi Kerja Pegawai Tahunan. */
    public static final String MODE_REALISASI = "realisasi";

    private static final int BATAS_BARIS = 200;

    private NewUiLkpTahunanController() { }

    public static void handle(HttpServletRequest request, HttpServletResponse response,
            String mode, String pageKey) throws Exception {
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        JSONObject json = new JSONObject();
        try {
            if (!modeDikenal(mode)) throw new IllegalArgumentException("Mode LKP tidak dikenal.");
            String action = text(request.getParameter("action"), "meta");
            if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, pageKey, action)) {
                response.setStatus(403);
                fail(json, "ACTION_FORBIDDEN", "Hak akses tidak tersedia.");
                write(response, json);
                return;
            }
            Tbmuser user = Common.getCurrentUser(request);
            if (user == null) throw new SecurityException("Sesi tidak dikenal.");

            if ("meta".equals(action)) meta(json, mode);
            else if ("lookup".equals(action)) lookup(json, request);
            else if ("list".equals(action)) daftar(json, request, mode);
            else if ("detail".equals(action)) detail(json, request);
            else throw new IllegalArgumentException("Aksi tidak dikenal.");
            json.put("ok", true);
        } catch (SecurityException e) {
            response.setStatus(403);
            fail(json, "FORBIDDEN", e.getMessage());
        } catch (IllegalArgumentException e) {
            response.setStatus(422);
            fail(json, "VALIDATION_FAILED", e.getMessage());
        } catch (Exception e) {
            response.setStatus(500);
            fail(json, "INTERNAL_ERROR", "Gagal memuat data LKP. Detail dicatat di log server.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiLkpTahunanController"); } catch (Exception ignored) { }
        }
        write(response, json);
    }

    public static boolean modeDikenal(String mode) {
        return MODE_TARGET.equals(mode) || MODE_REALISASI.equals(mode);
    }

    static String judul(String mode) {
        return MODE_TARGET.equals(mode) ? "Target Kerja Pegawai Tahunan"
                : "Realisasi Kerja Pegawai Tahunan";
    }

    // ------------------------------------------------------------------ meta

    private static void meta(JSONObject j, String mode) throws Exception {
        j.put("judul", judul(mode));
        j.put("mode", mode);
        j.put("periode", KegiatanTugasJabatan.TAHUNAN);
        // Layar tahunan memang TIDAK punya saringan bulan; dinyatakan agar klien
        // tidak menampilkannya lalu mengirim filter yang diabaikan server.
        j.put("pakaiBulan", false);
        j.put("bolehUbah", false);
        j.put("alasanBacaSaja", "Penambahan target menuntut pemilih pohon kegiatan tugas jabatan, "
                + "pencatatan realisasi ditangani layar tersendiri yang membawa parameter tambahan "
                + "per kegiatan, dan penandaan verifikasi adalah kewenangan terpisah. "
                + "Ketiganya tetap dilakukan di layar lama.");
        j.put("catatanPegawai", "Memilih pegawai menampilkan baris miliknya BESERTA pegawai yang "
                + "ia asesmen, sama seperti layar lama.");

        int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
        JSONArray pilihan = new JSONArray();
        for (int i = tahun + 1; i >= tahun - 10; i--) {
            pilihan.put(i);
        }
        j.put("pilihanTahun", pilihan);
        j.put("tahunBawaan", tahun);
        j.put("batasBaris", BATAS_BARIS);
    }

    /** Pencarian pegawai untuk saringan; daftar awal tampil tanpa mengetik. */
    private static void lookup(JSONObject j, HttpServletRequest r) throws Exception {
        String q = text(r.getParameter("q"), "").trim();
        JSONArray arr = new JSONArray();
        Session s = HibernateUtil.openSession();
        try {
            Criteria c = s.createCriteria(Pegawai.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .addOrder(Order.asc("nama")).setMaxResults(50);
            if (q.length() >= 2) {
                c.add(Restrictions.or(Restrictions.ilike("nama", "%" + q + "%"),
                        Restrictions.ilike("mycode", "%" + q + "%")));
            }
            for (Object o : c.list()) {
                Pegawai p = (Pegawai) o;
                arr.put(new JSONObject().put("id", p.getId()).put("nama", teks(p.getNama()))
                        .put("kode", teks(p.getMycode())));
            }
        } finally {
            s.close();
        }
        j.put("pegawai", arr);
        j.put("total", arr.length());
    }

    // ------------------------------------------------------------------ list

    @SuppressWarnings("unchecked")
    private static void daftar(JSONObject j, HttpServletRequest r, String mode) throws Exception {
        Session session = HibernateUtil.currentSession();
        int tahun = angka(r.getParameter("tahun"),
                ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));
        String nama = text(r.getParameter("q"), "").trim();
        Pegawai pegawai = pegawaiParam(session, r.getParameter("pegawaiId"));

        Criteria criteria = session.createCriteria(TargetKerjaPegawai.class)
                .createAlias("kegiatanTugasJabatan", "kegiatanTugasJabatan")
                .add(Restrictions.eq("kegiatanTugasJabatan.periode", KegiatanTugasJabatan.TAHUNAN));

        // Cakupan satuan kerja mengikuti pengguna aktif, sama seperti layar ZK.
        Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
        if (satuanKerjas != null && !satuanKerjas.isEmpty()) {
            criteria.add(Restrictions.in("kegiatanTugasJabatan.satuanKerja", satuanKerjas));
        }
        criteria.add(Restrictions.eq("tahun", Integer.valueOf(tahun)));
        if (nama.length() > 0) {
            criteria.add(Restrictions.ilike("kegiatanTugasJabatan.nama", nama, MatchMode.ANYWHERE));
        }
        List<Pegawai> lingkup = lingkupAsesor(session, pegawai);
        if (lingkup != null) {
            criteria.add(Restrictions.in("pegawai", lingkup));
        }
        criteria.addOrder(Order.asc("kegiatanTugasJabatan.noUrut")).addOrder(Order.desc("id"));
        criteria.setMaxResults(BATAS_BARIS);

        JSONArray rows = new JSONArray();
        for (Object o : criteria.list()) {
            TargetKerjaPegawai t = (TargetKerjaPegawai) o;
            JSONObject b = new JSONObject()
                    .put("id", t.getId())
                    .put("kegiatan", t.getKegiatanTugasJabatan() == null ? "-"
                            : teks(t.getKegiatanTugasJabatan().getNama()))
                    .put("pegawai", t.getPegawai() == null ? "-" : teks(t.getPegawai().getNama()))
                    .put("tahun", t.getTahun() == null ? 0 : t.getTahun().intValue())
                    .put("targetKuantitas", nilai(t.getKuantitas()))
                    .put("targetKualitas", nilai(t.getKualitas()))
                    .put("targetWaktu", nilai(t.getWaktu()))
                    .put("targetBiaya", nilai(t.getBiaya()))
                    .put("terverifikasi", Boolean.TRUE.equals(t.getVerifikasi()))
                    .put("catatan", teks(t.getCatatan()));
            if (MODE_REALISASI.equals(mode)) {
                // Layar realisasi menonjolkan capaian; angkanya dijumlahkan di
                // basis data agar tidak perlu memuat seluruh baris anak.
                b.put("realisasi", rekapRealisasi(session, t));
            }
            rows.put(b);
        }
        j.put("rows", rows);
        j.put("tahun", tahun);
        j.put("jumlah", rows.length());
        j.put("terpotong", rows.length() >= BATAS_BARIS);
        if (pegawai != null) {
            j.put("lingkupPegawai", lingkup == null ? 0 : lingkup.size());
        }
    }

    /**
     * Pegawai yang datanya boleh tampil ketika seorang pegawai dipilih.
     *
     * <p>Bukan hanya pegawai itu: layar ZK menyertakan seluruh pegawai yang ia
     * asesmen lewat {@link AsesorPegawai} aktif. Menyalin perilaku ini penting —
     * menyaring dengan {@code pegawai = ?} saja menghasilkan daftar yang lebih
     * pendek namun tampak wajar.</p>
     *
     * @return null bila tidak ada pegawai dipilih (tanpa penyaringan)
     */
    @SuppressWarnings("unchecked")
    private static List<Pegawai> lingkupAsesor(Session session, Pegawai pegawai) {
        if (pegawai == null) return null;
        List<Pegawai> hasil = new ArrayList<Pegawai>();
        try {
            Dosen dosen = pegawai.getDosen();
            List<Pegawai> asesmen = session.createCriteria(AsesorPegawai.class)
                    .setProjection(Projections.groupProperty("pegawai"))
                    .createAlias("asesor", "asesor")
                    .add(Restrictions.or(Restrictions.isNull("asesor.aktif"),
                            Restrictions.eq("asesor.aktif", true)))
                    .createAlias("asesor.asesorPenunjangKinerjaDosen", "asesorPenunjangKinerjaDosen")
                    .createAlias("asesor.tbmuser", "tbmuser")
                    .add(Restrictions.or(Restrictions.eq("tbmuser.pegawai", pegawai),
                            Restrictions.eq("tbmuser.dosen", dosen)))
                    .add(Restrictions.eq("asesorPenunjangKinerjaDosen.aktif", Boolean.TRUE)).list();
            if (asesmen != null) hasil.addAll(asesmen);
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "NewUiLkpTahunanController.lingkupAsesor");
        }
        hasil.add(pegawai);
        return hasil;
    }

    /** Jumlah capaian seluruh catatan realisasi milik satu target. */
    private static JSONObject rekapRealisasi(Session session, TargetKerjaPegawai target) throws Exception {
        JSONObject o = new JSONObject();
        Object[] hasil = null;
        try {
            hasil = (Object[]) session.createCriteria(RealisasiKerjaPegawai.class)
                    .add(Restrictions.eq("targetKerjaPegawai", target))
                    .setProjection(Projections.projectionList()
                            .add(Projections.rowCount())
                            .add(Projections.sum("kuantitas"))
                            .add(Projections.sum("waktu"))
                            .add(Projections.sum("biaya")))
                    .uniqueResult();
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "NewUiLkpTahunanController.rekapRealisasi");
        }
        o.put("jumlahCatatan", hasil == null ? 0 : angkaPanjang(hasil[0]));
        o.put("kuantitas", hasil == null ? 0 : pecahan(hasil[1]));
        o.put("waktu", hasil == null ? 0 : pecahan(hasil[2]));
        o.put("biaya", hasil == null ? 0 : pecahan(hasil[3]));
        double target1 = nilai(target.getKuantitas());
        // Persentase dihitung server supaya klien tidak menebak pembaginya, dan
        // target nol dilaporkan sebagai tanpa persentase alih-alih bagi nol.
        o.put("persenKuantitas", target1 > 0 ? (pecahan(hasil == null ? null : hasil[1]) * 100.0 / target1)
                : JSONObject.NULL);
        return o;
    }

    // ---------------------------------------------------------------- detail

    /** Catatan realisasi satu target, terbaru lebih dulu. */
    @SuppressWarnings("unchecked")
    private static void detail(JSONObject j, HttpServletRequest r) throws Exception {
        Session session = HibernateUtil.currentSession();
        Long id = idWajib(r.getParameter("id"));
        TargetKerjaPegawai target = (TargetKerjaPegawai) session.get(TargetKerjaPegawai.class, id);
        if (target == null) throw new IllegalArgumentException("Target kerja tidak ditemukan.");

        JSONArray rows = new JSONArray();
        List<Object> daftar = session.createCriteria(RealisasiKerjaPegawai.class)
                .add(Restrictions.eq("targetKerjaPegawai", target))
                .addOrder(Order.desc("id")).setMaxResults(BATAS_BARIS).list();
        for (Object o : daftar) {
            RealisasiKerjaPegawai x = (RealisasiKerjaPegawai) o;
            rows.put(new JSONObject().put("id", x.getId())
                    .put("kuantitas", nilai(x.getKuantitas()))
                    .put("waktu", nilai(x.getWaktu()))
                    .put("biaya", nilai(x.getBiaya()))
                    .put("mulai", tanggal(x.getTanggalWaktu()))
                    .put("sampai", tanggal(x.getTanggalWaktuSampai()))
                    .put("keterangan", teks(x.getKeterangan()))
                    .put("catatan", teks(x.getCatatan()))
                    .put("terverifikasi", Boolean.TRUE.equals(x.getVerifikasi())));
        }
        j.put("kegiatan", target.getKegiatanTugasJabatan() == null ? "-"
                : teks(target.getKegiatanTugasJabatan().getNama()));
        j.put("pegawai", target.getPegawai() == null ? "-" : teks(target.getPegawai().getNama()));
        j.put("rows", rows);
        j.put("jumlah", rows.length());
    }

    // ------------------------------------------------------------------ util

    private static Pegawai pegawaiParam(Session session, String nilai) {
        String v = text(nilai, "").trim();
        if (v.length() == 0) return null;
        try {
            long id = Long.parseLong(v);
            if (id <= 0) return null;
            return (Pegawai) session.get(Pegawai.class, Long.valueOf(id));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Pegawai tidak sah.");
        }
    }

    private static int angka(String nilai, int bawaan) {
        String v = text(nilai, "").trim();
        if (v.length() == 0) return bawaan;
        try {
            int t = Integer.parseInt(v);
            if (t < 2000 || t > 2999) throw new NumberFormatException();
            return t;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Tahun tidak sah.");
        }
    }

    private static Long idWajib(String nilai) {
        try {
            long l = Long.parseLong(text(nilai, "").trim());
            if (l <= 0) throw new NumberFormatException();
            return Long.valueOf(l);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Target kerja belum dipilih.");
        }
    }

    private static double nilai(Double d) {
        return d == null ? 0.0 : d.doubleValue();
    }

    private static double pecahan(Object o) {
        return o instanceof Number ? ((Number) o).doubleValue() : 0.0;
    }

    private static long angkaPanjang(Object o) {
        return o instanceof Number ? ((Number) o).longValue() : 0L;
    }

    private static String tanggal(java.util.Date d) {
        return d == null ? "" : new java.text.SimpleDateFormat("dd-MM-yyyy").format(d);
    }

    private static String teks(String s) {
        return s == null ? "" : s;
    }

    private static String text(String value, String fallback) {
        return value == null || value.trim().length() == 0 ? fallback : value;
    }

    private static void fail(JSONObject json, String code, String message) {
        try {
            json.put("ok", false);
            json.put("code", code);
            json.put("message", message == null ? "" : message);
        } catch (Exception ignored) { }
    }

    private static void write(HttpServletResponse response, JSONObject json) throws Exception {
        response.getWriter().write(json.toString());
    }
}
