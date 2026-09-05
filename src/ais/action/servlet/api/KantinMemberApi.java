package ais.action.servlet.api;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.OnlineBmtUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.Pedagang;
import ais.database.model.inventory.Toko;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.CaraPembayaranKoperasi;
import ais.database.model.koperasi.DraftPembelianAnggotaKoperasi;

/**
 * Mobile API endpoints untuk modul Kantin / Belanja Online.
 * Semua method memerlukan token valid pada field "token" di request JSON.
 */
public final class KantinMemberApi {

    private KantinMemberApi() {}

    // ── HELPER PRIVAT ─────────────────────────────────────────────────────────

    /**
     * Cari atau buat AnggotaKoperasi dalam session Hibernate yang sedang aktif.
     * Menghindari LazyInitializationException pada detached Tbmuser dari Api.tokens,
     * dan menerapkan logika auto-create yang sama dengan beranda.jsp.
     */
    @SuppressWarnings({"unchecked", "deprecation"})
    private static AnggotaKoperasi resolveAnggotaDb(Session s, Tbmuser detachedUser, boolean autoCreate) throws Exception {
        // 1. Reload Tbmuser dalam session supaya lazy associations bisa diakses
        // Tbmuser memakai String userId sebagai @Id (bukan Long id dari GeneralValueObject)
        if (detachedUser == null || detachedUser.getUserId() == null) return null;
        Tbmuser user = (Tbmuser) s.get(Tbmuser.class, detachedUser.getUserId());
        if (user == null) {
            // Fallback: user login via Mahasiswa/Siswa langsung (token di mahasiswa.token / siswa.token,
            // bukan di tbmuser.token). getUserId() = NIM/NIS, bukan PK tbmuser → s.get di atas null.
            // Cari AnggotaKoperasi berdasarkan entity terkait yang di-load ulang dalam session aktif.
            Criterion fallbackCriterion = null;
            if (detachedUser.getMahasiswa() != null && detachedUser.getMahasiswa().getId() != null) {
                Object mhsInSession = s.get(detachedUser.getMahasiswa().getClass(), detachedUser.getMahasiswa().getId());
                if (mhsInSession != null) fallbackCriterion = Restrictions.eq("mahasiswa", mhsInSession);
            } else if (detachedUser.getSiswa() != null && detachedUser.getSiswa().getId() != null) {
                Object siswaInSession = s.get(detachedUser.getSiswa().getClass(), detachedUser.getSiswa().getId());
                if (siswaInSession != null) fallbackCriterion = Restrictions.eq("siswa", siswaInSession);
            }
            if (fallbackCriterion == null) return null;
            return (AnggotaKoperasi) s.createCriteria(AnggotaKoperasi.class)
                .add(fallbackCriterion).setMaxResults(1).uniqueResult();
        }

        // 2. Cek via asosiasi langsung (sudah managed dalam session)
        AnggotaKoperasi anggota = user.getAnggotaKoperasi();

        // 3. Cari berdasarkan entity terkait (mengikuti logika beranda.jsp)
        if (anggota == null) {
            Criterion criterion;
            if (user.getMahasiswa() != null) {
                criterion = Restrictions.eq("mahasiswa", user.getMahasiswa());
            } else if (user.getSiswa() != null) {
                criterion = Restrictions.eq("siswa", user.getSiswa());
            } else if (user.getDosen() != null) {
                criterion = Restrictions.eq("dosen", user.getDosen());
            } else if (user.getGuru() != null) {
                criterion = Restrictions.eq("guru", user.getGuru());
            } else if (user.getPegawai() != null) {
                criterion = Restrictions.eq("pegawai", user.getPegawai());
            } else {
                criterion = Restrictions.eq("tbmuser", user);
            }
            anggota = (AnggotaKoperasi) s.createCriteria(AnggotaKoperasi.class)
                .add(criterion).setMaxResults(1).uniqueResult();
        }

        // 4. Auto-create jika diizinkan konfigurasi (identik dengan beranda.jsp)
        if (anggota == null && autoCreate && isKonfigAktif("jika_pengguna_login_secara_otomatis_jadi_anggota")) {
            anggota = new AnggotaKoperasi();
            anggota.setAktif(true);
            anggota.setTbmuser(user);
            anggota.setMahasiswa(user.getMahasiswa());
            anggota.setSiswa(user.getSiswa());
            anggota.setGuru(user.getGuru());
            anggota.setDosen(user.getDosen());
            anggota.setPegawai(user.getPegawai());
            anggota.setUserid(user.getUserId());
            try {
                anggota.setPass(Common.desEncrypter.get().decrypt(user.getUserPassword()));
            } catch (Exception e) {
                ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinMemberApi.java:resolveAnggotaDb");
            }
            anggota.setKode(AnggotaKoperasi.generateKodeMemberUnik(s, anggota, anggota.getTanggal()));
            if (!s.getTransaction().isActive()) s.getTransaction().begin();
            s.save(anggota);
            user.setAnggotaKoperasi(anggota);
            s.update(user);
            s.getTransaction().commit();
        }

        return anggota;
    }

    private static JSONObject noAuth() {
        return ApiHelperSupport.status("97", "Token tidak valid atau sudah kadaluarsa");
    }

    private static JSONObject noMember() {
        return ApiHelperSupport.status("99", "Anda belum terdaftar sebagai anggota koperasi");
    }

    private static Long jsonLong(long value) {
        return Long.valueOf(value);
    }

    private static boolean isKonfigAktif(String key) {
        try {
            return Common.bolehKonfigurasi(key);
        } catch (Exception e) {
            return false;
        }
    }

