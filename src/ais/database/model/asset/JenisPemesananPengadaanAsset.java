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
import ais.database.model.akunting.Akun;

/**
 * Entity master <b>Jenis Pemesanan Pengadaan Aset</b>
 * (tabel {@code asset.jenis_pemesanan_pengadaan_asset}) — katalog yang mendefinisikan
 * <i>bagaimana</i> sebuah pemesanan pengadaan diproses secara akuntansi: tiga akun buku besar
 * opsional ({@link #getAkunDp()}, {@link #getAkunUtangDp()}, {@link #getAkunUtangPekerjaan()})
 * dan dua flag perilaku ({@link #getAkunUtangDariAnggaran()},
 * {@link #getAdaProsesPenerimaan()}) yang benar-benar mempercabangkan alur mesin posting, bukan
 * sekadar label. Dipilih pada {@code PemesananPengadaanMasterAsset} lewat layar CRUD
 * {@code ais.action.master.asset.JenisPemesananPengadaanAssetAction}.
 *
 * <h2>Flag {@link #getAkunUtangDariAnggaran()}: dibaca berulang oleh posting pembayaran</h2>
 * <p>Dibaca berkali-kali oleh {@code ais.action.master.asset.PostingPembayaranAction} untuk
 * menentukan apakah akun hutang pembayaran diambil dari anggaran atau dari jalur akun hutang
 * normal. Defaultnya <b>{@code false}</b> saat {@code null} — kebalikan arah dari kebanyakan
 * flag {@code aktif}/{@code adaProsesPenerimaan} di paket ini yang default-nya {@code true}.
 * Baris lama yang belum pernah menyentuh field ini akan selalu dianggap <i>bukan</i> dari
 * anggaran.</p>
 *
 * <h2>Flag {@link #getAdaProsesPenerimaan()}: filter query, bukan sekadar tampilan</h2>
 * <p>Selain ditampilkan di grid CRUD, flag ini dipakai langsung sebagai kondisi
 * {@code Restrictions} pada {@code ais.action.master.asset.helper.AmbilDataPemesananPengadaanAsetBanbox}
 * (picker banyak-baris) untuk menyaring jenis pemesanan mana yang membutuhkan tahapan
 * penerimaan barang terpisah. Default-nya {@code true} saat {@code null}.</p>
 *
 * <h2>Field audit bayangan {@code oleh}/{@code olehId}/{@code tanggal_dirubah}</h2>
 * <p>Tidak diisi langsung oleh kode aplikasi. Hook {@link javax.persistence.PreUpdate}
 * {@link #onUpdate()} mendelegasikan ke
 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang menuliskan
 * pengguna aktif dan waktu perubahan pada setiap {@code UPDATE}. Ini keharusan teknis siklus
 * hidup Hibernate, bukan kode mati.</p>
 *
 * @see ais.database.model.asset.PemesananPengadaanMasterAsset
 * @see ais.action.master.asset.JenisPemesananPengadaanAssetAction
 * @see ais.action.master.asset.PostingPembayaranAction
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "jenis_pemesanan_pengadaan_asset")
public class JenisPemesananPengadaanAsset extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}; tidak pernah
	 * perlu diubah kecuali bentuk field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key auto-generated (IDENTITY) tabel {@code asset.jenis_pemesanan_pengadaan_asset}. */
	private Long id;
	/** Nama pengguna yang terakhir mengubah baris ini; diisi otomatis, lihat catatan kelas. */
	private String oleh;
	/** Id pengguna yang terakhir mengubah baris ini; diisi otomatis, lihat catatan kelas. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna audit, atau {@code null} bila belum pernah diubah sejak dimuat
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna audit. Guard di awal method membuat setter ini diam-diam mengabaikan
	 * nilai {@code null}/blank — tidak menghapus nilai lama, berbeda dari setter lain di kelas
	 * ini yang selalu menimpa.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau blank
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna audit. Guard yang sama seperti {@link #setOlehId(String)} membuat
	 * nilai {@code null}/blank diabaikan, bukan menghapus nilai lama.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau blank
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna audit, atau {@code null} bila belum pernah diubah sejak dimuat
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook siklus hidup JPA yang dipanggil Hibernate tepat sebelum setiap {@code UPDATE}.
	 * Mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}
	 * yang mengisi {@link #tanggal_dirubah}, {@link #oleh}, dan {@link #olehId} dengan waktu
	 * serta identitas pengguna aktif. Dipicu otomatis oleh Hibernate lewat
	 * {@link javax.persistence.PreUpdate}, tidak dipanggil manual di tempat lain.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi waktu perubahan terakhir. Field diinisialisasi ke waktu saat ini pada konstruksi
	 * objek, lalu ditimpa ulang oleh {@link #onUpdate()} setiap kali baris di-{@code UPDATE}.
	 *
	 * @param tanggal_dirubah waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini.
	 *
	 * @return timestamp perubahan terakhir; tidak pernah {@code null} karena field diinisialisasi
	 *         saat objek dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi ringkas untuk log/debug dan tampilan combobox generik: {@code id} diikuti
	 * {@link #nama}.
	 *
	 * @return string berformat {@code "<id>-<nama>"}
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode singkat jenis pemesanan pengadaan. */
	private String kode;

	/** Nama jenis pemesanan pengadaan. */
	private String nama;
	/** Keterangan bebas, opsional. */
	private String keterangan;
	/** Akun buku besar DP (uang muka) pengadaan. */
	private Akun akunDp;
	/** Akun buku besar hutang DP. */
	private Akun akunUtangDp;
	/** Akun buku besar hutang pekerjaan. */
	private Akun akunUtangPekerjaan;
	/** Flag "hutang dari anggaran"; {@code null} ditafsirkan sebagai {@code false} — lihat catatan kelas. */
	private Boolean akunUtangDariAnggaran;
	/** Flag "ada proses penerimaan terpisah"; {@code null} ditafsirkan sebagai {@code true} — lihat catatan kelas. */
	private Boolean adaProsesPenerimaan;
	/** Flag aktif; {@code null} ditafsirkan sebagai aktif oleh {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Konstruktor default tanpa argumen, dipakai Hibernate untuk instansiasi entity via
	 * refleksi dan oleh kode aplikasi saat membuat baris baru sebelum diisi setter.
	 */
	public JenisPemesananPengadaanAsset() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * @return id, atau {@code null} untuk instance baru yang belum disimpan
	 */
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
	 * @param id primary key
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode singkat jenis pemesanan. {@code null} dinormalisasi menjadi string
	 * kosong agar aman dipakai langsung di tampilan tanpa null-check tambahan.
	 *
	 * @return kode ter-trim, tidak pernah {@code null} (string kosong bila belum diisi)
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Mengisi kode singkat jenis pemesanan. Tidak dianotasi {@code @Column} — kolom dipetakan
	 * lewat konvensi nama Hibernate default.
	 *
	 * @param kode kode jenis pemesanan
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama jenis pemesanan, di-trim untuk menghindari whitespace tak sengaja dari
	 * input.
	 *
	 * @return nama ter-trim, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama jenis pemesanan. Tidak melakukan trim di sisi setter — trimming terjadi
	 * hanya saat dibaca lewat {@link #getNama()}.
	 *
	 * @param nama nama jenis pemesanan
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas baris ini.
	 *
	 * @return keterangan, boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas.
	 *
	 * @param keterangan teks keterangan, boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif jenis pemesanan. {@code null} database ditafsirkan sebagai
	 * aktif, sehingga baris lama yang belum pernah disentuh field ini akan selalu tampil aktif.
	 *
	 * @return {@code true} bila aktif atau belum pernah di-set; {@code false} hanya bila
	 *         eksplisit pernah di-set nonaktif
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengisi status aktif. Tidak ada normalisasi di setter — nilai {@code null} yang di-set di
	 * sini tetap tersimpan {@code null} dan akan dibaca sebagai aktif oleh {@link #getAktif()}.
	 *
	 * @param aktif status aktif baru; {@code null} diperbolehkan dan berarti "aktif" saat dibaca
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan akun buku besar DP (uang muka), meresolusi proxy lazy Hibernate lewat
	 * {@link GeneralValueObject#check(Object)} sebelum dikembalikan agar pemanggil tidak
	 * menerima proxy yang bisa meledak di luar sesi Hibernate.
	 *
	 * @return akun DP yang sudah teresolusi, atau {@code null} bila belum dipasangkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun_dp", nullable = true)
	public Akun getAkunDp() {
		akunDp = check(akunDp);
		return akunDp;
	}

	/**
	 * Mengisi akun buku besar DP (uang muka).
	 *
	 * @param akunDp akun DP, boleh {@code null}
	 */
	public void setAkunDp(Akun akunDp) {
		this.akunDp = akunDp;
	}

	/**
	 * Mengembalikan akun buku besar hutang DP, meresolusi proxy lazy Hibernate lewat
	 * {@link GeneralValueObject#check(Object)} sebelum dikembalikan.
	 *
	 * @return akun hutang DP yang sudah teresolusi, atau {@code null} bila belum dipasangkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun_utang_dp", nullable = true)
	public Akun getAkunUtangDp() {
		akunUtangDp = check(akunUtangDp);
		return akunUtangDp;
	}

	/**
	 * Mengisi akun buku besar hutang DP.
	 *
	 * @param akunUtangDp akun hutang DP, boleh {@code null}
	 */
	public void setAkunUtangDp(Akun akunUtangDp) {
		this.akunUtangDp = akunUtangDp;
	}

	/**
	 * Mengembalikan flag apakah hutang pemesanan berasal dari anggaran. {@code null} database
	 * ditafsirkan sebagai <b>{@code false}</b> — arah default yang berlawanan dari kebanyakan
	 * flag {@code aktif} di paket ini. Dibaca berulang kali oleh
	 * {@code ais.action.master.asset.PostingPembayaranAction} untuk mempercabangkan sumber akun
	 * hutang saat posting pembayaran.
	 *
	 * @return {@code true} hanya bila eksplisit pernah di-set demikian; {@code false} bila
	 *         {@code null} atau eksplisit {@code false}
	 */
	public Boolean getAkunUtangDariAnggaran() {
		return akunUtangDariAnggaran == null ? false : akunUtangDariAnggaran;
	}

	/**
	 * Mengisi flag "hutang dari anggaran". Tidak ada normalisasi di setter — nilai {@code null}
	 * yang di-set di sini tetap tersimpan {@code null} dan akan dibaca sebagai {@code false}
	 * oleh {@link #getAkunUtangDariAnggaran()}.
	 *
	 * @param akunUtangDariAnggaran nilai flag baru; {@code null} diperbolehkan dan berarti
	 *                              "bukan dari anggaran" saat dibaca
	 */
	public void setAkunUtangDariAnggaran(Boolean akunUtangDariAnggaran) {
		this.akunUtangDariAnggaran = akunUtangDariAnggaran;
	}

	/**
	 * Mengembalikan flag apakah jenis pemesanan ini membutuhkan tahapan proses penerimaan
	 * barang terpisah. {@code null} database ditafsirkan sebagai {@code true}. Dipakai langsung
	 * sebagai kondisi filter Hibernate Criteria di
	 * {@code ais.action.master.asset.helper.AmbilDataPemesananPengadaanAsetBanbox}, bukan
	 * sekadar label tampilan.
	 *
	 * @return {@code true} bila butuh proses penerimaan atau belum pernah di-set;
	 *         {@code false} hanya bila eksplisit pernah di-set demikian
	 */
	public Boolean getAdaProsesPenerimaan() {
		return adaProsesPenerimaan == null ? true : adaProsesPenerimaan;
	}

	/**
	 * Mengisi flag "ada proses penerimaan terpisah". Tidak ada normalisasi di setter — nilai
	 * {@code null} yang di-set di sini tetap tersimpan {@code null} dan akan dibaca sebagai
	 * {@code true} oleh {@link #getAdaProsesPenerimaan()}.
	 *
	 * @param adaProsesPenerimaan nilai flag baru; {@code null} diperbolehkan dan berarti "butuh
	 *                            proses penerimaan" saat dibaca
	 */
	public void setAdaProsesPenerimaan(Boolean adaProsesPenerimaan) {
		this.adaProsesPenerimaan = adaProsesPenerimaan;
	}

	/**
	 * Mengembalikan akun buku besar hutang pekerjaan, meresolusi proxy lazy Hibernate lewat
	 * {@link GeneralValueObject#check(Object)} sebelum dikembalikan.
	 *
	 * @return akun hutang pekerjaan yang sudah teresolusi, atau {@code null} bila belum
	 *         dipasangkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun_utang_pekerjaan", nullable = true)
	public Akun getAkunUtangPekerjaan() {
		akunUtangPekerjaan = check(akunUtangPekerjaan);
		return akunUtangPekerjaan;
	}

	/**
	 * Mengisi akun buku besar hutang pekerjaan.
	 *
	 * @param akunUtangPekerjaan akun hutang pekerjaan, boleh {@code null}
	 */
	public void setAkunUtangPekerjaan(Akun akunUtangPekerjaan) {
		this.akunUtangPekerjaan = akunUtangPekerjaan;
	}

}
