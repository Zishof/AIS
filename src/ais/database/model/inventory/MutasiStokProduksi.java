package ais.database.model.inventory;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

/**
 * Ledger pergerakan stok dari dokumen PRODUKSI (Fase 0 dok. 49) — suku ke-9
 * rumus stok {@code StokKantinUtil}.
 *
 * <p>Satu baris = efek stok SATU baris dokumen produksi pada SATU arah. Baris
 * {@code FORWARD} ditulis saat dokumen ISSUE/RETURN/OUTPUT/WASTE mencapai
 * POSTED; baris {@code REVERSE} ditulis saat dokumen di-REVERSED — ledger tidak
 * pernah dihapus (ADR kontrak data terpadu: koreksi lewat movement lawan, bukan
 * menghapus historis; pola yang sama dengan {@link DistribusiPostingStok}).</p>
 *
 * <p>Idempoten dua lapis: constraint unik {@code (dokumen_id, baris_id, arah)}
 * menjaga di tingkat baris, dan {@code kunci_idempoten} berformat fondasi
 * Fase 9 ({@code PRODUCTION:<dokumen>:<jenis>:<baris>:<arah>}) menjaga lintas
 * retry. Penulisnya memeriksa-lalu-melewati sebelum menulis, jadi memproses
 * ulang transisi status tidak menggandakan pergerakan.</p>
 *
 * <p>SENGAJA tabel sendiri, bukan menumpang {@code pemakaian_bahan_baku}
 * (konsumsi resep saat JUAL — dipakai laporan HPP) maupun
 * {@code mutasi_stok_toko} (transfer ANTAR toko — produksi masuk/keluar satu
 * toko). Menumpang salah satunya mencemari laporan pembacanya; lihat dok. 49
 * §3 Fase 0 dan JavaDoc {@code BahanBakuUtil}.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "koperasi", name = "mutasi_stok_produksi", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "dokumen_id", "baris_id", "arah" }),
		@UniqueConstraint(columnNames = { "kunci_idempoten" }) })
public class MutasiStokProduksi implements Serializable {
	private static final long serialVersionUID = 1L;

	/** Arah maju: baris ditulis saat dokumen produksi (ISSUE/RETURN/OUTPUT/WASTE) mencapai status POSTED -- efek stok pertama kali diterapkan. */
	public static final String ARAH_FORWARD = "FORWARD";
	/** Arah balik: baris ditulis saat dokumen produksi yang sudah POSTED di-REVERSED -- koreksi lewat movement lawan yang menetralkan efek FORWARD, BUKAN dengan menghapus baris FORWARD (ledger tidak pernah dihapus, lihat javadoc kelas). */
	public static final String ARAH_REVERSE = "REVERSE";

	/** Primary key baris ledger. Digenerasi database ({@code IDENTITY}); {@code null} sebelum baris pertama kali disimpan. */
	private Long id;
	/** Id dokumen produksi sumber (mis. {@code ProduksiDokumen.id}) yang memicu baris ledger ini. Bersama {@link #barisId} dan {@link #arah}, membentuk kunci unik {@code (dokumen_id, baris_id, arah)} -- lihat javadoc kelas soal idempotensi dua lapis. */
	private Long dokumenId;
	/** Id baris dokumen produksi sumber (mis. {@code ProduksiDokumenBaris.id}) -- satu dokumen produksi bisa punya banyak baris, tiap baris menghasilkan satu (atau dua, bila di-reverse) baris ledger di sini. */
	private Long barisId;
	/** Arah efek stok baris ini: {@link #ARAH_FORWARD} atau {@link #ARAH_REVERSE}. Bagian dari kunci unik {@code (dokumen_id, baris_id, arah)} yang mencegah baris FORWARD/REVERSE ganda untuk kombinasi dokumen+baris yang sama. */
	private String arah;
	/** Jenis dokumen produksi sumber (mis. ISSUE/RETURN/OUTPUT/WASTE) yang memicu baris ledger ini -- ikut membentuk komponen {@code <jenis>} pada format {@link #kunciIdempoten}. */
	private String jenis;
	/** Id toko tempat stok bergerak. Bertipe {@code Long} mentah (bukan relasi {@code @ManyToOne} ke {@link Toko}) -- kelas ini SENGAJA menyimpan id murni tanpa join Hibernate, konsisten dengan gaya ledger ringan yang dioptimalkan untuk penulisan cepat & idempoten, bukan untuk navigasi objek graph seperti entity lain di paket ini. */
	private Long toko;
	/** Id {@code koperasi.produk} yang stoknya bergerak (= {@code ProduksiDokumenBaris.itemId}). */
	private Long produk;
	/** Kuantitas yang MASUK ke stok produk pada baris ini (mis. hasil OUTPUT produksi). Default {@link BigDecimal#ZERO}, bukan {@code null} -- berbeda dari pola {@code Double} bernilai {@code null}-dinormalisasi-jadi-nol yang dipakai entity lain di paket ini (mis. {@link StokOpname#getStokFisik()}); di sini nol adalah nilai default yang benar-benar tersimpan, bukan hasil normalisasi getter. */
	private BigDecimal qtyMasuk = BigDecimal.ZERO;
	/** Kuantitas yang KELUAR dari stok produk pada baris ini (mis. konsumsi ISSUE atau pemakaian WASTE). Default {@link BigDecimal#ZERO}. Satu baris secara praktik lazimnya hanya mengisi salah satu dari {@link #qtyMasuk}/{@link #qtyKeluar} (arah tunggal per baris), bukan keduanya sekaligus -- tapi model ini TIDAK memvalidasi/menegakkan eksklusivitas itu; validasi tersebut, bila ada, berada di kode penulis ({@code ProduksiApiHelper}). */
	private BigDecimal qtyKeluar = BigDecimal.ZERO;
	/** Kunci idempotensi lintas-retry berformat {@code PRODUCTION:<dokumen>:<jenis>:<baris>:<arah>} (fondasi Fase 9) -- lapisan kedua penjaga anti-duplikasi di ATAS constraint unik {@code (dokumen_id, baris_id, arah)}, memastikan retry pemrosesan transisi status dokumen tidak menggandakan baris ledger meski dipanggil berkali-kali. Unik di level database ({@code @UniqueConstraint}). */
	private String kunciIdempoten;
	/** Catatan bebas teks untuk baris ledger ini, opsional, dibatasi 255 karakter oleh kolom. */
	private String keterangan;
	/** Userid/nama yang memicu penulisan baris ledger ini (jejak audit ringan, bebas teks, tidak ber-FK), dibatasi 100 karakter oleh kolom. Kelas ini TIDAK ber-{@code @Audited} (berbeda dari {@link StokOpname}/{@link MutasiStokToko}/{@link SesiStokOpname}/{@link AmbangStokGudang} yang semua envers-audited) -- riwayat perubahan sepenuhnya bergantung pada sifat append-only ledger ini sendiri (baris tidak pernah di-{@code UPDATE}/dihapus, hanya ditambah), bukan pada snapshot versi envers. */
	private String oleh;
	/** Waktu baris ledger ini dicatat. Default waktu konstruksi objek Java ({@code new Date()}) -- BUKAN nilai lazy yang dihitung ulang tiap getter dipanggil seperti pola {@code null}-check pada entity lain di paket ini (mis. {@link StokOpname#getWaktuOpname()}); di sini nilai dibekukan sekali saat instance dibuat. */
	private Date waktu = new Date();

	/**
	 * Primary key baris ledger ini. Digenerasi database via strategi {@code IDENTITY}; {@code null}
	 * pada objek yang belum pernah di-{@code save}.
	 * @return id baris, atau {@code null} jika belum tersimpan.
	 */
	@Id @GeneratedValue(strategy = IDENTITY) @Column(name = "id", unique = true, nullable = false)
	public Long getId() { return id; }
	/** @param value id baris. Normalnya TIDAK diisi manual -- dibiarkan Hibernate mengisinya via {@code IDENTITY} saat insert. */
	public void setId(Long value) { id = value; }

	/**
	 * Id dokumen produksi sumber yang memicu baris ledger ini. Wajib diisi; bagian dari kunci unik
	 * {@code (dokumen_id, baris_id, arah)}.
	 * @return id dokumen produksi sumber.
	 */
	@Column(name = "dokumen_id", nullable = false) public Long getDokumenId() { return dokumenId; }
	/** @param value id dokumen produksi sumber. */
	public void setDokumenId(Long value) { dokumenId = value; }

	/**
	 * Id baris dokumen produksi sumber yang memicu baris ledger ini. Wajib diisi; bagian dari kunci
	 * unik {@code (dokumen_id, baris_id, arah)}.
	 * @return id baris dokumen produksi sumber.
	 */
	@Column(name = "baris_id", nullable = false) public Long getBarisId() { return barisId; }
	/** @param value id baris dokumen produksi sumber. */
	public void setBarisId(Long value) { barisId = value; }

	/**
	 * Arah efek stok baris ini: {@link #ARAH_FORWARD} atau {@link #ARAH_REVERSE}. Wajib diisi, dibatasi
	 * 10 karakter oleh kolom; bagian dari kunci unik {@code (dokumen_id, baris_id, arah)} yang menjadi
	 * lapisan pertama penjaga idempotensi (lihat javadoc kelas).
	 * @return arah baris ledger ini, salah satu dari {@link #ARAH_FORWARD}/{@link #ARAH_REVERSE}.
	 */
	@Column(name = "arah", nullable = false, length = 10) public String getArah() { return arah; }
	/** @param value arah baris ledger ini; TIDAK divalidasi terhadap konstanta {@link #ARAH_FORWARD}/{@link #ARAH_REVERSE} pada level setter -- pemanggil bertanggung jawab memakai salah satu konstanta tersebut. */
	public void setArah(String value) { arah = value; }

	/**
	 * Jenis dokumen produksi sumber (mis. ISSUE/RETURN/OUTPUT/WASTE) yang memicu baris ledger ini.
	 * Wajib diisi, dibatasi 20 karakter oleh kolom; ikut membentuk komponen {@code <jenis>} pada format
	 * {@link #getKunciIdempoten()}.
	 * @return jenis dokumen produksi sumber.
	 */
	@Column(name = "jenis", nullable = false, length = 20) public String getJenis() { return jenis; }
	/** @param value jenis dokumen produksi sumber. */
	public void setJenis(String value) { jenis = value; }

	/**
	 * Id toko tempat stok bergerak. Wajib diisi. Disimpan sebagai {@code Long} mentah, bukan relasi
	 * {@code @ManyToOne} ke {@link Toko} -- pemanggil yang butuh objek {@link Toko} penuh harus memuatnya
	 * sendiri berdasarkan id ini (mis. {@code session.get(Toko.class, getToko())}), kelas ini tidak
	 * menyediakan navigasi relasi otomatis seperti entity lain di paket ini.
	 * @return id toko tempat stok bergerak.
	 */
	@Column(name = "toko", nullable = false) public Long getToko() { return toko; }
	/** @param value id toko tempat stok bergerak. */
	public void setToko(Long value) { toko = value; }

	/** Id {@code koperasi.produk} yang stoknya bergerak (= {@code ProduksiDokumenBaris.itemId}). */
	@Column(name = "produk", nullable = false) public Long getProduk() { return produk; } public void setProduk(Long value) { produk = value; }

	/**
	 * Kuantitas yang MASUK ke stok {@link #getProduk()} pada baris ini (mis. hasil OUTPUT produksi),
	 * presisi {@code precision=19, scale=4}. Wajib diisi ({@code nullable=false}), default {@link
	 * BigDecimal#ZERO}.
	 *
	 * <p><b>Penjaga keseimbangan ledger produksi (audit domain rawan barang hilang/duplikasi tanpa
	 * jejak).</b> Berbeda dari {@link MutasiStokToko} (transfer DUA arah dalam SATU baris, satu produk
	 * berkurang dan produk lain bertambah sekaligus), kelas ini mencatat efek SATU produk pada SATU
	 * toko per baris -- pasangan {@link #qtyMasuk}/{@link #qtyKeluar} dalam baris yang SAMA merupakan
	 * dua sisi dari efek bersih terhadap SATU stok yang sama, bukan dua sisi transfer antar dua entitas
	 * berbeda. Keseimbangan yang perlu dijaga di sini BUKAN "keluar dari A = masuk ke B" seperti pada
	 * {@code MutasiStokToko}, melainkan "efek FORWARD dinetralkan tepat oleh efek REVERSE": penelusuran
	 * kode penulis ({@code ProduksiApiHelper}) mengonfirmasi baris REVERSE dibuat dengan menyalin
	 * {@code dokumenId}/{@code barisId} dari baris FORWARD pasangannya sebelum memproses pembalikan --
	 * desain ini bergantung pada penulis ledger menuliskan qtyMasuk/qtyKeluar REVERSE sebagai NILAI
	 * TERTUKAR (mirror) dari baris FORWARD-nya (bukan negasi pada kolom yang sama, karena kedua kolom
	 * ini {@code nullable=false} dan didefault {@code ZERO}, bukan bertipe signed tunggal) agar
	 * SUM(qtyMasuk) - SUM(qtyKeluar) lintas FORWARD+REVERSE untuk satu {@code (dokumenId, barisId)}
	 * kembali ke nol -- efek bersih pembatalan penuh.</p>
	 *
	 * <p><b>Model ini sendiri TIDAK menegakkan simetri FORWARD/REVERSE itu.</b> Tidak ada constraint
	 * database maupun validasi getter/setter pada kelas ini yang memeriksa bahwa baris REVERSE untuk
	 * {@code (dokumenId, barisId)} tertentu benar-benar memiliki {@code qtyMasuk}/{@code qtyKeluar} yang
	 * sama persis (tertukar) dengan baris FORWARD pasangannya -- penjaga keseimbangan SEPENUHNYA berada
	 * di disiplin kode penulis {@code ProduksiApiHelper}, bukan di level data model/database. Bila kode
	 * penulis di masa depan menuliskan baris REVERSE dengan qty yang salah (mis. hasil kalkulasi ulang
	 * yang berbeda dari baris FORWARD aslinya, bukan salinan langsung), ledger ini akan menyimpan
	 * ketidakseimbangan permanen TANPA terdeteksi oleh constraint apa pun -- karena ledger tidak pernah
	 * dihapus/diubah (append-only by design), kesalahan semacam itu juga tidak bisa diperbaiki dengan
	 * mengedit baris yang salah, hanya bisa dikoreksi dengan menambah baris koreksi baru. Ini BUKAN
	 * temuan kerentanan baru yang genuinely berbeda dari pola yang sudah diketahui (soft-check di
	 * level aplikasi, bukan hard-guard di level data) -- dicatat di sini sebagai referensi audit, bukan
	 * sebagai dasar eskalasi task terpisah, karena constraint unik {@code (dokumen_id, baris_id, arah)}
	 * dan {@code kunci_idempoten} SUDAH secara efektif mencegah kelas kesalahan yang paling umum (baris
	 * ganda dari retry), sementara simetri nilai qty pada baris REVERSE adalah kelas kesalahan yang
	 * lebih sempit (bug logika penulis, bukan race condition/duplikasi).</p>
	 *
	 * @return kuantitas masuk pada baris ini, tidak pernah {@code null} (default {@link BigDecimal#ZERO}).
	 */
	@Column(name = "qty_masuk", nullable = false, precision = 19, scale = 4) public BigDecimal getQtyMasuk() { return qtyMasuk; }
	/** @param value kuantitas masuk pada baris ini. */
	public void setQtyMasuk(BigDecimal value) { qtyMasuk = value; }

	/**
	 * Kuantitas yang KELUAR dari stok {@link #getProduk()} pada baris ini (mis. konsumsi ISSUE atau
	 * pemakaian WASTE), presisi {@code precision=19, scale=4}. Wajib diisi, default {@link
	 * BigDecimal#ZERO}. Lihat javadoc {@link #getQtyMasuk()} untuk pembahasan lengkap penjaga
	 * keseimbangan FORWARD/REVERSE yang berlaku sama untuk field ini.
	 * @return kuantitas keluar pada baris ini, tidak pernah {@code null} (default {@link BigDecimal#ZERO}).
	 */
	@Column(name = "qty_keluar", nullable = false, precision = 19, scale = 4) public BigDecimal getQtyKeluar() { return qtyKeluar; }
	/** @param value kuantitas keluar pada baris ini. */
	public void setQtyKeluar(BigDecimal value) { qtyKeluar = value; }

	/**
	 * Kunci idempotensi lintas-retry berformat {@code PRODUCTION:<dokumen>:<jenis>:<baris>:<arah>}.
	 * Wajib diisi, unik di level database ({@code @UniqueConstraint}), dibatasi 120 karakter. Penulis
	 * ledger ({@code ProduksiApiHelper}) memeriksa keberadaan kunci ini (atau kombinasi {@code
	 * dokumen_id}/{@code baris_id}/{@code arah}) SEBELUM menulis baris baru ("periksa-lalu-melewati") --
	 * memproses ulang transisi status dokumen produksi yang sama tidak menggandakan baris ledger.
	 * @return kunci idempoten baris ini.
	 */
	@Column(name = "kunci_idempoten", nullable = false, length = 120) public String getKunciIdempoten() { return kunciIdempoten; }
	/** @param value kunci idempoten baris ini; harus unik lintas seluruh tabel ({@code @UniqueConstraint}), pelanggaran akan gagal saat flush/commit. */
	public void setKunciIdempoten(String value) { kunciIdempoten = value; }

	/**
	 * Catatan bebas teks untuk baris ledger ini, opsional, dibatasi 255 karakter.
	 * @return catatan/keterangan, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "keterangan", length = 255) public String getKeterangan() { return keterangan; }
	/** @param value catatan/keterangan baris ini. */
	public void setKeterangan(String value) { keterangan = value; }

	/**
	 * Userid/nama yang memicu penulisan baris ledger ini, opsional, dibatasi 100 karakter. Jejak audit
	 * ringan -- lihat javadoc field {@link #oleh} soal ketiadaan {@code @Audited} pada kelas ini.
	 * @return identitas pemicu baris ini, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "oleh", length = 100) public String getOleh() { return oleh; }
	/** @param value identitas pemicu baris ledger ini. */
	public void setOleh(String value) { oleh = value; }

	/**
	 * Waktu baris ledger ini dicatat. Wajib diisi ({@code nullable=false}), default waktu konstruksi
	 * objek Java ({@code new Date()}) -- dibekukan sekali saat instance dibuat, bukan dihitung ulang
	 * setiap getter dipanggil.
	 * @return waktu baris ini dicatat.
	 */
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "waktu", nullable = false)
	public Date getWaktu() { return waktu; }
	/** @param value waktu baris ledger ini dicatat. */
	public void setWaktu(Date value) { waktu = value; }
}
