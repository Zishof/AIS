package ais.common.newui.akademik;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
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
import org.json.JSONException;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.newui.NewUiCsrfUtil;
import ais.common.newui.NewUiRouteGuard;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Komentar;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;

/**
 * Ikut Perkuliahan Lainnya: mahasiswa mendaftar sebagai peserta tambahan di luar
 * KRS resmi.
 *
 * <h3>Apa arti layar ini, dan apa yang bukan</h3>
 * <p>Keikutsertaan lewat jalur ini <b>tidak memengaruhi nilai</b>; ia hanya
 * memberi akses ke aktivitas dan kegiatan perkuliahan. Catatan itu diambil dari
 * dokumentasi {@code IkutPerkuliahanHelper}, dan penting karena membedakannya
 * dari pengisian KRS: menyamakan keduanya akan membuat orang mengira layar ini
 * menambah beban studi.</p>
 *
 * <h3>Aturan yang disalin dari layar lama</h3>
 * <ul>
 *   <li><b>Kapasitas kelas.</b> Sebelum baris baru dibuat, jumlah peserta
 *       dihitung dengan {@code KrsUtilHelper.ambilJumlahDetailperkuliahan} lalu
 *       ditambah satu; bila melebihi {@code kapasitasKelas} — atau
 *       {@code Ruang.getDefaultKapasitas()} bila kelasnya tidak menyebutkan —
 *       perkuliahan itu <b>dilewati</b> dan dilaporkan, bukan menggagalkan
 *       seluruh permintaan.</li>
 *   <li><b>Sudah terdaftar berarti diperbarui, bukan digandakan.</b> Baris yang
 *       sudah ada untuk (perkuliahan, mahasiswa, semester, status semester
 *       pendek) dimuat dan dipakai ulang.</li>
 *   <li><b>Satu matakuliah sekali per permintaan</b>, sesuai penyaringan
 *       {@code matakuliahs} pada layar lama.</li>
 *   <li><b>Nilai awal:</b> {@code nilaiHuruf} kosong, {@code totalNilai} 0.0,
 *       {@code persetujuan} {@code BELUM_DISETUJUI}.</li>
 * </ul>
 *
 * <h3>Penghapusan</h3>
 * <p>Penjaganya disalin apa adanya, termasuk kejanggalannya: pesan layar lama
 * berbunyi "jika nilai tidak nol", tetapi syaratnya {@code totalNilai > 1.0}
 * dan hanya berlaku ketika konfigurasi
 * {@code batalkan_persetujuan_harus_memiliki_nilai_nol} menyala dan status
 * persetujuannya {@code DISETUJUI}. Mengetatkannya menjadi "lebih dari nol"
 * akan menolak penghapusan yang selama ini diperbolehkan.</p>
 *
 * <p>Komentar yang menunjuk baris itu dihapus lebih dulu, lalu barisnya sendiri
 * — keduanya lewat {@code Common.refreshDelete} supaya cache ikut disegarkan.</p>
 *
 * <h3>Milik pemilik sesi</h3>
 * <p>Mahasiswa diambil dari sesi, tidak pernah dari parameter. Aksi yang
 * mengubah data menuntut POST beserta token CSRF.</p>
 */
public final class NewUiIkutPerkuliahanController {

    private static final String MODULE = "root";

    private static final int MAKS_SEMESTER = 20;

    private NewUiIkutPerkuliahanController() { }

