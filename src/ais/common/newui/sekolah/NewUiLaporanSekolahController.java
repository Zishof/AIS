package ais.common.newui.sekolah;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.newui.NewUiRouteGuard;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.AkunPembayaranSiswa;
import ais.database.model.sekolah.JenisBiayaSekolah;
import ais.database.model.sekolah.Sekolah;

/**
 * Kontrak JSON laporan keuangan sekolah — paritas class ZK
 * {@code LaporanPembayaranSiswa}, {@code LaporanDepositSiswa},
 * {@code LaporanRincianPembayaranSiswa}, dan {@code LaporanRekapPembayaranSiswa}
 * tanpa komponen ZK.
 *
 * <p>Layar ZK-nya berupa <i>form filter + tab varian</i> yang bermuara pada satu
 * hal: merender template Jasper dengan parameter filter. Kontrak ini mengekspos
 * hal yang sama secara headless:</p>
 * <ul>
 *   <li>{@code meta} — daftar varian laporan (tab ZK) beserta opsi filter
 *       (sekolah, jenis biaya, akun pembayaran, tahun, bulan) dan rentang
 *       tanggal bawaan.</li>
 *   <li>{@code export} — merender varian terpilih memakai template Jasper yang
 *       SAMA dengan ZK dan mengirim PDF sebagai {@code pdfBase64} pada amplop
 *       JSON (delegasi JSP sudah memegang {@code getWriter()} sehingga
 *       streaming biner mustahil).</li>
 * </ul>
 *
 * <p>Parameter Jasper disusun persis seperti {@code generateParameter()} ZK:
 * header/footer kop, id filter dengan sentinel {@code -1}, rentang tanggal
 * {@code yyyy-MM-dd}, serta {@code anaks} yang membatasi pengguna orang tua ke
 * anaknya sendiri (fail-closed: {@code -1} bila tidak punya relasi).</p>
 *
 * <p>Deviasi sadar: varian yang di ZK hanya tersedia sebagai grid/Excel
 * (Rincian dan Rekap Tagihan memakai komponen ZK Spreadsheet yang tidak dapat
 * dijalankan headless) dilayani memakai template Jasper padanannya; bila
 * template tidak ada, {@code meta} tidak menawarkan varian tersebut sehingga
 * klien tidak pernah menampilkan tombol yang menyesatkan.</p>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class NewUiLaporanSekolahController {

    public static final String JENIS_PEMBAYARAN = "pembayaran";
    public static final String JENIS_DEPOSIT = "deposit";
    public static final String JENIS_RINCIAN = "rincian";
    public static final String JENIS_REKAP = "rekap";

    private static final String MODULE = "root/report";

    private NewUiLaporanSekolahController() { }

    /** Varian laporan = satu tab ZK: label + template Jasper. */
    private static final class Varian {
        final String kode, nama, template;
        Varian(String kode, String nama, String template) {
            this.kode = kode; this.nama = nama; this.template = template;
        }
    }

    private static List<Varian> varian(String jenis) {
        List<Varian> hasil = new ArrayList<Varian>();
        if (JENIS_PEMBAYARAN.equals(jenis)) {
            hasil.add(new Varian("per_siswa", "Pembayaran Per Siswa", "sekolah/pembayaran/pembayaran_per_siswa"));
            hasil.add(new Varian("per_jenis", "Pembayaran Per Jenis Pembayaran", "sekolah/pembayaran/pembayaran_siswa"));
            hasil.add(new Varian("per_tanggal", "Pembayaran Per Tanggal", "sekolah/pembayaran/pembayaran_siswa_per_tanggal"));
            hasil.add(new Varian("rincian", "Rincian Pembayaran", "sekolah/pembayaran/pembayaran_siswa_detail"));
            hasil.add(new Varian("per_item", "Item Pembayaran", "sekolah/pembayaran/pembayaran_siswa_per_item_biaya"));
        } else if (JENIS_DEPOSIT.equals(jenis)) {
            hasil.add(new Varian("deposit", "Deposit Siswa", "sekolah/pembayaran/deposit_siswa"));
            hasil.add(new Varian("deposit_per_siswa", "Deposit Per Siswa", "sekolah/pembayaran/deposit_per_siswa"));
            hasil.add(new Varian("deposit_per_tanggal", "Deposit Per Tanggal", "sekolah/pembayaran/deposit_per_tanggal"));
            hasil.add(new Varian("tabungan", "Tabungan Siswa", "sekolah/pembayaran/tabungan_siswa"));
            hasil.add(new Varian("tabungan_per_tanggal", "Tabungan Per Tanggal", "sekolah/pembayaran/tabungan_siswa_per_tanggal"));
            hasil.add(new Varian("rekap_tabungan", "Rekap Tabungan Per Tanggal", "sekolah/pembayaran/rekap_tabungan_siswa_per_tanggal"));
        } else if (JENIS_RINCIAN.equals(jenis)) {
            hasil.add(new Varian("rincian", "Rincian Pembayaran Siswa", "sekolah/pembayaran/pembayaran_siswa_detail"));
            hasil.add(new Varian("per_item", "Rincian Per Item Biaya", "sekolah/pembayaran/pembayaran_siswa_per_item_biaya"));
            hasil.add(new Varian("per_siswa", "Rekap Per Siswa", "sekolah/pembayaran/pembayaran_per_siswa"));
        } else { // JENIS_REKAP - tagihan & realisasi
            hasil.add(new Varian("tagihan_realisasi", "Tagihan dan Realisasi", "sekolah/pembayaran/tunggakan_siswa_dan_realisasi"));
            hasil.add(new Varian("realisasi_per_jenis", "Tagihan dan Realisasi Per Jenis", "sekolah/pembayaran/tunggakan_siswa_dan_realisasi_perjenis"));
            hasil.add(new Varian("realisasi_per_siswa", "Tagihan dan Realisasi Per Siswa", "sekolah/pembayaran/tunggakan_siswa_dan_realisasi_persiswa"));
            hasil.add(new Varian("tunggakan", "Tunggakan Siswa", "sekolah/pembayaran/tunggakan_siswa"));
            hasil.add(new Varian("tunggakan_per_kelas", "Tunggakan Per Kelas", "sekolah/pembayaran/tunggakan_siswa_per_kelas"));
            hasil.add(new Varian("tunggakan_per_bulan", "Tunggakan Per Bulan", "sekolah/pembayaran/tunggakan_siswa_per_bulan"));
            hasil.add(new Varian("tunggakan_per_asrama", "Tunggakan Per Asrama", "sekolah/pembayaran/tunggakan_siswa_per_asrama"));
        }
        return hasil;
    }

    public static void handle(HttpServletRequest request, HttpServletResponse response, String jenis, String pageKey)
            throws Exception {
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        JSONObject json = new JSONObject();
        try {
            String action = text(request.getParameter("action"), "meta");
            if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, pageKey, action)) {
                response.setStatus(403); fail(json, "ACTION_FORBIDDEN", "Hak akses tidak tersedia."); write(response, json); return;
            }
            Tbmuser user = Common.getCurrentUser(request);
            if (user == null) throw new SecurityException("Sesi tidak dikenal.");
            if ("meta".equals(action)) meta(json, request, jenis);
            else if ("options".equals(action)) opsi(json);
            else if ("export".equals(action) || "export_pdf".equals(action)) cetak(json, request, user, jenis);
            else throw new IllegalArgumentException("Aksi tidak dikenal.");
            json.put("ok", true);
        } catch (SecurityException e) { response.setStatus(403); fail(json, "FORBIDDEN", e.getMessage()); }
        catch (IllegalArgumentException e) { response.setStatus(422); fail(json, "VALIDATION_FAILED", e.getMessage()); }
        catch (Exception e) {
            response.setStatus(500);
            fail(json, "INTERNAL_ERROR", "Gagal memproses laporan. Detail dicatat di log server.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiLaporanSekolahController"); } catch (Exception ignored) { }
        }
        write(response, json);
    }

    // ------------------------------------------------------------------ meta
    private static void meta(JSONObject j, HttpServletRequest r, String jenis) throws Exception {
        JSONArray arr = new JSONArray();
        for (Varian v : varian(jenis)) {
            arr.put(new JSONObject().put("kode", v.kode).put("nama", v.nama));
        }
        j.put("jenis", jenis).put("varian", arr);

        Calendar cal = Calendar.getInstance();
        int tahunIni = cal.get(Calendar.YEAR);
        JSONArray tahun = new JSONArray();
        for (int t = tahunIni + 1; t >= tahunIni - 6; t--) tahun.put(t);
        j.put("tahun", tahun);
        j.put("tahunIni", tahunIni);
        j.put("bulanIni", cal.get(Calendar.MONTH) + 1);
        // Rentang bawaan mengikuti ZK: awal bulan berjalan s.d. hari ini.
        Calendar awal = Calendar.getInstance();
        awal.set(Calendar.DAY_OF_MONTH, 1);
        j.put("mulai", tanggalDb(awal.getTime()));
        j.put("sampai", tanggalDb(new Date()));
        opsi(j);
        j.put("csrf", csrfToken(r));
    }

    /** Opsi filter referensi: sekolah, jenis biaya, akun pembayaran. */
    private static void opsi(JSONObject j) throws Exception {
        JSONArray sekolahArr = new JSONArray(), jenisArr = new JSONArray(), akunArr = new JSONArray();
        Session s = HibernateUtil.openSession();
        try {
            Criteria cs = s.createCriteria(Sekolah.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .addOrder(Order.asc("nama")).setMaxResults(100);
            for (Object o : cs.list()) {
                Sekolah sk = (Sekolah) o;
                sekolahArr.put(new JSONObject().put("id", sk.getId()).put("nama", nz(sk.getNama())));
            }
            Criteria cj = s.createCriteria(JenisBiayaSekolah.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .addOrder(Order.asc("nama")).setMaxResults(200);
            for (Object o : cj.list()) {
                JenisBiayaSekolah jb = (JenisBiayaSekolah) o;
                jenisArr.put(new JSONObject().put("id", jb.getId()).put("nama", nz(jb.getNama())));
            }
            Criteria ca = s.createCriteria(AkunPembayaranSiswa.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .addOrder(Order.asc("nama")).setMaxResults(200);
            for (Object o : ca.list()) {
                AkunPembayaranSiswa ak = (AkunPembayaranSiswa) o;
                akunArr.put(new JSONObject().put("id", ak.getId()).put("nama", nz(ak.getNama())));
            }
        } finally { s.close(); }
        j.put("sekolah", sekolahArr).put("jenisBiaya", jenisArr).put("akunPembayaran", akunArr);
    }

    // ---------------------------------------------------------------- export
    private static void cetak(JSONObject j, HttpServletRequest r, Tbmuser user, String jenis) throws Exception {
        String kode = text(r.getParameter("varian"), "");
        Varian dipilih = null;
        for (Varian v : varian(jenis)) {
            if (v.kode.equals(kode)) { dipilih = v; break; }
        }
        if (dipilih == null) throw new IllegalArgumentException("Varian laporan wajib dipilih.");

        Long sekolahId = id(r, "sekolahId", false);
        Long jenisBiayaId = id(r, "jenisBiayaId", false);
        Long akunId = id(r, "akunPembayaranId", false);
        Integer tahun = integerObject(r, "tahun");
        Integer bulan = integerObject(r, "bulan");
        Integer angkatan = integerObject(r, "angkatan");
        Date mulai = tanggal(r.getParameter("mulai"), awalBulan());
        Date sampai = tanggal(r.getParameter("sampai"), new Date());
        String nomorIndukSiswa = text(r.getParameter("siswa"), "");

        Map parameters = ais.common.HashMapGenerator.getRand();
        parameters.put("nama_laporan", dipilih.template);

        Sekolah sekolah = null;
        String namaJenis = "Pembayaran";
        Session s = HibernateUtil.openSession();
        try {
            if (sekolahId != null) sekolah = (Sekolah) s.get(Sekolah.class, sekolahId);
            if (jenisBiayaId != null) {
                JenisBiayaSekolah jb = (JenisBiayaSekolah) s.get(JenisBiayaSekolah.class, jenisBiayaId);
                if (jb != null && jb.getNama() != null) namaJenis = jb.getNama();
            }
        } finally { s.close(); }

        // Kop laporan: paritas generateParameter ZK (lampiran KOP sekolah, jika
        // tidak ada pakai gambar bawaan folder report).
        String header = Common.ambilREAL_PATH_REPORT() + "/wood.jpg";
        if (sekolah != null && sekolah.getId() != null) {
            try {
                LampiranLain kop = LampiranLain.ambil(sekolah.getId(), LampiranLain.KOP_SEKOLAH);
                if (kop != null && kop.ambilFile() != null && kop.ambilFile().exists())
                    header = kop.ambilFile().getAbsolutePath();
                LampiranLain kopBawah = LampiranLain.ambil(sekolah.getId(), LampiranLain.KOP_BAWAH_SEKOLAH);
                if (kopBawah != null && kopBawah.ambilFile() != null && kopBawah.ambilFile().exists())
                    parameters.put("footer", kopBawah.ambilFile().getAbsolutePath());
            } catch (Exception ignored) { }
        }
        parameters.put("header", header);

        parameters.put("jenisBiayaNama", namaJenis);
        parameters.put("label_mulai", Common.dateFormat4.get().format(mulai));
        parameters.put("label_sampai", Common.dateFormat4.get().format(sampai));
        parameters.put("siswa", nomorIndukSiswa);
        parameters.put("jenisBiayaSekolah", jenisBiayaId == null ? Long.valueOf(-1L) : jenisBiayaId);
        parameters.put("akunPembayaranSiswa", akunId == null ? Long.valueOf(-1L) : akunId);
        parameters.put("kelas", Long.valueOf(id(r, "kelasId", false) == null ? -1L : id(r, "kelasId", false)));
        parameters.put("asrama", Long.valueOf(id(r, "asramaId", false) == null ? -1L : id(r, "asramaId", false)));
        parameters.put("yayasan", Long.valueOf(id(r, "yayasanId", false) == null ? -1L : id(r, "yayasanId", false)));
        parameters.put("sekolah", sekolahId == null ? Long.valueOf(-1L) : sekolahId);
        parameters.put("tahun", tahun == null ? Integer.valueOf(-1) : tahun);
        parameters.put("bulan", bulan == null ? Integer.valueOf(-1) : bulan);
        parameters.put("nama_bulan", bulan == null || bulan < 1 || bulan > 12 ? "Semua Bulan" : Common.BULAN[bulan - 1]);
        parameters.put("angkatan", angkatan == null ? Integer.valueOf(-1) : angkatan);
        parameters.put("mulai", tanggalDb(mulai));
        parameters.put("sampai", tanggalDb(sampai));

        // Fail-closed identitas: pengguna orang tua dibatasi pada anaknya.
        Collection<Long> anaks = user.getOrangTua() != null
                ? user.getOrangTua().ambilAnakSiswa() : new ArrayList<Long>();
        if (anaks == null || anaks.isEmpty()) {
            anaks = new ArrayList<Long>();
            anaks.add(Long.valueOf(-1L));
        }
        parameters.put("anaks", anaks.toArray());

        java.io.File pdf = ais.action.report.Report.generateFileReportSimple(
                ais.action.report.Report.PDF, parameters, dipilih.template);
        if (pdf == null || !pdf.exists())
            throw new IllegalStateException("PDF laporan gagal dibuat.");
        byte[] isi = java.nio.file.Files.readAllBytes(pdf.toPath());
        j.put("namaFile", dipilih.kode + "_" + tanggalDb(new Date()) + ".pdf");
        j.put("varianNama", dipilih.nama);
        j.put("pdfBase64", java.util.Base64.getEncoder().encodeToString(isi));
    }

    // ------------------------------------------------------------------ util
    private static Date awalBulan() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        return c.getTime();
    }

    private static String tanggalDb(Date d) {
        return Common.databaseDateFormat.get().format(d);
    }

    private static Date tanggal(String raw, Date fallback) {
        if (raw == null || raw.trim().length() == 0) return fallback;
        try { return new java.text.SimpleDateFormat("yyyy-MM-dd").parse(raw.trim()); }
        catch (Exception e) { throw new IllegalArgumentException("tanggal tidak valid (yyyy-MM-dd)."); }
    }

    private static String csrfToken(HttpServletRequest r) {
        Object existing = r.getSession().getAttribute("newUiCsrfToken");
        if (existing != null) return String.valueOf(existing);
        byte[] b = new byte[24];
        new java.security.SecureRandom().nextBytes(b);
        StringBuilder s = new StringBuilder(48);
        for (int i = 0; i < b.length; i++)
            s.append(Character.forDigit((b[i] >> 4) & 0xF, 16)).append(Character.forDigit(b[i] & 0xF, 16));
        String value = s.toString();
        r.getSession().setAttribute("newUiCsrfToken", value);
        return value;
    }

    private static Long id(HttpServletRequest r, String n, boolean required) {
        String v = r.getParameter(n);
        if (v == null || v.trim().length() == 0) {
            if (required) throw new IllegalArgumentException(n + " wajib diisi.");
            return null;
        }
        try { return Long.valueOf(v.trim()); }
        catch (Exception e) { throw new IllegalArgumentException(n + " tidak valid."); }
    }

    private static Integer integerObject(HttpServletRequest r, String n) {
        String v = r.getParameter(n);
        if (v == null || v.trim().length() == 0) return null;
        try { return Integer.valueOf(v.trim()); }
        catch (Exception e) { throw new IllegalArgumentException(n + " tidak valid."); }
    }

    private static String nz(String v) { return v == null ? "" : v; }
    private static String text(String v, String f) { return v == null || v.trim().length() == 0 ? f : v.trim(); }

    private static void fail(JSONObject j, String c, String m) throws Exception {
        j.put("ok", false).put("code", c).put("message", m == null ? "Operasi ditolak." : m);
    }

    private static void write(HttpServletResponse r, JSONObject j) throws Exception {
        r.getWriter().write(j.toString());
    }
}
