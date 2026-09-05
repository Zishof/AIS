package ais.database.model.epsbed;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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
 * Entity JPA/Hibernate master untuk tabel {@code epsbed.epsbed_jabatan_akademik}: daftar kode
 * jabatan akademik dosen (mis. Asisten Ahli, Lektor, Lektor Kepala, Guru Besar) sesuai skema kode
 * yang dipakai format pelaporan EPSBED ke Dikti/Kemdikbud. Baris-barisnya didaftarkan sebagai kelas
 * entity terkelola lewat {@code ais.common.InitData} dan dipetakan langsung sebagai relasi
 * {@code ManyToOne} pada {@code ais.database.model.Dosen#jabatanAkademik}
 * ({@code getJabatanAkademik()}/{@code setJabatanAkademik(EpsbedJabatanAkademik)}).
 *
 * <p><b>Relasi terverifikasi dengan jabatan fungsional dosen di {@code Dosen}:</b> entity ini
 * BUKAN pengganti maupun bentuk lama dari {@code JabatanFungsionalDosen} (hierarki fungsional resmi
 * asisten ahli s.d. guru besar yang dipakai untuk keperluan kepegawaian/tunjangan). Javadoc class
 * {@code Dosen} secara eksplisit mendaftarkan keduanya sebagai dua field/relasi terpisah dalam
 * kelompok "Jabatan": {@code JabatanFungsionalDosen} untuk jabatan fungsional resmi, dan
 * {@code EpsbedJabatanAkademik} ini khusus "untuk pelaporan" — yaitu skema kode jabatan akademik versi
 * EPSBED, yang tidak selalu satu-satu dengan skema {@code JabatanFungsionalDosen} karena keduanya
 * dikelola sebagai tabel lookup independen. Karena tidak ada mapping otomatis antara keduanya di kode
 * model ini, operator yang mengisi {@link ais.database.model.Dosen#getJabatanAkademik()
 * Dosen.jabatanAkademik} bertanggung jawab menjaga kesesuaiannya secara manual dengan jabatan
 * fungsional resmi dosen yang bersangkutan.</p>
 *
 * <p><b>Field audit shadow:</b> {@code oleh}/{@code olehId} (pencatat perubahan) dan
 * {@code tanggal_dirubah} (di-refresh oleh {@link #onUpdate()} melalui
 * {@code ais.database.hibernate.AuditTimestampInterceptor} pada setiap {@code @PreUpdate}) adalah
 * kolom audit yang ditulis oleh infrastruktur persistence, konsisten dengan entity lain sekeluarga
 * di paket {@code ais.database.model.epsbed}. Kelas mewarisi perilaku umum, validasi, dan lifecycle
 * dari {@link GeneralValueObject}; anotasi {@code @Audited} membuat setiap perubahan baris ini turut
 * tercatat oleh Hibernate Envers.</p>
 *
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "epsbed", name = "epsbed_jabatan_akademik")



public class EpsbedJabatanAkademik extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris kode jabatan akademik; dibangkitkan otomatis oleh database (identity). */
	private Long id;
	/** Nama pencatat perubahan terakhir; kolom audit yang diisi oleh lapisan pemanggil, bukan Hibernate. */
	private String oleh;
	/** Id pencatat perubahan terakhir; pasangan dari {@link #oleh}. */
	private String olehId;

	/**
	 * Mengembalikan id pencatat perubahan terakhir baris ini.
	 *
	 * @return id pencatat, atau {@code null} bila belum pernah diset.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pencatat perubahan terakhir. Nilai kosong/blank diabaikan sehingga id pencatat
	 * yang sudah tersimpan tidak pernah ditimpa nilai kosong.
	 *
	 * @param olehId id pencatat baru; diabaikan jika {@code null} atau string kosong setelah di-trim.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pencatat perubahan terakhir. Nilai kosong/blank diabaikan, simetris dengan
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pencatat baru; diabaikan jika {@code null} atau string kosong setelah di-trim.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pencatat perubahan terakhir baris ini.
	 *
	 * @return nama pencatat, atau {@code null} bila belum pernah diset.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback lifecycle JPA {@code @PreUpdate}: dipanggil otomatis oleh provider persistence sebelum
	 * baris ini di-{@code UPDATE}, mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} untuk memperbarui
	 * {@link #tanggal_dirubah}. Tidak dipanggil manual oleh kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan cap waktu perubahan terakhir secara eksplisit.
	 *
	 * @param tanggal_dirubah cap waktu perubahan baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir baris ini, diperbarui otomatis oleh
	 * {@link #onUpdate()} pada setiap update.
	 *
	 * @return cap waktu perubahan terakhir, tidak pernah {@code null}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi string baris ini, dipakai widget UI (mis. combobox/listbox) yang menampilkan
	 * entity lewat {@code toString()}.
	 *
	 * @return nilai field {@link #nama} apa adanya (tidak di-trim); bisa {@code null} bila belum
	 *         pernah diset.
	 */
	public String toString() {
		return nama;
	}

	/** Nama jabatan akademik (mis. "Asisten Ahli", "Lektor", "Lektor Kepala", "Guru Besar"). */
	private String nama;
	/** Kode jabatan akademik sesuai skema EPSBED, dipakai untuk ekspor pelaporan. */
	private String kode;

	/** Konstruktor default yang dibutuhkan Hibernate/JPA untuk instansiasi entity via refleksi. */
	public EpsbedJabatanAkademik() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * @return id baris, atau {@code null} bila entity belum pernah dipersist.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan primary key baris ini. Kolom dipetakan {@code insertable = false} sehingga nilai ini
	 * normalnya diisi oleh Hibernate dari identity generator database.
	 *
	 * @param id nilai id baru.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama jabatan akademik baris ini.
	 *
	 * @return nilai {@link #nama} yang di-trim; {@code null} bila belum pernah diset.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama jabatan akademik baris ini.
	 *
	 * @param nama nama jabatan akademik baru.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan kode jabatan akademik baris ini sesuai skema EPSBED. Berbeda dari
	 * {@link #getNama()}, nilai ini dikembalikan apa adanya tanpa {@code trim}.
	 *
	 * @return kode jabatan akademik; bisa {@code null} bila belum pernah diset.
	 */
	@Column(name = "kode", nullable = false, length = 255)
	public String getKode() {
		return this.kode;
	}

	/**
	 * Menetapkan kode jabatan akademik baris ini.
	 *
	 * @param kode kode jabatan akademik baru.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

}