    public static void handle(HttpServletRequest request, HttpServletResponse response,
            String pageKey) throws Exception {
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        JSONObject json = new JSONObject();
        try {
            String action = teks(request.getParameter("action"), "meta");
            if (!aksiDikenal(action)) {
                response.setStatus(405);
                gagal(json, "ACTION_NOT_ALLOWED", "Aksi tidak dikenal pada layar ini.");
                tulis(response, json);
                return;
            }
            if (!NewUiRouteGuard.isActionAuthorized(request, MODULE, pageKey, action)) {
                response.setStatus(403);
                gagal(json, "ACTION_FORBIDDEN", "Hak akses tidak tersedia.");
                tulis(response, json);
                return;
            }
            if (mengubah(action)) {
                if (!"POST".equalsIgnoreCase(request.getMethod())) {
                    response.setStatus(405);
                    gagal(json, "METHOD_NOT_ALLOWED", "Gunakan HTTP POST untuk perubahan data.");
                    tulis(response, json);
                    return;
                }
                if (!NewUiCsrfUtil.isValid(request)) {
                    response.setStatus(403);
                    gagal(json, "CSRF_INVALID", "Token keamanan tidak valid. Muat ulang halaman.");
                    tulis(response, json);
                    return;
                }
            }
            Tbmuser user = Common.getCurrentUser(request);
            if (user == null) throw new SecurityException("Sesi tidak dikenal.");
            Mahasiswa mahasiswa = user.getMahasiswa();
            if (mahasiswa == null || mahasiswa.getId() == null) {
                throw new SecurityException("Layar ini hanya untuk akun mahasiswa.");
            }

            if ("meta".equals(action)) meta(json, request, mahasiswa);
            else if ("list".equals(action)) daftar(json, request, mahasiswa);
            else if ("lookup".equals(action)) pilihan(json, request);
            else if ("create".equals(action)) ikuti(json, request, user, mahasiswa);
            else hapus(json, request, mahasiswa);
            json.put("ok", true);
        } catch (SecurityException e) {
            response.setStatus(403);
            gagal(json, "FORBIDDEN", e.getMessage());
        } catch (IllegalArgumentException e) {
            response.setStatus(422);
            gagal(json, "VALIDATION_FAILED", e.getMessage());
        } catch (Exception e) {
            response.setStatus(500);
            gagal(json, "INTERNAL_ERROR", "Permintaan gagal diproses.");
            try { ais.common.ErrorAuditUtil.record(e, "NewUiIkutPerkuliahanController"); }
            catch (Exception diabaikan) { }
        }
        tulis(response, json);
    }

    /**
     * Aksi yang dilayani layar ini.
     *
     * <p>Namanya sengaja memakai kata kerja yang dikenal
     * {@code NewUiRouteGuard}: {@code create} dan {@code delete}, bukan
     * {@code ikuti} dan {@code hapus}. Penjaga itu <b>menolak kata kerja yang
     * tidak dikenalnya</b>, sehingga nama sendiri akan membuat layar ini tidak
     * berfungsi untuk siapa pun — dan pemetaannya pun tepat: mendaftar membuat
     * baris {@code Detailperkuliahan} (butuh izin Create) dan membatalkannya
     * menghapus baris itu (butuh izin Delete).</p>
     */
    static boolean aksiDikenal(String action) {
        return "meta".equals(action) || "list".equals(action) || "lookup".equals(action)
                || "create".equals(action) || "delete".equals(action);
    }

    /** Aksi yang mengubah data; menuntut POST dan token CSRF. */
    static boolean mengubah(String action) {
        return "create".equals(action) || "delete".equals(action);
    }

    // ------------------------------------------------------------------ meta

    private static void meta(JSONObject j, HttpServletRequest request, Mahasiswa mahasiswa)
            throws JSONException {
        j.put("judul", "Ikut Perkuliahan Lainnya");
        j.put("catatan", "Keikutsertaan lewat layar ini tidak memengaruhi nilai; "
                + "hanya memberi akses aktivitas dan kegiatan perkuliahan.");
        j.put("mahasiswa", new JSONObject()
                .put("id", mahasiswa.getId())
                .put("nama", mahasiswa.getNama() == null ? "" : mahasiswa.getNama())
                .put("nim", mahasiswa.getNim() == null ? "" : mahasiswa.getNim()));
        JSONArray pilihan = new JSONArray();
        for (int i = 1; i <= MAKS_SEMESTER; i++) pilihan.put(i);
        j.put("pilihanSemester", pilihan);
        int sekarang = NewUiKuesionerMahasiswaController.semesterSekarang(mahasiswa);
        j.put("semesterBawaan", sekarang);
        j.put("csrfHeader", NewUiCsrfUtil.HEADER);
        j.put("csrfToken", NewUiCsrfUtil.getToken(request.getSession(true)));
    }

    // ------------------------------------------------------------------ list

    private static void daftar(JSONObject j, HttpServletRequest request, Mahasiswa mahasiswa)
            throws JSONException {
        int semester = semesterDiminta(request, mahasiswa);
        Integer tahapan = angkaOpsional(request.getParameter("tahapan"));
        Integer semesterPendek = semesterPendek(request);

        List<Long> ids = Common.getIkutDetailperkuliahans(mahasiswa,
                Integer.valueOf(semester), tahapan, null, semesterPendek, Boolean.FALSE);

        JSONArray baris = new JSONArray();
        for (int i = 0; ids != null && i < ids.size(); i++) {
            JSONObject o = barisDetail(mahasiswa, ids.get(i));
            if (o != null) baris.put(o);
        }
        j.put("baris", baris);
        j.put("total", baris.length());
        j.put("semester", semester);
    }

