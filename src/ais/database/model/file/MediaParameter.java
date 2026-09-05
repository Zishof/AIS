package ais.database.model.file;

/**
 * Objek parameter (bukan entitas Hibernate — POJO biasa, tidak beranotasi {@code @Entity}) yang
 * mendeskripsikan cara menampilkan/mengelola satu field foto/media milik suatu entitas lain,
 * dipakai oleh komponen media (lihat pemakaian di {@code ais.common.CommonMedia} dan kelas-kelas
 * {@code Foto*} pada paket {@link ais.database.model.file}, mis. {@code FotoSiswa},
 * {@code FotoPegawai}, {@code FotoDosen}, {@code FotoGuru}). Kombinasi {@link #getClazz()} +
 * {@link #getProperty()} menunjuk (lewat reflection) ke entitas dan properti target yang memuat
 * data foto, sementara {@link #getFilePropertyName()}/{@link #getMediaPropertyName()} menentukan
 * nama properti yang dipakai untuk menampilkan file fisik dan metadata medianya di UI.
 *
 * <h2>Ke mana isi objek ini bermuara</h2>
 * <p>Objek ini bukan sekadar wadah nilai: seluruh isinya dikemas menjadi satu objek JSON oleh
 * {@code CommonMedia.getMedia(MediaParameter)}, dienkripsi, lalu ditempelkan sebagai parameter
 * {@code d} pada alamat {@code /AmbilMedia?d=<token>}. Servlet {@code ais.action.servlet.AmbilMedia}
 * membongkarnya kembali di sisi penerima dan memakainya untuk mencari baris foto.</p>
 * <p>Dengan kata lain, <b>setiap field pada kelas ini adalah parameter pencarian yang akan
 * menyeberang ke sisi klien</b>. Itu perlu diingat saat menambah field baru di sini: apa pun yang
 * ditambahkan akan ikut menjadi bagian dari alamat yang dipegang peramban.</p>
 *
 * <h2>Hubungan dengan {@link FileFotoLain} dan servlet lampiran</h2>
 * <p>Kelas ini <b>tidak dipakai</b> oleh {@link FileFotoLain} maupun jalur lampiran
 * {@code /al} &mdash; keduanya jalur yang terpisah dan sejajar. Yang menghubungkan keduanya
 * adalah sisi penerima: {@code AmbilMedia} pada salah satu cabangnya memanggil
 * {@code FileFotoLain.ambil(usingId, value, jenis, myClass)}, sehingga nilai
 * {@link #getUsingId()} yang berangkat dari sini berakhir sebagai parameter {@code usingId} pada
 * mesin pencarian lampiran. Maknanya pun sama persis: mencocokkan acuan ke primary key baris foto
 * alih-alih ke kolom pemilik. Pahami {@code FileFotoLain.ambil(...)} lebih dahulu sebelum menyetel
 * {@link #setUsingId(Boolean)} menjadi {@code true}.</p>
 * <p><b>Perlu diketahui:</b> sebagaimana pada servlet lampiran, {@code AmbilMedia} membaca setiap
 * nilai dengan pola "ambil dari token terenkripsi bila ada, kalau tidak ambil dari parameter
 * permintaan biasa". Enkripsi token yang dilakukan {@code CommonMedia.getMedia(...)} karena itu
 * bukan kendali akses &mdash; ia membuat alamat rapi dan tidak mudah berubah tanpa sengaja, tetapi
 * tidak mencegah penyusunan permintaan sendiri dengan nilai pilihan siapa pun. Penjagaan yang
 * benar-benar membatasi akses harus berada di sisi penerima.</p>
 *
 * <h2>Field yang tidak wajib</h2>
 * <p>Hanya lima nilai pertama yang selalu terisi lewat konstruktor. {@link #getHeight()},
 * {@link #getWidth()}, {@link #getFotoUtama()}, {@link #getFotoId()}, dan {@link #getUsingId()}
 * bersifat pilihan dan hanya ikut dikemas bila memang bernilai &mdash; lihat rangkaian pemeriksaan
 * {@code != null} pada {@code CommonMedia.getMedia(...)}. Karena itu objek yang dibentuk lewat
 * konstruktor saja sudah sah dipakai; keempat field sisanya disetel belakangan bila diperlukan.</p>
 */
public class MediaParameter {

