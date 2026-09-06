package ais.database.model.spi;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

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
import ais.database.model.Tbmuser;

/**
 * <h2>TindakLanjutAuditSPI &mdash; Realisasi Tindak Lanjut Auditee atas Satu Temuan</h2>
 *
 * <p>
 * Mencatat apa yang SESUNGGUHNYA DILAKUKAN unit yang diaudit (auditee) dalam merespons
 * {@link TemuanAuditSPI#getRekomendasi() rekomendasi} yang diberikan auditor. Satu temuan bisa
 * memiliki BANYAK baris tindak lanjut &mdash; dicatat progresif dari waktu ke waktu seiring
 * pelaksanaan perbaikan berjalan (mis. "diajukan draf SOP baru" bulan ini, "SOP baru disahkan"
 * bulan berikutnya), sehingga riwayat perkembangan penanganan satu temuan tetap lengkap terekam,
 * bukan hanya status akhir. Lihat javadoc {@link TemuanAuditSPI} untuk penjelasan lengkap kenapa
 * Rekomendasi (auditor) dan Tindak Lanjut (auditee) SENGAJA dipisah menjadi dua entity berbeda.
 * </p>
 *
 * <p>
 * Struktur field kelas ini SENGAJA meniru persis {@code ais.database.model.spmi.
 * TindakLanjutTemuanSPMI} yang sudah production-proven di modul Audit Mutu Internal akademik,
 * karena kebutuhannya memang identik: deskripsi tindakan, penanggung jawab (PIC), target &amp;
 * tanggal realisasi, persentase kemajuan, dan status generik (Belum Dimulai/Sedang Berjalan/
 * Terlambat/Selesai) yang tidak spesifik-domain sehingga bisa dipakai apa adanya di konteks audit
 * internal maupun audit mutu akademik.
 * </p>
 *
 * <h3>Kelas ini TIDAK memakai mesin persetujuan AlurSop/DisposisiSop</h3>
 * <p>
 * Berbeda dari {@link PenugasanAuditSPI} (yang extends {@code DataSop} dan otomatis mendapat alur
 * persetujuan berjenjang lewat mesin SOP/Disposisi), kelas ini extends {@link GeneralValueObject}
 * BIASA &mdash; TIDAK ada relasi apapun ke {@code DisposisiSop}/{@code AlurSop}. Ini BUKAN celah
 * bypass-persetujuan mesin SOP generik (berbeda kasus dari isu itu): di sini memang SEJAK AWAL
 * tidak ada mesin persetujuan berjenjang yang dilewati &mdash; kebutuhannya jauh lebih ringan
 * (satu langkah verifikasi independen, bukan alur berjenjang penuh), sehingga ditangani lewat
 * pasangan field {@link #getDiverifikasiOleh()}/{@link #getTanggalVerifikasi()} di bawah, BUKAN
 * dengan menyeret seluruh mesin AlurSop/DisposisiSop untuk kasus sesederhana ini.
 * </p>
 *
 * <h3>FIX (task_fcc03cad): verifikasi independen SPI sebelum status SELESAI final</h3>
 * <p>
 * Sebelum perbaikan ini, {@link ais.action.master.spi.TindakLanjutAuditSPIAction#buildAddForm}
 * memungkinkan SIAPAPUN yang punya akses ke modul SPI (gerbangnya hanya pemeriksaan keamanan
 * generik di level menu, TANPA scoping peran auditor vs auditee) untuk langsung mencatat status
 * {@link #SELESAI} pada satu tindak lanjut TANPA ada langkah verifikasi terpisah oleh auditor/SPI
 * yang menyatakan perbaikan tersebut benar-benar memadai &mdash; berlawanan dengan semangat Three
 * Lines Model yang menjadi prinsip dasar {@link PenugasanAuditSPI}. Perbaikannya: field
 * {@link #getStatus()} tetap bisa diisi bebas oleh siapapun yang mencatat progres (klaim auditee
 * TETAP terekam apa adanya sebagai riwayat, lihat javadoc kelas bagian "riwayat progresif" di
 * atas), NAMUN status {@link #SELESAI} baru dianggap FINAL/terverifikasi bila
 * {@link #getDiverifikasiOleh()} bukan {@code null} &mdash; lihat {@link #isSelesaiTerverifikasi()}.
 * Pengisian {@code diverifikasiOleh}/{@code tanggalVerifikasi} dibatasi pada method
 * {@code TindakLanjutAuditSPIAction#onVerifikasi} kepada anggota {@link TimAuditSPI} aktif pada
 * {@link TemuanAuditSPI#getPenugasanAuditSPI() penugasan} yang menaungi temuan ini (atau admin
 * lain), BUKAN sembarang pengguna bermodul SPI &mdash; pola yang lebih ringan dari
 * {@link PenugasanAuditSPI#getDisetujuiOleh()} (yang mensyaratkan pihak KEDUA lewat mesin SOP
 * penuh) tapi tetap menjaga independensi pihak yang menyatakan "selesai" dari pihak yang
 * ditindaklanjuti.
 * </p>
 *
 * @author e-Campus SPI Team
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "tindak_lanjut_audit_spi")
public class TindakLanjutAuditSPI extends GeneralValueObject {

	/** Kode status "belum ada tindakan yang dimulai" &mdash; nilai default {@link #getStatus()}. */
	public static final String BELUM_DIMULAI = "Belum Dimulai";
	/** Kode status "tindakan perbaikan sedang dikerjakan auditee, belum tuntas". */
	public static final String SEDANG_BERJALAN = "Sedang Berjalan";
	/** Kode status "target penyelesaian sudah lewat namun belum selesai". CATATAN: status ini
	 * TIDAK dihitung/disetel otomatis oleh sistem berdasarkan perbandingan {@link #getTargetDate()}
	 * dengan tanggal berjalan &mdash; harus dipilih manual oleh pengisi form. */
	public static final String TERLAMBAT = "Terlambat";
	/** Kode status "tindakan perbaikan dinyatakan tuntas" &mdash; klaim ini bisa disetel langsung
	 * oleh siapapun yang mengisi form (riwayat progresif tetap terekam apa adanya), NAMUN baru
	 * dianggap FINAL setelah diverifikasi independen; lihat {@link #isSelesaiTerverifikasi()} dan
	 * javadoc kelas bagian "FIX (task_fcc03cad)". */
	public static final String SELESAI = "Selesai";

	/** Peta kode status &rarr; label bahasa manusia (di sini kode dan label sengaja identik),
	 * sumber tunggal untuk dropdown pilihan status di form tindak lanjut. */
	public static final Map<String, String> statusLabel = new LinkedHashMap<String, String>();
	static {
		statusLabel.put(BELUM_DIMULAI, BELUM_DIMULAI);
		statusLabel.put(SEDANG_BERJALAN, SEDANG_BERJALAN);
		statusLabel.put(TERLAMBAT, TERLAMBAT);
		statusLabel.put(SELESAI, SELESAI);
	}

	/*
	 * Kolom BARU pada tabel LAMA yang sudah ber-@Audited (tindak_lanjut_audit_spi). Kolom utama
	 * (public.tindak_lanjut_audit_spi) diserahkan ke Hibernate hbm2ddl=update, TAPI tabel audit
	 * Envers (new_audit.tindak_lanjut_audit_spi__audit) TIDAK ikut otomatis -- jalankan manual
	 * sekali sebelum baris pertama dengan field ini tersimpan:
	 *   ALTER TABLE public.tindak_lanjut_audit_spi ADD COLUMN diverifikasi_oleh bigint;
	 *   ALTER TABLE public.tindak_lanjut_audit_spi ADD COLUMN tanggal_verifikasi timestamp;
	 *   ALTER TABLE new_audit.tindak_lanjut_audit_spi__audit ADD COLUMN diverifikasi_oleh bigint;
	 *   ALTER TABLE new_audit.tindak_lanjut_audit_spi__audit ADD COLUMN tanggal_verifikasi timestamp;
	 */
	private static final long serialVersionUID = 1L;
	private Long id;
	private TemuanAuditSPI temuanAuditSPI;
	private String deskripsi;
	private String picNama;
	private Date targetDate;
	private Date tanggalSelesai;
	private int progressPersen;
	private String status;
	private String keterangan;
	private Tbmuser diverifikasiOleh;
	private Date tanggalVerifikasi;
	private Boolean aktif;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor tanpa argumen, wajib ada agar Hibernate dapat menginstansiasi entity ini. */
	public TindakLanjutAuditSPI() {
	}

	/**
	 * Konstruktor kenyamanan untuk langsung mengaitkan baris tindak lanjut baru ke satu temuan
	 * yang ditindaklanjuti, dipakai oleh
	 * {@link ais.action.master.spi.TindakLanjutAuditSPIAction#buildAddForm} saat mencatat entri
	 * baru dari panel tindak lanjut satu temuan.
	 *
	 * @param temuanAuditSPI temuan yang ditindaklanjuti oleh baris ini.
	 */
	public TindakLanjutAuditSPI(TemuanAuditSPI temuanAuditSPI) {
		this.temuanAuditSPI = temuanAuditSPI;
	}

	/**
	 * Callback JPA {@code @PreUpdate}, dipanggil otomatis Hibernate sebelum UPDATE, mendelegasikan
	 * ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} untuk menyegarkan
	 * {@link #getTanggal_dirubah()} secara otomatis, tanpa kode aplikasi perlu mengelolanya manual.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * ID primer baris ini, di-generate otomatis oleh database (strategi {@code IDENTITY}).
	 *
	 * @return ID unik baris ini, atau {@code null} bila entity belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/**
	 * Mengisi ID baris ini secara manual, terutama saat membangun objek referensi ringan untuk
	 * relasi {@code JoinColumn} tanpa memuat seluruh baris dari database.
	 *
	 * @param id ID baris yang akan diisi.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Temuan audit yang ditindaklanjuti oleh baris ini &mdash; satu temuan bisa memiliki BANYAK
	 * baris tindak lanjut (dicatat progresif dari waktu ke waktu), lihat javadoc kelas. Relasi
	 * wajib ({@code nullable = false}).
	 *
	 * @return temuan audit yang ditindaklanjuti.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "temuan_audit_spi", nullable = false)
	public TemuanAuditSPI getTemuanAuditSPI() {
		temuanAuditSPI = check(temuanAuditSPI);
		return temuanAuditSPI;
	}

	/**
	 * Mengaitkan baris tindak lanjut ini ke satu temuan. SENGAJA menolak (mengabaikan diam-diam)
	 * argumen yang {@code null} atau belum memiliki ID tersimpan &mdash; mencegah baris tindak
	 * lanjut kehilangan tautan wajibnya ke temuan hanya karena dipanggil dengan objek yang belum
	 * sempat di-persist.
	 *
	 * @param temuanAuditSPI temuan baru yang ditindaklanjuti; diabaikan bila {@code null} atau
	 *        belum memiliki ID (belum tersimpan).
	 */
	public void setTemuanAuditSPI(TemuanAuditSPI temuanAuditSPI) {
		if (temuanAuditSPI != null && temuanAuditSPI.getId() != null) {
			this.temuanAuditSPI = temuanAuditSPI;
		}
	}

	/**
	 * Uraian tindakan nyata yang sudah/sedang dilakukan auditee dalam merespons rekomendasi
	 * auditor. Nilai dikembalikan sudah di-{@code trim()}, dan kolom ini WAJIB terisi (
	 * {@code nullable = false}) &mdash; formulir {@link ais.action.master.spi.TindakLanjutAuditSPIAction}
	 * menolak simpan bila kosong.
	 *
	 * @return uraian tindak lanjut yang sudah dipangkas spasinya; string kosong bila belum diisi.
	 */
	@Column(name = "deskripsi", nullable = false, columnDefinition = "text")
	public String getDeskripsi() {
		return deskripsi == null ? "" : deskripsi.trim();
	}

	/**
	 * Mengisi uraian tindak lanjut ini.
	 *
	 * @param deskripsi uraian tindakan baru.
	 */
	public void setDeskripsi(String deskripsi) {
		this.deskripsi = deskripsi;
	}

	/**
	 * Nama penanggung jawab (PIC) pelaksanaan tindak lanjut ini di sisi auditee. SENGAJA berupa
	 * kolom teks bebas (bukan relasi ke {@link ais.database.model.Tbmuser}) karena PIC yang
	 * ditunjuk auditee tidak harus memiliki akun pengguna di aplikasi ini.
	 *
	 * @return nama PIC, atau {@code null} bila belum diisi.
	 */
	@Column(name = "pic_nama")
	public String getPicNama() {
		return picNama;
	}

	/**
	 * Mengisi nama PIC penanggung jawab tindak lanjut ini.
	 *
	 * @param picNama nama PIC baru.
	 */
	public void setPicNama(String picNama) {
		this.picNama = picNama;
	}

	/**
	 * Target tanggal penyelesaian tindak lanjut ini, dijanjikan oleh auditee saat pertama kali
	 * dicatat.
	 *
	 * @return target tanggal penyelesaian, atau {@code null} bila belum ditentukan.
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "target_date")
	public Date getTargetDate() {
		return targetDate;
	}

	/**
	 * Mengisi target tanggal penyelesaian tindak lanjut ini.
	 *
	 * @param targetDate target tanggal baru.
	 */
	public void setTargetDate(Date targetDate) {
		this.targetDate = targetDate;
	}

	/**
	 * Tanggal aktual tindak lanjut ini benar-benar dinyatakan selesai. CATATAN: field ini TIDAK
	 * otomatis diisi saat {@link #getStatus()} disetel ke {@link #SELESAI} &mdash; pengisian
	 * keduanya (status dan tanggal) sepenuhnya independen dan bergantung pada apa yang dimasukkan
	 * manual lewat form, tidak ada validasi silang di level entity ini.
	 *
	 * @return tanggal selesai aktual, atau {@code null} bila belum diisi.
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_selesai")
	public Date getTanggalSelesai() {
		return tanggalSelesai;
	}

	/**
	 * Mengisi tanggal aktual penyelesaian tindak lanjut ini.
	 *
	 * @param tanggalSelesai tanggal selesai baru.
	 */
	public void setTanggalSelesai(Date tanggalSelesai) {
		this.tanggalSelesai = tanggalSelesai;
	}

	/**
	 * Persentase kemajuan pelaksanaan tindak lanjut ini (0&ndash;100), ditampilkan sebagai bilah
	 * progres pada panel riwayat &mdash; lihat
	 * {@link ais.action.master.spi.TindakLanjutAuditSPIAction} method {@code buildRows}.
	 *
	 * @return persentase kemajuan, selalu dalam rentang 0&ndash;100 (lihat {@link #setProgressPersen(int)}).
	 */
	@Column(name = "progress_persen", nullable = false)
	public int getProgressPersen() {
		return progressPersen;
	}

	/**
	 * Mengisi persentase kemajuan tindak lanjut ini. Nilai SENGAJA di-<i>clamp</i> (dipaksa masuk
	 * rentang) ke 0&ndash;100 di sini &mdash; input di luar rentang (mis. dari kesalahan ketik
	 * atau data impor) tidak akan pernah tersimpan sebagai nilai tidak masuk akal seperti -5 atau
	 * 150.
	 *
	 * @param progressPersen persentase kemajuan baru; nilai di luar 0&ndash;100 dipangkas ke batas
	 *        terdekat.
	 */
	public void setProgressPersen(int progressPersen) {
		this.progressPersen = Math.max(0, Math.min(100, progressPersen));
	}

	/**
	 * Status pelaksanaan tindak lanjut ini, salah satu dari {@link #BELUM_DIMULAI}/
	 * {@link #SEDANG_BERJALAN}/{@link #TERLAMBAT}/{@link #SELESAI}. CATATAN: nilai {@link #SELESAI}
	 * di sini adalah KLAIM (siapapun yang mencatat progres boleh mengisinya) &mdash; gunakan
	 * {@link #isSelesaiTerverifikasi()}, bukan {@code SELESAI.equals(getStatus())}, untuk memeriksa
	 * apakah klaim tersebut sudah diverifikasi independen. Default {@link #BELUM_DIMULAI} bila
	 * belum diisi.
	 *
	 * @return kode status; {@link #BELUM_DIMULAI} bila nilai tersimpan {@code null}, selalu
	 *         di-{@code trim()} bila ada isinya.
	 */
	@Column(name = "status")
	public String getStatus() {
		return status == null ? BELUM_DIMULAI : status.trim();
	}

	/**
	 * Mengisi status pelaksanaan tindak lanjut ini TANPA validasi transisi apapun (mis. bisa
	 * langsung melompat dari {@link #BELUM_DIMULAI} ke {@link #SELESAI} tanpa melalui
	 * {@link #SEDANG_BERJALAN}) &mdash; lihat javadoc kelas untuk catatan lengkap soal
	 * ketiadaan gerbang verifikasi pada field ini.
	 *
	 * @param status kode status baru, idealnya salah satu dari {@link #BELUM_DIMULAI}/
	 *        {@link #SEDANG_BERJALAN}/{@link #TERLAMBAT}/{@link #SELESAI}.
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Menentukan apakah status {@link #SELESAI} pada baris ini sudah FINAL/terverifikasi &mdash;
	 * lihat javadoc kelas bagian "FIX (task_fcc03cad)". {@code true} hanya bila
	 * {@link #getStatus()} bernilai {@link #SELESAI} DAN {@link #getDiverifikasiOleh()} bukan
	 * {@code null}. Status {@link #SELESAI} tanpa verifikasi (klaim sepihak auditee) SENGAJA
	 * dianggap belum final oleh method ini, meski nilai mentah {@link #getStatus()} sendiri sudah
	 * berbunyi "Selesai" &mdash; kode tampilan yang perlu membedakan "diklaim selesai" vs "benar
	 * -benar tuntas" WAJIB memakai method ini, bukan membandingkan {@link #getStatus()} secara
	 * langsung dengan {@link #SELESAI}.
	 *
	 * @return {@code true} bila status {@link #SELESAI} DAN sudah diverifikasi.
	 */
	@javax.persistence.Transient
	public boolean isSelesaiTerverifikasi() {
		return SELESAI.equals(getStatus()) && getDiverifikasiOleh() != null;
	}

	/**
	 * Pengguna SPI/auditor yang memverifikasi independen bahwa tindak lanjut ini benar-benar
	 * memadai, atau {@code null} bila belum ada verifikasi. Lihat javadoc kelas bagian "FIX
	 * (task_fcc03cad)" untuk alasan lengkap keberadaan field ini dan {@link #isSelesaiTerverifikasi()}
	 * untuk cara memeriksa apakah status {@link #SELESAI} baris ini sudah final. Pengisian field ini
	 * DIBATASI oleh {@code TindakLanjutAuditSPIAction#onVerifikasi} kepada anggota aktif
	 * {@link TimAuditSPI} pada penugasan yang menaungi temuan ini (atau admin lain) &mdash; entity
	 * ini sendiri tidak memvalidasi siapa yang berhak, murni penyimpanan nilai.
	 *
	 * @return pengguna yang melakukan verifikasi, atau {@code null} bila belum diverifikasi.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "diverifikasi_oleh", nullable = true)
	public Tbmuser getDiverifikasiOleh() {
		return diverifikasiOleh;
	}

	/**
	 * Mengisi pengguna yang melakukan verifikasi independen tindak lanjut ini. Pemanggil (bukan
	 * entity ini) bertanggung jawab memastikan pengguna yang diisikan berhak melakukannya &mdash;
	 * lihat javadoc {@link #getDiverifikasiOleh()}.
	 *
	 * @param diverifikasiOleh pengguna verifikator baru; {@code null} untuk mencabut verifikasi
	 *        (mis. bila baris tindak lanjut ini direvisi/dibuka kembali).
	 */
	public void setDiverifikasiOleh(Tbmuser diverifikasiOleh) {
		this.diverifikasiOleh = diverifikasiOleh;
	}

	/**
	 * Tanggal/waktu verifikasi independen dilakukan, atau {@code null} bila belum diverifikasi.
	 *
	 * @return tanggal verifikasi, atau {@code null} bila belum diverifikasi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_verifikasi")
	public Date getTanggalVerifikasi() {
		return tanggalVerifikasi;
	}

	/**
	 * Mengisi tanggal/waktu verifikasi independen tindak lanjut ini.
	 *
	 * @param tanggalVerifikasi tanggal verifikasi baru.
	 */
	public void setTanggalVerifikasi(Date tanggalVerifikasi) {
		this.tanggalVerifikasi = tanggalVerifikasi;
	}

	/**
	 * Keterangan bebas tambahan mengenai tindak lanjut ini, mis. kendala pelaksanaan atau catatan
	 * tambahan yang tidak tercakup di {@link #getDeskripsi()}.
	 *
	 * @return teks keterangan, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Mengisi keterangan bebas untuk tindak lanjut ini.
	 *
	 * @param keterangan teks keterangan baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Status aktif/nonaktif baris tindak lanjut ini; nilai {@code null} SENGAJA diperlakukan
	 * sebagai {@code true} (aktif) demi kompatibilitas data lama &mdash; konvensi baku entity di
	 * aplikasi ini.
	 *
	 * @return {@code true} bila baris tindak lanjut ini aktif (termasuk saat nilai tersimpan
	 *         {@code null}).
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengubah status aktif/nonaktif baris tindak lanjut ini. Menonaktifkan (bukan menghapus)
	 * dianjurkan agar riwayat progresif tindak lanjut yang salah entri tetap tersimpan sebagai
	 * jejak, hanya disembunyikan dari tampilan.
	 *
	 * @param aktif status baru; {@code null} diperlakukan sebagai aktif oleh {@link #getAktif()}.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah baris ini. Field SHADOW dari riwayat Envers
	 * ({@code @Audited} pada kelas ini) &mdash; KEHARUSAN TEKNIS untuk menampilkan "terakhir diubah
	 * oleh siapa" secara murah di layar daftar tanpa query terpisah ke tabel riwayat revisi.
	 *
	 * @return nama pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Mengisi nama pengguna yang mengubah baris ini; nilai kosong/blank sengaja diabaikan agar
	 * tidak menimpa jejak yang sudah tercatat.
	 *
	 * @param oleh nama pengguna; {@code null} atau string kosong/spasi diabaikan.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) return;
		this.oleh = oleh;
	}

	/**
	 * Mengambil ID pengguna yang terakhir mengubah baris ini.
	 *
	 * @return ID pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi ID pengguna yang mengubah baris ini; nilai kosong/blank sengaja diabaikan.
	 *
	 * @param olehId ID pengguna; {@code null} atau string kosong/spasi diabaikan.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) return;
		this.olehId = olehId;
	}

	/**
	 * Mengambil waktu terakhir baris ini diubah.
	 *
	 * @return waktu perubahan terakhir baris ini.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Mengisi manual waktu terakhir baris ini diubah; dalam praktiknya disegarkan otomatis lewat
	 * {@link #onUpdate()} pada tiap UPDATE.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat baris tindak lanjut ini (format {@code "<id>-<deskripsi>"}) untuk
	 * log/debug.
	 *
	 * @return string gabungan ID dan uraian tindak lanjut.
	 */
	@Override
	public String toString() {
		return id + "-" + deskripsi;
	}
}
