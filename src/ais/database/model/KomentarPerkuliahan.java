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
 * Entity <b>komentar penilaian perkuliahan</b> pada tabel {@code public.komentar_perkuliahan}.
 * Satu baris adalah satu komentar bebas teks ({@link #getKeterangan()}) yang ditautkan ke satu
 * sesi {@link Perkuliahan}; ditulis lewat {@code KomentarPerkuliahanHelper} (dipanggil dari layar
 * Penilaian, {@code DetailperkuliahanForPenilaianHelper}) oleh dosen pengampu/pembimbing akademik
 * atau pihak berkepentingan lain, dan tampil sebagai satu thread bersama di halaman KRS mahasiswa
 * maupun dosen pembimbing untuk sesi tersebut.
 *
 * <p><b>Field {@link #getNama() nama} sebenarnya menyimpan {@code userId} penulis komentar</b>
 * (diisi lewat {@code komentarPerkuliahan.setNama(tbmuser.getUserId())} pada
 * {@code KomentarPerkuliahanHelper}), bukan nama tampilan — penamaan field ini menyesatkan bila
 * dibaca sekilas.</p>
 *
 * <p><b>Cakupan tampil (bukan celah lintas-pengguna):</b> daftar komentar dimuat per
 * {@code perkuliahan} ({@code Restrictions.eq("perkuliahan", perkuliahan)} di
 * {@code DetailperkuliahanForPenilaianHelper#loadDataKomentar()}), bukan per-penulis — ini
 * <b>disengaja</b>: seluruh komentar pada satu sesi perkuliahan memang dimaksudkan sebagai thread
 * bersama yang dilihat semua dosen pengampu dan pembimbing akademik mahasiswa terkait, bukan pesan
 * privat antar dua pihak.</p>
 *
 * <p><b>Namun tombol hapus hanya dijaga di sisi klien:</b> {@code KomentarPerkuliahanRenderer} di
 * {@code DetailperkuliahanForPenilaianHelper} menyembunyikan tombol hapus lewat
 * {@code button.setVisible(getNama().equals(userId login))}, tetapi listener {@code onClick}-nya
 * memanggil {@code Common.refreshDelete(komentarPerkuliahanBeans)} tanpa pengecekan ulang
 * kepemilikan di sisi server. Pola "tombol disembunyikan di klien, handler tidak re-verifikasi di
 * server" ini sejalan dengan pola bypass otorisasi UI-only yang berulang ditemukan di modul lain
 * (lihat catatan proyek terkait persetujuan/approval); pada entity ini dampaknya adalah pengguna
 * mana pun yang memiliki akses ke layar Penilaian berpotensi menghapus komentar milik pengguna
 * lain lewat request yang dibuat langsung ke komponen, bukan lewat tombol yang tersembunyi.</p>
 *
 * <p>Field audit ({@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah})
 * dideklarasikan ulang di sini karena {@link GeneralValueObject} bukan
 * {@code @Entity}/{@code @MappedSuperclass}; kontrak umumnya didokumentasikan di kelas
 * tersebut.</p>
 *
 * @see GeneralValueObject
 * @see Perkuliahan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "komentar_perkuliahan")

public class KomentarPerkuliahan extends GeneralValueObject {

	/**
	 * Versi serialisasi Java; dibiarkan sama dengan banyak entity sejenis hasil generate
	 * {@code hbm2java} 2010.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama tabel {@code public.komentar_perkuliahan} ({@code IDENTITY}). */
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

	/** @return representasi teks berbentuk {@code "<id>-<nama>"}, dengan {@code nama} berisi userId penulis. */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Berisi <b>userId</b> pengguna yang menulis komentar ini (diisi
	 * {@code tbmuser.getUserId()} oleh {@code KomentarPerkuliahanHelper}), <b>bukan</b> nama
	 * tampilan meski nama field-nya {@code nama}.
	 */
	private String nama;
	/** Isi teks komentar; dipetakan tipe {@code text}. */
	private String keterangan;
	/** Sesi perkuliahan yang menjadi topik komentar ini; relasi eager (lihat catatan kelas). */
	private Perkuliahan perkuliahan;

	/** Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. */
	public KomentarPerkuliahan() {
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
	 * @return userId penulis komentar (bukan nama tampilan), di-trim; {@code null} bila belum
	 *         pernah diisi.
	 */
	@Column(name = "nama", nullable = true, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama userId penulis komentar; disimpan apa adanya, trimming terjadi di {@link #getNama()}. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return isi teks komentar apa adanya, tanpa normalisasi. */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan isi teks komentar. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return {@link Perkuliahan} topik komentar ini, atau {@code null} bila belum diisi; relasi
	 *         {@code @ManyToOne} eager (bukan {@code FetchType.LAZY}) dengan strategi
	 *         {@code FetchMode.SELECT}, sehingga sudah terinisialisasi saat baris ini dimuat dan
	 *         tidak dipanggilkan {@code check()}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "perkuliahan", nullable = true)
	public Perkuliahan getPerkuliahan() {
		return perkuliahan;
	}

	/** @param perkuliahan sesi perkuliahan yang menjadi topik komentar ini; boleh {@code null}. */
	public void setPerkuliahan(Perkuliahan perkuliahan) {
		this.perkuliahan = perkuliahan;
	}

}
