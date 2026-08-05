package ais.database.model.spi;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * <h2>JenisAuditSPI &mdash; Kategori Besar Audit Internal</h2>
 *
 * <p>
 * Kelas ini merepresentasikan level PALING ATAS dari hierarki data master checklist audit milik
 * modul Satuan Pengawasan Internal (SPI): sebuah kategori besar yang menjadi payung bagi seluruh
 * kriteria dan langkah uji di bawahnya. Contoh isi baris pada tabel ini: "Audit Keuangan", "Audit
 * Pengadaan Barang/Jasa", "Audit Sumber Daya Manusia", "Audit Aset/Barang Milik", "Audit
 * Operasional/Akademik", dan "Audit Teknologi Informasi". Kategori-kategori ini sengaja dibuat
 * bebas/dinamis (bukan enum tetap di kode) agar staf SPI bisa menambah jenis audit baru sendiri
 * tanpa perlu perubahan program, sesuai kebutuhan lembaga yang bisa berubah dari waktu ke waktu.
 * </p>
 *
 * <h3>Kedudukan dalam hierarki data master</h3>
 * <p>
 * Struktur data master checklist audit SPI dibuat 3 tingkat (BUKAN 5 tingkat seperti modul SPMI
 * yang meniru struktur instrumen akreditasi BAN-PT yang memang berlapis banyak) karena checklist
 * audit internal pada praktiknya cukup direpresentasikan dengan: kategori besar &rarr; standar/
 * kriteria acuan &rarr; langkah uji konkret yang dicentang di lapangan. Tiga tingkat itu adalah:
 * </p>
 * <ol>
 *   <li>{@link JenisAuditSPI} (kelas ini) &mdash; kategori besar, level 1.</li>
 *   <li>{@link KriteriaAuditSPI} &mdash; standar/aturan acuan di bawah satu jenis audit, level 2.
 *       Satu jenis audit bisa memiliki banyak kriteria.</li>
 *   <li>{@link ChecklistAuditSPI} &mdash; langkah uji/pertanyaan konkret di bawah satu kriteria,
 *       level 3 (paling bawah/daun). Inilah yang benar-benar diperiksa satu per satu oleh auditor
 *       saat pelaksanaan audit, dan menghasilkan satu baris temuan per checklist per penugasan.</li>
 * </ol>
 *
 * <h3>Mengapa perlu tabel tersendiri (bukan enum/konstanta kode)</h3>
 * <p>
 * Jenis-jenis audit yang relevan bagi satu lembaga bisa berbeda dari lembaga lain, dan bisa
 * berkembang seiring waktu (mis. penambahan "Audit Keberlanjutan/ESG" di kemudian hari). Dengan
 * menyimpannya sebagai data (bukan kode terkompilasi), staf SPI dapat menambah/menonaktifkan
 * kategori lewat layar Setup SPI tanpa melibatkan pengembang, sekaligus tetap tercatat riwayat
 * perubahannya lewat anotasi {@code @Audited} (Hibernate Envers) yang dipasang di kelas ini.
 * </p>
 *
 * <h3>Pola implementasi</h3>
 * <p>
 * Struktur field, anotasi, dan konvensi penamaan kolom kelas ini mengikuti pola baku yang dipakai
 * seluruh entity "data master sederhana" di aplikasi ini (id auto-increment, jejak audit
 * {@code oleh}/{@code olehId}/{@code tanggal_dirubah} yang diisi otomatis oleh
 * {@code AuditTimestampInterceptor} lewat callback JPA {@code @PreUpdate}, field {@code aktif}
 * bertipe {@code Boolean} yang default-nya dianggap true bila null demi kompatibilitas data lama).
 * Pola ini SENGAJA disamakan dengan kelas-kelas sejenis lain di aplikasi (mis.
 * {@code ais.database.model.spmi.JenisSPMI}) supaya staf pengembang yang sudah familiar dengan
 * satu modul dapat langsung memahami modul lain tanpa belajar konvensi baru.
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
@Table(schema = "public", name = "jenis_audit_spi")
public class JenisAuditSPI extends GeneralValueObject {

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

	private String kode;
	private String nama;
	private String keterangan;
	private Boolean aktif;

	public JenisAuditSPI() {
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

	@Column(name = "kode", nullable = true, columnDefinition = "text")
	public String getKode() {
		return kode;
	}

	public void setKode(String kode) {
		this.kode = kode;
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

	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
