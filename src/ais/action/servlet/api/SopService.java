package ais.action.servlet.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.sop.DisposisiSopAction;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.master.sop.helper.SopUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.ParameterTambahan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sop.AktorSop;
import ais.database.model.sop.AlurSop;
import ais.database.model.sop.DisposisiAlurSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.sop.DokumenAlurSop;
import ais.database.model.sop.KelompokParameterTambahanAlurSop;
import ais.database.model.sop.ParameterTambahanAlurSop;
import ais.database.model.sop.Sop;
import ais.ui.util.FormSop;

/**
 * Layanan REST/JSON modul SOP (Standard Operational Procedure) untuk aplikasi mobile — dasbor
 * ringkasan (mirip {@code DasboardSop.java}) dan alur disposisi/workflow satu pengajuan SOP
 * (mirip {@code TampilanAlurSopAction.java}). File ini TERPISAH dari service lain sesuai
 * permintaan; mengikuti konvensi paket {@code ais.action.servlet.api} (tanpa lambda / tanpa
 * try-with-resources agar kompatibel Java 1.7, static-method utility class, auth via
 * {@link ApiUtil#currentUser}, response envelope via {@link ApiHelperSupport}).
 *
 * <p>Dua konsep otorisasi yang dipakai konsisten dengan versi web:</p>
 * <ul>
 * <li><b>Pengaju/pemohon</b> — {@link AktorSop#buatCriterionPengaju(Tbmuser, String)}: user adalah
 * submitter langsung (mahasiswa/siswa/diajukanOleh), atau admin ("boleh melihat semua SOP").</li>
 * <li><b>Aktor/petugas</b> — {@link AktorSop#buatCriterion(Tbmuser, boolean, Criteria)} /
 * {@link SopUtil#resolveAktor}: role/username pada konfigurasi tahap, hierarki atasan pegawai,
 * atau aturan dosen (dosen PA/kaprodi/dekan).</li>
 * </ul>
 */
public final class SopService {

    private SopService() {
    }

    private static final int SAMPLE_LIMIT = 500;
    private static final String[] KODE_KEYS = { "kode", "nomor", "nomorSurat", "noSurat", "no_surat", "kodeTransaksi" };

