package ais.database.model;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "paket_has_jurusan")
public class PaketJurusanPmb extends GeneralValueObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6142801292147884065L;

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
		paket = getPaket();
		jurusan = getJurusan();
		return paket + "_" + jurusan;
	}

	private Paket paket;
	private Jurusan jurusan;
	private Boolean pilihan1;
	private Boolean pilihan2;
	private Boolean pilihan3;
	private Boolean pilihan4;
	private Boolean pilihan5;
	private String kelamin;
	private Integer kuota;
	private Boolean kuotaBerlakuPerGelombang;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "paket")
	public Paket getPaket() {
		paket = check(paket);
		return paket;
	}

	public void setPaket(Paket paket) {
		this.paket = paket;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan")
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	public Boolean getPilihan1() {
		return pilihan1 == null ? true : pilihan1;
	}

	public void setPilihan1(Boolean pilihan1) {
		this.pilihan1 = pilihan1;
	}

	public Boolean getPilihan2() {
		return pilihan2 == null ? true : pilihan2;
	}

	public void setPilihan2(Boolean pilihan2) {
		this.pilihan2 = pilihan2;
	}

	public Boolean getPilihan3() {
		return pilihan3 == null ? true : pilihan3;
	}

	public void setPilihan3(Boolean pilihan3) {
		this.pilihan3 = pilihan3;
	}

	public Boolean getPilihan4() {
		return pilihan4 == null ? true : pilihan4;
	}

	public void setPilihan4(Boolean pilihan4) {
		this.pilihan4 = pilihan4;
	}

	public Boolean getPilihan5() {
		return pilihan5 == null ? true : pilihan5;
	}

	public void setPilihan5(Boolean pilihan5) {
		this.pilihan5 = pilihan5;
	}

	public String getKelamin() {
		return kelamin == null || kelamin.trim().isEmpty() ? "Semua" : kelamin;
	}

	public void setKelamin(String kelamin) {
		this.kelamin = kelamin;
	}

	public Integer getKuota() {
		return kuota == null ? 10000 : kuota;
	}

	public void setKuota(Integer kuota) {
		this.kuota = kuota;
	}

	public Boolean getKuotaBerlakuPerGelombang() {
		return kuotaBerlakuPerGelombang == null ? true : kuotaBerlakuPerGelombang;
	}

	public void setKuotaBerlakuPerGelombang(Boolean kuotaBerlakuPerGelombang) {
		this.kuotaBerlakuPerGelombang = kuotaBerlakuPerGelombang;
	}

}
