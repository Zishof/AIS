package ais.database.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.common.BacaTulisUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.KelasLesSiswa;
import ais.database.model.sekolah.Siswa;

/**
 * Value object/proyeksi data untuk vo siswa. Tipe ini merangkum gabungan nilai yang dibutuhkan UI
 * atau laporan tanpa memperkenalkan entity persistence atau aturan transaksi baru.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code String dataJSON};
 * inisialisasi/lifecycle ({@code reInitHasilUjianMahasiswa()}); pembacaan/pencarian ({@code
 * getKelasLesDipilih()}, {@code ambilKelasLesSiswaId()}, {@code ambilKelasLesSiswa()}, {@code
 * ambilLokasiHasilUjianMahasiswa()}, {@code getUdahHasilUjianMahasiswa()}, {@code ambilHasilUjianMahasiswa()});
 * operasi domain lain ({@code tulisLokasiHasilUjianMahasiswa()}, {@code bersihkanLokasiHasilUjianMahasiswa()},
 * {@code populateHasilUjianMahasiswa()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
 * <p><b>Efek samping:</b> selain accessor state, operasi domain yang disebut di atas dapat membaca/mengubah
 * persistence, memicu lifecycle, atau membentuk komponen UI. Jangan menganggap model ini selalu murni;
 * panggil operasi tersebut melalui alur service dengan session, transaksi, dan otorisasi yang sesuai agar
 * perilakunya tidak disalin ke tempat lain.</p>
 *
 * @see GeneralValueObject
 */
public abstract class VOSiswa extends GeneralValueObject {

	/**
	 * 
	 */
	/**
	 * Penanda versi serialisasi Java untuk keluarga {@code VOSiswa}.
	 *
	 * <p>Dikunci agar objek yang sudah pernah diserialisasi tetap dapat dibaca setelah kelas ini
	 * diubah. Jangan menggantinya hanya karena menambah field atau method.</p>
	 */
	private static final long serialVersionUID = -4136659196530916378L;

	/**
	 * Cetakan awal dokumen JSON kosong ({@code "{}"}) untuk berkas indeks milik keluarga
	 * {@code VOSiswa}.
	 *
	 * <p><b>Field ini {@code public static} dan tidak {@code final}</b>, sehingga penulisan ke sana
	 * berlaku untuk seluruh JVM dan seluruh tenant. Perlakukan sebagai konstanta baca-saja.</p>
	 *
	 * <p>Perhatikan bahwa kelas ini <b>tidak memakai field miliknya sendiri</b>:
	 * {@link #ambilLokasiHasilUjianMahasiswa()} mengembalikan {@code VOMahasiswa.dataJSON} sebagai
	 * nilai jatuh-tempo, bukan {@code dataJSON} di sini. Isinya kebetulan sama, jadi perilakunya
	 * tidak berbeda — tetapi siapa pun yang mengubah salah satunya harus tahu bahwa yang berpengaruh
	 * pada kelas ini adalah milik {@link VOMahasiswa}. Field ini sendiri hanya dibaca dari luar
	 * kelas.</p>
	 */
	public static String dataJSON = new JSONObject().toString();

	/**
	 * Daftar id kelas les yang dipilih, disimpan sebagai satu teks berpemisah koma.
	 *
	 * <p>Dideklarasikan abstrak karena setiap subclass menyimpannya di kolom tabelnya sendiri.
	 * Ketiga implementasinya juga tidak sekadar mengembalikan kolom: {@link CalonSiswa} dan
	 * {@code AnggotaKoperasi} mengambil alih nilai dari data {@link Siswa} yang tertaut bila ada,
	 * sehingga nilai yang dikembalikan belum tentu isi kolom objek itu sendiri.</p>
	 *
	 * <p>Dipakai oleh {@link #ambilKelasLesSiswaId()} dan {@link #ambilKelasLesSiswa()} untuk
	 * diuraikan menjadi daftar id atau daftar entity. Keduanya memanggil
	 * {@code StringUtils.split(..., ",")} yang aman terhadap {@code null} — ia menghasilkan
	 * {@code null} yang kemudian membuat perulangan {@code for-each} melempar
	 * {@code NullPointerException}. Implementasi karenanya sebaiknya mengembalikan string kosong,
	 * bukan {@code null}.</p>
	 *
	 * @return teks id kelas les berpemisah koma
	 * @see #ambilKelasLesSiswaId()
	 * @see #ambilKelasLesSiswa()
	 */
	public abstract String getKelasLesDipilih();

