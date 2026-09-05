package ais.database.model.jatelindo;

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
 * Entity Hibernate untuk balasan (response) dari payment gateway/switching <b>Jatelindo</b> atas
 * sebuah {@link JatelindoRequest}, dipetakan ke tabel {@code jatelindo_response}. Dibangkitkan oleh
 * hbm2java, lalu dilengkapi field tambahan dan konstanta status secara manual.
 *
 * <p>Kelas ini adalah kelas <i>balasan gateway</i> pada pola 4-kelas per payment gateway yang berulang
 * identik di seluruh integrasi H2H AIS (lihat {@link JatelindoRequest} untuk penjelasan pola lengkap).
 * Satu baris entity ini merepresentasikan balasan Jatelindo untuk satu request; relasi ke request
 * pemilik disimpan sebaliknya, lewat {@code JatelindoRequest.getJatelindoResponse()}. Konstanta
 * {@link #PENDING}, {@link #BERHASIL}, {@link #SEDANG_DIPROSES}, {@link #BATAL}, {@link #REFUND}
 * mendefinisikan nilai-nilai status transaksi Jatelindo yang dikenal AIS.</p>
 *
 * <p><b>Catatan keamanan:</b> sama seperti {@code BniResponse}, entity ini menyimpan payload callback
 * mentah utuh pada kolom {@code callback} ({@link #getCallback()}) -- bukan hanya field-field terurai.
 * Field {@link #getNama()} memetakan ke kolom fisik {@code sid} (BUKAN nama orang), disimpan plaintext.
 * Servlet {@code ais.action.servlet.JatelindoCallback} mencatat callback ini ke tabel
 * {@code log_host_to_host} yang sama dipakai H2H Bank Mandiri/OCBC NISP -- perluasan cakupan temuan
 * {@code LogHostToHostAction.java} (payload H2H tereskspos) ke gateway ini, bukan celah baru.</p>
 *
 * @see JatelindoRequest
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "jatelindo_response")



public class JatelindoResponse extends GeneralValueObject {

	/** Status "menunggu" -- transaksi Jatelindo belum tuntas diproses gateway. */
	public final static String PENDING = "pending";
	/** Status "berhasil" -- transaksi Jatelindo telah sukses diproses gateway. */
	public final static String BERHASIL = "berhasil";
	/** Status "sedang diproses" -- transaksi Jatelindo masih dalam proses di gateway/bank. */
	public final static String SEDANG_DIPROSES = "Sedang diproses";
	/** Status "batal" -- transaksi Jatelindo dibatalkan. */
	public final static String BATAL = "Batal";
	/** Status "refund" -- dana transaksi Jatelindo dikembalikan ke pembayar. */
	public final static String REFUND = "Refund";

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
	 * adalah session id transaksi mentah dari kolom {@code sid}, lihat {@link #getNama()}).
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Session id transaksi Jatelindo mentah pada balasan ini; lihat {@link #getNama()}. */
	private String nama;
	/** Status transaksi menurut balasan Jatelindo (lihat konstanta {@link #PENDING} dkk). */
	private String status;
	/** Kode status transaksi menurut balasan Jatelindo, default "0" (lihat {@link #getKodeStatus()}). */
	private String kodeStatus;
	/** Id transaksi (trx id) yang diterbitkan Jatelindo pada balasan ini. */
	private String trxId;
	/** Nama bank/merchant tujuan pembayaran (lihat {@link #getMerchant()}, default "Mandiri"). */
	private String merchant;
	/** Keterangan balasan; default string kosong bila belum diset (lihat {@link #getKeterangan()}). */
	private String keterangan;
	/** Payload callback mentah utuh yang diterima dari Jatelindo, disimpan apa adanya (lihat catatan keamanan Javadoc kelas). */
	private String callback;

	/**
	 * Konstruktor default (dipakai Hibernate).
	 */
	public JatelindoResponse() {
	}

	/**
	 * @return id unik (primary key, auto-increment) baris {@code jatelindo_response} ini.
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
	 * @return session id transaksi Jatelindo pada balasan ini (kolom fisik {@code sid}), sudah
	 * di-{@code trim()}, atau {@code null} jika belum diset. Nama getter ini menyesatkan (bukan nama
	 * orang) -- lihat catatan keamanan pada Javadoc kelas.
	 */
	@Column(name = "sid", columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * @param nama session id transaksi Jatelindo yang baru untuk balasan ini.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return id transaksi (trx id) Jatelindo pada balasan ini, sebagaimana adanya (tanpa fallback).
	 */
	public String getTrxId() {
		return trxId;
	}

	/**
	 * @param trxId id transaksi Jatelindo yang baru.
	 */
	public void setTrxId(String trxId) {
		this.trxId = trxId;
	}

	/**
	 * @return nama bank/merchant tujuan pembayaran; jika kosong/null, memberi fallback {@code "Mandiri"}
	 * (lihat catatan arsitektur pada Javadoc kelas {@link JatelindoRequest}: Jatelindo di AIS dipakai
	 * terutama sebagai switching VA Bank Mandiri).
	 */
	public String getMerchant() {
		return merchant == null || merchant.trim().isEmpty() ? "Mandiri" : merchant;
	}

	/**
	 * @param merchant nama bank/merchant tujuan pembayaran yang baru.
	 */
	public void setMerchant(String merchant) {
		this.merchant = merchant;
	}

	/**
	 * @return status transaksi menurut balasan Jatelindo (lihat konstanta {@link #PENDING},
	 * {@link #BERHASIL}, {@link #SEDANG_DIPROSES}, {@link #BATAL}, {@link #REFUND}); string kosong
	 * (bukan {@code null}) jika belum pernah diset -- pemanggilan getter ini juga melakukan lazy-init
	 * pada field {@link #status}.
	 */
	public String getStatus() {
		if (status == null) {
			status = "";
		}
		return status;
	}

	/**
	 * @param status status transaksi yang baru.
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * @return keterangan balasan Jatelindo; string kosong (bukan {@code null}) jika belum pernah diset
	 * -- pemanggilan getter ini juga melakukan lazy-init pada field {@link #keterangan}.
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		if (keterangan == null) {
			keterangan = "";
		}
		return keterangan;
	}

	/**
	 * @param keterangan keterangan balasan yang baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return payload callback mentah utuh yang diterima dari Jatelindo, sebagaimana adanya (tanpa
	 * fallback) -- lihat catatan keamanan pada Javadoc kelas soal penyimpanan payload H2H mentah.
	 */
	@Column(columnDefinition = "text")
	public String getCallback() {
		return callback;
	}

	/**
	 * @param callback payload callback mentah yang baru.
	 */
	public void setCallback(String callback) {
		this.callback = callback;
	}

	/**
	 * @return kode status transaksi menurut balasan Jatelindo; default {@code "0"} (bukan {@code null})
	 * jika belum pernah diset -- pemanggilan getter ini juga melakukan lazy-init pada field
	 * {@link #kodeStatus}.
	 */
	public String getKodeStatus() {
		if (kodeStatus == null) {
			kodeStatus = "0";
		}
		return kodeStatus;
	}

	/**
	 * @param kodeStatus kode status transaksi yang baru.
	 */
	public void setKodeStatus(String kodeStatus) {
		this.kodeStatus = kodeStatus;
	}

}
