package ais.database.model.inventory;

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
 * <h3>Satuan Produk (unit-of-measure) -- master satuan katalog Produk POS, DENGAN konversi
 * antar-satuan sekelompok.</h3>
 *
 * <p>Awalnya master ringan khusus fitur impor/ekspor Excel katalog barang di layar Produk Kasir
 * Desktop/Android (lihat JavaDoc {@code KantinHelper.produkImporExcel}), kini juga menjadi mesin
 * konversi UOM (Pcs/Dus/Kg/Gram/Liter/dst) yang dipakai saat pembelian/penjualan memakai satuan
 * berbeda dari satuan stok dasar produk (lihat {@code KantinHelper.faktorUomInputKeDasar}). Tidak
 * ada master satuan bersama sebelumnya di modul inventory ({@code rab.Satuan}/
 * {@code asset.SatuanMasterAsset} milik modul lain, tidak dipakai ulang di sini supaya tidak
 * menaut lintas-modul yang tak berkaitan).</p>
 *
 * <p><b>Model konversi -- "kategori" + satu satuan acuan per kategori:</b> setiap baris
 * dikelompokkan oleh {@link #getKategori()} (mis. {@code "BERAT"}, {@code "VOLUME"}, atau
 * {@code "UNIT"} default). Dalam satu kategori, TEPAT SATU baris boleh berperan sebagai acuan
 * ({@link #getTipeKonversi()} {@code = "REFERENCE"}, rasio dipaksa 1.0); baris lain berupa
 * {@code "BIGGER"} (mis. Dus, {@link #getRasio()} = berapa satuan acuan setara 1 satuan ini --
 * dikalikan langsung) atau {@code "SMALLER"} (mis. Gram bila acuannya Kg, {@link #getRasio()} =
 * berapa satuan ini setara 1 satuan acuan -- faktor konversi ke acuan adalah KEBALIKANNYA,
 * {@code 1/rasio}). Konversi antara dua satuan NON-acuan sekategori dihitung lewat acuan:
 * {@code faktor(dariUnitA ke unitB) = faktorKeAcuan(A) / faktorKeAcuan(B)}. Mengonversi lintas
 * kategori berbeda (mis. Kg ke Liter tanpa densitas) ditolak sebagai error tervalidasi di sisi
 * pemanggil, BUKAN oleh entity ini -- kelas ini murni penyimpan data, validasi/aritmetika hidup di
 * {@code KantinHelper}.</p>
 *
 * <p><b>Rawan bug rounding/konversi</b> -- yang perlu diperhatikan pemanggil: (1) kategori kosong
 * di DB dibaca sebagai {@code "UNIT"} oleh {@link #getKategori()}, jadi SEMUA baris lama tanpa
 * kategori eksplisit otomatis sekelompok dan bisa saling dikonversi walau secara fisik tidak
 * sepadan -- migrasi data lama WAJIB mengisi kategori yang benar sebelum fitur konversi lintas-UOM
 * diaktifkan; (2) {@link #getPresisiPembulatan()} (default 0.01) disimpan dan dapat diedit lewat
 * {@code SatuanProdukApiHelper.satuanProdukSimpan}. Sejak diwire ke {@code KantinHelper.
 * bulatkanKePresisi}, dipakai HANYA oleh {@code KantinHelper.terapkanSatuanJual} (jalur POS
 * penjualan Kantin) untuk membulatkan {@code jumlah} hasil konversi UOM ke presisi satuan
 * dasar produk. Pemanggil {@code faktorUomInputKeDasar} lain di luar jalur itu --
 * {@code SalesInventoryReceivableHelper} (Fase B) dan {@code StokThresholdScheduler} (Fase C) --
 * menghitung kuantitasnya sendiri dari faktor mentah dan TIDAK ikut membulatkan lewat presisi
 * ini kecuali dipanggil eksplisit di sana. Jangan asumsikan presisi ini diterapkan di luar jalur
 * penjualan Kantin tanpa memverifikasi ulang pemanggilnya.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "satuan_produk")
public class SatuanProduk extends GeneralValueObject {

	private static final long serialVersionUID = 1L;
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

	/** Nama/identitas petugas yang membuat/mengubah baris satuan ini -- jejak audit tampilan. */
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
	private Boolean aktif;
	private String kategori;
	private String tipeKonversi;
	private Double rasio;
	private Double presisiPembulatan;

	/** Konstruktor kosong wajib JPA/Hibernate (instansiasi via refleksi saat memuat entity dari DB). */
	public SatuanProduk() {
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
	 * Nama satuan, wajib diisi (mis. "Pcs", "Kg", "Dus") -- ditampilkan pada combobox
	 * {@link Produk#getSatuan()}/{@link Produk#getSatuanPembelian()} di form Produk dan pada
	 * daftar CRUD Satuan/UOM. Getter men-{@code trim()} nilai (spasi tepi dibuang saat DIBACA,
	 * bukan saat disimpan); {@code null} tetap dikembalikan sebagai {@code null} bila belum diisi.
	 */
	@Column(name = "nama", nullable = false, length = 100)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** Setter {@link #getNama()} -- TIDAK men-trim nilai masukan (trim terjadi di getter). */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Status aktif satuan ini. Getter null-safe: baris lama tanpa nilai eksplisit ({@code null})
	 * dibaca sebagai {@code true} (default aktif) -- konsisten dengan pola getter aktif pada
	 * entity master lain di domain koperasi/inventory (mis. {@link GrupProduk#getAktif()},
	 * {@link JenisProduk#getAktif()}). Menonaktifkan satuan TIDAK melepas relasi
	 * {@link Produk#getSatuan()}/{@link Produk#getSatuanPembelian()} produk yang sudah menunjuk
	 * ke sini -- hanya menyembunyikannya dari pilihan combobox aktif.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** Setter {@link #getAktif()}. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Kelompok konversi satuan ini (mis. {@code "BERAT"}, {@code "VOLUME"}) -- satuan hanya bisa
	 * dikonversi ke satuan lain dengan kategori SAMA (lihat penjelasan model konversi pada javadoc
	 * kelas). Getter menormalkan {@code null}/kosong menjadi {@code "UNIT"} dan huruf besar semua
	 * -- baris lama tanpa kategori eksplisit otomatis masuk kelompok {@code "UNIT"} bersama-sama,
	 * lihat catatan "Rawan bug rounding/konversi" pada javadoc kelas.
	 */
	@Column(name = "kategori", nullable = true, length = 50)
	public String getKategori() {
		return kategori == null || kategori.trim().isEmpty() ? "UNIT" : kategori.trim().toUpperCase();
	}

	/** Setter {@link #getKategori()} -- TIDAK menormalkan huruf besar/trim (normalisasi di getter). */
	public void setKategori(String kategori) {
		this.kategori = kategori;
	}

	/**
	 * Peran satuan ini dalam konversi kategorinya: {@code "REFERENCE"} (satuan acuan, tepat satu
	 * per kategori), {@code "BIGGER"} (lebih besar dari acuan), atau {@code "SMALLER"} (lebih
	 * kecil dari acuan) -- lihat penjelasan lengkap arah rasio pada javadoc kelas. Getter
	 * menormalkan {@code null}/kosong menjadi {@code "REFERENCE"} -- baris lama sebelum kolom ini
	 * ada otomatis diperlakukan sebagai satuan acuan dengan rasio 1:1 (perilaku aman, tidak
	 * memaksa konversi tak terduga pada data lama).
	 */
	@Column(name = "tipe_konversi", nullable = true, length = 20)
	public String getTipeKonversi() {
		return tipeKonversi == null || tipeKonversi.trim().isEmpty() ? "REFERENCE" : tipeKonversi.trim().toUpperCase();
	}

	/** Setter {@link #getTipeKonversi()} -- TIDAK menormalkan huruf besar/trim (normalisasi di getter). */
	public void setTipeKonversi(String tipeKonversi) {
		this.tipeKonversi = tipeKonversi;
	}

	/**
	 * Rasio konversi terhadap satuan acuan kategori ini -- ARAH pembacaannya bergantung
	 * {@link #getTipeKonversi()}: pada {@code "BIGGER"} berarti "1 satuan ini = {@code rasio}
	 * satuan acuan" (dikalikan langsung saat konversi ke acuan); pada {@code "SMALLER"} berarti
	 * "1 satuan acuan = {@code rasio} satuan ini" (faktor ke acuan adalah kebalikannya,
	 * {@code 1/rasio}); pada {@code "REFERENCE"} diabaikan (selalu diperlakukan 1.0 oleh
	 * pemanggil). Getter null-safe DAN menjaga terhadap nilai non-positif: {@code null} atau
	 * {@code <= 0} dibaca sebagai {@code 1.0}. Konsekuensi: {@code KantinHelper.faktorUomKeAcuan}
	 * memanggil getter ini (BUKAN field mentah) lalu memeriksa ulang {@code rasio <= 0.0} untuk
	 * melempar {@code IllegalArgumentException} -- karena getter ini SUDAH menormalkan nilai
	 * non-positif menjadi {@code 1.0} sebelum sempat diperiksa, pemeriksaan ulang itu praktis
	 * TIDAK PERNAH tercapai lewat jalur ini; satu-satunya efek nyata rasio non-positif/null adalah
	 * diam-diam diperlakukan sebagai 1:1, bukan ditolak sebagai error seperti pesan errornya
	 * menyiratkan.
	 */
	@Column(name = "rasio", nullable = true)
	public Double getRasio() {
		return rasio == null || rasio.doubleValue() <= 0.0 ? Double.valueOf(1.0) : rasio;
	}

	/** Setter {@link #getRasio()} -- tidak ada validasi nilai positif di setter (validasi di getter/pemanggil). */
	public void setRasio(Double rasio) {
		this.rasio = rasio;
	}

	/**
	 * Presisi pembulatan (Rupiah/kuantitas desimal, default {@code 0.01}) untuk hasil konversi
	 * satuan ini (kuantitas dalam satuan dasar produk). Getter null-safe dan menjaga nilai
	 * non-positif: {@code null} atau {@code <= 0} dibaca sebagai {@code 0.01}.
	 * <p>Dipakai {@code KantinHelper.bulatkanKePresisi} lewat {@code terapkanSatuanJual} (jalur
	 * POS penjualan Kantin) -- lihat catatan "Rawan bug rounding/konversi" pada javadoc kelas
	 * untuk cakupan pemanggil lain yang BELUM ikut membulatkan.</p>
	 */
	@Column(name = "presisi_pembulatan", nullable = true)
	public Double getPresisiPembulatan() {
		return presisiPembulatan == null || presisiPembulatan.doubleValue() <= 0.0
				? Double.valueOf(0.01) : presisiPembulatan;
	}

	/** Setter {@link #getPresisiPembulatan()}. */
	public void setPresisiPembulatan(Double presisiPembulatan) {
		this.presisiPembulatan = presisiPembulatan;
	}

}