	/**
	 * Menguraikan teks {@link #getKelasLesDipilih()} menjadi daftar id kelas les yang unik dan
	 * terurut.
	 *
	 * <p>Token kosong dilewati, token kembar dibuang, dan hasilnya diurutkan menaik. Ini pembacaan
	 * murni: tidak menyentuh basis data sama sekali, berbeda dengan {@link #ambilKelasLesSiswa()}
	 * yang menjalankan satu kueri per token.</p>
	 *
	 * <p><b>Token yang bukan angka dipetakan menjadi {@code -1L} dan tetap masuk ke daftar.</b> Ini
	 * perbedaan penting dari {@link #ambilKelasLesSiswa()}, yang memakai nilai {@code -1L} yang sama
	 * sebagai id kueri, tidak menemukan apa pun, lalu diam-diam melewatinya. Jadi untuk teks masukan
	 * yang sama, method ini mengembalikan satu entri {@code -1} sedangkan saudaranya mengembalikan
	 * daftar yang lebih pendek. Pemanggil yang memakai hasil di sini sebagai penanda pilihan pada
	 * komponen daftar — sebagaimana dilakukan layar siswa dan calon siswa — akan membawa serta id
	 * palsu {@code -1} itu. Karena hasil diurutkan menaik, {@code -1} selalu menjadi elemen pertama.
	 * Saring nilai negatif bila daftar ini dipakai untuk membangun kueri.</p>
	 *
	 * @return daftar id kelas les yang unik dan terurut menaik; dapat memuat {@code -1L} untuk setiap
	 *         token yang tidak berupa angka
	 */
	public List<Long> ambilKelasLesSiswaId() {

		List<Long> kelasLesSiswas = new ArrayList<Long>();

		for (String kode : StringUtils.split(getKelasLesDipilih(), ",")) {
			if (!kode.trim().isEmpty()) {
				Long id = !Common.isNumber(kode.trim()) ? -1L : Long.parseLong(kode.trim());
				if (id != null && !kelasLesSiswas.contains(id)) {
					kelasLesSiswas.add(id);
				}
			}
		}

		Collections.sort(kelasLesSiswas);
		return kelasLesSiswas;
	}

	/**
	 * Menguraikan teks {@link #getKelasLesDipilih()} menjadi daftar entity {@link KelasLesSiswa} yang
	 * benar-benar ada dan masih aktif.
	 *
	 * <p>Menjalankan <b>satu kueri terpisah untuk setiap token</b> pada teks masukan — pola N+1 yang
	 * jelas. Untuk peserta yang memilih banyak kelas les, jumlah perjalanan ke basis data sama dengan
	 * jumlah pilihannya. Bila daftar ini dibangun di dalam perulangan atas banyak siswa, biayanya
	 * berlipat. Pertimbangkan satu kueri {@code in (...)} bila jalur ini pernah menjadi titik
	 * lambat.</p>
	 *
	 * <p><b>Penyaring aktif bersifat dua arah:</b> {@code aktif is null OR aktif = true}. Baris lama
	 * yang kolom aktifnya belum pernah diisi diperlakukan sebagai aktif. Hanya nilai {@code false}
	 * yang benar-benar menyingkirkan sebuah kelas les. Ini pola yang berulang di basis kode ini dan
	 * perlu diingat saat menonaktifkan data: mengosongkan kolom tidak sama dengan menonaktifkan.</p>
	 *
	 * <p>Token yang bukan angka dijadikan id {@code -1L} yang tidak akan cocok dengan apa pun,
	 * sehingga hasilnya {@code null} dan dilewati tanpa pesan. Bandingkan dengan
	 * {@link #ambilKelasLesSiswaId()} yang justru menyimpan {@code -1L} itu ke dalam daftarnya. Kelas
	 * les yang sudah dihapus atau dinonaktifkan juga hilang diam-diam dari hasil, sehingga daftar ini
	 * bisa lebih pendek daripada jumlah pilihan yang tersimpan — jangan memakai ukurannya untuk
	 * memeriksa apakah peserta sudah memilih sesuatu; pakai
	 * {@link #ambilKelasLesSiswaId()} untuk itu.</p>
	 *
	 * <p>Pengurutan memakai {@code compareTo} milik {@link KelasLesSiswa}, bukan urutan id. Kueri
	 * dijalankan pada {@code HibernateUtil.currentSession()} — session milik permintaan yang sedang
	 * berjalan, jadi method ini tidak boleh dipanggil dari thread latar tanpa session yang sudah
	 * disiapkan.</p>
	 *
	 * @return daftar kelas les yang ada dan aktif, terurut menurut {@link KelasLesSiswa}
	 */
	public List<KelasLesSiswa> ambilKelasLesSiswa() {

		List<KelasLesSiswa> kelasLesSiswas = new ArrayList<KelasLesSiswa>();

		for (String kode : StringUtils.split(getKelasLesDipilih(), ",")) {
			if (!kode.trim().isEmpty()) {
				KelasLesSiswa kelasLesSiswa = (KelasLesSiswa) ConstantValues.simpleObject(HibernateUtil.currentSession()
						.createCriteria(KelasLesSiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("id", !Common.isNumber(kode.trim()) ? -1L : Long.parseLong(kode.trim())))
						.setMaxResults(1), KelasLesSiswa.class);
				if (kelasLesSiswa != null && !kelasLesSiswas.contains(kelasLesSiswa)) {
					kelasLesSiswas.add(kelasLesSiswa);
				}
			}
		}

		Collections.sort(kelasLesSiswas);
		return kelasLesSiswas;
	}