    private static JSONObject barisDetail(Mahasiswa mahasiswa, Long id) throws JSONException {
        Detailperkuliahan d;
        try {
            d = (Detailperkuliahan) ConstantValues.ambil(
                    Detailperkuliahan.class.getName(), (Serializable) id);
        } catch (Exception e) {
            return null;
        }
        if (d == null) return null;
        Perkuliahan perkuliahan = d.getIkutiPerkuliahan();
        if (perkuliahan == null) return null;

        Matakuliah matakuliah = perkuliahan.getMatakuliah();
        Matakuliah asli;
        try {
            Matakuliah[] pasangan = Common.getMatakuliahApakahEkivalen(
                    matakuliah, mahasiswa == null ? null : mahasiswa.getNim(), false);
            matakuliah = pasangan[0];
            asli = pasangan[1];
        } catch (Exception e) {
            asli = matakuliah;
        }
        if (matakuliah == null) return null;
        boolean sama = asli == null || matakuliah.getId() == null
                || matakuliah.getId().equals(asli.getId());

        return new JSONObject()
                .put("id", d.getId())
                .put("kode", NewUiUjianMahasiswaController.labelEkivalen(
                        matakuliah.getKode(), asli == null ? null : asli.getKode(), sama))
                .put("nama", NewUiUjianMahasiswaController.labelEkivalen(
                        matakuliah.getNama(), asli == null ? null : asli.getNama(), sama))
                .put("sks", NewUiUjianMahasiswaController.labelEkivalen(
                        String.valueOf(matakuliah.getSks()),
                        asli == null ? null : String.valueOf(asli.getSks()), sama))
                .put("dosen", aman(ais.action.master.helper.PerkuliahanUIHelper
                        .generateTeksDosenPerkuliahan(perkuliahan)))
                .put("jadwal", aman(ais.action.master.helper.PerkuliahanUIHelper
                        .generateHariJamRuanganPerkuliahanUmumText(perkuliahan)))
                .put("persetujuan", d.getPersetujuan() == null ? "" : String.valueOf(d.getPersetujuan()))
                .put("bolehHapus", bolehHapus(d));
    }

    // ---------------------------------------------------------------- lookup

