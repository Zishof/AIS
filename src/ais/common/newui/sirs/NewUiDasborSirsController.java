package ais.common.newui.sirs;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.sirs.chart.helper.DiagnosaTerbanyakDashboardBuilder;
import ais.action.master.sirs.chart.helper.KadaluarsaFarmasiDashboardBuilder;
import ais.action.master.sirs.chart.helper.OkupansiTempatTidurDashboardBuilder;
import ais.action.master.sirs.chart.helper.PendaftaranOverviewDashboardBuilder;
import ais.action.master.sirs.chart.helper.PendapatanDashboardBuilder;
import ais.action.master.sirs.chart.helper.RawatJalanDashboardBuilder;
import ais.common.Common;
import ais.common.newui.NewUiRouteGuard;
import ais.database.model.Tbmuser;
import ais.database.model.sirs.Poly;

/**
 * Kontrak native tujuh dasbor SIRS.
 *
 * <p>Ketujuhnya — ringkasan pendaftaran, kunjungan rawat jalan mingguan dan
 * bulanan, pendapatan pasien, okupansi tempat tidur, sepuluh diagnosa
 * terbanyak, dan kewaspadaan kadaluarsa farmasi — adalah layar baca saja yang
 * hanya menampilkan angka. Tidak ada satu pun aksi yang menulis.</p>
 *
 * <h3>Angka, bukan HTML</h3>
 * <p>Layar ZK-nya merangkai grafik sebagai HTML/CSS lalu menyuntikkannya ke
 * satu wadah. Kontrak ini mengirim <b>angkanya saja</b> supaya klien menggambar
 * kartu dan grafiknya secara native — HTML kiriman server tidak akan pernah
 * tampil benar di Flutter, dan mengirimnya sama saja memindahkan
 * ketergantungan pada WebView.</p>
 *
 * <h3>Kueri tetap satu tempat</h3>
 * <p>Agregasi tidak ditulis ulang di sini. Tiap builder dasbor kini memisahkan
 * pengambilan data dari penyajiannya, dan controller ini memanggil bagian
 * datanya — {@code PendaftaranOverviewDashboardBuilder.data(tahun)} dan
 * seterusnya. Menyalin SQL-nya akan membuat angka pada layar lama dan layar
 * native menyimpang begitu salah satunya diperbaiki.</p>
 *
 * <h3>Local-first</h3>
 * <p>Seluruh aksi memakai nama {@code meta}, yang boleh dilayani salinan lokal
 * oleh klien. Ini disengaja: dasbor bersifat ringkasan historis, bukan
 * transaksi, sehingga menampilkan salinan sedikit lama jauh lebih berguna
 * daripada layar kosong saat jaringan putus. Yang tidak boleh di-cache adalah
 * saldo dan pembayaran — dan tidak satu pun ada di sini.</p>
 *
 * <p>Fail-closed: mode di luar daftar ditolak, sesi tanpa pengguna ditolak.</p>
 */
public final class NewUiDasborSirsController {

    /** Harus sama dengan awalan folder JSP sebelum {@code /uiux/}. */
    private static final String MODULE = "sirs";

    public static final String MODE_PENDAFTARAN = "pendaftaran";
    public static final String MODE_RAWAT_JALAN_MINGGUAN = "rawat_jalan_mingguan";
    public static final String MODE_RAWAT_JALAN_BULANAN = "rawat_jalan_bulanan";
    public static final String MODE_PENDAPATAN = "pendapatan";
    public static final String MODE_OKUPANSI = "okupansi";
    public static final String MODE_DIAGNOSA = "diagnosa";
    public static final String MODE_KADALUARSA = "kadaluarsa";

    private static final String[] NAMA_BULAN = { "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
            "Jul", "Agu", "Sep", "Okt", "Nov", "Des" };

    /** Sama dengan urutan bucket pada builder kadaluarsa. */
    private static final String[] LABEL_KADALUARSA = { "Sudah kadaluarsa", "Kurang dari 30 hari",
            "30–90 hari", "Lebih dari 90 hari" };

    private NewUiDasborSirsController() { }

    public static void handle(HttpServletRequest request, HttpServletResponse response,
            String mode, String pageKey) throws Exception {
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        JSONObject json = new JSONObject();
        try {
            if (!modeDikenal(mode)) throw new IllegalArgumentException("Mode dasbor tidak dikenal.");
            String action = text(request.getParameter("action"), "meta");
            if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, pageKey, action)) {
                response.setStatus(403);
                fail(json, "ACTION_FORBIDDEN", "Hak akses tidak tersedia.");
                write(response, json);
                return;
            }
            Tbmuser user = Common.getCurrentUser(request);
            if (user == null) throw new SecurityException("Sesi tidak dikenal.");
            if (!"meta".equals(action)) throw new IllegalArgumentException("Aksi tidak dikenal.");

