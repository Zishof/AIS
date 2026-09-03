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

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.ParameterTambahan;

/**
 * Entity <b>penghubung</b> (lapis kedua) pada rantai <i>field kustom</i> ("parameter tambahan")
 * modul <b>SEKOLAH</b> untuk layar <b>Catatan Siswa</b>. Satu baris tabel ini menyatakan satu fakta
 * tunggal: <i>"definisi field X dipakai di bawah kategori Y pada formulir Catatan Siswa"</i>.
 *
 * <p>Kelas ini adalah padanan modul sekolah dari
 * {@link ais.database.model.ParameterTambahanCatatanMahasiswa} (versi perguruan tinggi). Keduanya
 * berbagi template hbm2java yang sama, termasuk {@code serialVersionUID} yang identik, tetapi
 * varian sekolah ini menambahkan dua kolom cakupan multi-tenant &mdash; {@link #getYayasan()} dan
 * {@link #getSekolah()} &mdash; yang tidak ada pada versi PT.</p>
 *
 * <h3>Rantai konfigurasi (4 lapis)</h3>
 * <ol>
 *   <li>{@link ParameterTambahan} &mdash; <b>definisi field generik</b>: label, tipe inputan
 *   ({@code tipeDataInputan}), nilai pilihan ({@code nilaiDataInputan}), wajib/tidak, wajib
 *   lampiran/tidak, nomor urut. Tabel ini dipakai <b>bersama oleh SELURUH modul</b> AIS (PT maupun
 *   sekolah), jadi satu definisi bisa dipetakan ke banyak layar sekaligus.</li>
 *   <li>{@link KelompokParameterTambahanCatatanSiswa} &mdash; <b>kategori/heading</b> yang menjadi
 *   judul seksi pada formulir.</li>
 *   <li><b>Kelas ini</b> &mdash; <b>penghubung</b> antara (1) dan (2). Tanpa baris di sini, sebuah
 *   definisi {@link ParameterTambahan} tidak akan pernah muncul di formulir Catatan Siswa.</li>
 *   <li>{@link JenisCatatanSiswa} &mdash; lapis <b>keempat</b> di atas semuanya: admin harus
 *   mencentang kategori mana saja ({@code @ManyToMany} ke
 *   {@link KelompokParameterTambahanCatatanSiswa}) yang berlaku untuk tiap jenis catatan. Formulir
 *   hanya membangun seksi dari kategori yang tercentang di jenis catatan yang sedang dipilih.</li>
 * </ol>
 *
 * <h3>Ke mana nilai isian pengguna disimpan</h3>
 * <p>Entity ini <b>tidak menyimpan nilai isian sama sekali</b> &mdash; ia murni konfigurasi.
 * Pemilik data sesungguhnya adalah {@link CatatanSiswa} (tabel {@code sekolah.catatan_siswa}), yang
 * menampung seluruh jawaban dalam <b>dua kolom {@code text} berformat teks</b>, ditulis berbarengan
 * oleh {@link CatatanSiswa#populateParameterTambahan(java.util.List)}:</p>
 * <ul>
 *   <li><b>{@code parameterTambahan}</b> &mdash; versi <i>berlabel</i> (untuk tampilan/laporan),
 *   satu baris per field dipisah {@code "\n"}, tiap baris <b>7 ruas</b> dipisah {@code "<=>"}:
 *   <pre>
 * namaKelompok-&gt;labelInputan &lt;=&gt; nilai &lt;=&gt; urlLampiran &lt;=&gt; nomorUrut &lt;=&gt; idParameter &lt;=&gt; idKelompok &lt;=&gt; keterangan
 *   </pre>
 *   Jumlah ruasnya <b>sama dengan</b> varian
 *   {@link ais.database.model.ParameterTambahanCatatanMahasiswa} dan seluruh sub-keluarga
 *   "Catatan*" (7 ruas, bukan 8 seperti varian Alumni/Mahasiswa biodata yang punya ruas
 *   {@code indexKe} tambahan). Pembacanya adalah
 *   {@link CatatanSiswa#ambilDataParameterTambahan()}, yang hanya memakai ruas 0&ndash;4 (label,
 *   nilai, URL, nomor urut, id parameter) dan <b>mengabaikan ruas 5&ndash;6</b>.</li>
 *   <li><b>{@code parameterTambahanInds}</b> &mdash; versi <i>ber-ID</i> (untuk mengisi ulang
 *   formulir), satu baris per field dipisah {@code "\n"}, tiap baris <b>4 ruas</b>:
 *   <pre>
 * idKelompok-&gt;idParameter &lt;=&gt; nilai &lt;=&gt; urlLampiran &lt;=&gt; keterangan
 *   </pre>
 *   Dibaca oleh {@link ais.action.master.sekolah.helper.ParameterTambahanCatatanSiswaListener}
 *   (form Tambah/Ubah) dan oleh {@code CatatanSiswaAction} (panel detail baca-saja).</li>
 * </ul>
 * <p>Kunci gabungan <b>{@code idKelompok + "->" + idParameter}</b> dipakai konsisten di tiga
 * tempat: sebagai ruas pertama {@code parameterTambahanInds}, sebagai kunci peta lampiran di
 * memori, dan sebagai argumen {@code jenis} pada
 * {@code LampiranLain.ambil(catatanSiswa.getId(), jenis)} &mdash; jadi berkas unggahan tiap field
 * disimpan di {@link ais.database.model.file.LampiranLain} dengan {@code idPemilik} = id
 * {@link CatatanSiswa} dan {@code jenis} = kunci gabungan tersebut. Mengubah id kelompok atau id
 * parameter setelah data terisi akan memutus ketiga kaitan itu sekaligus.</p>
 *
 * <h3>Bagaimana baris entity ini dibaca saat formulir dibangun</h3>
 * <p>Kedua perakit formulir ({@code ParameterTambahanCatatanSiswaListener.onEvent} dan panel detail
 * di {@code CatatanSiswaAction}) memakai query yang <b>sama persis</b>:</p>
 * <pre>
 * session.createCriteria(ParameterTambahanCatatanSiswa.class)
 *     .add(Restrictions.eq("kelompokParameterTambahanCatatanSiswa", kelompok))
 *     .createAlias("parameterTambahan", "parameterTambahan")
 *     .createAlias("kelompokParameterTambahanCatatanSiswa", "kelompokParameterTambahanCatatanSiswa")
 *     .add(Restrictions.eq("parameterTambahan.aktif", true))
 *     .add(Restrictions.eq("kelompokParameterTambahanCatatanSiswa.aktif", true))
 *     .setProjection(Projections.groupProperty("parameterTambahan.id"))
 * </pre>
 * <p>Perhatikan proyeksinya: hasil query <b>bukan</b> objek kelas ini, melainkan daftar
 * {@link ParameterTambahan} (id-nya di-{@code groupProperty}, lalu dimuat ulang oleh
 * {@code ConstantValues.simpleList(...)} dan diurutkan dengan {@code Collections.sort}). Baris
 * entity ini karena itu hanya berfungsi sebagai <b>syarat keberadaan</b> (baris ada = field ikut
 * dirender); tidak satu pun atributnya sendiri &mdash; termasuk {@link #getNomorUrut()},
 * {@link #getYayasan()}, dan {@link #getSekolah()} &mdash; ikut terbaca di jalur tampil formulir.
 * Lihat catatan pada masing-masing getter tersebut.</p>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 *   <li><b>Identitas &amp; audit:</b> {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 *   <li><b>Relasi rantai:</b> {@link #getParameterTambahan()} (definisi field),
 *   {@link #getKelompokParameterTambahanCatatanSiswa()} (kategori/heading).</li>
 *   <li><b>Cakupan multi-tenant (turunan):</b> {@link #getYayasan()}, {@link #getSekolah()}
 *   &mdash; keduanya <b>diturunkan</b> dari relasi rantai, bukan diisi pengguna.</li>
 *   <li><b>Pengurutan/denormalisasi:</b> {@link #getNomorUrut()} (salinan dari definisi).</li>
 *   <li><b>Konstruktor:</b> {@link #ParameterTambahanCatatanSiswa()}.</li>
 * </ul>
 *
 * <h3>Catatan warisan &amp; pemetaan (non-obvious)</h3>
 * <ul>
 *   <li>{@link GeneralValueObject} BUKAN {@code @Entity} maupun {@code @MappedSuperclass} &mdash; ia
 *   POJO abstrak biasa, sehingga Hibernate <b>tidak memetakan satu pun property induknya</b>. Karena
 *   itu deklarasi ULANG {@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah}, dan
 *   {@code nomorUrut} di kelas ini <b>bukan bug</b>, melainkan keharusan teknis agar kolom-kolom itu
 *   benar-benar tersimpan. Field induk yang tidak dideklarasikan ulang &mdash; {@code nama},
 *   {@code nim}, {@code keterangan} &mdash; tetap ada di memori tetapi tidak pernah terisi dari DB.</li>
 *   <li><b>Kelas ini TIDAK memiliki field {@code keterangan} sendiri.</b> {@code getKeterangan()},
 *   {@code getNama()}, {@code getNim()}, dan {@code toString()} sepenuhnya diwarisi dari
 *   {@link GeneralValueObject} dan selalu bernilai bawaan. (Pola "getKeterangan() membalik kontrak
 *   base class" karena itu <b>TIDAK ADA</b> di file ini.)</li>
 *   <li>Anotasi {@code @Id} berada pada <b>getter</b>, sehingga Hibernate memakai <i>property
 *   access</i> untuk seluruh property. Digabung dengan {@code dynamicUpdate = true}, getter yang
 *   menulis balik ke field ({@link #getNomorUrut()}, {@link #getYayasan()}, {@link #getSekolah()},
 *   dan {@code check(...)} pada getter relasi) dapat mengotori state dan memicu {@code UPDATE}
 *   beserta revisi Envers baru pada baris yang <b>sekadar dibaca</b>.</li>
 *   <li>{@code nomorUrut} tidak punya {@code @Column} eksplisit sehingga nama kolomnya mengikuti
 *   strategi penamaan bawaan Hibernate (nama property apa adanya, dilipat ke huruf kecil oleh
 *   PostgreSQL).</li>
 *   <li><b>Nama tabel &amp; kolom FK sisa template SOP.</b> Kelas ini dipetakan ke
 *   {@code sekolah.parameter_tambahan_alur_sop} dan relasi kategorinya ke kolom
 *   {@code kelompok_parameter_tambahan_alur_sop} &mdash; nama yang jelas berasal dari modul
 *   {@code sop} ({@link ais.database.model.sop.ParameterTambahanAlurSop}, tabel
 *   {@code public.parameter_tambahan_alur_sop}). <b>Tidak ada tabrakan</b> karena skemanya berbeda
 *   ({@code sekolah} vs {@code public}), tetapi perlu diketahui bahwa pasangan
 *   ini + {@link KelompokParameterTambahanCatatanSiswa} adalah <b>satu-satunya</b> pasangan di paket
 *   {@code ais.database.model.sekolah} yang masih memakai nama SOP; ketiga pasangan saudaranya
 *   (Catatan Guru, Catatan Kelas Siswa, Kegiatan Siswa, Calon Siswa) sudah diberi nama tabel yang
 *   benar. Dicatat agar tidak dikira salah relasi saat membaca skema DB mentah.</li>
 *   <li>Relasi ke {@link ParameterTambahan} {@code nullable = false}, sedangkan relasi ke kategori
 *   {@code nullable = true}. Praktisnya baris tanpa kategori adalah baris yatim: seluruh pembaca
 *   runtime menyaring dengan {@code Restrictions.eq("kelompokParameterTambahanCatatanSiswa", ...)},
 *   sehingga baris seperti itu tidak akan pernah cocok dengan kelompok mana pun &mdash; ia hanya
 *   tetap tampil di grid admin (dan renderer grid akan {@code NullPointerException} saat memanggil
 *   {@code getKelompokParameterTambahanCatatanSiswa().getNama()} pada baris tersebut).</li>
 *   <li>Ketiga relasi {@code @ManyToOne} memakai {@code cascade = {PERSIST, MERGE}}, jadi menyimpan
 *   baris ini bisa ikut mem-{@code persist}/{@code merge} master yang direferensikan.</li>
 *   <li>{@code @Audited} (Envers) aktif: setiap perubahan baris konfigurasi ini terekam di tabel
 *   audit, termasuk {@code UPDATE} tak sengaja dari getter penulis-balik di atas.</li>
 * </ul>
 *
 * <h3>Catatan keamanan pada layar pengelolanya</h3>
 * <p>Entity ini sendiri tidak menegakkan otorisasi apa pun (itu tugas lapisan Action), tetapi
 * pengelolanya {@code ais.action.master.sekolah.ParameterTambahanCatatanSiswaAction} <b>tidak
 * memiliki gerbang hak akses sama sekali</b>: field {@code edit} dan {@code delete} di-hardcode
 * {@code true} dan tidak ada satu pun pemanggilan {@code checkPrevilages} di seluruh file (557
 * baris), termasuk di {@code doAfterCompose}. Siapa pun yang bisa membuka layar tersebut dapat
 * menambah, mengubah, dan menghapus pemetaan parameter &mdash; yang berarti dapat
 * mengubah/menghilangkan field pada formulir Catatan Siswa untuk semua pengguna &mdash; serta
 * memakai tombol unggah data massal ({@code Common.uploadData}) yang dipasang tanpa syarat.
 * Kontrasnya mencolok: kelas saudara pada tab layar yang sama,
 * {@code KelompokParameterTambahanCatatanSiswaAction}, memasang gerbang
 * READ/CREATE/UPDATE/DELETE lengkap. Pola yang sama ditemukan pada seluruh keluarga
 * {@code ParameterTambahan*Action} yang sudah diperiksa; sudah dicatat sebagai temuan terpisah dan
 * <b>tidak diperbaiki di sini</b> (dokumentasi ini tidak mengubah logika kode apa pun).</p>
 *
 * @see ParameterTambahan
 * @see KelompokParameterTambahanCatatanSiswa
 * @see JenisCatatanSiswa
 * @see CatatanSiswa#populateParameterTambahan(java.util.List)
 * @see ais.action.master.sekolah.helper.ParameterTambahanCatatanSiswaListener
 * @see ais.database.model.ParameterTambahanCatatanMahasiswa
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "parameter_tambahan_alur_sop")
public class ParameterTambahanCatatanSiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Entity ini ikut diserialisasi karena disimpan sebagai atribut komponen
	 * ZK dan sebagai nilai item {@code Combobox} pada layar admin.
	 *
	 * <p>Nilainya <b>sama persis</b> dengan {@link ais.database.model.ParameterTambahanCatatanMahasiswa}
	 * dan saudara-saudaranya &mdash; sisa salin-tempel template hbm2java. Tidak berbahaya karena
	 * {@code serialVersionUID} hanya dibandingkan antar versi kelas yang sama.</p>
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
	 * <p>Perlu diingat bahwa {@link #getNomorUrut()}, {@link #getYayasan()}, dan
	 * {@link #getSekolah()} dapat mengotori field saat baris sekadar dibaca, sehingga callback ini
	 * bisa ikut terpicu pada {@code UPDATE} yang <b>tidak diminta pengguna mana pun</b> &mdash;
	 * jejak audit lalu mencatat pengguna yang kebetulan sedang membuka layar.</p>
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
	 * <p>Biasanya dipanggil otomatis lewat {@link #onUpdate()}; pemanggilan manual akan tertimpa
	 * pada {@code UPDATE} berikutnya.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (presisi {@code TIMESTAMP}).
	 *
	 * <p>Untuk baris yang belum pernah disimpan, nilainya adalah waktu pembuatan objek di JVM
	 * (inisialisasi field), bukan {@code null}.</p>
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Kategori/heading tempat field ini muncul di formulir; lihat {@link #getKelompokParameterTambahanCatatanSiswa()}. */
	private KelompokParameterTambahanCatatanSiswa kelompokParameterTambahanCatatanSiswa;
	/** Definisi field generik yang dipetakan; lihat {@link #getParameterTambahan()}. */
	private ParameterTambahan parameterTambahan;
	/** Cakupan yayasan (turunan, bukan input pengguna); lihat {@link #getYayasan()}. */
	private Yayasan yayasan;
	/** Cakupan sekolah (turunan, bukan input pengguna); lihat {@link #getSekolah()}. */
	private Sekolah sekolah;

	/** Salinan nomor urut dari {@link ParameterTambahan}; lihat {@link #getNomorUrut()}. */
	private Integer nomorUrut;

	/**
	 * Mengembalikan nomor urut tampil field ini &mdash; <b>selalu disalin ulang</b> dari
	 * {@link ParameterTambahan#getNomorUrut()} setiap kali dipanggil.
	 *
	 * <p><b>Efek samping (penting):</b> method ini bukan getter murni. Ia (a) memanggil
	 * {@link #getParameterTambahan()} yang menjalankan {@code check(...)} dan bisa mengganti
	 * instance yang tersimpan di field, lalu (b) <b>menimpa field {@code nomorUrut}</b> dengan nilai
	 * dari definisi. Karena Hibernate memakai <i>property access</i> pada entity ini dan
	 * {@code dynamicUpdate = true} aktif, membaca baris ini di dalam sesi yang aktif dapat memicu
	 * {@code UPDATE} kolom {@code nomorurut} plus revisi Envers baru meskipun tidak ada pengguna
	 * yang menyunting apa pun.</p>
	 *
	 * <p><b>Praktis tidak dipakai untuk pengurutan.</b> Seluruh pembaca runtime formulir Catatan
	 * Siswa memakai proyeksi {@code groupProperty("parameterTambahan.id")} sehingga yang mereka
	 * urutkan ({@code Collections.sort}) adalah objek {@link ParameterTambahan}, bukan objek kelas
	 * ini. Kolom ini hanya benar-benar terbaca lewat jalur cetak/unggah data generik pada layar
	 * admin ({@code Common.cetakData}/{@code Common.uploadData} dengan daftar kolom
	 * {@code {"id", "parameterTambahan", "kelompokParameterTambahanCatatanSiswa", "nomorUrut",
	 * "yayasan", "sekolah"}}).</p>
	 *
	 * @return nomor urut hasil salinan dari definisi field, atau {@code 1} bila keduanya kosong
	 *         (nilai bawaan, bukan {@code null})
	 */
	public Integer getNomorUrut() {
		parameterTambahan = getParameterTambahan();
		if (parameterTambahan != null) {
			nomorUrut = parameterTambahan.getNomorUrut();
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut lokal.
	 *
	 * <p>Nilai yang disetel akan <b>tertimpa</b> pada pemanggilan {@link #getNomorUrut()} berikutnya
	 * selama {@link #getParameterTambahan()} tidak {@code null}. Layar admin sendiri tidak
	 * menyediakan input untuk kolom ini; satu-satunya penulis praktis adalah jalur unggah data
	 * generik.</p>
	 *
	 * @param nomorUrut nomor urut yang diinginkan; boleh {@code null}
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Konstruktor kosong wajib Hibernate/JavaBean.
	 *
	 * <p>Juga dipakai layar admin untuk membuat baris baru sebelum {@code onSave()} mengisi kedua
	 * relasi rantai.</p>
	 */
	public ParameterTambahanCatatanSiswa() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * <p>Dihasilkan {@code IDENTITY} oleh PostgreSQL ({@code insertable = false}), jadi bernilai
	 * {@code null} sampai baris pertama kali di-{@code flush}.</p>
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key baris ini.
	 *
	 * <p>Umumnya hanya dipanggil Hibernate. Menyetel manual pada baris yang sudah tersimpan berisiko
	 * membuat entity terlepas dari identitasnya di sesi.</p>
	 *
	 * @param id primary key baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan <b>definisi field</b> yang dipetakan baris ini &mdash; sumber label, tipe
	 * inputan, daftar pilihan, bendera wajib-isi, dan bendera wajib-lampiran yang dipakai perakit
	 * formulir.
	 *
	 * <p>Relasi ini {@code nullable = false} di DB, jadi baris valid selalu punya definisi. Nilainya
	 * dilewatkan {@code check(...)} milik {@link GeneralValueObject} untuk meresolusi proxy lazy
	 * menjadi instance kanonik &mdash; <b>efek samping</b>: field lokal bisa terganti instance lain,
	 * yang di bawah <i>property access</i> + {@code dynamicUpdate} berpotensi ikut menandai baris
	 * sebagai kotor.</p>
	 *
	 * <p>Dipanggil dari renderer grid admin, {@link #getNomorUrut()}, {@link #getYayasan()},
	 * {@link #getSekolah()}, serta secara tidak langsung dari kedua perakit formulir lewat alias
	 * Criteria {@code "parameterTambahan"}.</p>
	 *
	 * @return definisi field generik yang dipetakan; secara praktis tidak pernah {@code null}
	 * @see GeneralValueObject#check(Object)
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
	 * <p>Dipanggil {@code ParameterTambahanCatatanSiswaAction.onSave()} dari pilihan combobox
	 * "Parameter". Mengubah nilai ini pada baris yang sudah dipakai akan memutus kaitan dengan data
	 * yang terlanjur tersimpan di {@link CatatanSiswa}, karena kunci gabungan
	 * {@code idKelompok->idParameter} ikut berubah (lihat dokumentasi kelas).</p>
	 *
	 * @param parameterTambahan definisi field generik; tidak boleh {@code null} agar {@code INSERT} berhasil
	 */
	public void setParameterTambahan(ParameterTambahan parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	/**
	 * Mengembalikan <b>kategori/heading</b> tempat field ini ditampilkan pada formulir Catatan
	 * Siswa.
	 *
	 * <p>Ini adalah sisi yang dipakai kedua perakit formulir sebagai penyaring utama
	 * ({@code Restrictions.eq("kelompokParameterTambahanCatatanSiswa", kelompok)}), dan id-nya
	 * menjadi ruas pertama kunci gabungan {@code idKelompok->idParameter}.</p>
	 *
	 * <p><b>Perhatikan pemetaan:</b> kolom FK bernama {@code kelompok_parameter_tambahan_alur_sop}
	 * &mdash; sisa salin-tempel template modul SOP, bukan salah relasi (lihat catatan pemetaan pada
	 * dokumentasi kelas). Relasi ini {@code nullable = true}, sehingga baris yatim tanpa kategori
	 * mungkin ada di DB; baris seperti itu tidak akan pernah muncul di formulir mana pun tetapi
	 * membuat renderer grid admin melempar {@code NullPointerException}.</p>
	 *
	 * <p><b>Efek samping:</b> {@code check(...)} dapat mengganti instance yang tersimpan di field
	 * (lihat {@link #getParameterTambahan()}).</p>
	 *
	 * @return kategori/heading pemilik field ini, atau {@code null} untuk baris yatim
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_parameter_tambahan_alur_sop", nullable = true)
	public KelompokParameterTambahanCatatanSiswa getKelompokParameterTambahanCatatanSiswa() {
		kelompokParameterTambahanCatatanSiswa = check(kelompokParameterTambahanCatatanSiswa);
		return kelompokParameterTambahanCatatanSiswa;
	}

	/**
	 * Menyetel kategori/heading pemilik field ini.
	 *
	 * <p>Dipanggil {@code ParameterTambahanCatatanSiswaAction.onSave()} dari pilihan combobox
	 * "Kelompok" (combobox itu di-{@code setReadonly(true)} agar hanya bisa dipilih, tidak diketik).
	 * Sama seperti {@link #setParameterTambahan(ParameterTambahan)}, mengubahnya setelah data terisi
	 * memutus kaitan dengan nilai isian dan lampiran yang sudah tersimpan.</p>
	 *
	 * @param kelompokParameterTambahanCatatanSiswa kategori/heading; boleh {@code null} (menghasilkan baris yatim)
	 */
	public void setKelompokParameterTambahanCatatanSiswa(
			KelompokParameterTambahanCatatanSiswa kelompokParameterTambahanCatatanSiswa) {
		this.kelompokParameterTambahanCatatanSiswa = kelompokParameterTambahanCatatanSiswa;
	}

	/**
	 * Menyetel cakupan yayasan baris ini.
	 *
	 * <p><b>Kuirk:</b> argumen yang {@code null} <i>atau</i> yang belum punya id (entity transient)
	 * dinormalisasi menjadi {@code null} &mdash; idiom yang dipakai seragam di seluruh modul sekolah
	 * untuk mencegah {@code TransientObjectException} saat cascade {@code PERSIST}/{@code MERGE}.
	 * Konsekuensinya, memilih yayasan yang belum tersimpan sama saja dengan mengosongkan
	 * cakupan.</p>
	 *
	 * <p>Tidak ada pemanggil di layar admin: {@code ParameterTambahanCatatanSiswaAction.onSave()}
	 * hanya menyetel kedua relasi rantai. Kolom ini praktis hanya terisi lewat penurunan otomatis di
	 * {@link #getYayasan()} atau lewat jalur unggah data generik.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau entity tanpa id akan disimpan sebagai {@code null} ("Semua")
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan cakupan yayasan baris ini, <b>diturunkan berjenjang</b> setiap kali dipanggil.
	 *
	 * <p>Urutan penentuannya:</p>
	 * <ol>
	 *   <li>resolusi proxy nilai tersimpan lewat {@code check(...)};</li>
	 *   <li>bila {@link #getSekolah()} tidak {@code null} &rarr; pakai yayasan milik sekolah itu
	 *   (menimpa nilai tersimpan);</li>
	 *   <li>selain itu, bila {@link #getParameterTambahan()} punya yayasan &rarr; pakai yayasan
	 *   definisi field.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping:</b> hasil penurunan <b>ditulis balik ke field</b>, dan cabang (2) memanggil
	 * {@link #getSekolah()} yang juga menulis balik. Di bawah <i>property access</i> +
	 * {@code dynamicUpdate = true}, sekadar membuka layar daftar bisa memicu {@code UPDATE} kolom
	 * {@code yayasan} (dan {@code sekolah}) plus revisi Envers baru tanpa ada perubahan yang
	 * diminta pengguna. Karena penurunannya deterministik dari data induk, efeknya bersifat
	 * <i>self-healing</i> &mdash; nilai manual apa pun akan terus dikembalikan ke hasil penurunan
	 * selama induknya punya yayasan.</p>
	 *
	 * <p><b>Cakupan ini TIDAK menyaring formulir.</b> Kedua perakit formulir hanya menyaring
	 * berdasarkan kategori + bendera {@code aktif}; kolom {@code yayasan}/{@code sekolah} hanya
	 * dipakai sebagai filter pada grid pencarian layar admin
	 * ({@code initCriteria()}/{@code onResetParameter()}), dan di sana pun dengan pola permisif
	 * {@code isNull(...) OR eq(...)} sehingga baris tanpa cakupan selalu ikut tampil. Pembatasan
	 * per-tenant yang efektif terjadi satu lapis di atas, lewat kategori yang dicentang pada
	 * {@link JenisCatatanSiswa}.</p>
	 *
	 * @return yayasan hasil penurunan, atau {@code null} bila seluruh sumber kosong (bermakna "Semua")
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan", nullable = true)
	public Yayasan getYayasan() {
		yayasan = check(yayasan);
		if (getSekolah() != null) {
			yayasan = getSekolah().getYayasan();
		} else if (getParameterTambahan() != null && getParameterTambahan().getYayasan() != null) {
			yayasan = getParameterTambahan().getYayasan();
		}
		return yayasan;
	}

	/**
	 * Mengembalikan cakupan sekolah baris ini, <b>diturunkan</b> dari definisi field setiap kali
	 * dipanggil.
	 *
	 * <p>Nilai tersimpan diresolusi lewat {@code check(...)}, lalu <b>ditimpa</b> oleh
	 * {@code getParameterTambahan().getSekolah()} bila definisi field punya sekolah. Bila definisi
	 * field bercakupan "Semua" ({@code null}), nilai tersimpan dipertahankan &mdash; berbeda dari
	 * {@link #getYayasan()} yang selalu punya cabang kedua.</p>
	 *
	 * <p><b>Efek samping:</b> hasil penurunan ditulis balik ke field; lihat peringatan
	 * {@code UPDATE}/Envers tak diminta pada {@link #getYayasan()}. Method ini juga dipanggil dari
	 * dalam {@link #getYayasan()}, sehingga satu pembacaan {@code yayasan} bisa mengotori dua kolom
	 * sekaligus.</p>
	 *
	 * <p>Pembaca runtime: renderer grid layar admin (menampilkan {@code "Semua"} bila {@code null})
	 * dan jalur cetak/unggah data generik. Seperti {@link #getYayasan()}, kolom ini <b>tidak</b>
	 * dipakai menyaring formulir Catatan Siswa.</p>
	 *
	 * @return sekolah hasil penurunan, atau {@code null} bila definisi field maupun nilai tersimpan kosong (bermakna "Semua")
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);

		if (getParameterTambahan() != null && getParameterTambahan().getSekolah() != null) {
			sekolah = getParameterTambahan().getSekolah();
		}

		return sekolah;
	}

	/**
	 * Menyetel cakupan sekolah baris ini.
	 *
	 * <p><b>Kuirk:</b> sama seperti {@link #setYayasan(Yayasan)}, argumen {@code null} atau entity
	 * tanpa id dinormalisasi menjadi {@code null}. Nilai yang disetel akan tertimpa pada pemanggilan
	 * {@link #getSekolah()} berikutnya bila definisi field punya sekolah sendiri.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau entity tanpa id akan disimpan sebagai {@code null} ("Semua")
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}
}
