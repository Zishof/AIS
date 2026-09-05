package ais.database.model.cimb;

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
 * Entity JPA/Hibernate balasan/callback dari payment gateway <b>CIMB Niaga</b> untuk satu
 * transaksi Virtual Account. Satu baris {@code cimb_response} ditautkan balik ke {@link
 * CimbRequest} lewat {@link CimbRequest#getCimbResponse()} begitu CIMB mengirim notifikasi
 * pembayaran (via web service {@code CIMB3rdParty.BillPaymentWS.BillPaymentServiceSoapImpl}) atau
 * hasil pengecekan status.
 *
 * <p>Lihat javadoc kelas {@link CimbRequest} untuk penjelasan pola arsitektur umum 4-entity
 * (Request/RequestDetail/RequestDetailBiaya/Response) yang dipakai di semua gateway H2H AIS, serta
 * catatan keamanan bahwa entity ini tidak menyimpan payload SOAP mentah — hanya status/keterangan
 * ringkas hasil pemrosesan.</p>
 *
 * @see CimbRequest
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "cimb_response")



public class CimbResponse extends GeneralValueObject {

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

	/** Session ID (SID) yang diberikan CIMB untuk transaksi ini; dipetakan ke kolom {@code sid}. */
	private String nama;
	/** Status pemrosesan hasil dari CIMB. */
	private String status;
	/** Kode status numerik/mentah dari hasil pemrosesan CIMB. */
	private String kodeStatus;
	/** ID transaksi (trxId) yang menjadi acuan pencocokan balik ke {@link CimbRequest}. */
	private String trxId;
	/** Keterangan tambahan mengenai hasil pemrosesan dari CIMB. */
	private String keterangan;

	/**
	 * Konstruktor default (dibutuhkan Hibernate).
	 */
	public CimbResponse() {
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
	 * Mengambil session ID (SID) dari CIMB, dengan whitespace di-trim. Dipetakan ke kolom
	 * {@code sid}.
	 *
	 * @return SID yang sudah di-trim, atau {@code null} bila belum diisi.
	 */
	@Column(name = "sid", columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi session ID (SID) dari CIMB.
	 *
	 * @param nama SID yang akan diisi.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil ID transaksi (trxId) acuan pencocokan balik ke {@link CimbRequest}.
	 *
	 * @return trxId, atau {@code null} bila belum diisi.
	 */
	public String getTrxId() {
		return trxId;
	}

	/**
	 * Mengisi ID transaksi (trxId) acuan pencocokan balik ke {@link CimbRequest}.
	 *
	 * @param trxId trxId yang akan diisi.
	 */
	public void setTrxId(String trxId) {
		this.trxId = trxId;
	}

	/**
	 * Mengambil status pemrosesan hasil dari CIMB.
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
	 * Mengisi status pemrosesan hasil dari CIMB.
	 *
	 * @param status status yang akan diisi.
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Mengambil keterangan tambahan mengenai hasil pemrosesan dari CIMB.
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
	 * Mengisi keterangan tambahan mengenai hasil pemrosesan dari CIMB.
	 *
	 * @param keterangan keterangan yang akan diisi.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengambil kode status numerik/mentah dari hasil pemrosesan CIMB.
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
	 * Mengisi kode status numerik/mentah dari hasil pemrosesan CIMB.
	 *
	 * @param kodeStatus kode status yang akan diisi.
	 */
	public void setKodeStatus(String kodeStatus) {
		this.kodeStatus = kodeStatus;
	}

}
