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
 * Master dimensi lokasi geografis RAB — satu baris mewakili satu wilayah/lokasi berkode (mis.
 * provinsi/kabupaten-kota, tergantung granularitas data yang dimasukkan pengguna lewat proses
 * seed/import; kelas ini sendiri tidak membatasi jenjang wilayahnya). Dipakai sebagai relasi
 * {@code @ManyToOne} bersama oleh tiga entity organisasi RAB: {@link SatuanKerja#getSatuanLokasi()},
 * {@link Kppn#getSatuanLokasi()}, dan diseminasi lewat {@link ais.common.InitData} pada bootstrap
 * aplikasi — ketiganya menyatakan "di wilayah mana" satuan kerja/KPPN itu berada. Entity ini
 * murni master ringan berisi {@code kode}, {@code nama}, dan {@code keterangan}; tidak menyimpan
 * hierarki wilayah (parent/child) maupun koordinat geografis.
 *
 * <p>Dipilih pengguna lewat komponen combo pencarian
 * {@code ais.action.master.rab.helper.AmbilDataSatuanLokasiBanbox} pada layar
 * {@code SatuanKerjaAction}/{@code SatuanKerjaTreeAction}, dan dibaca sebagai dimensi pengelompokan
 * pada {@code ais.action.report.format1.akunting.LaporanAkuntingHelper} saat menyusun laporan per
 * wilayah.</p>
 *
 * <h2>Pola arsitektur khas AIS yang muncul di kelas ini</h2>
 * <p>Field {@code oleh}, {@code olehId}, {@code tanggal_dirubah} beserta method
 * {@link #onUpdate()} adalah <b>field audit bayangan</b> yang menduplikasi field privat bernama
 * sama di {@link GeneralValueObject} — KEHARUSAN TEKNIS (induk bukan {@code @Entity} sehingga
 * tidak bisa mewariskan pemetaan kolom JPA), bukan salin-tempel ceroboh.</p>
 *
 * @see SatuanKerja#getSatuanLokasi()
 * @see Kppn#getSatuanLokasi()
 * @see ais.action.master.rab.helper.AmbilDataSatuanLokasiBanbox
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "rab", name = "satuan_lokasi")



public class SatuanLokasi extends GeneralValueObject {

	/**
	 * Versi serialisasi kelas. Nilainya sama dengan {@link MetodePengadaan}/{@link Satuan}
	 * (peninggalan hasil salin-tempel generator hbm2java); tidak masalah selama tidak ada dua
	 * kelas berbeda yang benar-benar diserialkan/dideserialkan saling tertukar sebagai satu sama
	 * lain.
	 */
	private static final long serialVersionUID = -8738027816264807168L;
	/** Primary key baris {@code rab.satuan_lokasi}. Lihat {@link #getId()}. */
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
	 * Representasi teks kelas ini, dipakai antara lain oleh renderer combo pencarian
	 * {@code AmbilDataSatuanLokasiBanbox}. <b>Meng-override</b> {@link GeneralValueObject#toString()}
	 * dengan menambahkan {@code id} mentah di depan format "kode - nama" standar
	 * ({@code "id-kode - nama"}), berbeda dari kebanyakan entity lain di paket ini yang memakai
	 * format induk apa adanya. Karena {@code id} dan {@code getNama()} bisa bernilai {@code null},
	 * hasilnya bisa memuat literal teks {@code "null"}.
	 *
	 * @return teks gabungan {@code id}, {@link #getKode()}, dan {@link #getNama()}
	 */
	public String toString() {
		return id + "-" + getKode() + " - " + getNama();
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

	/** Kode ringkas lokasi (mis. kode wilayah). Lihat {@link #getKode()}. */
	private String kode;
	/** Nama lokasi. Lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas untuk lokasi ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/**
	 * Constructor default tanpa argumen, WAJIB ada agar Hibernate dapat menginstansiasi entity
	 * lewat refleksi saat memuat baris dari database, dan agar layar CRUD dapat membuat object
	 * kosong untuk form tambah-baru.
	 */
	public SatuanLokasi() {
	}

	/**
	 * Constructor pintas untuk langsung menyetel nama lokasi, dipakai jalur pembuatan cepat
	 * (mis. seed data awal).
	 *
	 * @param nama nama lokasi
	 */
	public SatuanLokasi(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan primary key baris {@code rab.satuan_lokasi}.
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
	 * Mengembalikan nama lokasi, sudah di-{@code trim()} bila tidak {@code null}. Kolom wajib
	 * diisi ({@code nullable = false}) pada tabel.
	 *
	 * @return nama lokasi yang sudah dipangkas spasi tepi, atau {@code null} bila field mentahnya
	 *         {@code null}
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama lokasi. Tanpa validasi maupun pemangkasan spasi di sisi setter — pemangkasan
	 * hanya terjadi saat dibaca lewat {@link #getNama()}.
	 *
	 * @param nama nama lokasi baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas lokasi ini.
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

	/**
	 * Mengembalikan kode ringkas lokasi ini. Berbeda dari {@link #getNama()}, getter ini
	 * <b>tidak</b> dipetakan lewat anotasi {@code @Column} eksplisit — pemetaan kolomnya
	 * mengandalkan konvensi nama properti Hibernate default.
	 *
	 * @return kode lokasi, atau {@code null} bila belum diisi
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Menyetel kode lokasi. Tanpa validasi.
	 *
	 * @param kode kode lokasi baru
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

}