	/**
	 * Membaca berkas indeks hasil ujian milik peserta ini dan mengembalikan isinya sebagai teks JSON
	 * mentah.
	 *
	 * <p>Bagian dari mesin indeks berkas yang sejajar dengan milik {@link VOMahasiswa}: daftar hasil
	 * ujian seorang peserta tidak dimuat lewat relasi Hibernate melainkan lewat sebuah berkas cache
	 * berisi peta {@code {"<idHasilUjian>": "<idHasilUjian>"}}. Lokasinya diturunkan dari
	 * {@code Common.getFileLocation(this, "hasilUjianMahasiswa_" + getId())}, jadi kelas dan id
	 * bersama-sama menentukan berkas — {@link Siswa} ber-id 5 dan {@link CalonSiswa} ber-id 5 tidak
	 * berbagi berkas yang sama.</p>
	 *
	 * <p>Seluruh kegagalan — berkas tidak ada, kosong, atau tidak terbaca — menghasilkan
	 * {@code "{}"}. <b>Pemanggil tidak dapat membedakan "peserta ini memang belum punya hasil ujian"
	 * dari "berkas indeksnya hilang".</b> Jangan memakai hasil kosong sebagai dasar untuk menghapus
	 * data atau menyimpulkan peserta tidak pernah ujian; bangun ulang indeks lewat
	 * {@link #reInitHasilUjianMahasiswa(Session)} lebih dahulu.</p>
	 *
	 * <p>Memerlukan {@code getId() != null}; pada objek yang belum tersimpan akan melempar
	 * {@code NullPointerException} sebelum blok {@code try} sempat menangkap apa pun.</p>
	 *
	 * @return teks JSON indeks hasil ujian, atau {@code "{}"}; tidak pernah {@code null}
	 */
	public String ambilLokasiHasilUjianMahasiswa() {
		File file = Common.getFileLocation(this, "hasilUjianMahasiswa_" + getId().toString());
		try {
			// System.out.println(this + ", Baca file " + file);
			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOSiswa.java:78");
		}
		return VOMahasiswa.dataJSON;
	}

	/**
	 * Menimpa berkas indeks hasil ujian milik peserta ini dengan teks yang diberikan.
	 *
	 * <p>Pasangan tulis dari {@link #ambilLokasiHasilUjianMahasiswa()}. Menimpa seluruh isi berkas,
	 * bukan menambah; pemanggil yang hanya ingin menambah satu entri harus melakukan baca-ubah-tulis
	 * sendiri, sebagaimana dilakukan {@link #populateHasilUjianMahasiswa(HasilUjianMahasiswa)}.</p>
	 *
	 * <p>Tidak terkunci dan tidak atomik: dua alur yang memperbarui indeks peserta yang sama secara
	 * bersamaan akan saling menimpa. Akibatnya terbatas pada cache — baris hasil ujian di basis data
	 * tetap utuh dan indeks dapat dipulihkan dengan {@link #reInitHasilUjianMahasiswa(Session)}.</p>
	 *
	 * <p>Kegagalan I/O ditelan; pemanggil tidak akan tahu bila penulisan gagal.</p>
	 *
	 * @param data teks JSON lengkap yang akan menggantikan isi berkas
	 */
	public void tulisLokasiHasilUjianMahasiswa(String data) {
		File file = Common.getFileLocation(this, "hasilUjianMahasiswa_" + getId().toString());
		try {
			// System.out.println(this + ", Tulis file " + file);
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOSiswa.java:88");
		}
	}

