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
 * Tabel referensi berkode untuk <b>frekuensi peninjauan kurikulum</b> pada pelaporan EPSBED
 * ("Evaluasi Program Studi Berbasis Evaluasi Diri", format pelaporan Dikti/Kemdikbud yang lebih
 * tua dari feeder PDDIKTI) — mis. seberapa sering kurikulum program studi ditinjau ulang
 * (setiap tahun, beberapa tahun sekali, dsb). Setiap baris hanya berupa pasangan {@code kode}
 * (maks. 2 karakter, sesuai kamus data resmi EPSBED) dan {@code nama} (label tampilan); isi
 * baris (nilai kode aktual) berasal dari data yang di-seed ke database, bukan dari kode Java —
 * tidak ditemukan seed literal untuk tabel ini di {@code InitDataHelper}/{@code InitData}, jadi
 * daftar kode aktif harus dicek langsung di tabel {@code epsbed.epsbed_frekuensi_kurikulum}.
 *
 * <p><b>Pemakaian aktif</b> — kelas ini BUKAN entity yatim: dirujuk sebagai {@code ManyToOne}
 * oleh {@link ais.database.model.JenjangProgramStudi#getEpsbedFrekuensiKurikulum()} (kolom
 * {@code epsbed_frekuensi_kurikulum} pada tabel {@code jenjang_program_studi}) dan diisi lewat
 * combobox di {@code ais.action.master.JenjangProgramStudiAction} serta ditampilkan di layar
 * ekspor {@code ais.action.master.epsbed.MasterProgramStudi}. Ada sepasang kolom untuk konsep
 * yang sama pada {@code JenjangProgramStudi}: {@code frekuensiKurikulum} adalah teks bebas
 * (kolom {@code frekuensi_kurikulum}), sedangkan referensi berkode ke kelas ini
 * ({@code epsbedFrekuensiKurikulum}) diisi terpisah pada layar yang sama dan <b>tidak
 * disinkronkan otomatis</b> satu sama lain — keduanya independen dari sumbu
 * {@link EpsbedPelaksanaanKurikulum} (status pelaksanaan kurikulum), yang juga merupakan pasangan
 * teks-bebas/berkode terpisah pada entity yang sama.</p>
 *
 * <p>Field serta method di kelas ini murni pemetaan Hibernate untuk baris kode/nama tersebut;
 * lihat {@link GeneralValueObject} untuk kontrak umum entity (identitas, audit, dsb.) yang
 * diwarisi seluruh model AIS.</p>
 *
 * @see EpsbedPelaksanaanKurikulum
 * @see ais.database.model.JenjangProgramStudi
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "epsbed", name = "epsbed_frekuensi_kurikulum")



public class EpsbedFrekuensiKurikulum extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap kelas ini; diwariskan apa adanya dari template hbm2java sehingga
	 * sama dengan kelas-kelas lookup EPSBED lain yang dibangkitkan pada saat bersamaan.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris kode (identity, lihat {@link #getId()}). */
	private Long id;
	/**
	 * Nama pengguna (username) yang terakhir mengubah baris ini; field audit "siapa" yang
	 * dipasangkan dengan {@link #olehId} (audit "id"). Lihat {@link #getOleh()}.
	 */
	private String oleh;
	/**
	 * Id pengguna (bukan username) yang terakhir mengubah baris ini — pasangan shadow dari
	 * {@link #oleh} untuk keperluan audit yang tahan terhadap perubahan nama pengguna.
	 */
	private String olehId;
	/**
	 * Mengembalikan id pengguna (audit) yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {return olehId;}
	/**
	 * Menyetel id pengguna (audit) yang mengubah baris ini. Nilai {@code null} atau string kosong
	 * (setelah di-{@code trim()}) diabaikan secara diam-diam — baris ini TIDAK melempar
	 * exception dan TIDAK mengosongkan nilai lama, sesuai pola audit shadow-field yang berulang
	 * di seluruh model AIS (kesengajaan teknis, bukan bug).
	 *
	 * @param olehId id pengguna baru; diabaikan bila kosong/{@code null}
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}

	/**
	 * Menyetel nama pengguna (audit) yang mengubah baris ini. Sama seperti {@link
	 * #setOlehId(String)}, nilai kosong/{@code null} diabaikan diam-diam tanpa mengubah state.
	 *
	 * @param oleh nama pengguna baru; diabaikan bila kosong/{@code null}
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna (audit) yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: memperbarui {@link #tanggal_dirubah} lewat {@link
	 * ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} setiap kali baris ini
	 * di-{@code UPDATE}. Dipanggil otomatis oleh provider persistence, bukan oleh kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}
	/**
	 * Timestamp perubahan terakhir; default ke saat objek dibuat ({@link
	 * ais.ui.util.WaktuUtil#getDate()}) dan diperbarui lagi oleh {@link #onUpdate()} pada setiap
	 * {@code UPDATE}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel timestamp perubahan terakhir secara manual (mis. saat impor/migrasi data).
	 *
	 * @param tanggal_dirubah timestamp baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan timestamp perubahan terakhir baris ini.
	 *
	 * @return timestamp perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks entity ini untuk tampilan UI (mis. label pada combobox/listbox) — sama
	 * dengan {@link #getNama()} apa adanya (tanpa null-check).
	 *
	 * @return {@link #nama}
	 */
	public String toString() {
		return nama;
	}

	/** Label tampilan kode ini, mis. untuk combobox pemilihan frekuensi kurikulum. */
	private String nama;
	/** Kode resmi EPSBED (maks. 2 karakter) untuk frekuensi peninjauan kurikulum ini. */
	private String kode;

	/** Konstruktor tanpa argumen yang disyaratkan Hibernate untuk instansiasi lewat refleksi. */
	public EpsbedFrekuensiKurikulum() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * @return id baris, atau {@code null} bila entity belum dipersist
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key baris ini. Kolom dipetakan {@code insertable = false} sehingga
	 * penyetelan manual tidak berpengaruh pada {@code INSERT} — nilai sesungguhnya dibangkitkan
	 * basis data (identity column).
	 *
	 * @param id id baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan label tampilan kode ini, di-{@code trim()} agar spasi tersisa dari input
	 * (impor data lama) tidak ikut tampil.
	 *
	 * @return {@link #nama} yang sudah di-{@code trim()}, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel label tampilan kode ini.
	 *
	 * @param nama label baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan kode resmi EPSBED (maks. 2 karakter) untuk frekuensi peninjauan kurikulum
	 * ini.
	 *
	 * @return {@link #kode}
	 */
	@Column(name = "kode", nullable = false, length = 2)
	public String getKode() {
		return this.kode;
	}

	/**
	 * Menyetel kode resmi EPSBED baris ini.
	 *
	 * @param kode kode baru (maks. 2 karakter sesuai kamus data EPSBED)
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

}
