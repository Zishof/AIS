package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
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

/**
 * Entity master <b>kategori (aspek) kegiatan kemahasiswaan</b>, tabel
 * {@code public.kelompok_kegiatan_kemahasiswaan}. Satu baris mewakili satu rumpun besar kegiatan
 * mahasiswa &mdash; misalnya data bawaan {@code "Keagamaan dan moral pancasila"} yang disemai
 * {@code ais.common.InitDataHelper} &mdash; yang di bawahnya digantungi rincian kegiatan konkret.
 *
 * <h2>Posisi dalam hierarki master kegiatan kemahasiswaan</h2>
 *
 * <p>Modul kegiatan kemahasiswaan memakai klasifikasi <b>tiga tingkat</b>, dan class ini berada di
 * tingkat tengah:</p>
 *
 * <ol>
 *   <li>{@link JenisKelompokKegiatanKemahasiswaan} &mdash; tingkat teratas (data bawaan
 *       {@code "Kelompok Utama"} / {@code "Kelompok Penunjang"}). Ditunjuk dari sini lewat
 *       {@link #getJenisKelompokKegiatanKemahasiswaan()} dan bersifat <b>wajib</b>;</li>
 *   <li><b>class ini</b> &mdash; rumpun/aspek kegiatan;</li>
 *   <li>{@link DetailKelompokKegiatanKemahasiswaan} &mdash; rincian di bawah baris ini (mis.
 *       {@code "PHBI"}, {@code "PHBN"}). Relasinya <b>satu arah dari anak</b>: entity anak yang
 *       menyimpan properti {@code kelompokKegiatanKemahasiswaan}; class ini <b>tidak</b> punya
 *       koleksi balik, sehingga daftar anak selalu diambil lewat query
 *       ({@code Restrictions.eq("kelompokKegiatanKemahasiswaan", ...)}).</li>
 * </ol>
 *
 * <p>Konsumen akhir hierarki ini adalah {@link KegiatanKemahasiswaan}, yang menyimpan
 * <b>dua</b> referensi sekaligus &mdash; ke class ini
 * ({@code KegiatanKemahasiswaan.getKelompokKegiatanKemahasiswaan()}) <i>dan</i> ke
 * {@link DetailKelompokKegiatanKemahasiswaan} &mdash; jadi tingkat 1 di-denormalisasi ulang di
 * baris kegiatan, bukan ditelusuri lewat anaknya. Angka kredit
 * ({@link NilaiKegiatanKemahasiswaan}) justru digantungkan ke tingkat 3, bukan ke class ini.</p>
 *
 * <h2>PERINGATAN KOSAKATA: label layar membalik istilah class</h2>
 *
 * <p>Nama class dan label UI <b>tidak sejajar</b>; jangan menyimpulkan tingkat hierarki dari kata
 * "Kelompok" pada layar:</p>
 *
 * <ul>
 *   <li>Entity ini di layar disebut <b>"Aspek Kegiatan Kemahasiswaan"</b> &mdash; judul jendela
 *       {@code "Tambah/Ubah Aspek Kegiatan Kemahasiswaan"}, kolom grid
 *       {@code "Nama Aspek Kegiatan Kemahasiswaan"}, filter {@code "Nama Aspek Kegiatan"}, dan tab
 *       induk di {@code kegiatan_kemahasiswaan.zul} berlabel {@code "Aspek Kegiatan"};</li>
 *   <li>sebutan <b>"Kelompok Kegiatan Kemahasiswaan"</b> / <b>"Kelompok Aspek"</b> di layar justru
 *       menunjuk <b>induk</b> baris ini, yaitu {@link JenisKelompokKegiatanKemahasiswaan};</li>
 *   <li>di {@link KegiatanKemahasiswaan}, combobox untuk properti ini berlabel
 *       {@code "Aspek Kegiatan *"} dan combobox untuk {@link DetailKelompokKegiatanKemahasiswaan}
 *       berlabel {@code "Rincian Aspek Kegiatan *"}.</li>
 * </ul>
 *
 * <h2>PERINGATAN NAMA: tidak ada hubungannya dengan {@link Kegiatan}/{@link DetailKegiatan}</h2>
 *
 * <p>Seperti {@link KegiatanKemahasiswaan}, class ini termasuk keluarga {@code Kegiatan*} yang
 * <b>tidak</b> berkerabat dengan {@link Kegiatan}/{@link DetailKegiatan} milik modul
 * tagihan/billing. Hasil verifikasi langsung atas kode (bukan dugaan dari nama):</p>
 *
 * <ul>
 *   <li>class ini <b>nol</b> properti dan <b>nol</b> kolom yang menunjuk {@link Kegiatan} atau
 *       {@link DetailKegiatan}; satu-satunya {@code @ManyToOne} yang ada mengarah ke
 *       {@link JenisKelompokKegiatanKemahasiswaan};</li>
 *   <li>sebaliknya juga: {@link Kegiatan} dan {@link DetailKegiatan} <b>tidak menyebut</b> class
 *       ini sama sekali (nol kemunculan identifier {@code KelompokKegiatanKemahasiswaan} di kedua
 *       berkas tersebut);</li>
 *   <li>tabelnya pun terpisah &mdash; {@code kelompok_kegiatan_kemahasiswaan} vs
 *       {@code kegiatan}/{@code detail_kegiatan}; tidak ada foreign key di antara keduanya.</li>
 * </ul>
 *
 * <p>Kerabat sesungguhnya class ini adalah padanan lintas-modulnya:
 * {@code ais.database.model.KelompokKegiatanKedosenan} (sisi dosen) dan
 * {@code ais.database.model.sekolah.KelompokKegiatanKesiswaan} (sisi sekolah/siswa) &mdash; tiga
 * salinan struktur yang nyaris identik.</p>
 *
 * <h2>Pengelompokan anggota</h2>
 *
 * <ul>
 *   <li><b>Identitas</b> &mdash; {@link #getId()}, {@link #getNama()}, {@link #getKeterangan()},
 *       {@link #toString()};</li>
 *   <li><b>Klasifikasi</b> &mdash; {@link #getJenisKelompokKegiatanKemahasiswaan()};</li>
 *   <li><b>Kendali tampil/pilih</b> &mdash; {@link #getAktif()},
 *       {@link #getBisaDipilihMahasiswa()}, {@link #getNomorUrut()};</li>
 *   <li><b>Angka penilaian (dorman)</b> &mdash; {@link #getBobot()},
 *       {@link #getNilaiMinimal()};</li>
 *   <li><b>Jejak audit</b> &mdash; {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 * </ul>
 *
 * <h2>Hal non-obvious</h2>
 *
 * <ol>
 *   <li><b>Nama kolom FK menyesatkan.</b> Relasi ke
 *       {@link JenisKelompokKegiatanKemahasiswaan} dipetakan ke kolom bernama
 *       {@code skala_kegiatan_kemahasiswaan} (lihat
 *       {@link #getJenisKelompokKegiatanKemahasiswaan()}). Padahal ada entity <i>lain</i> yang
 *       benar-benar bernama {@code SkalaKegiatanKemahasiswaan} (skala lokal/nasional/
 *       internasional), dan entity itu <b>tidak berelasi sama sekali</b> dengan class ini &mdash;
 *       skala digantungkan ke {@link DetailKelompokKegiatanKemahasiswaan} lewat many-to-many.
 *       Nama kolom di sini murni sisa salin-tempel; jangan dijadikan petunjuk semantik.</li>
 *   <li><b>{@link #getKeterangan()} membalik kontrak base class.</b>
 *       {@link GeneralValueObject#getKeterangan()} menormalkan {@code null} menjadi {@code ""};
 *       override di sini mengembalikan nilai mentah sehingga {@code null} bisa lolos ke pemanggil.
 *       Detail dan dampaknya dijelaskan di {@link #getKeterangan()}.</li>
 *   <li><b>{@link #getBobot()} dan {@link #getNilaiMinimal()} praktis write-only.</b> Keduanya
 *       bisa diisi lewat grid layar masternya, tetapi tidak ada satu pun perhitungan, laporan,
 *       atau validasi di codebase yang membacanya kembali.</li>
 *   <li><b>{@link #getAktif()} dan {@link #getBisaDipilihMahasiswa()} justru benar-benar
 *       ditegakkan</b> (kebalikan dari dua field di atas) &mdash; keduanya dipakai sebagai
 *       {@code Restrictions} saat mengisi combobox aspek kegiatan di
 *       {@code KegiatanKemahasiswaanAction}. Contoh positif.</li>
 *   <li><b>Properti induk sengaja dideklarasikan ulang.</b> {@link GeneralValueObject} adalah POJO
 *       abstrak biasa &mdash; <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} &mdash;
 *       sehingga Hibernate tidak memetakan properti apa pun miliknya. Deklarasi ulang
 *       {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} di sini adalah
 *       <b>keharusan teknis</b>, bukan duplikasi yang perlu "dibersihkan".</li>
 * </ol>
 *
 * <h2>Layar dan hak akses</h2>
 *
 * <p>Layar masternya {@code /pages/master/kelompok_kegiatan_kemahasiswaan.zul} dengan action
 * {@code ais.action.master.KelompokKegiatanKemahasiswaanAction}. Layar itu bukan menu tersendiri,
 * melainkan disisipkan sebagai tab {@code "Aspek Kegiatan"} di dalam
 * {@code kegiatan_kemahasiswaan.zul}, dan pada gilirannya menampung tab-tab bersarang untuk
 * {@link JenisKelompokKegiatanKemahasiswaan}, {@code JabatanKegiatanKemahasiswaan},
 * {@code SkalaKegiatanKemahasiswaan}, dan {@link NilaiKegiatanKemahasiswaan}.</p>
 *
 * <p><b>Catatan hak akses (dicatat apa adanya, bukan anjuran perubahan di berkas ini):</b>
 * {@code KelompokKegiatanKemahasiswaanAction} memasang gerbang READ lewat
 * {@code Common.doCheckSecurity()} di {@code doBeforeCompose}, tetapi seluruh gerbang
 * CREATE/UPDATE/DELETE-nya <b>dikomentari mati</b> ({@code CommonPrivilages.CREATE},
 * {@code UPDATE}, {@code DELETE} semuanya nonaktif). Akibatnya tombol Tambah/Ubah/Hapus, tombol
 * unggah Excel massal (visibilitasnya diikatkan ke tombol Tambah yang selalu tampil), serta
 * penyuntingan langsung di grid untuk {@link #getNomorUrut()}, {@link #getBobot()},
 * {@link #getNilaiMinimal()}, {@link #getAktif()}, dan {@link #getBisaDipilihMahasiswa()} terbuka
 * bagi siapa pun yang bisa membuka layarnya. Sebagai imbangan, pencarian di layar itu memakai
 * {@code Restrictions.ilike} terparameter &mdash; tidak ada perakitan SQL mentah dari input
 * pengguna.</p>
 *
 * @see KegiatanKemahasiswaan
 * @see JenisKelompokKegiatanKemahasiswaan
 * @see DetailKelompokKegiatanKemahasiswaan
 * @see NilaiKegiatanKemahasiswaan
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "kelompok_kegiatan_kemahasiswaan")

public class KelompokKegiatanKemahasiswaan extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya kebetulan sama persis dengan milik
	 * {@link JenisKelompokKegiatanKemahasiswaan} dan beberapa entity lain hasil salin-tempel
	 * generator; tidak ada makna khusus di balik angka ini.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Primary key baris. Lihat {@link #getId()}. */
	private Long id;

	/** Nama pengguna pengubah terakhir. Lihat {@link #getOleh()}. */
	private String oleh;

	/** Identitas (NIM/NIP/username) pengubah terakhir. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna yang terakhir mengubah baris ini (NIM/NIP/username,
	 * tergantung jenis akun), sebagaimana diisi
	 * {@code ais.database.hibernate.AuditTimestampInterceptor}.
	 *
	 * @return identitas pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel identitas pengguna pengubah terakhir, dengan <b>penjagaan anti-timpa</b>: nilai
	 * {@code null} atau string kosong/spasi diabaikan diam-diam sehingga jejak audit yang sudah
	 * ada tidak terhapus oleh proses batch atau salinan bean yang tidak membawa konteks pengguna.
	 *
	 * <p>Konsekuensinya, nilai kolom ini <b>tidak dapat dikosongkan kembali</b> lewat setter;
	 * sekali terisi, hanya bisa diganti dengan identitas lain yang tidak kosong.</p>
	 *
	 * @param olehId identitas pengubah; diabaikan bila {@code null} atau kosong/hanya spasi
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan penjagaan anti-timpa yang sama seperti
	 * {@link #setOlehId(String)}: nilai {@code null} atau kosong/hanya spasi diabaikan diam-diam.
	 *
	 * @param oleh nama pengubah; diabaikan bila {@code null} atau kosong/hanya spasi
	 * @see #setOlehId(String)
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil Hibernate <b>tepat sebelum</b> baris ini di-{@code
	 * UPDATE}, lalu meneruskan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} untuk memperbarui
	 * {@link #tanggal_dirubah} beserta {@link #oleh}/{@link #olehId} dari konteks pengguna yang
	 * sedang aktif.
	 *
	 * <p>Tidak pernah dipanggil manual dari kode aplikasi. Tidak ada pasangan
	 * {@code @PrePersist}: pada baris baru, stempel waktu berasal dari inisialisasi field
	 * {@link #tanggal_dirubah} ({@code WaktuUtil.getDate()}) yang dieksekusi saat konstruktor
	 * berjalan.</p>
	 *
	 * <p><b>Catatan format:</b> deklarasi method ini dan deklarasi field {@code tanggal_dirubah}
	 * sengaja berbagi satu baris fisik &mdash; pola salin-tempel yang sama ditemukan di ratusan
	 * entity paket ini. Jangan dipecah tanpa alasan; perubahan kosmetik pada baris ini memicu
	 * konflik di banyak sesi paralel.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi; nilai {@code null} diterima.
	 *
	 * <p>Umumnya tidak dipanggil manual &mdash; {@link #onUpdate()} yang mengisinya otomatis.</p>
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini. Dipetakan sebagai
	 * {@code TIMESTAMP}, dan karena field-nya diinisialisasi {@code WaktuUtil.getDate()} saat
	 * konstruktor berjalan, nilainya tidak pernah {@code null} untuk objek yang dibuat lewat
	 * konstruktor.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini: <b>hanya nama</b>, tanpa awalan id.
	 *
	 * <p>Berbeda dari induknya {@link JenisKelompokKegiatanKemahasiswaan} yang memakai format
	 * {@code id + "-" + nama}, dan berbeda pula dari {@link GeneralValueObject#toString()}. Nilai
	 * inilah yang muncul sebagai label item combobox {@code "Aspek Kegiatan *"} di layar
	 * {@link KegiatanKemahasiswaan}.</p>
	 *
	 * <p><b>Perhatian:</b> membaca field {@code nama} secara langsung (bukan lewat
	 * {@link #getNama()}), jadi hasilnya <b>tidak di-{@code trim}</b> dan bisa {@code null} untuk
	 * objek baru yang belum diisi &mdash; berbeda dengan nilai yang tampil lewat
	 * {@link #getNama()}.</p>
	 *
	 * @return nama aspek kegiatan apa adanya, bisa {@code null}
	 */
	public String toString() {
		return nama;
	}

	/** Nama aspek/kelompok kegiatan, wajib. Lihat {@link #getNama()}. */
	private String nama;

	/** Keterangan bebas, opsional. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Induk klasifikasi tingkat teratas, wajib. Lihat {@link #getJenisKelompokKegiatanKemahasiswaan()}. */
	private JenisKelompokKegiatanKemahasiswaan jenisKelompokKegiatanKemahasiswaan;

	/** Penanda baris masih dipakai. Lihat {@link #getAktif()}. */
	private Boolean aktif;

	/** Penanda boleh dipilih akun mahasiswa. Lihat {@link #getBisaDipilihMahasiswa()}. */
	private Boolean bisaDipilihMahasiswa;

	/** Bobot penilaian (praktis tidak terpakai). Lihat {@link #getBobot()}. */
	private Double bobot;

	/** Nilai minimal (praktis tidak terpakai). Lihat {@link #getNilaiMinimal()}. */
	private Double nilaiMinimal;

	/** Nomor urut tampil. Lihat {@link #getNomorUrut()}. */
	private Integer nomorUrut;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. Semua field dibiarkan {@code null}
	 * kecuali {@link #tanggal_dirubah} yang langsung terisi waktu saat ini lewat inisialisasi
	 * field.
	 */
	public KelompokKegiatanKemahasiswaan() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * <p>Dihasilkan database dengan strategi {@link javax.persistence.GenerationType#IDENTITY}
	 * (sequence PostgreSQL di balik kolom {@code serial}), karena itu kolomnya dipetakan
	 * {@code insertable = false}. Bernilai {@code null} selama objek belum pernah disimpan
	 * &mdash; kondisi inilah yang dipakai layar masternya untuk membedakan mode "Tambah" dari
	 * "Ubah".</p>
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
	 * Menyetel primary key. Tanpa validasi.
	 *
	 * <p>Umumnya hanya dipanggil Hibernate saat memuat/menyimpan baris; kode aplikasi sebaiknya
	 * tidak menyetel id secara manual.</p>
	 *
	 * @param id primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama aspek/kelompok kegiatan kemahasiswaan (mis.
	 * {@code "Keagamaan dan moral pancasila"}), <b>sudah di-{@code trim}</b>.
	 *
	 * <p>Kolom {@code nama} bersifat {@code nullable = false} di database, tetapi layar masternya
	 * juga memvalidasi di sisi aplikasi: nama kosong ditolak, dan nama yang sudah ada ditolak
	 * sebagai duplikat (pengecekan {@code Restrictions.eq("nama", ...)} yang <b>case-sensitive</b>
	 * dan membandingkan nilai ter-{@code trim}, sehingga {@code "Olahraga"} dan {@code "olahraga"}
	 * tetap lolos sebagai dua baris berbeda). Tidak ada {@code unique constraint} di tingkat
	 * database yang menegakkan hal ini &mdash; jalur impor Excel massal karenanya bisa memasukkan
	 * duplikat.</p>
	 *
	 * <p><b>Berbeda dari beberapa entity lain di paket ini, getter ini tidak menulis balik hasil
	 * {@code trim} ke field</b>: pembersihan hanya berlaku pada nilai kembalian, tidak memicu
	 * {@code UPDATE} tersembunyi. Sebagai efek sampingnya, {@link #toString()} (yang membaca field
	 * langsung) bisa mengembalikan teks yang masih berspasi tepi.</p>
	 *
	 * @return nama aspek kegiatan tanpa spasi tepi, atau {@code null} bila field belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama aspek/kelompok kegiatan. Tanpa validasi dan tanpa {@code trim} &mdash;
	 * pemeriksaan wajib-isi serta anti-duplikat dilakukan di layar masternya sebelum setter ini
	 * dipanggil.
	 *
	 * @param nama nama aspek kegiatan yang baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas baris ini (isian textarea 3 baris di layar masternya, dan
	 * teks deskripsi pada item combobox aspek kegiatan).
	 *
	 * <p><b>Override ini membalik kontrak base class.</b>
	 * {@link GeneralValueObject#getKeterangan()} menjanjikan hasil <i>tidak pernah</i>
	 * {@code null} ({@code null} dinormalkan menjadi {@code ""}); versi di sini mengembalikan
	 * nilai field mentah, sehingga {@code null} bisa lolos ke pemanggil. Pola yang sama muncul di
	 * sejumlah entity turunan {@code hbm2java} lain di paket ini, jadi ini variasi arsitektural
	 * yang dikenal, bukan anomali terisolasi &mdash; tetapi kode pemanggil tetap tidak boleh
	 * mengandalkan jaminan non-null milik base class saat bekerja dengan tipe ini.</p>
	 *
	 * <p>Dampak praktisnya kecil namun nyata: cabang pembanding {@code keterangan} pada
	 * {@link GeneralValueObject#compareTo(GeneralValueObject)} tidak lagi selalu memenuhi syarat
	 * non-null, sehingga dua baris yang seharusnya diurutkan lewat keterangan bisa jatuh ke
	 * hasil {@code 0} (dianggap setara).</p>
	 *
	 * @return keterangan baris ini, bisa {@code null}
	 * @see GeneralValueObject#getKeterangan()
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas. Tanpa validasi; nilai {@code null} diterima dan &mdash; berbeda
	 * dari base class &mdash; akan terbaca kembali sebagai {@code null} lewat
	 * {@link #getKeterangan()}.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan penanda apakah aspek kegiatan ini masih dipakai, dengan <b>default aman
	 * {@code true}</b>: baris lama yang kolomnya masih {@code null} (termasuk seluruh baris hasil
	 * auto-seed {@code InitDataHelper}, yang tidak pernah menyetel field ini) tetap dianggap
	 * aktif.
	 *
	 * <p>Berbeda dari {@link #getBobot()}/{@link #getNilaiMinimal()}, bendera ini
	 * <b>benar-benar ditegakkan</b>: {@code KegiatanKemahasiswaanAction} menyaring isi combobox
	 * {@code "Aspek Kegiatan *"} dengan {@code Restrictions.or(isNull("aktif"), eq("aktif",
	 * true))}, sehingga menonaktifkan satu baris langsung menyembunyikannya dari form pengajuan
	 * kegiatan baru. Penyaringan hanya berlaku untuk <i>pilihan baru</i> &mdash; kegiatan lama
	 * yang sudah terlanjur menunjuk baris nonaktif tetap tampil apa adanya.</p>
	 *
	 * <p>Di grid layar masternya, bendera ini berupa checkbox yang menyimpan perubahannya
	 * <b>seketika</b> ({@code Common.refreshSaveOrUpdate}) tanpa dialog konfirmasi.</p>
	 *
	 * @return {@code true} bila aktif atau kolomnya masih {@code null}; {@code false} bila
	 *         dinonaktifkan eksplisit
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel penanda aktif. Tanpa validasi; {@code null} diterima dan akan terbaca sebagai
	 * {@code true} lewat {@link #getAktif()}.
	 *
	 * @param aktif {@code true} bila baris masih dipakai
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan induk klasifikasi baris ini &mdash; tingkat teratas hierarki, di layar
	 * berlabel {@code "Kelompok Kegiatan Kemahasiswaan"} / tab {@code "Kelompok Aspek"} (data
	 * bawaan: {@code "Kelompok Utama"} dan {@code "Kelompok Penunjang"}).
	 *
	 * <p>Relasinya {@code @ManyToOne} wajib ({@code nullable = false}) dengan
	 * {@code CascadeType.PERSIST} + {@code MERGE} &mdash; menyimpan baris ini ikut menyimpan induk
	 * yang belum tersimpan &mdash; dan {@code FetchMode.SELECT} sehingga induk dimuat lewat query
	 * terpisah, bukan {@code JOIN}. Getter ini <b>murni baca</b>: tidak ada normalisasi maupun
	 * tulis balik.</p>
	 *
	 * <p><b>Kuirk nama kolom (penting):</b> foreign key-nya dipetakan ke kolom bernama
	 * {@code skala_kegiatan_kemahasiswaan}, bukan sesuatu seperti
	 * {@code jenis_kelompok_kegiatan_kemahasiswaan}. Nama itu <b>menyesatkan</b>: ada entity
	 * terpisah {@code SkalaKegiatanKemahasiswaan} (skala lokal/nasional/internasional) yang tidak
	 * punya relasi apa pun dengan class ini &mdash; skala digantungkan ke
	 * {@link DetailKelompokKegiatanKemahasiswaan} sebagai koleksi many-to-many. Kolom di sini
	 * hanyalah sisa salin-tempel dari generator; membaca skema database tanpa membaca anotasi ini
	 * akan menghasilkan kesimpulan yang salah.</p>
	 *
	 * <p>Di layar masternya, combobox pemilih induk disetel {@code readonly} (hanya boleh dipilih
	 * dari daftar, tidak boleh diketik bebas) dan wajib terisi sebelum tombol Simpan mau
	 * melanjutkan.</p>
	 *
	 * @return induk klasifikasi, seharusnya tidak {@code null} untuk baris yang tersimpan lewat
	 *         layar masternya
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "skala_kegiatan_kemahasiswaan", nullable = false)
	public JenisKelompokKegiatanKemahasiswaan getJenisKelompokKegiatanKemahasiswaan() {
		return jenisKelompokKegiatanKemahasiswaan;
	}

	/**
	 * Menyetel induk klasifikasi baris ini. Tanpa validasi &mdash; kewajiban isi ditegakkan di
	 * layar masternya (tombol Simpan menolak bila combobox belum dipilih) dan oleh constraint
	 * {@code NOT NULL} di database.
	 *
	 * <p>Menyetel {@code null} di sini tidak langsung gagal; kegagalan baru muncul saat
	 * {@code flush}/{@code INSERT} berupa pelanggaran constraint. Perhatikan juga jalur auto-seed
	 * {@code InitDataHelper}: di sana nilai induk diambil dari variabel lokal yang <b>hanya terisi
	 * bila tabel jenis masih kosong</b>, sehingga pada basis data yang tabel jenisnya sudah terisi
	 * tetapi tabel ini masih kosong, penyemaian akan memanggil setter ini dengan {@code null} dan
	 * gagal di flush.</p>
	 *
	 * @param jenisKelompokKegiatanKemahasiswaan induk klasifikasi tingkat teratas
	 */
	public void setJenisKelompokKegiatanKemahasiswaan(
			JenisKelompokKegiatanKemahasiswaan jenisKelompokKegiatanKemahasiswaan) {
		this.jenisKelompokKegiatanKemahasiswaan = jenisKelompokKegiatanKemahasiswaan;
	}

	/**
	 * Mengembalikan nomor urut tampil baris ini, dengan <b>default {@code 1}</b> bila kolomnya
	 * masih {@code null}.
	 *
	 * <p>Dipakai sebagai kunci pengurutan utama di layar masternya
	 * ({@code addOrder(asc("nomorUrut")).addOrder(asc("nama"))}), sehingga baris tanpa nomor urut
	 * eksplisit ikut terurut pada posisi {@code 1}. Perhatikan bahwa pengurutan itu dilakukan di
	 * <b>database</b> memakai nilai kolom apa adanya &mdash; default {@code 1} milik getter ini
	 * tidak berlaku di sana, dan baris ber-{@code NULL} akan diurutkan mengikuti aturan
	 * {@code NULLS LAST} bawaan PostgreSQL, yaitu di <i>akhir</i> daftar, bukan di posisi 1.</p>
	 *
	 * <p>Nilainya bisa disunting langsung di grid; setiap perubahan langsung disimpan
	 * ({@code Common.refreshUpdate}) tanpa konfirmasi.</p>
	 *
	 * @return nomor urut tampil, atau {@code 1} bila belum diisi
	 */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampil. Tanpa validasi; nilai {@code null}, nol, maupun negatif
	 * diterima.
	 *
	 * @param nomorUrut nomor urut baru
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Mengembalikan penanda apakah aspek kegiatan ini boleh dipilih oleh <b>akun mahasiswa</b>
	 * saat mengajukan kegiatan sendiri, dengan default {@code true} bila kolomnya masih
	 * {@code null}.
	 *
	 * <p>Ditegakkan di {@code KegiatanKemahasiswaanAction}: filter
	 * {@code or(isNull("bisaDipilihMahasiswa"), eq("bisaDipilihMahasiswa", true))} <b>hanya
	 * dipasang bila pengguna yang login adalah mahasiswa</b>; untuk operator/admin filternya
	 * diganti {@code sqlRestriction("true")} sehingga seluruh aspek tetap terlihat. Jadi bendera
	 * ini membatasi pengajuan mandiri mahasiswa, bukan visibilitas data secara umum.</p>
	 *
	 * <p><b>Efek merambat ke bawah:</b> {@link DetailKelompokKegiatanKemahasiswaan} punya bendera
	 * senama, dan getter di entity anak itu <b>memaksa hasilnya menjadi {@code false}</b> bila
	 * induknya (baris ini) mengembalikan {@code false} &mdash; tetapi hanya ketika proxy induk
	 * sudah ter-{@code initialize}. Konsekuensinya, mematikan bendera di baris ini menutup semua
	 * rinciannya di jalur yang memuat induk, sedangkan jalur yang membaca anak tanpa memuat
	 * induknya bisa memberi jawaban berbeda untuk data yang sama.</p>
	 *
	 * @return {@code true} bila mahasiswa boleh memilih aspek ini, atau kolomnya masih
	 *         {@code null}
	 */
	public Boolean getBisaDipilihMahasiswa() {
		return bisaDipilihMahasiswa == null ? true : bisaDipilihMahasiswa;
	}

	/**
	 * Menyetel penanda boleh-dipilih-mahasiswa. Tanpa validasi; {@code null} diterima dan akan
	 * terbaca sebagai {@code true}.
	 *
	 * <p>Di grid layar masternya berupa checkbox {@code "Bisa Dipilih Mahasiswa"} yang menyimpan
	 * perubahan seketika tanpa konfirmasi.</p>
	 *
	 * @param bisaDipilihMahasiswa {@code true} bila mahasiswa boleh memilih aspek ini
	 */
	public void setBisaDipilihMahasiswa(Boolean bisaDipilihMahasiswa) {
		this.bisaDipilihMahasiswa = bisaDipilihMahasiswa;
	}

	/**
	 * Mengembalikan bobot penilaian aspek kegiatan ini, dengan default {@code 0.0} bila kolomnya
	 * masih {@code null}.
	 *
	 * <p><b>Praktis write-only.</b> Nilainya bisa diisi lewat kolom "Bobot" di grid layar
	 * masternya (dan lewat impor Excel), tetapi <b>tidak ada satu pun</b> perhitungan angka
	 * kredit, laporan, dasbor, atau validasi di codebase yang membacanya kembali &mdash;
	 * satu-satunya pembaca adalah grid yang menampilkannya untuk disunting lagi. Perhitungan
	 * angka kredit kegiatan kemahasiswaan yang sesungguhnya memakai
	 * {@link NilaiKegiatanKemahasiswaan}, yang digantungkan ke
	 * {@link DetailKelompokKegiatanKemahasiswaan} (tingkat 3), bukan ke baris ini.</p>
	 *
	 * <p>Dicatat apa adanya sebagai temuan, bukan sebagai anjuran perubahan.</p>
	 *
	 * @return bobot penilaian, atau {@code 0.0} bila belum diisi
	 */
	public Double getBobot() {
		return bobot == null ? 0.0 : bobot;
	}

	/**
	 * Menyetel bobot penilaian. Tanpa validasi (nilai negatif maupun sangat besar diterima).
	 * Perubahan lewat grid langsung disimpan tanpa konfirmasi. Lihat {@link #getBobot()} untuk
	 * catatan bahwa nilai ini tidak pernah dibaca ulang oleh logika bisnis mana pun.
	 *
	 * @param bobot bobot penilaian baru
	 */
	public void setBobot(Double bobot) {
		this.bobot = bobot;
	}

	/**
	 * Mengembalikan nilai minimal aspek kegiatan ini, dengan default {@code 0.0} bila kolomnya
	 * masih {@code null}.
	 *
	 * <p><b>Praktis write-only</b>, persis seperti {@link #getBobot()}: bisa diisi lewat kolom
	 * "Nilai Min." di grid layar masternya, tetapi tidak ada logika validasi maupun perhitungan di
	 * codebase yang membandingkan capaian mahasiswa terhadap ambang ini. Tidak ada peringatan
	 * apa pun bila seorang mahasiswa berada di bawah nilai minimal yang tercatat di sini.</p>
	 *
	 * @return nilai minimal, atau {@code 0.0} bila belum diisi
	 */
	public Double getNilaiMinimal() {
		return nilaiMinimal == null ? 0.0 : nilaiMinimal;
	}

	/**
	 * Menyetel nilai minimal. Tanpa validasi, dan tanpa pemeriksaan konsistensi terhadap
	 * {@link #getBobot()}. Lihat {@link #getNilaiMinimal()} untuk catatan bahwa nilai ini tidak
	 * pernah ditegakkan.
	 *
	 * @param nilaiMinimal nilai minimal baru
	 */
	public void setNilaiMinimal(Double nilaiMinimal) {
		this.nilaiMinimal = nilaiMinimal;
	}

}
