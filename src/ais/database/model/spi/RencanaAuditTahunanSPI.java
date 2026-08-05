package ais.database.model.spi;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

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
import javax.persistence.Transient;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.rab.SatuanKerja;

/**
 * <h2>RencanaAuditTahunanSPI &mdash; Program Kerja Pengawasan Tahunan (PKPT)</h2>
 *
 * <p>
 * Kelas ini merepresentasikan dokumen perencanaan audit tahunan yang di dunia audit internal
 * pemerintah/lembaga Indonesia dikenal sebagai <b>PKPT (Program Kerja Pengawasan Tahunan)</b>
 * &mdash; daftar unit kerja mana saja yang akan diaudit sepanjang satu tahun, jenis audit apa yang
 * akan dilaksanakan di masing-masing unit, dan pada periode/triwulan mana. Satu baris di tabel ini
 * adalah SATU rencana penugasan audit: satu kombinasi (unit kerja, jenis audit, tahun, triwulan).
 * PKPT inilah yang menjadi acuan resmi SPI dalam bekerja sepanjang tahun, dan lazimnya disusun di
 * awal tahun berdasarkan hasil pemetaan risiko ({@link ProfilRisikoSPI}) tahun sebelumnya.
 * </p>
 *
 * <h3>Dua jenis penugasan: Reguler vs Khusus</h3>
 * <p>
 * Praktik terbaik audit internal membedakan dua jalur masuknya sebuah unit ke dalam rencana kerja
 * tahunan (lihat {@link #JENIS_PENUGASAN_DATA}):
 * </p>
 * <ul>
 *   <li><b>{@link #REGULER}</b> &mdash; audit terjadwal yang murni dipilih berdasarkan hasil
 *       pemeringkatan risiko tahunan ({@link #getProfilRisikoSPI()} terisi), mengikuti siklus
 *       perencanaan normal.</li>
 *   <li><b>{@link #KHUSUS}</b> &mdash; audit di luar jadwal reguler yang dipicu kebutuhan mendadak
 *       (mis. pengaduan/whistleblowing, instruksi langsung pimpinan, atau indikasi kuat masalah
 *       yang tidak bisa menunggu siklus perencanaan berikutnya). Untuk jenis ini,
 *       {@link #getProfilRisikoSPI()} boleh kosong karena pemicunya bukan hasil pemeringkatan
 *       risiko terjadwal, melainkan kejadian/insiden tertentu yang dicatat di
 *       {@link #getKeterangan()}.
 *   </li>
 * </ul>
 * <p>
 * Membedakan dua jalur ini penting bagi pelaporan SPI ke pimpinan: proporsi audit khusus yang
 * terlalu tinggi dibanding reguler bisa jadi indikasi bahwa proses perencanaan berbasis risiko
 * belum cukup efektif menangkap area bermasalah sejak awal.
 * </p>
 *
 * <h3>Mengapa {@link #getProfilRisikoSPI()} adalah tautan opsional, bukan wajib</h3>
 * <p>
 * Field ini SENGAJA dibuat {@code nullable = true} (berbeda dari {@link #getSatuanKerja()} dan
 * {@link #getJenisAuditSPI()} yang wajib) karena tidak semua baris rencana berasal dari proses
 * pemeringkatan risiko formal &mdash; lihat penjelasan jenis penugasan "Khusus" di atas. Saat
 * terisi, field ini berfungsi sebagai jejak/bukti keterlacakan (<i>traceability</i>) yang
 * menunjukkan penilaian risiko mana yang menjadi dasar keputusan memasukkan unit tersebut ke PKPT
 * &mdash; kebutuhan umum saat rencana kerja ini nantinya diperiksa oleh auditor eksternal atau
 * pihak yang mengevaluasi efektivitas fungsi SPI itu sendiri.
 * </p>
 *
 * <h3>Triwulan sebagai satuan perencanaan, bukan tanggal presisi</h3>
 * <p>
 * {@link #getTriwulanRencana()} sengaja memakai granularitas triwulan (1&ndash;4), BUKAN tanggal
 * mulai/selesai yang presisi, karena pada tahap PERENCANAAN tahunan, SPI baru menentukan "kapan
 * kira-kira" suatu unit akan diaudit, sedangkan tanggal presisi pelaksanaan sesungguhnya baru
 * ditentukan belakangan saat penugasan benar-benar dijadwalkan (dibangun pada Bagian C:
 * pelaksanaan audit). Memisahkan keduanya mencegah rencana tahunan perlu diedit berulang kali
 * hanya karena jadwal pelaksanaan detail masih bergeser-geser dalam triwulan yang sama.
 * </p>
 *
 * @author e-Campus SPI Team
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "rencana_audit_tahunan_spi")
public class RencanaAuditTahunanSPI extends GeneralValueObject {

	public static final String REGULER = "REGULER";
	public static final String KHUSUS = "KHUSUS";

	public static final Map<String, String> JENIS_PENUGASAN_DATA = new TreeMap<String, String>();
	static {
		JENIS_PENUGASAN_DATA.put(REGULER, "Reguler (Berbasis Risiko)");
		JENIS_PENUGASAN_DATA.put(KHUSUS, "Khusus (Insidental)");
	}

	public static final String BELUM_DILAKSANAKAN = "BELUM_DILAKSANAKAN";
	public static final String SEDANG_BERJALAN = "SEDANG_BERJALAN";
	public static final String SELESAI = "SELESAI";

	public static final Map<String, String> STATUS_REALISASI_DATA = new TreeMap<String, String>();
	static {
		STATUS_REALISASI_DATA.put(BELUM_DILAKSANAKAN, "Belum Dilaksanakan");
		STATUS_REALISASI_DATA.put(SEDANG_BERJALAN, "Sedang Berjalan");
		STATUS_REALISASI_DATA.put(SELESAI, "Selesai");
	}

	private static final long serialVersionUID = 1L;
	private Long id;
	private String oleh;
	private String olehId;

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	public String getOleh() {
		return oleh;
	}

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public String toString() {
		return (satuanKerja == null || satuanKerja.getNama() == null ? "-" : satuanKerja.getNama())
				+ " - " + tahun + " TW" + triwulanRencana;
	}

	private Integer tahun;
	private SatuanKerja satuanKerja;
	private JenisAuditSPI jenisAuditSPI;
	private ProfilRisikoSPI profilRisikoSPI;
	private Integer triwulanRencana;
	private String jenisPenugasan;
	private String statusRealisasi;
	private String keterangan;
	private Boolean aktif;

	public RencanaAuditTahunanSPI() {
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "tahun", nullable = false)
	public Integer getTahun() {
		return tahun;
	}

	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = false)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_audit_spi", nullable = false)
	public JenisAuditSPI getJenisAuditSPI() {
		jenisAuditSPI = check(jenisAuditSPI);
		return jenisAuditSPI;
	}

	public void setJenisAuditSPI(JenisAuditSPI jenisAuditSPI) {
		this.jenisAuditSPI = jenisAuditSPI;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "profil_risiko_spi", nullable = true)
	public ProfilRisikoSPI getProfilRisikoSPI() {
		profilRisikoSPI = check(profilRisikoSPI);
		return profilRisikoSPI;
	}

	public void setProfilRisikoSPI(ProfilRisikoSPI profilRisikoSPI) {
		this.profilRisikoSPI = profilRisikoSPI;
	}

	@Column(name = "triwulan_rencana", nullable = false)
	public Integer getTriwulanRencana() {
		return triwulanRencana == null ? 1 : triwulanRencana;
	}

	public void setTriwulanRencana(Integer triwulanRencana) {
		this.triwulanRencana = triwulanRencana;
	}

	@Column(name = "jenis_penugasan", nullable = false, length = 20)
	public String getJenisPenugasan() {
		return jenisPenugasan == null ? REGULER : jenisPenugasan;
	}

	public void setJenisPenugasan(String jenisPenugasan) {
		this.jenisPenugasan = jenisPenugasan;
	}

	/** Label bahasa manusia dari {@link #getJenisPenugasan()}, dipakai langsung oleh tampilan. */
	@Transient
	public String getJenisPenugasanLabel() {
		String label = JENIS_PENUGASAN_DATA.get(getJenisPenugasan());
		return label == null ? getJenisPenugasan() : label;
	}

	@Column(name = "status_realisasi", nullable = false, length = 20)
	public String getStatusRealisasi() {
		return statusRealisasi == null ? BELUM_DILAKSANAKAN : statusRealisasi;
	}

	public void setStatusRealisasi(String statusRealisasi) {
		this.statusRealisasi = statusRealisasi;
	}

	/** Label bahasa manusia dari {@link #getStatusRealisasi()}, dipakai langsung oleh tampilan. */
	@Transient
	public String getStatusRealisasiLabel() {
		String label = STATUS_REALISASI_DATA.get(getStatusRealisasi());
		return label == null ? getStatusRealisasi() : label;
	}

	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
