package ais.database.model.penelitiandanpengabdian;

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



import ais.database.model.GeneralValueObject;

/**
 * Model entitas <b>master tahap pelaporan</b> untuk satu skema {@link PenelitianDanPengabdian}
 * (mis. "Laporan Kemajuan", "Laporan Akhir"): mendefinisikan tahap-tahap laporan yang wajib/boleh
 * diajukan dosen/mahasiswa untuk skema tersebut, masing-masing dengan jendela waktu
 * ({@link #getMulai()}–{@link #getSampai()}) dan penanda aktif ({@link #getAktif()}).
 *
 * <p>
 * Baris di tabel ini adalah <b>master/definisi</b>, bukan pengajuan aktual — realisasi laporan per
 * tahap oleh seorang pengaju disimpan di {@link PengajuanTahapanPelaporanPenelitianDanPengabdian}
 * (relasi many-to-one ke kelas ini). Perubahan pada baris master diaudit otomatis oleh Hibernate
 * Envers ({@code @Audited}) — setiap {@code INSERT}/{@code UPDATE} dicatat sebagai versi baru pada
 * tabel bayangan (shadow) {@code tahapan_pelaporan_penelitian_dan_pengabdian_aud}.
 * </p>
 *
 * @see PengajuanTahapanPelaporanPenelitianDanPengabdian
 * @see PenelitianDanPengabdian
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "penelitiandanpengabdian", name = "tahapan_pelaporan_penelitian_dan_pengabdian")



public class TahapanPelaporanPenelitianDanPengabdian extends GeneralValueObject {

	/**
	 * Versi kelas untuk kebutuhan serialisasi ({@link java.io.Serializable}). Berbeda satu digit
	 * dari {@code serialVersionUID} pada entitas lain di paket ini (mis.
	 * {@link AnggotaPengajuanPenelitianDanPengabdian}) — sisa dari pola salin-tempel hbm2java, tidak
	 * bermakna khusus.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris tahap pelaporan, auto-increment ({@code IDENTITY}) pada kolom {@code id}. */
	private Long id;
	/** Field audit legacy: nama pengguna yang melakukan perubahan terakhir (bebas format, isi manual). Lihat {@link #getOleh()}. */
	private String oleh;
	/** Field audit legacy: id/username pengguna yang melakukan perubahan terakhir. Lihat {@link #getOlehId()}. */
	private String olehId;

	/** @return id/username pengguna yang tercatat melakukan perubahan terakhir pada baris ini (field audit legacy, tidak dipetakan sebagai kolom entitas). */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pencatat perubahan terakhir. Nilai {@code null} atau string kosong/spasi
	 * diabaikan (tidak menimpa nilai yang sudah tersimpan).
	 *
	 * @param olehId id/username pengguna; diabaikan bila {@code null} atau kosong setelah di-trim
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pencatat perubahan terakhir. Nilai {@code null} atau string kosong/spasi
	 * diabaikan, dengan alasan yang sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong setelah di-trim
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pengguna yang tercatat melakukan perubahan terakhir pada baris ini (field audit legacy, tidak dipetakan sebagai kolom entitas). */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil otomatis oleh provider persistence sesaat sebelum
	 * setiap {@code UPDATE} baris ini dieksekusi, mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} untuk memperbarui
	 * {@link #tanggal_dirubah} ke waktu saat ini. Tidak dipanggil manual dari kode aplikasi; catatan
	 * versi Envers (dari {@code @Audited} pada kelas ini) berjalan terpisah dari mekanisme ini.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengatur cap waktu perubahan terakhir secara manual. Dalam alur normal field ini diperbarui
	 * otomatis lewat {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah cap waktu perubahan terakhir yang baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return cap waktu perubahan terakhir baris ini; diinisialisasi ke waktu pembuatan objek dan diperbarui otomatis oleh {@link #onUpdate()} pada setiap {@code UPDATE}. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi teks ringkas baris ini untuk keperluan log/debug/tampilan combobox: nama tahap pelaporan ({@link #getNama()}). */
	public String toString() {
		return nama;
	}

	/** Nama tahap pelaporan (mis. "Laporan Kemajuan", "Laporan Akhir"), wajib diisi. Lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan/deskripsi tambahan untuk tahap pelaporan ini, opsional. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Skema penelitian/pengabdian yang memiliki tahap pelaporan ini (FK wajib). */
	private PenelitianDanPengabdian penelitianDanPengabdian;
	/** Awal jendela waktu tahap ini boleh diajukan. Defaultnya waktu pembuatan objek; lihat {@link #getMulai()} untuk perilaku "tidak pernah null". */
	private Date mulai = ais.ui.util.WaktuUtil.getDate();
	/** Akhir jendela waktu tahap ini boleh diajukan. Defaultnya waktu pembuatan objek; lihat {@link #getSampai()} untuk perilaku "tidak pernah null". */
	private Date sampai = ais.ui.util.WaktuUtil.getDate();
	/** Penanda tahap ini aktif/ditampilkan. {@code null} diperlakukan sebagai aktif oleh {@link #getAktif()} (default aktif satu arah). */
	private Boolean aktif;
	/** Tahap pengajuan proposal induk ({@link PengajuanPenelitianDanPengabdian#TAHAP_PROPOSAL} dkk.) tempat tahap pelaporan ini berlaku. Lihat {@link #getTahapPengajuan()}. */
	private String tahapPengajuan;

	/** Konstruktor default (wajib untuk entitas Hibernate/JPA); seluruh field diisi lewat setter. */
	public TahapanPelaporanPenelitianDanPengabdian() {
	}

	/** @return primary key baris tahap pelaporan ini, atau {@code null} bila belum tersimpan. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengatur id baris ini secara manual. Karena kolom {@code id} dipetakan
	 * {@code insertable = false} (nilai dihasilkan basis data lewat {@code IDENTITY}), pengaturan
	 * manual di sini hanya berguna untuk menandai objek yang mewakili baris yang sudah ada.
	 *
	 * @param id primary key yang ingin diasosiasikan ke objek ini
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return nama tahap pelaporan (mis. "Laporan Kemajuan"), apa adanya (kolom {@code NOT NULL} di basis data). */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama;
	}

	/** @param nama nama baru tahap pelaporan ini. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan/deskripsi tambahan tahap pelaporan ini, apa adanya (boleh {@code null}). */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan/deskripsi baru untuk tahap pelaporan ini; boleh {@code null}. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Getter dengan default satu-arah: bila {@link #aktif} belum pernah diisi ({@code null}, mis.
	 * baris lama sebelum kolom ini ada, atau objek baru yang belum di-set), method ini
	 * <b>menuliskan</b> {@code true} ke field {@link #aktif} lalu mengembalikannya — sekali
	 * ternormalisasi menjadi {@code true}, nilai tidak akan kembali {@code null} pada instance yang
	 * sama. Tahap pelaporan yang secara eksplisit dinonaktifkan ({@code aktif = false}) tetap
	 * dikembalikan apa adanya.
	 *
	 * @return {@code true} bila tahap ini aktif atau belum pernah diatur; {@code false} bila secara eksplisit dinonaktifkan
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/** @param aktif status aktif baru untuk tahap pelaporan ini; {@code null} akan dibaca sebagai aktif oleh {@link #getAktif()}. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/** @return skema penelitian/pengabdian yang memiliki tahap pelaporan ini (FK wajib, tidak pernah {@code null} pada baris tersimpan). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penelitian_dan_pengabdian", nullable = false)
	public PenelitianDanPengabdian getPenelitianDanPengabdian() {
		return penelitianDanPengabdian;
	}

	/** @param penelitianDanPengabdian skema penelitian/pengabdian induk untuk tahap pelaporan ini. */
	public void setPenelitianDanPengabdian(PenelitianDanPengabdian penelitianDanPengabdian) {
		this.penelitianDanPengabdian = penelitianDanPengabdian;
	}

	/**
	 * Getter dengan efek samping "tidak pernah null": bila {@link #mulai} kosong (mis. hasil
	 * {@code refresh}/reload dari baris yang menyimpan {@code NULL}), method ini menuliskan waktu
	 * saat ini ke field {@link #mulai} lalu mengembalikannya, alih-alih mengembalikan {@code null}
	 * apa adanya. Perhatikan bahwa nilai yang "diperbaiki" ini bisa ikut tersimpan bila objek
	 * kemudian di-{@code update} tanpa mengatur ulang {@link #setMulai(Date)} secara eksplisit.
	 *
	 * @return awal jendela waktu tahap ini boleh diajukan; tidak pernah {@code null}
	 */
	@Temporal(TemporalType.DATE)
	public Date getMulai() {
		if (mulai == null) {
			mulai = ais.ui.util.WaktuUtil.getDate();
		}
		return mulai;
	}

	/** @param mulai awal jendela waktu baru tahap ini boleh diajukan. */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Getter dengan efek samping "tidak pernah null", sama seperti {@link #getMulai()}: bila
	 * {@link #sampai} kosong, method ini menuliskan waktu saat ini ke field lalu mengembalikannya.
	 *
	 * @return akhir jendela waktu tahap ini boleh diajukan; tidak pernah {@code null}
	 */
	@Temporal(TemporalType.DATE)
	public Date getSampai() {
		if (sampai == null) {
			sampai = ais.ui.util.WaktuUtil.getDate();
		}
		return sampai;
	}

	/** @param sampai akhir jendela waktu baru tahap ini boleh diajukan. */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * @return tahap pengajuan proposal ({@link PengajuanPenelitianDanPengabdian#TAHAP_PROPOSAL} dkk.)
	 *         tempat tahap pelaporan ini berlaku; {@link PengajuanPenelitianDanPengabdian#TAHAP_PROPOSAL}
	 *         dipakai sebagai default bila field belum diisi atau kosong setelah di-trim.
	 */
	public String getTahapPengajuan() {
		return tahapPengajuan == null || tahapPengajuan.trim().isEmpty()
				? PengajuanPenelitianDanPengabdian.TAHAP_PROPOSAL : tahapPengajuan;
	}

	/** @param tahapPengajuan tahap pengajuan baru tempat tahap pelaporan ini berlaku. */
	public void setTahapPengajuan(String tahapPengajuan) {
		this.tahapPengajuan = tahapPengajuan;
	}

}
