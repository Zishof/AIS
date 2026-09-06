package ais.database.model;

import java.io.Serializable;

/**
 * Kelas dasar value object/model AIS untuk common vo. Tipe ini menyatukan identitas, metadata
 * umum, representasi, serta perilaku lintas entity yang benar-benar berlaku bagi turunannya.
 *
 * <p><b>Batas tanggung jawab:</b> interface serialisasi/perbandingan hanya mendukung binding dan collection.
 * Tipe ini tetap merupakan pembawa data; validasi, transaksi, dan aturan domain harus berada pada service agar
 * tidak muncul sumber aturan paralel.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code String id}, {@code String name}, {@code
 * String name1}, {@code String name2}, {@code String name3}, {@code String name4}, {@code String name5}, {@code
 * Double mulai}; pembacaan/pencarian ({@code getId()}, {@code getName()}, {@code getName1()}, {@code
 * getName2()}, {@code getName3()}, {@code getName4()}); mutasi data ({@code setId()}, {@code setName()}, {@code
 * setName1()}, {@code setName2()}, {@code setName3()}, {@code setName4()}); operasi domain lain ({@code
 * toString()}, {@code compareTo()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
 * <p><b>Efek samping:</b> accessor, mutator, dan pembanding hanya membaca atau mengubah state value object di
 * memori. Tipe ini tidak membuka session, menjalankan transaksi, atau memuat data sendiri.</p>
 *
 * <h3>Posisi dalam hierarki model AIS</h3>
 * <p>Meski namanya mengandung "VO" seperti {@link VOMahasiswa} dan {@link VOPembelajaran}, kelas ini
 * <b>tidak</b> berada dalam keluarga entity {@code DataUtil → GeneralValueObject → DataSop →
 * VoKunci}. Ia berdiri sendiri, tidak dipetakan Hibernate, tidak punya subclass sama sekali di
 * seluruh basis kode, dan tidak punya lifecycle audit/Envers. Yang menghubungkannya dengan dunia
 * entity hanyalah satu slot opsional {@link #getValueObject()} yang boleh diisi entity sumber.</p>
 *
 * <h3>Bentuk data: slot generik, bukan field bernama</h3>
 * <p>CommonVO adalah "baris tabel serbaguna": satu kunci ({@code id}), enam slot teks
 * ({@code name} sampai {@code name5}), empat slot angka ({@code mulai}, {@code sampai},
 * {@code nilai}, {@code maksimal}), tiga penanda ({@code persen}, {@code masing}, {@code dibagi}),
 * satu nomor urut, dan satu slot entity. Arti setiap slot ditentukan sepenuhnya oleh kode
 * pemanggil — tidak ada di dalam kelas ini yang bisa memberitahu apakah {@code name2} berisi nama
 * program studi, kode akun, atau tanggal terformat. Karena itu setiap pembaca kode di sisi
 * pemanggil wajib menelusuri tempat pembuatannya untuk memahami isinya; kelas ini dipakai di
 * ratusan titik (dasbor, laporan BKD, ekspor Feeder, model pohon penggajian, resource
 * perpustakaan, dan sebagainya) dengan makna slot yang berbeda-beda di tiap tempat.</p>
 *
 * <h3>Dua ketidakserasian yang perlu diketahui sebelum memakainya</h3>
 * <ol>
 * <li><b>Normalisasi {@code null} tidak seragam.</b> {@link #getName3()} dan {@link #getName5()}
 * mengubah {@code null} menjadi string kosong (yang terakhir juga memangkas spasi), sedangkan
 * {@link #getName()}, {@link #getName1()}, {@link #getName2()}, dan {@link #getName4()}
 * mengembalikan nilai mentah. Kode tampilan yang menganggap seluruh keluarga {@code getNameN()}
 * bebas {@code null} akan tetap kebobolan pada slot-slot yang tidak dinormalkan. Semua normalisasi
 * di kelas ini dilakukan hanya-baca lewat ekspresi ternari, jadi kelas ini bebas dari pola
 * getter-yang-menulis-field yang tersebar luas di entity AIS lain.</li>
 * <li><b>{@code compareTo} tidak konsisten dengan {@code equals}.</b> Kelas ini meng-override
 * {@link #compareTo(CommonVO)} tetapi tidak {@link #equals(Object)} maupun
 * {@link #hashCode()}. Akibatnya perilaku koleksi terbelah: {@link java.util.HashSet} memakai
 * identitas objek (dua baris dengan isi identik dianggap dua elemen), sementara
 * {@link java.util.TreeSet} memakai {@code compareTo} (dua baris dengan isi identik dianggap satu
 * elemen dan yang kedua senyap terbuang). Rincian lengkap ada pada dokumentasi
 * {@link #compareTo(CommonVO)}.</li>
 * </ol>
 *
 * @see GeneralValueObject
 */
