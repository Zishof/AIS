package ais.database.model.epsbed;

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



import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;

/**
 * Model data untuk epsbed publikasi ilmiah dosen. Tipe ini membawa state yang dipertukarkan oleh
 * lapisan persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi
 * yang dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code Date tanggal_dirubah}, {@code Dosen dosen}, {@code EpsbedJenisKaryaIlmiah
 * kodeJenisPenelitian}, {@code EpsbedMediaPublikasi kodeMediaPublikasi}, {@code EpsbedPeranPenulisan
 * kodeAuthor}; pemetaan persistence: tabel {@code epsbed.epsbed_publikasi_ilmiah_dosen}; pembacaan/pencarian
 * ({@code getOlehId()}, {@code getId()}, {@code getOleh()}, {@code getTanggal_dirubah()}, {@code getDosen()},
 * {@code getKodeJenisPenelitian()}); mutasi data ({@code setOlehId()}, {@code setId()}, {@code setOleh()},
 * {@code onUpdate()}, {@code setTanggal_dirubah()}, {@code setDosen()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * <p><b>Relasi dengan {@link EpsbedPublikasiDosen} dan status dorman (diverifikasi dari kode, bukan asumsi
 * nama):</b> kelas ini bukan header/detail dari {@link EpsbedPublikasiDosen} — keduanya memetakan tabel
 * independen dengan relasi {@link Dosen} dan referensi kode yang identik, namun skema judul berbeda: kelas ini
 * hanya menyimpan satu {@code judul} (kolom {@code text}) plus {@code url} tautan publikasi, tanpa kolom urut,
 * sedangkan {@link EpsbedPublikasiDosen} memecah judul ke lima kolom ({@code judul1}..{@code judul5}) plus
 * {@code urut}. Entity ini tidak memiliki lapisan DAO sama sekali — tidak ada
 * {@code PublikasiIlmiahDosenDao}/{@code DaoFactory} yang memetakannya; satu-satunya pemakainya,
 * {@code ais.action.master.epsbed.PublikasiIlmiahDosenHelper}, mengakses entity ini langsung lewat
 * {@code session.createCriteria(EpsbedPublikasiIlmiahDosen.class)}. Helper tersebut sendiri tidak
 * diinstansiasi/dirujuk dari kelas Java lain mana pun di WC ini (tidak ditemukan {@code new
 * PublikasiIlmiahDosenHelper(...)} di seluruh pohon sumber), berbeda dengan {@link EpsbedPublikasiDosen} yang
 * punya DAO ({@code PublikasiDosenDao} via {@code DaoFactory#getPublikasiDosenDao()}) serta dipakai lewat
 * {@code PublikasiDosenHelper} dan {@code TransaksiPublikasiDosen}. Bukti ini menunjuk entity ini sebagai versi
 * lama/lebih sederhana yang tampaknya sudah digantikan oleh {@link EpsbedPublikasiDosen} dan kini yatim/dorman
 * dari sisi kode Java yang tersedia — walau kemungkinan masih ada rujukan menu/tab ZK di WC webapp terpisah
 * (repo {@code ^/web}) yang tidak diperiksa di sini. Karena tabel {@code epsbed_publikasi_ilmiah_dosen} tetap
 * ada dan berisi data dosen (nama publikasi, tahun, URL) bila pernah dipakai, entity yang dorman ini tetap
 * relevan dicatat sebagai kandidat audit data historis, bukan dihapus begitu saja.</p>
 *
 * @see GeneralValueObject
 * @see EpsbedPublikasiDosen
 * @see Dosen
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "epsbed", name = "epsbed_publikasi_ilmiah_dosen")



public class EpsbedPublikasiIlmiahDosen extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 8445312019405120038L;

	/** Primary key baris publikasi ilmiah dosen; dibangkitkan otomatis oleh database (identity). */
	private Long id;
	/** Nama pencatat perubahan terakhir; kolom audit yang diisi oleh lapisan pemanggil, bukan Hibernate. */
	private String oleh;
	/** Id pencatat perubahan terakhir; pasangan dari {@link #oleh}. */
	private String olehId;
	/**
	 * Mengembalikan id pencatat perubahan terakhir baris ini.
	 *
	 * @return id pencatat, atau {@code null} bila belum pernah diset.
	 */
	public String getOlehId() {return olehId;}
	/**
	 * Menetapkan id pencatat perubahan terakhir. Nilai kosong/blank diabaikan sehingga id pencatat
	 * yang sudah tersimpan tidak pernah ditimpa nilai kosong.
	 *
	 * @param olehId id pencatat baru; diabaikan jika {@code null} atau string kosong setelah di-trim.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * @return id baris, atau {@code null} bila entity belum pernah dipersist.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan primary key baris ini. Kolom dipetakan {@code insertable = false} sehingga nilai ini
	 * normalnya diisi oleh Hibernate dari identity generator database.
	 *
	 * @param id nilai id baru.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Menetapkan nama pencatat perubahan terakhir. Nilai kosong/blank diabaikan, simetris dengan
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pencatat baru; diabaikan jika {@code null} atau string kosong setelah di-trim.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pencatat perubahan terakhir baris ini.
	 *
	 * @return nama pencatat, atau {@code null} bila belum pernah diset.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback lifecycle JPA {@code @PreUpdate}: dipanggil otomatis oleh provider persistence sebelum
	 * baris ini di-{@code UPDATE}, mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} untuk memperbarui
	 * {@link #tanggal_dirubah}. Tidak dipanggil manual oleh kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan cap waktu perubahan terakhir secara eksplisit.
	 *
	 * @param tanggal_dirubah cap waktu perubahan baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir baris ini, diperbarui otomatis oleh
	 * {@link #onUpdate()} pada setiap update.
	 *
	 * @return cap waktu perubahan terakhir, tidak pernah {@code null}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Dosen pemilik/penulis publikasi ilmiah ini. */
	private Dosen dosen;
	/** Kode jenis karya ilmiah (mis. jurnal, buku, prosiding) publikasi ini. */
	private EpsbedJenisKaryaIlmiah kodeJenisPenelitian;
	/** Kode media/wadah publikasi (mis. nama jurnal/penerbit) tempat karya ini dipublikasikan. */
	private EpsbedMediaPublikasi kodeMediaPublikasi;
	/** Kode peran penulisan dosen pada publikasi ini (mis. penulis utama/anggota). */
	private EpsbedPeranPenulisan kodeAuthor;
	/** Kode penanda apakah publikasi ini dikerjakan mandiri atau berkelompok. */
	private String kodeKegiatanMandiriKelompok;
	/** Tahun publikasi karya ilmiah ini diterbitkan. */
	private Integer tahunPublikasi;
	/** Bulan publikasi karya ilmiah ini diterbitkan (1-12). */
	private Integer bulanPublikasi;
	/** Kode sumber pembiayaan penelitian/publikasi ini. */
	private EpsbedPembiayaanPenelitian kodePembiayaan;
	/** Jumlah biaya (dalam satuan mata uang lokal) yang dipakai untuk penelitian/publikasi ini. */
	private Long jumlahBiaya;
	/** Judul lengkap publikasi ini sebagai satu kolom teks (berbeda dari {@link EpsbedPublikasiDosen} yang memecah judul ke lima kolom). */
	private String judul;
	/** Tautan (URL) menuju publikasi ini, bila tersedia daring. */
	private String url;

	/**
	 * Mengembalikan dosen pemilik/penulis publikasi ilmiah ini.
	 *
	 * @return relasi {@link Dosen}; wajib ada ({@code nullable = false} pada kolom join), dimuat
	 *         dengan fetch terpisah ({@link FetchMode#SELECT}) dan di-cascade {@code PERSIST}/
	 *         {@code MERGE}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "dosen", nullable = false)
	public Dosen getDosen() {
		return dosen;
	}

	/**
	 * Menetapkan dosen pemilik/penulis publikasi ilmiah ini.
	 *
	 * @param dosen relasi dosen baru.
	 */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/**
	 * Mengembalikan kode jenis karya ilmiah publikasi ini.
	 *
	 * @return relasi {@link EpsbedJenisKaryaIlmiah}, boleh {@code null} (kolom join nullable).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kode_jenis_penelitian", nullable = true)
	public EpsbedJenisKaryaIlmiah getKodeJenisPenelitian() {
		return kodeJenisPenelitian;
	}

	/**
	 * Menetapkan kode jenis karya ilmiah publikasi ini.
	 *
	 * @param kodeJenisPenelitian relasi kode jenis karya ilmiah baru.
	 */
	public void setKodeJenisPenelitian(EpsbedJenisKaryaIlmiah kodeJenisPenelitian) {
		this.kodeJenisPenelitian = kodeJenisPenelitian;
	}

	/**
	 * Mengembalikan kode media/wadah publikasi ini.
	 *
	 * @return relasi {@link EpsbedMediaPublikasi}, boleh {@code null} (kolom join nullable).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "media_publikasi", nullable = true)
	public EpsbedMediaPublikasi getKodeMediaPublikasi() {
		return kodeMediaPublikasi;
	}

	/**
	 * Menetapkan kode media/wadah publikasi ini.
	 *
	 * @param kodeMediaPublikasi relasi kode media publikasi baru.
	 */
	public void setKodeMediaPublikasi(EpsbedMediaPublikasi kodeMediaPublikasi) {
		this.kodeMediaPublikasi = kodeMediaPublikasi;
	}

	/**
	 * Mengembalikan kode peran penulisan dosen pada publikasi ini.
	 *
	 * @return relasi {@link EpsbedPeranPenulisan}, boleh {@code null} (kolom join nullable).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kode_author", nullable = true)
	public EpsbedPeranPenulisan getKodeAuthor() {
		return kodeAuthor;
	}

	/**
	 * Menetapkan kode peran penulisan dosen pada publikasi ini.
	 *
	 * @param kodeAuthor relasi kode peran penulisan baru.
	 */
	public void setKodeAuthor(EpsbedPeranPenulisan kodeAuthor) {
		this.kodeAuthor = kodeAuthor;
	}

	/**
	 * Mengembalikan kode penanda mandiri/kelompok pengerjaan publikasi ini.
	 *
	 * @return kode mandiri/kelompok, bisa {@code null} bila belum diisi.
	 */
	@Column(name = "kode_kegiatan_mandiri_kelompok")
	public String getKodeKegiatanMandiriKelompok() {
		return kodeKegiatanMandiriKelompok;
	}

	/**
	 * Menetapkan kode penanda mandiri/kelompok pengerjaan publikasi ini.
	 *
	 * @param kodeKegiatanMandiriKelompok kode baru.
	 */
	public void setKodeKegiatanMandiriKelompok(String kodeKegiatanMandiriKelompok) {
		this.kodeKegiatanMandiriKelompok = kodeKegiatanMandiriKelompok;
	}

	/**
	 * Mengembalikan tahun publikasi karya ilmiah ini diterbitkan.
	 *
	 * @return tahun publikasi, bisa {@code null} bila belum diisi.
	 */
	@Column(name = "tahun_publikasi")
	public Integer getTahunPublikasi() {
		return tahunPublikasi;
	}

	/**
	 * Menetapkan tahun publikasi karya ilmiah ini.
	 *
	 * @param tahunPublikasi tahun publikasi baru.
	 */
	public void setTahunPublikasi(Integer tahunPublikasi) {
		this.tahunPublikasi = tahunPublikasi;
	}

	/**
	 * Mengembalikan bulan publikasi karya ilmiah ini diterbitkan.
	 *
	 * @return bulan publikasi (1-12), bisa {@code null} bila belum diisi.
	 */
	@Column(name = "bulan_publikasi")
	public Integer getBulanPublikasi() {
		return bulanPublikasi;
	}

	/**
	 * Menetapkan bulan publikasi karya ilmiah ini.
	 *
	 * @param bulanPublikasi bulan publikasi baru (1-12).
	 */
	public void setBulanPublikasi(Integer bulanPublikasi) {
		this.bulanPublikasi = bulanPublikasi;
	}

	/**
	 * Mengembalikan kode sumber pembiayaan penelitian/publikasi ini.
	 *
	 * @return relasi {@link EpsbedPembiayaanPenelitian}, boleh {@code null} (kolom join nullable).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kode_pembiayaan", nullable = true)
	public EpsbedPembiayaanPenelitian getKodePembiayaan() {
		return kodePembiayaan;
	}

	/**
	 * Menetapkan kode sumber pembiayaan penelitian/publikasi ini.
	 *
	 * @param kodePembiayaan relasi kode pembiayaan baru.
	 */
	public void setKodePembiayaan(EpsbedPembiayaanPenelitian kodePembiayaan) {
		this.kodePembiayaan = kodePembiayaan;
	}

	/**
	 * Mengembalikan jumlah biaya penelitian/publikasi ini.
	 *
	 * @return jumlah biaya; {@code 0L} bila belum pernah diset (tidak pernah {@code null}).
	 */
	@Column(name = "jumlah_biaya")
	public Long getJumlahBiaya() {
		if (jumlahBiaya == null) {
			jumlahBiaya = 0L;
		}
		return jumlahBiaya;
	}

	/**
	 * Menetapkan jumlah biaya penelitian/publikasi ini.
	 *
	 * @param jumlahBiaya jumlah biaya baru.
	 */
	public void setJumlahBiaya(Long jumlahBiaya) {
		this.jumlahBiaya = jumlahBiaya;
	}

	/**
	 * Mengembalikan judul lengkap publikasi ini.
	 *
	 * @return judul publikasi (kolom {@code text}), bisa {@code null} bila belum diisi.
	 */
	@Column(columnDefinition = "text")
	public String getJudul() {
		return judul;
	}

	/**
	 * Menetapkan judul lengkap publikasi ini.
	 *
	 * @param judul judul baru.
	 */
	public void setJudul(String judul) {
		this.judul = judul;
	}

	/**
	 * Mengembalikan tautan (URL) publikasi ini, dengan fallback: bila {@link #url} belum pernah
	 * diset ({@code null}), field ini diisi (dimutasi sebagai efek samping pembacaan) dengan string
	 * kosong sebelum dikembalikan, sehingga pemanggil tidak perlu menangani {@code null} secara
	 * terpisah dari string kosong.
	 *
	 * @return tautan publikasi (kolom {@code text}); tidak pernah {@code null} setelah pemanggilan
	 *         pertama (string kosong bila belum diisi).
	 */
	@Column(columnDefinition = "text")
	public String getUrl() {
		if(url==null){
			url="";
		}
		return url;
	}

	/**
	 * Menetapkan tautan (URL) publikasi ini.
	 *
	 * @param url tautan baru.
	 */
	public void setUrl(String url) {
		this.url = url;
	}

}
