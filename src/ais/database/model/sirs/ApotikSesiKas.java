package ais.database.model.sirs;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Sesi kas (shift) kasir apotek — IR-06.
 *
 * <p><b>Mengapa TIDAK memakai ulang {@code sesi_kas_*} POS umum.</b> Laporan
 * tutup kas POS umum ({@code ais.action.master.koperasi.helper.SesiKasUtil})
 * menghitung uang dari {@code koperasi.pembelian_anggota_koperasi}. Penjualan
 * apotek tidak pernah ditulis ke sana: jejaknya di
 * {@code sirs.detail_transaksi_pasien} (kode transaksi {@code AJ}) dan
 * pembayarannya di {@code sirs.apotik_pembayaran_transaksi}. Memakainya apa
 * adanya akan melaporkan penjualan tunai apotek sebesar NOL dan memunculkan
 * selisih kas sebesar seluruh penerimaan hari itu — angka yang salah, bukan
 * sekadar kurang lengkap.</p>
 *
 * <p>Tabel BARU sehingga {@code hbm2ddl=update} membuatnya berikut tabel
 * auditnya; tidak ada migrasi manual untuk entity ini.</p>
 *
 * <p><b>Angka dihitung server.</b> {@link #getTotalTunaiSistem()} dan
 * {@link #getSelisih()} diisi oleh {@code ApotikSesiKasHelper} dari data
 * pembayaran, BUKAN dari nilai kiriman klien. Yang boleh datang dari kasir
 * hanyalah modal awal dan hasil hitungan fisik laci; kalau angka sistem pun
 * boleh dikirim klien, rekonsiliasi berhenti menjadi pemeriksaan.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "apotik_sesi_kas")
public class ApotikSesiKas extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	public static final String STATUS_BUKA = "BUKA";
	public static final String STATUS_TUTUP = "TUTUP";

	private Long id;

	/** Akun kasir pemegang sesi. */
	private String userId;
	private String namaKasir;
	private String status;

	private Date waktuBuka;
	private Date waktuTutup;

	private Double modalAwal;

	/** Hasil hitungan fisik laci saat tutup — satu-satunya angka uang dari kasir. */
	private Double uangFisik;

	/** Penerimaan TUNAI menurut catatan pembayaran, dihitung server saat tutup. */
	private Double totalTunaiSistem;

	/** Penerimaan non-tunai pada periode yang sama (tidak masuk laci). */
	private Double totalNonTunaiSistem;

	/** uangFisik - (modalAwal + totalTunaiSistem). Negatif berarti kurang. */
	private Double selisih;

	private String keterangan;
	private String oleh;
	private String olehId;

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public String toString() {
		return (id == null ? "" : id) + "-" + (status == null ? "" : status);
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	@Column(name = "user_id", length = 60)
	public String getUserId() { return userId; }
	public void setUserId(String userId) { this.userId = userId; }

	@Column(name = "nama_kasir", length = 120)
	public String getNamaKasir() { return namaKasir; }
	public void setNamaKasir(String namaKasir) { this.namaKasir = namaKasir; }

	@Column(name = "status", length = 12, nullable = false)
	public String getStatus() { return status == null ? STATUS_BUKA : status; }
	public void setStatus(String status) { this.status = status; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu_buka", nullable = false)
	public Date getWaktuBuka() { return waktuBuka; }
	public void setWaktuBuka(Date waktuBuka) { this.waktuBuka = waktuBuka; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu_tutup")
	public Date getWaktuTutup() { return waktuTutup; }
	public void setWaktuTutup(Date waktuTutup) { this.waktuTutup = waktuTutup; }

	@Column(name = "modal_awal")
	public Double getModalAwal() { return modalAwal == null ? Double.valueOf(0) : modalAwal; }
	public void setModalAwal(Double modalAwal) { this.modalAwal = modalAwal; }

	@Column(name = "uang_fisik")
	public Double getUangFisik() { return uangFisik; }
	public void setUangFisik(Double uangFisik) { this.uangFisik = uangFisik; }

	@Column(name = "total_tunai_sistem")
	public Double getTotalTunaiSistem() { return totalTunaiSistem; }
	public void setTotalTunaiSistem(Double totalTunaiSistem) { this.totalTunaiSistem = totalTunaiSistem; }

	@Column(name = "total_non_tunai_sistem")
	public Double getTotalNonTunaiSistem() { return totalNonTunaiSistem; }
	public void setTotalNonTunaiSistem(Double totalNonTunaiSistem) { this.totalNonTunaiSistem = totalNonTunaiSistem; }

	@Column(name = "selisih")
	public Double getSelisih() { return selisih; }
	public void setSelisih(Double selisih) { this.selisih = selisih; }

	@Column(name = "keterangan", length = 500)
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
}
