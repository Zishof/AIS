package ais.common.newui.koperasi;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.koperasi.util.KantinDiskonEngine;
import ais.action.servlet.api.KantinHelper;
import ais.common.Common;
import ais.common.newui.NewUiCsrfUtil;
import ais.common.newui.NewUiRouteGuard;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.koperasi.AnggotaKoperasi;

/**
 * Kontrak native lima layar kantin milik anggota koperasi.
 *
 * <p>Kelimanya — Toko Online, Ringkasan Saya, Notifikasi Saya, Riwayat
 * Transaksi Saya, dan Pesanan Saya — berbagi satu sifat yang menentukan
 * bentuk kontrak ini: <b>seluruhnya terkunci pada identitas</b>. Tidak satu
 * pun menerima "id anggota" dari klien; anggotanya selalu diambil dari
 * {@code Tbmuser.getAnggotaKoperasi()} milik sesi yang sedang berjalan.
 * Menerima id dari klien akan membuat siapa pun dapat membaca saldo, riwayat
 * belanja, dan notifikasi orang lain hanya dengan mengganti satu angka.</p>
 *
 * <h3>Harga dihitung server, bukan dikirim klien</h3>
 * <p>Pada layar ZK, diskon dihitung oleh composer yang juga berjalan di server
 * sehingga angkanya tepercaya. Kontrak native menerima permintaan dari klien
 * yang tidak tepercaya, maka klien HANYA boleh menyebut (produk, jumlah).
 * Harga satuan dibaca ulang dari basis data dan potongan dinilai oleh
 * {@link KantinDiskonEngine} — mesin yang sama yang dipakai layar ZK, agar
 * pembeli memperoleh harga identik dari layar mana pun.</p>
 *
 * <h3>Local-first bertingkat</h3>
 * <p>Aksi {@code meta} dan {@code list} boleh dilayani salinan lokal: daftar
 * toko, cara pembayaran, dan katalog produk jarang berubah. Sebaliknya
 * {@code harga} (yang juga membawa saldo terkini) dan seluruh mutasi
 * <b>tidak pernah</b> disimpan — saldo dan pembayaran adalah data L0 yang
 * salah bila disajikan basi.</p>
 *
 * <p>Fail-closed: mode tak dikenal ditolak, sesi tanpa anggota koperasi
 * ditolak, mutasi wajib POST beserta token CSRF.</p>
 */
public final class NewUiKantinMemberController {

    /** Harus sama dengan awalan folder JSP sebelum {@code /uiux/}. */
    private static final String MODULE = "koperasi";

    /** Toko Online (belanja). */
    public static final String MODE_BERANDA = "beranda";
    /** Ringkasan Saya. */
    public static final String MODE_DASHBOARD = "dashboard";
    /** Notifikasi Saya. */
    public static final String MODE_NOTIFIKASI = "notifikasi";
    /** Riwayat Transaksi Saya. */
    public static final String MODE_RIWAYAT = "riwayat";
    /** Pesanan Saya (draft belum dibayar). */
    public static final String MODE_PESANAN = "pesanan";

    private static final int BATAS_PRODUK = 80;
    private static final int BATAS_BARIS = 300;
    private static final int BATAS_NOTIFIKASI = 200;
    /** Batas wajar satu keranjang; menahan permintaan yang dibuat-buat. */
    private static final int BATAS_KERANJANG = 100;

    private NewUiKantinMemberController() { }

    public static void handle(HttpServletRequest request, HttpServletResponse response,
            String mode, String pageKey) throws Exception {
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        JSONObject json = new JSONObject();
        try {
            if (!modeDikenal(mode)) throw new IllegalArgumentException("Mode kantin tidak dikenal.");
            String action = text(request.getParameter("action"), "meta");
            if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, pageKey, action)) {
                response.setStatus(403);
                fail(json, "ACTION_FORBIDDEN", "Hak akses tidak tersedia.");
                write(response, json);
                return;
            }
            Tbmuser user = Common.getCurrentUser(request);
            if (user == null) throw new SecurityException("Sesi tidak dikenal.");

