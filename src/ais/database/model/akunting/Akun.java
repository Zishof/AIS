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

import org.hibernate.envers.Audited;

import ais.database.model.Bank;
import ais.database.model.GeneralValueObject;
import ais.database.model.rab.SatuanKerja;

/**
 * <h3>Akun &mdash; satu baris Bagan Akun (<i>Chart of Accounts</i>)</h3>
 *
 * <p>Entity ini memetakan tabel <b>{@code akunting.akun}</b> dan merupakan <b>master akun
 * akuntansi</b> instalasi: "Kas", "Bank BNI", "Piutang Siswa", "Pendapatan SPP", "Beban Gaji",
 * dan seterusnya. Ia adalah katalog, bukan angka: nominal tidak pernah disimpan di sini,
 * melainkan di baris jurnal yang menunjuk ke akun ini.</p>
 *
 * <p><b>Perannya di dalam rantai akuntansi.</b> Setiap baris jurnal
 * {@link ais.database.model.akunting.Transaksi} memiliki kolom {@code akun} (relasi
 * {@code ManyToOne} ke entity ini) yang menyebutkan ke akun mana debet/kredit baris itu
 * dibukukan &mdash; hubungan ini TERVERIFIKASI pada
 * {@code Transaksi.getAkun()}/{@code setAkun(Akun)}. {@code Transaksi} juga punya kolom kedua
 * {@code akunOver} yang, bila terisi, <b>mengalahkan</b> nilai {@code akun} pada getter-nya
 * (mekanisme "akun pengganti" saat posting). Selain jurnal, puluhan master lain memakai
 * {@code Akun} sebagai <i>pointer pembukuan</i>: {@code JenisTransaksi}, {@code JenisKasBesar},
 * {@code JenisKasKecil}, {@code JenisUangMuka}, {@code TemplateJurnalPenyesuaian}
 * (pasangan {@code akunDebet}/{@code akunKredit}), {@code SaldoAwalAkun},
 * {@code KelompokLaporanPunyaAkun}, seluruh keluarga {@code ItemBiayaPunya*}
 * (piutang/diskon/denda/dibayar-di-muka), {@code ItemBiayaSekolah}, {@code ItemGaji} payroll,
 * {@code JenisTabungan}, {@code CaraPembayaranKoperasi}, sampai {@code Workspace} RAB.
 * Dengan kata lain: mengubah satu baris di sini menggeser tempat pencatatan uang di banyak
 * modul sekaligus.</p>
 *
 * <h4>Hierarki akun &mdash; ya, berjenjang (parent-child)</h4>
 * <p>Relasi {@link #getParent()} menunjuk ke {@code Akun} lain lewat kolom {@code parent},
 * membentuk pohon <b>N level</b> yang dirakit {@code ais.action.master.akunting.util.AkunTreeModel}
 * (akar = akun dengan {@code parent} {@code null}). Konvensi pemakaiannya:</p>
 * <ul>
 *   <li>Akun <b>daun</b> (tanpa turunan) adalah akun yang boleh dipilih di transaksi. Picker
 *       {@code AmbilDataAkunBanbox} menolak node yang masih punya sub-akun kecuali mode
 *       {@code bisaDipilihSemua} dinyalakan.</li>
 *   <li>Akun <b>induk</b> berfungsi sebagai pengelompok/penjumlah pada laporan.</li>
 *   <li>Layar master membuat kode akun anak secara otomatis dengan menempelkan digit {@code "0"}
 *       sebanyak nilai <i>system property</i> {@code akun_lenght} (bawaan 2) di belakang kode
 *       induk &mdash; jadi kode akun bersifat <b>berjenjang secara tekstual</b> juga.</li>
 * </ul>
 * <p><b>Tidak ada penjaga siklus.</b> Jalur simpan layar ZK ({@code AkunAction.onSave})
 * memasang {@code parent} apa adanya dari banbox tanpa memeriksa akun-menunjuk-diri-sendiri atau
 * lingkaran A&rarr;B&rarr;A. Hanya jalur API ({@code KodeAkunApiHelper.akunImpor}) yang menolak
 * "induk tidak boleh dirinya sendiri", dan itu pun hanya kasus satu tingkat. Lingkaran yang
 * terlanjur tersimpan akan membuat {@link #getGrupAkun()} berekursi tanpa henti
 * ({@code StackOverflowError}) dan {@code AkunTreeModel} menggantung.</p>
 *
 * <h4>{@code debit_credit} &mdash; saldo normal akun</h4>
 * <p>Kolom {@code debit_credit} (properti {@link #getDebetCredit() debetCredit}) menyimpan
 * <b>saldo normal</b> akun: {@link #DEBET} ({@code 1}) bila akun bertambah di sisi debet
 * (aset, beban), {@link #CREDIT} ({@code -1}) bila akun bertambah di sisi kredit (kewajiban,
 * ekuitas, pendapatan, dan akun lawan seperti Akumulasi Penyusutan). Nilai itu bukan hiasan
 * &mdash; ia dipakai di tiga tempat berbeda:</p>
 * <ol>
 *   <li><b>Label layar.</b> {@code AkunAction} dan seluruh picker menampilkan "Debet" bila
 *       nilainya sama dengan {@link #DEBET}, selain itu "Credit".</li>
 *   <li><b>Penyaring picker.</b> {@code AmbilDataAkunDebetBanbox} memasang
 *       {@code debetCredit = Akun.DEBET} dan {@code AmbilDataAkunKreditBanbox} memasang
 *       {@code Akun.CREDIT}, lalu menyaring dengan {@code Restrictions.eq("debetCredit", ...)}.
 *       Akun yang nilainya di luar {1, -1} <b>tidak pernah muncul</b> di kedua picker itu.</li>
 *   <li><b>Aritmetika laporan.</b> {@code LaporanKeuanganCoaHelper} mengalikan saldo akun dengan
 *       angka ini apa adanya: {@code nilai = saldo * akun.getDebetCredit()}. Jadi angka pada
 *       kolom ini masuk langsung ke besaran <i>dan</i> tanda pada laporan keuangan COA.</li>
 * </ol>
 * <p><b>PERINGATAN INTEGRITAS DATA &mdash; ada sandi ketiga yang beredar.</b>
 * {@code KodeAkunApiHelper.posisiDariTipeAccurate()} mengembalikan <b>{@code 2}</b> (bukan
 * {@code -1}) untuk seluruh tipe Accurate sisi kredit ({@code DEPR}, {@code APAY}, {@code OCLY},
 * {@code LTLY}, {@code EQTY}, {@code REVE}, {@code OINC}), dan nilai itu ditulis ke kolom
 * {@code debit_credit} yang sama oleh {@code akunImpor}. Akibatnya, akun kredit hasil impor
 * Accurate: (a) tetap <i>tampak</i> benar di layar (karena label hanya menguji "sama dengan 1?"),
 * (b) <b>hilang</b> dari picker akun kredit, dan (c) pada
 * {@code LaporanKeuanganCoaHelper} dikalikan {@code +2} alih-alih {@code -1} &mdash; besaran dua
 * kali lipat <b>dan tanda terbalik</b>. Model tenant baru
 * ({@code SalesInventoryFinanceTenant}) sudah menyadari perselisihan ini dan sengaja menerima
 * {@code -1} maupun {@code 2} sebagai "kredit" pada masukan, tetapi jalur legacy di paket ini
 * tidak melakukan normalisasi apa pun.</p>
 * <p><b>Kolom ini juga "kolom wajib" secara praktik.</b> DDL-nya {@code nullable = true}, tetapi
 * baik {@code AkunAction.onSave} maupun {@code AkunGenericCrudAdapter.validate} menolak simpan
 * bila Debet/Credit belum dipilih, dan {@link #getDebetCredit()} memaksa nilai {@code null}
 * menjadi {@link #CREDIT} (lihat catatan tulis-balik pada getter-nya). Baris dengan
 * {@code debit_credit} kosong karena itu berperilaku sebagai akun kredit, bukan sebagai
 * "belum ditentukan".</p>
 *
 * <h4>Cakupan (tenant) &mdash; bagan akun bersifat GLOBAL per instalasi</h4>
 * <p>Entity ini <b>tidak punya kolom {@code sekolah} maupun {@code yayasan}</b> sama sekali.
 * Bagan akun karena itu satu untuk seluruh instalasi, dan itu memang disengaja:
 * {@code AkunGenericCrudAdapter} menyatakannya lewat {@code applyReadScope}/{@code applyCountScope}
 * yang sengaja kosong ("bagan akun berlaku sepanjang institusi"). Konsekuensi yang perlu
 * disadari:</p>
 * <ul>
 *   <li><b>Keunikan kode bersifat global, bukan per-tenant.</b> {@code @Column(unique = true)}
 *       pada {@link #getKode()} adalah batasan tingkat basis data untuk SELURUH tabel, dan kedua
 *       validasi aplikasi mengikutinya: {@code AkunAction.checkNamaAkun()} dan
 *       {@code AkunGenericCrudAdapter.beforeSave()} menghitung baris berkode sama <b>tanpa</b>
 *       penyaring sekolah/yayasan/satuan kerja (hanya mengecualikan id baris yang sedang
 *       disunting). Dua sekolah pada satu instalasi tidak dapat memakai kode akun yang sama untuk
 *       akun yang berbeda.</li>
 *   <li><b>Pembatas satu-satunya bersifat opsional dan fail-open.</b> {@link #getSatuanKerja()}
 *       (unit kerja RAB) boleh {@code null} = "berlaku untuk semua". Penyaring di
 *       {@code AkunTreeModel} berbunyi: bila himpunan satuan kerja pengguna
 *       ({@code SekolahUtil.ambilSatuanKerjas()}) <b>kosong</b>, kriterianya diganti
 *       {@code Restrictions.sqlRestriction("1=1")} &mdash; yaitu <b>seluruh akun seluruh unit
 *       kerja ditampilkan</b>, bukan nol akun. Pola fail-open cakupan yang sama dengan temuan
 *       batch-batch sebelumnya.</li>
 * </ul>
 *
 * <h4>Hak akses layar</h4>
 * <p>Layar utamanya <b>punya menu sendiri</b>: "Setup Kode Akun" &rarr;
 * {@code /pages/master/akunting/akun.zul} (lihat {@code MenuSnapshotData}), dan
 * {@code AkunAction} memang memanggil {@code CommonPrivilages.checkPrevilages(CREATE/UPDATE/DELETE)}
 * &mdash; jadi entity ini <b>bukan</b> contoh master tanpa gerbang. Yang <b>tetap</b> merupakan
 * <b>pewarisan hak lewat menu induk</b> adalah dua tab yang disisipkan ke dalam halaman yang sama
 * lewat {@code MyInclude}: {@code /pages/master/akunting/jenis_transaksi.zul} dan
 * {@code /pages/master/bank.zul}. Keduanya terdaftar sebagai menu terpisah, tetapi
 * {@code CommonPrivilages.checkPrevilages} menyelesaikan hak terhadap
 * {@code Common.getCurrentMenu()} &mdash; menu yang <i>diklik</i> pengguna, yaitu "Setup Kode
 * Akun". Pengguna yang hanya diberi hak atas menu Setup Kode Akun dengan demikian ikut
 * memperoleh hak CRUD atas master <b>Bank</b> dan <b>Jenis Transaksi</b> tanpa pernah diberi
 * kedua menu itu.</p>
 * <p>Penghapusan massal lewat API ({@code kode_akun_bersihkan}) bergerbang
 * {@code Common.getApakahAdminLain}, bukan hak menu; ia hanya menyapu akun yang belum dipakai
 * jurnal dan tidak punya turunan.</p>
 *
 * <h4>Data rekening bank menempel di sini</h4>
 * <p>{@link #getBank()}, {@link #getAtasNama()}, dan {@link #getNoRek()} membuat sebuah akun
 * sekaligus merepresentasikan <b>rekening bank sungguhan</b>. Nilai-nilai itu bukan sekadar
 * catatan: {@code DaftarPengajuanTransfer} membaca {@code akun.getAtasNama()} sebagai nama
 * pemilik rekening <b>sumber dana</b> pada dokumen pengajuan transfer. Perhatikan
 * {@code Bank} juga punya kolom {@code akun} miliknya sendiri &mdash; keduanya adalah dua FK
 * searah yang berdiri sendiri, <b>bukan</b> pasangan dua arah yang dipetakan Hibernate, sehingga
 * keduanya bisa saling bertentangan tanpa ada yang menyelaraskan.</p>
 *
 * <h4>Pengelompokan method</h4>
 * <ul>
 *   <li><b>Identitas &amp; tampilan:</b> {@link #getId()}, {@link #getKode()}, {@link #getNama()},
 *       {@link #getKeterangan()}, {@link #toString()}, {@link #equals(Object)},
 *       {@link #compareTo(GeneralValueObject)}.</li>
 *   <li><b>Penggolongan akuntansi:</b> {@link #getDebetCredit()}, {@link #getGrupAkun()},
 *       {@link #getTipeAkun()}, {@link #getAktifitas()}, {@link #getChashFlow()}.</li>
 *   <li><b>Hierarki &amp; cakupan:</b> {@link #getParent()}, {@link #getSatuanKerja()}.</li>
 *   <li><b>Rekening bank:</b> {@link #getBank()}, {@link #getNoRek()}, {@link #getAtasNama()}.</li>
 *   <li><b>Kenyamanan pemakaian:</b> {@link #getJmlDipakai()} (pemeringkat picker).</li>
 *   <li><b>Jejak audit:</b> {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()}, {@link #onUpdate()}, ditambah {@code @Audited} (Envers).</li>
 * </ul>
 *
 * <h4>Hal-hal non-obvious yang mudah menjebak</h4>
 * <ol>
 *   <li><b>Getter yang menulis balik.</b> {@link #getGrupAkun()} <i>menugaskan</i> hasil warisan
 *       dari induk ke field-nya sendiri, dan {@link #getDebetCredit()} memaksa {@code null}
 *       menjadi {@code -1}. Karena Hibernate di kelas ini memakai <b>akses properti</b>
 *       (anotasi ada pada getter), kedua nilai turunan itu ikut ditulis ke basis data pada
 *       flush berikutnya. Lihat Javadoc masing-masing getter.</li>
 *   <li><b>{@code equals} tidak refleksif.</b> Override di bawah mengembalikan {@code false}
 *       bila salah satu {@code id} masih {@code null} &mdash; sebuah {@code Akun} baru
 *       <b>tidak sama dengan dirinya sendiri</b>, berbeda dari
 *       {@link GeneralValueObject#equals(Object)} yang jatuh kembali ke identitas objek.
 *       {@code hashCode()} juga tidak di-override di sini.</li>
 *   <li><b>Field yang dideklarasikan ulang bukan bug.</b> {@link GeneralValueObject} <b>bukan</b>
 *       {@code @Entity}/{@code @MappedSuperclass}; Hibernate tidak memetakan properti induknya.
 *       Karena itu {@code id}, {@code kode}, {@code nama}, {@code keterangan}, {@code oleh},
 *       {@code olehId}, dan {@code tanggal_dirubah} <b>harus</b> dideklarasikan lagi di kelas ini
 *       agar terpetakan. Konsekuensinya: properti induk yang <i>tidak</i> dideklarasikan ulang
 *       ({@code nomorUrut}, {@code nim}) tetap ada di API tetapi <b>tidak pernah tersimpan</b>.</li>
 *   <li><b>Konstanta mati.</b> {@link #JURNAL} dan {@link #TRANSAKSI} tidak dirujuk satu baris
 *       pun di seluruh repositori. {@link #getChashFlow()}/{@link #setChashFlow(Boolean)} juga
 *       nol pemanggil di luar kelas ini (Java maupun ZUL) &mdash; kolomnya tetap terpetakan dan
 *       selalu bernilai bawaan.</li>
 *   <li><b>{@code @Audited} (Envers).</b> Setiap versi baris akun digandakan ke tabel revisi
 *       {@code akun_aud}, termasuk {@code no_rek} dan {@code atas_nama}. Mengoreksi nomor
 *       rekening tidak menghapus nomor lama dari basis data.</li>
 * </ol>
 *
 * @see ais.database.model.akunting.Transaksi
 * @see ais.database.model.akunting.GrupAkun
 * @see ais.database.model.GeneralValueObject
 * @see ais.action.master.akunting.AkunAction
 * @see ais.action.master.akunting.util.AkunTreeModel
 * @see ais.database.dao.akunting.AkunDao
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "akunting", name = "akun")
public class Akun extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Nilainya sama persis dengan {@code serialVersionUID} milik
	 * {@link ais.database.model.akunting.GrupAkun} dan beberapa entity {@code akunting} lain
	 * &mdash; jejak salin-tempel dari berkas hasil {@code hbm2java} yang sama. Tidak berpengaruh
	 * pada pemetaan Hibernate; hanya relevan bila entity ini diserialisasi (sesi ZK, cache).</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * Sandi <b>saldo normal debet</b> untuk kolom {@code debit_credit}: akun bertambah di sisi
	 * debet (aset, beban, harga pokok).
	 *
	 * <p>Dipakai sebagai nilai {@code Comboitem} "Debet" di layar master, sebagai penyaring tetap
	 * {@code AmbilDataAkunDebetBanbox}, dan sebagai pengali saldo pada laporan keuangan COA.</p>
	 */
	public static final Integer DEBET = 1;
	/**
	 * Sandi <b>saldo normal kredit</b> untuk kolom {@code debit_credit}: akun bertambah di sisi
	 * kredit (kewajiban, ekuitas, pendapatan, akun lawan).
	 *
	 * <p>Nilainya sengaja {@code -1}, bukan {@code 2} atau {@code 0}, supaya laporan dapat
	 * <b>mengalikan</b> saldo dengan sandi ini dan langsung memperoleh tanda yang benar. Lihat
	 * peringatan pada Javadoc kelas: jalur impor Accurate menulis {@code 2} ke kolom yang sama,
	 * sehingga nilai konstanta ini tidak menjamin isi kolom.</p>
	 */
	public static final Integer CREDIT = -1;
	/**
	 * Konstanta warisan bersandi {@code "J"} (jurnal).
	 *
	 * <p><b>Tidak dipakai di mana pun</b> &mdash; nol rujukan {@code Akun.JURNAL} di seluruh
	 * repositori. Dipertahankan karena kompatibilitas biner konstanta publik, bukan karena
	 * ada yang membacanya.</p>
	 */
	public static final String JURNAL = "J";
	/**
	 * Konstanta warisan bersandi {@code "T"} (transaksi).
	 *
	 * <p>Sama seperti {@link #JURNAL}: <b>nol pemakai</b> di seluruh repositori.</p>
	 */
	public static final String TRANSAKSI = "T";

	/**
	 * Nilai sah pertama kolom {@link #getAktifitas() aktifitas}: <b>Aktifitas Operasi</b> pada
	 * Laporan Arus Kas (penerimaan/pengeluaran dari kegiatan utama institusi).
	 *
	 * <p>Nilainya disimpan sebagai <b>teks penuh</b>, bukan sandi &mdash; string inilah yang
	 * masuk ke kolom basis data. Dipakai {@code AkunAction} untuk membangun combobox Aktifitas.</p>
	 */
	public static final String OPERASI = "Aktifitas Operasi";
	/**
	 * Nilai sah kedua kolom {@link #getAktifitas() aktifitas}: <b>Aktifitas Investasi</b>
	 * (perolehan/pelepasan aset tetap dan investasi jangka panjang).
	 */
	public static final String INVESTASI = "Aktifitas Investasi";
	/**
	 * Nilai sah ketiga kolom {@link #getAktifitas() aktifitas}: <b>Aktifitas Pendanaan</b>
	 * (setoran modal, penarikan/pelunasan pinjaman).
	 */
	public static final String PENDANAAN = "Aktifitas Pendanaan";

	/**
	 * Kunci utama baris akun; dideklarasikan ulang karena {@link GeneralValueObject} tidak
	 * terpetakan Hibernate. Lihat {@link #getId()}.
	 */
	private Long id;
	/**
	 * Nama pengguna terakhir yang mengubah baris ini. Lihat {@link #setOleh(String)} untuk
	 * perilaku "tidak pernah dikosongkan".
	 */
	private String oleh;
	/**
	 * Id pengguna terakhir yang mengubah baris ini. Lihat {@link #setOlehId(String)}.
	 */
	private String olehId;

	/**
	 * Urutan alami akun: <b>menurut kode akun</b>, bukan menurut nama.
	 *
	 * <p><b>Tujuan.</b> Bagan akun selalu dibaca berurut kode ("1.1.01" sebelum "1.1.02"),
	 * sehingga override ini mendahulukan {@link #getKode()} di atas rantai kunci bawaan
	 * {@link GeneralValueObject#compareTo(GeneralValueObject)}
	 * ({@code nomorUrut} &rarr; {@code nim} &rarr; {@code nama} &rarr; {@code keterangan}).</p>
	 *
	 * <p><b>Cara kerja.</b> Bila kedua belah pihak punya kode non-{@code null}, hasilnya adalah
	 * {@code String.compareTo} atas kode &mdash; yaitu perbandingan <b>leksikografis</b>, bukan
	 * numerik: "1.10" mendahului "1.2". Konvensi kode berdigit tetap ({@code akun_lenght})
	 * itulah yang membuat urutan leksikografis kebetulan benar; kode yang panjang segmennya
	 * tidak seragam akan terurut tidak sesuai harapan.</p>
	 *
	 * <p><b>Jalur mundur.</b> Bila salah satu kode {@code null}, pemanggilan diteruskan ke
	 * {@code super.compareTo(arg0)}. Karena {@code nomorUrut} dan {@code nim} tidak pernah
	 * tersimpan pada entity ini, jalur mundur itu praktis membandingkan {@code nama} lalu
	 * {@code keterangan}.</p>
	 *
	 * <p><b>Efek samping / penanganan galat.</b> Seluruh badan method dibungkus {@code try-catch}
	 * yang menelan exception apa pun (dicatat ke {@code ErrorAuditUtil}) dan mengembalikan
	 * {@code 0} = "dianggap setara". Sama seperti kelas induknya, {@code compareTo} di sini
	 * <b>tidak konsisten</b> dengan {@link #equals(Object)}; hindari {@code TreeSet}/{@code TreeMap}
	 * berkunci {@code Akun}.</p>
	 *
	 * @param arg0 akun (atau entity lain) pembanding
	 * @return negatif/nol/positif sesuai kontrak {@link Comparable}; {@code 0} bila tidak ada
	 *         kunci pembanding yang tersedia atau terjadi exception
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		try {
			if (getKode() != null && arg0.getKode() != null) {
				return getKode().compareTo(arg0.getKode());
			} else {
				return super.compareTo(arg0);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/Akun.java:64");

		}

		return 0;
	}

	/**
	 * Mengembalikan id pengguna yang terakhir menyimpan baris akun ini (jejak audit).
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah disimpan lewat jalur yang
	 *         mengisi jejak audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna penyimpan terakhir.
	 *
	 * <p><b>Non-obvious:</b> masukan {@code null} atau berisi spasi saja <b>diabaikan diam-diam</b>
	 * (method langsung {@code return}), sehingga nilai lama dipertahankan. Jejak audit karena itu
	 * tidak pernah bisa dikosongkan &mdash; hanya bisa ditimpa oleh id lain yang tidak kosong.
	 * Pola ini seragam di seluruh entity AIS.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong tidak mengubah apa pun
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks akun: <code>kode + " - " + nama</code>, mis. {@code "1.1.01 - Kas"}.
	 *
	 * <p><b>Dipakai di mana:</b> teks yang ditampilkan pada bandbox pemilih akun
	 * ({@code AmbilDataAkunBanbox.setValue(akun.toString())}), pada combobox, dan pada berbagai
	 * label laporan.</p>
	 *
	 * <p><b>Non-obvious:</b> method ini membaca <b>field</b> {@code kode}/{@code nama} langsung,
	 * bukan getter-nya, sehingga <b>tidak</b> melakukan {@code trim()} seperti
	 * {@link #getKode()}/{@link #getNama()}. Bila salah satu {@code null}, hasilnya berisi
	 * literal {@code "null"} (tidak melempar {@code NullPointerException}).</p>
	 *
	 * @return kode dan nama akun yang digabung tanda hubung
	 */
	public String toString() {
		return kode + " - " + nama;
	}

	/**
	 * Kesamaan akun <b>hanya</b> berdasarkan {@code id} basis data.
	 *
	 * <p><b>Cara kerja.</b> Mengembalikan {@code true} hanya bila pembanding juga {@code Akun}
	 * dan <b>kedua</b> {@code id} tidak {@code null} serta sama nilainya.</p>
	 *
	 * <p><b>PERINGATAN &mdash; kontrak {@code equals} dilanggar.</b> Berbeda dari
	 * {@link GeneralValueObject#equals(Object)} yang jatuh kembali ke identitas objek
	 * ({@code super.equals}) saat id belum ada, override ini langsung mengembalikan
	 * {@code false}. Akibatnya sebuah {@code Akun} baru (belum tersimpan)
	 * <b>tidak sama dengan dirinya sendiri</b> &mdash; sifat refleksif hilang, sehingga
	 * {@code list.contains(akunBaru)} dan {@code list.remove(akunBaru)} tidak bekerja untuk
	 * objek transient. {@code hashCode()} tidak di-override di kelas ini, jadi jangan memakai
	 * {@code Akun} sebagai elemen {@code HashSet} atau kunci {@code HashMap}; pakai
	 * {@code Map<Long, Akun>} berkunci id seperti yang dilakukan helper laporan.</p>
	 *
	 * @param object objek pembanding; boleh {@code null}
	 * @return {@code true} hanya bila keduanya {@code Akun} dengan {@code id} sama dan tidak
	 *         {@code null}
	 */
	public boolean equals(Object object) {
		if (object == null) {
			return false;
		}
		if (object instanceof Akun) {
			Akun akun = (Akun) object;
			if (akun.id != null && id != null) {
				return akun.id.equals(id);
			}
		}
		return false;
	}

	/**
	 * Mengisi nama pengguna penyimpan terakhir.
	 *
	 * <p><b>Non-obvious:</b> sama seperti {@link #setOlehId(String)}, masukan {@code null} atau
	 * berisi spasi saja diabaikan diam-diam sehingga nilai lama bertahan.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/kosong tidak mengubah apa pun
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir menyimpan baris akun ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait {@code @PreUpdate} JPA yang menyegarkan stempel waktu audit tepat sebelum
	 * {@code UPDATE} dijalankan, plus deklarasi field {@code tanggal_dirubah} pada baris yang sama.
	 *
	 * <p><b>Tujuan.</b> Implementasi wajib dari satu-satunya method {@code abstract} milik
	 * {@link GeneralValueObject}. Badannya mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}, yang mengisi
	 * {@link #setTanggal_dirubah(Date)} dengan waktu server.</p>
	 *
	 * <p><b>Kapan dipanggil.</b> Oleh provider JPA/Hibernate saja, saat flush entity yang kotor.
	 * Tidak pernah dipanggil kode aplikasi. Perhatikan kait ini <b>tidak</b> menyala pada
	 * {@code INSERT} pertama &mdash; nilai awal {@code tanggal_dirubah} berasal dari inisialisasi
	 * field ({@code WaktuUtil.getDate()}) pada baris yang sama.</p>
	 *
	 * <p><b>Catatan gaya:</b> deklarasi field {@code tanggal_dirubah} sengaja dibiarkan menempel
	 * di baris yang sama seperti pada seluruh entity hasil generator di repositori ini; jangan
	 * dipecah tanpa alasan.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir baris akun ini.
	 *
	 * <p>Biasanya dipanggil {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}, bukan
	 * oleh kode layar. Berbeda dari {@link #setOleh(String)}, setter ini menerima {@code null}
	 * apa adanya.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris akun ini.
	 *
	 * <p>Dipetakan sebagai {@code TIMESTAMP}. Tidak ada {@code @Column}, sehingga nama kolomnya
	 * mengikuti nama properti apa adanya: {@code tanggal_dirubah}.</p>
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada objek baru karena field-nya
	 *         diinisialisasi {@code WaktuUtil.getDate()}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nama akun ("Kas", "Piutang Siswa"); wajib. Lihat {@link #getNama()}. */
	private String nama;
	/** Kode akun ("1.1.01.001"); wajib dan unik se-instalasi. Lihat {@link #getKode()}. */
	private String kode;
	/** Keterangan bebas; opsional. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Sandi tipe akun menurut penggolongan Accurate; opsional. Lihat {@link #getTipeAkun()}. */
	private String tipeAkun;
	/** Saldo normal akun ({@link #DEBET}/{@link #CREDIT}). Lihat {@link #getDebetCredit()}. */
	private Integer debetCredit;
	/** Grup akun (Aktiva/Kewajiban/Modal/...); diwarisi dari induk bila kosong. Lihat {@link #getGrupAkun()}. */
	private GrupAkun grupAkun;
	/** Pencacah popularitas akun untuk pengurutan picker; bawaan {@code 0}. Lihat {@link #getJmlDipakai()}. */
	private Long jmlDipakai = 0L;
	/** Akun induk dalam hierarki bagan akun; {@code null} untuk akar. Lihat {@link #getParent()}. */
	private Akun parent;
	/** Penanda arus kas yang <b>tidak dipakai kode mana pun</b>. Lihat {@link #getChashFlow()}. */
	private Boolean chashFlow = false;
	/** Unit kerja RAB pemilik akun; {@code null} = berlaku umum. Lihat {@link #getSatuanKerja()}. */
	private SatuanKerja satuanKerja;
	/** Klasifikasi arus kas ({@link #OPERASI}/{@link #INVESTASI}/{@link #PENDANAAN}). Lihat {@link #getAktifitas()}. */
	private String aktifitas;
	/** Bank pemilik rekening bila akun ini merepresentasikan rekening. Lihat {@link #getBank()}. */
	private Bank bank;
	/** Nomor rekening bank akun ini. Lihat {@link #getNoRek()}. */
	private String noRek;
	/** Nama pemilik rekening bank akun ini. Lihat {@link #getAtasNama()}. */
	private String atasNama;

	/**
	 * Constructor kosong yang diwajibkan Hibernate/JPA dan dipakai layar untuk membuat akun baru.
	 *
	 * <p>Objek hasil constructor ini berstatus <i>transient</i>: {@code id} masih {@code null},
	 * sehingga {@link #equals(Object)} mengembalikan {@code false} bahkan terhadap dirinya
	 * sendiri (lihat peringatan pada method itu).</p>
	 */
	public Akun() {
	}

	/**
	 * Membuat rujukan akun ringan hanya dari kunci utamanya.
	 *
	 * <p><b>Kapan dipakai.</b> Untuk memasang FK tanpa memuat baris penuh dari basis data.
	 * Contoh nyata di repositori: {@code TransaksiJurnalUmumHelper} memanggil
	 * {@code transaksi.setAkun(new Akun(idAkun))} saat menyusun baris jurnal dari id yang sudah
	 * diketahui.</p>
	 *
	 * <p><b>Perhatian.</b> Objek yang dihasilkan <b>tidak terhubung sesi</b> dan seluruh
	 * kolom lainnya {@code null}. Jangan membacanya untuk menampilkan nama/kode &mdash; simpan
	 * dulu, atau muat lewat {@code AkunDao}, atau lewatkan
	 * {@link GeneralValueObject#check(Object)} seperti yang dilakukan getter relasi di kelas ini.</p>
	 *
	 * @param id kunci utama akun yang dirujuk
	 */
	public Akun(Long id) {
		this.id = id;
	}

	/**
	 * Membuat akun sementara yang hanya berisi kode.
	 *
	 * <p><b>Kapan dipakai.</b> Satu-satunya pemakai di repositori adalah
	 * {@code ais.action.master.akunting.util.OldAkunTreeModel}, yang memanggil
	 * {@code super(new Akun("-1"))} untuk menyediakan <b>node akar semu</b> pohon &mdash; kode
	 * {@code "-1"} adalah penanda sentinel, bukan akun sungguhan, dan tidak pernah disimpan.</p>
	 *
	 * @param kode kode akun (atau sandi sentinel) yang ditanamkan ke objek baru
	 */
	public Akun(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan kunci utama baris akun.
	 *
	 * <p>Dibangkitkan basis data ({@code IDENTITY}); {@code insertable = false} karena nilainya
	 * ditetapkan sesudah {@code INSERT}. Bernilai {@code null} selama objek masih transient
	 * &mdash; kondisi yang dipakai layar untuk membedakan "tambah" dari "ubah"
	 * ({@code AkunAction.onSave}) dan yang membuat {@link #equals(Object)} selalu
	 * {@code false}.</p>
	 *
	 * @return id akun, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama secara manual.
	 *
	 * <p>Dipakai kerangka kerja dan constructor {@link #Akun(Long)}; layar tidak boleh
	 * memanggilnya untuk akun yang sudah tersimpan.</p>
	 *
	 * @param id kunci utama yang ditetapkan
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama akun, sudah di-{@code trim()}.
	 *
	 * <p>Kolom {@code nama} bersifat wajib di DDL ({@code nullable = false}, 255 karakter) dan
	 * divalidasi tidak-kosong oleh {@code AkunAction.onSave} maupun
	 * {@code AkunGenericCrudAdapter.validate}.</p>
	 *
	 * <p><b>Non-obvious &mdash; tulis balik ringan.</b> Karena Hibernate memetakan kelas ini
	 * lewat akses properti, hasil {@code trim()} inilah yang dibaca saat flush; nama yang
	 * tersimpan dengan spasi di ujung akan <b>terpangkas permanen</b> pada penyimpanan
	 * berikutnya. Bandingkan {@link #toString()} yang membaca field mentah tanpa {@code trim}.</p>
	 *
	 * @return nama akun tanpa spasi di ujung, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama akun.
	 *
	 * <p>Menyimpan nilai apa adanya (tanpa {@code trim}, tanpa penyaring XSS); pemangkasan baru
	 * terjadi saat dibaca lewat {@link #getNama()}.</p>
	 *
	 * @param nama nama akun
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas akun.
	 *
	 * <p>Opsional. Ditampilkan sebagai kolom tersendiri pada pohon dan grid master, serta ikut
	 * diekspor ke berkas Excel bagan akun.</p>
	 *
	 * <p><b>Non-obvious:</b> berbeda dari {@link GeneralValueObject#getKeterangan()} yang
	 * mengembalikan {@code ""} alih-alih {@code null}, override di sini mengembalikan nilai
	 * mentah &mdash; jadi bisa {@code null}. Perbedaan itu berpengaruh pada jalur mundur
	 * {@link #compareTo(GeneralValueObject)}.</p>
	 *
	 * @return keterangan akun, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Tipe akun menurut penggolongan <b>Accurate</b> ({@code BANK}, {@code AREC}, {@code OCAS},
	 * {@code INTR}, {@code FASS}, {@code DEPR}, {@code APAY}, {@code OCLY}, {@code LTLY},
	 * {@code EQTY}, {@code REVE}, {@code COGS}, {@code EXPS}, {@code OINC}, {@code OEXP}).
	 *
	 * <p><b>Kenapa disimpan, bukan disimpulkan.</b> Bagan akun institusi ini dipelihara di
	 * Accurate lalu dipindahkan lewat berkas Excel. Menyimpulkan kembali tipe itu dari
	 * {@code debetCredit} + {@code grupAkun} tidak mungkin tepat -- BANK dan OCAS sama-sama
	 * debet, APAY dan EQTY sama-sama kredit. Menyimpannya membuat berkas yang diunduh kembali
	 * identik dengan yang diunggah, sehingga bagan akun dapat bolak-balik tanpa kehilangan
	 * penggolongan aslinya.</p>
	 *
	 * <p>Kosong untuk akun yang tidak berasal dari Accurate; tidak ada satu pun perhitungan
	 * di aplikasi ini yang bergantung padanya.</p>
	 *
	 * <p><b>Yang menulisnya.</b> {@code KodeAkunApiHelper.akunImpor} mengisi kolom ini dari
	 * berkas impor, lalu menurunkan {@link #getDebetCredit() debetCredit} darinya lewat
	 * {@code posisiDariTipeAccurate()} &mdash; jalur yang menghasilkan sandi {@code 2} untuk sisi
	 * kredit (lihat peringatan integritas data pada Javadoc kelas). Kosakata pada kolom ini
	 * <b>berbeda</b> dari kelas akun model tenant baru ({@code ASET}, {@code KEWAJIBAN},
	 * {@code EKUITAS}, {@code PENDAPATAN}, {@code BEBAN}) dan sengaja tidak dicampur.</p>
	 *
	 * @return sandi tipe Accurate (maksimum 16 karakter), atau {@code null} bila akun tidak
	 *         berasal dari Accurate
	 */
	@Column(name = "tipe_akun", nullable = true, length = 16)
	public String getTipeAkun() {
		return this.tipeAkun;
	}

	/**
	 * Mengisi sandi tipe akun Accurate.
	 *
	 * <p>Tidak ada validasi daftar nilai di sini; nilai apa pun (termasuk kosakata dari sistem
	 * lain) akan diterima dan tersimpan.</p>
	 *
	 * @param tipeAkun sandi tipe Accurate, maksimum 16 karakter; boleh {@code null}
	 * @see #getTipeAkun()
	 */
	public void setTipeAkun(String tipeAkun) {
		this.tipeAkun = tipeAkun;
	}

	/**
	 * Mengisi keterangan bebas akun.
	 *
	 * @param keterangan keterangan akun; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan kode akun, sudah di-{@code trim()}.
	 *
	 * <p><b>Peran.</b> Kode adalah identitas <i>bisnis</i> akun ("1.1.01.001") &mdash; kunci
	 * alami yang dipakai jalur impor untuk mencocokkan baris ({@code getNaturalKeyProperties()}
	 * pada {@code AkunGenericCrudAdapter} mengembalikan tepat properti ini), dasar pengurutan
	 * alami ({@link #compareTo(GeneralValueObject)}), dan dasar penyaring "kode dimulai dengan"
	 * pada picker akun.</p>
	 *
	 * <p><b>Keunikan &mdash; GLOBAL, bukan per-tenant.</b> {@code unique = true} pada anotasi
	 * kolom adalah batasan tingkat basis data untuk seluruh tabel {@code akunting.akun}.
	 * Validasi aplikasinya mengikuti cakupan yang sama: {@code AkunAction.checkNamaAkun()} dan
	 * {@code AkunGenericCrudAdapter.beforeSave()} menghitung baris ber-{@code kode} sama tanpa
	 * penyaring sekolah/yayasan/satuan kerja apa pun, hanya mengecualikan id baris yang sedang
	 * disunting. Entity ini memang tidak punya kolom tenant sama sekali (lihat Javadoc kelas),
	 * sehingga bagan akun bersifat satu untuk seluruh instalasi.</p>
	 *
	 * <p><b>Non-obvious &mdash; tulis balik ringan.</b> Sama seperti {@link #getNama()}, hasil
	 * {@code trim()} inilah yang ikut tersimpan pada flush berikutnya. Perhatikan juga
	 * {@code AkunAction.onSave} sudah memanggil {@code kode.getValue().trim()} sebelum
	 * {@link #setKode(String)}, tetapi jalur impor tidak selalu melakukannya.</p>
	 *
	 * @return kode akun tanpa spasi di ujung, atau {@code null} bila belum diisi
	 */
	@Column(name = "kode", unique = true, nullable = false)
	public String getKode() {
		return kode == null ? null : kode.trim();
	}

	/**
	 * Mengisi kode akun.
	 *
	 * <p>Menyimpan nilai apa adanya; pemangkasan spasi baru terjadi saat dibaca. Tidak ada
	 * pemeriksaan keunikan di sini &mdash; keunikan ditegakkan oleh pemanggil
	 * ({@code AkunAction.checkNamaAkun()} / {@code AkunGenericCrudAdapter.beforeSave()}) dan
	 * pada akhirnya oleh batasan {@code unique} basis data.</p>
	 *
	 * @param kode kode akun
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Menetapkan saldo normal akun.
	 *
	 * <p>Nilai yang diharapkan hanya {@link #DEBET} ({@code 1}) atau {@link #CREDIT}
	 * ({@code -1}); layar master menjaminnya lewat combobox dua pilihan. <b>Tidak ada validasi
	 * di setter ini</b>, dan jalur impor Accurate memanfaatkannya untuk menulis {@code 2}
	 * &mdash; lihat peringatan integritas data pada Javadoc kelas.</p>
	 *
	 * @param debetCredit sandi saldo normal; boleh {@code null}, tetapi getter-nya akan
	 *                    memaksanya menjadi {@link #CREDIT} saat dibaca
	 * @see #getDebetCredit()
	 */
	public void setDebetCredit(Integer debetCredit) {
		this.debetCredit = debetCredit;
	}

	/**
	 * Mengembalikan <b>saldo normal</b> akun: {@link #DEBET} ({@code 1}) atau {@link #CREDIT}
	 * ({@code -1}).
	 *
	 * <p><b>Perannya.</b> Menentukan di sisi mana akun ini bertambah, dan karena itu ikut
	 * menentukan tanda angka pada laporan. Tiga pemakainya: label "Debet"/"Credit" di layar,
	 * penyaring picker debet/kredit ({@code AmbilDataAkunDebetBanbox} /
	 * {@code AmbilDataAkunKreditBanbox} lewat {@code Restrictions.eq("debetCredit", ...)}), dan
	 * <b>perkalian saldo</b> di {@code LaporanKeuanganCoaHelper}
	 * ({@code nilai = saldo * akun.getDebetCredit()}).</p>
	 *
	 * <p><b>PENTING &mdash; getter ini mengubah nilai, dan perubahannya bisa ikut tersimpan.</b>
	 * Bila {@code debetCredit} {@code null}, method mengembalikan {@code -1}, yakni
	 * {@link #CREDIT}. Kolom {@code debit_credit} sendiri {@code nullable = true}, sehingga baris
	 * lama atau baris hasil impor yang tidak menyebut saldo normal <b>diperlakukan sebagai akun
	 * kredit</b>, bukan sebagai "belum ditentukan". Karena kelas ini dipetakan lewat akses
	 * properti, Hibernate membaca getter ini saat flush &mdash; nilai {@code -1} hasil paksaan
	 * itu <b>ikut tertulis ke basis data</b> pada penyimpanan berikutnya, termasuk penyimpanan
	 * yang dipicu sekadar memilih akun di picker (yang menaikkan {@link #getJmlDipakai()} lalu
	 * memanggil {@code session.update(akun)}). Bedanya dengan {@code setDebetCredit(null)}:
	 * setter menyimpan {@code null}, getter membuatnya menjadi kredit permanen.</p>
	 *
	 * <p><b>Akibat lanjutan yang mudah terlewat.</b> Karena getter ini tidak pernah
	 * mengembalikan {@code null}, seluruh penjaga {@code akun.getDebetCredit() == null} di
	 * repositori adalah <b>cabang mati</b>. Dua contoh nyata: cabang "kosongkan label" pada
	 * renderer {@code AkunAction}/{@code AmbilDataAkunBanbox} tidak pernah tercapai (akun tanpa
	 * saldo normal ditampilkan "Credit"), dan
	 * {@code WorkspaceTreeModel.getRealisasi(...)} yang bermaksud mengembalikan {@code 0.0}
	 * untuk akun tanpa saldo normal justru lanjut menjumlahkan kolom {@code debet}.
	 * Ekspor Excel bagan akun ({@code AkunAction}) menuliskannya sebagai {@code "K"} dengan alasan
	 * yang sama &mdash; "tidak diisi" menjadi "kredit" saat berkas dibawa keluar.</p>
	 *
	 * @return {@link #DEBET} bila akun bersaldo normal debet; {@link #CREDIT} bila kredit
	 *         <b>atau</b> bila kolomnya kosong. Nilai lain ({@code 2}) dapat muncul untuk baris
	 *         hasil impor Accurate.
	 */
	@Column(name = "debit_credit", nullable = true)
	public Integer getDebetCredit() {
		return debetCredit == null ? -1 : debetCredit;
	}

	/**
	 * Menetapkan grup akun (Aktiva, Kewajiban, Modal, Pendapatan, Beban, ...).
	 *
	 * <p>Wajib diisi menurut validasi kedua jalur simpan, meskipun kolomnya
	 * {@code nullable = true} di DDL. Menyetel {@code null} tidak menghapus nilai efektif:
	 * {@link #getGrupAkun()} akan mengambil alih grup milik akun induk pada pembacaan
	 * berikutnya.</p>
	 *
	 * @param grupAkun grup akun; boleh {@code null}
	 * @see #getGrupAkun()
	 */
	public void setGrupAkun(GrupAkun grupAkun) {
		this.grupAkun = grupAkun;
	}

	/**
	 * Mengembalikan grup akun, <b>mewarisi dari akun induk bila akun ini belum punya</b>.
	 *
	 * <p><b>Tujuan.</b> Bagan akun berjenjang: akun anak yang tidak menyebut grupnya sendiri
	 * dianggap mengikuti grup induknya, sehingga laporan yang mengelompokkan menurut grup tetap
	 * mendapat nilai untuk setiap akun daun.</p>
	 *
	 * <p><b>Cara kerja.</b> (1) Menyegarkan referensi lewat
	 * {@link GeneralValueObject#check(Object)} &mdash; memuat ulang proxy Hibernate yang mungkin
	 * sudah lepas dari sesinya. (2) Bila hasilnya {@code null} <b>dan</b> {@link #getParent()}
	 * ada <b>dan</b> induk punya grup, nilai grup induk <b>ditugaskan ke field milik akun ini</b>.
	 * Karena {@code getParent().getGrupAkun()} memanggil method yang sama, pewarisan berjalan
	 * <b>rekursif naik</b> sampai menemukan leluhur yang punya grup.</p>
	 *
	 * <p><b>PENTING &mdash; getter destruktif (tulis balik).</b> Baris {@code grupAkun = ...}
	 * bukan sekadar cache dalam memori. Pemetaan kelas ini memakai akses properti, sehingga
	 * Hibernate membaca getter ini saat flush: begitu akun yang bersangkutan berada dalam sesi
	 * hidup dan sesuatu memicu flush, <b>grup milik induk akan tertulis permanen ke kolom
	 * {@code grup_akun} milik akun anak</b>. Sejak saat itu akun anak berhenti mengikuti induknya
	 * &mdash; memindahkan akun ke induk lain, atau mengganti grup induk, tidak lagi mengubah grup
	 * anak. Pemicu yang paling mudah terlewat adalah <b>memilih akun di picker</b>:
	 * {@code AmbilDataAkunBanbox} menaikkan {@link #getJmlDipakai()} lalu memanggil
	 * {@code session.update(akun)} di dalam transaksinya sendiri, jadi sekadar memilih akun di
	 * satu layar dapat membekukan warisan grup akun tersebut.</p>
	 *
	 * <p><b>Bahaya rekursi.</b> Tidak ada penjaga siklus. Bila {@code parent} membentuk
	 * lingkaran (A&rarr;B&rarr;A, atau akun menjadi induk dirinya sendiri &mdash; hal yang hanya
	 * dicegah jalur impor API, bukan layar ZK), pemanggilan ini berekursi tanpa henti sampai
	 * {@code StackOverflowError}.</p>
	 *
	 * <p><b>Biaya.</b> Relasi ini {@code LAZY}; setiap tingkat pewarisan berpotensi memicu kueri
	 * tersendiri, ditambah kemungkinan pembukaan sesi sementara oleh {@code check()} bila objek
	 * sudah lepas.</p>
	 *
	 * @return grup akun milik akun ini, atau grup leluhur terdekat yang punya; {@code null} bila
	 *         tidak satu pun leluhur menyebutkan grup
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "grup_akun", nullable = true)
	public GrupAkun getGrupAkun() {
		grupAkun = check(grupAkun);

		if (grupAkun == null && getParent() != null && getParent().getGrupAkun() != null) {
			grupAkun = getParent().getGrupAkun();
		}

		return grupAkun;
	}

	/**
	 * Mengembalikan pencacah berapa kali akun ini pernah dipilih lewat bandbox pemilih akun.
	 *
	 * <p><b>Untuk apa.</b> Semata-mata kenyamanan pemakaian: tab "sering dipakai" pada
	 * {@code AmbilDataAkunBanbox} dan sejenisnya mengurutkan menurun berdasarkan properti ini
	 * agar akun yang paling sering dipakai muncul lebih dulu. Tidak ada arti akuntansi sama
	 * sekali &mdash; jangan memakainya untuk menyimpulkan apakah sebuah akun sudah terpakai di
	 * jurnal (untuk itu, {@code KodeAkunApiHelper.akunBersihkan} memeriksa langsung ke tabel
	 * {@code akunting.transaksi}).</p>
	 *
	 * <p><b>Non-obvious &mdash; ditulis dari jalur pemilihan.</b> Nilainya dinaikkan oleh
	 * {@code AmbilDataAkunBanbox} setiap kali pengguna memilih akun: helper membaca nilai
	 * sekarang lewat proyeksi, menambah satu, lalu {@code session.update(akun)} + {@code commit}.
	 * Artinya <b>membuka layar transaksi dan memilih akun menerbitkan {@code UPDATE} ke tabel
	 * master bagan akun</b>, dan bersamanya ikut tersimpan pula nilai-nilai hasil paksaan getter
	 * lain di kelas ini (lihat {@link #getGrupAkun()} dan {@link #getDebetCredit()}).</p>
	 *
	 * <p>Tidak ada {@code @Column}; nama kolomnya mengikuti nama properti apa adanya.</p>
	 *
	 * @return jumlah pemakaian; bawaannya {@code 0} untuk objek baru
	 */
	public Long getJmlDipakai() {
		return jmlDipakai;
	}

	/**
	 * Menetapkan pencacah pemakaian akun.
	 *
	 * <p>Praktis hanya dipanggil bandbox pemilih akun untuk menaikkan nilainya satu. Tidak ada
	 * penjagaan {@code null} maupun nilai negatif.</p>
	 *
	 * @param jmlDipakai jumlah pemakaian baru
	 * @see #getJmlDipakai()
	 */
	public void setJmlDipakai(Long jmlDipakai) {
		this.jmlDipakai = jmlDipakai;
	}

	/**
	 * Mengembalikan penanda arus kas akun.
	 *
	 * <p><b>MATI &mdash; tidak ada pemakainya.</b> Baik getter maupun setter ini tidak dirujuk
	 * satu baris pun di luar kelas ini: nol pemanggil di kode Java, nol di halaman ZUL/JSP. Tidak
	 * ada layar yang menampilkannya, tidak ada laporan yang membacanya. Nilainya selalu tetap
	 * pada bawaannya ({@code false}) kecuali diubah langsung di basis data. Perhatikan namanya
	 * mengandung salah ketik ({@code chashFlow}, seharusnya <i>cashFlow</i>) &mdash; nama itu
	 * ikut menjadi nama kolom karena tidak ada {@code @Column}, jadi mengoreksinya berarti
	 * migrasi skema.</p>
	 *
	 * <p>Klasifikasi arus kas yang <b>benar-benar dipakai</b> ada pada
	 * {@link #getAktifitas()}.</p>
	 *
	 * @return penanda arus kas; bawaan {@code false}
	 */
	public Boolean getChashFlow() {
		return chashFlow;
	}

	/**
	 * Menetapkan penanda arus kas akun.
	 *
	 * <p>Tidak dipanggil dari mana pun di repositori &mdash; lihat {@link #getChashFlow()}.</p>
	 *
	 * @param chashFlow penanda arus kas; boleh {@code null} (setter tidak menjaganya, sehingga
	 *                  getter dapat mengembalikan {@code null} sesudahnya)
	 */
	public void setChashFlow(Boolean chashFlow) {
		this.chashFlow = chashFlow;
	}

	/**
	 * Mengembalikan akun <b>induk</b> dalam hierarki bagan akun.
	 *
	 * <p><b>Peran.</b> Relasi rujuk-diri ({@code ManyToOne} ke {@code Akun} lewat kolom
	 * {@code parent}) yang membentuk pohon bagan akun N tingkat. Akun dengan {@code parent}
	 * {@code null} adalah akar; akun tanpa turunan adalah daun dan itulah yang boleh dipakai
	 * dalam transaksi. {@code AkunTreeModel} merakit pohonnya dengan bertanya "akun apa saja yang
	 * {@code parent}-nya X" tingkat demi tingkat.</p>
	 *
	 * <p><b>Cara kerja.</b> Melewatkan referensi ke {@link GeneralValueObject#check(Object)}
	 * lebih dulu, supaya proxy {@code LAZY} yang sudah lepas dari sesinya dimuat ulang alih-alih
	 * melempar {@code LazyInitializationException}. Nilai hasil {@code check()} ditugaskan
	 * kembali ke field &mdash; secara teknis ini juga tulis balik, tetapi tidak destruktif karena
	 * yang ditulis adalah entity yang sama, bukan nilai lain.</p>
	 *
	 * <p><b>Tidak ada penjaga integritas hierarki.</b> Kelas ini (dan {@code AkunAction.onSave})
	 * menerima induk apa pun, termasuk akun itu sendiri atau keturunannya sendiri. Satu-satunya
	 * pemeriksaan di repositori ada di {@code KodeAkunApiHelper.akunImpor}
	 * ("induk tidak boleh dirinya sendiri"), dan itu pun hanya satu tingkat. Siklus yang
	 * terlanjur tersimpan membuat {@link #getGrupAkun()} berekursi tanpa henti dan
	 * {@code AkunTreeModel} menggantung.</p>
	 *
	 * <p><b>Cascade.</b> {@code PERSIST} dan {@code MERGE} &mdash; menyimpan akun anak ikut
	 * menyimpan induk yang belum tersimpan; tidak ada {@code REMOVE}, sehingga menghapus induk
	 * tidak menghapus anak (dan memang dicegah: layar hanya menampilkan tombol hapus untuk node
	 * tanpa anak, dan {@code AkunGenericCrudAdapter.canDelete} menolak penghapusan sepenuhnya).</p>
	 *
	 * @return akun induk, atau {@code null} bila akun ini berada di akar pohon
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "parent", nullable = true)
	public Akun getParent() {
		parent = check(parent);
		return parent;
	}

	/**
	 * Menetapkan akun induk.
	 *
	 * <p><b>Tanpa validasi apa pun</b>: tidak memeriksa akun-menjadi-induk-dirinya-sendiri, tidak
	 * memeriksa siklus, dan tidak memeriksa kekonsistenan kode berjenjang. Pemanggil yang perlu
	 * jaminan itu harus memeriksanya sendiri &mdash; lihat {@link #getParent()}.</p>
	 *
	 * @param parent akun induk; {@code null} menjadikan akun ini akar pohon
	 */
	public void setParent(Akun parent) {
		this.parent = parent;
	}

	/**
	 * Mengembalikan <b>unit kerja</b> (RAB) pemilik akun ini.
	 *
	 * <p><b>Peran.</b> Satu-satunya pembatas cakupan yang dimiliki entity ini &mdash; tidak ada
	 * kolom sekolah maupun yayasan. Nilai {@code null} berarti "akun umum, berlaku untuk semua
	 * unit kerja", dan itulah keadaan bawaannya. Layar master menampilkannya menempel pada label
	 * akun di pohon ({@code kode - nama - satuan kerja}).</p>
	 *
	 * <p><b>PENTING &mdash; penyaringnya fail-open.</b> {@code AkunTreeModel} membangun
	 * himpunan satuan kerja pengguna lewat {@code SekolahUtil.ambilSatuanKerjas()} lalu
	 * memasang kriteria "satuanKerja kosong ATAU termasuk himpunan itu". Bila himpunannya
	 * <b>kosong</b> &mdash; pengguna tanpa penugasan unit kerja, atau resolusi yang gagal diam-diam
	 * &mdash; kriterianya diganti {@code Restrictions.sqlRestriction("1=1")}, yaitu
	 * <b>seluruh akun seluruh unit kerja ditampilkan</b>, bukan nol akun. Kegagalan menentukan
	 * cakupan membuka data, bukan menutupnya.</p>
	 *
	 * <p><b>Cara kerja.</b> Sama seperti relasi lain di kelas ini: nilai dilewatkan
	 * {@link GeneralValueObject#check(Object)} untuk menghidupkan kembali proxy {@code LAZY} yang
	 * lepas dari sesinya, lalu ditugaskan kembali ke field.</p>
	 *
	 * @return unit kerja pemilik akun, atau {@code null} bila akun berlaku umum
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Menetapkan unit kerja pemilik akun.
	 *
	 * <p>Diisi layar master dari bandbox satuan kerja; {@code null} berarti akun berlaku umum.
	 * Perhatikan konsekuensi penyaringnya pada {@link #getSatuanKerja()}.</p>
	 *
	 * @param satuanKerja unit kerja; boleh {@code null}
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Mengembalikan <b>bank</b> tempat rekening akun ini berada.
	 *
	 * <p><b>Peran.</b> Hanya bermakna untuk akun yang merepresentasikan rekening bank
	 * sungguhan (mis. "Bank BNI - Operasional"). Bersama {@link #getNoRek()} dan
	 * {@link #getAtasNama()}, trio ini dibaca {@code DaftarPengajuanTransfer} sebagai identitas
	 * rekening <b>sumber dana</b> pada dokumen pengajuan transfer &mdash; jadi isinya bukan
	 * sekadar catatan, melainkan ikut tercetak pada instruksi pemindahan uang.</p>
	 *
	 * <p><b>Non-obvious &mdash; dua FK searah yang tidak saling menjaga.</b>
	 * {@link ais.database.model.Bank} juga memiliki kolom {@code akun} miliknya sendiri. Kedua
	 * relasi itu <b>bukan</b> pasangan dua arah yang dipetakan Hibernate ({@code mappedBy}),
	 * melainkan dua FK independen. Tidak ada kode yang menyelaraskannya, sehingga
	 * {@code akun.getBank().getAkun()} bisa saja menunjuk akun yang sama sekali lain.</p>
	 *
	 * <p><b>Cara kerja.</b> {@link GeneralValueObject#check(Object)} lebih dulu, sama seperti
	 * relasi lain di kelas ini.</p>
	 *
	 * @return bank pemilik rekening, atau {@code null} bila akun ini bukan rekening bank
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "bank_id", nullable = true)
	public Bank getBank() {
		bank = check(bank);

		return bank;
	}

	/**
	 * Menetapkan bank pemilik rekening akun ini.
	 *
	 * <p>Tidak menyentuh sisi {@code Bank.akun} &mdash; lihat catatan dua FK searah pada
	 * {@link #getBank()}.</p>
	 *
	 * @param bank bank; {@code null} berarti akun bukan rekening bank
	 */
	public void setBank(Bank bank) {
		this.bank = bank;
	}

	/**
	 * Mengembalikan <b>nomor rekening</b> bank akun ini.
	 *
	 * <p>Dikembalikan mentah tanpa normalisasi, penyamaran, maupun penyaringan. Ditampilkan di
	 * grid master, ikut diekspor ke berkas Excel bagan akun, dan ikut digandakan ke tabel revisi
	 * Envers ({@code akun_aud}) pada setiap perubahan &mdash; mengoreksi nomor rekening tidak
	 * menghapus nomor lama dari basis data.</p>
	 *
	 * <p>Tidak ada {@code @Column}; nama kolomnya mengikuti nama properti apa adanya.</p>
	 *
	 * @return nomor rekening, atau {@code null} bila akun bukan rekening bank
	 */
	public String getNoRek() {
		return noRek;
	}

	/**
	 * Menetapkan nomor rekening bank akun ini.
	 *
	 * <p>Tanpa validasi format maupun panjang.</p>
	 *
	 * @param noRek nomor rekening; boleh {@code null}
	 */
	public void setNoRek(String noRek) {
		this.noRek = noRek;
	}

	/**
	 * Mengembalikan <b>nama pemilik rekening</b> bank akun ini.
	 *
	 * <p>Dipakai {@code DaftarPengajuanTransfer} sebagai "atas nama" sisi <b>sumber dana</b>
	 * pada dokumen pengajuan transfer (lewat rantai
	 * {@code jenisUangMuka/jenisKasBesar/jenisKasKecil &rarr; akun &rarr; atasNama}), selain
	 * ditampilkan di grid master dan diekspor ke Excel.</p>
	 *
	 * <p>Tidak ada {@code @Column}; nama kolomnya mengikuti nama properti apa adanya.</p>
	 *
	 * @return nama pemilik rekening, atau {@code null} bila akun bukan rekening bank
	 */
	public String getAtasNama() {
		return atasNama;
	}

	/**
	 * Menetapkan nama pemilik rekening bank akun ini.
	 *
	 * <p>Tanpa validasi maupun penyaring XSS.</p>
	 *
	 * @param atasNama nama pemilik rekening; boleh {@code null}
	 */
	public void setAtasNama(String atasNama) {
		this.atasNama = atasNama;
	}

	/**
	 * Mengembalikan <b>klasifikasi arus kas</b> akun: {@link #OPERASI}, {@link #INVESTASI},
	 * atau {@link #PENDANAAN}.
	 *
	 * <p><b>Peran.</b> Menentukan bagian mana pada Laporan Arus Kas yang menampung akun ini.
	 * Nilainya disimpan sebagai <b>teks penuh</b> ("Aktifitas Operasi", ...), bukan sandi
	 * pendek; layar master membangun combobox-nya langsung dari ketiga konstanta di kelas ini,
	 * dan kolom ini pula yang menjadi salah satu penyaring pencarian pada grid master.</p>
	 *
	 * <p><b>Non-obvious &mdash; normalisasi yang bisa ikut tersimpan.</b> Getter mengubah string
	 * kosong/berisi spasi saja menjadi {@code null}, sehingga pemanggil cukup memeriksa
	 * {@code null} untuk "belum diklasifikasikan". Karena pemetaan memakai akses properti,
	 * nilai {@code null} hasil normalisasi ini <b>ikut tertulis</b> ke basis data pada flush
	 * berikutnya, menggantikan string kosong yang sebelumnya tersimpan. Perhatikan juga tidak ada
	 * validasi bahwa isinya termasuk salah satu dari ketiga konstanta &mdash; jalur impor API
	 * ({@code KodeAkunApiHelper}) menuliskan apa pun yang dikirim klien.</p>
	 *
	 * <p>Tidak ada {@code @Column}; nama kolomnya mengikuti nama properti apa adanya.</p>
	 *
	 * @return klasifikasi arus kas, atau {@code null} bila kosong/hanya berisi spasi
	 */
	public String getAktifitas() {
		return aktifitas == null || aktifitas.trim().isEmpty() ? null : aktifitas;
	}

	/**
	 * Menetapkan klasifikasi arus kas akun.
	 *
	 * <p>Menyimpan nilai apa adanya, termasuk string kosong (yang kemudian dinormalkan menjadi
	 * {@code null} oleh getter) dan nilai di luar ketiga konstanta yang dikenal.</p>
	 *
	 * @param aktifitas {@link #OPERASI}, {@link #INVESTASI}, {@link #PENDANAAN}, atau
	 *                  {@code null}
	 * @see #getAktifitas()
	 */
	public void setAktifitas(String aktifitas) {
		this.aktifitas = aktifitas;
	}

}
