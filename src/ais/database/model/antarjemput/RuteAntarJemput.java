package ais.database.model.antarjemput;

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

@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "rute_antar_jemput")
public class RuteAntarJemput extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439812L;

	public static final String JEMPUT = "JEMPUT";
	public static final String ANTAR = "ANTAR";
	public static final String PULANG_PERGI = "PULANG_PERGI";

	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	private String kode;
	private String nama;
	private String keterangan;
	private String jenisLayanan;
	private String titikAwal;
	private String titikAkhir;
	private Date jamBerangkat;
	private Integer estimasiMenit;
	private Boolean aktif;

	public RuteAntarJemput() {
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

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
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

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Column(name = "kode", length = 50)
	public String getKode() {
		return kode == null ? null : kode.trim();
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return nama == null ? null : nama.trim();
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@Column(name = "keterangan")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@Column(name = "jenis_layanan", length = 30)
	public String getJenisLayanan() {
		return jenisLayanan == null ? JEMPUT : jenisLayanan;
	}

	public void setJenisLayanan(String jenisLayanan) {
		this.jenisLayanan = jenisLayanan;
	}

	@Column(name = "titik_awal")
	public String getTitikAwal() {
		return titikAwal;
	}

	public void setTitikAwal(String titikAwal) {
		this.titikAwal = titikAwal;
	}

	@Column(name = "titik_akhir")
	public String getTitikAkhir() {
		return titikAkhir;
	}

	public void setTitikAkhir(String titikAkhir) {
		this.titikAkhir = titikAkhir;
	}

	@Temporal(TemporalType.TIME)
	public Date getJamBerangkat() {
		return jamBerangkat;
	}

	public void setJamBerangkat(Date jamBerangkat) {
		this.jamBerangkat = jamBerangkat;
	}

	public Integer getEstimasiMenit() {
		return estimasiMenit == null ? 0 : estimasiMenit;
	}

	public void setEstimasiMenit(Integer estimasiMenit) {
		this.estimasiMenit = estimasiMenit;
	}

	public Boolean getAktif() {
		return aktif == null ? Boolean.TRUE : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}
}
