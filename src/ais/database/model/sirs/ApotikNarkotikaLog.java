package ais.database.model.sirs;

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
 * Register penjualan obat terkendali -- narkotika/psikotropika (FASE A, kunci menu
 * {@code apotik_narkotika}; satu-satunya kebutuhan apotik TANPA padanan SIRS existing).
 *
 * <p>APPEND-ONLY: baris ditulis DALAM transaksi penjualan yang sama -- bila register tidak
 * bisa dibuat (data pembeli/dokter kurang), SELURUH penjualan di-rollback (transaksi DITAHAN,
 * bukan dilanjutkan diam-diam). Tidak ada aksi hapus; koreksi = catatan baru bertanda.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "apotik_narkotika_log")
public class ApotikNarkotikaLog extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	private ItemMedis item;
	private TransaksiMedisDetail transaksiDetail;
	private Resep resep;
	private Double qty;
	/** Snapshot golongan saat terjual -- profil item bisa berubah, register tidak. */
	private String golonganObat;
	private String namaPembeli;
	private String alamatPembeli;
	private String namaDokter;
	private String keterangan;
	private Date waktu;

	private String oleh;
	private String olehId;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public ApotikNarkotikaLog() {
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
	@JoinColumn(name = "item", nullable = false)
	public ItemMedis getItem() {
		return item;
	}

	public void setItem(ItemMedis item) {
		this.item = item;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "transaksi_detail", nullable = false)
	public TransaksiMedisDetail getTransaksiDetail() {
		return transaksiDetail;
	}

	public void setTransaksiDetail(TransaksiMedisDetail transaksiDetail) {
		this.transaksiDetail = transaksiDetail;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "resep", nullable = true)
	public Resep getResep() {
		return resep;
	}

	public void setResep(Resep resep) {
		this.resep = resep;
	}

	@Column(name = "qty")
	public Double getQty() {
		return qty == null ? Double.valueOf(0) : qty;
	}

	public void setQty(Double qty) {
		this.qty = qty;
	}

	@Column(name = "golongan_obat", length = 30)
	public String getGolonganObat() {
		return golonganObat;
	}

	public void setGolonganObat(String golonganObat) {
		this.golonganObat = golonganObat;
	}

	@Column(name = "nama_pembeli")
	public String getNamaPembeli() {
		return namaPembeli;
	}

	public void setNamaPembeli(String namaPembeli) {
		this.namaPembeli = namaPembeli;
	}

	@Column(name = "alamat_pembeli", columnDefinition = "text")
	public String getAlamatPembeli() {
		return alamatPembeli;
	}

	public void setAlamatPembeli(String alamatPembeli) {
		this.alamatPembeli = alamatPembeli;
	}

	@Column(name = "nama_dokter")
	public String getNamaDokter() {
		return namaDokter;
	}

	public void setNamaDokter(String namaDokter) {
		this.namaDokter = namaDokter;
	}

	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu")
	public Date getWaktu() {
		return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu;
	}

	public void setWaktu(Date waktu) {
		this.waktu = waktu;
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

	@Column(name = "oleh_id")
	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
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
