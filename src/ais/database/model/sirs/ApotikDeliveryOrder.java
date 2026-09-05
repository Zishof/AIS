package ais.database.model.sirs;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

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
 * Pesanan pengantaran obat dari transaksi apotik sampai bukti diterima.
 *
 * <h3>Apa yang diwakili satu baris</h3>
 *
 * <p>Satu baris adalah satu paket obat yang harus sampai ke alamat pasien: siapa
 * penerimanya, ke mana diantar, lewat kurir apa, dan sudah sampai di tahap mana.
 * Entity ini melengkapi transaksi penjualan yang sudah selesai secara keuangan —
 * uangnya sudah dibukukan lewat {@link ApotikPembayaranTransaksi}, stoknya sudah
 * berkurang lewat {@link ApotikBatchKonsumsi} — tetapi barangnya belum berpindah
 * tangan. Karena itu pembatalan pengantaran di sini TIDAK membatalkan penjualan,
 * tidak mengembalikan stok, dan tidak menghasilkan pengembalian uang. Kalau tiga
 * hal itu memang perlu terjadi, jalurnya adalah retur, bukan menyetel status di
 * entity ini.</p>
 *
 * <p>Perbedaan itu penting karena {@link #DIBATALKAN} terdengar seperti
 * pembatalan transaksi padahal bukan. Yang dibatalkan hanyalah pengantarannya;
 * obat dan uangnya tetap tercatat sudah berpindah. Membaca laporan penjualan
 * lalu mengurangkan baris berstatus DIBATALKAN akan menghasilkan angka yang
 * salah.</p>
 *
 * <h3>Data pribadi yang dibawa</h3>
 *
 * <p>Baris ini memuat alamat rumah, nomor telepon, dan nama penerima —
 * gabungan yang cukup untuk menemukan seseorang secara fisik. Digabung dengan
 * transaksinya, ia juga menyiratkan obat apa yang diterima orang tersebut.
 * Tidak ada penyamaran apa pun di lapisan ini dan tidak ada pemisahan antar
 * apotek: {@code ApotikDeliveryHelper.list} menyaring berdasarkan status dan
 * kata kunci saja, tanpa sumbu unit apa pun. Siapa pun yang lolos gerbang menu
 * pengantaran melihat seluruh baris yang ada di basis data. Itu perlu diketahui
 * sebelum menambah jalur baca, ekspor, atau integrasi kurir di atas tabel
 * ini.</p>
 *
 * <h3>Status: keanggotaan diperiksa, urutan tidak</h3>
 *
 * <p>{@code ApotikDeliveryHelper.statusSah} menolak nilai di luar keenam
 * konstanta, sehingga kolom status tidak dapat diisi teks bebas. Yang TIDAK
 * diperiksa adalah urutan: satu baris boleh melompat dari MENUNGGU langsung ke
 * TERKIRIM tanpa pernah melewati DIKIRIM, dan boleh mundur dari TERKIRIM
 * kembali ke MENUNGGU. Tidak ada mesin keadaan di mana pun.</p>
 *
 * <p>Untuk papan koordinasi harian kelonggaran itu wajar. Yang perlu disadari
 * adalah akibatnya pada dua stempel waktu: {@link #getWaktuKirim()} dan
 * {@link #getWaktuTerima()} diisi oleh helper hanya ketika status berpindah ke
 * DIKIRIM/TERKIRIM dan hanya bila masih kosong. Karena mundurnya status tidak
 * mengosongkan keduanya, sebuah baris dapat berakhir berstatus GAGAL namun
 * tetap membawa waktu terima — bentuk yang saling bertentangan dan tidak ada
 * yang mencegahnya. Laporan yang menghitung keberhasilan pengantaran harus
 * bersandar pada status, bukan pada ada-tidaknya stempel waktu.</p>
 *
 * @see ApotikPembayaranTransaksi metode dan nominal pembayaran transaksi yang sama
 * @see AntreanFarmasi papan antrean penyiapan sebelum obat masuk pengantaran
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "apotik_delivery_order")
public class ApotikDeliveryOrder extends GeneralValueObject {

	/** Versi serialisasi; tetap 1 selama bentuk field tidak berubah maknanya. */
	private static final long serialVersionUID = 1L;

	/**
	 * Status awal: pesanan tercatat, belum ada yang menyiapkan paketnya.
	 *
	 * <p>Sekaligus nilai yang dikembalikan {@link #getStatus()} bila kolomnya
	 * kosong, sehingga baris yang entah bagaimana tersimpan tanpa status tidak
	 * pernah tampil sebagai sudah-terkirim. Bawaan yang aman: menganggap
	 * pekerjaan belum dikerjakan lebih murah daripada menganggapnya selesai.</p>
	 */
	public static final String MENUNGGU = "MENUNGGU";

	/** Paket sudah dikemas dan menunggu diambil kurir. */
	public static final String DISIAPKAN = "DISIAPKAN";

	/** Paket sudah diserahkan ke kurir dan dalam perjalanan. */
	public static final String DIKIRIM = "DIKIRIM";

	/** Paket sudah diterima; {@link #getBuktiTerima()} semestinya terisi. */
	public static final String TERKIRIM = "TERKIRIM";

	/** Pengantaran gagal (alamat tidak ditemukan, penerima tidak ada, dsb.). */
	public static final String GAGAL = "GAGAL";

	/**
	 * Pengantaran dibatalkan.
	 *
	 * <p>Membatalkan PENGANTARAN, bukan penjualan — lihat catatan pada
	 * dokumentasi class. Uang dan stok tetap tercatat berpindah.</p>
	 */
	public static final String DIBATALKAN = "DIBATALKAN";

	/** Kunci baris; dibangkitkan basis data. */
	private Long id;

	/** Nomor pesanan pengantaran; unik di seluruh tabel. */
	private String kode;

	/** Transaksi penjualan sumber; boleh kosong. */
	private TransaksiMedis transaksi;

	/** Nama orang yang akan menerima paket. Wajib. */
	private String namaPenerima;

	/** Nomor telepon penerima untuk dihubungi kurir. */
	private String telepon;

	/** Alamat tujuan pengantaran. Wajib. */
	private String alamat;

	/** Nama jasa kurir/pengantar. */
	private String kurir;

	/** Jenis layanan kurir (reguler, sameday, dsb.). */
	private String layanan;

	/** Nomor resi/pelacakan dari kurir. */
	private String nomorPelacakan;

	/** Ongkos kirim; catatan saja, tidak ikut dibukukan. */
	private Double biayaKirim;

	/** Salah satu dari keenam konstanta status. */
	private String status;

	/** Waktu pesanan pengantaran dibuat. */
	private Date waktuPesan;

	/** Waktu paket diserahkan ke kurir. */
	private Date waktuKirim;

	/** Waktu paket diterima penerima. */
	private Date waktuTerima;

	/** Keterangan bukti penerimaan (nama penerima sebenarnya, catatan kurir). */
	private String buktiTerima;

	/** Catatan bebas operasional. */
	private String keterangan;

	/** Nama tampil pelaku perubahan terakhir (bayangan audit). */
	private String oleh;

	/** Identitas akun pelaku perubahan terakhir (bayangan audit). */
	private String olehId;

	/** Stempel ubah terakhir; disegarkan interseptor audit pada setiap UPDATE. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Kunci baris, dibangkitkan basis data ({@code IDENTITY}).
	 *
	 * <p>{@code insertable = false} memastikan nilai apa pun yang tersisa di
	 * objek Java tidak ikut dalam INSERT; basis data yang menentukan.</p>
	 *
	 * @return kunci baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }

	/**
	 * Menetapkan kunci baris; dipakai Hibernate, bukan kode aplikasi.
	 *
	 * @param id kunci baris
	 */
	public void setId(Long id) { this.id = id; }

	/**
	 * Nomor pesanan pengantaran — penanda yang diucapkan orang.
	 *
	 * <p>Dijaga {@code unique = true} di tingkat kolom. Pilihan itu tepat:
	 * nomor pengantaran adalah rujukan yang dipakai lintas pihak — petugas
	 * apotek, kurir, dan pasien yang menanyakan paketnya — dan dua baris
	 * bernomor sama membuat pertanyaan "pesanan DO-123 sampai mana" tidak
	 * punya jawaban tunggal.</p>
	 *
	 * <p>Kolomnya juga tempat pencarian: {@code ApotikDeliveryHelper.list}
	 * mencari dengan {@code ilike} atas kode dan nama penerima. Karena
	 * pencarian itu memakai kriteria Hibernate dengan parameter terikat, teks
	 * carian tidak dapat menyusup ke SQL.</p>
	 *
	 * @return nomor pesanan; tidak boleh {@code null} pada baris tersimpan
	 */
	@Column(name = "kode", unique = true, nullable = false, length = 60)
	public String getKode() { return kode; }

	/**
	 * Menetapkan nomor pesanan pengantaran.
	 *
	 * <p>Tidak memeriksa keunikan — itu urusan batasan basis data, yang akan
	 * menolak duplikat pada saat {@code flush} dengan pengecualian, bukan
	 * dengan nilai kembalian. Pemanggil yang membuat kode wajib bersiap
	 * menangani kegagalan itu.</p>
	 *
	 * @param kode nomor pesanan
	 */
	public void setKode(String kode) { this.kode = kode; }

	/**
	 * Transaksi penjualan sumber pengantaran ini.
	 *
	 * <p>Getter DESTRUKTIF: hasil {@code check(...)} ditulis balik ke field
	 * sebelum dikembalikan. {@code check} menormalkan proksi malas Hibernate
	 * yang sudah lepas dari sesinya menjadi {@code null}, alih-alih membiarkan
	 * {@code LazyInitializationException} meledak ketika objek dibaca di luar
	 * sesi. Konsekuensinya: memanggil getter ini dapat mengubah keadaan objek,
	 * dan dua panggilan berturut-turut tidak dijamin sama bila di antaranya
	 * sesi ditutup.</p>
	 *
	 * <p>Relasi ini BOLEH kosong — {@code @JoinColumn} tanpa
	 * {@code nullable = false}. Kelonggaran itu memungkinkan pengantaran
	 * dicatat untuk hal yang bukan penjualan langsung, tetapi juga berarti
	 * setiap pembaca wajib memperlakukan hasilnya sebagai mungkin-kosong.
	 * {@code ApotikDeliveryHelper} sudah melakukannya dengan benar: ia
	 * menuliskan kode transaksi sebagai string kosong ketika relasinya
	 * {@code null}, bukan melempar.</p>
	 *
	 * @return transaksi sumber, atau {@code null}
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "transaksi")
	public TransaksiMedis getTransaksi() { transaksi = check(transaksi); return transaksi; }

	/**
	 * Menetapkan transaksi penjualan sumber.
	 *
	 * @param transaksi transaksi sumber, boleh {@code null}
	 */
	public void setTransaksi(TransaksiMedis transaksi) { this.transaksi = transaksi; }

	/**
	 * Nama orang yang akan menerima paket.
	 *
	 * <p>Data pribadi. Tidak selalu sama dengan nama pasien — obat boleh
	 * diterimakan kepada keluarga — sehingga ia bukan pengganti identitas
	 * pasien dan tidak boleh dipakai mencocokkan rekam medis.</p>
	 *
	 * @return nama penerima
	 */
	@Column(name = "nama_penerima", nullable = false, length = 160)
	public String getNamaPenerima() { return namaPenerima; }

	/**
	 * Menetapkan nama penerima paket.
	 *
	 * @param namaPenerima nama penerima
	 */
	public void setNamaPenerima(String namaPenerima) { this.namaPenerima = namaPenerima; }

	/**
	 * Nomor telepon penerima.
	 *
	 * <p>Data pribadi. Disimpan apa adanya tanpa penyamaran maupun normalisasi
	 * bentuk, sehingga nomor yang sama dapat tersimpan dalam beberapa
	 * penulisan ({@code 08...}, {@code +628...}). Jangan memakainya sebagai
	 * kunci pencocokan pasien.</p>
	 *
	 * @return nomor telepon, atau {@code null}
	 */
	@Column(name = "telepon", length = 40)
	public String getTelepon() { return telepon; }

	/**
	 * Menetapkan nomor telepon penerima.
	 *
	 * @param telepon nomor telepon
	 */
	public void setTelepon(String telepon) { this.telepon = telepon; }

	/**
	 * Alamat tujuan pengantaran.
	 *
	 * <p>Data pribadi paling sensitif di baris ini: digabung dengan transaksinya
	 * ia menyatakan di mana seseorang tinggal DAN obat apa yang ia terima.
	 * Panjangnya 800 karakter karena alamat Indonesia yang lengkap dengan
	 * patokan memang panjang; itu bukan izin untuk menaruh catatan klinis di
	 * sini.</p>
	 *
	 * @return alamat tujuan
	 */
	@Column(name = "alamat", nullable = false, length = 800)
	public String getAlamat() { return alamat; }

	/**
	 * Menetapkan alamat tujuan.
	 *
	 * @param alamat alamat tujuan
	 */
	public void setAlamat(String alamat) { this.alamat = alamat; }

	/**
	 * Nama jasa kurir/pengantar.
	 *
	 * <p>Teks bebas, bukan relasi ke master mana pun. Untuk apotek yang memakai
	 * beberapa kurir sekaligus, akibatnya laporan per-kurir harus
	 * mengelompokkan teks yang penulisannya boleh berbeda-beda. Kalau
	 * pengelompokan itu suatu saat dibutuhkan, master kurir yang benar lebih
	 * baik daripada menormalkan teksnya secara diam-diam saat dibaca.</p>
	 *
	 * @return nama kurir, atau {@code null}
	 */
	@Column(name = "kurir", length = 120)
	public String getKurir() { return kurir; }

	/**
	 * Menetapkan nama jasa kurir.
	 *
	 * @param kurir nama kurir
	 */
	public void setKurir(String kurir) { this.kurir = kurir; }

	/**
	 * Jenis layanan kurir (reguler, sameday, instan, dan sebagainya).
	 *
	 * <p>Untuk obat rantai dingin pilihan layanan bukan sekadar soal biaya —
	 * lihat {@link ApotikItemProfile#getColdChain()}. Entity ini tidak
	 * mengetahui isi paket dan karena itu tidak dapat menuntut layanan
	 * tertentu; pertimbangan itu ada pada orang yang membuat pesanan.</p>
	 *
	 * @return jenis layanan, atau {@code null}
	 */
	@Column(name = "layanan", length = 80)
	public String getLayanan() { return layanan; }

	/**
	 * Menetapkan jenis layanan kurir.
	 *
	 * @param layanan jenis layanan
	 */
	public void setLayanan(String layanan) { this.layanan = layanan; }

	/**
	 * Nomor resi/pelacakan dari kurir.
	 *
	 * @return nomor pelacakan, atau {@code null} bila kurir tidak memberikannya
	 */
	@Column(name = "nomor_pelacakan", length = 120)
	public String getNomorPelacakan() { return nomorPelacakan; }

	/**
	 * Menetapkan nomor resi/pelacakan.
	 *
	 * @param nomorPelacakan nomor resi
	 */
	public void setNomorPelacakan(String nomorPelacakan) { this.nomorPelacakan = nomorPelacakan; }

	/**
	 * Ongkos kirim yang dibebankan.
	 *
	 * <p>Mengembalikan {@code 0} bila kosong sehingga penjumlahan di layar tidak
	 * perlu berjaga terhadap {@code null}.</p>
	 *
	 * <p><b>Catatan saja, bukan angka pembukuan.</b> Tidak ada satu pun jalur
	 * posting yang membaca kolom ini: {@code ApotikPostingHelper} membukukan
	 * penjualan dan HPP dari transaksi, {@code ApotikPbfPostingHelper}
	 * membukukan utang distributor — keduanya tidak menyentuh ongkos kirim.
	 * Jadi ongkos kirim yang tercatat di sini tidak pernah menjadi jurnal, tidak
	 * pernah menambah pendapatan maupun beban, dan tidak ikut dalam rekonsiliasi
	 * kas {@link ApotikSesiKas}. Kalau ongkos kirim memang harus dibukukan, ia
	 * perlu masuk sebagai baris pembayaran atau jurnal tersendiri; menaikkan
	 * angka di sini tidak akan berpengaruh apa-apa pada laporan keuangan.</p>
	 *
	 * @return ongkos kirim; {@code 0} bila kosong
	 */
	@Column(name = "biaya_kirim")
	public Double getBiayaKirim() { return biayaKirim == null ? Double.valueOf(0) : biayaKirim; }

	/**
	 * Menetapkan ongkos kirim.
	 *
	 * @param biayaKirim ongkos kirim
	 */
	public void setBiayaKirim(Double biayaKirim) { this.biayaKirim = biayaKirim; }

	/**
	 * Tahap pengantaran saat ini.
	 *
	 * <p>Mengembalikan {@link #MENUNGGU} bila kolom kosong — bawaan yang aman,
	 * karena menganggap paket belum berangkat lebih murah akibatnya daripada
	 * menganggapnya sudah sampai.</p>
	 *
	 * <p>Keanggotaan nilai dijaga {@code ApotikDeliveryHelper.statusSah};
	 * urutan perpindahannya tidak dijaga sama sekali. Lihat dokumentasi class
	 * untuk akibatnya pada stempel waktu.</p>
	 *
	 * @return status pengantaran; {@link #MENUNGGU} bila kolom kosong
	 */
	@Column(name = "status", nullable = false, length = 20)
	public String getStatus() { return status == null ? MENUNGGU : status; }

	/**
	 * Menetapkan tahap pengantaran.
	 *
	 * <p>Menyimpan apa adanya; penyaringan nilai dikerjakan pemanggil.</p>
	 *
	 * @param status salah satu dari keenam konstanta status
	 */
	public void setStatus(String status) { this.status = status; }

	/**
	 * Waktu pesanan pengantaran dibuat.
	 *
	 * @return waktu pemesanan
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu_pesan", nullable = false)
	public Date getWaktuPesan() { return waktuPesan; }

	/**
	 * Menetapkan waktu pemesanan.
	 *
	 * @param waktuPesan waktu pemesanan
	 */
	public void setWaktuPesan(Date waktuPesan) { this.waktuPesan = waktuPesan; }

	/**
	 * Waktu paket diserahkan ke kurir.
	 *
	 * <p>Diisi {@code ApotikDeliveryHelper.ubahStatus} saat status berpindah ke
	 * {@link #DIKIRIM}, dan HANYA bila masih kosong — sehingga perpindahan
	 * berulang tidak menggeser waktu keberangkatan yang pertama. Sisi lain dari
	 * penjagaan itu: nilainya juga tidak pernah dikosongkan kembali ketika
	 * status mundur, sehingga baris berstatus {@link #MENUNGGU} atau
	 * {@link #GAGAL} dapat tetap membawa waktu kirim. Bacalah status lebih
	 * dulu, jangan menyimpulkan tahap dari ada-tidaknya stempel ini.</p>
	 *
	 * @return waktu penyerahan ke kurir, atau {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu_kirim")
	public Date getWaktuKirim() { return waktuKirim; }

	/**
	 * Menetapkan waktu penyerahan ke kurir.
	 *
	 * @param waktuKirim waktu kirim
	 */
	public void setWaktuKirim(Date waktuKirim) { this.waktuKirim = waktuKirim; }

	/**
	 * Waktu paket diterima penerima.
	 *
	 * <p>Berlaku pertimbangan yang sama dengan {@link #getWaktuKirim()}: diisi
	 * sekali saat status berpindah ke {@link #TERKIRIM}, tidak pernah
	 * dikosongkan lagi.</p>
	 *
	 * @return waktu penerimaan, atau {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu_terima")
	public Date getWaktuTerima() { return waktuTerima; }

	/**
	 * Menetapkan waktu penerimaan.
	 *
	 * @param waktuTerima waktu terima
	 */
	public void setWaktuTerima(Date waktuTerima) { this.waktuTerima = waktuTerima; }

	/**
	 * Keterangan bukti penerimaan paket.
	 *
	 * <p>Berisi teks — nama orang yang benar-benar menerima, catatan kurir —
	 * bukan berkas gambar. Tidak ada satu pun pemeriksaan yang menuntut kolom
	 * ini terisi sebelum status boleh menjadi {@link #TERKIRIM}, sehingga
	 * "terkirim tanpa bukti" adalah keadaan yang sah menurut sistem. Untuk obat
	 * keras dan terkendali, bukti serah-terima yang dapat
	 * dipertanggungjawabkan seharusnya tidak bergantung pada kolom teks yang
	 * boleh kosong; jejak penyerahan yang lebih kuat ada di
	 * {@link ApotikDispensingLog} dan {@link ApotikNarkotikaLog}.</p>
	 *
	 * @return keterangan bukti terima, atau {@code null}
	 */
	@Column(name = "bukti_terima", length = 500)
	public String getBuktiTerima() { return buktiTerima; }

	/**
	 * Menetapkan keterangan bukti penerimaan.
	 *
	 * @param buktiTerima keterangan bukti
	 */
	public void setBuktiTerima(String buktiTerima) { this.buktiTerima = buktiTerima; }

	/**
	 * Catatan bebas operasional.
	 *
	 * <p>Bukan tempat data klinis: baris ini dibaca petugas pengantaran dan
	 * dapat ikut tercetak pada label paket.</p>
	 *
	 * @return catatan, atau {@code null}
	 */
	@Column(name = "keterangan", length = 800)
	public String getKeterangan() { return keterangan; }

	/**
	 * Menetapkan catatan operasional.
	 *
	 * @param keterangan catatan
	 */
	public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

	/**
	 * Nama tampil pelaku perubahan terakhir (bayangan audit).
	 *
	 * @return nama pelaku, atau {@code null}
	 */
	@Column(name = "oleh", length = 60)
	public String getOleh() { return oleh; }

	/**
	 * Menetapkan nama pelaku perubahan terakhir.
	 *
	 * <p>Berbeda dari sebagian besar entity di paket ini, setter ini menetapkan
	 * apa adanya — termasuk {@code null} dan string kosong. Perbedaan itu perlu
	 * disebut agar tidak dikira kekeliruan pembacaan: pada entity seperti
	 * {@link ApotikBatchKonsumsi} dan {@link ApotikNarkotikaLog}, setter yang
	 * setara menolak nilai kosong demi melindungi jejak pelaku dari penyalinan
	 * properti yang lugu. Di sini perlindungan itu tidak ada, sehingga jalur
	 * mana pun yang menyalin seluruh properti dari objek kosong akan menghapus
	 * nama pelaku tanpa tanda apa pun.</p>
	 *
	 * <p>Untuk pesanan pengantaran akibatnya terbatas — Envers masih menyimpan
	 * revisi sebelumnya di {@code new_audit.apotik_delivery_order__audit},
	 * sehingga pertanyaan "siapa mengubah status ini" tetap dapat dijawab dari
	 * sana meski kolom bayangannya sudah bersih. Yang hilang hanyalah
	 * kemudahan membacanya langsung dari baris. Jangan menjadikan ketiadaan
	 * penjagaan di sini sebagai alasan menghapusnya di entity lain, di mana
	 * pertaruhannya jauh lebih besar.</p>
	 *
	 * @param oleh nama pelaku
	 */
	public void setOleh(String oleh) { this.oleh = oleh; }

	/**
	 * Identitas akun pelaku perubahan terakhir (bayangan audit).
	 *
	 * @return id akun pelaku, atau {@code null}
	 */
	@Column(name = "oleh_id", length = 60)
	public String getOlehId() { return olehId; }

	/**
	 * Menetapkan id akun pelaku perubahan terakhir.
	 *
	 * <p>Menetapkan apa adanya; berlaku catatan pada {@link #setOleh(String)}.</p>
	 *
	 * @param olehId id akun pelaku
	 */
	public void setOlehId(String olehId) { this.olehId = olehId; }

	/**
	 * Stempel perubahan terakhir.
	 *
	 * @return waktu ubah terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dirubah")
	public Date getTanggal_dirubah() { return tanggal_dirubah; }

	/**
	 * Menetapkan stempel perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu ubah
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }

	/**
	 * Kait JPA sebelum UPDATE: menyegarkan {@link #getTanggal_dirubah()}.
	 *
	 * <p>Berjalan lewat {@code AuditTimestampInterceptor.ubah(this)} agar semua
	 * entity memakai satu sumber waktu yang sama. TIDAK berjalan pada INSERT —
	 * di sana nilai awal field yang berlaku.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }

	/**
	 * Representasi teks: nomor pesanan, atau string kosong bila belum ada.
	 *
	 * <p>Sengaja tidak pernah mengembalikan {@code null} supaya penggabungan
	 * teks di layar dan log tidak memunculkan kata "null". Membaca field
	 * langsung, bukan lewat getter, sehingga aman dipanggil pada objek yang
	 * sudah lepas dari sesi.</p>
	 *
	 * @return nomor pesanan, atau string kosong
	 */
	public String toString() { return kode == null ? "" : kode; }
}
