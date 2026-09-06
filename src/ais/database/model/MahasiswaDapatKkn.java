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
 * Entity <b>peserta KKN yang diterima</b> pada tabel {@code public.mahasiswa_dapat_kkn}. Satu
 * baris = satu mahasiswa <b>ditetapkan resmi sebagai peserta</b> suatu gelaran {@link Kkn}
 * — persis peran yang disebut eksplisit pada Javadoc kelas {@link Kkn}: "{@code MahasiswaDapatKkn}
 * (PESERTA yang akhirnya diterima)".
 *
 * <p><b>Bukan entity pendaftaran, dan bukan legacy.</b> Sudah diverifikasi lewat pemakaian
 * nyata ({@code KknAction}, {@code AmbilDataMahasiswaKknHelper},
 * {@code AmbilDataMahasiswaSeleksiKknHelper}, {@code MahasiswaDapatKknDao}): kelas ini aktif
 * dipakai dan berbeda peran dari {@link ais.database.model.kkn.MahasiswaDaftarKkn} (paket
 * {@code kkn}), yang menyimpan <b>pendaftaran</b> — status seleksi
 * ({@code BELUM_DIPROSES}/{@code DITERIMA}/{@code DITOLAK}), tanggal daftar, dan skor. Kelas
 * ini tidak menyimpan status seleksi maupun tanggal pendaftaran sendiri; ia adalah hasil
 * akhir setelah seleksi selesai, dipakai kelompokkan peserta ke
 * {@code ais.database.model.kkn.KelompokKkn} lewat entity terpisah
 * {@code MahasiswaDapatKelompokKkn}. Pola yang sama berlaku untuk PKL:
 * {@link ais.database.model.pkl.MahasiswaDaftarPkl} (pendaftaran) vs.
 * {@link MahasiswaDapatPkl} (penetapan peserta), dan untuk beasiswa:
 * {@link ais.database.model.beasiswa.MahasiswaDaftarBeasiswa} vs. {@link MahasiswaDapatBeasiswa}.</p>
 *
 * <p>Field audit ({@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah})
 * dideklarasikan ulang di sini karena {@link GeneralValueObject} bukan
 * {@code @Entity}/{@code @MappedSuperclass}; kontrak umumnya didokumentasikan di kelas
 * tersebut.</p>
 *
 * @see GeneralValueObject
 * @see Kkn
 * @see ais.database.model.kkn.MahasiswaDaftarKkn
 * @see MahasiswaDapatPkl
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "mahasiswa_dapat_kkn")

public class MahasiswaDapatKkn extends GeneralValueObject {

	/**
	 * Versi serialisasi Java; dibiarkan sama dengan banyak entity sejenis hasil generate
	 * {@code hbm2java} 2010.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama tabel {@code public.mahasiswa_dapat_kkn} ({@code IDENTITY}). */
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

	/** Gelaran KKN yang diikuti mahasiswa ini. Wajib diisi (kolom {@code NOT NULL}). */
	private Kkn kkn;
	/** Mahasiswa yang ditetapkan sebagai peserta. Wajib diisi (kolom {@code NOT NULL}). */
	private Mahasiswa mahasiswa;
	/** Catatan/keterangan bebas penetapan ini; boleh {@code null}. */
	private String keterangan;

	/** Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. */
	public MahasiswaDapatKkn() {
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

	/** @param kkn gelaran KKN yang diikuti mahasiswa ini. */
	public void setKkn(Kkn kkn) {
		this.kkn = kkn;
	}

	/**
	 * @return gelaran {@link Kkn} yang diikuti. Referensi dicek lewat {@code check(kkn)}
	 *         sebelum dikembalikan (proxy Hibernate basi diganti entity segar bila perlu).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kkn", nullable = false)
	public Kkn getKkn() {
		kkn = check(kkn);
		return kkn;
	}

	/** @param mahasiswa mahasiswa yang ditetapkan sebagai peserta. */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * @return mahasiswa peserta KKN ini. Referensi dicek lewat {@code check(mahasiswa)}
	 *         sebelum dikembalikan, sama seperti {@link #getKkn()}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = false)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

}
