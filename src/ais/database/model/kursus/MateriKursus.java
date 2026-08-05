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

import ais.database.model.GeneralValueObject;
import ais.database.model.Ujian;

/**
 * Satu item materi/lecture di dalam sebuah SeksiKursus. Video/lampiran fisik disimpan
 * lewat LampiranLain (ref = id materi ini, jenis = "Video Materi Kursus"), bukan kolom di sini.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "materi_kursus")
public class MateriKursus extends GeneralValueObject {

	public final static String VIDEO = "Video";
	public final static String ARTIKEL = "Artikel";
	public final static String QUIZ = "Quiz";
	public final static String TUGAS = "Tugas";

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
		return id + "-" + nama;
	}

	private String kode;
	private String nama;
	private String keterangan;
	private SeksiKursus seksiKursus;
	private String tipeKonten;
	private Integer durasiMenit;
	private Integer urutan;
	private Boolean preview;
	private Boolean aktif;
	private Ujian ujian;
	private Integer batasWaktuMenit;
	private Integer batasPercobaan;
	private Boolean acakSoal;
	private Boolean acakJawaban;
	private Integer jumlahSoalDitampilkan;

	public MateriKursus() {
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

	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "seksi_kursus", nullable = false)
	public SeksiKursus getSeksiKursus() {
		seksiKursus = check(seksiKursus);
		return seksiKursus;
	}

	public void setSeksiKursus(SeksiKursus seksiKursus) {
		this.seksiKursus = seksiKursus;
	}

	@Column(name = "tipe_konten", nullable = true, length = 50)
	public String getTipeKonten() {
		return tipeKonten == null || tipeKonten.isEmpty() ? VIDEO : tipeKonten;
	}

	public void setTipeKonten(String tipeKonten) {
		this.tipeKonten = tipeKonten;
	}

	public Integer getDurasiMenit() {
		return durasiMenit == null ? 0 : durasiMenit;
	}

	public void setDurasiMenit(Integer durasiMenit) {
		this.durasiMenit = durasiMenit;
	}

	public Integer getUrutan() {
		return urutan == null ? 0 : urutan;
	}

	public void setUrutan(Integer urutan) {
		this.urutan = urutan;
	}

	public Boolean getPreview() {
		return preview == null ? false : preview;
	}

	public void setPreview(Boolean preview) {
		this.preview = preview;
	}

	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/*
	 * Kolom BARU pada tabel LAMA yang sudah ber-@Audited. Sesuai keputusan yang
	 * disepakati, kolom ini SENGAJA murni diserahkan ke Hibernate hbm2ddl=update
	 * (tanpa helper self-heal manual seperti KursusSchemaFix) -- konsekuensinya
	 * tabel new_audit.materi_kursus__audit TIDAK otomatis mendapat kolom ini,
	 * dan proses simpan MateriKursus BISA GAGAL sampai kolom berikut ditambah
	 * manual satu kali di database:
	 *   ALTER TABLE public.materi_kursus ADD COLUMN ujian bigint;
	 *   ALTER TABLE public.materi_kursus ADD COLUMN batas_waktu_menit integer;
	 *   ALTER TABLE public.materi_kursus ADD COLUMN batas_percobaan integer;
	 *   ALTER TABLE public.materi_kursus ADD COLUMN acak_soal boolean;
	 *   ALTER TABLE public.materi_kursus ADD COLUMN acak_jawaban boolean;
	 *   ALTER TABLE public.materi_kursus ADD COLUMN jumlah_soal_ditampilkan integer;
	 *   ALTER TABLE new_audit.materi_kursus__audit ADD COLUMN ujian bigint;
	 *   ALTER TABLE new_audit.materi_kursus__audit ADD COLUMN batas_waktu_menit integer;
	 *   ALTER TABLE new_audit.materi_kursus__audit ADD COLUMN batas_percobaan integer;
	 *   ALTER TABLE new_audit.materi_kursus__audit ADD COLUMN acak_soal boolean;
	 *   ALTER TABLE new_audit.materi_kursus__audit ADD COLUMN acak_jawaban boolean;
	 *   ALTER TABLE new_audit.materi_kursus__audit ADD COLUMN jumlah_soal_ditampilkan integer;
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ujian", nullable = true)
	public Ujian getUjian() {
		ujian = check(ujian);
		return ujian;
	}

	public void setUjian(Ujian ujian) {
		this.ujian = ujian;
	}

	public Integer getBatasWaktuMenit() {
		return batasWaktuMenit;
	}

	public void setBatasWaktuMenit(Integer batasWaktuMenit) {
		this.batasWaktuMenit = batasWaktuMenit;
	}

	public Integer getBatasPercobaan() {
		return batasPercobaan;
	}

	public void setBatasPercobaan(Integer batasPercobaan) {
		this.batasPercobaan = batasPercobaan;
	}

	public Boolean getAcakSoal() {
		return acakSoal == null ? false : acakSoal;
	}

	public void setAcakSoal(Boolean acakSoal) {
		this.acakSoal = acakSoal;
	}

	public Boolean getAcakJawaban() {
		return acakJawaban == null ? false : acakJawaban;
	}

	public void setAcakJawaban(Boolean acakJawaban) {
		this.acakJawaban = acakJawaban;
	}

	public Integer getJumlahSoalDitampilkan() {
		return jumlahSoalDitampilkan;
	}

	public void setJumlahSoalDitampilkan(Integer jumlahSoalDitampilkan) {
		this.jumlahSoalDitampilkan = jumlahSoalDitampilkan;
	}

}
