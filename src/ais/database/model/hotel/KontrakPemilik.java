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
 * Kontrak pemilik kamar (model kondotel: tiap kamar dimiliki investor, operator
 * memungut komisi persen dari pendapatan kamar) -- LANGKAH 5 MitraInap, padanan
 * {@code hospitality_owner_contract} versi Node ({@code ownerContract()}
 * hospitality-longstay.service.ts). Pendapatan pemilik dihitung server saat
 * generate {@link LaporanPemilik} -- bukan angka kiriman klien.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "hotel_kontrak_pemilik")
public class KontrakPemilik extends GeneralValueObject {

	private static final long serialVersionUID = 7268413577548439830L;

	private Long id;
	private PropertiHotel properti;
	private Kamar kamar;
	private String namaPemilik;
	private String referensiPemilik;
	private Double persenKomisi;
	private Date berlakuDari;
	private Date berlakuSampai;
	private Boolean aktif;
	private String oleh;
	private String olehId;

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public String toString() {
		return (id == null ? "" : id) + "-" + (namaPemilik == null ? "" : namaPemilik);
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "properti", nullable = false)
	public PropertiHotel getProperti() { properti = check(properti); return properti; }
	public void setProperti(PropertiHotel properti) { this.properti = properti; }

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "kamar", nullable = false)
	public Kamar getKamar() { kamar = check(kamar); return kamar; }
	public void setKamar(Kamar kamar) { this.kamar = kamar; }

	@Column(name = "nama_pemilik", nullable = false, length = 255)
	public String getNamaPemilik() { return namaPemilik; }
	public void setNamaPemilik(String namaPemilik) { this.namaPemilik = namaPemilik; }

	/** Referensi bebas (no. KTP/NPWP/kode investor) -- opsional. */
	@Column(name = "referensi_pemilik", nullable = true, length = 100)
	public String getReferensiPemilik() { return referensiPemilik; }
	public void setReferensiPemilik(String referensiPemilik) { this.referensiPemilik = referensiPemilik; }

	/** Komisi OPERATOR dalam persen (0..100) dari pendapatan kamar; sisa = hak pemilik. */
	@Column(name = "persen_komisi", nullable = false)
	public Double getPersenKomisi() { return persenKomisi; }
	public void setPersenKomisi(Double persenKomisi) { this.persenKomisi = persenKomisi; }

	@Temporal(TemporalType.DATE)
	@Column(name = "berlaku_dari", nullable = false)
	public Date getBerlakuDari() { return berlakuDari; }
	public void setBerlakuDari(Date berlakuDari) { this.berlakuDari = berlakuDari; }

	@Temporal(TemporalType.DATE)
	@Column(name = "berlaku_sampai", nullable = true)
	public Date getBerlakuSampai() { return berlakuSampai; }
	public void setBerlakuSampai(Date berlakuSampai) { this.berlakuSampai = berlakuSampai; }

	@Column(name = "aktif", nullable = true)
	public Boolean getAktif() { return aktif == null ? Boolean.TRUE : aktif; }
	public void setAktif(Boolean aktif) { this.aktif = aktif; }

	public String getOleh() { return oleh; }
	public void setOleh(String oleh) { if (oleh == null || oleh.trim().isEmpty()) return; this.oleh = oleh; }
	public String getOlehId() { return olehId; }
	public void setOlehId(String olehId) { if (olehId == null || olehId.trim().isEmpty()) return; this.olehId = olehId; }

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() { return tanggal_dirubah; }
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }
}
