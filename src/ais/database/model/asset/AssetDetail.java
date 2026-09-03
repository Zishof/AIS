package ais.database.model.asset;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.text.SimpleDateFormat;
import java.util.Calendar;
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

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.action.master.asset.util.AssetUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Ruang;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.WaktuUtil;

/**
 * UNIT FISIK aset individual -- tingkat TERBAWAH dari tiga tingkat pencatatan aset tetap AIS.
 *
 * <h3>Posisi dalam hirarki tiga tingkat</h3>
 *
 * <p>Satu baris di sini adalah satu barang yang bisa dipegang tangan: satu laptop dengan
 * barcode / nomor inventaris tertentu, satu kursi, satu kendaraan. Kolom FK {@code asset}
 * menunjuk ke {@link Asset} (kepemilikan satu jenis aset oleh satu satuan kerja), dan dari sana
 * ke {@link MasterAsset} (katalog jenis barang, mis. "Laptop Dell XPS 13"). Urutan ini
 * diverifikasi dari arah kolom FK: tabel {@code asset.asset_detail} memuat kolom {@code asset},
 * bukan sebaliknya.</p>
 *
 * <p>Bahwa entitas inilah yang mewakili unit fisik -- dan bukan {@link Asset} -- ditegaskan oleh
 * tiga hal di dalam berkas ini sendiri: ia memegang {@link #getBarcode()} (nomor identitas per
 * unit), ia memegang koordinat {@link #getLat()}/{@link #getLng()} dan alamat titik pemasangan,
 * dan {@link #generateBarcode(AssetDetail, Integer, boolean)} memberi nomor urut dengan
 * MENGHITUNG jumlah baris {@code AssetDetail} yang ber-FK ke {@code Asset} yang sama. Nomor urut
 * semacam itu hanya masuk akal bila {@code Asset} adalah induk pengelompokan.</p>
 *
 * <p>{@link PenyusutanAsset} -- jadwal penyusutan per periode -- ber-FK ke entitas INI, bukan ke
 * {@code Asset}. Penyusutan karena itu dihitung per unit fisik, sesuai harga beli dan umur
 * ekonomis masing-masing unit.</p>
 *
 * <h3>Nilai perolehan: diturunkan, bukan diketik</h3>
 *
 * <p>Nilai-nilai ekonomis unit ini ({@link #getHargaBeli()}, {@link #getTanggalBeli()},
 * {@link #getNilaiMinimal()}, {@link #getUmurEkonomis()}) semuanya DITURUNKAN ULANG dari
 * dokumen asal-usul atau dari katalog setiap kali getter-nya dipanggil, lalu ditulis balik ke
 * field. Karena entitas ini memakai akses PROPERTI (anotasi {@code @Id} dipasang di getter),
 * Hibernate memanggil getter tersebut juga saat pemeriksaan perubahan, sehingga nilai hasil
 * penurunan itu TERTULIS PERMANEN ke basis data pada flush berikutnya. Uraian lengkap tiap
 * kasus ada pada dokumentasi masing-masing getter; ringkasnya, kolom-kolom ini tidak dapat
 * disunting bebas selama dokumen asal-usulnya terisi.</p>
 *
 * <p>Nilai buku dan beban penyusutan sendiri TIDAK disimpan di entitas ini. Keduanya ada di
 * {@link PenyusutanAsset} (kolom {@code nilai_penyusutan} dan {@code nilai_buku}) -- juga sebagai
 * getter yang menghitung ulang lalu menulis balik: beban per periode = harga beli dibagi umur
 * ekonomis, dan nilai buku = harga beli dikurangi (beban dikali tahun ke-n), ditambah nilai
 * minimal sebagai nilai residu.</p>
 *
 * <h3>Isolasi tenant</h3>
 *
 * <p>Kolom {@code satuan_kerja} ada LANGSUNG di sini (pola satu tingkat), tetapi nilainya
 * diturunkan dari {@code Asset} -- lihat {@link #getSatuanKerja()}. Entitas inilah satu-satunya
 * di klaster aset inti yang terdaftar pada Generic CRUD v2, melalui
 * {@code AssetDepreciationWorkflowGenericCrudAdapter} yang berstatus baca-saja; karena properti
 * {@code satuanKerja} ada dan bertipe asosiasi, penyaringan tenant otomatis adapter tersebut
 * aktif untuk entitas ini. {@link Asset}, {@link MasterAsset}, dan {@link KelompokAsset} TIDAK
 * terdaftar di Generic CRUD v2.</p>
 *
 * @see Asset induk pengelompokan per satuan kerja
 * @see MasterAsset katalog jenis barang
 * @see PenyusutanAsset jadwal penyusutan per unit
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "asset", name = "asset_detail")
public class AssetDetail extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Bernilai sama dengan entitas lain sepaket karena seluruh berkas dihasilkan hbm2java dari
	 * templat yang sama; tidak bermasalah karena nilai ini hanya dibandingkan antar-versi kelas
	 * yang sama.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama baris, kolom {@code id}; di-generate database (IDENTITY). */
	private Long id;

	/** Nama tampil pengguna terakhir yang menyunting baris ini. */
	private String oleh;

	/** Id pengguna terakhir yang menyunting baris ini. */
	private String olehId;

	/**
	 * Id pengguna terakhir yang menyunting baris ini.
	 *
	 * @return id pengguna penyunting terakhir, atau {@code null} bila belum terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna penyunting terakhir, MENGABAIKAN nilai kosong.
	 *
	 * <p>Nilai {@code null} atau berisi spasi saja tidak menimpa jejak audit lama, agar proses
	 * batch yang tidak mengenal pengguna aktif tidak menghapus riwayat yang sudah ada.</p>
	 *
	 * @param olehId id pengguna penyunting; diabaikan bila kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna penyunting terakhir, MENGABAIKAN nilai kosong.
	 *
	 * @param oleh nama pengguna penyunting; diabaikan bila kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama tampil pengguna terakhir yang menyunting baris ini.
	 *
	 * @return nama penyunting terakhir, atau {@code null} bila belum terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: memperbarui stempel waktu audit sebelum UPDATE dikirim.
	 *
	 * <p>Didelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} agar aturannya terpusat.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu penyuntingan terakhir, bernilai awal waktu server saat objek dibuat.
	 *
	 * <p>Bidang audit ini diulang di tiap entitas AIS sebagai KEHARUSAN TEKNIS: kelas induk
	 * {@link GeneralValueObject} bukan {@code @Entity} maupun {@code @MappedSuperclass}, sehingga
	 * Hibernate tidak mewarisi pemetaan kolom apa pun darinya.</p>
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu penyuntingan terakhir.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu penyuntingan terakhir baris ini.
	 *
	 * @return stempel waktu; tidak pernah {@code null} untuk objek hasil konstruktor
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks berupa nama unit apa adanya, dipakai label komponen ZK.
	 *
	 * <p>Berbeda dari {@link Asset#toString()}, di sini field {@code nama} dibaca LANGSUNG,
	 * bukan lewat getter-nya, sehingga {@code toString()} pada entitas ini tidak memicu efek
	 * samping apa pun.</p>
	 *
	 * @return nama unit apa adanya; bisa {@code null} bila belum diisi
	 */
	public String toString() {
		return nama;
	}

	/** Nomor inventaris / barcode unit; lihat {@link #generateBarcode(AssetDetail, Integer, boolean)}. */
	private String barcode;

	/** Nama unit fisik. */
	private String nama;

	/** Keterangan bebas. */
	private String keterangan;

	/** Induk pengelompokan unit ini; lihat {@link #getAsset()}. */
	private Asset asset;

	/** Status aktif / tidak aktif; DITURUNKAN dari dokumen penghapusan, lihat {@link #getStatusAsset()}. */
	private StatusAsset statusAsset;

	/** Pemilik legal unit; diwarisi dari {@link Asset} bila kosong. */
	private PemilikAsset pemilikAsset;

	/** Nilai perolehan unit; DITURUNKAN, lihat {@link #getHargaBeli()}. */
	private Double hargaBeli;

	/** Tanggal perolehan unit; DITURUNKAN, lihat {@link #getTanggalBeli()}. */
	private Date tanggalBeli;

	/** Lintang titik pemasangan / keberadaan unit (peta). */
	private Double lat;

	/** Bujur titik pemasangan / keberadaan unit (peta). */
	private Double lng;

	/** Alamat ringkas titik keberadaan unit; kolom {@code saja_alamat}. */
	private String alamat = "";

	/** Alamat rinci titik keberadaan unit; kolom {@code detail_alamat}. */
	private String detailAlamat = "";

	/** Jejak posting jurnal perolehan unit ini ke buku besar. */
	private PostingHistory postingHistory;

	/** Nilai residu / nilai sisa minimal setelah disusutkan penuh; DITURUNKAN dari katalog. */
	private Double nilaiMinimal;

	/** Umur ekonomis unit (satuan periode penyusutan); DITURUNKAN dari katalog. */
	private Double umurEkonomis;

	/** Penanda unit merupakan sarana yang dipakai bersama lintas satuan kerja. */
	private Boolean saranaBersama;

	/** Baris dokumen penghapusan yang mencakup unit ini; menentukan {@link #getStatusAsset()}. */
	private PenghapusanMasterAssetDetail penghapusanMasterAssetDetail;

	/** Lokasi (gedung/kampus) unit; diwarisi dari {@link Asset} bila kosong. */
	private Lokasi lokasi;

	/** Ruang penempatan unit; diwarisi dari {@link Asset} bila kosong. */
	private Ruang ruang;

	/** Satuan kerja pemilik; DITURUNKAN dari {@link Asset}, lihat {@link #getSatuanKerja()}. */
	private SatuanKerja satuanKerja;

	/**
	 * Tahun perolehan, dipakai pengaturan ulang nomor urut per tahun.
	 *
	 * <p><b>Perhatian:</b> lihat {@link #getTahun()} -- field ini tidak pernah terisi otomatis
	 * karena penjaga di getter-nya terbalik.</p>
	 */
	private Integer tahun;

	/**
	 * Bulan perolehan, dipakai pengaturan ulang nomor urut per bulan.
	 *
	 * <p><b>Perhatian:</b> lihat {@link #getBulan()} -- field ini tidak pernah terisi otomatis
	 * karena penjaga di getter-nya terbalik.</p>
	 */
	private Integer bulan;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate dan ZK data binding.
	 */
	public AssetDetail() {
	}

	/**
	 * Kunci utama baris.
	 *
	 * <p>{@code insertable = false} karena nilainya di-generate database.</p>
	 *
	 * @return id baris, atau {@code null} bila objek belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama; umumnya hanya dipanggil Hibernate seusai INSERT.
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama unit fisik.
	 *
	 * <p>Berbeda dari {@link Asset#getNama()}, getter ini TIDAK menyalin apa pun dari katalog:
	 * nama unit boleh berbeda dari nama jenisnya (mis. "Laptop XPS - Kaprodi"). Nilainya hanya
	 * di-{@code trim()}.</p>
	 *
	 * @return nama unit hasil {@code trim()}, atau {@code null} bila belum terisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama unit fisik.
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas untuk unit ini.
	 *
	 * <p>Mengembalikan teks kosong (bukan {@code null}) bila belum terisi, agar komponen ZK yang
	 * terikat langsung ke properti ini tidak menampilkan "null".</p>
	 *
	 * @return keterangan, atau {@code ""} bila belum terisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan == null ? "" : this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas.
	 *
	 * @param keterangan teks keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Induk pengelompokan unit ini -- baris {@link Asset} yang mewakili kepemilikan satu jenis
	 * aset oleh satu satuan kerja.
	 *
	 * <p>Dari relasi inilah hampir seluruh nilai turunan unit berasal: satuan kerja, lokasi,
	 * ruang, pemilik, harga beli, tanggal beli, umur ekonomis, dan nilai minimal. Dipetakan
	 * {@code Fetch(FetchMode.SELECT)} sehingga Hibernate menerbitkan SELECT terpisah alih-alih
	 * ikut dalam JOIN; pada daftar panjang, inilah yang membuat tiap baris memicu kueri
	 * tambahan.</p>
	 *
	 * <p>Berbeda dari relasi lain di kelas ini, getter ini TIDAK memanggil {@code check(...)},
	 * sehingga nilai yang dikembalikan bisa berupa proxy lazy yang belum teresolusi.</p>
	 *
	 * @return induk pengelompokan, atau {@code null} bila unit dicatat lepas
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "asset", nullable = true)
	public Asset getAsset() {
		return asset;
	}

	/**
	 * Menetapkan induk pengelompokan unit.
	 *
	 * @param asset induk baru, boleh {@code null}
	 */
	public void setAsset(Asset asset) {
		this.asset = asset;
	}

	/**
	 * Status aktif unit -- getter DESTRUKTIF yang MENURUNKAN status dari dokumen penghapusan.
	 *
	 * <h3>Aturan yang dijalankan</h3>
	 *
	 * <p>Setelah meresolusi proxy dengan {@code check(...)}, getter ini memeriksa
	 * {@link #getPenghapusanMasterAssetDetail()}. Bila unit ini tercantum pada suatu dokumen
	 * penghapusan aset, statusnya ditentukan oleh apakah dokumen tersebut sudah disetujui:</p>
	 *
	 * <ul>
	 * <li>dokumen penghapusan SUDAH disetujui ({@code getDisetujuiOleh() != null}) -- status
	 *     dipaksa menjadi {@code AssetUtil.TIDAK_AKTIF};</li>
	 * <li>dokumen penghapusan BELUM disetujui -- status dipaksa KEMBALI menjadi
	 *     {@code AssetUtil.AKTIF}.</li>
	 * </ul>
	 *
	 * <p>Perhatikan bahwa aturan kedua bersifat DUA ARAH: unit yang diusulkan dihapus tetapi
	 * persetujuannya kemudian dibatalkan akan otomatis aktif lagi, menimpa status apa pun yang
	 * pernah ditetapkan pengguna secara manual. Selama unit masih tertaut ke dokumen
	 * penghapusan, {@link #setStatusAsset(StatusAsset)} karena itu tidak berpengaruh -- pola
	 * flag aktif dua arah yang sudah berulang kali muncul pada entitas AIS lain.</p>
	 *
	 * <h3>Efek samping penyimpanan</h3>
	 *
	 * <p>Entitas ini memakai akses PROPERTI, sehingga Hibernate memanggil getter ini saat
	 * pemeriksaan perubahan. Status hasil penurunan karena itu tertulis permanen ke kolom
	 * {@code status_asset} pada flush berikutnya. Konsekuensi praktisnya: sekadar MEMBACA unit
	 * lewat halaman mana pun cukup untuk menyinkronkan kolom status dengan keadaan dokumen
	 * penghapusannya; unit yang belum pernah dibaca sejak dokumennya disetujui masih menyimpan
	 * status lama di basis data. Laporan yang menyaring langsung pada kolom {@code status_asset}
	 * lewat SQL karena itu bisa berbeda hasilnya dari tampilan berbasis objek.</p>
	 *
	 * <h3>Ketergantungan pada data acuan</h3>
	 *
	 * <p>{@code AssetUtil.AKTIF} dan {@code AssetUtil.TIDAK_AKTIF} bukan konstanta biasa
	 * melainkan baris {@link StatusAsset} yang dimuat -- atau DIBUAT bila belum ada -- saat kelas
	 * {@code AssetUtil} pertama kali diakses. Artinya pemanggilan getter ini pada basis data
	 * kosong dapat menimbulkan penulisan baris status baru sebagai efek samping tak langsung.</p>
	 *
	 * @return status unit hasil penurunan, atau status tersimpan bila unit tidak tertaut dokumen
	 *         penghapusan; boleh {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_asset", nullable = true)
	public StatusAsset getStatusAsset() {
		statusAsset = check(statusAsset);
		if (getPenghapusanMasterAssetDetail() != null
				&& getPenghapusanMasterAssetDetail().getPenghapusanMasterAsset().getDisetujuiOleh() != null) {
			statusAsset = AssetUtil.TIDAK_AKTIF;
		}

		if (getPenghapusanMasterAssetDetail() != null
				&& getPenghapusanMasterAssetDetail().getPenghapusanMasterAsset().getDisetujuiOleh() == null) {
			statusAsset = AssetUtil.AKTIF;
		}

		return statusAsset;
	}

	/**
	 * Menetapkan status unit.
	 *
	 * <p>Nilai yang diisi di sini akan DITIMPA oleh {@link #getStatusAsset()} selama unit masih
	 * tertaut ke dokumen penghapusan.</p>
	 *
	 * @param statusAsset status baru, boleh {@code null}
	 */
	public void setStatusAsset(StatusAsset statusAsset) {
		this.statusAsset = statusAsset;
	}

	/**
	 * Nomor inventaris / barcode unit.
	 *
	 * <p>Nilainya TIDAK dihitung di getter ini; ia hanya dibaca apa adanya. Pembuatannya
	 * dilakukan pemanggil secara eksplisit lewat
	 * {@link #generateBarcode(AssetDetail, Integer, boolean)} lalu disimpan dengan
	 * {@link #setBarcode(String)}.</p>
	 *
	 * @return barcode unit, atau {@code null} bila belum pernah dibuatkan
	 */
	public String getBarcode() {
		return barcode;
	}

	/**
	 * Mengisi nomor inventaris / barcode unit.
	 *
	 * @param barcode barcode baru
	 */
	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	/**
	 * Lintang titik keberadaan unit, untuk penandaan di peta.
	 *
	 * @return lintang, atau {@code null} bila belum dipetakan
	 */
	public Double getLat() {
		return lat;
	}

	/**
	 * Mengisi lintang titik keberadaan unit.
	 *
	 * @param lat lintang baru
	 */
	public void setLat(Double lat) {
		this.lat = lat;
	}

	/**
	 * Bujur titik keberadaan unit, untuk penandaan di peta.
	 *
	 * @return bujur, atau {@code null} bila belum dipetakan
	 */
	public Double getLng() {
		return lng;
	}

	/**
	 * Mengisi bujur titik keberadaan unit.
	 *
	 * @param lng bujur baru
	 */
	public void setLng(Double lng) {
		this.lng = lng;
	}

	/**
	 * Alamat ringkas titik keberadaan unit.
	 *
	 * <p>Dipetakan ke kolom {@code saja_alamat} (bukan {@code alamat}) bertipe {@code text};
	 * nama kolom yang tak lazim ini perlu diingat saat menulis kueri SQL langsung.</p>
	 *
	 * @return alamat ringkas; bernilai awal {@code ""}
	 */
	@Column(name = "saja_alamat", columnDefinition = "text")
	public String getAlamat() {
		return alamat;
	}

	/**
	 * Mengisi alamat ringkas titik keberadaan unit.
	 *
	 * @param alamat alamat baru
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Alamat rinci titik keberadaan unit (kolom {@code detail_alamat}, bertipe {@code text}).
	 *
	 * @return alamat rinci; bernilai awal {@code ""}
	 */
	@Column(name = "detail_alamat", columnDefinition = "text")
	public String getDetailAlamat() {
		return detailAlamat;
	}

	/**
	 * Mengisi alamat rinci titik keberadaan unit.
	 *
	 * @param detailAlamat alamat rinci baru
	 */
	public void setDetailAlamat(String detailAlamat) {
		this.detailAlamat = detailAlamat;
	}

	/**
	 * Nilai perolehan unit -- getter DESTRUKTIF yang MENURUNKAN ULANG harga dari katalog dan
	 * dari dokumen asal-usul.
	 *
	 * <h3>Urutan penentuan nilai</h3>
	 *
	 * <p>Getter ini mengubah field {@link #hargaBeli} melalui empat tahap berurutan, dan tahap
	 * yang belakangan MENIMPA hasil tahap sebelumnya:</p>
	 *
	 * <ol>
	 * <li><b>Normalisasi.</b> {@code null} diganti {@code 0.0}, sehingga getter ini tidak pernah
	 *     mengembalikan {@code null} dan pemanggil boleh langsung membandingkannya secara
	 *     numerik;</li>
	 * <li><b>Harga default katalog.</b> Bila harga tersimpan masih di bawah {@code 0.01} dan
	 *     {@code asset.masterAsset.hargaBeliDefault} lebih dari {@code 0.1}, harga default
	 *     katalog dipakai. Ambang {@code 0.01} berperan sebagai "dianggap belum diisi" -- pola
	 *     yang berulang di seluruh modul aset karena tipe kolomnya {@code Double} sehingga
	 *     perbandingan dengan nol persis tidak dapat diandalkan;</li>
	 * <li><b>Jalur pengadaan.</b> Bila unit berasal dari alur pengadaan dan rantai
	 *     {@code asset -> permintaanPengadaanMasterAssetDetail -> uangMuka -> pertangungjawaban}
	 *     sudah memiliki tanggal persetujuan, harga diambil dari NILAI PERTANGGUNGJAWABAN uang
	 *     muka tersebut. Jadi yang dicatat sebagai nilai perolehan adalah nilai yang benar-benar
	 *     dipertanggungjawabkan, bukan nilai yang semula diminta;</li>
	 * <li><b>Jalur penerimaan pengadaan / saldo awal.</b> Bila rantai
	 *     {@code asset -> saldoAwalMasterAssetDetail -> penerimaanPengadaanMasterAssetDetail ->
	 *     penerimaanPengadaanMasterAsset} sudah memiliki tanggal persetujuan, harga diambil dari
	 *     {@code saldoAwalMasterAssetDetail.getHarga()}.</li>
	 * </ol>
	 *
	 * <p>Tahap 3 dan 4 saling eksklusif ({@code if / else if}), dengan jalur pengadaan
	 * berprioritas lebih tinggi -- urutan yang sama dengan {@link Asset#getSatuanKerja()},
	 * sehingga pemilik dan nilai perolehan selalu berasal dari dokumen yang sama.</p>
	 *
	 * <h3>Perlakuan PPN</h3>
	 *
	 * <p>Pada tahap 4, harga yang dipakai adalah DPP (dasar pengenaan pajak) saja. Baris yang
	 * menambahkan PPN ada di berkas ini dalam keadaan DIKOMENTARI, dan variabel
	 * {@code hargaTotal} kini sekadar menyalin {@code dpp}. Artinya, secara sengaja, nilai
	 * perolehan aset dari jalur penerimaan pengadaan TIDAK mengapitalisasi PPN. Siapa pun yang
	 * membandingkan nilai aset dengan nilai tagihan vendor harus memperhitungkan selisih ini;
	 * jangan "memperbaiki" baris yang dikomentari itu tanpa memeriksa kebijakan akuntansi yang
	 * berlaku, karena mengaktifkannya akan mengubah seluruh dasar penyusutan yang sudah berjalan.</p>
	 *
	 * <h3>Efek samping penyimpanan dan dampaknya pada penyusutan</h3>
	 *
	 * <p>Seperti getter turunan lain di kelas ini, hasil perhitungan ditulis ke field, dan karena
	 * entitas memakai akses PROPERTI, Hibernate ikut memanggil getter ini saat pemeriksaan
	 * perubahan sehingga nilainya TERSIMPAN PERMANEN pada flush berikutnya. Ini punya akibat
	 * berantai yang perlu disadari: {@link PenyusutanAsset#getNilaiPenyusutan()} menghitung beban
	 * per periode sebagai {@code hargaBeli / umurEkonomis}, dan
	 * {@link PenyusutanAsset#getNilaiBuku()} menghitung nilai buku sebagai
	 * {@code hargaBeli - (beban * tahunKe) + nilaiMinimal}. Keduanya membaca lewat getter ini,
	 * bukan lewat kolom tersimpan. Bila harga default pada katalog atau nilai pertanggungjawaban
	 * pada dokumen pengadaan diubah, SELURUH jadwal penyusutan unit ikut berubah -- termasuk
	 * untuk periode yang sudah lewat dan sudah pernah diposting ke buku besar. Baris
	 * {@link PenyusutanAsset} yang sudah punya {@code postingHistory} tetap menunjuk jurnal
	 * lamanya, sehingga nilai yang ditampilkan bisa tidak lagi cocok dengan nilai yang terlanjur
	 * dijurnal. Setiap koreksi harga pada aset yang penyusutannya sudah diposting karena itu
	 * perlu diikuti pemeriksaan ulang jurnal terkait.</p>
	 *
	 * <h3>Biaya akses</h3>
	 *
	 * <p>Rantai terpanjang menyentuh lima entitas, dan {@code asset} sendiri dipetakan
	 * {@code Fetch(FetchMode.SELECT)}. Memanggil getter ini untuk tiap baris pada daftar aset
	 * yang panjang menghasilkan rentetan SELECT per baris; bila laporan aset terasa lambat,
	 * di sinilah tempat pertama yang perlu diperiksa.</p>
	 *
	 * @return nilai perolehan unit; tidak pernah {@code null}, minimal {@code 0.0}
	 */
	public Double getHargaBeli() {
		if (hargaBeli == null) {
			hargaBeli = 0.0;
		}
		if (hargaBeli < 0.01 && asset != null && asset.getMasterAsset() != null
				&& asset.getMasterAsset().getHargaBeliDefault() > 0.1) {
			hargaBeli = asset.getMasterAsset().getHargaBeliDefault();
		}

		if (getAsset() != null && asset.getPermintaanPengadaanMasterAssetDetail() != null
				&& asset.getPermintaanPengadaanMasterAssetDetail().getUangMuka() != null
				&& asset.getPermintaanPengadaanMasterAssetDetail().getUangMuka().getPertangungjawaban() != null
				&& asset.getPermintaanPengadaanMasterAssetDetail().getUangMuka().getPertangungjawaban()
						.getTanggalPersetujuan() != null) {
			hargaBeli = asset.getPermintaanPengadaanMasterAssetDetail().getUangMuka().getPertangungjawaban().getNilai();
		} else if (getAsset() != null && asset.getSaldoAwalMasterAssetDetail() != null
				&& asset.getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail() != null
				&& asset.getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail()
						.getPenerimaanPengadaanMasterAsset() != null
				&& asset.getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail()
						.getPenerimaanPengadaanMasterAsset().getTanggalPersetujuan() != null) {

			Double dpp = (asset.getSaldoAwalMasterAssetDetail().getHarga());
//			Double ppn = ((asset.getSaldoAwalMasterAssetDetail().getPersenPpn() / 100.0) * dpp);
			Double hargaTotal = (dpp);

			hargaBeli = hargaTotal;
		}

		return hargaBeli;
	}

	/**
	 * Menetapkan nilai perolehan unit.
	 *
	 * <p>Nilai yang diisi di sini akan DITIMPA oleh {@link #getHargaBeli()} bila unit tertaut ke
	 * dokumen pengadaan atau penerimaan yang sudah disetujui.</p>
	 *
	 * @param hargaBeli nilai perolehan baru
	 */
	public void setHargaBeli(Double hargaBeli) {
		this.hargaBeli = hargaBeli;
	}

	/**
	 * Nilai residu (nilai sisa minimal) unit -- getter DESTRUKTIF yang mewarisi nilai dari
	 * katalog.
	 *
	 * <p>Bila nilai tersimpan masih di bawah {@code 0.01} (dianggap belum diisi) dan katalog
	 * {@code asset.masterAsset.nilaiMinimal} lebih dari {@code 0.1}, nilai katalog disalin ke
	 * field ini. Karena entitas memakai akses PROPERTI, salinan itu tertulis permanen pada flush
	 * berikutnya; sesudah itu unit tidak lagi mengikuti perubahan nilai minimal katalognya.</p>
	 *
	 * <p>Nilai ini adalah dasar bawah penyusutan: {@link PenyusutanAsset#getNilaiBuku()}
	 * menambahkannya kembali setelah beban penyusutan dikurangkan, sehingga nilai buku unit
	 * tidak pernah turun di bawah nilai residu ini.</p>
	 *
	 * @return nilai residu; {@code 1.0} bila tidak ada nilai tersimpan maupun nilai katalog
	 */
	public Double getNilaiMinimal() {

		if ((nilaiMinimal == null || nilaiMinimal < 0.01) && asset != null && asset.getMasterAsset() != null
				&& asset.getMasterAsset().getNilaiMinimal() > 0.1) {
			nilaiMinimal = asset.getMasterAsset().getNilaiMinimal();
		}

		return nilaiMinimal == null ? 1.0 : nilaiMinimal;
	}

	/**
	 * Menetapkan nilai residu unit.
	 *
	 * @param nilaiMinimal nilai residu baru
	 */
	public void setNilaiMinimal(Double nilaiMinimal) {
		this.nilaiMinimal = nilaiMinimal;
	}

	/**
	 * Umur ekonomis unit -- getter DESTRUKTIF yang mewarisi umur dari katalog.
	 *
	 * <p>Bila nilai tersimpan masih di bawah {@code 0.01} dan katalog
	 * {@code asset.masterAsset.umurEkonomis} lebih dari {@code 0.1}, umur katalog disalin ke
	 * field ini lalu tersimpan permanen pada flush berikutnya. Perlu dicatat bahwa
	 * {@link MasterAsset#getUmurEkonomis()} sendiri juga menurunkan nilainya dari
	 * {@link KelompokAsset#getEstimasiUmurPakai()}, sehingga umur ekonomis sebuah unit dapat
	 * berasal dari tiga tingkat di atasnya.</p>
	 *
	 * <p>Nilai ini menjadi PEMBAGI pada perhitungan beban penyusutan di
	 * {@link PenyusutanAsset#getNilaiPenyusutan()}. Nilai {@code 0.0} yang dikembalikan saat
	 * belum ada data bukan sekadar nilai kosong: {@code PenyusutanAsset} menjaga diri dengan
	 * syarat {@code umurEkonomis > 0.1} sebelum membagi, sehingga unit tanpa umur ekonomis
	 * tidak akan menghasilkan beban penyusutan sama sekali -- bukan melempar kesalahan, melainkan
	 * diam-diam bernilai nol.</p>
	 *
	 * @return umur ekonomis unit; {@code 0.0} bila tidak ada nilai tersimpan maupun nilai katalog
	 */
	public Double getUmurEkonomis() {
		if ((umurEkonomis == null || umurEkonomis < 0.01) && asset != null && asset.getMasterAsset() != null
				&& asset.getMasterAsset().getUmurEkonomis() > 0.1) {
			umurEkonomis = asset.getMasterAsset().getUmurEkonomis();
		}

		return umurEkonomis == null ? 0.0 : umurEkonomis;
	}

	/**
	 * Menetapkan umur ekonomis unit.
	 *
	 * @param umurEkonomis umur ekonomis baru
	 */
	public void setUmurEkonomis(Double umurEkonomis) {
		this.umurEkonomis = umurEkonomis;
	}

	/**
	 * Tanggal perolehan unit -- getter DESTRUKTIF yang MENURUNKAN tanggal dari dokumen asal-usul.
	 *
	 * <h3>Urutan penentuan</h3>
	 *
	 * <ol>
	 * <li><b>Jalur pengadaan.</b> Bila rantai {@code asset ->
	 *     permintaanPengadaanMasterAssetDetail -> uangMuka -> pertangungjawaban} memiliki tanggal
	 *     persetujuan, tanggal itulah yang dipakai. Tanggal perolehan aset dengan demikian sama
	 *     dengan tanggal DISETUJUINYA pertanggungjawaban uang muka, bukan tanggal barang tiba;</li>
	 * <li><b>Jalur penerimaan pengadaan.</b> Bila rantai {@code asset ->
	 *     saldoAwalMasterAssetDetail -> penerimaanPengadaanMasterAssetDetail ->
	 *     penerimaanPengadaanMasterAsset} memiliki tanggal pembuatan, tanggal PEMBUATAN dokumen
	 *     penerimaan itulah yang dipakai. Perhatikan ketidaksimetrisan yang disengaja di sini:
	 *     jalur pertama memakai tanggal PERSETUJUAN, jalur kedua memakai tanggal PEMBUATAN;</li>
	 * <li><b>Cadangan.</b> Bila keduanya kosong dan tidak ada nilai tersimpan, getter
	 *     mengembalikan WAKTU SEKARANG.</li>
	 * </ol>
	 *
	 * <h3>Akibat penting dari nilai cadangan</h3>
	 *
	 * <p>Nilai cadangan "waktu sekarang" dihitung pada baris {@code return} dan TIDAK ditulis ke
	 * field, sehingga getter ini tidak pernah mengembalikan {@code null} sekalipun basis datanya
	 * kosong. Dua akibatnya perlu diketahui pemanggil:</p>
	 *
	 * <p>Pertama, untuk aset yang dicatat manual tanpa dokumen asal-usul dan tanpa tanggal beli
	 * tersimpan, umur aset selalu terlihat nol -- tanggal perolehannya bergeser mengikuti hari
	 * ini setiap kali dibaca. {@link PenyusutanAsset#getPerTanggal()} menghitung tanggal tiap
	 * periode penyusutan dengan menggeser {@code tanggalBeli} sebanyak {@code tahunKe} bulan,
	 * sehingga jadwal penyusutan unit semacam itu ikut bergeser tiap kali dibuka.</p>
	 *
	 * <p>Kedua -- dan inilah yang menjelaskan cacat pada {@link #getTahun()} dan
	 * {@link #getBulan()} -- karena getter ini TIDAK PERNAH mengembalikan {@code null},
	 * penjaga {@code if (getTanggalBeli() == null)} pada kedua getter tersebut tidak akan pernah
	 * benar. Lihat catatan lengkapnya di sana.</p>
	 *
	 * @return tanggal perolehan hasil penurunan, nilai tersimpan, atau waktu sekarang sebagai
	 *         cadangan; tidak pernah {@code null}
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalBeli() {

		if (getAsset() != null && asset.getPermintaanPengadaanMasterAssetDetail() != null
				&& asset.getPermintaanPengadaanMasterAssetDetail().getUangMuka() != null
				&& asset.getPermintaanPengadaanMasterAssetDetail().getUangMuka().getPertangungjawaban() != null
				&& asset.getPermintaanPengadaanMasterAssetDetail().getUangMuka().getPertangungjawaban()
						.getTanggalPersetujuan() != null) {
			tanggalBeli = asset.getPermintaanPengadaanMasterAssetDetail().getUangMuka().getPertangungjawaban()
					.getTanggalPersetujuan();
		} else if (getAsset() != null && asset.getSaldoAwalMasterAssetDetail() != null
				&& asset.getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail() != null
				&& asset.getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail()
						.getPenerimaanPengadaanMasterAsset() != null
				&& asset.getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail()
						.getPenerimaanPengadaanMasterAsset().getTanggalPembuatan() != null) {
			tanggalBeli = asset.getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail()
					.getPenerimaanPengadaanMasterAsset().getTanggalPembuatan();
		}

		return tanggalBeli == null ? ais.ui.util.WaktuUtil.getDate() : tanggalBeli;
	}

	/**
	 * Menetapkan tanggal perolehan unit.
	 *
	 * <p>Nilai yang diisi akan DITIMPA oleh {@link #getTanggalBeli()} bila unit tertaut ke
	 * dokumen pengadaan atau penerimaan.</p>
	 *
	 * @param tanggalBeli tanggal perolehan baru
	 */
	public void setTanggalBeli(Date tanggalBeli) {
		this.tanggalBeli = tanggalBeli;
	}

	/**
	 * Jejak posting jurnal perolehan unit ini ke buku besar.
	 *
	 * <p>Terisi setelah aset diposting; {@code null} berarti perolehan unit belum dijurnal.
	 * Dipetakan {@code Fetch(FetchMode.SELECT)} dan tidak melewati {@code check(...)}.</p>
	 *
	 * @return jejak posting, atau {@code null} bila belum diposting
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * Menetapkan jejak posting jurnal perolehan unit.
	 *
	 * @param postingHistory jejak posting, boleh {@code null}
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

	/**
	 * Baris dokumen penghapusan aset yang mencakup unit ini.
	 *
	 * <p>Relasi inilah yang dibaca {@link #getStatusAsset()} untuk memaksa status aktif atau
	 * tidak aktif. Dipetakan {@code Fetch(FetchMode.SELECT)} dan tidak melewati
	 * {@code check(...)}.</p>
	 *
	 * @return baris dokumen penghapusan, atau {@code null} bila unit tidak sedang diusulkan hapus
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penghapusan_master_asset_detail", nullable = true)
	public PenghapusanMasterAssetDetail getPenghapusanMasterAssetDetail() {
		return penghapusanMasterAssetDetail;
	}

	/**
	 * Menetapkan baris dokumen penghapusan yang mencakup unit ini.
	 *
	 * @param penghapusanMasterAssetDetail baris dokumen penghapusan, boleh {@code null}
	 */
	public void setPenghapusanMasterAssetDetail(PenghapusanMasterAssetDetail penghapusanMasterAssetDetail) {
		this.penghapusanMasterAssetDetail = penghapusanMasterAssetDetail;
	}

	/**
	 * Penanda unit merupakan sarana yang dipakai bersama lintas satuan kerja.
	 *
	 * <p>Mengembalikan {@code false} bila belum pernah ditetapkan, sehingga pemanggil boleh
	 * langsung memakainya sebagai {@code boolean} tanpa memeriksa {@code null}.</p>
	 *
	 * @return {@code true} bila unit ditandai sebagai sarana bersama
	 */
	public Boolean getSaranaBersama() {
		return saranaBersama == null ? false : saranaBersama;
	}

	/**
	 * Menetapkan penanda sarana bersama.
	 *
	 * @param saranaBersama {@code true} bila unit dipakai bersama lintas satuan kerja
	 */
	public void setSaranaBersama(Boolean saranaBersama) {
		this.saranaBersama = saranaBersama;
	}

	/**
	 * Lokasi (gedung / kampus) unit -- mewarisi dari induk bila kosong.
	 *
	 * <p>Berbeda dari getter turunan lain di kelas ini, pewarisan di sini bersifat SATU ARAH dan
	 * hanya mengisi kekosongan: lokasi induk disalin HANYA bila lokasi unit masih {@code null}.
	 * Unit yang lokasinya sudah ditetapkan sendiri -- misalnya sesudah mutasi lokasi -- tidak
	 * akan ditarik kembali ke lokasi induknya. Salinan itu tetap tersimpan permanen pada flush
	 * berikutnya karena entitas memakai akses PROPERTI.</p>
	 *
	 * @return lokasi unit, lokasi induk sebagai warisan, atau {@code null} bila keduanya kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi", nullable = true)
	public Lokasi getLokasi() {
		lokasi = check(lokasi);

		if (lokasi == null && asset != null && asset.getLokasi() != null) {
			lokasi = asset.getLokasi();
		}

		return lokasi;
	}

	/**
	 * Menetapkan lokasi unit.
	 *
	 * @param lokasi lokasi baru, boleh {@code null}
	 */
	public void setLokasi(Lokasi lokasi) {
		this.lokasi = lokasi;
	}

	/**
	 * Ruang penempatan unit -- mewarisi dari induk bila kosong.
	 *
	 * <p>Pewarisan satu arah yang sama dengan {@link #getLokasi()}: ruang induk disalin hanya
	 * bila ruang unit masih {@code null}.</p>
	 *
	 * @return ruang unit, ruang induk sebagai warisan, atau {@code null} bila keduanya kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ruang", nullable = true)
	public Ruang getRuang() {
		ruang = check(ruang);

		if (ruang == null && asset != null && asset.getRuang() != null) {
			ruang = asset.getRuang();
		}

		return this.ruang;
	}

	/**
	 * Menetapkan ruang penempatan unit.
	 *
	 * @param ruang ruang baru, boleh {@code null}
	 */
	public void setRuang(Ruang ruang) {
		this.ruang = ruang;
	}

	/**
	 * Satuan kerja pemilik unit -- getter DESTRUKTIF yang MENYELARASKAN paksa dengan induk.
	 *
	 * <h3>Perbedaan penting dari lokasi dan ruang</h3>
	 *
	 * <p>Berbeda dari {@link #getLokasi()} dan {@link #getRuang()} yang hanya mengisi kekosongan,
	 * pewarisan di sini TANPA SYARAT: begitu {@code asset.getSatuanKerja()} tidak {@code null},
	 * nilainya menimpa apa pun yang tersimpan di unit -- termasuk nilai yang baru saja
	 * ditetapkan lewat {@link #setSatuanKerja(SatuanKerja)}. Satuan kerja unit karena itu tidak
	 * dapat berbeda dari satuan kerja induknya; satu-satunya cara memindahkannya adalah
	 * memindahkan barisan {@link Asset}-nya.</p>
	 *
	 * <h3>Rantai penurunan yang sebenarnya</h3>
	 *
	 * <p>Karena {@link Asset#getSatuanKerja()} sendiri juga menurunkan nilainya dari rantai
	 * dokumen pengadaan atau saldo awal, pemilik sebuah unit fisik sesungguhnya ditentukan dua
	 * lapis: dokumen asal-usul menentukan pemilik {@code Asset}, lalu {@code Asset} menentukan
	 * pemilik unit. Mengoreksi satuan kerja pada dokumen pengadaan akan memindahkan tenant
	 * seluruh unit turunannya secara diam-diam pada akses berikutnya.</p>
	 *
	 * <h3>Kaitan dengan penyaringan tenant</h3>
	 *
	 * <p>Nilai hasil penurunan ini tersimpan permanen pada flush berikutnya (entitas memakai
	 * akses PROPERTI). Itu menguntungkan bagi penyaringan tenant Generic CRUD v2: adapter
	 * {@code AssetDepreciationWorkflowGenericCrudAdapter} menambahkan pembatas
	 * {@code Restrictions.eq("satuanKerja", ...)} pada level SQL, dan pembatas itu membaca kolom
	 * TERSIMPAN, bukan nilai terhitung. Baris yang belum pernah tersentuh flush sejak dokumen
	 * asalnya berubah karena itu masih tersaring memakai pemilik lamanya. Selisih antara nilai
	 * tersimpan dan nilai terhitung inilah yang perlu diingat saat hasil penyaringan terasa
	 * tidak konsisten dengan tampilan.</p>
	 *
	 * @return satuan kerja pemilik unit; mengikuti induk bila induknya punya pemilik
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);

		if (asset != null && asset.getSatuanKerja() != null) {
			satuanKerja = asset.getSatuanKerja();
		}

		return satuanKerja;
	}

	/**
	 * Menetapkan satuan kerja pemilik unit.
	 *
	 * <p>Nilai yang diisi akan DITIMPA TANPA SYARAT oleh {@link #getSatuanKerja()} bila induknya
	 * punya satuan kerja.</p>
	 *
	 * @param satuanKerja satuan kerja baru, boleh {@code null}
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Pemilik legal unit -- mewarisi dari induk bila kosong.
	 *
	 * <p>Pewarisan satu arah seperti {@link #getLokasi()}: pemilik induk disalin hanya bila
	 * pemilik unit masih {@code null}, sehingga unit yang kepemilikannya ditetapkan khusus
	 * (mis. barang milik pihak ketiga di antara barang milik sendiri) tetap terjaga.</p>
	 *
	 * @return pemilik unit, pemilik induk sebagai warisan, atau {@code null} bila keduanya kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pemilik_asset", nullable = true)
	public PemilikAsset getPemilikAsset() {
		pemilikAsset = check(pemilikAsset);

		if (pemilikAsset == null && asset != null && asset.getPemilikAsset() != null) {
			pemilikAsset = asset.getPemilikAsset();
		}

		return pemilikAsset;
	}

	/**
	 * Menetapkan pemilik legal unit.
	 *
	 * @param pemilikAsset pemilik baru, boleh {@code null}
	 */
	public void setPemilikAsset(PemilikAsset pemilikAsset) {
		this.pemilikAsset = pemilikAsset;
	}

	/**
	 * Pemformat bulan-tahun dua digit ({@code MMyy}) untuk potongan tanggal pada barcode.
	 *
	 * <p>Dibungkus {@link ThreadLocal} karena {@link SimpleDateFormat} TIDAK aman-thread: satu
	 * instance bersama yang dipakai beberapa permintaan HTTP sekaligus dapat menghasilkan teks
	 * kacau atau melempar kesalahan pengurai. Pola pembungkusan ini dipakai konsisten di seluruh
	 * AIS ({@code Common.dateFormat8} dan kerabatnya).</p>
	 */
	public static final ThreadLocal<SimpleDateFormat> dateFormat = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("MMyy");
		}
	};

	/**
	 * Membuat nomor inventaris / barcode untuk satu unit fisik, memilih sendiri salah satu dari
	 * TIGA strategi penomoran.
	 *
	 * <h3>Strategi 1 -- barcode terstruktur bawaan modul aset</h3>
	 *
	 * <p>Dipakai bila unit punya induk dan katalog, tetapi kelompok asetnya belum ditetapkan
	 * ATAU kelompok itu tidak memiliki templat {@link NomorSurat}. Hasilnya berupa rangkaian
	 * lima bagian yang dipisah titik:</p>
	 *
	 * <pre>&lt;kode satuan kerja&gt;.&lt;kode katalog&gt;.&lt;nomor urut 3 digit&gt;.&lt;MMyy&gt;.&lt;penanda&gt;</pre>
	 *
	 * <p>Rinciannya: kode satuan kerja dan kode katalog masing-masing DILEWATI (beserta titik
	 * pemisahnya) bila kosong, sehingga barcode dapat berakhir lebih pendek dari bentuk penuh di
	 * atas. Nomor urut diambil dengan MENGHITUNG jumlah baris {@code AssetDetail} yang ber-FK ke
	 * {@code Asset} yang sama, lalu ditambah satu -- inilah bukti struktural bahwa {@code Asset}
	 * berperan sebagai induk pengelompokan unit. Hasil hitungan dipadkan menjadi tepat tiga digit
	 * dengan mengambil tiga karakter terakhir dari teks yang sudah didahului nol; perlu disadari
	 * bahwa pemadan ini MEMBUANG digit berlebih, sehingga unit ke-1000 dan seterusnya akan
	 * mengulang pola "000", "001", dan seterusnya. Bagian {@code MMyy} adalah bulan dan dua digit
	 * tahun saat barcode dibuat -- bukan saat aset diperoleh. Penanda terakhir bernilai
	 * {@code "0"} bila kelompok aset kosong, {@code "1"} bila kelompok ditandai sebagai aset
	 * tetap, dan {@code "2"} bila bukan.</p>
	 *
	 * <p>Penomoran ini bersifat RENTAN BALAP: hitungan baris dan penyimpanan unit baru terjadi di
	 * transaksi berbeda, sehingga dua pengguna yang menambah unit pada {@code Asset} yang sama
	 * secara bersamaan dapat memperoleh nomor urut identik. Kolom {@code barcode} sendiri tidak
	 * dideklarasikan unik, jadi tabrakan semacam itu tidak akan ditolak basis data. Parameter
	 * {@code nomorUrutManual} disediakan justru untuk kasus pembuatan massal, di mana pemanggil
	 * mengendalikan sendiri urutan nomor alih-alih menghitung ulang per unit.</p>
	 *
	 * <h3>Strategi 2 -- mengikuti templat penomoran surat</h3>
	 *
	 * <p>Dipakai bila kelompok aset unit ini memiliki {@link NomorSurat}. Nomor indeks diambil
	 * dari {@code nomorSurat.getNomorIndex()} bila templat ditandai memakai indeks urut sendiri,
	 * atau dihitung lewat {@link #getindex(NomorSurat)} bila tidak. Bila argumen {@code tambah}
	 * bernilai {@code true}, indeks pada templat DINAIKKAN sebagai efek samping melalui
	 * {@code NomorSurat.tambahIndexNomorSurat} -- karena itu pemanggil yang hanya ingin
	 * MELIHAT calon nomor tanpa memakainya wajib memanggil dengan {@code tambah = false},
	 * agar tidak membakar satu nomor secara sia-sia. Hasil akhirnya diformat oleh templat
	 * bersangkutan dengan tanggal hari ini.</p>
	 *
	 * <h3>Strategi 3 -- cadangan acak</h3>
	 *
	 * <p>Bila unit tidak punya induk atau induknya tidak punya katalog, dikembalikan barcode acak
	 * dari {@code Common.getGeneratedBarCode()}. Ini menjamin metode tidak pernah gagal, tetapi
	 * juga berarti unit yatim memperoleh nomor yang tidak membawa informasi apa pun.</p>
	 *
	 * <h3>Catatan bagi pemanggil</h3>
	 *
	 * <p>Metode ini tidak menyimpan apa pun ke unit yang diberikan; pemanggil harus sendiri
	 * memanggil {@link #setBarcode(String)}. Ia juga tidak memeriksa apakah barcode hasilnya
	 * sudah dipakai unit lain. Pemanggil di modul aset ({@code AssetAction},
	 * {@code AssetDetailAction}, {@code SaldoAwalMasterAssetDetailAction}) memakainya persis
	 * dengan pola {@code detail.setBarcode(AssetDetail.generateBarcode(detail, ..., true))}.</p>
	 *
	 * @param assetDetail   unit yang akan diberi nomor; boleh {@code null}, akan jatuh ke
	 *                      strategi cadangan
	 * @param nomorUrutManual nomor urut yang dipaksakan pemanggil pada strategi 1; {@code null}
	 *                      berarti dihitung sendiri dari jumlah unit sekelompok
	 * @param tambah        pada strategi 2, {@code true} menaikkan indeks templat nomor surat
	 *                      sebagai efek samping; {@code false} hanya membaca
	 * @return barcode hasil salah satu dari tiga strategi; tidak pernah {@code null}
	 */
	public static String generateBarcode(AssetDetail assetDetail, Integer nomorUrutManual, boolean tambah) {
		if (assetDetail != null && assetDetail.getAsset() != null && assetDetail.getAsset().getMasterAsset() != null
				&& (assetDetail.getAsset().getMasterAsset().getKelompokAsset() == null
						|| assetDetail.getAsset().getMasterAsset().getKelompokAsset().getNomorSurat() == null)) {
			Integer max;
			if (nomorUrutManual == null) {
				Session session = HibernateUtil.currentSession();
				max = ((Number) session.createCriteria(AssetDetail.class)
						.add(assetDetail.getAsset() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("asset", assetDetail.getAsset()))
						.setProjection(Projections.rowCount()).uniqueResult()).intValue();

				if (max.equals(0)) {
					max = 1;
				} else {
					max++;
				}
			} else {
				max = nomorUrutManual;
			}
			String kode = "00000000000000000000000" + max;

			Calendar calendar = Calendar.getInstance();

			return (assetDetail.getSatuanKerja() == null ? "" : assetDetail.getSatuanKerja().getKode() + ".")
					+ (assetDetail.getAsset().getMasterAsset().getKode() == null ? ""
							: assetDetail.getAsset().getMasterAsset().getKode() + ".")
					+ kode.substring(kode.length() - 3, kode.length()) + "." + dateFormat.get().format(calendar.getTime())
					+ "."
					+ (assetDetail.getAsset().getMasterAsset().getKelompokAsset() == null ? "0"
							: assetDetail.getAsset().getMasterAsset().getKelompokAsset().getMerupakanAssetFix() ? "1"
									: "2");
		} else if (assetDetail != null && assetDetail.getAsset() != null
				&& assetDetail.getAsset().getMasterAsset() != null
				&& assetDetail.getAsset().getMasterAsset().getKelompokAsset() != null
				&& assetDetail.getAsset().getMasterAsset().getKelompokAsset().getNomorSurat() != null) {
			Long index = assetDetail.getAsset().getMasterAsset().getKelompokAsset().getNomorSurat()
					.getGunakanIndexUrut()
							? assetDetail.getAsset().getMasterAsset().getKelompokAsset().getNomorSurat().getNomorIndex()
							: getindex(assetDetail.getAsset().getMasterAsset().getKelompokAsset().getNomorSurat());
			if (tambah) {
				NomorSurat.tambahIndexNomorSurat(
						assetDetail.getAsset().getMasterAsset().getKelompokAsset().getNomorSurat());
			}
			String noAgenda = assetDetail.getAsset().getMasterAsset().getKelompokAsset().getNomorSurat().format(index,
					WaktuUtil.getDate());
			return noAgenda;
		} else {
			return Common.getGeneratedBarCode();
		}
	}

	/**
	 * Menghitung nomor urut berikutnya bagi templat {@link NomorSurat} tertentu, dengan cara
	 * MENGHITUNG unit aset yang sudah memakai templat itu.
	 *
	 * <h3>Cara kerja</h3>
	 *
	 * <p>Metode membangun satu {@code Criteria} atas {@code AssetDetail} dengan empat
	 * {@code LEFT JOIN} berantai -- {@code asset}, {@code masterAsset}, {@code kelompokAsset},
	 * {@code nomorSurat} -- lalu menambahkan empat pembatas yang masing-masing bisa berupa
	 * pembatas nyata atau {@code sqlRestriction("true")} (artinya: pembatas dinonaktifkan),
	 * bergantung pada pengaturan templat:</p>
	 *
	 * <ol>
	 * <li><b>Lingkup penghitungan.</b> Bila templat diurutkan berdasarkan nomor, hanya unit yang
	 *     kelompok asetnya memakai templat YANG SAMA yang dihitung. Bila diurutkan berdasarkan
	 *     kelompok nomor surat, penghitungan diperluas ke seluruh templat dalam kelompok nomor
	 *     surat yang sama -- sehingga beberapa kelompok aset dapat berbagi satu deret nomor.
	 *     Bila keduanya tidak aktif, tidak ada pembatas sama sekali dan SELURUH unit dihitung;</li>
	 * <li><b>Reset per tahun.</b> Bila diaktifkan, hanya unit dengan kolom {@code tahun} sama
	 *     dengan tahun berjalan yang dihitung;</li>
	 * <li><b>Reset per bulan.</b> Bila diaktifkan, unit disaring pada {@code tahun} DAN
	 *     {@code bulan} berjalan sekaligus;</li>
	 * <li><b>Reset per tanggal tertentu.</b> Bila templat punya tanggal reset yang sudah tiba
	 *     atau sudah lewat, hanya unit dengan {@code tanggalBeli} pada atau sesudah tanggal itu
	 *     yang dihitung.</li>
	 * </ol>
	 *
	 * <p>Hasil hitungan baris kemudian dinaikkan satu ({@code ++index}) dan dikembalikan sebagai
	 * nomor urut berikutnya. Jumlah {@code null} diperlakukan sebagai nol, sehingga nomor
	 * pertama selalu {@code 1}.</p>
	 *
	 * <h3>Cacat yang perlu diketahui: pembatas tahun dan bulan tidak pernah cocok</h3>
	 *
	 * <p>Pembatas nomor 2 dan 3 di atas menyaring pada properti {@code tahun} dan {@code bulan}
	 * milik {@code AssetDetail}. Kedua kolom itu TIDAK PERNAH TERISI: satu-satunya kode yang
	 * seharusnya mengisinya adalah {@link #getTahun()} dan {@link #getBulan()}, dan penjaga di
	 * kedua getter tersebut terbalik sehingga badannya mustahil dieksekusi (lihat penjelasan di
	 * sana), sementara tidak ada satu pun tempat lain di basis kode yang memanggil
	 * {@link #setTahun(Integer)} atau {@link #setBulan(Integer)} untuk entitas ini. Akibatnya,
	 * pada templat yang mengaktifkan reset per tahun atau per bulan, pembatas
	 * {@code eq("tahun", ...)} tidak pernah cocok dengan baris mana pun, hitungan selalu nol,
	 * dan metode ini selalu mengembalikan {@code 1}. Setiap unit baru akan memperoleh nomor
	 * yang SAMA. Templat yang tidak mengaktifkan reset per tahun/bulan tidak terpengaruh.</p>
	 *
	 * <p>Perbandingan yang berguna: entitas sekerabat seperti {@code PerbaikanAsset} memakai
	 * penjaga yang benar ({@code if (tahun == null)}) sehingga kolom tahun/bulannya terisi
	 * normal. Perbedaan itu menegaskan bahwa yang salah adalah penjaga di kelas ini, bukan
	 * rancangan penomorannya.</p>
	 *
	 * <h3>Sifat lain</h3>
	 *
	 * <p>Metode memakai {@code HibernateUtil.currentSession()}, jadi ia harus dipanggil di dalam
	 * konteks sesi yang sudah terbuka. Seperti strategi 1 pada
	 * {@link #generateBarcode(AssetDetail, Integer, boolean)}, penghitungan ini rentan balap
	 * karena tidak ada penguncian antara membaca jumlah dan menyimpan unit baru.</p>
	 *
	 * @param nomorSurat templat penomoran yang ingin dihitung nomor berikutnya; {@code null}
	 *                   menghasilkan {@code 0L}
	 * @return nomor urut berikutnya, minimal {@code 1}; atau {@code 0L} bila {@code nomorSurat}
	 *         {@code null}
	 */
	public static Long getindex(NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(AssetDetail.class)
				.createAlias("asset", "asset", Criteria.LEFT_JOIN)
				.createAlias("asset.masterAsset", "masterAsset", Criteria.LEFT_JOIN)
				.createAlias("masterAsset.kelompokAsset", "kelompokAsset", Criteria.LEFT_JOIN)
				.createAlias("kelompokAsset.nomorSurat", "nomorSurat", Criteria.LEFT_JOIN)

				.add(nomorSurat.getUrutBerdasarkanNomor() ? Restrictions.eq("kelompokAsset.nomorSurat", nomorSurat)

						: (nomorSurat.getUrutBerdasarkanKelompok() && nomorSurat.getKelompokNomorSurat() != null
								? Restrictions.eq("nomorSurat.kelompokNomorSurat", nomorSurat.getKelompokNomorSurat())
								: Restrictions.sqlRestriction("true")))

				.add(nomorSurat.getResetUrutanTiapTahun() ? Restrictions.eq("tahun", tahun)
						: Restrictions.sqlRestriction("true"))

				.add(nomorSurat.getResetUrutanTiapBulan()
						? Restrictions.and(Restrictions.eq("tahun", tahun), Restrictions.eq("bulan", bulan))
						: Restrictions.sqlRestriction("true"))

				.add(nomorSurat.getResetTiap() != null && (Common.dateFormat8.get().format(nomorSurat.getResetTiap())
						.equals(Common.dateFormat8.get().format(sekarang))
						|| nomorSurat.getResetTiap().before(sekarang))
								? Restrictions.ge("tanggalBeli", nomorSurat.getResetTiap())
								: Restrictions.sqlRestriction("true"))

				.setProjection(Projections.rowCount()).uniqueResult();

		Long index = indexO == null ? null : indexO.longValue();
		if (index == null) {
			index = 0L;
		}
		return ++index;
	}

	/**
	 * Menetapkan bulan perolehan unit.
	 *
	 * <p>Tidak ada satu pun tempat di basis kode yang memanggil setter ini untuk entitas
	 * {@code AssetDetail}; lihat {@link #getBulan()}.</p>
	 *
	 * @param bulan bulan perolehan (1--12)
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Bulan perolehan unit -- <b>penjaga terbalik, badan metode tidak pernah dieksekusi.</b>
	 *
	 * <p>Maksud metode ini jelas dari isinya: menurunkan bulan dari {@link #getTanggalBeli()}
	 * bila belum terisi. Namun penjaganya berbunyi {@code if (getTanggalBeli() == null)}, padahal
	 * niat yang wajar adalah {@code if (bulan == null)} -- bandingkan dengan entitas sekerabat
	 * {@code PerbaikanAsset} yang memakai bentuk benar tersebut.</p>
	 *
	 * <p>Penjaga itu bahkan tidak sekadar salah arah, melainkan MUSTAHIL benar:
	 * {@link #getTanggalBeli()} berakhir dengan {@code tanggalBeli == null ? WaktuUtil.getDate()
	 * : tanggalBeli}, sehingga ia tidak pernah mengembalikan {@code null}. Andaikata penjaga itu
	 * suatu saat menjadi benar, baris berikutnya ({@code calendar.setTime(getTanggalBeli())})
	 * justru akan melempar {@code NullPointerException} karena memanggil ulang getter yang baru
	 * saja dianggap {@code null}. Dengan kata lain, badan metode ini adalah kode mati.</p>
	 *
	 * <p><b>Akibat nyata.</b> Kolom {@code bulan} pada tabel {@code asset.asset_detail} tetap
	 * kosong selamanya, dan pembatas {@code eq("bulan", ...)} di {@link #getindex(NomorSurat)}
	 * tidak pernah cocok. Templat nomor surat yang mengaktifkan reset urutan tiap bulan karena
	 * itu selalu menghasilkan nomor {@code 1}, sehingga barcode unit menjadi kembar. Catatan ini
	 * sengaja dituliskan agar pembaca berikutnya tidak menyangka kolom ini memang tidak dipakai.</p>
	 *
	 * @return nilai {@code bulan} yang tersimpan apa adanya -- praktis selalu {@code null}
	 */
	public Integer getBulan() {
		if (getTanggalBeli() == null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(getTanggalBeli());
			bulan = calendar.get(Calendar.MONTH) + 1;
		}
		return bulan;
	}

	/**
	 * Menetapkan tahun perolehan unit.
	 *
	 * <p>Tidak ada satu pun tempat di basis kode yang memanggil setter ini untuk entitas
	 * {@code AssetDetail}; lihat {@link #getTahun()}.</p>
	 *
	 * @param tahun tahun perolehan
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Tahun perolehan unit -- <b>penjaga terbalik, badan metode tidak pernah dieksekusi.</b>
	 *
	 * <p>Persis sekasus dengan {@link #getBulan()}: penjaga {@code if (getTanggalBeli() == null)}
	 * mustahil benar karena {@link #getTanggalBeli()} selalu mengembalikan nilai, dan seandainya
	 * benar pun baris di dalamnya akan melempar {@code NullPointerException}. Kolom {@code tahun}
	 * karena itu tetap kosong selamanya, membuat pembatas reset urutan tiap tahun dan tiap bulan
	 * pada {@link #getindex(NomorSurat)} tidak pernah cocok dengan baris mana pun.</p>
	 *
	 * @return nilai {@code tahun} yang tersimpan apa adanya -- praktis selalu {@code null}
	 */
	public Integer getTahun() {
		if (getTanggalBeli() == null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(getTanggalBeli());
			tahun = calendar.get(Calendar.YEAR);
		}
		return tahun;
	}
}
