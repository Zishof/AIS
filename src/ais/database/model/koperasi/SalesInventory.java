package ais.database.model.koperasi;

import static javax.persistence.GenerationType.IDENTITY;

import java.math.BigDecimal;
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
import javax.persistence.Version;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.Toko;

/**
 * Master Sales / Penjual Keliling (varian "eBisnis Inventory &amp; Sales", layar legacy 07
 * "Data Sales atau Penjual Keliling" -- lihat docs/pos-inventory-sales di repo zishof-platform,
 * ERD_DAN_SPESIFIKASI_DATA_NOTA_SALES_JAVA_AIS.md &sect;3.1).
 *
 * <p>Menghubungkan akun login existing ({@link Tbmuser}) dgn profil sales per {@link Toko} --
 * BUKAN pengganti {@code Pedagang}: user Sales boleh TIDAK punya baris Pedagang sama sekali
 * (dia bukan kasir toko), dan resolver konteks
 * ({@code ais.action.servlet.api.EbisnisActorContextResolver}) TIDAK pernah menyimpulkan
 * "tanpa Pedagang = admin" -- itulah alasan entity ini lahir di fase P1 (fondasi RBAC), bukan
 * baru di P2 (CRUD Master Sales).</p>
 *
 * <p>{@code kode} = kode sales legacy DBF (2 karakter, dipertahankan sbg TEKS termasuk nol di
 * depan -- rekonsiliasi arsip); {@code nomorPerkiraan} = "No. Perkiraan" legacy (akun COA per
 * sales) -- nullable sampai mapping COA disetujui UAT (uat-required.md #1), TIDAK dikarang.
 * Uang ({@code targetBulanan}/{@code limitPenagihan}) memakai {@link BigDecimal} sesuai
 * larangan &sect;2 PERINTAH_MASTER (bukan {@code Double} spt entity lama). Sales berhistori
 * DINONAKTIFKAN ({@code aktif=false}), tidak pernah dihapus fisik.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "sales_inventory")
public class SalesInventory extends GeneralValueObject {

	/** Versi serialisasi tetap -- lihat catatan umum {@link GeneralValueObject} soal kompatibilitas. */
	private static final long serialVersionUID = 1L;

	private Long id;
	private String kode;
	private String nama;
	private Tbmuser tbmuser;
	private Toko toko;
	private String nomorPerkiraan;
	private String area;
	private String telepon;
	private String alamat;
	private BigDecimal targetBulanan;
	private BigDecimal limitPenagihan;
	private Boolean aktif;
	private Long version;

	private String oleh;
	private String olehId;
	private Date waktu;
	/**
	 * Hook Hibernate {@code @PreUpdate}: dipanggil OTOMATIS sesaat sebelum UPDATE dieksekusi,
	 * mendelegasikan pembaruan stempel waktu ke {@code AuditTimestampInterceptor.ubah(this)} --
	 * pola seragam yang dipakai seluruh subclass {@link GeneralValueObject} yang memetakan
	 * {@link #getTanggal_dirubah()} sebagai kolom fisik. Tidak dipanggil manual oleh kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Constructor kosong -- wajib untuk Hibernate (instansiasi via reflection); pemakai aplikasi mengisi field lewat setter. */
	public SalesInventory() {
	}

	/**
	 * @return id primer baris master sales ini, {@code null} untuk instance yang belum pernah
	 *         disimpan (belum dapat identity dari sequence/auto-increment kolom {@code id}).
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan id baris ini secara manual -- berguna untuk membentuk referensi entity
	 * "hanya berisi id" (mis. sebelum {@code session.load(...)}) tanpa query tambahan.
	 *
	 * @param id id baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/** Kode sales legacy (teks, nol di depan dipertahankan) -- unik per toko (dicek di helper simpan). */
	@Column(name = "kode", length = 30)
	public String getKode() {
		return kode;
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	/** @return nama lengkap sales/penjual keliling yang tampil di layar Master Sales dan nota. */
	@Column(name = "nama")
	public String getNama() {
		return nama;
	}

	/** @param nama nama lengkap sales; boleh {@code null} sebagai nilai mentah, meski form CRUD biasanya mewajibkan pengisian. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Akun login sales -- nullable: master sales boleh dibuat dulu tanpa akun (legacy DBF tidak
	 * punya akun per sales), dipasangkan belakangan. Satu akun aktif maksimal SATU profil sales
	 * per toko (invariant dicek di helper simpan, bukan constraint DB, supaya data legacy yang
	 * kotor tetap bisa diimpor lalu dibereskan lewat exception queue).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser_id", nullable = true)
	public Tbmuser getTbmuser() {
		tbmuser = check(tbmuser);
		return tbmuser;
	}

	/**
	 * Memasangkan/melepas akun login sales.
	 *
	 * <p>Invariant "satu akun aktif maksimal satu profil sales per toko" (lihat catatan
	 * {@link #getTbmuser()}) TIDAK ditegakkan oleh setter ini maupun oleh constraint database --
	 * pemanggil (helper simpan CRUD Master Sales) bertanggung jawab memvalidasinya sebelum
	 * memanggil setter ini.</p>
	 *
	 * @param tbmuser akun login yang dipasangkan; boleh {@code null} untuk melepas pasangan.
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * Toko yang menaungi profil sales ini -- menentukan cakupan penugasan: kode ({@link #getKode()})
	 * hanya dijamin unik DI DALAM satu toko, dan satu akun {@link #getTbmuser()} hanya boleh punya
	 * satu profil aktif per toko yang sama. Sales yang beroperasi di beberapa toko membutuhkan
	 * SATU baris {@code SalesInventory} terpisah per toko.
	 *
	 * @return toko pemilik profil sales ini; TIDAK PERNAH {@code null} pada baris tersimpan
	 *         (kolom dipetakan {@code nullable = false}).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko", nullable = false)
	public Toko getToko() {
		toko = check(toko);
		return toko;
	}

	/** @param toko toko pemilik profil sales ini; wajib diisi sebelum baris disimpan (kolom {@code nullable = false}). */
	public void setToko(Toko toko) {
		this.toko = toko;
	}

	/** "No. Perkiraan" legacy (akun COA sales) -- nullable sampai mapping COA disetujui (UAT_REQUIRED). */
	@Column(name = "nomor_perkiraan", length = 50)
	public String getNomorPerkiraan() {
		return nomorPerkiraan;
	}

	/** @param nomorPerkiraan "No. Perkiraan" legacy; boleh {@code null} sampai mapping COA sales disetujui. */
	public void setNomorPerkiraan(String nomorPerkiraan) {
		this.nomorPerkiraan = nomorPerkiraan;
	}

	/** @return area/wilayah penugasan sales (teks bebas, mis. nama kecamatan/rute) -- bukan relasi ke master wilayah manapun. */
	@Column(name = "area")
	public String getArea() {
		return area;
	}

	/** @param area area/wilayah penugasan sales; teks bebas, boleh {@code null}. */
	public void setArea(String area) {
		this.area = area;
	}

	/** @return nomor telepon/HP sales, atau {@code null} bila belum diisi. */
	@Column(name = "telepon", length = 50)
	public String getTelepon() {
		return telepon;
	}

	/** @param telepon nomor telepon/HP sales; boleh {@code null}. */
	public void setTelepon(String telepon) {
		this.telepon = telepon;
	}

	/** @return alamat sales (kolom {@code text}, tidak dibatasi panjang), atau {@code null} bila belum diisi. */
	@Column(name = "alamat", columnDefinition = "text")
	public String getAlamat() {
		return alamat;
	}

	/** @param alamat alamat sales; boleh {@code null}. */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Target penjualan/setoran bulanan sales ini (nominal referensi untuk evaluasi kinerja),
	 * disimpan {@link BigDecimal} sesuai larangan &sect;2 PERINTAH_MASTER atas {@code Double} untuk
	 * field uang -- lihat catatan Javadoc kelas. Diisi/dibaca lewat CRUD Master Sales
	 * ({@code SalesInventoryMasterHelper}, kunci JSON {@code target_bulanan}/{@code targetBulanan});
	 * belum ada pemakai lain (mis. mesin evaluasi kinerja otomatis) yang ditemukan pada penelusuran
	 * kode saat ini -- murni field konfigurasi/informasional pada fase ini.
	 *
	 * @return target bulanan, atau {@link BigDecimal#ZERO} bila belum diisi (TIDAK PERNAH
	 *         {@code null} -- pola normalisasi nilai uang yang konsisten dgn {@link #getLimitPenagihan()}).
	 */
	@Column(name = "target_bulanan", precision = 19, scale = 2)
	public BigDecimal getTargetBulanan() {
		return targetBulanan == null ? BigDecimal.ZERO : targetBulanan;
	}

	/** @param targetBulanan target bulanan baru; boleh {@code null} sebagai nilai field mentah (getter menormalkannya jadi {@code ZERO}). */
	public void setTargetBulanan(BigDecimal targetBulanan) {
		this.targetBulanan = targetBulanan;
	}

	/**
	 * Batas maksimal piutang/tagihan yang boleh ditumpuk sales ini terhadap pelanggan yang
	 * ditanganinya (pagu penagihan) -- pasangan konseptual {@link #getTargetBulanan()}: satu
	 * membatasi ke ATAS (target dicapai), satu membatasi ke BAWAH/risiko (piutang tidak boleh
	 * menumpuk lewat batas ini). Sama seperti target bulanan, saat ini murni field konfigurasi yang
	 * diisi/dibaca lewat CRUD Master Sales -- penegakan batas ini pada proses penagihan/penerbitan
	 * nota lapangan berada di luar cakupan entity ini (lihat helper transaksi di paket
	 * {@code ais.action.servlet.api} berawalan {@code SalesInventory*} untuk alur terkait, mis.
	 * {@code SalesInventoryReceivableHelper}).
	 *
	 * @return limit penagihan, atau {@link BigDecimal#ZERO} bila belum diisi (TIDAK PERNAH
	 *         {@code null}).
	 */
	@Column(name = "limit_penagihan", precision = 19, scale = 2)
	public BigDecimal getLimitPenagihan() {
		return limitPenagihan == null ? BigDecimal.ZERO : limitPenagihan;
	}

	/** @param limitPenagihan limit penagihan baru; boleh {@code null} sebagai nilai field mentah (getter menormalkannya jadi {@code ZERO}). */
	public void setLimitPenagihan(BigDecimal limitPenagihan) {
		this.limitPenagihan = limitPenagihan;
	}

	/**
	 * Menandai profil sales ini masih berlaku/dapat dipakai bertransaksi.
	 *
	 * <p>Default {@code true} bila kolom belum diisi -- pola umum flag "aktif" di kelas entity AIS:
	 * data lama otomatis dianggap aktif tanpa migrasi eksplisit. Sesuai catatan Javadoc kelas, sales
	 * berhistori DINONAKTIFKAN lewat flag ini, TIDAK PERNAH dihapus fisik (baris tetap dipertahankan
	 * agar riwayat nota/transaksi yang mereferensikannya tetap utuh).</p>
	 *
	 * @return {@code true} bila aktif/kolom belum diisi, {@code false} bila dinonaktifkan.
	 */
	@Column(name = "aktif")
	public Boolean getAktif() {
		return aktif == null ? Boolean.TRUE : aktif;
	}

	/** @param aktif status aktif baru; {@code null} diperlakukan sama seperti {@code true} oleh {@link #getAktif()}. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/** Optimistic locking (pola sama {@code Kegiatan.version}) -- master ini bisa diedit dari banyak perangkat. */
	@Version
	@Column(name = "version")
	public Long getVersion() {
		return version;
	}

	/**
	 * @param version nomor versi optimistic-locking; TIDAK PERLU di-set manual oleh kode aplikasi
	 *                pada alur normal -- Hibernate menaikkannya sendiri tiap UPDATE dan melempar
	 *                {@code StaleObjectStateException} bila baris sudah berubah sejak dibaca (lihat
	 *                catatan {@link #getVersion()}). Setter ini terpakai a.l. saat rekonsiliasi
	 *                objek terdeserialisasi.
	 */
	public void setVersion(Long version) {
		this.version = version;
	}

	/** @return nama pengguna (potret teks) yang terakhir mengubah baris ini, atau {@code null} bila belum tercatat. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menetapkan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Sengaja MENGABAIKAN input {@code null}/kosong (early-return tanpa mengubah field) --
	 * pola audit shadow yang berulang di seluruh entity AIS: sekali baris punya {@code oleh},
	 * nilai itu tidak boleh tertimpa "lupa diisi" oleh pemanggil yang tidak melewatkan identitas
	 * pengguna. Ini KEHARUSAN TEKNIS, bukan bug.</p>
	 *
	 * @param oleh nama pengguna; nilai {@code null}/kosong diabaikan.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/** @return id pengguna (String) yang terakhir mengubah baris ini, atau {@code null} bila belum tercatat. */
	@Column(name = "oleh_id")
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Pola sama dengan {@link #setOleh(String)}: input {@code null}/kosong diabaikan supaya
	 * jejak id pengubah tidak pernah tertimpa kosong.</p>
	 *
	 * @param olehId id pengguna; nilai {@code null}/kosong diabaikan.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Stempel waktu "resmi" baris ini (dimaksudkan sbg tanggal baris ini mulai berlaku/dibuat,
	 * berbeda dari {@link #getTanggal_dirubah()} yang murni jejak audit perubahan terakhir).
	 *
	 * <p><b>Catatan kehati-hatian:</b> bila kolom belum pernah diisi ({@code waktu == null}),
	 * getter mengembalikan {@code WaktuUtil.getDate()} -- yaitu WAKTU SAAT DIPANGGIL, BUKAN waktu
	 * tetap -- sehingga dua pemanggilan berturut-turut pada baris yang sama bisa mengembalikan
	 * nilai yang SEDIKIT berbeda selama field belum di-set eksplisit. Penelusuran kode pemanggil
	 * ({@code ais.action.servlet.api.SalesInventory*Helper}) tidak menemukan satu pun titik yang
	 * memanggil {@code setWaktu(...)} pada entity ini (CRUD Master Sales hanya mengisi
	 * kode/nama/toko/nomorPerkiraan/area/telepon/alamat/targetBulanan/limitPenagihan/aktif) --
	 * artinya pada praktiknya kolom ini saat ini SELALU {@code null} di baris yang dibuat lewat
	 * jalur CRUD yang ada, dan getter ini efektif hanya mengembalikan "sekarang" tiap dipanggil.
	 * Field/getter tersedia untuk pemakai masa depan (mis. jalur impor DBF yang membawa tanggal
	 * historis) tetapi belum diaktifkan.</p>
	 *
	 * @return {@code waktu} tersimpan, atau waktu saat ini bila belum pernah diisi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu")
	public Date getWaktu() {
		return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu;
	}

	/** @param waktu stempel waktu "resmi" baris ini; boleh {@code null} (getter lalu memakai waktu saat dipanggil -- lihat {@link #getWaktu()}). */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * @return stempel waktu perubahan terakhir baris ini -- diisi otomatis oleh
	 *         {@link #onUpdate()} pada setiap UPDATE, dan oleh inisialisasi field pada saat
	 *         object pertama dibuat (baris baru yang belum pernah di-UPDATE tetap punya nilai
	 *         non-null: waktu construction-nya sendiri).
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Menetapkan stempel waktu terakhir diubah secara eksplisit.
	 *
	 * <p>Biasanya TIDAK perlu dipanggil manual -- nilai defaultnya diisi saat field diinisialisasi
	 * ({@code WaktuUtil.getDate()} pada construction) dan diperbarui otomatis oleh
	 * {@link #onUpdate()} setiap UPDATE. Setter ini tersedia untuk kasus seperti impor data
	 * historis yang perlu mempertahankan stempel waktu asli.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
