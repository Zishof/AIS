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

/**
 * Entity master <b>gelaran ujian seleksi PSB</b> (Penerimaan Siswa Baru) — satu baris mewakili
 * satu penyelenggaraan ujian masuk milik satu {@link GelombangPendaftaranPsb}, lengkap dengan
 * lokasi, jumlah hari ujian (1&ndash;10), tanggal masing-masing hari, sakelar tampil di kartu,
 * serta dua blok teks informasi yang dicetak pada kartu calon siswa.
 *
 * <p><b>Bukan</b> jadwal per sesi/mata uji: rincian jam mulai&ndash;selesai dan nama sesi ada di
 * {@code JadwalUjianPSB} yang menunjuk balik ke entity ini. Entity ini adalah "payung"-nya, satu
 * tingkat di atas jadwal maupun ruang.</p>
 *
 * <h3>Posisi dalam rantai PSB (TERVERIFIKASI dari FK di kode)</h3>
 * <p>Arah kepemilikan FK berjalan dari bawah ke atas; entity ini <i>tidak</i> menyimpan koleksi
 * anak sama sekali:</p>
 * <pre>
 *   GelombangPendaftaranPsb
 *        &uarr; (gelombangPendaftaranPsb)
 *   <b>UjianPSB</b>  &larr;(ujianPSB) JadwalUjianPSB   [sesi/mata uji per hari]
 *        &uarr; (ujianPSB)
 *   RuangPSB                                     [ruang + kapasitas + gedung]
 *        &uarr; (ruangPSB)
 *   RuangGelombangPendaftaranPsbPSB              [penempatan 1 calon siswa di 1 ruang]
 *        &rarr; CalonSiswa
 * </pre>
 * <p>Rantai penuh {@code RuangGelombangPendaftaranPsbPSB} &rarr;
 * {@link ais.database.model.sekolah.RuangPSB} &rarr; {@code UjianPSB} inilah yang dipakai saat
 * mencetak kartu ujian: dari calon siswa ditelusuri baris penempatan
 * {@link ais.database.model.sekolah.RuangGelombangPendaftaranPsbPSB}, lalu ruangnya, lalu ujian
 * yang memiliki ruang itu, dan dari sanalah tanggal/lokasi/keterangan diambil.</p>
 *
 * <h3>Pemakai TERVERIFIKASI</h3>
 * <ul>
 *   <li><b>{@code UjianPSBAction}</b> (layar master satu-satunya). Menyediakan pencarian per nama
 *       dan per gelombang, dialog tambah/ubah, dan tombol hapus.</li>
 *   <li><b>{@code GelombangPendaftaranPsbAction}</b> — menyisipkan layar di atas sebagai tab
 *       "Ujian" lewat {@code MyInclude("/pages/psb/ujian_psb.zul?gelombangPendaftaranPsb=<id>")}.
 *       Ini <i>satu-satunya</i> jalur navigasi resmi; lihat catatan hak akses di bawah.</li>
 *   <li><b>{@code RuangPSBAction}</b> — combo "Ruang untuk ujian" memilih baris entity ini sebagai
 *       induk sebuah {@link ais.database.model.sekolah.RuangPSB}.</li>
 *   <li><b>{@code GelombangPendaftaranPsb.chekKuotaPendaftar()}</b> — <b>menyemai otomatis</b> satu
 *       baris {@code UjianPSB} bernama {@code "Online"} berlokasi {@code "Sekolah"} bila gelombang
 *       belum punya ujian sama sekali, lalu langsung membuat {@link RuangPSB} {@code "Online"}
 *       berkapasitas 10.000 yang menunjuk ke ujian tersebut. Dipanggil dari
 *       {@code GelombangPendaftaranPsbAction} saat gelombang disimpan.</li>
 *   <li><b>{@code CommonReportPsb}</b> — dialog absensi/album peserta ujian PSB memfilter
 *       {@code JadwalUjianPSB} lewat alias {@code ujianPSB.tahunAkademik} dan
 *       {@code ujianPSB.gelombangPendaftaranPsb}; label pilihan jadwal memakai
 *       {@link #getNama()} entity ini.</li>
 *   <li><b>{@code CommonReportHelper.onCetakSuratKeteranganLulus(CalonSiswa)}</b> — menyalin
 *       seluruh properti entity ini ke parameter laporan {@code sekolah/Keterangan_Lulus} dengan
 *       prefiks {@code "ujian"} melalui {@code Common.insertProperty(..., 0)}. Baris dipilih dengan
 *       {@code setMaxResults(1)} <b>tanpa {@code addOrder}</b> &rarr; bila satu gelombang punya
 *       lebih dari satu ujian, ujian mana yang tercetak tidak deterministik.</li>
 *   <li><b>Template Jasper</b> (membaca kolom DB langsung, bukan lewat getter):
 *       <ul>
 *         <li>{@code report/sekolah/KartuBayarPsbMandiri.jrxml} &rarr;
 *             {@code f.keterangan as info} — jadi {@link #getKeterangan()} adalah teks pada
 *             <b>kartu pembayaran/pendaftaran</b>;</li>
 *         <li>{@code report/sekolah/KartuUjianSpsbMandiri.jrxml} &rarr;
 *             {@code f.keterangansetelahbayar as info} — jadi
 *             {@link #getKeteranganSetelahBayar()} adalah teks pada <b>kartu ujian</b>;</li>
 *         <li>keduanya juga membaca {@code f.tanggalujian1..10}, {@code f.lokasi}, dan
 *             {@code f.tampilkanjadwalujiandikartuujian};</li>
 *         <li>{@code KartuUjianSpsbMandiri_subreport1/2.jrxml} menjalankan
 *             {@code select * from sekolah.jadwal_ujian_psb where ujian_psb = $P{ujian}} —
 *             konfirmasi bahwa {@code JadwalUjianPSB} adalah anak entity ini.</li>
 *       </ul>
 *       Penamaan kolom di DB adalah nama properti yang <b>dilipat ke huruf kecil tanpa garis
 *       bawah</b> ({@code keterangansetelahbayar}, {@code tanggalujian1},
 *       {@code tampilkanjadwalujiandikartuujian}), bukan gaya {@code snake_case}.</li>
 * </ul>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 *   <li><b>Identitas &amp; jejak audit:</b> {@link #getId()}, {@link #getOleh()},
 *       {@link #getOlehId()}, {@link #getTanggal_dirubah()}, {@link #onUpdate()},
 *       {@link #toString()}.</li>
 *   <li><b>Identitas ujian:</b> {@link #getNama()}, {@link #getLokasi()},
 *       {@link #getGelombangPendaftaranPsb()}.</li>
 *   <li><b>Nilai turunan dari gelombang:</b> {@link #getTahunAkademik()}, {@link #getTahun()}.</li>
 *   <li><b>Kalender ujian:</b> {@link #getJumlahHariUjian()} dan
 *       {@link #getTanggalUjian1()}&hellip;{@link #getTanggalUjian10()}.</li>
 *   <li><b>Teks cetak &amp; sakelar tampilan:</b>
 *       {@link #getTampilkanJadwalUjianDiKartuUjian()}, {@link #getKeterangan()},
 *       {@link #getKeteranganSetelahBayar()}, {@link #getKeteranganHeader()},
 *       {@link #getKeteranganSetelahBayarHeader()}.</li>
 * </ul>
 *
 * <h3>Hal non-obvious yang WAJIB diketahui</h3>
 * <ol>
 *   <li><b>Pemetaan memakai <i>property access</i>.</b> Anotasi JPA menempel pada getter
 *       ({@link #getId()} bertanda {@code @Id}), sehingga Hibernate <b>memanggil getter</b> setiap
 *       kali membaca state untuk INSERT/UPDATE. Konsekuensinya semua getter yang menulis balik ke
 *       field (butir 2&ndash;4) benar-benar tersimpan ke basis data, bukan sekadar nilai tampilan
 *       sementara.</li>
 *   <li><b>Getter yang menulis nilai default.</b> {@link #getKeterangan()},
 *       {@link #getKeteranganSetelahBayar()}, {@link #getKeteranganHeader()},
 *       {@link #getKeteranganSetelahBayarHeader()}, {@link #getJumlahHariUjian()}, dan
 *       {@link #getTampilkanJadwalUjianDiKartuUjian()} mengisi field bila kosong/{@code null}.
 *       Baris hasil semai otomatis {@code chekKuotaPendaftar()} karena itu tetap keluar dengan teks
 *       boilerplate pembayaran bank dan daftar bawaan "Alat Tuilis / Papan Ujian" (perhatikan salah
 *       ketik <i>Tuilis</i> yang ikut tercetak di kartu ujian).</li>
 *   <li><b>Getter DESTRUKTIF pada tanggal ujian.</b>
 *       {@link #getTanggalUjian2()}&hellip;{@link #getTanggalUjian10()} <b>menulis {@code null}</b>
 *       ke field-nya sendiri ketika {@link #getJumlahHariUjian()} lebih kecil dari nomor hari yang
 *       bersangkutan. Karena Hibernate membaca lewat getter, menurunkan "Jumlah Hari Ujian" dari
 *       10 menjadi 2 <b>menghapus permanen</b> tanggal hari ke-3..10 pada flush berikutnya;
 *       menaikkan angkanya kembali tidak memulihkan tanggal lama. Efek yang sama terjadi lewat
 *       jalur baca murni yang menyalin properti secara reflektif
 *       ({@code Common.insertProperty} pada {@code onCetakSuratKeteranganLulus}).</li>
 *   <li><b>Denormalisasi tahun akademik.</b> {@link #getTahunAkademik()} menimpa field dari
 *       {@code gelombangPendaftaranPsb.getTahunAjaran()} setiap kali dibaca, dan
 *       {@link #getTahun()} mengurai potongan pertama string itu. Kolomnya nyata dan
 *       <b>dipakai sebagai filter</b> oleh {@code CommonReportPsb}
 *       ({@code Restrictions.eq("ujianPSB.tahunAkademik", ...)}), sehingga salinan yang basi
 *       membuat daftar absensi/jadwal tampak kosong.</li>
 *   <li><b>Dua properti tanpa pemakai.</b> Pencarian menyeluruh atas kode Java, ZUL, JSP, dan
 *       seluruh {@code *.jrxml} tidak menemukan satu pun pembaca
 *       {@link #getKeteranganHeader()} maupun {@link #getKeteranganSetelahBayarHeader()}; layar
 *       master pun mendeklarasikan textbox-nya lalu <b>mengomentarinya</b>. Keduanya konfigurasi
 *       mati yang tetap menghabiskan satu kolom {@code text} per baris. Kembarannya di modul
 *       perguruan tinggi, {@code ais.database.model.UjianPMB}, mencatat kondisi yang sama.</li>
 *   <li><b>Tanpa kolom tenant.</b> Entity ini tidak punya {@code sekolah}/{@code yayasan};
 *       cakupan tenant hanya diwarisi lewat {@link #getGelombangPendaftaranPsb()} dan
 *       <b>tidak pernah ditegakkan</b> oleh layar mana pun (lihat catatan hak akses).</li>
 * </ol>
 *
 * <h3>Catatan hak akses (hasil audit, bukan bagian kontrak entity)</h3>
 * <ul>
 *   <li><b>Layar master bergerbang BENAR</b> (verifikasi negatif): {@code UjianPSBAction}
 *       memanggil {@code Common.doCheckSecurity()} di {@code doBeforeCompose()}, menolak sesi tanpa
 *       {@code usersTemp} atau tanpa {@code CommonPrivilages.READ}, lalu memasang
 *       {@code CREATE}/{@code UPDATE}/{@code DELETE} pada tombol Tambah/Ubah/Hapus. Layar ini juga
 *       <b>tidak menyisipkan panel atau helper detail apa pun</b> — {@code ujian_psb.zul} hanya
 *       berisi filter dan grid — sehingga pola "master benar, detail rusak" tidak punya sasaran di
 *       sini.</li>
 *   <li><b>Pewarisan hak lewat menu induk.</b> {@code ujian_psb.zul} tidak terdaftar sebagai menu
 *       mana pun; satu-satunya jalur adalah include di tab "Ujian" milik layar Gelombang
 *       Pendaftaran PSB. Karena {@code CommonPrivilages.checkPrevilages()} bertumpu pada
 *       {@code Common.getCurrentMenu()} yang membaca atribut <i>session</i> {@code "currentMenu"},
 *       hak yang dievaluasi adalah hak menu yang sedang aktif — yaitu menu Gelombang Pendaftaran
 *       PSB pada jalur normal, atau menu apa pun yang terakhir dibuka pengguna bila URL diketik
 *       langsung.</li>
 *   <li><b>Cakupan lintas sekolah.</b> Combo gelombang pada layar master maupun pada
 *       {@code RuangPSBAction} diisi {@code Common.insertCombo(...)} yang hanya menyaring
 *       {@code aktif}; {@code GelombangPendaftaranPsb} sendiri memiliki kolom {@code sekolah}.
 *       Parameter URL {@code gelombangPendaftaranPsb} pun dimuat dengan
 *       {@code Restrictions.idEq(...)} tanpa uji kepemilikan. Praktisnya operator satu sekolah
 *       dapat memilih gelombang sekolah lain di instalasi yang sama dan membaca/mengubah ujian
 *       miliknya.</li>
 * </ul>
 *
 * <p><b>Catatan pewarisan.</b> Kelas ini memperluas {@link ais.database.model.GeneralValueObject},
 * sebuah POJO abstrak biasa — <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass}.
 * Hibernate tidak memetakan properti milik induk, sehingga deklarasi ulang {@link #id},
 * {@link #oleh}, {@link #olehId}, dan {@link #tanggal_dirubah} di kelas ini adalah <b>keharusan
 * teknis</b>, bukan duplikasi yang perlu "dirapikan".</p>
 *
 * <p><b>Persistensi.</b> Tabel {@code sekolah.ujian_psb}. {@code dynamicInsert}/{@code dynamicUpdate}
 * aktif sehingga hanya kolom yang berubah yang dikirim. {@code @Audited} (Hibernate Envers)
 * merekam riwayat perubahan; layar master menampilkannya lewat
 * {@code RevisiHelper.createNewRevisi(UjianPSB.class, ...)}. Operasi massal berbasis SQL/HQL
 * native akan melewati Envers.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.sekolah.GelombangPendaftaranPsb
 * @see ais.database.model.sekolah.RuangPSB
 * @see ais.database.model.sekolah.RuangGelombangPendaftaranPsbPSB
 * @see ais.database.model.sekolah.JadwalUjianPSB
 * @see ais.database.model.UjianPMB
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "ujian_psb")

public class UjianPSB extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai tetap agar instance lama tetap dapat dibaca; jangan diubah.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * Kunci utama {@code sekolah.ujian_psb.id}. Dideklarasikan ulang di sini karena
	 * {@link GeneralValueObject} bukan superclass terpetakan. Lihat {@link #getId()}.
	 */
	private Long id;
	/**
	 * Nama pengguna terakhir yang mengubah baris ini, diisi oleh interceptor audit. Lihat
	 * {@link #getOleh()}.
	 */
	private String oleh;
	/**
	 * Id pengguna terakhir yang mengubah baris ini, pendamping {@link #oleh}. Lihat
	 * {@link #getOlehId()}.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna pengubah terakhir.
	 *
	 * <p>Nilai {@code null}, string kosong, atau string berisi spasi saja <b>diabaikan</b> —
	 * jejak audit yang sudah ada sengaja tidak boleh terhapus oleh pemanggil yang tidak
	 * mengetahui identitas pengguna. Pola ini seragam di seluruh entity turunan
	 * {@link GeneralValueObject}.</p>
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila kosong/{@code null}
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: masukan kosong/{@code null} diabaikan agar nilai
	 * audit lama tidak tertimpa.</p>
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila kosong/{@code null}
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait {@code @PreUpdate}: dipanggil Hibernate tepat sebelum UPDATE dieksekusi dan
	 * mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} untuk memperbarui stempel
	 * waktu serta identitas pengubah.
	 *
	 * <p>Baris kode yang sama juga mendeklarasikan field {@link #tanggal_dirubah} dengan nilai awal
	 * {@code WaktuUtil.getDate()} — tata letak satu baris ini dihasilkan penyuntingan massal;
	 * jangan dipisah tanpa alasan agar diff repo tetap bersih. Jangan panggil method ini secara
	 * manual: pemanggilnya adalah penyedia persistensi.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir. Umumnya diisi otomatis oleh
	 * {@link #onUpdate()}; setter ini disediakan untuk Hibernate dan proses migrasi data.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada objek yang baru dibuat
	 *         karena field diinisialisasi dengan waktu saat instansiasi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks entity: langsung mengembalikan {@link #nama} apa adanya (tanpa
	 * {@code trim()}, berbeda dari {@link #getNama()}).
	 *
	 * <p>Dipakai sebagai label item combo — mis. combo "Ruang untuk ujian" di
	 * {@code RuangPSBAction} — sehingga baris tanpa nama akan tampil sebagai {@code "null"} atau
	 * kosong di layar.</p>
	 *
	 * @return nama ujian, dapat {@code null} bila belum diisi
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Nama gelaran ujian (mis. {@code "Gelombang 1 Tahap Tulis"}, atau {@code "Online"} untuk baris
	 * hasil semai otomatis). Kolom {@code nama}, wajib. Lihat {@link #getNama()}.
	 */
	private String nama;
	/**
	 * Lokasi penyelenggaraan ujian dalam bentuk teks bebas (mis. {@code "Sekolah"}); dicetak pada
	 * kartu pembayaran dan kartu ujian. Lihat {@link #getLokasi()}.
	 */
	private String lokasi;
	/**
	 * Banyaknya hari ujian, 1&ndash;10, default 1. Menentukan berapa banyak baris "Tanggal Ujian
	 * ke-N" yang tampak di dialog dan — lebih penting — tanggal mana yang <b>dinolkan</b> oleh
	 * getter-nya. Lihat {@link #getJumlahHariUjian()}.
	 */
	private Integer jumlahHariUjian = 1;
	/** Tanggal hari ujian ke-1; satu-satunya tanggal yang selalu berlaku. Lihat {@link #getTanggalUjian1()}. */
	private Date tanggalUjian1;
	/** Tanggal hari ujian ke-2; dinolkan bila {@link #jumlahHariUjian} &lt; 2. Lihat {@link #getTanggalUjian2()}. */
	private Date tanggalUjian2;
	/** Tanggal hari ujian ke-3; dinolkan bila {@link #jumlahHariUjian} &lt; 3. Lihat {@link #getTanggalUjian3()}. */
	private Date tanggalUjian3;
	/** Tanggal hari ujian ke-4; dinolkan bila {@link #jumlahHariUjian} &lt; 4. Lihat {@link #getTanggalUjian4()}. */
	private Date tanggalUjian4;
	/** Tanggal hari ujian ke-5; dinolkan bila {@link #jumlahHariUjian} &lt; 5. Lihat {@link #getTanggalUjian5()}. */
	private Date tanggalUjian5;
	/** Tanggal hari ujian ke-6; dinolkan bila {@link #jumlahHariUjian} &lt; 6. Lihat {@link #getTanggalUjian6()}. */
	private Date tanggalUjian6;
	/** Tanggal hari ujian ke-7; dinolkan bila {@link #jumlahHariUjian} &lt; 7. Lihat {@link #getTanggalUjian7()}. */
	private Date tanggalUjian7;
	/** Tanggal hari ujian ke-8; dinolkan bila {@link #jumlahHariUjian} &lt; 8. Lihat {@link #getTanggalUjian8()}. */
	private Date tanggalUjian8;
	/** Tanggal hari ujian ke-9; dinolkan bila {@link #jumlahHariUjian} &lt; 9. Lihat {@link #getTanggalUjian9()}. */
	private Date tanggalUjian9;
	/** Tanggal hari ujian ke-10; dinolkan bila {@link #jumlahHariUjian} &lt; 10. Lihat {@link #getTanggalUjian10()}. */
	private Date tanggalUjian10;

	/**
	 * Salinan tahun ajaran gelombang (mis. {@code "2026/2027"}). Nilai <b>turunan</b>: ditimpa
	 * setiap kali {@link #getTahunAkademik()} dipanggil. Dipakai sebagai kolom filter oleh
	 * {@code CommonReportPsb}.
	 */
	private String tahunAkademik;
	/**
	 * Tahun awal dari {@link #tahunAkademik} sebagai bilangan (mis. {@code 2026}). Nilai turunan;
	 * lihat {@link #getTahun()}.
	 */
	private Integer tahun;
	/**
	 * Gelombang pendaftaran PSB pemilik gelaran ujian ini. Satu-satunya jalur pewarisan cakupan
	 * sekolah/yayasan bagi entity ini. Lihat {@link #getGelombangPendaftaranPsb()}.
	 */
	private GelombangPendaftaranPsb gelombangPendaftaranPsb;
	/**
	 * Sakelar: tampilkan blok jadwal ujian pada kartu ujian/kartu pembayaran. Default {@code true}.
	 * Dibaca langsung sebagai kolom {@code tampilkanjadwalujiandikartuujian} oleh template Jasper.
	 * Lihat {@link #getTampilkanJadwalUjianDiKartuUjian()}.
	 */
	private Boolean tampilkanJadwalUjianDiKartuUjian;
	/**
	 * Teks informasi pada <b>kartu pembayaran/pendaftaran</b> ({@code f.keterangan as info} di
	 * {@code KartuBayarPsbMandiri.jrxml}). Label layar: "Informasi ke peserta ujian pada kartu
	 * pembayaran". Lihat {@link #getKeterangan()}.
	 */
	private String keterangan;
	/**
	 * Judul yang seharusnya mendahului {@link #keterangan}. <b>Tanpa pemakai</b> di seluruh
	 * codebase. Lihat {@link #getKeteranganHeader()}.
	 */
	private String keteranganHeader;
	/**
	 * Teks informasi pada <b>kartu ujian</b> ({@code f.keterangansetelahbayar as info} di
	 * {@code KartuUjianSpsbMandiri.jrxml}) — dinamai "setelah bayar" karena kartu ujian baru bisa
	 * dicetak setelah kewajiban pembayaran terpenuhi. Label layar: "Informasi ke peserta ujian pada
	 * kartu Ujian". Lihat {@link #getKeteranganSetelahBayar()}.
	 */
	private String keteranganSetelahBayar;
	/**
	 * Judul yang seharusnya mendahului {@link #keteranganSetelahBayar}. <b>Tanpa pemakai</b> di
	 * seluruh codebase. Lihat {@link #getKeteranganSetelahBayarHeader()}.
	 */
	private String keteranganSetelahBayarHeader;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Juga dipakai langsung oleh {@code UjianPSBAction.onAdd()} untuk membuka dialog "Tambah
	 * Ujian PSB" dan oleh {@code GelombangPendaftaranPsb.chekKuotaPendaftar()} saat menyemai baris
	 * {@code "Online"} otomatis. Instance baru sudah membawa {@link #jumlahHariUjian} = 1 dan
	 * {@link #tanggal_dirubah} = waktu sekarang.</p>
	 */
	public UjianPSB() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Dipetakan {@code IDENTITY} dan {@code insertable = false} — nilainya dibangkitkan oleh
	 * basis data, bukan aplikasi. {@code null} berarti entity belum tersimpan; {@code UjianPSBAction}
	 * memakai fakta ini untuk memilih judul dialog "Tambah" atau "Ubah".</p>
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
	 * Menetapkan kunci utama. Disediakan untuk Hibernate dan penyalinan data; jangan diisi manual
	 * pada alur normal.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama gelaran ujian, sudah dipangkas spasi tepi.
	 *
	 * <p>Kolom wajib ({@code nullable = false}); {@code UjianPSBAction.onSave()} menolak simpan bila
	 * kosong. Nilai ini muncul sebagai kolom "Nama" di grid, label item combo di
	 * {@code RuangPSBAction}, judul entri revisi Envers, serta bagian label pilihan jadwal pada
	 * dialog absensi {@code CommonReportPsb}. Perhatikan {@link #toString()} <b>tidak</b> memangkas
	 * spasi sehingga bisa berbeda tipis dari nilai di sini.</p>
	 *
	 * @return nama ujian tanpa spasi tepi, atau {@code null} bila field belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama gelaran ujian. Nilai disimpan apa adanya (tanpa {@code trim()}); pemangkasan
	 * baru terjadi saat dibaca lewat {@link #getNama()}.
	 *
	 * @param nama nama ujian
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan teks informasi untuk <b>kartu pembayaran/pendaftaran</b> calon siswa.
	 *
	 * <p><b>Efek samping — getter yang menulis.</b> Bila field {@code null} atau berisi spasi saja,
	 * method ini <b>mengisi field</b> dengan teks bawaan enam butir (petunjuk membayar lewat bank
	 * memakai KODE PEMBAYARAN, menyimpan bukti, login ulang ke portal PSB, tombol Check Ulang,
	 * melengkapi biodata, lalu mencetak kartu ujian). Karena entity ini dipetakan dengan
	 * <i>property access</i>, isian tersebut ikut tertulis ke basis data pada flush berikutnya —
	 * termasuk saat entity hanya dibaca untuk dirender di grid.</p>
	 *
	 * <p>Template {@code KartuBayarPsbMandiri.jrxml} membaca kolom {@code f.keterangan} secara
	 * langsung, sehingga teks bawaan baru muncul di kartu setelah nilai default tersebut benar-benar
	 * tersimpan; untuk baris yang belum pernah disentuh aplikasi, kolom cetak akan kosong.</p>
	 *
	 * @return teks informasi kartu pembayaran; tidak pernah {@code null} setelah pemanggilan ini
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		if (keterangan == null) {
			keterangan = "";
		}

		if (keterangan.trim().isEmpty()) {
			keterangan = "1. Pembayaran dapat dilakukan melalui Bank dengan KODE PEMBAYARAN yang terdapat pada Kartu Pendaftaran bagian bawah calon Siswa Baru.\n"
					+ "2  Bukti Pembayaran harap disimpan sebagai bukti pembayaran.\n"
					+ "3. Setelah melakukan pembayaran Harap malakukan Login kembali ke portal penerimaan siswa baru, dan Klik Info Pembayaran, masukkan nomor registrasi dan tanggal lahir.\n"
					+ "4. Jika pembayaran Anda belum masuk, klik tombol Check Ulang, jika sudah, Anda bisa mencetak Bukti Pembayaran.\n"
					+ "5. Selanjutnya, tutup menu Info Pembayaran, dan click Tombol Login Calon Siswa untuk melengkapi pengisian Form Biodata Calon Siswa beserta lampiran-lampiran-nya.\n"
					+ "6. Terakhir, Cetak Kartu Ujian.";
		}

		return this.keterangan;
	}

	/**
	 * Menetapkan teks informasi kartu pembayaran.
	 *
	 * <p>Diisi dari textarea "Informasi ke peserta ujian pada kartu pembayaran" di
	 * {@code UjianPSBAction.onSave()}. Menyimpan string kosong tidak efektif: pembacaan berikutnya
	 * akan mengembalikan (dan menuliskan kembali) teks bawaan.</p>
	 *
	 * @param keterangan teks informasi kartu pembayaran
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan jumlah hari pelaksanaan ujian (1&ndash;10).
	 *
	 * <p><b>Efek samping:</b> bila field {@code null}, method menulis nilai {@code 1} ke field
	 * (bukan sekadar mengembalikannya), sehingga nilai itu ikut tersimpan pada flush berikutnya.</p>
	 *
	 * <p><b>Kepentingan khusus:</b> angka ini adalah ambang yang dipakai
	 * {@link #getTanggalUjian2()}&hellip;{@link #getTanggalUjian10()} untuk <b>menghapus</b> tanggal
	 * di luar jangkauan. Di layar master, mengubah {@code Intbox} "Jumlah Hari Ujian" hanya
	 * menyembunyikan/menampilkan baris tanggal; penghapusan datanya terjadi di entity ini.</p>
	 *
	 * @return jumlah hari ujian; tidak pernah {@code null} setelah pemanggilan ini
	 */
	public Integer getJumlahHariUjian() {
		if (jumlahHariUjian == null) {
			jumlahHariUjian = 1;
		}
		return jumlahHariUjian;
	}

	/**
	 * Menetapkan jumlah hari pelaksanaan ujian.
	 *
	 * <p><b>Peringatan:</b> menurunkan nilai ini setara dengan memerintahkan penghapusan permanen
	 * tanggal hari-hari di atasnya pada pembacaan/flush berikutnya. Tidak ada validasi batas atas
	 * 10 di sini maupun di {@code UjianPSBAction.onSave()}; nilai &gt; 10 hanya berarti tidak ada
	 * kolom tanggal yang tersedia untuk hari ke-11 dan seterusnya.</p>
	 *
	 * @param jumlahHariUjian jumlah hari ujian; {@code null} akan dinormalkan menjadi 1 saat dibaca
	 */
	public void setJumlahHariUjian(Integer jumlahHariUjian) {
		this.jumlahHariUjian = jumlahHariUjian;
	}

	/**
	 * Mengembalikan tanggal hari ujian ke-1.
	 *
	 * <p>Satu-satunya tanggal yang <b>tidak</b> pernah dinolkan otomatis, karena
	 * {@link #getJumlahHariUjian()} minimal bernilai 1.</p>
	 *
	 * @return tanggal hari ujian ke-1, atau {@code null} bila belum dijadwalkan
	 */
	public Date getTanggalUjian1() {
		return tanggalUjian1;
	}

	/**
	 * Menetapkan tanggal hari ujian ke-1.
	 *
	 * @param tanggalUjian1 tanggal hari ujian ke-1
	 */
	public void setTanggalUjian1(Date tanggalUjian1) {
		this.tanggalUjian1 = tanggalUjian1;
	}

	/**
	 * Mengembalikan tanggal hari ujian ke-2.
	 *
	 * <p><b>Efek samping destruktif:</b> bila {@link #getJumlahHariUjian()} &lt; 2, field
	 * <b>ditimpa {@code null}</b> sebelum dikembalikan. Nilai lama hilang permanen begitu perubahan
	 * ini ter-flush; menaikkan kembali jumlah hari tidak memulihkannya.</p>
	 *
	 * @return tanggal hari ujian ke-2, atau {@code null} bila belum dijadwalkan atau di luar
	 *         jangkauan {@link #getJumlahHariUjian()}
	 */
	public Date getTanggalUjian2() {
		if (getJumlahHariUjian() < 2) {
			tanggalUjian2 = null;
		}
		return tanggalUjian2;
	}

	/**
	 * Menetapkan tanggal hari ujian ke-2. Nilai hanya bertahan bila
	 * {@link #getJumlahHariUjian()} &ge; 2.
	 *
	 * @param tanggalUjian2 tanggal hari ujian ke-2
	 */
	public void setTanggalUjian2(Date tanggalUjian2) {
		this.tanggalUjian2 = tanggalUjian2;
	}

	/**
	 * Mengembalikan tanggal hari ujian ke-3.
	 *
	 * <p><b>Efek samping destruktif:</b> field ditimpa {@code null} bila
	 * {@link #getJumlahHariUjian()} &lt; 3 — lihat {@link #getTanggalUjian2()}.</p>
	 *
	 * @return tanggal hari ujian ke-3, atau {@code null}
	 */
	public Date getTanggalUjian3() {
		if (getJumlahHariUjian() < 3) {
			tanggalUjian3 = null;
		}
		return tanggalUjian3;
	}

	/**
	 * Menetapkan tanggal hari ujian ke-3. Nilai hanya bertahan bila
	 * {@link #getJumlahHariUjian()} &ge; 3.
	 *
	 * @param tanggalUjian3 tanggal hari ujian ke-3
	 */
	public void setTanggalUjian3(Date tanggalUjian3) {
		this.tanggalUjian3 = tanggalUjian3;
	}

	/**
	 * Mengembalikan tanggal hari ujian ke-4.
	 *
	 * <p><b>Efek samping destruktif:</b> field ditimpa {@code null} bila
	 * {@link #getJumlahHariUjian()} &lt; 4 — lihat {@link #getTanggalUjian2()}.</p>
	 *
	 * @return tanggal hari ujian ke-4, atau {@code null}
	 */
	public Date getTanggalUjian4() {
		if (getJumlahHariUjian() < 4) {
			tanggalUjian4 = null;
		}
		return tanggalUjian4;
	}

	/**
	 * Menetapkan tanggal hari ujian ke-4. Nilai hanya bertahan bila
	 * {@link #getJumlahHariUjian()} &ge; 4.
	 *
	 * @param tanggalUjian4 tanggal hari ujian ke-4
	 */
	public void setTanggalUjian4(Date tanggalUjian4) {
		this.tanggalUjian4 = tanggalUjian4;
	}

	/**
	 * Mengembalikan tanggal hari ujian ke-5.
	 *
	 * <p><b>Efek samping destruktif:</b> field ditimpa {@code null} bila
	 * {@link #getJumlahHariUjian()} &lt; 5 — lihat {@link #getTanggalUjian2()}.</p>
	 *
	 * @return tanggal hari ujian ke-5, atau {@code null}
	 */
	public Date getTanggalUjian5() {
		if (getJumlahHariUjian() < 5) {
			tanggalUjian5 = null;
		}
		return tanggalUjian5;
	}

	/**
	 * Menetapkan tanggal hari ujian ke-5. Nilai hanya bertahan bila
	 * {@link #getJumlahHariUjian()} &ge; 5.
	 *
	 * @param tanggalUjian5 tanggal hari ujian ke-5
	 */
	public void setTanggalUjian5(Date tanggalUjian5) {
		this.tanggalUjian5 = tanggalUjian5;
	}

	/**
	 * Mengembalikan tanggal hari ujian ke-6.
	 *
	 * <p><b>Efek samping destruktif:</b> field ditimpa {@code null} bila
	 * {@link #getJumlahHariUjian()} &lt; 6 — lihat {@link #getTanggalUjian2()}.</p>
	 *
	 * @return tanggal hari ujian ke-6, atau {@code null}
	 */
	public Date getTanggalUjian6() {
		if (getJumlahHariUjian() < 6) {
			tanggalUjian6 = null;
		}
		return tanggalUjian6;
	}

	/**
	 * Menetapkan tanggal hari ujian ke-6. Nilai hanya bertahan bila
	 * {@link #getJumlahHariUjian()} &ge; 6.
	 *
	 * @param tanggalUjian6 tanggal hari ujian ke-6
	 */
	public void setTanggalUjian6(Date tanggalUjian6) {
		this.tanggalUjian6 = tanggalUjian6;
	}

	/**
	 * Mengembalikan tanggal hari ujian ke-7.
	 *
	 * <p><b>Efek samping destruktif:</b> field ditimpa {@code null} bila
	 * {@link #getJumlahHariUjian()} &lt; 7 — lihat {@link #getTanggalUjian2()}.</p>
	 *
	 * @return tanggal hari ujian ke-7, atau {@code null}
	 */
	public Date getTanggalUjian7() {
		if (getJumlahHariUjian() < 7) {
			tanggalUjian7 = null;
		}
		return tanggalUjian7;
	}

	/**
	 * Menetapkan tanggal hari ujian ke-7. Nilai hanya bertahan bila
	 * {@link #getJumlahHariUjian()} &ge; 7.
	 *
	 * @param tanggalUjian7 tanggal hari ujian ke-7
	 */
	public void setTanggalUjian7(Date tanggalUjian7) {
		this.tanggalUjian7 = tanggalUjian7;
	}

	/**
	 * Mengembalikan tanggal hari ujian ke-8.
	 *
	 * <p><b>Efek samping destruktif:</b> field ditimpa {@code null} bila
	 * {@link #getJumlahHariUjian()} &lt; 8 — lihat {@link #getTanggalUjian2()}.</p>
	 *
	 * @return tanggal hari ujian ke-8, atau {@code null}
	 */
	public Date getTanggalUjian8() {
		if (getJumlahHariUjian() < 8) {
			tanggalUjian8 = null;
		}
		return tanggalUjian8;
	}

	/**
	 * Menetapkan tanggal hari ujian ke-8. Nilai hanya bertahan bila
	 * {@link #getJumlahHariUjian()} &ge; 8.
	 *
	 * @param tanggalUjian8 tanggal hari ujian ke-8
	 */
	public void setTanggalUjian8(Date tanggalUjian8) {
		this.tanggalUjian8 = tanggalUjian8;
	}

	/**
	 * Mengembalikan tanggal hari ujian ke-9.
	 *
	 * <p><b>Efek samping destruktif:</b> field ditimpa {@code null} bila
	 * {@link #getJumlahHariUjian()} &lt; 9 — lihat {@link #getTanggalUjian2()}.</p>
	 *
	 * @return tanggal hari ujian ke-9, atau {@code null}
	 */
	public Date getTanggalUjian9() {
		if (getJumlahHariUjian() < 9) {
			tanggalUjian9 = null;
		}
		return tanggalUjian9;
	}

	/**
	 * Menetapkan tanggal hari ujian ke-9. Nilai hanya bertahan bila
	 * {@link #getJumlahHariUjian()} &ge; 9.
	 *
	 * @param tanggalUjian9 tanggal hari ujian ke-9
	 */
	public void setTanggalUjian9(Date tanggalUjian9) {
		this.tanggalUjian9 = tanggalUjian9;
	}

	/**
	 * Mengembalikan tanggal hari ujian ke-10 (hari terakhir yang dapat ditampung skema ini).
	 *
	 * <p><b>Efek samping destruktif:</b> field ditimpa {@code null} bila
	 * {@link #getJumlahHariUjian()} &lt; 10 — lihat {@link #getTanggalUjian2()}.</p>
	 *
	 * @return tanggal hari ujian ke-10, atau {@code null}
	 */
	public Date getTanggalUjian10() {
		if (getJumlahHariUjian() < 10) {
			tanggalUjian10 = null;
		}
		return tanggalUjian10;
	}

	/**
	 * Menetapkan tanggal hari ujian ke-10. Nilai hanya bertahan bila
	 * {@link #getJumlahHariUjian()} &ge; 10.
	 *
	 * @param tanggalUjian10 tanggal hari ujian ke-10
	 */
	public void setTanggalUjian10(Date tanggalUjian10) {
		this.tanggalUjian10 = tanggalUjian10;
	}

	/**
	 * Mengembalikan tahun akademik penerimaan (mis. {@code "2026/2027"}).
	 *
	 * <p><b>Nilai turunan dengan efek samping.</b> Method memanggil
	 * {@link #getGelombangPendaftaranPsb()} lebih dulu (memaksa resolusi proxy lazy lewat
	 * {@code check(...)}), lalu — bila gelombang ada — <b>menimpa</b> field dengan
	 * {@code gelombangPendaftaranPsb.getTahunAjaran()}. Karena pemetaan memakai property access,
	 * salinan itu ikut tersimpan ke kolom {@code tahunakademik}. Bila gelombang {@code null}, nilai
	 * lama dipertahankan apa adanya.</p>
	 *
	 * <p><b>Mengapa kolom ini penting:</b> {@code CommonReportPsb} menyaring jadwal ujian dengan
	 * {@code Restrictions.eq("ujianPSB.tahunAkademik", <tahun terpilih>)}. Jadi salinan yang basi
	 * (mis. karena tahun ajaran gelombang diubah tanpa baris ujian pernah disentuh aplikasi)
	 * membuat daftar absensi/album peserta tampak kosong tanpa pesan kesalahan.</p>
	 *
	 * @return tahun akademik penerimaan, atau {@code null} bila gelombang belum ditetapkan dan
	 *         field belum pernah diisi
	 */
	public String getTahunAkademik() {
		getGelombangPendaftaranPsb();
		if (gelombangPendaftaranPsb != null) {
			tahunAkademik = gelombangPendaftaranPsb.getTahunAjaran();
		}
		return tahunAkademik;
	}

	/**
	 * Menetapkan tahun akademik penerimaan secara manual.
	 *
	 * <p>Nilai yang diisi di sini bersifat sementara: {@link #getTahunAkademik()} akan menimpanya
	 * dengan tahun ajaran gelombang pada pembacaan berikutnya selama
	 * {@link #getGelombangPendaftaranPsb()} tidak {@code null}. Tidak ada pemanggil aplikatif;
	 * setter ini praktis hanya dipakai Hibernate saat memuat baris.</p>
	 *
	 * @param tahunAkademik tahun akademik dalam format {@code "YYYY/YYYY"}
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengembalikan tahun awal penerimaan sebagai bilangan (mis. {@code 2026} dari
	 * {@code "2026/2027"}).
	 *
	 * <p><b>Efek samping:</b> field {@link #tahun} ditulis ulang dari hasil penguraian.</p>
	 *
	 * <p><b>Catatan kehati-hatian:</b> method membaca <i>field</i> {@link #tahunAkademik} secara
	 * langsung, bukan lewat {@link #getTahunAkademik()}. Bila entity belum pernah dibaca melalui
	 * getter tahun akademik, field bisa masih {@code null} sehingga nilai lama dikembalikan tanpa
	 * penyegaran. Sebaliknya, bila field berisi teks yang tidak berformat {@code "YYYY/..."},
	 * {@code Integer.parseInt} melempar {@code NumberFormatException} — dan karena Hibernate
	 * memanggil getter ini saat flush, kegagalan tersebut dapat menggagalkan penyimpanan baris.
	 * Tidak ada pemanggil aplikatif yang ditemukan; kolomnya hanya dipelihara oleh siklus
	 * persistensi.</p>
	 *
	 * @return tahun awal penerimaan, atau {@code null} bila belum pernah terisi
	 */
	public Integer getTahun() {
		if (tahunAkademik != null) {
			tahun = Integer.parseInt(tahunAkademik.split("/")[0]);
		}
		return tahun;
	}

	/**
	 * Menetapkan tahun awal penerimaan. Akan ditimpa oleh {@link #getTahun()} bila
	 * {@link #tahunAkademik} terisi.
	 *
	 * @param tahun tahun awal penerimaan
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengembalikan gelombang pendaftaran PSB pemilik gelaran ujian ini.
	 *
	 * <p>Relasi {@code @ManyToOne} lazy dengan cascade {@code PERSIST}/{@code MERGE} ke kolom
	 * {@code gelombang_pendaftaran_psb}. Kolomnya {@code nullable = true} pada tingkat pemetaan,
	 * namun {@code UjianPSBAction.onSave()} mewajibkan pengguna memilih gelombang sebelum menyimpan;
	 * baris tanpa gelombang hanya bisa muncul dari data lama atau penyuntingan langsung di DB.</p>
	 *
	 * <p><b>Efek samping:</b> nilai dilewatkan {@code check(...)} milik
	 * {@link ais.database.model.GeneralValueObject} lalu ditulis kembali ke field. Rutin itu
	 * meresolusi proxy lazy/menyamakan instance kanonik antar session, sehingga getter ini dapat
	 * memicu query ke basis data pada pemanggilan pertama.</p>
	 *
	 * <p><b>Peran tenant:</b> ini satu-satunya sumber cakupan sekolah/yayasan bagi entity ini —
	 * {@code GelombangPendaftaranPsb} yang memiliki kolom {@code sekolah}, bukan {@code UjianPSB}.
	 * Tidak ada layar yang menegakkan batas tersebut.</p>
	 *
	 * @return gelombang pendaftaran pemilik, atau {@code null} untuk baris tanpa induk
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gelombang_pendaftaran_psb", nullable = true)
	public GelombangPendaftaranPsb getGelombangPendaftaranPsb() {
		gelombangPendaftaranPsb = check(gelombangPendaftaranPsb);
		return gelombangPendaftaranPsb;
	}

	/**
	 * Menetapkan gelombang pendaftaran PSB pemilik.
	 *
	 * <p>Diisi dari combo "Gelombang Pendaftaran" pada dialog simpan, dan oleh
	 * {@code GelombangPendaftaranPsb.chekKuotaPendaftar()} saat menyemai ujian {@code "Online"}.
	 * Mengubah nilai ini otomatis mengubah {@link #getTahunAkademik()} pada pembacaan berikutnya.</p>
	 *
	 * @param gelombangPendaftaranPsb gelombang pemilik
	 */
	public void setGelombangPendaftaranPsb(GelombangPendaftaranPsb gelombangPendaftaranPsb) {
		this.gelombangPendaftaranPsb = gelombangPendaftaranPsb;
	}

	/**
	 * Mengembalikan lokasi penyelenggaraan ujian sebagai teks bebas.
	 *
	 * <p>Wajib diisi menurut validasi {@code UjianPSBAction.onSave()}; baris hasil semai otomatis
	 * memakai nilai {@code "Sekolah"}. Dicetak apa adanya oleh template kartu pembayaran dan kartu
	 * ujian (kolom {@code f.lokasi}) — tidak ada relasi ke entity {@code Gedung}/{@code Ruang}.</p>
	 *
	 * @return lokasi ujian, atau {@code null} bila belum diisi
	 */
	public String getLokasi() {
		return lokasi;
	}

	/**
	 * Menetapkan lokasi penyelenggaraan ujian.
	 *
	 * @param lokasi lokasi ujian dalam bentuk teks bebas
	 */
	public void setLokasi(String lokasi) {
		this.lokasi = lokasi;
	}

	/**
	 * Mengembalikan sakelar "tampilkan informasi ujian di kartu peserta".
	 *
	 * <p><b>Efek samping:</b> bila field {@code null}, method <b>menulis {@code true}</b> ke field
	 * sebelum mengembalikannya — jadi perilaku bawaan adalah "tampilkan", dan nilai itu ikut
	 * tersimpan pada flush berikutnya.</p>
	 *
	 * <p>Dibaca sebagai kolom {@code f.tampilkanjadwalujiandikartuujian} oleh
	 * {@code KartuUjianSpsbMandiri.jrxml} dan {@code KartuBayarPsbMandiri.jrxml}, serta dirender
	 * sebagai kolom "Info" ("Ya"/"Tidak") di grid layar master.</p>
	 *
	 * @return {@code true} bila blok jadwal ujian ditampilkan pada kartu; tidak pernah {@code null}
	 *         setelah pemanggilan ini
	 */
	public Boolean getTampilkanJadwalUjianDiKartuUjian() {
		if (tampilkanJadwalUjianDiKartuUjian == null) {
			tampilkanJadwalUjianDiKartuUjian = true;
		}
		return tampilkanJadwalUjianDiKartuUjian;
	}

	/**
	 * Menetapkan sakelar tampil informasi ujian di kartu peserta.
	 *
	 * <p>Diisi dari checkbox "Tampilkan Info Ujian Di Kartu Peserta" pada dialog simpan.</p>
	 *
	 * @param tampilkanJadwalUjianDiKartuUjian {@code true} untuk menampilkan blok jadwal di kartu
	 */
	public void setTampilkanJadwalUjianDiKartuUjian(Boolean tampilkanJadwalUjianDiKartuUjian) {
		this.tampilkanJadwalUjianDiKartuUjian = tampilkanJadwalUjianDiKartuUjian;
	}

	/**
	 * Mengembalikan teks informasi untuk <b>kartu ujian</b> (dicetak setelah kewajiban pembayaran
	 * terpenuhi).
	 *
	 * <p><b>Efek samping — getter yang menulis.</b> Bila field {@code null} atau berisi spasi saja,
	 * field diisi teks bawaan {@code "1. Alat Tuilis\n2. Papan Ujian"} — daftar barang bawaan
	 * peserta, lengkap dengan salah ketik <i>Tuilis</i> yang ikut tercetak. Isian tersebut tertulis
	 * ke basis data pada flush berikutnya.</p>
	 *
	 * <p>Template {@code KartuUjianSpsbMandiri.jrxml} membaca kolom
	 * {@code f.keterangansetelahbayar} langsung sebagai field {@code info}.</p>
	 *
	 * @return teks informasi kartu ujian; tidak pernah {@code null} setelah pemanggilan ini
	 */
	@Column(columnDefinition = "text")
	public String getKeteranganSetelahBayar() {
		if (keteranganSetelahBayar == null) {
			keteranganSetelahBayar = "";
		}
		if (keteranganSetelahBayar.trim().isEmpty()) {
			keteranganSetelahBayar = "1. Alat Tuilis\n2. Papan Ujian";
		}

		return keteranganSetelahBayar;
	}

	/**
	 * Menetapkan teks informasi kartu ujian.
	 *
	 * <p>Diisi dari textarea "Informasi ke peserta ujian pada kartu Ujian" di
	 * {@code UjianPSBAction.onSave()}. Menyimpan string kosong tidak efektif: pembacaan berikutnya
	 * mengembalikan (dan menuliskan kembali) teks bawaan.</p>
	 *
	 * @param keteranganSetelahBayar teks informasi kartu ujian
	 */
	public void setKeteranganSetelahBayar(String keteranganSetelahBayar) {
		this.keteranganSetelahBayar = keteranganSetelahBayar;
	}

	/**
	 * Mengembalikan judul yang dimaksudkan sebagai kepala blok {@link #getKeterangan()}.
	 *
	 * <p><b>Efek samping:</b> bila field {@code null}, diisi teks bawaan {@code "Pastikan bahwa data
	 * dibawah ini adalah benar data diri anda."} dan nilai itu tersimpan pada flush berikutnya.
	 * Berbeda dari saudara-saudaranya, pemeriksaannya hanya {@code null} — string kosong hasil
	 * penyuntingan pengguna tetap dipertahankan.</p>
	 *
	 * <p><b>Tanpa pemakai:</b> penelusuran menyeluruh atas kode Java, ZUL, JSP, dan seluruh
	 * {@code *.jrxml} tidak menemukan pembaca properti ini; textbox-nya di {@code UjianPSBAction}
	 * bahkan dikomentari. Nilai yang disimpan tidak pernah tampil di layar maupun di kartu.</p>
	 *
	 * @return judul blok keterangan kartu pembayaran; tidak pernah {@code null} setelah pemanggilan
	 *         ini
	 */
	@Column(columnDefinition = "text")
	public String getKeteranganHeader() {
		if (keteranganHeader == null) {
			keteranganHeader = "Pastikan bahwa data dibawah ini adalah benar data diri anda.";
		}
		return keteranganHeader;
	}

	/**
	 * Menetapkan judul blok keterangan kartu pembayaran. Tidak ada pemanggil aplikatif; lihat
	 * {@link #getKeteranganHeader()}.
	 *
	 * @param keteranganHeader judul blok keterangan
	 */
	public void setKeteranganHeader(String keteranganHeader) {
		this.keteranganHeader = keteranganHeader;
	}

	/**
	 * Mengembalikan judul yang dimaksudkan sebagai kepala blok
	 * {@link #getKeteranganSetelahBayar()}.
	 *
	 * <p><b>Efek samping:</b> sama dengan {@link #getKeteranganHeader()} — field diisi teks bawaan
	 * {@code "Pastikan bahwa data dibawah ini adalah benar data diri anda."} bila {@code null}.</p>
	 *
	 * <p><b>Tanpa pemakai</b>, persis seperti pasangannya; lihat {@link #getKeteranganHeader()}.</p>
	 *
	 * @return judul blok keterangan kartu ujian; tidak pernah {@code null} setelah pemanggilan ini
	 */
	@Column(columnDefinition = "text")
	public String getKeteranganSetelahBayarHeader() {
		if (keteranganSetelahBayarHeader == null) {
			keteranganSetelahBayarHeader = "Pastikan bahwa data dibawah ini adalah benar data diri anda.";
		}
		return keteranganSetelahBayarHeader;
	}

	/**
	 * Menetapkan judul blok keterangan kartu ujian. Tidak ada pemanggil aplikatif; lihat
	 * {@link #getKeteranganSetelahBayarHeader()}.
	 *
	 * @param keteranganSetelahBayarHeader judul blok keterangan kartu ujian
	 */
	public void setKeteranganSetelahBayarHeader(String keteranganSetelahBayarHeader) {
		this.keteranganSetelahBayarHeader = keteranganSetelahBayarHeader;
	}

}
