package ais.database.model.inventory;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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

import ais.database.model.GeneralValueObject;

/**
 * <h3>Jenis Produk -- kategori klasifikasi produk yang membawa pemetaan akun akunting.</h3>
 *
 * <p>DIMENSI KLASIFIKASI YANG BERBEDA dari {@link GrupProduk} meski nama keduanya mirip
 * ("jenis" vs "grup"): {@link Produk} memiliki DUA relasi terpisah,
 * {@link Produk#getJenisProduk()} dan {@link Produk#getGrupProduk()}, yang TIDAK saling
 * bergantung satu sama lain.
 * <ul>
 *   <li>{@code JenisProduk} (kelas ini) = kategori akunting per produk (mis. "Makanan",
 *       "Minuman", "ATK") -- setiap jenis membawa pemetaan {@link #getAkunPendapatan()},
 *       {@link #getAkunPpnKeluaran()}, {@link #getAkunHpp()}, {@link #getAkunSelisihPersediaan()},
 *       dan {@link #getAkunReturPenjualan()} yang dipakai mesin Posting Penjualan/HPP/Retur
 *       Kantin untuk menentukan akun jurnal yang benar per kategori produk. Berlaku LINTAS TOKO
 *       (bukan per-outlet).</li>
 *   <li>{@link GrupProduk} = pengelompokan HARGA terpusat lintas toko (opsional) -- lihat javadoc
 *       kelas itu. Satu produk bisa punya {@code JenisProduk} tanpa {@code GrupProduk}, dan
 *       sebaliknya; keduanya independen dan boleh dikombinasikan bebas.</li>
 * </ul>
 * </p>
 *
 * <p>Selain pemetaan akun, kelas ini juga membawa {@link #getMaksimalHarian()} -- batas nominal
 * pembelian harian per jenis produk yang ditegakkan {@code PembelianAction} saat siswa/mahasiswa
 * berbelanja di kantin (mis. membatasi jajan "Makanan Ringan" maksimal Rp X/hari) -- fitur ini
 * TIDAK berkaitan dengan akunting, murni kontrol pengeluaran pembeli.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "jenis_produk")

public class JenisProduk extends GeneralValueObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/** Id/username petugas pasangan {@link #getOleh()} -- jejak audit tampilan, bukan FK. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Setter {@link #getOlehId()}. Mengabaikan (tidak menimpa nilai lama) bila masukan
	 * {@code null}/kosong-setelah-trim -- pola pengaman umum di entity domain koperasi/inventory
	 * agar jejak audit "olehId" tidak pernah ditimpa kosong oleh pemanggil yang lalai mengisinya.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/** Setter {@link #getOleh()} -- pola pengaman sama dengan {@link #setOlehId(String)}. */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/** Nama/identitas petugas yang membuat/mengubah baris jenis produk ini -- jejak audit tampilan. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil otomatis Hibernate tepat sebelum tiap {@code UPDATE}
	 * baris ini, mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah}
	 * yang menyetel {@link #tanggal_dirubah} ke waktu saat itu. Deklarasi one-liner mengikuti gaya
	 * berkas hbm2java asli (tidak dirapikan agar diff minimal terhadap riwayat SVN).
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Setter {@link #getTanggal_dirubah()} -- normalnya hanya dipanggil {@link #onUpdate()}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Waktu baris ini terakhir diubah. Diinisialisasi ke waktu instansiasi objek pada deklarasi
	 * field, dan diperbarui otomatis oleh {@link #onUpdate()} setiap {@code UPDATE}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Representasi ringkas untuk log/debug dan combobox ZK -- hanya {@link #getNama()}. */
	public String toString() {
		return nama == null ? "" : nama;
	}

	private String nama;
	private String keterangan;
	private Double maksimalHarian;
	private Boolean defaultProduk = false;
	private Boolean aktif;

	/** Konstruktor kosong wajib JPA/Hibernate (instansiasi via refleksi saat memuat entity dari DB). */
	public JenisProduk() {
	}

	/**
	 * Kunci primer (identity, auto-generated DB). {@code null} sebelum baris pertama kali disimpan.
	 * {@code insertable = false} karena nilainya diserahkan sepenuhnya ke sequence/identity kolom
	 * DB, bukan diisi manual oleh aplikasi.
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
	 * Nama jenis produk, wajib diisi -- ditampilkan pada combobox {@link Produk#getJenisProduk()}
	 * di form Produk dan pada daftar CRUD Jenis Produk. Getter men-{@code trim()} nilai (spasi
	 * tepi dibuang saat DIBACA, bukan saat disimpan); {@code null} tetap dikembalikan sebagai
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

	/** Catatan bebas tentang jenis produk ini. Opsional, tidak dipakai logika posting/limit. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** Setter {@link #getKeterangan()}. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Penanda "jenis produk bawaan". Getter null-safe: menormalkan {@code null} menjadi
	 * {@code false} (BEDA dengan pola {@code getAktif()} di kelas ini dan entity lain domain
	 * inventory/koperasi yang menormalkan {@code null} menjadi {@code true} -- di sini defaultnya
	 * justru "bukan default"). Field ini disimpan dan dikembalikan API CRUD
	 * ({@code JenisProdukApiHelper.jenisProdukList}/{@code jenisProdukSimpan}) -- TIDAK ada
	 * kontrolnya sama sekali di form ZK admin ({@code JenisProdukAction}), hanya bisa diisi lewat
	 * API tersebut (dipakai app eBisnis/Flutter di luar repo ini).
	 * <p><b>TODO (belum diimplementasikan):</b> pada saat dokumentasi ini ditulis TIDAK ADA jalur
	 * baca di backend Java yang mengonsultasikan {@link #getDefaultProduk()} untuk memilih jenis
	 * "default" mana pun (mis. saat membuat {@link Produk} baru) -- tidak ditemukan jalur
	 * server-side pembuatan {@link Produk} baru yang bisa bermakna memakai flag ini (pembuatan
	 * Produk didorong klien). Nilai flag SUDAH ikut dikembalikan {@code jenisProdukList} sehingga
	 * app eBisnis/Flutter mungkin sudah mengonsumsinya sendiri di sisi klien -- jangan asumsikan
	 * flag ini mati total tanpa memeriksa app tersebut. Sebelum menambah logika baru yang
	 * bergantung padanya di backend, putuskan dulu apakah "jenis produk default" adalah
	 * tanggung jawab server (perlu endpoint/logika baru) atau murni konvensi klien.</p>
	 */
	public Boolean getDefaultProduk() {
		if (defaultProduk == null) {
			defaultProduk = false;
		}
		return defaultProduk;
	}

	/** Setter {@link #getDefaultProduk()}. */
	public void setDefaultProduk(Boolean defaultProduk) {
		this.defaultProduk = defaultProduk;
	}

	/**
	 * Batas nominal (Rupiah) pembelian harian per pembeli untuk produk-produk berjenis ini --
	 * ditegakkan {@code PembelianAction} (kantin sekolah/kampus): saat total harga pembelian hari
	 * berjalan pada satu jenis produk melebihi angka ini, transaksi baru DITOLAK dengan pesan
	 * peringatan (lihat penjumlahan per-{@code jenisProduk.id} pada listener tombol "Simpan" di
	 * {@code PembelianAction.doAfterCompose}). Getter null-safe: baris lama tanpa nilai eksplisit
	 * dibaca sebagai Rp 100.000.000 (praktis
	 * tanpa batas) -- default permisif, bukan default membatasi, agar jenis produk lama yang belum
	 * pernah diisi tidak mendadak memblokir transaksi.
	 */
	public Double getMaksimalHarian() {
		return maksimalHarian == null ? 100000000.0 : maksimalHarian;
	}

	/** Setter {@link #getMaksimalHarian()}. */
	public void setMaksimalHarian(Double maksimalHarian) {
		this.maksimalHarian = maksimalHarian;
	}

	/**
	 * Status aktif jenis produk ini. Getter null-safe: baris lama tanpa nilai eksplisit
	 * ({@code null}) dibaca sebagai {@code true} (default aktif) -- konsisten dengan pola getter
	 * aktif pada entity master lain di domain koperasi/inventory (mis.
	 * {@link GrupProduk#getAktif()}, {@link SatuanProduk#getAktif()}). Menonaktifkan jenis ini
	 * TIDAK melepas relasi {@link Produk#getJenisProduk()} produk yang sudah menunjuk ke sini --
	 * hanya menyembunyikannya dari pilihan combobox aktif.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** Setter {@link #getAktif()}. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	private ais.database.model.akunting.Akun akunPendapatan;
	private ais.database.model.akunting.Akun akunPpnKeluaran;

	/**
	 * Akun <b>Pendapatan Penjualan</b> (posisi KREDIT) untuk fitur Posting Penjualan Kantin —
	 * ditetapkan per jenis produk sehingga tiap kategori bisa memakai akun pendapatan berbeda.
	 * {@code @NotAudited}: field pemetaan akun, tak perlu histori audit (hindari sinkron
	 * {@code new_audit.jenis_produk__audit}). Kolom FK {@code akun_pendapatan} ke {@code akunting.akun}.
	 */
	@org.hibernate.envers.NotAudited
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "akun_pendapatan", nullable = true)
	public ais.database.model.akunting.Akun getAkunPendapatan() {
		akunPendapatan = check(akunPendapatan);
		return akunPendapatan;
	}

	/** Setter {@link #getAkunPendapatan()}. */
	public void setAkunPendapatan(ais.database.model.akunting.Akun akunPendapatan) {
		this.akunPendapatan = akunPendapatan;
	}

	/**
	 * Akun <b>PPN Keluaran</b> (posisi KREDIT) untuk Posting Penjualan Kantin — per jenis produk.
	 * {@code @NotAudited}. Kolom FK {@code akun_ppn_keluaran} ke {@code akunting.akun}.
	 */
	@org.hibernate.envers.NotAudited
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "akun_ppn_keluaran", nullable = true)
	public ais.database.model.akunting.Akun getAkunPpnKeluaran() {
		akunPpnKeluaran = check(akunPpnKeluaran);
		return akunPpnKeluaran;
	}

	/** Setter {@link #getAkunPpnKeluaran()}. */
	public void setAkunPpnKeluaran(ais.database.model.akunting.Akun akunPpnKeluaran) {
		this.akunPpnKeluaran = akunPpnKeluaran;
	}

	private ais.database.model.akunting.Akun akunHpp;

	/**
	 * Akun <b>Beban Pokok Penjualan (HPP)</b> (posisi DEBIT) untuk Posting HPP Kantin — per jenis
	 * produk. Bila diisi, dipakai lebih dulu daripada akun HPP di Kelompok Aset. {@code @NotAudited}.
	 * Kolom FK {@code akun_hpp} ke {@code akunting.akun}.
	 */
	@org.hibernate.envers.NotAudited
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "akun_hpp", nullable = true)
	public ais.database.model.akunting.Akun getAkunHpp() {
		akunHpp = check(akunHpp);
		return akunHpp;
	}

	/** Setter {@link #getAkunHpp()}. */
	public void setAkunHpp(ais.database.model.akunting.Akun akunHpp) {
		this.akunHpp = akunHpp;
	}


	/**
	 * Akun selisih persediaan (susut/temuan).
	 * <p>Lawan jurnal saat selisih stok opname dicatat; sejajar dengan Akun HPP pada master yang sama. Ditempelkan pada master ini (bukan konfigurasi global) supaya
	 * tiap outlet/jenis bisa berbeda; konfigurasi global tetap dipakai sebagai cadangan terakhir
	 * agar pemasangan lama tidak berubah perilakunya. Kolomnya dibuat otomatis oleh Hibernate.</p>
	 */
	private ais.database.model.akunting.Akun akunSelisihPersediaan;

	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "akun_selisih_persediaan", nullable = true)
	public ais.database.model.akunting.Akun getAkunSelisihPersediaan() {
		akunSelisihPersediaan = check(akunSelisihPersediaan);
		return akunSelisihPersediaan;
	}

	/** Setter {@link #getAkunSelisihPersediaan()}. */
	public void setAkunSelisihPersediaan(ais.database.model.akunting.Akun akunSelisihPersediaan) {
		this.akunSelisihPersediaan = akunSelisihPersediaan;
	}


	/**
	 * Akun Retur Penjualan (kontra-pendapatan).
	 * <p>Didebet saat retur penjualan dijurnal. Ditempelkan pada master ini &mdash; sejajar dengan
	 * Akun Pendapatan/PPN/HPP &mdash; supaya tiap jenis produk bisa memakai akun retur sendiri.
	 * Bila kosong, jurnal memakai akun pendapatan jenis produk yang bersangkutan. Kolomnya dibuat
	 * otomatis oleh Hibernate.</p>
	 */
	private ais.database.model.akunting.Akun akunReturPenjualan;

	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "akun_retur_penjualan", nullable = true)
	public ais.database.model.akunting.Akun getAkunReturPenjualan() {
		akunReturPenjualan = check(akunReturPenjualan);
		return akunReturPenjualan;
	}

	/** Setter {@link #getAkunReturPenjualan()}. */
	public void setAkunReturPenjualan(ais.database.model.akunting.Akun akunReturPenjualan) {
		this.akunReturPenjualan = akunReturPenjualan;
	}

}
