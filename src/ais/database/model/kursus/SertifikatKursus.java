package ais.database.model.kursus;

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

import ais.common.BarcodeCommon;
import ais.database.model.GeneralValueObject;

/**
 * Sertifikat kelulusan/kelengkapan sebuah ProdukKursus, diterbitkan otomatis begitu semua
 * MateriKursus milik satu enrollment (PesertaPunyaProdukKursus) sudah selesai. Tabel BARU
 * (bukan kolom baru di tabel lama) sehingga hbm2ddl=update cukup membuat tabel utama + tabel
 * bayangan audit sekaligus, tanpa perlu self-heal manual.
 *
 * Kolom bawaan "kode" (GeneralValueObject, auto BarcodeCommon.generateCode()) dipakai sebagai
 * kode verifikasi publik -- reuse pola yang sama persis dengan entity kursus lain, bukan kolom baru.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "sertifikat_kursus")
public class SertifikatKursus extends GeneralValueObject {

	public final static String AKTIF = "Aktif";
	public final static String DICABUT = "Dicabut";

	private static final long serialVersionUID = 2463821577548439808L;
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
		return kode + " - " + nama;
	}

	private String kode;
	private String nama;
	private String keterangan;
	private PesertaPunyaProdukKursus pesertaPunyaProdukKursus;
	private String nomorSertifikat;
	private Date tanggalTerbit;
	private Double nilaiAkhir;
	private Integer durasiBelajarMenit;
	private String status;

	public SertifikatKursus() {
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

	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@Column(unique = true)
	public String getKode() {
		if (kode == null) {
			kode = BarcodeCommon.generateCode();
		}
		return kode;
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	public String getNama() {
		if (getPesertaPunyaProdukKursus() != null && getPesertaPunyaProdukKursus().getPesertaKursus() != null
				&& getPesertaPunyaProdukKursus().getProdukKursus() != null) {
			nama = getPesertaPunyaProdukKursus().getPesertaKursus().getNama() + " - "
					+ getPesertaPunyaProdukKursus().getProdukKursus().getNama();
		}
		return nama;
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "peserta_punya_produk_kursus", nullable = false)
	public PesertaPunyaProdukKursus getPesertaPunyaProdukKursus() {
		pesertaPunyaProdukKursus = check(pesertaPunyaProdukKursus);
		return pesertaPunyaProdukKursus;
	}

	public void setPesertaPunyaProdukKursus(PesertaPunyaProdukKursus pesertaPunyaProdukKursus) {
		this.pesertaPunyaProdukKursus = pesertaPunyaProdukKursus;
	}

	@Column(unique = true)
	public String getNomorSertifikat() {
		return nomorSertifikat;
	}

	public void setNomorSertifikat(String nomorSertifikat) {
		this.nomorSertifikat = nomorSertifikat;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggalTerbit() {
		return tanggalTerbit == null ? new Date() : tanggalTerbit;
	}

	public void setTanggalTerbit(Date tanggalTerbit) {
		this.tanggalTerbit = tanggalTerbit;
	}

	public Double getNilaiAkhir() {
		return nilaiAkhir;
	}

	public void setNilaiAkhir(Double nilaiAkhir) {
		this.nilaiAkhir = nilaiAkhir;
	}

	public Integer getDurasiBelajarMenit() {
		return durasiBelajarMenit == null ? 0 : durasiBelajarMenit;
	}

	public void setDurasiBelajarMenit(Integer durasiBelajarMenit) {
		this.durasiBelajarMenit = durasiBelajarMenit;
	}

	public String getStatus() {
		return status == null || status.isEmpty() ? AKTIF : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}
