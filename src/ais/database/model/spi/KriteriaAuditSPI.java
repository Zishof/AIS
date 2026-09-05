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

	/**
	 * Mengambil ID pengguna yang terakhir mengubah baris ini. Field ini SHADOW dari jejak audit
	 * lengkap yang sudah tercatat oleh Hibernate Envers ({@code @Audited} pada kelas ini) &mdash;
	 * KEHARUSAN TEKNIS, bukan duplikasi keliru, karena menyediakan cara murah menampilkan "terakhir
	 * diubah oleh siapa" di layar daftar tanpa query terpisah ke tabel riwayat Envers.
	 *
	 * @return ID pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi ID pengguna yang mengubah baris ini; nilai kosong/blank sengaja diabaikan agar tidak
	 * menimpa jejak yang sudah tercatat sebelumnya.
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
	 * {@link #getTanggal_dirubah()} secara otomatis tanpa perlu kode aplikasi mengelolanya manual.
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
	 * Representasi teks singkat baris ini (format {@code "<id>-<nama>"}) untuk log/debug.
	 *
	 * @return string gabungan ID dan nama kriteria.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	private Integer nomorUrut;
	private JenisAuditSPI jenisAuditSPI;
	private String nama;
	private String keterangan;
	private Boolean aktif;

	/** Konstruktor tanpa argumen, wajib ada agar Hibernate dapat menginstansiasi entity ini. */
	public KriteriaAuditSPI() {
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
	 * Nama standar/aturan acuan yang diwakili baris ini (mis. "Kepatuhan SOP Pengelolaan Kas
	 * Kecil") &mdash; lihat javadoc kelas untuk contoh lengkap. Nilai dikembalikan sudah
	 * di-{@code trim()} untuk membersihkan spasi tak sengaja dari input pengguna.
	 *
	 * @return nama kriteria yang sudah dipangkas spasinya, atau {@code null} bila belum diisi.
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama kriteria ini.
	 *
	 * @param nama nama kriteria baru.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas tambahan, lazim dipakai mencantumkan rujukan sumber aturan (nomor
	 * peraturan, pasal, atau dokumen SOP internal terkait) &mdash; lihat javadoc kelas.
	 *
	 * @return teks keterangan, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas untuk kriteria ini.
	 *
	 * @param keterangan teks keterangan baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Nomor urut tampil kriteria ini di dalam satu {@link JenisAuditSPI} &mdash; lihat penjelasan
	 * lengkap di javadoc kelas. Default 1 bila belum diisi.
	 *
	 * @return nomor urut kriteria; 1 bila nilai tersimpan {@code null}.
	 */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Mengisi nomor urut tampil kriteria ini.
	 *
	 * @param nomorUrut nomor urut baru.
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Status aktif/nonaktif baris ini; nilai {@code null} SENGAJA diperlakukan sebagai
	 * {@code true} (aktif) demi kompatibilitas data lama sebelum kolom ini ada &mdash; konvensi
	 * yang konsisten dipakai di seluruh entity "data master sederhana" aplikasi ini.
	 *
	 * @return {@code true} bila kriteria ini aktif (termasuk saat nilai tersimpan {@code null}).
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengubah status aktif/nonaktif kriteria ini. Menonaktifkan (bukan menghapus) adalah cara
	 * yang dianjurkan agar tidak merusak integritas referensial baris {@link ChecklistAuditSPI}
	 * yang sudah pernah mengacu ke sini.
	 *
	 * @param aktif status baru; {@code null} diperlakukan sebagai aktif oleh {@link #getAktif()}.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Jenis audit (level 1) yang menaungi kriteria ini &mdash; lihat javadoc kelas untuk hierarki
	 * lengkap. Relasi wajib ({@code nullable = false}): setiap kriteria HARUS berada di bawah satu
	 * kategori jenis audit.
	 *
	 * @return jenis audit induk kriteria ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_audit_spi", nullable = false)
	public JenisAuditSPI getJenisAuditSPI() {
		jenisAuditSPI = check(jenisAuditSPI);
		return jenisAuditSPI;
	}

	/**
	 * Mengaitkan kriteria ini ke satu jenis audit induk.
	 *
	 * @param jenisAuditSPI jenis audit induk yang baru.
	 */
	public void setJenisAuditSPI(JenisAuditSPI jenisAuditSPI) {
		this.jenisAuditSPI = jenisAuditSPI;
	}

}
