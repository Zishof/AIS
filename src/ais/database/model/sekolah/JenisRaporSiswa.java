package ais.database.model.sekolah;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import org.json.JSONArray;

import ais.common.ConstantValues;
import ais.database.model.GeneralValueObject;

/**
 * Master <b>profil cetak rapor siswa</b> pada modul sekolah — tabel
 * {@code sekolah.jenis_rapor_siswa}.
 *
 * <h3>Peran domain (TERVERIFIKASI dari kode, bukan dari namanya)</h3>
 * <p>Nama kelasnya mudah disalahartikan sebagai daftar enumerasi "jenis rapor"
 * (tengah semester / akhir semester / kenaikan kelas). <b>Bukan itu.</b> Satu baris entity ini
 * adalah <b>satu variasi cetak rapor yang siap dipilih</b> pada layar
 * {@code ais.action.report.format1.sekolah.LaporanRaporSiswa}, dan isinya menentukan dua hal
 * sekaligus:</p>
 * <ol>
 * <li><b>LAYOUT</b> — berkas template JasperReports ({@code .jrxml}/{@code .jasper}) yang dipakai
 * mencetak. Berkasnya <b>tidak</b> disimpan pada kolom entity ini, melainkan pada
 * {@code ais.database.model.file.LampiranLain} dengan {@code ref = } id baris ini dan
 * {@code jenis = LampiranLain.FILE_JRXML_LAYOUT_JENIS_RAPOR} ("File jrxml jenis rapor"). Baris
 * ini yang menjadi <i>kunci pencariannya</i>. Gambar/latar pendukung memakai mekanisme yang sama
 * dengan {@code jenis} berawalan {@code "Jenis_Rapor_Siswa_"}.</li>
 * <li><b>RUANG LINGKUP ISI</b> — mata pelajaran mana yang dikeluarkan
 * ({@link #getMpYgTidakDiambil()}), blok data non-nilai mana yang ikut ditarik (lima bendera
 * {@code ambil*}), apakah rapor bersifat kumulatif lintas tingkat dan lintas semester
 * ({@link #getUntukSemuaSemester()}), dan apakah rata-rata kelas ikut dihitung
 * ({@link #getHitungRataRataKelas()}).</li>
 * </ol>
 * <p>Periode rapor (tahun ajaran, semester) <b>tidak</b> disimpan di sini — itu dipilih terpisah
 * pada combobox layar laporan dan diteruskan sebagai argumen
 * {@code LaporanRaporSiswa.generateParameter(..., ta, smtas, smta, ...)}. Jadi satu baris entity
 * ini dapat dipakai ulang untuk semester mana pun; yang membedakan antar baris adalah template
 * dan lingkup isinya, bukan periodenya. Kalaupun sebuah instalasi menamai barisnya "Rapor
 * Tengah Semester" atau "Rapor Kenaikan Kelas", perbedaan perilakunya hanya sejauh perbedaan
 * berkas jrxml dan bendera-bendera di bawah — sistem tidak memberi makna khusus pada
 * {@link #getNama()}.</p>
 *
 * <h3>Hubungan dengan mesin penilaian dan mesin nilai huruf — hasil verifikasi</h3>
 * <ul>
 * <li><b>Rantai penilaian 8 entity</b> ({@code JenisPenilaian} → {@code DetailJenisPenilaian} →
 * {@code GrupPenilaian} → {@code DetailGrupPenilaian} → {@code GrupKategoriItemPenilaianSiswa} →
 * {@code DetailGrupKategoriItemPenilaianSiswa} → {@code KategoriItemPenilaianSiswa} →
 * {@code JenisItemPenilaianSiswa}): entity ini <b>tidak punya FK ke satu pun di antaranya</b>.
 * Ia duduk <i>di atas</i> rantai itu sebagai penyaring dan penyetel, bukan sebagai anggota.
 * Rantai tetap ditelusuri sendiri oleh {@code LaporanRaporSiswa} berangkat dari
 * {@code KurikulumPunyaMatapelajaran} → {@code Matapelajaran.jenisPenilaian}. Sentuhan entity ini
 * atas rantai itu ada dua dan hanya dua: (1) {@link #ambilMk()} membuang sebagian
 * {@code Matapelajaran} sebelum rantai ditelusuri, dan (2) {@link #getHitungRataRataKelas()}
 * menyalakan perhitungan rata-rata sekelas atas tiap {@code GrupPenilaian}.</li>
 * <li><b>Mesin konversi nilai huruf</b> {@code NilaiHurufSekolah}: <b>TIDAK ADA relasi sama
 * sekali</b> — verifikasi negatif yang penting. Konversi angka → huruf pada rapor dipanggil
 * {@code LaporanRaporSiswa} lewat
 * {@code NilaiHurufSekolah.getNilaiHurufSekolah(total, siswa.getTahunMasuk(), siswa.getSekolah(),
 * siswa.getYayasan(), tahunAjaran, ganjil/genap, grupPenilaian.getJenisNilaiHuruf())}. Tidak
 * satu pun argumennya berasal dari entity ini; pemilih skala huruf adalah
 * {@code GrupPenilaian.getJenisNilaiHuruf()}. Artinya <b>mengganti jenis rapor tidak pernah
 * mengubah skala huruf</b> — dua template berbeda atas data yang sama selalu menghasilkan huruf
 * yang sama.</li>
 * </ul>
 *
 * <h3>Siapa yang memakai</h3>
 * <ul>
 * <li><b>Layar master</b> {@code ais.action.master.sekolah.JenisRaporSiswaAction} +
 * {@code /pages/master/sekolah/jenis_rapor_siswa.zul} — CRUD penuh, unggah berkas jrxml, unggah
 * gambar pendukung, dan pemilihan mata pelajaran yang dikecualikan lewat deretan
 * {@code Checkbox}. Grid-nya menampilkan kolom Nama / Sekolah / Keterangan / Tampil ke siswa /
 * Semua Smt / {@code tingkatKebelakang} / Rata-Rata Kelas / Absen / Kegiatan-Prestasi /
 * Pelanggaran / Apresiasi / Catatan / Aktif, seluruhnya dapat disunting langsung di baris grid
 * (tiap {@code onCheck}/{@code onChange} memanggil {@code Common.refreshSaveOrUpdate(...)},
 * jadi <b>tersimpan seketika tanpa tombol Simpan dan tanpa dialog konfirmasi</b>).
 * <b>Halaman ini tidak terdaftar sebagai menu mana pun</b> — lihat bagian Hak akses.</li>
 * <li><b>Layar laporan</b> {@code ais.action.report.format1.sekolah.LaporanRaporSiswa} —
 * (a) mengisi combobox "Jenis Rapor *" dari entity ini, (b) menyisipkan layar master di atas
 * sebagai tab kedua "Jenis Rapor", (c) memakai baris terpilih untuk memilih template dinamis di
 * {@code generateParameter(...)} dan {@code onReport(...)}, dan (d) membaca seluruh bendera saat
 * merakit parameter Jasper.</li>
 * <li><b>REST</b> {@code ais.action.servlet.api.LaporanApi.raport_siswa(...)} — jalur rapor untuk
 * aplikasi siswa; menerima {@code jenisRaporSiswa} berupa <b>id mentah</b> dari klien. Lihat
 * peringatan pada bagian Hak akses.</li>
 * <li><b>Pramuat cache</b> {@code ais.common.InitData} — kelasnya terdaftar pada daftar
 * {@code initClasses(...)} sehingga barisnya dimuat ke cache aplikasi saat startup; itulah yang
 * membuat {@code ConstantValues.ambil(JenisRaporSiswa.class.getName(), id)} bisa meresolusi baris
 * tanpa query. Murni pramuat — <b>tidak ada auto-seed</b>, instalasi baru mulai tanpa satu baris
 * pun sehingga combobox "Jenis Rapor" kosong dan laporan jatuh ke template statis
 * {@code "sekolah/report"}.</li>
 * <li><b>New UI (Generic CRUD v2)</b> — nama kelas ini tercantum pada {@code nuiServiceEntities}
 * berkas {@code webapp/WEB-INF/new/root/report/services/format1/sekolah/
 * laporan_rapor_siswa_service.jsp}, sehingga ikut diauto-registrasi oleh
 * {@code GenericCrudDefinitionRegistry}. <b>Kontras positif</b> terhadap temuan
 * {@code task_7b6038ac}: entity ini punya properti bernama persis {@code sekolah} dan
 * {@code yayasan}, dua di antara enam nama institusi yang selalu dipasang tanpa syarat peran oleh
 * {@code GenericCrudAutoEntityAdapter.scopeBindings()} — jadi jalur New UI-nya <b>tidak</b>
 * termasuk kandidat rentan pola whitelist-nama-properti itu.</li>
 * </ul>
 *
 * <h3>Pengelompokan anggota kelas ini</h3>
 * <ol>
 * <li><b>Identitas &amp; audit</b> — {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()},
 * {@link #getTanggal_dirubah()}, {@link #onUpdate()}, {@link #toString()}. Field-nya
 * dideklarasikan ulang di sini (lihat catatan {@link GeneralValueObject} di bawah).</li>
 * <li><b>Cakupan tenant</b> — {@link #getSekolah()} dan {@link #getYayasan()}; yayasan diturunkan
 * ulang dari sekolah pada setiap pembacaan.</li>
 * <li><b>Atribut deskriptif</b> — {@link #getKode()}, {@link #getNama()},
 * {@link #getKeterangan()}, {@link #getAktif()}.</li>
 * <li><b>Pengecualian mata pelajaran</b> — {@link #getMpYgTidakDiambil()} (JSON array id),
 * {@link #ambilMk()} (pengurainya), {@link #filterMk(List, Matapelajaran)} (kode mati, lihat
 * Javadocnya), dan konstanta bantu {@link #array}.</li>
 * <li><b>Penyetel isi rapor</b> — {@link #getUntukSemuaSemester()},
 * {@link #getHitungRataRataKelas()}, {@link #getTingkatKebelakang()} (tidak pernah dibaca mesin
 * laporan — lihat Javadocnya).</li>
 * <li><b>Bendera penarikan blok data non-nilai</b> — {@link #getAmbilAbsenPiket()},
 * {@link #getAmbilKegiatanSiswa()}, {@link #getAmbilPelanggaranSiswa()},
 * {@link #getAmbilApresiasiSiswa()}, {@link #getAmbilCatatanSiswa()}.</li>
 * <li><b>Visibilitas ke portal siswa</b> — {@link #getTampilKeSiswa()}.</li>
 * </ol>
 * <p>Tidak ada koleksi dan tidak ada query di kelas ini. Satu-satunya logika nyata ada pada
 * {@link #ambilMk()}, {@link #getYayasan()}, dan {@link #filterMk(List, Matapelajaran)}.</p>
 *
 * <h3>Konsekuensi <i>property access</i>: nilai bawaan getter ikut tersimpan</h3>
 * <p>{@code @Id} dipasang pada getter, sehingga Hibernate membaca seluruh properti entity ini
 * lewat method — bukan lewat field. Akibatnya setiap "nilai bawaan" yang dikarang getter saat
 * field-nya {@code null} <b>ikut tertulis ke kolom</b> pada INSERT/UPDATE pertama yang menyentuh
 * baris ini: {@link #getKode()} menulis {@code ""} (bukan {@code NULL}),
 * {@link #getMpYgTidakDiambil()} menulis {@code "[]"}, {@link #getAktif()} dan
 * {@link #getTampilKeSiswa()} dan {@link #getAmbilCatatanSiswa()} menulis {@code true},
 * {@link #getTingkatKebelakang()} menulis {@code 1}, sedangkan {@link #getUntukSemuaSemester()},
 * {@link #getHitungRataRataKelas()} dan empat bendera {@code ambil*} lain menulis {@code false}.
 * Jadi perbedaan antara "belum pernah disetel" dan "sengaja disetel ke nilai bawaan" <b>hilang
 * permanen</b> setelah penyimpanan pertama, dan mengubah nilai bawaan pada kode di kemudian hari
 * tidak akan memengaruhi baris lama.</p>
 *
 * <h3>Catatan penting tentang {@link GeneralValueObject}</h3>
 * <p>Induknya <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} — hanya POJO abstrak
 * biasa, sehingga Hibernate <b>tidak</b> memetakan satu pun properti induk. Karena itu field
 * {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} <b>wajib</b>
 * dideklarasikan ulang di setiap entity turunan. Duplikasi ini <b>bukan bug</b>, melainkan
 * keharusan teknis; jangan "dirapikan" dengan memindahkannya ke induk. Yang tetap diwarisi adalah
 * helper statisnya, terutama {@code check(...)} yang dipakai kedua getter relasi di sini.</p>
 *
 * <h3>Pola arsitektur berulang — hasil verifikasi pada berkas ini</h3>
 * <ul>
 * <li><b>Getter dengan efek tulis balik (write-back)</b> — <b>ADA, satu yang mengubah nilai
 * kolom</b>: {@link #getYayasan()} menghitung ulang {@code yayasan} dari
 * {@code sekolah.getYayasan()} setiap kali dibaca dan menugaskannya ke field. Karena entity ini
 * dibaca lewat property access, sekadar merender satu baris grid atau satu item combobox dapat
 * berubah menjadi UPDATE nyata pada kolom {@code yayasan_id} plus satu revisi Envers baru
 * (kelasnya {@code @Audited}) yang tercatat "diubah oleh" pengguna yang cuma membuka layar.
 * {@link #getSekolah()} juga menulis ke field, tetapi hanya menukar proxy lazy dengan instance
 * teresolusi — baris logis yang sama, nilai kolom tidak berubah.</li>
 * <li><b>Getter destruktif yang mengosongkan data</b> (pola {@code KelasSiswaPSB.getNama()}) —
 * <b>TIDAK ADA</b>. {@link #getKode()}, {@link #getNama()} dan {@link #getMpYgTidakDiambil()}
 * memang menormalkan hasil bacaan (trim / nilai bawaan), tetapi tidak menulis balik ke field
 * sehingga tidak ada data yang hilang di memori. Yang perlu disadari adalah efek <i>property
 * access</i> pada bagian sebelumnya: normalisasi itu tetap berakhir di kolom.</li>
 * <li><b>{@code getKeterangan()} yang membalik kontraknya</b> — <b>TIDAK ADA</b>;
 * {@link #getKeterangan()} adalah getter polos.</li>
 * <li><b>Penciutan {@code TreeSet}</b> — <b>TIDAK RELEVAN</b>: kelas ini tidak punya koleksi.</li>
 * <li><b>Cakupan tenant fail-open</b> — <b>ADA, tiga varian</b> (layar master, combobox laporan,
 * dan REST). Lihat bagian Hak akses.</li>
 * <li><b>Pewarisan hak lewat menu induk</b> — <b>ADA, dan di sini bersifat STRUKTURAL</b>: layar
 * master entity ini tidak punya menu sendiri sama sekali. Lihat bagian Hak akses.</li>
 * <li><b>SQL injection lewat nama baris master yang dirangkai jadi alias kolom SQL native di
 * dasbor</b> — <b>TIDAK ADA</b>: nol rujukan ke kelas ini dari seluruh paket
 * {@code ais.action.dashboard.**}; entity ini tidak pernah menjadi sumber alias kolom.</li>
 * </ul>
 *
 * <h3>Hak akses dan cakupan tenant</h3>
 * <p><b>Master tanpa menu: seluruh CRUD-nya diwarisi dari menu laporan.</b> Berkas
 * {@code /pages/master/sekolah/jenis_rapor_siswa.zul} <b>tidak terdaftar di
 * {@code ais.common.MenuInitializer} maupun di seed menu mana pun</b>; satu-satunya pintu
 * masuknya adalah tab "Jenis Rapor" yang dibuat {@code LaporanRaporSiswa.init()} lewat
 * {@code new MyInclude("/pages/master/sekolah/jenis_rapor_siswa.zul")}. Sisipan itu berbagi
 * halaman ZK yang sama, sedangkan {@code CommonPrivilages.checkPrevilages(...)} menentukan hak
 * dari {@code Common.getCurrentMenu()} — yaitu menu <b>"Rapor Siswa"</b> (id 127616,
 * {@code MenuInitializer} baris 91, {@code child} 73023 di bawah {@code root} 73). Jadi hak
 * TAMBAH/UBAH/HAPUS atas master ini sesungguhnya diberikan oleh hak atas menu laporan rapor.
 * Yang membuat instance pola ini lebih berat daripada instance-instance sebelumnya adalah
 * <b>apa</b> yang bisa dilakukan begitu tab terbuka: mengunggah berkas
 * {@code .jrxml}/{@code .jasper} yang kemudian <b>dikompilasi dan dieksekusi</b> oleh
 * {@code Report.generateCompileFileReport(...)} di server. Template JasperReports memuat
 * ekspresi Java dan dapat memuat <i>scriptlet</i>; unggahannya karena itu setara dengan
 * kemampuan menaruh logika baru di server, bukan sekadar mengganti tata letak.
 * Peredam satu-satunya: blok tab hanya dibangun bila
 * {@code tbmuser.getSiswa() == null && tbmuser.ambilGuru() == null}, sehingga pengguna berperan
 * siswa dan guru tidak melihatnya. Peran staf/admin mana pun yang diberi menu laporan rapor
 * memperolehnya utuh.</p>
 *
 * <p><b>Layar master: gerbang CRUD benar, tapis tenant nol.</b> {@code JenisRaporSiswaAction}
 * memanggil {@code Common.doCheckSecurity()} di {@code doBeforeCompose}, menyembunyikan tombol
 * Tambah tanpa hak {@code CREATE}, dan menonaktifkan seluruh kendali baris tanpa hak
 * {@code UPDATE}/{@code DELETE}. Namun {@code initCriteria()} hanya memasang pembatas untuk
 * nilai yang <i>dipilih pengguna</i> pada combobox Yayasan/Sekolah; bila dibiarkan kosong
 * pembatasnya menjadi {@code Restrictions.sqlRestriction("1=1")}. Tidak ada pembatas bawaan ke
 * sekolah milik pengguna, sehingga daftar yang tampil — dan yang dapat disunting/dihapus —
 * mencakup profil rapor <b>seluruh sekolah dan yayasan</b> pada satu instalasi. Sama seperti
 * instance-instance sebelumnya: bukan tapis yang gagal terbuka, melainkan tapis yang memang tidak
 * pernah ditulis.</p>
 *
 * <p><b>Combobox laporan: fail-open saat sekolah belum dipilih.</b> Pengisi combobox "Jenis
 * Rapor" memakai {@code s == null || s.getId() == null ? Restrictions.sqlRestriction("true") :
 * Restrictions.eq("sekolah", s)}. Selama combobox Sekolah belum terisi (atau resolusi tenant
 * mengembalikan objek ber-id {@code null} — akar struktural yang sudah tercatat pada batch 67),
 * daftar jenis rapor yang ditawarkan berisi seluruh instalasi.</p>
 *
 * <p><b>REST {@code LaporanApi.raport_siswa}: id mentah tanpa cek kepemilikan.</b> Jalur ini
 * mewajibkan token siswa yang sah ({@code tbmuser.getSiswa() != null}) dan — penting — data nilai
 * yang dicetak tetap dikunci ke siswa pemilik token ({@code Restrictions.eq("siswa",
 * tbmuser.getSiswa())}), sehingga <b>bukan</b> kebocoran nilai antarsiswa. Yang tidak diperiksa
 * adalah baris entity ini: id yang dikirim klien diresolusi apa adanya lewat
 * {@code ConstantValues.ambil(JenisRaporSiswa.class.getName(), id)} <b>tanpa</b> dibandingkan
 * dengan {@code tbmuser.ambilSekolah()}, <b>tanpa</b> memeriksa {@link #getAktif()}, dan
 * <b>tanpa</b> memeriksa {@link #getTampilKeSiswa()}. Padahal jalur ZK yang setara memasang
 * ketiganya (lihat {@link #getTampilKeSiswa()}). Konsekuensi yang terverifikasi: seorang siswa
 * dapat (1) melewati bendera {@code tampilKeSiswa = false} sehingga merender template yang
 * sengaja dirahasiakan dari siswa (mis. rapor draf berisi catatan wali kelas), (2) memakai
 * template milik sekolah/yayasan lain — yang berarti tata letak, teks tetap, dan gambar/kop milik
 * tenant lain terbaca dari luar, dan (3) menyalakan blok data yang oleh sekolahnya sendiri sengaja
 * dimatikan (pelanggaran, apresiasi, catatan) hanya dengan menunjuk baris jenis rapor lain yang
 * benderanya menyala. Kategorinya sama dengan IDOR REST yang sudah dicatat pada
 * {@code task_493423ef}/{@code task_5e93a600} — <b>tidak</b> dibuatkan task baru, tetapi
 * perbaikannya sederhana: batasi resolusi id ke {@code sekolah} milik token dan wajibkan
 * {@code aktif = true} serta {@code tampilKeSiswa = true} persis seperti jalur ZK.</p>
 *
 * <h3>Kejanggalan lain yang sudah diverifikasi</h3>
 * <ul>
 * <li>{@link #getTingkatKebelakang()} adalah <b>konfigurasi mati</b> — disunting di grid,
 * disimpan, ikut diekspor, tetapi tidak pernah dibaca mesin laporan. Rinciannya pada
 * Javadoc method itu.</li>
 * <li>{@link #getAmbilCatatanSiswa()} bernilai bawaan {@code true} sementara empat bendera
 * {@code ambil*} lainnya bernilai bawaan {@code false} — asimetri yang tidak berdokumen dan mudah
 * mengejutkan.</li>
 * <li>Satu jalur pengambilan {@code ApresiasiSiswa} di {@code LaporanRaporSiswa} (sekitar baris
 * 1555) memakai {@link #getAmbilPelanggaranSiswa()} sebagai gerbang, bukan
 * {@link #getAmbilApresiasiSiswa()} — bug yang sudah tercatat pada Javadoc
 * {@code ApresiasiSiswa}.</li>
 * <li>{@link #filterMk(List, Matapelajaran)} adalah salinan ketiga dari penyaring yang sama dan
 * <b>tidak dipakai siapa pun</b>. Lihat Javadocnya.</li>
 * </ul>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.action.master.sekolah.JenisRaporSiswaAction
 * @see ais.action.report.format1.sekolah.LaporanRaporSiswa
 * @see ais.database.model.file.LampiranLain#FILE_JRXML_LAYOUT_JENIS_RAPOR
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "jenis_rapor_siswa")
public class JenisRaporSiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya dipatok agar sesi ZK maupun cache pramuat
	 * {@code ais.common.InitData} yang berisi objek versi lama tidak menolak instance baru dengan
	 * {@code InvalidClassException}; entity ini dipegang layar master, layar laporan, dan
	 * combobox-nya sehingga ikut terserialisasi.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * Kunci utama baris, dibangkitkan basis data ({@code IDENTITY}). Dideklarasikan ulang di sini
	 * karena {@link GeneralValueObject} tidak dipetakan Hibernate.
	 */
	private Long id;
	/**
	 * Nama pengguna terakhir yang mengubah baris ini; diisi otomatis oleh
	 * {@code ais.database.hibernate.AuditTimestampInterceptor} lewat {@link #onUpdate()}.
	 * Dideklarasikan ulang karena induknya tidak dipetakan Hibernate.
	 */
	private String oleh;
	/**
	 * Id pengguna terakhir yang mengubah baris ini; pasangan teknis dari {@link #oleh} dan diisi
	 * dari sumber yang sama. Dideklarasikan ulang karena induknya tidak dipetakan Hibernate.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna pengubah, atau {@code null} bila baris belum pernah melewati UPDATE
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Menolak nilai kosong.</b> Argumen {@code null} atau yang hanya berisi spasi diabaikan
	 * diam-diam sehingga nilai lama tetap bertahan. Ini disengaja: jejak audit tidak boleh
	 * terhapus oleh alur yang kebetulan tidak mengenal pengguna (thread latar, pekerjaan
	 * terjadwal, pencetakan lewat REST). Konsekuensinya jejak audit <b>tidak dapat dikosongkan</b>
	 * lewat setter ini.</p>
	 *
	 * @param olehId id pengguna pengubah; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Berperilaku sama dengan
	 * {@link #setOlehId(String)}: nilai {@code null}/kosong diabaikan sehingga jejak audit yang
	 * sudah ada tidak dapat dihapus.
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah, atau {@code null} bila baris belum pernah melewati UPDATE
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum pernyataan UPDATE baris ini
	 * dijalankan, lalu meneruskan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #oleh}, {@link #olehId}, dan {@code tanggal_dirubah} dari konteks pengguna aktif.
	 *
	 * <p><b>Efek samping:</b> mengubah tiga field audit entity ini. Jangan dipanggil manual — ini
	 * kait lifecycle, bukan API.</p>
	 *
	 * <p><b>Catatan:</b> karena {@link #getYayasan()} menulis balik ke field-nya sendiri,
	 * pembacaan biasa dapat membuat <i>dirty checking</i> menemukan perubahan dan kait ini ikut
	 * berjalan — sehingga baris dapat tercatat "diubah oleh X" tanpa X pernah menyunting apa pun.
	 * Karena kelas ini {@code @Audited}, setiap kejadian seperti itu juga melahirkan satu revisi
	 * Envers baru.</p>
	 *
	 * <p><b>Perhatian pembaca:</b> deklarasi field {@code tanggal_dirubah} ditulis pada
	 * <i>baris fisik yang sama</i> dengan method ini (gaya asli berkas). Field itu diinisialisasi
	 * dengan waktu "sekarang" versi aplikasi ({@code ais.ui.util.WaktuUtil.getDate()}, bukan
	 * {@code new Date()}) agar mengikuti zona waktu dan penyetelan jam institusi, lalu diperbarui
	 * kait ini pada setiap UPDATE. Ia juga dideklarasikan ulang karena induknya tidak dipetakan
	 * Hibernate.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Umumnya tidak perlu dipanggil manual — nilainya
	 * diisi otomatis oleh {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah stempel waktu baru; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return stempel waktu perubahan terakhir (tanggal + jam)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini dalam bentuk {@code "<id>-<nama>"}.
	 *
	 * <p>Membaca field {@code nama} <b>langsung</b>, bukan lewat {@link #getNama()}, sehingga
	 * hasilnya tidak di-{@code trim} dan dapat berisi {@code "null"} untuk baris yang namanya
	 * belum diisi. Dipakai oleh log/diagnostik dan oleh komponen ZK yang menampilkan objek apa
	 * adanya; label combobox "Jenis Rapor" <b>tidak</b> memakai method ini melainkan dirangkai
	 * {@code Common.insertCombo(..., new String[] { "nama", "kode" }, "keterangan", ...)}.</p>
	 *
	 * @return {@code "<id>-<nama>"}; {@code id} berisi {@code "null"} untuk baris yang belum
	 *         disimpan
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Kode singkat profil rapor ini. Tidak ada anotasi {@code @Column}, sehingga dipetakan
	 * otomatis ke kolom bernama sama. Lihat {@link #getKode()}.
	 */
	private String kode;

	/** Nama profil rapor — kolom {@code nama} {@code text} {@code nullable = false}. */
	private String nama;
	/** Sekolah pemilik baris ini; FK opsional ({@code sekolah_id}). Lihat {@link #getSekolah()}. */
	private Sekolah sekolah;
	/**
	 * Yayasan pemilik baris ini; FK opsional ({@code yayasan_id}) yang <b>diturunkan ulang</b>
	 * dari {@link #sekolah} setiap kali {@link #getYayasan()} dipanggil.
	 */
	private Yayasan yayasan;
	/** Keterangan bebas; ikut menjadi deskripsi item pada combobox layar laporan. */
	private String keterangan;
	/** Bendera visibilitas ke portal/aplikasi siswa. Lihat {@link #getTampilKeSiswa()}. */
	private Boolean tampilKeSiswa;
	/** Bendera perhitungan rata-rata sekelas. Lihat {@link #getHitungRataRataKelas()}. */
	private Boolean hitungRataRataKelas;
	/** Bendera mode rapor kumulatif. Lihat {@link #getUntukSemuaSemester()}. */
	private Boolean untukSemuaSemester;

	/** Bendera penarikan rekap kehadiran. Lihat {@link #getAmbilAbsenPiket()}. */
	private Boolean ambilAbsenPiket;
	/** Bendera penarikan kegiatan/prestasi. Lihat {@link #getAmbilKegiatanSiswa()}. */
	private Boolean ambilKegiatanSiswa;
	/** Bendera penarikan pelanggaran. Lihat {@link #getAmbilPelanggaranSiswa()}. */
	private Boolean ambilPelanggaranSiswa;
	/** Bendera penarikan apresiasi. Lihat {@link #getAmbilApresiasiSiswa()}. */
	private Boolean ambilApresiasiSiswa;
	/** Bendera penarikan catatan wali kelas. Lihat {@link #getAmbilCatatanSiswa()}. */
	private Boolean ambilCatatanSiswa;


	/**
	 * Jumlah tingkat ke belakang yang seharusnya ikut dicetak pada mode kumulatif.
	 * <b>Tidak pernah dibaca mesin laporan</b> — lihat {@link #getTingkatKebelakang()}.
	 */
	private Integer tingkatKebelakang;
	/** Bendera aktif/nonaktif baris master ini. Lihat {@link #getAktif()}. */
	private Boolean aktif;
	/**
	 * Daftar id {@link Matapelajaran} yang dikeluarkan dari rapor, disimpan sebagai teks JSON
	 * array. Lihat {@link #getMpYgTidakDiambil()} dan {@link #ambilMk()}.
	 */
	private String mpYgTidakDiambil;

	/**
	 * Konstruktor kosong wajib Hibernate; juga dipakai {@code JenisRaporSiswaAction.onAdd(...)}
	 * untuk menyiapkan formulir "Tambah Jenis Rapor Siswa". Seluruh properti dibiarkan
	 * {@code null} dan nilai bawaannya dikarang oleh masing-masing getter (lihat catatan
	 * <i>property access</i> pada Javadoc kelas).
	 */
	public JenisRaporSiswa() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Nilainya juga menjadi <b>kunci penghubung ke berkas</b>: template jrxml dan gambar
	 * pendukung dicari di {@code LampiranLain} dengan {@code ref} sama dengan id ini
	 * ({@code LampiranLain.ambil(jenisRaporSiswa.getId(),
	 * LampiranLain.FILE_JRXML_LAYOUT_JENIS_RAPOR)}). Karena hubungan itu hanya berupa angka —
	 * bukan FK — <b>menghapus baris ini tidak menghapus berkasnya</b>, dan berkas yatim yang
	 * tertinggal dapat "menempel" ke baris baru bila basis data kelak menerbitkan ulang id yang
	 * sama.</p>
	 *
	 * @return id baris, atau {@code null} bila entity belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Umumnya tidak dipanggil kode aplikasi — id dibangkitkan basis data.
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode singkat profil rapor ini, sudah di-{@code trim}.
	 *
	 * <p><b>Tidak pernah {@code null}</b>: nilai {@code null} dipetakan menjadi string kosong.
	 * Karena entity ini dibaca Hibernate lewat property access, string kosong itulah yang
	 * tersimpan ke kolom pada penyimpanan berikutnya — kolom {@code kode} praktis tidak pernah
	 * berisi {@code NULL} setelah baris pernah melewati Hibernate sekali.</p>
	 *
	 * <p>Dipakai sebagai bagian label item combobox "Jenis Rapor"
	 * ({@code Common.insertCombo(..., new String[] { "nama", "kode" }, ...)}). Tidak ada layar
	 * yang menyediakan isian untuk properti ini — formulir master hanya memuat Nama, Yayasan,
	 * Sekolah, dan Keterangan — sehingga pada praktiknya kolom ini hanya terisi lewat impor
	 * massal atau skrip.</p>
	 *
	 * @return kode yang sudah di-{@code trim}; {@code ""} bila belum diisi
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Menyetel kode singkat. Nilai disimpan <b>apa adanya</b> (tidak di-{@code trim}); normalisasi
	 * baru terjadi saat dibaca {@link #getKode()}.
	 *
	 * @param kode kode baru; boleh {@code null}
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama profil rapor ini, sudah di-{@code trim}.
	 *
	 * <p>Inilah yang dilihat pengguna di grid master, di combobox "Jenis Rapor *", dan pada tautan
	 * riwayat revisi ({@code RevisiHelper.createNewRevisi(..., jenisRaporSiswa.getNama())}).
	 * Sistem <b>tidak memberi makna khusus</b> pada isinya — tidak ada pencocokan kata kunci
	 * "tengah semester"/"kenaikan kelas" di mana pun; seluruh perbedaan perilaku antar baris
	 * berasal dari bendera dan berkas template, bukan dari nama.</p>
	 *
	 * <p>Berbeda dengan {@link #getKode()}, method ini <b>meneruskan {@code null}</b> apa adanya
	 * meski kolomnya {@code nullable = false}; baris tanpa nama hanya tercegah oleh validasi layar
	 * ({@code JenisRaporSiswaAction.onSave} menolak nama kosong), bukan oleh entity.</p>
	 *
	 * @return nama yang sudah di-{@code trim}, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama profil rapor. Nilai disimpan apa adanya; pemangkasan spasi terjadi saat
	 * dibaca {@link #getNama()}.
	 *
	 * @param nama nama baru; kolomnya {@code nullable = false} sehingga {@code null} akan ditolak
	 *             basis data saat flush
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas baris ini, apa adanya (tanpa normalisasi).
	 *
	 * <p>Selain tampil sebagai kolom grid, teks ini dipakai sebagai deskripsi item combobox
	 * "Jenis Rapor" pada layar laporan.</p>
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas.
	 *
	 * @param keterangan keterangan baru; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif baris master ini, dengan nilai bawaan {@code true} bila kolomnya
	 * masih {@code null}.
	 *
	 * <p><b>Baris baru otomatis aktif.</b> Konsekuensinya profil rapor yang baru dibuat langsung
	 * muncul di combobox layar laporan — pengisi combobox mensyaratkan
	 * {@code Restrictions.eq("aktif", true)} — sekalipun berkas jrxml-nya belum diunggah. Bila
	 * template belum ada, pencetakan menampilkan pesan "File laporan rapor siswa belum diupload"
	 * (jalur ZK) atau membalas {@code status 97} (jalur REST).</p>
	 *
	 * <p>Perhatikan asimetri: pengisi combobox memakai {@code eq("aktif", true)} yang
	 * <b>membuang baris ber-{@code aktif} {@code NULL}</b>, sedangkan getter ini menganggap
	 * {@code NULL} sama dengan aktif. Baris warisan yang belum pernah melewati penyimpanan
	 * Hibernate (mis. hasil skrip SQL langsung) karena itu terlihat "Aktif" di grid master namun
	 * tidak pernah muncul di combobox laporan.</p>
	 *
	 * @return {@code true} bila aktif atau belum pernah disetel; {@code false} bila dinonaktifkan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif baris master ini.
	 *
	 * <p>Dipanggil dari kotak centang "Aktif" pada grid master; setiap centang langsung diikuti
	 * {@code Common.refreshSaveOrUpdate(...)} sehingga <b>tersimpan seketika</b> tanpa tombol
	 * Simpan dan tanpa konfirmasi.</p>
	 *
	 * @param aktif status baru; {@code null} akan dibaca sebagai {@code true} oleh
	 *              {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan sekolah pemilik profil rapor ini.
	 *
	 * <p><b>Efek samping:</b> memanggil {@code check(...)} warisan {@link GeneralValueObject} dan
	 * <b>menugaskan hasilnya kembali ke field</b>. Itu wajib — {@code check()} dapat
	 * mengembalikan instance lain (kanonik dari {@code EntityIdentityMap}, dari cache, atau hasil
	 * reload lewat session baru) sehingga proxy lazy yang sudah <i>detached</i> tidak meledak di
	 * pemanggil. Pertukaran instance ini tidak mengubah nilai kolom.</p>
	 *
	 * <p>Relasi ini adalah <b>satu-satunya penanda tenant nyata</b> entity ini
	 * ({@link #getYayasan()} hanya turunannya). Kolomnya {@code nullable} tanpa validasi entity;
	 * yang mewajibkan pengisian hanyalah {@code JenisRaporSiswaAction.onSave}.</p>
	 *
	 * @return sekolah pemilik, atau {@code null} bila baris tidak terikat sekolah mana pun
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik.
	 *
	 * <p><b>Menolak objek tanpa id.</b> Argumen {@code null} <i>maupun</i> objek {@link Sekolah}
	 * yang {@code getId()}-nya {@code null} sama-sama disimpan sebagai {@code null}. Ini penjaga
	 * terhadap objek tenant "kosong" yang dikembalikan resolusi tenant yang gagal — lebih baik
	 * relasi kosong daripada relasi ke baris hantu. Efek sampingnya: kesalahan pemilihan sekolah
	 * di layar <b>tidak melempar apa pun</b>, baris hanya diam-diam menjadi milik "semua".</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau objek tanpa id menghasilkan {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik profil rapor ini, <b>diturunkan ulang dari sekolah setiap kali
	 * dipanggil</b>.
	 *
	 * <p><b>Getter dengan efek tulis balik — instance pola (a).</b> Urutannya: panggil
	 * {@link #getSekolah()} (yang sendiri menulis ke field {@code sekolah}), lalu bila sekolahnya
	 * tidak {@code null} <b>timpa</b> field {@code yayasan} dengan {@code sekolah.getYayasan()},
	 * lalu resolusi {@code check(...)} dan tulis lagi ke field. Karena Hibernate membaca entity ini
	 * lewat property access, sekadar <b>membaca</b> baris ini — merender satu baris grid, mengisi
	 * satu item combobox, merakit parameter laporan — dapat mengubah nilai kolom
	 * {@code yayasan_id} dan memicu UPDATE nyata pada flush berikutnya, lengkap dengan satu revisi
	 * Envers baru dan stempel audit atas nama pengguna yang tidak menyunting apa pun (lihat
	 * {@link #onUpdate()}).</p>
	 *
	 * <p>Nilai yang disetel manual lewat {@link #setYayasan(Yayasan)} karena itu <b>tidak
	 * bertahan</b> selama sekolahnya terisi: pembacaan berikutnya menimpanya. Yayasan hanya
	 * bertahan apa adanya bila {@link #getSekolah()} mengembalikan {@code null}.</p>
	 *
	 * @return yayasan pemilik — yayasan milik {@link #getSekolah()} bila sekolahnya ada, selain
	 *         itu nilai yang tersimpan; boleh {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan_id")
	public Yayasan getYayasan() {
		sekolah = getSekolah();
		if (sekolah != null) {
			yayasan = sekolah.getYayasan();
		}
		yayasan = check(yayasan);
		return this.yayasan;
	}

	/**
	 * Menyetel yayasan pemilik, dengan penjaga "tolak objek tanpa id" yang sama seperti
	 * {@link #setSekolah(Sekolah)}.
	 *
	 * <p><b>Nilainya mudah tertimpa:</b> {@link #getYayasan()} menurunkan ulang yayasan dari
	 * sekolah pada setiap pembacaan, sehingga nilai yang disetel di sini hanya bertahan bila
	 * sekolahnya kosong.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau objek tanpa id menghasilkan {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan bendera <b>mode rapor kumulatif</b>, dengan nilai bawaan {@code false}.
	 *
	 * <p>Namanya menyebut "semua semester", tetapi yang dinyalakannya di
	 * {@code LaporanRaporSiswa.generateParameter(...)} lebih luas dari itu: sebuah cabang khusus
	 * yang, untuk tiap siswa, mengambil <b>seluruh keanggotaan kelasnya pada tingkat &le; tingkat
	 * sekarang</b> ({@code Restrictions.le("kelasSiswa.tingkat", tingkat)}) lalu menelusuri
	 * <b>kedua semester</b> ({@code for (int smt : new Integer[] { 1, 2 })}). Setiap nilai yang
	 * dihasilkan ditempatkan ke parameter Jasper dengan awalan
	 * {@code "<tingkat>.<semester>."} — mis. {@code "7.1.matapelajaran"}, {@code "8.2.kkm"} — di
	 * samping salinan tanpa awalan. Jadi ini praktis mode <b>transkrip/rapor kolektif</b>, dan
	 * template jrxml-nya harus memang menuliskan field ber-awalan itu; template biasa akan
	 * mencetak hasil yang sama seperti mode normal.</p>
	 *
	 * <p><b>Biaya:</b> cabang ini mengalikan jumlah kueri penilaian dengan jumlah tingkat dikali
	 * dua semester untuk setiap siswa. Menyalakannya pada cetak massal satu angkatan adalah
	 * perbedaan menit versus puluhan menit.</p>
	 *
	 * @return {@code true} bila mode kumulatif menyala; {@code false} bila belum pernah disetel
	 */
	public Boolean getUntukSemuaSemester() {
		return untukSemuaSemester == null ? false : untukSemuaSemester;
	}

	/**
	 * Menyetel bendera mode rapor kumulatif. Dipanggil dari kotak centang "Semua Smt" pada grid
	 * master; tersimpan seketika lewat {@code Common.refreshSaveOrUpdate(...)}.
	 *
	 * @param untukSemuaSemester bendera baru; {@code null} dibaca sebagai {@code false}
	 */
	public void setUntukSemuaSemester(Boolean untukSemuaSemester) {
		this.untukSemuaSemester = untukSemuaSemester;
	}

	/**
	 * Mengembalikan jumlah tingkat ke belakang yang <i>seharusnya</i> ikut dicetak pada mode
	 * kumulatif, dengan nilai bawaan {@code 1}.
	 *
	 * <p><b>KONFIGURASI MATI — hasil verifikasi.</b> Di seluruh basis kode, properti ini hanya
	 * muncul di tiga tempat: deklarasi di berkas ini, kotak angka pada grid
	 * {@code JenisRaporSiswaAction} (yang menyimpannya seketika), dan daftar kolom ekspor/impor
	 * {@code contents} pada layar yang sama. <b>Tidak ada satu pun pembacaan dari mesin
	 * laporan.</b> Cabang kumulatif {@link #getUntukSemuaSemester()} memakai batas keras
	 * {@code Restrictions.le("kelasSiswa.tingkat", tingkat)} — yakni <b>seluruh</b> tingkat di
	 * bawah tingkat berjalan, tanpa pernah menoleh ke angka ini.</p>
	 *
	 * <p>Akibatnya operator dapat menyetelnya ke 1 dengan maksud "cukup satu tingkat ke belakang"
	 * dan tetap memperoleh rapor berisi seluruh riwayat tingkat siswa — beserta seluruh biaya
	 * kuerinya. Bila kelak diperbaiki, tempat yang benar adalah pembatas {@code le(...)} tersebut;
	 * jangan sekadar menghapus properti ini karena namanya sudah telanjur ada di berkas ekspor
	 * pelanggan.</p>
	 *
	 * @return jumlah tingkat ke belakang yang disetel; {@code 1} bila belum pernah disetel
	 */
	public Integer getTingkatKebelakang() {
		return tingkatKebelakang == null ? 1 : tingkatKebelakang;
	}

	/**
	 * Menyetel jumlah tingkat ke belakang. Dipanggil dari kotak angka pada grid master dan
	 * tersimpan seketika — meski nilainya tidak pernah dipakai (lihat
	 * {@link #getTingkatKebelakang()}).
	 *
	 * @param tingkatKebelakang nilai baru; {@code null} dibaca sebagai {@code 1}
	 */
	public void setTingkatKebelakang(Integer tingkatKebelakang) {
		this.tingkatKebelakang = tingkatKebelakang;
	}

	/**
	 * Mengembalikan bendera <b>boleh dipilih siswa</b>, dengan nilai bawaan {@code true}.
	 *
	 * <p>Dipakai persis satu kali sebagai gerbang: saat pengguna yang sedang login adalah siswa
	 * ({@code tbmuser.getSiswa() != null}), pengisi combobox "Jenis Rapor" pada
	 * {@code LaporanRaporSiswa} menambahkan
	 * {@code Restrictions.or(isNull("tampilKeSiswa"), eq("tampilKeSiswa", true))}; untuk pengguna
	 * lain pembatasnya {@code sqlRestriction("true")}. Jadi mematikan bendera ini menyembunyikan
	 * profil rapor dari siswa sambil tetap membiarkannya dipakai staf — pola yang lazim untuk
	 * rapor draf atau rapor internal berisi catatan wali kelas.</p>
	 *
	 * <p><b>Gerbang ini tidak berlaku pada jalur REST.</b>
	 * {@code LaporanApi.raport_siswa(...)} menerima id jenis rapor mentah dari klien dan tidak
	 * pernah memeriksa bendera ini (juga tidak memeriksa {@link #getAktif()} maupun kepemilikan
	 * sekolah). Aplikasi siswa karena itu dapat mencetak dengan template yang sengaja
	 * disembunyikan dari siswa. Rinciannya pada bagian Hak akses di Javadoc kelas.</p>
	 *
	 * <p>Perhatikan bahwa nilai bawaannya {@code true}: baris baru <b>langsung terlihat siswa</b>
	 * kecuali dimatikan secara sadar.</p>
	 *
	 * @return {@code true} bila boleh dipilih siswa atau belum pernah disetel
	 */
	public Boolean getTampilKeSiswa() {
		return tampilKeSiswa == null ? true : tampilKeSiswa;
	}

	/**
	 * Menyetel bendera "Tampil ke siswa". Dipanggil dari kotak centang pada grid master dan
	 * tersimpan seketika.
	 *
	 * @param tampilKeSiswa bendera baru; {@code null} dibaca sebagai {@code true}
	 */
	public void setTampilKeSiswa(Boolean tampilKeSiswa) {
		this.tampilKeSiswa = tampilKeSiswa;
	}

	/**
	 * Konstanta bantu berisi {@link JSONArray} kosong; dipakai {@link #getMpYgTidakDiambil()}
	 * untuk menghasilkan teks {@code "[]"} saat kolomnya kosong.
	 *
	 * <p><b>Objek bersama yang bisa berubah.</b> Ia {@code static} dan visibilitasnya paket, dan
	 * {@code JSONArray} bukan tipe <i>immutable</i>: satu pemanggilan {@code array.put(...)} dari
	 * mana pun di paket ini akan mengubah nilai bawaan bagi <b>seluruh</b> baris entity di JVM.
	 * Saat ini tidak ada yang melakukannya — pemakaian satu-satunya adalah
	 * {@code array.toString()} — dan sebaiknya tetap begitu; bila butuh array kosong baru,
	 * buat instance lokal seperti yang dilakukan {@link #ambilMk()}.</p>
	 *
	 * <p>Perhatikan bahwa nama ini <b>dibayangi</b> variabel lokal bernama sama di dalam
	 * {@link #ambilMk()}; yang dipakai di sana adalah array lokal hasil parsing, bukan konstanta
	 * ini.</p>
	 */
	final static JSONArray array = new JSONArray();

	/**
	 * Mengembalikan daftar mata pelajaran yang <b>dikeluarkan</b> dari rapor ini, dalam bentuk
	 * teks JSON array berisi id {@link Matapelajaran} (mis. {@code "[12,45]"}).
	 *
	 * <p><b>Tidak pernah {@code null} dan tidak pernah kosong secara sintaksis</b>: nilai
	 * {@code null}/spasi dipetakan menjadi {@code "[]"} sehingga {@link #ambilMk()} selalu punya
	 * JSON yang sah untuk diurai. Karena property access, teks {@code "[]"} itu ikut tersimpan ke
	 * kolom pada penyimpanan berikutnya.</p>
	 *
	 * <p>Isinya diisi {@code JenisRaporSiswaAction.onSave} dari deretan kotak centang mata
	 * pelajaran pada formulir ("Matapelajaran yang tidak dimasukkan ke raport ini"). Daftar kotak
	 * centang itu <b>disaring per sekolah</b> yang dipilih di formulir, sehingga mengganti sekolah
	 * pada baris yang sudah ada akan menyimpan ulang daftar berdasarkan mata pelajaran sekolah
	 * baru — id milik sekolah lama yang tidak lagi tercentang <b>hilang tanpa peringatan</b>.</p>
	 *
	 * @return teks JSON array id mata pelajaran yang dikecualikan; {@code "[]"} bila tidak ada
	 * @see #ambilMk()
	 */
	@Column(columnDefinition = "text")
	public String getMpYgTidakDiambil() {
		return mpYgTidakDiambil == null || mpYgTidakDiambil.trim().isEmpty() ? array.toString() : mpYgTidakDiambil;
	}

	/**
	 * Menyetel daftar pengecualian mata pelajaran sebagai teks JSON array.
	 *
	 * <p>Tidak ada validasi bentuk sama sekali: teks apa pun diterima. Teks yang bukan JSON array
	 * membuat {@link #ambilMk()} mengembalikan daftar kosong (fail-open — seluruh mata pelajaran
	 * ikut tercetak) tanpa pesan kesalahan yang terlihat pengguna.</p>
	 *
	 * @param mpYgTidakDiambil teks JSON array id mata pelajaran; {@code null}/kosong dibaca sebagai
	 *                         {@code "[]"}
	 */
	public void setMpYgTidakDiambil(String mpYgTidakDiambil) {
		this.mpYgTidakDiambil = mpYgTidakDiambil;
	}

	/**
	 * Menyaring daftar anggota kelas, menyisakan yang <b>mengambil</b> mata pelajaran tertentu.
	 *
	 * <p><b>KODE MATI — jangan dijadikan acuan.</b> Tidak ada satu pun pemanggil method ini di
	 * seluruh basis kode. Ia adalah salinan ketiga — persis sama baris demi baris dengan
	 * {@code KelasSiswa.filterMk(...)}, yang juga sudah tercatat sebagai kode mati — dari penyaring
	 * yang versi hidupnya ada di {@code KelasSiswaPunyaSiswa.filterMk(...)}. Seluruh layar
	 * absensi, penilaian, pertemuan, dan jadwal memakai versi hidup itu.</p>
	 *
	 * <p><b>Ironi penempatan:</b> meski berada di kelas ini, method ini sama sekali tidak menyentuh
	 * state {@code JenisRaporSiswa} — ia bersifat {@code static} dan hanya membaca
	 * {@code KelasSiswaPunyaSiswa.ambilMk()} milik tiap anggota, yakni pengecualian tingkat
	 * <b>siswa + kelas</b>, bukan pengecualian tingkat jenis rapor ({@link #ambilMk()}). Jadi
	 * namanya menyesatkan: seandainya dipakai, ia tidak akan menerapkan pengecualian profil rapor
	 * sama sekali.</p>
	 *
	 * <p>Dua perbedaan penting terhadap versi hidup, bila kelak method ini dipakai: (1) versi ini
	 * <b>tidak</b> membuang anggota non-aktif ({@code getAktif() == false}), dan (2) versi ini
	 * melempar {@code NullPointerException} pada {@code matapelajaran.getId()} bila argumen
	 * {@code matapelajaran} bernilai {@code null}, sedangkan versi hidup mengembalikan daftar
	 * kosong.</p>
	 *
	 * @param siswa         daftar anggota kelas yang akan disaring; tidak boleh {@code null}
	 * @param matapelajaran mata pelajaran acuan; tidak boleh {@code null}
	 * @return daftar anggota yang mata pelajaran tersebut <b>tidak</b> ada di daftar
	 *         pengecualiannya
	 * @see ais.database.model.sekolah.KelasSiswaPunyaSiswa#filterMk(List, Matapelajaran)
	 */
	public static List<KelasSiswaPunyaSiswa> filterMk(List<KelasSiswaPunyaSiswa> siswa, Matapelajaran matapelajaran) {
		List<KelasSiswaPunyaSiswa> kelasSiswaPunyaSiswas = new ArrayList<KelasSiswaPunyaSiswa>();
		for (KelasSiswaPunyaSiswa kelasSiswaPunyaSiswa : siswa) {
			List<Long> longs = kelasSiswaPunyaSiswa.ambilMk();
			if (!longs.contains(matapelajaran.getId())) {
				kelasSiswaPunyaSiswas.add(kelasSiswaPunyaSiswa);
			}
			longs = null;
		}
		return kelasSiswaPunyaSiswas;
	}

	/**
	 * Mengurai {@link #getMpYgTidakDiambil()} menjadi daftar id {@link Matapelajaran} yang
	 * dikeluarkan dari rapor jenis ini.
	 *
	 * <p><b>Dipanggil dari mana:</b> {@code LaporanRaporSiswa.genarateKelompokInternal(...)} dan
	 * cabang kumulatif {@code generateParameter(...)}. Di kedua tempat hasilnya dipakai sebagai
	 * {@code Restrictions.not(Restrictions.in("matapelajaran.id", longsjenis))} atas kueri
	 * {@code KurikulumPunyaMatapelajaran}, <b>berdampingan</b> dengan daftar pengecualian milik
	 * siswa ({@code KelasSiswaPunyaSiswa.ambilMk()}). Keduanya bersifat aditif: sebuah mata
	 * pelajaran hilang dari rapor bila dikecualikan oleh <i>salah satu</i> dari keduanya.</p>
	 *
	 * <p><b>Verifikasi id, bukan sekadar parsing.</b> Tiap elemen di-{@code parse} menjadi
	 * {@code Long} lalu dicari lewat cache {@code ConstantValues.ambil(Matapelajaran…)}. Id yang
	 * tidak lagi meresolusi ke sebuah mata pelajaran <b>dibuang diam-diam</b>. Efek yang perlu
	 * disadari: bila sebuah mata pelajaran dihapus lalu dibuat ulang dengan id baru, atau bila
	 * cache belum memuatnya, mata pelajaran itu <b>kembali muncul</b> di rapor tanpa ada yang
	 * mengubah konfigurasi.</p>
	 *
	 * <p><b>Fail-open ganda.</b> Kesalahan pada satu elemen hanya melewati elemen itu; kesalahan
	 * pada penguraian JSON secara keseluruhan menghasilkan daftar kosong. Keduanya hanya dicatat
	 * ke {@code ErrorAuditUtil} (yang luar bahkan tanpa {@code printStackTrace}), sehingga
	 * konfigurasi pengecualian yang rusak berarti <b>seluruh mata pelajaran ikut tercetak</b> —
	 * aman secara akademik, tetapi menyembunyikan kerusakan data.</p>
	 *
	 * <p><b>Perbedaan dengan {@code KelasSiswaPunyaSiswa.ambilMk()}</b> yang bentuknya mirip:
	 * versi di sana masih menambahkan pengecualian tingkat kelas ke hasilnya; versi ini berdiri
	 * sendiri dan tidak menggabungkan daftar mana pun.</p>
	 *
	 * <p><b>Efek samping:</b> tidak ada penulisan ke state entity; hanya pembacaan cache yang
	 * berpotensi memicu resolusi lazy. Variabel lokal {@code array} di dalamnya membayangi
	 * konstanta {@link #array} — yang dipakai adalah array lokal.</p>
	 *
	 * @return daftar id mata pelajaran yang dikecualikan dan masih meresolusi ke baris yang ada;
	 *         tidak pernah {@code null}, bisa kosong
	 * @see #getMpYgTidakDiambil()
	 */
	public List<Long> ambilMk() {
		List<Long> longs = new ArrayList<Long>();
		try {
			JSONArray array = new JSONArray(getMpYgTidakDiambil());

			for (int i = 0; i < array.length(); i++) {
				try {
					Long key = Long.parseLong(array.get(i).toString());
					Matapelajaran matapelajaran = (Matapelajaran) ConstantValues.ambil(Matapelajaran.class.getName(),
							key);
					if (matapelajaran != null) {
						longs.add(matapelajaran.getId());
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/JenisRaporSiswa.java:236");
				}
			}
			return longs;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/JenisRaporSiswa.java:240");
		}
		return longs;
	}

	/**
	 * Mengembalikan bendera <b>hitung rata-rata kelas</b>, dengan nilai bawaan {@code false}.
	 *
	 * <p>Bila menyala, {@code LaporanRaporSiswa} — untuk setiap {@code GrupPenilaian} pada setiap
	 * mata pelajaran — menelusuri <b>seluruh anggota kelas</b> siswa yang sedang dicetak,
	 * menjumlahkan nilainya, lalu mengisi parameter Jasper
	 * {@code totalKelas_<idGrup>}, {@code jumlahKelas_<idGrup>}, dan
	 * {@code rataRataKelas_<idGrup>}. Bila padam, ketiga parameter itu tetap ada namun bernilai
	 * {@code 0} — template yang mencetaknya akan menampilkan nol, bukan kolom kosong.</p>
	 *
	 * <p><b>Biaya dan peredamnya:</b> ini kueri per (grup × mata pelajaran × siswa). Mesin laporan
	 * meredamnya dengan cache berlapis berkunci
	 * {@code grup_kelas_mapel_semester_target} sehingga hitungan dilakukan sekali per kelas untuk
	 * seluruh siswa dalam satu kali cetak. Tetap saja, menyalakannya pada cetak massal adalah
	 * perbedaan beban yang terasa.</p>
	 *
	 * <p><b>Catatan ketelitian angka:</b> kueri anggota kelas yang menjadi pembagi hanya memakai
	 * {@code Restrictions.eq("kelasSiswa", …)} — <b>tanpa</b> tapis {@code aktif}. Siswa yang sudah
	 * dinonaktifkan/pindah tetap menambah pembagi (dan menyumbang nilai 0), sehingga rata-rata
	 * kelas yang tercetak bisa lebih rendah dari rata-rata siswa yang benar-benar aktif.</p>
	 *
	 * <p>Bendera ini berbeda dari properti bernama sama pada {@code JenisItemPenilaianSiswa}: yang
	 * di sini menyalakan rata-rata pada tingkat <b>grup penilaian</b>, yang di sana pada tingkat
	 * <b>item penilaian</b>. Keduanya dievaluasi terpisah dan tidak saling menggantikan.</p>
	 *
	 * @return {@code true} bila rata-rata kelas dihitung; {@code false} bila belum pernah disetel
	 */
	public Boolean getHitungRataRataKelas() {
		return hitungRataRataKelas == null ? false : hitungRataRataKelas;
	}

	/**
	 * Menyetel bendera hitung rata-rata kelas. Dipanggil dari kotak centang "Rata-Rata Kelas" pada
	 * grid master; tersimpan seketika.
	 *
	 * @param hitungRataRataKelas bendera baru; {@code null} dibaca sebagai {@code false}
	 */
	public void setHitungRataRataKelas(Boolean hitungRataRataKelas) {
		this.hitungRataRataKelas = hitungRataRataKelas;
	}

	/**
	 * Mengembalikan bendera <b>tarik rekap kehadiran</b> (label layar: "Absen"), dengan nilai
	 * bawaan {@code false}.
	 *
	 * <p>Bila menyala, mesin laporan mengambil baris {@code AbsenPiket} untuk sekolah, tahun
	 * ajaran, dan semester yang dipilih, lalu merakitnya menjadi parameter rekap kehadiran rapor.
	 * Bila padam, kueri itu <b>tidak dijalankan sama sekali</b> (bukan sekadar hasilnya
	 * disembunyikan) — jadi mematikannya juga merupakan pengungkit kinerja, bukan hanya pilihan
	 * tampilan. Hal yang sama berlaku untuk keempat bendera {@code ambil*} lainnya.</p>
	 *
	 * @return {@code true} bila rekap kehadiran ikut ditarik; {@code false} bila belum pernah
	 *         disetel
	 */
	public Boolean getAmbilAbsenPiket() {
		return ambilAbsenPiket == null ? false : ambilAbsenPiket;
	}

	/**
	 * Menyetel bendera tarik rekap kehadiran. Dipanggil dari kotak centang "Absen" pada grid
	 * master; tersimpan seketika.
	 *
	 * @param ambilAbsenPiket bendera baru; {@code null} dibaca sebagai {@code false}
	 */
	public void setAmbilAbsenPiket(Boolean ambilAbsenPiket) {
		this.ambilAbsenPiket = ambilAbsenPiket;
	}

	/**
	 * Mengembalikan bendera <b>tarik kegiatan dan prestasi siswa</b> (label layar
	 * "Kegiatan/Prestasi"), dengan nilai bawaan {@code false}.
	 *
	 * <p>Cakupannya lebih luas dari namanya: bendera ini menggerbangi <b>tiga</b> kueri sekaligus
	 * — {@code KegiatanSiswa}, {@code PrestasiSiswa}, dan {@code FormulirKegiatanPeserta} — dan
	 * ikut menentukan apakah blok kegiatan dirakit ke parameter Jasper. Bersama
	 * {@link #getAmbilPelanggaranSiswa()} dan {@link #getAmbilApresiasiSiswa()}, ia juga menjadi
	 * pemasok data perhitungan poin siswa.</p>
	 *
	 * @return {@code true} bila kegiatan/prestasi ikut ditarik; {@code false} bila belum pernah
	 *         disetel
	 */
	public Boolean getAmbilKegiatanSiswa() {
		return ambilKegiatanSiswa == null ? false : ambilKegiatanSiswa;
	}

	/**
	 * Menyetel bendera tarik kegiatan/prestasi. Dipanggil dari kotak centang "Kegiatan/Prestasi"
	 * pada grid master; tersimpan seketika.
	 *
	 * @param ambilKegiatanSiswa bendera baru; {@code null} dibaca sebagai {@code false}
	 */
	public void setAmbilKegiatanSiswa(Boolean ambilKegiatanSiswa) {
		this.ambilKegiatanSiswa = ambilKegiatanSiswa;
	}

	/**
	 * Mengembalikan bendera <b>tarik pelanggaran siswa</b>, dengan nilai bawaan {@code false}.
	 *
	 * <p>Menggerbangi kueri {@code PelanggaranSiswa} dan, bersama
	 * {@link #getAmbilApresiasiSiswa()}, menggerbangi perakitan blok poin
	 * ({@code masukkanPoin(...)}) pada parameter rapor.</p>
	 *
	 * <p><b>Bendera ini juga menggerbangi satu kueri milik apresiasi.</b> Pada salah satu jalur
	 * {@code LaporanRaporSiswa} (sekitar baris 1555), daftar {@code ApresiasiSiswa} diambil dengan
	 * syarat {@code getAmbilPelanggaranSiswa()} alih-alih {@code getAmbilApresiasiSiswa()} —
	 * ketidakcocokan yang sudah tercatat pada Javadoc {@code ApresiasiSiswa}. Akibatnya sekolah
	 * yang mencentang "Apresiasi" tetapi tidak mencentang "Pelanggaran" bisa kehilangan blok
	 * apresiasinya pada jalur tersebut. Jangan "memperbaiki" gejalanya dari sisi entity ini —
	 * perbaikannya ada di berkas laporan.</p>
	 *
	 * @return {@code true} bila pelanggaran ikut ditarik; {@code false} bila belum pernah disetel
	 */
	public Boolean getAmbilPelanggaranSiswa() {
		return ambilPelanggaranSiswa == null ? false : ambilPelanggaranSiswa;
	}

	/**
	 * Menyetel bendera tarik pelanggaran. Dipanggil dari kotak centang "Pelanggaran" pada grid
	 * master; tersimpan seketika.
	 *
	 * @param ambilPelanggaranSiswa bendera baru; {@code null} dibaca sebagai {@code false}
	 */
	public void setAmbilPelanggaranSiswa(Boolean ambilPelanggaranSiswa) {
		this.ambilPelanggaranSiswa = ambilPelanggaranSiswa;
	}

	/**
	 * Mengembalikan bendera <b>tarik apresiasi siswa</b>, dengan nilai bawaan {@code false}.
	 *
	 * <p>Menggerbangi kueri {@code ApresiasiSiswa} pada jalur rapor kumulatif dan ikut
	 * menggerbangi perakitan blok poin. Perhatikan ketidakcocokan gerbang pada satu jalur lain
	 * yang dijelaskan di {@link #getAmbilPelanggaranSiswa()} — mencentang bendera ini saja belum
	 * tentu cukup untuk memunculkan apresiasi di semua jalur cetak.</p>
	 *
	 * @return {@code true} bila apresiasi ikut ditarik; {@code false} bila belum pernah disetel
	 */
	public Boolean getAmbilApresiasiSiswa() {
		return ambilApresiasiSiswa == null ? false : ambilApresiasiSiswa;
	}

	/**
	 * Menyetel bendera tarik apresiasi. Dipanggil dari kotak centang "Apresiasi" pada grid master;
	 * tersimpan seketika.
	 *
	 * @param ambilApresiasiSiswa bendera baru; {@code null} dibaca sebagai {@code false}
	 */
	public void setAmbilApresiasiSiswa(Boolean ambilApresiasiSiswa) {
		this.ambilApresiasiSiswa = ambilApresiasiSiswa;
	}

	/**
	 * Mengembalikan bendera <b>tarik catatan siswa</b>, dengan nilai bawaan {@code true}.
	 *
	 * <p><b>Satu-satunya bendera {@code ambil*} yang bawaannya menyala</b> — empat lainnya
	 * bawaannya padam. Asimetri ini tidak berdokumen di layar mana pun dan mudah mengejutkan:
	 * profil rapor yang baru dibuat langsung menarik catatan wali kelas tanpa ada yang
	 * mencentangnya. Bila template jrxml memuat bidang catatan, catatan itu akan tercetak
	 * meski operator tidak pernah menyalakannya.</p>
	 *
	 * <p>Menggerbangi dua kueri sekaligus, {@code CatatanKelasSiswa} dan {@code CatatanSiswa},
	 * serta pemanggilan perakit blok catatan pada parameter rapor.</p>
	 *
	 * @return {@code true} bila catatan ikut ditarik atau belum pernah disetel
	 */
	public Boolean getAmbilCatatanSiswa() {
		return ambilCatatanSiswa== null ? true : ambilCatatanSiswa;
	}

	/**
	 * Menyetel bendera tarik catatan siswa. Dipanggil dari kotak centang "Catatan" pada grid
	 * master; tersimpan seketika.
	 *
	 * @param ambilCatatanSiswa bendera baru; {@code null} dibaca sebagai {@code true}
	 */
	public void setAmbilCatatanSiswa(Boolean ambilCatatanSiswa) {
		this.ambilCatatanSiswa = ambilCatatanSiswa;
	}
}
