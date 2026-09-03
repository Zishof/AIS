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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;
import ais.database.model.inventory.Toko;

import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.Workspace;
import ais.database.model.sop.AlurSop;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiAlurSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * Header <b>Permintaan Pengadaan Barang/Jasa</b> (dokumen PR - <i>Purchase
 * Requisition</i>) untuk modul Aset Tetap. Entitas ini adalah dokumen
 * <b>tahap paling awal</b> dalam rantai pengadaan aset: unit kerja menyatakan
 * KEBUTUHAN-nya di sini, dokumen disetujui, lalu kebutuhan yang sudah disetujui
 * dipakai sebagai dasar penerbitan Pemesanan (PO) ke penyedia.
 *
 * <h3>Posisi dalam rantai pengadaan aset</h3>
 * <pre>
 *   PermintaanPengadaanMasterAsset  (PR - dokumen ini, "apa yang dibutuhkan")
 *        |
 *        +--&gt; PerjanjianKerjasamaMasterAsset   (opsional: kontrak payung)
 *        |
 *        v
 *   PemesananPengadaanMasterAsset   (PO - "dipesan ke penyedia siapa, harga berapa")
 *        v
 *   PenerimaanPengadaanMasterAsset  (BAST - "barang datang")
 *        v
 *   PembayaranPengadaanMasterAsset / PembayaranDpMasterAsset /
 *   PembayaranTerminMasterAsset     ("dibayar")
 * </pre>
 *
 * <h3>PENTING - relasi ke Pemesanan BENAR-BENAR memakai FK, bukan antrean kerja</h3>
 * <p>Pada paket <code>inventory</code> terdapat dokumen serupa
 * (<code>PengajuanPembelianGudang</code>) yang ternyata hanya berperan sebagai
 * <i>antrean kerja</i> tanpa satu pun kolom FK ke dokumen realisasinya. <b>Pola
 * itu TIDAK berlaku di sini.</b> Rantai PR-&gt;PO pada modul aset dijahit oleh
 * FK nyata pada <b>tiga</b> tempat sekaligus, ditambah satu jejak berbentuk CSV:</p>
 * <ol>
 *   <li><b>FK header-ke-header</b>: kolom <code>pemesanan_pengadaan_master_asset</code>
 *       pada tabel PR ini, lihat {@link #getPemesananPengadaanMasterAsset()}. Perhatikan
 *       arah kepemilikan kolomnya: yang menyimpan FK adalah baris PR, bukan baris PO.</li>
 *   <li><b>FK baris-ke-baris (arah PR-&gt;PO)</b>: kolom
 *       <code>pemesanan_pengadaan_master_asset_detail</code> pada
 *       {@link PermintaanPengadaanMasterAssetDetail}.</li>
 *   <li><b>FK baris-ke-baris (arah PO-&gt;PR)</b>: kolom
 *       <code>permintaan_pengadaan_master_asset_detail</code> pada
 *       {@link PemesananPengadaanMasterAssetDetail}. Kedua arah ini diisi
 *       BERSAMAAN oleh <code>PemesananPengadaanMasterAssetAction.onSave()</code>.</li>
 *   <li><b>Jejak CSV</b>: kolom teks
 *       <code>PemesananPengadaanMasterAsset.permintaanPengadaanMasterAssets</code>
 *       berisi daftar <i>id baris PR</i> dipisah koma. Ini BUKAN relasi terpetakan
 *       Hibernate melainkan denormalisasi untuk pelaporan/rekap anggaran; nilainya
 *       diisi ulang penuh setiap kali PO disimpan.</li>
 * </ol>
 * <p>Jadi jawaban atas pertanyaan "FK linear atau antrean kerja?" untuk modul aset
 * adalah: <b>FK linear, bahkan redundan berlapis</b>. Konsekuensinya, kerusakan
 * data lebih mungkin berupa <i>ketidaksinkronan antar-lapis</i> daripada kehilangan
 * jejak total seperti di modul gudang.</p>
 *
 * <h3>Batasan penting relasi ke Pemesanan: hanya SATU nilai (first-wins)</h3>
 * <p>{@link #getPemesananPengadaanMasterAsset()} bertipe tunggal, padahal secara
 * bisnis satu PR yang berisi banyak baris <b>boleh dipecah ke beberapa PO</b>
 * (pengguna memilih baris PR satu per satu lewat
 * <code>AmbilDataPermintaanPengadaanMasterAssetBanyak</code>). Kode penyimpanan PO
 * hanya menulis FK header ini <i>bila masih null</i>, sehingga kolom ini efektif
 * berarti "PO PERTAMA yang pernah menyentuh PR ini", bukan "PO yang memenuhi PR
 * ini". Jangan pakai kolom ini untuk menghitung realisasi PR; pakailah relasi pada
 * tingkat baris ({@link PermintaanPengadaanMasterAssetDetail}) atau kueri balik ke
 * {@link PemesananPengadaanMasterAssetDetail}.</p>
 *
 * <h3>Gerbang persetujuan</h3>
 * <p>Berbeda dengan temuan pada modul kepegawaian (SK belum disetujui yang tetap
 * bisa dipakai dokumen hilir), gerbang di sini <b>ADA dan aktif</b>. Pemilih PR
 * pada layar PO memfilter <code>Restrictions.isNotNull("disetujuiOleh")</code>
 * serta <code>aktif != false</code>, dan tombol "Beli Langsung" pada layar PR baru
 * muncul bila {@link #getDisetujuiOleh()} tidak null. PR yang belum disetujui atau
 * sudah ditolak tidak dapat ditarik menjadi PO lewat jalur UI normal.</p>
 * <p>Yang perlu diwaspadai bukan ketiadaan gerbang, melainkan <b>dua sumber
 * kebenaran</b> untuk status persetujuan: kolom fisik <code>disetujui_oleh</code>
 * (yang difilter oleh SQL) versus nilai turunan yang dihitung ulang oleh
 * {@link #getDisetujuiOleh()} dari {@link DisposisiSop}. Lihat penjelasan panjang
 * pada getter tersebut.</p>
 *
 * <h3>Catatan teknis lain</h3>
 * <ul>
 *   <li>Mewarisi {@link DataSop}, sehingga dokumen ini bisa dijalankan lewat mesin
 *       alur/disposisi SOP dan memperoleh helper <code>check()</code> untuk
 *       me-<i>reattach</i> proxy Hibernate yang sesinya sudah tertutup.</li>
 *   <li>Ber-anotasi {@link Audited} (Hibernate Envers): setiap perubahan
 *       menghasilkan baris revisi pada tabel bayangan <code>_aud</code>.</li>
 *   <li>Banyak getter di kelas ini bersifat <b>destruktif</b> - getter menulis
 *       kembali ke field instance (bukan sekadar membaca). Karena entitas berada
 *       dalam sesi Hibernate, penulisan itu ikut ter-<i>dirty check</i> dan bisa
 *       ter-flush ke basis data hanya karena dokumen dibaca. Ini pola arsitektur
 *       yang dipakai konsisten di seluruh AIS, bukan kecelakaan lokal; setiap
 *       getter semacam itu diberi catatan tersendiri di bawah.</li>
 * </ul>
 *
 * @see PermintaanPengadaanMasterAssetDetail baris item dari dokumen ini
 * @see PemesananPengadaanMasterAsset dokumen PO penerus rantai
 * @see DataSop kelas induk pembawa mesin alur SOP
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "asset", name = "permintaan_pengadaan_master_asset")
public class PermintaanPengadaanMasterAsset extends DataSop {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama (identity, dibangkitkan basis data). Lihat {@link #getId()}. */
	private Long id;
	/** Nomor urut tampilan yang tidak dipetakan ke kolom. Lihat {@link #getIndex()}. */
	private Long index;
	/** Nama pengguna terakhir yang menyentuh baris (audit bayangan). Lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang menyentuh baris (audit bayangan). Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan <b>id</b> pengguna yang terakhir menyentuh baris ini.
	 *
	 * <p>Bersama {@link #getOleh()} dan {@link #getTanggal_dirubah()}, field ini
	 * membentuk trio "audit bayangan" yang diisi otomatis oleh
	 * <code>AuditTimestampInterceptor</code>. Trio ini terlihat berlebihan karena
	 * entitas sudah ber-{@link Audited} (Envers), namun keberadaannya adalah
	 * <b>keharusan teknis</b>, bukan duplikasi yang bisa dibuang: tabel revisi
	 * Envers hanya efisien untuk penelusuran satu baris, sedangkan layar daftar
	 * dan laporan AIS perlu menampilkan "diubah oleh siapa, kapan" untuk ribuan
	 * baris sekaligus tanpa join ke tabel revisi.</p>
	 *
	 * @return id pengguna terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna terakhir yang menyentuh baris ini.
	 *
	 * <p><b>Perhatikan: setter ini menolak nilai kosong secara diam-diam.</b>
	 * Bila argumen {@code null} atau hanya berisi spasi, method langsung
	 * {@code return} tanpa mengubah apa pun, sehingga nilai lama dipertahankan.
	 * Perilaku ini disengaja: jejak audit tidak boleh terhapus oleh pemanggil yang
	 * kebetulan meneruskan nilai kosong (misalnya proses batch tanpa konteks
	 * pengguna). Akibat sampingannya, field ini <b>tidak dapat dikosongkan
	 * kembali</b> lewat setter; pengosongan hanya mungkin lewat SQL langsung.</p>
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
	 * Representasi teks dokumen, dipakai oleh komponen ZK (combobox, listbox,
	 * label ringkas) dan oleh log.
	 *
	 * <p>Mengembalikan field {@link #kode} secara <b>langsung</b>, bukan lewat
	 * {@link #getKode()}. Bedanya bermakna: {@link #getKode()} memangkas spasi dan
	 * mengubah string kosong menjadi {@code null}, sedangkan method ini akan
	 * mengembalikan apa adanya - termasuk string kosong. Untuk dokumen yang belum
	 * disimpan (kode belum dibangkitkan) hasilnya {@code null}, sehingga pemanggil
	 * yang merangkai string wajib berjaga terhadap teks "null".</p>
	 *
	 * @return kode dokumen apa adanya, bisa {@code null}
	 */
	public String toString() {
		return kode;
	}

	/**
	 * Menyetel nama pengguna terakhir yang menyentuh baris ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null} atau kosong
	 * diabaikan secara diam-diam agar jejak audit tidak terhapus.</p>
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
	 * @return nama pengguna terakhir, atau {@code null} bila belum pernah diisi
	 * @see #getOlehId()
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang dijalankan tepat sebelum baris di-UPDATE.
	 *
	 * <p>Mendelegasikan ke <code>AuditTimestampInterceptor.ubah(this)</code> yang
	 * mengisi {@link #setOleh(String)}, {@link #setOlehId(String)}, dan
	 * {@link #setTanggal_dirubah(Date)} dari konteks pengguna yang sedang aktif.
	 * Karena dipicu oleh Hibernate, method ini tidak boleh dipanggil manual.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Cap waktu perubahan terakhir. Diinisialisasi ke waktu server saat objek
	 * dibuat agar baris baru tidak pernah memiliki cap waktu null.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel cap waktu perubahan terakhir.
	 *
	 * <p>Berbeda dari {@link #setOleh(String)}, setter ini <b>menerima</b> nilai
	 * {@code null}. Umumnya dipanggil oleh <code>AuditTimestampInterceptor</code>,
	 * bukan oleh kode aksi.</p>
	 *
	 * @param tanggal_dirubah cap waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir baris ini.
	 *
	 * @return cap waktu perubahan terakhir; tidak pernah null untuk objek yang
	 *         dibuat lewat konstruktor kelas ini
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nomor dokumen PR, unik. Lihat {@link #getKode()}. */
	private String kode;
	/** Uraian bebas kebutuhan yang diminta. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Lingkup toko/tenant (dipakai modul POS). Lihat {@link #getToko()}. */
	private Toko toko;
	/** Mata anggaran pembebanan. Lihat {@link #getWorkspace()}. */
	private Workspace workspace;
	/** Penanda permintaan di luar anggaran. Lihat {@link #getTanpaAnggaran()}. */
	private Boolean tanpaAnggaran;
	/** Penanda pendanaan dari dana titipan. Lihat {@link #getDanaTitipan()}. */
	private Boolean danaTitipan;
	/** Entitas pemilik aset yang akan dituju. Lihat {@link #getPemilikAsset()}. */
	private PemilikAsset pemilikAsset;
	/** PO PERTAMA yang menyentuh PR ini (first-wins). Lihat {@link #getPemesananPengadaanMasterAsset()}. */
	private PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset;
	/** Lokasi penempatan aset yang diminta. Lihat {@link #getLokasi()}. */
	private Lokasi lokasi;
	/** Ruang penempatan aset yang diminta. Lihat {@link #getRuang()}. */
	private Ruang ruang;
	/** Tanggal dokumen dibuat. Lihat {@link #getTanggalPembuatan()}. */
	private Date tanggalPembuatan;
	/** Tanggal dokumen disetujui. Lihat {@link #getTanggalPersetujuan()}. */
	private Date tanggalPersetujuan;
	/** Tanggal dokumen ditolak. Lihat {@link #getTanggalDitolak()}. */
	private Date tanggalDitolak;
	/** Pembuat dokumen. Lihat {@link #getDibuatOleh()}. */
	private Tbmuser dibuatOleh;
	/** Penyetuju dokumen. Lihat {@link #getDisetujuiOleh()}. */
	private Tbmuser disetujuiOleh;
	/** Penolak dokumen. Lihat {@link #getDitolakOleh()}. */
	private Tbmuser ditolakOleh;
	/** Instans alur SOP yang menjalankan dokumen ini. Lihat {@link #getDisposisiSop()}. */
	private DisposisiSop disposisiSop;
	/** Unit kerja pemilik dokumen (dasar filter tenant). Lihat {@link #getSatuanKerja()}. */
	private SatuanKerja satuanKerja;
	/** Sisa pagu anggaran saat dokumen dibuat. Lihat {@link #getSaldo()}. */
	private Double saldo;
	/** Nilai total permintaan. Lihat {@link #getNilai()}. */
	private Double nilai;
	/** Tahun periode dokumen. Lihat {@link #getTahun()}. */
	private Integer tahun;
	/** Bulan periode dokumen (1-12). Lihat {@link #getBulan()}. */
	private Integer bulan;
	/** Template penomoran surat. Lihat {@link #getNomorSuratAlurPengadaan()}. */
	private NomorSuratAlurPengadaan nomorSuratAlurPengadaan;
	/** Penanda wajib melalui kontrak payung. Lihat {@link #getWajibAdaPerjanjianKerjasama()}. */
	private Boolean wajibAdaPerjanjianKerjasama;
	/** Penanda PR ditutup manual. Lihat {@link #getTutup()}. */
	private Boolean tutup;
	/** Akun pembebanan akuntansi. Lihat {@link #getAkun()}. */
	private Akun akun;
	/** Penanda dokumen masih berlaku. Lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** Penanda persetujuan manual (memutus derivasi dari SOP). Lihat {@link #getSetujuiManual()}. */
	private Boolean setujuiManual;
	/** Alasan penolakan. Lihat {@link #getAlasanDitolak()}. */
	private String alasanDitolak;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate untuk instansiasi
	 * lewat refleksi. Juga dipakai kode aksi untuk membuat dokumen PR baru; seluruh
	 * field dibiarkan null dan getter-getter berpelindung akan memasok nilai
	 * bawaan saat pertama dibaca.
	 */
	public PermintaanPengadaanMasterAsset() {
	}

	/**
	 * Konstruktor pintasan untuk membuat <i>referensi</i> ke baris yang sudah ada
	 * tanpa memuatnya dari basis data.
	 *
	 * <p>Berguna untuk merangkai kriteria Hibernate atau menyetel relasi ketika
	 * hanya id yang diketahui. Objek hasil konstruktor ini bersifat <b>detached
	 * dan tidak lengkap</b>: seluruh field selain {@link #id} bernilai null,
	 * sehingga jangan pernah menyimpannya lewat <code>session.update()</code>
	 * karena akan menimpa kolom-kolom nyata dengan null.</p>
	 *
	 * @param id kunci utama baris yang dirujuk
	 */
	public PermintaanPengadaanMasterAsset(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kunci utama dokumen.
	 *
	 * <p>Kolom bertanda <code>insertable = false</code> karena nilainya
	 * dibangkitkan oleh basis data (strategi IDENTITY). Nilai {@code null}
	 * menandakan dokumen belum pernah disimpan - beberapa getter lain
	 * (misalnya {@link #getKodeUnik()}) berperilaku berbeda pada kondisi
	 * tersebut.</p>
	 *
	 * @return kunci utama, atau {@code null} bila dokumen belum tersimpan
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
	 * Mengembalikan nomor dokumen PR, ternormalisasi.
	 *
	 * <p>Getter ini <b>menormalkan tanpa menulis balik</b>: spasi di ujung
	 * dipangkas dan string kosong dipetakan menjadi {@code null}, tetapi field
	 * {@link #kode} sendiri tidak diubah. Karena itu {@link #toString()} - yang
	 * membaca field mentah - bisa mengembalikan string berisi spasi sementara
	 * getter ini mengembalikan nilai bersih. Perbedaan ini penting saat
	 * membandingkan kode dengan hasil kueri.</p>
	 *
	 * <p>Kolom ditandai <code>unique</code> pada tingkat basis data, sehingga
	 * penomoran ganda akan ditolak sebagai pelanggaran constraint, bukan diterima
	 * diam-diam. Nomor dibangkitkan oleh kode aksi (pola
	 * <code>PR/&lt;toko&gt;/&lt;yyyyMM&gt;/&lt;urut&gt;</code> pada jalur POS)
	 * saat dokumen pertama kali disimpan.</p>
	 *
	 * @return nomor dokumen tanpa spasi ujung, atau {@code null} bila kosong
	 */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return this.kode == null || kode.trim().isEmpty() ? null : this.kode.trim();
	}

	/**
	 * Menyetel nomor dokumen PR.
	 *
	 * <p>Tidak melakukan normalisasi maupun validasi keunikan; keduanya menjadi
	 * tanggung jawab pemanggil dan constraint basis data.</p>
	 *
	 * @param kode nomor dokumen
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan uraian bebas kebutuhan yang diminta.
	 *
	 * <p>Dipetakan sebagai kolom <code>text</code> sehingga tidak berbatas panjang
	 * praktis. Kode aksi mewajibkan field ini terisi sebelum dokumen boleh
	 * disimpan, namun kewajiban itu ditegakkan di lapisan UI - basis data
	 * mengizinkan null.</p>
	 *
	 * @return uraian permintaan, bisa {@code null}
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel uraian bebas kebutuhan yang diminta.
	 *
	 * @param keterangan uraian permintaan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan toko (tenant ritel) pemilik dokumen.
	 *
	 * <p>Kolom ini ditambahkan pada r77725 ketika modul PR dipakai bersama oleh
	 * antarmuka JSP/ZKoss dan oleh modul POS: keputusan produknya adalah POS
	 * <b>tidak</b> memakai tabel sendiri melainkan tabel pengadaan yang sama,
	 * dan dibedakan lewat kolom ini. Untuk dokumen yang dibuat dari antarmuka
	 * non-POS nilainya {@code null}.</p>
	 *
	 * <p><b>Getter destruktif</b>: memanggil <code>check()</code> milik
	 * {@link DataSop} yang me-<i>reattach</i> proxy Hibernate ke sesi aktif bila
	 * sesi asalnya sudah tertutup, lalu menulis hasilnya kembali ke field. Efek
	 * sampingnya, membaca properti ini dapat memicu SELECT tambahan.</p>
	 *
	 * <p><b>Catatan pemisahan tenant</b>: kolom ini <i>tidak</i> dipakai sebagai
	 * filter oleh pemilih PR di layar PO (<code>AmbilDataPermintaan...Banyak</code>)
	 * yang menyaring berdasarkan {@link #getSatuanKerja()} saja. Pemisahan lingkup
	 * toko karena itu hanya ditegakkan pada jalur POS, bukan pada jalur ZK.</p>
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
	 * <p>Nilai yang disetel di sini dapat <b>ditimpa saat dibaca</b> oleh
	 * {@link #getDibuatOleh()} bila dokumen berjalan di atas alur SOP. Lihat
	 * penjelasan pada getter tersebut.</p>
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
	 * <ol>
	 *   <li>Field {@link #dibuatOleh} di-<i>reattach</i> lewat <code>check()</code>
	 *       agar proxy yang sesinya sudah tertutup tidak melempar
	 *       <code>LazyInitializationException</code>.</li>
	 *   <li>Bila dokumen terikat ke {@link DisposisiSop} dan disposisi itu memiliki
	 *       langkah awal (<code>getDisposisiStart()</code>) dengan pengaju yang
	 *       diketahui, maka <b>pengaju langkah awal itulah</b> yang dianggap
	 *       pembuat dokumen - menimpa apa pun yang tersimpan di kolom
	 *       <code>dibuat_oleh</code>.</li>
	 * </ol>
	 *
	 * <h3>Mengapa alur SOP menang atas kolom</h3>
	 * <p>Kolom <code>dibuat_oleh</code> diisi oleh layar yang menyimpan dokumen,
	 * yang belum tentu sama dengan orang yang <i>mengajukan</i> dokumen ke alur
	 * persetujuan. Contoh nyata: staf tata usaha mengetikkan permintaan atas nama
	 * kepala unit, lalu kepala unit sendiri yang mengajukannya ke alur. Yang
	 * bertanggung jawab secara administratif adalah pengaju, sehingga cetakan
	 * dokumen dan dasbor persetujuan harus menampilkan pengaju. Derivasi ini
	 * membuat kolom fisik menjadi sekadar nilai cadangan untuk dokumen yang tidak
	 * memakai alur SOP sama sekali.</p>
	 *
	 * <h3>Konsekuensi yang perlu diwaspadai</h3>
	 * <p>Pertama, ini adalah <b>getter destruktif</b>: hasil derivasi ditulis
	 * kembali ke field {@link #dibuatOleh}. Bila entitas sedang dikelola sebuah
	 * sesi Hibernate, penulisan itu terdeteksi sebagai perubahan dan dapat
	 * ter-flush ke kolom <code>dibuat_oleh</code> hanya karena dokumen ditampilkan.
	 * Efek ini sebetulnya <i>menguntungkan</i> - kolom perlahan menyelaraskan diri
	 * dengan alur SOP - tetapi berarti operasi baca murni tetap dapat menghasilkan
	 * UPDATE, sesuatu yang harus diperhitungkan saat menyetel
	 * <code>FlushMode</code> pada kalkulasi berat (bandingkan dengan
	 * <code>PemesananPengadaanMasterAsset.hitungDibayar(Session)</code> yang
	 * sengaja mematikan auto-flush karena alasan ini).</p>
	 * <p>Kedua, method ini memanggil {@link #getDisposisiSop()} sebanyak tiga kali
	 * dalam satu rangkaian kondisi. Kelas ini sudah pernah diperbaiki di
	 * {@link #getDisetujuiOleh()} dan {@link #getTanggalPersetujuan()} karena pola
	 * pemanggilan berulang atas getter <i>lazy</i> semacam ini dapat memberi hasil
	 * berbeda antara saat null-check dan saat dereference, sehingga memicu
	 * <code>NullPointerException</code>. Di method ini pola berulang tersebut
	 * <b>masih ada</b>; risikonya lebih rendah karena rantai null-check dan
	 * dereference berada dalam satu ekspresi <code>&amp;&amp;</code> yang
	 * dievaluasi berurutan, namun bila kelak method ini disentuh, terapkan pola
	 * yang sama: ambil sekali ke variabel lokal.</p>
	 *
	 * @return pengaju langkah awal alur SOP bila ada, jika tidak nilai kolom
	 *         <code>dibuat_oleh</code>; bisa {@code null} untuk dokumen yang belum
	 *         lengkap
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
	 * <p>Dipanggil dari dua tempat yang bermakna berbeda:</p>
	 * <ul>
	 *   <li>Tombol "Persetujuan" pada layar PR, yang menyetel pengguna aktif
	 *       <b>bersamaan dengan</b> {@link #setSetujuiManual(Boolean)} bernilai
	 *       {@code true} - sejak titik itu nilai persetujuan tidak lagi diturunkan
	 *       dari alur SOP.</li>
	 *   <li>Tombol "Batalkan", yang menyetel {@code null} dan mengembalikan
	 *       {@code setujuiManual = false}.</li>
	 * </ul>
	 *
	 * <p>Bila {@link #getSetujuiManual()} bernilai false, nilai yang disetel di
	 * sini akan <b>ditimpa saat dibaca</b> oleh {@link #getDisetujuiOleh()}.</p>
	 *
	 * @param disetujuiOleh pengguna penyetuju, atau {@code null} untuk membatalkan
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Mengembalikan pengguna yang menyetujui dokumen ini - <b>inti gerbang
	 * persetujuan PR-&gt;PO</b>.
	 *
	 * <h3>Mengapa method ini menentukan</h3>
	 * <p>Nilai balik method ini (bukan kolom <code>disetujui_oleh</code> secara
	 * langsung) adalah syarat yang dipakai di seluruh lapisan aksi untuk menjawab
	 * "apakah PR ini boleh dilanjutkan?": tombol "Beli Langsung" pada layar PR
	 * hanya muncul bila hasilnya bukan null, tombol Ubah/Hapus justru hanya muncul
	 * bila hasilnya null, dan seluruh dasbor pengadaan mengelompokkan dokumen
	 * dengan aturan yang sama. Karena itu logika di bawah bukan sekadar pembacaan
	 * field.</p>
	 *
	 * <h3>Dua mode: manual versus turunan alur SOP</h3>
	 * <p>Percabangan paling luar adalah <code>if (!getSetujuiManual())</code>.
	 * Artinya:</p>
	 * <ul>
	 *   <li><b>Mode manual</b> ({@code setujuiManual == true}) - seluruh blok
	 *       dilewati dan nilai kolom dikembalikan apa adanya. Mode ini diaktifkan
	 *       oleh tombol "Persetujuan" pada layar PR. Sejak saat itu dokumen
	 *       <b>tidak lagi ikut berubah</b> mengikuti alur SOP: seandainya instans
	 *       disposisi kemudian ditolak lewat modul SOP, method ini tetap
	 *       mengembalikan penyetuju manual tadi. Pertahanan berlapis yang menutup
	 *       celah ini adalah {@link #getAktif()}, yang tetap menghitung ulang dari
	 *       disposisi dan memberi {@code false} bila alur berakhir di simpul
	 *       penolakan - dan pemilih PR di layar PO <i>juga</i> memfilter
	 *       <code>aktif</code>. Jadi dokumen semacam itu tetap tersaring, namun
	 *       lewat kolom yang berbeda. Bila kelak filter <code>aktif</code> dilepas
	 *       dari pemilih, celah ini menjadi nyata.</li>
	 *   <li><b>Mode turunan</b> ({@code setujuiManual == false}, nilai bawaan) -
	 *       persetujuan dihitung ulang dari {@link DisposisiSop}: bila langkah
	 *       "setuju" ada dan memiliki pengaju, pengaju itulah penyetujunya. Bila
	 *       disposisi ada tetapi langkah setuju belum ada (atau ada tanpa pengaju),
	 *       hasilnya dipaksa {@code null}. Bila {@link #getDitolakOleh()} terisi,
	 *       hasilnya juga dipaksa {@code null} - penolakan selalu menang atas
	 *       persetujuan.</li>
	 * </ul>
	 *
	 * <h3>Perbaikan akar masalah yang sudah tertanam</h3>
	 * <p>Komentar di dalam badan method mendokumentasikan perbaikan NPE: semula
	 * {@link #getDisposisiSop()} dan <code>getDisposisiSetuju()</code> dipanggil
	 * berulang (empat kali) dalam ekspresi boolean yang berbeda. Karena keduanya
	 * memuat data secara <i>lazy</i> lewat <code>check()</code> - yang dapat
	 * membuka dan menutup sesi Hibernate sendiri - dua pemanggilan berturut-turut
	 * bisa memberi hasil berbeda: null-check lolos pada pemanggilan pertama,
	 * dereference gagal pada pemanggilan kedua. Perbaikannya adalah mengambil nilai
	 * sekali ke variabel lokal {@code ds} dan {@code setuju}, lalu memeriksa
	 * variabel itu secara konsisten. <b>Jangan mengembalikan pola pemanggilan
	 * berulang saat menyunting method ini.</b> Perhatikan bahwa
	 * {@link PemesananPengadaanMasterAsset#getDisetujuiOleh()} - saudara dokumen
	 * ini - belum menerima perbaikan yang sama dan masih memanggil berulang.</p>
	 *
	 * <h3>Sifat destruktif dan dua sumber kebenaran</h3>
	 * <p>Method ini menulis hasil derivasi kembali ke field {@link #disetujuiOleh}.
	 * Konsekuensinya kolom fisik <code>disetujui_oleh</code> perlahan menyelaraskan
	 * diri dengan alur SOP - tetapi <b>hanya ketika dokumen dibaca dalam sesi yang
	 * kemudian ter-flush</b>. Selama belum ada yang membacanya, kolom bisa
	 * menyimpan nilai basi. Ini penting karena gerbang di pemilih PR pada layar PO
	 * berupa <code>Restrictions.isNotNull("disetujuiOleh")</code>, yaitu kueri SQL
	 * atas <b>kolom</b>, bukan atas nilai turunan method ini. Secara teori sebuah
	 * PR yang persetujuannya sudah dicabut di alur SOP namun kolomnya belum
	 * terselaraskan masih dapat muncul di pemilih. Dalam praktik dokumen selalu
	 * dibaca lebih dulu (dasbor, cetakan, layar detail) sehingga penyelarasan
	 * terjadi, dan filter <code>aktif</code> menutup sisanya. Tetap catat ini
	 * sebagai utang desain: <i>satu status persetujuan disimpan di dua tempat
	 * dengan aturan sinkronisasi implisit.</i></p>
	 *
	 * @return pengguna penyetuju, atau {@code null} bila dokumen belum disetujui,
	 *         sudah ditolak, atau alur SOP belum mencapai langkah setuju
	 * @see #getSetujuiManual()
	 * @see #getAktif()
	 * @see #getTanggalPersetujuan()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		if (!getSetujuiManual()) {
			/*
			 * ROOT CAUSE FIX: getDisposisiSop() dan getDisposisiSop().getDisposisiSetuju()
			 * sebelumnya dipanggil berulang kali (4x) di dalam ekspresi boolean yang
			 * berbeda. Karena keduanya lazy-load via check() (bisa membuka/menutup
			 * session sendiri), dua pemanggilan berturut-turut bisa memberi hasil
			 * berbeda antara "null-check lolos" dan "dereference" -> NPE. Ambil sekali
			 * ke variabel lokal, lalu null-check variabel tsb secara konsisten.
			 */
			DisposisiSop ds = getDisposisiSop();
			DisposisiAlurSop setuju = ds == null ? null : ds.getDisposisiSetuju();

			if (getDitolakOleh() != null) {
				disetujuiOleh = null;
			} else {
				disetujuiOleh = check(disetujuiOleh);

				if (setuju != null && setuju.getDiajukanOleh() != null) {
					disetujuiOleh = setuju.getDiajukanOleh();
				}
			}

			if (ds != null && (setuju == null || setuju.getDiajukanOleh() == null)) {
				disetujuiOleh = null;
			}
		}
		return disetujuiOleh;
	}

	/**
	 * Menyetel tanggal persetujuan dokumen.
	 *
	 * <p>Seperti {@link #setDisetujuiOleh(Tbmuser)}, nilai ini akan ditimpa saat
	 * dibaca bila {@link #getSetujuiManual()} bernilai false.</p>
	 *
	 * @param tanggalPersetujuan cap waktu persetujuan, atau {@code null}
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengembalikan cap waktu persetujuan dokumen, dengan aturan derivasi yang
	 * <b>persis mencerminkan</b> {@link #getDisetujuiOleh()}.
	 *
	 * <h3>Cara kerja</h3>
	 * <p>Bila {@link #getSetujuiManual()} true, nilai kolom dikembalikan apa
	 * adanya. Bila false, cap waktu diturunkan dari alur SOP dengan urutan
	 * keputusan berikut:</p>
	 * <ol>
	 *   <li>Bila ada penolak ({@link #getDitolakOleh()} tidak null), cap waktu
	 *       dipaksa {@code null}.</li>
	 *   <li>Selain itu, bila langkah "setuju" pada disposisi ada dan memiliki
	 *       pengaju, cap waktu diambil dari waktu langkah tersebut
	 *       (<code>setuju.getWaktu()</code>) - yaitu kapan persetujuan benar-benar
	 *       diberikan di alur, bukan kapan kolom ditulis.</li>
	 *   <li>Terakhir, bila disposisi ada tetapi langkah setuju belum ada atau tanpa
	 *       pengaju, cap waktu dipaksa {@code null}.</li>
	 * </ol>
	 *
	 * <h3>Mengapa kedua getter harus dijaga bersama</h3>
	 * <p>Layar dan cetakan menampilkan pasangan "disetujui oleh X pada tanggal Y".
	 * Bila hanya satu dari keduanya diubah, dokumen akan mencetak nama penyetuju
	 * tanpa tanggal atau sebaliknya - cacat yang sulit terlihat di pengujian namun
	 * langsung terlihat pada dokumen resmi. Karena itu setiap perubahan pada salah
	 * satu method ini <b>wajib</b> dicerminkan ke pasangannya. Perhatikan bahwa
	 * urutan kondisinya sengaja dibuat identik, termasuk urutan pemaksaan
	 * {@code null} di akhir.</p>
	 *
	 * <h3>Perbaikan akar masalah yang sudah tertanam</h3>
	 * <p>Sama seperti {@link #getDisetujuiOleh()}, {@link #getDisposisiSop()} dan
	 * <code>getDisposisiSetuju()</code> diambil <b>sekali</b> ke variabel lokal
	 * {@code ds} dan {@code setuju}, bukan dipanggil ulang di setiap ekspresi.
	 * Pemanggilan berulang atas getter yang memuat data secara <i>lazy</i> pernah
	 * menyebabkan <code>NullPointerException</code> karena hasil pemanggilan
	 * pertama dan kedua bisa berbeda. Pertahankan pola ini.</p>
	 *
	 * <h3>Sifat destruktif</h3>
	 * <p>Hasil derivasi ditulis kembali ke field {@link #tanggalPersetujuan},
	 * sehingga membaca properti ini pada entitas terkelola dapat memicu UPDATE
	 * pada kolom <code>tanggal_persetujuan</code>. Bandingkan dengan
	 * {@link #getTanggalPembuatan()} yang juga destruktif namun tidak pernah
	 * memaksa {@code null}.</p>
	 *
	 * @return cap waktu persetujuan, atau {@code null} bila belum disetujui atau
	 *         sudah ditolak
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {

		if (!getSetujuiManual()) {
			// ROOT CAUSE FIX: sama seperti getDisetujuiOleh() -- ambil sekali ke variabel
			// lokal agar tidak memanggil ulang getDisposisiSop()/getDisposisiSetuju()
			// yang lazy-load, mencegah inkonsistensi antar-pemanggilan yang memicu NPE.
			DisposisiSop ds = getDisposisiSop();
			DisposisiAlurSop setuju = ds == null ? null : ds.getDisposisiSetuju();

			if (getDitolakOleh() != null) {
				tanggalPersetujuan = null;
			} else if (setuju != null && setuju.getDiajukanOleh() != null) {
				tanggalPersetujuan = setuju.getWaktu();
			}

			if (ds != null && (setuju == null || setuju.getDiajukanOleh() == null)) {
				tanggalPersetujuan = null;
			}
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
	 * pengaju, cap waktu diambil dari waktu langkah awal tersebut - konsisten
	 * dengan {@link #getDibuatOleh()} yang juga mengambil pembuat dari langkah
	 * awal. Nilai kolom diperlakukan sebagai cadangan.</p>
	 *
	 * <p><b>Tidak pernah mengembalikan null.</b> Bila hasil akhirnya kosong,
	 * method memberi waktu server saat ini lewat {@link WaktuUtil#getDate()}.
	 * Perilaku ini menghindarkan NPE di layar daftar dan cetakan, tetapi punya
	 * konsekuensi halus: dokumen yang belum tersimpan akan menampilkan tanggal
	 * yang <b>berubah setiap kali dibaca</b>, sehingga jangan mengandalkan nilai
	 * ini untuk perbandingan sebelum dokumen disimpan.</p>
	 *
	 * <p><b>Getter destruktif</b>: nilai turunan ditulis kembali ke field. Namun
	 * perhatikan bahwa nilai cadangan "waktu sekarang" <i>tidak</i> ditulis ke
	 * field - ia hanya muncul pada nilai balik - sehingga pembacaan berulang tidak
	 * mengotori entitas dengan cap waktu palsu.</p>
	 *
	 * @return cap waktu pembuatan; tidak pernah {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
				&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
			tanggalPembuatan = getDisposisiSop().getDisposisiStart().getWaktu();
		}

		return tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan;
	}

	/**
	 * Mengembalikan entitas pemilik aset yang dituju permintaan ini.
	 *
	 * <p>Dipakai untuk menentukan badan hukum/yayasan mana yang akan mencatat aset
	 * hasil pengadaan. Nilai ini diwariskan ke PO saat tombol "Beli Langsung"
	 * menyalin data PR ke dokumen pemesanan.</p>
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
	 * Menyetel entitas pemilik aset yang dituju permintaan ini.
	 *
	 * @param pemilikAsset pemilik aset
	 */
	public void setPemilikAsset(PemilikAsset pemilikAsset) {
		this.pemilikAsset = pemilikAsset;
	}

	/**
	 * Mengembalikan lokasi (gedung/kampus) tempat aset akan ditempatkan.
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
	 * Menyetel lokasi tempat aset akan ditempatkan.
	 *
	 * @param lokasi lokasi tujuan
	 */
	public void setLokasi(Lokasi lokasi) {
		this.lokasi = lokasi;
	}

	/**
	 * Mengembalikan ruang (unit terkecil penempatan) tujuan aset.
	 *
	 * <p><b>Getter destruktif</b> karena memanggil <code>check()</code>. Perhatikan
	 * bahwa method ini mengembalikan <code>this.ruang</code> secara eksplisit
	 * sementara getter sejenis mengembalikan variabel field tanpa
	 * <code>this</code>; keduanya setara dan perbedaannya semata gaya penulisan.</p>
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
	 * <p><b>Bukan kolom basis data.</b> Properti ini tidak beranotasi pemetaan,
	 * namun karena Hibernate memakai akses berbasis properti, ia otomatis dipetakan
	 * kecuali diabaikan. Nilainya diisi oleh kode aksi saat merender daftar dan
	 * tidak memiliki makna bisnis; jangan dipakai sebagai pengenal.</p>
	 *
	 * @return nomor urut tampilan, atau {@code null} bila tidak sedang dirender
	 */
	public Long getIndex() {
		return index;
	}

	/**
	 * Mengembalikan mata anggaran (workspace) yang membebani permintaan ini.
	 *
	 * <h3>Bendera satu arah yang memaksa null</h3>
	 * <p>Bila {@link #getTanpaAnggaran()} bernilai true, method ini <b>memaksa
	 * hasilnya menjadi {@code null}</b> - dan karena getter ini destruktif,
	 * pemaksaan itu ditulis kembali ke field sehingga kolom
	 * <code>workspace</code> ikut dikosongkan pada flush berikutnya. Ini pola
	 * "bendera aktif satu arah" yang berulang di AIS: sekali dokumen ditandai
	 * tanpa anggaran, kaitan anggarannya <b>hilang permanen</b>; mengembalikan
	 * bendera ke false tidak memulihkan workspace yang sudah terhapus. Kode aksi
	 * yang mengganti bendera ini harus menyetel ulang workspace secara eksplisit.</p>
	 *
	 * <h3>Efek berantai</h3>
	 * <p>Nilai ini menjadi sumber bagi dua properti turunan lain:
	 * {@link #getSatuanKerja()} mengambil satuan kerja dari workspace, dan
	 * {@link #getAkun()} mengambil akun pembebanan dari workspace. Karena itu
	 * mengosongkan workspace ikut melepas dokumen dari filter tenant berbasis
	 * satuan kerja - dokumen tanpa anggaran akan bergantung sepenuhnya pada nilai
	 * satuan kerja yang tersimpan di kolomnya sendiri.</p>
	 *
	 * <p><b>Getter destruktif</b> pada kedua cabang: cabang tanpa-anggaran menulis
	 * {@code null}, cabang normal menulis hasil <code>check()</code>.</p>
	 *
	 * @return mata anggaran pembebanan, atau {@code null} bila dokumen ditandai
	 *         tanpa anggaran
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
	 * {@link #getTanpaAnggaran()} bernilai true.</p>
	 *
	 * @param workspace mata anggaran
	 */
	public void setWorkspace(Workspace workspace) {
		this.workspace = workspace;
	}

	/** Kode gabungan untuk keunikan lintas revisi SOP. Lihat {@link #getKodeUnik()}. */
	private String kodeUnik;

	/**
	 * Mengembalikan kode gabungan yang menjamin keunikan dokumen lintas revisi
	 * alur SOP.
	 *
	 * <h3>Mengapa kolom ini ada</h3>
	 * <p>Kolom {@link #getKode()} sudah bertanda <code>unique</code>, tetapi mesin
	 * SOP memperbolehkan satu nomor dokumen memiliki beberapa <i>instans alur</i>
	 * (misalnya dokumen ditolak lalu diajukan ulang). Bila hanya mengandalkan
	 * <code>kode</code>, pengajuan ulang akan bertabrakan dengan constraint. Kode
	 * unik menggabungkan nomor dokumen dengan pengenal instans alur sehingga tiap
	 * pengajuan menempati ruang nama sendiri.</p>
	 *
	 * <h3>Cara pembentukan</h3>
	 * <p>Formatnya <code>&lt;kode&gt;_&lt;id disposisi&gt;</code> bila dokumen
	 * terikat alur SOP, atau <code>&lt;kode&gt;_&lt;id dokumen&gt;</code> bila
	 * tidak. Perhatikan bahwa nilai ini <b>selalu dihitung ulang</b> setiap kali
	 * dibaca dan langsung ditulis ke field - jadi kolom di basis data hanyalah
	 * cache dari perhitungan ini, bukan sumber kebenaran.</p>
	 *
	 * <h3>Jebakan yang perlu diketahui</h3>
	 * <p>Untuk dokumen yang belum disimpan dan belum berdisposisi, {@link #getId()}
	 * bernilai null sehingga hasilnya berupa string berakhiran
	 * <code>"_null"</code>. Karena kolom bertanda <code>unique</code>, dua dokumen
	 * baru yang berkode sama akan menghasilkan kode unik yang sama pula dan saling
	 * bertabrakan pada INSERT. Dalam praktik hal ini tidak terjadi karena
	 * {@link #getKode()} sendiri dibangkitkan berurutan sebelum penyimpanan.</p>
	 *
	 * @return kode unik gabungan; tidak pernah null selama {@link #getKode()}
	 *         terisi
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
	 * ulang nilainya, sehingga apa pun yang disetel di sini akan tertimpa pada
	 * pembacaan berikutnya. Setter tetap disediakan karena Hibernate
	 * membutuhkannya untuk memuat kolom.</p>
	 *
	 * @param kodeUnik kode unik (akan tertimpa saat dibaca)
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * Mengembalikan instans alur SOP (disposisi) yang menjalankan dokumen ini.
	 *
	 * <p>Objek inilah yang menyimpan rantai langkah persetujuan: langkah awal
	 * (<code>getDisposisiStart()</code>), langkah setuju
	 * (<code>getDisposisiSetuju()</code>), dan langkah akhir
	 * (<code>getDisposisiEnd()</code>). Hampir seluruh properti turunan di kelas
	 * ini - {@link #getDibuatOleh()}, {@link #getDisetujuiOleh()},
	 * {@link #getTanggalPersetujuan()}, {@link #getTanggalPembuatan()},
	 * {@link #getAktif()}, {@link #getKodeUnik()} - berakar pada nilai ini.</p>
	 *
	 * <p><b>Getter destruktif</b> karena memanggil <code>check()</code>. Karena
	 * begitu banyak getter lain yang memanggilnya, pemanggilan berulang dalam satu
	 * ekspresi adalah sumber NPE yang sudah terbukti di kelas ini; ambil sekali ke
	 * variabel lokal.</p>
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
	 * {@code return} tanpa mengubah apa pun. Artinya sekali dokumen terikat ke
	 * sebuah instans alur, <b>kaitan itu tidak dapat dilepas lewat setter</b> -
	 * hanya dapat diganti ke instans lain yang sudah tersimpan. Pelindung ini ada
	 * karena melepas kaitan alur akan membuat seluruh properti turunan
	 * (pembuat, penyetuju, tanggal, status aktif) berubah mendadak dan dokumen
	 * yang sudah disetujui bisa tampak kembali "belum disetujui".</p>
	 *
	 * <h3>Ekspresi ternary yang selalu memilih cabang yang sama</h3>
	 * <p>Setelah penjagaan di atas, ekspresi
	 * <code>(this.disposisiSop != null &amp;&amp; (disposisiSop == null ||
	 * disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop</code>
	 * secara logika <b>selalu</b> memilih cabang kedua: kedua syarat di dalam
	 * kurung sudah dipastikan salah oleh penjagaan awal. Jadi baris itu setara
	 * dengan penugasan langsung. Sisa kode ini adalah peninggalan dari versi
	 * sebelum penjagaan awal ditambahkan; dibiarkan apa adanya karena tidak
	 * berbahaya, namun jangan dijadikan contoh saat menulis setter serupa.</p>
	 *
	 * @param disposisiSop instans alur SOP yang sudah tersimpan; null atau tanpa
	 *                     id akan diabaikan
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
	 * Mengembalikan dokumen Pemesanan (PO) yang tercatat pada header PR ini.
	 *
	 * <h3>Ini FK sungguhan - bukan antrean kerja</h3>
	 * <p>Kolom <code>pemesanan_pengadaan_master_asset</code> adalah kunci asing
	 * nyata dari tabel PR ke tabel PO. Perhatikan arah kepemilikannya: <b>baris PR
	 * yang menunjuk ke PO</b>, bukan sebaliknya. Susunan ini berbeda dari intuisi
	 * "dokumen hilir menunjuk dokumen hulu" dan berbeda pula dari pola pada modul
	 * <code>inventory</code>, di mana dokumen pengajuan pembelian gudang sama
	 * sekali tidak memiliki FK ke dokumen realisasinya. Di modul aset, rantai
	 * PR-&gt;PO terjahit rapat - lihat Javadoc kelas untuk daftar lengkap keempat
	 * lapis kaitannya.</p>
	 *
	 * <h3>Semantik sebenarnya: "PO PERTAMA", bukan "PO pemenuh"</h3>
	 * <p>Karena bertipe tunggal, kolom ini tidak dapat mewakili satu PR yang
	 * dipecah ke beberapa PO - padahal pemecahan semacam itu sah secara bisnis dan
	 * didukung antarmuka (pemilih PR pada layar PO mengizinkan pengguna mencentang
	 * baris PR satu per satu). Kode penyimpanan PO hanya mengisi kolom ini
	 * <b>bila masih null</b>, sehingga nilainya berarti "PO pertama yang pernah
	 * menyentuh PR ini". PO kedua dan seterusnya tidak tercatat di sini sama
	 * sekali.</p>
	 * <p>Akibat praktisnya ada dua. Pertama, jangan memakai kolom ini untuk
	 * menghitung realisasi permintaan - pakai relasi tingkat baris pada
	 * {@link PermintaanPengadaanMasterAssetDetail#getPemesananPengadaanMasterAssetDetail()}
	 * atau kueri balik dari {@link PemesananPengadaanMasterAssetDetail}. Kedua,
	 * kolom ini merangkap sebagai <i>penanda status</i>: tombol "Beli Langsung"
	 * pada layar PR hanya muncul selama nilainya masih null, dan tombol "Batalkan
	 * Beli Langsung" hanya muncul setelah terisi. Menghapus PO tanpa
	 * mengosongkan kolom ini akan meninggalkan PR dalam keadaan tidak dapat
	 * diproses lagi lewat jalur beli langsung.</p>
	 *
	 * <h3>Perhatikan: TIDAK memanggil check()</h3>
	 * <p>Berbeda dari mayoritas getter relasi di kelas ini, method ini
	 * mengembalikan field mentah tanpa <code>check()</code>. Konsekuensinya method
	 * ini <b>tidak destruktif</b> - tetapi juga tidak melindungi dari
	 * <code>LazyInitializationException</code> bila entitas sudah lepas dari
	 * sesinya. Perlindungan itu tidak diperlukan karena relasi ini dipetakan
	 * dengan <code>@Fetch(FetchMode.SELECT)</code> tanpa
	 * <code>fetch = LAZY</code>, sehingga Hibernate memuatnya seketika (EAGER)
	 * lewat SELECT terpisah saat PR dibaca. Harga yang dibayar adalah satu kueri
	 * tambahan per baris PR yang dirender - salah satu sumber masalah N+1 pada
	 * layar daftar PR.</p>
	 *
	 * @return PO pertama yang menyentuh PR ini, atau {@code null} bila PR belum
	 *         pernah ditarik ke pemesanan mana pun
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pemesanan_pengadaan_master_asset", nullable = true)
	public PemesananPengadaanMasterAsset getPemesananPengadaanMasterAsset() {
		return pemesananPengadaanMasterAsset;
	}

	/**
	 * Menyetel dokumen Pemesanan yang tercatat pada header PR ini.
	 *
	 * <p>Disetel dari dua tempat: <code>PemesananPengadaanMasterAssetAction</code>
	 * saat menyimpan PO (hanya bila kolom masih null), dan
	 * <code>PermintaanPengadaanMasterAssetAction</code> saat membatalkan pembelian
	 * langsung (disetel {@code null} sebelum PO dihapus). Berbeda dari
	 * {@link #setDisposisiSop(DisposisiSop)}, setter ini <b>menerima</b>
	 * {@code null} - pengosongan memang diperlukan agar PR dapat diproses ulang
	 * setelah PO-nya dibatalkan.</p>
	 *
	 * @param pemesananPengadaanMasterAsset dokumen PO, atau {@code null} untuk
	 *                                      melepaskan kaitan
	 */
	public void setPemesananPengadaanMasterAsset(PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset) {
		this.pemesananPengadaanMasterAsset = pemesananPengadaanMasterAsset;
	}

	/**
	 * Mengembalikan satuan kerja pemilik dokumen - <b>dasar pemisahan tenant</b>
	 * pada modul pengadaan aset.
	 *
	 * <h3>Peran dalam keamanan data</h3>
	 * <p>Nilai ini adalah satu-satunya kriteria pembatasan lingkup yang dipakai
	 * pemilih PR di layar PO: kueri di sana membatasi hasil ke daftar satuan kerja
	 * yang boleh diakses pengguna, ditambah dokumen yang satuan kerjanya null.
	 * <b>Perhatikan konsekuensi cabang terakhir itu</b>: dokumen dengan satuan
	 * kerja kosong terlihat oleh semua pengguna. Karena {@link #getWorkspace()}
	 * memaksa null saat dokumen ditandai tanpa anggaran, dan satuan kerja di sini
	 * ikut diturunkan dari workspace, dokumen tanpa anggaran yang kolom satuan
	 * kerjanya juga kosong akan jatuh ke cabang "terlihat semua". Ini pola filter
	 * tenant lemah yang berulang di AIS dan pantas diperlakukan sebagai batasan
	 * yang diketahui, bukan sebagai jaminan isolasi.</p>
	 *
	 * <h3>Urutan derivasi</h3>
	 * <ol>
	 *   <li>Field di-<i>reattach</i> lewat <code>check()</code>.</li>
	 *   <li>Bila {@link #getWorkspace()} ada dan memiliki satuan kerja, satuan
	 *       kerja workspace itulah yang menang - <b>menimpa</b> nilai kolom.
	 *       Logikanya: pembebanan anggaran yang menentukan unit mana yang
	 *       bertanggung jawab, bukan pilihan pengguna di formulir.</li>
	 * </ol>
	 *
	 * <p><b>Getter destruktif</b>: hasil derivasi ditulis kembali ke field,
	 * sehingga kolom <code>satuan_kerja</code> lambat laun menyelaraskan diri
	 * dengan workspace. Perhatikan bahwa {@link #getWorkspace()} sendiri juga
	 * destruktif dan dipanggil <b>dua kali</b> di sini; pemanggilan kedua bisa
	 * memicu SELECT ulang. Bila method ini disunting, ikuti pola perbaikan pada
	 * {@link #getDisetujuiOleh()} dan ambil workspace sekali ke variabel lokal.</p>
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
	 * <p>Nilai yang disetel akan ditimpa saat dibaca bila {@link #getWorkspace()}
	 * memiliki satuan kerja sendiri.</p>
	 *
	 * @param satuanKerja satuan kerja pemilik
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Mengembalikan sisa pagu anggaran yang tercatat saat permintaan dibuat.
	 *
	 * <p>Nilai ini adalah <i>potret</i> (snapshot) sisa anggaran pada saat
	 * permintaan diajukan, bukan sisa anggaran terkini. Fungsinya untuk cetakan
	 * dokumen dan penelusuran: pembaca dokumen dapat melihat bahwa pada saat
	 * permintaan diajukan, pagu memang mencukupi. Jangan memakainya untuk
	 * validasi anggaran saat ini - untuk itu hitung ulang dari modul
	 * <code>rab</code>.</p>
	 *
	 * <p>Mengembalikan {@code 0.0} untuk nilai null agar aman dibungkus ke tipe
	 * primitif dan diformat tanpa penjagaan tambahan.</p>
	 *
	 * @return sisa pagu tercatat; {@code 0.0} bila belum diisi
	 */
	public Double getSaldo() {
		return saldo == null ? 0.0 : saldo;
	}

	/**
	 * Menyetel sisa pagu anggaran yang tercatat saat permintaan dibuat.
	 *
	 * @param saldo sisa pagu
	 */
	public void setSaldo(Double saldo) {
		this.saldo = saldo;
	}

	/**
	 * Mengembalikan nilai total permintaan.
	 *
	 * <p><b>Bukan nilai turunan.</b> Berbeda dari
	 * {@link PermintaanPengadaanMasterAssetDetail#getHargaTotal()} yang selalu
	 * menghitung ulang dari jumlah kali harga, nilai header ini disimpan apa
	 * adanya dan diisi oleh kode aksi setelah menjumlahkan seluruh baris. Artinya
	 * nilai ini <b>dapat basi</b> bila baris detail diubah lewat jalur yang tidak
	 * memicu perhitungan ulang header. Pada jalur POS
	 * (<code>PengadaanPosApiHelper</code>) nilai ini sengaja dihitung ulang di
	 * server dari baris detail, tepat untuk menghindari nilai kiriman klien yang
	 * tidak dapat dipercaya.</p>
	 *
	 * <p>Nilai ini juga menjadi sumber DP saat tombol "Beli Langsung" menyalin PR
	 * ke PO: <code>pemesanan.setDp(permintaan.getNilai())</code>.</p>
	 *
	 * @return nilai total permintaan; {@code 0.0} bila belum diisi
	 */
	public Double getNilai() {
		return nilai == null ? 0.0 : nilai;
	}

	/**
	 * Menyetel nilai total permintaan.
	 *
	 * @param nilai nilai total
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Mengembalikan tahun periode dokumen.
	 *
	 * <p><b>Getter destruktif dengan nilai bawaan</b>: bila field masih null,
	 * method mengisinya dengan tahun berjalan menurut waktu server
	 * ({@link WaktuUtil}) dan menuliskannya ke field. Pada entitas terkelola,
	 * membaca properti ini karena itu dapat memicu UPDATE.</p>
	 *
	 * <p>Perilaku ini berarti dokumen lama yang kolom tahunnya kosong akan
	 * "mengadopsi" tahun saat ia pertama kali dibuka - bukan tahun dokumen itu
	 * sebenarnya. Untuk pelaporan historis, andalkan
	 * {@link #getTanggalPembuatan()}, bukan properti ini.</p>
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
	 * Mengembalikan bulan periode dokumen dalam rentang 1-12.
	 *
	 * <p>Sama seperti {@link #getTahun()}, ini <b>getter destruktif dengan nilai
	 * bawaan</b>. Perhatikan penambahan <code>+ 1</code>: {@link Calendar} memakai
	 * bulan berbasis nol (Januari = 0), sedangkan kolom ini menyimpan bulan
	 * berbasis satu agar langsung dapat ditampilkan dan dibandingkan dengan
	 * periode akuntansi. Jangan menghapus penambahan itu.</p>
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
	 * Mengembalikan template penomoran surat untuk dokumen ini.
	 *
	 * <p><b>Getter destruktif dengan nilai bawaan konstan</b>: bila field masih
	 * null, method mengisinya dengan
	 * {@link NomorSuratAlurPengadaan#PERMINTAAN_PEMBELIAN_DATA} - template yang
	 * memang diperuntukkan bagi dokumen permintaan pembelian. Bandingkan dengan
	 * {@link PemesananPengadaanMasterAsset#getNomorSuratAlurPengadaan()} yang
	 * memakai konstanta <code>PEMESANAN_PEMBELIAN_DATA</code>; pasangan konstanta
	 * inilah yang membedakan format nomor surat kedua dokumen.</p>
	 *
	 * <p>Perhatikan asimetri percabangannya: <code>check()</code> hanya dipanggil
	 * pada cabang "sudah terisi". Pada cabang bawaan, konstanta dipakai langsung
	 * tanpa <i>reattach</i> - aman karena konstanta itu bukan proxy yang terikat
	 * sesi.</p>
	 *
	 * @return template penomoran; tidak pernah {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat_alur_pengadaan", nullable = true)
	public NomorSuratAlurPengadaan getNomorSuratAlurPengadaan() {
		if (nomorSuratAlurPengadaan == null) {
			nomorSuratAlurPengadaan = NomorSuratAlurPengadaan.PERMINTAAN_PEMBELIAN_DATA;
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
	 * Mengembalikan apakah pengadaan atas permintaan ini wajib melalui perjanjian
	 * kerjasama (kontrak payung) lebih dulu.
	 *
	 * <p>Bila true, alur yang diharapkan adalah PR -&gt;
	 * {@link PerjanjianKerjasamaMasterAsset} -&gt; PO, bukan PR -&gt; PO langsung.
	 * Perhatikan bahwa bendera ini bersifat <b>informatif</b>: tidak ada
	 * penegakan pada entitas ini maupun pada pemilih PR di layar PO yang mencegah
	 * PR bertanda wajib-kontrak ditarik langsung menjadi PO. Penegakannya, bila
	 * ada, berada di lapisan aksi.</p>
	 *
	 * <p>Mengembalikan {@code false} untuk null (pola bendera berpelindung yang
	 * dipakai konsisten di kelas ini) sehingga aman di-<i>unbox</i>.</p>
	 *
	 * @return {@code true} bila wajib melalui kontrak payung
	 */
	public Boolean getWajibAdaPerjanjianKerjasama() {
		return wajibAdaPerjanjianKerjasama == null ? false : wajibAdaPerjanjianKerjasama;
	}

	/**
	 * Menyetel penanda wajib melalui perjanjian kerjasama.
	 *
	 * @param wajibAdaPerjanjianKerjasama {@code true} bila wajib kontrak payung
	 */
	public void setWajibAdaPerjanjianKerjasama(Boolean wajibAdaPerjanjianKerjasama) {
		this.wajibAdaPerjanjianKerjasama = wajibAdaPerjanjianKerjasama;
	}

	/**
	 * Mengembalikan apakah permintaan ini diajukan di luar mata anggaran.
	 *
	 * <p>Bendera ini memiliki <b>efek merusak</b> pada {@link #getWorkspace()}:
	 * selama bernilai true, workspace dipaksa {@code null} dan pemaksaan itu
	 * ditulis ke kolom. Karena {@link #getSatuanKerja()} dan {@link #getAkun()}
	 * ikut diturunkan dari workspace, menyalakan bendera ini melepaskan dokumen
	 * dari tiga kaitan sekaligus. Lihat penjelasan lengkap pada
	 * {@link #getWorkspace()}.</p>
	 *
	 * <p>Mengembalikan {@code false} untuk null.</p>
	 *
	 * @return {@code true} bila permintaan di luar anggaran
	 */
	public Boolean getTanpaAnggaran() {
		return tanpaAnggaran == null ? false : tanpaAnggaran;
	}

	/**
	 * Menyetel penanda permintaan di luar mata anggaran.
	 *
	 * <p><b>Peringatan</b>: menyetel {@code true} lalu membaca
	 * {@link #getWorkspace()} akan mengosongkan kaitan anggaran secara permanen.
	 * Mengembalikan bendera ke {@code false} tidak memulihkannya.</p>
	 *
	 * @param tanpaAnggaran {@code true} bila di luar anggaran
	 */
	public void setTanpaAnggaran(Boolean tanpaAnggaran) {
		this.tanpaAnggaran = tanpaAnggaran;
	}

	/**
	 * Mengembalikan akun pembebanan akuntansi untuk permintaan ini.
	 *
	 * <p>Diturunkan dari {@link #getWorkspace()} bila workspace memiliki akun;
	 * jika tidak, nilai kolom dipakai setelah di-<i>reattach</i>. Urutan
	 * prioritasnya sama dengan {@link #getSatuanKerja()}: mata anggaran adalah
	 * sumber kebenaran, kolom hanya cadangan.</p>
	 *
	 * <p>Perhatikan perbedaan struktur dengan {@link #getSatuanKerja()}: di sini
	 * <code>check()</code> hanya dipanggil pada cabang <i>else</i>, sedangkan di
	 * sana dipanggil lebih dulu untuk kedua cabang. Efek akhirnya setara, tetapi
	 * bentuk ini menghemat satu <i>reattach</i> ketika workspace sudah memasok
	 * akun.</p>
	 *
	 * <p><b>Getter destruktif</b>, dan memanggil {@link #getWorkspace()} - yang
	 * juga destruktif - sebanyak dua kali.</p>
	 *
	 * @return akun pembebanan, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = true)
	public Akun getAkun() {
		if (getWorkspace() != null && getWorkspace().getAkun() != null) {
			akun = getWorkspace().getAkun();
		} else {
			akun = check(akun);
		}
		return akun;
	}

	/**
	 * Menyetel akun pembebanan akuntansi.
	 *
	 * <p>Nilai yang disetel akan ditimpa saat dibaca bila {@link #getWorkspace()}
	 * memiliki akun sendiri.</p>
	 *
	 * @param akun akun pembebanan
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Mengembalikan pengguna yang menolak dokumen ini.
	 *
	 * <p>Nilai ini adalah <b>penentu tertinggi</b> dalam logika status: baik
	 * {@link #getDisetujuiOleh()} maupun {@link #getTanggalPersetujuan()} memaksa
	 * hasilnya {@code null} begitu penolak terisi, terlepas dari apa yang dikatakan
	 * alur SOP. Dengan kata lain penolakan tidak dapat "tertutup" oleh persetujuan
	 * yang datang belakangan; pembatalan penolakan harus dilakukan eksplisit lewat
	 * tombol "Batalkan" yang mengosongkan penolak, tanggal penolakan, penyetuju,
	 * dan tanggal persetujuan sekaligus.</p>
	 *
	 * <p>Berbeda dari penyetuju, penolak <b>tidak</b> diturunkan dari alur SOP -
	 * ia murni nilai kolom yang di-<i>reattach</i>. Penolakan lewat alur SOP
	 * tercermin di tempat lain, yaitu pada {@link #getAktif()} yang memeriksa
	 * apakah langkah akhir disposisi berada di simpul penolakan.</p>
	 *
	 * <p><b>Getter destruktif</b> karena memanggil <code>check()</code>.</p>
	 *
	 * @return pengguna penolak, atau {@code null} bila dokumen belum ditolak
	 * @see #getAlasanDitolak()
	 * @see #getTanggalDitolak()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ditolak_oleh", nullable = true)
	public Tbmuser getDitolakOleh() {
		ditolakOleh = check(ditolakOleh);
		return ditolakOleh;
	}

	/**
	 * Menyetel pengguna yang menolak dokumen ini.
	 *
	 * <p>Disetel bersamaan dengan {@link #setTanggalDitolak(Date)} dan
	 * {@link #setSetujuiManual(Boolean)} = false oleh tombol "Ditolak" pada layar
	 * PR. Menyetel nilai ini secara terpisah akan membuat dokumen tampak ditolak
	 * tanpa tanggal penolakan.</p>
	 *
	 * @param ditolakOleh pengguna penolak, atau {@code null} untuk membatalkan
	 *                    penolakan
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
	 * PR.</p>
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
	 * Mengembalikan apakah permintaan ini sudah ditutup secara manual.
	 *
	 * <p>Penutupan dipakai ketika sisa kebutuhan pada PR memang tidak akan
	 * dipesan lagi, sehingga PR tidak perlu terus muncul sebagai pekerjaan
	 * tertunggak. Pemilih PR pada layar PO menyaring dokumen tertutup lewat
	 * kondisi <code>tutup IS NULL OR tutup = false</code>, yaitu saat kotak
	 * centang penyaring aktif dinyalakan.</p>
	 *
	 * <p><b>Perhatikan perbedaan dengan saudaranya.</b>
	 * {@link PemesananPengadaanMasterAsset#getTutup()} mengembalikan nilai
	 * {@link Boolean} mentah yang <b>bisa null</b>, sedangkan method ini
	 * menormalkan null menjadi {@code false}. Jangan menyalin pola penggunaan dari
	 * satu ke yang lain tanpa memeriksa: kode yang menulis
	 * <code>if (po.getTutup())</code> aman untuk PR namun berisiko
	 * <code>NullPointerException</code> untuk PO.</p>
	 *
	 * @return {@code true} bila permintaan sudah ditutup; tidak pernah
	 *         {@code null}
	 */
	public Boolean getTutup() {
		return tutup == null ? false : tutup;
	}

	/**
	 * Menyetel penanda permintaan ditutup manual.
	 *
	 * @param tutup {@code true} untuk menutup permintaan
	 */
	public void setTutup(Boolean tutup) {
		this.tutup = tutup;
	}

	/**
	 * Mengembalikan apakah dokumen masih berlaku - <b>lapis pertahanan kedua</b>
	 * gerbang persetujuan PR-&gt;PO.
	 *
	 * <h3>Mengapa penting</h3>
	 * <p>Pemilih PR pada layar PO memfilter dua hal: <code>disetujuiOleh IS NOT
	 * NULL</code> dan <code>aktif IS NULL OR aktif = true</code>. Filter kedua
	 * itulah yang menutup celah yang ditinggalkan mode persetujuan manual (lihat
	 * {@link #getSetujuiManual()}): sebuah PR yang disetujui manual lalu alurnya
	 * ditolak lewat modul SOP tetap melaporkan penyetuju di
	 * {@link #getDisetujuiOleh()}, tetapi method ini akan menghitung ulang dan
	 * memberi {@code false}, sehingga dokumen tersaring keluar.</p>
	 *
	 * <h3>Urutan keputusan</h3>
	 * <ol>
	 *   <li>Bila {@link #getDisetujuiOleh()} tidak null, dokumen ditandai aktif.
	 *       Perhatikan bahwa ini hanya <i>menyalakan</i>, tidak pernah mematikan -
	 *       aturan berikutnya masih dapat menimpanya menjadi false.</li>
	 *   <li>Bila disposisi ada dan bendera aktif disposisi <b>bukan</b>
	 *       {@code true}, dokumen dimatikan. Penulisan
	 *       <code>!Boolean.TRUE.equals(...)</code> dipilih agar aman terhadap
	 *       null - bandingkan dengan
	 *       {@link PemesananPengadaanMasterAsset#getAktif()} yang memakai
	 *       <code>!disposisiSop.getAktif()</code> dan mengandalkan getter di sisi
	 *       {@link DisposisiSop} untuk menormalkan null.</li>
	 *   <li>Bila langkah akhir disposisi berada pada simpul alur yang ditandai
	 *       "penolakan ada di sini", dokumen dimatikan. Inilah cara penolakan di
	 *       mesin SOP merambat ke dokumen.</li>
	 * </ol>
	 * <p>Nilai bawaan bila seluruh aturan tidak berlaku adalah {@code true} -
	 * dokumen dianggap berlaku sampai terbukti sebaliknya. Karena itu filter di
	 * pemilih PR harus menerima <code>aktif IS NULL</code>, dan memang begitu.</p>
	 *
	 * <h3>Perbaikan akar masalah yang sudah tertanam</h3>
	 * <p>Komentar dalam badan method mencatat bahwa disposisi dan langkah akhirnya
	 * diambil <b>sekali</b> ke variabel lokal, bukan dipanggil ulang di setiap
	 * ekspresi - pola yang sama dengan perbaikan pada {@link #getDisetujuiOleh()}.
	 * Perhatikan satu detail yang mudah terlewat: hasil {@link #getDisposisiSop()}
	 * ditugaskan ke <b>field</b> {@code disposisiSop}, bukan ke variabel lokal
	 * baru. Efeknya sama untuk keperluan konsistensi pembacaan, namun berarti
	 * method ini menulis ke field - satu lagi getter destruktif.</p>
	 *
	 * @return {@code true} bila dokumen masih berlaku; tidak pernah {@code null}
	 * @see #getSetujuiManual()
	 * @see #getDisetujuiOleh()
	 */
	public Boolean getAktif() {

		if (getDisetujuiOleh() != null) {
			aktif = true;
		}
		// ROOT CAUSE FIX: ambil disposisiSop dan disposisiEnd sekali ke variabel lokal
		// (bukan memanggil ulang getter lazy-load beberapa kali dalam satu ekspresi)
		// agar hasil null-check dan pemakaian selalu konsisten.
		disposisiSop = getDisposisiSop();
		if (disposisiSop != null && !Boolean.TRUE.equals(disposisiSop.getAktif())) {
			aktif = false;
		}
		if (disposisiSop != null) {
			DisposisiAlurSop end = disposisiSop.getDisposisiEnd();
			AlurSop endAlur = end == null ? null : end.getAlurSop();
			if (endAlur != null && Boolean.TRUE.equals(endAlur.getPenolakanAdaDiSini())) {
				aktif = false;
			}
		}
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel penanda dokumen berlaku.
	 *
	 * <p>Nilai yang disetel akan dihitung ulang - dan berpotensi ditimpa - pada
	 * pembacaan {@link #getAktif()} berikutnya bila dokumen terikat alur SOP.</p>
	 *
	 * @param aktif {@code true} bila dokumen berlaku
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan apakah dokumen ini disetujui secara <b>manual</b>, yaitu di
	 * luar mesin alur SOP.
	 *
	 * <h3>Fungsi: sakelar yang memutus derivasi</h3>
	 * <p>Bendera ini adalah sakelar yang mengubah perilaku dua getter status
	 * sekaligus. Selama bernilai {@code false} (bawaan),
	 * {@link #getDisetujuiOleh()} dan {@link #getTanggalPersetujuan()} menghitung
	 * ulang nilainya dari {@link DisposisiSop} setiap kali dibaca. Begitu bernilai
	 * {@code true}, kedua getter itu melewati seluruh perhitungan dan
	 * mengembalikan isi kolom apa adanya.</p>
	 *
	 * <h3>Siapa yang menyalakannya</h3>
	 * <p>Hanya satu tempat: tombol "Persetujuan" pada
	 * <code>PermintaanPengadaanMasterAssetAction</code>, yang menyalakan bendera
	 * ini bersamaan dengan menyetel pengguna aktif sebagai penyetuju. Dua tempat
	 * mematikannya kembali - tombol "Ditolak" dan tombol "Batalkan" - keduanya
	 * juga mengosongkan nilai persetujuan. Satu tempat lagi,
	 * <code>HibernateProcurementRequisitionPort</code>, menyetelnya {@code false}
	 * secara eksplisit saat membuat PR lewat jalur port, memastikan PR jalur itu
	 * tunduk pada alur SOP.</p>
	 *
	 * <h3>Implikasi keamanan yang perlu diketahui</h3>
	 * <p>Tombol "Persetujuan" itu sendiri dijaga oleh hak akses
	 * <code>approve</code> pada menu, dan hanya tampil ketika dokumen belum
	 * disetujui dan belum ditolak. Yang <b>tidak</b> diperiksa adalah apakah
	 * penyetuju sama dengan pembuat dokumen - persetujuan atas dokumen sendiri
	 * (<i>self-approval</i>) dimungkinkan bagi pengguna yang memegang hak
	 * <code>approve</code>. Ini konsisten dengan pola yang sudah tercatat di
	 * modul-modul lain AIS dan bukan temuan baru.</p>
	 * <p>Yang lebih halus: karena mode manual memutus derivasi, sebuah PR yang
	 * disetujui manual <b>tidak akan kehilangan status persetujuannya</b> meski
	 * instans alur SOP-nya kemudian ditolak lewat modul SOP. Celah ini tertutup
	 * oleh {@link #getAktif()} yang tetap menghitung ulang dari disposisi dan
	 * dipakai sebagai filter kedua di pemilih PR. Jadi gerbang tetap utuh, tetapi
	 * ia bertumpu pada <i>dua</i> kolom yang harus difilter bersama. Setiap kueri
	 * baru yang mencari "PR siap dipesan" wajib menyertakan <b>kedua</b> syarat -
	 * menyalin hanya syarat <code>disetujuiOleh IS NOT NULL</code> akan membuka
	 * celah tersebut.</p>
	 *
	 * <p>Mengembalikan {@code false} untuk null, sehingga dokumen lama yang
	 * kolomnya kosong otomatis berada di mode turunan alur SOP.</p>
	 *
	 * @return {@code true} bila persetujuan disetel manual; tidak pernah
	 *         {@code null}
	 */
	public Boolean getSetujuiManual() {
		return setujuiManual == null ? false : setujuiManual;
	}

	/**
	 * Menyetel mode persetujuan manual.
	 *
	 * <p><b>Peringatan</b>: menyalakan bendera ini tanpa sekaligus menyetel
	 * {@link #setDisetujuiOleh(Tbmuser)} akan membekukan dokumen pada nilai
	 * persetujuan yang tersimpan saat itu - termasuk nilai kosong - dan
	 * memutuskannya dari alur SOP. Selalu setel keduanya bersamaan, seperti yang
	 * dilakukan tombol "Persetujuan".</p>
	 *
	 * @param setujuiManual {@code true} untuk memutus derivasi dari alur SOP
	 */
	public void setSetujuiManual(Boolean setujuiManual) {
		this.setujuiManual = setujuiManual;
	}

	/**
	 * Mengembalikan apakah permintaan ini didanai dari dana titipan.
	 *
	 * <p>Dana titipan adalah dana pihak ketiga yang dikelola lembaga namun bukan
	 * milik lembaga, sehingga pembebanannya tidak melalui pagu anggaran biasa.
	 * Bendera ini dipakai modul akuntansi untuk memilih jurnal yang tepat.
	 * Berbeda dari {@link #getTanpaAnggaran()}, bendera ini <b>tidak</b> merusak
	 * kaitan workspace - ia murni penanda.</p>
	 *
	 * <p>Mengembalikan {@code false} untuk null.</p>
	 *
	 * @return {@code true} bila didanai dana titipan; tidak pernah {@code null}
	 */
	public Boolean getDanaTitipan() {
		return danaTitipan == null ? false : danaTitipan;
	}

	/**
	 * Menyetel penanda pendanaan dari dana titipan.
	 *
	 * @param danaTitipan {@code true} bila didanai dana titipan
	 */
	public void setDanaTitipan(Boolean danaTitipan) {
		this.danaTitipan = danaTitipan;
	}


	/**
	 * Alasan penolakan PR -- diisi saat dokumen ditolak agar pembuat PR tahu langkah
	 * perbaikannya. Ditambahkan 2026-08-20 bersama pemakaian modul ini oleh POS
	 * (Desktop/Android/JSP); kolom NULLABLE sehingga baris lama tidak terdampak.
	 *
	 * <p>Pada jalur POS (<code>PengadaanPosApiHelper</code>) pengisian alasan
	 * diwajibkan minimal lima karakter sebelum penolakan diterima. Pada jalur ZK,
	 * kewajiban itu tidak ditegakkan - tombol "Ditolak" hanya menyetel penolak dan
	 * tanggal penolakan, sehingga dokumen yang ditolak dari layar ZK dapat
	 * memiliki alasan kosong. Perbedaan ini penting bila menulis laporan yang
	 * mengasumsikan alasan selalu terisi.</p>
	 *
	 * @return alasan penolakan, atau {@code null} bila belum ditolak atau alasan
	 *         tidak diisi
	 * @see #getDitolakOleh()
	 */
	@Column(name = "alasan_ditolak", nullable = true)
	public String getAlasanDitolak() {
		return alasanDitolak;
	}

	/**
	 * Menyetel alasan penolakan dokumen.
	 *
	 * <p>Tidak memvalidasi panjang minimum; validasi itu berada di
	 * <code>PengadaanPosApiHelper</code> untuk jalur POS.</p>
	 *
	 * @param alasanDitolak alasan penolakan
	 */
	public void setAlasanDitolak(String alasanDitolak) {
		this.alasanDitolak = alasanDitolak;
	}
}