            if (MODE_NOTIFIKASI.equals(mode)) {
                // Notifikasi menempel pada userId, bukan pada keanggotaan koperasi,
                // sehingga pengguna non-anggota pun berhak membacanya.
                notifikasi(json, request, user, action);
            } else {
                AnggotaKoperasi member = user.getAnggotaKoperasi();
                if (member == null || member.getId() == null) {
                    throw new SecurityException("Halaman ini khusus Anggota Koperasi.");
                }
                if (MODE_BERANDA.equals(mode)) beranda(json, request, response, user, member, action);
                else if (MODE_DASHBOARD.equals(mode)) dashboard(json, member, action);
                else if (MODE_RIWAYAT.equals(mode)) riwayat(json, request, member, action);
                else pesanan(json, request, member, action);
            }
            json.put("ok", true);
        } catch (SecurityException e) {
            response.setStatus(403);
            fail(json, "FORBIDDEN", e.getMessage());
        } catch (IllegalArgumentException e) {
            response.setStatus(422);
            fail(json, "VALIDATION_FAILED", e.getMessage());
        } catch (Exception e) {
            response.setStatus(500);
            fail(json, "INTERNAL_ERROR", "Gagal memproses permintaan kantin. Detail dicatat di log server.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiKantinMemberController"); } catch (Exception ignored) { }
        }
        write(response, json);
    }

    public static boolean modeDikenal(String mode) {
        return MODE_BERANDA.equals(mode) || MODE_DASHBOARD.equals(mode) || MODE_NOTIFIKASI.equals(mode)
                || MODE_RIWAYAT.equals(mode) || MODE_PESANAN.equals(mode);
    }

    // =================================================================== toko online

    private static void beranda(JSONObject j, HttpServletRequest request, HttpServletResponse response,
            Tbmuser user, AnggotaKoperasi member, String action) throws Exception {
        if ("meta".equals(action)) {
            j.put("judul", "Toko Online");
            j.put("namaAnggota", teks(member.getNama()));
            // Token CSRF diterbitkan di sini karena meta selalu dipanggil lebih
            // dulu; mutasi belanja menolak permintaan tanpa token ini.
            j.put("csrfHeader", NewUiCsrfUtil.HEADER);
            j.put("csrfToken", NewUiCsrfUtil.getToken(request.getSession(true)));
            JSONArray toko = new JSONArray();
            for (Object[] r : rows("SELECT id, nama FROM koperasi.toko WHERE aktif = true ORDER BY nama ASC")) {
                toko.put(new JSONObject().put("id", lng(r[0])).put("nama", str(r[1])));
            }
            j.put("toko", toko);
            j.put("caraBayar", caraBayar(jenisId(member)));
            return;
        }
        if ("list".equals(action)) {
            Long tokoId = idWajib(request.getParameter("toko"), "Toko belum dipilih.");
            String kw = aman(request.getParameter("q"));
            StringBuilder sql = new StringBuilder("SELECT id, kode, nama, COALESCE(hargajual,0) FROM koperasi.produk "
                    + "WHERE aktif = true AND toko = " + tokoId);
            if (kw.length() > 0) {
                sql.append(" AND (nama ILIKE '%").append(kw).append("%' OR kode = '").append(kw).append("')");
            }
            sql.append(" ORDER BY nama ASC LIMIT ").append(BATAS_PRODUK);
            JSONArray produk = new JSONArray();
            for (Object[] r : rows(sql.toString())) {
                produk.put(new JSONObject().put("id", lng(r[0])).put("kode", str(r[1]))
                        .put("nama", str(r[2])).put("harga", num(r[3])));
            }
            j.put("rows", produk);
            j.put("terpotong", produk.length() >= BATAS_PRODUK);
            return;
        }
        if ("harga".equals(action)) {
            List<Baris> keranjang = bacaKeranjang(request.getParameter("keranjang"));
            JSONArray rincian = new JSONArray();
            double total = 0, totalDiskon = 0, totalCashback = 0;
            List<KantinDiskonEngine.Aturan> aturan = KantinDiskonEngine.muatAturan(HibernateUtil.currentSession());
            Date now = new Date();
            for (Baris b : keranjang) {
                KantinDiskonEngine.Baris nilai =
                        new KantinDiskonEngine.Baris(b.produkId, b.tokoId, b.harga, b.jumlah);
                KantinDiskonEngine.evaluasi(nilai, aturan, jenisId(member), tipeId(member), now);
                double subtotal = b.harga * b.jumlah - nilai.diskon;
                total += subtotal;
                totalDiskon += nilai.diskon;
                totalCashback += nilai.cashback;
                rincian.put(new JSONObject().put("produk", b.produkId).put("toko", b.tokoId)
                        .put("kode", b.kode).put("nama", b.nama).put("namaToko", b.namaToko)
                        .put("harga", b.harga).put("jumlah", b.jumlah)
                        .put("diskon", nilai.diskon).put("cashback", nilai.cashback)
                        .put("subtotal", subtotal));
            }
            j.put("rows", rincian);
            j.put("total", total);
            j.put("totalDiskon", totalDiskon);
            j.put("totalCashback", totalCashback);
            // Saldo ikut di sini, bukan di meta, supaya tidak pernah tersimpan
            // sebagai salinan lokal yang basi.
            j.put("saldo", saldo(member));
            return;
        }
        if ("create".equals(action)) {
            wajibMutasi(request);
            bayar(j, request, user, member);
            return;
        }
        throw new IllegalArgumentException("Aksi tidak dikenal.");
    }

    /**
     * Proses pembayaran belanja.
     *
     * <p>Meniru {@code BerandaAnggotaKantinAction.onBayar}: keranjang
     * dikelompokkan per toko lalu tiap kelompok diserahkan ke
     * {@link KantinHelper} — {@code bayar} untuk potong saldo, atau
     * {@code draft_bayar} bila cara bayarnya manual (bayar di kasir).</p>
     *
     * <p>Yang berbeda dari ZK: harga satuan dan potongan tidak berasal dari
     * klien melainkan dibaca dan dihitung ulang di sini.</p>
     */
    private static void bayar(JSONObject j, HttpServletRequest request, Tbmuser user, AnggotaKoperasi member)
            throws Exception {
        List<Baris> keranjang = bacaKeranjang(request.getParameter("keranjang"));
        if (keranjang.isEmpty()) throw new IllegalArgumentException("Keranjang masih kosong.");
        Long caraBayarId = idWajib(request.getParameter("caraBayar"), "Cara pembayaran belum dipilih.");

        // Cara bayar divalidasi ulang terhadap daftar yang memang boleh dipakai
        // jenis anggota ini; klien tidak boleh memakai metode di luar haknya.
        Boolean manual = manualUntuk(caraBayarId, jenisId(member));
        if (manual == null) throw new IllegalArgumentException("Cara pembayaran tidak tersedia untuk keanggotaan Anda.");

        List<KantinDiskonEngine.Aturan> aturan = KantinDiskonEngine.muatAturan(HibernateUtil.currentSession());
        Date now = new Date();
        double grandTotal = 0;
        for (Baris b : keranjang) {
            KantinDiskonEngine.Baris nilai = new KantinDiskonEngine.Baris(b.produkId, b.tokoId, b.harga, b.jumlah);
            KantinDiskonEngine.evaluasi(nilai, aturan, jenisId(member), tipeId(member), now);
            b.diskon = nilai.diskon;
            b.cashback = nilai.cashback;
            b.aturanDiskonId = nilai.aturanDiskonId;
            grandTotal += b.harga * b.jumlah - b.diskon;
        }
        double saldo = saldo(member);
        if (!manual.booleanValue() && saldo < grandTotal) {
            throw new IllegalArgumentException("Saldo tidak mencukupi. Saldo Rp " + bulat(saldo)
                    + ", total belanja Rp " + bulat(grandTotal) + ".");
        }

        Map<Long, List<Baris>> perToko = new LinkedHashMap<Long, List<Baris>>();
        for (Baris b : keranjang) {
            List<Baris> daftar = perToko.get(b.tokoId);
            if (daftar == null) {
                daftar = new ArrayList<Baris>();
                perToko.put(b.tokoId, daftar);
            }
            daftar.add(b);
        }

        String catatan = text(request.getParameter("catatan"), "").trim();
        List<String> gagal = new ArrayList<String>();
        int berhasil = 0;
        for (Map.Entry<Long, List<Baris>> e : perToko.entrySet()) {
            JSONArray transaksi = new JSONArray();
            for (Baris b : e.getValue()) {
                JSONObject t = new JSONObject();
                t.put("id", b.produkId);
                t.put("kode", b.kode == null ? "" : b.kode);
                t.put("nama", b.nama == null ? "" : b.nama);
                t.put("harga", b.harga);
                t.put("jumlah", b.jumlah);
                t.put("diskon", b.diskon);
                t.put("cashback", b.cashback);
                if (b.aturanDiskonId != null) t.put("aturanDiskon", b.aturanDiskonId);
                transaksi.put(t);
            }
            JSONObject payload = new JSONObject();
            payload.put("kodeUnik", "ONL-" + System.currentTimeMillis() + Common.getGeneratedBarCode(4));
            payload.put("idToko", e.getKey());
            payload.put("waktu", Common.dateFormat3.get().format(new Date()));
            payload.put("caraBayar", caraBayarId);
            payload.put("id_member", member.getId());
            if (catatan.length() > 0) payload.put("keterangan", catatan);
            payload.put("transaksi", transaksi);

            JSONObject hasil = new JSONObject();
            if (manual.booleanValue()) {
                KantinHelper.draft_bayar(user, payload, hasil);
            } else {
                KantinHelper.bayar(user, payload, hasil);
            }
            String st = hasil.optString("status", "");
            if ("00".equals(st) || "success".equalsIgnoreCase(st)) {
                berhasil++;
            } else {
                gagal.add(hasil.optString("description", "Transaksi salah satu tenant gagal."));
            }
        }

        j.put("tenantBerhasil", berhasil);
        j.put("tenantGagal", gagal.size());
        j.put("manual", manual.booleanValue());
        j.put("saldo", saldo(member));
        if (!gagal.isEmpty()) {
            // Sebagian tenant bisa berhasil sementara lainnya gagal; keadaan itu
            // dilaporkan apa adanya agar pengguna tahu apa yang sudah terjadi.
            j.put("peringatan", gabung(gagal));
        }
        j.put("pesan", gagal.isEmpty()
                ? (manual.booleanValue() ? "Pesanan dibuat. Silakan selesaikan pembayaran di kasir."
                        : "Pembayaran berhasil. Terima kasih telah berbelanja!")
                : "Sebagian transaksi tidak dapat diproses.");
    }

    /**
     * Baca keranjang kiriman klien.
     *
     * <p>Hanya (produk, toko, jumlah) yang dipercaya. Nama, kode, dan
     * <b>harga</b> dibaca ulang dari basis data; produk yang tidak aktif atau
     * bukan milik toko yang disebut akan ditolak, bukan diabaikan diam-diam.</p>
     */
    private static List<Baris> bacaKeranjang(String mentah) throws Exception {
        List<Baris> hasil = new ArrayList<Baris>();
        String isi = text(mentah, "").trim();
        if (isi.length() == 0) return hasil;
        JSONArray arr;
        try {
            arr = new JSONArray(isi);
        } catch (Exception e) {
            throw new IllegalArgumentException("Format keranjang tidak dikenal.");
        }
        if (arr.length() > BATAS_KERANJANG) {
            throw new IllegalArgumentException("Isi keranjang melebihi batas " + BATAS_KERANJANG + " baris.");
        }
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            long produkId = o.optLong("produk", 0);
            long tokoId = o.optLong("toko", 0);
            int jumlah = o.optInt("jumlah", 0);
            if (produkId <= 0 || tokoId <= 0) throw new IllegalArgumentException("Baris keranjang tidak lengkap.");
            if (jumlah <= 0) throw new IllegalArgumentException("Jumlah barang harus lebih dari nol.");
            List<Object[]> p = rows("SELECT kode, nama, COALESCE(hargajual,0), "
                    + "(SELECT nama FROM koperasi.toko WHERE id = " + tokoId + ") "
                    + "FROM koperasi.produk WHERE id = " + produkId + " AND aktif = true AND toko = " + tokoId);
            if (p.isEmpty()) {
                throw new IllegalArgumentException("Produk tidak tersedia pada toko yang dipilih.");
            }
            Object[] r = p.get(0);
            Baris b = new Baris();
            b.produkId = Long.valueOf(produkId);
            b.tokoId = Long.valueOf(tokoId);
            b.kode = str(r[0]);
            b.nama = str(r[1]);
            b.harga = num(r[2]);
            b.namaToko = str(r[3]);
            b.jumlah = jumlah;
            hasil.add(b);
        }
        return hasil;
    }

    /** Daftar cara pembayaran daring yang boleh dipakai jenis anggota tertentu. */
    private static JSONArray caraBayar(Long jenisAnggotaId) throws Exception {
        StringBuilder sql = new StringBuilder("SELECT cpk.id, cpk.nama, cpk.manual FROM koperasi.cara_pembayaran_koperasi cpk "
                + "WHERE cpk.aktif = true AND cpk.online = true");
        if (jenisAnggotaId != null) {
            sql.append(" AND (SELECT jak.daftar_cara_pembayaran_yang_boleh_di_pilih FROM koperasi.jenis_anggota_koperasi jak "
                    + "WHERE jak.id = ").append(jenisAnggotaId).append(") LIKE '%,' || cpk.id || ',%'");
        }
        sql.append(" ORDER BY cpk.nama ASC");
        JSONArray out = new JSONArray();
        for (Object[] r : rows(sql.toString())) {
            out.put(new JSONObject().put("id", lng(r[0])).put("nama", str(r[1])).put("manual", bool(r[2])));
        }
        return out;
    }

    /**
     * {@code TRUE}/{@code FALSE} bila cara bayar boleh dipakai jenis anggota ini
     * (nilainya menyatakan apakah metode itu manual), {@code null} bila tidak boleh.
     */
    private static Boolean manualUntuk(Long caraBayarId, Long jenisAnggotaId) throws Exception {
        JSONArray daftar = caraBayar(jenisAnggotaId);
        for (int i = 0; i < daftar.length(); i++) {
            JSONObject o = daftar.getJSONObject(i);
            if (caraBayarId.longValue() == o.optLong("id", -1)) {
                return Boolean.valueOf(o.optBoolean("manual", false));
            }
        }
        return null;
    }

    // =================================================================== ringkasan

    /**
     * Ringkasan Saya. Seluruh angkanya dikirim mentah (bukan HTML) agar klien
     * menggambar kartu, grafik tren, dan peta jam belanjanya sendiri secara
     * native — layar ZK menyusun HTML, kontrak ini tidak.
     */
    private static void dashboard(JSONObject j, AnggotaKoperasi member, String action) throws Exception {
        if (!"meta".equals(action)) throw new IllegalArgumentException("Aksi tidak dikenal.");
        Long id = member.getId();
        j.put("judul", "Ringkasan Saya");
        j.put("namaAnggota", teks(member.getNama()));

        Object[] b = satu("SELECT COUNT(id), COALESCE(SUM(total_biaya),0), "
                + "COALESCE(SUM(COALESCE(total_diskon,0)+COALESCE(totalcashback,0)),0) "
                + "FROM koperasi.pembelian_anggota_koperasi WHERE anggota_koperasi = " + id);
        j.put("jumlahTransaksi", b == null ? 0 : (long) num(b[0]));
        j.put("totalPengeluaran", b == null ? 0 : num(b[1]));
        j.put("totalHemat", b == null ? 0 : num(b[2]));

        // Sumber saldo resmi = public.deposit, sama seperti DepositHelper.
        // Memakai virtual_account_bank saja akan melewatkan top-up manual.
        Object[] tp = satu("SELECT COALESCE(SUM(nominal),0) FROM public.deposit WHERE anggota_koperasi = " + id);
        j.put("totalTopup", tp == null ? 0 : num(tp[0]));
        j.put("saldo", saldo(member));

        JSONArray tren = new JSONArray();
        for (Object[] r : rows("SELECT TO_CHAR(tanggal_pembayaran,'Mon YYYY'), COALESCE(SUM(total_biaya),0) "
                + "FROM koperasi.pembelian_anggota_koperasi WHERE anggota_koperasi = " + id
                + " GROUP BY TO_CHAR(tanggal_pembayaran,'Mon YYYY'), TO_CHAR(tanggal_pembayaran,'YYYY-MM') "
                + "ORDER BY TO_CHAR(tanggal_pembayaran,'YYYY-MM') ASC LIMIT 6")) {
            tren.put(new JSONObject().put("label", str(r[0])).put("nilai", num(r[1])));
        }
        j.put("tren", tren);

        JSONArray tenant = new JSONArray();
        for (Object[] r : rows("SELECT COALESCE(t.nama,'Koperasi Utama'), COALESCE(SUM(a.total_biaya),0) "
                + "FROM koperasi.pembelian_anggota_koperasi a LEFT JOIN koperasi.toko t ON t.id = a.toko "
                + "WHERE a.anggota_koperasi = " + id + " GROUP BY t.nama ORDER BY 2 DESC LIMIT 5")) {
            tenant.put(new JSONObject().put("nama", str(r[0])).put("nilai", num(r[1])));
        }
        j.put("tenantFavorit", tenant);

        JSONArray jam = new JSONArray();
        for (Object[] r : rows("SELECT CAST(EXTRACT(DOW FROM tanggal_pembayaran) AS integer), "
                + "CAST(EXTRACT(HOUR FROM tanggal_pembayaran) AS integer), COUNT(id) "
                + "FROM koperasi.pembelian_anggota_koperasi WHERE anggota_koperasi = " + id + " GROUP BY 1, 2")) {
            jam.put(new JSONObject().put("hari", (int) num(r[0])).put("jam", (int) num(r[1]))
                    .put("jumlah", (int) num(r[2])));
        }
        j.put("jamBelanja", jam);
    }

    // =================================================================== notifikasi

    private static void notifikasi(JSONObject j, HttpServletRequest request, Tbmuser user, String action)
            throws Exception {
        String uid = teks(user.getUserId()).replace("'", "''");
        if (uid.length() == 0) throw new SecurityException("Sesi tanpa identitas pengguna.");
        if ("meta".equals(action)) {
            j.put("judul", "Notifikasi Saya");
            return;
        }
        if (!"list".equals(action)) throw new IllegalArgumentException("Aksi tidak dikenal.");
        JSONArray out = new JSONArray();
        int belum = 0;
        for (Object[] r : rows("SELECT TO_CHAR(n.waktu,'dd-MM-yyyy HH24:MI'), COALESCE(n.keterangan,''), "
                + "COALESCE(n.hasil,''), n.buka FROM public.notifikasi n WHERE n.nama = '" + uid + "' "
                + "ORDER BY n.waktu DESC LIMIT " + BATAS_NOTIFIKASI)) {
            boolean dibaca = bool(r[3]);
            if (!dibaca) belum++;
            out.put(new JSONObject().put("waktu", str(r[0])).put("isi", str(r[1]))
                    .put("status", str(r[2])).put("dibaca", dibaca));
        }
        j.put("rows", out);
        j.put("total", out.length());
        j.put("belumDibaca", belum);
        j.put("terpotong", out.length() >= BATAS_NOTIFIKASI);
    }

    // =================================================================== riwayat

    private static void riwayat(JSONObject j, HttpServletRequest request, AnggotaKoperasi member, String action)
            throws Exception {
        Long id = member.getId();
        if ("meta".equals(action)) {
            j.put("judul", "Riwayat Transaksi Saya");
            j.put("namaAnggota", teks(member.getNama()));
            return;
        }
        if ("list".equals(action) || "export".equals(action)) {
            JSONArray out = new JSONArray();
            double totBelanja = 0, totDiskon = 0, totCashback = 0;
            for (Object[] r : rows("SELECT a.id, TO_CHAR(a.tanggal_pembayaran,'dd-MM-yyyy HH24:MI'), "
                    + "COALESCE(b.nama,'-'), COALESCE(a.total_diskon,0), COALESCE(a.totalcashback,0), "
                    + "COALESCE(a.total_biaya,0) FROM koperasi.pembelian_anggota_koperasi a "
                    + "LEFT JOIN koperasi.toko b ON b.id = a.toko WHERE a.anggota_koperasi = " + id
                    + " ORDER BY a.tanggal_pembayaran DESC, a.id DESC LIMIT " + BATAS_BARIS)) {
                totDiskon += num(r[3]);
                totCashback += num(r[4]);
                totBelanja += num(r[5]);
                out.put(new JSONObject().put("id", lng(r[0])).put("tanggal", str(r[1])).put("tenant", str(r[2]))
                        .put("diskon", num(r[3])).put("cashback", num(r[4])).put("total", num(r[5])));
            }
            j.put("rows", out);
            j.put("jumlahTransaksi", out.length());
            j.put("totalBelanja", totBelanja);
            j.put("totalDiskon", totDiskon);
            j.put("totalCashback", totCashback);
            j.put("terpotong", out.length() >= BATAS_BARIS);
            return;
        }
        if ("detail".equals(action)) {
            Long trxId = idWajib(request.getParameter("id"), "Transaksi belum dipilih.");
            // Kepemilikan diperiksa di klausa WHERE: transaksi milik orang lain
            // tidak menghasilkan baris apa pun, bukan sekadar disembunyikan di UI.
            if (satu("SELECT id FROM koperasi.pembelian_anggota_koperasi WHERE id = " + trxId
                    + " AND anggota_koperasi = " + id) == null) {
                throw new SecurityException("Transaksi tidak ditemukan pada riwayat Anda.");
            }
            JSONArray out = new JSONArray();
            for (Object[] r : rows("SELECT COALESCE(c.nama,''), COALESCE(a.hargajual,0), COALESCE(a.qty,0), "
                    + "COALESCE(a.diskon,0) FROM koperasi.pembelian a LEFT JOIN koperasi.produk c ON c.id = a.produk "
                    + "WHERE a.pembelian_anggota_koperasi = " + trxId)) {
                double sub = num(r[1]) * num(r[2]) - num(r[3]);
                out.put(new JSONObject().put("produk", str(r[0])).put("harga", num(r[1]))
                        .put("jumlah", num(r[2])).put("diskon", num(r[3])).put("subtotal", sub));
            }
            j.put("rows", out);
            return;
        }
        throw new IllegalArgumentException("Aksi tidak dikenal.");
    }

    // =================================================================== pesanan

    private static void pesanan(JSONObject j, HttpServletRequest request, AnggotaKoperasi member, String action)
            throws Exception {
        Long id = member.getId();
        if ("meta".equals(action)) {
            j.put("judul", "Pesanan Saya");
            j.put("csrfHeader", NewUiCsrfUtil.HEADER);
            j.put("csrfToken", NewUiCsrfUtil.getToken(request.getSession(true)));
            return;
        }
        if ("list".equals(action)) {
            String from = "FROM koperasi.draft_pembelian_anggota_koperasi a "
                    + "LEFT JOIN koperasi.toko b ON a.toko = b.id "
                    + "LEFT JOIN koperasi.cara_pembayaran_koperasi cp ON cp.id = a.cara_pembayaran_koperasi ";
            String where = saringan(request, id);
            Object[] sum = satu("SELECT COUNT(*), "
                    + "COALESCE(SUM(CASE WHEN a.lunas IS NULL THEN COALESCE(a.total_biaya,0) ELSE 0 END),0), "
                    + "COALESCE(SUM(CASE WHEN a.lunas IS NULL THEN 1 ELSE 0 END),0) " + from + where);
            j.put("totalPesanan", sum == null ? 0 : (long) num(sum[0]));
            j.put("tagihanBelumDibayar", sum == null ? 0 : num(sum[1]));
            j.put("belumDibayar", sum == null ? 0 : (long) num(sum[2]));

            JSONArray out = new JSONArray();
            for (Object[] r : rows("SELECT a.id, TO_CHAR(a.tanggal_pembayaran,'DD-MM-YYYY HH24:MI'), "
                    + "COALESCE(b.nama,'Kantin Utama'), COALESCE(cp.nama,'-'), COALESCE(a.keterangan,''), "
                    + "COALESCE(a.total_diskon,0), COALESCE(a.totalcashback,0), COALESCE(a.total_biaya,0), a.lunas "
                    + from + where + " ORDER BY a.id DESC LIMIT " + BATAS_BARIS)) {
                out.put(new JSONObject().put("id", lng(r[0])).put("waktu", str(r[1])).put("toko", str(r[2]))
                        .put("caraBayar", str(r[3])).put("keterangan", str(r[4]))
                        .put("diskon", num(r[5])).put("cashback", num(r[6])).put("total", num(r[7]))
                        .put("lunas", r[8] != null));
            }
            j.put("rows", out);
            j.put("terpotong", out.length() >= BATAS_BARIS);
            return;
        }
        if ("delete".equals(action)) {
            wajibMutasi(request);
            batalkan(j, request, id);
            return;
        }
        throw new IllegalArgumentException("Aksi tidak dikenal.");
    }

    /**
     * Batalkan pesanan yang belum dibayar.
     *
     * <p>Status lunas diperiksa ulang di dalam transaksi (anti balapan dengan
     * kasir yang mungkin sedang menerima pembayaran yang sama), dan setiap
     * klausa menyertakan pemilik agar pesanan orang lain tidak dapat dihapus
     * walau id-nya ditebak.</p>
     */
    private static void batalkan(JSONObject j, HttpServletRequest request, Long memberId) throws Exception {
        Long id = idWajib(request.getParameter("id"), "Pesanan belum dipilih.");
        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            Object ada = s.createSQLQuery("SELECT id FROM koperasi.draft_pembelian_anggota_koperasi "
                    + "WHERE id = " + id + " AND anggota_koperasi = " + memberId).uniqueResult();
            if (ada == null) throw new SecurityException("Pesanan tidak ditemukan pada daftar Anda.");
            Object lunas = s.createSQLQuery("SELECT lunas FROM koperasi.draft_pembelian_anggota_koperasi "
                    + "WHERE id = " + id + " AND anggota_koperasi = " + memberId).uniqueResult();
            if (lunas != null) {
                throw new IllegalArgumentException("Pesanan ini telah dibayar sehingga tidak dapat dibatalkan.");
            }
            org.hibernate.Transaction tx = s.beginTransaction();
            try {
                s.createSQLQuery("DELETE FROM koperasi.draft_pembelian WHERE draft_pembelian_anggota_koperasi = " + id)
                        .executeUpdate();
                int n = s.createSQLQuery("DELETE FROM koperasi.draft_pembelian_anggota_koperasi WHERE id = " + id
                        + " AND anggota_koperasi = " + memberId).executeUpdate();
                tx.commit();
                j.put("dihapus", n);
                j.put("pesan", "Pesanan telah dibatalkan.");
            } catch (Exception e) {
                try { tx.rollback(); } catch (Exception ignored) { }
                throw e;
            }
        } finally {
            try { s.close(); } catch (Exception ignored) { }
        }
    }

    /** Saringan daftar pesanan; sama dengan filter pada layar ZK. */
    private static String saringan(HttpServletRequest request, Long memberId) {
        StringBuilder w = new StringBuilder(" WHERE a.anggota_koperasi = " + memberId + " ");
        String mulai = tanggal(request.getParameter("mulai"));
        String sampai = tanggal(request.getParameter("sampai"));
        if (mulai != null && sampai != null) {
            w.append(" AND DATE(a.tanggal_pembayaran) BETWEEN DATE('").append(mulai)
                    .append("') AND DATE('").append(sampai).append("') ");
        }
        String kw = aman(request.getParameter("q"));
        if (kw.length() > 0) {
            w.append(" AND (b.nama ILIKE '%").append(kw).append("%' OR cp.nama ILIKE '%").append(kw).append("%') ");
        }
        return w.toString();
    }

    // =================================================================== util

    /** Satu baris keranjang setelah harga dibaca ulang dari basis data. */
    private static final class Baris {
        Long produkId, tokoId, aturanDiskonId;
        String kode, nama, namaToko;
        double harga, diskon, cashback;
        int jumlah;
    }

    /** Mutasi hanya lewat POST beserta token CSRF yang sah. */
    private static void wajibMutasi(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            throw new SecurityException("Mutasi hanya dilayani lewat POST.");
        }
        if (!NewUiCsrfUtil.isValid(request)) {
            throw new SecurityException("Token CSRF tidak sah. Muat ulang halaman.");
        }
    }

    private static double saldo(AnggotaKoperasi member) {
        try {
            Double s = ais.action.master.sekolah.util.DepositHelper.hitungDeposit(member);
            return s == null ? 0 : s.doubleValue();
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "NewUiKantinMemberController.saldo");
            return 0;
        }
    }

    private static Long jenisId(AnggotaKoperasi m) {
        return m.getJenisAnggotaKoperasi() == null ? null : m.getJenisAnggotaKoperasi().getId();
    }

    private static Long tipeId(AnggotaKoperasi m) {
        return m.getTipeAnggotaKoperasi() == null ? null : m.getTipeAnggotaKoperasi().getId();
    }

    /** Id numerik wajib; menolak nilai bukan angka agar tidak menjadi celah SQL. */
    private static Long idWajib(String nilai, String pesan) {
        String v = text(nilai, "").trim();
        if (v.length() == 0) throw new IllegalArgumentException(pesan);
        try {
            long l = Long.parseLong(v);
            if (l <= 0) throw new NumberFormatException();
            return Long.valueOf(l);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(pesan);
        }
    }

    /** Tanggal yyyy-MM-dd; bentuk lain diabaikan agar tidak masuk ke SQL. */
    private static String tanggal(String nilai) {
        String v = text(nilai, "").trim();
        return v.matches("\\d{4}-\\d{2}-\\d{2}") ? v : null;
    }

    /** Kata kunci untuk ILIKE; kutip tunggal digandakan dan panjangnya dibatasi. */
    private static String aman(String nilai) {
        String v = text(nilai, "").trim();
        if (v.length() > 60) v = v.substring(0, 60);
        return v.replace("'", "''").replace("\\", "");
    }

    @SuppressWarnings("unchecked")
    private static List<Object[]> rows(String sql) {
        SQLQuery q = HibernateUtil.currentSession().createSQLQuery(sql);
        return q.list();
    }

    private static Object[] satu(String sql) {
        List<Object[]> r = rows(sql);
        return r == null || r.isEmpty() ? null : r.get(0);
    }

    private static String gabung(List<String> pesan) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pesan.size(); i++) {
            if (i > 0) sb.append(" ");
            sb.append(pesan.get(i));
        }
        return sb.toString();
    }

    private static String bulat(double v) {
        return String.valueOf((long) v);
    }

    private static String teks(String s) {
        return s == null ? "" : s;
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

    private static boolean bool(Object o) {
        if (o == null) return false;
        if (o instanceof Boolean) return ((Boolean) o).booleanValue();
        String s = o.toString().trim();
        return s.equals("t") || s.equalsIgnoreCase("true") || s.equals("1");
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
