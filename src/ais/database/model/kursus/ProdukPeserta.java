package ais.database.model.kursus;

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.BarcodeCommon;
import ais.database.model.GeneralValueObject;

/**
 * Detail <b>1:1 pembelian produk kursus</b>: setiap baris {@code ProdukPeserta} melekat pada tepat
 * satu {@link PesertaPunyaProdukKursus} (kolom join {@code peserta_punya_produk_kursus} bertanda
 * {@code nullable = false, unique = true} — kombinasi yang menegakkan relasi satu-ke-satu, bukan
 * satu-ke-banyak biasa) dan menyimpan turunan daftar komponen harga produk yang dibeli.
 *
 * <p><b>Beda dengan dua entitas kursus lain yang mirip namanya.</b> Lihat javadoc kelas
 * {@link PesertaInginProdukKursus} untuk perbandingan lengkap ketiga entitas peserta&ndash;produk.
 * Ringkasnya: {@link PesertaPunyaProdukKursus} adalah header pembelian yang memegang
 * {@code status}/{@code hargaDibayar} (gerbang pembayaran sesungguhnya), sedangkan kelas ini
 * ({@code ProdukPeserta}) adalah detail pendampingnya yang isinya murni turunan/cache, bukan data
 * transaksi independen. FK {@code pesertaKursus} dan {@code produkKursus} di kelas ini
 * <b>redundan</b> dengan FK bernama sama di {@link PesertaPunyaProdukKursus} yang ditunjuk
 * {@link #getPesertaPunyaProdukKursus()} — keduanya harus konsisten secara manual karena tidak ada
 * validasi yang menjamin nilainya sama dengan induknya.</p>
 *
 * <p><b>{@link #getKomponens()} adalah getter destruktif yang menghitung ulang, bukan accessor
 * pasif.</b> Setiap kali dipanggil, field {@code komponens} ditimpa (mulai dari string kosong)
 * lalu dibangun ulang seluruhnya dengan mem-parse JSON {@link ProdukKursus#getHargaKomponens()}
 * milik produk terkait — nilai apa pun yang pernah ditetapkan lewat {@link #setKomponens(String)}
 * akan lenyap pada pemanggilan {@link #getKomponens()} berikutnya selama {@link #getProdukKursus()}
 * tidak {@code null}. Hasilnya adalah string gabungan id {@code komponenDataProdukKursus} yang
 * dipisah koma (format {@code ",id1,id2,..."}), dipakai untuk pencarian/pencocokan komponen data
 * produk yang berlaku bagi pembelian ini pada saat query dijalankan — bukan snapshot harga/komponen
 * pada waktu pembelian terjadi, karena selalu dihitung ulang dari definisi {@link ProdukKursus}
 * <i>saat ini</i>, bukan pada saat {@link PesertaPunyaProdukKursus#getWaktuBeli()}. Bila
 * {@link ProdukKursus#getHargaKomponens()} diubah administrator setelah pembelian, hasil
 * {@link #getKomponens()} untuk pembelian lama ikut berubah — bukan cacat baru yang memerlukan
 * perbaikan tersendiri, namun konsekuensi arsitektur yang perlu diketahui bila kelak fitur ini
 * dipakai untuk audit/laporan historis.</p>
 *
 * <p><b>Pemetaan.</b> Skema {@code public}, tabel {@code produk_peserta}, beranotasi
 * {@code @Audited} (Envers) dengan {@code dynamicInsert}/{@code dynamicUpdate}. Kolom {@code kode}
 * unik dibangkitkan otomatis via {@link BarcodeCommon#generateCode()} bila belum diisi.
 *
 * @see PesertaPunyaProdukKursus
 * @see PesertaInginProdukKursus
 * @see ProdukKursus#getHargaKomponens()
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "produk_peserta")
public class ProdukPeserta extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java, bernilai sama dengan entitas lain sepaket karena kerangka
	 * kelasnya disalin dari sumber yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama sekuensial dari basis data. Lihat {@link #getId()}. */
	private Long id;
	/**
	 * Blok audit bayangan yang dipadatkan ke satu baris — pola penyisipan otomatis yang dipakai
	 * di seluruh basis kode AIS agar dapat ditempelkan ke entitas lama tanpa mengubah struktur
	 * berkas. Isinya: field {@code oleh} (nama tampil pengubah terakhir, lihat
	 * {@link #getOleh()}), field {@code olehId} beserta getter-nya (identitas pengubah terakhir),
	 * dan setter {@code setOlehId} yang berpenjaga satu arah — argumen {@code null} atau berisi
	 * spasi saja diabaikan sehingga jejak audit yang sudah terisi tidak dapat dikosongkan kembali
	 * lewat setter.
	 *
	 * <p>Pengulangan blok ini di hampir setiap entitas adalah keharusan teknis, bukan cacat:
	 * {@link ais.database.model.GeneralValueObject} merupakan POJO abstrak biasa dan bukan
	 * {@code @MappedSuperclass}, sehingga properti yang dideklarasikan di sana tidak ikut dipetakan
	 * Hibernate ke kolom tabel turunannya.
	 */
	private String oleh;
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna pengubah terakhir baris detail ini.
	 *
	 * @return identitas (id) pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan identitas pengguna pengubah terakhir dengan penjaga satu arah: argumen
	 * {@code null} atau berisi spasi saja diabaikan.
	 *
	 * @param olehId identitas pengguna pengubah; {@code null}/kosong diabaikan diam-diam
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama tampil pengguna pengubah terakhir dengan penjaga satu arah yang sama seperti
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama tampil pengguna pengubah; {@code null}/kosong diabaikan diam-diam
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama tampil pengguna terakhir yang menyimpan baris detail ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA sebelum setiap {@code UPDATE}; mendelegasikan pencatatan stempel waktu
	 * dan identitas pengubah ke {@code AuditTimestampInterceptor.ubah(this)}.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Stempel waktu perubahan terakhir; diisi awal saat objek dibuat, ditulis ulang oleh {@link #onUpdate()} sebelum setiap {@code UPDATE}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir; biasanya sudah diurus {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah waktu perubahan yang ingin dicatat
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return waktu perubahan terakhir; untuk objek baru berisi waktu objek dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Mengembalikan representasi teks baris detail ini, gabungan {@code kode} dan
	 * {@code keterangan}.
	 *
	 * @return string "{@code kode} - {@code keterangan}"
	 */
	public String toString() {
		return kode + " - " + keterangan;
	}

	/** Kode unik baris detail, dibangkitkan otomatis bila belum diisi. Lihat {@link #getKode()}. */
	private String kode;
	/** Keterangan bebas. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Peserta pembeli, redundan dengan FK bernama sama di {@link PesertaPunyaProdukKursus} induk. Lihat {@link #getPesertaKursus()}. */
	private PesertaKursus pesertaKursus;
	/** Produk kursus yang dibeli, redundan dengan FK bernama sama di {@link PesertaPunyaProdukKursus} induk. Lihat {@link #getProdukKursus()}. */
	private ProdukKursus produkKursus;
	/** Header pembelian induk (relasi 1:1). Lihat {@link #getPesertaPunyaProdukKursus()}. */
	private PesertaPunyaProdukKursus pesertaPunyaProdukKursus;
	/** Daftar id komponen data produk, dihitung ulang tiap akses oleh {@link #getKomponens()}. */
	private String komponens;

	/** Konstruktor tanpa argumen yang diwajibkan Hibernate. */
	public ProdukPeserta() {
	}

	/**
	 * Mengembalikan kunci utama baris detail. Kolomnya {@code insertable = false} karena nilainya
	 * dibangkitkan basis data.
	 *
	 * @return id baris, atau {@code null} bila objek belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama; dipakai Hibernate dan proses impor.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan keterangan bebas baris detail ini, apa adanya.
	 *
	 * @return keterangan, boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas baris detail ini.
	 *
	 * @param keterangan catatan bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan kode unik baris detail ini, membangkitkan kode baru lewat
	 * {@link BarcodeCommon#generateCode()} pada akses pertama bila field masih {@code null} —
	 * getter ini punya efek samping menulis field {@code kode} (konsisten dengan
	 * {@code dynamicUpdate} entitas ini).
	 *
	 * @return kode unik baris, tidak pernah {@code null} setelah dipanggil sekali
	 */
	@Column(unique = true)
	public String getKode() {
		if (kode == null) {
			kode = BarcodeCommon.generateCode();
		}
		return kode;
	}

	/**
	 * Menetapkan kode unik baris detail ini apa adanya.
	 *
	 * @param kode kode unik; boleh {@code null} untuk memicu pembangkitan otomatis pada
	 *             {@link #getKode()} berikutnya
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan peserta pembeli, meresolusi proxy lazy lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} bila perlu sebelum
	 * dikembalikan. FK ini redundan dengan {@link PesertaPunyaProdukKursus#getPesertaKursus()}
	 * milik induk yang ditunjuk {@link #getPesertaPunyaProdukKursus()} — tidak ada validasi yang
	 * menjamin keduanya konsisten.
	 *
	 * @return peserta pembeli, atau {@code null} bila belum ditautkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "peserta_kursus", nullable = true)
	public PesertaKursus getPesertaKursus() {
		pesertaKursus = check(pesertaKursus);
		return pesertaKursus;
	}

	/**
	 * Menetapkan peserta pembeli.
	 *
	 * @param pesertaKursus peserta pembeli
	 */
	public void setPesertaKursus(PesertaKursus pesertaKursus) {
		this.pesertaKursus = pesertaKursus;
	}

	/**
	 * Mengembalikan produk kursus yang dibeli, meresolusi proxy lazy lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} bila perlu sebelum
	 * dikembalikan. Dipakai sebagai sumber data komponen harga oleh {@link #getKomponens()}. FK
	 * ini redundan dengan {@link PesertaPunyaProdukKursus#getProdukKursus()} milik induk.
	 *
	 * @return produk kursus yang dibeli, atau {@code null} bila belum ditautkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk_kursus", nullable = true)
	public ProdukKursus getProdukKursus() {
		produkKursus = check(produkKursus);
		return produkKursus;
	}

	/**
	 * Menetapkan produk kursus yang dibeli.
	 *
	 * @param produkKursus produk kursus yang dibeli
	 */
	public void setProdukKursus(ProdukKursus produkKursus) {
		this.produkKursus = produkKursus;
	}

	/**
	 * Mengembalikan header pembelian induk. Kolom join-nya beranotasi
	 * {@code nullable = false, unique = true} — kombinasi yang menegakkan relasi satu-ke-satu di
	 * level basis data: satu {@link PesertaPunyaProdukKursus} hanya boleh punya tepat satu baris
	 * {@code ProdukPeserta} ini. Berbeda dari kebanyakan relasi {@code @ManyToOne} lain di modul
	 * ini, relasi ini memakai {@code @Fetch(FetchMode.SELECT)} (EAGER, query {@code SELECT}
	 * terpisah) dan getter-nya tidak memanggil {@code check(...)}.
	 *
	 * @return header pembelian yang memiliki detail ini, tidak boleh {@code null} pada baris yang
	 *         sudah tersimpan (kolom {@code nullable = false})
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "peserta_punya_produk_kursus", nullable = false, unique = true)
	public PesertaPunyaProdukKursus getPesertaPunyaProdukKursus() {
		return pesertaPunyaProdukKursus;
	}

	/**
	 * Menetapkan header pembelian induk.
	 *
	 * @param pesertaPunyaProdukKursus header pembelian pemilik detail ini
	 */
	public void setPesertaPunyaProdukKursus(PesertaPunyaProdukKursus pesertaPunyaProdukKursus) {
		this.pesertaPunyaProdukKursus = pesertaPunyaProdukKursus;
	}

	/**
	 * Mengembalikan daftar id {@code komponenDataProdukKursus} yang berlaku bagi produk yang
	 * dibeli, dipisah koma dengan koma pembuka/penutup (format {@code ",id1,id2,..."}).
	 *
	 * <p><b>Getter destruktif yang menghitung ulang setiap kali dipanggil</b> — lihat javadoc
	 * kelas untuk penjelasan lengkap. Ringkasnya: field {@code komponens} selalu ditimpa (dimulai
	 * dari string kosong) lalu dibangun ulang dari JSON {@link ProdukKursus#getHargaKomponens()}
	 * milik {@link #getProdukKursus()} <i>saat ini</i> — bukan snapshot pada saat pembelian.
	 * Struktur JSON sumbernya dua tingkat: array luar objek {@code {key, arrayDetail}} per
	 * komponen produk, dan {@code arrayDetail} berisi objek {@code {key, komponenDataProdukKursus}}
	 * per komponen data; hanya entri dengan {@code key} terisi (non-null) di kedua tingkat yang
	 * disertakan — entri yang "dihapus" administrator (diganti objek JSON kosong tanpa
	 * {@code key}, lihat {@code KursusUtil}) dilewati secara otomatis. Kegagalan parse JSON
	 * (mis. data lama tidak valid) ditelan lewat {@code catch(Exception)} dan dicatat ke
	 * {@code ErrorAuditUtil}, membuat method mengembalikan hasil sebagian atau string kosong
	 * tanpa melempar exception ke pemanggil.
	 *
	 * @return string id komponen data produk dipisah koma, atau string kosong bila
	 *         {@link #getProdukKursus()} {@code null} atau gagal di-parse
	 */
	@Column(columnDefinition = "text")
	public String getKomponens() {
		komponens = "";
		if (getProdukKursus() != null) {
			try {
				JSONArray array = new JSONArray(produkKursus.getHargaKomponens());
				for (int i = 0; i < array.length(); i++) {
					JSONObject jsonObject = array.getJSONObject(i);
					Long key = null;
					if (!jsonObject.isNull("key")) {
						key = ais.common.CommonJSONUtil.ambilLong(jsonObject, "key");
					}

					if (key != null) {

						JSONArray arrayDetail = jsonObject.isNull("arrayDetail") ? new JSONArray()
								: jsonObject.getJSONArray("arrayDetail");

						for (int ii = 0; ii < arrayDetail.length(); ii++) {

							JSONObject jsonObjecti = arrayDetail.getJSONObject(ii);
							Long keyi = null;
							if (!jsonObjecti.isNull("key")) {
								keyi = ais.common.CommonJSONUtil.ambilLong(jsonObjecti, "key");
							}

							if (keyi != null) {
								Long kodeId = ais.common.CommonJSONUtil.ambilLong(jsonObjecti,
										"komponenDataProdukKursus");
								if (kodeId != null) {
									komponens += "," + kodeId + ",";
								}
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/kursus/ProdukPeserta.java:201");
			}
		}
		return komponens;
	}

	/**
	 * Menetapkan daftar id komponen data produk secara manual; akan ditimpa oleh
	 * {@link #getKomponens()} pada pemanggilan berikutnya selama {@link #getProdukKursus()} bukan
	 * {@code null}.
	 *
	 * @param komponens string id komponen data produk yang ingin ditetapkan sementara
	 */
	public void setKomponens(String komponens) {
		this.komponens = komponens;
	}

}
