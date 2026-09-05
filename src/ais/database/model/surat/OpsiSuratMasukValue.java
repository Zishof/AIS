package ais.database.model.surat;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Entity JPA/Hibernate untuk tabel {@code surat.opsi_surat_masuk_value}: baris "nilai terpilih"
 * dari sebuah opsi/checkbox dinamis {@link OpsiSuratMasuk} untuk satu {@link SuratMasuk} tertentu.
 *
 * <p>
 * Ini adalah pola header-value yang umum dipakai di modul surat AIS untuk menyimpan
 * pilihan/checkbox dinamis yang jumlah dan isinya bisa berbeda per instalasi (mirip pola
 * "parameter tambahan" pada modul lain): {@link OpsiSuratMasuk} adalah definisi/header opsi (mis.
 * checkbox "Perlu Tindak Lanjut", dibatasi per jenis/username pengguna lewat
 * {@link OpsiSuratMasuk#getJenisPengguna()}/{@link OpsiSuratMasuk#getUsernamePengguna()}),
 * sementara baris {@code OpsiSuratMasukValue} ini merekam bahwa opsi tersebut ({@code
 * opsiSuratMasuk}) dicentang/dipilih untuk surat masuk tertentu ({@code suratMasuk}) — satu baris
 * per kombinasi surat+opsi yang aktif. Field {@code nama}/{@code keterangan} pada baris ini dipakai
 * sebagai label/isi bebas tambahan (lihat pemakaiannya di {@code SuratMasukAction},
 * {@code AlurPersetujuanSuratMasukStatusAction}, dan {@code helper.DasboardSurat}, yang membaca
 * baris-baris ini via {@code Restrictions.eq("suratMasuk", ...)} untuk merangkai narasi/atribut
 * per surat).
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
@Table(schema = "surat", name = "opsi_surat_masuk_value")
public class OpsiSuratMasukValue extends GeneralValueObject {

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
	 * tampil sebagai nama/label nilai opsi itu sendiri.
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


	/** Surat masuk yang menjadi induk baris nilai opsi ini. */
	private SuratMasuk suratMasuk;
	/** Definisi/header opsi dinamis yang nilainya direkam oleh baris ini. */
	private OpsiSuratMasuk opsiSuratMasuk;
	private String nama;
	private String keterangan;

	/** Constructor default (dibutuhkan Hibernate). Tidak menginisialisasi field apa pun. */
	public OpsiSuratMasukValue() {
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
	 * Mengambil nama/label nilai opsi ini.
	 *
	 * @return nama setelah di-trim, atau {@code null} bila belum diisi.
	 */
	@Column(name = "nama", nullable = true, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengeset nama/label nilai opsi.
	 *
	 * @param nama nama baru (belum di-trim; trimming terjadi saat pembacaan via {@link #getNama()}).
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil keterangan/isi tambahan untuk nilai opsi ini.
	 *
	 * @return keterangan, bisa {@code null}.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengeset keterangan/isi tambahan.
	 *
	 * @param keterangan keterangan baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengambil surat masuk induk dari baris nilai opsi ini.
	 *
	 * @return {@link SuratMasuk} terkait, bisa {@code null} bila belum diset.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "surat_masuk", nullable = true)
	public SuratMasuk getSuratMasuk() {
		return suratMasuk;
	}

	/**
	 * Mengeset surat masuk induk dari baris nilai opsi ini.
	 *
	 * @param suratMasuk surat masuk terkait.
	 */
	public void setSuratMasuk(SuratMasuk suratMasuk) {
		this.suratMasuk = suratMasuk;
	}

	/**
	 * Mengambil definisi/header opsi dinamis ({@link OpsiSuratMasuk}) yang nilainya direkam oleh
	 * baris ini.
	 *
	 * @return header opsi terkait, bisa {@code null} bila belum diset.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "opsi_surat_masuk", nullable = true)
	public OpsiSuratMasuk getOpsiSuratMasuk() {
		return opsiSuratMasuk;
	}

	/**
	 * Mengeset definisi/header opsi dinamis yang nilainya direkam oleh baris ini.
	 *
	 * @param opsiSuratMasuk header opsi terkait.
	 */
	public void setOpsiSuratMasuk(OpsiSuratMasuk opsiSuratMasuk) {
		this.opsiSuratMasuk = opsiSuratMasuk;
	}

}
