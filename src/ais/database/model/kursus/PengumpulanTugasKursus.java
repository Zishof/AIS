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
 * Pengajuan/pengumpulan tugas (LK/Unggah Tugas) oleh peserta untuk satu MateriKursus
 * bertipe MateriKursus.TUGAS. Satu baris per (enrollment, materi) -- diperbarui (bukan
 * ditambah baris baru) setiap kali peserta unggah ulang berkasnya.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "pengumpulan_tugas_kursus")
public class PengumpulanTugasKursus extends GeneralValueObject {

	public final static String DIKUMPULKAN = "Dikumpulkan";
	public final static String DINILAI = "Dinilai";

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
	private MateriKursus materiKursus;
	private String namaFile;
	private String link;
	private Date waktuKumpul;
	private String status;
	private Double nilai;
	private String catatanPenilaian;

	public PengumpulanTugasKursus() {
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
			nama = getPesertaPunyaProdukKursus().getPesertaKursus().getNama() + " - " + materiKursus.getNama();
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

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "materi_kursus", nullable = false)
	public MateriKursus getMateriKursus() {
		materiKursus = check(materiKursus);
		return materiKursus;
	}

	public void setMateriKursus(MateriKursus materiKursus) {
		this.materiKursus = materiKursus;
	}

	public String getNamaFile() {
		return namaFile;
	}

	public void setNamaFile(String namaFile) {
		this.namaFile = namaFile;
	}

	@Column(name = "link", nullable = true, columnDefinition = "text")
	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuKumpul() {
		return waktuKumpul == null ? new Date() : waktuKumpul;
	}

	public void setWaktuKumpul(Date waktuKumpul) {
		this.waktuKumpul = waktuKumpul;
	}

	public String getStatus() {
		return status == null || status.isEmpty() ? DIKUMPULKAN : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Double getNilai() {
		return nilai;
	}

	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	@Column(name = "catatan_penilaian", nullable = true, columnDefinition = "text")
	public String getCatatanPenilaian() {
		return catatanPenilaian;
	}

	public void setCatatanPenilaian(String catatanPenilaian) {
		this.catatanPenilaian = catatanPenilaian;
	}

}