	/** Identitas/label baris data (biasanya hasil {@code toString()} entitas pemilik foto), dipakai untuk penamaan tampilan. */
	private String id;
	/** Nama properti pada entitas target yang menyimpan path/nama file. */
	private String filePropertyName;
	/** Nama properti pada entitas target yang menyimpan objek media terkait. */
	private String mediaPropertyName;
	/** Kelas entitas target yang memiliki field foto/media ini (dipakai untuk reflection). */
	private Class<?> clazz;
	/** Nama properti pada {@link #clazz} yang menjadi acuan pencarian foto (mis. relasi ke entitas induk). */
	private String property;
	/**
	 * Tinggi tampilan yang diminta, dalam piksel; {@code null} berarti tanpa batasan.
	 *
	 * <p>Hanya ikut dikemas ke dalam token bila bernilai. Nilainya adalah <i>permintaan</i>
	 * kepada sisi penerima, bukan ukuran berkas yang sesungguhnya &mdash; berkas aslinya
	 * tidak diubah oleh field ini.</p>
	 */
	private Integer height;
	/**
	 * Lebar tampilan yang diminta, dalam piksel; {@code null} berarti tanpa batasan.
	 *
	 * <p>Berlaku ketentuan yang sama seperti {@link #height}. Keduanya berdiri sendiri:
	 * mengisi salah satunya saja diperbolehkan, dan tidak ada perhitungan yang menjaga
	 * perbandingan sisi gambar tetap.</p>
	 */
	private Integer width;
	/** Menandakan foto ini adalah foto utama/profil (true) atau foto tambahan/lampiran (false). */
	private Boolean fotoUtama = false;
	/** ID foto spesifik yang dirujuk, dipakai bersama {@link #usingId}. */
	private Long fotoId = null;
	/** Menentukan apakah pencarian foto dilakukan berdasarkan {@link #fotoId} (true) atau berdasarkan entitas pemilik (false/default). */
	private Boolean usingId;

	/**
	 * Membentuk parameter media dengan lima nilai yang wajib ada, tanpa batasan dimensi
	 * tampilan.
	 *
	 * <p>Bentuk yang paling banyak dipakai; dijumpai antara lain pada
	 * {@code CommonMedia.getUrlFotoItem/getUrlFotoProduk/getUrlFotoKopSurat} serta pada
	 * {@code FotoTranskripAction} dan {@code SkripsiAction} yang menyusun foto mahasiswa.</p>
	 *
	 * <p><b>Tidak ada validasi apa pun di sini.</b> Nilai {@code null} diterima pada
	 * setiap parameter, termasuk {@code clazz} &mdash; padahal
	 * {@code CommonMedia.getMedia(...)} memanggil {@code getClazz().getName()} tanpa
	 * penjagaan, sehingga {@code clazz} bernilai {@code null} baru terlihat sebagai
	 * kegagalan jauh dari sini. Pastikan nilainya terisi di tempat pemanggilan.</p>
	 *
	 * @param id                identitas/label baris data pemilik foto
	 * @param filePropertyName  nama properti yang menyimpan path/nama berkas
	 * @param mediaPropertyName nama properti yang menyimpan objek media terkait
	 * @param clazz             kelas entitas target; dibaca lewat reflection di sisi penerima
	 * @param property          nama properti acuan pencarian foto pada {@code clazz}
	 */
	public MediaParameter(String id, String filePropertyName,
			String mediaPropertyName, Class<?> clazz, String property) {
		super();
		this.id = id;
		this.filePropertyName = filePropertyName;
		this.mediaPropertyName = mediaPropertyName;
		this.clazz = clazz;
		this.property = property;
	}

	/**
	 * Membentuk parameter media lengkap dengan permintaan dimensi tampilan.
	 *
	 * <p>Setara konstruktor lima parameter, ditambah {@code height} dan {@code width} yang
	 * akan ikut dikemas ke dalam token bila bernilai. Keduanya boleh {@code null} secara
	 * terpisah.</p>
	 *
	 * <p>Perhatikan urutan parameternya: <b>{@code height} mendahului {@code width}</b>,
	 * berlawanan dengan kebiasaan umum yang menyebut lebar lebih dahulu. Tertukarnya kedua
	 * nilai tidak akan tertangkap kompilator karena tipenya sama, dan akibatnya hanya
	 * terlihat sebagai gambar berukuran ganjil di layar.</p>
	 *
	 * @param id                identitas/label baris data pemilik foto
	 * @param filePropertyName  nama properti yang menyimpan path/nama berkas
	 * @param mediaPropertyName nama properti yang menyimpan objek media terkait
	 * @param clazz             kelas entitas target; dibaca lewat reflection di sisi penerima
	 * @param property          nama properti acuan pencarian foto pada {@code clazz}
	 * @param height            tinggi tampilan yang diminta dalam piksel; boleh {@code null}
	 * @param width             lebar tampilan yang diminta dalam piksel; boleh {@code null}
	 */
	public MediaParameter(String id, String filePropertyName,
			String mediaPropertyName, Class<?> clazz, String property,
			Integer height, Integer width) {
		super();
		this.id = id;
		this.filePropertyName = filePropertyName;
		this.mediaPropertyName = mediaPropertyName;
		this.clazz = clazz;
		this.property = property;
		this.height = height;
		this.width = width;
	}

