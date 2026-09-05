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
 * Entity JPA/Hibernate untuk tabel katalog {@code surat.penyampaian_surat}: dimaksudkan sebagai
 * master data "cara penyampaian surat" (mis. langsung, pos, kurir, email) — namun class ini
 * BUKAN sebuah log/catatan histori pengiriman fisik surat, melainkan lookup table generik berisi
 * kode/nama/keterangan saja, sama seperti {@link SifatSurat}/{@link StatusDipertahankan}.
 *
 * <p>
 * <b>Status: entity yatim/tidur (dormant).</b> Berbeda dari katalog surat lain di paket ini yang
 * masing-masing punya layar master data ({@code XxxAction}) dan sekurangnya satu field
 * {@code ManyToOne} penunjuk dari {@code SuratMasuk}/{@code SuratKeluar}, hasil penelusuran
 * seluruh source tree ({@code ais/action/**}, {@code ais/database/model/surat/**}) untuk
 * {@code PenyampaianSurat} hanya menemukan: mapping Hibernate di {@code hibernate.cfg.xml}, serta
 * lapisan DAO generik hasil generate ({@link ais.database.dao.surat.PenyampaianSuratDao},
 * {@link ais.database.dao.surat.PenyampaianSuratDaoImpl}, dan entri factory di
 * {@code HibernateDaoFactory}/{@code DaoFactory}) — tidak ada Action/controller, tidak ada layar
 * ZK, dan tidak ada field {@code ManyToOne} di {@code SuratMasuk} maupun {@code SuratKeluar} yang
 * menunjuk ke entity ini. Tabel {@code surat.penyampaian_surat} kemungkinan dibuat sebagai
 * scaffolding awal (mengikuti pola-pola katalog lain di modul ini) namun tidak pernah selesai
 * dikaitkan ke alur bisnis surat masuk/keluar. Karena tidak ada kode yang memuat baris entity ini
 * dengan filter kepemilikan apa pun (baik benar maupun salah), TIDAK ditemukan pola kebocoran
 * cross-user seperti pada {@code LogLogin}/{@code UploadLog} — tidak ada pemuatan data sama
 * sekali di luar lapisan DAO generik yang belum dipakai.
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
@Table(schema = "surat", name = "penyampaian_surat")
public class PenyampaianSurat extends GeneralValueObject {

	/**
	 * Nomor versi serialisasi, dibagi bersama entity AIS lain hasil template hbm2java yang sama;
	 * jangan diubah tanpa memeriksa dampaknya terhadap objek yang sudah terserialisasi.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	/** Nama shadow pencatat perubahan terakhir (audit); lihat {@link #getOleh()}. */
	private String oleh;private String olehId;
	/**
	 * Mengambil ID (username/NIP) shadow pencatat perubahan terakhir.
	 *
	 * @return ID pencatat perubahan terakhir, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {return olehId;}
	/**
	 * Mengeset ID shadow pencatat perubahan. Nilai kosong/blank sengaja diabaikan (silent no-op)
	 * agar nilai lama yang sudah tercatat tidak tertimpa oleh input kosong.
	 *
	 * @param olehId ID pencatat perubahan; diabaikan bila {@code null} atau hanya berisi spasi.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}

	/**
	 * Representasi teks singkat entity ini, dipakai antara lain oleh komponen ZK agar baris
	 * tampil sebagai nama cara penyampaian itu sendiri.
	 *
	 * @return nilai field {@code nama} apa adanya (tanpa trim).
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
	 * Mengambil stempel waktu perubahan terakhir. Diinisialisasi ke waktu saat ini saat object
	 * dibuat, dan diperbarui otomatis oleh {@link #onUpdate()} setiap UPDATE lewat Hibernate.
	 *
	 * @return waktu perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	private String nama;
	private String keterangan;

	/** Constructor default (dibutuhkan Hibernate). Tidak menginisialisasi field apa pun. */
	public PenyampaianSurat() {
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
	 * Mengambil nama cara penyampaian surat ini (mis. "Langsung", "Pos", "Kurir", "Email").
	 *
	 * @return nama setelah di-trim, atau {@code null} bila belum diisi.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengeset nama cara penyampaian surat.
	 *
	 * @param nama nama baru (belum di-trim; trimming terjadi saat pembacaan via {@link #getNama()}).
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil keterangan/deskripsi tambahan untuk cara penyampaian ini.
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

}
