package ais.database.model.rab;

// Generated Dec 20, 2009 1:12:40 PM by Hibernate Tools 3.2.4.CR1

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




import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;



/**
 * Master "satuan hasil" (satuan output) RAB — kombinasi <b>dua</b> {@link Satuan} dasar
 * ({@link #getSatuan1()} dan {@link #getSatuan2()}) menjadi satu satuan majemuk berlabel, mis.
 * satuan "Orang" &times; satuan "Bulan" = label {@code "OB"} (Orang-Bulan) untuk keperluan tolok
 * ukur output kegiatan (TOR) pada RAB. {@code label} adalah singkatan/kode tampil satuan majemuk
 * itu; {@code keterangan} adalah teks penjelas bebas.
 *
 * <p><b>Bukan sinonim dari {@link Satuan}.</b> {@link Satuan} adalah master satuan ukur atomik
 * tunggal (mis. "Orang", "Bulan", "Paket"); {@code HasilSatuan} adalah entity TERPISAH yang
 * <i>merujuk</i> dua {@link Satuan} sekaligus lewat {@code @ManyToOne} untuk menyatakan hasil
 * perkalian/kombinasinya. Baris {@code HasilSatuan} dijaga unik per pasangan
 * {@code (satuan1, satuan2)} oleh pemanggilnya (lihat
 * {@code ais.action.master.rab.util.RabImporter}, yang mencari baris existing lewat
 * {@code Restrictions.eq("satuan1", ...)}/{@code Restrictions.eq("satuan2", ...)} sebelum membuat
 * baris baru) — kelas ini sendiri tidak memaksakan constraint keunikan itu di level entity/DB.</p>
 *
 * <p>Dipakai oleh layar CRUD {@code ais.action.master.rab.HasilSatuanAction}, oleh
 * {@code RabImporter} saat mengimpor data RAB dari berkas eksternal (membuat {@link Satuan}
 * komponen dan baris {@code HasilSatuan} gabungannya sekaligus bila belum ada), serta oleh
 * {@code WorkspaceRevisiAction}/{@code WorkspaceRevisiBulananAction} saat memilih satuan hasil
 * pada revisi anggaran.</p>
 *
 * <h2>Pola arsitektur khas AIS yang muncul di kelas ini</h2>
 * <p>Field {@code oleh}, {@code olehId}, {@code tanggal_dirubah} beserta method
 * {@link #onUpdate()} adalah <b>field audit bayangan</b> yang menduplikasi field privat bernama
 * sama di {@link GeneralValueObject} — KEHARUSAN TEKNIS (induk bukan {@code @Entity} sehingga
 * tidak bisa mewariskan pemetaan kolom JPA), bukan salin-tempel ceroboh. Baris
 * {@code oleh}/{@code olehId} pada kelas ini bahkan dipadatkan menjadi satu baris fisik
 * (peninggalan generator), tetapi perilakunya identik dengan entity RAB lain di paket ini.</p>
 *
 * @see Satuan
 * @see ais.action.master.rab.HasilSatuanAction
 * @see ais.action.master.rab.util.RabImporter
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "rab", name = "hasil_satuan")



public class HasilSatuan extends GeneralValueObject {

	/**
	 * Versi serialisasi kelas. Nilainya spesifik untuk {@code HasilSatuan} (berbeda dari beberapa
	 * entity katalog RAB lain yang berbagi nilai turunan generator hbm2java).
	 */
	private static final long serialVersionUID = 4588020802004493802L;
	/** Primary key baris {@code rab.hasil_satuan}. Lihat {@link #getId()}. */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris ini (field audit bayangan). Lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini (field audit bayangan). Lihat {@link #getOlehId()}. */
	private String olehId;
	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {return olehId;}
	/**
	 * Menyetel id pengguna pengubah terakhir. Nilai {@code null} atau string kosong/spasi
	 * <b>diabaikan diam-diam</b> agar jejak audit yang sudah terisi tidak bisa terhapus oleh
	 * jalur simpan yang kebetulan tidak membawa informasi pengguna (mis. proses import batch).
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan validasi non-trivial yang sama seperti
	 * {@link #setOlehId(String)}: nilai {@code null}/kosong diabaikan diam-diam.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook {@code @PreUpdate} yang mengimplementasikan kontrak abstrak
	 * {@link GeneralValueObject#onUpdate()}: dipanggil JPA tepat sebelum UPDATE dieksekusi, dan
	 * menyerahkan penyegaran {@link #tanggal_dirubah} ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}. Karena kait ini hanya
	 * menempel pada {@code @PreUpdate} (bukan {@code @PrePersist}), jejak waktu pada operasi INSERT
	 * pertama bergantung sepenuhnya pada nilai awal field di bawah ini.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}
	/**
	 * Cap waktu perubahan terakhir (field audit bayangan; lihat catatan kelas). Diinisialisasi ke
	 * waktu server saat object dibuat lewat {@code WaktuUtil.getDate()} sehingga baris baru tidak
	 * pernah membawa nilai {@code null}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel cap waktu perubahan terakhir. Tanpa validasi — berbeda dari {@link #setOleh(String)}/
	 * {@link #setOlehId(String)}, {@code null} akan benar-benar tersimpan.
	 *
	 * @param tanggal_dirubah cap waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir, dipetakan sebagai kolom {@code TIMESTAMP}.
	 *
	 * @return cap waktu perubahan terakhir; praktis tidak pernah {@code null} untuk object yang
	 *         dibuat lewat konstruktor kelas ini
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Komponen satuan pertama dari kombinasi ini, mis. "Orang". Lihat {@link #getSatuan1()}. */
	private Satuan satuan1;
	/** Komponen satuan kedua dari kombinasi ini, mis. "Bulan". Lihat {@link #getSatuan2()}. */
	private Satuan satuan2;
	/** Label/kode tampil satuan majemuk hasil kombinasi {@link #satuan1} &times; {@link #satuan2}, mis. "OB". Lihat {@link #getLabel()}. */
	private String label;
	/** Keterangan bebas untuk satuan hasil ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/**
	 * Constructor default tanpa argumen, WAJIB ada agar Hibernate dapat menginstansiasi entity
	 * lewat refleksi saat memuat baris dari database, dan agar layar CRUD dapat membuat object
	 * kosong untuk form tambah-baru. Tidak seperti beberapa entity katalog RAB lain, kelas ini
	 * tidak menyediakan constructor pintas bertipe {@code String}/{@code Long}.
	 */
	public HasilSatuan() {
	}

	/**
	 * Mengembalikan primary key baris {@code rab.hasil_satuan}.
	 *
	 * @return primary key, atau {@code null} bila entity belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Tanpa validasi.
	 *
	 * @param id nilai primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan komponen satuan pertama kombinasi ini. Relasi {@code @ManyToOne} dengan
	 * {@code cascade = {PERSIST, MERGE}}: menyimpan {@code HasilSatuan} yang membawa
	 * {@link Satuan} baru (belum punya {@code id}) akan ikut menyimpan baris {@link Satuan}
	 * tersebut secara otomatis. Kolom {@code satuan1} nullable di database, meski secara
	 * konseptual kombinasi tanpa komponen pertama jarang berguna.
	 *
	 * @return entity {@link Satuan} komponen pertama, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@JoinColumn(name = "satuan1", nullable = true)
	public Satuan getSatuan1() {
		return this.satuan1;
	}

	/**
	 * Menyetel komponen satuan pertama. Tanpa validasi.
	 *
	 * @param satuan1 entity {@link Satuan} komponen pertama
	 */
	public void setSatuan1(Satuan satuan1) {
		this.satuan1 = satuan1;
	}

	/**
	 * Mengembalikan komponen satuan kedua kombinasi ini. Perilaku cascade dan nullability sama
	 * seperti {@link #getSatuan1()}.
	 *
	 * @return entity {@link Satuan} komponen kedua, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@JoinColumn(name = "satuan2", nullable = true)
	public Satuan getSatuan2() {
		return this.satuan2;
	}

	/**
	 * Menyetel komponen satuan kedua. Tanpa validasi.
	 *
	 * @param satuan2 entity {@link Satuan} komponen kedua
	 */
	public void setSatuan2(Satuan satuan2) {
		this.satuan2 = satuan2;
	}

	/**
	 * Mengembalikan label/kode tampil satuan majemuk ini. Kolom wajib diisi
	 * ({@code nullable = false}) pada tabel, berbeda dari {@link #getSatuan1()}/
	 * {@link #getSatuan2()} yang nullable.
	 *
	 * @return label satuan hasil, mis. "OB"
	 */
	@Column(name = "label", nullable = false, length = 50)
	public String getLabel() {
		return this.label;
	}

	/**
	 * Menyetel label satuan hasil. Tanpa validasi maupun pemangkasan spasi, berbeda dari
	 * {@link Satuan#getNama()}/{@link MetodePengadaan#getNama()} yang men-trim saat dibaca.
	 *
	 * @param label label satuan hasil baru
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * Mengembalikan keterangan bebas satuan hasil ini.
	 *
	 * @return teks keterangan, atau {@code null} bila belum diisi
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel keterangan bebas. Tanpa validasi.
	 *
	 * @param keterangan teks keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

}
