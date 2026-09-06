package ais.database.model.antarjemput;

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
import ais.database.model.Pegawai;

/**
 * Entitas Hibernate untuk tabel {@code public.transaksi_penjemputan_antar_jemput},
 * merepresentasikan satu transaksi/kejadian penjemputan siswa pada modul layanan antar-jemput.
 * Satu baris tercipta saat kartu penjemput ({@link #getKartuPenjemputAntarJemput()}) atau siswa
 * di-scan ({@link #getWaktuScan()}, {@link #getTipeScan()}, {@link #getNomorScan()}) di pintu
 * gerbang ({@link #getPintuGerbang()}), lalu mengalir melalui status antrian
 * {@link #MENUNGGU} &rarr; {@link #DIPANGGIL} &rarr; {@link #SELESAI} (atau {@link #DITOLAK})
 * yang dilihat lewat {@link #getStatus()}, dengan nomor antrian tampil di
 * {@link #getNomorAntrian()}.
 * <p>
 * Relasi {@code @ManyToOne} (lazy) ke {@link JadwalAntarJemput} (jadwal antar-jemput terkait),
 * {@link KartuPenjemputAntarJemput} (kartu identitas penjemput yang di-scan), dan
 * {@link Pegawai} sebagai {@link #getSatpam()} (petugas keamanan yang memproses transaksi ini
 * di gerbang).
 * <p>
 * Perubahan (create/update) tercatat historisnya lewat anotasi {@link Audited} (Hibernate
 * Envers), dan setiap update otomatis memperbarui {@link #getTanggal_dirubah()} lewat callback
 * {@link javax.persistence.PreUpdate} yang memanggil
 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "transaksi_penjemputan_antar_jemput")
public class TransaksiPenjemputanAntarJemput extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439816L;

	/** Status transaksi: baru dibuat, menunggu dipanggil. */
	public static final String MENUNGGU = "MENUNGGU";
	/** Status transaksi: siswa/nomor antrian sudah dipanggil. */
	public static final String DIPANGGIL = "DIPANGGIL";
	/** Status transaksi: proses penjemputan selesai. */
	public static final String SELESAI = "SELESAI";
	/** Status transaksi: penjemputan ditolak (mis. kartu/identitas tidak valid). */
	public static final String DITOLAK = "DITOLAK";

	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	private String kode;
	private String nama;
	private String keterangan;
	private Date waktuScan;
	private String tipeScan;
	private String nomorScan;
	private String pintuGerbang;
	private String nomorAntrian;
	private String status;

	private JadwalAntarJemput jadwalAntarJemput;
	private KartuPenjemputAntarJemput kartuPenjemputAntarJemput;
	private Pegawai satpam;

	/** Konstruktor default (dibutuhkan Hibernate). */
	public TransaksiPenjemputanAntarJemput() {
	}

	/** @return ID unik baris transaksi (primary key, auto-increment via {@code IDENTITY}). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/** @param id lihat {@link #getId()}. Normalnya tidak perlu diisi manual — dihasilkan DB saat insert. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return ID pengguna (username) yang terakhir mengubah baris ini. Field audit shadow — lihat {@link #getOleh()}. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Setter {@link #getOlehId()}. Nilai kosong/blank diabaikan (no-op) agar jejak audit lama
	 * tidak tertimpa saat proses simpan tidak membawa identitas pengguna — pola baku di semua
	 * entitas modul antarjemput.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/** @return nama pengguna yang terakhir mengubah baris ini (field audit shadow, diisi via {@link #onUpdate()}). */
	public String getOleh() {
		return oleh;
	}

	/** Setter {@link #getOleh()}. Nilai kosong/blank diabaikan (no-op), sama seperti {@link #setOlehId(String)}. */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum baris ini
	 * di-UPDATE, memperbarui {@link #tanggal_dirubah} (dan field audit terkait) lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** @return timestamp terakhir baris ini diubah; diisi otomatis saat objek dibuat dan diperbarui via {@link #onUpdate()}. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @param tanggal_dirubah lihat {@link #getTanggal_dirubah()}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return kode singkat transaksi ini, di-trim; {@code null} bila belum diisi. Berbeda dari {@link #getNomorAntrian()} — di servlet {@code AntarJemput}, kode diisi manual dengan format {@code "AJ-" + timestamp}, sedangkan nomor antrian dihasilkan terpisah lewat penghitungan baris. */
	@Column(name = "kode", length = 50)
	public String getKode() {
		return kode == null ? null : kode.trim();
	}

	/** @param kode lihat {@link #getKode()}. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/** @return nama tampilan transaksi; bila belum diisi manual, fallback (tanpa di-cache ke field) ke {@link #getNomorAntrian()}. */
	@Column(name = "nama", length = 255)
	public String getNama() {
		return nama == null ? getNomorAntrian() : nama.trim();
	}

	/** @param nama lihat {@link #getNama()}. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan/catatan bebas untuk transaksi ini (mis. alasan penolakan bila {@link #getStatus()} adalah {@link #DITOLAK}). */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return keterangan;
	}

	/** @param keterangan lihat {@link #getKeterangan()}. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return waktu kartu/siswa di-scan di gerbang; bila belum di-set, fallback ke waktu saat ini ({@code ais.ui.util.WaktuUtil.getDate()}) — TIDAK di-cache ke field, sehingga setiap pemanggilan getter sebelum field terisi akan mengembalikan waktu yang sedikit berbeda (nilai final sesungguhnya ditentukan oleh pemanggil lewat {@link #setWaktuScan(Date)} sebelum baris ini dipersist). */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuScan() {
		return waktuScan == null ? ais.ui.util.WaktuUtil.getDate() : waktuScan;
	}

	/** @param waktuScan lihat {@link #getWaktuScan()}. */
	public void setWaktuScan(Date waktuScan) {
		this.waktuScan = waktuScan;
	}

	/** @return tipe pemindaian yang memicu transaksi ini (mis. {@code "KARTU_QR_RFID"} yang dipakai servlet {@code AntarJemput}); default {@code "KARTU"} bila belum di-set (tidak di-cache ke field). */
	@Column(name = "tipe_scan", length = 30)
	public String getTipeScan() {
		return tipeScan == null ? "KARTU" : tipeScan;
	}

	/** @param tipeScan lihat {@link #getTipeScan()}. */
	public void setTipeScan(String tipeScan) {
		this.tipeScan = tipeScan;
	}

	/** @return nilai mentah nomor/kode yang dipindai (nomor kartu, barcode, atau input manual) — dicatat apa adanya untuk audit walau kartu tidak ditemukan/tidak valid (lihat {@link #getStatus()} {@link #DITOLAK}). */
	@Column(name = "nomor_scan", length = 255)
	public String getNomorScan() {
		return nomorScan;
	}

	/** @param nomorScan lihat {@link #getNomorScan()}. */
	public void setNomorScan(String nomorScan) {
		this.nomorScan = nomorScan;
	}

	/** @return label pintu gerbang tempat transaksi ini terjadi (mis. "Gerbang Utama"), format teks bebas — dipakai untuk mendukung beberapa titik gerbang fisik sekaligus. */
	@Column(name = "pintu_gerbang", length = 100)
	public String getPintuGerbang() {
		return pintuGerbang;
	}

	/** @param pintuGerbang lihat {@link #getPintuGerbang()}. */
	public void setPintuGerbang(String pintuGerbang) {
		this.pintuGerbang = pintuGerbang;
	}

	/** @return nomor antrian tampil untuk transaksi ini (mis. {@code "AJ-12"}), dihasilkan sekali saat transaksi dibuat — lihat {@code AntarJemput.generateNomorAntrian} (dihitung dari jumlah baris {@link TransaksiPenjemputanAntarJemput} yang sudah ada, BUKAN sequence DB — berpotensi bentrok/tidak stabil bila ada penghapusan baris atau akses konkuren tinggi, namun ini di luar cakupan paket model). */
	@Column(name = "nomor_antrian", length = 50)
	public String getNomorAntrian() {
		return nomorAntrian;
	}

	/** @param nomorAntrian lihat {@link #getNomorAntrian()}. */
	public void setNomorAntrian(String nomorAntrian) {
		this.nomorAntrian = nomorAntrian;
	}

	/** @return status transaksi; default {@link #MENUNGGU} bila belum di-set (tidak di-cache ke field). */
	@Column(name = "status", length = 30)
	public String getStatus() {
		return status == null ? MENUNGGU : status;
	}

	/** @param status lihat {@link #getStatus()}; nilai valid: {@link #MENUNGGU}, {@link #DIPANGGIL}, {@link #SELESAI}, {@link #DITOLAK}. Tidak divalidasi terhadap konstanta ini maupun urutan transisi oleh setter. */
	public void setStatus(String status) {
		this.status = status;
	}

	/** @return jadwal antar-jemput terkait transaksi ini, bila diketahui saat scan (relasi lazy); {@code null} bila scan tidak menyertakan parameter jadwal (lihat {@code AntarJemput.loadJadwal}). Dilewatkan {@code check()} agar proxy Hibernate yang sudah dihapus/tidak valid tidak ikut terekspos ke pemanggil. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jadwal_antar_jemput")
	public JadwalAntarJemput getJadwalAntarJemput() {
		jadwalAntarJemput = check(jadwalAntarJemput);
		return jadwalAntarJemput;
	}

	/** @param jadwalAntarJemput lihat {@link #getJadwalAntarJemput()}. */
	public void setJadwalAntarJemput(JadwalAntarJemput jadwalAntarJemput) {
		this.jadwalAntarJemput = jadwalAntarJemput;
	}

	/** @return kartu identitas penjemput yang di-scan untuk transaksi ini (relasi lazy); {@code null} bila kartu tidak ditemukan/tidak valid (transaksi tetap dibuat dengan status {@link #DITOLAK} untuk audit — lihat catatan keamanan pada javadoc {@link KartuPenjemputAntarJemput} dan {@link DetailPenjemputanAntarJemput}). Dilewatkan {@code check()} agar proxy Hibernate yang sudah dihapus/tidak valid tidak ikut terekspos ke pemanggil. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kartu_penjemput_antar_jemput")
	public KartuPenjemputAntarJemput getKartuPenjemputAntarJemput() {
		kartuPenjemputAntarJemput = check(kartuPenjemputAntarJemput);
		return kartuPenjemputAntarJemput;
	}

	/** @param kartuPenjemputAntarJemput lihat {@link #getKartuPenjemputAntarJemput()}. */
	public void setKartuPenjemputAntarJemput(KartuPenjemputAntarJemput kartuPenjemputAntarJemput) {
		this.kartuPenjemputAntarJemput = kartuPenjemputAntarJemput;
	}

	/** @return petugas keamanan (satpam) yang memproses transaksi ini di gerbang, bila dicatat (relasi lazy); umumnya tidak diisi otomatis oleh servlet kiosk {@code AntarJemput} (yang beroperasi tanpa akun staf login) — field ini lebih relevan bila transaksi diinput/diubah lewat layar staf {@code AntarJemputAction}. Dilewatkan {@code check()} agar proxy Hibernate yang sudah dihapus/tidak valid tidak ikut terekspos ke pemanggil. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satpam")
	public Pegawai getSatpam() {
		satpam = check(satpam);
		return satpam;
	}

	/** @param satpam lihat {@link #getSatpam()}. */
	public void setSatpam(Pegawai satpam) {
		this.satpam = satpam;
	}
}
