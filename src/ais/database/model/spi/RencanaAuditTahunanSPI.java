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

	/**
	 * Mengambil ID pengguna yang terakhir mengubah baris ini. Field SHADOW dari riwayat Envers
	 * ({@code @Audited} pada kelas ini) &mdash; KEHARUSAN TEKNIS untuk menampilkan "terakhir diubah
	 * oleh siapa" secara murah di layar daftar tanpa query terpisah ke tabel riwayat revisi.
	 *
	 * @return ID pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi ID pengguna yang mengubah baris ini; nilai kosong/blank sengaja diabaikan agar
	 * tidak menimpa jejak yang sudah tercatat.
	 *
	 * @param olehId ID pengguna; {@code null} atau string kosong/spasi diabaikan.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna yang mengubah baris ini; nilai kosong/blank sengaja diabaikan.
	 *
	 * @param oleh nama pengguna; {@code null} atau string kosong/spasi diabaikan.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}, dipanggil otomatis Hibernate sebelum UPDATE, mendelegasikan
	 * ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} untuk menyegarkan
	 * {@link #getTanggal_dirubah()} secara otomatis, tanpa kode aplikasi perlu mengelolanya manual.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi manual waktu terakhir baris ini diubah; dalam praktiknya disegarkan otomatis lewat
	 * {@link #onUpdate()} pada tiap UPDATE.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil waktu terakhir baris ini diubah.
	 *
	 * @return waktu perubahan terakhir baris ini.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat baris rencana ini untuk log/debug, format
	 * {@code "<nama unit> - <tahun> TW<triwulan>"}, mis. "Fakultas Teknik - 2026 TW2".
	 *
	 * @return string gabungan nama unit kerja, tahun, dan triwulan rencana; "-" bila unit kerja
	 *         atau namanya belum diisi.
	 */
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

	/** Konstruktor tanpa argumen, wajib ada agar Hibernate dapat menginstansiasi entity ini. */
	public RencanaAuditTahunanSPI() {
	}

	/**
	 * ID primer baris ini, di-generate otomatis oleh database (strategi {@code IDENTITY}).
	 *
	 * @return ID unik baris ini, atau {@code null} bila entity belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi ID baris ini secara manual, terutama saat membangun objek referensi ringan untuk
	 * relasi {@code JoinColumn} tanpa memuat seluruh baris dari database.
	 *
	 * @param id ID baris yang akan diisi.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Tahun anggaran/perencanaan yang diwakili baris PKPT ini &mdash; satu unit kerja bisa punya
	 * banyak baris rencana untuk tahun yang berbeda-beda, masing-masing menjadi baris terpisah.
	 *
	 * @return tahun rencana audit.
	 */
	@Column(name = "tahun", nullable = false)
	public Integer getTahun() {
		return tahun;
	}

	/**
	 * Mengisi tahun rencana audit ini.
	 *
	 * @param tahun tahun baru.
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Unit kerja (auditee) yang direncanakan akan diaudit pada baris PKPT ini. Relasi wajib
	 * ({@code nullable = false}) ke {@link SatuanKerja}, entity unit organisasi generik lintas
	 * eCampus/eSchool yang juga dipakai modul RAB/Anggaran &mdash; lihat javadoc
	 * {@link ProfilRisikoSPI} bagian "Mengapa memakai SatuanKerja" untuk alasan lengkap tidak
	 * membuat tabel unit organisasi baru khusus SPI.
	 *
	 * @return unit kerja yang direncanakan diaudit.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = false)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Mengaitkan baris rencana ini ke satu unit kerja.
	 *
	 * @param satuanKerja unit kerja baru yang direncanakan diaudit.
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Jenis/kategori audit yang direncanakan dilaksanakan pada unit kerja ini (mis. "Audit
	 * Keuangan", "Audit Pengadaan"). Relasi wajib ({@code nullable = false}): satu baris rencana
	 * HARUS menyatakan jenis audit apa yang dimaksud, karena satu unit kerja bisa direncanakan
	 * diaudit dengan lebih dari satu jenis audit dalam tahun yang sama (masing-masing menjadi
	 * baris rencana terpisah).
	 *
	 * @return jenis audit yang direncanakan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_audit_spi", nullable = false)
	public JenisAuditSPI getJenisAuditSPI() {
		jenisAuditSPI = check(jenisAuditSPI);
		return jenisAuditSPI;
	}

	/**
	 * Mengisi jenis audit yang direncanakan pada baris ini.
	 *
	 * @param jenisAuditSPI jenis audit baru.
	 */
	public void setJenisAuditSPI(JenisAuditSPI jenisAuditSPI) {
		this.jenisAuditSPI = jenisAuditSPI;
	}

	/**
	 * Tautan OPSIONAL ke profil/penilaian risiko yang menjadi dasar keputusan memasukkan unit ini
	 * ke PKPT &mdash; lihat javadoc kelas bagian "Mengapa getProfilRisikoSPI() adalah tautan
	 * opsional, bukan wajib" untuk penjelasan lengkap kenapa field ini {@code nullable = true}
	 * (berbeda dari {@link #getSatuanKerja()}/{@link #getJenisAuditSPI()} yang wajib): baris
	 * dengan {@link #JENIS_PENUGASAN_DATA jenis penugasan} {@link #KHUSUS} sah tidak memilikinya.
	 *
	 * @return profil risiko dasar penyusunan rencana ini, atau {@code null} bila rencana ini
	 *         bersifat {@link #KHUSUS} (insidental, di luar hasil pemeringkatan risiko terjadwal).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "profil_risiko_spi", nullable = true)
	public ProfilRisikoSPI getProfilRisikoSPI() {
		profilRisikoSPI = check(profilRisikoSPI);
		return profilRisikoSPI;
	}

	/**
	 * Mengaitkan baris rencana ini ke profil risiko yang menjadi dasar penyusunannya.
	 *
	 * @param profilRisikoSPI profil risiko baru; boleh {@code null} untuk rencana jenis {@link #KHUSUS}.
	 */
	public void setProfilRisikoSPI(ProfilRisikoSPI profilRisikoSPI) {
		this.profilRisikoSPI = profilRisikoSPI;
	}

	/**
	 * Triwulan (1&ndash;4) kapan kira-kira audit ini direncanakan dilaksanakan &mdash; lihat
	 * javadoc kelas bagian "Triwulan sebagai satuan perencanaan, bukan tanggal presisi" untuk
	 * alasan lengkap kenapa granularitasnya sengaja sekasar ini. Default 1 bila belum diisi.
	 *
	 * @return nomor triwulan rencana (1&ndash;4); 1 bila nilai tersimpan {@code null}.
	 */
	@Column(name = "triwulan_rencana", nullable = false)
	public Integer getTriwulanRencana() {
		return triwulanRencana == null ? 1 : triwulanRencana;
	}

	/**
	 * Mengisi triwulan rencana pelaksanaan audit ini.
	 *
	 * @param triwulanRencana nomor triwulan baru (idealnya 1&ndash;4).
	 */
	public void setTriwulanRencana(Integer triwulanRencana) {
		this.triwulanRencana = triwulanRencana;
	}

	/**
	 * Kode jalur masuknya baris ini ke rencana kerja tahunan ({@link #REGULER} atau
	 * {@link #KHUSUS}) &mdash; lihat javadoc kelas bagian "Dua jenis penugasan" untuk penjelasan
	 * lengkap perbedaan keduanya. Default {@link #REGULER} bila belum diisi.
	 *
	 * @return kode jenis penugasan; {@link #REGULER} bila nilai tersimpan {@code null}.
	 */
	@Column(name = "jenis_penugasan", nullable = false, length = 20)
	public String getJenisPenugasan() {
		return jenisPenugasan == null ? REGULER : jenisPenugasan;
	}

	/**
	 * Mengisi kode jenis penugasan baris ini.
	 *
	 * @param jenisPenugasan kode baru, idealnya salah satu dari {@link #REGULER}/{@link #KHUSUS}.
	 */
	public void setJenisPenugasan(String jenisPenugasan) {
		this.jenisPenugasan = jenisPenugasan;
	}

	/** Label bahasa manusia dari {@link #getJenisPenugasan()}, dipakai langsung oleh tampilan. */
	@Transient
	public String getJenisPenugasanLabel() {
		String label = JENIS_PENUGASAN_DATA.get(getJenisPenugasan());
		return label == null ? getJenisPenugasan() : label;
	}

	/**
	 * Status realisasi pelaksanaan rencana ini ({@link #BELUM_DILAKSANAKAN}/
	 * {@link #SEDANG_BERJALAN}/{@link #SELESAI}). CATATAN: field ini murni deskriptif dan diisi
	 * manual/oleh proses lain &mdash; TIDAK otomatis tersinkron dari status
	 * {@link PenugasanAuditSPI#getStatus()} pada penugasan riil yang mengacu ke rencana ini lewat
	 * {@link PenugasanAuditSPI#getRencanaAuditTahunanSPI()}, sehingga staf SPI perlu memastikan
	 * field ini diperbarui manual seiring pelaksanaan penugasan berjalan. Default
	 * {@link #BELUM_DILAKSANAKAN} bila belum diisi.
	 *
	 * @return kode status realisasi; {@link #BELUM_DILAKSANAKAN} bila nilai tersimpan {@code null}.
	 */
	@Column(name = "status_realisasi", nullable = false, length = 20)
	public String getStatusRealisasi() {
		return statusRealisasi == null ? BELUM_DILAKSANAKAN : statusRealisasi;
	}

	/**
	 * Mengisi status realisasi baris rencana ini.
	 *
	 * @param statusRealisasi kode status baru.
	 */
	public void setStatusRealisasi(String statusRealisasi) {
		this.statusRealisasi = statusRealisasi;
	}

	/** Label bahasa manusia dari {@link #getStatusRealisasi()}, dipakai langsung oleh tampilan. */
	@Transient
	public String getStatusRealisasiLabel() {
		String label = STATUS_REALISASI_DATA.get(getStatusRealisasi());
		return label == null ? getStatusRealisasi() : label;
	}

	/**
	 * Keterangan bebas tambahan mengenai baris rencana ini, terutama dipakai mencatat alasan/
	 * pemicu untuk rencana jenis {@link #KHUSUS} (mis. nomor pengaduan, instruksi pimpinan) karena
	 * baris jenis ini tidak memiliki {@link #getProfilRisikoSPI()} sebagai konteks &mdash; lihat
	 * javadoc kelas.
	 *
	 * @return teks keterangan, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Mengisi keterangan bebas untuk baris rencana ini.
	 *
	 * @param keterangan teks keterangan baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Status aktif/nonaktif baris rencana ini; nilai {@code null} SENGAJA diperlakukan sebagai
	 * {@code true} (aktif) demi kompatibilitas data lama &mdash; konvensi baku entity "data
	 * master sederhana" di aplikasi ini.
	 *
	 * @return {@code true} bila baris rencana ini aktif (termasuk saat nilai tersimpan {@code null}).
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengubah status aktif/nonaktif baris rencana ini. Menonaktifkan (bukan menghapus) menjaga
	 * integritas referensial baris {@link PenugasanAuditSPI} yang sudah pernah mengacu ke sini
	 * lewat {@link PenugasanAuditSPI#getRencanaAuditTahunanSPI()}.
	 *
	 * @param aktif status baru; {@code null} diperlakukan sebagai aktif oleh {@link #getAktif()}.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
