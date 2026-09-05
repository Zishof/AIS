package ais.database.model.sirs;

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
 * Master <b>jenis biaya</b>: penggolong komponen pembentuk tarif medis — jasa dokter, jasa sarana,
 * bahan habis pakai, administrasi, dan seterusnya. Setiap baris {@link Biaya} pada rincian tarif
 * membawa salah satu jenis biaya ini, dan dari sinilah baris tersebut mewarisi {@link Akun} buku
 * besarnya. Dengan begitu satu tarif tindakan dapat langsung terpecah ke beberapa akun pendapatan
 * yang berbeda tanpa perlu pemetaan tambahan di lapisan akuntansi.
 *
 * <h3>Empat tipe dan cara pemakaiannya</h3>
 * {@link #getTipe()} menggolongkan jenis biaya menurut objek apa yang ditarifkan, memakai salah
 * satu dari empat konstanta di kelas ini: {@link #TIPE_ITEM}, {@link #TIPE_TINDAKAAN},
 * {@link #TIPE_ALAT_MEDIS}, dan {@link #TIPE_PAKET}. Tipe ini dipakai
 * {@code ais.action.master.sirs.util.CommonTarifTindakan#getJenisBiayas} untuk menyaring komponen
 * biaya mana yang relevan bagi sebuah tindakan atau paket layanan.
 * <p>
 * Nilai tipe disimpan sebagai <b>string bebas</b>, bukan enum maupun kolom berkendala. Pada layar
 * {@code ais.action.master.sirs.JenisBiayaAction} pilihannya memang dibatasi combobox berisi
 * keempat konstanta itu, tetapi tidak ada apa pun di lapisan model atau basis data yang mencegah
 * nilai lain tersimpan lewat jalur impor, penyemaian awal, atau perubahan data langsung. Karena
 * penyaringnya memakai pencocokan string persis ({@code eq("jenisBiaya.tipe", ...)}), satu huruf
 * yang berbeda membuat komponen biaya tersebut diam-diam hilang dari rincian tarif — tanpa pesan
 * kesalahan. Perlu dicatat pula bahwa nama konstanta {@link #TIPE_TINDAKAAN} mengandung salah ketik
 * (huruf "A" ganda); nilainya sendiri, {@code "Tindakan/Layanan"}, sudah benar dan tidak boleh
 * diubah karena itulah yang tersimpan di basis data.
 * </p>
 *
 * <h3>Dua bendera dengan peran berbeda</h3>
 * <ul>
 * <li>{@link #getAktif()} — menentukan apakah jenis biaya masih boleh dipakai. Disaring ketat
 * dengan {@code eq("jenisBiaya.aktif", true)} baik saat menyusun daftar komponen tarif maupun saat
 * menjumlahkan nilai tarif di {@code CommonPendaftaranUtil}. Mematikannya membuat komponen biaya
 * itu <b>berhenti ikut dijumlahkan</b> pada perhitungan tarif berikutnya — jadi bendera ini bukan
 * sekadar penyembunyi dari daftar pilihan, melainkan benar-benar mengubah angka tarif. Perhatikan
 * bahwa penyaring ketat itu tidak sejalan dengan {@link #getAktif()} yang menormalkan {@code null}
 * menjadi {@code true}: baris dengan kolom {@code aktif} bernilai {@code NULL} tidak akan
 * terhitung, berlawanan dengan maksud getternya.</li>
 * <li>{@link #getDefaultAktif()} — menandai jenis biaya sebagai bagian dari <b>himpunan cadangan</b>.
 * Bila sebuah tindakan sama sekali belum memiliki komponen biaya yang dikonfigurasi,
 * {@code CommonTarifTindakan#getJenisBiayas} tidak mengembalikan daftar kosong melainkan jatuh ke
 * seluruh jenis biaya bertipe sesuai yang ditandai {@code defaultAktif}. Bendera ini karena itu
 * menentukan susunan komponen apa yang otomatis muncul bagi tindakan baru.</li>
 * </ul>
 * Keduanya bendera dua-arah yang benar, dengan getter yang menormalkan {@code null} menjadi
 * {@code true}.
 *
 * <h3>Kedudukan dalam urutan penetapan harga</h3>
 * Jenis biaya bekerja pada lapisan ketiga: setelah tarif khusus dipilih (lapisan pertama) dan baris
 * tarif per kelas perawatan diambil (lapisan kedua), rincian komponen menurut jenis biaya inilah
 * yang dijumlahkan menjadi angka tarif. Lapisan keempat — {@link Diskon} dan {@link PajakMedis} —
 * juga dicatat sebagai baris {@link Biaya}, tetapi baris-baris itu <b>tidak</b> memakai jenis
 * biaya: mereka mengisi kolom {@code diskon}/{@code pajak} sebagai gantinya, dan mewarisi akun dari
 * sana.
 *
 * <h3>Pola arsitektur</h3>
 * <ul>
 * <li><b>Getter destruktif</b> — {@link #getAktif()}, {@link #getDefaultAktif()},
 * {@link #getVariable()}, dan {@link #getAkun()} menulis balik ke field.</li>
 * <li><b>Field audit bayangan</b> — keharusan teknis infrastruktur audit.</li>
 * <li><b>Tanpa sumbu tenant</b> — katalog jenis biaya bersifat global lintas unit.</li>
 * <li>{@code tipe}, {@code aktif}, dan {@code defaultAktif} tidak diberi {@code @Column}, sehingga
 * dipetakan ke kolom bernama sesuai nama properti; {@code variable} justru dipetakan ke kolom
 * bernama {@code _variable} dengan awalan garis bawah, kemungkinan untuk menghindari kata yang
 * dicadangkan basis data.</li>
 * <li>{@link #getNama()} wajib diisi tetapi tanpa indeks unik dan tanpa penjaga tabrakan, sehingga
 * dua jenis biaya bernama sama akan tampil identik pada daftar pilihan dan rincian tarif.</li>
 * </ul>
 *
 * @see Biaya baris rincian yang membawa jenis biaya ini dan mewarisi akunnya
 * @see JenisBiayaLain penggolong biaya di luar tarif medis (kas, deposit, penjualan)
 * @see BiayaTindakanPerKelas tarif per kelas yang rinciannya digolongkan menurut jenis biaya
 * @see ais.action.master.sirs.util.CommonTarifTindakan#getJenisBiayas penyaring dan mekanisme fallback
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "jenis_biaya")
public class JenisBiaya extends GeneralValueObject {

	/** Nilai {@link #getTipe()} untuk komponen biaya atas item medis (obat/barang). */
	public static final String TIPE_ITEM = "Item";

	/**
	 * Nilai {@link #getTipe()} untuk komponen biaya atas tindakan/layanan medis. Nama konstanta
	 * mengandung salah ketik ("TINDAKAAN" dengan huruf A ganda) yang dipertahankan demi kompatibilitas
	 * kode pemanggil; nilainya sendiri, {@code "Tindakan/Layanan"}, sudah benar dan tersimpan apa
	 * adanya di basis data sehingga tidak boleh diubah.
	 */
	public static final String TIPE_TINDAKAAN = "Tindakan/Layanan";

	/** Nilai {@link #getTipe()} untuk komponen biaya atas alat medis/alat kesehatan. */
	public static final String TIPE_ALAT_MEDIS = "Alat Medis/Alat Kesehatan";

	/** Nilai {@link #getTipe()} untuk komponen biaya atas paket layanan. */
	public static final String TIPE_PAKET = "Paket";

	/**
	 * Versi serialisasi Java, seragam untuk seluruh entitas modul {@code sirs} karena berasal dari
	 * templat pembangkit yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama tabel {@code sirs.jenis_biaya}, dibangkitkan basis data (IDENTITY). */
	private Long id;

	/** Identitas pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna yang terakhir mengubah jenis biaya ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi identitas pengguna pengubah terakhir; nilai kosong/spasi diabaikan agar jejak audit
	 * tidak terhapus oleh form yang mengirim isian kosong.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/** Nama pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String oleh;

	/**
	 * Representasi teks jenis biaya untuk komponen ZK, memakai field {@link #nama} langsung. Karena
	 * nama tidak dijaga keunikannya, dua baris berbeda dapat menghasilkan label identik.
	 *
	 * @return nama jenis biaya; dapat {@code null} bila belum diisi
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir; nilai kosong/spasi diabaikan.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah jenis biaya ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate} yang mendelegasikan pencatatan audit ke
	 * {@code AuditTimestampInterceptor.ubah(this)} setiap kali baris ini diperbarui.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = new Date();

	/**
	 * Mengisi cap waktu perubahan terakhir; normalnya diisi otomatis oleh interceptor audit.
	 *
	 * @param tanggal_dirubah cap waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir jenis biaya ini.
	 *
	 * @return cap waktu perubahan terakhir (diinisialisasi ke waktu pembuatan objek)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nama jenis biaya, wajib diisi; dipakai sebagai label tampil pada rincian tarif. */
	private String nama;

	/** Penggolong tipe objek yang ditarifkan; salah satu dari keempat konstanta {@code TIPE_*}. */
	private String tipe;

	/** Penanda/label tambahan bebas yang mendampingi nama pada beberapa layar. */
	private String variable;

	/** Keterangan bebas atas jenis biaya. */
	private String keterangan;

	/** Status boleh-dipakai; ikut menentukan apakah komponen dijumlahkan ke dalam tarif. */
	private Boolean aktif;

	/** Penanda keanggotaan himpunan cadangan bagi tindakan yang belum punya komponen biaya. */
	private Boolean defaultAktif;

	/** Akun buku besar yang diwariskan ke baris {@link Biaya}; kolomnya {@code NOT NULL}. */
	private Akun akun;

	/** Konstruktor kosong yang diwajibkan JPA/Hibernate. */
	public JenisBiaya() {
	}

	/**
	 * Mengembalikan kunci utama jenis biaya.
	 *
	 * @return id baris, atau {@code null} bila belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama; umumnya hanya untuk kerangka kerja persistensi atau saat menyalin
	 * entitas menjadi baris baru.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama jenis biaya.
	 *
	 * @return nama jenis biaya (kolom wajib, maksimal 255 karakter)
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Mengisi nama jenis biaya. Tidak ada penjaga tabrakan nama maupun indeks unik pada kolomnya.
	 *
	 * @param nama nama jenis biaya
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas jenis biaya.
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas jenis biaya.
	 *
	 * @param keterangan catatan bebas
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status boleh-dipakai jenis biaya, menormalkan {@code null} menjadi {@code true}.
	 * <b>Getter destruktif</b>.
	 *
	 * <p>
	 * Bendera ini bukan sekadar penyembunyi dari daftar pilihan. Penyusun daftar komponen tarif
	 * ({@code CommonTarifTindakan#getJenisBiayas}) maupun penjumlah nilai tarif
	 * ({@code CommonPendaftaranUtil}) sama-sama menyaring dengan {@code eq("jenisBiaya.aktif", true)},
	 * sehingga mematikan sebuah jenis biaya membuat seluruh komponen bertipe itu <b>berhenti ikut
	 * dijumlahkan</b> — angka tarif berubah pada perhitungan berikutnya, sementara baris
	 * {@link Biaya} rinciannya tetap tersimpan di basis data. Perubahan bendera ini karena itu perlu
	 * diperlakukan sebagai perubahan tarif, bukan sekadar perapian katalog.
	 * </p>
	 *
	 * <p>
	 * Perhatikan pula bahwa penyaring ketat itu tidak sejalan dengan maksud nilai cadangan di sini:
	 * baris yang kolom {@code aktif}-nya {@code NULL} di basis data dianggap aktif oleh getter,
	 * tetapi tidak akan terpilih oleh {@code eq(aktif, true)}. Ketidakseragaman yang sama muncul
	 * pada {@link Diskon#getAktif()} dan {@link PajakMedis#getAktif()}.
	 * </p>
	 *
	 * @return {@code true} bila jenis biaya boleh dipakai, tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menyalakan atau mematikan jenis biaya. Mematikannya mengubah angka tarif pada perhitungan
	 * berikutnya — lihat {@link #getAktif()}.
	 *
	 * @param aktif {@code true} bila jenis biaya boleh dipakai
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan penanda keanggotaan himpunan cadangan, menormalkan {@code null} menjadi
	 * {@code true}. <b>Getter destruktif</b>.
	 *
	 * <p>
	 * Bendera ini menjawab pertanyaan "komponen biaya apa saja yang muncul untuk tindakan yang belum
	 * dikonfigurasi sama sekali". {@code CommonTarifTindakan#getJenisBiayas} mula-mula mencari
	 * komponen biaya yang memang sudah ditetapkan bagi tindakan bersangkutan; bila hasilnya kosong,
	 * ia jatuh ke seluruh jenis biaya bertipe sesuai yang ditandai {@code defaultAktif}. Karena
	 * nilai cadangan getter ini {@code true}, jenis biaya baru secara otomatis menjadi anggota
	 * himpunan cadangan kecuali operator sengaja mematikannya — sehingga menambah satu jenis biaya
	 * baru ke katalog akan langsung memengaruhi susunan komponen bagi seluruh tindakan yang belum
	 * dikonfigurasi.
	 * </p>
	 *
	 * @return {@code true} bila jenis biaya termasuk himpunan cadangan, tidak pernah {@code null}
	 */
	public Boolean getDefaultAktif() {
		if (defaultAktif == null) {
			defaultAktif = true;
		}
		return defaultAktif;
	}

	/**
	 * Menandai atau membatalkan keanggotaan jenis biaya dalam himpunan cadangan.
	 *
	 * @param defaultAktif {@code true} bila jenis biaya ikut muncul bagi tindakan yang belum dikonfigurasi
	 */
	public void setDefaultAktif(Boolean defaultAktif) {
		this.defaultAktif = defaultAktif;
	}

	/**
	 * Mengembalikan akun buku besar jenis biaya ini. <b>Getter destruktif</b> ({@code check(...)}).
	 * Kolomnya {@code NOT NULL}. Akun inilah sumber warisan pertama pada {@link Biaya#getAkun()},
	 * yakni jalur yang dipakai baris rincian tarif untuk memperoleh akun tanpa operator perlu
	 * menetapkannya per baris.
	 *
	 * @return akun buku besar, atau {@code null} bila belum ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = false)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/**
	 * Menetapkan akun buku besar jenis biaya ini. Perlu diingat bahwa baris {@link Biaya} yang sudah
	 * pernah dibaca akan menyimpan akun warisannya secara permanen, sehingga perubahan di sini tidak
	 * merambat surut ke rincian tarif yang sudah terbentuk.
	 *
	 * @param akun akun buku besar
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Mengembalikan penggolong tipe objek yang ditarifkan. Getter murni-baca tanpa normalisasi,
	 * sehingga dapat mengembalikan {@code null}.
	 *
	 * <p>
	 * Nilainya semestinya salah satu dari {@link #TIPE_ITEM}, {@link #TIPE_TINDAKAAN},
	 * {@link #TIPE_ALAT_MEDIS}, atau {@link #TIPE_PAKET}, tetapi kolomnya adalah string bebas tanpa
	 * kendala apa pun. Karena penyaring memakai pencocokan string persis, nilai yang menyimpang —
	 * termasuk {@code null}, spasi berlebih, atau beda huruf besar-kecil — membuat jenis biaya itu
	 * tidak pernah terpilih pada rincian tarif mana pun, tanpa pesan kesalahan. Layar
	 * {@code JenisBiayaAction} membatasi pilihan lewat combobox, sehingga penyimpangan hanya mungkin
	 * berasal dari impor, penyemaian awal, atau perubahan data langsung.
	 * </p>
	 *
	 * @return tipe jenis biaya, dapat {@code null}
	 */
	public String getTipe() {
		return tipe;
	}

	/**
	 * Mengisi penggolong tipe objek yang ditarifkan. Pakailah salah satu konstanta {@code TIPE_*};
	 * nilai lain akan tersimpan tanpa penolakan tetapi membuat jenis biaya ini tidak pernah
	 * terpilih.
	 *
	 * @param tipe salah satu dari {@link #TIPE_ITEM}, {@link #TIPE_TINDAKAAN},
	 *             {@link #TIPE_ALAT_MEDIS}, atau {@link #TIPE_PAKET}
	 */
	public void setTipe(String tipe) {
		this.tipe = tipe;
	}

	/**
	 * Mengembalikan penanda/label tambahan bebas, menormalkan {@code null} menjadi string kosong.
	 * <b>Getter destruktif</b>: normalisasi ditulis balik ke field, sehingga baris yang kolomnya
	 * {@code NULL} berubah menjadi string kosong hanya karena dibaca.
	 *
	 * <p>
	 * Isinya teks bebas yang diketik operator pada layar {@code JenisBiayaAction} dan dipakai
	 * sebagai pelengkap label — misalnya {@code ais.action.master.sirs.DokterAction} menampilkan
	 * daftar jenis biaya sebagai gabungan nama dan nilai ini saat memilih komponen biaya yang
	 * menjadi hak seorang dokter. Tidak ada makna semantik yang ditegakkan kode: nilai ini tidak
	 * diuraikan, tidak dicocokkan, dan tidak dipakai sebagai kunci. Perhatikan bahwa kolomnya
	 * bernama {@code _variable} dengan awalan garis bawah, kemungkinan untuk menghindari kata yang
	 * dicadangkan basis data.
	 * </p>
	 *
	 * @return penanda tambahan, tidak pernah {@code null}
	 */
	@Column(name = "_variable", length = 255)
	public String getVariable() {
		if (variable == null) {
			variable = "";
		}
		return variable;
	}

	/**
	 * Mengisi penanda/label tambahan bebas.
	 *
	 * @param variable teks penanda tambahan
	 */
	public void setVariable(String variable) {
		this.variable = variable;
	}

}
