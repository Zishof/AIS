package ais.common;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.EntityMode;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.Type;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.KrsMahasiswa;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Skripsi;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.file.FileFoto;
import ais.database.model.file.FotoAdmin;
import ais.database.model.file.FotoBiodataCalonMahasiswa;
import ais.database.model.file.FotoCalonPegawai;
import ais.database.model.file.FotoCalonSiswa;
import ais.database.model.file.FotoDosen;
import ais.database.model.file.FotoGambarProduk;
import ais.database.model.file.FotoGuru;
import ais.database.model.file.FotoMahasiswa;
import ais.database.model.file.FotoMahasiswaLulus;
import ais.database.model.file.FotoPegawai;
import ais.database.model.file.FotoSiswa;
import ais.database.model.file.LampiranBeasiswaMahasiswa;
import ais.database.model.file.LampiranKknMahasiswa;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.LampiranLainBiodataCalonMahasiswa;
import ais.database.model.file.LampiranLainMahasiswa;
import ais.database.model.file.LampiranPklMahasiswa;
import ais.database.model.file.PertemuanFileContent;
import ais.database.model.file.TugasFileContent;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.pkl.KelompokPkl;
import ais.database.model.streaming.AudioPertemuan;
import ais.database.model.streaming.VideoPertemuan;

/**
 * Mesin serialisasi/deserialisasi generik antara entitas domain AIS ({@link GeneralValueObject}
 * dan turunannya, dipetakan lewat metadata Hibernate) dengan {@link JSONObject}/{@link JSONArray}.
 * Kelas ini adalah tulang punggung banyak fitur AIS yang perlu mengirim/menerima objek domain
 * lengkap (bukan DTO manual) lewat JSON — misalnya menyimpan draft form ke file sementara,
 * mengirim payload antar-modul, atau menyerialisasi objek besar dengan relasi berlapis untuk
 * dikonsumsi klien. Berbeda dari serialisasi JSON berbasis reflection generik (mis. Gson/Jackson),
 * kelas ini secara sengaja memakai {@link ClassMetadata} Hibernate ({@code
 * HibernateUtil#getClassMetadata}/{@code StreamingHibernateUtil#getClassMetadata}) sebagai sumber
 * kebenaran daftar properti dan tipe setiap entitas, sehingga hasil serialisasi selalu konsisten
 * dengan pemetaan ORM yang sesungguhnya dipakai aplikasi (termasuk properti yang di-lazy-load).
 *
 * <h2>Dua "dunia" metadata Hibernate: reguler vs streaming</h2>
 * <p>
 * AIS memiliki dua {@code SessionFactory} terpisah: satu untuk entitas basis data utama
 * ({@link HibernateUtil}) dan satu lagi khusus entitas berkas biner besar seperti foto/lampiran/
 * audio/video ({@link StreamingHibernateUtil}, kemungkinan memakai penyimpanan atau strategi
 * pemetaan berbeda dari database utama). Konstanta {@link #STREAMING_CLASSES} adalah daftar tetap
 * (di-hardcode di kode sumber, bukan hasil pemindaian otomatis) nama kelas yang tergolong dunia
 * streaming tersebut — dipakai di seluruh method (de)serialisasi untuk menentukan
 * {@link ClassMetadata} mana yang harus diminta, sehingga salah memilih daftar ini dapat membuat
 * suatu entitas foto/video gagal di-(de)serialisasi meski entitasnya sendiri valid.
 * </p>
 *
 * <h2>Alur deserialisasi: {@code convertToObject}/{@code convertToList}/{@code convertToSet}</h2>
 * <p>
 * {@link #convertToObject(JSONObject, Class, int)} adalah implementasi kanonik: (1) menentukan
 * kelas target (dari parameter, atau dari properti {@code "class"} pada JSON bila parameter
 * {@code null} — pola "polymorphic JSON" yang membawa nama kelasnya sendiri); (2) mencoba memuat
 * entitas YANG SUDAH ADA di database lewat {@link #loadExistingObject} berdasarkan id (atau
 * {@code userId}/{@code roleId} untuk {@link Tbmuser}/{@link Tbmrole} yang primary key-nya bukan
 * {@code id} numerik biasa); (3) bila tidak ditemukan, membuat instance baru lewat refleksi
 * ({@code clazz.newInstance()}) dan mengisi id dasarnya lewat {@link #populateBasicIds}; (4)
 * mengisi seluruh properti lain lewat {@link #populateProperties}, yang untuk tiap properti mencoba
 * lebih dulu menafsirkannya sebagai relasi ke entitas lain ({@link #populateSubObject} — termasuk
 * relasi yang disimpan sebagai <b>referensi file</b> terpisah untuk objek besar/dalam, dibaca
 * rekursif dengan batas kedalaman {@code depth < 25} untuk mencegah rekursi tak berujung pada data
 * yang saling merujuk) sebelum jatuh ke properti primitif/tanggal biasa
 * ({@link #populatePrimitiveOrDate}); (5) memanggil hook finalisasi legacy
 * {@code GeneralValueObject.masukkanData(Class, GeneralValueObject)} agar entitas dapat melakukan
 * penyesuaian tambahan setelah deserialisasi generik selesai.
 * </p>
 * <p>
 * Representasi tanggal dalam JSON memakai skema ganda: satu kunci berformat teks manusiawi (dipilih
 * antara {@link Common#dateFormat1}/{@link Common#dateFormat3} berdasarkan
 * {@link #checkTanggalAtauTimeStamp}, yang menentukan per kelas+nama-properti apakah suatu tanggal
 * sebaiknya ditampilkan sebagai tanggal murni atau tanggal+waktu) DAN satu kunci
 * {@code <properti>_milis_str} berisi epoch milliseconds sebagai representasi presisi yang tidak
 * ambigu — saat parsing, kunci milis diprioritaskan bila tersedia, baru jatuh ke parsing teks
 * dengan percobaan format kedua sebagai fallback bila format pertama gagal.
 * </p>
 *
 * <h2>Alur serialisasi: {@code convertToJson}/{@code convertToJsonObject}/{@code
 * convertToJsonObjectSimple}</h2>
 * <p>
 * Ada DUA varian serialisasi objek ke JSON dengan strategi berbeda: {@link
 * #convertToJsonObject(Integer, GeneralValueObject, String...)} menulis relasi ke entitas
 * "kompleks" (bukan {@link ConstantValues#classExist master data sederhana}) sebagai REFERENSI FILE
 * terpisah di disk (lewat {@code subContent.write(...)}, dibaca balik memakai mekanisme rekursif
 * yang sama saat deserialisasi) — dipakai saat objek yang diserialisasi berpotensi sangat besar/
 * dalam dan tidak semuanya perlu ada dalam satu payload JSON sekaligus. Sebaliknya, {@link
 * #convertToJsonObjectSimple(GeneralValueObject, int)} menyematkan relasi LANGSUNG sebagai
 * sub-objek JSON ({@code <key>_data}) secara rekursif in-place, dibatasi kedalaman maksimum 2 level
 * ({@code dept < 2}) untuk mencegah payload membengkak tak terkendali pada graf objek yang saling
 * berelasi. Kedua varian sama-sama dapat menerima daftar {@code clazzPengecualian} (nama kelas yang
 * dikecualikan dari serialisasi, mis. untuk memutus siklus referensi balik atau menyembunyikan
 * relasi tertentu).
 * </p>
 *
 * <h2>Berkas JSON sementara per objek: {@code setJSONTemporary}/{@code getJSONTemporary}</h2>
 * <p>
 * {@link #getFileLocation(GeneralValueObject, String)} menentukan path file JSON unik per (kelas,
 * id objek, kunci logis) di bawah direktori {@link ConstantValues#ambilLokasiFileTemprorary(Class)},
 * dengan skema penamaan path KHUSUS untuk beberapa kelas (mis. {@link Tbmuser}/{@link Tbmrole}
 * memakai userId/roleId, {@link Perkuliahan} memakai hierarki fakultas/jurusan/tahun ajaran/
 * semester). Direktori induk dibuat otomatis sekali per path dan di-cache di {@link #ENSURED_DIRS}
 * (memakai {@link java.util.concurrent.ConcurrentHashMap} sebagai backing set thread-safe) untuk
 * menghindari pemeriksaan {@code exists()}/{@code mkdirs()} berulang-ulang yang lambat pada
 * filesystem jaringan — dijelaskan pada komentar sejarah performa di atas {@link #ENSURED_DIRS}
 * sebagai perbaikan atas startup yang macet karena pemanggilan {@code getFileLocation} ribuan kali.
 * {@link #setJSONTemporary}/{@link #getJSONTemporary} adalah pasangan tulis/baca sederhana di atas
 * lokasi file tersebut, dipakai misalnya untuk menyimpan draft form yang belum final.
 * </p>
 *
 * <h2>Utilitas tambahan</h2>
 * <p>
 * {@link #ambilLong(JSONObject, String)} adalah konversi angka/{@link String}/{@code null} yang
 * toleran ke {@link Long}, dipakai di hampir seluruh titik parsing id pada kelas ini. {@link
 * #getUrls(String)} mengekstrak seluruh URL (dengan atau tanpa skema {@code http(s)://}) dari teks
 * bebas memakai satu pola regex tetap, dengan pembersihan tanda baca penutup yang ikut tertangkap
 * dan penambahan skema {@code https://} otomatis untuk domain tanpa skema (mis. {@code www.
 * 4shared.com}) — dipakai kemungkinan besar untuk mendeteksi tautan yang disisipkan pengguna dalam
 * teks bebas (pengumuman, keterangan, dsb.) agar dapat ditampilkan sebagai tautan yang bisa diklik.
 * </p>
 */
