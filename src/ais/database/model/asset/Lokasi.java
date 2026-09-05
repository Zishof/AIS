package ais.database.model.asset;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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
import ais.database.model.inventory.Toko;
import ais.database.model.sirs.Bagian;
import ais.database.model.sirs.Gudang;

/**
 * <h2>Lokasi — master lokasi fisik aset/barang (tabel {@code asset.lokasi}).</h2>
 *
 * <p>
 * Entity ini adalah data master tempat fisik yang dijadikan rujukan oleh berbagai entity lain di
 * paket {@code ais.database.model.asset} (mis. {@link SaldoAwalMasterAsset}, {@link MutasiLokasi},
 * {@link PengirimanGudang}, {@code Asset}) untuk menandai <i>di mana</i> sebuah aset/barang berada.
 * Sebuah baris {@code Lokasi} boleh sekaligus mewakili sebuah {@link Bagian} (unit organisasi),
 * {@link Gudang}, dan/atau {@link Toko} — ketiga relasi bersifat opsional dan tidak saling
 * eksklusif, sehingga satu lokasi fisik dapat dilihat dari beberapa sudut pandang modul yang
 * berbeda (mis. sekaligus sebagai "gudang" pada modul pergudangan dan "toko" pada modul kasir).
 * </p>
 *
 * <h3>Data geografis &amp; jaringan</h3>
 * <p>
 * Menyimpan koordinat peta ({@link #getLat() lat}/{@link #getLng() lng}, default ke sebuah titik
 * di Jakarta bila belum diatur) beserta {@link #getCoordinate() coordinate} mentah dan alamat teks
 * bebas ({@link #getAlamat() alamat}, {@link #getDetailAlamat() detailAlamat}). Field
 * {@link #getIp() ip}/{@link #getIp1() ip1}..{@link #getIp4() ip4} menampung hingga lima alamat IP
 * per lokasi, dipakai modul lain (mis. pembatasan akses kasir/perangkat) untuk mengaitkan sebuah
 * alamat jaringan dengan lokasi fisik tertentu.
 * </p>
 *
 * <h3>Relasi &amp; audit</h3>
 * <p>
 * {@link #getJenisLokasi() jenisLokasi} mengklasifikasikan lokasi (lihat {@link JenisLokasi}) dan
 * bersifat opsional agar data lama yang belum dikategorikan tetap valid. Seperti entity lain di
 * paket ini, field jejak {@code oleh}/{@code olehId}/{@code tanggal_dirubah} diisi otomatis lewat
 * hook {@link javax.persistence.PreUpdate} {@link #onUpdate()}
 * ({@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}) dan direkam ke tabel
 * revisi Envers karena kelas ditandai {@link org.hibernate.envers.Audited @Audited}.
 * </p>
 *
 * @author AIS e-Kantin (modul pergudangan)
 * @see JenisLokasi
 * @see MutasiLokasi
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "lokasi")
public class Lokasi extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}; tidak perlu
	 * diubah kecuali bentuk field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key auto-generated (IDENTITY) tabel {@code asset.lokasi}. */
	private Long id;
	/** Nama pengguna yang terakhir mengubah baris ini; diisi otomatis, lihat {@link #onUpdate()}. */
	private String oleh;
	/** Id pengguna yang terakhir mengubah baris ini; diisi otomatis, lihat {@link #onUpdate()}. */
	private String olehId;

	/**
	 * @return id pengguna yang terakhir mengubah baris ini (audit), atau {@code null} bila belum
	 *         pernah diubah sejak dimuat.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna audit. Nilai {@code null}/kosong diabaikan (jejak lama tidak ditimpa
	 * hampa) agar tetap ada identitas terakhir yang valid.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau blank.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna audit. Nilai {@code null}/kosong diabaikan, sama seperti
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau blank.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna yang terakhir mengubah baris ini (audit), atau {@code null} bila
	 *         belum pernah diubah sejak dimuat.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook siklus hidup JPA yang dipanggil Hibernate tepat sebelum setiap {@code UPDATE}.
	 * Mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}
	 * yang mengisi {@link #tanggal_dirubah}, {@link #oleh}, dan {@link #olehId} dengan waktu serta
	 * identitas pengguna aktif. Dipicu otomatis oleh Hibernate, tidak dipanggil manual.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi waktu perubahan terakhir. Field diinisialisasi ke waktu saat objek dibuat, lalu
	 * ditimpa ulang oleh {@link #onUpdate()} setiap kali baris di-{@code UPDATE}.
	 *
	 * @param tanggal_dirubah waktu perubahan.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini; tidak pernah {@code null}. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi ringkas {@code id-nama} untuk log/combobox. */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama lokasi (mis. "Gudang Pusat", "Outlet Kantin A"); wajib diisi. */
	private String nama;
	/** Keterangan bebas, opsional. */
	private String keterangan;
	/** Lintang (latitude) titik lokasi; default titik Jakarta ({@code -6.195168}). */
	private Double lat = -6.195168;
	/** Bujur (longitude) titik lokasi; default titik Jakarta ({@code 106.846046}). */
	private Double lng = 106.846046;

	/** Koordinat mentah (format bebas, mis. hasil paste dari peta) sebagai teks; boleh kosong. */
	private String coordinate = "";
	/** Alamat lokasi dalam bentuk teks bebas; boleh kosong. */
	private String alamat = "";
	/** Detail alamat tambahan, disimpan dengan pembatas koma; lihat {@link #getDetailAlamat()}. */
	private String detailAlamat = "";
	/** Status aktif lokasi; {@code null} diperlakukan sebagai aktif, lihat {@link #getAktif()}. */
	private Boolean aktif;

	/** Alamat IP utama yang diasosiasikan dengan lokasi ini (mis. untuk pembatasan akses/kasir). */
	private String ip;
	/** Alamat IP tambahan #1 (multi-IP per lokasi). */
	private String ip1;
	/** Alamat IP tambahan #2 (multi-IP per lokasi). */
	private String ip2;
	/** Alamat IP tambahan #3 (multi-IP per lokasi). */
	private String ip3;
	/** Alamat IP tambahan #4 (multi-IP per lokasi). */
	private String ip4;
	/** Bagian/unit organisasi yang menaungi lokasi ini; opsional. */
	private Bagian bagian;
	/** Gudang terkait bila lokasi ini merupakan (atau berada di dalam) sebuah gudang; opsional. */
	private Gudang gudang;
	/** Toko/outlet terkait bila lokasi ini merupakan (atau berada di dalam) sebuah toko; opsional. */
	private Toko toko;
	/** Jenis/kategori lokasi (lihat {@link #getJenisLokasi()}); opsional. */
	private JenisLokasi jenisLokasi;

	/** Konstruktor default tanpa argumen, dipakai Hibernate untuk instansiasi via refleksi. */
	public Lokasi() {
	}

	/** @return primary key baris ini, atau {@code null} untuk instance baru yang belum disimpan. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi primary key. Kolom database bersifat {@code insertable = false} (IDENTITY,
	 * auto-generate oleh database), sehingga pengisian manual tidak berpengaruh pada
	 * {@code INSERT}.
	 *
	 * @param id primary key.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return nama lokasi, sudah di-{@code trim}; {@code null} bila belum diisi. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama lokasi. Tidak melakukan trim di sisi setter — trimming terjadi hanya saat
	 * dibaca lewat {@link #getNama()}.
	 *
	 * @param nama nama lokasi.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan bebas lokasi ini, boleh {@code null}. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas.
	 *
	 * @param keterangan teks keterangan, boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return koordinat mentah (format bebas) sebagai teks; tidak pernah {@code null}, default kosong. */
	@Column(name = "coordinate", columnDefinition = "text", nullable = true)
	public String getCoordinate() {
		return coordinate;
	}

	/**
	 * Mengisi koordinat mentah.
	 *
	 * @param coordinate teks koordinat, boleh kosong.
	 */
	public void setCoordinate(String coordinate) {
		this.coordinate = coordinate;
	}

	/** @return lintang (latitude) titik lokasi; default titik Jakarta bila belum diatur. */
	public Double getLat() {
		return lat;
	}

	/**
	 * Mengisi lintang (latitude) titik lokasi.
	 *
	 * @param lat lintang, dalam derajat desimal.
	 */
	public void setLat(Double lat) {
		this.lat = lat;
	}

	/** @return bujur (longitude) titik lokasi; default titik Jakarta bila belum diatur. */
	public Double getLng() {
		return lng;
	}

	/**
	 * Mengisi bujur (longitude) titik lokasi.
	 *
	 * @param lng bujur, dalam derajat desimal.
	 */
	public void setLng(Double lng) {
		this.lng = lng;
	}

	/** @return alamat lokasi sebagai teks bebas; boleh kosong, tidak pernah {@code null}. */
	@Column(name = "alamat", columnDefinition = "text", nullable = true)
	public String getAlamat() {
		return alamat;
	}

	/**
	 * Mengisi alamat lokasi.
	 *
	 * @param alamat teks alamat, boleh kosong.
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Mengembalikan detail alamat tambahan setelah dinormalisasi. Nilai mentah disimpan dengan
	 * pembatas koma di kedua ujung (mis. {@code ",RT 01,RW 02,"}) agar mudah dicari-per-token;
	 * getter ini membungkus ulang nilai tersimpan menjadi bentuk itu setiap kali dipanggil
	 * (bukan hanya saat disimpan), lalu menyederhanakan koma ganda berturut-turut
	 * ({@code ",,"} → {@code ","}) sebanyak tiga kali berturut-turut sebagai upaya membersihkan
	 * sisa penggabungan token kosong. Bila hasil akhir hanya berisi satu/dua/tiga koma tanpa isi
	 * ({@code ","}, {@code ",,"}, atau {@code ",,,"}), dianggap kosong dan dikembalikan sebagai
	 * string kosong. Hasil akhir di-{@code trim} sebelum dikembalikan.
	 *
	 * @return detail alamat yang sudah dinormalisasi (dibungkus koma) dan di-trim; string kosong
	 *         bila tidak ada isi bermakna.
	 */
	@Column(name = "detail_alamat", columnDefinition = "text", nullable = true)
	public String getDetailAlamat() {
		detailAlamat = (detailAlamat == null || detailAlamat.trim().equalsIgnoreCase(",") ? ""
				: "," + detailAlamat.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",").replaceAll(",,", ",");

		if (detailAlamat.equals(",")) {
			detailAlamat = "";
		} else if (detailAlamat.equals(",,")) {
			detailAlamat = "";
		} else if (detailAlamat.equals(",,,")) {
			detailAlamat = "";
		}

		return detailAlamat == null ? "" : detailAlamat.trim();
	}

	/**
	 * Mengisi detail alamat mentah (sebelum normalisasi); normalisasi baru terjadi saat dibaca
	 * lewat {@link #getDetailAlamat()}.
	 *
	 * @param detailAlamat teks detail alamat, boleh berisi token dipisah koma.
	 */
	public void setDetailAlamat(String detailAlamat) {
		this.detailAlamat = detailAlamat;
	}

	/**
	 * Status aktif lokasi. Bernilai {@code true} bila belum pernah diset (default aman) sehingga
	 * data lama otomatis dianggap aktif tanpa migrasi data.
	 *
	 * @return {@code true} bila lokasi ini masih berlaku/boleh dipilih.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengisi status aktif.
	 *
	 * @param aktif {@code true}/{@code false}; {@code null} diperlakukan sebagai aktif oleh
	 *              {@link #getAktif()}.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/** @return alamat IP utama lokasi ini, boleh {@code null}. */
	public String getIp() {
		return ip;
	}

	/**
	 * Mengisi alamat IP utama.
	 *
	 * @param ip alamat IP, boleh {@code null}.
	 */
	public void setIp(String ip) {
		this.ip = ip;
	}

	/** @return alamat IP tambahan #1, boleh {@code null}. */
	public String getIp1() {
		return ip1;
	}

	/**
	 * Mengisi alamat IP tambahan #1.
	 *
	 * @param ip1 alamat IP, boleh {@code null}.
	 */
	public void setIp1(String ip1) {
		this.ip1 = ip1;
	}

	/** @return alamat IP tambahan #2, boleh {@code null}. */
	public String getIp2() {
		return ip2;
	}

	/**
	 * Mengisi alamat IP tambahan #2.
	 *
	 * @param ip2 alamat IP, boleh {@code null}.
	 */
	public void setIp2(String ip2) {
		this.ip2 = ip2;
	}

	/** @return alamat IP tambahan #3, boleh {@code null}. */
	public String getIp3() {
		return ip3;
	}

	/**
	 * Mengisi alamat IP tambahan #3.
	 *
	 * @param ip3 alamat IP, boleh {@code null}.
	 */
	public void setIp3(String ip3) {
		this.ip3 = ip3;
	}

	/** @return alamat IP tambahan #4, boleh {@code null}. */
	public String getIp4() {
		return ip4;
	}

	/**
	 * Mengisi alamat IP tambahan #4.
	 *
	 * @param ip4 alamat IP, boleh {@code null}.
	 */
	public void setIp4(String ip4) {
		this.ip4 = ip4;
	}

	/**
	 * Mengembalikan bagian/unit organisasi yang menaungi lokasi ini, meresolusi proxy lazy
	 * Hibernate lewat {@link GeneralValueObject#check(Object)}.
	 *
	 * @return {@link Bagian} terkait, atau {@code null} bila belum ditetapkan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "bagian", nullable = true)
	public Bagian getBagian() {
		bagian = check(bagian);
		return bagian;
	}

	/**
	 * Mengisi bagian/unit organisasi terkait.
	 *
	 * @param bagian bagian terkait, boleh {@code null}.
	 */
	public void setBagian(Bagian bagian) {
		this.bagian = bagian;
	}

	/**
	 * Mengembalikan gudang terkait bila lokasi ini merupakan (atau berada di dalam) sebuah
	 * gudang, meresolusi proxy lazy Hibernate lewat {@link GeneralValueObject#check(Object)}.
	 *
	 * @return {@link Gudang} terkait, atau {@code null} bila lokasi ini bukan gudang.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gudang", nullable = true)
	public Gudang getGudang() {
		gudang = check(gudang);
		return gudang;
	}

	/**
	 * Mengisi gudang terkait.
	 *
	 * @param gudang gudang terkait, boleh {@code null}.
	 */
	public void setGudang(Gudang gudang) {
		this.gudang = gudang;
	}

	/**
	 * Mengembalikan toko/outlet terkait bila lokasi ini merupakan (atau berada di dalam) sebuah
	 * toko, meresolusi proxy lazy Hibernate lewat {@link GeneralValueObject#check(Object)}.
	 *
	 * @return {@link Toko} terkait, atau {@code null} bila lokasi ini bukan toko.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko", nullable = true)
	public Toko getToko() {
		toko = check(toko);
		return toko;
	}

	/**
	 * Mengisi toko/outlet terkait.
	 *
	 * @param toko toko terkait, boleh {@code null}.
	 */
	public void setToko(Toko toko) {
		this.toko = toko;
	}

	/**
	 * Jenis/kategori lokasi ini (mis. Gudang, Outlet, Kasir). Menentukan bagaimana lokasi
	 * diperlakukan pada laporan &amp; dasbor pergudangan (mis. dikelompokkan sebagai gudang vs outlet).
	 * Relasi opsional ({@code nullable}) agar data lama yang belum dikategorikan tetap valid.
	 *
	 * @return baris {@link JenisLokasi} terkait, atau {@code null} bila belum ditetapkan.
	 * @see JenisLokasi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_lokasi", nullable = true)
	public JenisLokasi getJenisLokasi() {
		jenisLokasi = check(jenisLokasi);
		return jenisLokasi;
	}

	/**
	 * Mengisi jenis/kategori lokasi.
	 *
	 * @param jenisLokasi jenis lokasi terkait, boleh {@code null}.
	 */
	public void setJenisLokasi(JenisLokasi jenisLokasi) {
		this.jenisLokasi = jenisLokasi;
	}

}
