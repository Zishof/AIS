package ais.database.model.epsbed;

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
 * Tabel referensi kode "status program studi" (PS = Program Studi) versi pelaporan EPSBED lama.
 * Secara desain sama persis dengan {@link EpsbedStatus} (pasangan {@code kode}/{@code nama} plus
 * jejak audit) — tabel terpisah karena format EPSBED membedakan status di level "jurusan"
 * ({@link EpsbedStatus}, dipakai {@link ais.database.model.JenjangProgramStudi}) dari status
 * di level "program studi" itu sendiri.
 *
 * <p><b>STATUS PEMAKAIAN — ENTITY YATIM (tidak dipakai sama sekali):</b> tidak ada satu pun
 * {@code Action}/{@code Helper}/entity lain di codebase ini yang mereferensikan
 * {@code EpsbedStatusPs} (tidak ada combobox, tidak ada relasi {@code @ManyToOne} dari entity lain,
 * tidak muncul di daftar {@code initClasses(...)} pada {@link ais.common.InitData}). Manifest CRUD
 * generik ({@code general_value_object_inventory.csv}) juga menandainya {@code REVIEW_REQUIRED} /
 * "modul integrasi eksternal" dan CRUD generik-nya default nonaktif. Class ini kemungkinan dibuat
 * hbm2java bersama tabel {@code epsbed.epsbed_status_ps} yang di-generate dari skema referensi
 * EPSBED asli tapi tidak pernah dipakai fitur pelaporan mana pun di AIS.</p>
 *
 * <p><b>Duplikat tanpa migrasi:</b> ada class lain dengan nama identik,
 * {@code ais.database.model.EpsbedStatusPs} (paket {@code ais.database.model}, BUKAN
 * {@code ais.database.model.epsbed}), yang memetakan tabel dengan nama sama tapi skema BERBEDA
 * ({@code @Table(schema = "public", name = "epsbed_status_ps")} — bukan skema {@code epsbed}).
 * Class duplikat itu juga tidak dipakai di mana pun. Ini kemungkinan sisa refactor pemindahan
 * model epsbed ke paket {@code ais.database.model.epsbed} yang lupa menghapus versi lama; JANGAN
 * dianggap sebagai sumber data yang saling menggantikan tanpa verifikasi skema mana yang benar-benar
 * berisi data di database produksi.</p>
 *
 * @see GeneralValueObject
 * @see EpsbedStatus
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "epsbed", name = "epsbed_status_ps")



public class EpsbedStatusPs extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	/** ID pengguna (nama login) yang terakhir mengubah baris ini; diisi lewat {@link #setOleh(String)}. */
	private String oleh;
	/** ID internal (bukan nama login) dari pengguna yang terakhir mengubah baris ini. */
	private String olehId;
	/** @return ID internal pengguna yang terakhir mengubah baris ini, atau {@code null} bila belum pernah diisi. */
	public String getOlehId() {return olehId;}
	/**
	 * Menyimpan ID internal pengguna yang mengubah baris ini. Nilai kosong/blank diabaikan secara
	 * senyap (fail-safe agar audit trail lama tidak tertimpa {@code null}/string kosong).
	 *
	 * @param olehId ID internal pengguna; {@code null} atau string kosong tidak melakukan apa-apa.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}

	/**
	 * Menyimpan nama login pengguna yang mengubah baris ini. Nilai kosong/blank diabaikan secara
	 * senyap, pola yang sama dengan {@link #setOlehId(String)}.
	 *
	 * @param oleh nama login pengguna; {@code null} atau string kosong tidak melakukan apa-apa.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama login pengguna yang terakhir mengubah baris ini, atau {@code null} bila belum pernah diisi. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook Hibernate yang otomatis memperbarui {@link #tanggal_dirubah} setiap kali baris di-{@code UPDATE}.
	 * Dipanggil oleh provider persistence sendiri, bukan untuk dipanggil manual dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyimpan cap waktu perubahan terakhir baris ini secara manual. Nilai default sudah diisi
	 * saat objek dibuat ({@code ais.ui.util.WaktuUtil.getDate()}); {@link #onUpdate()} menimpanya
	 * lagi otomatis tiap {@code UPDATE}.
	 *
	 * @param tanggal_dirubah cap waktu perubahan.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return cap waktu perubahan terakhir baris ini. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return {@link #nama} — representasi teks default entity ini (mis. untuk tampilan combobox). */
	public String toString() {
		return nama;
	}

	/** Label yang ditampilkan ke pengguna untuk kode status program studi ini (kolom {@code nama}). */
	private String nama;
	/** Kode status program studi resmi EPSBED (kolom {@code kode}); lihat catatan kelas soal status yatimnya. */
	private String kode;

	/** Konstruktor default yang dibutuhkan Hibernate; field diisi lewat setter. */
	public EpsbedStatusPs() {
	}

	/** @return id baris ini (primary key auto-increment, bukan bagian dari kode resmi EPSBED). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id id baris ini; diabaikan saat insert karena kolom {@code id} bersifat {@code insertable = false}. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return label status ini yang sudah di-{@code trim()}, atau {@code null} bila {@link #nama} belum diisi. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama label status yang ditampilkan ke pengguna. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return kode status program studi resmi EPSBED baris ini. */
	@Column(name = "kode", nullable = false, length = 255)
	public String getKode() {
		return this.kode;
	}

	/** @param kode kode status program studi resmi EPSBED baris ini. */
	public void setKode(String kode) {
		this.kode = kode;
	}

}
