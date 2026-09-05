package ais.database.model.sirs;

import static javax.persistence.GenerationType.IDENTITY;

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

/** Pesanan pengantaran obat dari transaksi apotik sampai bukti diterima. */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "apotik_delivery_order")
public class ApotikDeliveryOrder extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	public static final String MENUNGGU = "MENUNGGU";
	public static final String DISIAPKAN = "DISIAPKAN";
	public static final String DIKIRIM = "DIKIRIM";
	public static final String TERKIRIM = "TERKIRIM";
	public static final String GAGAL = "GAGAL";
	public static final String DIBATALKAN = "DIBATALKAN";

	private Long id;
	private String kode;
	private TransaksiMedis transaksi;
	private String namaPenerima;
	private String telepon;
	private String alamat;
	private String kurir;
	private String layanan;
	private String nomorPelacakan;
	private Double biayaKirim;
	private String status;
	private Date waktuPesan;
	private Date waktuKirim;
	private Date waktuTerima;
	private String buktiTerima;
	private String keterangan;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	@Column(name = "kode", unique = true, nullable = false, length = 60)
	public String getKode() { return kode; }
	public void setKode(String kode) { this.kode = kode; }

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "transaksi")
	public TransaksiMedis getTransaksi() { transaksi = check(transaksi); return transaksi; }
	public void setTransaksi(TransaksiMedis transaksi) { this.transaksi = transaksi; }

	@Column(name = "nama_penerima", nullable = false, length = 160)
	public String getNamaPenerima() { return namaPenerima; }
	public void setNamaPenerima(String namaPenerima) { this.namaPenerima = namaPenerima; }

	@Column(name = "telepon", length = 40)
	public String getTelepon() { return telepon; }
	public void setTelepon(String telepon) { this.telepon = telepon; }

	@Column(name = "alamat", nullable = false, length = 800)
	public String getAlamat() { return alamat; }
	public void setAlamat(String alamat) { this.alamat = alamat; }

	@Column(name = "kurir", length = 120)
	public String getKurir() { return kurir; }
	public void setKurir(String kurir) { this.kurir = kurir; }

	@Column(name = "layanan", length = 80)
	public String getLayanan() { return layanan; }
	public void setLayanan(String layanan) { this.layanan = layanan; }

	@Column(name = "nomor_pelacakan", length = 120)
	public String getNomorPelacakan() { return nomorPelacakan; }
	public void setNomorPelacakan(String nomorPelacakan) { this.nomorPelacakan = nomorPelacakan; }

	@Column(name = "biaya_kirim")
	public Double getBiayaKirim() { return biayaKirim == null ? Double.valueOf(0) : biayaKirim; }
	public void setBiayaKirim(Double biayaKirim) { this.biayaKirim = biayaKirim; }

	@Column(name = "status", nullable = false, length = 20)
	public String getStatus() { return status == null ? MENUNGGU : status; }
	public void setStatus(String status) { this.status = status; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu_pesan", nullable = false)
	public Date getWaktuPesan() { return waktuPesan; }
	public void setWaktuPesan(Date waktuPesan) { this.waktuPesan = waktuPesan; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu_kirim")
	public Date getWaktuKirim() { return waktuKirim; }
	public void setWaktuKirim(Date waktuKirim) { this.waktuKirim = waktuKirim; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu_terima")
	public Date getWaktuTerima() { return waktuTerima; }
	public void setWaktuTerima(Date waktuTerima) { this.waktuTerima = waktuTerima; }

	@Column(name = "bukti_terima", length = 500)
	public String getBuktiTerima() { return buktiTerima; }
	public void setBuktiTerima(String buktiTerima) { this.buktiTerima = buktiTerima; }

	@Column(name = "keterangan", length = 800)
	public String getKeterangan() { return keterangan; }
	public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

	@Column(name = "oleh", length = 60)
	public String getOleh() { return oleh; }
	public void setOleh(String oleh) { this.oleh = oleh; }

	@Column(name = "oleh_id", length = 60)
	public String getOlehId() { return olehId; }
	public void setOlehId(String olehId) { this.olehId = olehId; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dirubah")
	public Date getTanggal_dirubah() { return tanggal_dirubah; }
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }

	@javax.persistence.PreUpdate
	protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }

	public String toString() { return kode == null ? "" : kode; }
}
