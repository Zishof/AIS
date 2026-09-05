package ais.database.model.rab;

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
 * Entity JPA/Hibernate untuk tabel {@code rab.jenis_informasi_rab} — katalog jenis/kategori
 * pengumuman RAB (dirujuk oleh {@link InformasiRab#getJenisInformasiRab()}).
 *
 * <p>
 * <b>Tidak bersifat enum tertutup:</b> baris tabel ini adalah data master biasa yang bisa
 * ditambah/diubah/dihapus bebas lewat layar admin ZK {@link ais.action.master.rab.JenisInformasiRabAction}
 * (CRUD generik standar, tidak ada logika khusus tersembunyi di action tersebut). Namun secara
 * konvensi tiga baris tetap ("Informasi", "Pengumuman", "Peringatan") DIJAMIN selalu ada lewat pola
 * cari-atau-buat (find-or-create) pada blok statis {@code RabUtil}: konstanta
 * {@link ais.action.master.rab.util.RabUtil#INFORMASI}, {@link ais.action.master.rab.util.RabUtil#PENGUMUMAN},
 * dan {@link ais.action.master.rab.util.RabUtil#PERINGATAN} masing-masing dijalankan sekali saat
 * kelas {@code RabUtil} dimuat (class-loading), membuat baris yang belum ada lewat sesi Hibernate
 * terpisah ({@code HibernateUtil.getSessionFactory().openSession()}) di luar transaksi request yang
 * sedang berjalan. Kode yang bergantung pada salah satu dari tiga jenis baku ini SEBAIKNYA memakai
 * konstanta {@code RabUtil} tersebut, bukan mencari ulang lewat teks nama.
 * </p>
 *
 * @see InformasiRab
 * @see ais.action.master.rab.util.RabUtil
 * @see ais.action.master.rab.JenisInformasiRabAction
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "rab", name = "jenis_informasi_rab")



public class JenisInformasiRab extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}. Nilai ini identik
	 * dengan entity lain hasil template hbm2java yang sama di paket {@code ais.database.model.rab} —
	 * bukan kesalahan salin-tempel yang perlu diperbaiki.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key auto-increment (identitas baris jenis informasi). */
	private Long id;
	/** Nama tampilan pembuat/pengubah terakhir — field audit "siapa" (bukan referensi FK). */
	private String oleh;
	/** Id pengguna pembuat/pengubah terakhir — pasangan shadow audit dari {@link #oleh} (pola sama seperti {@link InformasiRab#getOlehId()}). */
	private String olehId;

	/**
	 * Mengambil id pengguna pembuat/pengubah terakhir.
	 *
	 * @return id pengguna audit, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {return olehId;}

	/**
	 * Mengisi id pengguna pembuat/pengubah terakhir. Guard fail-safe: nilai {@code null}/kosong/spasi
	 * diabaikan diam-diam agar audit trail "siapa" tidak tertimpa kosong — pola sama seperti
	 * {@link InformasiRab#setOlehId(String)}.
	 *
	 * @param olehId id pengguna audit baru; diabaikan bila kosong/{@code null}
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}

	/**
	 * Mengisi nama tampilan pembuat/pengubah terakhir. Guard fail-safe yang sama seperti
	 * {@link #setOlehId(String)}: nilai kosong/{@code null} diabaikan diam-diam.
	 *
	 * @param oleh nama tampilan audit baru; diabaikan bila kosong/{@code null}
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama tampilan pembuat/pengubah terakhir.
	 *
	 * @return nama audit, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate sesaat sebelum setiap
	 * {@code UPDATE} baris ini, mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} untuk menyegarkan
	 * {@link #tanggal_dirubah} ke waktu saat ini — pola audit-timestamp identik dengan entity lain
	 * di paket ini.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}
	/**
	 * Timestamp perubahan terakhir. Diinisialisasi ke waktu saat objek dibuat di memori dan
	 * disegarkan otomatis oleh {@link #onUpdate()} pada setiap {@code UPDATE} berikutnya.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi timestamp perubahan terakhir secara manual. Biasanya tidak perlu dipanggil pemanggil
	 * biasa karena {@link #onUpdate()} sudah menyegarkannya otomatis pada setiap update.
	 *
	 * @param tanggal_dirubah timestamp baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil timestamp perubahan terakhir.
	 *
	 * @return timestamp perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi string singkat objek ini, dipakai komponen ZK generik seperti combobox
	 * (mis. {@code Common.insertCombo(jenisInformasiRab, "nama", JenisInformasiRab.class)} pada
	 * {@code InformasiRabAction}) yang menampilkan objek lewat {@code toString()}.
	 *
	 * @return {@link #nama} jenis informasi ini (nilai field mentah, tanpa {@code trim()})
	 */
	public String toString() {
		return nama;
	}

	/** Nama jenis informasi (mis. "Informasi", "Pengumuman", "Peringatan" — lihat catatan tiga baris baku pada javadoc kelas). */
	private String nama;
	/** Keterangan/deskripsi tambahan untuk jenis informasi ini, opsional. */
	private String keterangan;

	/** Konstruktor default (wajib untuk Hibernate). */
	public JenisInformasiRab() {
	}

	/**
	 * Mengambil id baris (primary key).
	 *
	 * @return id baris, atau {@code null} untuk entity baru yang belum disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi id baris secara manual. Kolom dipetakan {@code insertable = false} (nilai sesungguhnya
	 * berasal dari {@code IDENTITY} auto-increment DB saat {@code INSERT}).
	 *
	 * @param id id baris baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil nama jenis informasi, di-trim dari spasi di awal/akhir.
	 *
	 * @return nama jenis informasi yang sudah di-trim, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama jenis informasi. Nilai disimpan APA ADANYA tanpa {@code trim()} di sini — proses
	 * pemangkasan spasi hanya terjadi saat dibaca lewat {@link #getNama()}, sehingga
	 * {@link #toString()} (yang membaca field {@link #nama} langsung) dapat mengembalikan nilai yang
	 * belum di-trim. Perlu diketahui: pola cari-atau-buat pada {@code RabUtil} mencari baris baku
	 * lewat {@code Restrictions.eq("nama", "Pengumuman")} dsb. yang membandingkan nilai APA ADANYA di
	 * DB (bukan hasil {@link #getNama()}) — bila nama diisi dengan spasi tersembunyi, pencarian
	 * tersebut bisa gagal mencocokkan dan menciptakan baris duplikat.
	 *
	 * @param nama nama jenis informasi baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil keterangan/deskripsi tambahan jenis informasi ini.
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan/deskripsi tambahan jenis informasi ini.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

}
