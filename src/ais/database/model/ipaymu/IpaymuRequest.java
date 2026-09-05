package ais.database.model.ipaymu;

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
 * Entity Hibernate untuk transaksi permintaan pembayaran (request) ke payment gateway <b>iPaymu</b>,
 * dipetakan ke tabel {@code ipaymu_request}. Dibangkitkan oleh hbm2java, lalu dilengkapi banyak field
 * dan logika turunan secara manual.
 *
 * <p>Entity ini adalah kelas <i>header transaksi</i> pada pola 4-kelas per payment gateway yang berulang
 * identik di seluruh integrasi payment gateway di AIS -- lihat juga paket {@code bni}, {@code bri},
 * {@code bsi}, {@code cimb}, {@code doku}, {@code faspay}, {@code finpay}, {@code jatelindo}:</p>
 * <ul>
 * <li>{@code <Gateway>Request} (kelas ini) -- header satu transaksi permintaan pembayaran: siapa yang
 * membayar ({@link #getMahasiswa()}/{@link #getBiodataCalonMahasiswa()}), untuk kegiatan/jadwal apa
 * ({@link #getJenisKegiatan()}, {@link #getJadwalPembayaran()}), berapa nominalnya
 * ({@link #getAmount()}, {@link #getNilaiBiayaHarusDiBayars()}), dan apa hasil balasan gateway-nya
 * ({@link #getIpaymuResponse()}).</li>
 * <li>{@code IpaymuRequestDetail} -- rincian per item/cicilan yang tercakup dalam satu request.</li>
 * <li>{@code IpaymuRequestDetailBiaya} -- rincian komponen biaya/fee tambahan per request.</li>
 * <li>{@link IpaymuResponse} -- balasan mentah dari gateway iPaymu untuk request ini.</li>
 * </ul>
 *
 * <p><b>Perbedaan model ancaman dengan gateway H2H perbankan:</b> berbeda dari BNI/Mandiri/OCBC yang
 * merupakan integrasi host-to-host langsung dengan satu bank, iPaymu adalah <i>payment gateway pihak
 * ketiga untuk konsumen umum</i> (agregator yang meneruskan ke berbagai kanal: VA bank, e-wallet, kartu,
 * dsb). Servlet {@code ais.action.servlet.IPayMuResponse} yang menerima callback iPaymu TIDAK mencatat
 * ke tabel {@code log_host_to_host} (berbeda dari Finpay/Jatelindo/BNI yang memakai tabel itu) --
 * transaksi iPaymu dicatat lewat entity {@code LogPembayaran} yang terpisah, sehingga cakupan/gerbang
 * akses log-nya perlu ditinjau tersendiri (lihat catatan keamanan pada Javadoc kelas ini).</p>
 *
 * <p><b>Catatan keamanan:</b> tidak ada field kartu/PIN/password/token di kelas ini. Field
 * {@link #getNama()} memetakan ke kolom fisik {@code session_id} (BUKAN nama orang), menyimpan session
 * id transaksi iPaymu apa adanya (plaintext) -- pola yang sama dengan {@code BniRequest}/
 * {@code JatelindoRequest}. Field {@link #getNoRekeningDeposit()} menyimpan nomor rekening bank tujuan
 * deposit/refund milik pembayar secara mentah (tanpa masking) -- ini adalah data finansial personal yang
 * lebih sensitif dibanding entity gateway H2H perbankan lain di paket ini (yang umumnya hanya menyimpan
 * VA tujuan milik institusi, bukan rekening pribadi pembayar). Entity ini tidak memiliki field
 * kepemilikan/tenant eksplisit -- cakupan akses ditentukan lewat relasi ke
 * {@link #getMahasiswa()}/{@link #getBiodataCalonMahasiswa()} dan gerbang otorisasi di lapisan Action
 * pemanggil.</p>
 *
 * @see IpaymuResponse
 * @see ais.database.model.bni.BniRequest
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "ipaymu_request")



public class IpaymuRequest extends GeneralValueObject {
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
	 * Mengeset id pengguna audit. Nilai {@code null} atau string kosong/blank diabaikan (fail-safe)
	 * agar id pengguna audit yang sudah tersimpan tidak tertimpa nilai kosong.
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
	 * adalah session id transaksi iPaymu mentah dari kolom {@code session_id}, lihat {@link #getNama()}).
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Session id transaksi iPaymu mentah untuk request ini; lihat {@link #getNama()}. */
	private String nama;
	/** URL redirect/pembayaran iPaymu untuk transaksi ini. */
	private String url;
	/** Id transaksi (trx id) yang diterbitkan iPaymu untuk request ini. */
	private String trxId;
	/** Nama/kode produk yang dibayar, sebagaimana dilaporkan gateway (lihat {@link #getProduct()}). */
	private String product;
	/** Kode merchant iPaymu yang menerima pembayaran (lihat {@link #getMerchant()}). */
	private String merchant;
	/** Nama/identitas pembeli (buyer) menurut balasan iPaymu (lihat {@link #getBuyer()}). */
	private String buyer;
	/** Nomor rekening bank tujuan deposit/refund milik pembayar (lihat {@link #getNoRekeningDeposit()}). */
	private String noRekeningDeposit;
	/** Komentar/catatan bebas untuk transaksi ini. */
	private String comments;
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
	/** Nominal yang diminta untuk dibayarkan lewat iPaymu pada transaksi ini. */
	private Double amount;
	/** Nilai total biaya yang seharusnya dibayar sebelum potongan/pengurangan. */
	private Double nilaiBiayaHarusDiBayars;
	/** Balasan gateway iPaymu untuk request ini (relasi 1 request -&gt; 1 response). */
	private IpaymuResponse ipaymuResponse;

	/**
	 * Konstruktor default (dipakai Hibernate).
	 */
	public IpaymuRequest() {
	}

	/**
	 * @return id unik (primary key, auto-increment) baris {@code ipaymu_request} ini.
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
	 * @return session id transaksi iPaymu untuk request ini (kolom fisik {@code session_id}), sudah
	 * di-{@code trim()}, atau {@code null} jika belum diset. Nama getter ini menyesatkan (bukan nama
	 * orang) -- lihat catatan keamanan pada Javadoc kelas.
	 */
	@Column(name = "session_id", columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * @param nama session id transaksi iPaymu yang baru untuk request ini.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return id transaksi (trx id) iPaymu untuk request ini, sudah di-{@code trim()}; string kosong
	 * jika belum diset (bukan {@code null}).
	 */
	public String getTrxId() {
		return trxId == null ? "" : trxId.trim();
	}

	/**
	 * @param trxId id transaksi iPaymu yang baru.
	 */
	public void setTrxId(String trxId) {
		this.trxId = trxId;
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
	 * @return balasan gateway iPaymu ({@link IpaymuResponse}) untuk request ini, atau {@code null} jika
	 * gateway belum membalas.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "ipaymu_response", nullable = true)
	public IpaymuResponse getIpaymuResponse() {
		return ipaymuResponse;
	}

	/**
	 * @param ipaymuResponse balasan gateway iPaymu yang baru untuk request ini.
	 */
	public void setIpaymuResponse(IpaymuResponse ipaymuResponse) {
		this.ipaymuResponse = ipaymuResponse;
	}

	/**
	 * @return URL redirect/pembayaran iPaymu untuk transaksi ini, sebagaimana adanya (tanpa fallback).
	 */
	@Column(columnDefinition = "text")
	public String getUrl() {
		return url;
	}

	/**
	 * @param url URL redirect/pembayaran iPaymu yang baru.
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * @return nama/kode produk yang dibayar. Jika {@link #ipaymuResponse} sudah ada, nilai disinkronkan
	 * (efek samping: field {@link #product} ikut termutasi) dari {@code IpaymuResponse.getProduct()}
	 * setiap kali getter ini dipanggil -- response menjadi sumber kebenaran begitu tersedia.
	 */
	@Column(columnDefinition = "text")
	public String getProduct() {
		if (ipaymuResponse != null) {
			product = ipaymuResponse.getProduct();
		}
		return product;
	}

	/**
	 * @param product nama/kode produk yang baru; dapat tertimpa kembali oleh {@link #getProduct()}
	 * selama {@link #ipaymuResponse} tidak null.
	 */
	public void setProduct(String product) {
		this.product = product;
	}

	/**
	 * @return kode merchant iPaymu. Jika {@link #ipaymuResponse} sudah ada, nilai disinkronkan (efek
	 * samping: field {@link #merchant} ikut termutasi) dari {@code IpaymuResponse.getMerchant()} setiap
	 * kali getter ini dipanggil.
	 */
	public String getMerchant() {
		if (ipaymuResponse != null) {
			merchant = ipaymuResponse.getMerchant();
		}
		return merchant;
	}

	/**
	 * @param merchant kode merchant iPaymu yang baru; dapat tertimpa kembali oleh {@link #getMerchant()}
	 * selama {@link #ipaymuResponse} tidak null.
	 */
	public void setMerchant(String merchant) {
		this.merchant = merchant;
	}

	/**
	 * @return nama/identitas pembeli (buyer). Jika {@link #ipaymuResponse} sudah ada, nilai
	 * disinkronkan (efek samping: field {@link #buyer} ikut termutasi) dari
	 * {@code IpaymuResponse.getBuyer()}; string kosong (bukan {@code null}) jika tetap tidak tersedia.
	 */
	public String getBuyer() {
		if (ipaymuResponse != null) {
			buyer = ipaymuResponse.getBuyer();
		}
		if (buyer == null) {
			buyer = "";
		}
		return buyer;
	}

	/**
	 * @param buyer nama/identitas pembeli yang baru; dapat tertimpa kembali oleh {@link #getBuyer()}
	 * selama {@link #ipaymuResponse} tidak null.
	 */
	public void setBuyer(String buyer) {
		this.buyer = buyer;
	}

	/**
	 * @return nomor rekening bank tujuan deposit/refund milik pembayar (data finansial personal,
	 * disimpan mentah tanpa masking -- lihat catatan keamanan pada Javadoc kelas). Jika
	 * {@link #ipaymuResponse} sudah ada, nilai disinkronkan (efek samping: field
	 * {@link #noRekeningDeposit} ikut termutasi) dari {@code IpaymuResponse.getNoRekeningDeposit()};
	 * string kosong (bukan {@code null}) jika tetap tidak tersedia.
	 */
	public String getNoRekeningDeposit() {
		if (ipaymuResponse != null) {
			noRekeningDeposit = ipaymuResponse.getNoRekeningDeposit();
		}
		if (noRekeningDeposit == null) {
			noRekeningDeposit = "";
		}
		return noRekeningDeposit;
	}

	/**
	 * @param noRekeningDeposit nomor rekening tujuan deposit/refund yang baru; dapat tertimpa kembali
	 * oleh {@link #getNoRekeningDeposit()} selama {@link #ipaymuResponse} tidak null.
	 */
	public void setNoRekeningDeposit(String noRekeningDeposit) {
		this.noRekeningDeposit = noRekeningDeposit;
	}

	/**
	 * @return komentar/catatan bebas untuk transaksi ini, sebagaimana adanya (tanpa fallback).
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
	 * @return nominal yang diminta untuk dibayarkan lewat iPaymu; {@code 0.0} (bukan {@code null}) jika
	 * belum pernah diset -- pemanggilan getter ini juga melakukan lazy-init pada field {@link #amount}.
	 */
	public Double getAmount() {
		if (amount == null) {
			amount = 0.0;
		}
		return amount;
	}

	/**
	 * @param amount nominal yang baru untuk dibayarkan lewat iPaymu.
	 */
	public void setAmount(Double amount) {
		this.amount = amount;
	}
}
