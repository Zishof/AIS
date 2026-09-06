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
 * <h3>PENTING — seluruh state kelas ini dibayangi oleh kedua subclass-nya</h3>
 * <p>{@code Karyawan} punya tepat dua turunan, {@link Dosen} dan {@link Pegawai}, dan keduanya
 * <b>mendeklarasikan ulang field-field di bawah ini sebagai field {@code private} milik sendiri</b>
 * sekaligus meng-override accessor-nya. Akibatnya salinan field yang dideklarasikan di kelas ini
 * hampir seluruhnya <i>mati</i>: yang terbaca dan tersimpan adalah salinan milik subclass, bukan
 * milik {@code Karyawan}. Rincian hasil verifikasi langsung terhadap kode kedua subclass:</p>
 * <ul>
 *   <li><b>Dibayangi oleh {@link Dosen} DAN {@link Pegawai}</b> (17 field): {@code code},
 *       {@code mycode}, {@code nama}, {@code alamat}, {@code email}, {@code telp},
 *       {@code kelamin}, {@code tempatlahir}, {@code pangkat}, {@code golongan}, {@code jabatan},
 *       {@code spesialisasi1}, {@code spesialisasi2}, {@code spesialisasi3}, {@code tanggallahir},
 *       {@code tetap}, {@code idfinger}.</li>
 *   <li><b>Dibayangi {@link Dosen} saja</b> (2 field): {@code jurusan} dan {@code fakultas} —
 *       {@link Pegawai} <b>tidak</b> mendeklarasikan ulang keduanya dan mewarisi accessor kelas
 *       ini apa adanya.</li>
 * </ul>
 * <p>Jadi {@link Dosen} membayangi <b>seluruh 19 field</b>, sedangkan {@link Pegawai} membayangi 17
 * dan mewarisi dua sisanya. Pasangan {@link #getJurusan()}/{@link #getFakultas()} di kelas inilah
 * satu-satunya bagian state {@code Karyawan} yang benar-benar masih hidup dalam praktik, yaitu saat
 * dipakai lewat {@link Pegawai}.</p>
 *
 * <h3>Mengapa dibayangi dan bukan diwarisi langsung?</h3>
 * <ol>
 *   <li><b>Field-nya {@code private}.</b> Subclass tidak bisa menyentuhnya, sedangkan {@link Dosen}
 *       dan {@link Pegawai} perlu membaca/menulis backing field secara langsung di dalam override
 *       getter mereka. Satu-satunya jalan adalah menyediakan backing field sendiri.</li>
 *   <li><b>Pemetaan Hibernate memakai <i>property access</i>.</b> Anotasi {@code @Column} dan
 *       {@code @ManyToOne} diletakkan pada getter, bukan field. {@link Dosen} dipetakan ke tabel
 *       {@code dosen} dan {@link Pegawai} ke tabel {@code pegawai} dengan panjang kolom serta
 *       anotasi yang berbeda-beda, sehingga pemetaan tidak bisa dipusatkan di sini.</li>
 *   <li><b>Kelas ini bukan entity.</b> {@code Karyawan} tidak beranotasi {@code @Entity} maupun
 *       {@code @MappedSuperclass} dan tidak terdaftar di {@code hibernate.cfg.xml}; ia murni kelas
 *       basis Java. Anotasi {@code @Column} yang menempel pada {@link #getEmail()} di kelas ini
 *       karena itu <b>tidak berefek apa pun</b> — sisa salin-tempel, bukan pemetaan aktif.</li>
 * </ol>
 *
 * <h3>Perilaku yang benar-benar berbeda antar lapisan</h3>
 * <ul>
 *   <li><b>{@code toString()}</b> — {@code Karyawan} <i>tidak</i> meng-override-nya sama sekali
 *       (memakai milik {@link GeneralValueObject}), {@link Dosen} memakai
 *       {@code id + "-" + nidn + "-" + nama}, {@link Pegawai} memakai {@code id + "-" + nama}.</li>
 *   <li><b>{@link #getNama()}</b> — di sini mengembalikan bentuk gabungan {@code code + "-" + nama}
 *       (lihat peringatan pada method tersebut), sedangkan {@link Dosen} mengembalikan nama bersih
 *       dengan cadangan dari cache berkas dan {@link Pegawai} mencerminkannya dari
 *       {@code Dosen}/{@code Guru} terkait.</li>
 *   <li><b>Nilai awal {@code tetap}</b> — {@code Karyawan} dan {@link Pegawai} memakai {@code 0},
 *       {@link Dosen} memakai {@code 1}.</li>
 *   <li><b>{@link #getEmail()}</b> — logika perapian koma disalin apa adanya ke kedua subclass;
 *       {@link Pegawai} menambahkan pencerminan dari {@code Dosen}/{@code Guru} di ujungnya.</li>
 * </ul>
 * <p><b>Konsekuensi praktis:</b> mengubah kode di kelas ini <i>tidak</i> otomatis mengubah perilaku
 * {@link Dosen} maupun {@link Pegawai} — perbaikan harus diterapkan di ketiga tempat. Sebaliknya,
 * jangan menyimpulkan perilaku {@code Dosen}/{@code Pegawai} dari membaca kelas ini saja.</p>
 *
 * @see GeneralValueObject
 * @see Dosen
 * @see Pegawai
 */
public abstract class Karyawan extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java untuk keluarga kelas ini.
	 */
	private static final long serialVersionUID = -4458435422391064065L;

	/**
	 * Kode identitas utama karyawan (NIP/NIDN/nomor induk, tergantung turunan). Ikut membentuk
	 * nilai kembalian {@link #getNama()} di kelas ini. Dibayangi oleh {@link Dosen} dan
	 * {@link Pegawai}.
	 */
	private String code;

	/**
	 * Kode alternatif/internal karyawan, dipakai bila institusi memelihara penomoran kedua di
	 * samping {@link #code}. Dibayangi oleh {@link Dosen} dan {@link Pegawai}.
	 */
	private String mycode;

	/**
	 * Nama lengkap karyawan. Perhatikan bahwa {@link #getNama()} <b>tidak</b> mengembalikan field
	 * ini apa adanya. Dibayangi oleh {@link Dosen} dan {@link Pegawai}.
	 */
	private String nama;

	/**
	 * Alamat tempat tinggal dalam bentuk teks bebas. Dibayangi oleh {@link Dosen} dan
	 * {@link Pegawai}.
	 */
	private String alamat;

	/**
	 * Daftar alamat surel dipisah koma — satu kolom menampung banyak alamat, bukan satu alamat
	 * saja. Dirapikan sewaktu dibaca oleh {@link #getEmail()} dan ditambah satu per satu oleh
	 * {@link #appendEmail(String)}. Dibayangi oleh {@link Dosen} dan {@link Pegawai}.
	 */
	private String email;

	/**
	 * Nomor telepon/ponsel karyawan. Dibayangi oleh {@link Dosen} dan {@link Pegawai}.
	 */
	private String telp;

	/**
	 * Jenis kelamin karyawan, disimpan sebagai teks bebas (bukan enum), sehingga penulisannya
	 * bergantung pada sumber data. Dibayangi oleh {@link Dosen} dan {@link Pegawai}.
	 */
	private String kelamin;

	/**
	 * Kota/tempat kelahiran, dipasangkan dengan {@link #tanggallahir}. Dibayangi oleh {@link Dosen}
	 * dan {@link Pegawai}.
	 */
	private String tempatlahir;

	/**
	 * Pangkat kepegawaian. Dibayangi oleh {@link Dosen} dan {@link Pegawai}.
	 */
	private String pangkat;

	/**
	 * Golongan kepegawaian, biasanya berpasangan dengan {@link #pangkat}. Dibayangi oleh
	 * {@link Dosen} dan {@link Pegawai}.
	 */
	private String golongan;

	/**
	 * Jabatan struktural/fungsional yang diemban. Dibayangi oleh {@link Dosen} dan
	 * {@link Pegawai}.
	 */
	private String jabatan;

	/**
	 * Bidang keahlian utama. Keahlian disimpan sebagai tiga kolom terpisah
	 * ({@link #spesialisasi1}, {@link #spesialisasi2}, {@link #spesialisasi3}) alih-alih relasi
	 * satu-ke-banyak, sehingga jumlah keahlian yang dapat dicatat dibatasi tiga. Dibayangi oleh
	 * {@link Dosen} dan {@link Pegawai}.
	 */
	private String spesialisasi1;

	/**
	 * Bidang keahlian kedua. Dibayangi oleh {@link Dosen} dan {@link Pegawai}.
	 *
	 * @see #spesialisasi1
	 */
	private String spesialisasi2;

	/**
	 * Bidang keahlian ketiga sekaligus yang terakhir yang dapat disimpan. Dibayangi oleh
	 * {@link Dosen} dan {@link Pegawai}.
	 *
	 * @see #spesialisasi1
	 */
	private String spesialisasi3;

	/**
	 * Tanggal lahir karyawan, dipasangkan dengan {@link #tempatlahir}. Dibayangi oleh {@link Dosen}
	 * dan {@link Pegawai}.
	 */
	private Date tanggallahir;

	/**
	 * Jurusan tempat karyawan bernaung.
	 *
	 * <p><b>Salah satu dari hanya dua field kelas ini yang tetap hidup:</b> {@link Dosen}
	 * membayanginya, tetapi {@link Pegawai} tidak — jadi untuk objek {@code Pegawai} nilai inilah
	 * yang benar-benar dibaca lewat {@link #getJurusan()}.</p>
	 */
	private Jurusan jurusan;

	/**
	 * Fakultas tempat karyawan bernaung, satu tingkat di atas {@link #jurusan}.
	 *
	 * <p>Sama seperti {@link #jurusan}: dibayangi {@link Dosen} tetapi tidak oleh {@link Pegawai},
	 * sehingga tetap menjadi penyimpan nyata bagi objek {@code Pegawai}.</p>
	 */
	private Fakultas fakultas;

	/**
	 * Penanda status kepegawaian tetap, dipakai sebagai flag numerik ({@code 0} = tidak tetap /
	 * kontrak, {@code 1} = tetap) alih-alih {@code Boolean}.
	 *
	 * <p><b>Nilai awal berbeda antar turunan:</b> di sini dan di {@link Pegawai} default-nya
	 * {@code 0}, sedangkan {@link Dosen} memakai {@code 1} — dosen dianggap tetap kecuali
	 * dinyatakan lain.</p>
	 */
	private Integer tetap = 0;

	/**
	 * Identitas karyawan pada mesin sidik jari/absensi, dipakai untuk mencocokkan rekaman kehadiran
	 * dengan data kepegawaian. Dibayangi oleh {@link Dosen} dan {@link Pegawai}.
	 */
	private String idfinger;

	/**
	 * Mengembalikan kode identitas utama karyawan apa adanya.
	 *
	 * @return kode identitas, atau {@code null} bila belum diisi
	 * @see #code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Menetapkan kode identitas utama karyawan tanpa validasi maupun normalisasi.
	 *
	 * @param code kode identitas baru
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * Mengembalikan kode alternatif/internal karyawan apa adanya.
	 *
	 * @return kode alternatif, atau {@code null} bila belum diisi
	 * @see #mycode
	 */
	public String getMycode() {
		return mycode;
	}

	/**
	 * Menetapkan kode alternatif/internal karyawan tanpa validasi.
	 *
	 * @param mycode kode alternatif baru
	 */
	public void setMycode(String mycode) {
		this.mycode = mycode;
	}

	/**
	 * Mengembalikan <b>gabungan</b> {@code code + "-" + nama}, bukan nama saja.
	 *
	 * <p><b>Peringatan — getter ini tidak simetris dengan {@link #setNama(String)}.</b> Nilai yang
	 * disetel lewat {@code setNama("Budi")} akan terbaca kembali sebagai {@code "12345-Budi"}.
	 * Bentuk gabungan ini praktis untuk label pilihan di layar, tetapi berbahaya bila hasilnya
	 * dipakai untuk pencocokan nama, pencarian, atau disimpan ulang ke kolom {@code nama} — pola
	 * <i>baca lalu simpan lagi</i> akan menumpuk prefiks kode berulang kali.</p>
	 *
	 * <p>Tidak ada penjagaan {@code null}: bila {@code code} atau {@code nama} kosong, hasilnya
	 * memuat literal {@code "null"} (misalnya {@code "null-Budi"}).</p>
	 *
	 * <p>Kedua subclass meng-override method ini dengan perilaku yang sepenuhnya berbeda dan
	 * <b>tidak</b> menambahkan prefiks kode: {@link Dosen#getNama()} mengembalikan nama bersih
	 * (dengan cadangan dari cache berkas), {@link Pegawai#getNama()} mencerminkan nama dari
	 * {@code Dosen}/{@code Guru} terkait. Karena {@code Dosen} dan {@code Pegawai} adalah
	 * satu-satunya turunan, bentuk gabungan ini praktis tidak pernah terlihat saat runtime.</p>
	 *
	 * @return string {@code "<code>-<nama>"}
	 */
	public String getNama() {
		return code + "-" + nama;
	}

	/**
	 * Menetapkan nama lengkap karyawan pada field mentah, tanpa trim maupun validasi.
	 *
	 * <p>Perhatikan asimetri dengan {@link #getNama()}: nilai yang disetel di sini akan terbaca
	 * kembali dalam bentuk gabungan berprefiks kode.</p>
	 *
	 * @param nama nama lengkap karyawan
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan alamat tempat tinggal karyawan apa adanya.
	 *
	 * @return alamat teks bebas, atau {@code null} bila belum diisi
	 */
	public String getAlamat() {
		return alamat;
	}

	/**
	 * Menetapkan alamat tempat tinggal karyawan tanpa validasi format.
	 *
	 * @param alamat alamat baru dalam bentuk teks bebas
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Mengembalikan daftar alamat surel karyawan (dipisah koma) setelah <b>merapikannya</b>.
	 *
	 * <p><b>Peringatan — getter ini mengubah state.</b> Method ini menulis balik ke field
	 * {@link #email}, bukan sekadar membacanya: koma ganda {@code ",,"} dimampatkan (maksimal lima
	 * iterasi), nilai {@code null} diganti string kosong, dan nilai yang hanya berisi {@code ","}
	 * dikosongkan. Pada objek yang sedang dikelola Hibernate, <i>sekadar membaca</i> properti ini
	 * dapat menandai entity sebagai kotor dan memicu {@code UPDATE} beserta revisi audit palsu.
	 * Ini satu instance dari pola getter-mutasi-field yang tersebar luas pada model AIS.</p>
	 *
	 * <p>Batas lima iterasi juga membuat perapian tidak tuntas: rentetan koma yang sangat panjang
	 * bisa menyisakan {@code ",,"}. Perapian pun hanya menyentuh koma — spasi di sekitar alamat
	 * maupun alamat duplikat tidak dibersihkan di sini.</p>
	 *
	 * <p>Anotasi {@code @Column} di bawah ini <b>tidak aktif</b> karena {@code Karyawan} bukan
	 * kelas terpetakan Hibernate; pemetaan sebenarnya berada pada override di {@link Dosen} dan
	 * {@link Pegawai}, yang menyalin logika perapian ini apa adanya.</p>
	 *
	 * @return daftar surel dipisah koma; string kosong bila tidak ada (tidak pernah {@code null}
	 *         setelah pemanggilan pertama)
	 * @see #appendEmail(String)
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

	/**
	 * Mengganti <b>seluruh</b> daftar surel dengan nilai baru, tanpa validasi apa pun.
	 *
	 * <p>Berbeda dengan {@link #appendEmail(String)} yang menambah satu alamat, method ini menimpa
	 * daftar yang sudah ada. String kosong maupun {@code null} diterima apa adanya dan akan
	 * menghapus seluruh alamat yang tersimpan.</p>
	 *
	 * @param email satu alamat surel, atau beberapa alamat dipisah koma
	 * @see #appendEmail(String)
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Menambahkan satu alamat surel ke daftar tanpa menghapus alamat yang sudah ada.
	 *
	 * <p>Ada tiga penjagaan sebelum alamat benar-benar ditambahkan:</p>
	 * <ol>
	 *   <li><b>Uji duplikat.</b> Bila alamat sudah termuat dalam daftar, panggilan diabaikan.
	 *       Perhatikan bahwa ujinya memakai {@code StringUtils.contains} (substring), bukan
	 *       pencocokan per elemen — sehingga {@code "adi@x.com"} akan dianggap sudah ada jika
	 *       daftar memuat {@code "wahyuadi@x.com"}, dan alamat itu diam-diam tidak jadi
	 *       ditambahkan.</li>
	 *   <li><b>Uji format.</b> Alamat harus lolos {@code Common.isValidEmailAddress()}.</li>
	 *   <li><b>Uji domain telanjang.</b> Alamat yang diawali {@code "@"} ditolak.</li>
	 * </ol>
	 * <p>Penggabungan memakai pemisah koma; bila daftar masih kosong alamat dipasang sebagai satu-
	 * satunya isi. Seluruh penolakan bersifat senyap — tidak ada nilai kembalian maupun exception,
	 * jadi pemanggil tidak dapat membedakan alamat yang berhasil ditambahkan dari yang ditolak.</p>
	 *
	 * <p>Catatan: penjagaan duplikat pada langkah pertama hanya berjalan bila {@code this.email}
	 * sudah tidak {@code null}. Karena {@link #getEmail()} mengubah {@code null} menjadi string
	 * kosong, urutan pemanggilan getter/appender ikut menentukan cabang mana yang dipakai.</p>
	 *
	 * @param email alamat surel yang hendak ditambahkan; diabaikan bila {@code null}, kosong, tidak
	 *              valid, diawali {@code "@"}, atau sudah termuat sebagai substring
	 * @see #setEmail(String)
	 * @see #getEmail()
	 */
	public void appendEmail(String email) {
		if (this.email != null && email != null && !email.trim().isEmpty() && StringUtils.contains(this.email, email)) {
			return;
		}
		if (email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email) && !email.startsWith("@")) {
			this.email = this.email == null || this.email.trim().isEmpty() ? email : this.email + "," + email;
		}
	}

	/**
	 * Mengembalikan nomor telepon/ponsel karyawan apa adanya.
	 *
	 * @return nomor telepon, atau {@code null} bila belum diisi
	 */
	public String getTelp() {
		return telp;
	}

	/**
	 * Menetapkan nomor telepon/ponsel karyawan tanpa normalisasi format nomor.
	 *
	 * @param telp nomor telepon baru
	 */
	public void setTelp(String telp) {
		this.telp = telp;
	}

	/**
	 * Mengembalikan jenis kelamin karyawan sebagai teks bebas.
	 *
	 * @return jenis kelamin dalam bentuk apa pun yang tersimpan, atau {@code null} bila belum
	 *         diisi
	 * @see #kelamin
	 */
	public String getKelamin() {
		return kelamin;
	}

	/**
	 * Menetapkan jenis kelamin karyawan. Tidak ada pembatasan nilai — pemanggil bertanggung jawab
	 * menjaga konsistensi penulisan.
	 *
	 * @param kelamin jenis kelamin baru
	 */
	public void setKelamin(String kelamin) {
		this.kelamin = kelamin;
	}

	/**
	 * Mengembalikan kota/tempat kelahiran karyawan.
	 *
	 * @return tempat lahir, atau {@code null} bila belum diisi
	 * @see #getTanggallahir()
	 */
	public String getTempatlahir() {
		return tempatlahir;
	}

	/**
	 * Menetapkan kota/tempat kelahiran karyawan.
	 *
	 * @param tempatlahir tempat lahir baru
	 */
	public void setTempatlahir(String tempatlahir) {
		this.tempatlahir = tempatlahir;
	}

	/**
	 * Mengembalikan pangkat kepegawaian karyawan.
	 *
	 * @return pangkat, atau {@code null} bila belum diisi
	 * @see #getGolongan()
	 */
	public String getPangkat() {
		return pangkat;
	}

	/**
	 * Menetapkan pangkat kepegawaian karyawan.
	 *
	 * @param pangkat pangkat baru
	 */
	public void setPangkat(String pangkat) {
		this.pangkat = pangkat;
	}

	/**
	 * Mengembalikan golongan kepegawaian karyawan.
	 *
	 * @return golongan, atau {@code null} bila belum diisi
	 * @see #getPangkat()
	 */
	public String getGolongan() {
		return golongan;
	}

	/**
	 * Menetapkan golongan kepegawaian karyawan.
	 *
	 * @param golongan golongan baru
	 */
	public void setGolongan(String golongan) {
		this.golongan = golongan;
	}

	/**
	 * Mengembalikan jabatan struktural/fungsional karyawan.
	 *
	 * @return jabatan, atau {@code null} bila belum diisi
	 */
	public String getJabatan() {
		return jabatan;
	}

	/**
	 * Menetapkan jabatan struktural/fungsional karyawan.
	 *
	 * @param jabatan jabatan baru
	 */
	public void setJabatan(String jabatan) {
		this.jabatan = jabatan;
	}

	/**
	 * Mengembalikan bidang keahlian utama karyawan.
	 *
	 * @return spesialisasi pertama, atau {@code null} bila belum diisi
	 * @see #spesialisasi1
	 */
	public String getSpesialisasi1() {
		return spesialisasi1;
	}

	/**
	 * Menetapkan bidang keahlian utama karyawan.
	 *
	 * @param spesialisasi1 spesialisasi pertama
	 */
	public void setSpesialisasi1(String spesialisasi1) {
		this.spesialisasi1 = spesialisasi1;
	}

	/**
	 * Mengembalikan bidang keahlian kedua karyawan.
	 *
	 * @return spesialisasi kedua, atau {@code null} bila belum diisi
	 * @see #spesialisasi1
	 */
	public String getSpesialisasi2() {
		return spesialisasi2;
	}

	/**
	 * Menetapkan bidang keahlian kedua karyawan.
	 *
	 * @param spesialisasi2 spesialisasi kedua
	 */
	public void setSpesialisasi2(String spesialisasi2) {
		this.spesialisasi2 = spesialisasi2;
	}

	/**
	 * Mengembalikan bidang keahlian ketiga karyawan.
	 *
	 * @return spesialisasi ketiga, atau {@code null} bila belum diisi
	 * @see #spesialisasi1
	 */
	public String getSpesialisasi3() {
		return spesialisasi3;
	}

	/**
	 * Menetapkan bidang keahlian ketiga — sekaligus yang terakhir yang dapat ditampung model ini.
	 *
	 * @param spesialisasi3 spesialisasi ketiga
	 */
	public void setSpesialisasi3(String spesialisasi3) {
		this.spesialisasi3 = spesialisasi3;
	}

	/**
	 * Mengembalikan tanggal lahir karyawan.
	 *
	 * <p>Objek {@link Date} dikembalikan sebagai referensi langsung (tidak disalin), jadi pemanggil
	 * yang memanggil {@code setTime()} pada hasilnya akan ikut mengubah state entity ini.</p>
	 *
	 * @return tanggal lahir, atau {@code null} bila belum diisi
	 * @see #getTempatlahir()
	 */
	public Date getTanggallahir() {
		return tanggallahir;
	}

	/**
	 * Menetapkan tanggal lahir karyawan. Referensi disimpan langsung tanpa penyalinan defensif.
	 *
	 * @param tanggallahir tanggal lahir baru
	 */
	public void setTanggallahir(Date tanggallahir) {
		this.tanggallahir = tanggallahir;
	}

	/**
	 * Mengembalikan jurusan tempat karyawan bernaung.
	 *
	 * <p><b>Salah satu dari dua accessor kelas ini yang benar-benar terpakai saat runtime:</b>
	 * {@link Dosen} meng-override-nya, tetapi {@link Pegawai} tidak — sehingga untuk objek
	 * {@code Pegawai} method inilah yang dipanggil dan field {@link #jurusan} milik
	 * {@code Karyawan} yang dibaca.</p>
	 *
	 * <p>Berbeda dengan accessor relasi pada entity terpetakan di keluarga model ini, di sini
	 * <b>tidak</b> ada pemanggilan {@code check(...)} untuk meresolusi proxy lazy, karena kelas ini
	 * bukan kelas terpetakan Hibernate.</p>
	 *
	 * @return jurusan penempatan, atau {@code null} bila belum diisi
	 * @see #getFakultas()
	 */
	public Jurusan getJurusan() {
		return jurusan;
	}

	/**
	 * Menetapkan jurusan tempat karyawan bernaung.
	 *
	 * <p>Tidak ada pemeriksaan konsistensi terhadap {@link #setFakultas(Fakultas)} — memasang
	 * jurusan yang tidak bernaung di bawah fakultas yang tersimpan tidak akan ditolak.</p>
	 *
	 * @param jurusan jurusan penempatan baru
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan fakultas tempat karyawan bernaung.
	 *
	 * <p>Seperti {@link #getJurusan()}, ini salah satu dari dua accessor kelas ini yang masih hidup
	 * karena {@link Pegawai} tidak meng-override-nya.</p>
	 *
	 * @return fakultas penempatan, atau {@code null} bila belum diisi
	 * @see #getJurusan()
	 */
	public Fakultas getFakultas() {
		return fakultas;
	}

	/**
	 * Menetapkan fakultas tempat karyawan bernaung, tanpa pemeriksaan konsistensi terhadap
	 * {@link #setJurusan(Jurusan)}.
	 *
	 * @param fakultas fakultas penempatan baru
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Mengembalikan penanda status kepegawaian tetap sebagai flag numerik.
	 *
	 * <p>Nilai awalnya {@code 0} di kelas ini, tetapi {@link Dosen} membayangi field-nya dengan
	 * default {@code 1}. Karena bertipe {@code Integer} dan bukan {@code boolean}, nilai selain
	 * {@code 0}/{@code 1} — termasuk {@code null} pada objek yang dimuat dari basis data — secara
	 * teknis mungkin; bandingkan dengan {@code Integer.valueOf(1)} atau lakukan penjagaan
	 * {@code null} sebelum meng-unbox.</p>
	 *
	 * @return {@code 1} bila pegawai tetap, {@code 0} bila tidak; dapat {@code null}
	 * @see #tetap
	 */
	public Integer getTetap() {
		return tetap;
	}

	/**
	 * Menetapkan penanda status kepegawaian tetap. Nilai tidak divalidasi terhadap rentang
	 * {@code 0}/{@code 1}.
	 *
	 * @param tetap {@code 1} untuk pegawai tetap, {@code 0} untuk tidak tetap
	 */
	public void setTetap(Integer tetap) {
		this.tetap = tetap;
	}

	/**
	 * Mengembalikan identitas karyawan pada mesin sidik jari/absensi.
	 *
	 * @return id mesin absensi, atau {@code null} bila karyawan belum terdaftar di mesin
	 * @see #idfinger
	 */
	public String getIdfinger() {
		return idfinger;
	}

	/**
	 * Menetapkan identitas karyawan pada mesin sidik jari/absensi.
	 *
	 * <p>Tidak ada penjagaan keunikan di lapisan model: dua karyawan dapat diberi id mesin yang
	 * sama, dan rekaman kehadiran untuk id tersebut menjadi ambigu. Keunikan harus dijaga di
	 * lapisan aksi atau lewat batasan basis data.</p>
	 *
	 * @param idfinger id mesin absensi baru
	 */
	public void setIdfinger(String idfinger) {
		this.idfinger = idfinger;
	}

}
