package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

/**
 * Entity Hibernate untuk tabel {@code public.tim_dosen} &mdash; baris penghubung yang
 * menyatakan seorang {@link Dosen} adalah anggota tim pengajar (co-lecturer) sebuah
 * {@link Perkuliahan} (kelas kuliah). Dimodelkan sebagai entity biasa dengan dua
 * {@code @ManyToOne} ({@link #getPerkuliahan()}, {@link #getDosen()}), bukan sebagai
 * tabel join {@code @ManyToMany} murni, sehingga tiap pasangan perkuliahan-dosen adalah
 * satu baris dengan {@code id} sendiri.
 *
 * <p>Dikelola lewat helper "pilih dari daftar"
 * {@code ais.action.master.helper.AmbilDataDosenHelper}: satu jendela modal menampilkan grid
 * dosen aktif dengan checkbox keanggotaan tim; {@code save()} di sana menyinkronkan checkbox
 * yang tercentang menjadi baris {@code TimDosen} (dibuat bila belum ada) dan yang tak
 * tercentang menjadi penghapusan baris. Tidak ada validasi unik di level basis data yang
 * mencegah duplikasi pasangan perkuliahan-dosen; pencarian sebelum insert/delete dilakukan
 * manual di helper lewat kombinasi {@code Restrictions.eq("perkuliahan", ...)} dan
 * {@code Restrictions.eq("dosen", ...)}.
 *
 * <p>Diakses lewat DAO generik {@code ais.database.dao.TimDosenDaoImpl} (tanpa method
 * tambahan). Diturunkan dari {@link GeneralValueObject}; {@code id}, {@code oleh},
 * {@code olehId}, dan {@link #tanggal_dirubah} dideklarasikan ulang di sini karena kelas
 * induk adalah POJO abstrak biasa (bukan {@code @Entity}/{@code @MappedSuperclass}) &mdash;
 * keharusan teknis, bukan duplikasi keliru.
 *
 * @see Perkuliahan
 * @see Dosen
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "tim_dosen")

public class TimDosen extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463825577548439808L;
	private Long id;
	/** Nama pelaku (audit shadow, lihat {@link GeneralValueObject}) yang membuat/mengubah baris ini. */
	private String oleh;
	/** Id pelaku (audit shadow) yang membuat/mengubah baris ini. */
	private String olehId;

	/** @return id pelaku terakhir yang mengubah baris ini, atau {@code null} bila belum pernah diisi. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pelaku. Nilai kosong/blank diabaikan (fail-safe agar audit shadow tidak
	 * tertimpa string kosong secara tidak sengaja) &mdash; bukan validasi keamanan.
	 *
	 * @param olehId id pelaku; diabaikan jika {@code null} atau hanya berisi spasi
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pelaku. Nilai kosong/blank diabaikan, sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pelaku; diabaikan jika {@code null} atau hanya berisi spasi
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pelaku terakhir yang mengubah baris ini, atau {@code null} bila belum pernah diisi. */
	public String getOleh() {
		return oleh;
	}

	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengubah stempel waktu perubahan terakhir secara manual. Nilai default sudah
	 * di-set ke waktu saat ini pada deklarasi field dan di-refresh otomatis oleh
	 * {@link #onUpdate()} pada setiap update; setter ini jarang perlu dipanggil langsung.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return stempel waktu perubahan terakhir baris ini. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return representasi ringkas berupa {@code perkuliahan_dosen} (memakai
	 *     {@code toString()} kedua objek relasi, bukan id/nama eksplisit), dipakai untuk
	 *     debugging/log.
	 */
	public String toString() {
		return perkuliahan + "_" + dosen;
	}

	private Perkuliahan perkuliahan;
	private Dosen dosen;
	private String keterangan;

	/** Konstruktor kosong yang diperlukan Hibernate. */
	public TimDosen() {
	}

	/** @return id baris (primary key, auto-generated identity di database). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id id baris; kolom tidak insertable sehingga nilai ini diabaikan saat insert. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return keterangan tambahan untuk keanggotaan tim ini, boleh {@code null}. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan tambahan untuk keanggotaan tim ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @param perkuliahan kelas kuliah yang diampu tim ini. */
	public void setPerkuliahan(Perkuliahan perkuliahan) {
		this.perkuliahan = perkuliahan;
	}

	/** @return kelas kuliah ({@link Perkuliahan}) yang diampu, boleh {@code null}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "perkuliahan", nullable = true)
	public Perkuliahan getPerkuliahan() {
		return perkuliahan;
	}

	/** @param dosen dosen anggota tim pengajar. */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/** @return dosen anggota tim pengajar untuk {@link #getPerkuliahan()}, boleh {@code null}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "dosen", nullable = true)
	public Dosen getDosen() {
		return dosen;
	}

}
