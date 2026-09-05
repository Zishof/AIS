package ais.database.model.asset;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.surat.NomorSurat;

/**
 * Katalog JENIS perbaikan aset (mis. "Servis Rutin", "Perbaikan Kerusakan", "Kalibrasi") --
 * sekaligus AKAR konfigurasi parameter tambahan dinamis untuk form {@link PerbaikanAsset}.
 *
 * <h3>Peran ganda: katalog + konfigurasi form dinamis</h3>
 *
 * <p>Selain sebagai entri master data biasa (kode, nama, keterangan, status aktif), kelas ini
 * memegang relasi many-to-many ke {@link KelompokParameterTambahanPerbaikanAsset} lewat {@link
 * #getKelompokParameterTambahanPerbaikanAssets()} -- daftar kelompok field tambahan yang berlaku
 * untuk jenis perbaikan ini. Saat pengguna memilih jenis perbaikan tertentu di form, {@link
 * ais.action.master.helper.ParameterTambahanPerbaikanAssetListener} membaca relasi ini untuk
 * membangun ulang baris-baris field dinamis pada form -- lihat javadoc listener tersebut dan
 * javadoc {@link PerbaikanAsset} untuk alur lengkapnya.</p>
 *
 * <h3>Cache statis lintas-request: {@link #mapParameters}</h3>
 *
 * <p>Relasi many-to-many di atas DI-CACHE secara statis per id jenis perbaikan lewat field
 * {@code static} {@link #mapParameters} -- lihat javadoc field dan getter/setter terkait untuk
 * konsekuensinya (cache ini dibagi oleh SELURUH request/sesi dalam satu JVM, tidak per-user
 * maupun per-request).</p>
 *
 * @see PerbaikanAsset baris perbaikan yang memakai jenis ini
 * @see KelompokParameterTambahanPerbaikanAsset kelompok parameter tambahan yang dikaitkan
 * @see ais.action.master.helper.ParameterTambahanPerbaikanAssetListener pemakai relasi ini di form
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "jenis_perbaikan_asset")
public class JenisPerbaikanAsset extends GeneralValueObject {

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
	 * Kait JPA {@code @PreUpdate}: memperbarui stempel waktu audit sebelum UPDATE dikirim, lalu
	 * mendeklarasikan field {@code tanggal_dirubah} pada baris yang sama (gaya penulisan padat
	 * warisan hbm2java, tidak diformat ulang agar diff commit tetap minimal terhadap bagian yang
	 * tidak diedit). Didelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} agar aturannya
	 * terpusat.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

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

	/**
	 * Representasi teks berupa id digabung nama, dipakai label komponen ZK dan pesan log.
	 *
	 * @return {@code "<id>-<nama>"}; bagian nama bisa berupa string {@code "null"} literal bila
	 *         field {@code nama} belum terisi (dipakai apa adanya, bukan lewat getter)
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode singkat jenis perbaikan. */
	private String kode;

	/** Nama jenis perbaikan; diwarisi oleh {@link PerbaikanAsset#getNama()} bila baris perbaikan belum diberi nama sendiri. */
	private String nama;

	/** Keterangan bebas untuk jenis perbaikan ini. */
	private String keterangan;

	/** Status aktif/tidak aktif jenis perbaikan ini. */
	private Boolean aktif;

	/** Nomor surat template yang dipakai penomoran dokumen perbaikan bertipe ini. */
	private NomorSurat nomorSurat;

	/**
	 * Cache statis relasi many-to-many {@link #kelompokParameterTambahanPerbaikanAssets},
	 * dikunci per id jenis perbaikan ({@link #getId()}).
	 *
	 * <p><b>Perhatian arsitektur:</b> field ini {@code static}, artinya dibagi oleh SELURUH
	 * thread/request/sesi pengguna dalam satu JVM -- BUKAN cache per-request atau per-sesi
	 * Hibernate. Sekali entri untuk suatu id ditulis (lewat {@link
	 * #setKelompokParameterTambahanPerbaikanAssets(Set)} pada instance mana pun dengan id
	 * tersebut), entri itu akan dipakai oleh {@link
	 * #getKelompokParameterTambahanPerbaikanAssets()} pada instance LAIN dengan id yang sama,
	 * bahkan pada sesi Hibernate yang berbeda. Cache ini TIDAK PERNAH dibersihkan/di-invalidasi
	 * di kelas ini -- tidak ada method {@code clear()}/{@code evict()} -- sehingga perubahan
	 * relasi kelompok parameter tambahan yang dilakukan lewat jalur lain (mis. langsung lewat
	 * SQL, atau lewat instance Hibernate yang berbeda tanpa memanggil setter ini) tidak akan
	 * terlihat sampai proses JVM di-restart atau entri di-timpa lewat setter. Karena berupa
	 * {@code Map} biasa (bukan struktur tersinkronisasi), akses bersamaan dari banyak thread juga
	 * berpotensi race condition, meski dampaknya terbatas karena isinya berupa data konfigurasi
	 * yang jarang berubah.</p>
	 */
	public static Map<Long, Set<KelompokParameterTambahanPerbaikanAsset>> mapParameters = new HashMap<Long, Set<KelompokParameterTambahanPerbaikanAsset>>();

	/** Kelompok parameter tambahan yang berlaku untuk jenis perbaikan ini; lihat {@link #getKelompokParameterTambahanPerbaikanAssets()}. */
	private Set<KelompokParameterTambahanPerbaikanAsset> kelompokParameterTambahanPerbaikanAssets = new TreeSet<KelompokParameterTambahanPerbaikanAsset>();

	/**
	 * Kelompok parameter tambahan yang berlaku untuk jenis perbaikan ini -- getter yang
	 * dipengaruhi CACHE STATIS {@link #mapParameters}.
	 *
	 * <p>Bila {@link #getId()} tidak {@code null} DAN {@link #mapParameters} sudah memiliki entri
	 * untuk id tersebut (ditulis sebelumnya lewat {@link
	 * #setKelompokParameterTambahanPerbaikanAssets(Set)} pada instance mana pun), entri cache
	 * itu MENIMPA nilai relasi Hibernate yang baru saja di-load -- artinya hasil query database
	 * yang sebenarnya bisa diabaikan demi nilai cache lama. Diurutkan lewat {@code @OrderBy}
	 * ({@code nomorUrut asc, nama asc}) pada level query Hibernate; urutan itu tidak berlaku lagi
	 * bila yang dipakai adalah nilai dari cache statis (urutan mengikuti apa pun yang tersimpan
	 * di cache saat itu).</p>
	 *
	 * @return kelompok parameter tambahan yang berlaku, dari relasi Hibernate atau dari
	 *         {@link #mapParameters} bila sudah ter-cache untuk id ini
	 */
	@ManyToMany(targetEntity = KelompokParameterTambahanPerbaikanAsset.class, cascade = { CascadeType.MERGE })
	@OrderBy(value = "nomorUrut asc, nama asc")
	@JoinTable(name = "jenis_perbaikan_asset_has_parameter", schema = "asset", joinColumns = @JoinColumn(name = "jenis_perbaikan_asset"), inverseJoinColumns = @JoinColumn(name = "parameter"))
	public Set<KelompokParameterTambahanPerbaikanAsset> getKelompokParameterTambahanPerbaikanAssets() {
		if (id != null) {
			Set<KelompokParameterTambahanPerbaikanAsset> temp = mapParameters.get(id);
			if (temp != null) {
				kelompokParameterTambahanPerbaikanAssets = temp;
			}
		}
		return kelompokParameterTambahanPerbaikanAssets;
	}

	/**
	 * Menetapkan kelompok parameter tambahan untuk jenis perbaikan ini, DAN sekaligus MENULIS
	 * entri baru ke cache statis {@link #mapParameters} bila {@link #getId()} tidak {@code null}
	 * -- lihat javadoc {@link #mapParameters} untuk konsekuensi cache lintas-JVM ini.
	 *
	 * @param kelompokParameterTambahanPerbaikanAssets kelompok baru yang berlaku
	 */
	public void setKelompokParameterTambahanPerbaikanAssets(
			Set<KelompokParameterTambahanPerbaikanAsset> kelompokParameterTambahanPerbaikanAssets) {
		this.kelompokParameterTambahanPerbaikanAssets = kelompokParameterTambahanPerbaikanAssets;
		if (id != null) {
			mapParameters.put(id, kelompokParameterTambahanPerbaikanAssets);
		}
	}

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate dan ZK data binding.
	 */
	public JenisPerbaikanAsset() {
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
	 * Kode singkat jenis perbaikan.
	 *
	 * @return kode hasil {@code trim()}, atau {@code ""} bila belum terisi
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Mengisi kode singkat jenis perbaikan.
	 *
	 * @param kode kode baru
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Nama jenis perbaikan.
	 *
	 * @return nama hasil {@code trim()}, atau {@code null} bila belum terisi
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama jenis perbaikan.
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas untuk jenis perbaikan ini.
	 *
	 * <p>Berbeda dari kebanyakan getter keterangan lain di paket ini, method ini mengembalikan
	 * field APA ADANYA tanpa {@code trim()} maupun substitusi ke string kosong -- bisa
	 * mengembalikan {@code null}.</p>
	 *
	 * @return keterangan tersimpan, atau {@code null} bila belum terisi
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas.
	 *
	 * @param keterangan teks keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Status aktif/tidak aktif jenis perbaikan ini.
	 *
	 * @return {@code true} bila aktif atau belum pernah diisi (default aktif); {@code false} bila
	 *         eksplisit dinonaktifkan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengisi status aktif.
	 *
	 * @param aktif nilai baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Nomor surat template yang dipakai penomoran dokumen perbaikan bertipe ini.
	 *
	 * <p>Dilewatkan lewat {@link #check(Object)} untuk memastikan proxy lazy sudah teresolusi.</p>
	 *
	 * @return template nomor surat, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat", nullable = true)
	public NomorSurat getNomorSurat() {
		nomorSurat = check(nomorSurat);
		return nomorSurat;
	}

	/**
	 * Menetapkan template nomor surat.
	 *
	 * @param nomorSurat template baru
	 */
	public void setNomorSurat(NomorSurat nomorSurat) {
		this.nomorSurat = nomorSurat;
	}

}
