package ais.database.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.apache.commons.lang.StringUtils;
import org.hibernate.envers.Audited;

import ais.common.Common;

import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Model data untuk badan hukum. Tipe ini membawa state yang dipertukarkan oleh lapisan
 * persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang
 * dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code Date tanggal_dirubah}, {@code String kode}, {@code String nama}, {@code String
 * alamat1}, {@code String alamat2}; pemetaan persistence: tabel {@code public.badanhukum}; pembacaan/pencarian
 * ({@code getOlehId()}, {@code getOleh()}, {@code getTanggal_dirubah()}, {@code getId()}, {@code getKode()},
 * {@code getNama()}); mutasi data ({@code setOlehId()}, {@code setOleh()}, {@code onUpdate()}, {@code
 * setTanggal_dirubah()}, {@code setId()}, {@code setKode()}); operasi domain lain ({@code toString()}, {@code
 * appendEmail()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> selain accessor state, operasi domain yang disebut di atas dapat membaca/mengubah
 * persistence, memicu lifecycle, atau membentuk komponen UI. Jangan menganggap model ini selalu murni;
 * panggil operasi tersebut melalui alur service dengan session, transaksi, dan otorisasi yang sesuai agar
 * perilakunya tidak disalin ke tempat lain.</p>
 *
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "badanhukum")

public class BadanHukum extends GeneralValueObject {

	/** Versi serialisasi Java untuk kompatibilitas {@code Serializable}. */
	private static final long serialVersionUID = 5232831172545879880L;
	/** Primary key entity (kolom {@code id}); BERBEDA dari pola umum -- lihat catatan {@link #getId()}. */
	private Long id;
	/**
	 * Nama pengguna pengubah terakhir. Field ini MENIMPA (shadow) field bernama sama pada
	 * {@link GeneralValueObject}; getter/setter di bawah beroperasi pada field lokal ini.
	 */
	private String oleh;
	/** Id pengguna pengubah terakhir; shadow dari field sama pada {@link GeneralValueObject}. */
	private String olehId;

	/**
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi.
	 * @see GeneralValueObject#getOlehId()
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir. Nilai {@code null}/kosong diabaikan diam-diam,
	 * sama seperti {@link GeneralValueObject#setOlehId(String)}.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Nilai {@code null}/kosong diabaikan diam-diam,
	 * sama seperti {@link #setOlehId(String)}.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA sebelum UPDATE: memperbarui {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     /** Stempel waktu perubahan terakhir; diinisialisasi ke saat object dibuat. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah stempel waktu perubahan terakhir yang baru. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return stempel waktu perubahan terakhir. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi ringkas untuk debug/log: nama badan hukum apa adanya. */
	public String toString() {
		return nama;
	}

	/** Kode ringkas badan hukum. */
	private String kode;
	/** Nama badan hukum (mis. yayasan/PT). */
	private String nama;
	/** Baris pertama alamat. */
	private String alamat1;
	/** Baris kedua alamat. */
	private String alamat2;
	/** Kota (teks bebas, bukan FK ke master kota). */
	private String kota;
	/** Kode pos. */
	private String kodePos;
	/** Nomor telepon. */
	private String telepon;
	/** Nomor faksimil. */
	private String faksimil;
	/** Tanggal akta pendirian. */
	private Date tanggalAkta;
	/** Nomor/nama akta pendirian. */
	private String namaAkta;
	/** Tanggal pengesahan badan hukum oleh instansi berwenang. */
	private Date tanggalPengesahan;
	/** Nomor surat pengesahan. */
	private String nomorPengesahan;
	/** Tanggal awal badan hukum ini berdiri/beroperasi. */
	private Date tanggalAwalPendirian;
	/** Daftar alamat surel, dipisah koma; lihat {@link #getEmail()}/{@link #appendEmail(String)}. */
	private String email;
	/** Alamat situs web resmi. */
	private String alamatWebsite;
	/** Path/nama berkas logo. */
	private String logo;

	/** Konstruktor kosong, dipakai Hibernate. */
	public BadanHukum() {

	}

	/**
	 * Konstruktor lengkap, mengisi seluruh field sekaligus.
	 *
	 * @param id                   primary key.
	 * @param kode                 kode ringkas.
	 * @param nama                 nama badan hukum.
	 * @param alamat1              baris pertama alamat.
	 * @param alamat2              baris kedua alamat.
	 * @param kota                 kota (teks bebas).
	 * @param kodePos              kode pos.
	 * @param telepon              nomor telepon.
	 * @param faksimil             nomor faksimil.
	 * @param tanggalAkta          tanggal akta pendirian.
	 * @param namaAkta             nomor/nama akta pendirian.
	 * @param tanggalPengesahan    tanggal pengesahan.
	 * @param nomorPengesahan      nomor surat pengesahan.
	 * @param tanggalAwalPendirian tanggal awal berdiri.
	 * @param email                daftar alamat surel dipisah koma.
	 * @param alamatWebsite        alamat situs web resmi.
	 * @param logo                 path/nama berkas logo.
	 */
	public BadanHukum(Long id, String kode, String nama, String alamat1, String alamat2, String kota, String kodePos,
			String telepon, String faksimil, Date tanggalAkta, String namaAkta, Date tanggalPengesahan,
			String nomorPengesahan, Date tanggalAwalPendirian, String email, String alamatWebsite, String logo) {
		super();
		this.id = id;
		this.kode = kode;
		this.nama = nama;
		this.alamat1 = alamat1;
		this.alamat2 = alamat2;
		this.kota = kota;
		this.kodePos = kodePos;
		this.telepon = telepon;
		this.faksimil = faksimil;
		this.tanggalAkta = tanggalAkta;
		this.namaAkta = namaAkta;
		this.tanggalPengesahan = tanggalPengesahan;
		this.nomorPengesahan = nomorPengesahan;
		this.tanggalAwalPendirian = tanggalAwalPendirian;
		this.email = email;
		this.alamatWebsite = alamatWebsite;
		this.logo = logo;
	}

	/**
	 * @return primary key entity. BERBEDA dari pola umum entity AIS lainnya: TIDAK memakai
	 *         {@code @GeneratedValue(strategy = IDENTITY)} -- id harus diisi manual (mis. lewat
	 *         konstruktor lengkap) sebelum di-persist, bukan diserahkan ke auto-increment database.
	 */
	@Id
	@Column(name = "id", insertable = false, nullable = false)
	public Long getId() {
		return id;
	}

	/** @param id primary key baru. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return kode ringkas badan hukum, boleh {@code null}. */
	@Column(name = "kode", length = 150)
	public String getKode() {
		return kode;
	}

	/** @param kode kode ringkas baru. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/** @return nama badan hukum, boleh {@code null}. */
	@Column(name = "nama", length = 150)
	public String getNama() {
		return nama;
	}

	/** @param nama nama badan hukum baru. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return baris pertama alamat, boleh {@code null}. */
	@Column(name = "alamat1")
	public String getAlamat1() {
		return alamat1;
	}

	/** @param alamat1 baris pertama alamat baru. */
	public void setAlamat1(String alamat1) {
		this.alamat1 = alamat1;
	}

	/** @return baris kedua alamat, boleh {@code null}. */
	@Column(name = "alamat2")
	public String getAlamat2() {
		return alamat2;
	}

	/** @param alamat2 baris kedua alamat baru. */
	public void setAlamat2(String alamat2) {
		this.alamat2 = alamat2;
	}

	/** @return kota (teks bebas), boleh {@code null}. */
	@Column(name = "kota", length = 150)
	public String getKota() {
		return kota;
	}

	/** @param kota kota (teks bebas) baru. */
	public void setKota(String kota) {
		this.kota = kota;
	}

	/** @return kode pos, boleh {@code null}. */
	@Column(name = "kodepos", length = 150)
	public String getKodePos() {
		return kodePos;
	}

	/** @param kodePos kode pos baru. */
	public void setKodePos(String kodePos) {
		this.kodePos = kodePos;
	}

	/** @return nomor telepon, boleh {@code null}. */
	@Column(name = "telepon", length = 100)
	public String getTelepon() {
		return telepon;
	}

	/** @param telepon nomor telepon baru. */
	public void setTelepon(String telepon) {
		this.telepon = telepon;
	}

	/** @return nomor faksimil, boleh {@code null}. */
	@Column(name = "faksimil", length = 100)
	public String getFaksimil() {
		return faksimil;
	}

	/** @param faksimil nomor faksimil baru. */
	public void setFaksimil(String faksimil) {
		this.faksimil = faksimil;
	}

	/** @return tanggal akta pendirian, boleh {@code null}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggalakta", length = 0)
	public Date getTanggalAkta() {
		return tanggalAkta;
	}

	/** @param tanggalAkta tanggal akta pendirian baru. */
	public void setTanggalAkta(Date tanggalAkta) {
		this.tanggalAkta = tanggalAkta;
	}

	/** @return nomor/nama akta pendirian, boleh {@code null}. */
	@Column(name = "namaakta", length = 150)
	public String getNamaAkta() {
		return namaAkta;
	}

	/** @param namaAkta nomor/nama akta pendirian baru. */
	public void setNamaAkta(String namaAkta) {
		this.namaAkta = namaAkta;
	}

	/** @return tanggal pengesahan badan hukum, boleh {@code null}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggalpengesahan", length = 0)
	public Date getTanggalPengesahan() {
		return tanggalPengesahan;
	}

	/** @param tanggalPengesahan tanggal pengesahan baru. */
	public void setTanggalPengesahan(Date tanggalPengesahan) {
		this.tanggalPengesahan = tanggalPengesahan;
	}

	/** @return nomor surat pengesahan, boleh {@code null}. */
	@Column(name = "nomorpengesahan", length = 100)
	public String getNomorPengesahan() {
		return nomorPengesahan;
	}

	/** @param nomorPengesahan nomor surat pengesahan baru. */
	public void setNomorPengesahan(String nomorPengesahan) {
		this.nomorPengesahan = nomorPengesahan;
	}

	/** @return tanggal awal berdiri, boleh {@code null}. */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggalawalpendirian", length = 0)
	public Date getTanggalAwalPendirian() {
		return tanggalAwalPendirian;
	}

	/** @param tanggalAwalPendirian tanggal awal berdiri baru. */
	public void setTanggalAwalPendirian(Date tanggalAwalPendirian) {
		this.tanggalAwalPendirian = tanggalAwalPendirian;
	}

	/**
	 * @return daftar alamat surel dipisah koma. Setiap pemanggilan MENORMALKAN field ini: koma
	 *         ganda dimampatkan (hingga 5 iterasi {@code replaceAll}), {@code null} diseragamkan
	 *         jadi string kosong, dan hasil yang hanya berisi satu koma diseragamkan jadi string
	 *         kosong juga.
	 */
	@Column(name = "email", length = 255)
	public String getEmail() {
		if (email != null && email.contains(",,")) {
			for (int i = 0; i < 5; i++) {
				email = email.replaceAll(",,", ",");
			}
		}
		if (email == null) {
			email = "";
		}
		if (email.trim().equals(",")) {
			email = "";
		}
		return this.email;
	}

	/** @param email daftar alamat surel dipisah koma yang baru; akan dinormalkan ulang oleh {@link #getEmail()}. */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Menambahkan satu alamat surel ke {@link #email} (dipisah koma), dengan validasi: diabaikan
	 * bila argumen {@code null}/kosong, sudah ada dalam daftar (substring check, BUKAN pencocokan
	 * exact per-alamat), bukan format surel valid ({@link Common#isValidEmailAddress(String)}),
	 * atau diawali {@code "@"}.
	 *
	 * @param email alamat surel yang akan ditambahkan.
	 */
	public void appendEmail(String email) {
		if (this.email != null && email != null && !email.trim().isEmpty() && StringUtils.contains(this.email, email)) {
			return;
		}
		if (email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email) && !email.startsWith("@")) {
			this.email = this.email == null || this.email.trim().isEmpty() ? email : this.email + "," + email;
		}
	}

	/** @return alamat situs web resmi, boleh {@code null}. */
	@Column(name = "alamatwebsite", length = 100)
	public String getAlamatWebsite() {
		return alamatWebsite;
	}

	/** @param alamatWebsite alamat situs web resmi baru. */
	public void setAlamatWebsite(String alamatWebsite) {
		this.alamatWebsite = alamatWebsite;
	}

	/** @return path/nama berkas logo, boleh {@code null}. */
	@Column(name = "logo", length = 100)
	public String getLogo() {
		return logo;
	}

	/** @param logo path/nama berkas logo baru. */
	public void setLogo(String logo) {
		this.logo = logo;
	}

}
