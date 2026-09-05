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

/**
 * Baris penghubung antara satu paket {@link TarifKhusus} dan satu {@link AlatMedis}: menyatakan
 * bahwa alat medis/alat kesehatan tersebut ditarifkan khusus dalam paket itu. Seperti kembarannya
 * {@link TarifKhususPunyaTindakan}, baris ini menjadi <b>kunci pencarian</b> tarif per kelas
 * perawatan versi khusus, karena {@link BiayaAlatMedisPerKelas} merujuknya lewat
 * {@link BiayaAlatMedisPerKelas#getTarifKhususPunyaAlatMedis()}.
 *
 * <h3>Kedudukan dalam resolusi tarif</h3>
 * {@code ais.action.master.sirs.util.CommonTarifAlatMedis} memanggil {@code CommonTarif.getTarif}
 * dengan kriteria {@code eq("alatMedis", alatMedis)} untuk mencari baris kelas ini yang paket
 * induknya berlaku hari ini dan cocok dengan konteks dokter/asuransi/komunitas/pasien. Bila
 * ditemukan, baris tarif per kelas yang diambil adalah yang {@code tarifKhususPunyaAlatMedis}-nya
 * menunjuk baris ini <i>dan</i> {@code alatMedis}-nya {@code NULL}; bila tidak, yang diambil adalah
 * baris tarif standar dengan pola kolom sebaliknya. Identitas alat medis pada baris tarif khusus
 * karena itu hanya tersimpan di sini.
 *
 * <h3>PERINGATAN — bendera {@code aktif} adalah bendera tidur</h3>
 * {@link #getAktif()} lengkap dengan setter dan normalisasi {@code null} menjadi {@code true},
 * sehingga tampak seperti saklar untuk menonaktifkan satu alat medis dari sebuah paket tarif
 * khusus. Kenyataannya <b>tidak ada satu pun kode di seluruh aplikasi yang membacanya</b>: mesin
 * resolusi hanya memeriksa {@code aktif} pada paket induk ({@link TarifKhusus#getAktif()}), tidak
 * pada baris penghubung. Menyimpan {@code aktif = false} di sini tidak berpengaruh apa pun. Satu-
 * satunya cara menghentikan tarif khusus bagi satu alat adalah menghapus baris penghubung ini, atau
 * menonaktifkan keseluruhan paket. Bendera tidur yang sama ada pada
 * {@link TarifKhususPunyaTindakan#getAktif()} dan {@link TarifKhususPunyaItem#getAktif()}.
 *
 * <h3>Nilai awal {@code semuahargasama} mematikan pewarisan dari master</h3>
 * {@link #getSemuahargasama()} bermaksud mewarisi nilai dari {@link AlatMedis#getSemuahargasama()}
 * untuk keadaan {@code null}, tetapi field {@link #semuahargasama} sudah diinisialisasi
 * {@code true} pada deklarasinya sehingga cabang pewarisan itu <b>kode mati</b> bagi setiap objek
 * baru. Dampaknya ringan karena saklar ini hanya kemudahan penyuntingan layar, bukan penentu harga.
 *
 * <h3>Pola arsitektur</h3>
 * <ul>
 * <li><b>Getter destruktif</b> — {@link #getTarifKhusus()}, {@link #getAlatMedis()},
 * {@link #getAktif()}, dan {@link #getSemuahargasama()} menulis balik ke field.</li>
 * <li><b>Field audit bayangan</b> — keharusan teknis infrastruktur audit. Kelas ini tidak memiliki
 * {@code toString()}, sehingga komponen ZK memakai representasi bawaan {@code Object} kecuali layar
 * merakit labelnya sendiri.</li>
 * <li><b>Tanpa sumbu tenant</b> — sesuai keterbatasan seluruh modul {@code sirs}.</li>
 * <li>Kedua FK ({@code tarif_khusus} dan {@code alat_medis}) bersifat {@code NOT NULL} sehingga
 * baris yatim tidak mungkin tersimpan, tetapi tanpa indeks unik atas pasangannya — satu alat medis
 * dapat terdaftar berulang dalam paket yang sama, dan duplikat yang kalah pada
 * {@code setMaxResults(1)} menjadi baris siluman yang tetap muncul di layar rincian paket.</li>
 * </ul>
 *
 * @see TarifKhusus paket tarif khusus induk yang membawa dimensi cakupan dan masa berlaku
 * @see BiayaAlatMedisPerKelas baris tarif per kelas versi khusus yang merujuk baris ini
 * @see TarifKhususPunyaTindakan kembaran entitas ini untuk tindakan/layanan
 * @see TarifKhususPunyaItem kembaran entitas ini untuk item medis
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "tarif_khusus_punya_alat_medis")
public class TarifKhususPunyaAlatMedis extends GeneralValueObject {

	/**
	 * Versi serialisasi Java, seragam untuk seluruh entitas modul {@code sirs} karena berasal dari
	 * templat pembangkit yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama tabel {@code sirs.tarif_khusus_punya_alat_medis}, dibangkitkan basis data (IDENTITY). */
	private Long id;

	/** Identitas pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna yang terakhir mengubah baris penghubung ini.
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
	 * Mengisi nama pengguna pengubah terakhir; nilai kosong/spasi diabaikan.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris penghubung ini.
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
	 * Mengembalikan cap waktu perubahan terakhir baris penghubung ini. Baris ini tidak memiliki
	 * masa berlaku sendiri; masa berlaku diatur pada paket {@link TarifKhusus} induknya.
	 *
	 * @return cap waktu perubahan terakhir (diinisialisasi ke waktu pembuatan objek)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Paket tarif khusus induk; kolomnya {@code NOT NULL}. */
	private TarifKhusus tarifKhusus;

	/** Alat medis yang ditarifkan khusus dalam paket ini; kolomnya {@code NOT NULL}. */
	private AlatMedis alatMedis;

	/** Kemudahan penyuntingan layar: harga sama untuk seluruh kelas perawatan. */
	private Boolean semuahargasama = true;

	/** Bendera tidur — tidak pernah dibaca kode mana pun. Lihat peringatan pada dokumentasi kelas. */
	private Boolean aktif;

	/** Keterangan bebas atas baris penghubung. */
	private String keterangan;

	/** Konstruktor kosong yang diwajibkan JPA/Hibernate. */
	public TarifKhususPunyaAlatMedis() {
	}

	/**
	 * Mengembalikan kunci utama baris penghubung.
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
	 * Mengembalikan keterangan bebas baris penghubung.
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas baris penghubung.
	 *
	 * @param keterangan catatan bebas
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan paket tarif khusus induk. <b>Getter destruktif</b> ({@code check(...)}).
	 * Seluruh syarat berlaku-tidaknya tarif khusus — masa berlaku, status aktif, dan keempat
	 * dimensi cakupan — berada pada objek yang dikembalikan getter ini.
	 *
	 * @return paket tarif khusus induk (kolom {@code NOT NULL})
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tarif_khusus", nullable = false)
	public TarifKhusus getTarifKhusus() {
		tarifKhusus = check(tarifKhusus);
		return tarifKhusus;
	}

	/**
	 * Menautkan baris penghubung ini ke paket tarif khusus induknya.
	 *
	 * @param tarifKhusus paket tarif khusus induk; tidak boleh {@code null} saat disimpan
	 */
	public void setTarifKhusus(TarifKhusus tarifKhusus) {
		this.tarifKhusus = tarifKhusus;
	}

	/**
	 * Mengembalikan alat medis yang ditarifkan khusus dalam paket ini. <b>Getter destruktif</b>
	 * ({@code check(...)}). Inilah satu-satunya tempat identitas alat medis tersimpan bagi baris
	 * {@link BiayaAlatMedisPerKelas} versi khusus, karena kolom {@code alat_medis} pada baris tarif
	 * tersebut sengaja dikosongkan.
	 *
	 * @return alat medis yang ditarifkan khusus (kolom {@code NOT NULL})
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "alat_medis", nullable = false)
	public AlatMedis getAlatMedis() {
		alatMedis = check(alatMedis);
		return alatMedis;
	}

	/**
	 * Menetapkan alat medis yang ditarifkan khusus dalam paket ini.
	 *
	 * @param alatMedis alat medis yang ditarifkan; tidak boleh {@code null} saat disimpan
	 */
	public void setAlatMedis(AlatMedis alatMedis) {
		this.alatMedis = alatMedis;
	}

	/**
	 * Mengembalikan bendera aktif baris penghubung, menormalkan {@code null} menjadi {@code true}.
	 * <b>Getter destruktif</b>.
	 *
	 * <p>
	 * <b>Bendera ini tidak pernah dibaca oleh kode mana pun.</b> Penelusuran seluruh basis kode
	 * tidak menemukan pemanggilan {@code getAktif()} atas entitas ini, dan mesin resolusi
	 * {@code CommonTarif.getTarif} hanya menyaring status aktif pada paket induk. Menyimpan
	 * {@code false} di sini tidak berpengaruh apa pun: alat medis tetap memperoleh tarif khusus.
	 * Untuk benar-benar menghentikannya, hapus baris penghubung ini atau nonaktifkan keseluruhan
	 * paket lewat {@link TarifKhusus#setAktif(Boolean)}.
	 * </p>
	 *
	 * @return {@code true} bila ditandai aktif, tidak pernah {@code null}; nilainya tidak berpengaruh
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menyalakan atau mematikan bendera aktif baris penghubung. Perubahan tersimpan ke basis data
	 * tetapi <b>tidak mengubah perilaku apa pun</b> — lihat {@link #getAktif()}.
	 *
	 * @param aktif nilai bendera; tidak berpengaruh pada resolusi tarif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Menetapkan saklar kemudahan penyuntingan "semua harga sama".
	 *
	 * @param semuahargasama {@code true} bila harga seluruh kelas perawatan disamakan saat disunting
	 */
	public void setSemuahargasama(Boolean semuahargasama) {
		this.semuahargasama = semuahargasama;
	}

	/**
	 * Mengembalikan saklar "semua harga sama" — kemudahan penyuntingan di layar tarif, yang bila
	 * dinyalakan membuat perubahan harga pada satu kelas perawatan ikut diterapkan ke seluruh kelas
	 * lain. Saklar ini <b>tidak</b> ikut menentukan harga saat transaksi. <b>Getter destruktif</b>.
	 *
	 * <p>
	 * Getter ini bermaksud mewarisi nilai dari {@link AlatMedis#getSemuahargasama()} bila baris
	 * penghubung belum menentukan sendiri, tetapi field {@link #semuahargasama} sudah diinisialisasi
	 * {@code true} pada deklarasinya sehingga cabang pewarisan itu tidak pernah tercapai untuk objek
	 * baru — ia hanya berjalan bagi baris lama yang kolomnya {@code NULL} di basis data. Cabang
	 * tersebut juga membaca field mentah {@code alatMedis}, bukan lewat {@link #getAlatMedis()},
	 * sehingga tidak memaksa materialisasi proxy lebih dahulu.
	 * </p>
	 *
	 * @return {@code true} bila harga seluruh kelas disamakan saat disunting, tidak pernah {@code null}
	 */
	public Boolean getSemuahargasama() {
		if (semuahargasama == null && alatMedis != null) {
			semuahargasama = alatMedis.getSemuahargasama();
		}
		if (semuahargasama == null) {
			semuahargasama = true;
		}
		return semuahargasama;
	}

}
