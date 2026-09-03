package ais.common.newui.koperasi;

import java.util.Calendar;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.koperasi.PengaturanKantinAction;
import ais.action.master.koperasi.ProsesPoinBulananHelper;
import ais.action.master.koperasi.util.PembagianShuHelper;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.newui.NewUiCsrfUtil;
import ais.common.newui.NewUiRouteGuard;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.koperasi.PembagianShu;
import ais.database.model.koperasi.ShuAnggota;
import ais.database.model.koperasi.TransaksiKoperasi;
import ais.database.model.rab.SatuanKerja;

/**
 * Kontrak native empat layar pengurus koperasi.
 *
 * <p>Keempatnya berbeda watak, sehingga batas yang diambil pun berbeda dan
 * disebutkan terang-terangan di sini:</p>
 *
 * <ul>
 *   <li><b>Pengaturan Kantin</b> tidak memiliki data sendiri. Layar ZK-nya
 *       hanyalah wadah tab yang memuat sebelas layar master lain. Kontrak ini
 *       karena itu hanya mengumumkan daftar tabnya — diambil dari konstanta
 *       yang sama yang dipakai layar ZK — dan klien membuka tiap tab sebagai
 *       halaman native yang sudah ada.</li>
 *   <li><b>Proses Poin Bulanan</b> adalah pekerjaan bertahap yang aman diulang:
 *       voucher yang sudah pernah terbit untuk periode yang sama tidak
 *       diterbitkan dua kali. Sifat itu milik
 *       {@link ProsesPoinBulananHelper}, bukan milik layar, sehingga kontrak
 *       ini cukup meneruskan periodenya.</li>
 *   <li><b>Pembagian SHU</b> menghitung dan menyimpan lewat
 *       {@link PembagianShuHelper} — rumus yang sama dengan layar ZK.</li>
 *   <li><b>Persetujuan Transaksi Koperasi</b> disajikan <b>baca saja</b>.
 *       Lihat catatan di bawah.</li>
 * </ul>
 *
 * <h3>Mengapa persetujuan tidak dapat disetujui dari sini</h3>
 * <p>Menyetujui transaksi koperasi pada layar ZK bukan sekadar menandai satu
 * kolom: persetujuan membangkitkan pengajuan transfer
 * ({@code DaftarPengajuanTransfer.simpanTransaksiKoperasi}), menyentuh
 * disposisi SOP, dan memicu pencetakan. Kontrak yang hanya membalik penanda
 * status akan menghasilkan transaksi yang tampak disetujui namun tanpa
 * pengajuan transfer — lebih berbahaya daripada tidak ada sama sekali. Sampai
 * seluruh rangkaian itu tersedia secara native, persetujuan tetap dilakukan di
 * layar ZK dan kontrak ini hanya menyajikan daftar serta rinciannya.</p>
 *
 * <p>Fail-closed: mode tak dikenal ditolak, sesi tanpa pengguna ditolak,
 * mutasi wajib POST beserta token CSRF.</p>
 */
public final class NewUiKoperasiOperasiController {

    /** Harus sama dengan awalan folder JSP sebelum {@code /uiux/}. */
    private static final String MODULE = "koperasi";

    /** Pengaturan Kantin (wadah tab). */
    public static final String MODE_PENGATURAN = "pengaturan";
    /** Proses Poin Bulanan. */
    public static final String MODE_POIN = "poin";
    /** Pembagian SHU. */
    public static final String MODE_SHU = "shu";
    /** Persetujuan Transaksi Koperasi (baca saja). */
    public static final String MODE_PERSETUJUAN = "persetujuan";

    private static final String[] NAMA_BULAN = {
            "Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September",
            "Oktober", "November", "Desember"
    };

    private static final int BATAS_BARIS = 300;

    private NewUiKoperasiOperasiController() { }

    public static void handle(HttpServletRequest request, HttpServletResponse response,
            String mode, String pageKey) throws Exception {
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        JSONObject json = new JSONObject();
        try {
            if (!modeDikenal(mode)) throw new IllegalArgumentException("Mode koperasi tidak dikenal.");
            String action = text(request.getParameter("action"), "meta");
            if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, pageKey, action)) {
                response.setStatus(403);
                fail(json, "ACTION_FORBIDDEN", "Hak akses tidak tersedia.");
                write(response, json);
                return;
            }
            Tbmuser user = Common.getCurrentUser(request);
            if (user == null) throw new SecurityException("Sesi tidak dikenal.");

