package ais.database.model.hotel;

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

/**
 * Statement pemilik kamar per periode -- LANGKAH 5 MitraInap, padanan
 * {@code hospitality_owner_statement} versi Node. PERBEDAAN DISENGAJA dari Node
 * ({@code statement()} menerima angka mentah dari klien): seluruh angka DIHITUNG
 * SERVER ({@code HotelApiHelper.laporanPemilikGenerate}) dari baris ROOM_CHARGE
 * {@link FolioTransaksi} kamar kontrak pada periode tsb. {@link #getSnapshot()}
 * menyimpan rincian perhitungan (JSON) dan {@link #getDokumenHash()} = SHA-256
 * snapshot -- bukti dokumen tidak berubah setelah terbit. Baris laporan bersifat
 * append-only: koreksi = generate ulang periode (baris lama tetap utk audit).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "hotel_laporan_pemilik")
public class LaporanPemilik extends GeneralValueObject {

	private static final long serialVersionUID = 7268413577548439831L;

	private Long id;
	private KontrakPemilik kontrak;
	private Date periodeMulai;
	private Date periodeSelesai;
	private Double pendapatanKotor;
	private Double komisi;
	private Double biaya;
	private Double bersihDibayarkan;
	private String snapshot;
	private String dokumenHash;
	private String oleh;
	private String olehId;

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public String toString() {
		return (id == null ? "" : id) + "-" + (dokumenHash == null ? "" : dokumenHash);
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "kontrak", nullable = false)
	public KontrakPemilik getKontrak() { kontrak = check(kontrak); return kontrak; }
	public void setKontrak(KontrakPemilik kontrak) { this.kontrak = kontrak; }

	@Temporal(TemporalType.DATE)
	@Column(name = "periode_mulai", nullable = false)
	public Date getPeriodeMulai() { return periodeMulai; }
	public void setPeriodeMulai(Date periodeMulai) { this.periodeMulai = periodeMulai; }

	@Temporal(TemporalType.DATE)
	@Column(name = "periode_selesai", nullable = false)
	public Date getPeriodeSelesai() { return periodeSelesai; }
	public void setPeriodeSelesai(Date periodeSelesai) { this.periodeSelesai = periodeSelesai; }

	@Column(name = "pendapatan_kotor", nullable = false)
	public Double getPendapatanKotor() { return pendapatanKotor; }
	public void setPendapatanKotor(Double pendapatanKotor) { this.pendapatanKotor = pendapatanKotor; }

	@Column(name = "komisi", nullable = false)
	public Double getKomisi() { return komisi; }
	public void setKomisi(Double komisi) { this.komisi = komisi; }

	@Column(name = "biaya", nullable = true)
	public Double getBiaya() { return biaya; }
	public void setBiaya(Double biaya) { this.biaya = biaya; }

	@Column(name = "bersih_dibayarkan", nullable = false)
	public Double getBersihDibayarkan() { return bersihDibayarkan; }
	public void setBersihDibayarkan(Double bersihDibayarkan) { this.bersihDibayarkan = bersihDibayarkan; }

	@Column(name = "snapshot", nullable = true, columnDefinition = "text")
	public String getSnapshot() { return snapshot; }
	public void setSnapshot(String snapshot) { this.snapshot = snapshot; }

	@Column(name = "dokumen_hash", nullable = true, length = 64)
	public String getDokumenHash() { return dokumenHash; }
	public void setDokumenHash(String dokumenHash) { this.dokumenHash = dokumenHash; }

	public String getOleh() { return oleh; }
	public void setOleh(String oleh) { if (oleh == null || oleh.trim().isEmpty()) return; this.oleh = oleh; }
	public String getOlehId() { return olehId; }
	public void setOlehId(String olehId) { if (olehId == null || olehId.trim().isEmpty()) return; this.olehId = olehId; }

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() { return tanggal_dirubah; }
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }
}
