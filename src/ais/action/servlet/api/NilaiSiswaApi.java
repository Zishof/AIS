package ais.action.servlet.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.sekolah.util.GrupPenilaianUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.DetailGrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.DetailGrupPenilaian;
import ais.database.model.sekolah.DetailJenisPenilaian;
import ais.database.model.sekolah.GrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.GrupPenilaian;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.JenisItemPenilaianSiswa;
import ais.database.model.sekolah.JenisPenilaian;
import ais.database.model.sekolah.KategoriItemPenilaianSiswa;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.KurikulumPunyaMatapelajaran;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.NilaiHurufSekolah;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.WaktuUtil;

/**
 * API untuk fitur penilaian siswa sekolah (mobile).
 * Menyediakan endpoint untuk membaca dan menulis nilai siswa oleh guru.
 */
public class NilaiSiswaApi {

    /**
     * Mengambil struktur nilai lengkap beserta ID-ID yang diperlukan untuk input nilai.
     * Digunakan oleh guru saat akan memasukkan atau mengubah nilai siswa.
     *
     * Request: { "token": "...", "jadwal": 123, "smt": 1 }
     * Response: { "status": "00", "data": { kelas info, siswas[], grup_penilaians[] } }
     */
    @SuppressWarnings("unchecked")
    public static JSONObject daftar_nilai_untuk_input(HttpServletRequest req, JSONObject request) {
        JSONObject jsonObject = new JSONObject();
        try {
            Tbmuser tbmuser = ApiUtil.currentUser(request, req);
            if (tbmuser == null || tbmuser.getUserId() == null) {
                jsonObject.put("status", "97");
                jsonObject.put("description", "Token tidak sesuai");
                return jsonObject;
            }

            Guru guru = tbmuser.ambilGuru();
            if (guru == null) {
                jsonObject.put("status", "91");
                jsonObject.put("description", "Hanya guru yang dapat mengakses fitur ini");
                return jsonObject;
            }

            Long jadwalId = request.isNull("jadwal") ? null : Long.parseLong(request.get("jadwal") + "");
            Integer smt = request.isNull("smt") ? null : request.getInt("smt");

            if (jadwalId == null) {
                jsonObject.put("status", "91");
                jsonObject.put("description", "ID jadwal harus disertakan");
                return jsonObject;
            }
            if (smt == null) {
                jsonObject.put("status", "91");
                jsonObject.put("description", "Semester harus disertakan");
                return jsonObject;
            }

            Session session = HibernateUtil.openSession();
            try {
                JadwalPelajaran jadwalPelajaran = (JadwalPelajaran) session.get(JadwalPelajaran.class, jadwalId);
                if (jadwalPelajaran == null) {
                    jsonObject.put("status", "91");
                    jsonObject.put("description", "Jadwal tidak ditemukan");
                    return jsonObject;
                }

                KelasSiswa kelasSiswa = jadwalPelajaran.getKelas();
                Matapelajaran matapelajaran = jadwalPelajaran.getMatapelajaran();

                if (kelasSiswa == null || matapelajaran == null) {
                    jsonObject.put("status", "91");
                    jsonObject.put("description", "Data kelas atau matapelajaran tidak lengkap pada jadwal ini");
                    return jsonObject;
                }

                JSONObject data = new JSONObject();
                data.put("jadwal_id", jadwalPelajaran.getId());
                data.put("kelas_id", kelasSiswa.getId());
                data.put("kelas", kelasSiswa.getNama());
                data.put("sekolah", kelasSiswa.getSekolah() == null ? "" : kelasSiswa.getSekolah().getNama());
                data.put("matapelajaran", matapelajaran.getNama());
                data.put("matapelajaran_kode", matapelajaran.getKode() == null ? "" : matapelajaran.getKode());
                data.put("matapelajaran_id", matapelajaran.getId());
                data.put("guru", guru.getNama());
                data.put("ta", kelasSiswa.getTahunAjaran());
                data.put("smt", smt);
                data.put("tingkat", kelasSiswa.getTingkat());

                // Ambil daftar siswa di kelas ini (aktif, diurutkan by nama)
                List<KelasSiswaPunyaSiswa> siswas = ConstantValues.simpleList(
                        session.createCriteria(KelasSiswaPunyaSiswa.class)
                                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                .add(Restrictions.eq("kelasSiswa", kelasSiswa))
                                .createAlias("siswa", "siswa")
                                .addOrder(Order.asc("siswa.nama")),
                        KelasSiswaPunyaSiswa.class);

                // Tentukan jenis penilaian untuk mata pelajaran ini
                JenisPenilaian jenisPenilaian = matapelajaran.getJenisPenilaian();
                if (kelasSiswa.getKurikulumSekolah() != null
                        && kelasSiswa.getKurikulumSekolah().getJenisPenilaian() != null) {
                    jenisPenilaian = kelasSiswa.getKurikulumSekolah().getJenisPenilaian();
                }

                // Ambil grup penilaian
                List<GrupPenilaian> grupPenilaians = ConstantValues.simpleList(
                        session.createCriteria(DetailJenisPenilaian.class)
                                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                .add(Restrictions.eq("jenisPenilaian", jenisPenilaian))
                                .setProjection(Projections.groupProperty("grupPenilaian.id")),
                        GrupPenilaian.class, false);
                Collections.sort(grupPenilaians);

                // Kumpulkan semua grup kategori dan jenis item untuk lookup nilai per siswa
                List<GrupKategoriItemPenilaianSiswa> allGrupKategori = new ArrayList<GrupKategoriItemPenilaianSiswa>();
                List<List<JenisItemPenilaianSiswa>> allJenisItems = new ArrayList<List<JenisItemPenilaianSiswa>>();

                JSONArray jsonArrayGrupPenilaians = new JSONArray();
                for (GrupPenilaian grupPenilaian : grupPenilaians) {
                    if (grupPenilaian != null && kelasSiswa.getTingkat() > 0
                            && grupPenilaian.getKhususTingkat() != null
                            && !grupPenilaian.getKhususTingkat().equals(kelasSiswa.getTingkat())) {
                        continue;
                    }

                    JSONObject jsonGrup = new JSONObject();
                    jsonGrup.put("id", grupPenilaian.getId());
                    jsonGrup.put("nama", grupPenilaian.getNama());
                    jsonGrup.put("keterangan", grupPenilaian.getKeterangan());

                    List<GrupKategoriItemPenilaianSiswa> grupKategoriItems = ConstantValues.simpleList(
                            session.createCriteria(DetailGrupPenilaian.class)
                                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                    .add(Restrictions.isNotNull("grupKategoriItemPenilaianSiswa"))
                                    .setProjection(Projections.groupProperty("grupKategoriItemPenilaianSiswa.id"))
                                    .add(Restrictions.eq("grupPenilaian", grupPenilaian)),
                            GrupKategoriItemPenilaianSiswa.class, false);
                    Collections.sort(grupKategoriItems);

                    JSONArray jsonArrayKategori = new JSONArray();
                    for (GrupKategoriItemPenilaianSiswa grupKategori : grupKategoriItems) {
                        if (grupKategori != null && kelasSiswa.getTingkat() > 0
                                && grupKategori.getKhususTingkat() != null
                                && !grupKategori.getKhususTingkat().equals(kelasSiswa.getTingkat())) {
                            continue;
                        }

                        List<KategoriItemPenilaianSiswa> kategoriIds = ConstantValues.simpleList(
                                session.createCriteria(DetailGrupKategoriItemPenilaianSiswa.class)
                                        .add(Restrictions.eq("grupKategoriItemPenilaianSiswa", grupKategori))
                                        .add(Restrictions.or(Restrictions.isNull("aktif"),
                                                Restrictions.eq("aktif", true)))
                                        .setProjection(Projections.groupProperty("kategoriItemPenilaianSiswa.id")),
                                KategoriItemPenilaianSiswa.class, false);

                        // Guard: Restrictions.in dengan list kosong melempar exception
                        // yang tertelan diam-diam — kategori tanpa item harus tetap
                        // menghasilkan jenis_items kosong yang eksplisit.
                        List<JenisItemPenilaianSiswa> jenisItems;
                        if (kategoriIds == null || kategoriIds.isEmpty()) {
                            jenisItems = new ArrayList<JenisItemPenilaianSiswa>();
                        } else {
                            jenisItems = ConstantValues.simpleList(
                                    session.createCriteria(JenisItemPenilaianSiswa.class)
                                            .createAlias("kategoriItemPenilaianSiswa", "kat")
                                            .addOrder(Order.asc("kat.kode"))
                                            .addOrder(Order.asc("nomorUrut"))
                                            .add(Restrictions.in("kategoriItemPenilaianSiswa", kategoriIds))
                                            .add(Restrictions.or(Restrictions.isNull("aktif"),
                                                    Restrictions.eq("aktif", true))),
                                    JenisItemPenilaianSiswa.class);
                        }

                        JSONObject jsonKategori = new JSONObject();
                        jsonKategori.put("id", grupKategori.getId());
                        jsonKategori.put("nama", grupKategori.getNama());

                        JSONArray jsonArrayItems = new JSONArray();
                        for (JenisItemPenilaianSiswa item : jenisItems) {
                            JSONObject jsonItem = new JSONObject();
                            jsonItem.put("id", item.getId());
                            jsonItem.put("nama", item.getNama());
                            jsonItem.put("kode", item.getKode() == null ? "" : item.getKode());
                            jsonItem.put("tipe", item.getTipeDataInputan());
                            jsonItem.put("is_formula", JenisItemPenilaianSiswa.FORMULA.equals(item.getTipeDataInputan()));
                            jsonArrayItems.put(jsonItem);
                        }
                        jsonKategori.put("jenis_items", jsonArrayItems);
                        jsonArrayKategori.put(jsonKategori);

                        allGrupKategori.add(grupKategori);
                        allJenisItems.add(jenisItems);
                    }
                    jsonGrup.put("grup_kategori_items", jsonArrayKategori);
                    jsonArrayGrupPenilaians.put(jsonGrup);
                }
                data.put("grup_penilaians", jsonArrayGrupPenilaians);

                // Bangun array siswa beserta nilai mereka saat ini
                JSONArray jsonArraySiswas = new JSONArray();
                for (KelasSiswaPunyaSiswa ksp : siswas) {
                    Siswa siswa = ksp.getSiswa();
                    if (siswa == null) continue;

                    JSONObject jsonSiswa = new JSONObject();
                    jsonSiswa.put("id", siswa.getId());
                    jsonSiswa.put("kelas_siswa_punya_siswa_id", ksp.getId());
                    jsonSiswa.put("nis", siswa.getNomorInduk() == null ? "" : siswa.getNomorInduk());
                    jsonSiswa.put("nama", siswa.getNama());
                    jsonSiswa.put("foto", CommonMedia.getUrlFotoPengguna(new Tbmuser(siswa)));

                    // Kumpulkan nilai per jenis item untuk siswa ini
                    JSONArray jsonArrayNilai = new JSONArray();
                    for (int i = 0; i < allGrupKategori.size(); i++) {
                        GrupKategoriItemPenilaianSiswa grupKategori = allGrupKategori.get(i);
                        List<JenisItemPenilaianSiswa> items = allJenisItems.get(i);

                        for (JenisItemPenilaianSiswa item : items) {
                            if (JenisItemPenilaianSiswa.FORMULA.equals(item.getTipeDataInputan())) {
                                continue; // Nilai formula dihitung otomatis, tidak perlu ditampilkan untuk input
                            }
                            String nilaiStr = ksp.retreiveDetailNilai(item, grupKategori, matapelajaran, smt, null);
                            Boolean verified = ksp.retreiveDetailVerify(item, grupKategori, matapelajaran, smt);

                            JSONObject jsonNilaiItem = new JSONObject();
                            jsonNilaiItem.put("grup_kategori_item_id", grupKategori.getId());
                            jsonNilaiItem.put("jenis_item_id", item.getId());
                            jsonNilaiItem.put("nilai", nilaiStr);
                            jsonNilaiItem.put("verified", verified);
                            jsonArrayNilai.put(jsonNilaiItem);
                        }
                    }
                    jsonSiswa.put("nilai", jsonArrayNilai);
                    jsonArraySiswas.put(jsonSiswa);
                }
                data.put("siswas", jsonArraySiswas);

                jsonObject.put("status", "00");
                jsonObject.put("data", data);

            } finally {
                HibernateUtil.closeSessionQuietly(session);
            }

        } catch (Exception e) {
            String err = Common.tampilErrorJikaAdmin(e);
            try {
                jsonObject.put("status", "90");
                jsonObject.put("description", err);
            } catch (Exception ee) {
                ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/NilaiSiswaApi.java:272");
            }
        }
        return jsonObject;
    }