public class CommonVO implements Serializable, Comparable<CommonVO> {

	/**
	 * Versi serialisasi kelas ini.
	 *
	 * <p>Nilainya dipatok eksplisit agar instance yang tersimpan di session HTTP (atau di replikasi
	 * session antar-node) tetap dapat dibaca setelah penambahan slot baru. Perhatikan bahwa
	 * jaminan itu hanya berlaku untuk field-field CommonVO sendiri; bila slot
	 * {@link #getValueObject()} terisi, kompatibilitas serialisasinya bergantung pada entity yang
	 * dititipkan di sana, bukan pada konstanta ini.</p>
	 */
	private static final long serialVersionUID = -8737675485624100335L;

	/**
	 * Membentuk baris kosong: seluruh slot teks {@code null}, {@code mulai}/{@code sampai}/
	 * {@code nilai} bernilai 0.0, {@code maksimal} bernilai {@link Double#MAX_VALUE}, dan
	 * ketiga penanda ({@code persen}, {@code masing}, {@code dibagi}) bernilai {@code null} yang
	 * akan dibaca sebagai {@code false}.
	 *
	 * <p>Dipakai oleh kode yang membangun baris secara bertahap lewat setter, mis. saat jumlah slot
	 * yang perlu diisi tidak cocok dengan satu pun overload konstruktor yang tersedia.</p>
	 */
	public CommonVO() {
	}

	/**
	 * Membentuk pasangan kunci-label paling dasar: {@code id} sebagai nilai yang dikirim balik ke
	 * server dan {@code name} sebagai teks yang dibaca pengguna.
	 *
	 * <p>Seluruh slot lain ({@code name1}..{@code name5}, {@code mulai}, {@code sampai},
	 * {@code nilai}, {@code maksimal}, {@code persen}, {@code masing}, {@code dibagi},
	 * {@code nomorUrut}, {@code valueObject}) tetap pada nilai default field-nya. Perhatikan bahwa
	 * {@code nomorUrut} TIDAK diisi oleh konstruktor mana pun sehingga
	 * {@link #getNomorUrut()} akan mengembalikan 1 untuk setiap instance hasil konstruktor; lihat
	 * catatan pada {@link #compareTo(CommonVO)} mengenai konsekuensinya terhadap pengurutan.</p>
	 *
	 * @param id   nilai kunci; boleh {@code null}, tetapi {@link #compareTo(CommonVO)} menelan
	 *             {@code NullPointerException} yang timbul karenanya sehingga urutan menjadi
	 *             "sama" alih-alih gagal
	 * @param name label yang ditampilkan; boleh {@code null} dengan konsekuensi serupa
	 */
	public CommonVO(String id, String name) {
		this.id = id;
		this.name = name;
	}

	/**
	 * Membentuk pasangan kunci-label sekaligus menyimpan entity sumbernya pada slot
	 * {@link #getValueObject()}.
	 *
	 * <p>Slot {@code valueObject} adalah "pintu darurat" arsitektural: pemanggil yang butuh lebih
	 * dari sekadar id dan label (mis. renderer grid yang ingin memanggil method domain pada entity
	 * asli) menyimpan referensi entity di sini alih-alih menambah field baru pada CommonVO. Karena
	 * CommonVO menyatakan diri {@link java.io.Serializable} sementara isi slot ini adalah entity
	 * Hibernate, hasil serialisasinya ikut menyeret graf entity beserta proxy lazy-loading-nya;
	 * jangan menaruh instance CommonVO ber-{@code valueObject} ke dalam session HTTP atau cache
	 * yang benar-benar diserialisasi tanpa memastikan entity tersebut sudah ter-detach dan
	 * ter-inisialisasi penuh.</p>
	 *
	 * @param id          nilai kunci
	 * @param name        label yang ditampilkan
	 * @param valueObject entity sumber yang ingin dibawa serta; boleh {@code null}
	 */
	public CommonVO(String id, String name, GeneralValueObject valueObject) {
		this.id = id;
		this.name = name;
		this.valueObject = valueObject;
	}

