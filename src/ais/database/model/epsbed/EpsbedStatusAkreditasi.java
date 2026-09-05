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
 * Tabel referensi kode "status akreditasi" versi pelaporan EPSBED lama. Satu baris = satu nilai
 * kode akreditasi resmi Dikti (kolom {@code kode}, dibatasi 2 karakter — pola singkat khas kode
 * peringkat akreditasi seperti {@code A}/{@code B}/{@code C}/{@code TT}, bukan nama panjang)
 * beserta label tampilannya (kolom {@code nama}).
 *
 * <p><b>Pemakaian:</b> dirujuk oleh
 * {@link ais.database.model.JenjangProgramStudi#getEpsbedStatusAkreditasi()} sebagai status
 * akreditasi EPSBED program studi/jurusan (kolom {@code epsbed_status_akreditasi} pada
 * {@code JenjangProgramStudi}), diisi lewat combobox di
 * {@link ais.action.master.JenjangProgramStudiAction} dan ditampilkan/diekspor oleh
 * {@link ais.action.master.epsbed.MasterProgramStudi}. Pada impor data lama dari format EPSBED asli
 * (lihat {@code ais/action/master/helper/impor/jenjang_program_studi.sql}), baris tabel ini
 * dicocokkan terhadap kolom {@code KDSTAMSPST} dari tabel sumber {@code importepsbed."MSPST"} —
 * jadi nilai {@code kode} di sini HARUS sama persis dengan kode status akreditasi resmi Dikti,
 * bukan kode bebas buatan AIS. Catatan: ini TIDAK sama dengan sistem akreditasi BAN-PT/LAM modern
 * (yang memakai peringkat "Unggul"/"Baik Sekali"/"Baik") — tabel ini mengikuti skema kode EPSBED
 * lama yang lebih sederhana.</p>
 *
 * <p><b>Isi kode:</b> baris konkretnya di-seed langsung ke tabel
 * {@code epsbed.epsbed_status_akreditasi} di database, bukan lewat skrip seed di source tree ini —
 * tidak ditemukan {@code InitDataHelper} atau SQL seed untuk tabel ini di paket ini, sehingga daftar
 * kode resminya harus dicek langsung ke DB atau dokumen "buku kode EPSBED" Dikti.</p>
 *
 * <p>Field {@code oleh}/{@code olehId}/{@code tanggal_dirubah} adalah jejak audit standar
 * ({@link org.hibernate.envers.Audited} di level kelas menambah audit history penuh di atasnya) —
 * bagian dari kontrak {@link GeneralValueObject}, bukan bug meski tampak redundan dengan Envers.</p>
 *
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "epsbed", name = "epsbed_status_akreditasi")



public class EpsbedStatusAkreditasi extends GeneralValueObject {

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

	/** Label yang ditampilkan ke pengguna untuk kode status akreditasi ini (kolom {@code nama}). */
	private String nama;
	/**
	 * Kode status akreditasi resmi EPSBED, maks. 2 karakter, sesuai kolom {@code KDSTAMSPST} pada
	 * format impor Dikti (kolom {@code kode}).
	 */
	private String kode;

	/** Konstruktor default yang dibutuhkan Hibernate; field diisi lewat setter. */
	public EpsbedStatusAkreditasi() {
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

	/** @return label status akreditasi ini yang sudah di-{@code trim()}, atau {@code null} bila {@link #nama} belum diisi. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama label status akreditasi yang ditampilkan ke pengguna. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return kode status akreditasi resmi EPSBED baris ini (maks. 2 karakter; harus sama dengan
	 * kode pada dokumen referensi Dikti, bukan kode bebas buatan AIS).
	 */
	@Column(name = "kode", nullable = false, length = 2)
	public String getKode() {
		return this.kode;
	}

	/** @param kode kode status akreditasi resmi EPSBED baris ini (maks. 2 karakter). */
	public void setKode(String kode) {
		this.kode = kode;
	}

}
