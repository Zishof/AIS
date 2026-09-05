package ais.database.model.surat;

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
 * Entity JPA/Hibernate untuk tabel katalog {@code surat.sifat_surat}: daftar master "sifat surat"
 * (mis. Biasa/Penting/Segera/Rahasia) yang dipakai untuk mengklasifikasikan tingkat kepentingan
 * atau kerahasiaan sebuah surat masuk.
 *
 * <p>
 * Class ini adalah lookup table sederhana bergaya hbm2java: hanya berisi kode, nama, keterangan,
 * dan flag aktif — tanpa relasi anak. Ia direferensikan sebagai field {@code ManyToOne} langsung
 * dari {@link ais.database.model.surat.SuratMasuk#getSifatSurat()} (nilai default juga bisa
 * diwarisi dari {@code KlasifikasiSuratMasuk} induknya bila diset di sana), serta dikelola lewat
 * layar master data {@code SifatSuratAction} dan ditampilkan pada dasbor
 * {@code DasboardSuratMasukSifatSurat}. Data awal (seed) untuk tabel ini disiapkan oleh
 * {@code InitData}/{@code InitDataHelper} saat instalasi baru.
 * </p>
 *
 * <p>
 * Mewarisi field audit shadow {@code oleh}/{@code olehId}/{@code tanggal_dirubah} dan pola
 * getter-defensif dari kerangka entity AIS (lihat {@link GeneralValueObject}); field-field
 * tersebut adalah kebutuhan teknis (integrasi Envers/cache), bukan cacat desain.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "surat", name = "sifat_surat")
public class SifatSurat extends GeneralValueObject {

	/**
	 * Nomor versi serialisasi. Nilai ini dibagi bersama banyak entity AIS lain yang di-generate
	 * dari template hbm2java yang sama; jangan diubah tanpa memeriksa dampaknya terhadap objek
	 * yang sudah terserialisasi (mis. di cache).
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengambil ID (username/NIP) shadow pencatat perubahan terakhir. Field audit ini diisi oleh
	 * {@code AuditTimestampInterceptor}/lapisan pemanggil, bukan oleh pengguna langsung.
	 *
	 * @return ID pencatat perubahan terakhir, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengeset ID shadow pencatat perubahan. Nilai kosong/blank sengaja diabaikan (silent no-op)
	 * agar nilai lama yang sudah tercatat tidak tertimpa oleh input kosong.
	 *
	 * @param olehId ID pencatat perubahan; diabaikan bila {@code null} atau hanya berisi spasi.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks singkat entity ini, dipakai antara lain oleh komponen combobox/listbox ZK
	 * agar baris tampil sebagai nama sifat surat itu sendiri.
	 *
	 * @return nilai {@link #getNama()} (belum melalui trim di sini karena mengakses field
	 *         {@code nama} langsung).
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Mengeset nama shadow pencatat perubahan (audit). Nilai kosong/blank sengaja diabaikan.
	 *
	 * @param oleh nama pencatat perubahan; diabaikan bila {@code null} atau hanya berisi spasi.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama shadow pencatat perubahan terakhir.
	 *
	 * @return nama pencatat perubahan terakhir, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/** Callback JPA {@code @PreUpdate}: memperbarui stempel waktu audit sebelum baris di-UPDATE. */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengeset stempel waktu perubahan terakhir secara manual.
	 *
	 * @param tanggal_dirubah waktu perubahan baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil stempel waktu perubahan terakhir. Field ini diinisialisasi ke waktu saat ini pada
	 * saat object dibuat, dan diperbarui otomatis oleh {@link #onUpdate()} setiap kali baris
	 * di-UPDATE lewat Hibernate.
	 *
	 * @return waktu perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	private String kode;
	private String nama;
	private String keterangan;
	private Boolean aktif;

	/** Constructor default (dibutuhkan Hibernate). Tidak menginisialisasi field apa pun. */
	public SifatSurat() {
	}

	/**
	 * Mengambil primary key baris ini.
	 *
	 * @return ID baris, atau {@code null} untuk entity yang belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengeset primary key. Normalnya tidak dipanggil manual karena kolom {@code id} bersifat
	 * {@code insertable = false} (auto-generated oleh database via strategi IDENTITY).
	 *
	 * @param id ID baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil nama sifat surat (mis. "Biasa", "Penting", "Segera", "Rahasia").
	 *
	 * @return nama sifat surat setelah di-trim, atau {@code null} bila belum diisi.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengambil kode singkat sifat surat. Berbeda dari beberapa entity katalog sejenis di paket
	 * ini, getter ini TIDAK melakukan trim atau normalisasi terhadap nilai {@code null}/kosong —
	 * nilai field dikembalikan apa adanya.
	 *
	 * @return kode sifat surat, bisa {@code null}.
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Mengeset kode singkat sifat surat.
	 *
	 * @param kode kode baru.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengeset nama sifat surat.
	 *
	 * @param nama nama baru (belum di-trim; trimming terjadi saat pembacaan via {@link #getNama()}).
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil keterangan/deskripsi tambahan untuk sifat surat ini.
	 *
	 * @return keterangan, bisa {@code null}.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengeset keterangan/deskripsi tambahan.
	 *
	 * @param keterangan keterangan baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengambil status aktif/tidaknya baris ini sebagai opsi yang bisa dipilih pengguna.
	 *
	 * @return {@code true} bila aktif; default {@code true} ketika field belum pernah diset
	 *         (baris lama sebelum kolom {@code aktif} ditambahkan dianggap tetap aktif).
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengeset status aktif/tidaknya baris ini.
	 *
	 * @param aktif status aktif baru.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