	/**
	 * Varian {@link #CommonVO(String, String, GeneralValueObject)} yang sekaligus mengisi slot
	 * label kedua {@code name1}.
	 *
	 * <p>Urutan parameternya sengaja menempatkan {@code valueObject} di tengah, berbeda dari
	 * keluarga konstruktor {@code (id, name, name1, ...)} yang murni teks. Perbedaan urutan ini
	 * adalah sumber kekeliruan yang mudah terjadi saat memanggil: {@code new CommonVO(id, nama,
	 * entity, kolomKedua)} dan {@code new CommonVO(id, nama, kolomKedua, kolomKetiga)} sama-sama
	 * berjumlah empat argumen, dan kompilator hanya membedakannya lewat tipe argumen ketiga.</p>
	 *
	 * @param id          nilai kunci
	 * @param name        label utama
	 * @param valueObject entity sumber yang dibawa serta; boleh {@code null}
	 * @param name1       label tambahan pertama, dibaca lewat {@link #getName1()} yang TIDAK
	 *                    menormalkan {@code null} menjadi string kosong
	 */
	public CommonVO(String id, String name, GeneralValueObject valueObject, String name1) {
		this.id = id;
		this.name = name;
		this.valueObject = valueObject;
		this.name1 = name1;
	}

	/**
	 * Membentuk baris dengan kunci dan dua kolom teks.
	 *
	 * @param id    nilai kunci
	 * @param name  kolom teks pertama (label utama)
	 * @param name1 kolom teks kedua; dibaca apa adanya oleh {@link #getName1()}, termasuk bila
	 *              {@code null}
	 */
	public CommonVO(String id, String name, String name1) {
		this.id = id;
		this.name = name;
		this.name1 = name1;
	}

	/**
	 * Membentuk baris dengan kunci dan tiga kolom teks.
	 *
	 * @param id    nilai kunci
	 * @param name  kolom teks pertama
	 * @param name1 kolom teks kedua
	 * @param name2 kolom teks ketiga; dibaca apa adanya oleh {@link #getName2()}
	 */
	public CommonVO(String id, String name, String name1, String name2) {
		this.id = id;
		this.name = name;
		this.name1 = name1;
		this.name2 = name2;
	}

	/**
	 * Membentuk baris dengan kunci dan empat kolom teks.
	 *
	 * <p>Mulai dari slot keempat perilaku pembacaan berubah: {@link #getName3()} menormalkan
	 * {@code null} menjadi string kosong, sedangkan {@link #getName()}, {@link #getName1()}, dan
	 * {@link #getName2()} tidak. Jadi baris yang dibangun lewat konstruktor ini akan "aman" saat
	 * kolom keempat kosong, tetapi tetap bisa menampilkan {@code null} pada tiga kolom pertama.</p>
	 *
	 * @param id    nilai kunci
	 * @param name  kolom teks pertama
	 * @param name1 kolom teks kedua
	 * @param name2 kolom teks ketiga
	 * @param name3 kolom teks keempat; {@code null} dibaca sebagai {@code ""}
	 */
	public CommonVO(String id, String name, String name1, String name2, String name3) {
		this.id = id;
		this.name = name;
		this.name1 = name1;
		this.name2 = name2;
		this.name3 = name3;
	}

	// public CommonVO(String id, String name, String name1, String name2,
	// String name3, String name4) {
	// this.id = id;
	// this.name = name;
	// this.name1 = name1;
	// this.name2 = name2;
	// this.name3 = name3;
	// this.name4 = name4;
	// }

	/**
	 * Konstruktor terluas: kunci ditambah enam kolom teks ({@code name} sampai {@code name5}).
	 *
	 * <p><b>Efek samping pada pengurutan.</b> Mengisi {@code name5} lewat konstruktor ini
	 * MENGUBAH strategi perbandingan yang dipakai {@link #compareTo(CommonVO)}. Selama
	 * {@code name5} kosong, perbandingan berjalan bertingkat: {@code nomorUrut}, lalu {@code id},
	 * lalu {@code name}. Begitu {@code name5} terisi, seluruh perbandingan beralih menjadi
	 * perbandingan string atas gabungan {@code name5 + " " + nomorUrut}, sehingga urutan
	 * {@code nomorUrut} menjadi leksikografis (10 mendahului 9) dan {@code id}/{@code name} tidak
	 * lagi ikut menentukan. Slot {@code name5} karenanya berfungsi sebagai kunci pengelompokan
	 * (mis. nama kategori/header) dan bukan sekadar kolom tampilan.</p>
	 *
	 * <p>Perhatikan juga bahwa overload enam-argumen yang hanya sampai {@code name4} sengaja
	 * dinonaktifkan (dikomentari di bawah konstruktor {@code (id, name, name1, name2, name3)}),
	 * karena akan bertabrakan tanda tangan dengan overload lain; untuk mengisi {@code name4} tanpa
	 * {@code name5}, panggil konstruktor ini dengan {@code name5} berisi {@code null} atau string
	 * kosong sehingga strategi perbandingan bertingkat tetap dipakai.</p>
	 *
	 * @param id    nilai kunci
	 * @param name  kolom teks pertama
	 * @param name1 kolom teks kedua
	 * @param name2 kolom teks ketiga
	 * @param name3 kolom teks keempat; {@code null} dibaca sebagai {@code ""}
	 * @param name4 kolom teks kelima; dibaca apa adanya oleh {@link #getName4()}
	 * @param name5 kunci pengelompokan/pengurutan; {@code null} dibaca sebagai {@code ""} dan
	 *              di-{@code trim()} oleh {@link #getName5()}
	 */
	public CommonVO(String id, String name, String name1, String name2, String name3, String name4, String name5) {
		this.id = id;
		this.name = name;
		this.name1 = name1;
		this.name2 = name2;
		this.name3 = name3;
		this.name4 = name4;
		this.name5 = name5;
	}

