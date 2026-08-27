package ais.database.model.koperasi;

import java.util.Date;

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
import ais.database.model.Tbmuser;

/**
 * Persetujuan satu-kali untuk transaksi member yang melampaui limit periodik
 * Tipe Member. Kode transaksi mengikat persetujuan ke satu checkout sehingga
 * keputusan tidak dapat dipakai untuk transaksi lain.
 */
@Entity
@Audited
@Table(schema = "koperasi", name = "pengajuan_limit_transaksi_member")
public class PengajuanLimitTransaksiMember extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	public static final String MENUNGGU = "MENUNGGU";
	public static final String DISETUJUI = "DISETUJUI";
	public static final String DITOLAK = "DITOLAK";
	public static final String DIPAKAI = "DIPAKAI";

	private Long id;
	private AnggotaKoperasi anggotaKoperasi;
	private TipeAnggotaKoperasi tipeAnggotaKoperasi;
	private String kodeTransaksi;
	private Double nominalTransaksi;
	private String periodeLimit;
	private Double limitTransaksi;
	private Double pemakaianBerjalan;
	private String status = MENUNGGU;
	private Tbmuser diajukanOleh;
	private Tbmuser diputuskanOleh;
	private Date tanggalPengajuan = ais.ui.util.WaktuUtil.getDate();
	private Date tanggalKeputusan;
	private Date tanggalDipakai;
	private PembelianAnggotaKoperasi pembelianAnggotaKoperasi;
	private String catatan;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	@Id
	@GeneratedValue(strategy = javax.persistence.GenerationType.IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "anggota_koperasi", nullable = false)
	public AnggotaKoperasi getAnggotaKoperasi() { return anggotaKoperasi; }
	public void setAnggotaKoperasi(AnggotaKoperasi value) { this.anggotaKoperasi = value; }

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tipe_anggota_koperasi")
	public TipeAnggotaKoperasi getTipeAnggotaKoperasi() { return tipeAnggotaKoperasi; }
	public void setTipeAnggotaKoperasi(TipeAnggotaKoperasi value) { this.tipeAnggotaKoperasi = value; }

	@Column(name = "kode_transaksi", nullable = false, unique = true, length = 100)
	public String getKodeTransaksi() { return kodeTransaksi; }
	public void setKodeTransaksi(String value) { this.kodeTransaksi = value; }

	@Column(name = "nominal_transaksi", nullable = false)
	public Double getNominalTransaksi() { return nominalTransaksi; }
	public void setNominalTransaksi(Double value) { this.nominalTransaksi = value; }

	@Column(name = "periode_limit", nullable = false, length = 20)
	public String getPeriodeLimit() { return periodeLimit; }
	public void setPeriodeLimit(String value) { this.periodeLimit = value; }

	@Column(name = "limit_transaksi", nullable = false)
	public Double getLimitTransaksi() { return limitTransaksi; }
	public void setLimitTransaksi(Double value) { this.limitTransaksi = value; }

	@Column(name = "pemakaian_berjalan", nullable = false)
	public Double getPemakaianBerjalan() { return pemakaianBerjalan; }
	public void setPemakaianBerjalan(Double value) { this.pemakaianBerjalan = value; }

	@Column(name = "status", nullable = false, length = 20)
	public String getStatus() { return status; }
	public void setStatus(String value) { this.status = value; }

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "diajukan_oleh")
	public Tbmuser getDiajukanOleh() { return diajukanOleh; }
	public void setDiajukanOleh(Tbmuser value) { this.diajukanOleh = value; }

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "diputuskan_oleh")
	public Tbmuser getDiputuskanOleh() { return diputuskanOleh; }
	public void setDiputuskanOleh(Tbmuser value) { this.diputuskanOleh = value; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pengajuan", nullable = false)
	public Date getTanggalPengajuan() { return tanggalPengajuan; }
	public void setTanggalPengajuan(Date value) { this.tanggalPengajuan = value; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_keputusan")
	public Date getTanggalKeputusan() { return tanggalKeputusan; }
	public void setTanggalKeputusan(Date value) { this.tanggalKeputusan = value; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dipakai")
	public Date getTanggalDipakai() { return tanggalDipakai; }
	public void setTanggalDipakai(Date value) { this.tanggalDipakai = value; }

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pembelian_anggota_koperasi")
	public PembelianAnggotaKoperasi getPembelianAnggotaKoperasi() { return pembelianAnggotaKoperasi; }
	public void setPembelianAnggotaKoperasi(PembelianAnggotaKoperasi value) { this.pembelianAnggotaKoperasi = value; }

	@Column(name = "catatan", length = 1000)
	public String getCatatan() { return catatan; }
	public void setCatatan(String value) { this.catatan = value; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dirubah")
	public Date getTanggal_dirubah() { return tanggal_dirubah; }
	public void setTanggal_dirubah(Date value) { this.tanggal_dirubah = value; }

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
	}
}
