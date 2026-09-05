package ais.database.model.finpay;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;




import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;



import ais.common.Common;
import ais.database.model.DetailBiaya;
import ais.database.model.GeneralValueObject;
import ais.database.model.ItemBiaya;
import ais.database.model.PengaturanPembayaranBulanan;

/**
 * Entity Hibernate untuk satu baris rincian item/cicilan yang tercakup dalam sebuah
 * {@link FinpayRequest}, dipetakan ke tabel {@code finpay_request_detail}. Dibangkitkan oleh hbm2java,
 * lalu dilengkapi logika turunan (fallback {@link #getKeterangan()}) secara manual.
 *
 * <p>Kelas ini adalah kelas <i>rincian item</i> pada pola 4-kelas per payment gateway yang berulang
 * identik di seluruh integrasi H2H AIS (lihat {@link FinpayRequest} untuk penjelasan pola lengkap).
 * Satu {@link FinpayRequest} dapat memiliki banyak baris {@code FinpayRequestDetail}, masing-masing
 * mewakili satu item biaya/cicilan yang dibayar dalam transaksi tersebut, dikaitkan opsional ke
 * {@link PengaturanPembayaranBulanan} (cicilan bulanan) dan/atau {@link ItemBiaya}/{@link DetailBiaya}
 * (item biaya non-cicilan).</p>
 *
 * <p><b>Catatan keamanan:</b> tidak ada field kartu/PIN/password/token di kelas ini. Entity ini tidak
 * memiliki field kepemilikan/tenant eksplisit -- kepemilikan mengikuti {@link #getFinpayRequest()}.</p>
 *
 * @see FinpayRequest
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "finpay_request_detail")



public class FinpayRequestDetail extends GeneralValueObject {
	/** 
	 * 
	 */
	private static final long serialVersionUID = 2463821327548439808L;
	private Long id;
	/** Nama pengguna (username) yang membuat/terakhir menyentuh baris audit ini. */
	private String oleh;
	/** Id pengguna yang membuat/terakhir menyentuh baris audit ini; pasangan dari {@link #oleh}. */
	private String olehId;

	/**
	 * @return id pengguna (audit) yang tercatat pada baris ini, sebagaimana adanya (tanpa fallback).
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengeset id pengguna audit. Nilai {@code null} atau string kosong/blank diabaikan (fail-safe)
	 * agar id pengguna audit yang sudah tersimpan tidak tertimpa nilai kosong.
	 * @param olehId id pengguna audit baru; diabaikan jika null/kosong.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengeset nama pengguna audit ({@link #oleh}). Nilai {@code null} atau kosong/blank diabaikan
	 * (fail-safe) supaya nama pengguna audit yang sudah tersimpan tidak tertimpa nilai kosong.
	 * @param oleh nama pengguna audit baru; diabaikan jika null/kosong.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna (audit) yang tercatat pada baris ini, sebagaimana adanya (tanpa fallback).
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: memperbarui {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} setiap kali baris ini
	 * di-{@code UPDATE}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah timestamp "terakhir diubah" baru untuk baris ini.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return timestamp terakhir baris ini diubah (kolom audit, diperbarui otomatis oleh
	 * {@link #onUpdate()} pada setiap {@code UPDATE}).
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return representasi ringkas entity ini untuk keperluan log/debug: gabungan id, request induk,
	 * item biaya, pengaturan pembayaran bulanan, nilai, tanggal, cicilan ke berapa, dan keterangan.
	 */
	public String toString() {
		return id + "-" + finpayRequest + "-" + itemBiaya + "-" + pengaturanPembayaranBulanan + "-" + nilai + "-"
				+ tanggal + "-" + ke + "-" + keterangan;
	}

	/** Request Finpay induk yang memiliki rincian ini. */
	private FinpayRequest finpayRequest;
	/** Pengaturan pembayaran bulanan (cicilan) yang dibayar oleh baris rincian ini, jika ada. */
	private PengaturanPembayaranBulanan pengaturanPembayaranBulanan;
	/** Item biaya non-cicilan yang dibayar oleh baris rincian ini, jika ada. */
	private ItemBiaya itemBiaya;
	/** Nominal yang dibayarkan pada baris rincian ini. */
	private Double nilai;
	/** Keterangan baris rincian; jika kosong akan diisi otomatis oleh {@link #getKeterangan()}. */
	private String keterangan;
	/** Tanggal transaksi/jatuh tempo terkait baris rincian ini. */
	private Date tanggal;
	/** Nomor urut cicilan ke berapa yang dibayar oleh baris rincian ini. */
	private Integer ke;
	/** Id baris {@code CicilanPembayaran} yang dirujuk/dilunasi oleh baris rincian ini. */
	private Long idCicilan;
	/** Nominal denda/penalti yang turut ditagihkan pada baris rincian ini. */
	private Double denda;
	/** Nilai asli tagihan sebelum penyesuaian/pengurangan, untuk keperluan audit. */
	private Double nilaiAsli;
	/** Detail biaya (skema/tarif) acuan nominal baris rincian ini, jika ada. */
	private DetailBiaya detailBiaya;

	/**
	 * Konstruktor default (dipakai Hibernate).
	 */
	public FinpayRequestDetail() {
	}

	/**
	 * @return id unik (primary key, auto-increment) baris {@code finpay_request_detail} ini.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id id baris ini; normalnya tidak diset manual karena kolom bersifat
	 * {@code insertable = false} (auto-increment oleh database).
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return request Finpay induk ({@link FinpayRequest}) yang memiliki rincian ini; tidak pernah
	 * {@code null} untuk baris yang tersimpan (kolom {@code finpay_request} bersifat
	 * {@code nullable = false}).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "finpay_request", nullable = false)
	public FinpayRequest getFinpayRequest() {
		return finpayRequest;
	}

	/**
	 * @param finpayRequest request Finpay induk yang baru untuk baris rincian ini.
	 */
	public void setFinpayRequest(FinpayRequest finpayRequest) {
		this.finpayRequest = finpayRequest;
	}

	/**
	 * @return pengaturan pembayaran bulanan (cicilan) yang dibayar baris rincian ini, atau {@code null}
	 * jika baris ini membayar item biaya non-cicilan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pengaturan_pembayaran_bulanan", nullable = true)
	public PengaturanPembayaranBulanan getPengaturanPembayaranBulanan() {
		return pengaturanPembayaranBulanan;
	}

	/**
	 * @param pengaturanPembayaranBulanan pengaturan pembayaran bulanan yang baru untuk baris rincian ini.
	 */
	public void setPengaturanPembayaranBulanan(PengaturanPembayaranBulanan pengaturanPembayaranBulanan) {
		this.pengaturanPembayaranBulanan = pengaturanPembayaranBulanan;
	}

	/**
	 * @return item biaya non-cicilan yang dibayar baris rincian ini, atau {@code null} jika baris ini
	 * membayar cicilan bulanan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "item_biaya", nullable = true)
	public ItemBiaya getItemBiaya() {
		return itemBiaya;
	}

	/**
	 * @param itemBiaya item biaya yang baru untuk baris rincian ini.
	 */
	public void setItemBiaya(ItemBiaya itemBiaya) {
		this.itemBiaya = itemBiaya;
	}

	/**
	 * @return nominal yang dibayarkan pada baris rincian ini; {@code 0.0} (bukan {@code null}) jika
	 * belum pernah diset -- pemanggilan getter ini juga melakukan lazy-init pada field {@link #nilai}.
	 */
	public Double getNilai() {
		if (nilai == null) {
			nilai = 0.0;
		}
		return nilai;
	}

	/**
	 * @param nilai nominal yang baru untuk baris rincian ini.
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * @return keterangan baris rincian ini. Jika belum diset (null/kosong), method ini melakukan
	 * lazy-generate: menyusun keterangan dari {@link #pengaturanPembayaranBulanan} (kode+nama item
	 * biaya, nama bulan, nominal setelah modifikasi) jika ada, atau dari {@link #itemBiaya} (kode+nama
	 * item biaya, nominal) sebagai fallback kedua. Kegagalan lazy-generate (mis.
	 * {@code LazyInitializationException} karena proxy Hibernate dari sesi lain yang sudah tertutup)
	 * ditangkap dan dicatat lewat {@code ErrorAuditUtil} tanpa membuat getter ini gagal/melempar.
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		if ((keterangan == null || keterangan.trim().isEmpty())) {
			if (pengaturanPembayaranBulanan != null) {
				try {
					// FIX LazyInitializationException: pengaturanPembayaranBulanan/detailBiaya bisa berupa
					// instance canonical/shared (AuditTimestampInterceptor) yang proxy-nya terikat ke Session
					// lain yang sudah closed -> jangan biarkan getter ini crash, kembalikan fallback aman.
					keterangan = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getKode() + "-"
							+ pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + ", bulan "
							+ pengaturanPembayaranBulanan.getNamaBulan() + " " + ", nominal Rp. "
							+ Common.numberFormat.get().format(pengaturanPembayaranBulanan.ambilNominalModifikasi(
									finpayRequest == null || finpayRequest.getJenisKegiatan() == null ? null
											: finpayRequest.getMahasiswa(),
									finpayRequest == null || finpayRequest.getJenisKegiatan() == null ? null
											: finpayRequest.getSemester()));
				} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/finpay/FinpayRequestDetail.java:getKeterangan-lazy");
				}
			} else if (itemBiaya != null && getNilai() != null) {
				keterangan = itemBiaya.getKode() + "-" + itemBiaya.getNama() + ", nominal Rp. "
						+ Common.numberFormat.get().format(getNilai());
			}
		}
		return keterangan;
	}

	/**
	 * @param keterangan keterangan baris rincian yang baru; mengeset nilai eksplisit menonaktifkan
	 * lazy-generate pada {@link #getKeterangan()} selama nilai ini tidak kosong.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return tanggal transaksi/jatuh tempo terkait baris rincian ini.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal() {
		return tanggal;
	}

	/**
	 * @param tanggal tanggal transaksi/jatuh tempo yang baru.
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * @return nomor urut cicilan ke berapa yang dibayar baris rincian ini, sebagaimana adanya (tanpa
	 * fallback).
	 */
	public Integer getKe() {
		return ke;
	}

	/**
	 * @param ke nomor urut cicilan yang baru.
	 */
	public void setKe(Integer ke) {
		this.ke = ke;
	}

	/**
	 * @return id baris {@code CicilanPembayaran} yang dirujuk/dilunasi baris rincian ini, sebagaimana
	 * adanya (tanpa fallback).
	 */
	public Long getIdCicilan() {
		return idCicilan;
	}

	/**
	 * @param idCicilan id cicilan pembayaran yang baru.
	 */
	public void setIdCicilan(Long idCicilan) {
		this.idCicilan = idCicilan;
	}

	/**
	 * @return nominal denda/penalti yang turut ditagihkan pada baris rincian ini, sebagaimana adanya
	 * (tanpa fallback).
	 */
	public Double getDenda() {
		return denda;
	}

	/**
	 * @param denda nominal denda/penalti yang baru.
	 */
	public void setDenda(Double denda) {
		this.denda = denda;
	}

	/**
	 * @return nilai asli tagihan sebelum penyesuaian/pengurangan, sebagaimana adanya (tanpa fallback).
	 */
	public Double getNilaiAsli() {
		return nilaiAsli;
	}

	/**
	 * @param nilaiAsli nilai asli tagihan yang baru sebelum penyesuaian.
	 */
	public void setNilaiAsli(Double nilaiAsli) {
		this.nilaiAsli = nilaiAsli;
	}


	/**
	 * @return detail biaya (skema/tarif) acuan nominal baris rincian ini, atau {@code null} jika baris
	 * ini tidak mengacu ke {@code DetailBiaya} tertentu.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "detail_biaya", nullable = true)
	public DetailBiaya getDetailBiaya() {
		return detailBiaya;
	}

	/**
	 * @param detailBiaya detail biaya (skema/tarif) yang baru untuk baris rincian ini.
	 */
	public void setDetailBiaya(DetailBiaya detailBiaya) {
		this.detailBiaya = detailBiaya;
	}
}
