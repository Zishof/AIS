package ais.database.model.sekolah;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
import ais.database.model.VOPembelajaran;
import ais.ui.util.WaktuUtil;

/**
 * Entity <b>jadwal pelajaran</b> modul sekolah — tabel {@code sekolah.jadwal_pelajaran},
 * ber-{@code @Audited} (Envers) dan {@code dynamicInsert}/{@code dynamicUpdate}.
 *
 * <h3>Peran domain</h3>
 * <p>Satu baris entity ini adalah <b>satu penugasan pembelajaran</b>: satu
 * {@link Matapelajaran} untuk satu rombongan belajar ({@link KelasSiswa}) <i>atau</i> satu
 * kelas les ({@link KelasLesSiswa}), pada satu tahun ajaran dan satu semester. Baris inilah
 * induk seluruh kegiatan belajar-mengajar berikutnya — pertemuan, absensi siswa, presensi
 * guru, materi, tugas, ujian, nilai, jurnal mengajar, sampai berkas RPP/RPS otomatis —
 * sehingga hampir semua layar modul sekolah bermuara padanya. Sekitar <b>166 berkas Java</b>
 * merujuk kelas ini. Menu pemakainya: "Jadwal Pelajaran"
 * ({@code /pages/master/sekolah/jadwal_pelajaran.zul}, layar
 * {@code ais.action.master.sekolah.JadwalPelajaranAction}) dan laporan
 * {@code LaporanJadwalPelajaran}.</p>
 *
 * <h3>Model DUA BELAS SLOT — inti kelas ini</h3>
 * <p>Alih-alih tabel anak berisi baris "hari + jam", entity ini menyimpan <b>dua belas slot
 * tetap sebagai kolom berulang pada satu baris</b>. Setiap slot ke-N (N = 1..12) terdiri
 * dari <b>empat</b> kolom yang selalu berpasangan:</p>
 * <table border="1">
 * <tr><th>Bagian slot</th><th>Properti</th><th>Kolom</th></tr>
 * <tr><td>Hari</td><td>{@code hari}, {@code hari2} … {@code hari12}</td><td>{@code hari} (length 6), {@code hari2} … {@code hari12}</td></tr>
 * <tr><td>Jam</td><td>{@code jamPelajaran}, {@code jamPelajaran2} … {@code jamPelajaran12}</td><td>{@code jam_pelajaran_id}, {@code jam_pelajaran2_id} … {@code jam_pelajaran12_id}</td></tr>
 * <tr><td>Guru</td><td>{@code guru}, {@code guru2} … {@code guru12}</td><td>{@code guru_id}, {@code guru2_id} … {@code guru12_id}</td></tr>
 * <tr><td>Sub mapel</td><td>{@code subMatapelajaran}, {@code subMatapelajaran2} … {@code subMatapelajaran12}</td><td>{@code sub_matapelajaran}, {@code sub_matapelajaran_2} … {@code sub_matapelajaran_12}</td></tr>
 * </table>
 * <p>Pada layar, keempatnya berdiri berdampingan dalam satu baris formulir berlabel
 * <b>"Hari dan Jam Pelajaran I"</b> sampai <b>"Hari dan Jam Pelajaran XII"</b>
 * ({@code JadwalPelajaranAction}, baris 1655–2647). Slot II–XII disembunyikan sampai
 * pengguna menekan "Tambah Jadwal Mengajar"; jumlah slot yang tampak ditentukan
 * {@link #populateHari()}.</p>
 *
 * <p>Model ini dipakai untuk dua hal sekaligus: (1) satu mata pelajaran yang jatuh pada
 * beberapa hari/jam berbeda dalam sepekan, dan (2) <i>team teaching</i> dengan sampai dua
 * belas pengampu berbeda. Konsekuensi struktural yang perlu diingat:</p>
 * <ul>
 * <li>Tidak ada koleksi — menambah slot ke-13 berarti mengubah skema, entity, layar,
 *     seluruh penyaring dan seluruh pemakai. Perluasan dari 10 ke 12 slot memang sudah
 *     pernah terjadi dan meninggalkan jejak tak lengkap (lihat "Kuirk & bug" di bawah).</li>
 * <li>Tidak ada batasan bahwa dua belas slot harus konsisten satu sama lain: slot boleh
 *     memakai {@link JamPelajaran} dari {@link JenisJadwalPelajaran} atau
 *     {@link KelompokJamPelajaran} yang berbeda-beda dalam satu baris.</li>
 * <li>Urutan deklarasi di berkas ini <b>tidak berurut</b> — {@code getJamPelajaran5()}
 *     dideklarasikan sebelum {@code getJamPelajaran4()}, dan pengelompokan getter slot
 *     6–12 terpisah jauh dari slot 1–5. Pemetaan {@code @JoinColumn} masing-masing tetap
 *     benar; hanya letaknya yang berserak.</li>
 * </ul>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 * <li><b>Per slot (12×4 pasang getter/setter):</b> {@code hariN}, {@code jamPelajaranN},
 *     {@code guruN}, {@code subMatapelajaranN}.</li>
 * <li><b>Agregat lintas slot:</b> {@link #populateHari()}, {@link #populateWaktu()},
 *     {@link #populateWaktu(String)}, {@link #populateWaktuMulai()},
 *     {@link #populateWaktuById()}, {@link #populateJamPelajaran()},
 *     {@link #populateGuru()}.</li>
 * <li><b>Identitas &amp; cakupan global:</b> {@link #getKelas()}, {@link #getKelasLesSiswa()},
 *     {@link #getMatapelajaran()}, {@link #getKurikulumPunyaMatapelajaran()},
 *     {@link #getSekolah()}, {@link #getYayasan()}, {@link #getTahunAjaran()},
 *     {@link #getSemester()}, {@link #getProgram()}, {@link #getRuang()}.</li>
 * <li><b>Kalender &amp; masa berlaku:</b> {@link #getMasaJadwalPelajaran()},
 *     {@link #getTanggalMulaiJadwalPelajaran()}, {@link #getJenis()},
 *     {@link #getLewatiTanggalMerahNasional()}.</li>
 * <li><b>Kebijakan &amp; saklar:</b> {@link #getAbaikanBentrok()}, {@link #getAktif()},
 *     {@link #getDikunci()}, {@link #getGuruBisaMerubahTanggalJadwalPelajaran()},
 *     {@link #getKehadiranGuruHarusDiinputSesuaiJadwal()},
 *     {@link #getMerupakan_tanpa_jadwal_perkuliahan()}, {@link #getUrutkanotomatis()}.</li>
 * <li><b>Muatan pembelajaran:</b> {@link #getDeskripsiPembelajaran()},
 *     {@link #getCapaianPembelajaranProdi()}, {@link #getPendahuluan()},
 *     {@link #getCourse()}.</li>
 * <li><b>Ringkasan teks:</b> {@link #toString()}, {@link #info()}, {@link #info(Guru)},
 *     {@link #infoSimple()}, {@link #ambilNama()}.</li>
 * <li><b>Audit warisan:</b> {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()},
 *     {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 * </ul>
 *
 * <h3>Catatan teknis: pengulangan field audit BUKAN bug</h3>
 * <p>Kelas ini turun dari {@link ais.database.model.VOPembelajaran} →
 * {@code VoKunci} → {@link ais.database.model.GeneralValueObject}. Kelas dasar itu
 * <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass}, melainkan POJO abstrak
 * biasa; Hibernate tidak memetakan satu pun properti induknya. Karena itu deklarasi ulang
 * {@code id}, {@code oleh}, {@code olehId} dan {@code tanggal_dirubah} di sini adalah
 * <b>keharusan teknis</b>, bukan salin-tempel yang terlewat.</p>
 *
 * <h3>Getter yang menulis balik (property access)</h3>
 * <p>{@code @Id} dipasang pada {@link #getId()}, sehingga seluruh entity memakai
 * <i>property access</i>: Hibernate membaca nilai lewat getter saat pengecekan kotor.
 * Getter yang mengubah field karena itu <b>benar-benar mengubah basis data</b> — membaca
 * sebuah baris jadwal dapat menulisinya. Daftar lengkapnya:</p>
 * <ul>
 * <li><b>Normalisasi proxy saja</b> (mengganti identitas objek, bukan nilai kolom):
 *     {@link #getJamPelajaran()}…{@code getJamPelajaran12()}, {@link #getKelas()},
 *     {@link #getKelasLesSiswa()}, {@link #getDikunci()},
 *     {@link #getKurikulumPunyaMatapelajaran()},
 *     {@link #getSubMatapelajaran()}…{@code getSubMatapelajaran12()}.</li>
 * <li><b>Mengisi nilai bila kosong:</b> {@link #getSemester()} (dari kalender berjalan),
 *     {@link #getWaktuMulai()} (jam saat ini bila tak ada slot jam),
 *     {@link #getMasaJadwalPelajaran()} (memilih sendiri masa berpenanda
 *     {@code defaultData}).</li>
 * <li><b>Menimpa nilai yang sudah ada — destruktif:</b> {@link #getMatapelajaran()},
 *     {@link #getSekolah()}, {@link #getYayasan()}, {@link #getTahunAjaran()},
 *     {@link #getRuang()}, {@link #getTanggalMulaiJadwalPelajaran()},
 *     {@link #getWaktuMulai()}, {@link #getWaktuSelesai()}, dan — paling merusak —
 *     {@link #getGuru()} sampai {@code getGuru12()}.</li>
 * </ul>
 * <p><b>Dua belas getter guru menghapus data.</b> {@code getGuruN()} menulis {@code null}
 * ke field {@code guruN} setiap kali {@code getHariN()} bernilai {@code null}. Pada baris
 * yang gurunya terisi tetapi harinya sudah dikosongkan (pilihan "Tidak ada jadwal" pada
 * combobox hari), sekadar merender baris itu di grid, memanggil {@link #info(Guru)},
 * {@link #infoSimple()}, {@link #populateGuru()} atau membacanya lewat API sudah cukup
 * untuk <b>menghapus permanen</b> kolom {@code guruN_id}. Ini adalah dua belas instance
 * pola getter destruktif dalam satu berkas — jumlah terbanyak yang tercatat sejauh ini
 * dalam satu entity.</p>
 *
 * <h3>Deteksi bentrok jadwal — TERVERIFIKASI RUSAK</h3>
 * <p>Tiga pemeriksa statis di {@code JadwalPelajaranAction} memeriksa tumpang tindih pada
 * tiga sumbu: {@code checkBentrokBerdasarRuangan} (ruang), {@code checkBentrokBerdasarKelas}
 * (kelas + semester + sekolah), dan {@code checkBentrokBerdasarGuru} (guru). Ketiganya
 * mengiterasi {@link #populateJamPelajaran()} sehingga <i>seharusnya</i> membandingkan
 * slot-per-slot: variabel lokal {@code hari}/{@code hariLain} berisi hari slot yang sedang
 * diperiksa, dan penjaga {@code if (hari.equals(hariLain))} sudah memastikan kedua slot
 * jatuh pada hari yang sama.</p>
 * <p><b>Namun kondisi akhir ketiganya menambahkan syarat berikut:</b></p>
 * <pre>
 * &amp;&amp; ((jadwalPelajaran.getHari() == null ? "" : jadwalPelajaran.getHari())
 *         .equals(jadwalPelajaranLain.getHari()))
 * </pre>
 * <p>Syarat itu membandingkan <b>hari slot ke-1</b> kedua baris jadwal — bukan hari slot
 * yang sedang diperiksa. Akibatnya, pola "penyaring salah kolom" yang sudah berulang di
 * basis kode ini muncul lagi, dengan dua kegagalan konkret:</p>
 * <ol>
 * <li><b>Bentrok pada slot II–XII tidak terdeteksi</b> kecuali kebetulan hari slot ke-1
 *     kedua baris juga sama persis. Dua jadwal yang sama-sama memakai Rabu jam ke-3 pada
 *     slot ke-5 tetapi slot ke-1-nya Senin dan Selasa akan lolos tanpa peringatan.</li>
 * <li><b>Baris yang slot ke-1-nya kosong tidak pernah bentrok sama sekali.</b> Bila
 *     {@code getHari()} bernilai {@code null} pada kedua baris, ruas kirinya menjadi
 *     {@code ""} dan ruas kanannya {@code null}; {@code "".equals(null)} bernilai
 *     {@code false}, sehingga seluruh pemeriksaan gugur. Baris yang seluruh
 *     penjadwalannya diletakkan di slot II–XII karena itu kebal terhadap ketiga
 *     pemeriksa.</li>
 * </ol>
 * <p>Interaksi dengan kuirk lain memperlebar lubangnya: {@link #populateJamPelajaran()}
 * hanya mengumpulkan slot yang <b>hari, jam, dan guru</b>-nya lengkap (slot berhari tetapi
 * belum bergurus tidak pernah diperiksa), {@link #getGuru()} dapat menghapus guru sehingga
 * slot lenyap dari pemeriksaan, dan {@link #getAbaikanBentrok()} bekerja dua arah
 * (baris bercentang tidak lagi terlihat sebagai pemakai ruang/kelas/guru oleh baris lain).
 * Dampaknya nyata di lapangan: ruang dan guru bisa terpesan ganda, dan satu rombongan
 * belajar bisa memiliki dua mata pelajaran pada jam yang sama, tanpa satu pun peringatan.</p>
 *
 * <h3>Kuirk &amp; bug lain yang terverifikasi</h3>
 * <ul>
 * <li><b>Penyaring "Hari" pada layar hanya mencakup 10 dari 12 slot.</b>
 *     {@code JadwalPelajaranAction.initCriteria(...)} merangkai {@code Restrictions.or}
 *     untuk {@code hari}, {@code hari2} … {@code hari10} tetapi <b>melewatkan
 *     {@code hari11} dan {@code hari12}</b>. Penyaring guru pada layar yang sama justru
 *     lengkap ({@code guru} … {@code guru12}) — jejak perluasan 10→12 slot yang tidak
 *     tuntas. Akibatnya jadwal yang hanya memakai slot XI/XII tidak pernah muncul saat
 *     pengguna menyaring per hari.</li>
 * <li><b>Dua metode agregat adalah kode mati:</b> {@link #populateWaktuById()} dan
 *     {@link #populateWaktuMulai()} tidak memiliki pemanggil di seluruh basis kode.</li>
 * <li><b>Variabel lokal bernama {@code gurus}</b> di lima metode {@code populate*} yang
 *     isinya bukan guru — sisa salin-tempel dari {@link #populateGuru()}.</li>
 * <li><b>{@code hari} slot ke-1 dibatasi {@code length = 6}</b>, pas persis menampung
 *     "Minggu"/"Selasa"/"Jum'at" dari {@code Common.haris}; sebelas kolom hari lainnya
 *     tidak membawa {@code @Column} sama sekali sehingga dipetakan otomatis dengan nama
 *     properti apa adanya.</li>
 * <li><b>{@code waktuMulai}/{@code waktuSelesai} adalah salinan turunan</b> dari slot jam
 *     ke-1 yang ikut tersimpan, dan {@link #getWaktuMulai()} mengisi jam <i>saat baris
 *     dibaca</i> bila slot ke-1 kosong.</li>
 * <li><b>Kepemilikan tenant bersifat turunan.</b> {@code sekolah_id}/{@code yayasan_id}
 *     dihitung ulang tiap pembacaan dari kelas/kelas les/kurikulum/mata pelajaran, bukan
 *     data yang berdiri sendiri.</li>
 * <li>Konstanta {@link #GANJIL}/{@link #GENAP} bertipe {@link String}, sementara kolom
 *     {@code semester} bertipe {@link Integer} — dua representasi semester yang mudah
 *     tertukar.</li>
 * </ul>
 *
 * <h3>Catatan kontrol akses (hasil audit, bukan instruksi perubahan)</h3>
 * <ul>
 * <li><b>Cakupan tenant fail-open pada layar.</b> {@code initCriteria(...)} tidak mengikat
 *     kueri ke sekolah/yayasan pengguna yang sedang login; pembatasan sekolah dan yayasan
 *     hanya datang dari combobox penyaring, dan bila pengguna memilih "Semua" kriterianya
 *     menjadi {@code Restrictions.sqlRestriction("1=1")} — daftar menampilkan jadwal
 *     seluruh sekolah pada instalasi. Pola yang sama muncul pada pengisian combobox kelas
 *     ({@code s == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("sekolah", s)}).</li>
 * <li><b>Pembatas anak untuk akun orang tua fail-open.</b> Penyaring
 *     {@code Restrictions.in("kelas.id", kelas)} hanya dipasang bila daftar kelas hasil
 *     {@code OrangTua.ambilAnakSiswa()} tidak kosong; bila relasi ortu–anak tidak
 *     teresolusi, penyaring dilewati seluruhnya dan akun orang tua melihat jadwal seluruh
 *     instalasi. Ini instance lanjutan dari pola fail-open {@code ambilAnakSiswa()} yang
 *     sudah tercatat sejak batch 42 dan diketahui akarnya pada batch 66.</li>
 * <li><b>Fail-open pada API.</b> {@code ApiBaruElearning.daftarJadwalPelajaran} membatasi
 *     hasil ke jadwal milik guru pemilik token, tetapi bila
 *     {@code tbmuser.ambilGuru()} bernilai {@code null} (token milik akun non-guru)
 *     kriterianya berubah menjadi {@code Restrictions.sqlRestriction("1=1")}; bila
 *     {@code ambilYayasan()}/{@code ambilSekolah()} juga {@code null}, seluruh jadwal
 *     instalasi ikut terbaca.</li>
 * <li><b>Pewarisan hak lewat menu induk.</b> Hanya {@code JadwalPelajaranAction} sendiri
 *     yang memanggil {@code CommonPrivilages.checkPrevilages}, dan hanya untuk tombol
 *     Tambah/Ubah/Hapus. <b>Dua puluh lima</b> helper yang menyisipkan atau membaca layar
 *     jadwal ({@code AbsensiHelper}, {@code DetailpertemuanHelper}, {@code PertemuanHelper},
 *     {@code ProsesKehadiranGuru}, seluruh {@code Rekapitulasi*Helper}, {@code TugasMandiriHelper},
 *     {@code TugasKelompokHelper}, {@code HasilUjianHelper}, dan seterusnya) tidak memuat
 *     satu pun pemanggilan {@code checkPrevilages}: haknya diwarisi dari menu apa pun yang
 *     kebetulan memuat helper tersebut.</li>
 * <li>Tombol unggah Excel pada layar ini menyertakan kolom {@code "id"} dalam daftar
 *     {@code contents}, sehingga berkas unggahan dapat menyasar baris jadwal yang sudah
 *     ada. Visibilitasnya memang digerbangi ({@code add && edit && delete}), berbeda dari
 *     beberapa layar lain yang pernah ditemukan tanpa gerbang.</li>
 * </ul>
 *
 * @see JamPelajaran
 * @see JenisJadwalPelajaran
 * @see KelompokJamPelajaran
 * @see SubMatapelajaran
 * @see MasaJadwalPelajaran
 * @see JadwalPelajaranPunyaItem
 * @see KelasSiswa
 * @see KelasLesSiswa
 * @see ais.database.model.VOPembelajaran
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "jadwal_pelajaran", schema = "sekolah")
public class JadwalPelajaran extends VOPembelajaran {

	/**
	 * Label semester <b>genap</b> ({@code "Genap"}) dalam ejaan yang dipakai seluruh layar
	 * sekolah. Konstanta ini <b>bukan</b> nilai kolom entity ini — kolom {@code semester}
	 * bertipe {@link Integer} (1 = ganjil, 2 = genap). Nilainya dipakai sebagai isi combobox
	 * dan sebagai nilai banding pada dasbor rekap (mis.
	 * {@code DashboardRekapAbsensiPerKelasDanMapel}, {@code DashboardRekapAbsensiPerKelasSekolah},
	 * {@code DasboardJadwalPelajaran}), serta saat memanggil {@code JadwalPelajaranAction.bentrok(...)}
	 * yang menerima label semester dalam bentuk teks.
	 *
	 * @see #GANJIL
	 * @see #getSemester()
	 */
	public static final String GENAP = "Genap";
	/**
	 * Label semester <b>ganjil</b> ({@code "Ganjil"}); pasangan {@link #GENAP}. Lihat catatan
	 * pada {@link #GENAP} mengenai perbedaannya dengan kolom {@code semester} yang bertipe angka.
	 *
	 * @see #GENAP
	 * @see #getSemester()
	 */
	public static final String GANJIL = "Ganjil";

	/**
	 * Versi serialisasi Java untuk entity ini. Nilainya dibangkitkan generator dan
	 * dipertahankan agar objek jadwal yang pernah diserialisasi (mis. ke dalam sesi ZK)
	 * tetap dapat dibaca setelah kelas ini berubah.
	 */
	private static final long serialVersionUID = 7154228487700348608L;
	/**
	 * Kunci utama baris jadwal, {@code sekolah.jadwal_pelajaran.id} (IDENTITY).
	 *
	 * <p>Dideklarasikan ulang di sini <b>bukan karena kelalaian</b>: {@link ais.database.model.GeneralValueObject}
	 * adalah POJO abstrak biasa (bukan {@code @Entity}/{@code @MappedSuperclass}), sehingga
	 * Hibernate tidak memetakan properti apa pun milik induk. Setiap entity turunan wajib
	 * mendeklarasikan kembali {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah}.</p>
	 */
	private Long id;
	/**
	 * Nama pengguna yang terakhir menyentuh baris ini; diisi otomatis oleh
	 * {@code ais.database.hibernate.AuditTimestampInterceptor}. Wajib dideklarasikan ulang —
	 * lihat catatan pada {@link #id}.
	 */
	private String oleh;
	/**
	 * Identitas (login id) pengguna yang terakhir menyentuh baris ini. Wajib dideklarasikan
	 * ulang — lihat catatan pada {@link #id}.
	 */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna yang terakhir mengubah baris jadwal ini.
	 *
	 * @return login id penyunting terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel identitas penyunting terakhir.
	 *
	 * <p><b>Kuirk penting:</b> setter ini <b>menolak diam-diam</b> nilai {@code null} atau
	 * string kosong — nilai lama dipertahankan. Jadi jejak audit tidak bisa dikosongkan
	 * lewat setter; ini disengaja agar interceptor tidak menghapus jejak yang sudah ada.</p>
	 *
	 * @param olehId login id penyunting; {@code null}/kosong diabaikan tanpa error
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama penyunting terakhir. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null} atau string kosong <b>diabaikan diam-diam</b>.
	 *
	 * @param oleh nama penyunting; {@code null}/kosong diabaikan tanpa error
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris jadwal ini.
	 *
	 * @return nama penyunting terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mencatat siapa dan kapan baris jadwal diubah dengan
	 * mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)}.
	 *
	 * <p><b>Efek samping:</b> mengisi {@link #getOleh()}, {@link #getOlehId()} dan
	 * {@link #getTanggal_dirubah()} tepat sebelum {@code UPDATE} dikirim. Karena entity ini
	 * memakai <i>property access</i> dan sejumlah getter menulis balik ke field (lihat Javadoc
	 * kelas), callback ini juga ikut berjalan pada perubahan yang tidak pernah diminta pengguna
	 * — mis. saat {@link #getSekolah()} atau {@link #getGuru()} mengubah nilai sewaktu baris
	 * sekadar dirender di grid.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Cap waktu perubahan terakhir, diinisialisasi ke waktu server saat objek dibuat dan
	 * diperbarui oleh {@link #onUpdate()}. Wajib dideklarasikan ulang — lihat catatan pada
	 * {@link #id}. Perhatikan penamaan bergaris bawah ({@code tanggal_dirubah}) yang berbeda
	 * dari konvensi camelCase field lain di kelas ini; nama itu dipertahankan karena sudah
	 * menjadi nama kolom dan nama properti yang dirujuk banyak layar.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel cap waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah cap waktu baru; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir baris jadwal ini.
	 *
	 * @return cap waktu perubahan terakhir (presisi TIMESTAMP)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * <b>Slot jam ke-1</b> dari dua belas slot tetap (lihat Javadoc kelas). Menunjuk satu baris
	 * {@link JamPelajaran} — mis. "Jam ke-1, 07.00–07.45" — pada kolom {@code jam_pelajaran_id}.
	 *
	 * <p>Dua belas field {@code jamPelajaran} … {@code jamPelajaran12} <b>bukan koleksi</b>
	 * melainkan dua belas kolom terpisah pada satu baris tabel. Konsekuensinya: menambah slot
	 * ke-13 berarti mengubah skema, kode entity, layar, penyaring, dan seluruh pemakai.</p>
	 */
	private JamPelajaran jamPelajaran;
	/**
	 * <b>Slot jam ke-2</b>; kolom {@code jam_pelajaran2_id}. Lihat {@link #jamPelajaran} untuk penjelasan model dua belas slot.
	 */
	private JamPelajaran jamPelajaran2;
	/**
	 * <b>Slot jam ke-3</b>; kolom {@code jam_pelajaran3_id}. Lihat {@link #jamPelajaran} untuk penjelasan model dua belas slot.
	 */
	private JamPelajaran jamPelajaran3;
	/**
	 * <b>Slot jam ke-4</b>; kolom {@code jam_pelajaran4_id}. Lihat {@link #jamPelajaran} untuk penjelasan model dua belas slot.
	 */
	private JamPelajaran jamPelajaran4;
	/**
	 * <b>Slot jam ke-5</b>; kolom {@code jam_pelajaran5_id}. Lihat {@link #jamPelajaran} untuk penjelasan model dua belas slot.
	 */
	private JamPelajaran jamPelajaran5;
	/**
	 * <b>Slot jam ke-6</b>; kolom {@code jam_pelajaran6_id}. Lihat {@link #jamPelajaran} untuk penjelasan model dua belas slot.
	 */
	private JamPelajaran jamPelajaran6;
	/**
	 * <b>Slot jam ke-7</b>; kolom {@code jam_pelajaran7_id}. Lihat {@link #jamPelajaran} untuk penjelasan model dua belas slot.
	 */
	private JamPelajaran jamPelajaran7;
	/**
	 * <b>Slot jam ke-8</b>; kolom {@code jam_pelajaran8_id}. Lihat {@link #jamPelajaran} untuk penjelasan model dua belas slot.
	 */
	private JamPelajaran jamPelajaran8;
	/**
	 * <b>Slot jam ke-9</b>; kolom {@code jam_pelajaran9_id}. Lihat {@link #jamPelajaran} untuk penjelasan model dua belas slot.
	 */
	private JamPelajaran jamPelajaran9;
	/**
	 * <b>Slot jam ke-10</b>; kolom {@code jam_pelajaran10_id}. Lihat {@link #jamPelajaran} untuk penjelasan model dua belas slot.
	 */
	private JamPelajaran jamPelajaran10;
	/**
	 * <b>Slot jam ke-11</b>; kolom {@code jam_pelajaran11_id}. Lihat {@link #jamPelajaran} untuk penjelasan model dua belas slot.
	 */
	private JamPelajaran jamPelajaran11;
	/**
	 * <b>Slot jam ke-12</b>; kolom {@code jam_pelajaran12_id}. Lihat {@link #jamPelajaran} untuk penjelasan model dua belas slot.
	 */
	private JamPelajaran jamPelajaran12;
	/**
	 * Rombongan belajar (kelas reguler) yang dijadwalkan, kolom {@code kelas_id}
	 * ({@code nullable = true}). Salah satu dari <b>dua</b> subjek jadwal yang saling
	 * menggantikan: {@code kelas} untuk kelas reguler, {@link #kelasLesSiswa} untuk kelas les.
	 * Baris jadwal juga boleh tidak memiliki keduanya (mis. jadwal berbasis
	 * {@link #kurikulumPunyaMatapelajaran} saja).
	 */
	private KelasSiswa kelas;
	/**
	 * Kelas les yang dijadwalkan, kolom {@code kelas_les_siswa} dengan {@code unique = true} —
	 * artinya <b>satu kelas les hanya boleh punya satu baris jadwal</b>, berbeda dari kelas
	 * reguler yang boleh punya banyak baris (satu per mata pelajaran).
	 *
	 * <p>Bila field ini terisi, ia <b>mendominasi</b> sejumlah getter turunan:
	 * {@link #getMatapelajaran()}, {@link #getSekolah()}, {@link #getRuang()} dan
	 * {@link #getMasaJadwalPelajaran()} mengambil nilainya dari kelas les dan menimpa isi
	 * field lokal.</p>
	 */
	private KelasLesSiswa kelasLesSiswa;
	/**
	 * Mata pelajaran yang diajarkan, kolom {@code matapelajaran_id} ({@code nullable = false}).
	 * Nilai efektifnya dapat ditimpa saat dibaca — lihat {@link #getMatapelajaran()}.
	 */
	private Matapelajaran matapelajaran;
	/**
	 * <b>Guru pengampu slot ke-1</b> dari dua belas slot tetap; kolom {@code guru_id}.
	 *
	 * <p>Dua belas field {@code guru} … {@code guru12} memungkinkan satu baris jadwal memuat
	 * sampai dua belas pengampu berbeda — dipakai untuk <i>team teaching</i> maupun untuk
	 * satu mata pelajaran yang jatuh pada beberapa hari/jam berbeda dalam sepekan, masing-masing
	 * dengan guru yang berlainan.</p>
	 *
	 * <p><b>Peringatan:</b> getter {@link #getGuru()} (dan sebelas saudaranya)
	 * <b>menghapus</b> isi field ini menjadi {@code null} bila {@link #getHari()} kosong.
	 * Lihat Javadoc getter tersebut.</p>
	 */
	private Guru guru;
	/**
	 * <b>Guru pengampu slot ke-2</b>; kolom {@code guru2_id}. Lihat {@link #guru} — termasuk peringatan getter yang menghapus isi field bila hari slot yang bersangkutan kosong.
	 */
	private Guru guru2;
	/**
	 * <b>Guru pengampu slot ke-3</b>; kolom {@code guru3_id}. Lihat {@link #guru} — termasuk peringatan getter yang menghapus isi field bila hari slot yang bersangkutan kosong.
	 */
	private Guru guru3;
	/**
	 * <b>Guru pengampu slot ke-4</b>; kolom {@code guru4_id}. Lihat {@link #guru} — termasuk peringatan getter yang menghapus isi field bila hari slot yang bersangkutan kosong.
	 */
	private Guru guru4;
	/**
	 * <b>Guru pengampu slot ke-5</b>; kolom {@code guru5_id}. Lihat {@link #guru} — termasuk peringatan getter yang menghapus isi field bila hari slot yang bersangkutan kosong.
	 */
	private Guru guru5;
	/**
	 * <b>Guru pengampu slot ke-6</b>; kolom {@code guru6_id}. Lihat {@link #guru} — termasuk peringatan getter yang menghapus isi field bila hari slot yang bersangkutan kosong.
	 */
	private Guru guru6;
	/**
	 * <b>Guru pengampu slot ke-7</b>; kolom {@code guru7_id}. Lihat {@link #guru} — termasuk peringatan getter yang menghapus isi field bila hari slot yang bersangkutan kosong.
	 */
	private Guru guru7;
	/**
	 * <b>Guru pengampu slot ke-8</b>; kolom {@code guru8_id}. Lihat {@link #guru} — termasuk peringatan getter yang menghapus isi field bila hari slot yang bersangkutan kosong.
	 */
	private Guru guru8;
	/**
	 * <b>Guru pengampu slot ke-9</b>; kolom {@code guru9_id}. Lihat {@link #guru} — termasuk peringatan getter yang menghapus isi field bila hari slot yang bersangkutan kosong.
	 */
	private Guru guru9;
	/**
	 * <b>Guru pengampu slot ke-10</b>; kolom {@code guru10_id}. Lihat {@link #guru} — termasuk peringatan getter yang menghapus isi field bila hari slot yang bersangkutan kosong.
	 */
	private Guru guru10;
	/**
	 * <b>Guru pengampu slot ke-11</b>; kolom {@code guru11_id}. Lihat {@link #guru} — termasuk peringatan getter yang menghapus isi field bila hari slot yang bersangkutan kosong.
	 */
	private Guru guru11;
	/**
	 * <b>Guru pengampu slot ke-12</b>; kolom {@code guru12_id}. Lihat {@link #guru} — termasuk peringatan getter yang menghapus isi field bila hari slot yang bersangkutan kosong.
	 */
	private Guru guru12;
	/**
	 * Sekolah pemilik baris jadwal, kolom {@code sekolah_id}. Nilai efektifnya diturunkan ulang
	 * setiap kali dibaca — lihat {@link #getSekolah()}.
	 */
	private Sekolah sekolah;
	/**
	 * Yayasan pemilik baris jadwal, kolom {@code yayasan_id}. Nilai efektifnya diturunkan dari
	 * {@link #sekolah} setiap kali dibaca — lihat {@link #getYayasan()}.
	 */
	private Yayasan yayasan;
	/**
	 * <b>Hari slot ke-1</b> dari dua belas slot tetap; kolom {@code hari} dengan
	 * {@code length = 6}.
	 *
	 * <p>Isinya salah satu dari {@code Common.haris} =
	 * {"Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jum'at", "Sabtu"} — teks bebas, bukan
	 * enum. Perhatikan bahwa panjang kolom 6 karakter <b>pas persis</b> menampung "Minggu",
	 * "Selasa" dan "Jum'at"; tidak ada ruang untuk ejaan lain ("Jumat" memang lebih pendek,
	 * tetapi "Minggu " berspasi atau nama hari berbahasa lain akan terpotong/ditolak).</p>
	 *
	 * <p>Kombinasi {@code hariN} + {@code jamPelajaranN} + {@code guruN} + {@code subMatapelajaranN}
	 * membentuk satu slot utuh. Pada layar, empat komponen itu berdiri di satu baris berlabel
	 * "Hari dan Jam Pelajaran I" … "XII".</p>
	 */
	private String hari;
	/**
	 * <b>Hari slot ke-2</b>; kolom {@code hari2}. Lihat {@link #hari} untuk daftar nilai yang sah dan penjelasan model dua belas slot. Berbeda dari slot ke-1, kolom ini <b>tidak</b> membawa anotasi {@code @Column} sehingga dipetakan otomatis memakai nama properti apa adanya.
	 */
	private String hari2;
	/**
	 * <b>Hari slot ke-3</b>; kolom {@code hari3}. Lihat {@link #hari} untuk daftar nilai yang sah dan penjelasan model dua belas slot. Berbeda dari slot ke-1, kolom ini <b>tidak</b> membawa anotasi {@code @Column} sehingga dipetakan otomatis memakai nama properti apa adanya.
	 */
	private String hari3;
	/**
	 * <b>Hari slot ke-4</b>; kolom {@code hari4}. Lihat {@link #hari} untuk daftar nilai yang sah dan penjelasan model dua belas slot. Berbeda dari slot ke-1, kolom ini <b>tidak</b> membawa anotasi {@code @Column} sehingga dipetakan otomatis memakai nama properti apa adanya.
	 */
	private String hari4;
	/**
	 * <b>Hari slot ke-5</b>; kolom {@code hari5}. Lihat {@link #hari} untuk daftar nilai yang sah dan penjelasan model dua belas slot. Berbeda dari slot ke-1, kolom ini <b>tidak</b> membawa anotasi {@code @Column} sehingga dipetakan otomatis memakai nama properti apa adanya.
	 */
	private String hari5;
	/**
	 * <b>Hari slot ke-6</b>; kolom {@code hari6}. Lihat {@link #hari} untuk daftar nilai yang sah dan penjelasan model dua belas slot. Berbeda dari slot ke-1, kolom ini <b>tidak</b> membawa anotasi {@code @Column} sehingga dipetakan otomatis memakai nama properti apa adanya.
	 */
	private String hari6;
	/**
	 * <b>Hari slot ke-7</b>; kolom {@code hari7}. Lihat {@link #hari} untuk daftar nilai yang sah dan penjelasan model dua belas slot. Berbeda dari slot ke-1, kolom ini <b>tidak</b> membawa anotasi {@code @Column} sehingga dipetakan otomatis memakai nama properti apa adanya.
	 */
	private String hari7;
	/**
	 * <b>Hari slot ke-8</b>; kolom {@code hari8}. Lihat {@link #hari} untuk daftar nilai yang sah dan penjelasan model dua belas slot. Berbeda dari slot ke-1, kolom ini <b>tidak</b> membawa anotasi {@code @Column} sehingga dipetakan otomatis memakai nama properti apa adanya.
	 */
	private String hari8;
	/**
	 * <b>Hari slot ke-9</b>; kolom {@code hari9}. Lihat {@link #hari} untuk daftar nilai yang sah dan penjelasan model dua belas slot. Berbeda dari slot ke-1, kolom ini <b>tidak</b> membawa anotasi {@code @Column} sehingga dipetakan otomatis memakai nama properti apa adanya.
	 */
	private String hari9;
	/**
	 * <b>Hari slot ke-10</b>; kolom {@code hari10}. Lihat {@link #hari} untuk daftar nilai yang sah dan penjelasan model dua belas slot. Berbeda dari slot ke-1, kolom ini <b>tidak</b> membawa anotasi {@code @Column} sehingga dipetakan otomatis memakai nama properti apa adanya.
	 */
	private String hari10;
	/**
	 * <b>Hari slot ke-11</b>; kolom {@code hari11}. Lihat {@link #hari} untuk daftar nilai yang sah dan penjelasan model dua belas slot. Berbeda dari slot ke-1, kolom ini <b>tidak</b> membawa anotasi {@code @Column} sehingga dipetakan otomatis memakai nama properti apa adanya.
	 */
	private String hari11;
	/**
	 * <b>Hari slot ke-12</b>; kolom {@code hari12}. Lihat {@link #hari} untuk daftar nilai yang sah dan penjelasan model dua belas slot. Berbeda dari slot ke-1, kolom ini <b>tidak</b> membawa anotasi {@code @Column} sehingga dipetakan otomatis memakai nama properti apa adanya.
	 */
	private String hari12;

	/**
	 * <b>Sub mata pelajaran slot ke-1</b>; kolom {@code sub_matapelajaran}.
	 *
	 * <p>Dua belas field {@code subMatapelajaran} … {@code subMatapelajaran12} memungkinkan
	 * tiap slot mengampu cabang yang berbeda dari satu mata pelajaran induk (mis. mapel "IPA"
	 * dengan sub "Fisika" pada slot Senin dan "Biologi" pada slot Rabu). Relasinya opsional;
	 * slot tanpa sub mata pelajaran berarti mengajarkan mapel induk apa adanya.</p>
	 *
	 * @see SubMatapelajaran
	 */
	private SubMatapelajaran subMatapelajaran;
	/**
	 * <b>Sub mata pelajaran slot ke-2</b>; kolom {@code sub_matapelajaran_2}. Lihat {@link #subMatapelajaran}.
	 */
	private SubMatapelajaran subMatapelajaran2;
	/**
	 * <b>Sub mata pelajaran slot ke-3</b>; kolom {@code sub_matapelajaran_3}. Lihat {@link #subMatapelajaran}.
	 */
	private SubMatapelajaran subMatapelajaran3;
	/**
	 * <b>Sub mata pelajaran slot ke-4</b>; kolom {@code sub_matapelajaran_4}. Lihat {@link #subMatapelajaran}.
	 */
	private SubMatapelajaran subMatapelajaran4;
	/**
	 * <b>Sub mata pelajaran slot ke-5</b>; kolom {@code sub_matapelajaran_5}. Lihat {@link #subMatapelajaran}.
	 */
	private SubMatapelajaran subMatapelajaran5;
	/**
	 * <b>Sub mata pelajaran slot ke-6</b>; kolom {@code sub_matapelajaran_6}. Lihat {@link #subMatapelajaran}.
	 */
	private SubMatapelajaran subMatapelajaran6;
	/**
	 * <b>Sub mata pelajaran slot ke-7</b>; kolom {@code sub_matapelajaran_7}. Lihat {@link #subMatapelajaran}.
	 */
	private SubMatapelajaran subMatapelajaran7;
	/**
	 * <b>Sub mata pelajaran slot ke-8</b>; kolom {@code sub_matapelajaran_8}. Lihat {@link #subMatapelajaran}.
	 */
	private SubMatapelajaran subMatapelajaran8;
	/**
	 * <b>Sub mata pelajaran slot ke-9</b>; kolom {@code sub_matapelajaran_9}. Lihat {@link #subMatapelajaran}.
	 */
	private SubMatapelajaran subMatapelajaran9;
	/**
	 * <b>Sub mata pelajaran slot ke-10</b>; kolom {@code sub_matapelajaran_10}. Lihat {@link #subMatapelajaran}.
	 */
	private SubMatapelajaran subMatapelajaran10;
	/**
	 * <b>Sub mata pelajaran slot ke-11</b>; kolom {@code sub_matapelajaran_11}. Lihat {@link #subMatapelajaran}.
	 */
	private SubMatapelajaran subMatapelajaran11;
	/**
	 * <b>Sub mata pelajaran slot ke-12</b>; kolom {@code sub_matapelajaran_12}. Lihat {@link #subMatapelajaran}.
	 */
	private SubMatapelajaran subMatapelajaran12;

	/**
	 * Semester berjalan sebagai <b>angka</b>: {@code 1} = ganjil, {@code 2} = genap; kolom
	 * {@code semester} ({@code nullable = false}). Jangan tertukar dengan konstanta teks
	 * {@link #GANJIL}/{@link #GENAP} yang dipakai combobox dan dasbor. Bila masih {@code null},
	 * {@link #getSemester()} mengisinya dari kalender akademik yang sedang berjalan.
	 */
	private Integer semester;
	/**
	 * Tahun ajaran dalam format {@code "2025/2026"}; kolom {@code tahun_ajaran}
	 * ({@code length = 9}, {@code nullable = false}). Nilai efektifnya dapat ditimpa saat
	 * dibaca — lihat {@link #getTahunAjaran()}.
	 */
	private String tahunAjaran;
	/**
	 * Catatan bebas administrator atas baris jadwal (dipakai kolom "Keterangan" pada grid dan
	 * sebagai penyaring pencarian {@code searchketerangan}). Getter-nya tidak membawa anotasi
	 * {@code @Column} sehingga dipetakan otomatis memakai nama properti apa adanya.
	 */
	private String keterangan;
	/**
	 * Nama program/rombongan penyelenggaraan (mis. "Reguler", "Akselerasi", "Boarding");
	 * default {@code "Reguler"} bila kosong — lihat {@link #getProgram()}. Dipakai untuk
	 * mencocokkan jadwal dengan {@link MasaJadwalPelajaran} yang juga memiliki kolom
	 * {@code program}.
	 */
	private String program;

	/**
	 * Jenis pengulangan jadwal; default {@code "Mingguan"} bila kosong — lihat
	 * {@link #getJenis()}. Menentukan bagaimana generator pertemuan menebar tanggal dari
	 * {@link #getTanggalMulaiJadwalPelajaran()}.
	 *
	 * <p><b>Catatan:</b> jangan tertukar dengan {@link JenisJadwalPelajaran}. Entity itu
	 * sama sekali <b>tidak</b> berelasi dengan baris jadwal; ia menempel pada
	 * {@link JamPelajaran} (kolom {@code jam_pelajaran.jenis_jadwal_pelajaran_id}, menu
	 * "Jenis Jam Pelajaran"). Kolom {@code jenis} di sini hanyalah teks bebas milik baris
	 * jadwal sendiri.</p>
	 */
	private String jenis;
	/**
	 * Saklar kebijakan: apakah guru pengampu boleh menggeser tanggal pertemuan yang
	 * dibangkitkan dari jadwal ini. <b>Default {@code true}</b> bila {@code null} — lihat
	 * {@link #getGuruBisaMerubahTanggalJadwalPelajaran()}; artinya instalasi baru bersifat
	 * permisif sampai administrator secara sadar mematikannya.
	 */
	private Boolean guruBisaMerubahTanggalJadwalPelajaran;
	/**
	 * Tanggal mulai berlakunya jadwal — titik awal penebaran pertemuan. Nilai efektifnya
	 * ditimpa dari {@link MasaJadwalPelajaran#getMulai()} setiap kali dibaca bila masa jadwal
	 * terisi — lihat {@link #getTanggalMulaiJadwalPelajaran()}.
	 */
	private Date tanggalMulaiJadwalPelajaran;
	/**
	 * Saklar: apakah generator pertemuan melompati hari libur nasional. <b>Default
	 * {@code true}</b> bila {@code null} — lihat {@link #getLewatiTanggalMerahNasional()}.
	 */
	private Boolean lewatiTanggalMerahNasional;
	/**
	 * Penanda bahwa baris ini adalah pembelajaran <b>tanpa jadwal tetap</b> (mis. mandiri,
	 * proyek, praktik lapangan). Default {@code false}. Bila {@code true},
	 * {@link #info()} dan {@link #infoSimple()} <b>menghilangkan seluruh bagian hari/jam</b>
	 * dari teks ringkasan, sehingga dua belas slot tidak pernah ditampilkan di manapun teks
	 * itu dipakai. Penamaan bergaris bawah dipertahankan karena sudah menjadi nama properti
	 * yang dirujuk pemakai lain.
	 */
	private Boolean merupakan_tanpa_jadwal_perkuliahan;
	/**
	 * Saklar: apakah presensi guru hanya boleh diinput pada rentang hari/jam sesuai slot
	 * jadwal. <b>Default {@code false}</b> bila {@code null} — lihat
	 * {@link #getKehadiranGuruHarusDiinputSesuaiJadwal()}; berbeda arah dengan
	 * {@link #guruBisaMerubahTanggalJadwalPelajaran} dan
	 * {@link #lewatiTanggalMerahNasional} yang default {@code true}.
	 */
	private Boolean kehadiranGuruHarusDiinputSesuaiJadwal;
	/**
	 * Pengguna yang mengunci baris jadwal, kolom {@code dikunci}. Bernilai {@code null} berarti
	 * <b>tidak terkunci</b>; terisi berarti baris dianggap final. Pola yang sama dipakai
	 * {@code Perkuliahan}, {@code Mahasiswa} dan {@code StatuskehadiranKaryawanHarian}: layar
	 * membekukan grid dan menyembunyikan tombol ubah ketika {@code getDikunci() != null}.
	 */
	private Tbmuser dikunci;
	/**
	 * Deskripsi pembelajaran (teks panjang, {@code columnDefinition = "text"}). Dipakai
	 * membangun berkas RPP/RPS otomatis — lihat
	 * {@code JadwalPelajaranAction.generateiIntroductoryText(...)} bagian "2. Deskripsi
	 * Pembelajaran".
	 */
	private String deskripsiPembelajaran;
	/**
	 * Capaian/kompetensi pembelajaran (teks panjang). Dipakai pada bagian
	 * "3. Capaian / Kompetensi" berkas RPP/RPS otomatis. Meski namanya menyebut "Prodi"
	 * (warisan modul perguruan tinggi), pada modul sekolah isinya adalah capaian mata
	 * pelajaran.
	 */
	private String capaianPembelajaranProdi;

	/**
	 * Salinan teks jam mulai ("07.00") — <b>data turunan yang ikut tersimpan</b>. Ditulis ulang
	 * dari {@code jamPelajaran.getMulaiS()} setiap kali {@link #getWaktuMulai()} dipanggil.
	 * Berbeda dari entity saudaranya {@code Perkuliahan} yang memetakan kolom ini secara
	 * eksplisit ({@code @Column(name = "waktu_mulai")}), di sini getter-nya tanpa anotasi
	 * sehingga dipetakan otomatis memakai nama properti apa adanya.
	 */
	private String waktuMulai;
	/**
	 * Salinan teks jam selesai; pasangan {@link #waktuMulai}. Ditulis ulang dari
	 * {@code jamPelajaran.getSampaiS()} setiap kali {@link #getWaktuSelesai()} dipanggil.
	 */
	private String waktuSelesai;
	/**
	 * Ruang tempat pembelajaran berlangsung, kolom {@code ruang} ({@code nullable = true}).
	 * Nilai efektifnya ditimpa dari kelas les / kelas reguler setiap kali dibaca — lihat
	 * {@link #getRuang()}. Ruang adalah salah satu dari tiga sumbu deteksi bentrok
	 * (ruang, kelas, guru).
	 */
	private Ruang ruang;

	/**
	 * Masa berlaku jadwal (rentang tanggal + tahun ajaran + semester + program), kolom
	 * {@code masa_jadwal_pelajaran}. Dipakai sebagai pemasok
	 * {@link #getTanggalMulaiJadwalPelajaran()}. Bila kosong, {@link #getMasaJadwalPelajaran()}
	 * akan <b>memilihkan sendiri</b> masa berpenanda {@code defaultData} yang cocok tahun
	 * ajaran dan semesternya.
	 */
	private MasaJadwalPelajaran masaJadwalPelajaran;
	/**
	 * Baris kurikulum (pasangan kurikulum–mata pelajaran) yang menurunkan jadwal ini, kolom
	 * {@code kurikulum_punya_matapelajaran}. Terisi bila jadwal dibuat lewat tombol
	 * "Tambah Berdasarkan Kurikulum". Bila terisi, ia menjadi sumber
	 * {@link #getMatapelajaran()} dan salah satu sumber {@link #getSekolah()}.
	 */
	private KurikulumPunyaMatapelajaran kurikulumPunyaMatapelajaran;
	/**
	 * Saklar per-baris "Abaikan jika ada jadwal bentrok" (label persis pada layar). Default
	 * {@code false}.
	 *
	 * <p><b>Semantik yang perlu diketahui:</b> saklar ini mematikan pemeriksaan bentrok
	 * <b>dua arah</b>. Ketiga pemeriksa
	 * ({@code checkBentrokBerdasarRuangan}/{@code checkBentrokBerdasarKelas}/{@code checkBentrokBerdasarGuru})
	 * melewati baris ini <b>baik ketika ia yang sedang disimpan maupun ketika ia yang menjadi
	 * pembanding</b>. Jadi satu baris jadwal yang dicentang "abaikan" membuat dirinya kebal
	 * <i>dan</i> tak terlihat oleh baris lain — ruang/kelas/guru yang sudah dipakainya tidak
	 * lagi dianggap terpakai oleh siapa pun.</p>
	 */
	private Boolean abaikanBentrok;

	/**
	 * Teks pendahuluan RPP/RPS (teks panjang), disimpan sebagai HTML hasil generator
	 * {@code generateiIntroductoryText(...)} atau suntingan manual pengguna.
	 */
	private String pendahuluan;

	/**
	 * Representasi teks ringkas untuk keperluan debug, log, dan sebagai <b>kunci
	 * pengelompokan</b> di beberapa layar. Bentuknya
	 * {@code id_matapelajaran_kelas_guru_semester_tahunAjaran}, dengan tiap bagian memakai
	 * {@code toString()} entity terkait.
	 *
	 * <p><b>Efek samping yang tidak kentara:</b> karena memanggil {@link #getMatapelajaran()},
	 * {@link #getKelas()}, {@link #getGuru()}, {@link #getSemester()} dan
	 * {@link #getTahunAjaran()}, sekadar mencetak objek ini dapat memicu seluruh penulisan
	 * balik yang dijelaskan pada Javadoc kelas — termasuk {@link #getGuru()} yang
	 * mengosongkan {@code guru_id} bila hari slot ke-1 kosong.</p>
	 *
	 * @return teks gabungan identitas jadwal
	 */
	public String toString() {
		return getId() + "_" + getMatapelajaran() + "_" + getKelas() + "_" + getGuru() + "_" + getSemester() + "_"
				+ getTahunAjaran();
	}

	/**
	 * Mengembalikan teks pendahuluan RPP/RPS.
	 *
	 * @return teks pendahuluan sudah di-{@code trim()}, atau string kosong bila belum diisi
	 *         (tidak pernah {@code null})
	 */
	@Column(columnDefinition = "text")
	public String getPendahuluan() {
		return pendahuluan == null ? "" : pendahuluan.trim();
	}

	/**
	 * Menyetel teks pendahuluan RPP/RPS.
	 *
	 * @param pendahuluan teks (umumnya HTML); boleh {@code null}
	 */
	public void setPendahuluan(String pendahuluan) {
		this.pendahuluan = pendahuluan;
	}

	/**
	 * Mengumpulkan <b>id</b> seluruh {@link JamPelajaran} dari dua belas slot yang benar-benar
	 * terpakai (slot dianggap terpakai bila objek jam-nya ada <i>dan</i>
	 * {@code getMulai() != null}).
	 *
	 * <p><b>Kuirk:</b> variabel lokalnya bernama {@code gurus} — sisa salin-tempel dari
	 * {@link #populateGuru()}; isinya id jam pelajaran, bukan guru. Metode ini juga
	 * <b>tidak memiliki satu pun pemanggil</b> di seluruh basis kode saat ini (kode mati yang
	 * dipertahankan).</p>
	 *
	 * @return daftar id jam pelajaran urut slot 1..12; kosong bila tak ada slot terpakai
	 */
	public List<Long> populateWaktuById() {
		List<Long> gurus = new ArrayList<Long>();

		if (getJamPelajaran() != null && getJamPelajaran().getMulai() != null) {
			gurus.add(getJamPelajaran().getId());
		}
		if (getJamPelajaran2() != null && getJamPelajaran2().getMulai() != null) {
			gurus.add(getJamPelajaran2().getId());
		}
		if (getJamPelajaran3() != null && getJamPelajaran3().getMulai() != null) {
			gurus.add(getJamPelajaran3().getId());
		}
		if (getJamPelajaran4() != null && getJamPelajaran4().getMulai() != null) {
			gurus.add(getJamPelajaran4().getId());
		}
		if (getJamPelajaran5() != null && getJamPelajaran5().getMulai() != null) {
			gurus.add(getJamPelajaran5().getId());
		}
		if (getJamPelajaran6() != null && getJamPelajaran6().getMulai() != null) {
			gurus.add(getJamPelajaran6().getId());
		}

		if (getJamPelajaran7() != null && getJamPelajaran7().getMulai() != null) {
			gurus.add(getJamPelajaran7().getId());
		}
		if (getJamPelajaran8() != null && getJamPelajaran8().getMulai() != null) {
			gurus.add(getJamPelajaran8().getId());
		}
		if (getJamPelajaran9() != null && getJamPelajaran9().getMulai() != null) {
			gurus.add(getJamPelajaran9().getId());
		}
		if (getJamPelajaran10() != null && getJamPelajaran10().getMulai() != null) {
			gurus.add(getJamPelajaran10().getId());
		}

		if (getJamPelajaran11() != null && getJamPelajaran11().getMulai() != null) {
			gurus.add(getJamPelajaran11().getId());
		}

		if (getJamPelajaran12() != null && getJamPelajaran12().getMulai() != null) {
			gurus.add(getJamPelajaran12().getId());
		}

		return gurus;
	}

	/**
	 * Mengumpulkan slot yang <b>lengkap</b> menjadi larik {@code {JamPelajaran, hari, Guru}}.
	 *
	 * <p>Sebuah slot ke-N ikut terkumpul hanya bila <b>ketiganya</b> terisi:
	 * {@code jamPelajaranN != null}, {@code hariN} tidak kosong, dan {@code guruN != null}.
	 * Inilah bentuk yang dipakai ketiga pemeriksa bentrok di
	 * {@code JadwalPelajaranAction} ({@code checkBentrokBerdasarRuangan},
	 * {@code checkBentrokBerdasarKelas}, {@code checkBentrokBerdasarGuru}) dan oleh
	 * {@code GuruMengajarAction} saat mencocokkan jam mengajar.</p>
	 *
	 * <p><b>Konsekuensi penyaringan bertiga:</b> slot yang harinya sudah diisi tetapi gurunya
	 * belum ditentukan <b>tidak pernah diperiksa bentroknya</b>. Ruang dan kelas pada slot
	 * seperti itu dapat dipesan ganda tanpa peringatan sampai gurunya diisi.</p>
	 *
	 * @return daftar larik 3 elemen ({@code [0]} {@link JamPelajaran}, {@code [1]} nama hari,
	 *         {@code [2]} {@link Guru}) urut slot 1..12
	 */
	public List<Object[]> populateJamPelajaran() {
		List<Object[]> gurus = new ArrayList<Object[]>();

		if (getJamPelajaran() != null && getHari() != null && !getHari().isEmpty() && getGuru() != null) {
			gurus.add(new Object[] { getJamPelajaran(), getHari(), getGuru() });
		}
		if (getJamPelajaran2() != null && getHari2() != null && !getHari2().isEmpty() && getGuru2() != null) {
			gurus.add(new Object[] { getJamPelajaran2(), getHari2(), getGuru2() });
		}
		if (getJamPelajaran3() != null && getHari3() != null && !getHari3().isEmpty() && getGuru3() != null) {
			gurus.add(new Object[] { getJamPelajaran3(), getHari3(), getGuru3() });
		}
		if (getJamPelajaran4() != null && getHari4() != null && !getHari4().isEmpty() && getGuru4() != null) {
			gurus.add(new Object[] { getJamPelajaran4(), getHari4(), getGuru4() });
		}
		if (getJamPelajaran5() != null && getHari5() != null && !getHari5().isEmpty() && getGuru5() != null) {
			gurus.add(new Object[] { getJamPelajaran5(), getHari5(), getGuru5() });
		}
		if (getJamPelajaran6() != null && getHari6() != null && !getHari6().isEmpty() && getGuru6() != null) {
			gurus.add(new Object[] { getJamPelajaran6(), getHari6(), getGuru6() });
		}
		if (getJamPelajaran7() != null && getHari7() != null && !getHari7().isEmpty() && getGuru7() != null) {
			gurus.add(new Object[] { getJamPelajaran7(), getHari7(), getGuru7() });
		}
		if (getJamPelajaran8() != null && getHari8() != null && !getHari8().isEmpty() && getGuru8() != null) {
			gurus.add(new Object[] { getJamPelajaran8(), getHari8(), getGuru8() });
		}
		if (getJamPelajaran9() != null && getHari9() != null && !getHari9().isEmpty() && getGuru9() != null) {
			gurus.add(new Object[] { getJamPelajaran9(), getHari9(), getGuru9() });
		}
		if (getJamPelajaran10() != null && getHari10() != null && !getHari10().isEmpty() && getGuru10() != null) {
			gurus.add(new Object[] { getJamPelajaran10(), getHari10(), getGuru10() });
		}

		if (getJamPelajaran11() != null && getHari11() != null && !getHari11().isEmpty() && getGuru11() != null) {
			gurus.add(new Object[] { getJamPelajaran11(), getHari11(), getGuru11() });
		}

		if (getJamPelajaran12() != null && getHari12() != null && !getHari12().isEmpty() && getGuru12() != null) {
			gurus.add(new Object[] { getJamPelajaran12(), getHari12(), getGuru12() });
		}

		return gurus;
	}

	/**
	 * Mengumpulkan nama hari dari dua belas slot yang harinya terisi.
	 *
	 * <p>Dipakai layar untuk memutuskan berapa baris slot yang perlu ditampilkan: bila
	 * {@code populateHari().size() == 1}, {@code JadwalPelajaranAction} menampilkan tombol
	 * "Tambah Jadwal Mengajar" dan menyembunyikan baris slot II–XII. Juga dipakai
	 * {@code PenjadwalanSiswaHelper} dan {@code GuruMengajarAction}.</p>
	 *
	 * <p><b>Kuirk:</b> variabel lokalnya bernama {@code gurus} (sisa salin-tempel) dan daftar
	 * yang dikembalikan <b>boleh berisi duplikat</b> — satu hari yang dipakai beberapa slot
	 * akan muncul beberapa kali.</p>
	 *
	 * @return daftar nama hari urut slot 1..12, mungkin memuat duplikat
	 */
	public List<String> populateHari() {
		List<String> gurus = new ArrayList<String>();

		if (getHari() != null) {
			gurus.add(getHari());
		}
		if (getHari2() != null) {
			gurus.add(getHari2());
		}
		if (getHari3() != null) {
			gurus.add(getHari3());
		}
		if (getHari4() != null) {
			gurus.add(getHari4());
		}
		if (getHari5() != null) {
			gurus.add(getHari5());
		}
		if (getHari6() != null) {
			gurus.add(getHari6());
		}

		if (getHari7() != null) {
			gurus.add(getHari7());
		}
		if (getHari8() != null) {
			gurus.add(getHari8());
		}
		if (getHari9() != null) {
			gurus.add(getHari9());
		}
		if (getHari10() != null) {
			gurus.add(getHari10());
		}

		if (getHari11() != null) {
			gurus.add(getHari11());
		}

		if (getHari12() != null) {
			gurus.add(getHari12());
		}

		return gurus;
	}

	/**
	 * Mengumpulkan jam mulai tiap slot terpakai sebagai teks berformat
	 * {@code Common.timeFormat2}.
	 *
	 * <p><b>Kode mati:</b> seperti {@link #populateWaktuById()}, metode ini tidak memiliki
	 * pemanggil di seluruh basis kode saat ini. Variabel lokalnya juga masih bernama
	 * {@code gurus}.</p>
	 *
	 * @return daftar teks jam mulai urut slot 1..12
	 */
	public List<String> populateWaktuMulai() {
		List<String> gurus = new ArrayList<String>();

		if (getJamPelajaran() != null && getJamPelajaran().getMulai() != null) {
			gurus.add(Common.timeFormat2.get().format(getJamPelajaran().getMulai()));
		}
		if (getJamPelajaran2() != null && getJamPelajaran2().getMulai() != null) {
			gurus.add(Common.timeFormat2.get().format(getJamPelajaran2().getMulai()));
		}
		if (getJamPelajaran3() != null && getJamPelajaran3().getMulai() != null) {
			gurus.add(Common.timeFormat2.get().format(getJamPelajaran3().getMulai()));
		}
		if (getJamPelajaran4() != null && getJamPelajaran4().getMulai() != null) {
			gurus.add(Common.timeFormat2.get().format(getJamPelajaran4().getMulai()));
		}
		if (getJamPelajaran5() != null && getJamPelajaran5().getMulai() != null) {
			gurus.add(Common.timeFormat2.get().format(getJamPelajaran5().getMulai()));
		}
		if (getJamPelajaran6() != null && getJamPelajaran6().getMulai() != null) {
			gurus.add(Common.timeFormat2.get().format(getJamPelajaran6().getMulai()));
		}

		if (getJamPelajaran7() != null && getJamPelajaran7().getMulai() != null) {
			gurus.add(Common.timeFormat2.get().format(getJamPelajaran7().getMulai()));
		}
		if (getJamPelajaran8() != null && getJamPelajaran8().getMulai() != null) {
			gurus.add(Common.timeFormat2.get().format(getJamPelajaran8().getMulai()));
		}
		if (getJamPelajaran9() != null && getJamPelajaran9().getMulai() != null) {
			gurus.add(Common.timeFormat2.get().format(getJamPelajaran9().getMulai()));
		}
		if (getJamPelajaran10() != null && getJamPelajaran10().getMulai() != null) {
			gurus.add(Common.timeFormat2.get().format(getJamPelajaran10().getMulai()));
		}

		if (getJamPelajaran11() != null && getJamPelajaran11().getMulai() != null) {
			gurus.add(Common.timeFormat2.get().format(getJamPelajaran11().getMulai()));
		}

		if (getJamPelajaran12() != null && getJamPelajaran12().getMulai() != null) {
			gurus.add(Common.timeFormat2.get().format(getJamPelajaran12().getMulai()));
		}

		return gurus;
	}

	/**
	 * Mengumpulkan objek {@link JamPelajaran} dari dua belas slot yang terpakai
	 * (objek ada <i>dan</i> {@code getMulai() != null}), tanpa memandang hari.
	 *
	 * @return daftar jam pelajaran urut slot 1..12
	 * @see #populateWaktu(String)
	 */
	public List<JamPelajaran> populateWaktu() {
		List<JamPelajaran> gurus = new ArrayList<JamPelajaran>();

		if (getJamPelajaran() != null && getJamPelajaran().getMulai() != null) {
			gurus.add(getJamPelajaran());
		}
		if (getJamPelajaran2() != null && getJamPelajaran2().getMulai() != null) {
			gurus.add(getJamPelajaran2());
		}
		if (getJamPelajaran3() != null && getJamPelajaran3().getMulai() != null) {
			gurus.add(getJamPelajaran3());
		}
		if (getJamPelajaran4() != null && getJamPelajaran4().getMulai() != null) {
			gurus.add(getJamPelajaran4());
		}
		if (getJamPelajaran5() != null && getJamPelajaran5().getMulai() != null) {
			gurus.add(getJamPelajaran5());
		}
		if (getJamPelajaran6() != null && getJamPelajaran6().getMulai() != null) {
			gurus.add(getJamPelajaran6());
		}

		if (getJamPelajaran7() != null && getJamPelajaran7().getMulai() != null) {
			gurus.add(getJamPelajaran7());
		}
		if (getJamPelajaran8() != null && getJamPelajaran8().getMulai() != null) {
			gurus.add(getJamPelajaran8());
		}
		if (getJamPelajaran9() != null && getJamPelajaran9().getMulai() != null) {
			gurus.add(getJamPelajaran9());
		}
		if (getJamPelajaran10() != null && getJamPelajaran10().getMulai() != null) {
			gurus.add(getJamPelajaran10());
		}

		if (getJamPelajaran11() != null && getJamPelajaran11().getMulai() != null) {
			gurus.add(getJamPelajaran11());
		}

		if (getJamPelajaran12() != null && getJamPelajaran12().getMulai() != null) {
			gurus.add(getJamPelajaran12());
		}

		return gurus;
	}

	/**
	 * Varian {@link #populateWaktu()} yang <b>disaring per hari</b>: hanya slot yang
	 * {@code hariN}-nya sama dengan argumen (dibandingkan
	 * {@code equalsIgnoreCase}) yang ikut terkumpul.
	 *
	 * <p>Dipakai {@code PenjadwalanSiswaHelper} untuk menyusun kolom jadwal per hari
	 * ({@code Common.haris[hari - 1]}). Berbeda dari ketiga pemeriksa bentrok, di sini
	 * perbandingan hari benar-benar dilakukan <b>per slot</b> — bukan hanya terhadap slot
	 * ke-1.</p>
	 *
	 * @param hari nama hari yang dicari (mis. {@code "Senin"}); {@code null} menghasilkan
	 *             daftar kosong
	 * @return daftar jam pelajaran pada hari tersebut, urut slot 1..12
	 */
	public List<JamPelajaran> populateWaktu(String hari) {
		List<JamPelajaran> gurus = new ArrayList<JamPelajaran>();

		if (hari != null && getHari() != null && hari.equalsIgnoreCase(getHari())) {
			if (getJamPelajaran() != null && getJamPelajaran().getMulai() != null) {
				gurus.add(getJamPelajaran());
			}
		}

		if (hari != null && getHari2() != null && hari.equalsIgnoreCase(getHari2())) {
			if (getJamPelajaran2() != null && getJamPelajaran2().getMulai() != null) {
				gurus.add(getJamPelajaran2());
			}
		}

		if (hari != null && getHari3() != null && hari.equalsIgnoreCase(getHari3())) {
			if (getJamPelajaran3() != null && getJamPelajaran3().getMulai() != null) {
				gurus.add(getJamPelajaran3());
			}
		}

		if (hari != null && getHari4() != null && hari.equalsIgnoreCase(getHari4())) {
			if (getJamPelajaran4() != null && getJamPelajaran4().getMulai() != null) {
				gurus.add(getJamPelajaran4());
			}
		}
		if (hari != null && getHari5() != null && hari.equalsIgnoreCase(getHari5())) {
			if (getJamPelajaran5() != null && getJamPelajaran5().getMulai() != null) {
				gurus.add(getJamPelajaran5());
			}
		}
		if (hari != null && getHari6() != null && hari.equalsIgnoreCase(getHari6())) {
			if (getJamPelajaran6() != null && getJamPelajaran6().getMulai() != null) {
				gurus.add(getJamPelajaran6());
			}
		}

		if (hari != null && getHari7() != null && hari.equalsIgnoreCase(getHari7())) {
			if (getJamPelajaran7() != null && getJamPelajaran7().getMulai() != null) {
				gurus.add(getJamPelajaran7());
			}
		}
		if (hari != null && getHari8() != null && hari.equalsIgnoreCase(getHari8())) {
			if (getJamPelajaran8() != null && getJamPelajaran8().getMulai() != null) {
				gurus.add(getJamPelajaran8());
			}
		}
		if (hari != null && getHari9() != null && hari.equalsIgnoreCase(getHari9())) {
			if (getJamPelajaran9() != null && getJamPelajaran9().getMulai() != null) {
				gurus.add(getJamPelajaran9());
			}
		}
		if (hari != null && getHari10() != null && hari.equalsIgnoreCase(getHari10())) {
			if (getJamPelajaran10() != null && getJamPelajaran10().getMulai() != null) {
				gurus.add(getJamPelajaran10());
			}
		}

		if (hari != null && getHari11() != null && hari.equalsIgnoreCase(getHari11())) {
			if (getJamPelajaran11() != null && getJamPelajaran11().getMulai() != null) {
				gurus.add(getJamPelajaran11());
			}
		}

		if (hari != null && getHari12() != null && hari.equalsIgnoreCase(getHari12())) {
			if (getJamPelajaran12() != null && getJamPelajaran12().getMulai() != null) {
				gurus.add(getJamPelajaran12());
			}
		}

		return gurus;
	}

	/**
	 * Mengumpulkan seluruh guru pengampu dari dua belas slot ke dalam peta ber-<b>kunci
	 * {@code "<idJadwal>-<idGuru>"}</b>, sehingga satu guru yang mengampu beberapa slot pada
	 * baris jadwal yang sama hanya muncul sekali.
	 *
	 * <p>Inilah sumber daftar "Guru" pada kolom grid, pada {@link #info()}/{@link #infoSimple()},
	 * pada berkas RPP/RPS ({@code generateiIntroductoryText}), pada layar pertemuan
	 * ({@code PertemuanJadwalPelajaranAction}), dan pada penentuan penerima surel
	 * ({@code CommonEmail}).</p>
	 *
	 * <p><b>Efek samping penting:</b> metode ini memanggil {@code getGuru()}..{@code getGuru12()}
	 * sehingga ikut memicu penghapusan {@code guruN} pada slot yang harinya kosong (lihat
	 * {@link #getGuru()}). Karena {@link #info()} dan {@link #infoSimple()} memakainya, efek itu
	 * terpicu di hampir setiap tempat jadwal dirender.</p>
	 *
	 * <p><b>Kuirk kunci peta:</b> karena {@code getId()} ikut masuk ke kunci, peta dari baris
	 * jadwal <i>berbeda</i> tidak pernah bertabrakan — tetapi pada baris jadwal yang belum
	 * tersimpan ({@code id == null}) kuncinya menjadi {@code "null-<idGuru>"}, yang tetap
	 * konsisten selama peta tidak digabungkan lintas baris.</p>
	 *
	 * @return peta guru pengampu tanpa duplikat; kosong bila tak ada slot bergurus
	 */
	public Map<String, Guru> populateGuru() {
		Map<String, Guru> gurus = new HashMap<String, Guru>();

		if (getGuru() != null) {
			gurus.put(getId() + "-" + getGuru().getId(), getGuru());
		}
		if (getGuru2() != null) {
			gurus.put(getId() + "-" + getGuru2().getId(), getGuru2());
		}
		if (getGuru3() != null) {
			gurus.put(getId() + "-" + getGuru3().getId(), getGuru3());
		}
		if (getGuru4() != null) {
			gurus.put(getId() + "-" + getGuru4().getId(), getGuru4());
		}
		if (getGuru5() != null) {
			gurus.put(getId() + "-" + getGuru5().getId(), getGuru5());
		}
		if (getGuru6() != null) {
			gurus.put(getId() + "-" + getGuru6().getId(), getGuru6());
		}

		if (getGuru7() != null) {
			gurus.put(getId() + "-" + getGuru7().getId(), getGuru7());
		}
		if (getGuru8() != null) {
			gurus.put(getId() + "-" + getGuru8().getId(), getGuru8());
		}
		if (getGuru9() != null) {
			gurus.put(getId() + "-" + getGuru9().getId(), getGuru9());
		}
		if (getGuru10() != null) {
			gurus.put(getId() + "-" + getGuru10().getId(), getGuru10());
		}
		if (getGuru11() != null) {
			gurus.put(getId() + "-" + getGuru11().getId(), getGuru11());
		}

		if (getGuru12() != null) {
			gurus.put(getId() + "-" + getGuru12().getId(), getGuru12());
		}

		return gurus;
	}

	/**
	 * Konstruktor tanpa argumen — <b>wajib ada</b> agar Hibernate dapat menginstansiasi
	 * entity. Seluruh field dibiarkan {@code null} kecuali {@link #tanggal_dirubah} yang
	 * langsung diisi waktu server pada deklarasi field.
	 */
	public JadwalPelajaran() {
	}

	/**
	 * Mengembalikan kunci utama baris jadwal.
	 *
	 * <p>Letak anotasi {@code @Id} pada <b>getter</b> inilah yang menetapkan seluruh entity ini
	 * memakai <i>property access</i>: Hibernate membaca nilai lewat getter, bukan lewat field.
	 * Itulah sebab semua penulisan balik yang dijelaskan pada Javadoc kelas benar-benar
	 * tersimpan ke basis data.</p>
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Umumnya hanya dipanggil Hibernate; pemanggilan manual dipakai
	 * jalur unggah Excel yang menyertakan kolom {@code "id"}.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan slot jam ke-1.
	 *
	 * <p>Memanggil {@code check()} untuk menuntaskan proxy lazy menjadi instance kanonik
	 * (lihat {@link ais.database.model.GeneralValueObject#check(Object)}) dan
	 * <b>menuliskan hasilnya kembali</b> ke field. Penulisan balik ini hanya mengganti
	 * identitas objek, bukan nilai kolom.</p>
	 *
	 * @return jam pelajaran slot ke-1, atau {@code null} bila slot kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran_id")
	public JamPelajaran getJamPelajaran() {
		jamPelajaran = check(jamPelajaran);
		return this.jamPelajaran;
	}

	/**
	 * Menyetel slot jam ke-1.
	 *
	 * @param jamPelajaran jam pelajaran yang dipilih; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran(JamPelajaran jamPelajaran) {
		this.jamPelajaran = jamPelajaran;
	}

	/**
	 * Mengembalikan rombongan belajar yang dijadwalkan (jalur kelas reguler).
	 *
	 * <p>Hanya menuntaskan proxy lazy lewat {@code check()}; tidak menurunkan nilai dari
	 * relasi lain. Bandingkan dengan {@link #getKelasLesSiswa()} sebagai subjek alternatif.</p>
	 *
	 * @return kelas reguler, atau {@code null} bila jadwal ini bukan jadwal kelas reguler
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelas_id", nullable = true)
	public KelasSiswa getKelas() {
		kelas = check(kelas);
		return this.kelas;
	}

	/**
	 * Menyetel rombongan belajar yang dijadwalkan.
	 *
	 * @param kelas kelas reguler; {@code null} bila jadwal memakai kelas les atau kurikulum
	 */
	public void setKelas(KelasSiswa kelas) {
		this.kelas = kelas;
	}

	/**
	 * Mengembalikan mata pelajaran yang diajarkan.
	 *
	 * <p><b>Getter yang menulis balik dan bersifat menimpa.</b> Urutan sumbernya:</p>
	 * <ol>
	 * <li>bila {@link #getKelasLesSiswa()} terisi → diambil dari mata pelajaran kelas les;</li>
	 * <li>bila tidak, dan {@link #getKurikulumPunyaMatapelajaran()} terisi → diambil dari
	 *     baris kurikulum;</li>
	 * <li>bila keduanya kosong → nilai field sendiri (setelah {@code check()}).</li>
	 * </ol>
	 *
	 * <p>Pada dua cabang pertama nilai hasil turunan <b>ditulis ke field</b>, sehingga pada
	 * {@code flush} berikutnya kolom {@code matapelajaran_id} baris ini ikut berubah walau
	 * pengguna hanya membuka daftar. Bila mata pelajaran kelas les atau kurikulum kemudian
	 * diubah, seluruh jadwal turunannya ikut berpindah mata pelajaran tanpa layar peninjauan
	 * apa pun.</p>
	 *
	 * @return mata pelajaran efektif; {@code null} hanya bila ketiga sumber kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "matapelajaran_id", nullable = false)
	public Matapelajaran getMatapelajaran() {

		if (getKelasLesSiswa() != null) {
			matapelajaran = getKelasLesSiswa().getMatapelajaran();
		} else if (getKurikulumPunyaMatapelajaran() != null) {
			matapelajaran = getKurikulumPunyaMatapelajaran().getMatapelajaran();
		} else {
			matapelajaran = check(matapelajaran);
		}
		return this.matapelajaran;
	}

	/**
	 * Menyetel mata pelajaran. Perhatikan bahwa nilai yang disetel dapat <b>ditimpa kembali</b>
	 * oleh {@link #getMatapelajaran()} bila jadwal ini terikat kelas les atau baris kurikulum.
	 *
	 * @param matapelajaran mata pelajaran; kolomnya {@code nullable = false} sehingga
	 *                      {@code null} akan ditolak basis data saat simpan
	 */
	public void setMatapelajaran(Matapelajaran matapelajaran) {
		this.matapelajaran = matapelajaran;
	}

	/**
	 * Mengembalikan sekolah pemilik baris jadwal.
	 *
	 * <p><b>Getter yang menulis balik dan bersifat menimpa.</b> Field diisi ulang berurutan:
	 * mula-mula {@code check(sekolah)}, lalu <b>ditimpa</b> oleh sumber pertama yang tersedia —
	 * kelas les → kelas reguler → kurikulum ({@code kurikulumSekolah.sekolah}) → mata
	 * pelajaran. Baru bila keempatnya kosong nilai asli kolom {@code sekolah_id}
	 * dipertahankan.</p>
	 *
	 * <p>Konsekuensinya: kepemilikan tenant baris jadwal <b>bukan data yang berdiri sendiri</b>,
	 * melainkan turunan yang dihitung ulang tiap kali dibaca dan disimpan diam-diam. Memindahkan
	 * sebuah kelas ke sekolah lain akan menarik pindah seluruh jadwalnya pada pembacaan
	 * berikutnya.</p>
	 *
	 * @return sekolah efektif, atau {@code null} bila tak satu pun sumber terisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		if (getKelasLesSiswa() != null) {
			sekolah = getKelasLesSiswa().getSekolah();
		} else if (getKelas() != null) {
			sekolah = getKelas().getSekolah();
		} else if (getKurikulumPunyaMatapelajaran() != null) {
			sekolah = getKurikulumPunyaMatapelajaran().getKurikulumSekolah().getSekolah();
		} else if (getMatapelajaran() != null) {
			sekolah = getMatapelajaran().getSekolah();
		}

		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik.
	 *
	 * <p><b>Kuirk:</b> objek {@link Sekolah} yang belum tersimpan ({@code getId() == null})
	 * diperlakukan sama dengan {@code null} — field dikosongkan, bukan diisi objek transien.
	 * Ini mencegah cascade menyimpan sekolah baru secara tak sengaja.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau objek ber-id {@code null} mengosongkan
	 *                field
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik baris jadwal.
	 *
	 * <p><b>Getter yang menulis balik.</b> Selalu memanggil {@link #getSekolah()} lebih dulu
	 * (sehingga seluruh efek samping getter itu ikut terpicu), lalu bila sekolah ketemu
	 * menimpa field {@code yayasan} dengan {@code sekolah.getYayasan()}. Kolom
	 * {@code yayasan_id} karena itu adalah <b>salinan turunan</b> dari sekolah, bukan
	 * data mandiri.</p>
	 *
	 * @return yayasan efektif, atau {@code null} bila sekolah tidak diketahui
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
	 * Menyetel yayasan pemilik. Sama seperti {@link #setSekolah(Sekolah)}, objek yang belum
	 * tersimpan ({@code getId() == null}) diperlakukan sebagai {@code null}.
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau objek ber-id {@code null} mengosongkan
	 *                field
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Mengembalikan nama hari slot ke-1.
	 *
	 * <p><b>Normalisasi penting:</b> string kosong atau hanya spasi dikembalikan sebagai
	 * {@code null}, dan hasilnya selalu di-{@code trim()}. Pilihan "Tidak ada jadwal" pada
	 * layar menyetel field ini ke {@code null}.</p>
	 *
	 * <p>Nilai getter inilah — <b>bukan hari per slot</b> — yang dipakai ketiga pemeriksa
	 * bentrok sebagai syarat tambahan; lihat pembahasan pada Javadoc kelas.</p>
	 *
	 * @return nama hari slot ke-1, atau {@code null} bila slot ke-1 tidak dijadwalkan
	 */
	@Column(name = "hari", nullable = true, length = 6)
	public String getHari() {
		return this.hari == null || hari.trim().isEmpty() ? null : hari.trim();
	}

	/**
	 * Menyetel nama hari slot ke-1. Nilai disimpan apa adanya (tanpa {@code trim()});
	 * normalisasi dilakukan getter.
	 *
	 * @param hari salah satu nilai {@code Common.haris}; {@code null} berarti slot ke-1 tidak
	 *             dipakai
	 */
	public void setHari(String hari) {
		this.hari = hari;
	}

	/**
	 * Mengembalikan semester sebagai angka (1 = ganjil, 2 = genap).
	 *
	 * <p><b>Getter yang menulis balik:</b> bila field masih {@code null}, nilainya diisi dari
	 * kalender yang sedang berjalan ({@code Common.isNowSemensterGanjil() ? 1 : 2}) dan
	 * <b>ikut tersimpan</b>. Konsekuensinya, baris lama yang semesternya belum terisi akan
	 * memperoleh semester <i>saat baris itu kebetulan dibaca</i> — bukan semester sebenarnya
	 * ketika jadwal dibuat.</p>
	 *
	 * @return semester (1 atau 2); tidak pernah {@code null} setelah pemanggilan ini
	 */
	@Column(name = "semester", nullable = false)
	public Integer getSemester() {
		if (semester == null) {
			semester = Common.isNowSemensterGanjil() ? 1 : 2;
		}
		return this.semester;
	}

	/**
	 * Menyetel semester.
	 *
	 * @param semester 1 untuk ganjil, 2 untuk genap
	 */
	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	/**
	 * Mengembalikan tahun ajaran baris jadwal.
	 *
	 * <p><b>Getter yang menulis balik dan bersifat menimpa.</b> Bila {@link #getKelas()} terisi,
	 * tahun ajaran <b>selalu</b> diambil dari kelas dan menimpa nilai yang tersimpan; bila
	 * setelah itu masih {@code null}, diisi tahun akademik berjalan
	 * ({@code Common.getCurrentTahunAkademik()}).</p>
	 *
	 * <p>Efek nyatanya: begitu sebuah kelas dinaikkan ke tahun ajaran baru, seluruh baris
	 * jadwal historis milik kelas itu ikut berpindah tahun ajaran pada pembacaan berikutnya —
	 * dan karena {@code tahunAjaran} adalah salah satu penyaring utama layar dan laporan,
	 * jadwal lama dapat lenyap dari tahun ajaran aslinya.</p>
	 *
	 * @return tahun ajaran format {@code "2025/2026"}; tidak pernah {@code null} setelah
	 *         pemanggilan ini
	 */
	@Column(name = "tahun_ajaran", nullable = false, length = 9)
	public String getTahunAjaran() {
		kelas = getKelas();
		if (kelas != null) {
			tahunAjaran = kelas.getTahunAjaran();
		}

		if (tahunAjaran == null) {
			tahunAjaran = Common.getCurrentTahunAkademik();
		}
		return this.tahunAjaran;
	}

	/**
	 * Menyetel tahun ajaran. Nilai ini dapat <b>ditimpa kembali</b> oleh
	 * {@link #getTahunAjaran()} bila jadwal terikat pada sebuah kelas.
	 *
	 * @param tahunAjaran tahun ajaran format {@code "2025/2026"}
	 */
	public void setTahunAjaran(String tahunAjaran) {
		this.tahunAjaran = tahunAjaran;
	}

	/**
	 * Mengembalikan guru pengampu slot ke-1 (baris layar "Hari dan Jam Pelajaran I"); kolom
	 * {@code guru_id}.
	 *
	 * <p><b>GETTER DESTRUKTIF — pola yang sama berulang pada dua belas slot
	 * ({@code getGuru()} … {@code getGuru12()}).</b> Setelah {@code check()}, bila
	 * {@link #getHari()} bernilai {@code null} — yaitu ketika kolom hari slot ini kosong atau
	 * hanya berisi spasi — field {@code guru} <b>ditulisi {@code null}</b>, bukan sekadar
	 * disembunyikan dari nilai kembalian.</p>
	 *
	 * <p>Karena {@code @Id} dipasang pada getter, seluruh entity ini memakai <i>property
	 * access</i>: Hibernate membaca nilai lewat getter saat pengecekan kotor, sehingga
	 * {@code null} tersebut menjadi selisih terhadap snapshot muat dan — dengan
	 * {@code dynamicUpdate = true} — dikirim sebagai {@code UPDATE jadwal_pelajaran SET
	 * guru_id = null}. Penghapusan terjadi <b>tanpa satu pun tindakan pengguna</b>: cukup baris
	 * itu dirender di grid, masuk ke {@link #info(Guru)}/{@link #infoSimple()}, ke
	 * {@link #populateGuru()}, ke berkas RPP/RPS, atau ke keluaran API — semuanya jalur baca.</p>
	 *
	 * <p>Kombinasi data yang terkena: baris jadwal yang <b>gurunya sudah diisi tetapi harinya
	 * kemudian dikosongkan</b> (pilihan "Tidak ada jadwal" pada combobox hari menyetel hari ke
	 * {@code null} tanpa menyentuh guru). Data itu tidak dapat dipulihkan dari layar; jejaknya
	 * hanya tersisa di tabel Envers {@code @Audited}.</p>
	 *
	 * <p>Perlu dicatat pula bahwa {@link #populateJamPelajaran()} — sumber data ketiga pemeriksa
	 * bentrok — mensyaratkan {@code guruN != null}. Jadi setiap slot yang gurunya terhapus di
	 * sini otomatis <b>lenyap dari pemeriksaan bentrok</b> juga.</p>
	 *
	 * @return guru pengampu slot ke-1; {@code null} bila slot tak bergurus <b>atau</b> bila hari
	 *         slot ke-1 kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru_id")
	public Guru getGuru() {
		guru = check(guru);
		if (getHari() == null) {
			guru = null;
		}
		return guru;
	}

	/**
	 * Menyetel guru pengampu slot ke-1. Nilai yang disetel akan <b>dibuang oleh getter-nya</b>
	 * bila hari slot ke-1 tidak ikut diisi — lihat {@link #getGuru()}.
	 *
	 * @param guru guru pengampu; {@code null} mengosongkan slot
	 */
	public void setGuru(Guru guru) {
		this.guru = guru;
	}

	/**
	 * Mengembalikan catatan bebas administrator.
	 *
	 * @return keterangan apa adanya (tanpa normalisasi), atau {@code null}
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel catatan bebas administrator.
	 *
	 * @param keterangan teks bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan pengguna yang mengunci baris jadwal.
	 *
	 * @return pengguna pengunci, atau {@code null} bila baris <b>tidak terkunci</b>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dikunci")
	public Tbmuser getDikunci() {
		dikunci = check(dikunci);
		return dikunci;
	}

	/**
	 * Mengunci ({@code != null}) atau membuka kunci ({@code null}) baris jadwal.
	 *
	 * @param dikunci pengguna yang mengunci; {@code null} membuka kunci
	 */
	public void setDikunci(Tbmuser dikunci) {
		this.dikunci = dikunci;
	}

	/**
	 * Mengembalikan guru pengampu slot ke-2 (baris layar "Hari dan Jam Pelajaran II"); kolom
	 * {@code guru2_id}.
	 *
	 * <p><b>Getter destruktif — pola sama pada dua belas slot.</b> Setelah {@code check()},
	 * bila {@link #getHari2()} bernilai {@code null} (hari slot ini kosong atau hanya spasi),
	 * field {@code guru2} <b>ditulisi {@code null}</b>. Karena entity ini memakai
	 * <i>property access</i> dan {@code dynamicUpdate}, nilai {@code null} itu ikut tersimpan
	 * pada {@code flush} berikutnya, sehingga kolom {@code guru2_id} <b>terhapus permanen</b> tanpa
	 * tindakan pengguna. Lihat pembahasan lengkap pada {@link #getGuru()}.</p>
	 *
	 * @return guru pengampu slot ke-2; {@code null} bila slot tak bergurus <b>atau</b> bila
	 *         hari slot ini kosong
	 * @see #getGuru()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru2_id")
	public Guru getGuru2() {
		guru2 = check(guru2);
		if (getHari2() == null) {
			guru2 = null;
		}
		return guru2;
	}

	/**
	 * Menyetel guru pengampu slot ke-2. Nilai yang disetel akan <b>dibuang oleh getter-nya</b>
	 * bila hari slot ke-2 tidak ikut diisi — lihat {@link #getGuru2()}.
	 *
	 * @param guru2 guru pengampu; {@code null} mengosongkan slot
	 */
	public void setGuru2(Guru guru2) {
		this.guru2 = guru2;
	}

	/**
	 * Mengembalikan guru pengampu slot ke-3 (baris layar "Hari dan Jam Pelajaran III"); kolom
	 * {@code guru3_id}.
	 *
	 * <p><b>Getter destruktif — pola sama pada dua belas slot.</b> Setelah {@code check()},
	 * bila {@link #getHari3()} bernilai {@code null} (hari slot ini kosong atau hanya spasi),
	 * field {@code guru3} <b>ditulisi {@code null}</b>. Karena entity ini memakai
	 * <i>property access</i> dan {@code dynamicUpdate}, nilai {@code null} itu ikut tersimpan
	 * pada {@code flush} berikutnya, sehingga kolom {@code guru3_id} <b>terhapus permanen</b> tanpa
	 * tindakan pengguna. Lihat pembahasan lengkap pada {@link #getGuru()}.</p>
	 *
	 * @return guru pengampu slot ke-3; {@code null} bila slot tak bergurus <b>atau</b> bila
	 *         hari slot ini kosong
	 * @see #getGuru()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru3_id")
	public Guru getGuru3() {
		guru3 = check(guru3);
		if (getHari3() == null) {
			guru3 = null;
		}
		return guru3;
	}

	/**
	 * Menyetel guru pengampu slot ke-3. Nilai yang disetel akan <b>dibuang oleh getter-nya</b>
	 * bila hari slot ke-3 tidak ikut diisi — lihat {@link #getGuru3()}.
	 *
	 * @param guru3 guru pengampu; {@code null} mengosongkan slot
	 */
	public void setGuru3(Guru guru3) {
		this.guru3 = guru3;
	}

	/**
	 * Mengembalikan guru pengampu slot ke-4 (baris layar "Hari dan Jam Pelajaran IV"); kolom
	 * {@code guru4_id}.
	 *
	 * <p><b>Getter destruktif — pola sama pada dua belas slot.</b> Setelah {@code check()},
	 * bila {@link #getHari4()} bernilai {@code null} (hari slot ini kosong atau hanya spasi),
	 * field {@code guru4} <b>ditulisi {@code null}</b>. Karena entity ini memakai
	 * <i>property access</i> dan {@code dynamicUpdate}, nilai {@code null} itu ikut tersimpan
	 * pada {@code flush} berikutnya, sehingga kolom {@code guru4_id} <b>terhapus permanen</b> tanpa
	 * tindakan pengguna. Lihat pembahasan lengkap pada {@link #getGuru()}.</p>
	 *
	 * @return guru pengampu slot ke-4; {@code null} bila slot tak bergurus <b>atau</b> bila
	 *         hari slot ini kosong
	 * @see #getGuru()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru4_id")
	public Guru getGuru4() {
		guru4 = check(guru4);
		if (getHari4() == null) {
			guru4 = null;
		}
		return guru4;
	}

	/**
	 * Menyetel guru pengampu slot ke-4. Nilai yang disetel akan <b>dibuang oleh getter-nya</b>
	 * bila hari slot ke-4 tidak ikut diisi — lihat {@link #getGuru4()}.
	 *
	 * @param guru4 guru pengampu; {@code null} mengosongkan slot
	 */
	public void setGuru4(Guru guru4) {
		this.guru4 = guru4;
	}

	/**
	 * Mengembalikan guru pengampu slot ke-5 (baris layar "Hari dan Jam Pelajaran V"); kolom
	 * {@code guru5_id}.
	 *
	 * <p><b>Getter destruktif — pola sama pada dua belas slot.</b> Setelah {@code check()},
	 * bila {@link #getHari5()} bernilai {@code null} (hari slot ini kosong atau hanya spasi),
	 * field {@code guru5} <b>ditulisi {@code null}</b>. Karena entity ini memakai
	 * <i>property access</i> dan {@code dynamicUpdate}, nilai {@code null} itu ikut tersimpan
	 * pada {@code flush} berikutnya, sehingga kolom {@code guru5_id} <b>terhapus permanen</b> tanpa
	 * tindakan pengguna. Lihat pembahasan lengkap pada {@link #getGuru()}.</p>
	 *
	 * @return guru pengampu slot ke-5; {@code null} bila slot tak bergurus <b>atau</b> bila
	 *         hari slot ini kosong
	 * @see #getGuru()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru5_id")
	public Guru getGuru5() {
		guru5 = check(guru5);
		if (getHari5() == null) {
			guru5 = null;
		}
		return guru5;
	}

	/**
	 * Menyetel guru pengampu slot ke-5. Nilai yang disetel akan <b>dibuang oleh getter-nya</b>
	 * bila hari slot ke-5 tidak ikut diisi — lihat {@link #getGuru5()}.
	 *
	 * @param guru5 guru pengampu; {@code null} mengosongkan slot
	 */
	public void setGuru5(Guru guru5) {
		this.guru5 = guru5;
	}

	/**
	 * Apakah guru pengampu boleh menggeser tanggal pertemuan yang dibangkitkan dari jadwal ini.
	 *
	 * <p><b>Default permisif:</b> {@code null} dibaca sebagai {@code true}. Baris lama yang
	 * dibuat sebelum kolom ini ada, dan baris baru yang belum pernah disentuh administrator,
	 * semuanya mengizinkan guru mengubah tanggal.</p>
	 *
	 * @return {@code true} bila guru diizinkan menggeser tanggal
	 */
	public Boolean getGuruBisaMerubahTanggalJadwalPelajaran() {
		return guruBisaMerubahTanggalJadwalPelajaran == null ? true : guruBisaMerubahTanggalJadwalPelajaran;
	}

	/**
	 * Menyetel izin guru menggeser tanggal pertemuan.
	 *
	 * @param guruBisaMerubahTanggalJadwalPelajaran {@code false} untuk melarang; {@code null}
	 *        dibaca sebagai {@code true}
	 */
	public void setGuruBisaMerubahTanggalJadwalPelajaran(Boolean guruBisaMerubahTanggalJadwalPelajaran) {
		this.guruBisaMerubahTanggalJadwalPelajaran = guruBisaMerubahTanggalJadwalPelajaran;
	}

	/**
	 * Mengembalikan tanggal mulai berlakunya jadwal.
	 *
	 * <p><b>Getter yang menulis balik dan bersifat menimpa.</b> Bila
	 * {@link #getMasaJadwalPelajaran()} terisi (termasuk ketika masa itu <i>baru saja dipilihkan
	 * sendiri</i> oleh getter tersebut), field ini ditimpa dengan {@code masaJadwalPelajaran.getMulai()}.
	 * Tanggal mulai yang diketik administrator karena itu tidak bertahan begitu sebuah masa
	 * jadwal menempel pada baris.</p>
	 *
	 * @return tanggal mulai berlaku, atau {@code null} bila belum ditentukan
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalMulaiJadwalPelajaran() {
		if (getMasaJadwalPelajaran() != null) {
			tanggalMulaiJadwalPelajaran = getMasaJadwalPelajaran().getMulai();
		}
		return tanggalMulaiJadwalPelajaran;
	}

	/**
	 * Menyetel tanggal mulai berlakunya jadwal. Dapat <b>ditimpa kembali</b> oleh getter-nya —
	 * lihat {@link #getTanggalMulaiJadwalPelajaran()}.
	 *
	 * @param tanggalMulaiJadwalPelajaran tanggal mulai; boleh {@code null}
	 */
	public void setTanggalMulaiJadwalPelajaran(Date tanggalMulaiJadwalPelajaran) {
		this.tanggalMulaiJadwalPelajaran = tanggalMulaiJadwalPelajaran;
	}

	/**
	 * Apakah generator pertemuan melompati hari libur nasional saat menebar tanggal.
	 *
	 * <p><b>Default permisif:</b> {@code null} dibaca sebagai {@code true} — libur nasional
	 * dilewati kecuali administrator secara sadar mematikannya.</p>
	 *
	 * @return {@code true} bila tanggal merah nasional dilewati
	 */
	public Boolean getLewatiTanggalMerahNasional() {
		return lewatiTanggalMerahNasional == null ? true : lewatiTanggalMerahNasional;
	}

	/**
	 * Menyetel perlakuan terhadap hari libur nasional.
	 *
	 * @param lewatiTanggalMerahNasional {@code false} agar libur nasional tetap dijadwalkan;
	 *        {@code null} dibaca sebagai {@code true}
	 */
	public void setLewatiTanggalMerahNasional(Boolean lewatiTanggalMerahNasional) {
		this.lewatiTanggalMerahNasional = lewatiTanggalMerahNasional;
	}

	/**
	 * Ringkasan lengkap satu baris jadwal dalam satu baris teks berlabel, tanpa guru tambahan.
	 * Setara dengan {@code info(null)}.
	 *
	 * @return teks ringkasan; lihat {@link #info(Guru)} untuk susunannya
	 * @see #info(Guru)
	 * @see #infoSimple()
	 */
	public String info() {
		return info(null);
	}

	/**
	 * Ringkasan lengkap satu baris jadwal dalam satu baris teks <b>berlabel</b>, dengan opsi
	 * menyisipkan satu guru tambahan (dipakai untuk menampilkan guru pengganti).
	 *
	 * <p>Susunannya: {@code "Matapelajaran: …, Semester: … <kelas>, Guru : …, Ruang: …,
	 * Hari: … <jam> s.d <jam>, …, Tahun Ajaran: …, Sekolah: …"}. Nama kelas diambil dari kelas
	 * reguler, atau dari kelas les bila jadwal ini jadwal les. Ruang diambil dari
	 * {@link #getRuang()} (yang sendirinya turunan).</p>
	 *
	 * <p>Bagian hari/jam <b>menampilkan seluruh dua belas slot</b> yang harinya terisi, dan
	 * <b>seluruhnya dilewati</b> bila {@link #getMerupakan_tanpa_jadwal_perkuliahan()} bernilai
	 * {@code true}.</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@link #getSekolah()}, {@link #getMatapelajaran()},
	 * {@link #getTahunAjaran()}, {@link #getRuang()} dan {@link #populateGuru()} — seluruh
	 * penulisan balik yang dijelaskan pada Javadoc kelas ikut terpicu setiap kali ringkasan ini
	 * dibuat. Karena {@code info()} dipakai antara lain oleh pesan peringatan bentrok dan oleh
	 * {@code LinimasaApi}, efek itu menjangkau jalur baca yang tampaknya tidak berbahaya.</p>
	 *
	 * @param guruTambahan guru yang ditambahkan di akhir daftar pengampu (mis. guru
	 *                     pengganti); {@code null} bila tidak ada
	 * @return teks ringkasan berlabel
	 */
	public String info(Guru guruTambahan) {
		sekolah = getSekolah();
		String matkul1 = getMatapelajaran() == null ? "" : getMatapelajaran().getNama();

		Integer semester1 = getSemester() == null ? 0 : getSemester();

		String kelas1 = getKelas() != null ? getKelas().getNama()
				: getKelasLesSiswa() != null ? getKelasLesSiswa().getNama() : "";

		String guru1 = "";
		for (Guru guru : populateGuru().values()) {
			guru1 += guru1.isEmpty() ? guru.getNama() : ", " + guru.getNama();
		}

		if (guruTambahan != null) {
			guru1 += guru1.isEmpty() ? guruTambahan.getNama() : ", " + guruTambahan.getNama();
		}

		String ruang = getRuang() == null ? "" : getRuang().getNama();

		String harijam = "";

		if (!getMerupakan_tanpa_jadwal_perkuliahan()) {
			if (getHari() != null) {
				harijam += ("Hari: " + getHari() + ", "
						+ (getJamPelajaran() == null ? "" : getJamPelajaran().getMulaiS()) + " s.d "
						+ (getJamPelajaran() == null ? "" : getJamPelajaran().getSampaiS()));
			}
			if (getHari2() != null) {
				harijam += (", Hari: " + getHari2() + ", "
						+ (getJamPelajaran2() == null ? "" : getJamPelajaran2().getMulaiS()) + " s.d "
						+ (getJamPelajaran2() == null ? "" : getJamPelajaran2().getSampaiS()));
			}

			if (getHari3() != null) {
				harijam += (", Hari: " + getHari3() + ", "
						+ (getJamPelajaran3() == null ? "" : getJamPelajaran3().getMulaiS()) + " s.d "
						+ (getJamPelajaran3() == null ? "" : getJamPelajaran3().getSampaiS()));
			}

			if (getHari4() != null) {
				harijam += (", Hari: " + getHari4() + ", "
						+ (getJamPelajaran4() == null ? "" : getJamPelajaran4().getMulaiS()) + " s.d "
						+ (getJamPelajaran4() == null ? "" : getJamPelajaran4().getSampaiS()));
			}

			if (getHari5() != null) {
				harijam += (", Hari: " + getHari5() + ", "
						+ (getJamPelajaran5() == null ? "" : getJamPelajaran5().getMulaiS()) + " s.d "
						+ (getJamPelajaran5() == null ? "" : getJamPelajaran5().getSampaiS()));
			}

			if (getHari6() != null) {
				harijam += (", Hari: " + getHari6() + ", "
						+ (getJamPelajaran6() == null ? "" : getJamPelajaran6().getMulaiS()) + " s.d "
						+ (getJamPelajaran6() == null ? "" : getJamPelajaran6().getSampaiS()));
			}

			if (getHari7() != null) {
				harijam += (", Hari: " + getHari7() + ", "
						+ (getJamPelajaran7() == null ? "" : getJamPelajaran7().getMulaiS()) + " s.d "
						+ (getJamPelajaran7() == null ? "" : getJamPelajaran7().getSampaiS()));
			}

			if (getHari8() != null) {
				harijam += (", Hari: " + getHari8() + ", "
						+ (getJamPelajaran8() == null ? "" : getJamPelajaran8().getMulaiS()) + " s.d "
						+ (getJamPelajaran8() == null ? "" : getJamPelajaran8().getSampaiS()));
			}

			if (getHari9() != null) {
				harijam += (", Hari: " + getHari9() + ", "
						+ (getJamPelajaran9() == null ? "" : getJamPelajaran9().getMulaiS()) + " s.d "
						+ (getJamPelajaran9() == null ? "" : getJamPelajaran9().getSampaiS()));
			}

			if (getHari10() != null) {
				harijam += (", Hari: " + getHari10() + ", "
						+ (getJamPelajaran10() == null ? "" : getJamPelajaran10().getMulaiS()) + " s.d "
						+ (getJamPelajaran10() == null ? "" : getJamPelajaran10().getSampaiS()));
			}

			if (getHari11() != null) {
				harijam += (", Hari: " + getHari11() + ", "
						+ (getJamPelajaran11() == null ? "" : getJamPelajaran11().getMulaiS()) + " s.d "
						+ (getJamPelajaran11() == null ? "" : getJamPelajaran11().getSampaiS()));
			}

			if (getHari12() != null) {
				harijam += (", Hari: " + getHari12() + ", "
						+ (getJamPelajaran12() == null ? "" : getJamPelajaran12().getMulaiS()) + " s.d "
						+ (getJamPelajaran12() == null ? "" : getJamPelajaran12().getSampaiS()));
			}
		}

		String groupTxt = "Matapelajaran: " + matkul1 + ", Semester: " + semester1 + " " + kelas1
				+ (guru1.equals("") ? "" : ", Guru : " + guru1) + (ruang.equals("") ? "" : ", Ruang: " + ruang)
				+ harijam + ", Tahun Ajaran: " + getTahunAjaran() + ", Sekolah: "
				+ (sekolah == null ? "" : sekolah.getNama());
		return groupTxt;
	}

	/**
	 * Ringkasan satu baris jadwal <b>tanpa label</b> — versi ringkas {@link #info(Guru)} untuk
	 * tempat sempit (judul tab, label dasbor, item combobox).
	 *
	 * <p>Perbedaan nyata dari {@link #info(Guru)} selain hilangnya label: <b>ruang diambil dari
	 * kelas</b> ({@code kelas.getRuang()} atau {@code kelasLesSiswa.getRuang()}), bukan dari
	 * {@link #getRuang()}. Pada baris yang ruangnya di-<i>override</i> di tingkat jadwal, kedua
	 * ringkasan ini dapat menyebut ruang yang berbeda.</p>
	 *
	 * <p><b>Efek samping:</b> sama dengan {@link #info(Guru)}.</p>
	 *
	 * @return teks ringkasan ringkas tanpa label
	 */
	public String infoSimple() {
		sekolah = getSekolah();
		String matkul1 = getMatapelajaran() == null ? "" : getMatapelajaran().getNama();

		Integer semester1 = getSemester() == null ? 0 : getSemester();

		String kelas1 = getKelas() != null ? getKelas().getNama()
				: getKelasLesSiswa() != null ? getKelasLesSiswa().getNama() : "";

		String guru1 = "";
		for (Guru guru : populateGuru().values()) {
			guru1 += guru1.isEmpty() ? guru.getNama() : ", " + guru.getNama();
		}

		String ruang = getKelas() != null ? (getKelas().getRuang() == null ? "" : getKelas().getRuang().getNama())
				: getKelasLesSiswa() != null
						? (getKelasLesSiswa().getRuang() == null ? "" : getKelasLesSiswa().getRuang().getNama())
						: "";

		String harijam = "";

		if (!getMerupakan_tanpa_jadwal_perkuliahan()) {
			if (getHari() != null) {
				harijam += ("Hari: " + getHari() + ", "
						+ (getJamPelajaran() == null ? "" : getJamPelajaran().getMulaiS()) + " s.d "
						+ (getJamPelajaran() == null ? "" : getJamPelajaran().getSampaiS()));
			}
			if (getHari2() != null) {
				harijam += (", Hari: " + getHari2() + ", "
						+ (getJamPelajaran2() == null ? "" : getJamPelajaran2().getMulaiS()) + " s.d "
						+ (getJamPelajaran2() == null ? "" : getJamPelajaran2().getSampaiS()));
			}

			if (getHari3() != null) {
				harijam += (", Hari: " + getHari3() + ", "
						+ (getJamPelajaran3() == null ? "" : getJamPelajaran3().getMulaiS()) + " s.d "
						+ (getJamPelajaran3() == null ? "" : getJamPelajaran3().getSampaiS()));
			}

			if (getHari4() != null) {
				harijam += (", Hari: " + getHari4() + ", "
						+ (getJamPelajaran4() == null ? "" : getJamPelajaran4().getMulaiS()) + " s.d "
						+ (getJamPelajaran4() == null ? "" : getJamPelajaran4().getSampaiS()));
			}

			if (getHari5() != null) {
				harijam += (", Hari: " + getHari5() + ", "
						+ (getJamPelajaran5() == null ? "" : getJamPelajaran5().getMulaiS()) + " s.d "
						+ (getJamPelajaran5() == null ? "" : getJamPelajaran5().getSampaiS()));
			}

			if (getHari6() != null) {
				harijam += (", Hari: " + getHari6() + ", "
						+ (getJamPelajaran6() == null ? "" : getJamPelajaran6().getMulaiS()) + " s.d "
						+ (getJamPelajaran6() == null ? "" : getJamPelajaran6().getSampaiS()));
			}

			if (getHari7() != null) {
				harijam += (", Hari: " + getHari7() + ", "
						+ (getJamPelajaran7() == null ? "" : getJamPelajaran7().getMulaiS()) + " s.d "
						+ (getJamPelajaran7() == null ? "" : getJamPelajaran7().getSampaiS()));
			}

			if (getHari8() != null) {
				harijam += (", Hari: " + getHari8() + ", "
						+ (getJamPelajaran8() == null ? "" : getJamPelajaran8().getMulaiS()) + " s.d "
						+ (getJamPelajaran8() == null ? "" : getJamPelajaran8().getSampaiS()));
			}

			if (getHari9() != null) {
				harijam += (", Hari: " + getHari9() + ", "
						+ (getJamPelajaran9() == null ? "" : getJamPelajaran9().getMulaiS()) + " s.d "
						+ (getJamPelajaran9() == null ? "" : getJamPelajaran9().getSampaiS()));
			}

			if (getHari10() != null) {
				harijam += (", Hari: " + getHari10() + ", "
						+ (getJamPelajaran10() == null ? "" : getJamPelajaran10().getMulaiS()) + " s.d "
						+ (getJamPelajaran10() == null ? "" : getJamPelajaran10().getSampaiS()));
			}

			if (getHari11() != null) {
				harijam += (", Hari: " + getHari11() + ", "
						+ (getJamPelajaran11() == null ? "" : getJamPelajaran11().getMulaiS()) + " s.d "
						+ (getJamPelajaran11() == null ? "" : getJamPelajaran11().getSampaiS()));
			}

			if (getHari12() != null) {
				harijam += (", Hari: " + getHari12() + ", "
						+ (getJamPelajaran12() == null ? "" : getJamPelajaran12().getMulaiS()) + " s.d "
						+ (getJamPelajaran12() == null ? "" : getJamPelajaran12().getSampaiS()));
			}
		}

		String groupTxt = matkul1 + " " + semester1 + " " + kelas1 + (guru1.equals("") ? "" : " " + guru1)
				+ (ruang.equals("") ? "" : " " + ruang) + harijam + " " + getTahunAjaran() + " "
				+ (sekolah == null ? "" : sekolah.getNama());
		return groupTxt;
	}

	/**
	 * Apakah baris ini adalah pembelajaran tanpa jadwal hari/jam tetap.
	 *
	 * <p>Bila {@code true}, {@link #info(Guru)} dan {@link #infoSimple()} menghilangkan seluruh
	 * bagian hari/jam. Default {@code false} bila {@code null} — berbeda arah dengan saklar
	 * {@link #getGuruBisaMerubahTanggalJadwalPelajaran()} dan
	 * {@link #getLewatiTanggalMerahNasional()} yang default {@code true}.</p>
	 *
	 * @return {@code true} bila pembelajaran tidak berjadwal tetap
	 */
	public Boolean getMerupakan_tanpa_jadwal_perkuliahan() {
		return merupakan_tanpa_jadwal_perkuliahan == null ? false : merupakan_tanpa_jadwal_perkuliahan;
	}

	/**
	 * Menyetel penanda pembelajaran tanpa jadwal tetap.
	 *
	 * @param merupakan_tanpa_jadwal_perkuliahan {@code true} untuk menyembunyikan bagian
	 *        hari/jam dari seluruh ringkasan; {@code null} dibaca sebagai {@code false}
	 */
	public void setMerupakan_tanpa_jadwal_perkuliahan(Boolean merupakan_tanpa_jadwal_perkuliahan) {
		this.merupakan_tanpa_jadwal_perkuliahan = merupakan_tanpa_jadwal_perkuliahan;
	}

	/**
	 * Mengembalikan deskripsi pembelajaran untuk berkas RPP/RPS.
	 *
	 * @return teks sudah di-{@code trim()}, atau string kosong bila belum diisi (tidak pernah
	 *         {@code null})
	 */
	@Column(columnDefinition = "text")
	public String getDeskripsiPembelajaran() {
		return deskripsiPembelajaran == null ? "" : deskripsiPembelajaran.trim();
	}

	/**
	 * Menyetel deskripsi pembelajaran.
	 *
	 * @param deskripsiPembelajaran teks panjang; boleh {@code null}
	 */
	public void setDeskripsiPembelajaran(String deskripsiPembelajaran) {
		this.deskripsiPembelajaran = deskripsiPembelajaran;
	}

	/**
	 * Mengembalikan capaian/kompetensi pembelajaran untuk berkas RPP/RPS.
	 *
	 * @return teks sudah di-{@code trim()}, atau string kosong bila belum diisi (tidak pernah
	 *         {@code null})
	 */
	@Column(columnDefinition = "text")
	public String getCapaianPembelajaranProdi() {
		return capaianPembelajaranProdi == null ? "" : capaianPembelajaranProdi.trim();
	}

	/**
	 * Menyetel capaian/kompetensi pembelajaran.
	 *
	 * @param capaianPembelajaranProdi teks panjang; boleh {@code null}
	 */
	public void setCapaianPembelajaranProdi(String capaianPembelajaranProdi) {
		this.capaianPembelajaranProdi = capaianPembelajaranProdi;
	}

	/**
	 * Apakah presensi guru hanya boleh diinput pada rentang hari/jam sesuai slot jadwal.
	 *
	 * @return {@code true} bila presensi dikunci ke jadwal; default {@code false} bila
	 *         {@code null}
	 */
	public Boolean getKehadiranGuruHarusDiinputSesuaiJadwal() {
		return kehadiranGuruHarusDiinputSesuaiJadwal == null ? false : kehadiranGuruHarusDiinputSesuaiJadwal;
	}

	/**
	 * Menyetel kebijakan penguncian presensi guru terhadap jadwal.
	 *
	 * @param kehadiranGuruHarusDiinputSesuaiJadwal {@code true} untuk mengunci; {@code null}
	 *        dibaca sebagai {@code false}
	 */
	public void setKehadiranGuruHarusDiinputSesuaiJadwal(Boolean kehadiranGuruHarusDiinputSesuaiJadwal) {
		this.kehadiranGuruHarusDiinputSesuaiJadwal = kehadiranGuruHarusDiinputSesuaiJadwal;
	}

	/**
	 * Mengembalikan jam mulai sebagai teks.
	 *
	 * <p><b>Getter yang menulis balik dan bersifat menimpa.</b> Bila slot jam ke-1 terisi, nilai
	 * selalu ditimpa dengan {@code jamPelajaran.getMulaiS()}. Bila setelah itu masih
	 * {@code null}, diisi <b>jam saat ini</b> ({@code Common.timeFormat} atas
	 * {@code WaktuUtil.getDate()}) — sehingga baris tanpa slot jam akan menyimpan jam
	 * sembarang, yaitu jam ketika baris itu kebetulan dibaca.</p>
	 *
	 * <p>Perhatikan pula bahwa hanya <b>slot ke-1</b> yang dipertimbangkan; sebelas slot lainnya
	 * tidak berpengaruh pada kolom ini.</p>
	 *
	 * @return teks jam mulai; tidak pernah {@code null} setelah pemanggilan ini
	 */
	public String getWaktuMulai() {
		if (getJamPelajaran() != null) {
			waktuMulai = getJamPelajaran().getMulaiS();
		}

		if (waktuMulai == null) {
			waktuMulai = Common.timeFormat.get().format(WaktuUtil.getDate());
		}

		return waktuMulai;
	}

	/**
	 * Menyetel teks jam mulai. Nilai ini akan ditimpa oleh {@link #getWaktuMulai()} bila slot
	 * jam ke-1 terisi.
	 *
	 * @param waktuMulai teks jam mulai; boleh {@code null}
	 */
	public void setWaktuMulai(String waktuMulai) {
		this.waktuMulai = waktuMulai;
	}

	/**
	 * Mengembalikan jam selesai sebagai teks.
	 *
	 * <p><b>Getter yang menulis balik:</b> bila slot jam ke-1 terisi, nilai ditimpa dengan
	 * {@code jamPelajaran.getSampaiS()}. Berbeda dari {@link #getWaktuMulai()}, tidak ada
	 * cadangan "jam sekarang" — hasilnya boleh {@code null}. Sama seperti pasangannya, hanya
	 * slot ke-1 yang dipertimbangkan.</p>
	 *
	 * @return teks jam selesai, atau {@code null} bila slot ke-1 kosong dan field belum diisi
	 */
	public String getWaktuSelesai() {
		if (getJamPelajaran() != null) {
			waktuSelesai = getJamPelajaran().getSampaiS();
		}
		return waktuSelesai;
	}

	/**
	 * Menyetel teks jam selesai. Nilai ini akan ditimpa oleh {@link #getWaktuSelesai()} bila
	 * slot jam ke-1 terisi.
	 *
	 * @param waktuSelesai teks jam selesai; boleh {@code null}
	 */
	public void setWaktuSelesai(String waktuSelesai) {
		this.waktuSelesai = waktuSelesai;
	}

	/**
	 * Mengembalikan ruang tempat pembelajaran berlangsung.
	 *
	 * <p><b>Getter yang menulis balik dan bersifat menimpa.</b> Setelah {@code check()}, field
	 * ditimpa oleh ruang kelas les (bila ada) atau ruang kelas reguler (bila ada). Ruang yang
	 * disetel khusus pada baris jadwal karena itu <b>tidak bertahan</b> selama kelasnya sendiri
	 * punya ruang: pada pembacaan berikutnya kolom {@code ruang} kembali menjadi ruang kelas.</p>
	 *
	 * <p>Ruang adalah salah satu dari tiga sumbu deteksi bentrok
	 * ({@code checkBentrokBerdasarRuangan}); baris dengan {@code ruang == null} dilewati
	 * sepenuhnya oleh pemeriksa itu.</p>
	 *
	 * @return ruang efektif, atau {@code null} bila jadwal maupun kelasnya tidak berruang
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ruang", nullable = true)
	public Ruang getRuang() {
		ruang = check(ruang);
		if (getKelasLesSiswa() != null && getKelasLesSiswa().getRuang() != null) {
			ruang = getKelasLesSiswa().getRuang();
		} else if (getKelas() != null && getKelas().getRuang() != null) {
			ruang = getKelas().getRuang();
		}
		return this.ruang;
	}

	/**
	 * Menyetel ruang pembelajaran. Nilai ini dapat <b>ditimpa kembali</b> oleh
	 * {@link #getRuang()} bila kelas/kelas les punya ruang sendiri.
	 *
	 * @param ruang ruang; {@code null} mengosongkan
	 */
	public void setRuang(Ruang ruang) {
		this.ruang = ruang;
	}

	/**
	 * Mengembalikan nama subjek jadwal — nama kelas les bila ada, jika tidak nama kelas
	 * reguler.
	 *
	 * <p>Dipakai antara lain oleh {@code DashboardRekapPertemuanJadwalPelajaran} dan
	 * {@code PengumumanAkademisAction} untuk menyebut "kelas" sebuah pertemuan tanpa perlu tahu
	 * jalur mana yang dipakai baris jadwal.</p>
	 *
	 * @return nama kelas les / kelas reguler; string kosong bila baris jadwal tidak terikat
	 *         keduanya (tidak pernah {@code null})
	 */
	public String ambilNama() {
		String nama = "";
		if (getKelasLesSiswa() != null) {
			nama = getKelasLesSiswa().getNama();
		} else if (getKelas() != null) {
			nama = getKelas().getNama();
		}
		return nama;
	}

	/**
	 * Mengembalikan nama hari slot ke-2 (baris layar "Hari dan Jam Pelajaran II"); kolom
	 * {@code hari2}.
	 *
	 * <p>Normalisasi seragam dengan slot ke-1: string kosong atau hanya spasi dikembalikan
	 * sebagai {@code null}, hasilnya selalu di-{@code trim()}. Nilai {@code null} di sini
	 * <b>menghapus</b> guru slot ini lewat {@link #getGuru2()}.</p>
	 *
	 * @return nama hari slot ke-2, atau {@code null} bila slot tidak dijadwalkan
	 * @see #getHari()
	 */
	public String getHari2() {
		return this.hari2 == null || hari2.trim().isEmpty() ? null : hari2.trim();
	}

	/**
	 * Menyetel nama hari slot ke-2. Nilai disimpan apa adanya; normalisasi dilakukan getter.
	 *
	 * @param hari2 salah satu nilai {@code Common.haris}; {@code null} berarti slot ke-2 tidak
	 *        dipakai — dan ikut menghapus {@code guru2} pada pembacaan berikutnya
	 */
	public void setHari2(String hari2) {
		this.hari2 = hari2;
	}

	/**
	 * Mengembalikan nama hari slot ke-3 (baris layar "Hari dan Jam Pelajaran III"); kolom
	 * {@code hari3}.
	 *
	 * <p>Normalisasi seragam dengan slot ke-1: string kosong atau hanya spasi dikembalikan
	 * sebagai {@code null}, hasilnya selalu di-{@code trim()}. Nilai {@code null} di sini
	 * <b>menghapus</b> guru slot ini lewat {@link #getGuru3()}.</p>
	 *
	 * @return nama hari slot ke-3, atau {@code null} bila slot tidak dijadwalkan
	 * @see #getHari()
	 */
	public String getHari3() {
		return this.hari3 == null || hari3.trim().isEmpty() ? null : hari3.trim();
	}

	/**
	 * Menyetel nama hari slot ke-3. Nilai disimpan apa adanya; normalisasi dilakukan getter.
	 *
	 * @param hari3 salah satu nilai {@code Common.haris}; {@code null} berarti slot ke-3 tidak
	 *        dipakai — dan ikut menghapus {@code guru3} pada pembacaan berikutnya
	 */
	public void setHari3(String hari3) {
		this.hari3 = hari3;
	}

	/**
	 * Mengembalikan nama hari slot ke-4 (baris layar "Hari dan Jam Pelajaran IV"); kolom
	 * {@code hari4}.
	 *
	 * <p>Normalisasi seragam dengan slot ke-1: string kosong atau hanya spasi dikembalikan
	 * sebagai {@code null}, hasilnya selalu di-{@code trim()}. Nilai {@code null} di sini
	 * <b>menghapus</b> guru slot ini lewat {@link #getGuru4()}.</p>
	 *
	 * @return nama hari slot ke-4, atau {@code null} bila slot tidak dijadwalkan
	 * @see #getHari()
	 */
	public String getHari4() {
		return this.hari4 == null || hari4.trim().isEmpty() ? null : hari4.trim();
	}

	/**
	 * Menyetel nama hari slot ke-4. Nilai disimpan apa adanya; normalisasi dilakukan getter.
	 *
	 * @param hari4 salah satu nilai {@code Common.haris}; {@code null} berarti slot ke-4 tidak
	 *        dipakai — dan ikut menghapus {@code guru4} pada pembacaan berikutnya
	 */
	public void setHari4(String hari4) {
		this.hari4 = hari4;
	}

	/**
	 * Mengembalikan nama hari slot ke-5 (baris layar "Hari dan Jam Pelajaran V"); kolom
	 * {@code hari5}.
	 *
	 * <p>Normalisasi seragam dengan slot ke-1: string kosong atau hanya spasi dikembalikan
	 * sebagai {@code null}, hasilnya selalu di-{@code trim()}. Nilai {@code null} di sini
	 * <b>menghapus</b> guru slot ini lewat {@link #getGuru5()}.</p>
	 *
	 * @return nama hari slot ke-5, atau {@code null} bila slot tidak dijadwalkan
	 * @see #getHari()
	 */
	public String getHari5() {
		return this.hari5 == null || hari5.trim().isEmpty() ? null : hari5.trim();
	}

	/**
	 * Menyetel nama hari slot ke-5. Nilai disimpan apa adanya; normalisasi dilakukan getter.
	 *
	 * @param hari5 salah satu nilai {@code Common.haris}; {@code null} berarti slot ke-5 tidak
	 *        dipakai — dan ikut menghapus {@code guru5} pada pembacaan berikutnya
	 */
	public void setHari5(String hari5) {
		this.hari5 = hari5;
	}

	/**
	 * Mengembalikan slot jam ke-2 (baris layar "Hari dan Jam Pelajaran II"); kolom
	 * {@code jam_pelajaran2_id}.
	 *
	 * <p>Pola seragam dengan sebelas saudaranya: memanggil {@code check()} untuk menuntaskan
	 * proxy lazy dan menuliskan instance kanonik kembali ke field. Tidak ada logika turunan
	 * lain.</p>
	 *
	 * @return jam pelajaran slot ke-2, atau {@code null} bila slot kosong
	 * @see #getJamPelajaran()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran2_id")
	public JamPelajaran getJamPelajaran2() {
		jamPelajaran2 = check(jamPelajaran2);
		return jamPelajaran2;
	}

	/**
	 * Menyetel slot jam ke-2.
	 *
	 * @param jamPelajaran2 jam pelajaran yang dipilih; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran2(JamPelajaran jamPelajaran2) {
		this.jamPelajaran2 = jamPelajaran2;
	}

	/**
	 * Mengembalikan slot jam ke-3 (baris layar "Hari dan Jam Pelajaran III"); kolom
	 * {@code jam_pelajaran3_id}.
	 *
	 * <p>Pola seragam dengan sebelas saudaranya: memanggil {@code check()} untuk menuntaskan
	 * proxy lazy dan menuliskan instance kanonik kembali ke field. Tidak ada logika turunan
	 * lain.</p>
	 *
	 * @return jam pelajaran slot ke-3, atau {@code null} bila slot kosong
	 * @see #getJamPelajaran()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran3_id")
	public JamPelajaran getJamPelajaran3() {
		jamPelajaran3 = check(jamPelajaran3);
		return jamPelajaran3;
	}

	/**
	 * Menyetel slot jam ke-3.
	 *
	 * @param jamPelajaran3 jam pelajaran yang dipilih; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran3(JamPelajaran jamPelajaran3) {
		this.jamPelajaran3 = jamPelajaran3;
	}

	/**
	 * Mengembalikan slot jam ke-5 (baris layar "Hari dan Jam Pelajaran V"); kolom
	 * {@code jam_pelajaran5_id}.
	 *
	 * <p>Pola seragam dengan sebelas saudaranya: memanggil {@code check()} untuk menuntaskan
	 * proxy lazy dan menuliskan instance kanonik kembali ke field. Tidak ada logika turunan
	 * lain.</p>
	 *
	 * @return jam pelajaran slot ke-5, atau {@code null} bila slot kosong
	 * @see #getJamPelajaran()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran5_id")
	public JamPelajaran getJamPelajaran5() {
		jamPelajaran5 = check(jamPelajaran5);
		return jamPelajaran5;
	}

	/**
	 * Menyetel slot jam ke-5.
	 *
	 * @param jamPelajaran5 jam pelajaran yang dipilih; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran5(JamPelajaran jamPelajaran5) {
		this.jamPelajaran5 = jamPelajaran5;
	}

	/**
	 * Mengembalikan slot jam ke-4 (baris layar "Hari dan Jam Pelajaran IV"); kolom
	 * {@code jam_pelajaran4_id}.
	 *
	 * <p>Pola seragam dengan sebelas saudaranya: memanggil {@code check()} untuk menuntaskan
	 * proxy lazy dan menuliskan instance kanonik kembali ke field. Tidak ada logika turunan
	 * lain.</p>
	 *
	 * @return jam pelajaran slot ke-4, atau {@code null} bila slot kosong
	 * @see #getJamPelajaran()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran4_id")
	public JamPelajaran getJamPelajaran4() {
		jamPelajaran4 = check(jamPelajaran4);
		return jamPelajaran4;
	}

	/**
	 * Menyetel slot jam ke-4.
	 *
	 * @param jamPelajaran4 jam pelajaran yang dipilih; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran4(JamPelajaran jamPelajaran4) {
		this.jamPelajaran4 = jamPelajaran4;
	}

	/**
	 * Mengembalikan masa berlaku jadwal.
	 *
	 * <p><b>Getter yang menulis balik dan melakukan pengisian otomatis.</b> Dua cabang:</p>
	 * <ol>
	 * <li>bila kelas les punya masa jadwal → dipakai dan ditulis ke field;</li>
	 * <li>bila tidak, dan field masih {@code null} sementara {@code tahunAjaran} serta
	 *     {@code semester} sudah terisi → getter <b>menelusuri seluruh</b>
	 *     {@link MasaJadwalPelajaran} dari cache {@code ConstantValues} dan memilih yang
	 *     berpenanda {@code defaultData} dengan tahun ajaran dan semester yang cocok, lalu
	 *     menuliskannya ke field.</li>
	 * </ol>
	 *
	 * <p><b>Konsekuensi:</b> baris jadwal dapat memperoleh FK {@code masa_jadwal_pelajaran}
	 * yang tidak pernah dipilih siapa pun, semata-mata karena kebetulan dibaca. Dan karena
	 * {@link #getTanggalMulaiJadwalPelajaran()} kemudian menimpa tanggal mulai dari masa
	 * tersebut, satu pembacaan dapat mengubah dua kolom sekaligus.</p>
	 *
	 * <p><b>Kuirk:</b> cabang kedua membaca field mentah {@code tahunAjaran}/{@code semester},
	 * bukan getter-nya. Jadi pengisian default dari {@link #getSemester()} dan
	 * {@link #getTahunAjaran()} tidak berlaku di sini — baris yang kedua kolom itu masih
	 * {@code null} tidak pernah mendapat masa jadwal otomatis. Eksepsi apa pun di dalam
	 * penelusuran ditelan (hanya dicatat ke {@code ErrorAuditUtil}) sehingga kegagalan pencarian
	 * tidak terlihat pengguna.</p>
	 *
	 * @return masa jadwal efektif, atau {@code null} bila tak ada yang cocok
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "masa_jadwal_pelajaran")
	public MasaJadwalPelajaran getMasaJadwalPelajaran() {

		if (getKelasLesSiswa() != null && getKelasLesSiswa().getMasaJadwalPelajaran() != null) {
			masaJadwalPelajaran = getKelasLesSiswa().getMasaJadwalPelajaran();
		} else {
			masaJadwalPelajaran = check(masaJadwalPelajaran);
			if (masaJadwalPelajaran == null && tahunAjaran != null && semester != null) {

				try {

					for (Object o : ConstantValues.ambilBerdasarClass(MasaJadwalPelajaran.class).values()) {
						MasaJadwalPelajaran m = (MasaJadwalPelajaran) o;
						if (m.getDefaultData() && tahunAjaran.equals(m.getTahunAjaran())
								&& semester.equals(m.getSemester())) {
							masaJadwalPelajaran = m;
							break;
						}
					}

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/JadwalPelajaran.java:1169");
				}

			}
		}
		return masaJadwalPelajaran;
	}

	/**
	 * Menyetel masa berlaku jadwal.
	 *
	 * @param masaJadwalPelajaran masa jadwal; {@code null} mengosongkan dan membuka peluang
	 *        pengisian otomatis oleh {@link #getMasaJadwalPelajaran()}
	 */
	public void setMasaJadwalPelajaran(MasaJadwalPelajaran masaJadwalPelajaran) {
		this.masaJadwalPelajaran = masaJadwalPelajaran;
	}

	/**
	 * Mengembalikan nama program penyelenggaraan.
	 *
	 * @return nama program sudah di-{@code trim()}; {@code "Reguler"} bila kosong (tidak pernah
	 *         {@code null})
	 */
	public String getProgram() {
		return program == null || program.trim().isEmpty() ? "Reguler" : program.trim();
	}

	/**
	 * Menyetel nama program penyelenggaraan.
	 *
	 * @param program nama program; {@code null}/kosong dibaca sebagai {@code "Reguler"}
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * Mengembalikan jenis pengulangan jadwal.
	 *
	 * <p>Bukan relasi ke {@link JenisJadwalPelajaran} — lihat catatan pada {@link #jenis}.</p>
	 *
	 * @return jenis pengulangan; {@code "Mingguan"} bila {@code null}
	 */
	public String getJenis() {
		return jenis == null ? "Mingguan" : jenis;
	}

	/**
	 * Menyetel jenis pengulangan jadwal.
	 *
	 * <p><b>Kuirk:</b> berbeda dari {@link #getProgram()}, di sini hanya {@code null} yang
	 * diganti default — string kosong disimpan dan dikembalikan apa adanya sebagai string
	 * kosong.</p>
	 *
	 * @param jenis jenis pengulangan (mis. {@code "Mingguan"}); {@code null} dibaca sebagai
	 *              {@code "Mingguan"}
	 */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/**
	 * Implementasi kontrak {@link ais.database.model.VOPembelajaran}: jumlah "detail
	 * perkuliahan" yang menempel langsung pada objek pembelajaran ini.
	 *
	 * <p><b>Selalu mengembalikan 1</b> — satu baris {@code JadwalPelajaran} memang mewakili
	 * tepat satu penugasan pembelajaran, tidak seperti {@code Perkuliahan} pada modul
	 * perguruan tinggi yang dapat memayungi banyak kelas paralel. Komentar
	 * {@code // TODO Auto-generated method stub} bawaan generator dibiarkan utuh.</p>
	 *
	 * @return selalu {@code 1}
	 */
	@Override
	public Integer ambilJumlahDetailperkuliahanLangsung() {
		// TODO Auto-generated method stub
		return 1;
	}

	/**
	 * Muatan e-learning baris jadwal dalam bentuk JSON (kolom {@code text}), implementasi
	 * kontrak abstrak {@link ais.database.model.VOPembelajaran#getCourse()}. Bila kosong
	 * dibaca sebagai objek JSON kosong {@code "{}"} — lihat {@link #getCourse()}.
	 */
	private String course;
	/**
	 * Saklar pengurutan otomatis pertemuan, implementasi kontrak abstrak
	 * {@link ais.database.model.VOPembelajaran#getUrutkanotomatis()}. <b>Default
	 * {@code true}</b> bila {@code null}.
	 */
	private Boolean urutkanotomatis;
	/**
	 * Saklar aktif/nonaktif baris jadwal. <b>Default {@code true}</b> bila {@code null},
	 * sehingga baris lama yang dibuat sebelum kolom ini ada tetap dianggap aktif.
	 */
	private Boolean aktif;

	/**
	 * Mengembalikan muatan e-learning baris jadwal sebagai teks JSON.
	 *
	 * @return JSON muatan e-learning; objek JSON kosong {@code "{}"} bila belum pernah diisi
	 *         (tidak pernah {@code null})
	 */
	@Override
	@Column(columnDefinition = "text")
	public String getCourse() {
		// TODO Auto-generated method stub
		return course == null || course.trim().isEmpty() ? new JSONObject().toString() : course;
	}

	/**
	 * Menyetel muatan e-learning baris jadwal.
	 *
	 * @param course teks JSON; {@code null}/kosong dibaca sebagai objek JSON kosong
	 */
	@Override
	public void setCourse(String course) {
		this.course = course;
	}

	/**
	 * Mengembalikan baris kurikulum yang menurunkan jadwal ini.
	 *
	 * <p>Hanya menuntaskan proxy lazy; tidak menimpa apa pun. Namun nilainya menjadi
	 * <b>sumber</b> bagi {@link #getMatapelajaran()} dan {@link #getSekolah()}, sehingga
	 * mengubah baris kurikulum berdampak pada kedua kolom itu di seluruh jadwal turunannya.</p>
	 *
	 * @return baris kurikulum–mata pelajaran, atau {@code null} bila jadwal tidak dibuat dari
	 *         kurikulum
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kurikulum_punya_matapelajaran")
	public KurikulumPunyaMatapelajaran getKurikulumPunyaMatapelajaran() {
		kurikulumPunyaMatapelajaran = check(kurikulumPunyaMatapelajaran);
		return kurikulumPunyaMatapelajaran;
	}

	/**
	 * Menyetel baris kurikulum sumber jadwal.
	 *
	 * @param kurikulumPunyaMatapelajaran baris kurikulum–mata pelajaran; {@code null}
	 *        mengosongkan
	 */
	public void setKurikulumPunyaMatapelajaran(KurikulumPunyaMatapelajaran kurikulumPunyaMatapelajaran) {
		this.kurikulumPunyaMatapelajaran = kurikulumPunyaMatapelajaran;
	}

	/**
	 * Mengembalikan slot jam ke-6 (baris layar "Hari dan Jam Pelajaran VI"); kolom
	 * {@code jam_pelajaran6_id}.
	 *
	 * <p>Pola seragam dengan sebelas saudaranya: memanggil {@code check()} untuk menuntaskan
	 * proxy lazy dan menuliskan instance kanonik kembali ke field. Tidak ada logika turunan
	 * lain.</p>
	 *
	 * @return jam pelajaran slot ke-6, atau {@code null} bila slot kosong
	 * @see #getJamPelajaran()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran6_id")
	public JamPelajaran getJamPelajaran6() {
		jamPelajaran6 = check(jamPelajaran6);
		return jamPelajaran6;
	}

	/**
	 * Menyetel slot jam ke-6.
	 *
	 * @param jamPelajaran6 jam pelajaran yang dipilih; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran6(JamPelajaran jamPelajaran6) {
		this.jamPelajaran6 = jamPelajaran6;
	}

	/**
	 * Mengembalikan guru pengampu slot ke-6 (baris layar "Hari dan Jam Pelajaran VI"); kolom
	 * {@code guru6_id}.
	 *
	 * <p><b>Getter destruktif — pola sama pada dua belas slot.</b> Setelah {@code check()},
	 * bila {@link #getHari6()} bernilai {@code null} (hari slot ini kosong atau hanya spasi),
	 * field {@code guru6} <b>ditulisi {@code null}</b>. Karena entity ini memakai
	 * <i>property access</i> dan {@code dynamicUpdate}, nilai {@code null} itu ikut tersimpan
	 * pada {@code flush} berikutnya, sehingga kolom {@code guru6_id} <b>terhapus permanen</b> tanpa
	 * tindakan pengguna. Lihat pembahasan lengkap pada {@link #getGuru()}.</p>
	 *
	 * @return guru pengampu slot ke-6; {@code null} bila slot tak bergurus <b>atau</b> bila
	 *         hari slot ini kosong
	 * @see #getGuru()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru6_id")
	public Guru getGuru6() {
		guru6 = check(guru6);
		if (getHari6() == null) {
			guru6 = null;
		}
		return guru6;
	}

	/**
	 * Menyetel guru pengampu slot ke-6. Nilai yang disetel akan <b>dibuang oleh getter-nya</b>
	 * bila hari slot ke-6 tidak ikut diisi — lihat {@link #getGuru6()}.
	 *
	 * @param guru6 guru pengampu; {@code null} mengosongkan slot
	 */
	public void setGuru6(Guru guru6) {
		this.guru6 = guru6;
	}

	/**
	 * Mengembalikan nama hari slot ke-6 (baris layar "Hari dan Jam Pelajaran VI"); kolom
	 * {@code hari6}.
	 *
	 * <p>Normalisasi seragam dengan slot ke-1: string kosong atau hanya spasi dikembalikan
	 * sebagai {@code null}, hasilnya selalu di-{@code trim()}. Nilai {@code null} di sini
	 * <b>menghapus</b> guru slot ini lewat {@link #getGuru6()}.</p>
	 *
	 * @return nama hari slot ke-6, atau {@code null} bila slot tidak dijadwalkan
	 * @see #getHari()
	 */
	public String getHari6() {
		return this.hari6 == null || hari6.trim().isEmpty() ? null : hari6.trim();
	}

	/**
	 * Menyetel nama hari slot ke-6. Nilai disimpan apa adanya; normalisasi dilakukan getter.
	 *
	 * @param hari6 salah satu nilai {@code Common.haris}; {@code null} berarti slot ke-6 tidak
	 *        dipakai — dan ikut menghapus {@code guru6} pada pembacaan berikutnya
	 */
	public void setHari6(String hari6) {
		this.hari6 = hari6;
	}

	/**
	 * Mengembalikan slot jam ke-7 (baris layar "Hari dan Jam Pelajaran VII"); kolom
	 * {@code jam_pelajaran7_id}.
	 *
	 * <p>Pola seragam dengan sebelas saudaranya: memanggil {@code check()} untuk menuntaskan
	 * proxy lazy dan menuliskan instance kanonik kembali ke field. Tidak ada logika turunan
	 * lain.</p>
	 *
	 * @return jam pelajaran slot ke-7, atau {@code null} bila slot kosong
	 * @see #getJamPelajaran()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran7_id")
	public JamPelajaran getJamPelajaran7() {
		jamPelajaran7 = check(jamPelajaran7);
		return jamPelajaran7;
	}

	/**
	 * Menyetel slot jam ke-7.
	 *
	 * @param jamPelajaran7 jam pelajaran yang dipilih; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran7(JamPelajaran jamPelajaran7) {
		this.jamPelajaran7 = jamPelajaran7;
	}

	/**
	 * Mengembalikan slot jam ke-8 (baris layar "Hari dan Jam Pelajaran VIII"); kolom
	 * {@code jam_pelajaran8_id}.
	 *
	 * <p>Pola seragam dengan sebelas saudaranya: memanggil {@code check()} untuk menuntaskan
	 * proxy lazy dan menuliskan instance kanonik kembali ke field. Tidak ada logika turunan
	 * lain.</p>
	 *
	 * @return jam pelajaran slot ke-8, atau {@code null} bila slot kosong
	 * @see #getJamPelajaran()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran8_id")
	public JamPelajaran getJamPelajaran8() {
		jamPelajaran8 = check(jamPelajaran8);
		return jamPelajaran8;
	}

	/**
	 * Menyetel slot jam ke-8.
	 *
	 * @param jamPelajaran8 jam pelajaran yang dipilih; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran8(JamPelajaran jamPelajaran8) {
		this.jamPelajaran8 = jamPelajaran8;
	}

	/**
	 * Mengembalikan slot jam ke-9 (baris layar "Hari dan Jam Pelajaran IX"); kolom
	 * {@code jam_pelajaran9_id}.
	 *
	 * <p>Pola seragam dengan sebelas saudaranya: memanggil {@code check()} untuk menuntaskan
	 * proxy lazy dan menuliskan instance kanonik kembali ke field. Tidak ada logika turunan
	 * lain.</p>
	 *
	 * @return jam pelajaran slot ke-9, atau {@code null} bila slot kosong
	 * @see #getJamPelajaran()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran9_id")
	public JamPelajaran getJamPelajaran9() {
		jamPelajaran9 = check(jamPelajaran9);
		return jamPelajaran9;
	}

	/**
	 * Menyetel slot jam ke-9.
	 *
	 * @param jamPelajaran9 jam pelajaran yang dipilih; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran9(JamPelajaran jamPelajaran9) {
		this.jamPelajaran9 = jamPelajaran9;
	}

	/**
	 * Mengembalikan slot jam ke-10 (baris layar "Hari dan Jam Pelajaran X"); kolom
	 * {@code jam_pelajaran10_id}.
	 *
	 * <p>Pola seragam dengan sebelas saudaranya: memanggil {@code check()} untuk menuntaskan
	 * proxy lazy dan menuliskan instance kanonik kembali ke field. Tidak ada logika turunan
	 * lain.</p>
	 *
	 * @return jam pelajaran slot ke-10, atau {@code null} bila slot kosong
	 * @see #getJamPelajaran()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran10_id")
	public JamPelajaran getJamPelajaran10() {
		jamPelajaran10 = check(jamPelajaran10);
		return jamPelajaran10;
	}

	/**
	 * Menyetel slot jam ke-10.
	 *
	 * @param jamPelajaran10 jam pelajaran yang dipilih; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran10(JamPelajaran jamPelajaran10) {
		this.jamPelajaran10 = jamPelajaran10;
	}

	/**
	 * Mengembalikan guru pengampu slot ke-7 (baris layar "Hari dan Jam Pelajaran VII"); kolom
	 * {@code guru7_id}.
	 *
	 * <p><b>Getter destruktif — pola sama pada dua belas slot.</b> Setelah {@code check()},
	 * bila {@link #getHari7()} bernilai {@code null} (hari slot ini kosong atau hanya spasi),
	 * field {@code guru7} <b>ditulisi {@code null}</b>. Karena entity ini memakai
	 * <i>property access</i> dan {@code dynamicUpdate}, nilai {@code null} itu ikut tersimpan
	 * pada {@code flush} berikutnya, sehingga kolom {@code guru7_id} <b>terhapus permanen</b> tanpa
	 * tindakan pengguna. Lihat pembahasan lengkap pada {@link #getGuru()}.</p>
	 *
	 * @return guru pengampu slot ke-7; {@code null} bila slot tak bergurus <b>atau</b> bila
	 *         hari slot ini kosong
	 * @see #getGuru()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru7_id")
	public Guru getGuru7() {
		guru7 = check(guru7);
		if (getHari7() == null) {
			guru7 = null;
		}
		return guru7;
	}

	/**
	 * Menyetel guru pengampu slot ke-7. Nilai yang disetel akan <b>dibuang oleh getter-nya</b>
	 * bila hari slot ke-7 tidak ikut diisi — lihat {@link #getGuru7()}.
	 *
	 * @param guru7 guru pengampu; {@code null} mengosongkan slot
	 */
	public void setGuru7(Guru guru7) {
		this.guru7 = guru7;
	}

	/**
	 * Mengembalikan guru pengampu slot ke-8 (baris layar "Hari dan Jam Pelajaran VIII"); kolom
	 * {@code guru8_id}.
	 *
	 * <p><b>Getter destruktif — pola sama pada dua belas slot.</b> Setelah {@code check()},
	 * bila {@link #getHari8()} bernilai {@code null} (hari slot ini kosong atau hanya spasi),
	 * field {@code guru8} <b>ditulisi {@code null}</b>. Karena entity ini memakai
	 * <i>property access</i> dan {@code dynamicUpdate}, nilai {@code null} itu ikut tersimpan
	 * pada {@code flush} berikutnya, sehingga kolom {@code guru8_id} <b>terhapus permanen</b> tanpa
	 * tindakan pengguna. Lihat pembahasan lengkap pada {@link #getGuru()}.</p>
	 *
	 * @return guru pengampu slot ke-8; {@code null} bila slot tak bergurus <b>atau</b> bila
	 *         hari slot ini kosong
	 * @see #getGuru()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru8_id")
	public Guru getGuru8() {
		guru8 = check(guru8);
		if (getHari8() == null) {
			guru8 = null;
		}
		return guru8;
	}

	/**
	 * Menyetel guru pengampu slot ke-8. Nilai yang disetel akan <b>dibuang oleh getter-nya</b>
	 * bila hari slot ke-8 tidak ikut diisi — lihat {@link #getGuru8()}.
	 *
	 * @param guru8 guru pengampu; {@code null} mengosongkan slot
	 */
	public void setGuru8(Guru guru8) {
		this.guru8 = guru8;
	}

	/**
	 * Mengembalikan guru pengampu slot ke-9 (baris layar "Hari dan Jam Pelajaran IX"); kolom
	 * {@code guru9_id}.
	 *
	 * <p><b>Getter destruktif — pola sama pada dua belas slot.</b> Setelah {@code check()},
	 * bila {@link #getHari9()} bernilai {@code null} (hari slot ini kosong atau hanya spasi),
	 * field {@code guru9} <b>ditulisi {@code null}</b>. Karena entity ini memakai
	 * <i>property access</i> dan {@code dynamicUpdate}, nilai {@code null} itu ikut tersimpan
	 * pada {@code flush} berikutnya, sehingga kolom {@code guru9_id} <b>terhapus permanen</b> tanpa
	 * tindakan pengguna. Lihat pembahasan lengkap pada {@link #getGuru()}.</p>
	 *
	 * @return guru pengampu slot ke-9; {@code null} bila slot tak bergurus <b>atau</b> bila
	 *         hari slot ini kosong
	 * @see #getGuru()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru9_id")
	public Guru getGuru9() {
		guru9 = check(guru9);
		if (getHari9() == null) {
			guru9 = null;
		}
		return guru9;
	}

	/**
	 * Menyetel guru pengampu slot ke-9. Nilai yang disetel akan <b>dibuang oleh getter-nya</b>
	 * bila hari slot ke-9 tidak ikut diisi — lihat {@link #getGuru9()}.
	 *
	 * @param guru9 guru pengampu; {@code null} mengosongkan slot
	 */
	public void setGuru9(Guru guru9) {
		this.guru9 = guru9;
	}

	/**
	 * Mengembalikan guru pengampu slot ke-10 (baris layar "Hari dan Jam Pelajaran X"); kolom
	 * {@code guru10_id}.
	 *
	 * <p><b>Getter destruktif — pola sama pada dua belas slot.</b> Setelah {@code check()},
	 * bila {@link #getHari10()} bernilai {@code null} (hari slot ini kosong atau hanya spasi),
	 * field {@code guru10} <b>ditulisi {@code null}</b>. Karena entity ini memakai
	 * <i>property access</i> dan {@code dynamicUpdate}, nilai {@code null} itu ikut tersimpan
	 * pada {@code flush} berikutnya, sehingga kolom {@code guru10_id} <b>terhapus permanen</b> tanpa
	 * tindakan pengguna. Lihat pembahasan lengkap pada {@link #getGuru()}.</p>
	 *
	 * @return guru pengampu slot ke-10; {@code null} bila slot tak bergurus <b>atau</b> bila
	 *         hari slot ini kosong
	 * @see #getGuru()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru10_id")
	public Guru getGuru10() {
		guru10 = check(guru10);
		if (getHari10() == null) {
			guru10 = null;
		}
		return guru10;
	}

	/**
	 * Menyetel guru pengampu slot ke-10. Nilai yang disetel akan <b>dibuang oleh getter-nya</b>
	 * bila hari slot ke-10 tidak ikut diisi — lihat {@link #getGuru10()}.
	 *
	 * @param guru10 guru pengampu; {@code null} mengosongkan slot
	 */
	public void setGuru10(Guru guru10) {
		this.guru10 = guru10;
	}

	/**
	 * Mengembalikan nama hari slot ke-7 (baris layar "Hari dan Jam Pelajaran VII"); kolom
	 * {@code hari7}.
	 *
	 * <p>Normalisasi seragam dengan slot ke-1: string kosong atau hanya spasi dikembalikan
	 * sebagai {@code null}, hasilnya selalu di-{@code trim()}. Nilai {@code null} di sini
	 * <b>menghapus</b> guru slot ini lewat {@link #getGuru7()}.</p>
	 *
	 * @return nama hari slot ke-7, atau {@code null} bila slot tidak dijadwalkan
	 * @see #getHari()
	 */
	public String getHari7() {
		return this.hari7 == null || hari7.trim().isEmpty() ? null : hari7.trim();
	}

	/**
	 * Menyetel nama hari slot ke-7. Nilai disimpan apa adanya; normalisasi dilakukan getter.
	 *
	 * @param hari7 salah satu nilai {@code Common.haris}; {@code null} berarti slot ke-7 tidak
	 *        dipakai — dan ikut menghapus {@code guru7} pada pembacaan berikutnya
	 */
	public void setHari7(String hari7) {
		this.hari7 = hari7;
	}

	/**
	 * Mengembalikan nama hari slot ke-8 (baris layar "Hari dan Jam Pelajaran VIII"); kolom
	 * {@code hari8}.
	 *
	 * <p>Normalisasi seragam dengan slot ke-1: string kosong atau hanya spasi dikembalikan
	 * sebagai {@code null}, hasilnya selalu di-{@code trim()}. Nilai {@code null} di sini
	 * <b>menghapus</b> guru slot ini lewat {@link #getGuru8()}.</p>
	 *
	 * @return nama hari slot ke-8, atau {@code null} bila slot tidak dijadwalkan
	 * @see #getHari()
	 */
	public String getHari8() {
		return this.hari8 == null || hari8.trim().isEmpty() ? null : hari8.trim();
	}

	/**
	 * Menyetel nama hari slot ke-8. Nilai disimpan apa adanya; normalisasi dilakukan getter.
	 *
	 * @param hari8 salah satu nilai {@code Common.haris}; {@code null} berarti slot ke-8 tidak
	 *        dipakai — dan ikut menghapus {@code guru8} pada pembacaan berikutnya
	 */
	public void setHari8(String hari8) {
		this.hari8 = hari8;
	}

	/**
	 * Mengembalikan nama hari slot ke-9 (baris layar "Hari dan Jam Pelajaran IX"); kolom
	 * {@code hari9}.
	 *
	 * <p>Normalisasi seragam dengan slot ke-1: string kosong atau hanya spasi dikembalikan
	 * sebagai {@code null}, hasilnya selalu di-{@code trim()}. Nilai {@code null} di sini
	 * <b>menghapus</b> guru slot ini lewat {@link #getGuru9()}.</p>
	 *
	 * @return nama hari slot ke-9, atau {@code null} bila slot tidak dijadwalkan
	 * @see #getHari()
	 */
	public String getHari9() {
		return this.hari9 == null || hari9.trim().isEmpty() ? null : hari9.trim();
	}

	/**
	 * Menyetel nama hari slot ke-9. Nilai disimpan apa adanya; normalisasi dilakukan getter.
	 *
	 * @param hari9 salah satu nilai {@code Common.haris}; {@code null} berarti slot ke-9 tidak
	 *        dipakai — dan ikut menghapus {@code guru9} pada pembacaan berikutnya
	 */
	public void setHari9(String hari9) {
		this.hari9 = hari9;
	}

	/**
	 * Mengembalikan nama hari slot ke-10 (baris layar "Hari dan Jam Pelajaran X"); kolom
	 * {@code hari10}.
	 *
	 * <p>Normalisasi seragam dengan slot ke-1: string kosong atau hanya spasi dikembalikan
	 * sebagai {@code null}, hasilnya selalu di-{@code trim()}. Nilai {@code null} di sini
	 * <b>menghapus</b> guru slot ini lewat {@link #getGuru10()}.</p>
	 *
	 * @return nama hari slot ke-10, atau {@code null} bila slot tidak dijadwalkan
	 * @see #getHari()
	 */
	public String getHari10() {
		return this.hari10 == null || hari10.trim().isEmpty() ? null : hari10.trim();
	}

	/**
	 * Menyetel nama hari slot ke-10. Nilai disimpan apa adanya; normalisasi dilakukan getter.
	 *
	 * @param hari10 salah satu nilai {@code Common.haris}; {@code null} berarti slot ke-10 tidak
	 *        dipakai — dan ikut menghapus {@code guru10} pada pembacaan berikutnya
	 */
	public void setHari10(String hari10) {
		this.hari10 = hari10;
	}

	/**
	 * Apakah baris ini dikecualikan dari pemeriksaan bentrok jadwal.
	 *
	 * <p>Label pada layar: <b>"Abaikan jika ada jadwal bentrok"</b>. Default {@code false}.
	 * Perhatikan sifat <b>dua arah</b>-nya yang dijelaskan pada {@link #abaikanBentrok}:
	 * baris bercentang tidak hanya lolos pemeriksaan atas dirinya sendiri, tetapi juga
	 * menghilang sebagai pembanding sehingga ruang/kelas/guru yang dipakainya tidak lagi
	 * terhitung terpakai oleh baris lain.</p>
	 *
	 * @return {@code true} bila pemeriksaan bentrok dilewati untuk baris ini
	 */
	public Boolean getAbaikanBentrok() {
		return abaikanBentrok == null ? false : abaikanBentrok;
	}

	/**
	 * Menyetel pengecualian pemeriksaan bentrok.
	 *
	 * @param abaikanBentrok {@code true} untuk melewati pemeriksaan; {@code null} dibaca
	 *        sebagai {@code false}
	 */
	public void setAbaikanBentrok(Boolean abaikanBentrok) {
		this.abaikanBentrok = abaikanBentrok;
	}

	/**
	 * Mengembalikan slot jam ke-11 (baris layar "Hari dan Jam Pelajaran XI"); kolom
	 * {@code jam_pelajaran11_id}.
	 *
	 * <p>Pola seragam dengan sebelas saudaranya: memanggil {@code check()} untuk menuntaskan
	 * proxy lazy dan menuliskan instance kanonik kembali ke field. Tidak ada logika turunan
	 * lain.</p>
	 *
	 * @return jam pelajaran slot ke-11, atau {@code null} bila slot kosong
	 * @see #getJamPelajaran()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran11_id")
	public JamPelajaran getJamPelajaran11() {
		jamPelajaran11 = check(jamPelajaran11);
		return jamPelajaran11;
	}

	/**
	 * Menyetel slot jam ke-11.
	 *
	 * @param jamPelajaran11 jam pelajaran yang dipilih; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran11(JamPelajaran jamPelajaran11) {
		this.jamPelajaran11 = jamPelajaran11;
	}

	/**
	 * Mengembalikan slot jam ke-12 (baris layar "Hari dan Jam Pelajaran XII"); kolom
	 * {@code jam_pelajaran12_id}.
	 *
	 * <p>Pola seragam dengan sebelas saudaranya: memanggil {@code check()} untuk menuntaskan
	 * proxy lazy dan menuliskan instance kanonik kembali ke field. Tidak ada logika turunan
	 * lain.</p>
	 *
	 * @return jam pelajaran slot ke-12, atau {@code null} bila slot kosong
	 * @see #getJamPelajaran()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jam_pelajaran12_id")
	public JamPelajaran getJamPelajaran12() {
		jamPelajaran12 = check(jamPelajaran12);
		return jamPelajaran12;
	}

	/**
	 * Menyetel slot jam ke-12.
	 *
	 * @param jamPelajaran12 jam pelajaran yang dipilih; {@code null} mengosongkan slot
	 */
	public void setJamPelajaran12(JamPelajaran jamPelajaran12) {
		this.jamPelajaran12 = jamPelajaran12;
	}

	/**
	 * Mengembalikan guru pengampu slot ke-11 (baris layar "Hari dan Jam Pelajaran XI"); kolom
	 * {@code guru11_id}.
	 *
	 * <p><b>Getter destruktif — pola sama pada dua belas slot.</b> Setelah {@code check()},
	 * bila {@link #getHari11()} bernilai {@code null} (hari slot ini kosong atau hanya spasi),
	 * field {@code guru11} <b>ditulisi {@code null}</b>. Karena entity ini memakai
	 * <i>property access</i> dan {@code dynamicUpdate}, nilai {@code null} itu ikut tersimpan
	 * pada {@code flush} berikutnya, sehingga kolom {@code guru11_id} <b>terhapus permanen</b> tanpa
	 * tindakan pengguna. Lihat pembahasan lengkap pada {@link #getGuru()}.</p>
	 *
	 * @return guru pengampu slot ke-11; {@code null} bila slot tak bergurus <b>atau</b> bila
	 *         hari slot ini kosong
	 * @see #getGuru()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru11_id")
	public Guru getGuru11() {
		guru11 = check(guru11);
		if (getHari11() == null) {
			guru11 = null;
		}
		return guru11;
	}

	/**
	 * Menyetel guru pengampu slot ke-11. Nilai yang disetel akan <b>dibuang oleh getter-nya</b>
	 * bila hari slot ke-11 tidak ikut diisi — lihat {@link #getGuru11()}.
	 *
	 * @param guru11 guru pengampu; {@code null} mengosongkan slot
	 */
	public void setGuru11(Guru guru11) {
		this.guru11 = guru11;
	}

	/**
	 * Mengembalikan guru pengampu slot ke-12 (baris layar "Hari dan Jam Pelajaran XII"); kolom
	 * {@code guru12_id}.
	 *
	 * <p><b>Getter destruktif — pola sama pada dua belas slot.</b> Setelah {@code check()},
	 * bila {@link #getHari12()} bernilai {@code null} (hari slot ini kosong atau hanya spasi),
	 * field {@code guru12} <b>ditulisi {@code null}</b>. Karena entity ini memakai
	 * <i>property access</i> dan {@code dynamicUpdate}, nilai {@code null} itu ikut tersimpan
	 * pada {@code flush} berikutnya, sehingga kolom {@code guru12_id} <b>terhapus permanen</b> tanpa
	 * tindakan pengguna. Lihat pembahasan lengkap pada {@link #getGuru()}.</p>
	 *
	 * @return guru pengampu slot ke-12; {@code null} bila slot tak bergurus <b>atau</b> bila
	 *         hari slot ini kosong
	 * @see #getGuru()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru12_id")
	public Guru getGuru12() {
		guru12 = check(guru12);
		if (getHari12() == null) {
			guru12 = null;
		}
		return guru12;
	}

	/**
	 * Menyetel guru pengampu slot ke-12. Nilai yang disetel akan <b>dibuang oleh getter-nya</b>
	 * bila hari slot ke-12 tidak ikut diisi — lihat {@link #getGuru12()}.
	 *
	 * @param guru12 guru pengampu; {@code null} mengosongkan slot
	 */
	public void setGuru12(Guru guru12) {
		this.guru12 = guru12;
	}

	/**
	 * Mengembalikan nama hari slot ke-11 (baris layar "Hari dan Jam Pelajaran XI"); kolom
	 * {@code hari11}.
	 *
	 * <p>Normalisasi seragam dengan slot ke-1: string kosong atau hanya spasi dikembalikan
	 * sebagai {@code null}, hasilnya selalu di-{@code trim()}. Nilai {@code null} di sini
	 * <b>menghapus</b> guru slot ini lewat {@link #getGuru11()}.</p>
	 *
	 * @return nama hari slot ke-11, atau {@code null} bila slot tidak dijadwalkan
	 * @see #getHari()
	 */
	public String getHari11() {
		return this.hari11 == null || hari11.trim().isEmpty() ? null : hari11.trim();
	}

	/**
	 * Menyetel nama hari slot ke-11. Nilai disimpan apa adanya; normalisasi dilakukan getter.
	 *
	 * @param hari11 salah satu nilai {@code Common.haris}; {@code null} berarti slot ke-11 tidak
	 *        dipakai — dan ikut menghapus {@code guru11} pada pembacaan berikutnya
	 */
	public void setHari11(String hari11) {
		this.hari11 = hari11;
	}

	/**
	 * Mengembalikan nama hari slot ke-12 (baris layar "Hari dan Jam Pelajaran XII"); kolom
	 * {@code hari12}.
	 *
	 * <p>Normalisasi seragam dengan slot ke-1: string kosong atau hanya spasi dikembalikan
	 * sebagai {@code null}, hasilnya selalu di-{@code trim()}. Nilai {@code null} di sini
	 * <b>menghapus</b> guru slot ini lewat {@link #getGuru12()}.</p>
	 *
	 * @return nama hari slot ke-12, atau {@code null} bila slot tidak dijadwalkan
	 * @see #getHari()
	 */
	public String getHari12() {
		return this.hari12 == null || hari12.trim().isEmpty() ? null : hari12.trim();
	}

	/**
	 * Menyetel nama hari slot ke-12. Nilai disimpan apa adanya; normalisasi dilakukan getter.
	 *
	 * @param hari12 salah satu nilai {@code Common.haris}; {@code null} berarti slot ke-12 tidak
	 *        dipakai — dan ikut menghapus {@code guru12} pada pembacaan berikutnya
	 */
	public void setHari12(String hari12) {
		this.hari12 = hari12;
	}

	/**
	 * Implementasi kontrak {@link ais.database.model.VOPembelajaran}: apakah pertemuan diurutkan
	 * otomatis.
	 *
	 * @return {@code true} bila pengurutan otomatis aktif; default {@code true} bila
	 *         {@code null}
	 */
	@Override
	public Boolean getUrutkanotomatis() {
		// TODO Auto-generated method stub
		return urutkanotomatis == null ? true : urutkanotomatis;
	}

	/**
	 * Menyetel pengurutan otomatis pertemuan.
	 *
	 * @param urutkanotomatis {@code false} untuk mematikan; {@code null} dibaca sebagai
	 *        {@code true}
	 */
	@Override
	public void setUrutkanotomatis(Boolean urutkanotomatis) {
		this.urutkanotomatis = urutkanotomatis;
	}

	/**
	 * Mengembalikan sub mata pelajaran slot ke-1; kolom {@code sub_matapelajaran}.
	 *
	 * <p>Hanya menuntaskan proxy lazy lewat {@code check()}. Berbeda dari
	 * {@link #getMatapelajaran()}, sub mata pelajaran <b>tidak pernah diturunkan</b> dari kelas
	 * les maupun kurikulum — ia murni pilihan administrator per slot.</p>
	 *
	 * @return sub mata pelajaran slot ke-1, atau {@code null} bila slot mengajarkan mapel induk
	 * @see SubMatapelajaran
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran")
	public SubMatapelajaran getSubMatapelajaran() {
		subMatapelajaran = check(subMatapelajaran);
		return subMatapelajaran;
	}

	/**
	 * Menyetel sub mata pelajaran slot ke-1.
	 *
	 * @param subMatapelajaran sub mata pelajaran; {@code null} berarti slot mengajarkan mapel
	 *        induk apa adanya
	 */
	public void setSubMatapelajaran(SubMatapelajaran subMatapelajaran) {
		this.subMatapelajaran = subMatapelajaran;
	}

	/**
	 * Mengembalikan sub mata pelajaran slot ke-2; kolom {@code sub_matapelajaran_2}.
	 *
	 * <p>Pola seragam: hanya menuntaskan proxy lazy lewat {@code check()}, tanpa logika turunan.</p>
	 *
	 * @return sub mata pelajaran slot ke-2, atau {@code null} bila slot mengajarkan mapel induk
	 * @see #getSubMatapelajaran()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_2")
	public SubMatapelajaran getSubMatapelajaran2() {
		subMatapelajaran2 = check(subMatapelajaran2);
		return subMatapelajaran2;
	}

	/**
	 * Menyetel sub mata pelajaran slot ke-2.
	 *
	 * @param subMatapelajaran2 sub mata pelajaran; {@code null} berarti slot mengajarkan mapel
	 *        induk apa adanya
	 */
	public void setSubMatapelajaran2(SubMatapelajaran subMatapelajaran2) {
		this.subMatapelajaran2 = subMatapelajaran2;
	}

	/**
	 * Mengembalikan sub mata pelajaran slot ke-3; kolom {@code sub_matapelajaran_3}.
	 *
	 * <p>Pola seragam: hanya menuntaskan proxy lazy lewat {@code check()}, tanpa logika turunan.</p>
	 *
	 * @return sub mata pelajaran slot ke-3, atau {@code null} bila slot mengajarkan mapel induk
	 * @see #getSubMatapelajaran()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_3")
	public SubMatapelajaran getSubMatapelajaran3() {
		subMatapelajaran3 = check(subMatapelajaran3);
		return subMatapelajaran3;
	}

	/**
	 * Menyetel sub mata pelajaran slot ke-3.
	 *
	 * @param subMatapelajaran3 sub mata pelajaran; {@code null} berarti slot mengajarkan mapel
	 *        induk apa adanya
	 */
	public void setSubMatapelajaran3(SubMatapelajaran subMatapelajaran3) {
		this.subMatapelajaran3 = subMatapelajaran3;
	}

	/**
	 * Mengembalikan sub mata pelajaran slot ke-4; kolom {@code sub_matapelajaran_4}.
	 *
	 * <p>Pola seragam: hanya menuntaskan proxy lazy lewat {@code check()}, tanpa logika turunan.</p>
	 *
	 * @return sub mata pelajaran slot ke-4, atau {@code null} bila slot mengajarkan mapel induk
	 * @see #getSubMatapelajaran()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_4")
	public SubMatapelajaran getSubMatapelajaran4() {
		subMatapelajaran4 = check(subMatapelajaran4);
		return subMatapelajaran4;
	}

	/**
	 * Menyetel sub mata pelajaran slot ke-4.
	 *
	 * @param subMatapelajaran4 sub mata pelajaran; {@code null} berarti slot mengajarkan mapel
	 *        induk apa adanya
	 */
	public void setSubMatapelajaran4(SubMatapelajaran subMatapelajaran4) {
		this.subMatapelajaran4 = subMatapelajaran4;
	}

	/**
	 * Mengembalikan sub mata pelajaran slot ke-5; kolom {@code sub_matapelajaran_5}.
	 *
	 * <p>Pola seragam: hanya menuntaskan proxy lazy lewat {@code check()}, tanpa logika turunan.</p>
	 *
	 * @return sub mata pelajaran slot ke-5, atau {@code null} bila slot mengajarkan mapel induk
	 * @see #getSubMatapelajaran()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_5")
	public SubMatapelajaran getSubMatapelajaran5() {
		subMatapelajaran5 = check(subMatapelajaran5);
		return subMatapelajaran5;
	}

	/**
	 * Menyetel sub mata pelajaran slot ke-5.
	 *
	 * @param subMatapelajaran5 sub mata pelajaran; {@code null} berarti slot mengajarkan mapel
	 *        induk apa adanya
	 */
	public void setSubMatapelajaran5(SubMatapelajaran subMatapelajaran5) {
		this.subMatapelajaran5 = subMatapelajaran5;
	}

	/**
	 * Mengembalikan sub mata pelajaran slot ke-6; kolom {@code sub_matapelajaran_6}.
	 *
	 * <p>Pola seragam: hanya menuntaskan proxy lazy lewat {@code check()}, tanpa logika turunan.</p>
	 *
	 * @return sub mata pelajaran slot ke-6, atau {@code null} bila slot mengajarkan mapel induk
	 * @see #getSubMatapelajaran()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_6")
	public SubMatapelajaran getSubMatapelajaran6() {
		subMatapelajaran6 = check(subMatapelajaran6);
		return subMatapelajaran6;
	}

	/**
	 * Menyetel sub mata pelajaran slot ke-6.
	 *
	 * @param subMatapelajaran6 sub mata pelajaran; {@code null} berarti slot mengajarkan mapel
	 *        induk apa adanya
	 */
	public void setSubMatapelajaran6(SubMatapelajaran subMatapelajaran6) {
		this.subMatapelajaran6 = subMatapelajaran6;
	}

	/**
	 * Mengembalikan sub mata pelajaran slot ke-7; kolom {@code sub_matapelajaran_7}.
	 *
	 * <p>Pola seragam: hanya menuntaskan proxy lazy lewat {@code check()}, tanpa logika turunan.</p>
	 *
	 * @return sub mata pelajaran slot ke-7, atau {@code null} bila slot mengajarkan mapel induk
	 * @see #getSubMatapelajaran()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_7")
	public SubMatapelajaran getSubMatapelajaran7() {
		subMatapelajaran7 = check(subMatapelajaran7);
		return subMatapelajaran7;
	}

	/**
	 * Menyetel sub mata pelajaran slot ke-7.
	 *
	 * @param subMatapelajaran7 sub mata pelajaran; {@code null} berarti slot mengajarkan mapel
	 *        induk apa adanya
	 */
	public void setSubMatapelajaran7(SubMatapelajaran subMatapelajaran7) {
		this.subMatapelajaran7 = subMatapelajaran7;
	}

	/**
	 * Mengembalikan sub mata pelajaran slot ke-8; kolom {@code sub_matapelajaran_8}.
	 *
	 * <p>Pola seragam: hanya menuntaskan proxy lazy lewat {@code check()}, tanpa logika turunan.</p>
	 *
	 * @return sub mata pelajaran slot ke-8, atau {@code null} bila slot mengajarkan mapel induk
	 * @see #getSubMatapelajaran()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_8")
	public SubMatapelajaran getSubMatapelajaran8() {
		subMatapelajaran8 = check(subMatapelajaran8);
		return subMatapelajaran8;
	}

	/**
	 * Menyetel sub mata pelajaran slot ke-8.
	 *
	 * @param subMatapelajaran8 sub mata pelajaran; {@code null} berarti slot mengajarkan mapel
	 *        induk apa adanya
	 */
	public void setSubMatapelajaran8(SubMatapelajaran subMatapelajaran8) {
		this.subMatapelajaran8 = subMatapelajaran8;
	}

	/**
	 * Mengembalikan sub mata pelajaran slot ke-9; kolom {@code sub_matapelajaran_9}.
	 *
	 * <p>Pola seragam: hanya menuntaskan proxy lazy lewat {@code check()}, tanpa logika turunan.</p>
	 *
	 * @return sub mata pelajaran slot ke-9, atau {@code null} bila slot mengajarkan mapel induk
	 * @see #getSubMatapelajaran()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_9")
	public SubMatapelajaran getSubMatapelajaran9() {
		subMatapelajaran9 = check(subMatapelajaran9);
		return subMatapelajaran9;
	}

	/**
	 * Menyetel sub mata pelajaran slot ke-9.
	 *
	 * @param subMatapelajaran9 sub mata pelajaran; {@code null} berarti slot mengajarkan mapel
	 *        induk apa adanya
	 */
	public void setSubMatapelajaran9(SubMatapelajaran subMatapelajaran9) {
		this.subMatapelajaran9 = subMatapelajaran9;
	}

	/**
	 * Mengembalikan sub mata pelajaran slot ke-10; kolom {@code sub_matapelajaran_10}.
	 *
	 * <p>Pola seragam: hanya menuntaskan proxy lazy lewat {@code check()}, tanpa logika turunan.</p>
	 *
	 * @return sub mata pelajaran slot ke-10, atau {@code null} bila slot mengajarkan mapel induk
	 * @see #getSubMatapelajaran()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_10")
	public SubMatapelajaran getSubMatapelajaran10() {
		subMatapelajaran10 = check(subMatapelajaran10);
		return subMatapelajaran10;
	}

	/**
	 * Menyetel sub mata pelajaran slot ke-10.
	 *
	 * @param subMatapelajaran10 sub mata pelajaran; {@code null} berarti slot mengajarkan mapel
	 *        induk apa adanya
	 */
	public void setSubMatapelajaran10(SubMatapelajaran subMatapelajaran10) {
		this.subMatapelajaran10 = subMatapelajaran10;
	}

	/**
	 * Mengembalikan sub mata pelajaran slot ke-11; kolom {@code sub_matapelajaran_11}.
	 *
	 * <p>Pola seragam: hanya menuntaskan proxy lazy lewat {@code check()}, tanpa logika turunan.</p>
	 *
	 * @return sub mata pelajaran slot ke-11, atau {@code null} bila slot mengajarkan mapel induk
	 * @see #getSubMatapelajaran()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_11")
	public SubMatapelajaran getSubMatapelajaran11() {
		subMatapelajaran11 = check(subMatapelajaran11);
		return subMatapelajaran11;
	}

	/**
	 * Menyetel sub mata pelajaran slot ke-11.
	 *
	 * @param subMatapelajaran11 sub mata pelajaran; {@code null} berarti slot mengajarkan mapel
	 *        induk apa adanya
	 */
	public void setSubMatapelajaran11(SubMatapelajaran subMatapelajaran11) {
		this.subMatapelajaran11 = subMatapelajaran11;
	}

	/**
	 * Mengembalikan sub mata pelajaran slot ke-12; kolom {@code sub_matapelajaran_12}.
	 *
	 * <p>Pola seragam: hanya menuntaskan proxy lazy lewat {@code check()}, tanpa logika turunan.</p>
	 *
	 * @return sub mata pelajaran slot ke-12, atau {@code null} bila slot mengajarkan mapel induk
	 * @see #getSubMatapelajaran()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sub_matapelajaran_12")
	public SubMatapelajaran getSubMatapelajaran12() {
		subMatapelajaran12 = check(subMatapelajaran12);
		return subMatapelajaran12;
	}

	/**
	 * Menyetel sub mata pelajaran slot ke-12.
	 *
	 * @param subMatapelajaran12 sub mata pelajaran; {@code null} berarti slot mengajarkan mapel
	 *        induk apa adanya
	 */
	public void setSubMatapelajaran12(SubMatapelajaran subMatapelajaran12) {
		this.subMatapelajaran12 = subMatapelajaran12;
	}

	/**
	 * Mengembalikan kelas les yang dijadwalkan.
	 *
	 * <p>Hanya menuntaskan proxy lazy; tidak menimpa apa pun. Namun bila terisi ia
	 * <b>mendominasi</b> {@link #getMatapelajaran()}, {@link #getSekolah()},
	 * {@link #getRuang()} dan {@link #getMasaJadwalPelajaran()}.</p>
	 *
	 * <p>Kolomnya {@code unique = true}: satu kelas les hanya boleh punya satu baris jadwal.</p>
	 *
	 * @return kelas les, atau {@code null} bila jadwal ini bukan jadwal les
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelas_les_siswa", nullable = true, unique = true)
	public KelasLesSiswa getKelasLesSiswa() {
		kelasLesSiswa = check(kelasLesSiswa);
		return kelasLesSiswa;
	}

	/**
	 * Menyetel kelas les yang dijadwalkan.
	 *
	 * @param kelasLesSiswa kelas les; {@code null} bila jadwal memakai kelas reguler
	 */
	public void setKelasLesSiswa(KelasLesSiswa kelasLesSiswa) {
		this.kelasLesSiswa = kelasLesSiswa;
	}
	
	/**
	 * Apakah baris jadwal masih aktif.
	 *
	 * @return {@code true} bila aktif; default {@code true} bila {@code null}, sehingga baris
	 *         lama yang dibuat sebelum kolom ini ada tetap dianggap aktif
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif; 
	}

	/**
	 * Menyetel status aktif baris jadwal.
	 *
	 * @param aktif {@code false} untuk menonaktifkan; {@code null} dibaca sebagai {@code true}
	 */
	public void setAktif(Boolean aktif) { 
		this.aktif = aktif;
	}
}
