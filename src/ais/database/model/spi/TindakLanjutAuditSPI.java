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

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * <h2>TindakLanjutAuditSPI &mdash; Realisasi Tindak Lanjut Auditee atas Satu Temuan</h2>
 *
 * <p>
 * Mencatat apa yang SESUNGGUHNYA DILAKUKAN unit yang diaudit (auditee) dalam merespons
 * {@link TemuanAuditSPI#getRekomendasi() rekomendasi} yang diberikan auditor. Satu temuan bisa
 * memiliki BANYAK baris tindak lanjut &mdash; dicatat progresif dari waktu ke waktu seiring
 * pelaksanaan perbaikan berjalan (mis. "diajukan draf SOP baru" bulan ini, "SOP baru disahkan"
 * bulan berikutnya), sehingga riwayat perkembangan penanganan satu temuan tetap lengkap terekam,
 * bukan hanya status akhir. Lihat javadoc {@link TemuanAuditSPI} untuk penjelasan lengkap kenapa
 * Rekomendasi (auditor) dan Tindak Lanjut (auditee) SENGAJA dipisah menjadi dua entity berbeda.
 * </p>
 *
 * <p>
 * Struktur field kelas ini SENGAJA meniru persis {@code ais.database.model.spmi.
 * TindakLanjutTemuanSPMI} yang sudah production-proven di modul Audit Mutu Internal akademik,
 * karena kebutuhannya memang identik: deskripsi tindakan, penanggung jawab (PIC), target &amp;
 * tanggal realisasi, persentase kemajuan, dan status generik (Belum Dimulai/Sedang Berjalan/
 * Terlambat/Selesai) yang tidak spesifik-domain sehingga bisa dipakai apa adanya di konteks audit
 * internal maupun audit mutu akademik.
 * </p>
 *
 * @author e-Campus SPI Team
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "tindak_lanjut_audit_spi")
public class TindakLanjutAuditSPI extends GeneralValueObject {

	public static final String BELUM_DIMULAI = "Belum Dimulai";
	public static final String SEDANG_BERJALAN = "Sedang Berjalan";
	public static final String TERLAMBAT = "Terlambat";
	public static final String SELESAI = "Selesai";

	public static final Map<String, String> statusLabel = new LinkedHashMap<String, String>();
	static {
		statusLabel.put(BELUM_DIMULAI, BELUM_DIMULAI);
		statusLabel.put(SEDANG_BERJALAN, SEDANG_BERJALAN);
		statusLabel.put(TERLAMBAT, TERLAMBAT);
		statusLabel.put(SELESAI, SELESAI);
	}

	private static final long serialVersionUID = 1L;
	private Long id;
	private TemuanAuditSPI temuanAuditSPI;
	private String deskripsi;
	private String picNama;
	private Date targetDate;
	private Date tanggalSelesai;
	private int progressPersen;
	private String status;
	private String keterangan;
	private Boolean aktif;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public TindakLanjutAuditSPI() {
	}

	public TindakLanjutAuditSPI(TemuanAuditSPI temuanAuditSPI) {
		this.temuanAuditSPI = temuanAuditSPI;
	}

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "temuan_audit_spi", nullable = false)
	public TemuanAuditSPI getTemuanAuditSPI() {
		temuanAuditSPI = check(temuanAuditSPI);
		return temuanAuditSPI;
	}

	public void setTemuanAuditSPI(TemuanAuditSPI temuanAuditSPI) {
		if (temuanAuditSPI != null && temuanAuditSPI.getId() != null) {
			this.temuanAuditSPI = temuanAuditSPI;
		}
	}

	@Column(name = "deskripsi", nullable = false, columnDefinition = "text")
	public String getDeskripsi() {
		return deskripsi == null ? "" : deskripsi.trim();
	}

	public void setDeskripsi(String deskripsi) {
		this.deskripsi = deskripsi;
	}

	@Column(name = "pic_nama")
	public String getPicNama() {
		return picNama;
	}

	public void setPicNama(String picNama) {
		this.picNama = picNama;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "target_date")
	public Date getTargetDate() {
		return targetDate;
	}

	public void setTargetDate(Date targetDate) {
		this.targetDate = targetDate;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_selesai")
	public Date getTanggalSelesai() {
		return tanggalSelesai;
	}

	public void setTanggalSelesai(Date tanggalSelesai) {
		this.tanggalSelesai = tanggalSelesai;
	}

	@Column(name = "progress_persen", nullable = false)
	public int getProgressPersen() {
		return progressPersen;
	}

	public void setProgressPersen(int progressPersen) {
		this.progressPersen = Math.max(0, Math.min(100, progressPersen));
	}

	@Column(name = "status")
	public String getStatus() {
		return status == null ? BELUM_DIMULAI : status.trim();
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Column(name = "keterangan", columnDefinition = "text")
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

	public String getOleh() {
		return oleh;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) return;
		this.oleh = oleh;
	}

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) return;
		this.olehId = olehId;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Override
	public String toString() {
		return id + "-" + deskripsi;
	}
}
