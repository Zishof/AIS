package ais.database.model;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.action.master.helper.KegiatanPersistenceHelper;
import ais.action.master.helper.PembayaranUtilHelper;
import ais.action.ws.util.PembayaranUtil;
import ais.common.BacaTulisUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;

/**
 * Value object/proyeksi data untuk vo mahasiswa. Tipe ini merangkum gabungan nilai yang dibutuhkan
 * UI atau laporan tanpa memperkenalkan entity persistence atau aturan transaksi baru.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * VoKunci}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code String dataJSON};
 * inisialisasi/lifecycle ({@code reInitHasilUjianMahasiswa()}, {@code reInitKegiatan()}); pembacaan/pencarian
 * ({@code ambilLokasiHasilUjianMahasiswa()}, {@code getUdahHasilUjianMahasiswa()}, {@code
 * ambilHasilUjianMahasiswa()}, {@code ambilLokasiDetailKegiatan()}, {@code ambilDetailKegiatanSaja()}, {@code
 * ambilDetailKegiatan()}); validasi/perhitungan ({@code hitungTotalCicilanPembayaran()}, {@code
 * hitungTotalCicilanPembayaran()}, {@code hitungTotalCicilanPembayaran()}, {@code
 * hitungTotalCicilanPembayaranPengecekanKrs()}, {@code hitungTotalCicilanPembayaran()}, {@code
 * hitungTotalCicilanPembayaran()}); penghapusan/pembatalan ({@code removeKegiatan()}, {@code
 * dendaCicilanDibatalkan()}); operasi domain lain ({@code tulisLokasiHasilUjianMahasiswa()}, {@code
 * bersihkanLokasiHasilUjianMahasiswa()}, {@code populateHasilUjianMahasiswa()}, {@code tulisLokasiKegiatan()},
 * {@code populateKegiatan()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut
 * di atas.</p>
 * <p><b>Efek samping:</b> selain accessor state, operasi domain yang disebut di atas dapat membaca/mengubah
 * persistence, memicu lifecycle, atau membentuk komponen UI. Jangan menganggap model ini selalu murni;
 * panggil operasi tersebut melalui alur service dengan session, transaksi, dan otorisasi yang sesuai agar
 * perilakunya tidak disalin ke tempat lain.</p>
 *
 *
 * <h3>Posisi dalam hierarki</h3>
 * <p>Rantai pewarisannya adalah {@code GeneralValueObject → ais.database.model.sop.DataSop →
 * VoKunci → VOMahasiswa}. Tiga subclass konkretnya adalah:</p>
 * <ul>
 * <li>{@link Mahasiswa} — mahasiswa terdaftar; relasi tagihannya memakai properti
 * {@code "mahasiswa"};</li>
 * <li>{@link BiodataCalonMahasiswa} — pendaftar yang belum menjadi mahasiswa; relasi tagihannya
 * memakai properti {@code "calonMahasiswa"} (kecuali pada
 * {@link #reInitHasilUjianMahasiswa(Session)} yang memakai {@code "biodataCalonMahasiswa"});</li>
 * <li>{@code ais.database.model.kursus.PesertaKursus} — peserta kursus. Kelas ini mewarisi
 * <b>seluruh</b> mesin penagihan di bawah tanpa pernah memakainya: modul kursus punya jalur
 * penagihannya sendiri, sementara tiap kueri di kelas ini bercabang hanya untuk
 * {@code Mahasiswa} dan {@code BiodataCalonMahasiswa}. Akibatnya, memanggil
 * {@link #ambilKegiatans()} atau {@link #ambilCicilan()} pada instance {@code PesertaKursus} akan
 * jatuh ke cabang "bukan Mahasiswa" dan mengeksekusi restriksi {@code calonMahasiswa} terhadap
 * objek yang bukan calon mahasiswa. Jangan memanggil operasi penagihan di kelas ini dari kode
 * modul kursus.</li>
 * </ul>
 *
 * <h3>Peta besar: mesin penagihan mahasiswa</h3>
 * <p>Sebagian besar isi kelas ini adalah satu mesin yang menjawab dua pertanyaan: "apa saja yang
 * ditagihkan kepada orang ini" dan "berapa yang sudah dibayar". Mesin tersebut bekerja di atas
 * tiga lapis penyimpanan yang perlu dipahami bersama-sama:</p>
 * <ol>
 * <li><b>Basis data</b> — sumber kebenaran. Entity {@link Kegiatan} (satu tagihan per
 * semester per {@link JenisKegiatan}), {@link DetailKegiatan} (rincian per item biaya), dan
 * {@link CicilanPembayaran} (setiap setoran pembayaran).</li>
 * <li><b>Berkas indeks per orang</b> — untuk setiap instance disimpan berkas JSON berisi
 * <i>daftar id</i> (bukan datanya) dari kegiatan, cicilan, hasil ujian, dan detail kegiatan
 * miliknya. Berkas-berkas inilah yang dibaca {@code ambilLokasiXxx()} dan ditulis
 * {@code tulisLokasiXxx()}. Bentuknya {@link org.json.JSONObject} dengan id sebagai kunci
 * sekaligus nilai; "penghapusan" dilakukan dengan menyetel nilainya menjadi string kosong,
 * bukan membuang kuncinya (lihat {@link #removeKegiatan(Serializable)}).</li>
 * <li><b>Cache objek dalam proses</b> — {@code masukkanData}/{@code ambilData}/
 * {@code ambilDataBanyak} warisan {@link GeneralValueObject} yang memetakan id menjadi objek
 * entity tanpa menyentuh basis data lagi.</li>
 * </ol>
 * <p>Alur bacanya seragam: {@code ambilLokasiXxx()} menghasilkan daftar id dari berkas indeks,
 * {@code ambilDataBanyak} mengubahnya menjadi objek, dan bila indeks dianggap basi
 * ({@code refresh == true} atau penanda {@code udah(...)} belum terpasang) maka
 * {@code reInitXxx(session)} menembak basis data lalu menulis ulang berkas indeks.</p>
 *
 * <h3>Yang harus diwaspadai saat memakai kelas ini</h3>
 * <ul>
 * <li><b>{@link #ambilLokasiCicilan()} bersifat destruktif</b> — ia menghapus berkas indeksnya
 * sendiri segera setelah membaca. Pemanggilan kedua tanpa {@code refresh} akan mengembalikan
 * JSON kosong. Ini bukan getter yang aman dipanggil berulang.</li>
 * <li><b>{@link #getUdahHasilUjianMahasiswa()} menulis saat dibaca</b> — namanya {@code get...}
 * tetapi ia memasang penanda berkas pada pemanggilan pertama (pola test-and-set).</li>
 * <li><b>{@link #hitungTotalCicilan(Kegiatan, PengaturanPembayaranBulanan, Collection)}
 * menulis ke basis data</b> — method statis bernama "hitung" ini membuka transaksi dan memperbarui
 * baris {@link CicilanPembayaran} sebagai efek samping. Lihat dokumentasinya.</li>
 * <li><b>Tidak ada penyaringan tenant/kepemilikan di kelas ini.</b> Semua kueri dibatasi pada
 * orang yang diwakili {@code this}, sehingga scoping datang dari objek pemanggil — bukan dari
 * pemeriksaan hak akses. Memuat entity {@link Mahasiswa} milik satuan kerja lain lalu memanggil
 * method di kelas ini akan mengembalikan datanya tanpa keberatan; otorisasi harus sudah selesai
 * sebelum sampai ke sini.</li>
 * <li><b>Manajemen session tidak seragam.</b> Sebagian method menerima {@link Session} dari
 * pemanggil, sebagian membuka session baru lewat {@code openSession()} dan menutupnya sendiri,
 * dan sebagian memakai {@code currentNativeSession()} lalu memanggil
 * {@code HibernateUtil.closeSession()} yang juga menutup session milik thread. Perhatikan
 * dokumentasi tiap method sebelum memanggilnya dari dalam transaksi yang sedang berjalan.</li>
 * </ul>
 * @see VoKunci
 */
public abstract class VOMahasiswa extends VoKunci {

	/**
	 * Versi serialisasi kelas ini, diwarisi oleh ketiga subclass konkretnya.
	 *
	 * <p>Dipatok eksplisit agar instance yang disimpan di session HTTP tetap terbaca setelah
	 * penambahan method/field pada kelas ini. Perhatikan bahwa state yang benar-benar
	 * diserialisasi hanyalah field entity milik subclass; seluruh cache yang dipakai mesin
	 * penagihan hidup di berkas indeks dan cache proses, bukan di dalam objek ini, sehingga objek
	 * hasil deserialisasi tetap harus melakukan {@code refresh} untuk mendapat data mutakhir.</p>
	 */
	private static final long serialVersionUID = -4136659196530916378L;

