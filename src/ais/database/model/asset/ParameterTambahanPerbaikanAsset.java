package ais.database.model.asset;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.ParameterTambahan;

/**
 * Baris PENGHUBUNG (mapping table entity) antara satu {@link KelompokParameterTambahanPerbaikanAsset}
 * dan satu {@link ais.database.model.ParameterTambahan} -- menentukan field dinamis apa saja yang
 * tampil untuk satu kelompok tertentu pada form {@link PerbaikanAsset}.
 *
 * <h3>Nama tabel basis data menyesatkan -- diverifikasi dari kode, BUKAN bug fungsional</h3>
 *
 * <p>Anotasi {@code @Table} menunjuk ke {@code asset.parameter_tambahan_catatan_siswa} -- nama
 * yang berasal dari domain "catatan siswa" (sekolah), BUKAN dari domain perbaikan aset. Ini
 * kemungkinan sisa penyalinan struktur tabel dari entitas sejenis di modul sekolah/akademik saat
 * fitur perbaikan aset pertama dibuat (pola yang sama terlihat pada {@link JenisPerbaikanAsset}
 * yang kolom FK-nya bernama {@code jenis_catatan_administrasi}). Ini TIDAK menimbulkan bug
 * fungsional selama tabel tersebut memang hanya dipakai kelas ini (setiap query di kode
 * -- lihat {@link ais.action.master.helper.ParameterTambahanPerbaikanAssetListener} dan
 * {@code ParameterTambahanPerbaikanAssetAction} -- selalu melalui kelas Java ini, tidak pernah
 * lewat SQL mentah ke nama tabel), tetapi menyesatkan bagi siapa pun yang membaca skema basis
 * data langsung tanpa konteks kelas Java ini, dan berisiko tabrakan penamaan bila suatu saat
 * modul catatan siswa yang sesungguhnya butuh tabel dengan nama yang sama.</p>
 *
 * @see KelompokParameterTambahanPerbaikanAsset kelompok yang dipetakan
 * @see ais.database.model.ParameterTambahan definisi field dinamis (tipe, wajib isi, dsb.)
 * @see PerbaikanAsset form yang memakai konfigurasi ini
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "asset", name = "parameter_tambahan_catatan_siswa")
public class ParameterTambahanPerbaikanAsset extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java; sama dengan entitas sepaket lain karena berasal dari
	 * templat hbm2java yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama baris, kolom {@code id}; di-generate database (IDENTITY). */
	private Long id;

	/** Nama tampil pengguna terakhir yang menyunting baris ini. */
	private String oleh;

	/** Id pengguna terakhir yang menyunting baris ini. */
	private String olehId;

	/**
	 * Id pengguna terakhir yang menyunting baris ini.
	 *
	 * @return id pengguna penyunting terakhir, atau {@code null} bila belum terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna penyunting terakhir, MENGABAIKAN nilai kosong agar jejak audit lama
	 * tidak tertimpa oleh proses batch tanpa konteks pengguna aktif.
	 *
	 * @param olehId id pengguna penyunting; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna penyunting terakhir, MENGABAIKAN nilai kosong.
	 *
	 * @param oleh nama pengguna penyunting; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama tampil pengguna terakhir yang menyunting baris ini.
	 *
	 * @return nama penyunting terakhir, atau {@code null} bila belum terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: memperbarui stempel waktu audit sebelum UPDATE dikirim.
	 * Didelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} agar aturannya terpusat.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu penyuntingan terakhir, bernilai awal waktu server saat objek dibuat. Bidang
	 * audit ini diulang di tiap entitas AIS sebagai KEHARUSAN TEKNIS: kelas induk tidak
	 * mewariskan pemetaan kolom apa pun untuknya.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu penyuntingan terakhir.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu penyuntingan terakhir baris ini.
	 *
	 * @return stempel waktu; tidak pernah {@code null} untuk objek hasil konstruktor
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Kelompok yang dipetakan; lihat {@link #getKelompokParameterTambahanPerbaikanAsset()}. */
	private KelompokParameterTambahanPerbaikanAsset kelompokParameterTambahanPerbaikanAsset;

	/** Definisi field dinamis yang dipetakan; lihat {@link #getParameterTambahan()}. */
	private ParameterTambahan parameterTambahan;

	/** Nomor urut tampil; DITURUNKAN dari {@link ParameterTambahan#getNomorUrut()}, lihat {@link #getNomorUrut()}. */
	private Integer nomorUrut;

	/**
	 * Nomor urut tampil baris ini -- getter DESTRUKTIF yang MENURUNKAN nilainya dari {@link
	 * ParameterTambahan#getNomorUrut()} milik relasi {@link #getParameterTambahan()}, lalu
	 * menulis balik ke field {@code nomorUrut} in-memory. Nomor urut lokal pada baris penghubung
	 * ini karena itu SELALU disinkronkan mengikuti nomor urut definisi field dinamisnya sendiri
	 * -- field {@code nomorUrut} pada baris ini efektif tidak bisa independen dari nomor urut di
	 * {@link ParameterTambahan}, kecuali relasi {@code parameterTambahan} kosong (baru dibuat,
	 * belum disimpan) sehingga nilai lama/default dipertahankan.
	 *
	 * @return nomor urut mengikuti {@link ParameterTambahan#getNomorUrut()} bila relasi tersedia;
	 *         nilai field tersimpan bila relasi kosong; atau {@code 1} bila keduanya belum terisi
	 */
	public Integer getNomorUrut() {
		parameterTambahan = getParameterTambahan();
		if (parameterTambahan != null) {
			nomorUrut = parameterTambahan.getNomorUrut();
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Mengisi nomor urut secara manual; nilai ini bisa TERTIMPA oleh {@link #getNomorUrut()} pada
	 * pemanggilan berikutnya bila relasi {@link #getParameterTambahan()} tersedia -- lihat
	 * javadoc getter tersebut.
	 *
	 * @param nomorUrut nomor urut baru
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate dan ZK data binding.
	 */
	public ParameterTambahanPerbaikanAsset() {
	}

	/**
	 * Kunci utama baris.
	 *
	 * <p>{@code insertable = false} karena nilainya di-generate database.</p>
	 *
	 * @return id baris, atau {@code null} bila objek belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama; umumnya hanya dipanggil Hibernate seusai INSERT.
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Definisi field dinamis (tipe inputan, wajib isi, wajib lampiran, dsb.) yang dipetakan baris
	 * ini. Kolom FK bersifat {@code nullable = false} -- setiap baris penghubung wajib menunjuk
	 * ke satu definisi parameter. Dilewatkan lewat {@link #check(Object)} untuk memastikan proxy
	 * lazy sudah teresolusi.
	 *
	 * @return definisi field dinamis
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "parameter_tambahan", nullable = false)
	public ParameterTambahan getParameterTambahan() {
		parameterTambahan = check(parameterTambahan);
		return parameterTambahan;
	}

	/**
	 * Menetapkan definisi field dinamis yang dipetakan.
	 *
	 * @param parameterTambahan definisi baru
	 */
	public void setParameterTambahan(ParameterTambahan parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	/**
	 * Kelompok tempat definisi field dinamis di atas dikelompokkan pada tampilan form.
	 * Dilewatkan lewat {@link #check(Object)} untuk memastikan proxy lazy sudah teresolusi.
	 *
	 * @return kelompok terkait, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_parameter_tambahan_perbaikan_aset", nullable = true)
	public KelompokParameterTambahanPerbaikanAsset getKelompokParameterTambahanPerbaikanAsset() {
		kelompokParameterTambahanPerbaikanAsset = check(kelompokParameterTambahanPerbaikanAsset);
		return kelompokParameterTambahanPerbaikanAsset;
	}

	/**
	 * Menetapkan kelompok pemetaan.
	 *
	 * @param kelompokParameterTambahanPerbaikanAsset kelompok baru
	 */
	public void setKelompokParameterTambahanPerbaikanAsset(
			KelompokParameterTambahanPerbaikanAsset kelompokParameterTambahanPerbaikanAsset) {
		this.kelompokParameterTambahanPerbaikanAsset = kelompokParameterTambahanPerbaikanAsset;
	}

}
