package ais.database.model.akunting;

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



import ais.database.model.GeneralValueObject;

/**
 * Baris rincian <b>laporan keuangan per tahun</b> yang sudah dihitung sebelumnya (pra-agregasi):
 * satu baris menyimpan pasangan nilai debet dan kredit sebuah {@link Akun} untuk satu
 * {@code tahun}, terpecah menjadi <b>13 slot</b> ({@code 0}&ndash;{@code 12}), opsional disegmen
 * per {@link Devisi}. Tabel: {@code akunting.detail_laporan_pertahun}.
 *
 * <h2>PERINGATAN UTAMA &mdash; entity ini TIDUR / YATIM (terverifikasi)</h2>
 * <p>Penelusuran menyeluruh seluruh repositori (berkas {@code .java}, {@code .zul}, {@code .jsp},
 * {@code .xml}, {@code .jrxml}, {@code .sql}) menemukan <b>nol pembaca dan nol penulis</b> untuk
 * kelas maupun tabel ini. Satu-satunya rujukan yang ada adalah:</p>
 * <ol>
 *   <li>pendaftaran pemetaan di {@code hibernate.cfg.xml}
 *       (<code>&lt;mapping class="ais.database.model.akunting.DetailLaporanPertahun"/&gt;</code>)
 *       &mdash; jadi tabelnya tetap dibuat/divalidasi saat aplikasi start, tetapi tidak pernah
 *       diisi maupun dibaca oleh kode aplikasi;</li>
 *   <li>satu baris inventaris di
 *       {@code WEB-INF/generic-crud/manifests/general_value_object_inventory.csv} dengan
 *       kelayakan {@code ELIGIBLE_METADATA_FIRST} dan catatan <i>"Kandidat CRUD metadata; tetap
 *       default disabled sampai verifikasi Hibernate/menu/scope"</i>. Belum ada adapter
 *       {@code *GenericCrudAdapter} untuk kelas ini, sehingga layar CRUD generiknya <b>belum
 *       aktif</b>.</li>
 * </ol>
 * <p>Tidak ada {@code Action}, {@code Helper}, DAO, servlet REST, layar ZK, JSP, maupun template
 * JasperReports yang menyentuhnya. Konsekuensinya: <b>seluruh uraian di bawah tentang "apa yang
 * terjadi bila entity ini dipakai" bersifat laten</b> &mdash; tidak ada satu pun jalur produksi
 * yang bisa memicunya hari ini. Ini adalah entity tidur keempat yang ditemukan di inisiatif ini
 * (setelah {@code PengumumanJadwalPelajaran}, {@link TemplateTransaksi}, dan
 * {@link TemplateGrupTransaksi}).</p>
 *
 * <h2>Mesin laporan tahunan yang BENAR-BENAR hidup</h2>
 * <p>Laporan keuangan 12 bulan yang sungguh dipakai <b>tidak</b> membaca tabel ini. Yang hidup
 * adalah layar {@code ais.action.report.format1.akunting.LaporanAkunting12Bulan} yang mencetak
 * template JasperReports {@code report/akunting/laporan_keuangan_12_bulan.jrxml}. Template itu
 * menghitung <b>langsung dari jurnal, saat laporan dicetak</b>, dengan pola:</p>
 * <ul>
 *   <li>sumber baris: {@link Transaksi} ({@code akunting.transaksi}) di-join ke
 *       {@link GrupTransaksi} ({@code akunting.grup_transaksi});</li>
 *   <li>pemetaan akun ke baris laporan lewat rantai
 *       {@link KelompokLaporanPunyaAkun} &rarr; {@link KelompokLaporan} &rarr;
 *       {@link MasterGrupLaporan} &rarr; {@link JenisLaporan} (Neraca / Laba&nbsp;Rugi /
 *       Arus&nbsp;Kas);</li>
 *   <li>nilai tiap kolom bulan dihitung
 *       {@code sum(case when tanggal_transaksi <= <akhir bulan ke-n> then (debet-kredit) else 0 end)}
 *       &mdash; artinya <b>kumulatif sejak awal data sampai akhir bulan ke-n</b>, bukan mutasi
 *       bulan berjalan, dan sudah <b>dipadatkan menjadi satu angka bertanda</b>;</li>
 *   <li>hanya jurnal yang sudah distempel posting yang ikut
 *       ({@code grup_transaksi.posting_history is not null});</li>
 *   <li>penyaring {@code kelompok}, {@code grup}, {@code nama}, {@code satuan_kerja}, dan
 *       {@code jenis_laporan} semuanya <b>opsional</b>: layar mengirim {@code -1} bila pilihan
 *       dikosongkan, dan SQL menerjemahkan {@code -1} menjadi "tanpa saringan". Default layar
 *       adalah kosong, sehingga laporan tahunan default mencakup <b>seluruh satuan kerja</b>.</li>
 * </ul>
 * <p>Jadi arsitektur yang berjalan adalah <b>hitung-saat-cetak</b> (live), sedangkan kelas ini
 * adalah rancangan alternatif <b>materialisasi/pra-hitung</b> yang tidak pernah dirampungkan.
 * Perbedaan bentuk datanya menegaskan hal itu: laporan hidup memakai <b>satu nilai bertanda</b>
 * per bulan ({@code debet-kredit}), sedangkan kelas ini menyimpan <b>debet dan kredit terpisah</b>
 * (pola yang sama dengan {@link Transaksi} dan {@link SaldoAwalAkun}, yaitu dua kolom nominal
 * tak-bertanda, bukan satu kolom bertanda).</p>
 *
 * <h2>Bentuk data: 13 slot, bukan 12 bulan</h2>
 * <p>Terdapat 26 kolom nominal berpasangan: {@code nilai_d_0}&hellip;{@code nilai_d_12} dan
 * {@code nilai_k_0}&hellip;{@code nilai_k_12}. Karena satu tahun hanya punya 12 bulan, indeks
 * {@code 0} jelas merupakan slot ke-13 yang <b>bukan bulan</b> &mdash; secara konvensi akuntansi
 * biasanya berarti <i>saldo awal / saldo pembuka tahun</i> (bandingkan {@link SaldoAwalAkun}),
 * sehingga {@code 1}&hellip;{@code 12} memetakan Januari&hellip;Desember. <b>Namun konvensi ini
 * tidak dapat dibuktikan dari kode:</b> karena tidak ada satu pun penulis maupun pembaca, tidak
 * ada tempat di repositori yang menetapkan makna indeks. Interpretasi lain yang sama masuk akal
 * (mis. {@code 0} = penyesuaian/pra-tutup-buku, atau {@code 12} = kolom penutup dan
 * {@code 0}&hellip;{@code 11} = Januari&hellip;Desember) tidak bisa disingkirkan. Siapa pun yang
 * menghidupkan entity ini <b>wajib menetapkan dan mendokumentasikan konvensi indeks lebih dulu</b>;
 * salah tafsir satu slot menggeser seluruh kolom laporan sebesar satu bulan.</p>
 *
 * <h2>Risiko integritas bila entity ini kelak dihidupkan</h2>
 * <p>Seluruh butir berikut adalah <b>cacat rancangan laten</b> pada skema, bukan bug aktif:</p>
 * <ol>
 *   <li><b>Tidak ada kolom cakupan tenant sama sekali.</b> Tidak ada {@code sekolah},
 *       {@code yayasan}, maupun {@code satuanKerja} di kelas ini &mdash; ini <b>bukan</b> kasus
 *       fail-open bersyarat (penyaring yang bisa dilewati), melainkan ketiadaan sumbu cakupan.
 *       Kunci logis satu baris hanyalah {@code (tahun, akun, devisi)}. Cakupan hanya bisa
 *       disimpulkan <b>tidak langsung</b> lewat {@code akun.satuan_kerja}, padahal kolom
 *       {@code akun} di sini {@code nullable} dan {@code Akun.satuanKerja} sendiri juga opsional.
 *       Perlu dicatat bahwa ini konsisten dengan buku besarnya: {@link GrupTransaksi} pun tidak
 *       punya kolom {@code sekolah}/{@code yayasan}, sehingga jurnal AIS memang satu buku besar
 *       bersama dengan {@code satuanKerja} sebagai satu-satunya sumbu pemisah.</li>
 *   <li><b>Tidak ada batasan unik.</b> Tidak ada {@code @UniqueConstraint} maupun indeks unik atas
 *       {@code (tahun, akun, devisi)}. Untuk tabel <b>agregat</b>, baris kembar bukan sekadar
 *       kotor: sebuah pembangkit ulang yang gagal separuh, atau dua permintaan pembangkitan yang
 *       berjalan bersamaan, akan meninggalkan dua baris untuk akun+tahun yang sama dan laporan
 *       yang menjumlahkannya akan <b>menggandakan angka</b>. Pola TOCTOU yang sama sudah
 *       terkonfirmasi di beberapa entity lain paket ini.</li>
 *   <li><b>Tidak ada kaitan ke {@link PostingHistory} maupun {@link Closing}.</b> Tidak ada cara
 *       mengetahui angka pra-hitung ini disusun dari keadaan jurnal yang mana. Bila jurnal
 *       kemudian ditambah, dibatalkan-posting, atau diposting ulang (lihat pola batal-posting
 *       pada dokumen-dokumen paket ini), baris di sini menjadi <b>basi secara diam-diam</b> tanpa
 *       penanda apa pun. Tidak ada kolom "dihitung pada" selain {@code tanggal_dirubah} yang
 *       maknanya sekadar "terakhir di-UPDATE".</li>
 *   <li><b>Tidak ada rekaman parameter pembangkitan.</b> Laporan hidup dibangkitkan dengan
 *       kombinasi penyaring (satuan kerja, jenis laporan, grup, kelompok). Tabel ini tidak
 *       menyimpan satu pun dari itu, sehingga dua pembangkitan dengan cakupan berbeda akan
 *       <b>saling menimpa atau saling menumpuk</b> pada kunci logis yang sama tanpa bisa
 *       dibedakan.</li>
 *   <li><b>Tidak ada penjaga keseimbangan.</b> Tidak ada validasi bahwa
 *       &Sigma;{@code nilai_d_*} = &Sigma;{@code nilai_k_*} untuk satu tahun, dan tidak ada
 *       validasi nilai negatif. Seluruh 26 setter menerima nilai apa adanya, termasuk negatif dan
 *       {@code null}.</li>
 *   <li><b>Cascade ke master.</b> Relasi {@link #getAkun() akun} dan {@link #getDevisi() devisi}
 *       memakai {@code cascade = {PERSIST, MERGE}}. Menyimpan sebuah baris laporan dengan instance
 *       {@link Akun} atau {@link Devisi} yang sudah diubah di memori akan <b>ikut menulis balik
 *       master bagan akun / divisi</b> &mdash; jalur tulis tak terduga dari sisi "laporan" ke sisi
 *       "master". Pola cascade ini seragam di paket {@code akunting} dan bukan kekhususan kelas
 *       ini, tetapi pada tabel pelaporan efeknya paling tidak diharapkan.</li>
 * </ol>
 *
 * <h2>Verifikasi risiko nilai/tanda salah (pola {@code debit_credit = 2})</h2>
 * <p>Diperiksa khusus terhadap pola kerusakan laporan keuangan yang dikenal pada
 * {@link Akun#getDebetCredit() Akun.debit_credit} (nilai {@code 2} tertulis untuk akun bersaldo
 * normal kredit, padahal sandi yang benar {@code -1}; di
 * {@code ais.action.master.koperasi.helper.LaporanKeuanganCoaHelper} saldo dikalikan sandi itu,
 * sehingga nilai cetak menjadi dua kali lipat dan terbalik tanda). Hasil verifikasi:</p>
 * <ul>
 *   <li><b>Pada lapisan penyimpanan: TIDAK rentan.</b> Kelas ini menyimpan debet dan kredit di
 *       <b>kolom terpisah</b> dan <b>tidak pernah mengalikan apa pun dengan {@code debit_credit}</b>
 *       &mdash; tidak ada satu pun operasi aritmetika di seluruh berkas. Angka yang masuk adalah
 *       angka yang keluar. Karena itu baris di tabel ini tidak bisa rusak oleh sandi saldo normal
 *       yang salah.</li>
 *   <li><b>Pada lapisan penyajian: TETAP TERPAPAR bila entity dihidupkan.</b> Laporan Neraca /
 *       Laba&nbsp;Rugi harus menyajikan satu angka bertanda per akun. Konsumen mana pun yang kelak
 *       membaca tabel ini harus memadatkan pasangan {@code d}/{@code k} menjadi satu nilai
 *       bertanda, dan cara yang lazim di basis kode ini adalah mengalikan dengan
 *       {@code akun.getDebetCredit()} &mdash; persis pengali yang bisa bernilai {@code 2}. Jadi
 *       kerentanannya <b>berpindah, bukan hilang</b>. Cara aman yang sudah terbukti dipakai
 *       laporan 12 bulan yang hidup adalah memakai selisih langsung {@code (debet - kredit)}
 *       tanpa pengali sandi apa pun; pola itulah yang harus ditiru konsumen baru.</li>
 * </ul>
 *
 * <h2>Hak akses</h2>
 * <p>Hari ini <b>tidak ada gerbang hak sama sekali</b> untuk entity ini &mdash; bukan karena
 * gerbangnya bocor, melainkan karena <b>tidak ada permukaan</b> (tidak ada menu, layar, maupun
 * endpoint). Bila kelak diaktifkan lewat CRUD generik, gerbangnya akan mengikuti
 * {@code ais.action.master.generic.GenericCrudAction} yang memanggil
 * {@code CommonPrivilages.checkPrevilages(CREATE/UPDATE/DELETE)}. Perlu diingat bahwa mekanisme
 * itu membaca atribut sesi {@code currentMenu}, sehingga tunduk pada pola <b>pewarisan hak lewat
 * menu induk</b> yang sudah berulang kali terkonfirmasi di inisiatif ini: hak yang berlaku adalah
 * hak menu yang sedang aktif di sesi, bukan hak atas layar yang benar-benar sedang dibuka. Untuk
 * tabel yang isinya angka laporan keuangan resmi, konsekuensi salah-menu berarti kemampuan
 * menyunting angka laporan tanpa jejak niat.</p>
 *
 * <h2>Pengelompokan method</h2>
 * <ul>
 *   <li><b>Identitas &amp; teks</b> &mdash; {@link #getId()}/{@link #setId(Long)},
 *       {@link #getKeterangan()}/{@link #setKeterangan(String)}, {@link #toString()}.</li>
 *   <li><b>Jejak audit</b> &mdash; {@link #getOleh()}/{@link #setOleh(String)},
 *       {@link #getOlehId()}/{@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan kait daur hidup
 *       {@link #onUpdate()} ({@code @PreUpdate}).</li>
 *   <li><b>Kunci logis / dimensi</b> &mdash; {@link #getTahun()}/{@link #setTahun(Integer)},
 *       {@link #getAkun()}/{@link #setAkun(Akun)},
 *       {@link #getDevisi()}/{@link #setDevisi(Devisi)}.</li>
 *   <li><b>Nominal</b> &mdash; 26 pasang getter/setter
 *       {@code getNilai_d_0}&hellip;{@code getNilai_k_12}, seluruhnya getter/setter murni tanpa
 *       logika.</li>
 * </ul>
 * <p><b>Tidak ada method bisnis sama sekali</b> di kelas ini: tidak ada agregasi, tidak ada
 * kalkulasi, tidak ada validasi, tidak ada query. Kelas ini murni wadah data. Satu-satunya method
 * yang berperilaku non-trivial adalah {@link #getAkun()} (resolusi proxy dengan penugasan balik),
 * {@link #setOleh(String)}/{@link #setOlehId(String)} (menolak nilai kosong secara diam-diam), dan
 * {@link #toString()} (bisa mengembalikan {@code null}).</p>
 *
 * <h2>Kuirk teknis</h2>
 * <ul>
 *   <li><b>{@link #toString()} bisa mengembalikan {@code null}.</b> Ia mengembalikan
 *       {@code keterangan} apa adanya, sedangkan kolom itu {@code nullable = true}. Komponen ZK
 *       yang menurunkan label dari {@code toString()} akan menerima {@code null}; perangkai string
 *       akan mencetak teks {@code "null"}. Selain itu {@code keterangan} adalah pilihan penanda
 *       yang buruk untuk baris agregat &mdash; identitas alaminya adalah
 *       {@code tahun + akun + devisi}, bukan catatan bebas.</li>
 *   <li><b>{@link #setOleh(String)} dan {@link #setOlehId(String)} menolak nilai kosong secara
 *       diam-diam.</b> Bila argumennya {@code null} atau hanya spasi, method langsung
 *       {@code return} tanpa menugaskan apa pun, sehingga <b>nilai lama dipertahankan</b>. Jejak
 *       "diubah oleh" karena itu tidak pernah bisa dikosongkan, dan sebuah pembaruan yang lupa
 *       mengisi pengubah akan tampak seolah dilakukan oleh pengubah sebelumnya. Pola ini seragam
 *       di seluruh entity hasil generator {@code hbm2java} repositori ini.</li>
 *   <li><b>{@link #getAkun()} adalah getter dengan penugasan balik.</b> Ia memanggil
 *       {@code check(akun)} milik {@link GeneralValueObject} lalu <b>menugaskan hasilnya kembali ke
 *       field</b>. Ini resolusi proxy lazy yang wajar (tanpa itu relasi lazy pada objek detached
 *       akan gagal), <i>bukan</i> penimpaan destruktif seperti {@code Transaksi.getAkun()} yang
 *       menukar akun dengan akun lain: di sini entity pengganti selalu ber-id sama, sehingga
 *       Hibernate tidak menganggapnya perubahan dan tidak ada UPDATE liar meskipun kelas ini
 *       {@code dynamicUpdate = true}. Yang tetap perlu disadari: pada kasus terburuk
 *       {@code check()} membuka session/koneksi baru dari dalam jalur getter (lihat Javadoc
 *       {@link GeneralValueObject}), jadi membaca ribuan baris laporan satu per satu bisa jauh
 *       lebih mahal dari yang terlihat &mdash; muat relasi lewat satu query, jangan lewat
 *       getter.</li>
 *   <li><b>{@link #getDevisi()} tidak memanggil {@code check()}.</b> Tidak masalah karena relasi
 *       itu dipetakan eager ({@code @ManyToOne} tanpa {@code fetch = LAZY}, ditambah
 *       {@code @Fetch(FetchMode.SELECT)} yang hanya memisahkan SELECT-nya, bukan menundanya).
 *       Ketidakseragaman dengan {@link #getAkun()} semata cerminan bedanya strategi fetch.</li>
 *   <li><b>Nama kolom mengikuti nama properti apa adanya.</b> Hanya {@code id} dan
 *       {@code keterangan} yang ber-{@code @Column} eksplisit; sisanya (termasuk 26 kolom nominal
 *       dan {@code tahun}) mengandalkan penamaan default Hibernate, sehingga kolom fisiknya
 *       persis {@code nilai_d_0}, {@code nilai_k_0}, dan seterusnya. Mengubah nama getter berarti
 *       mengubah nama kolom &mdash; jangan "dirapikan" menjadi camelCase.</li>
 *   <li><b>{@code @Audited}.</b> Setiap versi baris digandakan ke tabel revisi
 *       {@code detail_laporan_pertahun_aud}. Untuk tabel agregat yang dibangkitkan ulang berkala,
 *       ini berarti pertumbuhan tabel revisi berlipat-lipat dari tabel utamanya; pertimbangkan hal
 *       itu sebelum menghidupkan entity ini pada instalasi besar.</li>
 *   <li><b>{@code serialVersionUID} bukan sidik jari kelas.</b> Nilai
 *       {@code 2463821577548439808L} dipakai bersama oleh puluhan entity lain hasil generator yang
 *       sama; ia tidak menandakan hubungan atau kembaran dengan kelas mana pun.</li>
 *   <li><b>Komentar kelas asli menyesatkan.</b> Berkas ini semula berkomentar
 *       <i>"Bank generated by hbm2java"</i> &mdash; sisa salin-tempel dari entity {@code Bank},
 *       tidak ada hubungannya dengan isi kelas.</li>
 * </ul>
 *
 * <h2>Pewarisan dari {@code GeneralValueObject}</h2>
 * <p>Kelas ini {@code extends} {@link ais.database.model.GeneralValueObject}. Induk tersebut
 * <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} &mdash; ia POJO abstrak biasa,
 * sehingga Hibernate <b>tidak</b> memetakan satu pun properti miliknya. Karena itu kolom seperti
 * {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} <b>wajib</b> dideklarasikan ulang di
 * sini; pengulangan itu keharusan teknis, bukan duplikasi yang perlu dibersihkan. Yang diwarisi
 * dan benar-benar dipakai adalah utilitas runtime-nya, khususnya {@code check()} pada
 * {@link #getAkun()}.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see Akun
 * @see Devisi
 * @see Transaksi
 * @see GrupTransaksi
 * @see SaldoAwalAkun
 * @see KelompokLaporan
 * @see MasterGrupLaporan
 * @see JenisLaporan
 * @see KelompokLaporanPunyaAkun
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "akunting", name = "detail_laporan_pertahun")



public class DetailLaporanPertahun extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Nilai {@code 2463821577548439808L} ini <b>bukan sidik jari kelas ini</b>: konstanta yang
	 * sama muncul di puluhan entity lain hasil generator {@code hbm2java} yang sama, sehingga
	 * kesamaan nilainya tidak menyiratkan hubungan, kembaran, maupun salin-tempel antar-kelas.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama baris ({@code akunting.detail_laporan_pertahun.id}), dibangkitkan basis data. */
	private Long id;

	/** Nama/identitas pengguna terakhir yang menyimpan baris ini (jejak audit ringan). */
	private String oleh;

	/** Id pengguna terakhir yang menyimpan baris ini (jejak audit ringan, disimpan sebagai teks). */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang menyimpan baris ini.
	 *
	 * <p>Getter murni tanpa efek samping. Nilainya diisi lapisan pemanggil; karena entity ini tidak
	 * punya pemanggil (lihat Javadoc kelas), pada praktiknya selalu {@code null}.</p>
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna terakhir yang menyimpan baris ini.
	 *
	 * <p><b>Menolak nilai kosong secara diam-diam:</b> bila {@code olehId} bernilai {@code null}
	 * atau hanya berisi spasi, method langsung {@code return} <b>tanpa menugaskan apa pun</b>,
	 * sehingga nilai lama tetap bertahan. Efeknya jejak audit tidak pernah bisa dikosongkan, dan
	 * penyimpanan yang lupa mengisi pengubah akan tampak dilakukan oleh pengubah sebelumnya. Pola
	 * ini seragam di seluruh entity hasil generator repositori ini &mdash; jangan "diperbaiki"
	 * sepihak di satu berkas.</p>
	 *
	 * @param olehId id pengguna pengubah; {@code null} atau string kosong/spasi diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengembalikan representasi teks baris ini, yaitu isi {@code keterangan} apa adanya.
	 *
	 * <p><b>Dapat mengembalikan {@code null}</b> karena kolom {@code keterangan} bersifat
	 * {@code nullable}: komponen ZK yang menurunkan labelnya dari {@code toString()} akan menerima
	 * {@code null}, dan perangkaian string akan mencetak teks {@code "null"}. Perlu dicatat juga
	 * bahwa {@code keterangan} adalah penanda yang lemah untuk baris agregat &mdash; identitas
	 * alami baris ini adalah kombinasi {@code tahun}, {@code akun}, dan {@code devisi}, bukan
	 * catatan bebas.</p>
	 *
	 * @return isi {@code keterangan}, atau {@code null} bila kolom itu kosong
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Menetapkan nama/identitas pengguna terakhir yang menyimpan baris ini.
	 *
	 * <p><b>Menolak nilai kosong secara diam-diam</b>, persis seperti
	 * {@link #setOlehId(String)}: argumen {@code null} atau berisi spasi saja diabaikan dan nilai
	 * lama dipertahankan.</p>
	 *
	 * @param oleh nama/identitas pengguna pengubah; {@code null} atau string kosong/spasi diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama/identitas pengguna terakhir yang menyimpan baris ini.
	 *
	 * <p>Getter murni tanpa efek samping.</p>
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup {@code @PreUpdate} plus deklarasi field {@code tanggal_dirubah} &mdash;
	 * keduanya sengaja berada pada satu baris fisik mengikuti pola generator repositori ini.
	 *
	 * <p>{@code onUpdate()} dipanggil Hibernate tepat sebelum setiap {@code UPDATE} baris ini dan
	 * mendelegasikan pembaruan stempel waktu ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}. Field
	 * {@code tanggal_dirubah} diinisialisasi ke waktu server saat objek dibuat
	 * ({@code WaktuUtil.getDate()}), lalu disegarkan oleh kait ini pada tiap pembaruan.</p>
	 *
	 * <p><b>Batasannya:</b> kait ini hanya menyala untuk UPDATE yang melewati Hibernate.
	 * Pembaruan lewat SQL mentah (pola yang dipakai beberapa helper posting di paket ini)
	 * melewatinya, sehingga {@code tanggal_dirubah} bisa tertinggal dan revisi Envers tidak
	 * terbentuk. Untuk tabel agregat yang kelak mungkin diisi lewat {@code INSERT ... SELECT}
	 * massal, keterbatasan ini penting diketahui.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Setter murni tanpa validasi. Normalnya tidak dipanggil kode aplikasi &mdash; nilainya
	 * diurus {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}. Memanggilnya manual
	 * berarti memalsukan stempel audit.</p>
	 *
	 * @param tanggal_dirubah stempel waktu perubahan; {@code null} diperbolehkan oleh setter ini
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Dipetakan sebagai {@code TIMESTAMP}. Maknanya sekadar "terakhir di-UPDATE lewat
	 * Hibernate", <b>bukan</b> "kapan angka laporan ini dihitung" &mdash; entity ini tidak punya
	 * kolom waktu pembangkitan tersendiri.</p>
	 *
	 * @return stempel waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Tahun buku yang dicakup baris ini (mis. {@code 2026}); bagian dari kunci logis
	 * {@code (tahun, akun, devisi)}. Tanpa {@code @Column} eksplisit, jadi kolom fisiknya
	 * {@code tahun}.
	 */
	private Integer tahun;

	/**
	 * Divisi/unit yang menjadi sumbu segmentasi baris ini (opsional). Bukan sumbu tenant &mdash;
	 * {@link Devisi} sendiri tidak punya kolom sekolah/yayasan.
	 */
	private Devisi devisi;

	/**
	 * Akun bagan akun yang nilainya diringkas baris ini (opsional pada level skema, meski secara
	 * logis wajib untuk sebuah baris laporan).
	 */
	private Akun akun;

	/** Nominal <b>debet</b> slot 0 (lihat pembahasan konvensi slot pada Javadoc kelas). */
	private Double nilai_d_0;
	/** Nominal <b>kredit</b> slot 0 (lihat pembahasan konvensi slot pada Javadoc kelas). */
	private Double nilai_k_0;
	/** Nominal <b>debet</b> slot 1. */
	private Double nilai_d_1;
	/** Nominal <b>kredit</b> slot 1. */
	private Double nilai_k_1;
	/** Nominal <b>debet</b> slot 2. */
	private Double nilai_d_2;
	/** Nominal <b>kredit</b> slot 2. */
	private Double nilai_k_2;
	/** Nominal <b>debet</b> slot 3. */
	private Double nilai_d_3;
	/** Nominal <b>kredit</b> slot 3. */
	private Double nilai_k_3;
	/** Nominal <b>debet</b> slot 4. */
	private Double nilai_d_4;
	/** Nominal <b>kredit</b> slot 4. */
	private Double nilai_k_4;
	/** Nominal <b>debet</b> slot 5. */
	private Double nilai_d_5;
	/** Nominal <b>kredit</b> slot 5. */
	private Double nilai_k_5;
	/** Nominal <b>debet</b> slot 6. */
	private Double nilai_d_6;
	/** Nominal <b>kredit</b> slot 6. */
	private Double nilai_k_6;
	/** Nominal <b>debet</b> slot 7. */
	private Double nilai_d_7;
	/** Nominal <b>kredit</b> slot 7. */
	private Double nilai_k_7;
	/** Nominal <b>debet</b> slot 8. */
	private Double nilai_d_8;
	/** Nominal <b>kredit</b> slot 8. */
	private Double nilai_k_8;
	/** Nominal <b>debet</b> slot 9. */
	private Double nilai_d_9;
	/** Nominal <b>kredit</b> slot 9. */
	private Double nilai_k_9;
	/** Nominal <b>debet</b> slot 10. */
	private Double nilai_d_10;
	/** Nominal <b>kredit</b> slot 10. */
	private Double nilai_k_10;
	/** Nominal <b>debet</b> slot 11. */
	private Double nilai_d_11;
	/** Nominal <b>kredit</b> slot 11. */
	private Double nilai_k_11;
	/** Nominal <b>debet</b> slot 12. */
	private Double nilai_d_12;
	/** Nominal <b>kredit</b> slot 12. */
	private Double nilai_k_12;

	/** Catatan bebas untuk baris ini; juga menjadi keluaran {@link #toString()}. */
	private String keterangan;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA untuk instansiasi entity.
	 *
	 * <p>Tidak mengisi nilai default apa pun selain {@code tanggal_dirubah} yang diinisialisasi
	 * pada deklarasi field-nya. Seluruh 26 kolom nominal, {@code tahun}, {@code akun}, dan
	 * {@code devisi} bernilai {@code null} sampai diisi setter.</p>
	 */
	public DetailLaporanPertahun() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Dipetakan sebagai identity ({@code GenerationType.IDENTITY}) dengan
	 * {@code insertable = false}, jadi nilainya dibangkitkan basis data dan tidak pernah ikut
	 * dikirim pada {@code INSERT}.</p>
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
	 * Menetapkan kunci utama baris ini.
	 *
	 * <p>Setter murni. Normalnya hanya dipanggil Hibernate setelah {@code INSERT}; mengisinya
	 * manual pada objek baru berisiko membuat Hibernate menganggap objek sebagai detached.</p>
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan catatan bebas baris ini.
	 *
	 * <p>Getter murni. Nilai yang sama dipakai {@link #toString()}, sehingga kolom ini ikut
	 * menentukan bagaimana baris tampil di komponen UI yang menurunkan label dari
	 * {@code toString()}.</p>
	 *
	 * @return isi {@code keterangan}, atau {@code null} bila kosong
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan catatan bebas baris ini.
	 *
	 * <p>Setter murni tanpa validasi panjang maupun penyaringan karakter. Kolomnya tidak dibatasi
	 * panjang di anotasi, sehingga batas sesungguhnya ditentukan skema basis data.</p>
	 *
	 * @param keterangan catatan bebas; {@code null} diperbolehkan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan tahun buku yang dicakup baris ini.
	 *
	 * <p>Getter murni. Merupakan komponen kunci logis {@code (tahun, akun, devisi)} yang
	 * <b>tidak dijamin unik oleh skema</b> &mdash; tidak ada indeks unik atas kombinasi itu (lihat
	 * Javadoc kelas, butir risiko baris kembar).</p>
	 *
	 * @return tahun buku (mis. {@code 2026}), atau {@code null} bila belum diisi
	 */
	public Integer getTahun() {
		return tahun;
	}

	/**
	 * Menetapkan tahun buku yang dicakup baris ini.
	 *
	 * <p>Setter murni: tidak ada validasi rentang, tidak ada pemeriksaan terhadap periode tutup
	 * buku ({@link Closing}), dan tidak ada pemeriksaan tabrakan dengan baris lain bertahun sama
	 * untuk akun yang sama.</p>
	 *
	 * @param tahun tahun buku; {@code null} diperbolehkan
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengembalikan divisi/unit yang menjadi sumbu segmentasi baris ini.
	 *
	 * <p>Relasi {@code @ManyToOne} yang dipetakan <b>eager</b> (tanpa {@code fetch = LAZY});
	 * {@code @Fetch(FetchMode.SELECT)} hanya memisahkan pengambilannya menjadi SELECT tersendiri,
	 * bukan menundanya. Karena itu getter ini <b>tidak</b> perlu memanggil {@code check()} seperti
	 * {@link #getAkun()}, dan tidak punya efek samping apa pun.</p>
	 *
	 * <p><b>Bukan sumbu tenant.</b> {@link Devisi} hanya menyimpan nama/kode/keterangan dan tidak
	 * punya kolom sekolah, yayasan, maupun satuan kerja &mdash; jadi kolom ini tidak dapat dipakai
	 * untuk memisahkan data antar-penyewa.</p>
	 *
	 * @return divisi terkait, atau {@code null} bila baris ini tidak disegmen per divisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "devisi", nullable = true)
	public Devisi getDevisi() {
		return devisi;
	}

	/**
	 * Menetapkan divisi/unit segmentasi baris ini.
	 *
	 * <p>Setter murni. <b>Perhatikan cascade:</b> relasi ini ber-{@code cascade = {PERSIST, MERGE}},
	 * sehingga menyimpan baris laporan ini akan ikut mem-persist/merge instance {@link Devisi} yang
	 * ditugaskan &mdash; termasuk perubahan yang tanpa sengaja dilakukan pada instance master
	 * tersebut di memori.</p>
	 *
	 * @param devisi divisi terkait; {@code null} diperbolehkan
	 */
	public void setDevisi(Devisi devisi) {
		this.devisi = devisi;
	}

	/**
	 * Mengembalikan akun bagan akun yang nilainya diringkas baris ini.
	 *
	 * <p><b>Getter dengan penugasan balik.</b> Relasi ini dipetakan {@code fetch = LAZY}, sehingga
	 * field-nya bisa berisi proxy Hibernate yang belum terinisialisasi &mdash; dan bila objek
	 * {@code DetailLaporanPertahun} sudah detached, mengaksesnya langsung akan melempar
	 * {@code LazyInitializationException}. Untuk mencegah itu, getter memanggil
	 * {@code check(akun)} milik {@link GeneralValueObject} lalu <b>menugaskan hasilnya kembali ke
	 * field {@code akun}</b>; penugasan balik itu bukan kelalaian, melainkan bagian dari pola
	 * resolusi lazy repositori ini (hasil resolusi disimpan agar pemanggilan berikutnya murah).</p>
	 *
	 * <p><b>Bukan getter destruktif.</b> Berbeda dari {@code Transaksi.getAkun()} yang menimpa
	 * akun dengan akun <i>lain</i> ({@code akunOver}) dan karenanya memindahkan atribusi buku
	 * besar hanya dengan dibaca, di sini entity pengganti selalu ber-id sama dengan aslinya.
	 * Hibernate membandingkan relasi {@code @ManyToOne} berdasarkan identifier, jadi tidak ada
	 * baris yang dianggap kotor dan tidak ada {@code UPDATE} liar meskipun kelas ini
	 * {@code dynamicUpdate = true}. Nilai finansial baris tidak tersentuh sama sekali.</p>
	 *
	 * <p><b>Biaya yang perlu disadari:</b> pada kasus terburuk {@code check()} membuka session dan
	 * koneksi basis data baru dari dalam jalur getter. Membaca akun untuk ribuan baris laporan satu
	 * per satu karena itu jauh lebih mahal dari yang terlihat; muat relasinya lewat satu query
	 * (mis. {@code join fetch}) bila kelak ada konsumen yang mengiterasi tabel ini.</p>
	 *
	 * @return akun terkait dengan proxy yang sudah diresolusi bila memungkinkan; dapat
	 *         mengembalikan proxy apa adanya bila seluruh tahap resolusi gagal, atau {@code null}
	 *         bila baris memang tidak punya akun
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = true)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/**
	 * Menetapkan akun bagan akun yang diringkas baris ini.
	 *
	 * <p>Setter murni tanpa validasi: akun bertipe apa pun diterima, akun nonaktif diterima, dan
	 * {@code null} diterima (kolomnya memang {@code nullable}) walaupun baris laporan tanpa akun
	 * tidak punya makna. <b>Perhatikan cascade {@code PERSIST}/{@code MERGE}</b>: menyimpan baris
	 * ini akan ikut menulis instance {@link Akun} yang ditugaskan, sehingga perubahan tak sengaja
	 * pada master bagan akun bisa ikut tersimpan lewat jalur "laporan".</p>
	 *
	 * @param akun akun terkait; {@code null} diperbolehkan oleh skema
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Mengembalikan nominal <b>debet</b> slot 0 (lihat konvensi slot pada Javadoc kelas).
	 *
	 * @return nominal debet slot 0, atau {@code null} bila belum diisi
	 */
	public Double getNilai_d_0() {
		return nilai_d_0;
	}

	/**
	 * Menetapkan nominal <b>debet</b> slot 0. Setter murni: tidak ada validasi tanda, pembulatan,
	 * maupun pemeriksaan keseimbangan terhadap sisi kredit.
	 *
	 * @param nilai_d_0 nominal debet slot 0; {@code null} dan nilai negatif sama-sama diterima
	 */
	public void setNilai_d_0(Double nilai_d_0) {
		this.nilai_d_0 = nilai_d_0;
	}

	/**
	 * Mengembalikan nominal <b>kredit</b> slot 0 (lihat konvensi slot pada Javadoc kelas).
	 *
	 * @return nominal kredit slot 0, atau {@code null} bila belum diisi
	 */
	public Double getNilai_k_0() {
		return nilai_k_0;
	}

	/**
	 * Menetapkan nominal <b>kredit</b> slot 0. Setter murni tanpa validasi apa pun.
	 *
	 * @param nilai_k_0 nominal kredit slot 0; {@code null} dan nilai negatif sama-sama diterima
	 */
	public void setNilai_k_0(Double nilai_k_0) {
		this.nilai_k_0 = nilai_k_0;
	}

	/**
	 * Mengembalikan nominal <b>debet</b> slot 1.
	 *
	 * @return nominal debet slot 1, atau {@code null} bila belum diisi
	 */
	public Double getNilai_d_1() {
		return nilai_d_1;
	}

	/**
	 * Menetapkan nominal <b>debet</b> slot 1. Setter murni tanpa validasi apa pun.
	 *
	 * @param nilai_d_1 nominal debet slot 1; {@code null} diperbolehkan
	 */
	public void setNilai_d_1(Double nilai_d_1) {
		this.nilai_d_1 = nilai_d_1;
	}

	/**
	 * Mengembalikan nominal <b>kredit</b> slot 1.
	 *
	 * @return nominal kredit slot 1, atau {@code null} bila belum diisi
	 */
	public Double getNilai_k_1() {
		return nilai_k_1;
	}

	/**
	 * Menetapkan nominal <b>kredit</b> slot 1. Setter murni tanpa validasi apa pun.
	 *
	 * @param nilai_k_1 nominal kredit slot 1; {@code null} diperbolehkan
	 */
	public void setNilai_k_1(Double nilai_k_1) {
		this.nilai_k_1 = nilai_k_1;
	}

	/**
	 * Mengembalikan nominal <b>debet</b> slot 2.
	 *
	 * @return nominal debet slot 2, atau {@code null} bila belum diisi
	 */
	public Double getNilai_d_2() {
		return nilai_d_2;
	}

	/**
	 * Menetapkan nominal <b>debet</b> slot 2. Setter murni tanpa validasi apa pun.
	 *
	 * @param nilai_d_2 nominal debet slot 2; {@code null} diperbolehkan
	 */
	public void setNilai_d_2(Double nilai_d_2) {
		this.nilai_d_2 = nilai_d_2;
	}

	/**
	 * Mengembalikan nominal <b>kredit</b> slot 2.
	 *
	 * @return nominal kredit slot 2, atau {@code null} bila belum diisi
	 */
	public Double getNilai_k_2() {
		return nilai_k_2;
	}

	/**
	 * Menetapkan nominal <b>kredit</b> slot 2. Setter murni tanpa validasi apa pun.
	 *
	 * @param nilai_k_2 nominal kredit slot 2; {@code null} diperbolehkan
	 */
	public void setNilai_k_2(Double nilai_k_2) {
		this.nilai_k_2 = nilai_k_2;
	}

	/**
	 * Mengembalikan nominal <b>debet</b> slot 3.
	 *
	 * @return nominal debet slot 3, atau {@code null} bila belum diisi
	 */
	public Double getNilai_d_3() {
		return nilai_d_3;
	}

	/**
	 * Menetapkan nominal <b>debet</b> slot 3. Setter murni tanpa validasi apa pun.
	 *
	 * @param nilai_d_3 nominal debet slot 3; {@code null} diperbolehkan
	 */
	public void setNilai_d_3(Double nilai_d_3) {
		this.nilai_d_3 = nilai_d_3;
	}

	/**
	 * Mengembalikan nominal <b>kredit</b> slot 3.
	 *
	 * @return nominal kredit slot 3, atau {@code null} bila belum diisi
	 */
	public Double getNilai_k_3() {
		return nilai_k_3;
	}

	/**
	 * Menetapkan nominal <b>kredit</b> slot 3. Setter murni tanpa validasi apa pun.
	 *
	 * @param nilai_k_3 nominal kredit slot 3; {@code null} diperbolehkan
	 */
	public void setNilai_k_3(Double nilai_k_3) {
		this.nilai_k_3 = nilai_k_3;
	}

	/**
	 * Mengembalikan nominal <b>debet</b> slot 4.
	 *
	 * @return nominal debet slot 4, atau {@code null} bila belum diisi
	 */
	public Double getNilai_d_4() {
		return nilai_d_4;
	}

	/**
	 * Menetapkan nominal <b>debet</b> slot 4. Setter murni tanpa validasi apa pun.
	 *
	 * @param nilai_d_4 nominal debet slot 4; {@code null} diperbolehkan
	 */
	public void setNilai_d_4(Double nilai_d_4) {
		this.nilai_d_4 = nilai_d_4;
	}

	/**
	 * Mengembalikan nominal <b>kredit</b> slot 4.
	 *
	 * @return nominal kredit slot 4, atau {@code null} bila belum diisi
	 */
	public Double getNilai_k_4() {
		return nilai_k_4;
	}

	/**
	 * Menetapkan nominal <b>kredit</b> slot 4. Setter murni tanpa validasi apa pun.
	 *
	 * @param nilai_k_4 nominal kredit slot 4; {@code null} diperbolehkan
	 */
	public void setNilai_k_4(Double nilai_k_4) {
		this.nilai_k_4 = nilai_k_4;
	}

	/**
	 * Mengembalikan nominal <b>debet</b> slot 5.
	 *
	 * @return nominal debet slot 5, atau {@code null} bila belum diisi
	 */
	public Double getNilai_d_5() {
		return nilai_d_5;
	}

	/**
	 * Menetapkan nominal <b>debet</b> slot 5. Setter murni tanpa validasi apa pun.
	 *
	 * @param nilai_d_5 nominal debet slot 5; {@code null} diperbolehkan
	 */
	public void setNilai_d_5(Double nilai_d_5) {
		this.nilai_d_5 = nilai_d_5;
	}

	/**
	 * Mengembalikan nominal <b>kredit</b> slot 5.
	 *
	 * @return nominal kredit slot 5, atau {@code null} bila belum diisi
	 */
	public Double getNilai_k_5() {
		return nilai_k_5;
	}

	/**
	 * Menetapkan nominal <b>kredit</b> slot 5. Setter murni tanpa validasi apa pun.
	 *
	 * @param nilai_k_5 nominal kredit slot 5; {@code null} diperbolehkan
	 */
	public void setNilai_k_5(Double nilai_k_5) {
		this.nilai_k_5 = nilai_k_5;
	}

	/**
	 * Mengembalikan nominal <b>debet</b> slot 6.
	 *
	 * @return nominal debet slot 6, atau {@code null} bila belum diisi
	 */
	public Double getNilai_d_6() {
		return nilai_d_6;
	}

	/**
	 * Menetapkan nominal <b>debet</b> slot 6. Setter murni tanpa validasi apa pun.
	 *
	 * @param nilai_d_6 nominal debet slot 6; {@code null} diperbolehkan
	 */
	public void setNilai_d_6(Double nilai_d_6) {
		this.nilai_d_6 = nilai_d_6;
	}

	/**
	 * Mengembalikan nominal <b>kredit</b> slot 6.
	 *
	 * @return nominal kredit slot 6, atau {@code null} bila belum diisi
	 */
	public Double getNilai_k_6() {
		return nilai_k_6;
	}

	/**
	 * Menetapkan nominal <b>kredit</b> slot 6. Setter murni tanpa validasi apa pun.
	 *
	 * @param nilai_k_6 nominal kredit slot 6; {@code null} diperbolehkan
	 */
	public void setNilai_k_6(Double nilai_k_6) {
		this.nilai_k_6 = nilai_k_6;
	}

	/**
	 * Mengembalikan nominal <b>debet</b> slot 7.
	 *
	 * @return nominal debet slot 7, atau {@code null} bila belum diisi
	 */
	public Double getNilai_d_7() {
		return nilai_d_7;
	}

	/**
	 * Menetapkan nominal <b>debet</b> slot 7. Setter murni tanpa validasi apa pun.
	 *
	 * @param nilai_d_7 nominal debet slot 7; {@code null} diperbolehkan
	 */
	public void setNilai_d_7(Double nilai_d_7) {
		this.nilai_d_7 = nilai_d_7;
	}

	/**
	 * Mengembalikan nominal <b>kredit</b> slot 7.
	 *
	 * @return nominal kredit slot 7, atau {@code null} bila belum diisi
	 */
	public Double getNilai_k_7() {
		return nilai_k_7;
	}

	/**
	 * Menetapkan nominal <b>kredit</b> slot 7. Setter murni tanpa validasi apa pun.
	 *
	 * @param nilai_k_7 nominal kredit slot 7; {@code null} diperbolehkan
	 */
	public void setNilai_k_7(Double nilai_k_7) {
		this.nilai_k_7 = nilai_k_7;
	}

	/**
	 * Mengembalikan nominal <b>debet</b> slot 8.
	 *
	 * @return nominal debet slot 8, atau {@code null} bila belum diisi
	 */
	public Double getNilai_d_8() {
		return nilai_d_8;
	}

	/**
	 * Menetapkan nominal <b>debet</b> slot 8. Setter murni tanpa validasi apa pun.
	 *
	 * @param nilai_d_8 nominal debet slot 8; {@code null} diperbolehkan
	 */
	public void setNilai_d_8(Double nilai_d_8) {
		this.nilai_d_8 = nilai_d_8;
	}

	/**
	 * Mengembalikan nominal <b>kredit</b> slot 8.
	 *
	 * @return nominal kredit slot 8, atau {@code null} bila belum diisi
	 */
	public Double getNilai_k_8() {
		return nilai_k_8;
	}

	/**
	 * Menetapkan nominal <b>kredit</b> slot 8. Setter murni tanpa validasi apa pun.
	 *
	 * @param nilai_k_8 nominal kredit slot 8; {@code null} diperbolehkan
	 */
	public void setNilai_k_8(Double nilai_k_8) {
		this.nilai_k_8 = nilai_k_8;
	}

	/**
	 * Mengembalikan nominal <b>debet</b> slot 9.
	 *
	 * @return nominal debet slot 9, atau {@code null} bila belum diisi
	 */
	public Double getNilai_d_9() {
		return nilai_d_9;
	}

	/**
	 * Menetapkan nominal <b>debet</b> slot 9. Setter murni tanpa validasi apa pun.
	 *
	 * @param nilai_d_9 nominal debet slot 9; {@code null} diperbolehkan
	 */
	public void setNilai_d_9(Double nilai_d_9) {
		this.nilai_d_9 = nilai_d_9;
	}

	/**
	 * Mengembalikan nominal <b>kredit</b> slot 9.
	 *
	 * @return nominal kredit slot 9, atau {@code null} bila belum diisi
	 */
	public Double getNilai_k_9() {
		return nilai_k_9;
	}

	/**
	 * Menetapkan nominal <b>kredit</b> slot 9. Setter murni tanpa validasi apa pun.
	 *
	 * @param nilai_k_9 nominal kredit slot 9; {@code null} diperbolehkan
	 */
	public void setNilai_k_9(Double nilai_k_9) {
		this.nilai_k_9 = nilai_k_9;
	}

	/**
	 * Mengembalikan nominal <b>debet</b> slot 10.
	 *
	 * @return nominal debet slot 10, atau {@code null} bila belum diisi
	 */
	public Double getNilai_d_10() {
		return nilai_d_10;
	}

	/**
	 * Menetapkan nominal <b>debet</b> slot 10. Setter murni tanpa validasi apa pun.
	 *
	 * @param nilai_d_10 nominal debet slot 10; {@code null} diperbolehkan
	 */
	public void setNilai_d_10(Double nilai_d_10) {
		this.nilai_d_10 = nilai_d_10;
	}

	/**
	 * Mengembalikan nominal <b>kredit</b> slot 10.
	 *
	 * @return nominal kredit slot 10, atau {@code null} bila belum diisi
	 */
	public Double getNilai_k_10() {
		return nilai_k_10;
	}

	/**
	 * Menetapkan nominal <b>kredit</b> slot 10. Setter murni tanpa validasi apa pun.
	 *
	 * @param nilai_k_10 nominal kredit slot 10; {@code null} diperbolehkan
	 */
	public void setNilai_k_10(Double nilai_k_10) {
		this.nilai_k_10 = nilai_k_10;
	}

	/**
	 * Mengembalikan nominal <b>debet</b> slot 11.
	 *
	 * @return nominal debet slot 11, atau {@code null} bila belum diisi
	 */
	public Double getNilai_d_11() {
		return nilai_d_11;
	}

	/**
	 * Menetapkan nominal <b>debet</b> slot 11. Setter murni tanpa validasi apa pun.
	 *
	 * @param nilai_d_11 nominal debet slot 11; {@code null} diperbolehkan
	 */
	public void setNilai_d_11(Double nilai_d_11) {
		this.nilai_d_11 = nilai_d_11;
	}

	/**
	 * Mengembalikan nominal <b>kredit</b> slot 11.
	 *
	 * @return nominal kredit slot 11, atau {@code null} bila belum diisi
	 */
	public Double getNilai_k_11() {
		return nilai_k_11;
	}

	/**
	 * Menetapkan nominal <b>kredit</b> slot 11. Setter murni tanpa validasi apa pun.
	 *
	 * @param nilai_k_11 nominal kredit slot 11; {@code null} diperbolehkan
	 */
	public void setNilai_k_11(Double nilai_k_11) {
		this.nilai_k_11 = nilai_k_11;
	}

	/**
	 * Mengembalikan nominal <b>debet</b> slot 12.
	 *
	 * @return nominal debet slot 12, atau {@code null} bila belum diisi
	 */
	public Double getNilai_d_12() {
		return nilai_d_12;
	}

	/**
	 * Menetapkan nominal <b>debet</b> slot 12. Setter murni tanpa validasi apa pun.
	 *
	 * @param nilai_d_12 nominal debet slot 12; {@code null} diperbolehkan
	 */
	public void setNilai_d_12(Double nilai_d_12) {
		this.nilai_d_12 = nilai_d_12;
	}

	/**
	 * Mengembalikan nominal <b>kredit</b> slot 12.
	 *
	 * @return nominal kredit slot 12, atau {@code null} bila belum diisi
	 */
	public Double getNilai_k_12() {
		return nilai_k_12;
	}

	/**
	 * Menetapkan nominal <b>kredit</b> slot 12. Setter murni tanpa validasi apa pun.
	 *
	 * @param nilai_k_12 nominal kredit slot 12; {@code null} diperbolehkan
	 */
	public void setNilai_k_12(Double nilai_k_12) {
		this.nilai_k_12 = nilai_k_12;
	}

}