	/**
	 * Identitas/label baris data pemilik foto.
	 *
	 * <p>Dikemas ke dalam token dengan kunci {@code "id"}. Nilainya biasanya hasil
	 * {@code toString()} entitas pemilik atau id-nya yang sudah dijadikan teks; bertipe
	 * {@code String} justru supaya dapat menampung keduanya, termasuk acuan bertipe teks
	 * seperti userid pada {@code FotoAdmin}.</p>
	 *
	 * @return identitas baris pemilik foto
	 */
	public String getId() {
		return id;
	}

	/**
	 * Menyetel identitas/label baris data pemilik foto.
	 *
	 * <p>Setter murni tanpa validasi. Nilainya akan menjadi bagian dari alamat yang
	 * dipegang peramban, jadi hindari menaruh apa pun yang tidak layak terlihat di sana.</p>
	 *
	 * @param id identitas baris pemilik foto
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Nama properti pada entitas target yang menyimpan path/nama berkas.
	 *
	 * <p>Dikemas ke dalam token dengan kunci {@code "name"} &mdash; perhatikan bahwa nama
	 * kunci pada token <b>berbeda</b> dari nama field di sini, sehingga menelusuri
	 * pasangannya di sisi penerima menuntut kejelian. Nilainya dipakai secara reflektif di
	 * sana, bukan diperiksa di kelas ini.</p>
	 *
	 * @return nama properti berkas pada entitas target
	 */
	public String getFilePropertyName() {
		return filePropertyName;
	}

	/**
	 * Menyetel nama properti berkas pada entitas target.
	 *
	 * <p>Tidak ada pemeriksaan bahwa properti dengan nama itu benar-benar ada; salah tulis
	 * baru terlihat sebagai kegagalan reflection di sisi penerima.</p>
	 *
	 * @param filePropertyName nama properti berkas pada entitas target
	 */
	public void setFilePropertyName(String filePropertyName) {
		this.filePropertyName = filePropertyName;
	}

	/**
	 * Nama properti pada entitas target yang menyimpan objek media terkait.
	 *
	 * <p>Dikemas ke dalam token dengan kunci {@code "foto"}, lagi-lagi berbeda dari nama
	 * field di sini. Pasangannya adalah {@link #getFilePropertyName()}: yang satu menunjuk
	 * berkas fisiknya, yang lain menunjuk objek medianya.</p>
	 *
	 * @return nama properti objek media pada entitas target
	 */
	public String getMediaPropertyName() {
		return mediaPropertyName;
	}

	/**
	 * Menyetel nama properti objek media pada entitas target.
	 *
	 * @param mediaPropertyName nama properti objek media pada entitas target
	 */
	public void setMediaPropertyName(String mediaPropertyName) {
		this.mediaPropertyName = mediaPropertyName;
	}

	/**
	 * Kelas entitas target yang memiliki field foto/media ini.
	 *
	 * <p>Dikemas ke dalam token sebagai <b>nama kelas lengkap</b> lewat
	 * {@code getClazz().getName()}, dan di sisi penerima dibangkitkan kembali dengan
	 * {@code Class.forName(...)}. Artinya kelas yang dikueri servlet ditentukan oleh isi
	 * permintaan, bukan oleh layar yang sedang dibuka.</p>
	 *
	 * <p><b>Tidak boleh {@code null}:</b> {@code CommonMedia.getMedia(...)} memanggil
	 * {@code getName()} atas nilai ini tanpa penjagaan.</p>
	 *
	 * @return kelas entitas target; diharapkan tidak {@code null}
	 */
	public Class<?> getClazz() {
		return clazz;
	}