    private static void closeSession(Session s) {
        if (s == null) return;
        try { try { s.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinMemberApi.java:closeSession");} s.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinMemberApi.java:closeSession");}
        try { s.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinMemberApi.java:closeSession");}
    }

    private static int safeInt(JSONObject json, String key, int def) {
        try { return json.isNull(key) ? def : Integer.parseInt((json.get(key) + "").trim()); } catch (Exception e) { return def; }
    }

    private static String safeStr(JSONObject json, String key) {
        try { return json.isNull(key) ? "" : (json.get(key) + "").trim(); } catch (Exception e) { return ""; }
    }

    private static long safeLong(JSONObject json, String key, long def) {
        try {
            Object value = json == null || json.isNull(key) ? null : json.get(key);
            if (value instanceof Number) return ((Number) value).longValue();
            return value == null ? def : Long.parseLong(value.toString().trim());
        } catch (Exception e) {
            return def;
        }
    }

    // ── 1. INFO MEMBER ────────────────────────────────────────────────────────
    /**
     * Profil anggota + saldo + sisa cashback + flag konfigurasi.
     * Request: { token }
     * Response: { status:"00", data:{ id_member, nama, kode, saldo, sisa_cashback,
     *   aktifkan_topup, aktifkan_bayar_qr, aktifkan_pilihan_meja,
     *   label_saldo, label_cashback, tampilkan_cashback, tampilkan_saldo,
     *   minimal_saldo, id_jenis_anggota } }
     */
    public static JSONObject info(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception {
        Tbmuser user = ApiUtil.currentUser(json, req);
        if (user == null) return noAuth();

        // Session dibuka di sini agar resolveAnggotaDb dan akses lazy associations aman
        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            AnggotaKoperasi anggota = resolveAnggotaDb(s, user, true);
            if (anggota == null) return noMember();

            JSONObject data = new JSONObject();
            data.put("id_member", anggota.getId());
            data.put("nama",      anggota.getNama() != null ? anggota.getNama() : "");
            data.put("kode",      anggota.getKode() != null ? anggota.getKode() : "");
            data.put("aktifkan_topup",          isKonfigAktif("aktifkan_topup_di_anggota"));
            data.put("aktifkan_bayar_qr",       isKonfigAktif("aktifkan_bayar_via_qr_topup"));
            data.put("aktifkan_pilihan_meja",   isKonfigAktif("aktifkan_pilihan_meja"));

            // Lazy associations aman karena anggota masih managed dalam session s
            if (anggota.getJenisAnggotaKoperasi() != null) {
                ais.database.model.koperasi.JenisAnggotaKoperasi jak = anggota.getJenisAnggotaKoperasi();
                data.put("id_jenis_anggota",   jak.getId());
                data.put("label_saldo",        jak.getIstilahSisaSaldo()  != null ? jak.getIstilahSisaSaldo()  : "Saldo");
                data.put("label_cashback",     jak.getIstilahCashback()   != null ? jak.getIstilahCashback()   : "Cashback");
                data.put("tampilkan_cashback", jak.getTampilkanCashback()  != null && jak.getTampilkanCashback());
                data.put("tampilkan_saldo",    jak.getTampilkanSisaSaldo() == null || jak.getTampilkanSisaSaldo());
                data.put("minimal_saldo",      jak.getMinimalSaldo()       != null ? jak.getMinimalSaldo()      : 0);
            } else {
                data.put("label_saldo",        "Saldo");
                data.put("label_cashback",     "Cashback");
                data.put("tampilkan_cashback", false);
                data.put("tampilkan_saldo",    true);
                data.put("minimal_saldo",      0);
            }
            if (anggota.getTipeAnggotaKoperasi() != null) {
                data.put("id_tipe_anggota", anggota.getTipeAnggotaKoperasi().getId());
            }

            // Saldo via helper yang sudah ada (membuka session sendiri secara internal)
            JSONObject balReq = new JSONObject();
            balReq.put("id_member", anggota.getId().toString());
            JSONObject balRes = new JSONObject();
            KantinHelper.topup(balReq, balRes);
			// Runtime tenant lama hanya memiliki JSONObject.put(String,Object), bukan
			// overload put(String,long). Boxing eksplisit menjaga kompatibilitas biner.
			data.put("saldo", Long.valueOf(safeLong(balRes, "data", 0L)));

            // Sisa cashback (total cashback dari pembelian - yang sudah dicairkan)
            @SuppressWarnings("unchecked")
            List<Object> cbList = s.createSQLQuery(
                "SELECT COALESCE((SELECT SUM(p.cashback) FROM koperasi.pembelian p " +
                "LEFT JOIN koperasi.pembelian_anggota_koperasi pak ON p.pembelian_anggota_koperasi=pak.id " +
                "WHERE pak.anggota_koperasi=:m),0) - COALESCE((SELECT SUM(pd.nominal_cair) " +
                "FROM koperasi.pencairan_diskon pd WHERE pd.anggota_koperasi=:m AND pd.status='BERHASIL'),0)"
            ).setParameter("m", anggota.getId()).list();
			data.put("sisa_cashback", Long.valueOf(cbList.isEmpty() || cbList.get(0) == null
					? 0L : ((Number) cbList.get(0)).longValue()));

            JSONObject hasil = new JSONObject();
            hasil.put("data", data);
            hasil.put("status", "00");
            return hasil;
        } finally { closeSession(s); }
    }

    // ── 1b. DAFTAR MEMBER (auto-register tanpa cek konfigurasi server) ──────────
    /**
     * Daftarkan user sebagai AnggotaKoperasi jika belum terdaftar.
     * Dipanggil otomatis oleh mobile ketika kantin_info return status 99.
     * Tidak bergantung pada konfigurasi 'jika_pengguna_login_secara_otomatis_jadi_anggota'.
     *
     * Response: { status:"00", sudah_ada:<bool>, id_member:<long> }
     */
    @SuppressWarnings("deprecation")
    public static JSONObject daftarMember(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception {
        Tbmuser detachedUser = ApiUtil.currentUser(json, req);
        if (detachedUser == null) return noAuth();

        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            // Cek apakah sudah terdaftar
            AnggotaKoperasi existing = resolveAnggotaDb(s, detachedUser, false);
            if (existing != null) {
                JSONObject hasil = new JSONObject();
                hasil.put("status",    "00");
                hasil.put("sudah_ada", true);
                hasil.put("id_member", existing.getId());
                return hasil;
            }

            // Load user dalam session aktif
            Tbmuser user = (Tbmuser) s.get(Tbmuser.class, detachedUser.getUserId());

            // Buat AnggotaKoperasi baru — tanpa cek config server
            AnggotaKoperasi anggota = new AnggotaKoperasi();
            anggota.setAktif(true);

			if (user != null) {
                // Path normal: Tbmuser ada di database
                anggota.setTbmuser(user);
                anggota.setMahasiswa(user.getMahasiswa());
                anggota.setSiswa(user.getSiswa());
                anggota.setGuru(user.getGuru());
                anggota.setDosen(user.getDosen());
				anggota.setPegawai(user.getPegawai());
				if (user.getMahasiswa() != null) {
					s.setReadOnly(user.getMahasiswa(), true);
				}
				anggota.setUserid(user.getUserId());
                try {
                    anggota.setPass(Common.desEncrypter.get().decrypt(user.getUserPassword()));
                } catch (Exception e) {
                    ais.common.ErrorAuditUtil.record(e, "KantinMemberApi.daftarMember: setPass");
                }
            } else {
                // Fallback: user login via Mahasiswa/Siswa langsung tanpa baris di tabel Tbmuser.
                // getUserId() = NIM/NIS → s.get(Tbmuser.class, NIM) null. Identifikasi via entity terkait.
                anggota.setUserid(detachedUser.getUserId());
                if (detachedUser.getMahasiswa() != null && detachedUser.getMahasiswa().getId() != null) {
                    ais.database.model.Mahasiswa mhsInSession = (ais.database.model.Mahasiswa)
                        s.get(detachedUser.getMahasiswa().getClass(), detachedUser.getMahasiswa().getId());
					if (mhsInSession == null) return noAuth();
					s.setReadOnly(mhsInSession, true);
					anggota.setMahasiswa(mhsInSession);
                } else if (detachedUser.getSiswa() != null && detachedUser.getSiswa().getId() != null) {
                    ais.database.model.sekolah.Siswa siswaInSession = (ais.database.model.sekolah.Siswa)
                        s.get(detachedUser.getSiswa().getClass(), detachedUser.getSiswa().getId());
                    if (siswaInSession == null) return noAuth();
                    anggota.setSiswa(siswaInSession);
                } else {
                    return noAuth(); // Entity tidak dapat diidentifikasi
                }
            }

            anggota.setKode(AnggotaKoperasi.generateKodeMemberUnik(s, anggota, anggota.getTanggal()));

            if (!s.getTransaction().isActive()) s.getTransaction().begin();
            s.save(anggota);
            if (user != null) {
                user.setAnggotaKoperasi(anggota);
                s.update(user);
            }
            s.getTransaction().commit();

            JSONObject hasil = new JSONObject();
            hasil.put("status",    "00");
            hasil.put("sudah_ada", false);
            hasil.put("id_member", anggota.getId());
            return hasil;
        } finally { closeSession(s); }
    }

    // ── 2. SALDO (REFRESH) ────────────────────────────────────────────────────
    /**
     * Ambil saldo terkini anggota.
     * Response: { status:"00", data:<saldo_long> }
     */
    public static JSONObject saldo(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception {
        Tbmuser user = ApiUtil.currentUser(json, req);
        if (user == null) return noAuth();

        // Session mini hanya untuk resolusi anggota; KantinHelper.topup membuka session sendiri
        Long anggotaId;
        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            AnggotaKoperasi anggota = resolveAnggotaDb(s, user, true);
            if (anggota == null) return noMember();
            anggotaId = anggota.getId();
        } finally { closeSession(s); }

        JSONObject balReq = new JSONObject();
        balReq.put("id_member", anggotaId.toString());
        JSONObject hasil = new JSONObject();
        KantinHelper.topup(balReq, hasil);
        return hasil;
    }

    // ── 3. DAFTAR TOKO ────────────────────────────────────────────────────────
    /**
     * Semua toko / pedagang aktif.
     * Response: { status:"00", list:[{ id, nama }] }
     */
    public static JSONObject tokoList(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception {
        if (ApiUtil.currentUser(json, req) == null) return noAuth();
        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = s.createSQLQuery(
                "SELECT id, nama FROM koperasi.toko WHERE aktif=true ORDER BY nama ASC"
            ).list();
            JSONArray arr = new JSONArray();
            for (Object[] r : rows) {
                JSONObject o = new JSONObject();
                o.put("id",   r[0]);
                o.put("nama", r[1]);
                arr.put(o);
            }
            JSONObject hasil = new JSONObject();
            hasil.put("list",   arr);
            hasil.put("status", "00");
            return hasil;
        } finally { closeSession(s); }
    }

    // ── 4. CARA BAYAR ─────────────────────────────────────────────────────────
    /**
     * Metode pembayaran yang diizinkan untuk jenis anggota ini.
     * Request: { token, topup_only:"true"|"false" (opsional) }
     * Response biasa: { status:"00", list:[{ id, nama, manual }] }.
     * Untuk topup_only, satu cara bayar dapat dipecah menjadi beberapa kanal:
     * [{ id, nama, manual, channel, nama_channel, biaya_admin }]. Biaya admin
     * berasal dari konfigurasi server dan tidak pernah dipercaya dari klien.
     */
    @SuppressWarnings("unchecked")
    public static JSONObject caraBayar(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception {
        Tbmuser user = ApiUtil.currentUser(json, req);
        if (user == null) return noAuth();
        boolean topupOnly = "true".equalsIgnoreCase(safeStr(json, "topup_only"));

        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            AnggotaKoperasi anggota = resolveAnggotaDb(s, user, true);
            if (anggota == null) return noMember();
            // getJenisAnggotaKoperasi() aman: lazy load dalam session yang sama
            if (anggota.getJenisAnggotaKoperasi() == null)
                return ApiHelperSupport.status("99", "Jenis anggota tidak diketahui");
            String daftarId = anggota.getJenisAnggotaKoperasi()
                .getDaftarCaraPembayaranYangBolehDiPilih();
            org.hibernate.Criteria criteria = s.createCriteria(CaraPembayaranKoperasi.class)
                .add(Restrictions.eq("aktif", Boolean.TRUE))
                .add(Restrictions.eq("online", Boolean.TRUE))
                .addOrder(Order.asc("nama"));
            if (topupOnly) criteria.add(Restrictions.eq("manual", Boolean.FALSE));
            List<CaraPembayaranKoperasi> rows = criteria.list();
            JSONArray arr = new JSONArray();
            for (CaraPembayaranKoperasi cara : rows) {
                if (daftarId == null || daftarId.indexOf("," + cara.getId() + ",") < 0) continue;
                if (!topupOnly) {
                    JSONObject o = new JSONObject();
                    o.put("id", cara.getId());
                    o.put("nama", cara.getNama());
                    o.put("manual", cara.getManual());
                    arr.put(o);
                    continue;
                }
                if (cara.getKanalPembayaran() == null) continue;
                String variable = cara.getKanalPembayaran().getVariableBiayaAdminEsmartlink();
                boolean punyaChannel = false;
                if (variable != null && !variable.trim().isEmpty()) {
                    String[] channels = variable.split(";");
                    for (int i = 0; i < channels.length; i++) {
                        String[] bagian = channels[i].trim().split(":", 3);
                        if (bagian.length == 0 || bagian[0].trim().isEmpty()) continue;
                        JSONObject o = new JSONObject();
                        o.put("id", cara.getId());
                        o.put("nama", cara.getNama());
                        o.put("manual", cara.getManual());
                        o.put("channel", bagian[0].trim());
                        o.put("nama_channel", bagian.length >= 3 && !bagian[2].trim().isEmpty()
                            ? bagian[2].trim() : bagian[0].trim());
                        o.put("biaya_admin", bagian.length >= 2 && Common.isNumber(bagian[1].trim())
                            ? Double.valueOf(bagian[1].trim()) : Double.valueOf(0.0));
                        o.put("gateway", "smartlink");
                        arr.put(o);
                        punyaChannel = true;
                    }
                }
                if (!punyaChannel) {
                    JSONObject o = new JSONObject();
                    o.put("id", cara.getId());
                    o.put("nama", cara.getNama());
                    o.put("manual", cara.getManual());
                    o.put("channel", "");
                    o.put("nama_channel", cara.getNama());
                    o.put("biaya_admin", cara.getKanalPembayaran().getBiayaAdminEsmartlink());
                    o.put("gateway", "smartlink");
                    arr.put(o);
                }
				if (OnlineBmtUtil.isChannelReady(cara, null)) {
                    JSONObject o = new JSONObject();
                    o.put("id", cara.getId());
                    o.put("nama", OnlineBmtUtil.BANK_NAME);
                    o.put("manual", cara.getManual());
                    o.put("channel", "");
                    o.put("nama_channel", OnlineBmtUtil.BANK_NAME);
					o.put("biaya_admin", OnlineBmtUtil.resolveSettings(cara, null)
							.getAdministrationFee());
                    o.put("gateway", OnlineBmtUtil.PARAM_KEY);
                    arr.put(o);
                }
            }
            JSONObject hasil = new JSONObject();
            hasil.put("list",   arr);
            hasil.put("status", "00");
            return hasil;
        } finally { closeSession(s); }
    }

    // ── 4B. BUAT TAGIHAN TOPUP ───────────────────────────────────────────────
    /**
     * Membuat VA/payment-link topup milik anggota yang sedang login. Aksi ini
     * online-only: saldo tidak ditambah di sini, melainkan oleh callback resmi
     * bank/gateway setelah pembayaran sukses.
     */
    public static JSONObject topupBuat(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception {
        Tbmuser user = ApiUtil.currentUser(json, req);
        if (user == null) return noAuth();
        if (!isKonfigAktif("aktifkan_topup_di_anggota"))
            return ApiHelperSupport.status("99", "Fitur topup belum diaktifkan.");

        double nominal = json.optDouble("nominal", 0.0);
        String idCara = safeStr(json, "cara_pembayaran_id");
        String channel = safeStr(json, "channel");
        String gateway = safeStr(json, "gateway");
        boolean onlineBmt = OnlineBmtUtil.PARAM_KEY.equalsIgnoreCase(gateway)
            || OnlineBmtUtil.BANK_NAME.equalsIgnoreCase(gateway);
        if (nominal < 10000.0)
            return ApiHelperSupport.status("99", "Nominal pengisian saldo minimal Rp 10.000.");
        if (idCara.isEmpty() || !Common.isNumber(idCara))
            return ApiHelperSupport.status("99", "Cara pembayaran wajib dipilih.");

        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            AnggotaKoperasi anggota = resolveAnggotaDb(s, user, true);
            if (anggota == null) return noMember();
            if (anggota.getJenisAnggotaKoperasi() == null)
                return ApiHelperSupport.status("99", "Jenis anggota tidak diketahui.");

            CaraPembayaranKoperasi cara = (CaraPembayaranKoperasi) s.get(
                CaraPembayaranKoperasi.class, Long.valueOf(idCara));
            String daftarId = anggota.getJenisAnggotaKoperasi()
                .getDaftarCaraPembayaranYangBolehDiPilih();
            if (cara == null || !Boolean.TRUE.equals(cara.getAktif())
                    || !Boolean.TRUE.equals(cara.getOnline())
                    || !Boolean.FALSE.equals(cara.getManual())
                    || daftarId == null || daftarId.indexOf("," + cara.getId() + ",") < 0
                    || cara.getKanalPembayaran() == null)
                return ApiHelperSupport.status("99", "Cara pembayaran online tidak diizinkan untuk jenis anggota ini.");

			if (onlineBmt && !OnlineBmtUtil.isChannelReady(cara, null))
				return ApiHelperSupport.status("99", "Kanal Online BMT belum aktif atau konfigurasinya belum lengkap.");

            String variable = cara.getKanalPembayaran().getVariableBiayaAdminEsmartlink();
            if (!onlineBmt && variable != null && !variable.trim().isEmpty()) {
                if (channel.isEmpty())
                    return ApiHelperSupport.status("99", "Saluran pembayaran wajib dipilih.");
                boolean channelDitemukan = false;
                String[] channels = variable.split(";");
                for (int i = 0; i < channels.length; i++) {
                    String[] bagian = channels[i].trim().split(":", 3);
                    if (bagian.length > 0 && channel.equalsIgnoreCase(bagian[0].trim())) {
                        channelDitemukan = true;
                        break;
                    }
                }
                if (!channelDitemukan)
                    return ApiHelperSupport.status("99", "Saluran pembayaran tidak terdaftar pada konfigurasi server.");
            }

            JSONObject payload = new JSONObject();
            payload.put("bank", onlineBmt ? OnlineBmtUtil.BANK_NAME
                : (cara.getNama() == null ? "Online" : cara.getNama()));
            payload.put("topup", String.valueOf(nominal));
            payload.put("caraPembayaranKoperasi", String.valueOf(cara.getId()));
            if (!channel.isEmpty()) payload.put("channel", channel);

            JSONObject hasil = TopupHelper.topupAnggotaKoperasi(payload, req, user, anggota, cara);
            hasil.put("id_member", anggota.getId());
            hasil.put("member", anggota.getNama() == null ? "" : anggota.getNama());
            hasil.put("channel", channel);
            hasil.put("gateway", onlineBmt ? OnlineBmtUtil.PARAM_KEY : "smartlink");
            return hasil;
        } finally { closeSession(s); }
    }

    // ── 5. PRODUK LIST (paginasi + pencarian) ────────────────────────────────
    /**
     * Katalog produk per toko, paginasi 10/hal, pencarian nama/kode.
     * Request: { token, id_toko, keyword (opsional), page (def:1), limit (def:10) }
     * Response: { status:"00", list:[{ id, kode, nama, harga, id_toko, nama_toko }], total, page, limit }
     */
    public static JSONObject produkList(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception {
        if (ApiUtil.currentUser(json, req) == null) return noAuth();
        String idTokoStr = safeStr(json, "id_toko");
        if (idTokoStr.isEmpty() || !Common.isNumber(idTokoStr))
            return ApiHelperSupport.status("99", "id_toko diperlukan");

        String keyword = safeStr(json, "keyword");
        int page   = safeInt(json, "page",  1);
        int limit  = safeInt(json, "limit", 10);
        if (page  < 1) page  = 1;
        if (limit < 1) limit = 10;
        int offset = (page - 1) * limit;
        boolean hasKw = !keyword.isEmpty();

        String whereKw = hasKw ? " AND (p.nama ILIKE :kw OR p.kode = :kwExact)" : "";
        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            String countSql = "SELECT COUNT(p.id) FROM koperasi.produk p WHERE p.aktif=true AND p.toko=:toko" + whereKw;
            String dataSql  = "SELECT p.id, p.kode, p.nama, p.hargajual, t.id, t.nama " +
                "FROM koperasi.produk p LEFT JOIN koperasi.toko t ON p.toko=t.id " +
                "WHERE p.aktif=true AND p.toko=:toko" + whereKw + " ORDER BY p.nama ASC LIMIT :lim OFFSET :off";

            org.hibernate.Query cntQ = s.createSQLQuery(countSql).setParameter("toko", Long.parseLong(idTokoStr));
            org.hibernate.Query datQ = s.createSQLQuery(dataSql)
                .setParameter("toko", Long.parseLong(idTokoStr))
                .setParameter("lim",  limit)
                .setParameter("off",  offset);
            if (hasKw) {
                cntQ.setParameter("kw", "%" + keyword + "%").setParameter("kwExact", keyword);
                datQ.setParameter("kw", "%" + keyword + "%").setParameter("kwExact", keyword);
            }

            long total = ((Number) cntQ.uniqueResult()).longValue();
            @SuppressWarnings("unchecked")
            List<Object[]> rows = datQ.list();
            JSONArray arr = new JSONArray();
            for (Object[] r : rows) {
                JSONObject o = new JSONObject();
                o.put("id",        r[0]);
                o.put("kode",      r[1]);
                o.put("nama",      r[2]);
                o.put("harga",     r[3]);
                o.put("id_toko",   r[4]);
                o.put("nama_toko", r[5]);
                arr.put(o);
            }
            JSONObject hasil = new JSONObject();
            hasil.put("list",   arr);
            hasil.put("total",  jsonLong(total));
            hasil.put("page",   page);
            hasil.put("limit",  limit);
            hasil.put("status", "00");
            return hasil;
        } finally { closeSession(s); }
    }

    // ── 6. ATURAN DISKON ──────────────────────────────────────────────────────
    /**
     * Semua aturan diskon aktif. Nilai ini untuk pratinjau klien; keputusan akhir dan
     * perhitungan ulang tetap dilakukan server pada endpoint evaluasi/finalisasi pembayaran.
     * Response: { status:"00", list:[{ id, produk, toko, berlaku_semua_member, jenis_anggota,
     *   tipe_anggota, persentase, maksimal_potongan, nominal, potongan_langsung,
     *   berlaku_per_hari_dan_per_toko, tanggal_mulai, tanggal_selesai }] }
     */
    public static JSONObject aturanDiskon(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception {
        if (ApiUtil.currentUser(json, req) == null) return noAuth();
        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = s.createSQLQuery(
                "SELECT id, produk, toko, berlaku_semua_member, jenis_anggota, tipe_anggota, " +
                "persentase, maksimal_potongan, nominal, potongan_langsung, " +
                "berlaku_per_hari_dan_per_toko, tanggal_mulai, tanggal_selesai, hari_aktif, " +
                "COALESCE(prioritas,100) AS prioritas_nilai, COALESCE(dapat_digabung,false) AS dapat_digabung_nilai, " +
                "COALESCE(dasar_perhitungan,'SETELAH_DISKON') AS dasar_perhitungan_nilai, COALESCE(grup_eksklusif,'') AS grup_eksklusif_nilai " +
                "FROM koperasi.aturan_diskon WHERE aktif=true"
            ).list();
            JSONArray arr = new JSONArray();
            for (Object[] r : rows) {
                JSONObject o = new JSONObject();
                o.put("id",                         r[0]);
                o.put("produk",                     r[1]);
                o.put("toko",                       r[2]);
                o.put("berlaku_semua_member",       r[3]);
                o.put("jenis_anggota",              r[4]);
                o.put("tipe_anggota",               r[5]);
                o.put("persentase",                 r[6]);
                o.put("maksimal_potongan",          r[7]);
                o.put("nominal",                    r[8]);
                o.put("potongan_langsung",          r[9]);
                o.put("berlaku_per_hari_dan_per_toko", r[10]);
                o.put("tanggal_mulai",              r[11] != null ? r[11].toString() : null);
                o.put("tanggal_selesai",            r[12] != null ? r[12].toString() : null);
                o.put("hari_aktif",                 r[13]);
                o.put("prioritas",                  r[14]);
                o.put("dapat_digabung",             r[15]);
                o.put("dasar_perhitungan",          r[16]);
                o.put("grup_eksklusif",             r[17]);
                o.put("sumber",                     "ATURAN");
                arr.put(o);
            }
            @SuppressWarnings("unchecked")
            List<Object[]> groups = s.createSQLQuery(
                "SELECT g.id,d.produk,g.toko,COALESCE(g.berlaku_semua_member,NOT COALESCE(g.khusus_member,false)) AS berlaku_semua_member_nilai, " +
                "g.jenis_anggota,g.tipe_anggota,g.persentase,g.maksimal_potongan,g.nominal, " +
                "COALESCE(g.potongan_langsung,true) AS potongan_langsung_nilai,g.tanggal_mulai,g.tanggal_selesai,g.hari_aktif, " +
                "COALESCE(g.prioritas,100) AS prioritas_nilai,COALESCE(g.dapat_digabung,false) AS dapat_digabung_nilai, " +
                "COALESCE(g.dasar_perhitungan,'SETELAH_DISKON') AS dasar_perhitungan_nilai,COALESCE(g.grup_eksklusif,'') AS grup_eksklusif_nilai, " +
                "COALESCE(g.cashback,0) AS cashback_nilai,COALESCE(g.khusus_member,false) AS khusus_member_nilai, " +
                "COALESCE(g.jenis_member_json,'[]') AS jenis_member_json_nilai,COALESCE(g.tipe_member_json,'[]') AS tipe_member_json_nilai,g.nama_grup " +
                "FROM koperasi.grup_aturan_diskon g " +
                "JOIN koperasi.grup_aturan_diskon_detail d ON d.grup_aturan_diskon=g.id AND COALESCE(d.aktif,true) " +
                "WHERE COALESCE(g.aktif,true)"
            ).list();
            for (Object[] r : groups) {
                JSONObject o = new JSONObject();
                o.put("id", r[0]); o.put("produk", r[1]); o.put("toko", r[2]);
                o.put("berlaku_semua_member", r[3]); o.put("jenis_anggota", r[4]);
                o.put("tipe_anggota", r[5]); o.put("persentase", r[6]);
                o.put("maksimal_potongan", r[7]); o.put("nominal", r[8]);
                o.put("potongan_langsung", r[9]);
                o.put("tanggal_mulai", r[10] != null ? r[10].toString() : null);
                o.put("tanggal_selesai", r[11] != null ? r[11].toString() : null);
                o.put("hari_aktif", r[12]); o.put("prioritas", r[13]);
                o.put("dapat_digabung", r[14]); o.put("dasar_perhitungan", r[15]);
                o.put("grup_eksklusif", r[16]); o.put("cashback", r[17]);
                o.put("khusus_member", r[18]); o.put("jenis_member_json", r[19]);
                o.put("tipe_member_json", r[20]); o.put("nama_grup", r[21]);
                o.put("sumber", "GRUP");
                arr.put(o);
            }
            JSONObject hasil = new JSONObject();
            hasil.put("list",   arr);
            hasil.put("status", "00");
            return hasil;
        } finally { closeSession(s); }
    }

    // ── 7. BAYAR (pembayaran saldo/online) ────────────────────────────────────
    /**
     * Selesaikan pembayaran langsung (potong saldo / metode online non-manual).
     * Request: { token, kodeUnik, idToko, waktu, caraBayar, transaksi:[], keterangan?, mejaKantin? }
     * Response: { status:"00", data:[], pembelianAnggotaKoperasi:<id> }
     */
    public static JSONObject bayar(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception {
        Tbmuser user = ApiUtil.currentUser(json, req);
        if (user == null) return noAuth();

        // Session mini hanya untuk resolusi anggota; KantinHelper.bayar membuka session sendiri
        Long anggotaId;
        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            AnggotaKoperasi anggota = resolveAnggotaDb(s, user, true);
            if (anggota == null) return noMember();
            anggotaId = anggota.getId();
        } finally { closeSession(s); }

        json.put("id_member", anggotaId.toString());
        JSONObject hasil = new JSONObject();
        KantinHelper.bayar(user, json, hasil);
        return hasil;
    }

    // ── 8. DRAFT BAYAR (pembayaran manual/transfer) ───────────────────────────
    /**
     * Simpan pesanan sebagai draft — untuk metode pembayaran manual (transfer, tunai, dll).
     * Request: { token, kodeUnik, idToko, waktu, caraBayar, transaksi:[], keterangan?, mejaKantin? }
     * Response: { status:"00", data:[], pembelianAnggotaKoperasi:<id> }
     */
    public static JSONObject draftBayar(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception {
        Tbmuser user = ApiUtil.currentUser(json, req);
        if (user == null) return noAuth();

        // Session mini hanya untuk resolusi anggota; KantinHelper.draft_bayar membuka session sendiri
        Long anggotaId;
        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            AnggotaKoperasi anggota = resolveAnggotaDb(s, user, true);
            if (anggota == null) return noMember();
            anggotaId = anggota.getId();
        } finally { closeSession(s); }

        json.put("id_member", anggotaId.toString());
        JSONObject hasil = new JSONObject();
        KantinHelper.draft_bayar(user, json, hasil);
        return hasil;
    }

    // ── 9. PESANAN (draft) LIST ───────────────────────────────────────────────
    /**
     * Daftar pesanan/draft yang belum/sudah lunas (paginasi).
     * Request: { token, start_date?, end_date?, keyword?, page?, limit? }
     * Response: { status:"00", list:[{ id, tanggal, cara_bayar, keterangan, pedagang,
     *   total_diskon, total_cashback, total_biaya, lunas }], total, page }
     */
    public static JSONObject pesananList(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception {
        Tbmuser user = ApiUtil.currentUser(json, req);
        if (user == null) return noAuth();

        String startDate = safeStr(json, "start_date");
        String endDate   = safeStr(json, "end_date");
        String keyword   = safeStr(json, "keyword");
        int page  = safeInt(json, "page",  1);
        int limit = safeInt(json, "limit", 10);
        if (page < 1) page = 1;
        int offset = (page - 1) * limit;
        boolean hasDate = !startDate.isEmpty() && !endDate.isEmpty();
        boolean hasKw   = !keyword.isEmpty();

        String join  = " LEFT JOIN koperasi.toko b ON a.toko=b.id LEFT JOIN koperasi.cara_pembayaran_koperasi d ON d.id=a.cara_pembayaran_koperasi";
        String where = " WHERE a.anggota_koperasi=:m"
            + (hasDate ? " AND DATE(a.tanggal_pembayaran) BETWEEN CAST(:sd AS date) AND CAST(:ed AS date)" : "")
            + (hasKw   ? " AND (b.nama ILIKE :kw OR a.kode ILIKE :kw)" : "");

        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            // Resolusi anggota di dalam session yang sama
            AnggotaKoperasi anggota = resolveAnggotaDb(s, user, true);
            if (anggota == null) return noMember();

            String countSql = "SELECT COUNT(*) FROM koperasi.draft_pembelian_anggota_koperasi a" + join + where;
            String dataSql  = "SELECT a.id, TO_CHAR(a.tanggal_pembayaran,'DD Mon YYYY HH24:MI'), " +
                "d.nama, a.keterangan, b.nama, COALESCE(a.total_diskon,0), COALESCE(a.totalcashback,0), COALESCE(a.total_biaya,0), a.lunas " +
                "FROM koperasi.draft_pembelian_anggota_koperasi a" + join + where +
                " ORDER BY a.id DESC LIMIT :lim OFFSET :off";

            org.hibernate.Query cntQ = s.createSQLQuery(countSql).setParameter("m", anggota.getId());
            org.hibernate.Query datQ = s.createSQLQuery(dataSql)
                .setParameter("m", anggota.getId()).setParameter("lim", limit).setParameter("off", offset);
            if (hasDate) { cntQ.setParameter("sd", startDate).setParameter("ed", endDate); datQ.setParameter("sd", startDate).setParameter("ed", endDate); }
            if (hasKw)   { cntQ.setParameter("kw", "%" + keyword + "%"); datQ.setParameter("kw", "%" + keyword + "%"); }

            long total = ((Number) cntQ.uniqueResult()).longValue();
            @SuppressWarnings("unchecked")
            List<Object[]> rows = datQ.list();
            JSONArray arr = new JSONArray();
            for (Object[] r : rows) {
                JSONObject o = new JSONObject();
                o.put("id",             r[0]);
                o.put("tanggal",        r[1]);
                o.put("cara_bayar",     r[2]);
                o.put("keterangan",     r[3]);
                o.put("pedagang",       r[4]);
                o.put("total_diskon",   r[5]);
                o.put("total_cashback", r[6]);
                o.put("total_biaya",    r[7]);
                o.put("lunas",          r[8] != null);
                arr.put(o);
            }
            JSONObject hasil = new JSONObject();
            hasil.put("list",   arr);
            hasil.put("total",  jsonLong(total));
            hasil.put("page",   page);
            hasil.put("status", "00");
            return hasil;
        } finally { closeSession(s); }
    }

    // ── 10. BATAL PESANAN ─────────────────────────────────────────────────────
    /**
     * Batalkan / hapus pesanan draft yang belum lunas.
     * Request: { token, id }
     * Response: { status:"00", description:"..." }
     */
    @SuppressWarnings("deprecation")
    public static JSONObject batalPesanan(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception {
        Tbmuser user = ApiUtil.currentUser(json, req);
        if (user == null) return noAuth();

        String idStr = safeStr(json, "id");
        if (idStr.isEmpty() || !Common.isNumber(idStr))
            return ApiHelperSupport.status("99", "id pesanan diperlukan");

        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            // Resolusi anggota di dalam session yang sama agar .getAnggotaKoperasi() pada draft bisa dibandingkan
            AnggotaKoperasi anggota = resolveAnggotaDb(s, user, true);
            if (anggota == null) return noMember();

            DraftPembelianAnggotaKoperasi draft = (DraftPembelianAnggotaKoperasi)
                s.createCriteria(DraftPembelianAnggotaKoperasi.class)
                    .add(Restrictions.idEq(Long.parseLong(idStr)))
                    .uniqueResult();
            if (draft == null)
                return ApiHelperSupport.status("99", "Pesanan tidak ditemukan");
            if (draft.getLunas() != null)
                return ApiHelperSupport.status("99", "Pesanan sudah lunas, tidak dapat dibatalkan");
            // draft.getAnggotaKoperasi() aman: lazy load dalam session yang sama
            if (draft.getAnggotaKoperasi() == null || !draft.getAnggotaKoperasi().getId().equals(anggota.getId()))
                return ApiHelperSupport.status("97", "Akses ditolak");

            // Delete child rows dan draft dalam satu transaksi
            s.getTransaction().begin();
            s.createSQLQuery("DELETE FROM koperasi.draft_pembelian WHERE draft_pembelian_anggota_koperasi=" + idStr).executeUpdate();
            s.delete(draft);
            s.getTransaction().commit();
            return ApiHelperSupport.status("00", "Pesanan berhasil dibatalkan");
        } finally { closeSession(s); }
    }

    // ── 10b. CEK MEJA (dari QR) ─────────────────────────────
    /**
     * Terjemahkan kode QR meja menjadi id + nama meja.
     *
     * <p>Versi JSP melakukan ini dgn SQL mentah dari sisi klien
     * ({@code action:"sql"} ke servlet /Data). Jalur itu butuh sesi web dan
     * menerima SQL apa adanya dari browser, jadi TIDAK dipakai aplikasi
     * mobile; aksi ini menyediakan hasil yang sama lewat query berparameter.</p>
     *
     * Request: { token, kode }
     * Response: { status:"00", data:{ id, nama, kode } }
     */
    public static JSONObject mejaCek(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception {
        if (ApiUtil.currentUser(json, req) == null) return noAuth();
        String kode = safeStr(json, "kode");
        if (kode.isEmpty()) return ApiHelperSupport.status("99", "kode meja diperlukan");

        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = s.createSQLQuery(
                "SELECT id, nama, kode FROM koperasi.meja_kantin "
                    + "WHERE kode = :kode AND aktif = true LIMIT 1"
            ).setParameter("kode", kode).list();
            if (rows.isEmpty()) {
                return ApiHelperSupport.status("99", "QR meja tidak dikenali atau meja tidak aktif");
            }
            Object[] r = rows.get(0);
            JSONObject data = new JSONObject();
            data.put("id",   r[0]);
            data.put("nama", r[1]);
            data.put("kode", r[2]);
            JSONObject hasil = new JSONObject();
            hasil.put("data", data);
            hasil.put("status", "00");
            return hasil;
        } finally { closeSession(s); }
    }

    // ── 11. TRANSAKSI LIST ────────────────────────────────────────────────────
    /**
     * Riwayat transaksi yang sudah lunas (paginasi).
     * Response: { status:"00", list:[{ id, tanggal, nama_pembeli, pedagang, cara_bayar,
     *   total_diskon, total_cashback, total_biaya }], total, page }
     */
    public static JSONObject transaksiList(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception {
        Tbmuser user = ApiUtil.currentUser(json, req);
        if (user == null) return noAuth();

        String startDate = safeStr(json, "start_date");
        String endDate   = safeStr(json, "end_date");
        String keyword   = safeStr(json, "keyword");
        int page  = safeInt(json, "page",  1);
        int limit = safeInt(json, "limit", 10);
        if (page < 1) page = 1;
        int offset = (page - 1) * limit;
        boolean hasDate = !startDate.isEmpty() && !endDate.isEmpty();
        boolean hasKw   = !keyword.isEmpty();

        String join  = " LEFT JOIN koperasi.toko b ON a.toko=b.id LEFT JOIN koperasi.anggota_koperasi c ON c.id=a.anggota_koperasi LEFT JOIN koperasi.cara_pembayaran_koperasi d ON d.id=a.cara_pembayaran_koperasi";
        String where = " WHERE a.anggota_koperasi=:m"
            + (hasDate ? " AND DATE(a.tanggal_pembayaran) BETWEEN CAST(:sd AS date) AND CAST(:ed AS date)" : "")
            + (hasKw   ? " AND b.nama ILIKE :kw" : "");

        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            // Resolusi anggota di dalam session yang sama
            AnggotaKoperasi anggota = resolveAnggotaDb(s, user, true);
            if (anggota == null) return noMember();

            String countSql = "SELECT COUNT(*) FROM koperasi.pembelian_anggota_koperasi a" + join + where;
            String dataSql  = "SELECT a.id, TO_CHAR(a.tanggal_pembayaran,'DD Mon YYYY HH24:MI'), " +
                "c.nama, b.nama, d.nama, COALESCE(a.total_diskon,0), COALESCE(a.totalcashback,0), COALESCE(a.total_biaya,0) " +
                "FROM koperasi.pembelian_anggota_koperasi a" + join + where +
                " ORDER BY a.tanggal_pembayaran DESC, a.id DESC LIMIT :lim OFFSET :off";

            org.hibernate.Query cntQ = s.createSQLQuery(countSql).setParameter("m", anggota.getId());
            org.hibernate.Query datQ = s.createSQLQuery(dataSql)
                .setParameter("m", anggota.getId()).setParameter("lim", limit).setParameter("off", offset);
            if (hasDate) { cntQ.setParameter("sd", startDate).setParameter("ed", endDate); datQ.setParameter("sd", startDate).setParameter("ed", endDate); }
            if (hasKw)   { cntQ.setParameter("kw", "%" + keyword + "%"); datQ.setParameter("kw", "%" + keyword + "%"); }

            long total = ((Number) cntQ.uniqueResult()).longValue();
            @SuppressWarnings("unchecked")
            List<Object[]> rows = datQ.list();
            JSONArray arr = new JSONArray();
            for (Object[] r : rows) {
                JSONObject o = new JSONObject();
                o.put("id",             r[0]);
                o.put("tanggal",        r[1]);
                o.put("nama_pembeli",   r[2]);
                o.put("pedagang",       r[3]);
                o.put("cara_bayar",     r[4]);
                o.put("total_diskon",   r[5]);
                o.put("total_cashback", r[6]);
                o.put("total_biaya",    r[7]);
                arr.put(o);
            }
            JSONObject hasil = new JSONObject();
            hasil.put("list",   arr);
            hasil.put("total",  jsonLong(total));
            hasil.put("page",   page);
            hasil.put("status", "00");
            return hasil;
        } finally { closeSession(s); }
    }

    // ── 12. TRANSAKSI DETAIL ──────────────────────────────────────────────────
    /**
     * Detail item-item dari satu transaksi (untuk cetak struk).
     * Request: { token, id_transaksi }
     * Response: { status:"00", list:[{ nama, harga, jumlah, diskon, cashback }] }
     */
    public static JSONObject transaksiDetail(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception {
        Tbmuser user = ApiUtil.currentUser(json, req);
        if (user == null) return noAuth();

        String idStr = safeStr(json, "id_transaksi");
        if (idStr.isEmpty() || !Common.isNumber(idStr))
            return ApiHelperSupport.status("99", "id_transaksi diperlukan");

        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            // Validasi bahwa user adalah anggota koperasi
            if (resolveAnggotaDb(s, user, true) == null) return noMember();

            @SuppressWarnings("unchecked")
            List<Object[]> rows = s.createSQLQuery(
                "SELECT c.nama, a.hargajual, a.qty, COALESCE(a.diskon,0), COALESCE(a.cashback,0) " +
                "FROM koperasi.pembelian a INNER JOIN koperasi.produk c ON c.id=a.produk " +
                "WHERE a.pembelian_anggota_koperasi=:id"
            ).setParameter("id", Long.parseLong(idStr)).list();
            JSONArray arr = new JSONArray();
            for (Object[] r : rows) {
                JSONObject o = new JSONObject();
                o.put("nama",     r[0]);
                o.put("harga",    r[1]);
                o.put("jumlah",   r[2]);
                o.put("diskon",   r[3]);
                o.put("cashback", r[4]);
                arr.put(o);
            }
            JSONObject hasil = new JSONObject();
            hasil.put("list",   arr);
            hasil.put("status", "00");
            return hasil;
        } finally { closeSession(s); }
    }

    // ── 13. BARANG DIBELI LIST ────────────────────────────────────────────────
    /**
     * Riwayat barang-barang yang dibeli (paginasi).
     * Response: { status:"00", list:[{ waktu, namabarang, member, pedagang, jenismember, carabayar, qty, total }], total, page }
     */
    public static JSONObject barangList(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception {
        Tbmuser user = ApiUtil.currentUser(json, req);
        if (user == null) return noAuth();

        String startDate = safeStr(json, "start_date");
        String endDate   = safeStr(json, "end_date");
        String keyword   = safeStr(json, "keyword");
        int page  = safeInt(json, "page",  1);
        int limit = safeInt(json, "limit", 10);
        if (page < 1) page = 1;
        int offset = (page - 1) * limit;
        boolean hasDate = !startDate.isEmpty() && !endDate.isEmpty();
        boolean hasKw   = !keyword.isEmpty();

        String join  = " INNER JOIN koperasi.toko b ON a.toko=b.id AND b.aktif=true INNER JOIN koperasi.produk c ON c.id=a.produk";
        String where = " WHERE a.anggota_koperasi=:m"
            + (hasDate ? " AND DATE(a.waktu) BETWEEN CAST(:sd AS date) AND CAST(:ed AS date)" : "")
            + (hasKw   ? " AND (c.nama ILIKE :kw OR c.kode ILIKE :kw OR b.nama ILIKE :kw)" : "");

        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            // Resolusi anggota di dalam session yang sama
            AnggotaKoperasi anggota = resolveAnggotaDb(s, user, true);
            if (anggota == null) return noMember();

            String countSql = "SELECT COUNT(*) FROM koperasi.pembelian a" + join + where;
            String dataSql  = "SELECT TO_CHAR(a.waktu,'DD Mon YYYY HH24:MI'), c.nama, a.member, b.nama, a.jenismember, a.carabayar, a.qty, a.total " +
                "FROM koperasi.pembelian a" + join + where + " ORDER BY a.waktu DESC, a.id DESC LIMIT :lim OFFSET :off";

            org.hibernate.Query cntQ = s.createSQLQuery(countSql).setParameter("m", anggota.getId());
            org.hibernate.Query datQ = s.createSQLQuery(dataSql)
                .setParameter("m", anggota.getId()).setParameter("lim", limit).setParameter("off", offset);
            if (hasDate) { cntQ.setParameter("sd", startDate).setParameter("ed", endDate); datQ.setParameter("sd", startDate).setParameter("ed", endDate); }
            if (hasKw)   { cntQ.setParameter("kw", "%" + keyword + "%"); datQ.setParameter("kw", "%" + keyword + "%"); }

            long total = ((Number) cntQ.uniqueResult()).longValue();
            @SuppressWarnings("unchecked")
            List<Object[]> rows = datQ.list();
            JSONArray arr = new JSONArray();
            for (Object[] r : rows) {
                JSONObject o = new JSONObject();
                o.put("waktu",       r[0]);
                o.put("namabarang",  r[1]);
                o.put("member",      r[2]);
                o.put("pedagang",    r[3]);
                o.put("jenismember", r[4]);
                o.put("carabayar",   r[5]);
                o.put("qty",         r[6]);
                o.put("total",       r[7]);
                arr.put(o);
            }
            JSONObject hasil = new JSONObject();
            hasil.put("list",   arr);
            hasil.put("total",  jsonLong(total));
            hasil.put("page",   page);
            hasil.put("status", "00");
            return hasil;
        } finally { closeSession(s); }
    }

    // ── 14. VA LIST ───────────────────────────────────────────────────────────
    /**
     * Daftar Virtual Account / tagihan topup (paginasi).
     * Response: { status:"00", list:[{ id, bank, kode, total, keterangan, link, batas_waktu,
     *   status_bayar:"LUNAS"|"KEDALUWARSA"|"MENUNGGU" }], total, page }
     */
    public static JSONObject vaList(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception {
        Tbmuser user = ApiUtil.currentUser(json, req);
        if (user == null) return noAuth();

        String startDate = safeStr(json, "start_date");
        String endDate   = safeStr(json, "end_date");
        String keyword   = safeStr(json, "keyword");
        int page  = safeInt(json, "page",  1);
        int limit = safeInt(json, "limit", 10);
        if (page < 1) page = 1;
        int offset = (page - 1) * limit;
        boolean hasDate = !startDate.isEmpty() && !endDate.isEmpty();
        boolean hasKw   = !keyword.isEmpty();

        String where = " WHERE anggota_koperasi=:m"
            + (hasDate ? " AND DATE(kadaluarsa) BETWEEN CAST(:sd AS date) AND CAST(:ed AS date)" : "")
            + (hasKw   ? " AND (kode ILIKE :kw OR channel ILIKE :kw OR keterangan ILIKE :kw)" : "");

        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            // Resolusi anggota di dalam session yang sama
            AnggotaKoperasi anggota = resolveAnggotaDb(s, user, true);
            if (anggota == null) return noMember();

            String countSql = "SELECT COUNT(id) FROM public.virtual_account_bank" + where;
            String dataSql  = "SELECT id, channel, kode, total, keterangan, link, " +
                "TO_CHAR(kadaluarsawaktu,'DD Mon YYYY HH24:MI'), " +
                "CASE WHEN waktubayar IS NOT NULL THEN 'LUNAS' WHEN kadaluarsawaktu < NOW() THEN 'KEDALUWARSA' ELSE 'MENUNGGU' END " +
                "FROM public.virtual_account_bank" + where + " ORDER BY id DESC LIMIT :lim OFFSET :off";

            org.hibernate.Query cntQ = s.createSQLQuery(countSql).setParameter("m", anggota.getId());
            org.hibernate.Query datQ = s.createSQLQuery(dataSql)
                .setParameter("m", anggota.getId()).setParameter("lim", limit).setParameter("off", offset);
            if (hasDate) { cntQ.setParameter("sd", startDate).setParameter("ed", endDate); datQ.setParameter("sd", startDate).setParameter("ed", endDate); }
            if (hasKw)   { cntQ.setParameter("kw", "%" + keyword + "%"); datQ.setParameter("kw", "%" + keyword + "%"); }

            long total = ((Number) cntQ.uniqueResult()).longValue();
            @SuppressWarnings("unchecked")
            List<Object[]> rows = datQ.list();
            JSONArray arr = new JSONArray();
            for (Object[] r : rows) {
                JSONObject o = new JSONObject();
                o.put("id",          r[0]);
                o.put("bank",        r[1]);
                o.put("kode",        r[2]);
                o.put("total",       r[3]);
                o.put("keterangan",  r[4]);
                o.put("link",        r[5]);
                o.put("batas_waktu", r[6]);
                o.put("status_bayar",r[7]);
                arr.put(o);
            }
            JSONObject hasil = new JSONObject();
            hasil.put("list",   arr);
            hasil.put("total",  jsonLong(total));
            hasil.put("page",   page);
            hasil.put("status", "00");
            return hasil;
        } finally { closeSession(s); }
    }

    // ── 15. DASHBOARD ─────────────────────────────────────────────────────────
    /**
     * Ringkasan statistik pembelian: total transaksi, pengeluaran, hemat, topup,
     * tren 6 bulan, dan distribusi toko favorit.
     * Response: { status:"00", data:{ jml_trx, total_pengeluaran, total_hemat, total_topup,
     *   trend:[{ bulan_label, bulan_urut, total_nominal }],
     *   toko_favorit:[{ nama_toko, total_nominal }] } }
     */
    public static JSONObject dashboard(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception {
        Tbmuser user = ApiUtil.currentUser(json, req);
        if (user == null) return noAuth();

        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            // Resolusi anggota di dalam session yang sama
            AnggotaKoperasi anggota = resolveAnggotaDb(s, user, true);
            if (anggota == null) return noMember();

            // KE-FIX (Bad value for type double : "Toko Al Bahjah"): autodiscovery tipe kolom
            // Hibernate untuk native SQLQuery (uniqueResult()/list()) sempat memetakan kolom
            // teks (mis. COALESCE(t.nama,'Koperasi Utama')) sebagai double, melempar
            // org.postgresql.util.PSQLException: Bad value for type double. Baca lewat JDBC
            // PreparedStatement/ResultSet.getObject() (pola yang sudah dipakai
            // DashboardKantinAction.rows() utk bug yang sama) agar tipe kolom diambil apa
            // adanya, bukan ditebak Hibernate.
            java.sql.Connection conn = s.connection();

            Object[] summary = null;
            java.sql.PreparedStatement psSummary = null;
            java.sql.ResultSet rsSummary = null;
            try {
                psSummary = conn.prepareStatement(
                    "SELECT COUNT(id), COALESCE(SUM(total_biaya),0), " +
                    "COALESCE(SUM(COALESCE(total_diskon,0)+COALESCE(totalcashback,0)),0) " +
                    "FROM koperasi.pembelian_anggota_koperasi WHERE anggota_koperasi=?");
                psSummary.setLong(1, anggota.getId());
                rsSummary = psSummary.executeQuery();
                if (rsSummary.next()) {
                    summary = new Object[] { rsSummary.getObject(1), rsSummary.getObject(2), rsSummary.getObject(3) };
                }
            } finally {
                try { if (rsSummary != null) rsSummary.close(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinMemberApi.java:dashboard-summary-rs"); }
                try { if (psSummary != null) psSummary.close(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinMemberApi.java:dashboard-summary-ps"); }
            }

            Object topupRow = null;
            java.sql.PreparedStatement psTopup = null;
            java.sql.ResultSet rsTopup = null;
            try {
                psTopup = conn.prepareStatement("SELECT COALESCE(SUM(nominal),0) FROM public.deposit WHERE anggota_koperasi=?");
                psTopup.setLong(1, anggota.getId());
                rsTopup = psTopup.executeQuery();
                if (rsTopup.next()) {
                    topupRow = rsTopup.getObject(1);
                }
            } finally {
                try { if (rsTopup != null) rsTopup.close(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinMemberApi.java:dashboard-topup-rs"); }
                try { if (psTopup != null) psTopup.close(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinMemberApi.java:dashboard-topup-ps"); }
            }

            List<Object[]> trend = new java.util.ArrayList<Object[]>();
            java.sql.PreparedStatement psTrend = null;
            java.sql.ResultSet rsTrend = null;
            try {
                psTrend = conn.prepareStatement(
                    "SELECT TO_CHAR(tanggal_pembayaran,'Mon YYYY'), TO_CHAR(tanggal_pembayaran,'YYYY-MM'), COALESCE(SUM(total_biaya),0) " +
                    "FROM koperasi.pembelian_anggota_koperasi WHERE anggota_koperasi=? " +
                    "GROUP BY TO_CHAR(tanggal_pembayaran,'Mon YYYY'), TO_CHAR(tanggal_pembayaran,'YYYY-MM') " +
                    "ORDER BY 2 ASC LIMIT 6");
                psTrend.setLong(1, anggota.getId());
                rsTrend = psTrend.executeQuery();
                while (rsTrend.next()) {
                    trend.add(new Object[] { rsTrend.getObject(1), rsTrend.getObject(2), rsTrend.getObject(3) });
                }
            } finally {
                try { if (rsTrend != null) rsTrend.close(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinMemberApi.java:dashboard-trend-rs"); }
                try { if (psTrend != null) psTrend.close(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinMemberApi.java:dashboard-trend-ps"); }
            }

            List<Object[]> tokoFav = new java.util.ArrayList<Object[]>();
            java.sql.PreparedStatement psToko = null;
            java.sql.ResultSet rsToko = null;
            try {
                psToko = conn.prepareStatement(
                    "SELECT COALESCE(t.nama,'Koperasi Utama'), COALESCE(SUM(a.total_biaya),0) " +
                    "FROM koperasi.pembelian_anggota_koperasi a LEFT JOIN koperasi.toko t ON a.toko=t.id " +
                    "WHERE a.anggota_koperasi=? GROUP BY t.nama ORDER BY 2 DESC LIMIT 5");
                psToko.setLong(1, anggota.getId());
                rsToko = psToko.executeQuery();
                while (rsToko.next()) {
                    tokoFav.add(new Object[] { rsToko.getObject(1), rsToko.getObject(2) });
                }
            } finally {
                try { if (rsToko != null) rsToko.close(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinMemberApi.java:dashboard-toko-rs"); }
                try { if (psToko != null) psToko.close(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/servlet/api/KantinMemberApi.java:dashboard-toko-ps"); }
            }

            JSONObject data = new JSONObject();
            if (summary != null) {
                data.put("jml_trx",           jsonLong(summary[0] != null ? ((Number)summary[0]).longValue() : 0L));
                data.put("total_pengeluaran", jsonLong(summary[1] != null ? ((Number)summary[1]).longValue() : 0L));
                data.put("total_hemat",       jsonLong(summary[2] != null ? ((Number)summary[2]).longValue() : 0L));
            }
            data.put("total_topup", jsonLong(topupRow != null ? ((Number)topupRow).longValue() : 0L));

            JSONArray trendArr = new JSONArray();
            for (Object[] r : trend) {
                JSONObject o = new JSONObject();
                o.put("bulan_label",   r[0]);
                o.put("bulan_urut",    r[1]);
                o.put("total_nominal", jsonLong(r[2] != null ? ((Number)r[2]).longValue() : 0L));
                trendArr.put(o);
            }
            data.put("trend", trendArr);

            JSONArray tokoArr = new JSONArray();
            for (Object[] r : tokoFav) {
                JSONObject o = new JSONObject();
                o.put("nama_toko",     r[0]);
                o.put("total_nominal", jsonLong(r[1] != null ? ((Number)r[1]).longValue() : 0L));
                tokoArr.put(o);
            }
            data.put("toko_favorit", tokoArr);

            JSONObject hasil = new JSONObject();
            hasil.put("data",   data);
            hasil.put("status", "00");
            return hasil;
        } finally { closeSession(s); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PEDAGANG / KASIR ENDPOINTS
    // ─────────────────────────────────────────────────────────────────────────

    /** Helper: load Tbmuser dalam session dan return Pedagang-nya. Null jika bukan pedagang. */
    @SuppressWarnings("deprecation")
    private static Pedagang resolvePedagang(Session s, Tbmuser detachedUser) {
        if (detachedUser == null || detachedUser.getUserId() == null) return null;
        Tbmuser user = (Tbmuser) s.get(Tbmuser.class, detachedUser.getUserId());
        if (user == null) return null;
        return user.getPedagang();
    }

    private static JSONObject noPedagang() {
        return ApiHelperSupport.status("99", "Anda bukan pedagang/kasir terdaftar");
    }

    // ── P1. INFO TOKO ──────────────────────────────────────────────────────────
    /**
     * Info toko untuk pedagang yang sedang login.
     * Response: { status:"00", data:{ id_toko, nama_toko, supervisor } }
     */
    @SuppressWarnings("deprecation")
    public static JSONObject pedagangInfo(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception {
        Tbmuser detachedUser = ApiUtil.currentUser(json, req);
        if (detachedUser == null) return noAuth();

        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            Pedagang pedagang = resolvePedagang(s, detachedUser);
            if (pedagang == null) return noPedagang();

            Toko toko = pedagang.getToko();
            if (toko == null) return ApiHelperSupport.status("99", "Toko pedagang tidak ditemukan");

            JSONObject data = new JSONObject();
            data.put("id_toko",   toko.getId().toString());
            data.put("nama_toko", toko.getNama());
            data.put("supervisor", Boolean.TRUE.equals(pedagang.getSupervisor()));

            JSONObject hasil = new JSONObject();
            hasil.put("status", "00");
            hasil.put("data",   data);
            return hasil;
        } finally { closeSession(s); }
    }

    // ── P2. PESANAN MASUK (draft belum lunas untuk toko ini) ──────────────────
    /**
     * Daftar pesanan online yang belum diproses kasir.
     * Request: { token, page (def:1) }
     * Response: { status:"00", list:[{ id, kode, nama_member, keterangan,
     *   cara_bayar, total_biaya, total_diskon, dari_pembeli, items:[...] }] }
     */
    @SuppressWarnings({"deprecation", "unchecked"})
    public static JSONObject pedagangPesananList(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception {
        Tbmuser detachedUser = ApiUtil.currentUser(json, req);
        if (detachedUser == null) return noAuth();

        int page = safeInt(json, "page", 1);
        if (page < 1) page = 1;
        int limit = 20, offset = (page - 1) * limit;

        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            Pedagang pedagang = resolvePedagang(s, detachedUser);
            if (pedagang == null) return noPedagang();
            Toko toko = pedagang.getToko();
            if (toko == null) return ApiHelperSupport.status("99", "Toko tidak ditemukan");

            List<Object[]> rows = s.createSQLQuery(
                "SELECT d.id, d.kode, " +
                "  COALESCE(ak.userid, 'Pelanggan Umum') AS nama_member, " +
                "  COALESCE(d.keterangan,'') AS keterangan, " +
                "  COALESCE(cp.nama,'-') AS cara_bayar, " +
                "  COALESCE(d.total_biaya,0), COALESCE(d.total_diskon,0), " +
                "  (d.anggota_koperasi IS NOT NULL) AS dari_pembeli " +
                "FROM koperasi.draft_pembelian_anggota_koperasi d " +
                "LEFT JOIN koperasi.anggota_koperasi ak ON d.anggota_koperasi=ak.id " +
                "LEFT JOIN koperasi.cara_pembayaran_koperasi cp ON d.cara_pembayaran_koperasi=cp.id " +
                "WHERE d.toko=:toko AND d.lunas IS NULL " +
                "ORDER BY d.id DESC LIMIT :lim OFFSET :off"
            ).setParameter("toko", toko.getId()).setParameter("lim", limit).setParameter("off", offset).list();

            JSONArray list = new JSONArray();
            for (Object[] r : rows) {
                long draftId = ((Number) r[0]).longValue();

                List<Object[]> items = s.createSQLQuery(
                    "SELECT COALESCE(p.nama,'Produk') AS nama, COALESCE(dp.harga_satuan,0), " +
                    "  COALESCE(dp.qty,1), COALESCE(dp.diskon,0), COALESCE(dp.total,0) " +
                    "FROM koperasi.draft_pembelian dp " +
                    "LEFT JOIN koperasi.produk p ON dp.produk=p.id " +
                    "WHERE dp.draft_pembelian_anggota_koperasi=:did AND dp.aktif=true"
                ).setParameter("did", draftId).list();

                JSONArray itemArr = new JSONArray();
                for (Object[] ir : items) {
                    JSONObject io = new JSONObject();
                    io.put("nama",   ir[0]);
                    io.put("harga",  jsonLong(ir[1] != null ? ((Number) ir[1]).longValue() : 0L));
                    io.put("jumlah", ir[2] != null ? ((Number) ir[2]).intValue()  : 0);
                    io.put("diskon", jsonLong(ir[3] != null ? ((Number) ir[3]).longValue() : 0L));
                    io.put("total",  jsonLong(ir[4] != null ? ((Number) ir[4]).longValue() : 0L));
                    itemArr.put(io);
                }

                JSONObject o = new JSONObject();
                o.put("id",           r[0]);
                o.put("kode",         r[1]);
                o.put("nama_member",  r[2]);
                o.put("keterangan",   r[3]);
                o.put("cara_bayar",   r[4]);
                o.put("total_biaya",  jsonLong(r[5] != null ? ((Number) r[5]).longValue() : 0L));
                o.put("total_diskon", jsonLong(r[6] != null ? ((Number) r[6]).longValue() : 0L));
                o.put("dari_pembeli", r[7] != null && ((Boolean) r[7]));
                o.put("items",        itemArr);
                list.put(o);
            }

            JSONObject hasil = new JSONObject();
            hasil.put("status", "00");
            hasil.put("list",   list);
            return hasil;
        } finally { closeSession(s); }
    }

    // ── P3. PROSES PESANAN (finalize draft → PembelianAnggotaKoperasi) ─────────
    /**
     * Kasir menyelesaikan pesanan online: memanggil KantinHelper.bayar() dengan
     * id draft yang sudah ada sehingga draft di-link ke transaksi final.
     * Request: { token, id_draft, cara_bayar (id CaraPembayaranKoperasi), keterangan? }
     * Response: seperti kantin_bayar
     */
    @SuppressWarnings("deprecation")
    public static JSONObject pedagangProsesPesanan(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception {
        Tbmuser detachedUser = ApiUtil.currentUser(json, req);
        if (detachedUser == null) return noAuth();

        String idDraftStr  = safeStr(json, "id_draft");
        String caraBayarId = safeStr(json, "cara_bayar");
        String keterangan  = safeStr(json, "keterangan");
        if (idDraftStr.isEmpty())  return ApiHelperSupport.status("99", "id_draft diperlukan");
        if (caraBayarId.isEmpty()) return ApiHelperSupport.status("99", "cara_bayar diperlukan");

        Tbmuser userForBayar = null;
        JSONObject payload   = new JSONObject();

        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            Pedagang pedagang = resolvePedagang(s, detachedUser);
            if (pedagang == null) return noPedagang();
            Toko toko = pedagang.getToko();
            if (toko == null) return ApiHelperSupport.status("99", "Toko tidak ditemukan");

            DraftPembelianAnggotaKoperasi draft = (DraftPembelianAnggotaKoperasi)
                s.get(DraftPembelianAnggotaKoperasi.class, Long.parseLong(idDraftStr));
            if (draft == null)
                return ApiHelperSupport.status("99", "Pesanan tidak ditemukan");
            if (draft.getLunas() != null)
                return ApiHelperSupport.status("99", "Pesanan sudah diproses sebelumnya");
            if (draft.getToko() == null || !toko.getId().equals(draft.getToko().getId()))
                return ApiHelperSupport.status("99", "Pesanan bukan milik toko ini");

            // Rebuild transaksi dari draft items
            @SuppressWarnings("unchecked")
            java.util.List<Object[]> items = s.createSQLQuery(
                "SELECT COALESCE(p.id,0), COALESCE(p.kode,''), COALESCE(p.nama,'Produk'), " +
                "  COALESCE(dp.harga_satuan,0), COALESCE(dp.qty,1), " +
                "  COALESCE(dp.diskon,0), COALESCE(dp.cashback,0) " +
                "FROM koperasi.draft_pembelian dp " +
                "LEFT JOIN koperasi.produk p ON dp.produk=p.id " +
                "WHERE dp.draft_pembelian_anggota_koperasi=:did AND dp.aktif=true"
            ).setParameter("did", draft.getId()).list();

            JSONArray transaksi = new JSONArray();
            for (Object[] r : items) {
                JSONObject t = new JSONObject();
                t.put("id",       r[0]);
                t.put("kode",     r[1]);
                t.put("nama",     r[2]);
                t.put("harga",    jsonLong(r[3] != null ? ((Number) r[3]).longValue() : 0L));
                t.put("jumlah",   r[4] != null ? ((Number) r[4]).doubleValue() : 1.0);
                t.put("diskon",   jsonLong(r[5] != null ? ((Number) r[5]).longValue() : 0L));
                t.put("cashback", jsonLong(r[6] != null ? ((Number) r[6]).longValue() : 0L));
                transaksi.put(t);
            }

            payload.put("kodeUnik",                      "KASIR-" + System.currentTimeMillis());
            payload.put("idToko",                        toko.getId().toString());
            payload.put("waktu",                         new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new java.util.Date()));
            payload.put("caraBayar",                     caraBayarId);
            payload.put("transaksi",                     transaksi);
            payload.put("draftPembelianAnggotaKoperasi", draft.getId().toString());
            if (!keterangan.isEmpty()) payload.put("keterangan", keterangan);
            if (draft.getAnggotaKoperasi() != null)
                payload.put("id_member", draft.getAnggotaKoperasi().getId().toString());
            if (draft.getMejaKantin() != null)
                payload.put("mejaKantin", draft.getMejaKantin().getId().toString());

            // Gunakan tbmuser pembeli jika tersedia, fallback ke kasir
            userForBayar = detachedUser;
            if (draft.getAnggotaKoperasi() != null && draft.getAnggotaKoperasi().getTbmuser() != null) {
                userForBayar = draft.getAnggotaKoperasi().getTbmuser();
            }
        } finally { closeSession(s); }

        JSONObject hasil = new JSONObject();
        KantinHelper.bayar(userForBayar, payload, hasil);
        return hasil;
    }

    // ── P4. PRODUK LIST TOKO (untuk kasir POS) ────────────────────────────────
    /**
     * Katalog produk toko pedagang yang sedang login.
     * Request: { token, keyword (opsional), page (def:1) }
     * Response: sama dengan kantin_produk_list
     */
    @SuppressWarnings("deprecation")
    public static JSONObject pedagangProdukList(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception {
        Tbmuser detachedUser = ApiUtil.currentUser(json, req);
        if (detachedUser == null) return noAuth();

        String tokoId;
        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            Pedagang pedagang = resolvePedagang(s, detachedUser);
            if (pedagang == null) return noPedagang();
            Toko toko = pedagang.getToko();
            if (toko == null) return ApiHelperSupport.status("99", "Toko tidak ditemukan");
            tokoId = toko.getId().toString();
        } finally { closeSession(s); }

        json.put("id_toko", tokoId);
        return produkList(req, json, pt);
    }

    // ── P5. CARA BAYAR KASIR ──────────────────────────────────────────────────
    /**
     * Daftar metode pembayaran aktif — tanpa filter jenis anggota (untuk kasir).
     * Response: { status:"00", list:[{ id, nama, tunai }] }
     */
    @SuppressWarnings({"deprecation", "unchecked"})
    public static JSONObject pedagangCaraBayar(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception {
        Tbmuser detachedUser = ApiUtil.currentUser(json, req);
        if (detachedUser == null) return noAuth();

        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            Pedagang pedagang = resolvePedagang(s, detachedUser);
            if (pedagang == null) return noPedagang();

            List<Object[]> rows = s.createSQLQuery(
                "SELECT id, nama, manual FROM koperasi.cara_pembayaran_koperasi " +
                "WHERE aktif=true AND online=true ORDER BY nama ASC"
            ).list();

            JSONArray arr = new JSONArray();
            for (Object[] r : rows) {
                JSONObject o = new JSONObject();
                o.put("id",    r[0]);
                o.put("nama",  r[1]);
                o.put("tunai", r[2]);
                arr.put(o);
            }
            JSONObject hasil = new JSONObject();
            hasil.put("status", "00");
            hasil.put("list",   arr);
            return hasil;
        } finally { closeSession(s); }
    }

    // ── P6. KASIR BAYAR (transaksi langsung walk-in, tanpa member) ────────────
    /**
     * Checkout langsung di kasir (pelanggan umum atau member opsional).
     * Request: { token, kodeUnik?, idToko (otomatis diisi dari toko pedagang),
     *   waktu, caraBayar, transaksi:[{id,kode,nama,harga,jumlah,diskon,cashback}],
     *   keterangan?, id_member? }
     * Response: seperti kantin_bayar
     */
    @SuppressWarnings("deprecation")
    public static JSONObject pedagangKasirBayar(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception {
        Tbmuser detachedUser = ApiUtil.currentUser(json, req);
        if (detachedUser == null) return noAuth();

        Tbmuser userForBayar = detachedUser;

        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            Pedagang pedagang = resolvePedagang(s, detachedUser);
            if (pedagang == null) return noPedagang();
            Toko toko = pedagang.getToko();
            if (toko == null) return ApiHelperSupport.status("99", "Toko tidak ditemukan");

            // Paksa idToko dari toko pedagang (tidak bisa diganti client)
            json.put("idToko", toko.getId().toString());
            if (!json.has("kodeUnik") || safeStr(json, "kodeUnik").isEmpty()) {
                json.put("kodeUnik", "KASIR-" + System.currentTimeMillis());
            }
            if (!json.has("waktu") || safeStr(json, "waktu").isEmpty()) {
                json.put("waktu", new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new java.util.Date()));
            }
        } finally { closeSession(s); }

        JSONObject hasil = new JSONObject();
        KantinHelper.bayar(userForBayar, json, hasil);
        return hasil;
    }

    // ── P7. RIWAYAT TRANSAKSI HARI INI ───────────────────────────────────────
    /**
     * Transaksi selesai hari ini untuk toko pedagang.
     * Request: { token, page (def:1) }
     * Response: { status:"00", total_hari_ini, list:[{ id, kode, tanggal,
     *   nama_pembeli, cara_bayar, total_biaya }] }
     */
    @SuppressWarnings({"deprecation", "unchecked"})
    public static JSONObject pedagangRiwayat(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception {
        Tbmuser detachedUser = ApiUtil.currentUser(json, req);
        if (detachedUser == null) return noAuth();

        int page = safeInt(json, "page", 1);
        if (page < 1) page = 1;
        int limit = 20, offset = (page - 1) * limit;

        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            Pedagang pedagang = resolvePedagang(s, detachedUser);
            if (pedagang == null) return noPedagang();
            Toko toko = pedagang.getToko();
            if (toko == null) return ApiHelperSupport.status("99", "Toko tidak ditemukan");

            List<Object[]> rows = s.createSQLQuery(
                "SELECT p.id, p.kode, " +
                "  TO_CHAR(p.tanggal_pembayaran,'DD-MM-YYYY HH24:MI') AS tanggal, " +
                "  COALESCE(ak.userid,'Pelanggan Umum') AS nama_pembeli, " +
                "  COALESCE(cp.nama,'-') AS cara_bayar, " +
                "  COALESCE(p.total_biaya,0) " +
                "FROM koperasi.pembelian_anggota_koperasi p " +
                "LEFT JOIN koperasi.anggota_koperasi ak ON p.anggota_koperasi=ak.id " +
                "LEFT JOIN koperasi.cara_pembayaran_koperasi cp ON p.cara_pembayaran_koperasi=cp.id " +
                "WHERE p.toko=:toko AND DATE(p.tanggal_pembayaran)=CURRENT_DATE " +
                "ORDER BY p.tanggal_pembayaran DESC LIMIT :lim OFFSET :off"
            ).setParameter("toko", toko.getId()).setParameter("lim", limit).setParameter("off", offset).list();

            Object totalRow = s.createSQLQuery(
                "SELECT COALESCE(SUM(total_biaya),0) FROM koperasi.pembelian_anggota_koperasi " +
                "WHERE toko=:toko AND DATE(tanggal_pembayaran)=CURRENT_DATE"
            ).setParameter("toko", toko.getId()).uniqueResult();

            JSONArray arr = new JSONArray();
            for (Object[] r : rows) {
                JSONObject o = new JSONObject();
                o.put("id",           r[0]);
                o.put("kode",         r[1]);
                o.put("tanggal",      r[2] != null ? r[2] : "");
                o.put("nama_pembeli", r[3]);
                o.put("cara_bayar",   r[4]);
                o.put("total_biaya",  jsonLong(r[5] != null ? ((Number) r[5]).longValue() : 0L));
                arr.put(o);
            }

            JSONObject hasil = new JSONObject();
            hasil.put("status",       "00");
            hasil.put("list",         arr);
            hasil.put("total_hari_ini", jsonLong(totalRow != null ? ((Number) totalRow).longValue() : 0L));
            return hasil;
        } finally { closeSession(s); }
    }

    // ── P8. DASHBOARD PEDAGANG ────────────────────────────────────────────────
    /**
     * KPI hari ini untuk toko pedagang.
     * Response: { status:"00", data:{ total_transaksi, omzet_hari_ini,
     *   rata_rata_transaksi, pesanan_menunggu } }
     */
    @SuppressWarnings("deprecation")
    public static JSONObject pedagangDashboard(HttpServletRequest req, JSONObject json, PerguruanTinggi pt) throws Exception {
        Tbmuser detachedUser = ApiUtil.currentUser(json, req);
        if (detachedUser == null) return noAuth();

        Session s = HibernateUtil.getSessionFactory().openSession();
        try {
            Pedagang pedagang = resolvePedagang(s, detachedUser);
            if (pedagang == null) return noPedagang();
            Toko toko = pedagang.getToko();
            if (toko == null) return ApiHelperSupport.status("99", "Toko tidak ditemukan");

            Object[] summary = (Object[]) s.createSQLQuery(
                "SELECT COUNT(id), COALESCE(SUM(total_biaya),0), " +
                "  COALESCE(SUM(total_biaya)/NULLIF(COUNT(id),0),0) " +
                "FROM koperasi.pembelian_anggota_koperasi " +
                "WHERE toko=:toko AND DATE(tanggal_pembayaran)=CURRENT_DATE"
            ).setParameter("toko", toko.getId()).uniqueResult();

            Object menunggu = s.createSQLQuery(
                "SELECT COUNT(id) FROM koperasi.draft_pembelian_anggota_koperasi " +
                "WHERE toko=:toko AND lunas IS NULL"
            ).setParameter("toko", toko.getId()).uniqueResult();

            JSONObject data = new JSONObject();
            if (summary != null) {
                data.put("total_transaksi",     summary[0] != null ? ((Number) summary[0]).intValue()  : 0);
                data.put("omzet_hari_ini",      jsonLong(summary[1] != null ? ((Number) summary[1]).longValue() : 0L));
                data.put("rata_rata_transaksi", jsonLong(summary[2] != null ? ((Number) summary[2]).longValue() : 0L));
            } else {
                data.put("total_transaksi", 0);
                data.put("omzet_hari_ini",  0);
                data.put("rata_rata_transaksi", 0);
            }
            data.put("pesanan_menunggu", menunggu != null ? ((Number) menunggu).intValue() : 0);

            JSONObject hasil = new JSONObject();
            hasil.put("status", "00");
            hasil.put("data",   data);
            return hasil;
        } finally { closeSession(s); }
    }
}
