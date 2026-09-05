package ais.database.model.sirs;

// Blueprint Integrasi SIRS — Fase 2 (Fondasi data). Entitas BARU, aditif.

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

/**
 * Kepesertaan / jaminan pasien terhadap seorang payer ({@link Asuransi}) — analog "Coverage"
 * pada FHIR. Menyimpan detail eligibilitas yang TIDAK dapat ditampung oleh {@code Pasien.asuransi}
 * atau {@code Pendaftaran.asuransi} saja: nomor kepesertaan, kelas hak rawat, faskes tingkat 1,
 * jenis peserta, masa berlaku, urutan penjamin (untuk COB/Coordination of Benefits), serta
 * status &amp; sumber verifikasi.
 *
 * <p><b>Kompatibilitas:</b> entitas ini ADITIF. {@link Asuransi} TETAP master payer tunggal dan
 * {@code Pendaftaran.asuransi} / {@code Pasien.asuransi} TETAP dipakai apa adanya. Kepesertaan ini
 * memperkaya (bukan menggantikan) data payer, dan diisi bertahap. Baris kepesertaan boleh bersifat
 * level-pasien (field {@code pendaftaran} null) atau ditangkap saat sebuah encounter tertentu
 * (field {@code pendaftaran} terisi, sekadar jejak dari mana data direkam).</p>
 *
 * <p><b>Skema:</b> tabel {@code sirs.kepesertaan_pasien}; tabel audit dibuat otomatis oleh
 * {@code hbm2ddl.auto=update} + Envers di {@code new_audit.kepesertaan_pasien__audit} (entitas
 * baru → tabel utama dan audit dibuat sekaligus saat startup). @Audited penuh, tanpa @NotAudited.</p>
 *
 * <p>Semua relasi memakai pola {@code @ManyToOne(cascade={PERSIST,MERGE}, fetch=LAZY)} yang seragam
 * dengan seluruh model SIRS (child-owned FK, tanpa @OneToMany/REMOVE). Kompatibel Java 1.6/1.7 dan
 * Hibernate 3.6.</p>
 *
 * <h2>Status pemakaian: entity TIDUR — tabelnya kosong hari ini</h2>
 *
 * <p>Penelusuran seluruh basis kode menemukan <b>satu-satunya pembaca</b> entity ini:
 * {@code ais.action.master.sirs.util.PenjaminResolver#cariKepesertaanUtama(Session, Pasien, Date)}.
 * Dan {@code PenjaminResolver} sendiri menyatakan pada javadoc kelasnya: "Belum ada pemanggil yang
 * memakainya". Rantainya karena itu buntu di dua tingkat.</p>
 *
 * <p>Yang lebih penting: <b>tidak ada satu pun penulis</b>. Tidak ada {@code Action} ZK, helper,
 * endpoint API, maupun mesin impor yang melakukan {@code new KepesertaanPasien()} atau menyimpan
 * baris ke tabel ini. Katalog menu {@code ais.common.EbisnisMenuKatalog} mendaftarkan entri
 * {@code emedik_penjamin} ("Penjamin &amp; Asuransi") yang komentarnya menyebut
 * {@code sirs/AsuransiAction + util/PenjaminResolver} — tetapi {@code AsuransiAction} mengelola
 * master {@link Asuransi}, bukan kepesertaan. Konsekuensinya: tabel
 * {@code sirs.kepesertaan_pasien} dibuat oleh {@code hbm2ddl.auto=update} saat startup dan tetap
 * kosong; {@code cariKepesertaanUtama()} selalu mengembalikan {@code null}; dan setiap logika
 * masa depan yang mengandalkannya akan diam-diam jatuh ke perilaku "UMUM".</p>
 *
 * <p><b>Satu-satunya jalur yang hari ini dapat mengisi tabel ini adalah Generic CRUD v2.</b>
 * Entity memenuhi seluruh syarat {@code GenericCrudAutoDefinitionFactory.findMappedClass()}
 * (terpetakan Hibernate, turunan {@code GeneralValueObject}, tidak abstrak, punya konstruktor
 * tanpa argumen), dan nama kelasnya tidak memuat satu pun token pada
 * {@code BLOCKED_CLASS_TOKENS}. Ia karena itu berstatus {@code FULL_CRUD}, bukan
 * {@code READ_ONLY}. Ini pisau bermata dua: di satu sisi ia memberi jalan pengisian data
 * sementara sebelum layar khusus dibangun; di sisi lain, data jaminan yang menentukan siapa
 * membayar apa dapat dibuat dan diubah lewat permukaan generik <b>tanpa pembatas baris apa pun</b>
 * (lihat bagian tenancy di bawah) dan tanpa alur persetujuan.</p>
 *
 * <h2>Tenancy: tidak ada, sama seperti seluruh modul SIRS</h2>
 *
 * <p>Entity ini tidak memiliki properti {@code satuanKerja}, {@code yayasan}, {@code sekolah},
 * maupun relasi ke {@link RumahSakit}. Ia mewarisi keadaan yang sama dengan {@link Pasien}: modul
 * SIRS memang tidak punya sumbu tenant per-baris, karena dirancang untuk deployment satu fasilitas
 * per basis data. Akibatnya {@code GenericCrudAutoEntityAdapter.scopeBindings()} menghasilkan peta
 * kosong untuk kelas ini — whitelist-nya hanya mengenal {@code yayasan}, {@code sekolah},
 * {@code program}, {@code fakultas}, {@code jurusan}, {@code satuanKerja} — sehingga
 * {@code applyScope()} tidak memasang {@code Restrictions} apa pun dan {@code validateObjectScope()}
 * lolos tanpa memeriksa apa pun. Lihat javadoc {@link Pasien} untuk uraian lengkap dan untuk alasan
 * mengapa menambahkan {@code "satker"} ke whitelist adalah perbaikan yang keliru.</p>
 *
 * <h2>Privasi</h2>
 *
 * <p>Baris ini memuat {@link #getNomorKepesertaan() nomor kepesertaan} BPJS/asuransi,
 * {@link #getJenisPeserta() jenis peserta} (yang mengungkap status sosial-ekonomi — {@code PBI}
 * berarti penerima bantuan iuran), {@link #getKelasHak() kelas hak rawat}, dan faskes tingkat 1
 * pasien. Gabungan atribut ini cukup untuk menyimpulkan keadaan ekonomi seseorang, dan nomor
 * kepesertaan adalah pengenal yang dapat dipakai lintas sistem. Perlakukan sama hati-hatinya
 * dengan {@link Pasien#getNik()}.</p>
 *
 * @see Pasien
 * @see Asuransi
 * @see Pendaftaran
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "kepesertaan_pasien")
public class KepesertaanPasien extends GeneralValueObject {

	// Jenis peserta (contoh nilai BPJS/JKN — bebas diperluas dari master di masa depan)

	/**
	 * Jenis peserta "PBI" — Penerima Bantuan Iuran: iurannya dibayar pemerintah karena peserta
	 * tergolong fakir miskin atau tidak mampu.
	 *
	 * <p>Keempat konstanta jenis peserta di sini adalah <b>{@code String} lepas</b>, bukan enum
	 * dan bukan master basis data. {@link #setJenisPeserta(String)} tidak memvalidasi nilainya,
	 * sehingga kolom {@code jenis_peserta} dapat memuat teks apa pun — termasuk variasi ejaan
	 * yang akan memecah statistik. Komentar aslinya menyatakan daftar ini "bebas diperluas dari
	 * master di masa depan"; sampai itu terjadi, pembanding sebaiknya memakai konstanta ini
	 * alih-alih literal.</p>
	 *
	 * <p><b>Catatan privasi:</b> nilai {@code "PBI"} pada praktiknya adalah penanda status
	 * ekonomi. Laporan atau ekspor yang mengelompokkan pasien berdasarkan field ini secara
	 * efektif mengelompokkan mereka berdasarkan kemampuan ekonomi — pertimbangkan siapa yang
	 * berhak melihatnya.</p>
	 */
	public static final String PESERTA_PBI = "PBI"; // Penerima Bantuan Iuran

	/**
	 * Jenis peserta "PPU" — Pekerja Penerima Upah: karyawan yang iurannya dipotong dari gaji dan
	 * ditanggung bersama pemberi kerja. Lihat {@link #PESERTA_PBI} untuk catatan tentang sifat
	 * konstanta ini.
	 */
	public static final String PESERTA_PPU = "PPU"; // Pekerja Penerima Upah

	/**
	 * Jenis peserta "PBPU" — Pekerja Bukan Penerima Upah: peserta mandiri yang membayar iuran
	 * sendiri. Lihat {@link #PESERTA_PBI}.
	 */
	public static final String PESERTA_PBPU = "PBPU"; // Pekerja Bukan Penerima Upah

	/**
	 * Jenis peserta "BP" — Bukan Pekerja: pensiunan, veteran, investor, dan kategori lain yang
	 * tidak berstatus pekerja. Lihat {@link #PESERTA_PBI}.
	 */
	public static final String PESERTA_BP = "BP"; // Bukan Pekerja

	// Sumber verifikasi eligibilitas

	/**
	 * Sumber verifikasi "MANUAL": eligibilitas dipastikan petugas dengan melihat kartu fisik atau
	 * menelepon, bukan lewat antarmuka mesin.
	 *
	 * <p>Ketiga konstanta sumber verifikasi menandai <b>seberapa dapat dipercaya</b> data pada
	 * baris ini. {@code MANUAL} berarti tidak ada bukti mesin yang dapat ditelusuri ulang —
	 * relevan ketika klaim ditolak dan pertanyaannya menjadi "siapa yang menyatakan pasien ini
	 * eligible". Sama seperti jenis peserta, nilainya tidak divalidasi setter.</p>
	 */
	public static final String VERIF_MANUAL = "MANUAL";

	/**
	 * Sumber verifikasi "VCLAIM": eligibilitas dipastikan lewat antarmuka VClaim BPJS Kesehatan
	 * (jalur rujukan dan klaim rawat tingkat lanjut). Lihat {@link #VERIF_MANUAL}.
	 */
	public static final String VERIF_VCLAIM = "VCLAIM";

	/**
	 * Sumber verifikasi "PCARE": eligibilitas dipastikan lewat antarmuka PCare BPJS Kesehatan
	 * (jalur fasilitas kesehatan tingkat pertama). Lihat {@link #VERIF_MANUAL}.
	 */
	public static final String VERIF_PCARE = "PCARE";

	/**
	 * Versi serialisasi Java.
	 *
	 * <p>Berbeda dari entity SIRS hasil {@code hbm2java} yang seragam memakai
	 * {@code 2463821577548439808L}, entity Fase 2 seperti kelas ini dan {@link AlergiPasien}
	 * memakai pola bernomor sendiri ({@code ...021L}, {@code ...022L}) — penanda bahwa keduanya
	 * ditulis tangan, bukan hasil generator.</p>
	 */
	private static final long serialVersionUID = 4820100719000000021L;

	/** Kunci utama {@code sirs.kepesertaan_pasien.id}; lihat {@link #getId()}. */
	private Long id;

	/** Field audit bayangan: id pengguna pengubah terakhir; lihat {@link #getOlehId()}. */
	private String olehId;

	/** Field audit bayangan: identitas pengguna pengubah terakhir; lihat {@link #getOleh()}. */
	private String oleh;

	/** Stempel waktu perubahan terakhir; lihat {@link #getTanggal_dirubah()}. */
	private Date tanggal_dirubah = new Date();

	/** Pasien pemilik kepesertaan ini; lihat {@link #getPasien()}. */
	private Pasien pasien;

	/** Payer/penjamin yang menerbitkan kepesertaan ini; lihat {@link #getAsuransi()}. */
	private Asuransi asuransi;

	/**
	 * Encounter tempat data ini kebetulan direkam — jejak asal, bukan pemilik.
	 * Lihat {@link #getPendaftaran()}.
	 */
	private Pendaftaran pendaftaran;

	/** Nomor kartu/kepesertaan pada payer; lihat {@link #getNomorKepesertaan()}. */
	private String nomorKepesertaan;

	/**
	 * Kategori peserta ({@link #PESERTA_PBI} dan kerabatnya); lihat {@link #getJenisPeserta()}.
	 */
	private String jenisPeserta;

	/** Kelas hak rawat inap yang dijamin; lihat {@link #getKelasHak()}. */
	private String kelasHak;

	/** Kode faskes tingkat pertama peserta; lihat {@link #getFaskesTk1Kode()}. */
	private String faskesTk1Kode;

	/** Nama faskes tingkat pertama peserta; lihat {@link #getFaskesTk1Nama()}. */
	private String faskesTk1Nama;

	/** Awal masa berlaku kepesertaan; lihat {@link #getMulaiBerlaku()}. */
	private Date mulaiBerlaku;

	/** Akhir masa berlaku kepesertaan; lihat {@link #getAkhirBerlaku()}. */
	private Date akhirBerlaku;

	/** Penanda aktif kepesertaan; default {@code TRUE}. Lihat {@link #getStatusAktif()}. */
	private Boolean statusAktif = Boolean.TRUE;

	/**
	 * Urutan penjamin untuk Coordination of Benefits; lihat {@link #getUrutanPenjamin()}.
	 */
	private Integer urutanPenjamin; // 1 = penjamin utama, 2 = penjamin kedua (COB)

	/** Penanda keikutsertaan skema COB; default {@code FALSE}. Lihat {@link #getCob()}. */
	private Boolean cob = Boolean.FALSE; // Coordination of Benefits

	/**
	 * Kanal yang dipakai memverifikasi eligibilitas ({@link #VERIF_MANUAL} dan kerabatnya);
	 * lihat {@link #getSumberVerifikasi()}.
	 */
	private String sumberVerifikasi;

	/** Waktu verifikasi eligibilitas terakhir; lihat {@link #getTanggalVerifikasi()}. */
	private Date tanggalVerifikasi;

	/** Catatan bebas; lihat {@link #getKeterangan()}. */
	private String keterangan;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate.
	 *
	 * <p>Kehadirannya juga membuat {@code GenericCrudAutoDefinitionFactory.hasDefaultConstructor()}
	 * bernilai {@code true}, sehingga entity ini dapat dibuat lewat CRUD generik — hari ini
	 * satu-satunya jalur yang benar-benar dapat mengisi tabel ini. Lihat bagian "entity tidur"
	 * pada javadoc kelas.</p>
	 */
	public KepesertaanPasien() {
	}

	/**
	 * Representasi teks kepesertaan: {@code "<nama payer> <nomor kepesertaan>"}, sudah di-trim.
	 *
	 * <p><b>Membaca field langsung, bukan lewat getter.</b> Method memakai {@code asuransi} dan
	 * {@code nomorKepesertaan} apa adanya alih-alih memanggil {@link #getAsuransi()}. Ini
	 * disengaja dan benar untuk {@code toString()}: memanggil getter akan memicu
	 * {@code check(...)} yang dapat membuka session baru dan melakukan query — perilaku yang
	 * sangat tidak diinginkan pada method yang kerap dipanggil dari logger, debugger, atau
	 * penangan pengecualian. Harganya, bila {@code asuransi} masih berupa proxy lazy yang belum
	 * terinisialisasi, {@code asuransi.getNama()} tetap bisa melempar
	 * {@code LazyInitializationException} — sekadar tidak menambah query baru.</p>
	 *
	 * <p>Bila kedua komponen kosong, hasilnya string kosong (bukan {@code null}), sehingga aman
	 * dipakai sebagai label komponen ZK.</p>
	 *
	 * @return {@code "<payer> <nomor>"} yang sudah di-trim; string kosong bila keduanya belum diisi
	 */
	public String toString() {
		String payer = asuransi == null ? "" : asuransi.getNama();
		String no = nomorKepesertaan == null ? "" : nomorKepesertaan;
		return (payer + " " + no).trim();
	}

	/**
	 * Mengembalikan kunci utama baris. {@code IDENTITY}, karena itu {@code insertable = false}.
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
	 * Menyetel kunci utama; untuk kerangka persistensi.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan id pengguna pengubah terakhir.
	 *
	 * <p>Bagian dari trio audit bayangan yang wajib diulang di setiap entity karena
	 * {@link GeneralValueObject} bukan {@code @Entity}/{@code @MappedSuperclass} — keharusan
	 * teknis, bukan duplikasi ceroboh. Lihat {@link Pasien#getOlehId()}.</p>
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null}
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir, menolak nilai {@code null}/blank secara diam-diam
	 * agar jejak audit lama tidak terhapus tanpa sengaja.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau blank
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengembalikan identitas pengguna pengubah terakhir.
	 *
	 * @return identitas pengguna, atau {@code null}
	 * @see #getOlehId()
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menyetel identitas pengguna pengubah terakhir, menolak nilai kosong secara diam-diam.
	 *
	 * @param oleh identitas pengguna; diabaikan bila {@code null} atau blank
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: memperbarui stempel waktu audit tepat sebelum
	 * {@code UPDATE} dikirim, dengan mendelegasikan ke
	 * {@code AuditTimestampInterceptor.ubah(this)}. Berjalan hanya pada {@code UPDATE}, bukan
	 * {@code INSERT}. Jangan dipanggil manual.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris; dipetakan {@code TIMESTAMP}.
	 * Nama properti {@code snake_case} mengikuti konvensi seluruh model AIS — jangan diganti.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Menyetel stempel waktu perubahan terakhir; umumnya dipanggil
	 * {@code AuditTimestampInterceptor}, bukan kode aplikasi.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan pasien pemilik kepesertaan ini, meresolusi proxy lazy lebih dulu.
	 *
	 * <p>Relasi ini adalah inti entity: satu pasien boleh punya banyak baris kepesertaan
	 * sekaligus (beberapa payer, atau riwayat kepesertaan yang sudah berakhir). Karena itulah
	 * kelas ini ada — {@link Pasien#getAsuransi()} hanya menampung satu payer.</p>
	 *
	 * <p><b>Kolom {@code nullable = true} adalah kelemahan integritas.</b> Kepesertaan tanpa
	 * pasien tidak bermakna apa pun, tetapi skema mengizinkannya dan tidak ada validasi di
	 * entity. Nullable di sini mengikuti pola seragam model SIRS (hampir seluruh FK dibuat
	 * nullable agar {@code hbm2ddl.auto=update} tidak gagal pada tabel warisan yang sudah berisi
	 * data), bukan karena baris tanpa pasien memang diinginkan.
	 * {@code PenjaminResolver.cariKepesertaanUtama()} menyaring dengan
	 * {@code Restrictions.eq("pasien", pasien)} sehingga baris yatim tidak akan pernah terambil
	 * — ia sekadar menumpuk tanpa terlihat.</p>
	 *
	 * <p>Cascade {@code {PERSIST, MERGE}} tanpa {@code REMOVE}: menyimpan kepesertaan dapat
	 * menyimpan pasien yang belum tersimpan, tetapi menghapus kepesertaan tidak pernah menyentuh
	 * baris pasien.</p>
	 *
	 * @return pasien pemilik, atau {@code null} pada baris yatim
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pasien", nullable = true)
	public Pasien getPasien() {
		pasien = check(pasien);
		return pasien;
	}

	/**
	 * Menyetel pasien pemilik kepesertaan.
	 *
	 * @param pasien pasien pemilik
	 */
	public void setPasien(Pasien pasien) {
		this.pasien = pasien;
	}

	/**
	 * Mengembalikan payer/penjamin yang menerbitkan kepesertaan ini, meresolusi proxy lazy
	 * lebih dulu.
	 *
	 * <p>{@link Asuransi} tetap master payer <b>tunggal</b> di seluruh modul SIRS — BPJS bukan
	 * entity terpisah melainkan salah satu baris {@link Asuransi} dengan jenis payer tertentu.
	 * Rancangan itu disengaja dan dijelaskan pada javadoc {@code PenjaminResolver}: menambahkan
	 * entity khusus BPJS akan memaksa percabangan di enam titik FK {@link Asuransi} yang sudah
	 * ada ({@code Pasien.asuransi}, {@code Pendaftaran.asuransi},
	 * {@code BookingRegistrasi.asuransi}, {@code TarifKhusus/Diskon/PajakMedis.asuransi}).</p>
	 *
	 * @return payer penerbit kepesertaan, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "asuransi", nullable = true)
	public Asuransi getAsuransi() {
		asuransi = check(asuransi);
		return asuransi;
	}

	/**
	 * Menyetel payer/penjamin penerbit kepesertaan.
	 *
	 * @param asuransi payer penerbit
	 */
	public void setAsuransi(Asuransi asuransi) {
		this.asuransi = asuransi;
	}

	/**
	 * Mengembalikan encounter tempat data kepesertaan ini kebetulan direkam, meresolusi proxy
	 * lazy lebih dulu — <b>jejak asal, bukan pemilik</b>.
	 *
	 * <p>Javadoc kelas menyatakan maksudnya dengan jelas: baris kepesertaan boleh bersifat
	 * level-pasien ({@code pendaftaran} {@code null}) atau ditangkap saat sebuah encounter
	 * tertentu ({@code pendaftaran} terisi, "sekadar jejak dari mana data direkam"). Perbedaan
	 * ini penting untuk konsumen: kepesertaan yang terisi {@code pendaftaran} <b>tetap berlaku
	 * untuk kunjungan lain</b> pasien yang sama. Menyaring kepesertaan berdasarkan encounter
	 * yang sedang berjalan akan salah — pasien akan tampak tidak punya jaminan pada setiap
	 * kunjungan berikutnya.</p>
	 *
	 * <p>{@code PenjaminResolver.cariKepesertaanUtama()} sudah benar dalam hal ini: ia menyaring
	 * hanya dengan {@code pasien} dan menyaring masa berlaku lewat {@link #berlakuPada(Date)},
	 * tanpa menyentuh field ini.</p>
	 *
	 * @return encounter asal perekaman, atau {@code null} untuk kepesertaan level-pasien
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendaftaran", nullable = true)
	public Pendaftaran getPendaftaran() {
		pendaftaran = check(pendaftaran);
		return pendaftaran;
	}

	/**
	 * Menyetel encounter asal perekaman kepesertaan.
	 *
	 * @param pendaftaran encounter asal, atau {@code null} untuk kepesertaan level-pasien
	 */
	public void setPendaftaran(Pendaftaran pendaftaran) {
		this.pendaftaran = pendaftaran;
	}

	/**
	 * Mengembalikan nomor kartu/kepesertaan pasien pada payer ini.
	 *
	 * <p>Untuk BPJS, ini nomor kartu JKN 13 digit — nilai yang juga dapat disimpan di
	 * {@link Pasien#getNoKartuBpjs()}. <b>Dua tempat untuk data yang sama adalah sumber
	 * ketidaksesuaian</b>, dan javadoc {@link Pasien#getNoKartuBpjs()} sudah menandai bahwa
	 * field di sini seharusnya menjadi sumber kebenaran karena ia disertai masa berlaku, kelas
	 * hak, urutan penjamin, dan status verifikasi. Tidak ada mekanisme sinkronisasi antara
	 * keduanya; bila kelak layar pengisian kepesertaan dibangun, ia perlu memutuskan secara
	 * eksplisit apakah menulis balik ke {@link Pasien} atau membiarkan field lama membeku
	 * sebagai data warisan.</p>
	 *
	 * <p>Kolom dibatasi 30 karakter, nullable, tanpa validasi format maupun penjaga keunikan.
	 * Dua baris berbeda dapat menyimpan nomor kepesertaan yang sama untuk pasien yang
	 * berbeda — deteksi duplikasi harus dilakukan di lapisan aplikasi.</p>
	 *
	 * @return nomor kepesertaan, atau {@code null}
	 */
	@Column(name = "nomor_kepesertaan", nullable = true, length = 30)
	public String getNomorKepesertaan() {
		return nomorKepesertaan;
	}

	/**
	 * Menyetel nomor kartu/kepesertaan pada payer. Tanpa validasi format.
	 *
	 * @param nomorKepesertaan nomor kepesertaan
	 */
	public void setNomorKepesertaan(String nomorKepesertaan) {
		this.nomorKepesertaan = nomorKepesertaan;
	}

	/**
	 * Mengembalikan kategori peserta.
	 *
	 * <p>Nilai yang diharapkan adalah salah satu {@link #PESERTA_PBI}, {@link #PESERTA_PPU},
	 * {@link #PESERTA_PBPU}, atau {@link #PESERTA_BP} — tetapi tidak ada penjaga apa pun,
	 * sehingga pemanggil harus memperlakukan nilai tak dikenal sebagai "tidak diketahui".
	 * Lihat {@link #PESERTA_PBI} untuk catatan privasi: kategori ini mengungkap status
	 * sosial-ekonomi pasien.</p>
	 *
	 * @return kategori peserta, atau {@code null}
	 */
	@Column(name = "jenis_peserta", nullable = true, length = 40)
	public String getJenisPeserta() {
		return jenisPeserta;
	}

	/**
	 * Menyetel kategori peserta. Tanpa validasi terhadap konstanta {@code PESERTA_*}.
	 *
	 * @param jenisPeserta kategori peserta
	 */
	public void setJenisPeserta(String jenisPeserta) {
		this.jenisPeserta = jenisPeserta;
	}

	/**
	 * Mengembalikan kelas hak rawat inap yang dijamin payer (mis. {@code "1"}, {@code "2"},
	 * {@code "3"}, {@code "VIP"}).
	 *
	 * <p>Nilai ini menentukan sampai kelas berapa biaya kamar ditanggung penjamin; selisih ke
	 * kelas yang lebih tinggi menjadi tanggungan pasien. Disimpan sebagai teks bebas 15
	 * karakter, <b>tidak</b> ditautkan ke master {@link KelasPerawatan} milik modul ini —
	 * sehingga membandingkan hak kelas dengan kamar yang ditempati memerlukan pemetaan teks
	 * di lapisan aplikasi, dengan segala risiko variasi ejaannya.</p>
	 *
	 * @return kelas hak rawat, atau {@code null}
	 */
	@Column(name = "kelas_hak", nullable = true, length = 15)
	public String getKelasHak() {
		return kelasHak;
	}

	/**
	 * Menyetel kelas hak rawat inap yang dijamin.
	 *
	 * @param kelasHak kelas hak (teks bebas)
	 */
	public void setKelasHak(String kelasHak) {
		this.kelasHak = kelasHak;
	}

	/**
	 * Mengembalikan kode faskes tingkat pertama tempat peserta terdaftar.
	 *
	 * <p>Bersama {@link #getFaskesTk1Nama()}, field ini menyimpan <b>salinan denormalisasi</b>
	 * data faskes yang datang dari respons VClaim/PCare, bukan relasi ke entity mana pun. Itu
	 * disengaja: faskes tingkat pertama pasien umumnya berada di luar rumah sakit ini, sehingga
	 * tidak ada baris master lokal yang bisa dirujuk. Konsekuensinya, kode dan nama dapat
	 * menjadi tidak sinkron bila salah satu diperbarui tanpa yang lain.</p>
	 *
	 * @return kode faskes tingkat 1, atau {@code null}
	 */
	@Column(name = "faskes_tk1_kode", nullable = true, length = 30)
	public String getFaskesTk1Kode() {
		return faskesTk1Kode;
	}

	/**
	 * Menyetel kode faskes tingkat pertama peserta.
	 *
	 * @param faskesTk1Kode kode faskes tingkat 1
	 */
	public void setFaskesTk1Kode(String faskesTk1Kode) {
		this.faskesTk1Kode = faskesTk1Kode;
	}

	/**
	 * Mengembalikan nama faskes tingkat pertama peserta — salinan denormalisasi; lihat
	 * {@link #getFaskesTk1Kode()}.
	 *
	 * @return nama faskes tingkat 1, atau {@code null}
	 */
	@Column(name = "faskes_tk1_nama", nullable = true, length = 100)
	public String getFaskesTk1Nama() {
		return faskesTk1Nama;
	}

	/**
	 * Menyetel nama faskes tingkat pertama peserta.
	 *
	 * @param faskesTk1Nama nama faskes tingkat 1
	 */
	public void setFaskesTk1Nama(String faskesTk1Nama) {
		this.faskesTk1Nama = faskesTk1Nama;
	}

	/**
	 * Mengembalikan awal masa berlaku kepesertaan.
	 *
	 * <p>Dipetakan {@code TemporalType.DATE} (tanpa jam), nullable. Nilai {@code null} berarti
	 * "tidak dibatasi di sisi awal" dan diperlakukan demikian oleh {@link #berlakuPada(Date)}.
	 * Pasangannya adalah {@link #getAkhirBerlaku()}.</p>
	 *
	 * @return tanggal awal berlaku, atau {@code null} bila tidak dibatasi
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "mulai_berlaku", nullable = true)
	public Date getMulaiBerlaku() {
		return mulaiBerlaku;
	}

	/**
	 * Menyetel awal masa berlaku kepesertaan. Tidak ada validasi bahwa nilainya lebih awal dari
	 * {@link #getAkhirBerlaku()}.
	 *
	 * @param mulaiBerlaku tanggal awal berlaku
	 */
	public void setMulaiBerlaku(Date mulaiBerlaku) {
		this.mulaiBerlaku = mulaiBerlaku;
	}

	/**
	 * Mengembalikan akhir masa berlaku kepesertaan; {@code null} berarti tidak dibatasi di sisi
	 * akhir. Lihat {@link #getMulaiBerlaku()} dan {@link #berlakuPada(Date)}.
	 *
	 * @return tanggal akhir berlaku, atau {@code null} bila tidak dibatasi
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "akhir_berlaku", nullable = true)
	public Date getAkhirBerlaku() {
		return akhirBerlaku;
	}

	/**
	 * Menyetel akhir masa berlaku kepesertaan. Tidak ada validasi terhadap
	 * {@link #getMulaiBerlaku()} — rentang terbalik akan diterima dan membuat
	 * {@link #berlakuPada(Date)} selalu bernilai {@code false}.
	 *
	 * @param akhirBerlaku tanggal akhir berlaku
	 */
	public void setAkhirBerlaku(Date akhirBerlaku) {
		this.akhirBerlaku = akhirBerlaku;
	}

	/**
	 * Mengembalikan penanda aktif kepesertaan, <b>menormalkan {@code null} menjadi
	 * {@code TRUE}</b>.
	 *
	 * <p>Normalisasi ini berbeda dari {@link Pasien#getAktif()} yang mengembalikan {@code null}
	 * apa adanya, dan pilihannya di sini disengaja: baris kepesertaan yang kolom
	 * {@code status_aktif}-nya belum pernah diisi harus dianggap <b>berlaku</b>, bukan
	 * tidak-berlaku. Menganggapnya tidak aktif berarti kepesertaan sah diam-diam berhenti
	 * menjamin pasien — kegagalan yang merugikan pasien dan baru ketahuan saat klaim ditolak.
	 * Getter ini <b>tidak</b> menulis kembali ke field; hanya nilai kembaliannya yang
	 * dinormalkan, sehingga tidak ada efek samping persistensi.</p>
	 *
	 * <p>Perhatikan bahwa "aktif" di sini <b>bukan</b> flag soft delete seperti
	 * {@link Pasien#getAktif()}. Ia menyatakan status kepesertaan pada payer (kartu masih
	 * berlaku vs sudah dicabut/menunggak). Namun karena namanya diawali {@code status} —
	 * bukan {@code aktif} — {@code GenericCrudAutoDefinitionFactory.hasBooleanProperty(metadata,
	 * "aktif")} <b>tidak</b> menemukannya, sehingga entity ini dinyatakan <i>tidak</i>
	 * soft-deletable oleh CRUD generik. Kebetulan yang menguntungkan: penghapusan lewat CRUD
	 * generik menjadi tidak tersedia untuk kelas ini.</p>
	 *
	 * @return {@code TRUE} atau {@code FALSE}; tidak pernah {@code null}
	 */
	@Column(name = "status_aktif", nullable = true)
	public Boolean getStatusAktif() {
		return statusAktif == null ? Boolean.TRUE : statusAktif;
	}

	/**
	 * Menyetel penanda aktif kepesertaan. Menerima {@code null}, yang akan dibaca sebagai
	 * {@code TRUE} oleh {@link #getStatusAktif()}.
	 *
	 * @param statusAktif status aktif kepesertaan
	 */
	public void setStatusAktif(Boolean statusAktif) {
		this.statusAktif = statusAktif;
	}

	/**
	 * Mengembalikan urutan penjamin untuk skema Coordination of Benefits.
	 *
	 * <p>Konvensinya: {@code 1} = penjamin utama, {@code 2} = penjamin kedua, dan seterusnya.
	 * Nilai ini menentukan urutan penagihan — penjamin kedua hanya menanggung sisa setelah
	 * penjamin utama membayar bagiannya.</p>
	 *
	 * <p><b>Tidak ada penjaga keunikan.</b> Dua baris kepesertaan aktif untuk pasien yang sama
	 * dapat sama-sama bernilai {@code 1}, dan tidak ada constraint basis data maupun validasi
	 * entity yang mencegahnya.
	 * {@code PenjaminResolver.cariKepesertaanUtama()} mengurutkan hasilnya lalu mengambil yang
	 * pertama, sehingga pada keadaan itu penjamin mana yang terpilih bergantung pada urutan
	 * sekunder — tidak deterministik secara bisnis. Bila layar pengisian kepesertaan kelak
	 * dibangun, penjaga "hanya satu penjamin utama aktif per pasien" perlu ditambahkan di sana,
	 * dan idealnya diperkuat indeks unik parsial di basis data.</p>
	 *
	 * <p>Nullable: kepesertaan tunggal yang bukan bagian skema COB boleh membiarkannya kosong.</p>
	 *
	 * @return urutan penjamin, atau {@code null}
	 */
	@Column(name = "urutan_penjamin", nullable = true)
	public Integer getUrutanPenjamin() {
		return urutanPenjamin;
	}

	/**
	 * Menyetel urutan penjamin. Tanpa validasi keunikan per pasien — lihat
	 * {@link #getUrutanPenjamin()}.
	 *
	 * @param urutanPenjamin urutan penjamin ({@code 1} = utama)
	 */
	public void setUrutanPenjamin(Integer urutanPenjamin) {
		this.urutanPenjamin = urutanPenjamin;
	}

	/**
	 * Mengembalikan penanda apakah kepesertaan ini bagian dari skema Coordination of Benefits,
	 * menormalkan {@code null} menjadi {@code FALSE}.
	 *
	 * <p>Arah normalisasinya kebalikan dari {@link #getStatusAktif()}, dan sekali lagi itu
	 * disengaja: bila tidak dinyatakan, anggap tidak ada skema COB. Menganggap sebaliknya akan
	 * membuat penagihan mencari penjamin kedua yang tidak pernah ada.</p>
	 *
	 * <p>Field ini secara semantik <b>redundan</b> terhadap {@link #getUrutanPenjamin()}:
	 * kepesertaan dengan urutan ≥ 2 pada dasarnya sudah menyatakan adanya COB. Menyimpan
	 * keduanya membuka kemungkinan tidak konsisten ({@code cob = false} tetapi
	 * {@code urutanPenjamin = 2}), dan tidak ada validasi silang di entity maupun basis data.
	 * Konsumen sebaiknya memperlakukan {@link #getUrutanPenjamin()} sebagai sumber kebenaran
	 * dan field ini sebagai penanda kemudahan saja.</p>
	 *
	 * @return {@code TRUE} atau {@code FALSE}; tidak pernah {@code null}
	 */
	@Column(name = "cob", nullable = true)
	public Boolean getCob() {
		return cob == null ? Boolean.FALSE : cob;
	}

	/**
	 * Menyetel penanda skema COB. Tidak divalidasi silang terhadap
	 * {@link #getUrutanPenjamin()}.
	 *
	 * @param cob {@code true} bila bagian skema COB
	 */
	public void setCob(Boolean cob) {
		this.cob = cob;
	}

	/**
	 * Mengembalikan kanal yang dipakai memverifikasi eligibilitas kepesertaan.
	 *
	 * <p>Nilai yang diharapkan salah satu {@link #VERIF_MANUAL}, {@link #VERIF_VCLAIM}, atau
	 * {@link #VERIF_PCARE}. Bersama {@link #getTanggalVerifikasi()}, field ini menjawab
	 * pertanyaan yang selalu muncul saat klaim ditolak: siapa dan berdasarkan apa pasien ini
	 * dinyatakan eligible, dan kapan. {@code MANUAL} menandai tidak adanya bukti mesin yang
	 * dapat ditelusuri ulang.</p>
	 *
	 * <p>Tidak ada validasi terhadap konstanta, dan tidak ada mekanisme yang mengisi field ini
	 * secara otomatis — integrasi VClaim/PCare belum terpasang di basis kode ini.</p>
	 *
	 * @return sumber verifikasi, atau {@code null} bila belum pernah diverifikasi
	 */
	@Column(name = "sumber_verifikasi", nullable = true, length = 30)
	public String getSumberVerifikasi() {
		return sumberVerifikasi;
	}

	/**
	 * Menyetel kanal verifikasi eligibilitas.
	 *
	 * @param sumberVerifikasi sumber verifikasi
	 */
	public void setSumberVerifikasi(String sumberVerifikasi) {
		this.sumberVerifikasi = sumberVerifikasi;
	}

	/**
	 * Mengembalikan waktu verifikasi eligibilitas terakhir.
	 *
	 * <p>Dipetakan {@code TIMESTAMP} — berbeda dari {@link #getMulaiBerlaku()} dan
	 * {@link #getAkhirBerlaku()} yang {@code DATE}. Perbedaan itu masuk akal: masa berlaku
	 * adalah urusan hari, sedangkan verifikasi adalah peristiwa yang jamnya penting bila terjadi
	 * beberapa kali dalam sehari.</p>
	 *
	 * <p>Field ini <b>tidak</b> ikut dipertimbangkan {@link #berlakuPada(Date)}: kepesertaan yang
	 * verifikasinya sudah sangat lama tetap dianggap berlaku selama masa berlakunya belum
	 * lewat. Kebijakan "verifikasi kedaluwarsa setelah N hari" bila diperlukan harus
	 * ditegakkan pemanggil.</p>
	 *
	 * @return waktu verifikasi terakhir, atau {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_verifikasi", nullable = true)
	public Date getTanggalVerifikasi() {
		return tanggalVerifikasi;
	}

	/**
	 * Menyetel waktu verifikasi eligibilitas terakhir.
	 *
	 * @param tanggalVerifikasi waktu verifikasi
	 */
	public void setTanggalVerifikasi(Date tanggalVerifikasi) {
		this.tanggalVerifikasi = tanggalVerifikasi;
	}

	/**
	 * Mengembalikan catatan bebas tentang kepesertaan ini.
	 *
	 * @return keterangan, atau {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel catatan bebas tentang kepesertaan.
	 *
	 * @param keterangan catatan bebas
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * True bila kepesertaan aktif &amp; (bila diisi) tanggal sekarang berada dalam masa berlaku.
	 *
	 * <p>Satu-satunya logika bisnis pada entity ini, dan satu-satunya method yang benar-benar
	 * dipanggil dari luar (oleh {@code PenjaminResolver.cariKepesertaanUtama()}). Urutan
	 * pemeriksaannya:</p>
	 *
	 * <ol>
	 *   <li>{@link #getStatusAktif()} harus {@code true}. Karena getter itu menormalkan
	 *       {@code null} menjadi {@code TRUE}, baris warisan yang kolomnya belum terisi lolos
	 *       tahap ini — pilihan yang berpihak pada pasien.</li>
	 *   <li>Argumen {@code null} langsung menghasilkan {@code true}. Semantiknya: "tanpa tanggal
	 *       acuan, jangan menolak" — pemanggil yang tidak peduli tanggal cukup mengirim
	 *       {@code null} untuk memeriksa status aktif saja.</li>
	 *   <li>{@link #getMulaiBerlaku()} bila terisi: tanggal acuan tidak boleh
	 *       {@code before} nilainya.</li>
	 *   <li>{@link #getAkhirBerlaku()} bila terisi: tanggal acuan tidak boleh
	 *       {@code after} nilainya.</li>
	 * </ol>
	 *
	 * <p><b>Perangkap presisi waktu.</b> Method memakai {@code Date.before}/{@code Date.after}
	 * yang membandingkan sampai <b>milidetik</b>, sedangkan {@code mulaiBerlaku} dan
	 * {@code akhirBerlaku} dipetakan {@code TemporalType.DATE} sehingga terbaca dari basis data
	 * dengan komponen jam nol (tengah malam). Bila pemanggil mengirim {@code new Date()} — waktu
	 * saat ini, lengkap dengan jam — maka pada <b>hari terakhir</b> masa berlaku,
	 * {@code tanggal.after(akhirBerlaku)} bernilai {@code true} untuk setiap saat setelah pukul
	 * 00:00:00.000. Artinya kepesertaan yang seharusnya masih berlaku sepanjang hari terakhir
	 * <b>ditolak sejak dini hari</b>. Kesalahan sehari ini merugikan pasien: klaim pada hari
	 * terakhir kepesertaan akan diperlakukan sebagai pasien umum. Gejala serupa muncul pada hari
	 * <i>pertama</i>, tetapi di sana arahnya tidak merugikan ({@code tanggal.before(mulaiBerlaku)}
	 * bernilai {@code false} untuk waktu setelah tengah malam, sehingga hari pertama tetap
	 * diterima).</p>
	 *
	 * <p>Karena entity ini belum punya data (lihat bagian "entity tidur" pada javadoc kelas),
	 * cacat batas ini <b>belum pernah berdampak nyata</b>. Ia perlu ditambal sebelum layar
	 * pengisian kepesertaan diaktifkan — perbaikannya sederhana: normalkan tanggal acuan ke awal
	 * hari sebelum membandingkan, atau bandingkan {@code akhirBerlaku} dengan akhir hari.</p>
	 *
	 * <p>Method ini {@code public} tanpa anotasi {@code @Transient}, tetapi itu aman: Hibernate
	 * hanya memetakan properti yang mengikuti konvensi JavaBean, dan {@code berlakuPada(Date)}
	 * menerima argumen sehingga bukan getter. Bandingkan dengan {@link AlergiPasien#isAktif()}
	 * yang tanpa argumen dan karena itu <b>wajib</b> diberi {@code @Transient}.</p>
	 *
	 * @param tanggal tanggal acuan; {@code null} berarti "abaikan pemeriksaan masa berlaku"
	 * @return {@code true} bila kepesertaan aktif dan tanggal acuan berada dalam masa berlaku
	 */
	public boolean berlakuPada(Date tanggal) {
		if (!getStatusAktif().booleanValue()) {
			return false;
		}
		if (tanggal == null) {
			return true;
		}
		if (mulaiBerlaku != null && tanggal.before(mulaiBerlaku)) {
			return false;
		}
		if (akhirBerlaku != null && tanggal.after(akhirBerlaku)) {
			return false;
		}
		return true;
	}
}
