package ais.database.model.inventory;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * <h3>Grup Produk -- kendali HPP dan harga jual TERPUSAT lintas toko/outlet.</h3>
 *
 * <p>Latar (permintaan pelanggan waralaba ~90 outlet, 2026-08-18): {@link Produk} terikat SATU
 * {@link Toko}, sehingga produk yang sama (mis. "Ayam Marinasi") ada sebagai ~90 baris terpisah
 * dan perubahan HPP/harga jual harus diulang 90 kali. Grup ini menjadi induk lintas-toko:
 * produk dari toko mana pun boleh menunjuk satu grup ({@link Produk#getGrupProduk()}), dan
 * penyimpanan harga pada grup MENYALIN {@code hargaBeli}/{@code hargaJual} ke seluruh produk
 * anggota (lihat {@code GrupProdukAction.onSave}) -- harga tetap termaterialisasi di tiap baris
 * {@code Produk} sehingga TIDAK ADA jalur baca (POS kasir, struk, laporan, sinkronisasi
 * Desktop/Android) yang berubah perilakunya.</p>
 *
 * <p>Konsekuensi yang disengaja: suntingan harga lokal per-outlet tetap mungkin, tetapi akan
 * TERTIMPA pada penyimpanan grup berikutnya -- grup adalah sumber kebenaran harga anggotanya.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "grup_produk")
public class GrupProduk extends GeneralValueObject {

	private static final long serialVersionUID = 7268413577548439811L;

	private Long id;
	private String kode;
	private String nama;
	private String keterangan;
	private Double hargaBeli;
	private Double hargaJual;
	private String bahanBaku;
	private Boolean ikutHpp;
	private Boolean ikutHargaJual;
	private ais.database.model.koperasi.AturanDiskon aturanDiskon;
	private Boolean aktif;
	private String oleh;
	private String olehId;

	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil otomatis Hibernate tepat sebelum tiap {@code UPDATE}
	 * baris ini, mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah}
	 * yang menyetel {@link #tanggal_dirubah} ke waktu saat itu. Pemanggil (helper simpan/action ZK)
	 * TIDAK perlu men-set {@code tanggal_dirubah} manual pada jalur update.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor kosong wajib JPA/Hibernate (instansiasi via refleksi saat memuat entity dari DB). */
	public GrupProduk() {
	}

	/**
	 * Representasi ringkas untuk log/debug dan komponen combobox ZK (mis. pemilih grup pada form
	 * {@code Produk}) -- format {@code "<id>-<nama>"}. Kedua bagian diberi penjaga null sehingga
	 * grup baru yang belum tersimpan ({@code id == null}) tetap menghasilkan string tanpa
	 * {@code NullPointerException}.
	 */
	public String toString() {
		return (id == null ? "" : id) + "-" + (nama == null ? "" : nama);
	}

	/**
	 * Kunci primer (identity, auto-generated DB). {@code null} sebelum baris grup pertama kali
	 * disimpan. {@code insertable = false} karena nilainya diserahkan sepenuhnya ke sequence/identity
	 * kolom DB, bukan diisi manual oleh aplikasi.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** Setter {@link #getId()} -- normalnya hanya dipanggil Hibernate saat memuat baris dari DB. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Kode singkat opsional grup (mis. kode internal waralaba) -- murni label, tidak dipakai
	 * sebagai kunci pencarian/unik oleh mesin penyalinan harga di {@code GrupProdukAction.onSave}.
	 */
	@Column(name = "kode", nullable = true, length = 100)
	public String getKode() {
		return kode;
	}

	/** Setter {@link #getKode()}. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Nama grup, wajib diisi -- ditampilkan pada combobox {@link Produk#getGrupProduk()} di form
	 * Produk dan pada daftar CRUD Grup Produk. Getter men-{@code trim()} nilai (spasi tepi
	 * dibuang saat DIBACA, bukan saat disimpan); {@code null} tetap dikembalikan sebagai
	 * {@code null} bila belum diisi.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** Setter {@link #getNama()} -- TIDAK men-trim nilai masukan (trim terjadi di getter). */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** Catatan bebas tentang grup ini (mis. alasan pengelompokan). Opsional, tidak dipakai logika. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return keterangan;
	}

	/** Setter {@link #getKeterangan()}. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** HPP/harga beli terpusat; disalin ke seluruh {@link Produk} anggota saat grup disimpan. */
	@Column(name = "harga_beli", nullable = true)
	public Double getHargaBeli() {
		return hargaBeli;
	}

	/** Setter {@link #getHargaBeli()} -- lihat javadoc getter untuk efek penyalinan ke anggota. */
	public void setHargaBeli(Double hargaBeli) {
		this.hargaBeli = hargaBeli;
	}

	/** Harga jual terpusat; disalin ke seluruh {@link Produk} anggota saat grup disimpan. */
	@Column(name = "harga_jual", nullable = true)
	public Double getHargaJual() {
		return hargaJual;
	}

	/** Setter {@link #getHargaJual()} -- lihat javadoc getter untuk efek penyalinan ke anggota. */
	public void setHargaJual(Double hargaJual) {
		this.hargaJual = hargaJual;
	}

	/**
	 * Resep/bahan baku terpusat -- JSON array format SAMA dgn {@link Produk#getBahanBaku()}
	 * ({@code [{produk_id, nama, qty, harga}, ...]}); disalin apa adanya ke produk anggota
	 * saat {@link #getIkutHpp()} aktif. Snapshot per-baris tetap termaterialisasi di Produk
	 * sehingga jalur baca POS/laporan tidak berubah.
	 */
	@Column(name = "bahan_baku", columnDefinition = "text", nullable = true)
	public String getBahanBaku() {
		return bahanBaku;
	}

	/** Setter {@link #getBahanBaku()} -- lihat javadoc getter untuk format JSON dan efek penyalinan. */
	public void setBahanBaku(String bahanBaku) {
		this.bahanBaku = bahanBaku;
	}

	/**
	 * "HPP selalu mengikuti Grup Produk": bila TRUE, {@code hargaBeli} + {@code bahanBaku}
	 * disalin ke seluruh anggota tiap grup disimpan; bila FALSE grup hanya pengelompokan.
	 * NULL = baris lama sebelum kolom ini ada -- diperlakukan mengikuti perilaku lama
	 * (salin bila {@code hargaBeli} terisi), lihat {@code GrupProdukUtil.ikutHpp}.
	 */
	@Column(name = "ikut_hpp", nullable = true)
	public Boolean getIkutHpp() {
		return ikutHpp;
	}

	/** Setter {@link #getIkutHpp()} -- lihat javadoc getter untuk semantik NULL vs TRUE/FALSE. */
	public void setIkutHpp(Boolean ikutHpp) {
		this.ikutHpp = ikutHpp;
	}

	/** "Harga Jual selalu sama dengan Grup Produk" -- padanan {@link #getIkutHpp()} utk harga jual. */
	@Column(name = "ikut_harga_jual", nullable = true)
	public Boolean getIkutHargaJual() {
		return ikutHargaJual;
	}

	/** Setter {@link #getIkutHargaJual()}. */
	public void setIkutHargaJual(Boolean ikutHargaJual) {
		this.ikutHargaJual = ikutHargaJual;
	}

	/**
	 * Aturan diskon yang berlaku utk SEMUA produk anggota grup ini -- dievaluasi dinamis
	 * oleh mesin diskon POS ({@code KantinHelper.loadAturanDiskonKandidat} sumber ketiga),
	 * TIDAK disalin per-produk.
	 */
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "aturan_diskon", nullable = true)
	public ais.database.model.koperasi.AturanDiskon getAturanDiskon() {
		return aturanDiskon;
	}

	/** Setter {@link #getAturanDiskon()}. */
	public void setAturanDiskon(ais.database.model.koperasi.AturanDiskon aturanDiskon) {
		this.aturanDiskon = aturanDiskon;
	}

	/**
	 * Status aktif grup ini. Getter null-safe: baris lama tanpa nilai eksplisit ({@code null})
	 * dibaca sebagai {@code true} (default aktif) -- konsisten dengan pola getter aktif pada
	 * entity master lain di domain koperasi/inventory (mis. {@link JenisProduk#getAktif()},
	 * {@link SatuanProduk#getAktif()}). Menonaktifkan grup TIDAK melepas keanggotaan
	 * {@link Produk#getGrupProduk()} produk yang sudah menunjuk ke sini -- hanya menyembunyikan
	 * grup dari pilihan combobox aktif (lihat pemakai combobox di form Produk).
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** Setter {@link #getAktif()}. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/** Nama/identitas petugas yang membuat/mengubah baris grup ini -- jejak audit tampilan. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Setter {@link #getOleh()}. Mengabaikan (tidak menimpa nilai lama) bila masukan
	 * {@code null}/kosong-setelah-trim -- pola pengaman umum di entity domain koperasi/inventory
	 * agar jejak audit "oleh" tidak pernah ditimpa kosong oleh pemanggil yang lalai mengisinya.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/** Id/username petugas pasangan {@link #getOleh()} -- jejak audit tampilan, bukan FK. */
	public String getOlehId() {
		return olehId;
	}

	/** Setter {@link #getOlehId()} -- pola pengaman sama dengan {@link #setOleh(String)}. */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Waktu baris ini terakhir diubah. Diinisialisasi ke waktu instansiasi objek pada deklarasi
	 * field, dan diperbarui otomatis oleh {@link #onUpdate()} setiap {@code UPDATE} (hook
	 * {@code @PreUpdate}) -- BEDA dengan {@code AturanHargaProduk.waktu} yang tidak punya hook
	 * serupa dan harus di-set manual pemanggil.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Setter {@link #getTanggal_dirubah()} -- normalnya hanya dipanggil {@link #onUpdate()}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
