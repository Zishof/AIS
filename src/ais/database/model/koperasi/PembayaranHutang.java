package ais.database.model.koperasi;

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
 * <h3>Fitur "Mutasi Hutang" (Pelanggan) -- pembayaran/pelunasan hutang anggota koperasi.</h3>
 *
 * <p>Pasangan KREDIT dari sisi DEBIT "hutang bertambah" (yg dihitung dari
 * {@code koperasi.pembelian_anggota_koperasi} yang cara pembayarannya ditandai
 * {@link CaraPembayaranKoperasi#getMasukSebagaiHutang()} -- lihat JavaDoc di sana). Baris di sini
 * murni entri manual "member bayar cicilan/lunas hutang", polanya SENGAJA disamakan persis dgn
 * {@link ais.database.model.Deposit} (Topup) -- SATU baris = satu pembayaran, TIDAK ada status
 * "sudah dialokasikan ke transaksi mana" (mengikuti hutang seperti mengikuti tabungan: yg dilacak
 * hanya SALDO BERJALAN per anggota, bukan pelunasan per-transaksi).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "pembayaran_hutang")
public class PembayaranHutang extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439809L;
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
		return id + "-" + (anggotaKoperasi == null ? "" : anggotaKoperasi.getNama());
	}

	private AnggotaKoperasi anggotaKoperasi;
	private Double nominal;
	private Date waktu;
	private String keterangan;

	public PembayaranHutang() {
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

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@JoinColumn(name = "anggota_koperasi", nullable = false)
	public AnggotaKoperasi getAnggotaKoperasi() {
		return anggotaKoperasi;
	}

	public void setAnggotaKoperasi(AnggotaKoperasi anggotaKoperasi) {
		this.anggotaKoperasi = anggotaKoperasi;
	}

	@Column(name = "nominal")
	public Double getNominal() {
		return nominal == null ? 0.0 : nominal;
	}

	public void setNominal(Double nominal) {
		this.nominal = nominal;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu")
	public Date getWaktu() {
		return waktu;
	}

	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	@Column(name = "keterangan")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}
}