	/**
	 * Nilai kunci baris ini — biasanya id primer entity sumber yang sudah diubah menjadi
	 * {@link String} agar cocok dengan komponen ZK yang bekerja dengan nilai teks.
	 *
	 * <p>Tidak ada jaminan keunikan yang dipaksakan oleh kelas ini; keunikan sepenuhnya menjadi
	 * tanggung jawab kode yang membangun daftar CommonVO. Karena {@link #equals(Object)} tidak
	 * di-override sementara {@link #compareTo(CommonVO)} ikut membandingkan field ini, dua
	 * instance dengan {@code id} sama tetap dianggap objek berbeda oleh {@link java.util.HashSet}
	 * namun dianggap duplikat oleh {@link java.util.TreeSet}.</p>
	 */
	private String id;
	/** Label utama baris ini; dibaca apa adanya (termasuk {@code null}) oleh {@link #getName()}. */
	private String name;
	/** Slot teks tambahan pertama; dibaca apa adanya oleh {@link #getName1()}. */
	private String name1;
	/** Slot teks tambahan kedua; dibaca apa adanya oleh {@link #getName2()}. */
	private String name2;
	/** Slot teks tambahan ketiga; {@link #getName3()} menormalkan {@code null} menjadi {@code ""}. */
	private String name3;
	/** Slot teks tambahan keempat; dibaca apa adanya oleh {@link #getName4()}. */
	private String name4;
	/**
	 * Slot teks tambahan kelima yang merangkap kunci pengelompokan: bila terisi, ia mengambil alih
	 * seluruh logika {@link #compareTo(CommonVO)} (lihat dokumentasi method tersebut).
	 * {@link #getName5()} menormalkan {@code null} menjadi {@code ""} dan memangkas spasi.
	 */
	private String name5;

	/**
	 * Batas bawah rentang numerik yang diwakili baris ini (mis. nilai minimum suatu grade, awal
	 * rentang usia, atau ambang bawah aturan). Default 0.0, bukan {@code null}, sehingga
	 * {@link #getMulai()} aman dipakai langsung dalam operasi aritmetika selama tidak ada pemanggil
	 * yang secara eksplisit menyetel {@code null} lewat {@link #setMulai(Double)}.
	 */
	private Double mulai = 0.0;
	/**
	 * Batas atas rentang numerik yang diwakili baris ini, berpasangan dengan {@link #mulai}.
	 * Default 0.0; kelas ini tidak memvalidasi bahwa {@code sampai >= mulai}.
	 */
	private Double sampai = 0.0;
	/**
	 * Plafon/nilai maksimum yang berlaku untuk baris ini. Berbeda dari {@link #mulai} dan
	 * {@link #sampai}, defaultnya adalah {@link Double#MAX_VALUE} sehingga artinya "tanpa batas"
	 * selama tidak disetel.
	 *
	 * <p>Konsekuensi praktisnya: kode yang menampilkan nilai ini apa adanya akan mencetak
	 * {@code 1.7976931348623157E308} untuk baris yang tidak pernah menyetel plafon. Pemanggil yang
	 * menampilkan {@code maksimal} ke layar perlu memeriksa sendiri apakah nilainya masih
	 * {@link Double#MAX_VALUE}.</p>
	 */
	private Double maksimal = Double.MAX_VALUE;
	/**
	 * Nilai/bobot yang melekat pada baris ini (mis. bobot komponen penilaian, nominal, atau
	 * persentase bila {@link #persen} bernilai benar). Default 0.0.
	 */
	private Double nilai = 0.0;
	/**
	 * Penanda bahwa {@link #nilai} harus dibaca sebagai persentase, bukan nominal absolut.
	 * Disimpan sebagai {@link Boolean} yang bisa {@code null}; {@link #getPersen()} menormalkan
	 * {@code null} menjadi {@code false} tanpa menulis balik ke field, sehingga pembacaan tidak
	 * mengotori state (berbeda dari pola getter-mutasi yang tersebar di banyak entity AIS lain).
	 */
	private Boolean persen;
	/**
	 * Penanda bahwa aturan yang diwakili baris ini berlaku "masing-masing" (per satuan/per orang)
	 * dan bukan sebagai satu nilai gabungan. {@link #getMasing()} menormalkan {@code null} menjadi
	 * {@code false} secara hanya-baca.
	 */
	private Boolean masing;
	/**
	 * Penanda bahwa {@link #nilai} perlu dibagi rata di antara sejumlah pihak/periode alih-alih
	 * dipakai utuh. {@link #getDibagi()} menormalkan {@code null} menjadi {@code false} secara
	 * hanya-baca.
	 */
	private Boolean dibagi;

