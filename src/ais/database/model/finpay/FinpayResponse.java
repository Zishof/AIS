package ais.database.model.finpay;

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
 * Entity Hibernate untuk balasan (response) dari payment gateway <b>Finpay</b> atas sebuah
 * {@link FinpayRequest}, dipetakan ke tabel {@code finpay_response}. Dibangkitkan oleh hbm2java, lalu
 * dilengkapi field tambahan secara manual.
 *
 * <p>Kelas ini adalah kelas <i>balasan gateway</i> pada pola 4-kelas per payment gateway yang berulang
 * identik di seluruh integrasi H2H AIS (lihat {@link FinpayRequest} untuk penjelasan pola lengkap).
 * Satu baris entity ini merepresentasikan balasan Finpay untuk satu request; relasi ke request pemilik
 * disimpan sebaliknya, lewat {@code FinpayRequest.getFinpayResponse()}.</p>
 *
 * <p><b>Catatan keamanan:</b> tidak ada field kartu/PIN/password di kelas ini, dan tidak seperti
 * {@code BniResponse}/{@code JatelindoResponse} entity ini tidak menyimpan payload mentah gabungan
 * (tidak ada kolom {@code response}/{@code callback} berisi seluruh balasan gateway apa adanya) --
 * data Finpay disimpan terurai per kolom. Namun field {@link #getNama()} tetap memetakan ke kolom fisik
 * {@code mer_signature} (BUKAN nama orang), menyimpan signature balasan Finpay apa adanya (plaintext).
 * Servlet {@code ais.action.servlet.FinPayResponse} mencatat balasan Finpay ke tabel
 * {@code log_host_to_host} yang sama dipakai H2H Bank Mandiri/OCBC NISP -- perluasan cakupan temuan
 * {@code LogHostToHostAction.java} (payload H2H tereskspos) ke gateway ini, bukan celah baru.</p>
 *
 * @see FinpayRequest
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "finpay_response")



public class FinpayResponse extends GeneralValueObject {
	/** 
	 * 
	 */
	private static final long serialVersionUID = 2463821327548439808L;
	private Long id;
	/** Nama pengguna (username) yang membuat/terakhir menyentuh baris audit ini. */
	private String oleh;
	/** Id pengguna yang membuat/terakhir menyentuh baris audit ini; pasangan dari {@link #oleh}. */
	private String olehId;

	/**
	 * @return id pengguna (audit) yang tercatat pada baris ini, sebagaimana adanya (tanpa fallback).
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengeset id pengguna audit. Nilai {@code null} atau string kosong/blank diabaikan (fail-safe)
	 * agar id pengguna audit yang sudah tersimpan tidak tertimpa nilai kosong.
	 * @param olehId id pengguna audit baru; diabaikan jika null/kosong.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengeset nama pengguna audit ({@link #oleh}). Nilai {@code null} atau kosong/blank diabaikan
	 * (fail-safe) supaya nama pengguna audit yang sudah tersimpan tidak tertimpa nilai kosong.
	 * @param oleh nama pengguna audit baru; diabaikan jika null/kosong.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna (audit) yang tercatat pada baris ini, sebagaimana adanya (tanpa fallback).
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: memperbarui {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} setiap kali baris ini
	 * di-{@code UPDATE}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah timestamp "terakhir diubah" baru untuk baris ini.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return timestamp terakhir baris ini diubah (kolom audit, diperbarui otomatis oleh
	 * {@link #onUpdate()} pada setiap {@code UPDATE}).
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return representasi ringkas entity ini untuk keperluan log/debug: {@code id-nama} (nama di sini
	 * adalah signature merchant mentah dari kolom {@code mer_signature}, lihat {@link #getNama()}).
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Signature/tanda tangan merchant Finpay mentah pada balasan ini; lihat {@link #getNama()}. */
	private String nama;
	/** Tipe/jenis transaksi menurut balasan Finpay. */
	private String tipe;
	/** Kode merchant Finpay pada balasan ini. */
	private String merchant;
	/** Nomor invoice/tagihan yang dirujuk balasan ini. */
	private String invoice;
	/** Kode pembayaran (payment code/kode VA) yang diterbitkan Finpay pada balasan ini. */
	private String paymentCode;
	/** Kode hasil (result code) transaksi menurut balasan Finpay. */
	private String resultCode;
	/** Deskripsi/keterangan hasil transaksi menurut balasan Finpay. */
	private String resultDesc;
	/** Nomor log internal Finpay untuk transaksi ini. */
	private String logNo;
	/** Sumber/kanal pembayaran yang digunakan (mis. metode/channel bank) menurut balasan Finpay. */
	private String paymentSource;

