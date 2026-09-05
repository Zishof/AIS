package ais.database.model.sirs;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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
 * Entitas junction (tabel penghubung) yang merepresentasikan keanggotaan
 * pasien dalam suatu komunitas, pada schema {@code sirs} (tabel
 * {@code komunitas_punya_pasien}) — mis. kelompok Prolanis, Posyandu,
 * atau kelompok pemantauan penyakit kronis lain yang diverifikasi dari
 * konteks pemakaian {@link Komunitas} (lihat javadoc kelas tersebut).
 * Menghubungkan {@link #getKomunitas()} dan {@link #getPasien()} sebagai
 * relasi many-to-many yang dimodelkan eksplisit lewat entitas
 * perantara ini (bukan {@code @ManyToMany} langsung), sehingga tiap
 * baris keanggotaan bisa membawa {@link #getKeterangan()} sendiri (mis.
 * tanggal bergabung/catatan status keanggotaan).
 *
 * <p>
 * Kedua relasi ({@code komunitas} dan {@code pasien}) ditandai
 * {@code nullable = true} pada kolomnya, meski secara bisnis baris tanpa
 * salah satu relasi tersebut tidak bermakna sebagai "keanggotaan" —
 * validasi kewajiban keduanya terisi (bila ada) diserahkan sepenuhnya
 * ke lapisan Action ({@code KomunitasPunyaPasienAction}), bukan
 * ditegakkan di level kolom database.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "komunitas_punya_pasien")
public class KomunitasPunyaPasien extends GeneralValueObject {

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
	 * Representasi ringkas baris keanggotaan ini untuk keperluan
	 * tampilan/log.
	 *
	 * @return string {@code komunitas pasien}.
	 */
	public String toString() {
		return komunitas + " " + pasien;
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

	private Komunitas komunitas;
	private Pasien pasien;
	private String keterangan;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public KomunitasPunyaPasien() {
	}

	/**
	 * Primary key baris keanggotaan komunitas-pasien, auto-increment
	 * (IDENTITY) dan diisi database.
	 *
	 * @return ID unik baris keanggotaan ini.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID baris keanggotaan.
	 *
	 * @param id ID baris keanggotaan.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil relasi komunitas pada baris keanggotaan ini. Kolom
	 * ditandai {@code nullable = true} secara database meski secara
	 * bisnis relasi ini seharusnya selalu terisi — lihat javadoc kelas.
	 *
	 * @return komunitas terkait, atau {@code null} jika belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "komunitas", nullable = true)
	public Komunitas getKomunitas() {
		komunitas = check(komunitas);
		return komunitas;
	}

	/**
	 * Menetapkan relasi komunitas pada baris keanggotaan ini.
	 *
	 * @param komunitas komunitas terkait.
	 */
	public void setKomunitas(Komunitas komunitas) {
		this.komunitas = komunitas;
	}

	/**
	 * Mengambil relasi pasien pada baris keanggotaan ini. Kolom ditandai
	 * {@code nullable = true} secara database meski secara bisnis relasi
	 * ini seharusnya selalu terisi — lihat javadoc kelas.
	 *
	 * @return pasien terkait, atau {@code null} jika belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pasien", nullable = true)
	public Pasien getPasien() {
		pasien = check(pasien);
		return pasien;
	}

	/**
	 * Menetapkan relasi pasien pada baris keanggotaan ini.
	 *
	 * @param pasien pasien terkait.
	 */
	public void setPasien(Pasien pasien) {
		this.pasien = pasien;
	}

	/**
	 * Mengambil keterangan bebas baris keanggotaan ini (mis. catatan
	 * status/tanggal bergabung). Berbeda dari kebanyakan field lain di
	 * kelas ini, getter ini TIDAK diberi anotasi {@code @Column}
	 * eksplisit — Hibernate tetap memetakannya ke kolom {@code keterangan}
	 * lewat konvensi nama properti default (bukan berarti field ini tidak
	 * persisten).
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menetapkan keterangan bebas baris keanggotaan ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

}