            j(json, mode, request);
            json.put("ok", true);
        } catch (SecurityException e) {
            response.setStatus(403);
            fail(json, "FORBIDDEN", e.getMessage());
        } catch (IllegalArgumentException e) {
            response.setStatus(422);
            fail(json, "VALIDATION_FAILED", e.getMessage());
        } catch (Exception e) {
            response.setStatus(500);
            fail(json, "INTERNAL_ERROR", "Gagal menyusun dasbor. Detail dicatat di log server.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiDasborSirsController"); } catch (Exception ignored) { }
        }
        write(response, json);
    }

    public static boolean modeDikenal(String mode) {
        return MODE_PENDAFTARAN.equals(mode) || MODE_RAWAT_JALAN_MINGGUAN.equals(mode)
                || MODE_RAWAT_JALAN_BULANAN.equals(mode) || MODE_PENDAPATAN.equals(mode)
                || MODE_OKUPANSI.equals(mode) || MODE_DIAGNOSA.equals(mode)
                || MODE_KADALUARSA.equals(mode);
    }

    static String judul(String mode) {
        if (MODE_PENDAFTARAN.equals(mode)) return "Ringkasan Pendaftaran";
        if (MODE_RAWAT_JALAN_MINGGUAN.equals(mode)) return "Kunjungan Rawat Jalan (Mingguan)";
        if (MODE_RAWAT_JALAN_BULANAN.equals(mode)) return "Kunjungan Rawat Jalan (Bulanan)";
        if (MODE_PENDAPATAN.equals(mode)) return "Pendapatan Pasien";
        if (MODE_OKUPANSI.equals(mode)) return "Okupansi Tempat Tidur";
        if (MODE_DIAGNOSA.equals(mode)) return "10 Diagnosa Terbanyak";
        return "Kewaspadaan Kadaluarsa (Farmasi)";
    }

    /** true bila dasbor menerima saringan tahun. */
    static boolean pakaiTahun(String mode) {
        return !MODE_OKUPANSI.equals(mode) && !MODE_KADALUARSA.equals(mode);
    }

    /** true bila dasbor menerima saringan bulan. */
    static boolean pakaiBulan(String mode) {
        return MODE_RAWAT_JALAN_MINGGUAN.equals(mode);
    }

    // ------------------------------------------------------------------ isi

    private static void j(JSONObject j, String mode, HttpServletRequest request) throws Exception {
        Calendar kal = ais.ui.util.WaktuUtil.getCalendar();
        int tahun = tahunParam(request, kal.get(Calendar.YEAR));
        int bulan = bulanParam(request, kal.get(Calendar.MONTH) + 1);

        j.put("judul", judul(mode));
        j.put("mode", mode);
        j.put("pakaiTahun", pakaiTahun(mode));
        j.put("pakaiBulan", pakaiBulan(mode));
        if (pakaiTahun(mode)) {
            JSONArray tahunPilihan = new JSONArray();
            for (int t = kal.get(Calendar.YEAR); t >= kal.get(Calendar.YEAR) - 6; t--) {
                tahunPilihan.put(t);
            }
            j.put("pilihanTahun", tahunPilihan);
            j.put("tahun", tahun);
        }
        if (pakaiBulan(mode)) {
            JSONArray bulanPilihan = new JSONArray();
            for (int b = 1; b <= 12; b++) {
                bulanPilihan.put(new JSONObject().put("nilai", b).put("nama", NAMA_BULAN[b - 1]));
            }
            j.put("pilihanBulan", bulanPilihan);
            j.put("bulan", bulan);
        }

        if (MODE_PENDAFTARAN.equals(mode)) pendaftaran(j, tahun);
        else if (MODE_RAWAT_JALAN_MINGGUAN.equals(mode)) rawatJalan(j, RawatJalanDashboardBuilder.dataMingguan(tahun, bulan));
        else if (MODE_RAWAT_JALAN_BULANAN.equals(mode)) rawatJalan(j, RawatJalanDashboardBuilder.dataBulanan(tahun));
        else if (MODE_PENDAPATAN.equals(mode)) pendapatan(j, tahun);
        else if (MODE_OKUPANSI.equals(mode)) okupansi(j);
        else if (MODE_DIAGNOSA.equals(mode)) diagnosa(j, tahun);
        else kadaluarsa(j);
    }

    /** Ringkasan pendaftaran: jumlah per jenis kunjungan per bulan. */
    private static void pendaftaran(JSONObject j, int tahun) throws Exception {
        Map<String, double[]> perJenis = PendaftaranOverviewDashboardBuilder.data(tahun);
        JSONArray deret = new JSONArray();
        double[] totalPerBulan = new double[12];
        double grandTotal = 0;
        for (Map.Entry<String, double[]> e : perJenis.entrySet()) {
            double[] arr = e.getValue();
            double totalJenis = 0;
            JSONArray nilai = new JSONArray();
            for (int b = 0; b < 12; b++) {
                nilai.put(arr[b]);
                totalJenis += arr[b];
                totalPerBulan[b] += arr[b];
            }
            grandTotal += totalJenis;
            deret.put(new JSONObject().put("nama", e.getKey()).put("nilai", nilai)
                    .put("total", totalJenis));
        }
        j.put("bulan", bulanLabel());
        j.put("deret", deret);
        j.put("totalPerBulan", angkaArray(totalPerBulan));
        j.put("total", grandTotal);
        j.put("bulanTeramai", puncak(totalPerBulan));
    }

    /** Kunjungan rawat jalan: matriks periode × poli. */
    private static void rawatJalan(JSONObject j, RawatJalanDashboardBuilder.Data d) throws Exception {
        j.put("judulGrafik", d.judul);
        j.put("satuanPeriode", d.satuanPeriode);
        JSONArray poli = new JSONArray();
        for (Poly p : d.polies) {
            if (p == null) continue;
            poli.put(p.getNama() == null || p.getNama().trim().length() == 0
                    ? "(Tanpa Nama)" : p.getNama().trim());
        }
        j.put("poli", poli);

        JSONArray periode = new JSONArray();
        double total = 0;
        double[] totalPerPoli = new double[d.polies.size()];
        for (int i = 0; i < d.periodeLabels.size() && i < d.matriks.size(); i++) {
            double[] baris = d.matriks.get(i);
            JSONArray nilai = new JSONArray();
            double totalBaris = 0;
            for (int c = 0; c < baris.length; c++) {
                nilai.put(baris[c]);
                totalBaris += baris[c];
                totalPerPoli[c] += baris[c];
            }
            total += totalBaris;
            periode.put(new JSONObject().put("label", d.periodeLabels.get(i))
                    .put("nilai", nilai).put("total", totalBaris));
        }
        j.put("periode", periode);
        j.put("totalPerPoli", angkaArray(totalPerPoli));
        j.put("total", total);
    }

    /** Pendapatan pasien per bulan, dipilah tunai dan non-tunai. */
    private static void pendapatan(JSONObject j, int tahun) throws Exception {
        PendapatanDashboardBuilder.Data d = PendapatanDashboardBuilder.data(tahun);
        double total = 0, tunai = 0, nonTunai = 0;
        for (int b = 0; b < 12; b++) {
            total += d.totalBulan[b];
            tunai += d.tunaiBulan[b];
            nonTunai += d.nonTunaiBulan[b];
        }
        j.put("bulan", bulanLabel());
        j.put("totalPerBulan", angkaArray(d.totalBulan));
        j.put("tunaiPerBulan", angkaArray(d.tunaiBulan));
        j.put("nonTunaiPerBulan", angkaArray(d.nonTunaiBulan));
        j.put("total", total);
        j.put("totalTunai", tunai);
        j.put("totalNonTunai", nonTunai);
        j.put("bulanTertinggi", puncak(d.totalBulan));
    }

    /** Okupansi tempat tidur: hitungan menyeluruh, per status, dan per kelas. */
    private static void okupansi(JSONObject j) throws Exception {
        OkupansiTempatTidurDashboardBuilder.Data d = OkupansiTempatTidurDashboardBuilder.data();
        j.put("total", d.total);
        j.put("terisi", d.terisi);
        j.put("kosong", d.kosong);
        // Persentase dihitung server agar klien tidak perlu menebak pembaginya.
        j.put("persenTerisi", d.total > 0 ? (d.terisi * 100.0 / d.total) : 0.0);
        j.put("perStatus", petaAngka(d.perStatus));
        j.put("perKelasTotal", petaAngka(d.perKelasTotal));
        j.put("perKelasTerisi", petaAngka(d.perKelasTerisi));
    }

    /** Sepuluh diagnosa terbanyak beserta porsinya terhadap seluruh kasus. */
    private static void diagnosa(JSONObject j, int tahun) throws Exception {
        DiagnosaTerbanyakDashboardBuilder.Data d = DiagnosaTerbanyakDashboardBuilder.data(tahun);
        JSONArray rows = new JSONArray();
        for (Object[] r : d.baris) {
            if (r == null || r.length < 2) continue;
            double jml = r[1] instanceof Number ? ((Number) r[1]).doubleValue() : 0;
            rows.put(new JSONObject()
                    .put("diagnosa", r[0] == null ? "(Tanpa Nama)" : r[0].toString())
                    .put("jumlah", jml)
                    .put("porsi", d.totalSemua > 0 ? (jml * 100.0 / d.totalSemua) : 0.0));
        }
        j.put("rows", rows);
        j.put("totalSemua", d.totalSemua);
    }

    /** Kewaspadaan kadaluarsa: jumlah per rentang waktu dan sepuluh item terdekat. */
    private static void kadaluarsa(JSONObject j) throws Exception {
        KadaluarsaFarmasiDashboardBuilder.Data d = KadaluarsaFarmasiDashboardBuilder.data();
        double[] bucket = new double[4];
        for (Object[] r : d.bucket) {
            if (r == null || r.length < 2) continue;
            int idx = r[0] instanceof Number ? ((Number) r[0]).intValue() : -1;
            if (idx < 0 || idx > 3) continue;
            bucket[idx] = r[1] instanceof Number ? ((Number) r[1]).doubleValue() : 0;
        }
        JSONArray rentang = new JSONArray();
        for (int i = 0; i < 4; i++) {
            rentang.put(new JSONObject().put("label", LABEL_KADALUARSA[i]).put("jumlah", bucket[i]));
        }
        j.put("rentang", rentang);
        // Dipisahkan karena inilah angka yang menuntut tindakan segera.
        j.put("sudahKadaluarsa", bucket[0]);
        j.put("segeraKadaluarsa", bucket[1]);

        JSONArray item = new JSONArray();
        for (Object[] r : d.item) {
            if (r == null || r.length < 2) continue;
            item.put(new JSONObject()
                    .put("nama", r[0] == null ? "(Tanpa Nama)" : r[0].toString())
                    .put("sisaHari", r[1] instanceof Number ? ((Number) r[1]).intValue() : 0));
        }
        j.put("item", item);
    }

    // ------------------------------------------------------------------ util

    private static JSONArray bulanLabel() {
        JSONArray a = new JSONArray();
        for (int i = 0; i < 12; i++) {
            a.put(NAMA_BULAN[i]);
        }
        return a;
    }

    private static JSONArray angkaArray(double[] nilai) throws Exception {
        JSONArray a = new JSONArray();
        for (int i = 0; i < nilai.length; i++) {
            a.put(nilai[i]);
        }
        return a;
    }

    private static JSONArray petaAngka(Map<String, Integer> peta) throws Exception {
        JSONArray a = new JSONArray();
        for (Map.Entry<String, Integer> e : peta.entrySet()) {
            a.put(new JSONObject().put("nama", e.getKey())
                    .put("jumlah", e.getValue() == null ? 0 : e.getValue().intValue()));
        }
        return a;
    }

    /** Label bulan dengan nilai tertinggi; kosong bila seluruhnya nol. */
    private static String puncak(double[] perBulan) {
        int idx = -1;
        double maks = 0;
        for (int b = 0; b < perBulan.length && b < 12; b++) {
            if (perBulan[b] > maks) {
                maks = perBulan[b];
                idx = b;
            }
        }
        return idx < 0 ? "" : NAMA_BULAN[idx];
    }

    private static int tahunParam(HttpServletRequest request, int bawaan) {
        String v = text(request.getParameter("tahun"), "").trim();
        if (v.length() == 0) return bawaan;
        try {
            int t = Integer.parseInt(v);
            if (t < 2000 || t > 2999) throw new NumberFormatException();
            return t;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Tahun tidak sah.");
        }
    }

    private static int bulanParam(HttpServletRequest request, int bawaan) {
        String v = text(request.getParameter("bulan"), "").trim();
        if (v.length() == 0) return bawaan;
        try {
            int b = Integer.parseInt(v);
            if (b < 1 || b > 12) throw new NumberFormatException();
            return b;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bulan tidak sah.");
        }
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
