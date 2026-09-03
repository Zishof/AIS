package ais.database.model.payroll;

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

import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;
import ais.database.model.akunting.PostingHistory;

/**
 * Satu baris <b>transaksi pegawai</b> — mutasi keuangan pegawai <b>di luar komponen gaji
 * reguler</b>: potongan kasbon/pinjaman yang dicicil, potongan koperasi, denda, iuran, atau
 * tambahan lepas. Dipetakan ke tabel <code>payroll.transaksi_pegawai</code>.
 *
 * <p>Entity ini adalah <b>baris angsuran</b>, bukan dokumen pengajuannya. Dokumen pengajuannya
 * adalah {@link PengajuanTransaksiPegawai} (satu pengajuan dengan nominal total dan jumlah
 * angsuran); entity ini adalah satu tarikan bulanan dari pengajuan itu, dibedakan oleh
 * {@link #getKe()} (angsuran ke-1, ke-2, dan seterusnya). Jenis/kategorinya — beserta pasangan
 * akun jurnalnya — datang dari katalog {@link JenisTransaksiPegawai}.</p>
 *
 * <h3>1. Dua asal-usul baris, dan mengapa getter-nya menulis balik</h3>
 * <p>Baris di tabel ini lahir dari tiga jalur yang sangat berbeda:</p>
 * <ol>
 *   <li><b>Manual</b> — layar <code>TransaksiPegawaiAction</code>
 *       (<code>transaksi_pegawai.zul</code>). Operator mengisi sendiri pegawai, jenis, nilai,
 *       tanggal, dan keterangan. Baris seperti ini <b>tidak</b> punya
 *       {@link #getPengajuanTransaksiPegawai()}. Validasi <code>onSave</code>-nya hanya menuntut
 *       pegawai dan jenis terisi — nominal, tanggal, dan duplikasi tidak diperiksa.</li>
 *   <li><b>Impor Excel</b> — layar yang sama menyediakan
 *       <code>Common.uploadData(this, TransaksiPegawai.class, ...)</code> untuk kolom
 *       <code>{id, pegawai, jenisTransaksiPegawai, nilai, tanggal, keterangan,
 *       postingHistory}</code>. Jalur ini melewati <code>onSave</code>, jadi ia juga melewati satu-
 *       satunya validasi yang ada.</li>
 *   <li><b>Turunan pengajuan</b> — <code>PengajuanTransaksiPegawaiAction.populateTransaksi()</code>
 *       membuat <code>jumlahAngsur</code> baris sekaligus begitu sebuah pengajuan disetujui.
 *       Baris-baris itu dibuat dengan <b>hanya dua properti terisi</b>:
 *       {@code pengajuanTransaksiPegawai} dan {@code ke}. Pegawai, jenis transaksi, nilai,
 *       tanggal, dan keterangan seluruhnya dibiarkan {@code null} oleh pemanggil.</li>
 * </ol>
 * <p>Jalur kedua itulah kunci untuk memahami bentuk kelas ini. Kolom <code>pegawai</code> dan
 * <code>jenis_transaksi_pegawai</code> dideklarasikan <code>nullable = false</code>, jadi baris
 * yang lahir "kosong" seharusnya gagal disimpan. Yang menyelamatkannya adalah pemetaan
 * <b>property-access</b> (anotasi menempel pada getter, bukan field): saat Hibernate menyusun
 * INSERT, ia memanggil setiap getter — dan getter di kelas ini <b>menurunkan sendiri</b> nilainya
 * dari pengajuan induk lalu <b>menuliskannya balik</b> ke field. {@link #getPegawai()},
 * {@link #getJenisTransaksiPegawai()}, {@link #getNilai()}, {@link #getTanggal()},
 * {@link #getKeterangan()}, {@link #getTgl()}, {@link #getBln()}, dan {@link #getThn()} semuanya
 * bekerja begitu. Jadi tulis-balik pada getter di kelas ini <b>bukan kecelakaan</b>: itu mesin
 * pengisian barisnya.</p>
 * <p><b>Konsekuensinya, dan inilah bagian yang berbahaya:</b> mesin yang sama tidak pernah
 * berhenti bekerja setelah INSERT. Karena kelas ini juga <code>dynamicUpdate = true</code>, setiap
 * kali baris turunan pengajuan dibaca di dalam sesi Hibernate yang aktif, kelima nilai itu
 * <b>dihitung ulang dari keadaan pengajuan TERKINI</b> dan ditulis balik ke basis data. Nominal
 * yang sudah dikoreksi manual oleh operator akan diam-diam kembali ke hasil rumus; dan mengubah
 * <code>nilaiTransaksi</code>, <code>jumlahAngsur</code>, atau <code>tanggalJatuhTempo</code> pada
 * pengajuan akan <b>mengubah secara retroaktif</b> angsuran yang sudah lama terbit — termasuk
 * angsuran yang jurnalnya sudah dibukukan. Jurnal di buku besar tidak ikut berubah, sehingga
 * daftar transaksi dan buku besar bisa berbeda tanpa jejak. Baris manual (tanpa pengajuan) kebal
 * terhadap semua ini.</p>
 *
 * <h3>2. Peran sebagai dokumen sumber jurnal — TERVERIFIKASI dari sisi entity ini</h3>
 * <p>Entity ini <b>benar</b> dipakai sebagai <i>reference</i> mesin posting akuntansi. Jalurnya
 * diverifikasi ujung ke ujung dari kode:</p>
 * <ul>
 *   <li><code>PostingTransaksiPegawaiAction</code> memanggil overload khusus
 *       <code>CommonAkunting.saveTransaksi(TransaksiPegawai, Akun akunDebet, PostingHistory,
 *       boolean apakahUangMasuk, SatuanKerja, Session)</code>;</li>
 *   <li>overload itu membangun satu <code>GrupTransaksi</code> (header jurnal) dan dua
 *       <code>Transaksi</code> (baris debet dan kredit), lalu memanggil
 *       <code>grupTransaksi.setTransaksiPegawai(transaksiPegawai)</code> — yakni mengisi kolom
 *       referensi <code>akunting.grup_transaksi.transaksi_pegawai</code> dengan baris ini;</li>
 *   <li>pasangan akunnya diambil dari katalog: <b>debet</b> =
 *       <code>jenisTransaksiPegawai.getAkunDebet()</code>, <b>kredit</b> =
 *       <code>jenisTransaksiPegawai.getAkun()</code>. Arah dibalik bila {@link #getNilai()}
 *       negatif (parameter <code>apakahUangMasuk</code>), dan nominal selalu diambil
 *       <code>Math.abs()</code>-nya;</li>
 *   <li>bila salah satu dari kedua akun itu {@code null}, dokumen <b>dilewati diam-diam</b> —
 *       layar hanya menampilkan label "Transaksi tidak valid", jalur API mem-<code>continue</code>.
 *       Beban/potongannya tetap dipotong dari gaji tetapi tidak pernah masuk buku besar.</li>
 * </ul>
 * <p><b>Dua penyandian arah yang tidak pernah disinkronkan.</b> Katalog
 * {@link JenisTransaksiPegawai} punya kolom <code>jenisTransaksi</code> (1 = Debet/penambah,
 * 2 = Kredit/potongan) — tetapi kolom itu <b>hanya</b> dibaca layar katalog dan dua dasbor
 * penggajian sebagai label. Mesin posting sama sekali tidak melihatnya; arah jurnal ditentukan
 * semata oleh <b>tanda</b> {@link #getNilai()}. Jadi sebuah baris bisa berjenis "Kredit
 * (potongan)" di katalog namun terjurnal sebagai penambah karena nominalnya kebetulan positif,
 * tanpa satu pun penjaga yang menyelaraskan keduanya.</p>
 * <p>Cap posting disimpan pada {@link #getPostingHistory()}; {@code null} berarti belum diposting.
 * Cap itu pula yang dipakai kedua tombol pembatalan untuk memilih baris.</p>
 *
 * <h3>3. Tabrakan kunci jurnal dengan slip gaji — status dari sisi entity ini</h3>
 * <p>Header jurnal <code>GrupTransaksi</code> menyimpan kunci idempotensi
 * <code>kodeUnik</code> yang disusun <code>ambilUnik()</code> sebagai
 * "<i>nama kelas dokumen sumber</i>_<i>id</i>". Dulu cabang untuk field
 * <code>transaksiPegawai</code> keliru menuliskan nama kelas
 * {@link ais.database.model.payroll.PembayaranGajiPunyaPegawai} — bukan nama kelas ini. Akibatnya
 * jurnal yang bersumber dari <b>TransaksiPegawai id=N</b> menempati kunci yang seharusnya milik
 * <b>slip gaji id=N</b>. Karena kedua tabel memakai IDENTITY yang sama-sama mulai dari 1 dan kunci
 * itu global lintas tenant, tumpang tindih id kecil praktis pasti terjadi.</p>
 * <p><b>Status:</b> sudah diperbaiki. <code>svn blame</code> pada
 * <code>GrupTransaksi.ambilUnik()</code> menunjukkan baris tersebut ditulis ulang menjadi
 * <code>TransaksiPegawai.class.getName()</code> pada <b>r83966</b>, terpisah dari r75894 yang
 * menulis seluruh cabang lain di sekitarnya.</p>
 * <p><b>Yang baru terverifikasi dari sisi ini — arah kerusakan historisnya:</b> overload
 * <code>saveTransaksi(TransaksiPegawai, ...)</code> yang dipakai entity ini <b>tidak pernah</b>
 * memeriksa <code>kodeUnik</code> sebelum menyimpan; ia langsung <code>session.save()</code>.
 * Jadi kunci yang salah tidak pernah menghalangi jurnal transaksi pegawai itu sendiri — ia hanya
 * <b>meracuni</b> ruang kunci. Sebaliknya, overload generik
 * <code>saveTransaksi(Akun[], ..., Object reference, ...)</code> — yang justru dipakai posting
 * <b>gaji</b> (<code>PostingTransaksiPembayaranGajiAction</code>,
 * <code>PostingTransaksiPenggajianAction</code>) — <b>memang</b> mencari
 * <code>GrupTransaksi</code> ber-<code>kodeUnik</code> sama lebih dulu, dan bila ketemu ia hanya
 * menempelkan <code>postingHistory</code> pada jurnal lama itu lalu berhenti. Maka dampak
 * historisnya bukan "jurnal transaksi pegawai hilang", melainkan sebaliknya: <b>slip gaji id=N
 * yang diposting setelah transaksi pegawai id=N akan gagal terjurnal secara diam-diam</b> —
 * penjaga anti-jurnal-ganda menyangka jurnalnya sudah ada, padahal yang ada adalah jurnal potongan
 * kasbon milik dokumen lain. Slip itu tetap tampak "sudah diposting" karena cap
 * <code>postingHistory</code>-nya terpasang. Perbaikan r83966 menghentikan racun baru, tetapi
 * <b>tidak ada backfill</b>: baris <code>grup_transaksi.kode_unik</code> lama masih menyandang
 * nama kelas yang keliru, sehingga slip gaji lama yang belum pernah diposting masih bisa
 * terhalang. Pemeriksaan data historis berada di luar cakupan kode.</p>
 *
 * <h3>4. Kaitan ke slip gaji dan ke rumus gaji</h3>
 * <p>Ada dua kaitan yang berbeda dan mudah tertukar:</p>
 * <ul>
 *   <li><b>Pelunasan</b> — {@link #getPembayaranGajiPunyaPegawai()} menandai slip gaji mana yang
 *       akhirnya memotong baris ini. Kolom itu diisi oleh
 *       <code>PembayaranItemGajiPegawaiTreeModel.setLunas(Date)</code>, yang menyapu <b>semua</b>
 *       baris pegawai bersangkutan yang masih {@code null} dengan
 *       <code>thn &lt; tahun OR (thn = tahun AND bln &lt;= bulan)</code> lalu menstempelnya
 *       sekaligus. Perhatikan bahwa kriteria itu membaca <b>kolom</b> <code>bln</code>/
 *       <code>thn</code> di basis data, bukan hasil {@link #getBln()}/{@link #getThn()}; baris
 *       yang kedua kolom itu masih {@code null} tidak cocok dengan cabang manapun dan
 *       <b>terlewat selamanya</b>.</li>
 *   <li><b>Perhitungan rumus gaji</b> — <code>ItemGajiPegawaiTreeModel</code> menjumlahkan kolom
 *       <code>nilai</code> per {@link JenisTransaksiPegawai} dan menyuntikkan hasilnya ke mesin
 *       rumus {@link ItemGaji} sebagai variabel bernama <code>JenisTransaksiPegawai.kode</code>
 *       (dan, bila diisi, <code>formula</code>-nya). Jadi komponen gaji mana pun boleh menyebut
 *       kode jenis transaksi di rumusnya untuk memotong/menambah otomatis.</li>
 * </ul>
 * <p>Tiga kuirk pada jalur rumus gaji itu perlu diketahui sebelum menyentuhnya:</p>
 * <ul>
 *   <li>penjumlahannya memakai <b>proyeksi SQL</b> atas kolom <code>nilai</code>, jadi ia
 *       melewati {@link #getNilai()} sepenuhnya — yang dijumlahkan adalah nilai <i>terakhir kali
 *       ter-flush</i>, bukan hasil hitung ulang;</li>
 *   <li>daftar jenis ditemukan dengan <code>bln = bulan AND thn = tahun</code> (persis satu
 *       periode), tetapi nominalnya dijumlahkan dengan <code>bln &lt;= bulan</code> <b>dan</b>
 *       <code>thn &lt;= tahun</code> sebagai dua syarat terpisah. Itu bukan jendela "sampai
 *       dengan periode": menghitung Maret 2026 akan ikut menjumlahkan Januari–Maret <i>semua</i>
 *       tahun sebelumnya tetapi <b>membuang</b> April–Desember tahun-tahun itu. Untuk cicilan
 *       bulanan, angka yang masuk rumus gaji karena itu bukan satu angsuran melainkan akumulasi
 *       parsial yang membesar setiap tahun;</li>
 *   <li>penjumlahannya tidak menyaring {@link #getPembayaranGajiPunyaPegawai()} maupun
 *       <code>JenisTransaksiPegawai.aktif</code> — angsuran yang sudah dilunasi dan jenis yang
 *       sudah dinonaktifkan tetap ikut dijumlahkan.</li>
 * </ul>
 *
 * <h3>5. Tidak ada sumbu tenant sama sekali</h3>
 * <p>Entity ini <b>tidak punya</b> kolom <code>yayasan</code>, <code>sekolah</code>,
 * <code>satuanKerja</code>, maupun kolom pemilik institusi lain. Satu-satunya jalur ke tenant
 * adalah lewat {@link #getPegawai()}. Ini bukan fail-open bersyarat, melainkan ketiadaan sumbu —
 * sama seperti <code>Closing</code> dan <code>ProsesTransferStandingInstruction</code> di paket
 * akunting. Akibat yang terverifikasi:</p>
 * <ul>
 *   <li><code>PostingTransaksiPegawaiAction.initCriteria()</code> tidak menyaring tenant sama
 *       sekali; daftar dan aksi massalnya menjangkau <b>seluruh instalasi</b>;</li>
 *   <li>jalur API <code>postingSemua()</code> bahkan meneruskan <code>satuanKerja = null</code> ke
 *       mesin posting, sehingga jurnal yang terbit tidak bersatuan kerja (global);</li>
 *   <li>pada Generic CRUD v2, <code>GenericCrudAutoEntityAdapter.scopeBindings()</code> hanya
 *       memasang restriksi untuk 12 nama properti tetap. Tak satu pun properti relasi entity ini
 *       (<code>pegawai</code>, <code>jenisTransaksiPegawai</code>,
 *       <code>pembayaranGajiPunyaPegawai</code>, <code>pengajuanPeminjaman</code>,
 *       <code>pengajuanTransaksiPegawai</code>, <code>postingHistory</code>) ada di daftar itu,
 *       sehingga pengikatannya <b>kosong</b> dan <code>applyScope()</code> tidak menambahkan
 *       restriksi apa pun. <code>validateObjectScope()</code> menelusuri peta kosong yang sama,
 *       jadi jalur <b>tulis</b> pun tidak diperiksa;</li>
 *   <li>jangkauannya bukan teoretis: empat halaman New UI mendaftarkan entity ini
 *       (<code>transaksi_pegawai</code>, <code>posting_transaksi_pegawai</code>, dan dua halaman
 *       <i>tree model</i> gaji), dan pemilih kelas otomatis memenangkan kelas ini atas
 *       {@link JenisTransaksiPegawai} untuk halaman <code>transaksi_pegawai</code>. Nama kelasnya
 *       tidak cocok dengan token terlarang mana pun, dan <code>TransaksiPegawaiAction</code> punya
 *       <code>boolean onSave(Event)</code>, sehingga mode yang diberikan adalah <b>FULL_CRUD</b>
 *       (baca + buat + ubah). Hapus kebetulan tidak aktif — hanya karena entity ini tidak punya
 *       properti <code>aktif</code> untuk hapus lunak, bukan karena ada penjaga.</li>
 * </ul>
 * <p><b>Koreksi terhadap arah perbaikan yang pernah diusulkan:</b> berbeda dari {@link ItemGaji},
 * entity ini <b>memang</b> punya properti <code>pegawai</code>, sehingga menambahkan
 * <code>pegawai</code> ke whitelist akan benar-benar mengikat. Tetapi ia mengikat ke pegawai milik
 * <i>pengguna yang sedang login</i> — yang justru melumpuhkan layar bagi staf HRD, audiens
 * sesungguhnya halaman ini. Sumbu yang benar untuk data ini adalah institusi, dan sumbu itu tidak
 * ada di tabel sama sekali. Perbaikan yang sungguh menutup celah adalah menjadikan pengikatan
 * kosong sebagai <b>penolakan</b>, bukan restriksi-nol.</p>
 * <p>Cakupan tenant induknya sendiri sebenarnya ada:
 * <code>PengajuanTransaksiPegawai.getSatuanKerja()</code> menurunkannya dari pegawai, dan layar
 * pengajuan menyaring dengan pohon satuan kerja. Cakupan itu <b>hilang seluruhnya</b> begitu
 * pengajuan dimaterialisasi menjadi baris di kelas ini. Laporan Jasper
 * <code>laporan_transaksi_pegawai.jrxml</code> bahkan mendeklarasikan parameter
 * <code>sekolah</code>/<code>yayasan</code> dan menerimanya dari pemanggil, tetapi query-nya tidak
 * pernah memakainya.</p>
 *
 * <h3>6. Gerbang otorisasi posting</h3>
 * <p>Dua layar menyentuh entity ini dengan standar yang sangat berbeda:</p>
 * <ul>
 *   <li><code>TransaksiPegawaiAction</code> (CRUD manual) <b>benar</b> — tombol tambah/ubah/hapus
 *       masing-masing digerbangi <code>CommonPrivilages.CREATE/UPDATE/DELETE</code>;</li>
 *   <li><code>PostingTransaksiPegawaiAction</code> (posting ke buku besar) <b>tidak</b> —
 *       <code>doAfterCompose()</code> hanya memeriksa <code>READ</code>. Tombol "Posting Semua"
 *       dan "Batalkan Posting Semua" di <code>posting_transaksi_pegawai.zul</code> dirender tanpa
 *       syarat apa pun, dan kedua handler-nya (<code>onPostingSemua</code>,
 *       <code>onBatalkanPostingSemua</code>) tidak memeriksa hak sekali pun. Hanya tombol
 *       <i>batal per baris</i> yang meminta admin/APPROVE — tombol <i>posting per baris</i> pun
 *       tidak. Bentuknya sekeluarga dengan asimetri yang sudah dicatat untuk layar posting gaji,
 *       tetapi di sini bahkan sisi pembatalan massalnya ikut terbuka.</li>
 * </ul>
 * <p>Jalur REST-nya (<code>DraftJurnalApiHelper</code>, baris dasbor "Transaksi Pegawai" →
 * kunci menu <code>gaji</code>) memang memeriksa hak "create", tetapi <code>bolehAksi()</code>-nya
 * mengembalikan {@code true} ketika <code>tbmuser.hakAkses()</code> bernilai {@code null} — pola
 * fail-open yang sama dengan yang sudah dilacak di sederet helper API modul keuangan.</p>
 *
 * <h3>7. Catatan teknis lain</h3>
 * <ul>
 *   <li><b>Induk POJO.</b> {@link ais.database.model.GeneralValueObject} <b>bukan</b>
 *       <code>@Entity</code> maupun <code>@MappedSuperclass</code> — Hibernate tidak memetakan
 *       propertinya. Karena itu {@code id}, {@code oleh}, {@code olehId}, dan
 *       {@code tanggal_dirubah} <b>wajib</b> dideklarasikan ulang di sini; itu keharusan teknis,
 *       bukan duplikasi yang bisa dibersihkan.</li>
 *   <li><b>Envers.</b> Kelas ini <code>@Audited</code>: setiap versi baris digandakan ke
 *       <code>payroll.transaksi_pegawai_aud</code>. Karena getter turunan menulis balik pada
 *       pembacaan, revisi Envers bisa bertambah tanpa ada aksi pengguna sama sekali.</li>
 *   <li><b>Pembatalan posting jalur ZK menyisakan baris yatim.</b> Tombol batal di layar (baik per
 *       baris maupun massal) hanya menjalankan
 *       <code>delete from akunting.grup_transaksi where transaksi_pegawai=&hellip; and closing is
 *       null</code> — baris anak <code>akunting.transaksi</code> tidak ikut dihapus. Jalur API
 *       statis <code>batalkanPostingSemua()</code> sudah benar (menghapus anak lebih dulu). Pola
 *       yang sama sudah dikenal di modul uang muka.</li>
 *   <li><code>toString()</code> mengembalikan {@link #keterangan} <b>mentah</b> (field, bukan
 *       getter), sehingga bisa {@code null} untuk baris yang belum pernah dibaca.</li>
 * </ul>
 *
 * @see PengajuanTransaksiPegawai
 * @see JenisTransaksiPegawai
 * @see PembayaranGajiPunyaPegawai
 * @see PengajuanPeminjaman
 * @see ais.database.model.akunting.PostingHistory
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "payroll", name = "transaksi_pegawai")
public class TransaksiPegawai extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya sama persis dengan
	 * {@link JenisTransaksiPegawai#serialVersionUID} — jejak generator hbm2java 2010 yang memakai
	 * satu angka untuk sekeluarga kelas, bukan tanda kekerabatan.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama, IDENTITY. Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang menyentuh baris. Lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang menyentuh baris. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Id pengguna terakhir yang menyimpan baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna penyimpan.
	 *
	 * <p><b>Perhatian:</b> argumen {@code null}, string kosong, atau yang hanya berisi spasi
	 * <b>diabaikan diam-diam</b> — nilai lama dipertahankan. Jejak audit karena itu hanya bisa
	 * bertambah maju, tidak bisa dikosongkan; pemanggil yang ingin "menghapus" jejak tidak akan
	 * mendapat efek apa pun dan juga tidak mendapat error.</p>
	 *
	 * @param olehId id pengguna; nilai kosong/null diabaikan.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks baris untuk label ZK dan combo.
	 *
	 * <p><b>Kuirk:</b> yang dikembalikan adalah <b>field</b> {@link #keterangan} apa adanya, bukan
	 * {@link #getKeterangan()}. Untuk baris turunan pengajuan yang belum pernah dibaca lewat
	 * getter-nya, field itu masih {@code null} sehingga method ini mengembalikan {@code null} —
	 * bukan string kosong. Pemanggil yang merangkainya dengan {@code +} akan mendapat teks
	 * "null".</p>
	 *
	 * @return keterangan mentah, mungkin {@code null}.
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Menyetel nama pengguna penyimpan.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: nilai {@code null}/kosong/spasi diabaikan diam-diam.</p>
	 *
	 * @param oleh nama pengguna; nilai kosong/null diabaikan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna terakhir yang menyimpan baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA sebelum UPDATE: menyerahkan pembaruan stempel waktu ke
	 * {@code AuditTimestampInterceptor.ubah(this)}.
	 *
	 * <p>Deklarasi field {@link #tanggal_dirubah} sengaja ditempelkan pada baris yang sama oleh
	 * generator; jangan dipisah tanpa alasan agar diff terhadap kelas sekeluarga tetap rapi.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir. Diinisialisasi ke waktu pembuatan object dan diperbarui
	 * oleh {@link #onUpdate()}.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk object yang baru dibuat.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Pegawai pemilik transaksi. Diturunkan dari pengajuan bila ada — lihat {@link #getPegawai()}. */
	private Pegawai pegawai;
	/** Kategori transaksi sekaligus sumber pasangan akun jurnal. Lihat {@link #getJenisTransaksiPegawai()}. */
	private JenisTransaksiPegawai jenisTransaksiPegawai;
	/** Nominal satu angsuran. Dihitung ulang dari pengajuan pada setiap pembacaan — lihat {@link #getNilai()}. */
	private Double nilai;
	/** Tanggal jatuh tempo angsuran ini. Dihitung ulang dari pengajuan — lihat {@link #getTanggal()}. */
	private Date tanggal = ais.ui.util.WaktuUtil.getDate();
	/** Cerminan hari dari {@link #tanggal}. Lihat {@link #getTgl()}. */
	private Integer tgl;
	/** Cerminan bulan (1-12) dari {@link #tanggal}. Dipakai kriteria pelunasan &amp; rumus gaji — lihat {@link #getBln()}. */
	private Integer bln;
	/** Cerminan tahun dari {@link #tanggal}. Dipakai kriteria pelunasan &amp; rumus gaji — lihat {@link #getThn()}. */
	private Integer thn;

	/** Nomor urut angsuran (1..jumlahAngsur). Lihat {@link #getKe()}. */
	private Integer ke;

	/** Uraian baris; disusun ulang dari pengajuan bila ada — lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Slip gaji yang melunasi baris ini; {@code null} = belum dipotongkan. Lihat {@link #getPembayaranGajiPunyaPegawai()}. */
	private PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai;
	/** Dokumen pengajuan peminjaman terkait, bila baris berasal dari modul peminjaman. */
	private PengajuanPeminjaman pengajuanPeminjaman;
	/** Dokumen pengajuan transaksi pegawai induk; sumber seluruh nilai turunan kelas ini. */
	private PengajuanTransaksiPegawai pengajuanTransaksiPegawai;
	/** Cap posting jurnal; {@code null} = belum diposting. Lihat {@link #getPostingHistory()}. */
	private PostingHistory postingHistory;

	/** Konstruktor tanpa argumen yang diwajibkan Hibernate. */
	public TransaksiPegawai() {
	}

	/**
	 * Kunci utama baris, dibangkitkan basis data (IDENTITY).
	 *
	 * <p>Nilai inilah yang ikut menyusun kunci idempotensi jurnal
	 * <code>GrupTransaksi.kodeUnik</code> dalam bentuk
	 * "<code>ais.database.model.payroll.TransaksiPegawai_&lt;id&gt;</code>" — lihat bagian 3 pada
	 * dokumentasi kelas mengenai tabrakan kunci historis dengan slip gaji.</p>
	 *
	 * @return id baris, atau {@code null} untuk object yang belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Dipakai Hibernate; jangan dipanggil kode aplikasi.
	 *
	 * @param id kunci utama.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Uraian baris yang tampil di daftar, slip, dan keterangan jurnal.
	 *
	 * <p><b>Getter destruktif.</b> Bila baris ini turunan {@link PengajuanTransaksiPegawai} yang
	 * pegawainya terisi, keterangan <b>disusun ulang</b> menjadi
	 * "<i>nama pengajuan</i> <i>keterangan pengajuan</i> angsuran ke <i>N</i>" dan <b>ditulis
	 * balik</b> ke field. Karena kelas ini dipetakan property-access dengan
	 * <code>dynamicUpdate</code>, teks hasil susunan itu ikut tersimpan permanen begitu sesi
	 * di-flush. Uraian yang diketik operator pada baris turunan pengajuan karena itu tidak pernah
	 * bertahan.</p>
	 * <p>Efek samping tambahan: method ini memanggil {@link #getPengajuanTransaksiPegawai()},
	 * sehingga relasi pengajuan ikut di-resolve (dan field {@link #pengajuanTransaksiPegawai}
	 * ditugasi ulang) walau pemanggil hanya ingin membaca teks.</p>
	 * <p><b>Kasus tepi:</b> <code>pengajuan.getNama()</code> masih bisa mengembalikan {@code null}
	 * (bila nama pengajuan maupun nama jenis pengajuannya sama-sama kosong) dan dirangkai di sini
	 * tanpa penjaga, sehingga keterangan yang tersimpan — dan ikut menjadi keterangan jurnal —
	 * berbunyi "null &hellip; angsuran ke N".</p>
	 * <p>Dipanggil dari renderer kedua layar (<code>TransaksiPegawaiAction</code>,
	 * <code>PostingTransaksiPegawaiAction</code>), dari penyusun keterangan jurnal di
	 * <code>CommonAkunting.saveTransaksi(TransaksiPegawai, ...)</code>, dan dari laporan
	 * <code>LaporanTransaksiPegawai</code>.</p>
	 *
	 * @return uraian baris; {@code null} bila belum pernah diisi dan baris bukan turunan pengajuan.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		pengajuanTransaksiPegawai = getPengajuanTransaksiPegawai();
		if (pengajuanTransaksiPegawai != null && pengajuanTransaksiPegawai.getPegawai() != null) {
			keterangan = pengajuanTransaksiPegawai.getNama() + " " + pengajuanTransaksiPegawai.getKeterangan()
					+ " angsuran ke " + getKe();
		}
		return this.keterangan;
	}

	/**
	 * Menyetel uraian baris.
	 *
	 * <p>Untuk baris turunan pengajuan, nilai yang disetel akan ditimpa lagi pada pembacaan
	 * berikutnya oleh {@link #getKeterangan()}.</p>
	 *
	 * @param keterangan uraian baris.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Pegawai pemilik transaksi ini. Kolom <code>pegawai</code> berstatus
	 * <code>nullable = false</code>.
	 *
	 * <p><b>Getter destruktif.</b> Alurnya tiga langkah: (1) proxy lazy diresolusi lewat
	 * {@code check()} warisan {@link ais.database.model.GeneralValueObject}; (2) relasi pengajuan
	 * diresolusi; (3) <b>bila</b> pengajuan ada dan punya pegawai, field {@link #pegawai}
	 * <b>ditimpa</b> dengan pegawai milik pengajuan lalu ikut tersimpan. Inilah yang membuat baris
	 * angsuran yang lahir tanpa pegawai tetap lolos <code>NOT NULL</code> saat INSERT — tetapi juga
	 * berarti memindahkan pengajuan ke pegawai lain akan <b>memindahkan seluruh angsurannya secara
	 * retroaktif</b>, termasuk angsuran yang jurnalnya sudah terbit atas nama pegawai lama.</p>
	 * <p>Relasi ini adalah <b>satu-satunya</b> jalur entity ini menuju institusi/tenant; tabel
	 * sendiri tidak punya kolom pemilik institusi (lihat bagian 5 dokumentasi kelas).</p>
	 *
	 * @return pegawai pemilik; secara praktik tidak pernah {@code null} untuk baris tersimpan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = false)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		pengajuanTransaksiPegawai = getPengajuanTransaksiPegawai();
		if (pengajuanTransaksiPegawai != null && pengajuanTransaksiPegawai.getPegawai() != null) {
			pegawai = pengajuanTransaksiPegawai.getPegawai();
		}
		return pegawai;
	}

	/**
	 * Menyetel pegawai pemilik.
	 *
	 * <p>Untuk baris turunan pengajuan, nilai ini akan ditimpa lagi pada pembacaan berikutnya.
	 * Hanya baris manual yang benar-benar menghormati setter ini.</p>
	 *
	 * @param pegawai pegawai pemilik transaksi.
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Kategori transaksi — sekaligus <b>sumber pasangan akun jurnal</b>. Kolom
	 * <code>jenis_transaksi_pegawai</code> berstatus <code>nullable = false</code>.
	 *
	 * <p><b>Getter destruktif.</b> Sama polanya dengan {@link #getPegawai()}: bila baris ini
	 * turunan pengajuan, jenis diambil dari rantai
	 * <code>pengajuan → jenisPengajuanTransaksiPegawai → jenisTransaksiPegawai</code> dan
	 * <b>menimpa</b> nilai tersimpan. Mengganti pemetaan pada katalog
	 * {@code JenisPengajuanTransaksiPegawai} karena itu <b>memindahkan akun jurnal angsuran yang
	 * sudah terbit</b> — dan karena angsuran yang sudah diposting tidak dijurnal ulang, daftar
	 * layar bisa menampilkan pasangan akun yang berbeda dari yang benar-benar tercatat di buku
	 * besar.</p>
	 * <p>Object yang dikembalikan dipakai posting untuk mengambil <b>debet</b>
	 * ({@code getAkunDebet()}) dan <b>kredit</b> ({@code getAkun()}). Keduanya nullable di
	 * katalog; bila salah satu kosong, dokumen dilewati tanpa dijurnal dan tanpa pesan kesalahan
	 * (layar hanya menulis "Transaksi tidak valid").</p>
	 *
	 * @return kategori transaksi; secara praktik tidak pernah {@code null} untuk baris tersimpan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_transaksi_pegawai", nullable = false)
	public JenisTransaksiPegawai getJenisTransaksiPegawai() {
		jenisTransaksiPegawai = check(jenisTransaksiPegawai);
		pengajuanTransaksiPegawai = getPengajuanTransaksiPegawai();
		if (pengajuanTransaksiPegawai != null && pengajuanTransaksiPegawai.getJenisPengajuanTransaksiPegawai() != null
				&& pengajuanTransaksiPegawai.getJenisPengajuanTransaksiPegawai().getJenisTransaksiPegawai() != null) {
			jenisTransaksiPegawai = pengajuanTransaksiPegawai.getJenisPengajuanTransaksiPegawai()
					.getJenisTransaksiPegawai();
		}
		return jenisTransaksiPegawai;
	}

	/**
	 * Menyetel kategori transaksi.
	 *
	 * <p>Untuk baris turunan pengajuan, nilai ini akan ditimpa lagi pada pembacaan berikutnya.</p>
	 *
	 * @param jenisTransaksiPegawai kategori transaksi.
	 */
	public void setJenisTransaksiPegawai(JenisTransaksiPegawai jenisTransaksiPegawai) {
		this.jenisTransaksiPegawai = jenisTransaksiPegawai;
	}

	/**
	 * Nominal satu angsuran — angka yang benar-benar dipotong dari gaji dan dijurnal.
	 *
	 * <p><b>Getter destruktif yang menyentuh nominal — yang paling berdampak di kelas ini.</b>
	 * Perilakunya:</p>
	 * <ol>
	 *   <li>{@code null} dinormalkan menjadi {@code 0.0} (dan tersimpan sebagai 0.0);</li>
	 *   <li>bila baris ini turunan pengajuan, nilai <b>selalu dihitung ulang</b> sebagai
	 *       <code>pengajuan.getNilaiTransaksi() / pengajuan.getJumlahAngsur()</code> lalu ditulis
	 *       balik. Perhatikan syaratnya hanya "pengajuan tidak null" — tidak seperti
	 *       {@link #getKeterangan()}/{@link #getPegawai()} yang juga menuntut pegawai terisi.</li>
	 * </ol>
	 * <p><b>Kasus tepi:</b></p>
	 * <ul>
	 *   <li>tidak ada risiko NPE: <code>getNilaiTransaksi()</code> mengganti {@code null} dengan
	 *       {@code 0.0} dan <code>getJumlahAngsur()</code> mengganti {@code null} dengan
	 *       {@code 1};</li>
	 *   <li>tetapi <code>jumlahAngsur = 0</code> yang <b>tersimpan eksplisit</b> tidak dijaga:
	 *       pembagian floating-point menghasilkan {@code Infinity} (atau {@code NaN} bila
	 *       nominalnya juga 0), dan nilai itu ditulis balik ke kolom nominal serta ikut dijurnal.
	 *       Pembangkit angsuran tidak akan membuat baris untuk pengajuan ber-<code>jumlahAngsur
	 *       = 0</code>, jadi kasus ini muncul lewat <i>pengeditan</i> pengajuan yang angsurannya
	 *       sudah terbit;</li>
	 *   <li>pembagiannya <b>tidak dibulatkan</b>, sehingga sisa pembagian yang tidak habis tersebar
	 *       sebagai pecahan di setiap angsuran, bukan diselesaikan di angsuran terakhir;</li>
	 *   <li>nilai <b>negatif</b> bermakna arah jurnal dibalik — <code>PostingTransaksiPegawaiAction</code>
	 *       meneruskan <code>getNilai() &lt; 0.0</code> sebagai parameter
	 *       <code>apakahUangMasuk</code>, dan mesin posting memakai <code>Math.abs()</code> untuk
	 *       nominalnya.</li>
	 * </ul>
	 * <p><b>Konsekuensi integritas:</b> koreksi nominal manual pada baris turunan pengajuan tidak
	 * pernah bertahan, dan mengubah <code>nilaiTransaksi</code>/<code>jumlahAngsur</code> pada
	 * pengajuan mengubah nominal angsuran yang <b>sudah diposting</b> — tanpa menyentuh jurnalnya.
	 * Baris manual (tanpa pengajuan) hanya terkena normalisasi {@code null} → {@code 0.0}.</p>
	 *
	 * @return nominal angsuran; tidak pernah {@code null}, tetapi bisa {@code Infinity}/{@code NaN}
	 *         pada kasus tepi di atas.
	 */
	public Double getNilai() {
		if (nilai == null) {
			nilai = 0.0;
		}

		pengajuanTransaksiPegawai = getPengajuanTransaksiPegawai();
		if (pengajuanTransaksiPegawai != null) {
			nilai = pengajuanTransaksiPegawai.getNilaiTransaksi()
					/ pengajuanTransaksiPegawai.getJumlahAngsur().doubleValue();
		}

		return nilai;
	}

	/**
	 * Menyetel nominal angsuran.
	 *
	 * <p>Untuk baris turunan pengajuan, nilai ini akan ditimpa lagi pada pembacaan berikutnya —
	 * lihat peringatan pada {@link #getNilai()}.</p>
	 *
	 * @param nilai nominal angsuran.
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Tanggal jatuh tempo angsuran ini — dasar seluruh penyaringan periode di layar, laporan,
	 * kriteria pelunasan, dan tanggal transaksi jurnal.
	 *
	 * <p><b>Getter destruktif.</b> Perilakunya:</p>
	 * <ol>
	 *   <li>{@code null} disubstitusi dengan waktu sekarang (dan tersimpan);</li>
	 *   <li>bila baris ini turunan pengajuan <b>dan</b> {@link #getKe()} tidak {@code null} —
	 *       yang selalu benar, karena {@code getKe()} mengganti {@code null} dengan {@code 1} —
	 *       tanggal dihitung ulang sebagai <code>pengajuan.getTanggalJatuhTempo()</code> digeser
	 *       maju <code>(ke - 1)</code> bulan, lalu ditulis balik. Jadi cabang ini <b>selalu
	 *       aktif</b> untuk setiap baris turunan pengajuan.</li>
	 * </ol>
	 * <p><b>Kasus tepi:</b> penggeseran memakai
	 * <code>calendar.set(MONTH, get(MONTH) + (ke - 1))</code> pada {@code Calendar} lenient, jadi
	 * bulan di atas 11 berguling ke tahun berikutnya sebagaimana diharapkan. Namun hari tanggalnya
	 * tidak dinormalkan: jatuh tempo tanggal 31 akan bergulir ke bulan berikutnya ketika bulan
	 * sasaran lebih pendek (31 Januari + 1 bulan → 3 Maret). Angsuran karena itu bisa jatuh di
	 * bulan yang salah, yang berpengaruh langsung ke {@link #getBln()}/{@link #getThn()} dan
	 * karenanya ke bulan pemotongan gaji.</p>
	 * <p><code>pengajuan.getTanggalJatuhTempo()</code> tidak pernah mengembalikan {@code null}
	 * (disubstitusi hari ini), sehingga tidak ada risiko NPE di sini.</p>
	 *
	 * @return tanggal jatuh tempo angsuran; tidak pernah {@code null}.
	 */
	public Date getTanggal() {
		if (tanggal == null) {
			tanggal = ais.ui.util.WaktuUtil.getDate();
		}

		pengajuanTransaksiPegawai = getPengajuanTransaksiPegawai();
		if (pengajuanTransaksiPegawai != null && getKe() != null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(pengajuanTransaksiPegawai.getTanggalJatuhTempo());
			calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + (getKe() - 1));
			tanggal = calendar.getTime();
		}

		return tanggal;
	}

	/**
	 * Menyetel tanggal jatuh tempo.
	 *
	 * <p>Untuk baris turunan pengajuan, nilai ini <b>selalu</b> ditimpa lagi pada pembacaan
	 * berikutnya — lihat {@link #getTanggal()}.</p>
	 *
	 * @param tanggal tanggal jatuh tempo.
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Hari (tanggal dalam bulan) dari {@link #getTanggal()}, disimpan sebagai kolom tersendiri.
	 *
	 * <p><b>Getter destruktif.</b> Nilainya <b>selalu</b> dihitung ulang dari
	 * {@link #getTanggal()} — nilai tersimpan tidak pernah dipakai — lalu ditulis balik. Karena
	 * ikut memanggil {@code getTanggal()}, method ini juga memicu seluruh rantai penurunan tanggal
	 * beserta efek sampingnya.</p>
	 * <p>Berbeda dari {@link #getBln()}/{@link #getThn()}, kolom ini tidak dipakai kriteria query
	 * mana pun yang ditemukan; ia hanya redundansi tampilan/laporan.</p>
	 *
	 * @return hari dalam bulan (1-31).
	 */
	public Integer getTgl() {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(getTanggal());
		tgl = calendar.get(Calendar.DATE);
		return tgl;
	}

	/**
	 * Menyetel cerminan hari. Praktis tanpa efek: {@link #getTgl()} selalu menghitung ulang.
	 *
	 * @param tgl hari dalam bulan.
	 */
	public void setTgl(Integer tgl) {
		this.tgl = tgl;
	}

	/**
	 * Bulan (1-12) dari {@link #getTanggal()}, disimpan sebagai kolom tersendiri.
	 *
	 * <p><b>Getter destruktif</b> dengan pola sama seperti {@link #getTgl()}: selalu dihitung ulang
	 * dan ditulis balik. Perhatikan konversi {@code Calendar.MONTH + 1} — kolom ini berbasis 1,
	 * bukan 0.</p>
	 * <p><b>Kolom ini penting</b> karena dibaca langsung oleh dua query yang menentukan uang:</p>
	 * <ul>
	 *   <li><code>PembayaranItemGajiPegawaiTreeModel.setLunas()</code> — memilih baris yang akan
	 *       distempel ke slip gaji dengan
	 *       <code>thn &lt; tahun OR (thn = tahun AND bln &lt;= bulan)</code>;</li>
	 *   <li><code>ItemGajiPegawaiTreeModel.hitungItemGajiPegawai()</code> — menjumlahkan nominal
	 *       yang disuntikkan ke rumus gaji.</li>
	 * </ul>
	 * <p><b>Jebakan:</b> query-query itu membaca <b>kolom di basis data</b>, bukan hasil getter
	 * ini. Kolom hanya tersinkron ketika baris dibaca lewat getter di dalam sesi yang kemudian
	 * di-flush. Baris yang kolom <code>bln</code>/<code>thn</code>-nya masih {@code null} tidak
	 * cocok dengan cabang mana pun pada kriteria pelunasan maupun penjumlahan — potongannya
	 * <b>tidak pernah dipotongkan dan tidak pernah muncul lagi</b>, tanpa pesan kesalahan.</p>
	 *
	 * @return bulan berbasis 1 (Januari = 1).
	 */
	public Integer getBln() {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(getTanggal());
		bln = calendar.get(Calendar.MONTH) + 1;
		return bln;
	}

	/**
	 * Menyetel cerminan bulan. Praktis tanpa efek: {@link #getBln()} selalu menghitung ulang.
	 *
	 * @param bln bulan berbasis 1.
	 */
	public void setBln(Integer bln) {
		this.bln = bln;
	}

	/**
	 * Tahun dari {@link #getTanggal()}, disimpan sebagai kolom tersendiri.
	 *
	 * <p><b>Getter destruktif</b> dengan pola sama seperti {@link #getBln()}; dipakai oleh kriteria
	 * yang sama dan menanggung jebakan sinkronisasi kolom yang sama.</p>
	 *
	 * @return tahun empat digit.
	 */
	public Integer getThn() {
		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(getTanggal());
		thn = calendar.get(Calendar.YEAR);
		return thn;
	}

	/**
	 * Menyetel cerminan tahun. Praktis tanpa efek: {@link #getThn()} selalu menghitung ulang.
	 *
	 * @param thn tahun empat digit.
	 */
	public void setThn(Integer thn) {
		this.thn = thn;
	}

	/**
	 * Slip gaji yang melunasi (memotongkan) baris ini.
	 *
	 * <p>Getter murni — tidak ada penurunan maupun tulis-balik. {@code null} berarti baris belum
	 * pernah dipotongkan ke slip mana pun; itulah tanda yang dicari
	 * <code>PembayaranItemGajiPegawaiTreeModel.setLunas()</code> sebelum menstempel kolom ini
	 * secara massal untuk seluruh baris pegawai bersangkutan yang periodenya sudah lewat.</p>
	 * <p>Relasi ini juga yang menjelaskan mengapa tabrakan kunci jurnal historis (bagian 3
	 * dokumentasi kelas) menyangkut {@link PembayaranGajiPunyaPegawai}: kedua dokumen memang
	 * bersanding di modul yang sama, tetapi keduanya adalah <b>dokumen jurnal yang berbeda</b>
	 * dengan ruang id masing-masing.</p>
	 * <p><b>Catatan:</b> kolom ini <b>tidak</b> ikut menjadi syarat penjumlahan pada rumus gaji —
	 * <code>ItemGajiPegawaiTreeModel</code> menjumlahkan baris tanpa memeriksa apakah baris itu
	 * sudah pernah dilunasi.</p>
	 *
	 * @return slip gaji pelunas, atau {@code null} bila belum dipotongkan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pembayaran_gaji_punya_pegawai", nullable = true)
	public PembayaranGajiPunyaPegawai getPembayaranGajiPunyaPegawai() {
		return pembayaranGajiPunyaPegawai;
	}

	/**
	 * Menyetel slip gaji pelunas.
	 *
	 * <p>Satu-satunya pemanggil di luar Hibernate adalah
	 * <code>PembayaranItemGajiPegawaiTreeModel.setLunas(Date)</code>, yang memanggilnya di dalam
	 * loop untuk seluruh baris yang periodenya sudah jatuh tempo.</p>
	 *
	 * @param pembayaranGajiPunyaPegawai slip gaji pelunas, atau {@code null} untuk melepas.
	 */
	public void setPembayaranGajiPunyaPegawai(PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai) {
		this.pembayaranGajiPunyaPegawai = pembayaranGajiPunyaPegawai;
	}

	/**
	 * Cap posting jurnal untuk baris ini.
	 *
	 * <p>Getter murni. {@code null} berarti <b>belum diposting</b> — dan itu bukan sekadar
	 * informasi: seluruh kelayakan posting entity ini bersandar pada kolom ini. Kriteria posting
	 * massal memilih <code>postingHistory is null</code>, kriteria pembatalan memilih
	 * <code>postingHistory is not null</code>, dan renderer baris memakainya untuk memilih tombol
	 * mana yang tampil.</p>
	 * <p>Cap yang dipasang berjenis <code>PostingHistory.JENIS_TRANSAKSI_LAIN</code> dan
	 * <b>dibagi</b> oleh seluruh dokumen dalam satu batch posting.</p>
	 * <p><b>Kuirk pembatalan:</b> melepas cap ini lewat layar ZK hanya disertai penghapusan header
	 * <code>akunting.grup_transaksi</code> yang <code>closing is null</code>; baris anak
	 * <code>akunting.transaksi</code> tidak ikut dihapus sehingga tertinggal sebagai baris yatim
	 * yang tetap terbaca laporan buku besar. Jalur API statis
	 * <code>PostingTransaksiPegawaiAction.batalkanPostingSemua()</code> menghapus anak lebih
	 * dulu dan karena itu bersih.</p>
	 *
	 * @return cap posting, atau {@code null} bila belum diposting.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * Menyetel (atau melepas) cap posting.
	 *
	 * <p>Dipanggil mesin posting setelah jurnal berhasil ditulis, dan dipanggil dengan
	 * {@code null} oleh kedua jalur pembatalan.</p>
	 *
	 * @param postingHistory cap posting, atau {@code null} untuk menandai belum diposting.
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

	/**
	 * Dokumen pengajuan peminjaman yang menjadi asal baris ini, bila ada.
	 *
	 * <p>Getter murni — tidak ada penurunan nilai dari relasi ini. Berbeda dari
	 * {@link #getPengajuanTransaksiPegawai()}, relasi ini <b>tidak</b> menyuplai pegawai, jenis,
	 * nominal, maupun tanggal.</p>
	 * <p><b>Relasi ini TIDUR/YATIM.</b> Penyisiran seluruh repo menemukan nol pemanggil
	 * {@link #setPengajuanPeminjaman(PengajuanPeminjaman)} dan nol query yang menyaring kolom ini
	 * di luar berkas entity sendiri. Kolom <code>pengajuan_peminjaman</code> karena itu <b>selalu
	 * NULL</b> pada praktiknya. {@link PengajuanPeminjaman} sendiri adalah dokumen pengajuan
	 * pinjaman pegawai yang hidup dan lengkap berbasis disposisi SOP — yang tidak ada adalah
	 * jembatannya: tidak ada padanan
	 * <code>populateTransaksi()</code> yang mengubah pinjaman yang disetujui menjadi angsuran
	 * potong gaji. Satu-satunya pipa angsuran yang benar-benar bekerja adalah lewat
	 * {@link PengajuanTransaksiPegawai}. Jadi jangan menyimpulkan dari nama kolom ini bahwa modul
	 * peminjaman sudah terhubung ke penggajian — belum.</p>
	 *
	 * @return pengajuan peminjaman asal; pada praktiknya selalu {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pengajuan_peminjaman", nullable = true)
	public PengajuanPeminjaman getPengajuanPeminjaman() {
		return pengajuanPeminjaman;
	}

	/**
	 * Menyetel dokumen pengajuan peminjaman asal.
	 *
	 * <p>Tidak dipanggil di mana pun selain Hibernate — lihat {@link #getPengajuanPeminjaman()}.</p>
	 *
	 * @param pengajuanPeminjaman pengajuan peminjaman asal.
	 */
	public void setPengajuanPeminjaman(PengajuanPeminjaman pengajuanPeminjaman) {
		this.pengajuanPeminjaman = pengajuanPeminjaman;
	}

	/**
	 * Dokumen pengajuan transaksi pegawai yang melahirkan baris ini — <b>sumber seluruh nilai
	 * turunan kelas ini</b>.
	 *
	 * <p>Getter murni (tidak ada tulis-balik di sini sendiri), tetapi ia adalah pusat gravitasi
	 * kelas: {@link #getPegawai()}, {@link #getJenisTransaksiPegawai()}, {@link #getNilai()},
	 * {@link #getTanggal()}, dan {@link #getKeterangan()} semuanya memanggilnya lebih dulu, lalu
	 * menurunkan nilainya dari sini. Bila hasilnya {@code null} — baris manual — kelima getter itu
	 * berperilaku seperti getter biasa.</p>
	 * <p><b>Perhatian daur hidup:</b> baris turunan dibuat ulang dari nol setiap kali pengajuannya
	 * disimpan. <code>PengajuanTransaksiPegawaiAction.populateTransaksi()</code> menjalankan
	 * <code>delete from payroll.transaksi_pegawai where pengajuan_transaksi_pegawai = &hellip;</code>
	 * lebih dulu — tanpa memandang apakah baris itu sudah diposting ke buku besar atau sudah
	 * dipotongkan ke slip gaji — lalu membangkitkan kembali sebanyak <code>jumlahAngsur</code>
	 * baris dengan id yang baru. Jurnal lama tetap tinggal di
	 * <code>akunting.grup_transaksi</code> sambil menunjuk id yang sudah tidak ada, sedangkan
	 * baris pengganti berstatus "belum diposting" sehingga bisa diposting lagi.</p>
	 * <p>Pemicunya dua, dan keduanya ringan: penyimpanan biasa layar pengajuan, dan
	 * <b>satu klik pada checkbox "Setujui"</b> di baris daftar. Mencabut centang itu menjalankan
	 * penghapusan lalu membangkitkan nol baris — seluruh jadwal angsuran, termasuk yang sudah
	 * dipotongkan dan dijurnal, lenyap dari tabel. Bendera <code>aktif</code> pengajuan (yang
	 * menjadi {@code false} ketika disposisinya ditolak) tidak pernah diperiksa di sini, sehingga
	 * pengajuan yang sudah ditolak pun tetap membangkitkan ulang angsurannya selama
	 * <code>setujui</code>-nya masih benar.</p>
	 *
	 * @return pengajuan induk, atau {@code null} untuk baris yang dientri manual.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pengajuan_transaksi_pegawai", nullable = true)
	public PengajuanTransaksiPegawai getPengajuanTransaksiPegawai() {
		return pengajuanTransaksiPegawai;
	}

	/**
	 * Menyetel dokumen pengajuan induk.
	 *
	 * <p>Menyetel relasi ini <b>mengaktifkan seluruh mesin penurunan nilai</b> kelas ini: sejak
	 * saat itu pegawai, jenis, nominal, tanggal, dan keterangan baris tidak lagi ditentukan oleh
	 * nilai tersimpannya sendiri. Inilah satu-satunya properti (bersama {@link #setKe(Integer)})
	 * yang diisi pembangkit angsuran.</p>
	 *
	 * @param pengajuanTransaksiPegawai pengajuan induk.
	 */
	public void setPengajuanTransaksiPegawai(PengajuanTransaksiPegawai pengajuanTransaksiPegawai) {
		this.pengajuanTransaksiPegawai = pengajuanTransaksiPegawai;
	}

	/**
	 * Nomor urut angsuran, dihitung mulai dari 1.
	 *
	 * <p>Getter dengan substitusi bawaan (bukan destruktif — field tidak ditulis balik):
	 * {@code null} dilaporkan sebagai {@code 1}. Karena itu pemeriksaan
	 * <code>getKe() != null</code> pada {@link #getTanggal()} <b>selalu benar</b>, dan baris manual
	 * yang tidak pernah mengisi kolom ini tetap diperlakukan sebagai "angsuran ke-1".</p>
	 * <p>Nilainya dipakai dua tempat: menggeser tanggal jatuh tempo sebanyak
	 * <code>(ke - 1)</code> bulan pada {@link #getTanggal()}, dan menyusun teks
	 * "angsuran ke <i>N</i>" pada {@link #getKeterangan()}.</p>
	 *
	 * @return nomor angsuran; tidak pernah {@code null}, minimal {@code 1}.
	 */
	public Integer getKe() {
		return ke == null ? 1 : ke;
	}

	/**
	 * Menyetel nomor urut angsuran.
	 *
	 * <p>Diisi pembangkit angsuran dengan {@code 1..jumlahAngsur}. Mengubahnya menggeser tanggal
	 * jatuh tempo baris ini sebanyak <code>(ke - 1)</code> bulan dari jatuh tempo pengajuan, dan
	 * karenanya menggeser bulan pemotongan gajinya.</p>
	 *
	 * @param ke nomor angsuran; {@code null} diperlakukan sebagai {@code 1} saat dibaca.
	 */
	public void setKe(Integer ke) {
		this.ke = ke;
	}

}
