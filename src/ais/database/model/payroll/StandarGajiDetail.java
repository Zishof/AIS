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

/**
 * Baris <b>detail/rincian</b> katalog "standar gaji" &mdash; satu baris di sini mewakili
 * <i>satu komponen gaji</i> ({@link ItemGaji}) di dalam satu header {@link StandarGaji} (relasi
 * banyak-ke-satu lewat kolom {@code standar_gaji}, lihat {@link #getStandarGaji()}), lengkap
 * dengan rumus perhitungannya sendiri atau penanda untuk mewarisi rumus bawaan dari
 * {@link ItemGaji} yang bersangkutan. Bentuknya sama persis dengan pola header/detail lain di
 * paket ini (mis. {@code FormatItemGaji} &rarr; {@code ItemGaji}): header menyimpan identitas
 * pengelompokan, detail menyimpan isian per komponen.
 *
 * <h2>Kelas ini TIDAK terhubung ke rantai penggajian aktif</h2>
 * <p>Sama seperti {@link StandarGaji} (lihat Javadoc kelas itu untuk rincian pembuktian lengkap):
 * <b>terverifikasi lewat pencarian teks di seluruh pohon sumber ini (2 Sep 2026)</b> bahwa tidak
 * ada satu pun {@code Action}, {@code DAO}, {@code listener}, atau berkas tampilan ZK yang
 * mengimpor, membuat, membaca, atau mengubah baris {@code StandarGajiDetail}. Satu-satunya
 * penyebutan di luar kedua kelas ini sendiri ada di Javadoc {@code payroll.FormatItemGaji} dan
 * {@code payroll.ItemGaji} (murni dokumentasi, bukan kode), ditambah pendaftaran mapping di
 * {@code hibernate.cfg.xml}. Rantai penggajian aktif ({@code ItemGajiPegawai},
 * {@code PembayaranItemGajiPegawai}, dst.) tidak pernah membaca baris di sini untuk menentukan
 * rumus/nominal slip &mdash; komponen tersebut punya jalur rumusnya sendiri lewat
 * {@link ItemGaji#getDefaultFormula()} dan override per-pegawai, sama sekali independen dari
 * {@code StandarGajiDetail}. Perlakukan kelas ini sebagai katalog dorman/yatim, bukan bagian
 * dari alur perhitungan slip yang berjalan hari ini.</p>
 *
 * <h2>Field audit bayangan (bukan bug)</h2>
 * <p>Seperti dijelaskan pada Javadoc {@link StandarGaji}, field {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} di sini dideklarasikan ulang (bukan diwarisi) karena
 * {@link GeneralValueObject} bukan {@code @Entity}; ketiganya tetap terpetakan sebagai kolom
 * persisten lewat properti default meski tanpa anotasi {@code @Column} eksplisit, dan diisi
 * otomatis oleh {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} lewat hook
 * {@link #onUpdate()} ({@code @PreUpdate}).</p>
 *
 * <p>Kelas ditandai {@code @Audited} (Hibernate Envers), jadi setiap perubahan baris tercatat di
 * tabel bayangan {@code standar_gaji_detail_aud}.</p>
 *
 * @see StandarGaji
 * @see ItemGaji
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "payroll", name = "standar_gaji_detail")
public class StandarGajiDetail extends GeneralValueObject {

	/**
	 * Versi serialisasi. Nilainya sama persis dengan {@code serialVersionUID} pada
	 * {@link StandarGaji} (kebetulan hasil generate hbm2java yang tidak diregenerasi ulang per
	 * kelas) &mdash; tidak berdampak apa pun karena {@code serialVersionUID} hanya dibandingkan
	 * dalam lingkup satu kelas yang sama saat deserialisasi.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris detail. Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna pengubah terakhir (field audit bayangan). Lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna pengubah terakhir (field audit bayangan). Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris detail ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir. Nilai {@code null}/kosong/spasi diabaikan diam-diam
	 * sehingga jejak audit yang sudah terisi tidak bisa terhapus oleh jalur simpan yang kebetulan
	 * tidak membawa informasi pengguna (mis. proses batch tanpa sesi login).
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks baris ini: langsung mengembalikan {@link #getNama()} (field {@code nama}
	 * apa adanya, tanpa {@code trim} maupun awalan kode) &mdash; dipakai komponen ZK seperti
	 * {@code Combobox}/{@code Listcell} untuk menampilkan pilihan baris detail.
	 *
	 * @return nama baris detail, bisa {@code null} bila belum pernah disetel
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
	 * Mengembalikan nama pengguna yang terakhir mengubah baris detail ini.
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
	 * pengguna/waktu sesi Hibernate yang sedang berjalan. Badan method ini sengaja tidak
	 * melakukan apa pun selain itu.
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

	/** Nama/label baris detail; kolom {@code nama}. Lihat {@link #getNama()}. */
	private String nama;
	/** Header standar gaji pemilik baris ini; kolom {@code standar_gaji}. Lihat {@link #getStandarGaji()}. */
	private StandarGaji standarGaji;
	/** Komponen gaji yang diwakili baris ini; kolom {@code item_gaji}. Lihat {@link #getItemGaji()}. */
	private ItemGaji itemGaji;
	/** Rumus perhitungan baris ini; kolom implisit {@code formula}. Lihat {@link #getFormula()}. */
	private String formula;
	/** Penanda "ikuti rumus bawaan {@link ItemGaji}". Lihat {@link #getIkutiFormulaItemGaji()}. */
	private Boolean ikutiFormulaItemGaji;
	/** Keterangan bebas; kolom {@code keterangan}. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/**
	 * Constructor default tanpa argumen, dibutuhkan Hibernate untuk hidrasi entity dari hasil
	 * query. Tidak menginisialisasi field selain yang sudah punya nilai default di deklarasi
	 * (mis. {@link #tanggal_dirubah}).
	 */
	public StandarGajiDetail() {
	}

	/**
	 * Mengembalikan primary key baris detail.
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
	 * Mengembalikan nama baris detail, sudah di-{@code trim()}.
	 *
	 * <p>Kolom {@code nama} bersifat {@code NOT NULL} dan panjang maksimum 255 karakter.</p>
	 *
	 * @return nama baris detail setelah di-{@code trim}, atau {@code null} bila field belum
	 *         disetel
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama baris detail. Tanpa validasi/normalisasi pada jalur setter; pemangkasan
	 * spasi baru terjadi saat dibaca lewat {@link #getNama()}.
	 *
	 * @param nama nama/label baris detail baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas untuk baris detail ini.
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
	 * Mengembalikan rumus perhitungan efektif baris ini &mdash; dan, bila
	 * {@link #getIkutiFormulaItemGaji()} bernilai {@code true} serta {@link #itemGaji} sudah
	 * terisi, method ini <b>menulis balik field {@link #formula}</b> sebelum mengembalikannya.
	 * Ini adalah pola <b>getter destruktif</b> (getter dengan efek samping menulis state) yang
	 * berulang di beberapa entity lain pada codebase ini &mdash; getter ini BUKAN operasi baca
	 * murni, walau tanda tangannya terlihat seperti accessor biasa.
	 *
	 * <h4>Alur persis</h4>
	 * <pre>{@code
	 * public String getFormula() {
	 *     if (ikutiFormulaItemGaji != null && ikutiFormulaItemGaji && itemGaji != null) {
	 *         formula = itemGaji.getDefaultFormula();
	 *     }
	 *     return formula;
	 * }
	 * }</pre>
	 * <p>Ketiga syarat berikut harus benar SEKALIGUS agar penulisan-balik terjadi:</p>
	 * <ol>
	 *   <li>{@link #ikutiFormulaItemGaji} tidak {@code null} (default field ini memang
	 *   {@code null}, bukan {@code false}, sehingga baris baru yang belum pernah disetel
	 *   TIDAK mengikuti perilaku ini sampai field-nya benar-benar diisi {@code true} secara
	 *   eksplisit);</li>
	 *   <li>nilainya {@code true}; dan</li>
	 *   <li>{@link #itemGaji} sudah terhubung (bukan {@code null}) &mdash; catatan: field ini
	 *   dibaca langsung tanpa lebih dulu melalui getter-nya sendiri, jadi bila {@code itemGaji}
	 *   berupa proxy lazy Hibernate yang belum diresolusi {@code check(...)}, pengecekan
	 *   {@code != null} di sini tetap lolos (proxy bukan {@code null}) tetapi
	 *   {@link ItemGaji#getDefaultFormula()} yang dipanggil setelahnya bisa melempar
	 *   {@code LazyInitializationException} bila proxy tersebut sudah <i>detached</i> dari
	 *   session Hibernate-nya &mdash; berbeda dari pola getter relasi standar di
	 *   {@link GeneralValueObject} yang selalu memanggil {@code check(...)} lebih dulu.</li>
	 * </ol>
	 * <p>Bila ketiga syarat terpenuhi, {@link #formula} DITIMPA dengan
	 * {@link ItemGaji#getDefaultFormula()} milik {@link #itemGaji} saat ini &mdash; nilai apa pun
	 * yang sebelumnya tersimpan di {@code formula} (baik dari database maupun dari pemanggilan
	 * {@link #setFormula(String)} sebelumnya dalam permintaan yang sama) hilang begitu saja dan
	 * digantikan salinan rumus bawaan komponen. Efek ini murni <b>in-memory</b>: memanggil getter
	 * ini TIDAK menulis ke database dengan sendirinya, tetapi bila object ini kemudian disimpan
	 * lewat sesi Hibernate yang sama (mis. karena kode caller memanggil getter ini untuk
	 * ditampilkan lalu tanpa sengaja memicu {@code save}/{@code update} pada object yang sama),
	 * nilai {@code formula} yang termodifikasi itu akan ikut ter-persist &mdash; pola risiko yang
	 * sama seperti getter destruktif lain yang sudah terdokumentasi di batch javadoc sebelumnya
	 * pada paket ini (mis. {@code PembayaranGajiPunyaPegawai.getFormatItemGaji()} yang menulis
	 * balik warisan dari {@code pegawai.getFormatItemGaji()} &mdash; lihat Javadoc
	 * {@link FormatItemGaji}).</p>
	 * <p>Konsekuensi praktis bagi pemanggil: memanggil getter ini dua kali berturut-turut pada
	 * object yang sama boleh jadi menghasilkan nilai yang SAMA (karena penimpaan kedua memakai
	 * {@link ItemGaji#getDefaultFormula()} yang sama), tetapi nilai itu tidak lagi mencerminkan
	 * rumus KHUSUS yang sempat tersimpan di baris ini sebelum {@code ikutiFormulaItemGaji}
	 * dinyalakan &mdash; rumus lama itu, bila pernah ada, sudah tertimpa dan tidak bisa
	 * dikembalikan lewat kelas ini. Karena kelas ini saat ini tidak dijangkau kode aplikasi
	 * aktif manapun (lihat Javadoc kelas), belum ada bukti bug ini pernah nyata tereksploitasi
	 * dalam produksi; catatan ini disiapkan untuk pembaca yang mengaktifkan kembali fitur ini.
	 *
	 * @return rumus efektif baris ini: rumus bawaan {@link ItemGaji} bila
	 *         {@link #ikutiFormulaItemGaji} menyala dan komponen tersambung, atau nilai
	 *         {@link #formula} apa adanya pada kondisi lain (termasuk {@code null} bila belum
	 *         pernah disetel)
	 */
	public String getFormula() {
		if (ikutiFormulaItemGaji != null && ikutiFormulaItemGaji && itemGaji != null) {
			formula = itemGaji.getDefaultFormula();
		}
		return formula;
	}

	/**
	 * Menyetel rumus perhitungan baris ini secara manual. Tanpa validasi sintaks maupun
	 * pengecekan terhadap {@link #ikutiFormulaItemGaji}: menyetel nilai lewat method ini tidak
	 * mematikan {@code ikutiFormulaItemGaji}, sehingga pemanggilan {@link #getFormula()}
	 * berikutnya akan tetap menimpa nilai yang baru disetel di sini bila penanda itu masih
	 * {@code true}.
	 *
	 * @param formula ekspresi rumus baru
	 */
	public void setFormula(String formula) {
		this.formula = formula;
	}

	/**
	 * Mengembalikan penanda apakah baris ini mengikuti rumus bawaan {@link ItemGaji} (bila
	 * {@code true}) atau memakai rumus manualnya sendiri di {@link #formula} (bila {@code false}
	 * atau {@code null}). Lihat {@link #getFormula()} untuk efek penulisan-balik yang dipicu
	 * penanda ini.
	 *
	 * @return {@code true} bila mengikuti rumus bawaan komponen, {@code false}/{@code null} bila
	 *         memakai rumus sendiri
	 */
	public Boolean getIkutiFormulaItemGaji() {
		return ikutiFormulaItemGaji;
	}

	/**
	 * Menyetel penanda "ikuti rumus bawaan {@link ItemGaji}". Tanpa validasi.
	 *
	 * @param ikutiFormulaItemGaji penanda baru; boleh {@code null} (diperlakukan sama seperti
	 *        {@code false} oleh {@link #getFormula()})
	 */
	public void setIkutiFormulaItemGaji(Boolean ikutiFormulaItemGaji) {
		this.ikutiFormulaItemGaji = ikutiFormulaItemGaji;
	}

	/**
	 * Mengembalikan header {@link StandarGaji} pemilik baris detail ini.
	 *
	 * @return header standar gaji terkait, atau {@code null} bila baris ini belum dikaitkan ke
	 *         header mana pun
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "standar_gaji", nullable = true)
	public StandarGaji getStandarGaji() {
		return standarGaji;
	}

	/**
	 * Mengaitkan baris ini ke sebuah header {@link StandarGaji}. Tanpa validasi; {@code cascade}
	 * {@code PERSIST}/{@code MERGE} berarti menyimpan {@code StandarGajiDetail} yang membawa
	 * object {@code StandarGaji} baru (belum ber-id) akan ikut menyimpan/menggabungkan object
	 * itu.
	 *
	 * @param standarGaji header standar gaji baru, boleh {@code null} untuk melepas kaitan
	 */
	public void setStandarGaji(StandarGaji standarGaji) {
		this.standarGaji = standarGaji;
	}

	/**
	 * Mengembalikan {@link ItemGaji} (komponen gaji) yang diwakili baris detail ini.
	 *
	 * <p>Nilai relasi ini yang menentukan sumber rumus bawaan pada {@link #getFormula()} ketika
	 * {@link #ikutiFormulaItemGaji} menyala.</p>
	 *
	 * @return komponen gaji terkait, atau {@code null} bila baris ini belum dikaitkan ke
	 *         komponen mana pun
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "item_gaji", nullable = true)
	public ItemGaji getItemGaji() {
		return itemGaji;
	}

	/**
	 * Mengaitkan baris ini ke sebuah {@link ItemGaji}. Tanpa validasi; {@code cascade}
	 * {@code PERSIST}/{@code MERGE} berarti menyimpan {@code StandarGajiDetail} yang membawa
	 * object {@code ItemGaji} baru (belum ber-id) akan ikut menyimpan/menggabungkan object itu.
	 *
	 * @param itemGaji komponen gaji baru, boleh {@code null} untuk melepas kaitan
	 */
	public void setItemGaji(ItemGaji itemGaji) {
		this.itemGaji = itemGaji;
	}

}