    /**
     * Perkuliahan yang dapat diikuti.
     *
     * <p>Penyaringnya disalin dari {@code onSearchDefault} pada layar pemilih:
     * hanya yang aktif (atau {@code aktif} null), status semester pendek yang
     * cocok, tahun ajaran yang diminta, dan bukan kelas paralel. Melewatkan
     * penyaring "bukan paralel" akan memunculkan kelas yang sama berkali-kali.</p>
     */
    private static void pilihan(JSONObject j, HttpServletRequest request) throws JSONException {
        String tahunAjaran = teks(request.getParameter("tahunAjaran"), "");
        if (tahunAjaran.length() == 0) {
            throw new IllegalArgumentException("Tahun ajaran wajib diisi.");
        }
        Integer semesterPendek = semesterPendek(request);
        Session session = HibernateUtil.currentSession();

        Criteria kriteria = session.createCriteria(Perkuliahan.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
                .add(semesterPendek == null
                        ? Restrictions.isNull("statusSemesterPendek")
                        : Restrictions.eq("statusSemesterPendek", semesterPendek))
                .add(Restrictions.eq("tahunAjaran", tahunAjaran))
                .add(Restrictions.or(Restrictions.eq("merupakan_paralel", Boolean.FALSE),
                        Restrictions.isNull("merupakan_paralel")));

        Integer semester = angkaOpsional(request.getParameter("semester"));
        if (semester != null) kriteria.add(Restrictions.eq("semester", semester));
        String program = teks(request.getParameter("program"), "");
        if (program.length() > 0) kriteria.add(Restrictions.eq("program", program));

        String kode = teks(request.getParameter("kode"), "");
        String nama = teks(request.getParameter("nama"), "");
        Criteria mk = kriteria.createCriteria("matakuliah", "mk").addOrder(Order.asc("nama"));
        mk.add(Restrictions.ilike("kode", kode, MatchMode.ANYWHERE));
        mk.add(Restrictions.ilike("nama", nama, MatchMode.ANYWHERE));
        mk.setMaxResults(Common.MAX_RESULT);

        List<?> hasil = mk.list();
        JSONArray baris = new JSONArray();
        for (int i = 0; i < hasil.size(); i++) {
            Perkuliahan p = (Perkuliahan) hasil.get(i);
            Matakuliah m = p.getMatakuliah();
            if (m == null) continue;
            baris.put(new JSONObject()
                    .put("id", p.getId())
                    .put("kode", m.getKode() == null ? "" : m.getKode())
                    .put("nama", m.getNama() == null ? "" : m.getNama())
                    .put("sks", m.getSks())
                    .put("dosen", aman(ais.action.master.helper.PerkuliahanUIHelper
                            .generateTeksDosenPerkuliahan(p)))
                    .put("jadwal", aman(ais.action.master.helper.PerkuliahanUIHelper
                            .generateHariJamRuanganPerkuliahanUmumText(p))));
        }
        j.put("baris", baris);
        j.put("total", baris.length());
        j.put("batas", Common.MAX_RESULT);
    }

    // ----------------------------------------------------------------- ikuti

    @SuppressWarnings("unchecked")
    private static void ikuti(JSONObject j, HttpServletRequest request, Tbmuser user,
            Mahasiswa mahasiswa) throws JSONException {
        int semester = semesterDiminta(request, mahasiswa);
        Integer semesterPendek = semesterPendek(request);
        List<Long> diminta = daftarId(request.getParameter("perkuliahan"));
        if (diminta.isEmpty()) {
            throw new IllegalArgumentException("Pilih setidaknya satu perkuliahan.");
        }

        Session session = HibernateUtil.currentSession();
        Set<Long> matakuliahs = new HashSet<Long>();
        JSONArray ditolak = new JSONArray();
        int berhasil = 0;

        for (int i = 0; i < diminta.size(); i++) {
            Perkuliahan perkuliahan;
            try {
                perkuliahan = (Perkuliahan) session.get(Perkuliahan.class, diminta.get(i));
            } catch (Exception e) {
                continue;
            }
            if (perkuliahan == null || perkuliahan.getMatakuliah() == null) continue;

            // Satu matakuliah sekali per permintaan, sama seperti layar lama.
            Long idMatakuliah = perkuliahan.getMatakuliah().getId();
            if (matakuliahs.contains(idMatakuliah)) continue;
            matakuliahs.add(idMatakuliah);

            Long id;
            try {
                id = (Long) session.createCriteria(Detailperkuliahan.class)
                        .add(Restrictions.isNotNull("ikutiPerkuliahan"))
                        .setProjection(Projections.property("id"))
                        .add(Restrictions.eq("ikutiPerkuliahan", perkuliahan))
                        .add(Restrictions.eq("mahasiswa", mahasiswa))
                        .add(Restrictions.eq("semester", Integer.valueOf(semester)))
                        .createCriteria("ikutiPerkuliahan", Criteria.LEFT_JOIN)
                        .add(semesterPendek == null
                                ? Restrictions.isNull("statusSemesterPendek")
                                : Restrictions.eq("statusSemesterPendek", semesterPendek))
                        .uniqueResult();
            } catch (Exception e) {
                continue;
            }

            Detailperkuliahan detail;
            if (id != null) {
                detail = (Detailperkuliahan) session.load(Detailperkuliahan.class, id);
            } else {
                int terisi = ais.action.master.helper.KrsUtilHelper
                        .ambilJumlahDetailperkuliahan(session, perkuliahan, false) + 1;
                int kapasitas = perkuliahan.getKapasitasKelas() == null
                        ? Ruang.getDefaultKapasitas() : perkuliahan.getKapasitasKelas();
                if (terisi > kapasitas) {
                    // Dilewati dan dilaporkan, bukan menggagalkan seluruh permintaan.
                    ditolak.put(new JSONObject()
                            .put("perkuliahan", perkuliahan.getId())
                            .put("alasan", "Kapasitas kelas sudah penuh. Maksimal " + kapasitas
                                    + ", sedangkan permintaan ini menjadikannya " + terisi + "."));
                    continue;
                }
                detail = new Detailperkuliahan(user, NewUiIkutPerkuliahanController.class);
            }
            detail.setNilaiHuruf("");
            detail.setTotalNilai(Double.valueOf(0.0));
            detail.setMahasiswa(mahasiswa);
            detail.setIkutiPerkuliahan(perkuliahan);
            detail.setSemester(Integer.valueOf(semester));
            detail.setPersetujuan(Detailperkuliahan.BELUM_DISETUJUI);
            session.saveOrUpdate(detail);
            berhasil++;
        }
        j.put("berhasil", berhasil);
        j.put("ditolak", ditolak);
    }

    // ----------------------------------------------------------------- hapus

    @SuppressWarnings("unchecked")
    private static void hapus(JSONObject j, HttpServletRequest request, Mahasiswa mahasiswa)
            throws JSONException {
        Long id = angkaPanjang(request.getParameter("id"));
        if (id == null) throw new IllegalArgumentException("Baris yang dihapus harus disebutkan.");

        Session session = HibernateUtil.currentSession();
        Detailperkuliahan detail = (Detailperkuliahan) session.get(Detailperkuliahan.class, id);
        if (detail == null) throw new IllegalArgumentException("Data tidak ditemukan.");

        // Milik orang lain tidak boleh disentuh, meskipun id-nya diketahui.
        if (detail.getMahasiswa() == null || detail.getMahasiswa().getId() == null
                || !detail.getMahasiswa().getId().equals(mahasiswa.getId())) {
            throw new SecurityException("Data ini bukan milik Anda.");
        }
        if (detail.getIkutiPerkuliahan() == null) {
            // Baris KRS resmi tidak dihapus lewat layar ini.
            throw new IllegalArgumentException("Baris ini bukan keikutsertaan perkuliahan tambahan.");
        }
        if (!bolehHapus(detail)) {
            throw new IllegalArgumentException(
                    "Jika nilai tidak nol, anda tidak bisa menghapus matakuliah ini");
        }

        List<Komentar> komentars = session.createCriteria(Komentar.class)
                .add(Restrictions.eq("detailperkuliahan", detail.getId())).list();
        for (int i = 0; i < komentars.size(); i++) {
            Common.refreshDelete(komentars.get(i));
        }
        Common.refreshDelete(detail);
        j.put("dihapus", id);
    }

    /**
     * Penjaga penghapusan, disalin apa adanya.
     *
     * <p>Hanya berlaku ketika konfigurasi
     * {@code batalkan_persetujuan_harus_memiliki_nilai_nol} menyala; syaratnya
     * {@code DISETUJUI} dan {@code totalNilai > 1.0} — bukan "lebih dari nol",
     * meskipun pesannya berbunyi begitu. Mengetatkannya akan menolak
     * penghapusan yang selama ini diperbolehkan.</p>
     */
    static boolean bolehHapus(Detailperkuliahan detail) {
        try {
            if (!Common.bolehKonfigurasi("batalkan_persetujuan_harus_memiliki_nilai_nol")) return true;
        } catch (Exception e) {
            return true;
        }
        if (detail.getPersetujuan() == null
                || !detail.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)) {
            return true;
        }
        Double nilai = detail.getTotalNilai();
        return nilai == null || nilai.doubleValue() <= 1.0;
    }

