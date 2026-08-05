package ais.database.model.spi;

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
 * <h2>KriteriaAuditSPI &mdash; Standar/Aturan Acuan Audit</h2>
 *
 * <p>
 * Level 2 dari hierarki data master checklist audit SPI (lihat javadoc {@link JenisAuditSPI}
 * untuk gambaran lengkap 3 tingkat hierarkinya). Satu baris di sini merepresentasikan SATU
 * standar/aturan yang menjadi acuan pemeriksaan &mdash; misalnya "Kepatuhan SOP Pengelolaan Kas
 * Kecil", "Rekonsiliasi Bank Bulanan", "Otorisasi Berjenjang untuk Pengadaan di Atas Nilai
 * Tertentu", atau kutipan pasal dari peraturan yang relevan (mis. SPIP/Sistem Pengendalian Intern
 * Pemerintah, Permendikbud, atau SOP internal lembaga). Field {@link #getNama()} diisi teks bebas
 * yang menjelaskan standar tersebut, dan field {@link #getKeterangan()} bisa dipakai untuk
 * mencantumkan rujukan sumber aturan (nomor peraturan, pasal, atau dokumen SOP internal terkait).
 * </p>
 *
 * <h3>Relasi satu-ke-banyak yang disengaja</h3>
 * <p>
 * Satu {@link JenisAuditSPI} (mis. "Audit Keuangan") lazimnya memiliki BANYAK kriteria (mis.
 * "Kas Kecil", "Rekonsiliasi Bank", "Piutang", "Utang", dst.), dan pada gilirannya satu kriteria di
 * sini juga lazim diturunkan menjadi BANYAK langkah uji konkret di level {@link ChecklistAuditSPI}
 * (mis. kriteria "Kas Kecil" bisa menghasilkan beberapa langkah uji: "Periksa saldo fisik vs
 * catatan", "Periksa otorisasi pengeluaran", "Periksa rekonsiliasi akhir bulan"). Karena hubungan
 * satu-ke-banyak yang nyata dan bermakna pada kedua sisinya inilah kriteria dan checklist TETAP
 * dipisah menjadi dua tabel/level, bukan digabung menjadi satu &mdash; menggabungkannya akan
 * memaksa satu baris mewakili dua konsep berbeda (aturan vs langkah uji) sehingga pencarian dan
 * pelaporan per-kriteria menjadi tidak jelas.
 * </p>
 *
 * <h3>Field {@link #getNomorUrut()}</h3>
 * <p>
 * Menentukan urutan tampil kriteria di dalam satu jenis audit pada layar Setup SPI maupun saat
 * checklist dirender di layar pelaksanaan audit, sehingga staf SPI dapat menyusun urutan
 * pemeriksaan sesuai alur kerja yang logis (mis. dari yang paling berisiko/prioritas tinggi lebih
 * dulu), bukan sekadar urutan alfabetis atau urutan pembuatan data.
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
@Table(schema = "public", name = "kriteria_audit_spi")
public class KriteriaAuditSPI extends GeneralValueObject {

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
		return id + "-" + nama;
	}

	private Integer nomorUrut;
	private JenisAuditSPI jenisAuditSPI;
	private String nama;
	private String keterangan;
	private Boolean aktif;

	public KriteriaAuditSPI() {
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

	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	public Integer getNomorUrut() {
		return nomorUrut == null ? 1 : nomorUrut;
	}

	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
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

}
