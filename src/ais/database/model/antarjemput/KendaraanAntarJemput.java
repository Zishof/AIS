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

	public KendaraanAntarJemput() {
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	public String getOleh() {
		return oleh;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Column(name = "kode", length = 50)
	public String getKode() {
		return kode == null ? null : kode.trim();
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

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

	public void setNama(String nama) {
		this.nama = nama;
	}

	@Column(name = "keterangan")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@Column(name = "nomor_polisi", length = 30)
	public String getNomorPolisi() {
		return nomorPolisi == null ? null : nomorPolisi.trim().toUpperCase();
	}

	public void setNomorPolisi(String nomorPolisi) {
		this.nomorPolisi = nomorPolisi;
	}

	public Integer getKapasitasDuduk() {
		return kapasitasDuduk == null ? 0 : kapasitasDuduk;
	}

	public void setKapasitasDuduk(Integer kapasitasDuduk) {
		this.kapasitasDuduk = kapasitasDuduk;
	}

	public Boolean getAktif() {
		return aktif == null ? Boolean.TRUE : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "asset")
	public Asset getAsset() {
		asset = check(asset);
		return asset;
	}

	public void setAsset(Asset asset) {
		this.asset = asset;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "asset_detail")
	public AssetDetail getAssetDetail() {
		assetDetail = check(assetDetail);
		return assetDetail;
	}

	public void setAssetDetail(AssetDetail assetDetail) {
		this.assetDetail = assetDetail;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sopir")
	public Pegawai getSopir() {
		sopir = check(sopir);
		return sopir;
	}

	public void setSopir(Pegawai sopir) {
		this.sopir = sopir;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kenek1")
	public Pegawai getKenek1() {
		kenek1 = check(kenek1);
		return kenek1;
	}

	public void setKenek1(Pegawai kenek1) {
		this.kenek1 = kenek1;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kenek2")
	public Pegawai getKenek2() {
		kenek2 = check(kenek2);
		return kenek2;
	}

	public void setKenek2(Pegawai kenek2) {
		this.kenek2 = kenek2;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kenek3")
	public Pegawai getKenek3() {
		kenek3 = check(kenek3);
		return kenek3;
	}

	public void setKenek3(Pegawai kenek3) {
		this.kenek3 = kenek3;
	}
}
