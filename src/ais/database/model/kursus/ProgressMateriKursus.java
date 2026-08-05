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
 * Progress penyelesaian satu MateriKursus oleh satu peserta yang sudah enroll
 * (ditandai lewat PesertaPunyaProdukKursus). Satu baris per (enrollment, materi).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "progress_materi_kursus")
public class ProgressMateriKursus extends GeneralValueObject {

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
	private Boolean selesai;
	private Date waktuSelesai;
	private Date waktuMulai;
	private Date waktuTerakhir;
	private Integer detikVideoTerakhir;
	private Integer durasiDitonton;
	private Integer persentase;
	private Integer jumlahAkses;

	public ProgressMateriKursus() {
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

	public Boolean getSelesai() {
		return selesai == null ? false : selesai;
	}

	public void setSelesai(Boolean selesai) {
		this.selesai = selesai;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuSelesai() {
		return waktuSelesai;
	}

	public void setWaktuSelesai(Date waktuSelesai) {
		this.waktuSelesai = waktuSelesai;
	}

	/*
	 * Kolom BARU pada tabel LAMA yang sudah ber-@Audited (progress_materi_kursus).
	 * Sesuai kesepakatan sebelumnya utk MateriKursus.ujian dkk, kolom-kolom ini
	 * SENGAJA murni diserahkan ke Hibernate hbm2ddl=update (tanpa self-heal manual).
	 * Konsekuensi: new_audit.progress_materi_kursus__audit TIDAK otomatis dapat kolom
	 * ini -- simpan ProgressMateriKursus bisa gagal sampai dijalankan manual satu kali:
	 *   ALTER TABLE public.progress_materi_kursus ADD COLUMN waktumulai timestamp;
	 *   ALTER TABLE public.progress_materi_kursus ADD COLUMN waktuterakhir timestamp;
	 *   ALTER TABLE public.progress_materi_kursus ADD COLUMN detikvideoterakhir integer;
	 *   ALTER TABLE public.progress_materi_kursus ADD COLUMN durasiditonton integer;
	 *   ALTER TABLE public.progress_materi_kursus ADD COLUMN persentase integer;
	 *   ALTER TABLE public.progress_materi_kursus ADD COLUMN jumlahakses integer;
	 *   ALTER TABLE new_audit.progress_materi_kursus__audit ADD COLUMN waktumulai timestamp;
	 *   ALTER TABLE new_audit.progress_materi_kursus__audit ADD COLUMN waktuterakhir timestamp;
	 *   ALTER TABLE new_audit.progress_materi_kursus__audit ADD COLUMN detikvideoterakhir integer;
	 *   ALTER TABLE new_audit.progress_materi_kursus__audit ADD COLUMN durasiditonton integer;
	 *   ALTER TABLE new_audit.progress_materi_kursus__audit ADD COLUMN persentase integer;
	 *   ALTER TABLE new_audit.progress_materi_kursus__audit ADD COLUMN jumlahakses integer;
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuMulai() {
		return waktuMulai;
	}

	public void setWaktuMulai(Date waktuMulai) {
		this.waktuMulai = waktuMulai;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuTerakhir() {
		return waktuTerakhir;
	}

	public void setWaktuTerakhir(Date waktuTerakhir) {
		this.waktuTerakhir = waktuTerakhir;
	}

	public Integer getDetikVideoTerakhir() {
		return detikVideoTerakhir == null ? 0 : detikVideoTerakhir;
	}

	public void setDetikVideoTerakhir(Integer detikVideoTerakhir) {
		this.detikVideoTerakhir = detikVideoTerakhir;
	}

	public Integer getDurasiDitonton() {
		return durasiDitonton == null ? 0 : durasiDitonton;
	}

	public void setDurasiDitonton(Integer durasiDitonton) {
		this.durasiDitonton = durasiDitonton;
	}

	public Integer getPersentase() {
		if (persentase == null) return 0;
		if (persentase > 100) return 100;
		if (persentase < 0) return 0;
		return persentase;
	}

	public void setPersentase(Integer persentase) {
		this.persentase = persentase;
	}

	public Integer getJumlahAkses() {
		return jumlahAkses == null ? 0 : jumlahAkses;
	}

	public void setJumlahAkses(Integer jumlahAkses) {
		this.jumlahAkses = jumlahAkses;
	}

}
