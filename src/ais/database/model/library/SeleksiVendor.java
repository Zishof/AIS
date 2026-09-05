package ais.database.model.library;

// Modul Seleksi Vendor (Pemilihan Penilaian Vendor / Pra-Pembelian).
// Header pengajuan SOP: berisi data ringkas, bobot best-practice, ringkasan
// perbandingan, rekomendasi, dan tautan disposisi SOP (mirip UangMuka).

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.Tbmuser;
import ais.database.model.asset.NomorSuratAlurPengadaan;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;

/**
 * Entitas header <b>Seleksi Vendor</b> (tabel {@code library.seleksi_vendor}) — pengajuan proses
 * pemilihan/tender vendor buku dan bahan pustaka <b>sebelum</b> pembelian dilakukan, berisi bobot
 * best-practice sembilan kriteria penilaian, ringkasan perbandingan hingga tiga vendor, dan
 * rekomendasi akhir yang menunjuk salah satu {@link Penyedia}. Detail skor per-vendor disimpan
 * terpisah pada {@link SeleksiVendorDetail} (relasi satu-ke-banyak lewat
 * {@link SeleksiVendorDetail#getSeleksiVendor()}).
 *
 * <h2>Alur persetujuan: disposisi SOP, bukan status bebas</h2>
 * <p>Kelas ini mewarisi {@code DataSop} sehingga pengajuannya melewati alur disposisi SOP —
 * <b>gerbang persetujuannya benar-benar ditegakkan</b>, sejalan dengan pola
 * {@code UangMukaAction}/{@code asset.PenyediaAsset}: field pengajuan boleh diubah selama disposisi
 * masih berjalan, dan terkunci begitu disposisi mencapai langkah setuju atau selesai (lihat komentar
 * kelas {@code SeleksiVendorAction}). Empat method saling terkait membentuk gerbang ini:</p>
 * <ul>
 *   <li>{@link #getStatus()} mengembalikan {@link #DISETUJU} bila {@link #getDisetujuiOleh()}
 *       tidak {@code null}, jatuh kembali ke {@link #PENGAJUAN} bila status tersimpan sebelumnya
 *       {@link #DISETUJU} namun penyetujunya sudah hilang (mis. disposisi dibatalkan), dan
 *       ditimpa paksa menjadi {@link #DITOLAK} bila langkah akhir disposisi adalah langkah
 *       penolakan ({@code getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()});</li>
 *   <li>{@link #getDisetujuiOleh()} dan {@link #getTanggalPersetujuan()} diturunkan dari
 *       {@code getDisposisiSop().getDisposisiSetuju()} — keduanya <b>bukan</b> nilai yang diisi
 *       manual lewat form, melainkan cerminan keadaan disposisi terkini, dan secara eksplisit
 *       di-null-kan kembali bila disposisi setuju tidak/tidak lagi ada;</li>
 *   <li>{@link #getAktif()} ditimpa {@code false} bila disposisi tidak aktif atau langkah akhirnya
 *       penolakan, membuat pengajuan yang ditolak otomatis tidak lagi dianggap berjalan;</li>
 *   <li>{@link #setStatus(String)} adalah satu-satunya jalur "manual": menyetel status ke
 *       {@link #DITOLAK} sekaligus membersihkan {@link #disetujuiOleh} dan
 *       {@link #tanggalPersetujuan}, mencegah pengajuan yang ditolak tetap menyandang jejak
 *       persetujuan lama.</li>
 * </ul>
 * <p>Dengan kata lain, berbeda dari {@code SurveyVendor} (lihat kelasnya) yang statusnya sekadar
 * kolom teks bebas set tanpa penurunan dari sumber independen, gerbang di sini <b>tidak bisa
 * dipalsukan</b> sekadar dengan memanggil {@code setStatus("Disetujui")} — status yang dibaca
 * kembali akan segera dikoreksi oleh {@link #getStatus()} berdasarkan keadaan
 * {@link DisposisiSop} yang sesungguhnya, sepanjang pemanggil selalu membaca lewat getter
 * (bukan bidang mentah).</p>
 *
 * <h2>Bobot best-practice dan skor tertimbang</h2>
 * <p>Sembilan kriteria (harga, spesifikasi, ketersediaan, kejelasan, legalitas, pengalaman,
 * responsif, pembayaran, reputasi) masing-masing punya bobot persen dengan bawaan sama rata
 * (~11% tiap kriteria, lihat {@code getBobotHarga()} dkk. — totalnya 100). Bobot ini dibaca
 * {@link SeleksiVendorDetail#getSkorTertimbang()} untuk menghitung skor 0..100 tiap vendor;
 * kelas ini sendiri tidak menjumlahkan skor vendor manapun, murni menjadi sumber bobot dan wadah
 * ringkasan/rekomendasi akhir.</p>
 *
 * <h2>Bidang audit bayangan</h2>
 * <p>{@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} beserta {@link #onUpdate()} adalah
 * keharusan teknis agar {@code AuditTimestampInterceptor} dapat bekerja, bukan duplikasi yang bisa
 * dihapus.</p>
 *
 * @see SeleksiVendorDetail
 * @see Penyedia
 * @see DataSop
 * @see DisposisiSop
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "library", name = "seleksi_vendor")
public class SeleksiVendor extends DataSop {

	/** Penanda versi serialisasi Java. Nilai warisan cetakan hbm2java; jangan diubah tanpa alasan. */
	private static final long serialVersionUID = 2463821577548439810L;

	/** Status awal: pengajuan belum disetujui/ditolak — bawaan {@link #getStatus()}. */
	public static final String PENGAJUAN = "Pengajuan";
	/** Status setelah disposisi mencapai langkah setuju — lihat {@link #getStatus()}. */
	public static final String DISETUJU = "Disetujui";
	/** Status setelah disposisi berakhir pada langkah penolakan — lihat {@link #getStatus()}. */
	public static final String DITOLAK = "Ditolak";

	// Rekomendasi (Section D form)
	/** Rekomendasi Section D: vendor terpilih direkomendasikan untuk dipakai. */
	public static final String REKOM_DIREKOMENDASIKAN = "Direkomendasikan";
	/** Rekomendasi Section D: hasil seleksi perlu ditinjau ulang sebelum diputuskan. */
	public static final String REKOM_PERTIMBANGAN_ULANG = "Perlu pertimbangan ulang";
	/** Rekomendasi Section D: tidak ada vendor yang layak direkomendasikan. */
	public static final String REKOM_TIDAK = "Tidak direkomendasikan";

	/** Kunci utama basis data, dibangkitkan {@code IDENTITY}; {@code null} selama baris belum tersimpan. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi {@code AuditTimestampInterceptor}, bukan oleh form. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; pasangan teknis dari {@link #oleh}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah data pengajuan ini.
	 *
	 * @return id pengguna terakhir, atau {@code null} bila baris belum pernah diubah lewat jalur
	 *         yang memasang interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna yang terakhir mengubah data pengajuan ini.
	 *
	 * <p><b>Setter defensif:</b> masukan {@code null}/kosong diabaikan diam-diam agar bidang audit
	 * bayangan ini tidak pernah tertimpa kosong.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna yang terakhir mengubah data pengajuan ini.
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah data pengajuan ini.
	 *
	 * @return nama pengguna terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate} yang mendelegasikan pencatatan audit ke
	 * {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum baris diperbarui. Method sengaja
	 * {@code protected} dan tidak boleh dipanggil manual dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Umumnya dipanggil {@code AuditTimestampInterceptor}.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris pengajuan ini.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk objek baru di memori
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat berformat {@code "<id>-<nama>"}, dipakai label bawaan komponen ZK
	 * dan penelusuran log.
	 *
	 * @return gabungan id dan nama; bagian id bernilai teks {@code "null"} bila belum tersimpan
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode pengajuan, dapat berupa penomoran manual; lihat juga {@link #getKodeUnik()}. */
	private String kode;
	/** Kode unik turunan (tidak dipetakan kolom tetap) — lihat {@link #getKodeUnik()}. */
	private String kodeUnik;
	/** Perihal/judul pengajuan seleksi vendor. */
	private String nama;          // Perihal / judul pengajuan
	/** Latar belakang/catatan pengajuan. */
	private String keterangan;    // Latar belakang / catatan
	/** Jenis barang/jasa yang diadakan pada seleksi ini. */
	private String jenisPengadaan; // Jenis barang/jasa yang diadakan
	/** Tanggal pengajuan; bawaan tanggal hari ini bila belum diisi — lihat {@link #getTanggal()}. */
	private Date tanggal;         // Tanggal pengajuan
	/** Penanda pengajuan masih berjalan; sepenuhnya diturunkan dari disposisi — lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** Status alur (salah satu {@link #PENGAJUAN}/{@link #DISETUJU}/{@link #DITOLAK}); dikoreksi getter dari disposisi. */
	private String status;

	/** Disposisi SOP pengajuan ini; sumber kebenaran bagi status, aktif, dan data persetujuan. */
	private DisposisiSop disposisiSop;
	/** Templat penomoran surat; bawaannya {@code NomorSuratAlurPengadaan.PEMILIHAN_PENILAIAN_VENDOR_DATA}. */
	private NomorSuratAlurPengadaan nomorSuratAlurPengadaan;
	/** Pengaju; diturunkan dari disposisi awal SOP — lihat {@link #getDibuatOleh()}. */
	private Tbmuser dibuatOleh;
	/** Penyetuju; diturunkan dari disposisi SOP — lihat {@link #getDisetujuiOleh()}. */
	private Tbmuser disetujuiOleh;
	/** Waktu persetujuan; diturunkan dari disposisi SOP — lihat {@link #getTanggalPersetujuan()}. */
	private Date tanggalPersetujuan;
	/** Waktu pembuatan pengajuan; bawaan waktu sekarang bila belum diisi. */
	private Date tanggalPembuatan;

	// Bobot best-practice per kriteria (persen). Default sama-rata (~11 tiap kriteria).
	/** Bobot persen kriteria harga; bawaan 12 — lihat {@code getBobotHarga()}. */
	private Integer bobotHarga;
	/** Bobot persen kriteria kesesuaian spesifikasi; bawaan 12. */
	private Integer bobotSpesifikasi;
	/** Bobot persen kriteria ketersediaan barang/jasa; bawaan 11. */
	private Integer bobotKetersediaan;
	/** Bobot persen kriteria kejelasan penawaran; bawaan 11. */
	private Integer bobotKejelasan;
	/** Bobot persen kriteria legalitas vendor; bawaan 11. */
	private Integer bobotLegalitas;
	/** Bobot persen kriteria pengalaman vendor; bawaan 11. */
	private Integer bobotPengalaman;
	/** Bobot persen kriteria responsivitas vendor; bawaan 11. */
	private Integer bobotResponsif;
	/** Bobot persen kriteria kemudahan pembayaran; bawaan 11. */
	private Integer bobotPembayaran;
	/** Bobot persen kriteria reputasi vendor; bawaan 10 (total sembilan bobot bawaan = 100). */
	private Integer bobotReputasi;

	// Kolom "Ket." per kriteria (Section B)
	/** Catatan bebas kriteria harga (Section B form). */
	private String ketHarga;
	/** Catatan bebas kriteria kesesuaian spesifikasi. */
	private String ketSpesifikasi;
	/** Catatan bebas kriteria ketersediaan. */
	private String ketKetersediaan;
	/** Catatan bebas kriteria kejelasan penawaran. */
	private String ketKejelasan;
	/** Catatan bebas kriteria legalitas vendor. */
	private String ketLegalitas;
	/** Catatan bebas kriteria pengalaman vendor. */
	private String ketPengalaman;
	/** Catatan bebas kriteria responsivitas vendor. */
	private String ketResponsif;
	/** Catatan bebas kriteria kemudahan pembayaran. */
	private String ketPembayaran;
	/** Catatan bebas kriteria reputasi vendor. */
	private String ketReputasi;

	// Section C - Ringkasan Perbandingan
	/** Nama/ringkasan vendor pembanding pertama (Section C). */
	private String vendorPembanding1;
	/** Nama/ringkasan vendor pembanding kedua. */
	private String vendorPembanding2;
	/** Nama/ringkasan vendor pembanding ketiga. */
	private String vendorPembanding3;
	/** Alasan vendor tertentu dipilih di antara para pembanding. */
	private String alasanDipilih;

	// Section D - Rekomendasi
	/** Rekomendasi akhir Section D; salah satu konstanta {@code REKOM_*}. */
	private String rekomendasi;          // salah satu konstanta REKOM_*
	/** Nomor urut vendor yang direkomendasikan (1..n, mengacu urutan pada {@link SeleksiVendorDetail}). */
	private Integer rekomendasiNomor;    // vendor nomor yang direkomendasikan (1..n)
	/** Vendor ({@link Penyedia}) yang direkomendasikan — lihat {@link #getRekomendasiPenyedia()}. */
	private Penyedia rekomendasiPenyedia;
	/** Alasan utama rekomendasi Section D. */
	private String alasanUtama;

	// Section E - Penilai (dari pengguna disposisi tindak-lanjut)
	/** Nama penilai yang menandatangani Section E. */
	private String namaPenilai;
	/** Jabatan penilai Section E. */
	private String jabatanPenilai;
	/** Tanggal penilaian Section E ditandatangani. */
	private Date tanggalPenilaian;
	/** Tanda tangan penilai (umumnya tautan/berkas gambar) Section E. */
	private String ttd;

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public SeleksiVendor() {
	}

	/**
	 * Mengembalikan kunci utama baris pengajuan ini.
	 *
	 * @return id pengajuan, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris pengajuan ini. Hanya untuk kebutuhan Hibernate/penyalinan objek.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode pengajuan setelah dipangkas spasi tepinya.
	 *
	 * @return kode yang sudah dipangkas, atau {@code null} bila belum diisi atau hanya berisi spasi
	 */
	@Column(name = "kode", nullable = true)
	public String getKode() {
		return this.kode == null || kode.trim().isEmpty() ? null : this.kode.trim();
	}

	/**
	 * Menyetel kode pengajuan.
	 *
	 * @param kode kode pengajuan; boleh {@code null}
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan kode unik pengajuan, dibangun ulang setiap pemanggilan sebagai gabungan
	 * {@link #getKode()} dengan id disposisi SOP (atau id baris ini bila belum berdisposisi).
	 *
	 * <p><b>Cara kerja.</b> Formatnya {@code kode + "_" + (disposisiSop.id atau id)}. Karena
	 * {@link #getDisposisiSop()} dipanggil di dalamnya, method ini memicu resolusi proksi lazim.
	 * Nilai yang dihasilkan <b>ditulis kembali</b> ke bidang {@link #kodeUnik} sebelum
	 * dikembalikan — getter ini karena itu memutasi state, dan kolom {@code unique = true} pada
	 * anotasinya berarti tabrakan nilai (mis. dua baris dengan kode sama-sama belum berdisposisi
	 * sehingga sama-sama memakai id sebagai sufiks — walau id selalu unik per baris sehingga secara
	 * praktik tabrakan hampir tidak mungkin terjadi) akan gagal pada tingkat basis data saat
	 * disimpan.</p>
	 *
	 * @return kode unik pengajuan; tidak pernah {@code null} karena selalu memiliki sufiks id atau
	 *         id disposisi
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		kodeUnik = getKode() + "" + (getDisposisiSop() == null ? "_" + getId() : "_" + getDisposisiSop().getId());
		return kodeUnik;
	}

	/**
	 * Menyetel kode unik pengajuan secara manual. Nilai yang disetel di sini akan ditimpa
	 * {@link #getKodeUnik()} pada pembacaan berikutnya, sehingga setter ini pada praktiknya hanya
	 * berguna sesaat sebelum objek disimpan tanpa getter dipanggil lagi.
	 *
	 * @param kodeUnik kode unik; akan ditimpa pada pembacaan berikutnya
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * Mengembalikan perihal/judul pengajuan setelah dipangkas spasi tepinya.
	 *
	 * @return judul pengajuan yang sudah dipangkas, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel perihal/judul pengajuan.
	 *
	 * @param nama judul pengajuan
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan latar belakang/catatan pengajuan.
	 *
	 * @return catatan; boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel latar belakang/catatan pengajuan.
	 *
	 * @param keterangan catatan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan jenis barang/jasa yang diadakan pada seleksi ini.
	 *
	 * @return jenis pengadaan; boleh {@code null}
	 */
	@Column(name = "jenis_pengadaan", nullable = true)
	public String getJenisPengadaan() {
		return jenisPengadaan;
	}

	/**
	 * Menyetel jenis barang/jasa yang diadakan.
	 *
	 * @param jenisPengadaan jenis pengadaan; boleh {@code null}
	 */
	public void setJenisPengadaan(String jenisPengadaan) {
		this.jenisPengadaan = jenisPengadaan;
	}

	/**
	 * Mengembalikan tanggal pengajuan, dengan bawaan tanggal-waktu saat method dipanggil bila
	 * belum diisi.
	 *
	 * <p>Perhatikan bahwa bawaan ini <b>tidak</b> ditulis kembali ke bidang {@link #tanggal} —
	 * berbeda dari kebanyakan getter lain di kelas ini, method ini bebas efek samping — sehingga
	 * setiap pemanggilan pada baris yang tanggalnya kosong menghasilkan nilai {@code new Date()}
	 * yang sedikit berbeda setiap kali.</p>
	 *
	 * @return tanggal pengajuan tersimpan, atau tanggal-waktu saat ini bila belum diisi; tidak
	 *         pernah {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal")
	public Date getTanggal() {
		return tanggal == null ? new Date() : tanggal;
	}

	/**
	 * Menyetel tanggal pengajuan.
	 *
	 * @param tanggal tanggal pengajuan; boleh {@code null} untuk kembali ke bawaan tanggal berjalan
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengembalikan penanda apakah pengajuan seleksi vendor ini masih berjalan, diturunkan
	 * sepenuhnya dari keadaan {@link #getStatus()} dan disposisi SOP-nya — <b>ini adalah gerbang
	 * yang ditegakkan</b>, bukan flag bebas set.
	 *
	 * <p><b>Urutan evaluasi (tiga cabang, masing-masing dapat menimpa hasil cabang sebelumnya):</b></p>
	 * <ol>
	 *   <li>Bila {@link #getStatus()} sudah bernilai {@link #DISETUJU}, {@code aktif} diset
	 *       {@code true} — pengajuan yang disetujui dianggap berjalan/berlaku;</li>
	 *   <li>Disposisi SOP diselesaikan proksinya lewat {@link #getDisposisiSop()} lalu ditulis
	 *       kembali ke bidang {@link #disposisiSop}. Bila disposisi ada namun
	 *       {@code disposisiSop.getAktif()} bernilai {@code false} (disposisi sudah berakhir tanpa
	 *       mencapai langkah setuju, misalnya dibatalkan), {@code aktif} ditimpa {@code false};</li>
	 *   <li>Bila disposisi ada, memiliki langkah akhir, dan langkah akhir tersebut adalah langkah
	 *       penolakan ({@code getAlurSop().getPenolakanAdaDiSini()}), {@code aktif} ditimpa lagi
	 *       {@code false} — ini berlaku bahkan bila cabang pertama sempat menyetelnya {@code true},
	 *       sehingga penolakan selalu menang atas persetujuan yang mungkin tercatat sebelumnya.</li>
	 * </ol>
	 * <p>Bila baris belum pernah dievaluasi cabang manapun (bidang {@link #aktif} masih
	 * {@code null} dan tidak ada disposisi), method mengembalikan {@code true} sebagai bawaan.</p>
	 *
	 * <p><b>Getter yang memutasi state.</b> Nilai akhir ditulis kembali ke bidang instans
	 * {@link #aktif}, dan bidang {@link #disposisiSop} juga ikut tertulis sebagai efek samping
	 * pemanggilan {@link #getDisposisiSop()}. Objek yang terpasang pada sesi Hibernate dapat
	 * ikut ter-<i>flush</i> hanya karena getter ini dipanggil kode baca-saja (laporan, dasbor).</p>
	 *
	 * @return {@code true} bila pengajuan masih dianggap berjalan/berlaku; {@code false} bila
	 *         disposisinya sudah tidak aktif atau berakhir pada langkah penolakan
	 * @see #getStatus()
	 * @see #getDisposisiSop()
	 */
	public Boolean getAktif() {
		if (getStatus().equals(SeleksiVendor.DISETUJU)) {
			aktif = true;
		}
		disposisiSop = getDisposisiSop();
		if (disposisiSop != null && !disposisiSop.getAktif()) {
			aktif = false;
		}
		if (disposisiSop != null && disposisiSop.getDisposisiEnd() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
			aktif = false;
		}
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel penanda aktif secara manual. Nilai ini akan dievaluasi ulang dan berpotensi ditimpa
	 * oleh {@link #getAktif()} pada pembacaan berikutnya berdasarkan status dan disposisi SOP.
	 *
	 * @param aktif penanda aktif; dapat ditimpa getter berikutnya
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan disposisi SOP pengajuan ini setelah proksi malasnya diselesaikan
	 * {@code check(...)}. Inilah sumber kebenaran bagi {@link #getStatus()}, {@link #getAktif()},
	 * {@link #getDibuatOleh()}, {@link #getDisetujuiOleh()}, dan {@link #getTanggalPersetujuan()}.
	 *
	 * @return disposisi SOP pengajuan, atau {@code null} bila belum ditautkan ke alur SOP
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menyetel disposisi SOP pengajuan ini.
	 *
	 * <p><b>Ditolak diam-diam</b> bila {@code disposisiSop} bernilai {@code null} atau belum
	 * memiliki id (belum tersimpan) — nilai lama dipertahankan pada kedua kasus tersebut, mencegah
	 * pengajuan kehilangan tautan disposisinya karena dipasangi objek transient.</p>
	 *
	 * @param disposisiSop disposisi SOP; diabaikan bila {@code null} atau belum tersimpan
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {
		if (disposisiSop == null || disposisiSop.getId() == null) {
			return;
		}
		this.disposisiSop = disposisiSop;
	}

	/**
	 * Mengembalikan templat penomoran surat pengajuan ini, dengan bawaan
	 * {@code NomorSuratAlurPengadaan.PEMILIHAN_PENILAIAN_VENDOR_DATA} bila belum diisi.
	 *
	 * @return templat penomoran surat; tidak pernah {@code null} kecuali konstanta bawaannya sendiri
	 *         gagal disemai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat_alur_pengadaan", nullable = true)
	public NomorSuratAlurPengadaan getNomorSuratAlurPengadaan() {
		if (nomorSuratAlurPengadaan == null) {
			nomorSuratAlurPengadaan = NomorSuratAlurPengadaan.PEMILIHAN_PENILAIAN_VENDOR_DATA;
		} else {
			nomorSuratAlurPengadaan = check(nomorSuratAlurPengadaan);
		}
		return nomorSuratAlurPengadaan;
	}

	/**
	 * Menyetel templat penomoran surat pengajuan ini.
	 *
	 * @param nomorSuratAlurPengadaan templat penomoran surat; {@code null} kembali ke bawaan pada
	 *                                pembacaan berikutnya
	 */
	public void setNomorSuratAlurPengadaan(NomorSuratAlurPengadaan nomorSuratAlurPengadaan) {
		this.nomorSuratAlurPengadaan = nomorSuratAlurPengadaan;
	}

	/**
	 * Menyetel pengaju pengajuan secara manual. Nilai ini akan ditimpa {@link #getDibuatOleh()}
	 * pada pembacaan berikutnya bila disposisi awal SOP sudah mencatat pengajunya sendiri.
	 *
	 * @param dibuatOleh pengaju; dapat ditimpa getter berikutnya
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengembalikan pengaju pengajuan ini, diprioritaskan dari disposisi awal SOP.
	 *
	 * <p>Proksi lazim {@link #dibuatOleh} diselesaikan lebih dulu, lalu <b>ditimpa</b> bila
	 * {@code getDisposisiSop().getDisposisiStart().getDiajukanOleh()} tidak {@code null} — dengan
	 * kata lain pengaju yang tercatat di disposisi SOP selalu menang atas nilai yang tersimpan
	 * langsung pada baris ini. Hasil penimpaan ditulis kembali ke bidang instans, sehingga getter
	 * ini memutasi state dan tidak bebas efek samping.</p>
	 *
	 * @return pengaju pengajuan; diturunkan dari disposisi awal bila tersedia, atau nilai tersimpan
	 *         langsung, atau {@code null} bila keduanya kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = true)
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);
		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
				&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
			dibuatOleh = getDisposisiSop().getDisposisiStart().getDiajukanOleh();
		}
		return dibuatOleh;
	}

	/**
	 * Menyetel penyetuju pengajuan secara manual. Nilai ini dievaluasi ulang dan berpotensi ditimpa
	 * atau dikosongkan oleh {@link #getDisetujuiOleh()} pada pembacaan berikutnya.
	 *
	 * @param disetujuiOleh penyetuju; dapat ditimpa/dikosongkan getter berikutnya
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Mengembalikan penyetuju pengajuan ini — bagian inti dari gerbang persetujuan yang ditegakkan
	 * kelas ini (lihat javadoc kelas).
	 *
	 * <p><b>Logika lengkap.</b> Proksi lazim diselesaikan lebih dulu. Bila disposisi SOP-nya sudah
	 * mencapai langkah setuju dan langkah itu mencatat siapa yang mengajukannya
	 * ({@code getDisposisiSetuju().getDiajukanOleh()}), bidang {@link #disetujuiOleh} <b>ditimpa</b>
	 * dengan pengaju langkah setuju tersebut. Sebaliknya, bila disposisi setuju tidak ada atau
	 * pengajunya kosong, bidang ini <b>dikosongkan paksa</b> ({@code null}) — sehingga penyetuju
	 * yang tersimpan langsung pada baris (mis. hasil {@link #setDisetujuiOleh(Tbmuser)} manual)
	 * tidak pernah dianggap sah tanpa dukungan disposisi yang benar-benar menyetujui. Sebelum
	 * dikembalikan, hasil akhirnya diselesaikan sekali lagi lewat {@code check(...)}.</p>
	 *
	 * @return penyetuju pengajuan bila disposisi SOP-nya benar-benar sudah pada langkah setuju
	 *         dengan pengaju tercatat; {@code null} pada keadaan lain apa pun
	 * @see #getStatus()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujuiOleh = check(disetujuiOleh);
		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
				&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
			disetujuiOleh = getDisposisiSop().getDisposisiSetuju().getDiajukanOleh();
		}
		if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
				|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
			disetujuiOleh = null;
		}
		disetujuiOleh = check(disetujuiOleh);
		return disetujuiOleh;
	}

	/**
	 * Menyetel waktu persetujuan secara manual. Nilai ini dievaluasi ulang dan berpotensi ditimpa
	 * atau dikosongkan oleh {@link #getTanggalPersetujuan()} pada pembacaan berikutnya.
	 *
	 * @param tanggalPersetujuan waktu persetujuan; dapat ditimpa/dikosongkan getter berikutnya
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengembalikan waktu persetujuan pengajuan ini, diturunkan dari waktu disposisi SOP mencapai
	 * langkah setuju — pasangan {@link #getDisetujuiOleh()} dengan logika penimpaan/pengosongan
	 * yang sama persis.
	 *
	 * <p><b>Penanganan galat senyap.</b> Seluruh logika dibungkus {@code try/catch}: kegagalan
	 * apa pun saat menelusuri rantai {@code getDisposisiSop().getDisposisiSetuju()...} (mis. sesi
	 * Hibernate tertutup di tengah resolusi lazim) dicatat ke {@code ErrorAuditUtil} lewat blok
	 * {@code catch} lalu <b>ditelan</b> — method tidak melempar ulang, sehingga kegagalan hanya
	 * tampak sebagai nilai tanggal yang tidak berubah dari pembacaan sebelumnya, bukan error yang
	 * terlihat pengguna.</p>
	 *
	 * @return waktu disposisi menyetujui, bila disposisi SOP-nya sudah pada langkah setuju dengan
	 *         pengaju tercatat; {@code null} bila belum atau tidak lagi demikian
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {
		try {
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
					&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
				tanggalPersetujuan = getDisposisiSop().getDisposisiSetuju().getWaktu();
			}
			if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
					|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
				tanggalPersetujuan = null;
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/library/SeleksiVendor.java:getTanggalPersetujuan-lazy");
		}
		return tanggalPersetujuan;
	}

	/**
	 * Menyetel waktu pembuatan pengajuan.
	 *
	 * @param tanggalPembuatan waktu pembuatan; boleh {@code null} untuk kembali ke bawaan waktu
	 *                         berjalan
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Mengembalikan waktu pembuatan pengajuan, dengan bawaan waktu saat method dipanggil bila
	 * belum diisi. Sama seperti {@link #getTanggal()}, bawaan ini tidak ditulis kembali ke bidang
	 * instansnya sehingga bebas efek samping.
	 *
	 * @return waktu pembuatan tersimpan, atau waktu saat ini bila belum diisi; tidak pernah
	 *         {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		return tanggalPembuatan == null ? new Date() : tanggalPembuatan;
	}

	/**
	 * Mengembalikan status alur pengajuan ini — gerbang utama yang dijabarkan pada javadoc kelas.
	 *
	 * <p><b>Urutan evaluasi:</b></p>
	 * <ol>
	 *   <li>Bila {@link #getDisetujuiOleh()} tidak {@code null} (yang berarti disposisi SOP
	 *       benar-benar sudah pada langkah setuju), status ditimpa {@link #DISETUJU};</li>
	 *   <li>Sebaliknya, bila status tersimpan sebelumnya adalah {@link #DISETUJU} namun
	 *       {@code getDisetujuiOleh()} sekarang {@code null} (disposisi setuju hilang/dibatalkan),
	 *       status dikembalikan ke {@link #PENGAJUAN} — mencegah status "Disetujui" yang sudah
	 *       tidak lagi didukung disposisi tetap terbaca;</li>
	 *   <li>Disposisi diselesaikan lagi lewat {@link #getDisposisiSop()}; bila langkah akhirnya
	 *       adalah langkah penolakan, status ditimpa paksa {@link #DITOLAK} — cabang ini
	 *       <b>mengalahkan</b> kedua cabang sebelumnya, sehingga penolakan selalu menjadi kata
	 *       akhir.</li>
	 * </ol>
	 * <p>Bila setelah seluruh evaluasi bidang {@link #status} masih {@code null} atau kosong,
	 * method mengembalikan {@link #PENGAJUAN} sebagai bawaan. Nilai akhir ditulis kembali ke
	 * bidang instans, sehingga getter ini memutasi state.</p>
	 *
	 * @return salah satu {@link #PENGAJUAN}, {@link #DISETUJU}, atau {@link #DITOLAK}; tidak
	 *         pernah {@code null}
	 * @see #getAktif()
	 * @see #setStatus(String)
	 */
	public String getStatus() {
		if (getDisetujuiOleh() != null) {
			status = DISETUJU;
		} else if (status != null && status.equals(DISETUJU)) {
			status = PENGAJUAN;
		}
		disposisiSop = getDisposisiSop();
		if (disposisiSop != null && disposisiSop.getDisposisiEnd() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
			status = DITOLAK;
		}
		return status == null || status.trim().isEmpty() ? PENGAJUAN : status;
	}

	/**
	 * Menyetel status alur pengajuan secara manual.
	 *
	 * <p><b>Efek samping penting:</b> bila {@code status} yang disetel adalah {@link #DITOLAK},
	 * method ini langsung memanggil {@link #setDisetujuiOleh(Tbmuser)} dan
	 * {@link #setTanggalPersetujuan(Date)} dengan {@code null}, membersihkan jejak persetujuan
	 * lama agar pengajuan yang ditolak tidak menyandang data penyetuju/tanggal persetujuan yang
	 * sudah tidak berlaku. Perlu diingat bahwa nilai yang disetel di sini pada akhirnya tetap
	 * tunduk pada evaluasi {@link #getStatus()} setiap kali dibaca kembali — menyetel
	 * {@link #DISETUJU} secara manual tanpa disposisi yang benar-benar menyetujui akan segera
	 * dikoreksi kembali menjadi {@link #PENGAJUAN} pada pembacaan berikutnya.</p>
	 *
	 * @param status status baru; {@link #DITOLAK} memicu pembersihan data persetujuan
	 */
	public void setStatus(String status) {
		if (status != null && status.equals(DITOLAK)) {
			setDisetujuiOleh(null);
			setTanggalPersetujuan(null);
		}
		this.status = status;
	}

	// ---- Bobot best-practice (default sama-rata) ----
	/** @return bobot persen kriteria harga; bawaan 12 bila belum diisi */
	@Column(name = "bobot_harga")
	public Integer getBobotHarga() { return bobotHarga == null ? 12 : bobotHarga; }
	/** @param v bobot persen kriteria harga */
	public void setBobotHarga(Integer v) { this.bobotHarga = v; }

	/** @return bobot persen kriteria kesesuaian spesifikasi; bawaan 12 bila belum diisi */
	@Column(name = "bobot_spesifikasi")
	public Integer getBobotSpesifikasi() { return bobotSpesifikasi == null ? 12 : bobotSpesifikasi; }
	/** @param v bobot persen kriteria kesesuaian spesifikasi */
	public void setBobotSpesifikasi(Integer v) { this.bobotSpesifikasi = v; }

	/** @return bobot persen kriteria ketersediaan; bawaan 11 bila belum diisi */
	@Column(name = "bobot_ketersediaan")
	public Integer getBobotKetersediaan() { return bobotKetersediaan == null ? 11 : bobotKetersediaan; }
	/** @param v bobot persen kriteria ketersediaan */
	public void setBobotKetersediaan(Integer v) { this.bobotKetersediaan = v; }

	/** @return bobot persen kriteria kejelasan penawaran; bawaan 11 bila belum diisi */
	@Column(name = "bobot_kejelasan")
	public Integer getBobotKejelasan() { return bobotKejelasan == null ? 11 : bobotKejelasan; }
	/** @param v bobot persen kriteria kejelasan penawaran */
	public void setBobotKejelasan(Integer v) { this.bobotKejelasan = v; }

	/** @return bobot persen kriteria legalitas vendor; bawaan 11 bila belum diisi */
	@Column(name = "bobot_legalitas")
	public Integer getBobotLegalitas() { return bobotLegalitas == null ? 11 : bobotLegalitas; }
	/** @param v bobot persen kriteria legalitas vendor */
	public void setBobotLegalitas(Integer v) { this.bobotLegalitas = v; }

	/** @return bobot persen kriteria pengalaman vendor; bawaan 11 bila belum diisi */
	@Column(name = "bobot_pengalaman")
	public Integer getBobotPengalaman() { return bobotPengalaman == null ? 11 : bobotPengalaman; }
	/** @param v bobot persen kriteria pengalaman vendor */
	public void setBobotPengalaman(Integer v) { this.bobotPengalaman = v; }

	/** @return bobot persen kriteria responsivitas vendor; bawaan 11 bila belum diisi */
	@Column(name = "bobot_responsif")
	public Integer getBobotResponsif() { return bobotResponsif == null ? 11 : bobotResponsif; }
	/** @param v bobot persen kriteria responsivitas vendor */
	public void setBobotResponsif(Integer v) { this.bobotResponsif = v; }

	/** @return bobot persen kriteria kemudahan pembayaran; bawaan 11 bila belum diisi */
	@Column(name = "bobot_pembayaran")
	public Integer getBobotPembayaran() { return bobotPembayaran == null ? 11 : bobotPembayaran; }
	/** @param v bobot persen kriteria kemudahan pembayaran */
	public void setBobotPembayaran(Integer v) { this.bobotPembayaran = v; }

	/** @return bobot persen kriteria reputasi vendor; bawaan 10 bila belum diisi (total bawaan sembilan kriteria = 100) */
	@Column(name = "bobot_reputasi")
	public Integer getBobotReputasi() { return bobotReputasi == null ? 10 : bobotReputasi; }
	/** @param v bobot persen kriteria reputasi vendor */
	public void setBobotReputasi(Integer v) { this.bobotReputasi = v; }

	// ---- Keterangan per kriteria (Section B) ----
	/** @return catatan bebas kriteria harga */
	@Column(name = "ket_harga") public String getKetHarga() { return ketHarga; }
	/** @param v catatan bebas kriteria harga */
	public void setKetHarga(String v) { this.ketHarga = v; }
	/** @return catatan bebas kriteria kesesuaian spesifikasi */
	@Column(name = "ket_spesifikasi") public String getKetSpesifikasi() { return ketSpesifikasi; }
	/** @param v catatan bebas kriteria kesesuaian spesifikasi */
	public void setKetSpesifikasi(String v) { this.ketSpesifikasi = v; }
	/** @return catatan bebas kriteria ketersediaan */
	@Column(name = "ket_ketersediaan") public String getKetKetersediaan() { return ketKetersediaan; }
	/** @param v catatan bebas kriteria ketersediaan */
	public void setKetKetersediaan(String v) { this.ketKetersediaan = v; }
	/** @return catatan bebas kriteria kejelasan penawaran */
	@Column(name = "ket_kejelasan") public String getKetKejelasan() { return ketKejelasan; }
	/** @param v catatan bebas kriteria kejelasan penawaran */
	public void setKetKejelasan(String v) { this.ketKejelasan = v; }
	/** @return catatan bebas kriteria legalitas vendor */
	@Column(name = "ket_legalitas") public String getKetLegalitas() { return ketLegalitas; }
	/** @param v catatan bebas kriteria legalitas vendor */
	public void setKetLegalitas(String v) { this.ketLegalitas = v; }
	/** @return catatan bebas kriteria pengalaman vendor */
	@Column(name = "ket_pengalaman") public String getKetPengalaman() { return ketPengalaman; }
	/** @param v catatan bebas kriteria pengalaman vendor */
	public void setKetPengalaman(String v) { this.ketPengalaman = v; }
	/** @return catatan bebas kriteria responsivitas vendor */
	@Column(name = "ket_responsif") public String getKetResponsif() { return ketResponsif; }
	/** @param v catatan bebas kriteria responsivitas vendor */
	public void setKetResponsif(String v) { this.ketResponsif = v; }
	/** @return catatan bebas kriteria kemudahan pembayaran */
	@Column(name = "ket_pembayaran") public String getKetPembayaran() { return ketPembayaran; }
	/** @param v catatan bebas kriteria kemudahan pembayaran */
	public void setKetPembayaran(String v) { this.ketPembayaran = v; }
	/** @return catatan bebas kriteria reputasi vendor */
	@Column(name = "ket_reputasi") public String getKetReputasi() { return ketReputasi; }
	/** @param v catatan bebas kriteria reputasi vendor */
	public void setKetReputasi(String v) { this.ketReputasi = v; }

	// ---- Section C ----
	/** @return nama/ringkasan vendor pembanding pertama (Section C) */
	@Column(name = "vendor_pembanding1") public String getVendorPembanding1() { return vendorPembanding1; }
	/** @param v nama/ringkasan vendor pembanding pertama */
	public void setVendorPembanding1(String v) { this.vendorPembanding1 = v; }
	/** @return nama/ringkasan vendor pembanding kedua (Section C) */
	@Column(name = "vendor_pembanding2") public String getVendorPembanding2() { return vendorPembanding2; }
	/** @param v nama/ringkasan vendor pembanding kedua */
	public void setVendorPembanding2(String v) { this.vendorPembanding2 = v; }
	/** @return nama/ringkasan vendor pembanding ketiga (Section C) */
	@Column(name = "vendor_pembanding3") public String getVendorPembanding3() { return vendorPembanding3; }
	/** @param v nama/ringkasan vendor pembanding ketiga */
	public void setVendorPembanding3(String v) { this.vendorPembanding3 = v; }

	/** @return alasan vendor tertentu dipilih di antara para pembanding */
	@Column(name = "alasan_dipilih") public String getAlasanDipilih() { return alasanDipilih; }
	/** @param v alasan vendor tertentu dipilih */
	public void setAlasanDipilih(String v) { this.alasanDipilih = v; }

	// ---- Section D ----
	/** @return rekomendasi akhir Section D; salah satu konstanta {@code REKOM_*} */
	@Column(name = "rekomendasi") public String getRekomendasi() { return rekomendasi; }
	/** @param v rekomendasi akhir; sebaiknya salah satu konstanta {@code REKOM_*} */
	public void setRekomendasi(String v) { this.rekomendasi = v; }

	/** @return nomor urut vendor (1..n) yang direkomendasikan, mengacu urutan pada {@link SeleksiVendorDetail} */
	@Column(name = "rekomendasi_nomor") public Integer getRekomendasiNomor() { return rekomendasiNomor; }
	/** @param v nomor urut vendor yang direkomendasikan */
	public void setRekomendasiNomor(Integer v) { this.rekomendasiNomor = v; }

	/**
	 * Mengembalikan vendor yang direkomendasikan Section D, setelah proksi malasnya diselesaikan
	 * {@code check(...)}.
	 *
	 * @return vendor rekomendasi, atau {@code null} bila belum dipilih
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "rekomendasi_penyedia", nullable = true)
	public Penyedia getRekomendasiPenyedia() {
		rekomendasiPenyedia = check(rekomendasiPenyedia);
		return rekomendasiPenyedia;
	}

	/**
	 * Menyetel vendor yang direkomendasikan Section D.
	 *
	 * @param rekomendasiPenyedia vendor rekomendasi; boleh {@code null}
	 */
	public void setRekomendasiPenyedia(Penyedia rekomendasiPenyedia) {
		this.rekomendasiPenyedia = rekomendasiPenyedia;
	}

	/** @return alasan utama rekomendasi Section D */
	@Column(name = "alasan_utama") public String getAlasanUtama() { return alasanUtama; }
	/** @param v alasan utama rekomendasi */
	public void setAlasanUtama(String v) { this.alasanUtama = v; }

	// ---- Section E - Penilai ----
	/** @return nama penilai yang menandatangani Section E */
	@Column(name = "nama_penilai") public String getNamaPenilai() { return namaPenilai; }
	/** @param v nama penilai Section E */
	public void setNamaPenilai(String v) { this.namaPenilai = v; }

	/** @return jabatan penilai Section E */
	@Column(name = "jabatan_penilai") public String getJabatanPenilai() { return jabatanPenilai; }
	/** @param v jabatan penilai Section E */
	public void setJabatanPenilai(String v) { this.jabatanPenilai = v; }

	/** @return tanggal Section E ditandatangani */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_penilaian")
	public Date getTanggalPenilaian() { return tanggalPenilaian; }
	/** @param v tanggal Section E ditandatangani */
	public void setTanggalPenilaian(Date v) { this.tanggalPenilaian = v; }

	/** @return tanda tangan penilai (umumnya tautan/berkas gambar) Section E */
	@Column(name = "ttd") public String getTtd() { return ttd; }
	/** @param v tanda tangan penilai Section E */
	public void setTtd(String v) { this.ttd = v; }

}