	/**
	 * Menghapus berkas indeks hasil ujian milik peserta ini dari sistem berkas.
	 *
	 * <p>Hanya menyentuh cache; tidak ada baris {@link HasilUjianMahasiswa} yang dihapus dari basis
	 * data. Dipanggil sebagai langkah pertama {@link #reInitHasilUjianMahasiswa(Session)} sebelum
	 * indeks dibangun ulang.</p>
	 *
	 * <p><b>Tidak menyentuh penanda "sudah pernah dibangun"</b> yang dikelola
	 * {@link #getUdahHasilUjianMahasiswa()} — penanda itu berkas terpisah dengan nama yang berbeda.
	 * Memanggil method ini sendirian akan mengosongkan indeks sementara penanda tetap menyatakan
	 * indeks sudah siap, sehingga pembacaan berikutnya melaporkan peserta tidak punya hasil ujian
	 * sama sekali dan tidak akan membangun ulang. Jangan memanggilnya di luar
	 * {@link #reInitHasilUjianMahasiswa(Session)}.</p>
	 */
	public void bersihkanLokasiHasilUjianMahasiswa() {
		File file = Common.getFileLocation(this, "hasilUjianMahasiswa_" + getId().toString());
		BacaTulisUtil.doHapus(file, "hasilUjianMahasiswa");

	}

