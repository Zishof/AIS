package ais.database.model.inventory;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * <h2>SesiKasKasir — Sesi Kas Kasir (Buka / Tutup Kas) untuk Toko/Kantin.</h2>
 *
 * <p>
 * Entity BARU untuk mencatat <b>buka</b> dan <b>tutup kas</b> harian tiap kasir, sehingga akurasi kas
 * dapat dikontrol (laporan "Buka Tutup Kasir" dan "Selisih Kasir" pada katalog laporan Toko/Kantin).
 * Sebelumnya sistem hanya punya rekap penjualan per kasir (dari transaksi POS), namun belum ada
 * konsep <i>sesi kas</i> dengan modal awal, uang fisik saat tutup, dan selisih. Dengan adanya entity
 * ini + pendaftaran di {@code hibernate.cfg.xml}, tabel {@code koperasi.sesi_kas_kasir} otomatis
 * dibuat (hbm2ddl=update).
 * </p>
 *
 * <h3>Alur & perhitungan</h3>
 * <ul>
 *   <li><b>Buka kas:</b> kasir membuka sesi dengan mengisi <i>modal awal</i>; {@code status}=BUKA,
 *       {@code waktuBuka}=sekarang.</li>
 *   <li><b>Tutup kas:</b> saat menutup, sistem menghitung <i>total tunai</i> dan <i>total non‑tunai</i>
 *       dari transaksi POS oleh kasir yang sama dalam rentang {@code waktuBuka}..{@code waktuTutup}
 *       (dicocokkan lewat {@link #getKasirNama()}/{@link #getKasirUserId()} = identitas kasir pada
 *       transaksi -- BUKAN {@code oleh}/{@code olehId}, itu audit generik, lihat javadoc
 *       {@link #getKasirNama()}), lalu kasir mengisi <i>uang fisik</i> yang dihitung. <b>Selisih</b> =
 *       uangFisik − (modalAwal + totalTunai). {@code status}=TUTUP.</li>
 * </ul>
 *
 * <p>
 * Penamaan kolom mengikuti aturan Hibernate proyek ini: field ber-@Column memakai nama eksplisit,
 * sementara field numerik/tanggal tanpa @Column ter-<i>fold</i> menjadi huruf kecil tanpa underscore
 * (mis. {@code modalAwal}→{@code modalawal}, {@code totalTunai}→{@code totaltunai},
 * {@code uangFisik}→{@code uangfisik}, {@code waktuBuka}→{@code waktubuka}). Kompatibel Java 1.7 /
 * Hibernate 3.
 * </p>
 *
 * @author AIS e-Kantin (modul kas kasir)
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "sesi_kas_kasir")
public class SesiKasKasir extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	/** Status sesi masih terbuka (kas belum ditutup). */
	public static final String STATUS_BUKA = "BUKA";
	/** Status sesi sudah ditutup (kas selesai dihitung). */
	public static final String STATUS_TUTUP = "TUTUP";

	private Long id;
	private Toko toko;
	private String oleh;
	private String olehId;
	private String kasirNama;
	private String kasirUserId;
	private String idPerangkat;
	private String namaPerangkat;
	private Date waktuBuka;
	private Date waktuTutup;
	private Double modalAwal;
	private Double totalTunai;
	private Double totalNonTunai;
	private Double uangFisik;
	private Double selisih;
	private String status;
	private String keterangan;
	private String kode;
	private String laporanTutupJson;

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public SesiKasKasir() {
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko")
	public Toko getToko() {
		toko = check(toko);
		return toko;
	}

	public void setToko(Toko toko) {
		this.toko = toko;
	}

	public String getOleh() {
		return oleh;
	}

	public void setOleh(String oleh) {
		this.oleh = oleh;
	}

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		this.olehId = olehId;
	}

	/**
	 * Nama kasir yang membuka sesi ini -- SENGAJA field baru terpisah dari {@link #getOleh()} (itu
	 * metadata audit generik "siapa terakhir mengubah baris", diisi otomatis oleh interceptor/listener
	 * Hibernate di {@code ais.database.hibernate}, TIDAK dimaksudkan untuk dibaca sebagai data bisnis).
	 * Sebelum field ini ada, {@code SesiKasUtil.buka()} menyalahgunakan {@code oleh}/{@code olehId}
	 * untuk menyimpan identitas kasir -- interceptor audit generik menimpanya diam-diam sebelum baris
	 * tersimpan (root cause bug "Kas Terbuka tapi checkout ditolak", lihat commit 869f858d), karena
	 * kedua nama itu kebetulan sama dengan nama kolom audit. Pola field terpisah ini mengikuti preseden
	 * {@link ais.database.model.koperasi.PembelianAnggotaKoperasi#getKasirLoginNama()}.
	 */
	@Column(name = "kasir_nama", nullable = true)
	public String getKasirNama() {
		return kasirNama;
	}

	public void setKasirNama(String kasirNama) {
		this.kasirNama = kasirNama;
	}

	/** Pasangan id (userId) dari {@link #getKasirNama()} -- lihat javadoc di sana. */
	@Column(name = "kasir_user_id", nullable = true)
	public String getKasirUserId() {
		return kasirUserId;
	}

	public void setKasirUserId(String kasirUserId) {
		this.kasirUserId = kasirUserId;
	}

	/**
	 * Identitas instalasi/perangkat yang membuka sesi. Nilai ini dibuat sekali oleh aplikasi POS
	 * dan tetap sama setelah aplikasi dibuka ulang. Sesi kas baru wajib terikat ke perangkat agar
	 * akun yang sama pada mesin lain tidak dapat memakai sesi ini secara tidak sengaja.
	 */
	@Column(name = "id_perangkat", nullable = true, length = 128)
	public String getIdPerangkat() {
		return idPerangkat;
	}

	public void setIdPerangkat(String idPerangkat) {
		this.idPerangkat = idPerangkat;
	}

	/** Nama perangkat saat sesi dibuka; snapshot untuk informasi operator dan audit. */
	@Column(name = "nama_perangkat", nullable = true, length = 150)
	public String getNamaPerangkat() {
		return namaPerangkat;
	}

	public void setNamaPerangkat(String namaPerangkat) {
		this.namaPerangkat = namaPerangkat;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuBuka() {
		return waktuBuka == null ? ais.ui.util.WaktuUtil.getDate() : waktuBuka;
	}

	public void setWaktuBuka(Date waktuBuka) {
		this.waktuBuka = waktuBuka;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuTutup() {
		return waktuTutup;
	}

	public void setWaktuTutup(Date waktuTutup) {
		this.waktuTutup = waktuTutup;
	}

	public Double getModalAwal() {
		return modalAwal == null ? 0.0 : modalAwal;
	}

	public void setModalAwal(Double modalAwal) {
		this.modalAwal = modalAwal;
	}

	public Double getTotalTunai() {
		return totalTunai == null ? 0.0 : totalTunai;
	}

	public void setTotalTunai(Double totalTunai) {
		this.totalTunai = totalTunai;
	}

	public Double getTotalNonTunai() {
		return totalNonTunai == null ? 0.0 : totalNonTunai;
	}

	public void setTotalNonTunai(Double totalNonTunai) {
		this.totalNonTunai = totalNonTunai;
	}

	public Double getUangFisik() {
		return uangFisik == null ? 0.0 : uangFisik;
	}

	public void setUangFisik(Double uangFisik) {
		this.uangFisik = uangFisik;
	}

	public Double getSelisih() {
		return selisih == null ? 0.0 : selisih;
	}

	public void setSelisih(Double selisih) {
		this.selisih = selisih;
	}

	public String getStatus() {
		return status == null ? STATUS_BUKA : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Kode idempotensi yang DIBUAT KLIEN (bukan server) -- fondasi fitur "Sesi Kasir offline-first"
	 * (Desktop/Android menyimpan sesi ke database LOKAL dulu, baru disinkronkan ke server belakangan
	 * secara berkala/otomatis, termasuk saat baru online kembali setelah sempat offline). Sinkron bisa
	 * dicoba ULANG (retry) kapan saja -- {@code kode} inilah yang membuat percobaan ulang AMAN: server
	 * ({@code KantinHelper.sesiKasBuka}) mengecek dulu apakah baris dgn {@code kode} ini SUDAH ada
	 * sebelum membuat baris baru, jadi retry jaringan yang gagal di tengah jalan (respons hilang tapi
	 * baris sebenarnya sudah tersimpan) tidak pernah menghasilkan sesi DOBEL. {@code null} utk sesi
	 * lama/sebelum fitur ini ada, atau sesi yang dibuat langsung dari versi web (JSP/ZK) yang belum
	 * (dan tidak perlu) memakai alur offline-first ini.
	 */
	@Column(name = "kode", unique = true, length = 100)
	public String getKode() {
		return kode;
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Snapshot laporan saat kas ditutup. Disimpan sebagai JSON agar cetak ulang selalu memakai
	 * angka yang telah direkonsiliasi pada saat penutupan, bukan menghitung ulang data yang mungkin
	 * sudah berubah karena retur atau koreksi setelah shift berakhir.
	 */
	@Column(name = "laporan_tutup_json", columnDefinition = "text")
	public String getLaporanTutupJson() {
		return laporanTutupJson;
	}

	public void setLaporanTutupJson(String laporanTutupJson) {
		this.laporanTutupJson = laporanTutupJson;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
