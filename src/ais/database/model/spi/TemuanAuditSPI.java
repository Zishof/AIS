package ais.database.model.spi;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

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

/**
 * <h2>TemuanAuditSPI &mdash; Satu Temuan Audit dengan Struktur 5-Unsur Standar Profesi</h2>
 *
 * <p>
 * Kelas ini merepresentasikan SATU temuan audit &mdash; hasil pemeriksaan satu langkah uji
 * ({@link ChecklistAuditSPI}) pada satu pelaksanaan penugasan ({@link PenugasanAuditSPI}). Struktur
 * field kelas ini mengikuti format baku laporan temuan audit profesional yang dipakai luas baik di
 * standar audit Indonesia (AAIPI) maupun internasional (IIA), dikenal sebagai
 * "5-unsur temuan": {@link #getKondisi()} (fakta yang ditemukan), {@link #getKriteriaSnapshot()}
 * (Kriteria/standar acuan yang seharusnya dipenuhi), {@link #getSebab()} (akar masalah),
 * {@link #getAkibat()} (dampak/risiko yang timbul), dan {@link #getRekomendasi()} (langkah
 * perbaikan yang disarankan auditor). Format 5-unsur ini memaksa auditor berpikir runtut
 * (fakta &rarr; standar &rarr; sebab &rarr; dampak &rarr; solusi), bukan sekadar mencatat "ada
 * masalah" tanpa analisis, sehingga laporan yang dihasilkan lebih actionable bagi auditee.
 * </p>
 *
 * <h3>Rekomendasi (auditor) BUKAN Tindak Lanjut (auditee) &mdash; dua hal yang sengaja dipisah</h3>
 * <p>
 * {@link #getRekomendasi()} adalah usulan perbaikan yang DITULIS AUDITOR pada saat temuan dicatat
 * &mdash; bagian dari struktur 5-unsur di atas. Ini SENGAJA dipisah dari
 * {@link TindakLanjutAuditSPI}, yang mencatat apa yang SESUNGGUHNYA DILAKUKAN auditee dalam
 * merespons rekomendasi tersebut (bisa lebih dari satu entri, dicatat progresif dari waktu ke
 * waktu). Draf awal desain modul ini sempat mencampur kedua konsep ini menjadi satu field,
 * yang keliru secara metodologi audit: auditor merekomendasikan, auditee yang menindaklanjuti, dan
 * keduanya perlu tercatat terpisah agar kepatuhan auditee terhadap rekomendasi bisa dipantau secara
 * obyektif (mis. "rekomendasi sudah diberikan 3 bulan lalu, tindak lanjut belum ada satupun").
 * </p>
 *
 * <h3>Field snapshot: {@link #getKriteriaSnapshot()} dan {@link #getChecklistSnapshot()}</h3>
 * <p>
 * Selain menyimpan foreign key hidup ke {@link #getChecklistAuditSPI()} (untuk keperluan
 * telusur/rekap by-checklist), kelas ini JUGA menyalin (meng-<i>snapshot</i>) teks kriteria dan
 * checklist yang berlaku PADA SAAT temuan dicatat. Lihat javadoc {@link ChecklistAuditSPI} untuk
 * alasan lengkap prinsip ini: data master checklist bisa berubah/diperbarui dari waktu ke waktu,
 * namun dokumen temuan yang sudah terbit TIDAK BOLEH ikut berubah maknanya. Karena itu SETIAP kali
 * satu temuan baru dibuat, {@link ais.action.master.spi.TemuanAuditSPIAction} WAJIB menyalin teks
 * checklist/kriteria yang berlaku saat itu ke kedua field ini &mdash; bukan hanya mengandalkan
 * pembacaan ulang lewat {@link #getChecklistAuditSPI()} setiap kali laporan dibuka.
 * </p>
 *
 * <h3>Klasifikasi temuan: skala keparahan standar audit, bukan istilah akademik SPMI</h3>
 * <p>
 * {@link #getKlasifikasi()} memakai skala keparahan yang lazim dipakai audit internal umum
 * ({@link #KRITIS}/{@link #MAYOR}/{@link #MINOR}/{@link #OBSERVASI}/{@link #SESUAI}), BERBEDA dari
 * istilah "Sesuai/Melebihi Standar" yang dipakai modul SPMI akademik &mdash; karena SPMI menilai
 * kepatuhan terhadap standar mutu pendidikan (konteks akreditasi), sedangkan SPI menilai risiko
 * kepatuhan/pengendalian internal secara umum (konteks tata kelola &amp; keuangan), dua konteks
 * yang menuntut kosakata keparahan berbeda meski strukturnya (map kode&rarr;label) mengikuti pola
 * teknis yang sama.
 * </p>
 *
 * @author e-Campus SPI Team
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "temuan_audit_spi")
public class TemuanAuditSPI extends GeneralValueObject {

	public static final String KRITIS = "KRITIS";
	public static final String MAYOR = "MAYOR";
	public static final String MINOR = "MINOR";
	public static final String OBSERVASI = "OBSERVASI";
	public static final String SESUAI = "SESUAI";

	public static final Map<String, String> KLASIFIKASI_DATA = new LinkedHashMap<String, String>();
	static {
		KLASIFIKASI_DATA.put(KRITIS, "Kritis");
		KLASIFIKASI_DATA.put(MAYOR, "Mayor");
		KLASIFIKASI_DATA.put(MINOR, "Minor");
		KLASIFIKASI_DATA.put(OBSERVASI, "Observasi");
		KLASIFIKASI_DATA.put(SESUAI, "Sesuai / Tidak Ada Temuan");
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
	 * Representasi teks singkat baris temuan ini (format {@code "<id>-<checklistSnapshot>"})
	 * untuk log/debug, memakai teks checklist yang sudah di-snapshot (bukan dibaca ulang dari
	 * {@link #getChecklistAuditSPI()}) agar tetap konsisten dengan prinsip snapshot temuan.
	 *
	 * @return string gabungan ID dan teks checklist yang di-snapshot pada temuan ini.
	 */
	public String toString() {
		return id + "-" + checklistSnapshot;
	}

	private PenugasanAuditSPI penugasanAuditSPI;
	private ChecklistAuditSPI checklistAuditSPI;
	private String checklistSnapshot;
	private String kriteriaSnapshot;
	private String kondisi;
	private String sebab;
	private String akibat;
	private String rekomendasi;
	private String klasifikasi;
	private Boolean aktif;

	/** Konstruktor tanpa argumen, wajib ada agar Hibernate dapat menginstansiasi entity ini. */
	public TemuanAuditSPI() {
	}

	/**
	 * Konstruktor kenyamanan untuk membuat satu baris temuan baru sekaligus melakukan SNAPSHOT
	 * teks checklist &amp; kriteria yang berlaku PADA SAAT temuan ini dibuat &mdash; lihat javadoc
	 * kelas bagian "Field snapshot" dan javadoc {@link ChecklistAuditSPI} bagian "Prinsip SNAPSHOT"
	 * untuk alasan lengkapnya. Pemanggil (lazimnya
	 * {@link ais.action.master.spi.TemuanAuditSPIAction} saat merender formulir pemeriksaan dari
	 * daftar checklist aktif) TIDAK PERLU mengisi {@link #getChecklistSnapshot()}/
	 * {@link #getKriteriaSnapshot()} secara manual karena konstruktor ini sudah menyalinnya
	 * otomatis dari {@code checklistAuditSPI} yang diberikan.
	 *
	 * @param checklistAuditSPI langkah uji yang diperiksa; boleh {@code null} (snapshot akan
	 *        tetap kosong bila demikian).
	 * @param penugasanAuditSPI penugasan audit tempat temuan ini dicatat.
	 */
	public TemuanAuditSPI(ChecklistAuditSPI checklistAuditSPI, PenugasanAuditSPI penugasanAuditSPI) {
		this.checklistAuditSPI = checklistAuditSPI;
		this.penugasanAuditSPI = penugasanAuditSPI;
		if (checklistAuditSPI != null) {
			this.checklistSnapshot = checklistAuditSPI.getNama();
			this.kriteriaSnapshot = checklistAuditSPI.getKriteriaAuditSPI() == null
					? null : checklistAuditSPI.getKriteriaAuditSPI().getNama();
		}
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
	 * Penugasan audit tempat temuan ini dicatat &mdash; setiap temuan HARUS terkait ke satu
	 * pelaksanaan audit ({@code nullable = false}), sehingga rekap "semua temuan pada satu
	 * penugasan" bisa dihitung akurat.
	 *
	 * @return penugasan audit induk temuan ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "penugasan_audit_spi", nullable = false)
	public PenugasanAuditSPI getPenugasanAuditSPI() {
		penugasanAuditSPI = check(penugasanAuditSPI);
		return penugasanAuditSPI;
	}

	/**
	 * Mengaitkan temuan ini ke satu penugasan audit.
	 *
	 * @param penugasanAuditSPI penugasan audit baru.
	 */
	public void setPenugasanAuditSPI(PenugasanAuditSPI penugasanAuditSPI) {
		this.penugasanAuditSPI = penugasanAuditSPI;
	}

	/**
	 * Foreign key HIDUP ke langkah uji ({@link ChecklistAuditSPI}) yang diperiksa untuk
	 * menghasilkan temuan ini &mdash; dipakai untuk keperluan telusur/rekap by-checklist (mis.
	 * "berapa kali checklist X pernah menghasilkan temuan Kritis di seluruh unit"). PENTING: field
	 * ini BUKAN sumber teks yang ditampilkan di laporan &mdash; teks ditampilkan selalu memakai
	 * {@link #getChecklistSnapshot()}/{@link #getKriteriaSnapshot()} yang sudah di-snapshot, lihat
	 * javadoc kelas bagian "Field snapshot". Relasi wajib ({@code nullable = false}).
	 *
	 * @return langkah uji yang diperiksa untuk temuan ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "checklist_audit_spi", nullable = false)
	public ChecklistAuditSPI getChecklistAuditSPI() {
		checklistAuditSPI = check(checklistAuditSPI);
		return checklistAuditSPI;
	}

	/**
	 * Mengaitkan temuan ini ke satu langkah uji checklist. Mengubah relasi ini SETELAH temuan
	 * dibuat TIDAK otomatis memperbarui {@link #getChecklistSnapshot()}/{@link #getKriteriaSnapshot()}
	 * &mdash; pemanggil yang memang bermaksud mengganti checklist rujukan harus menyalin ulang
	 * teksnya secara eksplisit bila diinginkan, konsisten dengan prinsip snapshot pada kelas ini.
	 *
	 * @param checklistAuditSPI langkah uji baru yang diperiksa.
	 */
	public void setChecklistAuditSPI(ChecklistAuditSPI checklistAuditSPI) {
		this.checklistAuditSPI = checklistAuditSPI;
	}

	/**
	 * Teks langkah uji checklist yang DISALIN (snapshot) pada saat temuan ini dibuat &mdash; lihat
	 * javadoc kelas bagian "Field snapshot". Inilah teks yang SEHARUSNYA ditampilkan di
	 * laporan/cetak temuan, BUKAN hasil pembacaan ulang lewat {@link #getChecklistAuditSPI()},
	 * agar riwayat temuan historis tidak berubah makna hanya karena master checklist diedit
	 * belakangan.
	 *
	 * @return teks checklist yang di-snapshot, atau {@code null} bila belum pernah diisi.
	 */
	@Column(name = "checklist_snapshot", nullable = true, columnDefinition = "text")
	public String getChecklistSnapshot() {
		return checklistSnapshot;
	}

	/**
	 * Mengisi manual teks checklist yang di-snapshot pada temuan ini. Lazimnya diisi otomatis oleh
	 * konstruktor {@link #TemuanAuditSPI(ChecklistAuditSPI, PenugasanAuditSPI)}; setter ini
	 * tersedia untuk kasus penyesuaian/migrasi data manual.
	 *
	 * @param checklistSnapshot teks checklist baru yang akan disimpan sebagai snapshot.
	 */
	public void setChecklistSnapshot(String checklistSnapshot) {
		this.checklistSnapshot = checklistSnapshot;
	}

	/**
	 * Teks kriteria/standar acuan yang DISALIN (snapshot) pada saat temuan ini dibuat, diambil
	 * dari {@link KriteriaAuditSPI} induk checklist yang diperiksa &mdash; lihat javadoc kelas
	 * bagian "Field snapshot" dan struktur "5-unsur temuan" (unsur kedua: kriteria).
	 *
	 * @return teks kriteria yang di-snapshot, atau {@code null} bila belum pernah diisi.
	 */
	@Column(name = "kriteria_snapshot", nullable = true, columnDefinition = "text")
	public String getKriteriaSnapshot() {
		return kriteriaSnapshot;
	}

	/**
	 * Mengisi manual teks kriteria yang di-snapshot pada temuan ini. Lazimnya diisi otomatis oleh
	 * konstruktor {@link #TemuanAuditSPI(ChecklistAuditSPI, PenugasanAuditSPI)}.
	 *
	 * @param kriteriaSnapshot teks kriteria baru yang akan disimpan sebagai snapshot.
	 */
	public void setKriteriaSnapshot(String kriteriaSnapshot) {
		this.kriteriaSnapshot = kriteriaSnapshot;
	}

	/**
	 * Unsur pertama dari struktur "5-unsur temuan" (lihat javadoc kelas): fakta/kondisi riil yang
	 * ditemukan auditor di lapangan saat memeriksa checklist ini &mdash; apa yang SESUNGGUHNYA
	 * terjadi, bukan apa yang seharusnya terjadi (itu bagian {@link #getKriteriaSnapshot()}).
	 *
	 * @return teks kondisi/fakta temuan, atau {@code null} bila belum diisi.
	 */
	@Column(name = "kondisi", nullable = true, columnDefinition = "text")
	public String getKondisi() {
		return kondisi;
	}

	/**
	 * Mengisi teks kondisi/fakta yang ditemukan pada temuan ini.
	 *
	 * @param kondisi teks kondisi baru.
	 */
	public void setKondisi(String kondisi) {
		this.kondisi = kondisi;
	}

	/**
	 * Unsur ketiga dari struktur "5-unsur temuan" (lihat javadoc kelas): akar masalah/penyebab
	 * mengapa {@link #getKondisi()} bisa terjadi, hasil analisis auditor terhadap kesenjangan
	 * antara kondisi dan kriteria.
	 *
	 * @return teks sebab/akar masalah, atau {@code null} bila belum diisi.
	 */
	@Column(name = "sebab", nullable = true, columnDefinition = "text")
	public String getSebab() {
		return sebab;
	}

	/**
	 * Mengisi teks sebab/akar masalah pada temuan ini.
	 *
	 * @param sebab teks sebab baru.
	 */
	public void setSebab(String sebab) {
		this.sebab = sebab;
	}

	/**
	 * Unsur keempat dari struktur "5-unsur temuan" (lihat javadoc kelas): dampak/risiko yang
	 * timbul atau berpotensi timbul akibat {@link #getKondisi()}, dituliskan agar auditee
	 * memahami URGENSI perbaikan (bukan sekadar "ada masalah" tanpa konsekuensi yang jelas).
	 *
	 * @return teks akibat/dampak, atau {@code null} bila belum diisi.
	 */
	@Column(name = "akibat", nullable = true, columnDefinition = "text")
	public String getAkibat() {
		return akibat;
	}

	/**
	 * Mengisi teks akibat/dampak pada temuan ini.
	 *
	 * @param akibat teks akibat baru.
	 */
	public void setAkibat(String akibat) {
		this.akibat = akibat;
	}

	/**
	 * Unsur kelima/terakhir dari struktur "5-unsur temuan" (lihat javadoc kelas): usulan langkah
	 * perbaikan yang DITULIS AUDITOR pada saat temuan dicatat. SENGAJA dipisah dari
	 * {@link TindakLanjutAuditSPI}, yang mencatat apa yang SESUNGGUHNYA DILAKUKAN auditee &mdash;
	 * lihat javadoc kelas bagian "Rekomendasi (auditor) BUKAN Tindak Lanjut (auditee)" untuk
	 * penjelasan lengkap kenapa keduanya tidak boleh dicampur menjadi satu field.
	 *
	 * @return teks rekomendasi perbaikan dari auditor, atau {@code null} bila belum diisi.
	 */
	@Column(name = "rekomendasi", nullable = true, columnDefinition = "text")
	public String getRekomendasi() {
		return rekomendasi;
	}

	/**
	 * Mengisi teks rekomendasi perbaikan pada temuan ini.
	 *
	 * @param rekomendasi teks rekomendasi baru.
	 */
	public void setRekomendasi(String rekomendasi) {
		this.rekomendasi = rekomendasi;
	}

	/**
	 * Kode klasifikasi keparahan temuan ini, salah satu dari {@link #KRITIS}/{@link #MAYOR}/
	 * {@link #MINOR}/{@link #OBSERVASI}/{@link #SESUAI} &mdash; lihat javadoc kelas bagian
	 * "Klasifikasi temuan" untuk penjelasan kenapa skala ini berbeda dari istilah SPMI akademik.
	 * BERBEDA dari kebanyakan field enum-map lain di aplikasi ini, field ini TIDAK memiliki nilai
	 * default non-null (bisa tetap {@code null} bila auditor belum mengklasifikasikan), lihat
	 * {@link #isTerisi()} yang justru memakai kekosongan field ini sebagai salah satu penanda
	 * baris checklist yang belum benar-benar diperiksa.
	 *
	 * @return kode klasifikasi keparahan, atau {@code null} bila belum diklasifikasikan.
	 */
	@Column(name = "klasifikasi", nullable = true, length = 20)
	public String getKlasifikasi() {
		return klasifikasi;
	}

	/**
	 * Mengisi kode klasifikasi keparahan pada temuan ini.
	 *
	 * @param klasifikasi kode baru, idealnya salah satu dari {@link #KRITIS}/{@link #MAYOR}/
	 *        {@link #MINOR}/{@link #OBSERVASI}/{@link #SESUAI}.
	 */
	public void setKlasifikasi(String klasifikasi) {
		this.klasifikasi = klasifikasi;
	}

	/** Label bahasa manusia dari {@link #getKlasifikasi()}; kosong bila belum diklasifikasikan. */
	@Transient
	public String getKlasifikasiLabel() {
		if (klasifikasi == null) return "";
		String label = KLASIFIKASI_DATA.get(klasifikasi);
		return label == null ? klasifikasi : label;
	}

	/** Apakah temuan ini sudah benar-benar diisi (bukan sekadar baris checklist kosong). */
	@Transient
	public boolean isTerisi() {
		return (kondisi != null && !kondisi.trim().isEmpty()) || klasifikasi != null;
	}

	/**
	 * Status aktif/nonaktif baris temuan ini; nilai {@code null} SENGAJA diperlakukan sebagai
	 * {@code true} (aktif) demi kompatibilitas data lama &mdash; konvensi baku entity di aplikasi
	 * ini. Berbeda dari {@link #isTerisi()} yang menandai apakah temuan sudah diisi kontennya,
	 * field ini menandai apakah baris temuan (terisi maupun kosong) masih berlaku/ditampilkan.
	 *
	 * @return {@code true} bila temuan ini aktif (termasuk saat nilai tersimpan {@code null}).
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengubah status aktif/nonaktif baris temuan ini. Menonaktifkan (bukan menghapus) menjaga
	 * integritas referensial baris {@link TindakLanjutAuditSPI} yang sudah pernah mengacu ke sini.
	 *
	 * @param aktif status baru; {@code null} diperlakukan sebagai aktif oleh {@link #getAktif()}.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
