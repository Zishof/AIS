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

	public TemuanAuditSPI() {
	}

	public TemuanAuditSPI(ChecklistAuditSPI checklistAuditSPI, PenugasanAuditSPI penugasanAuditSPI) {
		this.checklistAuditSPI = checklistAuditSPI;
		this.penugasanAuditSPI = penugasanAuditSPI;
		if (checklistAuditSPI != null) {
			this.checklistSnapshot = checklistAuditSPI.getNama();
			this.kriteriaSnapshot = checklistAuditSPI.getKriteriaAuditSPI() == null
					? null : checklistAuditSPI.getKriteriaAuditSPI().getNama();
		}
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

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "penugasan_audit_spi", nullable = false)
	public PenugasanAuditSPI getPenugasanAuditSPI() {
		penugasanAuditSPI = check(penugasanAuditSPI);
		return penugasanAuditSPI;
	}

	public void setPenugasanAuditSPI(PenugasanAuditSPI penugasanAuditSPI) {
		this.penugasanAuditSPI = penugasanAuditSPI;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "checklist_audit_spi", nullable = false)
	public ChecklistAuditSPI getChecklistAuditSPI() {
		checklistAuditSPI = check(checklistAuditSPI);
		return checklistAuditSPI;
	}

	public void setChecklistAuditSPI(ChecklistAuditSPI checklistAuditSPI) {
		this.checklistAuditSPI = checklistAuditSPI;
	}

	@Column(name = "checklist_snapshot", nullable = true, columnDefinition = "text")
	public String getChecklistSnapshot() {
		return checklistSnapshot;
	}

	public void setChecklistSnapshot(String checklistSnapshot) {
		this.checklistSnapshot = checklistSnapshot;
	}

	@Column(name = "kriteria_snapshot", nullable = true, columnDefinition = "text")
	public String getKriteriaSnapshot() {
		return kriteriaSnapshot;
	}

	public void setKriteriaSnapshot(String kriteriaSnapshot) {
		this.kriteriaSnapshot = kriteriaSnapshot;
	}

	@Column(name = "kondisi", nullable = true, columnDefinition = "text")
	public String getKondisi() {
		return kondisi;
	}

	public void setKondisi(String kondisi) {
		this.kondisi = kondisi;
	}

	@Column(name = "sebab", nullable = true, columnDefinition = "text")
	public String getSebab() {
		return sebab;
	}

	public void setSebab(String sebab) {
		this.sebab = sebab;
	}

	@Column(name = "akibat", nullable = true, columnDefinition = "text")
	public String getAkibat() {
		return akibat;
	}

	public void setAkibat(String akibat) {
		this.akibat = akibat;
	}

	@Column(name = "rekomendasi", nullable = true, columnDefinition = "text")
	public String getRekomendasi() {
		return rekomendasi;
	}

	public void setRekomendasi(String rekomendasi) {
		this.rekomendasi = rekomendasi;
	}

	@Column(name = "klasifikasi", nullable = true, length = 20)
	public String getKlasifikasi() {
		return klasifikasi;
	}

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

	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
