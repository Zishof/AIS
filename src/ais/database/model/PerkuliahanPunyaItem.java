package ais.database.model;

// Generated Dec 22, 2009 12:14:16 PM by Hibernate Tools 3.2.4.CR1

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

import ais.database.model.library.Item;

/**
 * Entity relasi <b>perkuliahan &harr; item perpustakaan</b> pada tabel
 * {@code public.perkuliahan_punya_item}. Menautkan satu sesi/jadwal {@link Perkuliahan} dengan
 * satu {@link Item} perpustakaan (mis. bahan bacaan untuk pertemuan tersebut), lengkap dengan
 * {@link #getKeterangan() keterangan} bebas per pasangan. Mirip {@link KurikulumPunyaMatakuliahPunyaItem}
 * tetapi terikat ke satu sesi perkuliahan spesifik, bukan pasangan kurikulum-matakuliah.
 *
 * <p>Field audit ({@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah})
 * dideklarasikan ulang di sini karena {@link GeneralValueObject} bukan
 * {@code @Entity}/{@code @MappedSuperclass}; kontrak umumnya didokumentasikan di kelas
 * tersebut.</p>
 *
 * @see GeneralValueObject
 * @see Perkuliahan
 * @see Item
 * @see KurikulumPunyaMatakuliahPunyaItem
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "perkuliahan_punya_item")
public class PerkuliahanPunyaItem extends GeneralValueObject {

	/**
	 * Versi serialisasi Java; dibiarkan sama dengan banyak entity sejenis hasil generate
	 * {@code hbm2java} 2010.
	 */
	private static final long serialVersionUID = 1950126270979098967L;
	/** Kunci utama tabel {@code public.perkuliahan_punya_item} ({@code IDENTITY}). */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (bayangan field audit). */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris ini (bayangan field audit). */
	private String olehId;

	/**
	 * @return ID pengguna yang terakhir mengubah baris ini, atau {@code null} bila belum pernah
	 *         diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan ID pengguna pengubah terakhir. Masukan {@code null}/kosong/spasi diabaikan diam-diam
	 * (early return) sehingga nilai audit lama tidak bisa dihapus lewat setter ini.
	 *
	 * @param olehId ID pengguna; nilai kosong/{@code null} tidak berpengaruh
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)}, masukan
	 * kosong/{@code null} diabaikan diam-diam.
	 *
	 * @param oleh nama pengguna; nilai kosong/{@code null} tidak berpengaruh
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna yang terakhir mengubah baris ini, atau {@code null} bila belum pernah
	 *         diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}, dipanggil Hibernate sebelum {@code UPDATE} untuk mengisi ulang
	 * {@code oleh}/{@code olehId}/{@code tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Tidak untuk dipanggil
	 * langsung dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah waktu perubahan terakhir; boleh {@code null}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return representasi teks berbentuk {@code "<perkuliahan>_<item>"}. Method ini langsung
	 *         memakai field (tidak memanggil getter relasi), sehingga proxy lazy yang belum
	 *         terinisialisasi tidak ikut diresolusi di sini.
	 */
	public String toString() {
		return perkuliahan + "_" + item;
	}

	/** Sesi/jadwal perkuliahan pemilik relasi ini; relasi lazy, di-"check" sebelum dikembalikan. */
	private Perkuliahan perkuliahan;
	/** Item perpustakaan terkait; relasi lazy, di-"check" sebelum dikembalikan; boleh kosong. */
	private Item item;

	/** Catatan/keterangan bebas untuk pasangan perkuliahan-item ini; dipetakan tipe {@code text}. */
	private String keterangan;

	/** @return keterangan bebas pasangan ini apa adanya, tanpa normalisasi. */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/** @param keterangan catatan/keterangan bebas untuk pasangan perkuliahan-item ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. */
	public PerkuliahanPunyaItem() {
	}

	/**
	 * @return kunci utama baris ini, di-generate basis data ({@code IDENTITY}); {@code null}
	 *         sebelum baris pertama kali disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id kunci utama baris ini. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return {@link Perkuliahan} pemilik relasi ini; relasi {@code @ManyToOne} lazy, diresolusi
	 *         lewat {@link GeneralValueObject#check(Object)} sebelum dikembalikan agar aman
	 *         terhadap proxy yang sudah detached.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "perkuliahan", nullable = false)
	public Perkuliahan getPerkuliahan() {
		perkuliahan = check(perkuliahan);
		return this.perkuliahan;
	}

	/** @param perkuliahan sesi/jadwal perkuliahan pemilik relasi ini; wajib diisi. */
	public void setPerkuliahan(Perkuliahan perkuliahan) {
		this.perkuliahan = perkuliahan;
	}

	/**
	 * @return {@link Item} perpustakaan terkait, atau {@code null} bila belum diisi; relasi
	 *         {@code @ManyToOne} lazy, diresolusi lewat {@link GeneralValueObject#check(Object)}
	 *         sebelum dikembalikan agar aman terhadap proxy yang sudah detached.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item", nullable = true)
	public Item getItem() {
		item = check(item);
		return item;
	}

	/** @param item item perpustakaan terkait sesi perkuliahan ini; boleh {@code null}. */
	public void setItem(Item item) {
		this.item = item;
	}

}
