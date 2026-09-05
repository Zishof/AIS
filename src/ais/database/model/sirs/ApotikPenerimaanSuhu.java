package ais.database.model.sirs;

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
 * Bukti suhu penerimaan barang cold-chain (bagian IR-09 yang dapat dikerjakan
 * tanpa mengarang alur pengadaan).
 *
 * <p><b>Kenapa hanya suhu, bukan seluruh IR-09.</b> Nomor PO dan penerimaan
 * sebagian menuntut adanya dokumen pesanan lebih dulu — dan bentuk alur
 * pengadaan (siapa menyetujui, bagaimana harga disepakati, apakah PO dibuat di
 * AIS atau di luar) berbeda-beda antar apotek. Membuatnya tanpa keputusan
 * pemilik proses berarti menebak. Pencatatan suhu tidak butuh tebakan: barang
 * rantai dingin memang harus diukur saat diterima, dan rentang 2–8 °C adalah
 * standar yang sudah dipakai di layar formularium.</p>
 *
 * <p>Dicatat per FAKTUR, bukan per lot: termometer dibaca sekali saat kotak
 * dibuka, bukan per butir obat. Tabel BARU sehingga {@code hbm2ddl=update}
 * membuatnya berikut tabel auditnya — tanpa migrasi manual.</p>
 *
 * <p><b>Batas jujur:</b> server MENYIMPAN, tidak menolak. Tidak ada aturan
 * "tolak penerimaan bila suhu di luar rentang", karena keputusan menerima atau
 * menolak barang rantai dingin adalah wewenang apoteker penanggung jawab dan
 * bergantung pada SOP tiap apotek. Layar memperingatkan; yang memutuskan
 * manusia.</p>
 *
 * <h3>Apa arti "menyimpan, tidak menolak" dalam praktik</h3>
 *
 * <p>Paragraf di atas menyatakan sikapnya; ada gunanya menyebut dengan tepat
 * apa yang terjadi, supaya tidak ada yang mengira ada pagar yang sebenarnya
 * tidak ada. {@code ApotikPersediaanHelper} menulis baris bukti suhu di dalam
 * transaksi penerimaan yang sama, lalu mengembalikan
 * {@code suhuDiLuarRentang} pada responsnya. Nilai itu adalah PERINGATAN untuk
 * ditampilkan layar. Penerimaan tetap tersimpan, batch {@link Kadaluarsa} tetap
 * terbentuk, stok tetap bertambah, dan lot yang lahir tetap berstatus
 * {@link Kadaluarsa#LOT_ELIGIBLE} — layak dijual — betapa pun jauh suhunya di
 * luar rentang.</p>
 *
 * <p>Tidak ada pula tautan apa pun dari baris bukti suhu ke batch yang lahir
 * pada faktur yang sama: penghubungnya hanyalah {@link #getNoFaktur()} sebagai
 * teks, dan nomor faktur itu sendiri tidak dijaga unik di
 * {@link ApotikPbfDokumen}. Akibatnya, dari sebuah lot obat tidak ada jalan
 * langsung untuk menanyakan "berapa suhunya waktu diterima" — pertanyaan itu
 * harus dijawab dengan mencocokkan teks nomor faktur, dan jawabannya bisa lebih
 * dari satu.</p>
 *
 * <p><b>Ini bukan cacat yang harus segera ditutup dengan pagar keras.</b> Sikap
 * yang diambil — mencatat dan memperingatkan, menyerahkan keputusan kepada
 * apoteker penanggung jawab — memang bentuk yang benar untuk keputusan yang
 * bergantung pada SOP, jenis obat, dan berapa lama penyimpangannya berlangsung.
 * Menolak otomatis akan mendorong petugas mengakali pencatatan agar barangnya
 * dapat masuk, dan hasilnya justru catatan yang tidak jujur. Yang perlu
 * diketahui pembaca kode adalah bahwa perlindungan sesungguhnya berada pada
 * orang, bukan pada sistem, dan bahwa catatan ini berguna sebagai bukti setelah
 * kejadian — bukan sebagai pencegah.</p>
 *
 * <p>Bila kelak diputuskan bahwa penyimpangan suhu harus berakibat, bentuk yang
 * paling sedikit merusak adalah menandai lot yang lahir sebagai
 * {@link Kadaluarsa#LOT_QUARANTINE} alih-alih menolak seluruh penerimaan.
 * Dengan begitu barang tetap tercatat masuk — sehingga tidak ada dorongan untuk
 * menyembunyikannya — tetapi tidak dapat dijual sampai seseorang yang berwenang
 * melepaskannya. Mekanisme status lot itu sudah ada dan sudah ditegakkan
 * {@code ApotikApiHelper.bayar}; yang belum ada hanyalah yang menyetelnya
 * berdasarkan suhu.</p>
 *
 * @see ApotikItemProfile#getColdChain() penanda per item yang menentukan faktur ini wajib bersuhu
 * @see Kadaluarsa#getStatusLot() mekanisme yang dapat menahan lot dari penjualan
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "apotik_penerimaan_suhu")
public class ApotikPenerimaanSuhu extends GeneralValueObject {

	/** Versi serialisasi; tetap 1 selama bentuk field tidak berubah maknanya. */
	private static final long serialVersionUID = 1L;

	/**
	 * Rentang rantai dingin baku yang dipakai layar (2–8 derajat Celsius).
	 *
	 * <p>Batas bawah. Angka 2–8 °C adalah rentang penyimpanan lemari pendingin
	 * farmasi yang berlaku umum untuk vaksin dan sediaan biologis. Ia sengaja
	 * ditulis sebagai konstanta di entity, bukan diambil dari konfigurasi:
	 * rentangnya tidak berbeda antar apotek, dan konstanta yang terbaca di kode
	 * lebih mudah diperiksa daripada nilai yang tersembunyi di tabel
	 * pengaturan.</p>
	 *
	 * <p>Perlu disadari bahwa rentang ini berlaku untuk PENYIMPANAN dingin biasa.
	 * Sediaan yang menuntut beku ({@code -20} °C) atau beku sangat dalam akan
	 * dinyatakan "di luar rentang" oleh {@link #diLuarRentang(Double)} padahal
	 * suhunya justru benar. Sistem ini tidak membedakan keduanya karena
	 * {@link ApotikItemProfile} hanya mengenal satu penanda rantai dingin, bukan
	 * kelas suhu. Untuk apotek yang menangani sediaan beku, peringatan yang
	 * muncul akan keliru dan perlu diabaikan oleh manusia — alasan tambahan
	 * mengapa peringatan ini tidak boleh dijadikan penolakan otomatis tanpa
	 * lebih dulu memperkaya profil itemnya.</p>
	 */
	public static final double SUHU_MIN_WAJAR = 2d;

	/** Batas atas rentang rantai dingin baku; lihat {@link #SUHU_MIN_WAJAR}. */
	public static final double SUHU_MAKS_WAJAR = 8d;

	/** Kunci baris; dibangkitkan basis data. */
	private Long id;

	/** Nomor faktur — satu-satunya penghubung ke dokumen dan batchnya, sebagai teks. */
	private String noFaktur;

	/** Nama distributor pada faktur tersebut. */
	private String penyedia;

	/** Suhu terbaca saat barang diterima, derajat Celsius. */
	private Double suhuCelsius;

	/** true bila faktur ini memang memuat item bertanda cold-chain. */
	private Boolean adaColdChain;

	/** Catatan bebas tentang keadaan penerimaan. */
	private String keterangan;

	/** Waktu pembacaan termometer dicatat. */
	private Date waktu;

	/** Nama tampil pelaku pencatatan (bayangan audit). */
	private String oleh;

	/** Identitas akun pelaku pencatatan (bayangan audit). */
	private String olehId;

	/**
	 * Kait JPA sebelum UPDATE: menyegarkan {@link #getTanggal_dirubah()}.
	 *
	 * <p>Tidak pernah berjalan pada pemakaian sekarang: bukti suhu ditulis sekali
	 * di dalam transaksi penerimaan dan tidak ada jalur yang menyuntingnya.
	 * Untuk sebuah bukti, sifat itu memang yang diinginkan.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Stempel ubah terakhir; disegarkan interseptor audit pada setiap UPDATE. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Apakah [suhu] berada di luar rentang rantai dingin baku.
	 *
	 * <p>Statis dan tanpa keadaan, sehingga dapat dipanggil atas angka mana pun —
	 * termasuk sebelum baris bukti dibuat, ketika layar ingin memperingatkan
	 * petugas saat ia mengetikkan angkanya.</p>
	 *
	 * <p><b>Nilai kosong dianggap DI DALAM rentang</b> ({@code false}), bukan di
	 * luar. Arah bawaan itu perlu dipahami karena ia bukan pilihan yang paling
	 * hati-hati. Suhu yang tidak diukur sama sekali bukanlah bukti bahwa rantai
	 * dinginnya terjaga — ia justru ketiadaan bukti, keadaan yang untuk barang
	 * rantai dingin lebih mengkhawatirkan daripada satu pembacaan yang meleset,
	 * sebab yang terakhir setidaknya diketahui. Metode ini tetap menjawab
	 * {@code false} untuk keadaan itu.</p>
	 *
	 * <p>Pilihan itu dapat dibela pada tempatnya: metode ini menjawab pertanyaan
	 * sempit "apakah angka yang terbaca menyimpang", dan angka yang tidak ada
	 * tidak menyimpang dari apa pun. Yang perlu dijaga adalah pemakaiannya.
	 * Pemanggil sekarang sudah melakukannya dengan benar — {@code hasil} membawa
	 * {@code suhuTercatat} secara terpisah dari {@code suhuDiLuarRentang},
	 * sehingga layar dapat membedakan "diukur dan wajar" dari "tidak diukur".
	 * Siapa pun yang memakai metode ini sendirian, tanpa memeriksa lebih dulu
	 * apakah suhunya ada, akan menyimpulkan bahwa faktur yang tidak pernah
	 * diukur suhunya baik-baik saja.</p>
	 *
	 * @param suhu suhu terbaca dalam derajat Celsius; boleh {@code null}
	 * @return {@code true} bila suhu ada dan berada di luar
	 *         {@link #SUHU_MIN_WAJAR}..{@link #SUHU_MAKS_WAJAR};
	 *         {@code false} bila di dalam rentang ATAU bila suhu {@code null}
	 */
	public static boolean diLuarRentang(Double suhu) {
		if (suhu == null) return false;
		double v = suhu.doubleValue();
		return v < SUHU_MIN_WAJAR || v > SUHU_MAKS_WAJAR;
	}

	/**
	 * Representasi teks: kunci baris dan nomor faktur, dipisah tanda hubung.
	 *
	 * <p>Membaca field langsung, bukan lewat getter, sehingga aman dipanggil
	 * pada objek yang sudah lepas dari sesi Hibernate. Bagian kosong diganti
	 * string kosong supaya hasilnya tidak pernah memuat kata "null".</p>
	 *
	 * @return teks ringkas untuk log dan layar
	 */
	public String toString() {
		return (id == null ? "" : id) + "-" + (noFaktur == null ? "" : noFaktur);
	}

	/**
	 * Kunci baris, dibangkitkan basis data ({@code IDENTITY}).
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
	 * Nomor faktur penerimaan yang diukur suhunya.
	 *
	 * <p>Satu-satunya penghubung antara bukti suhu ini dan penerimaannya:
	 * disalin sebagai TEKS dari {@link ApotikPbfDokumen#getNoFaktur()}, tanpa
	 * relasi keras. Bentuk longgar itu punya harga yang perlu diketahui. Nomor
	 * faktur di dokumen PBF boleh kosong dan tidak dijaga unik, sehingga
	 * pencocokan teks di sini dapat menemukan nol dokumen, satu dokumen, atau
	 * beberapa dokumen sekaligus. Dari sebuah lot obat, karena itu, tidak ada
	 * jalan yang pasti untuk sampai ke pembacaan termometernya.</p>
	 *
	 * <p>Kolomnya sendiri boleh kosong. Baris bukti tanpa nomor faktur akan
	 * tercatat tetapi tidak dapat dihubungkan ke apa pun — hanya berguna sebagai
	 * catatan bahwa suatu penerimaan pernah diukur.</p>
	 *
	 * @return nomor faktur, atau {@code null}
	 */
	@Column(name = "no_faktur", length = 80)
	public String getNoFaktur() { return noFaktur; }

	/**
	 * Menetapkan nomor faktur penerimaan.
	 *
	 * @param noFaktur nomor faktur
	 */
	public void setNoFaktur(String noFaktur) { this.noFaktur = noFaktur; }

	/**
	 * Nama distributor pada faktur tersebut.
	 *
	 * <p>Disalin dari dokumen penerimaan, sebagai teks. Bersama
	 * {@link #getNoFaktur()} ia menjadi pasangan yang lebih meyakinkan untuk
	 * mencocokkan bukti ini dengan dokumennya daripada nomor faktur sendirian —
	 * distributor yang berbeda dapat memakai penomoran yang bertabrakan.</p>
	 *
	 * @return nama distributor, atau {@code null}
	 */
	@Column(name = "penyedia", length = 160)
	public String getPenyedia() { return penyedia; }

	/**
	 * Menetapkan nama distributor.
	 *
	 * @param penyedia nama distributor
	 */
	public void setPenyedia(String penyedia) { this.penyedia = penyedia; }

	/**
	 * Suhu terbaca saat barang diterima, derajat Celsius.
	 *
	 * <p>Mengembalikan {@code null} apa adanya — sengaja TIDAK diganti nol.
	 * Perbedaan itu menentukan: nol derajat Celsius adalah pembacaan yang sah
	 * dan berada di luar rentang aman, sedangkan tidak ada pembacaan adalah hal
	 * yang sama sekali lain. Mengganti kosong dengan nol akan membuat setiap
	 * faktur yang tidak diukur tampak sebagai faktur yang suhunya menyimpang —
	 * banjir peringatan palsu yang pada gilirannya membuat peringatan yang
	 * sungguhan diabaikan.</p>
	 *
	 * <p>Nilainya datang dari petugas lewat payload permintaan; tidak ada
	 * pembacaan otomatis dari alat, dan tidak ada batas kewajaran yang
	 * ditegakkan. Angka apa pun tersimpan, termasuk yang mustahil. Untuk
	 * catatan yang dimaksudkan sebagai bukti, keterbatasan itu perlu diketahui:
	 * ia membuktikan bahwa seseorang mengetikkan sebuah angka, bukan bahwa
	 * termometer menunjukkan angka itu.</p>
	 *
	 * @return suhu dalam derajat Celsius, atau {@code null} bila tidak diukur
	 */
	@Column(name = "suhu_celsius")
	public Double getSuhuCelsius() { return suhuCelsius; }

	/**
	 * Menetapkan suhu terbaca.
	 *
	 * @param suhuCelsius suhu dalam derajat Celsius, boleh {@code null}
	 */
	public void setSuhuCelsius(Double suhuCelsius) { this.suhuCelsius = suhuCelsius; }

	/**
	 * Apakah faktur ini memang memuat item bertanda rantai dingin.
	 *
	 * <p>Mengembalikan {@code FALSE} bila kolom kosong. Arah bawaan di sini
	 * berkebalikan dari {@link ApotikAkunMapping#getAktif()} dan memang
	 * seharusnya begitu: yang aman adalah menganggap sesuatu TIDAK berlaku
	 * sampai dinyatakan berlaku, sehingga baris lama yang penandanya kosong
	 * tidak tiba-tiba menuntut perhatian rantai dingin yang tidak pernah
	 * relevan baginya.</p>
	 *
	 * <p><b>Ditentukan peladen, bukan diklaim klien.</b>
	 * {@code ApotikPersediaanHelper} menelusuri sendiri profil setiap item pada
	 * faktur dan menyalakan penanda ini bila ada satu saja yang
	 * {@link ApotikItemProfile#getColdChain()}-nya benar. Klien tidak dapat
	 * memilih jawabannya. Sikap itu tepat dan patut ditiru: kalau klien yang
	 * menyatakan, maka klien yang lupa menandai akan membuat bukti suhu
	 * tampak tidak diperlukan — dan ketiadaan bukti untuk barang yang
	 * seharusnya berbukti adalah persis keadaan yang ingin dihindari.</p>
	 *
	 * <p>Perhatikan bahwa penanda ini bersifat memberi tahu, bukan menuntut:
	 * bernilai {@code true} tidak membuat {@link #getSuhuCelsius()} wajib
	 * terisi. Faktur berisi vaksin yang diterima tanpa pengukuran suhu tetap
	 * tersimpan, dengan baris bukti yang menyatakan "ada rantai dingin, suhu
	 * tidak dicatat". Bentuk itu setidaknya membuat ketiadaan pengukuran
	 * terlihat, alih-alih tak berbekas.</p>
	 *
	 * @return {@code TRUE} bila faktur memuat item rantai dingin;
	 *         {@code FALSE} bila tidak atau bila kolom kosong
	 */
	@Column(name = "ada_cold_chain")
	public Boolean getAdaColdChain() { return adaColdChain == null ? Boolean.FALSE : adaColdChain; }

	/**
	 * Menetapkan penanda muatan rantai dingin.
	 *
	 * @param adaColdChain {@code TRUE} bila faktur memuat item rantai dingin
	 */
	public void setAdaColdChain(Boolean adaColdChain) { this.adaColdChain = adaColdChain; }

	/**
	 * Catatan bebas tentang keadaan penerimaan.
	 *
	 * <p>Tempat yang tepat untuk hal-hal yang tidak muat di sebuah angka: kotak
	 * pendingin yang sudah tidak dingin saat tiba, es kering yang habis, berapa
	 * lama paket menunggu sebelum dibongkar. Justru keterangan semacam inilah
	 * yang menentukan apakah penyimpangan suhu berarti obatnya rusak, dan
	 * satu-satunya tempat penyimpanannya adalah kolom ini.</p>
	 *
	 * @return keterangan, atau {@code null}
	 */
	@Column(name = "keterangan", length = 500)
	public String getKeterangan() { return keterangan; }

	/**
	 * Menetapkan catatan keadaan penerimaan.
	 *
	 * @param keterangan keterangan
	 */
	public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

	/**
	 * Waktu pembacaan termometer dicatat.
	 *
	 * <p>Diisi pemanggil dengan waktu peladen saat penerimaan disimpan. Perlu
	 * disadari bahwa itu waktu PENCATATAN, yang belum tentu sama dengan waktu
	 * kotak dibuka dan termometer dibaca. Untuk barang rantai dingin selisih
	 * antara keduanya bermakna — obat yang menunggu berjam-jam di suhu ruang
	 * sebelum dicatat sudah tidak sama dengan yang langsung masuk pendingin —
	 * dan sistem ini tidak menyimpan waktu pembacaan yang sesungguhnya. Bila
	 * selisih itu penting, tuliskan di {@link #getKeterangan()}.</p>
	 *
	 * @return waktu pencatatan
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu", nullable = false)
	public Date getWaktu() { return waktu; }

	/**
	 * Menetapkan waktu pencatatan.
	 *
	 * @param waktu waktu pencatatan
	 */
	public void setWaktu(Date waktu) { this.waktu = waktu; }

	/**
	 * Nama tampil pelaku pencatatan (bayangan audit).
	 *
	 * <p>Pada baris bukti suhu, inilah orang yang menyatakan telah membaca
	 * termometer — bagian dari bukti itu sendiri, bukan sekadar bayangan
	 * teknis.</p>
	 *
	 * @return nama pelaku, atau {@code null}
	 */
	@Column(name = "oleh", length = 60)
	public String getOleh() { return oleh; }

	/**
	 * Menetapkan nama pelaku pencatatan.
	 *
	 * <p>Menetapkan apa adanya, termasuk nilai kosong — berbeda dari
	 * {@link ApotikNarkotikaLog#setOleh(String)} dan
	 * {@link ApotikDispensingLog#setOleh(String)} yang menolaknya. Untuk baris
	 * yang berfungsi sebagai bukti, penjagaan seperti pada kedua entity itu
	 * sebenarnya lebih pantas; yang menahan di sini hanyalah kenyataan bahwa
	 * tidak ada jalur yang menyunting baris bukti suhu setelah tersimpan,
	 * ditambah revisi Envers di
	 * {@code new_audit.apotik_penerimaan_suhu__audit}. Perbedaan ini disebut
	 * agar tidak dikira kekeliruan pembacaan.</p>
	 *
	 * @param oleh nama pelaku
	 */
	public void setOleh(String oleh) { this.oleh = oleh; }

	/**
	 * Identitas akun pelaku pencatatan (bayangan audit).
	 *
	 * @return id akun pelaku, atau {@code null}
	 */
	@Column(name = "oleh_id", length = 60)
	public String getOlehId() { return olehId; }

	/**
	 * Menetapkan id akun pelaku pencatatan.
	 *
	 * <p>Berlaku catatan pada {@link #setOleh(String)}.</p>
	 *
	 * @param olehId id akun pelaku
	 */
	public void setOlehId(String olehId) { this.olehId = olehId; }

	/**
	 * Stempel perubahan terakhir.
	 *
	 * <p>Selalu sama dengan waktu pembuatan baris: tidak ada jalur yang
	 * menyunting bukti suhu.</p>
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
}
