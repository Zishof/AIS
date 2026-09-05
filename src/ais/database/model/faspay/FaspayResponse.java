package ais.database.model.faspay;

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
 * Entity JPA/Hibernate balasan/callback dari payment gateway <b>Faspay</b> untuk satu transaksi.
 * Satu baris {@code faspay_response} ditautkan balik ke {@link FaspayRequest} lewat {@link
 * FaspayRequest#getFaspayResponse()} begitu Faspay mengirim notifikasi pembayaran (ditangani oleh
 * {@code ais.action.servlet.FasPayResponse}).
 *
 * <p>Lihat javadoc kelas {@link FaspayRequest} untuk penjelasan pola arsitektur umum 4-entity
 * (Request/RequestDetail/RequestDetailBiaya/Response) yang dipakai di semua gateway H2H AIS, serta
 * catatan keamanan mengenai {@link #getCallback()} yang menyimpan payload callback mentah dari
 * Faspay tanpa masking.</p>
 *
 * @see FaspayRequest
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "faspay_response")



public class FaspayResponse extends GeneralValueObject {

	/** Kode status: transaksi sedang menunggu pembayaran. */
	public final static String PENDING = "pending";
	/** Kode status: transaksi berhasil dibayar. */
	public final static String BERHASIL = "berhasil";
	/** Label status: transaksi sedang dalam proses verifikasi. */
	public final static String SEDANG_DIPROSES = "Sedang diproses";
	/** Label status: transaksi dibatalkan. */
	public final static String BATAL = "Batal";
	/** Label status: transaksi dikembalikan dananya (refund). */
	public final static String REFUND = "Refund";

	/**
	 * Versi serialisasi tetap untuk kompatibilitas antar build ({@link java.io.Serializable}).
	 */
	private static final long serialVersionUID = 2463821327548439808L;
	/** Primary key auto-increment (identity) baris respons ini. */
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
	 * @return string ringkas berisi {@link #getId()} dan {@link #getNama()}.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Session ID (SID) yang diberikan Faspay untuk transaksi ini; dipetakan ke kolom {@code sid}. */
	private String nama;
	/** Status pemrosesan hasil dari Faspay. */
	private String status;
	/** Kode status numerik/mentah dari hasil pemrosesan Faspay. */
	private String kodeStatus;
	/** ID transaksi (trxId) yang menjadi acuan pencocokan balik ke {@link FaspayRequest}. */
	private String trxId;
	/** ID/nama merchant Faspay yang memproses transaksi ini. */
	private String merchant;
	/** Keterangan tambahan mengenai hasil pemrosesan dari Faspay. */
	private String keterangan;
	/** Payload callback mentah (utuh, tanpa masking) yang diterima dari Faspay. */
	private String callback;

	/**
	 * Konstruktor default (dibutuhkan Hibernate).
	 */
	public FaspayResponse() {
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
	 * Mengambil session ID (SID) dari Faspay, dengan whitespace di-trim. Dipetakan ke kolom
	 * {@code sid}.
	 *
	 * @return SID yang sudah di-trim, atau {@code null} bila belum diisi.
	 */
	@Column(name = "sid", columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi session ID (SID) dari Faspay.
	 *
	 * @param nama SID yang akan diisi.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil ID transaksi (trxId) acuan pencocokan balik ke {@link FaspayRequest}.
	 *
	 * @return trxId, atau {@code null} bila belum diisi.
	 */
	public String getTrxId() {
		return trxId;
	}

	/**
	 * Mengisi ID transaksi (trxId) acuan pencocokan balik ke {@link FaspayRequest}.
	 *
	 * @param trxId trxId yang akan diisi.
	 */
	public void setTrxId(String trxId) {
		this.trxId = trxId;
	}

	/**
	 * Mengambil ID/nama merchant Faspay yang memproses transaksi ini.
	 *
	 * @return ID/nama merchant, atau {@code null} bila belum diisi.
	 */
	public String getMerchant() {
		return merchant;
	}

	/**
	 * Mengisi ID/nama merchant Faspay yang memproses transaksi ini.
	 *
	 * @param merchant ID/nama merchant yang akan diisi.
	 */
	public void setMerchant(String merchant) {
		this.merchant = merchant;
	}

	/**
	 * Mengambil status pemrosesan hasil dari Faspay.
	 *
	 * @return status; string kosong bila belum pernah diisi (nilai {@code null} di-default-kan
	 *         sekaligus disimpan sebagai efek samping getter).
	 */
	public String getStatus() {
		if (status == null) {
			status = "";
		}
		return status;
	}

	/**
	 * Mengisi status pemrosesan hasil dari Faspay.
	 *
	 * @param status status yang akan diisi.
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Mengambil keterangan tambahan mengenai hasil pemrosesan dari Faspay.
	 *
	 * @return keterangan; string kosong bila belum pernah diisi (nilai {@code null}
	 *         di-default-kan sekaligus disimpan sebagai efek samping getter).
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		if (keterangan == null) {
			keterangan = "";
		}
		return keterangan;
	}

	/**
	 * Mengisi keterangan tambahan mengenai hasil pemrosesan dari Faspay.
	 *
	 * @param keterangan keterangan yang akan diisi.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengambil payload callback mentah (utuh, tanpa masking) yang diterima dari Faspay.
	 *
	 * <p><b>Catatan keamanan:</b> nilai ini berpotensi memuat data finansial hasil callback
	 * (status, nominal, referensi bank/VA) dalam bentuk mentah — pola serupa dengan payload
	 * mentah pada {@link FaspayRequest#getRequest()}/{@link FaspayRequest#getResponse()}; lihat
	 * catatan keamanan pada javadoc kelas {@link FaspayRequest}.</p>
	 *
	 * @return payload callback mentah, atau {@code null} bila belum diisi.
	 */
	@Column(columnDefinition = "text")
	public String getCallback() {
		return callback;
	}

	/**
	 * Mengisi payload callback mentah yang diterima dari Faspay.
	 *
	 * @param callback payload callback yang akan diisi.
	 */
	public void setCallback(String callback) {
		this.callback = callback;
	}

	/**
	 * Mengambil kode status numerik/mentah dari hasil pemrosesan Faspay.
	 *
	 * @return kode status; {@code "0"} bila belum pernah diisi (nilai {@code null}
	 *         di-default-kan sekaligus disimpan sebagai efek samping getter).
	 */
	public String getKodeStatus() {
		if (kodeStatus == null) {
			kodeStatus = "0";
		}
		return kodeStatus;
	}

	/**
	 * Mengisi kode status numerik/mentah dari hasil pemrosesan Faspay.
	 *
	 * @param kodeStatus kode status yang akan diisi.
	 */
	public void setKodeStatus(String kodeStatus) {
		this.kodeStatus = kodeStatus;
	}

}