	/**
	 * Nomor urut tampil baris ini dan kunci pengurutan utama pada {@link #compareTo(CommonVO)}.
	 *
	 * <p><b>Catatan penting:</b> tidak ada satu pun konstruktor yang mengisi field ini, sehingga
	 * setiap instance yang dibangun lewat konstruktor akan bernilai {@code null} sampai
	 * {@link #setNomorUrut(Integer)} dipanggil secara eksplisit. {@link #getNomorUrut()}
	 * menormalkan {@code null} menjadi <b>1</b> (bukan 0), jadi seluruh baris yang tidak pernah
	 * disetel akan berbagi nomor urut yang sama dan perbandingan otomatis jatuh ke tingkat
	 * berikutnya ({@code id}, lalu {@code name}).</p>
	 */
	private Integer nomorUrut;

	/**
	 * Referensi ke entity sumber yang diwakili baris ini, bila pemanggil membutuhkannya kembali.
	 *
	 * <p>Field ini adalah satu-satunya jembatan dari value object ringan ini ke dunia entity
	 * Hibernate ({@link GeneralValueObject}). Isinya tidak pernah dibaca atau divalidasi oleh
	 * CommonVO sendiri — kelas ini hanya menyimpan dan mengembalikannya. Karena entity yang
	 * disimpan bisa berupa proxy lazy, mengakses relasinya di luar session yang masih terbuka akan
	 * memicu {@code LazyInitializationException} pada pemanggil, bukan di sini.</p>
	 */
	private GeneralValueObject valueObject;

	/**
	 * Representasi teks ringkas untuk keperluan debug/log, berbentuk
	 * {@code name==>mulai-sampai-nilai-persen-masing}.
	 *
	 * <p>Perhatikan bahwa representasi ini <b>tidak</b> menyertakan {@link #id},
	 * {@link #nomorUrut}, {@link #maksimal}, {@link #dibagi}, maupun slot {@code name1}..
	 * {@code name5}. Artinya dua baris yang berbeda kunci tetapi sama label dan sama rentang akan
	 * tercetak identik di log; jangan memakai keluaran method ini sebagai identitas baris, dan
	 * jangan pula menjadikannya label tampilan (komponen ZK yang memanggil {@code toString()}
	 * secara implisit akan menampilkan tanda panah dan angka mentah kepada pengguna).</p>
	 *
	 * <p>Nilai {@code null} pada field mana pun akan tercetak sebagai teks {@code "null"} karena
	 * method ini membaca field secara langsung, bukan lewat getter yang menormalkan {@code null}.
	 * Khususnya {@code persen} dan {@code masing} akan tampil {@code "null"} di sini walaupun
	 * {@link #getPersen()}/{@link #getMasing()} akan melaporkannya sebagai {@code false}.</p>
	 *
	 * @return ringkasan label dan angka-angka utama baris ini
	 */
	public String toString() {
		return name + "==>" + mulai + "-" + sampai + "-" + nilai + "-" + persen + "-" + masing;
	}

	/**
	 * Mengembalikan nilai kunci baris ini apa adanya, termasuk {@code null}.
	 *
	 * @return nilai {@link #id}; bisa {@code null}
	 */
	public String getId() {
		return id;
	}

