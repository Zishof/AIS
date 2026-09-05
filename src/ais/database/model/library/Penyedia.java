package ais.database.model.library;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;




import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;



/**
 * Entitas master <b>penyedia/vendor</b> (tabel {@code library.penyedia}) — data induk vendor buku
 * dan bahan pustaka yang dipakai proses pengadaan perpustakaan: {@link SeleksiVendor} (tender/
 * seleksi vendor sebelum pembelian) menautkannya lewat {@link SeleksiVendorDetail#getPenyedia()}
 * dan {@link SeleksiVendor#getRekomendasiPenyedia()}, sementara {@link SurveyVendor} (survei
 * evaluasi kinerja pasca-transaksi) menautkannya lewat {@link SurveyVendorVendor#getPenyedia()}.
 *
 * <p>Meski package-nya {@code ais.database.model.library}, kelas ini <b>bukan milik eksklusif
 * modul perpustakaan</b> — ia dipakai bersama sebagai vendor umum oleh modul {@code sirs} (sistem
 * informasi rumah sakit), {@code koperasi}, {@code inventory}, serta beberapa endpoint di
 * {@code ais.action.servlet.api}. Padanannya di modul lain adalah
 * {@code ais.database.model.asset.PenyediaAsset} (vendor pengadaan aset tetap, jauh lebih besar
 * dan melalui disposisi SOP) dan {@code koperasi.SupplierInventoryProfile}.</p>
 *
 * <h2>Tanpa gerbang kelayakan sama sekali</h2>
 * <p>Berbeda dari {@code asset.PenyediaAsset} yang setidaknya punya flag {@code aktif} yang
 * ditegakkan pemilih vendornya, kelas ini <b>tidak memiliki kolom status, aktif, maupun
 * blacklist apa pun</b>. {@code PenyediaAction} (layar CRUD-nya di paket
 * {@code ais.action.master.library}) tidak mengelola konsep semacam itu. Setiap baris yang
 * tersimpan di tabel ini selalu dianggap dapat dipakai — tidak ada mekanisme untuk menandai
 * vendor bermasalah tanpa menghapus barisnya sama sekali (yang berisiko merusak riwayat
 * {@link SeleksiVendorDetail}/{@link SurveyVendorVendor} yang sudah menunjuknya). Ini konsisten
 * dengan pola "status vendor murni deskriptif tak ditegakkan" yang berulang di modul lain, hanya
 * saja di sini bahkan kolom deskriptifnya pun tidak ada.</p>
 *
 * <h2>Isi data</h2>
 * <p>Data yang disimpan hanya identitas dan kontak dasar: {@link #getKode()}, {@link #getNama()},
 * {@link #getAlamat()}, {@link #getKodePos()}, {@link #getTelp()}, {@link #getFax()},
 * {@link #getKontak()}, {@link #getEmail()}, dan {@link #getKeterangan()} bebas. Tidak ada data
 * legalitas (NPWP, akta), tidak ada klasifikasi jenis/kategori, dan tidak ada koordinat lokasi —
 * jauh lebih ramping daripada {@code asset.PenyediaAsset}.</p>
 *
 * <h2>Akun utang</h2>
 * <p>{@link #getAkunUtang()} menautkan vendor ke bagan akun {@code akunting.Akun} untuk keperluan
 * penjurnalan kulakan/pembayaran utang supplier toko (ditambahkan 2026-08-20, sejajar dengan
 * {@code PenyediaAsset.akunUtang}). Bila kosong, pemakainya jatuh ke konfigurasi
 * {@code akun_utang_supplier_toko} lalu {@code akun_utang_id_default_data} — lihat komentar pada
 * bidang {@link #akunUtang}.</p>
 *
 * <h2>Bidang audit bayangan</h2>
 * <p>{@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} beserta {@link #onUpdate()} adalah
 * keharusan teknis agar {@code AuditTimestampInterceptor} dapat bekerja, bukan duplikasi yang bisa
 * dihapus. Setternya sengaja mengabaikan masukan kosong agar jejak audit yang sudah ada tidak
 * tertimpa string kosong dari jalur salin/klon objek.</p>
 *
 * @see SeleksiVendor
 * @see SeleksiVendorDetail
 * @see SurveyVendor
 * @see SurveyVendorVendor
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "library", name = "penyedia")



public class Penyedia extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilai warisan cetakan hbm2java; jangan diubah tanpa alasan.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama basis data, dibangkitkan {@code IDENTITY}; {@code null} selama baris belum tersimpan. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi {@code AuditTimestampInterceptor}, bukan oleh form. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; pasangan teknis dari {@link #oleh}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah data vendor ini.
	 *
	 * @return id pengguna terakhir, atau {@code null} bila baris belum pernah diubah lewat jalur
	 *         yang memasang interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna yang terakhir mengubah data vendor ini.
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
	 * Menyetel nama pengguna yang terakhir mengubah data vendor ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, masukan {@code null}/kosong diabaikan.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah data vendor ini.
	 *
	 * @return nama pengguna terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

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
	 * Mengembalikan stempel waktu perubahan terakhir baris vendor ini.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang baru
	 *         dibuat di memori karena diinisialisasi dari {@code WaktuUtil.getDate()}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat: nama vendor apa adanya (tidak dipangkas, tidak memicu resolusi
	 * lazy apa pun), dipakai label bawaan komponen ZK dan penelusuran log.
	 *
	 * @return nama vendor; dapat {@code null} bila belum diisi
	 */
	public String toString() {
		return nama;
	}

	/** Kode vendor (bebas, tidak dibangkitkan otomatis — beda dengan {@code asset.PenyediaAsset}). */
	private String kode;
	/** Nama vendor/badan usaha. */
	private String nama;
	/** Alamat jalan vendor. */
	private String alamat;
	/** Kode pos alamat vendor. */
	private String kodePos;
	/** Nomor telepon vendor. */
	private String telp;
	/** Nomor faks vendor. */
	private String fax;
	/** Nama orang yang dapat dihubungi di pihak vendor; dipakai sebagai bawaan
	 *  {@link SeleksiVendorDetail#getPicVendor()} dan {@link SurveyVendorVendor#getPicVendor()}. */
	private String kontak;
	/** Alamat surel vendor. */
	private String email;
	/** Catatan bebas tentang vendor. */
	private String keterangan;

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public Penyedia() {
	}

	/**
	 * Mengembalikan kunci utama baris vendor ini.
	 *
	 * @return id vendor, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris vendor ini. Hanya untuk kebutuhan Hibernate dan penyalinan objek.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama vendor setelah dipangkas spasi tepinya.
	 *
	 * @return nama vendor yang sudah dipangkas, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama vendor.
	 *
	 * @param nama nama vendor
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan catatan bebas tentang vendor.
	 *
	 * @return catatan bebas; boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel catatan bebas tentang vendor.
	 *
	 * @param keterangan teks catatan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan alamat jalan vendor.
	 *
	 * @return alamat; boleh {@code null}
	 */
	public String getAlamat() {
		return alamat;
	}

	/**
	 * Menyetel alamat jalan vendor.
	 *
	 * @param alamat alamat; boleh {@code null}
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Mengembalikan kode pos alamat vendor.
	 *
	 * @return kode pos; boleh {@code null}
	 */
	public String getKodePos() {
		return kodePos;
	}

	/**
	 * Menyetel kode pos alamat vendor.
	 *
	 * @param kodePos kode pos; boleh {@code null}
	 */
	public void setKodePos(String kodePos) {
		this.kodePos = kodePos;
	}

	/**
	 * Mengembalikan nomor telepon vendor.
	 *
	 * @return nomor telepon; boleh {@code null}
	 */
	public String getTelp() {
		return telp;
	}

	/**
	 * Menyetel nomor telepon vendor.
	 *
	 * @param telp nomor telepon; boleh {@code null}
	 */
	public void setTelp(String telp) {
		this.telp = telp;
	}

	/**
	 * Mengembalikan nomor faks vendor.
	 *
	 * @return nomor faks; boleh {@code null}
	 */
	public String getFax() {
		return fax;
	}

	/**
	 * Menyetel nomor faks vendor.
	 *
	 * @param fax nomor faks; boleh {@code null}
	 */
	public void setFax(String fax) {
		this.fax = fax;
	}

	/**
	 * Mengembalikan nama orang yang dapat dihubungi di pihak vendor.
	 *
	 * @return nama kontak; boleh {@code null}
	 */
	public String getKontak() {
		return kontak;
	}

	/**
	 * Menyetel nama orang yang dapat dihubungi di pihak vendor.
	 *
	 * @param kontak nama kontak; boleh {@code null}
	 */
	public void setKontak(String kontak) {
		this.kontak = kontak;
	}

	/**
	 * Mengembalikan alamat surel vendor.
	 *
	 * @return alamat surel; boleh {@code null}
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * Menyetel alamat surel vendor.
	 *
	 * @param email alamat surel; boleh {@code null}
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Mengembalikan kode vendor. Berbeda dari {@code asset.PenyediaAsset}, tidak ada pembangkitan
	 * otomatis dari id — kolom ini murni bebas isi.
	 *
	 * @return kode vendor; boleh {@code null}
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Menyetel kode vendor. Keunikannya tidak ditegakkan skema basis data maupun getter ini.
	 *
	 * @param kode kode vendor; boleh {@code null}
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}


	/**
	 * Akun utang dagang penyedia -- ditambahkan 2026-08-20 sebagai prasyarat penjurnalan
	 * kulakan & pembayaran hutang supplier toko. Sejajar dengan {@code PenyediaAsset.akunUtang}
	 * pada rantai pengadaan aset. Bila kosong, pemakainya jatuh ke konfigurasi
	 * {@code akun_utang_supplier_toko} lalu {@code akun_utang_id_default_data}.
	 * Kolomnya dibuat otomatis oleh Hibernate.
	 */
	private ais.database.model.akunting.Akun akunUtang;

	/**
	 * Mengembalikan akun utang dagang vendor ini setelah proksi malasnya diselesaikan
	 * {@code check(...)} milik {@code GeneralValueObject}.
	 *
	 * <p>Hasil resolusi ditulis kembali ke bidang instans {@link #akunUtang} agar resolusi tidak
	 * diulang pada pembacaan berikutnya — getter ini karena itu memutasi state, sejalan dengan
	 * pola {@code check(...)} yang berulang di seluruh entitas paket ini.</p>
	 *
	 * @return akun utang vendor, atau {@code null} bila belum ditautkan (pemanggil lazim jatuh ke
	 *         konfigurasi {@code akun_utang_supplier_toko} lalu {@code akun_utang_id_default_data})
	 */
	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "akun_utang", nullable = true)
	public ais.database.model.akunting.Akun getAkunUtang() {
		akunUtang = check(akunUtang);
		return akunUtang;
	}

	/**
	 * Menyetel akun utang dagang vendor ini.
	 *
	 * @param akunUtang akun bagan akun untuk pencatatan utang dagang; boleh {@code null}
	 */
	public void setAkunUtang(ais.database.model.akunting.Akun akunUtang) {
		this.akunUtang = akunUtang;
	}

}