	/**
	 * Konstruktor default (dipakai Hibernate).
	 */
	public FinpayResponse() {
	}

	/**
	 * @return id unik (primary key, auto-increment) baris {@code finpay_response} ini.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id id baris ini; normalnya tidak diset manual karena kolom bersifat
	 * {@code insertable = false} (auto-increment oleh database).
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return signature/tanda tangan merchant Finpay pada balasan ini (kolom fisik
	 * {@code mer_signature}), sudah di-{@code trim()}, atau {@code null} jika belum diset. Nama getter
	 * ini menyesatkan (bukan nama orang) -- lihat catatan keamanan pada Javadoc kelas.
	 */
	@Column(name = "mer_signature", columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * @param nama signature/tanda tangan merchant Finpay yang baru untuk balasan ini.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return tipe/jenis transaksi menurut balasan Finpay, sebagaimana adanya (tanpa fallback).
	 */
	public String getTipe() {
		return tipe;
	}

	/**
	 * @param tipe tipe/jenis transaksi yang baru.
	 */
	public void setTipe(String tipe) {
		this.tipe = tipe;
	}

	/**
	 * @return kode merchant Finpay pada balasan ini, sebagaimana adanya (tanpa fallback).
	 */
	public String getMerchant() {
		return merchant;
	}

	/**
	 * @param merchant kode merchant Finpay yang baru.
	 */
	public void setMerchant(String merchant) {
		this.merchant = merchant;
	}

	/**
	 * @return kode pembayaran (payment code) menurut balasan Finpay, sudah di-{@code trim()}; string
	 * kosong jika belum diset (bukan {@code null}).
	 */
	public String getPaymentCode() {
		return paymentCode == null ? "" : paymentCode.trim();
	}

	/**
	 * @param paymentCode kode pembayaran yang baru.
	 */
	public void setPaymentCode(String paymentCode) {
		this.paymentCode = paymentCode;
	}

	/**
	 * @return kode hasil (result code) transaksi menurut balasan Finpay, sebagaimana adanya (tanpa
	 * fallback).
	 */
	public String getResultCode() {
		return resultCode;
	}

	/**
	 * @param resultCode kode hasil transaksi yang baru.
	 */
	public void setResultCode(String resultCode) {
		this.resultCode = resultCode;
	}

	/**
	 * @return deskripsi/keterangan hasil transaksi menurut balasan Finpay, sebagaimana adanya (tanpa
	 * fallback).
	 */
	@Column(name = "result_desc", columnDefinition = "text")
	public String getResultDesc() {
		return resultDesc;
	}

	/**
	 * @param resultDesc deskripsi hasil transaksi yang baru.
	 */
	public void setResultDesc(String resultDesc) {
		this.resultDesc = resultDesc;
	}

	/**
	 * @return nomor log internal Finpay untuk transaksi ini, sebagaimana adanya (tanpa fallback).
	 */
	public String getLogNo() {
		return logNo;
	}

	/**
	 * @param logNo nomor log internal Finpay yang baru.
	 */
	public void setLogNo(String logNo) {
		this.logNo = logNo;
	}

	/**
	 * @return sumber/kanal pembayaran menurut balasan Finpay, sebagaimana adanya (tanpa fallback).
	 */
	public String getPaymentSource() {
		return paymentSource;
	}

	/**
	 * @param paymentSource sumber/kanal pembayaran yang baru.
	 */
	public void setPaymentSource(String paymentSource) {
		this.paymentSource = paymentSource;
	}

	/**
	 * @return nomor invoice/tagihan yang dirujuk balasan ini, sudah di-{@code trim()}; string kosong
	 * jika belum diset (bukan {@code null}).
	 */
	public String getInvoice() {
		return invoice == null ? "" : invoice.trim();
	}

	/**
	 * @param invoice nomor invoice/tagihan yang baru.
	 */
	public void setInvoice(String invoice) {
		this.invoice = invoice;
	}

}
