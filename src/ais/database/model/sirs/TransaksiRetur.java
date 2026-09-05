package ais.database.model.sirs;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.asset.Lokasi;

/**
 * Entitas <b>Transaksi Retur</b> pada schema {@code sirs} (tabel
 * {@code transaksi_retur}). Merepresentasikan pengembalian barang oleh PASIEN
 * atau pembeli atas sebuah {@link TransaksiMedis} yang sudah terjadi — retur
 * penjualan, bukan retur pembelian. Baris-baris itemnya ada di
 * {@link TransaksiReturDetail}.
 *
 * <h2>Berbeda jalur dari klaster pengadaan</h2>
 * <p>
 * Dokumen ini perlu dibedakan tegas dari {@link PenerimaanOrderKembali}, yang
 * meskipun namanya juga mengandung unsur "kembali" berada di jalur yang
 * berlawanan arah: {@link PenerimaanOrderKembali} mengembalikan barang KE
 * vendor pada jalur pembelian, sedangkan entitas ini menerima barang kembali
 * DARI pasien pada jalur penjualan. Keduanya sama-sama menambah atau mengurangi
 * stok, tetapi lawan transaksinya, dokumen dasarnya, dan konsekuensi uangnya
 * sama sekali berbeda.
 * </p>
 * <p>
 * Dokumen ini juga membawa dimensi yang tidak dimiliki dokumen inventaris
 * lain di klaster ini: dimensi KAS. Selain menambah kembali stok, retur
 * penjualan menimbulkan kewajiban mengembalikan uang kepada pasien, yang
 * tercermin pada {@link #getPembayaran()} dan {@link #getLunas()}. Karena itu
 * pula ia punya {@link #getShift()} dan {@link #getBagian()} — dimensi
 * operasional kasir yang tidak relevan bagi dokumen gudang.
 * </p>
 *
 * <h2>Catatan integritas</h2>
 * <p>
 * <b>Kode dokumen TIDAK unik.</b> Berbeda dari seluruh dokumen lain di klaster
 * ini ({@link PesananPembelian}, {@link PenerimaanOrder},
 * {@link KoreksiItemMedis}, {@link TransferItem} dan seterusnya) yang
 * memetakan {@code kode} dengan {@code @Column(nullable = false, unique =
 * true)}, {@link #getKode()} di sini sama sekali tanpa anotasi
 * {@code @Column} — sehingga tidak ada constraint {@code NOT NULL} maupun
 * {@code UNIQUE} atasnya. Dua dokumen retur dengan kode yang sama, atau tanpa
 * kode sama sekali, akan tersimpan tanpa keluhan.
 * </p>
 * <p>
 * <b>Tautan ke transaksi asal bersifat opsional.</b>
 * {@link #getTransaksi()} boleh kosong, sehingga retur dapat berdiri tanpa
 * transaksi penjualan yang mendasarinya — berbeda dari
 * {@link PenerimaanOrderKembali#getPenerimaanOrder()} yang WAJIB. Tanpa tautan
 * itu tidak ada nilai pembanding untuk membatasi kuantitas maupun nilai yang
 * diretur.
 * </p>
 * <p>
 * <b>Tidak ada jejak persetujuan sama sekali.</b> Berbeda dari seluruh dokumen
 * lain di klaster ini, entitas ini tidak punya {@code dibuatOleh},
 * {@code disetujuiOleh}, {@code tanggalPembuatan}, maupun
 * {@code tanggalPersetujuan}. Yang tersedia hanyalah dua flag Boolean —
 * {@link #getValidasi()} dan {@link #getLunas()} — yang menyimpan STATUS tanpa
 * menyimpan siapa yang menetapkannya dan kapan. Jejak audit dokumen ini karena
 * itu bersandar sepenuhnya pada field audit shadow
 * ({@link #getOleh()}/{@link #getOlehId()}) dan pada Hibernate Envers, bukan
 * pada kolom bisnis.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "transaksi_retur")
public class TransaksiRetur extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java untuk entitas ini, dipatok agar object
	 * yang sudah terserialisasi tetap kompatibel meski struktur kelas berubah.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private Long index;
	private String olehId;

	/**
	 * Mengambil ID pengguna yang terakhir mengubah dokumen retur ini. Field
	 * audit shadow yang diisi lewat {@link #setOlehId(String)}.
	 *
	 * <p>
	 * Karena entitas ini tidak punya kolom {@code dibuatOleh} maupun
	 * {@code disetujuiOleh}, field audit shadow inilah satu-satunya jejak
	 * pelaku yang tersimpan langsung pada barisnya.
	 * </p>
	 *
	 * @return ID pengguna terakhir yang mengubah, atau {@code null} jika belum
	 *         pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna yang mengubah dokumen retur ini. Nilai
	 * kosong/blank SENGAJA diabaikan (early return) agar field audit ini tidak
	 * pernah ditimpa jadi kosong — pola field audit shadow yang merupakan
	 * KEHARUSAN TEKNIS berulang di paket model AIS, bukan bug.
	 *
	 * @param olehId ID pengguna; nilai {@code null} atau string
	 *               kosong/whitespace-saja tidak akan mengubah nilai tersimpan.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	private String oleh;

	/**
	 * Representasi ringkas dokumen retur ini untuk tampilan combobox/listbox ZK
	 * dan log.
	 *
	 * <p>
	 * Perhatikan bahwa method ini memakai {@link #getKeterangan()} sebagai
	 * label, BUKAN {@link #getKode()} seperti dokumen-dokumen lain di klaster
	 * ini ({@link PesananPembelian}, {@link PenerimaanOrder},
	 * {@link KoreksiItemMedis}, {@link TransferItem}, dan seterusnya). Karena
	 * keterangan adalah teks bebas yang tidak wajib diisi, dokumen retur
	 * seringkali akan tampil sebagai teks kosong atau {@code null} pada daftar
	 * pilihan — dan dua dokumen berbeda dengan keterangan sama akan tampak
	 * identik di layar.
	 * </p>
	 *
	 * @return teks keterangan dokumen retur ini.
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Menetapkan nama pengguna yang mengubah dokumen retur ini. Nilai
	 * kosong/blank diabaikan agar field audit ini tidak pernah ditimpa kosong.
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau kosong/whitespace-saja
	 *             tidak akan mengubah nilai tersimpan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah dokumen retur ini.
	 *
	 * @return nama pengguna terakhir yang mengubah, atau {@code null} jika
	 *         belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate} yang memperbarui {@link #tanggal_dirubah}
	 * otomatis sebelum baris ini di-UPDATE, lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = new Date();

	/**
	 * Menetapkan timestamp perubahan terakhir dokumen ini secara manual.
	 *
	 * @param tanggal_dirubah timestamp perubahan baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil timestamp perubahan terakhir dokumen ini, diperbarui otomatis
	 * oleh {@link #onUpdate()} setiap kali baris di-UPDATE. Karena entitas ini
	 * tidak punya {@code tanggalPembuatan} maupun {@code tanggalPersetujuan},
	 * timestamp ini dan {@link #getTanggal()} adalah satu-satunya penanda waktu
	 * yang tersedia.
	 *
	 * @return timestamp perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	private String kode;
	private TransaksiMedis transaksi;
	private Date tanggal = new Date();
	private String keterangan;
	private Lokasi lokasi;
	private Shift shift;
	private Bagian bagian;

	private Pembayaran pembayaran;
	private Boolean lunas = false;

	private Boolean validasi = false;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public TransaksiRetur() {
	}

	/**
	 * Primary key dokumen retur ini, auto-increment (IDENTITY) dan diisi
	 * database.
	 *
	 * @return ID unik dokumen retur ini, atau {@code null} untuk baris yang
	 *         belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID dokumen retur ini.
	 *
	 * @param id ID dokumen retur.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil keterangan bebas dokumen retur ini. Karena entitas tidak punya
	 * kolom terstruktur untuk ALASAN retur, teks bebas inilah satu-satunya
	 * tempat sebab pengembalian dicatat. Nilainya juga dipakai
	 * {@link #toString()} sebagai label tampilan.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas dokumen retur ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan transaksi medis yang menjadi dasar retur ini.
	 *
	 * @param transaksi transaksi medis asal.
	 */
	public void setTransaksi(TransaksiMedis transaksi) {
		this.transaksi = transaksi;
	}

	/**
	 * Mengambil {@link TransaksiMedis} yang menjadi dasar retur ini — transaksi
	 * penjualan yang barangnya dikembalikan pasien.
	 *
	 * <p>
	 * Relasi ini OPSIONAL ({@code nullable = true}), berbeda dari
	 * {@link PenerimaanOrderKembali#getPenerimaanOrder()} pada jalur pembelian
	 * yang WAJIB. Retur penjualan karena itu dapat berdiri tanpa transaksi asal
	 * sama sekali — dan tanpa transaksi asal, tidak ada nilai pembanding untuk
	 * membatasi kuantitas maupun nilai yang diretur, sehingga dokumen ini
	 * menjadi jalur pengembalian uang sekaligus penambahan stok yang tidak
	 * terikat pada penjualan mana pun.
	 * </p>
	 * <p>
	 * Seperti pada tautan-tautan sejenis di klaster ini, keberadaan tautan pun
	 * tidak menjamin kesesuaian isinya: skema tidak memeriksa apakah item pada
	 * baris-baris retur benar-benar termasuk dalam transaksi yang ditunjuk di
	 * sini, tidak memeriksa batas kuantitasnya, dan tidak mencegah satu
	 * transaksi diretur berkali-kali. Tautan tingkat baris yang lebih presisi
	 * tersedia di {@link TransaksiReturDetail#getTransaksiDetail()}, dan tidak
	 * dijaga konsisten dengan tautan header ini.
	 * </p>
	 *
	 * @return transaksi medis asal, atau {@code null} bila retur ini tidak
	 *         ditautkan ke transaksi mana pun.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "transaksi", nullable = true)
	public TransaksiMedis getTransaksi() {
		return transaksi;
	}

	/**
	 * Menetapkan kode/nomor dokumen retur ini. Tidak ada validasi format
	 * maupun pengecekan keunikan, baik di level entitas maupun di level
	 * database.
	 *
	 * @param kode kode dokumen retur.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengambil kode/nomor dokumen retur ini.
	 *
	 * <p>
	 * PERHATIAN: getter ini sama sekali TIDAK memiliki anotasi
	 * {@code @Column}, sehingga kolomnya dipetakan dengan pengaturan bawaan —
	 * tanpa {@code nullable = false} dan tanpa {@code unique = true}. Ini
	 * menyimpang dari seluruh dokumen lain di klaster ini
	 * ({@link PesananPembelian#getKode()},
	 * {@link PenerimaanOrder#getKode()},
	 * {@link KoreksiItemMedis#getKode()},
	 * {@link TransferItem#getKode()}, {@link SaldoAwalMedis#getKode()} dan
	 * lainnya), yang semuanya memetakan kode sebagai {@code NOT NULL UNIQUE}.
	 * </p>
	 * <p>
	 * Akibatnya dua dokumen retur dengan kode yang sama persis dapat tersimpan
	 * berdampingan, dan dokumen tanpa kode sama sekali pun sah. Kode retur
	 * karena itu TIDAK dapat dipakai sebagai identitas bisnis: pencarian
	 * berdasarkan kode dapat mengembalikan lebih dari satu baris, dan kode yang
	 * dicetak pada bukti retur tidak menjamin merujuk satu dokumen tertentu.
	 * Untuk identifikasi yang pasti, hanya {@link #getId()} yang dapat
	 * diandalkan. Bila keunikan kode memang diinginkan, ia harus ditegakkan di
	 * lapisan action — dan pemeriksaan di lapisan itu rawan kondisi balapan
	 * karena database tidak ikut menjaganya.
	 * </p>
	 *
	 * @return kode dokumen retur, atau {@code null} bila tidak diisi.
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Menetapkan lokasi/gudang tempat retur ini dicatat.
	 *
	 * @param lokasi lokasi gudang retur.
	 */
	public void setLokasi(Lokasi lokasi) {
		this.lokasi = lokasi;
	}

	/**
	 * Mengambil lokasi/gudang tempat retur ini dicatat — gudang yang stoknya
	 * bertambah kembali saat barang diterima dari pasien, sekaligus
	 * satu-satunya sumbu pembatas lingkup data yang tersedia (modul
	 * {@code sirs} tidak punya sumbu tenant/satuan kerja).
	 *
	 * <p>
	 * Relasi OPSIONAL, dan skema tidak menjamin lokasi ini sama dengan lokasi
	 * tempat transaksi penjualannya dulu terjadi. Getter ini memanggil
	 * {@code check(...)} milik {@link ais.database.model.GeneralValueObject}
	 * untuk meresolusi proxy lazy dan menugaskan hasilnya kembali ke field —
	 * sehingga bukan getter murni: ia bisa mengubah state object dan membuka
	 * koneksi database sendiri saat sesi Hibernate asalnya sudah tertutup.
	 * </p>
	 *
	 * @return lokasi gudang retur, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi", nullable = true)
	public Lokasi getLokasi() {
		lokasi = check(lokasi);
		return lokasi;
	}

	/**
	 * Menetapkan bagian/unit tempat retur ini dicatat.
	 *
	 * @param bagian bagian/unit pencatat retur.
	 */
	public void setBagian(Bagian bagian) {
		this.bagian = bagian;
	}

	/**
	 * Mengambil {@link Bagian} tempat retur ini dicatat — dimensi organisasi
	 * pelayanan yang hanya dimiliki dokumen jalur penjualan, tidak ada pada
	 * dokumen gudang seperti {@link TransferItem} atau
	 * {@link KoreksiItemMedis}. Bersama {@link #getShift()} dan
	 * {@link #getLokasi()}, ia menentukan konteks kasir tempat pengembalian
	 * uang terjadi.
	 *
	 * <p>
	 * Relasi OPSIONAL. Getter ini memanggil {@code check(...)} sehingga bukan
	 * getter murni (lihat {@link #getLokasi()}).
	 * </p>
	 *
	 * @return bagian/unit pencatat retur, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "bagian", nullable = true)
	public Bagian getBagian() {
		bagian = check(bagian);
		return bagian;
	}

	/**
	 * Menetapkan nomor urut tampilan baris ini.
	 *
	 * @param index nomor urut tampilan.
	 */
	public void setIndex(Long index) {
		this.index = index;
	}

	/**
	 * Mengambil nomor urut tampilan baris ini. Dipakai grid/listbox ZK untuk
	 * penomoran baris; bukan bagian dari identitas dokumen.
	 *
	 * @return nomor urut tampilan, atau {@code null} bila belum diisi.
	 */
	public Long getIndex() {
		return index;
	}

	/**
	 * Menetapkan tanggal terjadinya retur ini.
	 *
	 * @param tanggal timestamp retur.
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengambil tanggal terjadinya retur ini. Field-nya di-inisialisasi
	 * {@code new Date()} pada saat object dibuat, sehingga dokumen baru
	 * otomatis bertanggal saat ini kecuali ditimpa secara eksplisit.
	 *
	 * <p>
	 * Karena entitas ini tidak punya {@code tanggalPembuatan} maupun
	 * {@code tanggalPersetujuan}, kolom inilah satu-satunya penanda waktu
	 * bisnis dokumen. Skema tidak memeriksa bahwa tanggal ini berada SETELAH
	 * tanggal {@link #getTransaksi()} yang menjadi dasarnya, sehingga retur
	 * bertanggal mendahului penjualannya tetap tersimpan tanpa keluhan.
	 * </p>
	 *
	 * @return timestamp retur, default waktu pembuatan object.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal", nullable = true)
	public Date getTanggal() {
		return tanggal;
	}

	/**
	 * Menetapkan pembayaran yang mengembalikan uang atas retur ini.
	 *
	 * @param pembayaran dokumen pembayaran pengembalian.
	 */
	public void setPembayaran(Pembayaran pembayaran) {
		this.pembayaran = pembayaran;
	}

	/**
	 * Mengambil {@link Pembayaran} yang mengembalikan uang atas retur ini —
	 * dimensi KAS yang membedakan dokumen ini dari seluruh dokumen inventaris
	 * lain di klaster ini, yang hanya berurusan dengan barang.
	 *
	 * <p>
	 * Relasi OPSIONAL, dan berdiri terpisah dari flag {@link #getLunas()}:
	 * skema tidak menjamin keduanya sejalan. Sebuah retur dapat ditandai lunas
	 * tanpa memiliki pembayaran yang tertaut, dan sebaliknya dapat memiliki
	 * pembayaran tertaut sementara flag lunasnya masih {@code false}.
	 * Ketidakcocokan itu tidak akan terdeteksi mekanisme apa pun di level
	 * model, sehingga kode yang menilai apakah uang benar-benar sudah
	 * dikembalikan sebaiknya memeriksa KEDUANYA dan tidak memercayai flag
	 * saja.
	 * </p>
	 *
	 * @return dokumen pembayaran pengembalian, atau {@code null} bila belum
	 *         ada.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pembayaran", nullable = true)
	public Pembayaran getPembayaran() {
		return pembayaran;
	}

	/**
	 * Menetapkan flag lunas dokumen retur ini.
	 *
	 * @param lunas {@code true} bila uang sudah dikembalikan.
	 */
	public void setLunas(Boolean lunas) {
		this.lunas = lunas;
	}

	/**
	 * Mengambil flag yang menyatakan bahwa uang atas retur ini sudah
	 * dikembalikan kepada pasien. Di-default {@code false} pada object baru.
	 *
	 * <p>
	 * Flag ini bersifat DUA ARAH dan tidak dikunci: setelah bernilai
	 * {@code true} ia tetap dapat dikembalikan menjadi {@code false} lewat
	 * {@link #setLunas(Boolean)}, tanpa jejak siapa yang mengubahnya dan kapan
	 * — karena entitas ini tidak punya kolom pelaku maupun timestamp untuk
	 * perubahan status. Untuk flag yang menyatakan uang sudah keluar, sifat
	 * dua arah tanpa jejak itu berarti riwayat pengembalian uang tidak dapat
	 * direkonstruksi dari kolom bisnis; hanya Hibernate Envers pada entitas ini
	 * yang menyimpannya.
	 * </p>
	 * <p>
	 * Tipe {@link Boolean} (bukan {@code boolean}) berarti nilainya bisa
	 * {@code null} bila kolom database kosong pada baris lama — keadaan ketiga
	 * di samping lunas dan belum lunas. Pemanggil yang menulis
	 * {@code if (retur.getLunas())} akan melempar
	 * {@link NullPointerException} pada baris semacam itu; bandingkan dengan
	 * {@link Boolean#TRUE} atau tangani {@code null} secara eksplisit.
	 * </p>
	 *
	 * @return {@code true} bila uang sudah dikembalikan, {@code false} bila
	 *         belum, atau {@code null} bila kolomnya kosong.
	 */
	public Boolean getLunas() {
		return lunas;
	}

	/**
	 * Mengambil flag validasi dokumen retur ini — penanda bahwa retur sudah
	 * diperiksa dan diakui sah. Di-default {@code false} pada object baru.
	 *
	 * <p>
	 * Flag inilah pengganti mekanisme persetujuan yang pada dokumen lain di
	 * klaster ini diwakili pasangan {@code disetujuiOleh} dan
	 * {@code tanggalPersetujuan}. Perbedaannya besar: pasangan kolom itu
	 * merekam SIAPA dan KAPAN, sedangkan flag ini hanya merekam BAHWA. Tidak
	 * ada kolom di entitas ini yang menyimpan siapa yang memvalidasi maupun
	 * kapan validasinya terjadi.
	 * </p>
	 * <p>
	 * Sama seperti {@link #getLunas()}, flag ini DUA ARAH dan tidak dikunci:
	 * dokumen yang sudah divalidasi dapat dikembalikan menjadi belum
	 * tervalidasi tanpa jejak. Bila validasi inilah yang memicu penambahan stok
	 * dan pengembalian uang, sifat dua arah tanpa jejak itu membuka
	 * kemungkinan siklus validasi berulang yang efeknya menumpuk — pola yang
	 * pada dokumen lain setidaknya meninggalkan jejak pada kolom penyetuju.
	 * </p>
	 * <p>
	 * Bertipe {@link Boolean} sehingga bisa {@code null}; berlaku peringatan
	 * auto-unboxing yang sama seperti pada {@link #getLunas()}.
	 * </p>
	 *
	 * @return {@code true} bila retur sudah divalidasi, {@code false} bila
	 *         belum, atau {@code null} bila kolomnya kosong.
	 */
	public Boolean getValidasi() {
		return validasi;
	}

	/**
	 * Menetapkan flag validasi dokumen retur ini. Tidak ada penjagaan arah:
	 * nilai {@code true} dapat dikembalikan menjadi {@code false} kapan saja.
	 *
	 * @param validasi {@code true} bila retur dinyatakan sah.
	 */
	public void setValidasi(Boolean validasi) {
		this.validasi = validasi;
	}

	/**
	 * Mengambil {@link Shift} kasir tempat retur ini dicatat — relasi WAJIB
	 * ({@code nullable = false}), satu-satunya relasi wajib pada entitas ini.
	 *
	 * <p>
	 * Sifat wajibnya masuk akal untuk dokumen yang menyentuh kas: setiap
	 * pengembalian uang harus dapat dipertanggungjawabkan pada satu shift kasir
	 * tertentu agar rekonsiliasi kas per shift dapat ditutup. Perlu dicatat
	 * bahwa entitas ini mewajibkan shift namun TIDAK mewajibkan
	 * {@link #getTransaksi()}, {@link #getLokasi()}, maupun
	 * {@link #getKode()} — susunan kewajiban yang menempatkan dimensi kas di
	 * atas dimensi dokumen dan barang.
	 * </p>
	 * <p>
	 * Getter ini memanggil {@code check(...)} sehingga bukan getter murni
	 * (lihat {@link #getLokasi()}).
	 * </p>
	 *
	 * @return shift kasir tempat retur dicatat.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "shift", nullable = false)
	public Shift getShift() {
		shift = check(shift);
		return shift;
	}

	/**
	 * Menetapkan shift kasir tempat retur ini dicatat.
	 *
	 * @param shift shift kasir.
	 */
	public void setShift(Shift shift) {
		this.shift = shift;
	}

}