	/**
	 * Menyetel nilai kunci baris ini. Tidak ada normalisasi, pemangkasan spasi, maupun pemeriksaan
	 * keunikan terhadap baris lain.
	 *
	 * @param id nilai kunci baru; boleh {@code null}
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Mengembalikan label utama apa adanya, termasuk {@code null}.
	 *
	 * <p>Berbeda dari {@link #getName3()} dan {@link #getName5()} yang menormalkan {@code null},
	 * getter ini mengembalikan nilai mentah. {@link #compareTo(CommonVO)} memanggilnya di dalam
	 * blok {@code try} justru untuk menyerap {@link NullPointerException} yang bisa timbul di
	 * sini.</p>
	 *
	 * @return label utama; bisa {@code null}
	 */
	public String getName() {
		return name;
	}

	/**
	 * Menyetel label utama baris ini.
	 *
	 * @param name label baru; boleh {@code null}
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Mengembalikan slot teks tambahan pertama apa adanya, termasuk {@code null}.
	 *
	 * @return isi {@link #name1}; bisa {@code null}
	 */
	public String getName1() {
		return name1;
	}

	/**
	 * Menyetel slot teks tambahan pertama.
	 *
	 * @param name1 isi baru; boleh {@code null}
	 */
	public void setName1(String name1) {
		this.name1 = name1;
	}

	/**
	 * Mengembalikan slot teks tambahan kedua apa adanya, termasuk {@code null}.
	 *
	 * @return isi {@link #name2}; bisa {@code null}
	 */
	public String getName2() {
		return name2;
	}

	/**
	 * Menyetel slot teks tambahan kedua.
	 *
	 * @param name2 isi baru; boleh {@code null}
	 */
	public void setName2(String name2) {
		this.name2 = name2;
	}

	/**
	 * Mengembalikan slot teks tambahan ketiga dengan {@code null} dinormalkan menjadi string
	 * kosong.
	 *
	 * <p>Normalisasi dilakukan secara hanya-baca (ekspresi ternari) sehingga field {@link #name3}
	 * sendiri tidak ikut berubah — pembacaan tidak membuat objek ini "kotor" di mata pemanggil
	 * yang membandingkan state sebelum/sesudah. Berbeda dari {@link #getName5()}, nilai yang
	 * dikembalikan tidak di-{@code trim()}.</p>
	 *
	 * @return isi {@link #name3}, atau {@code ""} bila {@code null}; tidak pernah {@code null}
	 */
	public String getName3() {
		return name3 == null ? "" : name3;
	}

	/**
	 * Menyetel slot teks tambahan ketiga. Nilai {@code null} tetap disimpan sebagai {@code null};
	 * normalisasi hanya terjadi saat dibaca lewat {@link #getName3()}.
	 *
	 * @param name3 isi baru; boleh {@code null}
	 */
	public void setName3(String name3) {
		this.name3 = name3;
	}

	/**
	 * Mengembalikan slot teks tambahan keempat apa adanya, termasuk {@code null}.
	 *
	 * <p>Perhatikan ketidakserasian sengaja/tidak sengaja pada keluarga getter ini: slot ke-3 dan
	 * ke-5 dinormalkan, sedangkan slot ke-4 di antaranya tidak. Kode tampilan yang menganggap
	 * seluruh {@code getNameN()} bebas {@code null} akan tetap kebobolan di sini.</p>
	 *
	 * @return isi {@link #name4}; bisa {@code null}
	 */
	public String getName4() {
		return name4;
	}

	/**
	 * Menyetel slot teks tambahan keempat.
	 *
	 * @param name4 isi baru; boleh {@code null}
	 */
	public void setName4(String name4) {
		this.name4 = name4;
	}

	/**
	 * Mengembalikan kunci pengelompokan {@link #name5} dengan {@code null} dinormalkan menjadi
	 * string kosong dan spasi di kedua ujungnya dipangkas.
	 *
	 * <p>Getter ini dipanggil dua kali oleh {@link #compareTo(CommonVO)}: sekali untuk memutuskan
	 * strategi perbandingan (kosong atau tidak), sekali lagi untuk menyusun kunci gabungan. Karena
	 * pemangkasan hanya terjadi saat dibaca, nilai {@code "   "} (hanya spasi) yang tersimpan akan
	 * dianggap kosong oleh perbandingan meskipun field-nya sendiri tidak kosong.</p>
	 *
	 * @return isi {@link #name5} yang sudah dipangkas, atau {@code ""} bila {@code null}; tidak
	 *         pernah {@code null}
	 */
	public String getName5() {
		return name5 == null ? "" : name5.trim();
	}

