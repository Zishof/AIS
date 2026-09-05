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

	/**
	 * Mengambil ID pengguna yang terakhir mengubah baris ini. Field SHADOW dari riwayat Envers
	 * ({@code @Audited} pada kelas ini) &mdash; KEHARUSAN TEKNIS untuk menampilkan "terakhir diubah
	 * oleh siapa" secara murah di layar daftar, bukan duplikasi yang keliru.
	 *
	 * @return ID pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi ID pengguna yang mengubah baris ini; nilai kosong/blank sengaja diabaikan.
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
	 * {@link #getTanggal_dirubah()} secara otomatis.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi manual waktu terakhir baris ini diubah; dalam praktiknya disegarkan otomatis lewat
	 * {@link #onUpdate()}.
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
	 * @return string gabungan ID dan nama langkah uji.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	private Integer nomorUrut;
	private KriteriaAuditSPI kriteriaAuditSPI;
	private String nama;
	private String keterangan;
	private Boolean aktif;

	/** Konstruktor tanpa argumen, wajib ada agar Hibernate dapat menginstansiasi entity ini. */
	public ChecklistAuditSPI() {
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
	 * Teks langkah uji/pertanyaan konkret yang diwakili baris ini &mdash; lihat javadoc kelas
	 * untuk contoh lengkap. Nilai dikembalikan sudah di-{@code trim()}. PENTING: teks di sini akan
	 * DISALIN (snapshot) ke {@link TemuanAuditSPI#getChecklistSnapshot()} setiap kali temuan baru
	 * dicatat &mdash; lihat javadoc kelas bagian "Prinsip SNAPSHOT" untuk alasan lengkapnya.
	 *
	 * @return teks langkah uji yang sudah dipangkas spasinya, atau {@code null} bila belum diisi.
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi teks langkah uji ini.
	 *
	 * @param nama teks langkah uji baru.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas tambahan mengenai langkah uji ini, mis. panduan teknis pelaksanaan
	 * pemeriksaan.
	 *
	 * @return teks keterangan, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas untuk langkah uji ini.
	 *
	 * @param keterangan teks keterangan baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Nomor urut tampil langkah uji ini di dalam satu {@link KriteriaAuditSPI} &mdash; lihat
	 * javadoc kelas. Default 1 bila belum diisi.
	 *
	 * @return nomor urut langkah uji; 1 bila nilai tersimpan {@code null}.
	 */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Mengisi nomor urut tampil langkah uji ini.
	 *
	 * @param nomorUrut nomor urut baru.
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Status aktif/nonaktif baris ini; nilai {@code null} SENGAJA diperlakukan sebagai
	 * {@code true} (aktif) demi kompatibilitas data lama &mdash; konvensi baku entity "data
	 * master sederhana" di aplikasi ini.
	 *
	 * @return {@code true} bila langkah uji ini aktif (termasuk saat nilai tersimpan {@code null}).
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengubah status aktif/nonaktif langkah uji ini. Menonaktifkan (bukan menghapus) menjaga
	 * integritas referensial baris {@link TemuanAuditSPI} yang sudah pernah mengacu ke sini
	 * &mdash; lagipula teks temuan historis sudah ter-snapshot sehingga tidak bergantung pada
	 * baris ini tetap aktif.
	 *
	 * @param aktif status baru; {@code null} diperlakukan sebagai aktif oleh {@link #getAktif()}.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Kriteria (level 2) yang menaungi langkah uji ini &mdash; lihat javadoc kelas untuk hierarki
	 * lengkap. Relasi wajib ({@code nullable = false}).
	 *
	 * @return kriteria induk langkah uji ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kriteria_audit_spi", nullable = false)
	public KriteriaAuditSPI getKriteriaAuditSPI() {
		kriteriaAuditSPI = check(kriteriaAuditSPI);
		return kriteriaAuditSPI;
	}

	/**
	 * Mengaitkan langkah uji ini ke satu kriteria induk.
	 *
	 * @param kriteriaAuditSPI kriteria induk yang baru.
	 */
	public void setKriteriaAuditSPI(KriteriaAuditSPI kriteriaAuditSPI) {
		this.kriteriaAuditSPI = kriteriaAuditSPI;
	}

}
