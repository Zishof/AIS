package ais.database.model;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.action.master.helper.KrsDetailHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.kursus.KomponenDataProdukKursus;
import ais.database.model.pkl.KelompokPkl;
import ais.database.model.recruitment.JadwalUjianPegawai;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.JadwalPertemuanPSB;
import ais.database.model.sekolah.JadwalUjianPSB;
import ais.database.model.sekolah.KelasLesSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sop.DisposisiSop;

/**
 * Value object/proyeksi data untuk vo pembelajaran. Tipe ini merangkum gabungan nilai yang
 * dibutuhkan UI atau laporan tanpa memperkenalkan entity persistence atau aturan transaksi baru.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * VoKunci}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code DisposisiSop disposisiSop};
 * inisialisasi/lifecycle ({@code reInitPertemuan()}, {@code reInitPertemuan()}, {@code reInitPertemuan()},
 * {@code reInitTugas()}, {@code reInitTugas()}, {@code reInitUjian()}); pembacaan/pencarian ({@code
 * getCourse()}, {@code getUrutkanotomatis()}, {@code getDisposisiSop()}, {@code ambilLokasiPertemuan()}, {@code
 * ambilJumlahPertemuan()}, {@code ambilJumlahPertemuan()}); mutasi data ({@code setCourse()}, {@code
 * setUrutkanotomatis()}, {@code setDisposisiSop()}); penghapusan/pembatalan ({@code removePertemuan()}); operasi
 * domain lain ({@code tulisLokasiPertemuan()}, {@code populatePertemuan()}, {@code masukkanPertemuanLocal()},
 * {@code adalahLockTimeoutPertemuan()}, {@code infoDosen()}, {@code infoSimple()}). Bagian lain dari kontrak
 * tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> selain accessor state, operasi domain yang disebut di atas dapat membaca/mengubah
 * persistence, memicu lifecycle, atau membentuk komponen UI. Jangan menganggap model ini selalu murni;
 * panggil operasi tersebut melalui alur service dengan session, transaksi, dan otorisasi yang sesuai agar
 * perilakunya tidak disalin ke tempat lain.</p>
 *
 *
 * <h3>Posisi dalam hierarki dan daftar subclass</h3>
 * <p>Rantainya {@code GeneralValueObject → ais.database.model.sop.DataSop → VoKunci →
 * VOPembelajaran}, sejajar dengan {@link VOMahasiswa} yang juga turunan langsung
 * {@link VoKunci}. Keduanya tidak saling mewarisi: {@code VOMahasiswa} memodelkan <i>orang</i>
 * yang ditagih, {@code VOPembelajaran} memodelkan <i>wadah kegiatan belajar</i> yang punya
 * pertemuan.</p>
 * <p>Tujuh belas subclass konkret tersebar di lima modul:</p>
 * <ul>
 * <li>perkuliahan: {@link Perkuliahan}, {@link KrsMahasiswa}, {@link GrupPertemuan},
 * {@link PertemuanPunyaGrupPertemuan};</li>
 * <li>tugas akhir: {@link Skripsi}, {@link MahasiswaRequestTugasAkhir}, {@link Wisuda};</li>
 * <li>pengabdian/praktik: {@code kkn.KelompokKkn}, {@code pkl.KelompokPkl},
 * {@link FormulirKegiatan};</li>
 * <li>penerimaan &amp; ujian: {@link JadwalUjianPMB}, {@code sekolah.JadwalUjianPSB},
 * {@code sekolah.JadwalPertemuanPSB}, {@code recruitment.JadwalUjianPegawai};</li>
 * <li>sekolah &amp; kursus: {@code sekolah.JadwalPelajaran}, {@code sekolah.KelasLesSiswa},
 * {@code kursus.KomponenDataProdukKursus}.</li>
 * </ul>
 *
 * <h3>Bentuk arsitekturnya: pemilahan berdasarkan {@code instanceof}</h3>
 * <p>Kelas ini hampir tidak punya state sendiri — hanya satu relasi
 * {@link #getDisposisiSop()} dan empat kontrak abstrak. Yang membuatnya besar adalah puluhan
 * method yang <b>memilah perilaku dengan rantai {@code instanceof} atas dirinya sendiri</b>,
 * lalu meng-{@code cast} {@code this} ke subclass yang bersangkutan. Pola ini adalah kebalikan
 * dari pengiriman pesan polimorfik: alih-alih tiap subclass mendefinisikan perilakunya,
 * kelas induk memuat seluruh pengetahuan tentang seluruh subclass.</p>
 * <p>Konsekuensi praktisnya, yang berlaku di hampir seluruh method di bawah:</p>
 * <ul>
 * <li><b>Menambah subclass baru tidak memaksa apa pun.</b> Subclass yang belum terdaftar pada
 * suatu rantai akan jatuh ke nilai bawaan — {@code "-"}, {@code false}, {@code null}, atau daftar
 * kosong — tanpa kompilator maupun runtime memberi peringatan. Ketika menambah subclass, telusuri
 * <b>setiap</b> method di kelas ini dan putuskan cabangnya secara sadar.</li>
 * <li><b>Cakupan tiap rantai berbeda-beda.</b> Tidak ada satu daftar subclass yang dipakai
 * seragam. {@link #ambilJenis()} mengenal tujuh, {@link #populateDosenBuNama()} sembilan,
 * {@link #reInitPertemuan(Session)} empat belas, {@link #ambilHari()} hanya satu. Perbedaan itu
 * tidak selalu disengaja; beberapa di antaranya dicatat pada dokumentasi method masing-masing.</li>
 * <li><b>Ketiga mesin sinkronisasi memakai rantai restriksi yang identik</b> —
 * {@link #reInitPertemuan(Session)}, {@link #reInitTugas(Session)}, dan
 * {@link #reInitUjian(Session)} menyalin empat belas cabang yang sama persis, ditutup
 * {@code Restrictions.sqlRestriction("false")} sebagai perilaku gagal-tertutup. Perubahan pada
 * salah satunya harus diterapkan pada ketiganya.</li>
 * </ul>
 *
 * <h3>Cache pertemuan berbasis berkas</h3>
 * <p>Seperti {@link VOMahasiswa}, kelas ini menyimpan <i>daftar id</i> pertemuan miliknya pada
 * berkas JSON per objek, bukan datanya. {@link #ambilLokasiPertemuan()} membaca,
 * {@link #tulisLokasiPertemuan(String)} menimpa, {@link #populatePertemuan(Pertemuan)} menambah
 * satu entri, dan {@link #removePertemuan(Serializable)} menandai entri sebagai terhapus dengan
 * menyetel nilainya menjadi string kosong. Penanda "sudah pernah dibangun" memakai
 * {@link GeneralValueObject#udah()} tanpa sufiks. Berbeda dari padanannya di
 * {@link VOMahasiswa}, pasangan baca/tulis di sini <b>tidak</b> memeriksa apakah id sudah ada,
 * sehingga tidak aman dipanggil pada objek yang belum tersimpan.</p>
 *
 * <h3>Yang perlu diwaspadai</h3>
 * <ul>
 * <li>{@link #getDisposisiSop()} menulis ke fieldnya sendiri saat dibaca, dan
 * {@link #setDisposisiSop(DisposisiSop)} tidak dapat mengosongkan relasi — searah saja.</li>
 * <li>{@link #reInitPertemuan(Session)}, {@link #reInitTugas(Session)}, dan
 * {@link #reInitUjian(Session)} membuka transaksi, memperbarui nomor pertemuan, dan menulis ke
 * basis data; ketiganya bukan operasi baca.</li>
 * <li>{@link #getAttendee(Pertemuan)} dan beberapa method lain memanggil
 * {@code HibernateUtil.closeSession()} yang menutup session milik thread pemanggil.</li>
 * <li>Tidak ada penyaringan tenant/kepemilikan di kelas ini. Seluruh kueri dibatasi pada objek
 * {@code this}; otorisasi harus sudah diselesaikan sebelum sampai ke sini. Perhatikan khususnya
 * {@link #getOrganizer(Pertemuan)} dan {@link #getAttendee(Pertemuan)} yang mengumpulkan alamat
 * surel peserta dan pengajar.</li>
 * </ul>
 *
 * @see VOMahasiswa
 * @see VoKunci
 */
public abstract class VOPembelajaran extends VoKunci {
	/**
	 * Versi serialisasi kelas ini, diwarisi seluruh subclass yang tidak mendeklarasikan versinya
	 * sendiri.
	 *
	 * <p>Dipatok eksplisit agar objek yang tersimpan di session HTTP tetap terbaca setelah
	 * penambahan method pada kelas ini. Perlu dicatat bahwa cache pertemuan tidak ikut
	 * diserialisasi — ia hidup di berkas indeks dan cache proses — sehingga objek hasil
	 * deserialisasi harus memanggil {@link #ambilPertemuan(boolean)} dengan {@code refresh} untuk
	 * mendapatkan daftar yang mutakhir.</p>
	 */
	private static final long serialVersionUID = -4193008320801300777L;

	/**
	 * Mengembalikan pengenal "course" objek pembelajaran ini pada sistem e-learning luar.
	 *
	 * <p>Kontrak ini sengaja abstrak karena tiap subclass menyimpannya di kolom yang berbeda —
	 * ada yang memetakannya ke kolom {@code course} sungguhan, ada yang menurunkannya dari relasi
	 * lain. Nilainya dipakai jalur integrasi e-learning untuk menautkan wadah kegiatan di AIS
	 * dengan ruang kelas di sistem luar.</p>
	 *
	 * <p>Boleh {@code null} atau kosong pada objek yang belum pernah ditautkan; pemanggil harus
	 * memeriksanya sendiri.</p>
	 *
	 * @return pengenal course pada sistem luar; bisa {@code null}
	 */
	public abstract String getCourse();

	/**
	 * Menyetel pengenal "course" objek pembelajaran ini pada sistem e-learning luar.
	 *
	 * <p>Implementasinya menjadi tanggung jawab subclass; kelas ini tidak memvalidasi bentuk
	 * maupun keunikan nilai yang diberikan.</p>
	 *
	 * @param course pengenal course baru; boleh {@code null} untuk memutus tautan
	 */
	public abstract void setCourse(String course);

	/**
	 * Menyatakan apakah nomor pertemuan disusun ulang secara otomatis menurut tanggal.
	 *
	 * <p><b>Penanda paling berpengaruh di kelas ini.</b> Ia menentukan tiga hal sekaligus:</p>
	 * <ol>
	 * <li><b>Kunci pengurutan cache.</b> Pada {@link #masukkanPertemuanLocal}, bila bernilai
	 * benar kunci disusun dari tanggal ({@code yyyyMMdd_id}); bila salah, dari nomor pertemuan
	 * yang dipadkan menjadi empat digit ({@code 0007_id}).</li>
	 * <li><b>Apakah nomor pertemuan ditimpa.</b> Ketiga mesin {@code reInit...} hanya menulis
	 * ulang {@code pertemuanKe} ketika penanda ini benar. Bila salah, nomor yang diisi manual
	 * oleh pengguna dipertahankan.</li>
	 * <li><b>Urutan kueri.</b> Rantai {@code reInit...} mengurutkan berdasarkan
	 * {@code pertemuanKe} bila penanda salah, dan berdasarkan {@code tanggal} bila tidak.</li>
	 * </ol>
	 *
	 * <p><b>Nilai {@code null} bukan sama dengan {@code false}.</b> Pemeriksaannya tidak seragam:
	 * ketiga mesin {@code reInit...} memakai bentuk {@code (nilai != null && !nilai)} yang aman
	 * terhadap {@code null}, tetapi {@link #masukkanPertemuanLocal} memakai {@code !nilai}
	 * telanjang sehingga {@code null} memicu {@link NullPointerException} — yang ditelan diam-diam
	 * dan menyebabkan pertemuan tersebut <b>hilang dari daftar</b>. Subclass wajib memastikan
	 * getter ini tidak pernah mengembalikan {@code null}.</p>
	 *
	 * @return {@code true} bila nomor pertemuan disusun otomatis menurut tanggal; sebaiknya tidak
	 *         pernah {@code null}
	 */
	public abstract Boolean getUrutkanotomatis();

	/**
	 * Menyetel penanda penyusunan ulang nomor pertemuan otomatis.
	 *
	 * <p>Mengubah nilai ini mengubah kunci pengurutan cache pertemuan, sehingga indeks berkas yang
	 * ada menjadi tidak konsisten dengan penanda barunya. Setelah menyetelnya, bangun ulang indeks
	 * lewat {@link #reInitPertemuan(Session)} atau panggil
	 * {@link #ambilPertemuan(boolean)} dengan {@code refresh} bernilai benar.</p>
	 *
	 * @param urutkanotomatis {@code true} agar nomor pertemuan disusun otomatis menurut tanggal;
	 *                        hindari menyetel {@code null}, lihat {@link #getUrutkanotomatis()}
	 */
	public abstract void setUrutkanotomatis(Boolean urutkanotomatis);

	/**
	 * Tautan opsional ke disposisi SOP yang menjadi dasar dibentuknya objek pembelajaran ini.
	 *
	 * <p>Satu-satunya state yang benar-benar dimiliki kelas ini; seluruh perilaku lainnya
	 * diturunkan dari subclass lewat pemilahan {@code instanceof}. Kolomnya
	 * ({@code disposisi_sop}) boleh kosong, karena mayoritas objek pembelajaran dibuat langsung
	 * tanpa melalui alur SOP.</p>
	 */
	private DisposisiSop disposisiSop;

	/**
	 * Mengembalikan disposisi SOP yang menjadi dasar objek pembelajaran ini, bila ada.
	 *
	 * <p><b>Getter ini menulis ke fieldnya sendiri.</b> Hasil {@code check(...)} — helper warisan
	 * yang memvalidasi dan menormalkan referensi entity, mengembalikan {@code null} untuk referensi
	 * yang tidak lagi sah — ditugaskan kembali ke field sebelum dikembalikan. Membaca properti ini
	 * karenanya dapat mengubah state objek dari sudut pandang Hibernate: entity yang sedang
	 * terkelola bisa menjadi "kotor" hanya karena dibaca, memicu pembaruan baris beserta revisi
	 * audit pada {@code flush} berikutnya walaupun pengguna tidak mengubah apa pun. Ini instansi
	 * dari pola getter-yang-mengubah-field yang tersebar luas di lapisan model AIS, bukan
	 * kekhususan kelas ini.</p>
	 *
	 * <p>Relasi dimuat secara lazy, jadi mengaksesnya di luar session yang masih terbuka akan
	 * gagal pada pemanggil. Cascade {@code PERSIST} dan {@code MERGE} berarti menyimpan objek
	 * pembelajaran ikut menyimpan disposisi yang tertaut, tetapi tidak menghapusnya.</p>
	 *
	 * @return disposisi SOP yang tertaut, atau {@code null} bila tidak ada maupun bila
	 *         referensinya dinilai tidak sah oleh {@code check(...)}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menautkan objek pembelajaran ini ke sebuah disposisi SOP.
	 *
	 * <p><b>Setter searah: relasi ini tidak dapat dikosongkan lewat method ini.</b> Argumen
	 * {@code null} maupun disposisi yang belum punya id ditolak dengan {@code return} lebih awal,
	 * sehingga nilai lama tetap bertahan. Perilaku itu melindungi tautan yang sudah ada dari
	 * penimpaan tidak sengaja oleh formulir CRUD generik yang mengirim seluruh properti termasuk
	 * yang tidak diisi pengguna — pola yang sama dipakai di beberapa entity AIS lain untuk relasi
	 * yang bersifat jejak. Konsekuensinya, memutus tautan harus dilakukan lewat pembaruan langsung
	 * di lapisan persistence, bukan lewat setter ini.</p>
	 *
	 * <p>Ekspresi ternari pada badan method tidak berpengaruh: setelah penjaga di atas lolos,
	 * kondisi {@code disposisiSop == null || disposisiSop.getId() == null} pasti bernilai salah,
	 * sehingga cabang yang terpilih selalu argumen yang baru. Ia merupakan sisa dari bentuk
	 * sebelum penjaga ditambahkan dan dapat disederhanakan tanpa mengubah perilaku.</p>
	 *
	 * @param disposisiSop disposisi yang ditautkan; {@code null} atau tanpa id diabaikan tanpa
	 *                     pesan kesalahan
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {
		if (disposisiSop == null || disposisiSop.getId() == null) {
			return;
		}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
	}

	/**
	 * Membaca berkas indeks pertemuan milik objek pembelajaran ini dan mengembalikan isinya sebagai
	 * teks JSON.
	 *
	 * <p>Berkas berkunci {@code "pertemuan_" + getId()} di bawah lokasi berkas milik entity ini.
	 * Isinya {@link org.json.JSONObject} yang memetakan id {@link Pertemuan} ke dirinya sendiri —
	 * himpunan id yang disimpan sebagai peta, bukan data pertemuannya. Nilai berupa string kosong
	 * menandai entri yang "dihapus" oleh {@link #removePertemuan(Serializable)}.</p>
	 *
	 * <p><b>Tidak null-safe terhadap id.</b> Berbeda dari padanannya di {@link VOMahasiswa} yang
	 * memasang penjaga {@code getId() == null}, method ini langsung memanggil
	 * {@code getId().toString()}. Memanggilnya pada objek pembelajaran yang belum tersimpan
	 * melempar {@link NullPointerException} — dan karena pemanggilan itu berada <i>di luar</i> blok
	 * {@code try}, kesalahannya tidak ditelan melainkan diteruskan ke pemanggil. Jangan memanggil
	 * method ini (atau apa pun yang bergantung padanya, termasuk
	 * {@link #ambilPertemuan(boolean)}) sebelum objek disimpan.</p>
	 *
	 * <p>Berkas yang belum ada atau gagal dibaca sama-sama menghasilkan {@link VOMahasiswa#dataJSON}
	 * — sentinel JSON kosong milik {@link VOMahasiswa} yang dipinjam bersama oleh banyak entity.
	 * Kegagalan pembacaan dicatat ke audit lalu ditelan, sehingga "tidak ada pertemuan" dan "gagal
	 * membaca cache" tidak dapat dibedakan dari nilai baliknya.</p>
	 *
	 * @return teks JSON berisi himpunan id pertemuan; tidak pernah {@code null}
	 */
	public String ambilLokasiPertemuan() {
		File file = Common.getFileLocation(this, "pertemuan_" + getId().toString());
		try {

			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:83");
		}
		return VOMahasiswa.dataJSON;
	}

	/**
	 * Menimpa berkas indeks pertemuan milik objek ini dengan teks JSON yang diberikan.
	 *
	 * <p>Pasangan tulis dari {@link #ambilLokasiPertemuan()}, memakai kunci berkas yang sama. Isi
	 * lama dibuang seluruhnya; untuk menambah satu id ke indeks yang ada, pakai
	 * {@link #populatePertemuan(Pertemuan)}.</p>
	 *
	 * <p>Dipanggil ketiga mesin {@code reInit...} untuk mengosongkan indeks sebelum membangunnya
	 * kembali. Perhatikan bahwa pada {@link #reInitPertemuan(Session)} dan
	 * {@link #reInitTugas(Session)}, pengosongan dilakukan <i>setelah</i> kueri berhasil dan
	 * sebelum perulangan pengisian — sehingga bila proses gagal di tengah, indeks tertinggal dalam
	 * keadaan sebagian. Pemanggilan berikutnya dengan {@code refresh} memperbaikinya.</p>
	 *
	 * <p><b>Tidak null-safe terhadap id</b>, sama seperti {@link #ambilLokasiPertemuan()}:
	 * pemanggilan pada objek yang belum tersimpan melempar {@link NullPointerException} dari luar
	 * blok {@code try}. Kegagalan penulisan itu sendiri dicatat ke audit lalu ditelan, sehingga
	 * pemanggil tidak pernah tahu apakah berkas benar-benar tertulis.</p>
	 *
	 * @param data teks JSON pengganti; bentuknya tidak divalidasi dan JSON rusak baru akan
	 *             ketahuan saat dibaca kembali
	 */
	public void tulisLokasiPertemuan(String data) {
		File file = Common.getFileLocation(this, "pertemuan_" + getId().toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:92");
			// TODO Auto-generated catch block

		}
	}