	/**
	 * Membaca berkas indeks hasil ujian milik orang ini dan mengembalikan isinya sebagai teks JSON.
	 *
	 * <p>Berkas yang dibaca berkunci {@code "hasilUjianMahasiswa_" + getId()} di bawah lokasi
	 * berkas milik entity ini ({@code Common.getFileLocation}). Isinya adalah
	 * {@link org.json.JSONObject} yang memetakan id {@link HasilUjianMahasiswa} ke dirinya sendiri
	 * — sebuah himpunan id yang disimpan sebagai peta, bukan data hasil ujian itu sendiri.</p>
	 *
	 * <p><b>Berbeda dari {@link #ambilLokasiCicilan()}, method ini TIDAK menghapus berkasnya
	 * setelah membaca</b>, sehingga aman dipanggil berulang kali.</p>
	 *
	 * <p>Tiga keadaan berikut sama-sama menghasilkan JSON kosong {@code "{}"} tanpa kesalahan:
	 * entity ini belum tersimpan (id masih {@code null}, mis. dipanggil dari layar registrasi
	 * sebelum simpan), berkas belum pernah ditulis, atau pembacaan berkas gagal (kegagalan dicatat
	 * ke audit lalu ditelan). Pemanggil karenanya tidak bisa membedakan "tidak ada hasil ujian"
	 * dari "gagal membaca cache" hanya dari nilai baliknya.</p>
	 *
	 * @return teks JSON berisi himpunan id hasil ujian; tidak pernah {@code null} dan minimal
	 *         berupa {@code "{}"}
	 */
	public String ambilLokasiHasilUjianMahasiswa() {
		// Null-safe: entitas transient (id null, mis. dipanggil sebelum tersimpan)
		// tak punya file keyed-by-id → cegah NPE (lihat pola sama di ambilLokasiKegiatan dkk).
		if (getId() == null) {
			return VOMahasiswa.dataJSON;
		}
		File file = Common.getFileLocation(this, "hasilUjianMahasiswa_" + getId().toString());
		try {
			// System.out.println(this + ", Baca file " + file);
			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOMahasiswa.java:44");
		}
		return VOMahasiswa.dataJSON;
	}

	/**
	 * Menimpa berkas indeks hasil ujian milik orang ini dengan teks JSON yang diberikan.
	 *
	 * <p>Pasangan tulis dari {@link #ambilLokasiHasilUjianMahasiswa()}, memakai kunci berkas yang
	 * sama. Isi lama dibuang seluruhnya — untuk menambah satu id ke indeks yang sudah ada, pakai
	 * {@link #populateHasilUjianMahasiswa(HasilUjianMahasiswa)} yang membaca dulu lalu menulis
	 * kembali.</p>
	 *
	 * <p>Bila entity ini belum punya id (masih transient), method langsung kembali tanpa berbuat
	 * apa pun: tidak ada berkas berkunci id yang bisa ditulis. Kegagalan I/O dicatat ke audit dan
	 * ditelan, sehingga pemanggil tidak pernah tahu apakah penulisan benar-benar berhasil.</p>
	 *
	 * @param data teks JSON yang akan menggantikan isi berkas indeks; pemanggil bertanggung jawab
	 *             memastikan bentuknya valid, karena method ini tidak memvalidasinya dan JSON
	 *             rusak baru akan ketahuan saat dibaca kembali
	 */
	public void tulisLokasiHasilUjianMahasiswa(String data) {
		// Null-safe: tanpa id tak ada file keyed-by-id untuk ditulis.
		if (getId() == null) {
			return;
		}
		File file = Common.getFileLocation(this, "hasilUjianMahasiswa_" + getId().toString());
		try {
			// System.out.println(this + ", Tulis file " + file);
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOMahasiswa.java:54");
		}
	}

	/**
	 * Menghapus berkas indeks hasil ujian milik orang ini dari penyimpanan.
	 *
	 * <p>Berbeda dari {@link #tulisLokasiHasilUjianMahasiswa(String)} yang menimpa isi berkas,
	 * method ini membuang berkasnya. Dipakai {@link #reInitHasilUjianMahasiswa(Session)} sebagai
	 * langkah pertama sebelum membangun ulang indeks dari basis data.</p>
	 *
	 * <p><b>Tidak null-safe.</b> Berbeda dari method sekeluarganya, method ini memanggil
	 * {@code getId().toString()} tanpa memeriksa {@code null} lebih dulu, sehingga memanggilnya
	 * pada entity yang belum tersimpan akan melempar {@link NullPointerException}. Dalam alur
	 * yang ada saat ini hal itu tidak terjadi karena satu-satunya pemanggilnya,
	 * {@link #reInitHasilUjianMahasiswa(Session)}, selalu dijalankan atas entity yang sudah
	 * tersimpan; tetapi pemanggil baru perlu menyadari perbedaan ini.</p>
	 *
	 * <p>Berkas yang dihapus hanya berisi daftar id — baris {@link HasilUjianMahasiswa} di basis
	 * data tidak tersentuh sama sekali.</p>
	 */
	public void bersihkanLokasiHasilUjianMahasiswa() {
		File file = Common.getFileLocation(this, "hasilUjianMahasiswa_" + getId().toString());
		BacaTulisUtil.doHapus(file, "hasilUjianMahasiswa");

	}

	/**
	 * Mendaftarkan satu {@link HasilUjianMahasiswa} ke dalam berkas indeks milik orang ini.
	 *
	 * <p>Urutan kerjanya: baca indeks yang ada lewat {@link #ambilLokasiHasilUjianMahasiswa()},
	 * panggil {@code hasilUjianMahasiswa.write()} agar hasil ujian tersebut menuliskan berkas
	 * datanya sendiri, tambahkan id-nya sebagai pasangan kunci-nilai, lalu tulis kembali indeks
	 * lewat {@link #tulisLokasiHasilUjianMahasiswa(String)}.</p>
	 *
	 * <p>Karena baca-ubah-tulis di sini tidak atomik, dua thread yang mendaftarkan hasil ujian
	 * berbeda untuk orang yang sama secara bersamaan dapat saling menimpa sehingga salah satu id
	 * hilang dari indeks. Data di basis data tetap utuh; yang hilang hanya entri cache, dan
	 * pemanggilan berikutnya dengan {@code refresh == true} akan memulihkannya.</p>
	 *
	 * <p>Seluruh kegagalan (termasuk {@code hasilUjianMahasiswa} atau id-nya bernilai
	 * {@code null}) ditelan dan dicatat ke audit; method tidak pernah melempar ke pemanggil.</p>
	 *
	 * @param hasilUjianMahasiswa hasil ujian yang akan didaftarkan; harus sudah punya id
	 */
	public void populateHasilUjianMahasiswa(HasilUjianMahasiswa hasilUjianMahasiswa) {
		try {
			JSONObject c = new JSONObject(ambilLokasiHasilUjianMahasiswa());
			hasilUjianMahasiswa.write();
			c.put(hasilUjianMahasiswa.getId().toString(), hasilUjianMahasiswa.getId().toString());
			tulisLokasiHasilUjianMahasiswa(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOMahasiswa.java:70");
		}
	}

	/**
	 * Penanda bergaya <b>test-and-set</b>: menjawab "apakah indeks hasil ujian orang ini sudah
	 * pernah dibangun", sekaligus memasang penandanya bila belum.
	 *
	 * <p><b>Ini bukan getter murni meski namanya diawali {@code get}.</b> Pada pemanggilan
	 * pertama untuk suatu entity, berkas penanda masih kosong sehingga method <i>menuliskan</i>
	 * {@code "true"} ke berkas tersebut lalu mengembalikan {@code false} ("belum pernah — dan
	 * sekarang sudah ditandai"). Pemanggilan berikutnya menemukan berkas terisi dan mengembalikan
	 * {@code true} tanpa mengubah apa pun. Pola inilah yang dipakai
	 * {@link #ambilHasilUjianMahasiswa(Session, boolean)} untuk menjalankan
	 * {@link #reInitHasilUjianMahasiswa(Session)} tepat sekali.</p>
	 *
	 * <p><b>Duplikasi dari {@link GeneralValueObject#udah(String)}.</b> Logika di sini ditulis
	 * ulang alih-alih memanggil penanda generik milik kelas induk, dan kunci berkas yang dipakai
	 * — {@code getClass().getName() + "_udah_" + getId()} — <b>identik</b> dengan kunci yang
	 * dihasilkan {@code udah("")} (yakni {@code udah()} tanpa argumen). Selama kode yang ada tidak
	 * pernah memanggil {@code udah()} tanpa argumen pada {@link Mahasiswa} maupun
	 * {@link BiodataCalonMahasiswa}, tabrakan itu tidak muncul; namun pemanggilan {@code udah()}
	 * baru pada salah satu subclass akan diam-diam mematikan inisialisasi indeks hasil ujian
	 * (atau sebaliknya), karena keduanya berbagi satu berkas penanda yang sama. Bila membutuhkan
	 * penanda baru pada subclass ini, pakai {@code udah("sufiksTersendiri")} agar kuncinya
	 * berbeda.</p>
	 *
	 * <p><b>Perilaku pada entity transient.</b> Bila id masih {@code null}, method mengembalikan
	 * {@code true} ("anggap sudah") agar {@link #reInitHasilUjianMahasiswa(Session)} dilewati:
	 * restriksi Hibernate di sana membutuhkan id sehingga akan gagal. Konsekuensinya daftar hasil
	 * ujian akan tampil kosong sampai entity tersimpan — pilihan yang disengaja agar layar tidak
	 * meledak, bukan bug.</p>
	 *
	 * <p><b>Perilaku saat gagal I/O</b> sama dengan {@link GeneralValueObject#udah(String)}:
	 * kesalahan dicetak dan dicatat, lalu method mengembalikan {@code true} sehingga pembangunan
	 * ulang indeks dilewati. Sikap ini menghindari kerja berat berulang tanpa henti, dengan risiko
	 * data cache basi; pemanggil yang membutuhkan data segar harus memakai parameter
	 * {@code refresh}.</p>
	 *
	 * <p>Method ini juga mencetak baris diagnostik ke keluaran standar setiap kali dipanggil.</p>
	 *
	 * @return {@code true} bila penanda sudah terpasang sebelumnya, bila entity belum tersimpan,
	 *         atau bila terjadi kegagalan I/O; {@code false} hanya pada pemanggilan pertama yang
	 *         berhasil memasang penanda
	 */
	public boolean getUdahHasilUjianMahasiswa() {
		// Null-safe: entitas transient (id null, mis. belum tersimpan) tak punya file
		// keyed-by-id dan tak bisa direstriksi ke DB (Restrictions.eq("mahasiswa", this)
		// butuh id). Anggap "udah" (true) supaya reInitHasilUjianMahasiswa dilewati,
		// bukan crash — hasil ujian akan tampil kosong sampai entitas tersimpan.
		if (getId() == null) {
			return true;
		}
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
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOMahasiswa.java:86");
		}
		return true;
	}

