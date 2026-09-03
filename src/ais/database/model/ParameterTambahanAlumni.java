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
 * <b>Alumni</b> (tracer study), dipetakan ke tabel {@code public.parameter_tambahan_alumni}.
 *
 * <h3>Posisi dalam rantai field kustom Alumni</h3>
 * <p>AIS mengizinkan setiap perguruan tinggi menambah pertanyaan/isian sendiri pada biodata alumni
 * tanpa mengubah skema database. Rantainya tiga lapis:</p>
 * <ol>
 *   <li>{@link ParameterTambahan} &mdash; <b>definisi</b> field kustom generik: label, tipe input
 *   ({@code tipeDataInputan}), daftar pilihan ({@code nilaiDataInputan}), wajib/tidak, perlu
 *   lampiran/tidak, nomor urut, bahkan induk ({@code parent}) untuk field bersarang. Tabel ini
 *   dipakai bersama oleh banyak modul (mahasiswa, calon mahasiswa, catatan pegawai, angket, &hellip;)
 *   &mdash; ia tidak tahu-menahu soal alumni.</li>
 *   <li><b>Kelas ini</b> &mdash; baris penghubung yang <b>mengadopsi</b> satu
 *   {@link ParameterTambahan} ke dalam satu
 *   {@link KelompokParameterTambahanAlumni}, sekaligus membawa <b>penyaring cakupan</b>
 *   (tahun angkatan, fakultas, jurusan, program, jenjang) dan <b>penimpa</b>
 *   {@link #getWajibDiisi() wajibDiisi} khusus konteks alumni.</li>
 *   <li>{@link KelompokParameterTambahanAlumni} &mdash; <b>kategori/judul kelompok</b>; satu baris
 *   di sana menjadi satu heading seksi pada formulir isian alumni.</li>
 * </ol>
 * <p>Karena adopsi terjadi di lapis ini, satu {@link ParameterTambahan} yang sama bisa dipakai
 * berkali-kali (di kelompok berbeda, atau dengan cakupan angkatan berbeda) tanpa digandakan
 * definisinya.</p>
 *
 * <h3>Siapa yang memakai baris-baris ini</h3>
 * <ul>
 *   <li><b>Layar admin</b> {@code ais.action.master.ParameterTambahanAlumniAction} (CRUD).
 *   {@code doAfterCompose()} layar tersebut memanggil
 *   {@link KelompokParameterTambahanAlumni#checkCreateDefault()} &mdash; jadi layar milik entity
 *   INILAH pemicu utama auto-seed kategori bawaan pada rantai ini.</li>
 *   <li><b>Perakit formulir</b> {@code ais.action.master.helper.ParameterTambahanAlumniListener}
 *   ({@code check()}, {@code onEvent()}, {@code displayRinci()}, dan {@code validate()} statis) yang
 *   membangun baris-baris input ZK dan memvalidasi isian wajib. Dipakai antara lain oleh
 *   {@code ais.action.maintenance.LoginAlumniAction} (portal alumni),
 *   {@code ais.action.master.MahasiswaAction}, {@code ais.action.master.alumni.MahasiswaAction},
 *   {@code ais.action.master.BiodataMahasiswaAction}, dan
 *   {@code ais.action.master.PendaftaranWisudaMahasiswaAction}.</li>
 *   <li><b>Rekap dasbor</b> {@code ais.action.master.dashboard.helper.DashboardRekapParameterTambahanAlumni}.</li>
 * </ul>
 *
 * <h3>Mekanisme penyaring tahun angkatan (terverifikasi dari kode pemanggil)</h3>
 * <p>Ini ciri khas lapis penghubung ini &mdash; dua field bekerja berpasangan:</p>
 * <ul>
 *   <li>{@link #getTampilDiSemuaTahunAngkatan()} &mdash; bila {@code true} (nilai bawaan), baris
 *   berlaku untuk semua angkatan dan {@code tahunAngkatans} diabaikan;</li>
 *   <li>{@link #getTahunAngkatans()} &mdash; kolom {@code text} berisi daftar angkatan terpilih.</li>
 * </ul>
 * <p><b>Format {@code tahunAngkatans}.</b> Layar admin merangkainya dari checkbox per angkatan
 * dengan pola {@code ";" + tahun + ";"} yang disambung langsung tanpa pemisah tambahan, sehingga
 * pilihan 2019, 2020, 2021 tersimpan sebagai {@code ";2019;;2020;;2021;"} (titik-koma ganda di
 * antara angkatan &mdash; bukan salah ketik dokumentasi ini). Daftar angkatan yang bisa dicentang
 * TIDAK diambil dari konstanta {@code Common.tahunAngkatans}, melainkan dari nilai
 * {@code tahunangkatan} DISTINCT pada {@link Mahasiswa} aktif.</p>
 * <p><b>Cara dibaca.</b> Layar admin membacanya kembali dengan {@code split(";")} sambil melewati
 * potongan kosong. Seluruh pembaca runtime (empat query di
 * {@code ParameterTambahanAlumniListener}) memakai bentuk yang sama persis:</p>
 * <pre>
 * Restrictions.or(
 *     Restrictions.eq("tampilDiSemuaTahunAngkatan", true),
 *     gel == null ? Restrictions.sqlRestriction("false")
 *                 : Restrictions.ilike("tahunAngkatans", ";" + gel + ";", MatchMode.ANYWHERE))
 * </pre>
 * <p>dengan {@code gel = biodataMahasiswa.getMahasiswa().getTahunangkatan()}. Titik-koma pengapit
 * pada pola pencarian itulah yang mencegah salah cocok parsial (mis. {@code ";202;"} tidak cocok
 * dengan {@code ";2020;"}). Konsekuensi yang perlu diingat:</p>
 * <ul>
 *   <li>alumni yang <b>tidak diketahui angkatannya</b> ({@code gel == null}, mis. biodata tanpa
 *   relasi {@link Mahasiswa}) hanya melihat baris {@code tampilDiSemuaTahunAngkatan = true} &mdash;
 *   cabang lain sengaja dimatikan dengan {@code sqlRestriction("false")};</li>
 *   <li>baris dengan {@code tampilDiSemuaTahunAngkatan = false} tetapi {@code tahunAngkatans} kosong
 *   TIDAK PERNAH muncul untuk siapa pun (kondisi yang terjadi persis setelah admin mematikan
 *   centang "Semua" dan belum mencentang satu angkatan pun);</li>
 *   <li>query memakai {@code Restrictions.eq(..., true)} yang di SQL <b>tidak</b> mencakup
 *   {@code NULL}. Penambalan {@code null}&nbsp;&rarr;&nbsp;{@code true} pada
 *   {@link #getTampilDiSemuaTahunAngkatan()} hanya berlaku bagi objek yang dibaca lewat Java,
 *   bukan bagi baris yang pernah disisipkan lewat SQL mentah/migrasi.</li>
 * </ul>
 *
 * <h3>Di mana jawaban alumni sesungguhnya disimpan (terverifikasi)</h3>
 * <p>Entity ini murni <b>konfigurasi</b>; tidak ada satu pun kolom nilai di sini. Jawaban alumni
 * disimpan sebagai <b>string terserialisasi</b> pada DUA kolom {@code text} di
 * {@link BiodataMahasiswa}, ditulis oleh
 * {@link BiodataMahasiswa#populateParameterTambahanAlumni(java.util.List)}:</p>
 * <ol>
 *   <li>{@link BiodataMahasiswa#getParameterTambahanAlumni()} &mdash; <b>versi berlabel</b>, 8 ruas
 *   per baris, baris dipisah {@code "\n"} dan ruas dipisah {@code "<=>"}:
 *   <pre>
 *   namaKelompok "-&gt;" labelInputan &lt;=&gt; nilai &lt;=&gt; urlLampiran &lt;=&gt; nomorUrut
 *       &lt;=&gt; idParameterTambahan &lt;=&gt; idKelompok &lt;=&gt; indexKe &lt;=&gt; keterangan
 *   </pre>
 *   dibongkar kembali oleh {@link BiodataMahasiswa#ambilDataParameterTambahanAlumni()} untuk layar
 *   tampil dan laporan ({@code CommonReportHelper});</li>
 *   <li>{@link BiodataMahasiswa#getParameterTambahanIndsAlumni()} &mdash; <b>versi ber-ID</b>, 4 ruas
 *   per baris:
 *   <pre>
 *   idKelompok "-&gt;" idParameterTambahan &lt;=&gt; nilai &lt;=&gt; urlLampiran &lt;=&gt; keterangan
 *   </pre>
 *   inilah yang dipakai mesin: {@code ParameterTambahanAlumniListener.parseParameterMap()}
 *   memetakannya menjadi {@code Map<String,String[]>} dengan kunci ruas pertama di-{@code toLowerCase()}.</li>
 * </ol>
 * <p><b>Kunci penyimpanan.</b> Jawaban dialamatkan oleh pasangan
 * <code>idKelompok + "-&gt;" + idParameterTambahan</code>. String yang sama juga dipakai sebagai
 * penanda {@code jenis} pada
 * {@link ais.database.model.file.LampiranLain#ambil(Long, String)} untuk berkas lampiran.
 * <b>ID baris entity ini sendiri tidak pernah ikut disimpan.</b> Akibatnya:</p>
 * <ul>
 *   <li>menghapus lalu membuat ulang baris penghubung ini <b>tidak</b> memutus jawaban lama, selama
 *   pasangan kelompok+parameter yang sama dipakai lagi;</li>
 *   <li>sebaliknya, <b>memindahkan</b> sebuah parameter ke kelompok lain mengubah kunci, sehingga
 *   seluruh jawaban historis menjadi yatim di dalam kolom {@code text} &mdash; tetap tersimpan,
 *   tetapi tidak pernah terbaca lagi oleh formulir maupun validator;</li>
 *   <li>karena penyimpanannya string, tidak ada foreign key: menghapus {@link ParameterTambahan}
 *   atau kategori tidak membersihkan jawaban lama.</li>
 * </ul>
 *
 * <h3>Kuirk cakupan fakultas/jurusan/program/jenjang (terverifikasi)</h3>
 * <p>Empat field cakupan ({@link #getFakultas()}, {@link #getJurusan()}, {@link #getProgram()},
 * {@link #getJenjang()}) tampak seperti penyaring, tetapi <b>tidak satu pun pembaca runtime yang
 * memakainya</b>. Seluruh query di {@code ParameterTambahanAlumniListener} (perakit formulir,
 * penghitung, dan validator wajib-isi) maupun di {@code DashboardRekapParameterTambahanAlumni}
 * hanya menyaring dengan tahun angkatan, {@code parameterTambahan.aktif},
 * {@code kelompokParameterTambahanAlumni.aktif}, dan {@code digunakanUntukPenggunaAlumni}. Keempat
 * field ini hanya dipakai layar admin sebagai kolom pencarian/tampilan. Praktisnya: field yang
 * dibatasi ke satu fakultas tetap muncul dan tetap wajib diisi bagi alumni fakultas mana pun.
 * Dicatat apa adanya &mdash; tidak diperbaiki di sini.</p>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 *   <li><b>Identitas &amp; audit:</b> {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 *   <li><b>Relasi rantai:</b> {@link #getParameterTambahan()} (definisi field),
 *   {@link #getKelompokParameterTambahanAlumni()} (kategori/heading).</li>
 *   <li><b>Penyaring tahun angkatan:</b> {@link #getTampilDiSemuaTahunAngkatan()},
 *   {@link #getTahunAngkatans()}.</li>
 *   <li><b>Cakupan struktural (tidak dipakai runtime):</b> {@link #getFakultas()},
 *   {@link #getJurusan()}, {@link #getProgram()}, {@link #getJenjang()}.</li>
 *   <li><b>Penimpa perilaku:</b> {@link #getWajibDiisi()}.</li>
 *   <li><b>Pengurutan:</b> {@link #getNomorUrut()} (satu-satunya kunci urut efektif; lihat
 *   {@link GeneralValueObject#compareTo(GeneralValueObject)}).</li>
 * </ul>
 *
 * <h3>Catatan warisan &amp; pemetaan (non-obvious)</h3>
 * <ul>
 *   <li>{@link GeneralValueObject} BUKAN {@code @Entity} maupun {@code @MappedSuperclass} &mdash; ia
 *   POJO abstrak biasa, sehingga Hibernate <b>tidak memetakan satu pun property induknya</b>. Karena
 *   itu deklarasi ULANG {@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah}, dan
 *   {@code nomorUrut} di kelas ini <b>bukan bug</b>, melainkan keharusan teknis agar kolom-kolom itu
 *   benar-benar tersimpan.</li>
 *   <li>Anotasi {@code @Id} berada pada <b>getter</b>, sehingga Hibernate memakai <i>property
 *   access</i> untuk seluruh property. Digabung dengan {@code dynamicUpdate = true}, getter
 *   penambal/penurun nilai ({@link #getNomorUrut()}, {@link #getWajibDiisi()},
 *   {@link #getTampilDiSemuaTahunAngkatan()}, {@link #getTahunAngkatans()}) dapat mengotori field
 *   dan memicu {@code UPDATE} beserta revisi Envers baru pada baris yang <b>sekadar dibaca</b>.</li>
 *   <li>Property tanpa {@code @Column} eksplisit ({@code tampilDiSemuaTahunAngkatan},
 *   {@code program}, {@code wajibDiisi}, {@code nomorUrut}) memakai nama kolom apa adanya sesuai
 *   strategi penamaan bawaan Hibernate.</li>
 *   <li>Seluruh relasi {@code @ManyToOne} memakai {@code cascade = {PERSIST, MERGE}}, jadi menyimpan
 *   baris ini bisa ikut mem-{@code persist}/{@code merge} master yang direferensikan.</li>
 *   <li>Kelas ini TIDAK memiliki field {@code keterangan} sendiri; {@link #toString()},
 *   {@code getNama()}, {@code getNim()}, dan {@code getKeterangan()} sepenuhnya diwarisi dari
 *   {@link GeneralValueObject} dan selalu bernilai bawaan karena tidak dipetakan.</li>
 *   <li>{@code @Audited} (Envers) aktif: setiap perubahan baris konfigurasi ini terekam di tabel
 *   audit, termasuk {@code UPDATE} tak sengaja dari getter penambal di atas.</li>
 * </ul>
 *
 * <h3>Kuirk layar admin yang mempengaruhi data entity ini</h3>
 * <ul>
 *   <li><b>Simpan langsung tanpa tombol Simpan.</b> Checkbox "Isian Wajib", "Semua", dan tiap
 *   checkbox angkatan di grid daftar memanggil {@code Common.refreshSaveOrUpdate(...)} seketika pada
 *   {@code onCheck} &mdash; satu klik = satu {@code UPDATE} + satu revisi Envers.</li>
 *   <li><b>Cakupan baris baru mengikuti filter pencarian.</b> {@code onAdd()} mengisi
 *   {@code fakultas}/{@code jurusan}/{@code program}/{@code jenjang} baris baru dari combobox
 *   PENCARIAN yang sedang aktif, bukan dari form isian.</li>
 *   <li><b>{@code program} bisa terhapus diam-diam saat mengubah baris.</b> Combobox {@code program}
 *   dibuat dan diisi di {@code doAfterCompose()}, tetapi TIDAK pernah dipasang sebagai baris pada
 *   dialog "Ubah Parameter" (dialog hanya memuat Kelompok, Fakultas, Prodi, Jenjang, Parameter).
 *   Meski begitu {@code onSave()} tetap membaca {@code program.getSelectedItem()} dan menuliskannya
 *   ke baris &mdash; sehingga sekadar membuka lalu menyimpan sebuah baris dapat mengosongkan nilai
 *   {@code program} yang sudah ada. Dicatat apa adanya.</li>
 * </ul>
 *
 * @see ParameterTambahan
 * @see KelompokParameterTambahanAlumni
 * @see BiodataMahasiswa#populateParameterTambahanAlumni(java.util.List)
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "parameter_tambahan_alumni")

public class ParameterTambahanAlumni extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Entity ini ikut diserialisasi karena disimpan sebagai atribut komponen
	 * ZK dan sebagai nilai item {@code Combobox} pada layar admin.
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
	 * {@code return}), sehingga nilai lama bertahan dan jejak audit tidak pernah bisa dikosongkan.</p>
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
	 * ini. Baris hasil {@code onAdd()} layar admin karena itu masuk <b>tanpa jejak</b>
	 * {@code oleh}/{@code olehId} sampai pertama kali diubah.</p>
	 *
	 * <p>Perlu diingat bahwa {@link #getNomorUrut()}, {@link #getWajibDiisi()},
	 * {@link #getTampilDiSemuaTahunAngkatan()}, dan {@link #getTahunAngkatans()} dapat mengotori
	 * field saat baris sekadar dibaca, sehingga callback ini bisa ikut terpicu pada {@code UPDATE}
	 * yang <b>tidak diminta pengguna mana pun</b> &mdash; jejak audit lalu mencatat pengguna yang
	 * kebetulan sedang membuka layar atau mengisi kuesioner.</p>
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja berada pada baris fisik yang sama dalam kode
	 * aslinya; nilainya diinisialisasi memakai jam aplikasi ({@code ais.ui.util.WaktuUtil.getDate()}),
	 * bukan {@code new Date()}, agar konsisten dengan zona waktu/penyetelan waktu server yang dipakai
	 * seluruh modul.</p>
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

	/** Cakupan fakultas; disimpan tetapi TIDAK dipakai penyaring runtime (lihat dokumentasi kelas). */
	private Fakultas fakultas;
	/** Cakupan jurusan/prodi; disimpan tetapi TIDAK dipakai penyaring runtime. */
	private Jurusan jurusan;
	/** Cakupan program (kelas/reguler/karyawan dsb.) sebagai teks; TIDAK dipakai penyaring runtime. */
	private String program;
	/** Cakupan jenjang (S1/S2/&hellip;); disimpan tetapi TIDAK dipakai penyaring runtime. */
	private Jenjang jenjang;
	/** Definisi field kustom yang diadopsi baris ini; wajib, kolom {@code parameter_tambahan}. */
	private ParameterTambahan parameterTambahan;
	/** Penanda "berlaku untuk semua angkatan"; bila {@code false}, {@link #tahunAngkatans} berlaku. */
	private Boolean tampilDiSemuaTahunAngkatan;
	/** Daftar angkatan terpilih berformat {@code ";2019;;2020;"}; lihat dokumentasi kelas. */
	private String tahunAngkatans;
	/** Kategori/heading tempat field ini ditampilkan; wajib, kolom {@code kelompok_parameter_tambahan_alumni}. */
	private KelompokParameterTambahanAlumni kelompokParameterTambahanAlumni;
	/** Penimpa "isian wajib" khusus konteks alumni; bila {@code null} diturunkan dari definisi. */
	private Boolean wajibDiisi;

	/**
	 * Cache nomor urut tampil; nilainya <b>selalu ditimpa</b> dari {@link ParameterTambahan} setiap
	 * kali {@link #getNomorUrut()} dipanggil.
	 */
	private Integer nomorUrut;

	/**
	 * Mengembalikan nomor urut tampil field ini, <b>diturunkan dari definisinya</b>
	 * ({@link ParameterTambahan#getNomorUrut()}).
	 *
	 * <p>Meng-override {@code getNomorUrut()} milik {@link GeneralValueObject} dan merupakan
	 * <b>satu-satunya kunci urut yang efektif</b> bagi entity ini: {@code compareTo} induk mencoba
	 * berturut-turut {@code nomorUrut} &rarr; {@code nim} &rarr; {@code nama} &rarr;
	 * {@code keterangan}, dan tiga kunci terakhir selalu bernilai bawaan di sini karena tidak
	 * dipetakan. Pengurutan itulah yang dipakai {@code ParameterTambahanAlumniListener.displayRinci()}
	 * lewat {@code Collections.sort(...)} untuk menentukan urutan field di formulir alumni.</p>
	 *
	 * <p><b>Efek samping (penting).</b> Method ini bukan pembaca murni:</p>
	 * <ul>
	 *   <li>menugaskan ulang field {@code parameterTambahan} dengan hasil
	 *   {@link #getParameterTambahan()} &mdash; artinya proxy lazy ikut diresolusi
	 *   ({@link GeneralValueObject#check(Object)}) dan dapat memicu query;</li>
	 *   <li>menimpa field {@code nomorUrut} dengan nilai dari definisi. Karena Hibernate memakai
	 *   property access dan {@code dynamicUpdate = true}, kolom {@code nomorUrut} baris ini bisa
	 *   ter-{@code UPDATE} (beserta revisi Envers baru) hanya karena baris dibaca dalam sesi aktif.
	 *   Praktisnya kolom itu adalah <b>salinan denormalisasi</b> yang tidak pernah boleh berbeda dari
	 *   induknya &mdash; {@link #setNomorUrut(Integer)} tidak punya pengaruh yang bertahan.</li>
	 * </ul>
	 *
	 * @return nomor urut tampil; {@code 1} bila definisi maupun cache belum punya nilai, tidak pernah
	 *         {@code null}
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
	 * {@code ParameterTambahanAlumniAction.onAdd()} dan jalur auto-adopsi di
	 * {@link ParameterTambahan} saat sebuah grup parameter dibuat/diimpor.
	 */
	public ParameterTambahanAlumni() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * <p>{@code insertable = false} karena nilainya dihasilkan database ({@code IDENTITY}).
	 * Perhatikan bahwa ID ini <b>tidak pernah ikut tersimpan</b> pada string jawaban alumni di
	 * {@link BiodataMahasiswa}; lihat "Kunci penyimpanan" pada dokumentasi kelas.</p>
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
	 * Mengembalikan jurusan/prodi cakupan baris ini.
	 *
	 * <p><b>Tidak dipakai penyaring runtime</b> &mdash; lihat "Kuirk cakupan" pada dokumentasi kelas.
	 * Hanya dipakai kolom pencarian dan label grid layar admin ({@code null} ditampilkan
	 * "Semua").</p>
	 *
	 * <p><b>Efek samping ringan:</b> proxy lazy diresolusi lewat
	 * {@link GeneralValueObject#check(Object)} dan hasilnya ditulis balik ke field, agar pemanggil
	 * tidak menerima proxy yang meledak saat sesi sudah ditutup.</p>
	 *
	 * @return jurusan cakupan, atau {@code null} bila berlaku untuk semua jurusan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Menyetel jurusan/prodi cakupan baris ini.
	 *
	 * @param jurusan jurusan cakupan; {@code null} berarti semua jurusan
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan <b>definisi</b> field kustom yang diadopsi baris ini &mdash; sumber label, tipe
	 * input, daftar pilihan, kewajiban lampiran, nomor urut, dan relasi {@code parent} untuk field
	 * bersarang.
	 *
	 * <p>Relasi wajib ({@code nullable = false}). Dipakai hampir di setiap pemakai entity ini:
	 * perakit formulir ({@code displayRinci} memanggil {@code ParameterTambahan.initComponent}),
	 * validator wajib-isi, renderer grid admin, dan {@link #getNomorUrut()}/{@link #getWajibDiisi()}
	 * di kelas ini sendiri.</p>
	 *
	 * <p><b>Efek samping ringan:</b> proxy lazy diresolusi lewat
	 * {@link GeneralValueObject#check(Object)} dan hasilnya ditulis balik ke field.</p>
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
	 * <p>Mengubah nilai ini berarti mengubah kunci penyimpanan jawaban
	 * (<code>idKelompok + "-&gt;" + idParameterTambahan</code>), sehingga jawaban alumni yang sudah
	 * tersimpan untuk pasangan lama menjadi yatim.</p>
	 *
	 * @param parameterTambahan definisi field kustom; wajib diisi sebelum baris disimpan
	 */
	public void setParameterTambahan(ParameterTambahan parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	/**
	 * Menyatakan apakah field ini berlaku untuk <b>semua</b> tahun angkatan.
	 *
	 * <p>Bila {@code true}, {@link #getTahunAngkatans()} diabaikan sepenuhnya oleh seluruh query
	 * pembaca. Bila {@code false}, hanya angkatan yang tercantum di sana yang melihat field ini.</p>
	 *
	 * <p><b>Getter menulis balik:</b> {@code null} ditambal menjadi {@code true} di field &mdash;
	 * artinya baris tanpa nilai eksplisit bersifat "tampil di mana saja". Penambalan ini hanya
	 * berlaku di sisi Java; query SQL memakai {@code Restrictions.eq(..., true)} yang tidak mencakup
	 * {@code NULL} (lihat dokumentasi kelas).</p>
	 *
	 * @return {@code true} bila berlaku untuk semua angkatan; tidak pernah {@code null}
	 */
	public Boolean getTampilDiSemuaTahunAngkatan() {
		if (tampilDiSemuaTahunAngkatan == null) {
			tampilDiSemuaTahunAngkatan = true;
		}
		return tampilDiSemuaTahunAngkatan;
	}

	/**
	 * Menyetel penanda "berlaku untuk semua tahun angkatan".
	 *
	 * <p>Dipanggil dari event {@code onCheck} checkbox "Semua" pada grid layar admin, yang langsung
	 * menyimpan perubahan tanpa tombol Simpan dan langsung membangun ulang daftar checkbox
	 * angkatan.</p>
	 *
	 * @param tampilDiSemuaTahunAngkatan {@code true} untuk berlaku ke semua angkatan
	 */
	public void setTampilDiSemuaTahunAngkatan(Boolean tampilDiSemuaTahunAngkatan) {
		this.tampilDiSemuaTahunAngkatan = tampilDiSemuaTahunAngkatan;
	}

	/**
	 * Mengembalikan daftar tahun angkatan yang berhak melihat field ini, dalam format string
	 * terserialisasi.
	 *
	 * <p><b>Format:</b> tiap angkatan ditulis {@code ";" + tahun + ";"} lalu dirangkai tanpa pemisah
	 * tambahan, sehingga pilihan 2019 dan 2020 menghasilkan {@code ";2019;;2020;"}. Kolom bertipe
	 * {@code text} agar muat untuk instalasi dengan puluhan angkatan.</p>
	 *
	 * <p><b>Cara dibaca:</b> layar admin memakai {@code split(";")} sambil melewati potongan kosong;
	 * pembaca runtime memakai {@code Restrictions.ilike("tahunAngkatans", ";" + gel + ";",
	 * MatchMode.ANYWHERE)} sehingga titik-koma pengapit mencegah salah cocok parsial. Nilai ini hanya
	 * berpengaruh bila {@link #getTampilDiSemuaTahunAngkatan()} bernilai {@code false}.</p>
	 *
	 * <p><b>Getter menulis balik:</b> {@code null} ditambal menjadi {@code ""} di field &mdash;
	 * penting karena layar admin memanggil {@code .split(";")} langsung pada hasilnya.</p>
	 *
	 * @return daftar angkatan terserialisasi; string kosong bila belum ada pilihan, tidak pernah
	 *         {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getTahunAngkatans() {
		if (tahunAngkatans == null) {
			tahunAngkatans = "";
		}
		return tahunAngkatans;
	}

	/**
	 * Menyetel daftar tahun angkatan terserialisasi.
	 *
	 * <p>Dipanggil dari event {@code onCheck} tiap checkbox angkatan di grid layar admin, yang
	 * merangkai ulang SELURUH daftar dari kondisi checkbox lalu langsung menyimpannya. Nilai yang
	 * diisi manual/lewat impor harus mengikuti format {@code ";tahun;"} per entri, jika tidak
	 * penyaring runtime tidak akan pernah cocok.</p>
	 *
	 * @param tahunAngkatans daftar angkatan terserialisasi
	 */
	public void setTahunAngkatans(String tahunAngkatans) {
		this.tahunAngkatans = tahunAngkatans;
	}

	/**
	 * Mengembalikan fakultas cakupan baris ini.
	 *
	 * <p><b>Tidak dipakai penyaring runtime</b> &mdash; lihat "Kuirk cakupan" pada dokumentasi kelas.
	 * Hanya dipakai kolom pencarian layar admin.</p>
	 *
	 * <p><b>Efek samping ringan:</b> proxy lazy diresolusi lewat
	 * {@link GeneralValueObject#check(Object)} dan hasilnya ditulis balik ke field.</p>
	 *
	 * @return fakultas cakupan, atau {@code null} bila berlaku untuk semua fakultas
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * Menyetel fakultas cakupan baris ini.
	 *
	 * @param fakultas fakultas cakupan; {@code null} berarti semua fakultas
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Mengembalikan program cakupan baris ini sebagai teks bebas (bukan relasi entity) &mdash; nilai
	 * yang sama dengan isi combobox {@code Common.initPrograms(...)}, mis. kelas reguler/karyawan.
	 *
	 * <p><b>Tidak dipakai penyaring runtime</b>, dan {@code null}/kosong ditampilkan "Semua" di grid
	 * admin. Tidak ada penambalan {@code null} di sini, jadi pemanggil wajib menjaga diri.</p>
	 *
	 * @return nama program cakupan, atau {@code null} bila berlaku untuk semua program
	 */
	public String getProgram() {
		return program;
	}

	/**
	 * Menyetel program cakupan baris ini.
	 *
	 * <p>Diisi {@code onAdd()} dari combobox pencarian, dan ditulis ulang {@code onSave()} dari
	 * combobox {@code program} yang tidak pernah tampil di dialog ubah &mdash; lihat kuirk pada
	 * dokumentasi kelas.</p>
	 *
	 * @param program nama program cakupan; {@code null} berarti semua program
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * Mengembalikan jenjang cakupan baris ini.
	 *
	 * <p><b>Tidak dipakai penyaring runtime</b> &mdash; lihat "Kuirk cakupan" pada dokumentasi kelas.
	 * Hanya dipakai kolom pencarian dan label grid layar admin.</p>
	 *
	 * <p><b>Efek samping ringan:</b> proxy lazy diresolusi lewat
	 * {@link GeneralValueObject#check(Object)} dan hasilnya ditulis balik ke field.</p>
	 *
	 * @return jenjang cakupan, atau {@code null} bila berlaku untuk semua jenjang
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenjang", nullable = true)
	public Jenjang getJenjang() {
		jenjang = check(jenjang);
		return jenjang;
	}

	/**
	 * Menyetel jenjang cakupan baris ini.
	 *
	 * @param jenjang jenjang cakupan; {@code null} berarti semua jenjang
	 */
	public void setJenjang(Jenjang jenjang) {
		this.jenjang = jenjang;
	}

	/**
	 * Mengembalikan <b>kategori/heading</b> tempat field ini ditampilkan pada formulir alumni.
	 *
	 * <p>Relasi wajib ({@code nullable = false}) dan merupakan lapis ketiga rantai field kustom.
	 * Sifat kategori inilah yang menentukan apakah field ikut tampil: seluruh query pembaca
	 * mensyaratkan {@code kelompokParameterTambahanAlumni.aktif = true} dan mencocokkan
	 * {@code digunakanUntukPenggunaAlumni} dengan konteks pengisian (portal alumni vs. layar internal
	 * staf).</p>
	 *
	 * <p>ID kategori ini juga menjadi <b>ruas pertama kunci penyimpanan jawaban</b>
	 * (<code>idKelompok + "-&gt;" + idParameterTambahan</code>) di {@link BiodataMahasiswa} dan
	 * penanda {@code jenis} pada {@link ais.database.model.file.LampiranLain}.</p>
	 *
	 * <p><b>Efek samping ringan:</b> proxy lazy diresolusi lewat
	 * {@link GeneralValueObject#check(Object)} dan hasilnya ditulis balik ke field.</p>
	 *
	 * @return kategori/heading pemilik field ini
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_parameter_tambahan_alumni", nullable = false)
	public KelompokParameterTambahanAlumni getKelompokParameterTambahanAlumni() {
		kelompokParameterTambahanAlumni = check(kelompokParameterTambahanAlumni);
		return kelompokParameterTambahanAlumni;
	}

	/**
	 * Menyetel kategori/heading pemilik field ini.
	 *
	 * <p><b>Perhatian:</b> memindahkan baris ke kategori lain mengubah kunci penyimpanan jawaban,
	 * sehingga seluruh jawaban alumni yang sudah tersimpan untuk pasangan lama tidak akan terbaca
	 * lagi (tetap ada di kolom {@code text}, tetapi yatim).</p>
	 *
	 * @param kelompokParameterTambahanAlumni kategori/heading; wajib diisi sebelum baris disimpan
	 */
	public void setKelompokParameterTambahanAlumni(KelompokParameterTambahanAlumni kelompokParameterTambahanAlumni) {
		this.kelompokParameterTambahanAlumni = kelompokParameterTambahanAlumni;
	}

	/**
	 * Menyatakan apakah field ini <b>wajib diisi</b> oleh alumni dalam konteks kelompok ini.
	 *
	 * <p>Nilai lokal berfungsi sebagai <b>penimpa per-adopsi</b>: satu definisi
	 * {@link ParameterTambahan} yang sama bisa wajib di satu kelompok dan opsional di kelompok lain.
	 * Dipakai {@code ParameterTambahanAlumniListener.validate(...)} yang memblokir penyimpanan
	 * biodata bila field wajib masih kosong (kecuali {@code tipeDataInputan} bernilai
	 * {@code ParameterTambahan.TIDAK_ADA}), dan dirender sebagai checkbox "Isian Wajib" di grid admin
	 * yang langsung menyimpan saat diklik.</p>
	 *
	 * <p><b>Getter menulis balik, dua tahap:</b> bila field masih {@code null}, nilai diwarisi dari
	 * {@link ParameterTambahan#getWajibDiisi()}; bila hasilnya masih {@code null} juga, ditambal
	 * {@code true}. Artinya <b>default sistem adalah WAJIB</b> &mdash; asimetri "ketat secara bawaan"
	 * yang perlu disadari saat mengadopsi field baru secara massal. Sekali nilai ditulis (oleh getter
	 * ini maupun oleh admin), nilai lokal menang dan perubahan pada definisi induk tidak lagi
	 * merambat ke sini.</p>
	 *
	 * <p>Seperti getter penambal lain di kelas ini, penulisan balik dapat memicu {@code UPDATE} dan
	 * revisi Envers pada baris yang sekadar dibaca.</p>
	 *
	 * @return {@code true} bila field wajib diisi; tidak pernah {@code null}
	 */
	public Boolean getWajibDiisi() {
		if (wajibDiisi == null && getParameterTambahan() != null) {
			wajibDiisi = getParameterTambahan().getWajibDiisi();
		}
		if (wajibDiisi == null) {
			wajibDiisi = true;
		}
		return wajibDiisi;
	}

	/**
	 * Menyetel penanda "isian wajib" khusus adopsi ini.
	 *
	 * <p>Menyetel nilai eksplisit (termasuk {@code false}) memutus pewarisan dari
	 * {@link ParameterTambahan#getWajibDiisi()} secara permanen untuk baris ini.</p>
	 *
	 * @param wajibDiisi {@code true} bila field harus diisi alumni
	 */
	public void setWajibDiisi(Boolean wajibDiisi) {
		this.wajibDiisi = wajibDiisi;
	}

}