	/**
	 * Menandai satu id {@link Pertemuan} sebagai terhapus pada berkas indeks milik objek ini.
	 *
	 * <p><b>Kuncinya tidak dibuang — nilainya disetel menjadi string kosong.</b> Pembaca indeks
	 * ({@link #ambilPertemuan(boolean)}) melewati entri bernilai kosong, sehingga efeknya sama
	 * dengan penghapusan dari sudut pandang tampilan. Konsekuensinya berkas indeks tumbuh secara
	 * monoton: id yang pernah dihapus tetap menempati ruang sampai salah satu mesin
	 * {@code reInit...} menulis ulang berkas dari nol. Konvensi yang sama dipakai
	 * {@link VOMahasiswa#removeKegiatan(Serializable)}.</p>
	 *
	 * <p><b>Tidak menyentuh basis data.</b> Baris {@link Pertemuan} tetap ada; yang berubah hanya
	 * cache. Pemanggilan {@link #ambilPertemuan(boolean)} dengan {@code refresh} akan memunculkan
	 * kembali pertemuan tersebut selama ia masih aktif. Untuk benar-benar menghilangkan sebuah
	 * pertemuan, setel penanda {@code aktif}-nya menjadi salah di basis data.</p>
	 *
	 * <p>Baca-ubah-tulis di sini tidak atomik; dua thread yang menghapus pertemuan berbeda pada
	 * objek yang sama dapat saling menimpa. Seluruh kegagalan — termasuk
	 * {@link NullPointerException} dari {@link #ambilLokasiPertemuan()} pada objek yang belum
	 * tersimpan — ditelan dan dicatat ke audit, sehingga method ini tidak pernah melempar.</p>
	 *
	 * @param id id pertemuan yang ditandai terhapus; dipakai lewat {@code toString()} sehingga
	 *           harus menghasilkan teks yang sama persis dengan kunci yang didaftarkan
	 *           {@link #populatePertemuan(Pertemuan)}
	 */
	public void removePertemuan(Serializable id) {
		try {
			JSONObject c = new JSONObject(ambilLokasiPertemuan());
			c.put(id.toString(), "");
			tulisLokasiPertemuan(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:103");

		}
	}

	/**
	 * Mendaftarkan satu {@link Pertemuan} ke dalam berkas indeks milik objek ini.
	 *
	 * <p>Baca indeks yang ada, tambahkan pasangan {@code id -> id}, tulis kembali. Mendaftarkan id
	 * yang sama dua kali hanya menimpa entri lama dengan nilai yang sama, sehingga pemanggilan
	 * berulang tidak menggandakan apa pun — sifat yang diandalkan ketiga mesin
	 * {@code reInit...} yang memanggil method ini di dalam perulangan.</p>
	 *
	 * <p>Argumen {@code null} ditolak dengan {@code return} lebih awal. Pertemuan yang belum punya
	 * id akan memicu {@link NullPointerException} yang ditelan oleh penangkap di method ini,
	 * sehingga tidak ada yang terdaftar dan tidak ada yang dilaporkan; kesalahannya tetap dicatat
	 * ke audit.</p>
	 *
	 * <p><b>Tidak memeriksa kepemilikan.</b> Pertemuan milik objek pembelajaran lain akan
	 * didaftarkan begitu saja ke indeks ini bila diberikan. Penyaringan kepemilikan sepenuhnya
	 * berada pada pemanggil — pada ketiga mesin {@code reInit...} hal itu dijamin oleh restriksi
	 * kueri, tetapi pemanggil di luar kelas ini harus memastikannya sendiri.</p>
	 *
	 * <p>Baca-ubah-tulis tidak atomik; pendaftaran bersamaan dari dua thread dapat menghilangkan
	 * salah satu id dari indeks. Data di basis data tetap utuh.</p>
	 *
	 * @param pertemuan pertemuan yang didaftarkan; {@code null} diabaikan tanpa efek
	 */
	public void populatePertemuan(Pertemuan pertemuan) {
		try {
			if (pertemuan == null) {
				return;
			}

			JSONObject c = new JSONObject(ambilLokasiPertemuan());
			c.put(pertemuan.getId().toString(), pertemuan.getId().toString());
			tulisLokasiPertemuan(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:117");
		}
	}

	/**
	 * Menghitung banyaknya pertemuan aktif milik objek pembelajaran ini, memakai cache.
	 *
	 * <p>Memanggil {@link #ambilPertemuan()} lalu mengambil ukuran peta hasilnya. Karena itu
	 * seluruh biaya pengambilan tetap dikeluarkan — termasuk kemungkinan membangun ulang indeks
	 * dari basis data pada pemanggilan pertama — walaupun yang dibutuhkan hanya satu angka. Bila
	 * daftar pertemuannya juga diperlukan, ambil sekali lewat {@link #ambilPertemuan()} dan baca
	 * ukurannya sendiri.</p>
	 *
	 * <p>Angka yang dikembalikan sudah memperhitungkan seluruh penyaringan
	 * {@link #ambilPertemuan(boolean)}: hanya pertemuan yang penanda {@code aktif}-nya bernilai
	 * benar yang dihitung, dan untuk {@link Perkuliahan} dengan penomoran manual, jumlahnya
	 * dipotong pada batas {@code jumlahMaksimalPertemuan}.</p>
	 *
	 * <p>Peta sementara di-{@code clear()} dan disetel {@code null} sebelum method kembali —
	 * kebiasaan yang tersebar di kelas ini untuk menekan jejak memori saat sinkronisasi massal
	 * ribuan objek pembelajaran secara paralel.</p>
	 *
	 * <p>Perhatikan bahwa method ini mengembalikan {@link Integer} sedangkan
	 * {@link #ambilJumlahPertemuan(boolean)} mengembalikan {@code int} primitif. Perbedaan itu
	 * tidak membawa makna tambahan — nilai baliknya tidak pernah {@code null} — tetapi berarti
	 * kedua overload tidak dapat saling menggantikan pada konteks yang peka tipe.</p>
	 *
	 * @return banyaknya pertemuan aktif; 0 bila tidak ada, tidak pernah {@code null}
	 */
	public Integer ambilJumlahPertemuan() {
		TreeMap<String, Long> pertemuans = ambilPertemuan();
		int size = pertemuans != null ? pertemuans.size() : 0;
		if (pertemuans != null) {
			pertemuans.clear();
			pertemuans = null;
		}
		return size;
	}

	/**
	 * Menghitung banyaknya pertemuan aktif milik objek ini, dengan pilihan membangun ulang indeks
	 * dari basis data lebih dulu.
	 *
	 * <p>Sama dengan {@link #ambilJumlahPertemuan()} kecuali bahwa {@code refresh} diteruskan ke
	 * {@link #ambilPertemuan(boolean)}. Memanggilnya dengan {@code true} berarti menjalankan
	 * seluruh mesin {@link #reInitPertemuan(Session)} — yang membuka transaksi, menulis ulang nomor
	 * pertemuan, dan menyinkronkan diskusi, ujian, tugas, izin, serta berkas media tiap pertemuan —
	 * hanya untuk memperoleh satu angka. Untuk kolom "jumlah pertemuan" pada grid, gunakan
	 * {@code false}.</p>
	 *
	 * @param refresh {@code true} untuk membangun ulang indeks dari basis data lebih dulu
	 * @return banyaknya pertemuan aktif; 0 bila tidak ada
	 */
	public int ambilJumlahPertemuan(boolean refresh) {
		TreeMap<String, Long> pertemuans = ambilPertemuan(refresh);
		int size = pertemuans != null ? pertemuans.size() : 0;
		if (pertemuans != null) {
			pertemuans.clear();
			pertemuans = null;
		}
		return size;
	}

	/**
	 * Menghimpun statistik pertemuan objek pembelajaran ini secara menyeluruh: jumlah pertemuan,
	 * berapa yang sudah berlalu, berapa yang sudah punya absensi, serta rekapitulasi status
	 * kehadiran.
	 *
	 * <p>Method ini menyiapkan datanya lebih dulu — membaca indeks lewat
	 * {@link #ambilPertemuan()}, mengubah tiap id menjadi objek {@link Pertemuan} dari cache
	 * proses, dan membuang yang tidak ditemukan — lalu menyerahkan perhitungannya ke
	 * {@link #ambilJumlahPertemuanStatistik(List, Mahasiswa, Dosen, boolean, boolean)} dengan
	 * mahasiswa dan dosen bernilai {@code null}, yang berarti "seluruh peserta, tanpa
	 * penyaringan".</p>
	 *
	 * <p><b>Hanya membaca cache proses.</b> Objek diambil dengan {@code ambilData} tanpa cadangan
	 * kueri basis data, sehingga id yang tercatat di indeks tetapi objeknya belum pernah dimuat
	 * akan dilewati diam-diam. Akibatnya angka statistik dapat lebih kecil daripada jumlah
	 * pertemuan yang sebenarnya bila cache proses baru saja dibersihkan. Bandingkan dengan
	 * {@link #ambilPertemuan(boolean)} yang menyediakan cadangan kueri untuk kasus yang sama.</p>
	 *
	 * <p><b>Bentuk nilai balik.</b> Larik sepuluh elemen dengan urutan tetap; rinciannya
	 * didokumentasikan pada
	 * {@link #ambilJumlahPertemuanStatistik(List, Mahasiswa, Dosen, boolean, boolean)}. Tidak ada
	 * konstanta indeks, sehingga pemanggil terikat pada urutan tersebut.</p>
	 *
	 * @param termasukDiskusi {@code true} untuk ikut menghitung jumlah diskusi tiap pertemuan;
	 *                        setiap pertemuan menambah satu pemanggilan tersendiri
	 * @param termasukUjian   {@code true} untuk ikut menghitung jumlah ujian tiap pertemuan; sama
	 *                        halnya, menambah satu pemanggilan per pertemuan
	 * @return larik statistik sepuluh elemen
	 * @throws Exception diteruskan dari perhitungan di method yang dipanggil; method ini sendiri
	 *                   tidak melempar secara langsung
	 */
	public Object[] ambilJumlahPertemuanStatistik(boolean termasukDiskusi, boolean termasukUjian) throws Exception {
		TreeMap<String, Long> pertemuanss = this.ambilPertemuan();
		List<Pertemuan> pertemuans = new ArrayList<Pertemuan>();
		
		if (pertemuanss != null && !pertemuanss.isEmpty()) {
			for (Long pertemuanid : pertemuanss.values()) {
				Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
				if (pertemuan != null) {
					pertemuans.add(pertemuan);
				}
			}
			pertemuanss.clear();
		}
		pertemuanss = null;

		return ambilJumlahPertemuanStatistik(pertemuans, null, null, termasukDiskusi, termasukUjian);
	}

	/**
	 * Menghitung statistik atas sekumpulan {@link Pertemuan} — bentuk inti yang dipakai seluruh
	 * varian {@code ambilJumlahPertemuanStatistik}.
	 *
	 * <h4>Susunan larik yang dikembalikan</h4>
	 * <p>Sepuluh elemen dengan urutan tetap dan tanpa konstanta indeks:</p>
	 * <ol start="0">
	 * <li>{@code Integer} — banyaknya pertemuan pada daftar masukan, dihitung sebelum penyaringan
	 * apa pun sehingga mencakup pertemuan tidak aktif juga;</li>
	 * <li>{@code Integer} — banyaknya pertemuan aktif yang tanggalnya sudah lewat;</li>
	 * <li>{@code Integer} — banyaknya pertemuan aktif yang sudah punya isian absensi;</li>
	 * <li>{@code Integer} — total seluruh hitungan status kehadiran mahasiswa;</li>
	 * <li>{@code Map<String,Integer>} — rekapitulasi status kehadiran mahasiswa per nama status;</li>
	 * <li>{@code Integer} — total hitungan status kehadiran dosen;</li>
	 * <li>{@code Map<String,Integer>} — rekapitulasi status kehadiran dosen per nama status;</li>
	 * <li>daftar {@link Pertemuan} masukan, diteruskan apa adanya;</li>
	 * <li>{@code Integer} — total ujian, hanya bila {@code termasukUjian};</li>
	 * <li>{@code Integer} — total diskusi, hanya bila {@code termasukDiskusi}.</li>
	 * </ol>
	 * <p><b>Elemen ke-7 berbeda tipe antar-overload.</b> Di sini ia
	 * {@code List<Pertemuan>}, sedangkan pada
	 * {@link #ambilJumlahPertemuanStatistik(Pertemuan, Collection, Dosen)} ia
	 * {@code Collection<Long>} berisi id peserta. Kode yang menerima larik dari kedua jalur wajib
	 * memeriksa tipenya sebelum melakukan {@code cast}.</p>
	 *
	 * <h4>Penyaringan dan makna parameter penyaring</h4>
	 * <p>Hanya pertemuan yang penanda {@code aktif}-nya bernilai benar yang diperhitungkan — kecuali
	 * pada elemen ke-0 yang sudah dihitung lebih dulu. Argumen {@code mahasiswa} dan {@code dosen}
	 * bukan penyaring daftar pertemuan, melainkan diteruskan ke perhitungan status: memberi
	 * {@code null} berarti "seluruh peserta", memberi seorang mahasiswa berarti rekapitulasi hanya
	 * untuk orang tersebut. Karena itu elemen ke-0 sampai ke-2 tidak berubah oleh kedua argumen
	 * ini, sedangkan elemen ke-3 sampai ke-6 berubah.</p>
	 *
	 * <p>Batas "sudah lewat" diukur terhadap waktu sekarang yang diambil sekali di awal lewat
	 * {@code WaktuUtil.getDate()}, bukan terhadap awal hari — pertemuan hari ini yang jamnya belum
	 * tiba tetap terhitung belum berlalu bila kolom tanggalnya menyimpan waktu.</p>
	 *
	 * <h4>Biaya</h4>
	 * <p>Untuk setiap pertemuan aktif, method memanggil perhitungan jumlah ujian, jumlah diskusi,
	 * status mahasiswa, dan status dosen secara terpisah. Kelas dengan enam belas pertemuan
	 * berarti puluhan pemanggilan berantai; menyalakan {@code termasukDiskusi} dan
	 * {@code termasukUjian} menggandakannya. Nyalakan hanya bila angkanya benar-benar ditampilkan.</p>
	 *
	 * <p>Tidak ada penangkap kesalahan di sepanjang method; kegagalan pada perhitungan status
	 * diteruskan ke pemanggil lewat {@code throws Exception}.</p>
	 *
	 * @param pertemuans      daftar pertemuan yang dihitung; {@code null} menghasilkan larik
	 *                        dengan seluruh angka bernilai nol
	 * @param mahasiswa       mahasiswa yang rekapitulasi kehadirannya diminta; {@code null} berarti
	 *                        seluruh mahasiswa
	 * @param dosen           dosen yang rekapitulasi kehadirannya diminta; {@code null} berarti
	 *                        seluruh dosen
	 * @param termasukDiskusi {@code true} untuk mengisi elemen ke-9
	 * @param termasukUjian   {@code true} untuk mengisi elemen ke-8
	 * @return larik statistik sepuluh elemen; tidak pernah {@code null}
	 * @throws Exception diteruskan dari perhitungan status kehadiran
	 */
	public Object[] ambilJumlahPertemuanStatistik(List<Pertemuan> pertemuans, Mahasiswa mahasiswa, Dosen dosen,
			boolean termasukDiskusi, boolean termasukUjian) throws Exception {

		int size = (pertemuans != null) ? pertemuans.size() : 0;
		int jumlahBerlalu = 0;
		int jumlahAbsensi = 0;
		int jumlahAbsensiTotal = 0;
		int jumlahUjianTotal = 0;
		int jumlahDiskusiTotal = 0;
		Map<String, Integer> semuaStatuses = new HashMap<String, Integer>();

		int jumlahAbsensiTotalDosen = 0;
		Map<String, Integer> semuaStatusesDosen = new HashMap<String, Integer>();

		Date sekarang = ais.ui.util.WaktuUtil.getDate();
		
		if (pertemuans != null) {
			for (Pertemuan pertemuan : pertemuans) {
				if (pertemuan != null && pertemuan.getAktif() != null && pertemuan.getAktif()) {
					if (termasukUjian) {
						jumlahUjianTotal += pertemuan.ambilJumlahPertemuanPunyaUjian(mahasiswa, null);
					}

					if (termasukDiskusi) {
						jumlahDiskusiTotal += pertemuan.ambilJumlahPertemuanPunyaDiskusi(mahasiswa, dosen);
					}

					if (pertemuan.getTanggal() != null && pertemuan.getTanggal().before(sekarang)) {
						jumlahBerlalu++;
					}
					
					if (pertemuan.getAbsensi() != null && !pertemuan.getAbsensi().isEmpty()) {
						jumlahAbsensi++;
						
						Map<String, Integer> statuses = pertemuan.hitungStatus(mahasiswa);
						Map<String, Integer> statusesDosen = pertemuan.hitungStatusDosen(dosen);

						if (statuses != null) {
							for (Map.Entry<String, Integer> entry : statuses.entrySet()) {
								String key = entry.getKey();
								Integer value = entry.getValue();
								jumlahAbsensiTotal += value;
								
								if (semuaStatuses.containsKey(key)) {
									semuaStatuses.put(key, semuaStatuses.get(key) + value);
								} else {
									semuaStatuses.put(key, value);
								}
							}
						}

						if (statusesDosen != null) {
							for (Map.Entry<String, Integer> entry : statusesDosen.entrySet()) {
								String key = entry.getKey();
								Integer value = entry.getValue();
								jumlahAbsensiTotalDosen += value;
								
								if (semuaStatusesDosen.containsKey(key)) {
									semuaStatusesDosen.put(key, semuaStatusesDosen.get(key) + value);
								} else {
									semuaStatusesDosen.put(key, value);
								}
							}
						}
					}
				}
			}
		}

		return new Object[] { size, jumlahBerlalu, jumlahAbsensi, jumlahAbsensiTotal, semuaStatuses,
				jumlahAbsensiTotalDosen, semuaStatusesDosen, pertemuans, jumlahUjianTotal, jumlahDiskusiTotal };
	}

	/**
	 * Menghitung statistik <b>satu</b> pertemuan yang dirinci per peserta — kebalikan sudut pandang
	 * dari {@link #ambilJumlahPertemuanStatistik(List, Mahasiswa, Dosen, boolean, boolean)} yang
	 * merinci per pertemuan.
	 *
	 * <p>Dipakai layar rincian satu pertemuan: berapa peserta, berapa yang sudah diabsen, dan
	 * bagaimana sebaran statusnya. Peserta diberikan sebagai koleksi id
	 * {@link Detailperkuliahan}, yang lalu diubah menjadi objek lewat cache proses; entri yang
	 * tidak ditemukan di cache atau yang relasi mahasiswanya kosong dilewati diam-diam.</p>
	 *
	 * <h4>Larik hasil memakai susunan yang sama, tetapi maknanya bergeser</h4>
	 * <p>Karena perulangan luarnya kini berjalan atas <i>peserta</i> dan bukan atas pertemuan,
	 * tiga elemen pertama berubah arti:</p>
	 * <ul>
	 * <li>elemen ke-0 adalah banyaknya <b>peserta</b>, bukan banyaknya pertemuan;</li>
	 * <li>elemen ke-1 ("sudah berlalu") bertambah satu untuk <b>setiap peserta</b> ketika tanggal
	 * pertemuan sudah lewat, sehingga nilainya menjadi nol atau sama dengan jumlah peserta —
	 * bukan angka pertemuan seperti yang disugerikan namanya;</li>
	 * <li>elemen ke-2 ("sudah ada absensi") berperilaku sama: nol atau sama dengan jumlah
	 * peserta.</li>
	 * </ul>
	 * <p>Elemen ke-7 di sini berisi koleksi id peserta ({@code Collection<Long>}), bukan daftar
	 * {@link Pertemuan}. Pemanggil yang memakai kedua overload harus memeriksa tipe sebelum
	 * meng-{@code cast}.</p>
	 *
	 * <h4>Statistik dosen dihitung terpisah</h4>
	 * <p>Setelah perulangan peserta selesai, method mengambil daftar dosen dari
	 * {@code pertemuan.getPerkuliahan().populateDosenBuNama()} — sehingga bagian ini <b>hanya
	 * berjalan untuk pertemuan yang tertaut ke {@link Perkuliahan}</b>; untuk KKN, PKL, skripsi,
	 * dan wadah lain, elemen ke-5 dan ke-6 selalu kosong. Argumen {@code dosen} berlaku sebagai
	 * penyaring: bila diberikan, hanya diskusi dosen tersebut yang ditambahkan, tetapi
	 * rekapitulasi status kehadiran tetap dikumpulkan untuk <b>seluruh</b> dosen pengampu.
	 * Ketidaksimetrisan itu ada pada kode dan didokumentasikan apa adanya.</p>
	 *
	 * <p><b>Jumlah ujian dan diskusi selalu dihitung</b> di sini — tidak ada bendera untuk
	 * mematikannya seperti pada overload berbasis daftar. Karena perhitungannya dilakukan sekali
	 * per peserta, kelas besar berarti banyak pemanggilan berantai.</p>
	 *
	 * <p>Method ini tidak melempar: masukan {@code null} pada {@code pertemuan} maupun
	 * {@code detailperkuliahans} melewati seluruh perhitungan dan menghasilkan larik bernilai
	 * nol.</p>
	 *
	 * @param pertemuan          pertemuan yang dirinci; {@code null} menghasilkan larik nol
	 * @param detailperkuliahans koleksi id peserta; {@code null} menghasilkan larik nol
	 * @param dosen              penyaring dosen untuk perhitungan diskusi; {@code null} berarti
	 *                           seluruh dosen pengampu
	 * @return larik statistik sepuluh elemen dengan makna yang bergeser sebagaimana dijelaskan di
	 *         atas
	 */
	public Object[] ambilJumlahPertemuanStatistik(Pertemuan pertemuan, Collection<Long> detailperkuliahans, Dosen dosen) {

		int size = (detailperkuliahans != null) ? detailperkuliahans.size() : 0;
		int jumlahBerlalu = 0;
		int jumlahAbsensi = 0;
		int jumlahAbsensiTotal = 0;
		int jumlahUjianTotal = 0;
		int jumlahDiskusiTotal = 0;
		Map<String, Integer> semuaStatuses = new HashMap<String, Integer>();

		int jumlahAbsensiTotalDosen = 0;
		Map<String, Integer> semuaStatusesDosen = new HashMap<String, Integer>();

		Date sekarang = ais.ui.util.WaktuUtil.getDate();
		
		if (detailperkuliahans != null && pertemuan != null) {
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				
				if (detailperkuliahan != null && detailperkuliahan.getMahasiswa() != null) {
					Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
					
					jumlahUjianTotal += pertemuan.ambilJumlahPertemuanPunyaUjian(mahasiswa, null);
					jumlahDiskusiTotal += pertemuan.ambilJumlahPertemuanPunyaDiskusi(mahasiswa, dosen);

					if (pertemuan.getTanggal() != null && pertemuan.getTanggal().before(sekarang)) {
						jumlahBerlalu++;
					}
					
					if (pertemuan.getAbsensi() != null && !pertemuan.getAbsensi().isEmpty()) {
						jumlahAbsensi++;
						Map<String, Integer> statuses = pertemuan.hitungStatus(mahasiswa);
						
						if (statuses != null) {
							for (Map.Entry<String, Integer> entry : statuses.entrySet()) {
								String key = entry.getKey();
								Integer value = entry.getValue();
								jumlahAbsensiTotal += value;
								
								if (semuaStatuses.containsKey(key)) {
									semuaStatuses.put(key, semuaStatuses.get(key) + value);
								} else {
									semuaStatuses.put(key, value);
								}
							}
						}
					}
				}
			}
			
			if (pertemuan.getPerkuliahan() != null) {
				List<Dosen> dosens = pertemuan.getPerkuliahan().populateDosenBuNama();
				if (dosens != null) {
					for (Dosen d : dosens) {
						if (dosen == null || (d.getId() != null && d.getId().equals(dosen.getId()))) {
							jumlahDiskusiTotal += pertemuan.ambilJumlahPertemuanPunyaDiskusi(null, d);
						}

						Map<String, Integer> statusesDosen = pertemuan.hitungStatusDosen(d);
						if (statusesDosen != null) {
							for (Map.Entry<String, Integer> entry : statusesDosen.entrySet()) {
								String key = entry.getKey();
								Integer value = entry.getValue();
								jumlahAbsensiTotalDosen += value;
								
								if (semuaStatusesDosen.containsKey(key)) {
									semuaStatusesDosen.put(key, semuaStatusesDosen.get(key) + value);
								} else {
									semuaStatusesDosen.put(key, value);
								}
							}
						}
					}
				}
			}
		}

		return new Object[] { size, jumlahBerlalu, jumlahAbsensi, jumlahAbsensiTotal, semuaStatuses,
				jumlahAbsensiTotalDosen, semuaStatusesDosen, detailperkuliahans, jumlahUjianTotal, jumlahDiskusiTotal };
	}

	/**
	 * Mengambil satu halaman id pertemuan untuk ditampilkan pada grid berhalaman, memakai cache.
	 *
	 * <p>Mengambil peta pertemuan lewat {@link #ambilPertemuan()} lalu meneruskannya ke
	 * {@link #ambilPertemuan(int, int, boolean, TreeMap)}. Bila pemanggil sudah memegang petanya —
	 * misalnya karena ingin menampilkan jumlah total sekaligus — panggil langsung varian yang
	 * menerima peta agar indeks tidak dibaca dua kali.</p>
	 *
	 * @param mulai     indeks awal halaman; diabaikan bila {@code tampilHal} bernilai benar
	 * @param banyak    banyaknya baris per halaman
	 * @param tampilHal {@code true} agar halaman ditentukan otomatis dari pertemuan terakhir yang
	 *                  sudah berlalu
	 * @return larik tiga elemen: daftar id halaman ini, jumlah seluruh pertemuan, dan indeks awal
	 *         yang benar-benar dipakai
	 */
	public Object[] ambilPertemuan(int mulai, int banyak, boolean tampilHal) {
		TreeMap<String, Long> pertemuansTemp = ambilPertemuan();
		return ambilPertemuan(mulai, banyak, tampilHal, pertemuansTemp);
	}

	/**
	 * Memotong satu halaman dari peta pertemuan yang sudah dipegang pemanggil, dengan kemampuan
	 * "lompat ke halaman berjalan".
	 *
	 * <h4>Susunan nilai balik</h4>
	 * <p>Larik tiga elemen: (0) {@code List<Long>} berisi id pertemuan pada halaman ini,
	 * (1) {@code Integer} jumlah seluruh pertemuan pada peta, dan (2) {@code Integer} indeks awal
	 * yang benar-benar dipakai setelah penyesuaian. Elemen ketiga penting bagi pemanggil karena
	 * nilai {@code mulai} yang dikirim bisa diubah oleh method ini.</p>
	 *
	 * <h4>Bagaimana halaman berjalan ditentukan</h4>
	 * <p>Bila {@code tampilHal} bernilai benar, method menghitung {@code aktifKe}: banyaknya
	 * pertemuan yang tanggalnya sudah lewat kemarin. Tanggal itu dibaca dari <b>kunci peta</b>,
	 * bukan dari entitas — kunci diurai dengan mengambil bagian sebelum garis bawah dan
	 * memformatnya sebagai tanggal delapan digit. Halaman lalu dipilih sebagai kelipatan
	 * {@code banyak} yang memuat {@code aktifKe}, atau tepat {@code aktifKe} bila satu baris per
	 * halaman.</p>
	 * <p><b>Fitur ini hanya bekerja pada objek pembelajaran yang penomorannya otomatis.</b> Kunci
	 * peta hanya berbentuk tanggal ketika {@link #getUrutkanotomatis()} bernilai benar; pada
	 * penomoran manual, {@link #masukkanPertemuanLocal} menyusun kunci dari nomor pertemuan yang
	 * dipadkan menjadi empat digit. Penjaga panjang minimal delapan karakter pada penguraian
	 * membuat seluruh kunci empat digit itu dilewati, sehingga {@code aktifKe} tetap nol dan
	 * {@code tampilHal} selalu mendarat di halaman pertama. Perilaku ini tidak dilaporkan sebagai
	 * kesalahan — penguraian yang gagal hanya dilewati diam-diam.</p>
	 *
	 * <h4>Penyesuaian indeks awal</h4>
	 * <p>Bila {@code mulai} berada di luar batas peta, ia dikurangi satu halaman; bila lalu menjadi
	 * negatif, ia disetel nol. Penyesuaian dilakukan sekali saja, sehingga {@code mulai} yang jauh
	 * melampaui ukuran peta tetap dapat berujung pada halaman kosong — nilai elemen ketiga
	 * memberi tahu pemanggil apa yang sebenarnya dipakai.</p>
	 *
	 * <p>Peta {@code null} menghasilkan larik dengan daftar kosong, jumlah nol, dan {@code mulai}
	 * apa adanya. Urutan id pada halaman mengikuti urutan kunci peta, yaitu urutan tanggal atau
	 * nomor pertemuan sesuai penanda penomoran.</p>
	 *
	 * @param mulai          indeks awal halaman; dapat disesuaikan oleh method ini
	 * @param banyak         banyaknya baris per halaman
	 * @param tampilHal      {@code true} agar halaman ditentukan otomatis dari pertemuan terakhir
	 *                       yang sudah berlalu
	 * @param pertemuansTemp peta pertemuan yang dipotong; boleh {@code null}
	 * @return larik tiga elemen sebagaimana dijelaskan di atas
	 */
	public Object[] ambilPertemuan(int mulai, int banyak, boolean tampilHal, TreeMap<String, Long> pertemuansTemp) {
		int index = 0;
		List<Long> pertemuans = new ArrayList<Long>();
		int aktifKe = 0;

		if (pertemuansTemp == null) {
			return new Object[] { pertemuans, 0, mulai };
		}

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
		
		for (String a : pertemuansTemp.keySet()) {
			try {
				if (a == null || a.indexOf("_") < 0 || a.split("_")[0] == null
						|| a.split("_")[0].trim().length() < 8) {
					continue;
				}
				Date tgl = Common.dateFormat8.get().parse(a.split("_")[0]);
				if (tgl.before(calendar.getTime())) {
					aktifKe++;
				}
			} catch (Exception e) {
				// Ignored
			}
		}

		if (tampilHal) {
			mulai = banyak == 1 ? aktifKe : ((int) (aktifKe / banyak)) * banyak;
		}

		if (mulai >= pertemuansTemp.size()) {
			mulai = mulai - banyak;
		}

		if (mulai < 0) {
			mulai = 0;
		}

		for (Long pertemuanid : pertemuansTemp.values()) {
			if (index >= mulai && index < (mulai + banyak)) {
				pertemuans.add(pertemuanid);
			}
			index++;
		}
		
		int size = pertemuansTemp.size();
		pertemuansTemp = null;
		
		return new Object[] { pertemuans, size, mulai };
	}

	/**
	 * Mengambil seluruh pertemuan aktif milik objek ini sebagai daftar objek, memakai cache.
	 *
	 * <p>Setara dengan {@code ambilPertemuanList(false)}. Berbeda dari
	 * {@link #ambilPertemuan()} yang mengembalikan peta id, method ini mengembalikan objek
	 * {@link Pertemuan} yang siap dibaca.</p>
	 *
	 * @return daftar pertemuan aktif terurut sesuai kunci cache; kosong bila tidak ada
	 */
	public List<Pertemuan> ambilPertemuanList() {
		return ambilPertemuanList(false);
	}

	/**
	 * Mengambil seluruh pertemuan aktif milik objek ini sebagai daftar objek.
	 *
	 * <p>Menelusuri peta hasil {@link #ambilPertemuan(boolean)} dan mengubah tiap id menjadi objek
	 * lewat cache proses, dengan tiga penyaringan berlapis:</p>
	 * <ol>
	 * <li>nilai peta yang {@code null} dilewati — peta bisa memuat nilai kosong akibat entri
	 * indeks yang rusak, dan tanpa penjaga ini pemanggilan {@code toString()} akan melempar;</li>
	 * <li>objek yang tidak ditemukan di cache proses dilewati;</li>
	 * <li>pertemuan yang penanda {@code aktif}-nya {@code null} atau salah dilewati.</li>
	 * </ol>
	 *
	 * <p><b>Hanya membaca cache proses.</b> Berbeda dari {@link #ambilPertemuan(boolean)} yang
	 * menyediakan cadangan kueri basis data untuk id yang belum termuat, method ini tidak. Namun
	 * karena {@link #ambilPertemuan(boolean)} dipanggil lebih dulu dan sudah memasukkan objek yang
	 * dijemputnya ke cache, dalam praktiknya seluruh id pada peta biasanya sudah tersedia. Yang
	 * tetap mungkin hilang adalah objek yang tergusur dari cache di antara kedua langkah.</p>
	 *
	 * <p>Penyaringan aktif di sini <b>menggandakan</b> penyaringan yang sudah dilakukan
	 * {@link #ambilPertemuan(boolean)}. Duplikasi itu tidak mengubah hasil, tetapi berarti
	 * pertemuan yang statusnya berubah di antara kedua langkah dinilai dua kali.</p>
	 *
	 * @param refresh {@code true} untuk membangun ulang indeks dari basis data lebih dulu; lihat
	 *                {@link #ambilPertemuan(boolean)} untuk efek sampingnya
	 * @return daftar pertemuan aktif; tidak pernah {@code null}
	 */
	public List<Pertemuan> ambilPertemuanList(boolean refresh) {
		TreeMap<String, Long> pertemuanss = ambilPertemuan(refresh);
		List<Pertemuan> pertemuans = new ArrayList<Pertemuan>();
		
		if (pertemuanss != null) {
			for (Long pertemuanid : pertemuanss.values()) {
				if (pertemuanid == null) continue; // KE-8/9: nilai map bisa null -> toString() NPE
				Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
				if (pertemuan != null && pertemuan.getAktif() != null && pertemuan.getAktif()) {
					pertemuans.add(pertemuan);
				}
			}
		}
		return pertemuans;
	}

	/**
	 * Menyisipkan satu {@link Pertemuan} ke peta kerja dengan kunci yang menentukan urutan
	 * tampilnya, sekaligus menyambungkan relasi baliknya ke {@code this}.
	 *
	 * <h4>Bentuk kunci menentukan urutan</h4>
	 * <p>Peta yang dipakai adalah {@link TreeMap}, sehingga urutan iterasinya adalah urutan
	 * leksikografis kuncinya. Ada dua bentuk kunci, dipilih menurut
	 * {@link #getUrutkanotomatis()} pada wadah pemilik pertemuan (bukan pada {@code this}):</p>
	 * <ul>
	 * <li><b>Penomoran manual</b> — {@code NNNN_id}, dengan nomor pertemuan dipadkan nol menjadi
	 * empat digit. Padding itu yang membuat urutan leksikografis sama dengan urutan numerik;
	 * batasnya empat digit, sehingga nomor pertemuan 10000 ke atas akan terpotong dan salah urut.
	 * Nomor pertemuan yang {@code null} menghasilkan kunci berawalan {@code "null"} yang jatuh
	 * setelah seluruh kunci berangka.</li>
	 * <li><b>Penomoran otomatis</b> — {@code yyyyMMdd_id}. Tanggal yang {@code null} menghasilkan
	 * awalan kosong sehingga pertemuan tak bertanggal <b>melompat ke urutan paling awal</b>, bukan
	 * ke akhir.</li>
	 * </ul>
	 * <p>Bagian {@code _id} pada kedua bentuk memastikan dua pertemuan bertanggal atau bernomor
	 * sama tidak saling menimpa di dalam peta.</p>
	 *
	 * <h4>Bahaya {@code null} pada penanda penomoran</h4>
	 * <p>Pemeriksaannya ditulis {@code !pembelajaran.getUrutkanotomatis()} tanpa penjaga
	 * {@code null}, sehingga wadah yang penandanya belum terisi memicu
	 * {@link NullPointerException} saat pembungkus {@link Boolean} dibuka. Kesalahan itu ditangkap
	 * oleh {@code catch} di method ini dan hanya dicatat ke audit — akibatnya
	 * {@code pertemuansTemp.put(...)} tidak pernah dijalankan dan <b>pertemuan tersebut hilang dari
	 * daftar tanpa jejak yang terlihat pengguna</b>. Gejalanya berupa jumlah pertemuan yang lebih
	 * sedikit dari semestinya, bukan pesan kesalahan. Pastikan {@link #getUrutkanotomatis()} pada
	 * setiap subclass selalu mengembalikan nilai.</p>
	 *
	 * <h4>Penyambungan relasi balik dan pemeriksaan tipe</h4>
	 * <p>Setelah penyisipan, method menyetel relasi pemilik pada objek pertemuan kembali ke
	 * {@code this} agar pembacaan berikutnya tidak perlu menginisialisasi proxy lazy. Rantai
	 * {@code else if} yang melakukannya memeriksa <b>dua</b> syarat: relasi bersangkutan pada
	 * pertemuan tidak kosong, <i>dan</i> {@code this} memang instance subclass yang cocok.</p>
	 * <p>Syarat kedua ditambahkan sebagai perbaikan: satu baris {@link Pertemuan} dapat memiliki
	 * lebih dari satu kolom relasi terisi sekaligus (mis. kelompok KKN dan formulir kegiatan
	 * bersamaan). Tanpa pemeriksaan tipe, cabang yang kebetulan lebih dulu cocok akan meng-{@code
	 * cast} {@code this} secara paksa dan melempar {@link ClassCastException}. Dengan pemeriksaan
	 * itu, cabang yang tidak cocok cukup dilewati.</p>
	 * <p>Rantai ini mengenal empat belas subclass. Beberapa turunan {@link VOPembelajaran} tidak
	 * punya cabang di sini — di antaranya {@link GrupPertemuan},
	 * {@code recruitment.JadwalUjianPegawai}, dan {@code kursus.KomponenDataProdukKursus} — sehingga
	 * pertemuannya tetap masuk peta tetapi relasi baliknya tidak disambungkan. Untuk wadah
	 * tersebut, pembacaan relasi pemilik akan menginisialisasi proxy seperti biasa.</p>
	 *
	 * <p>Argumen {@code null}, pertemuan tanpa id, dan peta {@code null} sama-sama membuat method
	 * kembali tanpa efek.</p>
	 *
	 * @param pertemuansTemp peta kerja yang disisipi; boleh {@code null} (diabaikan)
	 * @param pertemuan      pertemuan yang disisipkan; {@code null} atau tanpa id diabaikan
	 */
	private void masukkanPertemuanLocal(TreeMap<String, Long> pertemuansTemp, Pertemuan pertemuan) {
		if (pertemuan != null && pertemuan.getId() != null && pertemuansTemp != null) {
			try {
				VOPembelajaran pembelajaran = pertemuan.ambilVOPembelajaran();
				String urt;
				
				if (pembelajaran != null && !pembelajaran.getUrutkanotomatis()) {
					urt = "0000000000000000000000" + pertemuan.getPertemuanKe();
					urt = urt.substring(urt.length() - 4) + "_" + pertemuan.getId();
				} else {
					urt = (pertemuan.getTanggal() == null ? "" : Common.dateFormat8.get().format(pertemuan.getTanggal()))
							+ "_" + pertemuan.getId();
				}

				pertemuansTemp.put(urt, pertemuan.getId());
				
				// Setiap cabang di bawah HANYA valid bila runtime type `this` memang subclass
				// terkait — sebelumnya kondisi hanya mengecek pertemuan.getXxx() != null lalu
				// cast `this` tanpa syarat. Sebuah baris Pertemuan bisa punya lebih dari satu
				// FK non-null (mis. kelompok_kkn_id DAN formulir_kegiatan_id terisi bersamaan),
				// sehingga saat `this` sebenarnya instance FormulirKegiatan tapi cabang
				// getKelompokKkn()/getMahasiswaRequestTugasAkhir() kena duluan, cast paksa ke
				// KelompokKkn/MahasiswaRequestTugasAkhir melempar ClassCastException. Tambahkan
				// instanceof agar cabang yang tak cocok dilewati (bukan crash).
				if (pertemuan.getPerkuliahan() != null && this instanceof Perkuliahan) {
					pertemuan.setPerkuliahan((Perkuliahan) this);
				} else if (pertemuan.getKelompokKkn() != null && this instanceof KelompokKkn) {
					pertemuan.setKelompokKkn((KelompokKkn) this);
				} else if (pertemuan.getKelompokPkl() != null && this instanceof KelompokPkl) {
					pertemuan.setKelompokPkl((KelompokPkl) this);
				} else if (pertemuan.getMahasiswaRequestTugasAkhir() != null
						&& this instanceof MahasiswaRequestTugasAkhir) {
					pertemuan.setMahasiswaRequestTugasAkhir((MahasiswaRequestTugasAkhir) this);
				} else if (pertemuan.getSkripsi() != null && this instanceof Skripsi) {
					pertemuan.setSkripsi((Skripsi) this);
				} else if (pertemuan.getKrsMahasiswa() != null && this instanceof KrsMahasiswa) {
					pertemuan.setKrsMahasiswa((KrsMahasiswa) this);
				} else if (pertemuan.getPertemuanPunyaGrupPertemuan() != null
						&& this instanceof PertemuanPunyaGrupPertemuan) {
					pertemuan.setPertemuanPunyaGrupPertemuan((PertemuanPunyaGrupPertemuan) this);
				} else if (pertemuan.getJadwalUjianPMB() != null && this instanceof JadwalUjianPMB) {
					pertemuan.setJadwalUjianPMB((JadwalUjianPMB) this);
				} else if (pertemuan.getJadwalUjianPSB() != null && this instanceof JadwalUjianPSB) {
					pertemuan.setJadwalUjianPSB((JadwalUjianPSB) this);
				} else if (pertemuan.getFormulirKegiatan() != null && this instanceof FormulirKegiatan) {
					pertemuan.setFormulirKegiatan((FormulirKegiatan) this);
				} else if (pertemuan.getJadwalPelajaran() != null && this instanceof JadwalPelajaran) {
					pertemuan.setJadwalPelajaran((JadwalPelajaran) this);
				} else if (pertemuan.getJadwalPertemuanPSB() != null && this instanceof JadwalPertemuanPSB) {
					pertemuan.setJadwalPertemuanPSB((JadwalPertemuanPSB) this);
				} else if (pertemuan.getWisuda() != null && this instanceof Wisuda) {
					pertemuan.setWisuda((Wisuda) this);
				} else if (pertemuan.getKelasLesSiswa() != null && this instanceof KelasLesSiswa) {
					pertemuan.setKelasLesSiswa((KelasLesSiswa) this);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:432");
				// Ignored safely
			}
		}
	}

	/**
	 * Mengambil peta pertemuan aktif milik objek pembelajaran ini dari cache, tanpa memaksa
	 * pembaruan.
	 *
	 * <p>Setara dengan {@code ambilPertemuan(false)}. Kunci petanya menentukan urutan tampil dan
	 * bentuknya bergantung pada penanda penomoran; lihat {@link #masukkanPertemuanLocal}.</p>
	 *
	 * @return peta kunci-urutan ke id pertemuan; tidak pernah {@code null}
	 */
	public TreeMap<String, Long> ambilPertemuan() {
		return ambilPertemuan(false);
	}

	/**
	 * Inti pengambilan pertemuan: memastikan indeks ada, mengubah id menjadi objek, dan menyusunnya
	 * menjadi peta terurut.
	 *
	 * <p>Hampir seluruh method pertemuan di kelas ini bermuara ke sini —
	 * {@link #ambilJumlahPertemuan()}, {@link #ambilPertemuanList(boolean)},
	 * {@link #ambilPertemuan(int, int, boolean)}, dan
	 * {@link #ambilJumlahPertemuanStatistik(boolean, boolean)}.</p>
	 *
	 * <h4>Tahap 1 — memastikan indeks ada</h4>
	 * <p>Bila {@code refresh} bernilai benar <i>atau</i> penanda {@link GeneralValueObject#udah()}
	 * melaporkan indeks belum pernah dibangun, method membuka session baru lewat
	 * {@code openSession()} dan menjalankan {@link #reInitPertemuan(Session)} di atasnya, lalu
	 * menutup session itu sendiri pada blok {@code finally}. Perhatikan bahwa pemeriksaan penanda
	 * bersifat test-and-set: pemanggilan pertama untuk suatu objek selalu melaporkan "belum" dan
	 * sekaligus memasang penandanya, sehingga jalur pembangunan ulang otomatis dilalui sekali per
	 * objek per masa hidup berkas penanda — bukan hanya ketika pemanggil memintanya.</p>
	 * <p>Karena {@link #reInitPertemuan(Session)} menyinkronkan diskusi, ujian, tugas, izin, dan
	 * berkas media setiap pertemuan sekaligus menulis ulang nomor pertemuan, tahap ini jauh lebih
	 * mahal dan lebih berdampak daripada namanya menyiratkan. Session yang dipakai terpisah dari
	 * session pemanggil, sehingga perubahan yang belum di-{@code flush} oleh pemanggil tidak
	 * terlihat di sini — dan sebaliknya, penulisan di sini ter-{@code commit} secara mandiri.</p>
	 *
	 * <h4>Tahap 2 — dari indeks ke objek</h4>
	 * <p>Indeks dibaca lewat {@link #ambilLokasiPertemuan()}. Setiap kunci yang nilainya tidak
	 * kosong dicari di cache proses; yang ditemukan dan berstatus aktif langsung disisipkan lewat
	 * {@link #masukkanPertemuanLocal}, sedangkan yang tidak ditemukan dikumpulkan sebagai
	 * "belum ada".</p>
	 *
	 * <h4>Tahap 3 — menjemput yang belum ada dari basis data</h4>
	 * <p>Bila ada id yang belum termuat, method membuka session kedua dan mengueri seluruhnya
	 * sekaligus dengan {@code Restrictions.in}, dibatasi pada pertemuan yang aktifnya {@code null}
	 * atau benar. Hasilnya dimasukkan ke cache proses lalu disisipkan ke peta. Session ini pun
	 * ditutup sendiri. Berbeda dari tahap 1, kegagalan di sini tidak mengulang apa pun — id yang
	 * gagal dijemput sekadar tidak muncul.</p>
	 * <p>Perhatikan bahwa restriksi kueri ini <b>hanya</b> membatasi status aktif dan daftar id;
	 * tidak ada pembatasan kepemilikan. Keamanan bergantung sepenuhnya pada asumsi bahwa id di
	 * berkas indeks memang milik objek ini. Berkas indeks yang tercemar id milik wadah lain akan
	 * membuat pertemuan asing muncul di daftar ini.</p>
	 *
	 * <h4>Tahap 4 — pemotongan khusus Perkuliahan</h4>
	 * <p>Bila {@code this} adalah {@link Perkuliahan} dengan penomoran manual, peta dipotong pada
	 * {@code jumlahMaksimalPertemuan}: hanya sekian entri pertama yang dipertahankan. Pemotongan
	 * dilakukan <b>setelah</b> pengurutan, sehingga yang dibuang adalah pertemuan bernomor
	 * terbesar. Batas yang {@code null} diperlakukan sebagai nol, yang berarti <b>seluruh
	 * pertemuan dibuang</b> — perkuliahan berpenomoran manual yang belum diisi batas maksimalnya
	 * akan tampak tidak punya pertemuan sama sekali. Pemotongan ini tidak berlaku bagi subclass
	 * lain maupun bagi perkuliahan berpenomoran otomatis.</p>
	 *
	 * <p>Seluruh kegagalan pada tahap 2 dan 3 ditelan dan dicatat ke audit, sehingga method
	 * mengembalikan peta sebagian alih-alih melempar. Satu-satunya kesalahan yang benar-benar
	 * diteruskan adalah {@link NullPointerException} dari {@link #ambilLokasiPertemuan()} pada
	 * objek yang belum tersimpan.</p>
	 *
	 * @param refresh {@code true} untuk memaksa membangun ulang indeks lewat
	 *                {@link #reInitPertemuan(Session)}
	 * @return peta kunci-urutan ke id pertemuan; tidak pernah {@code null}
	 */
	@SuppressWarnings("unchecked")
	public TreeMap<String, Long> ambilPertemuan(boolean refresh) {
		Session sessionTemp = null;
		
		if (!udah() || refresh) {
			try {
				sessionTemp = HibernateUtil.getSessionFactory().openSession();
				reInitPertemuan(sessionTemp);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOPembelajaran.java:451");
			} finally {
				if (sessionTemp != null) {
					try { if (sessionTemp.isOpen()) sessionTemp.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:454");}
					try { sessionTemp.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:455");}
					try { if (sessionTemp.isOpen()) sessionTemp.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:456");}
				}
			}
		}
		
		TreeMap<String, Long> pertemuansTemp = new TreeMap<String, Long>();
		List<Long> idsBelumAda = new ArrayList<Long>();
		
		try {
			JSONObject c = new JSONObject(ambilLokasiPertemuan());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (s != null && !s.trim().isEmpty()) {
						Pertemuan pertemuan = null;
						GeneralValueObject generalValueObject = ambilData(Pertemuan.class, key);
						if (generalValueObject != null) {
							pertemuan = ((Pertemuan) generalValueObject);
						} else {
							idsBelumAda.add(Long.parseLong(key));
						}
						
						if (pertemuan != null && pertemuan.getAktif() != null && pertemuan.getAktif()) {
							masukkanPertemuanLocal(pertemuansTemp, pertemuan);
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:484");
					// Ignored
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:488");
			// Ignored
		}

		if (!idsBelumAda.isEmpty()) {
			Session session = null;
			try {
				session = HibernateUtil.getSessionFactory().openSession();
				List<Pertemuan> pertemuans = session.createCriteria(Pertemuan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.in("id", idsBelumAda)).list();
						
				for (Pertemuan pertemuan : pertemuans) {
					if (pertemuan.getAktif() != null && pertemuan.getAktif()) {
						GeneralValueObject.masukkanData(Pertemuan.class, pertemuan);
						masukkanPertemuanLocal(pertemuansTemp, pertemuan);
					}
				}
				pertemuans.clear();
				pertemuans = null;
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOPembelajaran.java:509");
			} finally {
				if (session != null) {
					try { if (session.isOpen()) session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:512");}
					try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:513");}
					try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:514");}
				}
			}
		}

		if (this instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) this;
			if (perkuliahan.getUrutkanotomatis() != null && !perkuliahan.getUrutkanotomatis()) {
				TreeMap<String, Long> pertemuansTempBaru = new TreeMap<String, Long>();
				int index = 1;
				int maks = perkuliahan.getJumlahMaksimalPertemuan() != null ? perkuliahan.getJumlahMaksimalPertemuan() : 0;
				for (Map.Entry<String, Long> entry : pertemuansTemp.entrySet()) {
					if (index <= maks) {
						pertemuansTempBaru.put(entry.getKey(), entry.getValue());
					}
					index++;
				}
				pertemuansTemp.clear();
				pertemuansTemp = pertemuansTempBaru;
			}
		}

		idsBelumAda.clear();
		idsBelumAda = null;

		return pertemuansTemp;
	}

	/**
	 * Membangun ulang indeks pertemuan dari daftar yang <b>sudah</b> disediakan pemanggil, tanpa
	 * mengueri basis data.
	 *
	 * <p>Varian paling ringan dari keluarga {@code reInitPertemuan}: pemanggil sudah tahu persis
	 * pertemuan mana yang relevan (mis. baru saja membuatnya), sehingga rantai restriksi
	 * {@code instanceof} yang panjang pada {@link #reInitPertemuan(Session)} tidak diperlukan.
	 * Karena tidak ada kueri, tidak ada pula penyaringan kepemilikan — <b>daftar apa pun yang
	 * diberikan akan didaftarkan ke indeks objek ini</b>, termasuk pertemuan milik wadah lain.
	 * Pemanggil bertanggung jawab penuh atas isi daftarnya.</p>
	 *
	 * <h4>Yang dikerjakan</h4>
	 * <p>Indeks dikosongkan lebih dulu, lalu untuk tiap pertemuan aktif: bila wadah pemiliknya
	 * memakai penomoran otomatis dan nomor pertemuannya belum sesuai urutan berjalan, nomor itu
	 * <b>ditulis ulang dan disimpan</b> ke basis data; setelah itu id-nya didaftarkan ke indeks.
	 * Nomor urut berjalan bertambah untuk setiap pertemuan aktif, termasuk yang nomornya tidak
	 * ditulis ulang.</p>
	 *
	 * <h4>Transaksi</h4>
	 * <p>Bila session yang diberikan belum punya transaksi aktif, method membukanya sendiri dan
	 * bertanggung jawab melakukan {@code commit} atau {@code rollback}. Bila pemanggil sudah
	 * memegang transaksi, penulisan di sini ikut serta di dalamnya dan penutupannya tetap menjadi
	 * urusan pemanggil. Session-nya sendiri tidak pernah ditutup oleh method ini.</p>
	 *
	 * <h4>Penanganan objek kembar pada session</h4>
	 * <p>Sebelum {@code update}, method memeriksa apakah instance pertemuan memang yang dikelola
	 * session. Bila bukan — hal yang terjadi ketika objek dengan id sama sudah termuat lebih dulu
	 * lewat jalur lain — instance lama di-{@code evict} agar Hibernate tidak melempar
	 * {@code NonUniqueObjectException}. Ini perbaikan yang disengaja, bukan kehati-hatian
	 * berlebihan.</p>
	 *
	 * <p>Kegagalan apa pun memicu {@code rollback} atas transaksi lokal, dicetak, dicatat ke audit,
	 * lalu ditelan — method tidak melempar ke pemanggil. Akibatnya indeks dapat tertinggal dalam
	 * keadaan sebagian tanpa pemanggil mengetahuinya.</p>
	 *
	 * @param pertemuans daftar pertemuan yang didaftarkan; {@code null} membuat method kembali
	 *                   tanpa efek — indeks pun tidak dikosongkan
	 * @param session    session Hibernate; {@code null} membuat method kembali tanpa efek
	 */
	public void reInitPertemuan(List<Pertemuan> pertemuans, Session session) {
		if (pertemuans == null || session == null) return;
		
		tulisLokasiPertemuan(new JSONObject().toString());
		int pertemuanKe = 1;
		boolean localTransaction = false;
		
		try {
			if (!session.getTransaction().isActive()) {
				session.beginTransaction();
				localTransaction = true;
			}
			
			for (Pertemuan pertemuan : pertemuans) {
				if (pertemuan != null && pertemuan.getAktif() != null && pertemuan.getAktif()) {
					VOPembelajaran pembelajaran = pertemuan.ambilVOPembelajaran();
					if (pembelajaran != null && pembelajaran.getUrutkanotomatis() != null && pembelajaran.getUrutkanotomatis()) {
						if (pertemuan.getPertemuanKe() == null || !pertemuan.getPertemuanKe().equals(pertemuanKe)) {
							pertemuan.setPertemuanKe(pertemuanKe);
							// KE-FIX (NonUniqueObjectException "a different object with the same
							// identifier value was already associated with the session"): pertemuan
							// bisa saja bukan instance yang sama dgn yang sudah managed session utk
							// id yang sama (mis. termuat via ambilVOPembelajaran()/populatePertemuan()
							// sebelumnya). Evict instance lama dulu sebelum update() bila beda instance.
							if (!session.contains(pertemuan)) {
								Object existing = session.get(Pertemuan.class, pertemuan.getId());
								if (existing != null && existing != pertemuan) {
									session.evict(existing);
								}
							}
							session.update(pertemuan);
						}
					}
					pertemuanKe++;
					populatePertemuan(pertemuan);
				}
			}
			
			if (localTransaction) {
				session.getTransaction().commit();
			}
		} catch (Exception e) {
			if (localTransaction && session.getTransaction().isActive()) {
				try { session.getTransaction().rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:574");}
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOPembelajaran.java:576");
		}
	}

	/**
	 * Membangun ulang seluruh data pertemuan objek pembelajaran ini dari basis data — mesin
	 * sinkronisasi terberat di kelas ini.
	 *
	 * <p>Pintu masuk publik yang meneruskan ke bentuk privatnya dengan pengulangan saat kunci macet
	 * diizinkan. Lihat {@link #reInitPertemuan(Session, boolean)} untuk rincian lengkap alur,
	 * cakupan subclass, efek samping penulisan, dan mekanisme pengulangannya.</p>
	 *
	 * <p><b>Bukan operasi baca.</b> Method ini membuka transaksi, menulis ulang nomor pertemuan,
	 * menyinkronkan diskusi, ujian, tugas, izin tidak masuk, parameter tambahan, serta berkas
	 * media setiap pertemuan. Untuk sekadar menampilkan daftar, pakai
	 * {@link #ambilPertemuan(boolean)} dengan {@code refresh} bernilai salah.</p>
	 *
	 * @param session session Hibernate; {@code null} membuat method kembali tanpa efek
	 */
	@SuppressWarnings("unchecked")
	public void reInitPertemuan(Session session) {
		reInitPertemuan(session, true);
	}

	/**
	 * Pelaksana sinkronisasi pertemuan, dengan bendera yang mengendalikan apakah boleh mengulang
	 * ketika transaksi dibatalkan karena kunci basis data macet.
	 *
	 * <h4>Rantai restriksi: siapa yang ikut dan siapa yang tidak</h4>
	 * <p>Pertemuan diambil dengan kueri berproyeksi id, dibatasi pada baris yang aktifnya
	 * {@code null} atau benar, bertanggal, dan merujuk ke {@code this} lewat relasi yang dipilih
	 * dari rantai {@code instanceof} berisi <b>empat belas</b> cabang. Rantai yang sama persis
	 * disalin ke {@link #reInitTugas(Session)} dan {@link #reInitUjian(Session)}.</p>
	 * <p>Cabang penutupnya adalah {@code Restrictions.sqlRestriction("false")} — perilaku
	 * gagal-tertutup yang menghasilkan nol baris. Sikap itu tepat sebagai perlindungan, tetapi
	 * berarti <b>tiga subclass {@link VOPembelajaran} yang tidak terdaftar tidak akan pernah dapat
	 * membangun ulang indeks pertemuannya</b>: {@link GrupPertemuan},
	 * {@code sekolah.JadwalPertemuanPSB}, dan {@code sekolah.KelasLesSiswa}. Dua yang terakhir
	 * bahkan punya cabang tersendiri di {@link #masukkanPertemuanLocal}, yang menunjukkan bahwa
	 * pertemuannya memang ada. Untuk ketiganya, indeks hanya terisi lewat pemanggilan
	 * {@link #populatePertemuan(Pertemuan)} dari jalur lain, dan setiap pemanggilan dengan
	 * {@code refresh} justru <b>mengosongkannya</b>. Periksa ulang sebelum menganggap daftar
	 * pertemuan yang kosong pada wadah tersebut sebagai data yang memang tidak ada.</p>
	 *
	 * <h4>Urutan dan penulisan ulang nomor pertemuan</h4>
	 * <p>Kueri diurutkan menurut {@code pertemuanKe} bila penomorannya manual, dan menurut
	 * {@code tanggal} bila otomatis, lalu menurut id sebagai pemecah seri. Untuk wadah berpenomoran
	 * otomatis, nomor pertemuan ditulis ulang menjadi urutan berjalan 1, 2, 3, ... dan disimpan ke
	 * basis data setiap kali berbeda dari nilai lama. Untuk wadah berpenomoran manual, nomor yang
	 * diisi pengguna dipertahankan.</p>
	 *
	 * <h4>Apa saja yang ikut disinkronkan per pertemuan</h4>
	 * <p>Untuk setiap pertemuan aktif, method memanggil enam pembangun ulang di dalam session
	 * utama — diskusi, ujian, tugas pertemuan, tugas kelompok, pengajuan izin tidak masuk, dan
	 * parameter tambahan — lalu menerapkan izin tidak masuk yang sudah disetujui sebagai isian
	 * absensi ({@code populate} disusul {@code refreshUpdate}). Setelah itu ia membuka
	 * <b>session terpisah pada penyimpanan streaming</b> untuk membangun ulang berkas materi,
	 * berkas tugas, video, dan audio pertemuan; session itu ditutup sendiri pada blok
	 * {@code finally}, dan kegagalannya tidak membatalkan sinkronisasi utama.</p>
	 * <p>Karena semua itu terjadi per pertemuan, satu pemanggilan untuk kelas berisi enam belas
	 * pertemuan berarti ratusan kueri dan belasan session streaming. Inilah alasan method ini
	 * hanya boleh dipanggil dari alur sinkronisasi eksplisit, bukan dari jalur render halaman.</p>
	 *
	 * <h4>Pengulangan saat kunci macet</h4>
	 * <p>Penyusunan ulang nomor pertemuan dapat berbarengan dengan absensi atau sinkronisasi kelas
	 * yang menyentuh baris yang sama, dan PostgreSQL membatalkan transaksi dengan
	 * {@code lock_timeout}. Bila {@code bolehUlangSaatLock} bernilai benar dan
	 * {@link #adalahLockTimeoutPertemuan(Throwable)} mengenali kegagalannya sebagai macet, method
	 * menunggu dua ratus milidetik lalu mengulang seluruh unit kerja <b>satu kali</b> pada session
	 * yang benar-benar baru — transaksi yang sudah dibatalkan tidak boleh dipakai lagi. Pengulangan
	 * itu memanggil dirinya dengan bendera bernilai salah sehingga tidak dapat berulang tanpa
	 * batas.</p>
	 *
	 * <p>Blok penanganan kesalahan memeriksa {@code session.isOpen()} sebelum menyentuh
	 * transaksinya. Penjaga itu disengaja: bila session sudah ditutup oleh helper bersarang di
	 * tengah proses, memanggil {@code getTransaction()} akan melempar "Session is closed!" yang
	 * menutupi kesalahan aslinya. Pada akhirnya seluruh kegagalan ditelan dan dicatat ke audit;
	 * method ini tidak melempar ke pemanggil — berbeda dari {@link #reInitTugas(Session)} yang
	 * justru melempar ulang.</p>
	 *
	 * @param session            session Hibernate; {@code null} membuat method kembali tanpa efek.
	 *                           Tidak pernah ditutup oleh method ini
	 * @param bolehUlangSaatLock {@code true} bila pengulangan sekali pada session baru diizinkan
	 *                           ketika kegagalan dikenali sebagai kunci macet
	 */
	@SuppressWarnings("unchecked")
	private void reInitPertemuan(Session session, boolean bolehUlangSaatLock) {
		if (session == null) return;
		
		boolean localTransaction = false;
		
		try {
			if (!session.getTransaction().isActive()) {
				session.beginTransaction();
				localTransaction = true;
			}
			
			List<Long> pertemuansId = session.createCriteria(Pertemuan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.property("id"))
					.addOrder((getUrutkanotomatis() != null && !getUrutkanotomatis()) ? Order.asc("pertemuanKe") : Order.asc("tanggal"))
					.add(Restrictions.isNotNull("tanggal"))
					.addOrder(Order.asc("id"))
					.add(this instanceof Perkuliahan ? Restrictions.eq("perkuliahan", this)
							: this instanceof KelompokKkn ? Restrictions.eq("kelompokKkn", this)
							: this instanceof FormulirKegiatan ? Restrictions.eq("formulirKegiatan", this)
							: this instanceof KelompokPkl ? Restrictions.eq("kelompokPkl", this)
							: this instanceof JadwalUjianPMB ? Restrictions.eq("jadwalUjianPMB", this)
							: this instanceof JadwalUjianPegawai ? Restrictions.eq("jadwalUjianPegawai", this)
							: this instanceof MahasiswaRequestTugasAkhir ? Restrictions.eq("mahasiswaRequestTugasAkhir", this)
							: this instanceof Skripsi ? Restrictions.eq("skripsi", this)
							: this instanceof KomponenDataProdukKursus ? Restrictions.eq("komponenDataProdukKursus", this)
							: this instanceof Wisuda ? Restrictions.eq("wisuda", this)
							: this instanceof KrsMahasiswa ? Restrictions.eq("krsMahasiswa", this)
							: this instanceof JadwalUjianPSB ? Restrictions.eq("jadwalUjianPSB", this)
							: this instanceof JadwalPelajaran ? Restrictions.eq("jadwalPelajaran", this)
							: this instanceof PertemuanPunyaGrupPertemuan ? Restrictions.eq("pertemuanPunyaGrupPertemuan", this)
							: Restrictions.sqlRestriction("false"))
					.list();

			if (pertemuansId == null || pertemuansId.isEmpty()) {
				if (localTransaction) session.getTransaction().commit();
				return;
			}

			List<Pertemuan> pertemuans = ambilDataBanyak(Pertemuan.class, pertemuansId);
			tulisLokasiPertemuan(new JSONObject().toString());
			
			int pertemuanKe = 1;
			for (Pertemuan pertemuan : pertemuans) {
				if (pertemuan != null) {
					session.refresh(pertemuan);
					if (pertemuan.getAktif() != null && pertemuan.getAktif()) {
						VOPembelajaran pembelajaran = pertemuan.ambilVOPembelajaran();
						if (pembelajaran != null && pembelajaran.getUrutkanotomatis() != null && pembelajaran.getUrutkanotomatis()) {
							if (pertemuan.getPertemuanKe() == null || !pertemuan.getPertemuanKe().equals(pertemuanKe)) {
								pertemuan.setPertemuanKe(pertemuanKe);
								session.update(pertemuan);
							}
						}
						pertemuanKe++;
						populatePertemuan(pertemuan);

						pertemuan.reInitPertemuanPunyaDiskusi(session);
						pertemuan.reInitPertemuanPunyaUjian(session);
						pertemuan.reInitTugasPertemuan(session);
						pertemuan.reInitTugasKelompok(session);
						pertemuan.reInitPengajuanIzinTidakMasukPerkuliahan(session);
						pertemuan.reInitKelompokParameterTambahanPertemuan(session);

						Collection<PengajuanIzinTidakMasukPerkuliahan> pengajuanIzinTidakMasukPerkuliahans = pertemuan.ambilPengajuanIzinTidakMasukPerkuliahanTotal();
						if (pengajuanIzinTidakMasukPerkuliahans != null) {
							for (PengajuanIzinTidakMasukPerkuliahan izin : pengajuanIzinTidakMasukPerkuliahans) {
								if (izin != null && izin.getDiizinkan() != null && izin.getDiizinkan()) {
									Mahasiswa mahasiswa = izin.getMahasiswa();
									if (mahasiswa != null) {
										pertemuan.populate(mahasiswa.getId(), izin.getStatusabsensi(),
												izin.getKeterangan(), null,
												pertemuan.retreiveAbsensiMulai(mahasiswa.getId()),
												pertemuan.retreiveAbsensiSampai(mahasiswa.getId()), "Mahasiswa");
										Common.refreshUpdate(session, pertemuan);
									}
								}
							}
							pengajuanIzinTidakMasukPerkuliahans.clear();
							pengajuanIzinTidakMasukPerkuliahans = null;
						}

						Session sessionSreaming = null;
						try {
							sessionSreaming = ais.database.hibernate.StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
							pertemuan.reInitPertemuanFileContent(sessionSreaming);
							pertemuan.reInitTugasFileContent(sessionSreaming);
							pertemuan.reInitVideoPertemuan(sessionSreaming);
							pertemuan.reInitAudioPertemuan(sessionSreaming);
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOPembelajaran.java:671");
						} finally {
							if (sessionSreaming != null) {
								try { if (sessionSreaming.isOpen()) sessionSreaming.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:674");}
								try { sessionSreaming.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:675");}
								try { if (sessionSreaming.isOpen()) sessionSreaming.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:676");}
							}
						}

						GeneralValueObject.masukkanData(Pertemuan.class, pertemuan);
					}
				}
			}
			
			pertemuans.clear();
			pertemuans = null;
			pertemuansId.clear();
			pertemuansId = null;

			if (localTransaction && session.isOpen()) {
				session.getTransaction().commit();
			}

		} catch (Exception e) {
			// Guard session.isOpen(): bila session sudah ditutup (mis. helper nested memanggil
			// closeSession di tengah proses), memanggil getTransaction() di sini akan melempar
			// "Session is closed!" yang MENUTUPI error asli. Cek isOpen dulu.
			if (localTransaction && session.isOpen() && session.getTransaction().isActive()) {
				try { session.getTransaction().rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:699");}
			}
			/* Penyusunan ulang nomor pertemuan dapat berbarengan dengan absensi atau
			 * sinkronisasi kelas yang mengubah row sama. PostgreSQL membatalkan transaksi
			 * dengan lock_timeout. Ulangi seluruh unit kerja sekali pada Session baru;
			 * transaksi yang gagal tidak boleh dipakai kembali. */
			if (bolehUlangSaatLock && adalahLockTimeoutPertemuan(e)) {
				Session sessionUlang = null;
				try {
					try { Thread.sleep(200L); } catch (InterruptedException terputus) {
						Thread.currentThread().interrupt();
					}
					sessionUlang = HibernateUtil.getSessionFactory().openSession();
					reInitPertemuan(sessionUlang, false);
					return;
				} catch (Exception ulangGagal) {
					ais.common.ErrorAuditUtil.record(ulangGagal,
							"retry reInitPertemuan setelah lock timeout");
				} finally {
					if (sessionUlang != null) {
						try { if (sessionUlang.isOpen()) sessionUlang.clear(); } catch (Exception abaikan) { }
						try { sessionUlang.disconnect(); } catch (Exception abaikan) { }
						try { if (sessionUlang.isOpen()) sessionUlang.close(); } catch (Exception abaikan) { }
					}
				}
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOPembelajaran.java:701");
		}
	}

	/**
	 * Menentukan apakah suatu kegagalan berasal dari kunci basis data yang macet, sehingga layak
	 * diulang.
	 *
	 * <p>Menelusuri seluruh rantai penyebab kesalahan dari luar ke dalam dan mencocokkan pesan tiap
	 * tingkat — dalam huruf kecil — terhadap empat penanda: {@code "lock timeout"},
	 * {@code "canceling statement due to lock timeout"}, {@code "deadlock detected"}, dan
	 * {@code "sqlstate: 40p01"}. Penelusuran rantai diperlukan karena kegagalan basis data
	 * biasanya sudah terbungkus beberapa lapis pengecualian Hibernate sebelum sampai ke
	 * pemanggil.</p>
	 *
	 * <p><b>Pencocokan berbasis teks pesan, bukan kode kesalahan.</b> Pendekatan ini bekerja untuk
	 * PostgreSQL berbahasa Inggris, tetapi akan gagal mengenali kondisi yang sama bila server
	 * dikonfigurasi dengan pesan berbahasa lain, bila driver mengubah susunan pesannya, atau bila
	 * basis data lain dipakai. Kegagalan mengenali berarti pengulangan tidak dijalankan dan
	 * sinkronisasi sekadar gagal — tidak ada kerusakan data, hanya indeks yang tertinggal
	 * sebagian. Penanda {@code sqlstate: 40p01} adalah kode {@code deadlock_detected} milik
	 * PostgreSQL dan merupakan satu-satunya penanda yang tidak bergantung bahasa.</p>
	 *
	 * <p>Pesan yang {@code null} pada suatu tingkat dilewati; penelusuran berhenti ketika rantai
	 * penyebab habis.</p>
	 *
	 * @param error kesalahan yang diperiksa; {@code null} menghasilkan {@code false}
	 * @return {@code true} bila salah satu tingkat rantai penyebab menyebut kunci macet atau
	 *         kebuntuan
	 */
	private static boolean adalahLockTimeoutPertemuan(Throwable error) {
		Throwable cek = error;
		while (cek != null) {
			String pesan = cek.getMessage();
			if (pesan != null) {
				String kecil = pesan.toLowerCase();
				if (kecil.indexOf("lock timeout") >= 0
						|| kecil.indexOf("canceling statement due to lock timeout") >= 0
						|| kecil.indexOf("deadlock detected") >= 0
						|| kecil.indexOf("sqlstate: 40p01") >= 0) {
					return true;
				}
			}
			cek = cek.getCause();
		}
		return false;
	}

	/**
	 * Membangun ulang data <b>tugas</b> seluruh pertemuan milik objek pembelajaran ini dari basis
	 * data.
	 *
	 * <p>Saudara sempit dari {@link #reInitPertemuan(Session)}: memakai rantai restriksi dan
	 * penomoran ulang yang sama persis, tetapi hanya menyinkronkan tugas pertemuan dan tugas
	 * kelompok — tanpa diskusi, ujian, izin tidak masuk, parameter tambahan, maupun berkas media.
	 * Dipakai ketika yang berubah memang hanya sisi penugasan, sehingga jauh lebih murah daripada
	 * sinkronisasi penuh.</p>
	 *
	 * <p><b>Berbeda dari saudaranya, method ini melempar ulang kegagalan</b> setelah mencatatnya
	 * (dibungkus {@link RuntimeException} bila belum berupa pengecualian tak-tercentang).
	 * {@link #reInitPertemuan(Session)} menelan kegagalannya. Pemanggil yang menjalankan keduanya
	 * dalam satu alur harus siap bahwa hanya yang ini yang dapat menghentikan alurnya.</p>
	 *
	 * <p>Sama seperti saudaranya, method ini bukan operasi baca: ia membuka transaksi, menulis
	 * ulang nomor pertemuan, dan menimpa indeks pertemuan.</p>
	 *
	 * @param session session Hibernate; {@code null} membuat method kembali tanpa efek
	 */
	@SuppressWarnings("unchecked")
	public void reInitTugas(Session session) {
		reInitTugas(session, true);
	}

	/**
	 * Pelaksana sinkronisasi tugas, dengan bendera yang mengendalikan pengulangan saat kunci basis
	 * data macet.
	 *
	 * <h4>Kesamaan dengan {@link #reInitPertemuan(Session, boolean)}</h4>
	 * <p>Rantai restriksi {@code instanceof} empat belas cabangnya identik baris demi baris,
	 * termasuk cabang penutup {@code Restrictions.sqlRestriction("false")} — sehingga
	 * {@link GrupPertemuan}, {@code sekolah.JadwalPertemuanPSB}, dan
	 * {@code sekolah.KelasLesSiswa} sama-sama tidak terjangkau di sini. Urutan kueri, penulisan
	 * ulang {@code pertemuanKe} untuk wadah berpenomoran otomatis, dan penimpaan indeks pertemuan
	 * juga sama. Setiap perubahan pada salah satu mesin harus diterapkan pada ketiganya.</p>
	 *
	 * <h4>Perbedaan yang perlu diketahui</h4>
	 * <ul>
	 * <li><b>Cakupan sinkronisasi jauh lebih sempit</b> — hanya {@code reInitTugasPertemuan} dan
	 * {@code reInitTugasKelompok} per pertemuan. Tidak ada session streaming, tidak ada penerapan
	 * izin tidak masuk.</li>
	 * <li><b>Melempar ulang.</b> Setelah rollback dan pencatatan, kegagalan dilempar kembali ke
	 * pemanggil alih-alih ditelan.</li>
	 * <li><b>Syarat pengulangan lebih ketat.</b> Pengulangan hanya dijalankan bila transaksinya
	 * memang dibuka oleh method ini ({@code localTransaction}); pada
	 * {@link #reInitPertemuan(Session, boolean)} syarat itu tidak ada. Artinya, ketika dipanggil
	 * dari dalam transaksi milik pemanggil, kegagalan karena kunci macet di sini langsung
	 * dilempar tanpa dicoba ulang — pilihan yang benar, karena transaksi milik pemanggil memang
	 * tidak boleh diulang diam-diam.</li>
	 * <li><b>Jeda pengulangan 250 milidetik</b>, sedikit lebih lama daripada 200 milidetik pada
	 * saudaranya. Session baru dibuka lewat {@code HibernateUtil.openSession()}, bukan lewat
	 * factory langsung.</li>
	 * <li><b>Blok pengulangan tidak punya {@code catch}</b>, hanya {@code finally}. Bila
	 * percobaan kedua ikut gagal, kegagalannya diteruskan apa adanya ke pemanggil — bukan dicatat
	 * lalu diabaikan seperti pada {@link #reInitPertemuan(Session, boolean)}.</li>
	 * <li><b>Rollback tidak dijaga {@code session.isOpen()}.</b> Penjaga yang sengaja ditambahkan
	 * di {@link #reInitPertemuan(Session, boolean)} — agar "Session is closed!" tidak menutupi
	 * kesalahan aslinya ketika helper bersarang menutup session di tengah proses — tidak ada di
	 * sini. Bila kondisi itu terjadi, pemanggil akan menerima pesan tentang session tertutup
	 * alih-alih penyebab sebenarnya. Hal yang sama berlaku pada pemanggilan {@code commit} di
	 * jalur berhasil.</li>
	 * </ul>
	 *
	 * @param session    session Hibernate; {@code null} membuat method kembali tanpa efek. Tidak
	 *                   pernah ditutup oleh method ini
	 * @param bolehUlang {@code true} bila pengulangan sekali pada session baru diizinkan; hanya
	 *                   berlaku bila transaksinya dibuka oleh method ini
	 */
	@SuppressWarnings("unchecked")
	private void reInitTugas(Session session, boolean bolehUlang) {
		if (session == null) return;
		
		boolean localTransaction = false;
		
		try {
			if (!session.getTransaction().isActive()) {
				session.beginTransaction();
				localTransaction = true;
			}
			
			List<Long> pertemuansId = session.createCriteria(Pertemuan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.property("id"))
					.addOrder((getUrutkanotomatis() != null && !getUrutkanotomatis()) ? Order.asc("pertemuanKe") : Order.asc("tanggal"))
					.add(Restrictions.isNotNull("tanggal"))
					.addOrder(Order.asc("id"))
					.add(this instanceof Perkuliahan ? Restrictions.eq("perkuliahan", this)
							: this instanceof KelompokKkn ? Restrictions.eq("kelompokKkn", this)
							: this instanceof FormulirKegiatan ? Restrictions.eq("formulirKegiatan", this)
							: this instanceof KelompokPkl ? Restrictions.eq("kelompokPkl", this)
							: this instanceof JadwalUjianPMB ? Restrictions.eq("jadwalUjianPMB", this)
							: this instanceof JadwalUjianPegawai ? Restrictions.eq("jadwalUjianPegawai", this)
							: this instanceof MahasiswaRequestTugasAkhir ? Restrictions.eq("mahasiswaRequestTugasAkhir", this)
							: this instanceof Skripsi ? Restrictions.eq("skripsi", this)
							: this instanceof KomponenDataProdukKursus ? Restrictions.eq("komponenDataProdukKursus", this)
							: this instanceof Wisuda ? Restrictions.eq("wisuda", this)
							: this instanceof KrsMahasiswa ? Restrictions.eq("krsMahasiswa", this)
							: this instanceof JadwalUjianPSB ? Restrictions.eq("jadwalUjianPSB", this)
							: this instanceof JadwalPelajaran ? Restrictions.eq("jadwalPelajaran", this)
							: this instanceof PertemuanPunyaGrupPertemuan ? Restrictions.eq("pertemuanPunyaGrupPertemuan", this)
							: Restrictions.sqlRestriction("false"))
					.list();

			if (pertemuansId == null || pertemuansId.isEmpty()) {
				if (localTransaction) session.getTransaction().commit();
				return;
			}

			List<Pertemuan> pertemuans = ambilDataBanyak(Pertemuan.class, pertemuansId);
			tulisLokasiPertemuan(new JSONObject().toString());
			
			int pertemuanKe = 1;
			for (Pertemuan pertemuan : pertemuans) {
				if (pertemuan != null) {
					session.refresh(pertemuan);
					if (pertemuan.getAktif() != null && pertemuan.getAktif()) {
						VOPembelajaran pembelajaran = pertemuan.ambilVOPembelajaran();
						if (pembelajaran != null && pembelajaran.getUrutkanotomatis() != null && pembelajaran.getUrutkanotomatis()) {
							if (pertemuan.getPertemuanKe() == null || !pertemuan.getPertemuanKe().equals(pertemuanKe)) {
								pertemuan.setPertemuanKe(pertemuanKe);
								session.update(pertemuan);
							}
						}
						pertemuanKe++;
						populatePertemuan(pertemuan);

						pertemuan.reInitTugasPertemuan(session);
						pertemuan.reInitTugasKelompok(session);

						GeneralValueObject.masukkanData(Pertemuan.class, pertemuan);
					}
				}
			}
			
			pertemuans.clear();
			pertemuans = null;
			pertemuansId.clear();
			pertemuansId = null;

			if (localTransaction) {
				session.getTransaction().commit();
			}

		} catch (Exception e) {
			if (localTransaction && session.getTransaction().isActive()) {
				try { session.getTransaction().rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:782");}
			}
			if (localTransaction && bolehUlang && adalahLockTimeoutPertemuan(e)) {
				Session sessionUlang = null;
				try {
					try { Thread.sleep(250L); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
					sessionUlang = HibernateUtil.openSession();
					reInitTugas(sessionUlang, false);
					return;
				} finally {
					if (sessionUlang != null) {
						try { sessionUlang.clear(); } catch (Exception abaikan) { }
						try { if (sessionUlang.isConnected()) sessionUlang.disconnect(); } catch (Exception abaikan) { }
						try { if (sessionUlang.isOpen()) sessionUlang.close(); } catch (Exception abaikan) { }
					}
				}
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOPembelajaran.java:784");
			throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
		}
	}

	/**
	 * Menyinkronkan (membangun ulang) SELURUH data UJIAN milik objek pembelajaran ini —
	 * mencakup DAFTAR ujian per pertemuan, DATA PESERTA ujian, serta SOAL-SOAL ujian — lalu
	 * menuliskan hasilnya ke "tabel flag" (flag store berbasis berkas dengan cermin basis data
	 * melalui {@link ais.common.BacaTulisUtil}). Method ini adalah mesin di balik tombol toolbar
	 * "Syn.Ujian" pada halaman Manajemen Jadwal Perkuliahan, sekaligus dipanggil oleh proses
	 * "Singkronkan" (sinkronisasi menyeluruh) agar ringkasan jumlah peserta, status
	 * keikutsertaan, dan ketersediaan soal selalu konsisten dengan kondisi sebenarnya di basis
	 * data.
	 *
	 * <p><b>Latar belakang.</b> Sistem menyimpan relasi berat (pertemuan, ujian, peserta, dan
	 * soal) di dalam flag store agar pembacaan pada grid/laporan tidak perlu melakukan query
	 * berat berulang-ulang. Flag store tersebut bisa menjadi usang bila data diubah dari jalur
	 * lain (impor, ujian remedial, penggantian bank soal, pembatalan keikutsertaan, penambahan
	 * soal, dsb.). Tanpa proses sinkronisasi eksplisit, kolom "Jml Mhs", ringkasan
	 * keikutsertaan ujian, atau daftar soal dapat menampilkan angka lama. Method ini membangun
	 * ulang flag store dari sumber kebenaran (basis data) sehingga tampilannya kembali akurat.
	 *
	 * <p><b>Alur kerja.</b> (1) Bila belum ada transaksi aktif pada {@code session}, method
	 * membuka transaksi lokal sendiri dan bertanggung jawab melakukan commit/rollback — sehingga
	 * aman dipanggil baik dari {@code SyncHelper} (yang menyediakan session terisolasi per-thread
	 * tanpa transaksi) maupun dari alur lain yang sudah memegang transaksi. (2) Mengambil seluruh
	 * id {@link Pertemuan} AKTIF (aktif = null/true) yang bertanggal dan merujuk ke objek
	 * pembelajaran ini (Perkuliahan/KelompokKkn/Skripsi/JadwalPelajaran, dst.), memakai restriksi
	 * tipe yang identik dengan {@link #reInitPertemuan(Session)} dan {@link #reInitTugas(Session)}.
	 * (3) Untuk setiap pertemuan aktif: memuat instance-nya, memanggil
	 * {@link Pertemuan#reInitPertemuanPunyaUjian(Session)} untuk membangun ulang DAFTAR ujian
	 * (PertemuanPunyaUjian) pada pertemuan tersebut, lalu mengiterasi setiap
	 * {@link PertemuanPunyaUjian} untuk (a) membangun ulang DATA PESERTA melalui
	 * {@link PertemuanPunyaUjian#reInitHasilUjianMahasiswa(Session)} dan (b) membangun ulang
	 * SOAL-SOAL ujian melalui {@link Ujian#reInitUjianPunyaSoal(Session)} pada objek {@link Ujian}
	 * terkait. Setiap entitas yang telah diproses dicatat ke cache data global
	 * ({@link GeneralValueObject#masukkanData}). (4) Menutup transaksi lokal (commit) bila dibuka
	 * oleh method ini.
	 *
	 * <p><b>Manajemen memori &amp; session.</b> Koleksi id dan entitas dibebaskan (clear + null)
	 * setelah dipakai untuk menekan jejak memori saat sinkronisasi massal ribuan kelas secara
	 * paralel. Method ini TIDAK menutup {@code session} yang diberikan; penutupan session tetap
	 * tanggung jawab pemanggil ({@code SyncHelper} menutup session per-thread di blok finally-nya).
	 * Bila terjadi kegagalan dan transaksi lokal masih aktif, dilakukan rollback agar tidak
	 * meninggalkan transaksi menggantung. Kegagalan ditelan secara terkendali (dicetak ke log)
	 * agar satu kelas bermasalah tidak menghentikan sinkronisasi kelas lain pada thread pool.
	 *
	 * <p><b>Idempoten.</b> Pemanggilan berulang menghasilkan keadaan flag store yang sama: setiap
	 * sub-proses selalu membersihkan lokasi flag terlebih dahulu (bersihkan/tulisLokasi kosong)
	 * sebelum menuliskan ulang dari basis data, sehingga tidak terjadi duplikasi maupun sisa data
	 * usang.
	 *
	 * @param session session Hibernate untuk query &amp; penulisan flag; bila {@code null} method
	 *                langsung keluar tanpa efek.
	 */
	public void reInitUjian(Session session) {
		if (session == null) return;

		boolean localTransaction = false;

		try {
			if (!session.getTransaction().isActive()) {
				session.beginTransaction();
				localTransaction = true;
			}

			List<Long> pertemuansId = session.createCriteria(Pertemuan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.property("id"))
					.add(Restrictions.isNotNull("tanggal"))
					.addOrder(Order.asc("id"))
					.add(this instanceof Perkuliahan ? Restrictions.eq("perkuliahan", this)
							: this instanceof KelompokKkn ? Restrictions.eq("kelompokKkn", this)
							: this instanceof FormulirKegiatan ? Restrictions.eq("formulirKegiatan", this)
							: this instanceof KelompokPkl ? Restrictions.eq("kelompokPkl", this)
							: this instanceof JadwalUjianPMB ? Restrictions.eq("jadwalUjianPMB", this)
							: this instanceof JadwalUjianPegawai ? Restrictions.eq("jadwalUjianPegawai", this)
							: this instanceof MahasiswaRequestTugasAkhir ? Restrictions.eq("mahasiswaRequestTugasAkhir", this)
							: this instanceof Skripsi ? Restrictions.eq("skripsi", this)
							: this instanceof KomponenDataProdukKursus ? Restrictions.eq("komponenDataProdukKursus", this)
							: this instanceof Wisuda ? Restrictions.eq("wisuda", this)
							: this instanceof KrsMahasiswa ? Restrictions.eq("krsMahasiswa", this)
							: this instanceof JadwalUjianPSB ? Restrictions.eq("jadwalUjianPSB", this)
							: this instanceof JadwalPelajaran ? Restrictions.eq("jadwalPelajaran", this)
							: this instanceof PertemuanPunyaGrupPertemuan ? Restrictions.eq("pertemuanPunyaGrupPertemuan", this)
							: Restrictions.sqlRestriction("false"))
					.list();

			if (pertemuansId == null || pertemuansId.isEmpty()) {
				if (localTransaction) session.getTransaction().commit();
				return;
			}

			List<Pertemuan> pertemuans = ambilDataBanyak(Pertemuan.class, pertemuansId);

			for (Pertemuan pertemuan : pertemuans) {
				if (pertemuan != null) {
					session.refresh(pertemuan);
					if (pertemuan.getAktif() != null && pertemuan.getAktif()) {

						// (a) Bangun ulang DAFTAR ujian (PertemuanPunyaUjian) pada pertemuan ini
						pertemuan.reInitPertemuanPunyaUjian(session);

						// (b) Untuk tiap ujian pada pertemuan: sinkron PESERTA + SOAL
						List<PertemuanPunyaUjian> pertemuanPunyaUjians = session
								.createCriteria(PertemuanPunyaUjian.class)
								.add(Restrictions.eq("pertemuan", pertemuan)).addOrder(Order.asc("id")).list();
						for (PertemuanPunyaUjian pertemuanPunyaUjian : pertemuanPunyaUjians) {
							if (pertemuanPunyaUjian == null) {
								continue;
							}

							// Peserta ujian (HasilUjianMahasiswa) -> flag store
							pertemuanPunyaUjian.reInitHasilUjianMahasiswa(session);

							// Soal-soal ujian (UjianPunyaSoal) pada objek Ujian terkait -> flag store
							Ujian ujian = pertemuanPunyaUjian.getUjian();
							if (ujian != null) {
								ujian.reInitUjianPunyaSoal(session);
							}

							GeneralValueObject.masukkanData(PertemuanPunyaUjian.class, pertemuanPunyaUjian);
						}

						GeneralValueObject.masukkanData(Pertemuan.class, pertemuan);
					}
				}
			}

			pertemuans.clear();
			pertemuans = null;
			pertemuansId.clear();
			pertemuansId = null;

			if (localTransaction) {
				session.getTransaction().commit();
			}

		} catch (Exception e) {
			if (localTransaction && session.getTransaction().isActive()) {
				try { session.getTransaction().rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:924");}
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOPembelajaran.java:926");
		}
	}



	/**
	 * Menyebutkan peran seorang {@link Dosen} pada objek pembelajaran ini — "Dosen Utama",
	 * "Pembimbing ke-2", "Ketua Sidang", dan seterusnya — sebagai teks siap tampil.
	 *
	 * <p>Dipakai layar dan laporan yang menampilkan daftar pengajar beserta kedudukannya.
	 * Perannya dipilih lewat rantai {@code instanceof} atas {@code this}, karena tiap jenis wadah
	 * menyimpan pengajarnya di kolom yang berbeda dan menamai perannya secara berbeda pula.</p>
	 *
	 * <h4>Cakupan tiap cabang</h4>
	 * <ul>
	 * <li>{@link KrsMahasiswa} — satu peran: "Dosen Pembimbing Akademik".</li>
	 * <li>{@link Perkuliahan} — sepuluh slot, dilaporkan "Dosen Utama" lalu "Dosen ke-2" sampai
	 * "Dosen ke-10".</li>
	 * <li>{@code kkn.KelompokKkn} dan {@code pkl.KelompokPkl} — "Pembimbing Utama" lalu
	 * "Pembimbing ke-2" sampai "Pembimbing ke-5". <b>Hanya lima slot pertama yang diperiksa</b>,
	 * padahal {@link #populateDosenBuNama()} mengenali sepuluh slot pada kedua wadah tersebut.
	 * Pembimbing keenam sampai kesepuluh karenanya muncul di daftar pengajar tetapi dilaporkan di
	 * sini sebagai bukan pembimbing.</li>
	 * <li>{@link Skripsi} dan {@link MahasiswaRequestTugasAkhir} — nama perannya tidak ditanam di
	 * program melainkan diambil dari master format nilai, sehingga institusi dapat menamai sendiri
	 * susunan sidangnya. Bila master formatnya belum diisi, cabangnya tidak menghasilkan apa pun.</li>
	 * <li>{@link GrupPertemuan} dan {@link PertemuanPunyaGrupPertemuan} — satu peran, "Dosen
	 * Pembimbing" ditambah jenis grupnya.</li>
	 * <li>{@link FormulirKegiatan} — sengaja mengembalikan teks kosong, bukan pesan penolakan.</li>
	 * </ul>
	 *
	 * <h4>Dua cabang yang mengabaikan argumennya</h4>
	 * <p><b>Pada {@link Skripsi} dan {@link MahasiswaRequestTugasAkhir}, argumen {@code dosen}
	 * tidak pernah dibandingkan dengan siapa pun.</b> Kedua cabang hanya memeriksa slot peran mana
	 * yang terisi, lalu mengembalikan label slot terisi <i>pertama</i> — tanpa memastikan bahwa
	 * dosen yang ditanyakan memang menempati slot itu. Akibatnya, memanggil method ini untuk dosen
	 * mana pun pada sebuah sidang skripsi menghasilkan jawaban yang sama, yaitu peran ketua sidang
	 * (atau peran pertama yang kebetulan terisi). Seluruh cabang lain melakukan pembandingan id
	 * dengan benar. Bila peran yang akurat dibutuhkan pada kedua wadah tersebut, bandingkan sendiri
	 * terhadap slot-slotnya alih-alih mengandalkan method ini.</p>
	 *
	 * <h4>Nilai balik ketika tidak cocok</h4>
	 * <p>Tiga keadaan sama-sama berujung pada teks {@code "Bukan dosen pengajar / pembimbing"}:
	 * dosen memang tidak menempati slot mana pun, wadahnya termasuk cabang yang dikenal tetapi
	 * datanya belum lengkap, dan <b>wadahnya tidak terdaftar sama sekali</b> pada rantai ini —
	 * termasuk {@code sekolah.JadwalPelajaran}, {@link Wisuda}, {@link JadwalUjianPMB}, dan
	 * seluruh wadah berbasis guru. Untuk wadah jenis terakhir, jawaban itu menyesatkan: bukan
	 * berarti dosennya bukan pengajar, melainkan bahwa pertanyaannya tidak berlaku. Pemanggil yang
	 * menampilkan teks ini apa adanya perlu memeriksa lebih dulu apakah wadahnya memang memakai
	 * dosen.</p>
	 *
	 * <p>Argumen {@code null} atau dosen tanpa id menghasilkan teks kosong. Tidak ada penangkap
	 * kesalahan: rantai pembacaan seperti {@code getDosen1().getId()} sudah dijaga pemeriksaan
	 * {@code null} pada objeknya, tetapi id yang {@code null} pada slot akan melempar ke
	 * pemanggil.</p>
	 *
	 * @param dosen dosen yang ditanyakan perannya; {@code null} atau tanpa id menghasilkan
	 *              {@code ""}
	 * @return nama peran siap tampil, {@code ""}, atau
	 *         {@code "Bukan dosen pengajar / pembimbing"}; tidak pernah {@code null}
	 */
	public String infoDosen(Dosen dosen) {
		if (dosen == null || dosen.getId() == null) {
			return "";
		} else if (this instanceof KrsMahasiswa) {
			KrsMahasiswa krsMahasiswa = (KrsMahasiswa) this;
			if (krsMahasiswa != null && krsMahasiswa.getDosenPa() != null
					&& krsMahasiswa.getDosenPa().getId().equals(dosen.getId())) {
				return "Dosen Pembimbing Akademik";
			}
		} else if (this instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) this;
			if (perkuliahan.getDosen1() != null && perkuliahan.getDosen1().getId().equals(dosen.getId())) {
				return "Dosen Utama";
			} else if (perkuliahan.getDosen2() != null && perkuliahan.getDosen2().getId().equals(dosen.getId())) {
				return "Dosen ke-2";
			} else if (perkuliahan.getDosen3() != null && perkuliahan.getDosen3().getId().equals(dosen.getId())) {
				return "Dosen ke-3";
			} else if (perkuliahan.getDosen4() != null && perkuliahan.getDosen4().getId().equals(dosen.getId())) {
				return "Dosen ke-4";
			} else if (perkuliahan.getDosen5() != null && perkuliahan.getDosen5().getId().equals(dosen.getId())) {
				return "Dosen ke-5";
			} else if (perkuliahan.getDosen6() != null && perkuliahan.getDosen6().getId().equals(dosen.getId())) {
				return "Dosen ke-6";
			} else if (perkuliahan.getDosen7() != null && perkuliahan.getDosen7().getId().equals(dosen.getId())) {
				return "Dosen ke-7";
			} else if (perkuliahan.getDosen8() != null && perkuliahan.getDosen8().getId().equals(dosen.getId())) {
				return "Dosen ke-8";
			} else if (perkuliahan.getDosen9() != null && perkuliahan.getDosen9().getId().equals(dosen.getId())) {
				return "Dosen ke-9";
			} else if (perkuliahan.getDosen10() != null && perkuliahan.getDosen10().getId().equals(dosen.getId())) {
				return "Dosen ke-10";
			}
		} else if (this instanceof KelompokKkn) {
			KelompokKkn kelompokKkn = (KelompokKkn) this;
			if (kelompokKkn.getDosen_pembimbing1() != null
					&& kelompokKkn.getDosen_pembimbing1().getId().equals(dosen.getId())) {
				return "Pembimbing Utama";
			} else if (kelompokKkn.getDosen_pembimbing2() != null
					&& kelompokKkn.getDosen_pembimbing2().getId().equals(dosen.getId())) {
				return "Pembimbing ke-2";
			} else if (kelompokKkn.getDosen_pembimbing3() != null
					&& kelompokKkn.getDosen_pembimbing3().getId().equals(dosen.getId())) {
				return "Pembimbing ke-3";
			} else if (kelompokKkn.getDosen_pembimbing4() != null
					&& kelompokKkn.getDosen_pembimbing4().getId().equals(dosen.getId())) {
				return "Pembimbing ke-4";
			} else if (kelompokKkn.getDosen_pembimbing5() != null
					&& kelompokKkn.getDosen_pembimbing5().getId().equals(dosen.getId())) {
				return "Pembimbing ke-5";
			}
		} else if (this instanceof KelompokPkl) {
			KelompokPkl kelompokPkl = (KelompokPkl) this;
			if (kelompokPkl.getDosen_pembimbing1() != null
					&& kelompokPkl.getDosen_pembimbing1().getId().equals(dosen.getId())) {
				return "Pembimbing Utama";
			} else if (kelompokPkl.getDosen_pembimbing2() != null
					&& kelompokPkl.getDosen_pembimbing2().getId().equals(dosen.getId())) {
				return "Pembimbing ke-2";
			} else if (kelompokPkl.getDosen_pembimbing3() != null
					&& kelompokPkl.getDosen_pembimbing3().getId().equals(dosen.getId())) {
				return "Pembimbing ke-3";
			} else if (kelompokPkl.getDosen_pembimbing4() != null
					&& kelompokPkl.getDosen_pembimbing4().getId().equals(dosen.getId())) {
				return "Pembimbing ke-4";
			} else if (kelompokPkl.getDosen_pembimbing5() != null
					&& kelompokPkl.getDosen_pembimbing5().getId().equals(dosen.getId())) {
				return "Pembimbing ke-5";
			}
		} else if (this instanceof Skripsi) {
			Skripsi skripsi = (Skripsi) this;
			FormatNilaiSkripsi f = skripsi.getFormatNilaiSkripsi();
			if (f != null) {
				if (skripsi.getKetuaSidang() != null) {
					return f.getDosen1();
				}
				if (skripsi.getPembimbing() != null) {
					return f.getDosen2();
				}

				if (skripsi.getPembimbing3() != null) {
					return "Pembimbing III";
				}

				if (skripsi.getPenguji1() != null) {
					return f.getDosen3();
				}
				if (skripsi.getPenguji2() != null) {
					return f.getDosen4();
				}
				if (skripsi.getPenguji3() != null) {
					return f.getDosen5();
				}
				if (skripsi.getPenguji4() != null) {
					return f.getDosen6();
				}
			}
		} else if (this instanceof MahasiswaRequestTugasAkhir) {
			MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) this;
			FormatNilaiProposalSkripsi f = mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi();
			if (f != null) {
				if (mahasiswaRequestTugasAkhir.getDosen1() != null) {
					return f.getDosen1();
				}
				if (mahasiswaRequestTugasAkhir.getDosen2() != null) {
					return f.getDosen2();
				}

				if (mahasiswaRequestTugasAkhir.getDosen3() != null) {
					return f.getDosen3();
				}

				if (mahasiswaRequestTugasAkhir.getDosen4() != null) {
					return f.getDosen4();
				}

				if (mahasiswaRequestTugasAkhir.getDosen5() != null) {
					return f.getDosen5();
				}
				if (mahasiswaRequestTugasAkhir.getDosen6() != null) {
					return f.getDosen6();
				}
			}
		} else if (this instanceof GrupPertemuan) {
			GrupPertemuan grupPertemuan = (GrupPertemuan) this;
			if (grupPertemuan != null && grupPertemuan.getDosen() != null
					&& grupPertemuan.getDosen().getId().equals(dosen.getId())) {
				return "Dosen Pembimbing " + grupPertemuan.getJenis();
			}
		} else if (this instanceof PertemuanPunyaGrupPertemuan) {
			PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan = (PertemuanPunyaGrupPertemuan) this;
			if (pertemuanPunyaGrupPertemuan != null && pertemuanPunyaGrupPertemuan.getGrupPertemuan() != null
					&& pertemuanPunyaGrupPertemuan.getGrupPertemuan().getDosen() != null
					&& pertemuanPunyaGrupPertemuan.getGrupPertemuan().getDosen().getId().equals(dosen.getId())) {
				return "Dosen Pembimbing " + pertemuanPunyaGrupPertemuan.getGrupPertemuan().getJenis();
			}
		} else if (this instanceof FormulirKegiatan) {
			return "";
		}

		return "Bukan dosen pengajar / pembimbing";
	}

	/**
	 * Menyusun keterangan ringkas wadah pembelajaran <b>dari sisi sebuah pertemuan</b>.
	 *
	 * <p>Berbeda dari {@link #infoSimple()} yang bekerja atas {@code this}, method statis ini
	 * menerima {@link Pertemuan} dan menelusuri relasi pemiliknya untuk menemukan wadah yang
	 * tepat, lalu memanggil {@link #infoSimple()} pada wadah tersebut. Dipakai ketika kode hanya
	 * memegang pertemuan — mis. saat menyusun judul undangan kalender atau baris log — dan tidak
	 * tahu wadah mana yang menaunginya.</p>
	 *
	 * <p>Nilai awalnya adalah {@code pertemuan.info()}, keterangan bawaan milik pertemuan itu
	 * sendiri; ia baru diganti bila salah satu dari sebelas relasi pemilik yang diperiksa terisi.
	 * Rantai {@code else if} berhenti pada relasi pertama yang tidak kosong, sehingga pertemuan
	 * yang punya lebih dari satu relasi terisi akan dilaporkan menurut relasi yang lebih dulu
	 * diperiksa — urutannya perkuliahan, KRS, jadwal ujian PMB, permohonan tugas akhir, kelompok
	 * KKN, kelompok PKL, skripsi, jadwal ujian PSB, jadwal pelajaran, pertemuan grup, lalu
	 * formulir kegiatan.</p>
	 *
	 * <p><b>Empat wadah tidak diperiksa di sini</b> — {@link Wisuda},
	 * {@code sekolah.KelasLesSiswa}, {@code sekolah.JadwalPertemuanPSB}, dan
	 * {@link GrupPertemuan} — sehingga pertemuannya selalu jatuh ke keterangan bawaan
	 * {@code pertemuan.info()}. Bandingkan dengan {@link #masukkanPertemuanLocal} yang justru
	 * mengenali tiga di antaranya; cakupan kedua rantai memang tidak sama.</p>
	 *
	 * <p>Argumen {@code null} melempar {@link NullPointerException} pada pemanggilan
	 * {@code pertemuan.info()} — tidak ada penjaga maupun penangkap kesalahan di method ini.</p>
	 *
	 * @param pertemuan pertemuan yang wadahnya dicari; tidak boleh {@code null}
	 * @return keterangan ringkas wadahnya, atau keterangan bawaan pertemuan bila wadahnya tidak
	 *         dikenali
	 */
	public static String infoSimple(Pertemuan pertemuan) {
		String key = pertemuan.info();
		if (pertemuan.getPerkuliahan() != null) {
			key = pertemuan.getPerkuliahan().infoSimple();
		} else if (pertemuan.getKrsMahasiswa() != null) {
			key = pertemuan.getKrsMahasiswa().infoSimple();
		} else if (pertemuan.getJadwalUjianPMB() != null) {
			key = pertemuan.getJadwalUjianPMB().infoSimple();
		} else if (pertemuan.getMahasiswaRequestTugasAkhir() != null) {
			key = pertemuan.getMahasiswaRequestTugasAkhir().infoSimple();
		} else if (pertemuan.getKelompokKkn() != null) {
			key = pertemuan.getKelompokKkn().infoSimple();
		} else if (pertemuan.getKelompokPkl() != null) {
			key = pertemuan.getKelompokPkl().infoSimple();
		} else if (pertemuan.getSkripsi() != null) {
			key = pertemuan.getSkripsi().infoSimple();
		} else if (pertemuan.getJadwalUjianPSB() != null) {
			key = pertemuan.getJadwalUjianPSB().infoSimple();
		} else if (pertemuan.getJadwalPelajaran() != null) {
			key = pertemuan.getJadwalPelajaran().infoSimple();
		} else if (pertemuan.getPertemuanPunyaGrupPertemuan() != null) {
			key = pertemuan.getPertemuanPunyaGrupPertemuan().infoSimple();
		} else if (pertemuan.getFormulirKegiatan() != null) {
			key = pertemuan.getFormulirKegiatan().infoSimple();
		}
		return key;
	}

	/**
	 * Mengumpulkan alamat surel <b>penyelenggara</b> sebuah pertemuan, untuk dipakai sebagai
	 * pengundang pada berkas undangan kalender.
	 *
	 * <p>Sumbernya dua: seluruh dosen pengampu wadah yang menaungi pertemuan tersebut, ditambah
	 * alamat pemantauan yang dikonfigurasi secara global.</p>
	 *
	 * <h4>Cakupan wadah</h4>
	 * <p>Hanya enam relasi pemilik yang diperiksa: perkuliahan, permohonan tugas akhir, skripsi,
	 * kelompok KKN, kelompok PKL, dan KRS. Pertemuan dari wadah lain — kegiatan, jadwal pelajaran,
	 * jadwal ujian, wisuda, dan seterusnya — tidak menghasilkan satu pun alamat dosen; yang tersisa
	 * hanyalah alamat pemantauan.</p>
	 *
	 * <h4>Penguraian alamat</h4>
	 * <p>Kolom surel boleh memuat beberapa alamat yang dipisahkan koma. Setiap potongan dipangkas
	 * spasinya dan divalidasi bentuknya lewat {@code Common.isValidEmailAddress}; yang tidak sah
	 * dibuang diam-diam. Alamat pemantauan diurai dengan aturan yang sama.</p>
	 *
	 * <h4>Dua hal yang perlu diketahui sebelum memakainya</h4>
	 * <ul>
	 * <li><b>Nilai baliknya {@link List}, bukan {@link Set}.</b> Dosen yang menempati dua slot
	 * pada wadah yang sama, atau alamat pemantauan yang kebetulan sama dengan alamat seorang
	 * dosen, akan muncul lebih dari sekali. Bandingkan dengan {@link #getAttendee(Pertemuan)} yang
	 * memakai {@link Set} sehingga otomatis bebas kembar. Pemanggil yang tidak ingin mengirim
	 * undangan ganda harus menyaringnya sendiri.</li>
	 * <li><b>Membaca konfigurasi dapat menulis ke basis data.</b> Pengambilan
	 * {@code alamat_email_monitoring} lewat {@code Common.getKonfigurasi} akan menuliskan baris
	 * konfigurasi bernilai bawaan bila kuncinya belum ada — perilaku umum mekanisme konfigurasi
	 * AIS, bukan kekhususan method ini. Artinya pemanggilan pertama method ini pada instalasi baru
	 * menghasilkan penulisan basis data sebagai efek samping.</li>
	 * </ul>
	 *
	 * <p>Method ini mengumpulkan data pribadi. Tidak ada pemeriksaan hak akses di dalamnya:
	 * siapa pun yang dapat memanggilnya memperoleh alamat surel seluruh dosen pengampu. Otorisasi
	 * adalah tanggung jawab pemanggil.</p>
	 *
	 * <p>Argumen {@code null} melempar {@link NullPointerException}; dosen tanpa surel dilewati.</p>
	 *
	 * @param pertemuan pertemuan yang penyelenggaranya dicari; tidak boleh {@code null}
	 * @return daftar alamat surel yang sah, mungkin memuat kembaran; kosong bila tidak ada
	 */
	public static List<String> getOrganizer(Pertemuan pertemuan) {
		List<String> emails = new ArrayList<String>();
		List<Dosen> dosens = new ArrayList<Dosen>();
		if (pertemuan.getPerkuliahan() != null) {
			dosens = pertemuan.getPerkuliahan().populateDosenBuNama();
		} else if (pertemuan.getMahasiswaRequestTugasAkhir() != null) {
			dosens = pertemuan.getMahasiswaRequestTugasAkhir().populateDosenBuNama();
		} else if (pertemuan.getSkripsi() != null) {
			dosens = pertemuan.getSkripsi().populateDosenBuNama();
		} else if (pertemuan.getKelompokKkn() != null) {
			dosens = pertemuan.getKelompokKkn().populateDosenBuNama();
		} else if (pertemuan.getKelompokPkl() != null) {
			dosens = pertemuan.getKelompokPkl().populateDosenBuNama();
		} else if (pertemuan.getKrsMahasiswa() != null) {
			dosens = pertemuan.getKrsMahasiswa().populateDosenBuNama();
		}
		for (Dosen dosen : dosens) {
			if (dosen.getEmail() != null && !dosen.getEmail().trim().isEmpty()) {
				String email = dosen.getEmail();
				for (String e : email.split(",")) {
					if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
						emails.add(e.trim());
					}
				}
			}
		}
		dosens = null;
		String email = Common.getKonfigurasi("alamat_email_monitoring", "").getNilai();
		for (String e : email.split(",")) {
			if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
				emails.add(e.trim());
			}
		}
		return emails;
	}

	/**
	 * Mengumpulkan alamat surel <b>peserta</b> sebuah pertemuan — mahasiswa, calon mahasiswa, atau
	 * peserta kegiatan — beserta dosen pengampunya, untuk dipakai sebagai daftar undangan
	 * kalender.
	 *
	 * <p>Nilai baliknya {@link Set} sehingga alamat kembar otomatis menyatu, berbeda dari
	 * {@link #getOrganizer(Pertemuan)} yang memakai {@link List}.</p>
	 *
	 * <h4>Tujuh cabang, tiga cara pengambilan berbeda</h4>
	 * <ul>
	 * <li><b>Perkuliahan</b> — menelusuri seluruh {@link Detailperkuliahan} peserta dari cache
	 * proses lalu membaca surel mahasiswanya. Rantai
	 * {@code detailperkuliahan.getMahasiswa().getEmail()} dibaca tanpa penjaga {@code null} pada
	 * mahasiswanya, sehingga baris peserta yang relasi mahasiswanya kosong melempar
	 * {@link NullPointerException} ke pemanggil.</li>
	 * <li><b>Jadwal ujian PMB</b> — satu kueri langsung ke {@link BiodataCalonMahasiswa} yang
	 * dikelompokkan menurut surel, dibatasi gelombang pendaftaran dan (bila ada) paket ujiannya.
	 * Ini satu-satunya cabang yang tidak melewati cache sama sekali.</li>
	 * <li><b>Formulir kegiatan</b> — satu kueri dengan {@code LEFT JOIN} ke dosen dan mahasiswa
	 * sekaligus, memproyeksikan kedua kolom surel; tiap baris diambil surel dosennya bila ada,
	 * kalau tidak surel mahasiswanya. Baris yang keduanya kosong menghasilkan kesalahan yang
	 * dicetak lalu dilewati.</li>
	 * <li><b>Permohonan tugas akhir, skripsi, dan KRS</b> — satu mahasiswa saja, ditambah dosen
	 * pembimbingnya.</li>
	 * <li><b>Kelompok KKN dan kelompok PKL</b> — menelusuri anggota kelompok.</li>
	 * </ul>
	 * <p>Wadah di luar ketujuh cabang tersebut menghasilkan himpunan kosong.</p>
	 *
	 * <h4>Efek samping pada session Hibernate</h4>
	 * <p>Dua cabang berbasis kueri — jadwal ujian PMB dan formulir kegiatan — mengambil session
	 * milik thread lewat {@code currentNativeSession()} lalu memanggil
	 * {@code HibernateUtil.closeSession()} setelah selesai. Pemanggilan itu <b>menutup session
	 * milik thread</b>, bukan sekadar session lokal method ini. Bila method dipanggil di tengah
	 * alur yang masih memegang session dan entity terkelola, entity tersebut berubah menjadi
	 * detached dan akses lazy berikutnya gagal. Kumpulkan daftar undangan sebelum memulai
	 * pekerjaan basis data lain, bukan di tengahnya.</p>
	 *
	 * <h4>Penguraian alamat dan privasi</h4>
	 * <p>Sama seperti {@link #getOrganizer(Pertemuan)}: kolom surel dipisah koma, tiap potongan
	 * dipangkas dan divalidasi, yang tidak sah dibuang diam-diam.</p>
	 * <p>Method ini mengumpulkan alamat surel seluruh peserta — termasuk calon mahasiswa yang
	 * belum menjadi bagian institusi — tanpa pemeriksaan hak akses apa pun. Penyaringan yang ada
	 * hanya penyaringan data (gelombang pendaftaran, paket, keanggotaan kelompok), bukan
	 * penyaringan wewenang. Pemanggil wajib memastikan pengguna yang berjalan memang berhak
	 * mengirim undangan atas pertemuan tersebut.</p>
	 *
	 * <p>Argumen {@code null} melempar {@link NullPointerException}.</p>
	 *
	 * @param pertemuan pertemuan yang pesertanya dicari; tidak boleh {@code null}
	 * @return himpunan alamat surel yang sah, bebas kembar; kosong bila wadahnya tidak dikenali
	 *         atau tidak ada peserta bersurel
	 */
	@SuppressWarnings("unchecked")
	public static Set<String> getAttendee(Pertemuan pertemuan) {

		List<Dosen> dosens = new ArrayList<Dosen>();
		Set<String> emails = new HashSet<String>();
		if (pertemuan.getPerkuliahan() != null) {
			dosens = pertemuan.getPerkuliahan().populateDosenBuNama();
			for (Long detailperkuliahanid : pertemuan.getPerkuliahan().ambilDetailperkuliahan()) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getMahasiswa().getEmail() != null
							&& !detailperkuliahan.getMahasiswa().getEmail().trim().isEmpty()) {

						String email = detailperkuliahan.getMahasiswa().getEmail();
						for (String e : email.split(",")) {
							if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
								emails.add(e.trim());
							}
						}
					}
				}
			}
		} else if (pertemuan.getJadwalUjianPMB() != null && pertemuan.getJadwalUjianPMB().getUjianPMB() != null
				&& pertemuan.getJadwalUjianPMB().getUjianPMB().getGelombangPendaftaran() != null) {
			Session session = HibernateUtil.currentNativeSession();
			List<String> d = session.createCriteria(BiodataCalonMahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("gelombangPendaftaran",
							pertemuan.getJadwalUjianPMB().getUjianPMB().getGelombangPendaftaran()))
					.add(pertemuan.getJadwalUjianPMB().getPaket() == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("paket", pertemuan.getJadwalUjianPMB().getPaket()))
					.setProjection(Projections.groupProperty("email")).add(Restrictions.isNotNull("email"))
					.add(Restrictions.ne("email", "")).list();
			HibernateUtil.closeSession();
			for (String email : d) {
				for (String e : email.split(",")) {
					if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
						emails.add(e.trim());
					}
				}
			}
		} else if (pertemuan.getFormulirKegiatan() != null) {
			Session session = HibernateUtil.currentNativeSession();
			List<Object[]> d = session.createCriteria(FormulirKegiatanPeserta.class)
					.add(Restrictions.eq("formulirKegiatan", pertemuan.getFormulirKegiatan()))
					.createAlias("dosen", "dosen", Criteria.LEFT_JOIN)
					.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
					.setProjection(Projections.projectionList().add(Projections.property("dosen.email"))
							.add(Projections.property("mahasiswa.email")))
					.add(Restrictions.or(Restrictions.isNotNull("dosen.email"),
							Restrictions.isNotNull("mahasiswa.email")))
					.add(Restrictions.or(Restrictions.ne("dosen.email", ""), Restrictions.ne("mahasiswa.email", "")))
					.list();
			HibernateUtil.closeSession();
			for (Object[] a : d) {
				try {
					String email = a[0] == null ? a[1].toString() : a[0].toString();
					for (String e : email.split(",")) {
						if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
							emails.add(e.trim());
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOPembelajaran.java:1202");
				}
			}
		} else if (pertemuan.getMahasiswaRequestTugasAkhir() != null) {
			dosens = pertemuan.getMahasiswaRequestTugasAkhir().populateDosenBuNama();
			if (pertemuan.getMahasiswaRequestTugasAkhir().getMahasiswa().getEmail() != null
					&& !pertemuan.getMahasiswaRequestTugasAkhir().getMahasiswa().getEmail().trim().isEmpty()) {

				String email = pertemuan.getMahasiswaRequestTugasAkhir().getMahasiswa().getEmail();
				for (String e : email.split(",")) {
					if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
						emails.add(e.trim());
					}
				}
			}
		} else if (pertemuan.getSkripsi() != null) {
			dosens = pertemuan.getSkripsi().populateDosenBuNama();
			if (pertemuan.getSkripsi().getMahasiswa().getEmail() != null
					&& !pertemuan.getSkripsi().getMahasiswa().getEmail().trim().isEmpty()) {
				String email = pertemuan.getSkripsi().getMahasiswa().getEmail();
				for (String e : email.split(",")) {
					if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
						emails.add(e.trim());
					}
				}
			}
		} else if (pertemuan.getKelompokKkn() != null) {
			dosens = pertemuan.getKelompokKkn().populateDosenBuNama();
			for (MahasiswaDapatKelompokKkn mahasiswaDapatKelompokKkn : pertemuan.getKelompokKkn()
					.ambilMahasiswaDapatKelompokKkn(false)) {
				if (mahasiswaDapatKelompokKkn.getMahasiswa().getEmail() != null
						&& !mahasiswaDapatKelompokKkn.getMahasiswa().getEmail().trim().isEmpty()) {

					String email = mahasiswaDapatKelompokKkn.getMahasiswa().getEmail();
					for (String e : email.split(",")) {
						if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
							emails.add(e.trim());
						}
					}

				}
			}
		} else if (pertemuan.getKelompokPkl() != null) {
			dosens = pertemuan.getKelompokPkl().populateDosenBuNama();
			for (MahasiswaDapatKelompokPkl mahasiswaDapatKelompokPkl : pertemuan.getKelompokPkl()
					.ambilMahasiswaDapatKelompokPkl(false)) {
				if (mahasiswaDapatKelompokPkl.getMahasiswa().getEmail() != null
						&& !mahasiswaDapatKelompokPkl.getMahasiswa().getEmail().trim().isEmpty()) {
					String email = mahasiswaDapatKelompokPkl.getMahasiswa().getEmail();
					for (String e : email.split(",")) {
						if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
							emails.add(e.trim());
						}
					}
				}
			}
		} else if (pertemuan.getKrsMahasiswa() != null) {
			dosens = pertemuan.getKrsMahasiswa().populateDosenBuNama();
			if (pertemuan.getKrsMahasiswa().getMahasiswa().getEmail() != null
					&& !pertemuan.getKrsMahasiswa().getMahasiswa().getEmail().trim().isEmpty()) {
				String email = pertemuan.getKrsMahasiswa().getMahasiswa().getEmail();
				for (String e : email.split(",")) {
					if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
						emails.add(e.trim());
					}
				}

			}
		}

		for (Dosen dosen : dosens) {
			if (dosen.getEmail() != null && !dosen.getEmail().trim().isEmpty()) {
				String email = dosen.getEmail();
				for (String e : email.split(",")) {
					if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
						emails.add(e.trim());
					}
				}
			}
		}
		dosens = null;
		return emails;
	}

	public TreeMap<String, Dosen> populateDosen() {
		TreeMap<String, Dosen> dosens = new TreeMap<String, Dosen>();

		if (this instanceof KrsMahasiswa) {
			KrsMahasiswa krsMahasiswa = (KrsMahasiswa) this;
			if (krsMahasiswa != null) {
				dosens.put(getId() + "-" + krsMahasiswa.getDosenPa().getId(), krsMahasiswa.getDosenPa());
			}
		} else if (this instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) this;
			if (perkuliahan.getDosen1() != null) {
				dosens.put(getId() + "-" + perkuliahan.getDosen1().getId(), perkuliahan.getDosen1());
			}
			if (perkuliahan.getDosen2() != null) {
				dosens.put(getId() + "-" + perkuliahan.getDosen2().getId(), perkuliahan.getDosen2());
			}
			if (perkuliahan.getDosen3() != null) {
				dosens.put(getId() + "-" + perkuliahan.getDosen3().getId(), perkuliahan.getDosen3());
			}
			if (perkuliahan.getDosen4() != null) {
				dosens.put(getId() + "-" + perkuliahan.getDosen4().getId(), perkuliahan.getDosen4());
			}
			if (perkuliahan.getDosen5() != null) {
				dosens.put(getId() + "-" + perkuliahan.getDosen5().getId(), perkuliahan.getDosen5());
			}
			if (perkuliahan.getDosen6() != null) {
				dosens.put(getId() + "-" + perkuliahan.getDosen6().getId(), perkuliahan.getDosen6());
			}
			if (perkuliahan.getDosen7() != null) {
				dosens.put(getId() + "-" + perkuliahan.getDosen7().getId(), perkuliahan.getDosen7());
			}
			if (perkuliahan.getDosen8() != null) {
				dosens.put(getId() + "-" + perkuliahan.getDosen8().getId(), perkuliahan.getDosen8());
			}
			if (perkuliahan.getDosen9() != null) {
				dosens.put(getId() + "-" + perkuliahan.getDosen9().getId(), perkuliahan.getDosen9());
			}
			if (perkuliahan.getDosen10() != null) {
				dosens.put(getId() + "-" + perkuliahan.getDosen10().getId(), perkuliahan.getDosen10());
			}
		}

		else if (this instanceof KelompokKkn) {
			KelompokKkn kelompokKkn = (KelompokKkn) this;
			if (kelompokKkn.getDosen_pembimbing1() != null) {
				dosens.put("Pembimbing I", kelompokKkn.getDosen_pembimbing1());
			}
			if (kelompokKkn.getDosen_pembimbing2() != null) {
				dosens.put("Pembimbing II", kelompokKkn.getDosen_pembimbing2());
			}
			if (kelompokKkn.getDosen_pembimbing3() != null) {
				dosens.put("Pembimbing III", kelompokKkn.getDosen_pembimbing3());
			}
			if (kelompokKkn.getDosen_pembimbing4() != null) {
				dosens.put("Pembimbing IV", kelompokKkn.getDosen_pembimbing4());
			}
			if (kelompokKkn.getDosen_pembimbing5() != null) {
				dosens.put("Pembimbing V", kelompokKkn.getDosen_pembimbing5());
			}
		}

		else if (this instanceof KelompokPkl) {
			KelompokPkl kelompokPkl = (KelompokPkl) this;
			if (kelompokPkl.getDosen_pembimbing1() != null) {
				dosens.put("Pembimbing I", kelompokPkl.getDosen_pembimbing1());
			}
			if (kelompokPkl.getDosen_pembimbing2() != null) {
				dosens.put("Pembimbing II", kelompokPkl.getDosen_pembimbing2());
			}
			if (kelompokPkl.getDosen_pembimbing3() != null) {
				dosens.put("Pembimbing III", kelompokPkl.getDosen_pembimbing3());
			}
			if (kelompokPkl.getDosen_pembimbing4() != null) {
				dosens.put("Pembimbing IV", kelompokPkl.getDosen_pembimbing4());
			}
			if (kelompokPkl.getDosen_pembimbing5() != null) {
				dosens.put("Pembimbing V", kelompokPkl.getDosen_pembimbing5());
			}
		}

		else if (this instanceof Skripsi) {
			Skripsi skripsi = (Skripsi) this;
			FormatNilaiSkripsi formatNilaiSkripsi = skripsi.getFormatNilaiSkripsi();

			if (skripsi.getPembimbing() != null) {
				dosens.put(formatNilaiSkripsi != null ? formatNilaiSkripsi.getDosen1()
						: getId() + "-" + skripsi.getPembimbing().getId(), skripsi.getPembimbing());
			}
			if (skripsi.getKetuaSidang() != null) {
				dosens.put(formatNilaiSkripsi != null ? formatNilaiSkripsi.getDosen2()
						: getId() + "-" + skripsi.getKetuaSidang().getId(), skripsi.getKetuaSidang());
			}
			if (skripsi.getPembimbing3() != null) {
				dosens.put("Pembimbing III", skripsi.getPembimbing3());
			}

			if (skripsi.getPenguji1() != null) {
				dosens.put(formatNilaiSkripsi != null ? formatNilaiSkripsi.getDosen3()
						: getId() + "-" + skripsi.getPenguji1().getId(), skripsi.getPenguji1());
			}
			if (skripsi.getPenguji2() != null) {
				dosens.put(formatNilaiSkripsi != null ? formatNilaiSkripsi.getDosen4()
						: getId() + "-" + skripsi.getPenguji2().getId(), skripsi.getPenguji2());
			}
			if (skripsi.getPenguji3() != null) {
				dosens.put(formatNilaiSkripsi != null ? formatNilaiSkripsi.getDosen5()
						: getId() + "-" + skripsi.getPenguji3().getId(), skripsi.getPenguji3());
			}
			if (skripsi.getPenguji4() != null) {
				dosens.put(formatNilaiSkripsi != null ? formatNilaiSkripsi.getDosen6()
						: getId() + "-" + skripsi.getPenguji4().getId(), skripsi.getPenguji4());
			}
			if (skripsi.getPenguji5() != null) {
				dosens.put(formatNilaiSkripsi != null ? formatNilaiSkripsi.getDosen7()
						: getId() + "-" + skripsi.getPenguji5().getId(), skripsi.getPenguji5());
			}
		}

		else if (this instanceof MahasiswaRequestTugasAkhir) {
			MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) this;
			FormatNilaiProposalSkripsi formatNilaiProposalSkripsi = mahasiswaRequestTugasAkhir
					.getFormatNilaiProposalSkripsi();
			if (mahasiswaRequestTugasAkhir.getDosen1() != null) {
				dosens.put(
						formatNilaiProposalSkripsi != null ? formatNilaiProposalSkripsi.getDosen1()
								: getId() + "-" + mahasiswaRequestTugasAkhir.getDosen1().getId(),
						mahasiswaRequestTugasAkhir.getDosen1());
			}
			if (mahasiswaRequestTugasAkhir.getDosen2() != null) {
				dosens.put(
						formatNilaiProposalSkripsi != null ? formatNilaiProposalSkripsi.getDosen2()
								: getId() + "-" + mahasiswaRequestTugasAkhir.getDosen2().getId(),
						mahasiswaRequestTugasAkhir.getDosen2());
			}
			if (mahasiswaRequestTugasAkhir.getDosen3() != null) {
				dosens.put(
						formatNilaiProposalSkripsi != null ? formatNilaiProposalSkripsi.getDosen3()
								: getId() + "-" + mahasiswaRequestTugasAkhir.getDosen3().getId(),
						mahasiswaRequestTugasAkhir.getDosen3());
			}
			if (mahasiswaRequestTugasAkhir.getDosen4() != null) {
				dosens.put(
						formatNilaiProposalSkripsi != null ? formatNilaiProposalSkripsi.getDosen4()
								: getId() + "-" + mahasiswaRequestTugasAkhir.getDosen4().getId(),
						mahasiswaRequestTugasAkhir.getDosen4());
			}
			if (mahasiswaRequestTugasAkhir.getDosen5() != null) {
				dosens.put(
						formatNilaiProposalSkripsi != null ? formatNilaiProposalSkripsi.getDosen5()
								: getId() + "-" + mahasiswaRequestTugasAkhir.getDosen5().getId(),
						mahasiswaRequestTugasAkhir.getDosen5());
			}
			if (mahasiswaRequestTugasAkhir.getDosen6() != null) {
				dosens.put(
						formatNilaiProposalSkripsi != null ? formatNilaiProposalSkripsi.getDosen6()
								: getId() + "-" + mahasiswaRequestTugasAkhir.getDosen6().getId(),
						mahasiswaRequestTugasAkhir.getDosen6());
			}
		} else if (this instanceof GrupPertemuan) {
			GrupPertemuan grupPertemuan = (GrupPertemuan) this;
			if (grupPertemuan != null && grupPertemuan.getDosen() != null) {
				dosens.put(getId() + "-" + grupPertemuan.getDosen().getId(), grupPertemuan.getDosen());
			}
		} else if (this instanceof PertemuanPunyaGrupPertemuan) {
			PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan = (PertemuanPunyaGrupPertemuan) this;
			if (pertemuanPunyaGrupPertemuan != null && pertemuanPunyaGrupPertemuan.getGrupPertemuan() != null
					&& pertemuanPunyaGrupPertemuan.getGrupPertemuan().getDosen() != null) {
				dosens.put(getId() + "-" + pertemuanPunyaGrupPertemuan.getGrupPertemuan().getDosen().getId(),
						pertemuanPunyaGrupPertemuan.getGrupPertemuan().getDosen());
			}
		}

		return dosens;
	}

	@SuppressWarnings("unchecked")
	public List<Long> ambilSiswaById() {
		List<Long> siswas = new ArrayList<Long>();
		if (this instanceof JadwalPelajaran) {
			Session session = HibernateUtil.currentNativeSession();
			siswas = session.createCriteria(KelasSiswaPunyaSiswa.class).createAlias("siswa", "siswa")
					.setProjection(Projections.property("siswa.id"))
					.add(Restrictions.eq("kelasSiswa", ((JadwalPelajaran) this).getKelas()))
					.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("siswa.nama")).list();
			// session.disconnect();
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
		return siswas;
	}

	@SuppressWarnings("unchecked")
	public List<Siswa> ambilSiswa() {
		List<Siswa> siswas = new ArrayList<Siswa>();
		if (this instanceof JadwalPelajaran) {
			Session session = HibernateUtil.currentNativeSession();
			siswas = ais.common.ConstantValues.simpleList(session.createCriteria(KelasSiswaPunyaSiswa.class)
					.createAlias("siswa", "siswa").setProjection(Projections.property("siswa.id"))
					.add(Restrictions.eq("kelasSiswa", ((JadwalPelajaran) this).getKelas()))
					.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("siswa.nama")), Siswa.class, false);
			// session.disconnect();
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
		return siswas;
	}

	public List<Long> ambilMahasiswaById() {
		return ambilMahasiswaById(false);
	}

	public List<Long> ambilMahasiswaById(boolean refresh) {
		List<Long> mhs = new ArrayList<Long>();

		if (this instanceof KrsMahasiswa) {
			KrsMahasiswa krsMahasiswa = (KrsMahasiswa) this;
			if (krsMahasiswa != null) {
				mhs.add(krsMahasiswa.getMahasiswa().getId());
			}
		} else if (this instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) this;
			mhs.addAll(perkuliahan.ambilMahasiswaId(refresh));
		}

		else if (this instanceof KelompokKkn) {

			KelompokKkn kelompokKkn = (KelompokKkn) this;
			Collection<MahasiswaDapatKelompokKkn> mahasiswaDapatKelompokKkns = kelompokKkn
					.ambilMahasiswaDapatKelompokKkn(refresh);
			for (MahasiswaDapatKelompokKkn mahasiswaDapatKelompokKkn : mahasiswaDapatKelompokKkns) {
				mhs.add(mahasiswaDapatKelompokKkn.getMahasiswa().getId());
			}
		}

		else if (this instanceof KelompokPkl) {
			KelompokPkl kelompokPkl = (KelompokPkl) this;
			Collection<MahasiswaDapatKelompokPkl> mahasiswaDapatKelompokPkls = kelompokPkl
					.ambilMahasiswaDapatKelompokPkl(refresh);
			for (MahasiswaDapatKelompokPkl mahasiswaDapatKelompokPkl : mahasiswaDapatKelompokPkls) {
				mhs.add(mahasiswaDapatKelompokPkl.getMahasiswa().getId());
			}
		}

		else if (this instanceof Skripsi) {
			Skripsi skripsi = (Skripsi) this;
			if (skripsi != null) {
				mhs.add(skripsi.getMahasiswa().getId());
			}
		}

		else if (this instanceof MahasiswaRequestTugasAkhir) {
			MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) this;
			if (mahasiswaRequestTugasAkhir != null) {
				mhs.add(mahasiswaRequestTugasAkhir.getMahasiswa().getId());
			}
		} else if (this instanceof GrupPertemuan) {
			@SuppressWarnings("unused")
			GrupPertemuan grupPertemuan = (GrupPertemuan) this;

		} else if (this instanceof PertemuanPunyaGrupPertemuan) {
			@SuppressWarnings("unused")
			PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan = (PertemuanPunyaGrupPertemuan) this;

		}

		return mhs;
	}

	public String ambilNamaDosens() {
		List<Dosen> dosens = populateDosenBuNama();
		String d = "";
		for (Dosen dosen : dosens) {
			d += d.isEmpty() ? dosen.getNama() : ", " + dosen.getNama();
		}
		dosens = null;
		return d;
	}

	public List<Guru> populateGuruBuNama() {
		List<Guru> gurus = new ArrayList<Guru>();
		if (this instanceof FormulirKegiatan) {
			FormulirKegiatan formulirKegiatan = (FormulirKegiatan) this;
			if (formulirKegiatan.getGuruPembina() != null) {
				gurus.add(formulirKegiatan.getGuruPembina());
			}

			if (formulirKegiatan.getGuruPembina2() != null) {
				gurus.add(formulirKegiatan.getGuruPembina2());
			}

			if (formulirKegiatan.getGuruPembina3() != null) {
				gurus.add(formulirKegiatan.getGuruPembina3());
			}
		} else if (this instanceof JadwalPelajaran) {

			JadwalPelajaran jadwalPelajaran = (JadwalPelajaran) this;
			if (jadwalPelajaran.getGuru() != null) {
				gurus.add(jadwalPelajaran.getGuru());
			}
			if (jadwalPelajaran.getGuru2() != null && !gurus.contains(jadwalPelajaran.getGuru2())) {
				gurus.add(jadwalPelajaran.getGuru2());
			}
			if (jadwalPelajaran.getGuru3() != null && !gurus.contains(jadwalPelajaran.getGuru3())) {
				gurus.add(jadwalPelajaran.getGuru3());
			}
			if (jadwalPelajaran.getGuru4() != null && !gurus.contains(jadwalPelajaran.getGuru4())) {
				gurus.add(jadwalPelajaran.getGuru4());
			}
			if (jadwalPelajaran.getGuru5() != null && !gurus.contains(jadwalPelajaran.getGuru5())) {
				gurus.add(jadwalPelajaran.getGuru5());
			}

			if (jadwalPelajaran.getGuru6() != null && !gurus.contains(jadwalPelajaran.getGuru6())) {
				gurus.add(jadwalPelajaran.getGuru6());
			}
			if (jadwalPelajaran.getGuru7() != null && !gurus.contains(jadwalPelajaran.getGuru7())) {
				gurus.add(jadwalPelajaran.getGuru7());
			}
			if (jadwalPelajaran.getGuru8() != null && !gurus.contains(jadwalPelajaran.getGuru8())) {
				gurus.add(jadwalPelajaran.getGuru8());
			}
			if (jadwalPelajaran.getGuru9() != null && !gurus.contains(jadwalPelajaran.getGuru9())) {
				gurus.add(jadwalPelajaran.getGuru9());
			}
			if (jadwalPelajaran.getGuru10() != null && !gurus.contains(jadwalPelajaran.getGuru10())) {
				gurus.add(jadwalPelajaran.getGuru10());
			}
			if (jadwalPelajaran.getGuru11() != null && !gurus.contains(jadwalPelajaran.getGuru11())) {
				gurus.add(jadwalPelajaran.getGuru11());
			}
			if (jadwalPelajaran.getGuru12() != null && !gurus.contains(jadwalPelajaran.getGuru12())) {
				gurus.add(jadwalPelajaran.getGuru12());
			}
		}

		return gurus;
	}

	public String ambilNamaGurus() {
		List<Guru> gurus = populateGuruBuNama();
		String d = "";
		for (Guru guru : gurus) {
			d += d.isEmpty() ? guru.getNama() : ", " + guru.getNama();
		}
		gurus = null;
		return d;
	}

	// private List<Dosen> daftarDosen = null;

	public boolean ada(Dosen dosenSelected) {
		if (dosenSelected == null || dosenSelected.getId() == null) {
			return false;
		}
		List<Dosen> dosens = populateDosenBuNama();
		boolean ada = false;
		for (Dosen dosen : dosens) {
			if (dosen != null && dosen.getId() != null && dosen.getId().equals(dosenSelected.getId())) {
				ada = true;
				break;
			}
		}
		dosens = null;
		return ada;
	}

	public List<Long> populateDosenBuId() {
		List<Dosen> dosens = populateDosenBuNama();
		List<Long> ids = new ArrayList<Long>();
		for (Dosen dosen : dosens) {
			ids.add(dosen.getId());
		}
		dosens.clear();
		dosens = null;
		return ids;
	}

	public List<Long> populateGuruBuId() {
		List<Guru> gurus = populateGuruBuNama();
		List<Long> ids = new ArrayList<Long>();
		for (Guru guru : gurus) {
			ids.add(guru.getId());
		}
		gurus.clear();
		gurus = null;
		return ids;
	}

	public List<Dosen> populateDosenBuNama() {
		List<Dosen> dosens = new ArrayList<Dosen>();

		try {
			if (this instanceof FormulirKegiatan) {
				FormulirKegiatan formulirKegiatan = (FormulirKegiatan) this;
				if (formulirKegiatan.getDosenPembina() != null) {
					dosens.add(formulirKegiatan.getDosenPembina());
				}
				if (formulirKegiatan.getDosenPembina2() != null) {
					dosens.add(formulirKegiatan.getDosenPembina2());
				}
				if (formulirKegiatan.getDosenPembina3() != null) {
					dosens.add(formulirKegiatan.getDosenPembina3());
				}
			} else if (this instanceof KrsMahasiswa) {
				KrsMahasiswa krsMahasiswa = (KrsMahasiswa) this;
				if (krsMahasiswa != null && krsMahasiswa.getDosenPa() != null) {
					dosens.add(krsMahasiswa.getDosenPa());
				}
			} else if (this instanceof Perkuliahan) {
				Perkuliahan perkuliahan = (Perkuliahan) this;
				if (perkuliahan.getDosen1() != null) {
					dosens.add(perkuliahan.getDosen1());
				}
				if (perkuliahan.getDosen2() != null) {
					dosens.add(perkuliahan.getDosen2());
				}
				if (perkuliahan.getDosen3() != null) {
					dosens.add(perkuliahan.getDosen3());
				}
				if (perkuliahan.getDosen4() != null) {
					dosens.add(perkuliahan.getDosen4());
				}
				if (perkuliahan.getDosen5() != null) {
					dosens.add(perkuliahan.getDosen5());
				}
				if (perkuliahan.getDosen6() != null) {
					dosens.add(perkuliahan.getDosen6());
				}
				if (perkuliahan.getDosen7() != null) {
					dosens.add(perkuliahan.getDosen7());
				}
				if (perkuliahan.getDosen8() != null) {
					dosens.add(perkuliahan.getDosen8());
				}
				if (perkuliahan.getDosen9() != null) {
					dosens.add(perkuliahan.getDosen9());
				}
				if (perkuliahan.getDosen10() != null) {
					dosens.add(perkuliahan.getDosen10());
				}
			}

			else if (this instanceof KelompokKkn) {
				KelompokKkn kelompokKkn = (KelompokKkn) this;
				if (kelompokKkn.getDosen_pembimbing1() != null) {
					dosens.add(kelompokKkn.getDosen_pembimbing1());
				}
				if (kelompokKkn.getDosen_pembimbing2() != null) {
					dosens.add(kelompokKkn.getDosen_pembimbing2());
				}
				if (kelompokKkn.getDosen_pembimbing3() != null) {
					dosens.add(kelompokKkn.getDosen_pembimbing3());
				}
				if (kelompokKkn.getDosen_pembimbing4() != null) {
					dosens.add(kelompokKkn.getDosen_pembimbing4());
				}
				if (kelompokKkn.getDosen_pembimbing5() != null) {
					dosens.add(kelompokKkn.getDosen_pembimbing5());
				}
				if (kelompokKkn.getDosen_pembimbing6() != null) {
					dosens.add(kelompokKkn.getDosen_pembimbing6());
				}
				if (kelompokKkn.getDosen_pembimbing7() != null) {
					dosens.add(kelompokKkn.getDosen_pembimbing7());
				}
				if (kelompokKkn.getDosen_pembimbing8() != null) {
					dosens.add(kelompokKkn.getDosen_pembimbing8());
				}
				if (kelompokKkn.getDosen_pembimbing9() != null) {
					dosens.add(kelompokKkn.getDosen_pembimbing9());
				}
				if (kelompokKkn.getDosen_pembimbing10() != null) {
					dosens.add(kelompokKkn.getDosen_pembimbing10());
				}
			}

			else if (this instanceof KelompokPkl) {
				KelompokPkl kelompokPkl = (KelompokPkl) this;
				if (kelompokPkl.getDosen_pembimbing1() != null) {
					dosens.add(kelompokPkl.getDosen_pembimbing1());
				}
				if (kelompokPkl.getDosen_pembimbing2() != null) {
					dosens.add(kelompokPkl.getDosen_pembimbing2());
				}
				if (kelompokPkl.getDosen_pembimbing3() != null) {
					dosens.add(kelompokPkl.getDosen_pembimbing3());
				}
				if (kelompokPkl.getDosen_pembimbing4() != null) {
					dosens.add(kelompokPkl.getDosen_pembimbing4());
				}
				if (kelompokPkl.getDosen_pembimbing5() != null) {
					dosens.add(kelompokPkl.getDosen_pembimbing5());
				}
				if (kelompokPkl.getDosen_pembimbing5() != null) {
					dosens.add(kelompokPkl.getDosen_pembimbing5());
				}
				if (kelompokPkl.getDosen_pembimbing6() != null) {
					dosens.add(kelompokPkl.getDosen_pembimbing6());
				}
				if (kelompokPkl.getDosen_pembimbing7() != null) {
					dosens.add(kelompokPkl.getDosen_pembimbing7());
				}
				if (kelompokPkl.getDosen_pembimbing8() != null) {
					dosens.add(kelompokPkl.getDosen_pembimbing8());
				}
				if (kelompokPkl.getDosen_pembimbing9() != null) {
					dosens.add(kelompokPkl.getDosen_pembimbing9());
				}
				if (kelompokPkl.getDosen_pembimbing10() != null) {
					dosens.add(kelompokPkl.getDosen_pembimbing10());
				}
			}

			else if (this instanceof Skripsi) {
				Skripsi skripsi = (Skripsi) this;
				if (skripsi.getKetuaSidang() != null) {
					dosens.add(skripsi.getKetuaSidang());
				}
				if (skripsi.getPembimbing() != null) {
					dosens.add(skripsi.getPembimbing());
				}

				if (skripsi.getPembimbing3() != null) {
					dosens.add(skripsi.getPembimbing3());
				}

				if (skripsi.getPenguji1() != null) {
					dosens.add(skripsi.getPenguji1());
				}
				if (skripsi.getPenguji2() != null) {
					dosens.add(skripsi.getPenguji2());
				}
				if (skripsi.getPenguji3() != null) {
					dosens.add(skripsi.getPenguji3());
				}
				if (skripsi.getPenguji4() != null) {
					dosens.add(skripsi.getPenguji4());
				}
				if (skripsi.getPenguji5() != null) {
					dosens.add(skripsi.getPenguji5());
				}
			}

			else if (this instanceof MahasiswaRequestTugasAkhir) {
				MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) this;
				if (mahasiswaRequestTugasAkhir.getDosen1() != null) {
					dosens.add(mahasiswaRequestTugasAkhir.getDosen1());
				}
				if (mahasiswaRequestTugasAkhir.getDosen2() != null) {
					dosens.add(mahasiswaRequestTugasAkhir.getDosen2());
				}
				if (mahasiswaRequestTugasAkhir.getDosen3() != null) {
					dosens.add(mahasiswaRequestTugasAkhir.getDosen3());
				}
				if (mahasiswaRequestTugasAkhir.getDosen4() != null) {
					dosens.add(mahasiswaRequestTugasAkhir.getDosen4());
				}
				if (mahasiswaRequestTugasAkhir.getDosen5() != null) {
					dosens.add(mahasiswaRequestTugasAkhir.getDosen5());
				}
				if (mahasiswaRequestTugasAkhir.getDosen6() != null) {
					dosens.add(mahasiswaRequestTugasAkhir.getDosen6());
				}

			} else if (this instanceof GrupPertemuan) {
				GrupPertemuan grupPertemuan = (GrupPertemuan) this;
				if (grupPertemuan != null && grupPertemuan.getDosen() != null) {
					dosens.add(grupPertemuan.getDosen());
				}
			} else if (this instanceof PertemuanPunyaGrupPertemuan) {
				PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan = (PertemuanPunyaGrupPertemuan) this;
				if (pertemuanPunyaGrupPertemuan != null && pertemuanPunyaGrupPertemuan.getGrupPertemuan() != null
						&& pertemuanPunyaGrupPertemuan.getGrupPertemuan().getDosen() != null) {
					dosens.add(pertemuanPunyaGrupPertemuan.getGrupPertemuan().getDosen());
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOPembelajaran.java:1869");
		}

		return dosens;
	}

	public Integer getJumlahDosen() {
		List<Dosen> dosens = populateDosenBuNama();
		int jumlahDosen = dosens.size();
		dosens = null;
		return jumlahDosen;
	}

	public String toIdSmt() {
		String tahunAjaran = ambilTahunAjaran();
		String jenisSemester = ambilJenisSemester();
		String tahun = tahunAjaran == null || tahunAjaran.trim().isEmpty() ? "-"
				: tahunAjaran.split("/")[0];
		String id_smt = tahun + (Boolean.TRUE.equals(ambilMerupakanSP()) ? "3"
				: (Perkuliahan.GENAP.equals(jenisSemester) ? "2" : "1"));
		return id_smt;
	}

	@Override
	public int compareTo(GeneralValueObject arg0) {
		try {
			if (arg0 != null && arg0 instanceof Perkuliahan) {
				Perkuliahan arg = (Perkuliahan) arg0;
				Perkuliahan ini = (Perkuliahan) this;

				int indexHari1 = 10;
				int indexHari2 = 10;
				int i = 10;
				for (String s : Common.haris) {
					if (arg.getHari() != null && s.equalsIgnoreCase(arg.getHari())) {
						indexHari1 = i;
					}
					if (ini.getHari() != null && s.equalsIgnoreCase(ini.getHari())) {
						indexHari2 = i;
					}
					i--;
				}

				String waktu1 = (100.0 - arg.getWaktuMulaiD()) + "_" + (100.0 - arg.getWaktuSelesaiD());
				String waktu2 = (100.0 - ini.getWaktuMulaiD()) + "_" + (100.0 - ini.getWaktuSelesaiD());

				String w1 = arg.toIdSmt() + (arg.getMerupakanPraPerkuliahan() ? "_0_pra" : "") + "_" + indexHari1 + "_"
						+ waktu1;
				String w2 = ini.toIdSmt() + (ini.getMerupakanPraPerkuliahan() ? "_0_pra" : "") + "_" + indexHari2 + "_"
						+ waktu2;

				return w2.compareTo(w1);

			} else if (arg0 instanceof VOPembelajaran) {
				VOPembelajaran voPembelajaran = (VOPembelajaran) arg0;
				return (toIdSmt() + ambilJenis()).compareTo(voPembelajaran.toIdSmt() + voPembelajaran.ambilJenis());
			} else {
				super.compareTo(arg0);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:1924");
		}

		return 0;
	}

	public abstract Integer ambilJumlahDetailperkuliahanLangsung();

	public String infoSimple() {
		Dosen d = null;
		return this.infoSimple(d);
	}

	public String infoSimple(Dosen dosenTambahan) {
		try {

			if (this instanceof Perkuliahan) {
				Perkuliahan perkuliahan = (Perkuliahan) this;
				String matkul1 = perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getNama();

				String semester1 = perkuliahan.getSemester() == null ? "" : perkuliahan.getSemester().toString();

				if (perkuliahan.getStatusSemesterPendek() != null
						&& perkuliahan.getStatusSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK)) {
					semester1 = semester1 + " (" + Common.getBahasaConfig(Perkuliahan.SP) + ")";
				}

				Integer sks = perkuliahan.getMatakuliah() == null ? 0 : perkuliahan.getMatakuliah().getSks();

				String kelas1 = perkuliahan.getKelas();

				String dosen1 = "";
				for (Dosen dosen : populateDosen().values()) {
					dosen1 += dosen1.isEmpty() ? dosen.getNama() : ", " + dosen.getNama();
				}

				if (dosenTambahan != null) {
					dosen1 += dosen1.isEmpty() ? dosenTambahan.getNama() : ", " + dosenTambahan.getNama();
				}

				String ruang = perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getNama();

				String harijam = (perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan() == null ? false
						: perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan()) ? ""
								: (", " + perkuliahan.getHari() + ", " + perkuliahan.getWaktuMulai() + " s.d "
										+ perkuliahan.getWaktuSelesai());

				String groupTxt = matkul1 + " (" + sks + " SKS) " + semester1 + " " + kelas1
						+ (dosen1.equals("") ? "" : " " + dosen1) + (ruang.equals("") ? "" : " " + ruang) + harijam;
				return groupTxt;
			} else if (this instanceof KelompokKkn) {
				KelompokKkn kelompokKkn = (KelompokKkn) this;
				return kelompokKkn.getNama() + " (" + kelompokKkn.getKkn().getNama() + ")";
			} else if (this instanceof KelompokPkl) {
				KelompokPkl kelompokPkl = (KelompokPkl) this;
				return kelompokPkl.getNama() + " (" + kelompokPkl.getPkl().getNama() + ")";
			} else if (this instanceof KrsMahasiswa) {
				KrsMahasiswa krsMahasiswa = (KrsMahasiswa) this;
				Mahasiswa mahasiswa = krsMahasiswa.getMahasiswa();
				if (mahasiswa != null) {
					return mahasiswa.getNama() + " TA " + krsMahasiswa.getTahunAkademik()
							+ (krsMahasiswa.getSemesterPendek() == null ? "" : " (SP)") + ", " +

							(mahasiswa.getStatusKeluar() == null
									? KrsDetailHelper.rubahKeteranganPengambilanKRSBersih(mahasiswa, krsMahasiswa.getSemester(),
											krsMahasiswa.getTahapan(), krsMahasiswa.getSemesterPendek(), krsMahasiswa,
											false)
									: (mahasiswa.getStatusKeluar().getNama()
											+ (mahasiswa.getPredikatKelulusan() == null ? ""
													: " / " + mahasiswa.getPredikatKelulusan().getNama())

											+ (mahasiswa.getStatusSetelahLulus() == null ? ""
													: " / " + mahasiswa.getStatusSetelahLulus().getNama())

											+ (mahasiswa.getStatusPekerjaanSetelahLulus() == null ? ""
													: " / " + mahasiswa.getStatusPekerjaanSetelahLulus().getNama())

											+ (mahasiswa.getStatusDomisiliSetelahLulus() == null ? ""
													: " / " + mahasiswa.getStatusDomisiliSetelahLulus().getNama())));
				}
			} else if (this instanceof Skripsi) {
				Skripsi skripsi = (Skripsi) this;
				return skripsi.getJudul()
						+ (skripsi.getFormatNilaiSkripsi() == null ? ""
								: " (" + skripsi.getFormatNilaiSkripsi().getNama() + ")")
						+ "-" + skripsi.getMahasiswa().getNim() + "-" + skripsi.getMahasiswa().getNama();
			} else if (this instanceof MahasiswaRequestTugasAkhir) {
				MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) this;
				return (mahasiswaRequestTugasAkhir.getJudul().isEmpty() ? mahasiswaRequestTugasAkhir.getJudul1()
						: mahasiswaRequestTugasAkhir.getJudul())
						+ (mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi() == null ? ""
								: " (" + mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getNama() + ")")
						+ "-" + mahasiswaRequestTugasAkhir.getMahasiswa().getNim() + "-"
						+ mahasiswaRequestTugasAkhir.getMahasiswa().getNama();
			} else if (this instanceof PertemuanPunyaGrupPertemuan) {
				PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan = (PertemuanPunyaGrupPertemuan) this;
				return pertemuanPunyaGrupPertemuan.getGrupPertemuan().getNama();
			} else if (this instanceof JadwalUjianPMB) {
				JadwalUjianPMB jadwalUjianPMB = (JadwalUjianPMB) this;
				return jadwalUjianPMB.getNama();
			} else if (this instanceof JadwalUjianPSB) {
				JadwalUjianPSB jadwalUjianPSB = (JadwalUjianPSB) this;
				return jadwalUjianPSB.getNama();
			} else if (this instanceof FormulirKegiatan) {
				FormulirKegiatan formulirKegiatan = (FormulirKegiatan) this;
				return formulirKegiatan.getNama();
			} else if (this instanceof Wisuda) {
				Wisuda wisuda = (Wisuda) this;
				return (wisuda.getMoto().isEmpty() ? "" : wisuda.getMoto() + " | ") + "Wisuda ke-"
						+ wisuda.getWisudaKe();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOPembelajaran.java:2036");
		}
		return "-";
	}

	public String infoSangatSimple() {
		Dosen d = null;
		return this.infoSangatSimple(d);
	}

	public String infoSangatSimple(Dosen dosenTambahan) {

		if (this instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) this;
			String matkul1 = perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getNama();

			String dosen1 = "";
			for (Dosen dosen : populateDosen().values()) {
				dosen1 += dosen1.isEmpty() ? dosen.getNama() : ", " + dosen.getNama();
			}

			if (dosenTambahan != null) {
				dosen1 += dosen1.isEmpty() ? dosenTambahan.getNama() : ", " + dosenTambahan.getNama();
			}

			String groupTxt = matkul1 + (dosen1.equals("") ? "" : " " + dosen1);
			return groupTxt;
		} else if (this instanceof JadwalPelajaran) {
			JadwalPelajaran jadwalPelajaran = (JadwalPelajaran) this;
			String matkul1 = jadwalPelajaran.getMatapelajaran() == null ? ""
					: jadwalPelajaran.getMatapelajaran().getNama();

			String dosen1 = "";
			if (jadwalPelajaran.getGuru() != null) {
				dosen1 = jadwalPelajaran.getGuru().getNama();
			}

			String groupTxt = matkul1 + (dosen1.equals("") ? "" : " " + dosen1);
			return groupTxt;
		} else if (this instanceof KelompokKkn) {
			KelompokKkn kelompokKkn = (KelompokKkn) this;
			return kelompokKkn.getNama();
		} else if (this instanceof KelompokPkl) {
			KelompokPkl kelompokPkl = (KelompokPkl) this;
			return kelompokPkl.getNama();
		} else if (this instanceof KrsMahasiswa) {
			KrsMahasiswa krsMahasiswa = (KrsMahasiswa) this;
			Mahasiswa mahasiswa = krsMahasiswa.getMahasiswa();
			if (mahasiswa != null) {
				return "KRS " + mahasiswa.getNama();
			}
		} else if (this instanceof Skripsi) {
			Skripsi skripsi = (Skripsi) this;
			return (skripsi.getFormatNilaiSkripsi() == null ? "" : skripsi.getFormatNilaiSkripsi().getNama());
		} else if (this instanceof MahasiswaRequestTugasAkhir) {
			MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) this;
			return (mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi() == null ? ""
					: mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getNama());
		} else if (this instanceof PertemuanPunyaGrupPertemuan) {
			PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan = (PertemuanPunyaGrupPertemuan) this;
			return pertemuanPunyaGrupPertemuan.getGrupPertemuan().getNama();
		} else if (this instanceof JadwalUjianPMB) {
			JadwalUjianPMB jadwalUjianPMB = (JadwalUjianPMB) this;
			return jadwalUjianPMB.getNama();
		} else if (this instanceof JadwalUjianPSB) {
			JadwalUjianPSB jadwalUjianPSB = (JadwalUjianPSB) this;
			return jadwalUjianPSB.getNama();
		} else if (this instanceof FormulirKegiatan) {
			FormulirKegiatan formulirKegiatan = (FormulirKegiatan) this;
			return formulirKegiatan.getNama();
		}

		return "-";
	}

