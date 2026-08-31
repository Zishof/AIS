package ais.common.newui.kampus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.helper.DaftarUlangPembayaranHelper;
import ais.action.master.helper.KegiatanPersistenceHelper;
import ais.action.master.helper.PembayaranGatewayKatalog;
import ais.action.master.helper.PembayaranUtilHelper;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.CommonHelperClass;
import ais.common.ConstantValues;
import ais.common.newui.NewUiRouteGuard;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisPembayaran;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.LogPembayaran;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Tbmuser;
import ais.ui.util.WaktuUtil;

/**
 * Kontrak JSON kasir daftar ulang mahasiswa/calon mahasiswa — paritas logika
 * daftarulang_mahasiswa_lama.zul (mode "lama"), daftarulang_mahasiswa_baru.zul
 * (mode "baru"), dan daftarulang_mahasiswa_calon.zul (mode "calon") tanpa
 * komponen ZK. Mesin data mengikuti resep yang sudah terbukti dipakai bersama
 * ZK+JSP: WizardPembayaranMhsHelper.muatTagihan (agregasi "dibayar" dengan
 * seluruh fallback kunci) dan _bayar_tunai_service.jsp (perhitungan denda per
 * item + simpan CicilanPembayaran satu transaksi).
 *
 * Action: lookup, meta, list, options, revisions, save (tunai/manual kasir).
 *
 * Paritas aturan yang ditegakkan server-side (fail-closed):
 * - save hanya petugas (ZK onSave menolak user mahasiswa/calon) + CSRF.
 * - payload kosong / total &lt; 1.0 DITOLAK — tidak pernah menjatuhkan diri ke
 *   jalur "cicilan default" ZK yang lewat Common.simpanCicilanTanpaMencicil
 *   (method itu MENGHAPUS semua cicilan kegiatan bila total &lt; 1.0).
 * - nominal per item tidak boleh melebihi kekurangan bila konfigurasi
 *   check_apakah_melebihi_tagihan aktif; cara bayar wajib bila
 *   integrasi_modul_akuntansi aktif; harus_menyertakan_bukti_pembayaran aktif
 *   membuat save ditolak dengan pesan jelas (unggah lampiran belum didukung
 *   kontrak ini — fail-closed, bukan dilonggarkan).
 * - guard anti-pembayaran-ganda: signature identik dalam rentang cooldown
 *   DaftarUlangPembayaranHelper ditolak dengan kode KONFIRMASI_GANDA; klien
 *   mengulang dengan konfirmasiGanda=true setelah petugas menegaskan
 *   (paritas dialog OK/Batal ZK).
 * - mode "baru"/"calon": Kegiatan.setStatusMahasiswa(AKTIF), setMahasiswa(null),
 *   setCalonMahasiswa (paritas Baru:4239-4243); mode "calon" memaksa
 *   jenisKegiatan=PENDAFTARAN_CALON_MAHASISWA dan semester=0 (subclass 28 baris).
 * - Bug ZK TIDAK direplikasi: Baru:4368 (membandingkan id JenisPembayaran dgn id
 *   CicilanPembayaran) dan Baru:4306 (NPE cpSebelumnya tanpa null-check).
 *
 * Deviasi sadar (didokumentasikan di inventaris):
 * - Jalur bayar = tunai/manual kasir saja; gateway/VA/keranjang/tabungan ZK
 *   belum termasuk kontrak ini.
 * - Kuitansi tidak dicetak dari controller; klien menerima id kegiatan+cicilan.
 * - resolusi jadwal: mode lama memblok bila jadwal tidak ditemukan (paritas
 *   Lama "BLOK LANGSUNG"); mode baru/calon memakai fallback jadwal Kegiatan
 *   tersimpan (paritas _bayar_tunai_service.jsp).
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class NewUiDaftarUlangMahasiswaController {

    public static final String MODE_LAMA = "lama";
    public static final String MODE_BARU = "baru";
    public static final String MODE_CALON = "calon";

    private static final String MODULE = "root";
    private static final String SESSION_GUARD = "nuiDaftarUlangLastBayar";

    private NewUiDaftarUlangMahasiswaController() { }

    private static String pageFor(String mode) {
        if (MODE_BARU.equals(mode)) return "daftar_ulang_mahasiswa_baru";
        if (MODE_CALON.equals(mode)) return "daftar_ulang_calon_mahasiswa";
        return "daftar_ulang_mahasiswa_lama";
    }

    public static void handle(HttpServletRequest request, HttpServletResponse response, String mode) throws Exception {
        handle(request, response, mode, pageFor(mode));
    }

    /**
     * pageKey harus sama dengan page pada NewUiRouteRegistry untuk menu yang diminta
     * (guard menolak bila tidak cocok) — mis. route pembayaran_mahasiswa memakai
     * mesin MODE_LAMA dengan pageKey "pembayaran_mahasiswa".
     */
    public static void handle(HttpServletRequest request, HttpServletResponse response, String mode, String pageKey)
            throws Exception {
        response.setHeader("Cache-Control", "no-store");
        JSONObject json = new JSONObject();
        try {
            String action = text(request.getParameter("action"), "meta");
            if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, pageKey, action)) {
                response.setContentType("application/json; charset=UTF-8");
                response.setStatus(403); fail(json, "ACTION_FORBIDDEN", "Hak akses tidak tersedia."); write(response, json); return;
            }
            boolean mutation = "save".equals(action) || "upload".equals(action);
            if (mutation && (!"POST".equalsIgnoreCase(request.getMethod()) || !csrf(request))) {
                response.setContentType("application/json; charset=UTF-8");
                response.setStatus(403); fail(json, "CSRF_INVALID", "Token CSRF tidak valid."); write(response, json); return;
            }
            Tbmuser user = Common.getCurrentUser(request);
            if (user == null) throw new SecurityException("Sesi tidak dikenal.");
            Subjek subjek = resolveSubjek(request, user, mode);
            response.setContentType("application/json; charset=UTF-8");
            if ("export_kuitansi".equals(action)) kuitansi(json, request, subjek, user);
            else if ("lookup".equals(action)) lookup(json, request, user, mode);
            else if ("meta".equals(action)) meta(json, request, subjek, mode);
            else if ("list".equals(action)) list(json, request, subjek, mode);
            else if ("options".equals(action)) options(json);
            else if ("revisions".equals(action)) riwayat(json, subjek);
            else if ("informasi".equals(action)) informasi(json, subjek, mode);
            else if ("upload".equals(action)) uploadBukti(json, request, subjek, user);
            else if ("save".equals(action)) bayar(json, request, subjek, user, mode);
            else throw new IllegalArgumentException("Aksi tidak dikenal.");
            json.put("ok", true);
        } catch (SecurityException e) { response.setStatus(403); fail(json, "FORBIDDEN", e.getMessage()); }
        catch (KonfirmasiGandaException e) { response.setStatus(409); fail(json, "KONFIRMASI_GANDA", e.getMessage()); }
        catch (IllegalArgumentException e) { response.setStatus(422); fail(json, "VALIDATION_FAILED", e.getMessage()); }
        catch (Exception e) {
            response.setStatus(500);
            fail(json, "INTERNAL_ERROR", "Gagal memproses kasir daftar ulang. Detail dicatat di log server.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiDaftarUlangMahasiswaController"); } catch (Exception ignored) { }
        }
        response.setContentType("application/json; charset=UTF-8");
        write(response, json);
    }

    /** Subjek kasir kampus: mahasiswa (mode lama) XOR calon (mode baru/calon). */
    private static final class Subjek {
        Mahasiswa mahasiswa; BiodataCalonMahasiswa calon;
        boolean staf;
    }

    /**
     * Tipe implementasi bersarang {@link KonfirmasiGandaException} milik {@link
     * NewUiDaftarUlangMahasiswaController}. Kelas ini memberi nama pada state atau perilaku lokal agar tanggung
     * jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiDaftarUlangMahasiswaController}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p> Tipe ini merupakan detail implementasi privat; pemanggil luar harus memakai API
     * kelas induk.
     *
     * @see NewUiDaftarUlangMahasiswaController
     */
    private static final class KonfirmasiGandaException extends RuntimeException {
        KonfirmasiGandaException(String m) { super(m); }
    }

    private static boolean modeCalon(String mode) { return MODE_BARU.equals(mode) || MODE_CALON.equals(mode); }

    private static Subjek resolveSubjek(HttpServletRequest r, Tbmuser user, String mode) {
        Subjek subjek = new Subjek();
        boolean relasiTerbatas = user.getMahasiswa() != null || user.getBiodataCalonMahasiswa() != null
                || user.getSiswa() != null || user.getCalonSiswa() != null;
        subjek.staf = !relasiTerbatas;
        // Pengguna ber-relasi terkunci ke dirinya sendiri (jangan percaya parameter id).
        if (user.getMahasiswa() != null) {
            if (!modeCalon(mode)) subjek.mahasiswa = user.getMahasiswa();
            return subjek;
        }
        if (user.getBiodataCalonMahasiswa() != null) {
            if (modeCalon(mode)) subjek.calon = user.getBiodataCalonMahasiswa();
            return subjek;
        }
        if (user.getSiswa() != null || user.getCalonSiswa() != null) return subjek;
        Long mhsId = id(r, "mahasiswaId", false);
        Long calonId = id(r, "calonMahasiswaId", false);
        Session s = HibernateUtil.openSession();
        try {
            if (!modeCalon(mode) && mhsId != null)
                subjek.mahasiswa = (Mahasiswa) s.get(Mahasiswa.class, mhsId);
            else if (modeCalon(mode) && calonId != null)
                subjek.calon = (BiodataCalonMahasiswa) s.get(BiodataCalonMahasiswa.class, calonId);
        } finally { s.close(); }
        return subjek;
    }

    private static void requireSubjek(Subjek subjek, String mode) {
        if (modeCalon(mode) ? subjek.calon == null : subjek.mahasiswa == null)
            throw new IllegalArgumentException(modeCalon(mode)
                    ? "Calon mahasiswa wajib dipilih." : "Mahasiswa wajib dipilih.");
    }

    // ---------------------------------------------------------------- lookup
    /**
     * Paritas banbox ZK: q kosong tetap mengembalikan halaman pertama daftar
     * (onOpen menampilkan daftar tanpa mengetik). Pengguna ber-relasi hanya
     * melihat dirinya (flag "sendiri" = true agar klien mengunci pilihan).
     */
    private static void lookup(JSONObject j, HttpServletRequest r, Tbmuser user, String mode) throws Exception {
        String q = text(r.getParameter("q"), "");
        JSONArray arr = new JSONArray();
        boolean sendiriSaja = user.getMahasiswa() != null || user.getBiodataCalonMahasiswa() != null;
        if (user.getMahasiswa() != null) {
            Mahasiswa m = user.getMahasiswa();
            if (!modeCalon(mode))
                arr.put(new JSONObject().put("id", m.getId()).put("nama", nz(m.getNama())).put("kode", nz(m.getNim())));
        } else if (user.getBiodataCalonMahasiswa() != null) {
            BiodataCalonMahasiswa c = user.getBiodataCalonMahasiswa();
            if (modeCalon(mode))
                arr.put(new JSONObject().put("id", c.getId()).put("nama", nz(c.getNama())).put("kode", nz(c.getNoRegistrasi())));
        } else {
            boolean adaFilter = q.length() >= 2;
            Session s = HibernateUtil.openSession();
            try {
                if (!modeCalon(mode)) {
                    Criteria c = s.createCriteria(Mahasiswa.class)
                            .addOrder(Order.asc("nama")).setMaxResults(20);
                    if (adaFilter) c.add(Restrictions.or(Restrictions.ilike("nama", "%" + q + "%"),
                            Restrictions.ilike("nim", "%" + q + "%")));
                    for (Object o : c.list()) {
                        Mahasiswa m = (Mahasiswa) o;
                        arr.put(new JSONObject().put("id", m.getId())
                                .put("nama", nz(m.getNama())).put("kode", nz(m.getNim())));
                    }
                } else {
                    Criteria c = s.createCriteria(BiodataCalonMahasiswa.class)
                            .addOrder(Order.asc("nama")).setMaxResults(20);
                    if (adaFilter) c.add(Restrictions.or(Restrictions.ilike("nama", "%" + q + "%"),
                            Restrictions.ilike("noRegistrasi", "%" + q + "%")));
                    for (Object o : c.list()) {
                        BiodataCalonMahasiswa cm = (BiodataCalonMahasiswa) o;
                        arr.put(new JSONObject().put("id", cm.getId())
                                .put("nama", nz(cm.getNama())).put("kode", nz(cm.getNoRegistrasi())));
                    }
                }
            } finally { s.close(); }
        }
        j.put(modeCalon(mode) ? "calon" : "mahasiswa", arr);
        j.put(modeCalon(mode) ? "mahasiswa" : "calon", new JSONArray());
        j.put("sendiri", sendiriSaja);
    }

    // ------------------------------------------------------------------ meta
    private static void meta(JSONObject j, HttpServletRequest r, Subjek subjek, String mode) throws Exception {
        j.put("staf", subjek.staf).put("mode", mode);
        JSONArray jenis = new JSONArray();
        if (MODE_CALON.equals(mode)) {
            JenisKegiatan jk = (JenisKegiatan) ConstantValues.PENDAFTARAN_CALON_MAHASISWA;
            if (jk != null) jenis.put(new JSONObject().put("id", jk.getId())
                    .put("nama", nz(jk.getNamaKegiatan())).put("utama", true));
        } else if (MODE_BARU.equals(mode)) {
            JenisKegiatan jk = (JenisKegiatan) ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU;
            if (jk != null) jenis.put(new JSONObject().put("id", jk.getId())
                    .put("nama", nz(jk.getNamaKegiatan())).put("utama", true));
        } else {
            // Paritas Common.initJenisPembayaranMahasiswa: jenisKegiatansTanpaDaftarUlang,
            // default PENDAFTARAN_MAHASISWA_LAMA.
            if (CommonHelperClass.jenisKegiatansTanpaDaftarUlang == null) CommonHelperClass.reloadJenisKegiatans();
            Long utama = ConstantValues.PENDAFTARAN_MAHASISWA_LAMA == null ? null
                    : ((JenisKegiatan) ConstantValues.PENDAFTARAN_MAHASISWA_LAMA).getId();
            if (CommonHelperClass.jenisKegiatansTanpaDaftarUlang != null) {
                for (JenisKegiatan jk : CommonHelperClass.jenisKegiatansTanpaDaftarUlang) {
                    jenis.put(new JSONObject().put("id", jk.getId()).put("nama", nz(jk.getNamaKegiatan()))
                            .put("utama", utama != null && utama.equals(jk.getId())));
                }
            }
        }
        j.put("jenisKegiatan", jenis);
        j.put("semesterTetap", MODE_CALON.equals(mode) ? 0 : JSONObject.NULL);
        j.put("buktiWajib", boleh("harus_menyertakan_bukti_pembayaran", false));
        j.put("integrasiAkunting", boleh("integrasi_modul_akuntansi", false));
        j.put("cekTunggakan", boleh("chek_tunggakan_sebelum_bayar", false));
        if (subjek.mahasiswa != null || subjek.calon != null) {
            j.put("subjekNama", subjek.mahasiswa != null ? nz(subjek.mahasiswa.getNama()) : nz(subjek.calon.getNama()));
            j.put("subjekKode", subjek.mahasiswa != null ? nz(subjek.mahasiswa.getNim()) : nz(subjek.calon.getNoRegistrasi()));
        }
        j.put("csrf", csrfToken(r));
    }

    // ------------------------------------------------------------------ list
    private static JenisKegiatan resolveJenis(HttpServletRequest r, String mode) {
        if (MODE_CALON.equals(mode)) {
            JenisKegiatan jk = (JenisKegiatan) ConstantValues.PENDAFTARAN_CALON_MAHASISWA;
            if (jk == null) throw new IllegalArgumentException("Jenis kegiatan pendaftaran calon belum dikonfigurasi.");
            return jk;
        }
        if (MODE_BARU.equals(mode)) {
            JenisKegiatan jk = (JenisKegiatan) ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU;
            if (jk == null) throw new IllegalArgumentException("Jenis kegiatan daftar ulang baru belum dikonfigurasi.");
            return jk;
        }
        Long jkId = id(r, "jenisKegiatanId", false);
        if (jkId == null && ConstantValues.PENDAFTARAN_MAHASISWA_LAMA != null)
            return (JenisKegiatan) ConstantValues.PENDAFTARAN_MAHASISWA_LAMA;
        if (jkId == null) throw new IllegalArgumentException("jenisKegiatanId wajib diisi.");
        Session s = HibernateUtil.openSession();
        try {
            JenisKegiatan jk = (JenisKegiatan) s.get(JenisKegiatan.class, jkId);
            if (jk == null) throw new IllegalArgumentException("Jenis pembayaran tidak ditemukan.");
            return jk;
        } finally { s.close(); }
    }

    private static int resolveSemester(HttpServletRequest r, String mode) {
        if (MODE_CALON.equals(mode)) return 0; // dipatok subclass DaftarUlangCalonMahasiswaAction
        Integer smt = integerObject(r, "smt");
        if (smt == null) return 1;
        if (smt < 0 || smt > 30) throw new IllegalArgumentException("smt tidak valid.");
        return smt;
    }

    private static String tahunAkademik(Subjek subjek, int smt) {
        try {
            Integer angkatan; Integer smtMasuk; String semesterMulai;
            if (subjek.mahasiswa != null) {
                angkatan = subjek.mahasiswa.getTahunangkatan() == null ? 0 : subjek.mahasiswa.getTahunangkatan();
                smtMasuk = subjek.mahasiswa.getPindahKeKampusIniMasukSemester() == null ? 0
                        : subjek.mahasiswa.getPindahKeKampusIniMasukSemester();
                semesterMulai = subjek.mahasiswa.getSemesterMulai() == null ? "" : subjek.mahasiswa.getSemesterMulai();
            } else {
                angkatan = subjek.calon.getTahun() == null ? 0 : subjek.calon.getTahun();
                smtMasuk = 0;
                semesterMulai = subjek.calon.getSemesterMulai() == null ? "" : subjek.calon.getSemesterMulai();
            }
            Integer mulai = Common.getTahunAkademik(smt, angkatan, smtMasuk, semesterMulai);
            if (mulai != null && mulai > 0) return mulai + "/" + (mulai + 1);
        } catch (Exception e) {
            try { ais.common.ErrorAuditUtil.record(e, "NewUiDaftarUlang tahunAkademik"); } catch (Exception ignored) { }
        }
        try {
            String ta = Common.getCurrentTahunAkademik();
            if (ta != null && ta.trim().length() > 0) return ta.trim();
        } catch (Exception ignored) { }
        return "";
    }

    /** Resolusi jadwal paritas Lama/_bayar_tunai_service; null = tidak ditemukan. */
    private static JadwalPembayaran resolveJadwal(Subjek subjek, JenisKegiatan jk, int smt, String ta, Date tanggal) {
        try {
            java.io.Serializable[] s;
            if (subjek.mahasiswa != null) {
                Mahasiswa m = subjek.mahasiswa;
                s = PembayaranUtil.getInstance().getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(tanggal, jk,
                        m.getJurusan() == null ? null : m.getJurusan().getJenjang(), ta, smt % 2 != 0,
                        m.getJenisSeleksi(), m.getProgram(), m.getNim(), null);
            } else {
                BiodataCalonMahasiswa c = subjek.calon;
                s = PembayaranUtil.getInstance().getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(tanggal, jk,
                        c.getJenjang(), ta, smt % 2 != 0, c.getJenisSeleksi(), c.getProgram(),
                        c.getNoRegistrasi(), c.getGelombangPendaftaran());
            }
            if (s != null && s.length > 0) return (JadwalPembayaran) s[0];
        } catch (Exception e) {
            try { ais.common.ErrorAuditUtil.record(e, "NewUiDaftarUlang resolveJadwal"); } catch (Exception ignored) { }
        }
        return null;
    }

    /** Satu baris tagihan hasil resep muatTagihan (DetailBiaya reguler atau slot bulanan). */
    private static final class Baris {
        DetailBiaya db; PengaturanPembayaranBulanan ppb;
        double tagihan; double dibayar; String keterangan = "";
        double kekurangan() { return Math.max(0, tagihan - dibayar); }
        String kunci() { return ppb != null ? "PB_" + ppb.getId() : "DB_" + db.getId(); }
    }

    /**
     * Muat baris tagihan + agregasi "dibayar" — resep WizardPembayaranMhsHelper.muatTagihan
     * (kunci itemBiaya+bayarKe dgn fallback per-item, slot bulanan per-id PPB dgn fallback
     * item+realBulan) untuk mahasiswa; sumber daftar untuk calon mengikuti
     * DaftarUlangMahasiswaBaruAction.listBiaya (getDetailBiayaCalonMahasiswa dgn fallback
     * jurusan prodiLulus->prodi1->prodi2 + getPengaturanPembayaranSemua bila bulanan).
     */
    private static List<Baris> muatBaris(Subjek subjek, JenisKegiatan jk, int smt, Kegiatan kegiatan) throws Exception {
        Collection sumber;
        Session session = HibernateUtil.openSession();
        try {
            if (subjek.mahasiswa != null) {
                sumber = PembayaranUtilHelper.getDetailBiayaMahasiswa(subjek.mahasiswa, smt, jk, true);
                int bulanan = PembayaranUtilHelper.countBulanan(session, subjek.mahasiswa, jk, smt, sumber, true, true);
                if (bulanan > 0) {
                    // Wajib overload 6-argumen dgn TRUE (lihat komentar wizard): slot bulanan
                    // yang sudah dibayar parsial harus tetap tampil.
                    sumber = PembayaranUtilHelper.getDetailBiayaMahasiswa(subjek.mahasiswa, smt, jk, "-1",
                            Boolean.TRUE, true);
                }
            } else {
                BiodataCalonMahasiswa calon = subjek.calon;
                Jurusan jurusan = calon.getProdiLulus();
                if (jurusan == null || jurusan.getId() == null)
                    jurusan = calon.getProdi1() == null ? calon.getProdi2() : calon.getProdi1();
                Collection<DetailBiaya> detail = ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
                        && jk.getId().equals(((JenisKegiatan) ConstantValues.PENDAFTARAN_CALON_MAHASISWA).getId())
                                ? PembayaranUtilHelper.getDetailBiayaCalonMahasiswa(calon, jk, jurusan, true)
                                : PembayaranUtilHelper.getDetailBiayaCalonMahasiswa(calon, jk, jurusan, smt, true);
                sumber = detail;
                int bulanan = PembayaranUtilHelper.countBulanan(session, calon, jk, smt, detail, true, true);
                if (bulanan > 0) {
                    Collection<PengaturanPembayaranBulanan> slot = PembayaranUtil.getInstance()
                            .getPengaturanPembayaranSemua(calon, session, smt, jk, detail, true, false);
                    if (slot != null && !slot.isEmpty()) sumber = slot;
                }
            }
        } finally { session.close(); }
        if (sumber == null) sumber = new ArrayList();

        List<CicilanPembayaran> cicilans = new ArrayList<CicilanPembayaran>();
        Collection<DetailKegiatan> detailKegiatans = null;
        if (kegiatan != null && kegiatan.getId() != null) {
            try {
                cicilans = KegiatanPersistenceHelper.ambilCicilan(kegiatan, true);
                detailKegiatans = kegiatan.ambilDetailKegiatan(true);
            } catch (Exception e) {
                try { ais.common.ErrorAuditUtil.record(e, "NewUiDaftarUlang ambilCicilan"); } catch (Exception ignored) { }
            }
        }

        Map<Long, Double> perPpb = new HashMap<Long, Double>();
        Map<String, Double> perItemBayarKe = new HashMap<String, Double>();
        Map<Long, Double> perItem = new HashMap<Long, Double>();
        Map<String, Double> perItemBulan = new HashMap<String, Double>();
        for (CicilanPembayaran cp : cicilans) {
            if (cp == null || cp.getNilai() == null) continue;
            if (cp.getPengaturanPembayaranBulanan() != null && cp.getPengaturanPembayaranBulanan().getId() != null) {
                Long idPpb = cp.getPengaturanPembayaranBulanan().getId();
                perPpb.put(idPpb, nvl(perPpb.get(idPpb)) + cp.getNilai());
                try {
                    PengaturanPembayaranBulanan p = cp.getPengaturanPembayaranBulanan();
                    if (p.getRealBulan() != null && cp.getItemBiaya() != null && cp.getItemBiaya().getId() != null) {
                        String k = cp.getItemBiaya().getId() + "_bln_" + p.getRealBulan();
                        perItemBulan.put(k, nvl(perItemBulan.get(k)) + cp.getNilai());
                    }
                } catch (Exception ignored) { }
            }
            if (cp.getItemBiaya() != null && cp.getItemBiaya().getId() != null) {
                String k = cp.getItemBiaya().getId() + "_" + cp.getBayarKe();
                perItemBayarKe.put(k, nvl(perItemBayarKe.get(k)) + cp.getNilai());
                Long idItem = cp.getItemBiaya().getId();
                perItem.put(idItem, nvl(perItem.get(idItem)) + cp.getNilai());
            }
        }

        Map<Long, Integer> jumlahBarisPerItem = new HashMap<Long, Integer>();
        for (Object o : sumber) {
            DetailBiaya db = o instanceof DetailBiaya ? (DetailBiaya) o
                    : o instanceof PengaturanPembayaranBulanan ? ((PengaturanPembayaranBulanan) o).getDetailBiaya() : null;
            if (db == null || db.getItemBiaya() == null || db.getItemBiaya().getId() == null) continue;
            Long idItem = db.getItemBiaya().getId();
            Integer n = jumlahBarisPerItem.get(idItem);
            jumlahBarisPerItem.put(idItem, n == null ? 1 : n + 1);
        }

        List<Baris> hasil = new ArrayList<Baris>();
        for (Object o : sumber) {
            Baris b = new Baris();
            if (o instanceof DetailBiaya) b.db = (DetailBiaya) o;
            else if (o instanceof PengaturanPembayaranBulanan) {
                b.ppb = (PengaturanPembayaranBulanan) o;
                try { b.db = b.ppb.getDetailBiaya(); } catch (Exception e) { b.db = null; }
            }
            if (b.db == null || b.db.getId() == null) continue;

            if (b.ppb != null) {
                Double j = null;
                try { j = Kegiatan.ambilJumlahTagihan(kegiatan, detailKegiatans, subjek.mahasiswa, smt, b.ppb); }
                catch (Exception e) {
                    try { ais.common.ErrorAuditUtil.record(e, "NewUiDaftarUlang jumlah bulanan ppb=" + b.ppb.getId()); } catch (Exception ignored) { }
                }
                b.tagihan = j != null ? j : nvl(b.ppb.getNominal());
                if (b.tagihan <= 0 && !Boolean.TRUE.equals(b.ppb.getTetapDitampilkanWalaupunNol())) continue;
            } else {
                Double j = null;
                try { j = Kegiatan.ambilJumlahTagihan(kegiatan, b.db); }
                catch (Exception e) {
                    try { ais.common.ErrorAuditUtil.record(e, "NewUiDaftarUlang jumlah item db=" + b.db.getId()); } catch (Exception ignored) { }
                }
                b.tagihan = j != null ? j : nvl(b.db.getNilaiBiaya());
            }
            if (b.tagihan < 0) continue; // baris diskon sudah tercermin di tagihan lain

            try {
                if (b.db.getItemBiaya() != null
                        && !b.db.getItemBiaya().getPenghitungan().equals(ais.database.model.ItemBiaya.TIDAK_ADA_PENGHITUNGAN)
                        && b.db.getKeterangan() != null)
                    b.keterangan = b.db.getKeterangan().trim();
            } catch (Exception ignored) { }

            if (b.ppb != null && b.ppb.getId() != null) {
                b.dibayar = nvl(perPpb.get(b.ppb.getId()));
                if (b.dibayar <= 0 && b.ppb.getRealBulan() != null && b.db.getItemBiaya() != null
                        && b.db.getItemBiaya().getId() != null) {
                    Double v = perItemBulan.get(b.db.getItemBiaya().getId() + "_bln_" + b.ppb.getRealBulan());
                    if (v != null) b.dibayar = v;
                }
            } else if (b.db.getItemBiaya() != null && b.db.getItemBiaya().getId() != null) {
                Long idItem = b.db.getItemBiaya().getId();
                b.dibayar = nvl(perItemBayarKe.get(idItem + "_" + b.db.getBayarKe()));
                Double vItem = perItem.get(idItem);
                Integer jumlahBaris = jumlahBarisPerItem.get(idItem);
                if (vItem != null && (jumlahBaris == null || jumlahBaris <= 1) && vItem > b.dibayar)
                    b.dibayar = vItem;
            }
            hasil.add(b);
        }
        return hasil;
    }

    private static void list(JSONObject j, HttpServletRequest r, Subjek subjek, String mode) throws Exception {
        requireSubjek(subjek, mode);
        JenisKegiatan jk = resolveJenis(r, mode);
        int smt = resolveSemester(r, mode);
        String ta = tahunAkademik(subjek, smt);
        Date sekarang = WaktuUtil.getDate();

        Kegiatan kegiatan = null;
        try {
            kegiatan = subjek.mahasiswa != null
                    ? subjek.mahasiswa.ambilKegiatansRefresh(smt, jk, true)
                    : subjek.calon.ambilKegiatansRefresh(smt, jk, true);
        } catch (Exception e) {
            try { ais.common.ErrorAuditUtil.record(e, "NewUiDaftarUlang ambilKegiatans"); } catch (Exception ignored) { }
        }

        JadwalPembayaran jadwal = resolveJadwal(subjek, jk, smt, ta, sekarang);
        if (jadwal == null && kegiatan != null) jadwal = kegiatan.getJadwalPembayaran();
        boolean jadwalAda = jadwal != null;
        // Paritas Lama "BLOK LANGSUNG": tanpa jadwal, mode lama tidak menampilkan tagihan.
        if (!jadwalAda && MODE_LAMA.equals(mode)) {
            j.put("jenisKegiatanId", jk.getId()).put("jenisKegiatan", nz(jk.getNamaKegiatan()))
             .put("smt", smt).put("tahunAkademik", ta).put("jadwalAda", false)
             .put("jadwalPesan", "Jadwal pembayaran belum tersedia, telah terlewat, atau belum dimulai.")
             .put("rows", new JSONArray()).put("totalTagihan", 0).put("totalDibayar", 0)
             .put("totalKekurangan", 0).put("csrf", csrfToken(r));
            return;
        }

        List<Baris> rows = muatBaris(subjek, jk, smt, kegiatan);
        JSONArray arr = new JSONArray();
        double totalTagihan = 0, totalDibayar = 0, totalKekurangan = 0;
        for (Baris b : rows) {
            JSONObject o = new JSONObject();
            o.put("key", b.kunci());
            o.put("item", b.db.getItemBiaya() == null ? "" : nz(b.db.getItemBiaya().getNama()));
            o.put("itemBiayaId", b.db.getItemBiaya() == null ? JSONObject.NULL : b.db.getItemBiaya().getId());
            o.put("detailBiayaId", b.db.getId());
            o.put("ppbId", b.ppb == null ? JSONObject.NULL : b.ppb.getId());
            String bulan = "";
            try { if (b.ppb != null) bulan = nz(b.ppb.getNamaBulan()); } catch (Exception ignored) { }
            o.put("bulan", bulan);
            o.put("bayarKe", b.db.getBayarKe());
            o.put("tagihan", b.tagihan);
            o.put("dibayar", b.dibayar);
            o.put("kekurangan", b.kekurangan());
            o.put("lunas", b.kekurangan() <= 0);
            o.put("keterangan", b.keterangan);
            arr.put(o);
            totalTagihan += b.tagihan; totalDibayar += b.dibayar; totalKekurangan += b.kekurangan();
        }
        j.put("jenisKegiatanId", jk.getId()).put("jenisKegiatan", nz(jk.getNamaKegiatan()))
         .put("smt", smt).put("tahunAkademik", ta)
         .put("kegiatanId", kegiatan == null || kegiatan.getId() == null ? JSONObject.NULL : kegiatan.getId())
         .put("jadwalAda", jadwalAda)
         .put("bolehTunai", PembayaranGatewayKatalog.tunaiAktif(jk))
         .put("rows", arr)
         .put("totalTagihan", totalTagihan).put("totalDibayar", totalDibayar)
         .put("totalKekurangan", totalKekurangan)
         .put("csrf", csrfToken(r));
    }

    // --------------------------------------------------------------- options
    /** Cara bayar manual: JenisPembayaran aktif non-tabungan; TUNAI = bawaan. */
    private static void options(JSONObject j) throws Exception {
        JSONArray arr = new JSONArray();
        Long tunaiId = ConstantValues.TUNAI == null ? null : ((JenisPembayaran) ConstantValues.TUNAI).getId();
        Session s = HibernateUtil.openSession();
        try {
            Criteria c = s.createCriteria(JenisPembayaran.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .add(Restrictions.isNull("jenisTabungan"))
                    .addOrder(Order.asc("nama"));
            for (Object o : c.list()) {
                JenisPembayaran jp = (JenisPembayaran) o;
                arr.put(new JSONObject().put("id", jp.getId()).put("nama", nz(jp.getNama()))
                        .put("utama", tunaiId != null && tunaiId.equals(jp.getId())));
            }
        } finally { s.close(); }
        j.put("caraBayar", arr);
    }

    // -------------------------------------------------------------- riwayat
    private static void riwayat(JSONObject j, Subjek subjek) throws Exception {
        JSONArray arr = new JSONArray();
        if (subjek.mahasiswa == null && subjek.calon == null) { j.put("rows", arr).put("total", 0); return; }
        Session s = HibernateUtil.openSession();
        try {
            Criteria c = s.createCriteria(CicilanPembayaran.class)
                    .createAlias("kegiatan", "kegiatan")
                    .addOrder(Order.desc("id"))
                    .setMaxResults(30);
            if (subjek.mahasiswa != null) c.add(Restrictions.eq("kegiatan.mahasiswa", subjek.mahasiswa));
            else c.add(Restrictions.eq("kegiatan.calonMahasiswa", subjek.calon));
            for (Object o : c.list()) {
                CicilanPembayaran cp = (CicilanPembayaran) o;
                JSONObject row = new JSONObject();
                row.put("id", cp.getId());
                row.put("item", cp.getItemBiaya() == null ? "" : nz(cp.getItemBiaya().getNama()));
                String bulan = "";
                try {
                    if (cp.getPengaturanPembayaranBulanan() != null)
                        bulan = nz(cp.getPengaturanPembayaranBulanan().getNamaBulan());
                } catch (Exception ignored) { }
                row.put("bulan", bulan);
                row.put("nilai", nvl(cp.getNilai()));
                row.put("denda", nvl(cp.getDenda()));
                row.put("ke", cp.getKe() == null ? JSONObject.NULL : cp.getKe());
                row.put("waktu", cp.getTanggal() == null ? JSONObject.NULL : cp.getTanggal().getTime());
                row.put("via", cp.getJenisPembayaran() == null ? "" : nz(cp.getJenisPembayaran().getNama()));
                Kegiatan k = cp.getKegiatan();
                row.put("jenis", k == null || k.getJenisKegiatan() == null ? "" : nz(k.getJenisKegiatan().getNamaKegiatan()));
                row.put("smt", k == null || k.getSemster() == null ? JSONObject.NULL : k.getSemster());
                row.put("tahunAkademik", k == null ? "" : nz(k.getTahunAkademik()));
                row.put("kegiatanId", k == null || k.getId() == null ? JSONObject.NULL : k.getId());
                arr.put(row);
            }
        } finally { s.close(); }
        j.put("rows", arr).put("total", arr.length());
    }

    // -------------------------------------------------- informasi pembayaran
    /**
     * Ringkasan seluruh kegiatan pembayaran seorang mahasiswa/calon — paritas
     * layar {@code informasi_pembayaran_mahasiswa.zul} (menu 1732) yang
     * sebelumnya jatuh ke daftar entity Mahasiswa mentah.
     *
     * <p>Read-only: satu baris per {@link Kegiatan} (jenis + TA + semester)
     * dengan tagihan, dibayar, kekurangan, denda, status lunas, jumlah
     * cicilan, dan {@code kegiatanId} untuk cetak kuitansi lewat
     * {@code export_kuitansi}.</p>
     */
    private static void informasi(JSONObject j, Subjek subjek, String mode) throws Exception {
        requireSubjek(subjek, mode);
        JSONArray arr = new JSONArray();
        double totalTagihan = 0, totalDibayar = 0;
        Session s = HibernateUtil.openSession();
        try {
            Criteria c = s.createCriteria(Kegiatan.class).addOrder(Order.desc("id")).setMaxResults(200);
            if (subjek.mahasiswa != null) c.add(Restrictions.eq("mahasiswa", subjek.mahasiswa));
            else c.add(Restrictions.eq("calonMahasiswa", subjek.calon));
            for (Object o : c.list()) {
                Kegiatan k = (Kegiatan) o;
                double amount = nvl(k.getAmount());
                double terhutang = nvl(k.getAmountTerhutang());
                double tagihan = amount + (terhutang > 0 ? terhutang : 0);
                int jumlahCicilan = 0;
                try {
                    List cicilans = KegiatanPersistenceHelper.ambilCicilan(k, false);
                    jumlahCicilan = cicilans == null ? 0 : cicilans.size();
                } catch (Exception ignored) { }
                JSONObject row = new JSONObject();
                row.put("kegiatanId", k.getId());
                row.put("jenis", k.getJenisKegiatan() == null ? "" : nz(k.getJenisKegiatan().getNamaKegiatan()));
                row.put("tahunAkademik", nz(k.getTahunAkademik()));
                row.put("smt", k.getSemster() == null ? JSONObject.NULL : k.getSemster());
                row.put("tanggal", k.getTanggal() == null ? JSONObject.NULL : k.getTanggal().getTime());
                row.put("tagihan", tagihan);
                row.put("dibayar", amount);
                row.put("kekurangan", terhutang > 0 ? terhutang : 0);
                row.put("denda", nvl(k.getDenda()));
                row.put("lunas", terhutang <= 0.1);
                row.put("jumlahCicilan", jumlahCicilan);
                row.put("validator", nz(k.getValidator()));
                row.put("keterangan", nz(k.getKeterangan()));
                arr.put(row);
                totalTagihan += tagihan;
                totalDibayar += amount;
            }
        } finally { s.close(); }
        j.put("rows", arr).put("total", arr.length());
        j.put("totalTagihan", totalTagihan).put("totalDibayar", totalDibayar);
        j.put("totalKekurangan", Math.max(0, totalTagihan - totalDibayar));
        j.put("subjekNama", subjek.mahasiswa != null ? nz(subjek.mahasiswa.getNama()) : nz(subjek.calon.getNama()));
        j.put("subjekKode", subjek.mahasiswa != null ? nz(subjek.mahasiswa.getNim()) : nz(subjek.calon.getNoRegistrasi()));
    }

    // --------------------------------------------------------- unggah bukti
    /**
     * Unggah bukti pembayaran (LampiranLain jenis "cicilanPembayaran") — paritas
     * tombol upload per baris cicilan ZK (Common.initCicilan +
     * LampiranLain.createDownloadUploadFileLain). Payload: `namaFile` + `data`
     * (base64, maks {@value #MAKS_BUKTI_BYTES} byte setelah decode). Hasil `id`
     * dikirim balik pada save sebagai items[i].idLampiran.
     */
    // Gunakan literal tanpa pemisah underscore agar kompatibel dengan source Java 6.
    private static final int MAKS_BUKTI_BYTES = 1500000;

    private static void uploadBukti(JSONObject j, HttpServletRequest r, Subjek subjek, Tbmuser user) throws Exception {
        if (!subjek.staf) throw new SecurityException("Unggah bukti hanya untuk petugas kasir.");
        String namaFile = text(r.getParameter("namaFile"), "");
        String data = text(r.getParameter("data"), "");
        if (namaFile.length() == 0 || data.length() == 0)
            throw new IllegalArgumentException("namaFile dan data (base64) wajib diisi.");
        String lower = namaFile.toLowerCase();
        if (!(lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
                || lower.endsWith(".gif") || lower.endsWith(".bmp") || lower.endsWith(".pdf")))
            throw new IllegalArgumentException("Jenis berkas bukti harus gambar (jpg/png/gif/bmp) atau PDF.");
        byte[] isi;
        try { isi = java.util.Base64.getDecoder().decode(data); }
        catch (Exception e) { throw new IllegalArgumentException("data bukan base64 yang valid."); }
        if (isi.length == 0 || isi.length > MAKS_BUKTI_BYTES)
            throw new IllegalArgumentException("Ukuran berkas bukti harus 1 byte s.d. "
                    + (MAKS_BUKTI_BYTES / 1000) + " KB.");

        // Nama berkas temp = nama asli tersanitasi: createFileFotoLain memakai
        // file.getName() sebagai nama tersimpan, jadi nama temp menentukan label
        // yang dilihat pengguna.
        String basis = namaFile.substring(0, namaFile.lastIndexOf('.'))
                .replaceAll("[^A-Za-z0-9._-]", "_");
        if (basis.length() > 60) basis = basis.substring(0, 60);
        if (basis.length() < 3) basis = basis + "_bukti"; // prefix temp minimal 3 karakter
        java.io.File temp = java.io.File.createTempFile(basis + "_",
                lower.substring(lower.lastIndexOf('.')));
        try {
            java.nio.file.Files.write(temp.toPath(), isi);
            org.hibernate.Session sesiStreaming =
                    ais.database.hibernate.StreamingHibernateUtil.getInstance().currentSession();
            try {
                ais.database.model.file.FileFotoLain lampiran =
                        ais.database.model.file.FileFotoLain.createFileFotoLain(user, sesiStreaming,
                                ais.database.model.file.LampiranLain.class, false, Common.refSementara(),
                                "cicilanPembayaran", null, temp, namaFile);
                if (lampiran == null || lampiran.getId() == null)
                    throw new IllegalStateException("Lampiran gagal tersimpan.");
                j.put("id", lampiran.getId());
                j.put("nama", nz(lampiran.getNama()));
            } finally {
                try { ais.database.hibernate.StreamingHibernateUtil.getInstance().closeSession(); }
                catch (Exception ignored) { }
            }
        } finally {
            try { temp.delete(); } catch (Exception ignored) { }
        }
    }

    // -------------------------------------------------------------- kuitansi
    /**
     * Cetak kuitansi (PDF) sebuah Kegiatan — paritas
     * CommonReportHelper.cetakBuktipembayaran[Calon]Mahasiswa(kirim=false); pada
     * konteks servlet (tanpa sesi ZK) helper otomatis memakai
     * generateFileReportSimple. PDF dikirim sebagai base64 di amplop JSON (JSP
     * delegasi sudah memegang getWriter() sehingga streaming biner mustahil).
     */
    private static void kuitansi(JSONObject j, HttpServletRequest r, Subjek subjek, Tbmuser user)
            throws Exception {
        Long kegiatanId = id(r, "kegiatanId", true);
        Kegiatan kegiatan;
        Session s = HibernateUtil.openSession();
        try { kegiatan = (Kegiatan) s.get(Kegiatan.class, kegiatanId); }
        finally { s.close(); }
        if (kegiatan == null) throw new IllegalArgumentException("Kegiatan tidak ditemukan.");
        // Scoping identitas: pengguna ber-relasi hanya boleh mencetak kegiatannya sendiri.
        if (!subjek.staf) {
            boolean milikSendiri =
                    (user.getMahasiswa() != null && kegiatan.getMahasiswa() != null
                            && user.getMahasiswa().getId().equals(kegiatan.getMahasiswa().getId()))
                    || (user.getBiodataCalonMahasiswa() != null && kegiatan.getCalonMahasiswa() != null
                            && user.getBiodataCalonMahasiswa().getId().equals(kegiatan.getCalonMahasiswa().getId()));
            if (!milikSendiri) throw new SecurityException("Kuitansi di luar cakupan pengguna.");
        }
        java.io.File pdf = kegiatan.getCalonMahasiswa() != null
                ? ais.action.report.CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(kegiatan, false)
                : ais.action.report.CommonReportHelper.cetakBuktipembayaranMahasiswa(kegiatan, false);
        if (pdf == null || !pdf.exists())
            throw new IllegalStateException("PDF kuitansi gagal dibuat.");
        byte[] isi = java.nio.file.Files.readAllBytes(pdf.toPath());
        j.put("namaFile", "kuitansi_kegiatan_" + kegiatanId + ".pdf");
        j.put("pdfBase64", java.util.Base64.getEncoder().encodeToString(isi));
    }

    // ------------------------------------------------------------------ save
    private static void bayar(JSONObject j, HttpServletRequest r, Subjek subjek, Tbmuser user, String mode)
            throws Exception {
        // Gate ZK onSave: hanya petugas; user mahasiswa/calon (atau relasi sekolah) ditolak.
        if (!subjek.staf) throw new SecurityException("Pembayaran tunai kasir hanya untuk petugas.");
        requireSubjek(subjek, mode);
        boolean buktiWajib = boleh("harus_menyertakan_bukti_pembayaran", false);

        JenisKegiatan jkRef = resolveJenis(r, mode);
        if (!PembayaranGatewayKatalog.tunaiAktif(jkRef))
            throw new SecurityException("Pembayaran tunai/manual tidak diaktifkan untuk jenis kegiatan ini.");
        int smt = resolveSemester(r, mode);
        String ta = tahunAkademik(subjek, smt);
        Date tglBayar = tanggal(r.getParameter("tanggal"));
        Date tglKwitansi = tanggal(r.getParameter("tanggalKwitansi"));
        String ket = text(r.getParameter("keterangan"), "Transaksi Manual Kasir");
        Long caraBayarId = id(r, "caraBayarId", false);
        if (caraBayarId == null && boleh("integrasi_modul_akuntansi", false))
            throw new IllegalArgumentException("Cara bayar wajib dipilih (integrasi modul akuntansi aktif).");

        // items = JSON array [{"key":"DB_9"|"PB_7","nominal":150000,
        //                      "idLampiran":123 (opsional; wajib saat buktiWajib)}, ...]
        String rawItems = text(r.getParameter("items"), "");
        if (rawItems.length() == 0) throw new IllegalArgumentException("items wajib diisi.");
        org.json.JSONArray items = new org.json.JSONArray(rawItems);
        double totalDiminta = 0;
        for (int i = 0; i < items.length(); i++) {
            JSONObject it = items.getJSONObject(i);
            double nominal = it.optDouble("nominal", 0);
            if (nominal <= 0) throw new IllegalArgumentException("Nominal setiap item harus lebih dari nol.");
            // Paritas validasiPembayaran ZK: bukti lampiran wajib per baris bila
            // konfigurasi harus_menyertakan_bukti_pembayaran aktif.
            if (buktiWajib && it.optLong("idLampiran", 0L) <= 0L)
                throw new IllegalArgumentException("Bukti pembayaran wajib diunggah untuk setiap item "
                        + "(konfigurasi harus_menyertakan_bukti_pembayaran aktif).");
            totalDiminta += nominal;
        }
        // Payload kosong/total < 1.0 DITOLAK — jangan pernah menjatuhkan ke jalur
        // "cicilan default" ZK (Common.simpanCicilanTanpaMencicil menghapus cicilan).
        if (items.length() == 0 || totalDiminta < 1.0)
            throw new IllegalArgumentException("Tidak ada nilai pembayaran yang dapat disimpan.");

        // Guard anti-pembayaran-ganda (paritas buildBayarSignature + cooldown ZK).
        Long subjekId = subjek.mahasiswa != null ? subjek.mahasiswa.getId() : subjek.calon.getId();
        StringBuilder sig = new StringBuilder(mode).append('|').append(subjekId).append('|')
                .append(jkRef.getId()).append('|').append(smt);
        List<String> potongan = new ArrayList<String>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject it = items.getJSONObject(i);
            potongan.add(it.optString("key", "") + "=" + it.optDouble("nominal", 0));
        }
        java.util.Collections.sort(potongan);
        for (String p : potongan) sig.append('|').append(p);
        String signature = sig.toString();
        boolean konfirmasiGanda = "true".equalsIgnoreCase(r.getParameter("konfirmasiGanda"));
        Object last = r.getSession().getAttribute(SESSION_GUARD);
        long cooldown = DaftarUlangPembayaranHelper.getBayarCooldownMs();
        if (!konfirmasiGanda && last instanceof String) {
            String[] bagian = ((String) last).split("\n", 2);
            if (bagian.length == 2 && bagian[1].equals(signature)) {
                long umur = System.currentTimeMillis() - Long.parseLong(bagian[0]);
                if (umur >= 0 && umur < cooldown)
                    throw new KonfirmasiGandaException("Pembayaran dengan rincian (item & nominal) yang sama "
                            + "baru saja diproses. Ulangi dengan konfirmasiGanda=true bila ini memang "
                            + "pembayaran berbeda.");
            }
        }

        Session sess = null; Transaction tx = null;
        try {
            sess = HibernateUtil.openSession();
            tx = sess.beginTransaction();
            JenisKegiatan jk = (JenisKegiatan) sess.get(JenisKegiatan.class, jkRef.getId());
            Mahasiswa mhs = subjek.mahasiswa == null ? null
                    : (Mahasiswa) sess.get(Mahasiswa.class, subjek.mahasiswa.getId());
            BiodataCalonMahasiswa calon = subjek.calon == null ? null
                    : (BiodataCalonMahasiswa) sess.get(BiodataCalonMahasiswa.class, subjek.calon.getId());
            if (jk == null || (mhs == null && calon == null))
                throw new IllegalArgumentException("Data subjek/jenis kegiatan tidak ditemukan.");

            // Satu kombinasi (subjek, jenis, smt) = SATU Kegiatan (kodeunik unik di DB):
            // pembayaran berikutnya MEMAKAI ULANG Kegiatan yang ada.
            Kegiatan kegiatan = null;
            try {
                kegiatan = mhs != null ? mhs.ambilKegiatansRefresh(smt, jk, true)
                        : calon.ambilKegiatansRefresh(smt, jk, true);
            } catch (Exception e) {
                try { ais.common.ErrorAuditUtil.record(e, "NewUiDaftarUlang save ambilKegiatans"); } catch (Exception ignored) { }
            }
            if (kegiatan != null && kegiatan.getId() != null)
                kegiatan = (Kegiatan) sess.load(Kegiatan.class, kegiatan.getId());
            else kegiatan = new Kegiatan();

            JadwalPembayaran jadwal = resolveJadwal(subjek, jk, smt, ta, tglBayar);
            if (jadwal == null && kegiatan.getJadwalPembayaran() != null) jadwal = kegiatan.getJadwalPembayaran();
            if (jadwal == null && MODE_LAMA.equals(mode))
                throw new IllegalArgumentException("Jadwal pembayaran belum tersedia, telah terlewat, "
                        + "atau belum dimulai.");

            kegiatan.setJenisKegiatan(jk);
            kegiatan.setSemster(smt);
            kegiatan.setTahunAkademik(ta);
            kegiatan.setTanggal(tglBayar);
            kegiatan.setValidated(1);
            kegiatan.setValidator(user.getUserNama());
            kegiatan.setKeterangan(ket);
            if (jadwal != null) kegiatan.setJadwalPembayaran(jadwal);
            if (mhs != null) {
                kegiatan.setMahasiswa(mhs);
                if (kegiatan.getStatusMahasiswa() == null) kegiatan.setStatusMahasiswa(ConstantValues.AKTIF);
            } else {
                // Paritas DaftarUlangMahasiswaBaruAction.onSave (Baru:4239-4243).
                kegiatan.setStatusMahasiswa(ConstantValues.AKTIF);
                kegiatan.setMahasiswa(null);
                kegiatan.setCalonMahasiswa(calon);
            }
            Common.refreshSaveOrUpdate(sess, kegiatan);

            Collection<DetailKegiatan> detailKegiatans = kegiatan.ambilDetailKegiatan(false);
            // Total tagihan konteks ini (untuk amountTerhutang, paritas Lama).
            List<Baris> semuaBaris = muatBaris(subjek, jk, smt, kegiatan.getId() == null ? null : kegiatan);
            double nilaiHarusDibayar = 0;
            Map<String, Baris> perKunci = new HashMap<String, Baris>();
            for (Baris b : semuaBaris) { nilaiHarusDibayar += b.tagihan; perKunci.put(b.kunci(), b); }
            boolean cekMelebihi = boleh("check_apakah_melebihi_tagihan", false);

            JenisPembayaran caraBayar = null;
            if (caraBayarId != null) caraBayar = (JenisPembayaran) sess.get(JenisPembayaran.class, caraBayarId);
            if (caraBayar == null) caraBayar = (JenisPembayaran) ConstantValues.TUNAI;

            JadwalPembayaran jdwKhusus = null;
            String nimAtauNoReg = mhs != null ? mhs.getNim() : calon.getNoRegistrasi();
            if (jadwal != null && jadwal.getKhususUntukNim() != null
                    && jadwal.getKhususUntukNim().contains("," + nimAtauNoReg + ","))
                jdwKhusus = jadwal;

            List<Long> cicilanIds = new ArrayList<Long>();
            double totalBayar = 0;
            int urutan = 1;
            for (int i = 0; i < items.length(); i++) {
                JSONObject it = items.getJSONObject(i);
                String kunci = it.optString("key", "");
                double nominal = it.optDouble("nominal", 0);

                Baris baris = perKunci.get(kunci);
                if (cekMelebihi && baris != null && nominal > baris.kekurangan() + 0.01)
                    throw new IllegalArgumentException("Nominal untuk \"" + (baris.db.getItemBiaya() == null ? kunci
                            : baris.db.getItemBiaya().getNama()) + "\" melebihi kekurangan tagihan.");

                DetailBiaya db = null; PengaturanPembayaranBulanan ppb = null; DetailKegiatan dkSesuai = null;
                double denda = 0;
                if (kunci.startsWith("DB_")) {
                    db = (DetailBiaya) sess.get(DetailBiaya.class, Long.valueOf(kunci.substring(3)));
                    if (db == null) throw new IllegalArgumentException("Item biaya tidak ditemukan: " + kunci);
                    if (detailKegiatans != null) {
                        for (DetailKegiatan dk : detailKegiatans) {
                            if (dk.getDetailBiaya() != null && dk.getDetailBiaya().getId().equals(db.getId())
                                    && dk.getPengaturanPembayaranBulanan() == null) { dkSesuai = dk; break; }
                        }
                    }
                    Double jml = Kegiatan.ambilJumlahTagihan(dkSesuai, kegiatan, db, false);
                    if (jml == null) jml = 0.0;
                    Double hasil = dkSesuai != null && dkSesuai.getMenggunakanDendaCustom() ? jml
                            : dkSesuai != null && (dkSesuai.getBatalkanDenda() || jml.intValue() == 0) ? jml
                            : db.checkDenda(jml, tglBayar, jdwKhusus,
                                    jadwal == null ? null : jadwal.getJenisKegiatan(), null);
                    if (dkSesuai != null && dkSesuai.getMenggunakanDendaCustom())
                        denda = nvl(dkSesuai.getDendaCustom());
                    else denda = hasil - jml;
                } else if (kunci.startsWith("PB_")) {
                    ppb = (PengaturanPembayaranBulanan) sess.get(PengaturanPembayaranBulanan.class,
                            Long.valueOf(kunci.substring(3)));
                    if (ppb == null) throw new IllegalArgumentException("Slot bulanan tidak ditemukan: " + kunci);
                    db = ppb.getDetailBiaya();
                    if (detailKegiatans != null) {
                        for (DetailKegiatan dk : detailKegiatans) {
                            if (dk.getPengaturanPembayaranBulanan() != null
                                    && dk.getPengaturanPembayaranBulanan().getId().equals(ppb.getId())) { dkSesuai = dk; break; }
                        }
                    }
                    Double jml = Kegiatan.ambilJumlahTagihan(dkSesuai, db, kegiatan, mhs, smt, ppb);
                    if (jml == null) jml = 0.0;
                    Double hasil = dkSesuai != null && (dkSesuai.getBatalkanDenda() || jml.intValue() == 0) ? jml
                            : dkSesuai != null && dkSesuai.getMenggunakanDendaCustom() ? jml
                            : ppb.checkDenda(jml, tglBayar, jdwKhusus,
                                    jadwal == null ? null : jadwal.getJenisKegiatan());
                    if (dkSesuai != null && dkSesuai.getMenggunakanDendaCustom())
                        denda = nvl(dkSesuai.getDendaCustom());
                    else denda = hasil - jml;
                } else {
                    throw new IllegalArgumentException("Kunci item tidak dikenal: " + kunci);
                }
                if (db == null) throw new IllegalArgumentException("Item biaya tidak valid: " + kunci);

                CicilanPembayaran cp = new CicilanPembayaran(db);
                cp.setKegiatan(kegiatan);
                cp.setKe(urutan++);
                cp.setDetailBiaya(db);
                cp.setItemBiaya(db.getItemBiaya());
                cp.setPengaturanPembayaranBulanan(ppb);
                cp.setNilai(nominal);
                cp.setDenda(denda);
                cp.setTanggal(tglBayar);
                cp.setTanggalKwitansi(tglKwitansi);
                cp.setKeterangan(ket);
                cp.setJenisPembayaran(caraBayar);
                cp.setValidator(user.getUserNama());
                // Bukti lampiran per item (hasil action `upload`); paritas
                // cicilanPembayaran.setIdLampiran pada onSave ZK.
                long idLampiran = it.optLong("idLampiran", 0L);
                if (idLampiran > 0L) cp.setIdLampiran(idLampiran);
                if (ppb != null && mhs != null) {
                    try { cp.setNilaiAsli(ppb.ambilNominalModifikasi(mhs, smt)); } catch (Exception ignored) { }
                }
                sess.save(cp);
                cicilanIds.add(cp.getId());
                totalBayar += nominal;
            }

            if (totalBayar > 0.1) {
                LogPembayaran log = new LogPembayaran();
                log.setKegiatan(kegiatan);
                log.setNominal(totalBayar);
                log.setKeterangan("Pembayaran manual");
                log.setValidator(user.getUserNama());
                Common.refreshSaveOrUpdate(sess, log);
            }

            sess.flush();
            Double[] d = kegiatan.hitungTotalDanDendaFromCicilan();
            double jumlah = nvl(d[0]); double dendaTotal = nvl(d[1]);
            kegiatan.setDenda(dendaTotal);
            kegiatan.setAmount(jumlah);
            kegiatan.setAmountTerhutang(nilaiHarusDibayar - (jumlah - dendaTotal));
            Common.refreshUpdate(sess, kegiatan);

            try { PembayaranUtil.getInstance().updateTunggakan(kegiatan, sess); }
            catch (Exception e) {
                try { ais.common.ErrorAuditUtil.record(e, "NewUiDaftarUlang updateTunggakan"); } catch (Exception ignored) { }
            }

            sess.flush();
            tx.commit();
            r.getSession().setAttribute(SESSION_GUARD, System.currentTimeMillis() + "\n" + signature);

            j.put("kegiatanId", kegiatan.getId());
            j.put("nominal", totalBayar);
            j.put("jumlahCicilan", cicilanIds.size());
            JSONArray idsArr = new JSONArray();
            for (Long idc : cicilanIds) idsArr.put(idc);
            j.put("cicilanIds", idsArr);
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) { try { tx.rollback(); } catch (Exception ignored) { } }
            throw e;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) { try { tx.rollback(); } catch (Exception ignored) { } }
            throw e;
        } finally {
            if (sess != null) { try { sess.close(); } catch (Exception ignored) { } }
        }
    }

    // -------------------------------------------------------------- util
    private static Date tanggal(String raw) {
        if (raw == null || raw.trim().length() == 0) return WaktuUtil.getDate();
        try { return new java.text.SimpleDateFormat("yyyy-MM-dd").parse(raw.trim()); }
        catch (Exception e) { throw new IllegalArgumentException("tanggal tidak valid (yyyy-MM-dd)."); }
    }

    private static boolean boleh(String kunci, boolean fallback) {
        try { return Common.bolehKonfigurasi(kunci); } catch (Exception e) { return fallback; }
    }

    private static boolean csrf(HttpServletRequest r) {
        Object e = r.getSession().getAttribute("newUiCsrfToken");
        String v = r.getHeader("X-CSRF-Token");
        return e != null && v != null && String.valueOf(e).equals(v);
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

    private static double nvl(Double v) { return v == null ? 0.0 : v.doubleValue(); }
    private static String nz(String v) { return v == null ? "" : v; }
    private static String text(String v, String f) { return v == null || v.trim().length() == 0 ? f : v.trim(); }

    private static void fail(JSONObject j, String c, String m) throws Exception {
        j.put("ok", false).put("code", c).put("message", m == null ? "Operasi ditolak." : m);
    }

    private static void write(HttpServletResponse r, JSONObject j) throws Exception {
        r.getWriter().write(j.toString());
    }
}
