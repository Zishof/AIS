package ais.action.servlet.api;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ParameterTambahan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.CatatanSiswa;
import ais.database.model.sekolah.JenisCatatanSiswa;
import ais.database.model.sekolah.KelompokParameterTambahanCatatanSiswa;
import ais.database.model.sekolah.ParameterTambahanCatatanSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.WaktuUtil;

/**
 * <h1>CatatanApi — Layanan REST/JSON modul "Catatan" untuk aplikasi mobile</h1>
 *
 * <p>Kelas ini adalah lapisan layanan (service layer) yang menjembatani aplikasi mobile dengan
 * modul-modul "Catatan" pada sistem AIS (Enterprise Education — eCampus/eSchool/ePesantren).
 * Modul Catatan adalah sekumpulan fitur pencatatan kejadian/peristiwa yang menempel pada sebuah
 * subjek (siswa, mahasiswa, guru, pegawai, kelas, atau administrasi). Di sisi web, modul ini
 * diwakili oleh kelas-kelas berpola nama <code>*Catatan*Action</code> (mis.
 * {@code CatatanSiswaAction}, {@code CatatanGuruAction}, {@code CatatanMahasiswaAction},
 * {@code CatatanPegawaiAction}, {@code CatatanKelasSiswaAction}, {@code CatatanAdministrasiAction}).
 * Implementasi referensi pada kelas ini difokuskan pada {@link CatatanSiswa} sebagai bentuk kanonik
 * karena ia memuat seluruh karakteristik yang membuat modul Catatan menarik untuk perangkat mobile:
 * relasi ke subjek (siswa), jenis catatan ({@link JenisCatatanSiswa}), parameter tambahan dinamis
 * ({@link ParameterTambahan}), serta lampiran berkas ({@link LampiranLain}). Saudara-saudara
 * catatan lain mengikuti pola model yang identik sehingga ekstensi ke tipe lain bersifat mekanis
 * (lihat dokumentasi akhir paket).</p>
 *
 * <h2>Konsep penting: "parameter tambahan" (custom field dinamis)</h2>
 * <p>Setiap {@link JenisCatatanSiswa} dapat memiliki sejumlah <i>parameter tambahan</i> — yaitu
 * field isian yang ditentukan oleh administrator sekolah, bukan kolom tetap pada tabel. Field-field
 * ini dikelompokkan dalam {@link KelompokParameterTambahanCatatanSiswa} dan masing-masing
 * menunjuk ke definisi {@link ParameterTambahan} (yang menyimpan label, tipe input, daftar pilihan,
 * wajib/tidak, dan apakah memerlukan lampiran). Nilai yang diisikan pengguna disimpan pada kolom
 * teks {@code parameterTambahan} (format internal berbasis pasangan kunci-nilai) dengan kunci unik
 * berbentuk <code>"{kelompokId}-&gt;{parameterId}"</code>. Karena field bersifat dinamis, aplikasi
 * mobile WAJIB lebih dulu memanggil {@link #parameter(HttpServletRequest, JSONObject)} untuk
 * mengetahui struktur form yang harus dirender, lalu mengirim balik nilainya saat menyimpan. Inilah
 * sebabnya "informasi parameter tambahan harus bisa ditampilkan di mobile" — endpoint
 * <code>parameter</code> mengembalikan metadata yang lengkap untuk membangun form secara dinamis.</p>
 *
 * <h2>Konsep penting: lampiran</h2>
 * <p>Sebagian parameter tambahan bertipe berkas (foto kejadian, surat, bukti, dsb.). Lampiran
 * disimpan terpisah melalui {@link LampiranLain} dan ditautkan ke catatan dengan kunci yang sama
 * (<code>"{kelompokId}-&gt;{parameterId}"</code>). Pada respons {@link #detail} dan {@link #daftar},
 * setiap lampiran disertai URL unduh siap pakai yang menunjuk ke servlet
 * <code>/AmbilLampiran</code> sehingga aplikasi mobile cukup membuka URL tersebut untuk menampilkan
 * atau mengunduh berkas.</p>
 *
 * <h2>Format respons umum</h2>
 * <p>Seluruh method mengembalikan {@link JSONObject} dengan field <code>status</code> dan
 * <code>description</code> sesuai konvensi {@link ApiHelperSupport}: <code>"00"</code> = sukses,
 * <code>"97"</code> = token/parameter tidak valid, <code>"99"</code> = kosong/tidak ada data,
 * <code>"500"</code> = kesalahan server. Data muatan ditempatkan pada field <code>data</code>
 * (objek) atau <code>list</code> (array), tergantung method.</p>
 *
 * <h2>Autentikasi</h2>
 * <p>Identitas pemanggil diperoleh dari token via {@link ApiUtil#currentUser(JSONObject,
 * HttpServletRequest)}. Untuk peran orang tua/siswa, subjek default adalah
 * {@code Tbmuser.getSiswa()}; sehingga bila aplikasi mobile tidak mengirim parameter
 * <code>siswa</code>, layanan akan memakai siswa milik akun yang sedang login. Untuk peran guru/admin,
 * <code>siswa</code> dapat dikirim eksplisit untuk menulis catatan bagi siswa yang dibinanya.</p>
 *
 * <h2>Kompatibilitas</h2>
 * <p>Mengikuti gaya paket {@code ais.action.servlet.api}: tanpa lambda, tanpa try-with-resources
 * (kompatibel Java 1.7), gagal-aman (selalu mengembalikan JSON, tidak melempar ke servlet), dan
 * memakai {@link HibernateUtil#getSessionFactory()}.openSession() untuk query baca serta
 * {@link Common#refreshSaveOrUpdate(Session, Object)} untuk tulis.</p>
 */