	public String ambilTahunAjaran() {
		try {
			if (this instanceof Perkuliahan) {
				Perkuliahan perkuliahan = (Perkuliahan) this;
				return perkuliahan.getTahunAjaran();
			} else if (this instanceof KelompokKkn) {
				KelompokKkn kelompokKkn = (KelompokKkn) this;
				return kelompokKkn.getKkn() == null ? "-" : kelompokKkn.getKkn().getTahunAkademik();
			} else if (this instanceof KelompokPkl) {
				KelompokPkl kelompokPkl = (KelompokPkl) this;
				return kelompokPkl.getPkl() == null ? "-" : kelompokPkl.getPkl().getTahunAkademik();
			} else if (this instanceof KrsMahasiswa) {
				KrsMahasiswa krsMahasiswa = (KrsMahasiswa) this;
				return krsMahasiswa.getTahunAkademik();
			} else if (this instanceof FormulirKegiatan) {
				FormulirKegiatan formulirKegiatan = (FormulirKegiatan) this;
				return formulirKegiatan.getTahunAkademik();
			} else if (this instanceof Skripsi) {
				Skripsi skripsi = (Skripsi) this;
				return skripsi.getTahunAkademik();
			} else if (this instanceof MahasiswaRequestTugasAkhir) {
				MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) this;
				return mahasiswaRequestTugasAkhir.getTahunAkademik();
			} else if (this instanceof PertemuanPunyaGrupPertemuan) {
				PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan = (PertemuanPunyaGrupPertemuan) this;
				return pertemuanPunyaGrupPertemuan.getGrupPertemuan().getTahunAkademik();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:2138");
			// TODO: handle exception
		}
		return "-";
	}

