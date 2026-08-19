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

@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "stok_opname")
public class StokOpname extends GeneralValueObject {

	private static final long serialVersionUID = 1L;
	private Long id;
	private Produk produk;
	private Toko toko;

	private Double stokSistem; // Stok di aplikasi saat opname dilakukan
	private Double stokFisik; // Stok nyata yang dihitung karyawan
	private Double selisih; // Fisik dikurang Sistem (Minus = Hilang/Rusak, Plus = Ketemu barang lebih)

	private Date waktuOpname;
	private String keterangan; // Alasan: "Barang Basi", "Hilang dicuri", dll

	private String oleh;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public StokOpname() {
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
	@JoinColumn(name = "produk", nullable = false)
	public Produk getProduk() {
		return produk;
	}

	public void setProduk(Produk produk) {
		this.produk = produk;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko", nullable = false)
	public Toko getToko() {
		return toko;
	}

	public void setToko(Toko toko) {
		this.toko = toko;
	}

	public Double getStokSistem() {
		return stokSistem == null ? 0.0 : stokSistem;
	}

	public void setStokSistem(Double stokSistem) {
		this.stokSistem = stokSistem;
	}

	public Double getStokFisik() {
		return stokFisik == null ? 0.0 : stokFisik;
	}

	public void setStokFisik(Double stokFisik) {
		this.stokFisik = stokFisik;
	}

	public Double getSelisih() {
		selisih = getStokFisik() - getStokSistem();
		return selisih;
	}

	public void setSelisih(Double selisih) {
		this.selisih = selisih;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuOpname() {
		return waktuOpname == null ? ais.ui.util.WaktuUtil.getDate() : waktuOpname;
	}

	public void setWaktuOpname(Date waktuOpname) {
		this.waktuOpname = waktuOpname;
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

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Penanda jurnal. Jurnal selisih opname: Persediaan lawan akun selisih persediaan. Diisi saat baris ini diposting ke buku besar; dipakai
	 * sebagai kunci anti-posting-ganda dan jejak balik dari jurnal ke dokumen sumbernya.
	 * Kolomnya dibuat otomatis oleh Hibernate.
	 */
	private ais.database.model.akunting.PostingHistory postingHistory;

	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "posting_history", nullable = true)
	public ais.database.model.akunting.PostingHistory getPostingHistory() {
		return postingHistory;
	}

	public void setPostingHistory(ais.database.model.akunting.PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

}
