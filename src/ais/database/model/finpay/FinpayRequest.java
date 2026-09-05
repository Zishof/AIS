package ais.database.model.finpay;

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
 * Entity Hibernate untuk transaksi permintaan pembayaran (request) ke payment gateway <b>Finpay</b>,
 * dipetakan ke tabel {@code finpay_request}. Dibangkitkan oleh hbm2java, lalu dilengkapi banyak field
 * dan logika turunan secara manual.
 *
 * <p>Entity ini adalah kelas <i>header transaksi</i> pada pola 4-kelas per payment gateway yang berulang
 * identik di seluruh integrasi host-to-host (H2H) AIS -- lihat juga paket {@code bni}, {@code bri},
 * {@code bsi}, {@code cimb}, {@code doku}, {@code faspay}, {@code ipaymu}, {@code jatelindo}:</p>
 * <ul>
 * <li>{@code <Gateway>Request} (kelas ini) -- header satu transaksi permintaan pembayaran: siapa yang
 * membayar ({@link #getMahasiswa()}/{@link #getBiodataCalonMahasiswa()}), untuk kegiatan/jadwal apa
 * ({@link #getJenisKegiatan()}, {@link #getJadwalPembayaran()}), berapa nominalnya
 * ({@link #getAmount()}, {@link #getNilaiBiayaHarusDiBayars()}), dan apa hasil balasan gateway-nya
 * ({@link #getFinpayResponse()}).</li>
 * <li>{@code FinpayRequestDetail} -- rincian per item/cicilan yang tercakup dalam satu request.</li>
 * <li>{@code FinpayRequestDetailBiaya} -- rincian komponen biaya/fee tambahan per request.</li>
 * <li>{@link FinpayResponse} -- balasan mentah dari gateway Finpay untuk request ini.</li>
 * </ul>
 *
 * <p><b>Catatan keamanan:</b> berbeda dengan {@code BniRequest}/{@code JatelindoRequest} yang menyimpan
 * payload request/response mentah utuh (kolom {@code request}/{@code response}/{@code callback}), entity
 * ini TIDAK menyimpan payload gabungan -- data Finpay disimpan terurai per kolom ({@link #getTipe()},
 * {@link #getInvoice()}, {@link #getPaymentCode()}, {@link #getResultCode()}, dst). Namun field
 * {@link #getNama()} tetap memetakan ke kolom fisik {@code mer_signature} (BUKAN nama orang) yang
 * menyimpan tanda tangan/signature merchant Finpay apa adanya (plaintext, tanpa hashing/enkripsi di
 * level entity) -- sama seperti kolom {@code session_id} yang disamarkan sebagai {@code nama} pada
 * {@code BniRequest}/{@code IpaymuRequest}/{@code JatelindoRequest}. Servlet
 * {@code ais.action.servlet.Finpay} dan {@code ais.action.servlet.FinPayResponse} mencatat transaksi
 * Finpay ke tabel {@code log_host_to_host} yang sama dipakai untuk H2H Bank Mandiri/OCBC NISP -- lihat
 * temuan {@code LogHostToHostAction.java} soal payload mentah H2H yang tereskspos (VA/trxId/nominal);
 * transaksi Finpay memperluas cakupan temuan tersebut ke gateway ini, bukan celah baru yang berbeda
 * kategori. Entity ini tidak memiliki field kepemilikan/tenant eksplisit (mis. {@code satuanKerja}) --
 * cakupan akses ditentukan lewat relasi ke {@link #getMahasiswa()}/{@link #getBiodataCalonMahasiswa()}
 * dan gerbang otorisasi di lapisan Action pemanggil.</p>
 *
 * @see FinpayResponse
 * @see ais.database.model.bni.BniRequest
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "finpay_request")



public class FinpayRequest extends GeneralValueObject {
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
	public String getOlehId() {return olehId;}
	/**
	 * Mengeset id pengguna audit. Nilai {@code null} atau string kosong/blank diabaikan (fail-safe,
	 * bukan fail-closed) agar id pengguna audit yang sudah tersimpan tidak tertimpa nilai kosong.
	 * @param olehId id pengguna audit baru; diabaikan jika null/kosong.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}

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
	 * di-{@code UPDATE}, sehingga timestamp audit "terakhir diubah" selalu konsisten dengan interceptor
	 * yang sama dipakai entity lain di AIS.
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

	/** Signature/tanda tangan merchant Finpay mentah untuk request ini; lihat {@link #getNama()}. */
	private String nama;
	/** Tipe/jenis transaksi Finpay (mis. kode metode pembayaran yang dipilih). */
	private String tipe;
	/** Kode merchant Finpay yang menerima pembayaran. */
	private String merchant;
	/** Nomor invoice/nomor tagihan yang dikirim ke Finpay untuk transaksi ini. */
	private String invoice;
	/** Kode pembayaran (payment code/kode VA) yang diterbitkan Finpay untuk transaksi ini. */
	private String paymentCode;
	/** Mahasiswa aktif pembayar, jika transaksi ini dibuat atas nama mahasiswa terdaftar. */
	private Mahasiswa mahasiswa;
	/** Calon mahasiswa pembayar, jika transaksi ini dibuat pada alur pendaftaran/PMB. */
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	/** Jenis kegiatan akademik yang menjadi konteks penagihan (mis. daftar ulang, her-registrasi). */
	private JenisKegiatan jenisKegiatan;
	/** Jadwal pembayaran yang menjadi acuan nominal/periode tagihan. */
	private JadwalPembayaran jadwalPembayaran;
	/** Semester akademik terkait transaksi. */
	private Integer semester;
	/** Tahun akademik terkait transaksi. */
	private String tahunAkademik;
	/** Keterangan bebas untuk transaksi ini. */
	private String keterangan;
	/** Nominal pengurangan/potongan yang diterapkan atas tagihan asli. */
	private Double pengurangan;
	/** Nominal yang diminta untuk dibayarkan lewat Finpay pada transaksi ini. */
	private Double amount;
	/** Nilai total biaya yang seharusnya dibayar sebelum potongan/pengurangan. */
	private Double nilaiBiayaHarusDiBayars;
	/** Balasan gateway Finpay untuk request ini (relasi 1 request -&gt; 1 response). */
	private FinpayResponse finpayResponse;
	/** Kode hasil (result code) balasan Finpay yang disalin/dicatat pada request ini. */
	private String resultCode;
	/** Status transaksi menurut catatan AIS (bukan status mentah dari gateway). */
	private String status;

	/**
	 * Konstruktor default (dipakai Hibernate).
	 */
	public FinpayRequest() {
	}

	/**
	 * @return id unik (primary key, auto-increment) baris {@code finpay_request} ini.
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
	 * @return signature/tanda tangan merchant Finpay untuk request ini (kolom fisik
	 * {@code mer_signature}), sudah di-{@code trim()}, atau {@code null} jika belum diset. Nama getter
	 * ini menyesatkan (bukan nama orang) -- lihat catatan keamanan pada Javadoc kelas.
	 */
	@Column(name = "mer_signature", columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * @param nama signature/tanda tangan merchant Finpay yang baru untuk request ini.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return tipe/jenis transaksi Finpay, sebagaimana adanya (tanpa fallback).
	 */
	public String getTipe() {
		return tipe;
	}

	/**
	 * @param tipe tipe/jenis transaksi Finpay yang baru.
	 */
	public void setTipe(String tipe) {
		this.tipe = tipe;
	}

	/**
	 * @return kode merchant Finpay penerima pembayaran, sebagaimana adanya (tanpa fallback).
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
	 * @return kode pembayaran (payment code) yang diterbitkan Finpay, sudah di-{@code trim()}; string
	 * kosong jika belum diset (bukan {@code null}).
	 */
	public String getPaymentCode() {
		return paymentCode == null ? "" : paymentCode.trim();
	}

	/**
	 * @param paymentCode kode pembayaran (payment code) yang baru untuk request ini.
	 */
	public void setPaymentCode(String paymentCode) {
		this.paymentCode = paymentCode;
	}

	/**
	 * @return nomor invoice/tagihan yang dikirim ke Finpay, sudah di-{@code trim()}; string kosong jika
	 * belum diset (bukan {@code null}).
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

	/**
	 * @return mahasiswa aktif pembayar transaksi ini, atau {@code null} jika transaksi ini bukan atas
	 * nama mahasiswa terdaftar (mis. calon mahasiswa pada alur PMB).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		return mahasiswa;
	}

	/**
	 * @param mahasiswa mahasiswa pembayar yang baru untuk transaksi ini.
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * @return calon mahasiswa pembayar transaksi ini (alur PMB/pendaftaran), atau {@code null} jika
	 * transaksi ini dibuat atas nama mahasiswa terdaftar.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "biodata_calon_mahasiswa", nullable = true)
	public BiodataCalonMahasiswa getBiodataCalonMahasiswa() {
		return biodataCalonMahasiswa;
	}

	/**
	 * @param biodataCalonMahasiswa calon mahasiswa pembayar yang baru untuk transaksi ini.
	 */
	public void setBiodataCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	/**
	 * @return jenis kegiatan akademik yang menjadi konteks penagihan transaksi ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jenis_kegiatan", nullable = true)
	public JenisKegiatan getJenisKegiatan() {
		return jenisKegiatan;
	}

	/**
	 * @param jenisKegiatan jenis kegiatan akademik yang baru untuk transaksi ini.
	 */
	public void setJenisKegiatan(JenisKegiatan jenisKegiatan) {
		this.jenisKegiatan = jenisKegiatan;
	}

	/**
	 * @return jadwal pembayaran acuan nominal/periode tagihan transaksi ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jadwal_pembayaran", nullable = true)
	public JadwalPembayaran getJadwalPembayaran() {
		return jadwalPembayaran;
	}

	/**
	 * @param jadwalPembayaran jadwal pembayaran acuan yang baru.
	 */
	public void setJadwalPembayaran(JadwalPembayaran jadwalPembayaran) {
		this.jadwalPembayaran = jadwalPembayaran;
	}

	/**
	 * @return semester akademik terkait transaksi ini, sebagaimana adanya (tanpa fallback).
	 */
	public Integer getSemester() {
		return semester;
	}

	/**
	 * @param semester semester akademik yang baru.
	 */
	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	/**
	 * @return tahun akademik terkait transaksi ini, sebagaimana adanya (tanpa fallback).
	 */
	public String getTahunAkademik() {
		return tahunAkademik;
	}

	/**
	 * @param tahunAkademik tahun akademik yang baru.
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * @return nominal pengurangan/potongan yang diterapkan; {@code 0.0} (bukan {@code null}) jika belum
	 * pernah diset -- pemanggilan getter ini juga melakukan lazy-init pada field {@link #pengurangan}.
	 */
	public Double getPengurangan() {
		if (pengurangan == null) {
			pengurangan = 0.0;
		}
		return pengurangan;
	}

	/**
	 * @param pengurangan nominal pengurangan/potongan yang baru.
	 */
	public void setPengurangan(Double pengurangan) {
		this.pengurangan = pengurangan;
	}

	/**
	 * @return nilai total biaya yang seharusnya dibayar sebelum potongan; {@code 0.0} (bukan
	 * {@code null}) jika belum pernah diset -- pemanggilan getter ini juga melakukan lazy-init pada
	 * field {@link #nilaiBiayaHarusDiBayars}.
	 */
	public Double getNilaiBiayaHarusDiBayars() {
		if (nilaiBiayaHarusDiBayars == null) {
			nilaiBiayaHarusDiBayars = 0.0;
		}
		return nilaiBiayaHarusDiBayars;
	}

	/**
	 * @param nilaiBiayaHarusDiBayars nilai total biaya yang baru sebelum potongan.
	 */
	public void setNilaiBiayaHarusDiBayars(Double nilaiBiayaHarusDiBayars) {
		this.nilaiBiayaHarusDiBayars = nilaiBiayaHarusDiBayars;
	}

	/**
	 * @return keterangan bebas untuk transaksi ini, sebagaimana adanya (tanpa fallback).
	 */
	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * @param keterangan keterangan bebas yang baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return balasan gateway Finpay ({@link FinpayResponse}) untuk request ini, atau {@code null} jika
	 * gateway belum membalas.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "finpay_response", nullable = true)
	public FinpayResponse getFinpayResponse() {
		return finpayResponse;
	}

	/**
	 * @param finpayResponse balasan gateway Finpay yang baru untuk request ini.
	 */
	public void setFinpayResponse(FinpayResponse finpayResponse) {
		this.finpayResponse = finpayResponse;
	}

	/**
	 * @return nominal yang diminta untuk dibayarkan lewat Finpay; {@code 0.0} (bukan {@code null}) jika
	 * belum pernah diset -- pemanggilan getter ini juga melakukan lazy-init pada field {@link #amount}.
	 */
	public Double getAmount() {
		if (amount == null) {
			amount = 0.0;
		}
		return amount;
	}

	/**
	 * @param amount nominal yang baru untuk dibayarkan lewat Finpay.
	 */
	public void setAmount(Double amount) {
		this.amount = amount;
	}

	/**
	 * @return kode hasil (result code) balasan Finpay yang dicatat pada request ini, sebagaimana adanya
	 * (tanpa fallback).
	 */
	public String getResultCode() {
		return resultCode;
	}

	/**
	 * @param resultCode kode hasil balasan Finpay yang baru.
	 */
	public void setResultCode(String resultCode) {
		this.resultCode = resultCode;
	}

	/**
	 * @return status transaksi menurut catatan AIS, sebagaimana adanya (tanpa fallback) -- berbeda
	 * dengan {@code BniRequest}/{@code JatelindoRequest}, entity ini tidak menyediakan nilai default
	 * "Belum diproses" saat status belum diset.
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * @param status status transaksi yang baru.
	 */
	public void setStatus(String status) {
		this.status = status;
	}
}
