package ais.database.model.penelitiandanpengabdian;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;
import org.json.JSONObject;

import ais.common.Common;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.ScholarArticle;
import ais.database.model.SintaArticle;
import ais.database.model.Tbmuser;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;

/**
 * Entitas artikel/publikasi ilmiah dosen (tabel {@code penelitiandanpengabdian.artikel}) — pusat
 * dari klaster "artikel &amp; publikasi ilmiah" pada modul penelitian dan pengabdian masyarakat.
 * Satu baris mewakili satu artikel yang diajukan/dicatat oleh dosen ({@link #tbmuser}) atau
 * mahasiswa ({@link #mahasiswa}), lengkap dengan metadata bibliografis (ISSN/e-ISSN, volume,
 * nomor, tahun, sitasi, kata kunci, bahasa), berkas pendukung ({@link FileArtikel}), daftar
 * penulis/kontributor ({@link AnggotaArtikel}), tingkat publikasi ({@link #tingkatArtikeles}), dan
 * status indeksasi jurnal ({@link #artikelTerindekses}).
 *
 * <p><b>Integrasi sumber eksternal:</b> sebuah artikel dapat ditautkan ke {@link ScholarArticle}
 * (hasil pengambilan data Google Scholar) dan/atau {@link SintaArticle} (hasil pengambilan data
 * SINTA/Science and Technology Index Kemdikbud). Ketika salah satu tertaut, beberapa getter
 * bibliografis ({@link #getJudul()}, {@link #getAbstrak()}, {@link #getTahun()},
 * {@link #getReferensi()}, {@link #getVol()}, {@link #getNomor()},
 * {@link #getEditorDanKontributor()}) diam-diam <b>menimpa</b> nilai lokal dengan data dari sumber
 * eksternal tersebut setiap kali dipanggil — nilai yang disetel manual lewat setter hanya dipakai
 * selama belum ada tautan Scholar/SINTA, atau untuk bidang yang sumber eksternalnya tidak mengisi
 * data tersebut.</p>
 *
 * <p><b>Alur persetujuan lewat SOP:</b> kelas ini mewarisi {@link DataSop} sehingga memiliki
 * {@link #getDisposisiSop()}/{@link #setDisposisiSop(DisposisiSop)}. Siapa yang mengajukan
 * ({@link #getDiajukanOleh()}), siapa yang menyetujui ({@link #getDisetujuiOleh()}), dan kapan
 * disetujui ({@link #getSetujuiTanggal()}) semuanya diturunkan dari rantai disposisi
 * {@link DisposisiSop} bila tersedia, menimpa field mentah yang mungkin disetel manual — pola yang
 * sama dipakai entitas SOP lain di seluruh aplikasi. Lihat {@link #getStatus()} untuk bagaimana
 * status ringkas ({@link #BELUM_DIPROSES}/{@link #SEDANG_DIPROSES}/{@link #DITOLAK}/
 * {@link #DISETUJUI}) diturunkan dari kombinasi disposisi dan {@link #getArticleId()}.</p>
 *
 * <h2>Bidang audit bayangan</h2>
 * <p>{@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} beserta {@link #onUpdate()} adalah
 * keharusan teknis agar {@code AuditTimestampInterceptor} dapat bekerja, bukan duplikasi yang bisa
 * dihapus. Setternya sengaja mengabaikan masukan kosong agar jejak audit yang sudah ada tidak
 * tertimpa string kosong dari jalur salin/klon objek.</p>
 *
 * @see AnggotaArtikel
 * @see FileArtikel
 * @see ArtikelTerindeks
 * @see TingkatArtikel
 * @see TahapanPenyusunanArtikel
 * @see JurnalPenelitian
 * @see ScholarArticle
 * @see SintaArticle
 * @see DisposisiSop
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "penelitiandanpengabdian", name = "artikel")
public class Artikel extends DataSop {

	/** Nilai {@link #getStatus()} ketika artikel belum diproses sama sekali (belum ada disposisi/persetujuan dan {@link #getArticleId()} masih berupa sentinel negatif). */
	public static final String BELUM_DIPROSES = "Belum Diproses";
	/** Nilai status "sedang diproses"; dideklarasikan sebagai konstanta tersedia namun saat ini tidak pernah dipakai oleh {@link #getStatus()} — status berjalan langsung dari {@link #BELUM_DIPROSES} ke {@link #DISETUJUI}. */
	public static final String SEDANG_DIPROSES = "Sedang Diproses";
	/** Nilai status penolakan; dideklarasikan tersedia namun tidak pernah diset otomatis oleh {@link #getStatus()} — hanya relevan bila disetel manual lewat {@link #setStatus(String)} lalu tidak tertimpa (lihat catatan pada {@link #getStatus()}). */
	public static final String DITOLAK = "Ditolak";
	/** Nilai {@link #getStatus()} ketika artikel sudah disetujui lewat disposisi SOP ({@link #getDisetujuiOleh()} terisi) atau sudah punya {@link #getArticleId()} positif (terdaftar di sistem repositori eksternal). */
	public static final String DISETUJUI = "Disetujui";

	/**
	 * Penanda versi serialisasi Java. Nilai warisan cetakan hbm2java; jangan diubah tanpa alasan.
	 */
	private static final long serialVersionUID = 2463822571548439808L;
	/** Kunci utama basis data, dibangkitkan {@code IDENTITY}; {@code null} selama baris belum tersimpan. */
	private Long id;

	/**
	 * Kait JPA {@code @PreUpdate} yang mendelegasikan pencatatan audit ke
	 * {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum baris diperbarui. Interceptor-lah
	 * yang mengisi {@link #oleh}, {@link #olehId}, dan {@link #getTanggal_dirubah()} dari konteks
	 * pengguna aktif. Method sengaja {@code protected} dan tidak boleh dipanggil manual.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Umumnya dipanggil {@code AuditTimestampInterceptor},
	 * bukan oleh form.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir artikel ini.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} — bila field internal
	 *         belum terisi (mis. objek baru), dikembalikan {@code WaktuUtil.getDate()} saat itu juga
	 *         sebagai gantinya
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah == null ? ais.ui.util.WaktuUtil.getDate() : tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat artikel ini, dipakai label bawaan komponen ZK dan penelusuran log.
	 *
	 * @return {@code "<id>-<judul>"}
	 */
	public String toString() {
		return id + "-" + judul;
	}

	/** Nama pengguna terakhir yang mengubah baris ini; diisi {@code AuditTimestampInterceptor}, bukan oleh form. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; pasangan teknis dari {@link #oleh}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah artikel ini.
	 *
	 * @return id pengguna terakhir, atau {@code null} bila belum pernah diubah lewat jalur yang
	 *         memasang interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna yang terakhir mengubah artikel ini.
	 *
	 * <p><b>Setter defensif:</b> masukan {@code null} atau yang hanya berisi spasi diabaikan
	 * diam-diam sehingga nilai lama dipertahankan, agar bidang audit bayangan ini tidak pernah
	 * ditimpa kosong oleh jalur salin/klon objek.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna yang terakhir mengubah artikel ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, masukan {@code null}/kosong diabaikan.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah artikel ini.
	 *
	 * @return nama pengguna terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/** Tanggal publikasi artikel; bila belum diisi, {@link #getTanggalPublikasi()} memakai {@link #getTanggal_dirubah()} sebagai gantinya. */
	private Date tanggalPublikasi;
	/** Id artikel pada sistem repositori eksternal (mis. repository institusi). Lihat {@link #getArticleId()} untuk pola sentinel negatif ketika belum diisi. */
	private Integer articleId;
	/** Judul artikel; ditimpa oleh {@link #scholarArticle}/{@link #sintaArticle} bila salah satunya tertaut — lihat {@link #getJudul()}. */
	private String judul;
	/** Kata kunci artikel, dipisah menurut konvensi form (biasanya koma). */
	private String keyword;
	/** Tautan/referensi ke sumber artikel; ditimpa dari {@link #scholarArticle}/{@link #sintaArticle} bila tertaut — lihat {@link #getReferensi()}. */
	private String referensi;
	/** Teks sitasi lengkap artikel, disimpan sebagai {@code text}. */
	private String sitasi;
	/** Tahun terbit; ditimpa dari {@link #scholarArticle}/{@link #sintaArticle} bila tertaut, atau memakai tahun berjalan bila tetap kosong — lihat {@link #getTahun()}. */
	private Integer tahun;
	/** Abstrak artikel; ditimpa dari {@link #scholarArticle} (dengan upaya parse JSON pada bidang "Deskripsi") bila tertaut — lihat {@link #getAbstrak()}. */
	private String abstrak;
	/** Dosen/pegawai pengaju artikel; dikosongkan otomatis bila {@link #mahasiswa} terisi (satu artikel hanya bisa diampu salah satu peran) — lihat {@link #getTbmuser()}. */
	private Tbmuser tbmuser;
	/** Mahasiswa pengaju artikel, alternatif dari {@link #tbmuser}. */
	private Mahasiswa mahasiswa;
	/** Lokasi berkas utama artikel di penyimpanan server. */
	private String path;
	/** URL publik/tautan unduh berkas utama artikel, alternatif dari {@link #path} untuk akses lewat web. */
	private String pathUrl;
	/** Flag aktif/nonaktif artikel; {@code null} diperlakukan sebagai aktif oleh {@link #getAktif()}. */
	private Boolean aktif;

	/** Penanda apakah artikel telah terindeks sitasi (mis. oleh Google Scholar/SINTA); {@code null} diperlakukan sebagai belum terindeks oleh {@link #getTelahTerindeksSitasi()}. */
	private Boolean telahTerindeksSitasi;

	/** ISSN (cetak) jurnal tempat artikel diterbitkan. */
	private String issn;
	/** e-ISSN (elektronik) jurnal tempat artikel diterbitkan. */
	private String eIssn;
	/** Nomor volume jurnal; dapat ditimpa hasil parse teks volume SINTA — lihat {@link #getVol()}. */
	private Integer vol;
	/** Nomor terbitan/halaman jurnal; ditimpa dari {@link #sintaArticle} bila tertaut dan tidak kosong — lihat {@link #getNomor()}. */
	private String nomor;

	/** URL lisensi (mis. Creative Commons) yang berlaku atas artikel. */
	private String licenseURL;
	/** Tahun hak cipta artikel; dapat berbeda dari {@link #tahun} (tahun terbit). */
	private Integer copyrightYear;
	/** Pemegang hak cipta artikel; bila kosong memakai label universitas dari konfigurasi — lihat {@link #getCopyrightHolder()}. */
	private String copyrightHolder;
	/** Sponsor/penyandang dana artikel; bila kosong memakai label universitas dari konfigurasi — lihat {@link #getSponsor()}. */
	private String sponsor;
	/** Daftar nama anggota/kontributor artikel dalam bentuk teks bebas (bukan relasi ke {@link AnggotaArtikel}). */
	private String anggota;
	/** Daftar nama anggota eksternal (di luar institusi) dalam bentuk teks bebas. */
	private String anggotaEksternal;

	/** Tautan/berkas pratinjau tampilan jurnal. */
	private String previewJurnal;
	/** Tautan/keterangan hasil pemeriksaan plagiarisme (mis. Turnitin). */
	private String plagiatChecker;
	/** Tautan/keterangan proses peer review artikel. */
	private String peerReview;

	/** Status ringkas artikel; lihat {@link #getStatus()} untuk aturan penurunan nilainya dari disposisi dan {@link #articleId}. */
	private String status;

	/** Jurnal penelitian tempat artikel diterbitkan. */
	private JurnalPenelitian jurnalPenelitian;
	/** Tahapan penyusunan artikel saat ini (mis. draf, review, submit, terbit) — lihat {@link TahapanPenyusunanArtikel}. */
	private TahapanPenyusunanArtikel tahapanPenyusunanArtikel;

	/** Himpunan status indeksasi jurnal (mis. Scopus/SINTA) yang berlaku atas artikel ini; relasi banyak-ke-banyak lewat tabel {@code artikel_has_terindeks}. */
	private Set<ArtikelTerindeks> artikelTerindekses = new HashSet<ArtikelTerindeks>();

	/** Himpunan tingkat publikasi (mis. nasional/internasional) yang berlaku atas artikel ini; relasi banyak-ke-banyak lewat tabel {@code artikel_has_tingkat}. */
	private Set<TingkatArtikel> tingkatArtikeles = new HashSet<TingkatArtikel>();
	/** Semester akademik penulisan/publikasi artikel; bila kosong diturunkan dari {@link #getTanggalPublikasi()} — lihat {@link #getSemester()}. */
	private String semester;
	/** Tahun akademik penulisan/publikasi artikel; bila kosong diturunkan dari {@link #getTanggalPublikasi()} — lihat {@link #getTahunAkademik()}. */
	private String tahunAkademik;
	/** Bahasa penulisan artikel. */
	private String bahasa;
	/** Masa penugasan penulisan artikel; bila kosong dianggap "1 semester" — lihat {@link #getMasaPenugasan()}. */
	private String masaPenugasan;
	/** Daftar editor dan kontributor artikel; ditimpa dari {@link #sintaArticle} (bidang penulis) bila tertaut — lihat {@link #getEditorDanKontributor()}. */
	private String editorDanKontributor;

	/** Tautan ke data hasil pengambilan Google Scholar; bila terisi, menimpa beberapa bidang bibliografis lokal (lihat javadoc kelas). */
	private ScholarArticle scholarArticle;
	/** Tanggal persetujuan artikel; diturunkan dari langkah persetujuan {@link DisposisiSop} bila tersedia — lihat {@link #getSetujuiTanggal()}. */
	private Date setujuiTanggal;
	/** Id item pada sistem repositori (mis. repository institusi) tempat artikel diarsipkan. */
	private Long repoItemId;
	/** Dosen/pegawai yang menyetujui artikel; diturunkan dari langkah persetujuan {@link DisposisiSop} bila tersedia — lihat {@link #getDisetujuiOleh()}. */
	private Tbmuser disetujiOleh;
	/** Dosen/pegawai yang mengajukan artikel; diturunkan dari langkah awal {@link DisposisiSop} bila tersedia — lihat {@link #getDiajukanOleh()}. */
	private Tbmuser diajukanOleh;
	/** Rantai disposisi SOP yang menaungi alur pengajuan/persetujuan artikel ini; lihat {@link DataSop}. */
	private DisposisiSop disposisiSop;
	/** Tautan ke data hasil pengambilan SINTA; bila terisi, menimpa beberapa bidang bibliografis lokal (lihat javadoc kelas). */
	private SintaArticle sintaArticle;

	/**
	 * Mengembalikan himpunan tingkat publikasi (mis. nasional/internasional) yang berlaku atas
	 * artikel ini. Dimuat dengan {@link FetchMode#SELECT} lewat tabel penghubung
	 * {@code artikel_has_tingkat}.
	 *
	 * @return himpunan {@link TingkatArtikel} terkait; tidak pernah {@code null}, dapat kosong
	 */
	@ManyToMany(targetEntity = TingkatArtikel.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@Fetch(FetchMode.SELECT)
	@JoinTable(schema = "penelitiandanpengabdian", name = "artikel_has_tingkat", joinColumns = @JoinColumn(name = "artikel"), inverseJoinColumns = @JoinColumn(name = "tingkat"))
	public Set<TingkatArtikel> getTingkatArtikeles() {
		return tingkatArtikeles;
	}

	/**
	 * Menyetel ulang seluruh himpunan tingkat publikasi artikel ini.
	 *
	 * @param tingkatArtikeles himpunan {@link TingkatArtikel} baru
	 */
	public void setTingkatArtikeles(Set<TingkatArtikel> tingkatArtikeles) {
		this.tingkatArtikeles = tingkatArtikeles;
	}

	/**
	 * Mengembalikan himpunan status indeksasi jurnal (mis. Scopus/SINTA) yang berlaku atas
	 * artikel ini. Dimuat dengan {@link FetchMode#SELECT} lewat tabel penghubung
	 * {@code artikel_has_terindeks}.
	 *
	 * @return himpunan {@link ArtikelTerindeks} terkait; tidak pernah {@code null}, dapat kosong
	 */
	@ManyToMany(targetEntity = ArtikelTerindeks.class, cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@Fetch(FetchMode.SELECT)
	@JoinTable(schema = "penelitiandanpengabdian", name = "artikel_has_terindeks", joinColumns = @JoinColumn(name = "artikel"), inverseJoinColumns = @JoinColumn(name = "terindeks"))
	public Set<ArtikelTerindeks> getArtikelTerindekses() {
		return artikelTerindekses;
	}

	/**
	 * Menyetel ulang seluruh himpunan status indeksasi jurnal artikel ini.
	 *
	 * @param artikelTerindekses himpunan {@link ArtikelTerindeks} baru
	 */
	public void setArtikelTerindekses(Set<ArtikelTerindeks> artikelTerindekses) {
		this.artikelTerindekses = artikelTerindekses;
	}

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public Artikel() {
	}

	/**
	 * Mengembalikan kunci utama artikel ini.
	 *
	 * @return id artikel, atau {@code null} bila belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama artikel ini. Hanya untuk kebutuhan Hibernate dan penyalinan objek.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan judul artikel.
	 *
	 * <p><b>Ditimpa sumber eksternal:</b> bila {@link #scholarArticle} tertaut, judul diambil dari
	 * {@code scholarArticle.getNama()}; bila tidak, dan {@link #sintaArticle} tertaut, judul
	 * diambil dari {@code sintaArticle.getNama()}. Nilai yang disetel manual lewat
	 * {@link #setJudul(String)} hanya bertahan selama tidak ada salah satu tautan tersebut.</p>
	 *
	 * @return judul artikel
	 */
	@Column(name = "judul", nullable = false, length = 1000)
	public String getJudul() {
		if (scholarArticle != null) {
			judul = scholarArticle.getNama();
		} else if (sintaArticle != null) {
			judul = sintaArticle.getNama();
		}
		return this.judul;
	}

	/**
	 * Menyetel judul artikel secara manual. Lihat catatan penimpaan pada {@link #getJudul()}.
	 *
	 * @param judul judul baru
	 */
	public void setJudul(String judul) {
		this.judul = judul;
	}

	/**
	 * Mengembalikan status aktif/nonaktif artikel.
	 *
	 * <p><b>Lazy-default:</b> bila field internal masih {@code null} (artikel yang belum pernah
	 * disetel status aktifnya secara eksplisit), method ini menetapkan {@code true} ke field
	 * tersebut sebelum mengembalikannya, sehingga panggilan berikutnya konsisten mengembalikan
	 * {@code true} tanpa perlu query ulang.</p>
	 *
	 * @return {@code true} bila artikel aktif; default {@code true} bila belum pernah disetel
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menyetel status aktif/nonaktif artikel.
	 *
	 * @param aktif status aktif baru; {@code null} akan kembali diperlakukan sebagai aktif oleh
	 *              {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan masa penugasan penulisan artikel.
	 *
	 * @return teks masa penugasan yang sudah di-{@code trim()}, atau {@code "1 semester"} bila
	 *         belum diisi atau hanya berisi spasi
	 */
	public String getMasaPenugasan() {
		return masaPenugasan == null || masaPenugasan.trim().isEmpty() ? "1 semester" : masaPenugasan.trim();
	}

	/**
	 * Menyetel masa penugasan penulisan artikel.
	 *
	 * @param masaPenugasan teks masa penugasan baru
	 */
	public void setMasaPenugasan(String masaPenugasan) {
		this.masaPenugasan = masaPenugasan;
	}

	/**
	 * Mengembalikan lokasi berkas utama artikel di penyimpanan server.
	 *
	 * @return path berkas yang sudah di-{@code trim()}, atau string kosong bila belum diisi
	 */
	@Column(name = "path", columnDefinition = "text")
	public String getPath() {
		return path == null ? "" : path.trim();
	}

	/**
	 * Menyetel lokasi berkas utama artikel.
	 *
	 * @param path path berkas baru
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Mengembalikan abstrak artikel.
	 *
	 * <p><b>Ditimpa sumber eksternal:</b> bila {@link #scholarArticle} tertaut, abstrak diambil
	 * dari {@code scholarArticle.getKeterangan()}, dengan upaya tambahan mem-parsingnya sebagai
	 * JSON dan mengambil bidang {@code "Deskripsi"} bila keterangan tersebut ternyata berformat
	 * JSON hasil pengambilan data Scholar. Kegagalan parse (bukan JSON, atau bidang tidak ada)
	 * dibiarkan diam-diam — nilai keterangan mentah tetap dipakai sebagai abstrak.</p>
	 *
	 * @return abstrak yang sudah di-{@code trim()}, atau string kosong bila kosong
	 */
	@Column(name = "abstrak", columnDefinition = "text")
	public String getAbstrak() {
		if (scholarArticle != null) {
			abstrak = scholarArticle.getKeterangan();
			try {
				JSONObject jsonObject = new JSONObject(scholarArticle.getKeterangan());
				abstrak = jsonObject.getString("Deskripsi");
				jsonObject = null;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/penelitiandanpengabdian/Artikel.java:238");
				// TODO: handle exception
			}
		}
		return abstrak == null ? "" : abstrak.trim();
	}

	/**
	 * Menyetel abstrak artikel secara manual. Lihat catatan penimpaan pada {@link #getAbstrak()}.
	 *
	 * @param abstrak abstrak baru
	 */
	public void setAbstrak(String abstrak) {
		this.abstrak = abstrak;
	}

	/**
	 * Mengembalikan dosen/pegawai pengaju artikel. Proxy lazy diresolusi lebih dulu lewat
	 * {@link GeneralValueObject#check(Object)}.
	 *
	 * <p><b>Saling eksklusif dengan {@link #getMahasiswa()}:</b> bila {@link #mahasiswa} terisi,
	 * bidang ini dipaksa {@code null} — satu artikel hanya boleh diampu salah satu peran
	 * (dosen/pegawai ATAU mahasiswa), tidak keduanya.</p>
	 *
	 * @return dosen/pegawai pengaju, atau {@code null} bila artikel ini diampu {@link #mahasiswa}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		if (mahasiswa != null) {
			tbmuser = null;
		}
		tbmuser = check(tbmuser);
		return tbmuser;
	}

	/**
	 * Menyetel dosen/pegawai pengaju artikel.
	 *
	 * @param tbmuser dosen/pegawai pengaju baru; boleh {@code null}
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * Mengembalikan mahasiswa pengaju artikel, alternatif dari {@link #getTbmuser()}. Proxy lazy
	 * diresolusi lebih dulu lewat {@link GeneralValueObject#check(Object)}.
	 *
	 * @return mahasiswa pengaju, atau {@code null} bila artikel ini diampu {@link #tbmuser}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * Menyetel mahasiswa pengaju artikel.
	 *
	 * @param mahasiswa mahasiswa pengaju baru; boleh {@code null}
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	@Column(name = "article_id", unique = true, nullable = false)
	public Integer getArticleId() {
		if (articleId == null) {
			articleId = -ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
		}
		return articleId;
	}

	public void setArticleId(Integer articleId) {
		this.articleId = articleId;
	}

	public String getIssn() {
		if (issn == null) {
			issn = "";
		}
		return issn;
	}

	public void setIssn(String issn) {
		this.issn = issn;
	}

	public String geteIssn() {
		if (eIssn == null) {
			eIssn = "";
		}
		return eIssn;
	}

	public void seteIssn(String eIssn) {
		this.eIssn = eIssn;
	}

	public Integer getVol() {
		if (vol == null) {
			vol = 0;
		}
		String volumeSinta = sintaArticle == null ? null : sintaArticle.getVol();
		Integer volumeTerbaca = parseVolumeSinta(volumeSinta);
		if (volumeTerbaca != null) {
			vol = volumeTerbaca;
		}
		return vol;
	}

	private Integer parseVolumeSinta(String nilai) {
		if (nilai == null || nilai.trim().length() == 0) {
			return null;
		}
		String teks = nilai.trim();
		try {
			return Integer.valueOf(teks);
		} catch (NumberFormatException bukanAngkaTunggal) {
			// Data SINTA lama kadang berisi sitasi lengkap, misalnya
			// "2548-9836 5 (1), 14-25", bukan hanya angka volume.
		}

		Pattern[] polaVolume = new Pattern[] {
				Pattern.compile("\\d{4}-\\d{3}[\\dXx]\\s+(\\d+)\\s*\\("),
				Pattern.compile("(?i)\\bvol(?:ume)?\\.?\\s*(\\d+)"),
				Pattern.compile("(?:^|\\s)(\\d+)\\s*\\(") };
		for (int i = 0; i < polaVolume.length; i++) {
			Matcher matcher = polaVolume[i].matcher(teks);
			if (matcher.find()) {
				try {
					return Integer.valueOf(matcher.group(1));
				} catch (NumberFormatException angkaVolumeTidakValid) {
					return null;
				}
			}
		}
		return null;
	}

	public void setVol(Integer vol) {
		this.vol = vol;
	}

	public String getNomor() {
		if (sintaArticle != null && !sintaArticle.getPage().trim().isEmpty()) {
			nomor = sintaArticle.getPage();
		}
		return nomor;
	}

	public void setNomor(String nomor) {
		this.nomor = nomor;
	}

	public String getLicenseURL() {
		return licenseURL;
	}

	public void setLicenseURL(String licenseURL) {
		this.licenseURL = licenseURL;
	}

	public Integer getCopyrightYear() {
		return copyrightYear;
	}

	public void setCopyrightYear(Integer copyrightYear) {
		this.copyrightYear = copyrightYear;
	}

	public String getCopyrightHolder() {
		return copyrightHolder == null || copyrightHolder.trim().isEmpty()
				? Common.getKonfigurasi("label_universitas", "").getNilai()
				: copyrightHolder.trim();
	}

	public void setCopyrightHolder(String copyrightHolder) {
		this.copyrightHolder = copyrightHolder;
	}

	public String getSponsor() {
		return sponsor == null || sponsor.trim().isEmpty() ? Common.getKonfigurasi("label_universitas", "").getNilai()
				: sponsor.trim();
	}

	public void setSponsor(String sponsor) {
		this.sponsor = sponsor;
	}

	public Integer getTahun() {

		if (scholarArticle != null) {
			try {
				JSONObject jsonObject = new JSONObject(scholarArticle.getKeterangan());
				tahun = Integer.parseInt(StringUtils.split(jsonObject.getString("Tanggal terbit"), "/")[0]);
				jsonObject = null;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/penelitiandanpengabdian/Artikel.java:379");
				// TODO: handle exception
			}
		} else if (sintaArticle != null) {
			tahun = sintaArticle.getTahun();
		}

		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}

		return tahun;
	}

	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	@Column(name = "referensi", columnDefinition = "text")
	public String getReferensi() {
		if (scholarArticle != null) {
			referensi = scholarArticle.getLink();
		} else if (sintaArticle != null) {
			referensi = sintaArticle.getLink();
		}
		return referensi;
	}

	public void setReferensi(String referensi) {
		this.referensi = referensi;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurnal_penelitian", nullable = true)
	public JurnalPenelitian getJurnalPenelitian() {
		jurnalPenelitian = check(jurnalPenelitian);
		return jurnalPenelitian;
	}

	public void setJurnalPenelitian(JurnalPenelitian jurnalPenelitian) {
		this.jurnalPenelitian = jurnalPenelitian;
	}

	@Column(name = "anggota", columnDefinition = "text")
	public String getAnggota() {
		return anggota == null ? "" : anggota.trim();
	}

	public void setAnggota(String anggota) {
		this.anggota = anggota;
	}

	public String getPathUrl() {
		return pathUrl;
	}

	public void setPathUrl(String pathUrl) {
		this.pathUrl = pathUrl;
	}

	public String getStatus() {

		if (getDisetujuiOleh() != null) {
			status = DISETUJUI;
		} else {

			if (status == null) {
				status = BELUM_DIPROSES;
			}

			if (getArticleId() > 0) {
				status = DISETUJUI;
			}

		}

		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getSemester() {
		if (semester == null) {
			semester = Common.isNowSemensterGanjil(getTanggalPublikasi()) ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
		}
		return semester;
	}

	public void setSemester(String semester) {
		this.semester = semester;
	}

	public String getTahunAkademik() {
		if (tahunAkademik == null) {
			tahunAkademik = Common.getCurrentTahunAkademik(getTanggalPublikasi());
		}
		return tahunAkademik;
	}

	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	public Date getTanggalPublikasi() {
		if (tanggalPublikasi == null) {
			tanggalPublikasi = getTanggal_dirubah();
		}
		return tanggalPublikasi;
	}

	public void setTanggalPublikasi(Date tanggalPublikasi) {
		this.tanggalPublikasi = tanggalPublikasi;
	}

	public String getKeyword() {
		return keyword == null ? "" : keyword.trim();
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	public void setBahasa(String bahasa) {
		this.bahasa = bahasa;
	}

	public String getBahasa() {
		// TODO Auto-generated method stub
		return bahasa == null ? "" : bahasa;
	}

	public String getPreviewJurnal() {
		return previewJurnal;
	}

	public void setPreviewJurnal(String previewJurnal) {
		this.previewJurnal = previewJurnal;
	}

	public String getPlagiatChecker() {
		return plagiatChecker;
	}

	public void setPlagiatChecker(String plagiatChecker) {
		this.plagiatChecker = plagiatChecker;
	}

	public String getPeerReview() {
		return peerReview;
	}

	public void setPeerReview(String peerReview) {
		this.peerReview = peerReview;
	}

	public Boolean getTelahTerindeksSitasi() {
		return telahTerindeksSitasi == null ? false : telahTerindeksSitasi;
	}

	public void setTelahTerindeksSitasi(Boolean telahTerindeksSitasi) {
		this.telahTerindeksSitasi = telahTerindeksSitasi;
	}

	@Column(columnDefinition = "text")
	public String getEditorDanKontributor() {
		if (sintaArticle != null) {
			editorDanKontributor = sintaArticle.getAuthor();
		}
		return editorDanKontributor == null ? "" : editorDanKontributor.trim();
	}

	public void setEditorDanKontributor(String editorDanKontributor) {
		this.editorDanKontributor = editorDanKontributor;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tahapan_penyusunan_artikel", nullable = true)
	public TahapanPenyusunanArtikel getTahapanPenyusunanArtikel() {
		tahapanPenyusunanArtikel = check(tahapanPenyusunanArtikel);
		return tahapanPenyusunanArtikel;
	}

	public void setTahapanPenyusunanArtikel(TahapanPenyusunanArtikel tahapanPenyusunanArtikel) {
		this.tahapanPenyusunanArtikel = tahapanPenyusunanArtikel;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "scholar_article", nullable = true, unique = true)
	public ScholarArticle getScholarArticle() {
		return scholarArticle;
	}

	public void setScholarArticle(ScholarArticle scholarArticle) {
		this.scholarArticle = scholarArticle;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "sinta_article", nullable = true)
	public SintaArticle getSintaArticle() {
		return sintaArticle;
	}

	public void setSintaArticle(SintaArticle sintaArticle) {
		this.sintaArticle = sintaArticle;
	}

	@Column(columnDefinition = "text")
	public String getSitasi() {
		return sitasi == null ? "" : sitasi.trim();
	}

	public void setSitasi(String sitasi) {
		this.sitasi = sitasi;
	}

	@Column(columnDefinition = "text")
	public String getAnggotaEksternal() {
		return anggotaEksternal == null ? "" : anggotaEksternal.trim();
	}

	public void setAnggotaEksternal(String anggotaEksternal) {
		this.anggotaEksternal = anggotaEksternal;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetuji_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujiOleh = check(disetujiOleh);

		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
				&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
			disetujiOleh = getDisposisiSop().getDisposisiSetuju().getDiajukanOleh();
		}

		if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
				|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
			disetujiOleh = null;
		}

		return disetujiOleh;
	}

	public void setDisetujuiOleh(Tbmuser disetujiOleh) {
		this.disetujiOleh = disetujiOleh;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getSetujuiTanggal() {

		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
				&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
			setujuiTanggal = getDisposisiSop().getDisposisiSetuju().getWaktu();
		}

		if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
				|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
			setujuiTanggal = null;
		}

		return setujuiTanggal;
	}

	public void setSetujuiTanggal(Date setujuiTanggal) {
		this.setujuiTanggal = setujuiTanggal;
	}

	@Column(name="repo_item_id") public Long getRepoItemId(){return repoItemId;}
	public void setRepoItemId(Long v){repoItemId=v;}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "diajukan_oleh", nullable = true)
	public Tbmuser getDiajukanOleh() {
		diajukanOleh = check(diajukanOleh);

		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
				&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
			diajukanOleh = getDisposisiSop().getDisposisiStart().getDiajukanOleh();
		}

		return diajukanOleh;
	}

	public void setDiajukanOleh(Tbmuser diajukanOleh) {
		this.diajukanOleh = diajukanOleh;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	public void setDisposisiSop(DisposisiSop disposisiSop) {if(disposisiSop==null||disposisiSop.getId()==null) {return;}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;
	}
}
