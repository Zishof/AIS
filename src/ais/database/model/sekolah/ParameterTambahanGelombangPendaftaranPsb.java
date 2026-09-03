package ais.database.model.sekolah;

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.ParameterTambahan;

/**
 * Entity <b>penghubung</b> (tabel jembatan) rantai field kustom pada modul
 * <b>Penerimaan Siswa Baru (PSB/PPDB)</b>. Satu baris entity ini menyatakan:
 * <i>"parameter tambahan X dipakai pada gelombang pendaftaran Y, ditampilkan di bawah judul
 * kelompok Z"</i>. Tabel fisiknya {@code sekolah.parameter_tambahan_gelombang_pendaftaran_psb}.
 *
 * <h3>Posisi dalam rantai (TERVERIFIKASI dari kode)</h3>
 * Rantai lengkapnya 4 lapis dan seluruhnya dikonfirmasi dari relasi yang dideklarasikan file ini
 * sendiri plus pembacanya di runtime:
 * <ol>
 *   <li>{@link ParameterTambahan} &mdash; master DEFINISI field (label, tipe data, nilai
 *       pilihan, wajib-isi, wajib-lampiran, nomor urut). Dipakai bersama oleh SELURUH modul
 *       parameter tambahan di AIS, bukan milik PSB.</li>
 *   <li>{@link KelompokParameterTambahanCalonSiswa} &mdash; master KATEGORI/judul seksi pada
 *       formulir PSB (mis. baris bawaan {@code "VII. Form Tambahan"} yang dibuat otomatis oleh
 *       {@link KelompokParameterTambahanCalonSiswa#checkCreateDefault()}).</li>
 *   <li><b>entity ini</b> &mdash; lapis PEMETAAN: memasangkan (parameter &times; kelompok &times;
 *       gelombang) dan menambahkan dua bendera visibilitas berbasis status login.</li>
 *   <li>{@link GelombangPendaftaranPsb} &mdash; cakupan/periode gelombang pendaftaran.</li>
 * </ol>
 *
 * <h3>Perbedaan penting dengan padanan versi Perguruan Tinggi</h3>
 * Padanan modul PMB/PT adalah {@code ais.database.model.ParameterTambahanPaket}. Struktur kedua
 * entity <b>BERBEDA secara fundamental pada cara cakupan gelombang dinyatakan</b>:
 * <ul>
 *   <li><b>Versi PT</b> mendenormalisasi cakupan gelombang ke dua kolom skalar
 *       ({@code tampilDiSemuaGelombang} boolean + {@code gelombangs} berisi daftar id
 *       terserialisasi {@code ";id;;id;"} yang dicari dengan {@code ilike ANYWHERE}) &mdash;
 *       tanpa relasi FK apa pun ke gelombang.</li>
 *   <li><b>Versi sekolah (file ini)</b> memakai relasi {@code @ManyToOne} SUNGGUHAN ke
 *       {@link GelombangPendaftaranPsb} dengan {@code nullable = true}. Semantik
 *       "berlaku untuk semua gelombang" dinyatakan sebagai <b>FK bernilai {@code NULL}</b>,
 *       bukan lewat bendera terpisah. Seluruh pembaca runtime menuliskannya secara konsisten
 *       sebagai {@code Restrictions.or(Restrictions.isNull("gelombangPendaftaranPsb"),
 *       Restrictions.eq("gelombangPendaftaranPsb", gel))}.</li>
 * </ul>
 * Konsekuensinya: satu parameter yang berlaku di 3 gelombang tertentu memerlukan 3 baris entity
 * ini (satu per gelombang), sedangkan versi PT cukup satu baris dengan 3 id di kolom text.
 * <p>Sebagai gantinya, versi sekolah punya dua kolom yang <b>TIDAK ADA</b> di versi PT:
 * {@code tampilDiFromSebelumLogin} dan {@code tampilDiFromSetelahLogin} &mdash; saringan
 * visibilitas berdasarkan status login pengisi formulir.</p>
 *
 * <h3>Di mana nilai isian calon siswa disimpan (TERVERIFIKASI)</h3>
 * Entity ini <b>tidak pernah menyimpan nilai yang diisi calon siswa</b> &mdash; ia hanya
 * mendefinisikan field mana yang muncul. Nilai isian ditulis ke <b>dua kolom text</b> pada entity
 * {@link CalonSiswa} (perhatikan: di skema {@code sekolah} <b>tidak ada</b> kelas
 * {@code BiodataCalonSiswa}; pemilik data adalah {@code CalonSiswa} itu sendiri), lewat
 * {@code CalonSiswa.populateParameterTambahan(List&lt;Row&gt;)}. Baris dipisah {@code "\n"},
 * ruas dipisah {@code "<=>"}:
 * <ul>
 *   <li><b>{@code CalonSiswa.parameterTambahan}</b> &mdash; varian BERLABEL, <b>7 ruas</b>:
 *       <pre>namaKelompok-&gt;labelInputan &lt;=&gt; nilai &lt;=&gt; urlLampiran &lt;=&gt; nomorUrut &lt;=&gt; idParameter &lt;=&gt; idKelompok &lt;=&gt; keterangan</pre>
 *       Dipakai untuk tampilan/laporan yang butuh teks siap baca, serta oleh
 *       {@code CalonSiswa.ambilSkor(ParameterTambahan)} untuk menghitung skor seleksi dari
 *       jawaban bertipe {@code PILIHAN_CUSTOM}.</li>
 *   <li><b>{@code CalonSiswa.parameterTambahanInds}</b> &mdash; varian ber-ID, <b>4 ruas</b>:
 *       <pre>idKelompok-&gt;idParameter &lt;=&gt; nilai &lt;=&gt; urlLampiran &lt;=&gt; keterangan</pre>
 *       Inilah varian yang dibaca kembali saat formulir dimuat ulang (oleh
 *       {@code ParameterTambahanPsbListener.onEvent} dan
 *       {@code TampilanPengumumanAkademisAction}).</li>
 * </ul>
 * Kunci gabungan <b>{@code idKelompok + "->" + idParameter}</b> berperan ganda: selain menjadi
 * ruas pertama kedua format di atas, ia juga dipakai apa adanya sebagai argumen {@code jenis}
 * pada {@code LampiranLain.ambil(idCalonSiswa, jenis)} untuk mengambil berkas unggahan yang
 * menyertai jawaban. Mengubah id kelompok atau id parameter setelah data terisi akan
 * <b>memutus ketiga jalur itu sekaligus</b> (nilai, keterangan, dan lampiran) tanpa error.
 *
 * <h3>Pembaca runtime</h3>
 * Di luar layar masternya sendiri, entity ini hanya di-query oleh dua kelas:
 * <ul>
 *   <li>{@code ais.action.master.sekolah.psb.ParameterTambahanPsbListener} &mdash; pembangun
 *       formulir dinamis PSB (jalur pendaftaran publik {@code /ppdb} maupun jalur setelah calon
 *       siswa login). Melakukan dua query bertingkat: pertama
 *       {@code groupProperty("kelompokParameterTambahanCalonSiswa")} untuk mendapat daftar
 *       judul seksi, lalu satu query per seksi dengan
 *       {@code groupProperty("parameterTambahan")} untuk isinya. Kedua query menyaring
 *       {@code parameterTambahan.aktif = true} DAN
 *       {@code kelompokParameterTambahanCalonSiswa.aktif = true}, ditambah saringan gelombang
 *       dan saringan login di atas.</li>
 *   <li>{@code ais.action.master.TampilanPengumumanAkademisAction} &mdash; layar
 *       pengumuman/verifikasi berkas, memakai pola query yang sama namun <b>tanpa</b> saringan
 *       {@code tampilDiFrom*} (petugas selalu melihat seluruh parameter gelombang tersebut).</li>
 * </ul>
 * <p><b>Konsekuensi pola {@code groupProperty}:</b> kedua pembaca mengembalikan objek
 * {@link ParameterTambahan}/{@link KelompokParameterTambahanCalonSiswa}, <b>bukan</b> instance
 * entity ini. Pengurutan pun dilakukan dengan {@code Collections.sort()} atas objek master
 * tersebut, sehingga {@code compareTo}/{@code nomorUrut} <b>milik entity ini tidak pernah
 * dipakai untuk mengurutkan apa pun di runtime</b> &mdash; kuirk yang sama sudah tercatat pada
 * saudara-saudaranya di keluarga {@code ParameterTambahan*}.</p>
 *
 * <h3>Kuirk &amp; catatan yang perlu diketahui</h3>
 * <ul>
 *   <li><b>Salah eja properti yang sudah terlanjur permanen.</b> Kedua bendera visibilitas
 *       bernama {@code tampilDiFrom...}, bukan {@code tampilDiForm...} ("From" vs "Form").
 *       Karena entity ini memakai <i>property access</i> tanpa {@code @Column} eksplisit, nama
 *       kolom di database ikut mengambil ejaan yang salah tersebut. Memperbaiki ejaan berarti
 *       migrasi skema + menyentuh seluruh {@code Restrictions.eq("tampilDiFrom...")} di
 *       pembacanya; jangan diperbaiki sepihak.</li>
 *   <li><b>SQL migrasi mentah pada layar masternya.</b> Sama seperti versi PT,
 *       {@code ParameterTambahanGelombangPendaftaranPsbAction.doAfterCompose} menjalankan
 *       {@code UPDATE} SQL mentah <b>tanpa syarat, setiap kali layar dibuka</b>, untuk mengisi
 *       {@code kelompok_parameter_tambahan_calon_siswa} yang masih {@code NULL} dengan id
 *       kelompok bawaan. Karena lewat {@code createSQLQuery(...).executeUpdate()}, perubahan itu
 *       <b>melewati Envers</b> &mdash; baris berubah tanpa jejak revisi audit sama sekali.</li>
 *   <li><b>Kolom FK kelompok {@code nullable = true} padahal pembacanya mengasumsikan non-null.</b>
 *       Renderer daftar memanggil
 *       {@code ...getKelompokParameterTambahanCalonSiswa().getNama()} tanpa penjagaan null;
 *       yang menyelamatkannya hanyalah SQL migrasi di atas. Baris yang dibuat lewat jalur lain
 *       (impor Excel, SQL manual) sebelum layar sempat dibuka berpotensi
 *       {@code NullPointerException} di renderer.</li>
 *   <li><b>Tidak ada batasan unik.</b> Tidak ada {@code unique = true} maupun pemeriksaan
 *       duplikat di jalur simpan; parameter yang sama bisa terdaftar dua kali pada gelombang
 *       yang sama bila didaftarkan lewat kelompok berbeda. Karena kunci nilai isian hanya
 *       memakai {@code idKelompok->idParameter}, duplikat lintas-kelompok akan menghasilkan
 *       dua baris isian berbeda untuk satu pertanyaan yang secara visual identik.</li>
 *   <li><b>Tidak punya field {@code keterangan}.</b> Konsisten dengan seluruh lapis penghubung
 *       {@code ParameterTambahan*} lainnya; teks keterangan yang tampil di formulir berasal dari
 *       {@code ParameterTambahan.getKeterangan()}, bukan dari entity ini.</li>
 * </ul>
 *
 * <h3>Pengelompokan anggota kelas</h3>
 * <ol>
 *   <li><b>Jejak audit warisan</b> ({@code oleh}, {@code olehId}, {@code tanggal_dirubah},
 *       {@code onUpdate()}) &mdash; lihat catatan {@code GeneralValueObject} di bawah.</li>
 *   <li><b>Identitas</b> ({@code id}) &mdash; {@code IDENTITY}, {@code insertable = false}.</li>
 *   <li><b>Tiga relasi inti</b> ({@code parameterTambahan}, {@code kelompokParameterTambahanCalonSiswa},
 *       {@code gelombangPendaftaranPsb}).</li>
 *   <li><b>Bendera visibilitas</b> ({@code tampilDiFromSebelumLogin},
 *       {@code tampilDiFromSetelahLogin}) &mdash; keduanya default {@code true} saat null.</li>
 *   <li><b>Urutan</b> ({@code nomorUrut}) &mdash; praktis kode mati untuk pengurutan, lihat di
 *       atas dan lihat {@link #getNomorUrut()}.</li>
 * </ol>
 *
 * <h3>Catatan teknis wajib soal {@code GeneralValueObject}</h3>
 * {@link ais.database.model.GeneralValueObject} <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass} &mdash; ia POJO abstrak biasa, sehingga Hibernate <b>tidak</b>
 * memetakan properti yang dideklarasikan di sana. Karena itu deklarasi ULANG field {@code id},
 * {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} di kelas ini <b>bukan bug dan bukan
 * duplikasi ceroboh</b>, melainkan keharusan teknis agar kolom-kolom tersebut ikut dipetakan.
 * Jangan "dirapikan" dengan menaikkannya ke kelas induk.
 *
 * @see ais.database.model.GeneralValueObject
 * @see ParameterTambahan
 * @see KelompokParameterTambahanCalonSiswa
 * @see GelombangPendaftaranPsb
 * @see CalonSiswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "parameter_tambahan_gelombang_pendaftaran_psb")

public class ParameterTambahanGelombangPendaftaranPsb extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya sengaja disamakan dengan seluruh entity keluarga
	 * {@code ParameterTambahan*}/{@code KelompokParameterTambahan*} hasil generator yang sama
	 * &mdash; kesamaan nilai antar kelas berbeda tidak berdampak apa pun karena
	 * {@code serialVersionUID} hanya dibandingkan per-kelas.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key {@code IDENTITY}; dideklarasikan ulang karena kelas induk tidak dipetakan Hibernate. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi oleh interceptor audit. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; diisi oleh interceptor audit. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah tersentuh interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah. <b>Perhatikan penjagaan di baris pertama</b>: nilai
	 * {@code null} atau berisi spasi saja <b>diabaikan diam-diam</b> (method langsung
	 * {@code return}), sehingga jejak audit yang sudah ada tidak bisa terhapus oleh pemanggil
	 * yang lalai. Pola ini seragam di seluruh entity turunan
	 * {@code GeneralValueObject}.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null}/kosong diabaikan diam-diam agar jejak audit lama tidak tertimpa.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: menyerahkan pengisian jejak audit
	 * ({@code oleh}/{@code olehId}/{@code tanggal_dirubah}) ke
	 * {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum {@code UPDATE} dikirim.
	 * <p><b>Efek samping:</b> memodifikasi state instance ini dalam transaksi yang sedang
	 * berjalan. Dipanggil oleh Hibernate, tidak pernah oleh kode aplikasi.</p>
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja ditempatkan pada baris yang sama
	 * oleh generator; nilainya diinisialisasi ke waktu server saat objek dibuat sehingga baris
	 * baru selalu punya stempel waktu walau {@code @PreUpdate} belum pernah berjalan.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir (presisi {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang dibuat lewat
	 *         konstruktor Java karena field diinisialisasi saat instansiasi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Kategori/judul seksi tempat parameter ini dikelompokkan pada formulir PSB. */
	private KelompokParameterTambahanCalonSiswa kelompokParameterTambahanCalonSiswa;
	/** Gelombang pendaftaran tempat pemetaan ini berlaku; {@code null} berarti SEMUA gelombang. */
	private GelombangPendaftaranPsb gelombangPendaftaranPsb;
	/** Definisi field yang dipetakan (label, tipe, pilihan, wajib-isi, wajib-lampiran). */
	private ParameterTambahan parameterTambahan;

	/** Bendera tampil pada formulir sebelum calon siswa login; {@code null} diperlakukan {@code true}. */
	private Boolean tampilDiFromSebelumLogin;
	/** Bendera tampil pada formulir setelah calon siswa login; {@code null} diperlakukan {@code true}. */
	private Boolean tampilDiFromSetelahLogin;
	/** Salinan lokal nomor urut; lihat {@link #getNomorUrut()} soal sifat dan kegunaannya. */
	private Integer nomorUrut;

	/**
	 * Mengembalikan nomor urut tampil parameter ini.
	 *
	 * <p><b>Bukan getter murni.</b> Bila relasi {@link #getParameterTambahan() parameterTambahan}
	 * sudah termuat, method ini <b>menimpa field lokal {@code nomorUrut}</b> dengan nilai
	 * {@code parameterTambahan.getNomorUrut()} sebelum mengembalikannya. Karena entity ini
	 * memakai <i>property access</i> dan {@code dynamicUpdate = true}, penimpaan tersebut
	 * membuat Hibernate mendeteksi perubahan state saat <i>flush</i> &mdash; sekadar membaca
	 * baris di dalam sesi aktif dapat memicu {@code UPDATE} kolom {@code nomorUrut} berikut
	 * satu revisi Envers "palsu" yang tidak berasal dari aksi pengguna. Pola getter destruktif
	 * ini sudah dikenal luas di seluruh turunan {@code GeneralValueObject}.</p>
	 * <p><b>Sekaligus praktis kode mati.</b> Tidak satu pun pembaca runtime memakai nilai ini:
	 * pengurutan formulir PSB dikerjakan {@code Collections.sort()} atas objek
	 * {@link ParameterTambahan}/{@link KelompokParameterTambahanCalonSiswa} hasil
	 * {@code groupProperty}, bukan atas instance entity ini. Layar masternya pun tidak
	 * menyediakan komponen untuk menyunting nilai ini.</p>
	 *
	 * @return nomor urut dari master parameter bila tersedia, jika tidak nilai lokal, dan
	 *         {@code 1} bila keduanya {@code null} (tidak pernah mengembalikan {@code null})
	 */
	public Integer getNomorUrut() {
		if (parameterTambahan != null) {
			nomorUrut = parameterTambahan.getNomorUrut();
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut lokal. Nilai yang disetel akan <b>tertimpa lagi</b> oleh
	 * {@link #getNomorUrut()} pada pembacaan berikutnya selama relasi
	 * {@code parameterTambahan} terisi, sehingga setter ini efektif hanya berpengaruh pada
	 * baris tanpa parameter (kondisi yang tidak mungkin terjadi karena kolom FK-nya
	 * {@code nullable = false}).
	 *
	 * @param nomorUrut nomor urut baru
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. Seluruh relasi dan bendera
	 * dibiarkan {@code null} dan diisi belakangan oleh jalur pemanggil (layar master atau
	 * impor). Perhatikan bahwa {@code tanggal_dirubah} sudah terisi waktu server pada titik ini
	 * lewat inisialisasi field.
	 */
	public ParameterTambahanGelombangPendaftaranPsb() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * <p>Nilai dibangkitkan database ({@code IDENTITY}) dan kolomnya ditandai
	 * {@code insertable = false}, sehingga id baru baru tersedia setelah {@code INSERT}
	 * di-<i>flush</i>. Id inilah yang dirujuk sebagai target pada SQL migrasi mentah di layar
	 * master (lihat javadoc kelas).</p>
	 *
	 * @return id baris, atau {@code null} untuk objek yang belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Hanya dipakai Hibernate saat memuat baris; kode aplikasi tidak boleh
	 * memanggilnya untuk objek yang sudah persisten.
	 *
	 * @param id primary key
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan gelombang pendaftaran tempat pemetaan ini berlaku.
	 *
	 * <p><b>Semantik {@code null} penting:</b> {@code null} <b>bukan</b> berarti data belum
	 * lengkap, melainkan berarti pemetaan berlaku untuk <b>seluruh gelombang</b>. Seluruh
	 * pembaca runtime menyaringnya dengan
	 * {@code Restrictions.or(isNull("gelombangPendaftaranPsb"), eq("gelombangPendaftaranPsb", gel))}.
	 * Inilah pengganti bendera {@code tampilDiSemuaGelombang} yang dipakai padanan versi PT
	 * ({@code ParameterTambahanPaket}).</p>
	 * <p>Relasi ini memakai {@code FetchMode.SELECT} dan tanpa {@code fetch = LAZY} eksplisit
	 * &mdash; berbeda dari dua relasi lain di kelas ini &mdash; sehingga tidak melewati helper
	 * {@code check()} seperti kedua saudaranya.</p>
	 *
	 * @return gelombang pendaftaran terkait, atau {@code null} bila berlaku untuk semua gelombang
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "gelombangPendaftaranPsb", nullable = true)
	public GelombangPendaftaranPsb getGelombangPendaftaranPsb() {
		return gelombangPendaftaranPsb;
	}

	/**
	 * Menyetel gelombang pendaftaran tempat pemetaan ini berlaku.
	 *
	 * @param gelombangPendaftaranPsb gelombang terkait; {@code null} berarti berlaku untuk
	 *                                seluruh gelombang (lihat {@link #getGelombangPendaftaranPsb()})
	 */
	public void setGelombangPendaftaranPsb(GelombangPendaftaranPsb gelombangPendaftaranPsb) {
		this.gelombangPendaftaranPsb = gelombangPendaftaranPsb;
	}

	/**
	 * Mengembalikan definisi field ({@link ParameterTambahan}) yang dipetakan baris ini.
	 *
	 * <p>Relasi {@code LAZY}, karena itu getter melewatkan nilainya ke helper
	 * {@link ais.database.model.GeneralValueObject#check(Object)} untuk meresolusi proxy lazy
	 * menjadi instance kanonik. Helper tersebut tidak pernah melempar exception dan tidak pernah
	 * mengembalikan {@code null} untuk argumen non-null &mdash; kegagalan resolusi bersifat
	 * senyap. Hasil resolusi <b>ditulis balik ke field</b> sehingga instance yang dikembalikan
	 * bisa berbeda dari yang sebelumnya tersimpan.</p>
	 * <p>Dari sinilah renderer daftar dan pembangun formulir mengambil {@code labelInputan},
	 * {@code tipeDataInputan}, {@code nilaiDataInputan}, {@code keterangan},
	 * {@code wajibDiisi}, {@code lampiranWajibDiisi}, dan {@code harusMenyertakanLampiran}.</p>
	 *
	 * @return definisi parameter tambahan; kolom FK-nya {@code nullable = false} sehingga secara
	 *         praktis selalu terisi untuk baris yang valid
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "parameter_tambahan", nullable = false)
	public ParameterTambahan getParameterTambahan() {
		parameterTambahan = check(parameterTambahan);
		return parameterTambahan;
	}

	/**
	 * Menyetel definisi field yang dipetakan baris ini.
	 *
	 * @param parameterTambahan definisi parameter tambahan; wajib non-{@code null} karena kolom
	 *                          FK-nya {@code nullable = false}
	 */
	public void setParameterTambahan(ParameterTambahan parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	/**
	 * Mengembalikan kategori/judul seksi tempat parameter ini dikelompokkan pada formulir PSB.
	 *
	 * <p>Relasi {@code LAZY}, diresolusi lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dengan efek write-back yang
	 * sama seperti {@link #getParameterTambahan()}.</p>
	 * <p><b>Peringatan:</b> kolom FK-nya {@code nullable = true}, tetapi renderer daftar pada
	 * layar master memanggil {@code getNama()} atas hasil method ini tanpa penjagaan null. Yang
	 * mencegah {@code NullPointerException} hanyalah SQL migrasi mentah yang dijalankan setiap
	 * kali layar dibuka (lihat javadoc kelas) &mdash; bukan batasan skema.</p>
	 * <p>Id yang dikembalikan relasi ini menjadi ruas pertama kunci gabungan
	 * {@code idKelompok->idParameter} yang menandai nilai isian di {@link CalonSiswa} dan berkas
	 * di {@code LampiranLain}.</p>
	 *
	 * @return kelompok/kategori parameter, atau {@code null} untuk baris yang belum pernah
	 *         tersentuh migrasi maupun layar master
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_parameter_tambahan_calon_siswa", nullable = true)
	public KelompokParameterTambahanCalonSiswa getKelompokParameterTambahanCalonSiswa() {
		kelompokParameterTambahanCalonSiswa = check(kelompokParameterTambahanCalonSiswa);
		return kelompokParameterTambahanCalonSiswa;
	}

	/**
	 * Menyetel kategori/judul seksi parameter ini.
	 *
	 * @param kelompokParameterTambahanCalonSiswa kelompok tujuan; mengubahnya setelah ada calon
	 *                                            siswa yang mengisi akan memutus kunci
	 *                                            {@code idKelompok->idParameter} pada data isian
	 *                                            dan lampiran yang sudah tersimpan
	 */
	public void setKelompokParameterTambahanCalonSiswa(
			KelompokParameterTambahanCalonSiswa kelompokParameterTambahanCalonSiswa) {
		this.kelompokParameterTambahanCalonSiswa = kelompokParameterTambahanCalonSiswa;
	}

	/**
	 * Menyatakan apakah parameter ini ikut ditampilkan pada formulir saat pengisi
	 * <b>belum login</b> (formulir pendaftaran publik {@code /ppdb}).
	 *
	 * <p>Nilai {@code null} diperlakukan sebagai {@code true} &mdash; default "tampil". Berbeda
	 * dengan {@link #getNomorUrut()}, method ini <b>tidak</b> menulis balik ke field, sehingga
	 * bebas dari efek samping {@code UPDATE} senyap.</p>
	 * <p>Bendera ini ditegakkan di lapis query oleh
	 * {@code ParameterTambahanPsbListener} dengan
	 * {@code Restrictions.or(isNull("tampilDiFromSebelumLogin"), eq("tampilDiFromSebelumLogin", true))}
	 * &mdash; konsisten dengan default {@code true} di sini. Perhatikan bahwa
	 * {@code TampilanPengumumanAkademisAction} <b>tidak</b> menerapkan saringan ini.</p>
	 * <p>Perhatikan salah eja properti yang sudah permanen: {@code From}, bukan {@code Form}.</p>
	 *
	 * @return {@code true} bila tampil sebelum login (termasuk saat nilai tersimpan {@code null})
	 */
	public Boolean getTampilDiFromSebelumLogin() {
		return tampilDiFromSebelumLogin == null ? true : tampilDiFromSebelumLogin;
	}

	/**
	 * Menyetel bendera tampil-sebelum-login. Disetel dari checkbox "Tampil Sebelum Login" pada
	 * renderer baris layar master, yang langsung menyimpan perubahan per klik.
	 *
	 * @param tampilDiFromSebelumLogin {@code true} agar parameter tampil pada formulir publik
	 */
	public void setTampilDiFromSebelumLogin(Boolean tampilDiFromSebelumLogin) {
		this.tampilDiFromSebelumLogin = tampilDiFromSebelumLogin;
	}

	/**
	 * Menyatakan apakah parameter ini ikut ditampilkan pada formulir saat pengisi
	 * <b>sudah login</b> sebagai calon siswa.
	 *
	 * <p>Berperilaku simetris dengan {@link #getTampilDiFromSebelumLogin()}: {@code null}
	 * diperlakukan {@code true}, tanpa write-back, dan ditegakkan di lapis query oleh
	 * {@code ParameterTambahanPsbListener}. Kedua bendera bersifat independen &mdash; mematikan
	 * keduanya menyembunyikan parameter dari seluruh jalur formulir calon siswa, meski barisnya
	 * tetap muncul di layar master dan di layar verifikasi petugas.</p>
	 * <p>Perhatikan salah eja properti yang sudah permanen: {@code From}, bukan {@code Form}.</p>
	 *
	 * @return {@code true} bila tampil setelah login (termasuk saat nilai tersimpan {@code null})
	 */
	public Boolean getTampilDiFromSetelahLogin() {
		return tampilDiFromSetelahLogin == null ? true : tampilDiFromSetelahLogin;
	}

	/**
	 * Menyetel bendera tampil-setelah-login. Disetel dari checkbox "Tampil Setelah Login" pada
	 * renderer baris layar master, yang langsung menyimpan perubahan per klik.
	 *
	 * @param tampilDiFromSetelahLogin {@code true} agar parameter tampil pada formulir calon
	 *                                 siswa yang sudah login
	 */
	public void setTampilDiFromSetelahLogin(Boolean tampilDiFromSetelahLogin) {
		this.tampilDiFromSetelahLogin = tampilDiFromSetelahLogin;
	}

}
