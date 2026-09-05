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



import ais.database.model.GeneralValueObject;

/**
 * Model entitas <b>pengajuan laporan aktual</b> untuk satu {@link TahapanPelaporanPenelitianDanPengabdian}
 * (tahap master, mis. "Laporan Kemajuan") pada satu proposal {@link PengajuanPenelitianDanPengabdian}:
 * dosen/mahasiswa mengunggah catatan (dan opsional berkas, lihat
 * {@link FilePengajuanTahapanPelaporanPenelitianDanPengabdian}) untuk tahap tertentu, lalu atasan
 * (dosen yang menjadi atasan pengaju) atau koresponden yang ditunjuk pada skema penelitian/pengabdian
 * terkait memberi status {@link #DISETUJUI}/{@link #DITOLAK} lewat kombobox pada layar daftar
 * pengajuan tahap.
 *
 * <p>
 * <b>Perhatian — gerbang persetujuan hanya diperiksa saat render UI:</b> berbeda dengan
 * {@link PengajuanPenelitianDanPengabdian} (yang menautkan status persetujuan proposalnya ke alur
 * disposisi SOP lewat {@code DisposisiSop}), field {@link #status} pada kelas ini adalah string
 * bebas yang ditulis langsung oleh {@link #setStatus(String)} tanpa validasi maupun keterkaitan ke
 * entitas workflow apa pun. Otorisasi "siapa boleh menyetujui" — cek
 * {@code Dosen.yangLoginMerupakanAtasan()} atau keanggotaan pada daftar koresponden skema —
 * <b>hanya dievaluasi satu kali saat baris grid dirender</b>
 * ({@code PengajuanTahapanPelaporanPenelitianDanPengabdianHelper.DetailPengajuanTahapanPelaporanPenelitianDanPengabdianRenderer.render}),
 * untuk memutuskan apakah menampilkan kombobox status yang bisa diedit atau label baca-saja.
 * Listener {@code onChange} kombobox tersebut memanggil {@link #setStatus(String)} dan
 * {@code Common.refreshUpdate(...)} <b>tanpa mengulang pemeriksaan otorisasi itu</b>. Ini adalah
 * pola yang sama dengan kerentanan "gerbang persetujuan UI-only" yang sudah dikonfirmasi pada tiga
 * domain independen lain (kepegawaian, dua alur persuratan) — lihat catatan arsitektur javadoc
 * project ini.
 * </p>
 *
 * @see TahapanPelaporanPenelitianDanPengabdian
 * @see PengajuanPenelitianDanPengabdian
 * @see FilePengajuanTahapanPelaporanPenelitianDanPengabdian
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Table(schema = "penelitiandanpengabdian", name = "pengajuan_tahapan_pelaporan_penelitian_dan_pengabdian")



public class PengajuanTahapanPelaporanPenelitianDanPengabdian extends GeneralValueObject {

	/** Status awal: laporan tahap sudah diajukan tetapi belum ditinjau siapa pun. Nilai default {@link #getStatus()} bila field belum pernah diatur. */
	public static final String BELUM_DIPROSES = "Belum Diproses";
	/** Status transisi: laporan tahap sedang ditinjau (dipilih manual lewat kombobox status, tidak diset otomatis oleh kode lain di paket ini). */
	public static final String SEDANG_DIPROSES = "Sedang Diproses";
	/** Status akhir: laporan tahap ditolak oleh atasan/koresponden peninjau. */
	public static final String DITOLAK = "Ditolak";
	/** Status akhir: laporan tahap disetujui. Begitu status ini tersimpan, form pengajuan (lihat helper terkait) dikunci baca-saja dan tombol hapus dinonaktifkan. */
	public static final String DISETUJUI = "Disetujui";

	/**
	 * Versi kelas untuk kebutuhan serialisasi ({@link java.io.Serializable}). Nilai ini identik
	 * dengan {@code serialVersionUID} pada beberapa entitas lain di paket ini — sisa pola
	 * salin-tempel hbm2java, tidak bermakna khusus.
	 */
	private static final long serialVersionUID = 2463812577548439808L;
	/** Primary key baris pengajuan laporan tahap, auto-increment ({@code IDENTITY}) pada kolom {@code id}. */
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
	 * {@link #tanggal_dirubah} ke waktu saat ini. Tidak dipanggil manual dari kode aplikasi.
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

	/** @return representasi teks ringkas baris ini untuk keperluan log/debug: gabungan {@code toString()} tahap pelaporan terkait dan {@link #status} (tanpa pemisah). */
	public String toString() {
		return tahapanPelaporanPenelitianDanPengabdian + status;
	}

	/** Catatan/isi laporan untuk tahap ini, diisi lewat editor kaya (CKEditor) pada form pengajuan. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Tahap pelaporan master (definisi tahap, mis. "Laporan Kemajuan") yang diajukan lewat baris ini (FK wajib). */
	private TahapanPelaporanPenelitianDanPengabdian tahapanPelaporanPenelitianDanPengabdian;
	/** Proposal pengajuan penelitian/pengabdian yang menjadi induk laporan tahap ini (FK wajib). */
	private PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdian;
	/** URL publik unduhan berkas laporan tahap terbaru ({@code FilePengajuanPengajuanTahapanPelaporanPenelitianDanPengabdian?id=...}), dibentuk asinkron setelah berkas tersimpan. Lihat {@link #getPathUrl()}. */
	private String pathUrl;

	/** Status peninjauan laporan tahap ini ({@link #BELUM_DIPROSES}/{@link #SEDANG_DIPROSES}/{@link #DITOLAK}/{@link #DISETUJUI}). Lihat catatan keamanan pada javadoc kelas mengenai bagaimana field ini diubah. */
	private String status;

	/** Konstruktor default (wajib untuk entitas Hibernate/JPA); seluruh field diisi lewat setter. */
	public PengajuanTahapanPelaporanPenelitianDanPengabdian() {
	}

	/** @return primary key baris pengajuan laporan tahap ini, atau {@code null} bila belum tersimpan. */
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

	/** @return catatan/isi laporan tahap ini, apa adanya (boleh {@code null}, tidak di-trim). */
	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan catatan/isi laporan baru untuk tahap ini; boleh {@code null}. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @param tahapanPelaporanPenelitianDanPengabdian tahap pelaporan master yang diajukan lewat baris ini. */
	public void setTahapanPelaporanPenelitianDanPengabdian(
			TahapanPelaporanPenelitianDanPengabdian tahapanPelaporanPenelitianDanPengabdian) {
		this.tahapanPelaporanPenelitianDanPengabdian = tahapanPelaporanPenelitianDanPengabdian;
	}

	/** @return tahap pelaporan master (definisi tahap) yang diajukan lewat baris ini (FK wajib, tidak pernah {@code null} pada baris tersimpan). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "tahapan_pelaporan_penelitian_dan_pengabdian", nullable = false)
	public TahapanPelaporanPenelitianDanPengabdian getTahapanPelaporanPenelitianDanPengabdian() {
		return tahapanPelaporanPenelitianDanPengabdian;
	}

	/**
	 * @return status peninjauan laporan tahap ini; {@link #BELUM_DIPROSES} dipakai sebagai default
	 *         bila field belum pernah diatur (dan ditulis balik ke field agar konsisten pada
	 *         pemanggilan berikutnya). Lihat catatan keamanan pada javadoc kelas: tidak ada
	 *         pemeriksaan bahwa transisi ke {@link #DISETUJUI}/{@link #DITOLAK} berasal dari
	 *         pemanggil yang berwenang — validasi itu, bila ada, dilakukan di lapisan pemanggil.
	 */
	public String getStatus() {
		if (status == null) {
			status = BELUM_DIPROSES;
		}
		return status;
	}

	/**
	 * Mengatur status peninjauan laporan tahap ini secara langsung, tanpa validasi nilai maupun
	 * pemeriksaan otorisasi apa pun di level entitas — lihat catatan keamanan pada javadoc kelas.
	 *
	 * @param status status baru, idealnya salah satu dari {@link #BELUM_DIPROSES}/{@link #SEDANG_DIPROSES}/{@link #DITOLAK}/{@link #DISETUJUI}
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/** @return proposal pengajuan penelitian/pengabdian yang menjadi induk laporan tahap ini (FK wajib, tidak pernah {@code null} pada baris tersimpan). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pengajuan_penelitian_dan_pengabdian", nullable = false)
	public PengajuanPenelitianDanPengabdian getPengajuanPenelitianDanPengabdian() {
		return pengajuanPenelitianDanPengabdian;
	}

	/** @param pengajuanPenelitianDanPengabdian proposal pengajuan induk untuk laporan tahap ini. */
	public void setPengajuanPenelitianDanPengabdian(PengajuanPenelitianDanPengabdian pengajuanPenelitianDanPengabdian) {
		this.pengajuanPenelitianDanPengabdian = pengajuanPenelitianDanPengabdian;
	}

	/** @return URL publik unduhan berkas laporan tahap terbaru, atau {@code null} bila belum ada berkas yang diunggah/URL belum dibentuk. */
	public String getPathUrl() {
		return pathUrl;
	}

	/** @param pathUrl URL publik unduhan berkas laporan tahap terbaru. */
	public void setPathUrl(String pathUrl) {
		this.pathUrl = pathUrl;
	}

}
