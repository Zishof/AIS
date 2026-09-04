package ais.database.model.koperasi;

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
import javax.persistence.Version;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.library.Penyedia;

/**
 * Profil supplier varian "eBisnis Inventory &amp; Sales" (layar legacy 01-03 "Data Supplier",
 * SUPPLIER.DBF) -- EXTENSION di atas {@link Penyedia} existing, BUKAN kolom baru di Penyedia
 * (aturan ERD &sect;1.2: jangan menambah kolom ke entity existing bila semantiknya modul lain).
 * Field legacy yang TIDAK ada di Penyedia hidup di sini: termin (SYARAT_BYR, dasar jatuh
 * tempo hutang), wilayah, rekening bank (REKRUPIAH/ATASNAMA/NAMABANK/ALMBANK), dan status
 * aktif (master berhistori DINONAKTIFKAN, tidak dihapus fisik).
 *
 * <p>Satu Penyedia maksimal SATU profil (invariant dijaga di helper simpan, bukan unique
 * constraint DB -- supaya data impor legacy yang kotor bisa masuk dulu lalu dibereskan).
 * Saldo hutang supplier TIDAK disimpan di sini -- selalu dihitung dari ledger pembelian/
 * pembayaran (baca-saja, layar 22-27 fase P3).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "supplier_inventory_profile")
public class SupplierInventoryProfile extends GeneralValueObject {

	/** Versi serialisasi tetap; dipertahankan hanya krn kontrak {@link GeneralValueObject}/
	 * {@code Serializable}. */
	private static final long serialVersionUID = 1L;

	/** PK auto-generated (identity). Lihat {@link #getId()}. */
	private Long id;
	/** Supplier (pemasok, entity {@link Penyedia} di paket {@code library}) pemilik profil ini --
	 * identitas dasar TETAP milik {@code Penyedia}, field di sini murni ekstensi legacy
	 * (termin/wilayah/rekening/status aktif) -- lihat Javadoc kelas. Lihat {@link #getPenyedia()}. */
	private Penyedia penyedia;
	/** Termin pembayaran (hari) -- SYARAT_BYR legacy. Lihat {@link #getTerminHari()}. */
	private Integer terminHari;
	/** Wilayah/area supplier. Lihat {@link #getWilayah()}. */
	private String wilayah;
	/** Nomor rekening bank supplier (REKRUPIAH legacy) tujuan pembayaran hutang. Lihat
	 * {@link #getNoRekening()}. */
	private String noRekening;
	/** Nama pemilik rekening pada {@link #noRekening} (ATASNAMA legacy). Lihat
	 * {@link #getAtasNama()}. */
	private String atasNama;
	/** Nama bank tempat {@link #noRekening} terdaftar (NAMABANK legacy). Lihat
	 * {@link #getBank()}. */
	private String bank;
	/** Alamat cabang bank (ALMBANK legacy). Lihat {@link #getAlamatBank()}. */
	private String alamatBank;
	/** Status aktif profil ini; {@code false} = DINONAKTIFKAN (master berhistori, bukan dihapus
	 * fisik) -- lihat Javadoc kelas. Lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** Nomor versi optimistic-locking Hibernate ({@code @Version}); dikelola otomatis oleh
	 * Hibernate, TIDAK boleh diset manual dari kode aplikasi. Lihat {@link #getVersion()}. */
	private Long version;

	/** Nama petugas yang membuat/mengubah baris profil ini (jejak audit tampilan, bukan FK).
	 * Lihat {@link #getOleh()}. */
	private String oleh;
	/** ID/username petugas yang membuat/mengubah baris profil ini. Lihat {@link #getOlehId()}. */
	private String olehId;
	/** Waktu baris ini dibuat/dientri. Lihat {@link #getWaktu()}. */
	private Date waktu;
	/**
	 * Callback JPA {@code @PreUpdate}: menandai kapan baris ini TERAKHIR diubah, dengan menuliskan
	 * waktu sekarang ke {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Dipanggil otomatis
	 * oleh Hibernate sebelum {@code UPDATE}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor tanpa argumen wajib JPA/Hibernate. */
	public SupplierInventoryProfile() {
	}

	/**
	 * PK identity baris profil ini. {@code null} sebelum entity di-{@code save}/{@code flush} ke
	 * Hibernate (ID baru dibuat DB saat insert, strategi {@link IDENTITY}).
	 *
	 * @return id baris profil, atau {@code null} bila belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Setter PK -- dipanggil Hibernate saat memuat entity dari DB. Kode aplikasi normal tidak
	 * perlu memanggil ini; id baru dibuat otomatis oleh DB saat insert.
	 *
	 * @param id id baris profil.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Supplier pemilik profil ini. {@code nullable = false} -- setiap profil wajib terikat satu
	 * {@link Penyedia}. Invariant "satu Penyedia maksimal SATU profil" dijaga di helper simpan,
	 * BUKAN unique constraint DB (lihat Javadoc kelas). Relasi {@code LAZY}: mengakses field pada
	 * objek di luar sesi Hibernate yang masih terbuka akan melempar
	 * {@code LazyInitializationException}.
	 *
	 * @return supplier pemilik profil ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "penyedia", nullable = false)
	public Penyedia getPenyedia() {
		penyedia = check(penyedia);
		return penyedia;
	}

	/**
	 * Menetapkan supplier pemilik profil. Tidak ada guard duplikasi di level entity -- pemeriksaan
	 * "satu Penyedia maksimal satu profil" dilakukan di helper simpan pemanggil, sehingga data
	 * impor legacy yang kotor (mis. lebih dari satu profil per supplier) tetap bisa masuk lalu
	 * dibereskan kemudian (lihat Javadoc kelas).
	 *
	 * @param penyedia supplier pemilik profil ini.
	 */
	public void setPenyedia(Penyedia penyedia) {
		this.penyedia = penyedia;
	}

	/**
	 * Termin pembayaran (hari) -- SYARAT_BYR legacy; dasar perhitungan jatuh tempo hutang. Getter
	 * null-safe: mengembalikan {@code 0} bila kolom NULL di DB (berarti tanpa termin / cash).
	 *
	 * @return termin hari, tidak pernah {@code null}.
	 */
	@Column(name = "termin_hari")
	public Integer getTerminHari() {
		return terminHari == null ? Integer.valueOf(0) : terminHari;
	}

	/**
	 * Menetapkan termin pembayaran supplier ini.
	 *
	 * @param terminHari termin baru dalam hari.
	 */
	public void setTerminHari(Integer terminHari) {
		this.terminHari = terminHari;
	}

	/**
	 * Wilayah/area supplier.
	 *
	 * @return wilayah supplier, atau {@code null} bila belum diisi.
	 */
	@Column(name = "wilayah")
	public String getWilayah() {
		return wilayah;
	}

	/**
	 * Menetapkan wilayah/area supplier.
	 *
	 * @param wilayah wilayah baru.
	 */
	public void setWilayah(String wilayah) {
		this.wilayah = wilayah;
	}

	/**
	 * Nomor rekening bank supplier (REKRUPIAH legacy), tujuan pembayaran hutang ke supplier ini.
	 *
	 * @return nomor rekening, atau {@code null} bila belum diisi.
	 */
	@Column(name = "no_rekening", length = 60)
	public String getNoRekening() {
		return noRekening;
	}

	/**
	 * Menetapkan nomor rekening bank supplier.
	 *
	 * @param noRekening nomor rekening baru.
	 */
	public void setNoRekening(String noRekening) {
		this.noRekening = noRekening;
	}

	/**
	 * Nama pemilik rekening pada {@link #getNoRekening()} (ATASNAMA legacy).
	 *
	 * @return nama pemilik rekening, atau {@code null} bila belum diisi.
	 */
	@Column(name = "atas_nama")
	public String getAtasNama() {
		return atasNama;
	}

	/**
	 * Menetapkan nama pemilik rekening.
	 *
	 * @param atasNama nama pemilik rekening baru.
	 */
	public void setAtasNama(String atasNama) {
		this.atasNama = atasNama;
	}

	/**
	 * Nama bank tempat {@link #getNoRekening()} terdaftar (NAMABANK legacy).
	 *
	 * @return nama bank, atau {@code null} bila belum diisi.
	 */
	@Column(name = "bank", length = 100)
	public String getBank() {
		return bank;
	}

	/**
	 * Menetapkan nama bank.
	 *
	 * @param bank nama bank baru.
	 */
	public void setBank(String bank) {
		this.bank = bank;
	}

	/**
	 * Alamat cabang bank (ALMBANK legacy) tempat {@link #getNoRekening()} terdaftar.
	 *
	 * @return alamat bank, atau {@code null} bila belum diisi.
	 */
	@Column(name = "alamat_bank", columnDefinition = "text")
	public String getAlamatBank() {
		return alamatBank;
	}

	/**
	 * Menetapkan alamat cabang bank.
	 *
	 * @param alamatBank alamat bank baru.
	 */
	public void setAlamatBank(String alamatBank) {
		this.alamatBank = alamatBank;
	}

	/**
	 * Status aktif profil ini. Getter null-safe: mengembalikan {@link Boolean#TRUE} bila kolom
	 * NULL di DB -- DEFAULT AKTIF, bukan default nonaktif. Baris {@code aktif = false} berarti
	 * supplier DINONAKTIFKAN (master berhistori, bukan dihapus fisik) -- lihat Javadoc kelas.
	 *
	 * @return {@code true} bila profil aktif, {@code false} bila dinonaktifkan.
	 */
	@Column(name = "aktif")
	public Boolean getAktif() {
		return aktif == null ? Boolean.TRUE : aktif;
	}

	/**
	 * Menetapkan status aktif profil ini.
	 *
	 * @param aktif status aktif baru; {@code null} diperlakukan sbg aktif oleh getter.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Nomor versi optimistic-locking Hibernate ({@code @Version}). Dinaikkan otomatis oleh
	 * Hibernate pada setiap {@code UPDATE} -- dipakai utk mendeteksi konflik tulis bersamaan
	 * ({@code StaleObjectStateException}), BUKAN field bisnis.
	 *
	 * @return nomor versi baris saat ini.
	 */
	@Version
	@Column(name = "version")
	public Long getVersion() {
		return version;
	}

	/**
	 * Setter versi -- dipanggil Hibernate saat memuat entity. Kode aplikasi normal tidak perlu
	 * (dan tidak boleh) memanggil ini secara manual.
	 *
	 * @param version nomor versi.
	 */
	public void setVersion(Long version) {
		this.version = version;
	}

	/**
	 * Nama petugas yang membuat/mengubah baris profil ini.
	 *
	 * @return nama petugas, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menetapkan {@link #oleh}. Guard null/blank: nilai {@code null}/kosong/spasi DIABAIKAN
	 * (early return) -- field yang sudah terisi tidak ditimpa balik ke kosong.
	 *
	 * @param oleh nama petugas; diabaikan bila {@code null} atau blank.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * ID/username petugas yang membuat/mengubah baris profil ini.
	 *
	 * @return id/username petugas, atau {@code null} bila belum pernah diisi.
	 */
	@Column(name = "oleh_id")
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan {@link #olehId}. Guard null/blank sama seperti {@link #setOleh(String)}.
	 *
	 * @param olehId id/username petugas; diabaikan bila {@code null} atau blank.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Waktu baris profil ini dibuat/dientri. Getter null-safe: mengembalikan waktu SEKARANG
	 * ({@link ais.ui.util.WaktuUtil#getDate()}) bila kolom NULL, dihitung ULANG setiap kali getter
	 * dipanggil pada baris yang kolomnya NULL (bukan waktu tetap saat objek dibuat).
	 *
	 * @return waktu entry baris ini, atau waktu panggilan getter saat ini bila kolom NULL.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu")
	public Date getWaktu() {
		return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu;
	}

	/**
	 * Menetapkan waktu entry baris ini.
	 *
	 * @param waktu waktu baris ini dibuat/dientri.
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Timestamp perubahan terakhir baris ini, diisi otomatis oleh {@link #onUpdate()}.
	 *
	 * @return waktu perubahan terakhir, atau waktu instansiasi objek bila belum pernah di-update.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Setter manual utk {@link #tanggal_dirubah}. Jarang dipakai langsung -- field ini biasanya
	 * diisi otomatis oleh {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah waktu perubahan yang ingin dicatat.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
