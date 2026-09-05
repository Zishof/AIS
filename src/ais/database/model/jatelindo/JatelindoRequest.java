package ais.database.model.jatelindo;

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
 * Entity Hibernate untuk transaksi permintaan pembayaran (request) ke payment gateway/switching
 * <b>Jatelindo</b>, dipetakan ke tabel {@code jatelindo_request}. Dibangkitkan oleh hbm2java, lalu
 * dilengkapi banyak field dan logika turunan secara manual.
 *
 * <p>Entity ini adalah kelas <i>header transaksi</i> pada pola 4-kelas per payment gateway yang berulang
 * identik di seluruh integrasi host-to-host (H2H) AIS -- lihat juga paket {@code bni}, {@code bri},
 * {@code bsi}, {@code cimb}, {@code doku}, {@code faspay}, {@code finpay}, {@code ipaymu}:</p>
 * <ul>
 * <li>{@code <Gateway>Request} (kelas ini) -- header satu transaksi permintaan pembayaran: siapa yang
 * membayar ({@link #getMahasiswa()}/{@link #getBiodataCalonMahasiswa()}), untuk kegiatan/jadwal apa
 * ({@link #getJenisKegiatan()}, {@link #getJadwalPembayaran()}), berapa nominalnya
 * ({@link #getAmount()}, {@link #getNilaiBiayaHarusDiBayars()}), dan apa hasil balasan gateway-nya
 * ({@link #getJatelindoResponse()}).</li>
 * <li>{@code JatelindoRequestDetail} -- rincian per item/cicilan yang tercakup dalam satu request.</li>
 * <li>{@code JatelindoRequestDetailBiaya} -- rincian komponen biaya/fee tambahan per request.</li>
 * <li>{@link JatelindoResponse} -- balasan mentah dari gateway Jatelindo untuk request ini.</li>
 * </ul>
 *
 * <p><b>Catatan arsitektur:</b> {@link #getMerchant()} memberi fallback {@code "Mandiri"} bila kolom
 * merchant kosong -- mengindikasikan Jatelindo di AIS dipakai terutama sebagai switching/perantara VA
 * untuk Bank Mandiri (Jatelindo adalah penyedia jasa switching pembayaran, bukan bank penerbit VA itu
 * sendiri).</p>
 *
 * <p><b>Catatan keamanan:</b> sama seperti {@code BniRequest}, entity ini menyimpan payload request
 * mentah utuh pada kolom {@code request} ({@link #getRequest()}) -- bukan hanya field-field terurai.
 * Field {@link #getNama()} memetakan ke kolom fisik {@code session_id} (BUKAN nama orang), disimpan
 * plaintext. Servlet {@code ais.action.servlet.JatelindoCallback} mencatat callback Jatelindo ke tabel
 * {@code log_host_to_host} yang sama dipakai untuk H2H Bank Mandiri/OCBC NISP -- ini adalah gateway yang
 * paling dekat kemiripannya dengan temuan {@code LogHostToHostAction.java} (payload mentah H2H yang
 * tereskspos: VA/trxId/nominal); transaksi Jatelindo memperluas cakupan temuan tersebut ke gateway ini,
 * bukan celah baru yang berbeda kategori. Entity ini tidak memiliki field kepemilikan/tenant eksplisit
 * -- cakupan akses ditentukan lewat relasi ke {@link #getMahasiswa()}/{@link #getBiodataCalonMahasiswa()}
 * dan gerbang otorisasi di lapisan Action pemanggil.</p>
 *
 * @see JatelindoResponse
 * @see ais.database.model.bni.BniRequest
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "jatelindo_request")



public class JatelindoRequest extends GeneralValueObject {
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
	 * adalah session id transaksi mentah dari kolom {@code session_id}, lihat {@link #getNama()}).
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Session id transaksi Jatelindo mentah untuk request ini; lihat {@link #getNama()}. */
	private String nama;
	/** Menandai apakah cicilan sebelumnya untuk tagihan yang sama harus dihapus sebelum request baru dibuat. */
	private Boolean hapusCicilanSebelumnya;
	/** Id transaksi (trx id) yang diterbitkan Jatelindo untuk request ini. */
	private String trxId;
	/** Nama bank/merchant tujuan pembayaran (lihat {@link #getMerchant()}, default "Mandiri"). */
	private String merchant;

	/** Payload request mentah utuh yang dikirim ke Jatelindo, disimpan apa adanya (lihat catatan keamanan Javadoc kelas). */
	private String request;
	/** Id merchant Jatelindo yang menerima pembayaran. */
	private String merchant_id;

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
	/** Nominal yang diminta untuk dibayarkan lewat Jatelindo pada transaksi ini. */
	private Double amount;
	/** Nilai total biaya yang seharusnya dibayar sebelum potongan/pengurangan. */
	private Double nilaiBiayaHarusDiBayars;
	/** Balasan gateway Jatelindo untuk request ini (relasi 1 request -&gt; 1 response). */
	private JatelindoResponse jatelindoResponse;

	/** Status transaksi menurut catatan AIS, default "Belum diproses" (lihat {@link #getStatus()}). */
	private String status;
	/** Kode status transaksi menurut catatan AIS, default "0" (lihat {@link #getKodeStatus()}). */
	private String kodeStatus;

	/** Nominal biaya administrasi, dengan fallback ke konfigurasi global "jatelindo_biaya_administrasi". */
	private Double biayaAdministrasi;

	/** Kumpulan kegiatan temporary yang tercakup dalam transaksi ini (relasi many-to-many). */
	private Set<KegiatanTemporary> kegiatanTemporarys = new HashSet<KegiatanTemporary>();
	/** Nominal biaya payment gateway, dengan fallback ke konfigurasi global "jatelindo_biaya_payment_gateway". */
	private Double biayaPaymentGateway;

	/**
	 * @return kumpulan {@link KegiatanTemporary} yang tercakup dalam transaksi request ini, lewat tabel
	 * penghubung {@code jatelindo_request_has_kegiatan_temporary}. Tidak pernah {@code null} (default
	 * {@code HashSet} kosong).
	 */
	@ManyToMany(targetEntity = KegiatanTemporary.class, cascade = { CascadeType.MERGE,
			CascadeType.PERSIST })
	@JoinTable(name = "jatelindo_request_has_kegiatan_temporary", joinColumns = @JoinColumn(name = "jatelindo_request") , inverseJoinColumns = @JoinColumn(name = "kegiatan_temporary") )
	public Set<KegiatanTemporary> getKegiatanTemporarys() {
		return kegiatanTemporarys;
	}

	/**
	 * @param kegiatanTemporarys kumpulan kegiatan temporary yang baru untuk transaksi ini.
	 */
	public void setKegiatanTemporarys(Set<KegiatanTemporary> kegiatanTemporarys) {
		this.kegiatanTemporarys = kegiatanTemporarys;
	}

	/**
	 * Konstruktor default (dipakai Hibernate).
	 */
	public JatelindoRequest() {
	}

	/**
	 * @return id unik (primary key, auto-increment) baris {@code jatelindo_request} ini.
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
	 * @return session id transaksi Jatelindo untuk request ini (kolom fisik {@code session_id}), sudah
	 * di-{@code trim()}, atau {@code null} jika belum diset. Nama getter ini menyesatkan (bukan nama
	 * orang) -- lihat catatan keamanan pada Javadoc kelas.
	 */
	@Column(name = "session_id", columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * @param nama session id transaksi Jatelindo yang baru untuk request ini.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return id transaksi (trx id) Jatelindo untuk request ini, sudah di-{@code trim()}; string kosong
	 * jika belum diset (bukan {@code null}).
	 */
	public String getTrxId() {
		return trxId == null ? "" : trxId.trim();
	}

	/**
	 * @param trxId id transaksi Jatelindo yang baru.
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
	 * @return balasan gateway Jatelindo ({@link JatelindoResponse}) untuk request ini, atau {@code null}
	 * jika gateway belum membalas.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jatelindo_response", nullable = true)
	public JatelindoResponse getJatelindoResponse() {
		return jatelindoResponse;
	}

	/**
	 * @param jatelindoResponse balasan gateway Jatelindo yang baru untuk request ini.
	 */
	public void setJatelindoResponse(JatelindoResponse jatelindoResponse) {
		this.jatelindoResponse = jatelindoResponse;
	}

	/**
	 * @return nama bank/merchant tujuan pembayaran. Jika {@link #jatelindoResponse} sudah ada, nilai
	 * disinkronkan (efek samping: field {@link #merchant} ikut termutasi) dari
	 * {@code JatelindoResponse.getMerchant()}; jika hasilnya tetap kosong/null, method ini memberi
	 * fallback {@code "Mandiri"} (lihat catatan arsitektur pada Javadoc kelas: Jatelindo di AIS dipakai
	 * terutama sebagai switching VA Bank Mandiri).
	 */
	public String getMerchant() {
		if (jatelindoResponse != null) {
			merchant = jatelindoResponse.getMerchant();
		}

		return merchant == null || merchant.trim().isEmpty() ? "Mandiri" : merchant;
	}

	/**
	 * @param merchant nama bank/merchant tujuan pembayaran yang baru; dapat tertimpa kembali oleh
	 * {@link #getMerchant()} selama {@link #jatelindoResponse} tidak null.
	 */
	public void setMerchant(String merchant) {
		this.merchant = merchant;
	}

	/**
	 * @return nominal yang diminta untuk dibayarkan lewat Jatelindo; {@code 0.0} (bukan {@code null})
	 * jika belum pernah diset -- pemanggilan getter ini juga melakukan lazy-init pada field
	 * {@link #amount}.
	 */
	public Double getAmount() {
		if (amount == null) {
			amount = 0.0;
		}
		return amount;
	}

	/**
	 * @param amount nominal yang baru untuk dibayarkan lewat Jatelindo.
	 */
	public void setAmount(Double amount) {
		this.amount = amount;
	}

	/**
	 * @return payload request mentah utuh yang dikirim ke Jatelindo, sebagaimana adanya (tanpa
	 * fallback) -- lihat catatan keamanan pada Javadoc kelas soal penyimpanan payload H2H mentah.
	 */
	@Column(columnDefinition = "text")
	public String getRequest() {
		return request;
	}

	/**
	 * @param request payload request mentah yang baru.
	 */
	public void setRequest(String request) {
		this.request = request;
	}

	/**
	 * @return id merchant Jatelindo yang menerima pembayaran, sebagaimana adanya (tanpa fallback).
	 */
	public String getMerchant_id() {
		return merchant_id;
	}

	/**
	 * @param merchant_id id merchant Jatelindo yang baru.
	 */
	public void setMerchant_id(String merchant_id) {
		this.merchant_id = merchant_id;
	}

	/**
	 * @return {@code true} jika cicilan sebelumnya untuk tagihan yang sama harus dihapus sebelum
	 * request baru dibuat; default {@code true} (bukan {@code null}) jika belum pernah diset --
	 * pemanggilan getter ini juga melakukan lazy-init pada field {@link #hapusCicilanSebelumnya}.
	 */
	public Boolean getHapusCicilanSebelumnya() {
		if (hapusCicilanSebelumnya == null) {
			hapusCicilanSebelumnya = true;
		}
		return hapusCicilanSebelumnya;
	}

	/**
	 * @param hapusCicilanSebelumnya flag baru untuk menghapus cicilan sebelumnya.
	 */
	public void setHapusCicilanSebelumnya(Boolean hapusCicilanSebelumnya) {
		this.hapusCicilanSebelumnya = hapusCicilanSebelumnya;
	}

	/**
	 * @return nominal biaya administrasi. Jika belum diset atau bernilai (hampir) nol (&lt; 0.01),
	 * method ini mengambil default dari konfigurasi global {@code jatelindo_biaya_administrasi} lewat
	 * {@code Common.getKonfigurasi(...)} (efek samping: field {@link #biayaAdministrasi} ikut
	 * termutasi); kegagalan parsing konfigurasi ditangkap dan dicatat lewat {@code ErrorAuditUtil}
	 * tanpa membuat getter ini gagal/melempar. Tidak pernah mengembalikan {@code null} ({@code 0.0}
	 * sebagai nilai akhir terburuk).
	 */
	@Column(name = "biaya_administrasi")
	public Double getBiayaAdministrasi() {
		if (biayaAdministrasi == null || biayaAdministrasi < 0.01) {
			try {
				biayaAdministrasi = Double
						.parseDouble(Common.getKonfigurasi("jatelindo_biaya_administrasi", "0.0").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/jatelindo/JatelindoRequest.java:321");

			}
		}
		return biayaAdministrasi == null ? 0.0 : biayaAdministrasi;
	}

	/**
	 * @param biayaAdministrasi nominal biaya administrasi yang baru.
	 */
	public void setBiayaAdministrasi(Double biayaAdministrasi) {
		this.biayaAdministrasi = biayaAdministrasi;
	}

	/**
	 * @return status transaksi menurut catatan AIS; default {@code "Belum diproses"} (bukan
	 * {@code null}) jika belum pernah diset.
	 */
	public String getStatus() {
		return status == null ? "Belum diproses" : status;
	}

	/**
	 * @param status status transaksi yang baru.
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * @return kode status transaksi menurut catatan AIS; default {@code "0"} (bukan {@code null}) jika
	 * belum pernah diset.
	 */
	public String getKodeStatus() {
		return kodeStatus == null ? "0" : kodeStatus;
	}

	/**
	 * @param kodeStatus kode status transaksi yang baru.
	 */
	public void setKodeStatus(String kodeStatus) {
		this.kodeStatus = kodeStatus;
	}

	/**
	 * @return nominal biaya payment gateway. Jika belum diset atau bernilai (hampir) nol (&lt; 0.01),
	 * method ini mengambil default dari konfigurasi global {@code jatelindo_biaya_payment_gateway}
	 * lewat {@code Common.getKonfigurasi(...)} (efek samping: field {@link #biayaPaymentGateway} ikut
	 * termutasi); kegagalan parsing konfigurasi ditangkap dan dicatat lewat {@code ErrorAuditUtil}
	 * tanpa membuat getter ini gagal/melempar. Tidak pernah mengembalikan {@code null} ({@code 0.0}
	 * sebagai nilai akhir terburuk).
	 */
	@Column(name = "biaya_payment_gateway")
	public Double getBiayaPaymentGateway() {
		if (biayaPaymentGateway == null || biayaPaymentGateway < 0.01) {
			try {
				biayaPaymentGateway = Double
						.parseDouble(Common.getKonfigurasi("jatelindo_biaya_payment_gateway", "0.0").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/jatelindo/JatelindoRequest.java:354");

			}
		}
		return biayaPaymentGateway == null ? 0.0 : biayaPaymentGateway;
	}

	/**
	 * @param biayaPaymentGateway nominal biaya payment gateway yang baru.
	 */
	public void setBiayaPaymentGateway(Double biayaPaymentGateway) {
		this.biayaPaymentGateway = biayaPaymentGateway;
	}
}
