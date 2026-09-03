package ais.database.model.koperasi;

// Bagian dari sub-modul Simpan Pinjam (USP) Koperasi — fitur Anggaran/Perencanaan Kas (RAPB).

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

/**
 * <h2>AnggaranKasKoperasi — Rencana Anggaran Kas (RAPB) Koperasi per Tahun Buku</h2>
 *
 * <p>
 * Entity ini menyimpan <b>rencana anggaran kas</b> koperasi untuk satu tahun buku: perkiraan seluruh
 * <b>penerimaan kas</b> (uang masuk) dan <b>pengeluaran kas</b> (uang keluar) beserta saldo kas awal
 * tahun. Sesuai SOM USPK dan praktik tata kelola koperasi, pengurus wajib menyusun Rencana Anggaran
 * Pendapatan dan Belanja (RAPB) yang disahkan Rapat Anggota; anggaran kas adalah bagian arus kas dari
 * rencana tersebut. Dengan menyimpannya di sini, koperasi dapat <b>membandingkan rencana dengan
 * realisasi</b> sepanjang tahun, mengetahui apakah penerimaan/pengeluaran sesuai target, serta
 * memproyeksikan saldo kas akhir agar likuiditas tetap terjaga.
 * </p>
 *
 * <h3>Mengapa rencana tahunan (bukan matriks bulanan)?</h3>
 * <p>
 * Anggaran disimpan sebagai angka tahunan per kategori — bukan matriks 12&nbsp;bulan &times; banyak pos —
 * agar mudah diisi pengurus koperasi kecil-menengah dan tidak memberatkan. Realisasi tetap dihitung
 * dari data transaksi nyata (setoran simpanan, angsuran pokok, jasa pinjaman, penyaluran pinjaman)
 * sehingga perbandingan tetap bermakna. Bila kelak diperlukan rincian bulanan, dapat ditambahkan
 * entity anak tanpa mengubah struktur ini.
 * </p>
 *
 * <h3>Kategori yang direncanakan</h3>
 * <ul>
 * <li><b>Penerimaan</b> — {@link #getRencanaSimpanan()} (setoran simpanan anggota),
 * {@link #getRencanaAngsuranPokok()} (pengembalian pokok pinjaman), {@link #getRencanaJasaPinjaman()}
 * (jasa/bunga pinjaman), {@link #getRencanaPenerimaanLain()} (penerimaan lain-lain).</li>
 * <li><b>Pengeluaran</b> — {@link #getRencanaPenyaluran()} (pinjaman yang disalurkan),
 * {@link #getRencanaBiayaOperasional()} (biaya operasional), {@link #getRencanaPengeluaranLain()}
 * (pengeluaran lain-lain).</li>
 * </ul>
 *
 * <p>
 * Beberapa metode {@code @Transient} menyediakan turunan yang sering dipakai tampilan/laporan tanpa
 * perlu disimpan: {@link #getTotalPenerimaanRencana()}, {@link #getTotalPengeluaranRencana()},
 * {@link #getSurplusRencana()} (selisih penerimaan dan pengeluaran), dan {@link #getSaldoAkhirRencana()}
 * (saldo kas awal ditambah surplus). Semuanya dihitung ulang dari komponen sehingga selalu konsisten.
 * </p>
 *
 * <h3>Catatan desain</h3>
 * <p>
 * Mengikuti pola rumah AIS agar seragam dan mudah dipelihara: kunci {@code IDENTITY}, relasi
 * {@link Koperasi} lazy dengan {@code check(...)}, hook audit {@code @PreUpdate}, {@code @Audited}
 * (anggaran adalah keputusan penting yang harus dapat ditelusuri), seluruh getter numerik aman-null
 * (mengembalikan 0.0 bila belum diisi), serta kompatibel Java 1.7. Terdaftar di
 * {@code hibernate.cfg.xml} sehingga {@code hbm2ddl=update} membuat tabel
 * <code>koperasi.anggaran_kas</code> secara otomatis. Kombinasi koperasi+tahun sebaiknya unik dan
 * dijaga di lapisan Action. Entity ini tidak menyentuh basis data secara langsung, hemat memori, dan
 * tidak mengubah perilaku entity lain.
 * </p>
 *
 * @see PembagianShu
 * @see ais.action.master.koperasi.AnggaranKasKoperasiAction
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "anggaran_kas")
public class AnggaranKasKoperasi extends ais.database.model.GeneralValueObject {

	private static final long serialVersionUID = 6620100014412771001L;

	/** Primary key, IDENTITY dari kolom {@code id}. */
	private Long id;
	/** Nama pengguna (username) pembuat/pengubah terakhir baris ini, untuk audit ringan. */
	private String oleh;
	/** Id pengguna pembuat/pengubah terakhir baris ini, pasangan {@link #oleh}. */
	private String olehId;

	/** Koperasi pemilik rencana anggaran kas tahun buku ini. */
	private Koperasi koperasi;
	/** Tahun buku yang direncanakan anggaran kasnya. */
	private Integer tahun = 0;
	/** Saldo kas yang diperkirakan/ditetapkan pada awal tahun buku (rupiah); dasar {@link #getSaldoAkhirRencana()}. */
	private Double saldoAwalKas = 0.0;

	/** Rencana penerimaan kas dari setoran simpanan anggota (rupiah) sepanjang tahun buku. */
	private Double rencanaSimpanan = 0.0;
	/** Rencana penerimaan kas dari pengembalian pokok pinjaman anggota (rupiah) sepanjang tahun buku. */
	private Double rencanaAngsuranPokok = 0.0;
	/** Rencana penerimaan kas dari jasa/bunga pinjaman anggota (rupiah) sepanjang tahun buku. */
	private Double rencanaJasaPinjaman = 0.0;
	/** Rencana penerimaan kas lain-lain di luar tiga kategori di atas (rupiah) sepanjang tahun buku. */
	private Double rencanaPenerimaanLain = 0.0;

	/** Rencana pengeluaran kas untuk penyaluran pinjaman baru ke anggota (rupiah) sepanjang tahun buku. */
	private Double rencanaPenyaluran = 0.0;
	/** Rencana pengeluaran kas untuk biaya operasional koperasi (rupiah) sepanjang tahun buku. */
	private Double rencanaBiayaOperasional = 0.0;
	/** Rencana pengeluaran kas lain-lain di luar dua kategori di atas (rupiah) sepanjang tahun buku. */
	private Double rencanaPengeluaranLain = 0.0;

	/** Catatan bebas mengenai rencana anggaran kas tahun buku ini. */
	private String keterangan;
	/** Penanda baris masih berlaku untuk ditampilkan/dipilih di layar; {@code true} secara default. */
	private Boolean aktif = true;

	/** Konstruktor kosong, dipakai Hibernate dan saat membangun rencana anggaran kas baru sebelum diisi. */
	public AnggaranKasKoperasi() {
	}

	/**
	 * Konstruktor pintasan untuk merujuk sebuah rencana anggaran kas yang sudah ada hanya lewat id-nya.
	 *
	 * @param id primary key rencana anggaran kas yang sudah ada
	 */
	public AnggaranKasKoperasi(Long id) {
		this.id = id;
	}

	/** @return primary key rencana anggaran kas ini, atau {@code null} bila belum tersimpan. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id primary key; kolom {@code insertable = false} sehingga id sesungguhnya berasal dari IDENTITY database. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return id pengguna pembuat/pengubah terakhir baris ini, atau {@code null} bila belum pernah di-set. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Set id pengguna pembuat/pengubah. Nilai kosong/hanya-spasi diabaikan (tidak menimpa nilai
	 * lama) agar audit tidak pernah kehilangan jejak pengguna karena panggilan kosong yang tidak
	 * disengaja.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Set nama pengguna pembuat/pengubah. Sama seperti {@link #setOlehId(String)}, nilai
	 * kosong/hanya-spasi diabaikan.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/** @return nama pengguna pembuat/pengubah terakhir baris ini. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook Hibernate yang dipanggil otomatis sebelum setiap {@code UPDATE}. Mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} untuk memperbarui
	 * {@link #tanggal_dirubah} (dan field audit sejenis) tanpa perlu campur tangan kode pemanggil.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Waktu terakhir baris ini diubah; default saat objek dibuat, diperbarui oleh {@link #onUpdate()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah waktu perubahan terakhir yang hendak diset secara manual. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu terakhir baris ini diubah. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return koperasi pemilik rencana anggaran kas ini, dimuat lazy lewat {@code check(...)}.
	 *         Catatan: relasi ini {@code nullable = true}; penyaringan menurut koperasi berjalan
	 *         (bila ada) dilakukan di lapisan Action/query pemanggil, bukan dipaksa di sini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "koperasi", nullable = true)
	public Koperasi getKoperasi() {
		koperasi = check(koperasi);
		return koperasi;
	}

	/**
	 * Set koperasi pemilik. Ditolak diam-diam (field tetap {@code null}) bila {@code koperasi}
	 * bernilai {@code null} atau belum memiliki id (belum tersimpan) — mencegah rencana anggaran
	 * kas tertaut ke koperasi yang belum valid di database.
	 *
	 * @param koperasi koperasi pemilik; diabaikan (diset {@code null}) bila belum memiliki id
	 */
	public void setKoperasi(Koperasi koperasi) {
		this.koperasi = koperasi == null || koperasi.getId() == null ? null : koperasi;
	}

	/** @return tahun buku yang direncanakan anggaran kasnya; tidak pernah {@code null} ({@code 0} sebagai fallback). */
	@Column(name = "tahun")
	public Integer getTahun() {
		return tahun == null ? 0 : tahun;
	}

	/** @param tahun tahun buku yang direncanakan anggaran kasnya. Kombinasi koperasi+tahun sebaiknya unik (dijaga di Action). */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * @return saldo kas yang diperkirakan/ditetapkan pada awal tahun buku (rupiah); tidak pernah
	 *         {@code null} ({@code 0.0} sebagai fallback). Dasar {@link #getSaldoAkhirRencana()}.
	 *         Nilai ini input manual pengurus (biasanya mengacu saldo kas akhir tahun sebelumnya),
	 *         tidak diambil otomatis dari buku besar/akunting.
	 */
	@Column(name = "saldo_awal_kas")
	public Double getSaldoAwalKas() {
		return saldoAwalKas == null ? 0.0 : saldoAwalKas;
	}

	/** @param saldoAwalKas saldo kas yang diperkirakan/ditetapkan pada awal tahun buku (rupiah). */
	public void setSaldoAwalKas(Double saldoAwalKas) {
		this.saldoAwalKas = saldoAwalKas;
	}

	/**
	 * @return rencana penerimaan kas dari setoran simpanan anggota (rupiah) sepanjang tahun buku;
	 *         tidak pernah {@code null} ({@code 0.0} sebagai fallback). Komponen
	 *         {@link #getTotalPenerimaanRencana()}; realisasinya dihitung terpisah oleh
	 *         {@code AnggaranKasKoperasiAction} dari data {@code TransaksiKoperasi} nyata bertipe
	 *         simpanan pada tahun yang sama.
	 */
	@Column(name = "rencana_simpanan")
	public Double getRencanaSimpanan() {
		return rencanaSimpanan == null ? 0.0 : rencanaSimpanan;
	}

	/** @param rencanaSimpanan rencana penerimaan kas dari setoran simpanan anggota (rupiah) sepanjang tahun buku. */
	public void setRencanaSimpanan(Double rencanaSimpanan) {
		this.rencanaSimpanan = rencanaSimpanan;
	}

	/**
	 * @return rencana penerimaan kas dari pengembalian pokok pinjaman anggota (rupiah) sepanjang
	 *         tahun buku; tidak pernah {@code null} ({@code 0.0} sebagai fallback). Komponen
	 *         {@link #getTotalPenerimaanRencana()}.
	 */
	@Column(name = "rencana_angsuran_pokok")
	public Double getRencanaAngsuranPokok() {
		return rencanaAngsuranPokok == null ? 0.0 : rencanaAngsuranPokok;
	}

	/** @param rencanaAngsuranPokok rencana penerimaan kas dari pengembalian pokok pinjaman anggota (rupiah) sepanjang tahun buku. */
	public void setRencanaAngsuranPokok(Double rencanaAngsuranPokok) {
		this.rencanaAngsuranPokok = rencanaAngsuranPokok;
	}

	/**
	 * @return rencana penerimaan kas dari jasa/bunga pinjaman anggota (rupiah) sepanjang tahun
	 *         buku; tidak pernah {@code null} ({@code 0.0} sebagai fallback). Komponen
	 *         {@link #getTotalPenerimaanRencana()}.
	 */
	@Column(name = "rencana_jasa_pinjaman")
	public Double getRencanaJasaPinjaman() {
		return rencanaJasaPinjaman == null ? 0.0 : rencanaJasaPinjaman;
	}

	/** @param rencanaJasaPinjaman rencana penerimaan kas dari jasa/bunga pinjaman anggota (rupiah) sepanjang tahun buku. */
	public void setRencanaJasaPinjaman(Double rencanaJasaPinjaman) {
		this.rencanaJasaPinjaman = rencanaJasaPinjaman;
	}

	/**
	 * @return rencana penerimaan kas lain-lain (rupiah) sepanjang tahun buku, di luar simpanan,
	 *         angsuran pokok, dan jasa pinjaman; tidak pernah {@code null} ({@code 0.0} sebagai
	 *         fallback). Komponen {@link #getTotalPenerimaanRencana()}; tidak memiliki padanan
	 *         realisasi otomatis (kategori "lain-lain" tidak dapat ditelusuri ke satu jenis
	 *         transaksi tertentu).
	 */
	@Column(name = "rencana_penerimaan_lain")
	public Double getRencanaPenerimaanLain() {
		return rencanaPenerimaanLain == null ? 0.0 : rencanaPenerimaanLain;
	}

	/** @param rencanaPenerimaanLain rencana penerimaan kas lain-lain (rupiah) sepanjang tahun buku. */
	public void setRencanaPenerimaanLain(Double rencanaPenerimaanLain) {
		this.rencanaPenerimaanLain = rencanaPenerimaanLain;
	}

	/**
	 * @return rencana pengeluaran kas untuk penyaluran pinjaman baru ke anggota (rupiah) sepanjang
	 *         tahun buku; tidak pernah {@code null} ({@code 0.0} sebagai fallback). Komponen
	 *         {@link #getTotalPengeluaranRencana()}.
	 */
	@Column(name = "rencana_penyaluran")
	public Double getRencanaPenyaluran() {
		return rencanaPenyaluran == null ? 0.0 : rencanaPenyaluran;
	}

	/** @param rencanaPenyaluran rencana pengeluaran kas untuk penyaluran pinjaman baru ke anggota (rupiah) sepanjang tahun buku. */
	public void setRencanaPenyaluran(Double rencanaPenyaluran) {
		this.rencanaPenyaluran = rencanaPenyaluran;
	}

	/**
	 * @return rencana pengeluaran kas untuk biaya operasional koperasi (rupiah) sepanjang tahun
	 *         buku; tidak pernah {@code null} ({@code 0.0} sebagai fallback). Komponen
	 *         {@link #getTotalPengeluaranRencana()}.
	 */
	@Column(name = "rencana_biaya_operasional")
	public Double getRencanaBiayaOperasional() {
		return rencanaBiayaOperasional == null ? 0.0 : rencanaBiayaOperasional;
	}

	/** @param rencanaBiayaOperasional rencana pengeluaran kas untuk biaya operasional koperasi (rupiah) sepanjang tahun buku. */
	public void setRencanaBiayaOperasional(Double rencanaBiayaOperasional) {
		this.rencanaBiayaOperasional = rencanaBiayaOperasional;
	}

	/**
	 * @return rencana pengeluaran kas lain-lain (rupiah) sepanjang tahun buku, di luar penyaluran
	 *         pinjaman dan biaya operasional; tidak pernah {@code null} ({@code 0.0} sebagai
	 *         fallback). Komponen {@link #getTotalPengeluaranRencana()}.
	 */
	@Column(name = "rencana_pengeluaran_lain")
	public Double getRencanaPengeluaranLain() {
		return rencanaPengeluaranLain == null ? 0.0 : rencanaPengeluaranLain;
	}

	/** @param rencanaPengeluaranLain rencana pengeluaran kas lain-lain (rupiah) sepanjang tahun buku. */
	public void setRencanaPengeluaranLain(Double rencanaPengeluaranLain) {
		this.rencanaPengeluaranLain = rencanaPengeluaranLain;
	}

	/** @return catatan bebas mengenai rencana anggaran kas tahun buku ini, atau {@code null} bila belum diisi. */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return keterangan;
	}

	/** @param keterangan catatan bebas mengenai rencana anggaran kas tahun buku ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return penanda baris masih berlaku untuk ditampilkan/dipilih di layar; tidak pernah {@code null} ({@code true} sebagai fallback). */
	@Column(name = "aktif")
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif penanda baris masih berlaku untuk ditampilkan/dipilih di layar. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * <h3>Total seluruh penerimaan kas yang direncanakan (rupiah)</h3>
	 *
	 * <p>
	 * Menjumlahkan keempat kategori penerimaan: {@link #getRencanaSimpanan()},
	 * {@link #getRencanaAngsuranPokok()}, {@link #getRencanaJasaPinjaman()}, dan
	 * {@link #getRencanaPenerimaanLain()}. Method ini murni turunan ({@code @Transient}, tidak
	 * dipersist) dan dihitung ulang setiap kali dipanggil dari nilai keempat field tersebut saat
	 * itu, sehingga selalu konsisten dan tidak pernah menjadi basi terhadap perubahan salah satu
	 * komponennya.
	 * </p>
	 *
	 * <p>
	 * Angka yang dikembalikan adalah <b>rencana</b>, bukan realisasi — murni penjumlahan input
	 * manual yang diisi pengurus lewat layar {@code AnggaranKasKoperasiAction}. Angka realisasi yang
	 * sebanding (dihitung dari transaksi nyata: setoran simpanan, angsuran pokok, dan jasa pinjaman
	 * pada tahun {@link #getTahun()} yang sama) dihitung <b>terpisah</b> oleh
	 * {@code AnggaranKasKoperasiAction.hitungRealisasi(int)} — bukan oleh entity ini — dan
	 * ditampilkan berdampingan dengan nilai method ini pada fitur "Analisis Rencana vs Realisasi".
	 * Penerimaan lain-lain tidak memiliki padanan realisasi otomatis (kategori tersebut tidak dapat
	 * ditelusuri ke satu jenis transaksi tertentu), sehingga bagian realisasi untuk pos itu selalu
	 * ditampilkan nol dengan keterangan bahwa datanya perlu ditutup manual — ini bukan kekurangan
	 * pada method ini, melainkan keterbatasan sumber data realisasi yang tersedia.
	 * </p>
	 *
	 * @return total seluruh penerimaan kas yang direncanakan (rupiah); {@code 0.0} bila keempat
	 *         komponennya belum diisi
	 */
	@javax.persistence.Transient
	public double getTotalPenerimaanRencana() {
		return getRencanaSimpanan() + getRencanaAngsuranPokok() + getRencanaJasaPinjaman()
				+ getRencanaPenerimaanLain();
	}

	/**
	 * <h3>Total seluruh pengeluaran kas yang direncanakan (rupiah)</h3>
	 *
	 * <p>
	 * Menjumlahkan ketiga kategori pengeluaran: {@link #getRencanaPenyaluran()},
	 * {@link #getRencanaBiayaOperasional()}, dan {@link #getRencanaPengeluaranLain()}. Sama seperti
	 * {@link #getTotalPenerimaanRencana()}, method ini murni turunan ({@code @Transient}) yang
	 * selalu dihitung ulang dari ketiga field sumbernya, dan angka yang dikembalikan adalah
	 * <b>rencana</b> (input manual pengurus) — realisasinya (penyaluran pinjaman &amp; biaya
	 * operasional nyata pada tahun yang sama) dihitung terpisah oleh
	 * {@code AnggaranKasKoperasiAction.hitungRealisasi(int)}.
	 * </p>
	 *
	 * @return total seluruh pengeluaran kas yang direncanakan (rupiah); {@code 0.0} bila ketiga
	 *         komponennya belum diisi
	 */
	@javax.persistence.Transient
	public double getTotalPengeluaranRencana() {
		return getRencanaPenyaluran() + getRencanaBiayaOperasional() + getRencanaPengeluaranLain();
	}

	/**
	 * <h3>Surplus/defisit kas yang direncanakan</h3>
	 *
	 * <p>
	 * Rumus: <code>surplus = totalPenerimaanRencana − totalPengeluaranRencana</code>. Nilai positif
	 * berarti rencana penerimaan melebihi rencana pengeluaran (kas diperkirakan bertambah sepanjang
	 * tahun buku); nilai negatif berarti defisit (kas diperkirakan berkurang) — pengurus perlu
	 * meninjau ulang rencana bila defisitnya membahayakan likuiditas. Method ini murni turunan
	 * ({@code @Transient}), tidak dipersist, selalu dihitung ulang dari
	 * {@link #getTotalPenerimaanRencana()} dan {@link #getTotalPengeluaranRencana()} terkini.
	 * </p>
	 *
	 * @return surplus (positif) atau defisit (negatif) kas yang direncanakan, dalam rupiah
	 */
	@javax.persistence.Transient
	public double getSurplusRencana() {
		return getTotalPenerimaanRencana() - getTotalPengeluaranRencana();
	}

	/**
	 * <h3>Perkiraan saldo kas akhir tahun</h3>
	 *
	 * <p>
	 * Rumus: <code>saldoAkhirRencana = saldoAwalKas + surplusRencana</code> — proyeksi saldo kas
	 * pada akhir tahun buku bila seluruh rencana penerimaan dan pengeluaran terealisasi persis
	 * sesuai angka yang direncanakan. Method ini murni turunan ({@code @Transient}), tidak
	 * dipersist, selalu dihitung ulang dari {@link #getSaldoAwalKas()} dan
	 * {@link #getSurplusRencana()} terkini sehingga tidak pernah menjadi basi.
	 * </p>
	 *
	 * <p>
	 * <b>Yang perlu diperhatikan pemanggil:</b> ini murni proyeksi linear tahunan berbasis rencana —
	 * tidak memperhitungkan distribusi arus kas sepanjang tahun (mis. musim tertentu di mana
	 * pengeluaran lebih besar dari penerimaan meski totalnya seimbang di akhir tahun), sehingga
	 * saldo kas pada bulan-bulan tertentu di tengah tahun bisa jauh lebih rendah dari rata-rata
	 * meski proyeksi akhir tahun positif — ini konsekuensi wajar dari desain "rencana tahunan, bukan
	 * matriks bulanan" yang dijelaskan pada Javadoc kelas, bukan cacat pada method ini. Padanan
	 * realisasi untuk nilai ini (saldo akhir berdasarkan realisasi nyata) dihitung terpisah oleh
	 * {@code AnggaranKasKoperasiAction} pada fitur "Analisis Rencana vs Realisasi".
	 * </p>
	 *
	 * @return perkiraan saldo kas akhir tahun buku (rupiah) = saldo awal + surplus rencana
	 */
	@javax.persistence.Transient
	public double getSaldoAkhirRencana() {
		return getSaldoAwalKas() + getSurplusRencana();
	}

	/** @return representasi ringkas "Anggaran Kas tahun" untuk debug/log/tampilan sederhana. */
	@Override
	public String toString() {
		return "Anggaran Kas " + getTahun();
	}
}