public class CommonJSONUtil {

    /**
     * Daftar tetap (hardcoded) nama kelas lengkap ({@code Class#getName()}) entitas yang dipetakan
     * lewat {@link StreamingHibernateUtil} (SessionFactory terpisah untuk berkas biner besar:
     * foto berbagai jenis pengguna, lampiran, video/audio pertemuan) alih-alih
     * {@link HibernateUtil} biasa. Dipakai di seluruh method (de)serialisasi kelas ini untuk memilih
     * sumber {@link ClassMetadata} yang benar; pencarian keanggotaan memakai {@link HashSet} untuk
     * kompleksitas waktu O(1) mengingat method-method tersebut dapat dipanggil sangat sering.
     */
    // Optimasi: Set statis untuk pencarian cepat (O(1))
    public static final Set<String> STREAMING_CLASSES = new HashSet<String>(Arrays.asList(
            PertemuanFileContent.class.getName(), TugasFileContent.class.getName(), LampiranLain.class.getName(),
            FotoCalonPegawai.class.getName(), FotoGambarProduk.class.getName(), FotoCalonSiswa.class.getName(),
            FotoBiodataCalonMahasiswa.class.getName(), FotoMahasiswaLulus.class.getName(),
            LampiranLainBiodataCalonMahasiswa.class.getName(), LampiranLainMahasiswa.class.getName(),
            FotoMahasiswa.class.getName(), FotoDosen.class.getName(), FotoPegawai.class.getName(),
            FotoGuru.class.getName(), FotoSiswa.class.getName(), FotoAdmin.class.getName(),
            LampiranBeasiswaMahasiswa.class.getName(), LampiranPklMahasiswa.class.getName(),
            LampiranKknMahasiswa.class.getName(), VideoPertemuan.class.getName(), AudioPertemuan.class.getName()
    ));

    // ==========================================
    // SECTION: CONVERT TO LIST / SET
    // ==========================================

    /**
     * Mengonversi setiap elemen {@link JSONArray} (diasumsikan berupa {@link JSONObject}) menjadi
     * entitas domain lewat {@link #convertToObject(JSONObject, Class, int)}, dikumpulkan sebagai
     * {@link List}. Bila {@code clazz} bernilai {@code null}, kelas target ditentukan per elemen
     * dari properti {@code "class"} pada JSON elemen tersebut (memungkinkan satu array berisi
     * campuran beberapa jenis entitas). Kegagalan konversi satu elemen dicatat ke audit dan
     * elemen tersebut dilewati, tidak menggagalkan konversi elemen lain dalam array yang sama.
     *
     * @param array daftar JSON sumber, boleh {@code null} (menghasilkan list kosong)
     * @param clazz kelas target seragam untuk seluruh elemen, atau {@code null} untuk resolusi
     *              per-elemen dari properti {@code "class"}
     * @return daftar entitas hasil konversi, tidak pernah {@code null} (list kosong bila
     *         {@code array} kosong/{@code null} atau seluruh elemen gagal dikonversi)
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static List convertToList(JSONArray array, Class clazz) {
        List data = new ArrayList();
        if (array == null) return data;

        int s = array.length();
        for (int i = 0; i < s; i++) {
            try {
                JSONObject json = array.getJSONObject(i);
                Class targetClass = clazz;
                // Jika clazz null, coba cari dari properti "class" di JSON
                if (targetClass == null && !json.isNull("class")) {
                    targetClass = Class.forName(json.getString("class"));
                }
                
                if (targetClass != null) {
                    data.add(convertToObject(json, targetClass));
                }
            } catch (Exception e) {
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonJSONUtil.java:93");
            }
        }
        return data;
    }

    /** Seperti {@link #convertToList(JSONArray, Class)} dengan kelas target ditentukan per elemen dari properti {@code "class"} pada JSON. */
    @SuppressWarnings("rawtypes")
    public static List convertToList(JSONArray array) {
        return convertToList(array, null);
    }

    /**
     * Seperti {@link #convertToList(JSONArray, Class)}, tetapi hasilnya dikumpulkan sebagai
     * {@link Set} (menghilangkan duplikat berdasarkan {@code equals()}/{@code hashCode()} entitas)
     * alih-alih {@link List}. Berbeda dari {@link #convertToList(JSONArray, Class)},
     * {@code clazz} di sini TIDAK memiliki fallback resolusi otomatis dari properti {@code "class"}
     * per elemen — kelas target harus selalu diberikan eksplisit oleh pemanggil.
     *
     * @param array daftar JSON sumber, boleh {@code null} (menghasilkan set kosong)
     * @param clazz kelas target seragam untuk seluruh elemen
     * @return himpunan entitas hasil konversi, tidak pernah {@code null}
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Set convertToSet(JSONArray array, Class clazz) {
        Set data = new HashSet();
        if (array == null) return data;

        int s = array.length();
        for (int i = 0; i < s; i++) {
            try {
                data.add(convertToObject(array.getJSONObject(i), clazz));
            } catch (Exception e) {
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonJSONUtil.java:114");
            }
        }
        return data;
    }

    // ==========================================
    // SECTION: CONVERT TO OBJECT (DESERIALIZATION)
    // ==========================================

    /** Seperti {@link #convertToObject(JSONObject, Class, int)} dengan kelas target ditentukan otomatis dari properti {@code "class"} pada JSON dan kedalaman rekursi awal {@code 0}. */
    public static GeneralValueObject convertToObject(JSONObject json) {
        return convertToObject(json, null, 0);
    }

    /** Seperti {@link #convertToObject(JSONObject, Class, int)} dengan kedalaman rekursi awal {@code 0}. */
    @SuppressWarnings("rawtypes")
    public static GeneralValueObject convertToObject(JSONObject json, Class clazz) {
        return convertToObject(json, clazz, 0);
    }

