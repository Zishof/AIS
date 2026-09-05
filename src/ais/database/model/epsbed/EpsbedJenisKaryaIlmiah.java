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
 * Tabel referensi kode "jenis karya ilmiah" versi pelaporan EPSBED lama (mis. penggolongan karya
 * penelitian/publikasi dosen seperti buku, jurnal, atau prosiding — kolom {@code kode} dibatasi
 * 2 karakter, pola singkat khas kode numerik dua digit seperti {@code 01}/{@code 02}/dst pada
 * "buku kode EPSBED" Dikti, bukan nama panjang).
 *
 * <p><b>Pemakaian:</b> dirujuk sebagai {@code kodeJenisPenelitian} oleh dua entity publikasi dosen
 * di paket yang sama: {@link EpsbedPublikasiDosen#getKodeJenisPenelitian()} dan
 * {@link EpsbedPublikasiIlmiahDosen#getKodeJenisPenelitian()}. Diisi lewat combobox
 * ({@code Common.insertCombo(..., EpsbedJenisKaryaIlmiah.class)}) di
 * {@link ais.action.master.epsbed.PublikasiDosenHelper} dan
 * {@link ais.action.master.epsbed.PublikasiIlmiahDosenHelper} — jadi modul ini AKTIF dipakai untuk
 * mencatat jenis karya ilmiah pada layar input publikasi dosen, bukan sekadar tabel warisan. Catatan
 * relasi: berbeda dari klaster tabel penelitian/pengabdian yang lebih baru
 * ({@code ais.database.model.penelitiandanpengabdian.Artikel}/{@code JurnalPenelitian}) — tidak ada
 * referensi silang langsung antara kelas tersebut dengan {@code EpsbedJenisKaryaIlmiah}; keduanya
 * tampak sebagai jalur pencatatan karya ilmiah yang terpisah (EPSBED lama vs. modul penelitian
 * modern), bukan satu hierarki yang saling menggantikan.</p>
 *
 * <p><b>Isi kode:</b> baris konkretnya di-seed langsung ke tabel
 * {@code epsbed.epsbed_jenis_karya_ilmiah} di database, bukan lewat skrip seed di source tree ini —
 * tidak ditemukan {@code InitDataHelper} atau SQL seed untuk tabel ini di paket ini, sehingga daftar
 * kode resminya (mis. kode mana untuk buku/jurnal/prosiding) harus dicek langsung ke DB atau dokumen
 * "buku kode EPSBED" Dikti.</p>
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
@Table(schema = "epsbed", name = "epsbed_jenis_karya_ilmiah")



public class EpsbedJenisKaryaIlmiah extends GeneralValueObject {

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

	/** Label yang ditampilkan ke pengguna untuk jenis karya ilmiah ini (kolom {@code nama}). */
	private String nama;
	/** Kode jenis karya ilmiah resmi EPSBED, maks. 2 karakter (kolom {@code kode}). */
	private String kode;

	/** Konstruktor default yang dibutuhkan Hibernate; field diisi lewat setter. */
	public EpsbedJenisKaryaIlmiah() {
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

	/** @return label jenis karya ilmiah ini yang sudah di-{@code trim()}, atau {@code null} bila {@link #nama} belum diisi. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama label jenis karya ilmiah yang ditampilkan ke pengguna (dan pada combobox pemilihan). */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return kode jenis karya ilmiah resmi EPSBED baris ini (maks. 2 karakter; dirujuk sebagai
	 * {@code kodeJenisPenelitian} oleh {@link EpsbedPublikasiDosen} dan
	 * {@link EpsbedPublikasiIlmiahDosen}).
	 */
	@Column(name = "kode", nullable = false, length = 2)
	public String getKode() {
		return this.kode;
	}

	/** @param kode kode jenis karya ilmiah resmi EPSBED baris ini (maks. 2 karakter). */
	public void setKode(String kode) {
		this.kode = kode;
	}

}
