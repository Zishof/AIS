package ais.database.model.payroll;

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

import ais.database.model.GeneralValueObject;
import ais.database.model.Jabatan;

/**
 * Baris <b>header</b> katalog "standar gaji" &mdash; patokan/skala gaji yang dikelompokkan per
 * {@link Jabatan} dan (opsional) per {@link FormatItemGaji}, dengan rincian nominal/formula per
 * komponen disimpan terpisah pada {@link StandarGajiDetail} (relasi satu-ke-banyak lewat FK
 * {@code standar_gaji} di tabel {@code payroll.standar_gaji_detail} &mdash; lihat
 * {@link StandarGajiDetail#getStandarGaji()}). Kelas ini sendiri hanya menyimpan identitas
 * (nama, keterangan) dan dua penanda pengelompokan; tidak satu pun nominal atau rumus tersimpan
 * di sini.
 *
 * <h2>Kelas ini TIDAK terhubung ke rantai penggajian aktif</h2>
 * <p><b>Terverifikasi lewat pencarian teks di seluruh pohon sumber ini (2 Sep 2026):</b> selain
 * kelas ini sendiri dan {@link StandarGajiDetail}, satu-satunya berkas lain yang menyebut
 * {@code StandarGaji} adalah {@code payroll.FormatItemGaji} dan {@code payroll.ItemGaji} &mdash;
 * dan keduanya HANYA menyebutnya di teks Javadoc (dokumentasi/catatan), bukan di kode yang
 * benar-benar dijalankan. Tidak ada satu pun {@code Action}, {@code DAO}, {@code listener},
 * maupun berkas tampilan ZK ({@code .zul}) di pohon sumber ini yang mengimpor, membuat, membaca,
 * atau mengubah baris {@code StandarGaji}/{@code StandarGajiDetail}. Rantai penggajian aktif
 * ({@code ItemGajiPegawai}, {@code PembayaranItemGajiPegawai}, {@code RencanaItemGajiPegawai},
 * {@code PembayaranGajiPunyaPegawai}, dan seterusnya &mdash; sudah didokumentasikan pada batch
 * javadoc sebelumnya) sama sekali tidak menunjuk balik ke entity ini. Satu-satunya "penggunaan"
 * yang tersisa adalah pemetaan Hibernate itu sendiri: {@code hibernate.cfg.xml} mendaftarkan
 * kedua kelas ini sebagai {@code <mapping class="...">}, sehingga tabelnya tetap dikenal
 * Hibernate (DDL/skema tetap tersinkron) walau tidak ada jalur aplikasi yang memakainya.</p>
 * <p>Kesimpulannya: per kondisi kode saat ini, {@code StandarGaji}/{@code StandarGajiDetail}
 * adalah pasangan entity <b>dorman/yatim (orphaned)</b> &mdash; dipetakan lengkap dengan relasi
 * dan validasi, tetapi tidak dijangkau fitur apa pun yang aktif. Ini bukan indikasi tabel kosong
 * di database (baris bisa saja pernah diisi lewat skrip SQL manual atau modul yang sudah
 * dihapus), namun pembaca yang mencari "dari mana standar gaji dipakai untuk menghitung slip"
 * TIDAK akan menemukan jawabannya di kode Java manapun selain dua kelas ini. Bila suatu saat
 * fitur ini diaktifkan kembali, periksa ulang bagian tenant di bawah karena kolom
 * {@link #getFormatItemGaji()} yang jadi satu-satunya kandidat pembawa {@code satuan_kerja}
 * bersifat <b>nullable</b> &mdash; berbeda dengan entity aktif seperti {@code ItemGaji} yang
 * mewajibkan kolom itu {@code NOT NULL} (lihat Javadoc {@link FormatItemGaji}).</p>
 *
 * <h2>Field audit bayangan (bukan bug)</h2>
 * <p>Seperti hampir seluruh entity {@code @Entity} turunan {@link GeneralValueObject} di paket
 * ini, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} dideklarasikan ULANG sebagai
 * field privat di kelas ini alih-alih mewarisi langsung dari induk &mdash; ini keharusan teknis,
 * bukan duplikasi ceroboh: {@link GeneralValueObject} sendiri <b>bukan</b> {@code @Entity}
 * (tidak beranotasi JPA), sehingga field-nya tidak otomatis menjadi kolom database bagi
 * subclass; setiap entity konkret wajib memiliki accessor sendiri agar Hibernate memetakannya.
 * Karena kelas ini memakai <i>property access</i> (anotasi JPA ditaruh di atas getter, lihat
 * {@link #getId()}) dan getter {@link #getOleh()}/{@link #getOlehId()}/{@link #getTanggal_dirubah()}
 * TIDAK ditandai {@code @Transient}, Hibernate tetap memetakan ketiganya sebagai kolom persisten
 * memakai nama properti default ({@code oleh}, {@code olehid}, {@code tanggal_dirubah}) meski
 * tidak ada anotasi {@code @Column} eksplisit di sini. Ketiganya diisi otomatis oleh
 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} lewat hook
 * {@link #onUpdate()} (dipicu {@code @PreUpdate}) setiap kali baris diperbarui lewat sesi
 * Hibernate biasa &mdash; lihat {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(Object)} yang menimpa
 * ketiga field ini dengan pengguna/waktu sesi saat ini, kecuali {@code AuditTrailHelper}
 * memutuskan tidak ada perubahan bisnis untuk dicatat.</p>
 *
 * <p>Kelas ditandai {@code @Audited} (Hibernate Envers), jadi setiap perubahan baris tercatat di
 * tabel bayangan {@code standar_gaji_aud}.</p>
 *
 * @see StandarGajiDetail
 * @see FormatItemGaji
 * @see Jabatan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "payroll", name = "standar_gaji")
public class StandarGaji extends GeneralValueObject {

	/**
	 * Versi serialisasi. Nilainya sama persis dengan {@code serialVersionUID} pada
	 * {@link StandarGajiDetail} (kebetulan hasil generate hbm2java yang tidak diregenerasi ulang
	 * per kelas) &mdash; tidak berdampak apa pun karena {@code serialVersionUID} hanya
	 * dibandingkan dalam lingkup satu kelas yang sama saat deserialisasi.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris standar gaji. Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna pengubah terakhir (field audit bayangan). Lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna pengubah terakhir (field audit bayangan). Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris standar gaji ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir. Nilai {@code null}/kosong/spasi diabaikan diam-diam
	 * sehingga jejak audit yang sudah terisi tidak bisa terhapus oleh jalur simpan yang kebetulan
	 * tidak membawa informasi pengguna (mis. proses batch tanpa sesi login) &mdash; pola yang sama
	 * seperti {@code GeneralValueObject.setOlehId(String)}.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks baris ini: langsung mengembalikan {@link #getNama()} (field {@code nama}
	 * apa adanya, tanpa {@code trim} maupun awalan kode) &mdash; dipakai komponen ZK seperti
	 * {@code Combobox}/{@code Listcell} untuk menampilkan pilihan standar gaji. Berbeda dari
	 * {@code GeneralValueObject.toString()} yang menambahkan awalan {@code kode -}, kelas ini
	 * meng-override langsung tanpa kode.
	 *
	 * @return nama standar gaji, bisa {@code null} bila belum pernah disetel
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan aturan abaikan-bila-kosong yang sama
	 * seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris standar gaji ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook siklus-hidup Hibernate yang WAJIB diimplementasikan tiap subclass
	 * {@link GeneralValueObject}. Dipicu otomatis oleh anotasi {@code @PreUpdate} sesaat sebelum
	 * Hibernate menuliskan pembaruan baris ini ke database, dan hanya mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang menimpa
	 * {@link #oleh}, {@link #olehId}, dan {@link #tanggal_dirubah} dengan identitas
	 * pengguna/waktu sesi Hibernate yang sedang berjalan (kecuali dianggap tidak ada perubahan
	 * bisnis yang perlu dicatat). Badan method ini sengaja tidak melakukan apa pun selain itu;
	 * tidak ada logika turunan/normalisasi lain milik {@code StandarGaji} di sini.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi; nilai ini biasanya ditimpa lagi
	 * oleh {@link #onUpdate()} pada jalur simpan Hibernate normal.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir. Diinisialisasi ke waktu pembuatan object
	 * ({@code WaktuUtil.getDate()}) sehingga instance baru selalu punya nilai walau belum pernah
	 * disimpan.
	 *
	 * @return waktu perubahan terakhir baris ini
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nama/label standar gaji; kolom {@code nama}. Lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas; kolom {@code keterangan}. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Penanda format/tenant opsional; kolom {@code format_item_gaji}. Lihat {@link #getFormatItemGaji()}. */
	private FormatItemGaji formatItemGaji;
	/** Jabatan/golongan yang menjadi acuan standar gaji ini; kolom {@code jabatan}. Lihat {@link #getJabatan()}. */
	private Jabatan jabatan;

	/**
	 * Constructor default tanpa argumen, dibutuhkan Hibernate untuk hidrasi entity dari hasil
	 * query. Tidak menginisialisasi field selain yang sudah punya nilai default di deklarasi
	 * (mis. {@link #tanggal_dirubah}).
	 */
	public StandarGaji() {
	}

	/**
	 * Mengembalikan primary key baris standar gaji.
	 *
	 * <p>Dipetakan {@code insertable = false} karena nilainya dibangkitkan database
	 * ({@link GeneratedValue} strategi {@link javax.persistence.GenerationType#IDENTITY}), bukan
	 * disetel aplikasi saat insert.</p>
	 *
	 * @return primary key, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Tanpa validasi; normalnya hanya dipakai Hibernate sendiri saat
	 * hidrasi, atau untuk membuat object "penunjuk" berisi id saja.
	 *
	 * @param id nilai primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama standar gaji, sudah di-{@code trim()}.
	 *
	 * <p>Kolom {@code nama} bersifat {@code NOT NULL} dan panjang maksimum 255 karakter. Nilai ini
	 * juga yang dikembalikan {@link #toString()} tanpa {@code trim} tambahan (karena field
	 * {@code nama} dibaca langsung, bukan lewat getter ini, pada {@link #toString()}).</p>
	 *
	 * @return nama standar gaji setelah di-{@code trim}, atau {@code null} bila field belum
	 *         disetel
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama standar gaji. Tanpa validasi/normalisasi pada jalur setter; pemangkasan
	 * spasi baru terjadi saat dibaca lewat {@link #getNama()}.
	 *
	 * @param nama nama/label standar gaji baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas untuk baris standar gaji ini.
	 *
	 * @return teks keterangan, boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas. Tanpa validasi.
	 *
	 * @param keterangan teks keterangan baru, boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan {@link FormatItemGaji} opsional yang menjadi penanda pengelompokan/tenant
	 * bagi baris standar gaji ini.
	 *
	 * <p>Kolom {@code format_item_gaji} <b>nullable</b> &mdash; berbeda dari entity aktif seperti
	 * {@code ItemGaji}/{@code ItemGajiPegawai} yang mewajibkannya {@code NOT NULL} sebagai
	 * satu-satunya jalur ke {@code satuan_kerja} (lihat Javadoc kelas {@link FormatItemGaji}).
	 * Karena kelas ini tidak dipakai jalur aplikasi mana pun saat ini (lihat catatan di Javadoc
	 * kelas), kolom ini praktis tidak punya konsumen yang menegakkan cakupan tenant &mdash;
	 * catat ini sebagai peringatan bila entity diaktifkan kembali di masa depan, bukan sebagai
	 * kerentanan yang sedang dieksploitasi kode aktif manapun.</p>
	 *
	 * @return format item gaji terkait, atau {@code null} bila baris ini tidak dikaitkan ke
	 *         format/tenant tertentu
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "format_item_gaji", nullable = true)
	public FormatItemGaji getFormatItemGaji() {
		return formatItemGaji;
	}

	/**
	 * Mengaitkan baris ini ke sebuah {@link FormatItemGaji}. Tanpa validasi; {@code cascade}
	 * {@code PERSIST}/{@code MERGE} berarti menyimpan {@code StandarGaji} yang membawa object
	 * {@code FormatItemGaji} baru (belum ber-id) akan ikut menyimpan/menggabungkan object itu.
	 *
	 * @param formatItemGaji format item gaji baru, boleh {@code null} untuk melepas kaitan
	 */
	public void setFormatItemGaji(FormatItemGaji formatItemGaji) {
		this.formatItemGaji = formatItemGaji;
	}

	/**
	 * Mengembalikan {@link Jabatan} yang menjadi acuan golongan/jabatan bagi standar gaji ini
	 * &mdash; mis. "standar gaji Guru Golongan III".
	 *
	 * @return jabatan terkait, atau {@code null} bila standar gaji ini tidak dibatasi ke jabatan
	 *         tertentu
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jabatan", nullable = true)
	public Jabatan getJabatan() {
		return jabatan;
	}

	/**
	 * Mengaitkan baris ini ke sebuah {@link Jabatan}. Tanpa validasi; {@code cascade}
	 * {@code PERSIST}/{@code MERGE} berarti menyimpan {@code StandarGaji} yang membawa object
	 * {@code Jabatan} baru (belum ber-id) akan ikut menyimpan/menggabungkan object itu.
	 *
	 * @param jabatan jabatan baru, boleh {@code null} untuk melepas kaitan
	 */
	public void setJabatan(Jabatan jabatan) {
		this.jabatan = jabatan;
	}

}
