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
 * Entity JPA/Hibernate untuk tabel katalog {@code surat.masa_berlaku_surat}: daftar master "masa
 * berlaku"/retensi surat masuk (mis. "1 Tahun", "5 Tahun", "Permanen"), lengkap dengan rentang
 * tanggal {@link #getMulai()}/{@link #getSampai()} opsional untuk kasus masa berlaku yang terikat
 * periode kalender tertentu (bukan sekadar durasi relatif).
 *
 * <p>
 * Class ini adalah lookup table sederhana bergaya hbm2java: tanpa relasi anak. Ia direferensikan
 * sebagai field {@code ManyToOne} dari {@code KlasifikasiSuratMasuk#getMasaBerlakuSurat()} (nilai
 * default retensi untuk surat masuk dengan klasifikasi tersebut), dikelola lewat layar master data
 * {@code MasaBerlakuSuratAction}, dan ditampilkan pada dasbor
 * {@code DasboardSuratMasukMasaBerlaku}. Data awal (seed) disiapkan oleh {@code InitData} saat
 * instalasi baru.
 * </p>
 *
 * <p>
 * Mewarisi field audit shadow {@code oleh}/{@code olehId}/{@code tanggal_dirubah} dari kerangka
 * entity AIS (lihat {@link GeneralValueObject}); field-field tersebut adalah kebutuhan teknis
 * (integrasi Envers/cache), bukan cacat desain.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "surat", name = "masa_berlaku_surat")
public class MasaBerlakuSurat extends GeneralValueObject {

	/**
	 * Nomor versi serialisasi, dibagi bersama entity AIS lain hasil template hbm2java yang sama;
	 * jangan diubah tanpa memeriksa dampaknya terhadap objek yang sudah terserialisasi.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengambil ID (username/NIP) shadow pencatat perubahan terakhir.
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
	 * Mengambil stempel waktu perubahan terakhir. Diinisialisasi ke waktu saat ini saat object
	 * dibuat, dan diperbarui otomatis oleh {@link #onUpdate()} setiap UPDATE lewat Hibernate.
	 *
	 * @return waktu perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks entity ini, berupa gabungan ID dan nama (mis. {@code "2-5 Tahun"}).
	 *
	 * @return string berformat {@code "<id>-<nama>"}.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	private String kode;
	private Date mulai;
	private Date sampai;
	private String nama;
	private String keterangan;
	private Boolean aktif;

	/** Constructor default (dibutuhkan Hibernate). Tidak menginisialisasi field apa pun. */
	public MasaBerlakuSurat() {
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
	 * Mengambil kode singkat masa berlaku ini.
	 *
	 * @return kode setelah di-trim, atau string kosong ({@code ""}) bila field belum diisi atau
	 *         kosong — getter ini TIDAK pernah mengembalikan {@code null}.
	 */
	public String getKode() {
		return kode == null || kode.isEmpty() ? "" : kode.trim();
	}

	/**
	 * Mengeset kode singkat masa berlaku.
	 *
	 * @param kode kode baru.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengambil nama masa berlaku (mis. "1 Tahun", "Permanen").
	 *
	 * @return nama setelah di-trim, atau {@code null} bila belum diisi.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengeset nama masa berlaku.
	 *
	 * @param nama nama baru (belum di-trim; trimming terjadi saat pembacaan via {@link #getNama()}).
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil keterangan/deskripsi tambahan untuk masa berlaku ini.
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
	 * @return {@code true} bila aktif; default {@code true} ketika field belum pernah diset.
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

	/**
	 * Mengambil tanggal awal berlakunya masa berlaku ini, untuk kasus di mana masa berlaku
	 * terikat rentang kalender tertentu (bukan sekadar durasi relatif seperti "1 Tahun").
	 *
	 * @return tanggal mulai, bisa {@code null} bila tidak dipakai/tidak relevan untuk baris ini.
	 */
	@Temporal(TemporalType.DATE)
	public Date getMulai() {
		return mulai;
	}

	/**
	 * Mengeset tanggal awal berlakunya masa berlaku ini.
	 *
	 * @param mulai tanggal mulai baru.
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Mengambil tanggal akhir berlakunya masa berlaku ini (pasangan dari {@link #getMulai()}).
	 *
	 * @return tanggal sampai, bisa {@code null} bila tidak dipakai/tidak relevan untuk baris ini.
	 */
	@Temporal(TemporalType.DATE)
	public Date getSampai() {
		return sampai;
	}

	/**
	 * Mengeset tanggal akhir berlakunya masa berlaku ini.
	 *
	 * @param sampai tanggal sampai baru.
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

}