	/**
	 * Menyetel kunci pengelompokan {@link #name5}.
	 *
	 * <p><b>Efek samping tak langsung:</b> mengisi nilai non-kosong di sini mengubah perilaku
	 * {@link #compareTo(CommonVO)} untuk instance ini. Bila objek ini sudah berada di dalam
	 * {@link java.util.TreeSet} atau {@link java.util.TreeMap}, memanggil setter ini merusak
	 * invarian struktur data tersebut (elemen menjadi tidak dapat ditemukan kembali). Setel
	 * sebelum memasukkan ke koleksi terurut, bukan sesudahnya.</p>
	 *
	 * @param name5 kunci pengelompokan baru; boleh {@code null}
	 */
	public void setName5(String name5) {
		this.name5 = name5;
	}

	/**
	 * Mengembalikan batas bawah rentang. Default field adalah 0.0, tetapi nilai {@code null} yang
	 * disetel secara eksplisit lewat {@link #setMulai(Double)} akan dikembalikan apa adanya.
	 *
	 * @return batas bawah rentang; umumnya non-{@code null}
	 */
	public Double getMulai() {
		return mulai;
	}

	/**
	 * Menyetel batas bawah rentang. Menyetel {@code null} di sini menghilangkan jaminan
	 * non-{@code null} yang diberikan nilai default field.
	 *
	 * @param mulai batas bawah baru
	 */
	public void setMulai(Double mulai) {
		this.mulai = mulai;
	}

	/**
	 * Mengembalikan batas atas rentang. Default field adalah 0.0.
	 *
	 * @return batas atas rentang; umumnya non-{@code null}
	 */
	public Double getSampai() {
		return sampai;
	}

	/**
	 * Menyetel batas atas rentang. Tidak ada validasi bahwa nilainya tidak lebih kecil dari
	 * {@link #getMulai()}.
	 *
	 * @param sampai batas atas baru
	 */
	public void setSampai(Double sampai) {
		this.sampai = sampai;
	}

	/**
	 * Mengembalikan nilai/bobot baris ini. Default field adalah 0.0. Apakah angka ini bermakna
	 * nominal atau persentase ditentukan oleh {@link #getPersen()}, bukan oleh getter ini.
	 *
	 * @return nilai/bobot; umumnya non-{@code null}
	 */
	public Double getNilai() {
		return nilai;
	}

	/**
	 * Menyetel nilai/bobot baris ini. Tidak ada pemeriksaan terhadap {@link #getMaksimal()}.
	 *
	 * @param nilai nilai/bobot baru
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Menyatakan apakah {@link #getNilai()} harus dibaca sebagai persentase.
	 *
	 * <p>Normalisasi {@code null} menjadi {@code false} dilakukan secara hanya-baca, tanpa menulis
	 * balik ke field. Konsekuensinya, "belum disetel" dan "disetel {@code false}" tidak dapat
	 * dibedakan lewat getter ini; bila perbedaan itu penting, baca field lewat jalur lain atau
	 * pakai penanda tersendiri.</p>
	 *
	 * @return {@code true} bila nilai adalah persentase; {@code false} bila bukan atau belum
	 *         disetel — tidak pernah {@code null}
	 */
	public Boolean getPersen() {
		return persen == null ? false : persen;
	}

	/**
	 * Menyetel penanda persentase.
	 *
	 * @param persen {@code true} bila {@link #getNilai()} adalah persentase; {@code null}
	 *               diperlakukan sama dengan {@code false} saat dibaca
	 */
	public void setPersen(Boolean persen) {
		this.persen = persen;
	}

	/**
	 * Menyatakan apakah aturan baris ini berlaku per satuan ("masing-masing") dan bukan sebagai
	 * satu nilai gabungan. Normalisasi {@code null} menjadi {@code false} bersifat hanya-baca.
	 *
	 * @return {@code true} bila berlaku per satuan; tidak pernah {@code null}
	 */
	public Boolean getMasing() {
		return masing == null ? false : masing;
	}

	/**
	 * Menyetel penanda berlaku-per-satuan.
	 *
	 * @param masing {@code true} bila aturan berlaku per satuan; {@code null} dibaca sebagai
	 *               {@code false}
	 */
	public void setMasing(Boolean masing) {
		this.masing = masing;
	}

