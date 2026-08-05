package ais.database.model.asset;

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
 * <h2>PengirimanGudang — dokumen kepala pengiriman antar {@link Lokasi} (Gudang Pusat ↔ Cabang/Outlet).</h2>
 *
 * <p>
 * Melengkapi {@code StokLokasiUtil.catatTransfer} (transfer instan-atomik, tanpa jeda waktu) dengan
 * alur <b>kirim → dalam perjalanan → terima</b> yang mencerminkan kenyataan fisik: barang butuh waktu
 * tempuh, dan stok di lokasi tujuan seharusnya baru bertambah SETELAH penerima mengonfirmasi barang
 * benar-benar diterima (boleh penuh, boleh sebagian) — bukan seketika saat pengiriman dibuat.
 * </p>
 *
 * <h3>Desain: lokasi transit virtual, BUKAN kolom status baru di {@link MutasiLokasi}</h3>
 * <p>
 * Entity ini SENGAJA tidak menambah kolom status ke {@code MutasiLokasi} (berisiko terhadap seluruh
 * laporan yang sudah bergantung pada {@code SUM(qty)} polos). Sebagai gantinya, setiap
 * {@code PengirimanGudang} memiliki SATU {@link #getLokasiTransit() lokasi transit} virtual
 * (dibuat otomatis, {@code aktif=false} agar tak muncul di picker manual mana pun) yang berperan
 * sebagai "gudang singgah": saat dikirim, barang dicatat KELUAR dari {@link #getLokasiAsal()} dan
 * MASUK ke lokasi transit (dua baris {@code MutasiLokasi} riil, lewat
 * {@code StokLokasiUtil.catatKeluar}/{@code catatMasuk} yang SUDAH ADA, tanpa perubahan apa pun ke
 * kelas itu). Saat diterima, barang dicatat KELUAR dari lokasi transit dan MASUK ke
 * {@link #getLokasiTujuan()} — hanya sejumlah yang benar-benar dikonfirmasi diterima
 * ({@code qtyTerima} di {@link PengirimanGudangDetail}, boleh kurang dari {@code qtyKirim} untuk
 * penerimaan sebagian). Sisa yang belum diterima tetap "mengendap" di lokasi transit sebagai jejak
 * audit yang terlihat pada laporan pergudangan — TIDAK dikoreksi otomatis (selaras prinsip stock
 * opname: temuan selisih perlu ditindaklanjuti, bukan langsung dikoreksi diam-diam).
 * </p>
 *
 * <h3>Status dokumen</h3>
 * <p>
 * {@link #getStatus() status} murni penanda tampilan/alur kerja pada level DOKUMEN (bukan pada baris
 * {@code MutasiLokasi} mana pun): {@link #DIKIRIM}, {@link #DITERIMA}, {@link #DITERIMA_SEBAGIAN},
 * {@link #DIBATALKAN}. Tidak memengaruhi perhitungan stok — itu murni hasil {@code SUM(qty)} atas
 * {@code MutasiLokasi} seperti biasa.
 * </p>
 *
 * <h3>Cakupan yang SENGAJA di luar entity ini</h3>
 * <p>
 * Tidak menyentuh {@code koperasi.Produk.stok}, {@code KantinHelper}, atau alur checkout POS sama
 * sekali — murni pergerakan stok internal gudang/outlet di skema {@code asset}, terpisah total dari
 * jalur penjualan POS (lihat catatan arsitektur pada dokumen analisis sistem terpadu).
 * </p>
 *
 * @author AIS e-Kantin (modul pergudangan)
 * @see PengirimanGudangDetail
 * @see ais.action.master.inventory.PengirimanGudangUtil
 * @see ais.action.master.inventory.StokLokasiUtil
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "pengiriman_gudang")
public class PengirimanGudang extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	/** Dikirim, menunggu konfirmasi terima (barang berada di lokasi transit). */
	public static final String DIKIRIM = "DIKIRIM";
	/** Seluruh baris diterima penuh (qtyTerima == qtyKirim di semua baris). */
	public static final String DITERIMA = "DITERIMA";
	/** Sebagian baris/qty diterima; sisanya masih mengendap di lokasi transit. */
	public static final String DITERIMA_SEBAGIAN = "DITERIMA_SEBAGIAN";
	/** Dibatalkan sebelum diterima (barang dikembalikan manual ke asal, di luar cakupan otomatis). */
	public static final String DIBATALKAN = "DIBATALKAN";

	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	private String kode;
	private Lokasi lokasiAsal;
	private Lokasi lokasiTujuan;
	private Lokasi lokasiTransit;
	private Date tanggalKirim = ais.ui.util.WaktuUtil.getDate();
	private Date tanggalTerima;
	private String status;
	private String keterangan;
	private String keteranganTerima;
	private String dikirimOleh;
	private String dikirimOlehId;
	private String diterimaOleh;
	private String diterimaOlehId;

	public PengirimanGudang() {
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dirubah")
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return kode dokumen unik (format {@code "KRM-" + barcode}), dipakai juga sbg {@code referensi} baris MutasiLokasi terkait. */
	@Column(name = "kode", length = 64, unique = true)
	public String getKode() {
		return kode;
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	/** @return lokasi asal (mis. Gudang Pusat). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi_asal", nullable = false)
	public Lokasi getLokasiAsal() {
		lokasiAsal = check(lokasiAsal);
		return lokasiAsal;
	}

	public void setLokasiAsal(Lokasi lokasiAsal) {
		this.lokasiAsal = lokasiAsal;
	}

	/** @return lokasi tujuan (mis. Cabang/Outlet). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi_tujuan", nullable = false)
	public Lokasi getLokasiTujuan() {
		lokasiTujuan = check(lokasiTujuan);
		return lokasiTujuan;
	}

	public void setLokasiTujuan(Lokasi lokasiTujuan) {
		this.lokasiTujuan = lokasiTujuan;
	}

	/** @return lokasi transit virtual milik pengiriman ini (dibuat otomatis, satu per dokumen). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "lokasi_transit", nullable = false)
	public Lokasi getLokasiTransit() {
		lokasiTransit = check(lokasiTransit);
		return lokasiTransit;
	}

	public void setLokasiTransit(Lokasi lokasiTransit) {
		this.lokasiTransit = lokasiTransit;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_kirim")
	public Date getTanggalKirim() {
		return tanggalKirim;
	}

	public void setTanggalKirim(Date tanggalKirim) {
		this.tanggalKirim = tanggalKirim;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_terima")
	public Date getTanggalTerima() {
		return tanggalTerima;
	}

	public void setTanggalTerima(Date tanggalTerima) {
		this.tanggalTerima = tanggalTerima;
	}

	/** @return status dokumen ({@link #DIKIRIM}/{@link #DITERIMA}/{@link #DITERIMA_SEBAGIAN}/{@link #DIBATALKAN}). */
	@Column(name = "status", length = 32)
	public String getStatus() {
		return status == null ? DIKIRIM : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@Column(name = "keterangan_terima", columnDefinition = "text")
	public String getKeteranganTerima() {
		return keteranganTerima;
	}

	public void setKeteranganTerima(String keteranganTerima) {
		this.keteranganTerima = keteranganTerima;
	}

	@Column(name = "dikirim_oleh", length = 255)
	public String getDikirimOleh() {
		return dikirimOleh;
	}

	public void setDikirimOleh(String dikirimOleh) {
		this.dikirimOleh = dikirimOleh;
	}

	@Column(name = "dikirim_oleh_id", length = 64)
	public String getDikirimOlehId() {
		return dikirimOlehId;
	}

	public void setDikirimOlehId(String dikirimOlehId) {
		this.dikirimOlehId = dikirimOlehId;
	}

	@Column(name = "diterima_oleh", length = 255)
	public String getDiterimaOleh() {
		return diterimaOleh;
	}

	public void setDiterimaOleh(String diterimaOleh) {
		this.diterimaOleh = diterimaOleh;
	}

	@Column(name = "diterima_oleh_id", length = 64)
	public String getDiterimaOlehId() {
		return diterimaOlehId;
	}

	public void setDiterimaOlehId(String diterimaOlehId) {
		this.diterimaOlehId = diterimaOlehId;
	}
}
