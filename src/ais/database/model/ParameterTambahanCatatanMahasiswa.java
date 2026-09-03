package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

/**
 * Entity <b>penghubung</b> antara definisi field kustom generik dan kategori tampilnya pada modul
 * <b>Catatan Mahasiswa</b>, dipetakan ke tabel
 * {@code public.parameter_tambahan_catatan_mahasiswa}.
 *
 * <h3>Posisi dalam rantai field kustom Catatan Mahasiswa</h3>
 * <p>{@link CatatanMahasiswa} adalah pencatatan kejadian yang menempel pada seorang mahasiswa
 * (pelanggaran, prestasi, konseling/BK, peringatan, dan sejenisnya). Karena isi catatan berbeda
 * antar perguruan tinggi &mdash; bahkan antar jenis catatan di satu perguruan tinggi &mdash; AIS
 * tidak menambah kolom baru untuk setiap kebutuhan, melainkan memakai rantai "parameter tambahan"
 * <b>empat</b> lapis:</p>
 * <ol>
 *   <li>{@link ParameterTambahan} &mdash; <b>definisi</b> field kustom generik: label
 *   ({@code labelInputan}), tipe input ({@code tipeDataInputan}), daftar pilihan
 *   ({@code nilaiDataInputan}), wajib/tidak, perlu lampiran/tidak, nomor urut, dan induk
 *   ({@code parent}) untuk field bersarang. Tabel ini dipakai bersama oleh banyak modul (mahasiswa,
 *   alumni, calon mahasiswa, catatan pegawai, alur SOP, angket, &hellip;) &mdash; ia tidak
 *   tahu-menahu soal catatan mahasiswa.</li>
 *   <li><b>Kelas ini</b> &mdash; baris penghubung yang <b>mengadopsi</b> satu
 *   {@link ParameterTambahan} ke dalam satu
 *   {@link KelompokParameterTambahanCatatanMahasiswa}. Satu baris di sini = satu field yang muncul
 *   di bawah heading kelompok yang bersangkutan.</li>
 *   <li>{@link KelompokParameterTambahanCatatanMahasiswa} &mdash; <b>kategori/judul kelompok</b>;
 *   satu baris di sana menjadi satu heading seksi pada formulir isian catatan.</li>
 *   <li>{@link JenisCatatanMahasiswa} &mdash; lapisan tambahan yang TIDAK ada pada varian
 *   Alumni/Mahasiswa/Calon Mahasiswa: relasi {@code @ManyToMany} (tabel
 *   {@code jenis_catatan_mahasiswa_has_parameter}) yang menentukan kelompok mana saja yang muncul
 *   untuk sebuah jenis catatan. Formulir isian karena itu <b>bervariasi per jenis catatan</b>,
 *   bukan seragam satu form seperti pada modul biodata.</li>
 * </ol>
 * <p>Karena adopsi terjadi di lapis ini, satu {@link ParameterTambahan} yang sama bisa dipakai
 * berkali-kali (di kelompok berbeda) tanpa digandakan definisinya.</p>
 *
 * <h3>Anggota keluarga penghubung yang paling ramping</h3>
 * <p>Dibandingkan saudara-saudaranya, kelas ini hanya memiliki <b>dua relasi + satu cache nomor
 * urut</b>. Yang <b>TIDAK ADA</b> di sini padahal ada di varian lain:</p>
 * <ul>
 *   <li>tidak ada penimpa {@code wajibDiisi} per-adopsi (bandingkan
 *   {@link ParameterTambahanAlumni#getWajibDiisi()} dan
 *   {@link ParameterTambahanMahasiswa}). Validator
 *   {@code ParameterTambahanCatatanMahasiswaListener.validate()} membaca
 *   {@code parameterTambahan.getWajibDiisi()} <b>langsung dari definisinya</b>, sehingga sebuah
 *   field otomatis wajib/opsional di SEMUA kelompok sekaligus &mdash; tidak bisa dibedakan
 *   per-kelompok seperti pada modul Alumni;</li>
 *   <li>tidak ada field cakupan {@code fakultas}/{@code jurusan}/{@code program}/{@code jenjang}
 *   (pada varian Alumni/Mahasiswa keempatnya tersimpan tetapi terbukti tidak pernah dibaca
 *   runtime);</li>
 *   <li>tidak ada penyaring tahun angkatan
 *   ({@code tampilDiSemuaTahunAngkatan}/{@code tahunAngkatans}). Penyaringan di modul ini memang
 *   dikerjakan lapis keempat ({@link JenisCatatanMahasiswa}), bukan oleh kolom di baris ini.</li>
 * </ul>
 * <p>Konsekuensinya: baris entity ini praktis hanya <b>pasangan (kelompok, parameter)</b> ditambah
 * metadata audit.</p>
 *
 * <h3>Di mana isian pengguna sesungguhnya disimpan (TERVERIFIKASI dari kode)</h3>
 * <p>Entity ini murni <b>konfigurasi</b>; tidak ada satu pun kolom nilai di sini. Jawaban disimpan
 * sebagai <b>string terserialisasi</b> pada DUA kolom {@code text} di entity pemilik data
 * {@link CatatanMahasiswa}, keduanya ditulis sekaligus oleh
 * {@link CatatanMahasiswa#populateParameterTambahan(java.util.List)}:</p>
 * <ol>
 *   <li>{@link CatatanMahasiswa#getParameterTambahan()} &mdash; <b>versi berlabel</b>, baris dipisah
 *   {@code "\n"} dan ruas dipisah {@code "<=>"}, <b>7 ruas</b> per baris:
 *   <pre>
 *   namaKelompok "-&gt;" labelInputan &lt;=&gt; nilai &lt;=&gt; urlLampiran &lt;=&gt; nomorUrut
 *       &lt;=&gt; idParameterTambahan &lt;=&gt; idKelompok &lt;=&gt; keterangan
 *   </pre>
 *   dibongkar kembali oleh {@link CatatanMahasiswa#ambilDataParameterTambahan()} menjadi
 *   {@code List&lt;CommonVO&gt;} untuk layar tampil/laporan;</li>
 *   <li>{@link CatatanMahasiswa#getParameterTambahanInds()} &mdash; <b>versi ber-ID</b>, <b>4
 *   ruas</b> per baris:
 *   <pre>
 *   idKelompok "-&gt;" idParameterTambahan &lt;=&gt; nilai &lt;=&gt; urlLampiran &lt;=&gt; keterangan
 *   </pre>
 *   inilah yang dipakai mesin saat merakit ulang formulir: ketiga pembaca runtime memindai baris
 *   demi baris dan mencocokkan ruas pertama dengan kunci {@code jenis}.</li>
 * </ol>
 * <p><b>Beda nyata dari varian Alumni/Mahasiswa</b> (jangan disamakan): versi berlabel di modul
 * <i>ini</i> hanya <b>7 ruas</b> &mdash; tidak ada ruas {@code indexKe} yang ada pada
 * {@code BiodataMahasiswa.populateParameterTambahanAlumni(...)} (8 ruas). Versi ber-ID sama-sama
 * 4 ruas. Pemisah baris/ruas ({@code "\n"} dan {@code "<=>"}) identik.</p>
 * <p><b>Kunci penyimpanan.</b> Jawaban dialamatkan oleh pasangan
 * <code>idKelompok + "-&gt;" + idParameterTambahan</code> (variabel {@code jenis} di kode
 * pemanggil). String yang sama juga dipakai sebagai penanda {@code jenis} pada
 * {@link ais.database.model.file.LampiranLain#ambil(Long, String)} dengan pemilik
 * {@code catatanMahasiswa.getId()}, untuk mengambil berkas lampiran field tersebut.
 * <b>ID baris entity ini sendiri tidak pernah ikut disimpan.</b> Akibatnya:</p>
 * <ul>
 *   <li>menghapus lalu membuat ulang baris penghubung ini <b>tidak</b> memutus isian lama, selama
 *   pasangan kelompok+parameter yang sama dipakai lagi;</li>
 *   <li>sebaliknya, <b>memindahkan</b> sebuah parameter ke kelompok lain mengubah kunci, sehingga
 *   seluruh isian historis menjadi yatim di dalam kolom {@code text} &mdash; tetap tersimpan,
 *   tetapi tidak pernah terbaca lagi oleh formulir, laporan, maupun validator;</li>
 *   <li>karena penyimpanannya string, tidak ada foreign key: menghapus {@link ParameterTambahan}
 *   atau kategori tidak membersihkan isian lama maupun lampirannya.</li>
 * </ul>
 *
 * <h3>Siapa yang memakai baris-baris ini (tiga pembaca runtime, query identik)</h3>
 * <ul>
 *   <li><b>Layar admin</b> {@code ais.action.master.ParameterTambahanCatatanMahasiswaAction} (CRUD
 *   pasangan kelompok&harr;parameter). {@code doAfterCompose()} layar tersebut memanggil
 *   {@link KelompokParameterTambahanCatatanMahasiswa#checkCreateDefault()} &mdash; layar milik
 *   entity INILAH satu-satunya pemicu auto-seed kategori bawaan pada rantai ini.</li>
 *   <li><b>Perakit formulir</b>
 *   {@link ais.action.master.helper.ParameterTambahanCatatanMahasiswaListener} (dipakai
 *   {@code ais.action.master.CatatanMahasiswaAction} saat mengisi/mengubah catatan).</li>
 *   <li><b>Tampilan ringkas</b> {@code CatatanMahasiswaAction} (panel detail read-only).</li>
 *   <li><b>Laporan</b> {@code ais.action.report.format1.akademik.LaporanCatatanMahasiswa}.</li>
 * </ul>
 * <p>Ketiga pembaca runtime memakai query yang <b>sama persis</b>:</p>
 * <pre>
 * session.createCriteria(ParameterTambahanCatatanMahasiswa.class)
 *     .add(Restrictions.eq("kelompokParameterTambahanCatatanMahasiswa", kelompok))
 *     .createAlias("parameterTambahan", "parameterTambahan")
 *     .createAlias("kelompokParameterTambahanCatatanMahasiswa", "kelompokParameterTambahanCatatanMahasiswa")
 *     .add(Restrictions.eq("parameterTambahan.aktif", true))
 *     .add(Restrictions.eq("kelompokParameterTambahanCatatanMahasiswa.aktif", true))
 *     .setProjection(Projections.groupProperty("parameterTambahan.id"))
 * </pre>
 * <p>Perhatikan proyeksinya: hasil query <b>bukan</b> objek kelas ini, melainkan daftar
 * {@link ParameterTambahan} (id-nya di-{@code groupProperty}, lalu dimuat ulang oleh
 * {@code ConstantValues.simpleList(...)}). Dua konsekuensi penting dibahas di
 * {@link #getNomorUrut()}: baris entity ini hanya berfungsi sebagai <b>syarat keberadaan</b>, dan
 * seluruh atributnya sendiri tidak pernah ikut terbaca di jalur tampil.</p>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 *   <li><b>Identitas &amp; audit:</b> {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 *   <li><b>Relasi rantai:</b> {@link #getParameterTambahan()} (definisi field),
 *   {@link #getKelompokParameterTambahanCatatanMahasiswa()} (kategori/heading).</li>
 *   <li><b>Pengurutan/denormalisasi:</b> {@link #getNomorUrut()} (salinan dari definisi; lihat
 *   catatan "kode praktis mati" pada dokumentasinya).</li>
 *   <li><b>Konstruktor:</b> {@link #ParameterTambahanCatatanMahasiswa()}.</li>
 * </ul>
 *
 * <h3>Catatan warisan &amp; pemetaan (non-obvious)</h3>
 * <ul>
 *   <li>{@link GeneralValueObject} BUKAN {@code @Entity} maupun {@code @MappedSuperclass} &mdash; ia
 *   POJO abstrak biasa, sehingga Hibernate <b>tidak memetakan satu pun property induknya</b>. Karena
 *   itu deklarasi ULANG {@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah}, dan
 *   {@code nomorUrut} di kelas ini <b>bukan bug</b>, melainkan keharusan teknis agar kolom-kolom itu
 *   benar-benar tersimpan. Field induk yang tidak dideklarasikan ulang &mdash; {@code nama},
 *   {@code nim}, {@code keterangan} &mdash; tetap ada di memori tetapi tidak pernah terisi.</li>
 *   <li><b>Kelas ini TIDAK memiliki field {@code keterangan} sendiri.</b> {@code getKeterangan()},
 *   {@code getNama()}, {@code getNim()}, dan {@code toString()} sepenuhnya diwarisi dari
 *   {@link GeneralValueObject} dan selalu bernilai bawaan. (Pola "getKeterangan() membalik kontrak
 *   base class" karena itu <b>TIDAK ADA</b> di file ini.)</li>
 *   <li>Anotasi {@code @Id} berada pada <b>getter</b>, sehingga Hibernate memakai <i>property
 *   access</i> untuk seluruh property. Digabung dengan {@code dynamicUpdate = true}, getter yang
 *   menulis balik ke field ({@link #getNomorUrut()}, dan {@code check(...)} pada kedua getter
 *   relasi) dapat mengotori state dan memicu {@code UPDATE} beserta revisi Envers baru pada baris
 *   yang <b>sekadar dibaca</b>.</li>
 *   <li>{@code nomorUrut} tidak punya {@code @Column} eksplisit sehingga nama kolomnya mengikuti
 *   strategi penamaan bawaan Hibernate (nama property apa adanya, dilipat ke huruf kecil oleh
 *   PostgreSQL).</li>
 *   <li><b>Nama kolom FK sisa template SOP.</b> Relasi ke kategori dipetakan ke kolom
 *   {@code kelompok_parameter_tambahan_alur_sop} &mdash; nama yang jelas berasal dari modul
 *   {@code sop} ({@link ais.database.model.sop.ParameterTambahanAlurSop}) dan ikut tersalin ke
 *   seluruh varian "Catatan*" (Catatan Pegawai, Catatan Administrasi, Catatan Guru, Catatan Siswa,
 *   Catatan Kelas Siswa). Ini <b>menyesatkan tetapi tidak merusak</b>: kolomnya milik tabel ini
 *   sendiri, jadi pemetaannya tetap benar. Dicatat agar tidak dikira salah relasi saat membaca
 *   skema DB mentah.</li>
 *   <li>Berbeda dari varian Alumni/Mahasiswa yang memberi {@code nullable = false}, relasi kategori
 *   di sini {@code nullable = true}. Praktisnya tidak ada bedanya karena SELURUH pembaca runtime
 *   menyaring dengan {@code Restrictions.eq("kelompokParameterTambahanCatatanMahasiswa", ...)},
 *   sehingga baris tanpa kategori tidak akan pernah cocok dengan kelompok mana pun &mdash; ia hanya
 *   menjadi baris yatim yang tetap tampil di grid admin.</li>
 *   <li>Kedua relasi {@code @ManyToOne} memakai {@code cascade = {PERSIST, MERGE}}, jadi menyimpan
 *   baris ini bisa ikut mem-{@code persist}/{@code merge} master yang direferensikan.</li>
 *   <li>{@code @Audited} (Envers) aktif: setiap perubahan baris konfigurasi ini terekam di tabel
 *   audit, termasuk {@code UPDATE} tak sengaja dari getter penulis-balik di atas.</li>
 * </ul>
 *
 * <h3>Catatan keamanan pada layar pengelolanya</h3>
 * <p>Entity ini sendiri tidak menegakkan otorisasi apa pun (itu tugas lapisan Action), tetapi
 * pengelolanya {@code ais.action.master.ParameterTambahanCatatanMahasiswaAction} <b>tidak memiliki
 * gerbang hak akses sama sekali</b>: field {@code edit} dan {@code delete} di-hardcode {@code true}
 * dan tidak ada satu pun pemanggilan {@code checkPrevilages} di seluruh file, termasuk di
 * {@code doAfterCompose}. Siapa pun yang bisa membuka layar tersebut dapat menambah, mengubah, dan
 * menghapus pemetaan parameter &mdash; yang berarti dapat mengubah/menghilangkan field pada
 * formulir Catatan Mahasiswa untuk semua pengguna. Pola yang sama ditemukan pada seluruh keluarga
 * {@code ParameterTambahan*Action} yang sudah diperiksa; sudah dicatat sebagai temuan terpisah dan
 * <b>tidak diperbaiki di sini</b> (dokumentasi ini tidak mengubah logika kode apa pun).</p>
 *
 * @see ParameterTambahan
 * @see KelompokParameterTambahanCatatanMahasiswa
 * @see JenisCatatanMahasiswa
 * @see CatatanMahasiswa#populateParameterTambahan(java.util.List)
 * @see ais.action.master.helper.ParameterTambahanCatatanMahasiswaListener
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "parameter_tambahan_catatan_mahasiswa")
public class ParameterTambahanCatatanMahasiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Entity ini ikut diserialisasi karena disimpan sebagai atribut komponen
	 * ZK dan sebagai nilai item {@code Combobox} pada layar admin.
	 *
	 * <p>Nilainya <b>sama persis</b> dengan {@link ParameterTambahanAlumni} dan saudara-saudaranya
	 * &mdash; sisa salin-tempel template hbm2java. Tidak berbahaya karena {@code serialVersionUID}
	 * hanya dibandingkan antar versi kelas yang sama.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key {@code IDENTITY}; dideklarasikan ulang karena induk tidak dipetakan Hibernate. */
	private Long id;
	/** Nama pengguna pengubah terakhir; diisi otomatis oleh {@link #onUpdate()}. */
	private String oleh;
	/** ID pengguna pengubah terakhir; diisi otomatis oleh {@link #onUpdate()}. */
	private String olehId;

	/**
	 * Mengembalikan ID pengguna terakhir yang mengubah baris ini.
	 *
	 * @return ID pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pengguna pengubah terakhir.
	 *
	 * <p><b>Kuirk:</b> nilai {@code null}/kosong/hanya spasi diabaikan diam-diam (method langsung
	 * {@code return}), sehingga nilai lama bertahan dan jejak audit tidak pernah bisa
	 * dikosongkan.</p>
	 *
	 * @param olehId ID pengguna pengubah; {@code null}/kosong/spasi diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p><b>Kuirk:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong/spasi
	 * diabaikan diam-diam sehingga nilai lama bertahan.</p>
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/kosong/spasi diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil Hibernate TEPAT SEBELUM setiap {@code UPDATE} baris
	 * ini, lalu meneruskan ke {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}
	 * yang mengisi {@link #setOleh(String)}, {@link #setOlehId(String)}, dan
	 * {@link #setTanggal_dirubah(Date)} dari konteks pengguna yang sedang login.
	 *
	 * <p><b>Efek samping:</b> memodifikasi state objek di tengah siklus flush. Jangan dipanggil
	 * manual dari kode aplikasi &mdash; Hibernate yang memicunya.</p>
	 *
	 * <p>Hanya {@code @PreUpdate} yang dipasang; baris BARU ({@code INSERT}) tidak melewati callback
	 * ini. Baris hasil {@code onAdd()}/{@code onSave()} layar admin karena itu masuk <b>tanpa
	 * jejak</b> {@code oleh}/{@code olehId} sampai pertama kali diubah.</p>
	 *
	 * <p>Perlu diingat bahwa {@link #getNomorUrut()} dapat mengotori field saat baris sekadar
	 * dibaca, sehingga callback ini bisa ikut terpicu pada {@code UPDATE} yang <b>tidak diminta
	 * pengguna mana pun</b> &mdash; jejak audit lalu mencatat pengguna yang kebetulan sedang membuka
	 * layar.</p>
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja berada pada baris fisik yang sama dalam
	 * kode aslinya; nilainya diinisialisasi memakai jam aplikasi
	 * ({@code ais.ui.util.WaktuUtil.getDate()}), bukan {@code new Date()}, agar konsisten dengan
	 * zona waktu/penyetelan waktu server yang dipakai seluruh modul.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir baris ini.
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null} (tidak ada penambalan di sini)
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini.
	 *
	 * <p>Tidak pernah {@code null} untuk objek baru karena field-nya diinisialisasi saat deklarasi
	 * (lihat {@link #onUpdate()}).</p>
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Kategori/heading tempat field ini ditampilkan pada formulir catatan mahasiswa; dipetakan ke
	 * kolom {@code kelompok_parameter_tambahan_alur_sop} (lihat catatan nama kolom pada dokumentasi
	 * kelas).
	 */
	private KelompokParameterTambahanCatatanMahasiswa kelompokParameterTambahanCatatanMahasiswa;
	/** Definisi field kustom yang diadopsi baris ini; wajib, kolom {@code parameter_tambahan}. */
	private ParameterTambahan parameterTambahan;

	/**
	 * Cache nomor urut tampil; nilainya <b>selalu ditimpa</b> dari {@link ParameterTambahan} setiap
	 * kali {@link #getNomorUrut()} dipanggil.
	 */
	private Integer nomorUrut;

	/**
	 * Mengembalikan nomor urut tampil field ini, <b>diturunkan dari definisinya</b>
	 * ({@link ParameterTambahan#getNomorUrut()}).
	 *
	 * <p>Meng-override {@code getNomorUrut()} milik {@link GeneralValueObject} dan secara teori
	 * menjadi satu-satunya kunci urut yang efektif bagi entity ini: {@code compareTo} induk mencoba
	 * berturut-turut {@code nomorUrut} &rarr; {@code nim} &rarr; {@code nama} &rarr;
	 * {@code keterangan}, dan tiga kunci terakhir selalu bernilai bawaan di sini karena tidak
	 * dipetakan.</p>
	 *
	 * <p><b>Kenyataannya nyaris tidak terpakai (terverifikasi).</b> Ketiga pembaca runtime
	 * (perakit formulir, panel detail, dan laporan) memakai query yang di-{@code
	 * setProjection(Projections.groupProperty("parameterTambahan.id"))}, sehingga yang mereka
	 * urutkan dengan {@code Collections.sort(...)} adalah daftar {@link ParameterTambahan}
	 * &mdash; bukan objek kelas ini. Urutan field pada formulir karena itu ditentukan
	 * {@code ParameterTambahan.getNomorUrut()} secara langsung, dan nilai kolom {@code nomorUrut}
	 * pada tabel ini <b>tidak pernah dibaca oleh jalur tampil mana pun</b>. Satu-satunya pemakai
	 * yang tersisa adalah jalur reflektif ekspor/impor Excel ({@code Common.cetakData(...)} dan
	 * {@code Common.uploadData(...)} pada layar admin, yang mencantumkan {@code "nomorUrut"} di
	 * daftar kolomnya).</p>
	 *
	 * <p><b>Efek samping (penting).</b> Method ini bukan pembaca murni:</p>
	 * <ul>
	 *   <li>menugaskan ulang field {@code parameterTambahan} dengan hasil
	 *   {@link #getParameterTambahan()} &mdash; artinya proxy lazy ikut diresolusi
	 *   ({@link GeneralValueObject#check(Object)}) dan dapat memicu query;</li>
	 *   <li>menimpa field {@code nomorUrut} dengan nilai dari definisi. Karena Hibernate memakai
	 *   property access dan {@code dynamicUpdate = true}, kolom {@code nomorUrut} baris ini bisa
	 *   ter-{@code UPDATE} (beserta revisi Envers baru) hanya karena baris dibaca dalam sesi aktif.
	 *   Praktisnya kolom itu adalah <b>salinan denormalisasi</b> yang tidak pernah boleh berbeda
	 *   dari induknya &mdash; {@link #setNomorUrut(Integer)} tidak punya pengaruh yang bertahan.</li>
	 * </ul>
	 *
	 * @return nomor urut tampil; {@code 1} bila definisi maupun cache belum punya nilai, tidak
	 *         pernah {@code null}
	 */
	public Integer getNomorUrut() {
		parameterTambahan = getParameterTambahan();
		if (parameterTambahan != null) {
			nomorUrut = parameterTambahan.getNomorUrut();
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menyetel cache nomor urut tampil.
	 *
	 * <p>Praktis tidak berguna: {@link #getNomorUrut()} menimpanya lagi dari
	 * {@link ParameterTambahan} pada pembacaan berikutnya. Disediakan karena dibutuhkan Hibernate
	 * (property access) dan jalur impor data reflektif {@code Common.uploadData(...)}.</p>
	 *
	 * @param nomorUrut nomor urut tampil
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate; dipakai juga oleh
	 * {@code ParameterTambahanCatatanMahasiswaAction} pada jalur {@code onAdd()}/{@code onSave()}
	 * saat sebuah parameter dikaitkan ke sebuah kelompok.
	 */
	public ParameterTambahanCatatanMahasiswa() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * <p>{@code insertable = false} karena nilainya dihasilkan database ({@code IDENTITY}).
	 * Perhatikan bahwa ID ini <b>tidak pernah ikut tersimpan</b> pada string isian di
	 * {@link CatatanMahasiswa}; kunci yang dipakai adalah pasangan
	 * <code>idKelompok + "-&gt;" + idParameterTambahan</code> &mdash; lihat "Kunci penyimpanan" pada
	 * dokumentasi kelas.</p>
	 *
	 * @return ID baris; {@code null} untuk objek yang belum disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key baris ini; normalnya hanya dilakukan Hibernate.
	 *
	 * @param id ID baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan <b>definisi</b> field kustom yang diadopsi baris ini &mdash; sumber label, tipe
	 * input, daftar pilihan, kewajiban isi, kewajiban lampiran, nomor urut, dan relasi
	 * {@code parent} untuk field bersarang.
	 *
	 * <p>Relasi wajib ({@code nullable = false}). Dipakai di hampir setiap pemakai entity ini:
	 * perakit formulir (yang memanggil {@code ParameterTambahan.initComponent(...)}), validator
	 * wajib-isi/lampiran-wajib, renderer grid admin (kolom Parameter, Lampiran, Tipe Data, Nilai
	 * Data semuanya dibaca dari sini), dan {@link #getNomorUrut()} di kelas ini sendiri.</p>
	 *
	 * <p><b>Efek samping ringan:</b> proxy lazy diresolusi lewat
	 * {@link GeneralValueObject#check(Object)} dan hasilnya ditulis balik ke field, agar pemanggil
	 * tidak menerima proxy yang meledak saat sesi sudah ditutup.</p>
	 *
	 * @return definisi field kustom; secara teori tidak pernah {@code null} untuk baris tersimpan,
	 *         namun pemanggil di codebase tetap memeriksanya
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "parameter_tambahan", nullable = false)
	public ParameterTambahan getParameterTambahan() {
		parameterTambahan = check(parameterTambahan);
		return parameterTambahan;
	}

	/**
	 * Menyetel definisi field kustom yang diadopsi baris ini.
	 *
	 * <p>Dipanggil layar admin dari combobox "Parameter" pada dialog Tambah/Ubah.</p>
	 *
	 * <p><b>Perhatian:</b> mengubah nilai ini berarti mengubah kunci penyimpanan isian
	 * (<code>idKelompok + "-&gt;" + idParameterTambahan</code>), sehingga seluruh isian yang sudah
	 * tersimpan pada {@link CatatanMahasiswa} untuk pasangan lama menjadi yatim &mdash; termasuk
	 * lampirannya di {@link ais.database.model.file.LampiranLain}.</p>
	 *
	 * @param parameterTambahan definisi field kustom; wajib diisi sebelum baris disimpan
	 */
	public void setParameterTambahan(ParameterTambahan parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	/**
	 * Mengembalikan <b>kategori/heading</b> tempat field ini ditampilkan pada formulir catatan
	 * mahasiswa.
	 *
	 * <p>Sifat kategori inilah yang menentukan apakah field ikut tampil: seluruh query pembaca
	 * mensyaratkan {@code kelompokParameterTambahanCatatanMahasiswa.aktif = true}, dan kategori
	 * tersebut baru dilewati sama sekali bila sudah dipilih pada
	 * {@link JenisCatatanMahasiswa#getKelompokParameterTambahanCatatanMahasiswas()} untuk jenis
	 * catatan yang sedang dipakai (lapis keempat rantai).</p>
	 *
	 * <p>ID kategori ini juga menjadi <b>ruas pertama kunci penyimpanan isian</b>
	 * (<code>idKelompok + "-&gt;" + idParameterTambahan</code>) di {@link CatatanMahasiswa} dan
	 * penanda {@code jenis} pada {@link ais.database.model.file.LampiranLain}.</p>
	 *
	 * <p>Dipetakan ke kolom {@code kelompok_parameter_tambahan_alur_sop} &mdash; nama sisa template
	 * modul SOP, bukan salah relasi; lihat dokumentasi kelas. Dideklarasikan
	 * {@code nullable = true} (berbeda dari varian Alumni/Mahasiswa), tetapi baris tanpa kategori
	 * tidak akan pernah cocok dengan filter pembaca mana pun sehingga efektif menjadi baris
	 * yatim.</p>
	 *
	 * <p><b>Efek samping ringan:</b> proxy lazy diresolusi lewat
	 * {@link GeneralValueObject#check(Object)} dan hasilnya ditulis balik ke field.</p>
	 *
	 * @return kategori/heading pemilik field ini, atau {@code null} bila baris belum/tidak dikaitkan
	 *         ke kategori mana pun
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_parameter_tambahan_alur_sop", nullable = true)
	public KelompokParameterTambahanCatatanMahasiswa getKelompokParameterTambahanCatatanMahasiswa() {
		kelompokParameterTambahanCatatanMahasiswa = check(kelompokParameterTambahanCatatanMahasiswa);
		return kelompokParameterTambahanCatatanMahasiswa;
	}

	/**
	 * Menyetel kategori/heading pemilik field ini.
	 *
	 * <p>Dipanggil layar admin dari combobox "Kelompok" pada dialog Tambah/Ubah.</p>
	 *
	 * <p><b>Perhatian:</b> memindahkan baris ke kategori lain mengubah kunci penyimpanan isian,
	 * sehingga seluruh isian yang sudah tersimpan untuk pasangan lama tidak akan terbaca lagi
	 * (tetap ada di kolom {@code text} {@link CatatanMahasiswa}, tetapi yatim), dan lampiran lama
	 * tidak lagi ditemukan oleh {@link ais.database.model.file.LampiranLain#ambil(Long, String)}.</p>
	 *
	 * @param kelompokParameterTambahanCatatanMahasiswa kategori/heading pemilik field ini
	 */
	public void setKelompokParameterTambahanCatatanMahasiswa(
			KelompokParameterTambahanCatatanMahasiswa kelompokParameterTambahanCatatanMahasiswa) {
		this.kelompokParameterTambahanCatatanMahasiswa = kelompokParameterTambahanCatatanMahasiswa;
	}

}
