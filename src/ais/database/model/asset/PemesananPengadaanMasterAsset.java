package ais.database.model.asset;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

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

import org.hibernate.Session;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import ais.database.model.inventory.Toko;
import org.json.JSONArray;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Pertangungjawaban;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.Workspace;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * Header <b>Pemesanan Pengadaan Barang/Jasa</b> (dokumen PO - <i>Purchase
 * Order</i>) untuk modul Aset Tetap: pesanan resmi kepada penyedia, lengkap
 * dengan harga sepakat, skema pembayaran, dan batas waktu kirim.
 *
 * <h3>Posisi dalam rantai pengadaan aset</h3>
 * <pre>
 *   PermintaanPengadaanMasterAsset   (PR - "apa yang dibutuhkan")
 *        v
 *   PemesananPengadaanMasterAsset    (PO - dokumen ini)
 *        v
 *   PenerimaanPengadaanMasterAsset   (BAST - "barang datang")
 *        v
 *   PembayaranDpMasterAsset / PembayaranTerminMasterAsset /
 *   PembayaranPengadaanMasterAsset / SaldoAwalMasterAsset   ("dibayar")
 * </pre>
 * <p>Dokumen ini adalah <b>titik komitmen finansial</b>: sejak PO disetujui,
 * lembaga terikat kepada penyedia. Karena itu hampir seluruh perhitungan
 * pembayaran modul aset berpusat di sini - lihat {@link #hitungDibayar()} yang
 * menjumlahkan empat kanal pembayaran berbeda menjadi satu angka.</p>
 *
 * <h3>Bagaimana PO tersambung ke PR: FK nyata, bukan antrean kerja</h3>
 * <p>Pada paket <code>inventory</code>, dokumen sejenis
 * (<code>PengajuanPembelianGudang</code>) ternyata hanya antrean kerja tanpa FK
 * ke dokumen realisasinya. <b>Pola itu tidak berlaku di modul aset.</b> Sambungan
 * PR-&gt;PO di sini berlapis empat:</p>
 * <ol>
 *   <li>FK header pada sisi PR
 *       ({@link PermintaanPengadaanMasterAsset#getPemesananPengadaanMasterAsset()}) -
 *       perhatikan bahwa kolomnya berada di tabel PR, bukan di tabel PO ini;</li>
 *   <li>FK baris arah PO-&gt;PR
 *       ({@link PemesananPengadaanMasterAssetDetail#getPermintaanPengadaanMasterAssetDetail()});</li>
 *   <li>FK baris arah PR-&gt;PO
 *       ({@link PermintaanPengadaanMasterAssetDetail#getPemesananPengadaanMasterAssetDetail()});</li>
 *   <li>jejak CSV {@link #getPermintaanPengadaanMasterAssets()} pada dokumen ini -
 *       daftar id baris PR dipisah koma, denormalisasi untuk pelaporan.</li>
 * </ol>
 * <p>Konsekuensinya, risiko integritas pada modul aset berupa
 * <i>ketidaksinkronan antar-lapis</i>, bukan hilangnya jejak sama sekali.</p>
 *
 * <h3>Gerbang persetujuan dan jalan-jalan pintasnya</h3>
 * <p>Gerbang PR-&gt;PO <b>ada dan bekerja</b>: dialog pemilih PR pada layar PO
 * menyaring <code>disetujuiOleh IS NOT NULL</code> dan
 * <code>aktif != false</code>, sehingga permintaan yang belum disetujui atau
 * sudah ditolak tidak dapat ditarik ke pesanan. Namun ada dua bendera pada
 * dokumen ini yang mengubah aturan main dan harus dipahami bersama:</p>
 * <ul>
 *   <li>{@link #getTampaPermintaan()} - PO diterbitkan <b>tanpa PR sama
 *       sekali</b>. Gerbang PR otomatis tidak berlaku; satu-satunya kendali yang
 *       tersisa adalah persetujuan atas PO ini sendiri.</li>
 *   <li>{@link #getPembelianLangsung()} - PO menyetujui dirinya sendiri:
 *       {@link #getDisetujuiOleh()} mengembalikan {@link #getDibuatOleh()} dan
 *       {@link #getTanggalPersetujuan()} mengembalikan
 *       {@link #getTanggalPembuatan()}, tanpa langkah persetujuan terpisah dan
 *       tanpa batas nilai. Bendera ini hanya dapat dinyalakan lewat tombol "Beli
 *       Langsung", yang pada gilirannya menuntut PR sumbernya <b>sudah
 *       disetujui</b> - jadi kendali bergeser ke hulu, bukan hilang.</li>
 * </ul>
 * <p>Selain itu perhatikan bahwa persetujuan PO tidak memeriksa apakah penyetuju
 * sama dengan pembuat; persetujuan atas dokumen sendiri dimungkinkan bagi
 * pemegang hak <code>approve</code>. Ini pola yang sudah tercatat di
 * modul-modul lain AIS.</p>
 *
 * <h3>Empat skema pembayaran yang saling meniadakan</h3>
 * <p>Satu PO menempuh tepat satu skema, ditentukan oleh kombinasi bendera:</p>
 * <ul>
 *   <li><b>Termin</b> ({@link #getByTermin()}) - dibayar bertahap menurut
 *       {@link #getFormula()}; DP dipaksa nol dan PPN DP dilepas;</li>
 *   <li><b>Uang muka / DP</b> ({@link #getDptotal()} &gt; 0,1) - ada pembayaran
 *       di muka lewat {@link PembayaranDpMasterAsset};</li>
 *   <li><b>Pembelian langsung</b> ({@link #getPembelianLangsung()}) - nilai PO
 *       diambil alih dari total DP;</li>
 *   <li><b>Biasa</b> - dibayar setelah barang diterima, lewat
 *       {@link PembayaranPengadaanMasterAsset} atau
 *       {@link SaldoAwalMasterAsset}.</li>
 * </ul>
 * <p>{@link #hitungDibayar(Session)} memakai bendera-bendera itu untuk memilih
 * kanal mana yang dijumlahkan dan mana yang dinolkan - salah menyetel bendera
 * berarti pembayaran yang nyata tidak terhitung.</p>
 *
 * <h3>Catatan teknis</h3>
 * <ul>
 *   <li>Mewarisi {@link DataSop} sehingga dapat dijalankan mesin alur SOP dan
 *       memperoleh helper <code>check()</code> untuk me-<i>reattach</i> proxy
 *       Hibernate.</li>
 *   <li>Ber-anotasi {@link Audited} (Envers).</li>
 *   <li>Banyak getter bersifat <b>destruktif</b> (menulis balik ke field saat
 *       dibaca) sehingga pembacaan pada entitas terkelola dapat memicu UPDATE -
 *       alasan langsung mengapa {@link #hitungDibayar(Session)} sengaja mematikan
 *       auto-flush selama menghitung.</li>
 * </ul>
 *
 * @see PermintaanPengadaanMasterAsset dokumen PR hulu
 * @see PemesananPengadaanMasterAssetDetail baris item pesanan
 * @see PenerimaanPengadaanMasterAsset dokumen BAST hilir
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "asset", name = "pemesanan_pengadaan_master_asset")
public class PemesananPengadaanMasterAsset extends DataSop {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama (identity). Lihat {@link #getId()}. */
	private Long id;
	/** Nomor urut tampilan, bukan data bisnis. Lihat {@link #getIndex()}. */
	private Long index;
	/** Nama pengguna terakhir yang menyentuh baris (audit bayangan). Lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang menyentuh baris (audit bayangan). Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan <b>id</b> pengguna yang terakhir menyentuh baris ini.
	 *
	 * <p>Bagian dari trio audit bayangan ({@link #getOleh()},
	 * {@link #getOlehId()}, {@link #getTanggal_dirubah()}) yang diisi
	 * <code>AuditTimestampInterceptor</code>. Kehadirannya di samping {@link Audited}
	 * milik Envers adalah <b>keharusan teknis</b>, bukan duplikasi yang bisa
	 * dibuang: layar daftar perlu menampilkan jejak perubahan untuk banyak baris
	 * sekaligus tanpa join ke tabel revisi.</p>
	 *
	 * @return id pengguna terakhir, atau {@code null}
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna terakhir yang menyentuh baris ini.
	 *
	 * <p>Nilai {@code null} atau berisi spasi saja <b>diabaikan diam-diam</b>
	 * sehingga jejak audit tidak terhapus oleh pemanggil tanpa konteks pengguna.
	 * Akibatnya field ini tidak dapat dikosongkan lewat setter.</p>
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
	 * Representasi teks dokumen untuk komponen ZK dan log.
	 *
	 * <p>Mengembalikan field {@link #kode} secara <b>langsung</b>, bukan lewat
	 * {@link #getKode()} yang memangkas spasi dan memetakan string kosong menjadi
	 * {@code null}. Untuk PO yang belum disimpan hasilnya {@code null}.</p>
	 *
	 * @return nomor PO apa adanya, bisa {@code null}
	 */
	public String toString() {
		return kode;
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
	 * dibuat sehingga dokumen baru tidak pernah bercap waktu null.
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
	 * Mengembalikan cap waktu perubahan terakhir dokumen ini.
	 *
	 * @return cap waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nomor dokumen PO, unik. Lihat {@link #getKode()}. */
	private String kode;
	/** Nomor invoice, dibangkitkan untuk PO ber-DP. Lihat {@link #getKodeInvoice()}. */
	private String kodeInvoice;
	/** Uraian bebas pesanan. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Lingkup toko/tenant (dipakai modul POS). Lihat {@link #getToko()}. */
	private Toko toko;
	/** Entitas pemilik aset yang akan dicatat. Lihat {@link #getPemilikAsset()}. */
	private PemilikAsset pemilikAsset;
	/** Lokasi penempatan aset. Lihat {@link #getLokasi()}. */
	private Lokasi lokasi;
	/** Ruang penempatan aset. Lihat {@link #getRuang()}. */
	private Ruang ruang;
	/** Penyedia/vendor tujuan pesanan. Lihat {@link #getPenyedia()}. */
	private PenyediaAsset penyedia;

	/** Klasifikasi jenis pemesanan. Lihat {@link #getJenisPemesananPengadaanAsset()}. */
	private JenisPemesananPengadaanAsset jenisPemesananPengadaanAsset;
	/** Tanggal dokumen dibuat. Lihat {@link #getTanggalPembuatan()}. */
	private Date tanggalPembuatan;
	/** Tanggal dokumen disetujui. Lihat {@link #getTanggalPersetujuan()}. */
	private Date tanggalPersetujuan;
	/** Batas waktu kirim (kolom baru). Lihat {@link #getPengirimanPalingLambat()}. */
	private Date pengirimanPalingLambat;
	/** Batas waktu kirim (kolom warisan). Lihat {@link #getPengirimanPalingLambatOld()}. */
	private Date pengirimanPalingLambatOld;
	/** Pembuat dokumen. Lihat {@link #getDibuatOleh()}. */
	private Tbmuser dibuatOleh;
	/** Penyetuju dokumen. Lihat {@link #getDisetujuiOleh()}. */
	private Tbmuser disetujuiOleh;

	/** Nilai uang muka sebelum PPN. Lihat {@link #getDp()}. */
	private Double dp;
	/** Nilai uang muka termasuk PPN (selalu dihitung ulang). Lihat {@link #getDptotal()}. */
	private Double dptotal;
	/** Cache total yang sudah dibayar. Lihat {@link #getDibayar()} dan {@link #hitungDibayar()}. */
	private Double dibayar;
	/** Penanda lunas (selalu dihitung ulang). Lihat {@link #getLunas()}. */
	private Boolean lunas;
	/** Unit kerja pemilik dokumen (dasar filter tenant). Lihat {@link #getSatuanKerja()}. */
	private SatuanKerja satuanKerja;
	/** Jejak CSV id baris PR asal. Lihat {@link #getPermintaanPengadaanMasterAssets()}. */
	private String permintaanPengadaanMasterAssets;
	/** Jejak CSV id mata anggaran asal. Lihat {@link #getAngarans()}. */
	private String angarans;
	/** Instans alur SOP yang menjalankan dokumen ini. Lihat {@link #getDisposisiSop()}. */
	private DisposisiSop disposisiSop;
	/** Penanda PO tanpa PR. Lihat {@link #getTampaPermintaan()}. */
	private Boolean tampaPermintaan;
	/** Riwayat posting ke buku besar. Lihat {@link #getPostingHistory()}. */
	private PostingHistory postingHistory;

	/** Tahun periode dokumen. Lihat {@link #getTahun()}. */
	private Integer tahun;
	/** Bulan periode dokumen (1-12). Lihat {@link #getBulan()}. */
	private Integer bulan;
	/** Template penomoran surat. Lihat {@link #getNomorSuratAlurPengadaan()}. */
	private NomorSuratAlurPengadaan nomorSuratAlurPengadaan;
	/** Klausul kesepakatan yang tercetak pada PO. Lihat {@link #getCatatanKesepakatan()}. */
	private String catatanKesepakatan;
	/** Kontrak payung yang menaungi pesanan. Lihat {@link #getPerjanjianKerjasamaMasterAsset()}. */
	private PerjanjianKerjasamaMasterAsset perjanjianKerjasamaMasterAsset;
	/** Nilai total pesanan. Lihat {@link #getNilai()}. */
	private Double nilai;
	/** Jenis PPN atas uang muka. Lihat {@link #getJenisPajakPpnDp()}. */
	private JenisPajakPpn jenisPajakPpnDp;

	/** Penanda skema pembayaran bertermin. Lihat {@link #getByTermin()}. */
	private Boolean byTermin;
	/** Definisi termin dalam bentuk JSON. Lihat {@link #getFormula()}. */
	private String formula;
	/** Awal masa berlaku pesanan/kontrak. Lihat {@link #getTanggalMulai()}. */
	private Date tanggalMulai;
	/** Akhir masa berlaku pesanan/kontrak. Lihat {@link #getTanggalSampai()}. */
	private Date tanggalSampai;
	/** Tanggal dokumen ditolak. Lihat {@link #getTanggalDitolak()}. */
	private Date tanggalDitolak;
	/** Penolak dokumen. Lihat {@link #getDitolakOleh()}. */
	private Tbmuser ditolakOleh;
	/** Alasan penolakan. Lihat {@link #getAlasanDitolak()}. */
	private String alasanDitolak;
	/** Penanda pesanan di luar anggaran. Lihat {@link #getTanpaAnggaran()}. */
	private Boolean tanpaAnggaran;
	/** Penanda pembelian langsung (menyetujui diri sendiri). Lihat {@link #getPembelianLangsung()}. */
	private Boolean pembelianLangsung;
	/** Mata anggaran pembebanan. Lihat {@link #getWorkspace()}. */
	private Workspace workspace;
	/** Penanda sisa pesanan ditutup. Lihat {@link #getTutup()}. */
	private Boolean tutup;
	/** Alasan sisa pesanan ditutup. Lihat {@link #getAlasanTutup()}. */
	private String alasanTutup;
	/** Pesanan asal bila dokumen ini pesanan susulan. Lihat {@link #getPoInduk()}. */
	private PemesananPengadaanMasterAsset poInduk;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate untuk instansiasi
	 * lewat refleksi, sekaligus dipakai kode aksi untuk membuat PO baru. Seluruh
	 * field dibiarkan null; getter berpelindung memasok nilai bawaan saat pertama
	 * dibaca.
	 */
	public PemesananPengadaanMasterAsset() {
	}

	/**
	 * Konstruktor pintasan untuk membuat <i>referensi</i> ke PO yang sudah ada
	 * tanpa memuatnya dari basis data.
	 *
	 * <p>Objek hasilnya bersifat <b>detached dan tidak lengkap</b>: seluruh field
	 * selain {@link #id} bernilai null. Jangan pernah menyimpannya lewat
	 * <code>session.update()</code> karena akan menimpa kolom-kolom nyata dengan
	 * null.</p>
	 *
	 * @param id kunci utama PO yang dirujuk
	 */
	public PemesananPengadaanMasterAsset(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kunci utama dokumen.
	 *
	 * <p>Kolom bertanda <code>insertable = false</code> karena dibangkitkan basis
	 * data (IDENTITY). Nilai {@code null} berarti PO belum tersimpan - kondisi ini
	 * dipakai sebagai penjagaan awal oleh {@link #hitungDibayar()} dan
	 * mempengaruhi {@link #getKodeInvoice()} serta {@link #getKodeUnik()}.</p>
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
	 * Menyetel kunci utama dokumen. Dipakai Hibernate saat memuat baris; kode
	 * aplikasi umumnya tidak perlu memanggilnya.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nomor dokumen PO, ternormalisasi.
	 *
	 * <p>Memangkas spasi ujung dan memetakan string kosong menjadi {@code null},
	 * <b>tanpa</b> menulis balik ke field - sehingga {@link #toString()} yang
	 * membaca field mentah dapat memberi hasil berbeda. Kolom bertanda
	 * <code>unique</code>, jadi penomoran ganda ditolak basis data. Nomor
	 * dibangkitkan kode aksi (<code>generateCode()</code>) tepat sebelum
	 * penyimpanan pertama.</p>
	 *
	 * <p>Perhatikan bahwa {@link #getKodeUnik()} merangkai nilai ini dengan
	 * pengenal instans alur, sehingga satu nomor PO dapat diajukan ulang lewat
	 * alur SOP tanpa melanggar constraint keunikan.</p>
	 *
	 * @return nomor PO tanpa spasi ujung, atau {@code null} bila kosong
	 */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return this.kode == null || kode.trim().isEmpty() ? null : this.kode.trim();
	}

	/**
	 * Menyetel nomor dokumen PO. Tidak menormalkan maupun memvalidasi keunikan.
	 *
	 * @param kode nomor PO
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan uraian bebas pesanan.
	 *
	 * <p>Dipetakan sebagai kolom <code>text</code> sehingga tidak berbatas panjang
	 * praktis. Layar PO mewajibkan field ini terisi sebelum menyimpan, namun
	 * kewajiban itu ditegakkan di lapisan UI - basis data mengizinkan null. Untuk
	 * PO yang dibuat lewat tombol "Beli Langsung", nilainya diisi otomatis dengan
	 * teks "Pembelian langsung".</p>
	 *
	 * @return uraian pesanan, bisa {@code null}
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel uraian bebas pesanan.
	 *
	 * @param keterangan uraian pesanan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan toko (tenant ritel) pemilik dokumen.
	 *
	 * <p>Ditambahkan pada r77725 ketika modul pengadaan dipakai bersama oleh
	 * antarmuka ZK dan modul POS: POS sengaja <b>tidak</b> memakai tabel sendiri
	 * melainkan tabel yang sama, dibedakan lewat kolom ini. Untuk dokumen dari
	 * antarmuka non-POS nilainya {@code null}.</p>
	 *
	 * <p><b>Getter destruktif</b> karena memanggil <code>check()</code>.</p>
	 *
	 * <p><b>Catatan pemisahan tenant</b>: pemilihan dan penyaringan PO pada jalur
	 * ZK bertumpu pada {@link #getSatuanKerja()}, bukan pada kolom ini. Pemisahan
	 * lingkup toko karena itu hanya ditegakkan pada jalur POS.</p>
	 *
	 * @return toko pemilik dokumen, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko", nullable = true)
	public Toko getToko() {
		toko = check(toko);
		return toko;
	}

	/**
	 * Menyetel toko (tenant ritel) pemilik dokumen.
	 *
	 * @param toko toko pemilik
	 */
	public void setToko(Toko toko) {
		this.toko = toko;
	}

	/**
	 * Menyetel pembuat dokumen.
	 *
	 * <p>Disetel oleh kode aksi ke pengguna aktif saat PO pertama kali disimpan.
	 * Nilainya dapat <b>ditimpa saat dibaca</b> oleh {@link #getDibuatOleh()} bila
	 * dokumen berjalan di atas alur SOP.</p>
	 *
	 * @param dibuatOleh pengguna pembuat dokumen
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengembalikan pengguna pembuat dokumen, dengan alur SOP sebagai sumber
	 * kebenaran bila tersedia.
	 *
	 * <h3>Cara kerja</h3>
	 * <p>Field di-<i>reattach</i> lewat <code>check()</code>, lalu bila dokumen
	 * terikat {@link DisposisiSop} yang langkah awalnya memiliki pengaju, pengaju
	 * itulah yang dianggap pembuat - menimpa isi kolom <code>dibuat_oleh</code>.
	 * Logikanya sama dengan pada dokumen PR: yang bertanggung jawab secara
	 * administratif adalah orang yang <i>mengajukan</i> dokumen ke alur, belum
	 * tentu orang yang mengetiknya.</p>
	 *
	 * <h3>Mengapa method ini penting melampaui sekadar tampilan</h3>
	 * <p>Pada PO bertanda {@link #getPembelianLangsung()}, nilai balik method ini
	 * <b>menjadi penyetuju dokumen</b>: {@link #getDisetujuiOleh()} secara harfiah
	 * mengembalikan <code>getDibuatOleh()</code>. Artinya siapa pun yang
	 * teridentifikasi sebagai pembuat pada PO pembelian langsung otomatis tercatat
	 * sebagai penyetujunya. Perubahan apa pun pada derivasi di sini karena itu
	 * merambat langsung ke jejak persetujuan, bukan hanya ke label di layar.</p>
	 *
	 * <h3>Sifat destruktif dan pemanggilan berulang</h3>
	 * <p>Hasil derivasi ditulis balik ke field, sehingga membaca properti ini pada
	 * entitas terkelola dapat memicu UPDATE kolom <code>dibuat_oleh</code>. Selain
	 * itu {@link #getDisposisiSop()} - yang memuat data secara <i>lazy</i> -
	 * dipanggil tiga kali dalam satu rangkaian kondisi. Kelas saudaranya,
	 * {@link PermintaanPengadaanMasterAsset}, sudah menerima perbaikan akar
	 * masalah untuk pola semacam ini (ambil sekali ke variabel lokal) pada
	 * getter persetujuannya; terapkan pola yang sama bila method ini disunting.</p>
	 *
	 * @return pengaju langkah awal alur SOP bila ada, jika tidak isi kolom
	 *         <code>dibuat_oleh</code>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = false)
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);
		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
				&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
			dibuatOleh = getDisposisiSop().getDisposisiStart().getDiajukanOleh();
		}
		return dibuatOleh;
	}

	/**
	 * Menyetel penyetuju dokumen.
	 *
	 * <p>Dipanggil dari tiga tempat pada <code>PemesananPengadaanMasterAssetAction</code>:
	 * tombol "Persetujuan" (menyetel pengguna aktif), tombol "Batalkan" (menyetel
	 * {@code null}), dan penyimpanan PO ketika kotak centang "Setujui Pemesanan
	 * Barang / Jasa ini" tercentang. Nilai yang disetel dapat <b>ditimpa saat
	 * dibaca</b> oleh {@link #getDisetujuiOleh()}.</p>
	 *
	 * @param disetujuiOleh pengguna penyetuju, atau {@code null} untuk membatalkan
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Mengembalikan pengguna yang menyetujui pesanan ini - <b>penentu apakah PO
	 * sudah mengikat</b>.
	 *
	 * <h3>Mengapa method ini menentukan</h3>
	 * <p>Hampir seluruh dokumen hilir bergantung pada nilai balik method ini.
	 * Tombol Ubah dan Hapus pada layar PO hanya tampil selama hasilnya null;
	 * kriteria penjumlahan pembayaran di {@link #hitungDibayar(Session)} menyaring
	 * dokumen pembayaran lewat <code>disetujuiOleh IS NOT NULL</code>; dan dasbor
	 * pengadaan mengelompokkan PO dengan aturan yang sama. Method ini karena itu
	 * bukan pembacaan field biasa melainkan mesin status kecil.</p>
	 *
	 * <h3>Urutan keputusan</h3>
	 * <ol>
	 *   <li>Bila {@link #getDitolakOleh()} terisi, penyetuju dipaksa
	 *       {@code null} - penolakan selalu menang.</li>
	 *   <li>Selain itu, bila alur SOP memiliki langkah "setuju" dengan pengaju,
	 *       pengaju itulah yang menjadi penyetuju, menimpa isi kolom.</li>
	 *   <li>Bila disposisi ada tetapi langkah setuju belum ada (atau ada tanpa
	 *       pengaju), penyetuju dipaksa {@code null}.</li>
	 *   <li><b>Bila {@link #getPembelianLangsung()} bernilai true, penyetuju
	 *       diganti menjadi {@link #getDibuatOleh()}</b> - lihat bagian
	 *       berikutnya.</li>
	 *   <li>Pemeriksaan penolakan diulang di akhir agar penolakan tetap menang
	 *       bahkan atas pembelian langsung.</li>
	 * </ol>
	 *
	 * <h3>Pembelian langsung: persetujuan oleh diri sendiri, secara desain</h3>
	 * <p>Langkah keempat membuat PO bertanda pembelian langsung <b>selalu
	 * berstatus disetujui oleh pembuatnya sendiri</b>, tanpa langkah persetujuan
	 * terpisah, tanpa alur SOP, dan <b>tanpa batas nilai</b>. Ini bukan
	 * kecelakaan melainkan mekanisme yang disengaja untuk pengadaan kecil yang
	 * langsung dibayar. Kendalinya tidak hilang, melainkan bergeser ke hulu:
	 * bendera pembelian langsung hanya dapat dinyalakan lewat tombol "Beli
	 * Langsung", yang di layar PR menuntut permintaannya sudah disetujui
	 * (<code>getDisetujuiOleh() != null</code>) dan tidak ada baris beruang muka,
	 * atau lewat dialog pemilih PR yang menyaring hal yang sama. Jadi orang yang
	 * membuat PO pembelian langsung tetap harus mendapatkan PR yang telah melewati
	 * gerbang persetujuan.</p>
	 * <p>Yang perlu dicatat sebagai batasan yang <b>diketahui</b>: begitu bendera
	 * itu tersimpan di kolom, status "disetujui" pada PO menjadi konsekuensi
	 * otomatis - tidak ada pemeriksaan ulang terhadap PR sumber pada saat
	 * pembacaan. Setiap jalur baru yang dapat menyetel
	 * {@link #setPembelianLangsung(Boolean)} karena itu <b>wajib</b> menegakkan
	 * sendiri syarat PR-nya sudah disetujui.</p>
	 *
	 * <h3>Perbedaan dengan dokumen PR - pemanggilan berulang belum diperbaiki</h3>
	 * <p>{@link PermintaanPengadaanMasterAsset#getDisetujuiOleh()} sudah menerima
	 * perbaikan akar masalah NPE: {@link #getDisposisiSop()} dan
	 * <code>getDisposisiSetuju()</code> diambil <b>sekali</b> ke variabel lokal
	 * karena keduanya memuat data secara <i>lazy</i> lewat <code>check()</code>
	 * dan dua pemanggilan berturut-turut dapat memberi hasil berbeda antara
	 * null-check dan dereference. Method ini <b>belum</b> diperbaiki - ia memanggil
	 * rantai tersebut enam kali. Bila method ini disunting, terapkan pola yang
	 * sama; jangan menyalin bentuk sekarang ke tempat lain.</p>
	 * <p>Perbedaan lain: PR memiliki bendera <code>setujuiManual</code> yang
	 * memutus derivasi dari SOP, sedangkan PO tidak. PO karena itu <b>selalu</b>
	 * mencerminkan alur SOP-nya bila punya - lebih konsisten, tetapi berarti
	 * persetujuan manual pada PO ber-SOP akan tertimpa saat dibaca.</p>
	 *
	 * <h3>Sifat destruktif</h3>
	 * <p>Setiap cabang menulis ke field {@link #disetujuiOleh}, sehingga membaca
	 * status persetujuan pada entitas terkelola dapat memicu UPDATE. Inilah salah
	 * satu alasan {@link #hitungDibayar(Session)} mematikan auto-flush selama
	 * menghitung.</p>
	 *
	 * @return pengguna penyetuju, atau {@code null} bila belum disetujui atau
	 *         sudah ditolak
	 * @see #getPembelianLangsung()
	 * @see #getTanggalPersetujuan()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		if (getDitolakOleh() != null) {
			disetujuiOleh = null;
		} else {
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
					&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
				disetujuiOleh = getDisposisiSop().getDisposisiSetuju().getDiajukanOleh();
			}
		}

		if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
				|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
			disetujuiOleh = null;
		}

		if (getPembelianLangsung()) {
			disetujuiOleh = getDibuatOleh();
		}

		if (getDitolakOleh() != null) {
			disetujuiOleh = null;
		}

		disetujuiOleh = check(disetujuiOleh);
		return disetujuiOleh;
	}

	/**
	 * Menyetel tanggal persetujuan dokumen.
	 *
	 * <p>Nilainya dapat ditimpa saat dibaca oleh {@link #getTanggalPersetujuan()},
	 * baik oleh cap waktu langkah SOP maupun oleh tanggal pembuatan pada PO
	 * pembelian langsung.</p>
	 *
	 * @param tanggalPersetujuan cap waktu persetujuan, atau {@code null}
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengembalikan cap waktu persetujuan dokumen, dengan aturan derivasi yang
	 * <b>mencerminkan persis</b> {@link #getDisetujuiOleh()}.
	 *
	 * <h3>Urutan keputusan</h3>
	 * <p>Sama persis dengan {@link #getDisetujuiOleh()}: penolakan memaksa
	 * {@code null}; langkah "setuju" pada alur SOP memasok cap waktunya
	 * (<code>getWaktu()</code>, yaitu kapan persetujuan benar-benar diberikan di
	 * alur - bukan kapan kolom ditulis); disposisi tanpa langkah setuju memaksa
	 * {@code null}; pembelian langsung mengganti cap waktu dengan
	 * {@link #getTanggalPembuatan()}; dan pemeriksaan penolakan diulang di akhir.
	 * Kedua method ini <b>wajib</b> diubah bersamaan - bila hanya satu yang
	 * disunting, dokumen resmi akan mencetak nama penyetuju tanpa tanggal atau
	 * sebaliknya.</p>
	 *
	 * <h3>Seluruh badan method dibungkus try-catch: mengapa</h3>
	 * <p>Komentar dalam badan menjelaskan akarnya: {@link #disposisiSop} dapat
	 * berupa instans kanonik/bersama yang dipegang
	 * <code>AuditTimestampInterceptor</code>, dan proxy pada instans semacam itu
	 * terikat ke sesi Hibernate <b>lain</b> yang sudah tertutup. Mendereferensinya
	 * melempar <code>LazyInitializationException</code>. Karena getter ini
	 * dipanggil dari perenderan layar dan cetakan - tempat kegagalan berarti
	 * halaman kosong, bukan sekadar satu nilai hilang - keputusannya adalah
	 * <b>gagal tanpa suara</b>: seluruh blok derivasi dilewati dan nilai kolom
	 * dipertahankan sebagai cadangan.</p>
	 * <p>Konsekuensi yang harus disadari: pada kondisi tersebut, cap waktu yang
	 * ditampilkan berasal dari kolom dan <b>bisa tidak sinkron</b> dengan
	 * {@link #getDisetujuiOleh()} yang tidak dibungkus try-catch serupa. Pengecualian
	 * tidak ditelan begitu saja - ia dicatat lewat
	 * <code>ErrorAuditUtil.record()</code> dengan penanda lokasi, sehingga
	 * kemunculannya dapat ditelusuri di log audit galat. Bila penanda
	 * <code>getTanggalPersetujuan-lazy</code> sering muncul, akar masalahnya ada
	 * pada manajemen sesi di pemanggil, bukan di sini.</p>
	 *
	 * <h3>Sifat destruktif</h3>
	 * <p>Setiap cabang menulis ke field {@link #tanggalPersetujuan}. Pembacaan pada
	 * entitas terkelola dapat memicu UPDATE.</p>
	 *
	 * @return cap waktu persetujuan, atau {@code null} bila belum disetujui atau
	 *         sudah ditolak
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {
		try {
			// FIX LazyInitializationException: disposisiSop bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (getDitolakOleh() != null) {
				tanggalPersetujuan = null;
			} else if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
					&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
				tanggalPersetujuan = getDisposisiSop().getDisposisiSetuju().getWaktu();
			}

			if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
					|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
				tanggalPersetujuan = null;
			}

			if (getPembelianLangsung()) {
				tanggalPersetujuan = getTanggalPembuatan();
			}

			if (getDitolakOleh() != null) {
				tanggalPersetujuan = null;
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/asset/PemesananPengadaanMasterAsset.java:getTanggalPersetujuan-lazy");
		}

		return tanggalPersetujuan;
	}

	/**
	 * Menyetel tanggal pembuatan dokumen.
	 *
	 * @param tanggalPembuatan cap waktu pembuatan
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Mengembalikan cap waktu pembuatan dokumen.
	 *
	 * <p>Bila dokumen berjalan di atas alur SOP dan langkah awalnya memiliki
	 * pengaju, cap waktu diambil dari waktu langkah awal - konsisten dengan
	 * {@link #getDibuatOleh()}. Nilai kolom diperlakukan sebagai cadangan.</p>
	 *
	 * <p>Blok derivasi dibungkus try-catch dengan alasan yang sama seperti pada
	 * {@link #getTanggalPersetujuan()}: proxy disposisi dapat terikat ke sesi yang
	 * sudah tertutup, dan kegagalan di sini tidak boleh menjatuhkan seluruh
	 * halaman. Pengecualian dicatat ke audit galat dengan penanda
	 * <code>getTanggalPembuatan-lazy</code>.</p>
	 *
	 * <p><b>Tidak pernah mengembalikan null</b>: bila hasil akhirnya kosong,
	 * method memberi waktu server saat ini. Perhatikan bahwa nilai cadangan itu
	 * <i>tidak</i> ditulis ke field, hanya muncul pada nilai balik - jadi
	 * pembacaan berulang tidak mengotori entitas dengan cap waktu palsu. Tetapi
	 * berarti PO yang belum tersimpan menampilkan tanggal yang berubah setiap kali
	 * dibaca, sehingga jangan diandalkan untuk perbandingan.</p>
	 * <p>Perhatikan efek berantainya pada PO pembelian langsung: karena
	 * {@link #getTanggalPersetujuan()} mengambil nilai dari method ini, PO
	 * pembelian langsung yang kolom tanggal pembuatannya kosong akan melaporkan
	 * tanggal persetujuan berupa waktu saat ini.</p>
	 *
	 * @return cap waktu pembuatan; tidak pernah {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		try {
			// FIX LazyInitializationException: disposisiSop bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
					&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
				tanggalPembuatan = getDisposisiSop().getDisposisiStart().getWaktu();
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/asset/PemesananPengadaanMasterAsset.java:getTanggalPembuatan-lazy");
		}

		return tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan;
	}

	/**
	 * Menyetel nomor urut tampilan.
	 *
	 * @param index nomor urut baris pada grid
	 */
	public void setIndex(Long index) {
		this.index = index;
	}

	/**
	 * Mengembalikan nomor urut tampilan baris pada grid.
	 *
	 * <p>Tidak memiliki makna bisnis dan diisi kode aksi saat merender daftar;
	 * jangan dipakai sebagai pengenal.</p>
	 *
	 * @return nomor urut tampilan, atau {@code null}
	 */
	public Long getIndex() {
		return index;
	}

	/**
	 * Mengembalikan penyedia (vendor) tujuan pesanan ini.
	 *
	 * <p>Properti wajib menurut validasi layar PO - pesanan tanpa penyedia tidak
	 * dapat disimpan. Nilainya <b>diwariskan paksa</b> ke dokumen penerimaan
	 * (BAST): <code>PenerimaanPengadaanMasterAsset.getPenyedia()</code> mengambil
	 * penyedia dari PO-nya alih-alih dari kolomnya sendiri, sehingga vendor
	 * penerima pembayaran tidak dapat dialihkan diam-diam pada tahap penerimaan.
	 * Perubahan penyedia karena itu hanya sah dilakukan di dokumen ini, selagi PO
	 * belum disetujui.</p>
	 *
	 * <p><b>Getter destruktif</b> karena memanggil <code>check()</code>.</p>
	 *
	 * @return penyedia tujuan pesanan, atau {@code null} bila PO belum lengkap
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "penyedia", nullable = true)
	public PenyediaAsset getPenyedia() {
		penyedia = check(penyedia);
		return penyedia;
	}

	/**
	 * Menyetel penyedia tujuan pesanan.
	 *
	 * @param penyedia penyedia/vendor
	 */
	public void setPenyedia(PenyediaAsset penyedia) {
		this.penyedia = penyedia;
	}

	/**
	 * Mengembalikan entitas pemilik aset yang akan mencatat hasil pengadaan.
	 *
	 * <p>Disalin dari PR saat PO dibuat lewat tombol "Beli Langsung", atau dipilih
	 * manual pada layar PO. Menentukan badan hukum mana yang membukukan aset.</p>
	 *
	 * <p><b>Getter destruktif</b> karena memanggil <code>check()</code>.</p>
	 *
	 * @return pemilik aset, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pemilik_asset", nullable = true)
	public PemilikAsset getPemilikAsset() {
		pemilikAsset = check(pemilikAsset);
		return pemilikAsset;
	}

	/**
	 * Menyetel entitas pemilik aset.
	 *
	 * @param pemilikAsset pemilik aset
	 */
	public void setPemilikAsset(PemilikAsset pemilikAsset) {
		this.pemilikAsset = pemilikAsset;
	}

	/**
	 * Mengembalikan lokasi (gedung/kampus) tujuan penempatan aset.
	 *
	 * <p><b>Getter destruktif</b> karena memanggil <code>check()</code>.</p>
	 *
	 * @return lokasi tujuan, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi", nullable = true)
	public Lokasi getLokasi() {
		lokasi = check(lokasi);
		return lokasi;
	}

	/**
	 * Menyetel lokasi tujuan penempatan aset.
	 *
	 * @param lokasi lokasi tujuan
	 */
	public void setLokasi(Lokasi lokasi) {
		this.lokasi = lokasi;
	}

	/**
	 * Mengembalikan ruang (unit terkecil penempatan) tujuan aset.
	 *
	 * <p><b>Getter destruktif</b> karena memanggil <code>check()</code>.</p>
	 *
	 * @return ruang tujuan, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ruang", nullable = true)
	public Ruang getRuang() {
		ruang = check(ruang);
		return this.ruang;
	}

	/**
	 * Menyetel ruang tujuan aset.
	 *
	 * @param ruang ruang tujuan
	 */
	public void setRuang(Ruang ruang) {
		this.ruang = ruang;
	}

	/**
	 * Mengembalikan satuan kerja pemilik dokumen - <b>dasar pemisahan tenant</b>
	 * pada jalur ZK.
	 *
	 * <p>Diturunkan dari {@link #getWorkspace()} bila mata anggaran memiliki
	 * satuan kerja; nilai kolom hanya cadangan. Logikanya sama dengan pada dokumen
	 * PR: pembebanan anggaran yang menentukan unit penanggung jawab, bukan pilihan
	 * pengguna di formulir. Nilai ini kemudian <b>diwariskan paksa</b> ke dokumen
	 * penerimaan, sehingga lingkup unit tidak dapat berpindah di tengah rantai.</p>
	 *
	 * <p><b>Perhatikan batasan pemisahan tenant.</b> Penyaring berbasis satuan
	 * kerja di modul pengadaan menerima cabang "satuanKerja IS NULL" agar dokumen
	 * lintas unit tetap terlihat. Karena {@link #getWorkspace()} memaksa null saat
	 * {@link #getTanpaAnggaran()} menyala, PO tanpa anggaran yang kolom satuan
	 * kerjanya juga kosong jatuh ke cabang "terlihat semua pengguna". Ini pola
	 * filter tenant lemah yang berulang di AIS - perlakukan sebagai batasan yang
	 * diketahui, bukan jaminan isolasi.</p>
	 *
	 * <p><b>Getter destruktif</b>, dan memanggil {@link #getWorkspace()} - yang
	 * juga destruktif - dua kali.</p>
	 *
	 * @return satuan kerja pemilik dokumen, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		if (getWorkspace() != null && getWorkspace().getSatuanKerja() != null) {
			satuanKerja = getWorkspace().getSatuanKerja();
		}
		return satuanKerja;
	}

	/**
	 * Menyetel satuan kerja pemilik dokumen.
	 *
	 * <p>Nilai yang disetel ditimpa saat dibaca bila mata anggaran memiliki satuan
	 * kerja sendiri.</p>
	 *
	 * @param satuanKerja satuan kerja pemilik
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Mengembalikan jejak <b>id baris Permintaan Pengadaan</b> yang menjadi asal
	 * pesanan ini, sebagai daftar dipisah koma.
	 *
	 * <h3>Apa ini dan apa yang bukan</h3>
	 * <p>Kolom <code>text</code> ini <b>bukan relasi terpetakan Hibernate</b>.
	 * Isinya adalah denormalisasi: daftar
	 * <code>PermintaanPengadaanMasterAssetDetail.id</code> yang dirangkai kode
	 * aksi saat PO disimpan (<code>onSave()</code> membangunnya ulang penuh dari
	 * baris-baris PR yang dipilih pengguna). Relasi yang sebenarnya dipetakan
	 * berada di tempat lain - lihat Javadoc kelas untuk keempat lapisnya. Jejak
	 * CSV ini ada karena laporan dan modul anggaran perlu menjawab "PO ini
	 * memenuhi permintaan mana" tanpa menembus dua tingkat join baris.</p>
	 *
	 * <h3>Pembersihan nilai sampah</h3>
	 * <p>Getter memangkas spasi, lalu memeriksa empat pola koma kosong secara
	 * harfiah (<code>","</code>, <code>",,"</code>, <code>",,,"</code>,
	 * <code>",,,,"</code>) dan menggantinya dengan string kosong. Pola ini muncul
	 * dari versi lama perangkai yang menambahkan pemisah walau tidak ada id.
	 * Perhatikan bahwa pembersihan ini <b>tidak lengkap</b>: lima koma atau lebih
	 * lolos, demikian pula bentuk campuran seperti <code>"12,,"</code>. Pemakai
	 * yang mem-<code>split(",")</code> hasilnya - dan ada beberapa di
	 * <code>PemesananPengadaanMasterAssetAction</code>, <code>Pajak</code>, serta
	 * <code>PenggunaanAnggaran</code> - harus tetap menyaring potongan kosong
	 * sendiri sebelum mem-parsing menjadi angka.</p>
	 *
	 * <h3>Sifat destruktif</h3>
	 * <p>Hasil pemangkasan dan pembersihan ditulis balik ke field, sehingga
	 * membaca properti ini pada entitas terkelola dapat memicu UPDATE. Perhatikan
	 * juga bahwa getter ini <b>tidak pernah mengembalikan null</b> - nilai null
	 * dipetakan menjadi string kosong dan ditulis balik, yang berarti pembacaan
	 * saja dapat mengubah kolom dari NULL menjadi ''.</p>
	 *
	 * <p>Kelas {@link PerjanjianKerjasamaMasterAsset} dan
	 * <code>UangMuka</code> memiliki properti senama dengan pola pembersihan yang
	 * sama; perubahan di sini sebaiknya dicerminkan ke sana.</p>
	 *
	 * @return daftar id baris PR dipisah koma; tidak pernah {@code null}, bisa
	 *         berupa string kosong
	 * @see #getAngarans()
	 */
	@Column(columnDefinition = "text")
	public String getPermintaanPengadaanMasterAssets() {
		permintaanPengadaanMasterAssets = (permintaanPengadaanMasterAssets == null ? ""
				: permintaanPengadaanMasterAssets.trim());

		if (permintaanPengadaanMasterAssets.equals(",")) {
			permintaanPengadaanMasterAssets = "";
		} else if (permintaanPengadaanMasterAssets.equals(",,")) {
			permintaanPengadaanMasterAssets = "";
		} else if (permintaanPengadaanMasterAssets.equals(",,,")) {
			permintaanPengadaanMasterAssets = "";
		} else if (permintaanPengadaanMasterAssets.equals(",,,,")) {
			permintaanPengadaanMasterAssets = "";
		}
		return permintaanPengadaanMasterAssets;
	}

	/**
	 * Menyetel jejak CSV id baris Permintaan Pengadaan asal.
	 *
	 * <p>Dipanggil kode aksi dengan daftar yang <b>dibangun ulang penuh</b> setiap
	 * penyimpanan PO, bukan ditambahkan sedikit demi sedikit. Menyetel nilai
	 * sembarang di sini akan merusak pelaporan realisasi anggaran; biarkan kode
	 * aksi yang membangunnya.</p>
	 *
	 * @param permintaanPengadaanMasterAssets daftar id dipisah koma
	 */
	public void setPermintaanPengadaanMasterAssets(String permintaanPengadaanMasterAssets) {
		this.permintaanPengadaanMasterAssets = permintaanPengadaanMasterAssets;
	}

	/** Kode gabungan untuk keunikan lintas revisi SOP. Lihat {@link #getKodeUnik()}. */
	private String kodeUnik;
	/** Penanda dokumen masih berlaku. Lihat {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Mengembalikan kode gabungan yang menjamin keunikan dokumen lintas revisi
	 * alur SOP.
	 *
	 * <p>{@link #getKode()} sudah bertanda <code>unique</code>, tetapi mesin SOP
	 * memperbolehkan satu nomor dokumen memiliki beberapa instans alur (misalnya
	 * PO ditolak lalu diajukan ulang). Kode unik menggabungkan nomor PO dengan
	 * pengenal instans alur - berformat
	 * <code>&lt;kode&gt;_&lt;id disposisi&gt;</code>, atau
	 * <code>&lt;kode&gt;_&lt;id dokumen&gt;</code> bila tidak berdisposisi -
	 * sehingga tiap pengajuan menempati ruang nama sendiri.</p>
	 *
	 * <p><b>Selalu dihitung ulang</b> dan ditulis balik ke field; kolom di basis
	 * data hanyalah cache. Untuk PO baru tanpa disposisi, {@link #getId()} masih
	 * null sehingga hasilnya berakhiran <code>"_null"</code> - tidak menjadi
	 * masalah dalam praktik karena nomor PO dibangkitkan berurutan tepat sebelum
	 * penyimpanan.</p>
	 *
	 * @return kode unik gabungan
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		kodeUnik = getKode() + "" + (getDisposisiSop() == null ? "_" + getId() : "_" + getDisposisiSop().getId());
		return kodeUnik;
	}

	/**
	 * Menyetel kode unik.
	 *
	 * <p><b>Praktis tidak berguna</b>: {@link #getKodeUnik()} selalu menghitung
	 * ulang. Setter tetap ada karena dibutuhkan Hibernate untuk memuat kolom.</p>
	 *
	 * @param kodeUnik kode unik (akan tertimpa saat dibaca)
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * Mengembalikan instans alur SOP (disposisi) yang menjalankan dokumen ini.
	 *
	 * <p>Objek ini menyimpan rantai langkah persetujuan - langkah awal, langkah
	 * setuju, langkah akhir - dan menjadi akar bagi hampir seluruh properti
	 * turunan di kelas ini: {@link #getDibuatOleh()}, {@link #getDisetujuiOleh()},
	 * {@link #getTanggalPersetujuan()}, {@link #getTanggalPembuatan()},
	 * {@link #getAktif()}, dan {@link #getKodeUnik()}.</p>
	 *
	 * <p><b>Getter destruktif</b> karena memanggil <code>check()</code>. Karena
	 * begitu banyak getter lain memanggilnya berulang dalam satu ekspresi -
	 * {@link #getDisetujuiOleh()} melakukannya enam kali - method ini adalah sumber
	 * NPE dan <code>LazyInitializationException</code> yang sudah terbukti di
	 * modul ini. Selalu ambil sekali ke variabel lokal.</p>
	 *
	 * @return instans alur SOP, atau {@code null} bila dokumen tidak dijalankan
	 *         lewat mesin SOP
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menyetel instans alur SOP yang menjalankan dokumen ini.
	 *
	 * <h3>Setter berpelindung: kaitan alur tidak dapat dilepas</h3>
	 * <p>Bila argumen {@code null} atau belum memiliki id, method langsung
	 * {@code return}. Sekali PO terikat ke sebuah instans alur, kaitan itu hanya
	 * dapat <b>diganti</b> ke instans lain yang sudah tersimpan, tidak dapat
	 * dilepas. Pelindung ini penting karena melepas kaitan alur akan membuat
	 * seluruh properti status berubah mendadak - PO yang sudah disetujui bisa
	 * tampak kembali "belum disetujui" dan tombol Ubah/Hapus muncul kembali.</p>
	 *
	 * <h3>Ekspresi ternary yang selalu memilih cabang yang sama</h3>
	 * <p>Setelah penjagaan di atas, kedua syarat di dalam kurung pada ekspresi
	 * ternary sudah dipastikan salah, sehingga baris itu setara dengan penugasan
	 * langsung. Sisa kode ini peninggalan versi sebelum penjagaan ditambahkan;
	 * dibiarkan karena tidak berbahaya, tetapi jangan dijadikan contoh.</p>
	 *
	 * @param disposisiSop instans alur SOP yang sudah tersimpan; null atau tanpa
	 *                     id diabaikan
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {
		if (disposisiSop == null || disposisiSop.getId() == null) {
			return;
		}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
	}

	/**
	 * Mengembalikan klasifikasi jenis pemesanan (misalnya barang, jasa, atau
	 * pekerjaan konstruksi).
	 *
	 * <p>Properti wajib menurut validasi layar PO. Klasifikasi ini mempengaruhi
	 * pemilihan template cetakan dan pengelompokan pada laporan pengadaan.</p>
	 *
	 * <p><b>Getter destruktif</b> karena memanggil <code>check()</code>.</p>
	 *
	 * @return jenis pemesanan, atau {@code null} bila PO belum lengkap
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pemesanan_pengadaan_asset", nullable = true)
	public JenisPemesananPengadaanAsset getJenisPemesananPengadaanAsset() {
		jenisPemesananPengadaanAsset = check(jenisPemesananPengadaanAsset);
		return jenisPemesananPengadaanAsset;
	}

	/**
	 * Menyetel klasifikasi jenis pemesanan.
	 *
	 * @param jenisPemesananPengadaanAsset jenis pemesanan
	 */
	public void setJenisPemesananPengadaanAsset(JenisPemesananPengadaanAsset jenisPemesananPengadaanAsset) {
		this.jenisPemesananPengadaanAsset = jenisPemesananPengadaanAsset;
	}

	/**
	 * Mengembalikan apakah pesanan ini diterbitkan <b>tanpa dokumen Permintaan
	 * Pengadaan</b> sama sekali.
	 *
	 * <h3>Arti dan konsekuensi</h3>
	 * <p>Nama fieldnya mengandung salah ketik yang sudah membeku di kolom basis
	 * data ("tampa" seharusnya "tanpa"); jangan diperbaiki tanpa migrasi. Ketika
	 * bernilai true, layar PO melewati seluruh mekanisme pemilihan baris PR:
	 * <code>generateDetail()</code> tidak membersihkan dan tidak mengisi grid dari
	 * permintaan, dan pengguna mengetikkan sendiri baris-baris pesanan.</p>
	 * <p>Ini berarti <b>gerbang persetujuan PR tidak berlaku</b> untuk PO semacam
	 * ini - tidak ada PR yang perlu disetujui lebih dulu. Kendali yang tersisa
	 * hanyalah persetujuan atas PO ini sendiri (hak <code>approve</code> pada
	 * menu PO) dan, bila dokumen dijalankan lewat SOP, alur persetujuannya. Untuk
	 * mengimbangi, validasi layar PO mewajibkan mata anggaran diisi bila PO tanpa
	 * permintaan dan tidak bertanda {@link #getTanpaAnggaran()}, sehingga
	 * pembebanan tetap tertelusur ke pagu.</p>
	 * <p>Jangan menyamakan bendera ini dengan {@link #getPembelianLangsung()}:
	 * yang ini menghapus <i>dokumen hulu</i>, sedangkan yang itu menghapus
	 * <i>langkah persetujuan</i> pada dokumen ini. Keduanya dapat menyala
	 * bersamaan.</p>
	 *
	 * <p>Mengembalikan {@code false} untuk null sehingga aman di-<i>unbox</i>.</p>
	 *
	 * @return {@code true} bila PO diterbitkan tanpa PR; tidak pernah {@code null}
	 */
	public Boolean getTampaPermintaan() {
		return tampaPermintaan == null ? false : tampaPermintaan;
	}

	/**
	 * Menyetel penanda pesanan tanpa dokumen permintaan.
	 *
	 * @param tampaPermintaan {@code true} bila PO diterbitkan tanpa PR
	 */
	public void setTampaPermintaan(Boolean tampaPermintaan) {
		this.tampaPermintaan = tampaPermintaan;
	}

	/**
	 * Mengembalikan batas waktu pengiriman yang disepakati dengan penyedia.
	 *
	 * <p><b>Pola migrasi kolom yang perlu dipahami.</b> Terdapat dua kolom untuk
	 * makna yang sama: <code>pengiriman_paling_lambat</code> (baru, dipetakan di
	 * sini) dan <code>pengirimanpalinglambat</code> (warisan, dipetakan di
	 * {@link #getPengirimanPalingLambatOld()}). Getter ini menjembatani keduanya:
	 * bila kolom baru masih kosong, nilai kolom warisan disalin ke sana dan
	 * <b>ditulis balik</b> ke field, sehingga dokumen lama perlahan bermigrasi
	 * sendiri saat dibaca. Migrasi ini <b>searah</b> - perubahan pada kolom baru
	 * tidak pernah disalin balik ke kolom warisan, sehingga setelah migrasi kedua
	 * kolom dapat berbeda dan kolom warisan menjadi basi. Kode baru harus selalu
	 * memakai getter ini, bukan versi <code>Old</code>.</p>
	 *
	 * <p>Tanggal ini adalah dasar pengukuran keterlambatan penyedia pada dasbor
	 * analisis vendor.</p>
	 *
	 * @return batas waktu pengiriman, atau {@code null} bila kedua kolom kosong
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "pengiriman_paling_lambat")
	public Date getPengirimanPalingLambat() {
		if (pengirimanPalingLambat == null && getPengirimanPalingLambatOld() != null) {
			pengirimanPalingLambat = getPengirimanPalingLambatOld();
		}
		return pengirimanPalingLambat;
	}

	/**
	 * Menyetel batas waktu pengiriman.
	 *
	 * <p>Hanya menulis ke kolom baru; kolom warisan tidak ikut diperbarui - lihat
	 * {@link #getPengirimanPalingLambat()}.</p>
	 *
	 * @param pengirimanPalingLambat batas waktu pengiriman
	 */
	public void setPengirimanPalingLambat(Date pengirimanPalingLambat) {
		this.pengirimanPalingLambat = pengirimanPalingLambat;
	}

	/**
	 * Mengembalikan nilai uang muka (DP) <b>sebelum</b> PPN.
	 *
	 * <p>Bila pesanan memakai skema termin ({@link #getByTermin()}), nilai ini
	 * <b>dipaksa nol</b> - kedua skema saling meniadakan, karena pembayaran
	 * bertahap sudah diatur seluruhnya oleh {@link #getFormula()}. Pemaksaan itu
	 * ditulis balik ke field, sehingga menyalakan bendera termin lalu membaca DP
	 * akan menghapus nilai DP dari basis data secara permanen: mengembalikan
	 * bendera ke false <b>tidak</b> memulihkan angkanya. Ini pola bendera satu
	 * arah yang berulang di AIS.</p>
	 *
	 * <p>Untuk nilai DP termasuk PPN, gunakan {@link #getDptotal()}. Pada PO
	 * pembelian langsung, DP berperan sebagai keseluruhan nilai pesanan - lihat
	 * {@link #getNilai()}.</p>
	 *
	 * @return nilai DP sebelum pajak; tidak pernah {@code null}
	 */
	public Double getDp() {

		if (getByTermin()) {
			dp = 0.0;
		}

		return dp == null ? 0.0 : dp;
	}

	/**
	 * Menyetel nilai uang muka sebelum PPN.
	 *
	 * <p><b>Peringatan</b>: nilai ini akan dihapus menjadi nol saat dibaca bila
	 * {@link #getByTermin()} menyala.</p>
	 *
	 * @param dp nilai DP sebelum pajak
	 */
	public void setDp(Double dp) {
		this.dp = dp;
	}

	/**
	 * Mengembalikan nilai total pesanan.
	 *
	 * <h3>Dua sumber nilai</h3>
	 * <ul>
	 *   <li><b>PO biasa</b> - nilai kolom, yang diisi kode aksi dengan hasil
	 *       penjumlahan {@link PemesananPengadaanMasterAssetDetail#getHargaTotal()}
	 *       seluruh baris (sudah termasuk potongan, PPN, dan opsional PPh).</li>
	 *   <li><b>PO pembelian langsung</b> - nilai <b>diambil alih</b> dari
	 *       {@link #getDptotal()}, yaitu DP termasuk PPN. Konsisten dengan cara
	 *       tombol "Beli Langsung" membentuk dokumen: ia menyetel DP sebesar nilai
	 *       permintaan, bukan menyusun baris berharga.</li>
	 * </ul>
	 *
	 * <h3>Konsekuensi: nilai header dapat menyimpang dari baris</h3>
	 * <p>Untuk PO biasa, nilai ini adalah <b>hasil hitung yang disimpan</b>, bukan
	 * nilai turunan yang dihitung ulang saat dibaca. Bila baris detail diubah
	 * lewat jalur yang tidak memicu perhitungan ulang header, nilai ini menjadi
	 * basi tanpa peringatan. Tidak ada pemeriksaan konsistensi antara nilai header
	 * dan penjumlahan baris di dalam entitas ini. Karena {@link #getLunas()}
	 * membandingkan nilai ini terhadap {@link #getDibayar()}, nilai header yang
	 * basi langsung mempengaruhi penilaian lunas.</p>
	 * <p>Untuk PO pembelian langsung, pengambilalihan dari DP terjadi <b>setiap
	 * kali dibaca</b> dan ditulis balik ke field, sehingga nilai apa pun yang
	 * disetel ke kolom akan tergantikan.</p>
	 *
	 * @return nilai total pesanan; tidak pernah {@code null}
	 * @see #getLunas()
	 * @see #getDptotal()
	 */
	public Double getNilai() {
		if (getPembelianLangsung()) {
			nilai = getDptotal();
		}
		return nilai == null ? 0.0 : nilai;
	}

	/**
	 * Menyetel nilai total pesanan.
	 *
	 * <p>Nilai yang disetel akan tergantikan oleh {@link #getDptotal()} saat
	 * dibaca bila PO bertanda pembelian langsung.</p>
	 *
	 * @param nilai nilai total pesanan
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Mengembalikan riwayat posting dokumen ini ke buku besar.
	 *
	 * <p>Kehadiran nilai ini menandakan PO sudah dijurnal. Modul akuntansi
	 * memakainya untuk mencegah posting ganda dan sebagai jangkar pembatalan
	 * jurnal. Perhatikan bahwa entitas ini <b>tidak</b> memakai nilai tersebut
	 * sebagai penjagaan apa pun - tidak ada getter di kelas ini yang menolak
	 * perubahan karena dokumen sudah terposting; penjagaan semacam itu, bila ada,
	 * berada di lapisan aksi.</p>
	 *
	 * <p><b>Tidak memanggil <code>check()</code></b>; dimuat seketika lewat SELECT
	 * terpisah (<code>@Fetch(FetchMode.SELECT)</code>).</p>
	 *
	 * @return riwayat posting, atau {@code null} bila belum dijurnal
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * Menyetel riwayat posting dokumen ini ke buku besar.
	 *
	 * @param postingHistory riwayat posting
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

	/**
	 * Mengembalikan tahun periode dokumen.
	 *
	 * <p><b>Getter destruktif dengan nilai bawaan</b>: bila field masih null, diisi
	 * tahun berjalan menurut waktu server dan ditulis balik ke field, sehingga
	 * pembacaan pada entitas terkelola dapat memicu UPDATE. Akibatnya dokumen lama
	 * yang kolom tahunnya kosong akan "mengadopsi" tahun saat pertama kali dibuka,
	 * bukan tahun dokumen sebenarnya. Untuk pelaporan historis andalkan
	 * {@link #getTanggalPembuatan()}.</p>
	 *
	 * @return tahun periode; tidak pernah {@code null}
	 */
	public Integer getTahun() {
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	/**
	 * Menyetel tahun periode dokumen.
	 *
	 * @param tahun tahun periode
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengembalikan template penomoran surat untuk dokumen ini.
	 *
	 * <p><b>Getter destruktif dengan nilai bawaan konstan</b>: bila field masih
	 * null, diisi {@link NomorSuratAlurPengadaan#PEMESANAN_PEMBELIAN_DATA}.
	 * Bandingkan dengan
	 * {@link PermintaanPengadaanMasterAsset#getNomorSuratAlurPengadaan()} yang
	 * memakai konstanta <code>PERMINTAAN_PEMBELIAN_DATA</code>; pasangan konstanta
	 * inilah yang membedakan format nomor surat PR dan PO.</p>
	 *
	 * <p>Perhatikan asimetri percabangannya: <code>check()</code> hanya dipanggil
	 * pada cabang "sudah terisi", karena konstanta bukan proxy terikat sesi.</p>
	 *
	 * @return template penomoran; tidak pernah {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat_alur_pengadaan", nullable = true)
	public NomorSuratAlurPengadaan getNomorSuratAlurPengadaan() {
		if (nomorSuratAlurPengadaan == null) {
			nomorSuratAlurPengadaan = NomorSuratAlurPengadaan.PEMESANAN_PEMBELIAN_DATA;
		} else {
			nomorSuratAlurPengadaan = check(nomorSuratAlurPengadaan);
		}
		return nomorSuratAlurPengadaan;
	}

	/**
	 * Menyetel template penomoran surat.
	 *
	 * @param nomorSuratAlurPengadaan template penomoran
	 */
	public void setNomorSuratAlurPengadaan(NomorSuratAlurPengadaan nomorSuratAlurPengadaan) {
		this.nomorSuratAlurPengadaan = nomorSuratAlurPengadaan;
	}

	/**
	 * Mengembalikan bulan periode dokumen dalam rentang 1-12.
	 *
	 * <p><b>Getter destruktif dengan nilai bawaan</b>, sama seperti
	 * {@link #getTahun()}. Perhatikan penambahan <code>+ 1</code>:
	 * {@link Calendar} memakai bulan berbasis nol sedangkan kolom ini menyimpan
	 * bulan berbasis satu agar sejajar dengan periode akuntansi. Jangan menghapus
	 * penambahan itu.</p>
	 *
	 * @return bulan periode 1-12; tidak pernah {@code null}
	 */
	public Integer getBulan() {
		if (bulan == null) {
			bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		}
		return bulan;
	}

	/**
	 * Menyetel bulan periode dokumen.
	 *
	 * <p>Tidak memvalidasi rentang; pemanggil bertanggung jawab memakai basis satu
	 * (1-12), bukan basis nol seperti {@link Calendar}.</p>
	 *
	 * @param bulan bulan periode 1-12
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Mengembalikan klausul kesepakatan yang tercetak pada lembar PO.
	 *
	 * <h3>Nilai bawaan berasal dari konfigurasi, bukan dari kode</h3>
	 * <p>Bila kolom kosong, teks bawaan diambil dari konfigurasi bernama
	 * <code>default_catatan_pemesanan_barang_jasa</code> lewat
	 * <code>Common.getKonfigurasi(nama, nilaiBawaan)</code>. Teks panjang yang
	 * tertulis sebagai argumen kedua di dalam kode <b>bukanlah</b> nilai yang
	 * dipakai sehari-hari - ia hanya benih.</p>
	 * <p><b>Peringatan penting yang berlaku di seluruh AIS</b>:
	 * <code>getKonfigurasi</code> berperilaku <i>auto-seed</i> - bila kunci
	 * konfigurasi belum ada di basis data, ia <b>menuliskan</b> nilai bawaan itu
	 * ke tabel konfigurasi lalu mengembalikannya. Artinya sejak pertama kali
	 * method ini dipanggil pada sebuah instalasi, teks bawaan sudah tersimpan di
	 * basis data, dan <b>mengubah teks di dalam kode Java ini tidak lagi
	 * berpengaruh apa pun</b>. Perubahan klausul harus dilakukan lewat layar
	 * konfigurasi. Jangan menyunting string di sini dengan harapan mengubah
	 * cetakan PO.</p>
	 * <p>Sisa dari benih itu sendiri sudah menua - ia menyebut tanggal serah
	 * "31 Maret 2023" dan menyisakan titik-titik untuk data rekening bank. Ini
	 * makin menegaskan bahwa nilai nyatanya berada di konfigurasi, bukan di sini.</p>
	 *
	 * <p>Berbeda dari kebanyakan getter di kelas ini, method ini <b>tidak
	 * destruktif</b>: nilai bawaan hanya muncul pada nilai balik, tidak ditulis ke
	 * field. Jadi kolom dokumen tetap null sampai pengguna menyunting klausulnya
	 * sendiri - dan dokumen yang klausulnya belum disunting akan selalu mengikuti
	 * konfigurasi terkini, termasuk perubahan setelah PO dicetak.</p>
	 *
	 * @return klausul kesepakatan dokumen ini, atau teks bawaan dari konfigurasi
	 */
	@Column(name = "catatan_kesepakatan_data", nullable = true, columnDefinition = "text")
	public String getCatatanKesepakatan() {
		return catatanKesepakatan == null ? Common.getKonfigurasi("default_catatan_pemesanan_barang_jasa",
				"Dengan kesepakatan sebagai berikut :\r\n"
						+ "1. Spesifikasi barang / kerangka acuan kerja, terlampir.\r\n"
						+ "2. Nilai total PO sudah termasuk PPN.\r\n"
						+ "3. Barang / Jasa akan diserahkan kepada pihak Yayasan Taruna Bakti, selambatnya 31 Maret 2023\r\n"
						+ "4. Pembayaran dilakukan dengan cara transfer via bank ke Rekening :\r\n" + "Bank .....\r\n"
						+ "Rek. .....\r\n" + "a/n. .....\r\n" + "KC. .....")
				.getNilai() : catatanKesepakatan;
	}

	/**
	 * Menyetel klausul kesepakatan khusus dokumen ini.
	 *
	 * <p>Menyetel nilai non-null membuat dokumen ini <b>berhenti mengikuti</b>
	 * teks konfigurasi - klausulnya membeku pada apa yang disetel.</p>
	 *
	 * @param catatanKesepakatan klausul kesepakatan
	 */
	public void setCatatanKesepakatan(String catatanKesepakatan) {
		this.catatanKesepakatan = catatanKesepakatan;
	}

	/**
	 * Mengembalikan <b>cache</b> total yang sudah dibayar atas pesanan ini.
	 *
	 * <p>Ini pembacaan kolom murni - tidak menghitung apa pun. Angka sebenarnya
	 * dihitung oleh {@link #hitungDibayar()} yang menembak basis data, dan hasil
	 * itu harus disimpan ke kolom ini oleh kode pemanggil agar layar daftar dapat
	 * menampilkan status pembayaran tanpa menjalankan empat kueri agregat per
	 * baris.</p>
	 *
	 * <p><b>Konsekuensi yang harus disadari</b>: nilai ini <b>basi sampai ada yang
	 * menyegarkannya</b>. {@link #getLunas()} membandingkan
	 * {@link #getNilai()} terhadap nilai ini - bukan terhadap
	 * {@link #hitungDibayar()} - sehingga status lunas yang ditampilkan hanya
	 * seakurat penyegaran terakhir. Untuk keputusan yang mengikat (misalnya
	 * menolak pembayaran berlebih), hitung ulang lewat
	 * {@link #hitungDibayar(Session)}.</p>
	 *
	 * <p>Mengembalikan {@code 0.0} untuk null.</p>
	 *
	 * @return total dibayar menurut cache terakhir; tidak pernah {@code null}
	 */
	public Double getDibayar() {
		return dibayar == null ? 0.0 : dibayar;
	}

	/**
	 * Menyetel cache total yang sudah dibayar.
	 *
	 * @param dibayar total dibayar
	 */
	public void setDibayar(Double dibayar) {
		this.dibayar = dibayar;
	}

	/**
	 * Menghitung ulang total yang sudah dibayar atas pesanan ini, <b>mengelola
	 * sesi Hibernate sendiri</b>.
	 *
	 * <h3>Kapan memakai varian ini</h3>
	 * <p>Gunakan dari konteks yang <b>tidak</b> sedang berada di dalam transaksi -
	 * misalnya perenderan layar daftar atau tugas latar. Bila sudah berada di
	 * dalam transaksi, panggil {@link #hitungDibayar(Session)} dengan sesi milik
	 * pemanggil.</p>
	 *
	 * <h3>Penjagaan sesi yang menentukan</h3>
	 * <p>Komentar dalam badan method mendokumentasikan perangkap yang pernah
	 * menjadi sumber galat nyata: <code>HibernateUtil.currentNativeSession()</code>
	 * bersifat <i>thread-local</i>. Bila pemanggil sedang membuka transaksi pada
	 * sesi yang sama - contoh yang disebut komentar adalah
	 * <code>PembayaranTerminMasterAssetHelper.loadDataDetail</code> - maka menutup
	 * sesi di sini akan membuat <code>tx.commit()</code> milik pemanggil gagal
	 * dengan pesan "Session is closed". Karena itu method memeriksa dulu apakah
	 * ada transaksi aktif, dan <b>hanya menutup sesi bila tidak ada</b>, yang
	 * berarti sesi itu memang miliknya sendiri.</p>
	 * <p>Pemeriksaan transaksi itu sendiri dibungkus try-catch dan gagal ke
	 * <code>false</code> - keputusan yang perlu dipahami arahnya: bila
	 * pemeriksaan gagal, method menganggap <b>tidak ada</b> transaksi dan
	 * karenanya <b>akan menutup</b> sesi. Pilihan konservatif untuk kebocoran
	 * sumber daya, tetapi agresif terhadap pemanggil bertransaksi. Bila kelak
	 * muncul galat "Session is closed" yang bersumber dari sini, arahkan pemanggil
	 * ke varian bersesi eksplisit, jangan mengubah arah kegagalan di sini tanpa
	 * mempertimbangkan kebocoran.</p>
	 * <p>Penutupan dilakukan berlapis - <code>disconnect()</code>, lalu
	 * <code>close()</code>, lalu <code>HibernateUtil.closeSession()</code> - agar
	 * baik koneksi JDBC maupun pencatatan sesi pada utilitas ikut dilepas.</p>
	 *
	 * <h3>Penjagaan awal</h3>
	 * <p>Bila {@link #getId()} masih null, langsung dikembalikan {@code 0.0}
	 * tanpa menyentuh basis data: PO yang belum tersimpan mustahil memiliki
	 * pembayaran, dan kriteria yang membandingkan id null akan menghasilkan kueri
	 * tak bermakna.</p>
	 *
	 * <h3>Perhatikan: hasilnya tidak disimpan</h3>
	 * <p>Method ini <b>tidak</b> menulis hasil ke {@link #setDibayar(Double)}.
	 * Penyegaran cache adalah tanggung jawab pemanggil. Karena itu memanggil
	 * method ini saja tidak akan memperbaiki nilai {@link #getLunas()} yang
	 * ditampilkan.</p>
	 *
	 * @return total yang sudah dibayar menurut basis data; {@code 0.0} bila PO
	 *         belum tersimpan
	 * @see #hitungDibayar(Session)
	 * @see #getDibayar()
	 */
	public Double hitungDibayar() {

		if (getId() == null) {
			return 0.0;
		}
		Session session = HibernateUtil.currentNativeSession();

		/* PENTING: currentNativeSession() bersifat thread-local. Bila
		 * pemanggil sedang membuka transaksi pada session yang sama
		 * (mis. PembayaranTerminMasterAssetHelper.loadDataDetail), maka
		 * session TIDAK boleh ditutup di sini - menutupnya membuat
		 * tx.commit() pemanggil gagal "Session is closed". Session hanya
		 * ditutup bila tidak ada transaksi aktif (berarti milik sendiri). */
		boolean transaksiAktif = false;
		try {
			transaksiAktif = session.getTransaction() != null && session.getTransaction().isActive();
		} catch (Exception e) {
			transaksiAktif = false;
		}

		Double hasil = hitungDibayar(session);

		if (!transaksiAktif) {
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}

		return hasil;
	}

	/**
	 * Varian yang memakai session milik pemanggil dan TIDAK pernah
	 * menutupnya. Gunakan ini dari dalam transaksi yang sedang berjalan.
	 *
	 * <h3>Apa yang dijumlahkan: empat kanal yang saling meniadakan</h3>
	 * <p>Pembayaran atas sebuah PO dapat masuk lewat empat jalur berbeda, dan
	 * skema pesanan menentukan jalur mana yang sah. Method ini menghitung keempat
	 * agregat itu tetapi <b>menonolkan</b> yang tidak berlaku, memakai bendera
	 * {@link #getByTermin()}, {@link #getPembelianLangsung()}, dan
	 * {@link #getDptotal()} sebagai penentu:</p>
	 * <ol>
	 *   <li><b>Uang muka</b> ({@link PembayaranDpMasterAssetDetail}) - dihitung
	 *       hanya bila PO bertanda pembelian langsung <i>atau</i> memiliki DP di
	 *       atas ambang 0,1.</li>
	 *   <li><b>Termin</b> ({@link PembayaranTerminMasterAssetDetail}) - dihitung
	 *       hanya bila PO bertermin.</li>
	 *   <li><b>Pembayaran pengadaan biasa</b>
	 *       ({@link PembayaranPengadaanMasterAssetDetail}) - dihitung hanya bila
	 *       PO <i>bukan</i> bertermin, <i>bukan</i> pembelian langsung, dan
	 *       <i>tidak</i> ber-DP. Jalur ini menembus dua tingkat: baris pembayaran
	 *       menunjuk dokumen penerimaan, dan dokumen penerimaan itulah yang
	 *       menunjuk PO ini.</li>
	 *   <li><b>Saldo awal</b> ({@link SaldoAwalMasterAsset}) - syarat sama dengan
	 *       jalur ketiga, ditambah keharusan memiliki daftar pengajuan transfer.
	 *       Jalur ini menangkap pembayaran yang dibukukan lewat mekanisme saldo
	 *       awal aset.</li>
	 * </ol>
	 * <p>Perhatikan bahwa <b>setiap</b> kanal menyaring
	 * <code>disetujuiOleh IS NOT NULL</code> pada dokumen pembayarannya. Pembayaran
	 * yang belum disetujui tidak dihitung - gerbang persetujuan ditegakkan di
	 * tingkat agregasi, bukan hanya di layar.</p>
	 * <p>Ambang <code>getDptotal() &gt; 0.1</code> dipakai alih-alih perbandingan
	 * terhadap nol karena nilai bertipe {@link Double} hasil perkalian persentase
	 * pajak dapat menyisakan galat pembulatan yang sangat kecil. Konsekuensinya, DP
	 * bernilai di bawah 0,1 satuan mata uang diperlakukan sebagai tidak ada -
	 * tidak bermakna dalam praktik rupiah.</p>
	 * <p>Keempat hasil dijumlahkan dengan menormalkan null menjadi nol, karena
	 * <code>Projections.sum()</code> mengembalikan {@code null} bila tidak ada
	 * baris yang cocok.</p>
	 *
	 * <h3>Mengapa FlushMode dimatikan selama menghitung</h3>
	 * <p>Komentar dalam badan method menjelaskan akarnya, dan penjelasan itu
	 * berlaku umum untuk seluruh modul: entitas AIS dipenuhi <b>getter
	 * destruktif</b> yang menulis balik ke field saat dibaca. Bila Hibernate
	 * berada pada mode flush otomatis, kueri agregat di sini akan memicu
	 * <i>auto-flush</i>, dan flush itu menuliskan entitas "kotor" - yang paling
	 * berbahaya adalah {@link DisposisiSop} yang menjadi kotor akibat getter
	 * status di kelas ini sendiri - di tengah perhitungan. Akibat nyatanya adalah
	 * <i>statement timeout</i> atau <i>deadlock</i> pada tabel
	 * <code>disposisi_sop</code>. Karena itu mode flush diubah ke
	 * <code>MANUAL</code> selama perhitungan.</p>
	 * <p>Mode asli <b>dipulihkan di blok <code>finally</code></b> agar perilaku
	 * transaksi pemanggil tidak berubah - ini penting karena method ini memakai
	 * sesi milik orang lain. Baik penyetelan maupun pemulihan dibungkus try-catch
	 * yang mencatat kegagalannya ke <code>ErrorAuditUtil</code>, sehingga sesi yang
	 * sudah tidak sehat tidak menjatuhkan perhitungan.</p>
	 * <p><b>Jangan menghapus pengaturan FlushMode ini</b> saat menyunting method.
	 * Gejalanya tidak muncul di pengujian bervolume kecil; ia muncul sebagai
	 * kebuntuan basis data pada beban nyata.</p>
	 *
	 * <h3>Kontrak sesi</h3>
	 * <p>Method ini <b>tidak pernah</b> menutup atau memutus sesi yang diberikan,
	 * dan tidak membuka transaksi sendiri. Seluruh tanggung jawab daur hidup sesi
	 * ada pada pemanggil. Sama seperti varian tanpa argumen, hasilnya <b>tidak</b>
	 * disimpan ke {@link #setDibayar(Double)}.</p>
	 *
	 * @param session sesi Hibernate milik pemanggil; tidak akan ditutup
	 * @return total yang sudah dibayar menurut basis data; {@code 0.0} bila PO
	 *         belum tersimpan
	 * @see #hitungDibayar()
	 */
	public Double hitungDibayar(Session session) {

		if (getId() == null) {
			return 0.0;
		}

		// FlushMode MANUAL selama kalkulasi baca (SUM pembayaran). Tanpa ini, auto-flush dapat
		// menulis entitas "kotor" (mis. DisposisiSop akibat getter ber-efek samping) di tengah
		// perhitungan → memicu statement timeout/deadlock pada disposisi_sop. Mode flush asli
		// dipulihkan di finally agar perilaku transaksi pemanggil tidak berubah.
		org.hibernate.FlushMode flushDibayarAsli = null;
		try {
			try {
				flushDibayarAsli = session.getFlushMode();
				session.setFlushMode(org.hibernate.FlushMode.MANUAL);
			} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/database/model/asset/PemesananPengadaanMasterAsset.java:584");
			}

		Number dibayarDp = !(getPembelianLangsung() || getDptotal() > 0.1) ? 0.0
				: ((Number) session.createCriteria(PembayaranDpMasterAssetDetail.class)
						.createAlias("pembayaranDpMasterAsset", "pembayaranDpMasterAsset")
						.add(Restrictions.isNotNull("pembayaranDpMasterAsset.disetujuiOleh"))
						.add(Restrictions.eq("pemesananPengadaanMasterAsset.id", getId()))
						.setProjection(Projections.sum("dibayar")).uniqueResult());

		Number dibayarTermin = !getByTermin() ? 0.0
				: ((Number) session.createCriteria(PembayaranTerminMasterAssetDetail.class)
						.createAlias("pembayaranTerminMasterAsset", "pembayaranTerminMasterAsset")
						.add(Restrictions.isNotNull("pembayaranTerminMasterAsset.disetujuiOleh"))
						.add(Restrictions.eq("pemesananPengadaanMasterAsset.id", getId()))
						.setProjection(Projections.sum("dibayar")).uniqueResult());

		Number dibayarPengadaan = (getByTermin() || getPembelianLangsung() || getDptotal() > 0.1) ? 0.0
				: ((Number) session.createCriteria(PembayaranPengadaanMasterAssetDetail.class)
						.createAlias("pembayaranPengadaanMasterAsset", "pembayaranPengadaanMasterAsset")
						.add(Restrictions.isNotNull("pembayaranPengadaanMasterAsset.disetujuiOleh"))
						.createAlias("penerimaanPengadaanMasterAsset", "penerimaanPengadaanMasterAsset")
						.add(Restrictions.eq("penerimaanPengadaanMasterAsset.pemesananPengadaanMasterAsset.id",
								getId()))
						.setProjection(Projections.sum("dibayar")).uniqueResult());

		Number dibayarSaldoAwal = (getByTermin() || getPembelianLangsung() || getDptotal() > 0.1) ? 0.0
				: ((Number) session.createCriteria(SaldoAwalMasterAsset.class)
						.add(Restrictions.isNotNull("daftarPengajuanTransfer"))
						.add(Restrictions.isNotNull("disetujuiOleh"))
						.createAlias("penerimaanPengadaanMasterAsset", "penerimaanPengadaanMasterAsset")
						.add(Restrictions.eq("penerimaanPengadaanMasterAsset.pemesananPengadaanMasterAsset.id",
								getId()))
						.setProjection(Projections.sum("nilai")).uniqueResult());

		Double dibayar = (dibayarDp == null ? 0.0 : dibayarDp.doubleValue())
				+ (dibayarTermin == null ? 0.0 : dibayarTermin.doubleValue())
				+ (dibayarPengadaan == null ? 0.0 : dibayarPengadaan.doubleValue())
				+ (dibayarSaldoAwal == null ? 0.0 : dibayarSaldoAwal.doubleValue());

			return dibayar;
		} finally {
			if (flushDibayarAsli != null) {
				try {
					session.setFlushMode(flushDibayarAsli);
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/database/model/asset/PemesananPengadaanMasterAsset.java:629");
				}
			}
		}
	}

	/**
	 * Mengembalikan apakah pesanan ini sudah lunas.
	 *
	 * <h3>Selalu dihitung ulang - dan perbandingannya memakai pembulatan ke bawah</h3>
	 * <p>Nilai kolom tidak pernah dipercaya; status dihitung sebagai
	 * <code>getNilai().intValue() &lt;= getDibayar().intValue()</code> lalu
	 * ditulis balik ke field (sehingga pembacaan pada entitas terkelola dapat
	 * memicu UPDATE).</p>
	 * <p>Pemakaian <code>intValue()</code> pada kedua sisi <b>bukan sekadar
	 * konversi</b> - ia memangkas bagian pecahan ke bawah. Efeknya: pesanan
	 * dianggap lunas meski masih kurang bayar hingga satu satuan mata uang penuh
	 * (misalnya nilai 1.000.000,90 melawan dibayar 1.000.000,00 dinilai lunas).
	 * Untuk rupiah, selisih di bawah satu rupiah memang tidak bermakna, dan
	 * toleransi ini justru mencegah PO menggantung "belum lunas" hanya karena
	 * galat pembulatan pada perhitungan PPN. Namun perlu diketahui bahwa ini
	 * <b>toleransi searah</b>: kelebihan bayar tidak dideteksi sama sekali.</p>
	 * <p><b>Peringatan luapan</b>: <code>intValue()</code> pada
	 * {@link Double} memotong ke rentang 32-bit. Nilai pesanan di atas sekitar
	 * 2,15 miliar akan meluap dan menghasilkan perbandingan yang keliru. Untuk
	 * pengadaan bernilai besar, jangan mengandalkan properti ini.</p>
	 *
	 * <h3>Bertumpu pada dua nilai yang keduanya dapat basi</h3>
	 * <p>Sisi kiri, {@link #getNilai()}, adalah hasil hitung yang disimpan dan
	 * tidak diverifikasi ulang terhadap baris detail. Sisi kanan,
	 * {@link #getDibayar()}, adalah <i>cache</i> yang hanya seakurat penyegaran
	 * terakhir - ia <b>tidak</b> memanggil {@link #hitungDibayar()}. Jadi status
	 * lunas yang ditampilkan adalah hasil membandingkan dua angka yang keduanya
	 * bisa tertinggal dari kenyataan. Untuk keputusan yang mengikat, hitung ulang
	 * lewat {@link #hitungDibayar(Session)} dan bandingkan sendiri.</p>
	 *
	 * <p><b>CATATAN PERBAIKAN:</b> Perbandingan sebelumnya memakai
	 * {@code intValue()} pada kedua sisi, yang memotong bagian pecahan ke bawah
	 * dan meluap pada nilai di atas ~2,15 miliar (batas {@code int} 32-bit) —
	 * risiko nyata untuk transaksi pengadaan aset bernilai besar (gedung,
	 * kendaraan). Perbandingan kini memakai {@code double} secara langsung
	 * dengan toleransi pembulatan kecil yang eksplisit, sehingga tidak ada lagi
	 * potensi luapan dan arah pembulatan tidak lagi bergantung pada pemotongan
	 * implisit tipe data. Sisi kiri dan kanan tetap {@link #getNilai()} dan
	 * {@link #getDibayar()} — keduanya cache yang dapat basi seperti dicatat di
	 * atas; pemanggil yang memerlukan kepastian tetap harus menghitung ulang
	 * lewat {@link #hitungDibayar(Session)}.</p>
	 *
	 * @return {@code true} bila nilai pesanan sudah tertutup pembayaran (dengan
	 *         toleransi pembulatan kecil); tidak pernah {@code null}
	 * @see #hitungDibayar(Session)
	 */
	public Boolean getLunas() {
		double toleransiPembulatan = 1.0;
		lunas = (getNilai() - getDibayar()) <= toleransiPembulatan;
		return lunas;
	}

	/**
	 * Menyetel penanda lunas.
	 *
	 * <p><b>Praktis tidak berguna</b>: {@link #getLunas()} selalu menghitung
	 * ulang. Setter tetap ada karena dibutuhkan Hibernate untuk memuat kolom.</p>
	 *
	 * @param lunas penanda lunas (akan tertimpa saat dibaca)
	 */
	public void setLunas(Boolean lunas) {
		this.lunas = lunas;
	}

	/**
	 * Mengembalikan nomor invoice pesanan.
	 *
	 * <p><b>Pembangkitan bersyarat</b>: nomor hanya dibangkitkan untuk PO yang
	 * memiliki uang muka ({@link #getDptotal()} di atas ambang 0,1), berformat
	 * <code>"INV-" + id</code>, dan hanya bila PO sudah tersimpan serta kolomnya
	 * masih kosong. Hasilnya ditulis balik ke field, sehingga pembacaan pada
	 * entitas terkelola dapat memicu UPDATE - <b>getter destruktif</b>.</p>
	 * <p>Alasan syaratnya: invoice diperlukan ketika ada tagihan di muka kepada
	 * pihak internal sebelum barang datang. PO tanpa DP ditagih lewat dokumen
	 * penerimaan, bukan lewat invoice PO.</p>
	 * <p>Karena bertumpu pada {@link #getId()}, nomor invoice tidak akan pernah
	 * terbentuk untuk PO yang belum disimpan. Dan karena hanya dibangkitkan saat
	 * kolom masih kosong, nomor yang sudah pernah terbentuk <b>tidak berubah</b>
	 * meski DP kemudian dihapus - jejaknya sengaja dipertahankan.</p>
	 *
	 * @return nomor invoice, atau {@code null} bila PO tidak ber-DP dan kolomnya
	 *         belum pernah diisi
	 */
	public String getKodeInvoice() {
		if (getDptotal() > 0.1) {
			if (getId() != null && (kodeInvoice == null || kodeInvoice.isEmpty())) {
				kodeInvoice = "INV-" + getId();
			}
		}
		return kodeInvoice;
	}

	/**
	 * Menyetel nomor invoice pesanan.
	 *
	 * <p>Nilai yang disetel dipertahankan; {@link #getKodeInvoice()} hanya
	 * membangkitkan nomor bila kolom masih kosong.</p>
	 *
	 * @param kodeInvoice nomor invoice
	 */
	public void setKodeInvoice(String kodeInvoice) {
		this.kodeInvoice = kodeInvoice;
	}

	/**
	 * Mengembalikan perjanjian kerjasama (kontrak payung) yang menaungi pesanan
	 * ini.
	 *
	 * <p>Bila pengadaan menempuh kontrak payung, alurnya menjadi
	 * PR -&gt; Perjanjian Kerjasama -&gt; PO. Dalam skema itu baris-baris PO
	 * dibentuk dari baris kontrak, bukan langsung dari baris PR - dan
	 * {@link PemesananPengadaanMasterAssetDetail#getPermintaanPengadaanMasterAssetDetail()}
	 * memulihkan asal-usul permintaannya lewat baris kontrak, sehingga jejak ke PR
	 * tetap utuh meski dokumen perantaranya berbeda.</p>
	 *
	 * <p><b>Tidak memanggil <code>check()</code></b>; dimuat seketika lewat SELECT
	 * terpisah.</p>
	 *
	 * @return kontrak payung, atau {@code null} bila pengadaan langsung
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "perjanjian_kerjasama_master_asset", nullable = true)
	public PerjanjianKerjasamaMasterAsset getPerjanjianKerjasamaMasterAsset() {
		return perjanjianKerjasamaMasterAsset;
	}

	/**
	 * Menyetel perjanjian kerjasama yang menaungi pesanan ini.
	 *
	 * @param perjanjianKerjasamaMasterAsset kontrak payung
	 */
	public void setPerjanjianKerjasamaMasterAsset(PerjanjianKerjasamaMasterAsset perjanjianKerjasamaMasterAsset) {
		this.perjanjianKerjasamaMasterAsset = perjanjianKerjasamaMasterAsset;
	}

	/**
	 * Mengembalikan jejak <b>id mata anggaran</b> yang dibebani pesanan ini,
	 * sebagai daftar dipisah koma.
	 *
	 * <p>Sama seperti {@link #getPermintaanPengadaanMasterAssets()}, ini kolom
	 * <code>text</code> berisi denormalisasi - bukan relasi terpetakan. Isinya
	 * dirangkai kode aksi dari <code>Workspace.getId()</code> milik setiap
	 * permintaan yang dipilih, sehingga satu PO yang menyerap beberapa PR lintas
	 * mata anggaran tetap dapat dilacak pembebanannya. Nama fieldnya mengandung
	 * salah eja yang sudah membeku di kolom ("angarans" seharusnya "anggarans");
	 * jangan diperbaiki tanpa migrasi.</p>
	 *
	 * <p>Pembersihan nilai sampah dan sifat destruktifnya identik dengan
	 * {@link #getPermintaanPengadaanMasterAssets()} - termasuk keterbatasannya:
	 * hanya empat pola koma kosong yang ditangani, dan pembacaan saja dapat
	 * mengubah kolom dari NULL menjadi string kosong. Pemakai yang mem-split hasil
	 * harus menyaring potongan kosong sendiri.</p>
	 *
	 * @return daftar id mata anggaran dipisah koma; tidak pernah {@code null}
	 * @see #getPermintaanPengadaanMasterAssets()
	 */
	@Column(columnDefinition = "text")
	public String getAngarans() {
		angarans = (angarans == null ? "" : angarans.trim());

		if (angarans.equals(",")) {
			angarans = "";
		} else if (angarans.equals(",,")) {
			angarans = "";
		} else if (angarans.equals(",,,")) {
			angarans = "";
		} else if (angarans.equals(",,,,")) {
			angarans = "";
		}
		return angarans;
	}

	/**
	 * Menyetel jejak CSV id mata anggaran.
	 *
	 * <p>Dibangun ulang penuh oleh kode aksi setiap penyimpanan PO.</p>
	 *
	 * @param angarans daftar id mata anggaran dipisah koma
	 */
	public void setAngarans(String angarans) {
		this.angarans = angarans;
	}

	/**
	 * Mengembalikan apakah pesanan ini dibayar secara <b>bertermin</b>.
	 *
	 * <p>Bendera penentu skema pembayaran yang mempengaruhi empat properti lain:
	 * {@link #getDp()} dipaksa nol, {@link #getJenisPajakPpnDp()} dipaksa
	 * {@code null}, {@link #hitungDibayar(Session)} beralih menjumlahkan kanal
	 * termin dan menonolkan tiga kanal lainnya, dan {@link #getFormula()} menjadi
	 * bermakna sebagai definisi tahapan.</p>
	 * <p>Perhatikan bahwa dua di antaranya - DP dan PPN DP - bersifat
	 * <b>merusak</b>: pemaksaannya ditulis balik ke field sehingga nilai yang
	 * pernah tersimpan hilang permanen. Menyalakan bendera ini lalu membatalkannya
	 * tidak memulihkan nilai DP maupun jenis pajaknya.</p>
	 *
	 * <p>Mengembalikan {@code false} untuk null.</p>
	 *
	 * @return {@code true} bila pesanan bertermin; tidak pernah {@code null}
	 */
	public Boolean getByTermin() {
		return byTermin == null ? false : byTermin;
	}

	/**
	 * Menyetel penanda pembayaran bertermin.
	 *
	 * <p><b>Peringatan</b>: menyalakan bendera ini menghapus DP dan jenis PPN DP
	 * secara permanen pada pembacaan berikutnya.</p>
	 *
	 * @param byTermin {@code true} bila pesanan bertermin
	 */
	public void setByTermin(Boolean byTermin) {
		this.byTermin = byTermin;
	}

	/**
	 * Nilai bawaan formula termin berupa larik JSON kosong.
	 *
	 * <p><b>Perhatikan: konstanta ini tidak dipakai oleh {@link #getFormula()}.</b>
	 * Getter tersebut justru memakai
	 * {@link Pertangungjawaban#DEFAULT_FORMULA} sebagai nilai bawaan. Konstanta di
	 * sini disediakan untuk pemanggil luar yang perlu membentuk formula kosong
	 * tanpa merujuk kelas akuntansi. Karena bersifat <code>public static</code>
	 * dan <b>bukan <code>final</code></b>, nilainya secara teknis dapat diubah dari
	 * mana saja - jangan mengandalkannya sebagai konstanta yang benar-benar
	 * tetap.</p>
	 */
	public static String DEFAULT_FORMULA = new JSONArray().toString();

	/**
	 * Mengembalikan definisi tahapan pembayaran (termin) dalam bentuk teks JSON.
	 *
	 * <p>Berisi larik JSON yang menguraikan tiap termin - persentase atau nominal,
	 * beserta syaratnya - dan dibaca modul pembayaran termin untuk membentuk
	 * jadwal tagihan. Isinya hanya bermakna bila {@link #getByTermin()}
	 * menyala.</p>
	 *
	 * <p>Bila kolom kosong atau berisi string kosong, dikembalikan
	 * {@link Pertangungjawaban#DEFAULT_FORMULA} - bukan {@link #DEFAULT_FORMULA}
	 * milik kelas ini, meski keduanya bernama sama. Bentuk berbagi konstanta ini
	 * disengaja agar struktur formula pada PO dan pada dokumen pertanggungjawaban
	 * tetap sepadan; bila format formula diubah di modul akuntansi, PO ikut
	 * berubah tanpa perlu disunting.</p>
	 *
	 * <p>Berbeda dari kebanyakan getter di kelas ini, method ini <b>tidak
	 * destruktif</b>: nilai bawaan hanya muncul pada nilai balik dan tidak ditulis
	 * ke field.</p>
	 *
	 * @return teks JSON definisi termin; tidak pernah {@code null}
	 */
	@Column(name = "formula", nullable = true, columnDefinition = "text")
	public String getFormula() {
		return formula == null || formula.isEmpty() ? Pertangungjawaban.DEFAULT_FORMULA : formula;
	}

	/**
	 * Menyetel definisi tahapan pembayaran dalam bentuk teks JSON.
	 *
	 * <p>Tidak memvalidasi bahwa isinya JSON yang sah; kesalahan format baru
	 * terdeteksi saat modul pembayaran termin mem-parsingnya.</p>
	 *
	 * @param formula teks JSON definisi termin
	 */
	public void setFormula(String formula) {
		this.formula = formula;
	}

	/**
	 * Mengembalikan tanggal awal masa berlaku pesanan atau pekerjaan.
	 *
	 * <p>Dipetakan sebagai {@link TemporalType#DATE} - <b>tanpa komponen jam</b>,
	 * berbeda dari tanggal-tanggal audit di kelas ini yang bertipe TIMESTAMP.
	 * Bersama {@link #getTanggalSampai()} menandai rentang pelaksanaan, terutama
	 * relevan untuk pesanan jasa dan pekerjaan bertermin.</p>
	 *
	 * @return tanggal mulai, atau {@code null}
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalMulai() {
		return tanggalMulai;
	}

	/**
	 * Menyetel tanggal awal masa berlaku pesanan.
	 *
	 * @param tanggalMulai tanggal mulai
	 */
	public void setTanggalMulai(Date tanggalMulai) {
		this.tanggalMulai = tanggalMulai;
	}

	/**
	 * Mengembalikan tanggal akhir masa berlaku pesanan atau pekerjaan.
	 *
	 * <p>Dipetakan sebagai {@link TemporalType#DATE}, tanpa komponen jam. Entitas
	 * ini <b>tidak</b> memvalidasi bahwa tanggal akhir berada setelah
	 * {@link #getTanggalMulai()}; validasi semacam itu, bila ada, berada di
	 * lapisan aksi.</p>
	 *
	 * @return tanggal sampai, atau {@code null}
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalSampai() {
		return tanggalSampai;
	}

	/**
	 * Menyetel tanggal akhir masa berlaku pesanan.
	 *
	 * @param tanggalSampai tanggal sampai
	 */
	public void setTanggalSampai(Date tanggalSampai) {
		this.tanggalSampai = tanggalSampai;
	}

	/**
	 * Mengembalikan pengguna yang menolak pesanan ini.
	 *
	 * <p><b>Penentu tertinggi</b> dalam logika status: baik
	 * {@link #getDisetujuiOleh()} maupun {@link #getTanggalPersetujuan()} memaksa
	 * hasilnya {@code null} begitu penolak terisi - <b>termasuk mengalahkan
	 * pembelian langsung</b>, karena pemeriksaan penolakan diulang setelah cabang
	 * pembelian langsung pada kedua getter tersebut. Penolakan karena itu tidak
	 * dapat "tertutup" oleh mekanisme apa pun; pembatalannya harus eksplisit lewat
	 * tombol "Batalkan".</p>
	 *
	 * <p>Berbeda dari penyetuju, penolak tidak diturunkan dari alur SOP - ia murni
	 * nilai kolom yang di-<i>reattach</i>. Penolakan lewat alur SOP tercermin di
	 * {@link #getAktif()}.</p>
	 *
	 * <p><b>Getter destruktif</b> karena memanggil <code>check()</code>.</p>
	 *
	 * @return pengguna penolak, atau {@code null} bila belum ditolak
	 * @see #getAlasanDitolak()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ditolak_oleh", nullable = true)
	public Tbmuser getDitolakOleh() {
		ditolakOleh = check(ditolakOleh);
		return ditolakOleh;
	}

	/**
	 * Menyetel pengguna yang menolak pesanan ini.
	 *
	 * <p>Disetel bersamaan dengan {@link #setTanggalDitolak(Date)} oleh tombol
	 * "Ditolak"; menyetelnya sendirian membuat dokumen tampak ditolak tanpa
	 * tanggal.</p>
	 *
	 * @param ditolakOleh pengguna penolak, atau {@code null} untuk membatalkan
	 */
	public void setDitolakOleh(Tbmuser ditolakOleh) {
		this.ditolakOleh = ditolakOleh;
	}

	/**
	 * Mengembalikan cap waktu penolakan dokumen.
	 *
	 * <p>Berbeda dari {@link #getTanggalPersetujuan()}, method ini <b>tidak
	 * destruktif dan tidak menurunkan nilai apa pun</b> dari alur SOP - hanya
	 * membaca kolom. Asimetri ini disengaja: persetujuan dapat datang dari mesin
	 * SOP, sedangkan penolakan pada dokumen ini selalu tindakan manual di layar
	 * PO.</p>
	 *
	 * @return cap waktu penolakan, atau {@code null} bila belum ditolak
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_ditolak")
	public Date getTanggalDitolak() {
		return tanggalDitolak;
	}

	/**
	 * Menyetel cap waktu penolakan dokumen.
	 *
	 * @param tanggalDitolak cap waktu penolakan, atau {@code null}
	 */
	public void setTanggalDitolak(Date tanggalDitolak) {
		this.tanggalDitolak = tanggalDitolak;
	}

	/**
	 * Mengembalikan mata anggaran (workspace) yang membebani pesanan ini.
	 *
	 * <h3>Bendera satu arah yang memaksa null</h3>
	 * <p>Bila {@link #getTanpaAnggaran()} menyala, method ini <b>memaksa hasilnya
	 * {@code null}</b> dan - karena destruktif - menuliskan pemaksaan itu ke field,
	 * sehingga kolom <code>workspace</code> ikut dikosongkan pada flush
	 * berikutnya. Kaitan anggaran <b>hilang permanen</b>; mengembalikan bendera ke
	 * false tidak memulihkannya. Kode aksi yang mengganti bendera harus menyetel
	 * ulang workspace secara eksplisit.</p>
	 *
	 * <h3>Efek berantai</h3>
	 * <p>Nilai ini menjadi sumber bagi {@link #getSatuanKerja()}. Mengosongkan
	 * workspace karena itu ikut melepas dokumen dari filter tenant berbasis satuan
	 * kerja - lihat peringatan pada getter tersebut.</p>
	 *
	 * <p><b>Getter destruktif</b> pada kedua cabang.</p>
	 *
	 * @return mata anggaran pembebanan, atau {@code null} bila PO ditandai tanpa
	 *         anggaran
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace", nullable = true)
	public Workspace getWorkspace() {

		if (getTanpaAnggaran()) {
			workspace = null;
		} else {
			workspace = check(workspace);
		}
		return workspace;
	}

	/**
	 * Menyetel mata anggaran pembebanan.
	 *
	 * <p>Nilai yang disetel akan dibuang saat dibaca bila
	 * {@link #getTanpaAnggaran()} menyala.</p>
	 *
	 * @param workspace mata anggaran
	 */
	public void setWorkspace(Workspace workspace) {
		this.workspace = workspace;
	}

	/**
	 * Mengembalikan apakah pesanan ini diterbitkan di luar mata anggaran.
	 *
	 * <p>Bendera ini memiliki <b>efek merusak</b> pada {@link #getWorkspace()}:
	 * selama menyala, workspace dipaksa {@code null} dan pemaksaan itu ditulis ke
	 * kolom, sekaligus melepaskan turunannya {@link #getSatuanKerja()}. Selain itu
	 * ia melonggarkan validasi layar PO: kewajiban mengisi mata anggaran untuk PO
	 * tanpa permintaan hanya berlaku bila bendera ini padam.</p>
	 *
	 * <p>Mengembalikan {@code false} untuk null.</p>
	 *
	 * @return {@code true} bila pesanan di luar anggaran; tidak pernah
	 *         {@code null}
	 */
	public Boolean getTanpaAnggaran() {
		return tanpaAnggaran == null ? false : tanpaAnggaran;
	}

	/**
	 * Menyetel penanda pesanan di luar mata anggaran.
	 *
	 * <p><b>Peringatan</b>: menyetel {@code true} lalu membaca
	 * {@link #getWorkspace()} akan mengosongkan kaitan anggaran secara permanen.</p>
	 *
	 * @param tanpaAnggaran {@code true} bila di luar anggaran
	 */
	public void setTanpaAnggaran(Boolean tanpaAnggaran) {
		this.tanpaAnggaran = tanpaAnggaran;
	}

	/**
	 * Mengembalikan apakah pesanan ini merupakan <b>pembelian langsung</b>.
	 *
	 * <h3>Apa yang diubah bendera ini</h3>
	 * <p>Bendera ini mengubah empat perilaku sekaligus, dan tiga di antaranya
	 * menyangkut status persetujuan:</p>
	 * <ol>
	 *   <li>{@link #getDisetujuiOleh()} mengembalikan {@link #getDibuatOleh()} -
	 *       dokumen menyetujui dirinya sendiri;</li>
	 *   <li>{@link #getTanggalPersetujuan()} mengembalikan
	 *       {@link #getTanggalPembuatan()};</li>
	 *   <li>{@link #getNilai()} diambil alih dari {@link #getDptotal()};</li>
	 *   <li>{@link #hitungDibayar(Session)} beralih menjumlahkan kanal uang muka
	 *       dan menonolkan kanal pembayaran pengadaan biasa serta saldo awal.</li>
	 * </ol>
	 * <p>Pada layar PO, dokumen bertanda ini juga kehilangan tombol Setujui,
	 * Batalkan, dan Ubah - karena statusnya sudah "disetujui" sejak lahir.</p>
	 *
	 * <h3>Gerbang persetujuan: bergeser ke hulu, bukan hilang</h3>
	 * <p>Persetujuan diri sendiri terdengar seperti celah, tetapi kendalinya
	 * berada di tempat lain. Bendera ini <b>tidak dapat dinyalakan dari formulir
	 * PO</b> - satu-satunya jalur adalah tombol "Beli Langsung", yang muncul di
	 * dua tempat dan keduanya menuntut prasyarat:</p>
	 * <ul>
	 *   <li><b>Layar PR</b> - tombol hanya tampil bila PR belum pernah ditarik ke
	 *       PO mana pun, <b>{@link PermintaanPengadaanMasterAsset#getDisetujuiOleh()}
	 *       tidak null</b>, pengguna memegang hak <code>edit</code>, dan tidak ada
	 *       satu pun baris PR yang terkait uang muka (agar biaya tidak dobel).</li>
	 *   <li><b>Dialog pemilih PR</b> - baris yang dapat dipilih sudah disaring
	 *       dengan <code>disetujuiOleh IS NOT NULL</code> dan
	 *       <code>aktif != false</code>.</li>
	 * </ul>
	 * <p>Jadi setiap PO pembelian langsung selalu berakar pada PR yang <b>sudah
	 * melewati gerbang persetujuan</b>. Ini berbeda dari pola yang pernah
	 * ditemukan di modul lain AIS, di mana dokumen hilir dapat berjalan atas
	 * dokumen hulu yang belum disetujui.</p>
	 * <p>Yang tetap perlu dicatat sebagai batasan yang <b>diketahui</b>: (a) tidak
	 * ada batas nilai - pembelian langsung bernilai berapa pun memperoleh
	 * perlakuan yang sama; (b) begitu bendera tersimpan, status "disetujui" pada
	 * PO menjadi konsekuensi otomatis tanpa pemeriksaan ulang terhadap PR sumber
	 * saat dibaca. Karena itu <b>setiap jalur baru</b> yang dapat memanggil
	 * {@link #setPembelianLangsung(Boolean)} wajib menegakkan sendiri syarat PR
	 * sudah disetujui - kelas ini tidak akan menegakkannya.</p>
	 *
	 * <p>Mengembalikan {@code false} untuk null.</p>
	 *
	 * @return {@code true} bila pesanan berupa pembelian langsung; tidak pernah
	 *         {@code null}
	 * @see #getDisetujuiOleh()
	 * @see #getTampaPermintaan()
	 */
	public Boolean getPembelianLangsung() {
		return pembelianLangsung == null ? false : pembelianLangsung;
	}

	/**
	 * Menyetel penanda pembelian langsung.
	 *
	 * <p><b>Peringatan keamanan</b>: menyalakan bendera ini membuat dokumen
	 * langsung berstatus disetujui oleh pembuatnya, tanpa langkah persetujuan
	 * terpisah dan tanpa batas nilai. Kelas ini tidak memeriksa apa pun sebelum
	 * menerimanya. Pemanggil <b>wajib</b> memastikan lebih dulu bahwa permintaan
	 * sumbernya sudah disetujui dan masih aktif - seperti yang dilakukan tombol
	 * "Beli Langsung" pada layar PR dan dialog pemilih PR. Lihat
	 * {@link #getPembelianLangsung()}.</p>
	 *
	 * @param pembelianLangsung {@code true} bila pesanan berupa pembelian langsung
	 */
	public void setPembelianLangsung(Boolean pembelianLangsung) {
		this.pembelianLangsung = pembelianLangsung;
	}

	/**
	 * Mengembalikan apakah dokumen masih berlaku.
	 *
	 * <h3>Urutan keputusan</h3>
	 * <ol>
	 *   <li>Bila disposisi ada dan bendera aktif disposisi bernilai false, dokumen
	 *       dimatikan.</li>
	 *   <li>Bila langkah akhir disposisi berada pada simpul alur yang ditandai
	 *       "penolakan ada di sini", dokumen dimatikan. Inilah cara penolakan di
	 *       mesin SOP merambat ke dokumen tanpa mengisi
	 *       {@link #getDitolakOleh()}.</li>
	 * </ol>
	 * <p>Nilai bawaannya {@code true} - dokumen dianggap berlaku sampai terbukti
	 * sebaliknya - sehingga kueri penyaring harus menerima
	 * <code>aktif IS NULL</code>.</p>
	 *
	 * <h3>Perbedaan dengan versi pada dokumen PR</h3>
	 * <p>{@link PermintaanPengadaanMasterAsset#getAktif()} memiliki satu aturan
	 * tambahan yang tidak ada di sini: ia menyalakan bendera bila dokumen sudah
	 * memiliki penyetuju. Aturan itu diperlukan di sana untuk mengimbangi mode
	 * persetujuan manual; PO tidak memiliki mode tersebut sehingga tidak
	 * membutuhkannya.</p>
	 * <p>Perbedaan kedua bersifat teknis: versi PR menulis
	 * <code>!Boolean.TRUE.equals(disposisiSop.getAktif())</code> yang aman terhadap
	 * null, sedangkan di sini ditulis <code>!disposisiSop.getAktif()</code> yang
	 * meng-<i>unbox</i> langsung. Bentuk ini aman <b>hanya karena</b>
	 * {@link DisposisiSop#getAktif()} sendiri menormalkan null menjadi
	 * {@code true}; ia akan menjadi sumber <code>NullPointerException</code> bila
	 * getter di sisi sana kelak diubah. Hal yang sama berlaku untuk
	 * <code>getPenolakanAdaDiSini()</code>. Bila method ini disunting, pertimbangkan
	 * menyelaraskannya dengan bentuk defensif versi PR.</p>
	 *
	 * <h3>Sifat destruktif</h3>
	 * <p>Hasil {@link #getDisposisiSop()} ditugaskan ke <b>field</b>
	 * {@code disposisiSop}, bukan ke variabel lokal baru - pola yang sama dengan
	 * versi PR. Bendera <code>aktif</code> juga ditulis. Keduanya membuat method
	 * ini destruktif.</p>
	 *
	 * @return {@code true} bila dokumen masih berlaku; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		disposisiSop = getDisposisiSop();
		if (disposisiSop != null && !disposisiSop.getAktif()) {
			aktif = false;
		}
		if (disposisiSop != null && disposisiSop.getDisposisiEnd() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
			aktif = false;
		}
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel penanda dokumen berlaku.
	 *
	 * <p>Nilai yang disetel akan dihitung ulang - dan berpotensi ditimpa menjadi
	 * {@code false} - pada pembacaan {@link #getAktif()} berikutnya bila dokumen
	 * terikat alur SOP. Perhatikan bahwa {@link #getAktif()} tidak pernah
	 * menyalakan kembali bendera yang sudah padam, sehingga penyetelan
	 * {@code true} di sini akan bertahan selama disposisinya sehat.</p>
	 *
	 * @param aktif {@code true} bila dokumen berlaku
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan jenis PPN yang dikenakan atas uang muka.
	 *
	 * <p>Menentukan persentase PPN yang dipakai {@link #getDptotal()} untuk
	 * menghitung nilai DP kotor. Bila pesanan bertermin ({@link #getByTermin()}),
	 * nilai ini <b>dipaksa {@code null}</b> - konsisten dengan {@link #getDp()}
	 * yang juga dinolkan, karena pajak pada skema termin diatur per tahap.</p>
	 * <p>Pemaksaan itu <b>ditulis balik ke field</b>, sehingga menyalakan bendera
	 * termin lalu membaca properti ini menghapus jenis pajak DP dari basis data
	 * secara permanen. Pola bendera satu arah yang sama dengan {@link #getDp()};
	 * membatalkan bendera tidak memulihkan nilainya.</p>
	 *
	 * <p>Nilai ini juga <b>menular ke baris</b>: pada PO pembelian langsung,
	 * {@link PemesananPengadaanMasterAssetDetail#getJenisPajakPpn()} mengambil
	 * alih jenis PPN dari properti ini, menimpa apa pun yang disetel per baris.</p>
	 *
	 * <p><b>Getter destruktif</b>: <code>check()</code> dipanggil lebih dulu untuk
	 * kedua cabang, lalu pemaksaan null diterapkan di atasnya.</p>
	 *
	 * @return jenis PPN atas uang muka, atau {@code null} bila tidak ada atau
	 *         pesanan bertermin
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pajak_ppn_dp", nullable = true)
	public JenisPajakPpn getJenisPajakPpnDp() {
		jenisPajakPpnDp = check(jenisPajakPpnDp);
		if (getByTermin()) {
			jenisPajakPpnDp = null;
		}
		return jenisPajakPpnDp;
	}

	/**
	 * Menyetel jenis PPN atas uang muka.
	 *
	 * <p><b>Peringatan</b>: nilai ini akan dihapus saat dibaca bila
	 * {@link #getByTermin()} menyala.</p>
	 *
	 * @param jenisPajakPpnDp jenis PPN atas uang muka
	 */
	public void setJenisPajakPpnDp(JenisPajakPpn jenisPajakPpnDp) {
		this.jenisPajakPpnDp = jenisPajakPpnDp;
	}

	/**
	 * Mengembalikan nilai uang muka <b>termasuk PPN</b>.
	 *
	 * <h3>Rumus</h3>
	 * <p><code>dptotal = dp + (persenPpn / 100) * dp</code>, dengan persentase
	 * diambil dari {@link #getJenisPajakPpnDp()} dan dianggap nol bila jenis pajak
	 * tidak ada. <b>Selalu dihitung ulang</b> dan ditulis balik ke field, sehingga
	 * kolom di basis data hanyalah cache - dan pembacaan pada entitas terkelola
	 * dapat memicu UPDATE.</p>
	 *
	 * <h3>Mengapa properti ini berpengaruh luas</h3>
	 * <p>Nilai ini dipakai sebagai <b>penanda skema pembayaran</b> di beberapa
	 * tempat lewat perbandingan <code>getDptotal() &gt; 0.1</code>:
	 * {@link #hitungDibayar(Session)} memakainya untuk memutuskan kanal mana yang
	 * dijumlahkan, dan {@link #getKodeInvoice()} memakainya untuk memutuskan
	 * apakah nomor invoice perlu dibangkitkan. Ambang 0,1 - alih-alih nol -
	 * dipakai karena hasil perkalian persentase pada tipe {@link Double} dapat
	 * menyisakan galat pembulatan yang sangat kecil.</p>
	 * <p>Pada PO pembelian langsung, nilai ini <b>menjadi</b> nilai pesanan:
	 * {@link #getNilai()} mengambil alih dari sini. Rantai pengaruhnya karena itu
	 * panjang - mengubah DP pada PO pembelian langsung mengubah nilai pesanan,
	 * yang mengubah penilaian {@link #getLunas()}.</p>
	 *
	 * <p>Karena bertumpu pada {@link #getDp()} dan {@link #getJenisPajakPpnDp()}
	 * yang keduanya dipaksa kosong pada pesanan bertermin, PO bertermin selalu
	 * menghasilkan {@code 0.0} di sini - itulah yang membuat cabang-cabang
	 * berambang 0,1 berperilaku benar tanpa perlu memeriksa bendera termin
	 * secara terpisah.</p>
	 *
	 * @return nilai DP termasuk PPN; tidak pernah {@code null}
	 */
	public Double getDptotal() {
		dptotal = getDp()
				+ (((getJenisPajakPpnDp() == null ? 0.0 : getJenisPajakPpnDp().getPersen()) / 100.0) * getDp());
		return dptotal;
	}

	/**
	 * Menyetel nilai uang muka termasuk PPN.
	 *
	 * <p><b>Praktis tidak berguna</b>: {@link #getDptotal()} selalu menghitung
	 * ulang. Setter tetap ada karena dibutuhkan Hibernate untuk memuat kolom.</p>
	 *
	 * @param dptotal nilai DP termasuk PPN (akan tertimpa saat dibaca)
	 */
	public void setDptotal(Double dptotal) {
		this.dptotal = dptotal;
	}

	/**
	 * Mengembalikan batas waktu pengiriman versi <b>kolom warisan</b>
	 * (<code>pengirimanpalinglambat</code>, tanpa pemisah).
	 *
	 * <p>Kolom ini digantikan oleh <code>pengiriman_paling_lambat</code> yang
	 * dipetakan {@link #getPengirimanPalingLambat()}. Ia dipertahankan semata agar
	 * dokumen lama tidak kehilangan datanya, dan dibaca <b>hanya</b> oleh getter
	 * baru itu sebagai sumber migrasi bertahap saat dokumen dibuka.</p>
	 *
	 * <p><b>Jangan memakai method ini di kode baru.</b> Migrasinya searah -
	 * penulisan lewat {@link #setPengirimanPalingLambat(Date)} tidak menyalin
	 * balik ke sini - sehingga setelah dokumen bermigrasi, nilai di sini menjadi
	 * basi dan dapat berbeda dari batas waktu yang sebenarnya berlaku.</p>
	 *
	 * @return batas waktu pengiriman menurut kolom warisan, atau {@code null}
	 * @see #getPengirimanPalingLambat()
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "pengirimanpalinglambat")
	public Date getPengirimanPalingLambatOld() {
		return pengirimanPalingLambatOld;
	}

	/**
	 * Menyetel batas waktu pengiriman pada kolom warisan.
	 *
	 * <p>Disediakan untuk Hibernate saat memuat baris lama. Kode aplikasi harus
	 * memakai {@link #setPengirimanPalingLambat(Date)}.</p>
	 *
	 * @param pengirimanPalingLambatOld batas waktu pengiriman versi warisan
	 */
	public void setPengirimanPalingLambatOld(Date pengirimanPalingLambatOld) {
		this.pengirimanPalingLambatOld = pengirimanPalingLambatOld;
	}

	/**
	 * Alasan penolakan PO -- diisi saat penolakan supaya pembuat tahu apa yang harus
	 * diperbaiki. Ditambahkan 2026-08-20 bersama pemakaian modul ini oleh POS
	 * (Desktop/Android/JSP); kolom NULLABLE sehingga baris lama tidak terdampak.
	 *
	 * <p>Pada jalur POS pengisian alasan diwajibkan minimal lima karakter sebelum
	 * penolakan diterima. Pada jalur ZK kewajiban itu tidak ditegakkan - tombol
	 * "Ditolak" hanya menyetel {@link #setDitolakOleh(Tbmuser)} dan
	 * {@link #setTanggalDitolak(Date)} - sehingga PO yang ditolak dari layar ZK
	 * dapat memiliki alasan kosong. Penting diketahui bila menulis laporan yang
	 * mengasumsikan alasan selalu terisi.</p>
	 *
	 * @return alasan penolakan, atau {@code null}
	 * @see #getDitolakOleh()
	 */
	@Column(name = "alasan_ditolak", nullable = true)
	public String getAlasanDitolak() {
		return alasanDitolak;
	}

	/**
	 * Menyetel alasan penolakan PO.
	 *
	 * <p>Tidak memvalidasi panjang minimum; validasi itu berada di
	 * <code>PengadaanPosApiHelper</code> untuk jalur POS.</p>
	 *
	 * @param alasanDitolak alasan penolakan
	 */
	public void setAlasanDitolak(String alasanDitolak) {
		this.alasanDitolak = alasanDitolak;
	}

	/**
	 * Sisa pesanan yang TIDAK akan dikirim lagi sudah ditutup (<i>short close</i>).
	 *
	 * <p>Dipakai saat barang datang kurang dan pemesan memutuskan tidak menunggu: sisanya
	 * dibatalkan di sini, lalu -- bila masih dibutuhkan -- diterbitkan pesanan susulan
	 * (<i>back order</i>) yang menunjuk dokumen ini lewat {@link #getPoInduk()}. Selama
	 * bernilai true, sisa baris pesanan ini tidak boleh diterima lagi dan tidak lagi dihitung
	 * sebagai "sudah dipesan" terhadap Permintaan Pembelian asalnya.</p>
	 *
	 * <p>Kolom NULLABLE; dokumen lama bernilai null dan diperlakukan sebagai belum ditutup.
	 * Ditambahkan 2026-08-21.</p>
	 *
	 * <p><b>Peringatan pemakaian - berbeda dari saudaranya.</b> Getter ini
	 * mengembalikan {@link Boolean} <b>mentah yang bisa {@code null}</b>,
	 * sedangkan {@link PermintaanPengadaanMasterAsset#getTutup()} menormalkan null
	 * menjadi {@code false}. Kode yang menulis <code>if (dokumen.getTutup())</code>
	 * aman untuk PR tetapi melempar <code>NullPointerException</code> untuk PO
	 * lama. Selalu pakai <code>Boolean.TRUE.equals(po.getTutup())</code> di sini.
	 * Jangan menyalin pola pemakaian dari satu kelas ke kelas lain tanpa
	 * memeriksa.</p>
	 *
	 * @return {@code true} bila sisa pesanan sudah ditutup, {@code false} bila
	 *         belum, atau {@code null} untuk dokumen lama
	 * @see #getAlasanTutup()
	 * @see #getPoInduk()
	 */
	@javax.persistence.Column(name = "tutup", nullable = true)
	public Boolean getTutup() {
		return tutup;
	}

	/**
	 * Menyetel penanda sisa pesanan ditutup.
	 *
	 * <p>Selalu setel bersamaan dengan {@link #setAlasanTutup(String)} agar
	 * keputusan membatalkan sisa pesanan tetap dapat ditelusuri.</p>
	 *
	 * @param tutup {@code true} untuk menutup sisa pesanan
	 */
	public void setTutup(Boolean tutup) {
		this.tutup = tutup;
	}

	/**
	 * Alasan sisa pesanan ditutup -- wajib diisi saat menutup, supaya keputusan membatalkan
	 * sisa pesanan selalu dapat ditelusuri. Kolom NULLABLE. Ditambahkan 2026-08-21.
	 *
	 * <p>Kewajiban pengisian ditegakkan di lapisan aksi, bukan di entitas ini -
	 * kolomnya sendiri mengizinkan null agar dokumen lama tidak terdampak. Panjang
	 * dibatasi 500 karakter.</p>
	 *
	 * @return alasan penutupan sisa pesanan, atau {@code null} bila belum ditutup
	 * @see #getTutup()
	 */
	@javax.persistence.Column(name = "alasan_tutup", nullable = true, length = 500)
	public String getAlasanTutup() {
		return alasanTutup;
	}

	/**
	 * Menyetel alasan sisa pesanan ditutup.
	 *
	 * <p>Tidak memvalidasi keterisian maupun panjang; keduanya ditegakkan lapisan
	 * aksi dan constraint kolom.</p>
	 *
	 * @param alasanTutup alasan penutupan
	 */
	public void setAlasanTutup(String alasanTutup) {
		this.alasanTutup = alasanTutup;
	}

	/**
	 * Pesanan asal bila dokumen ini adalah pesanan susulan (<i>back order</i>) atas kekurangan
	 * kiriman. Membentuk rantai yang dapat ditelusuri: PO asal -> PO susulan -> dan seterusnya.
	 * Kolom NULLABLE; pesanan biasa bernilai null. Ditambahkan 2026-08-21.
	 *
	 * <p>Relasi ini <b>menunjuk ke kelas yang sama</b> (rujukan diri), sehingga
	 * rantai susulan dapat berlapis tanpa batas. Perhatikan bahwa tidak ada
	 * penjagaan terhadap lingkaran - menyetel sebuah PO sebagai induk dirinya
	 * sendiri, atau membentuk siklus antar-PO, akan diterima entitas ini dan baru
	 * menimbulkan masalah saat rantainya ditelusuri. Pemanggil bertanggung jawab
	 * menjaga rantai tetap berbentuk pohon.</p>
	 *
	 * <p><b>Tidak memanggil <code>check()</code></b> meski dipetakan
	 * <code>fetch = LAZY</code>, sehingga berbeda dari getter relasi <i>lazy</i>
	 * lain di kelas ini yang semuanya me-<i>reattach</i>. Membaca properti ini
	 * pada entitas yang sudah lepas dari sesinya karena itu berisiko
	 * <code>LazyInitializationException</code>.</p>
	 *
	 * @return pesanan asal, atau {@code null} bila dokumen ini bukan pesanan
	 *         susulan
	 * @see #getTutup()
	 */
	@javax.persistence.ManyToOne(cascade = { javax.persistence.CascadeType.PERSIST,
			javax.persistence.CascadeType.MERGE }, fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "po_induk", nullable = true)
	public PemesananPengadaanMasterAsset getPoInduk() {
		return poInduk;
	}

	/**
	 * Menyetel pesanan asal bagi dokumen pesanan susulan ini.
	 *
	 * <p>Tidak memeriksa lingkaran rujukan; lihat {@link #getPoInduk()}.</p>
	 *
	 * @param poInduk pesanan asal
	 */
	public void setPoInduk(PemesananPengadaanMasterAsset poInduk) {
		this.poInduk = poInduk;
	}

}
