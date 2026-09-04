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

/**
 * Profil customer varian "eBisnis Inventory &amp; Sales" (layar legacy 04-06 "Data Customer",
 * LANGGANAN.DBF) -- EXTENSION di atas {@link AnggotaKoperasi} existing (identitas: kode 5
 * karakter teks/nama/alamat/telepon TETAP milik AnggotaKoperasi; jangan duplikat). Field
 * khusus distribusi/sales hidup di sini: termin, diskon default, wilayah, sales pembina
 * ({@link SalesInventory}), rekening bank. Member retail POS TIDAK otomatis punya profil ini
 * (aturan ERD &sect;6.2: jangan samakan member retail dgn customer distributor tanpa profil
 * eksplisit).
 *
 * <p>Saldo piutang TIDAK disimpan -- baca-saja dihitung dari ledger existing (belanja
 * ber-cara-bayar {@code masuk_sebagai_hutang} dikurangi {@code pembayaran_hutang}, formula
 * yang sama dgn {@code KantinHelper.mutasiHutangList}).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "customer_inventory_profile")
public class CustomerInventoryProfile extends GeneralValueObject {

	/** Versi serialisasi tetap; dipertahankan hanya krn kontrak {@link GeneralValueObject}/
	 * {@code Serializable}. */
	private static final long serialVersionUID = 1L;

	/** PK auto-generated (identity). Lihat {@link #getId()}. */
	private Long id;
	/** Anggota koperasi (customer) pemilik profil ini -- identitas (kode/nama/alamat/telepon)
	 * TETAP milik {@link AnggotaKoperasi}, field di sini murni ekstensi distribusi/sales. Lihat
	 * {@link #getAnggotaKoperasi()}. */
	private AnggotaKoperasi anggotaKoperasi;
	/** Sales pembina customer ini. Lihat {@link #getSalesOwner()}. */
	private SalesInventory salesOwner;
	/** Termin pembayaran (hari) default customer ini; dasar perhitungan jatuh tempo piutang.
	 * Lihat {@link #getTerminHari()}. */
	private Integer terminHari;
	/** Persentase diskon default yang diberikan ke customer ini pada transaksi jual. Lihat
	 * {@link #getDiskonDefaultPersen()}. */
	private BigDecimal diskonDefaultPersen;
	/** Wilayah/area customer, dipakai utk pengelompokan laporan sales per wilayah. Lihat
	 * {@link #getWilayah()}. */
	private String wilayah;
	/** Nomor rekening bank customer (mis. utk retur/refund). Lihat {@link #getNoRekening()}. */
	private String noRekening;
	/** Nama pemilik rekening pada {@link #noRekening}. Lihat {@link #getAtasNama()}. */
	private String atasNama;
	/** Nama bank tempat {@link #noRekening} terdaftar. Lihat {@link #getBank()}. */
	private String bank;
	/** Status aktif profil ini; {@code false} = dinonaktifkan (master berhistori, bukan dihapus
	 * fisik). Lihat {@link #getAktif()}. */
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
	public CustomerInventoryProfile() {
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
	 * Anggota koperasi (customer) pemilik profil ini. {@code nullable = false} -- setiap profil
	 * wajib terikat satu {@link AnggotaKoperasi}. Ini relasi 1:1 arah tunggal (dijaga di helper
	 * simpan, BUKAN unique constraint DB pada kolom {@code anggota_koperasi} -- lihat catatan
	 * invariant serupa pada Javadoc kelas {@link SupplierInventoryProfile}): identitas
	 * (kode/nama/alamat/telepon) TIDAK diduplikasi di sini, hanya field ekstensi
	 * distribusi/sales. Relasi {@code LAZY}: mengakses field pada objek di luar sesi Hibernate
	 * yang masih terbuka akan melempar {@code LazyInitializationException}.
	 *
	 * @return anggota koperasi (customer) pemilik profil ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "anggota_koperasi", nullable = false)
	public AnggotaKoperasi getAnggotaKoperasi() {
		anggotaKoperasi = check(anggotaKoperasi);
		return anggotaKoperasi;
	}

	/**
	 * Menetapkan anggota koperasi (customer) pemilik profil. Tidak ada guard duplikasi di level
	 * entity -- pemeriksaan "satu customer maksimal satu profil" (bila ada) dilakukan di helper
	 * simpan pemanggil.
	 *
	 * @param anggotaKoperasi anggota koperasi (customer) pemilik profil ini.
	 */
	public void setAnggotaKoperasi(AnggotaKoperasi anggotaKoperasi) {
		this.anggotaKoperasi = anggotaKoperasi;
	}

	/**
	 * Sales pembina customer ini -- snapshot pada faktur historis TIDAK ikut berubah bila diganti.
	 * {@code nullable = true}: customer boleh belum punya sales pembina. Relasi {@code LAZY} --
	 * sama catatan lazy-loading dgn {@link #getAnggotaKoperasi()}.
	 *
	 * @return sales pembina customer ini, atau {@code null} bila belum ditetapkan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sales_owner", nullable = true)
	public SalesInventory getSalesOwner() {
		salesOwner = check(salesOwner);
		return salesOwner;
	}

	/**
	 * Menetapkan sales pembina customer. Mengganti nilai ini TIDAK mengubah sales yang sudah
	 * tercatat pada faktur historis (faktur menyimpan snapshot sendiri, bukan referensi ke field
	 * ini) -- lihat Javadoc {@link #getSalesOwner()}.
	 *
	 * @param salesOwner sales pembina baru, atau {@code null} utk melepas penugasan.
	 */
	public void setSalesOwner(SalesInventory salesOwner) {
		this.salesOwner = salesOwner;
	}

	/**
	 * Termin pembayaran (hari) default customer ini; dasar perhitungan jatuh tempo piutang pada
	 * transaksi jual ke customer ini. Getter null-safe: mengembalikan {@code 0} bila kolom NULL
	 * di DB (berarti tanpa termin / cash).
	 *
	 * @return termin hari, tidak pernah {@code null}.
	 */
	@Column(name = "termin_hari")
	public Integer getTerminHari() {
		return terminHari == null ? Integer.valueOf(0) : terminHari;
	}

	/**
	 * Menetapkan termin pembayaran default customer ini.
	 *
	 * @param terminHari termin baru dalam hari.
	 */
	public void setTerminHari(Integer terminHari) {
		this.terminHari = terminHari;
	}

	/**
	 * Persentase diskon default yang diberikan ke customer ini pada transaksi jual. Getter
	 * null-safe: mengembalikan {@link BigDecimal#ZERO} bila kolom NULL di DB.
	 *
	 * <p><b>Catatan integritas:</b> tidak ada validasi rentang (mis. 0-100) di level entity --
	 * pola longgar yang sama dgn field harga pada {@link HargaJualCustomer}/
	 * {@link HargaBeliSupplier}; dicatat sbg observasi, bukan diajukan sbg task baru krn dampaknya
	 * salah-entri data dari user berwenang, bukan celah otorisasi.
	 *
	 * @return diskon default dalam persen, tidak pernah {@code null}.
	 */
	@Column(name = "diskon_default_persen", precision = 7, scale = 3)
	public BigDecimal getDiskonDefaultPersen() {
		return diskonDefaultPersen == null ? BigDecimal.ZERO : diskonDefaultPersen;
	}

	/**
	 * Menetapkan persentase diskon default customer ini.
	 *
	 * @param diskonDefaultPersen diskon default baru dalam persen.
	 */
	public void setDiskonDefaultPersen(BigDecimal diskonDefaultPersen) {
		this.diskonDefaultPersen = diskonDefaultPersen;
	}

	/**
	 * Wilayah/area customer, dipakai utk pengelompokan laporan sales per wilayah.
	 *
	 * @return wilayah customer, atau {@code null} bila belum diisi.
	 */
	@Column(name = "wilayah")
	public String getWilayah() {
		return wilayah;
	}

	/**
	 * Menetapkan wilayah/area customer.
	 *
	 * @param wilayah wilayah baru.
	 */
	public void setWilayah(String wilayah) {
		this.wilayah = wilayah;
	}

	/**
	 * Nomor rekening bank customer, dipasangkan dgn {@link #getAtasNama()} dan {@link #getBank()}
	 * (mis. utk keperluan retur/refund ke customer).
	 *
	 * @return nomor rekening, atau {@code null} bila belum diisi.
	 */
	@Column(name = "no_rekening", length = 60)
	public String getNoRekening() {
		return noRekening;
	}

	/**
	 * Menetapkan nomor rekening bank customer.
	 *
	 * @param noRekening nomor rekening baru.
	 */
	public void setNoRekening(String noRekening) {
		this.noRekening = noRekening;
	}

	/**
	 * Nama pemilik rekening pada {@link #getNoRekening()}.
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
	 * Nama bank tempat {@link #getNoRekening()} terdaftar.
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
	 * Status aktif profil ini. Getter null-safe: mengembalikan {@link Boolean#TRUE} bila kolom
	 * NULL di DB -- DEFAULT AKTIF, bukan default nonaktif. Baris {@code aktif = false} adalah
	 * profil yang dinonaktifkan (master berhistori, bukan dihapus fisik) -- konsisten dgn aturan
	 * ERD &sect;6.2 yang dirujuk pada Javadoc kelas.
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
