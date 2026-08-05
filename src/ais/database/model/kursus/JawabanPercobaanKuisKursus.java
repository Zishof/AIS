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

import ais.database.model.BankSoal;
import ais.database.model.BankSoalDetail;
import ais.database.model.GeneralValueObject;

/**
 * Jawaban peserta utk satu soal (BankSoal, di-reuse dari sistem CBT akademik) dalam
 * satu PercobaanKuisKursus. Untuk soal Pilihan Ganda/Benar-Salah, jawaban berupa pilihan
 * BankSoalDetail (skor otomatis via BankSoalDetail.getSkor()). Untuk Esai, jawaban berupa
 * teks bebas yang butuh koreksi manual instruktur.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "jawaban_percobaan_kuis_kursus")
public class JawabanPercobaanKuisKursus extends GeneralValueObject {

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
	private PercobaanKuisKursus percobaanKuisKursus;
	private BankSoal bankSoal;
	private BankSoalDetail bankSoalDetailDipilih;
	private String jawabanEsai;
	private Double skor;
	private Boolean benar;
	private Integer urutanTampil;
	private Boolean sudahDinilai;
	private String catatanPenilaian;

	public JawabanPercobaanKuisKursus() {
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

	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	public String getNama() {
		return nama;
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "percobaan_kuis_kursus", nullable = false)
	public PercobaanKuisKursus getPercobaanKuisKursus() {
		percobaanKuisKursus = check(percobaanKuisKursus);
		return percobaanKuisKursus;
	}

	public void setPercobaanKuisKursus(PercobaanKuisKursus percobaanKuisKursus) {
		this.percobaanKuisKursus = percobaanKuisKursus;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "bank_soal", nullable = false)
	public BankSoal getBankSoal() {
		bankSoal = check(bankSoal);
		return bankSoal;
	}

	public void setBankSoal(BankSoal bankSoal) {
		this.bankSoal = bankSoal;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "bank_soal_detail_dipilih", nullable = true)
	public BankSoalDetail getBankSoalDetailDipilih() {
		bankSoalDetailDipilih = check(bankSoalDetailDipilih);
		return bankSoalDetailDipilih;
	}

	public void setBankSoalDetailDipilih(BankSoalDetail bankSoalDetailDipilih) {
		this.bankSoalDetailDipilih = bankSoalDetailDipilih;
	}

	@Column(name = "jawaban_esai", nullable = true, columnDefinition = "text")
	public String getJawabanEsai() {
		return jawabanEsai;
	}

	public void setJawabanEsai(String jawabanEsai) {
		this.jawabanEsai = jawabanEsai;
	}

	public Double getSkor() {
		return skor == null ? 0.0 : skor;
	}

	public void setSkor(Double skor) {
		this.skor = skor;
	}

	public Boolean getBenar() {
		return benar;
	}

	public void setBenar(Boolean benar) {
		this.benar = benar;
	}

	public Integer getUrutanTampil() {
		return urutanTampil == null ? 0 : urutanTampil;
	}

	public void setUrutanTampil(Integer urutanTampil) {
		this.urutanTampil = urutanTampil;
	}

	public Boolean getSudahDinilai() {
		return sudahDinilai == null ? false : sudahDinilai;
	}

	public void setSudahDinilai(Boolean sudahDinilai) {
		this.sudahDinilai = sudahDinilai;
	}

	@Column(name = "catatan_penilaian", nullable = true, columnDefinition = "text")
	public String getCatatanPenilaian() {
		return catatanPenilaian;
	}

	public void setCatatanPenilaian(String catatanPenilaian) {
		this.catatanPenilaian = catatanPenilaian;
	}

}
