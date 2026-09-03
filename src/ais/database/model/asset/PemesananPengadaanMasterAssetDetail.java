package ais.database.model.asset;

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

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;

/**
 * Baris item pada dokumen <b>Pemesanan Pengadaan Barang/Jasa</b> (PO) modul Aset
 * Tetap: satu baris menyatakan satu jenis barang/jasa yang dipesan ke penyedia,
 * lengkap dengan kuantitas, harga sepakat, potongan, dan struktur pajaknya.
 *
 * <h3>Bedanya dengan baris Permintaan</h3>
 * <p>Kelas ini adalah kelanjutan langsung dari
 * {@link PermintaanPengadaanMasterAssetDetail}, tetapi jauh lebih kaya secara
 * finansial. Baris permintaan hanya menyimpan jumlah dan perkiraan harga; baris
 * pesanan menambahkan <b>potongan</b> ({@link #getHargaPotongan()} dan
 * {@link #getDiskonDalamBentukPersen()}), <b>PPN</b>
 * ({@link #getJenisPajakPpn()}), dan <b>PPh</b>
 * ({@link #getJenisPajakBarang()}). Karena itu
 * {@link #getHargaTotal()} di sini menghitung
 * <code>(DPP + PPN) - PPh</code> sementara versi permintaan hanya menghitung
 * <code>jumlah x harga</code>. <b>Nilai baris PR dan nilai baris PO atas barang
 * yang sama tidak seharusnya diharapkan cocok</b>; jangan menulis rekonsiliasi
 * yang mengasumsikan demikian.</p>
 *
 * <h3>Jaring relasi - rantai PR-&gt;PO memakai FK nyata</h3>
 * <p>Baris ini menunjuk ke empat dokumen tetangga sekaligus:</p>
 * <ul>
 *   <li>{@link #getPemesananPengadaanMasterAsset()} - header PO induknya;</li>
 *   <li>{@link #getPermintaanPengadaanMasterAssetDetail()} - baris PR asalnya
 *       (arah PO-&gt;PR), pasangan dari
 *       {@link PermintaanPengadaanMasterAssetDetail#getPemesananPengadaanMasterAssetDetail()}
 *       yang menunjuk balik;</li>
 *   <li>{@link #getPenerimaanPengadaanMasterAssetDetail()} - baris BAST yang
 *       merealisasikan pesanan ini;</li>
 *   <li>{@link #getPerjanjianKerjasamaMasterAssetDetail()} - baris kontrak
 *       payung, bila pengadaan menempuh kontrak.</li>
 * </ul>
 * <p>Berbeda dari <code>PengajuanPembelianGudang</code> di paket
 * <code>inventory</code> yang ternyata hanya antrean kerja tanpa satu pun FK ke
 * dokumen realisasinya, rantai pengadaan aset <b>terjahit oleh kunci asing
 * nyata</b> - bahkan dua arah pada tingkat baris. Risiko integritasnya berupa
 * ketidaksinkronan antar-lapis, bukan hilangnya jejak.</p>
 *
 * <h3>Catatan teknis</h3>
 * <ul>
 *   <li>Mewarisi {@link GeneralValueObject} (bukan <code>DataSop</code> seperti
 *       headernya) - baris tidak menjalani alur persetujuan sendiri melainkan
 *       mengikuti status headernya. Dari kelas induk itu datang helper
 *       <code>check()</code> untuk me-<i>reattach</i> proxy Hibernate.</li>
 *   <li>Ber-anotasi {@link Audited} (Envers), sehingga perubahan harga, potongan,
 *       dan pajak terekam pada tabel revisi.</li>
 *   <li>Beberapa getter bersifat <b>destruktif</b> dan satu setter
 *       ({@link #setJenisPajakPpn(JenisPajakPpn)}) memiliki efek samping yang
 *       menyetel properti lain. Setiap kasus ditandai pada anggotanya
 *       masing-masing.</li>
 * </ul>
 *
 * @see PemesananPengadaanMasterAsset header dokumen PO
 * @see PermintaanPengadaanMasterAssetDetail baris PR hulu
 * @see PenerimaanPengadaanMasterAssetDetail baris BAST hilir
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "asset", name = "pemesanan_pengadaan_master_asset_detail")
public class PemesananPengadaanMasterAssetDetail extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama (identity). Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang menyentuh baris (audit bayangan). Lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang menyentuh baris (audit bayangan). Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan <b>id</b> pengguna yang terakhir menyentuh baris ini.
	 *
	 * <p>Bagian dari trio audit bayangan ({@link #getOleh()},
	 * {@link #getOlehId()}, {@link #getTanggal_dirubah()}) yang diisi
	 * <code>AuditTimestampInterceptor</code>. Kehadirannya di samping
	 * {@link Audited} milik Envers adalah <b>keharusan teknis</b>, bukan duplikasi
	 * yang bisa dibuang: layar daftar perlu menampilkan jejak perubahan untuk
	 * banyak baris sekaligus tanpa join ke tabel revisi.</p>
	 *
	 * @return id pengguna terakhir, atau {@code null}
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna terakhir yang menyentuh baris ini.
	 *
	 * <p>Nilai {@code null} atau berisi spasi saja <b>diabaikan diam-diam</b> agar
	 * jejak audit tidak terhapus oleh pemanggil tanpa konteks pengguna. Akibatnya
	 * field ini tidak dapat dikosongkan lewat setter.</p>
	 *
	 * @param olehId id pengguna; nilai null/kosong diabaikan
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks baris, berformat <code>&lt;id&gt;-&lt;master aset&gt;</code>.
	 *
	 * <p><b>Method ini destruktif</b>: sebelum merangkai teks ia memanggil
	 * {@link #getMasterAsset()} dan menugaskan hasilnya kembali ke field, sehingga
	 * sekadar mencetak baris ke log dapat memicu <i>reattach</i> proxy dan SELECT
	 * tambahan ke tabel master aset. Perilaku ini tidak lazim untuk
	 * <code>toString()</code> dan perlu diingat saat memasang log berlevel debug
	 * pada perulangan besar.</p>
	 *
	 * <p>Untuk baris yang belum tersimpan, {@link #getId()} bernilai null sehingga
	 * hasilnya diawali teks "null".</p>
	 *
	 * @return teks pengenal baris
	 */
	public String toString() {
		masterAsset = getMasterAsset();
		return id + "-" + masterAsset + "";
	}

	/**
	 * Menyetel nama pengguna terakhir yang menyentuh baris ini.
	 *
	 * <p>Nilai null/kosong diabaikan, sama seperti {@link #setOlehId(String)}.</p>
	 *
	 * @param oleh nama pengguna; nilai null/kosong diabaikan
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan <b>nama</b> pengguna yang terakhir menyentuh baris ini.
	 *
	 * @return nama pengguna terakhir, atau {@code null}
	 * @see #getOlehId()
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang berjalan tepat sebelum baris di-UPDATE.
	 *
	 * <p>Mendelegasikan ke <code>AuditTimestampInterceptor.ubah(this)</code> yang
	 * mengisi trio audit bayangan dari konteks pengguna aktif. Dipicu Hibernate;
	 * jangan dipanggil manual.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Cap waktu perubahan terakhir, diinisialisasi ke waktu server saat objek
	 * dibuat sehingga baris baru tidak pernah bercap waktu null.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel cap waktu perubahan terakhir. Umumnya dipanggil
	 * <code>AuditTimestampInterceptor</code>, bukan kode aksi.
	 *
	 * @param tanggal_dirubah cap waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir baris ini.
	 *
	 * @return cap waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Katalog barang/jasa yang dipesan. Lihat {@link #getMasterAsset()}. */
	private MasterAsset masterAsset;
	/** Kuantitas yang dipesan. Lihat {@link #getJumlah()}. */
	private Double jumlah;
	/** Header PO induk. Lihat {@link #getPemesananPengadaanMasterAsset()}. */
	private PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset;
	/** Baris PR asal pesanan ini. Lihat {@link #getPermintaanPengadaanMasterAssetDetail()}. */
	private PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail;
	/** Baris BAST yang merealisasikan pesanan ini. Lihat {@link #getPenerimaanPengadaanMasterAssetDetail()}. */
	private PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail;
	/** Baris kontrak payung. Lihat {@link #getPerjanjianKerjasamaMasterAssetDetail()}. */
	private PerjanjianKerjasamaMasterAssetDetail perjanjianKerjasamaMasterAssetDetail;
	/** Uraian bebas baris. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Harga satuan sepakat. Lihat {@link #getHargaBeli()}. */
	private Double hargaBeli;
	/** Nilai total baris setelah potongan dan pajak. Lihat {@link #getHargaTotal()}. */
	private Double hargaTotal;
	/** Besar potongan (persen atau nominal). Lihat {@link #getHargaPotongan()}. */
	private Double hargaPotongan;
	/** Penanda potongan dibaca sebagai persen. Lihat {@link #getDiskonDalamBentukPersen()}. */
	private Boolean diskonDalamBentukPersen;
	/** Persentase PPN (cache dari jenis pajak). Lihat {@link #getPersenPpn()}. */
	private Double persenPpn;
	/** Jenis pajak barang penentu PPh. Lihat {@link #getJenisPajakBarang()}. */
	private JenisPajakBarang jenisPajakBarang;
	/** Persentase PPh (cache dari jenis pajak barang). Lihat {@link #getPersenPph()}. */
	private Double persenPph;
	/** Jenis PPN yang dikenakan. Lihat {@link #getJenisPajakPpn()}. */
	private JenisPajakPpn jenisPajakPpn;
	/** Kode gabungan penjamin keunikan baris. Lihat {@link #getKodeUnik()}. */
	private String kodeUnik;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate untuk instansiasi
	 * lewat refleksi, sekaligus dipakai layar PO untuk menambah baris baru pada
	 * grid - baik baris kosong maupun baris hasil salinan dari permintaan atau
	 * kontrak payung. Seluruh field dibiarkan null; getter berpelindung memasok
	 * nilai bawaan saat pertama dibaca.
	 */
	public PemesananPengadaanMasterAssetDetail() {
	}

	/**
	 * Mengembalikan kunci utama baris.
	 *
	 * <p>Kolom bertanda <code>insertable = false</code> karena dibangkitkan basis
	 * data (IDENTITY). Nilai {@code null} berarti baris belum tersimpan - kondisi
	 * yang membuat {@link #getKodeUnik()} menghasilkan teks berawalan "null".</p>
	 *
	 * @return kunci utama, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris. Dipakai Hibernate saat memuat; kode aplikasi
	 * umumnya tidak perlu memanggilnya.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan uraian bebas untuk baris ini, misalnya spesifikasi teknis yang
	 * disepakati dengan penyedia.
	 *
	 * <p>Nilai ini <b>disalin dari baris permintaan</b> saat layar PO membangun
	 * pesanan dari PR, sehingga spesifikasi yang ditulis pemohon ikut sampai ke
	 * penyedia lewat cetakan PO.</p>
	 *
	 * @return uraian baris, atau {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel uraian bebas untuk baris ini.
	 *
	 * @param keterangan uraian baris
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menyetel katalog barang/jasa yang dipesan.
	 *
	 * @param masterAsset entri katalog {@link MasterAsset}
	 */
	public void setMasterAsset(MasterAsset masterAsset) {
		this.masterAsset = masterAsset;
	}

	/**
	 * Mengembalikan entri katalog barang/jasa yang dipesan pada baris ini.
	 *
	 * <p>Properti wajib menurut validasi layar PO - pesanan dengan baris tanpa
	 * master aset ditolak saat disimpan. Selain menentukan apa yang dibeli, entri
	 * katalog memasok harga bawaan lewat {@link #getHargaBeli()} dan ikut
	 * membentuk {@link #getKodeUnik()}.</p>
	 *
	 * <p><b>Getter destruktif</b> karena memanggil <code>check()</code>.
	 * Perhatikan bahwa {@link #getHargaBeli()} membaca <b>field mentah</b>
	 * {@link #masterAsset} dan bukan getter ini - lihat catatan di sana.</p>
	 *
	 * <p>Kolom pemetaannya bernama <code>masterAsset</code> (camelCase),
	 * menyimpang dari konvensi <i>snake_case</i> kolom lain di tabel ini.
	 * Peninggalan pembangkitan hbm2java; jangan "dirapikan" tanpa migrasi basis
	 * data.</p>
	 *
	 * @return entri katalog, atau {@code null} bila baris belum lengkap
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "masterAsset", nullable = true)
	public MasterAsset getMasterAsset() {
		masterAsset = check(masterAsset);
		return masterAsset;
	}

	/**
	 * Menyetel kuantitas yang dipesan.
	 *
	 * @param jumlah kuantitas pesanan
	 */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * Mengembalikan kuantitas yang dipesan pada baris ini.
	 *
	 * <p>Nilai bawaannya {@code 1.0} - bukan {@code 0.0} - agar baris yang baru
	 * ditambahkan langsung berkuantitas masuk akal dan tidak menghasilkan total
	 * nol yang membingungkan.</p>
	 *
	 * <p><b>Berbeda dari padanannya di baris permintaan</b>:
	 * {@link PermintaanPengadaanMasterAssetDetail#getJumlah()} memaksa nilai
	 * {@code 1.0} untuk baris yang terkait uang muka ber-LPJ disetujui. Aturan itu
	 * tidak ada di sini karena baris PO tidak pernah menempuh jalur uang muka -
	 * pesanan ke penyedia dan panjar ke pelaksana adalah dua mekanisme yang
	 * terpisah. Getter ini karena itu <b>tidak destruktif</b> dan tidak menurunkan
	 * nilai dari mana pun.</p>
	 *
	 * <p>Saat layar PO membangun baris dari permintaan, kuantitas diisi dengan
	 * <code>jumlah PR dikurangi jumlah yang sudah datang</code> - perhatikan
	 * peringatan integritas pada
	 * {@link PermintaanPengadaanMasterAssetDetail#getJumlahDatang()} mengenai
	 * dasar perhitungan sisa tersebut.</p>
	 *
	 * @return kuantitas pesanan; tidak pernah {@code null}, minimal {@code 1.0}
	 *         untuk baris baru
	 */
	public Double getJumlah() {
		return jumlah == null ? 1.0 : jumlah;
	}

	/**
	 * Menyetel header PO induk baris ini.
	 *
	 * <p>Wajib diisi sebelum baris disimpan. Kolomnya <i>nullable</i> di basis
	 * data, tetapi baris tanpa header menjadi <b>baris yatim</b> yang tidak akan
	 * pernah muncul di layar mana pun karena semua kueri baris menyaring lewat
	 * headernya - dan {@link #getKodeUnik()} akan mengembalikan {@code null}
	 * untuknya.</p>
	 *
	 * @param pemesananPengadaanMasterAsset header PO induk
	 */
	public void setPemesananPengadaanMasterAsset(PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset) {
		this.pemesananPengadaanMasterAsset = pemesananPengadaanMasterAsset;
	}

	/**
	 * Mengembalikan header PO induk baris ini.
	 *
	 * <p>Selain menjadi induk struktural, header memasok satu nilai yang menimpa
	 * pengaturan baris: pada PO bertanda
	 * {@link PemesananPengadaanMasterAsset#getPembelianLangsung()},
	 * {@link #getJenisPajakPpn()} mengambil alih jenis PPN dari
	 * {@link PemesananPengadaanMasterAsset#getJenisPajakPpnDp()} milik header.</p>
	 *
	 * <p><b>Tidak memanggil <code>check()</code></b> - mengembalikan field mentah.
	 * Aman karena relasi dipetakan tanpa <code>fetch = LAZY</code>, sehingga
	 * Hibernate memuatnya seketika lewat SELECT terpisah
	 * (<code>@Fetch(FetchMode.SELECT)</code>). Harganya satu kueri tambahan per
	 * baris yang dirender - salah satu sumber pola N+1 pada layar daftar.</p>
	 *
	 * @return header PO induk, atau {@code null} untuk baris yatim
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pemesanan_pengadaan_master_asset", nullable = true)
	public PemesananPengadaanMasterAsset getPemesananPengadaanMasterAsset() {
		return pemesananPengadaanMasterAsset;
	}

	/**
	 * Mengembalikan harga satuan sepakat untuk baris ini.
	 *
	 * <p>Bila kolom masih kosong atau bernilai nol, harga diambil dari
	 * <code>MasterAsset.getHargaBeliDefault()</code> dan <b>ditulis balik</b> ke
	 * field - sehingga baris baru langsung menampilkan harga wajar tanpa perlu
	 * diketik. Ini membuat method bersifat <b>destruktif</b>: pembacaan pada
	 * entitas terkelola dapat memicu UPDATE.</p>
	 *
	 * <p><b>Jebakan: memeriksa field mentah, bukan getter.</b> Syarat cabangnya
	 * berbunyi <code>masterAsset != null</code>, memeriksa <b>field</b> alih-alih
	 * {@link #getMasterAsset()}. Field tidak melewati <code>check()</code>,
	 * sehingga bila proxy master aset berasal dari sesi yang sudah tertutup,
	 * pemanggilan <code>getHargaBeliDefault()</code> di baris berikutnya dapat
	 * melempar <code>LazyInitializationException</code> - persis jenis kegagalan
	 * yang <code>check()</code> ada untuk mencegahnya. Dalam praktik jarang
	 * terjadi karena perenderan grid (dan {@link #toString()}) biasanya sudah
	 * menyegarkan field lebih dulu. Bila method ini disunting, ganti
	 * pemeriksaannya menjadi <code>getMasterAsset() != null</code>. Pola yang sama
	 * ada di
	 * {@link PermintaanPengadaanMasterAssetDetail#getHargaBeli()}.</p>
	 *
	 * <p>Berbeda dari versi permintaan, method ini <b>tidak</b> memiliki cabang
	 * pengambilalihan nilai dari LPJ uang muka - baris PO tidak menempuh jalur
	 * panjar.</p>
	 *
	 * @return harga satuan; tidak pernah {@code null}
	 */
	public Double getHargaBeli() {
		if ((hargaBeli == null || hargaBeli == 0.0) && masterAsset != null) {
			hargaBeli = masterAsset.getHargaBeliDefault();
		}
		return hargaBeli == null ? 0.0 : hargaBeli;
	}

	/**
	 * Menyetel harga satuan sepakat.
	 *
	 * <p>Menyetel nilai {@code 0.0} tidak akan bertahan: {@link #getHargaBeli()}
	 * memperlakukan nol sebagai "belum diisi" dan menggantinya dengan harga bawaan
	 * katalog pada pembacaan berikutnya. Untuk barang yang memang berharga nol,
	 * gunakan potongan seratus persen lewat {@link #setHargaPotongan(Double)}.</p>
	 *
	 * @param hargaBeli harga satuan
	 */
	public void setHargaBeli(Double hargaBeli) {
		this.hargaBeli = hargaBeli;
	}

	/**
	 * Mengembalikan besar potongan harga untuk baris ini.
	 *
	 * <p><b>Satuannya bergantung pada bendera lain</b>: bila
	 * {@link #getDiskonDalamBentukPersen()} bernilai true (yang merupakan nilai
	 * bawaannya), angka ini dibaca sebagai <b>persentase</b> dari DPP; bila false,
	 * dibaca sebagai <b>nominal</b>. Membaca angka ini tanpa memeriksa benderanya
	 * karena itu tidak bermakna - selalu pasangkan keduanya.</p>
	 *
	 * <p>Potongan dikurangkan dari DPP <b>sebelum</b> PPN dan PPh dihitung, di
	 * ketiga tempat perhitungan: {@link #getHargaTotal()}, {@link #hitungPpn()},
	 * dan {@link #hitungPph()}. Urutan itu penting - memotong setelah pajak akan
	 * menghasilkan angka berbeda dan tidak sesuai praktik perpajakan.</p>
	 *
	 * <p>Mengembalikan {@code 0.0} untuk null sehingga aman dipakai dalam
	 * aritmetika tanpa penjagaan tambahan. Tidak ada pembatasan nilai: potongan
	 * persen di atas seratus, atau potongan nominal melebihi DPP, akan
	 * menghasilkan DPP negatif tanpa peringatan.</p>
	 *
	 * @return besar potongan (persen atau nominal); tidak pernah {@code null}
	 */
	public Double getHargaPotongan() {
		return hargaPotongan == null ? 0.0 : hargaPotongan;
	}

	/**
	 * Menyetel besar potongan harga.
	 *
	 * <p>Satuannya ditentukan {@link #getDiskonDalamBentukPersen()}. Tidak ada
	 * validasi rentang - lihat {@link #getHargaPotongan()}.</p>
	 *
	 * @param hargaPotongan besar potongan (persen atau nominal)
	 */
	public void setHargaPotongan(Double hargaPotongan) {
		this.hargaPotongan = hargaPotongan;
	}

	/**
	 * Mengembalikan baris Permintaan Pengadaan (PR) yang menjadi asal pesanan ini.
	 *
	 * <h3>Kaitan dua arah PR-&gt;PO</h3>
	 * <p>Kolom <code>permintaan_pengadaan_master_asset_detail</code> adalah
	 * pasangan dari
	 * {@link PermintaanPengadaanMasterAssetDetail#getPemesananPengadaanMasterAssetDetail()}
	 * yang menunjuk balik; keduanya ditulis bersamaan oleh
	 * <code>PemesananPengadaanMasterAssetAction.onSave()</code>. Inilah bukti
	 * paling langsung bahwa rantai permintaan-ke-pemesanan pada modul aset
	 * <b>memakai kunci asing nyata</b>, bukan antrean kerja seperti pada modul
	 * gudang.</p>
	 *
	 * <h3>Pemulihan asal-usul lewat kontrak payung</h3>
	 * <p>Bila baris ini dibentuk dari baris kontrak payung alih-alih langsung dari
	 * permintaan, kolomnya kosong saat dibuat. Getter menutupi celah itu: bila
	 * {@link #getPerjanjianKerjasamaMasterAssetDetail()} ada dan baris kontrak itu
	 * sendiri menunjuk sebuah baris permintaan, nilai itulah yang dikembalikan -
	 * dan <b>ditulis balik ke field</b>, sehingga kolom perlahan terisi sendiri.
	 * Dengan begitu jejak PO-ke-PR tetap utuh meski dokumen perantaranya berbeda,
	 * dan laporan tidak perlu tahu apakah pengadaan menempuh kontrak atau
	 * tidak.</p>
	 * <p>Perhatikan arah menangnya: nilai turunan dari kontrak <b>menimpa</b> isi
	 * kolom, bukan sebaliknya. Bila baris kontrak kelak menunjuk permintaan yang
	 * berbeda, baris PO ini akan ikut berpindah asal-usul pada pembacaan
	 * berikutnya.</p>
	 *
	 * <p><b>Getter destruktif</b> pada cabang kontrak, dan memanggil
	 * {@link #getPerjanjianKerjasamaMasterAssetDetail()} dua kali. Karena getter
	 * itu tidak <i>lazy</i>, pemanggilan ganda di sini tidak menimbulkan risiko
	 * inkonsistensi seperti pada rantai disposisi SOP di kelas header.</p>
	 *
	 * @return baris PR asal, atau {@code null} bila pesanan diterbitkan tanpa
	 *         permintaan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "permintaan_pengadaan_master_asset_detail", nullable = true)
	public PermintaanPengadaanMasterAssetDetail getPermintaanPengadaanMasterAssetDetail() {

		if (getPerjanjianKerjasamaMasterAssetDetail() != null
				&& getPerjanjianKerjasamaMasterAssetDetail().getPermintaanPengadaanMasterAssetDetail() != null) {
			permintaanPengadaanMasterAssetDetail = getPerjanjianKerjasamaMasterAssetDetail()
					.getPermintaanPengadaanMasterAssetDetail();
		}

		return permintaanPengadaanMasterAssetDetail;
	}

	/**
	 * Menyetel baris Permintaan Pengadaan asal pesanan ini.
	 *
	 * <p>Nilai yang disetel akan <b>ditimpa saat dibaca</b> bila baris ini terkait
	 * kontrak payung yang menunjuk permintaan lain - lihat
	 * {@link #getPermintaanPengadaanMasterAssetDetail()}.</p>
	 *
	 * @param permintaanPengadaanMasterAssetDetail baris PR asal
	 */
	public void setPermintaanPengadaanMasterAssetDetail(
			PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail) {
		this.permintaanPengadaanMasterAssetDetail = permintaanPengadaanMasterAssetDetail;
	}

	/**
	 * Mengembalikan baris Penerimaan (BAST) yang merealisasikan pesanan ini.
	 *
	 * <p>Kehadiran nilai ini menandakan barang sudah datang untuk baris pesanan
	 * ini. Dialog pemilih PR pada layar PO memakainya untuk menghitung berapa yang
	 * sudah benar-benar diterima atas sebuah baris permintaan - ia menelusuri
	 * seluruh baris PO yang menunjuk baris PR tersebut, lalu menjumlahkan
	 * <code>getDiterima()</code> dari baris BAST yang tergantung di sini.</p>
	 * <p><b>Batasan yang perlu diketahui</b>: kolom ini bertipe tunggal, sehingga
	 * satu baris pesanan yang barangnya datang bertahap dalam beberapa BAST hanya
	 * dapat mencatat satu di antaranya. Untuk penelusuran lengkap, lakukan kueri
	 * balik dari {@link PenerimaanPengadaanMasterAssetDetail}.</p>
	 *
	 * <p><b>Tidak memanggil <code>check()</code></b>; dimuat seketika lewat SELECT
	 * terpisah.</p>
	 *
	 * @return baris BAST, atau {@code null} bila barang belum diterima
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penerimaan_pengadaan_master_asset_detail", nullable = true)
	public PenerimaanPengadaanMasterAssetDetail getPenerimaanPengadaanMasterAssetDetail() {
		return penerimaanPengadaanMasterAssetDetail;
	}

	/**
	 * Menyetel baris Penerimaan yang merealisasikan pesanan ini.
	 *
	 * @param penerimaanPengadaanMasterAssetDetail baris BAST
	 */
	public void setPenerimaanPengadaanMasterAssetDetail(
			PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail) {
		this.penerimaanPengadaanMasterAssetDetail = penerimaanPengadaanMasterAssetDetail;
	}

	/**
	 * Mengembalikan baris perjanjian kerjasama (kontrak payung) yang menaungi
	 * baris pesanan ini.
	 *
	 * <p>Bila pengadaan menempuh kontrak payung, layar PO membentuk baris pesanan
	 * dari baris kontrak - menyalin master aset, kuantitas, keterangan, dan harga
	 * beli darinya - lalu menyimpan kaitan di sini. Nilai ini kemudian menjadi
	 * jembatan yang memulihkan asal-usul permintaan lewat
	 * {@link #getPermintaanPengadaanMasterAssetDetail()}.</p>
	 *
	 * <p><b>Tidak memanggil <code>check()</code></b>; dimuat seketika lewat SELECT
	 * terpisah.</p>
	 *
	 * @return baris kontrak payung, atau {@code null} bila pengadaan langsung
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "perjanjian_kerjasama_master_asset_detail", nullable = true)
	public PerjanjianKerjasamaMasterAssetDetail getPerjanjianKerjasamaMasterAssetDetail() {
		return perjanjianKerjasamaMasterAssetDetail;
	}

	/**
	 * Menyetel baris perjanjian kerjasama yang menaungi baris pesanan ini.
	 *
	 * <p><b>Perhatikan efek sampingnya</b>: menyetel nilai ini membuat
	 * {@link #getPermintaanPengadaanMasterAssetDetail()} mengambil asal-usul
	 * permintaan dari baris kontrak tersebut, menimpa kaitan yang sudah ada.</p>
	 *
	 * @param perjanjianKerjasamaMasterAssetDetail baris kontrak payung
	 */
	public void setPerjanjianKerjasamaMasterAssetDetail(
			PerjanjianKerjasamaMasterAssetDetail perjanjianKerjasamaMasterAssetDetail) {
		this.perjanjianKerjasamaMasterAssetDetail = perjanjianKerjasamaMasterAssetDetail;
	}

	/**
	 * Mengembalikan persentase PPN yang dikenakan pada baris ini.
	 *
	 * <p>Bila {@link #getJenisPajakPpn()} ada, persentasenya <b>selalu diambil
	 * dari sana</b> dan ditulis balik ke field - sehingga kolom
	 * <code>persenPpn</code> berfungsi sebagai cache dari jenis pajak, bukan
	 * sebagai nilai yang dapat disetel bebas. <b>Getter destruktif.</b></p>
	 *
	 * <p>Kolom cache ini bukan duplikasi yang bisa dibuang: ia menjaga agar
	 * dokumen lama tetap mencetak persentase yang berlaku <i>pada saat itu</i>
	 * seandainya tarif pada master jenis pajak kelak diubah - selama dokumen lama
	 * tidak dibaca ulang dan ter-flush. Perlu diketahui bahwa perlindungan itu
	 * <b>rapuh</b> justru karena sifat destruktif di atas: begitu dokumen lama
	 * dibuka dalam sesi yang ter-flush, persentasenya ikut diperbarui ke tarif
	 * terkini.</p>
	 *
	 * <p>Nilai bawaannya {@code 0.0} - baris tanpa jenis pajak dianggap tidak
	 * ber-PPN.</p>
	 *
	 * @return persentase PPN; tidak pernah {@code null}
	 * @see #getJenisPajakPpn()
	 */
	public Double getPersenPpn() {

		if (getJenisPajakPpn() != null) {
			persenPpn = getJenisPajakPpn().getPersen();
		}

		return persenPpn == null ? 0.0 : persenPpn;
	}

	/**
	 * Menyetel persentase PPN.
	 *
	 * <p>Nilai yang disetel akan ditimpa oleh persentase jenis pajak pada
	 * pembacaan berikutnya bila {@link #getJenisPajakPpn()} tidak null. Setter ini
	 * juga dipanggil sebagai efek samping oleh
	 * {@link #setJenisPajakPpn(JenisPajakPpn)}.</p>
	 *
	 * @param persenPpn persentase PPN
	 */
	public void setPersenPpn(Double persenPpn) {
		this.persenPpn = persenPpn;
	}

	/**
	 * Mengembalikan jenis pajak barang yang menentukan tarif PPh baris ini.
	 *
	 * <p>Berbeda dari {@link #getJenisPajakPpn()} yang menyangkut pajak
	 * pertambahan nilai, entitas ini menentukan potongan pajak penghasilan atas
	 * pembayaran ke penyedia. Tarifnya dibaca {@link #getPersenPph()}.</p>
	 *
	 * <p><b>Getter destruktif</b> karena memanggil <code>check()</code>.</p>
	 *
	 * @return jenis pajak barang, atau {@code null} bila tidak ada potongan PPh
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pajak_barang", nullable = true)
	public JenisPajakBarang getJenisPajakBarang() {
		jenisPajakBarang = check(jenisPajakBarang);

		return jenisPajakBarang;
	}

	/**
	 * Menyetel jenis pajak barang penentu tarif PPh.
	 *
	 * <p>Berbeda dari {@link #setJenisPajakPpn(JenisPajakPpn)}, setter ini
	 * <b>tidak</b> menyegarkan cache persentasenya. Kolom
	 * <code>persenPph</code> baru diperbarui saat {@link #getPersenPph()}
	 * dibaca.</p>
	 *
	 * @param jenisPajakBarang jenis pajak barang
	 */
	public void setJenisPajakBarang(JenisPajakBarang jenisPajakBarang) {
		this.jenisPajakBarang = jenisPajakBarang;
	}

	/**
	 * Mengembalikan persentase PPh yang dipotong pada baris ini.
	 *
	 * <p>Bila {@link #getJenisPajakBarang()} ada, persentasenya diambil dari sana
	 * dan ditulis balik ke field - <b>getter destruktif</b>, dengan peran cache
	 * yang sama (dan kerapuhan yang sama) seperti {@link #getPersenPpn()}.</p>
	 *
	 * <p><b>Catatan gaya penulisan</b>: syarat cabangnya memanggil
	 * {@link #getJenisPajakBarang()} - yang melakukan <code>check()</code> -
	 * tetapi badan cabangnya membaca <b>field mentah</b>
	 * <code>jenisPajakBarang</code>. Bentuk campuran ini aman <i>hanya karena</i>
	 * getter yang dipanggil di syarat sudah menyegarkan field tepat sebelumnya.
	 * Ia akan menjadi rapuh bila urutannya diubah; bila method ini disunting,
	 * pakai getter di kedua tempat.</p>
	 *
	 * <p>Nilai bawaannya {@code 0.0}.</p>
	 *
	 * @return persentase PPh; tidak pernah {@code null}
	 * @see #hitungPph()
	 */
	public Double getPersenPph() {
		if (getJenisPajakBarang() != null) {
			persenPph = jenisPajakBarang.getPersen();
		}
		return persenPph == null ? 0.0 : persenPph;
	}

	/**
	 * Menyetel persentase PPh.
	 *
	 * <p>Nilai yang disetel akan ditimpa oleh tarif jenis pajak barang pada
	 * pembacaan berikutnya bila {@link #getJenisPajakBarang()} tidak null.</p>
	 *
	 * @param persenPph persentase PPh
	 */
	public void setPersenPph(Double persenPph) {
		this.persenPph = persenPph;
	}

	/**
	 * Menghitung nilai total baris: <b>(DPP setelah potongan + PPN) dikurangi
	 * PPh</b>.
	 *
	 * <h3>Urutan perhitungan</h3>
	 * <ol>
	 *   <li><b>DPP kotor</b> = {@link #getJumlah()} x {@link #getHargaBeli()}.</li>
	 *   <li><b>Potongan</b> - bila {@link #getDiskonDalamBentukPersen()} true,
	 *       potongan dihitung sebagai persentase dari DPP kotor; bila false,
	 *       dipakai sebagai nominal. Hasilnya dikurangkan sehingga menghasilkan
	 *       <b>DPP bersih</b>.</li>
	 *   <li><b>PPN</b> = persentase PPN x DPP bersih. Dihitung <i>setelah</i>
	 *       potongan - urutan yang penting dan sesuai praktik perpajakan.</li>
	 *   <li><b>PPh</b> = persentase PPh x DPP bersih, <b>tetapi hanya bila
	 *       konfigurasi <code>pph_mengurangi_lpj</code> menyala</b>; bila padam,
	 *       PPh diperlakukan nol di sini.</li>
	 *   <li>Total = (DPP bersih + PPN) - PPh.</li>
	 * </ol>
	 *
	 * <h3>Konfigurasi pph_mengurangi_lpj: satu sakelar, dua makna</h3>
	 * <p>Perlakuan PPh berbeda antar-instalasi: sebagian lembaga mencatat nilai
	 * pesanan sebagai jumlah yang <i>dibayarkan ke penyedia</i> (PPh dipotong,
	 * jadi mengurangi), sebagian lagi mencatatnya sebagai jumlah <i>tagihan
	 * penyedia</i> (PPh disetor terpisah, jadi tidak mengurangi). Sakelar
	 * <code>pph_mengurangi_lpj</code> memilih di antara keduanya, dan dibaca lewat
	 * <code>Common.bolehKonfigurasi</code> yang mengambilnya dari tabel
	 * {@link Konfigurasi}.</p>
	 * <p><b>Konsekuensi yang harus disadari</b>: mengubah konfigurasi ini mengubah
	 * nilai total <b>seluruh dokumen lama</b> saat dibaca ulang, karena method ini
	 * menghitung dari nol setiap kali dan tidak pernah mempercayai kolom. Dokumen
	 * yang sudah dicetak dapat menampilkan angka berbeda bila dicetak ulang
	 * setelah konfigurasi diubah. Perlakukan sakelar ini sebagai keputusan
	 * sekali-pasang saat instalasi.</p>
	 * <p>Perhatikan bahwa {@link #hitungPph()} sengaja <b>tidak</b> tunduk pada
	 * sakelar ini - lihat penjelasan di sana.</p>
	 *
	 * <h3>Sifat destruktif dan biaya baca</h3>
	 * <p>Hasil ditulis balik ke field {@link #hargaTotal}, sehingga kolom di basis
	 * data hanyalah cache dan pembacaan pada entitas terkelola dapat memicu
	 * UPDATE. Selain itu, satu pemanggilan method ini merantai pembacaan
	 * konfigurasi ditambah beberapa getter destruktif lain
	 * ({@link #getHargaBeli()}, {@link #getPersenPpn()} yang menembus ke
	 * {@link #getJenisPajakPpn()}, {@link #getPersenPph()} yang menembus ke
	 * {@link #getJenisPajakBarang()}). Layar PO memanggilnya sekali per baris saat
	 * menjumlahkan nilai header - salah satu titik terberat perenderan
	 * pesanan.</p>
	 *
	 * <h3>Variabel lokal yang menunjuk this</h3>
	 * <p>Badan method membuat variabel lokal
	 * <code>pemesananPengadaanMasterAssetDetail = this</code> lalu memanggil
	 * getter melaluinya. Ini tidak mengubah perilaku apa pun - sekadar peninggalan
	 * gaya dari saat logika ini dipindahkan ke sini dari kelas aksi, di mana objek
	 * memang datang sebagai parameter. Pola yang sama muncul di
	 * {@link #hitungPpn()} dan {@link #hitungPph()}.</p>
	 *
	 * @return nilai total baris setelah potongan dan pajak; tidak pernah
	 *         {@code null}, dapat bernilai negatif bila potongan melebihi DPP
	 * @see #hitungPpn()
	 * @see #hitungPph()
	 */
	public Double getHargaTotal() {

		boolean pph_mengurangi_lpj = Common.bolehKonfigurasi("pph_mengurangi_lpj");

		PemesananPengadaanMasterAssetDetail pemesananPengadaanMasterAssetDetail = this;
		Double dpp = (pemesananPengadaanMasterAssetDetail.getJumlah()
				* pemesananPengadaanMasterAssetDetail.getHargaBeli());

		Double potongan = getDiskonDalamBentukPersen()
				? ((pemesananPengadaanMasterAssetDetail.getHargaPotongan() / 100.0) * dpp)
				: pemesananPengadaanMasterAssetDetail.getHargaPotongan();
		dpp = dpp - potongan;

		Double ppn = ((pemesananPengadaanMasterAssetDetail.getPersenPpn() / 100.0) * dpp);

		Double pph = !pph_mengurangi_lpj ? 0.0 : ((pemesananPengadaanMasterAssetDetail.getPersenPph() / 100.0) * dpp);

		hargaTotal = (dpp + ppn) - (pph);

		return hargaTotal;
	}

	/**
	 * Menghitung <b>nominal PPN</b> baris ini, untuk ditampilkan pada kolom
	 * tersendiri dan direkap ke laporan pajak.
	 *
	 * <h3>Perhitungan</h3>
	 * <p>DPP kotor ({@link #getJumlah()} x {@link #getHargaBeli()}) dikurangi
	 * potongan - dibaca sebagai persen atau nominal menurut
	 * {@link #getDiskonDalamBentukPersen()} - lalu dikalikan
	 * {@link #getPersenPpn()} dibagi seratus. Rumusnya <b>identik</b> dengan
	 * bagian PPN di dalam {@link #getHargaTotal()}, disengaja agar nominal yang
	 * ditampilkan selalu cocok dengan yang tercakup dalam total.</p>
	 *
	 * <h3>Mengapa dipisah, bukan diambil dari getHargaTotal</h3>
	 * <p>{@link #getHargaTotal()} mengembalikan satu angka gabungan; nominal PPN
	 * di dalamnya tidak dapat dipulihkan kembali dengan aman karena PPh sudah ikut
	 * dikurangkan (bergantung konfigurasi). Method terpisah ini memberi angka PPN
	 * murni yang dibutuhkan cetakan faktur dan rekap pajak.</p>
	 *
	 * <p><b>Berbeda dari getter-getter di kelas ini, method ini tidak menulis
	 * apa pun ke field</b> - ia murni menghitung dan tidak memiliki kolom
	 * penyimpan. Namun ia tetap memanggil getter-getter destruktif
	 * ({@link #getHargaBeli()}, {@link #getPersenPpn()}), sehingga pemanggilannya
	 * tetap dapat mengotori entitas secara tidak langsung.</p>
	 *
	 * @return nominal PPN baris ini; tidak pernah {@code null}
	 * @see #hitungPph()
	 * @see #getHargaTotal()
	 */
	public Double hitungPpn() {
		PemesananPengadaanMasterAssetDetail pemesananPengadaanMasterAssetDetail = this;

		Double dpp = (pemesananPengadaanMasterAssetDetail.getJumlah()
				* pemesananPengadaanMasterAssetDetail.getHargaBeli());

		Double potongan = getDiskonDalamBentukPersen()
				? ((pemesananPengadaanMasterAssetDetail.getHargaPotongan() / 100.0) * dpp)
				: pemesananPengadaanMasterAssetDetail.getHargaPotongan();
		dpp = dpp - potongan;
		Double ppn = ((pemesananPengadaanMasterAssetDetail.getPersenPpn() / 100.0) * dpp);

		return ppn;
	}

	/**
	 * Menghitung <b>nominal PPh</b> baris ini, untuk ditampilkan pada kolom
	 * tersendiri.
	 *
	 * <h3>Perbedaan penting dari getHargaTotal</h3>
	 * <p>Komentar dalam badan method menegaskan hal yang paling mudah salah
	 * dipahami di kelas ini: nominal PPh yang dihitung di sini <b>tidak</b> tunduk
	 * pada konfigurasi <code>pph_mengurangi_lpj</code>. Sakelar itu hanya menjawab
	 * pertanyaan "apakah PPh <i>mengurangi</i> nilai total dan LPJ", yang
	 * dikendalikan terpisah di {@link #getHargaTotal()} dan di
	 * <code>PertangungjawabanAction</code>. Pertanyaan "berapa nominal PPh-nya"
	 * dijawab di sini, dan jawabannya harus tetap muncul di layar walau sakelar
	 * itu padam - sebab lembaga tetap wajib menyetorkan PPh meski tidak
	 * memotongnya dari pembayaran.</p>
	 * <p><b>Jangan menambahkan penjagaan konfigurasi ke method ini</b> dengan
	 * niat "menyeragamkan" dengan {@link #getHargaTotal()}; itu akan menghapus
	 * nominal PPh dari tampilan pada instalasi yang sakelarnya padam.</p>
	 *
	 * <h3>Perhitungan</h3>
	 * <p>DPP kotor dikurangi potongan - dibaca sebagai persen atau nominal menurut
	 * {@link #getDiskonDalamBentukPersen()} - lalu dikalikan
	 * {@link #getPersenPph()} dibagi seratus. Basis pengurang potongannya identik
	 * dengan {@link #hitungPpn()} dan dengan {@link #getHargaTotal()}, sehingga
	 * ketiga angka selalu berasal dari DPP bersih yang sama.</p>
	 *
	 * <p>Tidak menulis apa pun ke field, tetapi memanggil getter destruktif
	 * sehingga tetap dapat mengotori entitas secara tidak langsung.</p>
	 *
	 * @return nominal PPh baris ini; tidak pernah {@code null}
	 * @see #hitungPpn()
	 * @see #getHargaTotal()
	 */
	public Double hitungPph() {
		// N.PPH = NILAI PPH (nominal) untuk DITAMPILKAN di kolom. Apakah PPH mengurangi
		// total/LPJ dikontrol TERPISAH oleh konfigurasi pph_mengurangi_lpj (getHargaTotal /
		// PertangungjawabanAction). Jadi di sini PPH JANGAN di-nol-kan agar nilainya tetap
		// tampil walau konfigurasi tersebut nonaktif.
		PemesananPengadaanMasterAssetDetail pemesananPengadaanMasterAssetDetail = this;

		Double dpp = (pemesananPengadaanMasterAssetDetail.getJumlah()
				* pemesananPengadaanMasterAssetDetail.getHargaBeli());

		Double potongan = getDiskonDalamBentukPersen()
				? ((pemesananPengadaanMasterAssetDetail.getHargaPotongan() / 100.0) * dpp)
				: pemesananPengadaanMasterAssetDetail.getHargaPotongan();
		dpp = dpp - potongan;
		Double pph = ((pemesananPengadaanMasterAssetDetail.getPersenPph() / 100.0) * dpp);

		return pph;
	}

	/**
	 * Menyetel nilai total baris.
	 *
	 * <p><b>Praktis tidak berguna</b>: {@link #getHargaTotal()} selalu menghitung
	 * ulang dari nol, sehingga apa pun yang disetel di sini tertimpa pada
	 * pembacaan berikutnya. Setter tetap ada karena dibutuhkan Hibernate untuk
	 * memuat kolom.</p>
	 *
	 * @param hargaTotal nilai total (akan tertimpa saat dibaca)
	 */
	public void setHargaTotal(Double hargaTotal) {
		this.hargaTotal = hargaTotal;
	}

	/**
	 * Mengembalikan apakah {@link #getHargaPotongan()} harus dibaca sebagai
	 * <b>persentase</b> (true) atau <b>nominal</b> (false).
	 *
	 * <p><b>Perhatikan nilai bawaannya: {@code true}, bukan {@code false}.</b> Ini
	 * menyimpang dari pola umum bendera {@link Boolean} di paket ini - yang hampir
	 * seluruhnya menormalkan null menjadi {@code false} - dan menyimpangnya
	 * disengaja: potongan pada pengadaan lazimnya dinyatakan dalam persen, dan
	 * baris warisan yang kolom ini masih null berasal dari era ketika satu-satunya
	 * bentuk potongan adalah persen. Menyeragamkan nilai bawaan ini menjadi
	 * {@code false} akan <b>mengubah nilai seluruh dokumen lama</b>: potongan yang
	 * dimaksudkan 10 persen akan mendadak dibaca sebagai potongan 10 rupiah.
	 * Jangan diseragamkan.</p>
	 *
	 * <p>Bendera ini dibaca di tiga tempat yang harus tetap sepakat:
	 * {@link #getHargaTotal()}, {@link #hitungPpn()}, dan {@link #hitungPph()}.</p>
	 *
	 * @return {@code true} bila potongan dinyatakan dalam persen; tidak pernah
	 *         {@code null}
	 */
	public Boolean getDiskonDalamBentukPersen() {
		return diskonDalamBentukPersen == null ? true : diskonDalamBentukPersen;
	}

	/**
	 * Menyetel bentuk pembacaan potongan.
	 *
	 * @param diskonDalamBentukPersen {@code true} bila potongan dalam persen,
	 *                                {@code false} bila nominal
	 */
	public void setDiskonDalamBentukPersen(Boolean diskonDalamBentukPersen) {
		this.diskonDalamBentukPersen = diskonDalamBentukPersen;
	}

	/**
	 * Mengembalikan jenis PPN yang dikenakan pada baris ini.
	 *
	 * <h3>Tiga aturan berlapis</h3>
	 * <ol>
	 *   <li><b>Migrasi data warisan</b> - bila jenis pajak belum diisi tetapi
	 *       cache {@link #getPersenPpn()} bernilai tepat 11, jenis pajak diisi
	 *       dengan konstanta {@link JenisPajakPpn#PPN}. Ini menjembatani baris
	 *       lama dari era ketika hanya persentase yang disimpan, sebelum tabel
	 *       jenis PPN ada. Angka 11 adalah tarif PPN Indonesia yang berlaku sejak
	 *       April 2022 - <b>angka ajaib yang tertanam di kode</b>. Bila tarif
	 *       nasional berubah lagi, aturan ini tidak akan mengenali baris bertarif
	 *       baru; ia memang hanya dimaksudkan untuk memigrasi data lama, bukan
	 *       untuk menetapkan tarif berjalan.</li>
	 *   <li><b>Jalur normal</b> - bila jenis pajak sudah ada, ia di-<i>reattach</i>
	 *       lewat <code>check()</code>.</li>
	 *   <li><b>Pengambilalihan oleh header</b> - bila PO induk bertanda
	 *       {@link PemesananPengadaanMasterAsset#getPembelianLangsung()}, jenis
	 *       PPN diganti dengan {@link PemesananPengadaanMasterAsset#getJenisPajakPpnDp()}
	 *       milik header, <b>menimpa</b> apa pun hasil dua aturan sebelumnya.
	 *       Logikanya: pada pembelian langsung, nilai pesanan diambil alih dari DP
	 *       header, sehingga struktur pajaknya juga harus mengikuti DP agar total
	 *       baris dan nilai header tidak saling bertentangan.</li>
	 * </ol>
	 *
	 * <h3>Konsekuensi</h3>
	 * <p>Ketiga aturan menulis balik ke field, sehingga ini <b>getter
	 * destruktif</b> - pembacaan pada entitas terkelola dapat memicu UPDATE, dan
	 * pada PO pembelian langsung jenis PPN per baris akan tergantikan permanen
	 * oleh jenis PPN DP header. Perhatikan pula bahwa aturan ketiga memanggil
	 * {@link #getPemesananPengadaanMasterAsset()} dua kali; karena getter itu tidak
	 * <i>lazy</i>, pemanggilan ganda di sini tidak berisiko inkonsistensi.</p>
	 *
	 * <p>Nilai ini menjadi sumber bagi {@link #getPersenPpn()}, yang pada
	 * gilirannya dipakai {@link #getHargaTotal()} dan {@link #hitungPpn()}.</p>
	 *
	 * @return jenis PPN, atau {@code null} bila baris tidak ber-PPN
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pajak_ppn", nullable = true)
	public JenisPajakPpn getJenisPajakPpn() {
		if (jenisPajakPpn == null && persenPpn != null && persenPpn.intValue() == 11) {
			jenisPajakPpn = JenisPajakPpn.PPN;
		} else {
			jenisPajakPpn = check(jenisPajakPpn);
		}

		if (getPemesananPengadaanMasterAsset() != null && getPemesananPengadaanMasterAsset().getPembelianLangsung()) {
			jenisPajakPpn = getPemesananPengadaanMasterAsset().getJenisPajakPpnDp();
		}

		return jenisPajakPpn;
	}

	/**
	 * Menyetel jenis PPN untuk baris ini.
	 *
	 * <p><b>Setter dengan efek samping.</b> Berbeda dari setter lain di kelas ini,
	 * method ini <b>tidak hanya</b> menyimpan argumennya: ia lebih dulu memanggil
	 * {@link #setPersenPpn(Double)} dengan persentase jenis pajak tersebut - atau
	 * {@code 0.0} bila argumennya null - sehingga cache persentase langsung
	 * selaras tanpa menunggu {@link #getPersenPpn()} dibaca. Perilaku ini penting
	 * bagi layar PO yang menghitung ulang total baris seketika setelah pengguna
	 * mengganti jenis pajak pada combobox.</p>
	 *
	 * <p>Perhatikan asimetri dengan {@link #setJenisPajakBarang(JenisPajakBarang)}
	 * yang <b>tidak</b> menyegarkan cache PPh-nya. Bila kelak ditemukan bahwa
	 * persentase PPh tertinggal setelah penggantian jenis pajak barang, penyebabnya
	 * ada di sana, bukan di sini.</p>
	 *
	 * @param jenisPajakPpn jenis PPN, atau {@code null} untuk membebaskan baris
	 *                      dari PPN
	 */
	public void setJenisPajakPpn(JenisPajakPpn jenisPajakPpn) {
		setPersenPpn(jenisPajakPpn == null ? 0.0 : jenisPajakPpn.getPersen());
		this.jenisPajakPpn = jenisPajakPpn;
	}

	/**
	 * Mengembalikan kode gabungan penjamin keunikan baris, berformat
	 * <code>&lt;id baris&gt;_&lt;id master aset&gt;_&lt;id PO&gt;</code>.
	 *
	 * <h3>Fungsi</h3>
	 * <p>Kolom bertanda <code>unique</code> ini mencegah satu barang yang sama
	 * tercatat dua kali pada satu pesanan lewat baris berbeda - kesalahan yang
	 * mudah terjadi ketika baris ditarik dari beberapa permintaan sekaligus.
	 * Penegakannya berada di tingkat basis data, sehingga pelanggaran muncul
	 * sebagai galat constraint, bukan diterima diam-diam.</p>
	 *
	 * <h3>Jebakan: mengembalikan null untuk baris yang belum lengkap</h3>
	 * <p>Bila master aset <b>atau</b> header PO masih kosong, method mengembalikan
	 * {@code null} dan menuliskan null ke field. Perilaku ini disengaja - kode
	 * unik tak bermakna tanpa kedua komponennya - tetapi berdampak pada
	 * penegakannya: kolom <code>unique</code> pada sebagian besar basis data
	 * memperbolehkan <b>banyak baris bernilai NULL</b>, sehingga baris-baris yang
	 * belum lengkap lolos tanpa dibatasi. Penjagaan sesungguhnya baru bekerja
	 * setelah baris memiliki master aset dan header.</p>
	 * <p>Perhatikan pula bahwa {@link #getId()} ikut masuk ke dalam kode. Karena
	 * id setiap baris berbeda, kode unik dua baris <b>tidak akan pernah</b>
	 * bertabrakan meski barang dan PO-nya sama - artinya constraint ini pada
	 * praktiknya tidak mencegah duplikasi barang dalam satu PO seperti yang
	 * mungkin diharapkan dari namanya. Ia hanya menjamin keunikan yang sudah
	 * dijamin kunci utama. Jangan mengandalkannya sebagai penjaga duplikasi;
	 * pencegahan duplikasi nyata dilakukan layar PO lewat pencarian baris yang
	 * sudah ada sebelum membuat baris baru.</p>
	 *
	 * <p><b>Getter destruktif</b>: nilai selalu dihitung ulang dan ditulis balik,
	 * sehingga kolom di basis data hanyalah cache.</p>
	 *
	 * @return kode unik gabungan, atau {@code null} bila baris belum memiliki
	 *         master aset atau header
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		if (getMasterAsset() != null && getPemesananPengadaanMasterAsset() != null) {
			kodeUnik = getId() + "_" + getMasterAsset().getId() + "_" + getPemesananPengadaanMasterAsset().getId();
		} else {
			kodeUnik = null;
		}
		return kodeUnik;
	}

	/**
	 * Menyetel kode unik baris.
	 *
	 * <p><b>Praktis tidak berguna</b>: {@link #getKodeUnik()} selalu menghitung
	 * ulang. Setter tetap ada karena dibutuhkan Hibernate untuk memuat kolom.</p>
	 *
	 * @param kodeUnik kode unik (akan tertimpa saat dibaca)
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}
}
