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
 * Entity JPA/Hibernate untuk tabel {@code surat.variable_surat_keluar}: definisi variabel global
 * (mail-merge) yang tersedia untuk disisipkan ke template cetak surat keluar — dikelola lewat
 * layar master data {@code VariableSuratKeluarAction}.
 *
 * <p>
 * Ini adalah mesin mail-merge yang nyata dipakai, bukan sekadar skema: pada
 * {@code ais.action.master.surat.util.SuratUtilHelper} (mesin utama penyusun peta parameter cetak
 * surat keluar), seluruh baris {@code VariableSuratKeluar} diambil lewat
 * {@code ConstantValues.ambilBerdasarClass(VariableSuratKeluar.class)} lalu setiap barisnya
 * dipetakan sebagai {@code parameters.put(v.getKey(), v.getNilai())} — sehingga placeholder
 * bernama {@link #getKey()} pada template surat keluar akan otomatis diganti dengan
 * {@link #getNilai()} saat surat dicetak. Dengan begitu, admin bisa menambah variabel global baru
 * (mis. alamat kantor, nomor telepon, nama pejabat berwenang) tanpa perlu perubahan kode.
 * </p>
 *
 * <p>
 * Field {@link #getTipe()} menandai jenis nilai variabel via salah satu konstanta
 * {@link #TEXT}/{@link #GAMBAR}/{@link #DATA}, dan {@link #getLampiranId()} menunjuk ke lampiran
 * (mis. gambar/logo) bila tipenya {@link #GAMBAR}. {@link #getKey()} dihitung otomatis dari
 * {@link #getNama()} (di-slug-kan: huruf kecil, spasi menjadi underscore) sehingga key placeholder
 * selalu konsisten dengan nama tampilan variabel — perubahan nama otomatis mengubah key pada
 * pembacaan berikutnya (efek samping baca-tulis pada getter, bukan hanya di setter).
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
@Table(schema = "surat", name = "variable_surat_keluar")
public class VariableSuratKeluar extends GeneralValueObject {

	/** Nilai {@link #getTipe()} untuk variabel berjenis gambar (mis. logo/tanda tangan). */
	public static final String GAMBAR = "Image";
	/** Nilai {@link #getTipe()} untuk variabel berjenis teks biasa. */
	public static final String TEXT = "Text";
	/** Nilai {@link #getTipe()} untuk variabel berjenis data terstruktur. */
	public static final String DATA = "Data";

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
	 * Representasi teks singkat entity ini, dipakai antara lain oleh komponen ZK agar baris
	 * tampil sebagai nama variabel itu sendiri.
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
	private String key;
	private String keterangan;
	private String nilai;
	private String tipe;
	private Boolean tampil;
	private Long lampiranId;
	private Integer nomorUrut;

	/** Constructor default (dibutuhkan Hibernate). Tidak menginisialisasi field apa pun. */
	public VariableSuratKeluar() {
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
	 * Mengambil nama tampilan variabel ini (mis. "Alamat Kantor"). Kolom ini unik di database —
	 * tidak boleh ada dua variabel dengan nama yang sama.
	 *
	 * @return nama setelah di-trim, atau {@code null} bila belum diisi.
	 */
	@Column(name = "nama", nullable = false, length = 255, unique = true)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengeset nama tampilan variabel. Mengubah nama akan mengubah hasil {@link #getKey()} pada
	 * pembacaan berikutnya karena key di-derive otomatis dari nama.
	 *
	 * @param nama nama baru (belum di-trim; trimming terjadi saat pembacaan via {@link #getNama()}).
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil keterangan/deskripsi tambahan untuk variabel ini.
	 *
	 * @return keterangan, tidak pernah {@code null} — dinormalisasi menjadi string kosong
	 *         ({@code ""}) bila field belum diisi atau hanya berisi spasi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		if (keterangan == null || keterangan.trim().isEmpty()) {
			keterangan = "";
		}
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
	 * Mengambil key placeholder unik yang dipakai template cetak surat keluar untuk merujuk
	 * variabel ini (lihat {@code SuratUtilHelper}, yang memetakan
	 * {@code parameters.put(v.getKey(), v.getNilai())} saat menyusun parameter cetak). Kolom ini
	 * unik di database.
	 *
	 * <p>
	 * <b>Perhatian:</b> key TIDAK disimpan independen — setiap kali getter ini dipanggil dan
	 * {@link #getNama()} sedang terisi, field {@code key} dihitung ulang dari nama (di-lowercase,
	 * spasi diganti underscore) dan ditulis kembali ke field, menimpa nilai {@code key} yang
	 * mungkin sudah diset manual lewat {@link #setKey(String)}. Efek samping baca-tulis ini
	 * memastikan key selalu sinkron dengan nama tampilan terbaru.
	 * </p>
	 *
	 * @return key placeholder turunan dari nama, atau nilai {@code key} apa adanya bila nama
	 *         belum diisi.
	 */
	@Column(name = "key_", unique = true)
	public String getKey() {
		boolean ada = nama != null && !nama.trim().equals("");
		if (ada) {
			key = nama.trim().toLowerCase().replaceAll(" ", "_");
		}
		return key;
	}

	/**
	 * Mengeset key placeholder secara manual. Nilai ini akan ditimpa oleh {@link #getKey()} pada
	 * pembacaan berikutnya selama {@link #getNama()} terisi — lihat catatan pada {@link #getKey()}.
	 *
	 * @param key key placeholder baru.
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * Mengambil nilai aktual variabel ini — string yang akan menggantikan placeholder
	 * {@link #getKey()} pada template surat keluar saat dicetak.
	 *
	 * @return nilai variabel, tidak pernah {@code null} — dinormalisasi menjadi string kosong
	 *         ({@code ""}) bila field belum diisi atau hanya berisi spasi.
	 */
	@Column(columnDefinition = "text", name = "nilai_")
	public String getNilai() {
		if (nilai == null || nilai.trim().isEmpty()) {
			nilai = "";
		}

		return nilai;
	}

	/**
	 * Mengeset nilai aktual variabel ini.
	 *
	 * @param nilai nilai baru.
	 */
	public void setNilai(String nilai) {
		this.nilai = nilai;
	}

	/**
	 * Mengambil jenis nilai variabel ini, salah satu dari konstanta {@link #TEXT}/
	 * {@link #GAMBAR}/{@link #DATA}.
	 *
	 * @return tipe variabel; default {@link String}{@code .class.getName()} (setara
	 *         {@link #TEXT} secara konseptual) ketika field belum pernah diset.
	 */
	public String getTipe() {
		if (tipe == null) {
			tipe = String.class.getName();
		}
		return tipe;
	}

	/**
	 * Mengeset jenis nilai variabel ini.
	 *
	 * @param tipe tipe baru, idealnya salah satu dari {@link #TEXT}/{@link #GAMBAR}/{@link #DATA}.
	 */
	public void setTipe(String tipe) {
		this.tipe = tipe;
	}

	/**
	 * Mengambil ID lampiran (mis. file gambar/logo) yang menjadi nilai variabel ini bila
	 * {@link #getTipe()} adalah {@link #GAMBAR}.
	 *
	 * @return ID lampiran, bisa {@code null} bila variabel ini bukan bertipe gambar.
	 */
	public Long getLampiranId() {
		return lampiranId;
	}

	/**
	 * Mengeset ID lampiran yang menjadi nilai variabel ini.
	 *
	 * @param lampiranId ID lampiran baru.
	 */
	public void setLampiranId(Long lampiranId) {
		this.lampiranId = lampiranId;
	}

	/**
	 * Mengambil status apakah variabel ini ditampilkan pada daftar variabel yang bisa dipilih.
	 *
	 * @return {@code true} bila ditampilkan; default {@code true} ketika field belum pernah diset.
	 */
	public Boolean getTampil() {
		return tampil == null ? true : tampil;
	}

	/**
	 * Mengeset status tampil/tidaknya variabel ini.
	 *
	 * @param tampil status tampil baru.
	 */
	public void setTampil(Boolean tampil) {
		this.tampil = tampil;
	}

	/**
	 * Mengambil nomor urut tampilan variabel ini relatif terhadap variabel lain (untuk
	 * pengurutan pada daftar/form).
	 *
	 * @return nomor urut; default {@code 1} ketika field belum pernah diset.
	 */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Mengeset nomor urut tampilan variabel ini.
	 *
	 * @param nomorUrut nomor urut baru.
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}
}
