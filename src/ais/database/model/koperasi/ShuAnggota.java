package ais.database.model.koperasi;

// Bagian dari sub-modul Simpan Pinjam (USP) Koperasi — fitur pembagian SHU.

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

/**
 * <h2>ShuAnggota — Rincian Bagian SHU per Anggota</h2>
 *
 * <p>
 * Entity anak dari {@link PembagianShu} ini menyimpan <b>hak SHU setiap anggota</b> pada satu tahun
 * buku. Satu baris pada tabel <code>koperasi.shu_anggota</code> memuat dasar perhitungan (total
 * simpanan sebagai basis jasa modal dan total partisipasi/jasa sebagai basis jasa usaha), nilai
 * jasa modal, nilai jasa usaha, total SHU anggota, serta status pembayarannya.
 * </p>
 *
 * <h3>Prinsip perhitungan (adil &amp; sebanding)</h3>
 * <p>
 * Sesuai UU Perkoperasian dan SOM USPK, SHU dibagikan secara adil sebanding dengan jasa usaha dan
 * jasa modal masing-masing anggota. Bagian anggota dihitung proporsional oleh sistem:
 * <em>jasa modal anggota = (simpanan anggota / total simpanan seluruh anggota) × nominal jasa
 * modal</em>, dan <em>jasa usaha anggota = (partisipasi anggota / total partisipasi seluruh anggota)
 * × nominal jasa usaha</em>. Basis {@link #getTotalSimpanan()} dan {@link #getTotalTransaksi()}
 * disimpan agar perhitungan dapat diaudit ulang kapan pun.
 * </p>
 *
 * <h3>Catatan desain</h3>
 * <p>
 * <b>Tidak {@code @Audited}</b>: baris rincian bervolume besar (satu per anggota per tahun) dan
 * sudah tertaut ke {@link PembagianShu} yang teraudit; menghindari tabel <code>_aud</code>
 * menghemat ruang. Relasi lazy dengan {@code check(...)}, getter numerik null-safe, kompatibel
 * Java 1.7.
 * </p>
 *
 * @see PembagianShu
 * @see AnggotaKoperasi
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "koperasi", name = "shu_anggota")
public class ShuAnggota extends ais.database.model.GeneralValueObject {

	private static final long serialVersionUID = 7742100014412002002L;

	private Long id;
	private String oleh;
	private String olehId;

	private PembagianShu pembagianShu;
	private AnggotaKoperasi anggota;
	private Double totalSimpanan = 0.0;
	private Double totalTransaksi = 0.0;
	private Double jasaModal = 0.0;
	private Double jasaUsaha = 0.0;
	private Double totalShu = 0.0;
	private Boolean sudahDibayar = false;
	private Date tanggalBayar;

	public ShuAnggota() {
	}

	public ShuAnggota(Long id) {
		this.id = id;
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

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pembagian_shu", nullable = true)
	public PembagianShu getPembagianShu() {
		pembagianShu = check(pembagianShu);
		return pembagianShu;
	}

	public void setPembagianShu(PembagianShu pembagianShu) {
		this.pembagianShu = pembagianShu == null || pembagianShu.getId() == null ? null : pembagianShu;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "anggota", nullable = true)
	public AnggotaKoperasi getAnggota() {
		anggota = check(anggota);
		return anggota;
	}

	public void setAnggota(AnggotaKoperasi anggota) {
		this.anggota = anggota == null || anggota.getId() == null ? null : anggota;
	}

	/** Total simpanan anggota — basis proporsi jasa modal. */
	@Column(name = "total_simpanan")
	public Double getTotalSimpanan() {
		return totalSimpanan == null ? 0.0 : totalSimpanan;
	}

	public void setTotalSimpanan(Double totalSimpanan) {
		this.totalSimpanan = totalSimpanan;
	}

	/** Total partisipasi/jasa anggota (mis. bunga dibayar) — basis proporsi jasa usaha. */
	@Column(name = "total_transaksi")
	public Double getTotalTransaksi() {
		return totalTransaksi == null ? 0.0 : totalTransaksi;
	}

	public void setTotalTransaksi(Double totalTransaksi) {
		this.totalTransaksi = totalTransaksi;
	}

	@Column(name = "jasa_modal")
	public Double getJasaModal() {
		return jasaModal == null ? 0.0 : jasaModal;
	}

	public void setJasaModal(Double jasaModal) {
		this.jasaModal = jasaModal;
	}

	@Column(name = "jasa_usaha")
	public Double getJasaUsaha() {
		return jasaUsaha == null ? 0.0 : jasaUsaha;
	}

	public void setJasaUsaha(Double jasaUsaha) {
		this.jasaUsaha = jasaUsaha;
	}

	@Column(name = "total_shu")
	public Double getTotalShu() {
		return totalShu == null ? 0.0 : totalShu;
	}

	public void setTotalShu(Double totalShu) {
		this.totalShu = totalShu;
	}

	@Column(name = "sudah_dibayar")
	public Boolean getSudahDibayar() {
		return sudahDibayar == null ? false : sudahDibayar;
	}

	public void setSudahDibayar(Boolean sudahDibayar) {
		this.sudahDibayar = sudahDibayar;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_bayar")
	public Date getTanggalBayar() {
		return tanggalBayar;
	}

	public void setTanggalBayar(Date tanggalBayar) {
		this.tanggalBayar = tanggalBayar;
	}

	@Override
	public String toString() {
		String namaAnggota = "";
		try {
			namaAnggota = getAnggota() == null ? "" : getAnggota().getNama();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/koperasi/ShuAnggota.java:222");
		}
		return namaAnggota + " - SHU " + getTotalShu();
	}
}
