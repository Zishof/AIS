package ais.database.model.inventory;

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

/**
 * <h2>SesiStokOpname — Sesi/Jadwal Stock Opname (formal) untuk Toko/Kantin.</h2>
 *
 * <p>
 * Entity BARU yang melengkapi pencatatan opname baris-per-baris ({@link StokOpname}) dengan
 * <b>kepala kegiatan opname</b>: jadwal, lokasi/toko, kategori, petugas, status, dan catatan.
 * Diperlukan untuk laporan katalog §2.6 <i>Jadwal Stock Opname</i> dan <i>Berita Acara Stock
 * Opname</i> yang belum bisa dibuat karena {@code stok_opname} hanya menyimpan hasil per barang
 * tanpa konsep sesi. Baris hasil opname tetap pada {@code koperasi.stok_opname}; berita acara
 * dirakit dengan meng-agregat baris tersebut pada rentang tanggal &amp; toko sesi ini (tanpa mengubah
 * tabel {@code stok_opname}).
 * </p>
 *
 * <p>
 * Dengan pendaftaran di {@code hibernate.cfg.xml}, tabel {@code koperasi.sesi_stok_opname} otomatis
 * dibuat (hbm2ddl=update). Penamaan kolom mengikuti aturan proyek (field tanpa @Column ter-fold ke
 * huruf kecil tanpa underscore, mis. {@code tanggalRencana}→{@code tanggalrencana}). Kompatibel
 * Java 1.7 / Hibernate 3.
 * </p>
 *
 * @author AIS e-Kantin (modul stock opname)
 * @see StokOpname
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "sesi_stok_opname")
public class SesiStokOpname extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	/** Baru dijadwalkan, belum dikerjakan. */
	public static final String STATUS_RENCANA = "RENCANA";
	/** Sedang berjalan. */
	public static final String STATUS_BERJALAN = "BERJALAN";
	/** Sudah selesai (berita acara final). */
	public static final String STATUS_SELESAI = "SELESAI";

	private Long id;
	private Toko toko;
	private String kode;
	private Date tanggalRencana;
	private Date tanggalMulai;
	private Date tanggalSelesai;
	private String kategori;
	private String petugas;
	private String status;
	private String keterangan;
	private String oleh;
	private String olehId;

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public SesiStokOpname() {
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko")
	public Toko getToko() {
		toko = check(toko);
		return toko;
	}

	public void setToko(Toko toko) {
		this.toko = toko;
	}

	public String getKode() {
		return kode;
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggalRencana() {
		return tanggalRencana == null ? ais.ui.util.WaktuUtil.getDate() : tanggalRencana;
	}

	public void setTanggalRencana(Date tanggalRencana) {
		this.tanggalRencana = tanggalRencana;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggalMulai() {
		return tanggalMulai;
	}

	public void setTanggalMulai(Date tanggalMulai) {
		this.tanggalMulai = tanggalMulai;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggalSelesai() {
		return tanggalSelesai;
	}

	public void setTanggalSelesai(Date tanggalSelesai) {
		this.tanggalSelesai = tanggalSelesai;
	}

	public String getKategori() {
		return kategori;
	}

	public void setKategori(String kategori) {
		this.kategori = kategori;
	}

	public String getPetugas() {
		return petugas;
	}

	public void setPetugas(String petugas) {
		this.petugas = petugas;
	}

	public String getStatus() {
		return status == null ? STATUS_RENCANA : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	public String getOleh() {
		return oleh;
	}

	public void setOleh(String oleh) {
		this.oleh = oleh;
	}

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		this.olehId = olehId;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
