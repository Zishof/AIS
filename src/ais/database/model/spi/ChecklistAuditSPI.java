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
 * <h2>ChecklistAuditSPI &mdash; Langkah Uji Konkret (Level Daun)</h2>
 *
 * <p>
 * Level 3 (paling bawah/daun) dari hierarki data master checklist audit SPI &mdash; lihat javadoc
 * {@link JenisAuditSPI} untuk gambaran lengkap 3 tingkat hierarkinya. Satu baris di sini adalah
 * SATU langkah uji/pertanyaan konkret yang benar-benar dicentang/diperiksa satu per satu oleh
 * auditor di lapangan saat pelaksanaan satu penugasan audit &mdash; misalnya di bawah kriteria
 * "Kepatuhan SOP Kas Kecil": "Periksa apakah saldo kas kecil fisik sesuai dengan catatan
 * pembukuan", "Periksa apakah setiap pengeluaran kas kecil memiliki otorisasi berjenjang sesuai
 * nilai nominalnya", "Periksa apakah kas kecil direkonsiliasi setiap akhir bulan".
 * </p>
 *
 * <h3>Bagaimana kelas ini akan dipakai di bagian pelaksanaan audit (fase berikutnya)</h3>
 * <p>
 * Saat sebuah penugasan audit dijalankan, SETIAP baris {@link ChecklistAuditSPI} yang aktif di
 * bawah jenis audit yang dipilih akan dirender sebagai satu baris pada formulir pemeriksaan, dan
 * auditor mengisi hasil pemeriksaannya (kondisi, klasifikasi masalah, dst.) yang tersimpan sebagai
 * satu baris "Temuan Audit" terhubung ke checklist ini.
 * </p>
 *
 * <h3>Prinsip penting: SNAPSHOT, bukan hanya referensi hidup</h3>
 * <p>
 * Meski setiap temuan audit nantinya akan menyimpan foreign key ke baris {@link ChecklistAuditSPI}
 * yang diperiksa (untuk keperluan penelusuran/laporan agregat), teks kondisi/kriteria/temuan yang
 * sesungguhnya akan DISALIN (di-<i>snapshot</i>) ke tabel temuan pada saat audit dilaksanakan,
 * BUKAN hanya dibaca ulang dari tabel ini setiap kali laporan dibuka. Alasannya: data master
 * checklist ini bisa berubah dari waktu ke waktu (kriteria diperbarui, redaksi diperhalus, atau
 * bahkan dinonaktifkan) sesuai kebutuhan organisasi yang terus berkembang &mdash; namun riwayat
 * temuan audit yang SUDAH TERJADI di masa lalu tidak boleh ikut berubah maknanya hanya karena
 * master checklist diedit belakangan. Ini prinsip dasar pencatatan audit trail yang baik: dokumen
 * historis harus tetap merepresentasikan kondisi PADA SAAT dokumen itu dibuat.
 * </p>
 *
 * <h3>Field {@link #getNomorUrut()} dan struktur turunan</h3>
 * <p>
 * Sama seperti {@link KriteriaAuditSPI}, field ini menentukan urutan tampil di dalam satu kriteria,
 * sehingga urutan pemeriksaan di lapangan mengikuti alur kerja yang sudah dirancang staf SPI
 * (mis. dari pemeriksaan dokumen dulu, baru pemeriksaan fisik), bukan urutan acak.
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
@Table(schema = "public", name = "checklist_audit_spi")
public class ChecklistAuditSPI extends GeneralValueObject {

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
	private KriteriaAuditSPI kriteriaAuditSPI;
	private String nama;
	private String keterangan;
	private Boolean aktif;

	public ChecklistAuditSPI() {
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
	@JoinColumn(name = "kriteria_audit_spi", nullable = false)
	public KriteriaAuditSPI getKriteriaAuditSPI() {
		kriteriaAuditSPI = check(kriteriaAuditSPI);
		return kriteriaAuditSPI;
	}

	public void setKriteriaAuditSPI(KriteriaAuditSPI kriteriaAuditSPI) {
		this.kriteriaAuditSPI = kriteriaAuditSPI;
	}

}
