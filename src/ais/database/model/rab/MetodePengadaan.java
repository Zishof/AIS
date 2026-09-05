package ais.database.model.rab;

// Generated Dec 19, 2009 10:58:09 PM by Hibernate Tools 3.2.4.CR1

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
 * Master metode pengadaan RAB (Rencana Anggaran Biaya) — mis. lelang, penunjukan langsung,
 * pengadaan langsung, swakelola, e-purchasing, dsb. Daftar nilainya <b>sepenuhnya data-driven</b>:
 * kelas ini tidak menghardcode satu pun nama metode, hanya menyediakan pasangan {@code nama}/
 * {@code keterangan} bebas yang diisi dan dipelihara pengguna lewat layar CRUD
 * {@code ais.action.master.rab.MetodePengadaanAction} (validasi keunikan nama dilakukan di
 * action tersebut, bukan di kelas ini).
 *
 * <p>Satu-satunya pemakai relasi entity ini adalah
 * {@link ais.database.model.rab.Workspace#getMetodePengadaan()}: setiap item RAB (workspace)
 * dapat menandai metode pengadaan mana yang direncanakan untuknya. Selain lewat
 * {@code MetodePengadaanAction}, entity ini juga dipakai oleh {@code WorkspaceRevisiAction}/
 * {@code WorkspaceRevisiBulananAction} saat memilih ulang metode pengadaan pada proses revisi
 * anggaran, dan disemai lewat {@link ais.common.InitData} pada bootstrap aplikasi.</p>
 *
 * <h2>Pola arsitektur khas AIS yang muncul di kelas ini</h2>
 * <p>Field {@code oleh}, {@code olehId}, {@code tanggal_dirubah} beserta method
 * {@link #onUpdate()} adalah <b>field audit bayangan</b> yang menduplikasi field privat bernama
 * sama di {@link GeneralValueObject}. Ini KEHARUSAN TEKNIS, bukan salin-tempel ceroboh: induk
 * bukan {@code @Entity} sehingga tidak bisa mewariskan pemetaan kolom JPA — setiap subclass yang
 * ingin memetakan kolom {@code oleh}/{@code olehId} wajib mendeklarasikan ulang field privatnya
 * sendiri agar anotasi Hibernate pada getter/setternya memiliki backing field yang benar.</p>
 *
 * @see ais.database.model.rab.Workspace#getMetodePengadaan()
 * @see ais.action.master.rab.MetodePengadaanAction
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "rab", name = "metode_pengadaan")



public class MetodePengadaan extends GeneralValueObject {

	/**
	 * Versi serialisasi kelas. Nilainya identik dengan beberapa entity katalog RAB lain
	 * (peninggalan hasil salin-tempel generator hbm2java); tidak masalah selama tidak ada dua
	 * kelas berbeda yang benar-benar diserialkan/dideserialkan saling tertukar sebagai satu sama
	 * lain.
	 */
	private static final long serialVersionUID = -8738027816264807168L;
	/** Primary key baris {@code rab.metode_pengadaan}. Lihat {@link #getId()}. */
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
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir. Nilai {@code null} atau string kosong/spasi
	 * <b>diabaikan diam-diam</b> agar jejak audit yang sudah terisi tidak bisa terhapus oleh
	 * jalur simpan yang kebetulan tidak membawa informasi pengguna (mis. proses batch/import).
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

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

	/** Nama metode pengadaan, mis. "Lelang", "Penunjukan Langsung" (bebas diisi pengguna). Lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas untuk metode pengadaan ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/**
	 * Constructor default tanpa argumen, WAJIB ada agar Hibernate dapat menginstansiasi entity
	 * lewat refleksi saat memuat baris dari database, dan agar layar CRUD dapat membuat object
	 * kosong untuk form tambah-baru.
	 */
	public MetodePengadaan() {
	}

	/**
	 * Constructor pintas untuk langsung menyetel nama metode pengadaan, dipakai jalur
	 * pembuatan cepat (mis. seed data awal).
	 *
	 * @param nama nama metode pengadaan
	 */
	public MetodePengadaan(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan primary key baris {@code rab.metode_pengadaan}.
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
	 * Mengembalikan nama metode pengadaan, sudah di-{@code trim()} bila tidak {@code null}.
	 * Kolom wajib diisi ({@code nullable = false}) pada tabel.
	 *
	 * @return nama metode pengadaan yang sudah dipangkas spasi tepi, atau {@code null} bila field
	 *         mentahnya {@code null}
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama metode pengadaan. Tanpa validasi maupun pemangkasan spasi di sisi setter —
	 * pemangkasan hanya terjadi saat dibaca lewat {@link #getNama()}.
	 *
	 * @param nama nama metode pengadaan baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas metode pengadaan ini.
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