	/**
	 * Membangun ulang berkas indeks hasil ujian orang ini dari basis data, membuang isi lama.
	 *
	 * <p>Langkah kerjanya: kueri seluruh {@link HasilUjianMahasiswa} milik orang ini terurut
	 * menaik berdasarkan id, hapus berkas indeks lama
	 * ({@link #bersihkanLokasiHasilUjianMahasiswa()}), tulis indeks kosong, lalu untuk setiap
	 * baris hasil kueri masukkan objeknya ke cache proses ({@code masukkanData}) dan daftarkan
	 * id-nya ke indeks ({@link #populateHasilUjianMahasiswa(HasilUjianMahasiswa)}).</p>
	 *
	 * <p><b>Percabangan properti relasi.</b> Restriksi kueri dipilih dengan
	 * {@code this instanceof Mahasiswa}: bila benar dipakai properti {@code "mahasiswa"}, bila
	 * tidak dipakai {@code "biodataCalonMahasiswa"}. Perhatikan bahwa nama properti cabang kedua
	 * di sini adalah {@code "biodataCalonMahasiswa"}, sedangkan pada
	 * {@link #reInitKegiatan(Session)} dan {@link #ambilPengeluaranMahasiswa()} properti yang
	 * dipakai untuk objek yang sama bernama {@code "calonMahasiswa"} — penamaan yang berbeda pada
	 * entity yang berbeda, jadi jangan menyeragamkannya tanpa memeriksa pemetaan Hibernate
	 * masing-masing. Karena percabangan hanya mengenal dua kemungkinan, subclass ketiga
	 * ({@code PesertaKursus}) akan masuk ke cabang calon mahasiswa dan menghasilkan kueri yang
	 * tidak bermakna.</p>
	 *
	 * <p><b>Tidak menutup atau membuka session.</b> Session yang dipakai adalah milik pemanggil
	 * dan harus masih terbuka. Method ini juga tidak dibungkus transaksi karena hanya membaca;
	 * yang berubah adalah berkas indeks di penyimpanan, bukan basis data.</p>
	 *
	 * <p><b>Tidak memasang penanda.</b> Berbeda dari sebagian mesin sejenis, method ini tidak
	 * memanggil {@link #getUdahHasilUjianMahasiswa()}; penanda dipasang oleh pemeriksaan di
	 * {@link #ambilHasilUjianMahasiswa(Session, boolean)} sebelum method ini dipanggil.</p>
	 *
	 * @param session session Hibernate yang masih terbuka; {@code null} akan menyebabkan
	 *                {@link NullPointerException} karena method ini tidak menyediakan cadangan
	 */
	@SuppressWarnings("unchecked")
	public void reInitHasilUjianMahasiswa(Session session) {
		List<HasilUjianMahasiswa> hasilUjianMahasiswas = session.createCriteria(HasilUjianMahasiswa.class)
				.addOrder(Order.asc("id")).add((this instanceof Mahasiswa) ? Restrictions.eq("mahasiswa", this)
						: Restrictions.eq("biodataCalonMahasiswa", this))
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
	 * Mengambil seluruh hasil ujian milik orang ini, memakai berkas indeks bila memungkinkan.
	 *
	 * <p>Alurnya terdiri atas dua tahap.</p>
	 *
	 * <p><b>Tahap 1 — memastikan indeks ada.</b> Bila {@code refresh} bernilai benar, atau bila
	 * {@link #getUdahHasilUjianMahasiswa()} melaporkan indeks belum pernah dibangun, maka
	 * {@link #reInitHasilUjianMahasiswa(Session)} dijalankan lebih dulu. Ingat bahwa pemeriksaan
	 * penanda itu sendiri menulis berkas pada pemanggilan pertama.</p>
	 *
	 * <p><b>Tahap 2 — mengubah id menjadi objek.</b> Setiap kunci pada indeks ditelusuri. Untuk
	 * tiap id, objeknya dicari lebih dulu di cache proses ({@code ambilData}); bila tidak ada di
	 * sana, method menembak basis data dengan {@code Restrictions.idEq} dan memasukkan hasilnya ke
	 * cache. Entri yang nilainya string kosong dilewati — itulah cara indeks menandai id yang
	 * "dihapus" tanpa membuang kuncinya.</p>
	 *
	 * <p><b>Penyambungan relasi balik.</b> Setiap objek yang dikumpulkan disetel relasi
	 * pemiliknya kembali ke {@code this} ({@code setMahasiswa} atau
	 * {@code setBiodataCalonMahasiswa} sesuai tipe). Ini menghindari inisialisasi proxy lazy saat
	 * pemanggil membaca relasi tersebut, tetapi juga berarti objek yang dikembalikan
	 * <b>dimodifikasi</b> oleh method ini. Bila objek tersebut sedang terkelola oleh session yang
	 * terbuka, perubahan itu berpotensi ikut tersimpan pada {@code flush} berikutnya walaupun
	 * nilainya semestinya sama; jangan memanggil method ini di tengah transaksi tulis kecuali
	 * memang menghendaki hal tersebut.</p>
	 *
	 * <p><b>Ketahanan terhadap indeks basi.</b> Id yang tercatat di indeks tetapi barisnya sudah
	 * terhapus di basis data akan menghasilkan {@code null} dari kueri; entri seperti itu
	 * dilewati, tidak membatalkan seluruh pengambilan. Seluruh kegagalan per-entri maupun
	 * kegagalan penguraian JSON ditelan dan dicatat ke audit, sehingga method ini mengembalikan
	 * daftar sebagian alih-alih melempar — pemanggil tidak dapat membedakan "memang kosong" dari
	 * "gagal sebagian".</p>
	 *
	 * <p><b>Session.</b> Parameter {@code session} boleh {@code null} untuk tahap 2: method akan
	 * mengambil session milik thread lewat {@code HibernateUtil.currentSession()} saat pertama
	 * kali benar-benar membutuhkannya. Namun bila tahap 1 sampai berjalan,
	 * {@link #reInitHasilUjianMahasiswa(Session)} dipanggil dengan nilai {@code session} apa
	 * adanya dan akan gagal bila {@code null}. Jadi: {@code null} hanya aman ketika indeks
	 * dipastikan sudah ada dan {@code refresh} bernilai salah.</p>
	 *
	 * @param session session Hibernate; boleh {@code null} hanya dalam kondisi yang dijelaskan di
	 *                atas
	 * @param refresh {@code true} untuk memaksa membangun ulang indeks dari basis data
	 * @return daftar hasil ujian; kosong bila tidak ada, tidak pernah {@code null}. Urutannya
	 *         mengikuti urutan penelusuran kunci JSON, bukan urutan id
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
							if (hasilUjianMahasiswa != null && (this instanceof Mahasiswa)) {
								hasilUjianMahasiswa.setMahasiswa((Mahasiswa) this);
							} else if (hasilUjianMahasiswa != null && (this instanceof BiodataCalonMahasiswa)) {
								hasilUjianMahasiswa.setBiodataCalonMahasiswa((BiodataCalonMahasiswa) this);
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
							// FIX NPE: uniqueResult() bisa null (id tercatat di lokasi cache tapi baris
							// HasilUjianMahasiswa sudah terhapus di DB) -> masukkanData(clazz, null)
							// meledak di DataUtil.masukkanData saat obj.getId(). Lewati entri ini saja.
							if (hasilUjianMahasiswa != null) {
								masukkanData(HasilUjianMahasiswa.class, hasilUjianMahasiswa);
								if (this instanceof Mahasiswa) {
									hasilUjianMahasiswa.setMahasiswa((Mahasiswa) this);
								} else if (this instanceof BiodataCalonMahasiswa) {
									hasilUjianMahasiswa.setBiodataCalonMahasiswa((BiodataCalonMahasiswa) this);
								}
								hasilUjianMahasiswasa.add(hasilUjianMahasiswa);
							}

						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOMahasiswa.java:150");

				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOMahasiswa.java:154");

		}
		return hasilUjianMahasiswasa;
	}

	public String ambilLokasiDetailKegiatan() {
		// Null-safe: entitas transient (id null) tak punya file keyed-by-id → cegah NPE.
		if (getId() == null) {
			return VOMahasiswa.dataJSON;
		}
		File file = Common.getFileLocation(this, "detailKegiatan_" + getId().toString());
		if (file != null && file.exists()) {
			try {
				// System.out.println(this + ", Baca file " + file);
				String data = ais.common.BacaTulisUtil.baca(file);
				// ais.common.BacaTulisUtil.hapus(file);
				return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOMahasiswa.java:172");
			}
		}
		return VOMahasiswa.dataJSON;
	}

	public Collection<DetailKegiatan> ambilDetailKegiatanSaja(boolean refresh) {
		// Panggil Helper Statis dengan menyuntikkan nilai dari konteks class ini (this)
		return KegiatanPersistenceHelper.ambilDetailKegiatanSaja(this, // Parameter 1: Object student (this merujuk ke
																		// class saat ini)
				this.ambilLokasiDetailKegiatan(), // Parameter 2: JSON Cache lokasi dari method di class ini
				this.ambilKegiatansData(refresh, null), // Parameter 4: Data parent Kegiatan dari cache
				refresh // Parameter 5: Flag boolean refresh dari argument
		);
	}

	public Collection<DetailKegiatan> ambilDetailKegiatan(Kegiatan kegiatan, boolean refresh) {
		return KegiatanPersistenceHelper.ambilDetailKegiatanSaja(kegiatan, refresh);
	}

	public List<DetailKegiatan> ambilDetailKegiatan(Kegiatan kegiatan, DetailBiaya detailBiaya, boolean refresh) {
		Collection<DetailKegiatan> detailKegiatansTemp = ambilDetailKegiatan(kegiatan, refresh);
		List<DetailKegiatan> detailKegiatans = new ArrayList<DetailKegiatan>();
		for (DetailKegiatan detailKegiatan : detailKegiatansTemp) {
			try {

				if (kegiatan.getId().equals(detailKegiatan.getKegiatan().getId())
						&& detailBiaya.getId().equals(detailKegiatan.getDetailBiaya().getId())) {
					detailKegiatans.add(detailKegiatan);
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOMahasiswa.java:203");
			}
		}

		if (detailKegiatans.isEmpty()) {
			for (DetailKegiatan detailKegiatan : detailKegiatansTemp) {
				try {
					if (kegiatan.getId().equals(detailKegiatan.getKegiatan().getId())
							&& detailBiaya.getItemBiaya().getId()
									.equals(detailKegiatan.getDetailBiaya().getItemBiaya().getId())
							&& detailBiaya.getBayarKe().equals(detailKegiatan.getDetailBiaya().getBayarKe())) {
						detailKegiatans.add(detailKegiatan);
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOMahasiswa.java:217");
				}
			}
		}

		// System.out.println("detailKegiatansTemp detailBiaya " + detailBiaya +
		// " -> " + detailKegiatans);
		detailKegiatansTemp = null;
		return detailKegiatans;
	}

	public List<DetailKegiatan> ambilDetailKegiatan(Kegiatan kegiatan,
			PengaturanPembayaranBulanan pengaturanPembayaranBulanan, boolean refresh) {
		Collection<DetailKegiatan> detailKegiatansTemp = ambilDetailKegiatan(kegiatan, refresh);
		List<DetailKegiatan> detailKegiatans = new ArrayList<DetailKegiatan>();
		for (DetailKegiatan detailKegiatan : detailKegiatansTemp) {
			try {
				if (kegiatan.getId().equals(detailKegiatan.getKegiatan().getId()) && pengaturanPembayaranBulanan.getId()
						.equals(detailKegiatan.getPengaturanPembayaranBulanan().getId())) {
					detailKegiatans.add(detailKegiatan);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOMahasiswa.java:238");
				// TODO: handle exception
			}
		}
		detailKegiatansTemp = null;
		return detailKegiatans;
	}

	public List<DetailKegiatan> ambilDetailKegiatan(Kegiatan kegiatan,
			PengaturanPembayaranBulanan pengaturanPembayaranBulanan, Collection<DetailKegiatan> detailKegiatansTemp) {
		List<DetailKegiatan> detailKegiatans = new ArrayList<DetailKegiatan>();
		if (detailKegiatansTemp != null && !detailKegiatansTemp.isEmpty()) {
			for (DetailKegiatan detailKegiatan : detailKegiatansTemp) {
				try {
					if (kegiatan.getId().equals(detailKegiatan.getKegiatan().getId()) && pengaturanPembayaranBulanan
							.getId().equals(detailKegiatan.getPengaturanPembayaranBulanan().getId())) {
						detailKegiatans.add(detailKegiatan);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOMahasiswa.java:256");
					// e.printStackTrace();
				}
			}
		}

		if (detailKegiatansTemp != null && detailKegiatans.isEmpty()) {
			for (DetailKegiatan detailKegiatan : detailKegiatansTemp) {
				try {
					// Long kegId = kegiatan.getId();
					// Long kegIdref = detailKegiatan.getKegiatan().getId();
					// Long detBiaya =
					// pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getId();
					// Long detBiayaref =
					// detailKegiatan.getDetailBiaya().getItemBiaya().getId();
					Integer realbulan = pengaturanPembayaranBulanan.getRealBulan();
					Integer realbulanref = detailKegiatan.getPengaturanPembayaranBulanan().getRealBulan();
					// System.out.println(
					// "kegId " + kegId + " kegIdref " + kegIdref + " detBiaya "
					// + detBiaya + " detBiayaref "
					// + detBiayaref + " realbulan " + realbulan + "
					// realbulanref " + realbulanref);
					if (kegiatan.getId().equals(detailKegiatan.getKegiatan().getId())

							&& pengaturanPembayaranBulanan.getDetailBiaya().getBayarKe()
									.equals(detailKegiatan.getDetailBiaya().getBayarKe())

							&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getId()
									.equals(detailKegiatan.getDetailBiaya().getItemBiaya().getId())
							&& realbulan.equals(realbulanref)) {
						detailKegiatans.add(detailKegiatan);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOMahasiswa.java:288");
					// e.printStackTrace();
				}
			}
		}

		return detailKegiatans;
	}

	@SuppressWarnings("unchecked")
	public List<PengeluaranMahasiswa> ambilPengeluaranMahasiswa() {
		List<PengeluaranMahasiswa> pengeluaranMahasiswas = new ArrayList<PengeluaranMahasiswa>();

		Session session = null;

		try {
			// Buka session secara eksplisit dan terisolasi
			session = HibernateUtil.getSessionFactory().openSession();

			// 2. Pembuatan Criteria secara dinamis dan efisien
			Criteria criteria = session.createCriteria(PengeluaranMahasiswa.class);
			criteria.setProjection(Projections.property("id"));

			// Kondisi spesifik berdasarkan instance
			if (this instanceof Mahasiswa) {
				criteria.add(Restrictions.eq("mahasiswa", this));
			} else {
				criteria.add(Restrictions.eq("calonMahasiswa", this));
			}

			// Pengurutan (Ordering)
			criteria.addOrder(Order.asc("id"));

			TreeSet<Long> keyData = new TreeSet<Long>(criteria.list());
			pengeluaranMahasiswas = GeneralValueObject.ambilDataBanyak(PengeluaranMahasiswa.class, keyData);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOMahasiswa.java:324");
		} finally {
			// 8. Pembersihan Session yang sangat ketat mencegah Memory Leak & Connection
			// Timeout
			if (session != null) {
				try {
					if (session.isOpen()) {
						session.clear();
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOMahasiswa.java:333");
				}

				try {
					session.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOMahasiswa.java:338");
				}

				try {
					if (session.isOpen()) {
						session.close();
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOMahasiswa.java:345");
				}
			}
		}

		return pengeluaranMahasiswas;
	}

	// ----------------- PENGELUARAN

	public static String dataJSON = new JSONObject().toString();

	public String ambilLokasiKegiatan() {
		// Null-safe: entitas transient (mis. calon mahasiswa baru yang belum tersimpan)
		// belum punya id → tak ada file lokasi kegiatan keyed-by-id. Tanpa guard ini
		// getId().toString() melempar NPE (mis. render grid CetakRegistrasi).
		if (getId() == null) {
			return VOMahasiswa.dataJSON;
		}
		File file = Common.getFileLocation(this, "kegiatan_" + getId().toString());
		try {
			// System.out.println(this + ", Baca file " + file);
			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOMahasiswa.java:369");
		}
		return VOMahasiswa.dataJSON;
	}

	public void tulisLokasiKegiatan(String data) {
		// Null-safe: tanpa id tak ada file keyed-by-id untuk ditulis. reInitKegiatan
		// memanggil ini SEBELUM cek getId()!=null, jadi guard di sini mencegah NPE.
		if (getId() == null) {
			return;
		}
		File file = Common.getFileLocation(this, "kegiatan_" + getId().toString());
		try {
			// System.out.println(this + ", Tulis file " + file);
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOMahasiswa.java:384");
		}
	}

	public void populateKegiatan(Long kegiatanid) {
		try {
			JSONObject c = new JSONObject(ambilLokasiKegiatan());
			c.put(kegiatanid.toString(), kegiatanid.toString());
			tulisLokasiKegiatan(c.toString());
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOMahasiswa.java:394");
		}
	}

	@SuppressWarnings("unchecked")
	public List<Long> reInitKegiatan(Session session) {
		tulisLokasiKegiatan(new JSONObject().toString());
		if (getId() != null) {
			System.out.println("========== reInitKegiatan");
			List<Long> kegiatans = session.createCriteria(Kegiatan.class)
					.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
					.setProjection(Projections.property("id"))
					.add((this instanceof Mahasiswa) ? Restrictions.eq("mahasiswa", this)
							: Restrictions.eq("calonMahasiswa", this))
					.addOrder(Order.asc("semster")).addOrder(Order.asc("jenisKegiatan")).addOrder(Order.asc("id"))
					.list();
			System.out.println("========== reInitKegiatan size -> " + kegiatans.size());
			for (Long kegiatanid : kegiatans) {
				populateKegiatan(kegiatanid);
			}
			return kegiatans;
		}
		return new ArrayList<Long>();
	}

	public void removeKegiatan(Serializable id) {
		try {
			JSONObject c = new JSONObject(ambilLokasiKegiatan());
			c.put(id.toString(), "");
			tulisLokasiKegiatan(c.toString());
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOMahasiswa.java:425");
		}
	}

	public List<CicilanPembayaran> ambilCicilan() {
		boolean refresh = false;
		return ambilCicilan(null, refresh);
	}

	public List<CicilanPembayaran> ambilCicilan(boolean refresh) {
		return ambilCicilan(null, refresh);
	}

	public List<CicilanPembayaran> ambilCicilan(JenisKegiatan jenisKegiatanData) {
		boolean refresh = false;
		return ambilCicilan(jenisKegiatanData, refresh);
	}

	public List<CicilanPembayaran> ambilCicilan(JenisKegiatan jenisKegiatanData, boolean refresh) {
		// Panggil Helper Statis dengan menyuntikkan 'state' dan 'method' dari class ini
		// (this)
		return KegiatanPersistenceHelper.ambilCicilan(this, // Parameter: Object student (Mahasiswa/CalonMahasiswa)
				this.ambilLokasiCicilan(), // Parameter: JSON String lokasi cicilan
				this.ambilKegiatansData(refresh, jenisKegiatanData), // Parameter: Cache kegiatan dari DB
				jenisKegiatanData, // Parameter: Filter Jenis Kegiatan
				refresh // Parameter: Flag refresh
		);
	}

	public String ambilLokasiCicilan() {
		// Null-safe: entitas transient (id null) tak punya file keyed-by-id → cegah NPE.
		if (getId() == null) {
			return VOMahasiswa.dataJSON;
		}
		File file = Common.getFileLocation(this, "cicilan_" + getId().toString());
		if (file != null && file.exists()) {
			try {
				// System.out.println(this + ", Baca file " + file);
				String data = ais.common.BacaTulisUtil.baca(file);
				ais.common.BacaTulisUtil.hapus(file);
				return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOMahasiswa.java:466");
			}
		}
		return VOMahasiswa.dataJSON;
	}

	public List<CicilanPembayaran> ambilCicilanPembayaran(Integer semester) {
		List<CicilanPembayaran> cicilanPembayaransTemp = ambilCicilan();
		List<CicilanPembayaran> cicilanPembayarans = new ArrayList<CicilanPembayaran>();
		for (CicilanPembayaran cicilanPembayaran : cicilanPembayaransTemp) {
			if ((cicilanPembayaran.getNilai() < -0.1 || cicilanPembayaran.getNilai() > 0.1)
					&& semester.equals(cicilanPembayaran.getKegiatan().getSemster())) {
				cicilanPembayarans.add(cicilanPembayaran);
			}
		}
		cicilanPembayaransTemp = null;
		return cicilanPembayarans;
	}

	public Collection<Kegiatan> ambilKegiatans() {
		return ambilKegiatans(false);
	}

	public Collection<Kegiatan> ambilKegiatans(boolean refresh) {
		JenisKegiatan jenisKegiatan = null;
		return ambilKegiatansData(refresh, jenisKegiatan);
	}

	@SuppressWarnings("unchecked")
	public Collection<Kegiatan> ambilKegiatansData(boolean refresh, JenisKegiatan jenisKegiatan) {
		List<Long> keydataUtama = new ArrayList<Long>();
		// 1. Pengecekan Refresh Cache
		if (refresh || !udah("kegiatan_pembayaran")) {
			Session session = null;
			try {
				session = HibernateUtil.currentNativeSession();
				keydataUtama = reInitKegiatan(session);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOMahasiswa.java:504");
			} finally {
				if (session != null) {
					try { session.disconnect(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/VOMahasiswa.java:507");}
					try { session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/VOMahasiswa.java:508");}
				}
				HibernateUtil.closeSession();
			}
		}

		else {

			try {

				JSONObject c = new JSONObject(ambilLokasiKegiatan());
				java.util.Iterator<String> keys = c.keys();

				while (keys.hasNext()) {
					String key = keys.next();
					try {
						String s = c.getString(key);
						if (s != null && !s.trim().isEmpty()) {
							keydataUtama.add(Long.parseLong(key.trim()));
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOMahasiswa.java:528");
					}
				}

				// Cegah query kosong
				if (keydataUtama.isEmpty()) {
					return new ArrayList<Kegiatan>();
				}

			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOMahasiswa.java:538");
			}
		}

		System.out.println("keydataUtama -> " + keydataUtama);

		List<Kegiatan> kegiatans = new ArrayList<Kegiatan>();
		List<Kegiatan> tempkegiatans = ambilDataBanyak(Kegiatan.class, keydataUtama);
		if (tempkegiatans == null) {
			return kegiatans;
		}

		// 3. Gunakan HashSet untuk Filter Duplikasi (Jauh lebih cepat dari ArrayList)
		java.util.Set<Long> sudah = new java.util.HashSet<Long>();

		// 4. Looping dengan metode Fail-Fast (Eliminasi data yang tidak sesuai)
		for (Kegiatan kegiatan : tempkegiatans) {

			// Abaikan jika data null
			if (kegiatan == null || kegiatan.getId() == null) {
				continue;
			}

			// Abaikan jika sudah ada di dalam list (mencegah duplikat)
			if (sudah.contains(kegiatan.getId())) {
				continue;
			}

			// Abaikan jika tidak aktif (aman dari null pointer)
			if (!Boolean.TRUE.equals(kegiatan.getAktif())) {
				continue;
			}

			// Abaikan jika filter Jenis Kegiatan tidak cocok
			if (jenisKegiatan != null) {
				if (kegiatan.getJenisKegiatan() == null
						|| !jenisKegiatan.getId().equals(kegiatan.getJenisKegiatan().getId())) {
					continue;
				}
			}

			// Abaikan jika semester null
			Integer smt = kegiatan.getSemster();
			if (smt == null) {
				continue;
			}

			// Abaikan jika menyalahi aturan batas minimal & maksimal dari master Jenis
			// Kegiatan
			if (kegiatan.getJenisKegiatan() != null) {
				Integer minSmtMaster = kegiatan.getJenisKegiatan().getMinSmt();
				Integer maxSmtMaster = kegiatan.getJenisKegiatan().getMaxSmt();

				if (minSmtMaster != null && smt < minSmtMaster) {
					continue;
				}
				if (maxSmtMaster != null && smt > maxSmtMaster) {
					continue;
				}
			}

			// Jika lolos semua filter di atas, tambahkan ke hasil akhir
			kegiatans.add(kegiatan);
			sudah.add(kegiatan.getId());
		}

		// 5. Sorting berdasarkan semester
		Collections.sort(kegiatans, Common.compareBySmt);

		return kegiatans;
	}

	public Kegiatan ambilKegiatans(JenisKegiatan jenisKegiatan) {
		return ambilKegiatans(null, jenisKegiatan);
	}

	public Kegiatan ambilKegiatans(Integer semester, JenisKegiatan jenisKegiatan) {
		boolean refresh = false;
		Kegiatan kegiatan = ambilKegiatans(semester, jenisKegiatan, refresh);
		return kegiatan;
	}

	public Kegiatan ambilKegiatansRefresh(Integer semester, JenisKegiatan jenisKegiatan) {
		boolean refresh = true;
		return ambilKegiatansRefresh(semester, jenisKegiatan, refresh);
	}

	public Kegiatan ambilKegiatansRefresh(Integer semester, JenisKegiatan jenisKegiatan, boolean refresh) {

		Kegiatan kegiatan = ambilKegiatans(semester, jenisKegiatan, refresh);
		if (semester == null && kegiatan != null && kegiatan.getAmount() < 0.01) {
			semester = 1;
			kegiatan = ambilKegiatans(semester, jenisKegiatan, refresh);
		}
		return kegiatan;
	}

	public Kegiatan ambilKegiatans(Integer semester, JenisKegiatan jenisKegiatan, boolean refresh) {

		if (jenisKegiatan != null && ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
				&& jenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId())) {
			semester = 0;
		}

		Kegiatan keg = null;

		Collection<Kegiatan> kegiatans = ambilKegiatans(refresh);
		for (Kegiatan kegiatan : kegiatans) {

			if (semester == null && kegiatan.getJenisKegiatan() != null && jenisKegiatan != null
					&& kegiatan.getJenisKegiatan().getId().equals(jenisKegiatan.getId())
					&& kegiatan.getAmount() > 0.1) {
				keg = kegiatan;
				break;
			}

			else if (kegiatan != null && semester != null && jenisKegiatan != null
					&& kegiatan.getJenisKegiatan() != null && kegiatan.getSemster() != null
					&& kegiatan.getJenisKegiatan().getId().equals(jenisKegiatan.getId())
					&& kegiatan.getSemster().equals(semester) && kegiatan.getAmount() > 0.1) {
				keg = kegiatan;
				break;
			}

		}

		if (keg == null) {
			for (Kegiatan kegiatan : kegiatans) {
				if (semester == null && kegiatan.getJenisKegiatan() != null && jenisKegiatan != null
						&& kegiatan.getJenisKegiatan().getId().equals(jenisKegiatan.getId())) {
					keg = kegiatan;
					break;
				}

				else if (kegiatan != null && semester != null && jenisKegiatan != null
						&& kegiatan.getJenisKegiatan() != null && kegiatan.getSemster() != null
						&& kegiatan.getJenisKegiatan().getId().equals(jenisKegiatan.getId())
						&& kegiatan.getSemster().equals(semester)) {
					keg = kegiatan;
					break;
				}
			}
		}

		// PENAMBAHAN LOGIKA BARU: Pengecekan Database menggunakan Kode Unik jika memori kosong
		if (keg == null) {
			String kodeUnikCari = null;
			if (this instanceof ais.database.model.Mahasiswa) {
				kodeUnikCari = Kegiatan.generateKodeUnik((ais.database.model.Mahasiswa) this, null, jenisKegiatan, semester, null, null);
			} else if (this instanceof ais.database.model.BiodataCalonMahasiswa) {
				kodeUnikCari = Kegiatan.generateKodeUnik(null, (ais.database.model.BiodataCalonMahasiswa) this, jenisKegiatan, semester, null, null);
			}

			if (kodeUnikCari != null) {
				org.hibernate.Session sessionCek = null;
				try {
					sessionCek = ais.database.hibernate.HibernateUtil.getSessionFactory().openSession();
					keg = (Kegiatan) sessionCek.createCriteria(Kegiatan.class)
							.add(org.hibernate.criterion.Restrictions.eq("kodeunik", kodeUnikCari))
							.setMaxResults(1).uniqueResult();
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOMahasiswa.java:699");
				} finally {
					if (sessionCek != null) {
						try { if (sessionCek.isOpen()) sessionCek.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOMahasiswa.java:702");}
						try { sessionCek.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOMahasiswa.java:703");}
						try { if (sessionCek.isOpen()) sessionCek.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOMahasiswa.java:704");}
					}
				}
			}
		}

		if (semester == null && keg != null && keg.getAmount() < 0.01) {
			semester = 1;
			keg = ambilKegiatans(semester, jenisKegiatan, refresh);
		}

		kegiatans = null;

		return keg;
	}

	public List<Kegiatan> ambilKegiatans(Integer semester, Set<JenisKegiatan> jenisKegiatans) {
		return ambilKegiatans(semester, jenisKegiatans, false);
	}

	public List<Kegiatan> ambilKegiatans(Integer semester, Set<JenisKegiatan> jenisKegiatans, boolean refresh) {
		List<Long> inds = new ArrayList<Long>();
		for (JenisKegiatan jenisKegiatan : jenisKegiatans) {
			inds.add(jenisKegiatan.getId());
		}
		Collection<Kegiatan> kegiatans = ambilKegiatans(refresh);
		List<Kegiatan> hasil = new ArrayList<Kegiatan>();
		for (Kegiatan kegiatan : kegiatans) {
			if (kegiatan != null && semester != null && !inds.isEmpty() && kegiatan.getJenisKegiatan() != null
					&& kegiatan.getSemster() != null && inds.contains(kegiatan.getJenisKegiatan().getId())
					&& kegiatan.getSemster().equals(semester)) {
				hasil.add(kegiatan);
			}
		}
		return hasil;
	}

	public List<Kegiatan> ambilKegiatans(Integer semester) {
		return ambilKegiatans(semester, false);
	}

	public List<Kegiatan> ambilKegiatans(Integer semester, boolean refresh) {
		Collection<Kegiatan> kegiatans = ambilKegiatans(refresh);
		List<Kegiatan> hasil = new ArrayList<Kegiatan>();
		for (Kegiatan kegiatan : kegiatans) {
			if (kegiatan != null && semester != null && kegiatan.getJenisKegiatan() != null
					&& kegiatan.getSemster() != null && kegiatan.getSemster().equals(semester)) {
				hasil.add(kegiatan);
			} else if (semester == null && kegiatan != null && kegiatan.getJenisKegiatan() != null) {
				hasil.add(kegiatan);
			}
		}
		return hasil;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Double> ambilKodeTagihan(Integer semester, boolean refresh) {
		Session session = HibernateUtil.currentSession();
		List<Kegiatan> kegiatans = this.ambilKegiatans(semester);
		Map<String, Double> tagihans = new HashMap<String, Double>();
		for (Kegiatan kegiatan : kegiatans) {

			if (!kegiatan.getLunas()) {
				List<CicilanPembayaran> cicilanPembayarans = this.ambilCicilan(kegiatan.getJenisKegiatan(), refresh);

				Map<String, Double> nilais = new HashMap<String, Double>();
				for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
					if (cicilanPembayaran != null && cicilanPembayaran.getId() != null
							&& cicilanPembayaran.getItemBiaya() != null) {

						String key = cicilanPembayaran.getItemBiaya().getId() + "_" + cicilanPembayaran.getBayarKe();

						if (nilais.keySet().contains(key)) {
							Double nilai = nilais.get(key) + cicilanPembayaran.getNilai();
							nilais.put(key, nilai);
						} else {
							nilais.put(key, cicilanPembayaran.getNilai());
						}
					}
				}

				if (this instanceof Mahasiswa) {
					Mahasiswa mahasiswa = (Mahasiswa) this;
					Collection<DetailKegiatan> detailKegiatans = kegiatan == null || kegiatan.getId() == null ? null
							: kegiatan.ambilDetailKegiatan(refresh);

					@SuppressWarnings("rawtypes")
					Collection mydetailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, semester,
							kegiatan.getJenisKegiatan(), true);
					int countPengaturanBulanan = PembayaranUtilHelper.countBulanan(session, mahasiswa,
							kegiatan.getJenisKegiatan(), semester, mydetailBiayas, true, true);
					if (countPengaturanBulanan > 0) {
						mydetailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, semester,
								kegiatan.getJenisKegiatan(), "-1", true, true);

					}

					for (Object o : mydetailBiayas) {
						if (o instanceof DetailBiaya) {
							DetailBiaya detailBiaya = (DetailBiaya) o;
							Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya, refresh);
							String key = detailBiaya.getItemBiaya().getId() + "_" + detailBiaya.getBayarKe();
							Double nilai = nilais.get(key);
							if (nilai == null) {
								nilai = 0.0;
							}
							if (jumlah == null) {
								jumlah = 0.0;
							}
							Double sisa = jumlah - nilai;

							if (sisa.intValue() != 0) {
								Double hasil = tagihans.get(detailBiaya.getItemBiaya().getKode());
								if (hasil == null) {
									hasil = 0.0;
								}
								hasil += sisa;
								tagihans.put(detailBiaya.getItemBiaya().getKode(), hasil);

//								System.out.println("mahasiswa " + mahasiswa + ", tagihan " + jumlah + ", dibayar "
//										+ nilai + ", sisa = " + sisa + ", detailBiaya -> " + detailBiaya);

							}

						} else if (o instanceof PengaturanPembayaranBulanan) {
							PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
							Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailKegiatans, mahasiswa, semester,
									pengaturanPembayaranBulanan);
							Number sumCicilan = VOMahasiswa.hitungTotalCicilan(kegiatan, pengaturanPembayaranBulanan,
									cicilanPembayarans);
							Double nilai = sumCicilan == null ? 0.0 : sumCicilan.doubleValue();
							if (jumlah == null) {
								jumlah = 0.0;
							}
							Double sisa = jumlah - nilai;
							if (sisa.intValue() != 0) {
								Double hasil = tagihans
										.get(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getKode());
								if (hasil == null) {
									hasil = 0.0;
								}
								hasil += sisa;
								tagihans.put(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getKode(),
										hasil);

//								System.out.println("mahasiswa " + mahasiswa + ", tagihan " + jumlah + ", dibayar "
//										+ nilai + ", sisa = " + sisa + ", pengaturanPembayaranBulanan -> "
//										+ pengaturanPembayaranBulanan);
							}
						}
					}
				} else if (this instanceof BiodataCalonMahasiswa) {
					BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) this;
					Collection<DetailKegiatan> detailKegiatans = kegiatan == null || kegiatan.getId() == null ? null
							: kegiatan.ambilDetailKegiatan(refresh);

					@SuppressWarnings("rawtypes")
					Collection mydetailBiayas = PembayaranUtilHelper.getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa,
							kegiatan.getJenisKegiatan(),
							biodataCalonMahasiswa.getProdiLulus() == null ? biodataCalonMahasiswa.getProdi1()
									: biodataCalonMahasiswa.getProdiLulus(),
							semester, true);
					int countPengaturanBulanan = PembayaranUtilHelper.countBulanan(session, biodataCalonMahasiswa,
							kegiatan.getJenisKegiatan(), semester, mydetailBiayas, true, true);
					if (countPengaturanBulanan > 0) {
						mydetailBiayas = PembayaranUtil.getInstance().getPengaturanPembayaranSemua(
								biodataCalonMahasiswa, session, semester, kegiatan.getJenisKegiatan(), mydetailBiayas,
								true, true);
					}

					for (Object o : mydetailBiayas) {
						if (o instanceof DetailBiaya) {
							DetailBiaya detailBiaya = (DetailBiaya) o;
							Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya, refresh);

							String key = detailBiaya.getItemBiaya().getId() + "_" + detailBiaya.getBayarKe();
							Double nilai = nilais.get(key);
							if (nilai == null) {
								nilai = 0.0;
							}
							if (jumlah == null) {
								jumlah = 0.0;
							}
							Double sisa = jumlah - nilai;

							if (sisa.intValue() != 0) {
								Double hasil = tagihans.get(detailBiaya.getItemBiaya().getKode());
								if (hasil == null) {
									hasil = 0.0;
								}
								hasil += sisa;
								tagihans.put(detailBiaya.getItemBiaya().getKode(), hasil);

//								System.out.println("biodataCalonMahasiswa " + biodataCalonMahasiswa + ", tagihan "
//										+ jumlah + ", dibayar " + nilai + ", sisa = " + sisa + ", detailBiaya -> "
//										+ detailBiaya);

							}

						} else if (o instanceof PengaturanPembayaranBulanan) {
							PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
							Double jumlah = Kegiatan.ambilJumlahTagihan(kegiatan, detailKegiatans, null, semester,
									pengaturanPembayaranBulanan);
							Number sumCicilan = VOMahasiswa.hitungTotalCicilan(kegiatan, pengaturanPembayaranBulanan,
									cicilanPembayarans);
							Double nilai = sumCicilan == null ? 0.0 : sumCicilan.doubleValue();
							if (jumlah == null) {
								jumlah = 0.0;
							}
							Double sisa = jumlah - nilai;
							if (sisa.intValue() != 0) {
								Double hasil = tagihans
										.get(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getKode());
								if (hasil == null) {
									hasil = 0.0;
								}
								hasil += sisa;
								tagihans.put(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getKode(),
										hasil);

//								System.out.println("biodataCalonMahasiswa " + biodataCalonMahasiswa + ", tagihan "
//										+ jumlah + ", dibayar " + nilai + ", sisa = " + sisa
//										+ ", pengaturanPembayaranBulanan -> " + pengaturanPembayaranBulanan);
							}
						}
					}
				}
			}
		}

//		System.out.println("tagihans -> " + tagihans);
		return tagihans;
	}

	public Collection<JenisKegiatan> ambilJenisKegiatans(Integer semester, String kodeItemBiaya) {
		Map<Long, JenisKegiatan> jenisKegiatans = new HashMap<Long, JenisKegiatan>();
		List<String> kodes = kodeItemBiaya == null ? null : new ArrayList<String>();
		for (String s : kodeItemBiaya.split(";")) {
			if (!s.trim().isEmpty()) {
				kodes.add(s.trim().toLowerCase());
			}
		}

		List<CicilanPembayaran> cicilanPembayaransTemp = ambilCicilan();

		for (CicilanPembayaran cicilanPembayaran : cicilanPembayaransTemp) {
			if ((cicilanPembayaran.getNilai() < -0.1 || cicilanPembayaran.getNilai() > 0.1)
					&& cicilanPembayaran.getKegiatan() != null
					&& (semester == null || cicilanPembayaran.getKegiatan().getSemster().equals(semester))
					&& (kodes == null || kodes.contains(cicilanPembayaran.getItemBiaya().getKode().toLowerCase()))) {

				jenisKegiatans.put(cicilanPembayaran.getKegiatan().getJenisKegiatan().getId(),
						cicilanPembayaran.getKegiatan().getJenisKegiatan());
			}
		}
		cicilanPembayaransTemp = null;
		return jenisKegiatans.values();
	}

	public List<CicilanPembayaran> ambilCicilanPembayaran(Kegiatan kegiatan) {
		List<CicilanPembayaran> cicilanPembayaransTemp = ambilCicilan();
		List<CicilanPembayaran> cicilanPembayarans = ambilCicilanPembayaran(kegiatan, cicilanPembayaransTemp);
		cicilanPembayaransTemp = null;
		return cicilanPembayarans;
	}

	public List<CicilanPembayaran> ambilCicilanPembayaran(Kegiatan kegiatan,
			List<CicilanPembayaran> cicilanPembayaransTemp) {

		List<CicilanPembayaran> cicilanPembayarans = new ArrayList<CicilanPembayaran>();
		java.util.Set<Long> existingIds = new java.util.HashSet<Long>(); // Tracker untuk ID yang sudah masuk

		for (CicilanPembayaran cicilanPembayaran : cicilanPembayaransTemp) {
			try {
				if ((cicilanPembayaran.getNilai() < -0.1 || cicilanPembayaran.getNilai() > 0.1)
						&& cicilanPembayaran.getKegiatan() != null && kegiatan != null && kegiatan.getId() != null
						&& kegiatan.getId().equals(cicilanPembayaran.getKegiatan().getId())) {

					Long idCicilan = cicilanPembayaran.getId();

					// Jika ID belum tercatat di dalam pelacak, masukkan ke list dan catat ID-nya
					if (idCicilan != null && !existingIds.contains(idCicilan)) {
						existingIds.add(idCicilan);
						cicilanPembayarans.add(cicilanPembayaran);
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOMahasiswa.java:986");
				// TODO: handle exception
			}
		}

		existingIds.clear(); // Bersihkan Set untuk optimalisasi Garbage Collector

		return cicilanPembayarans;
	}

	public List<CicilanPembayaran> ambilCicilanPembayaran(String kodeItemBiaya, Integer semester) {
		List<CicilanPembayaran> cicilanPembayarans = new ArrayList<CicilanPembayaran>();
		Map<String, Double> kodes = new HashMap<String, Double>();
		for (String s : kodeItemBiaya.split(";")) {
			try {
				if (!s.trim().isEmpty()) {
					String[] k = StringUtils.split(s.trim().toLowerCase(), ":");
					kodes.put(k.length > 0 ? k[0] : "", k.length > 1 ? Double.parseDouble(k[1].trim()) : 99.0);
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOMahasiswa.java:1006");
			}
		}

		// System.out.println("ambilCicilanPembayaran => " + kodes);

		if (kodes.isEmpty()) {
			return cicilanPembayarans;
		}

		List<CicilanPembayaran> cicilanPembayaransTemp = ambilCicilanPembayaran(semester);

		for (CicilanPembayaran cicilanPembayaran : cicilanPembayaransTemp) {
			if (kodes.containsKey(cicilanPembayaran.getItemBiaya().getKode().toLowerCase())
					&& (cicilanPembayaran.getNilai() < -0.1 || cicilanPembayaran.getNilai() > 0.1)) {

				Double totalCicilan = 0.0;
				for (CicilanPembayaran cicilanPembayarantemp : cicilanPembayaransTemp) {
					if (cicilanPembayaran.getItemBiaya().getKode().toLowerCase()
							.equalsIgnoreCase(cicilanPembayarantemp.getItemBiaya().getKode().toLowerCase())) {
						totalCicilan += cicilanPembayarantemp.getNilai();
					}
				}

				Double nilai = kodes.get(cicilanPembayaran.getItemBiaya().getKode().toLowerCase());
				// System.out.println("ambilCicilanPembayaran => nilai->" +
				// nilai + ", totalCicilan->" + totalCicilan);
				if (totalCicilan >= nilai) {
					cicilanPembayarans.add(cicilanPembayaran);
				}
			}
		}
		cicilanPembayaransTemp = null;
		return cicilanPembayarans;
	}

	public Double hitungTotalCicilanPembayaran(Integer semester, Integer tahap, List<String> kodes) {
		List<CicilanPembayaran> cicilanPembayaransTemp = ambilCicilan();
		Double total = 0.0;
		for (CicilanPembayaran cicilanPembayaran : cicilanPembayaransTemp) {

			try {
				if ((cicilanPembayaran.getNilai() < -0.1 || cicilanPembayaran.getNilai() > 0.1)
						&& (kodes.contains(cicilanPembayaran.getItemBiaya().getKode()))) {

					System.out.println(
							"nilai => " + cicilanPembayaran.getNilai() + ", item " + cicilanPembayaran.getItemBiaya());

					if (ConstantValues.aktifkanTahapanTerhubungKeKeuangan && tahap != null
							&& cicilanPembayaran.getTahap() != null && tahap > 0
							&& tahap.equals(cicilanPembayaran.getTahap())) {
						total += cicilanPembayaran.getNilai();
					} else if (semester.equals(cicilanPembayaran.getKegiatan().getSemster())) {
						total += cicilanPembayaran.getNilai();
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOMahasiswa.java:1063");
			}

		}
		cicilanPembayaransTemp = null;
		return total;
	}

	public Double hitungTotalCicilanPembayaran(Integer semester, Boolean sekaliBayar, Integer tahap, String kode) {
		List<CicilanPembayaran> cicilanPembayaransTemp = ambilCicilan();
		return hitungTotalCicilanPembayaran(semester, sekaliBayar, tahap, kode, cicilanPembayaransTemp);
	}

	public Double hitungTotalCicilanPembayaran(Integer semester, Boolean sekaliBayar, Integer tahap, String kode,
			List<CicilanPembayaran> cicilanPembayaransTemp) {

		Double total = 0.0;
		for (CicilanPembayaran cicilanPembayaran : cicilanPembayaransTemp) {

			try {
				if ((cicilanPembayaran.getNilai() < -0.1 || cicilanPembayaran.getNilai() > 0.1)
						&& (kode.equalsIgnoreCase(cicilanPembayaran.getItemBiaya().getKode()))) {

					if (sekaliBayar) {
						total += cicilanPembayaran.getNilai();
					} else if (ConstantValues.aktifkanTahapanTerhubungKeKeuangan && tahap != null
							&& cicilanPembayaran.getTahap() != null && tahap > 0
							&& tahap.equals(cicilanPembayaran.getTahap())) {
						total += cicilanPembayaran.getNilai();
					} else if (semester.equals(cicilanPembayaran.getKegiatan().getSemster())) {
						total += cicilanPembayaran.getNilai();
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOMahasiswa.java:1097");
			}
		}
		cicilanPembayaransTemp = null;
		return total;
	}

	public Double hitungTotalCicilanPembayaranPengecekanKrs(Integer semester, Integer tahap) {
		List<CicilanPembayaran> cicilanPembayaransTemp = ambilCicilan();
		Double total = 0.0;
		for (CicilanPembayaran cicilanPembayaran : cicilanPembayaransTemp) {
			try {
				if ((cicilanPembayaran.getNilai() < -0.1 || cicilanPembayaran.getNilai() > 0.1)
						&& (cicilanPembayaran.getKegiatan().getJenisKegiatan().getDigunakanUntukPengecekanKrs())) {
					if (ConstantValues.aktifkanTahapanTerhubungKeKeuangan && tahap != null
							&& cicilanPembayaran.getTahap() != null && tahap > 0
							&& tahap.equals(cicilanPembayaran.getTahap())) {
						total += cicilanPembayaran.getNilai();
					} else if (semester.equals(cicilanPembayaran.getKegiatan().getSemster())) {
						total += cicilanPembayaran.getNilai();
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOMahasiswa.java:1119");
				// TODO: handle exception
			}
		}
		cicilanPembayaransTemp = null;
		return total;
	}

	public Double hitungTotalCicilanPembayaran(Integer semester, Integer tahap, JenisKegiatan jenisKegiatan) {
		List<CicilanPembayaran> cicilanPembayaransTemp = ambilCicilan();
		Double total = 0.0;
		for (CicilanPembayaran cicilanPembayaran : cicilanPembayaransTemp) {
			try {
				if ((cicilanPembayaran.getNilai() < -0.1 || cicilanPembayaran.getNilai() > 0.1)
						&& (cicilanPembayaran.getKegiatan().getJenisKegiatan().getId().equals(jenisKegiatan.getId()))) {
					if (ConstantValues.aktifkanTahapanTerhubungKeKeuangan && tahap != null
							&& cicilanPembayaran.getTahap() != null && tahap > 0
							&& tahap.equals(cicilanPembayaran.getTahap())) {
						total += cicilanPembayaran.getNilai();
					} else if (semester.equals(cicilanPembayaran.getKegiatan().getSemster())) {
						total += cicilanPembayaran.getNilai();
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOMahasiswa.java:1142");
				// TODO: handle exception
			}
		}
		cicilanPembayaransTemp = null;
		return total;
	}

	public int ambilJumlahCicilanPembayaran(String kodeItemBiaya, Integer semester) {
		return ambilCicilanPembayaran(kodeItemBiaya, semester).size();
	}

	/**
	 * Apakah denda cicilan ini DIBATALKAN lewat tombol "Batalkan Denda"? Sumber kebenaran =
	 * {@code Kegiatan.pembatalanDenda} (CSV id PengaturanPembayaranBulanan), sama persis dengan
	 * {@code DetailBiaya.checkDendaCicilan}. Bersifat REVERSIBLE: bila pembatalan di-uncheck, id
	 * hilang dari CSV sehingga denda dihitung kembali. Nilai denda tersimpan TIDAK dihapus
	 * (cicilan.getDenda tetap), hanya dikecualikan dari penjumlahan saat statusnya dibatalkan.
	 */
	private boolean dendaCicilanDibatalkan(CicilanPembayaran cicilanPembayaran) {
		try {
			if (cicilanPembayaran == null || cicilanPembayaran.getPengaturanPembayaranBulanan() == null
					|| cicilanPembayaran.getKegiatan() == null) {
				return false;
			}
			return StringUtils.contains(cicilanPembayaran.getKegiatan().getPembatalanDenda(),
					"," + cicilanPembayaran.getPengaturanPembayaranBulanan().getId() + ",");
		} catch (Exception e) {
			return false;
		}
	}

	public Double hitungTotalCicilanPembayaran(Kegiatan kegiatan) {
		List<CicilanPembayaran> cicilanPembayaransTemp = ambilCicilan();
		Double total = 0.0;
		if (kegiatan == null || kegiatan.getId() == null || cicilanPembayaransTemp == null) return total;
		for (CicilanPembayaran cicilanPembayaran : cicilanPembayaransTemp) {
			if (cicilanPembayaran != null && cicilanPembayaran.getKegiatan() != null
					&& kegiatan.getId().equals(cicilanPembayaran.getKegiatan().getId())) {
					Double nilai = cicilanPembayaran.getNilai() == null ? 0.0 : cicilanPembayaran.getNilai();
					// Denda yang DIBATALKAN tidak boleh menambah total (nilai cicilan = nominal + denda).
					if (dendaCicilanDibatalkan(cicilanPembayaran)) {
						nilai -= (cicilanPembayaran.getDenda() == null ? 0.0 : cicilanPembayaran.getDenda());
					}
					total += nilai;
			}
		}
		cicilanPembayaransTemp = null;
		return total;
	}

	public Double hitungDendaCicilanPembayaran(Kegiatan kegiatan) {
		List<CicilanPembayaran> cicilanPembayaransTemp = ambilCicilan();
		Double total = 0.0;
		if (kegiatan == null || kegiatan.getId() == null || cicilanPembayaransTemp == null) return total;
		for (CicilanPembayaran cicilanPembayaran : cicilanPembayaransTemp) {
			if (cicilanPembayaran != null && cicilanPembayaran.getKegiatan() != null
					&& kegiatan.getId().equals(cicilanPembayaran.getKegiatan().getId())) {
					// Lewati denda yang sudah DIBATALKAN (reversible: ikut Kegiatan.pembatalanDenda).
					if (!dendaCicilanDibatalkan(cicilanPembayaran)) {
						total += cicilanPembayaran.getDenda() == null ? 0.0 : cicilanPembayaran.getDenda();
					}
			}
		}
		cicilanPembayaransTemp = null;
		return total;
	}

	public Double[] hitungTotalCicilanDanDendaPembayaran(Kegiatan kegiatan) {
		List<CicilanPembayaran> cicilanPembayaransTemp = ambilCicilan();
		Double total = 0.0;
		Double denda = 0.0;
		if (kegiatan == null || kegiatan.getId() == null || cicilanPembayaransTemp == null) {
			return new Double[] { total, denda };
		}
		for (CicilanPembayaran cicilanPembayaran : cicilanPembayaransTemp) {
			if (cicilanPembayaran != null && cicilanPembayaran.getKegiatan() != null
					&& kegiatan.getId().equals(cicilanPembayaran.getKegiatan().getId())) {
					Double nilai = cicilanPembayaran.getNilai() == null ? 0.0 : cicilanPembayaran.getNilai();
					Double dendaItem = cicilanPembayaran.getDenda() == null ? 0.0 : cicilanPembayaran.getDenda();
					// Denda DIBATALKAN → keluarkan dari nilai cicilan (= nominal + denda) dan dari total denda.
					if (dendaCicilanDibatalkan(cicilanPembayaran)) {
						nilai -= dendaItem;
						dendaItem = 0.0;
					}
					total += nilai;
					denda += dendaItem;
			}
		}
		cicilanPembayaransTemp = null;
		return new Double[] { total, denda };
	}

	public static Double hitungTotalCicilan(Kegiatan kegiatan, PengaturanPembayaranBulanan pengaturanPembayaranBulanan,
			Collection<CicilanPembayaran> cicilanPembayaransTemp) {
		if (kegiatan == null || kegiatan.getId() == null || pengaturanPembayaranBulanan == null
				|| pengaturanPembayaranBulanan.getDetailBiaya() == null
				|| pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() == null
				|| cicilanPembayaransTemp == null) {
			return 0.0;
		}
		Double total = 0.0;

		boolean semuaTidakAdaBulanan = true;
		boolean ada = false;
		for (CicilanPembayaran cicilanPembayaran : cicilanPembayaransTemp) {
			try {
				if (cicilanPembayaran != null && cicilanPembayaran.getItemBiaya() != null
						&& cicilanPembayaran.getKegiatan() != null
						&& cicilanPembayaran.getItemBiaya().getId()
								.equals(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getId())
						&& pengaturanPembayaranBulanan.getDetailBiaya().getBayarKe()
								.equals(cicilanPembayaran.getBayarKe())
						&& cicilanPembayaran.getKegiatan().getId().equals(kegiatan.getId())) {
					semuaTidakAdaBulanan &= cicilanPembayaran.getPengaturanPembayaranBulanan() == null;
					ada = true;
					if (pengaturanPembayaranBulanan != null
							&& cicilanPembayaran.getPengaturanPembayaranBulanan() != null) {
						PengaturanPembayaranBulanan p = cicilanPembayaran.getPengaturanPembayaranBulanan();
						if (p.getDetailBiaya().getItemBiaya().getId()
								.equals(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getId())
								&& p.getRealBulan().equals(pengaturanPembayaranBulanan.getRealBulan())) {
							total += cicilanPembayaran.getNilai();
						}
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOMahasiswa.java:1260");
				// TODO: handle exception
			}
		}

//		System.out.println("semuaTidakAdaBulanan -> " + semuaTidakAdaBulanan + " " + pengaturanPembayaranBulanan);

		if (semuaTidakAdaBulanan && ada) {
			for (CicilanPembayaran cicilanPembayaran : cicilanPembayaransTemp) {
				try {
					if (cicilanPembayaran != null && cicilanPembayaran.getKegiatan() != null
							&& cicilanPembayaran.getItemBiaya() != null

							&& pengaturanPembayaranBulanan.getDetailBiaya().getBayarKe()
									.equals(cicilanPembayaran.getBayarKe())

							&& cicilanPembayaran.getKegiatan().getId().equals(kegiatan.getId())) {

						if (cicilanPembayaran.getItemBiaya().getId()
								.equals(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getId())) {
							total += cicilanPembayaran.getNilai();

							if (cicilanPembayaran.getPengaturanPembayaranBulanan() == null) {
								cicilanPembayaran.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
								try {
									Session session = HibernateUtil.currentNativeSession();
									session.getTransaction().begin();
									Common.refreshUpdate(session, cicilanPembayaran);
									session.getTransaction().commit();
									// session.disconnect();
									if (session.isOpen()) {
										session.disconnect();
										session.close();
									}
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOMahasiswa.java:1294");
								}
								HibernateUtil.closeSession();
							}
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOMahasiswa.java:1300");
					// TODO: handle exception
				}
			}
		}

		return total;
	}

	public static Double hitungTotalCicilan(Kegiatan kegiatan, DetailBiaya detailBiaya,
			List<CicilanPembayaran> cicilanPembayaransTemp) {
		if (kegiatan == null || kegiatan.getId() == null || detailBiaya == null
				|| detailBiaya.getItemBiaya() == null || cicilanPembayaransTemp == null) {
			return 0.0;
		}

		Double total = 0.0;
		for (CicilanPembayaran cicilanPembayaran : cicilanPembayaransTemp) {
			if (cicilanPembayaran != null && cicilanPembayaran.getItemBiaya() != null
					&& cicilanPembayaran.getKegiatan() != null
					&& detailBiaya.getBayarKe().equals(cicilanPembayaran.getBayarKe())
					&& cicilanPembayaran.getKegiatan().getId().equals(kegiatan.getId())
					&& detailBiaya.getItemBiaya().getId().equals(cicilanPembayaran.getItemBiaya().getId())) {
				total += cicilanPembayaran.getNilai();
			}
		}
		cicilanPembayaransTemp = null;
		return total;
	}

}
