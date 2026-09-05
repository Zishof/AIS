package ais.database.model.rab;

// Generated Dec 19, 2009 10:58:09 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.model.GeneralValueObject;

/**
 * Master satuan ukur atomik RAB (Rencana Anggaran Biaya) — satu baris mewakili SATU unit ukur
 * tunggal seperti "Orang", "Bulan", "Paket", "Unit", "Dokumen", dsb. (nilai konkretnya
 * data-driven, dipelihara pengguna lewat layar CRUD {@code ais.action.master.rab.SatuanAction},
 * bukan enum hardcode). Dipakai luas sebagai satuan kuantitas pada perencanaan anggaran RAB, dan
 * juga sebagai komponen pembentuk satuan majemuk {@link HasilSatuan} (lihat
 * {@link HasilSatuan#getSatuan1()}/{@link HasilSatuan#getSatuan2()}).
 *
 * <p><b>Bukan sinonim dari {@link HasilSatuan}.</b> Kelas ini murni satu unit ukur tunggal;
 * {@link HasilSatuan} adalah entity TERPISAH yang merujuk DUA {@code Satuan} sekaligus untuk
 * menyatakan hasil kombinasinya (mis. "Orang" &times; "Bulan" = label "OB"). Perhatikan pula
 * ada entity lain bernama serupa tapi tak berelasi:
 * {@link ais.database.model.inventory.SatuanProduk} adalah master satuan barang inventaris,
 * entity yang sama sekali berbeda tabel/skema dari kelas ini.</p>
 *
 * <h2>Getter {@link #getAktif()} — flag dengan default terhitung dan efek samping mutasi state</h2>
 * <p>Berbeda dari getter boolean pada entity lain di paket ini, {@link #getAktif()} BUKAN
 * accessor murni: bila {@code aktif} belum pernah disetel ({@code null}), getter ini
 * <b>menghitung</b> nilai default dari {@code !Common.isNumber(nama.trim())} — artinya satuan
 * yang namanya berupa angka murni (mis. hasil import data yang belum diberi nama sesungguhnya)
 * dianggap TIDAK aktif secara default, sedangkan satuan bernama teks biasa dianggap aktif — lalu
 * <b>menyimpan hasil hitungan itu ke field {@code aktif}</b> sehingga panggilan berikutnya tidak
 * menghitung ulang. Ini adalah pola "getter destruktif" (memutasi state instance dari method yang
 * namanya menyarankan operasi baca murni): kode pemanggil yang membandingkan {@code aktif} SEBELUM
 * dan SESUDAH memanggil getter ini pada object yang sama bisa melihat field berubah dari
 * {@code null} menjadi nilai konkret tanpa pernah memanggil {@link #setAktif(Boolean)} secara
 * eksplisit. Seluruh perhitungan dibungkus {@code try/catch} kosong: bila {@code nama} bernilai
 * {@code null} ({@code nama.trim()} melempar NPE), exception ditelan dan {@code aktif} tetap
 * {@code null}, sehingga getter jatuh ke fallback {@code true} di baris terakhir — akibatnya
 * satuan tanpa nama justru dianggap AKTIF, kebalikan dari niat baris di atasnya.</p>
 *
 * <h2>Pola arsitektur khas AIS yang muncul di kelas ini</h2>
 * <p>Field {@code oleh}, {@code olehId}, {@code tanggal_dirubah} beserta method
 * {@link #onUpdate()} adalah <b>field audit bayangan</b> yang menduplikasi field privat bernama
 * sama di {@link GeneralValueObject} — KEHARUSAN TEKNIS (induk bukan {@code @Entity} sehingga
 * tidak bisa mewariskan pemetaan kolom JPA), bukan salin-tempel ceroboh.</p>
 *
 * @see HasilSatuan
 * @see ais.action.master.rab.SatuanAction
 * @see ais.database.model.inventory.SatuanProduk
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "rab", name = "satuan")
public class Satuan extends GeneralValueObject {

	/**
	 * Versi serialisasi kelas. Nilainya sama dengan {@link MetodePengadaan}/{@link SatuanLokasi}
	 * (peninggalan hasil salin-tempel generator hbm2java); tidak masalah selama tidak ada dua
	 * kelas berbeda yang benar-benar diserialkan/dideserialkan saling tertukar sebagai satu sama
	 * lain.
	 */
	private static final long serialVersionUID = -8738027816264807168L;
	/** Primary key baris {@code rab.satuan}. Lihat {@link #getId()}. */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris ini (field audit bayangan). Lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini (field audit bayangan). Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir. Nilai {@code null} atau string kosong/spasi
	 * <b>diabaikan diam-diam</b> agar jejak audit yang sudah terisi tidak bisa terhapus oleh
	 * jalur simpan yang kebetulan tidak membawa informasi pengguna (mis. proses batch/import).
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan validasi non-trivial yang sama seperti
	 * {@link #setOlehId(String)}: nilai {@code null}/kosong diabaikan diam-diam.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook {@code @PreUpdate} yang mengimplementasikan kontrak abstrak
	 * {@link GeneralValueObject#onUpdate()}: dipanggil JPA tepat sebelum UPDATE dieksekusi, dan
	 * menyerahkan penyegaran {@link #tanggal_dirubah} ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}. Karena kait ini hanya
	 * menempel pada {@code @PreUpdate} (bukan {@code @PrePersist}), jejak waktu pada operasi INSERT
	 * pertama bergantung sepenuhnya pada nilai awal field di bawah ini.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}
	/**
	 * Cap waktu perubahan terakhir (field audit bayangan; lihat catatan kelas). Diinisialisasi ke
	 * waktu server saat object dibuat lewat {@code WaktuUtil.getDate()} sehingga baris baru tidak
	 * pernah membawa nilai {@code null}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel cap waktu perubahan terakhir. Tanpa validasi — berbeda dari {@link #setOleh(String)}/
	 * {@link #setOlehId(String)}, {@code null} akan benar-benar tersimpan.
	 *
	 * @param tanggal_dirubah cap waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir, dipetakan sebagai kolom {@code TIMESTAMP}.
	 *
	 * @return cap waktu perubahan terakhir; praktis tidak pernah {@code null} untuk object yang
	 *         dibuat lewat konstruktor kelas ini
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nama satuan, mis. "Orang", "Bulan", "Paket" (bebas diisi pengguna). Lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas untuk satuan ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Penanda satuan aktif/tidak, dengan default terhitung bila belum disetel. Lihat {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Constructor default tanpa argumen, WAJIB ada agar Hibernate dapat menginstansiasi entity
	 * lewat refleksi saat memuat baris dari database, dan agar layar CRUD dapat membuat object
	 * kosong untuk form tambah-baru.
	 */
	public Satuan() {
	}

	/**
	 * Constructor pintas yang langsung menyetel primary key, dipakai untuk membuat object
	 * "penunjuk" (hanya berisi id) sebagai parameter kriteria/relasi tanpa memuat seluruh baris.
	 *
	 * @param id primary key satuan yang dirujuk
	 */
	public Satuan(Long id) {
		this.id = id;
	}

	/**
	 * Constructor pintas untuk langsung menyetel nama satuan, dipakai jalur pembuatan cepat
	 * (mis. saat {@code RabImporter} membuat satuan baru dari data impor).
	 *
	 * @param nama nama satuan
	 */
	public Satuan(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan primary key baris {@code rab.satuan}.
	 *
	 * @return primary key, atau {@code null} bila entity belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Tanpa validasi.
	 *
	 * @param id nilai primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama satuan, sudah di-{@code trim()} bila tidak {@code null}. Kolom wajib
	 * diisi ({@code nullable = false}) pada tabel.
	 *
	 * @return nama satuan yang sudah dipangkas spasi tepi, atau {@code null} bila field mentahnya
	 *         {@code null}
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama satuan. Tanpa validasi maupun pemangkasan spasi di sisi setter — pemangkasan
	 * hanya terjadi saat dibaca lewat {@link #getNama()}, dan nilai ini pula yang dipakai
	 * {@link #getAktif()} untuk menentukan default aktif/tidak.
	 *
	 * @param nama nama satuan baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas satuan ini.
	 *
	 * @return teks keterangan, atau {@code null} bila belum diisi
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel keterangan bebas. Tanpa validasi.
	 *
	 * @param keterangan teks keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif/tidak aktif satuan ini, dengan <b>default terhitung dan efek
	 * samping mutasi state</b> — lihat penjelasan lengkap di Javadoc kelas.
	 *
	 * <p>Ringkasnya: bila {@link #aktif} belum pernah disetel eksplisit, method ini menghitung
	 * {@code !Common.isNumber(getNama().trim())} lalu <b>menyimpan hasilnya ke field</b>
	 * (bukan cuma mengembalikannya) — satuan bernama angka murni dianggap default tidak aktif,
	 * satuan bernama teks dianggap default aktif. Bila {@code nama} bernilai {@code null},
	 * {@code NullPointerException} dari {@code nama.trim()} ditelan oleh blok
	 * {@code catch} kosong, {@link #aktif} tetap {@code null}, dan method jatuh ke fallback
	 * baris terakhir yang mengembalikan {@code true} — sehingga satuan tanpa nama tampak AKTIF,
	 * bertentangan dengan arah default yang dimaksud baris perhitungan di atasnya.</p>
	 *
	 * @return {@code true}/{@code false} hasil perhitungan atau nilai yang sudah disetel eksplisit;
	 *         {@code true} sebagai fallback akhir bila perhitungan gagal
	 */
	public Boolean getAktif() {
		try {
			if (aktif == null) {
				aktif = !Common.isNumber(nama.trim());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/rab/Satuan.java:118");
			// TODO: handle exception
		}
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif secara eksplisit. Setelah dipanggil dengan nilai non-{@code null},
	 * {@link #getAktif()} tidak akan pernah lagi menghitung ulang default dari {@code nama}.
	 *
	 * @param aktif status aktif baru; {@code null} mengembalikan field ke keadaan "belum disetel"
	 *              sehingga {@link #getAktif()} akan menghitung ulang defaultnya
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}
}