    // ---------------------------------------------------------------- utilitas

    private static int semesterDiminta(HttpServletRequest request, Mahasiswa mahasiswa) {
        int bawaan = NewUiKuesionerMahasiswaController.semesterSekarang(mahasiswa);
        int semester = angka(request.getParameter("semester"), bawaan);
        if (semester < 1 || semester > MAKS_SEMESTER) {
            throw new IllegalArgumentException("Semester harus antara 1 dan " + MAKS_SEMESTER + ".");
        }
        return semester;
    }

    /** {@code SEMESTER_PENDEK} bila diminta, {@code null} bila tidak — bukan nol. */
    private static Integer semesterPendek(HttpServletRequest request) {
        String v = request.getParameter("semesterPendek");
        boolean ya = "1".equals(v) || "true".equalsIgnoreCase(v) || "ya".equalsIgnoreCase(v);
        return ya ? Perkuliahan.SEMESTER_PENDEK : null;
    }

    private static List<Long> daftarId(String nilai) {
        List<Long> hasil = new ArrayList<Long>();
        if (nilai == null) return hasil;
        String[] bagian = nilai.split(",");
        for (int i = 0; i < bagian.length; i++) {
            Long id = angkaPanjang(bagian[i]);
            if (id != null) hasil.add(id);
        }
        return hasil;
    }

    private static Long angkaPanjang(String nilai) {
        try { return nilai == null ? null : Long.valueOf(nilai.trim()); }
        catch (Exception e) { return null; }
    }

    private static Integer angkaOpsional(String nilai) {
        try { return nilai == null || nilai.trim().length() == 0 ? null : Integer.valueOf(nilai.trim()); }
        catch (Exception e) { return null; }
    }

    private static int angka(String nilai, int bawaan) {
        try { return nilai == null || nilai.trim().length() == 0 ? bawaan : Integer.parseInt(nilai.trim()); }
        catch (Exception e) { return bawaan; }
    }

    private static String aman(String nilai) {
        return nilai == null ? "" : nilai;
    }

    private static String teks(String nilai, String bawaan) {
        return nilai == null || nilai.trim().length() == 0 ? bawaan : nilai.trim();
    }

    private static void gagal(JSONObject j, String kode, String pesan) throws JSONException {
        j.put("ok", false);
        j.put("code", kode);
        j.put("message", pesan == null ? "" : pesan);
    }

    private static void tulis(HttpServletResponse response, JSONObject j) throws java.io.IOException {
        response.getWriter().print(j.toString());
    }
}
