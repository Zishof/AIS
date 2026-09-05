package ais.database.model.file;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.sql.Blob;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.imgscalr.Scalr;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Box;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.generic.AmbilDataLampiranFileLain;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.VOMahasiswa;
import ais.database.model.asset.PenyediaAsset;
import ais.database.model.streaming.AudioPertemuan;
import ais.database.model.streaming.VideoPertemuan;
import ais.ui.util.MyJSONObject;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.SetelahUpload;
import ais.ui.util.WaktuUtil;

/**
 * Model data untuk file foto lain. Tipe ini membawa state yang dipertukarkan oleh lapisan
 * persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang
 * dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * FileFoto}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Map RELASI_MAP}, {@code long
 * SOFT_DELETE_ID}; inisialisasi/lifecycle ({@code setupJurusanCombo()}, {@code setupDownloadButtonAction()});
 * pembacaan/pencarian ({@code ambilRef()}, {@code ambilClazz()}, {@code getJenis()}, {@code getLink()}, {@code
 * getOlehId()}, {@code getOleh()}); mutasi data ({@code setUrl()}, {@code resetLokasi()}, {@code
 * hapusAtauUpdate()}); penghapusan/pembatalan ({@code delete()}, {@code performDelete()}); operasi domain lain
 * ({@code createLinkUri()}, {@code createLinkUri()}, {@code createLinkUri()}, {@code iconNggakAda()}, {@code
 * iconNggakAda()}, {@code tulisLokasi()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
 * <p><b>Efek samping:</b> selain accessor state, operasi domain yang disebut di atas dapat membaca/mengubah
 * persistence, memicu lifecycle, atau membentuk komponen UI. Jangan menganggap model ini selalu murni;
 * panggil operasi tersebut melalui alur service dengan session, transaksi, dan otorisasi yang sesuai agar
 * perilakunya tidak disalin ke tempat lain.</p>
 *
 * <h2>Kedudukan kelas ini dalam keluarga lampiran</h2>
 * <p>{@code FileFotoLain} adalah <b>superclass abstrak bersama</b> bagi hampir seluruh entitas
 * berkas pada paket {@link ais.database.model.file}: {@code LampiranLain}, {@code
 * LampiranLainMahasiswa}, {@code LampiranLainBiodataCalonMahasiswa}, {@code FotoMahasiswa},
 * {@code FotoMahasiswaLulus}, {@code FotoBiodataCalonMahasiswa}, {@code FotoDosen}, {@code
 * FotoPegawai}, {@code FotoGuru}, {@code FotoCalonSiswa}, {@code FotoCalonPegawai}, {@code
 * FotoSiswa}, {@code FotoAdmin}, {@code LampiranPklMahasiswa}, {@code LampiranKknMahasiswa},
 * {@code LampiranBeasiswaMahasiswa}, {@code TugasFileContent}, {@code PertemuanFileContent},
 * {@code FotoGambarProduk}, serta entitas streaming {@code AudioPertemuan}/{@code
 * VideoPertemuan}. Semua nama itu terdaftar di {@code RELASI_MAP} pada blok {@code static}
 * di bawah.</p>
 * <p><b>Ini penting untuk dipahami sebelum membaca sisa kelas:</b> {@code LampiranLain} BUKAN
 * saudara sejajar melainkan salah satu <i>subclass</i>-nya. Praktis seluruh method statis
 * {@code LampiranLain.ambil(...)}, {@code LampiranLain.resetLokasi(...)}, dan {@code
 * LampiranLain.ambilLinkLampiranLain(...)} hanyalah pembungkus tipis yang meneruskan
 * pekerjaan ke method senama di kelas ini dengan {@code clazz = LampiranLain.class}
 * (lihat {@code LampiranLain} baris 1491-1492 dan 1548-1550). Konsekuensinya: apa pun yang
 * berlaku pada {@code ambil()} di sini berlaku pula bagi <b>setiap</b> entitas berkas di atas,
 * bukan hanya bagi {@code LampiranLain}. Perbaikan perilaku pencarian/otorisasi lampiran
 * karena itu harus dilakukan DI SINI, bukan di masing-masing subclass.</p>
 *
 * <h2>Tiga lapis penyimpanan yang dikelola bersamaan</h2>
 * <ol>
 *   <li><b>Baris basis data</b> pada tabel milik subclass, dengan kolom biner {@code foto}
 *       bertipe PostgreSQL Large Object (oid). Ditulis lewat {@code createFileFotoLain()} pada
 *       koneksi non-autocommit khusus, dibaca lewat {@code ambilIsiBlob()}.</li>
 *   <li><b>Berkas fisik</b> di direktori media ({@code CommonMedia.getMediaDirectory()}),
 *       ditata per entitas melalui {@code FileFoto.segmenFolderBerkas()} dan dilayani
 *       <i>statis</i> lewat awalan URL {@code /f<prefix>/...} tanpa melewati servlet.</li>
 *   <li><b>Berkas cache metadata</b> berisi JSON hasil {@code Common.convertToJsonObject(...)},
 *       ditulis {@code tulisLokasi()} dan dibaca {@code ambilLokasi()}. Cache inilah yang
 *       dipakai {@code ambil()} untuk memintas query basis data sama sekali.</li>
 * </ol>
 * <p>Ketiga lapis itu tidak pernah dibuat konsisten secara transaksional. Sebagian besar
 * keanehan perilaku yang didokumentasikan pada method-method di bawah berakar pada
 * perbedaan waktu hidup ketiga lapis ini &mdash; terutama lapis (3) yang punya kunci
 * berbeda untuk {@code usingId=true} dan {@code usingId=false} sehingga satu baris yang
 * sama dapat punya DUA cache yang dibersihkan pada saat yang berbeda.</p>
 *
 * @see FileFoto
 */
@SuppressWarnings({ "rawtypes", "unchecked", "serial" })
public abstract class FileFotoLain extends FileFoto {

	// Konfigurasi Mapping Kelas ke Nama Field Relasi (Menggantikan if-else raksasa)
	/**
	 * Peta kelas entitas berkas &rarr; nama properti Hibernate yang menyimpan acuan
	 * ({@code ref}) ke baris pemilik lampiran. Menggantikan rangkaian {@code if-else}
	 * raksasa pada versi lama.
	 *
	 * <p><b>Fungsi.</b> Dibaca lewat {@code getRefField(Class)} dan menentukan tiga hal
	 * sekaligus: (a) properti yang dipakai {@code ambil()} pada klausa {@code
	 * Restrictions.eq(refName, ref)}; (b) nama setter yang dipanggil secara reflektif oleh
	 * {@code createFileFotoLain()} saat menautkan lampiran baru ke pemiliknya; (c) kolom
	 * yang ditimpa nilai sentinel oleh {@code hapusAtauUpdate()} ketika lampiran
	 * "dihapus".</p>
	 *
	 * <p><b>Tiga golongan nilai, dengan perilaku yang berbeda tajam:</b></p>
	 * <ul>
	 *   <li>Nama relasi biasa ({@code "mahasiswa"}, {@code "dosen"}, {@code "pegawai"},
	 *       {@code "siswa"}, {@code "guru"}, {@code "calonSiswa"}, {@code "calonPegawai"},
	 *       {@code "biodataCalonMahasiswa"}, {@code "persyaratanPkl"}, {@code
	 *       "persyaratanKkn"}, {@code "persyaratanBeasiswa"}, {@code "produk"}, {@code
	 *       "ref"}) &rarr; jalur normal: pencarian memakai kolom relasi, penghapusan
	 *       memakai <i>soft delete</i> dengan menimpa kolom itu dengan
	 *       {@code SOFT_DELETE_ID}.</li>
	 *   <li>{@code "tbmuser"} (hanya {@code FotoAdmin}) &rarr; kolomnya bertipe
	 *       {@code String} berisi userid, bukan angka. Seluruh jalur diberi cabang khusus:
	 *       konversi {@code ref} ke {@code String} pada {@code ambil()}, setter khusus
	 *       {@code setTbmuser(String)} pada {@code createFileFotoLain()}, dan sentinel
	 *       yang di-{@code String.valueOf()}-kan pada {@code hapusAtauUpdate()}.</li>
	 *   <li>{@code "id"} ({@code TugasFileContent}, {@code PertemuanFileContent},
	 *       {@code AudioPertemuan}, {@code VideoPertemuan}) &rarr; entitas TIDAK punya
	 *       kolom acuan pemilik sama sekali; {@code ref} dicocokkan langsung ke primary
	 *       key. Golongan ini punya dua akibat penting yang dijelaskan pada
	 *       {@code ambil()} dan {@code hapusAtauUpdate()}: penyaringan {@code jenis}
	 *       dimatikan, dan penghapusan non-{@code usingId} tidak melakukan apa-apa.</li>
	 * </ul>
	 *
	 * <p><b>Kelas yang tidak terdaftar</b> tidak ditolak: {@code getRefField()}
	 * mengembalikan {@code "id"} sebagai default, sehingga entitas berkas baru yang lupa
	 * didaftarkan di sini akan diam-diam berperilaku sebagai golongan ketiga di atas.
	 * Ini kegagalan yang <i>fail-open</i> dan tidak menghasilkan pesan kesalahan apa pun.</p>
	 */
	private static final Map<Class<?>, String> RELASI_MAP = new HashMap<Class<?>, String>();

	/**
	 * Nilai sentinel yang ditulis ke kolom acuan pemilik untuk menandai lampiran
	 * "terhapus" &mdash; <b>penghapusan di sini bersifat lunak (soft delete)</b>.
	 *
	 * <p>{@code hapusAtauUpdate()} pada jalur non-{@code usingId} tidak menjalankan
	 * {@code DELETE}, melainkan {@code UPDATE ... SET <refField> = -111111119}. Barisnya
	 * tetap ada di tabel, kolom binernya tetap berisi seluruh isi berkas, dan primary
	 * key-nya tidak berubah.</p>
	 *
	 * <p><b>Akibat yang harus disadari:</b> karena {@code ambil()} dengan
	 * {@code usingId=true} mencocokkan {@code ref} ke <i>primary key</i> dan sama sekali
	 * tidak melihat kolom acuan pemilik, baris yang sudah "dihapus" dengan cara ini
	 * <b>tetap dapat ditemukan dan diunduh</b>. Nilai sentinel hanya menyembunyikan baris
	 * dari pencarian berbasis pemilik, bukan dari pencarian berbasis id. Rinciannya
	 * dijelaskan pada Javadoc {@code ambil(Boolean, Serializable, String, int, Class,
	 * boolean, String)}.</p>
	 *
	 * <p>Nilainya negatif dan berjarak jauh dari rentang id nyata sehingga tidak mungkin
	 * bertabrakan dengan acuan pemilik yang sah; alasan serupa dipakai pada
	 * {@code Common.refSementara()} (lihat catatan di {@code createFileFotoLain()}).</p>
	 */
	private static final long SOFT_DELETE_ID = -111111119L;

	static {
		RELASI_MAP.put(LampiranLain.class, "ref");
		RELASI_MAP.put(LampiranLainMahasiswa.class, "mahasiswa");
		RELASI_MAP.put(LampiranLainBiodataCalonMahasiswa.class, "biodataCalonMahasiswa");
		RELASI_MAP.put(FotoMahasiswa.class, "mahasiswa");
		RELASI_MAP.put(FotoMahasiswaLulus.class, "mahasiswa");
		RELASI_MAP.put(FotoBiodataCalonMahasiswa.class, "biodataCalonMahasiswa");
		RELASI_MAP.put(FotoDosen.class, "dosen");
		RELASI_MAP.put(FotoPegawai.class, "pegawai");
		RELASI_MAP.put(FotoGuru.class, "guru");
		RELASI_MAP.put(FotoCalonSiswa.class, "calonSiswa");
		RELASI_MAP.put(FotoCalonPegawai.class, "calonPegawai");
		RELASI_MAP.put(FotoSiswa.class, "siswa");
		RELASI_MAP.put(FotoAdmin.class, "tbmuser"); // Khusus string, ditangani terpisah
		RELASI_MAP.put(LampiranPklMahasiswa.class, "persyaratanPkl");
		RELASI_MAP.put(LampiranKknMahasiswa.class, "persyaratanKkn");
		RELASI_MAP.put(LampiranBeasiswaMahasiswa.class, "persyaratanBeasiswa");

		// Class yang menggunakan ID langsung atau tidak punya field relasi spesifik
		RELASI_MAP.put(TugasFileContent.class, "id");
		RELASI_MAP.put(PertemuanFileContent.class, "id");
		RELASI_MAP.put(AudioPertemuan.class, "id");
		RELASI_MAP.put(VideoPertemuan.class, "id");
		RELASI_MAP.put(FotoGambarProduk.class, "produk");
	}

	/**
	 * Acuan ({@code ref}) ke baris pemilik lampiran ini, sebagaimana tersimpan pada kolom
	 * yang dipetakan {@code RELASI_MAP} untuk kelas konkretnya.
	 *
	 * <p>Subclass mengembalikan isi kolom relasinya masing-masing: {@code LampiranLain}
	 * mengembalikan kolom {@code ref} generiknya, {@code FotoMahasiswa} mengembalikan id
	 * mahasiswa, dan seterusnya. Untuk golongan {@code "id"} pada {@code RELASI_MAP}
	 * (mis. {@code TugasFileContent}) nilainya identik dengan primary key.</p>
	 *
	 * <p><b>Nilai yang mungkin muncul dan artinya:</b> nilai positif normal berarti acuan
	 * ke baris pemilik yang nyata; nilai negatif besar berarti acuan sementara hasil
	 * {@code Common.refSementara()} untuk data yang belum disimpan (lihat catatan panjang
	 * pada {@code createFileFotoLain()} tentang mengapa acuan sementara WAJIB negatif);
	 * dan nilai {@code -111111119} berarti baris ini sudah "dihapus" secara lunak oleh
	 * {@code hapusAtauUpdate()} namun isinya masih utuh di basis data.</p>
	 *
	 * <p>Dipakai antara lain oleh {@code ambilLinkLampiranLain(FileFotoLain, ...)} untuk
	 * menyusun parameter {@code ref} pada tautan {@code /al?d=...}, dan oleh
	 * {@code delete()} untuk menentukan kunci cache metadata yang harus dibuang.</p>
	 *
	 * @return acuan pemilik, {@code null} bila belum ditautkan ke siapa pun
	 */
	public abstract Long ambilRef();

	/**
	 * Kelas entitas konkret yang mewakili baris ini, dipakai sebagai parameter
	 * {@code clazz} pada seluruh method statis di kelas ini.
	 *
	 * <p>Sengaja tidak memakai {@code getClass()} karena instance yang dikembalikan
	 * {@code ambil()} bisa berupa hasil {@code Common.convertToObject()} atas cache JSON
	 * ataupun proxy Hibernate; {@code getClass()} pada proxy akan mengembalikan kelas
	 * bayangan hasil <i>bytecode enhancement</i>, bukan kelas entitas yang terdaftar di
	 * {@code RELASI_MAP}. Implementasi subclass mengembalikan literal kelasnya sendiri
	 * sehingga nilainya stabil pada semua kondisi.</p>
	 *
	 * <p>Nilai ini menentukan tabel yang dikueri {@code ambil()}, nama entitas pada HQL
	 * {@code hapusAtauUpdate()}, folder berkas ({@code FileFoto.segmenFolderBerkas()}),
	 * berkas cache metadata ({@code Common.getFileLocation(clazz, ...)}), sekaligus ikon
	 * pengganti yang dipilih {@code iconNggakAda(Class)}.</p>
	 *
	 * @return kelas entitas konkret; tidak pernah {@code null} pada implementasi yang benar
	 */
	public abstract Class ambilClazz();

	/**
	 * Penanda jenis lampiran &mdash; label bebas berbentuk teks yang membedakan beberapa
	 * lampiran milik satu pemilik yang sama (mis. {@code "ktp"}, {@code "ijazah"},
	 * {@code "foto"}).
	 *
	 * <p>Bersama {@code ambilRef()}, nilai inilah yang biasanya membentuk identitas logis
	 * sebuah lampiran: satu pemilik boleh punya banyak lampiran asalkan {@code jenis}-nya
	 * berbeda, dan mengunggah ulang dengan {@code jenis} yang sama akan menghapus-lunak
	 * yang lama lebih dahulu (lihat {@code createFileFotoLain()} langkah 1).</p>
	 *
	 * <p><b>Catatan penting.</b> Nilai ini tidak selalu ikut menyaring pencarian. Pada
	 * {@code ambil()} penyaringan {@code jenis} dimatikan seluruhnya bila
	 * {@code usingId=true}, dan juga bila kelasnya termasuk golongan yang dianggap tidak
	 * punya kolom {@code jenis} (nama kelas berawalan {@code Foto}, berakhiran
	 * {@code FileContent}, atau ber-{@code refField} {@code "id"}/{@code "tbmuser"}).
	 * Karena itu {@code jenis} <b>tidak boleh diperlakukan sebagai batas keamanan</b>;
	 * ia adalah label penataan, bukan penyekat namespace.</p>
	 *
	 * @return penanda jenis lampiran, dapat {@code null} pada entitas yang tidak memilikinya
	 */
	public abstract String getJenis();

	/**
	 * Tautan luar (URL) sebagai pengganti berkas yang tersimpan di basis data.
	 *
	 * <p>Sebagian lampiran tidak diunggah melainkan hanya dirujuk. Pada kasus itu
	 * {@code getNama()} berisi teks penanda {@code "Berupa link file"} dan seluruh isi
	 * berkas tidak ada di kolom biner. {@code ambilLinkLampiranLain(FileFotoLain, ...)}
	 * memeriksa penanda tersebut lebih dahulu dan langsung mengembalikan nilai method ini
	 * apa adanya sebagai tautan akhir.</p>
	 *
	 * <p><b>Konsekuensi keamanan yang perlu diketahui pemanggil:</b> nilai yang dikembalikan
	 * berasal dari masukan pengguna saat pengisian data dan diteruskan ke peramban tanpa
	 * pembatasan skema. {@code AmbilLampiran} bahkan membukanya sisi-server dengan
	 * {@code new URL(link).openStream()} (baris 417 pada berkas servlet tersebut) bila
	 * tautannya bukan Google Photos &mdash; artinya alamat yang diisi pengguna ditarik oleh
	 * server, bukan oleh peramban. Setiap penambahan pemanggil baru sebaiknya
	 * mempertimbangkan hal ini.</p>
	 *
	 * @return URL lampiran, atau {@code null}/kosong bila berkasnya memang tersimpan sendiri
	 */
	public abstract String getLink();

	/**
	 * Penanda identitas pengguna yang terakhir mengubah baris ini &mdash; <b>field audit
	 * bayangan</b>, bukan relasi ORM ke {@code Tbmuser}.
	 *
	 * <p>Diisi {@code createFileFotoLain()} langkah 3 dengan hasil
	 * {@code Common.generateOlehId(tbmuser)}. Bentuknya sengaja berupa teks lepas dan
	 * bukan foreign key: baris audit harus tetap terbaca meski pengguna yang bersangkutan
	 * kelak dihapus atau berganti identitas, dan tabel lampiran hidup pada
	 * {@code SessionFactory} streaming yang terpisah sehingga relasi lintas
	 * {@code SessionFactory} memang tidak dapat dibentuk. Ini <b>keharusan teknis</b>,
	 * bukan kelalaian pemodelan.</p>
	 *
	 * <p>Nilai ini tidak pernah dipakai untuk otorisasi di kelas ini; tidak ada satu pun
	 * jalur baca yang membandingkannya dengan pengguna yang sedang aktif.</p>
	 *
	 * @return penanda pengubah terakhir, atau {@code null} bila baris berasal dari
	 *         pembaruan tanpa sesi pengguna
	 */
	public abstract String getOlehId();

