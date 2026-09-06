package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.common.Common;

/**
 * Model data untuk satu bukti kinerja dosen di luar pengajaran/perkuliahan langsung: kegiatan
 * penunjang Tridharma (pendidikan/penelitian/pengabdian/penunjang lain) beserta bukti dokumennya,
 * dipakai sebagai komponen beban kerja dosen (BKD) per semester. Tipe ini membawa state yang
 * dipertukarkan oleh lapisan persistence, service, dan UI; makna bisnis utamanya ditentukan oleh
 * field serta relasi yang dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code Date tanggal_dirubah}, {@code Pegawai pegawai}, {@code Double sks}, {@code String
 * jenis}, {@code Date tanggalMulai}, {@code Date tanggalSampai}; pemetaan persistence: tabel
 * {@code public.penunjang_kinerja_dosen}; pembacaan/pencarian ({@code getOlehId()}, {@code getId()}, {@code
 * getOleh()}, {@code getTanggal_dirubah()}, {@code getPegawai()}, {@code getJenis()}, {@code getSemester()});
 * mutasi data ({@code setOlehId()}, {@code setId()}, {@code setOleh()}, {@code onUpdate()}, {@code
 * setTanggal_dirubah()}, {@code setPegawai()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
 * <p><b>Catatan relasi:</b> field pemilik bernama {@link #getPegawai()} ({@code Pegawai}), BUKAN {@code Dosen},
 * walau kelas ini secara konsep khusus untuk dosen -- dosen di AIS umumnya juga tercatat sebagai baris
 * {@code Pegawai}, jadi relasinya tetap konsisten dengan pola itu.</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "penunjang_kinerja_dosen")

public class PenunjangKinerjaDosen extends GeneralValueObject {

	/** Kategori Tridharma: pendidikan (bagian dari {@link #getJenis()}). */
	public static final String PENDIDIKAN = "Pendidikan";
	/** Kategori Tridharma: penelitian. */
	public static final String PENELITIAN = "Penelitian";
	/** Kategori Tridharma: pengabdian kepada masyarakat. */
	public static final String PENGABDIAN = "Pengabdian";
	/** Kategori default/"lainnya": penunjang -- dipakai {@link #getJenis()} bila kolomnya kosong. */
	public static final String PENUNJANG = "Penunjang";

	/** Versi serialisasi Java untuk kompatibilitas {@code Serializable}. */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key entity (kolom {@code id}, identity/auto-increment). */
	private Long id;
	/**
	 * Nama pengguna pengubah terakhir. Field ini MENIMPA (shadow) field bernama sama pada
	 * {@link GeneralValueObject}; getter/setter di bawah beroperasi pada field lokal ini.
	 */
	private String oleh;
	/** Id pengguna pengubah terakhir; shadow dari field sama pada {@link GeneralValueObject}. */
	private String olehId;

	/**
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi.
	 * @see GeneralValueObject#getOlehId()
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir. Nilai {@code null}/kosong diabaikan diam-diam,
	 * sama seperti {@link GeneralValueObject#setOlehId(String)}.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Nilai {@code null}/kosong diabaikan diam-diam,
	 * sama seperti {@link #setOlehId(String)}.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA sebelum UPDATE: memperbarui {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     /** Stempel waktu perubahan terakhir; diinisialisasi ke saat object dibuat. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah stempel waktu perubahan terakhir yang baru. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return stempel waktu perubahan terakhir. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi ringkas untuk debug/log: {@code "<id>-<nama>-<buktiDokumen>"}. */
	public String toString() {
		return id + "-" + nama + "-" + buktiDokumen;
	}

	/** Nama/judul kegiatan penunjang kinerja. */
	private String nama;
	/** Uraian bukti kegiatan (teks bebas). */
	private String bukti;
	/** Keterangan bebas. */
	private String keterangan;
	/** Beban SKS yang diklaim dari kegiatan ini. */
	private Double sks;
	/** Masa/periode penugasan (teks bebas). */
	private String masaPenugasan;
	/** Path/nama berkas bukti dokumen (mis. sertifikat, surat tugas) yang diunggah. */
	private String buktiDokumen;
	/** Tautan eksternal ke dokumen bukti (alternatif dari unggah berkas). */
	private String linkDokumen;
	/** Pegawai (dosen) pemilik kegiatan penunjang ini. */
	private Pegawai pegawai;
	/** Tanggal mulai kegiatan; lihat {@link #getTanggalMulai()} untuk perilaku default. */
	private Date tanggalMulai;
	/** Tanggal akhir kegiatan, boleh {@code null}. */
	private Date tanggalSampai;
	/** Kategori Tridharma kegiatan; lihat konstanta {@link #PENDIDIKAN}, {@link #PENELITIAN}, dst. */
	private String jenis;

	/** Semester kegiatan; lihat {@link #getSemester()} untuk perilaku default. */
	private String semester;
	/** Tahun akademik kegiatan; lihat {@link #getTahunAkademik()} untuk perilaku default. */
	private String tahunAkademik;

	/** Konstruktor kosong, dipakai Hibernate. */
	public PenunjangKinerjaDosen() {
	}

	/** @return primary key entity, atau {@code null} bila belum tersimpan. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id primary key baru. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return nama/judul kegiatan, sudah di-{@code trim}; {@code null} bila belum diisi. */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama/judul kegiatan baru. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan bebas, boleh {@code null}. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan bebas yang baru. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return uraian bukti kegiatan (teks bebas). */
	@Column(columnDefinition = "text")
	public String getBukti() {
		return bukti;
	}

	/** @param bukti uraian bukti kegiatan yang baru. */
	public void setBukti(String bukti) {
		this.bukti = bukti;
	}

	/** @return beban SKS yang diklaim, {@code 0} bila belum diisi (bukan {@code null}). */
	@Column(name = "sks_beban", nullable = true)
	public Double getSks() {
		return sks == null ? 0 : sks;
	}

	/** @param sks beban SKS baru. */
	public void setSks(Double sks) {
		this.sks = sks;
	}

	/** @return masa/periode penugasan (teks bebas), boleh {@code null}. */
	public String getMasaPenugasan() {
		return masaPenugasan;
	}

	/** @param masaPenugasan masa/periode penugasan yang baru. */
	public void setMasaPenugasan(String masaPenugasan) {
		this.masaPenugasan = masaPenugasan;
	}

	/** @return tautan eksternal dokumen bukti, string kosong bila belum diisi (bukan {@code null}). */
	@Column(columnDefinition = "text")
	public String getLinkDokumen() {
		return linkDokumen == null ? "" : linkDokumen.trim();
	}

	/** @param linkDokumen tautan eksternal dokumen bukti yang baru. */
	public void setLinkDokumen(String linkDokumen) {
		this.linkDokumen = linkDokumen;
	}

	/** @return path/nama berkas bukti dokumen yang diunggah, boleh {@code null}. */
	@Column(columnDefinition = "text")
	public String getBuktiDokumen() {
		return buktiDokumen;
	}

	/** @param buktiDokumen path/nama berkas bukti dokumen yang baru. */
	public void setBuktiDokumen(String buktiDokumen) {
		this.buktiDokumen = buktiDokumen;
	}

	/** @return tanggal mulai kegiatan; bila belum pernah diisi, DISETEL SEKALIGUS ke hari ini (bukan {@code null}). */
	public Date getTanggalMulai() {
		if (tanggalMulai == null) {
			tanggalMulai = ais.ui.util.WaktuUtil.getDate();
		}
		return tanggalMulai;
	}

	/** @param tanggalMulai tanggal mulai kegiatan yang baru. */
	public void setTanggalMulai(Date tanggalMulai) {
		this.tanggalMulai = tanggalMulai;
	}

	/** @return tanggal akhir kegiatan, boleh {@code null}. */
	public Date getTanggalSampai() {
		return tanggalSampai;
	}

	/** @param tanggalSampai tanggal akhir kegiatan yang baru. */
	public void setTanggalSampai(Date tanggalSampai) {
		this.tanggalSampai = tanggalSampai;
	}

	/** @return kategori Tridharma; default {@link #PENUNJANG} bila kolom kosong. */
	public String getJenis() {
		return jenis == null || jenis.trim().isEmpty() ? PENUNJANG : jenis.trim();
	}

	/** @param jenis kategori Tridharma yang baru. */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/**
	 * @return semester kegiatan; bila belum pernah diisi, DITENTUKAN SEKALI dari
	 *         {@link Common#isNowSemensterGanjil(Date)} atas {@link #getTanggalMulai()} lalu
	 *         disimpan ke field (nilai berikutnya konsisten, tidak dihitung ulang tiap panggilan).
	 */
	public String getSemester() {
		if (semester == null) {
			semester = Common.isNowSemensterGanjil(getTanggalMulai()) ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
		}
		return semester;
	}

	/** @param semester semester baru. */
	public void setSemester(String semester) {
		this.semester = semester;
	}

	/**
	 * @return tahun akademik; bila belum pernah diisi, DITENTUKAN SEKALI dari
	 *         {@link Common#getCurrentTahunAkademik(Date)} atas {@link #getTanggalMulai()} lalu
	 *         disimpan ke field.
	 */
	public String getTahunAkademik() {
		if (tahunAkademik == null) {
			tahunAkademik = Common.getCurrentTahunAkademik(getTanggalMulai());
		}
		return tahunAkademik;
	}

	/** @param tahunAkademik tahun akademik baru. */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/** @return pegawai (dosen) pemilik kegiatan; dimuat lazy lewat sesi Hibernate aktif. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pegawai", nullable = true)
	public Pegawai getPegawai() {
		return pegawai;
	}

	/** @param pegawai pegawai (dosen) pemilik kegiatan yang baru. */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

}
