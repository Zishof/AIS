package ais.database.model.ipaymu;

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
 * Entity Hibernate untuk balasan (response) dari payment gateway <b>iPaymu</b> atas sebuah
 * {@link IpaymuRequest}, dipetakan ke tabel {@code ipaymu_response}. Dibangkitkan oleh hbm2java, lalu
 * dilengkapi field tambahan dan konstanta status secara manual.
 *
 * <p>Kelas ini adalah kelas <i>balasan gateway</i> pada pola 4-kelas per payment gateway yang berulang
 * identik di seluruh integrasi payment gateway AIS (lihat {@link IpaymuRequest} untuk penjelasan pola
 * lengkap). Satu baris entity ini merepresentasikan balasan iPaymu untuk satu request; relasi ke request
 * pemilik disimpan sebaliknya, lewat {@code IpaymuRequest.getIpaymuResponse()}. Konstanta
 * {@link #PENDING}, {@link #BERHASIL}, {@link #SEDANG_DIPROSES}, {@link #BATAL}, {@link #REFUND}
 * mendefinisikan nilai-nilai status transaksi iPaymu yang dikenal AIS.</p>
 *
 * <p><b>Catatan keamanan:</b> tidak ada field kartu/PIN/password di kelas ini, dan tidak seperti
 * {@code BniResponse}/{@code JatelindoResponse} entity ini tidak menyimpan payload mentah gabungan --
 * data iPaymu disimpan terurai per kolom. Namun field {@link #getNama()} tetap memetakan ke kolom fisik
 * {@code sid} (BUKAN nama orang), menyimpan session id transaksi iPaymu apa adanya (plaintext). Field
 * {@link #getNoRekeningDeposit()} menyimpan nomor rekening bank tujuan deposit/refund milik pembayar
 * secara mentah (tanpa masking) -- data finansial personal yang lebih sensitif dibanding entity gateway
 * H2H perbankan lain di paket ini. Berbeda dari servlet Finpay/Jatelindo/BNI, servlet
 * {@code ais.action.servlet.IPayMuResponse} yang menerima callback iPaymu TIDAK mencatat ke tabel
 * {@code log_host_to_host} -- ia mencatat lewat entity {@code LogPembayaran} yang terpisah, sehingga
 * cakupan/gerbang akses log-nya perlu ditinjau tersendiri dari temuan {@code LogHostToHostAction.java}.</p>
 *
 * @see IpaymuRequest
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "ipaymu_response")



public class IpaymuResponse extends GeneralValueObject {

	/** Status "menunggu" -- transaksi iPaymu belum tuntas diproses gateway. */
	public final static String PENDING = "pending";
	/** Status "berhasil" -- transaksi iPaymu telah sukses diproses gateway. */
	public final static String BERHASIL = "berhasil";
	/** Status "sedang diproses" -- transaksi iPaymu masih dalam proses di gateway/bank. */
	public final static String SEDANG_DIPROSES = "Sedang diproses";
	/** Status "batal" -- transaksi iPaymu dibatalkan. */
	public final static String BATAL = "Batal";
	/** Status "refund" -- dana transaksi iPaymu dikembalikan ke pembayar. */
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
	 * adalah session id transaksi iPaymu mentah dari kolom {@code sid}, lihat {@link #getNama()}).
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Session id transaksi iPaymu mentah pada balasan ini; lihat {@link #getNama()}. */
	private String nama;
	/** Status transaksi menurut balasan iPaymu (lihat konstanta {@link #PENDING} dkk). */
	private String status;
	/** Id transaksi (trx id) yang diterbitkan iPaymu pada balasan ini. */
	private String trxId;
	/** Nama/kode produk yang dibayar menurut balasan iPaymu. */
	private String product;
	/** Kode merchant iPaymu pada balasan ini. */
	private String merchant;
	/** Nama/identitas pembeli (buyer) menurut balasan iPaymu. */
	private String buyer;
	/** Nomor rekening bank tujuan deposit/refund milik pembayar (lihat catatan keamanan Javadoc kelas). */
	private String noRekeningDeposit;
	/** Komentar/catatan bebas menurut balasan iPaymu. */
	private String comments;
	/** Keterangan balasan; default string kosong bila belum diset (lihat {@link #getKeterangan()}). */
	private String keterangan;

	/**
	 * Konstruktor default (dipakai Hibernate).
	 */
	public IpaymuResponse() {
	}

	/**
	 * @return id unik (primary key, auto-increment) baris {@code ipaymu_response} ini.
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
	 * @return session id transaksi iPaymu pada balasan ini (kolom fisik {@code sid}), sudah
	 * di-{@code trim()}, atau {@code null} jika belum diset. Nama getter ini menyesatkan (bukan nama
	 * orang) -- lihat catatan keamanan pada Javadoc kelas.
	 */
	@Column(name = "sid", columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * @param nama session id transaksi iPaymu yang baru untuk balasan ini.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return id transaksi (trx id) iPaymu pada balasan ini, sebagaimana adanya (tanpa fallback).
	 */
	public String getTrxId() {
		return trxId;
	}

	/**
	 * @param trxId id transaksi iPaymu yang baru.
	 */
	public void setTrxId(String trxId) {
		this.trxId = trxId;
	}

	/**
	 * @return nama/kode produk yang dibayar menurut balasan iPaymu, sebagaimana adanya (tanpa fallback).
	 */
	@Column(columnDefinition = "text")
	public String getProduct() {
		return product;
	}

	/**
	 * @param product nama/kode produk yang baru.
	 */
	public void setProduct(String product) {
		this.product = product;
	}

	/**
	 * @return kode merchant iPaymu pada balasan ini, sebagaimana adanya (tanpa fallback).
	 */
	public String getMerchant() {
		return merchant;
	}

	/**
	 * @param merchant kode merchant iPaymu yang baru.
	 */
	public void setMerchant(String merchant) {
		this.merchant = merchant;
	}

	/**
	 * @return nama/identitas pembeli (buyer) menurut balasan iPaymu, sebagaimana adanya (tanpa fallback).
	 */
	public String getBuyer() {
		return buyer;
	}

	/**
	 * @param buyer nama/identitas pembeli yang baru.
	 */
	public void setBuyer(String buyer) {
		this.buyer = buyer;
	}

	/**
	 * @return nomor rekening bank tujuan deposit/refund milik pembayar, sebagaimana adanya (tanpa
	 * fallback) -- data finansial personal, disimpan mentah tanpa masking (lihat catatan keamanan
	 * pada Javadoc kelas).
	 */
	public String getNoRekeningDeposit() {
		return noRekeningDeposit;
	}

	/**
	 * @param noRekeningDeposit nomor rekening tujuan deposit/refund yang baru.
	 */
	public void setNoRekeningDeposit(String noRekeningDeposit) {
		this.noRekeningDeposit = noRekeningDeposit;
	}

	/**
	 * @return komentar/catatan bebas menurut balasan iPaymu, sebagaimana adanya (tanpa fallback).
	 */
	@Column(columnDefinition = "text")
	public String getComments() {
		return comments;
	}

	/**
	 * @param comments komentar/catatan bebas yang baru.
	 */
	public void setComments(String comments) {
		this.comments = comments;
	}

	/**
	 * @return status transaksi menurut balasan iPaymu (lihat konstanta {@link #PENDING},
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
	 * @return keterangan balasan iPaymu; string kosong (bukan {@code null}) jika belum pernah diset --
	 * pemanggilan getter ini juga melakukan lazy-init pada field {@link #keterangan}.
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

}
