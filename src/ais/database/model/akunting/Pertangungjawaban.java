package ais.database.model.akunting;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
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
import org.json.JSONArray;

import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * <h2>Pertangungjawaban &mdash; dokumen Laporan Pertanggungjawaban (LPJ) uang muka pegawai</h2>
 *
 * <p><b>Peran.</b> Entity ini adalah <b>dokumen penutup</b> dari siklus panjar (uang muka)
 * pegawai. Setelah seorang pegawai menerima pencairan uang muka
 * ({@link ais.database.model.akunting.UangMuka}) dan melaksanakan kegiatannya, ia wajib
 * melaporkan <i>bagaimana uang itu dipakai</i>. Laporan itulah yang disimpan di baris
 * <code>akunting.pertangungjawaban</code> ini: satu baris = satu LPJ atas satu dokumen
 * uang muka. Rincian belanjanya sendiri tidak disimpan sebagai tabel anak melainkan sebagai
 * <b>dokumen JSON</b> di kolom <code>formula</code> (lihat {@link #getFormula()}); kolom
 * {@link #getNilai() nilai}, {@link #getPajak() pajak} dan {@link #getDikembalikan()
 * dikembalikan} adalah ringkasan hasil hitung ulang atas JSON tersebut.</p>
 *
 * <p><b>Ejaan.</b> Nama kelas, tabel dan seluruh kolom memakai ejaan &quot;Pertangungjawaban&quot;
 * (satu <code>g</code>). Ini <i>salah eja</i> dibanding Bahasa Indonesia baku
 * (&quot;pertanggungjawaban&quot;) tetapi dipakai konsisten di seluruh repo &mdash; nama tabel,
 * nama kolom FK di {@code GrupTransaksi}/{@code Pajak}/{@code DaftarPengajuanTransfer},
 * nama berkas ZUL, dan nama kelas Action. Jangan &quot;memperbaiki&quot; ejaannya tanpa migrasi
 * skema menyeluruh.</p>
 *
 * <h3>1. Posisi dalam rantai dokumen keuangan</h3>
 *
 * <pre>
 *   JenisUangMuka (master, memegang bagan akun)
 *        |
 *   UangMuka  ......  pengajuan panjar, disetujui, dicairkan
 *        |               |
 *        |               +--&gt; DaftarPengajuanTransfer --&gt; ProsesTransfer (realisasi bank)
 *        |
 *   Pertangungjawaban (KELAS INI) ..... LPJ atas pemakaian panjar
 *        |            |             |
 *        |            |             +--&gt; Pajak (baris PPh per item formula, dibuat Pajak.buat)
 *        |            +--&gt; DaftarPengajuanTransfer (bila LPJ menagih kekurangan)
 *        +--&gt; GrupTransaksi (jurnal; kolom FK grup_transaksi.pertangungjawaban)
 * </pre>
 *
 * <p><b>Relasi ke {@link ais.database.model.akunting.UangMuka} bersifat dua arah lewat DUA kolom
 * FK yang saling independen</b> dan tanpa <code>mappedBy</code>:</p>
 * <ul>
 *   <li><code>pertangungjawaban.uang_muka</code> &rarr; dipetakan oleh {@link #getUangMuka()}
 *       di kelas ini, <b>wajib</b> (<code>nullable = false</code>);</li>
 *   <li><code>uang_muka.pertangungjawaban</code> &rarr; dipetakan oleh
 *       <code>UangMuka.getPertangungjawaban()</code>, opsional.</li>
 * </ul>
 * <p>Kedua kolom ditulis di tempat yang berbeda: kolom di sini ditulis saat LPJ disimpan
 * (<code>PertangungjawabanAction.onSave</code> / <code>PertangungjawabanApiHelper.simpan</code>),
 * sedangkan kolom balik di <code>UangMuka</code> ditulis <i>belakangan</i> oleh timer ZK
 * (<code>Common.createDefaultTimer</code>) sesudah simpan. Konsekuensinya: bila timer itu gagal
 * atau sesi ditutup lebih dulu, <b>arah balik bisa kosong sementara arah maju sudah terisi</b>.
 * Kode konsumen karenanya tidak boleh menganggap kedua kolom selalu konsisten &mdash; gunakan arah
 * yang relevan untuk pertanyaan yang sedang diajukan.</p>
 *
 * <h3>2. Siklus hidup LPJ</h3>
 * <ol>
 *   <li><b>Pengajuan.</b> Pegawai membuka menu &quot;Pertanggungjawaban Uang Muka&quot;
 *       (<code>/pages/master/akunting/pertangungjawaban.zul</code>), memilih dokumen
 *       <code>UangMuka</code> miliknya yang sudah disetujui &amp; direalisasikan, lalu mengisi
 *       rincian belanja baris-per-baris. Rincian itu diserialisasi ke {@link #getFormula()}.
 *       Nomor agenda dibentuk otomatis (<code>generateCode</code>) dan disimpan di
 *       {@link #getKode()}; {@link #getStatus()} awal = {@link #PENGAJUAN}.</li>
 *   <li><b>Persetujuan.</b> Pejabat berwenang membuka menu terpisah
 *       &quot;Persetujuan Pertanggungjawaban Uang Muka&quot;
 *       (<code>persetujuan_pertangungjawaban.zul</code> &rarr;
 *       <code>PersetujuanPertangungjawabanAction</code>, yang hanya memanggil
 *       <code>super(true)</code>). Di mode itu radiogroup Status muncul dan approver memilih
 *       {@link #DISETUJU} / {@link #DITOLAK}. Alternatifnya, seluruh langkah persetujuan dapat
 *       dijalankan lewat mesin SOP ({@link ais.database.model.sop.DisposisiSop}), dan bila
 *       disposisi ada maka <i>jejak SOP mengalahkan kolom</i> (lihat bagian 4).</li>
 *   <li><b>Penjurnalan (3 cap, bagian 3).</b></li>
 *   <li><b>Pengembalian sisa panjar.</b> Bila belanja lebih kecil dari panjar,
 *       {@link #getDikembalikan()} bernilai positif; pegawai menyetor sisanya dan
 *       {@link #getTanggalStor()} / {@link #getTelahDikembalikan()} /
 *       {@link #getTanggalDikembalikan()} menandainya.</li>
 * </ol>
 *
 * <h3>3. Mekanisme TIGA cap posting (terverifikasi)</h3>
 *
 * <p>Entity ini adalah <b>dokumen ber-kaki-ganda</b>: satu baris LPJ dapat melahirkan tiga
 * kelompok jurnal yang <i>diposting terpisah, oleh tiga layar berbeda</i>, dan masing-masing
 * meninggalkan cap {@link ais.database.model.akunting.PostingHistory} sendiri di tiga kolom FK
 * kelas ini. Berikut status masing-masing kaki <b>sebagaimana diverifikasi dari kode saat
 * dokumentasi ini ditulis</b>:</p>
 *
 * <table border="1" summary="Tiga cap posting Pertangungjawaban">
 *   <tr><th>Kolom</th><th>Layar yang menulis</th><th>Jenis PostingHistory</th><th>Jurnal</th></tr>
 *   <tr>
 *     <td>{@link #getPostingHistory() posting_history}</td>
 *     <td><code>PostingPertangungjawabanAction</code> (menu &quot;Posting Pertanggungjawaban
 *         Uang Muka&quot;)</td>
 *     <td><code>PostingHistory.JENIS_PERTANGGUNGJAWABAN_UANG_MUKA</code></td>
 *     <td>Dr <code>UangMuka.akun</code> sebesar nilai realisasi (+ sponsor), Cr
 *         <code>JenisUangMuka.akun</code> sebesar nilai realisasi dikurangi total PPh, plus
 *         <code>JenisUangMuka.akunSponsor</code> dan &mdash; bila realisasi lebih kecil dari
 *         panjar &mdash; <code>JenisUangMuka.akunKelebihan</code> untuk selisihnya.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #getPostingHistoryPajak() posting_history_pajak}</td>
 *     <td><b>TIDAK ADA</b></td>
 *     <td>&mdash;</td>
 *     <td><b>Kolom mati.</b> Lihat peringatan di bawah.</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #getPostingHistoryPengembalian() posting_history_pengembalian}</td>
 *     <td><code>PostingPertangungjawabanPengembalianAction</code> (menu &quot;Posting
 *         Pengembalian Uang Muka&quot;)</td>
 *     <td><code>PostingHistory.JENIS_PENGEMBALIAN_UANG_MUKA</code></td>
 *     <td>Dr <code>JenisUangMuka.akunKelebihan</code> / Cr <code>JenisUangMuka.akun</code>
 *         sebesar {@link #getDikembalikan()}.</td>
 *   </tr>
 * </table>
 *
 * <p><b>Peringatan &mdash; kaki pajak tidak memakai kolom ini.</b> Penelusuran seluruh repo
 * menunjukkan {@link #setPostingHistoryPajak(PostingHistory)} <b>nol pemanggil</b> di luar entity
 * ini sendiri. Layar <code>PostingPertangungjawabanPajakAction</code> memang ada dan memang
 * membuat <code>PostingHistory.JENIS_PERTANGGUNGJAWABAN_PAJAK</code>, tetapi ia bekerja atas
 * entity {@link ais.database.model.akunting.Pajak} dan mencapkan hasilnya ke
 * <code>Pajak.postingHistory</code>, <i>bukan</i> ke kolom di sini. Akibatnya
 * <code>pertangungjawaban.posting_history_pajak</code> praktis selalu NULL. Kolom ini tetap
 * <b>dibaca</b> di satu tempat: penjaga hapus di
 * <code>PertangungjawabanApiHelper</code>/<code>PertangungjawabanKasBesarApiHelper</code>
 * (&quot;sudah dijurnal sehingga tidak boleh dihapus&quot;) memeriksa ketiga kolom sekaligus &mdash;
 * sehingga penjaga itu <b>tidak pernah menyala lewat jalur pajak</b> dan LPJ yang pajaknya sudah
 * dijurnal masih bisa dihapus sepanjang dua kolom lain kosong. Temuan yang sama sudah dicatat dari
 * sisi {@link ais.database.model.akunting.Pajak}.</p>
 *
 * <p><b>Peringatan &mdash; pembatalan posting meninggalkan baris jurnal yatim.</b>
 * <code>PostingPertangungjawabanAction.onBatalkanPostingSemua</code> hanya menjalankan
 * <code>delete from akunting.grup_transaksi where ref is null and pertangungjawaban = ? and
 * closing is null</code> lalu menyetel {@link #setPostingHistory(PostingHistory)} ke
 * <code>null</code>. Baris anak di <code>akunting.transaksi</code> (debit/kredit per akun)
 * <b>tidak ikut dihapus</b>, sehingga rangkaian batal&nbsp;&rarr;&nbsp;posting ulang dapat
 * menggandakan isi buku besar. Pola ini identik dengan yang sudah dicatat untuk
 * {@link ais.database.model.akunting.UangMuka}.</p>
 *
 * <h3>4. Getter yang MENULIS BALIK (property access + dynamicUpdate)</h3>
 *
 * <p>Kelas ini beranotasi <code>@org.hibernate.annotations.Entity(dynamicInsert = true,
 * dynamicUpdate = true)</code> dan dipetakan dengan <b>property access</b> (anotasi ada di
 * getter). Artinya Hibernate memanggil getter untuk membaca <i>state persisten</i>, dan
 * setiap getter yang mengubah field instance <b>ikut tersimpan</b> pada flush berikutnya bila
 * objeknya masih <i>managed</i>. Getter berikut bukan getter murni:</p>
 * <ul>
 *   <li>{@link #getAktif()} &mdash; memaksa <code>aktif = true</code> begitu status
 *       {@link #DISETUJU}; LPJ yang sudah dinonaktifkan (soft-delete) dapat <b>hidup lagi</b>
 *       hanya karena dibaca.</li>
 *   <li>{@link #getDibuatOleh()}, {@link #getDisetujuiOleh()}, {@link #getTanggalPersetujuan()},
 *       {@link #getStatus()} &mdash; menimpa kolom dengan jejak
 *       {@link ais.database.model.sop.DisposisiSop}. {@link #getDisetujuiOleh()} dan
 *       {@link #getTanggalPersetujuan()} bahkan <b>meng-NULL-kan</b> cap persetujuan ketika
 *       disposisi ada tetapi langkah &quot;setuju&quot;-nya belum terisi &mdash; sekadar membaca
 *       dokumen yang sudah disetujui secara manual dapat <b>menghapus jejak persetujuannya</b>
 *       dan menurunkan {@link #getStatus()} dari {@link #DISETUJU} kembali ke
 *       {@link #PENGAJUAN}.</li>
 *   <li>{@link #getSatuanKerja()} &mdash; selalu menyalin ulang dari
 *       <code>uangMuka.getSatuanKerja()</code>; kolom unit kerja LPJ tidak pernah berdiri
 *       sendiri (lihat bagian 5 soal cakupan).</li>
 *   <li>{@link #getKodeUnik()} &mdash; menghitung ulang setiap kali dibaca, walaupun kolomnya
 *       <code>unique</code>.</li>
 *   <li>{@link #getTanggalTransaksi()} &mdash; menurunkan tanggal dari rantai
 *       <code>DaftarPengajuanTransfer</code>.</li>
 *   <li>{@link #getTanggalDikembalikan()} dan {@link #getTanggalStor()} &mdash; mengisi/menghapus
 *       tanggal berdasarkan nilai kolom lain (lihat Javadoc masing-masing).</li>
 *   <li>{@link #getTahun()}, {@link #getBulan()}, {@link #getNomorSuratAlurKeuangan()} &mdash;
 *       mengisi default bila kosong.</li>
 * </ul>
 * <p>Praktik aman: jangan panggil getter-getter ini dari jalur render/laporan yang tidak
 * bermaksud mengubah data, atau lakukan di sesi read-only / atas objek yang sudah
 * <i>detached</i>.</p>
 *
 * <h3>5. Cakupan data (tenant) dan hak akses &mdash; catatan audit</h3>
 * <ul>
 *   <li><b>Tidak ada kolom <code>sekolah</code>/<code>yayasan</code> sama sekali.</b> Satu-satunya
 *       pembatas adalah {@link #getSatuanKerja()}, dan itu pun diturunkan dari
 *       <code>UangMuka</code>.</li>
 *   <li><b>Fail-open cakupan.</b> <code>PertangungjawabanAction.initCriteria</code> membangun
 *       filter <code>Restrictions.or(Restrictions.isNull(&quot;satuanKerja&quot;), ...)</code>
 *       sehingga <b>setiap LPJ dengan <code>satuan_kerja</code> NULL terlihat oleh semua
 *       pengguna</b>; dan bila <code>SekolahUtil.ambilSatuanKerjas()</code> mengembalikan
 *       himpunan kosong, seluruh klausa unit kerja berubah menjadi <code>1=1</code> sehingga
 *       <b>semua LPJ seluruh instalasi</b> tampil. Karena {@link #getSatuanKerja()} menyalin dari
 *       <code>UangMuka</code>, satu dokumen panjar tanpa unit kerja cukup untuk membuat LPJ-nya
 *       ikut bocor.</li>
 *   <li><b>Pewarisan hak lewat menu induk.</b> Layar posting (tiga kaki di atas), layar
 *       persetujuan, dan tab-tab dasbor keuangan yang menampilkan LPJ tidak semuanya terdaftar
 *       sebagai menu tersendiri; hak yang dicek adalah hak menu tempat tab itu disisipkan.</li>
 *   <li><b>Broken access control pada persetujuan (KONFIRMASI pola lintas modul &quot;uang&quot;).</b>
 *       <code>PertangungjawabanAction.doAfterCompose</code> membaca
 *       <code>execution.getParameter(&quot;persetujuan&quot;)</code> dari URL dan langsung
 *       memakai nilainya sebagai mode halaman. Menu &quot;Pertanggungjawaban Uang Muka&quot; dan
 *       &quot;Persetujuan Pertanggungjawaban Uang Muka&quot; adalah <b>dua entri menu terpisah
 *       dengan hak terpisah</b> (lihat <code>MenuSnapshotData</code>), tetapi pemilik hak menu
 *       pertama dapat menambahkan <code>?persetujuan=true</code> pada URL dan memperoleh
 *       radiogroup Status &mdash; tanpa pemeriksaan peran, atasan, batas nominal, maupun larangan
 *       menyetujui pengajuan sendiri. Jalur REST <code>PertangungjawabanApiHelper.simpan</code>
 *       memiliki celah setara: gerbangnya hanya <code>create</code>/<code>update</code>, namun ia
 *       menerima <code>statusDokumen = &quot;Disetujui&quot;</code> dari klien dan mencapkan
 *       <code>disetujuiOleh = pengguna itu sendiri</code>. Selain itu <code>bolehAksi</code>
 *       di helper tersebut <b>fail-open</b>: pengguna tanpa role sama sekali dianggap boleh
 *       melakukan semua aksi, termasuk <code>approve</code>. Kontras positif: jalur UI baru
 *       (<code>NewUiPersetujuanAkuntingController</code>) hanya read-only dan dijaga
 *       <code>NewUiRouteGuard</code>.</li>
 * </ul>
 *
 * <h3>6. Integritas nominal &mdash; catatan penting</h3>
 * <ul>
 *   <li><b>Rumus sisa panjar tidak seragam antar jalur.</b> Layar ZK menghitung
 *       <code>dikembalikan = uangMuka.nilai + dariSponsor &minus; nilaiRealisasi</code>
 *       (benar). Jalur REST <code>PertangungjawabanApiHelper.simpan</code> menghitung
 *       <code>dikembalikan = uangMuka.nilai &minus; nilaiRealisasi</code> &mdash; <b>dana sponsor
 *       tidak diperhitungkan</b>, sehingga sisa yang wajib disetor kembali dilaporkan lebih kecil
 *       dari seharusnya.</li>
 *   <li><b>Tombol &quot;Hitung Ulang&quot; memakai basis yang salah.</b> Pada
 *       <code>PertangungjawabanAction</code> penyapu massal menghitung
 *       <code>dikembalikan = <u>pertangungjawaban.getNilai()</u> + dariSponsor &minus;
 *       nilaiBaru</code>, yakni memakai <i>nilai LPJ lama</i> sebagai pengganti nilai panjar.
 *       Karena blok itu hanya dieksekusi ketika nilai lama dan baru <i>berbeda</i>, hasilnya
 *       adalah selisih antar-revisi LPJ, bukan sisa panjar. Angka inilah yang kemudian dijurnal
 *       apa adanya oleh <code>PostingPertangungjawabanPengembalianAction</code>
 *       (<code>nilai = pj.getDikembalikan()</code>).</li>
 *   <li><b>Basis pajak.</b> Nilai realisasi per item dihitung
 *       <code>(jumlah + jumlah&times;ppn%) &minus; (pph_mengurangi_lpj ? pph : 0)</code>, dan PPh
 *       dihitung dari <code>jumlah</code> mentah. Sama seperti yang dicatat pada
 *       {@link ais.database.model.akunting.Pajak}, <b>potongan harga tidak menjadi bagian dari
 *       basis</b>; bila item LPJ berdiskon, DPP yang dipakai untuk PPh/PPN lebih besar daripada
 *       nilai yang benar-benar dibayar.</li>
 *   <li><b>Tarif dibaca live.</b> Persentase PPh diambil dari master
 *       <code>JenisPajakBarang.getPersen()</code> pada saat hitung ulang, bukan disnapshot ke
 *       JSON. Mengubah tarif master akan mengubah nominal LPJ lama begitu tombol
 *       &quot;Hitung Ulang&quot; ditekan, sementara jurnal yang sudah terbit tidak ikut berubah.</li>
 * </ul>
 *
 * <h3>7. Pengelompokan anggota kelas</h3>
 * <ul>
 *   <li><b>Identitas &amp; penamaan:</b> {@link #getId()}, {@link #getKode()},
 *       {@link #getKodeUnik()}, {@link #getNama()}, {@link #getKeterangan()},
 *       {@link #toString()}.</li>
 *   <li><b>Nominal:</b> {@link #getNilai()}, {@link #getPajak()}, {@link #getDikembalikan()},
 *       {@link #getDariSponsor()}, {@link #getNamaSponsor()}.</li>
 *   <li><b>Rincian belanja:</b> {@link #getFormula()}, {@link #DEFAULT_FORMULA}.</li>
 *   <li><b>Alur persetujuan:</b> {@link #PENGAJUAN}, {@link #DISETUJU}, {@link #DITOLAK},
 *       {@link #getStatus()}, {@link #getDibuatOleh()}, {@link #getDisetujuiOleh()},
 *       {@link #getTanggalPembuatan()}, {@link #getTanggalPersetujuan()},
 *       {@link #getTanggalPersetujuanManual()}, {@link #getDisposisiSop()},
 *       {@link #getAktif()}.</li>
 *   <li><b>Relasi dokumen:</b> {@link #getUangMuka()}, {@link #getSatuanKerja()},
 *       {@link #getDaftarPengajuanTransfer()}, {@link #getNomorSuratAlurKeuangan()}.</li>
 *   <li><b>Cap posting:</b> {@link #getPostingHistory()}, {@link #getPostingHistoryPajak()},
 *       {@link #getPostingHistoryPengembalian()}.</li>
 *   <li><b>Pengembalian sisa:</b> {@link #getTelahDikembalikan()},
 *       {@link #getTanggalDikembalikan()}, {@link #getTanggalStor()}.</li>
 *   <li><b>Periode &amp; waktu:</b> {@link #getTahun()}, {@link #getBulan()},
 *       {@link #getTanggalTransaksi()}, {@link #getTanggal_dirubah()}.</li>
 *   <li><b>Jejak audit ringan:</b> {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #onUpdate()}.</li>
 * </ul>
 *
 * <h3>8. Catatan teknis pewarisan</h3>
 * <p>Rantai warisannya adalah <code>Pertangungjawaban extends {@link DataSop} extends
 * {@link ais.database.model.GeneralValueObject}</code>. <b>Keduanya kelas abstrak biasa &mdash;
 * bukan <code>@Entity</code> dan bukan <code>@MappedSuperclass</code></b>, sehingga Hibernate
 * <b>tidak</b> memetakan properti milik kelas induk. Field seperti {@link #oleh},
 * {@link #olehId} dan {@link #tanggal_dirubah} karena itu <b>sengaja dideklarasikan ulang</b> di
 * sini; itu keharusan teknis, bukan duplikasi yang perlu dirapikan.
 * {@link DataSop} sendiri hanya mengumumkan kontrak abstrak
 * {@link #getDisposisiSop()}/{@link #setDisposisiSop(DisposisiSop)}, sedangkan utilitas statis
 * {@link ais.database.model.GeneralValueObject#check(Object)} dipakai di banyak getter kelas ini
 * untuk mengganti proxy Hibernate yatim/basi dengan instance yang aman dibaca.</p>
 *
 * <p>Kelas ini juga beranotasi {@link org.hibernate.envers.Audited}, sehingga <b>setiap versi
 * baris disalin permanen</b> ke tabel revisi <code>akunting.pertangungjawaban_aud</code>.
 * Menghapus atau meralat baris utama tidak menghapus jejak versi lamanya.</p>
 *
 * @see ais.database.model.akunting.UangMuka
 * @see ais.database.model.akunting.Pajak
 * @see ais.database.model.akunting.PostingHistory
 * @see ais.database.model.akunting.GrupTransaksi
 * @see ais.database.model.akunting.PertangungjawabanKasBesar
 * @see ais.database.model.sop.DisposisiSop
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "akunting", name = "pertangungjawaban")
public class Pertangungjawaban extends DataSop {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama <code>akunting.pertangungjawaban.id</code>; lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang menyentuh baris ini; lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang menyentuh baris ini; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang menyimpan baris ini.
	 *
	 * @return id pengguna, atau <code>null</code> bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna terakhir yang menyimpan baris ini.
	 *
	 * <p><b>Non-obvious:</b> setter ini <b>menolak</b> nilai <code>null</code> maupun string
	 * kosong/blanko dan langsung <code>return</code> tanpa mengubah apa pun. Jadi kolom ini
	 * hanya bisa <i>ditimpa</i> oleh id lain, tidak pernah bisa dikosongkan kembali. Pola yang
	 * sama dipakai di {@link #setOleh(String)} dan {@link #setDisposisiSop(DisposisiSop)}.</p>
	 *
	 * @param olehId id pengguna; <code>null</code>/kosong diabaikan diam-diam
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna terakhir yang menyimpan baris ini.
	 *
	 * <p><b>Non-obvious:</b> sama seperti {@link #setOlehId(String)}, nilai <code>null</code>
	 * atau blanko diabaikan diam-diam sehingga nama lama tetap bertahan.</p>
	 *
	 * @param oleh nama pengguna; <code>null</code>/kosong diabaikan
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang menyimpan baris ini.
	 *
	 * @return nama pengguna, atau <code>null</code> bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA yang dijalankan tepat sebelum baris ini di-UPDATE.
	 *
	 * <p><b>Tujuan:</b> mendelegasikan pencatatan jejak audit ringan ke
	 * <code>ais.database.hibernate.AuditTimestampInterceptor.ubah(this)</code>, yang mengisi
	 * {@link #setOleh(String)}, {@link #setOlehId(String)} dan
	 * {@link #setTanggal_dirubah(Date)} dari konteks pengguna aktif.</p>
	 *
	 * <p><b>Efek samping:</b> mengubah tiga field instance di atas sesaat sebelum SQL UPDATE
	 * dibentuk, sehingga nilainya ikut tersimpan. Karena dipanggil oleh provider JPA, method ini
	 * tidak boleh dipanggil manual dari kode aplikasi.</p>
	 *
	 * <p><b>Kasus tepi:</b> callback ini <i>tidak</i> berjalan pada INSERT (tidak ada
	 * <code>@PrePersist</code>), juga tidak berjalan pada UPDATE massal lewat
	 * <code>createSQLQuery(...)</code>/HQL bulk &mdash; dan di kelas ini pembatalan posting
	 * memang memakai SQL mentah.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir; lihat {@link #getTanggal_dirubah()}. Diinisialisasi ke
	 * waktu pembuatan objek Java (bukan waktu simpan) lewat {@code WaktuUtil.getDate()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh <code>null</code>
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * <p><b>Non-obvious:</b> nilai ini dipakai ulang sebagai <i>fallback</i> tanggal setor oleh
	 * {@link #getTanggalStor()}, sehingga &quot;tanggal setor&quot; sebuah LPJ dapat berakhir
	 * sama dengan waktu penyuntingan terakhirnya, bukan waktu penyetoran sesungguhnya.</p>
	 *
	 * @return waktu perubahan terakhir; default waktu objek dibentuk
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Status awal sebuah LPJ: sudah diajukan, menunggu keputusan approver.
	 * Nilai literal <code>&quot;Pengajuan&quot;</code> disimpan apa adanya di kolom
	 * <code>status</code> dan dibandingkan dengan <code>equals</code> di banyak tempat.
	 */
	public static final String PENGAJUAN = "Pengajuan";
	/**
	 * Status LPJ yang telah disetujui. Perhatikan ejaannya: <code>&quot;Disetujui&quot;</code>
	 * (nilai), sedangkan nama konstantanya <code>DISETUJU</code> tanpa <code>i</code>. Nilai ini
	 * identik dengan konstanta bernama sama di {@code UangMuka}/{@code DanaTalangan}, dan
	 * <code>PertangungjawabanAction.onSave</code> memang membandingkannya dengan
	 * <code>DanaTalangan.DISETUJU</code> &mdash; aman karena string-nya sama persis.
	 */
	public static final String DISETUJU = "Disetujui";
	/**
	 * Status LPJ yang ditolak approver. Menyetel status ini lewat {@link #setStatus(String)}
	 * sekaligus membersihkan {@link #setDisetujuiOleh(Tbmuser)} dan
	 * {@link #setTanggalPersetujuan(Date)}.
	 */
	public static final String DITOLAK = "Ditolak";

	/**
	 * Representasi teks singkat untuk combobox, log, dan grid ZK.
	 *
	 * <p><b>Non-obvious:</b> memakai field <code>nama</code> <i>mentah</i> (bukan
	 * {@link #getNama()}), sehingga <b>tidak</b> memanfaatkan fallback ke nama uang muka dan
	 * dapat menghasilkan teks seperti <code>&quot;123-null&quot;</code> untuk LPJ yang namanya
	 * belum diisi. Untuk id yang belum ter-generate hasilnya diawali <code>&quot;null-&quot;</code>.</p>
	 *
	 * @return gabungan <code>id + &quot;-&quot; + nama</code>
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nomor agenda LPJ; lihat {@link #getKode()}. */
	private String kode;
	/** Rincian belanja dalam bentuk JSON array; lihat {@link #getFormula()}. */
	private String formula;
	/** Judul LPJ; lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas; lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Dokumen panjar yang dipertanggungjawabkan; lihat {@link #getUangMuka()}. */
	private UangMuka uangMuka;
	/** Total nilai realisasi belanja; lihat {@link #getNilai()}. */
	private Double nilai;
	/** Total PPh yang melekat pada rincian; lihat {@link #getPajak()}. */
	private Double pajak;
	/** Sisa panjar yang wajib disetor kembali; lihat {@link #getDikembalikan()}. */
	private Double dikembalikan;
	/** Bendera aktif/soft-delete; lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** Pengaju LPJ; lihat {@link #getDibuatOleh()}. */
	private Tbmuser dibuatOleh;
	/** Penyetuju LPJ; lihat {@link #getDisetujuiOleh()}. */
	private Tbmuser disetujuiOleh;
	/** Waktu persetujuan; lihat {@link #getTanggalPersetujuan()}. */
	private Date tanggalPersetujuan;
	/** Waktu pengajuan; lihat {@link #getTanggalPembuatan()}. */
	private Date tanggalPembuatan;
	/** Status alur ({@link #PENGAJUAN}/{@link #DISETUJU}/{@link #DITOLAK}); lihat {@link #getStatus()}. */
	private String status;
	/** Unit kerja pemilik LPJ; lihat {@link #getSatuanKerja()}. */
	private SatuanKerja satuanKerja;
	/** Kontribusi dana pihak ketiga; lihat {@link #getDariSponsor()}. */
	private Double dariSponsor;
	/** Nama pihak ketiga penyumbang dana; lihat {@link #getNamaSponsor()}. */
	private String namaSponsor;
	/** Jejak langkah SOP; lihat {@link #getDisposisiSop()}. */
	private DisposisiSop disposisiSop;

	/** Cap posting kaki utama LPJ; lihat {@link #getPostingHistory()}. */
	private PostingHistory postingHistory;
	/** Cap posting kaki pajak &mdash; <b>kolom mati</b>; lihat {@link #getPostingHistoryPajak()}. */
	private PostingHistory postingHistoryPajak;
	/** Cap posting kaki pengembalian sisa; lihat {@link #getPostingHistoryPengembalian()}. */
	private PostingHistory postingHistoryPengembalian;
	/** Tahun periode dokumen; lihat {@link #getTahun()}. */
	private Integer tahun;
	/** Seri penomoran surat alur keuangan; lihat {@link #getNomorSuratAlurKeuangan()}. */
	private NomorSuratAlurKeuangan nomorSuratAlurKeuangan;
	/** Bulan periode dokumen (1&ndash;12); lihat {@link #getBulan()}. */
	private Integer bulan;
	/** Daftar pengajuan transfer terkait; lihat {@link #getDaftarPengajuanTransfer()}. */
	private DaftarPengajuanTransfer daftarPengajuanTransfer;
	/** Tanggal efektif transaksi kas/bank; lihat {@link #getTanggalTransaksi()}. */
	private Date tanggalTransaksi;

	/** Penanda sisa panjar sudah dikembalikan; lihat {@link #getTelahDikembalikan()}. */
	private Boolean telahDikembalikan;
	/** Tanggal sisa panjar dikembalikan; lihat {@link #getTanggalDikembalikan()}. */
	private Date tanggalDikembalikan;

	/** Tanggal setor sisa panjar ke kas; lihat {@link #getTanggalStor()}. */
	private Date tanggalStor;

	/**
	 * Konstruktor tanpa argumen yang dibutuhkan Hibernate/JPA untuk instansiasi dan proxy.
	 *
	 * <p>Semua field dibiarkan pada nilai default kecuali {@link #tanggal_dirubah} yang
	 * diinisialisasi pada deklarasi field ke waktu sekarang.</p>
	 */
	public Pertangungjawaban() {
	}

	/**
	 * Mengembalikan kunci utama baris LPJ ini.
	 *
	 * <p>Kolom <code>id</code> memakai <code>IDENTITY</code> (auto-increment database) dan
	 * ditandai <code>insertable = false</code> sehingga nilainya diisi database, bukan aplikasi.
	 * Bernilai <code>null</code> selama objek belum pernah disimpan.</p>
	 *
	 * @return id baris, atau <code>null</code> untuk objek baru
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Hanya dipanggil oleh Hibernate dan oleh kode yang sengaja
	 * merekonstruksi referensi; jangan diubah untuk baris yang sudah persisten.
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nomor agenda LPJ (mis. hasil <code>generateCode</code> pada layar
	 * pengajuan), sudah dipangkas spasi.
	 *
	 * <p><b>Kasus tepi:</b> string kosong/blanko dinormalkan menjadi <code>null</code>. Karena
	 * {@link #getKodeUnik()} merangkai nilai ini apa adanya, LPJ tanpa kode menghasilkan kunci
	 * unik berawalan literal <code>&quot;null&quot;</code>.</p>
	 *
	 * @return nomor agenda tanpa spasi tepi, atau <code>null</code> bila kosong
	 */
	public String getKode() {
		return this.kode == null || kode.trim().isEmpty() ? null : this.kode.trim();
	}

	/**
	 * Menyetel nomor agenda LPJ.
	 *
	 * @param kode nomor agenda; disimpan apa adanya (pemangkasan dilakukan di getter)
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan judul LPJ.
	 *
	 * <p><b>Non-obvious:</b> bila kolom <code>nama</code> masih <code>null</code>, getter ini
	 * <b>meminjam nama dokumen panjar</b> lewat <code>getUangMuka().getNama()</code>, dan
	 * mengembalikan string kosong bila panjar pun belum terpasang. Karena kolomnya
	 * <code>nullable = false</code>, fallback inilah yang mencegah kegagalan INSERT untuk LPJ
	 * yang judulnya tidak diisi pengguna. Perhatikan bahwa nilai pinjaman itu <b>tidak</b>
	 * ditulis balik ke field, sehingga judul LPJ ikut berubah bila nama panjar induknya
	 * kelak diedit.</p>
	 *
	 * @return judul LPJ (sudah dipangkas), nama uang muka sebagai cadangan, atau string kosong
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {

		return this.nama == null ? (getUangMuka() == null ? "" : getUangMuka().getNama()) : this.nama.trim();
	}

	/**
	 * Menyetel judul LPJ.
	 *
	 * @param nama judul; <code>null</code> mengaktifkan fallback pada {@link #getNama()}
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas LPJ (kolom <code>text</code>, tanpa batas panjang).
	 *
	 * @return keterangan apa adanya, boleh <code>null</code>
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas LPJ.
	 *
	 * <p><b>Kasus tepi:</b> tidak ada penyaringan HTML/XSS di level entity; teks ini ditampilkan
	 * ulang oleh renderer grid dan laporan, sehingga penyaringan menjadi tanggung jawab lapisan
	 * UI.</p>
	 *
	 * @param keterangan teks bebas; boleh <code>null</code>
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan bendera aktif (soft-delete) LPJ &mdash; <b>getter yang menulis balik</b>.
	 *
	 * <p><b>Tujuan:</b> menyatukan tiga sumber kebenaran menjadi satu bendera yang dipakai
	 * filter daftar (<code>PertangungjawabanAction.initCriteria</code> menyaring
	 * <code>aktif is null or aktif = true</code>) dan oleh laporan.</p>
	 *
	 * <p><b>Cara kerja &amp; urutan evaluasi:</b></p>
	 * <ol>
	 *   <li>Bila {@link #getStatus()} bernilai {@link #DISETUJU}, field <code>aktif</code>
	 *       <b>dipaksa menjadi <code>true</code></b>.</li>
	 *   <li>Field <code>disposisiSop</code> disegarkan lewat {@link #getDisposisiSop()}; bila
	 *       disposisinya sendiri tidak aktif, <code>aktif</code> menjadi <code>false</code>.</li>
	 *   <li>Bila langkah akhir disposisi berada pada simpul alur yang ditandai
	 *       <code>getPenolakanAdaDiSini()</code>, <code>aktif</code> juga menjadi
	 *       <code>false</code>.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping (penting):</b> ketiga cabang di atas menulis ke field instance. Dengan
	 * <code>dynamicUpdate</code> + property access, <b>sekadar membaca</b> LPJ yang sudah
	 * disetujui dalam sesi Hibernate yang hidup dapat menerbitkan UPDATE yang
	 * <b>menghidupkan kembali</b> baris yang sebelumnya dinonaktifkan. Urutan juga berarti
	 * langkah 1 dapat dibatalkan oleh langkah 2/3, tetapi tidak sebaliknya.</p>
	 *
	 * <p><b>Kasus tepi:</b> memanggil {@link #getStatus()} di baris pertama memicu seluruh
	 * rantai efek samping getter tersebut (termasuk kemungkinan meng-NULL-kan
	 * {@link #getDisetujuiOleh()}). <code>aktif == null</code> dibaca sebagai
	 * <code>true</code> &mdash; <i>fail-open</i>: baris lama yang kolomnya belum pernah diisi
	 * dianggap aktif.</p>
	 *
	 * @return <code>true</code> bila LPJ dianggap aktif; tidak pernah <code>null</code>
	 */
	public Boolean getAktif() {

		if (getStatus().equals(Pertangungjawaban.DISETUJU)) {
			aktif = true;
		}
		disposisiSop = getDisposisiSop();
		if (disposisiSop != null && !disposisiSop.getAktif()) {
			aktif = false;
		}

		if (disposisiSop != null && disposisiSop.getDisposisiEnd() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
			aktif = false;
		}

		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel bendera aktif secara eksplisit.
	 *
	 * <p><b>Kasus tepi:</b> nilai yang disetel di sini dapat ditimpa kembali oleh
	 * {@link #getAktif()} pada pembacaan berikutnya bila status LPJ {@link #DISETUJU}.</p>
	 *
	 * @param aktif <code>true</code> aktif, <code>false</code> nonaktif, <code>null</code>
	 *        diperlakukan sebagai aktif saat dibaca
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan total nilai realisasi belanja yang dipertanggungjawabkan.
	 *
	 * <p><b>Cara nilai ini terbentuk:</b> bukan diketik pengguna, melainkan dihitung ulang dari
	 * {@link #getFormula()} oleh lapisan Action &mdash; untuk setiap item:
	 * <code>(jumlah + jumlah&times;ppn%) &minus; (konfigurasi <code>pph_mengurangi_lpj</code>
	 * aktif ? pph : 0)</code> &mdash; lalu dijumlahkan. Nilai inilah yang menjadi nominal debet
	 * jurnal kaki utama.</p>
	 *
	 * <p><b>Validasi terkait:</b> layar pengajuan menolak simpan bila
	 * <code>uangMuka.getNilai() &lt; nilai</code> (perbandingan dalam satuan bulat/
	 * <code>longValue()</code>, sehingga selisih sen diabaikan). Pesan galatnya menyebut
	 * &quot;sisa nilai pengajuan&quot;, padahal yang dibandingkan adalah <b>nilai penuh</b>
	 * dokumen panjar.</p>
	 *
	 * <p><b>Kasus tepi:</b> <code>null</code> dinormalkan menjadi <code>0.0</code> sehingga
	 * pemanggil tidak perlu menjaga NPE, namun juga tidak bisa membedakan &quot;belum diisi&quot;
	 * dari &quot;nol&quot;.</p>
	 *
	 * @return total realisasi dalam rupiah; tidak pernah <code>null</code>
	 */
	public Double getNilai() {
		return nilai == null ? 0.0 : nilai;
	}

	/**
	 * Menyetel total nilai realisasi belanja.
	 *
	 * @param nilai nominal rupiah; <code>null</code> akan dibaca sebagai <code>0.0</code>
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Menyetel pengaju LPJ.
	 *
	 * <p><b>Kasus tepi:</b> nilai ini dapat ditimpa pada pembacaan berikutnya oleh
	 * {@link #getDibuatOleh()} bila LPJ punya {@link DisposisiSop}.</p>
	 *
	 * @param dibuatOleh pengguna pengaju; boleh <code>null</code>
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengembalikan pengguna yang mengajukan LPJ ini &mdash; <b>getter yang menulis balik</b>.
	 *
	 * <p><b>Cara kerja:</b> field disegarkan lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} (mengganti proxy yatim/basi
	 * dengan instance yang aman dibaca), lalu &mdash; bila LPJ berjalan di atas mesin SOP dan
	 * langkah awal disposisinya punya pengaju &mdash; nilainya <b>ditimpa</b> dengan
	 * <code>disposisiSop.getDisposisiStart().getDiajukanOleh()</code>. Jejak SOP dengan sengaja
	 * dijadikan sumber kebenaran yang mengalahkan kolom.</p>
	 *
	 * <p><b>Efek samping:</b> penimpaan tersebut mengenai field instance, sehingga kolom
	 * <code>dibuat_oleh</code> ikut berubah pada flush berikutnya.</p>
	 *
	 * <p><b>Kasus tepi:</b> tidak seperti {@link #getTanggalPersetujuan()}, rantai
	 * <code>getDisposisiSop()...</code> di sini <b>tidak</b> dibungkus try/catch, sehingga
	 * <code>LazyInitializationException</code> dari proxy yang terikat sesi tertutup akan
	 * merambat ke pemanggil.</p>
	 *
	 * @return pengaju LPJ, atau <code>null</code> bila belum diketahui
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = true)
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);
		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
				&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
			dibuatOleh = getDisposisiSop().getDisposisiStart().getDiajukanOleh();
		}
		return dibuatOleh;
	}

	/**
	 * Menyetel penyetuju LPJ.
	 *
	 * <p><b>Dipanggil dari:</b> layar persetujuan ZK
	 * (<code>PertangungjawabanAction.onSave</code>, hanya bila radiogroup status bernilai
	 * &quot;Disetujui&quot;), jalur REST <code>PertangungjawabanApiHelper.simpan</code> dan
	 * <code>ubahStatus</code>, serta {@link #setStatus(String)} sendiri untuk membersihkan cap
	 * saat status berubah menjadi {@link #DITOLAK}.</p>
	 *
	 * <p><b>Kasus tepi:</b> nilai yang disetel bisa dibatalkan lagi oleh
	 * {@link #getDisetujuiOleh()} bila LPJ punya disposisi SOP tanpa langkah setuju.</p>
	 *
	 * @param disetujuiOleh pengguna penyetuju, atau <code>null</code> untuk menghapus cap
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Mengembalikan pengguna yang menyetujui LPJ ini &mdash; <b>getter destruktif</b>.
	 *
	 * <p><b>Cara kerja:</b></p>
	 * <ol>
	 *   <li>Field disegarkan lewat {@link ais.database.model.GeneralValueObject#check(Object)}.</li>
	 *   <li>Bila ada disposisi SOP <i>dan</i> langkah &quot;setuju&quot;-nya punya pengaju,
	 *       penyetuju <b>ditimpa</b> dengan pengaju langkah tersebut.</li>
	 *   <li>Bila ada disposisi SOP <i>tetapi</i> langkah setujunya belum ada (atau ada tanpa
	 *       pengaju), penyetuju <b>dipaksa <code>null</code></b>.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping (berat):</b> cabang ke-3 menghapus cap persetujuan di memori, dan
	 * dengan <code>dynamicUpdate</code> perubahan itu <b>tersimpan</b>. Dokumen yang disetujui
	 * secara manual lalu <i>kemudian</i> dikaitkan ke sebuah alur SOP dapat kehilangan jejak
	 * penyetujunya hanya karena dibaca. Karena {@link #getStatus()} menurunkan statusnya dari
	 * {@link #DISETUJU} ketika penyetuju kosong, dampaknya berantai ke status dokumen.
	 * Peredam parsialnya ada di {@link #getTanggalPersetujuan()}, yang mengembalikan
	 * <code>tanggalPersetujuanManual</code> bila penyetuju masih ada.</p>
	 *
	 * <p><b>Dipanggil dari:</b> renderer grid, laporan, penjaga posting, dan
	 * {@link #getStatus()} &mdash; artinya jalur render biasa pun memicunya.</p>
	 *
	 * @return penyetuju LPJ, atau <code>null</code> bila belum/tidak lagi disetujui
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujuiOleh = check(disetujuiOleh);

		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
				&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
			disetujuiOleh = getDisposisiSop().getDisposisiSetuju().getDiajukanOleh();
		}

		if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
				|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
			disetujuiOleh = null;
		}

		return disetujuiOleh;
	}

	/**
	 * Menyetel waktu persetujuan LPJ.
	 *
	 * @param tanggalPersetujuan waktu persetujuan, atau <code>null</code> untuk menghapusnya
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengembalikan waktu persetujuan LPJ &mdash; <b>getter destruktif</b>.
	 *
	 * <p><b>Cara kerja:</b> sejajar dengan {@link #getDisetujuiOleh()}, dengan dua tambahan:</p>
	 * <ol>
	 *   <li>Seluruh rantai <code>getDisposisiSop()...</code> dibungkus <code>try/catch</code>
	 *       karena instance kanonik/berbagi dari <code>AuditTimestampInterceptor</code> dapat
	 *       membawa proxy yang terikat ke <code>Session</code> yang sudah tertutup. Bila
	 *       <code>LazyInitializationException</code> terjadi, kejadiannya dicatat ke
	 *       <code>ErrorAuditUtil</code> dan nilai kolom yang sudah ada dipertahankan &mdash;
	 *       jadi getter ini <b>tidak pernah gagal</b>, tetapi <i>diam-diam melewati</i>
	 *       sinkronisasi dengan SOP.</li>
	 *   <li>Setelah itu, bila {@link #getTanggalPersetujuanManual()} terisi <b>dan</b> penyetuju
	 *       masih ada, tanggal manual itulah yang menang. Inilah jalan pulih bagi dokumen yang
	 *       disetujui manual: nilainya dipulihkan meski cabang SOP sempat mengosongkannya.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping:</b> menulis ke field <code>tanggalPersetujuan</code> dan
	 * <code>disetujuiOleh</code>; keduanya kolom terpetakan.</p>
	 *
	 * @return waktu persetujuan, atau <code>null</code> bila LPJ belum disetujui
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {

		try {
			// FIX LazyInitializationException: getDisposisiSop() bisa berupa instance
			// canonical/shared (AuditTimestampInterceptor) yang proxy-nya terikat ke
			// Session lain yang sudah closed -> jangan biarkan getter ini crash, cukup
			// lewati bagian ini (nilai fallback dipertahankan).
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
					&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
				tanggalPersetujuan = getDisposisiSop().getDisposisiSetuju().getWaktu();
			}

			if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
					|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
				tanggalPersetujuan = null;
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/akunting/Pertangungjawaban.java:getTanggalPersetujuan-lazy");
		}

		disetujuiOleh = check(disetujuiOleh);
		if (getTanggalPersetujuanManual() != null && disetujuiOleh != null) {
			tanggalPersetujuan = getTanggalPersetujuanManual();
		}

		return tanggalPersetujuan;
	}

	/**
	 * Menyetel waktu pengajuan LPJ.
	 *
	 * <p><b>Dipanggil dari:</b> jalur simpan ZK dan REST, hanya ketika
	 * {@link #getDibuatOleh()} masih kosong (yakni pada penyimpanan pertama).</p>
	 *
	 * @param tanggalPembuatan waktu pengajuan; boleh <code>null</code>
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Mengembalikan waktu pengajuan LPJ &mdash; <b>getter yang menulis balik</b>.
	 *
	 * <p><b>Cara kerja:</b> bila LPJ berjalan di atas SOP dan langkah awal disposisinya punya
	 * pengaju, kolom ditimpa dengan waktu langkah tersebut. Bila hasil akhirnya masih
	 * <code>null</code>, getter mengembalikan <code>new Date()</code> &mdash; <b>waktu
	 * sekarang</b>.</p>
	 *
	 * <p><b>Kasus tepi (penting):</b> fallback <code>new Date()</code> <i>tidak</i> disimpan ke
	 * field, sehingga nilai yang dikembalikan <b>berubah setiap kali dipanggil</b> untuk LPJ
	 * yang kolomnya kosong. Ini merambat: {@link #getTanggalTransaksi()} memakainya sebagai
	 * cadangan, dan <code>PertangungjawabanAction.initCriteria</code> menyaring rentang
	 * berdasarkan kolom <code>tanggal_pembuatan</code> di database (bukan getter) &mdash; LPJ
	 * lama yang kolomnya NULL karena itu <b>tidak akan pernah cocok</b> dengan filter tanggal
	 * mana pun sekalipun layar menampilkan tanggal hari ini.</p>
	 *
	 * @return waktu pengajuan; tidak pernah <code>null</code>
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
				&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
			tanggalPembuatan = getDisposisiSop().getDisposisiStart().getWaktu();
		}

		return tanggalPembuatan == null ? new Date() : tanggalPembuatan;
	}

	/**
	 * Mengembalikan status alur LPJ &mdash; <b>getter yang menulis balik</b>.
	 *
	 * <p><b>Cara kerja:</b></p>
	 * <ol>
	 *   <li>Bila {@link #getDisetujuiOleh()} terisi, status dipaksa {@link #DISETUJU}
	 *       &mdash; adanya penyetuju dianggap bukti persetujuan.</li>
	 *   <li>Sebaliknya, bila penyetuju kosong tetapi kolom status masih {@link #DISETUJU},
	 *       status <b>diturunkan</b> kembali ke {@link #PENGAJUAN}.</li>
	 *   <li>Bila langkah akhir disposisi berada pada simpul alur bertanda
	 *       <code>getPenolakanAdaDiSini()</code>, status menjadi {@link #DITOLAK} &mdash;
	 *       penolakan SOP mengalahkan dua cabang sebelumnya.</li>
	 *   <li>Status kosong/blanko dibaca sebagai {@link #PENGAJUAN}.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping:</b> memanggil {@link #getDisetujuiOleh()} di baris pertama sudah
	 * memicu getter destruktif tersebut; penurunan status di cabang 2 juga ditulis ke field
	 * <code>status</code>. Jadi <b>membaca status dapat mengubah status</b>.</p>
	 *
	 * <p><b>Dipanggil dari:</b> {@link #getAktif()}, renderer grid, filter tombol posting
	 * (&quot;hanya yang sudah disetujui&quot;), penjaga hapus di API, dan laporan.</p>
	 *
	 * @return salah satu dari {@link #PENGAJUAN}, {@link #DISETUJU}, {@link #DITOLAK};
	 *         tidak pernah <code>null</code>
	 */
	public String getStatus() {
		if (getDisetujuiOleh() != null) {
			status = DISETUJU;
		} else if (status != null && status.equals(DISETUJU)) {
			status = PENGAJUAN;
		}

		disposisiSop = getDisposisiSop();
		if (disposisiSop != null && disposisiSop.getDisposisiEnd() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
			status = DITOLAK;
		}

		return status == null || status.trim().isEmpty() ? PENGAJUAN : status;
	}

	/**
	 * Menyetel status alur LPJ.
	 *
	 * <p><b>Efek samping:</b> bila status yang disetel adalah {@link #DITOLAK}, method ini
	 * sekaligus membersihkan cap persetujuan &mdash; {@link #setDisetujuiOleh(Tbmuser)} dan
	 * {@link #setTanggalPersetujuan(Date)} dipanggil dengan <code>null</code>. Ini penting agar
	 * {@link #getStatus()} tidak segera menaikkan kembali statusnya ke {@link #DISETUJU} pada
	 * pembacaan berikutnya.</p>
	 *
	 * <p><b>Kasus tepi:</b> perbandingan memakai <code>equals</code> yang peka huruf besar-kecil;
	 * status berupa string bebas sehingga nilai di luar tiga konstanta akan tersimpan apa adanya
	 * dan dibaca kembali apa adanya oleh {@link #getStatus()}.</p>
	 *
	 * <p><b>Kontrol akses (audit):</b> tidak ada pemeriksaan hak di level entity. Siapa pun yang
	 * dapat mencapai jalur simpan &mdash; termasuk lewat parameter URL
	 * <code>?persetujuan=true</code> pada layar pengajuan, atau lewat field
	 * <code>statusDokumen</code> pada REST <code>simpan</code> &mdash; dapat menetapkan
	 * {@link #DISETUJU}.</p>
	 *
	 * @param status status baru; boleh <code>null</code> (dibaca sebagai {@link #PENGAJUAN})
	 */
	public void setStatus(String status) {
		if (status != null && status.equals(DITOLAK)) {
			setDisetujuiOleh(null);
			setTanggalPersetujuan(null);
		}
		this.status = status;
	}

	/**
	 * Mengembalikan dokumen panjar yang dipertanggungjawabkan oleh LPJ ini.
	 *
	 * <p>Kolom <code>uang_muka</code> bersifat <b>wajib</b> (<code>nullable = false</code>) dan
	 * di-fetch dengan <code>FetchMode.SELECT</code> sehingga dimuat lewat query terpisah, bukan
	 * join. Relasi ini adalah sumber bagi {@link #getSatuanKerja()}, cadangan bagi
	 * {@link #getNama()}, dan pemasok bagan akun bagi ketiga kaki jurnal
	 * (<code>UangMuka.akun</code>, <code>JenisUangMuka.akun</code>,
	 * <code>JenisUangMuka.akunKelebihan</code>, <code>JenisUangMuka.akunSponsor</code>).</p>
	 *
	 * <p><b>Non-obvious:</b> berbeda dari kebanyakan getter relasi di kelas ini, getter ini
	 * <b>tidak</b> memanggil {@link ais.database.model.GeneralValueObject#check(Object)},
	 * sehingga proxy basi akan tetap dikembalikan apa adanya.</p>
	 *
	 * @return dokumen uang muka induk; secara skema tidak boleh <code>null</code>, namun
	 *         objek yang belum disimpan bisa saja masih kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "uang_muka", nullable = false)
	public UangMuka getUangMuka() {
		return uangMuka;
	}

	/**
	 * Menyetel dokumen panjar yang dipertanggungjawabkan.
	 *
	 * <p><b>Catatan:</b> ini hanya mengisi arah maju (<code>pertangungjawaban.uang_muka</code>).
	 * Arah balik (<code>uang_muka.pertangungjawaban</code>) harus diisi terpisah lewat
	 * <code>UangMuka.setPertangungjawaban(...)</code>; jalur ZK melakukannya dari timer setelah
	 * simpan.</p>
	 *
	 * @param uangMuka dokumen panjar induk
	 */
	public void setUangMuka(UangMuka uangMuka) {
		this.uangMuka = uangMuka;
	}

	/**
	 * Mengembalikan unit kerja pemilik LPJ &mdash; <b>getter yang menulis balik</b>.
	 *
	 * <p><b>Cara kerja:</b> bila dokumen panjar induk ada, unit kerja <b>selalu disalin ulang</b>
	 * dari <code>uangMuka.getSatuanKerja()</code>; hanya bila panjar belum terpasang, nilai kolom
	 * sendiri dipakai (setelah disegarkan lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)}).</p>
	 *
	 * <p><b>Efek samping:</b> penyalinan itu mengenai field terpetakan, sehingga kolom
	 * <code>satuan_kerja</code> LPJ ikut berpindah bila unit kerja panjar induk diubah &mdash;
	 * dan LPJ berpindah pula dalam filter daftar.</p>
	 *
	 * <p><b>Cakupan data (audit):</b> ini satu-satunya pembatas cakupan yang dimiliki entity ini
	 * (tidak ada kolom sekolah/yayasan). <code>PertangungjawabanAction.initCriteria</code>
	 * menyaring dengan <code>or(isNull(&quot;satuanKerja&quot;), ...)</code>, sehingga bila
	 * panjar induk tidak punya unit kerja maka LPJ ini <b>terlihat oleh semua pengguna</b>.
	 * <code>GrupTransaksi</code> juga menurunkan unit kerja jurnalnya dari sini.</p>
	 *
	 * @return unit kerja pemilik, atau <code>null</code> bila tidak ada
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		if (getUangMuka() != null) {
			satuanKerja = getUangMuka().getSatuanKerja();
		} else {
			satuanKerja = check(satuanKerja);
		}
		return satuanKerja;
	}

	/**
	 * Menyetel unit kerja pemilik LPJ.
	 *
	 * <p><b>Kasus tepi:</b> nilai yang disetel di sini praktis tidak bertahan &mdash;
	 * {@link #getSatuanKerja()} akan menimpanya dari dokumen panjar pada pembacaan berikutnya.
	 * Jalur REST tetap memanggilnya (dari parameter <code>satuanKerjaId</code>), tetapi
	 * efeknya hanya sementara.</p>
	 *
	 * @param satuanKerja unit kerja; boleh <code>null</code>
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/** Kunci unik turunan; lihat {@link #getKodeUnik()}. */
	private String kodeUnik;
	/** Tanggal persetujuan yang diketik manual; lihat {@link #getTanggalPersetujuanManual()}. */
	private Date tanggalPersetujuanManual;

	/**
	 * Mengembalikan kunci unik dokumen &mdash; <b>getter yang menulis balik</b>.
	 *
	 * <p><b>Tujuan:</b> memberi setiap LPJ satu string pembeda pada kolom ber-<code>unique</code>
	 * constraint, dipakai untuk mencegah dokumen kembar (mis. saat impor/sinkronisasi).</p>
	 *
	 * <p><b>Cara kerja:</b> nilainya <b>dihitung ulang pada setiap pembacaan</b> sebagai
	 * <code>getKode() + &quot;_&quot; + (disposisiSop != null ? disposisiSop.getId() :
	 * getId())</code>.</p>
	 *
	 * <p><b>Kasus tepi (penting):</b></p>
	 * <ul>
	 *   <li>Bila {@link #getKode()} <code>null</code>, hasilnya berawalan literal
	 *       <code>&quot;null&quot;</code>, mis. <code>&quot;null_412&quot;</code>.</li>
	 *   <li>Untuk objek baru, {@link #getId()} masih <code>null</code> sehingga hasilnya
	 *       <code>&quot;KODE_null&quot;</code> &mdash; dua LPJ baru berkode sama akan
	 *       <b>bertabrakan pada constraint unik</b> sebelum id-nya terbit.</li>
	 *   <li>Sumber sufiksnya <b>berganti</b> begitu sebuah disposisi SOP dipasang: kunci yang
	 *       tadinya berbasis id LPJ berubah menjadi berbasis id disposisi. Karena kunci ini
	 *       tidak pernah dibekukan, nilai lama tidak dapat dipakai sebagai referensi historis.</li>
	 *   <li>Kunci ini <b>tidak memuat penanda tenant/unit kerja</b>, sehingga ruang namanya
	 *       global untuk seluruh instalasi.</li>
	 * </ul>
	 *
	 * @return kunci unik turunan; tidak pernah <code>null</code> (bisa memuat teks
	 *         &quot;null&quot;)
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		kodeUnik = getKode() + "" + (getDisposisiSop() == null ? "_" + getId() : "_" + getDisposisiSop().getId());
		return kodeUnik;
	}

	/**
	 * Menyetel kunci unik secara eksplisit.
	 *
	 * <p><b>Kasus tepi:</b> praktis tanpa pengaruh &mdash; {@link #getKodeUnik()} selalu
	 * menghitung ulang nilainya sebelum Hibernate membacanya.</p>
	 *
	 * @param kodeUnik kunci unik; akan ditimpa pada pembacaan berikutnya
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * Mengembalikan jejak langkah SOP yang menaungi LPJ ini.
	 *
	 * <p>Implementasi kontrak abstrak {@link DataSop#getDisposisiSop()}. Field disegarkan lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)} sebelum dikembalikan sehingga
	 * proxy yatim diganti instance yang aman.</p>
	 *
	 * <p><b>Peran:</b> objek inilah yang, bila ada, mengalahkan kolom
	 * {@link #getDibuatOleh()}, {@link #getDisetujuiOleh()}, {@link #getTanggalPersetujuan()},
	 * {@link #getTanggalPembuatan()}, {@link #getStatus()} dan {@link #getAktif()}. Ia juga
	 * menjadi sumber sufiks {@link #getKodeUnik()}.</p>
	 *
	 * @return disposisi SOP, atau <code>null</code> bila LPJ tidak berjalan di atas mesin SOP
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menyetel jejak langkah SOP yang menaungi LPJ ini.
	 *
	 * <p><b>Cara kerja:</b> nilai <code>null</code> atau disposisi yang belum punya id
	 * <b>ditolak diam-diam</b> (langsung <code>return</code>) sehingga kaitan SOP yang sudah
	 * terpasang tidak bisa dilepas lewat setter ini.</p>
	 *
	 * <p><b>Non-obvious:</b> ekspresi ternary di baris penugasan menguji ulang kondisi
	 * <code>disposisiSop == null || disposisiSop.getId() == null</code> yang <i>sudah dipastikan
	 * salah</i> oleh penjaga di atasnya. Cabang &quot;pertahankan nilai lama&quot; karena itu
	 * <b>tidak pernah tercapai</b>; efektifnya method ini selalu menugaskan argumennya. Kode
	 * mati ini dibiarkan apa adanya (dokumentasi ini tidak mengubah logika).</p>
	 *
	 * @param disposisiSop disposisi SOP baru; <code>null</code>/tanpa id diabaikan
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
	 * Nilai awal kolom {@link #getFormula() formula}: representasi teks dari JSON array kosong
	 * (<code>&quot;[]&quot;</code>).
	 *
	 * <p><b>Non-obvious:</b> konstanta ini <b>tidak <code>final</code></b> &mdash; ia
	 * <code>public static</code> saja, sehingga secara teknis dapat ditimpa dari luar kelas.
	 * Nilainya dibangun sekali saat kelas dimuat lewat <code>new JSONArray().toString()</code>.</p>
	 */
	public static String DEFAULT_FORMULA = new JSONArray().toString();

	/**
	 * Mengembalikan rincian belanja LPJ dalam bentuk teks JSON array.
	 *
	 * <p><b>Struktur:</b> setiap elemen array adalah objek JSON satu baris biaya dengan kunci
	 * antara lain <code>nama</code>, <code>qty</code>, <code>harga</code>, <code>jumlah</code>,
	 * <code>ppn</code> (persen), <code>pajak</code> (id <code>JenisPajakBarang</code> untuk PPh),
	 * <code>ntpn</code>, <code>npwp</code>, <code>namaWp</code>, <code>tanggalStor</code>, dan
	 * <code>id_file</code> (lampiran bukti).</p>
	 *
	 * <p><b>Konsumen:</b> <code>PertangungjawabanAction.reloadFormula</code>/
	 * <code>reloadDataFormula</code> membangun ulang tabel biaya di layar dari array ini;
	 * <code>PostingPertangungjawabanAction</code> menguraikannya untuk membentuk daftar akun
	 * PPh per <code>JenisPajakBarang</code>; dan
	 * <code>Pajak.buat(pertangungjawaban, null, itemJson, null)</code> membuat satu baris
	 * {@link ais.database.model.akunting.Pajak} per elemen yang punya kunci
	 * <code>pajak</code>.</p>
	 *
	 * <p><b>Kasus tepi:</b> <code>null</code> maupun string kosong dikembalikan sebagai
	 * {@link #DEFAULT_FORMULA} sehingga pemanggil selalu aman melakukan
	 * <code>new JSONArray(getFormula())</code>. Isi kolom <b>tidak divalidasi</b> di level entity
	 * &mdash; JSON rusak akan melempar <code>JSONException</code> di pemanggil.</p>
	 *
	 * <p><b>Integritas (audit):</b> karena rincian disimpan sebagai dokumen dan bukan tabel anak,
	 * tidak ada FK, tidak ada batasan tipe, dan tidak ada snapshot tarif pajak. Tarif diambil
	 * <i>live</i> dari master saat hitung ulang, sehingga nominal LPJ lama dapat berubah bila
	 * tarif master diubah.</p>
	 *
	 * @return teks JSON array rincian biaya; tidak pernah <code>null</code>
	 */
	@Column(name = "formula", nullable = true, columnDefinition = "text")
	public String getFormula() {
		return formula == null || formula.isEmpty() ? Pertangungjawaban.DEFAULT_FORMULA : formula;
	}

	/**
	 * Menyetel rincian belanja LPJ.
	 *
	 * @param formula teks JSON array; <code>null</code>/kosong akan dibaca sebagai
	 *        {@link #DEFAULT_FORMULA}
	 */
	public void setFormula(String formula) {
		this.formula = formula;
	}

	/**
	 * Mengembalikan cap posting <b>kaki utama</b> LPJ.
	 *
	 * <p>Terisi oleh <code>PostingPertangungjawabanAction</code> saat jurnal realisasi belanja
	 * diterbitkan dengan jenis
	 * <code>PostingHistory.JENIS_PERTANGGUNGJAWABAN_UANG_MUKA</code>. Nilai <code>null</code>
	 * berarti &quot;belum diposting&quot; dan itulah filter yang dipakai layar posting untuk
	 * memilih kandidat.</p>
	 *
	 * <p><b>Non-obvious:</b> kolom ini juga menjadi penjaga hapus di
	 * <code>PertangungjawabanApiHelper</code>, dan dikosongkan kembali oleh
	 * &quot;Batalkan Posting&quot; &mdash; yang hanya menghapus baris
	 * <code>akunting.grup_transaksi</code>, bukan baris anak <code>akunting.transaksi</code>.</p>
	 *
	 * @return cap posting kaki utama, atau <code>null</code> bila belum dijurnal
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * Menyetel (atau menghapus) cap posting kaki utama.
	 *
	 * <p><b>Dipanggil dari:</b> <code>PostingPertangungjawabanAction</code> &mdash; diisi saat
	 * posting massal/per-baris berhasil, dan disetel <code>null</code> saat pembatalan posting.</p>
	 *
	 * @param postingHistory cap posting, atau <code>null</code> untuk membatalkan cap
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

	/**
	 * Mengembalikan tahun periode dokumen &mdash; <b>getter yang menulis balik</b>.
	 *
	 * <p><b>Cara kerja:</b> bila kolom masih kosong, diisi dengan tahun berjalan dari
	 * <code>WaktuUtil.getCalendar()</code> (jam server aplikasi, bukan tanggal dokumen).</p>
	 *
	 * <p><b>Kasus tepi:</b> pengisian default terjadi pada <i>saat baris dibaca</i>, sehingga
	 * LPJ lama yang kolomnya NULL akan mendapat tahun <b>sekarang</b>, bukan tahun
	 * pengajuannya. Jalur REST <code>simpan</code> menghindari jebakan ini dengan menurunkan
	 * tahun/bulan dari {@link #getTanggalPembuatan()} secara eksplisit.</p>
	 *
	 * @return tahun periode; tidak pernah <code>null</code>
	 */
	public Integer getTahun() {
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	/**
	 * Menyetel tahun periode dokumen.
	 *
	 * @param tahun tahun empat digit; <code>null</code> memicu pengisian default di getter
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengembalikan seri penomoran surat alur keuangan untuk dokumen ini &mdash; <b>getter yang
	 * menulis balik</b>.
	 *
	 * <p><b>Cara kerja:</b> bila kolom kosong, diisi dengan singleton statis
	 * <code>NomorSuratAlurKeuangan.PERTANGGUNGJAWABAN_DATA</code> (seri kode
	 * <code>&quot;003&quot;</code>, nama &quot;Pertanggungjawaban&quot;) yang dimuat/di-seed
	 * sekali saat aplikasi start. Bila sudah terisi, nilainya hanya disegarkan lewat
	 * {@link ais.database.model.GeneralValueObject#check(Object)}.</p>
	 *
	 * <p><b>Kasus tepi:</b> bila proses seeding belum berjalan, singleton statis itu masih
	 * <code>null</code> dan getter mengembalikan <code>null</code> tanpa galat. Objek statis
	 * tersebut juga berasal dari <code>Session</code> lain, sehingga menautkannya ke entity
	 * yang sedang dikelola dapat memunculkan masalah proxy lintas sesi &mdash; alasan mengapa
	 * beberapa getter di kelas ini membungkus akses SOP dengan try/catch.</p>
	 *
	 * @return seri penomoran surat, atau <code>null</code> bila master belum ter-seed
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat_alur_keuangan", nullable = true)
	public NomorSuratAlurKeuangan getNomorSuratAlurKeuangan() {
		if (nomorSuratAlurKeuangan == null) {
			nomorSuratAlurKeuangan = NomorSuratAlurKeuangan.PERTANGGUNGJAWABAN_DATA;
		} else {
			nomorSuratAlurKeuangan = check(nomorSuratAlurKeuangan);
		}
		return nomorSuratAlurKeuangan;
	}

	/**
	 * Menyetel seri penomoran surat alur keuangan.
	 *
	 * @param nomorSuratAlurKeuangan seri penomoran; <code>null</code> memicu pengisian default
	 */
	public void setNomorSuratAlurKeuangan(NomorSuratAlurKeuangan nomorSuratAlurKeuangan) {
		this.nomorSuratAlurKeuangan = nomorSuratAlurKeuangan;
	}

	/**
	 * Mengembalikan bulan periode dokumen &mdash; <b>getter yang menulis balik</b>.
	 *
	 * <p><b>Cara kerja:</b> bila kosong, diisi dengan bulan berjalan
	 * <code>Calendar.MONTH + 1</code> sehingga skalanya <b>1&ndash;12</b> (bukan 0&ndash;11 khas
	 * <code>Calendar</code>). Konsumen yang mengubahnya kembali menjadi indeks
	 * <code>Calendar</code> wajib mengurangi 1.</p>
	 *
	 * <p><b>Kasus tepi:</b> sama seperti {@link #getTahun()}, default diambil dari waktu
	 * <i>pembacaan</i>, bukan waktu dokumen.</p>
	 *
	 * @return bulan periode dalam rentang 1&ndash;12; tidak pernah <code>null</code>
	 */
	public Integer getBulan() {
		if (bulan == null) {
			bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		}
		return bulan;
	}

	/**
	 * Menyetel bulan periode dokumen.
	 *
	 * @param bulan bulan dalam rentang 1&ndash;12; <code>null</code> memicu default di getter
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Mengembalikan nominal dana yang berasal dari sponsor/pihak ketiga.
	 *
	 * <p><b>Peran:</b> dana ini menambah sumber pembiayaan di luar panjar, sehingga pada layar
	 * ZK ia masuk rumus sisa panjar:
	 * <code>dikembalikan = uangMuka.nilai + dariSponsor &minus; nilai</code>. Pada jurnal kaki
	 * utama ia muncul sebagai pasangan Dr <code>UangMuka.akun</code> / Cr
	 * <code>JenisUangMuka.akunSponsor</code>.</p>
	 *
	 * <p><b>Integritas (audit):</b> jalur REST <code>PertangungjawabanApiHelper.simpan</code>
	 * <b>tidak</b> menyertakan nilai ini dalam perhitungan sisa panjar, sehingga dua jalur simpan
	 * menghasilkan angka pengembalian yang berbeda untuk data yang sama. Kolom ini juga hanya
	 * tampil di layar bila konfigurasi <code>sponsor_tampil_lpj</code> aktif.</p>
	 *
	 * <p><b>Kasus tepi:</b> <code>null</code> dinormalkan menjadi <code>0.0</code>; nilai negatif
	 * tidak divalidasi.</p>
	 *
	 * @return nominal dana sponsor; tidak pernah <code>null</code>
	 */
	public Double getDariSponsor() {
		return dariSponsor == null ? 0.0 : dariSponsor;
	}

	/**
	 * Menyetel nominal dana sponsor.
	 *
	 * @param dariSponsor nominal rupiah; <code>null</code> dibaca sebagai <code>0.0</code>
	 */
	public void setDariSponsor(Double dariSponsor) {
		this.dariSponsor = dariSponsor;
	}

	/**
	 * Mengembalikan nama pihak ketiga penyumbang dana.
	 *
	 * <p><b>Kasus tepi:</b> <code>null</code> dinormalkan menjadi string kosong dan spasi tepi
	 * dipangkas, sehingga getter aman dipakai langsung sebagai label ZK.</p>
	 *
	 * @return nama sponsor, atau string kosong; tidak pernah <code>null</code>
	 */
	public String getNamaSponsor() {
		return namaSponsor == null ? "" : namaSponsor.trim();
	}

	/**
	 * Menyetel nama pihak ketiga penyumbang dana.
	 *
	 * @param namaSponsor nama sponsor; boleh <code>null</code>
	 */
	public void setNamaSponsor(String namaSponsor) {
		this.namaSponsor = namaSponsor;
	}

	/**
	 * Mengembalikan sisa panjar yang wajib dikembalikan pegawai ke kas.
	 *
	 * <p><b>Peran:</b> ini adalah <b>nominal uang riil</b>. Ia menentukan (a) apakah baris
	 * &quot;Tanggal Stor&quot; wajib diisi di layar (ambang <code>&gt; 0.1</code>), (b) apakah
	 * {@link #getTanggalStor()} dipertahankan atau dihapus, dan (c) <b>nominal jurnal kaki
	 * pengembalian</b> &mdash; <code>PostingPertangungjawabanPengembalianAction</code> memakai
	 * nilai ini apa adanya sebagai debet <code>JenisUangMuka.akunKelebihan</code> dan kredit
	 * <code>JenisUangMuka.akun</code>.</p>
	 *
	 * <p><b>Cara nilai ini terbentuk &mdash; TIGA rumus berbeda di repo:</b></p>
	 * <ol>
	 *   <li><b>Layar ZK (benar):</b>
	 *       <code>uangMuka.getNilai() + dariSponsor &minus; nilaiRealisasi</code>.</li>
	 *   <li><b>REST <code>PertangungjawabanApiHelper.simpan</code>:</b>
	 *       <code>uangMuka.getNilai() &minus; nilaiRealisasi</code> &mdash; <b>dana sponsor
	 *       hilang dari rumus</b>, sisa yang wajib disetor dilaporkan lebih kecil.</li>
	 *   <li><b>Tombol &quot;Hitung Ulang&quot; massal di
	 *       <code>PertangungjawabanAction</code>:</b>
	 *       <code>pertangungjawaban.getNilai() + dariSponsor &minus; nilaiRealisasiBaru</code>
	 *       &mdash; memakai <b>nilai LPJ lama</b> sebagai pengganti nilai panjar. Karena blok
	 *       itu hanya berjalan ketika nilai lama &ne; nilai baru, hasilnya adalah selisih
	 *       antar-revisi LPJ, bukan sisa panjar. Angka itu lalu ditulis ke kolom ini dan
	 *       dijurnal apa adanya oleh layar posting pengembalian.</li>
	 * </ol>
	 *
	 * <p><b>Kasus tepi:</b> <code>null</code> dinormalkan menjadi <code>0.0</code>. Nilai negatif
	 * (realisasi melebihi panjar) tidak ditolak di level entity; layar ZK-lah yang menolak simpan
	 * bila realisasi melampaui panjar.</p>
	 *
	 * @return sisa panjar dalam rupiah; tidak pernah <code>null</code>
	 */
	public Double getDikembalikan() {
		return dikembalikan == null ? 0.0 : dikembalikan;
	}

	/**
	 * Menyetel sisa panjar yang wajib dikembalikan.
	 *
	 * <p><b>Dipanggil dari:</b> <code>PertangungjawabanAction.onSave</code>, penyapu
	 * &quot;Hitung Ulang&quot;, dan <code>PertangungjawabanApiHelper.simpan</code> &mdash;
	 * ketiganya dengan rumus yang berbeda (lihat {@link #getDikembalikan()}).</p>
	 *
	 * @param dikembalikan nominal rupiah; <code>null</code> dibaca sebagai <code>0.0</code>
	 */
	public void setDikembalikan(Double dikembalikan) {
		this.dikembalikan = dikembalikan;
	}

	/**
	 * Mengembalikan baris daftar pengajuan transfer yang terkait dengan LPJ ini.
	 *
	 * <p><b>Peran:</b> bila LPJ menagih kekurangan dana (atau menyalurkan pembayaran ke pihak
	 * ketiga), ia masuk ke <code>DaftarPengajuanTransfer</code> untuk diproses bagian keuangan
	 * hingga terealisasi di bank lewat <code>ProsesTransfer</code>. Relasi ini menjadi sumber
	 * {@link #getTanggalTransaksi()} dan salah satu penjaga hapus di API (&quot;sudah masuk
	 * daftar pengajuan transfer&quot;).</p>
	 *
	 * <p><b>Non-obvious:</b> kaitan ini tidak selalu dipasang saat simpan. Tombol
	 * &quot;Hitung Ulang&quot; pada layar LPJ melakukan pencarian susulan
	 * (<code>DaftarPengajuanTransfer</code> teraktif dengan <code>pertangungjawaban =
	 * dokumen ini</code>, urut id menurun, ambil satu) dan menambal kolom ini bila LPJ sudah
	 * disetujui tetapi kolomnya masih kosong.</p>
	 *
	 * @return baris pengajuan transfer terkait, atau <code>null</code>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "daftar_pengajuan_transfer", nullable = true)
	public DaftarPengajuanTransfer getDaftarPengajuanTransfer() {
		return daftarPengajuanTransfer;
	}

	/**
	 * Menyetel baris daftar pengajuan transfer terkait.
	 *
	 * @param daftarPengajuanTransfer baris pengajuan transfer; boleh <code>null</code>
	 */
	public void setDaftarPengajuanTransfer(DaftarPengajuanTransfer daftarPengajuanTransfer) {
		this.daftarPengajuanTransfer = daftarPengajuanTransfer;
	}

	/**
	 * Mengembalikan tanggal efektif transaksi kas/bank dokumen ini &mdash; <b>getter yang menulis
	 * balik</b>.
	 *
	 * <p><b>Tujuan:</b> memberi jurnal satu tanggal yang mencerminkan kapan uang benar-benar
	 * berpindah, bukan kapan dokumen diketik.</p>
	 *
	 * <p><b>Urutan prioritas:</b></p>
	 * <ol>
	 *   <li>Bila pengajuan transfer terkait bersifat <i>transitori</i> dan punya proses
	 *       transitori &rarr; <code>prosesTransitori.getTanggalPembuatan()</code>.</li>
	 *   <li>Bila pengajuan transfer punya <code>ProsesTransfer</code> &rarr; tanggal
	 *       realisasinya; bila realisasi masih kosong, tanggal pembuatan proses transfer.</li>
	 *   <li>Selain itu &rarr; {@link #getTanggalPembuatan()}.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping:</b> hasilnya selalu ditulis ke field terpetakan
	 * <code>tanggalTransaksi</code>, sehingga membaca dokumen dapat menerbitkan UPDATE pada
	 * kolom yang menjadi tanggal jurnal.</p>
	 *
	 * <p><b>Kasus tepi:</b> pada cabang 3, nilai yang ditulis berasal dari
	 * {@link #getTanggalPembuatan()} yang sendiri dapat berupa <code>new Date()</code> &mdash;
	 * artinya LPJ tanpa tanggal pembuatan akan <b>membekukan waktu pembacaan pertama</b> sebagai
	 * tanggal transaksinya. Rantai <code>getDaftarPengajuanTransfer()...</code> tidak dilindungi
	 * try/catch terhadap <code>LazyInitializationException</code>.</p>
	 *
	 * @return tanggal efektif transaksi; tidak pernah <code>null</code> dalam praktik
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_transaksi")
	public Date getTanggalTransaksi() {
		if (getDaftarPengajuanTransfer() != null && daftarPengajuanTransfer.getTransitori()
				&& daftarPengajuanTransfer.getTransitoriData() != null
				&& daftarPengajuanTransfer.getTransitoriData().getProsesTransitori() != null) {
			tanggalTransaksi = daftarPengajuanTransfer.getTransitoriData().getProsesTransitori().getTanggalPembuatan();
		} else if (getDaftarPengajuanTransfer() != null && daftarPengajuanTransfer.getProsesTransfer() != null) {
			tanggalTransaksi = daftarPengajuanTransfer.getProsesTransfer().getTanggalRealisasikan() == null
					? daftarPengajuanTransfer.getProsesTransfer().getTanggalPembuatan()
					: daftarPengajuanTransfer.getProsesTransfer().getTanggalRealisasikan();
		} else {
			tanggalTransaksi = getTanggalPembuatan();
		}
		return tanggalTransaksi;
	}

	/**
	 * Menyetel tanggal efektif transaksi.
	 *
	 * <p><b>Kasus tepi:</b> praktis tanpa pengaruh &mdash; {@link #getTanggalTransaksi()} selalu
	 * menghitung ulang nilainya.</p>
	 *
	 * @param tanggalTransaksi tanggal transaksi; akan ditimpa pada pembacaan berikutnya
	 */
	public void setTanggalTransaksi(Date tanggalTransaksi) {
		this.tanggalTransaksi = tanggalTransaksi;
	}

	/**
	 * Mengembalikan cap posting <b>kaki pajak</b> LPJ &mdash; <b>kolom mati</b>.
	 *
	 * <p><b>Status terverifikasi:</b> tidak ada satu pun kode di repo yang memanggil
	 * {@link #setPostingHistoryPajak(PostingHistory)} selain setter di kelas ini sendiri,
	 * sehingga kolom <code>posting_history_pajak</code> praktis <b>selalu NULL</b>. Jurnal pajak
	 * yang lahir dari rincian LPJ memang ada &mdash; dibuat
	 * <code>PostingPertangungjawabanPajakAction</code> dengan jenis
	 * <code>PostingHistory.JENIS_PERTANGGUNGJAWABAN_PAJAK</code> &mdash; tetapi capnya
	 * ditempelkan pada entity {@link ais.database.model.akunting.Pajak}
	 * (<code>Pajak.postingHistory</code>), bukan di sini.</p>
	 *
	 * <p><b>Satu-satunya konsumen:</b> penjaga hapus di
	 * <code>PertangungjawabanApiHelper</code>/<code>PertangungjawabanKasBesarApiHelper</code>
	 * yang menolak penghapusan LPJ bila salah satu dari tiga cap terisi. Karena kolom ini tidak
	 * pernah terisi, <b>penjaga itu tidak pernah menyala lewat jalur pajak</b>: sebuah LPJ yang
	 * baris pajaknya sudah dijurnal masih dapat dihapus selama kaki utama dan kaki pengembalian
	 * belum diposting &mdash; menyisakan jurnal pajak tanpa dokumen sumber.</p>
	 *
	 * @return selalu <code>null</code> dalam praktik
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history_pajak", nullable = true)
	public PostingHistory getPostingHistoryPajak() {
		return postingHistoryPajak;
	}

	/**
	 * Menyetel cap posting kaki pajak.
	 *
	 * <p><b>Nol pemanggil.</b> Tidak ada kode di repo yang memanggil method ini; ia dipertahankan
	 * agar pemetaan Hibernate atas kolom <code>posting_history_pajak</code> tetap valid. Bila
	 * kelak jalur posting pajak hendak disambungkan ke dokumen LPJ, di sinilah cap itu harus
	 * ditulis.</p>
	 *
	 * @param postingHistoryPajak cap posting kaki pajak
	 */
	public void setPostingHistoryPajak(PostingHistory postingHistoryPajak) {
		this.postingHistoryPajak = postingHistoryPajak;
	}

	/**
	 * Menyatakan apakah sisa panjar sudah dikembalikan pegawai.
	 *
	 * <p><b>Kasus tepi:</b> <code>null</code> dibaca sebagai <code>false</code>. Bendera ini
	 * berdiri sendiri &mdash; tidak diturunkan dari {@link #getDikembalikan()} maupun dari cap
	 * {@link #getPostingHistoryPengembalian()}, sehingga ketiganya dapat saling bertentangan
	 * (mis. bendera <code>true</code> padahal jurnal pengembalian belum pernah terbit).</p>
	 *
	 * @return <code>true</code> bila sisa panjar dinyatakan sudah kembali; tidak pernah
	 *         <code>null</code>
	 */
	public Boolean getTelahDikembalikan() {
		return telahDikembalikan == null ? false : telahDikembalikan;
	}

	/**
	 * Menyetel bendera &quot;sisa panjar sudah dikembalikan&quot;.
	 *
	 * <p><b>Efek samping tidak langsung:</b> menyetel <code>true</code> membuat
	 * {@link #getTanggalDikembalikan()} mengisi tanggal otomatis pada pembacaan berikutnya bila
	 * tanggal itu masih kosong.</p>
	 *
	 * @param telahDikembalikan bendera; <code>null</code> dibaca sebagai <code>false</code>
	 */
	public void setTelahDikembalikan(Boolean telahDikembalikan) {
		this.telahDikembalikan = telahDikembalikan;
	}

	/**
	 * Mengembalikan tanggal sisa panjar dikembalikan &mdash; <b>getter yang menulis balik</b>.
	 *
	 * <p><b>Cara kerja:</b> bila {@link #getTelahDikembalikan()} bernilai <code>true</code>
	 * sedangkan tanggalnya masih kosong, kolom diisi <code>WaktuUtil.getDate()</code> &mdash;
	 * waktu <i>pembacaan</i>.</p>
	 *
	 * <p><b>Kasus tepi (penting):</b> tanggal yang tercatat karena itu adalah <b>kapan baris ini
	 * pertama kali dibaca setelah bendera dinyalakan</b>, bukan kapan uang benar-benar disetor.
	 * Bila bendera dinyalakan oleh proses batch dan barisnya baru dibuka berminggu-minggu
	 * kemudian, tanggal pengembalian yang terekam ikut mundur ke tanggal pembukaan itu. Sekali
	 * terisi, nilainya tidak pernah dihitung ulang; mematikan kembali benderanya juga tidak
	 * menghapus tanggal yang sudah terlanjur tercatat.</p>
	 *
	 * @return tanggal pengembalian, atau <code>null</code> bila bendera belum menyala
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dikembalikan")
	public Date getTanggalDikembalikan() {
		if (getTelahDikembalikan() && tanggalDikembalikan == null) {
			tanggalDikembalikan = WaktuUtil.getDate();
		}
		return tanggalDikembalikan;
	}

	/**
	 * Menyetel tanggal sisa panjar dikembalikan.
	 *
	 * @param tanggalDikembalikan tanggal pengembalian; boleh <code>null</code>
	 */
	public void setTanggalDikembalikan(Date tanggalDikembalikan) {
		this.tanggalDikembalikan = tanggalDikembalikan;
	}

	/**
	 * Mengembalikan cap posting <b>kaki pengembalian sisa panjar</b>.
	 *
	 * <p>Terisi oleh <code>PostingPertangungjawabanPengembalianAction</code> dengan jenis
	 * <code>PostingHistory.JENIS_PENGEMBALIAN_UANG_MUKA</code>. Jurnal yang diterbitkan adalah
	 * Dr <code>uangMuka.jenisUangMuka.akunKelebihan</code> / Cr
	 * <code>uangMuka.jenisUangMuka.akun</code> sebesar {@link #getDikembalikan()}; baris dengan
	 * nilai nol atau akun belum lengkap dilewati.</p>
	 *
	 * <p><b>Non-obvious:</b> kaki ini <b>tidak mensyaratkan</b> kaki utama sudah diposting &mdash;
	 * ketiga layar posting bekerja atas filternya masing-masing, sehingga urutan cap tidak
	 * dijamin. Pembatalan posting pada layar ini pun, seperti pada kaki utama, hanya menghapus
	 * baris <code>grup_transaksi</code> dan menyisakan baris <code>transaksi</code>.</p>
	 *
	 * @return cap posting kaki pengembalian, atau <code>null</code> bila belum dijurnal
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history_pengembalian", nullable = true)
	public PostingHistory getPostingHistoryPengembalian() {
		return postingHistoryPengembalian;
	}

	/**
	 * Menyetel (atau menghapus) cap posting kaki pengembalian.
	 *
	 * <p><b>Dipanggil dari:</b> <code>PostingPertangungjawabanPengembalianAction</code> &mdash;
	 * diisi saat posting berhasil, disetel <code>null</code> saat pembatalan posting
	 * (per-baris maupun massal).</p>
	 *
	 * @param postingHistoryPengembalian cap posting, atau <code>null</code> untuk membatalkannya
	 */
	public void setPostingHistoryPengembalian(PostingHistory postingHistoryPengembalian) {
		this.postingHistoryPengembalian = postingHistoryPengembalian;
	}

	/**
	 * Mengembalikan total PPh yang melekat pada rincian belanja LPJ ini.
	 *
	 * <p><b>Jangan dikelirukan</b> dengan entity {@link ais.database.model.akunting.Pajak}: yang
	 * ini sekadar <i>angka ringkasan</i> hasil penjumlahan
	 * <code>JenisPajakBarang.getPersen() / 100 &times; jumlah</code> untuk setiap item
	 * {@link #getFormula() formula} yang punya kunci <code>pajak</code>. Baris pajak yang
	 * sesungguhnya (beserta NTPN, NPWP, dan cap postingnya) dibuat terpisah oleh
	 * <code>Pajak.buat(...)</code>.</p>
	 *
	 * <p><b>Peran dalam jurnal:</b> pada kaki utama, nominal kredit
	 * <code>JenisUangMuka.akun</code> adalah <b>nilai realisasi dikurangi total PPh</b>,
	 * sementara PPh-nya dikreditkan ke akun pajak masing-masing.</p>
	 *
	 * <p><b>Integritas (audit):</b> basis PPh adalah <code>jumlah</code> mentah tiap item &mdash;
	 * potongan harga tidak dikurangkan lebih dulu, sejalan dengan temuan pada
	 * {@link ais.database.model.akunting.Pajak}. Selain itu, apakah PPh mengurangi nilai LPJ
	 * atau tidak ditentukan konfigurasi global <code>pph_mengurangi_lpj</code>, sehingga
	 * mengubah konfigurasi tersebut mengubah arti angka ini untuk dokumen lama.</p>
	 *
	 * <p><b>Kasus tepi:</b> <code>null</code> dinormalkan menjadi <code>0.0</code>.</p>
	 *
	 * @return total PPh dalam rupiah; tidak pernah <code>null</code>
	 */
	public Double getPajak() {
		return pajak == null ? 0.0 : pajak;
	}

	/**
	 * Menyetel total PPh ringkasan.
	 *
	 * @param pajak nominal rupiah; <code>null</code> dibaca sebagai <code>0.0</code>
	 */
	public void setPajak(Double pajak) {
		this.pajak = pajak;
	}

	/**
	 * Mengembalikan tanggal setor sisa panjar ke kas &mdash; <b>getter destruktif</b>.
	 *
	 * <p><b>Cara kerja:</b></p>
	 * <ol>
	 *   <li>Bila {@link #getDikembalikan()} lebih kecil dari <code>0.1</code> (praktis: tidak ada
	 *       sisa yang harus disetor), tanggal setor <b>dihapus</b> menjadi <code>null</code>.</li>
	 *   <li>Bila ada sisa tetapi tanggalnya belum terisi, tanggal diisi dari
	 *       {@link #getTanggal_dirubah()}.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping (penting):</b> cabang 1 bersifat merusak. Sebuah LPJ yang tanggal
	 * setornya sudah diisi petugas dapat <b>kehilangan tanggal itu secara permanen</b> hanya
	 * karena {@link #getDikembalikan()} berubah menjadi (mendekati) nol &mdash; misalnya setelah
	 * revisi rincian, atau setelah tombol &quot;Hitung Ulang&quot; menulis ulang kolom
	 * <code>dikembalikan</code> dengan rumus yang keliru. Karena kelas ini
	 * <code>dynamicUpdate</code>, penghapusan itu tersimpan pada flush berikutnya.</p>
	 *
	 * <p><b>Kasus tepi:</b> cabang 2 memakai stempel perubahan terakhir, sehingga tanggal setor
	 * yang terekam bisa jadi hanyalah waktu penyuntingan terakhir dokumen, bukan tanggal
	 * penyetoran sesungguhnya. Kolom ini <code>TemporalType.DATE</code> (tanpa komponen jam),
	 * berbeda dari mayoritas kolom waktu lain di kelas ini yang bertipe
	 * <code>TIMESTAMP</code>.</p>
	 *
	 * @return tanggal setor, atau <code>null</code> bila tidak ada sisa yang harus disetor
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalStor() {
		if (getDikembalikan() < 0.1) {
			tanggalStor = null;
		} else if (tanggalStor == null) {
			tanggalStor = getTanggal_dirubah();
		}
		return tanggalStor;
	}

	/**
	 * Menyetel tanggal setor sisa panjar.
	 *
	 * <p><b>Kasus tepi:</b> nilai yang disetel akan dihapus kembali oleh
	 * {@link #getTanggalStor()} bila sisa panjar dokumen ini kurang dari <code>0.1</code>.</p>
	 *
	 * @param tanggalStor tanggal setor; boleh <code>null</code>
	 */
	public void setTanggalStor(Date tanggalStor) {
		this.tanggalStor = tanggalStor;
	}

	/**
	 * Mengembalikan tanggal persetujuan yang diketik manual oleh approver.
	 *
	 * <p><b>Peran:</b> kolom ini adalah <i>jalan pulih</i> bagi dokumen yang disetujui di luar
	 * mesin SOP. {@link #getTanggalPersetujuan()} memprioritaskannya selama
	 * {@link #getDisetujuiOleh()} masih terisi, sehingga tanggal manual bertahan meski cabang
	 * SOP sempat mengosongkan tanggal turunan.</p>
	 *
	 * <p><b>Kasus tepi:</b> tidak ada validasi bahwa tanggal ini berada dalam periode
	 * {@link #getTahun()}/{@link #getBulan()}, tidak ada larangan tanggal mundur, dan tidak ada
	 * pemeriksaan bahwa ia lebih besar dari {@link #getTanggalPembuatan()} &mdash; approver dapat
	 * memasukkan tanggal persetujuan sebelum tanggal pengajuan.</p>
	 *
	 * @return tanggal persetujuan manual, atau <code>null</code> bila tidak dipakai
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggalPersetujuanManual() {
		return tanggalPersetujuanManual;
	}

	/**
	 * Menyetel tanggal persetujuan manual.
	 *
	 * <p><b>Dipanggil dari:</b> <code>PertangungjawabanAction.onSave</code> (nilai datebox
	 * &quot;Tanggal Persetujuan&quot; pada mode persetujuan) dan blok sinkronisasi status SOP.</p>
	 *
	 * @param tanggalPersetujuanManual tanggal persetujuan yang diketik manual; boleh
	 *        <code>null</code>
	 */
	public void setTanggalPersetujuanManual(Date tanggalPersetujuanManual) {
		this.tanggalPersetujuanManual = tanggalPersetujuanManual;
	}
}
