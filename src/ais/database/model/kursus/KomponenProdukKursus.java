package ais.database.model.kursus;

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
 * Master <b>jenis komponen pembelajaran</b> yang dapat menyusun sebuah {@link ProdukKursus}: satu
 * baris menyatakan satu jenis komponen beserta harga bawaannya (mis. "Video", "Buku", "Ujian").
 * Kelas ini murni daftar referensi/master data — berbeda dari
 * {@link ais.database.model.kursus.KomponenDataProdukKursus} yang merinci INSTANCE konkret
 * komponen di dalam satu produk kursus tertentu.
 *
 * <h3>Konstanta jenis dipakai sebagai nilai String, bukan relasi</h3>
 * <p>Kedelapan konstanta {@link #VIDEO}..{@link #EKSTRA_KURIKULER} dan array {@link #s} adalah
 * daftar nilai baku yang, di {@link ais.database.model.kursus.KomponenDataProdukKursus}, disalin
 * sebagai field {@code String} biasa ({@code komponenProdukKursus}) — bukan lewat relasi
 * {@code ManyToOne} ke kelas master ini. Konsekuensinya baris tabel {@code komponen_produk_kursus}
 * ini sendiri lebih berperan sebagai daftar harga bawaan per jenis (dipakai UI untuk
 * combobox/pilihan) daripada sebagai sumber integritas referensial: tidak ada penjaga yang
 * mencegah {@code KomponenDataProdukKursus.komponenProdukKursus} menyimpan nilai yang tidak
 * terdaftar di {@link #s} maupun di tabel ini.</p>
 *
 * @see ais.database.model.kursus.KomponenDataProdukKursus pemakai nilai jenis komponen ini
 * @see ProdukKursus produk kursus yang komponennya disusun dari jenis-jenis ini
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "komponen_produk_kursus")
public class KomponenProdukKursus extends GeneralValueObject {

	/** Jenis komponen: video pembelajaran. */
	public static final String VIDEO = "Video";
	/** Jenis komponen: buku fisik/cetak. */
	public static final String BUKU = "Buku";
	/** Jenis komponen: buku elektronik. */
	public static final String EBOOK = "Ebook";
	/** Jenis komponen: latihan soal. */
	public static final String LATIHAN_SOAL = "Latihan Soal";
	/** Jenis komponen: ujian. */
	public static final String UJIAN = "Ujian";
	/**
	 * Jenis komponen: pembelajaran tatap muka. Dirujuk langsung (perbandingan string) oleh
	 * {@code KomponenDataProdukKursus.getStatusPertemuan()} untuk memaksa status pertemuan menjadi
	 * {@code ConstantValues.TATAP_MUKA}.
	 */
	public static final String PEMBELAJARAN_TATAP_MUKA = "Pembelajaran Tatap Muka";
	/**
	 * Jenis komponen: pembelajaran jarak jauh/daring. Dirujuk langsung (perbandingan string) oleh
	 * {@code KomponenDataProdukKursus.getStatusPertemuan()} untuk memaksa status pertemuan menjadi
	 * {@code ConstantValues.DARING}.
	 */
	public static final String PEMBELAJARAN_JARAK_JAUH = "Pembelajaran Jarak Jauh";
	/** Jenis komponen: kegiatan ekstra kurikuler. */
	public static final String EKSTRA_KURIKULER = "Ekstra Kurikuler";

	/**
	 * Daftar seluruh nilai jenis komponen baku, dalam urutan deklarasi konstanta di atas. Dipakai
	 * pemanggil untuk mengisi pilihan combobox/validasi tanpa perlu query ke tabel ini.
	 */
	public static final String[] s = new String[] { KomponenProdukKursus.VIDEO, KomponenProdukKursus.BUKU,
			KomponenProdukKursus.EBOOK, KomponenProdukKursus.LATIHAN_SOAL, KomponenProdukKursus.UJIAN,
			KomponenProdukKursus.PEMBELAJARAN_TATAP_MUKA, KomponenProdukKursus.PEMBELAJARAN_JARAK_JAUH,
			KomponenProdukKursus.EKSTRA_KURIKULER };

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris {@code komponen_produk_kursus}, dibangkitkan basis data (IDENTITY). */
	private Long id;
	/** Nama pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String oleh;
	/** Id pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris master ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah terakhir. Nilai {@code null}/kosong/spasi diabaikan diam-diam
	 * agar jejak audit yang sudah terisi tidak terhapus oleh jalur simpan tanpa identitas pengguna.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir, dengan validasi non-trivial yang sama seperti
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris master ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: mendelegasikan pencatatan stempel audit ke
	 * {@code AuditTimestampInterceptor.ubah(this)} setiap kali baris ini diperbarui.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir. Tanpa validasi.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir, dipetakan sebagai {@code TIMESTAMP}.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas jenis komponen: {@code "id-nama"}.
	 *
	 * @return gabungan id dan nama jenis komponen
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode ringkas jenis komponen. */
	private String kode;

	/** Nama jenis komponen (kolom wajib, maksimal 255 karakter; biasanya salah satu nilai {@link #s}). */
	private String nama;
	/** Keterangan bebas jenis komponen. */
	private String keterangan;
	/** Harga bawaan untuk jenis komponen ini. */
	private Double harga;
	/** Status aktif/nonaktif jenis komponen; {@code null} dianggap aktif oleh {@link #getAktif()}. */
	private Boolean aktif;

	/** Konstruktor kosong yang diwajibkan JPA/Hibernate. */
	public KomponenProdukKursus() {
	}

	/**
	 * Mengembalikan primary key jenis komponen.
	 *
	 * @return primary key, atau {@code null} bila belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Tanpa validasi; normalnya diisi otomatis oleh Hibernate.
	 *
	 * @param id primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode ringkas jenis komponen, menormalkan {@code null} menjadi string kosong dan
	 * memangkas spasi tepi.
	 *
	 * @return kode jenis komponen, tidak pernah {@code null}
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Mengisi kode ringkas jenis komponen.
	 *
	 * @param kode kode jenis komponen baru
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama jenis komponen, dipangkas spasi tepi.
	 *
	 * @return nama jenis komponen (dipangkas), atau {@code null} bila belum pernah diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama jenis komponen.
	 *
	 * @param nama nama jenis komponen baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas jenis komponen. Getter murni-baca, tanpa normalisasi.
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas jenis komponen.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif/nonaktif jenis komponen, menormalkan {@code null} menjadi
	 * {@code true}.
	 *
	 * @return {@code true} bila jenis komponen aktif, tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyalakan atau mematikan jenis komponen.
	 *
	 * @param aktif {@code true} bila jenis komponen aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan harga bawaan jenis komponen ini, menormalkan {@code null} menjadi {@code 0.0}.
	 *
	 * @return harga bawaan, tidak pernah {@code null}
	 */
	public Double getHarga() {
		return harga == null ? 0.0 : harga;
	}

	/**
	 * Mengisi harga bawaan jenis komponen ini.
	 *
	 * @param harga harga bawaan baru
	 */
	public void setHarga(Double harga) {
		this.harga = harga;
	}

}