	/**
	 * Nama pengguna yang terakhir mengubah baris ini, disalin sebagai teks pada saat
	 * perubahan &mdash; pasangan {@code getOlehId()} pada pola field audit bayangan.
	 *
	 * <p>Diisi {@code createFileFotoLain()} dari {@code getNamaOleh(Tbmuser)}, yang
	 * mengambil nama dari entitas mahasiswa/dosen/pegawai yang tertaut pada pengguna, dan
	 * jatuh ke {@code "external_update"} bila perubahan terjadi tanpa sesi pengguna
	 * (mis. lewat integrasi atau proses terjadwal).</p>
	 *
	 * <p>Karena nilainya disalin, ia tetap menampilkan nama <i>pada saat kejadian</i>
	 * walaupun nama pengguna kemudian diubah. Untuk keperluan audit sifat ini justru
	 * diinginkan; jangan "diperbaiki" menjadi relasi hidup ke tabel pengguna.</p>
	 *
	 * @return nama pengubah terakhir, {@code "external_update"} bila tanpa sesi pengguna
	 */
	public abstract String getOleh();

	/**
	 * Waktu perubahan terakhir baris ini, dalam penamaan gaya lama yang memakai garis
	 * bawah karena mengikuti nama kolom basis datanya.
	 *
	 * <p>Diisi {@code createFileFotoLain()} dengan {@code WaktuUtil.getDate()} &mdash;
	 * bukan {@code new Date()} &mdash; supaya seluruh aplikasi memakai satu sumber waktu
	 * yang sama (termasuk saat waktu server digeser untuk keperluan pengujian).</p>
	 *
	 * <p>Nilai ini murni informatif: tidak dipakai sebagai penentu urutan pada
	 * {@code ambil()} (yang mengurutkan dengan {@code Order.desc("id")}), tidak dipakai
	 * sebagai validator cache berkas ({@code AmbilLampiran} menurunkan ETag dari ukuran
	 * dan waktu ubah <i>berkas fisik</i>, bukan dari kolom ini), dan tidak ikut menentukan
	 * penamaan berkas.</p>
	 *
	 * @return waktu perubahan terakhir, dapat {@code null} pada baris warisan lama
	 */
	public abstract Date getTanggal_dirubah();

	/**
	 * Alamat berkas hasil penyimpanan, sebagaimana disimpan pada kolom entitas konkret.
	 *
	 * <p>Berbeda dari {@code getLink()} yang merupakan tautan luar yang diisi pengguna,
	 * nilai ini merupakan alamat yang dihasilkan sistem penyimpanan. Tidak semua subclass
	 * benar-benar mengisinya; sebagian mengembalikan {@code null} secara permanen dan
	 * mengandalkan {@code ambilFile()} milik {@link FileFoto} untuk menemukan berkasnya.</p>
	 *
	 * <p>Pasangan penulisnya adalah {@link #setUrl(String)}. Karena pasangan
	 * getter/setter ini dideklarasikan abstrak di sini, seluruh subclass wajib
	 * menyediakannya walaupun tidak memakainya &mdash; sehingga ketiadaan nilai bukan
	 * pertanda kesalahan.</p>
	 *
	 * @return alamat berkas, umumnya {@code null} pada sebagian besar subclass
	 */
	public abstract String getUrl();

	/**
	 * Menyetel alamat berkas hasil penyimpanan; pasangan penulis {@link #getUrl()}.
	 *
	 * <p>Setter murni tanpa validasi maupun efek samping pada implementasi subclass:
	 * tidak ada normalisasi skema, tidak ada pemeriksaan panjang, dan tidak ada
	 * pembersihan karakter. Pemanggil yang menyusun nilainya dari masukan luar
	 * bertanggung jawab atas validasinya sendiri.</p>
	 *
	 * @param url alamat berkas yang hendak disimpan; boleh {@code null}
	 */
	public abstract void setUrl(String url);

	/**
	 * Membuat tombol ZK untuk mengunggah lampiran <b>langsung ke Google Drive</b>, sebagai
	 * alternatif jalur unggah biasa yang menyimpan isi berkas ke kolom biner basis data.
	 *
	 * <p><b>Cara kerja.</b> Seluruh pekerjaan didelegasikan ke
	 * {@code AmbilDataLampiranFileLain}: parameter yang diterima method ini dikemas apa
	 * adanya menjadi satu objek dialog, {@code eventListener} pemanggil dipasang sebagai
	 * penerima hasil, lalu {@code tampilkanTombolUploadGDrive("")} menghasilkan
	 * {@code Toolbarbutton} yang sudah membawa perilakunya sendiri. Method ini hanya
	 * menambahkan dua sentuhan tampilan: ukuran huruf 9px agar seragam dengan tombol
	 * lampiran lain, dan atribut {@code "janganDisabled"} yang menjadi penanda bagi
	 * mekanisme penguncian formulir agar tombol ini tetap dapat diklik meski formulir
	 * induknya sedang dinonaktifkan.</p>
	 *
	 * <p><b>Perbedaan dengan {@code tampilkanTombolUpload(...)}.</b> Selain tujuan
	 * penyimpanan, ada satu beda perilaku yang mudah terlewat: argumen terakhir
	 * konstruktor {@code AmbilDataLampiranFileLain} di sini bernilai {@code false},
	 * sedangkan pada {@code tampilkanTombolUpload} bernilai {@code true}. Argumen itu
	 * menentukan apakah objek diperlakukan sebagai dialog modal yang dipasang ke pohon
	 * komponen halaman. Di sini objeknya hanya menjadi pabrik tombol, bukan jendela;
	 * karena itu method ini tidak menyentuh {@code ExecutionsCtrl} sama sekali dan lebih
	 * aman dipanggil dari konteks selain penanganan event.</p>
	 *
	 * <p><b>Parameter yang diterima tetapi tidak dipakai.</b> {@code downloadButton},
	 * {@code hapusButton}, dan {@code setelahUpload} sengaja ikut dideklarasikan agar
	 * tanda tangan method ini sejajar dengan {@code tampilkanTombolUpload(...)} sehingga
	 * pemanggil dapat berpindah jalur unggah tanpa mengubah daftar argumen &mdash; namun
	 * ketiganya <b>tidak dipakai</b> pada implementasi ini. Akibat praktisnya: setelah
	 * unggah lewat tombol ini, pembaruan label tombol unduh, penampakan tombol hapus, dan
	 * pratinjau di layar sepenuhnya menjadi tanggung jawab {@code eventListener} yang
	 * dikirim pemanggil. Jangan berasumsi tampilan akan menyegarkan dirinya sendiri
	 * seperti pada jalur unggah biasa.</p>
	 *
	 * <p>Nilai {@code ref} dan {@code usingId} diteruskan apa adanya ke dialog dan kelak
	 * dipakai untuk menautkan hasil unggah; makna keduanya sama persis dengan yang
	 * dijelaskan pada {@code ambil(Boolean, Serializable, String, int, Class, boolean,
	 * String)}, termasuk konsekuensi {@code usingId=true} yang mematikan penyaringan
	 * {@code jenis}.</p>
	 *
	 * @param downloadButton    tombol unduh milik pemanggil; <b>tidak dipakai</b> di sini
	 * @param hapusButton       tombol hapus milik pemanggil; <b>tidak dipakai</b> di sini
	 * @param eventListener     penerima hasil unggah; satu-satunya jalan bagi pemanggil
	 *                          untuk mengetahui bahwa berkas sudah terunggah
	 * @param setelahUpload     penata pratinjau; <b>tidak dipakai</b> di sini
	 * @param fileFotoLain      lampiran yang sudah ada, bila ada; <b>tidak dipakai</b> di sini
	 * @param jenis             penanda jenis lampiran yang akan dilekatkan pada hasil unggah
	 * @param hanyaIcon         menandai gaya tampilan ringkas; <b>tidak dipakai</b> di sini
	 *                          karena label tombol selalu dikosongkan
	 * @param cutomUkuranUpload batas ukuran unggah khusus dalam satuan yang dipahami dialog;
	 *                          {@code null} berarti memakai batas bawaan
	 * @param keterangan        teks keterangan lampiran yang ditampilkan pada dialog
	 * @param jurusan           combo jurusan bila konfigurasi mengizinkan pembedaan per
	 *                          jurusan; boleh {@code null}
	 * @param harusPdf          {@code true} bila hanya berkas PDF yang boleh diunggah
	 * @param ref               acuan baris pemilik lampiran
	 * @param usingId           {@code true} bila {@code ref} bermakna primary key lampiran
	 * @param clazz             kelas entitas berkas tujuan penyimpanan
	 * @return tombol ZK siap pasang; pemanggil masih harus menambahkannya ke wadah
	 */
	public static Toolbarbutton tampilkanTombolUploadGdrive(final Button downloadButton, final Button hapusButton,
			final EventListener eventListener, final SetelahUpload setelahUpload, FileFotoLain fileFotoLain,
			final String jenis, final Boolean hanyaIcon, final Integer cutomUkuranUpload, final String keterangan,
			final Combobox jurusan, final Boolean harusPdf, final Long ref, final Boolean usingId, final Class clazz) {
		AmbilDataLampiranFileLain ambilDataLampiranFileLain = new AmbilDataLampiranFileLain(jenis, cutomUkuranUpload,
				keterangan, jurusan, harusPdf, ref, usingId, clazz, false);
		ambilDataLampiranFileLain.setEventListener(eventListener);
		Toolbarbutton ambil = ambilDataLampiranFileLain.tampilkanTombolUploadGDrive("");
		ambil.setStyle("font-size:9px");
		ambil.setAttribute("janganDisabled", true);
		return ambil;
	}

	// --- Helper Methods ---

	/**
	 * Mendapatkan nama field relasi berdasarkan class.
	 *
	 * <p>Satu-satunya pembaca {@code RELASI_MAP}, sehingga seluruh perilaku yang bergantung
	 * pada pemetaan kelas &rarr; kolom acuan pemilik berpangkal di sini: klausa pencarian
	 * pada {@code ambil()}, pemilihan setter reflektif pada {@code createFileFotoLain()},
	 * kolom yang ditimpa sentinel pada {@code hapusAtauUpdate()}, dan penentuan perlu
	 * tidaknya <i>re-attachment</i> pada {@code refreshFotoTemporaryGDrive()}.</p>
	 *
	 * <p><b>Perilaku fail-open yang perlu disadari.</b> Kelas yang tidak terdaftar tidak
	 * ditolak dan tidak menimbulkan peringatan apa pun; nilainya jatuh ke {@code "id"}.
	 * Padahal {@code "id"} bukan nilai netral: ia adalah golongan khusus yang membuat
	 * {@code ambil()} mencocokkan {@code ref} ke primary key sekaligus <b>mematikan
	 * penyaringan {@code jenis}</b>, dan membuat cabang <i>soft delete</i> pada
	 * {@code hapusAtauUpdate()} tidak melakukan apa pun. Entitas berkas baru yang lupa
	 * didaftarkan di {@code RELASI_MAP} karena itu akan langsung mewarisi dua perilaku
	 * tersebut tanpa ada yang menyadarinya. Pencocokan memakai identitas kelas persis
	 * ({@code Map.get}), jadi subclass dari kelas terdaftar pun tidak ikut mewarisi
	 * pemetaan induknya.</p>
	 *
	 * @param clazz kelas entitas berkas yang ditanyakan
	 * @return nama properti acuan pemilik, atau {@code "id"} bila kelas tidak terdaftar
	 */
	private static String getRefField(Class clazz) {
		String field = RELASI_MAP.get(clazz);
		return field != null ? field : "id";
	}

	/**
	 * Mengambil nama properti setter (misal: "mahasiswa" -&gt; "setMahasiswa").
	 *
	 * <p>Dipakai {@code createFileFotoLain()} untuk menemukan setter kolom acuan pemilik
	 * secara reflektif, sehingga satu jalur penyimpanan dapat melayani belasan entitas
	 * berkas dengan nama kolom yang berbeda-beda tanpa percabangan.</p>
	 *
	 * <p>Nama {@code "id"} diperlakukan khusus dan dipetakan ke {@code "setId"} apa adanya
	 * agar tidak menghasilkan {@code "setId"} berhuruf besar ganda; selebihnya cukup
	 * mengapitalkan huruf pertama dengan {@code StringUtils.capitalize}. Perhatikan bahwa
	 * pemeriksaan {@code "id"} bersifat <i>case-insensitive</i> sedangkan cabang lain
	 * tidak, dan bahwa hasilnya tidak pernah diverifikasi keberadaannya di sini &mdash;
	 * kegagalan menemukan setter baru muncul sebagai {@code NoSuchMethodException} di
	 * pemanggil, yang pada {@code createFileFotoLain()} sengaja hanya dicatat lalu
	 * diabaikan.</p>
	 *
	 * @param fieldName nama properti acuan pemilik dari {@code RELASI_MAP}
	 * @return nama method setter yang diharapkan ada pada entitas
	 */
	private static String getSetterName(String fieldName) {
		if ("id".equalsIgnoreCase(fieldName))
			return "setId";
		return "set" + StringUtils.capitalize(fieldName);
	}

	// --- Core Logic ---

	/**
	 * Menyusun alamat untuk mengakses lampiran ini, dengan pilihan bawaan: boleh memakai
	 * jalur berkas statis bila berkasnya ada ({@code ketemu = true}) dan mengembalikan
	 * alamat mutlak lengkap dengan protokol dan host ({@code relative = false}).
	 *
	 * <p>Bentuk yang paling banyak dipakai pemanggil, termasuk oleh {@code
	 * ais.action.servlet.Data} saat menyusun jawaban JSON berisi {@code url} lampiran.
	 * Karena {@code relative = false}, alamat yang dihasilkan mengandung host permintaan
	 * yang sedang berjalan &mdash; jangan menyimpan hasilnya ke basis data atau ke berkas
	 * yang berumur panjang, sebab alamat itu ikut basi ketika aplikasi diakses dari nama
	 * host lain.</p>
	 *
	 * <p>Efek samping dan seluruh rincian perilakunya dijelaskan pada
	 * {@link #createLinkUri(boolean, boolean)}; method ini hanya meneruskan.</p>
	 *
	 * @return alamat mutlak lampiran ini
	 * @throws Exception bila penyusunan alamat gagal total di jalur delegasi
	 */
	public String createLinkUri() throws Exception {
		return createLinkUri(true, false);
	}

	/**
	 * Sama dengan {@link #createLinkUri()} namun pemanggil dapat menentukan sendiri apakah
	 * jalur berkas statis boleh dipakai.
	 *
	 * <p>Nilai {@code ketemu = false} memaksa alamat yang dihasilkan selalu berupa
	 * {@code /al?d=...} yang dilayani servlet {@code AmbilLampiran}, tidak pernah berupa
	 * alamat berkas statis {@code /f<prefix>/...}. Alasan praktis dipilihnya {@code false}
	 * pada beberapa pemanggil tercatat pada {@code setupDownloadButtonAction()}: alamat
	 * berkas statis dapat menjawab 404 dan memicu halaman kesalahan yang memuat ulang
	 * dirinya sendiri tanpa henti, sehingga pratinjau tidak pernah selesai.</p>
	 *
	 * @param ketemu {@code true} mengizinkan pemakaian alamat berkas statis bila berkasnya
	 *               benar-benar ada; {@code false} memaksa melalui servlet
	 * @return alamat mutlak lampiran ini
	 * @throws Exception bila penyusunan alamat gagal total di jalur delegasi
	 */
	public String createLinkUri(boolean ketemu) throws Exception {
		return createLinkUri(ketemu, false);
	}

	/**
	 * Menyusun alamat akses lampiran ini, sekaligus &mdash; sebagai efek samping &mdash;
	 * <b>menyalin berkas fisiknya ke direktori media</b> agar dapat dilayani secara statis.
	 *
	 * <p><b>Tiga jalur keluaran, diperiksa berurutan.</b></p>
	 * <ol>
	 *   <li><b>Google Drive.</b> Bila kolom {@code gdrive} terisi, seluruh langkah lain
	 *       dilewati dan alamat ekspor Google Drive dikembalikan. Isi berkas memang tidak
	 *       ada di basis data pada kasus ini.</li>
	 *   <li><b>Penyalinan ke direktori media.</b> Bila berkas fisiknya ada dan tidak
	 *       kosong, sebuah salinan ditulis ke akar direktori media dengan nama hasil
	 *       enkripsi {@code id + nama kelas sederhana}. Langkah ini dibungkus
	 *       {@code try-catch} yang sengaja diam: berkas fisik bisa hilang atau baris ini
	 *       sebenarnya hanya tautan lama, dan halaman tidak boleh gagal hanya karena
	 *       salinan cache tidak dapat dibuat.</li>
	 *   <li><b>Penyusunan alamat.</b> Apa pun hasil langkah 2, alamat akhirnya disusun
	 *       {@code LampiranLain.ambilLinkLampiranLain(...)} dari metadata baris ini.</li>
	 * </ol>
	 *
	 * <p><b>Yang perlu diperhatikan pada langkah 2.</b> Nama berkas salinan dibentuk dari
	 * {@code Common.desEncrypter.get().encrypt(getId() + getClass().getSimpleName())} yang
	 * kemudian dibuang seluruh tanda bacanya kecuali {@code _} dan {@code -}, lalu
	 * di-{@code URLEncoder.encode}. Sifat penting yang mengikutinya: (a) enkripsinya
	 * <i>deterministik</i>, sehingga satu lampiran selalu menghasilkan nama berkas yang
	 * sama pada setiap pemanggilan &mdash; inilah yang membuat pemeriksaan
	 * {@code !fileTujuan.exists()} berfungsi sebagai cache; (b) pembuangan tanda baca
	 * memperkecil ruang nama, sehingga secara teori dua ciphertext berbeda dapat runtuh
	 * menjadi nama yang sama, dan yang menang adalah yang lebih dahulu tersalin karena
	 * salinan berikutnya dilewati oleh pemeriksaan {@code exists()} itu juga; (c) salinan
	 * diletakkan di <b>akar</b> direktori media, bukan di dalam
	 * {@code segmenFolderBerkas()} per entitas seperti yang dipakai jalur lain, sehingga
	 * pemisahan folder per entitas tidak berlaku untuk salinan ini.</p>
	 *
	 * <p>Perlu dicatat pula bahwa berkas hasil langkah 2 berada di dalam direktori media
	 * yang dilayani secara statis. Siapa pun yang dapat menebak atau memperoleh nama
	 * berkas tersebut dapat mengunduhnya tanpa melewati servlet {@code AmbilLampiran}.
	 * Kerahasiaan lampiran pada jalur ini karena itu bersandar sepenuhnya pada
	 * ketidakterdugaan hasil enkripsi nama, bukan pada pemeriksaan hak akses.</p>
	 *
	 * <p>Method ini juga menjadi contoh hubungan dua arah antara kelas ini dan
	 * {@code LampiranLain}: kelas induk memanggil method statis milik subclass-nya untuk
	 * menyusun alamat, sementara method statis tersebut pada gilirannya kembali memanggil
	 * {@code ambilLinkLampiranLain(FileFotoLain, ...)} di kelas ini. Perubahan pada salah
	 * satu sisi selalu berdampak pada sisi yang lain.</p>
	 *
	 * @param ketemu   {@code true} mengizinkan alamat berkas statis {@code /f<prefix>/...}
	 *                 bila berkasnya ada; {@code false} memaksa melalui {@code /al?d=...}
	 * @param relative {@code true} menghasilkan alamat relatif terhadap konteks aplikasi
	 *                 ({@code Common.ROOT}), {@code false} menghasilkan alamat mutlak
	 *                 lengkap dengan protokol dan host permintaan yang sedang berjalan
	 * @return alamat akses lampiran ini; pada kegagalan penyusunan, jalur delegasi
	 *         mengembalikan alamat ikon pengganti alih-alih melempar
	 * @throws Exception bila penyusunan alamat gagal di luar penanganan jalur delegasi
	 */
	public String createLinkUri(boolean ketemu, boolean relative) throws Exception {
		if (getGdrive() != null && !getGdrive().trim().isEmpty()) {
			return exportGDriveUrl();
		}

		try {
			File f = ambilFile();
			if (f != null && f.exists() && f.length() > 0L) {
				String ex = FilenameUtils.getExtension(getNama());
				String safeId = Common.desEncrypter.get().encrypt(getId() + this.getClass().getSimpleName())
						.replaceAll("[\\p{Punct}&&[^_-]]+", "");
				String fileName = URLEncoder.encode(safeId + "." + ex, "UTF-8");
				File fileTujuan = new File(CommonMedia.getMediaDirectory().getAbsolutePath() + "/" + fileName);

				if (!fileTujuan.exists()) {
					FileUtils.copyFile(f, fileTujuan);
				}
			}
		} catch (Exception e) {
			// Berkas fisik bisa hilang/berupa link lama. Link lampiran tetap dibangun
			// lewat metadata agar halaman tidak gagal hanya karena cache file tidak ada.
		}

		String uri = LampiranLain.ambilLinkLampiranLain(this, false, true, ambilClazz(), ketemu, relative);

		return uri;
	}