	/**
	 * Mendaftarkan satu hasil ujian ke dalam berkas indeks milik peserta ini.
	 *
	 * <p>Melakukan baca-ubah-tulis pada dokumen indeks: memuat isinya, menambahkan entri, lalu
	 * menulis ulang seluruh dokumen. Entri yang ditulis berbentuk {@code "<id>": "<id>"} — kunci dan
	 * nilai sama persis. Dokumen ini karenanya berfungsi sebagai himpunan id belaka, berbeda dengan
	 * indeks pengumpulan tugas pada {@link Tugas} yang memetakan id peserta ke id berkas. Jalur
	 * pembacaan di {@link #ambilHasilUjianMahasiswa(Session, boolean)} memang hanya memakai kuncinya
	 * dan sekadar memastikan nilainya tidak kosong.</p>
	 *
	 * <p><b>Memanggil {@code hasilUjianMahasiswa.write()} sebagai efek samping</b> — objek yang
	 * didaftarkan ikut ditulis ke berkasnya sendiri. Jadi method ini menyentuh dua berkas sekaligus,
	 * bukan hanya indeks. Bila salah satu penulisan gagal, yang lain bisa saja sudah berhasil dan
	 * keduanya menjadi tidak sinkron.</p>
	 *
	 * <p>Seluruh kegagalan ditelan tanpa memberi tahu pemanggil, termasuk
	 * {@code NullPointerException} bila {@code hasilUjianMahasiswa} atau id-nya {@code null} — baris
	 * itu diam-diam tidak masuk indeks.</p>
	 *
	 * @param hasilUjianMahasiswa hasil ujian yang didaftarkan; {@code null} diabaikan diam-diam
	 */
	public void populateHasilUjianMahasiswa(HasilUjianMahasiswa hasilUjianMahasiswa) {
		try {
			JSONObject c = new JSONObject(ambilLokasiHasilUjianMahasiswa());
			hasilUjianMahasiswa.write();
			c.put(hasilUjianMahasiswa.getId().toString(), hasilUjianMahasiswa.getId().toString());
			tulisLokasiHasilUjianMahasiswa(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOSiswa.java:104");
		}
	}

	/**
	 * Memeriksa apakah indeks hasil ujian peserta ini sudah pernah dibangun — <b>dan sekaligus
	 * menandainya sudah dibangun</b>.
	 *
	 * <h3>Namanya getter, perilakunya bukan</h3>
	 * <p>Meskipun berawalan {@code get} dan mengembalikan {@code boolean}, method ini <b>menulis ke
	 * sistem berkas</b>. Bila berkas penanda belum ada atau kosong, ia menuliskan {@code "true"} ke
	 * sana lalu mengembalikan {@code false}. Artinya pemanggilan pertama melaporkan "belum" dan
	 * seluruh pemanggilan berikutnya melaporkan "sudah", terlepas dari apakah indeks benar-benar
	 * pernah dibangun. Penanda dipasang oleh pemeriksaannya sendiri, bukan oleh pembangunan
	 * indeksnya.</p>
	 * <p>Konsekuensi praktisnya: siapa pun yang memanggil method ini sekadar untuk "mengintip" status
	 * — misalnya di dalam log, pemeriksaan diagnostik, atau ekspresi kondisi yang ternyata tidak jadi
	 * membangun indeks — telah menghabiskan satu-satunya kesempatan pembangunan otomatis. Setelah itu
	 * indeks hanya dapat dibangun dengan {@code refresh} eksplisit pada
	 * {@link #ambilHasilUjianMahasiswa(Session, boolean)}. Pola yang sama ada pada
	 * {@link VOMahasiswa#getUdahHasilUjianMahasiswa()} dan sudah didokumentasikan di sana.</p>
	 *
	 * <h3>Kegagalan dianggap "sudah"</h3>
	 * <p>Bila terjadi pengecualian saat membaca atau menulis penanda — cakram penuh, izin berkas,
	 * lintasan tidak sah — method mengembalikan {@code true}, yaitu "indeks sudah siap". Ini arah
	 * gagal yang <b>membuka</b>: pemanggil melewatkan pembangunan indeks dan menampilkan daftar hasil
	 * ujian yang kosong seolah-olah peserta memang belum pernah ujian. Untuk data nilai, gagal ke
	 * arah "bangun ulang saja" ({@code false}) akan jauh lebih aman daripada arah sekarang.</p>
	 *
	 * <h3>Catatan lain</h3>
	 * <p>Berkas penanda diberi nama memakai {@code this.getClass().getName()}, sehingga pada objek
	 * berproksi Hibernate namanya berbeda dari kelas aslinya dan satu objek yang sama dapat memiliki
	 * lebih dari satu penanda. Berkas indeksnya sendiri (lihat
	 * {@link #ambilLokasiHasilUjianMahasiswa()}) tidak memakai nama kelas, sehingga keduanya tidak
	 * selalu sejalan.</p>
	 * <p>Method ini juga masih memuat {@code System.out.println} yang mencetak isi penanda, id
	 * peserta, dan lintasan absolut berkas ke keluaran standar pada setiap pemanggilan. Ini sisa
	 * penelusuran yang seharusnya dibuang: ia mengotori log dan menuliskan id peserta beserta tata
	 * letak sistem berkas server ke sana.</p>
	 *
	 * @return {@code false} hanya pada pemanggilan pertama untuk peserta ini; {@code true} pada
	 *         pemanggilan berikutnya dan pada setiap kegagalan
	 */
	public boolean getUdahHasilUjianMahasiswa() {
		try {
			File file = Common.getFileLocation(this, this.getClass().getName() + "_udah_" + getId().toString());
			String data = ais.common.BacaTulisUtil.baca(file);
			System.out.println("data => " + data + ", id " + getId() + ", file " + file.getAbsolutePath());
			if (data == null || data.trim().isEmpty()) {
				ais.common.BacaTulisUtil.tulis(file, "true");
				return false;
			} else {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOSiswa.java:120");
		}
		return true;
	}

	/**
	 * Membangun ulang berkas indeks hasil ujian peserta ini dari basis data, menggantikan isi cache
	 * lama sepenuhnya.
	 *
	 * <p>Mengambil seluruh {@link HasilUjianMahasiswa} milik peserta ini, menghapus berkas indeks
	 * lama, menulis dokumen kosong, lalu mendaftarkan kembali setiap baris lewat
	 * {@link #populateHasilUjianMahasiswa(HasilUjianMahasiswa)} sambil memasukkannya ke cache
	 * entity.</p>
	 *
	 * <h3>Percabangan pemilik hanya mengenal dua dari tiga subclass</h3>
	 * <p>Penyaring pemiliknya ditulis sebagai
	 * {@code (this instanceof Siswa) ? eq("siswa", this) : eq("calonSiswa", this)} — sebuah
	 * percabangan dua arah yang mengandaikan hanya ada dua kemungkinan. Padahal {@link VOSiswa}
	 * memiliki <b>tiga</b> subclass: {@link Siswa}, {@link CalonSiswa}, dan
	 * {@code ais.database.model.koperasi.AnggotaKoperasi}. Cabang {@code else} karenanya juga
	 * menampung {@code AnggotaKoperasi}, dan akan mencocokkan objek itu terhadap relasi yang
	 * bertipe {@link CalonSiswa}.</p>
	 * <p>Yang dibandingkan Hibernate pada relasi seperti itu adalah nilai kunci asingnya, yaitu
	 * angka id. Karena {@code AnggotaKoperasi} dan {@link CalonSiswa} berada di tabel berbeda dengan
	 * urutan id yang berdiri sendiri, seorang anggota koperasi ber-id 30 akan menarik hasil ujian
	 * milik calon siswa ber-id 30 — lalu menuliskannya ke berkas indeks miliknya sendiri. Ini varian
	 * pola tabrakan id lintas-tabel yang berulang di basis kode ini.</p>
	 * <p><b>Keadaan ini belum dapat dicapai saat ini:</b> penelusuran seluruh basis kode tidak
	 * menemukan satu pun pemanggil yang menjalankan method ini — atau
	 * {@link #ambilHasilUjianMahasiswa(Session, boolean)} — pada objek {@link Siswa},
	 * {@link CalonSiswa}, maupun {@code AnggotaKoperasi}. Seluruh pemanggilan hasil ujian yang hidup
	 * berjalan lewat keluarga {@link VOMahasiswa} atau lewat {@code PertemuanPunyaUjian}. Jadi
	 * pasangan method ini adalah <b>kode tidur</b> — tidak berbahaya hari ini, tetapi merupakan
	 * ranjau bagi pemanggil pertama yang datang kemudian. Sebelum menghidupkannya, ganti percabangan
	 * itu menjadi tiga cabang eksplisit dengan cabang terakhir yang menolak tipe tak dikenal alih-alih
	 * menebaknya sebagai {@link CalonSiswa}.</p>
	 *
	 * <p>Seluruh baris dimuat ke memori sekaligus tanpa paging, dan berkas indeks ditulis ulang satu
	 * kali per baris. Di antara penghapusan berkas lama dan selesainya perulangan, indeks berada
	 * dalam keadaan tidak lengkap tanpa penguncian apa pun yang melindunginya.</p>
	 *
	 * @param session session Hibernate untuk kueri; dimiliki pemanggil dan tidak ditutup di sini
	 */
	@SuppressWarnings("unchecked")
	public void reInitHasilUjianMahasiswa(Session session) {
		List<HasilUjianMahasiswa> hasilUjianMahasiswas = session.createCriteria(HasilUjianMahasiswa.class)
				.addOrder(Order.asc("id"))
				.add((this instanceof Siswa) ? Restrictions.eq("siswa", this) : Restrictions.eq("calonSiswa", this))
				.list();
		bersihkanLokasiHasilUjianMahasiswa();
		tulisLokasiHasilUjianMahasiswa(new JSONObject().toString());
		for (HasilUjianMahasiswa hasilUjianMahasiswa : hasilUjianMahasiswas) {
			masukkanData(HasilUjianMahasiswa.class, hasilUjianMahasiswa);
			populateHasilUjianMahasiswa(hasilUjianMahasiswa);
		}
		hasilUjianMahasiswas = null;
	}

	/**
	 * Memuat seluruh hasil ujian milik peserta ini, membangun ulang indeksnya lebih dahulu bila
	 * perlu.
	 *
	 * <p>Indeks dibangun ulang bila {@code refresh} bernilai {@code true}, atau bila
	 * {@link #getUdahHasilUjianMahasiswa()} melaporkan indeks belum pernah dibangun — dengan catatan
	 * penting bahwa method pemeriksa itu memasang penandanya sendiri saat dibaca, sehingga
	 * pembangunan otomatis hanya terjadi sekali seumur hidup penanda tersebut.</p>
	 *
	 * <p>Setiap kunci pada dokumen indeks dicari lebih dahulu di cache entity; bila tidak ada, baris
	 * diambil dari basis data dan dimasukkan ke cache. Pada jalur kedua, {@code session} yang
	 * {@code null} akan diganti dengan {@code HibernateUtil.currentSession()} — jadi method ini dapat
	 * dipanggil tanpa session, tetapi hanya dari dalam daur permintaan yang sudah punya session.</p>
	 *
	 * <h3>Pembacaan yang menulis balik kepemilikan</h3>
	 * <p>Setiap baris yang ditemukan dikenai {@code setSiswa((Siswa) this)} atau
	 * {@code setCalonSiswa((CalonSiswa) this)} sebelum dimasukkan ke daftar hasil. Nilainya diambil
	 * dari {@code this}, bukan dari baris itu sendiri. Bila objek yang dimuat sedang terikat pada
	 * session Hibernate, mutasi ini menjadikannya kotor dan dapat ikut tersimpan pada flush
	 * berikutnya — sekadar membaca daftar hasil ujian cukup untuk <b>memindahkan kepemilikan sebuah
	 * hasil ujian secara permanen</b> ke peserta yang sedang dibaca. Digabungkan dengan salah-cabang
	 * pada {@link #reInitHasilUjianMahasiswa(Session)}, ini jalur yang dapat mengalihkan nilai ujian
	 * seseorang ke orang lain. Sekali lagi: pasangan method ini belum punya pemanggil hidup, jadi
	 * keadaan tersebut belum dapat dicapai hari ini.</p>
	 * <p>Untuk {@code AnggotaKoperasi} kedua cabang {@code instanceof} tidak terpenuhi, sehingga
	 * barisnya masuk ke daftar tanpa dimutasi — hasil ujian milik orang lain dikembalikan apa adanya
	 * tanpa satu pun penanda bahwa pemiliknya berbeda.</p>
	 *
	 * <p>Baris yang tidak dapat dimuat menghasilkan elemen {@code null} yang tetap ditambahkan ke
	 * daftar: cabang kedua memanggil {@code add(...)} tanpa memeriksa hasil kuerinya. Pemanggil wajib
	 * menjaga {@code null} saat memindai daftar yang dikembalikan.</p>
	 *
	 * <p>Setiap kegagalan per entri ditelan dan perulangan berlanjut; kegagalan pada seluruh dokumen
	 * menghasilkan daftar kosong. Dalam kedua hal itu pemanggil tidak diberi tahu apa pun — daftar
	 * sekadar tampil lebih pendek atau kosong.</p>
	 *
	 * @param session session Hibernate; boleh {@code null} bila dipanggil dari dalam daur permintaan
	 * @param refresh {@code true} untuk memaksa pembangunan ulang indeks dari basis data
	 * @return daftar hasil ujian milik peserta ini; dapat memuat elemen {@code null}
	 */
	@SuppressWarnings("unchecked")
	public List<HasilUjianMahasiswa> ambilHasilUjianMahasiswa(Session session, boolean refresh) {
		if (!getUdahHasilUjianMahasiswa() || refresh) {
			reInitHasilUjianMahasiswa(session);
		}

		List<HasilUjianMahasiswa> hasilUjianMahasiswasa = new ArrayList<HasilUjianMahasiswa>();
		try {
			JSONObject c = new JSONObject(ambilLokasiHasilUjianMahasiswa());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {

						GeneralValueObject generalValueObject = ambilData(HasilUjianMahasiswa.class, key);
						if (generalValueObject != null) {
							HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) generalValueObject;
							if (hasilUjianMahasiswa != null && (this instanceof Siswa)) {
								hasilUjianMahasiswa.setSiswa((Siswa) this);
							} else if (hasilUjianMahasiswa != null && (this instanceof CalonSiswa)) {
								hasilUjianMahasiswa.setCalonSiswa((CalonSiswa) this);
							}
							hasilUjianMahasiswasa.add(hasilUjianMahasiswa);
						} else {

							Long hasilUjianMahasiswaId = Long.parseLong(key);
							if (session == null) {
								session = HibernateUtil.currentSession();
							}
							HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) session
									.createCriteria(HasilUjianMahasiswa.class)
									.add(Restrictions.idEq(hasilUjianMahasiswaId)).uniqueResult();
							masukkanData(HasilUjianMahasiswa.class, hasilUjianMahasiswa);
							if (hasilUjianMahasiswa != null && (this instanceof Siswa)) {
								hasilUjianMahasiswa.setSiswa((Siswa) this);
							} else if (hasilUjianMahasiswa != null && (this instanceof CalonSiswa)) {
								hasilUjianMahasiswa.setCalonSiswa((CalonSiswa) this);
							}
							hasilUjianMahasiswasa.add(hasilUjianMahasiswa);

						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOSiswa.java:184");

				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOSiswa.java:188");

		}
		return hasilUjianMahasiswasa;
	}

}
