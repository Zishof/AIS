package ais.database.model;

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.common.Common;

/**
 * Master <b>aturan umum</b> pembatasan jumlah SKS maksimal yang boleh diambil mahasiswa saat
 * pengisian KRS, berdasarkan rentang nilai IP yang diperolehnya.
 *
 * <p>Satu baris tabel {@code pembatasan_nilai_ipk_untuk_pengambilan_krs} menyatakan satu kalimat
 * kebijakan akademik, kira-kira: <i>"mahasiswa Fakultas X, Prodi Y, Program Z, angkatan &ge; A,
 * yang IP terakhirnya &ge; B, boleh mengambil paling banyak C SKS"</i>. Kebijakan lengkap sebuah
 * institusi biasanya berupa <b>beberapa baris bertingkat</b> (mis. IP&nbsp;&ge;&nbsp;3,00 &rarr;
 * 24&nbsp;SKS; IP&nbsp;&ge;&nbsp;2,50 &rarr; 21&nbsp;SKS; IP&nbsp;&ge;&nbsp;2,00 &rarr;
 * 18&nbsp;SKS; dan seterusnya), bukan satu baris tunggal.</p>
 *
 * <h2>PERINGATAN nama: "IPK" pada dua field ini berarti hal yang berbeda</h2>
 *
 * <p>Penamaan hasil generator sangat menyesatkan dan sudah terlanjur dipakai di seluruh basis kode
 * (kolom DB, ekspor/impor massal, laporan). Baca dengan hati-hati:</p>
 * <ul>
 *   <li>{@link #getBatasTerendahIPK()} &mdash; <b>benar-benar nilai IP</b> ({@link Double}).
 *       Ini ambang <i>bawah</i> pita/band aturan.</li>
 *   <li>{@link #getBatasMaksimumIPKYangBolehDiambil()} &mdash; <b>BUKAN IPK sama sekali</b>,
 *       melainkan <b>jumlah SKS maksimal</b> ({@link Integer}). Layar
 *       {@code PembatasanNilaiIPKUntukPengambilanKRSAction} sudah memberi label yang benar
 *       ("Batas maksimum SKS"); hanya nama Java/kolomnya yang salah warisan.</li>
 * </ul>
 *
 * <h2>IP yang mana? IPS atau IPK</h2>
 *
 * <p>Nilai yang dibandingkan dengan {@link #getBatasTerendahIPK()} dihitung
 * {@code Common.ipTerakhir(mahasiswa, semester)} dan bergantung konfigurasi
 * {@code pembatasan_maksimal_sks_pada_pegambilan_krs_berdasarkan_ip_semester_sebelum_nya}:</p>
 * <ul>
 *   <li>konfigurasi <b>aktif</b> &rarr; dipakai <b>IPS</b> semester valid terakhir (semester cuti
 *       dan semester non-aktif dilewati mundur);</li>
 *   <li>konfigurasi <b>tidak aktif</b> &rarr; dipakai <b>IPK</b> kumulatif semester sebelumnya;</li>
 *   <li>semester 1 &rarr; 4.0; IP {@code null}/NaN/&lt;&nbsp;0,01 &rarr; dipaksa 0,5.</li>
 * </ul>
 * <p>Karena itu judul kolom di layar pun berubah-ubah ("IP Semester" vs "IP Kumulatif") mengikuti
 * konfigurasi yang sama. Mengubah konfigurasi tersebut mengubah arti <b>seluruh</b> baris tabel ini
 * tanpa satu pun baris ikut berubah &mdash; aturan yang tadinya wajar bisa jadi terlalu longgar
 * atau terlalu ketat.</p>
 *
 * <h2>Cakupan aturan: dari paling umum sampai satu mahasiswa</h2>
 *
 * <p>Enam kolom menentukan siapa yang terkena satu baris aturan. Semuanya {@code nullable};
 * {@code null} berarti "tidak dibatasi pada dimensi ini":</p>
 * <ol>
 *   <li>{@link #getFakultas()} &mdash; fakultas;</li>
 *   <li>{@link #getJurusan()} &mdash; jurusan/prodi;</li>
 *   <li>{@link #getProgram()} &mdash; program (Reguler/Karyawan/&hellip;), dicocokkan lewat
 *       <b>nama</b>-nya, bukan id-nya;</li>
 *   <li>{@link #getMinimumAngkatan()} &mdash; tahun angkatan minimum;</li>
 *   <li>{@link #getSemesterPendek()} &mdash; membedakan aturan semester reguler dari semester
 *       pendek;</li>
 *   <li>{@link #getMahasiswa()} &mdash; <b>satu mahasiswa tertentu</b> (opsional).</li>
 * </ol>
 *
 * <h2>Beda entity ini dengan keluarga <code>Pengecualian*</code></h2>
 *
 * <p>Entity {@code Pengecualian&hellip;} (mis.
 * {@code PengecualianJadwalPengisianKRSMahasiswa}) adalah <b>dispensasi per-individu</b>: barisnya
 * selalu menunjuk satu mahasiswa, umumnya berumur satu periode, dan bekerja sebagai
 * <i>tambalan</i> di atas aturan umum yang tetap berlaku bagi orang lain. Entity ini sebaliknya
 * adalah <b>aturan umum/kebijakan</b>: baris normalnya <b>tidak</b> menunjuk mahasiswa
 * ({@code mahasiswa = null}) dan berlaku untuk seluruh populasi yang cocok, tanpa masa berlaku
 * berbasis tanggal &mdash; hanya dimatikan lewat {@link #getAktif()}.</p>
 *
 * <p><b>Namun entity ini <i>juga</i> menyediakan jalur per-individu</b> lewat
 * {@link #getMahasiswa()} ("Khusus untuk mahasiswa" di layar). Perlu dipahami bahwa semantiknya
 * <b>berbeda tajam</b> dari keluarga {@code Pengecualian*}: begitu ada <b>minimal satu</b> baris
 * ber-{@code mahasiswa} = mahasiswa yang bersangkutan (dan cocok
 * {@code semesterPendek} + {@code aktif}), resolver <b>berpindah total</b> ke himpunan baris milik
 * mahasiswa itu dan <b>tidak pernah kembali</b> ke aturan umum. Jadi ini bukan pengecualian yang
 * ditumpuk, melainkan <b>penggantian</b> seluruh tabel kebijakan bagi mahasiswa tersebut. Akibat
 * praktisnya dibahas pada bagian "Kuirk" di bawah.</p>
 *
 * <h2>Bagaimana aturan dipilih (resolver)</h2>
 *
 * <p>Seluruh logika pemilihan ada di
 * {@code CommonAcademicSyncHelper.getIpkUntukPengambilanKRSDenganIPLast(...)}, bukan di kelas ini.
 * Ringkasnya:</p>
 * <ol>
 *   <li>Hitung {@code countBatas} = jumlah baris untuk mahasiswa ini (dengan
 *       {@code semesterPendek} cocok dan {@code aktif} null/true). Bila {@code semester <= 1} dan
 *       {@code countBatas == 0}, resolver mengembalikan {@code null} &mdash; mahasiswa semester 1
 *       tidak dibatasi kecuali punya baris pribadi.</li>
 *   <li>Hitung {@code iplast} = {@code Common.ipTerakhir(...)}.</li>
 *   <li>Jalankan <b>tujuh</b> query bertingkat, dari kombinasi paling spesifik ke paling longgar
 *       (fakultas+jurusan+program &rarr; fakultas+jurusan &rarr; fakultas+program &rarr; fakultas
 *       &rarr; fakultas tanpa syarat angkatan &rarr; program &rarr; tanpa cakupan). Query
 *       pertama yang menghasilkan baris menang. Semua tingkat memakai
 *       {@code batasTerendahIPK <= iplast} dan (kecuali satu tingkat)
 *       {@code minimumAngkatan <= tahunAngkatan}, diurutkan
 *       {@code minimumAngkatan DESC, batasTerendahIPK DESC} lalu {@code setMaxResults(1)} &mdash;
 *       artinya <b>pita IP tertinggi yang masih terlampaui</b> oleh mahasiswa yang dipakai.</li>
 *   <li>Bila {@code countBatas > 0}, ketujuh query di atas semuanya menambah
 *       {@code mahasiswa = <mahasiswa ini>}; bila {@code countBatas == 0}, semuanya menambah
 *       {@code mahasiswa IS NULL}.</li>
 * </ol>
 *
 * <p>Bila akhirnya tidak ada baris yang cocok, pemanggil jatuh ke angka default
 * {@link #getDefaultPembatasanNilaiIpUntukAmbilKRS()} (konfigurasi
 * {@code default_pembatasan_nilai_ip_untuk_ambil_KRS}, bawaan 24 SKS).</p>
 *
 * <h2>Siapa yang memakai</h2>
 * <ul>
 *   <li><b>Penegakan saat simpan KRS</b> &mdash;
 *       {@code Common.checkPembatasanSKSBerdasarkanIP(...)}: bila SKS yang diambil melebihi
 *       {@link #getBatasMaksimumIPKYangBolehDiambil()}, muncul Messagebox penolakan dan KRS tidak
 *       tersimpan. Dilewati bila semester &lt; {@code minimal_smt_syarat_krs} (bawaan 2).</li>
 *   <li><b>Tampilan layar KRS</b> &mdash; {@code Common.getMinDanMaxIPK(...)} yang mengembalikan
 *       {@code [maxSKS, minIP, ipLast]}, dipakai {@code KrsKurikulumHelper} dan
 *       {@code KrsNonPaketHelper} untuk menampilkan sisa jatah SKS.</li>
 *   <li><b>Laporan</b> &mdash; {@code CommonReportHelper} mengisi parameter Jasper
 *       {@code max_sks}, {@code max_sks_next}, {@code max_sks_berikut} pada KHS/KRS (termasuk
 *       memanggil resolver untuk {@code semester + 1} guna menampilkan jatah semester berikutnya).</li>
 *   <li><b>Layar master</b> &mdash; {@code ais.action.master.PembatasanNilaiIPKUntukPengambilanKRSAction}
 *       (CRUD + filter + unduh/unggah data massal).</li>
 * </ul>
 *
 * <h2>Pemetaan Hibernate</h2>
 * <ul>
 *   <li>Akses <b>properti</b> (anotasi ada pada getter), {@code dynamicInsert}/{@code dynamicUpdate}
 *       aktif, dan {@link Audited @Audited} sehingga setiap perubahan menghasilkan revisi Envers.</li>
 *   <li>{@link #getFakultas()}, {@link #getJurusan()}, {@link #getMahasiswa()} dipetakan
 *       {@code FetchType.LAZY} &mdash; karena itu ketiganya (dan hanya ketiganya) memanggil
 *       {@code check(...)} untuk meresolusi proxy. {@link #getProgram()} dibiarkan
 *       <i>eager</i> ({@code ManyToOne} bawaan) dengan {@code @Fetch(FetchMode.SELECT)}, jadi tidak
 *       memerlukan {@code check(...)}.</li>
 *   <li>{@link #getAktif()} dan {@link #getSemesterPendek()} <b>tidak</b> memakai
 *       {@link Column @Column}, sehingga jatuh ke {@code MyNamingStrategy} yang mewarisi
 *       {@code DefaultNamingStrategy}: nama kolom = nama properti apa adanya, yaitu
 *       {@code aktif} dan &mdash; perhatikan &mdash; {@code semesterPendek} dalam <i>camelCase</i>,
 *       tidak mengikuti gaya {@code snake_case} kolom-kolom lain di tabel yang sama.</li>
 * </ul>
 *
 * <h2>Hubungan dengan {@link GeneralValueObject}</h2>
 *
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} &mdash;
 * ia POJO abstrak biasa, sehingga Hibernate <b>tidak</b> memetakan properti induknya. Karena itu
 * {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()}, dan {@link #getTanggal_dirubah()}
 * <b>wajib</b> dideklarasikan ulang di sini. Pengulangan itu <b>bukan bug dan jangan dibersihkan</b>:
 * menghapusnya membuat kolom-kolom tersebut hilang dari pemetaan. Semantik lengkap
 * {@code check(...)}, cache, dan jejak audit ada di Javadoc kelas induk.</p>
 *
 * <h2>Kuirk dan jebakan yang sudah terverifikasi</h2>
 * <ul>
 *   <li><b>Baris dengan {@code batasTerendahIPK} kosong tidak pernah dipakai.</b> Ketujuh tingkat
 *       resolver memakai {@code Restrictions.le("batasTerendahIPK", iplast)}; pada SQL,
 *       {@code NULL <= x} bernilai <i>unknown</i>, sehingga baris seperti itu selalu tersaring
 *       keluar &mdash; baris mati yang tetap tampil di layar seolah berlaku.</li>
 *   <li><b>Hal yang sama berlaku untuk {@code minimumAngkatan} kosong</b> pada enam dari tujuh
 *       tingkat. Hanya satu tingkat (fakultas + {@code batasTerendahIPK}, tanpa syarat angkatan)
 *       yang bisa menangkapnya, dan tingkat itu baru dicoba setelah empat tingkat lain gagal.</li>
 *   <li><b>Baris pribadi mematikan aturan umum, bukan menambahnya.</b> Bila seorang mahasiswa
 *       diberi satu baris khusus tetapi baris itu tidak cocok (mis. IP-nya turun di bawah
 *       {@code batasTerendahIPK} baris tersebut, atau angkatannya di bawah
 *       {@code minimumAngkatan}), resolver tetap tidak melihat aturan umum dan mengembalikan
 *       {@code null}; pemanggil lalu memakai default {@link #getDefaultPembatasanNilaiIpUntukAmbilKRS()}
 *       (bawaan 24 SKS) &mdash; yang bisa jauh <b>lebih longgar</b> daripada aturan umum yang
 *       seharusnya mengikatnya.</li>
 *   <li><b>Baris pribadi juga membuat mahasiswa semester 1 ikut dibatasi</b>, padahal jalur umum
 *       sengaja melepas semester 1 dari pembatasan.</li>
 *   <li><b>{@code aktif} kosong dianggap aktif</b> ({@link #getAktif()} mengembalikan {@code true}
 *       bila field {@code null}), sejalan dengan resolver yang memakai
 *       {@code aktif IS NULL OR aktif = true}. Konsekuensinya, checkbox "Aktif" di layar tampak
 *       tercentang untuk baris lama meski nilainya masih {@code null} di DB.</li>
 *   <li><b>{@code semesterPendek} adalah flag dua nilai, bukan angka.</b> Layar hanya pernah
 *       mengisinya dengan {@code null} (reguler) atau {@link Perkuliahan#SEMESTER_PENDEK} (= 1),
 *       dan resolver membedakannya dengan {@code IS NULL} vs {@code = nilai}. Nilai lain (mis. 2)
 *       hanya bisa masuk lewat unggah massal dan tidak akan pernah cocok dengan pemanggil manapun.</li>
 *   <li><b>Cakupan dicocokkan dengan kesetaraan persis, bukan hierarki.</b> Baris yang mengisi
 *       {@code jurusan} tetapi mengosongkan {@code fakultas} tidak akan terjaring oleh tingkat
 *       manapun yang menyertakan {@code fakultas}, karena resolver membandingkan
 *       {@code fakultas = <fakultas mahasiswa>}, bukan "kosong berarti semua".</li>
 *   <li><b>Program dicocokkan lewat nama.</b> Resolver memakai {@code program.nama}, sehingga
 *       mengganti nama sebuah {@link Program} diam-diam melepaskan seluruh aturan yang menunjuknya.</li>
 *   <li><b>Komentar generator salah salin-tempel</b> ("Bank generated by hbm2java") &mdash; kelas
 *       ini tidak ada hubungannya dengan entity {@code Bank}.</li>
 * </ul>
 *
 * @see GeneralValueObject
 * @see ais.action.master.PembatasanNilaiIPKUntukPengambilanKRSAction
 * @see ais.database.dao.PembatasanNilaiIPKUntukPengambilanKRSDao
 * @see Mahasiswa
 * @see Program
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "pembatasan_nilai_ipk_untuk_pengambilan_krs")
public class PembatasanNilaiIPKUntukPengambilanKRS extends GeneralValueObject {

	// public static int DEFAULT = 24;

	/**
	 * Batas SKS bawaan yang dipakai ketika <b>tidak ada satu pun</b> baris aturan yang cocok
	 * untuk mahasiswa yang sedang mengisi KRS.
	 *
	 * <p><b>Sumber nilai.</b> Konfigurasi {@code default_pembatasan_nilai_ip_untuk_ambil_KRS}
	 * dengan bawaan {@code "24"}. Perhatikan bahwa {@code Common.getKonfigurasi(kunci, bawaan)}
	 * bersifat <b>auto-seed</b>: bila kunci belum ada di tabel konfigurasi, nilai bawaan
	 * <i>ditulis</i> ke DB pada pemanggilan pertama. Karena itu mengubah literal {@code "24"} di
	 * sini <b>tidak</b> mengubah perilaku instalasi yang sudah berjalan &mdash; nilainya sudah
	 * terlanjur tersimpan di DB dan harus diubah lewat layar konfigurasi.</p>
	 *
	 * <p><b>Nama yang menyesatkan.</b> Meski namanya menyebut "NilaiIp", yang dikembalikan adalah
	 * <b>jumlah SKS</b>, sejalan dengan {@link #getBatasMaksimumIPKYangBolehDiambil()}.</p>
	 *
	 * <p><b>Dipanggil dari.</b> {@code Common.checkPembatasanSKSBerdasarkanIP(...)},
	 * {@code Common.getMinDanMaxIPK(...)}, {@code CommonReportHelper} (parameter laporan
	 * {@code max_sks}), serta {@code KrsKurikulumHelper}/{@code KrsNonPaketHelper} saat menyusun
	 * teks informasi jatah SKS.</p>
	 *
	 * <p><b>Risiko.</b> {@link Integer#parseInt(String)} dipanggil tanpa penjagaan; nilai
	 * konfigurasi yang bukan angka murni (mis. {@code "24 SKS"} atau {@code "24,0"}) melempar
	 * {@link NumberFormatException} yang menjalar ke pemanggil dan menggagalkan penyimpanan KRS.
	 * Hanya spasi di tepi yang ditangani ({@code trim()}).</p>
	 *
	 * @return batas maksimal SKS bawaan hasil parsing konfigurasi
	 */
	public static int getDefaultPembatasanNilaiIpUntukAmbilKRS() {
		return Integer
				.parseInt(Common.getKonfigurasi("default_pembatasan_nilai_ip_untuk_ambil_KRS", "24").getNilai().trim());
	}

	/**
	 * Versi serialisasi kelas. Entity ini melintasi sesi/desktop ZK sehingga wajib
	 * {@link java.io.Serializable}; nilainya dipertahankan apa adanya agar object lama tetap
	 * kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama baris aturan; lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna pengubah terakhir (jejak audit); lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna pengubah terakhir (jejak audit); lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris aturan ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah terakhir.
	 *
	 * <p><b>Menolak nilai kosong secara diam-diam:</b> {@code null} maupun string yang hanya berisi
	 * spasi diabaikan (method langsung {@code return}), sehingga jejak lama dipertahankan. Ini pola
	 * audit standar seluruh entity paket ini: proses batch tanpa konteks pengguna tidak boleh
	 * menghapus jejak yang sudah ada.</p>
	 *
	 * @param olehId id pengguna baru; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: nilai {@code null} atau berisi spasi saja
	 * diabaikan tanpa peringatan.</p>
	 *
	 * @param oleh nama pengguna baru; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris aturan ini.
	 *
	 * @return nama pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum {@code UPDATE} baris ini.
	 *
	 * <p>Mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #setOleh(String)}/{@link #setOlehId(String)} dari konteks pengguna aktif serta
	 * memperbarui {@link #setTanggal_dirubah(Date)}. Tidak pernah dipanggil manual dari kode
	 * aplikasi, dan <b>tidak</b> berjalan pada {@code INSERT} (hanya {@code UPDATE}).</p>
	 *
	 * <p>Perhatikan interaksinya dengan {@link #toString()} dan getter relasi yang menulis balik
	 * hasil {@code check(...)} ke field: pembacaan biasa pun berpotensi membuat entity kotor,
	 * memicu {@code UPDATE}, memanggil kait ini, dan menghasilkan revisi Envers atas nama pengguna
	 * yang kebetulan sedang membuka layar.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir. Diinisialisasi ke jam server saat object dibuat
	 * ({@code WaktuUtil.getDate()}), lalu diperbarui kait {@link #onUpdate()} pada setiap
	 * {@code UPDATE}. Deklarasi ulang dari {@link GeneralValueObject}; lihat Javadoc kelas.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir. Umumnya dipanggil
	 * {@code AuditTimestampInterceptor}, bukan kode layar.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir baris aturan ini.
	 *
	 * <p>Tanpa {@link Column @Column}, sehingga jatuh ke penamaan default {@code MyNamingStrategy}
	 * &mdash; kolom {@code tanggal_dirubah} apa adanya, bertipe {@code TIMESTAMP}.</p>
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada object yang baru dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Ringkasan teks satu baris aturan untuk keperluan log/debug.
	 *
	 * <p><b>Awas, method ini punya efek samping.</b> Sebelum merangkai teks, ia memanggil
	 * {@link #getFakultas()}, {@link #getJurusan()}, dan {@link #getProgram()} lalu
	 * <b>menugaskan kembali</b> hasilnya ke field {@code fakultas}, {@code jurusan}, dan
	 * {@code program}. Untuk dua yang pertama ini berarti proxy lazy ikut diresolusi dan
	 * field-nya bisa <i>tertukar</i> ke instance kanonik hasil {@code check(...)} &mdash;
	 * pembacaan yang dari luar terlihat murni bisa menandai entity sebagai kotor dan memicu
	 * {@code UPDATE} beserta revisi Envers lewat {@link #onUpdate()}. Hindari memanggilnya di
	 * dalam loop pada sesi yang masih terbuka, dan jangan menganggapnya aman untuk dipanggil
	 * dari kode audit.</p>
	 *
	 * <p>Field {@code mahasiswa}, {@code aktif}, dan {@code semesterPendek} sengaja (atau
	 * terlupakan) tidak ikut dicetak, padahal ketiganya justru penentu apakah baris ini terpakai
	 * &mdash; jangan mengandalkan keluarannya untuk membedakan aturan umum dari aturan pribadi.</p>
	 *
	 * @return teks berisi cakupan (fakultas/jurusan/program), angkatan minimum, ambang IP terendah,
	 *         batas SKS maksimal, dan keterangan
	 */
	public String toString() {
		fakultas = getFakultas();
		jurusan = getJurusan();
		program = getProgram();
		return "fakultas = " + fakultas + ", jurusan = " + jurusan + ", program = " + program + ", minimumAngkatan = "
				+ minimumAngkatan + ", batasTerendahIPK = " + batasTerendahIPK + ", batasMaksimumIPKYangBolehDiambil = "
				+ batasMaksimumIPKYangBolehDiambil + ", " + keterangan;
	}

	/** Ambang <b>bawah</b> pita IP yang mengaktifkan aturan ini; lihat {@link #getBatasTerendahIPK()}. */
	private Double batasTerendahIPK;
	/** Jumlah <b>SKS</b> maksimal yang diizinkan (bukan IPK); lihat {@link #getBatasMaksimumIPKYangBolehDiambil()}. */
	private Integer batasMaksimumIPKYangBolehDiambil;
	/** Fakultas yang dicakup aturan ini, atau {@code null}; lihat {@link #getFakultas()}. */
	private Fakultas fakultas;
	/** Jurusan/prodi yang dicakup aturan ini, atau {@code null}; lihat {@link #getJurusan()}. */
	private Jurusan jurusan;
	/** Program (Reguler/Karyawan/&hellip;) yang dicakup, atau {@code null}; lihat {@link #getProgram()}. */
	private Program program;
	/**
	 * Tahun angkatan minimum yang terkena aturan ini, diinisialisasi {@code 2000} untuk object baru.
	 * Nilai awal ini hanya berlaku bagi object yang dibuat di memori; object yang dimuat dari DB
	 * memakai isi kolomnya, termasuk {@code null}. Lihat {@link #getMinimumAngkatan()}.
	 */
	private Integer minimumAngkatan = 2000;
	/** Catatan bebas operator; lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Mahasiswa tertentu yang aturannya dikhususkan, atau {@code null} untuk aturan umum; lihat {@link #getMahasiswa()}. */
	private Mahasiswa mahasiswa;
	/** Penanda aturan semester pendek ({@code null} = reguler); lihat {@link #getSemesterPendek()}. */
	private Integer semesterPendek;
	/** Saklar aktif/nonaktif baris aturan ({@code null} dianggap aktif); lihat {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate untuk membuat instance saat memuat baris, dan
	 * dipakai layar master saat menekan tombol "Tambah".
	 *
	 * <p>Tidak menyetel apa pun selain nilai awal field: {@link #getMinimumAngkatan()} bernilai
	 * {@code 2000} dan {@link #getTanggal_dirubah()} bernilai jam server saat ini.</p>
	 */
	public PembatasanNilaiIPKUntukPengambilanKRS() {
	}

	/**
	 * Kunci utama baris aturan.
	 *
	 * <p>Dipetakan {@code IDENTITY} dan {@code insertable = false}: nilai dibangkitkan sequence
	 * PostgreSQL, bukan oleh aplikasi.</p>
	 *
	 * @return id baris, atau {@code null} bila object belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama. Praktis hanya dipakai Hibernate saat memuat baris; kode layar tidak
	 * pernah menyetelnya sendiri.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Catatan bebas yang ditulis operator, mis. nomor SK atau alasan kebijakan.
	 *
	 * <p>Tidak dipakai logika apa pun &mdash; murni dokumentasi bagi operator berikutnya. Ikut
	 * dicetak {@link #toString()} dan termasuk kolom yang diekspor/diimpor fitur data massal.</p>
	 *
	 * @return keterangan bebas, atau {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengisi ambang bawah pita IP.
	 *
	 * <p>Layar master mengisinya dari {@code Decimalbox} lewat {@code .doubleValue()}.</p>
	 *
	 * @param batasTerendahIPK ambang IP terendah; {@code null} membuat baris ini
	 *                         <b>tidak pernah terpilih</b> resolver (lihat Javadoc kelas)
	 */
	public void setBatasTerendahIPK(Double batasTerendahIPK) {
		this.batasTerendahIPK = batasTerendahIPK;
	}

	/**
	 * Ambang <b>bawah</b> pita IP yang membuat aturan ini berlaku.
	 *
	 * <p>Resolver memakainya sebagai {@code batasTerendahIPK <= ipTerakhir} dan mengurutkan
	 * kandidat {@code DESC}, sehingga di antara beberapa baris yang sama-sama terlampaui,
	 * yang ambangnya <b>paling tinggi</b> yang menang. Batas atas pita tidak pernah disimpan
	 * &mdash; ia terbentuk secara implisit dari ambang baris berikutnya.</p>
	 *
	 * <p>Yang dibandingkan bisa IPS atau IPK bergantung konfigurasi; lihat Javadoc kelas.</p>
	 *
	 * @return ambang IP terendah, atau {@code null} (baris efektif mati)
	 */
	@Column(name = "batas_terendah_ipk", nullable = true, precision = 15)
	public Double getBatasTerendahIPK() {
		return batasTerendahIPK;
	}

	/**
	 * Mengisi fakultas yang dicakup aturan ini.
	 *
	 * @param fakultas fakultas cakupan; {@code null} berarti aturan tidak dibatasi per fakultas
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Fakultas yang dicakup aturan ini.
	 *
	 * <p>Dipetakan {@code LAZY}, karena itu getter meresolusi proxy lewat {@code check(...)} dari
	 * {@link GeneralValueObject} dan <b>menulis balik hasilnya ke field</b> {@code fakultas}.
	 * Penulisan balik ini hanya menyentuh memori (bukan DB) dan tidak menutup sesi Hibernate,
	 * tetapi dapat menukar instance object menjadi instance kanonik dari cache; lihat Javadoc
	 * kelas induk untuk urutan resolusi lengkapnya.</p>
	 *
	 * <p>Resolver mencocokkannya dengan kesetaraan persis terhadap fakultas mahasiswa, jadi
	 * {@code null} <b>bukan</b> berarti "cocok dengan semua fakultas" pada tingkat-tingkat query
	 * yang menyertakan kolom ini &mdash; baris tanpa fakultas hanya terjaring tingkat yang memang
	 * tidak menyaring fakultas.</p>
	 *
	 * @return fakultas cakupan, atau {@code null} bila aturan tidak dibatasi per fakultas
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * Mengisi jurusan/prodi yang dicakup aturan ini.
	 *
	 * @param jurusan jurusan cakupan; {@code null} berarti aturan tidak dibatasi per jurusan
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Jurusan/program studi yang dicakup aturan ini.
	 *
	 * <p>Sama seperti {@link #getFakultas()}: dipetakan {@code LAZY}, meresolusi proxy lewat
	 * {@code check(...)}, dan menulis balik hasilnya ke field (memori saja, tanpa akses tulis DB
	 * dan tanpa menutup sesi).</p>
	 *
	 * <p>Perlu diingat kombinasinya dengan {@link #getFakultas()}: resolver menyaring
	 * fakultas <i>dan</i> jurusan bersamaan pada tingkat-tingkat awal, sehingga baris yang mengisi
	 * jurusan tetapi mengosongkan fakultas praktis tidak pernah terjaring pada tingkat-tingkat
	 * tersebut.</p>
	 *
	 * @return jurusan cakupan, atau {@code null} bila aturan tidak dibatasi per jurusan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Mengisi jumlah SKS maksimal yang diizinkan aturan ini.
	 *
	 * <p>Layar master mengambilnya dari {@code Decimalbox} "Batas maksimum SKS" lewat
	 * {@code .intValue()}, jadi pecahan yang diketik operator dipangkas diam-diam.</p>
	 *
	 * @param batasMaksimumIPKYangBolehDiambil jumlah SKS maksimal (meski namanya menyebut "IPK")
	 */
	public void setBatasMaksimumIPKYangBolehDiambil(Integer batasMaksimumIPKYangBolehDiambil) {
		this.batasMaksimumIPKYangBolehDiambil = batasMaksimumIPKYangBolehDiambil;
	}

	/**
	 * <b>Jumlah SKS maksimal</b> yang boleh diambil bila aturan ini yang terpilih.
	 *
	 * <p><b>Nama dan kolomnya menyesatkan.</b> Terlepas dari kata "IPK" pada nama method dan kolom
	 * {@code batas_maksimum_ipk_yang_boleh_diambil}, nilai yang disimpan adalah <b>SKS</b>
	 * ({@link Integer}), bukan indeks prestasi. Seluruh pemanggil memperlakukannya demikian:
	 * {@code Common.checkPembatasanSKSBerdasarkanIP(...)} membandingkannya langsung dengan jumlah
	 * SKS yang diambil, {@code Common.getMinDanMaxIPK(...)} mengembalikannya sebagai elemen
	 * {@code maxSKS}, dan {@code CommonReportHelper} memasangnya ke parameter Jasper
	 * {@code max_sks}/{@code max_sks_next}/{@code max_sks_berikut}.</p>
	 *
	 * <p>Bila {@code null}, pemanggil seperti {@code Common.checkPembatasanSKSBerdasarkanIP(...)}
	 * akan melakukan unboxing pada perbandingan {@code jumlahSks > maxSks} dan melempar
	 * {@link NullPointerException}: baris aturan yang terpilih tetapi kolom SKS-nya kosong
	 * menggagalkan penyimpanan KRS, bukan melonggarkannya.</p>
	 *
	 * @return batas SKS maksimal, atau {@code null} bila belum diisi
	 */
	@Column(name = "batas_maksimum_ipk_yang_boleh_diambil", nullable = true)
	public Integer getBatasMaksimumIPKYangBolehDiambil() {
		return batasMaksimumIPKYangBolehDiambil;
	}

	/**
	 * Tahun angkatan <b>minimum</b> mahasiswa yang terkena aturan ini.
	 *
	 * <p>Resolver memakainya sebagai {@code minimumAngkatan <= tahunAngkatan mahasiswa} dan
	 * mengurutkan {@code DESC}, sehingga angkatan yang lebih baru otomatis memakai aturan
	 * terbaru sementara angkatan lama tetap memakai aturan lama &mdash; inilah mekanisme
	 * "kurikulum/kebijakan berlaku mulai angkatan sekian" pada tabel ini.</p>
	 *
	 * <p><b>Awas:</b> nilai {@code null} membuat baris tersaring keluar pada enam dari tujuh
	 * tingkat query resolver (SQL {@code NULL <= x} bernilai <i>unknown</i>). Layar master
	 * mewajibkan kolom ini diisi, tetapi jalur unggah massal tidak.</p>
	 *
	 * @return tahun angkatan minimum ({@code 2000} pada object baru), atau {@code null}
	 */
	@Column(name = "minimum_angkatan", nullable = true)
	public Integer getMinimumAngkatan() {
		return minimumAngkatan;
	}

	/**
	 * Mengisi tahun angkatan minimum.
	 *
	 * @param minimumAngkatan tahun angkatan minimum; sebaiknya tidak {@code null} (lihat
	 *                        {@link #getMinimumAngkatan()})
	 */
	public void setMinimumAngkatan(Integer minimumAngkatan) {
		this.minimumAngkatan = minimumAngkatan;
	}

	/**
	 * Program (Reguler/Karyawan/Kelas Malam/&hellip;) yang dicakup aturan ini.
	 *
	 * <p>Berbeda dari {@link #getFakultas()}/{@link #getJurusan()}/{@link #getMahasiswa()},
	 * getter ini <b>tidak</b> memanggil {@code check(...)} dan memang tidak perlu: relasinya
	 * dibiarkan memakai fetch bawaan {@code ManyToOne} (eager) dengan
	 * {@code @Fetch(FetchMode.SELECT)}, sehingga Hibernate sudah memuat object nyata &mdash; bukan
	 * proxy &mdash; lewat {@code SELECT} terpisah saat baris ini dimuat. Konsekuensinya tidak ada
	 * penulisan balik ke field di sini, tetapi setiap baris aturan yang dimuat menghasilkan satu
	 * query tambahan (N+1) walau programnya tidak pernah dibaca.</p>
	 *
	 * <p>Resolver mencocokkannya lewat {@code program.nama}, bukan id &mdash; mengganti nama
	 * sebuah {@link Program} diam-diam melepaskan aturan yang menunjuknya.</p>
	 *
	 * @return program cakupan, atau {@code null} bila aturan tidak dibatasi per program
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "program", nullable = true)
	public Program getProgram() {
		return program;
	}

	/**
	 * Mengisi program yang dicakup aturan ini.
	 *
	 * @param program program cakupan; {@code null} berarti aturan tidak dibatasi per program
	 */
	public void setProgram(Program program) {
		this.program = program;
	}

	/**
	 * Mahasiswa tertentu yang aturannya dikhususkan &mdash; kolom yang mengubah baris ini dari
	 * kebijakan umum menjadi aturan pribadi.
	 *
	 * <p><b>Semantiknya bukan "pengecualian tambahan", melainkan "penggantian".</b> Bila seorang
	 * mahasiswa punya minimal satu baris di sini, resolver
	 * ({@code CommonAcademicSyncHelper.getIpkUntukPengambilanKRSDenganIPLast}) beralih sepenuhnya
	 * ke himpunan baris miliknya dan tidak pernah lagi melihat baris ber-{@code mahasiswa} kosong.
	 * Bila tidak satu pun baris pribadinya cocok, hasilnya {@code null} dan pemanggil jatuh ke
	 * {@link #getDefaultPembatasanNilaiIpUntukAmbilKRS()} &mdash; bukan ke aturan umum. Perbedaan
	 * ini yang membedakan tabel ini dari keluarga entity {@code Pengecualian*}; lihat Javadoc
	 * kelas.</p>
	 *
	 * <p>Dipetakan {@code LAZY}, sehingga getter meresolusi proxy lewat {@code check(...)} dan
	 * menulis balik hasilnya ke field {@code mahasiswa} (memori saja; tidak menulis DB dan tidak
	 * menutup sesi Hibernate).</p>
	 *
	 * <p>Layar master mengisinya lewat banbox "Khusus untuk mahasiswa" dengan petunjuk
	 * "Kosongkan jika bukan untuk mahasiswa tertentu", dan layar daftarnya memakai
	 * {@code LEFT JOIN} agar aturan umum tetap ikut tampil.</p>
	 *
	 * @return mahasiswa yang dikhususkan, atau {@code null} untuk aturan umum
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * Mengisi mahasiswa yang aturannya dikhususkan.
	 *
	 * <p>Menyetel nilai bukan-{@code null} mengubah baris ini dari kebijakan umum menjadi aturan
	 * pribadi dengan konsekuensi yang dijelaskan pada {@link #getMahasiswa()} &mdash; termasuk
	 * <b>mematikan seluruh aturan umum</b> bagi mahasiswa tersebut.</p>
	 *
	 * @param mahasiswa mahasiswa target, atau {@code null} untuk mengembalikannya jadi aturan umum
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Saklar aktif/nonaktif baris aturan.
	 *
	 * <p><b>Menormalkan {@code null} menjadi {@code true}</b>: baris warisan yang kolomnya belum
	 * pernah diisi tetap dianggap berlaku. Normalisasi ini hanya pada nilai balik &mdash; field
	 * {@code aktif} <b>tidak</b> ikut ditulisi, sehingga getter ini tidak destruktif dan tidak
	 * membuat entity kotor.</p>
	 *
	 * <p>Perilakunya sejalan dengan resolver yang menyaring
	 * {@code aktif IS NULL OR aktif = true}. Efek sampingnya di layar: checkbox "Aktif" tampak
	 * tercentang untuk baris yang sebenarnya masih {@code null} di DB, dan baru tersimpan sebagai
	 * {@code true} eksplisit setelah operator meng-<i>uncheck</i> lalu men-<i>check</i> lagi
	 * (setiap perubahan checkbox langsung disimpan lewat {@code Common.refreshSaveOrUpdate}).</p>
	 *
	 * <p>Tanpa {@link Column @Column}, jadi kolomnya bernama {@code aktif} mengikuti penamaan
	 * default.</p>
	 *
	 * @return {@code true} bila aturan berlaku (termasuk saat field masih {@code null}),
	 *         {@code false} bila sengaja dinonaktifkan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengaktifkan atau menonaktifkan baris aturan.
	 *
	 * <p>Menonaktifkan adalah satu-satunya cara "mencabut" kebijakan tanpa menghapus barisnya;
	 * tabel ini tidak punya kolom masa berlaku berbasis tanggal.</p>
	 *
	 * @param aktif {@code true} = berlaku, {@code false} = dinonaktifkan, {@code null} = dianggap
	 *              berlaku (lihat {@link #getAktif()})
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Penanda bahwa aturan ini berlaku untuk <b>semester pendek</b>, bukan semester reguler.
	 *
	 * <p>Meski bertipe {@link Integer}, perilakunya dua nilai: {@code null} = aturan semester
	 * reguler, {@link Perkuliahan#SEMESTER_PENDEK} ({@code 1}) = aturan semester pendek. Resolver
	 * membedakannya dengan {@code semesterPendek IS NULL} versus
	 * {@code semesterPendek = <nilai yang diminta>}, sehingga kedua kelompok aturan benar-benar
	 * terpisah: aturan reguler <b>tidak</b> berlaku pada semester pendek, dan sebaliknya. Bila
	 * belum ada aturan semester pendek sama sekali, pengisian KRS semester pendek langsung jatuh
	 * ke default {@link #getDefaultPembatasanNilaiIpUntukAmbilKRS()}.</p>
	 *
	 * <p>Nilai selain {@code null} dan {@code 1} tidak pernah dibuat layar master (checkbox "SP"
	 * hanya menulis kedua nilai itu) dan tidak akan pernah cocok dengan pemanggil manapun; nilai
	 * seperti itu hanya bisa masuk lewat unggah data massal.</p>
	 *
	 * <p>Tanpa {@link Column @Column}, sehingga nama kolomnya {@code semesterPendek} dalam
	 * <i>camelCase</i> &mdash; menyimpang dari gaya {@code snake_case} kolom lain di tabel ini.</p>
	 *
	 * @return {@link Perkuliahan#SEMESTER_PENDEK} bila aturan semester pendek, {@code null} bila
	 *         aturan semester reguler
	 */
	public Integer getSemesterPendek() {
		return semesterPendek;
	}

	/**
	 * Menyetel penanda semester pendek.
	 *
	 * @param semesterPendek {@link Perkuliahan#SEMESTER_PENDEK} untuk aturan semester pendek, atau
	 *                       {@code null} untuk aturan semester reguler
	 */
	public void setSemesterPendek(Integer semesterPendek) {
		this.semesterPendek = semesterPendek;
	}

}
