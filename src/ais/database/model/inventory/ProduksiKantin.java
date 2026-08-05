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
 * <h2>ProduksiKantin — catatan Produksi Harian menu kantin.</h2>
 *
 * <p>
 * Entity BARU untuk mencatat <b>hasil produksi</b> tiap menu kantin per hari sehingga tersedia laporan
 * katalog §3.5: <i>Rencana Produksi</i>, <i>Realisasi Produksi</i>, dan <i>Sisa Makanan/Waste</i>.
 * Sebelumnya sistem hanya punya penjualan (porsi terjual) dan {@link PemakaianBahanBaku} (pemakaian
 * bahan) namun belum ada konsep <i>porsi dibuat/sisa/dibuang</i>. Dengan pendaftaran di
 * {@code hibernate.cfg.xml}, tabel {@code koperasi.produksi_kantin} otomatis dibuat (hbm2ddl=update).
 * </p>
 *
 * <h3>Kolom & perhitungan</h3>
 * <ul>
 *   <li>{@code porsiRencana} — target porsi yang direncanakan.</li>
 *   <li>{@code porsiDibuat} — porsi yang benar-benar diproduksi.</li>
 *   <li>{@code porsiTerjual} — porsi terjual (boleh diisi manual atau dicocokkan dengan penjualan).</li>
 *   <li>{@code porsiSisa} — sisa yang tidak terjual namun masih layak.</li>
 *   <li>{@code porsiWaste} — porsi terbuang/rusak/basi (kerugian).</li>
 * </ul>
 *
 * <p>
 * Penamaan kolom mengikuti aturan proyek (field tanpa @Column ter-fold ke huruf kecil tanpa underscore,
 * mis. {@code porsiRencana}→{@code porsirencana}). Kompatibel Java 1.7 / Hibernate 3.
 * </p>
 *
 * @author AIS e-Kantin (modul produksi)
 * @see PemakaianBahanBaku
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "produksi_kantin")
public class ProduksiKantin extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	private Produk produk;
	private Toko toko;
	private Date tanggal;
	private Double porsiRencana;
	private Double porsiDibuat;
	private Double porsiTerjual;
	private Double porsiSisa;
	private Double porsiWaste;
	private String keterangan;
	private String oleh;
	private String olehId;

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public ProduksiKantin() {
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
	@JoinColumn(name = "produk")
	public Produk getProduk() {
		produk = check(produk);
		return produk;
	}

	public void setProduk(Produk produk) {
		this.produk = produk;
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

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal() {
		return tanggal == null ? ais.ui.util.WaktuUtil.getDate() : tanggal;
	}

	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	public Double getPorsiRencana() {
		return porsiRencana == null ? 0.0 : porsiRencana;
	}

	public void setPorsiRencana(Double porsiRencana) {
		this.porsiRencana = porsiRencana;
	}

	public Double getPorsiDibuat() {
		return porsiDibuat == null ? 0.0 : porsiDibuat;
	}

	public void setPorsiDibuat(Double porsiDibuat) {
		this.porsiDibuat = porsiDibuat;
	}

	public Double getPorsiTerjual() {
		return porsiTerjual == null ? 0.0 : porsiTerjual;
	}

	public void setPorsiTerjual(Double porsiTerjual) {
		this.porsiTerjual = porsiTerjual;
	}

	public Double getPorsiSisa() {
		return porsiSisa == null ? 0.0 : porsiSisa;
	}

	public void setPorsiSisa(Double porsiSisa) {
		this.porsiSisa = porsiSisa;
	}

	public Double getPorsiWaste() {
		return porsiWaste == null ? 0.0 : porsiWaste;
	}

	public void setPorsiWaste(Double porsiWaste) {
		this.porsiWaste = porsiWaste;
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