	/**
	 * Mengembalikan plafon baris ini.
	 *
	 * <p>Nilai default field adalah {@link Double#MAX_VALUE} yang berarti "tanpa batas"; getter ini
	 * mengembalikannya apa adanya tanpa menerjemahkannya menjadi {@code null} atau nol. Kode yang
	 * menjumlahkan atau menampilkan nilai ini wajib memeriksa sendiri kasus tersebut, sebab
	 * menjumlahkan {@link Double#MAX_VALUE} akan menghasilkan {@code Infinity} alih-alih melempar
	 * kesalahan.</p>
	 *
	 * @return plafon; {@link Double#MAX_VALUE} bila tidak pernah disetel
	 */
	public Double getMaksimal() {
		return maksimal;
	}

	/**
	 * Menyetel plafon baris ini, menggantikan default "tanpa batas".
	 *
	 * @param maksimal plafon baru
	 */
	public void setMaksimal(Double maksimal) {
		this.maksimal = maksimal;
	}

	/**
	 * Menyatakan apakah {@link #getNilai()} perlu dibagi rata di antara sejumlah pihak/periode.
	 * Normalisasi {@code null} menjadi {@code false} bersifat hanya-baca.
	 *
	 * @return {@code true} bila nilai harus dibagi; tidak pernah {@code null}
	 */
	public Boolean getDibagi() {
		return dibagi == null ? false : dibagi;
	}

	/**
	 * Menyetel penanda pembagian nilai.
	 *
	 * @param dibagi {@code true} bila nilai harus dibagi rata; {@code null} dibaca sebagai
	 *               {@code false}
	 */
	public void setDibagi(Boolean dibagi) {
		this.dibagi = dibagi;
	}

	/**
	 * Mengembalikan entity sumber yang dibawa serta baris ini, bila ada.
	 *
	 * <p>Isi slot ini tidak pernah dimuat sendiri oleh CommonVO; ia hanya berisi apa yang
	 * dititipkan pemanggil lewat konstruktor atau {@link #setValueObject(GeneralValueObject)}.
	 * Nilai balik bisa berupa proxy Hibernate yang belum ter-inisialisasi, sehingga mengakses
	 * relasinya di luar session terbuka akan gagal pada pemanggil.</p>
	 *
	 * @return entity sumber; sering kali {@code null} karena mayoritas konstruktor tidak
	 *         mengisinya
	 */
	public GeneralValueObject getValueObject() {
		return valueObject;
	}

	/**
	 * Menitipkan entity sumber pada baris ini.
	 *
	 * @param valueObject entity sumber; boleh {@code null}. Menyimpan entity terkelola di sini
	 *                    berarti masa hidup objek tampilan ikut menahan graf entity tersebut;
	 *                    untuk daftar panjang, pertimbangkan menyimpan id saja pada {@link #id}
	 */
	public void setValueObject(GeneralValueObject valueObject) {
		this.valueObject = valueObject;
	}

	@Override
	public int compareTo(CommonVO o) {
		int compare = 0;
		if (getName5().trim().isEmpty()) {

			compare = getNomorUrut().compareTo(o.getNomorUrut());
			if (compare == 0) {
				try {
					compare = getId().compareTo(o.getId());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/CommonVO.java:230");

				}
			}
			if (compare == 0) {
				try {
					compare = getName().compareTo(o.getName());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/CommonVO.java:237");

				}
			}
		} else {
			compare = (getName5() + " " + getNomorUrut()).compareTo((o.getName5() + " " + o.getNomorUrut()));
		}
		return compare;
	}

	/**
	 * Mengembalikan nomor urut tampil baris ini, dengan {@code null} dinormalkan menjadi
	 * <b>1</b>.
	 *
	 * <p>Angka default 1 (bukan 0) dipilih agar baris yang belum diberi nomor tetap berada di
	 * kelompok pertama saat diurutkan. Karena tidak satu pun konstruktor mengisi field ini,
	 * default tersebut berlaku untuk hampir semua instance kecuali yang secara eksplisit
	 * memanggil {@link #setNomorUrut(Integer)}.</p>
	 *
	 * <p>Normalisasi dilakukan hanya-baca tanpa menulis balik ke field, sehingga pembacaan tidak
	 * mengubah state objek.</p>
	 *
	 * @return nomor urut; tidak pernah {@code null}
	 */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampil baris ini.
	 *
	 * <p><b>Efek samping tak langsung:</b> field ini adalah kunci pengurutan utama pada
	 * {@link #compareTo(CommonVO)}. Mengubahnya setelah objek dimasukkan ke {@link
	 * java.util.TreeSet}/{@link java.util.TreeMap} merusak invarian struktur data tersebut.</p>
	 *
	 * @param nomorUrut nomor urut baru; {@code null} dibaca kembali sebagai 1
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}
}