	public Boolean ambilMerupakanSP() {
		if (this instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) this;
			return perkuliahan.getStatusSemesterPendek() != null;
		} else if (this instanceof KelompokKkn) {
			return false;
		} else if (this instanceof KelompokPkl) {
			return false;
		} else if (this instanceof KrsMahasiswa) {
			KrsMahasiswa krsMahasiswa = (KrsMahasiswa) this;
			return krsMahasiswa.getSemesterPendek() != null;
		} else if (this instanceof Skripsi) {
			return false;
		} else if (this instanceof MahasiswaRequestTugasAkhir) {
			return false;
		} else if (this instanceof PertemuanPunyaGrupPertemuan) {
			PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan = (PertemuanPunyaGrupPertemuan) this;
			return pertemuanPunyaGrupPertemuan.getGrupPertemuan().getSemesterPendek() != null;
		}
		return false;
	}

	public String ambilJenis() {
		if (this instanceof Perkuliahan) {
			return "Perkuliahan";
		} else if (this instanceof KelompokKkn) {
			return "KKN";
		} else if (this instanceof KelompokPkl) {
			return "PKL";
		} else if (this instanceof KrsMahasiswa) {
			return "Bimbingan Akademik";
		} else if (this instanceof Skripsi) {
			return "Sidang Skripsi / TA";
		} else if (this instanceof FormulirKegiatan) {
			return "Kegiatan";
		} else if (this instanceof MahasiswaRequestTugasAkhir) {
			return "Bimbingan Skripsi / TA";
		} else if (this instanceof PertemuanPunyaGrupPertemuan) {
			PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan = (PertemuanPunyaGrupPertemuan) this;
			return pertemuanPunyaGrupPertemuan.getGrupPertemuan().getJenis();
		}
		return "-";
	}

	public String ambilTahunAkademik() {
		return ambilTahunAjaran();
	}

	public Integer ambilSemester() {
		if (this instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) this;
			return perkuliahan.getSemester();
		} else if (this instanceof KelompokKkn) {
			return 1;
		} else if (this instanceof KelompokPkl) {
			return 1;
		} else if (this instanceof KrsMahasiswa) {
			KrsMahasiswa krsMahasiswa = (KrsMahasiswa) this;
			return krsMahasiswa.getSemester();
		} else if (this instanceof Skripsi) {
			Skripsi skripsi = (Skripsi) this;
			return skripsi.getSemester();
		} else if (this instanceof MahasiswaRequestTugasAkhir) {
			MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) this;
			return mahasiswaRequestTugasAkhir.getSemester();
		} else if (this instanceof PertemuanPunyaGrupPertemuan) {
			return 1;
		}
		return -1;
	}

	public String ambilJenisSemester() {
		if (this instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) this;
			return perkuliahan.getGanjilGenap();
		} else if (this instanceof KelompokKkn) {
			KelompokKkn kelompokKkn = (KelompokKkn) this;
			return kelompokKkn.getKkn() == null ? null : kelompokKkn.getKkn().getSemester();
		} else if (this instanceof KelompokPkl) {
			KelompokPkl kelompokPkl = (KelompokPkl) this;
			return kelompokPkl.getPkl() == null ? null : kelompokPkl.getPkl().getSemester();
		} else if (this instanceof FormulirKegiatan) {
			FormulirKegiatan formulirKegiatan = (FormulirKegiatan) this;
			return formulirKegiatan.getSemester();
		} else if (this instanceof KrsMahasiswa) {
			KrsMahasiswa krsMahasiswa = (KrsMahasiswa) this;
			return krsMahasiswa.getSemester() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL;
		} else if (this instanceof Skripsi) {
			Skripsi skripsi = (Skripsi) this;
			return skripsi.getSemester() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL;
		} else if (this instanceof MahasiswaRequestTugasAkhir) {
			MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) this;
			return mahasiswaRequestTugasAkhir.getSemester() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL;
		} else if (this instanceof PertemuanPunyaGrupPertemuan) {
			PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan = (PertemuanPunyaGrupPertemuan) this;
			return pertemuanPunyaGrupPertemuan.getGrupPertemuan().getJenisSemester();
		}
		return "-";
	}

	public String ambilKeyword() {

		if (this instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) this;
			String keyword = "";

			List<Dosen> dosens = perkuliahan.populateDosenBuNama();
			for (Dosen dosen : dosens) {
				keyword += dosen.getNama() + " ";
			}
			dosens = null;

			keyword += perkuliahan.getMatakuliah().getNama() + " ";
			keyword += perkuliahan.getMatakuliah().getKode() + " ";
			return keyword.trim();
		} else if (this instanceof KelompokKkn) {
			KelompokKkn kelompokKkn = (KelompokKkn) this;
			String keyword = "";
			List<Dosen> dosens = kelompokKkn.populateDosenBuNama();
			for (Dosen dosen : dosens) {
				keyword += dosen.getNama() + " ";
			}
			dosens = null;

			keyword += kelompokKkn.getNama() + " ";
			keyword += kelompokKkn.getKkn().getNama() + " ";
			return keyword.trim();
		} else if (this instanceof KelompokPkl) {
			KelompokPkl kelompokPkl = (KelompokPkl) this;
			String keyword = "";
			List<Dosen> dosens = kelompokPkl.populateDosenBuNama();
			for (Dosen dosen : dosens) {
				keyword += dosen.getNama() + " ";
			}
			dosens = null;

			keyword += kelompokPkl.getNama() + " ";
			keyword += kelompokPkl.getPkl().getNama() + " ";
			return keyword.trim();
		} else if (this instanceof FormulirKegiatan) {
			FormulirKegiatan formulirKegiatan = (FormulirKegiatan) this;
			String keyword = "";

			keyword += formulirKegiatan.getNama() + " ";
			return keyword.trim();
		} else if (this instanceof KrsMahasiswa) {
			KrsMahasiswa krsMahasiswa = (KrsMahasiswa) this;
			String keyword = "";

			keyword += krsMahasiswa.getMahasiswa().getNim() + " ";
			keyword += krsMahasiswa.getMahasiswa().getNama() + " ";
			keyword += krsMahasiswa.getCatatan() + " ";
			keyword += krsMahasiswa.getCatatanKhs() + " ";

			List<Dosen> dosens = krsMahasiswa.populateDosenBuNama();
			for (Dosen dosen : dosens) {
				keyword += dosen.getNama() + " ";
			}
			dosens = null;
//			System.out.println("keyword => " + keyword);
			return keyword.trim();
		} else if (this instanceof Skripsi) {
			Skripsi skripsi = (Skripsi) this;
			String keyword = "";
			List<Dosen> dosens = skripsi.populateDosenBuNama();
			for (Dosen dosen : dosens) {
				keyword += dosen.getNama() + " ";
			}
			dosens = null;

			keyword += skripsi.getJudul() + " ";
			keyword += skripsi.getKeyword() + " ";

			keyword += skripsi.getMahasiswa().getNim() + " ";
			keyword += skripsi.getMahasiswa().getNama() + " ";

			return keyword.trim();
		} else if (this instanceof MahasiswaRequestTugasAkhir) {
			MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) this;
			String keyword = "";
			List<Dosen> dosens = mahasiswaRequestTugasAkhir.populateDosenBuNama();
			for (Dosen dosen : dosens) {
				keyword += dosen.getNama() + " ";
			}
			dosens = null;

			keyword += mahasiswaRequestTugasAkhir.getMahasiswa().getNim() + " ";
			keyword += mahasiswaRequestTugasAkhir.getMahasiswa().getNama() + " ";

			keyword += mahasiswaRequestTugasAkhir.getJudul() + " ";
			return keyword.trim();
		} else if (this instanceof PertemuanPunyaGrupPertemuan) {
			PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan = (PertemuanPunyaGrupPertemuan) this;
			String keyword = "";
			List<Dosen> dosens = pertemuanPunyaGrupPertemuan.populateDosenBuNama();
			for (Dosen dosen : dosens) {
				keyword += dosen.getNama() + " ";
			}
			dosens = null;

			keyword += pertemuanPunyaGrupPertemuan.getGrupPertemuan().getNama() + " ";
			keyword += pertemuanPunyaGrupPertemuan.getGrupPertemuan().getJenis() + " ";
			return keyword.trim();
		}
		return "";
	}

	public String ambilKelas() {

		if (this instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) this;
			return perkuliahan.getKelas().trim();
		} else if (this instanceof KrsMahasiswa) {
			KrsMahasiswa krsMahasiswa = (KrsMahasiswa) this;
			return krsMahasiswa.getMahasiswa().getKelas().trim();
		} else if (this instanceof Skripsi) {
			Skripsi skripsi = (Skripsi) this;
			return skripsi.getMahasiswa().getKelas().trim();
		} else if (this instanceof MahasiswaRequestTugasAkhir) {
			MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) this;
			return mahasiswaRequestTugasAkhir.getMahasiswa().getKelas().trim();
		}
		return "";
	}

	public String ambilHari() {
		if (this instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) this;
			return perkuliahan.getHari();
		} else {
			return null;
		}
	}

	public Integer ambilExtraKulikuler() {
		if (this instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) this;
			return perkuliahan.getMatakuliah().getExtraKulikuler() ? Perkuliahan.EKSTRA : null;
		} else {
			return null;
		}
	}

	public Boolean ambilMerupakanPraPerkuliahan() {
		if (this instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) this;
			return perkuliahan.getMerupakanPraPerkuliahan();
		} else {
			return false;
		}
	}

	public Boolean ambilMerupakanRemedial() {
		if (this instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) this;
			return perkuliahan.getMerupakanRemedial();
		} else {
			return false;
		}
	}

	public Boolean ambilMerupakanParalel() {
		if (this instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) this;
			return perkuliahan.getMerupakan_paralel() || perkuliahan.flagParalel;
		} else {
			return false;
		}
	}

}