    /**
     * Menyimpan atau memperbarui nilai satu siswa untuk satu jenis item penilaian.
     * Hanya dapat diakses oleh guru.
     *
     * Request: {
     *   "token": "...",
     *   "kelas_siswa_punya_siswa_id": 456,
     *   "jenis_item_penilaian_id": 1,
     *   "matapelajaran_id": 10,
     *   "grup_kategori_item_id": 1,
     *   "smt": 1,
     *   "nilai": "85"
     * }
     */
    @SuppressWarnings("unchecked")
    public static JSONObject input_nilai_siswa(HttpServletRequest req, JSONObject request) {
        JSONObject jsonObject = new JSONObject();
        try {
            Tbmuser tbmuser = ApiUtil.currentUser(request, req);
            if (tbmuser == null || tbmuser.getUserId() == null) {
                jsonObject.put("status", "97");
                jsonObject.put("description", "Token tidak sesuai");
                return jsonObject;
            }

            Guru guru = tbmuser.ambilGuru();
            if (guru == null) {
                jsonObject.put("status", "91");
                jsonObject.put("description", "Hanya guru yang dapat memasukkan nilai siswa");
                return jsonObject;
            }

            Long kspId = request.isNull("kelas_siswa_punya_siswa_id") ? null
                    : Long.parseLong(request.get("kelas_siswa_punya_siswa_id") + "");
            Long jenisItemId = request.isNull("jenis_item_penilaian_id") ? null
                    : Long.parseLong(request.get("jenis_item_penilaian_id") + "");
            Long matpelId = request.isNull("matapelajaran_id") ? null
                    : Long.parseLong(request.get("matapelajaran_id") + "");
            Long grupKategoriId = request.isNull("grup_kategori_item_id") ? null
                    : Long.parseLong(request.get("grup_kategori_item_id") + "");
            Integer smt = request.isNull("smt") ? null : request.getInt("smt");
            String nilaiStr = request.isNull("nilai") ? "" : request.getString("nilai").trim();

            if (kspId == null || jenisItemId == null || matpelId == null || grupKategoriId == null || smt == null) {
                jsonObject.put("status", "91");
                jsonObject.put("description", "Parameter tidak lengkap. Diperlukan: kelas_siswa_punya_siswa_id, jenis_item_penilaian_id, matapelajaran_id, grup_kategori_item_id, smt");
                return jsonObject;
            }

            Session session = HibernateUtil.openSession();
            try {
                KelasSiswaPunyaSiswa ksp = (KelasSiswaPunyaSiswa) session.get(KelasSiswaPunyaSiswa.class, kspId);
                JenisItemPenilaianSiswa jenisItem = (JenisItemPenilaianSiswa) session
                        .get(JenisItemPenilaianSiswa.class, jenisItemId);
                Matapelajaran matapelajaran = (Matapelajaran) session.get(Matapelajaran.class, matpelId);
                GrupKategoriItemPenilaianSiswa grupKategori = (GrupKategoriItemPenilaianSiswa) session
                        .get(GrupKategoriItemPenilaianSiswa.class, grupKategoriId);

                if (ksp == null) {
                    jsonObject.put("status", "91");
                    jsonObject.put("description", "Data siswa di kelas tidak ditemukan");
                    return jsonObject;
                }
                if (jenisItem == null || matapelajaran == null || grupKategori == null) {
                    jsonObject.put("status", "91");
                    jsonObject.put("description", "Data jenis item, matapelajaran, atau kategori tidak ditemukan");
                    return jsonObject;
                }

                // Simpan nilai (verify=true jika nilai tidak kosong)
                boolean verify = !nilaiStr.isEmpty();
                ksp.populateDetailNilai(jenisItem, matapelajaran, grupKategori, nilaiStr, verify, smt);

                // Ambil semua item dalam kategori ini untuk menghitung total
                List<KategoriItemPenilaianSiswa> kategoriIds = ConstantValues.simpleList(
                        session.createCriteria(DetailGrupKategoriItemPenilaianSiswa.class)
                                .add(Restrictions.eq("grupKategoriItemPenilaianSiswa", grupKategori))
                                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                .setProjection(Projections.groupProperty("kategoriItemPenilaianSiswa.id")),
                        KategoriItemPenilaianSiswa.class, false);

                List<JenisItemPenilaianSiswa> allItems;
                if (kategoriIds == null || kategoriIds.isEmpty()) {
                    allItems = new ArrayList<JenisItemPenilaianSiswa>();
                } else {
                    allItems = ConstantValues.simpleList(
                            session.createCriteria(JenisItemPenilaianSiswa.class)
                                    .createAlias("kategoriItemPenilaianSiswa", "kat")
                                    .addOrder(Order.asc("kat.kode"))
                                    .addOrder(Order.asc("nomorUrut"))
                                    .add(Restrictions.in("kategoriItemPenilaianSiswa", kategoriIds))
                                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
                            JenisItemPenilaianSiswa.class);
                }

                // Ambil grup penilaian untuk formula total
                GrupPenilaian grupPenilaian = (GrupPenilaian) ConstantValues.simpleObject(
                        session.createCriteria(DetailGrupPenilaian.class)
                                .add(Restrictions.eq("grupKategoriItemPenilaianSiswa", grupKategori))
                                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                .setProjection(Projections.property("grupPenilaian.id"))
                                .setMaxResults(1),
                        GrupPenilaian.class, false);

                // Hitung ulang total kategori
                Double totalKategori = 0.0;
                try {
                    Date sekarang = WaktuUtil.getDate();
                    String formula = grupKategori.getFormula();
                    String target = GrupPenilaianUtil.ambilTarget(formula, sekarang);
                    totalKategori = ksp.retreiveTotalNilai(allItems, target, matapelajaran, grupPenilaian, grupKategori,
                            smt, null);
                } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/api/NilaiSiswaApi.java:385");
                }

                // Simpan total kategori
                ksp.populateDetailNilaiTotal(matapelajaran, grupKategori, totalKategori, verify, smt);

                // Dapatkan nilai huruf berdasarkan total
                Siswa siswa = ksp.getSiswa();
                NilaiHurufSekolah nilaiHuruf = null;
                if (grupPenilaian != null && siswa != null) {
                    nilaiHuruf = NilaiHurufSekolah.getNilaiHurufSekolah(totalKategori, siswa.getTahunMasuk(),
                            siswa.getSekolah(), siswa.getYayasan(),
                            ksp.getKelasSiswa().getTahunAjaran(),
                            smt % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL,
                            grupPenilaian.getJenisNilaiHuruf());
                }

                // Simpan ke database
                session.getTransaction().begin();
                session.update(ksp);
                session.getTransaction().commit();

                JSONObject result = new JSONObject();
                result.put("kelas_siswa_punya_siswa_id", kspId);
                result.put("jenis_item_id", jenisItemId);
                result.put("grup_kategori_item_id", grupKategoriId);
                result.put("smt", smt);
                result.put("nilai", nilaiStr);
                result.put("total_kategori", totalKategori);
                result.put("huruf", nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());

                jsonObject.put("status", "00");
                jsonObject.put("description", "Nilai berhasil disimpan");
                jsonObject.put("data", result);

            } finally {
                HibernateUtil.closeSessionQuietly(session);
            }

        } catch (Exception e) {
            String err = Common.tampilErrorJikaAdmin(e);
            try {
                jsonObject.put("status", "90");
                jsonObject.put("description", err);
            } catch (Exception ee) {
                ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/NilaiSiswaApi.java:430");
            }
        }
        return jsonObject;
    }
}