	/**
	 * Menyetel kelas entitas target.
	 *
	 * <p>Tidak ada pembatasan jenis kelas yang boleh dipasang di sini. Penyaringan kelas
	 * yang sah &mdash; bila ada &mdash; menjadi tanggung jawab sisi penerima, bukan kelas
	 * ini.</p>
	 *
	 * @param clazz kelas entitas target
	 */
	public void setClazz(Class<?> clazz) {
		this.clazz = clazz;
	}

	/**
	 * Nama properti pada {@link #getClazz()} yang menjadi acuan pencarian foto.
	 *
	 * <p>Dikemas ke dalam token dengan kunci {@code "property"} dan digabung dengan
	 * {@code ""} lebih dahulu, sehingga nilai {@code null} berubah menjadi teks
	 * {@code "null"} alih-alih hilang dari token. Sisi penerima memakainya sebagai nama
	 * kolom pada pembatas pencariannya &mdash; kecuali bila {@link #getUsingId()} bernilai
	 * {@code true}, yang membuat pencarian beralih ke primary key dan properti ini
	 * diabaikan.</p>
	 *
	 * @return nama properti acuan pencarian foto
	 */
	public String getProperty() {
		return property;
	}

	/**
	 * Menyetel nama properti acuan pencarian foto.
	 *
	 * @param property nama properti acuan pencarian foto
	 */
	public void setProperty(String property) {
		this.property = property;
	}

	/**
	 * Tinggi tampilan yang diminta, dalam piksel.
	 *
	 * <p>Hanya ikut dikemas ke dalam token bila bernilai; {@code null} berarti tanpa
	 * batasan tinggi.</p>
	 *
	 * @return tinggi yang diminta, atau {@code null} bila tanpa batasan
	 */
	public Integer getHeight() {
		return height;
	}

	/**
	 * Menyetel tinggi tampilan yang diminta.
	 *
	 * <p>Nilai tidak diperiksa: angka nol maupun negatif diterima apa adanya dan baru
	 * menimbulkan akibat di sisi penerima.</p>
	 *
	 * @param height tinggi dalam piksel; {@code null} menghapus batasan
	 */
	public void setHeight(Integer height) {
		this.height = height;
	}

	/**
	 * Lebar tampilan yang diminta, dalam piksel.
	 *
	 * <p>Hanya ikut dikemas ke dalam token bila bernilai; {@code null} berarti tanpa
	 * batasan lebar.</p>
	 *
	 * @return lebar yang diminta, atau {@code null} bila tanpa batasan
	 */
	public Integer getWidth() {
		return width;
	}

	/**
	 * Menyetel lebar tampilan yang diminta.
	 *
	 * <p>Berlaku ketentuan yang sama seperti {@link #setHeight(Integer)}: nilainya tidak
	 * diperiksa di sini.</p>
	 *
	 * @param width lebar dalam piksel; {@code null} menghapus batasan
	 */
	public void setWidth(Integer width) {
		this.width = width;
	}

	/**
	 * Menandakan foto yang dicari adalah foto utama/profil.
	 *
	 * <p>Berbeda dari field pilihan lain, nilai awalnya {@code false} dan bukan
	 * {@code null}, sehingga getter ini aman dipanggil tanpa penjagaan &mdash; selama tidak
	 * ada yang menyetelnya kembali menjadi {@code null} lewat {@link #setFotoUtama(Boolean)},
	 * yang menerimanya tanpa keluhan dan akan membuat pembacaan di
	 * {@code CommonMedia.getMedia(...)} gagal saat nilainya dibuka otomatis menjadi
	 * {@code boolean}.</p>
	 *
	 * <p>Kunci {@code "fotoUtama"} hanya ditambahkan ke token ketika nilainya benar; ketika
	 * salah, kunci itu tidak ada sama sekali dan sisi penerima memperlakukan ketiadaannya
	 * sebagai "tanpa penyaringan foto utama" &mdash; bukan sebagai "cari yang bukan foto
	 * utama".</p>
	 *
	 * @return {@code true} bila yang dicari foto utama/profil
	 */
	public Boolean getFotoUtama() {
		return fotoUtama;
	}

	/**
	 * Menyetel penanda foto utama/profil.
	 *
	 * <p><b>Hindari menyetel {@code null}</b>; lihat penjelasan pada
	 * {@link #getFotoUtama()}. Berbeda dari {@link #setUsingId(Boolean)}, getter
	 * pasangannya tidak memasang jaring pengaman terhadap nilai kosong.</p>
	 *
	 * @param fotoUtama {@code true} bila yang dicari foto utama/profil
	 */
	public void setFotoUtama(Boolean fotoUtama) {
		this.fotoUtama = fotoUtama;
	}

