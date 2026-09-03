package ais.database.model.akunting;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * <h2>JenisLaporan &mdash; master &quot;Jenis Laporan&quot; (sumbu PERTAMA struktur laporan keuangan)</h2>
 *
 * <p>Entity ini memetakan tabel <code>akunting.jenis_laporan</code> dan menjawab satu pertanyaan saja:
 * <b>laporan yang MANA</b> &mdash; Neraca, Rugi&nbsp;Laba, Arus&nbsp;Kas, atau apa pun yang diketikkan
 * operator. Ia adalah puncak hierarki penyusunan laporan keuangan berbasis jurnal di AIS, tetapi
 * <em>bukan</em> pemilik hierarki itu: seluruh relasi menggantung dari
 * {@link ais.database.model.akunting.KelompokLaporan}, yang memegang DUA foreign key ortogonal
 * sekaligus &mdash; satu ke entity ini (laporan mana) dan satu ke
 * {@link ais.database.model.akunting.MasterGrupLaporan} (seksi/blok cetak di dalam laporan itu, mis.
 * &quot;AKTIVA LANCAR&quot;, &quot;PENDAPATAN OPERASIONAL&quot;). Dengan kata lain, kelas ini tidak
 * mengenal anak-anaknya sama sekali; tidak ada satu pun koleksi <code>@OneToMany</code> di sini.</p>
 *
 * <h3>Bentuk penuh hierarki (terverifikasi dari kode konsumen)</h3>
 * <pre>
 *   JenisLaporan          (kelas ini)      -- laporan MANA          : "Neraca" / "Rugi Laba" / ...
 *          x
 *   MasterGrupLaporan                      -- seksi/blok di dalamnya: "AKTIVA LANCAR" / ...
 *          |
 *          v
 *   KelompokLaporan       (jenis_laporan + master_grup_laporan)     -- SATU BARIS cetak laporan
 *          |
 *          v
 *   KelompokLaporanPunyaAkun               -- akun-akun bagan yang dijumlahkan pada baris itu
 *          |
 *          v
 *   Akun -> Transaksi (debet/kredit) -> angka yang tercetak
 * </pre>
 *
 * <h3>VERIFIKASI PENTING: Neraca / Rugi&nbsp;Laba / Arus&nbsp;Kas adalah TEKS BEBAS, bukan konstanta</h3>
 *
 * <p>Rancangan aslinya memang berniat menjadikan ketiganya konstanta kode: kelas ini masih menyimpan
 * bekasnya berupa tiga field statis <code>NRC</code>, <code>RL</code>, dan <code>ARUS_KAS</code> beserta
 * seluruh badan {@link #reloadDefault()} yang menyemai baris bawaan &mdash; <b>semuanya dikomentari
 * total</b>. Hasilnya, pada kode yang berjalan hari ini:</p>
 *
 * <ul>
 *   <li>tidak ada satu pun konstanta jenis laporan di seluruh repositori;</li>
 *   <li>tidak ada baris bawaan yang pernah dibuat otomatis (lihat catatan {@link #reloadDefault()});</li>
 *   <li>baris tabel ini <b>sepenuhnya data operator</b>, diketik lewat layar master
 *       <code>JenisLaporanAction</code> (menu &quot;Jenis Laporan&quot;), persis seperti temuan
 *       <code>GrupAkun</code>: nama boleh apa saja, tidak ada kode/enum yang mengikat.</li>
 * </ul>
 *
 * <p><b>Namun berbeda dari <code>GrupAkun</code></b> (yang teksnya benar-benar tanpa makna sistem),
 * teks pada baris entity ini <b>DIBACA sebagai semantik</b> oleh beberapa konsumen lewat pencocokan
 * substring. Ini yang membuat entity yang tampak sepele ini punya konsekuensi nyata:</p>
 *
 * <table border="1" summary="Pencocokan teks yang menentukan perilaku laporan">
 *   <tr><th>Konsumen</th><th>Kolom yang dibaca</th><th>Pola</th><th>Akibat bila tidak cocok</th></tr>
 *   <tr>
 *     <td><code>LaporanKeuanganCoaHelper.kumulatif()</code>/<code>susun()</code></td>
 *     <td>{@link #getNama()}</td>
 *     <td><code>toLowerCase().contains(&quot;neraca&quot;)</code></td>
 *     <td>laporan dihitung <b>periodik</b> (hanya rentang tanggal) alih-alih <b>kumulatif</b>
 *         (sejak awal) &mdash; angka Neraca menjadi salah tanpa pesan galat apa pun</td>
 *   </tr>
 *   <tr>
 *     <td><code>PemetaanAkunHelper</code> (REST <code>pemetaan_akun_*</code>)</td>
 *     <td>{@link #getKeterangan()} (bukan nama!)</td>
 *     <td>uppercase, <code>indexOf(&quot;NERACA&quot;)</code>, lalu
 *         <code>indexOf(&quot;RUGI&quot;)||indexOf(&quot;LABA&quot;)</code></td>
 *     <td>baris tidak dikenali sebagai jenis mana pun &rarr; usulan pemetaan akun dilewati dengan
 *         pesan &quot;Jenis Laporan ... belum ada di master&quot;</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #getTampilDiDashboard()} (kelas ini sendiri)</td>
 *     <td>{@link #getNama()}</td>
 *     <td><code>toLowerCase().contains(&quot;kas&quot;)</code></td>
 *     <td>menentukan nilai bawaan tampil/tidak di dasbor</td>
 *   </tr>
 *   <tr>
 *     <td><code>DasboardAkunting</code> / <code>DasboardAkuntansi</code> (teks penjelas panel)</td>
 *     <td>judul panel = {@link #getNama()}</td>
 *     <td><code>neraca</code> / <code>laba|rugi|operasional</code> / <code>arus|kas</code></td>
 *     <td>panel mendapat kalimat penjelas generik (kosmetik saja)</td>
 *   </tr>
 * </table>
 *
 * <p><b>Kuirk yang perlu diketahui pemelihara:</b> dua konsumen terpenting membaca KOLOM YANG BERBEDA.
 * <code>LaporanKeuanganCoaHelper</code> mencocokkan <code>nama</code>, sedangkan
 * <code>PemetaanAkunHelper</code> mencocokkan <code>keterangan</code>. Baris yang diisi
 * <code>nama=&quot;Neraca&quot;</code> tetapi <code>keterangan=&quot;Laporan Posisi Keuangan&quot;</code>
 * akan dianggap Neraca oleh helper pertama <b>dan sekaligus</b> jatuh ke cabang &quot;RUGI LABA&quot; pada
 * helper kedua (cabang <code>else</code>-nya tidak punya opsi &quot;tidak dikenal&quot;). Tidak ada
 * validasi, indeks unik, maupun peringatan yang mencegah kombinasi seperti ini. Karena itu <b>mengganti
 * nama sebuah baris jenis laporan bukan operasi kosmetik</b> &mdash; ia dapat mengubah cara laporan
 * keuangan resmi dihitung.</p>
 *
 * <h3>Cakupan tenant: TIDAK ADA sama sekali</h3>
 *
 * <p>Entity ini <b>tidak punya kolom sekolah/yayasan/satuan kerja apa pun</b>. Ia bukan kasus
 * &quot;fail-open kondisional&quot; (penyaring ada tapi bisa longgar) melainkan memang katalog GLOBAL
 * satu instalasi: <code>JenisLaporanAction.onSearchDefault</code> hanya menyaring berdasarkan
 * <code>nama</code>, tanpa satu pun pembatas tenant. Satu operator sekolah A yang menambah/mengubah/
 * menghapus jenis laporan mengubahnya untuk SELURUH tenant pada instalasi yang sama. Pemisahan per
 * tenant baru muncul jauh di hilir, pada kolom <code>satuan_kerja</code> milik
 * <code>grup_transaksi</code> saat angka dijumlahkan.</p>
 *
 * <h3>Jalur tulis yang diketahui</h3>
 * <ol>
 *   <li><b>Layar ZK <code>JenisLaporanAction</code></b> (menu master &quot;Jenis Laporan&quot;) &mdash;
 *       tambah/ubah/hapus penuh, digerbangi <code>CommonPrivilages.checkPrevilages(READ/CREATE/UPDATE/
 *       DELETE)</code>. Ini satu-satunya CRUD yang dimaksudkan.</li>
 *   <li><b>Checkbox &quot;Tampilkan Di Dashboard&quot;</b> pada grid layar yang sama &mdash; menyimpan
 *       langsung ke DB pada <code>onCheck</code> tanpa tombol Simpan
 *       (<code>Common.refreshSaveOrUpdate</code>). <b>Verifikasi negatif:</b> berbeda dari pola checkbox
 *       tanpa gerbang yang ditemukan di <code>JenisKasBesar</code>/<code>CaraPembayaranTransfer</code>,
 *       di sini checkbox <em>digerbangi</em>: <code>checkbox.setDisabled(!edit)</code> dengan
 *       <code>edit</code> berasal dari hak UPDATE.</li>
 *   <li><b>Cascade tidak langsung dari {@link ais.database.model.akunting.KelompokLaporan}</b> &mdash;
 *       relasi <code>@ManyToOne(cascade = {PERSIST, MERGE})</code> di sana berarti menyimpan sebuah
 *       KelompokLaporan yang menggenggam instance JenisLaporan <em>transient</em> akan MENCIPTAKAN
 *       baris master baru di tabel ini. Tidak ada pemanggil yang diketahui memanfaatkannya, tetapi
 *       jalurnya terbuka.</li>
 *   <li><b>Penulisan &quot;hantu&quot; oleh Hibernate</b> akibat getter bernilai substitusi &mdash;
 *       lihat {@link #getTampilDiDashboard()} dan {@link #getNama()}.</li>
 * </ol>
 *
 * <h3>Keterjangkauan REST &amp; hak akses (hasil verifikasi)</h3>
 * <ul>
 *   <li><b>CRUD master lewat REST: TIDAK ADA.</b> Entity ini <b>bukan</b> salah satu master yang
 *       dilayani <code>MasterKeuanganApiHelper</code> (yang hanya menangani <code>JenisUangMuka</code>,
 *       <code>JenisKasKecil</code>, <code>JenisKasBesar</code>, <code>JenisReimbursement</code>,
 *       <code>CaraPembayaranTransfer</code>, dst.). Pola fail-open <code>bolehAksi()</code> pada peran
 *       null <b>tidak menjangkau baris tabel ini</b> &mdash; verifikasi NEGATIF, sejalan dengan hasil
 *       untuk <code>AkunPajak</code> dan {@link ais.database.model.akunting.MasterGrupLaporan}.</li>
 *   <li><b>Namun entity ini TETAP terbaca dan terpakai dari REST</b>: aksi
 *       <code>pemetaan_akun_usulan</code>/<code>pemetaan_akun_terapkan</code> pada
 *       <code>PosApi</code> membaca <code>akunting.jenis_laporan</code> dan MEMBUAT baris
 *       <code>KelompokLaporan</code> yang menunjuk ke baris tabel ini. Cabang dispatcher
 *       <code>action.startsWith(&quot;pemetaan_akun_&quot;)</code> di <code>PosApi</code>
 *       <b>tidak memasang gerbang hak apa pun</b>, dan <code>PemetaanAkunHelper.proses(...)</code>
 *       menerima parameter <code>Tbmuser</code> lalu <b>tidak pernah memakainya</b> &mdash; jadi bukan
 *       fail-open melainkan NOL gerbang. Setiap pemegang token perangkat POS yang sah, peran apa pun,
 *       dapat mengubah struktur pemetaan akun&rarr;baris laporan untuk seluruh instalasi (helper ini
 *       juga tidak menyaring tenant: ia membaca <code>akunting.akun</code> apa adanya).</li>
 *   <li><b>Pewarisan hak lewat menu induk: TIDAK BERLAKU.</b> <code>jenis_laporan.zul</code> tidak
 *       pernah di-<code>include</code> sebagai tab di layar lain mana pun, sehingga mekanisme
 *       &quot;<code>currentMenu</code> tidak di-resolve ulang untuk halaman ter-include&quot; tidak
 *       menyentuh master ini &mdash; verifikasi NEGATIF.</li>
 *   <li><b>Paparan ANONIM (di luar kendali kelas ini, tetapi memengaruhi datanya).</b>
 *       <code>/WEB-INF/baru/modul/akuntansi/_monitor_akunting_service.jsp</code> membuka sesi Hibernate
 *       dan mengembalikan daftar entity ini <em>beserta angka laporan keuangan hasil agregasi jurnal</em>
 *       sebagai JSON <b>tanpa memeriksa sesi sama sekali</b>; berkas itu terjangkau publik lewat
 *       dispatcher <code>/anjungan?hanya_tampil_jsp=true&amp;p=akuntansi&amp;s=_monitor_akunting_service</code>
 *       (lihat JavaDoc <code>ais.action.servlet.Anjungan</code>). Parameter <code>satuan_kerja_id</code>
 *       bersifat opsional; bila tidak dikirim, penyaring satuan kerja dilewati dan agregat mencakup
 *       SELURUH tenant. Verifikasi menenangkan pada satu sisi: seluruh parameter angka di JSP itu
 *       melewati <code>Long.parseLong</code>/<code>Integer.parseInt</code> dan tanggalnya diformat ulang,
 *       sehingga <b>tidak</b> ada celah SQL injection meskipun SQL-nya dirangkai dengan konkatenasi.</li>
 * </ul>
 *
 * <h3>Konsumen terverifikasi</h3>
 * <ul>
 *   <li><code>ais.action.master.akunting.JenisLaporanAction</code> &mdash; CRUD master + unggah tiga
 *       berkas template <code>.jrxml</code> per baris (lihat bagian Template di bawah).</li>
 *   <li><code>ais.action.master.akunting.KelompokLaporanAction</code> dan
 *       <code>KelompokLaporanDanDetailAction</code> &mdash; combobox pemilih/penyaring jenis laporan.</li>
 *   <li><code>ais.action.master.koperasi.helper.LaporanKeuanganCoaHelper</code> &mdash; mesin penyusun
 *       laporan keuangan berbasis bagan akun (dipakai layar ZK koperasi/kantin dan kontrak native).</li>
 *   <li><code>ais.action.master.dashboard.helper.DashboardAkuntingHelper</code> /
 *       <code>DashboardAkuntingTahunHelper</code> &mdash; SQL mentah dengan
 *       <code>c.jenis_laporan = &lt;id&gt;</code>.</li>
 *   <li><code>ais.action.master.akunting.helper.DasboardAkunting</code> /
 *       <code>DasboardAkuntansi</code> &mdash; daftar panel dasbor akuntansi + hitungan
 *       &quot;laporan aktif&quot;.</li>
 *   <li><code>ais.action.report.format1.akunting.LaporanAkuntingSaldoBulanMaster</code>,
 *       <code>LaporanAkunting2Bulan</code>, <code>LaporanAkunting2Tahun</code>,
 *       <code>LaporanAkunting12Bulan</code> &mdash; parameter Jasper <code>jenis_laporan</code>
 *       (id, atau <code>-1L</code> bila &quot;Semua&quot;).</li>
 *   <li><code>ais.common.newui.laporan.NewUiLaporanAkuntingSaldoBulanController</code> dan
 *       <code>NewUiLaporanKeuanganKoperasiController</code> &mdash; jalur UI baru.</li>
 *   <li><code>ais.action.servlet.api.PemetaanAkunHelper</code> dan
 *       <code>ais.action.servlet.api.TutupBukuHelper</code> &mdash; jalur REST (baca).</li>
 *   <li><code>ais.common.InitData</code> &mdash; pendaftaran cache
 *       (<code>ConstantValues.ambilBerdasarClass</code>) + pemanggilan {@link #reloadDefault()}.</li>
 * </ul>
 *
 * <h3>Template Jasper per baris (relasi tanpa foreign key)</h3>
 *
 * <p>Setiap baris jenis laporan bisa membawa sampai TIGA berkas <code>.jrxml</code> kustom yang
 * disimpan di <code>LampiranLain</code> dengan kunci gabungan <code>(id baris ini, konstanta jenis
 * berkas)</code>:</p>
 * <ul>
 *   <li><code>FILE_JRXML_LAYOUT_JENIS_LAPORAN_AKUNTANSI</code> (tanpa akhiran) &rarr; dipakai
 *       <code>LaporanAkuntingSaldoBulanMaster</code> dan jalur UI baru;</li>
 *   <li>konstanta yang sama + <code>&quot;_1&quot;</code> &rarr; <code>LaporanAkunting2Bulan</code>;</li>
 *   <li>konstanta yang sama + <code>&quot;_2&quot;</code> &rarr; <code>LaporanAkunting2Tahun</code>.</li>
 * </ul>
 * <p>Karena kuncinya diberi ruang-nama oleh konstanta teks, <b>tidak ada</b> tabrakan id lintas tabel
 * seperti yang pernah ditemukan pada <code>TemplateGrupTransaksi</code> &mdash; verifikasi negatif.
 * Perilaku layar berubah bergantung keberadaan berkas: bila ada minimal satu baris bertemplate,
 * combobox jenis laporan dikunci (<code>setReadonly(true)</code>) hanya pada baris-baris bertemplate;
 * bila tidak ada, combobox diisi seluruh baris ditambah opsi &quot;Semua&quot;.</p>
 *
 * <h3>Catatan teknis pewarisan</h3>
 *
 * <p>Kelas ini meng-<code>extends</code> {@link ais.database.model.GeneralValueObject}, yang <b>bukan</b>
 * <code>@Entity</code> maupun <code>@MappedSuperclass</code> &mdash; hanya POJO abstrak biasa. Hibernate
 * karena itu <b>tidak</b> memetakan properti milik induk. Deklarasi ulang <code>id</code>,
 * <code>nama</code>, <code>keterangan</code>, <code>oleh</code>, <code>olehId</code>, dan
 * <code>tanggal_dirubah</code> di kelas ini <b>bukan bug dan bukan duplikasi ceroboh</b>, melainkan
 * KEHARUSAN TEKNIS supaya kolom-kolom itu benar-benar tersimpan. Lihat
 * {@link ais.database.model.GeneralValueObject} untuk penjelasan lengkap pola tersebut.</p>
 *
 * <p>Pemetaan memakai <b>property access</b> (anotasi <code>@Id</code> menempel pada
 * {@link #getId()}), sehingga <b>setiap getter publik tanpa <code>@Transient</code> ikut dipetakan</b>
 * &mdash; termasuk {@link #getTampilDiDashboard()} yang sama sekali tidak beranotasi. Konsekuensinya
 * dibahas pada JavaDoc getter tersebut. <code>@Audited</code> (Envers) berarti setiap versi baris
 * digandakan ke tabel revisi <code>jenis_laporan_aud</code>;
 * <code>dynamicInsert</code>/<code>dynamicUpdate</code> membuat pernyataan SQL hanya memuat kolom yang
 * benar-benar berubah.</p>
 *
 * <h3>Pengelompokan method</h3>
 * <ol>
 *   <li><b>Identitas &amp; kunci utama</b>: {@link #getId()}, {@link #setId(Long)},
 *       {@link #toString()}.</li>
 *   <li><b>Isi bisnis</b>: {@link #getNama()}, {@link #setNama(String)}, {@link #getKeterangan()},
 *       {@link #setKeterangan(String)}.</li>
 *   <li><b>Perilaku penyajian</b>: {@link #getTampilDiDashboard()},
 *       {@link #setTampilDiDashboard(Boolean)}.</li>
 *   <li><b>Jejak audit</b>: {@link #getOleh()}, {@link #setOleh(String)}, {@link #getOlehId()},
 *       {@link #setOlehId(String)}, {@link #getTanggal_dirubah()},
 *       {@link #setTanggal_dirubah(Date)}, {@code onUpdate()}.</li>
 *   <li><b>Siklus hidup statis</b>: {@link #reloadDefault()} (kini no-op total).</li>
 * </ol>
 *
 * @see ais.database.model.akunting.KelompokLaporan
 * @see ais.database.model.akunting.MasterGrupLaporan
 * @see ais.database.model.akunting.KelompokLaporanPunyaAkun
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "akunting", name = "jenis_laporan")
public class JenisLaporan extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi.
	 *
	 * <p>Nilai <code>2463821577548439808L</code> <b>identik</b> dengan yang dipakai
	 * {@link ais.database.model.akunting.KelompokLaporan} dan
	 * {@link ais.database.model.akunting.MasterGrupLaporan} &mdash; jejak bahwa ketiga entity keluar
	 * dari satu cetakan hbm2java yang sama pada April 2010, bukan hasil perhitungan hash per kelas.
	 * Karena itu nilai ini tidak bisa dipakai untuk membedakan kelas satu dari yang lain.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * Kunci utama baris, dipetakan ke kolom <code>id</code> (IDENTITY, dibangkitkan basis data).
	 *
	 * <p>Dideklarasikan ulang di sini meskipun {@link ais.database.model.GeneralValueObject} juga
	 * punya field bernama sama: induk bukan <code>@MappedSuperclass</code> sehingga field-nya tidak
	 * pernah dipetakan Hibernate.</p>
	 */
	private Long id;
	/**
	 * Nama pengguna terakhir yang mengubah baris ini, diisi otomatis oleh
	 * <code>AuditTimestampInterceptor.ubah(...)</code> pada <code>@PreUpdate</code>.
	 */
	private String oleh;
	/**
	 * Identitas (user id) pengguna terakhir yang mengubah baris ini, pasangan dari {@link #oleh}.
	 */
	private String olehId;

	/**
	 * <b>Tujuan:</b> Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna terakhir, atau {@code null} bila baris belum pernah diubah lewat jalur yang
	 *         memasang stempel audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * <b>Tujuan:</b> Menetapkan id pengguna pengubah terakhir.
	 *
	 * <p><b>Perilaku non-obvious:</b> penetapan <b>diabaikan diam-diam</b> bila argumen {@code null}
	 * atau hanya berisi spasi. Penjaga ini dimaksudkan agar stempel audit lama tidak terhapus oleh
	 * proses latar yang tidak punya konteks pengguna, tetapi efek sampingnya: bila
	 * <code>AuditTimestampInterceptor.olehId()</code> mengembalikan nilai kosong (mis. perubahan
	 * terjadi di luar sesi web/token POS), baris tetap membawa id pengubah <em>sebelumnya</em> &mdash;
	 * stempel audit menjadi menyesatkan, bukan kosong.</p>
	 *
	 * @param olehId id pengguna pengubah; {@code null}/kosong diabaikan tanpa galat
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * <b>Tujuan:</b> Representasi teks ringkas baris, dipakai ZK saat menampilkan objek ini pada
	 * komponen yang tidak memiliki renderer khusus dan pada pesan debug.
	 *
	 * <p><b>Perilaku non-obvious:</b> method membaca field {@link #nama} <b>secara langsung</b>, bukan
	 * lewat {@link #getNama()}, sehingga nilai yang muncul <em>tidak</em> di-<code>trim</code> dan bisa
	 * berbeda dari yang tampil di grid. Untuk baris yang belum tersimpan, {@code id} masih
	 * {@code null} sehingga hasilnya berbentuk {@code "null-<nama>"}.</p>
	 *
	 * @return gabungan {@code id + "-" + nama}
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * <b>Tujuan:</b> Menetapkan nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, argumen {@code null} atau kosong
	 * <b>diabaikan diam-diam</b> sehingga nilai lama dipertahankan.</p>
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/kosong diabaikan tanpa galat
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * <b>Tujuan:</b> Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna terakhir, atau {@code null} bila belum pernah distempel
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * <b>Tujuan:</b> Kait daur hidup JPA yang memasang stempel audit tepat sebelum baris diperbarui.
	 *
	 * <p><b>Cara kerja:</b> dipanggil kontainer persistensi pada <code>@PreUpdate</code>, meneruskan
	 * {@code this} ke <code>ais.database.hibernate.AuditTimestampInterceptor.ubah(...)</code>. Helper
	 * itu memeriksa dulu apakah memang ada perubahan bisnis (lewat
	 * <code>AuditTrailHelper.peekUpdateDecision</code>); bila tidak ada, ia keluar tanpa menyentuh apa
	 * pun. Bila ada, ia memanggil {@link #setTanggal_dirubah(Date)}, {@link #setOleh(String)}, dan
	 * {@link #setOlehId(String)} &mdash; ketiganya lewat pengiriman polimorfik sehingga penjaga
	 * null/kosong pada dua setter terakhir tetap berlaku.</p>
	 *
	 * <p><b>Efek samping:</b> mengubah state objek ini selama flush. Method mengimplementasikan
	 * kontrak abstrak {@code onUpdate()} milik {@link ais.database.model.GeneralValueObject}.</p>
	 *
	 * <p><b>Catatan tata letak:</b> pada baris fisik yang sama juga dideklarasikan field
	 * {@code tanggal_dirubah} beserta nilai awalnya (waktu pembuatan objek di JVM, lewat
	 * {@code WaktuUtil.getDate()}). Bentuk satu-baris ini adalah hasil penyuntingan massal lintas
	 * ratusan entity; jangan dipecah tanpa alasan agar diff antar-entity tetap sebanding.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * <b>Tujuan:</b> Menetapkan waktu perubahan terakhir baris ini.
	 *
	 * <p>Berbeda dari {@link #setOleh(String)}/{@link #setOlehId(String)}, setter ini <b>tidak</b>
	 * punya penjaga null: memberi {@code null} benar-benar mengosongkan stempel waktu.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * <b>Tujuan:</b> Mengembalikan waktu perubahan terakhir baris ini.
	 *
	 * <p>Nilai awalnya adalah waktu objek Java dibuat, bukan waktu baris tersimpan &mdash; untuk baris
	 * yang tidak pernah di-<code>update</code>, kolom ini praktis berfungsi sebagai stempel
	 * &quot;dibuat pada&quot;.</p>
	 *
	 * @return waktu perubahan terakhir sebagai TIMESTAMP
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Nama jenis laporan sebagaimana diketik operator, mis. <code>&quot;Neraca&quot;</code>,
	 * <code>&quot;Rugi Laba&quot;</code>, <code>&quot;Arus Kas&quot;</code>.
	 *
	 * <p>Teks bebas tanpa daftar nilai sah. Beberapa konsumen memperlakukannya sebagai <b>semantik</b>
	 * lewat pencocokan substring &mdash; lihat tabel pada JavaDoc kelas.</p>
	 */
	private String nama;
	/**
	 * Keterangan/uraian panjang jenis laporan.
	 *
	 * <p>Bukan sekadar catatan: <code>PemetaanAkunHelper</code> menentukan apakah sebuah jenis laporan
	 * adalah &quot;NERACA&quot; atau &quot;RUGI LABA&quot; dari kolom INI (bukan dari {@link #nama}).
	 * Beberapa layar juga menampilkannya sebagai label combobox pendamping nama.</p>
	 */
	private String keterangan;
	/**
	 * Bendera &quot;tampilkan jenis laporan ini di dasbor akuntansi&quot;.
	 *
	 * <p>Bertipe {@code Boolean} (bukan {@code boolean}) sehingga bernilai tiga keadaan: benar, salah,
	 * dan <b>belum pernah diisi</b> ({@code null}). Keadaan ketiga itulah sumber ketidakcocokan yang
	 * dibahas pada {@link #getTampilDiDashboard()}. Tidak ada anotasi <code>@Column</code> pada
	 * getter-nya, jadi kolomnya dipetakan secara implisit oleh strategi penamaan bawaan Hibernate.</p>
	 */
	private Boolean tampilDiDashboard;

	// Bekas rancangan awal: ketiga jenis laporan pokok sempat direncanakan menjadi KONSTANTA kode
	// (dipakai mis. oleh MasterGrupLaporan.reloadDefault() yang juga sudah dikomentari). Seluruhnya
	// dinonaktifkan, sehingga Neraca/Rugi Laba/Arus Kas kini murni baris data bebas milik operator.
//	public static JenisLaporan NRC = null;
//	public static JenisLaporan RL = null;
//	public static JenisLaporan ARUS_KAS = null;

	/**
	 * <b>Tujuan (rancangan asli):</b> menyemai baris bawaan Neraca / Rugi&nbsp;Laba / Arus&nbsp;Kas ke
	 * basis data bila belum ada, sekaligus mengisi cache statis {@code NRC}, {@code RL}, dan
	 * {@code ARUS_KAS} agar kode lain bisa merujuk jenis laporan tanpa query.
	 *
	 * <p><b>Keadaan sesungguhnya hari ini: METHOD INI NO-OP TOTAL.</b> Seluruh badannya dikomentari
	 * &mdash; tidak ada query, tidak ada penyimpanan, tidak ada cache yang diisi. Method tetap ada
	 * hanya agar kedua pemanggilnya tetap dapat dikompilasi.</p>
	 *
	 * <p><b>Kapan/dari mana dipanggil:</b></p>
	 * <ol>
	 *   <li><code>ais.common.InitData</code> saat penyalaan aplikasi, didahului baris log
	 *       <code>System.out.println(&quot;reloadDefaults: JenisLaporan ...&quot;)</code>. <b>Log itu
	 *       berbohong</b>: pesan menyiratkan struktur bawaan sedang disemai, padahal tidak satu baris
	 *       pun pernah dibuat. Pola identik ditemukan pada
	 *       {@link ais.database.model.akunting.MasterGrupLaporan#reloadDefault()}.</li>
	 *   <li><code>JenisLaporanAction.doAfterCompose</code> setiap kali layar master dibuka &mdash;
	 *       JavaDoc di sana masih mengklaim method ini &quot;memperbarui cache statis jenis laporan di
	 *       memori&quot;; klaim itu <b>tidak lagi benar</b>.</li>
	 * </ol>
	 *
	 * <p><b>Konsekuensi operasional:</b> instalasi baru mulai dengan tabel
	 * <code>akunting.jenis_laporan</code> KOSONG. Sampai operator mengetikkan barisnya sendiri, seluruh
	 * laporan keuangan berbasis <code>KelompokLaporan</code> tidak menghasilkan apa pun, dan
	 * <code>PemetaanAkunHelper</code> menolak setiap usulan dengan pesan &quot;Jenis Laporan ... belum
	 * ada di master&quot;.</p>
	 *
	 * <p><b>Bila kelak dihidupkan kembali</b>, kode lama di bawah perlu ditulis ulang: ia memakai
	 * <code>HibernateUtil.currentNativeSession()</code> lalu menutup sesi lewat
	 * <code>HibernateUtil.closeSession()</code> &mdash; pola yang sudah terbukti melanggar kontrak
	 * pengelolaan sesi ZK (menutup sesi yang sedang dipakai layar) pada beberapa entity lain, dan
	 * <code>catch</code>-nya menelan seluruh exception.</p>
	 *
	 * <p><b>Efek samping saat ini:</b> tidak ada.</p>
	 */
	public static void reloadDefault() {
//		Session session = HibernateUtil.currentNativeSession();
//		try {
//			NRC = (JenisLaporan) session.createCriteria(JenisLaporan.class)
//
//					.add(Restrictions.or(Restrictions.ilike("nama", "Neraca", MatchMode.ANYWHERE),
//							Restrictions.or(Restrictions.ilike("nama", "Balance Sheet", MatchMode.ANYWHERE),
//									Restrictions.ilike("nama", "NRC", MatchMode.EXACT))))
//
//					.setMaxResults(1).uniqueResult();
//			if (NRC == null) {
//				NRC = new JenisLaporan();
//				NRC.setNama("NRC");
//				NRC.setKeterangan("Neraca");
//				session.getTransaction().begin();
//				session.save(NRC);
//				session.getTransaction().commit();
//			}
//			RL = (JenisLaporan) session.createCriteria(JenisLaporan.class)
//
//					.add(Restrictions.or(Restrictions.ilike("nama", "Laba", MatchMode.ANYWHERE),
//							Restrictions.or(Restrictions.ilike("nama", "Income Statement", MatchMode.ANYWHERE),
//									Restrictions.ilike("nama", "RL", MatchMode.EXACT))))
//
//					.setMaxResults(1).uniqueResult();
//			if (RL == null) {
//				RL = new JenisLaporan();
//				RL.setNama("RL");
//				RL.setKeterangan("Rugi Laba");
//				session.getTransaction().begin();
//				session.save(RL);
//				session.getTransaction().commit();
//			}
//			ARUS_KAS = (JenisLaporan) session.createCriteria(JenisLaporan.class)
//					.add(Restrictions.or(Restrictions.ilike("nama", "KAS", MatchMode.ANYWHERE),
//							Restrictions.ilike("nama", "Cash Flow", MatchMode.ANYWHERE)))
//					.setMaxResults(1).uniqueResult();
//			if (ARUS_KAS == null) {
//				ARUS_KAS = new JenisLaporan();
//				ARUS_KAS.setNama("ARUS KAS");
//				ARUS_KAS.setKeterangan("Arus Kas");
//				session.getTransaction().begin();
//				session.save(ARUS_KAS);
//				session.getTransaction().commit();
//			}
//		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/JenisLaporan.java:125");
//			e.printStackTrace();
//		}
//		HibernateUtil.closeSession();
	}

	/**
	 * <b>Tujuan:</b> Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA untuk instansiasi saat
	 * memuat baris, sekaligus dipakai layar master untuk membuat baris baru
	 * (<code>init(new JenisLaporan())</code>) dan oleh <code>Common.getContentAsObject</code>.
	 *
	 * <p>Tidak mengisi apa pun kecuali nilai awal {@code tanggal_dirubah} yang sudah ditetapkan pada
	 * deklarasi field.</p>
	 */
	public JenisLaporan() {
	}

	/**
	 * <b>Tujuan:</b> Mengembalikan kunci utama baris.
	 *
	 * <p>Anotasi <code>@Id</code> di sini yang menetapkan seluruh kelas memakai <b>property access</b>
	 * &mdash; fakta yang menjelaskan mengapa getter tanpa anotasi seperti
	 * {@link #getTampilDiDashboard()} tetap ikut dipetakan. Kolom bersifat
	 * <code>insertable = false</code> karena nilainya dibangkitkan basis data (IDENTITY).</p>
	 *
	 * <p>Id inilah yang dipakai sebagai parameter Jasper <code>jenis_laporan</code> pada laporan
	 * akuntansi, sebagai bagian kunci pencarian template <code>LampiranLain</code>, dan sebagai nilai
	 * kolom <code>kelompok_laporan.jenis_laporan</code>.</p>
	 *
	 * @return kunci utama, atau {@code null} bila baris belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * <b>Tujuan:</b> Menetapkan kunci utama. Dipakai Hibernate saat memuat baris; kode aplikasi
	 * sebaiknya tidak memanggilnya langsung.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * <b>Tujuan:</b> Mengembalikan nama jenis laporan.
	 *
	 * <p><b>Cara kerja:</b> nilai dikembalikan dalam bentuk sudah di-<code>trim</code>; {@code null}
	 * tetap {@code null}.</p>
	 *
	 * <p><b>Efek samping halus (penulisan &quot;hantu&quot;):</b> karena kelas ini memakai property
	 * access, Hibernate mengambil <em>snapshot</em> keadaan tersimpan langsung dari hasil query,
	 * sementara pemeriksaan kotor saat <em>flush</em> membaca nilai lewat getter ini. Bila kolom
	 * <code>nama</code> di basis data mengandung spasi di ujung, kedua nilai itu berbeda, baris dinilai
	 * kotor, dan Hibernate menerbitkan <code>UPDATE</code> yang menormalkan kolom &mdash; sekaligus
	 * membuat revisi Envers baru &mdash; hanya karena baris itu dibaca di dalam transaksi yang
	 * kemudian di-flush. Tidak merusak data (nilainya memang setara), tetapi menjelaskan munculnya
	 * revisi &quot;tanpa perubahan&quot; pada riwayat.</p>
	 *
	 * <p><b>Dipakai untuk semantik:</b> <code>LaporanKeuanganCoaHelper</code> memutuskan laporan
	 * bersifat kumulatif atau periodik dari hasil method ini; {@link #getTampilDiDashboard()} juga
	 * memanggilnya. Lihat JavaDoc kelas.</p>
	 *
	 * @return nama jenis laporan sudah ter-<code>trim</code>, atau {@code null}
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * <b>Tujuan:</b> Menetapkan nama jenis laporan.
	 *
	 * <p>Tanpa penjaga apa pun: nilai disimpan apa adanya (tidak di-<code>trim</code>, tidak divalidasi
	 * terhadap daftar nilai). Keunikan nama <b>tidak</b> dijamin basis data; satu-satunya pemeriksaan
	 * ada di lapisan UI (<code>JenisLaporanAction.checkNamaJenisLaporan()</code>, pembandingan
	 * <em>case-sensitive</em> yang juga rentan TOCTOU). Kolom bersifat <code>nullable = false</code>,
	 * jadi menyimpan tanpa mengisi nama akan gagal di tingkat basis data.</p>
	 *
	 * @param nama nama jenis laporan
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * <b>Tujuan:</b> Mengembalikan keterangan jenis laporan apa adanya (tanpa <code>trim</code>, tanpa
	 * substitusi).
	 *
	 * <p><b>Penting:</b> kolom inilah &mdash; bukan {@link #getNama()} &mdash; yang dibaca
	 * <code>PemetaanAkunHelper</code> untuk memutuskan sebuah jenis laporan tergolong
	 * &quot;NERACA&quot; atau &quot;RUGI LABA&quot;. Karena tidak ada nilai &quot;tidak dikenal&quot;
	 * pada cabang di sana, keterangan yang tidak memuat kata NERACA otomatis diperlakukan sebagai
	 * Rugi&nbsp;Laba.</p>
	 *
	 * @return keterangan jenis laporan, atau {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * <b>Tujuan:</b> Menetapkan keterangan jenis laporan.
	 *
	 * @param keterangan keterangan/uraian jenis laporan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * <b>Tujuan:</b> Menyatakan apakah jenis laporan ini ikut ditampilkan pada dasbor akuntansi.
	 *
	 * <p><b>Cara kerja:</b> bila kolomnya sudah pernah diisi, nilainya dikembalikan apa adanya. Bila
	 * masih {@code null} (baris baru, atau baris lama dari sebelum kolom ini ada), getter
	 * <b>menghitung nilai pengganti dari nama</b>: nama yang mengandung substring <code>&quot;kas&quot;</code>
	 * (tanpa memperhatikan besar-kecil huruf) dianggap {@code false}, selain itu {@code true}. Maksud
	 * aslinya jelas &mdash; menyembunyikan &quot;Arus Kas&quot; dari dasbor secara bawaan.</p>
	 *
	 * <p><b>Kuirk 1 &mdash; pencocokan terlalu longgar.</b> Pemeriksaannya <em>substring</em>, bukan
	 * kata utuh. Setiap nama yang kebetulan memuat rangkaian huruf &quot;kas&quot; ikut tersembunyi,
	 * termasuk yang sama sekali bukan laporan arus kas.</p>
	 *
	 * <p><b>Kuirk 2 &mdash; ketidakcocokan antara getter dan penyaring SQL (terverifikasi).</b>
	 * Konsumen yang menyaring lewat Hibernate Criteria memakai
	 * <code>Restrictions.eq(&quot;tampilDiDashboard&quot;, true)</code>, yang dieksekusi langsung ke
	 * KOLOM basis data dan karena itu <b>tidak pernah mencocokkan baris bernilai NULL</b>. Jadi untuk
	 * baris yang checkbox-nya belum pernah disentuh operator:</p>
	 * <ul>
	 *   <li>getter ini bilang &quot;tampilkan&quot; ({@code true}), tetapi</li>
	 *   <li><code>DasboardAkunting.init()</code> tidak pernah memuatnya (baris tidak lolos penyaring),
	 *       dan</li>
	 *   <li><code>DasboardAkuntansi.getJumlahJenisLaporanAktif()</code> tidak menghitungnya sehingga
	 *       kartu &quot;laporan aktif&quot; melaporkan angka yang terlalu kecil.</li>
	 * </ul>
	 * <p>Akibat praktisnya: jenis laporan yang baru dibuat <b>tidak muncul di dasbor</b> sampai
	 * operator mencentang lalu (kalau perlu) mencentang ulang checkbox &quot;Tampilkan Di
	 * Dashboard&quot; &mdash; padahal layar master menampilkan checkbox itu dalam keadaan <em>sudah
	 * tercentang</em> (nilainya berasal dari getter ini). Dari sudut pandang operator, dasbor tampak
	 * mengabaikan setelan yang sudah benar.</p>
	 *
	 * <p><b>Kuirk 3 &mdash; getter yang menulis balik.</b> Getter ini tidak beranotasi
	 * <code>@Transient</code>, jadi ia dipetakan sebagai properti persisten. Dengan property access,
	 * snapshot keadaan tersimpan berisi {@code null} sementara pemeriksaan kotor saat flush membaca
	 * nilai substitusi ({@code true}/{@code false}). Keduanya berbeda, sehingga <b>sekadar memuat
	 * baris di dalam transaksi yang kemudian di-flush cukup untuk membuat Hibernate menuliskan nilai
	 * bawaan itu secara permanen ke basis data</b> (plus satu revisi Envers). Setelah itu terjadi,
	 * baris tersebut mulai lolos penyaring SQL di atas &mdash; artinya perilaku dasbor bisa berubah
	 * sendiri tanpa ada operator yang mengubah apa pun, tergantung layar mana yang kebetulan pernah
	 * membaca baris tersebut lebih dulu. Ini juga berarti keputusan berbasis nama
	 * (&quot;mengandung&nbsp;kas&quot;) dapat <b>membeku</b> ke dalam data: mengganti nama baris
	 * setelah nilainya terlanjur tertulis tidak akan mengubah apa-apa lagi.</p>
	 *
	 * @return {@code true} bila jenis laporan ini ditampilkan di dasbor; {@code false} bila tidak.
	 *         Tidak pernah mengembalikan {@code null}.
	 */
	public Boolean getTampilDiDashboard() {
		return tampilDiDashboard == null ? (getNama() != null && getNama().toLowerCase().contains("kas") ? false : true)
				: tampilDiDashboard;
	}

	/**
	 * <b>Tujuan:</b> Menetapkan bendera tampil-di-dasbor.
	 *
	 * <p><b>Kapan/dari mana dipanggil:</b> satu-satunya pemanggil nyata adalah listener
	 * <code>onCheck</code> pada checkbox &quot;Tampilkan Di Dashboard&quot; di grid
	 * <code>JenisLaporanAction.JenisLaporanRenderer</code>, yang langsung menyusulnya dengan
	 * <code>Common.refreshSaveOrUpdate(...)</code> &mdash; perubahan tersimpan seketika tanpa tombol
	 * Simpan. Checkbox itu dinonaktifkan bagi pengguna tanpa hak UPDATE
	 * (<code>setDisabled(!edit)</code>), berbeda dari beberapa grid master lain di modul akunting yang
	 * membiarkan checkbox-nya tanpa gerbang.</p>
	 *
	 * <p>Checkbox senama pada toolbar <code>DasboardAkunting</code> <b>tidak</b> memanggil setter ini:
	 * ia hanya menambah/membuang objek dari daftar panel di memori dan tidak menyentuh basis data.</p>
	 *
	 * @param tampilDiDashboard {@code true} untuk menampilkan di dasbor, {@code false} untuk
	 *                          menyembunyikan; {@code null} mengembalikan baris ke perilaku bawaan
	 *                          berbasis nama (lihat {@link #getTampilDiDashboard()})
	 */
	public void setTampilDiDashboard(Boolean tampilDiDashboard) {
		this.tampilDiDashboard = tampilDiDashboard;
	}

}
