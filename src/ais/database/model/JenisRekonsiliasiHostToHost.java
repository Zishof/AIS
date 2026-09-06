package ais.database.model;

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

import ais.action.master.helper.DefaultJenisParsingReconsile;

/**
 * Entity master data <b>jenis rekonsiliasi host-to-host (H2H)</b> pada tabel
 * {@code public.jenis_rekonsiliasi_host_to_host}. Satu baris mewakili satu <i>strategi parser</i>
 * untuk format file mutasi/rekonsiliasi bank H2H tertentu: {@link #getNamaKelas() namaKelas}
 * menyimpan nama kelas Java lengkap yang mengimplementasikan
 * {@code ais.action.master.helper.JenisParsingReconsile} (mis.
 * {@link DefaultJenisParsingReconsile}, {@code EdupayJenisParsingReconsile}), diinstansiasi lewat
 * refleksi ({@code Class.forName(namaKelas).newInstance()}) oleh
 * {@code RekonsiliasiHostToHostAction} dan {@code ReconsilePembayaranHostToHostSyncrhonizerProcessor}
 * untuk memproses file rekonsiliasi yang diunggah/dijadwalkan.
 *
 * <p><b>Verifikasi keamanan (bukan perluasan temuan LogHostToHost):</b> entity ini <b>tidak</b>
 * menyimpan data pembayaran H2H mentah — hanya {@code nama}, {@code keterangan}, dan
 * {@code namaKelas} (nama strategi parser). Ini berbeda mekanisme dari temuan kritis pada
 * {@code LogHostToHost} (penyimpanan payload H2H mentah), sehingga tidak dicatat sebagai
 * perluasan temuan tersebut. Yang perlu diwaspadai justru mekanisme instansiasi refleksi:
 * {@code namaKelas} pada baris yang dibuat otomatis selalu berasal dari
 * {@code Konfigurasi("default_class_yang_digunakan_untuk_memproses_reconsile_pembayaran_host_to_host")}
 * yang sudah tervalidasi lewat {@code Class.forName} lebih dulu (lihat
 * {@code RekonsiliasiHostToHostAction}), dan combobox pemilihannya bersifat {@code readonly} —
 * sehingga jalur UI standar tidak mengekspos input bebas teks untuk field ini. Trust boundary-nya
 * setara dengan siapa pun yang berwenang mengubah {@code Konfigurasi} tersebut atau mengedit
 * baris ini lewat CRUD generik/akses DB langsung; entity ini tidak menambah privilese baru di
 * luar itu.</p>
 *
 * <p>Field audit ({@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah})
 * dideklarasikan ulang di sini karena {@link GeneralValueObject} bukan
 * {@code @Entity}/{@code @MappedSuperclass}; kontrak umumnya didokumentasikan di kelas
 * tersebut.</p>
 *
 * @see GeneralValueObject
 * @see DefaultJenisParsingReconsile
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "jenis_rekonsiliasi_host_to_host")
public class JenisRekonsiliasiHostToHost extends GeneralValueObject {

	/**
	 * Versi serialisasi Java; dibiarkan sama dengan banyak entity sejenis hasil generate
	 * {@code hbm2java} 2010.
	 */
	private static final long serialVersionUID = 2463821577543439808L;
	/** Kunci utama tabel {@code public.jenis_rekonsiliasi_host_to_host} ({@code IDENTITY}). */
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

	/** @return {@link #getNama() nama} jenis rekonsiliasi apa adanya. */
	public String toString() {
		return nama;
	}

	/** Nama jenis rekonsiliasi H2H (lazimnya nama simpel kelas parsernya); wajib diisi. */
	private String nama;
	/** Catatan/keterangan bebas tentang jenis rekonsiliasi ini; dipetakan tipe {@code text}. */
	private String keterangan;
	/**
	 * Nama kelas Java lengkap ({@code JenisParsingReconsile}) yang menangani parsing untuk jenis
	 * ini; lihat {@link #getNamaKelas()} untuk perilaku default.
	 */
	private String namaKelas;

	/** Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. */
	public JenisRekonsiliasiHostToHost() {
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

	/** @return nama jenis rekonsiliasi, di-trim; {@code null} bila belum pernah diisi. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama jenis rekonsiliasi; disimpan apa adanya, trimming terjadi di {@link #getNama()}. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan bebas jenis rekonsiliasi ini apa adanya, tanpa normalisasi. */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan catatan/keterangan bebas untuk jenis rekonsiliasi ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menurunkan nama kelas parser bila belum pernah diisi, lalu <b>menuliskannya balik ke
	 * field</b> {@code namaKelas} sebagai memoisasi (pola getter-mutasi derivatif yang berulang di
	 * paket ini). Default-nya adalah {@link DefaultJenisParsingReconsile} sehingga baris yang
	 * belum dikonfigurasi tetap punya strategi parser yang valid untuk diinstansiasi lewat
	 * refleksi.
	 *
	 * @return nama kelas Java lengkap {@code JenisParsingReconsile} yang menangani jenis ini
	 */
	public String getNamaKelas() {
		if (namaKelas == null || namaKelas.trim().isEmpty()) {
			namaKelas = DefaultJenisParsingReconsile.class.getName();
		}
		return namaKelas;
	}

	/** @param namaKelas nama kelas Java lengkap {@code JenisParsingReconsile} untuk jenis ini. */
	public void setNamaKelas(String namaKelas) {
		this.namaKelas = namaKelas;
	}

}