    /**
     * Implementasi kanonik deserialisasi JSON menjadi entitas domain {@link GeneralValueObject}.
     * Lihat javadoc kelas {@link CommonJSONUtil} bagian "Alur deserialisasi" untuk uraian langkah
     * demi langkah lengkap (resolusi kelas, pemuatan entitas existing vs instance baru, pengisian
     * properti relasi/primitif/tanggal, hook finalisasi legacy).
     *
     * @param json  objek JSON sumber; bila {@code null}, method langsung mengembalikan {@code null}
     * @param clazz kelas target entitas; bila {@code null}, ditentukan dari properti {@code "class"}
     *              pada {@code json} (bila properti itu juga tidak ada, method mengembalikan
     *              {@code null})
     * @param depth kedalaman rekursi saat ini, dipakai untuk membatasi pembacaan relasi berbentuk
     *              referensi file bersarang (lihat {@link #populateSubObject}) agar tidak berulang
     *              tak terhingga pada {@code depth < 25}
     * @return entitas hasil deserialisasi (existing yang diperbarui atau instance baru), atau
     *         {@code null} bila {@code json} kosong, kelas tidak dapat ditentukan, atau terjadi
     *         kegagalan yang tertangkap secara internal (dicatat ke audit, tidak dilempar ke
     *         pemanggil)
     */
    @SuppressWarnings("rawtypes")
    public static GeneralValueObject convertToObject(JSONObject json, Class clazz, int depth) {
        if (json == null) return null;

        GeneralValueObject obj = null;
        try {
            // Resolusi Class
            if (clazz == null && !json.isNull("class")) {
                clazz = Class.forName(json.getString("class"));
            }
            if (clazz == null) return null;

            // 1. Coba load existing object dari Database
            obj = loadExistingObject(json, clazz);

            // 2. Jika tidak ada, buat instance baru
            if (obj == null) {
                obj = (GeneralValueObject) clazz.newInstance();
                populateBasicIds(obj, json);
            }

            // 3. Ambil Metadata Hibernate
            ClassMetadata classMetadata = STREAMING_CLASSES.contains(clazz.getName())
                    ? StreamingHibernateUtil.getInstance().getClassMetadata(clazz)
                    : HibernateUtil.getClassMetadata(clazz);

            if (classMetadata == null) return obj;

            // 4. Isi property object
            populateProperties(obj, json, classMetadata, depth);

            // 5. Finalisasi (Custom method legacy)
            try {
                GeneralValueObject.masukkanData(clazz, obj);
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonJSONUtil.java:167"); /* Ignore */ }

        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonJSONUtil.java:170");
        }

        return obj;
    }

    /**
     * Mencoba memuat entitas yang SUDAH ADA di database berdasarkan pengenal yang tersedia pada
     * {@code json}: kolom {@code "id"} untuk entitas biasa, atau {@code "userId"}/{@code "roleId"}
     * khusus untuk {@link Tbmuser}/{@link Tbmrole} (yang primary key-nya bukan {@code id} numerik).
     * Hanya mencoba bila {@code clazz} tergolong "master data" yang dikenal
     * ({@link ConstantValues#classExist(Class)}). Bila entitas ditemukan, method juga memicu
     * penyegaran datanya lewat hook legacy {@code GeneralValueObject.masukkanData}.
     *
     * @param json  objek JSON sumber pengenal
     * @param clazz kelas entitas yang dicari
     * @return entitas yang ditemukan di database, atau {@code null} bila tidak ditemukan, bukan
     *         master data yang dikenal, atau terjadi kegagalan (dicatat ke audit)
     */
    // Helper: Load object dari DB
    @SuppressWarnings("rawtypes")
    private static GeneralValueObject loadExistingObject(JSONObject json, Class clazz) {
        try {
            if (!ConstantValues.classExist(clazz)) return null;

            GeneralValueObject obj = null;
            if (!json.isNull("id")) {
                obj = ConstantValues.ambil(clazz.getName(), ais.common.CommonJSONUtil.ambilLong(json,"id"));
            } else if (!json.isNull("userId") && clazz.getName().equals(Tbmuser.class.getName())) {
                obj = ConstantValues.ambil(clazz.getName(), json.getString("userId"));
            } else if (!json.isNull("roleId") && clazz.getName().equals(Tbmrole.class.getName())) {
                obj = ConstantValues.ambil(clazz.getName(), json.getString("roleId"));
            }
            
            // Jika object ditemukan, refresh data
            if (obj != null) {
                GeneralValueObject.masukkanData(clazz, obj);
            }
            return obj;
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonJSONUtil.java:197");
            return null;
        }
    }

    /**
     * Mengisi pengenal dasar (id/userId/roleId) pada entitas BARU (belum ada di database) yang baru
     * dibuat lewat refleksi di {@link #convertToObject(JSONObject, Class, int)}, dari nilai yang
     * tersedia pada {@code json}. Kegagalan diabaikan (dicatat ke audit) tanpa menghentikan proses
     * deserialisasi.
     */
    // Helper: Set ID/User/Role untuk object baru
    private static void populateBasicIds(GeneralValueObject obj, JSONObject json) {
        try {
            if (obj instanceof Tbmuser && !json.isNull("userId")) {
                ((Tbmuser) obj).setUserId(json.getString("userId"));
            } else if (obj instanceof Tbmrole && !json.isNull("roleId")) {
                ((Tbmrole) obj).setRoleId(json.getString("roleId"));
            } else if (!json.isNull("id")) {
                obj.setId(ais.common.CommonJSONUtil.ambilLong(json,"id"));
            } 
        } catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonJSONUtil.java:212"); }
    }
    
    /**
     * Konversi toleran nilai pada kunci {@code key} milik {@code json} menjadi {@link Long}: nilai
     * bertipe {@link Number} langsung dikonversi lewat {@code longValue()}; nilai tipe lain
     * (biasanya {@link String}) di-trim dan diparsing, dengan string kosong atau bernilai literal
     * {@code "null"} (case-insensitive) diperlakukan sebagai tidak ada nilai. Dipakai di hampir
     * seluruh titik parsing id pada kelas ini sebagai pengganti {@code json.getLong(key)} yang lebih
     * ketat dan mudah melempar exception pada data yang tidak seragam.
     *
     * @param json objek JSON sumber, boleh {@code null}
     * @param key  nama kunci yang diambil nilainya
     * @return nilai {@link Long} hasil konversi, atau {@code null} bila {@code json} {@code null},
     *         kunci tidak ada/bernilai JSON null, kosong setelah di-trim, literal {@code "null"},
     *         atau gagal diparsing sebagai angka
     */
    public static Long ambilLong(JSONObject json, String key) {
//    	try {
//    		return Long.parseLong((json.opt(key)+"").trim());
//    	}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonJSONUtil.java:218");
//			return null;
//		}
    	
    	if (json == null || json.isNull(key)) {
            return null;
        }
        try {
            Object value = json.opt(key);
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            // Fallback untuk String atau tipe lain
            String strVal = String.valueOf(value).trim();
            if (strVal.isEmpty() || strVal.equalsIgnoreCase("null")) {
                return null;
            }
            return Long.parseLong(strVal);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Mengisi seluruh properti {@code obj} (sesuai daftar {@code meta.getPropertyNames()}) dari
     * {@code json}, satu per satu: properti {@code fileLocation}/{@code copyDari}/{@code foto}
     * selalu dilewati (bukan bagian dari data yang perlu di-deserialize), properti yang nilainya
     * JSON null juga dilewati (tidak menimpa nilai yang mungkin sudah ada pada entitas existing).
     * Untuk setiap properti lain, dicoba lebih dulu sebagai relasi ke entitas lain lewat
     * {@link #populateSubObject}, baru jatuh ke {@link #populatePrimitiveOrDate} bila bukan relasi.
     * Kegagalan pada satu properti dicatat ke audit dan TIDAK menghentikan pengisian properti
     * lainnya.
     */
    // Helper: Loop semua property
    private static void populateProperties(GeneralValueObject obj, JSONObject json, ClassMetadata meta, int depth) {
        String[] properties = meta.getPropertyNames();
        for (String key : properties) {
            if ("fileLocation".equalsIgnoreCase(key) || "copyDari".equalsIgnoreCase(key)|| "foto".equalsIgnoreCase(key)) continue;
            if (json.isNull(key)) continue;

            try {
                // Prioritas 1: Sub Object (Relasi)
                if (populateSubObject(obj, json, key, meta, depth)) continue;

                // Prioritas 2: Primitive & Date
                populatePrimitiveOrDate(obj, json, key, meta);
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonJSONUtil.java:254");
                // Error 1 field jangan mematikan proses loading field lain
            }
        }
    }

    /**
     * Mencoba menafsirkan properti {@code key} sebagai RELASI ke entitas {@link GeneralValueObject}
     * lain dan mengisinya bila berhasil. Method ini menangani beberapa bentuk representasi relasi
     * dalam JSON, diperiksa berurutan:
     * <ol>
     * <li>Bila tipe properti yang dideklarasikan entitas BUKAN turunan {@link GeneralValueObject},
     * langsung kembali {@code false} tanpa mencoba apa pun — mencegah properti yang sebenarnya
     * hanya {@link Long} id polos (bukan relasi {@code @ManyToOne} sungguhan) dipaksa diisi dengan
     * objek entitas penuh hanya karena JSON-nya (ditulis serializer generik) tetap menyertakan
     * penanda {@code <key>_class}/{@code <key>.id} seolah relasi.</li>
     * <li>{@code <key>.id} + {@code <key>_class} — relasi biasa berbasis id numerik, dimuat lewat
     * {@link ConstantValues#ambil(String, Serializable)}.</li>
     * <li>{@code <key>.userId}/{@code <key>.roleId} — relasi ke {@link Tbmuser}/{@link Tbmrole}.</li>
     * <li>{@code <key>.file_reference} — relasi ke objek KOMPLEKS yang disimpan sebagai berkas JSON
     * terpisah di disk; dibaca dan dikonversi secara REKURSIF lewat {@link
     * #convertToObject(JSONObject, Class, int)} dengan {@code depth + 1}, hanya bila berkas tersebut
     * ada dan {@code depth} belum mencapai batas 25 (mencegah rekursi tak berujung pada data yang
     * saling merujuk).</li>
     * <li>Fallback: {@code <key>_class} saja (tanpa {@code .id}/{@code .userId}/{@code .roleId}) —
     * kunci {@code key} sendiri dipakai sebagai pengenal, dicoba sebagai {@link Long} lebih dulu lalu
     * jatuh ke {@link String} bila gagal.</li>
     * </ol>
     * <p>
     * Bila entitas relasi tidak ditemukan di database TAPI {@code <key>.id} tersedia, method
     * membuat instance BARU dari kelas relasi tersebut dan mengisi id-nya saja (objek "shell" berisi
     * id tanpa data lain) — berguna untuk referensi yang idnya valid tapi datanya belum/tidak perlu
     * dimuat penuh saat itu.
     * </p>
     *
     * @return {@code true} bila {@code key} berhasil ditafsirkan dan diisi sebagai relasi (baik
     *         entitas penuh maupun objek "shell" berid saja); {@code false} bila {@code key} bukan
     *         relasi (baik karena tipe deklarasinya bukan entitas, maupun karena tidak ada pola JSON
     *         relasi yang cocok) sehingga pemanggil perlu mencoba
     *         {@link #populatePrimitiveOrDate} sebagai gantinya
     * @throws Exception diteruskan dari kegagalan resolusi kelas atau akses metadata Hibernate
     */
    @SuppressWarnings("rawtypes")
    private static boolean populateSubObject(GeneralValueObject obj, JSONObject json, String key, ClassMetadata meta, int depth) throws Exception {
        // FIX PropertyAccessException/ClassCastException: sebagian field HANYA berupa Long id
        // biasa (bukan relasi @ManyToOne), mis. TugasFileContent.mahasiswa. Kalau JSON-nya (ditulis
        // serializer generik) tetap menyertakan "<key>_class"/"<key>.id" seolah relasi, jangan
        // paksa set entity penuh ke field yang tipe deklarasinya bukan GeneralValueObject --
        // biarkan populatePrimitiveOrDate() yang menangani sbg Long/String biasa.
        Type propertyType = meta.getPropertyType(key);
        if (propertyType != null && propertyType.getReturnedClass() != null
                && !GeneralValueObject.class.isAssignableFrom(propertyType.getReturnedClass())) {
            return false;
        }

        GeneralValueObject subobj = null;
        String classKey = key + "_class";

        // Cek ID Relasi
        if (!json.isNull(key + ".id")) {
            String clsName = json.optString(classKey);
            if (clsName != null && !clsName.isEmpty()) {
                subobj = ConstantValues.ambil(clsName, ais.common.CommonJSONUtil.ambilLong(json,key + ".id"));
            }
        } else if (!json.isNull(key + ".userId")) {
            subobj = ConstantValues.ambil(json.getString(classKey), json.getString(key + ".userId"));
        } else if (!json.isNull(key + ".roleId")) {
            subobj = ConstantValues.ambil(json.getString(classKey), json.getString(key + ".roleId"));
        } 
        // Cek File Reference (Rekursif)
        else if (!json.isNull(key + ".file_reference")) {
            File fileRef = new File(json.getString(key + ".file_reference"));
            if (fileRef.exists() && depth < 25) {
                Class clazzData = Class.forName(json.getString(classKey));
                // Baca file JSON referensi
                JSONObject jsonref = new JSONObject(ais.common.BacaTulisUtil.baca(fileRef));
                subobj = convertToObject(jsonref, clazzData, depth + 1);
            }
        }
        // Fallback: key langsung
        else if (!json.isNull(classKey) && ConstantValues.classExist(json.getString(classKey))) {
            Serializable serializable;
            try {
                serializable = ais.common.CommonJSONUtil.ambilLong(json,key);
            } catch (Exception e) {
                serializable = json.getString(key);
            }
            subobj = ConstantValues.ambil(json.getString(classKey), serializable);
        }

        if (subobj != null) {
            meta.setPropertyValue(obj, key, subobj, EntityMode.POJO);
            return true;
        }

        // Object Baru (ID ada tapi di DB tidak ada)
        if (!json.isNull(key + ".id")) {
             Class clazzData = Class.forName(json.getString(classKey));
             subobj = (GeneralValueObject) clazzData.newInstance();
             subobj.setId(ais.common.CommonJSONUtil.ambilLong(json,key + ".id"));
             meta.setPropertyValue(obj, key, subobj, EntityMode.POJO);
             return true;
        }

        return false;
    }

    /**
     * Mengisi properti {@code key} sebagai nilai PRIMITIF/tanggal (dipanggil setelah
     * {@link #populateSubObject} menentukan {@code key} bukan relasi). Tipe target ditentukan dari
     * penanda {@code <key>_class} pada JSON: {@link Integer}, {@link Double}, {@link Boolean},
     * {@link Long} (lewat {@link #ambilLong}), {@link Date} (lewat {@link #parseDate}), atau bila
     * {@code <key>_class} menunjuk ke kelas turunan {@link GeneralValueObject}
     * ({@link #isEntityRelationType}) — kasus relasi yang lolos dari {@link #populateSubObject}
     * karena tidak memiliki penanda {@code .id}/{@code .userId}/{@code .roleId} eksplisit (mis.
     * properti bertipe {@link Tbmuser} yang di-serialize hanya sebagai {@code userId} mentah tanpa
     * kunci {@code .userId}) — diresolusi dengan cara yang sama seperti fallback pada {@link
     * #populateSubObject}: id numerik dicoba lebih dulu, jatuh ke {@link String}, dan bila tipe
     * properti yang dideklarasikan entitas ternyata BUKAN turunan {@link GeneralValueObject} (data
     * lama/skema berbeda), hanya nilai id numeriknya saja yang diisi (bukan objek entitas). Bila
     * {@code <key>_class} tidak ada sama sekali, nilai diperlakukan sebagai {@link String} apa
     * adanya; bila ada tapi tidak cocok dengan tipe mana pun di atas, juga jatuh ke {@link String}.
     *
     * @throws Exception diteruskan dari kegagalan parsing nilai sesuai tipe yang terdeteksi atau
     *                    kegagalan akses metadata Hibernate saat menulis nilai
     */
    private static void populatePrimitiveOrDate(GeneralValueObject obj, JSONObject json, String key, ClassMetadata meta) throws Exception {
        String classKey = key + "_class";
        // Default string jika class tidak ada
        if (json.isNull(classKey)) {
             meta.setPropertyValue(obj, key, json.getString(key), EntityMode.POJO);
             return;
        }
        
        String typeName = json.getString(classKey);

        if (typeName.equals(Integer.class.getName())) {
            meta.setPropertyValue(obj, key, json.getInt(key), EntityMode.POJO);
        } else if (typeName.equals(Double.class.getName())) {
            meta.setPropertyValue(obj, key, json.getDouble(key), EntityMode.POJO);
        } else if (typeName.equals(Boolean.class.getName())) {
            meta.setPropertyValue(obj, key, json.getBoolean(key), EntityMode.POJO);
        } else if (typeName.equals(Long.class.getName())) {
            meta.setPropertyValue(obj, key, ais.common.CommonJSONUtil.ambilLong(json,key), EntityMode.POJO);
        } else if (typeName.equals(Date.class.getName())) {
            Date d = parseDate(json, key, obj);
            meta.setPropertyValue(obj, key, d, EntityMode.POJO);
        } else if (isEntityRelationType(typeName)) {
            // Properti relasi (mis. FormatNilai.kunci -> Tbmuser) yang lolos dari populateSubObject
            // (tidak ada key+".id"/".userId"/".roleId": Tbmuser.getId() selalu null krn PK aslinya
            // userId, jadi saat serialisasi handleSubObjectToJson hanya menulis key+"_class" & key
            // mentah = userId via putIdUserRoleForKey). Resolusi sama seperti fallback di
            // populateSubObject: pakai Long id jika numerik, kalau tidak pakai String (co.
            // userId/roleId). Sebelumnya jatuh ke branch String default di bawah dan meng-set
            // object relasi dengan raw String -> ClassCastException dari Hibernate setter.
            Serializable idVal = ais.common.CommonJSONUtil.ambilLong(json, key);
            if (idVal == null) {
                idVal = json.getString(key);
            }
            // FIX PropertyAccessException/ClassCastException (mis. TugasFileContent.mahasiswa):
            // "<key>_class" bisa menandai tipe entity (data lama/skema beda) padahal properti
            // TUJUAN sekarang deklarasinya Long biasa (bukan relasi). Cek tipe deklarasi properti
            // dulu -- kalau bukan turunan GeneralValueObject, cukup set id numeriknya saja,
            // jangan paksa set objek entity yang akan ditolak Hibernate setter.
            Type propertyTypeRel = meta.getPropertyType(key);
            if (propertyTypeRel != null && propertyTypeRel.getReturnedClass() != null
                    && !GeneralValueObject.class.isAssignableFrom(propertyTypeRel.getReturnedClass())) {
                if (idVal instanceof Long) {
                    meta.setPropertyValue(obj, key, idVal, EntityMode.POJO);
                }
                return;
            }
            Object subobj = ConstantValues.ambil(typeName, idVal);
            if (subobj != null) {
                meta.setPropertyValue(obj, key, subobj, EntityMode.POJO);
            }
        } else {
            meta.setPropertyValue(obj, key, json.getString(key), EntityMode.POJO);
        }
    }

    /** Mengecek apakah nama kelas {@code typeName} adalah kelas yang dapat dimuat dan merupakan turunan {@link GeneralValueObject} (dipakai untuk mendeteksi properti relasi yang lolos dari {@link #populateSubObject}). Mengembalikan {@code false} bila kelas tidak ditemukan. */
    private static boolean isEntityRelationType(String typeName) {
        try {
            Class<?> c = Class.forName(typeName);
            return GeneralValueObject.class.isAssignableFrom(c);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Mem-parsing nilai tanggal properti {@code key} dari {@code json}. Diprioritaskan kunci
     * {@code <key>_milis_str} (epoch milliseconds sebagai string, representasi presisi yang tidak
     * ambigu) bila tersedia; jika tidak, jatuh ke parsing teks pada kunci {@code key} itu sendiri
     * dengan format yang dipilih lewat {@link #checkTanggalAtauTimeStamp} ({@link
     * Common#dateFormat1} untuk tanggal murni atau {@link Common#dateFormat3} untuk tanggal+waktu),
     * dan bila format yang dipilih gagal, dicoba sekali lagi dengan format satunya sebagai fallback.
     *
     * @return {@link Date} hasil parsing, atau {@code null} bila kedua percobaan format gagal
     *         (dicatat ke audit)
     */
    private static Date parseDate(JSONObject json, String key, GeneralValueObject obj) {
        try {
            if (!json.isNull(key + "_milis_str")) {
                return new Date(Long.parseLong(json.getString(key + "_milis_str")));
            }
            
            String dateStr = json.getString(key);
            boolean isDateFormat1 = checkTanggalAtauTimeStamp(obj, key);
            
            try {
                return isDateFormat1 ? Common.dateFormat1.get().parse(dateStr) : Common.dateFormat3.get().parse(dateStr);
            } catch (Exception e) {
                 return isDateFormat1 ? Common.dateFormat3.get().parse(dateStr) : Common.dateFormat1.get().parse(dateStr);
            }
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonJSONUtil.java:355");
            return null;
        }
    }

    // ==========================================
    // SECTION: CONVERT TO JSON (SERIALIZATION)
    // ==========================================

    /**
     * Menyerialisasi setiap elemen {@code obj} (diasumsikan {@link GeneralValueObject}) lewat
     * {@link #convertToJsonObject(GeneralValueObject, String...)} menjadi satu {@link JSONArray}.
     * Kegagalan pada satu elemen dicatat ke audit dan elemen tersebut dilewati, tidak menggagalkan
     * serialisasi elemen lain.
     *
     * @param obj               koleksi entitas sumber, boleh {@code null} (menghasilkan array
     *                          kosong)
     * @param clazzPengecualian nama kelas yang dikecualikan dari properti relasi tiap entitas, lihat
     *                          {@link #convertToJsonObject(Integer, GeneralValueObject, String...)}
     * @return array JSON hasil serialisasi, tidak pernah {@code null}
     */
    @SuppressWarnings("rawtypes")
    public static JSONArray convertToJson(Collection obj, String... clazzPengecualian) {
        JSONArray array = new JSONArray();
        if (obj == null) return array;

        for (Object o : obj) {
            try {
                array.put(convertToJsonObject((GeneralValueObject) o, clazzPengecualian));
            } catch (Exception e) {
                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonJSONUtil.java:373");
            }
        }
        return array;
    }

    /** Seperti {@link #convertToJsonObject(Integer, GeneralValueObject, String...)} dengan indeks berkas referensi awal {@code 0}. */
    public static JSONObject convertToJsonObject(GeneralValueObject obj, String... clazzPengecualian) {
        return convertToJsonObject(0, obj, clazzPengecualian);
    }

    /**
     * Implementasi kanonik serialisasi entitas ke JSON dengan strategi "referensi file" untuk relasi
     * kompleks — lihat javadoc kelas {@link CommonJSONUtil} bagian "Alur serialisasi" untuk
     * perbandingan dengan {@link #convertToJsonObjectSimple(GeneralValueObject, int)}.
     *
     * <p>
     * Bila {@code obj} tergolong master data sederhana ({@link ConstantValues#classExist(Class)}),
     * hanya pengenalnya saja yang ditulis (lewat {@link #putIdUserRole}) — tidak ada properti lain
     * yang diserialisasi, karena entitas semacam ini diasumsikan dapat dimuat ulang penuh dari
     * database cukup dari id-nya. Untuk entitas lain, seluruh properti ({@code classMetadata
     * .getPropertyNames()}, mengecualikan {@code fileLocation} dan — khusus entitas streaming —
     * {@code foto}) ditulis: relasi ke {@link GeneralValueObject} lain ditangani lewat
     * {@link #handleSubObjectToJson} (yang menulis referensi file untuk relasi kompleks), tanggal
     * lewat {@link #handleDateToJson}, dan nilai lain ditulis langsung. Properti bertipe kelas yang
     * namanya ada dalam {@code clazzPengecualian} dilewati sepenuhnya (tidak ditulis sama sekali,
     * termasuk penanda {@code <key>_class}-nya). Khusus entitas streaming bertipe {@link FileFoto},
     * ditambahkan properti {@code lokasi_file_absolut} berisi URL unduhan Google Drive (bila
     * tersimpan di Gdrive) atau path absolut lokal.
     * </p>
     *
     * @param indexke           indeks berkas referensi berjalan, diteruskan (dan diinkremen) ke
     *                          {@link #handleSubObjectToJson} untuk relasi kompleks, agar berkas
     *                          referensi yang ditulis untuk objek berbeda dalam satu operasi
     *                          serialisasi tidak saling menimpa
     * @param obj               entitas yang diserialisasi; bila {@code null}, mengembalikan
     *                          {@link JSONObject} kosong
     * @param clazzPengecualian nama kelas (lengkap) yang propertinya dikecualikan dari hasil
     * @return objek JSON hasil serialisasi
     */
    public static JSONObject convertToJsonObject(Integer indexke, GeneralValueObject obj, String... clazzPengecualian) {
        JSONObject ob = new JSONObject();
        if (obj == null) return ob;

        // 1. Jika Master Data / Class sederhana, ambil ID saja
        if (ConstantValues.classExist(obj.getClass())) {
            putIdUserRole(ob, obj);
            return ob;
        }

        // 2. Metadata Check
        boolean isStreaming = STREAMING_CLASSES.contains(obj.getClass().getName());
        ClassMetadata classMetadata = isStreaming
                ? StreamingHibernateUtil.getInstance().getClassMetadata(obj.getClass())
                : HibernateUtil.getClassMetadata(obj.getClass());

        // 3. Handle FileFoto (URL Gdrive/Local)
        if (isStreaming && obj instanceof FileFoto) {
            try {
                FileFoto fileFoto = (FileFoto) obj;
                String path = (fileFoto.getGdrive() != null) ? fileFoto.downloadGDriveUrl() : fileFoto.ambilFile().getAbsolutePath();
                ob.put("lokasi_file_absolut", path);
            } catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonJSONUtil.java:405"); }
        }

        putIdUserRole(ob, obj);

        // 4. Populate JSON Properties
        if (classMetadata != null) {
            String[] props = classMetadata.getPropertyNames();
            for (String key : props) {
                if ("fileLocation".equalsIgnoreCase(key)||(isStreaming && "foto".equalsIgnoreCase(key))) continue;

                try {
                    Object subContent = classMetadata.getPropertyValue(obj, key, EntityMode.POJO);
                    if (subContent == null) continue;

                    Type type = classMetadata.getPropertyType(key);
                    String returnedClassName = type.getReturnedClass().getName();

                    if (isExcluded(returnedClassName, clazzPengecualian)) continue;

                    ob.put(key + "_class", returnedClassName);

                    if (subContent instanceof GeneralValueObject) {
                        handleSubObjectToJson(ob, key, (GeneralValueObject) subContent, indexke, clazzPengecualian);
                    } else if (subContent instanceof Date) {
                        handleDateToJson(ob, key, (Date) subContent, obj);
                    } else {
                        ob.put(key, subContent);
                    }
                } catch (Exception e) {
                    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonJSONUtil.java:435");
                }
            }
        }
        return ob;
    }

    /**
     * Varian serialisasi alternatif yang menyematkan relasi LANGSUNG sebagai sub-objek JSON in-place
     * ({@code <key>_data}) secara rekursif, alih-alih menulis referensi file terpisah seperti
     * {@link #convertToJsonObject(Integer, GeneralValueObject, String...)} — lihat javadoc kelas
     * {@link CommonJSONUtil} bagian "Alur serialisasi" untuk perbandingan kedua strategi. Untuk
     * setiap properti: {@link String} ditulis lewat {@code toString()}, {@link Date} lewat
     * {@link #handleDateSimple}, {@link Number}/{@link Boolean} ditulis langsung, dan relasi ke
     * {@link GeneralValueObject} lain di-rekursi lewat pemanggilan diri sendiri dengan
     * {@code dept + 1} — HANYA bila {@code dept} saat ini masih kurang dari 2 (membatasi kedalaman
     * penyematan maksimum 2 level relasi, mencegah payload membengkak tak terkendali pada graf objek
     * yang saling berelasi banyak level); relasi yang melebihi batas kedalaman ini tidak disematkan
     * sama sekali (properti tersebut diabaikan, bukan ditulis parsial). Untuk relasi yang disematkan,
     * selain {@code <key>_data} juga ditulis {@code <key>.id} dan pengenal user/role/id yang sesuai
     * lewat {@link #putIdUserRoleForKey}. Berbeda dari {@link #convertToJsonObject(Integer,
     * GeneralValueObject, String...)}, method ini tidak menerima parameter pengecualian kelas.
     *
     * @param obj  entitas yang diserialisasi; bila {@code null}, mengembalikan {@link JSONObject}
     *             kosong
     * @param dept kedalaman rekursi saat ini (panggilan awal biasanya memakai {@code 0}); relasi
     *             hanya disematkan rekursif selama {@code dept < 2}
     * @return objek JSON hasil serialisasi, dengan relasi tersemat langsung hingga 2 level
     */
    public static JSONObject convertToJsonObjectSimple(GeneralValueObject obj, int dept) {
        JSONObject ob = new JSONObject();
        if (obj == null) return ob;

        boolean isStreaming = isStreamingClass(obj);

        ClassMetadata classMetadata = isStreaming 
                ? StreamingHibernateUtil.getInstance().getClassMetadata(obj.getClass())
                : HibernateUtil.getClassMetadata(obj.getClass());

        // Handle FileFoto
        if (isStreaming && obj instanceof FileFoto) {
            try {
                FileFoto fileFoto = (FileFoto) obj;
                if (fileFoto.ambilFile() != null) {
                    ob.put("lokasi_file_absolut", fileFoto.ambilFile().getAbsolutePath());
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonJSONUtil.java:459"); /* Silent fail */ }
        }

        // Handle Basic IDs
        putIdUserRole(ob, obj);

        // Handle Properties
        if (classMetadata != null) {
            String[] propertyNames = classMetadata.getPropertyNames();

            for (String key : propertyNames) {
                if ("fileLocation".equalsIgnoreCase(key)) continue;

                try {
                    Object subContent = classMetadata.getPropertyValue(obj, key, EntityMode.POJO);
                    if (subContent == null) continue;

                    Class<?> returnedClass = classMetadata.getPropertyType(key).getReturnedClass();
                    String className = returnedClass.getName();

                    ob.put(key + "_class", className);

                    if (String.class.isAssignableFrom(returnedClass)) {
                        ob.put(key, subContent.toString());
                    } else if (Date.class.isAssignableFrom(returnedClass)) {
                        handleDateSimple(ob, key, (Date) subContent, obj);
                    } else if (Number.class.isAssignableFrom(returnedClass) || Boolean.class.isAssignableFrom(returnedClass)) {
                        ob.put(key, subContent);
                    } else if (subContent instanceof GeneralValueObject && dept < 2) {
                        // RECURSION logic
                        GeneralValueObject subObj = (GeneralValueObject) subContent;
                        
                        // FIX: Gunakan dept + 1, jangan ubah variabel dept dengan ++
                        JSONObject subData = convertToJsonObjectSimple(subObj, dept + 1);
                        
                        ob.put(key + "_data", subData);
                        ob.put(key + ".id", subObj.getId());
                        putIdUserRoleForKey(ob, key, subObj);
                    }

                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonJSONUtil.java:499"); /* Ignore single field error */ }
            }
        }
        return ob;
    }

    // ==========================================
    // SECTION: UTILITIES (FILE & HELPERS)
    // ==========================================

    /** Menulis pengenal entitas ke {@code ob}: kunci {@code "userId"} untuk {@link Tbmuser}, {@code "roleId"} untuk {@link Tbmrole}, atau {@code "id"} untuk entitas lain. */
    private static void putIdUserRole(JSONObject ob, GeneralValueObject obj) {
        try {
            if (obj instanceof Tbmuser) ob.put("userId", ((Tbmuser) obj).getUserId());
            else if (obj instanceof Tbmrole) ob.put("roleId", ((Tbmrole) obj).getRoleId());
            else ob.put("id", obj.getId());
        } catch (JSONException e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonJSONUtil.java:514"); }
    }
    
    /** Seperti {@link #putIdUserRole(JSONObject, GeneralValueObject)}, tetapi menulis nilai pengenal ke kunci {@code key} yang diberikan (dipakai saat menulis pengenal relasi bernama, bukan pengenal objek utama). */
    private static void putIdUserRoleForKey(JSONObject ob, String key, GeneralValueObject obj) throws JSONException {
        if (obj instanceof Tbmuser) ob.put(key, ((Tbmuser) obj).getUserId());
        else if (obj instanceof Tbmrole) ob.put(key, ((Tbmrole) obj).getRoleId());
        else ob.put(key, obj.getId());
    }

    /**
     * Menulis satu properti relasi {@code key} (bernilai {@code subContent}) ke {@code ob} sebagai
     * bagian dari {@link #convertToJsonObject(Integer, GeneralValueObject, String...)}. Bila
     * {@code subContent} tergolong master data sederhana ({@link ConstantValues#classExist(Class)}),
     * cukup ditulis pengenalnya ({@code <key>.id} plus userId/roleId yang sesuai). Bila kompleks,
     * objeknya ditulis ke BERKAS TERPISAH di disk lewat {@code subContent.write(indexke + 1,
     * exceptions)} (rekursi kedalaman berikutnya, meneruskan daftar pengecualian kelas yang sama),
     * dan {@code ob} hanya menyimpan referensi ke berkas tersebut ({@code <key>.file_reference})
     * beserta id objeknya — dibaca balik lewat mekanisme rekursif yang sama pada
     * {@link #populateSubObject} saat deserialisasi.
     *
     * @throws Exception diteruskan dari kegagalan penulisan berkas referensi
     */
    private static void handleSubObjectToJson(JSONObject ob, String key, GeneralValueObject subContent, int indexke, String[] exceptions) throws Exception {
        if (ConstantValues.classExist(subContent.getClass())) {
            ob.put(key + ".id", subContent.getId());
            putIdUserRoleForKey(ob, key, subContent);
        } else {
            // Write complex object to file reference
            ob.put(key + "_class_object", true);
            String ref = subContent.write(indexke + 1, exceptions).getAbsolutePath();
            ob.put(key + ".file_reference", ref);
            ob.put(key, subContent.getId());
        }
    }
    
    /**
     * Menulis satu properti bertipe {@link Date} ke {@code ob} dalam skema ganda: epoch
     * milliseconds sebagai string di {@code <key>_milis_str} (representasi presisi), dan teks
     * berformat manusiawi di {@code key} sendiri, memakai {@link Common#dateFormat1} atau {@link
     * Common#dateFormat3} tergantung hasil {@link #checkTanggalAtauTimeStamp(GeneralValueObject,
     * String)} untuk kombinasi (kelas {@code parentObj}, nama properti {@code key}). Bila
     * pemformatan teks gagal, jatuh ke {@code date.toString()} sebagai fallback terakhir.
     */
    private static void handleDateToJson(JSONObject ob, String key, Date date, GeneralValueObject parentObj) throws JSONException {
        ob.put(key + "_milis_str", String.valueOf(date.getTime()));
        try {
            if (checkTanggalAtauTimeStamp(parentObj, key)) {
                ob.put(key, Common.dateFormat1.get().format(date));
            } else {
                ob.put(key, Common.dateFormat3.get().format(date));
            }
        } catch (Exception e) { ob.put(key, date.toString()); }
    }

    /** Seperti {@link #handleDateToJson(JSONObject, String, Date, GeneralValueObject)}, dipakai oleh {@link #convertToJsonObjectSimple(GeneralValueObject, int)}; kegagalan penulisan diabaikan sepenuhnya (dicatat ke audit, tanpa fallback {@code toString()}). */
    private static void handleDateSimple(JSONObject ob, String key, Date date, GeneralValueObject obj) {
        try {
            ob.put(key + "_milis_str", String.valueOf(date.getTime()));
            if (Common.checkTanggalAtauTimeStamp(obj, key)) {
                ob.put(key, Common.dateFormat1.get().format(date));
            } else {
                ob.put(key, Common.dateFormat3.get().format(date));
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonJSONUtil.java:555"); /* Silent */ }
    }

    /** Mengecek apakah {@code className} ada (case-insensitive) dalam daftar {@code exceptions}. Mengembalikan {@code false} bila {@code exceptions} bernilai {@code null}. */
    private static boolean isExcluded(String className, String[] exceptions) {
        if (exceptions == null) return false;
        for (String s : exceptions) {
            if (s.equalsIgnoreCase(className)) return true;
        }
        return false;
    }

    /** Mengecek apakah kelas {@code obj} tergolong {@link #STREAMING_CLASSES} (dipetakan lewat {@link StreamingHibernateUtil}, bukan {@link HibernateUtil} biasa). */
    private static boolean isStreamingClass(GeneralValueObject obj) {
        return STREAMING_CLASSES.contains(obj.getClass().getName());
    }

    /**
     * Menentukan, untuk kombinasi (kelas entitas, nama properti) tertentu, apakah properti tanggal
     * tersebut sebaiknya diformat sebagai TANGGAL MURNI ({@code true}, format
     * {@link Common#dateFormat1}) atau TANGGAL+WAKTU ({@code false}, format
     * {@link Common#dateFormat3}). Daftar kecocokan di-hardcode per kelas entitas: {@link
     * Pertemuan} ({@code tanggal}/{@code tanggalRealisasi}), {@link Perkuliahan} ({@code
     * perkuliahanDimulai}/{@code perkuliahanSampai}/{@code tanggalMulaiPerkuliahan}),
     * {@link KelompokKkn}/{@link KelompokPkl} ({@code tanggal_mulai}/{@code tanggal_selesai}),
     * {@link MahasiswaRequestTugasAkhir} ({@code tanggalAwalBimbingan}/{@code
     * tanggalAkhirBimbingan}), {@link Skripsi} ({@code tanggalSidang}/{@code tanggalSeminar}/
     * {@code tglSk}), {@link KrsMahasiswa} ({@code tanggalAwalBimbingan}). Perbandingan nama
     * properti tidak peka huruf besar/kecil. Kombinasi yang tidak dikenali (termasuk {@code obj}
     * atau {@code key} bernilai {@code null}) mengembalikan {@code false} (diperlakukan sebagai
     * tanggal+waktu).
     *
     * @param obj entitas yang tipenya diperiksa terhadap daftar kecocokan di atas
     * @param key nama properti tanggal yang diperiksa
     * @return {@code true} bila kombinasi kelas+properti dikenal sebagai tanggal murni;
     *         {@code false} sebaliknya (default: tanggal+waktu)
     */
    public static boolean checkTanggalAtauTimeStamp(GeneralValueObject obj, String key) {
        if (obj == null || key == null) return false;
        String k = key.toLowerCase();

        if (obj instanceof Pertemuan) return k.equals("tanggal") || k.equals("tanggalrealisasi");
        if (obj instanceof Perkuliahan) return k.equals("perkuliahandimulai") || k.equals("perkuliahansampai") || k.equals("tanggalmulaiperkuliahan");
        if (obj instanceof KelompokKkn || obj instanceof KelompokPkl) return k.equals("tanggal_mulai") || k.equals("tanggal_selesai");
        if (obj instanceof MahasiswaRequestTugasAkhir) return k.equals("tanggalawalbimbingan") || k.equals("tanggalakhirbimbingan");
        if (obj instanceof Skripsi) return k.equals("tanggalsidang") || k.equals("tanggalseminar") || k.equals("tglsk");
        if (obj instanceof KrsMahasiswa) return k.equals("tanggalawalbimbingan");

        return false;
    }

	/**
	 * Cache direktori yang SUDAH dipastikan ada (backing set thread-safe di atas
	 * {@link java.util.concurrent.ConcurrentHashMap}). {@code getFileLocation()} dipanggil RIBUAN
	 * kali (retrieve/put/FileFotoLain/branding); sebelum ada cache ini, tiap panggilan melakukan
	 * {@code parent.exists()+mkdirs()} yang sangat lambat pada filesystem lambat/mount jaringan dan
	 * pernah menyebabkan startup aplikasi macet. Dengan cache ini, pemeriksaan {@code exists()}/
	 * {@code mkdirs()} hanya dilakukan sekali per path direktori sepanjang umur JVM.
	 */
	// Cache direktori yang SUDAH dipastikan ada. getFileLocation() dipanggil RIBUAN kali
	// (retreive/put/FileFotoLain/branding); sebelumnya tiap panggilan melakukan
	// parent.exists()+mkdirs() yang sangat lambat di filesystem lambat/mount jaringan dan
	// menyebabkan startup macet. Dengan cache, exists()/mkdirs() hanya sekali per direktori.
	private static final java.util.Set<String> ENSURED_DIRS =
			java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<String, Boolean>());

	/**
	 * Memastikan direktori induk {@code file} ada, membuatnya (beserta direktori antara) bila belum
	 * ada. Memakai cache {@link #ENSURED_DIRS} agar pemeriksaan/pembuatan direktori yang sama hanya
	 * dilakukan sekali. Tidak melakukan apa pun bila {@code file} atau direktori induknya
	 * {@code null}; kegagalan pembuatan direktori dicatat ke audit tanpa melempar exception.
	 */
	private static void ensureParentDir(File file) {
		if (file == null) {
			return;
		}
		File parent = file.getParentFile();
		if (parent == null) {
			return;
		}
		String path = parent.getPath();
		if (ENSURED_DIRS.contains(path)) {
			return;
		}
		try {
			if (!parent.exists()) {
				parent.mkdirs();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonJSONUtil.java:607");
		}
		ENSURED_DIRS.add(path);
	}

    /**
     * Menentukan path file JSON sementara untuk kombinasi (kelas, id, kunci logis) tanpa memerlukan
     * instance objek — berguna saat pemanggil hanya memiliki id, bukan entitas penuh. Path dibentuk
     * dari {@link ConstantValues#ambilLokasiFileTemprorary(Class)} + nama kelas sederhana + id +
     * kunci + ekstensi {@code .json}. Direktori induk dipastikan ada lewat {@link
     * #ensureParentDir(File)} sebelum {@link File} dikembalikan.
     *
     * @param clazz kelas entitas
     * @param id    pengenal entitas (id numerik, userId, roleId, dst. — diterima sebagai
     *              {@link Serializable} generik)
     * @param key   kunci logis data (nama "slot" JSON sementara ini)
     * @return objek {@link File} pada path yang ditentukan (belum tentu berisi data — direktori
     *         induknya sudah dipastikan ada, tetapi berkasnya sendiri mungkin belum ada)
     */
    @SuppressWarnings("rawtypes")
    public static File getFileLocation(Class clazz, Serializable id, String key) {
        String lokasiFileTemprorary = ConstantValues.ambilLokasiFileTemprorary(clazz);
        // Clean Logic: ID Tbmuser/Tbmrole logic sama dengan default
        String fileLocation = lokasiFileTemprorary + clazz.getSimpleName() + "/" + id + "/" + key + ".json";
        
        File file = new File(fileLocation);
        ensureParentDir(file);
        return file;
    }

    /**
     * Menentukan path file JSON sementara untuk entitas {@code obj} dan kunci logis {@code key}.
     * Skema penamaan default adalah {@code <lokasi>/<NamaKelas>/<id>/<key>.json}, dengan pengecualian
     * khusus per kelas: {@link Tbmuser} memakai {@code userId} (bukan {@code id}, karena primary key
     * aslinya {@code userId}), {@link Tbmrole} memakai {@code roleId}, dan {@link Perkuliahan}
     * memakai hierarki path {@code <fakultasId>/<jurusanId>/<tahunAjaran>/<semester>/<key>.json}
     * (id fakultas/jurusan jatuh ke {@code 0} bila relasi jurusan/fakultas belum diisi). Direktori
     * induk dipastikan ada lewat {@link #ensureParentDir(File)} sebelum {@link File} dikembalikan.
     *
     * @param obj entitas sumber path; bila {@code null}, method mengembalikan {@code null}
     * @param key kunci logis data
     * @return objek {@link File} pada path yang ditentukan, atau {@code null} bila {@code obj}
     *         {@code null}
     */
    @SuppressWarnings("rawtypes")
    public static File getFileLocation(GeneralValueObject obj, String key) {
        if (obj == null) return null;
        Class clazz = obj.getClass();
        String lokasi = ConstantValues.ambilLokasiFileTemprorary(clazz);
        
        // Default Logic
        String suffix = "/" + obj.getId() + "/" + key + ".json";
        
        // Custom Override Logic (Sesuai kode legacy)
        if (obj instanceof Tbmuser) {
             suffix = "/" + ((Tbmuser) obj).getUserId() + "/" + key + ".json";
        } else if (obj instanceof Tbmrole) {
             suffix = "/" + ((Tbmrole) obj).getRoleId() + "/" + key + ".json";
        } else if (obj instanceof Perkuliahan) {
             Perkuliahan p = (Perkuliahan) obj;
             long fakId = (p.getJurusan() != null && p.getJurusan().getFakultas() != null) ? p.getJurusan().getFakultas().getId() : 0;
             long jurId = (p.getJurusan() != null) ? p.getJurusan().getId() : 0;
             suffix = "/" + fakId + "/" + jurId + "/" + p.getTahunAjaran() + "/" + p.getSemester() + "/" + key + ".json";
        }
        // Tambahkan else if untuk class lain seperti DetailPerkuliahan jika diperlukan...

        File file = new File(lokasi + clazz.getSimpleName() + suffix);
        ensureParentDir(file);
        return file;
    }

    /**
     * Menulis {@code jsonObject} (setelah disisipi properti {@code "class"} berisi nama kelas
     * {@code obj}, agar dapat dideserialisasi kembali secara polymorphic lewat
     * {@link #convertToObject(JSONObject)}) ke lokasi file sementara yang ditentukan
     * {@link #getFileLocation(GeneralValueObject, String)}. Dipakai misalnya untuk menyimpan draft
     * data yang belum final ke disk sebelum benar-benar disimpan ke database.
     *
     * @param obj        entitas pemilik data, menentukan path file
     * @param key        kunci logis data
     * @param jsonObject data yang akan ditulis
     * @return {@link File} yang berhasil ditulis, atau {@code null} bila salah satu parameter
     *         {@code null}, path tidak dapat ditentukan, atau penulisan gagal (dicatat ke audit dan
     *         konsol)
     */
    public static File setJSONTemporary(GeneralValueObject obj, String key, JSONObject jsonObject) {
        if (obj == null || jsonObject == null || key == null) return null;

        try {
            jsonObject.put("class", obj.getClass().getName());
            File file = CommonJSONUtil.getFileLocation(obj, key);
            if (file != null) {
                ais.common.BacaTulisUtil.tulis(file, jsonObject.toString());
                return file;
            }
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonJSONUtil.java:661");
            System.err.println("Error saving JSON temp: " + obj.getClass().getSimpleName());
        }
        return null;
    }

    /**
     * Membaca kembali data JSON sementara yang sebelumnya ditulis
     * {@link #setJSONTemporary(GeneralValueObject, String, JSONObject)} untuk entitas dan kunci yang
     * sama.
     *
     * @param obj entitas pemilik data, menentukan path file yang dibaca
     * @param key kunci logis data
     * @return {@link JSONObject} hasil parsing isi berkas, atau {@link JSONObject} KOSONG (bukan
     *         {@code null}) bila salah satu parameter {@code null}, berkas tidak ada/kosong, atau
     *         gagal dibaca/diparsing (dicatat ke audit)
     */
    public static JSONObject getJSONTemporary(GeneralValueObject obj, String key) {
        if (obj == null || key == null) return new JSONObject();

        try {
            File file = CommonJSONUtil.getFileLocation(obj, key);
            if (file != null && file.exists()) {
                String content = ais.common.BacaTulisUtil.baca(file);
                if (content != null && !content.trim().isEmpty()) {
                    return new JSONObject(content);
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonJSONUtil.java:679");
        }
        return new JSONObject();
    }

    /**
     * Pola pengenal URL di dalam teks bebas: mulai dari {@code "http://"}, {@code "https://"}, atau
     * {@code "www."} dan berhenti di karakter yang jelas BUKAN bagian URL — whitespace, kutip
     * ganda/tunggal, dan tanda kurung siku HTML ({@code < >}) — supaya fragmen HTML mentah yang
     * ikut tertangkap (mis. {@code href="https://...">Klik}) tidak ikut terbawa sebagai satu "URL".
     */
    // Pola pengenal URL di dalam teks bebas: mulai dari "http://", "https://", atau "www."
    // dan berhenti di karakter yang jelas BUKAN bagian URL -- whitespace, kutip ganda/tunggal,
    // dan tanda kurung siku HTML (< >) -- supaya fragmen HTML mentah yang tertangkap
    // (mis. href="https://...">Klik) tidak ikut terbawa sebagai satu "URL".
    private static final java.util.regex.Pattern URL_TOKEN_PATTERN = java.util.regex.Pattern.compile(
            "(?i)(https?://[^\\s\"'<>]+|www\\.[^\\s\"'<>]+)");

    /**
     * Mengekstrak seluruh kandidat URL dari teks bebas {@code s} memakai {@link #URL_TOKEN_PATTERN}.
     * Untuk setiap kecocokan pola: tanda baca penutup yang mungkin ikut tertangkap di ujung URL
     * (titik/koma akhir kalimat, kurung tutup dari teks sekitarnya) dibuang; kasus khusus tautan
     * Google Drive ({@code drive.google.com}) memiliki koma dalam URL-nya sendiri diganti underscore
     * agar tidak salah terpotong oleh pembersihan tanda baca; domain tanpa skema (mis.
     * {@code www.4shared.com}) otomatis diberi awalan {@code https://} agar tetap terdeteksi valid
     * dan dapat diklik. Setiap kandidat divalidasi lewat konstruksi {@link URL} — kandidat yang
     * gagal divalidasi dilewati (dicatat ke audit) tanpa menggagalkan ekstraksi kandidat lain dalam
     * teks yang sama.
     *
     * @param s teks bebas yang akan diperiksa, boleh {@code null}
     * @return daftar URL valid yang ditemukan (sudah dilengkapi skema {@code https://} bila
     *         awalnya tidak ada), tidak pernah {@code null}; list kosong bila {@code s}
     *         {@code null} atau tidak ada URL yang cocok/valid
     */
    public static List<String> getUrls(String s) {
        List<String> urls = new ArrayList<String>();
        if (s == null) return urls;

        java.util.regex.Matcher matcher = URL_TOKEN_PATTERN.matcher(s);
        while (matcher.find()) {
            String item = matcher.group();
            // Buang tanda baca penutup yang mungkin ikut tertangkap di ujung URL
            // (mis. titik/koma akhir kalimat, atau kurung tutup dari teks sekitarnya)
            item = item.replaceAll("[)\\]\\.,;:]+$", "");
            if (item.isEmpty()) {
                continue;
            }
            // Setiap kandidat diproses dalam try-catch tersendiri (per-item) sehingga satu
            // kandidat URL yang gagal di-parse TIDAK menggagalkan ekstraksi URL lain dalam
            // teks yang sama.
            try {
                if (item.contains("drive.google.com")) {
                    item = item.replace(",", "_");
                }
                String itemLower = item.toLowerCase();
                String candidate = item;
                if (!itemLower.startsWith("http://") && !itemLower.startsWith("https://")) {
                    // Domain tanpa protokol (mis. "www.4shared.com") -- tambahkan "https://" secara
                    // otomatis supaya link tetap terdeteksi valid dan bisa diklik, bukan dilewati.
                    candidate = "https://" + item;
                }
                new URL(candidate);
                urls.add(candidate);
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonJSONUtil.java:696"); /* Not a URL */ }
        }
        return urls;
    }
}