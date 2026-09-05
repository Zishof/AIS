package ais.database.model.sirs;

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
 * Entitas katalog master prioritas/triase pasien pada schema {@code sirs}
 * (tabel {@code prioritas_pasien}) — mis. label triase UGD seperti
 * "Merah" (gawat darurat), "Kuning" (darurat tidak gawat), "Hijau" (tidak
 * darurat), atau skala prioritas serupa. Dipakai sebagai relasi
 * {@code ManyToOne} dari {@link ais.database.model.sirs.Pasien#getPrioritasPasien()}
 * — prioritas melekat pada PASIEN, bukan pada satu kunjungan/pendaftaran
 * tertentu.
 *
 * <p>
 * Field {@link #getNilaiPrioritas()} adalah angka 1&ndash;10 WAJIB DIPILIH
 * di form CRUD-nya sendiri ({@code PrioritasPasienAction}, lewat
 * {@code Combobox} berisi pilihan 1..10). Meski namanya menyiratkan urutan
 * antrean, PENELUSURAN KODE TIDAK MENEMUKAN query {@code ORDER BY}/
 * {@code Order.asc}/{@code Order.desc} berbasis {@code nilaiPrioritas} di
 * mana pun pada basis kode — nilai ini tersimpan sebagai atribut
 * deskriptif master data, dan pengurutan antrean berdasarkan prioritas
 * (bila ada) tampaknya diserahkan pada pengurutan kolom manual di layar
 * daftar pasien, BUKAN ditegakkan otomatis oleh backend lewat field ini.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "prioritas_pasien")
public class PrioritasPasien extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String olehId;

	/**
	 * Mengambil ID pengguna (username/oleh-id) yang terakhir mengubah baris
	 * ini. Field audit shadow yang diisi lewat {@link #setOlehId(String)}.
	 *
	 * @return ID pengguna terakhir yang mengubah, atau {@code null} jika
	 *         belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna yang mengubah baris ini. Nilai kosong/blank
	 * SENGAJA diabaikan (early return) agar field audit ini tidak pernah
	 * ditimpa jadi kosong — pola field audit shadow yang merupakan
	 * KEHARUSAN TEKNIS berulang di paket model AIS, bukan bug.
	 *
	 * @param olehId ID pengguna; nilai {@code null} atau string
	 *               kosong/whitespace-saja tidak akan mengubah nilai
	 *               tersimpan.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	private String oleh;

	/**
	 * Representasi ringkas prioritas pasien ini untuk keperluan
	 * tampilan/log.
	 *
	 * @return nama prioritas pasien.
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Menetapkan nama pengguna yang mengubah baris ini. Nilai kosong/blank
	 * diabaikan agar field audit ini tidak pernah ditimpa kosong.
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau
	 *             kosong/whitespace-saja tidak akan mengubah nilai
	 *             tersimpan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna terakhir yang mengubah, atau {@code null} jika
	 *         belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate} yang memperbarui {@link #tanggal_dirubah}
	 * otomatis sebelum baris ini di-UPDATE, lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = new Date();

	/**
	 * Menetapkan timestamp perubahan terakhir baris ini secara manual.
	 *
	 * @param tanggal_dirubah timestamp perubahan baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil timestamp perubahan terakhir baris ini, diperbarui
	 * otomatis oleh {@link #onUpdate()} setiap kali baris di-UPDATE.
	 *
	 * @return timestamp perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	private String nama;
	private Integer nilaiPrioritas;
	private String keterangan;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public PrioritasPasien() {
	}

	/**
	 * Primary key baris prioritas pasien, auto-increment (IDENTITY) dan
	 * diisi database.
	 *
	 * @return ID unik prioritas pasien ini.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID prioritas pasien.
	 *
	 * @param id ID prioritas pasien.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil nama/label prioritas pasien (mis. "Merah", "Kuning",
	 * "Hijau" untuk triase UGD).
	 *
	 * @return nama prioritas pasien.
	 */
	@Column(name = "nama", nullable = false, length = 50)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menetapkan nama/label prioritas pasien.
	 *
	 * @param nama nama prioritas pasien.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil keterangan bebas prioritas pasien ini.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas prioritas pasien ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan nilai numerik prioritas ini (1&ndash;10, dipilih lewat
	 * combobox di form CRUD-nya). Lihat javadoc kelas untuk catatan
	 * bahwa nilai ini TIDAK ditemukan ditegakkan lewat query pengurutan
	 * apa pun di basis kode.
	 *
	 * @param nilaiPrioritas nilai prioritas, 1&ndash;10.
	 */
	public void setNilaiPrioritas(Integer nilaiPrioritas) {
		this.nilaiPrioritas = nilaiPrioritas;
	}

	/**
	 * Mengambil nilai numerik prioritas ini.
	 *
	 * @return nilai prioritas (1&ndash;10), atau {@code null} jika belum
	 *         diisi.
	 */
	@Column(name = "nilai_prioritas", nullable = true)
	public Integer getNilaiPrioritas() {
		return nilaiPrioritas;
	}

}
