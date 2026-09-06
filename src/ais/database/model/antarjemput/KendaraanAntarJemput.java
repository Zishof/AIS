package ais.database.model.antarjemput;

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
import ais.database.model.Pegawai;
import ais.database.model.asset.Asset;
import ais.database.model.asset.AssetDetail;

/**
 * Entitas Hibernate untuk satu kendaraan layanan antar-jemput siswa — dipetakan ke tabel
 * {@code public.kendaraan_antar_jemput} (modul {@code antarjemput}). Merepresentasikan armada
 * (bus/mobil jemputan) beserta kru default (sopir + hingga 3 kenek/pendamping), opsional terhubung
 * ke aset inventaris AIS lewat {@link Asset}/{@link AssetDetail} untuk pelacakan aset fisiknya.
 * Dipakai sebagai default kendaraan &amp; sopir pada {@link JadwalAntarJemput} (lihat
 * {@link JadwalAntarJemput#getSopir()}).
 *
 * <h2>Fallback nilai default</h2>
 * <p>
 * {@link #getNama()} memakai nama {@link #assetDetail} lalu {@link #asset} sebagai fallback bila
 * nama kendaraan belum diisi manual; {@link #getNomorPolisi()} selalu dinormalisasi ke huruf
 * besar; {@link #getKapasitasDuduk()} default {@code 0}; {@link #getAktif()} default {@code true}.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "kendaraan_antar_jemput")
public class KendaraanAntarJemput extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439811L;

	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	private String kode;
	/** Nama kendaraan; bila belum diisi, fallback ke nama {@link #assetDetail} lalu {@link #asset} — lihat {@link #getNama()}. */
	private String nama;
	private String keterangan;
	/** Nomor polisi/plat kendaraan; selalu dikembalikan huruf besar oleh {@link #getNomorPolisi()}. */
	private String nomorPolisi;
	/** Kapasitas jumlah tempat duduk penumpang; default {@code 0} bila belum di-set. */
	private Integer kapasitasDuduk;
	private Boolean aktif;

	/** Aset inventaris AIS yang berkorespondensi dengan kendaraan ini, bila dicatat sebagai aset (level induk). */
	private Asset asset;
	/** Detail aset inventaris AIS yang berkorespondensi dengan kendaraan ini (lebih spesifik dari {@link #asset}). */
	private AssetDetail assetDetail;
	/** Sopir default kendaraan ini. */
	private Pegawai sopir;
	private Pegawai kenek1;
	private Pegawai kenek2;
	private Pegawai kenek3;

	/** Konstruktor default (dibutuhkan Hibernate). */
	public KendaraanAntarJemput() {
	}

	/** @return ID unik baris kendaraan (primary key, auto-increment via {@code IDENTITY}). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/** @param id lihat {@link #getId()}. Normalnya tidak perlu diisi manual — dihasilkan DB saat insert. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return ID pengguna (username) yang terakhir mengubah baris ini. Field audit shadow — lihat {@link #getOleh()}. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Setter {@link #getOlehId()}. Nilai kosong/blank diabaikan (no-op) agar jejak audit lama
	 * tidak tertimpa saat proses simpan tidak membawa identitas pengguna — pola baku di semua
	 * entitas modul antarjemput.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/** @return nama pengguna yang terakhir mengubah baris ini (field audit shadow, diisi via {@link #onUpdate()}). */
	public String getOleh() {
		return oleh;
	}

	/** Setter {@link #getOleh()}. Nilai kosong/blank diabaikan (no-op), sama seperti {@link #setOlehId(String)}. */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum baris ini
	 * di-UPDATE, memperbarui {@link #tanggal_dirubah} (dan field audit terkait) lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** @return timestamp terakhir baris ini diubah; diisi otomatis saat objek dibuat dan diperbarui via {@link #onUpdate()}. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @param tanggal_dirubah lihat {@link #getTanggal_dirubah()}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return kode singkat kendaraan ini, di-trim; {@code null} bila belum diisi. */
	@Column(name = "kode", length = 50)
	public String getKode() {
		return kode == null ? null : kode.trim();
	}

	/** @param kode lihat {@link #getKode()}. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * @return nama kendaraan, di-trim. Bila belum diisi manual, jatuh berurutan ke nama
	 *         {@link #getAssetDetail()} lalu {@link #getAsset()} (dua sumber aset inventaris
	 *         AIS); hasil fallback itu ikut di-cache ke field {@link #nama} in-memory (pola yang
	 *         sama seperti {@link JadwalAntarJemput#getNama()}).
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if (nama == null && getAssetDetail() != null) {
			nama = getAssetDetail().getNama();
		}
		if (nama == null && getAsset() != null) {
			nama = getAsset().getNama();
		}
		return nama == null ? null : nama.trim();
	}

	/** @param nama lihat {@link #getNama()}. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan/catatan bebas untuk kendaraan ini. */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return keterangan;
	}

	/** @param keterangan lihat {@link #getKeterangan()}. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return nomor polisi/plat kendaraan, selalu dikembalikan huruf besar (di-trim lalu {@code toUpperCase()}); {@code null} bila belum diisi. */
	@Column(name = "nomor_polisi", length = 30)
	public String getNomorPolisi() {
		return nomorPolisi == null ? null : nomorPolisi.trim().toUpperCase();
	}

	/** @param nomorPolisi lihat {@link #getNomorPolisi()}; nilai apa adanya disimpan (normalisasi huruf besar hanya terjadi saat dibaca lewat getter). */
	public void setNomorPolisi(String nomorPolisi) {
		this.nomorPolisi = nomorPolisi;
	}

	/** @return kapasitas jumlah tempat duduk penumpang; default {@code 0} bila belum di-set. */
	public Integer getKapasitasDuduk() {
		return kapasitasDuduk == null ? 0 : kapasitasDuduk;
	}

	/** @param kapasitasDuduk lihat {@link #getKapasitasDuduk()}. */
	public void setKapasitasDuduk(Integer kapasitasDuduk) {
		this.kapasitasDuduk = kapasitasDuduk;
	}

	/** @return {@code true} bila kendaraan aktif/beroperasi; default {@code true} bila belum di-set (tidak di-cache ke field). */
	public Boolean getAktif() {
		return aktif == null ? Boolean.TRUE : aktif;
	}

	/** @param aktif lihat {@link #getAktif()}. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/** @return aset inventaris AIS (level induk) yang berkorespondensi dengan kendaraan ini, bila dicatat sebagai aset; sumber fallback kedua untuk {@link #getNama()}. Dilewatkan {@code check()} agar proxy Hibernate yang sudah dihapus/tidak valid tidak ikut terekspos ke pemanggil. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "asset")
	public Asset getAsset() {
		asset = check(asset);
		return asset;
	}

	/** @param asset lihat {@link #getAsset()}. */
	public void setAsset(Asset asset) {
		this.asset = asset;
	}

	/** @return detail aset inventaris AIS (lebih spesifik dari {@link #getAsset()}) yang berkorespondensi dengan kendaraan ini; sumber fallback pertama untuk {@link #getNama()}. Dilewatkan {@code check()} agar proxy Hibernate yang sudah dihapus/tidak valid tidak ikut terekspos ke pemanggil. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "asset_detail")
	public AssetDetail getAssetDetail() {
		assetDetail = check(assetDetail);
		return assetDetail;
	}

	/** @param assetDetail lihat {@link #getAssetDetail()}. */
	public void setAssetDetail(AssetDetail assetDetail) {
		this.assetDetail = assetDetail;
	}

	/** @return sopir default kendaraan ini; dipakai sebagai fallback sopir jadwal bila jadwal tidak menentukan sopirnya sendiri — lihat {@link JadwalAntarJemput#getSopir()}. Dilewatkan {@code check()} agar proxy Hibernate yang sudah dihapus/tidak valid tidak ikut terekspos ke pemanggil. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sopir")
	public Pegawai getSopir() {
		sopir = check(sopir);
		return sopir;
	}

	/** @param sopir lihat {@link #getSopir()}. */
	public void setSopir(Pegawai sopir) {
		this.sopir = sopir;
	}

	/** @return kenek/pendamping default pertama kendaraan ini, bila ada. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kenek1")
	public Pegawai getKenek1() {
		kenek1 = check(kenek1);
		return kenek1;
	}

	/** @param kenek1 lihat {@link #getKenek1()}. */
	public void setKenek1(Pegawai kenek1) {
		this.kenek1 = kenek1;
	}

	/** @return kenek/pendamping default kedua kendaraan ini, bila ada. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kenek2")
	public Pegawai getKenek2() {
		kenek2 = check(kenek2);
		return kenek2;
	}

	/** @param kenek2 lihat {@link #getKenek2()}. */
	public void setKenek2(Pegawai kenek2) {
		this.kenek2 = kenek2;
	}

	/** @return kenek/pendamping default ketiga kendaraan ini, bila ada. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kenek3")
	public Pegawai getKenek3() {
		kenek3 = check(kenek3);
		return kenek3;
	}

	/** @param kenek3 lihat {@link #getKenek3()}. */
	public void setKenek3(Pegawai kenek3) {
		this.kenek3 = kenek3;
	}
}
