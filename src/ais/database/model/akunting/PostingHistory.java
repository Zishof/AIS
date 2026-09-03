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

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;

/**
 * <b>Cap posting</b> (<i>posting stamp</i>) — satu baris tabel
 * <code>akunting.posting_history</code> yang mewakili <b>satu kali operasi posting</b>:
 * siapa yang memposting, kapan, jenis dokumen apa, dan keterangan bebas.
 *
 * <h2>Peran dalam mesin posting jurnal</h2>
 * <p>
 * Entity ini adalah <b>penanda</b>, bukan pembawa angka. Ia tidak menyimpan nominal, akun
 * lawan, maupun daftar dokumen yang diposting. Fungsinya semata menjadi <i>token</i> yang
 * ditempelkan ke semua artefak yang lahir dari satu operasi posting, sehingga sesudahnya
 * sistem dapat menjawab tiga pertanyaan: (1) apakah dokumen X sudah diposting, (2) siapa
 * dan kapan yang memposting, dan (3) baris jurnal mana saja yang lahir bersamaan dalam
 * satu batch yang sama.
 * </p>
 * <p>
 * Pola pemakaiannya seragam di seluruh <code>ais/action/**</code> — puluhan kelas
 * <code>Posting*Action</code> (jurnal umum, kas kecil, kas besar, uang muka,
 * pertanggungjawaban, pajak, pengadaan/aset, penggajian, koperasi/kantin, pembayaran
 * siswa/mahasiswa, deposit, dan seterusnya) menjalankan urutan yang sama saat tombol
 * <i>"Posting Semua"</i> ditekan:
 * </p>
 * <ol>
 *   <li>membuat <b>satu</b> instance {@code new PostingHistory(JENIS_...)};</li>
 *   <li>mengisi {@link #setTbmuser(Tbmuser) tbmuser} (pelaku), {@link #setTanggal(Date)
 *       tanggal}, sering juga {@link #setTanggalPosting(Date) tanggalPosting} dan
 *       {@link #setKeterangan(String) keterangan};</li>
 *   <li>menyimpannya lebih dulu — biasanya <code>session.save(postingHistory)</code> di
 *       dalam transaksi <i>terpisah</i> yang di-<i>commit</i> sendiri, supaya id-nya sudah
 *       terbit sebelum ribuan baris jurnal ditulis;</li>
 *   <li>mengiterasi dokumen sumber yang belum diposting
 *       (<code>Restrictions.isNull("postingHistory")</code>) dan memanggil
 *       <code>CommonAkunting.saveTransaksi(..., postingHistory, ...)</code> untuk
 *       masing-masing; helper itulah yang menempelkan cap ini ke
 *       {@link ais.database.model.akunting.GrupTransaksi} (header jurnal) <i>dan</i> ke
 *       setiap {@link ais.database.model.akunting.Transaksi} (baris debet/kredit) yang
 *       dihasilkan;</li>
 *   <li>terakhir memanggil <code>dokumen.setPostingHistory(postingHistory)</code> pada
 *       dokumen sumbernya sendiri, sebagai penanda "dokumen ini sudah dijurnal".</li>
 * </ol>
 * <p>
 * Akibatnya <b>satu baris {@code PostingHistory} lazim dipakai bersama oleh puluhan sampai
 * ribuan baris</b>: N dokumen sumber + N header jurnal + 2N (atau lebih) baris jurnal.
 * Relasi banyak-ke-satu itu hanya dipetakan dari sisi <i>anak</i>; kelas ini sengaja tidak
 * memiliki koleksi balik, sehingga cakupan sebuah batch hanya bisa ditemukan dengan
 * mengueri anak-anaknya (<code>Restrictions.eq("postingHistory", riwayat)</code>).
 * </p>
 *
 * <h2>Relasi terverifikasi ke {@link ais.database.model.akunting.GrupTransaksi}</h2>
 * <p>
 * {@code GrupTransaksi} memiliki properti {@code postingHistory} yang dipetakan
 * {@code @ManyToOne} ke kolom <code>grup_transaksi.posting_history</code> (nullable).
 * Semantiknya: <b>{@code null} = jurnal masih draft</b>, non-null = jurnal sudah diposting.
 * Kolom {@code grup_transaksi.jenis} adalah <i>cerminan</i> dari {@link #getJenis()} —
 * {@code GrupTransaksi.getJenis()} menimpa field-nya dengan {@code postingHistory.getJenis()}
 * setiap kali dibaca, sehingga nilai {@link #JENIS_UMUM} dan kerabatnya di kelas ini ikut
 * menentukan label jenis jurnal. Dokumen sumber pun memakai nama kolom yang sama
 * (<code>posting_history</code>) masing-masing; sebuah dokumen bahkan bisa memiliki
 * <b>lebih dari satu</b> cap yang independen — misalnya
 * {@code Pertangungjawaban} menyimpan cap utama di {@code posting_history} dan cap setoran
 * pajaknya di kolom terpisah {@code posting_history_pajak}, sehingga jurnal biaya dan jurnal
 * pajak dokumen yang sama dapat diposting/dibatalkan sendiri-sendiri.
 * </p>
 *
 * <h2>Makna kolom {@code posting} — persetujuan batch, bukan penanda "sudah dijurnal"</h2>
 * <p>
 * Kolom {@link #getPosting() posting} bertipe {@code Boolean} <b>nullable</b> dan sering
 * disalahpahami. Ia <i>bukan</i> penanda "dokumen sudah diposting" — peran itu dipegang oleh
 * keberadaan cap ini pada dokumen. {@code posting} adalah <b>bendera pengakuan batch ke buku
 * besar</b> pada instalasi yang menjalankan posting dua langkah:
 * </p>
 * <ul>
 *   <li>bila konfigurasi <code>otomatis_terposting</code> <b>aktif</b>
 *       ({@code ConstantValues.otomatisTerposting == true}, nilai <i>default</i> hardcoded
 *       dan yang lazim di lapangan), laporan buku besar/neraca lajur/laporan keuangan
 *       <b>mengabaikan</b> kolom ini sama sekali — cukup {@code posting_history is not null}
 *       untuk masuk hitungan;</li>
 *   <li>bila konfigurasi itu <b>dimatikan</b>, laporan menambahkan syarat
 *       <code>ph.posting = true</code> — jurnal yang capnya belum "diakui" tidak ikut
 *       membentuk saldo, dan layar {@code GrupTransaksiAction} menampilkan
 *       <i>checkbox</i> "Posting" untuk mengakui/mencabutnya.</li>
 * </ul>
 * <p>
 * {@link #getPosting()} karena itu mengembalikan {@code ConstantValues.otomatisTerposting}
 * ketika kolomnya {@code null} ("bila mode otomatis, anggap sudah diakui"). <b>Non-obvious
 * dan penting:</b> nilai <i>default</i> tersebut hanya berlaku di sisi Java. Sebagian besar
 * pembuat cap tidak pernah memanggil {@link #setPosting(Boolean)}, sehingga kolomnya
 * tertulis {@code NULL} di basis data, sedangkan puluhan layar posting menyaring dengan
 * <code>Restrictions.eq("postingHistory.posting", true)</code> (tab "sudah diposting") dan
 * <code>postingHistory.id is null or postingHistory.posting = false</code> (tab "belum
 * diposting"). Baris ber-{@code posting} {@code NULL} tidak cocok dengan <i>kedua</i>
 * filter itu dan menghilang dari dua-duanya. Hanya helper
 * <code>PostingJurnalHelper.terapkanStatusPostingHistory</code> yang menangani {@code NULL}
 * dengan benar (<code>posting = true or posting is null</code>) — nilai itulah yang setara
 * dengan semantik getter di kelas ini.
 * </p>
 * <p>
 * Perhatikan pula bahwa {@link ais.database.model.akunting.Transaksi#getStatusPosting()}
 * hanya memeriksa <i>keberadaan</i> cap, bukan bendera ini: baris jurnal yang batch-nya
 * sudah dicabut ({@code posting = false}) tetap dilaporkan "selesai diposting" oleh baris
 * jurnal itu sendiri, meski laporan buku besar sudah tidak menghitungnya.
 * </p>
 *
 * <h2>Pembatalan posting</h2>
 * <p>
 * Ada dua jalur yang berbeda tajam. Jalur baru berbasis dasbor/API (lihat
 * <code>PostingTransaksiHarianAction</code>) melakukan urutan lengkap: melepas cap pada
 * header jurnal, melepas cap + mengembalikan {@code statusPosting}/{@code tanggalPosting}
 * pada seluruh baris jurnal anak, lalu <b>menghapus baris {@code PostingHistory} ini</b>
 * hanya bila tidak ada lagi header maupun baris yang menunjuknya (pengumpulan sampah
 * berbasis hitungan referensi). Jalur ZK lama pada beberapa modul lebih pendek — ia
 * cukup menyetel <code>dokumen.setPostingHistory(null)</code> sehingga dokumen kembali
 * tampak "belum diposting" sementara header/baris jurnalnya bisa tertinggal di buku besar.
 * Konsekuensinya sudah dilacak terpisah sebagai temuan batal-posting tidak lengkap.
 * </p>
 *
 * <h2>Jejak audit dan pewarisan</h2>
 * <p>
 * Kelas dianotasi {@link org.hibernate.envers.Audited}, sehingga setiap versi baris digandakan
 * ke tabel revisi <code>akunting.posting_history_aud</code>; layar "Sejarah Posting" menautkan
 * setiap baris ke penampil revisi lewat {@code RevisiHelper}. Superclass
 * {@link ais.database.model.GeneralValueObject} adalah <b>POJO abstrak biasa</b> — bukan
 * {@code @Entity} maupun {@code @MappedSuperclass} — sehingga Hibernate tidak memetakan
 * properti induknya. Karena itu {@code id}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} <b>dideklarasikan ulang di kelas ini sebagai keharusan teknis,
 * bukan duplikasi yang keliru</b>.
 * </p>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ol>
 *   <li><b>Katalog jenis</b> — 47 konstanta {@code JENIS_*} (plus
 *       {@link #PENGAJUAN_TRANSITORI}) yang mengisi {@link #getJenis()}; masing-masing
 *       modul memilih satu saat membuat cap.</li>
 *   <li><b>Identitas</b> — {@link #getId()}.</li>
 *   <li><b>Pelaku dan waktu</b> — {@link #getTbmuser()} (pelaku posting, wajib),
 *       {@link #getTanggal()} (waktu operasi posting), {@link #getTanggalPosting()}
 *       (tanggal efektif posting, sering sama).</li>
 *   <li><b>Deskripsi</b> — {@link #getJenis()}, {@link #getNama()} (label turunan),
 *       {@link #getKeterangan()}, {@link #toString()}.</li>
 *   <li><b>Status batch</b> — {@link #getPosting()}.</li>
 *   <li><b>Metadata audit</b> — {@link #getOleh()}, {@link #getOlehId()},
 *       {@link #getTanggal_dirubah()}, {@code onUpdate()}.</li>
 *   <li><b>Relasi tak terpakai</b> — {@link #getAkun()} (lihat catatan pada method).</li>
 * </ol>
 *
 * <h2>Catatan untuk pembaca berikutnya</h2>
 * <ul>
 *   <li>Entity ini <b>tidak memiliki kolom sekolah/yayasan</b>. Katalog cap posting bersifat
 *       global untuk seluruh instalasi, dan layar "Sejarah Posting"
 *       ({@code PostingHistoryAction}) menyaring hanya berdasarkan rentang tanggal dan nama —
 *       tanpa pembatas tenant apa pun. Nama pegawai pelaku posting serta keterangan batch
 *       (yang kerap memuat rentang tanggal dan nama modul) karena itu terlihat lintas tenant.</li>
 *   <li>Layar detail memuat <i>iframe</i>
 *       <code>posting_transaksi_harian.zul?postingHistory=&lt;id&gt;</code>; layar tujuan
 *       me-resolve id itu langsung dari parameter URL tanpa pemeriksaan kepemilikan —
 *       varian pola IDOR parameter URL yang sudah dilacak secara sistemik.</li>
 *   <li>{@link #getNama()} adalah <b>getter dengan write-back</b>: ia menghitung ulang dan
 *       menimpa kolom <code>nama</code> setiap kali dibaca. Label batch karena itu bukan
 *       potret historis.</li>
 *   <li>Jangan memakai kelas ini sebagai "log posting" yang bisa dihapus bebas: menghapus
 *       satu baris berarti seluruh dokumen dan jurnal yang mencapnya kehilangan status
 *       posting sekaligus.</li>
 * </ul>
 *
 * @see ais.database.model.akunting.GrupTransaksi
 * @see ais.database.model.akunting.Transaksi
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.Tbmuser
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "akunting", name = "posting_history")
public class PostingHistory extends GeneralValueObject {

	/** Jenis cap untuk posting jurnal umum/harian dari dasbor draft jurnal. */
	public static final String JENIS_UMUM = "Umum";
	/** Jenis cap untuk transaksi simpan pinjam koperasi. */
	public static final String JENIS_SIMPAN_PINJAM = "Simpan Pinjam Koperasi";
	/** Jenis cap untuk jurnal balik pembatalan penjualan kantin. */
	public static final String JENIS_PEMBATALAN_KANTIN = "Pembatalan Penjualan Kantin";
	/** Jenis cap untuk jurnal penghapusan (disposal) aset tetap. */
	public static final String JENIS_PENGHAPUSAN_ASET = "Penghapusan Aset";
	/** Jenis cap untuk penambahan saldo (topup) dana anggota koperasi. */
	public static final String JENIS_TOPUP_SALDO_ANGGOTA = "Topup Saldo Anggota";
	/** Jenis cap untuk pencairan diskon/insentif anggota koperasi. */
	public static final String JENIS_PENCAIRAN_DISKON = "Pencairan Diskon Anggota";
	/** Jenis cap untuk penyesuaian manual saldo dana anggota koperasi. */
	public static final String JENIS_PENYESUAIAN_SALDO = "Penyesuaian Saldo Anggota";
	/** Jenis cap untuk setoran modal penyertaan anggota koperasi. */
	public static final String JENIS_MODAL_PENYERTAAN = "Modal Penyertaan";
	/** Jenis cap untuk pembagian Sisa Hasil Usaha koperasi. */
	public static final String JENIS_PEMBAGIAN_SHU = "Pembagian SHU";
	/** Jenis cap untuk pengembalian modal penyertaan kepada anggota. */
	public static final String JENIS_PENGEMBALIAN_MODAL = "Pengembalian Modal Penyertaan";
	/** Jenis cap untuk pembebanan biaya satu sesi sales koperasi. */
	public static final String JENIS_BIAYA_SALES = "Biaya Sesi Sales";
	/** Jenis cap untuk pengisian ulang (replenishment) dana kas kecil. */
	public static final String JENIS_PENGGANTIAN_KAS_KECIL = "Penggantian Kas Kecil";
	/** Jenis cap untuk pengeluaran yang dibayar dari kas kecil. */
	public static final String JENIS_PENGGUNAAN_KAS_KECIL = "Penggunaan Kas Kecil";
	/** Jenis cap untuk pengeluaran yang dibayar dari kas besar. */
	public static final String JENIS_PENGGUNAAN_KAS_BESAR = "Penggunaan Kas Besar";
	/** Jenis cap umum untuk transaksi keuangan mahasiswa (perguruan tinggi). */
	public static final String JENIS_MAHASISWA = "Mahasiswa";
	/** Jenis cap untuk pembayaran mahasiswa yang diterima di muka (pendapatan diterima dimuka). */
	public static final String JENIS_MAHASISWA_DIBAYAR_DIMUKA = "Pembayaran Mahasiswa Dibayar Dimuka";
	/** Jenis cap untuk pembayaran/cicilan tagihan siswa (sekolah). */
	public static final String JENIS_PEMBAYARAN_SISWA = "Pembayaran Siswa";
	/** Jenis cap untuk pengakuan piutang tagihan siswa. */
	public static final String JENIS_PIUTANG_SISWA = "Piutang Siswa";
	/** Jenis cap untuk pengakuan piutang denda keterlambatan siswa. */
	public static final String JENIS_PIUTANG_DENDA_SISWA = "Piutang Denda Siswa";
	/** Jenis cap untuk pengakuan utang diskon/keringanan yang diberikan kepada siswa. */
	public static final String JENIS_UTANG_DISKON_SISWA = "Utang Diskon Siswa";
	/** Jenis cap untuk pembayaran siswa yang diterima mendahului tagihannya. */
	public static final String JENIS_PEMBAYARAN_SISWA_DIBAYAR_DIMUKA = "Pembayaran Siswa Dibayar Dimuka";
	/** Jenis cap untuk tagihan siswa yang jatuh pada periode di muka. */
	public static final String JENIS_TAGIHAN_SISWA_DIBAYAR_DIMUKA = "Tagihan Siswa Dibayar Dimuka";
	/** Jenis cap untuk jurnal persetujuan/pencairan uang muka (kasbon) pegawai. */
	public static final String JENIS_PERSETUJUAN_UANG_MUKA = "Persetujun uang Muka";
	/** Jenis cap untuk jurnal daftar pengajuan transfer (antrean pembayaran bank). */
	public static final String JENIS_PENGAJUAN_TRANSFER = "Daftar pengajuan transfer";
	/**
	 * Jenis cap untuk pengajuan transitori (dana titipan/perantara).
	 *
	 * <p>Satu-satunya konstanta jenis di kelas ini yang tidak memakai awalan
	 * {@code JENIS_} — penamaannya menyimpang dari konvensi tetangganya, bukan kategori
	 * yang berbeda.</p>
	 */
	public static final String PENGAJUAN_TRANSITORI = "Pengajuan transitori";
	/** Jenis cap untuk jurnal pertanggungjawaban (LPJ) atas uang muka pegawai. */
	public static final String JENIS_PERTANGGUNGJAWABAN_UANG_MUKA = "Pertanggungjawaban Uang Muka";
	/** Jenis cap untuk penggantian biaya yang ditalangi pegawai (reimbursement). */
	public static final String JENIS_REIMBURSEMENT_PEGAWAI = "Reimbursement Pegawai";
	/** Jenis cap untuk jurnal pertanggungjawaban penggunaan kas besar. */
	public static final String JENIS_PERTANGGUNGJAWABAN_KAS_BESAR = "Pertanggungjawaban Kas Besar";
	/**
	 * Jenis cap untuk jurnal <b>setoran pajak</b> pada pertanggungjawaban.
	 *
	 * <p>Cap berjenis ini dilekatkan ke kolom terpisah
	 * <code>pertangungjawaban.posting_history_pajak</code>, sehingga satu dokumen
	 * pertanggungjawaban dapat memiliki dua cap yang berdiri sendiri: jurnal biaya dan
	 * jurnal pajaknya.</p>
	 */
	public static final String JENIS_PERTANGGUNGJAWABAN_PAJAK = "Pajak";
	/** Jenis cap untuk jurnal pengembalian sisa uang muka yang tidak terpakai. */
	public static final String JENIS_PENGEMBALIAN_UANG_MUKA = "Pengembalian uang muka";
	/** Jenis cap untuk jurnal pembentukan saldo awal dana kas kecil. */
	public static final String JENIS_SALDO_AWAL_KAS_KECIL = "Saldo Awal Kas Kecil";
	/** Jenis cap penerimaan barang/jasa — mengakui utang sementara sebelum tagihan terbit. */
	public static final String JENIS_PENERIMAAN_BARANG_JASA = "Peneriman Barang/Jasa (Hutang Sementra)";
	/** Jenis cap penerimaan tagihan barang/jasa — memindahkan utang sementara ke utang penyedia. */
	public static final String JENIS_PENERIMAAN_TAGIHAN_BARANG_JASA = "Peneriman Tagihan Barang/Jasa (Hutang Penyedia)";
	/** Jenis cap untuk pembayaran uang muka (DP) atas pemesanan pengadaan. */
	public static final String JENIS_PEMBAYARAN_DP_PEMESANAN = "Pembayaran DP Pemesanan";
	/** Jenis cap untuk tagihan pekerjaan/kontrak (progres penuh). */
	public static final String JENIS_TAGIHAN_PEKERJAAN = "Tagihan Pekerjaan";
	/** Jenis cap untuk tagihan uang muka pekerjaan/kontrak. */
	public static final String JENIS_TAGIHAN_DP_PEKERJAAN = "Tagihan DP Pekerjaan";
	/** Jenis cap untuk jurnal balik (pengembalian) uang muka pekerjaan. */
	public static final String JENIS_TAGIHAN_DP_BALIK_PEKERJAAN = "Tagihan DP Balik Pekerjaan";
	/** Jenis cap untuk jurnal perjanjian kerjasama (MoU/kontrak aset). */
	public static final String JENIS_PERJANJIAN_KERJASAMA = "Perjanjian Kerjasama";
	/** Jenis cap untuk pelunasan tagihan penyedia. */
	public static final String JENIS_PEMBAYARAN_TAGIHAN = "Pembayaran Tagihan";
	/** Jenis cap untuk pembayaran bagian uang muka dari sebuah tagihan. */
	public static final String JENIS_PEMBAYARAN_TAGIHAN_DP = "Pembayaran Tagihan DP";
	/** Jenis cap untuk pembayaran tagihan bertahap (termin). */
	public static final String JENIS_PEMBAYARAN_TAGIHAN_TERMIN = "Pembayaran Tagihan Termin";
	/** Jenis cap untuk jurnal beban penyusutan aset tetap periodik. */
	public static final String JENIS_PENYUSUTAN_ASET = "Penyusutan Aset";
	/** Jenis cap untuk penutupan/rekap transaksi kasir. */
	public static final String JENIS_KASIR = "Kasir";
	/** Jenis cap untuk transaksi deposit (titipan dana siswa/mahasiswa/anggota). */
	public static final String JENIS_DEPOSIT = "Deposit";
	/** Jenis cap untuk jurnal penggajian pegawai. */
	public static final String JENIS_PENGGAJIAN = "Penggajian";
	/** Jenis cap untuk jurnal pengadaan barang/jasa. */
	public static final String JENIS_PENGADAAN = "Pengadaan";
	/** Jenis cap penampung untuk transaksi yang tidak masuk kategori mana pun di atas. */
	public static final String JENIS_TRANSAKSI_LAIN = "Transaksi Lain";

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama, kolom <code>id</code>, dibangkitkan basis data (IDENTITY). */
	private Long id;
	/**
	 * Metadata audit: identitas (nama) pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Dideklarasikan ulang dari {@link ais.database.model.GeneralValueObject} karena
	 * superclass tersebut bukan {@code @MappedSuperclass}. Diisi otomatis oleh
	 * <code>AuditTimestampInterceptor.ubah</code> lewat {@code onUpdate()}.</p>
	 */
	private String oleh;
	/**
	 * Metadata audit: id pengguna terakhir yang mengubah baris ini. Lihat catatan pada
	 * {@link #oleh}.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris cap ini (metadata audit).
	 *
	 * <p>Nilainya diisi oleh <code>AuditTimestampInterceptor</code> saat baris
	 * di-<i>update</i>. Karena kelas ini hanya memasang {@code @PreUpdate} (tanpa
	 * {@code @PrePersist}) dan baris cap posting umumnya ditulis sekali lalu tidak pernah
	 * disunting, kolom ini pada praktiknya sering tetap {@code null} — pelaku posting yang
	 * sesungguhnya dibaca dari {@link #getTbmuser()}, bukan dari sini.</p>
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila baris belum pernah
	 *         di-<i>update</i>.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir (metadata audit).
	 *
	 * <p><b>Non-obvious:</b> argumen {@code null} atau berisi spasi saja <b>diabaikan diam-diam</b> —
	 * method langsung {@code return} tanpa mengubah apa pun. Nilai lama karena itu tidak
	 * pernah bisa dikosongkan lewat setter ini.</p>
	 *
	 * @param olehId id pengguna pengubah; nilai kosong/{@code null} tidak berpengaruh.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks cap posting untuk ditampilkan di layar dan label.
	 *
	 * <p>Mendelegasikan sepenuhnya ke {@link #getNama()}, sehingga <b>ikut menanggung efek
	 * samping getter tersebut</b> (menghitung ulang dan menimpa kolom <code>nama</code>)
	 * serta ikut melempar {@code NullPointerException} bila {@link #getTbmuser()} belum
	 * terisi. Dipakai antara lain pada kolom status layar dokumen ("sudah diposting …")
	 * dan pada tautan revisi di layar "Sejarah Posting".</p>
	 *
	 * @return label "<i>tanggal</i> oleh <i>nama pegawai/user</i>".
	 */
	public String toString() {
		return getNama();
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir (metadata audit).
	 *
	 * <p><b>Non-obvious:</b> sama seperti {@link #setOlehId(String)}, argumen {@code null}
	 * atau kosong diabaikan diam-diam.</p>
	 *
	 * @param oleh nama pengguna pengubah; nilai kosong/{@code null} tidak berpengaruh.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris cap ini (metadata audit).
	 *
	 * <p>Lihat {@link #getOlehId()} untuk alasan mengapa kolom ini kerap {@code null} pada
	 * entity ini.</p>
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null}.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * <i>Callback</i> JPA {@code @PreUpdate} yang menstempel metadata audit, sekaligus baris
	 * deklarasi field {@code tanggal_dirubah}.
	 *
	 * <p>Kedua konstruksi sengaja ditulis pada satu baris fisik oleh generator kode repo ini;
	 * jangan dipisah tanpa alasan kuat karena pola yang sama dipakai di seluruh paket model.</p>
	 *
	 * <p>{@code onUpdate()} memanggil <code>AuditTimestampInterceptor.ubah(this)</code> yang
	 * mengisi {@link #setTanggal_dirubah(Date)}, {@link #setOleh(String)}, dan
	 * {@link #setOlehId(String)} dari pengguna aktif — kecuali bila
	 * {@code AuditTrailHelper} menyimpulkan tidak ada perubahan bisnis, yang membuat
	 * penstempelan dilewati. Field {@code tanggal_dirubah} diinisialisasi ke waktu server
	 * saat objek dibuat.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir baris ini.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (kolom <code>tanggal_dirubah</code>,
	 * presisi TIMESTAMP).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang dibuat di
	 *         JVM ini karena field-nya diinisialisasi saat konstruksi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Relasi opsional ke satu {@link Akun} bagan akun.
	 *
	 * <p><b>Tidak terpakai:</b> tidak ada satu pun pemanggil
	 * {@code postingHistory.setAkun(...)} atau {@code postingHistory.getAkun()} di seluruh
	 * repo. Akun debet/kredit jurnal selalu ditentukan oleh dokumen sumber dan diteruskan ke
	 * <code>CommonAkunting.saveTransaksi</code>, bukan diambil dari cap posting.</p>
	 */
	private Akun akun;
	/**
	 * Label batch, kolom <code>nama</code> (wajib, maksimal 255 karakter).
	 * Selalu dihitung ulang oleh {@link #getNama()} — lihat catatan write-back di sana.
	 */
	private String nama;
	/**
	 * Kategori operasi posting; salah satu konstanta {@code JENIS_*} di kelas ini.
	 * Nilainya tercermin kembali ke kolom <code>grup_transaksi.jenis</code>.
	 */
	private String jenis;
	/** Keterangan bebas batch posting (rentang tanggal, asal layar, catatan operator). */
	private String keterangan;
	/** Pengguna pelaku posting; kolom <code>tbmuser</code>, wajib (FK not null). */
	private Tbmuser tbmuser;
	/** Waktu operasi posting dijalankan; diinisialisasi ke waktu server saat objek dibuat. */
	private Date tanggal = ais.ui.util.WaktuUtil.getDate();
	/** Tanggal efektif posting ke buku besar; sering sama dengan {@link #tanggal}. */
	private Date tanggalPosting = ais.ui.util.WaktuUtil.getDate();
	/**
	 * Bendera pengakuan batch ke buku besar; {@code null} berarti "belum pernah disetel"
	 * dan diterjemahkan oleh {@link #getPosting()} menjadi
	 * {@code ConstantValues.otomatisTerposting}. Lihat pembahasan rinci di Javadoc kelas.
	 */
	private Boolean posting;

	/**
	 * Konstruktor tanpa argumen yang dibutuhkan Hibernate untuk membentuk instance dari
	 * baris basis data.
	 *
	 * <p>Field {@link #tanggal} dan {@link #tanggalPosting} tetap terisi waktu server melalui
	 * inisialisasi field, lalu ditimpa nilai dari basis data saat hidrasi.</p>
	 */
	public PostingHistory() {
	}

	/**
	 * Konstruktor yang dipakai seluruh mesin posting: membuat cap baru dengan kategori
	 * tertentu.
	 *
	 * <p>Ini adalah bentuk pemanggilan yang lazim di puluhan {@code Posting*Action}, mis.
	 * {@code new PostingHistory(PostingHistory.JENIS_UMUM)}. Pemanggil masih wajib mengisi
	 * {@link #setTbmuser(Tbmuser)} (kolomnya {@code not null}) sebelum menyimpan, dan
	 * biasanya juga mengisi tanggal serta keterangan.</p>
	 *
	 * @param jenis kategori operasi posting; idealnya salah satu konstanta {@code JENIS_*}
	 *        pada kelas ini. Tidak divalidasi — nilai bebas apa pun akan tersimpan apa
	 *        adanya dan ikut tercermin ke {@code grup_transaksi.jenis}.
	 */
	public PostingHistory(String jenis) {
		this.jenis = jenis;
	}

	/**
	 * Mengembalikan kunci utama baris cap ini.
	 *
	 * <p>Dibangkitkan basis data ({@code IDENTITY}) dan tidak ikut disisipkan
	 * ({@code insertable = false}). Karena itu id baru tersedia setelah
	 * <code>session.save(...)</code> — alasan mengapa mesin posting menyimpan cap lebih dulu
	 * dalam transaksi terpisah sebelum menempelkannya ke ribuan baris jurnal.</p>
	 *
	 * @return id baris, atau {@code null} untuk objek yang belum disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris cap ini.
	 *
	 * <p>Hanya untuk keperluan Hibernate/uji. Kode aplikasi tidak pernah menyetelnya sendiri.</p>
	 *
	 * @param id kunci utama.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan label batch posting, mis. <i>"03-09-2026 oleh Budi Santosa"</i>.
	 *
	 * <p><b>Efek samping (getter dengan write-back):</b> field {@code nama} <b>selalu ditimpa</b>
	 * dengan hasil perhitungan ulang sebelum dikembalikan — gabungan
	 * {@link #getTanggal()} berformat {@code Common.dateFormat3} dan nama pelaku. Nama pelaku
	 * diambil dari pegawai yang tertaut ke {@link #getTbmuser()}
	 * ({@code tbmuser.ambilPegawai().getNama()}), dan jatuh ke {@code tbmuser.getUserId()}
	 * bila pengguna tersebut tidak punya data pegawai. Karena entity ini memakai akses
	 * properti dan {@code dynamicUpdate}, penulisan itu ikut ter-<i>flush</i> ke basis data
	 * pada sesi yang hidup: <b>sekadar membaca cap dapat menerbitkan UPDATE</b> dan, karena
	 * kelas ini {@code @Audited}, menambah revisi baru di
	 * <code>posting_history_aud</code>. Konsekuensi fungsionalnya: label batch bukan potret
	 * historis — mengganti nama pegawai akan mengubah label seluruh cap posting lamanya.
	 * Nilai yang pernah disetel lewat {@link #setNama(String)} juga selalu hilang.</p>
	 *
	 * <p><b>Prasyarat:</b> {@link #getTbmuser()} harus sudah terisi; bila {@code null} method
	 * ini melempar {@code NullPointerException}. Hal ini praktis tidak terjadi pada baris yang
	 * berasal dari basis data karena kolomnya {@code not null}, tetapi bisa terjadi pada
	 * instance baru yang belum dilengkapi.</p>
	 *
	 * <p>Dipanggil dari {@link #toString()}, dari kolom "Nama" layar "Sejarah Posting", dan
	 * dari tautan revisi {@code RevisiHelper}.</p>
	 *
	 * @return label batch yang sudah dipangkas spasi tepi, atau {@code null} bila hasil
	 *         perhitungan tidak tersimpan (praktis tidak terjadi).
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		nama = Common.dateFormat3.get().format(getTanggal()) + " oleh "
				+ (tbmuser.ambilPegawai() == null ? tbmuser.getUserId() : tbmuser.ambilPegawai().getNama());
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel label batch posting.
	 *
	 * <p><b>Tidak berdampak:</b> {@link #getNama()} selalu menghitung ulang dan menimpa field
	 * ini, sehingga nilai yang disetel di sini hilang pada pembacaan berikutnya.</p>
	 *
	 * @param nama label batch.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas batch posting.
	 *
	 * <p>Diisi oleh layar posting dengan kalimat seperti <i>"Posting massal jurnal umum dari
	 * dasbor draft jurnal \nTgl: … s.d …"</i>, atau dengan catatan yang diketik operator.
	 * Dipakai sebagai kolom keterangan pada layar "Sejarah Posting".</p>
	 *
	 * @return keterangan yang sudah dipangkas spasi tepi; string kosong (bukan {@code null})
	 *         bila kolomnya kosong, sehingga aman langsung ditampilkan.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan == null ? "" : this.keterangan.trim();
	}

	/**
	 * Menyetel keterangan bebas batch posting.
	 *
	 * @param keterangan catatan operasi posting; boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan waktu operasi posting dijalankan.
	 *
	 * <p><b>Efek samping ringan (self-healing):</b> bila field-nya {@code null}, method
	 * mengisinya dengan waktu server saat itu juga sebelum mengembalikannya. Perubahan ini
	 * ikut tersimpan pada sesi yang hidup. Penjagaan tersebut ada karena {@link #getNama()}
	 * memformat nilai ini tanpa pemeriksaan {@code null}.</p>
	 *
	 * <p>Kolomnya tidak dianotasi {@code @Temporal}, sehingga dipetakan sebagai TIMESTAMP
	 * (default Hibernate) — berbeda dari {@link #getTanggalPosting()} yang eksplisit
	 * {@code DATE}. Layar "Sejarah Posting" mengurutkan daftar menurun berdasarkan kolom ini.</p>
	 *
	 * @return waktu operasi posting; tidak pernah {@code null}.
	 */
	public Date getTanggal() {
		if (tanggal == null) {
			tanggal = ais.ui.util.WaktuUtil.getDate();
		}
		return tanggal;
	}

	/**
	 * Menyetel waktu operasi posting.
	 *
	 * <p>Hampir semua {@code Posting*Action} memanggilnya dengan tanggal posting yang dipilih
	 * operator (atau waktu server bila tidak diisi), sehingga nilai ini yang muncul pada
	 * label {@link #getNama()}.</p>
	 *
	 * @param tanggal waktu operasi posting; {@code null} akan dipulihkan menjadi waktu server
	 *        pada pembacaan berikutnya oleh {@link #getTanggal()}.
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengembalikan pengguna pelaku posting (kolom <code>tbmuser</code>, wajib).
	 *
	 * <p>Inilah sumber otoritatif "siapa yang memposting" pada entity ini — bukan
	 * {@link #getOleh()}. Diisi mesin posting dari {@code Common.getCurrentUser()} atau dari
	 * pengguna yang diteruskan jalur API/terjadwal. Relasi di-<i>fetch</i> dengan strategi
	 * {@code SELECT} (bukan join) dan meng-<i>cascade</i> {@code PERSIST}/{@code MERGE}.</p>
	 *
	 * @return pengguna pelaku posting.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "tbmuser", nullable = false)
	public Tbmuser getTbmuser() {
		return tbmuser;
	}

	/**
	 * Menyetel pengguna pelaku posting.
	 *
	 * <p>Wajib diisi sebelum {@code save} karena kolomnya {@code not null}, dan wajib pula
	 * sebelum {@link #getNama()}/{@link #toString()} dipanggil.</p>
	 *
	 * @param tbmuser pengguna pelaku posting.
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * Mengembalikan {@link Akun} yang tertaut ke cap ini (kolom <code>akun</code>, opsional).
	 *
	 * <p><b>Relasi mati:</b> tidak ada pemanggil {@code getAkun()}/{@code setAkun(...)} pada
	 * entity ini di seluruh repo, sehingga kolomnya selalu {@code null}. Akun jurnal
	 * ditentukan dokumen sumber, bukan cap posting.</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@code check(...)} milik
	 * {@link ais.database.model.GeneralValueObject} untuk meresolusi proxy lazy, lalu
	 * <b>menimpa field</b> dengan hasilnya. Bila resolusi gagal, {@code check} mengembalikan
	 * argumen apa adanya (proxy yang mungkin belum terinisialisasi), bukan {@code null}.</p>
	 *
	 * @return akun terkait, atau {@code null} (kasus normal).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = true)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/**
	 * Menyetel akun yang tertaut ke cap ini. Lihat catatan relasi mati pada
	 * {@link #getAkun()}.
	 *
	 * @param akun akun bagan akun; boleh {@code null}.
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Mengembalikan kategori operasi posting.
	 *
	 * <p>Berisi salah satu konstanta {@code JENIS_*} kelas ini. Nilai ini <b>ikut menentukan
	 * kolom <code>grup_transaksi.jenis</code></b>: {@code GrupTransaksi.getJenis()} menimpa
	 * field-nya dengan nilai di sini setiap kali dibaca. Layar "Sejarah Posting" menampilkannya
	 * sebagai kolom pertama, dan berbagai layar posting memakainya untuk memisahkan batch milik
	 * modulnya sendiri.</p>
	 *
	 * <p>Kolomnya tidak dianotasi {@code @Column}, sehingga dipetakan ke kolom
	 * <code>jenis</code> dengan panjang default.</p>
	 *
	 * @return kategori operasi posting, atau {@code null} bila cap dibuat lewat konstruktor
	 *         tanpa argumen dan tidak pernah diisi.
	 */
	public String getJenis() {
		return jenis;
	}

	/**
	 * Menyetel kategori operasi posting.
	 *
	 * <p>Nilai tidak divalidasi terhadap daftar konstanta {@code JENIS_*}; string bebas apa pun
	 * diterima dan akan merambat ke {@code grup_transaksi.jenis}.</p>
	 *
	 * @param jenis kategori operasi posting.
	 */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/**
	 * Mengembalikan tanggal efektif posting ke buku besar (kolom <code>tanggal_posting</code>,
	 * presisi DATE).
	 *
	 * <p>Berbeda dari {@link #getTanggal()} yang merekam <i>kapan operasi dijalankan</i>,
	 * kolom ini merekam <i>tanggal buku</i> yang dipilih operator. Pada praktiknya sebagian
	 * besar layar mengisi keduanya dengan nilai yang sama, dan sebagian layar lama tidak
	 * mengisinya sama sekali sehingga tetap memakai nilai inisialisasi waktu server.</p>
	 *
	 * <p>Perhatikan: tanggal yang dipakai pada baris jurnal sendiri
	 * ({@code Transaksi.tanggalPosting}) diisi terpisah oleh
	 * <code>CommonAkunting.saveTransaksi</code> dengan waktu server, jadi kedua nilai bisa
	 * berbeda bila operator memilih tanggal buku mundur.</p>
	 *
	 * @return tanggal efektif posting.
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalPosting() {
		return tanggalPosting;
	}

	/**
	 * Menyetel tanggal efektif posting ke buku besar.
	 *
	 * @param tanggalPosting tanggal buku posting.
	 */
	public void setTanggalPosting(Date tanggalPosting) {
		this.tanggalPosting = tanggalPosting;
	}

	/**
	 * Mengembalikan bendera pengakuan batch posting ke buku besar.
	 *
	 * <p><b>Bukan penanda "sudah dijurnal".</b> Status "sudah diposting" sebuah dokumen
	 * ditentukan oleh ada/tidaknya cap ini padanya, sedangkan bendera di sini menentukan
	 * apakah batch tersebut <i>diakui</i> pada instalasi yang menjalankan posting dua langkah.</p>
	 *
	 * <p><b>Nilai default yang bergantung konfigurasi:</b> bila kolomnya {@code null}, method
	 * mengembalikan {@code ConstantValues.otomatisTerposting} — sebuah <b>variabel statis
	 * global per-JVM</b> yang diisi saat inisialisasi aplikasi dari konfigurasi
	 * <code>otomatis_terposting</code> (nilai hardcoded awal: {@code true}). Artinya:</p>
	 * <ul>
	 *   <li>mode otomatis ({@code true}) — cap tanpa nilai eksplisit dianggap sudah diakui;</li>
	 *   <li>mode persetujuan manual ({@code false}) — cap tanpa nilai eksplisit dianggap
	 *       <i>belum</i> diakui, dan jurnalnya belum membentuk saldo buku besar.</li>
	 * </ul>
	 *
	 * <p><b>Non-obvious — default ini hanya berlaku di sisi Java.</b> Kueri yang membaca kolom
	 * secara langsung tidak melihatnya: <code>Restrictions.eq("postingHistory.posting", true)</code>
	 * dan <code>ph.posting = true</code> pada SQL laporan sama-sama tidak cocok dengan
	 * {@code NULL}. Sebagian besar {@code Posting*Action} tidak pernah memanggil
	 * {@link #setPosting(Boolean)}, sehingga banyak baris memang bernilai {@code NULL}. Hanya
	 * <code>PostingJurnalHelper.terapkanStatusPostingHistory</code> yang menangani hal ini
	 * dengan benar (<code>posting = true or posting is null</code>).</p>
	 *
	 * <p>Dibaca antara lain oleh <i>checkbox</i> "Posting" pada baris jurnal di
	 * {@code GrupTransaksiAction} — perhatikan bahwa <i>checkbox</i> itu menyunting cap yang
	 * dipakai <b>bersama seluruh batch</b>, bukan hanya jurnal pada barisnya.</p>
	 *
	 * @return {@code true}/{@code false} bila kolomnya terisi; bila kolomnya {@code null},
	 *         nilai {@code ConstantValues.otomatisTerposting} yang berlaku saat pemanggilan.
	 *         Tidak pernah mengembalikan {@code null}.
	 */
	public Boolean getPosting() {
		return posting == null ? ConstantValues.otomatisTerposting : posting;
	}

	/**
	 * Menyetel bendera pengakuan batch posting ke buku besar.
	 *
	 * <p>Dipanggil oleh sebagian layar posting dengan {@code true} tepat setelah cap dibuat,
	 * dan oleh layar jurnal umum untuk mencabut/mengembalikan pengakuan sebuah batch.</p>
	 *
	 * <p><b>Efek samping berskala batch:</b> karena satu {@code PostingHistory} dipakai
	 * bersama oleh seluruh dokumen, header jurnal, dan baris jurnal dari satu operasi
	 * "Posting Semua", menyetel {@code false} di sini mencabut pengakuan <b>seluruh batch
	 * sekaligus</b> — bukan hanya jurnal yang sedang dilihat. Pada instalasi dengan
	 * <code>otomatis_terposting</code> nonaktif, hal itu langsung mengeluarkan semua jurnal
	 * batch tersebut dari buku besar dan laporan keuangan.</p>
	 *
	 * @param posting {@code true} untuk mengakui batch, {@code false} untuk mencabut
	 *        pengakuan, {@code null} untuk mengembalikan ke perilaku default yang mengikuti
	 *        konfigurasi (lihat {@link #getPosting()}).
	 */
	public void setPosting(Boolean posting) {
		this.posting = posting;
	}

}
