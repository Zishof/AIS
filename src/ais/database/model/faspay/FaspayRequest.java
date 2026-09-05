package ais.database.model.faspay;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;




import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;



import ais.common.Common;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.GeneralValueObject;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.KegiatanTemporary;
import ais.database.model.Mahasiswa;

/**
 * Entity JPA/Hibernate header transaksi permintaan pembayaran host-to-host (H2H) via payment
 * gateway <b>Faspay</b> untuk pembayaran biaya mahasiswa, calon mahasiswa, atau peserta kegiatan.
 * Baris {@code faspay_request} dibuat sistem ketika pengguna memulai pembayaran, ditautkan
 * opsional ke {@link Mahasiswa} atau {@link BiodataCalonMahasiswa}, {@link JenisKegiatan}, dan
 * {@link JadwalPembayaran}, serta banyak-ke-banyak ke {@link KegiatanTemporary} (kegiatan
 * sementara yang menyertai satu transaksi pembayaran, mis. beberapa item pendaftaran ulang
 * sekaligus). Entity ini direlasikan satu-ke-satu (opsional, diisi belakangan) ke {@link
 * FaspayResponse} begitu balasan/callback dari Faspay diterima. Rincian item biaya yang
 * ditagihkan disimpan terpisah pada {@code ais.database.model.faspay.FaspayRequestDetail} (per
 * pos tagihan) dan {@code ais.database.model.faspay.FaspayRequestDetailBiaya} (per komponen biaya
 * administrasi/fee).
 *
 * <p><b>Pola arsitektur berulang:</b> kelas ini mengikuti pola generik 4-entity (Request/
 * RequestDetail/RequestDetailBiaya/Response) yang identik strukturnya di lebih dari 9 paket model
 * integrasi bank/payment-gateway berbeda di AIS (mis. {@code ais.database.model.bni.BniRequest}
 * untuk Bank BNI, {@code ais.database.model.cimb.CimbRequest} untuk CIMB Niaga, {@code
 * ais.database.model.doku.DokuRequest} untuk Doku). Nama kolom dan makna field pada umumnya sama;
 * kelas ini secara struktur paling mirip dengan {@code BniRequest} (keduanya memiliki field {@code
 * request}/{@code response} untuk payload mentah dan relasi {@code @ManyToMany} ke {@link
 * KegiatanTemporary}), namun menambahkan field khusus Faspay: {@link #getSignature()} dan {@link
 * #getPayment_channel_name()}.</p>
 *
 * <p><b>Catatan keamanan — entity dengan eksposur data H2H paling besar di antara CIMB/Doku/
 * Faspay:</b></p>
 * <ul>
 * <li>{@link #getRequest()} dan {@link #getResponse()} menyimpan payload mentah (utuh, tidak
 * di-mask) dari permintaan dan balasan API ke/dari Faspay — pola yang identik dengan payload
 * mentah Bank Mandiri/OCBC NISP yang sudah tercatat sebagai temuan keamanan Tier 1 pada {@code
 * LogHostToHostAction.java} (baris 481, 557 pada revisi tercatat).</li>
 * <li>{@link #getSignature()} menyimpan nilai signature/MAC transaksi (biasanya hash yang
 * melibatkan kredensial merchant Faspay) secara mentah, tanpa masking.</li>
 * <li>Baris ini, lengkap dengan kolom {@code request}, {@code response}, dan {@code status},
 * ditampilkan langsung di grid UI ZK {@code ais.action.master.faspay.FaspayRequestAction} kepada
 * setiap pengguna yang memiliki hak akses READ pada layar tersebut (gerbang login/privilege ada,
 * namun tidak ada masking terhadap payload finansial mentah pada kolom grid itu sendiri) — kanal
 * paparan ini <b>terpisah</b> dari {@code LogHostToHostAction} (yang khusus Mandiri/OCBC): tidak
 * ada satu "log H2H" bersama untuk seluruh gateway, melainkan tiap gateway punya UI/servlet
 * sendiri dengan pola paparan yang serupa namun independen.</li>
 * </ul>
 *
 * @see FaspayResponse
 * @see ais.database.model.bni.BniRequest
 * @see ais.database.model.cimb.CimbRequest
 * @see ais.database.model.doku.DokuRequest
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "faspay_request")



public class FaspayRequest extends GeneralValueObject {
	/**
	 * Versi serialisasi tetap untuk kompatibilitas antar build ({@link java.io.Serializable}).
	 */
	private static final long serialVersionUID = 2463821327548439808L;
	/** Primary key auto-increment (identity) baris permintaan Faspay ini. */
	private Long id;
	/** Nama/label pengguna (audit shadow) yang terakhir membuat/mengubah baris ini. */
	private String oleh;
	/** ID pengguna (audit shadow) yang terakhir membuat/mengubah baris ini, independen dari
	 * relasi entity user. */
	private String olehId;

	/**
	 * Mengambil ID pengguna (audit shadow) yang terakhir membuat/mengubah baris ini.
	 *
	 * @return ID pengguna, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi ID pengguna (audit shadow). Nilai {@code null} atau kosong diabaikan sehingga
	 * nilai audit sebelumnya tetap dipertahankan.
	 *
	 * @param olehId ID pengguna yang akan dicatat; diabaikan bila {@code null}/kosong.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna (audit shadow). Nilai {@code null} atau kosong diabaikan sehingga
	 * nilai audit sebelumnya tetap dipertahankan.
	 *
	 * @param oleh nama pengguna yang akan dicatat; diabaikan bila {@code null}/kosong.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna (audit shadow) yang terakhir membuat/mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: memperbarui {@link #getTanggal_dirubah()} otomatis lewat
	 * {@code AuditTimestampInterceptor.ubah(this)} setiap kali baris ini di-UPDATE.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi timestamp terakhir baris ini diubah. Biasanya diisi otomatis oleh {@link
	 * #onUpdate()}.
	 *
	 * @param tanggal_dirubah timestamp perubahan terakhir.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil timestamp terakhir baris ini diubah.
	 *
	 * @return timestamp perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas untuk debugging/log: {@code id-nama}.
	 *
	 * @return string ringkas berisi {@link #getId()} dan nama sesi pengguna.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Label sesi/nama pengguna yang membuat permintaan ini; dipetakan ke kolom {@code session_id}. */
	private String nama;
	/** Penanda apakah cicilan sebelumnya (belum lunas) harus dihapus saat permintaan baru dibuat. */
	private Boolean hapusCicilanSebelumnya;
	/** Penanda apakah status transaksi perlu dicek ulang ke Faspay sebelum diproses lebih lanjut. */
	private Boolean checkUlang;
	/** URL redirect pembayaran Faspay yang diberikan kepada pengguna untuk menyelesaikan transaksi. */
	private String url;
	/** ID transaksi (trxId) yang dikirim ke/diterima dari Faspay sebagai identifier unik transaksi. */
	private String trxId;
	/** Nomor tagihan (bill number) yang dikirim ke Faspay. */
	private String billNo;
	/** ID/nama merchant Faspay yang memproses transaksi ini; diproyeksikan dari {@link
	 * #faspayResponse} bila sudah tersedia. */
	private String merchant;

	/** Payload mentah (utuh, tanpa masking) permintaan yang dikirim ke API Faspay. */
	private String request;
	/** ID merchant Faspay yang mengirimkan permintaan ini. */
	private String merchant_id;
	/** Nilai signature/MAC transaksi (biasanya hash yang melibatkan kredensial merchant Faspay),
	 * disimpan mentah tanpa masking. */
	private String signature;

	/** Kode respons mentah dari Faspay atas permintaan ini. */
	private String response_code;
	/** Deskripsi/pesan respons mentah dari Faspay atas permintaan ini. */
	private String response_desc;
	/** Nama kanal pembayaran yang dipilih/digunakan pengguna (mis. nama bank VA, dompet
	 * digital); di-default-kan ke {@code "Faspay"} bila belum diisi. */
	private String payment_channel_name;

	/** Mahasiswa pemilik transaksi ini, bila permintaan dibuat untuk mahasiswa aktif. */
	private Mahasiswa mahasiswa;
	/** Calon mahasiswa pemilik transaksi ini, bila permintaan dibuat untuk proses PMB (belum
	 * menjadi mahasiswa). */
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	/** Jenis kegiatan (mis. daftar ulang, her-registrasi) yang menjadi konteks tagihan ini. */
	private JenisKegiatan jenisKegiatan;
	/** Jadwal pembayaran yang menjadi acuan komponen biaya yang ditagihkan. */
	private JadwalPembayaran jadwalPembayaran;
	/** Semester akademik terkait transaksi ini. */
	private Integer semester;
	/** Tahun akademik terkait transaksi ini. */
	private String tahunAkademik;
	/** Keterangan bebas mengenai transaksi ini. */
	private String keterangan;
	/** Nilai potongan/pengurangan yang diterapkan pada tagihan. */
	private Double pengurangan;
	/** Total nominal yang diminta untuk dibayarkan melalui Faspay. */
	private Double amount;
	/** Total nilai biaya yang seharusnya dibayar sebelum potongan (nilai acuan, bukan nilai
	 * final tagihan). */
	private Double nilaiBiayaHarusDiBayars;
	/** Balasan/hasil dari Faspay untuk permintaan ini, diisi setelah callback diterima. */
	private FaspayResponse faspayResponse;
	/** Status pemrosesan transaksi dalam bahasa Indonesia (mis. "Belum diproses"). */
	private String status;
	/** Kode status numerik/mentah dari hasil pemrosesan transaksi. */
	private String kodeStatus;
	/** Payload mentah (utuh, tanpa masking) balasan/respons yang diterima dari API Faspay. */
	private String response;
	/** Biaya administrasi yang dibebankan untuk transaksi ini. */
	private Double biayaAdministrasi;
	/** Biaya payment gateway (fee Faspay) yang dibebankan untuk transaksi ini. */
	private Double biayaPaymentGateway;

	/** Kumpulan kegiatan sementara ({@link KegiatanTemporary}) yang menyertai transaksi ini
	 * (mis. beberapa item pendaftaran ulang yang dibayar sekaligus dalam satu transaksi). */
	private Set<KegiatanTemporary> kegiatanTemporarys = new HashSet<KegiatanTemporary>();

	/**
	 * Mengambil kumpulan kegiatan sementara yang menyertai transaksi ini, lewat tabel penghubung
	 * {@code faspay_request_has_kegiatan_temporary}.
	 *
	 * @return himpunan {@link KegiatanTemporary} terkait; tidak pernah {@code null} (diinisialisasi
	 *         sebagai {@link HashSet} kosong).
	 */
	@ManyToMany(targetEntity = KegiatanTemporary.class, cascade = { CascadeType.MERGE,
			CascadeType.PERSIST })
	@JoinTable(name = "faspay_request_has_kegiatan_temporary", joinColumns = @JoinColumn(name = "faspay_request") , inverseJoinColumns = @JoinColumn(name = "kegiatan_temporary") )
	public Set<KegiatanTemporary> getKegiatanTemporarys() {
		return kegiatanTemporarys;
	}

	/**
	 * Mengisi kumpulan kegiatan sementara yang menyertai transaksi ini.
	 *
	 * @param kegiatanTemporarys himpunan {@link KegiatanTemporary} yang akan diisi.
	 */
	public void setKegiatanTemporarys(Set<KegiatanTemporary> kegiatanTemporarys) {
		this.kegiatanTemporarys = kegiatanTemporarys;
	}

	/**
	 * Konstruktor default (dibutuhkan Hibernate).
	 */
	public FaspayRequest() {
	}

	/**
	 * Mengambil primary key baris ini.
	 *
	 * @return ID baris, atau {@code null} bila belum dipersistensi.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi primary key baris ini. Umumnya tidak dipanggil manual karena kolom {@code id}
	 * bersifat {@code insertable = false}.
	 *
	 * @param id nilai ID yang akan diisi.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil label sesi/nama pengguna pembuat permintaan, dengan whitespace di-trim.
	 * Dipetakan ke kolom {@code session_id}.
	 *
	 * @return nama yang sudah di-trim, atau {@code null} bila belum diisi.
	 */
	@Column(name = "session_id", columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi label sesi/nama pengguna pembuat permintaan.
	 *
	 * @param nama nama/label sesi yang akan diisi.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil ID transaksi (trxId) unik untuk permintaan ini, dengan whitespace di-trim.
	 *
	 * @return trxId yang sudah di-trim; string kosong bila belum pernah diisi.
	 */
	public String getTrxId() {
		return trxId == null ? "" : trxId.trim();
	}

	/**
	 * Mengisi ID transaksi (trxId) secara eksplisit.
	 *
	 * @param trxId trxId yang akan diisi.
	 */
	public void setTrxId(String trxId) {
		this.trxId = trxId;
	}

	/**
	 * Mengambil relasi ke mahasiswa pemilik transaksi ini.
	 *
	 * @return {@link Mahasiswa} terkait, atau {@code null} bila permintaan ini untuk calon
	 *         mahasiswa (bukan mahasiswa aktif).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		return mahasiswa;
	}

	/**
	 * Mengisi relasi ke mahasiswa pemilik transaksi ini.
	 *
	 * @param mahasiswa mahasiswa yang akan ditautkan.
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Mengambil relasi ke calon mahasiswa pemilik transaksi ini.
	 *
	 * @return {@link BiodataCalonMahasiswa} terkait, atau {@code null} bila permintaan ini untuk
	 *         mahasiswa aktif (bukan calon mahasiswa).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "biodata_calon_mahasiswa", nullable = true)
	public BiodataCalonMahasiswa getBiodataCalonMahasiswa() {
		return biodataCalonMahasiswa;
	}

	/**
	 * Mengisi relasi ke calon mahasiswa pemilik transaksi ini.
	 *
	 * @param biodataCalonMahasiswa calon mahasiswa yang akan ditautkan.
	 */
	public void setBiodataCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	/**
	 * Mengambil jenis kegiatan yang menjadi konteks tagihan ini.
	 *
	 * @return {@link JenisKegiatan} terkait, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jenis_kegiatan", nullable = true)
	public JenisKegiatan getJenisKegiatan() {
		return jenisKegiatan;
	}

	/**
	 * Mengisi jenis kegiatan yang menjadi konteks tagihan ini.
	 *
	 * @param jenisKegiatan jenis kegiatan yang akan ditautkan.
	 */
	public void setJenisKegiatan(JenisKegiatan jenisKegiatan) {
		this.jenisKegiatan = jenisKegiatan;
	}

	/**
	 * Mengambil jadwal pembayaran acuan komponen biaya yang ditagihkan.
	 *
	 * @return {@link JadwalPembayaran} terkait, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jadwal_pembayaran", nullable = true)
	public JadwalPembayaran getJadwalPembayaran() {
		return jadwalPembayaran;
	}

	/**
	 * Mengisi jadwal pembayaran acuan komponen biaya yang ditagihkan.
	 *
	 * @param jadwalPembayaran jadwal pembayaran yang akan ditautkan.
	 */
	public void setJadwalPembayaran(JadwalPembayaran jadwalPembayaran) {
		this.jadwalPembayaran = jadwalPembayaran;
	}

	/**
	 * Mengambil semester akademik terkait transaksi ini.
	 *
	 * @return nomor semester, atau {@code null} bila belum diisi.
	 */
	public Integer getSemester() {
		return semester;
	}

	/**
	 * Mengisi semester akademik terkait transaksi ini.
	 *
	 * @param semester nomor semester yang akan diisi.
	 */
	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	/**
	 * Mengambil tahun akademik terkait transaksi ini.
	 *
	 * @return tahun akademik, atau {@code null} bila belum diisi.
	 */
	public String getTahunAkademik() {
		return tahunAkademik;
	}

	/**
	 * Mengisi tahun akademik terkait transaksi ini.
	 *
	 * @param tahunAkademik tahun akademik yang akan diisi.
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengambil nilai potongan/pengurangan tagihan.
	 *
	 * @return nilai pengurangan; {@code 0.0} bila belum pernah diisi (nilai {@code null}
	 *         di-default-kan sekaligus disimpan sebagai efek samping getter).
	 */
	public Double getPengurangan() {
		if (pengurangan == null) {
			pengurangan = 0.0;
		}
		return pengurangan;
	}

	/**
	 * Mengisi nilai potongan/pengurangan tagihan.
	 *
	 * @param pengurangan nilai pengurangan yang akan diisi.
	 */
	public void setPengurangan(Double pengurangan) {
		this.pengurangan = pengurangan;
	}

	/**
	 * Mengambil total nilai biaya yang seharusnya dibayar (acuan sebelum potongan).
	 *
	 * @return nilai biaya harus dibayar; {@code 0.0} bila belum pernah diisi (nilai {@code null}
	 *         di-default-kan sekaligus disimpan sebagai efek samping getter).
	 */
	public Double getNilaiBiayaHarusDiBayars() {
		if (nilaiBiayaHarusDiBayars == null) {
			nilaiBiayaHarusDiBayars = 0.0;
		}
		return nilaiBiayaHarusDiBayars;
	}

	/**
	 * Mengisi total nilai biaya yang seharusnya dibayar.
	 *
	 * @param nilaiBiayaHarusDiBayars nilai yang akan diisi.
	 */
	public void setNilaiBiayaHarusDiBayars(Double nilaiBiayaHarusDiBayars) {
		this.nilaiBiayaHarusDiBayars = nilaiBiayaHarusDiBayars;
	}

	/**
	 * Mengambil keterangan bebas mengenai transaksi ini.
	 *
	 * @return keterangan, atau {@code null} bila belum diisi.
	 */
	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Mengisi keterangan bebas mengenai transaksi ini.
	 *
	 * @param keterangan keterangan yang akan diisi.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengambil balasan/hasil dari Faspay untuk permintaan ini.
	 *
	 * @return {@link FaspayResponse} terkait, atau {@code null} bila callback belum diterima.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "faspay_response", nullable = true)
	public FaspayResponse getFaspayResponse() {
		return faspayResponse;
	}

	/**
	 * Mengisi balasan/hasil dari Faspay untuk permintaan ini.
	 *
	 * @param faspayResponse respons Faspay yang akan ditautkan.
	 */
	public void setFaspayResponse(FaspayResponse faspayResponse) {
		this.faspayResponse = faspayResponse;
	}

	/**
	 * Mengambil URL redirect pembayaran Faspay yang diberikan kepada pengguna untuk
	 * menyelesaikan transaksi.
	 *
	 * @return URL redirect, atau {@code null} bila belum diisi.
	 */
	@Column(columnDefinition = "text")
	public String getUrl() {
		return url;
	}

	/**
	 * Mengisi URL redirect pembayaran Faspay.
	 *
	 * @param url URL redirect yang akan diisi.
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Mengambil ID/nama merchant Faspay yang memproses transaksi ini. Bila {@link
	 * #faspayResponse} tersedia, nilainya <b>diambil-alih (overwrite)</b> dari {@link
	 * FaspayResponse#getMerchant()} sebagai efek samping getter, sehingga nilai yang dikembalikan
	 * selalu sinkron dengan respons terbaru dari Faspay selama relasi tersebut sudah ter-load.
	 *
	 * @return ID/nama merchant; bisa {@code null} bila belum ada respons dan belum pernah diisi
	 *         manual.
	 */
	public String getMerchant() {
		if (faspayResponse != null) {
			merchant = faspayResponse.getMerchant();
		}
		return merchant;
	}

	/**
	 * Mengisi ID/nama merchant Faspay secara eksplisit.
	 *
	 * @param merchant ID/nama merchant yang akan diisi.
	 */
	public void setMerchant(String merchant) {
		this.merchant = merchant;
	}

	/**
	 * Mengambil total nominal yang diminta untuk dibayarkan melalui Faspay.
	 *
	 * @return nominal transaksi; {@code 0.0} bila belum pernah diisi (nilai {@code null}
	 *         di-default-kan sekaligus disimpan sebagai efek samping getter).
	 */
	public Double getAmount() {
		if (amount == null) {
			amount = 0.0;
		}
		return amount;
	}

	/**
	 * Mengisi total nominal yang diminta untuk dibayarkan melalui Faspay.
	 *
	 * @param amount nominal yang akan diisi.
	 */
	public void setAmount(Double amount) {
		this.amount = amount;
	}

	/**
	 * Mengambil payload mentah (utuh, tanpa masking) permintaan yang dikirim ke API Faspay.
	 *
	 * <p><b>Catatan keamanan:</b> nilai ini berpotensi memuat data finansial transaksi (nominal,
	 * identitas pembayar, nomor VA/kanal) dalam bentuk mentah — lihat catatan keamanan pada
	 * javadoc kelas.</p>
	 *
	 * @return payload permintaan mentah, atau {@code null} bila belum diisi.
	 */
	@Column(columnDefinition = "text")
	public String getRequest() {
		return request;
	}

	/**
	 * Mengisi payload mentah permintaan yang dikirim ke API Faspay.
	 *
	 * @param request payload permintaan yang akan diisi.
	 */
	public void setRequest(String request) {
		this.request = request;
	}

	/**
	 * Mengambil ID merchant Faspay yang mengirimkan permintaan ini.
	 *
	 * @return ID merchant, atau {@code null} bila belum diisi.
	 */
	public String getMerchant_id() {
		return merchant_id;
	}

	/**
	 * Mengisi ID merchant Faspay yang mengirimkan permintaan ini.
	 *
	 * @param merchant_id ID merchant yang akan diisi.
	 */
	public void setMerchant_id(String merchant_id) {
		this.merchant_id = merchant_id;
	}

	// public String getBill_no() {
	// return bill_no;
	// }
	//
	// public void setBill_no(String bill_no) {
	// this.bill_no = bill_no;
	// }
	//
	// public String getBill_reff() {
	// return bill_reff;
	// }
	//
	// public void setBill_reff(String bill_reff) {
	// this.bill_reff = bill_reff;
	// }
	//
	// public Date getBill_date() {
	// return bill_date;
	// }
	//
	// public void setBill_date(Date bill_date) {
	// this.bill_date = bill_date;
	// }
	//
	// public Date getBill_expired() {
	// return bill_expired;
	// }
	//
	// public void setBill_expired(Date bill_expired) {
	// this.bill_expired = bill_expired;
	// }
	//
	// public String getBill_desc() {
	// return bill_desc;
	// }
	//
	// public void setBill_desc(String bill_desc) {
	// this.bill_desc = bill_desc;
	// }
	//
	// public String getBill_currency() {
	// return bill_currency;
	// }
	//
	// public void setBill_currency(String bill_currency) {
	// this.bill_currency = bill_currency;
	// }
	//
	// public Double getBill_gross() {
	// return bill_gross;
	// }
	//
	// public void setBill_gross(Double bill_gross) {
	// this.bill_gross = bill_gross;
	// }
	//
	// public Double getBill_tax() {
	// return bill_tax;
	// }
	//
	// public void setBill_tax(Double bill_tax) {
	// this.bill_tax = bill_tax;
	// }
	//
	// public Double getBill_miscfee() {
	// return bill_miscfee;
	// }
	//
	// public void setBill_miscfee(Double bill_miscfee) {
	// this.bill_miscfee = bill_miscfee;
	// }
	//
	// public Double getBill_total() {
	// return bill_total;
	// }
	//
	// public void setBill_total(Double bill_total) {
	// this.bill_total = bill_total;
	// }
	//
	// public String getCust_no() {
	// return cust_no;
	// }
	//
	// public void setCust_no(String cust_no) {
	// this.cust_no = cust_no;
	// }
	//
	// public String getCust_name() {
	// return cust_name;
	// }
	//
	// public void setCust_name(String cust_name) {
	// this.cust_name = cust_name;
	// }
	//
	// public String getPayment_channel() {
	// return payment_channel;
	// }
	//
	// public void setPayment_channel(String payment_channel) {
	// this.payment_channel = payment_channel;
	// }
	//
	// public String getPay_type() {
	// return pay_type;
	// }
	//
	// public void setPay_type(String pay_type) {
	// this.pay_type = pay_type;
	// }
	//
	// public String getBank_userid() {
	// return bank_userid;
	// }
	//
	// public void setBank_userid(String bank_userid) {
	// this.bank_userid = bank_userid;
	// }
	//
	// public String getMsisdn() {
	// return msisdn;
	// }
	//
	// public void setMsisdn(String msisdn) {
	// this.msisdn = msisdn;
	// }
	//
	// public String getEmail() {
	// return email;
	// }
	//
	// public void setEmail(String email) {
	// this.email = email;
	// }
	//
	// public String getTerminal() {
	// return terminal;
	// }
	//
	// public void setTerminal(String terminal) {
	// this.terminal = terminal;
	// }
	//
	// public String getBilling_address() {
	// return billing_address;
	// }
	//
	// public void setBilling_address(String billing_address) {
	// this.billing_address = billing_address;
	// }
	//
	// public String getBilling_address_city() {
	// return billing_address_city;
	// }
	//
	// public void setBilling_address_city(String billing_address_city) {
	// this.billing_address_city = billing_address_city;
	// }
	//
	// public String getBilling_address_region() {
	// return billing_address_region;
	// }
	//
	// public void setBilling_address_region(String billing_address_region) {
	// this.billing_address_region = billing_address_region;
	// }
	//
	// public String getBilling_address_state() {
	// return billing_address_state;
	// }
	//
	// public void setBilling_address_state(String billing_address_state) {
	// this.billing_address_state = billing_address_state;
	// }
	//
	// public String getBilling_address_poscode() {
	// return billing_address_poscode;
	// }
	//
	// public void setBilling_address_poscode(String billing_address_poscode) {
	// this.billing_address_poscode = billing_address_poscode;
	// }
	//
	// public String getBilling_address_country_code() {
	// return billing_address_country_code;
	// }
	//
	// public void setBilling_address_country_code(String
	// billing_address_country_code) {
	// this.billing_address_country_code = billing_address_country_code;
	// }
	//
	// public String getReceiver_name_for_shipping() {
	// return receiver_name_for_shipping;
	// }
	//
	// public void setReceiver_name_for_shipping(String
	// receiver_name_for_shipping) {
	// this.receiver_name_for_shipping = receiver_name_for_shipping;
	// }
	//
	// public String getShipping_address() {
	// return shipping_address;
	// }
	//
	// public void setShipping_address(String shipping_address) {
	// this.shipping_address = shipping_address;
	// }
	//
	// public String getShipping_address_city() {
	// return shipping_address_city;
	// }
	//
	// public void setShipping_address_city(String shipping_address_city) {
	// this.shipping_address_city = shipping_address_city;
	// }
	//
	// public String getShipping_address_region() {
	// return shipping_address_region;
	// }
	//
	// public void setShipping_address_region(String shipping_address_region) {
	// this.shipping_address_region = shipping_address_region;
	// }
	//
	// public String getShipping_address_state() {
	// return shipping_address_state;
	// }
	//
	// public void setShipping_address_state(String shipping_address_state) {
	// this.shipping_address_state = shipping_address_state;
	// }
	//
	// public String getShipping_address_poscode() {
	// return shipping_address_poscode;
	// }
	//
	// public void setShipping_address_poscode(String shipping_address_poscode)
	// {
	// this.shipping_address_poscode = shipping_address_poscode;
	// }

	/**
	 * Mengambil nilai signature/MAC transaksi yang dikirim ke/diterima dari Faspay.
	 *
	 * <p><b>Catatan keamanan:</b> nilai signature biasanya merupakan hash yang melibatkan
	 * kredensial merchant Faspay (mis. password/merchant key); disimpan mentah tanpa masking di
	 * kolom ini.</p>
	 *
	 * @return nilai signature, atau {@code null} bila belum diisi.
	 */
	public String getSignature() {
		return signature;
	}

	/**
	 * Mengisi nilai signature/MAC transaksi.
	 *
	 * @param signature nilai signature yang akan diisi.
	 */
	public void setSignature(String signature) {
		this.signature = signature;
	}

	/**
	 * Mengambil kode respons mentah dari Faspay atas permintaan ini.
	 *
	 * @return kode respons, atau {@code null} bila belum diisi.
	 */
	public String getResponse_code() {
		return response_code;
	}

	/**
	 * Mengisi kode respons mentah dari Faspay.
	 *
	 * @param response_code kode respons yang akan diisi.
	 */
	public void setResponse_code(String response_code) {
		this.response_code = response_code;
	}

	/**
	 * Mengambil deskripsi/pesan respons mentah dari Faspay atas permintaan ini.
	 *
	 * @return deskripsi respons, atau {@code null} bila belum diisi.
	 */
	public String getResponse_desc() {
		return response_desc;
	}

	/**
	 * Mengisi deskripsi/pesan respons mentah dari Faspay.
	 *
	 * @param response_desc deskripsi respons yang akan diisi.
	 */
	public void setResponse_desc(String response_desc) {
		this.response_desc = response_desc;
	}

	/**
	 * Mengambil payload mentah (utuh, tanpa masking) balasan/respons yang diterima dari API
	 * Faspay.
	 *
	 * <p><b>Catatan keamanan:</b> nilai ini berpotensi memuat data finansial transaksi hasil
	 * pemrosesan (status, nominal, referensi bank) dalam bentuk mentah — lihat catatan keamanan
	 * pada javadoc kelas.</p>
	 *
	 * @return payload respons mentah, atau {@code null} bila belum diisi.
	 */
	@Column(columnDefinition = "text")
	public String getResponse() {
		return response;
	}

	/**
	 * Mengisi payload mentah balasan/respons yang diterima dari API Faspay.
	 *
	 * @param response payload respons yang akan diisi.
	 */
	public void setResponse(String response) {
		this.response = response;
	}

	/**
	 * Mengambil nomor tagihan (bill number) yang dikirim ke Faspay.
	 *
	 * @return nomor tagihan, atau {@code null} bila belum diisi.
	 */
	public String getBillNo() {
		return billNo;
	}

	/**
	 * Mengisi nomor tagihan (bill number) yang dikirim ke Faspay.
	 *
	 * @param billNo nomor tagihan yang akan diisi.
	 */
	public void setBillNo(String billNo) {
		this.billNo = billNo;
	}

	/**
	 * Mengambil status pemrosesan transaksi.
	 *
	 * @return status transaksi; {@code "Belum diproses"} bila belum pernah diisi (nilai {@code
	 *         null} di-default-kan sekaligus disimpan sebagai efek samping getter).
	 */
	public String getStatus() {
		if (status == null) {
			status = "Belum diproses";
		}
		return status;
	}

	/**
	 * Mengisi status pemrosesan transaksi.
	 *
	 * @param status status yang akan diisi.
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Mengambil kode status numerik/mentah dari hasil pemrosesan transaksi.
	 *
	 * @return kode status, atau {@code null} bila belum diisi.
	 */
	public String getKodeStatus() {
		return kodeStatus;
	}

	/**
	 * Mengisi kode status numerik/mentah dari hasil pemrosesan transaksi.
	 *
	 * @param kodeStatus kode status yang akan diisi.
	 */
	public void setKodeStatus(String kodeStatus) {
		this.kodeStatus = kodeStatus;
	}

	/**
	 * Mengambil nama kanal pembayaran yang dipilih/digunakan pengguna.
	 *
	 * @return nama kanal pembayaran; {@code "Faspay"} bila belum pernah diisi (nilai {@code
	 *         null} di-default-kan sekaligus disimpan sebagai efek samping getter).
	 */
	public String getPayment_channel_name() {
		if (payment_channel_name == null) {
			payment_channel_name = "Faspay";
		}
		return payment_channel_name;
	}

	/**
	 * Mengisi nama kanal pembayaran yang dipilih/digunakan pengguna.
	 *
	 * @param payment_channel_name nama kanal pembayaran yang akan diisi.
	 */
	public void setPayment_channel_name(String payment_channel_name) {
		this.payment_channel_name = payment_channel_name;
	}

	/**
	 * Mengambil penanda apakah cicilan sebelumnya yang belum lunas harus dihapus saat permintaan
	 * baru dibuat.
	 *
	 * @return {@code true} (default bila belum pernah diisi) atau {@code false}.
	 */
	public Boolean getHapusCicilanSebelumnya() {
		if (hapusCicilanSebelumnya == null) {
			hapusCicilanSebelumnya = true;
		}
		return hapusCicilanSebelumnya;
	}

	/**
	 * Mengisi penanda apakah cicilan sebelumnya yang belum lunas harus dihapus.
	 *
	 * @param hapusCicilanSebelumnya nilai penanda yang akan diisi.
	 */
	public void setHapusCicilanSebelumnya(Boolean hapusCicilanSebelumnya) {
		this.hapusCicilanSebelumnya = hapusCicilanSebelumnya;
	}

	/**
	 * Mengambil penanda apakah status transaksi perlu dicek ulang ke Faspay.
	 *
	 * @return {@code false} (default bila belum pernah diisi) atau {@code true}.
	 */
	public Boolean getCheckUlang() {
		if (checkUlang == null) {
			checkUlang = false;
		}
		return checkUlang;
	}

	/**
	 * Mengisi penanda apakah status transaksi perlu dicek ulang ke Faspay.
	 *
	 * @param checkUlang nilai penanda yang akan diisi.
	 */
	public void setCheckUlang(Boolean checkUlang) {
		this.checkUlang = checkUlang;
	}

	/**
	 * Mengambil biaya administrasi yang dibebankan untuk transaksi ini. Bila belum pernah diisi
	 * secara signifikan (kurang dari {@code 0.01}), nilainya diambil dari konfigurasi sistem
	 * {@code faspay_biaya_administrasi} (via {@code Common.getKonfigurasi}) dan disimpan sebagai
	 * efek samping getter; kegagalan parsing konfigurasi ditangkap dan dicatat ke {@code
	 * ErrorAuditUtil} tanpa melempar exception ke pemanggil.
	 *
	 * @return biaya administrasi; {@code 0.0} bila field maupun konfigurasi tidak tersedia.
	 */
	@Column(name = "biaya_administrasi")
	public Double getBiayaAdministrasi() {
		if (biayaAdministrasi == null || biayaAdministrasi < 0.01) {
			try {
				biayaAdministrasi = Double
						.parseDouble(Common.getKonfigurasi("faspay_biaya_administrasi", "0.0").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/faspay/FaspayRequest.java:661");

			}
		}
		return biayaAdministrasi == null ? 0.0 : biayaAdministrasi;
	}

	/**
	 * Mengisi biaya administrasi yang dibebankan untuk transaksi ini secara eksplisit.
	 *
	 * @param biayaAdministrasi biaya administrasi yang akan diisi.
	 */
	public void setBiayaAdministrasi(Double biayaAdministrasi) {
		this.biayaAdministrasi = biayaAdministrasi;
	}

	/**
	 * Mengambil biaya payment gateway (fee Faspay) yang dibebankan untuk transaksi ini. Bila
	 * belum pernah diisi secara signifikan (kurang dari {@code 0.01}), nilainya diambil dari
	 * konfigurasi sistem {@code faspay_biaya_payment_gateway} (via {@code Common.getKonfigurasi})
	 * dan disimpan sebagai efek samping getter; kegagalan parsing konfigurasi ditangkap dan
	 * dicatat ke {@code ErrorAuditUtil} tanpa melempar exception ke pemanggil.
	 *
	 * @return biaya payment gateway; {@code 0.0} bila field maupun konfigurasi tidak tersedia.
	 */
	@Column(name = "biaya_payment_gateway")
	public Double getBiayaPaymentGateway() {
		if (biayaPaymentGateway == null || biayaPaymentGateway < 0.01) {
			try {
				biayaPaymentGateway = Double
						.parseDouble(Common.getKonfigurasi("faspay_biaya_payment_gateway", "0.0").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/faspay/FaspayRequest.java:678");

			}
		}
		return biayaPaymentGateway == null ? 0.0 : biayaPaymentGateway;
	}

	/**
	 * Mengisi biaya payment gateway (fee Faspay) yang dibebankan untuk transaksi ini secara
	 * eksplisit.
	 *
	 * @param biayaPaymentGateway biaya payment gateway yang akan diisi.
	 */
	public void setBiayaPaymentGateway(Double biayaPaymentGateway) {
		this.biayaPaymentGateway = biayaPaymentGateway;
	}
}
