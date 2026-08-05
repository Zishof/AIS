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
 * Satu percobaan (attempt) peserta kursus mengerjakan MateriKursus bertipe TUGAS/QUIZ
 * yang tertaut ke sebuah Ujian (soal-soalnya di-reuse dari BankSoal/BankSoalDetail/Ujian
 * yang sudah ada -- entity ini HANYA menyimpan riwayat percobaan+skor, bukan definisi soal).
 * Tabel BARU (bukan kolom baru di tabel lama) sehingga hbm2ddl=update cukup membuat tabel
 * utama + tabel bayangan audit sekaligus, tanpa perlu self-heal manual.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "percobaan_kuis_kursus")
public class PercobaanKuisKursus extends GeneralValueObject {

	public final static String BERLANGSUNG = "Berlangsung";
	public final static String SELESAI = "Selesai";

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
	private MateriKursus materiKursus;
	private PesertaPunyaProdukKursus pesertaPunyaProdukKursus;
	private Integer nomorPercobaan;
	private Date waktuMulai;
	private Date waktuSelesai;
	private String status;
	private Double totalNilai;
	private Boolean lulus;
	private Integer jumlahSoal;
	private Integer jumlahBenar;

	public PercobaanKuisKursus() {
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
		if (getMateriKursus() != null && getPesertaPunyaProdukKursus() != null
				&& getPesertaPunyaProdukKursus().getPesertaKursus() != null) {
			nama = getPesertaPunyaProdukKursus().getPesertaKursus().getNama() + " - " + materiKursus.getNama()
					+ " (percobaan " + getNomorPercobaan() + ")";
		}
		return nama;
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "materi_kursus", nullable = false)
	public MateriKursus getMateriKursus() {
		materiKursus = check(materiKursus);
		return materiKursus;
	}

	public void setMateriKursus(MateriKursus materiKursus) {
		this.materiKursus = materiKursus;
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

	public Integer getNomorPercobaan() {
		return nomorPercobaan == null ? 1 : nomorPercobaan;
	}

	public void setNomorPercobaan(Integer nomorPercobaan) {
		this.nomorPercobaan = nomorPercobaan;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuMulai() {
		return waktuMulai == null ? new Date() : waktuMulai;
	}

	public void setWaktuMulai(Date waktuMulai) {
		this.waktuMulai = waktuMulai;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuSelesai() {
		return waktuSelesai;
	}

	public void setWaktuSelesai(Date waktuSelesai) {
		this.waktuSelesai = waktuSelesai;
	}

	public String getStatus() {
		return status == null || status.isEmpty() ? BERLANGSUNG : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Double getTotalNilai() {
		return totalNilai == null ? 0.0 : totalNilai;
	}

	public void setTotalNilai(Double totalNilai) {
		this.totalNilai = totalNilai;
	}

	public Boolean getLulus() {
		return lulus;
	}

	public void setLulus(Boolean lulus) {
		this.lulus = lulus;
	}

	public Integer getJumlahSoal() {
		return jumlahSoal == null ? 0 : jumlahSoal;
	}

	public void setJumlahSoal(Integer jumlahSoal) {
		this.jumlahSoal = jumlahSoal;
	}

	public Integer getJumlahBenar() {
		return jumlahBenar == null ? 0 : jumlahBenar;
	}

	public void setJumlahBenar(Integer jumlahBenar) {
		this.jumlahBenar = jumlahBenar;
	}

}
