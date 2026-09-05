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
 * Baris penghubung antara satu paket {@link TarifKhusus} dan satu {@link ItemMedis}: menyatakan
 * bahwa item medis (obat atau barang) tersebut dihargai khusus dalam paket itu. Entitas ini adalah
 * kembaran {@link TarifKhususPunyaTindakan} dan {@link TarifKhususPunyaAlatMedis} untuk sumbu
 * barang.
 *
 * <h3>Perbedaan penting: item tidak memakai tarif per kelas perawatan</h3>
 * Berbeda dari kedua kembarannya yang masing-masing memiliki entitas tarif per kelas tersendiri
 * ({@link BiayaTindakanPerKelas} dan {@link BiayaAlatMedisPerKelas}) yang merujuk balik ke baris
 * penghubung, tidak ada entitas {@code BiayaItemPerKelas}. Harga item medis ditentukan
 * {@link HargaJualItem}, dan {@link Biaya} merujuk harga jual itu langsung lewat
 * {@link Biaya#getHargaJualItem()}. Karena itu kelas ini <b>tidak memiliki entitas anak yang
 * merujuknya</b> di klaster tarif — ia hanya menandai keanggotaan item dalam paket tarif khusus dan
 * menjadi sasaran pencarian {@code CommonTarif.getTarif} dengan kriteria
 * {@code eq("item", item)}. Konsekuensinya, dukungan harga khusus untuk item medis berhenti pada
 * penandaan keanggotaan ini; bagaimana harga khusus itu benar-benar diterapkan bergantung pada
 * layar dan helper yang membacanya, bukan pada susunan tarif per kelas seperti pada tindakan dan
 * alat medis.
 *
 * <h3>PERINGATAN — bendera {@code aktif} adalah bendera tidur</h3>
 * {@link #getAktif()} lengkap dengan setter dan normalisasi {@code null} menjadi {@code true},
 * sehingga tampak seperti saklar untuk menonaktifkan satu item dari sebuah paket tarif khusus.
 * Kenyataannya <b>tidak ada satu pun kode di seluruh aplikasi yang membacanya</b>: mesin resolusi
 * {@code CommonTarif.getTarif} hanya memeriksa {@code aktif} pada paket induk
 * ({@link TarifKhusus#getAktif()}). Menyimpan {@code aktif = false} di sini tidak berpengaruh apa
 * pun. Bendera tidur yang sama ada pada {@link TarifKhususPunyaTindakan#getAktif()} dan
 * {@link TarifKhususPunyaAlatMedis#getAktif()}.
 *
 * <h3>Nilai awal {@code semuahargasama} mematikan pewarisan dari master</h3>
 * {@link #getSemuahargasama()} bermaksud mewarisi nilai dari {@link ItemMedis#getSemuahargasama()}
 * untuk keadaan {@code null}, tetapi field {@link #semuahargasama} sudah diinisialisasi
 * {@code true} pada deklarasinya sehingga cabang pewarisan itu <b>kode mati</b> bagi setiap objek
 * baru. Dampaknya ringan karena saklar ini hanya kemudahan penyuntingan layar.
 *
 * <h3>Pola arsitektur</h3>
 * <ul>
 * <li><b>Getter destruktif</b> — {@link #getTarifKhusus()}, {@link #getItem()},
 * {@link #getAktif()}, dan {@link #getSemuahargasama()} menulis balik ke field.</li>
 * <li><b>Field audit bayangan</b> — keharusan teknis infrastruktur audit. Kelas ini tidak memiliki
 * {@code toString()}, sehingga komponen ZK memakai representasi bawaan {@code Object} kecuali layar
 * merakit labelnya sendiri.</li>
 * <li><b>Tanpa sumbu tenant</b> — sesuai keterbatasan seluruh modul {@code sirs}.</li>
 * <li>Kedua FK ({@code tarif_khusus} dan {@code item}) bersifat {@code NOT NULL} sehingga baris
 * yatim tidak mungkin tersimpan, tetapi tanpa indeks unik atas pasangannya — satu item dapat
 * terdaftar berulang dalam paket yang sama.</li>
 * </ul>
 *
 * @see TarifKhusus paket tarif khusus induk yang membawa dimensi cakupan dan masa berlaku
 * @see HargaJualItem penentu harga item medis yang dirujuk langsung oleh {@link Biaya}
 * @see TarifKhususPunyaTindakan kembaran entitas ini untuk tindakan/layanan
 * @see TarifKhususPunyaAlatMedis kembaran entitas ini untuk alat medis
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "tarif_khusus_punya_item")
public class TarifKhususPunyaItem extends GeneralValueObject {

	/**
	 * Versi serialisasi Java, seragam untuk seluruh entitas modul {@code sirs} karena berasal dari
	 * templat pembangkit yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama tabel {@code sirs.tarif_khusus_punya_item}, dibangkitkan basis data (IDENTITY). */
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

	/** Item medis (obat/barang) yang dihargai khusus dalam paket ini; kolomnya {@code NOT NULL}. */
	private ItemMedis item;

	/** Kemudahan penyuntingan layar: harga sama untuk seluruh kelas perawatan. */
	private Boolean semuahargasama = true;

	/** Bendera tidur — tidak pernah dibaca kode mana pun. Lihat peringatan pada dokumentasi kelas. */
	private Boolean aktif;

	/** Keterangan bebas atas baris penghubung. */
	private String keterangan;

	/** Konstruktor kosong yang diwajibkan JPA/Hibernate. */
	public TarifKhususPunyaItem() {
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
	 * Mengembalikan item medis yang dihargai khusus dalam paket ini. <b>Getter destruktif</b>
	 * ({@code check(...)}).
	 *
	 * @return item medis yang dihargai khusus (kolom {@code NOT NULL})
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item", nullable = false)
	public ItemMedis getItem() {
		item = check(item);
		return item;
	}

	/**
	 * Menetapkan item medis yang dihargai khusus dalam paket ini.
	 *
	 * @param item item medis; tidak boleh {@code null} saat disimpan
	 */
	public void setItem(ItemMedis item) {
		this.item = item;
	}

	/**
	 * Mengembalikan bendera aktif baris penghubung, menormalkan {@code null} menjadi {@code true}.
	 * <b>Getter destruktif</b>.
	 *
	 * <p>
	 * <b>Bendera ini tidak pernah dibaca oleh kode mana pun.</b> Penelusuran seluruh basis kode
	 * tidak menemukan pemanggilan {@code getAktif()} atas entitas ini, dan mesin resolusi
	 * {@code CommonTarif.getTarif} hanya menyaring status aktif pada paket induk. Menyimpan
	 * {@code false} di sini tidak berpengaruh apa pun. Untuk benar-benar mengeluarkan sebuah item
	 * dari paket, hapus baris penghubung ini atau nonaktifkan keseluruhan paket lewat
	 * {@link TarifKhusus#setAktif(Boolean)}.
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
	 * Getter ini bermaksud mewarisi nilai dari {@link ItemMedis#getSemuahargasama()} bila baris
	 * penghubung belum menentukan sendiri, tetapi field {@link #semuahargasama} sudah diinisialisasi
	 * {@code true} pada deklarasinya sehingga cabang pewarisan itu tidak pernah tercapai untuk objek
	 * baru — ia hanya berjalan bagi baris lama yang kolomnya {@code NULL} di basis data. Cabang
	 * tersebut juga membaca field mentah {@code item}, bukan lewat {@link #getItem()}, sehingga
	 * tidak memaksa materialisasi proxy lebih dahulu.
	 * </p>
	 *
	 * @return {@code true} bila harga seluruh kelas disamakan saat disunting, tidak pernah {@code null}
	 */
	public Boolean getSemuahargasama() {
		if (semuahargasama == null && item != null) {
			semuahargasama = item.getSemuahargasama();
		}
		if (semuahargasama == null) {
			semuahargasama = true;
		}
		return semuahargasama;
	}

}