            if (MODE_PENGATURAN.equals(mode)) pengaturan(json, action);
            else if (MODE_POIN.equals(mode)) poin(json, request, user, action);
            else if (MODE_SHU.equals(mode)) shu(json, request, action);
            else persetujuan(json, request, action);
            json.put("ok", true);
        } catch (SecurityException e) {
            response.setStatus(403);
            fail(json, "FORBIDDEN", e.getMessage());
        } catch (IllegalArgumentException e) {
            response.setStatus(422);
            fail(json, "VALIDATION_FAILED", e.getMessage());
        } catch (Exception e) {
            response.setStatus(500);
            fail(json, "INTERNAL_ERROR", "Gagal memproses permintaan koperasi. Detail dicatat di log server.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiKoperasiOperasiController"); } catch (Exception ignored) { }
        }
        write(response, json);
    }

    public static boolean modeDikenal(String mode) {
        return MODE_PENGATURAN.equals(mode) || MODE_POIN.equals(mode)
                || MODE_SHU.equals(mode) || MODE_PERSETUJUAN.equals(mode);
    }

    // =================================================================== pengaturan

    private static void pengaturan(JSONObject j, String action) throws Exception {
        if (!"meta".equals(action)) throw new IllegalArgumentException("Aksi tidak dikenal.");
        j.put("judul", "Pengaturan Kantin");
        JSONArray tab = new JSONArray();
        for (int i = 0; i < PengaturanKantinAction.TABS.length; i++) {
            tab.put(new JSONObject()
                    .put("label", PengaturanKantinAction.TABS[i][0])
                    .put("route", PengaturanKantinAction.TABS[i][1]));
        }
        j.put("tab", tab);
    }

    // =================================================================== poin bulanan

    private static void poin(JSONObject j, HttpServletRequest request, Tbmuser user, String action) throws Exception {
        if ("meta".equals(action)) {
            j.put("judul", "Proses Poin Bulanan");
            j.put("csrfHeader", NewUiCsrfUtil.HEADER);
            j.put("csrfToken", NewUiCsrfUtil.getToken(request.getSession(true)));
            Calendar kal = ais.ui.util.WaktuUtil.getCalendar();
            JSONArray bulan = new JSONArray();
            for (int i = 0; i < NAMA_BULAN.length; i++) {
                bulan.put(new JSONObject().put("nilai", i + 1).put("nama", NAMA_BULAN[i]));
            }
            j.put("pilihanBulan", bulan);
            j.put("bulanBawaan", kal.get(Calendar.MONTH) + 1);
            j.put("tahunBawaan", kal.get(Calendar.YEAR));
            // Dinyatakan eksplisit agar klien berani menampilkan tombolnya tanpa
            // peringatan menakutkan: mengulang proses tidak menerbitkan dobel.
            j.put("amanDiulang", true);
            return;
        }
        if ("create".equals(action)) {
            wajibMutasi(request);
            int bulan = angka(request.getParameter("bulan"), 1, 12, "Bulan tidak sah.");
            int tahun = angka(request.getParameter("tahun"), 2000, 2999, "Tahun tidak sah.");
            String oleh = user.getUserId() == null ? "admin" : user.getUserId();
            ProsesPoinBulananHelper.HasilProsesPoin hasil =
                    ProsesPoinBulananHelper.prosesUntukBulan(tahun, bulan, oleh);
            j.put("pegawaiDiproses", hasil.pegawaiDiproses);
            j.put("voucherDiterbitkan", hasil.voucherDiterbitkan);
            j.put("totalNominal", hasil.totalNominal);
            j.put("dilewatiSudahAda", hasil.dilewatiSudahAda);
            j.put("periode", NAMA_BULAN[bulan - 1] + " " + tahun);
            j.put("pesan", "Selesai. Pegawai diproses: " + hasil.pegawaiDiproses
                    + ", voucher diterbitkan: " + hasil.voucherDiterbitkan
                    + ", dilewati karena sudah pernah diterbitkan: " + hasil.dilewatiSudahAda + ".");
            return;
        }
        throw new IllegalArgumentException("Aksi tidak dikenal.");
    }

    // =================================================================== pembagian SHU

    private static void shu(JSONObject j, HttpServletRequest request, String action) throws Exception {
        Session session = HibernateUtil.currentSession();
        if ("meta".equals(action)) {
            j.put("judul", "Pembagian SHU");
            j.put("csrfHeader", NewUiCsrfUtil.HEADER);
            j.put("csrfToken", NewUiCsrfUtil.getToken(request.getSession(true)));
            int thn = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
            j.put("tahunBawaan", thn);
            JSONArray tahun = new JSONArray();
            for (int t = thn + 1; t >= thn - 8; t--) {
                tahun.put(t);
            }
            j.put("pilihanTahun", tahun);
            j.put("prefill", prefill(session, tahunParam(request, thn)));
            return;
        }
        if ("list".equals(action)) {
            int thn = tahunParam(request, ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));
            PembagianShu p = PembagianShuHelper.cari(session, thn);
            j.put("tahun", thn);
            if (p == null) {
                j.put("ada", false);
                j.put("rows", new JSONArray());
                return;
            }
            j.put("ada", true);
            j.put("ringkasan", ringkasan(p));
            JSONArray out = new JSONArray();
            double dibagikan = 0;
            for (ShuAnggota s : PembagianShuHelper.rincian(session, p)) {
                String nama = s.getAnggota() == null || s.getAnggota().getNama() == null
                        ? "-" : s.getAnggota().getNama();
                dibagikan += s.getTotalShu();
                out.put(new JSONObject().put("anggota", nama)
                        .put("totalSimpanan", s.getTotalSimpanan())
                        .put("partisipasi", s.getTotalTransaksi())
                        .put("jasaModal", s.getJasaModal())
                        .put("jasaUsaha", s.getJasaUsaha())
                        .put("totalShu", s.getTotalShu())
                        .put("dibayar", s.getSudahDibayar() != null && s.getSudahDibayar().booleanValue()));
            }
            j.put("rows", out);
            j.put("totalDibagikan", dibagikan);
            j.put("jumlahPenerima", out.length());
            return;
        }
        if ("save".equals(action)) {
            wajibMutasi(request);
            int thn = tahunParam(request, ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));
            PembagianShuHelper.Parameter p = new PembagianShuHelper.Parameter();
            p.totalShu = pecahan(request.getParameter("totalShu"));
            if (p.totalShu <= 0) throw new IllegalArgumentException("Total SHU harus lebih dari nol.");
            p.persenCadangan = persen(request.getParameter("persenCadangan"));
            p.persenJasaModal = persen(request.getParameter("persenJasaModal"));
            p.persenJasaUsaha = persen(request.getParameter("persenJasaUsaha"));
            p.persenPendidikan = persen(request.getParameter("persenPendidikan"));
            p.persenPengurus = persen(request.getParameter("persenPengurus"));
            p.persenSosial = persen(request.getParameter("persenSosial"));
            double jumlahPersen = p.persenCadangan + p.persenJasaModal + p.persenJasaUsaha
                    + p.persenPendidikan + p.persenPengurus + p.persenSosial;
            // Layar ZK tidak memaksa jumlahnya 100%, jadi kontrak ini pun tidak
            // menolak — tetapi angkanya dikembalikan agar klien dapat
            // memperingatkan pengurus bila alokasinya tidak genap.
            if (jumlahPersen > 100.0001) {
                throw new IllegalArgumentException("Jumlah persentase alokasi melebihi 100%.");
            }
            PembagianShu hasil = PembagianShuHelper.hitungDanSimpan(session, thn, p);
            j.put("tahun", thn);
            j.put("jumlahPersen", jumlahPersen);
            j.put("ringkasan", ringkasan(hasil));
            j.put("pesan", "Pembagian SHU tahun " + thn + " berhasil dihitung dan disimpan.");
            return;
        }
        throw new IllegalArgumentException("Aksi tidak dikenal.");
    }

    /** Nilai formulir SHU tahun terpilih; bawaan alokasi lazim bila belum pernah disimpan. */
    private static JSONObject prefill(Session session, int tahun) throws Exception {
        PembagianShu p = PembagianShuHelper.cari(session, tahun);
        JSONObject o = new JSONObject();
        o.put("tahun", tahun);
        if (p == null) {
            // Sama dengan bawaan layar ZK; pengurus tetap dapat mengubahnya.
            o.put("tersimpan", false);
            o.put("totalShu", 0);
            o.put("persenCadangan", 25.0);
            o.put("persenJasaModal", 25.0);
            o.put("persenJasaUsaha", 30.0);
            o.put("persenPendidikan", 10.0);
            o.put("persenPengurus", 5.0);
            o.put("persenSosial", 5.0);
            return o;
        }
        o.put("tersimpan", true);
        o.put("totalShu", p.getTotalShu());
        o.put("persenCadangan", p.getPersenCadangan());
        o.put("persenJasaModal", p.getPersenJasaModal());
        o.put("persenJasaUsaha", p.getPersenJasaUsaha());
        o.put("persenPendidikan", p.getPersenPendidikan());
        o.put("persenPengurus", p.getPersenPengurus());
        o.put("persenSosial", p.getPersenSosial());
        return o;
    }

    /** Komposisi alokasi SHU; nominalnya dihitung server agar klien tidak menebak rumus. */
    private static JSONObject ringkasan(PembagianShu p) throws Exception {
        double t = p.getTotalShu();
        return new JSONObject()
                .put("tahun", p.getTahun())
                .put("totalShu", t)
                .put("danaCadangan", t * p.getPersenCadangan() / 100.0)
                .put("jasaModal", p.getNominalJasaModal())
                .put("jasaUsaha", p.getNominalJasaUsaha())
                .put("pendidikan", t * p.getPersenPendidikan() / 100.0)
                .put("insentifPengurus", t * p.getPersenPengurus() / 100.0)
                .put("danaSosial", t * p.getPersenSosial() / 100.0);
    }

    // =================================================================== persetujuan

    private static void persetujuan(JSONObject j, HttpServletRequest request, String action) throws Exception {
        Session session = HibernateUtil.currentSession();
        if ("meta".equals(action)) {
            j.put("judul", "Persetujuan Transaksi Koperasi");
            // Klien TIDAK boleh menampilkan tombol setujui/tolak: rangkaian
            // persetujuan (pengajuan transfer, disposisi SOP, cetak) belum
            // tersedia native. Lihat catatan kelas.
            j.put("bolehUbah", false);
            j.put("alasanBacaSaja", "Persetujuan membangkitkan pengajuan transfer dan disposisi SOP "
                    + "yang belum tersedia secara native; gunakan layar lama untuk menyetujui.");
            JSONArray status = new JSONArray();
            status.put(TransaksiKoperasi.PENGAJUAN);
            status.put(TransaksiKoperasi.DISETUJU);
            status.put(TransaksiKoperasi.DITOLAK);
            j.put("pilihanStatus", status);
            j.put("statusBawaan", TransaksiKoperasi.PENGAJUAN);
            return;
        }
        if ("list".equals(action)) {
            String status = text(request.getParameter("status"), TransaksiKoperasi.PENGAJUAN);
            if (!TransaksiKoperasi.PENGAJUAN.equals(status) && !TransaksiKoperasi.DISETUJU.equals(status)
                    && !TransaksiKoperasi.DITOLAK.equals(status)) {
                throw new IllegalArgumentException("Status tidak dikenal.");
            }
            // SENGAJA lewat HQL, bukan SQL mentah. Pada entity ini `status` dan
            // `margin` adalah nilai TURUNAN: status menjadi "Disetujui" begitu
            // kolom persetujuan terisi, dan margin dihitung dari bunga serta
            // jangka waktu produknya. Membaca kolom apa adanya lewat SQL akan
            // menampilkan angka dan status yang berbeda dari layar ZK.
            StringBuilder hql = new StringBuilder(
                    "select distinct t from TransaksiKoperasi t "
                            + "left join fetch t.anggotaKoperasi a "
                            + "left join fetch t.produkKoperasi p "
                            + "left join fetch t.satuanKerja sk "
                            + "left join fetch t.disetujuiOleh d where ");
            if (TransaksiKoperasi.DISETUJU.equals(status)) {
                hql.append("t.disetujuiOleh is not null");
            } else if (TransaksiKoperasi.DITOLAK.equals(status)) {
                hql.append("t.disetujuiOleh is null and t.status = :ditolak");
            } else {
                hql.append("t.disetujuiOleh is null and (t.status is null or t.status <> :ditolak)");
            }
            // Dibatasi ke satuan kerja yang berhak dilihat pengguna login, sama
            // seperti initCriteria() pada TransaksiKoperasiAction (layar ZK
            // pembandingnya). Transaksi berkolom satuanKerja kosong tetap
            // ditampilkan, mengikuti pola yang sama pada
            // NewUiStandingInstructionService.
            Set<SatuanKerja> allowed = SekolahUtil.ambilSatuanKerjas();
            boolean batasi = allowed != null && !allowed.isEmpty();
            if (batasi) {
                hql.append(" and (sk is null or sk in (:allowed))");
            } else {
                hql.append(" and sk is null");
            }
            hql.append(" order by t.id desc");
            org.hibernate.Query q = session.createQuery(hql.toString());
            if (!TransaksiKoperasi.DISETUJU.equals(status)) {
                q.setParameter("ditolak", TransaksiKoperasi.DITOLAK);
            }
            if (batasi) {
                q.setParameterList("allowed", allowed);
            }
            q.setMaxResults(BATAS_BARIS);
            JSONArray out = new JSONArray();
            for (Object o : q.list()) {
                TransaksiKoperasi t = (TransaksiKoperasi) o;
                out.put(new JSONObject().put("id", t.getId())
                        .put("nama", teks(t.getNama()))
                        .put("anggota", t.getAnggotaKoperasi() == null ? "-" : teks(t.getAnggotaKoperasi().getNama()))
                        .put("produk", t.getProdukKoperasi() == null ? "-" : teks(t.getProdukKoperasi().getNama()))
                        .put("nilai", t.getNilai())
                        .put("margin", t.getMargin())
                        .put("total", t.getTotal())
                        // getTanggal() dipetakan ke kolom tanggal_pengajuan.
                        .put("tanggal", tanggalTeks(t.getTanggal()))
                        .put("tanggalPersetujuan", tanggalTeks(t.getTanggalPersetujuan()))
                        .put("status", t.getStatus()));
            }
            j.put("rows", out);
            j.put("status", status);
            j.put("terpotong", out.length() >= BATAS_BARIS);
            return;
        }
        throw new IllegalArgumentException("Aksi tidak dikenal.");
    }

    // =================================================================== util

    private static void wajibMutasi(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            throw new SecurityException("Mutasi hanya dilayani lewat POST.");
        }
        if (!NewUiCsrfUtil.isValid(request)) {
            throw new SecurityException("Token CSRF tidak sah. Muat ulang halaman.");
        }
    }

    private static int tahunParam(HttpServletRequest request, int bawaan) {
        String v = text(request.getParameter("tahun"), "").trim();
        if (v.length() == 0) return bawaan;
        return angka(v, 2000, 2999, "Tahun tidak sah.");
    }

    private static int angka(String nilai, int min, int max, String pesan) {
        try {
            int v = Integer.parseInt(text(nilai, "").trim());
            if (v < min || v > max) throw new NumberFormatException();
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(pesan);
        }
    }

    private static double pecahan(String nilai) {
        try {
            return Double.parseDouble(text(nilai, "0").trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Nilai bukan angka yang sah.");
        }
    }

    private static double persen(String nilai) {
        double v = pecahan(nilai);
        if (v < 0 || v > 100) throw new IllegalArgumentException("Persentase harus antara 0 dan 100.");
        return v;
    }

    private static String teks(String s) {
        return s == null || s.trim().length() == 0 ? "-" : s;
    }

    /** Tanggal untuk tampilan; kosong bila belum ada. */
    private static String tanggalTeks(java.util.Date d) {
        return d == null ? "" : new java.text.SimpleDateFormat("dd-MM-yyyy").format(d);
    }

    private static String text(String value, String fallback) {
        return value == null || value.trim().length() == 0 ? fallback : value;
    }

    private static double num(Object o) {
        return o == null ? 0.0 : ((Number) o).doubleValue();
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    private static Long lng(Object o) {
        return o == null ? null : Long.valueOf(((Number) o).longValue());
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
