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
import ais.database.model.inventory.Toko;

/**
 * Class untuk menyimpan riwayat transaksi Pencairan Saldo Diskon (Cashback)
 * Member. Digunakan apabila diskon disetting "potonganLangsung = false" pada
 * AturanDiskon.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "pencairan_diskon")
public class PencairanDiskon extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	private String kodePencairan; // Nomor referensi/struk pencairan (Contoh: WD-20260301-001)

	// --- RELASI UTAMA ---
	private AnggotaKoperasi anggotaKoperasi; // WAJIB: Member siapa yang mencairkan saldo
	private Toko toko; // OPSIONAL: Di toko/kios mana pencairan dilakukan (Jika ditarik via Kasir)
	private CaraPembayaranKoperasi caraPembayaran; // WAJIB: Dicairkan via apa? (Tunai, Transfer Bank, Potong Belanja,
													// dll)

	// --- DATA NOMINAL & WAKTU ---
	private Double nominalCair; // Jumlah saldo yang ditarik/dicairkan
	private Date waktuPencairan; // Tanggal dan jam eksekusi pencairan
	private Date tanggalExpiredJikaBerupaTopup; // Jika berupa topup, maka bisa expired, tapi boleh kosong

	// --- STATUS & KETERANGAN ---
	private String status; // PENDING, BERHASIL, DITOLAK
	private String keterangan; // Catatan (misal: "Dicairkan tunai oleh kasir A", "Ditransfer ke BCA")

	// --- AUDIT TRAIL ---
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

	public PencairanDiskon() {
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

	@Column(name = "kode_pencairan", unique = true, nullable = false, length = 100)
	public String getKodePencairan() {
		return kodePencairan;
	}

	public void setKodePencairan(String kodePencairan) {
		this.kodePencairan = kodePencairan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "anggota_koperasi", nullable = false)
	public AnggotaKoperasi getAnggotaKoperasi() {
		anggotaKoperasi = check(anggotaKoperasi);
		return anggotaKoperasi;
	}

	public void setAnggotaKoperasi(AnggotaKoperasi anggotaKoperasi) {
		this.anggotaKoperasi = anggotaKoperasi;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko", nullable = true)
	public Toko getToko() {
		toko = check(toko);
		return toko;
	}

	public void setToko(Toko toko) {
		this.toko = toko;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "cara_pembayaran", nullable = false)
	public CaraPembayaranKoperasi getCaraPembayaran() {
		caraPembayaran = check(caraPembayaran);
		return caraPembayaran;
	}

	public void setCaraPembayaran(CaraPembayaranKoperasi caraPembayaran) {
		this.caraPembayaran = caraPembayaran;
	}

	@Column(name = "nominal_cair", nullable = false)
	public Double getNominalCair() {
		return nominalCair == null ? 0.0 : nominalCair;
	}

	public void setNominalCair(Double nominalCair) {
		this.nominalCair = nominalCair;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu_pencairan", nullable = false)
	public Date getWaktuPencairan() {
		return waktuPencairan == null ? ais.ui.util.WaktuUtil.getDate() : waktuPencairan;
	}

	public void setWaktuPencairan(Date waktuPencairan) {
		this.waktuPencairan = waktuPencairan;
	}

	@Column(name = "status", length = 50)
	public String getStatus() {
		return status == null ? "PENDING" : status;
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

	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_expired_jika_berupa_topup")
	public Date getTanggalExpiredJikaBerupaTopup() {
		return tanggalExpiredJikaBerupaTopup;
	}

	public void setTanggalExpiredJikaBerupaTopup(Date tanggalExpiredJikaBerupaTopup) {
		this.tanggalExpiredJikaBerupaTopup = tanggalExpiredJikaBerupaTopup;
	}

}