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
import ais.database.model.sirs.Gudang;

/**
 * Pengajuan pembelian/pemesanan stok -- baris "kerja" (work item) yang perlu ditindaklanjuti staf
 * gudang, hasil fitur "Purchase: notifikasi stok minimum otomatis 2 tingkat" (gap analisis PDF
 * klien 2026-07-26). Bisa dibuat OTOMATIS ({@link #otomatis}=true, oleh
 * {@link ais.common.StokThresholdScheduler} saat stok sebuah {@link Produk} di sebuah
 * {@link Gudang} menyentuh {@link AmbangStokGudang}), atau MANUAL oleh staf lewat layar admin.
 *
 * <p><b>Kenapa entity BARU, bukan reuse {@code PermintaanPembelian}/{@code PengadaanProduk} yang
 * sudah ada</b> (riset sebelum implementasi, 2026-07-26): {@code PermintaanPembelian}
 * (schema {@code sirs}) dibangun utk {@code ItemMedis}/farmasi, bukan {@code Produk}/gudang
 * kantin-koperasi; {@code PengadaanProduk} adalah catatan barang MASUK langsung (tanpa status
 * draft/approval) dan tidak punya relasi ke {@code Gudang} sama sekali. Memaksakan salah satunya
 * akan mengaburkan makna field-nya utk pemakai modul lain. Baris di sini murni "antrean kerja"
 * ringan -- setelah staf memprosesnya, pencatatan stok MASUK sungguhan tetap lewat mekanisme yang
 * SUDAH ADA ({@code PengadaanProduk} utk gudang pusat→vendor, {@code PengirimanGudangUtil} utk
 * cabang→pusat) -- entity ini TIDAK menduplikasi logika stok apa pun, murni penanda "perlu
 * ditindaklanjuti".</p>
 *
 * <p><b>Arah pengajuan</b> ditentukan oleh {@link #gudangTujuan}: bila terisi (gudang cabang
 * mengajukan ke gudang induknya), berarti permintaan INTERNAL antar gudang (staf akan
 * menindaklanjuti lewat "Pengiriman Antar Gudang"). Bila {@code null} (gudang tanpa
 * {@code gudangInduk}, artinya sudah di gudang pusat/puncak hierarki), berarti permintaan ke
 * VENDOR EKSTERNAL (staf akan menindaklanjuti lewat layar Pengadaan/Kulakan yang sudah ada).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "pengajuan_pembelian_gudang")
public class PengajuanPembelianGudang extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	/** Baru dibuat, belum ditindaklanjuti sama sekali. */
	public static final String STATUS_BARU = "BARU";
	/** Staf sudah mulai memproses (mis. sudah membuat draft Pengadaan/Pengiriman terkait). */
	public static final String STATUS_DIPROSES = "DIPROSES";
	/** Stok sudah terisi ulang / pengajuan tuntas. */
	public static final String STATUS_SELESAI = "SELESAI";
	/** Dibatalkan (mis. ternyata stok sudah cukup / duplikat). */
	public static final String STATUS_DIBATALKAN = "DIBATALKAN";

	private Long id;
	private String oleh;
	private String olehId;

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	public String getOleh() {
		return oleh;
	}

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public String toString() {
		produk = getProduk();
		return id + "-" + produk + "-" + status;
	}

	private Produk produk;
	private Gudang gudangAsal;
	private Gudang gudangTujuan;
	private Double qtyDiminta;
	private Double stokSaatDiajukan;
	private String status;
	private Boolean otomatis;
	private Date waktuDibuat;
	private String keterangan;
	private Long woId;

	public PengajuanPembelianGudang() {
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk", nullable = false)
	public Produk getProduk() {
		produk = check(produk);
		return produk;
	}

	public void setProduk(Produk produk) {
		this.produk = produk;
	}

	/** Gudang yang stoknya menipis -- sumber pengajuan. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gudang_asal", nullable = false)
	public Gudang getGudangAsal() {
		gudangAsal = check(gudangAsal);
		return gudangAsal;
	}

	public void setGudangAsal(Gudang gudangAsal) {
		this.gudangAsal = gudangAsal;
	}

	/** {@code null} = ke vendor eksternal (gudang asal sudah di puncak hierarki). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gudang_tujuan", nullable = true)
	public Gudang getGudangTujuan() {
		gudangTujuan = check(gudangTujuan);
		return gudangTujuan;
	}

	public void setGudangTujuan(Gudang gudangTujuan) {
		this.gudangTujuan = gudangTujuan;
	}

	@Column(name = "qty_diminta", nullable = true)
	public Double getQtyDiminta() {
		return qtyDiminta;
	}

	public void setQtyDiminta(Double qtyDiminta) {
		this.qtyDiminta = qtyDiminta;
	}

	/** Stok yang terekam SAAT pengajuan dibuat -- arsip/bukti, bukan nilai live. */
	@Column(name = "stok_saat_diajukan", nullable = true)
	public Double getStokSaatDiajukan() {
		return stokSaatDiajukan;
	}

	public void setStokSaatDiajukan(Double stokSaatDiajukan) {
		this.stokSaatDiajukan = stokSaatDiajukan;
	}

	@Column(name = "status", nullable = false, length = 20)
	public String getStatus() {
		return status == null ? STATUS_BARU : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Boolean getOtomatis() {
		return otomatis == null ? false : otomatis;
	}

	public void setOtomatis(Boolean otomatis) {
		this.otomatis = otomatis;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu_dibuat", nullable = true)
	public Date getWaktuDibuat() {
		return waktuDibuat;
	}

	public void setWaktuDibuat(Date waktuDibuat) {
		this.waktuDibuat = waktuDibuat;
	}

	/**
	 * Id {@code ProduksiDokumen} WO pemicu (Fase D dok. 48 P4): pengajuan yang lahir dari
	 * kekurangan komponen saat rilis Work Order. {@code null} = pengajuan biasa (ambang stok
	 * atau manual) -- data lama tidak berubah makna.
	 */
	@Column(name = "wo_id", nullable = true)
	public Long getWoId() {
		return woId;
	}

	public void setWoId(Long woId) {
		this.woId = woId;
	}

	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}
}