	/**
	 * Memaksa Hibernate "menyadari" kembali relasi pemilik lampiran ini dengan cara
	 * membaca lalu menulis balik nilai kolom acuannya melalui refleksi.
	 *
	 * <p><b>Mengapa terlihat tidak melakukan apa-apa.</b> Isi method ini pada dasarnya
	 * adalah {@code setX(getX())} &mdash; nilainya tidak berubah sama sekali. Yang
	 * dituju bukan perubahan nilai, melainkan efek sampingnya pada objek: pemanggilan
	 * getter memaksa proxy Hibernate yang masih malas ({@code lazy}) untuk terwujud, dan
	 * pemanggilan setter menandai properti tersebut sebagai tersentuh. Pada objek yang
	 * berasal dari cache JSON ({@code Common.convertToObject}) atau yang sudah terlepas
	 * dari session, langkah ini membuat relasinya kembali dapat dipakai tanpa perlu
	 * membuka session baru.</p>
	 *
	 * <p><b>Dua golongan yang sengaja dilewati.</b> {@code "id"} dilewati karena bukan
	 * relasi melainkan primary key &mdash; menulis balik primary key pada entitas yang
	 * dikelola adalah tindakan yang justru berbahaya. {@code "tbmuser"} dilewati karena
	 * kolomnya bertipe {@code String} pada {@code FotoAdmin} dan bukan relasi ORM, jadi
	 * tidak ada proxy yang perlu diwujudkan.</p>
	 *
	 * <p><b>Kegagalan sengaja dibiarkan senyap.</b> Bila getter/setter yang dicari tidak
	 * ada, {@code NoSuchMethodException} hanya dicatat ke {@code ErrorAuditUtil} lalu
	 * diabaikan, mengikuti perilaku versi lama yang menggantikan blok {@code if-else}
	 * raksasa. Artinya method ini <b>tidak pernah menjamin</b> relasinya benar-benar
	 * tersegarkan; pemanggil tidak boleh menganggap keberhasilannya sebagai prasyarat.
	 * Perhatikan juga bahwa {@code getRefField(this.getClass())} memakai kelas runtime,
	 * sehingga pada instance yang berupa proxy Hibernate pemetaan bisa meleset ke default
	 * {@code "id"} dan seluruh isi method dilewati tanpa jejak.</p>
	 */
	public void refreshFotoTemporaryGDrive() {
		// Menggunakan reflection ringan untuk melakukan re-attachment entity
		// Menggantikan blok if-else raksasa
		try {
			String fieldName = getRefField(this.getClass());
			if (!"id".equals(fieldName) && !"tbmuser".equals(fieldName)) {
				String getterName = "get" + StringUtils.capitalize(fieldName);
				String setterName = "set" + StringUtils.capitalize(fieldName);
				Method getter = this.getClass().getMethod(getterName);
				Method setter = this.getClass().getMethod(setterName, getter.getReturnType());
				Object value = getter.invoke(this);
				setter.invoke(this, value);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/file/FileFotoLain.java:194");
			// Ignore if method not found, fallback to silent fail similar to original code
		}
	}

	/**
	 * Membaca <b>berkas cache metadata</b> lampiran &mdash; lapis penyimpanan ketiga yang
	 * dijelaskan pada Javadoc kelas ini &mdash; dan mengembalikan isinya sebagai teks JSON.
	 *
	 * <p><b>Peran cache ini.</b> {@code ambil()} memanggil method ini <i>lebih dahulu</i>
	 * daripada membuka koneksi basis data. Bila isinya bukan penanda kosong, seluruh query
	 * dilewati dan objek lampiran dibangun ulang dari JSON tersebut. Dengan begitu halaman
	 * yang menampilkan puluhan lampiran sekaligus tidak menghasilkan puluhan koneksi.</p>
	 *
	 * <p><b>Tiga nilai kembalian yang bermakna berbeda:</b></p>
	 * <ul>
	 *   <li>{@code VOMahasiswa.dataJSON} (penanda kosong bawaan) &rarr; cache belum
	 *       pernah diisi atau tidak terbaca &mdash; pemanggil harus bertanya ke basis data.
	 *       Nilai ini juga dikembalikan pada setiap kegagalan, termasuk berkas tidak ada,
	 *       gagal membaca, maupun {@code jenis} yang tidak dapat di-<i>encode</i>.</li>
	 *   <li>{@code "0"} &rarr; hasil <b>negatif yang di-cache</b>: basis data pernah
	 *       ditanya dan memang tidak ada barisnya. Pemanggil tidak perlu bertanya lagi.</li>
	 *   <li>Teks JSON &rarr; metadata baris lampiran hasil
	 *       {@code Common.convertToJsonObject(...)}, lengkap dengan {@code class} dan
	 *       {@code id} yang ditambahkan {@code ambil()} sebelum menulisnya.</li>
	 * </ul>
	 *
	 * <p><b>Bentuk kunci cache.</b> Berkasnya ditentukan oleh
	 * {@code Common.getFileLocation(clazz, ref, prefix + "_lampiran_" + encode(jenis) + "_"
	 * + ref)}. Perhatikan bahwa {@code prefix} yang dikirim {@code ambil()} sudah
	 * memuat pembeda {@code usingId}: {@code "data_baru_"} untuk pencarian berbasis pemilik
	 * dan {@code "data_baru__id"} untuk pencarian berbasis primary key. <b>Satu baris
	 * lampiran karena itu dapat memiliki dua berkas cache yang terpisah</b>, dan keduanya
	 * tidak pernah dibersihkan bersamaan &mdash; sifat inilah yang menjadi akar
	 * ketidaksinkronan yang dicatat pada {@code delete()} dan {@code resetLokasi()}.</p>
	 *
	 * <p>Cache ini <b>tidak mengandung unsur identitas pengguna</b> sama sekali: kuncinya
	 * hanya kelas, acuan, dan jenis. Isinya karena itu dipakai bersama oleh semua sesi,
	 * dan tidak dapat dijadikan tempat menyimpan hasil yang sudah tersaring hak akses.</p>
	 *
	 * @param prefix awalan kunci cache; oleh pemanggil di kelas ini selalu
	 *               {@code "data_baru_"} atau {@code "data_baru__id"}
	 * @param ref    acuan baris pemilik, atau primary key lampiran bila varian {@code _id}
	 * @param jenis  penanda jenis lampiran; ikut membentuk nama berkas cache
	 * @param clazz  kelas entitas berkas; ikut menentukan lokasi berkas cache
	 * @return teks JSON metadata, {@code "0"} untuk hasil negatif, atau
	 *         {@code VOMahasiswa.dataJSON} bila cache belum ada/gagal dibaca
	 */
	public static String ambilLokasi(String prefix, Serializable ref, String jenis, Class clazz) {
		try {
			File file = Common.getFileLocation(clazz, ref,
					prefix + "_lampiran_" + URLEncoder.encode(jenis, "UTF-8") + "_" + ref.toString());
			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) {
			return VOMahasiswa.dataJSON;
		}
	}

	/**
	 * Membuang isi berkas cache metadata untuk satu kombinasi
	 * ({@code clazz}, {@code ref}, {@code jenis}, varian {@code usingId}), sehingga
	 * pemanggilan {@code ambil()} berikutnya terpaksa bertanya kembali ke basis data.
	 *
	 * <p>Pengosongan dilakukan dengan menulis teks kosong, bukan menghapus berkasnya.
	 * {@code ambilLokasi()} memperlakukan isi kosong sama seperti cache yang belum
	 * pernah ada, jadi hasilnya setara namun tanpa risiko gagal hapus.</p>
	 *
	 * <p><b>Yang membuat method ini mudah dipakai keliru.</b> Kunci cache dibedakan oleh
	 * parameter {@code usingId}: {@code false} membersihkan berkas berawalan
	 * {@code "data_baru_"}, {@code true} membersihkan {@code "data_baru__id"}. Keduanya
	 * merujuk baris lampiran yang sama tetapi merupakan <b>dua berkas berbeda</b>. Satu
	 * kali panggilan hanya membersihkan salah satunya. Karena itu jalur yang mengubah atau
	 * menghapus lampiran wajib memanggil method ini untuk <i>kedua</i> varian bila baris
	 * tersebut memang pernah diakses lewat kedua jalur &mdash; dan hal itu tidak selalu
	 * terjadi. Contoh nyatanya ada pada {@code delete()} di kelas ini, yang mengunci
	 * {@code usingId = false} sehingga cache varian {@code _id} milik baris yang sama
	 * tidak pernah dibersihkan. Selama cache itu masih ada, {@code ambil()} dengan
	 * {@code usingId=true} akan tetap membangun kembali objek lampiran dari metadata lama
	 * beserta jalan menuju berkas fisiknya.</p>
	 *
	 * <p>Pemanggilnya tersebar: {@code performDelete()} dan {@code hapusAtauUpdate()} di
	 * kelas ini, jalur unggah pada {@code tampilkanTombolUpload()}, serta pembungkus
	 * {@code LampiranLain.resetLokasi(Boolean, Long, String)} yang hanya meneruskan ke
	 * sini dengan {@code clazz = LampiranLain.class}.</p>
	 *
	 * @param usingId {@code true} membersihkan cache varian primary key
	 *                ({@code "data_baru__id"}), {@code false} varian berbasis pemilik
	 * @param ref     acuan yang membentuk kunci cache
	 * @param jenis   penanda jenis lampiran yang membentuk kunci cache
	 * @param clazz   kelas entitas berkas yang membentuk lokasi cache
	 */
	public static void resetLokasi(Boolean usingId, Serializable ref, String jenis, Class clazz) {
		String keyFilePrefix = "data_baru_" + (usingId ? "_id" : "");
		tulisLokasi("", keyFilePrefix, ref, jenis, clazz);
	}

	/**
	 * Menyusun tautan {@code /al?d=<token>} &mdash; bentuk baku alamat lampiran yang
	 * dilayani servlet {@code ais.action.servlet.AmbilLampiran} (dipetakan ke
	 * {@code /al} pada {@code web.xml}).
	 *
	 * <p><b>Isi token.</b> Seluruh parameter pencarian dikemas menjadi satu objek JSON
	 * berisi {@code ref}, {@code rezise}, {@code jurusan} (selalu kosong dari sini),
	 * {@code jenis}, {@code usingId}, {@code download}, dan {@code clazz} (nama kelas
	 * lengkap). JSON itu dienkripsi dengan {@code Common.desEncrypter} lalu
	 * di-<i>URL-encode</i> menjadi nilai parameter {@code d}.</p>
	 *
	 * <p><b>Enkripsi di sini bukan kendali akses.</b> Ini bagian terpenting untuk
	 * dipahami sebelum menambah pemanggil baru. Sisi penerima, yaitu
	 * {@code AmbilLampiran.process()}, membaca setiap nilai dengan pola
	 * "ambil dari token bila ada, <b>kalau tidak ambil dari parameter permintaan biasa</b>"
	 * &mdash; lihat baris 308-312 dan 367-378 pada berkas servlet tersebut. Artinya
	 * {@code ref}, {@code clazz}, {@code jenis}, {@code jurusan}, {@code download}, dan
	 * {@code usingId} semuanya dapat dikirim sebagai parameter kueri biasa tanpa token
	 * sama sekali. Enkripsi pada method ini hanya membuat tautan terlihat rapi dan tidak
	 * mudah diubah <i>tanpa sengaja</i>; ia sama sekali tidak mencegah siapa pun menyusun
	 * permintaan sendiri dengan nilai pilihannya. Setiap penjagaan yang benar-benar
	 * membatasi akses harus ditempatkan di sisi servlet atau di dalam {@code ambil()},
	 * bukan diandalkan dari kerahasiaan token ini.</p>
	 *
	 * <p>Perhatikan pula bahwa {@code clazz.getName()} ikut masuk ke dalam token dan pada
	 * sisi servlet dipakai untuk {@code Class.forName(...)}. Nilai kelas karena itu
	 * berasal dari permintaan, bukan dari konteks layar yang sedang dibuka.</p>
	 *
	 * @param ref      acuan yang akan dicari servlet; primary key lampiran bila
	 *                 {@code usingId} bernilai {@code true}
	 * @param jenis    penanda jenis lampiran; diabaikan servlet bila {@code usingId}
	 * @param usingId  {@code true} membuat servlet mencocokkan {@code ref} ke primary key
	 *                 sekaligus mematikan penyaringan {@code jenis}
	 * @param download {@code true} meminta berkas disajikan sebagai unduhan
	 *                 ({@code Content-Disposition: attachment}) alih-alih ditampilkan
	 * @param clazz    kelas entitas berkas yang akan dikueri servlet
	 * @param relative {@code true} menghasilkan alamat relatif terhadap konteks aplikasi
	 * @param rezise   {@code true} meminta servlet menyajikan thumbnail 128px alih-alih
	 *                 berkas gambar aslinya
	 * @return tautan lengkap menuju servlet lampiran
	 * @throws Exception bila enkripsi atau <i>encoding</i> gagal
	 */
	private static String ambilLinkLampiranLainLink(Serializable ref, String jenis, Boolean usingId, Boolean download,
			Class clazz, boolean relative, boolean rezise) throws Exception {
		MyJSONObject jsonObject = new MyJSONObject();
		jsonObject.put("ref", ref);
		jsonObject.put("rezise", rezise);
		jsonObject.put("jurusan", "");
		jsonObject.put("jenis", jenis);
		jsonObject.put("usingId", usingId.toString());
		jsonObject.put("download", download.toString());
		jsonObject.put("clazz", clazz.getName());
		String encript = Common.desEncrypter.get().encrypt(jsonObject.toString());
		return (relative ? Common.ROOT : Common.getRequestHostWithProtocol()) + "/al?d="
				+ URLEncoder.encode(encript, "UTF-8");
	}

	/**
	 * Menyusun tautan menuju sebuah <b>berkas di disk</b> &mdash; bukan menuju baris
	 * lampiran di basis data &mdash; dengan alamat mutlak lengkap protokol dan host.
	 *
	 * <p>Varian ringkas dari {@link #ambilLinkLampiranLain(File, boolean)}; seluruh
	 * penjelasan bentuk token, batasan direktori, dan konsekuensinya ada di sana.</p>
	 *
	 * @param file berkas yang hendak disajikan
	 * @return tautan mutlak menuju berkas tersebut
	 * @throws Exception bila enkripsi atau <i>encoding</i> gagal
	 */
	public static String ambilLinkLampiranLain(File file) throws Exception {
		return ambilLinkLampiranLain(file, false);
	}

	/**
	 * Menyusun tautan {@code /al?d=<token>} yang menunjuk <b>langsung ke sebuah berkas di
	 * disk</b> lewat jalur mutlaknya, tanpa melibatkan baris lampiran mana pun.
	 *
	 * <p>Berbeda dari saudara-saudaranya yang mengemas {@code ref}/{@code jenis}/{@code
	 * clazz}, token di sini hanya berisi satu kunci: {@code "file"} berisi
	 * {@code file.getAbsolutePath()}. Servlet {@code AmbilLampiran} memeriksa kunci itu
	 * paling awal (baris 287-301 pada berkas servlet) dan bila terisi langsung menyajikan
	 * berkasnya, tidak pernah sampai ke jalur pencarian basis data.</p>
	 *
	 * <p><b>Pembatas yang menjaga jalur ini.</b> Karena isi token adalah jalur berkas apa
	 * pun, jalur ini pernah menjadi celah pembacaan berkas sembarang. Penjagaannya
	 * sekarang berada di sisi servlet: {@code isDalamDirektoriDiizinkan(File)} menolak
	 * setiap berkas yang &mdash; setelah dinormalkan menjadi <i>canonical path</i>
	 * sehingga {@code ..} tidak dapat dipakai untuk keluar &mdash; tidak berada di dalam
	 * direktori media atau direktori aplikasi. Komentar pada penjaga tersebut menyebut
	 * method inilah satu-satunya pembuat token berkunci {@code "file"}. Pernyataan itu
	 * benar untuk kode di dalam paket ini; menambah pembuat token semacam ini di tempat
	 * lain berarti memperluas permukaan yang harus dijaga penjaga tadi, jadi sebaiknya
	 * dihindari.</p>
	 *
	 * <p>Bila berkasnya kelak dipindah atau dihapus, tautan yang sudah terlanjur beredar
	 * tidak dapat dipulihkan: tidak ada baris basis data yang menjadi acuan sehingga
	 * servlet akan jatuh ke jalur pencarian biasa dan berakhir pada ikon pengganti.</p>
	 *
	 * @param file     berkas yang hendak disajikan; jalur mutlaknya masuk ke dalam token
	 * @param relative {@code true} menghasilkan alamat relatif terhadap konteks aplikasi
	 *                 ({@code Common.ROOT}), {@code false} alamat mutlak dengan host
	 *                 permintaan yang sedang berjalan
	 * @return tautan menuju berkas tersebut
	 * @throws Exception bila enkripsi atau <i>encoding</i> gagal
	 */
	public static String ambilLinkLampiranLain(File file, boolean relative) throws Exception {
		MyJSONObject jsonObject = new MyJSONObject();
		jsonObject.put("file", file.getAbsolutePath());
		String encript = Common.desEncrypter.get().encrypt(jsonObject.toString());
		return (relative ? Common.ROOT : Common.getRequestHostWithProtocol()) + "/al?d="
				+ URLEncoder.encode(encript, "UTF-8");
	}

	// Overloads for ambilLinkLampiranLain
	/**
	 * Menyusun alamat tampil/unduh untuk satu lampiran, dengan pilihan bawaan: boleh
	 * memakai alamat berkas statis bila berkasnya ada, alamat mutlak, tanpa thumbnail.
	 *
	 * <p>Bentuk terpendek dari keluarga {@code ambilLinkLampiranLain(FileFotoLain, ...)};
	 * seluruh perilaku, urutan pemeriksaan, dan konsekuensinya dijelaskan pada
	 * {@link #ambilLinkLampiranLain(FileFotoLain, Boolean, Boolean, Class, boolean,
	 * boolean, boolean)}.</p>
	 *
	 * @param fileFoto lampiran yang hendak ditautkan; boleh {@code null}
	 * @param usingId  {@code true} membuat tautan meminta pencocokan ke primary key
	 * @param download {@code true} meminta berkas disajikan sebagai unduhan
	 * @param clazz    kelas entitas berkas yang dicantumkan pada tautan
	 * @return alamat lampiran, atau alamat ikon pengganti bila tidak dapat disusun
	 * @throws Exception praktis tidak pernah dilempar; jalur utamanya menangkap semua
	 *                   kegagalan dan jatuh ke ikon pengganti
	 */
	public static String ambilLinkLampiranLain(FileFotoLain fileFoto, Boolean usingId, Boolean download, Class clazz)
			throws Exception {
		return ambilLinkLampiranLain(fileFoto, usingId, download, clazz, true, false, false);
	}

	/**
	 * Sama dengan {@link #ambilLinkLampiranLain(FileFotoLain, Boolean, Boolean, Class)}
	 * namun pemanggil menentukan sendiri apakah alamat berkas statis boleh dipakai.
	 *
	 * <p>Varian inilah yang dipakai {@code setupDownloadButtonAction()} dengan
	 * {@code ketemu = false} untuk memaksa alamat selalu melewati servlet
	 * {@code AmbilLampiran}; alasannya tercatat pada method tersebut.</p>
	 *
	 * @param fileFoto lampiran yang hendak ditautkan; boleh {@code null}
	 * @param usingId  {@code true} membuat tautan meminta pencocokan ke primary key
	 * @param download {@code true} meminta berkas disajikan sebagai unduhan
	 * @param clazz    kelas entitas berkas yang dicantumkan pada tautan
	 * @param ketemu   {@code true} mengizinkan alamat berkas statis bila berkasnya ada
	 * @return alamat lampiran, atau alamat ikon pengganti bila tidak dapat disusun
	 * @throws Exception praktis tidak pernah dilempar
	 */
	public static String ambilLinkLampiranLain(FileFotoLain fileFoto, Boolean usingId, Boolean download, Class clazz,
			boolean ketemu) throws Exception {
		return ambilLinkLampiranLain(fileFoto, usingId, download, clazz, ketemu, false, false);
	}

	/**
	 * Sama dengan varian berparameter {@code ketemu}, ditambah pilihan alamat relatif.
	 *
	 * <p>Pakai {@code relative = true} bila hasilnya akan disematkan pada halaman yang
	 * sudah berada di dalam konteks aplikasi yang sama, sehingga alamatnya tidak ikut
	 * membekukan nama host permintaan yang sedang berjalan.</p>
	 *
	 * @param fileFoto lampiran yang hendak ditautkan; boleh {@code null}
	 * @param usingId  {@code true} membuat tautan meminta pencocokan ke primary key
	 * @param download {@code true} meminta berkas disajikan sebagai unduhan
	 * @param clazz    kelas entitas berkas yang dicantumkan pada tautan
	 * @param ketemu   {@code true} mengizinkan alamat berkas statis bila berkasnya ada
	 * @param relative {@code true} menghasilkan alamat relatif terhadap konteks aplikasi
	 * @return alamat lampiran, atau alamat ikon pengganti bila tidak dapat disusun
	 * @throws Exception praktis tidak pernah dilempar
	 */
	public static String ambilLinkLampiranLain(FileFotoLain fileFoto, Boolean usingId, Boolean download, Class clazz,
			boolean ketemu, boolean relative) throws Exception {
		return ambilLinkLampiranLain(fileFoto, usingId, download, clazz, ketemu, relative, false);
	}

	/**
	 * Menyusun alamat tampil/unduh untuk satu lampiran &mdash; bentuk lengkap tempat
	 * seluruh keputusan sebenarnya diambil.
	 *
	 * <p><b>Empat kemungkinan alamat, diperiksa berurutan dan yang pertama cocok
	 * menang:</b></p>
	 * <ol>
	 *   <li><b>Lampiran tidak ada</b> ({@code fileFoto == null}) &rarr; langsung
	 *       mengembalikan alamat ikon pengganti dari {@code iconNggakAda(clazz)}. Tidak
	 *       ada kesalahan yang dilempar; ketiadaan lampiran adalah keadaan normal.</li>
	 *   <li><b>Lampiran berupa tautan luar</b> &rarr; dikenali dari {@code getNama()} yang
	 *       persis bertuliskan {@code "Berupa link file"}; nilai {@code getLink()}
	 *       dikembalikan apa adanya. Perhatikan bahwa penanda ini berupa <i>teks yang
	 *       dibandingkan persis</i>, bukan kolom penanda tersendiri &mdash; nama berkas
	 *       yang kebetulan sama akan ikut dianggap tautan luar.</li>
	 *   <li><b>Lampiran di Google Drive</b> &rarr; alamat pratinjau Google Drive disusun
	 *       dari kolom {@code gdrive}.</li>
	 *   <li><b>Berkas fisik ditemukan</b> dan {@code ketemu = true} &rarr; alamat berkas
	 *       statis {@code /f<prefix>/<segmen>/<nama>}; bila {@code rezise = true} sebuah
	 *       thumbnail 128px dibuat lebih dahulu (sekali saja, karena berkas yang sudah ada
	 *       dipakai ulang) dan alamat thumbnail itulah yang dikembalikan.</li>
	 * </ol>
	 * <p>Bila tak satu pun terpenuhi, yang tersisa adalah tautan
	 * {@code /al?d=<token>} hasil {@code ambilLinkLampiranLainLink(...)} yang sudah
	 * disusun lebih dahulu di awal method.</p>
	 *
	 * <p><b>Tentang pemisahan folder per entitas.</b> Jalur berkas pada langkah 4 memakai
	 * {@code fileFoto.segmenFolderBerkas()} sehingga berbentuk
	 * {@code <media>/<NamaKelas>/<id>/<nama>}. Sebelumnya bentuknya hanya
	 * {@code <media>/<id>/}, sehingga dua entitas <i>berbeda</i> yang kebetulan memiliki
	 * primary key sama berbagi satu folder dan berkasnya dapat tertukar &mdash; foto
	 * seseorang muncul pada data orang lain. Segmen nama kelas itulah yang memisahkannya.
	 * Perlu dicatat bahwa {@code createLinkUri()} dan salah satu cabang penyalinan di
	 * {@code AmbilLampiran} <b>tidak</b> memakai pola ini, sehingga pemisahan tersebut
	 * tidak berlaku menyeluruh di semua jalur.</p>
	 *
	 * <p><b>Pembersihan nama berkas hanya kosmetik.</b> Nama yang dipakai untuk menyusun
	 * jalur maupun alamat hanyalah {@code getNama()} dengan spasi, {@code %}, dan
	 * {@code #} diganti garis bawah. Karakter pemisah direktori tidak disentuh sama
	 * sekali. Nama kosong diganti {@code "lampiran"}. Karena {@code getNama()} pada
	 * jalur unggah berasal dari nama berkas yang dikirim pengguna, pemanggil sebaiknya
	 * tidak menganggap nilai ini sudah bersih; pembersihan yang sesungguhnya &mdash;
	 * penggantian {@code /} dan {@code \} &mdash; hanya dilakukan
	 * {@code createFileFotoLain()} pada kasus nama yang kosong, bukan pada nama yang
	 * dikirim pengguna.</p>
	 *
	 * <p><b>Kegagalan selalu berujung ikon pengganti.</b> Seluruh badan method dibungkus
	 * {@code try-catch} yang mengembalikan alamat {@code iconNggakAda(clazz)}. Karena itu
	 * method ini praktis tidak pernah melempar, dan pemanggil tidak dapat membedakan
	 * "lampiran memang tidak ada" dari "terjadi kesalahan saat menyusun alamatnya".
	 * Untuk keperluan tampilan sifat ini diinginkan; untuk keperluan diagnosis ia
	 * menyembunyikan masalah, karena kesalahan yang tertangkap juga tidak dicatat.</p>
	 *
	 * @param fileFoto lampiran yang hendak ditautkan; {@code null} menghasilkan ikon
	 *                 pengganti
	 * @param usingId  {@code true} membuat tautan meminta pencocokan ke primary key,
	 *                 dengan seluruh konsekuensi yang dijelaskan pada {@code ambil()}
	 * @param download {@code true} meminta berkas disajikan sebagai unduhan
	 * @param clazz    kelas entitas berkas yang dicantumkan pada tautan sekaligus penentu
	 *                 ikon pengganti
	 * @param ketemu   {@code true} mengizinkan alamat berkas statis bila berkasnya ada;
	 *                 {@code false} memaksa melalui servlet
	 * @param relative {@code true} menghasilkan alamat relatif terhadap konteks aplikasi
	 * @param rezise   {@code true} meminta thumbnail 128px; hanya berlaku pada langkah 4
	 *                 dan diabaikan pada jalur tautan luar maupun Google Drive
	 * @return alamat lampiran, atau alamat ikon pengganti
	 * @throws Exception dideklarasikan demi keseragaman keluarga method ini; jalur
	 *                   normalnya menangkap semua kegagalan
	 */
	public static String ambilLinkLampiranLain(FileFotoLain fileFoto, Boolean usingId, Boolean download, Class clazz,
			boolean ketemu, boolean relative, boolean rezise) throws Exception {
		try {
			if (fileFoto == null) {
				return (relative ? Common.ROOT : Common.getRequestHostWithProtocol()) + "/img/" + iconNggakAda(clazz);
			}

			String namaAsli = fileFoto.getNama();
			if (namaAsli == null || namaAsli.trim().length() == 0) {
				namaAsli = "lampiran";
			}
			String namaFile = StringUtils.replace(
					StringUtils.replace(StringUtils.replace(namaAsli, " ", "_"), "%", "_"), "#", "_");
//			boolean isImage = namaFile.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif)$");
//			boolean resi = rezise && isImage;

			String link = ambilLinkLampiranLainLink(fileFoto.ambilRef(), fileFoto.getJenis(), usingId, download, clazz,
					relative, rezise);

			if ("Berupa link file".equalsIgnoreCase(namaAsli)) {
				link = fileFoto.getLink();
			} else if (fileFoto.getGdrive() != null && !fileFoto.getGdrive().isEmpty()) {
				link = "https://drive.google.com/file/d/" + fileFoto.getGdrive() + "/preview";
			} else if (ketemu) {
				// Tata letak berkas kini DIPISAH per entitas: <media>/<NamaKelas>/<id>/<nama>.
				// Sebelumnya hanya <media>/<id>/ sehingga entitas berbeda dgn PK sama berbagi
				// folder dan berkasnya bisa tertukar (lihat FileFoto.segmenFolderBerkas()).
				String segmen = fileFoto.segmenFolderBerkas();
				String fileDiMedia = CommonMedia.getMediaDirectory().getAbsolutePath() + "/" + segmen + "/"
						+ namaFile;
				Path filePath = Paths.get(fileDiMedia);

				if (Files.exists(filePath) && Files.size(filePath) > 0) {
					if (rezise) {
						File fileKecil = new File(CommonMedia.getMediaDirectory(),
								segmen + "/thumbnail_" + namaFile);
						if (!fileKecil.exists()) {
							BufferedImage originalImage = ais.common.CommonFileMediaHelper.bacaGambarAman(filePath.toFile());
							if (originalImage != null) {
								BufferedImage thumbnail = Scalr.resize(originalImage, 128);
								String ext = Common.getFileExtension(filePath.toFile());
								ImageIO.write(thumbnail, ext, fileKecil);
							}
						}
						link = (relative ? Common.ROOT : Common.getRequestHostWithProtocol()) + "/f"
								+ CommonMedia.prefix + "/" + segmen + "/" + fileKecil.getName();
					} else {
						link = (relative ? Common.ROOT : Common.getRequestHostWithProtocol()) + "/f"
								+ CommonMedia.prefix + "/" + segmen + "/" + namaFile;
					}
				}
			}
			return link;
		} catch (Exception e) {
//			e.printStackTrace();
			return (relative ? Common.ROOT : Common.getRequestHostWithProtocol()) + "/img/" + iconNggakAda(clazz);
		}
	}

	/**
	 * Nama berkas ikon pengganti yang dipakai ketika lampiran tidak ditemukan, varian yang
	 * menerima objek kelas.
	 *
	 * <p>Hanya meneruskan ke {@link #iconNggakAda(String)} dengan nama kelas lengkap.
	 * Akan melempar {@code NullPointerException} bila {@code clazz} bernilai {@code null};
	 * pemanggil di dalam kelas ini selalu menyediakan nilai, sedangkan pemanggil pada
	 * {@code AmbilLampiran} memakai variabel {@code clazz} yang sudah berisi
	 * {@code LampiranLain.class} sebagai nilai awal sehingga tidak pernah kosong.</p>
	 *
	 * @param clazz kelas entitas berkas yang lampirannya tidak ditemukan
	 * @return nama berkas ikon di dalam direktori {@code /img/}
	 */
	public static String iconNggakAda(Class clazz) {
		return iconNggakAda(clazz.getName());
	}

	/**
	 * Nama berkas ikon pengganti yang dipakai ketika lampiran tidak ditemukan, dipilih
	 * berdasarkan nama kelas entitasnya.
	 *
	 * <p>Membedakan dua kemungkinan saja: entitas yang lampirannya berupa <b>foto
	 * orang</b> mendapat {@code "user_default.png"}, selebihnya mendapat
	 * {@code "administrator-icon_default.png"}. Golongan pertama didaftar satu per satu:
	 * {@code FotoAdmin}, {@code FotoMahasiswa}, {@code FotoSiswa}, {@code FotoDosen},
	 * {@code FotoGuru}, {@code FotoBiodataCalonMahasiswa}, {@code FotoCalonSiswa},
	 * {@code FotoPegawai}, ditambah {@code PenyediaAsset} yang bukan entitas berkas
	 * melainkan entitas penyedia aset &mdash; satu-satunya nama di daftar ini yang berasal
	 * dari luar paket berkas.</p>
	 *
	 * <p><b>Daftar ini adalah duplikasi yang mudah tertinggal.</b> Ia tidak diturunkan
	 * dari {@code RELASI_MAP} maupun dari hierarki kelas, melainkan disusun manual dengan
	 * perbandingan nama kelas lengkap. Entitas foto orang yang ditambahkan kemudian tanpa
	 * disisipkan ke sini akan diam-diam menampilkan ikon administrator, dan tidak ada
	 * pengujian maupun peringatan kompilasi yang menangkapnya. Perbandingan memakai nama
	 * lengkap sebagai teks, sehingga kelas dengan nama sederhana yang sama di paket lain
	 * tidak ikut terkena &mdash; sifat yang di sini justru menguntungkan.</p>
	 *
	 * <p>Nilai kembaliannya hanyalah nama berkas; pemanggil masih harus menambahkan
	 * awalan {@code /img/} sendiri, sebagaimana dilakukan
	 * {@code ambilLinkLampiranLain(FileFotoLain, ...)} dan beberapa cabang penyelamat di
	 * {@code AmbilLampiran}.</p>
	 *
	 * @param clazzName nama kelas lengkap entitas berkas
	 * @return {@code "user_default.png"} untuk entitas foto orang, selain itu
	 *         {@code "administrator-icon_default.png"}
	 */
	public static String iconNggakAda(String clazzName) {
		if (clazzName.equals(FotoAdmin.class.getName()) || clazzName.equals(FotoMahasiswa.class.getName())
				|| clazzName.equals(FotoSiswa.class.getName()) || clazzName.equals(FotoDosen.class.getName())
				|| clazzName.equals(FotoGuru.class.getName())
				|| clazzName.equals(FotoBiodataCalonMahasiswa.class.getName())
				|| clazzName.equals(FotoCalonSiswa.class.getName()) || clazzName.equals(PenyediaAsset.class.getName())
				|| clazzName.equals(FotoPegawai.class.getName())) {
			return "user_default.png";
		}
		return "administrator-icon_default.png";
	}

	/**
	 * Menulis isi berkas cache metadata lampiran &mdash; pasangan penulis
	 * {@link #ambilLokasi(String, Serializable, String, Class)}.
	 *
	 * <p><b>Tiga bentuk isi yang ditulis pemanggil</b>, masing-masing dengan arti yang
	 * dipahami {@code ambilLokasi()}: teks JSON metadata baris (hasil pencarian berhasil),
	 * {@code "0"} (hasil negatif yang sengaja di-cache agar pencarian yang sama tidak
	 * mengulang query), dan teks kosong (pembatalan cache lewat {@code resetLokasi()}).</p>
	 *
	 * <p><b>Nama berkasnya</b> dibentuk dari {@code prefix + "_lampiran_" + encode(jenis) +
	 * "_" + ref} di dalam lokasi yang ditentukan {@code Common.getFileLocation(clazz, ref,
	 * ...)}. Perhatikan bahwa {@code prefix} sudah memuat pembeda {@code usingId}, sehingga
	 * satu baris lampiran dapat memiliki dua berkas cache terpisah &mdash; lihat catatan
	 * pada {@code resetLokasi()} tentang akibatnya.</p>
	 *
	 * <p><b>Penjagaan {@code ref} bernilai {@code null}.</b> Kode aslinya memanggil
	 * {@code ref.toString()} tanpa syarat. Nilai {@code null} nyata terjadi pada
	 * {@code Tbmuser} baru yang belum tertaut ke entitas Pegawai/Guru/Dosen &mdash; dan
	 * itu terjadi <i>setiap kali proses login berjalan</i>. Akibatnya bukan sekadar satu
	 * {@code NullPointerException}: karena penulisan cache gagal, pencarian yang sama akan
	 * kembali menembus basis data pada setiap pemanggilan berikutnya, lalu kembali gagal
	 * menulis cache, berulang tanpa henti. Placeholder {@code "0"} pada bagian nama berkas
	 * memutus lingkaran itu. Perhatikan bahwa penjagaan ini hanya dipasang pada rangkaian
	 * nama; {@code Common.getFileLocation} tetap menerima {@code ref} apa adanya.</p>
	 *
	 * <p><b>Kegagalan sengaja senyap.</b> Seluruh badan method dibungkus {@code try-catch}
	 * yang hanya mencatat ke {@code ErrorAuditUtil}. Ini disengaja: cache adalah
	 * pengoptimalan, dan kegagalan menulisnya tidak boleh menggagalkan operasi yang sedang
	 * berjalan. Konsekuensinya, pemanggil <b>tidak pernah tahu</b> apakah cache benar-benar
	 * tertulis &mdash; termasuk saat method ini dipakai untuk <i>membatalkan</i> cache.
	 * Pembatalan cache yang gagal tidak menghasilkan tanda apa pun, dan cache lama akan
	 * terus dipakai {@code ambil()} sampai ada yang berhasil menimpanya.</p>
	 *
	 * @param data   isi yang ditulis: JSON metadata, {@code "0"}, atau teks kosong
	 * @param prefix awalan kunci cache, sudah memuat pembeda varian {@code usingId}
	 * @param ref    acuan yang membentuk kunci cache; boleh {@code null}
	 * @param jenis  penanda jenis lampiran yang membentuk kunci cache
	 * @param clazz  kelas entitas berkas yang menentukan lokasi cache
	 */
	private static void tulisLokasi(String data, String prefix, Serializable ref, String jenis, Class clazz) {
		try {
			// FIX NPE rutin: ref bisa null (mis. Tbmuser baru tanpa entitas
			// terkait spt Pegawai/Guru/Dosen -- terjadi tiap proses login).
			// ref.toString() sebelumnya melempar NPE sehingga cache lokasi
			// GAGAL ditulis & query DB + percobaan tulis ini terulang tiap
			// panggilan berikutnya. Pakai placeholder aman utk ref null.
			File file = Common.getFileLocation(clazz, ref, prefix + "_lampiran_" + URLEncoder.encode(jenis, "UTF-8")
					+ "_" + (ref == null ? "0" : ref.toString()));
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/file/FileFotoLain.java:325");
		}
	}

	// --- Ambil Methods (Simplified) ---

	/**
	 * Mencari satu lampiran <b>milik baris pemilik tertentu</b> dengan jenis tertentu,
	 * memakai cache metadata bila tersedia.
	 *
	 * <p>Varian teraman dari keluarga {@code ambil(...)}: {@code usingId} dikunci
	 * {@code false} sehingga pencarian benar-benar memakai kolom acuan pemilik dan
	 * penyaringan {@code jenis} tetap hidup untuk kelas yang memilikinya. Pemanggil yang
	 * hanya ingin "ambilkan lampiran jenis X milik data Y" sebaiknya memakai varian ini
	 * atau saudaranya yang berparameter {@code refresh}, bukan varian ber-{@code usingId}.</p>
	 *
	 * <p>Seluruh mekanisme &mdash; termasuk peran cache, arti golongan {@code refName},
	 * dan apa yang berubah bila {@code usingId} bernilai {@code true} &mdash; dijelaskan
	 * pada {@link #ambil(Boolean, Serializable, String, int, Class, boolean, String)}.</p>
	 *
	 * @param ref   acuan baris pemilik lampiran
	 * @param jenis penanda jenis lampiran yang dicari
	 * @param clazz kelas entitas berkas yang dikueri
	 * @return lampiran yang ditemukan, atau {@code null} bila tidak ada
	 */
	public static FileFotoLain ambil(Serializable ref, String jenis, Class clazz) {
		return ambil(false, ref, jenis, clazz, false);
	}

	/**
	 * Sama dengan {@link #ambil(Serializable, String, Class)}, dengan pilihan memaksa
	 * pembacaan ulang dari basis data.
	 *
	 * <p>Pakai {@code refresh = true} tepat setelah lampiran diubah atau dihapus dalam
	 * alur yang sama, ketika cache metadata belum tentu sudah dibatalkan. Perlu diingat
	 * bahwa {@code refresh} hanya memaksa <i>pembacaan</i> ulang untuk varian kunci cache
	 * yang sedang dipakai; ia tidak menyentuh berkas cache varian yang lain.</p>
	 *
	 * @param ref     acuan baris pemilik lampiran
	 * @param jenis   penanda jenis lampiran yang dicari
	 * @param clazz   kelas entitas berkas yang dikueri
	 * @param refresh {@code true} melewati cache dan langsung bertanya ke basis data
	 * @return lampiran yang ditemukan, atau {@code null} bila tidak ada
	 */
	public static FileFotoLain ambil(Serializable ref, String jenis, Class clazz, boolean refresh) {
		return ambil(false, ref, jenis, clazz, refresh);
	}

	/**
	 * Mencari satu lampiran dengan {@code usingId} ditentukan pemanggil.
	 *
	 * <p><b>Perhatikan makna {@code usingId} sebelum memakai varian ini.</b> Nilai
	 * {@code true} mengubah arti {@code ref} dari "acuan baris pemilik" menjadi "primary
	 * key baris lampiran", sekaligus mematikan penyaringan {@code jenis}. Rinciannya
	 * beserta akibatnya dijelaskan panjang lebar pada
	 * {@link #ambil(Boolean, Serializable, String, int, Class, boolean, String)}.</p>
	 *
	 * @param usingId {@code true} mencocokkan {@code ref} ke primary key lampiran
	 * @param ref     acuan pemilik, atau primary key lampiran bila {@code usingId}
	 * @param jenis   penanda jenis lampiran; diabaikan bila {@code usingId}
	 * @param clazz   kelas entitas berkas yang dikueri
	 * @return lampiran yang ditemukan, atau {@code null} bila tidak ada
	 */
	public static FileFotoLain ambil(Boolean usingId, Serializable ref, String jenis, Class clazz) {
		return ambil(usingId, ref, jenis, 0, clazz, false);
	}

	/**
	 * Mencari satu lampiran dengan {@code usingId} dan {@code refresh} ditentukan
	 * pemanggil &mdash; varian yang paling banyak dipakai dari luar kelas ini.
	 *
	 * <p>Dipakai antara lain oleh {@code createDownloadUpload(...)} saat memuat keadaan
	 * awal tombol lampiran, dan oleh servlet {@code AmbilLampiran} yang memanggilnya
	 * sampai empat kali berturut-turut dengan kombinasi {@code usingId}/{@code refresh}
	 * yang berbeda sampai salah satunya menemukan sesuatu. Pola berjenjang di servlet itu
	 * berarti permintaan yang gagal pada jalur ber-{@code usingId} tetap dicoba lagi pada
	 * jalur berbasis pemilik, dan sebaliknya.</p>
	 *
	 * @param usingId {@code true} mencocokkan {@code ref} ke primary key lampiran
	 * @param ref     acuan pemilik, atau primary key lampiran bila {@code usingId}
	 * @param jenis   penanda jenis lampiran; diabaikan bila {@code usingId}
	 * @param clazz   kelas entitas berkas yang dikueri
	 * @param refresh {@code true} melewati cache dan langsung bertanya ke basis data
	 * @return lampiran yang ditemukan, atau {@code null} bila tidak ada
	 */
	public static FileFotoLain ambil(Boolean usingId, Serializable ref, String jenis, Class clazz, boolean refresh) {
		return ambil(usingId, ref, jenis, 0, clazz, refresh);
	}

	/**
	 * Menghapus lampiran ini lewat lifecycle {@link FileFoto#delete()}, setelah lebih
	 * dahulu membatalkan berkas cache metadatanya.
	 *
	 * <p>Urutannya penting: cache dibatalkan <i>sebelum</i> penghapusan sebenarnya
	 * dijalankan, sehingga pembacaan berikutnya tidak sempat mengambil metadata basi.</p>
	 *
	 * <p><b>Yang perlu diketahui pemanggil: hanya satu dari dua cache yang dibersihkan.</b>
	 * Baris 1311 mengunci {@code usingId = false} sebagai nilai tetap, sehingga kunci
	 * cache yang dibuang hanya varian berbasis pemilik ({@code "data_baru_"}). Berkas
	 * cache varian primary key ({@code "data_baru__id"}) untuk baris yang sama tidak
	 * disentuh sama sekali. Selama berkas itu masih berisi metadata lama, pemanggilan
	 * {@code ambil()} dengan {@code usingId=true} atas baris ini akan mengambil jalur
	 * cache dan membangun kembali objek lampiran dari metadata tersebut tanpa pernah
	 * menyentuh basis data &mdash; lengkap dengan jalan menuju berkas fisiknya. Efeknya
	 * bertumpuk dengan sifat penghapusan yang memang lunak: lihat {@code SOFT_DELETE_ID}
	 * dan {@code hapusAtauUpdate()}, yang pada jalur non-{@code usingId} hanya menimpa
	 * kolom acuan pemilik dan meninggalkan barisnya utuh.</p>
	 *
	 * <p>Perlu dicatat pula bahwa method ini sama sekali tidak menyentuh <b>berkas
	 * fisik</b> di direktori media maupun salinan yang pernah dibuat
	 * {@code createLinkUri()} di akar direktori media. Penghapusan pada tingkat model
	 * hanya berurusan dengan baris dan cache metadata.</p>
	 */
	public void delete() {
		Boolean usingId = false;
		String keyFilePrefix = "data_baru_" + (usingId ? "_id" : "");
		tulisLokasi("", keyFilePrefix, ambilRef(), getJenis(), ambilClazz());
		super.delete();
	}

	/**
	 * Mencari satu lampiran tanpa syarat SQL tambahan, dengan penghitung percobaan ulang
	 * yang ditentukan pemanggil.
	 *
	 * <p>Menetapkan {@code kondisiTambahan} menjadi teks kosong sehingga jalur
	 * {@code Restrictions.sqlRestriction(...)} pada method inti dilewati sepenuhnya.
	 * Inilah bentuk yang dipakai seluruh pemanggil di dalam paket ini; hanya
	 * {@code ais.action.servlet.Data} yang memakai bentuk tujuh parameter dengan syarat
	 * tambahan terisi.</p>
	 *
	 * <p>Parameter {@code jumlahCoba} adalah penjaga rekursi, bukan pilihan perilaku:
	 * pemanggil dari luar seharusnya selalu mengirim {@code 0}. Perannya dijelaskan pada
	 * {@link #ambil(Boolean, Serializable, String, int, Class, boolean, String)}.</p>
	 *
	 * @param usingId    {@code true} mencocokkan {@code ref} ke primary key lampiran
	 * @param ref        acuan pemilik, atau primary key lampiran bila {@code usingId}
	 * @param jenis      penanda jenis lampiran; diabaikan bila {@code usingId}
	 * @param jumlahCoba penghitung percobaan ulang; kirim {@code 0} dari luar
	 * @param clazz      kelas entitas berkas yang dikueri
	 * @param refresh    {@code true} melewati cache dan langsung bertanya ke basis data
	 * @return lampiran yang ditemukan, atau {@code null} bila tidak ada
	 */
	public static FileFotoLain ambil(Boolean usingId, Serializable ref, String jenis, int jumlahCoba, Class clazz,
			boolean refresh) {
		String kondisiTambahan = "";
		return ambil(usingId, ref, jenis, jumlahCoba, clazz, refresh, kondisiTambahan);
	}

	/**
	 * <b>Method inti pencarian lampiran untuk seluruh keluarga entitas berkas.</b> Semua
	 * varian {@code ambil(...)} lain, seluruh pembungkus di {@code LampiranLain}, jalur
	 * pemuatan {@code createDownloadUpload(...)}, servlet {@code AmbilLampiran} yang
	 * melayani {@code /al}, dan servlet {@code ais.action.servlet.Data} pada akhirnya
	 * bermuara ke sini. Karena itu setiap sifat yang dijelaskan di bawah berlaku bagi
	 * <i>semua</i> entitas berkas paket ini, bukan bagi satu entitas saja.
	 *
	 * <h2>1. Alur besar: cache dahulu, basis data belakangan</h2>
	 * <p>Kunci cache dihitung di baris 1490 sebagai
	 * {@code "data_baru_" + (usingId ? "_id" : "")}, lalu {@code ambilLokasi()} dibaca.
	 * Ada tiga cabang:</p>
	 * <ul>
	 *   <li>Cache kosong/penanda bawaan, atau {@code refresh = true} &rarr; basis data
	 *       dikueri (bagian 2 di bawah), hasilnya ditulis balik ke cache.</li>
	 *   <li>Cache berisi {@code "0"} &rarr; {@code null} dikembalikan tanpa menyentuh
	 *       basis data. Ini <b>hasil negatif yang di-cache</b>: kalau lampiran kemudian
	 *       benar-benar diunggah tetapi cache ini tidak dibatalkan, pembacaan akan terus
	 *       menjawab "tidak ada".</li>
	 *   <li>Cache berisi JSON &rarr; objek dibangun ulang dengan
	 *       {@code Common.convertToObject(...)} tanpa query sama sekali. Bila JSON itu
	 *       menyebut berkas yang sudah tidak ada di disk, satu percobaan ulang dilakukan
	 *       dengan {@code jumlahCoba = 1}; penghitung inilah yang mencegah rekursi tak
	 *       berujung ketika cache terus-menerus rusak.</li>
	 * </ul>
	 *
	 * <h2>2. Bentuk query dan tiga saklar yang mengubahnya</h2>
	 * <p>Query disusun sebagai {@code Criteria} pada baris 1517-1523 dengan tiga
	 * pembatas dan satu pengurutan:</p>
	 * <pre>
	 * .add(kondisiTambahan kosong ? sqlRestriction("true") : sqlRestriction(kondisiTambahan))
	 * .addOrder(Order.desc("id"))
	 * .add(usingId || !adaJenis  ? sqlRestriction("true") : eq("jenis", jenis))
	 * .add(usingId || "id".equals(refName) ? idEq(ref)    : eq(refName, ref))
	 * .setMaxResults(1)
	 * </pre>
	 * <p>Perhatikan pola {@code sqlRestriction("true")}: pembatas tidak dihilangkan
	 * melainkan diganti dengan syarat yang selalu benar. Secara hasil sama saja, tetapi
	 * secara pembacaan kode ini membuat "pembatas dimatikan" terlihat seperti "pembatas
	 * dipasang", sehingga mudah terlewat saat menelaah.</p>
	 * <p>Variabel {@code adaJenis} dihitung pada baris 1498-1499:
	 * penyaringan {@code jenis} <b>dimatikan</b> bila {@code refName} bernilai
	 * {@code "id"} atau {@code "tbmuser"}, bila nama kelas berawalan {@code "Foto"}, atau
	 * bila nama kelas berakhiran {@code "FileContent"}. Jadi bahkan tanpa {@code usingId},
	 * seluruh entitas {@code Foto*} sudah tidak menyaring {@code jenis} sama sekali.</p>
	 *
	 * <h2>3. Apa persisnya yang terjadi bila {@code usingId = true}</h2>
	 * <p>Ini bagian yang paling penting untuk dipahami. Satu nilai {@code boolean}
	 * mengubah <b>dua</b> pembatas sekaligus:</p>
	 * <ol>
	 *   <li><b>Penyaringan {@code jenis} dimatikan.</b> Pada baris 1521-1522,
	 *       {@code usingId} muncul sebagai suku pertama disjungsi, sehingga argumen
	 *       {@code jenis} yang dikirim pemanggil tidak pernah dipakai. Lampiran ijazah,
	 *       lampiran KTP, foto profil, dan berkas apa pun yang tersimpan pada tabel yang
	 *       sama menjadi tidak terbedakan.</li>
	 *   <li><b>{@code ref} dicocokkan ke primary key.</b> Pada baris 1523,
	 *       {@code Restrictions.idEq(ref)} menggantikan {@code Restrictions.eq(refName,
	 *       ref)}. Nilai {@code ref} tidak lagi bermakna "milik siapa berkas ini",
	 *       melainkan "baris nomor berapa".</li>
	 * </ol>
	 * <p>Gabungan keduanya berarti: dengan {@code usingId = true}, pencarian tidak lagi
	 * memuat satu pun pembatas yang berhubungan dengan <i>kepemilikan</i>. Yang tersisa
	 * hanyalah nomor baris. Kolom primary key seluruh entitas ini bertipe
	 * {@code IDENTITY} sehingga nilainya berurutan dan mudah ditebak; menelusuri
	 * {@code 1, 2, 3, ...} akan melintasi seluruh isi tabel.</p>
	 * <p><b>Jalur masuk dari luar.</b> Nilai {@code usingId} tidak ditentukan oleh layar
	 * yang sedang dibuka. Servlet {@code AmbilLampiran} membacanya dengan pola
	 * "ambil dari token terenkripsi bila ada, kalau tidak ambil dari parameter permintaan
	 * biasa" (baris 373-375 pada berkas servlet), dan pola yang sama dipakai untuk
	 * {@code ref}, {@code jenis}, dan {@code clazz} (baris 308-320). Artinya permintaan
	 * {@code GET /al?usingId=true&ref=<N>} tanpa token apa pun sudah cukup untuk memanggil
	 * method ini dengan {@code usingId=true} dan {@code ref=N}. Bahkan bila
	 * {@code usingId} tidak dikirim, servlet tetap mencoba jalur ber-{@code usingId}
	 * lebih dahulu lalu jatuh ke jalur berbasis pemilik &mdash; empat pemanggilan
	 * berturut-turut pada baris 384-398 servlet tersebut.</p>
	 * <p><b>Yang tidak diperiksa di sini.</b> Method ini tidak memanggil
	 * {@code Common.getCurrentUser()}, tidak menerima parameter pengguna, dan tidak
	 * membandingkan apa pun dengan sesi yang sedang berjalan. Tidak ada penyaring satuan
	 * kerja, tidak ada penyaring kepemilikan, dan kolom audit {@code olehId}/{@code oleh}
	 * tidak ikut dilibatkan. Otorisasi &mdash; bila ada &mdash; sepenuhnya menjadi
	 * tanggung jawab pemanggil, dan pemanggil berupa servlet tidak melakukannya.</p>
	 * <p><b>Bertumpuk dengan penghapusan lunak.</b> Karena {@code hapusAtauUpdate()} pada
	 * jalur non-{@code usingId} hanya menimpa kolom acuan pemilik dengan
	 * {@code SOFT_DELETE_ID} dan membiarkan barisnya utuh, baris yang sudah "dihapus"
	 * pengguna tetap memiliki primary key yang sama dan tetap ditemukan lewat
	 * {@code idEq(ref)}. Penghapusan menyembunyikan berkas dari pencarian berbasis
	 * pemilik, bukan dari pencarian berbasis id.</p>
	 * <p><b>Bertumpuk dengan cache.</b> Kunci cache varian {@code "data_baru__id"}
	 * dibersihkan hanya bila ada yang memanggil {@code resetLokasi()} dengan
	 * {@code usingId = true}; {@code delete()} tidak melakukannya (lihat catatan di
	 * sana). Jadi jalur ini pun dapat menjawab dari cache setelah barisnya tidak lagi
	 * seharusnya terlihat.</p>
	 *
	 * <h2>4. {@code kondisiTambahan}: fragmen SQL mentah</h2>
	 * <p>Bila terisi, nilainya masuk ke {@code Restrictions.sqlRestriction(kondisiTambahan)}
	 * pada baris 1518-1519 &mdash; artinya diteruskan ke basis data <b>sebagai potongan
	 * SQL apa adanya</b>, tanpa parameterisasi dan tanpa pemeriksaan bentuk. Di dalam
	 * paket ini nilainya selalu kosong. Pemanggil dari luar wajib memperlakukan parameter
	 * ini sebagai bagian dari kueri, bukan sebagai data: nilai yang berasal dari masukan
	 * luar tidak boleh sampai ke sini.</p>
	 *
	 * <h2>5. Penanganan khusus {@code FotoAdmin}</h2>
	 * <p>Dua cabang pada baris 1501-1513 menangani kekhasan {@code FotoAdmin} yang kolom
	 * acuannya bertipe {@code String} berisi userid. Bila {@code ref} memang
	 * {@code String}, {@code refName} dipaksa {@code "tbmuser"} dan {@code usingId}
	 * dipaksa {@code false}. Bila {@code ref} bukan {@code String} &mdash; keadaan yang
	 * terjadi pada pengguna baru yang memakai acuan sementara berupa {@code Long} negatif
	 * &mdash; nilainya diubah menjadi {@code String} supaya query tidak melempar
	 * {@code ClassCastException}; hasilnya tidak cocok dengan baris mana pun dan
	 * mengembalikan {@code null}, yang memang benar karena fotonya belum ada.</p>
	 *
	 * <h2>6. Transaksi, kesalahan, dan hasil</h2>
	 * <p>Session dibuka sendiri dari {@code StreamingHibernateUtil} dan selalu ditutup di
	 * blok {@code finally}; transaksi di-{@code rollback} secara diam bila belum sempat
	 * di-{@code commit}. Setiap kegagalan menuliskan {@code "0"} ke cache lalu
	 * mengembalikan {@code null} &mdash; perhatikan bahwa ini berarti <b>kegagalan
	 * sementara pun ikut di-cache sebagai "tidak ada"</b>, sehingga gangguan koneksi
	 * sesaat dapat membuat lampiran terlihat hilang sampai ada yang memaksa
	 * {@code refresh} atau membatalkan cache.</p>
	 * <p>Pengurutan {@code Order.desc("id")} dengan {@code setMaxResults(1)} berarti bila
	 * ada lebih dari satu baris yang cocok, yang dikembalikan adalah yang <b>terbaru</b>.
	 * Duplikat seperti itu memang mungkin terjadi &mdash; lihat catatan pada
	 * {@code hapusAtauUpdate()} tentang golongan {@code refName} bernilai {@code "id"}
	 * yang penghapusannya tidak melakukan apa pun sehingga baris lama menumpuk.</p>
	 *
	 * @param usingId         {@code true} mematikan penyaringan {@code jenis} sekaligus
	 *                        mencocokkan {@code ref} ke primary key lampiran &mdash; lihat
	 *                        bagian 3
	 * @param ref             acuan baris pemilik, atau primary key lampiran bila
	 *                        {@code usingId}
	 * @param jenis           penanda jenis lampiran; <b>diabaikan</b> bila {@code usingId}
	 *                        atau bila kelasnya termasuk golongan tanpa {@code jenis}
	 * @param jumlahCoba      penghitung percobaan ulang untuk cache yang menunjuk berkas
	 *                        hilang atau JSON rusak; kirim {@code 0} dari luar
	 * @param clazz           kelas entitas berkas yang dikueri; menentukan tabel, kolom
	 *                        acuan, dan lokasi cache
	 * @param refresh         {@code true} melewati cache dan langsung bertanya ke basis
	 *                        data
	 * @param kondisiTambahan fragmen SQL mentah yang ditambahkan ke query; teks kosong
	 *                        berarti tanpa syarat tambahan &mdash; lihat bagian 4
	 * @return lampiran yang ditemukan, atau {@code null} bila tidak ada maupun bila
	 *         terjadi kegagalan
	 */
	public static FileFotoLain ambil(Boolean usingId, Serializable ref, String jenis, int jumlahCoba, Class clazz,
			boolean refresh, String kondisiTambahan) {
		String keyFilePrefix = "data_baru_" + (usingId ? "_id" : "");
		String udah = ambilLokasi(keyFilePrefix, ref, jenis, clazz);

		if (udah == null || udah.trim().isEmpty() || VOMahasiswa.dataJSON.equalsIgnoreCase(udah) || refresh) {
			Session session = null;
			Transaction transaction = null;
			try {
				String refName = getRefField(clazz);
				boolean adaJenis = !refName.equals("id") && !refName.equals("tbmuser")
						&& !clazz.getSimpleName().startsWith("Foto") && !clazz.getSimpleName().endsWith("FileContent");

				// Special handling for legacy hardcoded string ref in FotoAdmin
				if (clazz.equals(FotoAdmin.class) && ref instanceof String) {
					refName = "tbmuser";
					usingId = false;
				}
				// FotoAdmin: kolom "tbmuser" bertipe String (userid). Untuk user BARU, ref berupa
				// Long placeholder (-randLong dari createDownloadUpload) sehingga jalur
				// Restrictions.eq("tbmuser", ref) melempar ClassCastException Long->String. Ubah ref
				// ke String agar query AMAN (tidak ada yang cocok -> null, memang belum ada foto).
				if (clazz.equals(FotoAdmin.class) && !usingId && "tbmuser".equals(refName)
						&& ref != null && !(ref instanceof String)) {
					ref = String.valueOf(ref);
				}

				session = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
				transaction = session.beginTransaction();
				FileFotoLain result = (FileFotoLain) session.createCriteria(clazz)
						.add(kondisiTambahan.trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.sqlRestriction(kondisiTambahan))
						.addOrder(Order.desc("id"))
						.add(usingId || !adaJenis ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("jenis", jenis))
						.add(usingId || "id".equals(refName) ? Restrictions.idEq(ref) : Restrictions.eq(refName, ref))
						.setMaxResults(1).uniqueResult();

				if (result != null) {
					JSONObject jsonObject = Common.convertToJsonObject(result);
					jsonObject.put("class", clazz.getName());
					jsonObject.put("id", result.getId());
					tulisLokasi(jsonObject.toString(), keyFilePrefix, ref, jenis, clazz);
					transaction.commit();
					transaction = null;
					return result;
				} else {
					tulisLokasi("0", keyFilePrefix, ref, jenis, clazz);
					transaction.commit();
					transaction = null;
					return null;
				}
			} catch (Exception e) {
				rollbackQuietly(transaction);
				transaction = null;
				tulisLokasi("0", keyFilePrefix, ref, jenis, clazz);
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/file/FileFotoLain.java:408");
				return null;
			} finally {
				rollbackQuietly(transaction);
				if (session != null && session.isOpen())
					session.close();
			}
		} else if ("0".equals(udah.trim())) {
			return null;
		} else {
			try {
				JSONObject jsonObject = new JSONObject(udah);
				String fileLokasi = jsonObject.optString("lokasi_file_absolut");
				if (fileLokasi != null && Files.notExists(Paths.get(fileLokasi)) && jumlahCoba == 0) {
					return ambil(usingId, ref, jenis, 1, clazz, refresh);
				}
				return (FileFotoLain) Common.convertToObject(jsonObject);
			} catch (Exception e) {
				tulisLokasi("", keyFilePrefix, ref, jenis, clazz);
				return jumlahCoba == 0 ? ambil(usingId, ref, jenis, 1, clazz, refresh) : null;
			}
		}
	}

	/**
	 * Membangun kendali lampiran paling sederhana pada sebuah baris {@code Hbox}: tombol
	 * unduh, tombol hapus, dan tombol unggah untuk satu jenis lampiran.
	 *
	 * <p>Bentuk terpendek dari keluarga {@code createDownloadUpload(...)}, dengan pilihan
	 * bawaan berupa tampilan berlabel penuh (bukan ikon saja), pencarian berbasis pemilik
	 * ({@code usingId = false}), tombol unggah ditampilkan, dan tanpa peta penampung
	 * hasil.</p>
	 *
	 * <p><b>Penjagaan acuan kosong.</b> Bila {@code myref} bernilai {@code null} &mdash;
	 * keadaan yang wajar pada formulir data yang belum pernah disimpan &mdash; sebuah
	 * acuan sementara dari {@code Common.refSementara()} dipakai sebagai gantinya. Nilai
	 * itu selalu negatif; alasannya dijelaskan pada {@code createFileFotoLain()}.
	 * Konsekuensi bagi pemanggil: berkas yang diunggah sebelum data induknya disimpan
	 * akan tertaut ke acuan sementara, dan alur penyimpanan data induk harus
	 * memindahkannya ke acuan yang sebenarnya.</p>
	 *
	 * <p>Seluruh perilaku pembangunan komponen dijelaskan pada bentuk lengkapnya,
	 * {@link #createDownloadUpload(Component, Serializable, String, String, Boolean,
	 * EventListener, Map, Boolean, Boolean, Boolean, Boolean, Integer, Boolean, Boolean,
	 * Component, Class, boolean)}.</p>
	 *
	 * @param row           wadah tempat kendali lampiran ditambahkan
	 * @param myref         acuan baris pemilik; {@code null} diganti acuan sementara
	 * @param jenis         penanda jenis lampiran yang dikelola kendali ini
	 * @param keterangan    teks keterangan yang tampil pada label dan dialog
	 * @param harusPdf      {@code true} bila hanya berkas PDF yang boleh diunggah
	 * @param eventListener penerima pemberitahuan setelah unggah berhasil; boleh
	 *                      {@code null}
	 * @param clazz         kelas entitas berkas tempat lampiran disimpan
	 */
	public static void createDownloadUpload(Hbox row, Long myref, String jenis, String keterangan, Boolean harusPdf,
			EventListener eventListener, Class clazz) {
		Long ref = myref == null ? Common.refSementara() : myref;
		createDownloadUpload(row, ref, jenis, keterangan, harusPdf, eventListener, null, false, false, false, true,
				null, clazz);
	}

	/**
	 * Sama dengan {@link #createDownloadUpload(Hbox, Long, String, String, Boolean,
	 * EventListener, Class)}, ditambah batas ukuran unggah khusus.
	 *
	 * <p>Batas ukuran diteruskan apa adanya ke {@code AmbilDataLampiranFileLain} dan
	 * ditegakkan di sana, bukan di kelas ini. Nilai {@code null} berarti memakai batas
	 * bawaan aplikasi.</p>
	 *
	 * @param row               wadah tempat kendali lampiran ditambahkan
	 * @param myref             acuan baris pemilik; {@code null} diganti acuan sementara
	 * @param jenis             penanda jenis lampiran yang dikelola kendali ini
	 * @param keterangan        teks keterangan yang tampil pada label dan dialog
	 * @param harusPdf          {@code true} bila hanya berkas PDF yang boleh diunggah
	 * @param eventListener     penerima pemberitahuan setelah unggah; boleh {@code null}
	 * @param cutomUkuranUpload batas ukuran unggah khusus; {@code null} memakai bawaan
	 * @param clazz             kelas entitas berkas tempat lampiran disimpan
	 */
	public static void createDownloadUpload(Hbox row, Long myref, String jenis, String keterangan, Boolean harusPdf,
			EventListener eventListener, Integer cutomUkuranUpload, Class clazz) {
		Long ref = myref == null ? Common.refSementara() : myref;
		createDownloadUpload(row, ref, jenis, keterangan, harusPdf, eventListener, null, cutomUkuranUpload, clazz);
	}

	/**
	 * Varian dengan peta penampung hasil <b>dan</b> batas ukuran unggah khusus.
	 *
	 * <p>Peta {@code lampiranLains} diisi oleh jalur pemuatan dengan pasangan
	 * {@code jenis -> lampiran} bila lampirannya memang ada. Gunanya agar layar yang
	 * membangun banyak kendali lampiran sekaligus dapat memeriksa kelengkapan berkas
	 * tanpa mengulang pencarian. <b>Perhatikan</b> bahwa peta hanya diisi pada saat
	 * kendali dibangun; ia tidak diperbarui ketika pengguna kemudian mengunggah atau
	 * menghapus berkas, sehingga isinya dapat menjadi basi dalam satu tampilan yang sama.</p>
	 *
	 * @param row               wadah tempat kendali lampiran ditambahkan
	 * @param myref             acuan baris pemilik; {@code null} diganti acuan sementara
	 * @param jenis             penanda jenis lampiran yang dikelola kendali ini
	 * @param keterangan        teks keterangan yang tampil pada label dan dialog
	 * @param harusPdf          {@code true} bila hanya berkas PDF yang boleh diunggah
	 * @param eventListener     penerima pemberitahuan setelah unggah; boleh {@code null}
	 * @param lampiranLains     peta penampung hasil pemuatan; boleh {@code null}
	 * @param cutomUkuranUpload batas ukuran unggah khusus; {@code null} memakai bawaan
	 * @param clazz             kelas entitas berkas tempat lampiran disimpan
	 */
	public static void createDownloadUpload(Hbox row, Long myref, String jenis, String keterangan, Boolean harusPdf,
			EventListener eventListener, Map<String, FileFotoLain> lampiranLains, Integer cutomUkuranUpload,
			Class clazz) {
		Long ref = myref == null ? Common.refSementara() : myref;
		createDownloadUpload(row, ref, jenis, keterangan, harusPdf, eventListener, lampiranLains, false, false, false,
				true, cutomUkuranUpload, clazz);
	}

	/**
	 * Varian dengan peta penampung hasil, memakai batas ukuran unggah bawaan.
	 *
	 * @param row           wadah tempat kendali lampiran ditambahkan
	 * @param myref         acuan baris pemilik; {@code null} diganti acuan sementara
	 * @param jenis         penanda jenis lampiran yang dikelola kendali ini
	 * @param keterangan    teks keterangan yang tampil pada label dan dialog
	 * @param harusPdf      {@code true} bila hanya berkas PDF yang boleh diunggah
	 * @param eventListener penerima pemberitahuan setelah unggah; boleh {@code null}
	 * @param lampiranLains peta penampung hasil pemuatan; boleh {@code null}
	 * @param clazz         kelas entitas berkas tempat lampiran disimpan
	 */
	public static void createDownloadUpload(Hbox row, Long myref, String jenis, String keterangan, Boolean harusPdf,
			EventListener eventListener, Map<String, FileFotoLain> lampiranLains, Class clazz) {
		Long ref = myref == null ? Common.refSementara() : myref;
		createDownloadUpload(row, ref, jenis, keterangan, harusPdf, eventListener, lampiranLains, false, false, false,
				true, null, clazz);
	}

	/**
	 * Varian tempat pemanggil mulai mengendalikan tampilan dan cara pencarian.
	 *
	 * <p>Perhatikan bahwa varian inilah titik pertama {@code usingId} dapat diisi
	 * pemanggil. Nilai {@code true} membuat seluruh jalur &mdash; pemuatan awal,
	 * penyusunan tautan unduh, penghapusan, dan penautan hasil unggah &mdash; memakai
	 * primary key lampiran alih-alih acuan pemilik, dengan segala akibat yang dijelaskan
	 * pada {@code ambil(Boolean, Serializable, String, int, Class, boolean, String)}.
	 * Untuk kendali lampiran pada formulir data biasa, {@code false} hampir selalu yang
	 * dimaksud.</p>
	 *
	 * <p>Berbeda dari varian yang lebih pendek, di sini {@code myref} diteruskan apa
	 * adanya tanpa penggantian acuan sementara &mdash; penggantian itu baru dilakukan
	 * bentuk lengkapnya.</p>
	 *
	 * @param row                wadah tempat kendali lampiran ditambahkan
	 * @param myref              acuan baris pemilik, atau primary key bila {@code usingId}
	 * @param jenis              penanda jenis lampiran yang dikelola kendali ini
	 * @param keterangan         teks keterangan yang tampil pada label dan dialog
	 * @param harusPdf           {@code true} bila hanya berkas PDF yang boleh diunggah
	 * @param eventListener      penerima pemberitahuan setelah unggah; boleh {@code null}
	 * @param lampiranLains      peta penampung hasil pemuatan; boleh {@code null}
	 * @param tidakTampilJurusan penanda penyembunyian combo jurusan &mdash; perhatikan
	 *                           bahwa maknanya terbalik dari namanya; lihat
	 *                           {@code setupJurusanCombo()}
	 * @param hanyaIcon          {@code true} menampilkan tombol sebagai ikon tanpa label
	 * @param usingId            {@code true} memakai primary key lampiran sebagai acuan
	 * @param tampilUpload       {@code true} menampilkan tombol unggah dan tombol hapus
	 * @param clazz              kelas entitas berkas tempat lampiran disimpan
	 */
	public static void createDownloadUpload(Hbox row, Long myref, String jenis, String keterangan, Boolean harusPdf,
			EventListener eventListener, Map<String, FileFotoLain> lampiranLains, Boolean tidakTampilJurusan,
			Boolean hanyaIcon, Boolean usingId, Boolean tampilUpload, Class clazz) {
		createDownloadUpload(row, myref, jenis, keterangan, harusPdf, eventListener, lampiranLains, tidakTampilJurusan,
				hanyaIcon, usingId, tampilUpload, null, clazz);
	}

	/**
	 * Varian pengendali tampilan lengkap dengan batas ukuran unggah khusus.
	 *
	 * <p>Menetapkan dua pilihan tata letak yang tidak dapat diubah dari sini:
	 * {@code vertical = false} (tombol disusun mendatar) dan
	 * {@code janganPreviewDiLayarUtama = true} (pratinjau tidak dibentangkan di layar
	 * utama). Pemanggil yang membutuhkan pratinjau di layar utama harus memakai bentuk
	 * yang lebih panjang.</p>
	 *
	 * @param row                wadah tempat kendali lampiran ditambahkan
	 * @param myref              acuan baris pemilik, atau primary key bila {@code usingId}
	 * @param jenis              penanda jenis lampiran yang dikelola kendali ini
	 * @param keterangan         teks keterangan yang tampil pada label dan dialog
	 * @param harusPdf           {@code true} bila hanya berkas PDF yang boleh diunggah
	 * @param eventListener      penerima pemberitahuan setelah unggah; boleh {@code null}
	 * @param lampiranLains      peta penampung hasil pemuatan; boleh {@code null}
	 * @param tidakTampilJurusan penanda combo jurusan; maknanya terbalik dari namanya
	 * @param hanyaIcon          {@code true} menampilkan tombol sebagai ikon tanpa label
	 * @param usingId            {@code true} memakai primary key lampiran sebagai acuan
	 * @param tampilUpload       {@code true} menampilkan tombol unggah dan tombol hapus
	 * @param cutomUkuranUpload  batas ukuran unggah khusus; {@code null} memakai bawaan
	 * @param clazz              kelas entitas berkas tempat lampiran disimpan
	 */
	public static void createDownloadUpload(Hbox row, Long myref, String jenis, String keterangan, Boolean harusPdf,
			EventListener eventListener, Map<String, FileFotoLain> lampiranLains, Boolean tidakTampilJurusan,
			Boolean hanyaIcon, Boolean usingId, Boolean tampilUpload, Integer cutomUkuranUpload, Class clazz) {
		createDownloadUpload(row, myref, jenis, keterangan, harusPdf, eventListener, lampiranLains, tidakTampilJurusan,
				hanyaIcon, usingId, tampilUpload, cutomUkuranUpload, false, true, clazz);

	}

	/**
	 * Varian dengan kendali tata letak penuh, tanpa wadah pratinjau dari luar.
	 *
	 * <p>Menyerahkan {@code parentPreview} bernilai {@code null} sehingga bentuk
	 * lengkapnya membuat sendiri wadah pratinjau di dalam susunan komponen yang
	 * dibangunnya. Pemanggil yang ingin menempatkan pratinjau di bagian lain halaman
	 * harus memakai bentuk yang menerima {@code parentPreviewAja}.</p>
	 *
	 * @param row                       wadah tempat kendali lampiran ditambahkan
	 * @param myref                     acuan pemilik, atau primary key bila {@code usingId}
	 * @param jenis                     penanda jenis lampiran yang dikelola kendali ini
	 * @param keterangan                teks keterangan pada label dan dialog
	 * @param harusPdf                  {@code true} bila hanya PDF yang boleh diunggah
	 * @param eventListener             penerima pemberitahuan setelah unggah
	 * @param lampiranLains             peta penampung hasil pemuatan; boleh {@code null}
	 * @param tidakTampilJurusan        penanda combo jurusan; makna terbalik dari namanya
	 * @param hanyaIcon                 {@code true} menampilkan ikon tanpa label
	 * @param usingId                   {@code true} memakai primary key sebagai acuan
	 * @param tampilUpload              {@code true} menampilkan tombol unggah dan hapus
	 * @param cutomUkuranUpload         batas ukuran unggah khusus; {@code null} bawaan
	 * @param vertical                  {@code true} menyusun tombol menurun
	 * @param janganPreviewDiLayarUtama {@code true} menahan pratinjau agar tidak
	 *                                  dibentangkan di layar utama
	 * @param clazz                     kelas entitas berkas tempat lampiran disimpan
	 */
	public static void createDownloadUpload(Hbox row, Serializable myref, String jenis, String keterangan,
			Boolean harusPdf, EventListener eventListener, Map<String, FileFotoLain> lampiranLains,
			Boolean tidakTampilJurusan, Boolean hanyaIcon, Boolean usingId, Boolean tampilUpload,
			Integer cutomUkuranUpload, Boolean vertical, Boolean janganPreviewDiLayarUtama, Class clazz) {
		Component parentPreview = null;
		createDownloadUpload(row, myref, jenis, keterangan, harusPdf, eventListener, lampiranLains, tidakTampilJurusan,
				hanyaIcon, usingId, tampilUpload, cutomUkuranUpload, vertical, janganPreviewDiLayarUtama, parentPreview,
				clazz);
	}

	/**
	 * Varian yang menerima wadah pratinjau dari luar, tanpa pemaksaan pembacaan ulang.
	 *
	 * <p>Meneruskan {@code refresh = false} sehingga pemuatan awal boleh memakai cache
	 * metadata. Bentuk ini dipakai saat kendali lampiran dibangun pertama kali; jalur
	 * penyusunan ulang setelah unggah atau hapus memanggil bentuk lengkapnya dengan
	 * {@code refresh = true} supaya keadaan terbaru benar-benar terbaca dari basis data.</p>
	 *
	 * @param rowUtama                  wadah tempat kendali lampiran ditambahkan
	 * @param myrefId                   acuan pemilik, atau primary key bila {@code usingId}
	 * @param jenis                     penanda jenis lampiran yang dikelola kendali ini
	 * @param keterangan                teks keterangan pada label dan dialog
	 * @param harusPdf                  {@code true} bila hanya PDF yang boleh diunggah
	 * @param eventListener             penerima pemberitahuan setelah unggah
	 * @param lampiranLains             peta penampung hasil pemuatan; boleh {@code null}
	 * @param tidakTampilJurusan        penanda combo jurusan; makna terbalik dari namanya
	 * @param hanyaIcon                 {@code true} menampilkan ikon tanpa label
	 * @param usingId                   {@code true} memakai primary key sebagai acuan
	 * @param tampilUpload              {@code true} menampilkan tombol unggah dan hapus
	 * @param cutomUkuranUpload         batas ukuran unggah khusus; {@code null} bawaan
	 * @param vertical                  {@code true} menyusun tombol menurun
	 * @param janganPreviewDiLayarUtama {@code true} menahan pratinjau di layar utama
	 * @param parentPreviewAja          wadah pratinjau milik pemanggil; {@code null}
	 *                                  membuat wadah sendiri
	 * @param clazz                     kelas entitas berkas tempat lampiran disimpan
	 */
	@SuppressWarnings({})
	public static void createDownloadUpload(final Component rowUtama, Serializable myrefId, final String jenis,
			final String keterangan, final Boolean harusPdf, final EventListener eventListener, final Map lampiranLains,
			final Boolean tidakTampilJurusan, final Boolean hanyaIcon, final Boolean usingId,
			final Boolean tampilUpload, final Integer cutomUkuranUpload, final Boolean vertical,
			final Boolean janganPreviewDiLayarUtama, final Component parentPreviewAja, final Class clazz) {
		createDownloadUpload(rowUtama, myrefId, jenis, keterangan, harusPdf, eventListener, lampiranLains,
				tidakTampilJurusan, hanyaIcon, usingId, tampilUpload, cutomUkuranUpload, vertical,
				janganPreviewDiLayarUtama, parentPreviewAja, clazz, false);
	}

	/**
	 * <b>Bentuk lengkap pembangun kendali lampiran</b> &mdash; satu-satunya tempat susunan
	 * komponen ZK untuk mengunduh, mengunggah, meninjau, dan menghapus lampiran benar-benar
	 * dibentuk. Seluruh varian {@code createDownloadUpload(...)} lain bermuara ke sini.
	 *
	 * <h2>Susunan komponen yang dibangun</h2>
	 * <p>Sebuah {@code Vbox} ditambahkan ke {@code rowUtama}, berisi wadah tombol
	 * ({@code Hbox} bila {@code hanyaIcon} atau tidak {@code vertical}, selain itu
	 * {@code Vbox}) dan wadah pratinjau. Wadah tombol juga dipasang sebagai atribut
	 * {@code "tombol"} pada {@code rowUtama} supaya pemanggil dapat menemukannya kembali,
	 * dan jumlah lampiran yang berhasil dimuat dipasang sebagai atribut
	 * {@code "jumlah_upload"} &mdash; nilainya hanya {@code 0} atau {@code 1}, karena
	 * pencarian memakai {@code setMaxResults(1)}. Wadah pratinjau dibuat sendiri kecuali
	 * pemanggil menyediakan {@code parentPreviewAja}.</p>
	 *
	 * <h2>Pemuatan keadaan awal</h2>
	 * <p>Satu pemanggilan {@code FileFotoLain.ambil(usingId, ref, jenis, clazz, refresh)}
	 * menentukan seluruh penampakan awal: tombol unduh dan tombol hapus hanya terlihat
	 * bila lampirannya ada, seluruh baris disembunyikan bila lampiran tidak ada
	 * <i>dan</i> {@code tampilUpload} bernilai {@code false}, dan label tombol unduh
	 * diambil dari nama berkas &mdash; dipangkas pada garis bawah terakhir, lalu
	 * disingkat 20 karakter bila tombol unggah tidak ditampilkan. Nilai {@code usingId}
	 * yang diteruskan ke sini ikut menentukan cara pencarian itu; lihat
	 * {@code ambil(Boolean, Serializable, String, int, Class, boolean, String)}.</p>
	 *
	 * <h2>Penyegaran setelah unggah atau hapus</h2>
	 * <p>{@code resetAfterUploadData} membongkar seluruh isi {@code rowUtama} lalu
	 * membangunnya kembali lewat pemanggilan rekursif ke method ini dengan
	 * {@code refresh = true}, dijadwalkan melalui {@code Common.createDefaultTimer(...)}
	 * agar berjalan setelah putaran event berjalan selesai. Pola bongkar-pasang ini
	 * berarti setiap referensi komponen yang disimpan pemanggil menjadi tidak berlaku
	 * setelah unggah atau hapus; ambil kembali lewat atribut {@code "tombol"}.</p>
	 *
	 * <h2>Pemeriksaan Google Drive dan penjagaan di sekitarnya</h2>
	 * <p>Bila kebijakan mengizinkan dan lampiran belum ada di Google Drive, sebuah tombol
	 * penyalinan ke Drive ditambahkan. Bagian ini dijaga berlapis karena pernah
	 * menjatuhkan seluruh layar: {@code ambilFile()} dapat mengembalikan berkas
	 * pengganti {@code logo.png} ketika berkas aslinya hilang atau tidak cocok dengan
	 * baris ini &mdash; keadaan itu <b>tidak</b> diteruskan ke Google Drive, melainkan
	 * ditandai dengan label merah "Berkas tidak ditemukan" &mdash; sedangkan
	 * {@code Common.simpanKeDrive()} masih dapat melempar {@code RuntimeException} lain
	 * (mis. gangguan jaringan). Sebelum dibungkus, satu baris lampiran bermasalah cukup
	 * untuk menghentikan seluruh penggambaran grid unggah maupun beranda PMB. Sekarang
	 * baris itu dilewati dengan catatan ke {@code ErrorAuditUtil} dan baris lain tetap
	 * tampil.</p>
	 *
	 * <h2>Tombol unggah dan tombol hapus</h2>
	 * <p>Tombol unggah dibangun {@code tampilkanTombolUpload(...)} bila ada pengguna yang
	 * sedang masuk dan {@code ref} terisi; bila tidak, jalur cadangan memakai
	 * {@code AmbilDataLampiranFileLain} secara langsung dengan label yang diubah menjadi
	 * "Ganti" ketika lampirannya sudah ada. Tombol hapus meminta konfirmasi lebih dahulu,
	 * dan bila disetujui memanggil {@code performDelete(usingId, ref, jenis, clazz)}
	 * disusul penyegaran tampilan.</p>
	 *
	 * <p><b>Perhatikan ketidaksesuaian antara peringatan dan kenyataan.</b> Pesan
	 * konfirmasi menyebut tindakan ini "bersifat permanen dan tidak dapat dibatalkan".
	 * Pada jalur non-{@code usingId} yang sesungguhnya terjadi adalah <i>soft delete</i>:
	 * {@code hapusAtauUpdate()} hanya menimpa kolom acuan pemilik dengan
	 * {@code SOFT_DELETE_ID} sehingga baris beserta seluruh isi berkasnya tetap ada di
	 * basis data dan tetap dapat ditemukan lewat pencarian berbasis primary key. Pada
	 * golongan kelas ber-{@code refField} {@code "id"} bahkan tidak ada perubahan apa pun
	 * yang dijalankan. Jangan menyimpulkan dari pesan ini bahwa data benar-benar
	 * dimusnahkan; lihat {@code hapusAtauUpdate()} untuk apa yang sungguh-sungguh
	 * terjadi.</p>
	 *
	 * <h2>Kekhasan lain yang perlu diketahui</h2>
	 * <p>Tombol hapus dibuat lebih awal tetapi baru ditambahkan ke wadah di dalam blok
	 * {@code tampilUpload}. Bila {@code tampilUpload} bernilai {@code false}, tombol itu
	 * tetap dibuat, tetap dipasang ke {@code SetelahUpload}, namun tidak pernah muncul di
	 * layar &mdash; komponen yatim yang tidak berbahaya tetapi mudah membingungkan saat
	 * menelusuri kode. Perhatikan pula bahwa {@code jumlah} selalu bernilai {@code 0}
	 * atau {@code 1}: kendali ini memang hanya melayani satu lampiran per
	 * ({@code jenis}, {@code ref}), bukan daftar berkas.</p>
	 *
	 * @param rowUtama                  wadah tempat seluruh susunan komponen ditambahkan
	 * @param myrefId                   acuan pemilik, atau primary key bila {@code usingId};
	 *                                  {@code null} diganti {@code Common.refSementara()}
	 * @param jenis                     penanda jenis lampiran yang dikelola kendali ini
	 * @param keterangan                teks keterangan pada label, tooltip, dan dialog
	 * @param harusPdf                  {@code true} bila hanya PDF yang boleh diunggah
	 * @param eventListener             penerima pemberitahuan setelah unggah berhasil;
	 *                                  dipanggil tertunda lewat timer
	 * @param lampiranLains             peta penampung {@code jenis -> lampiran}, diisi
	 *                                  hanya saat pemuatan awal; boleh {@code null}
	 * @param tidakTampilJurusan        penanda combo jurusan; maknanya terbalik dari
	 *                                  namanya, lihat {@code setupJurusanCombo()}
	 * @param hanyaIcon                 {@code true} menampilkan tombol sebagai ikon tanpa
	 *                                  label dan memaksa susunan mendatar
	 * @param usingId                   {@code true} membuat seluruh jalur memakai primary
	 *                                  key lampiran alih-alih acuan pemilik
	 * @param tampilUpload              {@code true} menampilkan tombol unggah dan hapus
	 * @param cutomUkuranUpload         batas ukuran unggah khusus; {@code null} bawaan
	 * @param vertical                  {@code true} menyusun tombol menurun
	 * @param janganPreviewDiLayarUtama {@code true} menahan pratinjau di layar utama
	 * @param parentPreviewAja          wadah pratinjau milik pemanggil; {@code null}
	 *                                  membuat wadah sendiri di dalam susunan ini
	 * @param clazz                     kelas entitas berkas tempat lampiran disimpan
	 * @param refresh                   {@code true} memaksa pemuatan awal melewati cache
	 *                                  metadata; dipakai jalur penyegaran setelah unggah
	 */
	public static void createDownloadUpload(final Component rowUtama, Serializable myrefId, final String jenis,
			final String keterangan, final Boolean harusPdf, final EventListener eventListener, final Map lampiranLains,
			final Boolean tidakTampilJurusan, final Boolean hanyaIcon, final Boolean usingId,
			final Boolean tampilUpload, final Integer cutomUkuranUpload, final Boolean vertical,
			final Boolean janganPreviewDiLayarUtama, final Component parentPreviewAja, final Class clazz,
			final boolean refresh) {

		final Serializable ref = myrefId == null ? Common.refSementara() : myrefId;

		// Layout setup
		Vbox vbox = new Vbox();
		vbox.setHflex("1");
		vbox.setVflex("1");
		rowUtama.appendChild(vbox);

		Box containerTombol = (hanyaIcon || !vertical) ? new Hbox() : new Vbox();
		containerTombol.setHflex("1");
		containerTombol.setVflex("1");
		vbox.appendChild(containerTombol);
		rowUtama.setAttribute("tombol", containerTombol);

		final Component vbPreview = parentPreviewAja == null ? new Vbox() : parentPreviewAja;
		if (parentPreviewAja == null)
			vbox.appendChild(vbPreview);

		// Reset Handler
		final EventListener resetAfterUploadData = new EventListener() {
			public void onEvent(Event arg0) throws Exception {
				Common.clear(rowUtama);
				if (parentPreviewAja != null) {
					Common.clear(parentPreviewAja);
					parentPreviewAja.setVisible(true);
				}
				Common.createDefaultTimer(new EventListener() {
					public void onEvent(Event arg0) throws Exception {
						createDownloadUpload(rowUtama, ref, jenis, keterangan, harusPdf, eventListener, lampiranLains,
								tidakTampilJurusan, hanyaIcon, usingId, tampilUpload, cutomUkuranUpload, vertical,
								janganPreviewDiLayarUtama, parentPreviewAja, clazz, true);
						Clients.scrollIntoView(parentPreviewAja != null ? parentPreviewAja : rowUtama);
					}
				});
			}
		};

		// UI Components
		setupJurusanCombo(containerTombol, tidakTampilJurusan);
		final MyToolbarbuttonConfig downloadButton = createButton(hanyaIcon, "Download " + keterangan,
				"/img/svg/attachment-2.svg");
		containerTombol.appendChild(downloadButton);
		final MyToolbarbuttonConfig hapusButton = createButton(hanyaIcon, "Hapus", "/img/svg/trash.svg");

		final SetelahUpload setelahUpload = new SetelahUpload(downloadButton, janganPreviewDiLayarUtama, ref, jenis,
				usingId, vbPreview, hapusButton, clazz);
		final List<MyToolbarbuttonConfig> gdriveButtons = new ArrayList<MyToolbarbuttonConfig>();

		// Logic Load Data
		FileFotoLain fileFotoLain = FileFotoLain.ambil(usingId, ref, jenis, clazz, refresh);
		int jumlah = fileFotoLain != null ? 1 : 0;
		rowUtama.setAttribute("jumlah_upload", jumlah);

		if (jumlah == 0 && !tampilUpload)
			rowUtama.setVisible(false);

		downloadButton.setVisible(jumlah > 0);
		hapusButton.setVisible(jumlah > 0);
		if (parentPreviewAja != null)
			parentPreviewAja.setVisible(jumlah > 0);

		if (jumlah > 0) {
			setupDownloadButtonAction(downloadButton, fileFotoLain, usingId, clazz, keterangan);
			if (lampiranLains != null)
				lampiranLains.put(jenis, fileFotoLain);

			downloadButton.setImage(fileFotoLain.iconDonwload());
			if (!hanyaIcon) {
				String nama = fileFotoLain.getNama();
				if (nama.contains("_"))
					nama = nama.substring(nama.lastIndexOf("_") + 1);
				if (!tampilUpload)
					nama = StringUtils.abbreviate(nama, 20);

				downloadButton.setLabel(nama);
				try {
					setelahUpload.onEvent(new Event("langsung", downloadButton, fileFotoLain));
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/file/FileFotoLain.java:580");
				}

				// GDrive Check
				// PENJAGA: fileFotoLain.ambilFile() bisa saja berkas fisiknya hilang atau
				// tak cocok dgn baris ini (lihat FileFoto.ambilFile: berkas-salah-baris /
				// file-tidak-ditemukan) -> method tsb SUDAH mencatat IllegalStateException /
				// FileNotFoundException ke ErrorAuditUtil lalu mengembalikan placeholder
				// logo.png, tak pernah melempar exception itu ke sini. Namun logika
				// Common.simpanKeDrive() di bawah bisa saja melempar RuntimeException lain
				// (mis. IO ke Google Drive) yg sebelumnya TIDAK ditangkap sama sekali,
				// sehingga 1 baris lampiran bermasalah menghentikan seluruh render grid
				// upload (ItemAction, buku perpustakaan) maupun beranda PMB (PMBAction).
				// Bungkus supaya baris ini di-skip dgn peringatan, baris lain tetap tampil.
				if (AmbilDataLampiranFileLain.bolehDriveAtauLink(clazz, jenis, keterangan)
						&& fileFotoLain.getGdrive() == null) {
					try {
						File f = fileFotoLain.ambilFile();
						if (f != null && f.exists()) {
							if ("logo.png".equals(f.getName())) {
								// Placeholder dari ambilFile() -> berkas asli hilang/tak cocok.
								// Jangan lanjut ke Google Drive dgn placeholder, cukup beri tanda.
								containerTombol.appendChild(new ais.ui.util.MyLabelAgakKecilBoldMerah(
										"Berkas tidak ditemukan"));
							} else {
								MyToolbarbuttonConfig gdrive = Common.simpanKeDrive(fileFotoLain, f, keterangan,
										resetAfterUploadData);
								gdrive.setParent(containerTombol);
								gdriveButtons.add(gdrive);
							}
						}
					} catch (RuntimeException e) {
						ais.common.ErrorAuditUtil.record(e,
								"lampiran-lain-gdrive-check-gagal-diabaikan src/ais/database/model/file/FileFotoLain.java:595");
					}
				}
			}
		}

		// Upload Button Logic
		if (tampilUpload) {
			Tbmuser tbmuser = Common.getCurrentUser();
			if (tbmuser != null && ref != null) {
				containerTombol.appendChild(tampilkanTombolUpload(downloadButton, hapusButton, eventListener,
						setelahUpload, fileFotoLain, jenis, hanyaIcon, cutomUkuranUpload, keterangan, null, harusPdf,
						ref, usingId, resetAfterUploadData, clazz));
			} else {
				// Fallback legacy upload button if needed
				AmbilDataLampiranFileLain adl = new AmbilDataLampiranFileLain(jenis, cutomUkuranUpload, keterangan,
						null, harusPdf, ref, usingId, clazz, false);
				adl.setEventListener(wrapListener(eventListener, resetAfterUploadData));
				Toolbarbutton btn = adl.tampilkanTombolUpload("");
				btn.setStyle("font-size:9px");
				containerTombol.appendChild(btn);
				if (fileFotoLain != null)
					btn.setLabel(btn.getLabel().replace("Upload", "Ganti"));
			}

			containerTombol.appendChild(hapusButton);
			hapusButton.addEventListener("onClick", new EventListener() {
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.showFormatCb(
							"Apakah Bapak/Ibu yakin ingin menghapus \"{V1}\"? Tindakan ini bersifat permanen dan tidak dapat dibatalkan. Silakan pilih OK untuk melanjutkan penghapusan, atau Batal untuk membatalkan.",
							"Konfirmasi", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {
								public void onEvent(Event e) throws Exception {
									if (Integer.parseInt(e.getData().toString()) == MyMessageboxConfig.OK) {
										performDelete(usingId, ref, jenis, clazz);
										resetAfterUploadData.onEvent(e);
									}
								}
							}, keterangan);
				}
			});
		}
	}