    // ════════════════════════════════════════════════════════════════════════
    // 1) DASBOR SOP — action: "sop_dashboard"
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Ringkasan dasbor SOP: 6 angka utama (port {@code getCountXxx} dari {@code DasboardSop.java},
     * memakai COUNT query asli, bukan sampling) + analitik sebaran/deadline/kelengkapan data dari
     * sampel maksimal {@link #SAMPLE_LIMIT} baris terbaru (sama seperti web — sampling hanya untuk
     * breakdown, bukan angka utama).
     */
    public static JSONObject dashboard(HttpServletRequest req, JSONObject json) {
        JSONObject hasil = new JSONObject();
        Session session = null;
        try {
            Tbmuser tbmuser = ApiUtil.currentUser(json, req);
            if (tbmuser == null || tbmuser.getUserId() == null) {
                return ApiHelperSupport.status("97", "Token tidak sesuai");
            }
            Date mulai = parseTanggal(json, "mulai");
            Date sampai = parseTanggal(json, "sampai");
            String keyword = ApiHelperSupport.optString(json, "keyword").trim();
            // Penyaring ketiga toolbar dasbor ZKoss, di samping periode dan kata kunci.
            Long satkerId = ApiHelperSupport.isNullOrEmptyJsonValue(json, "satuanKerjaId") ? null
                    : Long.valueOf(Long.parseLong(ApiHelperSupport.optString(json, "satuanKerjaId")));

            session = HibernateUtil.getSessionFactory().openSession();

            int jumlahSopBaru = countCriteria(
                    satkerSop(criteriaPengajuanAnda(session, tbmuser, mulai, sampai, keyword), satkerId));
            int menungguSaya = countCriteria(satkerAlur(criteriaMenungguSaya(session, tbmuser, mulai, sampai, keyword, false), satkerId));
            int sudahSayaDisposisi = countCriteria(satkerAlur(criteriaSudahDisposisi(session, tbmuser, mulai, sampai, keyword), satkerId));
            int selesai = countCriteria(satkerAlur(criteriaSelesai(session, tbmuser, mulai, sampai, keyword), satkerId));
            int menungguAktor = countCriteria(satkerAlur(criteriaMenungguAktor(session, tbmuser, mulai, sampai, keyword), satkerId));
            int lewatDeadline = countCriteria(satkerAlur(criteriaMenungguSaya(session, tbmuser, mulai, sampai, keyword, true), satkerId));
            int totalAntrian = menungguSaya + menungguAktor;
            int totalAktivitas = jumlahSopBaru + menungguSaya + sudahSayaDisposisi + selesai + menungguAktor;

            JSONObject data = new JSONObject();
            data.put("jumlahSopBaru", jumlahSopBaru);
            data.put("menungguSaya", menungguSaya);
            data.put("sudahSayaDisposisi", sudahSayaDisposisi);
            data.put("selesai", selesai);
            data.put("menungguAktor", menungguAktor);
            data.put("lewatDeadline", lewatDeadline);
            data.put("totalAntrian", totalAntrian);
            data.put("totalAktivitas", totalAktivitas);

            Criteria sample = satkerAlur(criteriaDipantau(session, tbmuser, mulai, sampai, keyword), satkerId);
            sample.addOrder(Order.desc("id"));
            sample.setMaxResults(SAMPLE_LIMIT);

            @SuppressWarnings("unchecked")
            List<DisposisiAlurSop> rows = sample.list();
            JSONObject analytic = analyzeDashboardRows(rows);
            // "Proses Dipantau" adalah angka utama, jadi dihitung TEPAT lewat COUNT
            // DISTINCT -- bukan dari sampel 500 baris yang dipakai analitik sebaran.
            // Dari sampel, angkanya jenuh di batas sampel begitu data melewatinya, sehingga
            // instalasi sibuk selalu melihat angka yang sama dan keliru.
            int totalDipantau = hitungDipantauTepat(session, tbmuser, mulai, sampai, keyword, satkerId);
            if (totalDipantau <= 0) {
                totalDipantau = analytic.getInt("totalDipantau");
            }
            data.put("totalDipantau", totalDipantau > 0 ? totalDipantau : totalAktivitas);
            data.put("deadline", analytic.getJSONObject("deadline"));
            data.put("metadataQuality", analytic.getJSONObject("metadataQuality"));
            data.put("perSop", analytic.getJSONArray("perSop"));
            data.put("perAktor", analytic.getJSONArray("perAktor"));
            data.put("perBulan", analytic.getJSONArray("perBulan"));
            data.put("aktivitasTerbaru", analytic.getJSONArray("aktivitasTerbaru"));

            hasil.put("data", data);
            ApiHelperSupport.putSuccess(hasil, "OK");
            return hasil;
        } catch (Exception e) {
            return ApiHelperSupport.errorResponse("Gagal mengambil dasbor SOP");
        } finally {
            closeQuietly(session);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 2) DAFTAR SOP (terpaginasi, per kategori) — action: "sop_daftar"
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Daftar SOP terpaginasi per kategori — satu endpoint fleksibel dipakai untuk seluruh 6 kartu
     * dasbor SOP (menggantikan pola popup-detail-per-kartu di web dengan satu list ter-page):
     * {@code kategori} = {@code pengajuan_anda | menunggu_saya | sudah_disposisi | selesai |
     * menunggu_aktor | lewat_deadline}. Filter opsional: {@code mulai/sampai} (yyyy-MM-dd),
     * {@code keyword}, {@code sopNama}/{@code aktorTahap} (untuk tap-through dari breakdown
     * sebaran SOP / beban aktor pada dasbor), {@code page}/{@code limit}.
     */
    public static JSONObject daftar(HttpServletRequest req, JSONObject json) {
        JSONObject hasil = new JSONObject();
        Session session = null;
        try {
            Tbmuser tbmuser = ApiUtil.currentUser(json, req);
            if (tbmuser == null || tbmuser.getUserId() == null) {
                return ApiHelperSupport.status("97", "Token tidak sesuai");
            }
            String kategori = ApiHelperSupport.optString(json, "kategori").trim();
            if (!ApiHelperSupport.hasText(kategori)) {
                return ApiHelperSupport.status("97", "Kategori harus dipilih");
            }

            Date mulai = parseTanggal(json, "mulai");
            Date sampai = parseTanggal(json, "sampai");
            String keyword = ApiHelperSupport.optString(json, "keyword").trim();
            String sopNama = ApiHelperSupport.optString(json, "sopNama").trim();
            String aktorTahap = ApiHelperSupport.optString(json, "aktorTahap").trim();
            Long satkerId = ApiHelperSupport.isNullOrEmptyJsonValue(json, "satuanKerjaId") ? null
                    : Long.valueOf(Long.parseLong(ApiHelperSupport.optString(json, "satuanKerjaId")));

            int page = safeInt(json, "page", 1);
            if (page < 1) {
                page = 1;
            }
            int limit = safeInt(json, "limit", 10);
            if (limit < 1) {
                limit = 10;
            }
            if (limit > 100) {
                limit = 100;
            }
            int offset = (page - 1) * limit;

            session = HibernateUtil.getSessionFactory().openSession();

            JSONArray arr = new JSONArray();
            int total;

            if ("pengajuan_anda".equals(kategori)) {
                Criteria count = satkerSop(criteriaPengajuanAnda(session, tbmuser, mulai, sampai, keyword), satkerId);
                total = countCriteria(count);
                Criteria list = satkerSop(criteriaPengajuanAnda(session, tbmuser, mulai, sampai, keyword), satkerId);
                list.addOrder(Order.desc("id")).setFirstResult(offset).setMaxResults(limit);
                @SuppressWarnings("unchecked")
                List<DisposisiSop> rows = list.list();
                for (DisposisiSop d : rows) {
                    arr.put(mapDisposisiSopRow(session, d));
                }
            } else {
                Criteria count = satkerAlur(buildKategoriCriteria(session, tbmuser, kategori, mulai, sampai, keyword, sopNama, aktorTahap), satkerId);
                if (count == null) {
                    return ApiHelperSupport.status("97", "Kategori tidak dikenali");
                }
                total = countCriteria(count);
                Criteria list = satkerAlur(buildKategoriCriteria(session, tbmuser, kategori, mulai, sampai, keyword, sopNama, aktorTahap), satkerId);
                list.addOrder(Order.desc("id")).setFirstResult(offset).setMaxResults(limit);
                @SuppressWarnings("unchecked")
                List<DisposisiAlurSop> rows = list.list();
                for (DisposisiAlurSop d : rows) {
                    arr.put(mapDisposisiAlurSopRow(d));
                }
            }

            hasil.put("list", arr);
            hasil.put("total", total);
            hasil.put("page", page);
            hasil.put("limit", limit);
            ApiHelperSupport.putStatus(hasil, arr.length() > 0 ? "00" : "99",
                    arr.length() > 0 ? "OK" : "Belum ada data pada kategori ini");
            return hasil;
        } catch (Exception e) {
            return ApiHelperSupport.errorResponse("Gagal mengambil daftar SOP");
        } finally {
            closeQuietly(session);
        }
    }

    private static Criteria buildKategoriCriteria(Session session, Tbmuser tbmuser, String kategori, Date mulai,
            Date sampai, String keyword, String sopNama, String aktorTahap) {
        Criteria c;
        if ("menunggu_saya".equals(kategori)) {
            c = criteriaMenungguSaya(session, tbmuser, mulai, sampai, keyword, false);
        } else if ("lewat_deadline".equals(kategori)) {
            c = criteriaMenungguSaya(session, tbmuser, mulai, sampai, keyword, true);
        } else if ("sudah_disposisi".equals(kategori)) {
            c = criteriaSudahDisposisi(session, tbmuser, mulai, sampai, keyword);
        } else if ("selesai".equals(kategori)) {
            c = criteriaSelesai(session, tbmuser, mulai, sampai, keyword);
        } else if ("menunggu_aktor".equals(kategori)) {
            c = criteriaMenungguAktor(session, tbmuser, mulai, sampai, keyword);
        } else {
            return null;
        }
        if (ApiHelperSupport.hasText(sopNama)) {
            c.createAlias("disposisiSop.sop", "sopFilter", Criteria.LEFT_JOIN);
            c.add(Restrictions.eq("sopFilter.nama", sopNama));
        }
        if (ApiHelperSupport.hasText(aktorTahap)) {
            int idx = aktorTahap.indexOf(" - ");
            String aktor = idx > 0 ? aktorTahap.substring(0, idx) : aktorTahap;
            String tahap = idx > 0 ? aktorTahap.substring(idx + 3) : aktorTahap;
            c.add(Restrictions.or(Restrictions.eq("alurSop.aktor", aktor), Restrictions.eq("alurSop.nama", tahap)));
        }
        return c;
    }

    // ════════════════════════════════════════════════════════════════════════
    // 3) DETAIL + ALUR WORKFLOW SATU PENGAJUAN SOP — action: "sop_detail"
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Detail satu pengajuan SOP: header, riwayat langkah yang sudah diambil tindakan, banner
     * status selesai, tahap yang sedang menunggu (siapa berhak & apakah user saat ini berhak —
     * lewat {@link SopUtil#resolveAktor}), data form terkait (read-only, flatten), overview alur
     * kerja (semua tahap desain SOP + status masing-masing), dan flag {@code bisaBatalkanPengajuan}.
     * Ini adalah port dari {@code TampilanAlurSopAction.tampil(...)}.
     */
    public static JSONObject detail(HttpServletRequest req, JSONObject json) {
        JSONObject hasil = new JSONObject();
        Session session = null;
        try {
            Tbmuser tbmuser = ApiUtil.currentUser(json, req);
            if (tbmuser == null || tbmuser.getUserId() == null) {
                return ApiHelperSupport.status("97", "Token tidak sesuai");
            }
            if (ApiHelperSupport.isNullOrEmptyJsonValue(json, "disposisiSopId")) {
                return ApiHelperSupport.status("97", "Id pengajuan SOP harus dikirim");
            }
            Long id = Long.parseLong(ApiHelperSupport.optString(json, "disposisiSopId"));

            session = HibernateUtil.getSessionFactory().openSession();
            DisposisiSop disposisiSop = (DisposisiSop) session.get(DisposisiSop.class, id);
            if (disposisiSop == null) {
                return ApiHelperSupport.status("99", "Pengajuan SOP tidak ditemukan");
            }

            @SuppressWarnings("unchecked")
            List<DisposisiAlurSop> semuaLangkah = session.createCriteria(DisposisiAlurSop.class)
                    .add(Restrictions.isNotNull("alurSop")).add(Restrictions.eq("disposisiSop", disposisiSop))
                    .addOrder(Order.asc("id")).list();

            JSONObject data = new JSONObject();
            data.put("id", disposisiSop.getId());
            data.put("kode", kodePengajuan(disposisiSop, null));
            data.put("sop", disposisiSop.getSop() == null ? "" : ApiHelperSupport.safeString(disposisiSop.getSop().getNama()));
            data.put("pengaju", namaPengaju(disposisiSop));
            data.put("waktuPengajuan", disposisiSop.getWaktu() == null ? "" : Common.dateFormat51.get().format(disposisiSop.getWaktu()));
            data.put("keterangan", ApiHelperSupport.safeString(disposisiSop.getKeterangan()));

            JSONArray riwayat = new JSONArray();
            int jumlahDiambil = 0;
            DisposisiAlurSop terakhirDiambil = null;
            for (DisposisiAlurSop langkah : semuaLangkah) {
                boolean diambil = langkah.getDiajukanOleh() != null || langkah.getMahasiswa() != null
                        || langkah.getSiswa() != null;
                if (!diambil) {
                    continue;
                }
                jumlahDiambil++;
                terakhirDiambil = langkah;
                riwayat.put(mapRiwayatLangkah(langkah));
            }
            data.put("riwayat", riwayat);

            boolean selesaiTotal = false;
            String catatanSelesai = "";
            if (terakhirDiambil != null && terakhirDiambil.getWaktu() != null
                    && (Boolean.TRUE.equals(terakhirDiambil.getSelesai()) || tidakAdaLangkahLanjutan(terakhirDiambil.getAlurSop()))) {
                selesaiTotal = true;
                catatanSelesai = ApiHelperSupport.safeString(terakhirDiambil.getKeterangan());
            }
            data.put("selesai", selesaiTotal);
            data.put("catatanSelesai", catatanSelesai);

            JSONArray pending = new JSONArray();
            for (DisposisiAlurSop langkah : semuaLangkah) {
                boolean sudahDiambil = langkah.getDiajukanOleh() != null || langkah.getMahasiswa() != null
                        || langkah.getSiswa() != null;
                if (sudahDiambil || !Boolean.TRUE.equals(langkah.getAktif())) {
                    continue;
                }
                pending.put(mapTahapPending(session, tbmuser, disposisiSop, langkah));
            }
            data.put("tahapPending", pending);

            data.put("form", resolveFormDataReadOnly(session, disposisiSop, semuaLangkah));
            data.put("alur", buildWorkflowOverview(session, disposisiSop, semuaLangkah));

            boolean bisaBatalkan = false;
            try {
                if (disposisiSop.getDiajukanOleh() != null && ApiHelperSupport.hasText(tbmuser.getUserId())
                        && tbmuser.getUserId().equalsIgnoreCase(disposisiSop.getDiajukanOleh().getUserId())
                        && jumlahDiambil <= 1) {
                    bisaBatalkan = true;
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:350");
            }
            data.put("bisaBatalkanPengajuan", bisaBatalkan);

            hasil.put("data", data);
            ApiHelperSupport.putSuccess(hasil, "OK");
            return hasil;
        } catch (Exception e) {
            return ApiHelperSupport.errorResponse("Gagal mengambil detail SOP");
        } finally {
            closeQuietly(session);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 4) PENCARIAN SOP — action: "sop_cari"
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Pencarian pengajuan SOP yang dapat dilihat user (submitter/mahasiswa-siswa terkait, atau
     * pernah/berpotensi menjadi aktor pada salah satu tahapnya), port dari {@code loadData}/
     * {@code initCriteria} pada {@code TampilanAlurSopAction}. Untuk kesederhanaan cakupan,
     * pencocokan "aktor" di sini memakai pencocokan role/username langsung (bagian dasar
     * {@link AktorSop#buatCriterion}), tanpa ekspansi hierarki atasan/kaprodi/dekan — otorisasi
     * PENUH (termasuk hierarki) tetap dipakai di {@link #detail} untuk menentukan siapa yang
     * benar-benar berhak memproses suatu tahap.
     */
    public static JSONObject cari(HttpServletRequest req, JSONObject json) {
        JSONObject hasil = new JSONObject();
        Session session = null;
        try {
            Tbmuser tbmuser = ApiUtil.currentUser(json, req);
            if (tbmuser == null || tbmuser.getUserId() == null) {
                return ApiHelperSupport.status("97", "Token tidak sesuai");
            }
            String keyword = ApiHelperSupport.optString(json, "keyword").trim();

            session = HibernateUtil.getSessionFactory().openSession();
            Criteria c = session.createCriteria(DisposisiSop.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

            if (!AktorSop.bolehMelihatSemuaSop(tbmuser)) {
                Criterion pengajuOrSubjek = AktorSop.buatCriterionPengaju(tbmuser, "");
                List<Long> idsAsActor = cariDisposisiSopIdSebagaiAktor(session, tbmuser);
                c.add(idsAsActor.isEmpty() ? pengajuOrSubjek
                        : Restrictions.or(pengajuOrSubjek, Restrictions.in("id", idsAsActor)));
            }

            if (ApiHelperSupport.hasText(keyword)) {
                c.createAlias("sop", "sopCari", Criteria.LEFT_JOIN);
                c.add(Restrictions.or(Restrictions.ilike("properti", keyword, MatchMode.ANYWHERE),
                        Restrictions.or(Restrictions.ilike("keterangan", keyword, MatchMode.ANYWHERE),
                                Restrictions.ilike("sopCari.nama", keyword, MatchMode.ANYWHERE))));
            }

            c.addOrder(Order.desc("id")).setMaxResults(100);
            @SuppressWarnings("unchecked")
            List<DisposisiSop> rows = c.list();
            JSONArray arr = new JSONArray();
            for (DisposisiSop d : rows) {
                arr.put(mapDisposisiSopRow(session, d));
            }
            hasil.put("list", arr);
            ApiHelperSupport.putStatus(hasil, arr.length() > 0 ? "00" : "99",
                    arr.length() > 0 ? "OK" : "Tidak ditemukan");
            return hasil;
        } catch (Exception e) {
            return ApiHelperSupport.errorResponse("Gagal mencari SOP");
        } finally {
            closeQuietly(session);
        }
    }

    private static List<Long> cariDisposisiSopIdSebagaiAktor(Session session, Tbmuser tbmuser) {
        List<Long> ids = new ArrayList<Long>();
        try {
            String roleId = safeRoleId(tbmuser);
            Criteria c = session.createCriteria(DisposisiAlurSop.class).add(Restrictions.isNotNull("alurSop"))
                    .createAlias("alurSop", "alurSopSub")
                    .createAlias("alurSopSub.aktorSop", "aktorSopSub", Criteria.LEFT_JOIN)
                    .createAlias("disposisiSop", "disposisiSopSub")
                    .add(Restrictions.or(
                            Restrictions.ilike("aktorSopSub.jenisPengguna", "," + roleId + ",", MatchMode.ANYWHERE),
                            Restrictions.ilike("aktorSopSub.usernamePengguna", "," + safeString(tbmuser.getUserId()) + ",",
                                    MatchMode.ANYWHERE)))
                    .setProjection(Projections.distinct(Projections.property("disposisiSopSub.id"))).setMaxResults(500);
            @SuppressWarnings("unchecked")
            List<Long> found = c.list();
            if (found != null) {
                ids.addAll(found);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:441");
        }
        return ids;
    }

    // ════════════════════════════════════════════════════════════════════════
    // 5) PROSES DISPOSISI (approve/forward/reject/kembali/selesai) — action: "sop_proses"
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Menindaklanjuti satu tahap SOP yang sedang menunggu ({@code disposisiAlurSopId}, harus
     * salah satu entri {@code tahapPending} dari {@link #detail}). Port urutan tulis dari
     * {@code DisposisiAlurSopAction.onSave} (§2.9): validasi wajib (catatan/pilihan/parameter
     * tambahan/lampiran), tandai tahap ini sebagai diproses oleh user saat ini, lalu buat baris
     * placeholder untuk tahap berikutnya (mendukung multi-branch dan opsi "kembali ke alur
     * sebelumnya"), lalu perbarui {@code disposisiSop.disposisiEnd}/{@code disposisiSetuju} secara
     * kondisional persis seperti web. Hanya menangani tindakan PERTAMA KALI atas tahap yang masih
     * pending — mengedit tahap yang sudah diambil (fitur "Ubah" di web) TIDAK termasuk cakupan v1
     * ini (lihat catatan pada plan).
     *
     * <p>Input: {@code disposisiAlurSopId} (wajib), {@code keterangan}, dan salah satu dari:
     * {@code kembali=true} (kembali ke alur sebelumnya), {@code selesai=true} ("Setujui dan
     * Selesai"), atau {@code alurSopIds} (array id AlurSop tujuan, mendukung multi-pilih). Opsional:
     * {@code parameterTambahan} (array {@code {kelompokId, parameterId, nilai, catatan}}) — lampiran
     * terkait diunggah TERPISAH lewat servlet {@code DoUpload} sebelum memanggil aksi ini, dengan
     * {@code ref=disposisiAlurSopId} dan {@code jenis} sesuai {@code key} pada definisi parameter/
     * dokumen dari {@link #detail}.</p>
     */
    public static JSONObject proses(HttpServletRequest req, JSONObject json) {
        JSONObject hasil = new JSONObject();
        Session session = null;
        try {
            Tbmuser tbmuser = ApiUtil.currentUser(json, req);
            if (tbmuser == null || tbmuser.getUserId() == null) {
                return ApiHelperSupport.status("97", "Token tidak sesuai");
            }
            if (ApiHelperSupport.isNullOrEmptyJsonValue(json, "disposisiAlurSopId")) {
                return ApiHelperSupport.status("97", "Id tahap SOP harus dikirim");
            }
            Long id = Long.parseLong(ApiHelperSupport.optString(json, "disposisiAlurSopId"));

            session = HibernateUtil.getSessionFactory().openSession();
            DisposisiAlurSop disposisiAlurSop = (DisposisiAlurSop) session.get(DisposisiAlurSop.class, id);
            if (disposisiAlurSop == null || disposisiAlurSop.getAlurSop() == null) {
                return ApiHelperSupport.status("99", "Tahap SOP tidak ditemukan");
            }
            if (!Boolean.TRUE.equals(disposisiAlurSop.getAktif())) {
                return ApiHelperSupport.status("97", "Tahap ini sudah tidak aktif");
            }
            boolean sudahDiambil = disposisiAlurSop.getDiajukanOleh() != null || disposisiAlurSop.getMahasiswa() != null
                    || disposisiAlurSop.getSiswa() != null;
            if (sudahDiambil) {
                return ApiHelperSupport.status("97", "Tahap ini sudah diproses sebelumnya");
            }

            DisposisiSop disposisiSop = disposisiAlurSop.getDisposisiSop();
            AlurSop alurSop = disposisiAlurSop.getAlurSop();
            if (disposisiSop == null) {
                return ApiHelperSupport.status("99", "Pengajuan SOP terkait tidak ditemukan");
            }

            String jenisPengguna = alurSop.getAktorSop() != null ? alurSop.getAktorSop().getJenisPengguna() : "";
            SopUtil.AktorResolusi resolusi = SopUtil.resolveAktor(tbmuser, alurSop.getKhususUsername(), jenisPengguna, disposisiSop, alurSop);
            if (!resolusi.ada) {
                return ApiHelperSupport.status("97", "Anda tidak berhak memproses tahap ini");
            }

            String keterangan = ApiHelperSupport.optString(json, "keterangan").trim();
            boolean kembali = json.optBoolean("kembali", false);
            boolean selesai = json.optBoolean("selesai", false);
            JSONArray alurSopIdsInput = json.has("alurSopIds") && !json.isNull("alurSopIds") ? json.optJSONArray("alurSopIds") : null;
            JSONArray parameterTambahanInput = json.has("parameterTambahan") && !json.isNull("parameterTambahan")
                    ? json.optJSONArray("parameterTambahan") : null;

            if (Boolean.TRUE.equals(alurSop.getCatatanWajibDiisi()) && !ApiHelperSupport.hasText(keterangan)) {
                return ApiHelperSupport.status("97", "Catatan/keterangan harus diisi");
            }

            if (kembali) {
                if (disposisiAlurSop.getSebelumnya() == null || !Boolean.TRUE.equals(alurSop.getKembaliKeAktorSebelumnya())) {
                    return ApiHelperSupport.status("97", "Opsi kembali ke alur sebelumnya tidak tersedia untuk tahap ini");
                }
            } else if (selesai) {
                if (!Boolean.TRUE.equals(alurSop.getJikaProsesDisetujuiMakaSelesai())) {
                    return ApiHelperSupport.status("97", "Opsi setujui dan selesai tidak tersedia untuk tahap ini");
                }
            } else if (!Boolean.TRUE.equals(alurSop.getAlurSetelahnyaTidakWajib())
                    && (alurSopIdsInput == null || alurSopIdsInput.length() == 0)) {
                return ApiHelperSupport.status("97", "Pilihan tindak lanjut/langkah berikutnya harus dipilih");
            }

            String errParam = validateParameterTambahanWajib(session, alurSop, parameterTambahanInput, disposisiAlurSop.getId());
            if (errParam != null) {
                return ApiHelperSupport.status("97", errParam);
            }
            String errDok = validateDokumenWajib(alurSop, disposisiAlurSop.getId());
            if (errDok != null) {
                return ApiHelperSupport.status("97", errDok);
            }
            if (Common.bolehKonfigurasi("tampilkan_lampiran_catatan_disposisi") && Boolean.TRUE.equals(alurSop.getLampiranCatatanWajibDiisi())) {
                LampiranLain lam = LampiranLain.ambil(disposisiAlurSop.getId(), "Lampiran Catatan Disposisi");
                if (lam == null) {
                    return ApiHelperSupport.status("97", "Lampiran catatan disposisi harus diunggah terlebih dahulu");
                }
            }

            List<AlurSop> nextNodes = new ArrayList<AlurSop>();
            if (!kembali && !selesai && alurSopIdsInput != null) {
                for (int i = 0; i < alurSopIdsInput.length(); i++) {
                    try {
                        Long nextId = alurSopIdsInput.getLong(i);
                        AlurSop next = (AlurSop) session.get(AlurSop.class, nextId);
                        if (next != null) {
                            nextNodes.add(next);
                        }
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:556");
                    }
                }
            }

            // ── Transaksi utama: tandai tahap ini sebagai sudah diproses oleh user saat ini ──
            session.getTransaction().begin();
            if (tbmuser.getMahasiswa() != null) {
                disposisiAlurSop.setMahasiswa(tbmuser.getMahasiswa());
            } else if (tbmuser.getSiswa() != null) {
                disposisiAlurSop.setSiswa(tbmuser.getSiswa());
            } else {
                disposisiAlurSop.setDiajukanOleh(tbmuser);
            }
            // Waktu disposisi. ZKoss menampilkannya sebagai kolom yang BISA diubah hanya
            // bila tahap dikonfigurasi tanggalDisposisiBolehDiubah; selain itu ditampilkan
            // sbg label mati dan memakai waktu server. Sebelumnya API selalu memaksa waktu
            // server, sehingga tahap yang SENGAJA dibuka untuk koreksi tanggal tidak bisa
            // dipakai dari POS/mobile.
            Date waktuDisposisi = new Date();
            if (Boolean.TRUE.equals(alurSop.getTanggalDisposisiBolehDiubah())
                    && ApiHelperSupport.hasText(ApiHelperSupport.optString(json, "waktu"))) {
                try {
                    waktuDisposisi = Common.dateFormat3.get().parse(
                            ApiHelperSupport.optString(json, "waktu").trim());
                } catch (Exception eWaktu) {
                    ais.common.ErrorAuditUtil.record(eWaktu, "SopService.proses.waktu");
                }
            }
            disposisiAlurSop.setWaktu(waktuDisposisi);
            disposisiAlurSop.setKeterangan(keterangan);
            disposisiAlurSop.setUsernamePengguna(ApiHelperSupport.safeString(tbmuser.getUserId()));
            disposisiAlurSop.setKembali(kembali);
            disposisiAlurSop.setSelesai(selesai);
            if (parameterTambahanInput != null) {
                disposisiAlurSop.setParameterTambahanInds(encodeParameterTambahanInds(parameterTambahanInput, disposisiAlurSop.getId()));
            }
            Common.refreshSaveOrUpdate(session, disposisiAlurSop);
            session.getTransaction().commit();

            // ── Transaksi lanjutan: buat baris placeholder untuk tahap berikutnya ──
            try {
                session.getTransaction().begin();
                DisposisiAlurSop langkahSetelah = null;
                if (kembali) {
                    langkahSetelah = cariAtauBuatDisposisiAlurSop(session, disposisiSop,
                            disposisiAlurSop.getSebelumnya().getAlurSop(), disposisiAlurSop);
                } else if (!nextNodes.isEmpty()) {
                    for (AlurSop nextNode : nextNodes) {
                        DisposisiAlurSop dibuat = cariAtauBuatDisposisiAlurSop(session, disposisiSop, nextNode, disposisiAlurSop);
                        if (dibuat != null) {
                            langkahSetelah = dibuat;
                        }
                    }
                }

                if (langkahSetelah != null) {
                    disposisiAlurSop.setSetelahnya(langkahSetelah);
                    session.update(disposisiAlurSop);
                }

                boolean isSetuju = selesai || disposisiAlurSop.setujui();
                if (isSetuju && (disposisiSop.getDisposisiSetuju() == null || disposisiSop.getDisposisiSetuju().getId() == null
                        || (disposisiAlurSop.getId() != null && disposisiSop.getDisposisiSetuju().getId() < disposisiAlurSop.getId()))) {
                    disposisiSop.setDisposisiSetuju(disposisiAlurSop);
                }
                if (disposisiSop.getDisposisiEnd() == null || disposisiSop.getDisposisiEnd().getId() == null
                        || (disposisiAlurSop.getId() != null && disposisiSop.getDisposisiEnd().getId() < disposisiAlurSop.getId())) {
                    disposisiSop.setDisposisiEnd(disposisiAlurSop);
                }
                session.update(disposisiSop);
                session.getTransaction().commit();
            } catch (Exception eInner) {
                ApiHelperSupport.rollbackQuietly(session.getTransaction());
            }

            try {
                TampilanAlurSopAction.cetakDisposisi(disposisiSop, true);
            } catch (Exception eNotif) { ais.common.ErrorAuditUtil.record(eNotif, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:619");
            }

            hasil.put("disposisiSopId", disposisiSop.getId());
            hasil.put("disposisiAlurSopId", disposisiAlurSop.getId());
            ApiHelperSupport.putSuccess(hasil, "Disposisi berhasil diproses");
            return hasil;
        } catch (Exception e) {
            return ApiHelperSupport.errorResponse("Gagal memproses disposisi SOP");
        } finally {
            closeQuietly(session);
        }
    }

    private static DisposisiAlurSop cariAtauBuatDisposisiAlurSop(Session session, DisposisiSop disposisiSop,
            AlurSop alurSopTujuan, DisposisiAlurSop sebelumnya) {
        try {
            DisposisiAlurSop existing = (DisposisiAlurSop) session.createCriteria(DisposisiAlurSop.class)
                    .add(Restrictions.eq("disposisiSop", disposisiSop)).add(Restrictions.eq("alurSop", alurSopTujuan))
                    .add(Restrictions.eq("sebelumnya", sebelumnya)).setMaxResults(1).uniqueResult();
            if (existing != null) {
                return existing;
            }
            DisposisiAlurSop baru = new DisposisiAlurSop();
            baru.setDisposisiSop(disposisiSop);
            baru.setAlurSop(alurSopTujuan);
            baru.setSebelumnya(sebelumnya);
            session.save(baru);
            return baru;
        } catch (Exception e) {
            return null;
        }
    }

    private static String encodeParameterTambahanInds(JSONArray input, Long disposisiAlurSopId) {
        if (input == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            try {
                JSONObject item = input.getJSONObject(i);
                String kelompokId = String.valueOf(item.get("kelompokId"));
                String parameterId = String.valueOf(item.get("parameterId"));
                String nilai = item.has("nilai") && !item.isNull("nilai") ? String.valueOf(item.get("nilai")) : "";
                String key = kelompokId + "->" + parameterId;
                String url = "";
                try {
                    LampiranLain lam = disposisiAlurSopId == null ? null : LampiranLain.ambil(disposisiAlurSopId, key);
                    if (lam != null) {
                        url = lam.createLinkUri();
                    }
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:671");
                }
                String catatan = item.has("catatan") && !item.isNull("catatan") ? String.valueOf(item.get("catatan")) : "";
                String line = key + "<=>" + nilai + "<=>" + url + "<=>" + catatan;
                sb.append(sb.length() == 0 ? line : "\n" + line);
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:676");
            }
        }
        return sb.toString();
    }

    private static String validateParameterTambahanWajib(Session session, AlurSop alurSop, JSONArray input, Long disposisiAlurSopId) {
        try {
            Map<String, String> provided = new HashMap<String, String>();
            if (input != null) {
                for (int i = 0; i < input.length(); i++) {
                    JSONObject item = input.optJSONObject(i);
                    if (item == null) {
                        continue;
                    }
                    String key = String.valueOf(item.opt("kelompokId")) + "->" + String.valueOf(item.opt("parameterId"));
                    provided.put(key, item.has("nilai") && !item.isNull("nilai") ? String.valueOf(item.get("nilai")) : "");
                }
            }
            for (KelompokParameterTambahanAlurSop kelompok : alurSop.getKelompokParameterTambahanAlurSops()) {
                if (kelompok == null) {
                    continue;
                }
                List<ParameterTambahanAlurSop> daftar = ConstantValues.simpleList(
                        session.createCriteria(ParameterTambahanAlurSop.class)
                                .add(Restrictions.eq("kelompokParameterTambahanAlurSop", kelompok))
                                .createAlias("parameterTambahan", "pt").add(Restrictions.eq("pt.aktif", true)),
                        ParameterTambahanAlurSop.class);
                if (daftar == null) {
                    continue;
                }
                for (ParameterTambahanAlurSop pas : daftar) {
                    ParameterTambahan pt = pas.getParameterTambahan();
                    if (pt == null) {
                        continue;
                    }
                    String key = kelompok.getId() + "->" + pt.getId();
                    if (Boolean.TRUE.equals(pt.getWajibDiisi()) && !ApiHelperSupport.hasText(provided.get(key))) {
                        return "Isian \"" + pt.getLabelInputan() + "\" harus diisi";
                    }
                    if (Boolean.TRUE.equals(pt.getHarusMenyertakanLampiran()) && Boolean.TRUE.equals(pt.getLampiranWajibDiisi())) {
                        LampiranLain lam = disposisiAlurSopId == null ? null : LampiranLain.ambil(disposisiAlurSopId, key);
                        if (lam == null) {
                            return "Lampiran untuk \"" + pt.getLabelInputan() + "\" harus diunggah terlebih dahulu";
                        }
                    }
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:724");
        }
        return null;
    }

    private static String validateDokumenWajib(AlurSop alurSop, Long disposisiAlurSopId) {
        try {
            for (DokumenAlurSop dok : alurSop.getDokumenAlurSops()) {
                if (dok == null || !Boolean.TRUE.equals(dok.getAktif()) || !Boolean.TRUE.equals(dok.getWajib())) {
                    continue;
                }
                String jenis = DokumenAlurSop.class.getName() + "_alur_" + dok.getId();
                LampiranLain lam = disposisiAlurSopId == null ? null : LampiranLain.ambil(disposisiAlurSopId, jenis);
                if (lam == null) {
                    return "Dokumen \"" + dok.getNama() + "\" wajib diunggah terlebih dahulu";
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:741");
        }
        return null;
    }

    // ════════════════════════════════════════════════════════════════════════
    // 6) BATALKAN PENGAJUAN (soft delete, submitter-only) — action: "sop_batalkan_pengajuan"
    // ════════════════════════════════════════════════════════════════════════

    /** Port {@code batalkan} pada TampilanAlurSopAction §3.1: soft delete, hanya submitter, hanya jika belum diproses lebih dari 1 tahap. */
    public static JSONObject batalkanPengajuan(HttpServletRequest req, JSONObject json) {
        JSONObject hasil = new JSONObject();
        Session session = null;
        try {
            Tbmuser tbmuser = ApiUtil.currentUser(json, req);
            if (tbmuser == null || tbmuser.getUserId() == null) {
                return ApiHelperSupport.status("97", "Token tidak sesuai");
            }
            if (ApiHelperSupport.isNullOrEmptyJsonValue(json, "disposisiSopId")) {
                return ApiHelperSupport.status("97", "Id pengajuan SOP harus dikirim");
            }
            Long id = Long.parseLong(ApiHelperSupport.optString(json, "disposisiSopId"));

            session = HibernateUtil.getSessionFactory().openSession();
            DisposisiSop disposisiSop = (DisposisiSop) session.get(DisposisiSop.class, id);
            if (disposisiSop == null) {
                return ApiHelperSupport.status("99", "Pengajuan SOP tidak ditemukan");
            }
            if (disposisiSop.getDiajukanOleh() == null || !ApiHelperSupport.hasText(tbmuser.getUserId())
                    || !tbmuser.getUserId().equalsIgnoreCase(disposisiSop.getDiajukanOleh().getUserId())) {
                return ApiHelperSupport.status("97", "Hanya pengaju yang dapat membatalkan pengajuan ini");
            }
            if (hitungLangkahDiambil(session, disposisiSop) > 1) {
                return ApiHelperSupport.status("97", "Pengajuan tidak dapat dibatalkan karena sudah diproses lebih dari satu tahap");
            }

            session.getTransaction().begin();
            disposisiSop.setAktif(false);
            Common.refreshUpdate(session, disposisiSop);
            session.getTransaction().commit();

            ApiHelperSupport.putSuccess(hasil, "Pengajuan berhasil dibatalkan");
            return hasil;
        } catch (Exception e) {
            return ApiHelperSupport.errorResponse("Gagal membatalkan pengajuan SOP");
        } finally {
            closeQuietly(session);
        }
    }

    private static int hitungLangkahDiambil(Session session, DisposisiSop disposisiSop) {
        try {
            Number n = (Number) session.createCriteria(DisposisiAlurSop.class)
                    .add(Restrictions.eq("disposisiSop", disposisiSop)).add(Restrictions.isNotNull("alurSop"))
                    .add(Restrictions.or(Restrictions.isNotNull("diajukanOleh"),
                            Restrictions.or(Restrictions.isNotNull("mahasiswa"), Restrictions.isNotNull("siswa"))))
                    .setProjection(Projections.rowCount()).uniqueResult();
            return n == null ? 0 : n.intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 7) BATALKAN SATU TAHAP PENDING (actor-only) — action: "sop_batalkan_langkah"
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Port {@code appendTombolBatalMenungguJikaAktor} §3.2: replikasi PERSIS percabangan
     * start-vs-non-start — tahap start → hard delete SEMUA {@code DisposisiAlurSop} lalu
     * {@code DisposisiSop} induknya; tahap non-start → lepas FK yang menunjuk ke baris ini lalu
     * hard delete baris itu saja.
     */
    public static JSONObject batalkanLangkah(HttpServletRequest req, JSONObject json) {
        JSONObject hasil = new JSONObject();
        Session session = null;
        try {
            Tbmuser tbmuser = ApiUtil.currentUser(json, req);
            if (tbmuser == null || tbmuser.getUserId() == null) {
                return ApiHelperSupport.status("97", "Token tidak sesuai");
            }
            if (ApiHelperSupport.isNullOrEmptyJsonValue(json, "disposisiAlurSopId")) {
                return ApiHelperSupport.status("97", "Id tahap harus dikirim");
            }
            Long id = Long.parseLong(ApiHelperSupport.optString(json, "disposisiAlurSopId"));

            session = HibernateUtil.getSessionFactory().openSession();
            DisposisiAlurSop langkah = (DisposisiAlurSop) session.get(DisposisiAlurSop.class, id);
            if (langkah == null || langkah.getAlurSop() == null) {
                return ApiHelperSupport.status("99", "Tahap tidak ditemukan");
            }
            boolean sudahDiambil = langkah.getDiajukanOleh() != null || langkah.getMahasiswa() != null || langkah.getSiswa() != null;
            if (sudahDiambil) {
                return ApiHelperSupport.status("97", "Tahap ini sudah diproses dan tidak dapat dibatalkan");
            }

            DisposisiSop disposisiSop = langkah.getDisposisiSop();
            AlurSop alurSop = langkah.getAlurSop();
            String jenisPengguna = alurSop.getAktorSop() != null ? alurSop.getAktorSop().getJenisPengguna() : "";
            SopUtil.AktorResolusi resolusi = SopUtil.resolveAktor(tbmuser, alurSop.getKhususUsername(), jenisPengguna, disposisiSop, alurSop);
            if (!resolusi.ada) {
                return ApiHelperSupport.status("97", "Anda tidak berhak membatalkan tahap ini");
            }

            Long disposisiSopId = disposisiSop == null ? null : disposisiSop.getId();

            if (Boolean.TRUE.equals(alurSop.getStart())) {
                session.getTransaction().begin();
                session.createSQLQuery("delete from disposisi_alur_sop where disposisi_sop=" + disposisiSopId).executeUpdate();
                session.getTransaction().commit();
                if (disposisiSopId != null) {
                    Session s2 = null;
                    try {
                        s2 = HibernateUtil.getSessionFactory().openSession();
                        s2.getTransaction().begin();
                        s2.createSQLQuery("delete from disposisi_sop where id=" + disposisiSopId).executeUpdate();
                        s2.getTransaction().commit();
                    } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:858");
                    } finally {
                        closeQuietly(s2);
                    }
                }
            } else {
                boolean ubah = false;
                if (disposisiSop.getDisposisiEnd() != null && disposisiSop.getDisposisiEnd().getId() != null
                        && disposisiSop.getDisposisiEnd().getId().equals(langkah.getId())) {
                    disposisiSop.setDisposisiEnd(null);
                    ubah = true;
                }
                if (disposisiSop.getDisposisiSetuju() != null && disposisiSop.getDisposisiSetuju().getId() != null
                        && disposisiSop.getDisposisiSetuju().getId().equals(langkah.getId())) {
                    disposisiSop.setDisposisiSetuju(null);
                    ubah = true;
                }
                if (disposisiSop.getDisposisiStart() != null && disposisiSop.getDisposisiStart().getId() != null
                        && disposisiSop.getDisposisiStart().getId().equals(langkah.getId())) {
                    disposisiSop.setDisposisiStart(null);
                    ubah = true;
                }
                session.getTransaction().begin();
                if (ubah) {
                    session.update(disposisiSop);
                }
                DisposisiAlurSop hapus = (DisposisiAlurSop) session.get(DisposisiAlurSop.class, langkah.getId());
                if (hapus != null) {
                    session.delete(hapus);
                }
                session.getTransaction().commit();
            }

            ApiHelperSupport.putSuccess(hasil, "Tahap berhasil dibatalkan");
            return hasil;
        } catch (Exception e) {
            return ApiHelperSupport.errorResponse("Gagal membatalkan tahap SOP");
        } finally {
            closeQuietly(session);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 8) CETAK PDF DISPOSISI — action: "sop_cetak"
    // ════════════════════════════════════════════════════════════════════════

    /** Reuse langsung {@link TampilanAlurSopAction#cetakDisposisi(DisposisiSop, boolean)} — bukan reimplementasi. */
    public static JSONObject cetak(HttpServletRequest req, JSONObject json) {
        JSONObject hasil = new JSONObject();
        try {
            Tbmuser tbmuser = ApiUtil.currentUser(json, req);
            if (tbmuser == null || tbmuser.getUserId() == null) {
                return ApiHelperSupport.status("97", "Token tidak sesuai");
            }
            if (ApiHelperSupport.isNullOrEmptyJsonValue(json, "disposisiSopId")) {
                return ApiHelperSupport.status("97", "Id pengajuan SOP harus dikirim");
            }
            Long id = Long.parseLong(ApiHelperSupport.optString(json, "disposisiSopId"));
            DisposisiSop disposisiSop = (DisposisiSop) ConstantValues.ambil(DisposisiSop.class.getName(), id);
            if (disposisiSop == null) {
                return ApiHelperSupport.status("99", "Pengajuan SOP tidak ditemukan");
            }

            java.io.File file = TampilanAlurSopAction.cetakDisposisi(disposisiSop, false);
            if (file == null || !file.exists()) {
                return ApiHelperSupport.status("99", "Berkas cetak belum dapat dibuat");
            }
            String url = Common.getRequestHostWithProtocolSimple() + file.getAbsolutePath().split("webapps")[1];
            hasil.put("url", url);
            ApiHelperSupport.putSuccess(hasil, "OK");
            return hasil;
        } catch (Exception e) {
            return ApiHelperSupport.errorResponse("Gagal mencetak SOP");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 9) PENGAJUAN SOP BARU — port DisposisiSopAction.java ("Tambah" / onAddExternal)
    //    action: "sop_jenis", "sop_mulai_info", "sop_cari_entitas", "sop_ajukan"
    // ════════════════════════════════════════════════════════════════════════

    private static final String LAMPIRAN_CATATAN_DISPOSISI_KEY = "Lampiran Catatan Disposisi";

    /** Daftar jenis SOP yang bisa DIMULAI oleh user saat ini — port kombinasi filter AlurSop.start di §2 dan {@link DisposisiSopAction#createCriterionSop}. */
    public static JSONObject jenis(HttpServletRequest req, JSONObject json) {
        JSONObject hasil = new JSONObject();
        Session session = null;
        try {
            Tbmuser tbmuser = ApiUtil.currentUser(json, req);
            if (tbmuser == null || tbmuser.getUserId() == null) {
                return ApiHelperSupport.status("97", "Token tidak sesuai");
            }
            session = HibernateUtil.getSessionFactory().openSession();
            List<Sop> daftar = daftarSopBisaDiajukan(session, tbmuser);
            JSONArray arr = new JSONArray();
            for (Sop s : daftar) {
                JSONObject o = new JSONObject();
                o.put("id", s.getId());
                o.put("kode", ApiHelperSupport.safeString(s.getKode()));
                o.put("nama", ApiHelperSupport.safeString(s.getNama()));
                o.put("keterangan", ApiHelperSupport.safeString(s.getKeterangan()));
                o.put("jenisSop", s.getJenisSop() == null ? "" : ApiHelperSupport.safeString(s.getJenisSop().getNama()));
                arr.put(o);
            }
            hasil.put("list", arr);
            ApiHelperSupport.putStatus(hasil, arr.length() > 0 ? "00" : "99",
                    arr.length() > 0 ? "OK" : "Tidak ada jenis SOP yang dapat Anda ajukan");
            return hasil;
        } catch (Exception e) {
            return ApiHelperSupport.errorResponse("Gagal mengambil daftar jenis SOP");
        } finally {
            closeQuietly(session);
        }
    }

    private static List<Sop> daftarSopBisaDiajukan(Session session, Tbmuser tbmuser) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> sopIds = session.createCriteria(AlurSop.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .add(Restrictions.eq("start", true)).setProjection(Projections.groupProperty("sop.id"))
                    .createAlias("aktorSop", "aktorSop").add(AktorSop.buatCriterion(tbmuser)).list();

            Criteria c = session.createCriteria(Sop.class)
                    .add(sopIds.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", sopIds))
                    .add(DisposisiSopAction.createCriterionSop(tbmuser)).addOrder(Order.asc("nama"));
            @SuppressWarnings("unchecked")
            List<Sop> list = c.list();
            return list;
        } catch (Exception e) {
            return new ArrayList<Sop>();
        }
    }

    /**
     * Info lengkap untuk memulai pengajuan SOP baru: tahap start, opsi tindak lanjut, definisi
     * parameter tambahan/dokumen (sama seperti tahap pending biasa), PLUS definisi field data
     * dinamis (skalar/tanggal/boolean/relasi) untuk entitas {@code FormSop} milik tahap start
     * (bila SOP ini punya {@code formInputan}) — port §3/§4 dari {@code DisposisiSopAction}.
     * Juga mengembalikan {@code refSementara}: id placeholder negatif untuk mengunggah lampiran
     * (dokumen/parameter/lampiran catatan) SEBELUM pengajuan benar-benar disimpan — persis pola
     * {@code ref = -Common.randLong()} pada web, dipetakan ulang ke id asli saat {@code sop_ajukan}.
     */
    public static JSONObject mulaiInfo(HttpServletRequest req, JSONObject json) {
        JSONObject hasil = new JSONObject();
        Session session = null;
        try {
            Tbmuser tbmuser = ApiUtil.currentUser(json, req);
            if (tbmuser == null || tbmuser.getUserId() == null) {
                return ApiHelperSupport.status("97", "Token tidak sesuai");
            }
            if (ApiHelperSupport.isNullOrEmptyJsonValue(json, "sopId")) {
                return ApiHelperSupport.status("97", "SOP harus dipilih");
            }
            Long sopId = Long.parseLong(ApiHelperSupport.optString(json, "sopId"));

            session = HibernateUtil.getSessionFactory().openSession();
            List<Sop> boleh = daftarSopBisaDiajukan(session, tbmuser);
            Sop sop = null;
            for (Sop s : boleh) {
                if (s.getId() != null && s.getId().equals(sopId)) {
                    sop = s;
                    break;
                }
            }
            if (sop == null) {
                return ApiHelperSupport.status("97", "Anda tidak berhak mengajukan SOP ini");
            }

            AlurSop alurSop = resolveStartAlurSop(session, sop);
            if (alurSop == null) {
                return ApiHelperSupport.status("99", "Alur Awal (start) untuk SOP ini tidak ditemukan");
            }

            JSONObject data = new JSONObject();
            data.put("sopId", sop.getId());
            data.put("sopNama", ApiHelperSupport.safeString(sop.getNama()));
            data.put("alurSopId", alurSop.getId());
            data.put("tahap", ApiHelperSupport.safeString(alurSop.getNama()));
            data.put("catatanWajib", alurSop.getCatatanWajibDiisi() == null || Boolean.TRUE.equals(alurSop.getCatatanWajibDiisi()));
            data.put("bolehDiisiCatatan", Boolean.TRUE.equals(alurSop.getBolehDiisiCatatan()));
            data.put("tanggalBolehDiubah", Boolean.TRUE.equals(alurSop.getTanggalDisposisiBolehDiubah()));
            data.put("lampiranCatatanWajib", Boolean.TRUE.equals(alurSop.getLampiranCatatanWajibDiisi())
                    && Common.bolehKonfigurasi("tampilkan_lampiran_catatan_disposisi"));
            data.put("nextOptions", buildNextOptions(alurSop));
            data.put("berupaPilihanTunggal", Boolean.TRUE.equals(alurSop.getAlurSetelahnyaBerupaPilihan()));
            data.put("nextTidakWajib", Boolean.TRUE.equals(alurSop.getAlurSetelahnyaTidakWajib()));
            data.put("parameterDefinisi", buildParameterDefinisi(session, alurSop));
            data.put("dokumenDefinisi", buildDokumenDefinisi(alurSop));

            String formClass = alurSop.getFormInputan();
            if (ApiHelperSupport.hasText(formClass)) {
                try {
                    Object obj = Class.forName(formClass.trim()).newInstance();
                    if (obj instanceof FormSop) {
                        FormSop formSop = (FormSop) obj;
                        data.put("formClass", formClass);
                        String istilah = "";
                        try {
                            istilah = formSop.istilah();
                        } catch (Exception e) {
                        }
                        data.put("formIstilah", ApiHelperSupport.hasText(istilah) ? istilah : "Data");
                        data.put("formFields", buildFormInputDefinisi(formSop.ambilClass()));
                    }
                } catch (Exception e) {
                }
            }

            Long refSementara = -Math.abs(Common.randLong());
            data.put("refSementara", refSementara);

            hasil.put("data", data);
            ApiHelperSupport.putSuccess(hasil, "OK");
            return hasil;
        } catch (Exception e) {
            return ApiHelperSupport.errorResponse("Gagal mengambil info pengajuan SOP");
        } finally {
            closeQuietly(session);
        }
    }

    private static AlurSop resolveStartAlurSop(Session session, Sop sop) {
        try {
            return (AlurSop) session.createCriteria(AlurSop.class).add(Restrictions.eq("sop", sop))
                    .add(Restrictions.eq("start", true)).addOrder(Order.asc("id")).setMaxResults(1).uniqueResult();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Pencarian entitas GENERIK (dipakai sebagai picker untuk field ber-tipe "relasi" pada form
     * data dinamis manapun) — mencari berdasarkan {@code nama} via Hibernate Criteria. Hanya
     * menerima class yang benar-benar ter-mapping Hibernate & merupakan turunan
     * {@link GeneralValueObject} (divalidasi via {@link HibernateUtil#getClassMetadata}, BUKAN
     * dengan meng-instantiate class sembarangan dari input pengguna), dan hanya mengembalikan
     * {@code id/nama/kode} — dirancang agar aman dipakai lintas SEMUA jenis relasi pada 50+ class
     * *Action yang mengimplementasikan FormSop, tanpa endpoint terpisah per jenis relasi.
     */
    public static JSONObject cariEntitas(HttpServletRequest req, JSONObject json) {
        JSONObject hasil = new JSONObject();
        Session session = null;
        try {
            Tbmuser tbmuser = ApiUtil.currentUser(json, req);
            if (tbmuser == null || tbmuser.getUserId() == null) {
                return ApiHelperSupport.status("97", "Token tidak sesuai");
            }
            String clazzName = ApiHelperSupport.optString(json, "clazz").trim();
            String keyword = ApiHelperSupport.optString(json, "keyword").trim();
            if (!ApiHelperSupport.hasText(clazzName)) {
                return ApiHelperSupport.status("97", "Jenis data pencarian harus dikirim");
            }

            Class<?> clazz;
            try {
                clazz = Class.forName(clazzName);
            } catch (Exception e) {
                return ApiHelperSupport.status("97", "Jenis data tidak dikenali");
            }
            if (!GeneralValueObject.class.isAssignableFrom(clazz)) {
                return ApiHelperSupport.status("97", "Jenis data tidak didukung untuk pencarian");
            }

            session = HibernateUtil.getSessionFactory().openSession();
            org.hibernate.metadata.ClassMetadata meta = HibernateUtil.getClassMetadata(clazz);
            if (meta == null) {
                return ApiHelperSupport.status("97", "Jenis data tidak didukung untuk pencarian");
            }

            Criteria c = session.createCriteria(clazz);
            boolean adaAktif = false;
            for (String p : meta.getPropertyNames()) {
                if ("aktif".equals(p)) {
                    adaAktif = true;
                    break;
                }
            }
            if (adaAktif) {
                c.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
            }
            if (ApiHelperSupport.hasText(keyword)) {
                c.add(Restrictions.ilike("nama", keyword, MatchMode.ANYWHERE));
            }
            c.addOrder(Order.asc("nama")).setMaxResults(20);

            @SuppressWarnings("unchecked")
            List<GeneralValueObject> list = c.list();
            JSONArray arr = new JSONArray();
            for (GeneralValueObject o : list) {
                try {
                    JSONObject jo = new JSONObject();
                    jo.put("id", o.getId());
                    jo.put("nama", ApiHelperSupport.safeString(o.getNama()));
                    jo.put("kode", ApiHelperSupport.safeString(o.getKode()));
                    arr.put(jo);
                } catch (Exception e) {
                }
            }
            hasil.put("list", arr);
            ApiHelperSupport.putSuccess(hasil, "OK");
            return hasil;
        } catch (Exception e) {
            return ApiHelperSupport.errorResponse("Gagal mencari data");
        } finally {
            closeQuietly(session);
        }
    }

    /**
     * Mengajukan SOP baru — port {@code DisposisiSopAction.onSave} (§5): simpan entitas FormSop
     * (bila ada, via field data dinamis §3), buat {@code DisposisiSop} + tahap start
     * {@code DisposisiAlurSop}, tautkan {@code disposisiStart}/{@code disposisiEnd}/
     * {@code disposisiSetuju}, buat placeholder tahap berikutnya sesuai rute dipilih, pindahkan
     * lampiran dari {@code refSementara} ke id tahap start yang sebenarnya, lalu picu notifikasi —
     * seluruhnya dalam SATU transaksi atomik (REST, bukan rangkaian timer ZK seperti web).
     */
    public static JSONObject ajukan(HttpServletRequest req, JSONObject json) {
        JSONObject hasil = new JSONObject();
        Session session = null;
        try {
            Tbmuser tbmuser = ApiUtil.currentUser(json, req);
            if (tbmuser == null || tbmuser.getUserId() == null) {
                return ApiHelperSupport.status("97", "Token tidak sesuai");
            }
            if (ApiHelperSupport.isNullOrEmptyJsonValue(json, "sopId")) {
                return ApiHelperSupport.status("97", "SOP harus dipilih");
            }
            Long sopId = Long.parseLong(ApiHelperSupport.optString(json, "sopId"));
            Long refSementara = ApiHelperSupport.isNullOrEmptyJsonValue(json, "refSementara") ? null
                    : Long.parseLong(ApiHelperSupport.optString(json, "refSementara"));

            session = HibernateUtil.getSessionFactory().openSession();
            List<Sop> boleh = daftarSopBisaDiajukan(session, tbmuser);
            Sop sop = null;
            for (Sop s : boleh) {
                if (s.getId() != null && s.getId().equals(sopId)) {
                    sop = s;
                    break;
                }
            }
            if (sop == null) {
                return ApiHelperSupport.status("97", "Anda tidak berhak mengajukan SOP ini");
            }
            AlurSop alurSop = resolveStartAlurSop(session, sop);
            if (alurSop == null) {
                return ApiHelperSupport.status("99", "Alur Awal (start) untuk SOP ini tidak ditemukan");
            }

            String keterangan = ApiHelperSupport.optString(json, "keterangan").trim();
            JSONArray alurSopIdsInput = json.has("alurSopIds") && !json.isNull("alurSopIds") ? json.optJSONArray("alurSopIds") : null;
            JSONArray parameterTambahanInput = json.has("parameterTambahan") && !json.isNull("parameterTambahan")
                    ? json.optJSONArray("parameterTambahan") : null;
            JSONObject formData = json.has("formData") && !json.isNull("formData") ? json.optJSONObject("formData") : null;

            if (Boolean.TRUE.equals(alurSop.getCatatanWajibDiisi()) && !ApiHelperSupport.hasText(keterangan)) {
                return ApiHelperSupport.status("97", "Catatan/keterangan harus diisi");
            }
            if (!Boolean.TRUE.equals(alurSop.getAlurSetelahnyaTidakWajib())
                    && (alurSopIdsInput == null || alurSopIdsInput.length() == 0) && !alurSop.ambilAlurSetelahnya().isEmpty()) {
                return ApiHelperSupport.status("97", "Pilihan tindak lanjut/langkah berikutnya harus dipilih");
            }
            String errParam = validateParameterTambahanWajib(session, alurSop, parameterTambahanInput, refSementara);
            if (errParam != null) {
                return ApiHelperSupport.status("97", errParam);
            }
            String errDok = validateDokumenWajib(alurSop, refSementara);
            if (errDok != null) {
                return ApiHelperSupport.status("97", errDok);
            }
            if (Common.bolehKonfigurasi("tampilkan_lampiran_catatan_disposisi") && Boolean.TRUE.equals(alurSop.getLampiranCatatanWajibDiisi())) {
                LampiranLain lam = refSementara == null ? null : LampiranLain.ambil(refSementara, LAMPIRAN_CATATAN_DISPOSISI_KEY);
                if (lam == null) {
                    return ApiHelperSupport.status("97", "Lampiran catatan disposisi harus diunggah terlebih dahulu");
                }
            }

            List<AlurSop> nextNodes = new ArrayList<AlurSop>();
            if (alurSopIdsInput != null) {
                for (int i = 0; i < alurSopIdsInput.length(); i++) {
                    try {
                        Long nextId = alurSopIdsInput.getLong(i);
                        AlurSop next = (AlurSop) session.get(AlurSop.class, nextId);
                        if (next != null) {
                            nextNodes.add(next);
                        }
                    } catch (Exception e) {
                    }
                }
            }

            session.getTransaction().begin();
            try {
                GeneralValueObject formEntity = null;
                Class<?> formEntityClass = null;
                String formClassName = alurSop.getFormInputan();
                if (ApiHelperSupport.hasText(formClassName)) {
                    Object obj = Class.forName(formClassName.trim()).newInstance();
                    if (obj instanceof FormSop) {
                        FormSop formSop = (FormSop) obj;
                        formEntityClass = formSop.ambilClass();
                        if (formEntityClass != null) {
                            formEntity = (GeneralValueObject) formEntityClass.newInstance();
                            applyFormDataDinamis(session, formEntityClass, formEntity, formData);
                            session.save(formEntity);
                        }
                    }
                }

                DisposisiSop disposisiSop = new DisposisiSop();
                if (tbmuser.getMahasiswa() != null) {
                    disposisiSop.setMahasiswa(tbmuser.getMahasiswa());
                } else if (tbmuser.getSiswa() != null) {
                    disposisiSop.setSiswa(tbmuser.getSiswa());
                } else {
                    disposisiSop.setDiajukanOleh(tbmuser);
                }
                disposisiSop.setSop(sop);
                disposisiSop.setWaktu(new Date());
                disposisiSop.setKeterangan(keterangan);
                session.save(disposisiSop);

                DisposisiAlurSop alurSopAwal = new DisposisiAlurSop();
                alurSopAwal.setAlurSop(alurSop);
                alurSopAwal.setDisposisiSop(disposisiSop);
                alurSopAwal.setKeterangan(keterangan);
                if (parameterTambahanInput != null) {
                    alurSopAwal.setParameterTambahanInds(encodeParameterTambahanInds(parameterTambahanInput, refSementara));
                }
                session.save(alurSopAwal);

                disposisiSop.setDisposisiStart(alurSopAwal);
                if (disposisiSop.getDisposisiEnd() == null) {
                    disposisiSop.setDisposisiEnd(alurSopAwal);
                }
                boolean isSetuju = Boolean.TRUE.equals(alurSop.getJikaProsesDisetujuiMakaSelesai()) || alurSopAwal.setujui();
                if (isSetuju && disposisiSop.getDisposisiSetuju() == null) {
                    disposisiSop.setDisposisiSetuju(alurSopAwal);
                }

                if (formEntity != null && formEntityClass != null) {
                    // FIX (compile error "cannot find symbol setDisposisiSop"): formEntity dideklarasikan
                    // generik sbg GeneralValueObject karena kelas form-nya ditentukan DINAMIS per jenis SOP
                    // (lewat FormSop.ambilClass(), lihat atas) -- panggilan langsung
                    // formEntity.setDisposisiSop(...) tak bisa dikompilasi krn method itu cuma ada di
                    // SEBAGIAN subclass, bukan di GeneralValueObject. Set properti "disposisiSop" secara
                    // reflektif lewat Hibernate ClassMetadata, pola yang sama persis dipakai
                    // applyFormDataDinamis(...) di atas, dan hanya bila kelas form ybs memang punya
                    // properti bernama "disposisiSop" (form yang tak punya properti ini dilewati, tak error).
                    try {
                        org.hibernate.metadata.ClassMetadata metaFormEntity = HibernateUtil.getClassMetadata(formEntityClass);
                        if (metaFormEntity != null
                                && java.util.Arrays.asList(metaFormEntity.getPropertyNames()).contains("disposisiSop")) {
                            metaFormEntity.setPropertyValue(formEntity, "disposisiSop", disposisiSop,
                                    org.hibernate.EntityMode.POJO);
                        }
                    } catch (Exception e) {
                        ais.common.ErrorAuditUtil.record(e,
                                "auto-audit(fix-compile) src/ais/action/servlet/api/SopService.java:formEntity-setDisposisiSop");
                    }
                    session.update(formEntity);
                    try {
                        JSONObject root = ApiHelperSupport.hasText(disposisiSop.getProperti())
                                ? new JSONObject(disposisiSop.getProperti()) : new JSONObject();
                        JSONObject entri = new JSONObject();
                        entri.put("id", formEntity.getId());
                        entri.put("kode", ApiHelperSupport.safeString(formEntity.getKode()));
                        entri.put("nama", ApiHelperSupport.safeString(formEntity.getNama()));
                        root.put(formEntityClass.getName(), entri);
                        disposisiSop.setProperti(root.toString());
                    } catch (Exception e) {
                    }
                }
                session.update(disposisiSop);

                DisposisiAlurSop langkahSetelah = null;
                for (AlurSop nextNode : nextNodes) {
                    DisposisiAlurSop dibuat = cariAtauBuatDisposisiAlurSop(session, disposisiSop, nextNode, alurSopAwal);
                    if (dibuat != null) {
                        langkahSetelah = dibuat;
                    }
                }
                if (langkahSetelah != null) {
                    alurSopAwal.setSetelahnya(langkahSetelah);
                    session.update(alurSopAwal);
                }

                if (refSementara != null) {
                    pindahkanLampiran(session, refSementara, alurSopAwal.getId());
                }

                session.getTransaction().commit();

                try {
                    TampilanAlurSopAction.cetakDisposisi(disposisiSop, true);
                } catch (Exception eNotif) {
                }

                hasil.put("disposisiSopId", disposisiSop.getId());
                hasil.put("disposisiAlurSopId", alurSopAwal.getId());
                ApiHelperSupport.putSuccess(hasil, "Pengajuan SOP berhasil disimpan");
                return hasil;
            } catch (Exception eTx) {
                ApiHelperSupport.rollbackQuietly(session.getTransaction());
                return ApiHelperSupport.errorResponse("Gagal menyimpan pengajuan SOP: data yang dimasukkan tidak lengkap atau tidak valid");
            }
        } catch (Exception e) {
            return ApiHelperSupport.errorResponse("Gagal mengajukan SOP");
        } finally {
            closeQuietly(session);
        }
    }

    private static void pindahkanLampiran(Session session, Long refLama, Long refBaru) {
        if (refLama == null || refBaru == null) {
            return;
        }
        try {
            @SuppressWarnings("unchecked")
            List<LampiranLain> list = session.createCriteria(LampiranLain.class).add(Restrictions.eq("ref", refLama)).list();
            for (LampiranLain lam : list) {
                try {
                    lam.setRef(refBaru);
                    session.update(lam);
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
        }
    }

    // ── Field data dinamis (INPUT) untuk entitas FormSop — pasangan tulis dari buildDynamicDataFields ──

    /** Kategori tipe UI untuk satu properti Hibernate: relasi | koleksi (diabaikan) | tanggal | boolean | angka | teks. */
    private static String kategoriTipeProperti(org.hibernate.type.Type type) {
        try {
            if (type.isEntityType()) {
                return "relasi";
            }
            if (type.isCollectionType()) {
                return "koleksi";
            }
            Class<?> rc = type.getReturnedClass();
            if (rc == null) {
                return "teks";
            }
            if (Date.class.isAssignableFrom(rc)) {
                return "tanggal";
            }
            if (Boolean.class.isAssignableFrom(rc) || rc == boolean.class) {
                return "boolean";
            }
            if (Number.class.isAssignableFrom(rc) || rc == int.class || rc == long.class || rc == double.class || rc == float.class) {
                return "angka";
            }
            return "teks";
        } catch (Exception e) {
            return "teks";
        }
    }

    /** Definisi field INPUT dinamis untuk entitas FormSop manapun (pasangan tulis dari {@link #buildDynamicDataFields}). */
    private static JSONArray buildFormInputDefinisi(Class<?> entityClass) {
        JSONArray arr = new JSONArray();
        try {
            if (entityClass == null) {
                return arr;
            }
            org.hibernate.metadata.ClassMetadata meta = HibernateUtil.getClassMetadata(entityClass);
            if (meta == null) {
                return arr;
            }
            String[] properties = meta.getPropertyNames();
            org.hibernate.type.Type[] types = meta.getPropertyTypes();
            boolean[] nullability = meta.getPropertyNullability();
            if (properties == null || types == null) {
                return arr;
            }
            for (int i = 0; i < properties.length && i < types.length; i++) {
                String property = properties[i];
                if (!ApiHelperSupport.hasText(property) || FIELD_TEKNIS_DATASOP.contains(property)) {
                    continue;
                }
                String kategori = kategoriTipeProperti(types[i]);
                if ("koleksi".equals(kategori)) {
                    continue;
                }
                JSONObject o = new JSONObject();
                o.put("property", property);
                o.put("label", humanizeLabelProperti(property));
                o.put("tipe", kategori);
                o.put("wajib", nullability != null && i < nullability.length && !nullability[i]);
                if ("relasi".equals(kategori)) {
                    try {
                        Class<?> relClass = types[i].getReturnedClass();
                        o.put("relasiClass", relClass == null ? "" : relClass.getName());
                    } catch (Exception e) {
                    }
                }
                arr.put(o);
            }
        } catch (Exception e) {
        }
        return arr;
    }

    /**
     * Menerapkan {@code formData} (kiriman mobile, sesuai definisi dari {@link #buildFormInputDefinisi})
     * ke entitas FormSop yang baru dibuat, memakai metadata Hibernate yang SAMA (bukan konfigurasi
     * manual per-class) sehingga otomatis berlaku untuk SEMUA class *Action yang mengimplementasikan
     * FormSop. Field relasi diterima berupa {@code {"id": ...}} (atau id polos) dan di-resolve via
     * {@code session.get(...)}; field kosong/tidak dikenali dilewati dengan aman.
     */
    private static void applyFormDataDinamis(Session session, Class<?> entityClass, GeneralValueObject entity, JSONObject formData) {
        try {
            if (entityClass == null || entity == null || formData == null) {
                return;
            }
            org.hibernate.metadata.ClassMetadata meta = HibernateUtil.getClassMetadata(entityClass);
            if (meta == null) {
                return;
            }
            String[] properties = meta.getPropertyNames();
            org.hibernate.type.Type[] types = meta.getPropertyTypes();
            if (properties == null || types == null) {
                return;
            }
            for (int i = 0; i < properties.length && i < types.length; i++) {
                String property = properties[i];
                if (!ApiHelperSupport.hasText(property) || FIELD_TEKNIS_DATASOP.contains(property)) {
                    continue;
                }
                if (!formData.has(property) || formData.isNull(property)) {
                    continue;
                }
                String kategori = kategoriTipeProperti(types[i]);
                try {
                    if ("relasi".equals(kategori)) {
                        Object item = formData.get(property);
                        Long relId = null;
                        if (item instanceof JSONObject && ((JSONObject) item).has("id") && !((JSONObject) item).isNull("id")) {
                            relId = Long.parseLong(String.valueOf(((JSONObject) item).get("id")));
                        } else if (!(item instanceof JSONObject)) {
                            relId = Long.parseLong(String.valueOf(item));
                        }
                        if (relId != null) {
                            Class<?> relClass = types[i].getReturnedClass();
                            Object relEntity = relClass == null ? null : session.get(relClass, relId);
                            if (relEntity != null) {
                                meta.setPropertyValue(entity, property, relEntity, org.hibernate.EntityMode.POJO);
                            }
                        }
                    } else if (!"koleksi".equals(kategori)) {
                        Object nilai = formData.get(property);
                        Object converted = konversiNilaiSkalar(types[i].getReturnedClass(), kategori, String.valueOf(nilai));
                        if (converted != null) {
                            meta.setPropertyValue(entity, property, converted, org.hibernate.EntityMode.POJO);
                        }
                    }
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
        }
    }

    private static Object konversiNilaiSkalar(Class<?> targetClass, String kategori, String nilai) {
        try {
            if (!ApiHelperSupport.hasText(nilai)) {
                return null;
            }
            if ("tanggal".equals(kategori)) {
                return Common.dateFormat1.get().parse(nilai);
            }
            if ("boolean".equals(kategori)) {
                String v = nilai.trim().toLowerCase();
                return Boolean.valueOf("ya".equals(v) || "true".equals(v) || "1".equals(v));
            }
            if (targetClass == null) {
                return nilai;
            }
            String bersih = nilai.trim();
            if (targetClass == Integer.class || targetClass == int.class) {
                return Integer.valueOf((int) Double.parseDouble(bersih));
            }
            if (targetClass == Long.class || targetClass == long.class) {
                return Long.valueOf((long) Double.parseDouble(bersih));
            }
            if (targetClass == Double.class || targetClass == double.class) {
                return Double.valueOf(bersih.replace(",", "."));
            }
            if (targetClass == Float.class || targetClass == float.class) {
                return Float.valueOf(bersih.replace(",", "."));
            }
            if (targetClass == java.math.BigDecimal.class) {
                return new java.math.BigDecimal(bersih.replace(",", "."));
            }
            return nilai;
        } catch (Exception e) {
            return null;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Criteria builder — port DasboardSop.java (count/panel) & sesuai kategori dasbor
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Kriteria "proses yang dipantau pengguna ini" -- pengaju, petugas, atau subjek
     * pengajuan. Dipisah menjadi metode sendiri supaya bentuknya PERSIS SAMA antara
     * sampel analitik dan hitungan tepat {@link #hitungDipantauTepat}; kalau keduanya
     * disusun terpisah, keduanya bisa menyimpang tanpa ketahuan.
     */
    private static Criteria criteriaDipantau(Session session, Tbmuser tbmuser, Date mulai, Date sampai,
            String keyword) {
        Criteria c = baseDisposisiAlurCriteria(session);
        Criterion pengaju = AktorSop.buatCriterionPengaju(tbmuser, "disposisiSop");
        Criterion petugas = aktorRestriction(tbmuser, c);
        Criterion rootUser = AktorSop.buatCriterionPengaju(tbmuser, "");
        c.add(Restrictions.or(pengaju, Restrictions.or(petugas, rootUser)));
        applyGlobalDisposisiFilter(c, mulai, sampai, keyword);
        return c;
    }

    /**
     * Jumlah pengajuan (bukan langkah) yang benar-benar dipantau pengguna ini.
     * Mengembalikan 0 bila gagal, supaya pemanggil dapat jatuh ke angka sampel.
     */
    private static int hitungDipantauTepat(Session session, Tbmuser tbmuser, Date mulai, Date sampai,
            String keyword, Long satuanKerjaId) {
        try {
            Object n = satkerAlur(criteriaDipantau(session, tbmuser, mulai, sampai, keyword), satuanKerjaId)
                    .setProjection(Projections.countDistinct("disposisiSop.id")).uniqueResult();
            return n == null ? 0 : ((Number) n).intValue();
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "SopService.hitungDipantauTepat");
            return 0;
        }
    }

    private static Criteria baseDisposisiAlurCriteria(Session session) {
        return session.createCriteria(DisposisiAlurSop.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .add(Restrictions.isNotNull("alurSop")).createAlias("alurSop", "alurSop")
                .add(Restrictions.isNotNull("alurSop.sop"))
                .createAlias("alurSop.aktorSop", "aktorSop", Criteria.LEFT_JOIN)
                .createAlias("disposisiSop", "disposisiSop")
                .createAlias("sebelumnya", "sebelumnya", Criteria.LEFT_JOIN)
                .add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"), Restrictions.eq("disposisiSop.aktif", true)));
    }

    private static Criterion aktorRestriction(Tbmuser tbmuser, Criteria criteria) {
        return AktorSop.buatCriterion(tbmuser, true, criteria);
    }

    /** "Menunggu Saya" (+ variasi "Lewat Batas Waktu" bila hanyaLewatDeadline). Port createMenungguSayaSesuaiPanelCriteria. */
    private static Criteria criteriaMenungguSaya(Session session, Tbmuser tbmuser, Date mulai, Date sampai,
            String keyword, boolean hanyaLewatDeadline) {
        Criteria c = baseDisposisiAlurCriteria(session);
        // Sejajar DasboardSop.createMenungguSayaSesuaiPanelCriteria dan versi JSP:
        // langkah yang sudah ditandai selesai tidak lagi menunggu siapa pun.
        c.add(Restrictions.or(Restrictions.isNull("selesai"), Restrictions.eq("selesai", false)));
        c.add(aktorRestriction(tbmuser, c));
        c.add(Restrictions.isNull("diajukanOleh"));
        c.add(Restrictions.isNull("siswa"));
        c.add(Restrictions.isNull("mahasiswa"));
        c.add(Restrictions.isNull("setelahnya"));
        c.add(Restrictions.isNotNull("sebelumnya"));
        if (hanyaLewatDeadline) {
            c.add(Restrictions.isNotNull("waktuMaksimal"));
            c.add(Restrictions.lt("waktuMaksimal", new Date()));
        }
        applyGlobalDisposisiFilter(c, mulai, sampai, keyword);
        return c;
    }

    /** "Sudah Disposisi" — langkah non-start yang sudah diproses oleh user ini sebagai aktor-of-record. Port getCountDisposisi. */
    private static Criteria criteriaSudahDisposisi(Session session, Tbmuser tbmuser, Date mulai, Date sampai, String keyword) {
        Criteria c = session.createCriteria(DisposisiAlurSop.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .add(Restrictions.isNotNull("alurSop")).createAlias("alurSop", "alurSop")
                .add(Restrictions.isNotNull("alurSop.sop")).add(Restrictions.eq("alurSop.start", false))
                .add(Restrictions.isNotNull("waktu")).createAlias("disposisiSop", "disposisiSop")
                .add(AktorSop.buatCriterionPengaju(tbmuser, ""))
                // Sejajar DasboardSop.getCountDisposisi dan PengajuanAndaSopUtil (JSP): langkah
                // yang INDUK pengajuannya sudah dinonaktifkan/dibatalkan tidak boleh ikut
                // terhitung. Tanpa baris ini angka "Sudah Disposisi" di POS/mobile lebih
                // besar daripada versi ZKoss dan JSP untuk data yang sama.
                .add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"),
                        Restrictions.eq("disposisiSop.aktif", true)));
        applyGlobalDisposisiFilter(c, mulai, sampai, keyword);
        return c;
    }

    /** "Selesai" — pengajuan submitted oleh user ini yang tahap terkininya sudah tuntas. Port createSelesaiSesuaiPanelCriteria. */
    private static Criteria criteriaSelesai(Session session, Tbmuser tbmuser, Date mulai, Date sampai, String keyword) {
        Criteria c = session.createCriteria(DisposisiAlurSop.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .add(Restrictions.isNotNull("alurSop")).createAlias("alurSop", "alurSop")
                .add(Restrictions.isNotNull("alurSop.sop")).add(Restrictions.eq("alurSop.start", false));
        Criterion semuaSetelahnyaNull = Restrictions.and(Restrictions.isNull("alurSop.setelahnya"),
                Restrictions.and(Restrictions.isNull("alurSop.setelahnya2"),
                        Restrictions.and(Restrictions.isNull("alurSop.setelahnya3"),
                                Restrictions.and(Restrictions.isNull("alurSop.setelahnya4"),
                                        Restrictions.isNull("alurSop.setelahnya5")))));
        c.add(Restrictions.or(Restrictions.eq("selesai", true), semuaSetelahnyaNull));
        c.add(Restrictions.isNotNull("waktu"));
        c.createAlias("disposisiSop", "disposisiSop");
        c.add(AktorSop.buatCriterionPengaju(tbmuser, "disposisiSop"));
        c.add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"), Restrictions.eq("disposisiSop.aktif", true)));
        applyGlobalDisposisiFilter(c, mulai, sampai, keyword);
        return c;
    }

    /** "Menunggu Petugas" — pengajuan yang disubmit user ini, tahap pending-nya ada di aktor lain. Port createMenungguAktorSesuaiPanelCriteria. */
    private static Criteria criteriaMenungguAktor(Session session, Tbmuser tbmuser, Date mulai, Date sampai, String keyword) {
        Criteria c = session.createCriteria(DisposisiAlurSop.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .add(Restrictions.isNotNull("alurSop")).createAlias("alurSop", "alurSop")
                .add(Restrictions.isNotNull("alurSop.sop")).add(Restrictions.isNull("diajukanOleh"))
                .add(Restrictions.isNull("mahasiswa")).add(Restrictions.isNull("siswa"))
                .createAlias("disposisiSop", "disposisiSop").add(AktorSop.buatCriterionPengaju(tbmuser, "disposisiSop"))
                .add(Restrictions.or(Restrictions.isNull("disposisiSop.aktif"), Restrictions.eq("disposisiSop.aktif", true)));
        applyGlobalDisposisiFilter(c, mulai, sampai, keyword);
        return c;
    }

    /** "Jumlah Data Pengajuan Anda" — root DisposisiSop. Port getCountDataPengajuanAnda. */
    private static Criteria criteriaPengajuanAnda(Session session, Tbmuser tbmuser, Date mulai, Date sampai, String keyword) {
        Criteria c = session.createCriteria(DisposisiSop.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                .add(AktorSop.buatCriterionPengaju(tbmuser, ""));
        applyGlobalDisposisiSopFilter(c, mulai, sampai, keyword);
        return c;
    }

    private static int countCriteria(Criteria criteria) {
        try {
            Number n = (Number) criteria.setProjection(Projections.rowCount()).uniqueResult();
            return n == null ? 0 : n.intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    /** Filter global untuk criteria ber-root DisposisiSop (mis. "Jumlah Data Pengajuan Anda"). */
    private static void applyGlobalDisposisiSopFilter(Criteria c, Date mulai, Date sampai, String keyword) {
        if (mulai != null) {
            c.add(Restrictions.ge("waktu", awalHari(mulai)));
        }
        if (sampai != null) {
            c.add(Restrictions.lt("waktu", awalHariBerikutnya(sampai)));
        }
        if (ApiHelperSupport.hasText(keyword)) {
            c.createAlias("sop", "sopKeyword", Criteria.LEFT_JOIN);
            c.add(Restrictions.or(Restrictions.ilike("properti", keyword, MatchMode.ANYWHERE),
                    Restrictions.or(Restrictions.ilike("keterangan", keyword, MatchMode.ANYWHERE),
                            Restrictions.ilike("sopKeyword.nama", keyword, MatchMode.ANYWHERE))));
        }
    }

    /**
     * Filter global untuk criteria ber-root DisposisiAlurSop, dengan alias "disposisiSop" WAJIB
     * sudah dibuat pada criteria sebelum dipanggil (semua criteria builder di atas melakukannya).
     * Tanggal difilter dari {@code disposisiSop.waktu} (tanggal SUBMIT keseluruhan pengajuan),
     * BUKAN {@code this.waktu} (tanggal langkah, null untuk tahap pending) — persis seperti bug
     * yang sudah diperbaiki di versi web (lihat komentar di DasboardSop.java).
     */
    /**
     * Menyaring menurut Satuan Kerja pada criteria ber-root {@code DisposisiAlurSop}
     * -- port {@code DasboardSop.applyGlobalSatkerFilter}: satker diambil dari pegawai
     * milik PENGAJU pengajuan, atau pegawai milik pengambil langkah itu sendiri.
     *
     * <p>Mengembalikan criteria yang sama supaya bisa dibungkus langsung di titik
     * panggil, tanpa mengubah tanda tangan rantai criteria yang sudah ada.</p>
     */
    private static Criteria satkerAlur(Criteria c, Long satuanKerjaId) {
        if (c == null || satuanKerjaId == null) {
            return c;
        }
        try {
            c.createAlias("disposisiSop.diajukanOleh", "satkerPengajuUser", Criteria.LEFT_JOIN);
            c.createAlias("satkerPengajuUser.pegawai", "satkerPengajuPegawai", Criteria.LEFT_JOIN);
            c.createAlias("diajukanOleh", "satkerRootUser", Criteria.LEFT_JOIN);
            c.createAlias("satkerRootUser.pegawai", "satkerRootPegawai", Criteria.LEFT_JOIN);
            c.add(Restrictions.or(
                    Restrictions.eq("satkerPengajuPegawai.satuanKerja.id", satuanKerjaId),
                    Restrictions.eq("satkerRootPegawai.satuanKerja.id", satuanKerjaId)));
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "SopService.satkerAlur");
        }
        return c;
    }

    /** Padanan {@link #satkerAlur} untuk criteria ber-root {@code DisposisiSop}. */
    private static Criteria satkerSop(Criteria c, Long satuanKerjaId) {
        if (c == null || satuanKerjaId == null) {
            return c;
        }
        try {
            c.createAlias("diajukanOleh", "satkerSopUser", Criteria.LEFT_JOIN);
            c.createAlias("satkerSopUser.pegawai", "satkerSopPegawai", Criteria.LEFT_JOIN);
            c.add(Restrictions.eq("satkerSopPegawai.satuanKerja.id", satuanKerjaId));
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "SopService.satkerSop");
        }
        return c;
    }

    private static void applyGlobalDisposisiFilter(Criteria c, Date mulai, Date sampai, String keyword) {
        if (mulai != null) {
            c.add(Restrictions.ge("disposisiSop.waktu", awalHari(mulai)));
        }
        if (sampai != null) {
            c.add(Restrictions.lt("disposisiSop.waktu", awalHariBerikutnya(sampai)));
        }
        if (ApiHelperSupport.hasText(keyword)) {
            c.add(Restrictions.or(Restrictions.ilike("disposisiSop.properti", keyword, MatchMode.ANYWHERE),
                    Restrictions.or(Restrictions.ilike("properti", keyword, MatchMode.ANYWHERE),
                            Restrictions.ilike("keyword", keyword, MatchMode.ANYWHERE))));
        }
    }

    private static Date awalHari(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private static Date awalHariBerikutnya(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(awalHari(d));
        cal.add(Calendar.DATE, 1);
        return cal.getTime();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Analitik sampel dasbor — port analyzeDashboardRows dari DasboardSop.java
    // ════════════════════════════════════════════════════════════════════════

    private static JSONObject analyzeDashboardRows(List<DisposisiAlurSop> rows) {
        JSONObject hasil = new JSONObject();
        Map<String, Integer> perSop = new LinkedHashMap<String, Integer>();
        Map<String, Integer> perAktor = new LinkedHashMap<String, Integer>();
        // Tren bulanan -- padanan d.perBulan pada DasboardSop (renderSopTrendBulananV13).
        Map<String, Integer> perBulan = new LinkedHashMap<String, Integer>();
        Set<Long> uniqueDisposisi = new HashSet<Long>();
        int deadlineLewat = 0, deadlineHariIni = 0, deadlineMingguIni = 0, deadlineAman = 0;
        int tanpaDeadline = 0, tanpaAktor = 0, tanpaTahap = 0, tanpaCatatan = 0;
        JSONArray aktivitasTerbaru = new JSONArray();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date hariIni = cal.getTime();
        cal.add(Calendar.DATE, 1);
        Date besok = cal.getTime();
        cal.add(Calendar.DATE, 6);
        Date tujuhHari = cal.getTime();

        try {
            for (DisposisiAlurSop row : rows) {
                if (row == null || row.getDisposisiSop() == null) {
                    continue;
                }
                Long disposisiId = row.getDisposisiSop().getId();
                if (disposisiId != null) {
                    uniqueDisposisi.add(disposisiId);
                }
                addCounter(perSop, getNamaSop(row));
                addCounter(perAktor, getNamaAktor(row));
                addCounter(perBulan, kunciBulanTahun(row));

                try {
                    if (row.getWaktuMaksimal() == null) {
                        tanpaDeadline++;
                    }
                } catch (Exception e) {
                    tanpaDeadline++;
                }
                try {
                    if (row.getAlurSop() == null || !ApiHelperSupport.hasText(row.getAlurSop().getAktor())) {
                        tanpaAktor++;
                    }
                } catch (Exception e) {
                    tanpaAktor++;
                }
                try {
                    if (row.getAlurSop() == null || !ApiHelperSupport.hasText(row.getAlurSop().getNama())) {
                        tanpaTahap++;
                    }
                } catch (Exception e) {
                    tanpaTahap++;
                }
                try {
                    if (!ApiHelperSupport.hasText(row.getKeterangan())) {
                        tanpaCatatan++;
                    }
                } catch (Exception e) {
                    tanpaCatatan++;
                }

                if (aktivitasTerbaru.length() < 8) {
                    aktivitasTerbaru.put(buildRecentItem(row));
                }

                Date deadline = null;
                try {
                    deadline = row.getWaktuMaksimal();
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1164");
                }
                if (deadline != null && !isSelesai(row)) {
                    if (deadline.before(hariIni)) {
                        deadlineLewat++;
                    } else if (deadline.before(besok)) {
                        deadlineHariIni++;
                    } else if (deadline.before(tujuhHari)) {
                        deadlineMingguIni++;
                    } else {
                        deadlineAman++;
                    }
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1178");
        }

        try {
            hasil.put("totalDipantau", uniqueDisposisi.size());
            JSONObject deadline = new JSONObject();
            deadline.put("lewat", deadlineLewat);
            deadline.put("hariIni", deadlineHariIni);
            deadline.put("mingguIni", deadlineMingguIni);
            deadline.put("aman", deadlineAman);
            hasil.put("deadline", deadline);
            JSONObject metadataQuality = new JSONObject();
            metadataQuality.put("tanpaDeadline", tanpaDeadline);
            metadataQuality.put("tanpaAktor", tanpaAktor);
            metadataQuality.put("tanpaTahap", tanpaTahap);
            metadataQuality.put("tanpaCatatan", tanpaCatatan);
            hasil.put("metadataQuality", metadataQuality);
            hasil.put("perSop", topCounter(perSop, 8));
            hasil.put("perAktor", topCounter(perAktor, 8));
            hasil.put("perBulan", deretWaktu(perBulan, 12));
            hasil.put("aktivitasTerbaru", aktivitasTerbaru);
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1198");
        }
        return hasil;
    }

    private static void addCounter(Map<String, Integer> map, String key) {
        String k = ApiHelperSupport.hasText(key) ? key : "Tidak diketahui";
        Integer cur = map.get(k);
        map.put(k, cur == null ? 1 : cur + 1);
    }

    /**
     * Kunci bulan {@code yyyy-MM} untuk satu langkah alur -- port
     * {@code DasboardSop.getBulanTahunKey}: memakai waktu langkah, dan bila kosong
     * jatuh ke waktu pengajuan induknya.
     */
    private static String kunciBulanTahun(DisposisiAlurSop row) {
        try {
            Date waktu = null;
            try {
                waktu = row.getWaktu();
            } catch (Exception e) {
                waktu = null;
            }
            if (waktu == null && row.getDisposisiSop() != null) {
                waktu = row.getDisposisiSop().getWaktu();
            }
            if (waktu != null) {
                String f = Common.databaseDateFormat.get().format(waktu);
                if (f != null && f.length() >= 7) {
                    return f.substring(0, 7);
                }
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "SopService.kunciBulanTahun");
        }
        return "?";
    }

    /**
     * Serialisasi deret WAKTU: urut kronologis menaik lalu diambil {@code limit}
     * bulan TERAKHIR. Sengaja tidak memakai {@link #topCounter}, yang mengurutkan
     * menurut jumlah -- benar untuk peringkat, salah untuk tren.
     */
    private static JSONArray deretWaktu(Map<String, Integer> map, int limit) {
        JSONArray arr = new JSONArray();
        try {
            List<String> kunci = new ArrayList<String>(map.keySet());
            kunci.remove("?");
            Collections.sort(kunci);
            int mulai = kunci.size() > limit ? kunci.size() - limit : 0;
            for (int i = mulai; i < kunci.size(); i++) {
                JSONObject o = new JSONObject();
                o.put("label", kunci.get(i));
                o.put("jumlah", map.get(kunci.get(i)));
                arr.put(o);
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "SopService.deretWaktu");
        }
        return arr;
    }

    private static JSONArray topCounter(Map<String, Integer> map, int limit) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String, Integer>>(map.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return b.getValue() - a.getValue();
            }
        });
        JSONArray arr = new JSONArray();
        for (int i = 0; i < entries.size() && i < limit; i++) {
            try {
                JSONObject o = new JSONObject();
                o.put("label", entries.get(i).getKey());
                o.put("jumlah", entries.get(i).getValue());
                arr.put(o);
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1223");
            }
        }
        return arr;
    }

    private static JSONObject buildRecentItem(DisposisiAlurSop row) {
        JSONObject o = new JSONObject();
        try {
            o.put("sop", getNamaSop(row));
            o.put("aktor", getNamaAktor(row));
            o.put("waktu", row.getWaktu() == null ? "" : Common.dateFormat51.get().format(row.getWaktu()));
            o.put("status", getStatusRingkas(row));
            Date deadline = row.getWaktuMaksimal();
            o.put("deadline", deadline == null ? "" : Common.dateFormat51.get().format(deadline));
            o.put("disposisiSopId", row.getDisposisiSop() == null ? null : row.getDisposisiSop().getId());
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1239");
        }
        return o;
    }

    // ════════════════════════════════════════════════════════════════════════
    // Pemetaan baris → JSON (port row renderer / status label DasboardSop & TampilanAlurSopAction)
    // ════════════════════════════════════════════════════════════════════════

    private static JSONObject mapDisposisiAlurSopRow(DisposisiAlurSop row) {
        JSONObject o = new JSONObject();
        try {
            o.put("id", row.getId());
            o.put("disposisiSopId", row.getDisposisiSop() == null ? null : row.getDisposisiSop().getId());
            o.put("kode", kodePengajuan(row.getDisposisiSop(), row));
            o.put("sop", getNamaSop(row));
            o.put("pengaju", namaPengaju(row.getDisposisiSop()));
            o.put("aktorTahap", getNamaAktor(row));
            o.put("status", getStatusRingkas(row));
            o.put("waktu", row.getWaktu() == null ? "" : Common.dateFormat51.get().format(row.getWaktu()));
            Date deadline = null;
            try {
                deadline = row.getWaktuMaksimal();
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1262");
            }
            o.put("waktuMaksimal", deadline == null ? "" : Common.dateFormat51.get().format(deadline));
            o.put("lewatBatasWaktu", deadline != null && deadline.before(new Date()) && !isSelesai(row));
            o.put("catatan", ApiHelperSupport.safeString(row.getKeterangan()));
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1267");
        }
        return o;
    }

    private static JSONObject mapDisposisiSopRow(Session session, DisposisiSop d) {
        JSONObject o = new JSONObject();
        try {
            o.put("id", d.getId());
            o.put("disposisiSopId", d.getId());
            o.put("kode", kodePengajuan(d, null));
            o.put("sop", d.getSop() == null ? "SOP Tidak Diketahui" : ApiHelperSupport.safeString(d.getSop().getNama()));
            o.put("pengaju", namaPengaju(d));
            o.put("waktu", d.getWaktu() == null ? "" : Common.dateFormat51.get().format(d.getWaktu()));
            DisposisiAlurSop terakhir = (DisposisiAlurSop) session.createCriteria(DisposisiAlurSop.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .add(Restrictions.eq("disposisiSop", d)).addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
            if (terakhir != null) {
                o.put("status", getStatusRingkas(terakhir));
                o.put("aktorTahap", getNamaAktor(terakhir));
                Date deadline = null;
                try {
                    deadline = terakhir.getWaktuMaksimal();
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1290");
                }
                o.put("waktuMaksimal", deadline == null ? "" : Common.dateFormat51.get().format(deadline));
                o.put("lewatBatasWaktu", deadline != null && deadline.before(new Date()) && !isSelesai(terakhir));
            } else {
                o.put("status", "");
                o.put("aktorTahap", "");
                o.put("waktuMaksimal", "");
                o.put("lewatBatasWaktu", false);
            }
            o.put("catatan", ApiHelperSupport.safeString(d.getKeterangan()));
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1301");
        }
        return o;
    }

    private static JSONObject mapRiwayatLangkah(DisposisiAlurSop langkah) {
        JSONObject o = new JSONObject();
        try {
            o.put("id", langkah.getId());
            o.put("tahap", langkah.getAlurSop() == null ? "" : ApiHelperSupport.safeString(langkah.getAlurSop().getNama()));
            o.put("aktor", langkah.getAlurSop() == null ? "" : ApiHelperSupport.safeString(langkah.getAlurSop().getAktor()));
            o.put("olehNama", namaAktorLangkah(langkah));
            o.put("waktu", langkah.getWaktu() == null ? "" : Common.dateFormat51.get().format(langkah.getWaktu()));
            o.put("status", getStatusRingkas(langkah));
            o.put("catatan", ApiHelperSupport.safeString(langkah.getKeterangan()));
            o.put("selesai", Boolean.TRUE.equals(langkah.getSelesai()));
            o.put("kembali", Boolean.TRUE.equals(langkah.getKembali()));
            Date deadline = null;
            try {
                deadline = langkah.getWaktuMaksimal();
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1321");
            }
            o.put("waktuMaksimal", deadline == null ? "" : Common.dateFormat51.get().format(deadline));
            String opsi = "";
            try {
                if (langkah.getSetelahnya() != null && langkah.getSetelahnya().getAlurSop() != null) {
                    opsi = ApiHelperSupport.safeString(langkah.getSetelahnya().getAlurSop().getOpsi());
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1329");
            }
            o.put("opsi", opsi);
            o.put("parameterTambahan", decodeParameterTambahanInds(langkah.getParameterTambahanInds()));
            o.put("dokumen", mapDokumenLampiran(langkah));
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1334");
        }
        return o;
    }

    private static JSONObject mapTahapPending(Session session, Tbmuser tbmuser, DisposisiSop disposisiSop, DisposisiAlurSop langkah) {
        JSONObject o = new JSONObject();
        try {
            AlurSop alurSop = langkah.getAlurSop();
            o.put("disposisiAlurSopId", langkah.getId());
            o.put("tahap", alurSop == null ? "" : ApiHelperSupport.safeString(alurSop.getNama()));
            o.put("kode", kodePengajuan(disposisiSop, langkah));
            Date deadline = null;
            try {
                deadline = langkah.getWaktuMaksimal();
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1349");
            }
            o.put("waktuMaksimal", deadline == null ? "" : Common.dateFormat51.get().format(deadline));

            boolean ditolak = false;
            String infoDitolak = "";
            try {
                if (alurSop != null && Boolean.TRUE.equals(alurSop.getPenolakanAdaDiSini()) && langkah.getSebelumnya() != null) {
                    ditolak = true;
                    DisposisiAlurSop sebelumnya = langkah.getSebelumnya();
                    infoDitolak = "Ditolak: " + (sebelumnya.getAlurSop() == null ? "" : ApiHelperSupport.safeString(sebelumnya.getAlurSop().getAktor()))
                            + " " + namaAktorLangkah(sebelumnya);
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1362");
            }
            o.put("ditolak", ditolak);
            o.put("infoDitolak", infoDitolak);

            String jenisPengguna = alurSop != null && alurSop.getAktorSop() != null ? alurSop.getAktorSop().getJenisPengguna() : "";
            String khususUser = alurSop == null ? "" : alurSop.getKhususUsername();
            SopUtil.AktorResolusi resolusi = SopUtil.resolveAktor(tbmuser, khususUser, jenisPengguna, disposisiSop, alurSop);
            o.put("bisaProses", resolusi.ada);
            o.put("bisaBatalkanLangkah", resolusi.ada);
            o.put("aktorLabel", alurSop == null ? "" : ApiHelperSupport.safeString(alurSop.getAktor()));
            o.put("hakAkses", ApiHelperSupport.safeString(jenisPengguna));
            JSONArray aktorList = new JSONArray();
            for (Tbmuser a : resolusi.aktors) {
                JSONObject ao = new JSONObject();
                ao.put("userId", ApiHelperSupport.safeString(a.getUserId()));
                ao.put("nama", ApiHelperSupport.safeString(a.getUserNama()));
                aktorList.put(ao);
            }
            o.put("aktorBerhak", aktorList);

            o.put("parameterDefinisi", buildParameterDefinisi(session, alurSop));
            o.put("dokumenDefinisi", buildDokumenDefinisi(alurSop));
            o.put("nextOptions", buildNextOptions(alurSop));
            o.put("berupaPilihanTunggal", alurSop == null || Boolean.TRUE.equals(alurSop.getAlurSetelahnyaBerupaPilihan()));
            o.put("nextTidakWajib", alurSop != null && Boolean.TRUE.equals(alurSop.getAlurSetelahnyaTidakWajib()));
            o.put("bisaKembali", alurSop != null && Boolean.TRUE.equals(alurSop.getKembaliKeAktorSebelumnya()) && langkah.getSebelumnya() != null);
            o.put("bisaSelesai", alurSop != null && Boolean.TRUE.equals(alurSop.getJikaProsesDisetujuiMakaSelesai()));
            o.put("catatanWajib", alurSop == null || alurSop.getCatatanWajibDiisi() == null || Boolean.TRUE.equals(alurSop.getCatatanWajibDiisi()));
            // Dua konfigurasi tahap yang dipakai form disposisi ZKoss namun belum pernah
            // sampai ke klien: apakah kolom catatan ditampilkan sama sekali, dan apakah
            // waktu disposisi boleh diubah pengguna (DisposisiAlurSopAction baris 720-733).
            o.put("bolehDiisiCatatan", alurSop == null || Boolean.TRUE.equals(alurSop.getBolehDiisiCatatan()));
            o.put("tanggalBolehDiubah", alurSop != null && Boolean.TRUE.equals(alurSop.getTanggalDisposisiBolehDiubah()));
            // Tahap yang membekukan dokumen tidak boleh menerima unggahan baru
            // (DisposisiAlurSopAction baris 1238). Tanpa ini POS mengizinkan unggahan
            // pada tahap yang di ZKoss justru terkunci.
            o.put("bekukanDokumen", alurSop != null && Boolean.TRUE.equals(alurSop.getBekukanDokumen()));
            o.put("lampiranCatatanWajib", alurSop != null && Boolean.TRUE.equals(alurSop.getLampiranCatatanWajibDiisi())
                    && Common.bolehKonfigurasi("tampilkan_lampiran_catatan_disposisi"));
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1393");
        }
        return o;
    }

    private static JSONArray buildParameterDefinisi(Session session, AlurSop alurSop) {
        JSONArray kelompokArr = new JSONArray();
        try {
            for (KelompokParameterTambahanAlurSop kelompok : alurSop.getKelompokParameterTambahanAlurSops()) {
                if (kelompok == null || (kelompok.getAktif() != null && !kelompok.getAktif())) {
                    continue;
                }
                List<ParameterTambahanAlurSop> daftar = ConstantValues.simpleList(
                        session.createCriteria(ParameterTambahanAlurSop.class)
                                .add(Restrictions.eq("kelompokParameterTambahanAlurSop", kelompok))
                                .createAlias("parameterTambahan", "pt").add(Restrictions.eq("pt.aktif", true)),
                        ParameterTambahanAlurSop.class);
                JSONArray paramArr = new JSONArray();
                if (daftar != null) {
                    Collections.sort(daftar);
                    for (ParameterTambahanAlurSop pas : daftar) {
                        ParameterTambahan pt = pas.getParameterTambahan();
                        if (pt == null) {
                            continue;
                        }
                        JSONObject po = new JSONObject();
                        po.put("kelompokId", kelompok.getId());
                        po.put("parameterId", pt.getId());
                        po.put("key", kelompok.getId() + "->" + pt.getId());
                        po.put("label", ApiHelperSupport.safeString(pt.getLabelInputan()));
                        po.put("keterangan", ApiHelperSupport.safeString(pt.getLabelInputanKeterangan()));
                        po.put("tipe", ApiHelperSupport.safeString(pt.getTipeDataInputan()));
                        po.put("pilihan", ApiHelperSupport.safeString(pt.getNilaiDataInputan()));
                        po.put("nilaiDefault", ApiHelperSupport.safeString(pt.getNilaiDefault()));
                        po.put("wajib", Boolean.TRUE.equals(pt.getWajibDiisi()));
                        po.put("harusMenyertakanLampiran", Boolean.TRUE.equals(pt.getHarusMenyertakanLampiran()));
                        po.put("lampiranWajib", Boolean.TRUE.equals(pt.getLampiranWajibDiisi()));
                        paramArr.put(po);
                    }
                }
                JSONObject ko = new JSONObject();
                ko.put("id", kelompok.getId());
                ko.put("nama", ApiHelperSupport.safeString(kelompok.getNama()));
                ko.put("parameter", paramArr);
                kelompokArr.put(ko);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1439");
        }
        return kelompokArr;
    }

    private static JSONArray buildDokumenDefinisi(AlurSop alurSop) {
        JSONArray arr = new JSONArray();
        try {
            for (DokumenAlurSop dok : alurSop.getDokumenAlurSops()) {
                if (dok == null || !Boolean.TRUE.equals(dok.getAktif())) {
                    continue;
                }
                JSONObject o = new JSONObject();
                o.put("id", dok.getId());
                o.put("nama", ApiHelperSupport.safeString(dok.getNama()));
                o.put("wajib", Boolean.TRUE.equals(dok.getWajib()));
                o.put("key", DokumenAlurSop.class.getName() + "_alur_" + dok.getId());
                arr.put(o);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1458");
        }
        return arr;
    }

    private static JSONArray buildNextOptions(AlurSop alurSop) {
        JSONArray arr = new JSONArray();
        try {
            List<AlurSop> nexts = alurSop.ambilAlurSetelahnya();
            List<String> opsis = alurSop.ambilOpsiAlurSetelahnya();
            for (int i = 0; nexts != null && i < nexts.size(); i++) {
                AlurSop next = nexts.get(i);
                if (next == null) {
                    continue;
                }
                JSONObject o = new JSONObject();
                o.put("alurSopId", next.getId());
                o.put("nama", ApiHelperSupport.safeString(next.getNama()));
                o.put("opsi", opsis != null && i < opsis.size() && opsis.get(i) != null ? opsis.get(i).trim() : "");
                // ZKoss merender PRATINJAU penerima tahap lanjutan (DisposisiAlurSopAction
                // baris 1035/1171): bila tahap tujuan ber-kembaliKePengaju, dokumen kembali ke
                // PENGAJU, bukan ke aktor berbasis peran. Tanpa penanda ini pengguna memilih
                // rute tanpa tahu ke siapa dokumennya pergi.
                o.put("kembaliKePengaju", Boolean.TRUE.equals(next.getKembaliKePengaju()));
                o.put("aktorLabel", next.getAktorSop() == null ? ""
                        : ApiHelperSupport.safeString(next.getAktor()));
                arr.put(o);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1479");
        }
        return arr;
    }

    private static boolean tidakAdaLangkahLanjutan(AlurSop alurSop) {
        if (alurSop == null) {
            return true;
        }
        try {
            return alurSop.getSetelahnya() == null && alurSop.getSetelahnya2() == null && alurSop.getSetelahnya3() == null
                    && alurSop.getSetelahnya4() == null && alurSop.getSetelahnya5() == null && alurSop.getSetelahnya6() == null
                    && alurSop.getSetelahnya7() == null && alurSop.getSetelahnya8() == null && alurSop.getSetelahnya9() == null
                    && alurSop.getSetelahnya10() == null;
        } catch (Exception e) {
            return true;
        }
    }

    private static JSONArray decodeParameterTambahanInds(String raw) {
        JSONArray arr = new JSONArray();
        if (!ApiHelperSupport.hasText(raw)) {
            return arr;
        }
        try {
            String[] lines = raw.split("\n");
            for (String line : lines) {
                if (!ApiHelperSupport.hasText(line)) {
                    continue;
                }
                String[] parts = line.split("<=>");
                String key = parts.length > 0 ? parts[0].trim() : "";
                String value = parts.length > 1 ? parts[1].trim() : "";
                String url = parts.length > 2 ? parts[2].trim() : "";
                String catatan = parts.length > 3 ? parts[3].trim() : "";
                JSONObject o = new JSONObject();
                o.put("key", key);
                o.put("nilai", value);
                o.put("url", url);
                o.put("catatan", catatan);
                String[] kp = key.split("->");
                if (kp.length > 1) {
                    try {
                        Long paramId = Long.parseLong(kp[1].trim());
                        ParameterTambahan pt = (ParameterTambahan) ConstantValues.ambil(ParameterTambahan.class.getName(), paramId);
                        if (pt != null) {
                            o.put("label", ApiHelperSupport.safeString(pt.getLabelInputan()));
                        }
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1527");
                    }
                }
                arr.put(o);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1532");
        }
        return arr;
    }

    /** Lampiran dokumen tetap per tahap (DokumenAlurSop) — port §2.6 (ref key beda untuk tahap start vs lanjutan). */
    private static JSONArray mapDokumenLampiran(DisposisiAlurSop langkah) {
        JSONArray arr = new JSONArray();
        try {
            if (langkah.getAlurSop() == null) {
                return arr;
            }
            boolean isStart = Boolean.TRUE.equals(langkah.getAlurSop().getStart());
            Long refId = isStart ? (langkah.getDisposisiSop() == null ? null : langkah.getDisposisiSop().getId()) : langkah.getId();
            String clazzName = isStart ? DisposisiSop.class.getName() : DisposisiAlurSop.class.getName();
            for (DokumenAlurSop dok : langkah.getAlurSop().getDokumenAlurSops()) {
                if (dok == null || !Boolean.TRUE.equals(dok.getAktif())) {
                    continue;
                }
                String jenis = DokumenAlurSop.class.getName() + (isStart ? "_" : "_alur_") + dok.getId();
                JSONObject o = new JSONObject();
                o.put("nama", ApiHelperSupport.safeString(dok.getNama()));
                o.put("wajib", Boolean.TRUE.equals(dok.getWajib()));
                o.put("key", jenis);
                LampiranLain lampiran = refId == null ? null : LampiranLain.ambil(refId, jenis);
                if (lampiran != null) {
                    o.put("fileNama", ApiHelperSupport.safeString(lampiran.getNama()));
                    o.put("url", "/AmbilLampiran?download=1&ref=" + refId + "&clazz=" + clazzName + "&jenis=" + jenis);
                }
                arr.put(o);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1563");
        }
        return arr;
    }

    /**
     * Data form dinamis (FormSop) terkait pengajuan, ditampilkan READ-ONLY (flatten key-value) —
     * port cascading lookup {@code resolveFormData} dari TampilanAlurSopAction.
     */
    private static JSONObject resolveFormDataReadOnly(Session session, DisposisiSop disposisiSop, List<DisposisiAlurSop> semuaLangkah) {
        JSONObject hasil = new JSONObject();
        try {
            String formClass = null;
            for (DisposisiAlurSop langkah : semuaLangkah) {
                if (langkah.getAlurSop() != null && ApiHelperSupport.hasText(langkah.getAlurSop().getFormInputan())) {
                    formClass = langkah.getAlurSop().getFormInputan();
                    break;
                }
            }
            if (!ApiHelperSupport.hasText(formClass)) {
                return hasil;
            }

            Object obj = Class.forName(formClass.trim()).newInstance();
            if (!(obj instanceof FormSop)) {
                return hasil;
            }
            FormSop formSop = (FormSop) obj;
            formSop.setPersetujuan(true);

            GeneralValueObject data = null;
            try {
                data = disposisiSop.ambil(session, formSop);
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1596");
            }

            if ((data == null || data.getId() == null) && ApiHelperSupport.hasText(disposisiSop.getProperti())) {
                try {
                    JSONObject root = new JSONObject(disposisiSop.getProperti());
                    JSONObject jo = root;
                    String key = formSop.ambilClass() == null ? null : formSop.ambilClass().getName();
                    if (ApiHelperSupport.hasText(key) && root.has(key) && !root.isNull(key)) {
                        jo = root.getJSONObject(key);
                    }
                    if (jo != null && jo.has("id") && !jo.isNull("id") && formSop.ambilClass() != null) {
                        String idStr = String.valueOf(jo.get("id"));
                        if (ApiHelperSupport.hasText(idStr)) {
                            data = (GeneralValueObject) GeneralValueObject.ambilData(formSop.ambilClass(), idStr, true);
                        }
                    }
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1613");
                }
            }

            if ((data == null || data.getId() == null) && formSop.ambilClass() != null) {
                try {
                    data = (GeneralValueObject) session.createCriteria(formSop.ambilClass())
                            .add(Restrictions.eq("disposisiSop", disposisiSop)).setMaxResults(1)
                            .setFlushMode(org.hibernate.FlushMode.MANUAL).uniqueResult();
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1622");
                }
            }

            if (data == null || data.getId() == null) {
                return hasil;
            }

            hasil.put("id", data.getId());
            hasil.put("kode", ApiHelperSupport.safeString(data.getKode()));
            hasil.put("nama", ApiHelperSupport.safeString(data.getNama()));
            hasil.put("formClass", formClass);
            String istilah = "";
            try {
                istilah = formSop.istilah();
            } catch (Exception e) {
            }
            hasil.put("istilah", ApiHelperSupport.hasText(istilah) ? istilah : "Data");
            hasil.put("fields", buildDynamicDataFields(formSop.ambilClass(), data));
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1635");
        }
        return hasil;
    }

    // ════════════════════════════════════════════════════════════════════════
    // Ekstraksi field "Data" secara DINAMIS untuk SEMUA entitas yang menjadi
    // basis class *Action yang mengimplementasikan ais.ui.util.FormSop.
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Field teknis/boilerplate yang ada pada hampir SEMUA entitas turunan
     * {@code DataSop}/{@code GeneralValueObject} (audit, referensi balik ke SOP, dsb.) —
     * tidak relevan ditampilkan sebagai "Data" ke pengguna, terlepas dari class *Action
     * mana yang memakainya.
     */
    private static final Set<String> FIELD_TEKNIS_DATASOP = new HashSet<String>(Arrays.asList("id", "oleh", "olehId",
            "tanggal_dirubah", "tanggalDirubah", "kodeUnik", "kode_unik", "disposisiSop", "properti", "fileLocation",
            "aktif", "keyword", "usernamePengguna"));

    /**
     * Ekstraksi field "Data" secara DINAMIS dari entitas manapun yang menjadi basis salah satu
     * dari SEMUA class *Action yang mengimplementasikan {@link FormSop} (UangMukaAction,
     * PembayaranGajiAction, SuratKeluarAction, SkripsiAction, dst — 50+ class, dan otomatis
     * mencakup class baru di masa depan tanpa perlu kode tambahan per-class).
     *
     * <p>Tidak melakukan reflection Java mentah (yang bisa memicu getter ber-efek-samping/berat),
     * melainkan memakai {@link org.hibernate.metadata.ClassMetadata} — mekanisme YANG SAMA dipakai
     * {@code Common.insertProperty}/{@code ManajemenProperty} di seluruh aplikasi ini untuk
     * menyerialisasi entitas — sehingga hanya properti yang benar-benar ter-mapping Hibernate
     * (persis field yang disimpan ke database oleh {@code onSave()} masing-masing *Action) yang
     * diekstrak, dalam urutan deklarasi mapping-nya. Ini adalah satu-satunya sumber metadata field
     * yang benar-benar seragam di seluruh 50+ class tersebut (tidak ada superclass/helper form-row
     * yang dipakai bersama oleh class-class itu), sehingga pendekatan berbasis Hibernate metadata
     * inilah yang dapat "dinamis" mencakup semuanya tanpa konfigurasi manual per-class.</p>
     *
     * <p>Label ditampilkan dengan meng-humanize nama properti (mis. {@code tanggalKegiatan} →
     * "Tanggal Kegiatan") — cocok untuk mayoritas field karena konvensi penamaan getter di codebase
     * ini memang sudah dekat dengan label yang ditampilkan pada form ZK aslinya. Relasi ke entitas
     * lain (mis. Satuan Kerja, Bank, Akun) ditampilkan sebagai satu nilai bersih (nama, fallback
     * kode) alih-alih di-dump seluruh objeknya.</p>
     */
    private static JSONArray buildDynamicDataFields(Class<?> entityClass, GeneralValueObject entity) {
        JSONArray arr = new JSONArray();
        try {
            if (entityClass == null || entity == null) {
                return arr;
            }
            org.hibernate.metadata.ClassMetadata meta = HibernateUtil.getClassMetadata(entityClass);
            if (meta == null) {
                return arr;
            }
            String[] properties = meta.getPropertyNames();
            if (properties == null) {
                return arr;
            }
            int jumlah = 0;
            for (String property : properties) {
                if (!ApiHelperSupport.hasText(property) || FIELD_TEKNIS_DATASOP.contains(property)) {
                    continue;
                }
                if (jumlah >= 60) {
                    break;
                }
                try {
                    Object val = meta.getPropertyValue(entity, property, org.hibernate.EntityMode.POJO);
                    String tampil = formatNilaiDataDinamis(val);
                    if (tampil == null) {
                        continue;
                    }
                    JSONObject o = new JSONObject();
                    o.put("label", humanizeLabelProperti(property));
                    o.put("property", property);
                    o.put("nilai", tampil);
                    arr.put(o);
                    jumlah++;
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
        }
        return arr;
    }

    private static String formatNilaiDataDinamis(Object val) {
        try {
            if (val == null) {
                return null;
            }
            if (val instanceof Date) {
                return Common.dateFormat51.get().format((Date) val);
            }
            if (val instanceof Boolean) {
                return Boolean.TRUE.equals(val) ? "Ya" : "Tidak";
            }
            if (val instanceof GeneralValueObject) {
                if (!org.hibernate.Hibernate.isInitialized(val)) {
                    return null;
                }
                GeneralValueObject rel = (GeneralValueObject) val;
                try {
                    if (ApiHelperSupport.hasText(rel.getNama())) {
                        return rel.getNama();
                    }
                } catch (Exception e) {
                }
                try {
                    if (ApiHelperSupport.hasText(rel.getKode())) {
                        return rel.getKode();
                    }
                } catch (Exception e) {
                }
                return rel.getId() == null ? null : ("#" + rel.getId());
            }
            if (val instanceof java.util.Collection) {
                if (!org.hibernate.Hibernate.isInitialized(val)) {
                    return null;
                }
                java.util.Collection<?> col = (java.util.Collection<?>) val;
                if (col.isEmpty()) {
                    return null;
                }
                StringBuilder sb = new StringBuilder();
                int n = 0;
                for (Object o : col) {
                    if (n >= 5) {
                        sb.append(", …");
                        break;
                    }
                    if (n > 0) {
                        sb.append(", ");
                    }
                    String label = null;
                    if (o instanceof GeneralValueObject) {
                        try {
                            label = ((GeneralValueObject) o).getNama();
                        } catch (Exception e) {
                        }
                    }
                    sb.append(ApiHelperSupport.hasText(label) ? label : String.valueOf(o));
                    n++;
                }
                return sb.toString();
            }
            if (val instanceof Number) {
                return Common.numberFormat.get().format(val);
            }
            String s = String.valueOf(val).trim();
            return s.isEmpty() ? null : s;
        } catch (Exception e) {
            return null;
        }
    }

    private static String humanizeLabelProperti(String property) {
        if (!ApiHelperSupport.hasText(property)) {
            return property;
        }
        StringBuilder spaced = new StringBuilder();
        for (int i = 0; i < property.length(); i++) {
            char c = property.charAt(i);
            if (c == '_') {
                spaced.append(' ');
                continue;
            }
            if (i > 0 && Character.isUpperCase(c) && !Character.isUpperCase(property.charAt(i - 1))) {
                spaced.append(' ');
            }
            spaced.append(c);
        }
        String[] words = spaced.toString().trim().split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(w.charAt(0)));
            if (w.length() > 1) {
                out.append(w.substring(1));
            }
        }
        return out.toString();
    }

    /** Overview alur kerja (semua tahap desain SOP + status masing-masing) — port ambilSemuaAlurSopUntukOverview. */
    private static JSONObject buildWorkflowOverview(Session session, DisposisiSop disposisiSop, List<DisposisiAlurSop> semuaLangkah) {
        JSONObject hasil = new JSONObject();
        JSONArray nodes = new JSONArray();
        int selesaiCount = 0, menungguCount = 0, belumDilewatiCount = 0, lewatCount = 0;
        try {
            if (disposisiSop.getSop() == null) {
                hasil.put("nodes", nodes);
                hasil.put("status", "Belum Ada Alur");
                return hasil;
            }
            @SuppressWarnings("unchecked")
            List<AlurSop> semuaNode = session.createCriteria(AlurSop.class).add(Restrictions.eq("sop", disposisiSop.getSop()))
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .addOrder(Order.asc("nomor")).addOrder(Order.asc("kode")).addOrder(Order.asc("nama")).list();

            Map<Long, DisposisiAlurSop> byAlur = new HashMap<Long, DisposisiAlurSop>();
            for (DisposisiAlurSop l : semuaLangkah) {
                if (l.getAlurSop() != null && l.getAlurSop().getId() != null && !byAlur.containsKey(l.getAlurSop().getId())) {
                    byAlur.put(l.getAlurSop().getId(), l);
                }
            }

            Date now = new Date();
            for (AlurSop node : semuaNode) {
                DisposisiAlurSop match = node.getId() == null ? null : byAlur.get(node.getId());
                boolean sudahDilewati = match != null;
                boolean sudahDiisi = match != null
                        && (match.getDiajukanOleh() != null || match.getMahasiswa() != null || match.getSiswa() != null);
                Date deadline = null;
                try {
                    deadline = match == null ? null : match.getWaktuMaksimal();
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1672");
                }
                boolean lewat = sudahDilewati && !sudahDiisi && deadline != null && deadline.before(now);

                String status;
                if (sudahDiisi) {
                    status = "selesai";
                    selesaiCount++;
                } else if (lewat) {
                    status = "lewat_batas_waktu";
                    lewatCount++;
                } else if (sudahDilewati) {
                    status = "menunggu";
                    menungguCount++;
                } else {
                    status = "belum_dilewati";
                    belumDilewatiCount++;
                }

                JSONObject o = new JSONObject();
                o.put("alurSopId", node.getId());
                o.put("nama", ApiHelperSupport.safeString(node.getNama()));
                o.put("aktor", ApiHelperSupport.safeString(node.getAktor()));
                o.put("start", Boolean.TRUE.equals(node.getStart()));
                o.put("status", status);
                o.put("waktuMaksimal", deadline == null ? "" : Common.dateFormat51.get().format(deadline));
                nodes.put(o);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1700");
        }
        try {
            hasil.put("nodes", nodes);
            String status;
            if (nodes.length() == 0) {
                status = "Belum Ada Alur";
            } else if (menungguCount == 0 && belumDilewatiCount == 0) {
                status = "Selesai";
            } else if (lewatCount > 0) {
                status = "Perlu Segera Ditindaklanjuti";
            } else {
                status = "Sedang Berjalan";
            }
            hasil.put("status", status);
            JSONObject ringkasan = new JSONObject();
            ringkasan.put("selesai", selesaiCount);
            ringkasan.put("menunggu", menungguCount);
            ringkasan.put("belumDilewati", belumDilewatiCount);
            ringkasan.put("lewatBatasWaktu", lewatCount);
            hasil.put("ringkasan", ringkasan);
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1721");
        }
        return hasil;
    }

    // ════════════════════════════════════════════════════════════════════════
    // Helper kode pengajuan, nama, status — port dari DasboardSop.java & TampilanAlurSopAction.java
    // ════════════════════════════════════════════════════════════════════════

    private static String getNamaSop(DisposisiAlurSop row) {
        try {
            if (row != null && row.getDisposisiSop() != null && row.getDisposisiSop().getSop() != null) {
                return row.getDisposisiSop().getSop().getNama();
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1735");
        }
        return "SOP Tidak Diketahui";
    }

    private static String getNamaAktor(DisposisiAlurSop row) {
        try {
            if (row != null && row.getAlurSop() != null) {
                String aktor = row.getAlurSop().getAktor();
                String tahap = row.getAlurSop().getNama();
                if (ApiHelperSupport.hasText(aktor) && ApiHelperSupport.hasText(tahap)) {
                    return aktor + " - " + tahap;
                }
                if (ApiHelperSupport.hasText(aktor)) {
                    return aktor;
                }
                if (ApiHelperSupport.hasText(tahap)) {
                    return tahap;
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1755");
        }
        return "Tahap Tidak Diketahui";
    }

    private static boolean isSelesai(DisposisiAlurSop row) {
        try {
            return Boolean.TRUE.equals(row.getSelesai());
        } catch (Exception e) {
            return false;
        }
    }

    private static String getStatusRingkas(DisposisiAlurSop row) {
        try {
            if (isSelesai(row)) {
                return "Selesai";
            }
            if (row.getDiajukanOleh() == null && row.getSiswa() == null && row.getMahasiswa() == null) {
                return "Menunggu Disposisi";
            }
            if (row.getWaktu() != null) {
                return "Sudah Diproses";
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1779");
        }
        return "Dalam Proses";
    }

    private static String namaAktorLangkah(DisposisiAlurSop langkah) {
        try {
            if (langkah.getMahasiswa() != null) {
                return ApiHelperSupport.safeString(langkah.getMahasiswa().getNama());
            }
            if (langkah.getSiswa() != null) {
                return ApiHelperSupport.safeString(langkah.getSiswa().getNama());
            }
            if (langkah.getDiajukanOleh() != null) {
                return ApiHelperSupport.safeString(langkah.getDiajukanOleh().getUserNama());
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1795");
        }
        return "";
    }

    private static String namaPengaju(DisposisiSop d) {
        try {
            if (d == null) {
                return "";
            }
            if (d.getMahasiswa() != null) {
                return ApiHelperSupport.safeString(d.getMahasiswa().getNama());
            }
            if (d.getSiswa() != null) {
                return ApiHelperSupport.safeString(d.getSiswa().getNama());
            }
            if (d.getDiajukanOleh() != null) {
                return ApiHelperSupport.safeString(d.getDiajukanOleh().getUserNama());
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1814");
        }
        return "";
    }

    /** Port ambilKodeDariProperti + renderKodePengajuanMultiSumber: coba disposisiSop.properti → langkah.properti → disposisiStart.properti. */
    private static String kodePengajuan(DisposisiSop disposisiSop, DisposisiAlurSop step) {
        try {
            String kode = kodeDariProperti(disposisiSop == null ? null : disposisiSop.getProperti());
            if (ApiHelperSupport.hasText(kode)) {
                return kode;
            }
            kode = kodeDariProperti(step == null ? null : step.getProperti());
            if (ApiHelperSupport.hasText(kode)) {
                return kode;
            }
            if (disposisiSop != null && disposisiSop.getDisposisiStart() != null) {
                kode = kodeDariProperti(disposisiSop.getDisposisiStart().getProperti());
                if (ApiHelperSupport.hasText(kode)) {
                    return kode;
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1836");
        }
        return "";
    }

    private static String kodeDariProperti(String propertiJson) {
        if (!ApiHelperSupport.hasText(propertiJson)) {
            return "";
        }
        try {
            JSONObject root = new JSONObject(propertiJson);
            for (String key : KODE_KEYS) {
                if (root.has(key) && !root.isNull(key)) {
                    String v = root.optString(key, "");
                    if (ApiHelperSupport.hasText(v)) {
                        return v;
                    }
                }
            }
            Iterator<String> it = root.keys();
            while (it.hasNext()) {
                String k = it.next();
                Object v = root.opt(k);
                if (v instanceof JSONObject) {
                    JSONObject sub = (JSONObject) v;
                    for (String key : KODE_KEYS) {
                        if (sub.has(key) && !sub.isNull(key)) {
                            String s = sub.optString(key, "");
                            if (ApiHelperSupport.hasText(s)) {
                                return s;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1871");
        }
        return "";
    }

    // ════════════════════════════════════════════════════════════════════════
    // Util umum
    // ════════════════════════════════════════════════════════════════════════

    private static String safeRoleId(Tbmuser tbmuser) {
        try {
            return tbmuser.hakAkses() == null || tbmuser.hakAkses().getRoleId() == null ? ""
                    : tbmuser.hakAkses().getRoleId().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }

    private static Date parseTanggal(JSONObject json, String key) {
        try {
            if (ApiHelperSupport.isNullOrEmptyJsonValue(json, key)) {
                return null;
            }
            return Common.dateFormat1.get().parse(ApiHelperSupport.optString(json, key));
        } catch (Exception e) {
            return null;
        }
    }

    private static int safeInt(JSONObject json, String key, int def) {
        try {
            if (ApiHelperSupport.isNullOrEmptyJsonValue(json, key)) {
                return def;
            }
            return Integer.parseInt(ApiHelperSupport.optString(json, key).trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static void closeQuietly(Session session) {
        if (session == null) {
            return;
        }
        try {
            session.clear();
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1921");
        }
        try {
            session.disconnect();
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1925");
        }
        try {
            session.close();
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/SopService.java:1929");
        }
    }
}
