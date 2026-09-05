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

	/**
	 * Callback JPA {@code @PreUpdate}: mendelegasikan pencatatan jejak audit ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} setiap kali baris
	 * laporan ini di-UPDATE. Perhatikan: baris laporan dirancang APPEND-ONLY -- koreksi
	 * seharusnya berupa generate ulang periode (baris lama tetap tersimpan untuk audit),
	 * bukan UPDATE pada baris yang sudah terbit. Dipanggil otomatis oleh provider JPA.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Timestamp shadow untuk audit trail; diinisialisasi ke waktu sekarang saat entity
	 * dibuat di memori -- KEHARUSAN TEKNIS pola audit timestamp di seluruh model, bukan bug.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Representasi ringkas untuk log/debug: {@code id-dokumenHash}. */
	public String toString() {
		return (id == null ? "" : id) + "-" + (dokumenHash == null ? "" : dokumenHash);
	}

	/** @return id unik baris laporan pemilik (primary key, auto-generated IDENTITY). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	/** @param id id baris laporan (biasanya diisi Hibernate sendiri, insertable=false). */
	public void setId(Long id) { this.id = id; }

	/**
	 * Kontrak pemilik yang menjadi dasar perhitungan laporan ini.
	 * @return kontrak pemilik terkait; getter diresolusi lewat {@link #check(Object)}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "kontrak", nullable = false)
	public KontrakPemilik getKontrak() { kontrak = check(kontrak); return kontrak; }
	/** @param kontrak kontrak pemilik dasar laporan; wajib diisi (kolom NOT NULL). */
	public void setKontrak(KontrakPemilik kontrak) { this.kontrak = kontrak; }

	/** @return tanggal mulai periode laporan (inklusif). */
	@Temporal(TemporalType.DATE)
	@Column(name = "periode_mulai", nullable = false)
	public Date getPeriodeMulai() { return periodeMulai; }
	/** @param periodeMulai tanggal mulai periode laporan; wajib diisi (kolom NOT NULL). */
	public void setPeriodeMulai(Date periodeMulai) { this.periodeMulai = periodeMulai; }

	/** @return tanggal akhir periode laporan (inklusif). */
	@Temporal(TemporalType.DATE)
	@Column(name = "periode_selesai", nullable = false)
	public Date getPeriodeSelesai() { return periodeSelesai; }
	/** @param periodeSelesai tanggal akhir periode laporan; wajib diisi (kolom NOT NULL). */
	public void setPeriodeSelesai(Date periodeSelesai) { this.periodeSelesai = periodeSelesai; }

	/**
	 * Total pendapatan kotor kamar (SUM baris {@code ROOM_CHARGE} milik kamar kontrak pada
	 * periode ini) -- dihitung server, bukan angka kiriman klien.
	 * @return pendapatan kotor periode ini.
	 */
	@Column(name = "pendapatan_kotor", nullable = false)
	public Double getPendapatanKotor() { return pendapatanKotor; }
	/** @param pendapatanKotor pendapatan kotor periode ini (hasil hitung server). */
	public void setPendapatanKotor(Double pendapatanKotor) { this.pendapatanKotor = pendapatanKotor; }

	/**
	 * Bagian operator: {@code pendapatanKotor * persenKomisi kontrak / 100}.
	 * @return nominal komisi operator periode ini.
	 */
	@Column(name = "komisi", nullable = false)
	public Double getKomisi() { return komisi; }
	/** @param komisi nominal komisi operator periode ini (hasil hitung server). */
	public void setKomisi(Double komisi) { this.komisi = komisi; }

	/**
	 * Biaya tambahan opsional yang dikurangkan dari hak pemilik (mis. biaya perawatan/klaim),
	 * diinput operator saat generate laporan.
	 * @return biaya tambahan periode ini, atau {@code null} bila tidak ada.
	 */
	@Column(name = "biaya", nullable = true)
	public Double getBiaya() { return biaya; }
	/** @param biaya biaya tambahan yang mengurangi hak pemilik. */
	public void setBiaya(Double biaya) { this.biaya = biaya; }

	/**
	 * Hak bersih pemilik: {@code pendapatanKotor - komisi - biaya}.
	 * @return nominal yang harus dibayarkan ke pemilik periode ini.
	 */
	@Column(name = "bersih_dibayarkan", nullable = false)
	public Double getBersihDibayarkan() { return bersihDibayarkan; }
	/** @param bersihDibayarkan nominal bersih yang dibayarkan ke pemilik (hasil hitung server). */
	public void setBersihDibayarkan(Double bersihDibayarkan) { this.bersihDibayarkan = bersihDibayarkan; }

	/**
	 * Rincian perhitungan (JSON, berisi daftar baris ROOM_CHARGE yang menyusun pendapatan
	 * kotor) -- sumber yang di-hash menjadi {@link #getDokumenHash()}.
	 * @return snapshot JSON rincian perhitungan, atau {@code null} bila belum diisi.
	 */
	@Column(name = "snapshot", nullable = true, columnDefinition = "text")
	public String getSnapshot() { return snapshot; }
	/** @param snapshot JSON rincian perhitungan laporan ini. */
	public void setSnapshot(String snapshot) { this.snapshot = snapshot; }

	/**
	 * SHA-256 dari {@link #getSnapshot()} -- bukti dokumen tidak berubah setelah terbit.
	 * @return hash dokumen (hex, 64 karakter), atau {@code null} bila belum diisi.
	 */
	@Column(name = "dokumen_hash", nullable = true, length = 64)
	public String getDokumenHash() { return dokumenHash; }
	/** @param dokumenHash hash SHA-256 snapshot laporan ini. */
	public void setDokumenHash(String dokumenHash) { this.dokumenHash = dokumenHash; }

	/** @return nama aktor yang terakhir membuat/mengubah baris (kolom audit, bukan FK). */
	public String getOleh() { return oleh; }
	/** @param oleh nama aktor; input kosong/blank diabaikan supaya nilai lama tidak tertimpa. */
	public void setOleh(String oleh) { if (oleh == null || oleh.trim().isEmpty()) return; this.oleh = oleh; }
	/** @return id aktor yang terakhir membuat/mengubah baris (kolom audit, bukan FK). */
	public String getOlehId() { return olehId; }
	/** @param olehId id aktor; input kosong/blank diabaikan supaya nilai lama tidak tertimpa. */
	public void setOlehId(String olehId) { if (olehId == null || olehId.trim().isEmpty()) return; this.olehId = olehId; }

	/** @return timestamp shadow terakhir baris ini diubah (diisi otomatis lewat {@link #onUpdate()}). */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() { return tanggal_dirubah; }
	/** @param tanggal_dirubah timestamp perubahan; umumnya tidak perlu diset manual. */
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }
}
