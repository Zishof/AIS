package ais.database.model;

import java.util.Date;

import javax.persistence.Column;

import org.apache.commons.lang.StringUtils;

import ais.common.Common;

/**
 * Model data untuk karyawan. Tipe ini membawa state yang dipertukarkan oleh lapisan persistence,
 * service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code String code}, {@code String mycode},
 * {@code String nama}, {@code String alamat}, {@code String email}, {@code String telp}, {@code String kelamin},
 * {@code String tempatlahir}; pembacaan/pencarian ({@code getCode()}, {@code getMycode()}, {@code getNama()},
 * {@code getAlamat()}, {@code getEmail()}, {@code getTelp()}); mutasi data ({@code setCode()}, {@code
 * setMycode()}, {@code setNama()}, {@code setAlamat()}, {@code setEmail()}, {@code setTelp()}); operasi domain
 * lain ({@code appendEmail()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut
 * di atas.</p>
 * <p><b>Efek samping:</b> selain accessor state, operasi domain yang disebut di atas dapat membaca/mengubah
 * persistence, memicu lifecycle, atau membentuk komponen UI. Jangan menganggap model ini selalu murni;
 * panggil operasi tersebut melalui alur service dengan session, transaksi, dan otorisasi yang sesuai agar
 * perilakunya tidak disalin ke tempat lain.</p>
 *
 * @see GeneralValueObject
 */
public abstract class Karyawan extends GeneralValueObject { 

	/**
	 * 
	 */
	private static final long serialVersionUID = -4458435422391064065L;

	private String code;
	private String mycode;
	private String nama;
	private String alamat;
	private String email;
	private String telp;
	private String kelamin;
	private String tempatlahir;
	private String pangkat;
	private String golongan;
	private String jabatan;
	private String spesialisasi1;
	private String spesialisasi2;
	private String spesialisasi3;
	private Date tanggallahir;

	private Jurusan jurusan;
	private Fakultas fakultas;

	private Integer tetap = 0;

	private String idfinger;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getMycode() {
		return mycode;
	}

	public void setMycode(String mycode) {
		this.mycode = mycode;
	}

	public String getNama() {
		return code + "-" + nama;
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	public String getAlamat() {
		return alamat;
	}

	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

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

	public void setEmail(String email) {
		this.email = email;
	}

	public void appendEmail(String email) {
		if (this.email != null && email != null && !email.trim().isEmpty() && StringUtils.contains(this.email, email)) {
			return;
		}
		if (email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email) && !email.startsWith("@")) {
			this.email = this.email == null || this.email.trim().isEmpty() ? email : this.email + "," + email;
		}
	}

	public String getTelp() {
		return telp;
	}

	public void setTelp(String telp) {
		this.telp = telp;
	}

	public String getKelamin() {
		return kelamin;
	}

	public void setKelamin(String kelamin) {
		this.kelamin = kelamin;
	}

	public String getTempatlahir() {
		return tempatlahir;
	}

	public void setTempatlahir(String tempatlahir) {
		this.tempatlahir = tempatlahir;
	}

	public String getPangkat() {
		return pangkat;
	}

	public void setPangkat(String pangkat) {
		this.pangkat = pangkat;
	}

	public String getGolongan() {
		return golongan;
	}

	public void setGolongan(String golongan) {
		this.golongan = golongan;
	}

	public String getJabatan() {
		return jabatan;
	}

	public void setJabatan(String jabatan) {
		this.jabatan = jabatan;
	}

	public String getSpesialisasi1() {
		return spesialisasi1;
	}

	public void setSpesialisasi1(String spesialisasi1) {
		this.spesialisasi1 = spesialisasi1;
	}

	public String getSpesialisasi2() {
		return spesialisasi2;
	}

	public void setSpesialisasi2(String spesialisasi2) {
		this.spesialisasi2 = spesialisasi2;
	}

	public String getSpesialisasi3() {
		return spesialisasi3;
	}

	public void setSpesialisasi3(String spesialisasi3) {
		this.spesialisasi3 = spesialisasi3;
	}

	public Date getTanggallahir() {
		return tanggallahir;
	}

	public void setTanggallahir(Date tanggallahir) {
		this.tanggallahir = tanggallahir;
	}

	public Jurusan getJurusan() {
		return jurusan;
	}

	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	public Fakultas getFakultas() {
		return fakultas;
	}

	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	public Integer getTetap() {
		return tetap;
	}

	public void setTetap(Integer tetap) {
		this.tetap = tetap;
	}

	public String getIdfinger() {
		return idfinger;
	}

	public void setIdfinger(String idfinger) {
		this.idfinger = idfinger;
	}

}
