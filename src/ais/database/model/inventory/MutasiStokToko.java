package ais.database.model.inventory;

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
 * Mutasi Stok Antar Outlet (Fase 3 roadmap F&amp;B, klien item #5) -- ledger bertanda (signed) yang
 * menyambung LANGSUNG ke {@link ais.action.master.inventory.StokKantinUtil#formulaStokSql} (pola
 * SAMA PERSIS {@code pemakaian_bahan_baku}), BUKAN migrasi ke mesin {@code MutasiLokasi}/Gudang yang
 * sudah ada -- mesin itu terputus dari {@link Produk#getStok()} yang dipakai POS/Kasir.
 *
 * <p>Karena tiap outlet punya baris {@link Produk} TERPISAH utk "barang yang sama" (skema toko-per-baris
 * yang sudah berlaku di seluruh sistem ini), satu transfer dicatat sbg SATU baris menunjuk KEDUA sisi
 * sekaligus: {@code produkAsal} (baris Produk milik toko asal, stoknya BERKURANG) dan
 * {@code produkTujuan} (baris Produk milik toko tujuan, stoknya BERTAMBAH) -- bukan dua baris terpisah
 * spt {@code Deposit}/{@code Pembelian}. {@code tokoAsal}/{@code tokoTujuan} didenormalisasi (ikut
 * disimpan) murni utk mempercepat filter riwayat per-toko tanpa join ke {@code koperasi.produk}.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "mutasi_stok_toko")
public class MutasiStokToko extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	/** Primary key baris mutasi. Digenerasi database ({@code IDENTITY}); {@code null} sebelum baris pertama kali disimpan. */
	private Long id;
	/** Baris {@link Produk} milik toko ASAL -- sisi yang stoknya BERKURANG oleh transfer ini. Lihat javadoc {@link #getProdukAsal()} untuk pembahasan lengkap desain dua-sisi-satu-baris. */
	private Produk produkAsal;
	/** Baris {@link Produk} milik toko TUJUAN -- sisi yang stoknya BERTAMBAH oleh transfer ini. Lihat javadoc {@link #getProdukTujuan()} untuk pembahasan lengkap. */
	private Produk produkTujuan;
	/** Toko asal, didenormalisasi dari {@link #produkAsal}{@code .getToko()} murni untuk mempercepat filter riwayat per-toko tanpa join ke {@code koperasi.produk}. */
	private Toko tokoAsal;
	/** Toko tujuan, didenormalisasi dari {@link #produkTujuan}{@code .getToko()} murni untuk mempercepat filter riwayat per-toko tanpa join ke {@code koperasi.produk}. */
	private Toko tokoTujuan;

	/** Kuantitas yang ditransfer -- SATU angka yang berlaku bagi KEDUA sisi (dikurangkan dari produk asal, ditambahkan ke produk tujuan). Lihat javadoc {@link #getQty()}. */
	private Double qty;
	/** Waktu transfer ini terjadi/dicatat. */
	private Date waktu;
	/** Catatan bebas teks untuk transfer ini, opsional (mis. alasan transfer antar-outlet). */
	private String keterangan;
	/** Userid/nama yang melakukan transfer (jejak audit ringan, bebas teks, tidak ber-FK), terpisah dari mekanisme envers ({@code @Audited}). */
	private String oleh;
	/**
	 * Hook lifecycle Hibernate dipanggil otomatis sebelum setiap {@code UPDATE} terhadap baris mutasi ini
	 * (dipicu anotasi {@link javax.persistence.PreUpdate}, BUKAN dipanggil manual). Mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang menstempel ulang {@link
	 * #tanggal_dirubah}. Method ini murni hook siklus hidup entity -- tidak melakukan validasi
	 * keseimbangan transfer (mis. memastikan {@link #getQty()} tetap konsisten dengan histori FEFO batch
	 * terkait); validasi semacam itu, bila ada, berada di lapisan service pemanggil (lihat pembahasan di
	 * {@link #getProdukAsal()}), bukan di model ini, dan lagipula hook ini hanya bereaksi pada
	 * {@code UPDATE} -- baris mutasi pada praktiknya adalah ledger append-only yang jarang di-update
	 * setelah pertama kali disimpan.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}
	/**
	 * Stempel waktu terakhir baris ini diubah -- field audit shadow diisi otomatis oleh {@link
	 * #onUpdate()} pada tiap {@code UPDATE}, terpisah dari riwayat versi penuh envers ({@code @Audited}).
	 * Inisialisasi default memakai waktu konstruksi objek Java, bukan waktu commit transaksi.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Constructor default tanpa argumen, dipakai Hibernate untuk instansiasi entity via refleksi saat memuat baris dari database; kode aplikasi yang mencatat transfer baru juga memakainya lalu mengisi field lewat setter (lihat {@code KantinHelper}, fitur "Mutasi Stok Antar Outlet"). */
	public MutasiStokToko() {
	}

	/**
	 * Primary key baris mutasi ini. Digenerasi database via strategi {@code IDENTITY}; {@code null}
	 * pada objek yang belum pernah di-{@code save}.
	 * @return id baris, atau {@code null} jika belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id id baris. Normalnya TIDAK diisi manual -- dibiarkan Hibernate mengisinya via {@code IDENTITY} saat insert. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Baris {@link Produk} milik toko ASAL -- sisi yang stoknya BERKURANG oleh transfer ini. Relasi
	 * {@code LAZY}, wajib diisi.
	 *
	 * <p><b>Penjaga keseimbangan mutasi (audit domain rawan barang hilang/duplikasi tanpa jejak).</b>
	 * Berbeda dari desain mutasi dua-baris-terpisah yang lazim di modul finansial lain kelas ini (mis.
	 * {@code Deposit}/{@code Pembelian}, di mana debit dan kredit masing-masing baris terpisah dan
	 * secara teoretis bisa "kehilangan pasangannya" bila salah satu gagal tersimpan), transfer antar
	 * outlet di kelas ini SENGAJA dicatat sebagai SATU baris tunggal yang menunjuk KEDUA sisi sekaligus
	 * ({@link #produkAsal} dan {@link #produkTujuan}) dengan SATU angka {@link #getQty()} yang berlaku
	 * untuk keduanya. Ini adalah bentuk penjaga keseimbangan STRUKTURAL, bukan sekadar konvensi
	 * pemrograman: secara matematis TIDAK MUNGKIN satu baris {@code MutasiStokToko} mencatat qty keluar
	 * berbeda dari qty masuk, karena hanya ada SATU kolom {@code qty} yang dibaca oleh KEDUA arah --
	 * tidak ada kombinasi input yang bisa membuat sisi asal berkurang N unit sementara sisi tujuan
	 * bertambah M&ne;N unit, sebagaimana bisa terjadi pada desain dua-baris independen yang rawan
	 * inkonsistensi bila salah satu baris gagal ditulis atau diubah terpisah setelah tersimpan.</p>
	 *
	 * <p><b>Verifikasi pada jalur penulisan nyata.</b> Penelusuran pemanggil tunggal kelas ini ({@code
	 * KantinHelper}, fitur "Mutasi Stok Antar Outlet") mengkonfirmasi pola ini diikuti secara konsisten:
	 * satu baris {@code MutasiStokToko} dibuat dalam SATU transaksi database bersama pemanggilan {@code
	 * transferBatchFefo} (memindahkan batch FEFO/kadaluarsa dari sisi asal ke sisi tujuan) dan KEDUA
	 * panggilan {@code StokKantinUtil.recomputeStokProdukNative} (satu untuk {@code produkAsal.getId()},
	 * satu untuk {@code produkTujuan.getId()}) sebelum {@code commit()} -- baris ledger dan efek stok
	 * kedua sisi selalu tersinkron dalam satu unit kerja atomik. Karena {@code recomputeStokProdukNative}
	 * pada kedua sisi membaca ULANG dari SATU baris ledger yang sama (bukan dari dua sumber independen),
	 * stok kedua sisi selalu konsisten dengan ledger -- tidak ada window waktu di mana sisi asal sudah
	 * berkurang tapi sisi tujuan belum bertambah (atau sebaliknya) yang bisa dieksploitasi dengan
	 * membaca stok di antara kedua operasi, karena keduanya berada dalam transaksi yang sama.</p>
	 *
	 * <p><b>Batas dari jaminan ini -- yang TIDAK dijamin oleh struktur satu-baris.</b> Penjaga
	 * keseimbangan ini menjamin KONSISTENSI INTERNAL (qty keluar selalu sama dengan qty masuk dalam satu
	 * transfer), tapi TIDAK menjamin bahwa transfer itu SENDIRI valid secara bisnis -- tidak ada
	 * pemeriksaan pada level model bahwa {@link #produkAsal} benar-benar punya stok mencukupi sebelum
	 * transfer dicatat (baris bisa tersimpan dengan {@code qty} melebihi stok tersedia, menghasilkan
	 * stok negatif di sisi asal setelah recompute -- validasi ketersediaan, bila ada, berada di kode
	 * pemanggil {@code KantinHelper}, bukan di model ini). Juga tidak ada validasi bahwa {@link
	 * #produkAsal} dan {@link #produkTujuan} benar-benar merepresentasikan "barang yang sama" secara
	 * substansi (auto-match berbasis kode/barcode dilakukan di lapisan pemanggil sebelum baris ini
	 * dibuat, bukan divalidasi ulang di sini) -- operator dengan akses supervisor/admin (gerbang akses
	 * SENGAJA dibuat ketat persis karena keterbatasan ini) secara teknis bisa membuat baris yang
	 * menunjuk dua produk yang tidak berhubungan sama sekali.</p>
	 *
	 * @return baris produk asal transfer (bisa proxy lazy).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk_asal", nullable = false)
	public Produk getProdukAsal() {
		produkAsal = check(produkAsal);
		return produkAsal;
	}

	/** @param produkAsal baris produk asal transfer (stoknya akan berkurang sejumlah {@link #getQty()}). */
	public void setProdukAsal(Produk produkAsal) {
		this.produkAsal = produkAsal;
	}

	/**
	 * Baris {@link Produk} milik toko TUJUAN -- sisi yang stoknya BERTAMBAH oleh transfer ini. Relasi
	 * {@code LAZY}, wajib diisi. Lihat javadoc {@link #getProdukAsal()} untuk pembahasan lengkap desain
	 * penjaga keseimbangan satu-baris-dua-sisi yang berlaku sama untuk field ini.
	 * @return baris produk tujuan transfer (bisa proxy lazy).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk_tujuan", nullable = false)
	public Produk getProdukTujuan() {
		produkTujuan = check(produkTujuan);
		return produkTujuan;
	}

	/** @param produkTujuan baris produk tujuan transfer (stoknya akan bertambah sejumlah {@link #getQty()}). */
	public void setProdukTujuan(Produk produkTujuan) {
		this.produkTujuan = produkTujuan;
	}

	/**
	 * Toko asal, didenormalisasi dari {@code produkAsal.getToko()} murni untuk mempercepat filter
	 * riwayat per-toko tanpa join ke {@code koperasi.produk}. Relasi {@code LAZY}, wajib diisi.
	 * Konsistensinya dengan {@link #getProdukAsal()}{@code .getToko()} bergantung sepenuhnya pada
	 * disiplin kode pemanggil saat menyimpan baris ({@code KantinHelper} men-set keduanya searah dari
	 * {@code produkAsal.getToko()} pada saat pembuatan) -- model ini TIDAK menegakkan konsistensi ini
	 * lewat constraint atau validasi apa pun; kolom denormalisasi ini bisa menyimpang dari relasi
	 * kanonik bila di-{@code set} independen oleh kode lain di masa depan.
	 * @return toko asal transfer (bisa proxy lazy, didenormalisasi).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko_asal", nullable = false)
	public Toko getTokoAsal() {
		tokoAsal = check(tokoAsal);
		return tokoAsal;
	}

	/** @param tokoAsal toko asal transfer (denormalisasi, idealnya selalu sama dengan {@code getProdukAsal().getToko()}). */
	public void setTokoAsal(Toko tokoAsal) {
		this.tokoAsal = tokoAsal;
	}

	/**
	 * Toko tujuan, didenormalisasi dari {@code produkTujuan.getToko()} murni untuk mempercepat filter
	 * riwayat per-toko tanpa join ke {@code koperasi.produk}. Sama seperti {@link #getTokoAsal()},
	 * konsistensinya dengan relasi kanonik bergantung pada disiplin kode pemanggil, tidak ditegakkan
	 * oleh model ini.
	 * @return toko tujuan transfer (bisa proxy lazy, didenormalisasi).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko_tujuan", nullable = false)
	public Toko getTokoTujuan() {
		tokoTujuan = check(tokoTujuan);
		return tokoTujuan;
	}

	/** @param tokoTujuan toko tujuan transfer (denormalisasi, idealnya selalu sama dengan {@code getProdukTujuan().getToko()}). */
	public void setTokoTujuan(Toko tokoTujuan) {
		this.tokoTujuan = tokoTujuan;
	}

	/**
	 * Kuantitas yang ditransfer -- SATU angka yang berlaku bagi KEDUA sisi transfer: dikurangkan dari
	 * stok {@link #getProdukAsal()}, ditambahkan ke stok {@link #getProdukTujuan()}. Karena hanya ada
	 * satu kolom qty untuk kedua arah (bukan kolom terpisah "qtyKeluar"/"qtyMasuk" seperti pada {@link
	 * MutasiStokProduksi}), penjaga keseimbangan kuantitas transfer bersifat struktural -- lihat
	 * pembahasan lengkap di javadoc {@link #getProdukAsal()}. {@code null} dinormalisasi menjadi
	 * {@code 0.0}. Tidak ada validasi pada level model bahwa nilai ini positif -- qty nol atau negatif
	 * secara teknis bisa tersimpan bila tidak dicegah lebih dulu oleh kode pemanggil (kontrak {@code
	 * KantinHelper} mensyaratkan {@code qty > 0} sebagai validasi lapisan pemanggil, bukan di sini).
	 * @return kuantitas transfer, tidak pernah {@code null} (default {@code 0.0}).
	 */
	@Column(name = "qty")
	public Double getQty() {
		return qty == null ? 0.0 : qty;
	}

	/** @param qty kuantitas transfer; boleh {@code null} (dibaca sebagai {@code 0.0}). */
	public void setQty(Double qty) {
		this.qty = qty;
	}

	/**
	 * Waktu transfer ini terjadi/dicatat. {@code null} dibaca sebagai waktu-baca saat ini ({@link
	 * ais.ui.util.WaktuUtil#getDate()}) -- nilai ini dihitung ulang setiap getter dipanggil selama field
	 * mentah masih {@code null}, sama seperti pola default-waktu pada {@link StokOpname#getWaktuOpname()}.
	 * @return waktu transfer; default waktu-baca saat ini bila belum pernah diisi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu")
	public Date getWaktu() {
		return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu;
	}

	/** @param waktu waktu transfer ini terjadi/dicatat. */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Catatan bebas teks untuk transfer ini (mis. alasan transfer antar-outlet), opsional, kolom
	 * {@code text} tanpa batas panjang keras.
	 * @return catatan/keterangan transfer, atau {@code null} bila tidak diisi.
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/** @param keterangan catatan/keterangan transfer ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Userid/nama yang melakukan transfer ini (jejak audit ringan, bebas teks, tidak ber-FK ke tabel
	 * user), terpisah dari mekanisme envers ({@code @Audited}). Relevan untuk audit trail mengingat
	 * gerbang akses fitur ini SENGAJA dibatasi supervisor/admin-only.
	 * @return identitas pelaku transfer, atau {@code null} bila tidak diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/** @param oleh identitas pelaku transfer ini. */
	public void setOleh(String oleh) {
		this.oleh = oleh;
	}

	/**
	 * Stempel waktu terakhir baris ini diubah, diisi otomatis oleh {@link #onUpdate()} pada tiap
	 * {@code UPDATE}. Lihat javadoc field {@link #tanggal_dirubah} untuk detail perbedaannya dari
	 * mekanisme envers.
	 * @return waktu terakhir diubah.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @param tanggal_dirubah waktu terakhir diubah. Normalnya TIDAK diisi manual oleh kode aplikasi --
	 *                        dikelola otomatis oleh {@link #onUpdate()}; setter ini ada untuk kebutuhan
	 *                        Hibernate (property accessor) dan skenario migrasi/backfill data historis.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Penanda jurnal. Jurnal mutasi antar outlet: hanya bila akun persediaan kedua outlet berbeda. Diisi saat baris ini diposting ke buku besar; dipakai
	 * sebagai kunci anti-posting-ganda dan jejak balik dari jurnal ke dokumen sumbernya.
	 * Kolomnya dibuat otomatis oleh Hibernate.
	 */
	private ais.database.model.akunting.PostingHistory postingHistory;

	/**
	 * Jejak posting jurnal untuk transfer ini -- HANYA relevan bila akun persediaan kedua outlet
	 * berbeda (dua outlet dengan akun persediaan yang SAMA tidak memerlukan jurnal, karena transfer
	 * antar mereka tidak mengubah saldo akun apa pun secara akunting). {@code null} berarti baris ini
	 * BELUM diposting -- baik karena belum diproses proses posting, MAUPUN karena kedua outlet memang
	 * berbagi akun persediaan yang sama dan transfer ini secara desain tidak akan pernah menghasilkan
	 * jurnal. Kedua kemungkinan ini TIDAK bisa dibedakan hanya dari nilai {@code null} pada field ini --
	 * pemanggil yang perlu tahu mana yang berlaku harus memeriksa apakah akun persediaan kedua toko
	 * sama, bukan mengandalkan field ini semata. Relasi {@code LAZY}.
	 * @return jejak posting jurnal, atau {@code null} bila belum diposting (atau memang tidak perlu diposting).
	 */
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "posting_history", nullable = true)
	public ais.database.model.akunting.PostingHistory getPostingHistory() {
		postingHistory = check(postingHistory);
		return postingHistory;
	}

	/**
	 * @param postingHistory jejak posting jurnal transfer ini. Diisi oleh proses posting akunting, bukan
	 *                        oleh alur pencatatan transfer itu sendiri -- pemisahan tanggung jawab yang
	 *                        sama dipakai di seluruh model finansial modul ini (lihat {@link
	 *                        StokOpname#getPostingHistory()}).
	 */
	public void setPostingHistory(ais.database.model.akunting.PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

}