public final class CatatanApi {

    private CatatanApi() {
    }

    private static final String CLAZZ_CATATAN_SISWA = CatatanSiswa.class.getName();

    // ════════════════════════════════════════════════════════════════════════
    // 1) DAFTAR JENIS CATATAN  — action: "catatan_siswa_jenis"
    // ════════════════════════════════════════════════════════════════════════

    /**
     * <h2>{@code jenisSiswa} — Daftar Jenis Catatan Siswa</h2>
     *
     * <p>Mengembalikan seluruh {@link JenisCatatanSiswa} yang aktif untuk dipakai sebagai pilihan
     * (dropdown) ketika aplikasi mobile hendak membuat catatan baru atau memfilter daftar catatan.
     * "Jenis catatan" adalah kategori catatan yang ditetapkan administrator sekolah, misalnya
     * "Pelanggaran", "Prestasi", "Catatan Kesehatan", "Catatan Konseling", dan sebagainya. Setiap
     * jenis dapat memiliki kumpulan parameter tambahan yang berbeda (lihat
     * {@link #parameter(HttpServletRequest, JSONObject)}), sehingga pemilihan jenis menentukan
     * bentuk form yang akan dirender pada perangkat.</p>
     *
     * <h3>Permintaan (request)</h3>
     * <ul>
     *   <li><b>token</b> (wajib) — token sesi hasil login mobile.</li>
     *   <li><b>q</b> (opsional) — kata kunci pencarian nama jenis (case-insensitive, sebagian).</li>
     * </ul>
     *
     * <h3>Respons</h3>
     * <p>Field <code>list</code> berisi array objek dengan atribut: <code>id</code>,
     * <code>nama</code>, <code>keterangan</code>. Bila tidak ada data, status bernilai
     * <code>"99"</code> namun <code>list</code> tetap dikirim sebagai array kosong agar aplikasi
     * mobile dapat memprosesnya tanpa pengecekan null tambahan.</p>
     *
     * <h3>Catatan implementasi</h3>
     * <p>Query memakai {@link HibernateUtil#getSessionFactory()}.openSession() dengan kriteria
     * {@code aktif} bernilai benar atau null (agar data lama yang belum diberi flag tetap tampil),
     * diurutkan menaik berdasarkan nama. Hasil dipetakan menjadi JSON ringan (hanya field yang
     * dibutuhkan mobile) untuk menghemat ukuran payload dan menghindari kebocoran data sensitif.
     * Method ini murni baca dan tidak melakukan perubahan apa pun pada basis data; aman dipanggil
     * berkali-kali.</p>
     *
     * @param req  objek {@link HttpServletRequest} (dipakai resolusi token dari sesi bila ada).
     * @param json payload JSON permintaan dari aplikasi mobile.
     * @return {@link JSONObject} berisi status + array <code>list</code> jenis catatan siswa.
     */
    @SuppressWarnings("unchecked")
    public static JSONObject jenisSiswa(HttpServletRequest req, JSONObject json) {
        JSONObject hasil = new JSONObject();
        Session session = null;
        try {
            Tbmuser tbmuser = ApiUtil.currentUser(json, req);
            if (tbmuser == null || tbmuser.getUserId() == null) {
                return ApiHelperSupport.status("97", "Token tidak sesuai");
            }
            session = HibernateUtil.getSessionFactory().openSession();
            Criteria criteria = session.createCriteria(JenisCatatanSiswa.class)
                    .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                    .addOrder(Order.asc("nama"));
            String q = ApiHelperSupport.optString(json, "q");
            if (ApiHelperSupport.hasText(q)) {
                criteria.add(Restrictions.ilike("nama", q, org.hibernate.criterion.MatchMode.ANYWHERE));
            }
            List<JenisCatatanSiswa> jenisList = ConstantValues.simpleList(criteria, JenisCatatanSiswa.class);
            JSONArray arr = new JSONArray();
            if (jenisList != null) {
                for (JenisCatatanSiswa j : jenisList) {
                    if (j == null) {
                        continue;
                    }
                    JSONObject o = new JSONObject();
                    o.put("id", j.getId());
                    o.put("nama", ApiHelperSupport.safeString(j.getNama()));
                    o.put("keterangan", ApiHelperSupport.safeString(j.getKeterangan()));
                    arr.put(o);
                }
            }
            hasil.put("list", arr);
            ApiHelperSupport.putStatus(hasil, arr.length() > 0 ? "00" : "99",
                    arr.length() > 0 ? "OK" : "Belum ada jenis catatan siswa");
            return hasil;
        } catch (Exception e) {
            return ApiHelperSupport.errorResponse("Gagal mengambil jenis catatan siswa");
        } finally {
            if (session != null) {
                try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/CatatanApi.java:174");}
                try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/CatatanApi.java:175");}
                try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/CatatanApi.java:176");}
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 2) PARAMETER TAMBAHAN (definisi form dinamis)  — action: "catatan_siswa_parameter"
    // ════════════════════════════════════════════════════════════════════════

    /**
     * <h2>{@code parameter} — Definisi Parameter Tambahan untuk satu Jenis Catatan</h2>
     *
     * <p>Mengembalikan struktur lengkap parameter tambahan (custom field dinamis) yang dimiliki
     * sebuah {@link JenisCatatanSiswa}. Inilah endpoint kunci yang memenuhi kebutuhan "informasi
     * parameter tambahan harus bisa ditampilkan di mobile": aplikasi mobile memanggilnya setelah
     * pengguna memilih jenis catatan, lalu MERENDER form secara dinamis berdasarkan metadata yang
     * dikembalikan. Tanpa endpoint ini, aplikasi mobile tidak akan tahu field apa saja yang harus
     * ditampilkan, tipe inputnya, pilihan yang tersedia, mana yang wajib, dan mana yang memerlukan
     * lampiran.</p>
     *
     * <h3>Permintaan</h3>
     * <ul>
     *   <li><b>token</b> (wajib).</li>
     *   <li><b>jenis</b> (wajib) — id {@link JenisCatatanSiswa} yang dipilih pengguna.</li>
     * </ul>
     *
     * <h3>Respons</h3>
     * <p>Field <code>kelompok</code> berisi array kelompok parameter. Tiap kelompok memuat
     * <code>id</code>, <code>nama</code>, dan array <code>parameter</code>. Tiap parameter memuat:
     * <code>key</code> (kunci unik <code>"{kelompokId}-&gt;{parameterId}"</code> yang HARUS dikirim
     * balik saat menyimpan), <code>label</code> (judul field), <code>keterangan</code> (teks bantu),
     * <code>tipe</code> (tipe input: teks/angka/tanggal/pilihan/dll. dari
     * {@code getTipeDataInputan()}), <code>pilihan</code> (opsi untuk tipe pilihan, dari
     * {@code getNilaiDataInputan()}), <code>nilaiDefault</code>, <code>wajib</code> (boolean), dan
     * <code>lampiranWajib</code> (boolean — apakah parameter ini mewajibkan unggah berkas).</p>
     *
     * <h3>Cara kerja</h3>
     * <p>Method menelusuri {@code jenis.getKelompokParameterTambahanCatatanSiswas()}; untuk tiap
     * kelompok (yang aktif), ia menjalankan kriteria pada {@link ParameterTambahanCatatanSiswa}
     * yang menautkan kelompok dengan {@link ParameterTambahan} aktif, lalu memetakan tiap
     * {@link ParameterTambahan} menjadi metadata JSON. Logika ini SENGAJA disamakan dengan
     * {@code CatatanSiswaAction} agar bentuk dan kunci field benar-benar konsisten antara tampilan
     * web dan mobile — sehingga catatan yang dibuat di mobile dapat dibuka/diedit di web dan
     * sebaliknya tanpa perbedaan interpretasi kunci.</p>
     *
     * <h3>Validasi &amp; kesalahan</h3>
     * <p>Bila token tidak valid → status <code>"97"</code>. Bila <code>jenis</code> tidak dikirim
     * atau tidak ditemukan → status <code>"97"</code> dengan deskripsi yang menjelaskan. Bila jenis
     * tidak memiliki parameter tambahan sama sekali → status <code>"00"</code> dengan
     * <code>kelompok</code> berupa array kosong (artinya form hanya berisi field standar: nama,
     * keterangan, tanggal). Seluruh proses murni baca dan tidak mengubah data.</p>
     *
     * @param req  {@link HttpServletRequest}.
     * @param json payload JSON; wajib memuat <code>jenis</code>.
     * @return {@link JSONObject} berisi status + array <code>kelompok</code> beserta parameter.
     */
    @SuppressWarnings("unchecked")
    public static JSONObject parameter(HttpServletRequest req, JSONObject json) {
        JSONObject hasil = new JSONObject();
        Session session = null;
        try {
            Tbmuser tbmuser = ApiUtil.currentUser(json, req);
            if (tbmuser == null || tbmuser.getUserId() == null) {
                return ApiHelperSupport.status("97", "Token tidak sesuai");
            }
            if (ApiHelperSupport.isNullOrEmptyJsonValue(json, "jenis")) {
                return ApiHelperSupport.status("97", "Jenis catatan siswa harus dipilih");
            }
            JenisCatatanSiswa jenis = (JenisCatatanSiswa) ConstantValues.ambil(JenisCatatanSiswa.class.getName(),
                    Long.parseLong(ApiHelperSupport.optString(json, "jenis")));
            if (jenis == null) {
                return ApiHelperSupport.status("97", "Jenis catatan siswa tidak ditemukan");
            }
            session = HibernateUtil.getSessionFactory().openSession();
            JSONArray kelompokArr = new JSONArray();
            if (jenis.getKelompokParameterTambahanCatatanSiswas() != null) {
                for (KelompokParameterTambahanCatatanSiswa kelompok : jenis
                        .getKelompokParameterTambahanCatatanSiswas()) {
                    if (kelompok == null || (kelompok.getAktif() != null && !kelompok.getAktif())) {
                        continue;
                    }
                    List<ParameterTambahan> params = ConstantValues.simpleList(
                            session.createCriteria(ParameterTambahanCatatanSiswa.class)
                                    .add(Restrictions.eq("kelompokParameterTambahanCatatanSiswa", kelompok))
                                    .createAlias("parameterTambahan", "parameterTambahan")
                                    .add(Restrictions.eq("parameterTambahan.aktif", true))
                                    .setProjection(Projections.groupProperty("parameterTambahan.id")),
                            ParameterTambahan.class, false);
                    JSONArray paramArr = new JSONArray();
                    if (params != null) {
                        java.util.Collections.sort(params);
                        for (ParameterTambahan p : params) {
                            if (p == null) {
                                continue;
                            }
                            JSONObject po = new JSONObject();
                            po.put("key", kelompok.getId() + "->" + p.getId());
                            po.put("parameterId", p.getId());
                            po.put("label", ApiHelperSupport.safeString(p.getLabelInputan()));
                            po.put("keterangan", ApiHelperSupport.safeString(p.getLabelInputanKeterangan()));
                            po.put("tipe", ApiHelperSupport.safeString(p.getTipeDataInputan()));
                            po.put("pilihan", ApiHelperSupport.safeString(p.getNilaiDataInputan()));
                            po.put("nilaiDefault", ApiHelperSupport.safeString(p.getNilaiDefault()));
                            po.put("wajib", Boolean.TRUE.equals(p.getWajibDiisi()));
                            po.put("lampiranWajib", Boolean.TRUE.equals(p.getLampiranWajibDiisi()));
                            paramArr.put(po);
                        }
                    }
                    JSONObject ko = new JSONObject();
                    ko.put("id", kelompok.getId());
                    ko.put("nama", ApiHelperSupport.safeString(getKelompokNama(kelompok)));
                    ko.put("parameter", paramArr);
                    kelompokArr.put(ko);
                }
            }
            hasil.put("kelompok", kelompokArr);
            ApiHelperSupport.putSuccess(hasil, "OK");
            return hasil;
        } catch (Exception e) {
            return ApiHelperSupport.errorResponse("Gagal mengambil parameter tambahan catatan siswa");
        } finally {
            if (session != null) {
                try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/CatatanApi.java:298");}
                try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/CatatanApi.java:299");}
                try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/CatatanApi.java:300");}
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 3) DAFTAR CATATAN  — action: "catatan_siswa_daftar"
    // ════════════════════════════════════════════════════════════════════════

    /**
     * <h2>{@code daftar} — Daftar Catatan Siswa (operasi READ pada CRUD)</h2>
     *
     * <p>Mengembalikan daftar catatan ({@link CatatanSiswa}) yang dapat difilter berdasarkan siswa,
     * rentang tanggal, dan jenis catatan. Method ini adalah komponen "R" (Read/List) dari CRUD dan
     * dirancang ramah-mobile: payload ringan, terurut dari terbaru, serta menyertakan ringkasan
     * parameter tambahan dalam bentuk teks siap tampil sehingga aplikasi mobile dapat menampilkan
     * kartu/daftar tanpa harus memanggil endpoint detail satu per satu.</p>
     *
     * <h3>Permintaan</h3>
     * <ul>
     *   <li><b>token</b> (wajib).</li>
     *   <li><b>siswa</b> (opsional) — id siswa. Bila kosong, dipakai {@code Tbmuser.getSiswa()}
     *       (akun orang tua/siswa). Guru/admin dapat mengirim id siswa binaannya.</li>
     *   <li><b>jenis</b> (opsional) — id jenis catatan untuk menyaring.</li>
     *   <li><b>mulai</b>, <b>sampai</b> (opsional, format <code>yyyy-MM-dd</code>) — rentang tanggal
     *       <code>waktu</code> catatan. Default: 6 bulan terakhir hingga hari ini.</li>
     * </ul>
     *
     * <h3>Respons</h3>
     * <p>Field <code>list</code> berisi array catatan; tiap elemen memuat <code>id</code>,
     * <code>nama</code> (judul), <code>keterangan</code> (isi), <code>waktu</code> (tanggal kejadian,
     * format <code>yyyy-MM-dd</code>), <code>jenis</code> (objek {id, nama}), <code>oleh</code>
     * (pembuat), <code>parameterTambahanInds</code> (ringkasan parameter siap-tampil), dan
     * <code>jumlahLampiran</code> (estimasi banyak lampiran terkait). Untuk rincian penuh termasuk
     * URL lampiran, panggil {@link #detail(HttpServletRequest, JSONObject)}.</p>
     *
     * <h3>Keamanan data</h3>
     * <p>Bila pemanggil berperan orang tua/siswa dan tidak mengirim <code>siswa</code>, layanan
     * mengunci hasil ke siswa milik akun — mencegah akun melihat catatan siswa lain. Untuk peran
     * guru/admin, pemfilteran kewenangan lanjutan (mis. hanya siswa di kelas yang diajar) dapat
     * ditambahkan pada lapisan ini sesuai kebijakan; secara default, id siswa yang dikirim dihormati.
     * </p>
     *
     * @param req  {@link HttpServletRequest}.
     * @param json payload JSON permintaan.
     * @return {@link JSONObject} berisi status + array <code>list</code> catatan.
     */
    @SuppressWarnings("unchecked")
    public static JSONObject daftar(HttpServletRequest req, JSONObject json) {
        JSONObject hasil = new JSONObject();
        Session session = null;
        try {
            Tbmuser tbmuser = ApiUtil.currentUser(json, req);
            if (tbmuser == null || tbmuser.getUserId() == null) {
                return ApiHelperSupport.status("97", "Token tidak sesuai");
            }
            Siswa siswa = resolveSiswa(json, tbmuser);
            session = HibernateUtil.getSessionFactory().openSession();
            Criteria criteria = session.createCriteria(CatatanSiswa.class).addOrder(Order.desc("waktu"));
            if (siswa != null) {
                criteria.add(Restrictions.eq("siswa", siswa));
            } else if (tbmuser.getSiswa() != null) {
                criteria.add(Restrictions.eq("siswa", tbmuser.getSiswa()));
            }
            if (!ApiHelperSupport.isNullOrEmptyJsonValue(json, "jenis")) {
                JenisCatatanSiswa j = (JenisCatatanSiswa) ConstantValues.ambil(JenisCatatanSiswa.class.getName(),
                        Long.parseLong(ApiHelperSupport.optString(json, "jenis")));
                if (j != null) {
                    criteria.add(Restrictions.eq("jenisCatatanSiswa", j));
                }
            }
            Date[] rentang = rentangTanggal(json);
            criteria.add(Restrictions.ge("waktu", rentang[0]));
            criteria.add(Restrictions.le("waktu", rentang[1]));

            List<CatatanSiswa> list = ConstantValues.simpleList(criteria, CatatanSiswa.class);
            JSONArray arr = new JSONArray();
            if (list != null) {
                for (CatatanSiswa c : list) {
                    if (c == null) {
                        continue;
                    }
                    arr.put(ringkasCatatan(c));
                }
            }
            hasil.put("list", arr);
            ApiHelperSupport.putStatus(hasil, arr.length() > 0 ? "00" : "99",
                    arr.length() > 0 ? "OK" : "Belum ada catatan");
            return hasil;
        } catch (Exception e) {
            return ApiHelperSupport.errorResponse("Gagal mengambil daftar catatan siswa");
        } finally {
            if (session != null) {
                try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/CatatanApi.java:393");}
                try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/CatatanApi.java:394");}
                try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/CatatanApi.java:395");}
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 4) DETAIL CATATAN  — action: "catatan_siswa_detail"
    // ════════════════════════════════════════════════════════════════════════

    /**
     * <h2>{@code detail} — Rincian satu Catatan Siswa beserta parameter &amp; lampiran</h2>
     *
     * <p>Mengembalikan satu {@link CatatanSiswa} secara lengkap. Selain field standar, respons
     * memuat dua hal penting bagi mobile: (1) nilai parameter tambahan mentah (<code>parameterTambahan</code>)
     * dan ringkasan siap-tampil (<code>parameterTambahanInds</code>), serta (2) daftar lampiran
     * lengkap dengan URL unduh. Dengan ini aplikasi mobile dapat menampilkan halaman detail catatan
     * apa adanya — termasuk membuka foto/berkas bukti — tanpa logika tambahan.</p>
     *
     * <h3>Permintaan</h3>
     * <ul>
     *   <li><b>token</b> (wajib).</li>
     *   <li><b>id</b> (wajib) — id {@link CatatanSiswa}.</li>
     * </ul>
     *
     * <h3>Respons</h3>
     * <p>Field <code>data</code> memuat: <code>id</code>, <code>nama</code>, <code>keterangan</code>,
     * <code>waktu</code>, <code>tahunAjaran</code>, <code>semester</code>, <code>oleh</code>,
     * objek <code>jenis</code> {id, nama}, objek <code>siswa</code> {id, nama, nis}, string mentah
     * <code>parameterTambahan</code>, ringkasan <code>parameterTambahanInds</code>, dan array
     * <code>lampiran</code>. Tiap lampiran memuat <code>key</code> (kunci parameter),
     * <code>nama</code> (nama berkas), dan <code>url</code> (tautan unduh ke
     * <code>/AmbilLampiran</code>).</p>
     *
     * <h3>URL lampiran</h3>
     * <p>URL dibentuk dengan pola
     * <code>/AmbilLampiran?download=1&amp;ref={idCatatan}&amp;clazz={namaKelasCatatan}&amp;jenis={key}</code>.
     * Servlet {@code AmbilLampiran} akan menstream berkas yang sesuai. URL ini stateless terhadap
     * token sehingga sebaiknya dibuka pada konteks sesi yang sama dengan aplikasi (atau dilengkapi
     * mekanisme otorisasi unduh sesuai kebijakan keamanan).</p>
     *
     * @param req  {@link HttpServletRequest}.
     * @param json payload JSON; wajib memuat <code>id</code>.
     * @return {@link JSONObject} berisi status + objek <code>data</code>.
     */
    public static JSONObject detail(HttpServletRequest req, JSONObject json) {
        JSONObject hasil = new JSONObject();
        try {
            Tbmuser tbmuser = ApiUtil.currentUser(json, req);
            if (tbmuser == null || tbmuser.getUserId() == null) {
                return ApiHelperSupport.status("97", "Token tidak sesuai");
            }
            if (ApiHelperSupport.isNullOrEmptyJsonValue(json, "id")) {
                return ApiHelperSupport.status("97", "Id catatan harus dikirim");
            }
            CatatanSiswa c = (CatatanSiswa) ConstantValues.ambil(CLAZZ_CATATAN_SISWA,
                    Long.parseLong(ApiHelperSupport.optString(json, "id")));
            if (c == null) {
                return ApiHelperSupport.status("99", "Catatan tidak ditemukan");
            }
            JSONObject data = ringkasCatatan(c);
            data.put("tahunAjaran", ApiHelperSupport.safeString(c.getTahunAjaran()));
            data.put("semester", c.getSemester() == null ? "" : c.getSemester());
            data.put("parameterTambahan", ApiHelperSupport.safeString(c.getParameterTambahan()));
            JSONObject siswaObj = new JSONObject();
            if (c.getSiswa() != null) {
                siswaObj.put("id", c.getSiswa().getId());
                siswaObj.put("nama", ApiHelperSupport.safeString(c.getSiswa().getNama()));
                siswaObj.put("nis", ApiHelperSupport.safeString(c.getSiswa().getNis()));
            }
            data.put("siswa", siswaObj);
            data.put("lampiran", lampiranCatatan(c));
            hasil.put("data", data);
            ApiHelperSupport.putSuccess(hasil, "OK");
            return hasil;
        } catch (Exception e) {
            return ApiHelperSupport.errorResponse("Gagal mengambil detail catatan siswa");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 5) SIMPAN CATATAN (Create + Update)  — action: "catatan_siswa_simpan"
    // ════════════════════════════════════════════════════════════════════════

    /**
     * <h2>{@code simpan} — Buat atau Perbarui Catatan Siswa (operasi Create/Update pada CRUD)</h2>
     *
     * <p>Menyimpan sebuah catatan. Bila <code>id</code> dikirim dan ditemukan, catatan diperbarui;
     * bila tidak, catatan baru dibuat. Method ini menggabungkan "C" dan "U" pada CRUD karena pola
     * upsert lebih ringkas untuk klien mobile dan menghindari duplikasi logika validasi. Nilai
     * parameter tambahan dikirim sebagai string oleh aplikasi mobile (dibangun mengikuti definisi
     * dari {@link #parameter(HttpServletRequest, JSONObject)} dengan kunci
     * <code>"{kelompokId}-&gt;{parameterId}"</code>), lalu disimpan apa adanya pada kolom
     * {@code parameterTambahan}; ringkasan tampilan dapat dikirim pada
     * <code>parameterTambahanInds</code> atau dibiarkan kosong.</p>
     *
     * <h3>Permintaan</h3>
     * <ul>
     *   <li><b>token</b> (wajib).</li>
     *   <li><b>id</b> (opsional) — bila ada → update; bila tidak → create.</li>
     *   <li><b>siswa</b> (wajib saat create kecuali akun adalah siswa) — id siswa subjek.</li>
     *   <li><b>jenis</b> (wajib) — id {@link JenisCatatanSiswa}.</li>
     *   <li><b>nama</b> (wajib) — judul catatan.</li>
     *   <li><b>keterangan</b> (opsional) — isi/uraian catatan.</li>
     *   <li><b>waktu</b> (opsional, <code>yyyy-MM-dd</code>) — tanggal kejadian; default hari ini.</li>
     *   <li><b>tahunAjaran</b>, <b>semester</b> (opsional).</li>
     *   <li><b>parameterTambahan</b> (opsional) — string nilai parameter tambahan.</li>
     *   <li><b>parameterTambahanInds</b> (opsional) — ringkasan tampilan parameter.</li>
     * </ul>
     *
     * <h3>Respons</h3>
     * <p>Pada keberhasilan, status <code>"00"</code> dan field <code>id</code> berisi id catatan yang
     * tersimpan (berguna bagi mobile untuk langsung membuka detail atau mengunggah lampiran
     * berikutnya). Pada kegagalan validasi (token, jenis/siswa/nama wajib), status <code>"97"</code>
     * dengan deskripsi.</p>
     *
     * <h3>Atribut audit &amp; konteks</h3>
     * <p>Field <code>oleh</code>/<code>olehId</code> diisi otomatis dari identitas token saat create.
     * Subjek siswa diambil dari parameter <code>siswa</code>, atau dari {@code Tbmuser.getSiswa()}
     * bila akun adalah siswa dan tidak mengirim id. Penyimpanan memakai
     * {@link Common#refreshSaveOrUpdate(Session, Object)} yang mengelola transaksi sehingga aman
     * terhadap kondisi gagal sebagian.</p>
     *
     * <h3>Catatan tentang lampiran</h3>
     * <p>Unggah berkas lampiran TIDAK dilakukan melalui method JSON ini (lampiran memerlukan
     * transfer biner/multipart). Pola yang dianjurkan: simpan catatan lebih dulu untuk memperoleh
     * <code>id</code>, kemudian unggah berkas ke endpoint berkas khusus dengan menyertakan
     * <code>ref={id}</code> dan <code>jenis={key parameter}</code>. Endpoint {@link #detail} dan
     * {@link #daftar} kemudian akan menampilkan lampiran tersebut.</p>
     *
     * @param req  {@link HttpServletRequest}.
     * @param json payload JSON permintaan.
     * @return {@link JSONObject} status + <code>id</code> catatan tersimpan.
     */
    public static JSONObject simpan(HttpServletRequest req, JSONObject json) {
        JSONObject hasil = new JSONObject();
        Session session = null;
        try {
            Tbmuser tbmuser = ApiUtil.currentUser(json, req);
            if (tbmuser == null || tbmuser.getUserId() == null) {
                return ApiHelperSupport.status("97", "Token tidak sesuai");
            }
            CatatanSiswa c;
            boolean baru;
            if (!ApiHelperSupport.isNullOrEmptyJsonValue(json, "id")) {
                c = (CatatanSiswa) ConstantValues.ambil(CLAZZ_CATATAN_SISWA,
                        Long.parseLong(ApiHelperSupport.optString(json, "id")));
                if (c == null) {
                    return ApiHelperSupport.status("99", "Catatan tidak ditemukan");
                }
                baru = false;
            } else {
                c = new CatatanSiswa();
                baru = true;
            }

            if (ApiHelperSupport.isNullOrEmptyJsonValue(json, "jenis")) {
                return ApiHelperSupport.status("97", "Jenis catatan harus dipilih");
            }
            JenisCatatanSiswa jenis = (JenisCatatanSiswa) ConstantValues.ambil(JenisCatatanSiswa.class.getName(),
                    Long.parseLong(ApiHelperSupport.optString(json, "jenis")));
            if (jenis == null) {
                return ApiHelperSupport.status("97", "Jenis catatan tidak ditemukan");
            }
            Siswa siswa = resolveSiswa(json, tbmuser);
            if (siswa == null) {
                siswa = tbmuser.getSiswa();
            }
            if (baru && siswa == null) {
                return ApiHelperSupport.status("97", "Siswa harus dipilih");
            }
            String nama = ApiHelperSupport.optString(json, "nama");
            if (!ApiHelperSupport.hasText(nama)) {
                return ApiHelperSupport.status("97", "Nama/judul catatan harus diisi");
            }

            c.setJenisCatatanSiswa(jenis);
            if (siswa != null) {
                c.setSiswa(siswa);
            }
            c.setNama(nama.trim());
            c.setKeterangan(ApiHelperSupport.optString(json, "keterangan"));
            c.setWaktu(json.isNull("waktu") ? (c.getWaktu() == null ? WaktuUtil.getDate() : c.getWaktu())
                    : Common.dateFormat1.get().parse(ApiHelperSupport.optString(json, "waktu")));
            if (!ApiHelperSupport.isNullOrEmptyJsonValue(json, "tahunAjaran")) {
                c.setTahunAjaran(ApiHelperSupport.optString(json, "tahunAjaran"));
            }
            if (!ApiHelperSupport.isNullOrEmptyJsonValue(json, "semester")) {
                c.setSemester(Integer.valueOf(Integer.parseInt(ApiHelperSupport.optString(json, "semester"))));
            }
            if (json.has("parameterTambahan") && !json.isNull("parameterTambahan")) {
                c.setParameterTambahan(ApiHelperSupport.optString(json, "parameterTambahan"));
            }
            if (json.has("parameterTambahanInds") && !json.isNull("parameterTambahanInds")) {
                c.setParameterTambahanInds(ApiHelperSupport.optString(json, "parameterTambahanInds"));
            }
            if (baru) {
                c.setOleh(ApiHelperSupport.safeString(tbmuser.getNama()));
                c.setOlehId(ApiHelperSupport.safeString(tbmuser.getUserId()));
            }

            session = HibernateUtil.getSessionFactory().openSession();
            session.getTransaction().begin();
            Common.refreshSaveOrUpdate(session, c);
            session.getTransaction().commit();

            hasil.put("id", c.getId());
            ApiHelperSupport.putSuccess(hasil, baru ? "Catatan berhasil disimpan" : "Catatan berhasil diperbarui");
            return hasil;
        } catch (Exception e) {
            return ApiHelperSupport.errorResponse("Gagal menyimpan catatan siswa");
        } finally {
            if (session != null) {
                try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/CatatanApi.java:607");}
                try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/CatatanApi.java:608");}
                try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/CatatanApi.java:609");}
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // 6) HAPUS CATATAN (Delete)  — action: "catatan_siswa_hapus"
    // ════════════════════════════════════════════════════════════════════════

    /**
     * <h2>{@code hapus} — Hapus Catatan Siswa (operasi Delete pada CRUD)</h2>
     *
     * <p>Menghapus satu {@link CatatanSiswa} berdasarkan id. Operasi ini melengkapi siklus CRUD.
     * Mengingat catatan dapat menjadi bagian dari rekam jejak resmi (mis. catatan pelanggaran),
     * pemanggilan sebaiknya dibatasi pada peran yang berwenang (guru wali/admin) pada lapisan
     * otorisasi aplikasi; method ini menerima id dan menghapus selama token valid, sehingga
     * pembatasan peran tambahan dapat dipasang di sini sesuai kebijakan sekolah.</p>
     *
     * <h3>Permintaan</h3>
     * <ul>
     *   <li><b>token</b> (wajib).</li>
     *   <li><b>id</b> (wajib) — id catatan yang dihapus.</li>
     * </ul>
     *
     * <h3>Respons</h3>
     * <p>Status <code>"00"</code> bila berhasil dihapus, <code>"99"</code> bila id tidak ditemukan,
     * <code>"97"</code> bila token/parameter tidak valid, atau <code>"500"</code> pada kesalahan
     * server. Penghapusan dilakukan via {@link Common#refreshDelete(Session, Object)} yang mengelola
     * transaksi. Lampiran terkait (bila ada) dikelola terpisah; kebijakan pembersihan lampiran
     * yatim dapat ditambahkan sesuai kebutuhan.</p>
     *
     * @param req  {@link HttpServletRequest}.
     * @param json payload JSON; wajib memuat <code>id</code>.
     * @return {@link JSONObject} status hasil penghapusan.
     */
    public static JSONObject hapus(HttpServletRequest req, JSONObject json) {
        JSONObject hasil = new JSONObject();
        Session session = null;
        try {
            Tbmuser tbmuser = ApiUtil.currentUser(json, req);
            if (tbmuser == null || tbmuser.getUserId() == null) {
                return ApiHelperSupport.status("97", "Token tidak sesuai");
            }
            if (ApiHelperSupport.isNullOrEmptyJsonValue(json, "id")) {
                return ApiHelperSupport.status("97", "Id catatan harus dikirim");
            }
            CatatanSiswa c = (CatatanSiswa) ConstantValues.ambil(CLAZZ_CATATAN_SISWA,
                    Long.parseLong(ApiHelperSupport.optString(json, "id")));
            if (c == null) {
                return ApiHelperSupport.status("99", "Catatan tidak ditemukan");
            }
            session = HibernateUtil.getSessionFactory().openSession();
            session.getTransaction().begin();
            Common.refreshDelete(session, c);
            session.getTransaction().commit();
            ApiHelperSupport.putSuccess(hasil, "Catatan berhasil dihapus");
            return hasil;
        } catch (Exception e) {
            return ApiHelperSupport.errorResponse("Gagal menghapus catatan siswa");
        } finally {
            if (session != null) {
                try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/CatatanApi.java:670");}
                try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/CatatanApi.java:671");}
                try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/CatatanApi.java:672");}
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Helper internal
    // ════════════════════════════════════════════════════════════════════════

    /** Petakan satu CatatanSiswa menjadi JSON ringkas (dipakai daftar + dasar detail). */
    private static JSONObject ringkasCatatan(CatatanSiswa c) throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", c.getId());
        o.put("nama", ApiHelperSupport.safeString(c.getNama()));
        o.put("keterangan", ApiHelperSupport.safeString(c.getKeterangan()));
        o.put("waktu", c.getWaktu() == null ? "" : Common.dateFormat1.get().format(c.getWaktu()));
        o.put("oleh", ApiHelperSupport.safeString(c.getOleh()));
        o.put("parameterTambahanInds", ApiHelperSupport.safeString(c.getParameterTambahanInds()));
        JSONObject jenisObj = new JSONObject();
        if (c.getJenisCatatanSiswa() != null) {
            jenisObj.put("id", c.getJenisCatatanSiswa().getId());
            jenisObj.put("nama", ApiHelperSupport.safeString(c.getJenisCatatanSiswa().getNama()));
        }
        o.put("jenis", jenisObj);
        return o;
    }

    /** Bangun daftar lampiran (JSON array) untuk sebuah catatan berdasarkan parameter berlampiran. */
    @SuppressWarnings("unchecked")
    private static JSONArray lampiranCatatan(CatatanSiswa c) {
        JSONArray arr = new JSONArray();
        Session session = null;
        try {
            if (c == null || c.getId() == null || c.getJenisCatatanSiswa() == null) {
                return arr;
            }
            session = HibernateUtil.getSessionFactory().openSession();
            JenisCatatanSiswa jenis = c.getJenisCatatanSiswa();
            if (jenis.getKelompokParameterTambahanCatatanSiswas() == null) {
                return arr;
            }
            for (KelompokParameterTambahanCatatanSiswa kelompok : jenis
                    .getKelompokParameterTambahanCatatanSiswas()) {
                if (kelompok == null) {
                    continue;
                }
                List<ParameterTambahan> params = ConstantValues.simpleList(
                        session.createCriteria(ParameterTambahanCatatanSiswa.class)
                                .add(Restrictions.eq("kelompokParameterTambahanCatatanSiswa", kelompok))
                                .createAlias("parameterTambahan", "parameterTambahan")
                                .add(Restrictions.eq("parameterTambahan.aktif", true))
                                .setProjection(Projections.groupProperty("parameterTambahan.id")),
                        ParameterTambahan.class, false);
                if (params == null) {
                    continue;
                }
                for (ParameterTambahan p : params) {
                    if (p == null || !Boolean.TRUE.equals(p.getLampiranWajibDiisi())) {
                        // Hanya parameter berlampiran yang relevan; tetap cek keberadaan berkas.
                    }
                    String key = kelompok.getId() + "->" + p.getId();
                    LampiranLain lampiran = LampiranLain.ambil(c.getId(), key);
                    if (lampiran == null) {
                        continue;
                    }
                    JSONObject lo = new JSONObject();
                    lo.put("key", key);
                    lo.put("label", ApiHelperSupport.safeString(p.getLabelInputan()));
                    lo.put("nama", ApiHelperSupport.safeString(lampiran.getNama()));
                    lo.put("url", "/AmbilLampiran?download=1&ref=" + c.getId() + "&clazz=" + CLAZZ_CATATAN_SISWA
                            + "&jenis=" + key);
                    arr.put(lo);
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/CatatanApi.java:746");
        } finally {
            if (session != null) {
                try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/CatatanApi.java:749");}
                try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/CatatanApi.java:750");}
                try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/api/CatatanApi.java:751");}
            }
        }
        return arr;
    }

    /** Resolusi siswa subjek dari parameter "siswa"; null bila tidak ada/invalid. */
    private static Siswa resolveSiswa(JSONObject json, Tbmuser tbmuser) {
        try {
            if (!ApiHelperSupport.isNullOrEmptyJsonValue(json, "siswa")) {
                return (Siswa) ConstantValues.ambil(Siswa.class.getName(),
                        Long.parseLong(ApiHelperSupport.optString(json, "siswa")));
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/CatatanApi.java:764");
        }
        return null;
    }

    /** Rentang tanggal default 6 bulan terakhir s.d. hari ini; menghormati "mulai"/"sampai". */
    private static Date[] rentangTanggal(JSONObject json) throws Exception {
        Calendar cal = WaktuUtil.getCalendar();
        cal.add(Calendar.MONTH, -6);
        Date mulai = json.isNull("mulai") ? cal.getTime()
                : Common.dateFormat1.get().parse(ApiHelperSupport.optString(json, "mulai"));
        Date sampai = json.isNull("sampai") ? WaktuUtil.getDate()
                : Common.dateFormat1.get().parse(ApiHelperSupport.optString(json, "sampai"));
        return new Date[] { mulai, sampai };
    }

    /** Ambil nama kelompok secara aman (beberapa varian getter nama dipakai di model). */
    private static String getKelompokNama(KelompokParameterTambahanCatatanSiswa kelompok) {
        try {
            return kelompok.getNama();
        } catch (Throwable t) {
            return "";
        }
    }
}
