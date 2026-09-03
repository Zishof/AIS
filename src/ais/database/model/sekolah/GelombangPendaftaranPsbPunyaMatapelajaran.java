package ais.database.model.sekolah;

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
import ais.database.model.MatapelajaranSekolah;

/**
 * <h2>Daftar mata pelajaran yang nilai rapornya diverifikasi pada satu gelombang PSB</h2>
 *
 * <p>Entity penghubung (<i>join table</i>) murni antara satu gelombang pendaftaran siswa baru
 * ({@link ais.database.model.sekolah.GelombangPendaftaranPsb}) dan satu mata pelajaran sekolah
 * ({@link ais.database.model.MatapelajaranSekolah}), dipetakan ke tabel fisik
 * {@code sekolah.gelombang_pendaftaran_psb_punya_matapelajaran}. Satu baris berarti: <i>"pada
 * gelombang X, nilai rapor mata pelajaran Y termasuk yang harus diisi dan diverifikasi panitia
 * PPDB"</i>.</p>
 *
 * <h3>Koreksi domain — BUKAN mata pelajaran yang diujikan</h3>
 * <p>Nama tabel yang generik ("punya matapelajaran") mudah disalahartikan sebagai daftar mata
 * pelajaran <b>ujian masuk</b>. Verifikasi dari kode menunjukkan sebaliknya: seluruh pembaca
 * entity ini berada di jalur <b>verifikasi nilai rapor</b>, bukan jalur ujian/soal. Tab yang
 * menampilkan layar pengelolanya di
 * {@code GelombangPendaftaranPsbAction} pun diberi label <b>"Rapor"</b>. Materi ujian masuk
 * ditangani rantai lain sama sekali ({@code RuangPSB}, {@code kartu ujian},
 * {@code _ikut_ujian_online}), yang nol referensi ke tabel ini.</p>
 *
 * <h3>Pembaca yang terverifikasi (hanya dua)</h3>
 * <ol>
 *   <li>{@code ais.action.master.sekolah.psb.VerifikasiMatapelajaranPSBHelper.tampilkanVerifikasi(...)}
 *       — membangun panel <i>"Verifikasi Nilai Rapor"</i>: satu <b>baris per mata pelajaran</b>
 *       hasil query tabel ini, dikali <b>kolom per kelas/semester</b> hasil parsing
 *       {@code GelombangPendaftaranPsb.getKelasVerifikasiRapor()} (format
 *       {@code "kelas:semester;kelas:semester;..."}). Setiap sel berisi nilai rapor + KKM +
 *       penanda "sudah diverifikasi", disimpan ke
 *       {@link ais.database.model.sekolah.CalonSiswaPunyaVerifikasiMatapelajaran}. Panel ini
 *       dipasang dari layar Calon Siswa dan dari formulir pendaftaran mandiri
 *       {@code PPDB_Simple8} (hanya untuk calon siswa yang sudah punya {@code id}).</li>
 *   <li>{@code ais.action.master.sekolah.CalonSiswaAction} — ekspor Excel <i>"Nilai Raport"</i>
 *       per gelombang, yang memakai query identik untuk menentukan urutan baris mata pelajaran
 *       pada lembar kerja.</li>
 * </ol>
 * <p>Kedua pembaca memakai kriteria yang sama persis:
 * {@code createCriteria(GelombangPendaftaranPsbPunyaMatapelajaran.class)} +
 * {@code Restrictions.eq("gelombangPendaftaranPsb", gel)} +
 * {@code Restrictions.eq("matapelajaranSekolah.aktif", true)} + urut {@code matapelajaranSekolah.nama},
 * dengan proyeksi hanya ke {@code matapelajaranSekolah.id}. Artinya baris penghubung ini
 * <b>tidak pernah dibaca sebagai objek</b> oleh alur bisnis — perannya murni sebagai penyaring
 * daftar mata pelajaran. Konsekuensinya: <b>menghapus satu baris di sini berarti kolom nilai
 * rapor mata pelajaran itu hilang dari formulir verifikasi seluruh calon siswa pada gelombang
 * tersebut</b>, sedangkan nilai yang terlanjur tersimpan di
 * {@code CalonSiswaPunyaVerifikasiMatapelajaran} tetap tertinggal sebagai data yatim yang tidak
 * pernah lagi tampil maupun ikut dirata-rata.</p>
 *
 * <h3>Efek samping pembacaan (non-obvious)</h3>
 * <p>Menampilkan panel verifikasi bersifat <b>menulis</b>, bukan sekadar membaca: untuk setiap
 * mata pelajaran yang terdaftar di sini dan belum punya pasangan baris
 * {@code CalonSiswaPunyaVerifikasiMatapelajaran} bagi calon siswa yang sedang dibuka, helper
 * langsung meng-INSERT baris kosongnya ({@code Common.refreshSaveOrUpdate(...)}). Menambah satu
 * baris di tabel ini karena itu menimbulkan gelombang INSERT baru sebanyak jumlah calon siswa
 * yang formulirnya dibuka setelahnya, dan ekspor Excel melakukan hal yang sama untuk
 * <b>seluruh</b> calon siswa pada gelombang sekaligus.</p>
 *
 * <h3>Struktur — hanya empat kolom nyata</h3>
 * <ul>
 *   <li>{@code id} — kunci utama {@code IDENTITY};</li>
 *   <li>{@code gelombang_pendaftaran_psb} — FK ke {@link GelombangPendaftaranPsb},
 *       <b>nullable</b> (tidak ada {@code nullable = false});</li>
 *   <li>{@code matapelajaran_sekolah} — FK ke {@link MatapelajaranSekolah}, wajib
 *       ({@code nullable = false});</li>
 *   <li>jejak audit {@code oleh}/{@code olehId}/{@code tanggal_dirubah}.</li>
 * </ul>
 * <p>Tidak ada kolom skalar bisnis sama sekali: tanpa {@code aktif}, tanpa {@code nomorUrut},
 * tanpa {@code keterangan}, tanpa bobot/KKM. KKM dan nilai tersimpan di baris transaksi
 * ({@code CalonSiswaPunyaVerifikasiMatapelajaran}), bukan di sini. Entity ini juga tidak memiliki
 * koleksi apa pun, sehingga <b>pola bug penciutan {@code TreeSet}</b> (yang menimpa
 * {@code GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa} lewat
 * {@code getNomorUrut()} yang tak pernah {@code null}) <b>tidak berlaku di sini</b> — tidak ada
 * {@code getNomorUrut()} maupun {@code SortedSet} yang bisa runtuh.</p>
 *
 * <h3>Tidak ada kolom tenant sendiri</h3>
 * <p>Baris ini tidak menyimpan {@code Sekolah}/{@code Yayasan}. Cakupan tenant hanya diwarisi
 * secara tidak langsung dari {@link GelombangPendaftaranPsb#getSekolah()};
 * {@link MatapelajaranSekolah} sendiri adalah katalog <b>global instalasi</b> (tanpa FK sekolah
 * maupun yayasan). Setiap query terhadap tabel ini karena itu <b>wajib</b> menyaring lewat
 * {@code gelombangPendaftaranPsb} untuk tetap berada di dalam tenant — dan layar pengelolanya
 * justru tidak melakukannya (lihat di bawah).</p>
 *
 * <h3>Layar pengelola dan gerbang hak akses — TIDAK ADA GERBANG SAMA SEKALI</h3>
 * <p>Layar {@code /pages/master/sekolah/gelombang_pendaftaran_psb_punya_matapelajaran.zul}
 * (controller {@code ais.action.master.sekolah.GelombangPendaftaranPsbPunyaMatapelajaranAction})
 * adalah satu-satunya UI CRUD entity ini, dan disisipkan sebagai tab <b>"Rapor"</b> di layar
 * Gelombang Pendaftaran PSB lewat {@code MyInclude(".../gelombang_pendaftaran_psb_punya_matapelajaran.zul?gelombangPendaftaranPsb=<id>")}.
 * Hasil verifikasi kode:</p>
 * <ul>
 *   <li>Controller-nya <b>nol {@code CommonPrivilages.checkPrevilages(...)}</b>. Flag
 *       {@code edit} dan {@code delete} di-<i>hardcode</i> {@code true} pada deklarasi field dan
 *       tidak pernah ditimpa, sehingga tombol Ubah dan Hapus selalu tampil untuk siapa pun.
 *       Bandingkan dengan layar induknya {@code GelombangPendaftaranPsbAction} yang justru
 *       BENAR: default {@code false}, lalu diisi dari
 *       {@code CommonPrivilages.checkPrevilages(CREATE/UPDATE/DELETE)}.</li>
 *   <li>Satu-satunya pemeriksaan yang ada adalah {@code Common.doCheckSecurity()} di
 *       {@code doBeforeCompose()}. Pemeriksaan itu bermuara ke
 *       {@code CommonPrivilages.doCheckPrevilagesRead()}, yang hanya menegakkan hak READ bila
 *       URL halaman ada di dalam array {@code MUST_CHECKED} berisi <b>12 URL modul perguruan
 *       tinggi</b> ({@code mahasiswa.zul}, {@code dosen.zul}, dst.). URL layar ini tidak
 *       termasuk, sehingga panggilan tersebut <b>tidak berefek apa pun</b> di sini.</li>
 *   <li>Tombol "Tambah" ({@code onAdd}) — yang membuka {@code AmbilDataMatapelajaranSekolahBanyak}
 *       dan menyimpan banyak baris sekaligus — juga tanpa gerbang.</li>
 *   <li>Combobox pencarian gelombang/mata pelajaran serta {@code onSearchDefault()} <b>tanpa
 *       filter tenant apa pun</b>: {@code Common.insertCombo(...)} memuat SELURUH
 *       {@code GelombangPendaftaranPsb} aktif dari seluruh sekolah/yayasan pada instalasi, dan
 *       grid menampilkan seluruh baris penghubung lintas tenant.</li>
 * </ul>
 * <p>Gabungannya membentuk pola yang sudah dikenal pada keluarga entity PSB — <i>pewarisan hak
 * lewat menu induk</i> ditambah <i>nol {@code checkPrevilages}</i> — dan menjadikan berkas ini
 * <b>instance keenam</b> setelah {@code RuangPSB},
 * {@code CalonSiswaPunyaVerifikasiParameter},
 * {@code GelombangPendaftaranPsbPunyaParameterVerifikasiCalonSiswa}/{@code InterviewPunyaCalonSiswa},
 * {@code RuangGelombangPendaftaranPsbPSB}, dan {@code VerifikasiPSBHelper}. Dampak nyatanya
 * bukan kebocoran data pribadi melainkan <b>perusakan gerbang seleksi</b>: menghapus baris di
 * sini melenyapkan syarat verifikasi rapor sebuah gelombang, menambah baris memaksa mata
 * pelajaran baru diverifikasi bagi seluruh pendaftar — pada gelombang milik sekolah mana pun.</p>
 *
 * <h3>Verifikasi jalur pra-otentikasi <code>/ppdb</code> — NEGATIF</h3>
 * <p>Dicek langsung terhadap seluruh berkas {@code WEB-INF/baru/modul/ppdb/*.jsp} (dispatcher
 * {@code /ppdb?hanya_tampil_jsp=true&p=ppdb&s=...} yang sudah tiga kali terbukti pra-otentikasi
 * pada {@code _wawancara_service}, {@code _ikut_ujian_online_service} dan {@code _sukses_login}):
 * <b>nol kemunculan</b> kata "matapelajaran" — entity ini tidak tersentuh sama sekali oleh jalur
 * JSP tersebut. Berkas scaffold {@code new/sekolah/services/...} dan
 * {@code new/sekolah/uiux/...} yang menyebut nama kelas ini hanyalah adaptor metadata hasil
 * generator ({@code generate_new_jsp_scaffold.py}) yang meneruskan ke {@code dispatcher.jsp}
 * tanpa satu pun akses data. Pembacaan lewat {@code PPDB_Simple8} berlangsung di dalam komponen
 * ZK dan hanya untuk calon siswa yang sudah tersimpan ({@code calonSiswa.getId() != null}).</p>
 *
 * <h3>Kuirk dan bom waktu yang terverifikasi</h3>
 * <ul>
 *   <li><b>Baris yatim ber-gelombang NULL.</b> Kolom {@code gelombang_pendaftaran_psb} nullable
 *       dan {@code onSave()} menyimpan apa pun yang terpilih di combobox. Bila layar dibuka
 *       <i>tanpa</i> parameter {@code gelombangPendaftaranPsb} (akses URL langsung, bukan lewat
 *       tab "Rapor"), {@code onAdd()} memakai {@code Restrictions.isNull("gelombangPendaftaranPsb")}
 *       dan baris yang tercipta ber-gelombang {@code null}. Baris seperti itu tidak pernah cocok
 *       dengan kriteria {@code eq("gelombangPendaftaranPsb", gel)} milik kedua pembaca, jadi
 *       tidak berefek apa pun — sampah diam.</li>
 *   <li><b>NPE di renderer daftar.</b> {@code PilihanGelombangPendaftaranPsbRenderer.render()}
 *       memanggil {@code getGelombangPendaftaranPsb().getNama()} <b>tanpa penjagaan null</b>
 *       (sementara {@code getMatapelajaranSekolah()} justru dijaga). Begitu satu baris yatim di
 *       atas terbentuk, seluruh grid layar ini gagal dirender.</li>
 *   <li><b>Tanpa unique constraint.</b> Tidak ada {@code @UniqueConstraint} atas pasangan
 *       (gelombang, mata pelajaran). Duplikat mungkin terjadi; dampaknya diredam kebetulan oleh
 *       proyeksi {@code Projections.property("matapelajaranSekolah.id")} +
 *       {@code ConstantValues.simpleList(...)} di sisi pembaca, tetapi grid layar pengelola akan
 *       menampilkan baris kembar.</li>
 *   <li><b>Label formulir salah tempel.</b> {@code init()} memberi label <i>"Jurusan Sekolah"</i>
 *       pada combobox mata pelajaran, dan judul jendela {@code addWindow} pada berkas
 *       {@code .zul} berbunyi <i>"Tambah Gelombang Registrasi Mahasiswa"</i> (sisa salin-tempel
 *       dari modul PMB perguruan tinggi). Kosmetik, tanpa dampak fungsional.</li>
 * </ul>
 *
 * <h3>Pemetaan Hibernate</h3>
 * <p>{@code @Entity} + {@code @org.hibernate.annotations.Entity(dynamicInsert = true,
 * dynamicUpdate = true)} sehingga SQL yang dihasilkan hanya memuat kolom yang benar-benar
 * berubah, dan {@code @Audited} (Hibernate Envers) sehingga setiap penambahan/penghapusan mata
 * pelajaran gelombang terekam di tabel revisi. Akses properti (anotasi pada getter), bukan
 * akses field.</p>
 *
 * <h3>Catatan pewarisan {@link ais.database.model.GeneralValueObject}</h3>
 * <p>Kelas induk <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} — ia POJO abstrak
 * biasa, sehingga Hibernate tidak memetakan satu pun properti miliknya. Deklarasi ulang
 * {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} di kelas ini karena itu
 * <b>bukan duplikasi yang keliru melainkan keharusan teknis</b>: tanpa deklarasi ulang tersebut
 * kolom-kolom itu tidak akan pernah dipetakan. Fasilitas yang tetap diwarisi dan dipakai di sini
 * adalah {@code check(...)} (lihat getter relasi) beserta kontrak {@code Comparable} yang
 * dipakai {@code Collections.sort(...)} saat mengisi combobox.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see GelombangPendaftaranPsb
 * @see MatapelajaranSekolah
 * @see CalonSiswaPunyaVerifikasiMatapelajaran
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "gelombang_pendaftaran_psb_punya_matapelajaran")
public class GelombangPendaftaranPsbPunyaMatapelajaran extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Nilainya dibangkitkan sekali oleh Hibernate Tools dan sengaja dipertahankan apa adanya.
	 * Instance entity ini ikut terserialisasi bersama state komponen ZK (grid layar pengelola,
	 * atribut baris panel verifikasi), sehingga mengubah nilai ini akan mematahkan sesi yang
	 * sudah terlanjur terserialisasi maupun replikasi antar-node.</p>
	 */
	private static final long serialVersionUID = 1463822577548439808L;

	/**
	 * Kunci utama baris penghubung, dibangkitkan basis data ({@code IDENTITY}).
	 *
	 * <p>Bernilai {@code null} selama objek belum disimpan; {@code onSave()} pada layar pengelola
	 * memakai kondisi tersebut untuk membedakan alur tambah dari alur ubah.</p>
	 */
	private Long id;

	/**
	 * Nama pengguna terakhir yang menyimpan baris ini, diisi otomatis oleh interceptor audit.
	 *
	 * <p>Dideklarasikan ulang di sini karena {@link ais.database.model.GeneralValueObject} bukan
	 * {@code @MappedSuperclass} (lihat Javadoc kelas).</p>
	 */
	private String oleh;

	/**
	 * Identitas (id pengguna) terakhir yang menyimpan baris ini, pendamping {@link #oleh}.
	 *
	 * <p>Disimpan sebagai {@code String} agar dapat menampung id dari beragam jenis akun
	 * (pegawai, guru, akun sistem) tanpa relasi FK.</p>
	 */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna penyimpan terakhir apa adanya, tanpa transformasi.
	 *
	 * @return id pengguna penyimpan terakhir, boleh {@code null} bila baris belum pernah melewati
	 *         interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi identitas pengguna penyimpan terakhir, dengan penjagaan "tolak nilai kosong".
	 *
	 * <p><b>Non-obvious:</b> setter ini <b>mengabaikan</b> argumen {@code null} maupun string
	 * yang hanya berisi spasi — nilai lama dipertahankan, bukan ditimpa. Perilaku ini disengaja
	 * agar jejak audit yang sudah benar tidak terhapus oleh alur penyimpanan yang kebetulan tidak
	 * membawa konteks pengguna (mis. penyimpanan dari proses batch atau dari alur PPDB anonim).
	 * Akibatnya jejak {@code olehId} tidak dapat dikosongkan lewat setter ini.</p>
	 *
	 * @param olehId id pengguna penyimpan; diabaikan sepenuhnya bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna penyimpan terakhir, dengan penjagaan "tolak nilai kosong" yang sama
	 * seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna penyimpan; diabaikan sepenuhnya bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna penyimpan terakhir apa adanya, tanpa transformasi.
	 *
	 * @return nama pengguna penyimpan terakhir, boleh {@code null}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate} yang menyegarkan jejak audit tepat sebelum baris diperbarui,
	 * sekaligus baris deklarasi field {@link #tanggal_dirubah}.
	 *
	 * <p><b>Tujuan.</b> Setiap kali Hibernate hendak menerbitkan {@code UPDATE} atas baris ini,
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} dipanggil untuk mengisi
	 * ulang {@code oleh}/{@code olehId} dari konteks pengguna aktif dan menyetel
	 * {@code tanggal_dirubah} ke waktu sekarang. Callback ini <b>tidak</b> berjalan pada
	 * {@code INSERT}; untuk baris baru nilai awal {@code tanggal_dirubah} berasal dari
	 * inisialisasi field ({@code ais.ui.util.WaktuUtil.getDate()}) yang dieksekusi saat objek
	 * dibentuk oleh {@code onAdd()}/{@code onSave()}.</p>
	 *
	 * <p><b>Non-obvious — dua deklarasi berbagi satu baris sumber.</b> Method callback dan
	 * deklarasi field {@code tanggal_dirubah} sengaja ditulis pada baris fisik yang sama
	 * (gaya penyisipan otomatis lintas berkas model di repo ini). Penataan ulang baris tersebut
	 * termasuk perubahan kode, bukan perubahan komentar, sehingga tidak dilakukan di sini.</p>
	 *
	 * <p><b>Efek samping.</b> Mengubah state objek yang sedang dipersistensi; tidak melakukan
	 * query maupun I/O sendiri. Dipanggil eksklusif oleh penyedia JPA — jangan dipanggil manual.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan waktu perubahan terakhir baris ini.
	 *
	 * <p>Dipakai terutama oleh {@code AuditTimestampInterceptor} dan oleh Hibernate saat memuat
	 * baris dari basis data. Nilai diterima apa adanya, tanpa validasi maupun normalisasi zona
	 * waktu.</p>
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Dipetakan sebagai {@code TIMESTAMP} sehingga bagian jam/menit/detik ikut tersimpan.
	 * Nilainya tidak pernah {@code null} untuk objek yang dibentuk di dalam JVM (field
	 * diinisialisasi saat konstruksi), tetapi bisa {@code null} untuk baris lama yang dimuat dari
	 * basis data dengan kolom kosong.</p>
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris penghubung dalam bentuk {@code "<gelombang>_<matapelajaran>"}.
	 *
	 * <p><b>Non-obvious — method ini menulis ke field.</b> Sebelum merangkai teks, kedua field
	 * relasi ditimpa dengan hasil getter-nya masing-masing
	 * ({@code gelombangPendaftaranPsb = getGelombangPendaftaranPsb();} dan padanannya). Karena
	 * getter tersebut memanggil {@code check(...)}, memanggil {@code toString()} atas objek yang
	 * relasinya masih berupa proxy lazy dapat memicu inisialisasi proxy, pengambilan dari
	 * {@code EntityIdentityMap}, bahkan pembacaan ulang lewat session terpisah. Konsekuensinya
	 * {@code toString()} <b>tidak bebas efek samping</b> dan tidak aman dipakai di dalam blok
	 * logging yang berjalan setelah session ditutup.</p>
	 *
	 * <p>Bagian teks dihasilkan oleh {@code toString()} masing-masing relasi
	 * ({@code MatapelajaranSekolah} mengembalikan {@code "id-nama"}), dan menghasilkan literal
	 * {@code "null"} bila relasinya kosong — termasuk untuk baris yatim ber-gelombang {@code null}
	 * yang dijelaskan pada Javadoc kelas.</p>
	 *
	 * @return teks gabungan kedua relasi, tidak pernah {@code null}
	 */
	public String toString() {
		gelombangPendaftaranPsb = getGelombangPendaftaranPsb();
		matapelajaranSekolah = getMatapelajaranSekolah();
		return gelombangPendaftaranPsb + "_" + matapelajaranSekolah;
	}

	/**
	 * Gelombang pendaftaran PSB pemilik baris ini — sisi "induk" relasi.
	 *
	 * <p>Boleh {@code null} secara skema; lihat catatan "baris yatim ber-gelombang NULL" pada
	 * Javadoc kelas.</p>
	 */
	private GelombangPendaftaranPsb gelombangPendaftaranPsb;

	/**
	 * Mata pelajaran sekolah yang nilai rapornya diverifikasi — sisi "anak" relasi.
	 *
	 * <p>Wajib terisi ({@code nullable = false} pada kolom join). Katalog sumbernya bersifat
	 * global instalasi, tanpa FK sekolah/yayasan.</p>
	 */
	private MatapelajaranSekolah matapelajaranSekolah;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA untuk instansiasi reflektif.
	 *
	 * <p>Dipakai pula secara langsung oleh {@code GelombangPendaftaranPsbPunyaMatapelajaranAction.onAdd()}
	 * saat membentuk baris baru bagi setiap mata pelajaran yang dipilih pengguna pada dialog
	 * {@code AmbilDataMatapelajaranSekolahBanyak}. Objek yang dihasilkan belum lengkap: kedua
	 * relasi masih {@code null} dan harus diisi lewat setter sebelum disimpan.</p>
	 */
	public GelombangPendaftaranPsbPunyaMatapelajaran() {
	}

	/**
	 * Mengembalikan kunci utama baris penghubung.
	 *
	 * <p>Kolom dipetakan dengan {@code insertable = false} karena nilainya sepenuhnya dibangkitkan
	 * basis data ({@code IDENTITY}); nilai apa pun yang diisikan lewat {@link #setId(Long)} tidak
	 * akan ikut dalam pernyataan {@code INSERT}.</p>
	 *
	 * @return id baris, {@code null} bila objek belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama baris penghubung.
	 *
	 * <p>Praktisnya hanya dipakai Hibernate saat memuat/menyimpan baris. Kode aplikasi tidak boleh
	 * menetapkan id secara manual karena kolomnya {@code insertable = false}.</p>
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan gelombang pendaftaran PSB pemilik baris ini.
	 *
	 * <p><b>Non-obvious — getter ini menulis balik ke field.</b> Sebelum mengembalikan nilai,
	 * hasil {@code check(gelombangPendaftaranPsb)} warisan
	 * {@link ais.database.model.GeneralValueObject} ditugaskan kembali ke field yang sama.
	 * {@code check(...)} berfungsi sebagai jaring pengaman {@code LazyInitializationException}:
	 * ia mengembalikan objek kanonik dari {@code EntityIdentityMap}, atau objek dari cache yang
	 * aman, atau menginisialisasi proxy pada session aktif, atau — sebagai upaya terakhir —
	 * membaca ulang entity lewat session terpisah. Dua akibat yang perlu diingat: (a) instance
	 * yang dikembalikan bisa <b>berbeda referensi</b> dari yang tadinya dipegang, dan karena
	 * pemetaan memakai akses properti, referensi baru itulah yang dilihat pengecekan <i>dirty</i>
	 * Hibernate pada penyimpanan berikutnya; (b) sekadar membaca relasi ini dapat memicu query.
	 * Berbeda dengan pola "getter destruktif" pada beberapa entity lain, {@code check(...)}
	 * <b>tidak pernah mengubah nilai non-null menjadi {@code null}</b> — argumen {@code null}
	 * dikembalikan apa adanya — sehingga getter ini tidak berisiko mengosongkan FK.</p>
	 *
	 * <p>Relasi bersifat {@code LAZY} dengan {@code cascade = {PERSIST, MERGE}}: menyimpan baris
	 * penghubung ikut mem-persist/merge gelombangnya, tetapi <b>tidak</b> ikut menghapusnya
	 * (tidak ada {@code CascadeType.REMOVE}).</p>
	 *
	 * @return gelombang pendaftaran pemilik, boleh {@code null} untuk baris yatim
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gelombang_pendaftaran_psb")
	public GelombangPendaftaranPsb getGelombangPendaftaranPsb() {
		gelombangPendaftaranPsb = check(gelombangPendaftaranPsb);
		return gelombangPendaftaranPsb;
	}

	/**
	 * Menetapkan gelombang pendaftaran PSB pemilik baris ini.
	 *
	 * <p>Dipanggil dari {@code onAdd()} (memakai gelombang yang dibawa parameter URL
	 * {@code gelombangPendaftaranPsb}, bisa {@code null} bila layar dibuka tanpa parameter) dan
	 * dari {@code onSave()} (memakai item combobox terpilih). Tidak ada validasi: nilai
	 * {@code null} diterima dan menghasilkan baris yatim yang tidak pernah terbaca alur bisnis
	 * mana pun.</p>
	 *
	 * @param gelombangPendaftaranPsb gelombang pemilik, boleh {@code null}
	 */
	public void setGelombangPendaftaranPsb(GelombangPendaftaranPsb gelombangPendaftaranPsb) {
		this.gelombangPendaftaranPsb = gelombangPendaftaranPsb;
	}

	/**
	 * Mengembalikan mata pelajaran sekolah yang nilai rapornya diverifikasi pada gelombang ini.
	 *
	 * <p>Sama seperti {@link #getGelombangPendaftaranPsb()}, getter ini <b>menulis balik</b> hasil
	 * {@code check(...)} ke field-nya (lihat penjelasan lengkap di getter tersebut). Relasi
	 * {@code LAZY} dengan {@code cascade = {PERSIST, MERGE}}.</p>
	 *
	 * <p>Meski kolom join dideklarasikan {@code nullable = false}, getter tetap dapat
	 * mengembalikan {@code null} untuk objek yang baru dibentuk di memori dan belum diisi
	 * setter-nya — karena itu {@code PilihanGelombangPendaftaranPsbRenderer} menjaga nilai null di
	 * sisi ini (dan, secara tidak konsisten, tidak menjaganya di sisi gelombang).</p>
	 *
	 * @return mata pelajaran terkait
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "matapelajaran_sekolah", nullable = false)
	public MatapelajaranSekolah getMatapelajaranSekolah() {
		matapelajaranSekolah = check(matapelajaranSekolah);
		return matapelajaranSekolah;
	}

	/**
	 * Menetapkan mata pelajaran sekolah yang nilai rapornya diverifikasi pada gelombang ini.
	 *
	 * <p>Dipanggil dari {@code onAdd()} untuk setiap mata pelajaran yang dipilih pengguna pada
	 * dialog pemilihan banyak, dan dari {@code onSave()} untuk item combobox terpilih. Tidak ada
	 * validasi anti-duplikat: menyimpan pasangan (gelombang, mata pelajaran) yang sudah ada akan
	 * menghasilkan baris kembar karena tabel tidak memiliki <i>unique constraint</i>.</p>
	 *
	 * @param matapelajaranSekolah mata pelajaran terkait; semestinya tidak {@code null} karena
	 *                             kolom join bersifat wajib
	 */
	public void setMatapelajaranSekolah(MatapelajaranSekolah matapelajaranSekolah) {
		this.matapelajaranSekolah = matapelajaranSekolah;
	}

}
