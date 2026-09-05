package ais.database.model.doku;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
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



import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.GeneralValueObject;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.Mahasiswa;

/**
 * Entity JPA/Hibernate header transaksi permintaan pembayaran host-to-host (H2H) via payment
 * gateway <b>Doku</b> untuk pembayaran biaya mahasiswa, calon mahasiswa, atau peserta kegiatan.
 * Baris {@code doku_request} dibuat sistem ketika pengguna memulai pembayaran, ditautkan opsional
 * ke {@link Mahasiswa} atau {@link BiodataCalonMahasiswa}, {@link JenisKegiatan}, dan {@link
 * JadwalPembayaran}, lalu direlasikan satu-ke-satu (opsional, diisi belakangan) ke {@link
 * DokuResponse} begitu balasan/callback dari Doku diterima (lihat {@code
 * ais.action.servlet.DokuResponseServlet} dan {@code ais.action.servlet.DokuVerifyServlet}).
 * Rincian item biaya yang ditagihkan disimpan terpisah pada {@code
 * ais.database.model.doku.DokuRequestDetail} (per pos tagihan) dan {@code
 * ais.database.model.doku.DokuRequestDetailBiaya} (per komponen biaya administrasi/fee).
 *
 * <p><b>Pola arsitektur berulang:</b> kelas ini mengikuti pola generik 4-entity (Request/
 * RequestDetail/RequestDetailBiaya/Response) yang identik strukturnya di lebih dari 9 paket model
 * integrasi bank/payment-gateway berbeda di AIS (mis. {@code ais.database.model.bni.BniRequest}
 * untuk Bank BNI, {@code ais.database.model.cimb.CimbRequest} untuk CIMB Niaga). Nama kolom dan
 * makna field pada umumnya sama, namun kelas per gateway tetap dapat memiliki field spesifik
 * gateway tersebut (lihat javadoc tiap field di bawah).</p>
 *
 * <p><b>Catatan keamanan:</b> seperti {@code CimbRequest}, entity ini <i>tidak</i> menyimpan
 * payload request/response mentah dari Doku (tidak ada field {@code request}/{@code response}
 * seperti pada BNI/Faspay) — hanya identifier transaksi ({@link #getTrxId()}) dan atribut ringkas
 * hasil transaksi ({@link #getProduct()}, {@link #getMerchant()}, {@link #getBuyer()}, {@link
 * #getNoRekeningDeposit()}). Field {@link #getNoRekeningDeposit()} menyimpan nomor rekening bank
 * tujuan deposit/refund secara <b>mentah tanpa masking</b> — nilai ini diproyeksikan dari {@link
 * DokuResponse} sehingga sama-sama tersimpan tanpa masking di kedua entity. Payload H2H
 * sesungguhnya (respons callback mentah dari Doku) ditangani terpisah oleh servlet {@code
 * ais.action.servlet.DokuResponseServlet}, yang diketahui menulis data respons ke stdout server
 * (System.out.println, lihat {@code "==> DokuResponseServlet data => " + data}) tanpa masking —
 * pola paparan data finansial serupa (walau kanal berbeda: servlet HTTP callback, bukan grid log
 * ZK) dengan temuan tercatat pada {@code LogHostToHostAction} untuk Bank Mandiri/OCBC NISP.</p>
 *
 * @see DokuResponse
 * @see ais.database.model.bni.BniRequest
 * @see ais.database.model.cimb.CimbRequest
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "doku_request")



public class DokuRequest extends GeneralValueObject {
	/**
	 * Versi serialisasi tetap untuk kompatibilitas antar build ({@link java.io.Serializable}).
	 */
	private static final long serialVersionUID = 2463821327548439808L;
	/** Primary key auto-increment (identity) baris permintaan Doku ini. */
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
	/** ID transaksi (trxId) yang dikirim ke/diterima dari Doku sebagai identifier unik transaksi. */
	private String trxId;
	/** Nama produk/layanan Doku yang dipakai untuk transaksi ini; diproyeksikan dari {@link
	 * #dokuResponse} bila sudah tersedia. */
	private String product;
	/** ID merchant Doku yang memproses transaksi ini; diproyeksikan dari {@link #dokuResponse}
	 * bila sudah tersedia. */
	private String merchant;
	/** Identitas pembeli (buyer) menurut Doku; diproyeksikan dari {@link #dokuResponse} bila
	 * sudah tersedia. */
	private String buyer;
	/** Nomor rekening bank tujuan deposit/refund; disimpan mentah tanpa masking, diproyeksikan
	 * dari {@link #dokuResponse} bila sudah tersedia. */
	private String noRekeningDeposit;
	/** Komentar/catatan tambahan dari Doku mengenai transaksi ini. */
	private String comments;
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
	/** Total nominal yang diminta untuk dibayarkan melalui Doku. */
	private Double amount;
	/** Total nilai biaya yang seharusnya dibayar sebelum potongan (nilai acuan, bukan nilai
	 * final tagihan). */
	private Double nilaiBiayaHarusDiBayars;
	/** Balasan/hasil dari Doku untuk permintaan ini, diisi setelah callback diterima. */
	private DokuResponse dokuResponse;

	/**
	 * Konstruktor default (dibutuhkan Hibernate).
	 */
	public DokuRequest() {
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
	 * Mengambil balasan/hasil dari Doku untuk permintaan ini.
	 *
	 * @return {@link DokuResponse} terkait, atau {@code null} bila callback belum diterima.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "doku_response", nullable = true)
	public DokuResponse getDokuResponse() {
		return dokuResponse;
	}

	/**
	 * Mengisi balasan/hasil dari Doku untuk permintaan ini.
	 *
	 * @param dokuResponse respons Doku yang akan ditautkan.
	 */
	public void setDokuResponse(DokuResponse dokuResponse) {
		this.dokuResponse = dokuResponse;
	}



	/**
	 * Mengambil nama produk/layanan Doku yang dipakai untuk transaksi ini. Bila {@link
	 * #dokuResponse} tersedia, nilainya <b>diambil-alih (overwrite)</b> dari {@link
	 * DokuResponse#getProduct()} sebagai efek samping getter, sehingga nilai yang dikembalikan
	 * selalu sinkron dengan respons terbaru dari Doku selama relasi tersebut sudah ter-load.
	 *
	 * @return nama produk; bisa {@code null} bila belum ada respons dan belum pernah diisi
	 *         manual.
	 */
	@Column(columnDefinition = "text")
	public String getProduct() {
		if (dokuResponse != null) {
			product = dokuResponse.getProduct();
		}
		return product;
	}

	/**
	 * Mengisi nama produk/layanan Doku secara eksplisit.
	 *
	 * @param product nama produk yang akan diisi.
	 */
	public void setProduct(String product) {
		this.product = product;
	}

	/**
	 * Mengambil ID merchant Doku yang memproses transaksi ini. Bila {@link #dokuResponse}
	 * tersedia, nilainya <b>diambil-alih (overwrite)</b> dari {@link DokuResponse#getMerchant()}
	 * sebagai efek samping getter.
	 *
	 * @return ID merchant; bisa {@code null} bila belum ada respons dan belum pernah diisi
	 *         manual.
	 */
	public String getMerchant() {
		if (dokuResponse != null) {
			merchant = dokuResponse.getMerchant();
		}
		return merchant;
	}

	/**
	 * Mengisi ID merchant Doku secara eksplisit.
	 *
	 * @param merchant ID merchant yang akan diisi.
	 */
	public void setMerchant(String merchant) {
		this.merchant = merchant;
	}

	/**
	 * Mengambil identitas pembeli (buyer) menurut Doku. Bila {@link #dokuResponse} tersedia,
	 * nilainya <b>diambil-alih (overwrite)</b> dari {@link DokuResponse#getBuyer()} sebagai efek
	 * samping getter; bila masih kosong setelah itu, di-default-kan ke string kosong (juga
	 * disimpan sebagai efek samping getter).
	 *
	 * @return identitas pembeli; tidak pernah {@code null}.
	 */
	public String getBuyer() {
		if (dokuResponse != null) {
			buyer = dokuResponse.getBuyer();
		}
		if (buyer == null) {
			buyer = "";
		}
		return buyer;
	}

	/**
	 * Mengisi identitas pembeli (buyer) secara eksplisit.
	 *
	 * @param buyer identitas pembeli yang akan diisi.
	 */
	public void setBuyer(String buyer) {
		this.buyer = buyer;
	}

	/**
	 * Mengambil nomor rekening bank tujuan deposit/refund. Bila {@link #dokuResponse} tersedia,
	 * nilainya <b>diambil-alih (overwrite)</b> dari {@link DokuResponse#getNoRekeningDeposit()}
	 * sebagai efek samping getter; bila masih kosong setelah itu, di-default-kan ke string kosong
	 * (juga disimpan sebagai efek samping getter). Nilai ini tersimpan mentah tanpa masking.
	 *
	 * @return nomor rekening deposit; tidak pernah {@code null}.
	 */
	public String getNoRekeningDeposit() {
		if (dokuResponse != null) {
			noRekeningDeposit = dokuResponse.getNoRekeningDeposit();
		}
		if (noRekeningDeposit == null) {
			noRekeningDeposit = "";
		}
		return noRekeningDeposit;
	}

	/**
	 * Mengisi nomor rekening bank tujuan deposit/refund secara eksplisit.
	 *
	 * @param noRekeningDeposit nomor rekening yang akan diisi.
	 */
	public void setNoRekeningDeposit(String noRekeningDeposit) {
		this.noRekeningDeposit = noRekeningDeposit;
	}

	/**
	 * Mengambil komentar/catatan tambahan dari Doku mengenai transaksi ini.
	 *
	 * @return komentar, atau {@code null} bila belum diisi.
	 */
	@Column(columnDefinition = "text")
	public String getComments() {
		return comments;
	}

	/**
	 * Mengisi komentar/catatan tambahan dari Doku mengenai transaksi ini.
	 *
	 * @param comments komentar yang akan diisi.
	 */
	public void setComments(String comments) {
		this.comments = comments;
	}

	/**
	 * Mengambil total nominal yang diminta untuk dibayarkan melalui Doku.
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
	 * Mengisi total nominal yang diminta untuk dibayarkan melalui Doku.
	 *
	 * @param amount nominal yang akan diisi.
	 */
	public void setAmount(Double amount) {
		this.amount = amount;
	}
}
