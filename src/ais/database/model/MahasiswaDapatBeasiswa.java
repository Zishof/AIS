package ais.database.model;

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

/**
 * Entity <b>penetapan penerima beasiswa</b> pada tabel {@code public.mahasiswa_dapat_beasiswa}.
 * Satu baris = satu mahasiswa <b>ditetapkan resmi sebagai penerima</b> suatu {@link Beasiswa}.
 *
 * <p><b>Bukan entity pendaftaran.</b> Sesuai Javadoc kelas {@link Beasiswa} (§"Posisi dalam
 * alur beasiswa"), pendaftaran mahasiswa disimpan terpisah di
 * {@link ais.database.model.beasiswa.MahasiswaDaftarBeasiswa} (paket {@code beasiswa}, berisi
 * status seleksi {@code terima}/{@code tanggalDaftar} dsb.), sedangkan kelas ini adalah baris
 * <b>hasil akhir</b> setelah proses seleksi selesai — analog dengan pasangan
 * {@link ais.database.model.kkn.MahasiswaDaftarKkn}/{@link MahasiswaDapatKkn} pada modul KKN
 * dan {@link ais.database.model.pkl.MahasiswaDaftarPkl}/{@link MahasiswaDapatPkl} pada modul
 * PKL. Kelas ini tidak memiliki kolom status seleksi maupun tanggal pendaftaran sendiri; hanya
 * relasi ke {@link #getBeasiswa()}, {@link #getMahasiswa()}, dan {@link #getKeterangan()}.</p>
 *
 * <p>Field audit ({@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah})
 * dideklarasikan ulang di sini karena {@link GeneralValueObject} bukan
 * {@code @Entity}/{@code @MappedSuperclass}; kontrak umumnya didokumentasikan di kelas
 * tersebut.</p>
 *
 * @see GeneralValueObject
 * @see Beasiswa
 * @see ais.database.model.beasiswa.MahasiswaDaftarBeasiswa
 * @see PengajuanBeasiswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "mahasiswa_dapat_beasiswa")

public class MahasiswaDapatBeasiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java; dibiarkan sama dengan banyak entity sejenis hasil generate
	 * {@code hbm2java} 2010.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama tabel {@code public.mahasiswa_dapat_beasiswa} ({@code IDENTITY}). */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (bayangan field audit). */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris ini (bayangan field audit). */
	private String olehId;

	/**
	 * @return ID pengguna yang terakhir mengubah baris ini, atau {@code null} bila belum
	 *         pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan ID pengguna pengubah terakhir. Masukan {@code null}/kosong/spasi diabaikan
	 * diam-diam (early return) sehingga nilai audit lama tidak bisa dihapus lewat setter ini.
	 *
	 * @param olehId ID pengguna; nilai kosong/{@code null} tidak berpengaruh
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)},
	 * masukan kosong/{@code null} diabaikan diam-diam.
	 *
	 * @param oleh nama pengguna; nilai kosong/{@code null} tidak berpengaruh
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna yang terakhir mengubah baris ini, atau {@code null} bila belum
	 *         pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}, dipanggil Hibernate sebelum {@code UPDATE} untuk mengisi
	 * ulang {@code oleh}/{@code olehId}/{@code tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Tidak untuk
	 * dipanggil langsung dari kode aplikasi.
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

	/** @return {@link #getKeterangan()} apa adanya — representasi teks ringkas baris ini. */
	public String toString() {
		return keterangan;
	}

	/** Program beasiswa yang diterima mahasiswa ini. Wajib diisi (kolom {@code NOT NULL}). */
	private Beasiswa beasiswa;
	/** Mahasiswa yang ditetapkan sebagai penerima. Wajib diisi (kolom {@code NOT NULL}). */
	private Mahasiswa mahasiswa;
	/** Catatan/keterangan bebas penetapan ini; boleh {@code null}. */
	private String keterangan;

	/** Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. */
	public MahasiswaDapatBeasiswa() {
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

	/** @return keterangan bebas penetapan ini apa adanya, tanpa normalisasi. */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan catatan/keterangan bebas untuk penetapan ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @param beasiswa program beasiswa yang diterima mahasiswa ini. */
	public void setBeasiswa(Beasiswa beasiswa) {
		this.beasiswa = beasiswa;
	}

	/**
	 * @return program {@link Beasiswa} yang diterima. Referensi dicek lewat
	 *         {@code check(beasiswa)} sebelum dikembalikan (proxy Hibernate basi diganti
	 *         entity segar bila perlu).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "beasiswa", nullable = false)
	public Beasiswa getBeasiswa() {
		beasiswa = check(beasiswa);
		return beasiswa;
	}

	/** @param mahasiswa mahasiswa yang ditetapkan sebagai penerima. */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * @return mahasiswa penerima beasiswa ini. Referensi dicek lewat {@code check(mahasiswa)}
	 *         sebelum dikembalikan, sama seperti {@link #getBeasiswa()}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = false)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

}
