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

import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.UangMuka;
import ais.database.model.rab.SatuanKerja;

/**
 * Baris item pada dokumen <b>Permintaan Pengadaan Barang/Jasa</b> (PR) modul
 * Aset Tetap: satu baris menyatakan satu jenis barang/jasa
 * ({@link MasterAsset}) beserta jumlah dan perkiraan harga belinya.
 *
 * <h3>Peran dalam rantai pengadaan</h3>
 * <p>Baris inilah - bukan headernya - yang menjadi <b>satuan kerja sebenarnya</b>
 * dalam rantai pengadaan aset. Pengguna memilih baris PR satu per satu (bukan
 * seluruh dokumen) ketika menerbitkan Pemesanan, sehingga satu dokumen PR dapat
 * terpecah ke beberapa PO. Karena itu seluruh perhitungan realisasi permintaan
 * harus bertumpu pada entitas ini, bukan pada FK tunggal di
 * {@link PermintaanPengadaanMasterAsset#getPemesananPengadaanMasterAsset()} yang
 * hanya mencatat PO pertama.</p>
 *
 * <h3>Jaring relasi</h3>
 * <p>Baris ini merupakan simpul dengan relasi terbanyak di antara dokumen awal
 * pengadaan. Ia menunjuk ke:</p>
 * <ul>
 *   <li>{@link #getPermintaanPengadaanMasterAsset()} - header PR induknya;</li>
 *   <li>{@link #getMasterAsset()} - katalog barang/jasa yang diminta;</li>
 *   <li>{@link #getPemesananPengadaanMasterAssetDetail()} - baris PO yang
 *       merealisasikan baris ini (arah PR-&gt;PO), pasangan dari
 *       {@link PemesananPengadaanMasterAssetDetail#getPermintaanPengadaanMasterAssetDetail()}
 *       yang menunjuk balik;</li>
 *   <li>{@link #getPerjanjianKerjasamaMasterAsset()} dan
 *       {@link #getPerjanjianKerjasamaMasterAssetDetail()} - kontrak payung bila
 *       pengadaan melewatinya;</li>
 *   <li>{@link #getUangMuka()} - dokumen uang muka bila kebutuhan ini dipenuhi
 *       lewat mekanisme panjar, bukan lewat PO ke penyedia;</li>
 *   <li>{@link #getAsset()} - aset yang akhirnya terbentuk.</li>
 * </ul>
 * <p>Jadi jawabannya jelas untuk modul ini: rantai permintaan-ke-pemesanan
 * memakai <b>kunci asing nyata dua arah</b>, bukan antrean kerja tanpa jejak
 * seperti pada <code>PengajuanPembelianGudang</code> di paket
 * <code>inventory</code>.</p>
 *
 * <h3>Peringatan: relasi ke baris PO bertipe TUNGGAL</h3>
 * <p>{@link #getPemesananPengadaanMasterAssetDetail()} hanya dapat menyimpan satu
 * baris PO, padahal satu baris permintaan bisa dipesan bertahap (misalnya 10 unit
 * dipesan 6 dulu, sisanya menyusul). Kode penyimpanan PO menimpa nilai ini setiap
 * kali baris PR ditarik ke PO baru, sehingga kaitan ke PO sebelumnya
 * <b>hilang</b>. Untuk menelusuri seluruh PO yang menyentuh satu baris PR,
 * lakukan kueri balik pada {@link PemesananPengadaanMasterAssetDetail} - persis
 * yang dilakukan <code>AmbilDataPermintaanPengadaanMasterAssetBanyak</code>.</p>
 *
 * <h3>Peringatan integritas: sisa yang belum dipesan dihitung dari BARANG DATANG</h3>
 * <p>Lihat {@link #getJumlahDatang()}. Penanda "sudah terpenuhi" pada baris ini
 * berbasis jumlah yang <b>sudah diterima</b> (BAST), bukan jumlah yang
 * <b>sudah dipesan</b>. Akibatnya baris PR yang seluruh kuantitasnya sudah
 * dipesan namun barangnya belum datang masih dapat ditarik ke PO berikutnya, dan
 * layar PO akan mengusulkan kembali kuantitas penuh. Rinciannya didokumentasikan
 * pada getter tersebut.</p>
 *
 * <h3>Catatan teknis</h3>
 * <ul>
 *   <li>Mewarisi {@link GeneralValueObject} (bukan <code>DataSop</code> seperti
 *       headernya) - baris tidak menjalani alur persetujuan sendiri, ia mengikuti
 *       status headernya. Dari kelas induk itu pula datang helper
 *       <code>check()</code> untuk me-<i>reattach</i> proxy Hibernate.</li>
 *   <li>Ber-anotasi {@link Audited} (Envers), sehingga perubahan kuantitas dan
 *       harga terekam pada tabel revisi.</li>
 *   <li>Beberapa getter bersifat <b>destruktif</b> dan/atau <b>menurunkan nilai
 *       dari entitas lain</b>, sehingga nilai yang disetel lewat setter dapat
 *       berbeda dari nilai yang dibaca kembali. Setiap kasus ditandai pada
 *       getternya masing-masing.</li>
 * </ul>
 *
 * @see PermintaanPengadaanMasterAsset header dokumen PR
 * @see PemesananPengadaanMasterAssetDetail baris PO penerus rantai
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "asset", name = "permintaan_pengadaan_master_asset_detail")
public class PermintaanPengadaanMasterAssetDetail extends GeneralValueObject {

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
	 * {@link #getOlehId()}, {@link #getTanggal_dirubah()}) yang diisi otomatis
	 * oleh <code>AuditTimestampInterceptor</code>. Trio ini tampak berlebihan di
	 * samping {@link Audited} milik Envers, namun merupakan <b>keharusan
	 * teknis</b>: layar daftar dan laporan perlu menampilkan jejak "diubah oleh
	 * siapa" untuk banyak baris sekaligus tanpa join ke tabel revisi.</p>
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
	 * {@link #getMasterAsset()} dan menugaskan hasilnya kembali ke field
	 * {@link #masterAsset}. Artinya sekadar mencetak baris ini ke log atau
	 * menampilkannya di komponen ZK dapat memicu <i>reattach</i> proxy dan
	 * SELECT tambahan ke tabel master aset. Perilaku ini tidak lazim untuk
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

	/** Katalog barang/jasa yang diminta. Lihat {@link #getMasterAsset()}. */
	private MasterAsset masterAsset;
	/** Kuantitas yang diminta. Lihat {@link #getJumlah()}. */
	private Double jumlah;
	/** Perkiraan harga satuan. Lihat {@link #getHargaBeli()}. */
	private Double hargaBeli;
	/** Hasil kali jumlah x harga beli (selalu dihitung ulang). Lihat {@link #getHargaTotal()}. */
	private Double hargaTotal;
	/** Header PR induk. Lihat {@link #getPermintaanPengadaanMasterAsset()}. */
	private PermintaanPengadaanMasterAsset permintaanPengadaanMasterAsset;
	/** Baris PO terakhir yang merealisasikan baris ini. Lihat {@link #getPemesananPengadaanMasterAssetDetail()}. */
	private PemesananPengadaanMasterAssetDetail pemesananPengadaanMasterAssetDetail;
	/** Kontrak payung (header). Lihat {@link #getPerjanjianKerjasamaMasterAsset()}. */
	private PerjanjianKerjasamaMasterAsset perjanjianKerjasamaMasterAsset;
	/** Kontrak payung (baris). Lihat {@link #getPerjanjianKerjasamaMasterAssetDetail()}. */
	private PerjanjianKerjasamaMasterAssetDetail perjanjianKerjasamaMasterAssetDetail;
	/** Dokumen uang muka bila kebutuhan dipenuhi lewat panjar. Lihat {@link #getUangMuka()}. */
	private UangMuka uangMuka;
	/** Uraian bebas baris. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Cache kuantitas yang SUDAH DITERIMA. Lihat {@link #getJumlahDatang()}. */
	private Double jumlahDatang;
	/** Satuan kerja, diturunkan dari header. Lihat {@link #getSatuanKerja()}. */
	private SatuanKerja satuanKerja;
	/** Tanggal pembuatan, diturunkan dari header. Lihat {@link #getTanggalPembuatan()}. */
	private Date tanggalPembuatan;
	/** Aset yang akhirnya terbentuk dari baris ini. Lihat {@link #getAsset()}. */
	private Asset asset;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate untuk instansiasi
	 * lewat refleksi, sekaligus dipakai kode aksi untuk menambah baris baru pada
	 * grid. Seluruh field dibiarkan null; getter berpelindung memasok nilai bawaan
	 * saat pertama dibaca (misalnya {@link #getJumlah()} yang memberi
	 * {@code 1.0}).
	 */
	public PermintaanPengadaanMasterAssetDetail() {
	}

	/**
	 * Mengembalikan kunci utama baris.
	 *
	 * <p>Kolom bertanda <code>insertable = false</code> karena nilainya
	 * dibangkitkan basis data (IDENTITY). Nilai {@code null} berarti baris belum
	 * tersimpan.</p>
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
	 * Mengembalikan uraian bebas untuk baris ini, misalnya spesifikasi tambahan
	 * atau alasan kebutuhan.
	 *
	 * <p>Nilai ini <b>disalin ke baris PO</b> saat permintaan ditarik menjadi
	 * pemesanan (lihat <code>PemesananPengadaanMasterAssetAction.generateDetail()</code>),
	 * sehingga spesifikasi yang ditulis pemohon ikut sampai ke penyedia.</p>
	 *
	 * <p>Berbeda dari keterangan header yang dipetakan sebagai <code>text</code>,
	 * kolom ini memakai tipe varchar bawaan sehingga panjangnya terbatas.</p>
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
	 * Menyetel katalog barang/jasa yang diminta.
	 *
	 * @param masterAsset entri katalog {@link MasterAsset}
	 */
	public void setMasterAsset(MasterAsset masterAsset) {
		this.masterAsset = masterAsset;
	}

	/**
	 * Mengembalikan entri katalog barang/jasa yang diminta pada baris ini.
	 *
	 * <p>Ini properti terpenting baris: ia menentukan apa yang dibeli, memasok
	 * harga bawaan lewat {@link #getHargaBeli()}, dan ikut membentuk kode unik
	 * baris PO yang menurunkan baris ini.</p>
	 *
	 * <p><b>Getter destruktif</b>: memanggil <code>check()</code> untuk
	 * me-<i>reattach</i> proxy yang sesinya sudah tertutup, lalu menulis hasilnya
	 * ke field. Perhatikan bahwa {@link #getHargaBeli()} membaca <b>field mentah</b>
	 * {@link #masterAsset} dan bukan getter ini - lihat catatan di sana.</p>
	 *
	 * <p>Kolom pemetaannya bernama <code>masterAsset</code> (gaya camelCase),
	 * menyimpang dari konvensi <i>snake_case</i> yang dipakai kolom lain di tabel
	 * ini. Ini peninggalan pembangkitan hbm2java; jangan "dirapikan" tanpa migrasi
	 * basis data.</p>
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
	 * Menyetel kuantitas yang diminta.
	 *
	 * <p>Nilai yang disetel dapat <b>ditimpa menjadi {@code 1.0}</b> saat dibaca
	 * bila baris ini terkait uang muka yang sudah dipertanggungjawabkan - lihat
	 * {@link #getJumlah()}.</p>
	 *
	 * @param jumlah kuantitas yang diminta
	 */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * Mengembalikan kuantitas yang diminta pada baris ini.
	 *
	 * <h3>Aturan khusus: baris uang muka dipaksa berkuantitas satu</h3>
	 * <p>Bila baris ini terkait sebuah {@link UangMuka} yang
	 * pertanggungjawabannya (LPJ) <b>sudah disetujui</b>, kuantitas dipaksa
	 * menjadi {@code 1.0}. Alasannya: begitu LPJ disetujui, baris tidak lagi
	 * mewakili "sekian unit barang" melainkan satu nilai realisasi tunggal yang
	 * diambil dari LPJ - dan {@link #getHargaBeli()} pada saat yang sama diganti
	 * dengan nilai LPJ tersebut. Pasangan aturan itu membuat
	 * {@link #getHargaTotal()} (yaitu jumlah x harga) menghasilkan tepat nilai
	 * realisasi LPJ, tidak berlipat.</p>
	 * <p><b>Konsekuensi yang harus disadari</b>: sejak LPJ disetujui, kuantitas
	 * asli permintaan <b>tidak lagi dapat dibaca</b> lewat getter ini. Nilainya
	 * masih tersimpan di kolom (getter ini tidak menulis balik, hanya menugaskan
	 * ke field lokal sebelum <code>return</code> - dan penugasan itu memang
	 * mengotori entitas, lihat di bawah), tetapi setiap pembacaan berikutnya akan
	 * mengembalikan 1.0. Untuk keperluan audit kuantitas asli, andalkan tabel
	 * revisi Envers.</p>
	 *
	 * <h3>Sifat destruktif</h3>
	 * <p>Penugasan <code>jumlah = 1.0</code> menulis ke field pada entitas yang
	 * mungkin sedang dikelola sesi Hibernate, sehingga pembacaan biasa dapat
	 * memicu UPDATE kolom <code>jumlah</code>. Rantai pemanggilannya juga panjang
	 * - {@link #getUangMuka()} dipanggil tiga kali berturut-turut dalam satu
	 * ekspresi, masing-masing berpotensi memuat data secara <i>lazy</i>. Bila
	 * method ini disunting, ikuti pola perbaikan yang sudah diterapkan pada
	 * {@link PermintaanPengadaanMasterAsset#getDisetujuiOleh()}: ambil sekali ke
	 * variabel lokal agar hasil null-check dan dereference selalu konsisten.</p>
	 *
	 * <h3>Nilai bawaan</h3>
	 * <p>Bila field masih null, dikembalikan {@code 1.0} - bukan {@code 0.0}.
	 * Pilihan ini disengaja agar baris yang baru ditambahkan langsung berkuantitas
	 * masuk akal. Perhatikan efek sampingnya pada validasi: layar PR menolak
	 * persetujuan bila ada baris dengan <code>jumlah &lt; 1.0</code> di basis
	 * data, dan pemeriksaan itu berupa kueri SQL atas <b>kolom</b>, sehingga baris
	 * yang kolomnya benar-benar null tidak tertangkap oleh pemeriksaan tersebut
	 * meskipun getter ini menyamarkannya sebagai 1.0.</p>
	 *
	 * @return kuantitas yang diminta; tidak pernah {@code null}, minimal
	 *         {@code 1.0} untuk baris baru
	 */
	public Double getJumlah() {

		if (getUangMuka() != null && getUangMuka().getPertangungjawaban() != null
				&& getUangMuka().getPertangungjawaban().getDisetujuiOleh() != null) {
			jumlah = 1.0;
		}

		return jumlah == null ? 1.0 : jumlah;
	}

	/**
	 * Menyetel header PR induk baris ini.
	 *
	 * <p>Wajib diisi sebelum baris disimpan; kolomnya memang <i>nullable</i> di
	 * basis data, tetapi baris tanpa header menjadi <b>baris yatim</b> yang tidak
	 * akan pernah muncul di layar mana pun karena semua kueri baris menyaring
	 * lewat headernya.</p>
	 *
	 * @param permintaanPengadaanMasterAsset header PR induk
	 */
	public void setPermintaanPengadaanMasterAsset(PermintaanPengadaanMasterAsset permintaanPengadaanMasterAsset) {
		this.permintaanPengadaanMasterAsset = permintaanPengadaanMasterAsset;
	}

	/**
	 * Mengembalikan header PR induk baris ini.
	 *
	 * <p>Menjadi sumber bagi dua properti turunan: {@link #getSatuanKerja()} dan
	 * {@link #getTanggalPembuatan()} keduanya mengambil nilainya dari header ini,
	 * menimpa apa pun yang tersimpan di kolom baris.</p>
	 *
	 * <p><b>Tidak memanggil <code>check()</code></b> - mengembalikan field mentah.
	 * Aman karena relasi dipetakan tanpa <code>fetch = LAZY</code>, sehingga
	 * Hibernate memuatnya seketika lewat SELECT terpisah
	 * (<code>@Fetch(FetchMode.SELECT)</code>). Harganya adalah satu kueri tambahan
	 * per baris yang dirender - salah satu sumber pola N+1 pada layar daftar
	 * baris PR.</p>
	 *
	 * @return header PR induk, atau {@code null} untuk baris yatim
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "permintaan_pengadaan_master_asset", nullable = true)
	public PermintaanPengadaanMasterAsset getPermintaanPengadaanMasterAsset() {
		return permintaanPengadaanMasterAsset;
	}

	/**
	 * Mengembalikan perkiraan harga satuan barang/jasa pada baris ini.
	 *
	 * <h3>Tiga sumber nilai, berurutan</h3>
	 * <ol>
	 *   <li><b>Kolom baris</b>, bila sudah terisi dan bukan nol - yaitu harga yang
	 *       diketik pemohon.</li>
	 *   <li><b>Harga bawaan katalog</b>, bila kolom masih kosong atau nol: diambil
	 *       dari <code>MasterAsset.getHargaBeliDefault()</code> dan
	 *       <b>ditulis balik</b> ke field. Ini yang membuat baris baru langsung
	 *       menampilkan harga wajar tanpa pemohon perlu mengetiknya.</li>
	 *   <li><b>Nilai LPJ</b>, bila baris terkait {@link UangMuka} yang
	 *       pertanggungjawabannya sudah disetujui: harga diganti dengan nilai
	 *       realisasi LPJ, menimpa kedua sumber di atas. Bersama
	 *       {@link #getJumlah()} yang dipaksa {@code 1.0}, pasangan ini membuat
	 *       {@link #getHargaTotal()} menghasilkan tepat nilai realisasi.</li>
	 * </ol>
	 *
	 * <h3>Jebakan: membaca field mentah, bukan getter</h3>
	 * <p>Perhatikan syarat cabang kedua: <code>masterAsset != null</code> - ia
	 * memeriksa <b>field</b>, bukan {@link #getMasterAsset()}. Bedanya bermakna:
	 * field tidak melewati <code>check()</code>, sehingga bila proxy master aset
	 * berasal dari sesi yang sudah tertutup, pemanggilan
	 * <code>getHargaBeliDefault()</code> di baris berikutnya dapat melempar
	 * <code>LazyInitializationException</code> - persis jenis kegagalan yang
	 * <code>check()</code> ada untuk mencegahnya. Dalam praktik hal ini jarang
	 * terjadi karena {@link #getMasterAsset()} biasanya sudah dipanggil lebih dulu
	 * oleh perenderan grid (dan oleh {@link #toString()}), yang menyegarkan field.
	 * Bila method ini disunting, ganti pemeriksaannya menjadi
	 * <code>getMasterAsset() != null</code>.</p>
	 *
	 * <h3>Sifat destruktif dan rantai lazy</h3>
	 * <p>Kedua cabang menulis ke field {@link #hargaBeli}, sehingga membaca harga
	 * pada entitas terkelola dapat memicu UPDATE. Selain itu
	 * {@link #getUangMuka()} dipanggil tiga kali berturut-turut; berlaku peringatan
	 * yang sama seperti pada {@link #getJumlah()}.</p>
	 *
	 * @return harga satuan; tidak pernah {@code null}, {@code 0.0} bila tak ada
	 *         sumber nilai
	 */
	public Double getHargaBeli() {
		if ((hargaBeli == null || hargaBeli == 0.0) && masterAsset != null) {
			hargaBeli = masterAsset.getHargaBeliDefault();
		}

		if (getUangMuka() != null && getUangMuka().getPertangungjawaban() != null
				&& getUangMuka().getPertangungjawaban().getDisetujuiOleh() != null) {
			hargaBeli = getUangMuka().getPertangungjawaban().getNilai();
		}

		return hargaBeli == null ? 0.0 : hargaBeli;
	}

	/**
	 * Menyetel perkiraan harga satuan.
	 *
	 * <p>Nilai yang disetel dapat ditimpa saat dibaca oleh harga bawaan katalog
	 * (bila disetel nol) atau oleh nilai LPJ - lihat {@link #getHargaBeli()}.</p>
	 *
	 * @param hargaBeli harga satuan
	 */
	public void setHargaBeli(Double hargaBeli) {
		this.hargaBeli = hargaBeli;
	}

	/**
	 * Mengembalikan baris Pemesanan (PO) yang merealisasikan baris permintaan ini.
	 *
	 * <h3>Kaitan dua arah PR-&gt;PO</h3>
	 * <p>Kolom <code>pemesanan_pengadaan_master_asset_detail</code> ini adalah
	 * pasangan dari
	 * {@link PemesananPengadaanMasterAssetDetail#getPermintaanPengadaanMasterAssetDetail()}
	 * yang menunjuk balik. Keduanya ditulis <b>bersamaan</b> oleh
	 * <code>PemesananPengadaanMasterAssetAction.onSave()</code>: setelah baris PO
	 * disimpan dan di-flush, kode mengambil instans baris PR yang dikelola sesi
	 * lewat <code>session.get(id)</code>, menyetel kaitan ini, lalu menyimpannya.
	 * Pengambilan ulang lewat <code>get()</code> itu sengaja dilakukan untuk
	 * menghindari <code>NonUniqueObjectException</code> - objek dari graf baris PO
	 * bisa berupa instans <i>detached</i> ber-id sama dengan instans lain yang
	 * sudah termuat di sesi.</p>
	 *
	 * <h3>Peringatan: bertipe TUNGGAL, bersifat timpa-terakhir</h3>
	 * <p>Satu baris permintaan dapat dipesan bertahap lewat beberapa PO, tetapi
	 * kolom ini hanya menampung satu nilai dan <b>ditimpa</b> setiap kali baris PR
	 * ditarik ke PO baru. Kaitan ke PO sebelumnya hilang dari sisi ini. Untuk
	 * menelusuri seluruh PO yang menyentuh baris PR tertentu, lakukan kueri balik:
	 * <code>createCriteria(PemesananPengadaanMasterAssetDetail.class).add(
	 * Restrictions.eq("permintaanPengadaanMasterAssetDetail", baris))</code> -
	 * persis yang dilakukan pemilih PR pada layar PO untuk menghitung total yang
	 * sudah diterima.</p>
	 *
	 * <p><b>Tidak memanggil <code>check()</code></b>; dimuat seketika lewat SELECT
	 * terpisah.</p>
	 *
	 * @return baris PO terakhir yang merealisasikan baris ini, atau {@code null}
	 *         bila belum pernah dipesan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pemesanan_pengadaan_master_asset_detail", nullable = true)
	public PemesananPengadaanMasterAssetDetail getPemesananPengadaanMasterAssetDetail() {
		return pemesananPengadaanMasterAssetDetail;
	}

	/**
	 * Menyetel baris Pemesanan yang merealisasikan baris permintaan ini.
	 *
	 * <p>Dipanggil dari dua tempat: saat layar PO membangun baris pemesanan dari
	 * baris permintaan (<code>generateDetail()</code>), dan saat PO disimpan
	 * (<code>onSave()</code>). Menimpa nilai sebelumnya tanpa peringatan - lihat
	 * {@link #getPemesananPengadaanMasterAssetDetail()}.</p>
	 *
	 * @param pemesananPengadaanMasterAssetDetail baris PO
	 */
	public void setPemesananPengadaanMasterAssetDetail(
			PemesananPengadaanMasterAssetDetail pemesananPengadaanMasterAssetDetail) {
		this.pemesananPengadaanMasterAssetDetail = pemesananPengadaanMasterAssetDetail;
	}

	/**
	 * Mengembalikan baris perjanjian kerjasama (kontrak payung) yang menaungi
	 * baris permintaan ini.
	 *
	 * <p>Bila permintaan diproses lewat kontrak payung, alurnya menjadi
	 * PR -&gt; Perjanjian Kerjasama -&gt; PO alih-alih PR -&gt; PO langsung. Baris
	 * kontrak menyimpan kaitan balik ke baris PR ini, dan
	 * {@link PemesananPengadaanMasterAssetDetail#getPermintaanPengadaanMasterAssetDetail()}
	 * memanfaatkan kaitan itu untuk memulihkan asal-usul permintaan meski baris PO
	 * dibentuk dari kontrak, bukan langsung dari PR.</p>
	 *
	 * <p><b>Tidak memanggil <code>check()</code></b>; dimuat seketika lewat SELECT
	 * terpisah.</p>
	 *
	 * @return baris kontrak payung, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "perjanjian_kerjasama_master_asset_detail", nullable = true)
	public PerjanjianKerjasamaMasterAssetDetail getPerjanjianKerjasamaMasterAssetDetail() {
		return perjanjianKerjasamaMasterAssetDetail;
	}

	/**
	 * Menyetel baris perjanjian kerjasama yang menaungi baris permintaan ini.
	 *
	 * @param perjanjianKerjasamaMasterAssetDetail baris kontrak payung
	 */
	public void setPerjanjianKerjasamaMasterAssetDetail(
			PerjanjianKerjasamaMasterAssetDetail perjanjianKerjasamaMasterAssetDetail) {
		this.perjanjianKerjasamaMasterAssetDetail = perjanjianKerjasamaMasterAssetDetail;
	}

	/**
	 * Mengembalikan dokumen uang muka (panjar) yang membiayai baris permintaan
	 * ini.
	 *
	 * <h3>Jalur pemenuhan alternatif</h3>
	 * <p>Tidak semua kebutuhan dipenuhi lewat PO ke penyedia. Sebagian dipenuhi
	 * dengan memberi panjar kepada pelaksana, yang kemudian
	 * mempertanggungjawabkannya lewat LPJ. Bila baris ini menempuh jalur tersebut,
	 * kolom ini terisi - dan kehadirannya <b>mengubah perilaku dua getter lain</b>
	 * begitu LPJ disetujui: {@link #getJumlah()} dipaksa {@code 1.0} dan
	 * {@link #getHargaBeli()} diganti nilai realisasi LPJ.</p>
	 *
	 * <h3>Menghalangi pembelian langsung</h3>
	 * <p>Layar PR menyembunyikan tombol "Beli Langsung" bila ada satu saja baris
	 * pada dokumen yang kolom ini terisi. Logikanya jelas: kebutuhan yang sudah
	 * dibiayai panjar tidak boleh dipesankan ulang lewat PO, karena akan
	 * membebankan biaya dua kali.</p>
	 *
	 * <p><b>Tidak memanggil <code>check()</code></b>; dimuat seketika lewat SELECT
	 * terpisah. Perhatikan bahwa {@link #getJumlah()} dan {@link #getHargaBeli()}
	 * memanggil getter ini masing-masing tiga kali dalam satu ekspresi.</p>
	 *
	 * @return dokumen uang muka, atau {@code null} bila baris ditempuh lewat PO
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "uang_muka", nullable = true)
	public UangMuka getUangMuka() {
		return uangMuka;
	}

	/**
	 * Menyetel dokumen uang muka yang membiayai baris permintaan ini.
	 *
	 * <p><b>Peringatan</b>: menyetel nilai ini pada baris yang LPJ-nya sudah
	 * disetujui akan langsung mengubah kuantitas dan harga baris saat dibaca
	 * berikutnya. Lihat {@link #getJumlah()} dan {@link #getHargaBeli()}.</p>
	 *
	 * @param uangMuka dokumen uang muka
	 */
	public void setUangMuka(UangMuka uangMuka) {
		this.uangMuka = uangMuka;
	}

	/**
	 * Mengembalikan nilai total baris, yaitu harga satuan dikali kuantitas.
	 *
	 * <p><b>Selalu dihitung ulang</b> dari {@link #getHargaBeli()} dan
	 * {@link #getJumlah()}; nilai kolom tidak pernah dipercaya. Hasilnya kemudian
	 * ditulis balik ke field, sehingga kolom di basis data berfungsi sebagai cache
	 * dari perhitungan ini - dan pembacaan pada entitas terkelola dapat memicu
	 * UPDATE.</p>
	 *
	 * <p>Perhitungannya sengaja dibuat sesederhana mungkin: <b>tanpa pajak dan
	 * tanpa diskon</b>. Ini membedakannya secara tajam dari
	 * {@link PemesananPengadaanMasterAssetDetail#getHargaTotal()} yang
	 * memperhitungkan potongan, PPN, dan (bergantung konfigurasi) PPh. Perbedaan
	 * itu masuk akal secara bisnis - pada tahap permintaan, harga masih perkiraan
	 * dan struktur pajaknya belum diketahui karena penyedia belum dipilih -
	 * tetapi berarti nilai total PR dan nilai total PO atas barang yang sama
	 * <b>tidak seharusnya diharapkan sama</b>. Jangan menulis rekonsiliasi yang
	 * mengasumsikan keduanya cocok.</p>
	 *
	 * <p>Karena bertumpu pada dua getter yang keduanya berperilaku khusus untuk
	 * baris uang muka, nilai total baris yang LPJ-nya disetujui akan sama persis
	 * dengan nilai realisasi LPJ (1.0 x nilai LPJ).</p>
	 *
	 * @return nilai total baris; tidak pernah {@code null}
	 */
	public Double getHargaTotal() {
		hargaTotal = getHargaBeli() * getJumlah();
		return hargaTotal;
	}

	/**
	 * Menyetel nilai total baris.
	 *
	 * <p><b>Praktis tidak berguna</b>: {@link #getHargaTotal()} selalu menghitung
	 * ulang, sehingga apa pun yang disetel di sini tertimpa pada pembacaan
	 * berikutnya. Setter tetap ada karena dibutuhkan Hibernate untuk memuat
	 * kolom.</p>
	 *
	 * @param hargaTotal nilai total (akan tertimpa saat dibaca)
	 */
	public void setHargaTotal(Double hargaTotal) {
		this.hargaTotal = hargaTotal;
	}

	/**
	 * Mengembalikan satuan kerja pemilik baris, diturunkan dari headernya.
	 *
	 * <p>Bila header ada, satuan kerja header <b>selalu menang</b> dan menimpa
	 * nilai kolom baris. Duplikasi kolom di tingkat baris ini bukan kesalahan
	 * normalisasi melainkan keperluan pelaporan: laporan yang mengagregasi baris
	 * lintas dokumen dapat memfilter satuan kerja langsung di tabel baris tanpa
	 * join ke header.</p>
	 *
	 * <p><b>Getter destruktif</b>: hasil derivasi ditulis balik ke field, sehingga
	 * kolom baris perlahan menyelaraskan diri dengan header. Perhatikan bahwa
	 * berbeda dari getter relasi lain di kelas ini, method ini <b>tidak</b>
	 * memanggil <code>check()</code> atas field sendiri - bila header bernilai
	 * null, field dikembalikan apa adanya tanpa <i>reattach</i>, sehingga baris
	 * yatim yang dibaca di luar sesi asalnya berisiko
	 * <code>LazyInitializationException</code>.</p>
	 *
	 * <p>Perhatikan pula bahwa {@link #getPermintaanPengadaanMasterAsset()}
	 * dipanggil dua kali; karena getter itu tidak <i>lazy</i>, pemanggilan ganda
	 * di sini tidak menimbulkan risiko NPE seperti pada rantai
	 * {@link #getUangMuka()}.</p>
	 *
	 * @return satuan kerja pemilik baris, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		if (getPermintaanPengadaanMasterAsset() != null) {
			satuanKerja = getPermintaanPengadaanMasterAsset().getSatuanKerja();
		}
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Menyetel satuan kerja pemilik baris.
	 *
	 * <p>Nilai yang disetel akan ditimpa oleh satuan kerja header saat dibaca,
	 * selama header baris ini tidak null.</p>
	 *
	 * @param satuanKerja satuan kerja
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Mengembalikan tanggal pembuatan baris, diturunkan dari headernya.
	 *
	 * <p>Sama seperti {@link #getSatuanKerja()}, ini duplikasi yang disengaja agar
	 * laporan dapat memfilter periode langsung di tabel baris. Nilai header selalu
	 * menang bila header ada.</p>
	 *
	 * <p><b>Getter destruktif</b>. Perhatikan satu efek berantai yang mudah
	 * terlewat: {@link PermintaanPengadaanMasterAsset#getTanggalPembuatan()}
	 * <b>tidak pernah mengembalikan null</b> - bila kosong ia memberi waktu server
	 * saat ini. Akibatnya, membaca properti ini pada baris yang headernya belum
	 * bertanggal akan menuliskan <i>waktu saat pembacaan</i> ke kolom baris,
	 * sekalipun kolom header sendiri tetap kosong. Untuk periode yang dapat
	 * dipertanggungjawabkan, andalkan kolom header atau tabel revisi Envers.</p>
	 *
	 * @return tanggal pembuatan, atau {@code null} bila baris yatim dan kolomnya
	 *         kosong
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		if (getPermintaanPengadaanMasterAsset() != null) {
			tanggalPembuatan = getPermintaanPengadaanMasterAsset().getTanggalPembuatan();
		}
		return tanggalPembuatan;
	}

	/**
	 * Menyetel tanggal pembuatan baris.
	 *
	 * <p>Nilai yang disetel akan ditimpa oleh tanggal header saat dibaca.</p>
	 *
	 * @param tanggalPembuatan tanggal pembuatan
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Mengembalikan header perjanjian kerjasama (kontrak payung) yang menaungi
	 * baris ini.
	 *
	 * <p>Pelengkap {@link #getPerjanjianKerjasamaMasterAssetDetail()} pada tingkat
	 * dokumen. Menyimpan keduanya memang redundan - header dapat diperoleh dari
	 * barisnya - namun kaitan tingkat header memudahkan kueri "permintaan apa saja
	 * yang tercakup kontrak X" tanpa join berlapis.</p>
	 *
	 * <p><b>Tidak memanggil <code>check()</code></b>; dimuat seketika lewat SELECT
	 * terpisah.</p>
	 *
	 * @return header kontrak payung, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "perjanjian_kerjasama_master_asset", nullable = true)
	public PerjanjianKerjasamaMasterAsset getPerjanjianKerjasamaMasterAsset() {
		return perjanjianKerjasamaMasterAsset;
	}

	/**
	 * Menyetel header perjanjian kerjasama yang menaungi baris ini.
	 *
	 * @param perjanjianKerjasamaMasterAsset header kontrak payung
	 */
	public void setPerjanjianKerjasamaMasterAsset(PerjanjianKerjasamaMasterAsset perjanjianKerjasamaMasterAsset) {
		this.perjanjianKerjasamaMasterAsset = perjanjianKerjasamaMasterAsset;
	}

	/**
	 * Mengembalikan aset yang akhirnya terbentuk dari baris permintaan ini.
	 *
	 * <p>Ujung paling hilir dari rantai: setelah barang diterima dan dicatat,
	 * <code>SaldoAwalMasterAssetDetailAction</code> mengaitkan aset yang terbentuk
	 * balik ke baris permintaan asalnya lewat sisi {@link Asset}. Kolom di sini
	 * adalah kaitan searah dari sisi permintaan, dipakai laporan penelusuran
	 * "aset ini berasal dari permintaan mana".</p>
	 *
	 * <p>Sama seperti {@link #getPemesananPengadaanMasterAssetDetail()}, kolom ini
	 * bertipe tunggal padahal satu baris permintaan berkuantitas banyak dapat
	 * melahirkan banyak aset. Untuk penelusuran lengkap, kueri balik dari
	 * {@link Asset}.</p>
	 *
	 * <p><b>Tidak memanggil <code>check()</code></b>; dimuat seketika lewat SELECT
	 * terpisah.</p>
	 *
	 * @return aset hasil, atau {@code null} bila belum terbentuk
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "asset", nullable = true)
	public Asset getAsset() {
		return asset;
	}

	/**
	 * Menyetel aset yang terbentuk dari baris permintaan ini.
	 *
	 * @param asset aset hasil
	 */
	public void setAsset(Asset asset) {
		this.asset = asset;
	}

	/**
	 * Mengembalikan kuantitas yang <b>sudah diterima</b> (bukan yang sudah
	 * dipesan) atas baris permintaan ini.
	 *
	 * <h3>Apa sebenarnya yang disimpan di sini</h3>
	 * <p>Meskipun namanya menyerupai "jumlah yang sudah datang", kolom ini
	 * bukanlah nilai yang dipelihara oleh dokumen penerimaan. Ia adalah
	 * <b>cache yang disegarkan sebagai efek samping perenderan</b>: satu-satunya
	 * penulisnya dalam alur normal adalah
	 * <code>AmbilDataPermintaanPengadaanMasterAssetBanyak</code> - dialog pemilih
	 * PR pada layar PO - yang saat menggambar setiap baris menjumlahkan
	 * <code>getDiterima()</code> dari seluruh baris penerimaan yang terkait, lalu
	 * menuliskannya ke kolom ini bila berbeda. (Satu penulis lain,
	 * <code>HibernateProcurementRequisitionPort</code>, hanya menginisialisasi
	 * nilai {@code 0.0} saat membuat baris baru.)</p>
	 * <p>Konsekuensi langsungnya: <b>nilai kolom ini basi selama tidak ada yang
	 * membuka dialog pemilih PR.</b> Laporan yang membacanya secara langsung -
	 * misalnya <code>DasboardAnalisisVendor</code> dan API POS - dapat menampilkan
	 * angka yang tertinggal dari kenyataan.</p>
	 *
	 * <h3>Peringatan integritas: sisa pesanan dihitung dari penerimaan</h3>
	 * <p>Nilai ini dipakai <code>PemesananPengadaanMasterAssetAction.generateDetail()</code>
	 * untuk mengusulkan kuantitas baris PO baru, dengan rumus
	 * <code>jumlah - jumlahDatang</code>. Karena pengurangnya adalah kuantitas
	 * yang sudah <b>diterima</b> dan bukan yang sudah <b>dipesan</b>, sebuah baris
	 * permintaan yang seluruh kuantitasnya sudah dituangkan ke PO namun barangnya
	 * belum datang tetap menghasilkan sisa sebesar kuantitas penuh.</p>
	 * <p>Penjagaan di dialog pemilih PR memakai basis yang sama: baris hanya
	 * kehilangan kotak centangnya (sehingga tidak dapat dipilih lagi) ketika
	 * <i>total yang diterima</i> sudah mencapai kuantitas yang diminta. Baris
	 * dengan PO terbuka yang belum ada BAST-nya masih dapat dicentang. Gabungan
	 * kedua hal itu memungkinkan penerbitan PO kedua atas kebutuhan yang sama
	 * tanpa peringatan, sementara
	 * {@link #getPemesananPengadaanMasterAssetDetail()} - yang bertipe tunggal -
	 * akan tertimpa sehingga kaitan ke PO pertama hilang.</p>
	 * <p>Pengaman yang tersedia bersifat manual atau tidak langsung:
	 * {@link PermintaanPengadaanMasterAsset#getTutup()} yang harus ditutup
	 * operator, serta pengendalian pagu anggaran lewat mata anggaran - yang
	 * sendiri dapat dilewati bendera <code>tanpaAnggaran</code>. Perlakukan ini
	 * sebagai batasan yang <b>diketahui</b>: setiap kode baru yang menghitung
	 * "sisa yang belum dipesan" wajib menjumlahkan kuantitas dari baris PO terkait,
	 * bukan memakai properti ini.</p>
	 *
	 * <p>Mengembalikan {@code 0.0} untuk null agar aman dipakai dalam aritmetika
	 * tanpa penjagaan tambahan.</p>
	 *
	 * @return kuantitas yang sudah diterima menurut cache terakhir; tidak pernah
	 *         {@code null}
	 * @see #getJumlah()
	 * @see #getPemesananPengadaanMasterAssetDetail()
	 */
	public Double getJumlahDatang() {
		return jumlahDatang == null ? 0.0 : jumlahDatang;
	}

	/**
	 * Menyetel kuantitas yang sudah diterima.
	 *
	 * <p>Dalam alur normal hanya dipanggil oleh dialog pemilih PR sebagai
	 * penyegaran cache, dan oleh port pembuatan PR untuk menginisialisasi nol.
	 * <b>Jangan</b> memanggilnya dari kode penerimaan barang untuk "memperbarui
	 * stok" - nilai ini bukan sumber kebenaran penerimaan, melainkan turunan dari
	 * baris {@link PenerimaanPengadaanMasterAssetDetail}.</p>
	 *
	 * @param jumlahDatang kuantitas yang sudah diterima
	 */
	public void setJumlahDatang(Double jumlahDatang) {
		this.jumlahDatang = jumlahDatang;
	}
}
