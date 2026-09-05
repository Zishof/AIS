package ais.database.model.surat;

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

import ais.database.model.Gedung;
import ais.database.model.GeneralValueObject;
import ais.database.model.Ruang;
import ais.database.model.rab.SatuanKerja;

/**
 * Entity JPA/Hibernate untuk tabel {@code surat.loker_surat}: master data lokasi fisik
 * penyimpanan arsip surat masuk/keluar — "loker" di sini merujuk pada lokasi rak/lemari fisik
 * (gedung, ruang, lantai, lemari, rak, boks), bukan loker virtual/elektronik.
 *
 * <p>
 * Dikelola lewat layar master data {@code LokerSuratAction} dan direferensikan sebagai field
 * {@code ManyToOne} dari {@code SuratMasuk}/{@code SuratKeluar} (menandai di mana fisik surat
 * tersebut disimpan setelah diarsipkan). Dipakai pula pada dasbor
 * {@code DasboardSuratMasukLokerSurat} dan laporan rekap per-loker
 * ({@code LaporanSuratPerLoker}, {@code LaporanStatistikPerLoker}).
 * </p>
 *
 * <p>
 * Field {@link #getGedung()}, {@link #getRuang()}, dan {@link #getSatuanKerja()} adalah relasi
 * {@code ManyToOne} lazy yang di-deproxy/di-cache lewat {@code check(...)} (helper bersama pada
 * {@link GeneralValueObject}) setiap kali dibaca — pola umum di banyak entity AIS untuk
 * menghindari {@code LazyInitializationException} di luar sesi Hibernate. Khusus
 * {@link #getGedung()}, bila {@link #getRuang()} sudah diisi dan ruang tersebut memiliki gedung
 * sendiri, gedung ruang tersebut MENIMPA field {@code gedung} milik loker ini — sehingga gedung
 * loker selalu konsisten dengan gedung ruangnya bila keduanya diisi bersamaan (getter dengan efek
 * samping tulis, bukan hanya baca).
 * </p>
 *
 * <p>
 * Mewarisi field audit shadow {@code oleh}/{@code olehId}/{@code tanggal_dirubah} dari kerangka
 * entity AIS; field-field tersebut adalah kebutuhan teknis (integrasi Envers/cache), bukan cacat
 * desain.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "surat", name = "loker_surat")
public class LokerSurat extends GeneralValueObject {

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
	 * tampil sebagai nama loker itu sendiri.
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

	private String kode;
	private String nama;
	private String boks;
	private String lantai;
	private String lemari;
	private String rak;
	private Gedung gedung;
	private Ruang ruang;
	private String keterangan;
	private String tipe;
	private SatuanKerja satuanKerja;
	private Boolean aktif;

	/** Constructor default (dibutuhkan Hibernate). Tidak menginisialisasi field apa pun. */
	public LokerSurat() {
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
	 * Mengambil kode singkat loker ini.
	 *
	 * @return kode, bisa {@code null} — berbeda dari beberapa katalog sejenis di paket ini,
	 *         getter ini TIDAK melakukan trim atau normalisasi.
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Mengeset kode singkat loker.
	 *
	 * @param kode kode baru.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengambil nama loker ini.
	 *
	 * @return nama setelah di-trim, atau {@code null} bila belum diisi.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengeset nama loker.
	 *
	 * @param nama nama baru (belum di-trim; trimming terjadi saat pembacaan via {@link #getNama()}).
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil keterangan/deskripsi tambahan untuk loker ini.
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
	 * Mengambil gedung tempat loker ini berada.
	 *
	 * <p>
	 * <b>Efek samping:</b> method ini terlebih dahulu memanggil {@link #getRuang()} (yang juga
	 * mem-fetch/deproxy ruang lewat {@code check(...)}); bila ruang tersebut ada dan memiliki
	 * gedung sendiri, field {@code gedung} milik loker ini DITIMPA dengan gedung dari ruang
	 * tersebut sebelum dikembalikan. Bila tidak, field {@code gedung} yang tersimpan langsung
	 * di-deproxy/di-cache lewat {@code check(...)} seperti biasa. Dengan kata lain, gedung ruang
	 * selalu lebih diutamakan daripada gedung yang diset langsung ke loker.
	 * </p>
	 *
	 * @return gedung efektif tempat loker ini berada, bisa {@code null} bila keduanya
	 *         (gedung langsung maupun gedung dari ruang) tidak diset.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gedung", nullable = true)
	public Gedung getGedung() {
		ruang = getRuang();
		if (ruang != null && ruang.getGedung() != null) {
			gedung = ruang.getGedung();
		} else {
			gedung = check(gedung);
		}

		return gedung;
	}

	/**
	 * Mengeset gedung tempat loker ini berada secara langsung. Nilai ini bisa ditimpa saat
	 * dibaca lewat {@link #getGedung()} bila {@link #getRuang()} memiliki gedungnya sendiri —
	 * lihat catatan pada {@link #getGedung()}.
	 *
	 * @param gedung gedung baru.
	 */
	public void setGedung(Gedung gedung) {
		this.gedung = gedung;
	}

	/**
	 * Mengambil ruang tempat loker ini berada.
	 *
	 * @return ruang terkait setelah di-deproxy/di-cache lewat {@code check(...)}, bisa
	 *         {@code null} bila belum diset.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ruang", nullable = true)
	public Ruang getRuang() {
		ruang = check(ruang);
		return ruang;
	}

	/**
	 * Mengeset ruang tempat loker ini berada.
	 *
	 * @param ruang ruang baru.
	 */
	public void setRuang(Ruang ruang) {
		this.ruang = ruang;
	}

	/**
	 * Mengambil label lantai fisik tempat loker ini berada (teks bebas, mis. "2").
	 *
	 * @return lantai, bisa {@code null}.
	 */
	public String getLantai() {
		return lantai;
	}

	/**
	 * Mengeset label lantai fisik.
	 *
	 * @param lantai lantai baru.
	 */
	public void setLantai(String lantai) {
		this.lantai = lantai;
	}

	/**
	 * Mengambil label lemari fisik tempat loker ini berada (teks bebas).
	 *
	 * @return lemari, bisa {@code null}.
	 */
	public String getLemari() {
		return lemari;
	}

	/**
	 * Mengeset label lemari fisik.
	 *
	 * @param lemari lemari baru.
	 */
	public void setLemari(String lemari) {
		this.lemari = lemari;
	}

	/**
	 * Mengambil label rak fisik tempat loker ini berada (teks bebas).
	 *
	 * @return rak, bisa {@code null}.
	 */
	public String getRak() {
		return rak;
	}

	/**
	 * Mengeset label rak fisik.
	 *
	 * @param rak rak baru.
	 */
	public void setRak(String rak) {
		this.rak = rak;
	}

	/**
	 * Mengambil status aktif/tidaknya loker ini sebagai opsi yang bisa dipilih pengguna.
	 *
	 * @return {@code true} bila aktif; default {@code true} ketika field belum pernah diset.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengeset status aktif/tidaknya loker ini.
	 *
	 * @param aktif status aktif baru.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengambil label boks fisik tempat loker ini berada (teks bebas, unit terkecil di bawah rak).
	 *
	 * @return boks, bisa {@code null}.
	 */
	public String getBoks() {
		return boks;
	}

	/**
	 * Mengeset label boks fisik.
	 *
	 * @param boks boks baru.
	 */
	public void setBoks(String boks) {
		this.boks = boks;
	}

	/**
	 * Mengambil tipe/jenis loker ini (teks bebas, mis. untuk membedakan loker surat masuk vs
	 * surat keluar, atau kategori penyimpanan lain).
	 *
	 * @return tipe, bisa {@code null}.
	 */
	public String getTipe() {
		return tipe;
	}

	/**
	 * Mengeset tipe/jenis loker.
	 *
	 * @param tipe tipe baru.
	 */
	public void setTipe(String tipe) {
		this.tipe = tipe;
	}

	/**
	 * Mengambil satuan kerja pemilik/pengelola loker ini.
	 *
	 * @return satuan kerja terkait setelah di-deproxy/di-cache lewat {@code check(...)}, bisa
	 *         {@code null} bila belum diset (loker berlaku umum/tanpa pembatasan satuan kerja).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Mengeset satuan kerja pemilik/pengelola loker ini.
	 *
	 * @param satuanKerja satuan kerja baru.
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

}
