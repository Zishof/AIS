package ais.database.model.doku;

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
 * Entity JPA/Hibernate balasan/callback dari payment gateway <b>Doku</b> untuk satu transaksi.
 * Satu baris {@code doku_response} ditautkan balik ke {@link DokuRequest} lewat {@link
 * DokuRequest#getDokuResponse()} begitu Doku mengirim notifikasi pembayaran (ditangani oleh {@code
 * ais.action.servlet.DokuResponseServlet}) atau hasil verifikasi (ditangani oleh {@code
 * ais.action.servlet.DokuVerifyServlet}).
 *
 * <p>Lihat javadoc kelas {@link DokuRequest} untuk penjelasan pola arsitektur umum 4-entity
 * (Request/RequestDetail/RequestDetailBiaya/Response) yang dipakai di semua gateway H2H AIS, serta
 * catatan keamanan mengenai {@link #getNoRekeningDeposit()} yang tersimpan mentah tanpa masking
 * dan paparan payload callback via {@code System.out.println} di {@code DokuResponseServlet}.</p>
 *
 * @see DokuRequest
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "doku_response")



public class DokuResponse extends GeneralValueObject {


	/** Label status: transaksi berhasil diproses oleh Doku. */
	public final static String BERHASIL = "Success";

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
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}

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

	/** Session ID (SID) yang diberikan Doku untuk transaksi ini; dipetakan ke kolom {@code sid}. */
	private String nama;
	/** Status pemrosesan hasil dari Doku. */
	private String status;
	/** ID transaksi (trxId) yang menjadi acuan pencocokan balik ke {@link DokuRequest}. */
	private String trxId;
	/** Nama produk/layanan Doku yang dipakai untuk transaksi ini. */
	private String product;
	/** ID merchant Doku yang memproses transaksi ini. */
	private String merchant;
	/** Identitas pembeli (buyer) menurut Doku. */
	private String buyer;
	/** Nomor rekening bank tujuan deposit/refund; disimpan mentah tanpa masking. */
	private String noRekeningDeposit;
	/** Komentar/catatan tambahan dari Doku mengenai transaksi ini. */
	private String comments;
	/** Keterangan tambahan mengenai hasil pemrosesan dari Doku. */
	private String keterangan;

	/**
	 * Konstruktor default (dibutuhkan Hibernate).
	 */
	public DokuResponse() {
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
	 * Mengambil session ID (SID) dari Doku, dengan whitespace di-trim. Dipetakan ke kolom
	 * {@code sid}.
	 *
	 * @return SID yang sudah di-trim, atau {@code null} bila belum diisi.
	 */
	@Column(name = "sid", columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi session ID (SID) dari Doku.
	 *
	 * @param nama SID yang akan diisi.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil ID transaksi (trxId) acuan pencocokan balik ke {@link DokuRequest}.
	 *
	 * @return trxId, atau {@code null} bila belum diisi.
	 */
	public String getTrxId() {
		return trxId;
	}

	/**
	 * Mengisi ID transaksi (trxId) acuan pencocokan balik ke {@link DokuRequest}.
	 *
	 * @param trxId trxId yang akan diisi.
	 */
	public void setTrxId(String trxId) {
		this.trxId = trxId;
	}

	/**
	 * Mengambil nama produk/layanan Doku yang dipakai untuk transaksi ini.
	 *
	 * @return nama produk, atau {@code null} bila belum diisi.
	 */
	@Column(columnDefinition = "text")
	public String getProduct() {
		return product;
	}

	/**
	 * Mengisi nama produk/layanan Doku yang dipakai untuk transaksi ini.
	 *
	 * @param product nama produk yang akan diisi.
	 */
	public void setProduct(String product) {
		this.product = product;
	}

	/**
	 * Mengambil ID merchant Doku yang memproses transaksi ini.
	 *
	 * @return ID merchant, atau {@code null} bila belum diisi.
	 */
	public String getMerchant() {
		return merchant;
	}

	/**
	 * Mengisi ID merchant Doku yang memproses transaksi ini.
	 *
	 * @param merchant ID merchant yang akan diisi.
	 */
	public void setMerchant(String merchant) {
		this.merchant = merchant;
	}

	/**
	 * Mengambil identitas pembeli (buyer) menurut Doku.
	 *
	 * @return identitas pembeli, atau {@code null} bila belum diisi.
	 */
	public String getBuyer() {
		return buyer;
	}

	/**
	 * Mengisi identitas pembeli (buyer) menurut Doku.
	 *
	 * @param buyer identitas pembeli yang akan diisi.
	 */
	public void setBuyer(String buyer) {
		this.buyer = buyer;
	}

	/**
	 * Mengambil nomor rekening bank tujuan deposit/refund. Nilai ini tersimpan mentah tanpa
	 * masking.
	 *
	 * @return nomor rekening deposit, atau {@code null} bila belum diisi.
	 */
	public String getNoRekeningDeposit() {
		return noRekeningDeposit;
	}

	/**
	 * Mengisi nomor rekening bank tujuan deposit/refund.
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
	 * Mengambil status pemrosesan hasil dari Doku.
	 *
	 * @return status; string kosong bila belum pernah diisi (nilai {@code null}
	 *         di-default-kan sekaligus disimpan sebagai efek samping getter).
	 */
	public String getStatus() {
		if (status == null) {
			status = "";
		}
		return status;
	}

	/**
	 * Mengisi status pemrosesan hasil dari Doku.
	 *
	 * @param status status yang akan diisi.
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Mengambil keterangan tambahan mengenai hasil pemrosesan dari Doku.
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
	 * Mengisi keterangan tambahan mengenai hasil pemrosesan dari Doku.
	 *
	 * @param keterangan keterangan yang akan diisi.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

}
