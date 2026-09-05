package ais.database.model.asset;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
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
import ais.database.model.akunting.PostingHistory;

/**
 * Satu BARIS JADWAL PENYUSUTAN (depresiasi) untuk satu unit fisik aset ({@link AssetDetail})
 * pada satu periode ke-n ({@code tahunKe}) -- inilah yang memegang beban penyusutan dan nilai
 * buku aset, BUKAN {@link AssetDetail} itu sendiri (lihat javadoc {@link AssetDetail}, bagian
 * "Nilai perolehan: diturunkan, bukan diketik").
 *
 * <h3>Rumus yang dijalankan -- diverifikasi dari kode</h3>
 *
 * <p>Beban penyusutan per periode ({@link #getNilaiPenyusutan()}) dihitung garis lurus (straight
 * line): {@code hargaBeli / umurEkonomis}, keduanya diambil dari {@link
 * AssetDetail#getHargaBeli()} dan {@link AssetDetail#getUmurEkonomis()}. Nilai buku pada periode
 * ke-n ({@link #getNilaiBuku()}) adalah {@code max(0, hargaBeli - (beban * tahunKe)) + nilaiMinimal},
 * dengan {@code nilaiMinimal} ({@link AssetDetail#getNilaiMinimal()}) bertindak sebagai nilai
 * residu/sisa setelah disusutkan penuh. Komponen {@code hargaBeli - (beban * tahunKe)} DIPAKSA
 * {@code 0.0} bila hasilnya kurang dari {@code 0.01} SEBELUM {@code nilaiMinimal} ditambahkan --
 * inilah mekanisme yang membuat nilai buku TIDAK PERNAH turun di bawah {@code nilaiMinimal}
 * meski {@code tahunKe} jauh melampaui umur ekonomis unit (lihat juga javadoc {@link
 * AssetDetail#getNilaiMinimal()}, yang menegaskan hal yang sama dari sisi {@code AssetDetail}).
 * Lihat javadoc {@link #getNilaiBuku()} untuk detail ambang batas ini.</p>
 *
 * <h3>Getter destruktif -- pola yang sama dengan {@link AssetDetail}</h3>
 *
 * <p>Seperti {@link AssetDetail}, entitas ini memakai akses properti dan getter-getternya
 * MENGHITUNG ULANG lalu MENULIS BALIK ke field in-memory setiap kali dipanggil ({@link
 * #getNama()}, {@link #getNilaiPenyusutan()}, {@link #getNilaiBuku()}, {@link #getKodeUnik()},
 * {@link #getPerTanggal()}) -- nilai turunan ini ikut ter-flush ke basis data pada dirty-checking
 * Hibernate berikutnya.</p>
 *
 * <h3>Posting jurnal</h3>
 *
 * <p>Berbeda dari {@link PerbaikanAsset} (yang TIDAK memiliki jalur akuntansi sama sekali),
 * kelas ini memegang relasi {@link #getPostingHistory()} dan diposting lewat
 * {@code PostingPenyusutanAssetAction} -- jadwal penyusutan di sini karena itu memang dimaksudkan
 * masuk ke buku besar akuntansi, konsisten dengan sifatnya sebagai beban periodik aset tetap.</p>
 *
 * @see AssetDetail unit fisik yang disusutkan; sumber hargaBeli/umurEkonomis/nilaiMinimal
 * @see PerbaikanAsset riwayat perbaikan unit yang sama (jalur non-akuntansi, kontras dengan kelas ini)
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "penyusutan_asset")
public class PenyusutanAsset extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java; sama dengan entitas sepaket lain karena berasal dari
	 * templat hbm2java yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama baris, kolom {@code id}; di-generate database (IDENTITY). */
	private Long id;

	/** Nama tampil pengguna terakhir yang menyunting baris ini. */
	private String oleh;

	/** Id pengguna terakhir yang menyunting baris ini. */
	private String olehId;

	/**
	 * Id pengguna terakhir yang menyunting baris ini.
	 *
	 * @return id pengguna penyunting terakhir, atau {@code null} bila belum terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna penyunting terakhir, MENGABAIKAN nilai kosong agar jejak audit lama
	 * tidak tertimpa oleh proses batch tanpa konteks pengguna aktif.
	 *
	 * @param olehId id pengguna penyunting; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna penyunting terakhir, MENGABAIKAN nilai kosong.
	 *
	 * @param oleh nama pengguna penyunting; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Nama tampil pengguna terakhir yang menyunting baris ini.
	 *
	 * @return nama penyunting terakhir, atau {@code null} bila belum terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: memperbarui stempel waktu audit sebelum UPDATE dikirim, lalu
	 * mendeklarasikan field {@code tanggal_dirubah} pada baris yang sama (gaya penulisan padat
	 * warisan hbm2java, tidak diformat ulang agar diff commit tetap minimal terhadap bagian yang
	 * tidak diedit). Didelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} agar aturannya
	 * terpusat.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu penyuntingan terakhir.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu penyuntingan terakhir baris ini.
	 *
	 * @return stempel waktu; tidak pernah {@code null} untuk objek hasil konstruktor
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks berupa nama baris apa adanya, dipakai label komponen ZK.
	 *
	 * <p>Berbeda dari {@link #getNama()}, di sini field {@code nama} dibaca LANGSUNG (bukan lewat
	 * getter), sehingga {@code toString()} tidak memicu efek samping penurunan nama dari {@link
	 * #assetDetail} dan {@link #tahunKe}.</p>
	 *
	 * @return nama baris apa adanya; bisa {@code null} bila belum pernah diturunkan atau diisi
	 */
	public String toString() {
		return nama;
	}

	/** Nama baris jadwal penyusutan; DITURUNKAN dari barcode+nama unit+tahunKe, lihat {@link #getNama()}. */
	private String nama;

	/** Keterangan bebas untuk baris ini. */
	private String keterangan;

	/** Unit fisik aset yang disusutkan; SUMBER seluruh angka penyusutan (hargaBeli, umurEkonomis, nilaiMinimal, tanggalBeli). */
	private AssetDetail assetDetail;

	/** Tanggal akhir periode ke-{@link #tahunKe} sejak tanggal beli; DITURUNKAN, lihat {@link #getPerTanggal()}. */
	private Date perTanggal;

	/** Periode ke-berapa (dihitung dalam satuan bulan sejak tanggal beli, meski nama field menyiratkan "tahun"); lihat {@link #getPerTanggal()}. */
	private Integer tahunKe;

	/** Beban penyusutan per periode; DITURUNKAN dari hargaBeli/umurEkonomis, lihat {@link #getNilaiPenyusutan()}. */
	private Double nilaiPenyusutan = 0.0;

	/** Nilai buku aset pada akhir periode ke-{@link #tahunKe}; DITURUNKAN, lihat {@link #getNilaiBuku()}. */
	private Double nilaiBuku;

	/** Kode unik baris, kombinasi id unit dan periode; lihat {@link #getKodeUnik()}. */
	private String kodeUnik;

	/** Jejak posting jurnal penyusutan baris ini ke buku besar. */
	private PostingHistory postingHistory;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate dan ZK data binding.
	 */
	public PenyusutanAsset() {
	}

	/**
	 * Kunci utama baris.
	 *
	 * <p>{@code insertable = false} karena nilainya di-generate database.</p>
	 *
	 * @return id baris, atau {@code null} bila objek belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama; umumnya hanya dipanggil Hibernate seusai INSERT.
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama baris jadwal penyusutan -- getter DESTRUKTIF yang MENYUSUN nama dari barcode dan nama
	 * unit fisik digabung nomor periode, lalu menulis balik ke field {@code nama}.
	 *
	 * <p>Bila {@link #assetDetail} dan {@link #tahunKe} keduanya tidak {@code null}, nama
	 * dibentuk ulang sebagai {@code "<barcode>-<namaUnit>-<tahunKe>"} setiap kali getter ini
	 * dipanggil -- MENIMPA nama kustom apa pun yang sebelumnya diisi manual, selama kedua field
	 * sumber tersebut terisi. Bila salah satu {@code assetDetail} atau {@code tahunKe} kosong,
	 * nilai field lama (bila ada) dipertahankan apa adanya.</p>
	 *
	 * @return nama hasil {@code trim()}, atau {@code null} bila field kosong dan
	 *         {@code assetDetail}/{@code tahunKe} juga tidak tersedia untuk menyusunnya
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if (assetDetail != null && tahunKe != null) {
			nama = assetDetail.getBarcode() + "-" + assetDetail.getNama() + "-" + tahunKe;
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama baris secara manual; nilai ini bisa TERTIMPA oleh {@link #getNama()} pada
	 * pemanggilan berikutnya bila {@link #assetDetail} dan {@link #tahunKe} tersedia -- lihat
	 * javadoc getter tersebut.
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas untuk baris ini.
	 *
	 * <p>Mengembalikan teks kosong (bukan {@code null}) bila belum terisi, agar komponen ZK yang
	 * terikat langsung ke properti ini tidak menampilkan "null".</p>
	 *
	 * @return keterangan, atau {@code ""} bila belum terisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan == null ? "" : this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas.
	 *
	 * @param keterangan teks keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Unit fisik aset yang disusutkan -- kolom FK {@code nullable = false}, setiap baris jadwal
	 * penyusutan WAJIB menunjuk ke satu unit fisik.
	 *
	 * <p>Dipetakan {@code Fetch(FetchMode.SELECT)} sehingga Hibernate menerbitkan SELECT terpisah
	 * untuk relasi ini alih-alih ikut dalam JOIN. Berbeda dari beberapa relasi lain di paket ini,
	 * getter ini TIDAK memanggil {@code check(...)}, sehingga nilai yang dikembalikan bisa berupa
	 * proxy lazy yang belum teresolusi.</p>
	 *
	 * @return unit fisik yang disusutkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "asset_detail", nullable = false)
	public AssetDetail getAssetDetail() {
		return assetDetail;
	}

	/**
	 * Menetapkan unit fisik yang disusutkan.
	 *
	 * @param assetDetail unit fisik baru
	 */
	public void setAssetDetail(AssetDetail assetDetail) {
		this.assetDetail = assetDetail;
	}

	/**
	 * Nomor periode ke-berapa dalam jadwal penyusutan unit ini.
	 *
	 * <p><b>Perhatian satuan:</b> meski nama field menyiratkan "tahun", {@link #getPerTanggal()}
	 * MEMPERLAKUKAN nilai ini sebagai OFFSET BULAN (ditambahkan langsung ke {@link
	 * Calendar#MONTH} tanggal beli, bukan ke {@link Calendar#YEAR}) -- lihat javadoc getter
	 * tersebut. Nilai ini juga dipakai sebagai pengali pada {@link #getNilaiBuku()}
	 * ({@code beban * tahunKe}), sehingga satuannya harus konsisten dengan satuan {@link
	 * AssetDetail#getUmurEkonomis()} (yang menjadi pembagi beban penyusutan) agar rumus tetap
	 * benar secara ekonomis -- kelas ini sendiri tidak memvalidasi konsistensi satuan tersebut.</p>
	 *
	 * @return nomor periode
	 */
	@Column(name = "tahun_ke", nullable = false)
	public Integer getTahunKe() {
		return tahunKe;
	}

	/**
	 * Mengisi nomor periode.
	 *
	 * @param tahunKe nomor periode baru
	 */
	public void setTahunKe(Integer tahunKe) {
		this.tahunKe = tahunKe;
	}

	/**
	 * Beban penyusutan per periode -- getter DESTRUKTIF yang MENGHITUNG ULANG nilainya (garis
	 * lurus/straight-line) dan menulis balik ke field {@code nilaiPenyusutan} setiap kali
	 * dipanggil, selama seluruh prasyarat berikut terpenuhi.
	 *
	 * <h3>Rumus dan prasyarat</h3>
	 *
	 * <p>Rumus: {@code hargaBeli / umurEkonomis}, keduanya diambil DARI {@link #assetDetail}
	 * (yakni {@link AssetDetail#getHargaBeli()} dan {@link AssetDetail#getUmurEkonomis()} --
	 * kedua getter itu sendiri destruktif dan bisa mewarisi nilai dari katalog, lihat javadoc
	 * masing-masing). Perhitungan hanya dijalankan bila SEMUA syarat berikut benar: {@link
	 * #assetDetail} tidak {@code null}, {@link AssetDetail#getAsset()} tidak {@code null} (unit
	 * punya induk pengelompokan), {@code umurEkonomis > 0.1}, dan {@code hargaBeli > 0.1}. Bila
	 * salah satu syarat gagal, nilai LAMA (atau {@code 0.0} bila belum pernah dihitung)
	 * dipertahankan -- TIDAK ada nilai penyusutan yang dipaksa nol secara eksplisit dalam kasus
	 * ini, hanya tidak diperbarui.</p>
	 *
	 * <h3>Penanganan galat: fail-open diam-diam</h3>
	 *
	 * <p>Seluruh blok perhitungan dibungkus {@code try/catch} yang MENELAN exception apa pun
	 * (mis. {@link NullPointerException} bila {@code assetDetail.getAsset()} melempar galat lain
	 * saat resolusi proxy lazy) -- hanya direkam ke {@link ais.common.ErrorAuditUtil#record}
	 * sebagai audit, TIDAK dilemparkan ulang maupun ditampilkan ke pengguna. Ini FAIL-OPEN:
	 * kegagalan perhitungan tidak menghentikan alur kerja (mis. penyimpanan baris atau posting
	 * jurnal berjalan terus dengan nilai lama/nol), yang bisa menghasilkan beban penyusutan salah
	 * TANPA pemberitahuan eksplisit ke pengguna maupun penghentian proses bila galat tersembunyi
	 * ini terjadi berulang.</p>
	 *
	 * @return beban penyusutan per periode; {@code 0.0} bila belum pernah berhasil dihitung
	 */
	public Double getNilaiPenyusutan() {
		try {
			if (assetDetail != null && assetDetail.getAsset() != null && assetDetail.getUmurEkonomis() > 0.1
					&& assetDetail.getHargaBeli() > 0.1) {
				nilaiPenyusutan = assetDetail.getHargaBeli() / assetDetail.getUmurEkonomis();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/asset/PenyusutanAsset.java:153");
			// TODO: handle exception
		}
		return nilaiPenyusutan == null ? 0.0 : nilaiPenyusutan;
	}

	/**
	 * Mengisi beban penyusutan secara manual; nilai ini bisa TERTIMPA oleh {@link
	 * #getNilaiPenyusutan()} pada pemanggilan berikutnya bila prasyarat perhitungan otomatis
	 * terpenuhi -- lihat javadoc getter tersebut.
	 *
	 * @param nilaiPenyusutan beban penyusutan baru
	 */
	public void setNilaiPenyusutan(Double nilaiPenyusutan) {
		this.nilaiPenyusutan = nilaiPenyusutan;
	}

	/**
	 * Nilai buku aset pada akhir periode ke-{@link #getTahunKe()} -- getter DESTRUKTIF yang
	 * MENGHITUNG ULANG nilainya dan menulis balik ke field {@code nilaiBuku} setiap kali
	 * dipanggil, selama prasyaratnya terpenuhi. Method inilah rujukan utama untuk pertanyaan
	 * "berapa nilai buku aset ini sekarang" di seluruh AIS (lihat javadoc kelas {@link
	 * AssetDetail}, bagian "Nilai perolehan: diturunkan, bukan diketik").
	 *
	 * <h3>Rumus lengkap</h3>
	 *
	 * <p>Bila {@code n = getNilaiPenyusutan()} lebih dari {@code 0.1}, DAN
	 * {@code assetDetail.getHargaBeli() > 0.1}, DAN {@link #getTahunKe()} tidak {@code null}:
	 * <ol>
	 * <li>{@code m = hargaBeli - (n * tahunKe)} -- nilai buku SEBELUM nilai residu ditambahkan;</li>
	 * <li>bila {@code m < 0.01}, {@code m} DIPAKSA {@code 0.0} -- inilah mekanisme yang mencegah
	 * komponen tersusutkan menjadi negatif setelah {@code tahunKe} melampaui umur ekonomis;</li>
	 * <li>{@code nilaiBuku = m + assetDetail.getNilaiMinimal()} -- nilai residu SELALU
	 * ditambahkan di langkah terakhir ini.</li>
	 * </ol>
	 * Kombinasi langkah 2 dan 3 inilah yang membuat nilai buku TIDAK PERNAH turun di bawah {@link
	 * AssetDetail#getNilaiMinimal()} berapa pun besar {@code tahunKe} -- begitu {@code m} mencapai
	 * nol, nilai buku menetap tepat di {@code nilaiMinimal} untuk seterusnya (bukan terus turun
	 * ataupun naik kembali). Sebaliknya, bila salah satu dari ketiga prasyarat di atas gagal
	 * (mis. {@code getNilaiPenyusutan()} sendiri gagal dihitung dan bernilai {@code 0.0} karena
	 * fail-open pada getter itu -- lihat javadoc {@link #getNilaiPenyusutan()}), nilai LAMA
	 * (atau {@code 0.0} bila belum pernah dihitung) dipertahankan APA ADANYA, TANPA nilai
	 * residu ditambahkan -- baris yang gagal dihitung karena itu bisa tampil {@code 0.0} alih-alih
	 * {@code nilaiMinimal}, kontras dengan baris yang berhasil dihitung penuh.</p>
	 *
	 * <p><b>Catatan potensi {@link NullPointerException}:</b> akses {@code assetDetail.getHargaBeli()}
	 * pada baris kedua kondisi TIDAK dilindungi null-check terhadap {@link #assetDetail} sendiri
	 * (berbeda dari {@link #getNilaiPenyusutan()} yang memeriksa {@code assetDetail != null}
	 * lebih dulu) -- bila method ini dipanggil pada baris {@code PenyusutanAsset} yang
	 * {@code assetDetail}-nya belum diisi, baris ini akan melempar {@link
	 * NullPointerException} yang TIDAK ditangkap di sini (berbeda dari {@link
	 * #getNilaiPenyusutan()} yang membungkus seluruh perhitungannya dengan {@code try/catch}).
	 * Karena kolom {@code asset_detail} di tabel ini {@code nullable = false}, risiko ini secara
	 * praktik hanya muncul pada objek in-memory yang belum lengkap diisi sebelum disimpan.</p>
	 *
	 * @return nilai buku aset pada periode ke-{@code tahunKe}; {@code 0.0} bila belum pernah
	 *         berhasil dihitung
	 */
	public Double getNilaiBuku() {
		Double n = getNilaiPenyusutan();
		if (n > 0.1 && assetDetail.getHargaBeli() > 0.1 && getTahunKe() != null) {
			Double m = (assetDetail.getHargaBeli() - (n * getTahunKe()));
			if (m < 0.01) {
				m = 0.0;
			}
			nilaiBuku = m + assetDetail.getNilaiMinimal();

		}
		return nilaiBuku == null ? 0.0 : nilaiBuku;
	}

	/**
	 * Mengisi nilai buku secara manual; nilai ini bisa TERTIMPA oleh {@link #getNilaiBuku()} pada
	 * pemanggilan berikutnya bila prasyarat perhitungan otomatis terpenuhi -- lihat javadoc
	 * getter tersebut.
	 *
	 * @param nilaiBuku nilai buku baru
	 */
	public void setNilaiBuku(Double nilaiBuku) {
		this.nilaiBuku = nilaiBuku;
	}

	/**
	 * Kode unik baris -- getter DESTRUKTIF yang MENGHITUNG ULANG kombinasi id unit fisik dan
	 * nomor periode setiap kali dipanggil, lalu menulis balik ke field {@code kodeUnik}. Karena
	 * kolom ini {@code unique = true} di basis data, kombinasi ini mencegah dua baris jadwal
	 * penyusutan untuk UNIT DAN PERIODE yang sama tercatat lebih dari sekali.
	 *
	 * @return {@code "<idAssetDetail>-<tahunKe>"} bila {@link #assetDetail} dan {@link #tahunKe}
	 *         keduanya terisi; nilai field lama (bisa {@code null}) bila salah satunya kosong
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		if (assetDetail != null && tahunKe != null) {
			kodeUnik = assetDetail.getId() + "-" + tahunKe;
		}
		return kodeUnik;
	}

	/**
	 * Mengisi kode unik secara manual; nilai ini bisa TERTIMPA oleh {@link #getKodeUnik()} pada
	 * pemanggilan berikutnya bila {@link #assetDetail} dan {@link #tahunKe} tersedia.
	 *
	 * @param kodeUnik nilai baru
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * Tanggal akhir periode ke-{@link #getTahunKe()} -- getter DESTRUKTIF yang MENGHITUNG ULANG
	 * tanggal ini dari {@link AssetDetail#getTanggalBeli()} ditambah {@link #tahunKe}, lalu
	 * menulis balik ke field {@code perTanggal}.
	 *
	 * <p><b>Perhatian satuan:</b> nilai {@link #tahunKe} ditambahkan ke komponen {@link
	 * Calendar#MONTH} tanggal beli ({@code calendar.set(Calendar.MONTH, ... + getTahunKe())}),
	 * BUKAN ke {@link Calendar#YEAR} -- meski nama field {@code tahunKe} menyiratkan satuan
	 * tahun, method ini secara konkret memperlakukannya sebagai OFFSET BULAN. Bila
	 * {@link #getTahunKe()} atau {@link AssetDetail#getTanggalBeli()} kosong, nilai field lama
	 * (bisa {@code null}) dipertahankan.</p>
	 *
	 * @return tanggal akhir periode ke-{@code tahunKe} sejak tanggal beli, atau {@code null} bila
	 *         belum bisa dihitung dan belum pernah diisi
	 */
	@Temporal(TemporalType.DATE)
	public Date getPerTanggal() {
		if (getTahunKe() != null && assetDetail.getTanggalBeli() != null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(assetDetail.getTanggalBeli());
			calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + getTahunKe());
			perTanggal = calendar.getTime();
		}
		return perTanggal;
	}

	/**
	 * Mengisi tanggal akhir periode secara manual; nilai ini bisa TERTIMPA oleh {@link
	 * #getPerTanggal()} pada pemanggilan berikutnya bila prasyarat perhitungan otomatis
	 * terpenuhi.
	 *
	 * @param perTanggal tanggal baru
	 */
	public void setPerTanggal(Date perTanggal) {
		this.perTanggal = perTanggal;
	}

	/**
	 * Jejak posting jurnal penyusutan baris ini ke buku besar akuntansi -- lihat
	 * {@code PostingPenyusutanAssetAction}. Dipetakan {@code Fetch(FetchMode.SELECT)} sehingga
	 * Hibernate menerbitkan SELECT terpisah untuk relasi ini alih-alih ikut dalam JOIN.
	 *
	 * @return jejak posting, atau {@code null} bila baris ini belum pernah diposting
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * Menetapkan jejak posting jurnal.
	 *
	 * @param postingHistory jejak posting baru
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}
}
