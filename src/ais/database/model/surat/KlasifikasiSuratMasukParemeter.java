package ais.database.model.surat;

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
 * DEFINISI satu field tambahan dinamis pada formulir surat masuk: label, kunci, tipe data, dan
 * nilai bawaannya. Nilai yang benar-benar diketik operator untuk sebuah surat disimpan terpisah
 * di {@link KlasifikasiSuratMasukParemeterValue}.
 *
 * <h3>Rantai relasi (DIVERIFIKASI dari kode, bukan disamakan dengan modul lain)</h3>
 *
 * <pre>
 * {@link KlasifikasiSuratMasuk}                  katalog / kategori surat masuk
 *   &darr; kolom klasifikasi_surat_masuk
 * KlasifikasiSuratMasukParemeter             DEFINISI field (kelas ini)
 *   &darr; kolom klasifikasi_surat_masuk_parameter
 * {@link KlasifikasiSuratMasukParemeterValue}    NILAI per {@link SuratMasuk}
 * </pre>
 *
 * <p>Rantai ini hanya dua tingkat dan berdiri sendiri. Ia BUKAN mekanisme generik
 * {@code ais.database.model.ParameterTambahan} beserta {@code KelompokParameterTambahan*} yang
 * dipakai modul aset, penggajian, dan koperasi: tidak ada tingkat "kelompok", tidak ada kolom
 * urutan tampilan, dan tidak ada flag {@code aktif} untuk menonaktifkan satu field tanpa
 * menghapusnya. Definisi yang tidak lagi dipakai HARUS dihapus, dan penghapusan itu dilakukan
 * dengan {@code session.delete(...)} telanjang oleh
 * {@code ais.action.master.surat.helper.KlasifikasiSuratMasukParameterHelper} tanpa lebih dulu
 * membersihkan baris nilai anaknya.</p>
 *
 * <h3>Empat tipe yang didukung, semuanya disimpan sebagai teks</h3>
 *
 * <p>{@code KlasifikasiSuratMasukParameterHelper} hanya menawarkan empat pilihan pada
 * {@link #getTipe()}: {@code java.lang.String}, {@code java.lang.Integer},
 * {@code java.lang.Double}, dan {@code java.util.Date}. Perakit formulir di
 * {@code SuratMasukAction} memilih widget berdasarkan nilai itu ({@code Textbox},
 * {@code Intbox}, {@code Doublebox}, {@code Datebox}) tetapi selalu menyimpan hasilnya sebagai
 * teks pada {@link KlasifikasiSuratMasukParemeterValue#getNama()}. Bandingkan dengan kembarannya
 * untuk surat keluar, {@code KlasifikasiSuratKeluarParemeter}, yang memiliki tipe tambahan
 * ({@code DATA}, {@code GAMBAR}) -- kedua kelas TIDAK sebangun meski namanya sejajar.</p>
 *
 * <h3>Kolom {@code key} berfungsi di surat keluar, TIDAK berfungsi di surat masuk</h3>
 *
 * <p>Penelusuran seluruh pemanggil {@code getKey()} menunjukkan asimetri yang perlu diketahui
 * sebelum menambah fitur di sini. Pada jalur SURAT KELUAR, {@code KlasifikasiSuratKeluarAction}
 * memakai {@code getKey()} sebagai NAMA PARAMETER laporan
 * ({@code parameters.put(paremeter.getKey(), paremeter.getNilai())}) sehingga kunci itu benar-benar
 * mengikat isian dinamis ke placeholder di templat surat. Pada jalur SURAT MASUK, satu-satunya
 * pemanggil {@code getKey()} adalah {@code KlasifikasiSuratMasukParameterHelper} yang sekadar
 * menampilkannya sebagai {@code Label} pada grid pengaturan. Tidak ada templat surat masuk yang
 * menyerap kunci ini. Jadi kolom {@code key} di sini bersifat DEKORATIF: berisi data yang benar,
 * ditampilkan kepada admin, tetapi tidak pernah mengikat apa pun.</p>
 *
 * @see KlasifikasiSuratMasukParemeterValue nilai per surat masuk untuk definisi ini
 * @see KlasifikasiSuratMasuk katalog klasifikasi pemilik definisi ini
 * @see SuratMasuk dokumen yang formulirnya memuat field dinamis ini
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "surat", name = "klasifikasi_surat_masuk_paremeter")
public class KlasifikasiSuratMasukParemeter extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java; nilainya sama dengan hampir seluruh entitas hasil templat
	 * hbm2java di basis kode ini karena disalin dari templat generator yang sama, bukan karena
	 * kelas-kelas tersebut kompatibel secara biner satu sama lain.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama baris, kolom {@code id}; dibangkitkan basis data (IDENTITY). */
	private Long id;
	/** Nama tampil pengguna yang terakhir menyunting baris ini (jejak audit bayangan). */
	private String oleh;
	/** Id pengguna yang terakhir menyunting baris ini (jejak audit bayangan). */
	private String olehId;

	/**
	 * Id pengguna penyunting terakhir.
	 *
	 * @return id pengguna penyunting terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna penyunting terakhir, MENGABAIKAN nilai kosong.
	 *
	 * <p>Pengabaian nilai kosong adalah KEHARUSAN TEKNIS, bukan cacat. Definisi parameter
	 * disimpan ulang oleh listener {@code onChange} setiap kali admin mengubah nilai bawaan
	 * atau tipe pada grid pengaturan; sebagian jalur penyimpanan itu berjalan tanpa konteks
	 * pengguna aktif, dan bila setter ini menerima nilai kosong, jejak "diubah oleh siapa"
	 * akan terhapus oleh proses yang bukan perbuatan manusia.</p>
	 *
	 * @param olehId id pengguna penyunting; diabaikan bila {@code null} atau hanya spasi
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks definisi ini, yaitu LABEL field dinamis ({@code nama}) apa adanya.
	 *
	 * <p>Nilai diambil langsung dari field, bukan lewat {@link #getNama()}, sehingga hasilnya
	 * bisa {@code null} untuk baris yang belum diberi nama -- berbeda dari {@link #getNama()}
	 * yang selalu me-{@code trim}. Metode ini dipakai komponen ZK yang menampilkan objek
	 * secara generik.</p>
	 *
	 * @return label field dinamis, mungkin {@code null}
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Mengisi nama pengguna penyunting terakhir, MENGABAIKAN nilai kosong dengan alasan yang
	 * sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna penyunting; diabaikan bila {@code null} atau hanya spasi
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Nama tampil pengguna penyunting terakhir.
	 *
	 * @return nama pengguna penyunting terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait {@code @PreUpdate} JPA: menyerahkan pembaruan stempel waktu dan identitas pengubah
	 * ke {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum Hibernate menulis perubahan
	 * baris ini ke basis data.
	 *
	 * <p>Kait ini hanya berjalan pada UPDATE, bukan INSERT; nilai awal {@code tanggal_dirubah}
	 * karena itu diberikan lewat inisialisasi field yang ditulis pada baris yang sama dengan
	 * metode ini. Kelas juga beranotasi {@code @Audited} sehingga Envers menyimpan riwayat
	 * versi terpisah; trio {@code oleh}/{@code olehId}/{@code tanggal_dirubah} adalah jejak
	 * audit BAYANGAN yang menempel pada baris hidup agar tampilan tidak perlu menyentuh tabel
	 * revisi Envers hanya untuk menampilkan "diubah oleh siapa, kapan" -- keharusan teknis,
	 * bukan duplikasi yang keliru.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir. Umumnya tidak dipanggil kode aplikasi karena
	 * {@link #onUpdate()} sudah mengurusnya; disediakan untuk impor dan perbaikan data.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir baris ini.
	 *
	 * @return stempel waktu perubahan terakhir; untuk objek baru berisi waktu pembuatan objek
	 *         karena field diinisialisasi pada deklarasi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** LABEL field dinamis yang ditampilkan di formulir surat masuk. */
	private String nama;
	/**
	 * Kunci teknis hasil turunan dari {@link #nama}. Bersifat dekoratif pada modul surat masuk;
	 * lihat javadoc kelas dan {@link #getKey()}.
	 */
	private String key;
	/** Penjelasan singkat untuk admin mengenai kegunaan field dinamis ini. */
	private String keterangan;
	/** Nilai BAWAAN yang dipakai selama surat belum punya baris nilai sendiri. */
	private String nilai;
	/** Nama kelas Java penanda tipe isian; salah satu dari String/Integer/Double/Date. */
	private String tipe;
	/** Klasifikasi surat masuk pemilik definisi field ini. */
	private KlasifikasiSuratMasuk klasifikasiSuratMasuk;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate. Baris baru belum sah disimpan sebelum
	 * {@link #setNama} diisi, karena kolom {@code nama} beranotasi {@code nullable = false};
	 * {@link #getTipe()} sendiri akan mengisi tipe bawaan {@code java.lang.String} bila
	 * dibiarkan kosong.
	 */
	public KlasifikasiSuratMasukParemeter() {
	}

	/**
	 * Kunci utama baris.
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
	 * Mengisi kunci utama. Hanya dipakai Hibernate dan proses impor.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * LABEL field dinamis, sudah di-{@code trim}.
	 *
	 * <p>Label inilah yang dipasang sebagai judul kolom pada formulir surat masuk dan yang
	 * muncul di kiri tanda titik dua pada ringkasan "label : nilai". Nilai isiannya sendiri
	 * ada di {@link KlasifikasiSuratMasukParemeterValue#getNama()} -- penamaan properti yang
	 * sama pada dua entitas berbeda ini mudah tertukar saat membaca kode pemanggil.</p>
	 *
	 * <p>Label juga menjadi sumber tunggal {@link #getKey()}: mengubah label berarti mengubah
	 * kunci teknis, tanpa peringatan apa pun.</p>
	 *
	 * @return label field dinamis yang sudah dipangkas spasi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi label field dinamis.
	 *
	 * <p>Pemanggil di {@code KlasifikasiSuratMasukParameterHelper} selalu mengirim hasil
	 * {@code trim()} dari kotak isian dialog "Tambah Paremeter". Perhatikan efek lanjutannya:
	 * karena {@link #getKey()} menurunkan kunci dari label pada setiap pembacaan, mengubah
	 * label lewat setter ini akan menulis ulang kolom {@code key} pada flush berikutnya.</p>
	 *
	 * @param nama label field dinamis
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Penjelasan singkat kegunaan field dinamis ini.
	 *
	 * <p>Diisi lewat dialog "Tambah Paremeter", tetapi baris yang menampilkannya pada grid
	 * pengaturan berada dalam kode yang dikomentari, sehingga saat ini keterangan hanya bisa
	 * dibaca kembali lewat ekspor data atau CRUD generik.</p>
	 *
	 * @return penjelasan singkat, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi penjelasan singkat kegunaan field dinamis ini.
	 *
	 * @param keterangan penjelasan singkat; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Kunci teknis field dinamis, DITURUNKAN ULANG dari {@link #getNama()} pada setiap
	 * pembacaan.
	 *
	 * <h3>Getter destruktif -- turunan menimpa kolom tersimpan</h3>
	 *
	 * <p>Selama {@code nama} tidak kosong, metode ini MENULIS ke field {@code key} hasil
	 * transformasi {@code nama.trim().toLowerCase().replaceAll(" ", "_")} sebelum
	 * mengembalikannya. Karena {@code key} adalah properti persisten dan kelas beranotasi
	 * {@code dynamicUpdate = true}, nilai turunan itu ikut ter-flush ke basis data pada
	 * transaksi berjalan. Konsekuensinya ada dua: (1) nilai yang pernah diisi lewat
	 * {@link #setKey(String)} atau lewat impor TIDAK akan bertahan begitu {@code nama} terisi;
	 * (2) mengganti label parameter otomatis mengganti kuncinya, tanpa konfirmasi dan tanpa
	 * mekanisme migrasi.</p>
	 *
	 * <p>Transformasinya sendiri hanya mengganti SPASI dengan garis bawah dan menurunkan huruf;
	 * karakter lain (titik, garis miring, tanda kutip, huruf beraksen) diteruskan apa adanya,
	 * sehingga kunci yang dihasilkan tidak dijamin aman sebagai identifier.</p>
	 *
	 * <h3>Di modul surat masuk kunci ini tidak mengikat apa pun</h3>
	 *
	 * <p>Penelusuran pemanggil menunjukkan satu-satunya pembaca {@code getKey()} pada jalur
	 * surat masuk adalah {@code KlasifikasiSuratMasukParameterHelper}, yang menampilkannya
	 * sebagai {@code Label} di grid pengaturan. Berbeda dengan jalur surat keluar, tempat
	 * {@code KlasifikasiSuratKeluarAction} memakai kunci sejenis sebagai nama parameter laporan
	 * ({@code parameters.put(key, nilai)}) untuk mengisi placeholder templat surat. Karena itu
	 * dampak praktis dari penulisan ulang kolom {@code key} di sini terbatas pada tampilan --
	 * TETAPI hal itu berlaku hanya selama modul surat masuk belum mendapat pencetakan berbasis
	 * templat; begitu fitur seperti itu ditambahkan, sifat destruktif getter ini berubah dari
	 * gangguan kosmetik menjadi sumber kerusakan tautan templat.</p>
	 *
	 * @return kunci teknis hasil turunan label; nilai lama yang tersimpan bila {@code nama}
	 *         masih kosong, dan {@code null} bila keduanya belum pernah diisi
	 */
	public String getKey() {
		boolean ada = nama != null && !nama.trim().equals("");
		if (ada) {
			key = nama.trim().toLowerCase().replaceAll(" ", "_");
		}
		return key;
	}

	/**
	 * Mengisi kunci teknis secara langsung.
	 *
	 * <p>PERHATIAN: nilai yang diisi lewat setter ini hanya bertahan selama {@link #getNama()}
	 * masih kosong. Begitu label terisi, pembacaan {@link #getKey()} berikutnya akan
	 * menimpanya dengan turunan label. Setter ini praktis hanya berguna bagi Hibernate saat
	 * memuat baris dari basis data.</p>
	 *
	 * @param key kunci teknis
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * Klasifikasi surat masuk pemilik definisi field ini.
	 *
	 * <p>Relasi dipetakan LAZY, karena itu getter membongkar proxy lebih dulu lewat
	 * {@code check(...)} milik {@code GeneralValueObject} agar pemanggil selalu menerima objek
	 * nyata dan bukan proxy yang bisa meledak di luar sesi Hibernate.</p>
	 *
	 * <p>Relasi inilah kunci pengelompokan seluruh definisi parameter: {@code initCriteria} di
	 * {@code KlasifikasiSuratMasukParameterHelper} menyaring tepat dengan
	 * {@code Restrictions.eq("klasifikasiSuratMasuk", ...)}, dan perakit formulir di
	 * {@code SuratMasukAction} memakai penyaring yang sama untuk menentukan field dinamis apa
	 * saja yang muncul pada sebuah surat. Karena kolomnya {@code nullable = true}, definisi
	 * yatim (tanpa klasifikasi) tetap bisa tersimpan namun TIDAK akan pernah tampil di
	 * formulir mana pun.</p>
	 *
	 * @return klasifikasi surat masuk pemilik, atau {@code null} untuk definisi yatim
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "klasifikasi_surat_masuk", nullable = true)
	public KlasifikasiSuratMasuk getKlasifikasiSuratMasuk() {
		klasifikasiSuratMasuk = check(klasifikasiSuratMasuk);
		return klasifikasiSuratMasuk;
	}

	/**
	 * Menautkan definisi field ini ke satu klasifikasi surat masuk.
	 *
	 * @param klasifikasiSuratMasuk klasifikasi pemilik; {@code null} menjadikan definisi yatim
	 */
	public void setKlasifikasiSuratMasuk(KlasifikasiSuratMasuk klasifikasiSuratMasuk) {
		this.klasifikasiSuratMasuk = klasifikasiSuratMasuk;
	}

	/**
	 * Nilai BAWAAN field dinamis, dinormalkan ke teks kosong bila belum diisi.
	 *
	 * <p>Nilai ini dipakai sebagai isi awal widget ketika sebuah surat masuk BELUM memiliki
	 * baris {@link KlasifikasiSuratMasukParemeterValue} untuk definisi ini. Pola tepatnya
	 * terlihat pada keempat cabang tipe di {@code SuratMasukAction}, yang semuanya berbentuk
	 * {@code value == null ? paremeter.getNilai().trim() : value.getNama().trim()}. Begitu
	 * operator menyentuh widget dan listener {@code onChange} menyimpan baris nilai, nilai
	 * bawaan tidak lagi berperan untuk surat tersebut -- mengubahnya kemudian TIDAK mengubah
	 * surat yang sudah terisi.</p>
	 *
	 * <p>Getter ini destruktif ringan: bila field masih {@code null} ia menuliskan teks kosong
	 * ke field lebih dulu. Normalisasi itu penting karena pemanggil langsung merangkai
	 * {@code .trim()} pada hasilnya tanpa pemeriksaan {@code null}; tanpa normalisasi,
	 * pembukaan formulir surat untuk parameter yang nilai bawaannya belum pernah diisi akan
	 * melempar {@code NullPointerException}.</p>
	 *
	 * <p>Untuk parameter bertipe {@code Date}, isi kolom ini harus berupa teks tanggal yang
	 * dapat diurai {@code Common.dateFormat2}; bila tidak, penguraian gagal dalam
	 * {@code try/catch} dan widget diam-diam jatuh ke tanggal hari ini. Untuk tipe angka,
	 * kegagalan penguraian jatuh ke nol. Tidak ada validasi yang mencegah admin mengisi nilai
	 * bawaan yang tidak sesuai dengan tipe yang dipilih.</p>
	 *
	 * @return nilai bawaan dalam bentuk teks; teks kosong bila belum diisi, tidak pernah
	 *         {@code null}
	 */
	public String getNilai() {
		if (nilai == null) {
			nilai = "";
		}
		return nilai;
	}

	/**
	 * Mengisi nilai bawaan field dinamis.
	 *
	 * <p>Dipanggil listener {@code onChange} kotak isian "Nilai" pada grid pengaturan, yang
	 * langsung melakukan {@code session.update(...)} untuk baris yang sudah ber-id sehingga
	 * perubahan tersimpan tanpa tombol simpan terpisah.</p>
	 *
	 * @param nilai nilai bawaan dalam bentuk teks
	 */
	public void setNilai(String nilai) {
		this.nilai = nilai;
	}

	/**
	 * Nama kelas Java yang menandai tipe isian field dinamis, dengan bawaan
	 * {@code java.lang.String}.
	 *
	 * <p>Nilai yang sah hanya empat, sesuai daftar yang disediakan
	 * {@code KlasifikasiSuratMasukParameterHelper}: {@code java.lang.String},
	 * {@code java.lang.Integer}, {@code java.lang.Double}, dan {@code java.util.Date}.
	 * Perakit formulir di {@code SuratMasukAction} membandingkan hasil getter ini dengan
	 * {@code String.class.getName()} dan seterusnya memakai rantai {@code if/else if} tanpa
	 * cabang {@code else} penutup. Artinya nilai tipe di luar keempatnya -- yang bisa masuk
	 * lewat impor data atau CRUD generik -- membuat field dinamis TIDAK DIGAMBAR sama sekali
	 * pada formulir: tidak ada widget, tidak ada pesan galat, dan isian yang mungkin sudah
	 * tersimpan untuk field itu menjadi tak tersunting lewat antarmuka.</p>
	 *
	 * <p>Getter ini destruktif ringan (menuliskan bawaan ke field saat masih {@code null}),
	 * yang justru diperlukan karena pemanggil langsung memanggil {@code .equals(...)} pada
	 * hasilnya tanpa pemeriksaan {@code null}.</p>
	 *
	 * <p>Perubahan tipe pada definisi yang sudah punya isian TIDAK memigrasikan isian lama:
	 * nilai tetap tersimpan sebagai teks dan akan diurai ulang menurut tipe baru, dengan
	 * kegagalan penguraian yang jatuh diam-diam ke nol atau tanggal hari ini.</p>
	 *
	 * @return nama kelas penanda tipe; {@code java.lang.String} bila belum pernah dipilih
	 */
	public String getTipe() {
		if (tipe == null) {
			tipe = String.class.getName();
		}
		return tipe;
	}

	/**
	 * Mengisi penanda tipe isian field dinamis.
	 *
	 * <p>Pemanggil normal adalah listener {@code onChange} pada {@code Combobox} "Tipe" di grid
	 * pengaturan, yang mengirim {@code getName()} salah satu dari empat kelas yang didukung dan
	 * langsung menyimpan perubahan lewat {@code session.update(...)}. Tidak ada validasi di
	 * setter ini yang menolak nama kelas lain; lihat {@link #getTipe()} untuk akibatnya.</p>
	 *
	 * @param tipe nama kelas Java penanda tipe isian
	 */
	public void setTipe(String tipe) {
		this.tipe = tipe;
	}
}
