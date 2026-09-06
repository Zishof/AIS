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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.common.Common;

/**
 * Entity Hibernate/JPA untuk tabel {@code public.kegiatan_temporary} — <b>header staging</b>
 * mesin billing: struktur field-nya sengaja dibuat serupa dengan {@link Kegiatan} (header final
 * tagihan, sudah didokumentasikan lengkap pada batch sebelumnya), dipakai untuk menyusun draf
 * tagihan <b>sebelum disahkan</b> menjadi {@code Kegiatan} sesungguhnya.
 *
 * <p><b>Relasi ke {@link Kegiatan}:</b> kelas ini punya field {@link #getKegiatan()} (FK kolom
 * {@code kegiatan}) yang menunjuk BALIK ke {@link Kegiatan} — kebalikan dari arah relasi yang
 * mungkin diduga dari namanya. Ini bukan "Kegiatan versi sementara yang nanti menjadi Kegiatan
 * permanen dengan ID baru", melainkan penanda bahwa satu header staging ini SUDAH/SEDANG ditaut­
 * kan ke satu {@link Kegiatan} final tertentu (lihat catatan di {@link Kegiatan} kelas: "{@code
 * DetailKegiatan#kodeUnik} menerima salah satu dari keduanya, sehingga sebuah baris rincian dapat
 * bernaung di bawah header staging maupun header final"). Baris {@link DetailKegiatan} (rincian)
 * dapat bernaung di bawah header staging ({@code KegiatanTemporary}) INI, atau di bawah header
 * final ({@link Kegiatan}) — lihat {@link DetailKegiatan#getKegiatanTemporary()} pada mesin
 * billing pusat.</p>
 *
 * <p><b>Perbedaan yang teramati dari sisi file ini sendiri</b> dibanding {@link Kegiatan}:
 * beberapa getter di sini melakukan lazy-init-dan-tulis-balik yang mirip pola "getter destruktif"
 * (lihat {@link #getProgram()}, {@link #getTanggal()}, {@link #getAmount()}, {@link
 * #getJumlahTelahDibayar()}); dan {@link #getKegiatan()} — SATU-SATUNYA relasi {@code
 * @ManyToOne} di kelas ini yang TIDAK memanggil {@code check()} untuk resolusi proxy lazy,
 * berbeda dari enam relasi lain ({@link #getJenisKegiatan()}, {@link #getMahasiswa()}, {@link
 * #getCalonMahasiswa()}, {@link #getJadwalPembayaran()}, {@link #getStatusMahasiswa()}) yang
 * semuanya konsisten memakainya.</p>
 *
 * @see Kegiatan
 * @see DetailKegiatan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "kegiatan_temporary")
public class KegiatanTemporary extends GeneralValueObject {

	/**
	 * ID versi serialisasi Java untuk kompatibilitas antar build (bukan kolom database).
	 */
	private static final long serialVersionUID = 2413822577548439808L;
	/** Primary key baris {@code kegiatan_temporary}, kolom {@code id} (identity, auto-generate). */
	private Long id;
	/** Nama/username aktor yang membuat/terakhir mengubah baris ini (field audit longgar, bukan FK). */
	private String oleh;
	/** ID aktor yang membuat/terakhir mengubah baris ini (pasangan {@link #oleh}, bukan FK). */
	private String olehId;

	/**
	 * @return ID aktor ({@link #olehId}) yang tercatat membuat/mengubah baris ini; boleh {@code null}.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID aktor audit. Setter ini <b>fail-closed diam-diam</b>: nilai {@code null} atau
	 * string kosong/berspasi diabaikan sepenuhnya (nilai lama tetap dipertahankan), tanpa
	 * exception maupun log.
	 *
	 * @param olehId ID aktor baru; nilai kosong/{@code null} tidak berefek
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama aktor audit. Sama seperti {@link #setOlehId(String)}: nilai {@code null} atau
	 * kosong/berspasi diabaikan diam-diam, nilai lama dipertahankan.
	 *
	 * @param oleh nama aktor baru; nilai kosong/{@code null} tidak berefek
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama aktor ({@link #oleh}) yang tercatat membuat/mengubah baris ini; boleh {@code null}.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum {@code
	 * UPDATE} dieksekusi, mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah}
	 * untuk memperbarui jejak audit "terakhir diubah" milik entity ini.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu "terakhir diubah" secara manual. Field ini juga diinisialisasi ke
	 * waktu sekarang saat instance dibuat, dan ditulis ulang otomatis oleh {@link #onUpdate()}
	 * setiap kali baris di-{@code UPDATE}.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return stempel waktu terakhir baris ini diubah (kolom timestamp), diisi otomatis oleh
	 *         {@link #onUpdate()} pada setiap {@code UPDATE}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas untuk log/debug, menggabungkan identitas mahasiswa/calon mahasiswa,
	 * jenis kegiatan, tahun akademik/semester, program, dan nominal ({@code amount}) yang
	 * diformat sebagai Rupiah.
	 *
	 * <p><b>Catatan format:</b> baik {@code mahasiswa} maupun {@code calonMahasiswa} yang
	 * tidak {@code null} sama-sama diawali literal {@code "-"} tambahan pada string hasil
	 * (selain {@code "-"} pemisah utama), sehingga tampilannya bisa memuat tanda hubung ganda
	 * ({@code "--"}); dicatat apa adanya sebagai artefak format, bukan bug fungsional (method
	 * ini murni untuk log/debug).</p>
	 *
	 * @return string ringkas identitas baris ini
	 */
	public String toString() {
		return id + "-" + (mahasiswa == null ? "" : "-" + mahasiswa)
				+ (calonMahasiswa == null ? "" : "-" + calonMahasiswa) + "-"
				+ (jenisKegiatan == null ? "" : "-" + jenisKegiatan.getNamaKegiatan()) + tahunAkademik + "-" + semster
				+ "-" + program + "- Rp." + (amount == null ? "" : Common.numberFormat.get().format(amount));
	}

	/** Mahasiswa aktif pemilik header staging ini (FK {@code mahasiswa}); boleh kosong untuk header calon mahasiswa. */
	private Mahasiswa mahasiswa;
	/** Calon mahasiswa (PMB) pemilik header staging ini (FK {@code calon_mahasiswa}); boleh kosong untuk header mahasiswa aktif. */
	private BiodataCalonMahasiswa calonMahasiswa;
	/** Jenis kegiatan/tagihan header staging ini (FK {@code jenis_kegiatan}). */
	private JenisKegiatan jenisKegiatan;

	/** Tahun akademik header staging ini (format bebas, maks. 20 karakter). */
	private String tahunAkademik;
	/** Program studi; ditimpa otomatis dari mahasiswa/calon mahasiswa terkait, lihat {@link #getProgram()}. */
	private String program;
	/** Tanggal header staging ini; lazy-init ke waktu sekarang bila belum diisi, lihat {@link #getTanggal()}. */
	private Date tanggal;
	/** Semester header staging ini. */
	private Integer semster;
	/** Status validasi header staging ini (kode angka, makna ditentukan pemanggil). */
	private Integer validated;
	/** Nama/ID validator yang memvalidasi header staging ini. */
	private String validator;
	/** Keterangan bebas header staging ini. */
	private String keterangan;
	/** Nominal tagihan header staging ini; lazy-init ke {@code 0.0} bila belum diisi, lihat {@link #getAmount()}. */
	private Double amount;

	/** Status mahasiswa pada saat header staging ini dibuat. */
	private StatusMahasiswa statusMahasiswa;

	/** Jadwal pembayaran acuan header staging ini. */
	private JadwalPembayaran jadwalPembayaran;

	/** Jumlah yang telah dibayarkan atas header staging ini; default {@code 0.0}. */
	private Double jumlahTelahDibayar = 0.0;

	/** {@link Kegiatan} final yang ditautkan dari header staging ini (FK {@code kegiatan}); lihat catatan arah relasi pada Javadoc kelas. */
	private Kegiatan kegiatan;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate untuk instansiasi entity via refleksi.
	 */
	public KegiatanTemporary() {
	}

	/**
	 * Konstruktor dengan ID langsung — berguna untuk membuat referensi ringan (proxy manual)
	 * tanpa memuat seluruh baris dari database, mis. untuk dipakai sebagai FK pada entity lain.
	 *
	 * @param id primary key yang sudah diketahui
	 */
	public KegiatanTemporary(Long id) {
		this.id = id;
	}

	/**
	 * @return primary key baris {@code kegiatan_temporary}; {@code null} sebelum baris
	 *         di-{@code INSERT}.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id primary key; biasanya tidak perlu diset manual karena kolomnya {@code
	 *           insertable = false} (identity, dibangkitkan database).
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return jenis kegiatan/tagihan header staging ini (proxy lazy diresolusi via {@code
	 *         check()}); boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_kegiatan")
	public JenisKegiatan getJenisKegiatan() {
		jenisKegiatan = check(jenisKegiatan);
		return this.jenisKegiatan;
	}

	/**
	 * @param jenisKegiatan jenis kegiatan baru untuk header staging ini.
	 */
	public void setJenisKegiatan(JenisKegiatan jenisKegiatan) {
		this.jenisKegiatan = jenisKegiatan;
	}

	/**
	 * @param mahasiswa mahasiswa aktif pemilik baru; {@code null} untuk melepas tautan.
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * @return mahasiswa aktif pemilik header staging ini (proxy lazy diresolusi via {@code
	 *         check()}); {@code null} untuk header calon mahasiswa (lihat {@link
	 *         #getCalonMahasiswa()}).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * @param tahunAkademik tahun akademik baru untuk header staging ini.
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * @return tahun akademik header staging ini; boleh {@code null}.
	 */
	@Column(name = "tahun_akademik", length = 20)
	public String getTahunAkademik() {
		return tahunAkademik;
	}

	/**
	 * @param program program studi baru; bisa tetap ditimpa saat dibaca via {@link
	 *                #getProgram()} bila mahasiswa/calon mahasiswa terkait tersedia.
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * Program studi header staging ini.
	 *
	 * <p><b>Getter yang menulis balik (diturunkan dari relasi):</b> field {@link #program}
	 * ditimpa dengan {@code mahasiswa.getProgram()} (bila ada mahasiswa aktif terkait) atau
	 * {@code calonMahasiswa.getProgram()} (bila tidak ada mahasiswa tapi ada calon mahasiswa)
	 * setiap kali getter ini dipanggil — nilai yang pernah diset manual lewat {@link
	 * #setProgram(String)} akan tertimpa selama salah satu relasi itu tersedia dan mengembalikan
	 * program non-null.</p>
	 *
	 * @return program studi efektif (dari relasi bila tersedia, atau field lokal bila tidak);
	 *         boleh {@code null}.
	 */
	@Column(name = "program", length = 20)
	public String getProgram() {
		mahasiswa = getMahasiswa();
		calonMahasiswa = getCalonMahasiswa();
		if (mahasiswa != null) {
			program = mahasiswa.getProgram();
		} else if (calonMahasiswa != null) {
			program = calonMahasiswa.getProgram();
		}
		return program;
	}

	/**
	 * @param tanggal tanggal baru untuk header staging ini.
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Tanggal header staging ini.
	 *
	 * <p><b>Getter yang menulis balik (lazy-default):</b> bila field mentah {@code null},
	 * ditulis dan disimpan permanen menjadi waktu SAAT GETTER INI DIPANGGIL (bukan waktu baris
	 * dibuat) — sekadar membaca tanggal yang belum diisi pada satu instance dapat "membekukan"
	 * tanggal itu ke saat pembacaan pertama.</p>
	 *
	 * @return tanggal header staging ini; tidak pernah {@code null} setelah pembacaan pertama.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal")
	public Date getTanggal() {
		if (tanggal == null) {
			tanggal = ais.ui.util.WaktuUtil.getDate();
		}
		return tanggal;
	}

	/**
	 * @param semster semester baru untuk header staging ini.
	 */
	public void setSemster(Integer semster) {
		this.semster = semster;
	}

	/**
	 * @return semester header staging ini; boleh {@code null}.
	 */
	@Column(name = "semster", length = 20)
	public Integer getSemster() {
		return semster;
	}

	/**
	 * @param validated kode status validasi baru.
	 */
	public void setValidated(Integer validated) {
		this.validated = validated;
	}

	/**
	 * @return kode status validasi header staging ini; boleh {@code null}. Makna kode angka
	 *         ditentukan oleh pemanggil (tidak dinormalkan/didokumentasikan sebagai konstanta
	 *         pada kelas ini).
	 */
	@Column(name = "validated")
	public Integer getValidated() {
		return validated;
	}

	/**
	 * @param calonMahasiswa calon mahasiswa pemilik baru; {@code null} untuk melepas tautan.
	 */
	public void setCalonMahasiswa(BiodataCalonMahasiswa calonMahasiswa) {
		this.calonMahasiswa = calonMahasiswa;
	}

	/**
	 * @return calon mahasiswa (PMB) pemilik header staging ini (proxy lazy diresolusi via
	 *         {@code check()}); {@code null} untuk header mahasiswa aktif (lihat {@link
	 *         #getMahasiswa()}).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "calon_mahasiswa", nullable = true)
	public BiodataCalonMahasiswa getCalonMahasiswa() {
		calonMahasiswa = check(calonMahasiswa);
		return calonMahasiswa;
	}

	/**
	 * @param jadwalPembayaran jadwal pembayaran acuan baru; {@code null} untuk melepas tautan.
	 */
	public void setJadwalPembayaran(JadwalPembayaran jadwalPembayaran) {
		this.jadwalPembayaran = jadwalPembayaran;
	}

	/**
	 * @return jadwal pembayaran acuan header staging ini (proxy lazy diresolusi via {@code
	 *         check()}); boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jadwal_pembayaran", nullable = true)
	public JadwalPembayaran getJadwalPembayaran() {
		jadwalPembayaran = check(jadwalPembayaran);
		return jadwalPembayaran;
	}

	/**
	 * @param validator nama/ID validator baru.
	 */
	public void setValidator(String validator) {
		this.validator = validator;
	}

	/**
	 * @return nama/ID validator header staging ini; boleh {@code null}.
	 */
	public String getValidator() {
		return validator;
	}

	/**
	 * @param keterangan keterangan baru untuk header staging ini.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return keterangan bebas header staging ini; boleh {@code null} (berbeda dari beberapa
	 *         entity lain di cluster ini yang menormalkan hasil {@code null} menjadi {@code ""}).
	 */
	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * @param amount nominal tagihan baru.
	 */
	public void setAmount(Double amount) {
		this.amount = amount;
	}

	/**
	 * Nominal tagihan header staging ini.
	 *
	 * <p><b>Getter yang menulis balik (lazy-default):</b> bila field mentah {@code null},
	 * ditulis dan disimpan permanen menjadi {@code 0.0} pada pembacaan pertama.</p>
	 *
	 * @return nominal tagihan; tidak pernah {@code null} setelah pembacaan pertama.
	 */
	public Double getAmount() {
		if (amount == null) {
			amount = 0.0;
		}
		return amount;
	}

	/**
	 * @param statusMahasiswa status mahasiswa baru pada saat header staging ini dibuat.
	 */
	public void setStatusMahasiswa(StatusMahasiswa statusMahasiswa) {
		this.statusMahasiswa = statusMahasiswa;
	}

	/**
	 * @return status mahasiswa pada saat header staging ini dibuat (proxy lazy diresolusi via
	 *         {@code check()}); boleh {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "status_mahasiswa", nullable = true)
	public StatusMahasiswa getStatusMahasiswa() {
		statusMahasiswa = check(statusMahasiswa);
		return statusMahasiswa;
	}

	/**
	 * Jumlah yang telah dibayarkan atas header staging ini.
	 *
	 * <p><b>Getter yang menulis balik (lazy-default):</b> sama seperti {@link #getAmount()} —
	 * bila field mentah {@code null}, ditulis dan disimpan permanen menjadi {@code 0.0} pada
	 * pembacaan pertama (meski field ini sudah diinisialisasi {@code 0.0} sejak deklarasi,
	 * sehingga jalur {@code null} praktis hanya tercapai lewat deserialisasi/refleksi khusus).</p>
	 *
	 * @return jumlah yang telah dibayarkan; tidak pernah {@code null}.
	 */
	public Double getJumlahTelahDibayar() {
		if (jumlahTelahDibayar == null) {
			jumlahTelahDibayar = 0.0;
		}
		return jumlahTelahDibayar;
	}

	/**
	 * @param jumlahTelahDibayar jumlah yang telah dibayarkan, nilai baru.
	 */
	public void setJumlahTelahDibayar(Double jumlahTelahDibayar) {
		this.jumlahTelahDibayar = jumlahTelahDibayar;
	}

	/**
	 * {@link Kegiatan} final yang ditautkan dari header staging ini — lihat catatan arah relasi
	 * pada Javadoc kelas (FK ini menunjuk DARI staging KE final, bukan sebaliknya).
	 *
	 * <p><b>Tidak memakai {@code check()}:</b> berbeda dari enam relasi {@code @ManyToOne} lain
	 * di kelas ini, getter ini mengembalikan field {@link #kegiatan} apa adanya tanpa resolusi
	 * proxy lazy lewat {@code check()} — bila entity ini diakses di luar sesi Hibernate yang
	 * memuatnya, membaca relasi ini berisiko melempar {@code LazyInitializationException} yang
	 * TIDAK ditangani di sini (berbeda dari relasi lain yang dilindungi {@code check()}).
	 * Anotasi fetch-nya juga berbeda: memakai {@code @Fetch(FetchMode.SELECT)} eksplisit,
	 * bukan hanya {@code fetch = FetchType.LAZY} polos seperti relasi lain.</p>
	 *
	 * @return {@code Kegiatan} final terkait; boleh {@code null} bila header staging ini belum
	 *         ditautkan ke satu pun {@code Kegiatan}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kegiatan", nullable = true)
	public Kegiatan getKegiatan() {
		return kegiatan;
	}

	/**
	 * @param kegiatan {@code Kegiatan} final baru untuk ditautkan; {@code null} untuk melepas tautan.
	 */
	public void setKegiatan(Kegiatan kegiatan) {
		this.kegiatan = kegiatan;
	}

}