	/**
	 * Primary key baris foto yang dirujuk secara langsung.
	 *
	 * <p>Dikemas ke dalam token dengan kunci {@code "foto_id"} dan hanya bila bernilai.
	 * Bermakna bersama {@link #getUsingId()}: yang satu menyediakan nomornya, yang lain
	 * menyalakan mode pencarian berbasis nomor itu.</p>
	 *
	 * <p>Perlu diketahui bahwa nilai ini adalah primary key yang dibangkitkan basis data
	 * secara berurutan, sehingga bukan penanda rahasia dan mudah ditebak. Menyusun
	 * permintaan dengan nomor lain adalah hal yang sepele dilakukan; karena itu pembatasan
	 * atas foto mana yang boleh dilihat harus ditegakkan di sisi penerima, bukan
	 * diandalkan dari sulitnya menebak nomor.</p>
	 *
	 * @return primary key baris foto, atau {@code null} bila tidak merujuk baris tertentu
	 */
	public Long getFotoId() {
		return fotoId;
	}

	/**
	 * Menyetel primary key baris foto yang dirujuk secara langsung.
	 *
	 * <p>Menyetel nilai ini saja belum mengubah cara pencarian; ia baru berpengaruh bila
	 * {@link #setUsingId(Boolean)} juga disetel {@code true}. Menyetel salah satunya saja
	 * adalah kekeliruan yang tidak menimbulkan pesan apa pun: nomor terkirim tetapi
	 * diabaikan, atau mode menyala tanpa nomor yang dituju.</p>
	 *
	 * @param fotoId primary key baris foto
	 */
	public void setFotoId(Long fotoId) {
		this.fotoId = fotoId;
	}

	/**
	 * Menentukan apakah pencarian foto memakai primary key ({@link #getFotoId()}) alih-alih
	 * properti pemilik ({@link #getProperty()}).
	 *
	 * <p><b>Getter ini memasang jaring pengaman:</b> nilai {@code null} dikembalikan sebagai
	 * {@code false}, sehingga field yang tidak pernah disetel tetap berperilaku sebagai
	 * pencarian berbasis pemilik &mdash; pilihan bawaan yang lebih aman. Perhatikan bahwa
	 * nilai yang dikembalikan karena itu <b>tidak selalu sama</b> dengan isi field-nya;
	 * kode yang membaca field secara langsung (mis. lewat reflection) akan melihat
	 * {@code null} dan bisa berperilaku lain.</p>
	 *
	 * <p><b>Arti {@code true} dan mengapa perlu hati-hati.</b> Nilai ini berangkat ke sisi
	 * penerima dan di sana menjadi parameter {@code usingId} pada mesin pencarian &mdash;
	 * termasuk pada pemanggilan {@code FileFotoLain.ambil(usingId, ...)} di salah satu
	 * cabang {@code AmbilMedia}. Di sana ia mengubah pencocokan dari kolom pemilik menjadi
	 * primary key, dan pada jalur lampiran ia sekaligus mematikan penyaringan {@code jenis}.
	 * Akibatnya tidak tersisa pembatas yang berhubungan dengan kepemilikan. Setel
	 * {@code true} hanya bila memang bermaksud merujuk satu baris foto tertentu yang sudah
	 * dipastikan boleh dilihat oleh pengguna yang bersangkutan.</p>
	 *
	 * @return {@code true} bila pencarian memakai primary key; {@code false} bila berbasis
	 *         pemilik, termasuk ketika field-nya belum pernah disetel
	 */
	public Boolean getUsingId() {
		return usingId == null ? false : usingId;
	}

	/**
	 * Menyetel mode pencarian foto berbasis primary key.
	 *
	 * <p>Setter murni: nilai {@code null} diterima dan diperlakukan sebagai {@code false}
	 * oleh {@link #getUsingId()}. Sebelum menyetel {@code true}, baca penjelasan pada
	 * getter tersebut &mdash; nilai ini melonggarkan pembatas pencarian di sisi penerima,
	 * bukan sekadar mengganti kolom yang dicocokkan.</p>
	 *
	 * @param usingId {@code true} memakai primary key sebagai acuan pencarian
	 */
	public void setUsingId(Boolean usingId) {
		this.usingId = usingId;
	}

}