	// --- Helper UI Methods ---

	private static EventListener wrapListener(final EventListener original, final EventListener reset) {
		return new EventListener() {
			public void onEvent(Event arg0) throws Exception {
				if (original != null)
					original.onEvent(arg0);
				reset.onEvent(arg0);
			}
		};
	}

	private static void performDelete(Boolean usingId, Serializable ref, String jenis, Class clazz) {
		Session session = StreamingHibernateUtil.getInstance().currentSession();
		try {
			FileFotoLain.resetLokasi(usingId, ref, jenis, clazz);
			FileFotoLain temp = (FileFotoLain) clazz.newInstance();
			hapusAtauUpdate(temp, session, usingId, ref, jenis);
		} catch (Exception ex) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(ex);
		} finally {
			StreamingHibernateUtil.getInstance().closeSession();
		}
	}

	private static MyToolbarbuttonConfig createButton(boolean iconOnly, String label, String iconPath) {
		MyToolbarbuttonConfig btn = iconOnly ? new MyToolbarbuttonConfig("", iconPath)
				: new MyToolbarbuttonConfig(label, iconPath);
		if (!iconOnly)
			btn.setTooltiptext(label);
		btn.setStyle("font-size:9px");
		btn.setAttribute("janganDisabled", true);
		return btn;
	}

	private static void setupJurusanCombo(Component parent, Boolean tidakTampil) {
		if (tidakTampil && "Y".equals(
				Common.getKonfigurasi("upload_file_di_konfigurasi_tiap_jurusan_bisa_beda", Konfigurasi.TIDAK_AKTIF)
						.getNilai())) {
			Combobox jurusan = new Combobox();
			Common.insertComboDanSemua(jurusan, "nama", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			jurusan.setReadonly(true);
			parent.appendChild(jurusan);
		}
	}

	private static void setupDownloadButtonAction(Toolbarbutton btn, final FileFotoLain file, final Boolean usingId,
			final Class clazz, final String ket) {
		btn.addEventListener("onClick", new EventListener() {
			public void onEvent(Event arg0) throws Exception {
				if (file.getNama().endsWith(".jrxml") || file.getNama().endsWith(".xml")) {
					Filedownload.save(file.ambilFile(), file.getKeterangan());
				} else if (file.getGdrive() != null) {
					file.tampilGDrive(null);
				} else {
					String link = file.getLink();
					if (link == null || link.isEmpty())
						// ketemu=false: selalu pakai URL /al?d=... agar melalui servlet AmbilLampiran
						// yang terautentikasi. URL media (/f/ais/...) bisa return 404 dan memicu
						// broken.jsp yang memiliki reload loop sehingga preview tidak pernah selesai.
						link = FileFotoLain.ambilLinkLampiranLain(file, usingId, true, clazz, false);

					if (link != null && !link.isEmpty()) {
						if (file.bisaPreview()) {
							Common.displayWindow(file.merupakanGambar(), link, true, "95%", "95%", true, file);
						} else {
							if (link.contains("AmbilLampiran") || Common.isMobile()) {
								ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
							} else {
								Clients.evalJavaScript(
										"popupCenter({url: '" + link + "', title: 'data', w: 1200, h: 600});");
							}
						}
					} else {
						MyMessageboxConfig.show(
								"Mohon maaf, Bapak/Ibu. Berkas yang diminta tidak ditemukan. Langkah yang dapat dilakukan: (1) pastikan berkas telah diunggah dengan benar; (2) muat ulang halaman lalu coba kembali; (3) apabila masalah masih berlanjut, mohon menghubungi Administrator.",
								"Error", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					}
				}
			}
		});
	}

	// --- Create & Update Logic (Generic) ---

	public static MyToolbarbuttonConfig tampilkanTombolUpload(final Button downloadButton, final Button hapusButton,
			final EventListener eventListener, final SetelahUpload setelahUpload, FileFotoLain fileFotoLain,
			final String jenis, final Boolean hanyaIcon, final Integer cutomUkuranUpload, final String keterangan,
			final Combobox jurusan, final Boolean harusPdf, final Serializable ref, final Boolean usingId,
			final EventListener resetAfterUpload, final Class clazz) {

		EventListener eventListenerUpload = new EventListener() {
			public void onEvent(Event arg0) throws Exception {
				AmbilDataLampiranFileLain dialog = new AmbilDataLampiranFileLain(jenis, cutomUkuranUpload, keterangan,
						jurusan, harusPdf, ref, usingId, clazz, true);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(dialog);
				dialog.onModal();
				dialog.setEventListener(new EventListener() {
					public void onEvent(Event ev) throws Exception {
						FileFotoLain.resetLokasi(usingId, ref, jenis, clazz);
						final FileFotoLain hasilUpload = (FileFotoLain) ev.getData();

						if (hasilUpload != null) {
							Session session = StreamingHibernateUtil.getInstance().currentSession();
							try {
								String key = "data_baru_" + (usingId ? "_id" : "");
								FileFotoLain finalObj = hasilUpload;

								if (!"Baru".equalsIgnoreCase(ev.getName())) {
									Tbmuser user = Common.getCurrentUser();
									finalObj = FileFotoLain.createFileFotoLain(user, session, clazz, usingId, ref,
											jenis, hasilUpload, null, hasilUpload.getNama());
								}

								// Update UI
								if (downloadButton != null) {
									downloadButton.setImage(finalObj.iconDonwload());
									if (!hanyaIcon)
										downloadButton.setLabel(finalObj.getNama());
									if (setelahUpload != null)
										setelahUpload.onEvent(new Event("", downloadButton, finalObj));
								}
								if (hapusButton != null)
									hapusButton.setVisible(true);

								JSONObject json = Common.convertToJsonObject(finalObj);
								json.put("class", clazz.getName());
								json.put("id", finalObj.getId());
								tulisLokasi(json.toString(), key, ref, jenis, clazz);

								if (eventListener != null) {
									final FileFotoLain callbackObj = finalObj;
									Common.createDefaultTimer(new EventListener() {
										public void onEvent(Event e) throws Exception {
											eventListener.onEvent(new Event("", downloadButton, callbackObj));
										}
									});
								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/file/FileFotoLain.java:776");
							} finally {
								StreamingHibernateUtil.getInstance().closeSession();
								if (resetAfterUpload != null)
									resetAfterUpload.onEvent(null);
							}
						}
					}
				});
			}
		};

		String lbl = (fileFotoLain == null ? "Upload " : "Ganti ") + StringUtils.abbreviate(keterangan, 10);
		MyToolbarbuttonConfig btn = new MyToolbarbuttonConfig(hanyaIcon ? "" : lbl, "/img/svg/upload.svg");
		if (!hanyaIcon)
			btn.setTooltiptext(lbl + " " + keterangan);
		btn.setStyle("font-size:9px");
		btn.setAttribute("janganDisabled", true);
		btn.addEventListener("onClick", eventListenerUpload);
		return btn;
	}

	@SuppressWarnings("deprecation")
	public static FileFotoLain createFileFotoLain(Tbmuser tbmuser, Session session, Class clazz, Boolean usingId,
			Serializable ref, String jenis, FileFotoLain source, File file, String nama) throws Exception {

		// Lapis kedua (chokepoint tunggal): apa pun pemanggilnya, hanya subkelas FileFotoLain
		// yang boleh dikonstruksi. Diperiksa SEBELUM newInstance sehingga konstruktor kelas
		// asing tidak pernah berjalan meski nama kelasnya lolos dari pemanggil. Lihat
		// DoUpload.resolveKelasLampiran untuk lapis pertama pada input klien.
		if (clazz == null || !FileFotoLain.class.isAssignableFrom(clazz)) {
			throw new IllegalArgumentException("Kelas lampiran tidak sah: "
					+ (clazz == null ? "null" : clazz.getName()));
		}
		FileFotoLain target = (FileFotoLain) clazz.newInstance();

		if (file != null && file.exists()) {
			try {
				FileInputStream fis = new FileInputStream(file);
				target.setFoto(Hibernate.createBlob(IOUtils.toByteArray(fis)));
				target.setNama(file.getName());
				fis.close();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/file/FileFotoLain.java:811");
			}
		} else {
			target.setNama(nama);
		}
		if (target.getNama() == null || target.getNama().trim().length() == 0) {
			String namaDasar = jenis == null || jenis.trim().length() == 0 ? clazz.getSimpleName() : jenis.trim();
			target.setNama(namaDasar.replace('/', '_').replace('\\', '_') + "_" + System.currentTimeMillis());
		}

		/*
		 * Acuan sementara WAJIB negatif. Sebelumnya dipakai Math.abs(Common.randLong())
		 * yang menghasilkan angka POSITIF <= 99_999_998 - persis rentang id asli. Acuan
		 * seperti itu bisa menunjuk ke baris milik entitas lain yang benar-benar ada,
		 * sehingga hapusAtauUpdate() di bawah menimpa lampiran milik data lain dan berkas
		 * yang baru diunggah muncul di data tersebut.
		 */
		if (ref == null || (ref instanceof Long && (Long) ref == -1L)) {
			ref = Common.refSementara();
		}

		// 1. Hapus atau "Soft Delete" data lama
		FileFotoLain.hapusAtauUpdate(target, session, usingId, ref, jenis);

		// 2. Set Data Utama Menggunakan Reflection (Generic)
		String refFieldName = getRefField(clazz);

		// Set Ref (Foreign Key)
		if (!"id".equals(refFieldName)) {
			if ("tbmuser".equals(refFieldName)) {
				// Khusus FotoAdmin yang fieldnya String tbmuser
				try {
					Method setTbmuser = clazz.getMethod("setTbmuser", String.class);
					setTbmuser.invoke(target, ref.toString());
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/file/FileFotoLain.java:835");
				}
			} else {
				try {
					// Cari setter yang menerima Long atau Serializable
					Method setter = clazz.getMethod(getSetterName(refFieldName), Long.class);
					setter.invoke(target, ref);
				} catch (NoSuchMethodException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/file/FileFotoLain.java:842");
					// Coba tipe parameter lain jika Long gagal (jarang terjadi di struktur ini)
				}
			}
		}

		// Set Jenis (jika ada)
		try {
			Method setJenis = clazz.getMethod("setJenis", String.class);
			setJenis.invoke(target, jenis);
		} catch (NoSuchMethodException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/file/FileFotoLain.java:852");
			/* Ignore if no jenis field */ }

		// Set CopyDari (Copy properti lain dari source)
		try {
			// Asumsi: method setCopyDari menerima parameter bertipe Class itu sendiri
			Method setCopyDari = clazz.getMethod("setCopyDari", clazz);
			setCopyDari.invoke(target, source);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/file/FileFotoLain.java:860");
			/* Ignore */ }
		if (file == null) {
			copySourceBlobToTarget(target, source);
		}

		// 3. Set Audit Info
		target.setTanggal_dirubah(WaktuUtil.getDate());
		target.setOlehId(Common.generateOlehId(tbmuser));
		target.setOleh(getNamaOleh(tbmuser));

		// 4. Save — LampiranLain memakai kolom PostgreSQL Large Object (oid) +
		// hibernate.jdbc.use_streams_for_binary=true, sehingga INSERT-nya menulis Large Object yang
		// WAJIB di dalam transaksi (non-autocommit). Pada SessionFactory streaming, pool c3p0
		// (hibernate.c3p0.*) menjadi ConnectionProvider dan MELEPAS koneksi antar-statement (release
		// agresif), sehingga doWork()/beginTransaction() hanya menyetel auto-commit pada koneksi
		// SEMENTARA yang langsung dilepas — saat save() menjalankan insert IDENTITY, ia mengambil
		// koneksi BARU dari c3p0 yang autoCommit=true → gagal "Large Objects may not be used in
		// auto-commit mode".
		//
		// SOLUSI: ambil SATU koneksi langsung dari ConnectionProvider streaming, set autoCommit=false,
		// lalu buka session DI ATAS koneksi itu via openSession(Connection). Karena koneksi disuplai
		// pengguna, Hibernate TIDAK melepasnya antar-statement, sehingga seluruh insert + penulisan
		// Large Object terjadi pada koneksi yang SAMA (non-autocommit). Tetap memakai StreamingHibernateUtil.
		org.hibernate.engine.SessionFactoryImplementor sfStreaming = (org.hibernate.engine.SessionFactoryImplementor) StreamingHibernateUtil
				.getInstance().getSessionFactory();
		org.hibernate.connection.ConnectionProvider connProvider = sfStreaming.getConnectionProvider();
		java.sql.Connection blobConn = null;
		Session blobSession = null;
		try {
			blobConn = connProvider.getConnection();
			blobConn.setAutoCommit(false);
			blobSession = StreamingHibernateUtil.getInstance().getSessionFactory().openSession(blobConn);
			blobSession.save(target);
			blobSession.flush();
			blobConn.commit();
		} catch (Exception eBlob) {
			if (blobConn != null) {
				try {
					blobConn.rollback();
				} catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/database/model/file/FileFotoLain.java:900");
				}
			}
			throw eBlob;
		} finally {
			if (blobSession != null) {
				ais.database.hibernate.HibernateUtil.closeSessionQuietly(blobSession);
			}
			if (blobConn != null) {
				try {
					blobConn.setAutoCommit(true);
				} catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/database/model/file/FileFotoLain.java:916");
				}
				try {
					connProvider.closeConnection(blobConn);
				} catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/database/model/file/FileFotoLain.java:920");
				}
			}
		}

		return target;
	}

	private static void copySourceBlobToTarget(FileFotoLain target, FileFotoLain source) throws Exception {
		if (target == null || source == null) {
			return;
		}
		byte[] bytes = readSourceBlobBytes(source);
		if (bytes != null && bytes.length > 0) {
			target.setFoto(Hibernate.createBlob(bytes));
		}
	}

	/**
	 * Membaca isi Large Object satu berkas DENGAN AMAN, untuk pemanggil di luar kelas ini.
	 *
	 * <p>Kolom foto memakai PostgreSQL Large Object (oid). Membacanya lewat
	 * {@code getFoto().getBinaryStream()} begitu saja akan gagal dengan
	 * <i>"Large Objects may not be used in auto-commit mode"</i>, karena koneksi
	 * harus berada DI DALAM transaksi. Aturan itu mudah terlewat -- dan memang pernah
	 * terlewat pada aksi unduh lampiran Pengadaan (2026-08-22). Metode ini menyediakan
	 * satu jalan masuk yang benar supaya tidak perlu diulang-ulang di tiap pemanggil.</p>
	 *
	 * @return isi berkas, atau {@code null} bila berkasnya disimpan di Google Drive
	 *         (isinya tidak ada di basis data) atau tidak dapat dibaca.
	 */
	public static byte[] ambilIsiBlob(FileFotoLain berkas) throws Exception {
		return readSourceBlobBytes(berkas);
	}

	private static byte[] readSourceBlobBytes(FileFotoLain source) throws Exception {
		if (source == null) {
			return null;
		}
		try {
			if (source.getGdrive() != null && source.getGdrive().trim().length() > 0) {
				return null;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/file/FileFotoLain.java:946");
		}
		if (source.getId() == null) {
			return readBlobBytes(source.getFoto());
		}
		Session readSession = null;
		Transaction transaction = null;
		try {
			readSession = StreamingHibernateUtil.getInstance().openSession();
			transaction = readSession.beginTransaction();
			Object loaded = readSession.get(source.getClass(), source.getId());
			if (!(loaded instanceof FileFotoLain)) {
				rollbackQuietly(transaction);
				transaction = null;
				return null;
			}
			byte[] bytes = readBlobBytes(((FileFotoLain) loaded).getFoto());
			rollbackQuietly(transaction);
			transaction = null;
			return bytes;
		} finally {
			rollbackQuietly(transaction);
			if (readSession != null) {
				try {
					if (readSession.isOpen()) {
						readSession.close();
					}
				} catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/database/model/file/FileFotoLain.java:973");
				}
			}
		}
	}

	private static byte[] readBlobBytes(Blob blob) throws Exception {
		if (blob == null) {
			return null;
		}
		InputStream in = null;
		try {
			in = blob.getBinaryStream();
			return in == null ? null : IOUtils.toByteArray(in);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/database/model/file/FileFotoLain.java:991");
				}
			}
		}
	}

	private static void rollbackQuietly(Transaction transaction) {
		if (transaction != null) {
			try {
				transaction.rollback();
			} catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/database/model/file/FileFotoLain.java:1001");
			}
		}
	}

	private static String getNamaOleh(Tbmuser user) {
		if (user == null)
			return "external_update";
		if (user.getMahasiswa() != null)
			return user.getMahasiswa().getNama();
		if (user.ambilDosen() != null)
			return user.ambilDosen().getNama();
		if (user.ambilPegawai() != null)
			return user.ambilPegawai().getNama();
		return user.getUserNama();
	}

	public static void hapusAtauUpdate(FileFotoLain a, Session session, boolean usingId, Serializable ref,
			String jenis) {
		try {
			FileFotoLain.resetLokasi(usingId, ref, jenis, a.ambilClazz());

			String refField = getRefField(a.ambilClazz());
			// boolean useHql = true; // Use HQL for safer, database-agnostic queries

			String entityName = a.ambilClazz().getName();
			boolean hasJenis = true;
			try {
				a.ambilClazz().getMethod("getJenis");
			} catch (NoSuchMethodException e) {
				hasJenis = false;
			}

			session.getTransaction().begin();

			if (usingId) {
				// Delete by ID
				String hql = "delete from " + entityName + " where id = :ref" + (hasJenis ? " and jenis = :jenis" : "");
				Query q = session.createQuery(hql);
				q.setParameter("ref", ref);
				if (hasJenis)
					q.setParameter("jenis", jenis);
				q.executeUpdate();
			} else {
				// Soft Delete (Update Ref to -111...)
				if ("id".equals(refField)) {
					// Jika ref field adalah ID, kita tidak bisa update ID-nya sembarangan biasanya,
					// tapi logic asli melakukan delete jika usingId=true.
					// Jika usingId=false tapi refField="id", ini logic yang aneh di code asli,
					// tapi kita ikuti pola update FK nya.
					// Untuk case "id", biasanya tidak ada soft delete update FK, jadi skip atau
					// delete real.
					// Asumsi: Logic asli hanya melakukan update pada field relasi, bukan PK.
				} else {
					String hql = "update " + entityName + " set " + refField + " = :softDel where " + refField
							+ " = :ref";
					if (hasJenis)
						hql += " and jenis = :jenis";

					Query q = session.createQuery(hql);
					if ("tbmuser".equals(refField)) {
						q.setParameter("softDel", String.valueOf(SOFT_DELETE_ID));
						q.setParameter("ref", ref.toString());
					} else {
						q.setParameter("softDel", SOFT_DELETE_ID);
						q.setParameter("ref", ref);
					}

					if (hasJenis)
						q.setParameter("jenis", jenis);
					q.executeUpdate();
				}
			}
			session.getTransaction().commit();

		} catch (Exception e) {
			try {
				session.getTransaction().rollback();
			} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/database/model/file/FileFotoLain.java:1079");
			}
			// e.printStackTrace(); // Optional logging
		}
	}
}
