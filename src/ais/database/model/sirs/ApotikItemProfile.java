package ais.database.model.sirs;

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
 * Profil farmasi per {@link ItemMedis} -- varian "POS Apotik" (FASE A).
 *
 * <p>ENTITY BARU (bukan kolom tambahan di ItemMedis) DISENGAJA: ItemMedis ber-{@code @Audited}
 * dan hbm2ddl TIDAK menyinkronkan kolom baru ke tabel audit {@code new_audit.item_medis__audit}
 * (lihat peringatan operasional di hibernate.cfg.xml) -- kolom baru di sana bisa menggagalkan
 * INSERT audit dan me-rollback simpan item. Tabel baru + tabel auditnya tercipta utuh sekaligus.</p>
 *
 * <p>{@code golonganObat} = konstanta teks tervalidasi ({@link #GOLONGAN_NARKOTIKA} dst.);
 * NARKOTIKA/PSIKOTROPIKA menuntut register penjualan ({@link ApotikNarkotikaLog}) -- transaksi
 * DITAHAN server bila register tidak bisa dibuat, bukan dilanjutkan diam-diam. {@code lasa}
 * (Look-Alike Sound-Alike) murni penanda tampilan kasir -- obat mirip ditampilkan berbeda.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "apotik_item_profile")
public class ApotikItemProfile extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	public static final String GOLONGAN_BEBAS = "BEBAS";
	public static final String GOLONGAN_BEBAS_TERBATAS = "BEBAS_TERBATAS";
	public static final String GOLONGAN_KERAS = "KERAS";
	public static final String GOLONGAN_NARKOTIKA = "NARKOTIKA";
	public static final String GOLONGAN_PSIKOTROPIKA = "PSIKOTROPIKA";

	/** Golongan yang menuntut register penjualan (obat terkendali). */
	public static boolean terkendali(String golongan) {
		return GOLONGAN_NARKOTIKA.equals(golongan) || GOLONGAN_PSIKOTROPIKA.equals(golongan);
	}

	public static boolean golonganValid(String golongan) {
		return GOLONGAN_BEBAS.equals(golongan) || GOLONGAN_BEBAS_TERBATAS.equals(golongan)
				|| GOLONGAN_KERAS.equals(golongan) || GOLONGAN_NARKOTIKA.equals(golongan)
				|| GOLONGAN_PSIKOTROPIKA.equals(golongan);
	}

	private Long id;
	private ItemMedis item;
	private String golonganObat;
	private Boolean lasa;
	// IR-01 (modernisasi UI/UX apotik): atribut yang dibutuhkan kasir untuk
	// membedakan obat secara cepat dan menandai risiko tinggi. Semua NULLABLE
	// supaya baris profil lama tetap sah tanpa migrasi data.
	private String bentukSediaan;
	private String kekuatan;
	private Boolean highAlert;
	private Boolean coldChain;
	private String keterangan;

	private String oleh;
	private String olehId;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public ApotikItemProfile() {
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item", nullable = false)
	public ItemMedis getItem() {
		item = check(item);
		return item;
	}

	public void setItem(ItemMedis item) {
		this.item = item;
	}

	@Column(name = "golongan_obat", length = 30)
	public String getGolonganObat() {
		return golonganObat == null || golonganObat.trim().isEmpty() ? GOLONGAN_BEBAS : golonganObat;
	}

	public void setGolonganObat(String golonganObat) {
		this.golonganObat = golonganObat;
	}

	@Column(name = "lasa")
	public Boolean getLasa() {
		return lasa == null ? Boolean.FALSE : lasa;
	}

	public void setLasa(Boolean lasa) {
		this.lasa = lasa;
	}

	/** Bentuk sediaan (tablet, sirup, injeksi, salep, ...). Teks bebas ringkas. */
	@Column(name = "bentuk_sediaan", length = 60)
	public String getBentukSediaan() {
		return bentukSediaan;
	}

	public void setBentukSediaan(String bentukSediaan) {
		this.bentukSediaan = bentukSediaan;
	}

	/** Kekuatan/dosis satuan (mis. "500 mg", "5 mg/5 mL"). */
	@Column(name = "kekuatan", length = 60)
	public String getKekuatan() {
		return kekuatan;
	}

	public void setKekuatan(String kekuatan) {
		this.kekuatan = kekuatan;
	}

	/**
	 * Obat high-alert (risiko cedera tinggi bila salah): insulin, heparin,
	 * elektrolit pekat, dsb. Dipakai UI untuk menandai baris secara mencolok.
	 * BUKAN pengganti golongan obat -- keduanya berdiri sendiri.
	 */
	@Column(name = "high_alert")
	public Boolean getHighAlert() {
		return highAlert == null ? Boolean.FALSE : highAlert;
	}

	public void setHighAlert(Boolean highAlert) {
		this.highAlert = highAlert;
	}

	/** Wajib rantai dingin (2-8 C). Menentukan peringatan penyimpanan/kirim. */
	@Column(name = "cold_chain")
	public Boolean getColdChain() {
		return coldChain == null ? Boolean.FALSE : coldChain;
	}

	public void setColdChain(Boolean coldChain) {
		this.coldChain = coldChain;
	}

	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
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

	@Column(name = "oleh_id")
	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
