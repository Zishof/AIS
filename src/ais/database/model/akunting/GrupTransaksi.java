package ais.database.model.akunting;
/* ENHANCED_PENGGUNAAN_ANGGARAN_MEMORY_SAFE_2026_06_03 - Java 1.6/1.7 compatible. */

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Calendar;
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

import org.hibernate.Session;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.ProsesTransferAction;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisPembayaran;
import ais.database.model.Kegiatan;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Deposit;
import ais.database.model.DetailKegiatan;
import ais.database.model.GeneralValueObject;
import ais.database.model.LogPembayaran;
import ais.database.model.Pegawai;
import ais.database.model.PengeluaranMahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.asset.PembayaranDpMasterAssetDetail;
import ais.database.model.asset.PembayaranPengadaanMasterAssetDetail;
import ais.database.model.asset.PembayaranTerminMasterAssetDetail;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAssetDetail;
import ais.database.model.asset.PenyusutanAsset;
import ais.database.model.asset.PerjanjianKerjasamaMasterAsset;
import ais.database.model.asset.SaldoAwalMasterAsset;
import ais.database.model.asset.SaldoAwalMasterAssetDetail;
import ais.database.model.koperasi.TransaksiKoperasi;
import ais.database.model.payroll.PembayaranGaji;
import ais.database.model.payroll.PembayaranGajiPunyaPegawai;
import ais.database.model.payroll.TransaksiPegawai;
import ais.database.model.rab.Mitra;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.Workspace;
import ais.database.model.sekolah.DepositSiswa;
import ais.database.model.sekolah.AkunPembayaranSiswa;
import ais.database.model.sekolah.ItemBiayaSekolah;
import ais.database.model.sekolah.PembayaranSiswaDetail;
import ais.database.model.sekolah.Tagihan;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelAgakKecilBoldMerah;
import ais.ui.util.MyLabelBoldMerah;

/**
 * Entity <b>GRUP TRANSAKSI</b> — satu <b>JURNAL</b> pada mesin pembukuan
 * <i>double-entry</i> AIS. Satu baris {@code akunting.grup_transaksi} adalah
 * <b>kepala jurnal</b> (nomor jurnal, tanggal, keterangan, jenis transaksi, penanda
 * posting/closing) yang memayungi sekumpulan <b>baris debit/kredit</b>
 * {@link Transaksi} (tabel {@code akunting.transaksi}, relasi terbalik lewat
 * properti {@code grupTransaksi} pada anak — entity ini TIDAK memegang koleksi anak).
 * Seluruh laporan resmi akunting (buku besar, neraca, laba rugi, arus kas, jurnal
 * umum) dibangun dari pasangan {@code GrupTransaksi} + {@code Transaksi}, sehingga
 * kelas ini adalah entity paling sentral pada sisi keuangan aplikasi.
 *
 * <h3>1. Anatomi satu jurnal</h3>
 * <pre>
 * akunting.grup_transaksi  (kepala)   1 ── n   akunting.transaksi (baris Dr/Cr)
 *   kode            = nomor jurnal              akun
 *   tanggalTransaksi                            debet / kredit
 *   keterangan                                  keterangan
 *   jenisTransaksi  = seri penomoran            grupTransaksi (FK balik)
 *   postingHistory  = cap "sudah diposting"     postingHistory
 *   closing         = cap "sudah ditutup"
 *   &lt;41 kolom referensi dokumen sumber&gt;
 *   ref             = pembeda KAKI jurnal
 *   kodeUnik        = kunci idempotensi
 * </pre>
 * Entity ini <b>tidak menyimpan sendiri</b> total debit/kredit yang dipakai laporan:
 * {@code totalDebet}/{@code totalKredit} adalah kolom pelengkap yang sering
 * dibiarkan nol; angka sesungguhnya selalu dijumlahkan ulang dari baris
 * {@link Transaksi} (lihat {@link #populateDeskripsi()}).
 *
 * <h3>2. Kolom referensi dokumen sumber — 41 relasi, satu terisi</h3>
 * Jurnal di AIS hampir selalu <b>turunan</b> dari sebuah dokumen operasional
 * (pembayaran siswa, penggajian, pengadaan aset, kas kecil, koperasi, pajak, …).
 * Karena itu entity ini memiliki <b>41 kolom FK opsional</b> — masing-masing menunjuk
 * satu jenis dokumen sumber — dan pada praktiknya <b>hanya satu</b> yang terisi per
 * baris. {@code CommonAkunting.saveTransaksi} memilih kolom mana yang diisi lewat
 * satu rantai {@code instanceof} terhadap argumen {@code Object reference}.
 * Kolom-kolom itu dipakai untuk tiga hal:
 * <ol>
 *   <li>menelusuri jurnal balik ke dokumen asal (tombol "lihat jurnal");</li>
 *   <li>menghitung status "sudah/belum diposting" per modul di dasbor
 *       (lihat {@code ais.action.master.helper.PostingJurnalHelper});</li>
 *   <li><b>pembatalan posting</b> — SQL native menghapus baris berdasarkan kolom
 *       referensi ini.</li>
 * </ol>
 *
 * <h3>3. {@link #ambilUnik()} dan {@link #getKodeUnik()} — kunci idempotensi</h3>
 * <p>{@link #ambilUnik()} menyusun kunci logis satu jurnal sebagai:</p>
 * <pre>{@code
 * kodeUnik = <nama kelas dokumen sumber> + "_" + <id dokumen sumber> + <ref>
 * }</pre>
 * <p>Perhatikan tiga sifat penting yang <b>menentukan perilaku seluruh mesin posting</b>:</p>
 * <ul>
 *   <li><b>Kolom {@code jenis} TIDAK ikut kunci.</b> {@link #getJenis()} hanyalah
 *   cerminan {@code postingHistory.jenis}, bukan komponen kunci. Dua kaki jurnal
 *   berbeda pada dokumen yang sama TIDAK dibedakan oleh {@code jenis}.</li>
 *   <li><b>{@code ref} adalah satu-satunya pembeda kaki.</b> Karena itu berlaku aturan
 *   baku: pada satu dokumen sumber, <b>kaki utama ber-{@code ref} {@code null}</b> dan
 *   <b>setiap kaki tambahan WAJIB memiliki {@code ref} sendiri yang berbeda</b>
 *   ({@code "pajak"}, {@code "pengembalian"}, {@code "denda_siswa"},
 *   {@code "diskon_siswa"}, {@code "dimuka_siswa"}, {@code "DP_PEKERJAAN"}, …;
 *   lihat konstanta {@code REF_*} pada {@code PostingJurnalHelper}). Melanggar aturan
 *   ini menyebabkan kaki kedua <b>hilang tanpa galat</b> (lihat butir 4).</li>
 *   <li><b>{@link #getKodeUnik()} bukan getter murni</b> — ia MENGHITUNG ULANG kunci
 *   dari field referensi setiap kali dibaca dan menugaskannya kembali ke
 *   {@code this.kodeUnik}. Nilai yang tersimpan di kolom {@code kode_unik} karena itu
 *   tidak pernah bisa menyimpang dari hasil hitung; {@link #setKodeUnik(String)}
 *   praktis tidak berpengaruh, dan koreksi manual di database akan tertimpa pada
 *   flush berikutnya.</li>
 * </ul>
 *
 * <h3>4. Pola POSTING — {@code CommonAkunting.saveTransaksi}</h3>
 * Seluruh jurnal otomatis dibangun oleh {@code CommonAkunting.saveTransaksi(...)}
 * (paket {@code ais.action.master.akunting.util}). Alurnya:
 * <ol>
 *   <li>membuat {@code GrupTransaksi} baru, mengisi <b>satu</b> kolom referensi sesuai
 *       tipe {@code reference}, lalu {@code setRef(ref)};</li>
 *   <li>mencari grup lain dengan {@code kodeUnik} yang sama
 *       ({@code Restrictions.eq("kodeUnik", grupTransaksi.getKodeUnik())},
 *       {@code setMaxResults(1)});</li>
 *   <li><b>bila ketemu</b>: grup lama hanya <b>dicap ulang</b>
 *       ({@code setPostingHistory}) — <b>tidak ada baris jurnal baru yang ditulis</b>;</li>
 *   <li><b>bila tidak</b>: nomor jurnal di-{@code generate}, kepala jurnal disimpan,
 *       lalu baris {@link Transaksi} debit dan kredit ditulis satu per satu.</li>
 * </ol>
 * <p><b>PERINGATAN INTEGRITAS (masih berlaku pada revisi ini).</b> Langkah 3 adalah
 * sumber kelas bug "kaki jurnal hilang": bila dua kaki jurnal pada satu dokumen
 * memakai {@code ref} yang sama (atau sama-sama {@code null}), keduanya menghasilkan
 * {@code kodeUnik} identik, kaki kedua dianggap duplikat, dan <b>jurnalnya senyap
 * tidak tertulis</b> sementara cap posting kaki pertama ditimpa. Contoh yang sudah
 * ditangani: dokumen {@link Tagihan} siswa memikul empat kaki (piutang, denda,
 * diskon, dibayar dimuka) dan kini dibedakan oleh
 * {@code REF_DENDA_SISWA}/{@code REF_DISKON_SISWA}/{@code REF_DIMUKA_SISWA}.</p>
 * <p><b>PERINGATAN INTEGRITAS TAMBAHAN — cakupan {@link #ambilUnik()} tidak lengkap.</b>
 * Dari 41 kolom referensi, {@link #ambilUnik()} hanya mengenali <b>26</b>. Lima belas
 * sisanya — {@code logPembayaran}, {@code penerimaanPengadaanMasterAsset},
 * {@code saldoAwalMasterAsset}, {@code pertangungjawabanKasBesar},
 * {@code transaksiKoperasi}, {@code pajak}, {@code pembayaranGaji},
 * {@code pembatalanTransaksiKantin}, {@code penghapusanMasterAsset},
 * {@code pembayaranAnggotaKoperasi}, {@code pencairanDiskon},
 * {@code penyesuaianSaldoAnggota}, {@code modalPenyertaanKoperasi},
 * {@code pembagianShu}, {@code notaSalesBiaya} — tidak menyumbang komponen apa pun ke
 * kunci, sehingga:</p>
 * <ul>
 *   <li>bila {@code ref} juga {@code null} ⇒ {@code kodeUnik} bernilai {@code null},
 *   pencocokan {@code kode_unik = NULL} tidak pernah cocok, dan <b>setiap posting ulang
 *   dokumen yang sama menghasilkan jurnal DUPLIKAT</b> (nilai tercatat dua kali di
 *   buku besar);</li>
 *   <li>bila {@code ref} terisi ⇒ {@code kodeUnik} menjadi <b>string {@code ref}
 *   telanjang</b> — mis. {@code "pajak"} untuk seluruh jurnal pajak LPJ dan
 *   {@code "DP_PEKERJAAN"} untuk seluruh jurnal DP pekerjaan vendor — sehingga kunci
 *   itu <b>global untuk seluruh instalasi</b>, melintasi dokumen maupun tenant. Efeknya
 *   kebalikan dari kasus pertama: hanya jurnal PERTAMA yang pernah tertulis dengan
 *   {@code ref} tersebut yang benar-benar ada; setiap posting berikutnya dianggap
 *   duplikat, jurnalnya tidak ditulis, dan cap posting dokumen lain ikut tertimpa.</li>
 * </ul>
 *
 * <h3>5. Pola PEMBATALAN posting</h3>
 * Urutan baku pada seluruh {@code Posting*Action}:
 * <ol>
 *   <li>hapus baris anak {@code akunting.transaksi} yang {@code grup_transaksi in
 *       (select id from akunting.grup_transaksi where &lt;kolom referensi&gt; = ?
 *       and closing is null)};</li>
 *   <li>hapus kepala {@code akunting.grup_transaksi} dengan syarat yang sama —
 *       <b>selalu disertai {@code and closing is null}</b> agar periode yang sudah
 *       ditutup tidak bisa dibongkar;</li>
 *   <li>lepas penanda posting pada dokumen sumber ({@code postingHistory = null}).</li>
 * </ol>
 * <p><b>Jebakan {@code NULL} pada saringan {@code ref}.</b> Kaki utama tersimpan dengan
 * {@code ref = NULL}. Dalam SQL, {@code ref != 'x'} pada baris ber-{@code ref} {@code NULL}
 * bernilai {@code NULL} (bukan {@code true}), sehingga baris itu <b>tidak pernah ikut
 * terhapus</b> — hasilnya jurnal yatim yang cap postingnya sudah dilepas. Bentuk yang
 * benar adalah {@code (ref is null or ref != 'x')} atau
 * {@code ref is not null and ref != 'x'} bila memang hanya kaki tambahan yang disasar.
 * Bentuk aman ini sudah dipakai a.l. oleh {@code PostingJurnalHelper.restriksiRefClosing}
 * dan {@code NewUiJournalService}.</p>
 * <p><b>Dokumen ber-kaki ganda.</b> {@link LogPembayaran}, {@link CicilanPembayaran},
 * {@code Pertangungjawaban} dan {@link Tagihan} siswa dapat memiliki lebih dari satu
 * jurnal. Pembatalan yang menghapus <i>hanya</i> berdasarkan kolom referensi tanpa
 * pembeda {@code ref} akan ikut melenyapkan jurnal kaki lain yang masih sah.</p>
 *
 * <h3>6. Tidak ada penjaga keseimbangan Dr = Cr di jalur klasik</h3>
 * {@code CommonAkunting.saveTransaksi} <b>tidak memvalidasi</b> bahwa total debit sama
 * dengan total kredit; jurnal timpang tersimpan tanpa galat. Yang ada hanyalah
 * <b>penanda tampilan</b>: {@link #populateDeskripsi()},
 * {@link #populateDeskripsiLengkap()} dan {@link #tampilkanJurnal(List, List, List,
 * List, String)} menyisipkan baris merah "TIDAK BALANCE". Penjaga sungguhan hanya ada
 * di mesin pemanggil tertentu (Penggajian, HPP/Kantin, {@code SaldoAwalAkunHelper})
 * dan di layar jurnal manual generasi baru ({@code NewUiJournalService.save} menolak
 * draft tidak seimbang dengan {@code IllegalArgumentException}).
 * <p>Perlu dicatat, pemeriksaan "balance" pada kelas ini membandingkan <b>string hasil
 * format</b> ({@code Common.numberFormat}), bukan angka mentah — selisih yang lebih
 * kecil dari presisi tampilan akan dilaporkan sebagai seimbang.</p>
 *
 * <h3>7. Idiom tanda nilai (bukan bug)</h3>
 * Di seluruh mesin jurnal AIS berlaku idiom lama: nilai bertanda negatif membalik
 * posisi Dr/Cr, dan nilai dengan {@code Math.abs(nilai) <= 0.1} diabaikan sebagai nol.
 * Lihat {@link #normalisasiDebitKredit(List, List, List, List, List, List, boolean)}.
 * Ini keputusan desain demi paritas dengan tombol posting generasi lama, bukan cacat.
 *
 * <h3>8. Getter yang MENULIS (write-back) — hati-hati saat sekadar membaca</h3>
 * Beberapa getter pada entity ini bukan pembaca murni: mereka menugaskan kembali nilai
 * ke field <b>terpetakan</b>, sehingga sekadar membaca entity di dalam session hidup
 * dapat menerbitkan {@code UPDATE} saat flush.
 * <ul>
 *   <li>{@link #getKodeUnik()} — menghitung ulang kunci idempotensi;</li>
 *   <li>{@link #getSatuanKerja()} — menurunkan satuan kerja dari rantai dokumen sumber;</li>
 *   <li>{@link #getAngarans()} — menormalkan dan menyalin daftar anggaran dari dokumen aset;</li>
 *   <li>{@link #getJenis()} — menyalin {@code postingHistory.jenis};</li>
 *   <li>{@link #getClosing()} — dapat <b>memindahkan</b> jurnal ke periode closing lain;</li>
 *   <li>{@link #getJenisTransaksi()} — jatuh ke {@code JenisTransaksi.DEFAULT} bila kosong;</li>
 *   <li>{@link #getTotalDebet()}/{@link #getTotalKredit()} — {@code null} → {@code 0.0}.</li>
 * </ul>
 * Selain itu {@link #getKepada()} dan {@link #getNomorTagihan()} mengembalikan
 * {@code ""} untuk nilai {@code null} tanpa menugaskan balik ke field: karena Hibernate
 * membaca properti lewat getter, baris yang di database bernilai {@code NULL} akan
 * selalu terlihat "kotor" dan tertulis ulang sebagai string kosong (dan nilainya
 * di-{@code trim}).
 *
 * <h3>9. Cakupan tenant</h3>
 * Entity ini <b>tidak memiliki kolom {@code sekolah} maupun {@code yayasan}</b>.
 * Pemisahan antar unit sepenuhnya bergantung pada {@link #getSatuanKerja()},
 * {@link #getWorkspace()}, dan rantai dokumen sumber. Setiap kueri jurnal yang lupa
 * menyaring salah satu dari itu bersifat <b>fail-open</b>: ia melihat jurnal seluruh
 * instalasi. Hal yang sama berlaku untuk {@link Closing} — entity periode tutup buku
 * juga tanpa kolom tenant, jadi periode closing memang berlaku global secara desain.
 *
 * <h3>10. Hak akses dan jejak audit</h3>
 * Layar jurnal utama {@code ais.action.master.akunting.GrupTransaksiAction} menegakkan
 * {@code CommonPrivilages.checkPrevilages(READ/CREATE/UPDATE/DELETE/APPROVE)} — gerbang
 * hak akses di sini <b>positif</b>, berbeda dengan sejumlah layar master lain. Namun
 * jendela {@code Posting*Action} yang benar-benar menulis jurnal umumnya diwarisi dari
 * menu induk modulnya masing-masing dan tidak terdaftar sebagai menu tersendiri.
 * <p>Kelas ini ber-{@code @Audited} (Hibernate Envers): setiap versi baris digandakan ke
 * {@code akunting.grup_transaksi_aud}, sehingga penghapusan jurnal di tabel utama tidak
 * menghapus jejaknya di tabel revisi.</p>
 *
 * <h3>11. Pengelompokan method di berkas ini</h3>
 * <ol>
 *   <li><b>Identitas &amp; audit</b> — {@link #getId()}, {@link #getKode()},
 *   {@link #getOleh()}, {@link #getOlehId()}, {@link #getTanggal_dirubah()},
 *   {@link #onUpdate()}, {@link #toString()}.</li>
 *   <li><b>Atribut kepala jurnal</b> — tanggal, keterangan, keperluan, kepada, alamat,
 *   nomor/tanggal tagihan, {@code jenisJurnal}, {@code parentCode}, {@code bulan},
 *   {@code tahun}, total debit/kredit.</li>
 *   <li><b>Relasi aktor &amp; klasifikasi</b> — {@link #getTbmuser()},
 *   {@link #getPegawai()}, {@link #getDisetujui()}, {@link #getMitra()},
 *   {@link #getJenisTransaksi()}, {@link #getSatuanKerja()}, {@link #getWorkspace()}.</li>
 *   <li><b>Penanda siklus hidup</b> — {@link #getPostingHistory()}, {@link #getJenis()},
 *   {@link #getClosing()}, {@link #getRef()}, {@link #getKodeUnik()},
 *   {@link #ambilUnik()}.</li>
 *   <li><b>41 kolom referensi dokumen sumber</b> — pasangan getter/setter sederhana.</li>
 *   <li><b>Render jurnal untuk layar</b> — {@link #populateDeskripsi()},
 *   {@link #populateDeskripsiLengkap()}, keluarga {@code tampilkanJurnal(...)},
 *   {@link #tampilkanJurnalPembayaranMahasiswa(List, List, List, Kegiatan)},
 *   {@link #tampilkanJurnalPembayaranSiswa(List, AkunPembayaranSiswa, boolean, Double)}
 *   beserta belasan helper statis privat pencari akun. Seluruh kelompok ini
 *   <b>hanya menampilkan</b> — tidak menyimpan jurnal apa pun.</li>
 *   <li><b>Rincian Kas Kecil</b> — {@link #kumpulkanRincianKasKecil(Session)},
 *   {@link #cocokkanRincianKasKecil(List, Transaksi)},
 *   {@link #petakanRincianKasKecilUntukEdit(Session, List)},
 *   {@link #simpanNamaRincianKasKecil(Session, Long, int, String)}. Satu-satunya
 *   kelompok pada kelas ini yang benar-benar MENULIS ke database
 *   ({@code KasKecil.formula}).</li>
 *   <li><b>Penelusuran dokumen pendukung</b> — {@link #ambilDaftarPengajuanTransfer()},
 *   {@link #ambilDisposisiSop()}.</li>
 * </ol>
 *
 * <p><b>Catatan pewarisan.</b> {@link ais.database.model.GeneralValueObject} bukan
 * {@code @Entity} maupun {@code @MappedSuperclass} — ia POJO abstrak biasa dan
 * Hibernate tidak memetakan propertinya. Karena itu {@code id}, {@code oleh},
 * {@code olehId} dan {@code tanggal_dirubah} <b>sengaja dideklarasikan ulang</b> di
 * kelas ini; itu keharusan teknis, bukan duplikasi yang perlu dibersihkan.</p>
 *
 * @see Transaksi
 * @see PostingHistory
 * @see Closing
 * @see JenisTransaksi
 * @see ais.action.master.akunting.util.CommonAkunting
 * @see ais.action.master.helper.PostingJurnalHelper
 * @see ais.action.master.akunting.GrupTransaksiAction
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "akunting", name = "grup_transaksi")
public class GrupTransaksi extends GeneralValueObject {

	/** Versi serialisasi kelas. Nilai ini kebetulan identik dengan beberapa entity lain di
	 * paket {@code akunting} (mis. {@link Closing}) — sisa salin-tempel generator, tidak
	 * mempengaruhi pemetaan. */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key {@code akunting.grup_transaksi.id}, identity/auto-increment. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (diisi
	 * {@code AuditTimestampInterceptor}). Lihat {@link #setOleh(String)} — setter menolak
	 * nilai kosong. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini. Lihat {@link #setOlehId(String)}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang menyentuh baris jurnal ini.
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah diubah lewat jalur
	 *         yang mengisi jejak audit.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Perilaku non-standar:</b> nilai {@code null} atau string kosong
	 * <b>diabaikan</b> — field lama dipertahankan. Konsekuensinya jejak audit tidak
	 * dapat dikosongkan lagi setelah pernah terisi, dan instance yang dipakai ulang
	 * (pola instance kanonik pada {@code AuditTimestampInterceptor}/
	 * {@code EntityIdentityMap}) bisa membawa atribusi lama bila jalur pemuatan
	 * menyetelnya dengan {@code null}.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong diabaikan tanpa galat.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks jurnal = <b>nomor jurnal</b> ({@link #getKode()}).
	 *
	 * <p>Dipakai combobox/label ZK dan sebagian laporan. Perhatikan bahwa method ini
	 * dapat mengembalikan {@code null} untuk jurnal yang belum bernomor (mis. instance
	 * baru yang belum lewat {@code generateNoJurnal}).</p>
	 *
	 * @return nomor jurnal apa adanya, bisa {@code null}.
	 */
	public String toString() {
		return kode;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Nilai {@code null}/kosong diabaikan —
	 * lihat catatan pada {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan tanpa galat.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris jurnal ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah terisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: menyerahkan pengisian jejak audit
	 * ({@code oleh}, {@code olehId}, {@code tanggal_dirubah}) ke
	 * {@code AuditTimestampInterceptor} tepat sebelum {@code UPDATE} dieksekusi.
	 *
	 * <p>Tidak dipanggil manual; hanya oleh penyedia persistence. Perlu diingat bahwa
	 * karena beberapa getter pada kelas ini menulis balik ke field terpetakan
	 * (lihat dokumentasi kelas), kait ini bisa ikut jalan pada operasi yang secara
	 * kasat mata hanya "membaca" jurnal.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Cap waktu perubahan terakhir; diinisialisasi ke waktu server saat instance dibuat
	 * dan disegarkan oleh {@link #onUpdate()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel cap waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah cap waktu baru; boleh {@code null}.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Cap waktu perubahan terakhir baris jurnal ini.
	 *
	 * @return cap waktu {@code TIMESTAMP}; tidak pernah {@code null} untuk instance yang
	 *         dibuat lewat konstruktor kelas ini.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nomor jurnal (kolom {@code kode}, {@code NOT NULL}). Diterbitkan oleh
	 * {@code CommonAkunting.generateNoJurnal(jenisTransaksi, true)} mengikuti seri
	 * {@link JenisTransaksi}. */
	private String kode;
	/** Tanggal efektif jurnal — inilah tanggal yang menentukan periode buku besar,
	 * BUKAN {@link #tanggal_dirubah}. */
	private Date tanggalTransaksi = ais.ui.util.WaktuUtil.getDate();
	/** Pengguna yang membuat jurnal ({@code Common.getCurrentUser()} saat posting). */
	private Tbmuser tbmuser;
	/** Pegawai terkait jurnal (dipakai jalur penggajian/kasbon). */
	private Pegawai pegawai;
	/** Keterangan/uraian jurnal; disalin juga ke setiap baris {@link Transaksi}. */
	private String keterangan;
	/** Total debit pelengkap. Sering dibiarkan {@code 0.0}; angka resmi selalu
	 * dijumlahkan ulang dari baris {@link Transaksi}. */
	private Double totalDebet;
	/** Total kredit pelengkap. Lihat catatan pada {@link #totalDebet}. */
	private Double totalKredit;
	/** Golongan jurnal (mis. jurnal transaksi vs jurnal penyesuaian/penutup). */
	private String jenisJurnal;
	/** Kode jurnal induk, untuk merangkai jurnal yang lahir dari satu proses beruntun. */
	private String parentCode;
	/** Bulan periode. Lihat peringatan pada {@link #getBulan()} — penurunan otomatisnya
	 * tidak berfungsi. */
	private Integer bulan;
	/** Tahun periode. Lihat peringatan pada {@link #getTahun()}. */
	private Integer tahun;
	/** Keperluan/uraian panjang (kolom {@code text}), dipakai cetakan bukti kas. */
	private String keperluan;
	/** Nama penerima pada bukti kas/transfer. */
	private String kepada;
	/** Alamat penerima pada bukti kas/transfer. */
	private String alamat;
	/** Keterangan rekening tujuan pada bukti transfer. */
	private String keteranganRekening;
	/** Nomor tagihan/faktur pihak ketiga yang mendasari jurnal. */
	private String nomorTagihan;
	/** Tanggal tagihan/faktur pihak ketiga (disimpan sebagai {@code DATE}). */
	private Date tanggalTagihan;

	/** Mitra/vendor lawan transaksi (modul RAB &amp; pengadaan). */
	private Mitra mitra;
	/** Seri penomoran + akun penciri jurnal. Getter menjatuhkan ke
	 * {@code JenisTransaksi.DEFAULT} bila kosong. */
	private JenisTransaksi jenisTransaksi;
	/** Pengguna yang menyetujui jurnal (alur persetujuan/approve). */
	private Tbmuser disetujui;
	/** Cap "sudah diposting". {@code null} = masih draft. Inilah penanda utama yang
	 * dipasang saat posting dan dilepas saat pembatalan. */
	private PostingHistory postingHistory;
	/** Cerminan {@code postingHistory.jenis}. TIDAK ikut menyusun {@link #ambilUnik()} —
	 * lihat dokumentasi kelas. */
	private String jenis;

	// reference
	/** Referensi dokumen sumber: cicilan pembayaran mahasiswa. <b>Ikut</b>
	 * {@link #ambilUnik()}. */
	private CicilanPembayaran cicilanPembayaran;
	/** Referensi dokumen sumber: detail kegiatan. <b>Ikut</b> {@link #ambilUnik()}. */
	private DetailKegiatan detailKegiatan;
	/** Referensi dokumen sumber: detail pembayaran siswa. <b>Ikut</b>
	 * {@link #ambilUnik()}. */
	private PembayaranSiswaDetail pembayaranSiswaDetail;
	/** Referensi dokumen sumber: setoran/tabungan siswa. <b>Ikut</b>
	 * {@link #ambilUnik()}. */
	private DepositSiswa depositSiswa;
	/** Referensi dokumen sumber: slip gaji per pegawai. <b>Ikut</b>
	 * {@link #ambilUnik()}. */
	private PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai;
	/** Referensi dokumen sumber: transaksi pegawai (kasbon/potongan). <b>Ikut</b>
	 * {@link #ambilUnik()} dengan nama kelasnya sendiri sejak perbaikan tabrakan kunci
	 * dengan {@link #pembayaranGajiPunyaPegawai} ber-id sama (sebelumnya {@link
	 * #ambilUnik()} keliru menuliskan nama kelas {@code PembayaranGajiPunyaPegawai}
	 * untuk field ini). */
	private TransaksiPegawai transaksiPegawai;
	/** Referensi dokumen sumber: log pembayaran (payment gateway/host-to-host).
	 * <b>TIDAK ikut</b> {@link #ambilUnik()} — lihat peringatan pada dokumentasi kelas. */
	private LogPembayaran logPembayaran;
	/** Referensi dokumen sumber: detail penerimaan pengadaan aset. <b>Ikut</b>
	 * {@link #ambilUnik()}. */
	private PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail;
	/** Referensi dokumen sumber: kepala penerimaan pengadaan aset. <b>TIDAK ikut</b>
	 * {@link #ambilUnik()}. */
	private PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset;
	/** Referensi dokumen sumber: detail saldo awal aset. <b>Ikut</b>
	 * {@link #ambilUnik()}. */
	private SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail;
	/** Referensi dokumen sumber: kepala saldo awal aset — dipakai juga oleh jurnal DP
	 * pekerjaan vendor ({@code ref = "DP_PEKERJAAN"}). <b>TIDAK ikut</b>
	 * {@link #ambilUnik()}. */
	private SaldoAwalMasterAsset saldoAwalMasterAsset;
	/** Referensi dokumen sumber: penyusutan aset. <b>Ikut</b> {@link #ambilUnik()}. */
	private PenyusutanAsset penyusutanAsset;
	/** Referensi dokumen sumber: pemesanan pengadaan aset. <b>Ikut</b>
	 * {@link #ambilUnik()}. */
	private PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset;
	/** Referensi dokumen sumber: deposit (mahasiswa/umum). <b>Ikut</b>
	 * {@link #ambilUnik()}. */
	private Deposit deposit;
	/** Referensi dokumen sumber: tagihan siswa. Dokumen ini memikul EMPAT kaki jurnal
	 * yang dibedakan lewat {@link #ref}. <b>Ikut</b> {@link #ambilUnik()}. */
	private Tagihan tagihan;
	/** Referensi dokumen sumber: pengeluaran mahasiswa. <b>Ikut</b>
	 * {@link #ambilUnik()}. */
	private PengeluaranMahasiswa pengeluaranMahasiswa;

	/** Referensi dokumen sumber: LPJ uang muka. Dokumen ini memikul beberapa kaki jurnal
	 * ({@code null} utama, {@code "pajak"}, {@code "pengembalian"}). <b>Ikut</b>
	 * {@link #ambilUnik()}. */
	private Pertangungjawaban pertangungjawaban;
	/** Referensi dokumen sumber: LPJ kas besar. <b>TIDAK ikut</b>
	 * {@link #ambilUnik()}. */
	private PertangungjawabanKasBesar pertangungjawabanKasBesar;
	/** Referensi dokumen sumber: uang muka. <b>Ikut</b> {@link #ambilUnik()}. */
	private UangMuka uangMuka;
	/** Referensi dokumen sumber: kas kecil. <b>Ikut</b> {@link #ambilUnik()}. */
	private KasKecil kasKecil;
	/** Referensi dokumen sumber: penggantian (reimburse) kas kecil. <b>Ikut</b>
	 * {@link #ambilUnik()}. */
	private PenggantianKasKecil penggantianKasKecil;
	/** Referensi dokumen sumber: jenis/pos kas kecil. <b>Ikut</b>
	 * {@link #ambilUnik()}. */
	private JenisKasKecil jenisKasKecil;

	/** Referensi dokumen sumber: dana talangan. <b>Ikut</b> {@link #ambilUnik()}. */
	private DanaTalangan danaTalangan;

	/** Referensi dokumen sumber: detail pembayaran pengadaan aset. <b>Ikut</b>
	 * {@link #ambilUnik()}. */
	private PembayaranPengadaanMasterAssetDetail pembayaranPengadaanMasterAssetDetail;
	/** Referensi dokumen sumber: detail pembayaran DP aset. <b>Ikut</b>
	 * {@link #ambilUnik()}. */
	private PembayaranDpMasterAssetDetail pembayaranDpMasterAssetDetail;
	/** Referensi dokumen sumber: detail pembayaran termin aset. <b>Ikut</b>
	 * {@link #ambilUnik()}. */
	private PembayaranTerminMasterAssetDetail pembayaranTerminMasterAssetDetail;
	/** Referensi dokumen sumber: perjanjian kerja sama aset. <b>Ikut</b>
	 * {@link #ambilUnik()}. */
	private PerjanjianKerjasamaMasterAsset perjanjianKerjasamaMasterAsset;
	/** Referensi dokumen sumber: kas besar. <b>Ikut</b> {@link #ambilUnik()}. */
	private KasBesar kasBesar;
	/** Referensi dokumen sumber: transaksi koperasi/kantin. <b>TIDAK ikut</b>
	 * {@link #ambilUnik()}. */
	private TransaksiKoperasi transaksiKoperasi;
	/** Referensi dokumen sumber: daftar pengajuan transfer. <b>Ikut</b>
	 * {@link #ambilUnik()}. */
	private DaftarPengajuanTransfer daftarPengajuanTransfer;
	/** Referensi dokumen sumber: transitori (rekening antara). <b>Ikut</b>
	 * {@link #ambilUnik()}. */
	private Transitori transitori;
	/** Referensi dokumen sumber: pajak. Dipakai dengan {@code ref = "pajak"} —
	 * <b>TIDAK ikut</b> {@link #ambilUnik()}, sehingga kunci uniknya menjadi string
	 * {@code "pajak"} telanjang. Lihat peringatan pada dokumentasi kelas. */
	private Pajak pajak;
	/** Satuan kerja pemilik jurnal. Salah satu dari dua penyaring tenant de facto pada
	 * entity ini — lihat {@link #getSatuanKerja()}. */
	private SatuanKerja satuanKerja;
	/** Anggaran/workspace pemilik jurnal (modul RAB). */
	private Workspace workspace;
	/** Referensi dokumen sumber: kepala pembayaran gaji. <b>TIDAK ikut</b>
	 * {@link #ambilUnik()}. */
	private PembayaranGaji pembayaranGaji;
	/** Daftar id anggaran terkait dalam bentuk string dipisah koma. Getter menormalkan
	 * dan menyalinnya dari dokumen aset — lihat {@link #getAngarans()}. */
	private String angarans;
	/** <b>Pembeda KAKI jurnal</b> pada satu dokumen sumber, dan satu-satunya komponen
	 * pembeda selain kelas+id dokumen di {@link #ambilUnik()}. Kaki utama {@code null};
	 * kaki tambahan memakai konstanta {@code REF_*} pada
	 * {@code ais.action.master.helper.PostingJurnalHelper}. */
	private String ref;
	/** Cap periode tutup buku. Selama {@code null}, jurnal masih boleh dibatalkan
	 * (seluruh SQL pembatalan menyertakan {@code and closing is null}). */
	private Closing closing;
	/** Kunci idempotensi tersimpan. Nilainya selalu ditimpa hasil {@link #ambilUnik()}
	 * pada setiap pembacaan {@link #getKodeUnik()}. */
	private String kodeUnik;

	/** Konstruktor kosong yang diwajibkan Hibernate/JavaBean. */
	public GrupTransaksi() {
	}

	/**
	 * Konstruktor pintasan berdasarkan primary key — dipakai untuk membentuk referensi
	 * ringan tanpa memuat baris dari database.
	 *
	 * @param id primary key jurnal.
	 */
	public GrupTransaksi(Long id) {
		this.id = id;
	}

	/**
	 * Primary key jurnal.
	 *
	 * <p>Kolom dipetakan {@code insertable = false} karena nilainya diterbitkan database
	 * (identity/auto-increment).</p>
	 *
	 * @return primary key, {@code null} untuk instance yang belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Normalnya hanya dipanggil Hibernate.
	 *
	 * @param id primary key jurnal.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nomor jurnal (kolom {@code kode}, {@code NOT NULL}).
	 *
	 * <p>Diterbitkan {@code CommonAkunting.generateNoJurnal(...)} mengikuti seri
	 * {@link JenisTransaksi} yang akunnya cocok dengan salah satu akun debit/kredit
	 * jurnal; bila tidak ada yang cocok dipakai seri {@code JenisTransaksi.DEFAULT}.</p>
	 *
	 * @return nomor jurnal.
	 */
	@Column(name = "kode", nullable = false, length = 255)
	public String getKode() {
		return this.kode;
	}

	/**
	 * Menyetel nomor jurnal.
	 *
	 * @param kode nomor jurnal hasil {@code generateNoJurnal}.
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Keterangan/uraian jurnal. Nilai yang sama disalin ke setiap baris
	 * {@link Transaksi} anak saat posting.
	 *
	 * @return keterangan jurnal, boleh {@code null}.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan jurnal.
	 *
	 * @param keterangan uraian jurnal.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menyetel tanggal efektif jurnal.
	 *
	 * <p><b>Penting:</b> inilah tanggal yang menentukan periode buku besar dan yang
	 * dibandingkan dengan tanggal {@link Closing} pada {@link #getClosing()} — bukan
	 * tanggal input.</p>
	 *
	 * @param tanggalTransaksi tanggal efektif jurnal.
	 */
	public void setTanggalTransaksi(Date tanggalTransaksi) {
		this.tanggalTransaksi = tanggalTransaksi;
	}

	/**
	 * Tanggal efektif jurnal (kolom {@code tanggal_transaksi}, {@code NOT NULL}).
	 *
	 * @return tanggal efektif; untuk instance baru sudah terisi waktu server.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_transaksi", nullable = false)
	public Date getTanggalTransaksi() {
		return tanggalTransaksi;
	}

	/**
	 * Menyetel pengguna pembuat jurnal.
	 *
	 * @param tbmuser pengguna pembuat; diisi {@code Common.getCurrentUser()} oleh
	 *        {@code CommonAkunting.saveTransaksi}.
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * Pengguna yang membuat jurnal ini.
	 *
	 * <p>Memanggil {@code check(...)} lebih dulu untuk menyelesaikan proxy lazy yang
	 * mungkin sudah <i>detached</i> (lihat
	 * {@link ais.database.model.GeneralValueObject#check(Object)}); hasilnya ditugaskan
	 * kembali ke field karena instance kanonik bisa berbeda dari proxy semula.</p>
	 *
	 * @return pengguna pembuat, atau {@code null} untuk jurnal yang dibuat proses batch
	 *         tanpa konteks pengguna.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		tbmuser = check(tbmuser);
		return tbmuser;
	}

	/**
	 * Menyetel total debit pelengkap.
	 *
	 * @param totalDebet total debit; boleh {@code null}.
	 */
	public void setTotalDebet(Double totalDebet) {
		this.totalDebet = totalDebet;
	}

	/**
	 * Total debit pelengkap pada kepala jurnal.
	 *
	 * <p><b>Bukan angka resmi.</b> Nilai ini sering dibiarkan {@code 0.0} oleh jalur
	 * posting; total yang dipakai laporan maupun penanda "TIDAK BALANCE" selalu
	 * dijumlahkan ulang dari baris {@link Transaksi}. Getter menormalkan {@code null}
	 * menjadi {@code 0.0} dan menugaskannya kembali ke field, sehingga pembacaan saja
	 * dapat menandai entity kotor.</p>
	 *
	 * @return total debit, tidak pernah {@code null}.
	 */
	@Column(name = "total_debet", nullable = true)
	public Double getTotalDebet() {
		if (totalDebet == null) {
			totalDebet = 0.0;
		}
		return totalDebet;
	}

	/**
	 * Menyetel total kredit pelengkap.
	 *
	 * @param totalKredit total kredit; boleh {@code null}.
	 */
	public void setTotalKredit(Double totalKredit) {
		this.totalKredit = totalKredit;
	}

	/**
	 * Total kredit pelengkap pada kepala jurnal. Lihat catatan pada
	 * {@link #getTotalDebet()}.
	 *
	 * @return total kredit, tidak pernah {@code null}.
	 */
	@Column(name = "total_kredit", nullable = true)
	public Double getTotalKredit() {
		if (totalKredit == null) {
			totalKredit = 0.0;
		}
		return totalKredit;
	}

	/**
	 * Menyetel golongan jurnal.
	 *
	 * @param jenisJurnal golongan jurnal (mis. jurnal transaksi, penyesuaian, penutup).
	 */
	public void setJenisJurnal(String jenisJurnal) {
		this.jenisJurnal = jenisJurnal;
	}

	/**
	 * Golongan jurnal — memisahkan jurnal transaksi harian dari jurnal penyesuaian dan
	 * jurnal penutup pada laporan.
	 *
	 * @return golongan jurnal, boleh {@code null}.
	 */
	@Column(name = "jenis_jurnal", nullable = true)
	public String getJenisJurnal() {
		return jenisJurnal;
	}

	/**
	 * Menyetel kode jurnal induk.
	 *
	 * @param parentCode kode jurnal induk.
	 */
	public void setParentCode(String parentCode) {
		this.parentCode = parentCode;
	}

	/**
	 * Kode jurnal induk — merangkai beberapa jurnal yang lahir dari satu proses beruntun
	 * (mis. pembayaran + jurnal pajaknya) agar dapat ditelusuri bersama.
	 *
	 * @return kode jurnal induk, boleh {@code null}.
	 */
	@Column(name = "parent_code", nullable = true)
	public String getParentCode() {
		return parentCode;
	}

	/**
	 * Pegawai yang terkait jurnal ini (jalur penggajian, kasbon, dan pertanggungjawaban
	 * pegawai). Memanggil {@code check(...)} untuk menyelesaikan proxy lazy.
	 *
	 * @return pegawai terkait, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = true)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	/**
	 * Menyetel pegawai terkait jurnal.
	 *
	 * @param pegawai pegawai terkait; boleh {@code null}.
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Keperluan/uraian panjang jurnal (kolom bertipe {@code text}), dipakai pada cetakan
	 * bukti kas keluar/masuk.
	 *
	 * @return keperluan, boleh {@code null}.
	 */
	@Column(name = "keperluan", columnDefinition = "text", nullable = true)
	public String getKeperluan() {
		return keperluan;
	}

	/**
	 * Menyetel keperluan/uraian panjang jurnal.
	 *
	 * @param keperluan uraian panjang.
	 */
	public void setKeperluan(String keperluan) {
		this.keperluan = keperluan;
	}

	/**
	 * Nama penerima pada bukti kas/transfer.
	 *
	 * <p><b>Getter tidak murni terhadap persistence:</b> nilai {@code null} dikembalikan
	 * sebagai string kosong dan nilai non-null di-{@code trim} — tanpa ditugaskan
	 * kembali ke field. Karena Hibernate membaca properti lewat getter, baris yang di
	 * database bernilai {@code NULL} akan terus terlihat "kotor" dan tertulis ulang
	 * sebagai {@code ''}, serta spasi tepi hilang secara permanen.</p>
	 *
	 * @return nama penerima, tidak pernah {@code null}.
	 */
	public String getKepada() {
		return kepada == null ? "" : kepada.trim();
	}

	/**
	 * Menyetel nama penerima pada bukti kas/transfer.
	 *
	 * @param kepada nama penerima; boleh {@code null}.
	 */
	public void setKepada(String kepada) {
		this.kepada = kepada;
	}

	/**
	 * Alamat penerima pada bukti kas/transfer.
	 *
	 * @return alamat penerima, boleh {@code null}.
	 */
	public String getAlamat() {
		return alamat;
	}

	/**
	 * Menyetel alamat penerima.
	 *
	 * @param alamat alamat penerima.
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Keterangan rekening tujuan yang dicetak pada bukti transfer.
	 *
	 * @return keterangan rekening, boleh {@code null}.
	 */
	public String getKeteranganRekening() {
		return keteranganRekening;
	}

	/**
	 * Menyetel keterangan rekening tujuan.
	 *
	 * @param keteranganRekening keterangan rekening tujuan.
	 */
	public void setKeteranganRekening(String keteranganRekening) {
		this.keteranganRekening = keteranganRekening;
	}

	/**
	 * Nomor tagihan/faktur pihak ketiga yang mendasari jurnal.
	 *
	 * <p>Berperilaku sama dengan {@link #getKepada()}: {@code null} menjadi string kosong
	 * dan nilai di-{@code trim} tanpa write-back — lihat catatan di sana.</p>
	 *
	 * @return nomor tagihan, tidak pernah {@code null}.
	 */
	public String getNomorTagihan() {
		return nomorTagihan == null ? "" : nomorTagihan.trim();
	}

	/**
	 * Menyetel nomor tagihan/faktur pihak ketiga.
	 *
	 * @param nomorTagihan nomor tagihan; boleh {@code null}.
	 */
	public void setNomorTagihan(String nomorTagihan) {
		this.nomorTagihan = nomorTagihan;
	}

	/**
	 * Tanggal tagihan/faktur pihak ketiga (disimpan sebagai {@code DATE}, tanpa jam).
	 *
	 * @return tanggal tagihan, boleh {@code null}.
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalTagihan() {
		return tanggalTagihan;
	}

	/**
	 * Menyetel tanggal tagihan/faktur pihak ketiga.
	 *
	 * @param tanggalTagihan tanggal tagihan.
	 */
	public void setTanggalTagihan(Date tanggalTagihan) {
		this.tanggalTagihan = tanggalTagihan;
	}

	/**
	 * Mitra/vendor lawan transaksi (modul RAB dan pengadaan aset). Memanggil
	 * {@code check(...)} untuk menyelesaikan proxy lazy.
	 *
	 * @return mitra, atau {@code null} untuk jurnal internal.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mitra", nullable = true)
	public Mitra getMitra() {
		mitra = check(mitra);
		return mitra;
	}

	/**
	 * Menyetel mitra/vendor lawan transaksi.
	 *
	 * @param mitra mitra; boleh {@code null}.
	 */
	public void setMitra(Mitra mitra) {
		this.mitra = mitra;
	}

	/**
	 * Jenis transaksi jurnal — menentukan <b>seri penomoran</b> jurnal dan akun
	 * pencirinya.
	 *
	 * <p><b>Getter dengan efek samping (fallback + write-back):</b> setelah
	 * {@code check(...)}, bila hasilnya masih {@code null} field ditugaskan
	 * {@code JenisTransaksi.DEFAULT}. Artinya jurnal yang di database berkolom
	 * {@code jenis_transaksi = NULL} akan tampak (dan pada flush berikutnya tersimpan)
	 * sebagai jenis DEFAULT. Ini disengaja agar laporan tidak pernah kehilangan baris
	 * karena jenis kosong.</p>
	 *
	 * @return jenis transaksi; tidak pernah {@code null} selama konstanta
	 *         {@code JenisTransaksi.DEFAULT} tersedia.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_transaksi", nullable = true)
	public JenisTransaksi getJenisTransaksi() {
		jenisTransaksi = check(jenisTransaksi);
		if (jenisTransaksi == null) {
			jenisTransaksi = JenisTransaksi.DEFAULT;
		}
		return jenisTransaksi;
	}

	/**
	 * Menyetel jenis transaksi jurnal.
	 *
	 * @param jenisTransaksi jenis transaksi; boleh {@code null} (getter akan
	 *        menjatuhkannya ke {@code DEFAULT}).
	 */
	public void setJenisTransaksi(JenisTransaksi jenisTransaksi) {
		this.jenisTransaksi = jenisTransaksi;
	}

	/**
	 * Pengguna yang menyetujui jurnal pada alur persetujuan.
	 *
	 * @return pengguna penyetuju, atau {@code null} bila belum/tidak melalui persetujuan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui", nullable = true)
	public Tbmuser getDisetujui() {
		disetujui = check(disetujui);
		return disetujui;
	}

	/**
	 * Menyetel pengguna penyetuju jurnal.
	 *
	 * @param disetujui pengguna penyetuju.
	 */
	public void setDisetujui(Tbmuser disetujui) {
		this.disetujui = disetujui;
	}

	/**
	 * Cap "sudah diposting" jurnal ini.
	 *
	 * <p>{@code null} berarti jurnal masih <b>draft</b>. Nilai non-null dipasang oleh
	 * {@code CommonAkunting.saveTransaksi} — baik saat jurnal baru ditulis maupun saat
	 * grup lama ber-{@code kodeUnik} sama hanya <b>dicap ulang</b>. Pembatalan posting
	 * melepas cap ini pada dokumen sumber setelah baris jurnal dihapus.</p>
	 *
	 * <p>Nilai {@code postingHistory.jenis} juga tercermin di {@link #getJenis()}.</p>
	 *
	 * @return riwayat posting, atau {@code null} bila masih draft.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * Menyetel cap posting jurnal.
	 *
	 * @param postingHistory riwayat posting; {@code null} mengembalikan jurnal ke status
	 *        draft.
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

	/**
	 * Referensi dokumen sumber: cicilan pembayaran mahasiswa.
	 *
	 * @return cicilan pembayaran asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "cicilan_pembayaran", nullable = true)
	public CicilanPembayaran getCicilanPembayaran() {
		return cicilanPembayaran;
	}

	/**
	 * Menyetel referensi cicilan pembayaran mahasiswa.
	 *
	 * <p>Ikut menyusun {@link #ambilUnik()}; mengubahnya mengubah kunci idempotensi
	 * jurnal.</p>
	 *
	 * @param cicilanPembayaran dokumen cicilan asal jurnal.
	 */
	public void setCicilanPembayaran(CicilanPembayaran cicilanPembayaran) {
		this.cicilanPembayaran = cicilanPembayaran;
	}

	/**
	 * Membangun rangkuman jurnal ini sebagai <b>tabel HTML</b> untuk tooltip/kolom
	 * "Uraian" pada daftar jurnal.
	 *
	 * <p>Alur: menyegarkan instance dari database ({@code session.refresh(this)}),
	 * memuat seluruh baris {@link Transaksi} anak (urut debit menurun lalu tanggal
	 * menaik), merangkainya menjadi tabel Akun/Keterangan/Debet/Kredit, lalu menambahkan
	 * baris TOTAL.</p>
	 *
	 * <p><b>Penanda keseimbangan.</b> Status balance ditentukan dengan membandingkan
	 * <b>string hasil format</b> ({@code Common.numberFormat}) total debit dan total
	 * kredit — bukan angka mentah. Selisih yang lebih kecil dari presisi tampilan akan
	 * dilaporkan sebagai seimbang. Bila tidak seimbang, satu baris merah "TIDAK BALANCE"
	 * disisipkan. Ini murni penanda visual: tidak ada penolakan simpan di sini maupun di
	 * {@code CommonAkunting.saveTransaksi}.</p>
	 *
	 * <p><b>Efek samping / biaya:</b> {@code session.refresh(this)} adalah satu
	 * <i>round-trip</i> database per pemanggilan, dan memuat baris anak adalah kueri
	 * kedua. Karena itu varian {@link #populateDeskripsiLengkap()} sengaja TIDAK
	 * memanggil {@code refresh} — jangan memakai method ini di dalam <i>renderer</i>
	 * daftar berisi banyak baris.</p>
	 *
	 * <p>Untuk jurnal yang belum tersimpan ({@code getId() == null}) dikembalikan
	 * {@code {false, ""}}.</p>
	 *
	 * @return array dua elemen: {@code [0]} {@link Boolean} status seimbang,
	 *         {@code [1]} {@link String} tabel HTML.
	 */
	@SuppressWarnings("unchecked")
	public Object[] populateDeskripsi() {
		String deskripsi = "";
		boolean balance = false;
		if (getId() != null) {
			Session session = HibernateUtil.currentSession();
			session.refresh(this);
			List<Transaksi> transaksis = session.createCriteria(Transaksi.class).addOrder(Order.desc("debet"))
					.addOrder(Order.asc("tanggalTransaksi")).add(Restrictions.eq("grupTransaksi", this)).list();
			deskripsi = "<table style='width:100%;'>" + "<thead>";
			deskripsi += "<tr>";
			deskripsi += "<th style='border:solid;border-width: thin;'>Akun</th>";
			deskripsi += "<th style='border:solid;border-width: thin;'>Keterangan</th>";
			deskripsi += "<th style='border:solid;border-width: thin;'>Debet</th>";
			deskripsi += "<th style='border:solid;border-width: thin;'>Kredit</th>";
			deskripsi += "</tr>" + "</thead>" + "<tbody>";
			Double totalDebet = 0.0;
			Double totalKredit = 0.0;
			for (Transaksi transaksi : transaksis) {
				deskripsi += "<tr>";
				deskripsi += "<td style='border:solid;border-width: thin;'>" + (transaksi.getAkun() == null ? "" : transaksi.getAkun().getKode() + " - " + transaksi.getAkun().getNama()) + "</td>";
				deskripsi += "<td style='border:solid;border-width: thin;' >" + transaksi.getKeterangan() + "</td>";
				deskripsi += "<td style='border:solid;border-width: thin;' align='right'>"
						+ Common.numberFormat.get().format(transaksi.getDebet()) + "</td>";
				deskripsi += "<td style='border:solid;border-width: thin;' align='right'>"
						+ Common.numberFormat.get().format(transaksi.getKredit()) + "</td>";
				deskripsi += "</tr>";

				totalDebet += transaksi.getDebet();
				totalKredit += transaksi.getKredit();
			}

			deskripsi += "<tr>";
			deskripsi += "<td style='border:solid;border-width: thin;font-weight: bolder;'></td>";
			deskripsi += "<td style='border:solid;border-width: thin;font-weight: bolder;'>TOTAL : </td>";
			deskripsi += "<td style='border:solid;border-width: thin;font-weight: bolder;' align='right'>"
					+ Common.numberFormat.get().format(totalDebet) + "</td>";
			deskripsi += "<td style='border:solid;border-width: thin;font-weight: bolder;' align='right'>"
					+ Common.numberFormat.get().format(totalKredit) + "</td>";
			deskripsi += "</tr>";

			balance = Common.numberFormat.get().format(totalDebet)
					.equals(Common.numberFormat.get().format(totalKredit));

			if (!balance) {
				deskripsi += "<tr>";
				deskripsi += "<td style='border:solid;border-width: thin;' align='right'></td>";
				deskripsi += "<td style='border:solid;border-width: thin;color:red;font-weight: bolder;'>TIDAK BALANCE</td>";
				deskripsi += "<td style='border:solid;border-width: thin;' align='right'></td>";
				deskripsi += "<td style='border:solid;border-width: thin;' align='right'></td>";
				deskripsi += "</tr>";
			}

			deskripsi += "</tbody></table>";
		}
		return new Object[] { balance, deskripsi };
	}

	/**
	 * Versi kaya dari {@link #populateDeskripsi()}: merangkai jurnal sebagai komponen ZK
	 * {@link Grid} (bukan HTML) sehingga baris jurnal dapat memuat tautan yang bisa
	 * diklik.
	 *
	 * <p>Perbedaan penting dari {@link #populateDeskripsi()}:</p>
	 * <ul>
	 *   <li><b>tanpa {@code session.refresh(this)}</b> — sengaja dihapus agar aman
	 *   dipakai dari <i>renderer</i> daftar (lihat komentar di badan method);</li>
	 *   <li>bila jurnal bersumber dari Kas Kecil / Penggantian Kas Kecil, dua kolom
	 *   tambahan "Keterangan Biaya" dan "Anggaran" ditampilkan, diisi dari JSON
	 *   {@code KasKecil.formula} lewat {@link #kumpulkanRincianKasKecil(Session)} dan
	 *   {@link #cocokkanRincianKasKecil(List, Transaksi)}. Untuk jurnal jenis lain
	 *   tampilan tetap empat kolom seperti semula;</li>
	 *   <li>bila jurnal tertaut pengajuan transfer ({@link #ambilDaftarPengajuanTransfer()})
	 *   atau disposisi SOP ({@link #ambilDisposisiSop()}), sebuah baris tambahan berisi
	 *   tautan dipasang: tautan pertama membuka layar proses transfer
	 *   ({@code ProsesTransferAction.onAddExternal}), tautan kedua membuka alur SOP
	 *   ({@code TampilanAlurSopAction.prosess}).</li>
	 * </ul>
	 *
	 * <p>Penanda "TIDAK BALANCE" memakai pembandingan string terformat yang sama dengan
	 * {@link #populateDeskripsi()} — lihat catatan di sana.</p>
	 *
	 * <p>Untuk jurnal yang belum tersimpan dikembalikan grid kosong dan status
	 * {@code false}.</p>
	 *
	 * @return array dua elemen: {@code [0]} {@link Boolean} status seimbang,
	 *         {@code [1]} {@link Grid} siap dipasang ke komponen induk.
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	public Object[] populateDeskripsiLengkap() {
		Grid grid = new Grid();
		grid.setWidth("100%");
		grid.setHeight("100%");
		boolean balance = false;
		if (getId() != null) {
			Session session = HibernateUtil.currentSession();
			// session.refresh(this) dihapus — menghindari DB round-trip per baris renderer
			List<Transaksi> transaksis = session.createCriteria(Transaksi.class).addOrder(Order.desc("debet"))
					.addOrder(Order.asc("tanggalTransaksi")).add(Restrictions.eq("grupTransaksi", this)).list();

			// Rincian Kas Kecil (Keterangan Biaya + Anggaran) untuk ditampilkan per baris jurnal.
			// Hanya terisi bila jurnal ini bersumber dari Kas Kecil / Penggantian Kas Kecil; jurnal
			// jenis lain tetap tampil seperti semula (2 kolom ini tidak ditambahkan).
			final List<Object[]> rincianItems = kumpulkanRincianKasKecil(session);
			final boolean adaRincian = !rincianItems.isEmpty();

			Columns columns = new Columns();
			columns.setParent(grid);
			columns.setSizable(true);
			columns.setStyle("background:#f8fafc; border-bottom:1px solid #e5e7eb; font-weight:bold;");

			MyColumnConfig column = new MyColumnConfig("Akun");
			column.setParent(columns);
			column.setWidth("25%");

			column = new MyColumnConfig("Keterangan");
			column.setParent(columns);
			column.setWidth(adaRincian ? "24%" : "45%");

			if (adaRincian) {
				column = new MyColumnConfig("Keterangan Biaya");
				column.setParent(columns);
				column.setWidth("21%");

				column = new MyColumnConfig("Anggaran");
				column.setParent(columns);
				column.setWidth("18%");
			}

			column = new MyColumnConfig("Debet");
			column.setAlign("right");
			column.setParent(columns);

			column = new MyColumnConfig("Kredit");
			column.setAlign("right");
			column.setParent(columns);

			Rows rows = new Rows();
			rows.setParent(grid);

			Double totalDebet = 0.0;
			Double totalKredit = 0.0;
			for (Transaksi transaksi : transaksis) {

				Row row = new Row();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new Label(transaksi.getAkun() == null ? "" : transaksi.getAkun().getKode() + " - " + transaksi.getAkun().getNama()));

				row.appendChild(new MyLabelAgakKecil(transaksi.getKeterangan()));

				if (adaRincian) {
					String[] rincian = cocokkanRincianKasKecil(rincianItems, transaksi);
					row.appendChild(new MyLabelAgakKecil(rincian[0]));
					row.appendChild(new MyLabelAgakKecil(rincian[1]));
				}

				row.appendChild(new Label(Common.numberFormat.get().format(transaksi.getDebet())));
				row.appendChild(new Label(Common.numberFormat.get().format(transaksi.getKredit())));

				totalDebet += transaksi.getDebet();
				totalKredit += transaksi.getKredit();
			}

			balance = Common.numberFormat.get().format(totalDebet)
					.equals(Common.numberFormat.get().format(totalKredit));

			if (!balance) {

				Row row = new Row();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new Label());

				row.appendChild(new MyLabelBoldMerah("TIDAK BALANCE"));

				if (adaRincian) {
					row.appendChild(new Label());
					row.appendChild(new Label());
				}

				row.appendChild(new Label());
				row.appendChild(new Label());
			}

			final DaftarPengajuanTransfer d = ambilDaftarPengajuanTransfer();
			final DisposisiSop disposisiSop = ambilDisposisiSop();

			if (d != null || disposisiSop != null) {
				Row row = new Row();
				row.setValign("top");
				ais.ui.util.ZkCompat.setSpans(row, adaRincian ? "6" : "4");
				row.setParent(rows);
				Vbox vbox = new Vbox();
				vbox.setParent(row);
				vbox.setWidth("99%");

				if (d != null && d.getProsesTransfer() != null) {

					A a = new A(d.getProsesTransfer().getKode());
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							ProsesTransferAction.onAddExternal(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

								}
							}, d.getProsesTransfer());

						}
					});
					a.setStyle("font-size:12px;");
					a.setParent(vbox);
				}

				if (disposisiSop != null) {
					A aa;
					(aa = new A("SOP " + disposisiSop.getKeterangan() + " (" + disposisiSop.getSop().getNama() + ")"))
							.setParent(vbox);

					aa.setStyle("font-size:9px;");
					aa.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							TampilanAlurSopAction.prosess(disposisiSop.getId(), null, null, true, arg0.getTarget());
						}
					});
					aa.setParent(vbox);
				}

			}

			Foot foot = new Foot();
			foot.setParent(grid);

			Footer footer = new Footer("");
			footer.setParent(foot);

			footer = new Footer("Total");
			footer.setParent(foot);

			if (adaRincian) {
				footer = new Footer("");
				footer.setParent(foot);
				footer = new Footer("");
				footer.setParent(foot);
			}

			footer = new Footer(Common.numberFormat.get().format(totalDebet));
			footer.setParent(foot);

			footer = new Footer(Common.numberFormat.get().format(totalKredit));
			footer.setParent(foot);

		}
		return new Object[] { balance, grid };
	}

	/**
	 * Mengumpulkan rincian item Kas Kecil (Keterangan Biaya + Anggaran) untuk grup
	 * transaksi ini, sebagai bahan dua kolom tambahan pada
	 * {@link #populateDeskripsiLengkap()}.
	 *
	 * <p>Sumber data adalah JSON pada {@code KasKecil.getFormula()} — tiap item memuat
	 * {@code nama} (Keterangan Biaya), {@code workspace} (Anggaran), serta {@code akun}
	 * dan {@code jumlah} yang dipakai untuk mencocokkan item ke baris jurnal. Bila
	 * jurnal ini bersumber dari <b>Penggantian Kas Kecil</b>, seluruh Kas Kecil di
	 * bawahnya dibaca. Untuk jurnal jenis lain list kosong dikembalikan sehingga kolom
	 * tambahan tidak ditampilkan sama sekali.</p>
	 *
	 * <p><b>Selalu defensif.</b> Setiap tahap dibungkus {@code try/catch} (dicatat lewat
	 * {@code ErrorAuditUtil}) agar JSON yang rusak, akun yang sudah dihapus, atau
	 * anggaran yang hilang tidak menggagalkan render jurnal — apa yang sudah terkumpul
	 * tetap dikembalikan.</p>
	 *
	 * <p><b>Kasus tepi:</b> {@code akun} dan {@code workspace} diresolusi lewat cache
	 * {@code ConstantValues} yang bersifat global instalasi, bukan lewat session; id yang
	 * tidak ditemukan menghasilkan {@code null}/string kosong, bukan galat.</p>
	 *
	 * @param session session Hibernate yang sedang berjalan ({@code currentSession});
	 *        TIDAK ditutup di sini. Dipakai hanya bila jurnal bersumber dari Penggantian
	 *        Kas Kecil.
	 * @return list {@code Object[]{ Long akunId, Double jumlah, String keteranganBiaya,
	 *         String anggaran }}; kosong (bukan {@code null}) bila jurnal bukan Kas
	 *         Kecil.
	 */
	@SuppressWarnings("unchecked")
	private List<Object[]> kumpulkanRincianKasKecil(Session session) {
		List<Object[]> hasil = new ArrayList<Object[]>();
		try {
			List<KasKecil> kasKecils = new ArrayList<KasKecil>();
			if (getKasKecil() != null) {
				kasKecils.add(getKasKecil());
			} else if (getPenggantianKasKecil() != null) {
				List<KasKecil> anak = session.createCriteria(KasKecil.class)
						.add(Restrictions.eq("penggantianKasKecil", getPenggantianKasKecil())).list();
				if (anak != null) {
					kasKecils.addAll(anak);
				}
			}

			for (int k = 0; k < kasKecils.size(); k++) {
				KasKecil kasKecil = kasKecils.get(k);
				if (kasKecil == null || kasKecil.getFormula() == null) {
					continue;
				}
				org.json.JSONArray array;
				try {
					array = new org.json.JSONArray(kasKecil.getFormula());
				} catch (Exception e) {
					continue;
				}
				for (int i = 0; i < array.length(); i++) {
					try {
						org.json.JSONObject o = array.getJSONObject(i);

						Long akunId = null;
						if (!o.isNull("akun")) {
							Akun a = (Akun) ConstantValues.ambil(Akun.class.getName(),
									ais.common.CommonJSONUtil.ambilLong(o, "akun"));
							akunId = a == null ? null : a.getId();
						}

						Double jumlah = o.isNull("jumlah") ? 0.0 : o.getDouble("jumlah");
						String keteranganBiaya = o.isNull("nama") ? "" : (o.get("nama") + "");

						String anggaran = "";
						if (!o.isNull("workspace")) {
							Workspace w = (Workspace) ConstantValues.ambil(Workspace.class.getName(),
									new java.math.BigDecimal(o.get("workspace") + "").longValue());
							anggaran = w == null ? "" : Common.simpleString(w.getNama());
						}

						hasil.add(new Object[] { akunId, jumlah, keteranganBiaya, anggaran });
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:731");
						// lewati item yang bermasalah, jangan hentikan pengumpulan
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:736");
			// jangan ganggu render jurnal; kembalikan apa yang sudah terkumpul
		}
		return hasil;
	}

	/**
	 * Mencocokkan satu baris jurnal ({@link Transaksi}) ke item rincian Kas Kecil, lalu
	 * <b>MENGONSUMSI</b> item yang cocok agar tidak dipakai baris lain.
	 *
	 * <p>Strategi pencocokan dua tahap: (1) nominal <i>dan</i> akun sama; (2) bila gagal,
	 * nominal saja. Nominal baris jurnal diambil dari sisi debit bila
	 * {@code debet &gt;= 1.0}, selain itu dari sisi kredit, lalu dibulatkan ke bilangan
	 * bulat sebelum dibandingkan — jadi pencocokan bersifat toleran terhadap pecahan.</p>
	 *
	 * <p><b>Efek samping:</b> item yang terpilih di-{@code set(pick, null)} pada list
	 * {@code rincianItems} milik pemanggil, sehingga list itu berubah. Konsekuensinya
	 * urutan pemanggilan menentukan hasil: dua baris jurnal bernominal sama akan
	 * mengambil item sesuai urutan iterasi, bukan berdasarkan makna bisnis.</p>
	 *
	 * @param rincianItems hasil {@link #kumpulkanRincianKasKecil(Session)}; <b>dimodifikasi</b>.
	 * @param transaksi baris jurnal yang sedang dirender.
	 * @return {@code String[]{ keteranganBiaya, anggaran }} — dua string kosong bila
	 *         tidak ada yang cocok (mis. baris kredit Kas Kecil atau jurnal non-item).
	 */
	private String[] cocokkanRincianKasKecil(List<Object[]> rincianItems, Transaksi transaksi) {
		String keteranganBiaya = "";
		String anggaran = "";

		double amount = transaksi.getDebet() != null && transaksi.getDebet() >= 1.0 ? transaksi.getDebet()
				: (transaksi.getKredit() == null ? 0.0 : transaksi.getKredit());
		long amt = Math.round(amount);
		Long akunId = transaksi.getAkun() == null ? null : transaksi.getAkun().getId();

		int pick = -1;
		// 1) cocok akun + nominal
		for (int i = 0; i < rincianItems.size(); i++) {
			Object[] it = rincianItems.get(i);
			if (it == null) {
				continue;
			}
			Long itAkun = (Long) it[0];
			long itJml = Math.round(((Double) it[1]).doubleValue());
			if (itJml == amt && akunId != null && akunId.equals(itAkun)) {
				pick = i;
				break;
			}
		}
		// 2) fallback: cocok nominal saja
		if (pick < 0) {
			for (int i = 0; i < rincianItems.size(); i++) {
				Object[] it = rincianItems.get(i);
				if (it == null) {
					continue;
				}
				long itJml = Math.round(((Double) it[1]).doubleValue());
				if (itJml == amt) {
					pick = i;
					break;
				}
			}
		}

		if (pick >= 0) {
			Object[] it = rincianItems.get(pick);
			keteranganBiaya = (String) it[2];
			anggaran = (String) it[3];
			rincianItems.set(pick, null);
		}

		return new String[] { keteranganBiaya, anggaran };
	}

	/**
	 * Memetakan tiap baris jurnal ({@link Transaksi}) ke item rincian Kas Kecil untuk
	 * keperluan <b>EDIT</b> — yaitu agar layar dapat menyimpan balik nama item
	 * ("Keterangan Biaya") ke JSON {@code KasKecil.formula}.
	 *
	 * <p>Aturan pencocokan sama persis dengan tampilan
	 * ({@link #cocokkanRincianKasKecil(List, Transaksi)}): nominal dengan preferensi akun
	 * yang sama, item dikonsumsi agar tiap baris memetakan ke satu item unik. Baris
	 * bernominal nol dilewati. Method ini HANYA berlaku untuk jurnal Kas Kecil /
	 * Penggantian Kas Kecil; selain itu peta kosong dikembalikan.</p>
	 *
	 * <p>Seluruh badan method dibungkus {@code try/catch} — kegagalan apa pun
	 * menghasilkan peta parsial, bukan galat ke pengguna.</p>
	 *
	 * @param session session Hibernate berjalan; bila {@code null} peta kosong
	 *        dikembalikan.
	 * @param transaksis baris jurnal yang sedang ditampilkan; bila {@code null}/kosong
	 *        peta kosong dikembalikan.
	 * @return peta {@code transaksi.id -> Object[]{ Long kasKecilId, Integer index,
	 *         String nama }}, siap diumpankan ke
	 *         {@link #simpanNamaRincianKasKecil(Session, Long, int, String)}.
	 */
	public java.util.Map<Long, Object[]> petakanRincianKasKecilUntukEdit(Session session, List<Transaksi> transaksis) {
		java.util.Map<Long, Object[]> peta = new java.util.HashMap<Long, Object[]>();
		if (session == null || transaksis == null || transaksis.isEmpty()) {
			return peta;
		}
		try {
			List<KasKecil> kasKecils = new ArrayList<KasKecil>();
			if (getKasKecil() != null) {
				kasKecils.add(getKasKecil());
			} else if (getPenggantianKasKecil() != null) {
				List<KasKecil> anak = session.createCriteria(KasKecil.class)
					.add(Restrictions.eq("penggantianKasKecil", getPenggantianKasKecil())).list();
				if (anak != null) {
					kasKecils.addAll(anak);
				}
			}
			if (kasKecils.isEmpty()) {
				return peta;
			}

			// items: Object[]{ Long kasKecilId, Integer index, Long akunId, Double jumlah, String nama }
			List<Object[]> items = new ArrayList<Object[]>();
			for (int k = 0; k < kasKecils.size(); k++) {
				KasKecil kasKecil = kasKecils.get(k);
				if (kasKecil == null || kasKecil.getFormula() == null) {
					continue;
				}
				org.json.JSONArray array;
				try {
					array = new org.json.JSONArray(kasKecil.getFormula());
				} catch (Exception e) {
					continue;
				}
				for (int i = 0; i < array.length(); i++) {
					try {
						org.json.JSONObject o = array.getJSONObject(i);
						Long akunId = null;
						if (!o.isNull("akun")) {
							Akun a = (Akun) ConstantValues.ambil(Akun.class.getName(),
								ais.common.CommonJSONUtil.ambilLong(o, "akun"));
							akunId = a == null ? null : a.getId();
						}
						Double jumlah = o.isNull("jumlah") ? Double.valueOf(0.0) : Double.valueOf(o.getDouble("jumlah"));
						String nama = o.isNull("nama") ? "" : (o.get("nama") + "");
						items.add(new Object[] { kasKecil.getId(), Integer.valueOf(i), akunId, jumlah, nama });
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:851");
					}
				}
			}
			if (items.isEmpty()) {
				return peta;
			}

			// Cocokkan tiap transaksi (nominal + preferensi akun), konsumsi item.
			for (int t = 0; t < transaksis.size(); t++) {
				Transaksi transaksi = transaksis.get(t);
				if (transaksi == null || transaksi.getId() == null) {
					continue;
				}
				double amount = transaksi.getDebet() != null && transaksi.getDebet() >= 1.0 ? transaksi.getDebet()
					: (transaksi.getKredit() == null ? 0.0 : transaksi.getKredit());
				long amt = Math.round(amount);
				if (amt == 0) {
					continue;
				}
				Long akunId = transaksi.getAkun() == null ? null : transaksi.getAkun().getId();
				int pick = -1;
				for (int i = 0; i < items.size(); i++) {
					Object[] it = items.get(i);
					if (it == null) {
						continue;
					}
					long itJml = Math.round(((Double) it[3]).doubleValue());
					if (itJml == amt && akunId != null && akunId.equals(it[2])) {
						pick = i;
						break;
					}
				}
				if (pick < 0) {
					for (int i = 0; i < items.size(); i++) {
						Object[] it = items.get(i);
						if (it == null) {
							continue;
						}
						long itJml = Math.round(((Double) it[3]).doubleValue());
						if (itJml == amt) {
							pick = i;
							break;
						}
					}
				}
				if (pick >= 0) {
					Object[] it = items.get(pick);
					peta.put(transaksi.getId(), new Object[] { it[0], it[1], it[4] });
					items.set(pick, null);
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:903");
		}
		return peta;
	}

	/**
	 * Menyimpan balik nama item ("Keterangan Biaya") ke JSON
	 * {@code KasKecil.getFormula()} pada indeks tertentu, lalu mem-<i>persist</i> Kas
	 * Kecil tersebut.
	 *
	 * <p>Hanya field {@code nama} pada objek JSON indeks tersebut yang diubah; struktur
	 * dan field lain (akun, jumlah, workspace) dipertahankan apa adanya.</p>
	 *
	 * <p><b>Ini satu-satunya jalur pada kelas ini yang benar-benar MENULIS ke
	 * database</b> ({@code Common.refreshSaveOrUpdate}). Method bersifat {@code static}
	 * dan tidak melakukan pemeriksaan hak akses maupun kepemilikan tenant apa pun: id
	 * Kas Kecil dan indeks item diterima apa adanya dari pemanggil, sehingga
	 * pemeriksaan itu adalah tanggung jawab layar pemanggil.</p>
	 *
	 * <p>Kegagalan tidak dilempar ke atas: exception ditelan (ditampilkan hanya kepada
	 * admin lewat {@code Common.tampilErrorJikaAdmin}) dan {@code false} dikembalikan.</p>
	 *
	 * @param session session Hibernate berjalan; {@code null} → {@code false}.
	 * @param kasKecilId id Kas Kecil yang JSON formulanya akan diubah; {@code null} →
	 *        {@code false}.
	 * @param index indeks item di dalam array JSON; di luar rentang → {@code false}.
	 * @param namaBaru nama/Keterangan Biaya baru; {@code null} disimpan sebagai string
	 *        kosong.
	 * @return {@code true} bila perubahan berhasil disimpan.
	 */
	public static boolean simpanNamaRincianKasKecil(Session session, Long kasKecilId, int index, String namaBaru) {
		if (session == null || kasKecilId == null) {
			return false;
		}
		try {
			KasKecil kasKecil = (KasKecil) session.get(KasKecil.class, kasKecilId);
			if (kasKecil == null || kasKecil.getFormula() == null) {
				return false;
			}
			org.json.JSONArray array = new org.json.JSONArray(kasKecil.getFormula());
			if (index < 0 || index >= array.length()) {
				return false;
			}
			org.json.JSONObject o = array.getJSONObject(index);
			o.put("nama", namaBaru == null ? "" : namaBaru);
			array.put(index, o);
			kasKecil.setFormula(array.toString());
			Common.refreshSaveOrUpdate(session, kasKecil);
			return true;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return false;
		}
	}


	/**
	 * Membentuk <b>pratinjau</b> jurnal pembayaran mahasiswa dari riwayat cicilan
	 * dan/atau daftar tagihan yang sedang tampil di layar.
	 *
	 * <p>Bila ada cicilan: untuk setiap cicilan bernilai berarti
	 * ({@code |nilai| &gt; 0.1}) dibentuk satu baris <b>debit</b> pada akun cara
	 * pembayaran ({@link #ambilAkunPembayaran(CicilanPembayaran)}) dan baris
	 * <b>kredit</b> pada akun tagihan/piutang/pendapatan item biaya
	 * ({@link #ambilAkunTagihanPembayaran(ItemBiaya, Kegiatan, JurnalPembayaranAkunCache)}).
	 * Bila cicilan memuat denda, pokok dipisahkan dari denda
	 * ({@link #hitungNilaiPokokPembayaran(CicilanPembayaran, double, double)}) dan denda
	 * dikreditkan ke akun pendapatan denda — bila akun denda belum diatur, dipakai akun
	 * tagihan utama disertai peringatan.</p>
	 *
	 * <p>Bila belum ada cicilan sama sekali, pratinjau hanya menampilkan sisi kredit
	 * (gambaran tagihan) yang disusun dari pengaturan pembayaran bulanan bila ada, atau
	 * dari detail biaya.</p>
	 *
	 * <p>Setiap akun yang belum terkonfigurasi tidak menggagalkan pratinjau: barisnya
	 * dilewati dan sebuah peringatan berbahasa manusia ditambahkan (deduplikasi lewat
	 * {@link #addWarning(StringBuffer, String)}), lalu ditampilkan sebagai blok merah di
	 * bawah tabel.</p>
	 *
	 * <p><b>Method ini murni tampilan</b> — tidak menyimpan {@code GrupTransaksi} maupun
	 * {@link Transaksi} apa pun, tidak menyentuh {@code postingHistory}, dan tidak
	 * membuka transaksi database. Pencarian akun dipercepat dengan
	 * {@link JurnalPembayaranAkunCache} sehingga item biaya yang sama tidak dicari
	 * berulang.</p>
	 *
	 * @param cicilanPembayarans riwayat cicilan; boleh {@code null}/kosong.
	 * @param detailBiayas detail biaya yang sedang tampil, dipakai sebagai sumber
	 *        cadangan item biaya dan pratinjau tagihan; boleh {@code null}.
	 * @param pengaturanPembayaranBulanans pengaturan pembayaran bulanan; boleh
	 *        {@code null}.
	 * @param kegiatan kegiatan/angkatan yang menentukan tarif dan pemetaan akun; boleh
	 *        {@code null} (akan diambil dari cicilan).
	 * @return {@link Grid} ZK berisi tabel Akun/Debet/Kredit, baris TOTAL, penanda "TIDAK
	 *         BALANCE" bila perlu, dan blok peringatan.
	 */
	@SuppressWarnings({ "rawtypes" })
	public static Grid tampilkanJurnalPembayaranMahasiswa(List<CicilanPembayaran> cicilanPembayarans,
			List<DetailBiaya> detailBiayas, List<PengaturanPembayaranBulanan> pengaturanPembayaranBulanans,
			Kegiatan kegiatan) {
		List<Akun> akunsDebets = new ArrayList<Akun>();
		List<Double> nilaiDebets = new ArrayList<Double>();
		List<Akun> akunsKredits = new ArrayList<Akun>();
		List<Double> nilaiKredits = new ArrayList<Double>();
		StringBuffer warnings = new StringBuffer();
		JurnalPembayaranAkunCache cache = new JurnalPembayaranAkunCache();

		boolean adaCicilan = cicilanPembayarans != null && cicilanPembayarans.size() > 0;
		if (adaCicilan) {
			for (int i = 0; i < cicilanPembayarans.size(); i++) {
				CicilanPembayaran cicilanPembayaran = cicilanPembayarans.get(i);
				if (cicilanPembayaran == null) {
					continue;
				}

				Kegiatan kegiatanBayar = ambilKegiatanPembayaran(cicilanPembayaran, kegiatan);
				ItemBiaya itemBiaya = ambilItemBiayaPembayaran(cicilanPembayaran, detailBiayas,
						pengaturanPembayaranBulanans);
				double nilaiBayar = safeDouble(cicilanPembayaran.getNilai());
				if (Math.abs(nilaiBayar) <= 0.1) {
					continue;
				}

				Akun akunPembayaran = ambilAkunPembayaran(cicilanPembayaran);
				tambahAkunNilai(akunsDebets, nilaiDebets, akunPembayaran, nilaiBayar, warnings,
						"Akun pembayaran belum diatur untuk " + labelCicilanPembayaran(cicilanPembayaran));

				double nilaiDenda = safeDouble(cicilanPembayaran.getDenda());
				double nilaiPokok = hitungNilaiPokokPembayaran(cicilanPembayaran, nilaiBayar, nilaiDenda);

				Akun akunTagihan = ambilAkunTagihanPembayaran(itemBiaya, kegiatanBayar, cache);
				tambahAkunNilai(akunsKredits, nilaiKredits, akunTagihan, nilaiPokok, warnings,
						"Akun tagihan/piutang/pendapatan belum diatur untuk " + labelItemBiaya(itemBiaya));

				if (nilaiDenda > 0.1) {
					Akun akunDenda = ambilAkunPendapatanDenda(itemBiaya, kegiatanBayar, cache);
					if (akunDenda == null) {
						akunDenda = akunTagihan;
						addWarning(warnings, "Akun pendapatan denda belum diatur untuk " + labelItemBiaya(itemBiaya)
								+ ", sehingga sementara memakai akun tagihan utama.");
					}
					tambahAkunNilai(akunsKredits, nilaiKredits, akunDenda, nilaiDenda, warnings,
							"Akun pendapatan denda belum diatur untuk " + labelItemBiaya(itemBiaya));
				}
			}
		} else {
			addWarning(warnings,
					"Belum ada data pembayaran. Jurnal menampilkan akun tagihan sebagai gambaran awal; akun debet akan lengkap setelah cara bayar dipilih/disimpan.");
			if (pengaturanPembayaranBulanans != null && pengaturanPembayaranBulanans.size() > 0) {
				tambahPreviewJurnalDariPengaturanBulanan(akunsKredits, nilaiKredits, pengaturanPembayaranBulanans,
						kegiatan, warnings, cache);
			} else {
				tambahPreviewJurnalDariDetailBiaya(akunsKredits, nilaiKredits, detailBiayas, kegiatan, warnings, cache);
			}
		}

		if (akunsDebets.size() == 0 && akunsKredits.size() == 0) {
			addWarning(warnings,
					"Akun jurnal pembayaran belum bisa ditampilkan karena data cicilan/detail biaya belum tersedia atau akun belum dikonfigurasi.");
		}
		return tampilkanJurnal(akunsDebets, nilaiDebets, akunsKredits, nilaiKredits, warnings.toString());
	}

	/**
	 * Pintasan {@link #tampilkanJurnalPembayaranMahasiswa(List, List, List, Kegiatan)}
	 * tanpa daftar detail biaya maupun pengaturan bulanan.
	 *
	 * @param cicilanPembayarans riwayat cicilan; boleh {@code null}/kosong.
	 * @param kegiatan kegiatan/angkatan penentu tarif dan pemetaan akun.
	 * @return {@link Grid} pratinjau jurnal.
	 */
	public static Grid tampilkanJurnalPembayaranMahasiswa(List<CicilanPembayaran> cicilanPembayarans,
			Kegiatan kegiatan) {
		return tampilkanJurnalPembayaranMahasiswa(cicilanPembayarans, null, null, kegiatan);
	}

	/**
	 * Membentuk <b>pratinjau</b> jurnal pembayaran siswa dari daftar tagihan yang dipilih
	 * pada layar Pembayaran Online.
	 *
	 * <p>Untuk setiap tagihan dihitung {@code pokokSetelahDiskon = nominal - diskon}
	 * (tidak pernah negatif) lalu {@code nilaiTagihan = pokokSetelahDiskon + denda}.
	 * Tagihan dengan nilai {@code <= 0.1} dilewati. Pokok dikreditkan ke akun
	 * piutang/dibayar-dimuka/pendapatan item biaya sekolah
	 * ({@link #ambilAkunTagihanPembayaranSiswa(ItemBiayaSekolah)}), denda dikreditkan ke
	 * akun denda ({@link #ambilAkunDendaPembayaranSiswa(ItemBiayaSekolah)}) — bila akun
	 * denda kosong dipakai akun tagihan utama disertai peringatan.</p>
	 *
	 * <p>Sisi debit diisi satu baris berisi TOTAL seluruh tagihan pada akun cara
	 * pembayaran, atau pada akun deposit/tabungan bila {@code gunakanAkunDeposit}
	 * bernilai {@code true}. Bila ada tambahan setoran tabungan
	 * ({@code tambahanDeposit &gt; 0.1}) ditambahkan sepasang baris: debit ke akun kas
	 * dan kredit ke akun deposit.</p>
	 *
	 * <p><b>Method ini murni tampilan</b> — tidak menyimpan jurnal apa pun. Seluruh
	 * pembacaan properti tagihan dibungkus {@code try/catch}
	 * ({@link #safeDoubleTagihan(Tagihan, String)}) sehingga tagihan rusak tidak
	 * menggagalkan layar pembayaran.</p>
	 *
	 * @param tagihans daftar tagihan yang dipilih; boleh {@code null}.
	 * @param akunPembayaranSiswa konfigurasi akun cara pembayaran/deposit siswa; boleh
	 *        {@code null} (menghasilkan peringatan, bukan galat).
	 * @param gunakanAkunDeposit {@code true} bila pembayaran diambil dari saldo
	 *        deposit/tabungan siswa.
	 * @param tambahanDeposit nominal setoran tabungan tambahan; boleh {@code null}.
	 * @return {@link Grid} pratinjau jurnal pembayaran siswa.
	 */
	@SuppressWarnings({ "rawtypes" })
	public static Grid tampilkanJurnalPembayaranSiswa(List<Tagihan> tagihans,
			AkunPembayaranSiswa akunPembayaranSiswa, boolean gunakanAkunDeposit, Double tambahanDeposit) {
		List<Akun> akunsDebets = new ArrayList<Akun>();
		List<Double> nilaiDebets = new ArrayList<Double>();
		List<Akun> akunsKredits = new ArrayList<Akun>();
		List<Double> nilaiKredits = new ArrayList<Double>();
		StringBuffer warnings = new StringBuffer();

		double totalPembayaran = 0.0;
		if (tagihans != null) {
			for (int i = 0; i < tagihans.size(); i++) {
				Tagihan tagihan = tagihans.get(i);
				if (tagihan == null) {
					continue;
				}
				ItemBiayaSekolah itemBiayaSekolah = ambilItemBiayaSekolah(tagihan);
				double nominal = safeDoubleTagihan(tagihan, "nominal");
				double denda = safeDoubleTagihan(tagihan, "denda");
				double diskon = safeDoubleTagihan(tagihan, "diskon");
				double pokokSetelahDiskon = nominal - diskon;
				if (pokokSetelahDiskon < 0.0) {
					pokokSetelahDiskon = 0.0;
				}
				double nilaiTagihan = pokokSetelahDiskon + denda;
				if (Math.abs(nilaiTagihan) <= 0.1) {
					continue;
				}

				Akun akunTagihan = ambilAkunTagihanPembayaranSiswa(itemBiayaSekolah);
				tambahAkunNilai(akunsKredits, nilaiKredits, akunTagihan, pokokSetelahDiskon, warnings,
						"Akun tagihan/piutang/pendapatan belum diatur untuk "
								+ labelItemBiayaSekolah(itemBiayaSekolah));

				if (denda > 0.1) {
					Akun akunDenda = ambilAkunDendaPembayaranSiswa(itemBiayaSekolah);
					if (akunDenda == null) {
						akunDenda = akunTagihan;
						addWarning(warnings, "Akun denda belum diatur untuk "
								+ labelItemBiayaSekolah(itemBiayaSekolah)
								+ ", sehingga sementara memakai akun tagihan utama.");
					}
					tambahAkunNilai(akunsKredits, nilaiKredits, akunDenda, denda, warnings,
							"Akun denda belum diatur untuk " + labelItemBiayaSekolah(itemBiayaSekolah));
				}
				totalPembayaran += nilaiTagihan;
			}
		}

		double nilaiTambahanDeposit = safeDouble(tambahanDeposit);
		Akun akunPembayaran = ambilAkunPembayaranSiswa(akunPembayaranSiswa, gunakanAkunDeposit);
		String labelAkunPembayaran = gunakanAkunDeposit ? "Akun deposit/tabungan siswa belum diatur"
				: "Akun cara pembayaran siswa belum diatur";
		tambahAkunNilai(akunsDebets, nilaiDebets, akunPembayaran, totalPembayaran, warnings, labelAkunPembayaran);

		if (nilaiTambahanDeposit > 0.1) {
			Akun akunKas = ambilAkunPembayaranSiswa(akunPembayaranSiswa, false);
			Akun akunDeposit = ambilAkunDepositSiswa(akunPembayaranSiswa);
			tambahAkunNilai(akunsDebets, nilaiDebets, akunKas, nilaiTambahanDeposit, warnings,
					"Akun cara pembayaran siswa belum diatur untuk tambahan tabungan/deposit.");
			tambahAkunNilai(akunsKredits, nilaiKredits, akunDeposit, nilaiTambahanDeposit, warnings,
					"Akun deposit/tabungan siswa belum diatur untuk tambahan tabungan/deposit.");
		}

		if (akunsDebets.size() == 0 && akunsKredits.size() == 0) {
			addWarning(warnings,
					"Jurnal pembayaran siswa belum bisa ditampilkan karena tagihan atau konfigurasi akun belum tersedia.");
		}
		return tampilkanJurnal(akunsDebets, nilaiDebets, akunsKredits, nilaiKredits, warnings.toString());
	}

	/**
	 * Membaca item biaya sekolah dari sebuah tagihan secara aman.
	 *
	 * @param tagihan tagihan sumber; boleh {@code null}.
	 * @return item biaya sekolah, atau {@code null} bila tagihan {@code null} atau
	 *         relasinya gagal diresolusi (proxy detached).
	 */
	private static ItemBiayaSekolah ambilItemBiayaSekolah(Tagihan tagihan) {
		try {
			return tagihan == null ? null : tagihan.getItemBiayaSekolah();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Memilih akun sisi debit untuk pembayaran siswa.
	 *
	 * @param akunPembayaranSiswa konfigurasi cara pembayaran siswa; boleh {@code null}.
	 * @param gunakanAkunDeposit bila {@code true}, akun deposit diutamakan dan hanya
	 *        jatuh ke akun kas bila akun deposit belum diatur.
	 * @return akun terpilih, atau {@code null} bila konfigurasi belum tersedia.
	 */
	private static Akun ambilAkunPembayaranSiswa(AkunPembayaranSiswa akunPembayaranSiswa, boolean gunakanAkunDeposit) {
		try {
			if (akunPembayaranSiswa == null) {
				return null;
			}
			if (gunakanAkunDeposit) {
				Akun akunDeposit = akunPembayaranSiswa.getAkunDeposit();
				if (akunDeposit != null) {
					return akunDeposit;
				}
			}
			return akunPembayaranSiswa.getAkun();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Membaca akun deposit/tabungan siswa secara aman.
	 *
	 * @param akunPembayaranSiswa konfigurasi cara pembayaran siswa; boleh {@code null}.
	 * @return akun deposit, atau {@code null}.
	 */
	private static Akun ambilAkunDepositSiswa(AkunPembayaranSiswa akunPembayaranSiswa) {
		try {
			return akunPembayaranSiswa == null ? null : akunPembayaranSiswa.getAkunDeposit();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Menentukan akun lawan (sisi kredit) untuk pokok tagihan siswa, dengan urutan
	 * prioritas: <b>piutang</b> → <b>dibayar dimuka</b> → <b>akun pendapatan</b> item
	 * biaya.
	 *
	 * <p>Urutan ini mencerminkan kaidah pembukuan modul tagihan siswa: bila tagihan sudah
	 * pernah diakui sebagai piutang, pembayaran menutup piutang; bila belum, pembayaran
	 * dicatat sebagai pendapatan diterima dimuka; barulah terakhir langsung ke
	 * pendapatan.</p>
	 *
	 * @param itemBiayaSekolah item biaya sekolah dari tagihan; boleh {@code null}.
	 * @return akun terpilih, atau {@code null} bila tidak satu pun terkonfigurasi.
	 */
	private static Akun ambilAkunTagihanPembayaranSiswa(ItemBiayaSekolah itemBiayaSekolah) {
		if (itemBiayaSekolah == null) {
			return null;
		}
		try {
			Akun akun = itemBiayaSekolah.getAkunPiutang();
			if (akun != null) {
				return akun;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:1135");
		}
		try {
			Akun akun = itemBiayaSekolah.getAkunDibayarDimuka();
			if (akun != null) {
				return akun;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:1142");
		}
		try {
			return itemBiayaSekolah.getAkun();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Menentukan akun denda tagihan siswa: akun denda lebih dulu, lalu akun piutang
	 * denda.
	 *
	 * @param itemBiayaSekolah item biaya sekolah dari tagihan; boleh {@code null}.
	 * @return akun denda terpilih, atau {@code null}.
	 */
	private static Akun ambilAkunDendaPembayaranSiswa(ItemBiayaSekolah itemBiayaSekolah) {
		if (itemBiayaSekolah == null) {
			return null;
		}
		try {
			Akun akun = itemBiayaSekolah.getAkunDenda();
			if (akun != null) {
				return akun;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:1160");
		}
		try {
			return itemBiayaSekolah.getAkunPiutangDenda();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Membaca satu properti nominal dari tagihan berdasarkan namanya, secara aman.
	 *
	 * <p>Dipakai agar pratinjau tidak perlu menulis {@code try/catch} berulang untuk tiap
	 * properti. Nama properti yang tidak dikenal menghasilkan {@code 0.0}, bukan galat.</p>
	 *
	 * @param tagihan tagihan sumber; boleh {@code null}.
	 * @param property salah satu dari {@code "nominal"}, {@code "denda"},
	 *        {@code "diskon"}.
	 * @return nilai properti, atau {@code 0.0} bila tidak tersedia.
	 */
	private static double safeDoubleTagihan(Tagihan tagihan, String property) {
		try {
			if (tagihan == null || property == null) {
				return 0.0;
			}
			if ("nominal".equals(property)) {
				return safeDouble(tagihan.getNominal());
			}
			if ("denda".equals(property)) {
				return safeDouble(tagihan.getDenda());
			}
			if ("diskon".equals(property)) {
				return safeDouble(tagihan.getDiskon());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:1183");
		}
		return 0.0;
	}

	/**
	 * Menyusun label item biaya sekolah untuk pesan peringatan: {@code "kode - nama"},
	 * lalu {@code "nama"} saja, lalu teks umum.
	 *
	 * @param itemBiayaSekolah item biaya sekolah; boleh {@code null}.
	 * @return label yang selalu layak tampil, tidak pernah {@code null}.
	 */
	private static String labelItemBiayaSekolah(ItemBiayaSekolah itemBiayaSekolah) {
		try {
			if (itemBiayaSekolah != null && itemBiayaSekolah.getKode() != null && itemBiayaSekolah.getNama() != null) {
				return itemBiayaSekolah.getKode() + " - " + itemBiayaSekolah.getNama();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:1193");
		}
		try {
			if (itemBiayaSekolah != null && itemBiayaSekolah.getNama() != null) {
				return itemBiayaSekolah.getNama();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:1199");
		}
		return "item biaya sekolah";
	}

	/**
	 * Memisahkan nilai <b>pokok</b> dari nilai bayar yang sudah termasuk denda.
	 *
	 * <p>Urutan penentuan: (1) bila tidak ada denda, seluruh nilai bayar adalah pokok;
	 * (2) bila {@code nilaiAsli} cicilan tersedia dan {@code nilaiAsli + denda} cocok
	 * dengan nilai bayar dalam toleransi 1.0, dipakai {@code nilaiAsli}; (3) bila tidak,
	 * denda dikurangkan dari nilai bayar <b>dengan mempertahankan tanda</b> nilai bayar
	 * (mengikuti idiom tanda Dr/Cr AIS); (4) bila denda ternyata lebih besar dari nilai
	 * bayar, nilai bayar dikembalikan apa adanya agar hasil tidak berbalik tanda.
	 *
	 * @param cicilanPembayaran cicilan sumber; boleh {@code null}.
	 * @param nilaiBayar nilai bayar total (pokok + denda), bisa negatif.
	 * @param nilaiDenda nilai denda; {@code <= 0.1} dianggap tidak ada denda.
	 * @return nilai pokok pembayaran.
	 */
	private static double hitungNilaiPokokPembayaran(CicilanPembayaran cicilanPembayaran, double nilaiBayar,
			double nilaiDenda) {
		if (nilaiDenda <= 0.1) {
			return nilaiBayar;
		}
		double nilaiAsli = 0.0;
		try {
			nilaiAsli = safeDouble(cicilanPembayaran == null ? null : cicilanPembayaran.getNilaiAsli());
		} catch (Exception e) {
			nilaiAsli = 0.0;
		}
		if (nilaiAsli > 0.1 && Math.abs((nilaiAsli + nilaiDenda) - nilaiBayar) <= 1.0) {
			return nilaiAsli;
		}
		if (Math.abs(nilaiBayar) > nilaiDenda) {
			return nilaiBayar > 0.0 ? nilaiBayar - nilaiDenda : nilaiBayar + nilaiDenda;
		}
		return nilaiBayar;
	}

	/**
	 * Menambahkan baris pratinjau sisi <b>kredit</b> yang disusun dari pengaturan
	 * pembayaran bulanan (dipakai ketika belum ada satu pun cicilan).
	 *
	 * <p>Nominal diambil dari pengaturan bulanan; bila nol, dihitung dari detail biaya
	 * lewat {@link #hitungNilaiDetailBiayaUntukJurnal(DetailBiaya, Kegiatan)}.</p>
	 *
	 * @param akunsKredits list akun kredit yang diisi; <b>dimodifikasi</b>.
	 * @param nilaiKredits list nilai kredit sejajar indeks; <b>dimodifikasi</b>.
	 * @param pengaturanPembayaranBulanans sumber data; boleh {@code null}.
	 * @param kegiatan kegiatan penentu tarif/akun.
	 * @param warnings penampung peringatan; <b>dimodifikasi</b>.
	 * @param cache cache pencarian akun; boleh {@code null}.
	 */
	private static void tambahPreviewJurnalDariPengaturanBulanan(List<Akun> akunsKredits, List<Double> nilaiKredits,
			List<PengaturanPembayaranBulanan> pengaturanPembayaranBulanans, Kegiatan kegiatan, StringBuffer warnings,
			JurnalPembayaranAkunCache cache) {
		if (pengaturanPembayaranBulanans == null) {
			return;
		}
		for (int i = 0; i < pengaturanPembayaranBulanans.size(); i++) {
			PengaturanPembayaranBulanan bulanan = pengaturanPembayaranBulanans.get(i);
			if (bulanan == null) {
				continue;
			}
			DetailBiaya detailBiaya = null;
			try {
				detailBiaya = bulanan.getDetailBiaya();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:1238");
			}
			ItemBiaya itemBiaya = null;
			try {
				itemBiaya = detailBiaya == null ? null : detailBiaya.getItemBiaya();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:1243");
			}
			double nilai = safeDouble(bulanan.getNominal());
			if (Math.abs(nilai) <= 0.1 && detailBiaya != null) {
				nilai = hitungNilaiDetailBiayaUntukJurnal(detailBiaya, kegiatan);
			}
			Akun akun = ambilAkunTagihanPembayaran(itemBiaya, kegiatan, cache);
			tambahAkunNilai(akunsKredits, nilaiKredits, akun, nilai, warnings,
					"Akun tagihan/piutang/pendapatan belum diatur untuk " + labelItemBiaya(itemBiaya));
		}
	}

	/**
	 * Menambahkan baris pratinjau sisi <b>kredit</b> yang disusun dari daftar detail
	 * biaya (jalur cadangan ketika pengaturan bulanan tidak tersedia).
	 *
	 * @param akunsKredits list akun kredit yang diisi; <b>dimodifikasi</b>.
	 * @param nilaiKredits list nilai kredit sejajar indeks; <b>dimodifikasi</b>.
	 * @param detailBiayas sumber data; boleh {@code null}.
	 * @param kegiatan kegiatan penentu tarif/akun.
	 * @param warnings penampung peringatan; <b>dimodifikasi</b>.
	 * @param cache cache pencarian akun; boleh {@code null}.
	 */
	private static void tambahPreviewJurnalDariDetailBiaya(List<Akun> akunsKredits, List<Double> nilaiKredits,
			List<DetailBiaya> detailBiayas, Kegiatan kegiatan, StringBuffer warnings, JurnalPembayaranAkunCache cache) {
		if (detailBiayas == null) {
			return;
		}
		for (int i = 0; i < detailBiayas.size(); i++) {
			DetailBiaya detailBiaya = detailBiayas.get(i);
			if (detailBiaya == null) {
				continue;
			}
			ItemBiaya itemBiaya = null;
			try {
				itemBiaya = detailBiaya.getItemBiaya();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:1268");
			}
			double nilai = hitungNilaiDetailBiayaUntukJurnal(detailBiaya, kegiatan);
			Akun akun = ambilAkunTagihanPembayaran(itemBiaya, kegiatan, cache);
			tambahAkunNilai(akunsKredits, nilaiKredits, akun, nilai, warnings,
					"Akun tagihan/piutang/pendapatan belum diatur untuk " + labelItemBiaya(itemBiaya));
		}
	}

	/**
	 * Menentukan kegiatan/angkatan yang dipakai untuk memetakan akun: kegiatan yang
	 * dikirim layar selalu menang, baru kegiatan milik cicilan.
	 *
	 * @param cicilanPembayaran cicilan sumber; boleh {@code null}.
	 * @param kegiatanDefault kegiatan dari layar; boleh {@code null}.
	 * @return kegiatan terpilih, atau {@code null}.
	 */
	private static Kegiatan ambilKegiatanPembayaran(CicilanPembayaran cicilanPembayaran, Kegiatan kegiatanDefault) {
		if (kegiatanDefault != null) {
			return kegiatanDefault;
		}
		try {
			return cicilanPembayaran == null ? null : cicilanPembayaran.getKegiatan();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Mencari item biaya milik sebuah cicilan lewat empat jalur berurutan: relasi
	 * langsung, detail biaya, pengaturan pembayaran bulanan, lalu pencocokan terhadap
	 * daftar yang sedang tampil di layar.
	 *
	 * <p>Diperlukan karena cicilan lama tidak selalu menyimpan relasi item biaya secara
	 * langsung; tanpa item biaya, akun lawan jurnal tidak bisa ditentukan.</p>
	 *
	 * @param cicilanPembayaran cicilan sumber; boleh {@code null}.
	 * @param detailBiayas daftar detail biaya di layar; boleh {@code null}.
	 * @param pengaturanPembayaranBulanans daftar pengaturan bulanan di layar; boleh
	 *        {@code null}.
	 * @return item biaya, atau {@code null} bila seluruh jalur gagal.
	 */
	private static ItemBiaya ambilItemBiayaPembayaran(CicilanPembayaran cicilanPembayaran, List<DetailBiaya> detailBiayas,
			List<PengaturanPembayaranBulanan> pengaturanPembayaranBulanans) {
		ItemBiaya itemBiaya = null;
		try {
			itemBiaya = cicilanPembayaran == null ? null : cicilanPembayaran.getItemBiaya();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:1293");
		}
		if (itemBiaya != null) {
			return itemBiaya;
		}
		try {
			DetailBiaya detailBiaya = cicilanPembayaran == null ? null : cicilanPembayaran.getDetailBiaya();
			if (detailBiaya != null && detailBiaya.getItemBiaya() != null) {
				return detailBiaya.getItemBiaya();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:1303");
		}
		try {
			PengaturanPembayaranBulanan bulanan = cicilanPembayaran == null ? null
					: cicilanPembayaran.getPengaturanPembayaranBulanan();
			if (bulanan != null && bulanan.getDetailBiaya() != null && bulanan.getDetailBiaya().getItemBiaya() != null) {
				return bulanan.getDetailBiaya().getItemBiaya();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:1311");
		}
		return ambilItemBiayaDariDaftar(cicilanPembayaran, detailBiayas, pengaturanPembayaranBulanans);
	}

	/**
	 * Jalur terakhir {@link #ambilItemBiayaPembayaran(CicilanPembayaran, List, List)}:
	 * mencocokkan cicilan ke daftar yang sedang tampil berdasarkan id item biaya dan
	 * urutan angsuran ({@code bayarKe}).
	 *
	 * @param cicilanPembayaran cicilan sumber; boleh {@code null}.
	 * @param detailBiayas daftar detail biaya; boleh {@code null}.
	 * @param pengaturanPembayaranBulanans daftar pengaturan bulanan; boleh {@code null}.
	 * @return item biaya hasil pencocokan, atau {@code null}.
	 */
	private static ItemBiaya ambilItemBiayaDariDaftar(CicilanPembayaran cicilanPembayaran, List<DetailBiaya> detailBiayas,
			List<PengaturanPembayaranBulanan> pengaturanPembayaranBulanans) {
		Long idItem = null;
		Integer bayarKe = null;
		try {
			idItem = cicilanPembayaran != null && cicilanPembayaran.getItemBiaya() != null
					? cicilanPembayaran.getItemBiaya().getId()
					: null;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:1324");
		}
		try {
			bayarKe = cicilanPembayaran == null ? null : cicilanPembayaran.getBayarKe();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:1328");
		}
		ItemBiaya hasil = ambilItemBiayaDariDetailBiaya(idItem, bayarKe, detailBiayas);
		if (hasil != null) {
			return hasil;
		}
		return ambilItemBiayaDariPengaturanBulanan(idItem, bayarKe, pengaturanPembayaranBulanans);
	}

	/**
	 * Mencocokkan id item biaya + urutan angsuran ke daftar detail biaya.
	 *
	 * <p><b>Kasus tepi:</b> {@code null} pada {@code idItem} atau {@code bayarKe}
	 * diperlakukan sebagai "cocok dengan apa pun", sehingga entri pertama yang layak akan
	 * terpilih. Ini disengaja agar pratinjau tetap menampilkan sesuatu, tetapi berarti
	 * hasilnya bisa tidak tepat untuk cicilan yang datanya tidak lengkap.</p>
	 *
	 * @param idItem id item biaya yang dicari; boleh {@code null}.
	 * @param bayarKe urutan angsuran; boleh {@code null}.
	 * @param detailBiayas daftar detail biaya; boleh {@code null}.
	 * @return item biaya yang cocok, atau {@code null}.
	 */
	private static ItemBiaya ambilItemBiayaDariDetailBiaya(Long idItem, Integer bayarKe, List<DetailBiaya> detailBiayas) {
		if (detailBiayas == null) {
			return null;
		}
		for (int i = 0; i < detailBiayas.size(); i++) {
			try {
				DetailBiaya detailBiaya = detailBiayas.get(i);
				if (detailBiaya == null || detailBiaya.getItemBiaya() == null) {
					continue;
				}
				boolean samaItem = idItem == null || idItem.equals(detailBiaya.getItemBiaya().getId());
				boolean samaBayarKe = bayarKe == null || detailBiaya.getBayarKe() == null
						|| bayarKe.equals(detailBiaya.getBayarKe());
				if (samaItem && samaBayarKe) {
					return detailBiaya.getItemBiaya();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:1353");
			}
		}
		return null;
	}

	/**
	 * Varian {@link #ambilItemBiayaDariDetailBiaya(Long, Integer, List)} yang menelusuri
	 * daftar pengaturan pembayaran bulanan. Kaidah pencocokan dan kasus tepinya identik.
	 *
	 * @param idItem id item biaya yang dicari; boleh {@code null}.
	 * @param bayarKe urutan angsuran; boleh {@code null}.
	 * @param pengaturanPembayaranBulanans daftar pengaturan bulanan; boleh {@code null}.
	 * @return item biaya yang cocok, atau {@code null}.
	 */
	private static ItemBiaya ambilItemBiayaDariPengaturanBulanan(Long idItem, Integer bayarKe,
			List<PengaturanPembayaranBulanan> pengaturanPembayaranBulanans) {
		if (pengaturanPembayaranBulanans == null) {
			return null;
		}
		for (int i = 0; i < pengaturanPembayaranBulanans.size(); i++) {
			try {
				PengaturanPembayaranBulanan bulanan = pengaturanPembayaranBulanans.get(i);
				if (bulanan == null || bulanan.getDetailBiaya() == null
						|| bulanan.getDetailBiaya().getItemBiaya() == null) {
					continue;
				}
				boolean samaItem = idItem == null || idItem.equals(bulanan.getDetailBiaya().getItemBiaya().getId());
				boolean samaBayarKe = bayarKe == null || bulanan.getDetailBiaya().getBayarKe() == null
						|| bayarKe.equals(bulanan.getDetailBiaya().getBayarKe());
				if (samaItem && samaBayarKe) {
					return bulanan.getDetailBiaya().getItemBiaya();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:1377");
			}
		}
		return null;
	}

	/**
	 * Menentukan akun sisi <b>debit</b> pembayaran mahasiswa: akun jenis tabungan lebih
	 * dulu (pembayaran diambil dari tabungan), baru akun jenis pembayaran (kas/bank).
	 *
	 * @param cicilanPembayaran cicilan sumber; boleh {@code null}.
	 * @return akun terpilih, atau {@code null} bila belum dikonfigurasi.
	 */
	private static Akun ambilAkunPembayaran(CicilanPembayaran cicilanPembayaran) {
		Akun akun = null;
		try {
			JenisPembayaran jenisTabungan = cicilanPembayaran == null ? null : cicilanPembayaran.getJenisTabungan();
			if (jenisTabungan != null && jenisTabungan.getAkun() != null) {
				akun = jenisTabungan.getAkun();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:1390");
		}
		if (akun != null) {
			return akun;
		}
		try {
			JenisPembayaran jenisPembayaran = cicilanPembayaran == null ? null : cicilanPembayaran.getJenisPembayaran();
			if (jenisPembayaran != null) {
				akun = jenisPembayaran.getAkun();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:1400");
		}
		return akun;
	}

	/**
	 * Pintasan {@link #ambilAkunTagihanPembayaran(ItemBiaya, Kegiatan,
	 * JurnalPembayaranAkunCache)} tanpa cache.
	 *
	 * @param itemBiaya item biaya; boleh {@code null}.
	 * @param kegiatan kegiatan penentu pemetaan akun.
	 * @return akun lawan, atau {@code null}.
	 */
	private static Akun ambilAkunTagihanPembayaran(ItemBiaya itemBiaya, Kegiatan kegiatan) {
		return ambilAkunTagihanPembayaran(itemBiaya, kegiatan, null);
	}

	/**
	 * Menentukan akun lawan (sisi kredit) pembayaran mahasiswa dengan urutan prioritas
	 * <b>piutang</b> → <b>dibayar dimuka</b> → <b>pendapatan</b>, seluruhnya diambil per
	 * kegiatan lewat {@code ItemBiaya.ambilPiutang/ambilDibayarDimuka/ambilAkun}.
	 *
	 * <p>Setiap tahap dilayani cache {@link JurnalPembayaranAkunCache} agar item biaya
	 * yang berulang di banyak cicilan tidak memicu pencarian berulang. Kegagalan
	 * pencarian menghasilkan {@code null} (tahap berikutnya dicoba), bukan exception.</p>
	 *
	 * @param itemBiaya item biaya; {@code null} → {@code null}.
	 * @param kegiatan kegiatan penentu pemetaan akun; boleh {@code null}.
	 * @param cache cache pencarian akun; boleh {@code null} (pencarian tanpa cache).
	 * @return akun lawan terpilih, atau {@code null} bila tidak satu pun terkonfigurasi.
	 */
	private static Akun ambilAkunTagihanPembayaran(ItemBiaya itemBiaya, Kegiatan kegiatan,
			JurnalPembayaranAkunCache cache) {
		if (itemBiaya == null) {
			return null;
		}
		Akun akun = ambilAkunDariCache(cache, "PIUTANG", itemBiaya, kegiatan);
		if (akun != null) {
			return akun;
		}
		try {
			akun = itemBiaya.ambilPiutang(kegiatan);
		} catch (Exception e) {
			akun = null;
		}
		simpanAkunKeCache(cache, "PIUTANG", itemBiaya, kegiatan, akun);
		if (akun != null) {
			return akun;
		}

		akun = ambilAkunDariCache(cache, "DIMUKA", itemBiaya, kegiatan);
		if (akun != null) {
			return akun;
		}
		try {
			akun = itemBiaya.ambilDibayarDimuka(kegiatan);
		} catch (Exception e) {
			akun = null;
		}
		simpanAkunKeCache(cache, "DIMUKA", itemBiaya, kegiatan, akun);
		if (akun != null) {
			return akun;
		}

		akun = ambilAkunDariCache(cache, "PENDAPATAN", itemBiaya, kegiatan);
		if (akun != null) {
			return akun;
		}
		try {
			akun = itemBiaya.ambilAkun(kegiatan);
		} catch (Exception e) {
			akun = null;
		}
		simpanAkunKeCache(cache, "PENDAPATAN", itemBiaya, kegiatan, akun);
		return akun;
	}

	/**
	 * Pintasan {@link #ambilAkunPendapatanDenda(ItemBiaya, Kegiatan,
	 * JurnalPembayaranAkunCache)} tanpa cache.
	 *
	 * @param itemBiaya item biaya; boleh {@code null}.
	 * @param kegiatan kegiatan penentu pemetaan akun.
	 * @return akun pendapatan denda, atau {@code null}.
	 */
	private static Akun ambilAkunPendapatanDenda(ItemBiaya itemBiaya, Kegiatan kegiatan) {
		return ambilAkunPendapatanDenda(itemBiaya, kegiatan, null);
	}

	/**
	 * Menentukan akun pendapatan denda untuk sebuah item biaya pada kegiatan tertentu,
	 * dengan bantuan cache.
	 *
	 * @param itemBiaya item biaya; {@code null} → {@code null}.
	 * @param kegiatan kegiatan penentu pemetaan akun; boleh {@code null}.
	 * @param cache cache pencarian akun; boleh {@code null}.
	 * @return akun pendapatan denda, atau {@code null} bila belum diatur — pemanggil
	 *         akan jatuh ke akun tagihan utama disertai peringatan.
	 */
	private static Akun ambilAkunPendapatanDenda(ItemBiaya itemBiaya, Kegiatan kegiatan,
			JurnalPembayaranAkunCache cache) {
		if (itemBiaya == null) {
			return null;
		}
		Akun akun = ambilAkunDariCache(cache, "DENDA", itemBiaya, kegiatan);
		if (akun != null) {
			return akun;
		}
		try {
			akun = itemBiaya.ambilPendapatanDenda(kegiatan);
		} catch (Exception e) {
			akun = null;
		}
		simpanAkunKeCache(cache, "DENDA", itemBiaya, kegiatan, akun);
		return akun;
	}

	/**
	 * Membaca akun dari cache pratinjau.
	 *
	 * @param cache cache; boleh {@code null}.
	 * @param tipe penciri jenis akun ({@code "PIUTANG"}, {@code "DIMUKA"},
	 *        {@code "PENDAPATAN"}, {@code "DENDA"}).
	 * @param itemBiaya item biaya; boleh {@code null}.
	 * @param kegiatan kegiatan; boleh {@code null}.
	 * @return akun yang pernah disimpan, atau {@code null} bila belum ada.
	 */
	private static Akun ambilAkunDariCache(JurnalPembayaranAkunCache cache, String tipe, ItemBiaya itemBiaya,
			Kegiatan kegiatan) {
		if (cache == null || itemBiaya == null || cache.akunMap == null) {
			return null;
		}
		return cache.akunMap.get(buildKeyCacheAkun(tipe, itemBiaya, kegiatan));
	}

	/**
	 * Menyimpan akun ke cache pratinjau. Nilai {@code null} sengaja TIDAK disimpan
	 * sehingga pencarian yang gagal akan dicoba lagi bila item biaya yang sama muncul
	 * kembali.
	 *
	 * @param cache cache; boleh {@code null} (tidak melakukan apa pun).
	 * @param tipe penciri jenis akun.
	 * @param itemBiaya item biaya; {@code null} → tidak disimpan.
	 * @param kegiatan kegiatan; boleh {@code null}.
	 * @param akun akun hasil pencarian; {@code null} → tidak disimpan.
	 */
	private static void simpanAkunKeCache(JurnalPembayaranAkunCache cache, String tipe, ItemBiaya itemBiaya,
			Kegiatan kegiatan, Akun akun) {
		if (cache == null || itemBiaya == null || akun == null || cache.akunMap == null) {
			return;
		}
		cache.akunMap.put(buildKeyCacheAkun(tipe, itemBiaya, kegiatan), akun);
	}

	/**
	 * Menyusun kunci cache {@code "tipe:idItemBiaya:idKegiatan"}. Id yang {@code null}
	 * atau gagal dibaca diwakili {@code "0"}.
	 *
	 * @param tipe penciri jenis akun.
	 * @param itemBiaya item biaya; boleh {@code null}.
	 * @param kegiatan kegiatan; boleh {@code null}.
	 * @return kunci cache, tidak pernah {@code null}.
	 */
	private static String buildKeyCacheAkun(String tipe, ItemBiaya itemBiaya, Kegiatan kegiatan) {
		Long itemId = null;
		Long kegiatanId = null;
		try {
			itemId = itemBiaya == null ? null : itemBiaya.getId();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:1498");
		}
		try {
			kegiatanId = kegiatan == null ? null : kegiatan.getId();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:1502");
		}
		return safeString(tipe) + ":" + (itemId == null ? "0" : itemId.toString()) + ":"
				+ (kegiatanId == null ? "0" : kegiatanId.toString());
	}

	/**
	 * Menghitung nominal satu detail biaya untuk kebutuhan pratinjau jurnal: tarif per
	 * kegiatan lebih dulu ({@code Kegiatan.ambilJumlahTagihan}), baru total bawaan detail
	 * biaya ({@code detailBiaya.hitungTotal()}).
	 *
	 * @param detailBiaya detail biaya; {@code null} → {@code 0.0}.
	 * @param kegiatan kegiatan penentu tarif; boleh {@code null}.
	 * @return nominal, atau {@code 0.0} bila tidak dapat dihitung.
	 */
	private static double hitungNilaiDetailBiayaUntukJurnal(DetailBiaya detailBiaya, Kegiatan kegiatan) {
		if (detailBiaya == null) {
			return 0.0;
		}
		try {
			if (kegiatan != null) {
				Double nilai = Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya);
				if (nilai != null) {
					return nilai.doubleValue();
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:1519");
		}
		try {
			Double nilai = detailBiaya.hitungTotal();
			return nilai == null ? 0.0 : nilai.doubleValue();
		} catch (Exception e) {
			return 0.0;
		}
	}

	/**
	 * Menambahkan pasangan (akun, nilai) ke daftar pratinjau, <b>menggabungkan</b> nilai
	 * bila akun yang sama sudah ada.
	 *
	 * <p>Aturan: nilai dengan {@code Math.abs(nilai) <= 0.1} diabaikan sebagai nol
	 * (idiom toleransi pembulatan AIS); akun {@code null} tidak ditambahkan tetapi
	 * memicu peringatan {@code warningJikaKosong} bila penampung peringatan tersedia.
	 * Perbandingan akun memakai id, lalu kode
	 * ({@link #akunSama(Akun, Akun)}).</p>
	 *
	 * @param akuns daftar akun; <b>dimodifikasi</b>. {@code null} → tidak melakukan apa pun.
	 * @param nilais daftar nilai sejajar indeks; <b>dimodifikasi</b>.
	 * @param akun akun yang ditambahkan; {@code null} memicu peringatan.
	 * @param nilai nominal, boleh bertanda negatif.
	 * @param warnings penampung peringatan; boleh {@code null}.
	 * @param warningJikaKosong pesan yang dicatat bila {@code akun} {@code null}.
	 */
	private static void tambahAkunNilai(List<Akun> akuns, List<Double> nilais, Akun akun, double nilai,
			StringBuffer warnings, String warningJikaKosong) {
		if (Math.abs(nilai) <= 0.1) {
			return;
		}
		if (akun == null) {
			addWarning(warnings, warningJikaKosong);
			return;
		}
		if (akuns == null || nilais == null) {
			return;
		}
		for (int i = 0; i < akuns.size(); i++) {
			Akun akunLama = akuns.get(i);
			if (akunSama(akunLama, akun)) {
				double nilaiLama = nilais.size() > i && nilais.get(i) != null ? nilais.get(i).doubleValue() : 0.0;
				nilais.set(i, Double.valueOf(nilaiLama + nilai));
				return;
			}
		}
		akuns.add(akun);
		nilais.add(Double.valueOf(nilai));
	}

	/**
	 * Membandingkan dua akun: berdasarkan id bila keduanya tersedia, selain itu
	 * berdasarkan kode.
	 *
	 * <p>Diperlukan karena pratinjau bisa mencampur akun dari cache
	 * {@code ConstantValues} dan akun hasil pembacaan session — dua instance Java berbeda
	 * untuk baris database yang sama.</p>
	 *
	 * @param akun1 akun pertama; {@code null} → {@code false}.
	 * @param akun2 akun kedua; {@code null} → {@code false}.
	 * @return {@code true} bila kedua akun dianggap sama.
	 */
	private static boolean akunSama(Akun akun1, Akun akun2) {
		if (akun1 == null || akun2 == null) {
			return false;
		}
		try {
			if (akun1.getId() != null && akun2.getId() != null) {
				return akun1.getId().equals(akun2.getId());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:1561");
		}
		try {
			return akun1.getKode() != null && akun1.getKode().equals(akun2.getKode());
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Mengubah {@link Double} yang mungkin {@code null} menjadi {@code double}.
	 *
	 * @param nilai nilai; boleh {@code null}.
	 * @return nilai, atau {@code 0.0}.
	 */
	private static double safeDouble(Double nilai) {
		return nilai == null ? 0.0 : nilai.doubleValue();
	}

	/**
	 * Mengubah {@link String} yang mungkin {@code null} menjadi string kosong.
	 *
	 * @param value nilai; boleh {@code null}.
	 * @return nilai, atau string kosong.
	 */
	private static String safeString(String value) {
		return value == null ? "" : value;
	}

	/**
	 * Menyusun label sebuah cicilan untuk pesan peringatan: label item biayanya bila ada,
	 * selain itu {@code "cicilan ID <id>"}, dan terakhir teks umum {@code "pembayaran"}.
	 *
	 * @param cicilanPembayaran cicilan; boleh {@code null}.
	 * @return label yang selalu layak tampil, tidak pernah {@code null}.
	 */
	private static String labelCicilanPembayaran(CicilanPembayaran cicilanPembayaran) {
		try {
			if (cicilanPembayaran != null && cicilanPembayaran.getItemBiaya() != null) {
				return labelItemBiaya(cicilanPembayaran.getItemBiaya());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:1583");
		}
		try {
			return cicilanPembayaran == null || cicilanPembayaran.getId() == null ? "pembayaran"
					: "cicilan ID " + cicilanPembayaran.getId();
		} catch (Exception e) {
			return "pembayaran";
		}
	}

	/**
	 * Menyusun label item biaya untuk pesan peringatan: {@code "kode - nama"}, lalu
	 * {@code "nama"} saja, lalu teks umum {@code "item biaya"}.
	 *
	 * @param itemBiaya item biaya; boleh {@code null}.
	 * @return label yang selalu layak tampil, tidak pernah {@code null}.
	 */
	private static String labelItemBiaya(ItemBiaya itemBiaya) {
		try {
			if (itemBiaya != null && itemBiaya.getKode() != null && itemBiaya.getNama() != null) {
				return itemBiaya.getKode() + " - " + itemBiaya.getNama();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:1598");
		}
		try {
			if (itemBiaya != null && itemBiaya.getNama() != null) {
				return itemBiaya.getNama();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:1604");
		}
		return "item biaya";
	}

	/**
	 * Menambahkan satu baris peringatan ke penampung, <b>tanpa duplikasi</b>.
	 *
	 * <p>Deduplikasi memakai {@code indexOf} pada seluruh teks yang sudah terkumpul,
	 * sehingga peringatan yang merupakan substring peringatan lain juga dianggap sudah
	 * ada. Baris dipisahkan {@code "\n"}.</p>
	 *
	 * @param warnings penampung; {@code null} → tidak melakukan apa pun.
	 * @param warning teks peringatan; {@code null}/kosong diabaikan.
	 */
	private static void addWarning(StringBuffer warnings, String warning) {
		if (warnings == null || warning == null || warning.trim().length() == 0) {
			return;
		}
		if (warnings.toString().indexOf(warning) >= 0) {
			return;
		}
		if (warnings.length() > 0) {
			warnings.append("\n");
		}
		warnings.append(warning);
	}

	/**
	 * Cache pencarian akun untuk satu kali render pratinjau jurnal pembayaran.
	 *
	 * <p>Pratinjau kerap memproses puluhan cicilan yang menunjuk item biaya yang sama;
	 * tanpa cache, {@code ItemBiaya.ambilPiutang/ambilDibayarDimuka/ambilAkun} akan
	 * dipanggil berulang untuk pasangan item+kegiatan yang identik. Kunci peta disusun
	 * {@link GrupTransaksi#buildKeyCacheAkun(String, ItemBiaya, Kegiatan)}.</p>
	 *
	 * <p><b>Lingkup:</b> tipe bersifat {@code static} dan tidak menangkap instance
	 * {@link GrupTransaksi}; umurnya hanya selama satu pemanggilan method pratinjau,
	 * sehingga tidak pernah menjadi cache basi. Hasil pencarian yang {@code null} sengaja
	 * tidak disimpan.</p>
	 *
	 * @see GrupTransaksi
	 */
	private static class JurnalPembayaranAkunCache {
		private Map<String, Akun> akunMap = new HashMap<String, Akun>();
	}

	/**
	 * Pintasan {@link #tampilkanJurnal(List, List, List, List, String)} tanpa teks
	 * peringatan.
	 *
	 * @param akunsDebetsTemp akun sisi debit sebelum normalisasi tanda.
	 * @param nilaiDebetsTemp nilai sisi debit sejajar indeks.
	 * @param akunsKreditsTemp akun sisi kredit sebelum normalisasi tanda.
	 * @param nilaiKreditsTemp nilai sisi kredit sejajar indeks.
	 * @return {@link Grid} tabel jurnal.
	 */
	@SuppressWarnings({})
	public static Grid tampilkanJurnal(List<Akun> akunsDebetsTemp, List<Double> nilaiDebetsTemp,
			List<Akun> akunsKreditsTemp, List<Double> nilaiKreditsTemp) {
		String warnings = null;
		return tampilkanJurnal(akunsDebetsTemp, nilaiDebetsTemp, akunsKreditsTemp, nilaiKreditsTemp, warnings);
	}

	/**
	 * Merender sepasang daftar akun+nilai menjadi tabel jurnal ZK tiga kolom
	 * (Akun / Debet / Kredit) lengkap dengan baris TOTAL.
	 *
	 * <p>Ini adalah penyaji baku seluruh pratinjau jurnal di aplikasi. Sebelum dirender,
	 * kedua sisi dilewatkan {@link #normalisasiDebitKredit(List, List, List, List, List,
	 * List, boolean)} yang menerapkan <b>idiom tanda AIS</b>: nilai negatif pada sisi
	 * debit dipindahkan ke sisi kredit dan sebaliknya, lalu nilai diambil mutlak;
	 * akun yang sama digabung. Nilai dengan {@code |nilai| <= 0.1} dibuang.</p>
	 *
	 * <p>Bila total debit dan kredit berbeda, sebuah baris merah "TIDAK BALANCE, SELISIH
	 * …" disisipkan. <b>Perbandingannya memakai string hasil format</b>
	 * ({@code Common.numberFormat}), sehingga selisih di bawah presisi tampilan tidak
	 * terdeteksi. Ini semata penanda visual — tidak ada penolakan simpan di jalur
	 * ini.</p>
	 *
	 * <p>Teks {@code warnings} (bila ada) ditampilkan sebagai satu baris merah membentang
	 * di bawah tabel; bila tidak ada baris sama sekali, ditampilkan pesan "Belum ada akun
	 * jurnal yang dapat ditampilkan."</p>
	 *
	 * @param akunsDebetsTemp akun sisi debit sebelum normalisasi tanda; boleh
	 *        {@code null}.
	 * @param nilaiDebetsTemp nilai sisi debit sejajar indeks; boleh {@code null}.
	 * @param akunsKreditsTemp akun sisi kredit sebelum normalisasi tanda; boleh
	 *        {@code null}.
	 * @param nilaiKreditsTemp nilai sisi kredit sejajar indeks; boleh {@code null}.
	 * @param warnings teks peringatan gabungan; boleh {@code null}/kosong.
	 * @return {@link Grid} siap dipasang ke komponen induk.
	 */
	@SuppressWarnings({ "deprecation" })
	public static Grid tampilkanJurnal(List<Akun> akunsDebetsTemp, List<Double> nilaiDebetsTemp,
			List<Akun> akunsKreditsTemp, List<Double> nilaiKreditsTemp, String warnings) {

		List<Akun> akunsDebets = new ArrayList<Akun>();
		List<Akun> akunsKredits = new ArrayList<Akun>();
		List<Double> nilaiDebets = new ArrayList<Double>();
		List<Double> nilaiKredits = new ArrayList<Double>();

		normalisasiDebitKredit(akunsDebetsTemp, nilaiDebetsTemp, akunsDebets, nilaiDebets, akunsKredits, nilaiKredits,
				true);
		normalisasiDebitKredit(akunsKreditsTemp, nilaiKreditsTemp, akunsDebets, nilaiDebets, akunsKredits, nilaiKredits,
				false);

		Grid grid = new Grid();
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setStyle("border:1px solid #e2e8f0;border-radius:10px;overflow:hidden;background:#ffffff;");

		Columns columns = new Columns();
		columns.setParent(grid);
		columns.setSizable(true);
		columns.setStyle("background:#f8fafc;border-bottom:1px solid #e5e7eb;font-weight:bold;");

		MyColumnConfig column = new MyColumnConfig("Akun");
		column.setParent(columns);
		column.setWidth("50%");

		column = new MyColumnConfig("Debet");
		column.setAlign("right");
		column.setParent(columns);

		column = new MyColumnConfig("Kredit");
		column.setAlign("right");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		Double totalDebet = 0.0;
		Double totalKredit = 0.0;

		for (int i = 0; i < akunsDebets.size(); i++) {
			Akun akun = akunsDebets.get(i);
			if (akun == null) {
				continue;
			}
			Double nilai = nilaiDebets.size() > i && nilaiDebets.get(i) != null ? nilaiDebets.get(i) : 0.0;
			if (Math.abs(nilai.doubleValue()) <= 0.1) {
				continue;
			}
			Row row = new Row();
			row.setValign("top");
			row.setStyle("border-bottom:1px solid #f1f5f9;");
			row.setParent(rows);
			row.appendChild(new Label(labelAkunJurnal(akun)));
			row.appendChild(new Label(Common.numberFormat.get().format(nilai)));
			row.appendChild(new Label(Common.numberFormat.get().format(0.0)));
			totalDebet = Double.valueOf(totalDebet.doubleValue() + nilai.doubleValue());
		}

		for (int i = 0; i < akunsKredits.size(); i++) {
			Akun akun = akunsKredits.get(i);
			if (akun == null) {
				continue;
			}
			Double nilai = nilaiKredits.size() > i && nilaiKredits.get(i) != null ? nilaiKredits.get(i) : 0.0;
			if (Math.abs(nilai.doubleValue()) <= 0.1) {
				continue;
			}
			Row row = new Row();
			row.setValign("top");
			row.setStyle("border-bottom:1px solid #f1f5f9;");
			row.setParent(rows);
			row.appendChild(new Label(labelAkunJurnal(akun)));
			row.appendChild(new Label(Common.numberFormat.get().format(0.0)));
			row.appendChild(new Label(Common.numberFormat.get().format(nilai)));
			totalKredit = Double.valueOf(totalKredit.doubleValue() + nilai.doubleValue());
		}

		boolean balance = Common.numberFormat.get().format(totalDebet).equals(Common.numberFormat.get().format(totalKredit));
		if (!balance) {
			Row row = new Row();
			row.setValign("top");
			row.setStyle("background:#fff7ed;");
			row.setParent(rows);
			row.appendChild(new Label(""));
			row.appendChild(new MyLabelBoldMerah(
					"TIDAK BALANCE, SELISIH " + Common.numberFormat.get().format(Math.abs(totalDebet - totalKredit))));
			row.appendChild(new Label(""));
		}

		if (warnings != null && !warnings.trim().isEmpty()) {
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "3");
			row.setStyle("background:#fef2f2;");
			row.appendChild(new MyLabelAgakKecilBoldMerah(warnings));
		}

		if (rows.getChildren() == null || rows.getChildren().size() == 0) {
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "3");
			row.appendChild(new MyLabelAgakKecil("Belum ada akun jurnal yang dapat ditampilkan."));
		}

		Foot foot = new Foot();
		foot.setParent(grid);

		Footer footer = new Footer("Total");
		footer.setParent(foot);

		footer = new Footer(Common.numberFormat.get().format(totalDebet));
		footer.setParent(foot);

		footer = new Footer(Common.numberFormat.get().format(totalKredit));
		footer.setParent(foot);

		return grid;
	}

	/**
	 * Varian {@link #tampilkanJurnal(List, List, List, List, String)} dengan kolom
	 * tambahan "Keterangan Biaya" (nama rincian item) pada setiap baris <b>debit</b>.
	 *
	 * <p>{@code keteranganBiayaDebets} sejajar indeks dengan {@code akunsDebetsTemp}
	 * (urutan item pada formula Kas Kecil). Berbeda dari varian tiga kolom, baris debit
	 * di sini <b>TIDAK digabung per akun</b> — justru agar tiap item rincian tampil
	 * dengan keterangannya masing-masing; konsekuensinya normalisasi tanda juga tidak
	 * dijalankan, nilai hanya diambil mutlak.</p>
	 *
	 * <p>Bila {@code keteranganBiayaDebets} {@code null}, kosong, atau seluruh isinya
	 * blank, method ini jatuh ke tampilan tiga kolom yang lama.</p>
	 *
	 * @param akunsDebetsTemp akun sisi debit; boleh {@code null}.
	 * @param nilaiDebetsTemp nilai sisi debit sejajar indeks; boleh {@code null}.
	 * @param akunsKreditsTemp akun sisi kredit; boleh {@code null}.
	 * @param nilaiKreditsTemp nilai sisi kredit sejajar indeks; boleh {@code null}.
	 * @param warnings teks peringatan gabungan; boleh {@code null}/kosong.
	 * @param keteranganBiayaDebets keterangan biaya per baris debit; boleh {@code null}.
	 * @return {@link Grid} empat kolom, atau hasil varian tiga kolom bila tidak ada
	 *         keterangan yang berarti.
	 */
	public static Grid tampilkanJurnal(List<Akun> akunsDebetsTemp, List<Double> nilaiDebetsTemp,
			List<Akun> akunsKreditsTemp, List<Double> nilaiKreditsTemp, String warnings,
			List<String> keteranganBiayaDebets) {
		
		boolean adaKeterangan = false;
		if (keteranganBiayaDebets != null) {
			for (String sKet : keteranganBiayaDebets) {
				if (sKet != null && !sKet.trim().isEmpty()) {
					adaKeterangan = true;
					break;
				}
			}
		}
		if (!adaKeterangan) {
			return tampilkanJurnal(akunsDebetsTemp, nilaiDebetsTemp, akunsKreditsTemp, nilaiKreditsTemp, warnings);
		}
		
		Grid grid = new Grid();
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setStyle("border:1px solid #e2e8f0;border-radius:10px;overflow:hidden;background:#ffffff;");
		
		Columns columns = new Columns();
		columns.setParent(grid);
		columns.setSizable(true);
		columns.setStyle("background:#f8fafc;border-bottom:1px solid #e5e7eb;font-weight:bold;");
		
		MyColumnConfig column = new MyColumnConfig("Akun");
		column.setParent(columns);
		column.setWidth("38%");
		
		column = new MyColumnConfig("Keterangan Biaya");
		column.setParent(columns);
		column.setWidth("28%");
		
		column = new MyColumnConfig("Debet");
		column.setAlign("right");
		column.setParent(columns);
		
		column = new MyColumnConfig("Kredit");
		column.setAlign("right");
		column.setParent(columns);
		
		Rows rows = new Rows();
		rows.setParent(grid);
		
		Double totalDebet = 0.0;
		Double totalKredit = 0.0;
		
		int nDebet = akunsDebetsTemp == null ? 0 : akunsDebetsTemp.size();
		for (int i = 0; i < nDebet; i++) {
			Akun akun = akunsDebetsTemp.get(i);
			if (akun == null) {
				continue;
			}
			Double nilaiObj = (nilaiDebetsTemp != null && nilaiDebetsTemp.size() > i && nilaiDebetsTemp.get(i) != null)
				? nilaiDebetsTemp.get(i) : Double.valueOf(0.0);
			double nilai = Math.abs(nilaiObj.doubleValue());
			if (nilai <= 0.1) {
				continue;
			}
			String ketBiaya = (keteranganBiayaDebets != null && keteranganBiayaDebets.size() > i
				&& keteranganBiayaDebets.get(i) != null) ? keteranganBiayaDebets.get(i) : "";
			Row row = new Row();
			row.setValign("top");
			row.setStyle("border-bottom:1px solid #f1f5f9;");
			row.setParent(rows);
			row.appendChild(new Label(labelAkunJurnal(akun)));
			row.appendChild(new Label(ketBiaya));
			row.appendChild(new Label(Common.numberFormat.get().format(nilai)));
			row.appendChild(new Label(Common.numberFormat.get().format(0.0)));
			totalDebet = Double.valueOf(totalDebet.doubleValue() + nilai);
		}
		
		int nKredit = akunsKreditsTemp == null ? 0 : akunsKreditsTemp.size();
		for (int i = 0; i < nKredit; i++) {
			Akun akun = akunsKreditsTemp.get(i);
			if (akun == null) {
				continue;
			}
			Double nilaiObj = (nilaiKreditsTemp != null && nilaiKreditsTemp.size() > i && nilaiKreditsTemp.get(i) != null)
				? nilaiKreditsTemp.get(i) : Double.valueOf(0.0);
			double nilai = Math.abs(nilaiObj.doubleValue());
			if (nilai <= 0.1) {
				continue;
			}
			Row row = new Row();
			row.setValign("top");
			row.setStyle("border-bottom:1px solid #f1f5f9;");
			row.setParent(rows);
			row.appendChild(new Label(labelAkunJurnal(akun)));
			row.appendChild(new Label(""));
			row.appendChild(new Label(Common.numberFormat.get().format(0.0)));
			row.appendChild(new Label(Common.numberFormat.get().format(nilai)));
			totalKredit = Double.valueOf(totalKredit.doubleValue() + nilai);
		}
		
		boolean balance = Common.numberFormat.get().format(totalDebet).equals(Common.numberFormat.get().format(totalKredit));
		if (!balance) {
			Row row = new Row();
			row.setValign("top");
			row.setStyle("background:#fff7ed;");
			row.setParent(rows);
			row.appendChild(new Label(""));
			row.appendChild(new Label(""));
			row.appendChild(new MyLabelBoldMerah(
				"TIDAK BALANCE, SELISIH " + Common.numberFormat.get().format(Math.abs(totalDebet - totalKredit))));
			row.appendChild(new Label(""));
		}
		
		if (warnings != null && !warnings.trim().isEmpty()) {
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "4");
			row.setStyle("background:#fef2f2;");
			row.appendChild(new MyLabelAgakKecilBoldMerah(warnings));
		}
		
		if (rows.getChildren() == null || rows.getChildren().size() == 0) {
			Row row = new Row();
			row.setValign("top");
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "4");
			row.appendChild(new MyLabelAgakKecil("Belum ada akun jurnal yang dapat ditampilkan."));
		}
		
		Foot foot = new Foot();
		foot.setParent(grid);
		
		Footer footer = new Footer("Total");
		footer.setParent(foot);
		
		footer = new Footer("");
		footer.setParent(foot);
		
		footer = new Footer(Common.numberFormat.get().format(totalDebet));
		footer.setParent(foot);
		
		footer = new Footer(Common.numberFormat.get().format(totalKredit));
		footer.setParent(foot);
		
		return grid;
	}

	/**
	 * Menerapkan <b>idiom tanda Dr/Cr AIS</b>: memindahkan setiap pasangan (akun, nilai)
	 * ke sisi yang benar berdasarkan tanda nilainya, lalu menggabungkannya ke daftar
	 * hasil dengan nilai mutlak.
	 *
	 * <p>Aturannya: entri yang berasal dari sisi debit dengan nilai {@code >= 0} tetap di
	 * debit, sedangkan nilai negatif dipindahkan ke kredit; entri yang berasal dari sisi
	 * kredit berlaku kebalikannya. Nilai dengan {@code |nilai| <= 0.1} dan akun
	 * {@code null} dibuang. Iterasi dibatasi ukuran terkecil di antara kedua list sumber
	 * sehingga list yang tidak sejajar tidak menimbulkan {@code IndexOutOfBounds}.</p>
	 *
	 * <p>Idiom ini diwarisi demi paritas dengan tombol posting generasi lama dan
	 * <b>bukan cacat</b>: mesin posting memang menyatakan pembalikan arah jurnal
	 * (mis. koreksi/retur) lewat tanda nilai, bukan lewat kolom terpisah.</p>
	 *
	 * @param akunTemp akun sumber; {@code null} → tidak melakukan apa pun.
	 * @param nilaiTemp nilai sumber sejajar indeks; {@code null} → tidak melakukan apa pun.
	 * @param akunsDebets daftar akun debit hasil; <b>dimodifikasi</b>.
	 * @param nilaiDebets daftar nilai debit hasil; <b>dimodifikasi</b>.
	 * @param akunsKredits daftar akun kredit hasil; <b>dimodifikasi</b>.
	 * @param nilaiKredits daftar nilai kredit hasil; <b>dimodifikasi</b>.
	 * @param sumberDebet {@code true} bila pasangan sumber berasal dari sisi debit.
	 */
	private static void normalisasiDebitKredit(List<Akun> akunTemp, List<Double> nilaiTemp, List<Akun> akunsDebets,
			List<Double> nilaiDebets, List<Akun> akunsKredits, List<Double> nilaiKredits, boolean sumberDebet) {
		if (akunTemp == null || nilaiTemp == null || akunsDebets == null || nilaiDebets == null || akunsKredits == null
				|| nilaiKredits == null) {
			return;
		}
		int batas = Math.min(akunTemp.size(), nilaiTemp.size());
		for (int i = 0; i < batas; i++) {
			Akun akun = akunTemp.get(i);
			Double nilaiObject = nilaiTemp.get(i);
			double nilai = nilaiObject == null ? 0.0 : nilaiObject.doubleValue();
			if (akun == null || Math.abs(nilai) <= 0.1) {
				continue;
			}
			if ((sumberDebet && nilai >= 0.0) || (!sumberDebet && nilai < 0.0)) {
				tambahAkunNilai(akunsDebets, nilaiDebets, akun, Math.abs(nilai), null, null);
			} else {
				tambahAkunNilai(akunsKredits, nilaiKredits, akun, Math.abs(nilai), null, null);
			}
		}
	}

	/**
	 * Menyusun label akun untuk tabel jurnal: {@code "kode - nama"}, atau salah satunya
	 * bila yang lain kosong.
	 *
	 * <p>Pembacaan {@code kode} dan {@code nama} masing-masing dibungkus {@code try/catch}
	 * karena akun bisa berupa proxy lazy yang session-nya sudah tertutup — label kosong
	 * lebih baik daripada layar jurnal yang gagal render.</p>
	 *
	 * @param akun akun; {@code null} → string kosong.
	 * @return label akun, tidak pernah {@code null}.
	 */
	private static String labelAkunJurnal(Akun akun) {
		if (akun == null) {
			return "";
		}
		String kode = "";
		String nama = "";
		try {
			kode = akun.getKode() == null ? "" : akun.getKode();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:1939");
		}
		try {
			nama = akun.getNama() == null ? "" : akun.getNama();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:1943");
		}
		if (kode.length() > 0 && nama.length() > 0) {
			return kode + " - " + nama;
		}
		return kode.length() > 0 ? kode : nama;
	}

	/**
	 * Pintasan {@link #tampilkanJurnal(List, List, List, List, String)} untuk jurnal
	 * paling sederhana: satu akun debit dan satu akun kredit.
	 *
	 * @param akunsDebets akun sisi debit; {@code null} → sisi debit dikosongkan.
	 * @param nilaiDebets nilai debit; {@code null} dianggap {@code 0.0}.
	 * @param akunsKredits akun sisi kredit; {@code null} → sisi kredit dikosongkan.
	 * @param nilaiKredits nilai kredit; {@code null} dianggap {@code 0.0}.
	 * @return {@link Grid} tabel jurnal dua baris.
	 */
	@SuppressWarnings({})
	public static Grid tampilkanJurnal(Akun akunsDebets, Double nilaiDebets, Akun akunsKredits, Double nilaiKredits) {
		List<Akun> listAkunDebet = new ArrayList<Akun>();
		List<Double> listNilaiDebet = new ArrayList<Double>();
		List<Akun> listAkunKredit = new ArrayList<Akun>();
		List<Double> listNilaiKredit = new ArrayList<Double>();
		if (akunsDebets != null) {
			listAkunDebet.add(akunsDebets);
			listNilaiDebet.add(nilaiDebets == null ? 0.0 : nilaiDebets);
		}
		if (akunsKredits != null) {
			listAkunKredit.add(akunsKredits);
			listNilaiKredit.add(nilaiKredits == null ? 0.0 : nilaiKredits);
		}
		return tampilkanJurnal(listAkunDebet, listNilaiDebet, listAkunKredit, listNilaiKredit, null);
	}

	/**
	 * Satuan kerja pemilik jurnal — salah satu dari dua penyaring tenant de facto pada
	 * entity ini (yang lain {@link #getWorkspace()}).
	 *
	 * <p><b>Getter dengan WRITE-BACK.</b> Method ini tidak sekadar membaca: ia
	 * <b>menurunkan</b> satuan kerja dari dokumen sumber lewat rantai prioritas
	 * (pembayaran siswa → deposit siswa → uang muka → LPJ → dana talangan → kas kecil →
	 * kas besar → workspace → jenis kas kecil) dan menugaskan hasilnya kembali ke field
	 * terpetakan {@code satuanKerja}. Karena Hibernate membaca properti lewat getter,
	 * sekadar menampilkan jurnal di dalam session hidup dapat menerbitkan {@code UPDATE}
	 * pada kolom {@code satuan_kerja}. Hanya bila seluruh rantai gagal, nilai tersimpan
	 * dipakai apa adanya (setelah {@code check(...)}).</p>
	 *
	 * <p><b>Implikasi cakupan tenant:</b> karena entity ini tidak punya kolom
	 * {@code sekolah}/{@code yayasan}, kolom inilah yang biasanya dipakai laporan untuk
	 * memisahkan unit. Jurnal yang dokumen sumbernya tidak menyediakan satuan kerja
	 * (mis. jurnal manual) akan bernilai {@code null} dan lolos dari saringan berbasis
	 * satuan kerja — bersifat <b>fail-open</b> bagi kueri yang tidak menangani
	 * {@code null} secara eksplisit.</p>
	 *
	 * @return satuan kerja pemilik jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {

		if (pembayaranSiswaDetail != null && pembayaranSiswaDetail.getPembayaranSiswa() != null
				&& pembayaranSiswaDetail.getPembayaranSiswa().getSekolah() != null
				&& pembayaranSiswaDetail.getPembayaranSiswa().getSekolah().getSatuanKerja() != null) {
			satuanKerja = pembayaranSiswaDetail.getPembayaranSiswa().getSekolah().getSatuanKerja();
		} else if (depositSiswa != null && depositSiswa.getSekolah() != null
				&& depositSiswa.getSekolah().getSatuanKerja() != null) {
			satuanKerja = depositSiswa.getSekolah().getSatuanKerja();
		} else if (getUangMuka() != null && uangMuka.getSatuanKerja() != null) {
			satuanKerja = uangMuka.getSatuanKerja();
		} else if (getPertangungjawaban() != null && pertangungjawaban.getSatuanKerja() != null) {
			satuanKerja = pertangungjawaban.getSatuanKerja();
		} else if (getDanaTalangan() != null && danaTalangan.getSatuanKerja() != null) {
			satuanKerja = danaTalangan.getSatuanKerja();
		} else if (getKasKecil() != null && kasKecil.getSatuanKerja() != null) {
			satuanKerja = kasKecil.getSatuanKerja();
		} else if (getKasBesar() != null && kasBesar.getSatuanKerja() != null) {
			satuanKerja = kasBesar.getSatuanKerja();
		} else if (getWorkspace() != null && workspace.getSatuanKerja() != null) {
			satuanKerja = workspace.getSatuanKerja();
		} else if (getJenisKasKecil() != null && jenisKasKecil.getSatuanKerja() != null) {
			satuanKerja = jenisKasKecil.getSatuanKerja();
		} else {
			satuanKerja = check(satuanKerja);
		}
		return satuanKerja;
	}

	/**
	 * Menyetel satuan kerja pemilik jurnal.
	 *
	 * <p>Perhatikan bahwa nilai yang disetel di sini dapat <b>ditimpa</b> oleh
	 * {@link #getSatuanKerja()} pada pembacaan berikutnya bila dokumen sumber menyediakan
	 * satuan kerja sendiri.</p>
	 *
	 * @param satuanKerja satuan kerja; boleh {@code null}.
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Referensi dokumen sumber: transaksi pegawai (kasbon, potongan, tunjangan lepas).
	 *
	 * <p><b>Catatan kunci unik:</b> field ini ikut menyusun {@link #ambilUnik()} dengan
	 * nama kelasnya sendiri ({@code TransaksiPegawai}). Sebelum diperbaiki, cabang ini
	 * keliru menulis {@code PembayaranGajiPunyaPegawai.class.getName()} sehingga kunci
	 * jurnal transaksi pegawai ber-id N bertabrakan dengan kunci slip gaji ber-id N.</p>
	 *
	 * @return transaksi pegawai asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "transaksi_pegawai", nullable = true)
	public TransaksiPegawai getTransaksiPegawai() {
		return transaksiPegawai;
	}

	/**
	 * Menyetel referensi transaksi pegawai.
	 *
	 * @param transaksiPegawai dokumen transaksi pegawai asal jurnal.
	 */
	public void setTransaksiPegawai(TransaksiPegawai transaksiPegawai) {
		this.transaksiPegawai = transaksiPegawai;
	}

	/**
	 * Referensi dokumen sumber: log pembayaran (kanal <i>payment gateway</i> /
	 * host-to-host).
	 *
	 * <p><b>Catatan kunci unik:</b> field ini <b>TIDAK</b> ikut menyusun
	 * {@link #ambilUnik()}. Jurnal yang hanya bereferensi ke log pembayaran karena itu
	 * tidak punya kunci idempotensi berbasis dokumen — lihat peringatan pada dokumentasi
	 * kelas. Dokumen ini juga termasuk yang dapat memikul lebih dari satu kaki
	 * jurnal.</p>
	 *
	 * @return log pembayaran asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "log_pembayaran", nullable = true)
	public LogPembayaran getLogPembayaran() {
		return logPembayaran;
	}

	/**
	 * Menyetel referensi log pembayaran.
	 *
	 * @param logPembayaran dokumen log pembayaran asal jurnal.
	 */
	public void setLogPembayaran(LogPembayaran logPembayaran) {
		this.logPembayaran = logPembayaran;
	}

	/**
	 * Referensi dokumen sumber: detail pembayaran siswa (satu baris item yang dibayar).
	 * Ikut menyusun {@link #ambilUnik()}, dan menjadi sumber turunan
	 * {@link #getSatuanKerja()}.
	 *
	 * @return detail pembayaran siswa asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pembayaran_siswa_detail", nullable = true)
	public PembayaranSiswaDetail getPembayaranSiswaDetail() {
		return pembayaranSiswaDetail;
	}

	/**
	 * Menyetel referensi detail pembayaran siswa.
	 *
	 * @param pembayaranSiswaDetail dokumen detail pembayaran siswa asal jurnal.
	 */
	public void setPembayaranSiswaDetail(PembayaranSiswaDetail pembayaranSiswaDetail) {
		this.pembayaranSiswaDetail = pembayaranSiswaDetail;
	}

	/**
	 * Referensi dokumen sumber: setoran/tabungan (deposit) siswa. Ikut menyusun
	 * {@link #ambilUnik()} dan menjadi sumber turunan {@link #getSatuanKerja()}.
	 *
	 * @return deposit siswa asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "deposit_siswa", nullable = true)
	public DepositSiswa getDepositSiswa() {
		return depositSiswa;
	}

	/**
	 * Menyetel referensi deposit siswa.
	 *
	 * @param depositSiswa dokumen deposit siswa asal jurnal.
	 */
	public void setDepositSiswa(DepositSiswa depositSiswa) {
		this.depositSiswa = depositSiswa;
	}

	/**
	 * Anggaran/workspace pemilik jurnal (modul RAB) — penyaring tenant de facto kedua
	 * pada entity ini.
	 *
	 * <p>Hanya memanggil {@code check(...)} untuk menyelesaikan proxy lazy. Blok
	 * penurunan otomatis dari LPJ/uang muka/dana talangan/pemesanan pengadaan
	 * <b>sengaja dinonaktifkan</b> (dikomentari) — bila diaktifkan kembali, getter ini
	 * akan menjadi getter write-back seperti {@link #getSatuanKerja()} dan dapat
	 * memindahkan jurnal ke anggaran lain hanya karena dibaca.</p>
	 *
	 * @return workspace/anggaran pemilik jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace", nullable = true)
	public Workspace getWorkspace() {
		workspace = check(workspace);
//		if (getPertangungjawaban() != null && getPertangungjawaban().getUangMuka() != null
//				&& getPertangungjawaban().getUangMuka().getWorkspace() != null) {
//			workspace = getPertangungjawaban().getUangMuka().getWorkspace();
//		}
//
//		if (getUangMuka() != null && getUangMuka().getWorkspace() != null) {
//			workspace = getUangMuka().getWorkspace();
//		}
//
//		if (getDanaTalangan() != null && getDanaTalangan().getUangMuka().getWorkspace() != null) {
//			workspace = getDanaTalangan().getUangMuka().getWorkspace();
//		}
//
//		if (getPemesananPengadaanMasterAsset() != null && getPemesananPengadaanMasterAsset().getWorkspace() != null) {
//			workspace = getPemesananPengadaanMasterAsset().getWorkspace();
//		}

		return workspace;
	}

	/**
	 * Menyetel workspace/anggaran pemilik jurnal.
	 *
	 * @param workspace anggaran; boleh {@code null}.
	 */
	public void setWorkspace(Workspace workspace) {
		this.workspace = workspace;
	}

	/**
	 * Referensi dokumen sumber: detail kegiatan. Ikut menyusun {@link #ambilUnik()}.
	 *
	 * @return detail kegiatan asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "detail_kegiatan", nullable = true)
	public DetailKegiatan getDetailKegiatan() {
		return detailKegiatan;
	}

	/**
	 * Menyetel referensi detail kegiatan.
	 *
	 * @param detailKegiatan dokumen detail kegiatan asal jurnal.
	 */
	public void setDetailKegiatan(DetailKegiatan detailKegiatan) {
		this.detailKegiatan = detailKegiatan;
	}

	/**
	 * Referensi dokumen sumber: detail penerimaan barang pengadaan aset. Ikut menyusun
	 * {@link #ambilUnik()}.
	 *
	 * @return detail penerimaan pengadaan aset asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penerimaan_pengadaan_master_asset_detail", nullable = true)
	public PenerimaanPengadaanMasterAssetDetail getPenerimaanPengadaanMasterAssetDetail() {
		return penerimaanPengadaanMasterAssetDetail;
	}

	/**
	 * Menyetel referensi detail penerimaan pengadaan aset.
	 *
	 * @param penerimaanPengadaanMasterAssetDetail dokumen detail penerimaan asal jurnal.
	 */
	public void setPenerimaanPengadaanMasterAssetDetail(
			PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail) {
		this.penerimaanPengadaanMasterAssetDetail = penerimaanPengadaanMasterAssetDetail;
	}

	/**
	 * Referensi dokumen sumber: penyusutan aset (jurnal beban penyusutan berkala). Ikut
	 * menyusun {@link #ambilUnik()}.
	 *
	 * @return dokumen penyusutan aset asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penyusutan_asset", nullable = true)
	public PenyusutanAsset getPenyusutanAsset() {
		return penyusutanAsset;
	}

	/**
	 * Menyetel referensi penyusutan aset.
	 *
	 * @param penyusutanAsset dokumen penyusutan aset asal jurnal.
	 */
	public void setPenyusutanAsset(PenyusutanAsset penyusutanAsset) {
		this.penyusutanAsset = penyusutanAsset;
	}

	/**
	 * Referensi dokumen sumber: deposit (mahasiswa/umum). Ikut menyusun
	 * {@link #ambilUnik()}.
	 *
	 * @return dokumen deposit asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "deposit", nullable = true)
	public Deposit getDeposit() {
		return deposit;
	}

	/**
	 * Menyetel referensi deposit.
	 *
	 * @param deposit dokumen deposit asal jurnal.
	 */
	public void setDeposit(Deposit deposit) {
		this.deposit = deposit;
	}

	/**
	 * Referensi dokumen sumber: tagihan siswa.
	 *
	 * <p><b>Dokumen ber-kaki ganda.</b> Satu {@link Tagihan} dapat menghasilkan EMPAT
	 * jurnal sekaligus (piutang, denda, diskon, dibayar dimuka). Karena
	 * {@link #ambilUnik()} hanya membedakan kaki lewat {@link #getRef()}, kaki utama
	 * (piutang) memakai {@code ref} {@code null} sedangkan tiga kaki lain WAJIB memakai
	 * {@code REF_DENDA_SISWA}/{@code REF_DISKON_SISWA}/{@code REF_DIMUKA_SISWA} dari
	 * {@code PostingJurnalHelper}. Pembatalan yang menghapus hanya berdasarkan kolom
	 * {@code tagihan} tanpa pembeda {@code ref} akan melenyapkan keempatnya.</p>
	 *
	 * @return tagihan siswa asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "tagihan", nullable = true)
	public Tagihan getTagihan() {
		return tagihan;
	}

	/**
	 * Menyetel referensi tagihan siswa.
	 *
	 * @param tagihan dokumen tagihan siswa asal jurnal.
	 */
	public void setTagihan(Tagihan tagihan) {
		this.tagihan = tagihan;
	}

	/**
	 * Referensi dokumen sumber: LPJ (pertanggungjawaban) uang muka.
	 *
	 * <p><b>Dokumen ber-kaki ganda</b> — kaki utama ber-{@code ref} {@code null}, kaki
	 * pajak {@code "pajak"}, kaki pengembalian sisa {@code "pengembalian"}. Ikut menyusun
	 * {@link #ambilUnik()}, dan menjadi sumber turunan {@link #getSatuanKerja()},
	 * {@link #ambilDaftarPengajuanTransfer()} serta {@link #ambilDisposisiSop()}.</p>
	 *
	 * @return LPJ uang muka asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pertangungjawaban", nullable = true)
	public Pertangungjawaban getPertangungjawaban() {
		return pertangungjawaban;
	}

	/**
	 * Menyetel referensi LPJ uang muka.
	 *
	 * @param pertangungjawaban dokumen LPJ asal jurnal.
	 */
	public void setPertangungjawaban(Pertangungjawaban pertangungjawaban) {
		this.pertangungjawaban = pertangungjawaban;
	}

	/**
	 * Referensi dokumen sumber: uang muka. Ikut menyusun {@link #ambilUnik()} dan menjadi
	 * sumber turunan satuan kerja, pengajuan transfer, serta disposisi SOP.
	 *
	 * @return dokumen uang muka asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "uang_muka", nullable = true)
	public UangMuka getUangMuka() {
		return uangMuka;
	}

	/**
	 * Menyetel referensi uang muka.
	 *
	 * @param uangMuka dokumen uang muka asal jurnal.
	 */
	public void setUangMuka(UangMuka uangMuka) {
		this.uangMuka = uangMuka;
	}

	/**
	 * Referensi dokumen sumber: kas kecil.
	 *
	 * <p>Selain menyusun {@link #ambilUnik()}, relasi ini yang mengaktifkan dua kolom
	 * tambahan "Keterangan Biaya" dan "Anggaran" pada
	 * {@link #populateDeskripsiLengkap()} — rinciannya dibaca dari JSON
	 * {@code KasKecil.formula}.</p>
	 *
	 * @return dokumen kas kecil asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kas_kecil", nullable = true)
	public KasKecil getKasKecil() {
		return kasKecil;
	}

	/**
	 * Menyetel referensi kas kecil.
	 *
	 * @param kasKecil dokumen kas kecil asal jurnal.
	 */
	public void setKasKecil(KasKecil kasKecil) {
		this.kasKecil = kasKecil;
	}

	/**
	 * Referensi dokumen sumber: penggantian (reimburse) kas kecil. Ikut menyusun
	 * {@link #ambilUnik()}; bila terisi, rincian Kas Kecil dikumpulkan dari SELURUH kas
	 * kecil anak dokumen ini.
	 *
	 * @return dokumen penggantian kas kecil asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penggantian_kas_kecil", nullable = true)
	public PenggantianKasKecil getPenggantianKasKecil() {
		return penggantianKasKecil;
	}

	/**
	 * Menyetel referensi penggantian kas kecil.
	 *
	 * @param penggantianKasKecil dokumen penggantian kas kecil asal jurnal.
	 */
	public void setPenggantianKasKecil(PenggantianKasKecil penggantianKasKecil) {
		this.penggantianKasKecil = penggantianKasKecil;
	}

	/**
	 * Referensi dokumen sumber: jenis/pos kas kecil. Ikut menyusun {@link #ambilUnik()}.
	 * Memanggil {@code check(...)} untuk menyelesaikan proxy lazy.
	 *
	 * @return jenis kas kecil asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_kas_kecil", nullable = true)
	public JenisKasKecil getJenisKasKecil() {
		jenisKasKecil = check(jenisKasKecil);
		return jenisKasKecil;
	}

	/**
	 * Menyetel referensi jenis kas kecil.
	 *
	 * @param jenisKasKecil jenis/pos kas kecil asal jurnal.
	 */
	public void setJenisKasKecil(JenisKasKecil jenisKasKecil) {
		this.jenisKasKecil = jenisKasKecil;
	}

	/**
	 * Referensi dokumen sumber: detail saldo awal aset. Ikut menyusun
	 * {@link #ambilUnik()}. Memanggil {@code check(...)} untuk menyelesaikan proxy lazy.
	 *
	 * @return detail saldo awal aset asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "saldo_awal_master_asset_detail", nullable = true)
	public SaldoAwalMasterAssetDetail getSaldoAwalMasterAssetDetail() {
		saldoAwalMasterAssetDetail = check(saldoAwalMasterAssetDetail);
		return saldoAwalMasterAssetDetail;
	}

	/**
	 * Menyetel referensi detail saldo awal aset.
	 *
	 * @param saldoAwalMasterAssetDetail detail saldo awal aset asal jurnal.
	 */
	public void setSaldoAwalMasterAssetDetail(SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail) {
		this.saldoAwalMasterAssetDetail = saldoAwalMasterAssetDetail;
	}

	/**
	 * Referensi dokumen sumber: pemesanan pengadaan aset. Ikut menyusun
	 * {@link #ambilUnik()}.
	 *
	 * @return dokumen pemesanan pengadaan aset asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pemesanan_pengadaan_master_asset", nullable = true)
	public PemesananPengadaanMasterAsset getPemesananPengadaanMasterAsset() {
		return pemesananPengadaanMasterAsset;
	}

	/**
	 * Menyetel referensi pemesanan pengadaan aset.
	 *
	 * @param pemesananPengadaanMasterAsset dokumen pemesanan pengadaan asal jurnal.
	 */
	public void setPemesananPengadaanMasterAsset(PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset) {
		this.pemesananPengadaanMasterAsset = pemesananPengadaanMasterAsset;
	}

	/**
	 * Referensi dokumen sumber: pengeluaran mahasiswa. Ikut menyusun
	 * {@link #ambilUnik()}.
	 *
	 * @return dokumen pengeluaran mahasiswa asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pengeluaran_mahasiswa", nullable = true)
	public PengeluaranMahasiswa getPengeluaranMahasiswa() {
		return pengeluaranMahasiswa;
	}

	/**
	 * Menyetel referensi pengeluaran mahasiswa.
	 *
	 * @param pengeluaranMahasiswa dokumen pengeluaran mahasiswa asal jurnal.
	 */
	public void setPengeluaranMahasiswa(PengeluaranMahasiswa pengeluaranMahasiswa) {
		this.pengeluaranMahasiswa = pengeluaranMahasiswa;
	}

	/**
	 * Referensi dokumen sumber: dana talangan. Ikut menyusun {@link #ambilUnik()} dan
	 * menjadi sumber turunan satuan kerja, pengajuan transfer, serta disposisi SOP.
	 *
	 * @return dokumen dana talangan asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "dana_talangan", nullable = true)
	public DanaTalangan getDanaTalangan() {
		return danaTalangan;
	}

	/**
	 * Menyetel referensi dana talangan.
	 *
	 * @param danaTalangan dokumen dana talangan asal jurnal.
	 */
	public void setDanaTalangan(DanaTalangan danaTalangan) {
		this.danaTalangan = danaTalangan;
	}

	/**
	 * Jenis/kategori posting jurnal (mis. {@code PIUTANG_SISWA},
	 * {@code PEMBAYARAN_SISWA_DIBAYAR_DIMUKA}, {@code PIUTANG_DENDA_SISWA},
	 * {@code UTANG_DISKON_SISWA} — lihat {@code PostingJurnalHelper.JENIS_CLOSING}).
	 *
	 * <p><b>Getter dengan WRITE-BACK:</b> bila {@link #getPostingHistory()} terisi,
	 * field {@code jenis} <b>ditimpa</b> dengan {@code postingHistory.getJenis()}. Kolom
	 * {@code jenis} karena itu tidak pernah bisa menyimpang dari riwayat postingnya, dan
	 * membaca jurnal dalam session hidup dapat menerbitkan {@code UPDATE}.</p>
	 *
	 * <p><b>PENTING untuk pembeda kaki jurnal:</b> justru karena {@code jenis} hanyalah
	 * turunan dari {@code postingHistory}, kolom ini <b>TIDAK ikut menyusun</b>
	 * {@link #ambilUnik()}. Dua kaki jurnal yang berbeda {@code jenis}-nya tetap dianggap
	 * duplikat bila {@code ref}-nya sama. Pembeda kaki yang sah hanyalah
	 * {@link #getRef()}.</p>
	 *
	 * @return jenis posting, atau {@code null} untuk jurnal draft/manual.
	 */
	public String getJenis() {
		if (postingHistory != null) {
			jenis = postingHistory.getJenis();
		}
		return jenis;
	}

	/**
	 * Menyetel jenis posting jurnal.
	 *
	 * <p>Nilai yang disetel di sini akan <b>ditimpa</b> oleh {@link #getJenis()} pada
	 * pembacaan berikutnya bila {@code postingHistory} terisi.</p>
	 *
	 * @param jenis jenis posting.
	 */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/**
	 * Referensi dokumen sumber: perjanjian kerja sama aset. Ikut menyusun
	 * {@link #ambilUnik()}.
	 *
	 * @return dokumen perjanjian kerja sama aset asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "perjanjian_kerjasama_master_asset", nullable = true)
	public PerjanjianKerjasamaMasterAsset getPerjanjianKerjasamaMasterAsset() {
		return perjanjianKerjasamaMasterAsset;
	}

	/**
	 * Menyetel referensi perjanjian kerja sama aset.
	 *
	 * @param perjanjianKerjasamaMasterAsset dokumen perjanjian kerja sama asal jurnal.
	 */
	public void setPerjanjianKerjasamaMasterAsset(PerjanjianKerjasamaMasterAsset perjanjianKerjasamaMasterAsset) {
		this.perjanjianKerjasamaMasterAsset = perjanjianKerjasamaMasterAsset;
	}

	/**
	 * Referensi dokumen sumber: slip gaji per pegawai. Ikut menyusun
	 * {@link #ambilUnik()} dengan nama kelasnya sendiri — lihat riwayat tabrakan yang
	 * sudah diperbaiki pada {@link #getTransaksiPegawai()}.
	 *
	 * @return slip gaji per pegawai asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pembayaran_gaji_punya_pegawai", nullable = true)
	public PembayaranGajiPunyaPegawai getPembayaranGajiPunyaPegawai() {
		return pembayaranGajiPunyaPegawai;
	}

	/**
	 * Menyetel referensi slip gaji per pegawai.
	 *
	 * @param pembayaranGajiPunyaPegawai slip gaji per pegawai asal jurnal.
	 */
	public void setPembayaranGajiPunyaPegawai(PembayaranGajiPunyaPegawai pembayaranGajiPunyaPegawai) {
		this.pembayaranGajiPunyaPegawai = pembayaranGajiPunyaPegawai;
	}

	/**
	 * Referensi dokumen sumber: detail pembayaran pengadaan aset. Ikut menyusun
	 * {@link #ambilUnik()} dan menjadi sumber turunan {@link #getAngarans()},
	 * {@link #ambilDaftarPengajuanTransfer()} serta {@link #ambilDisposisiSop()}.
	 *
	 * @return detail pembayaran pengadaan aset asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pembayaran_pengadaan_master_asset_detail", nullable = true)
	public PembayaranPengadaanMasterAssetDetail getPembayaranPengadaanMasterAssetDetail() {
		return pembayaranPengadaanMasterAssetDetail;
	}

	/**
	 * Menyetel referensi detail pembayaran pengadaan aset.
	 *
	 * @param pembayaranPengadaanMasterAssetDetail detail pembayaran pengadaan asal
	 *        jurnal.
	 */
	public void setPembayaranPengadaanMasterAssetDetail(
			PembayaranPengadaanMasterAssetDetail pembayaranPengadaanMasterAssetDetail) {
		this.pembayaranPengadaanMasterAssetDetail = pembayaranPengadaanMasterAssetDetail;
	}

	/**
	 * Daftar id anggaran terkait jurnal, dalam bentuk string dipisah koma (kolom
	 * {@code text}).
	 *
	 * <p><b>Getter dengan WRITE-BACK ganda.</b> Method ini (a) menormalkan nilai —
	 * {@code null} menjadi string kosong, di-{@code trim}, dan string yang hanya berisi
	 * koma ({@code ","} sampai {@code ",,,,"}) dikosongkan; lalu (b) <b>menyalin ulang</b>
	 * daftar anggaran dari dokumen pengadaan aset bila jurnal ini bereferensi ke
	 * pembayaran pengadaan / DP / termin. Hasilnya ditugaskan kembali ke field
	 * terpetakan, sehingga membaca jurnal di dalam session hidup dapat menerbitkan
	 * {@code UPDATE} pada kolom {@code angarans} — dan nilai yang pernah disetel manual
	 * lewat {@link #setAngarans(String)} akan hilang bila dokumen aset menyediakan
	 * nilainya sendiri.</p>
	 *
	 * <p>Seluruh blok penyalinan dibungkus {@code try/catch} karena rantai
	 * {@code getPemesananPengadaanMasterAsset()} bisa menyentuh proxy yang session-nya
	 * sudah tertutup ({@code LazyInitializationException}); bila itu terjadi, nilai
	 * cadangan hasil normalisasi dipertahankan dan render jurnal tetap berjalan.</p>
	 *
	 * @return daftar id anggaran dipisah koma; tidak pernah {@code null}.
	 */
	@Column(columnDefinition = "text")
	public String getAngarans() {
		angarans = (angarans == null ? "" : angarans.trim());

		if (angarans.equals(",")) {
			angarans = "";
		} else if (angarans.equals(",,")) {
			angarans = "";
		} else if (angarans.equals(",,,")) {
			angarans = "";
		} else if (angarans.equals(",,,,")) {
			angarans = "";
		}

		try {
			// FIX LazyInitializationException: getPembayaranPengadaanMasterAssetDetail()/
			// getPembayaranDpMasterAssetDetail()/getPembayaranTerminMasterAssetDetail() bisa
			// berupa instance canonical/shared (AuditTimestampInterceptor) yang proxy rantai
			// getPemesananPengadaanMasterAsset()-nya terikat ke Session lain yang sudah
			// closed -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai
			// fallback dipertahankan).
			if (getPembayaranPengadaanMasterAssetDetail() != null && getPembayaranPengadaanMasterAssetDetail()
					.getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset() != null) {
				angarans = getPembayaranPengadaanMasterAssetDetail().getPenerimaanPengadaanMasterAsset()
						.getPemesananPengadaanMasterAsset().getAngarans();
			}

			if (getPembayaranDpMasterAssetDetail() != null
					&& getPembayaranDpMasterAssetDetail().getPemesananPengadaanMasterAsset() != null) {
				angarans = getPembayaranDpMasterAssetDetail().getPemesananPengadaanMasterAsset().getAngarans();
			}

			if (getPembayaranTerminMasterAssetDetail() != null
					&& getPembayaranTerminMasterAssetDetail().getPemesananPengadaanMasterAsset() != null
					&& getPembayaranTerminMasterAssetDetail().getPemesananPengadaanMasterAsset().getAngarans() != null) {
				angarans = getPembayaranTerminMasterAssetDetail().getPemesananPengadaanMasterAsset().getAngarans();
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/akunting/GrupTransaksi.java:getAngarans-lazy");
		}

		return angarans;
	}

	/**
	 * Menyetel daftar id anggaran terkait jurnal.
	 *
	 * @param angarans daftar id anggaran dipisah koma; dapat ditimpa oleh
	 *        {@link #getAngarans()} — lihat catatan di sana.
	 */
	public void setAngarans(String angarans) {
		this.angarans = angarans;
	}

	/**
	 * Referensi dokumen sumber: detail pembayaran DP aset. Ikut menyusun
	 * {@link #ambilUnik()} dan menjadi sumber turunan {@link #getAngarans()}.
	 *
	 * @return detail pembayaran DP aset asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pembayaran_dp_master_asset_detail", nullable = true)
	public PembayaranDpMasterAssetDetail getPembayaranDpMasterAssetDetail() {
		return pembayaranDpMasterAssetDetail;
	}

	/**
	 * Menyetel referensi detail pembayaran DP aset.
	 *
	 * @param pembayaranDpMasterAssetDetail detail pembayaran DP aset asal jurnal.
	 */
	public void setPembayaranDpMasterAssetDetail(PembayaranDpMasterAssetDetail pembayaranDpMasterAssetDetail) {
		this.pembayaranDpMasterAssetDetail = pembayaranDpMasterAssetDetail;
	}

	/**
	 * Referensi dokumen sumber: detail pembayaran termin aset. Ikut menyusun
	 * {@link #ambilUnik()} dan menjadi sumber turunan {@link #getAngarans()}.
	 *
	 * @return detail pembayaran termin aset asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pembayaran_termin_master_asset_detail", nullable = true)
	public PembayaranTerminMasterAssetDetail getPembayaranTerminMasterAssetDetail() {
		return pembayaranTerminMasterAssetDetail;
	}

	/**
	 * Menyetel referensi detail pembayaran termin aset.
	 *
	 * @param pembayaranTerminMasterAssetDetail detail pembayaran termin aset asal jurnal.
	 */
	public void setPembayaranTerminMasterAssetDetail(
			PembayaranTerminMasterAssetDetail pembayaranTerminMasterAssetDetail) {
		this.pembayaranTerminMasterAssetDetail = pembayaranTerminMasterAssetDetail;
	}

	/**
	 * Referensi dokumen sumber: daftar pengajuan transfer. Ikut menyusun
	 * {@link #ambilUnik()}.
	 *
	 * <p>Untuk mendapatkan pengajuan transfer <b>efektif</b> — termasuk yang diwarisi
	 * dari dokumen induk — pakai {@link #ambilDaftarPengajuanTransfer()}, bukan getter
	 * ini.</p>
	 *
	 * @return pengajuan transfer yang tertaut langsung, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "daftar_pengajuan_transfer", nullable = true)
	public DaftarPengajuanTransfer getDaftarPengajuanTransfer() {
		return daftarPengajuanTransfer;
	}

	/**
	 * Menyetel referensi daftar pengajuan transfer.
	 *
	 * @param daftarPengajuanTransfer dokumen pengajuan transfer asal jurnal.
	 */
	public void setDaftarPengajuanTransfer(DaftarPengajuanTransfer daftarPengajuanTransfer) {
		this.daftarPengajuanTransfer = daftarPengajuanTransfer;
	}

	/**
	 * <b>Pembeda KAKI jurnal</b> pada satu dokumen sumber.
	 *
	 * <p>Bersama kelas+id dokumen sumber, nilai inilah yang menyusun kunci idempotensi
	 * {@link #ambilUnik()}. Aturan bakunya: <b>kaki utama {@code null}</b>, setiap kaki
	 * tambahan memakai penanda sendiri yang berbeda — konstanta {@code REF_*} pada
	 * {@code ais.action.master.helper.PostingJurnalHelper}
	 * ({@code "dimuka"}, {@code "pengembalian"}, {@code "pajak"},
	 * {@code "payment_gateway"}, {@code "DP_PEKERJAAN"},
	 * {@code "DP_BALIK_PEKERJAAN"}, {@code "denda_siswa"}, {@code "diskon_siswa"},
	 * {@code "dimuka_siswa"}).</p>
	 *
	 * <p><b>Jebakan {@code NULL} pada pembatalan.</b> Kaki utama tersimpan sebagai
	 * {@code NULL}, dan dalam SQL {@code ref != 'x'} pada baris {@code NULL} bernilai
	 * {@code NULL} (bukan {@code true}). Kondisi hapus harus ditulis
	 * {@code (ref is null or ref != 'x')} — atau {@code ref is not null and ref != 'x'}
	 * bila memang hanya kaki tambahan yang disasar — kalau tidak, kaki utama tidak
	 * pernah ikut terhapus dan menjadi jurnal yatim setelah cap postingnya dilepas.</p>
	 *
	 * @return penanda kaki jurnal, atau {@code null} untuk kaki utama.
	 */
	public String getRef() {
		return ref;
	}

	/**
	 * Menyetel penanda kaki jurnal.
	 *
	 * <p>Dipanggil {@code CommonAkunting.saveTransaksi} sebelum kunci idempotensi
	 * dihitung. Mengubah nilai ini <b>mengubah identitas logis jurnal</b>: dua kaki pada
	 * dokumen yang sama dengan {@code ref} identik akan saling menghapus (kaki kedua
	 * tidak pernah tertulis).</p>
	 *
	 * @param ref penanda kaki jurnal; {@code null} untuk kaki utama.
	 */
	public void setRef(String ref) {
		this.ref = ref;
	}

	/**
	 * Referensi dokumen sumber: transitori (rekening antara/penampung sementara). Ikut
	 * menyusun {@link #ambilUnik()}.
	 *
	 * @return dokumen transitori asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "transitori", nullable = true)
	public Transitori getTransitori() {
		return transitori;
	}

	/**
	 * Menyetel referensi transitori.
	 *
	 * @param transitori dokumen transitori asal jurnal.
	 */
	public void setTransitori(Transitori transitori) {
		this.transitori = transitori;
	}

	/**
	 * Menyusun <b>kunci idempotensi</b> jurnal ini: {@code <nama kelas dokumen
	 * sumber> + "_" + <id dokumen sumber> + <ref>}.
	 *
	 * <p>Inilah method paling menentukan pada seluruh mesin posting AIS.
	 * {@code CommonAkunting.saveTransaksi} memakai hasilnya untuk memutuskan apakah
	 * sebuah jurnal <b>ditulis</b> atau cukup <b>dicap ulang</b>: bila sudah ada
	 * {@code GrupTransaksi} lain dengan {@code kodeUnik} sama, grup lama hanya diberi
	 * {@code postingHistory} baru dan <b>tidak satu pun baris jurnal baru ditulis</b>.</p>
	 *
	 * <h4>Cara kerja</h4>
	 * <p>Satu rantai {@code if/else if} memeriksa 26 field referensi <b>secara langsung
	 * (bukan lewat getter)</b> dan berhenti pada yang pertama non-{@code null}; nama
	 * kelas dan id dokumen itu menjadi awalan kunci. Terakhir {@code ref} yang sudah
	 * di-{@code trim} ditempelkan tanpa pemisah. Karena akses langsung ke field, method
	 * ini tidak mengaktifkan proxy lazy — konsisten dengan pemakaiannya dari
	 * {@link #getKodeUnik()} yang bisa dipanggil Hibernate saat flush.</p>
	 *
	 * <h4>Urutan pemeriksaan berarti</h4>
	 * <p>Bila lebih dari satu kolom referensi terisi, hanya yang <b>pertama</b> dalam
	 * urutan rantai yang menentukan kunci. {@code CommonAkunting.saveTransaksi} memang
	 * hanya mengisi satu kolom, tetapi jurnal yang diedit manual bisa memiliki lebih dari
	 * satu.</p>
	 *
	 * <h4>Batasan yang WAJIB diketahui sebelum menambah jenis jurnal baru</h4>
	 * <ol>
	 *   <li><b>Kolom {@code jenis} tidak ikut kunci.</b> {@link #getJenis()} hanya
	 *   cerminan {@code postingHistory.jenis}, jadi dua kaki jurnal berbeda jenis pada
	 *   dokumen yang sama tetap bertabrakan. Satu-satunya pembeda kaki adalah
	 *   {@link #getRef()}.</li>
	 *   <li><b>Cakupan tidak lengkap — 15 dari 41 kolom referensi tidak dikenali.</b>
	 *   {@code logPembayaran}, {@code penerimaanPengadaanMasterAsset},
	 *   {@code saldoAwalMasterAsset}, {@code pertangungjawabanKasBesar},
	 *   {@code transaksiKoperasi}, {@code pajak}, {@code pembayaranGaji},
	 *   {@code pembatalanTransaksiKantin}, {@code penghapusanMasterAsset},
	 *   {@code pembayaranAnggotaKoperasi}, {@code pencairanDiskon},
	 *   {@code penyesuaianSaldoAnggota}, {@code modalPenyertaanKoperasi},
	 *   {@code pembagianShu}, dan {@code notaSalesBiaya} tidak menyumbang apa pun ke
	 *   kunci. Konsekuensinya bercabang dua:
	 *   <ul>
	 *     <li>bila {@code ref} juga kosong, kunci menjadi string kosong dan
	 *     {@link #getKodeUnik()} mengembalikan {@code null} — pencocokan
	 *     {@code kode_unik = NULL} tidak pernah cocok, sehingga <b>setiap posting ulang
	 *     menghasilkan jurnal duplikat</b>;</li>
	 *     <li>bila {@code ref} terisi, kunci menjadi <b>string {@code ref} telanjang</b>
	 *     — mis. {@code "pajak"} untuk seluruh jurnal pajak LPJ
	 *     ({@code PostingPertangungjawabanPajakAction}) dan {@code "DP_PEKERJAAN"} untuk
	 *     seluruh jurnal DP pekerjaan vendor
	 *     ({@code PostingDpPemesananPekerjaanAction}) — sehingga kunci itu berlaku
	 *     <b>global untuk seluruh instalasi dan seluruh tenant</b>. Hanya jurnal pertama
	 *     yang pernah tertulis dengan {@code ref} tersebut yang benar-benar ada; posting
	 *     berikutnya dianggap duplikat, jurnalnya tidak ditulis, dan cap posting dokumen
	 *     lain ikut tertimpa.</li>
	 *   </ul>
	 *   Karena itu <b>setiap penambahan kolom referensi baru wajib disertai penambahan
	 *   cabang di method ini</b>.</li>
	 * </ol>
	 *
	 * <p><b>Tanpa efek samping:</b> method ini murni menghitung dan tidak mengubah state
	 * apa pun (berbeda dari {@link #getKodeUnik()} yang menugaskan hasilnya ke field).</p>
	 *
	 * @return kunci idempotensi; string kosong bila tidak ada satu pun kolom referensi
	 *         yang dikenali dan {@code ref} juga kosong.
	 * @see #getKodeUnik()
	 * @see #getRef()
	 * @see ais.action.master.helper.PostingJurnalHelper
	 */
	public String ambilUnik() {
		String ko = "";

		if (cicilanPembayaran != null) {
			ko = CicilanPembayaran.class.getName() + "_" + cicilanPembayaran.getId();
		} else if (detailKegiatan != null) {
			ko = DetailKegiatan.class.getName() + "_" + detailKegiatan.getId();
		} else if (pembayaranSiswaDetail != null) {
			ko = PembayaranSiswaDetail.class.getName() + "_" + pembayaranSiswaDetail.getId();
		} else if (depositSiswa != null) {
			ko = DepositSiswa.class.getName() + "_" + depositSiswa.getId();
		} else if (pembayaranGajiPunyaPegawai != null) {
			ko = PembayaranGajiPunyaPegawai.class.getName() + "_" + pembayaranGajiPunyaPegawai.getId();
		} else if (transaksiPegawai != null) {
			ko = TransaksiPegawai.class.getName() + "_" + transaksiPegawai.getId();
		} else if (penerimaanPengadaanMasterAssetDetail != null) {
			ko = PenerimaanPengadaanMasterAssetDetail.class.getName() + "_"
					+ penerimaanPengadaanMasterAssetDetail.getId();
		} else if (saldoAwalMasterAssetDetail != null) {
			ko = SaldoAwalMasterAssetDetail.class.getName() + "_" + saldoAwalMasterAssetDetail.getId();
		} else if (penyusutanAsset != null) {
			ko = PenyusutanAsset.class.getName() + "_" + penyusutanAsset.getId();
		} else if (pemesananPengadaanMasterAsset != null) {
			ko = PemesananPengadaanMasterAsset.class.getName() + "_" + pemesananPengadaanMasterAsset.getId();
		} else if (deposit != null) {
			ko = Deposit.class.getName() + "_" + deposit.getId();
		} else if (tagihan != null) {
			ko = Tagihan.class.getName() + "_" + tagihan.getId();
		} else if (pengeluaranMahasiswa != null) {
			ko = PengeluaranMahasiswa.class.getName() + "_" + pengeluaranMahasiswa.getId();
		} else if (pertangungjawaban != null) {
			ko = Pertangungjawaban.class.getName() + "_" + pertangungjawaban.getId();
		} else if (uangMuka != null) {
			ko = UangMuka.class.getName() + "_" + uangMuka.getId();
		} else if (kasKecil != null) {
			ko = KasKecil.class.getName() + "_" + kasKecil.getId();
		} else if (kasBesar != null) {
			ko = KasBesar.class.getName() + "_" + kasBesar.getId();
		} else if (penggantianKasKecil != null) {
			ko = PenggantianKasKecil.class.getName() + "_" + penggantianKasKecil.getId();
		} else if (jenisKasKecil != null) {
			ko = JenisKasKecil.class.getName() + "_" + jenisKasKecil.getId();
		} else if (danaTalangan != null) {
			ko = DanaTalangan.class.getName() + "_" + danaTalangan.getId();
		} else if (pembayaranPengadaanMasterAssetDetail != null) {
			ko = PembayaranPengadaanMasterAssetDetail.class.getName() + "_"
					+ pembayaranPengadaanMasterAssetDetail.getId();
		} else if (pembayaranDpMasterAssetDetail != null) {
			ko = PembayaranDpMasterAssetDetail.class.getName() + "_" + pembayaranDpMasterAssetDetail.getId();
		} else if (pembayaranTerminMasterAssetDetail != null) {
			ko = PembayaranTerminMasterAssetDetail.class.getName() + "_" + pembayaranTerminMasterAssetDetail.getId();
		} else if (perjanjianKerjasamaMasterAsset != null) {
			ko = PerjanjianKerjasamaMasterAsset.class.getName() + "_" + perjanjianKerjasamaMasterAsset.getId();
		} else if (daftarPengajuanTransfer != null) {
			ko = DaftarPengajuanTransfer.class.getName() + "_" + daftarPengajuanTransfer.getId();
		} else if (transitori != null) {
			ko = Transitori.class.getName() + "_" + transitori.getId();
		}

		return ko + (ref == null || ref.trim().isEmpty() ? "" : ref.trim());
	}

	/**
	 * Kunci idempotensi jurnal yang dipetakan ke kolom {@code kode_unik}
	 * ({@code unique = true}).
	 *
	 * <p><b>Getter dengan WRITE-BACK — bukan pembaca murni.</b> Setiap pembacaan
	 * menghitung ulang kunci lewat {@link #ambilUnik()} dan menugaskannya kembali ke
	 * field. Akibatnya:</p>
	 * <ul>
	 *   <li>nilai yang tersimpan di database <b>tidak pernah bisa menyimpang</b> dari
	 *   hasil hitung — koreksi manual di database akan tertimpa pada flush berikutnya;</li>
	 *   <li>{@link #setKodeUnik(String)} praktis tidak berpengaruh;</li>
	 *   <li>bila kolom referensi jurnal diubah, kunci ikut berubah dan Hibernate akan
	 *   menerbitkan {@code UPDATE} pada kolom bertanda {@code unique} — yang dapat gagal
	 *   dengan pelanggaran batasan bila kunci baru sudah dipakai baris lain.</li>
	 * </ul>
	 *
	 * <p>Nilai kosong dinormalkan menjadi {@code null} sehingga baris tanpa dokumen
	 * sumber yang dikenali tidak saling bertabrakan pada batasan {@code unique} — namun
	 * konsekuensinya baris-baris itu juga <b>tidak pernah terdeteksi sebagai duplikat</b>
	 * oleh {@code CommonAkunting.saveTransaksi} ({@code kode_unik = NULL} tidak pernah
	 * cocok). Lihat peringatan lengkap pada {@link #ambilUnik()}.</p>
	 *
	 * @return kunci idempotensi, atau {@code null} bila tidak ada dokumen sumber yang
	 *         dikenali dan {@code ref} kosong.
	 * @see #ambilUnik()
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		kodeUnik = ambilUnik();
		return kodeUnik == null || kodeUnik.trim().isEmpty() ? null : kodeUnik;
	}

	/**
	 * Menyetel kunci idempotensi.
	 *
	 * <p><b>Praktis tidak berpengaruh:</b> {@link #getKodeUnik()} menghitung ulang nilai
	 * ini pada setiap pembacaan. Setter tetap ada karena dibutuhkan Hibernate saat
	 * memuat baris dari database.</p>
	 *
	 * @param kodeUnik kunci idempotensi dari database.
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * Referensi dokumen sumber: kas besar. Ikut menyusun {@link #ambilUnik()} dan menjadi
	 * sumber turunan satuan kerja, pengajuan transfer, serta disposisi SOP.
	 *
	 * @return dokumen kas besar asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kas_besar", nullable = true)
	public KasBesar getKasBesar() {
		return kasBesar;
	}

	/**
	 * Menyetel referensi kas besar.
	 *
	 * @param kasBesar dokumen kas besar asal jurnal.
	 */
	public void setKasBesar(KasBesar kasBesar) {
		this.kasBesar = kasBesar;
	}

	/**
	 * Menelusuri <b>pengajuan transfer efektif</b> jurnal ini — yaitu pengajuan transfer
	 * milik dokumen sumber, bukan sekadar yang tertaut langsung.
	 *
	 * <p>Dimulai dari {@link #getDaftarPengajuanTransfer()} sebagai nilai awal, lalu satu
	 * rantai {@code else if} menelusuri dokumen sumber sesuai urutan prioritas: uang muka
	 * → dana talangan → kas besar → penggantian kas kecil (lewat kas kecil) →
	 * penggantian kas kecil langsung → jenis kas kecil → LPJ → pembayaran pengadaan /
	 * DP / termin aset → transitori → LPJ milik pajak.</p>
	 *
	 * <p><b>Kasus tepi:</b> karena rantai memakai {@code else if}, cabang pertama yang
	 * cocok menang; nilai awal dari relasi langsung hanya bertahan bila TIDAK ada satu
	 * pun cabang yang cocok. Cabang terakhir ({@code pajak}) mengakses field
	 * {@code pajak} secara langsung tanpa {@code check(...)} dan dapat mengembalikan
	 * {@code null} meski cabangnya terpilih.</p>
	 *
	 * <p>Dipakai {@link #populateDeskripsiLengkap()} untuk memasang tautan ke layar
	 * proses transfer di bawah tabel jurnal.</p>
	 *
	 * @return pengajuan transfer efektif, atau {@code null} bila jurnal tidak berasal
	 *         dari alur transfer.
	 */
	public DaftarPengajuanTransfer ambilDaftarPengajuanTransfer() {
		DaftarPengajuanTransfer hasil = getDaftarPengajuanTransfer();
		if (getUangMuka() != null && uangMuka.getDaftarPengajuanTransfer() != null) {
			hasil = uangMuka.getDaftarPengajuanTransfer();
		} else if (getDanaTalangan() != null && danaTalangan.getDaftarPengajuanTransfer() != null) {
			hasil = danaTalangan.getDaftarPengajuanTransfer();
		} else if (getKasBesar() != null && kasBesar.getDaftarPengajuanTransfer() != null) {
			hasil = kasBesar.getDaftarPengajuanTransfer();
		} else if (getKasKecil() != null && kasKecil.getPenggantianKasKecil() != null
				&& kasKecil.getPenggantianKasKecil().getDaftarPengajuanTransfer() != null) {
			hasil = kasKecil.getPenggantianKasKecil().getDaftarPengajuanTransfer();
		} else if (getPenggantianKasKecil() != null && penggantianKasKecil.getDaftarPengajuanTransfer() != null) {
			hasil = penggantianKasKecil.getDaftarPengajuanTransfer();
		} else if (getJenisKasKecil() != null && jenisKasKecil.getDaftarPengajuanTransfer() != null) {
			hasil = jenisKasKecil.getDaftarPengajuanTransfer();
		} else if (getPertangungjawaban() != null && pertangungjawaban.getDaftarPengajuanTransfer() != null) {
			hasil = pertangungjawaban.getDaftarPengajuanTransfer();
		} else if (getPembayaranPengadaanMasterAssetDetail() != null
				&& pembayaranPengadaanMasterAssetDetail.getDaftarPengajuanTransfer() != null) {
			hasil = pembayaranPengadaanMasterAssetDetail.getDaftarPengajuanTransfer();
		} else if (getPembayaranDpMasterAssetDetail() != null
				&& pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer() != null) {
			hasil = pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer();
		} else if (getPembayaranTerminMasterAssetDetail() != null
				&& pembayaranTerminMasterAssetDetail.getDaftarPengajuanTransfer() != null) {
			hasil = pembayaranTerminMasterAssetDetail.getDaftarPengajuanTransfer();
		} else if (getTransitori() != null && transitori.getDaftarPengajuanTransfer() != null) {
			hasil = transitori.getDaftarPengajuanTransfer();
		} else if (pajak != null && pajak.getPertangungjawaban() != null) {
			hasil = pajak.getPertangungjawaban().getDaftarPengajuanTransfer();
		}

		return hasil;
	}

	/**
	 * Menelusuri <b>disposisi SOP efektif</b> jurnal ini — dokumen alur persetujuan yang
	 * mendasari transaksi.
	 *
	 * <p>Rantai prioritasnya: uang muka → dana talangan → kas besar → penggantian kas
	 * kecil (lewat kas kecil) → penggantian kas kecil langsung → LPJ → pembayaran
	 * pengadaan / DP / termin aset (lewat kepala dokumennya) →
	 * {@link #ambilDaftarPengajuanTransfer()} → LPJ milik pajak → penerimaan pengadaan
	 * lewat rantai {@code pajak → saldoAwalMasterAssetDetail →
	 * penerimaanPengadaanMasterAssetDetail}.</p>
	 *
	 * <p><b>Catatan biaya:</b> berbeda dari {@link #ambilDaftarPengajuanTransfer()},
	 * cabang kesembilan memanggil {@code ambilDaftarPengajuanTransfer()}
	 * <b>dua kali</b> (sekali untuk memeriksa, sekali untuk mengambil), sehingga seluruh
	 * rantai penelusuran itu dijalankan ulang. Tidak salah secara hasil, hanya boros bila
	 * dipanggil per baris pada daftar panjang.</p>
	 *
	 * <p>Dipakai {@link #populateDeskripsiLengkap()} untuk memasang tautan pembuka alur
	 * SOP di bawah tabel jurnal.</p>
	 *
	 * @return disposisi SOP efektif, atau {@code null} bila jurnal tidak berasal dari
	 *         alur SOP.
	 */
	public DisposisiSop ambilDisposisiSop() {

		DisposisiSop hasil = null;
		if (getUangMuka() != null && uangMuka.getDisposisiSop() != null) {
			hasil = uangMuka.getDisposisiSop();
		} else if (getDanaTalangan() != null && danaTalangan.getDisposisiSop() != null) {
			hasil = danaTalangan.getDisposisiSop();
		} else if (getKasBesar() != null && kasBesar.getDisposisiSop() != null) {
			hasil = kasBesar.getDisposisiSop();
		} else if (getKasKecil() != null && kasKecil.getPenggantianKasKecil() != null
				&& kasKecil.getPenggantianKasKecil().getDisposisiSop() != null) {
			hasil = kasKecil.getPenggantianKasKecil().getDisposisiSop();
		} else if (getPenggantianKasKecil() != null && penggantianKasKecil.getDisposisiSop() != null) {
			hasil = penggantianKasKecil.getDisposisiSop();
		} else if (getPertangungjawaban() != null && pertangungjawaban.getDisposisiSop() != null) {
			hasil = pertangungjawaban.getDisposisiSop();
		} else if (getPembayaranPengadaanMasterAssetDetail() != null
				&& pembayaranPengadaanMasterAssetDetail.getPembayaranPengadaanMasterAsset() != null
				&& pembayaranPengadaanMasterAssetDetail.getPembayaranPengadaanMasterAsset().getDisposisiSop() != null) {
			hasil = pembayaranPengadaanMasterAssetDetail.getPembayaranPengadaanMasterAsset().getDisposisiSop();
		} else if (getPembayaranDpMasterAssetDetail() != null
				&& pembayaranDpMasterAssetDetail.getPembayaranDpMasterAsset() != null
				&& pembayaranDpMasterAssetDetail.getPembayaranDpMasterAsset().getDisposisiSop() != null) {
			hasil = pembayaranDpMasterAssetDetail.getPembayaranDpMasterAsset().getDisposisiSop();
		} else if (getPembayaranTerminMasterAssetDetail() != null
				&& pembayaranTerminMasterAssetDetail.getPembayaranTerminMasterAsset() != null
				&& pembayaranTerminMasterAssetDetail.getPembayaranTerminMasterAsset().getDisposisiSop() != null) {
			hasil = pembayaranTerminMasterAssetDetail.getPembayaranTerminMasterAsset().getDisposisiSop();
		} else if (ambilDaftarPengajuanTransfer() != null && ambilDaftarPengajuanTransfer().getDisposisiSop() != null) {
			hasil = ambilDaftarPengajuanTransfer().getDisposisiSop();
		} else if (pajak != null && pajak.getPertangungjawaban() != null) {
			hasil = pajak.getPertangungjawaban().getDisposisiSop();
		} else if (pajak != null && pajak.getSaldoAwalMasterAssetDetail() != null
				&& pajak.getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail() != null
				&& pajak.getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail()
						.getPenerimaanPengadaanMasterAsset() != null) {
			hasil = pajak.getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail()
					.getPenerimaanPengadaanMasterAsset().getDisposisiSop();
		}

		return hasil;
	}

	/**
	 * Menyetel bulan periode jurnal (1–12).
	 *
	 * <p>Karena penurunan otomatis pada {@link #getBulan()} tidak berfungsi, nilai ini
	 * <b>harus</b> disetel eksplisit oleh pemanggil bila laporan menyaring berdasarkan
	 * bulan.</p>
	 *
	 * @param bulan bulan periode (1–12).
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Bulan periode jurnal.
	 *
	 * <p><b>PERINGATAN — penjaga terbalik, penurunan otomatis tidak pernah jalan.</b>
	 * Blok penurunan hanya dieksekusi ketika {@code getTanggalTransaksi() == null},
	 * padahal di dalamnya {@code calendar.setTime(tanggalTransaksi)} justru membutuhkan
	 * tanggal yang tidak {@code null}. Akibatnya: (a) untuk jurnal bertanggal normal
	 * blok itu dilewati sehingga {@code bulan} <b>tidak pernah diturunkan</b> dari
	 * tanggal transaksi dan bernilai apa adanya (bisa {@code null}); (b) untuk jurnal
	 * tanpa tanggal, blok itu justru dijalankan dan {@code setTime(null)} melempar
	 * {@link NullPointerException}. Kondisi yang dimaksudkan hampir pasti
	 * {@code if (bulan == null && getTanggalTransaksi() != null)}. Tidak diperbaiki di
	 * sini karena mengubah nilai kolom periode akan mengubah hasil laporan lama;
	 * pemanggil yang membutuhkan periode sebaiknya menghitung sendiri dari
	 * {@link #getTanggalTransaksi()} atau memakai
	 * {@code PostingJurnalHelper.dateSql(...)} yang menyaring berdasarkan tanggal.</p>
	 *
	 * @return bulan periode sebagaimana tersimpan, bisa {@code null}.
	 */
	public Integer getBulan() {
		if (getTanggalTransaksi() == null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(tanggalTransaksi);
			bulan = calendar.get(Calendar.MONTH) + 1;
		}
		return bulan;
	}

	/**
	 * Menyetel tahun periode jurnal.
	 *
	 * <p>Sama seperti {@link #setBulan(Integer)}, nilai ini harus disetel eksplisit —
	 * lihat peringatan pada {@link #getTahun()}.</p>
	 *
	 * @param tahun tahun periode.
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Tahun periode jurnal.
	 *
	 * <p><b>PERINGATAN:</b> mengidap cacat penjaga terbalik yang persis sama dengan
	 * {@link #getBulan()} — penurunan otomatis tidak pernah jalan untuk jurnal bertanggal
	 * normal, dan melempar {@link NullPointerException} untuk jurnal tanpa tanggal. Lihat
	 * penjelasan lengkap di sana.</p>
	 *
	 * @return tahun periode sebagaimana tersimpan, bisa {@code null}.
	 */
	public Integer getTahun() {
		if (getTanggalTransaksi() == null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(tanggalTransaksi);
			tahun = calendar.get(Calendar.YEAR);
		}
		return tahun;
	}

	/**
	 * Referensi dokumen sumber: pajak (jurnal pemotongan/penyetoran pajak atas LPJ).
	 *
	 * <p><b>TIDAK ikut menyusun {@link #ambilUnik()}.</b> Karena jalur postingnya memakai
	 * {@code ref = "pajak"}, kunci idempotensi jurnal pajak menjadi string
	 * {@code "pajak"} telanjang yang berlaku global untuk seluruh instalasi — lihat
	 * peringatan lengkap pada {@link #ambilUnik()}.</p>
	 *
	 * <p>Diakses secara langsung (tanpa {@code check(...)}) oleh
	 * {@link #ambilDaftarPengajuanTransfer()} dan {@link #ambilDisposisiSop()}.</p>
	 *
	 * @return dokumen pajak asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pajak", nullable = true)
	public Pajak getPajak() {
		return pajak;
	}

	/**
	 * Menyetel referensi pajak.
	 *
	 * @param pajak dokumen pajak asal jurnal.
	 */
	public void setPajak(Pajak pajak) {
		this.pajak = pajak;
	}

	/**
	 * Referensi dokumen sumber: kepala penerimaan pengadaan aset. <b>TIDAK ikut</b>
	 * menyusun {@link #ambilUnik()} — berbeda dari
	 * {@link #getPenerimaanPengadaanMasterAssetDetail()} yang ikut.
	 *
	 * @return kepala penerimaan pengadaan aset asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penerimaan_pengadaan_master_asset", nullable = true)
	public PenerimaanPengadaanMasterAsset getPenerimaanPengadaanMasterAsset() {
		return penerimaanPengadaanMasterAsset;
	}

	/**
	 * Menyetel referensi kepala penerimaan pengadaan aset.
	 *
	 * @param penerimaanPengadaanMasterAsset kepala penerimaan pengadaan asal jurnal.
	 */
	public void setPenerimaanPengadaanMasterAsset(PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset) {
		this.penerimaanPengadaanMasterAsset = penerimaanPengadaanMasterAsset;
	}

	/**
	 * Referensi dokumen sumber: LPJ kas besar. <b>TIDAK ikut</b> menyusun
	 * {@link #ambilUnik()}.
	 *
	 * @return LPJ kas besar asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pertangungjawaban_kas_besar", nullable = true)
	public PertangungjawabanKasBesar getPertangungjawabanKasBesar() {
		return pertangungjawabanKasBesar;
	}

	/**
	 * Menyetel referensi LPJ kas besar.
	 *
	 * @param pertangungjawabanKasBesar LPJ kas besar asal jurnal.
	 */
	public void setPertangungjawabanKasBesar(PertangungjawabanKasBesar pertangungjawabanKasBesar) {
		this.pertangungjawabanKasBesar = pertangungjawabanKasBesar;
	}

	/**
	 * Referensi dokumen sumber: kepala pembayaran gaji (satu periode penggajian).
	 * <b>TIDAK ikut</b> menyusun {@link #ambilUnik()} — berbeda dari
	 * {@link #getPembayaranGajiPunyaPegawai()} yang ikut.
	 *
	 * @return kepala pembayaran gaji asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pembayaran_gaji", nullable = true)
	public PembayaranGaji getPembayaranGaji() {
		return pembayaranGaji;
	}

	/**
	 * Menyetel referensi kepala pembayaran gaji.
	 *
	 * @param pembayaranGaji kepala pembayaran gaji asal jurnal.
	 */
	public void setPembayaranGaji(PembayaranGaji pembayaranGaji) {
		this.pembayaranGaji = pembayaranGaji;
	}

	/**
	 * Referensi dokumen sumber: kepala saldo awal aset — dipakai juga sebagai dokumen
	 * induk jurnal DP pekerjaan vendor ({@code ref = "DP_PEKERJAAN"} /
	 * {@code "DP_BALIK_PEKERJAAN"}).
	 *
	 * <p><b>TIDAK ikut</b> menyusun {@link #ambilUnik()}, sehingga jurnal DP pekerjaan
	 * berkunci {@code "DP_PEKERJAAN"} telanjang — lihat peringatan pada
	 * {@link #ambilUnik()}.</p>
	 *
	 * @return kepala saldo awal aset asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "saldo_awal_master_asset", nullable = true)
	public SaldoAwalMasterAsset getSaldoAwalMasterAsset() {
		return saldoAwalMasterAsset;
	}

	/**
	 * Menyetel referensi kepala saldo awal aset.
	 *
	 * @param saldoAwalMasterAsset kepala saldo awal aset asal jurnal.
	 */
	public void setSaldoAwalMasterAsset(SaldoAwalMasterAsset saldoAwalMasterAsset) {
		this.saldoAwalMasterAsset = saldoAwalMasterAsset;
	}

	/**
	 * Referensi dokumen sumber: transaksi koperasi/kantin. <b>TIDAK ikut</b> menyusun
	 * {@link #ambilUnik()}.
	 *
	 * @return transaksi koperasi asal jurnal, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "transaksi_koperasi", nullable = true)
	public TransaksiKoperasi getTransaksiKoperasi() {
		return transaksiKoperasi;
	}

	/**
	 * Menyetel referensi transaksi koperasi.
	 *
	 * @param transaksiKoperasi transaksi koperasi asal jurnal.
	 */
	public void setTransaksiKoperasi(TransaksiKoperasi transaksiKoperasi) {
		this.transaksiKoperasi = transaksiKoperasi;
	}

	/**
	 * Periode tutup buku ({@link Closing}) yang mengunci jurnal ini.
	 *
	 * <p>Selama bernilai {@code null}, jurnal masih boleh dibongkar: seluruh SQL
	 * pembatalan pada {@code Posting*Action} menyertakan {@code and closing is null}.
	 * Relasi dipetakan {@code @NotFound(IGNORE)} sehingga baris closing yang sudah
	 * dihapus tidak membuat pemuatan jurnal gagal.</p>
	 *
	 * <p><b>Getter dengan WRITE-BACK — dapat MEMINDAHKAN jurnal ke periode closing
	 * lain.</b> Bila {@code closing} sudah terisi, method ini memindai <b>seluruh</b>
	 * {@link Closing} dari cache global {@code ConstantValues.ambilBerdasarClass} dan
	 * menugaskan kembali {@code this.closing} ke periode <b>terbaru</b> yang tanggalnya
	 * sama dengan atau setelah {@link #getTanggalTransaksi()}. Konsekuensinya:</p>
	 * <ul>
	 *   <li>membaca jurnal di dalam session hidup dapat menerbitkan {@code UPDATE} pada
	 *   kolom {@code closing};</li>
	 *   <li>jurnal yang sudah pernah dikunci <b>tidak akan pernah kembali</b> ke
	 *   {@code null} lewat jalur ini (penjaga {@code if (closing != null)}), jadi
	 *   pemindahan tidak membuka kunci pembatalan;</li>
	 *   <li>cache yang dipindai bersifat <b>global instalasi</b>. Ini konsisten dengan
	 *   desain {@link Closing} yang memang tidak memiliki kolom
	 *   {@code sekolah}/{@code yayasan} — periode tutup buku berlaku untuk seluruh
	 *   instalasi, bukan per tenant;</li>
	 *   <li>{@code closing.getTanggal()} diakses tanpa penjaga {@code null}; baris
	 *   {@link Closing} tanpa tanggal akan memicu {@link NullPointerException} di sini.</li>
	 * </ul>
	 *
	 * @return periode tutup buku yang berlaku, atau {@code null} bila jurnal masih
	 *         terbuka.
	 */
	@SuppressWarnings("rawtypes")
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@NotFound(action = NotFoundAction.IGNORE)
	@JoinColumn(name = "closing", nullable = true)
	public Closing getClosing() {
		closing = check(closing);

		if (closing != null) {
			Map maps = (Map) ConstantValues.ambilBerdasarClass(Closing.class);
			if (!maps.isEmpty()) {
				Date d = null;
				for (Object o : maps.values()) {
					Closing closing = (Closing) o;

					if (d == null || closing.getTanggal().after(d)) {

						if (closing != null && (Common.dateFormat83.get().format(closing.getTanggal())
								.equals(Common.dateFormat83.get().format(getTanggalTransaksi()))
								|| closing.getTanggal().after(getTanggalTransaksi()))) {
							d = closing.getTanggal();
							this.closing = closing;
						}
					}
				}
			}
		}

		return closing;
	}

	/**
	 * Menyetel periode tutup buku jurnal.
	 *
	 * <p>Menyetel {@code null} membuka kembali jurnal untuk pembatalan; menyetel nilai
	 * non-null mengunci jurnal. Nilai non-null dapat dipindahkan ke periode lain oleh
	 * {@link #getClosing()} — lihat catatan di sana.</p>
	 *
	 * @param closing periode tutup buku; {@code null} berarti jurnal terbuka.
	 */
	public void setClosing(Closing closing) {
		this.closing = closing;
	}

	/** Referensi dokumen sumber: pembatalan transaksi kantin. <b>TIDAK ikut</b>
	 * {@link #ambilUnik()}. */
	private ais.database.model.koperasi.PembatalanTransaksiKantin pembatalanTransaksiKantin;

	/**
	 * Referensi dokumen sumber: pembatalan transaksi kantin (jurnal balik penjualan
	 * kantin).
	 *
	 * <p>Satu-satunya relasi pada kelas ini yang dipetakan
	 * {@code @Audited(targetAuditMode = NOT_AUDITED)} — revisi Envers jurnal menyimpan
	 * id-nya saja tanpa merevisi entity pembatalan itu sendiri.</p>
	 *
	 * <p><b>TIDAK ikut</b> menyusun {@link #ambilUnik()}.</p>
	 *
	 * @return dokumen pembatalan transaksi kantin asal jurnal, atau {@code null}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@org.hibernate.envers.Audited(targetAuditMode = org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pembatalan_transaksi", nullable = true)
	public ais.database.model.koperasi.PembatalanTransaksiKantin getPembatalanTransaksiKantin() {
		return pembatalanTransaksiKantin;
	}

	/**
	 * Menyetel referensi pembatalan transaksi kantin.
	 *
	 * @param pembatalanTransaksiKantin dokumen pembatalan transaksi kantin asal jurnal.
	 */
	public void setPembatalanTransaksiKantin(
			ais.database.model.koperasi.PembatalanTransaksiKantin pembatalanTransaksiKantin) {
		this.pembatalanTransaksiKantin = pembatalanTransaksiKantin;
	}


	/** Referensi dokumen sumber: penghapusan/pelepasan aset. <b>TIDAK ikut</b>
	 * {@link #ambilUnik()}. */
	private ais.database.model.asset.PenghapusanMasterAsset penghapusanMasterAsset;

	/**
	 * Referensi dokumen sumber: penghapusan/pelepasan aset. <b>TIDAK ikut</b> menyusun
	 * {@link #ambilUnik()}.
	 *
	 * @return dokumen penghapusan aset asal jurnal, atau {@code null}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penghapusan_master_asset", nullable = true)
	public ais.database.model.asset.PenghapusanMasterAsset getPenghapusanMasterAsset() {
		return penghapusanMasterAsset;
	}

	/**
	 * Menyetel referensi penghapusan aset.
	 *
	 * @param penghapusanMasterAsset dokumen penghapusan aset asal jurnal.
	 */
	public void setPenghapusanMasterAsset(
			ais.database.model.asset.PenghapusanMasterAsset penghapusanMasterAsset) {
		this.penghapusanMasterAsset = penghapusanMasterAsset;
	}


	/** Referensi dokumen sumber: pembayaran anggota koperasi. <b>TIDAK ikut</b>
	 * {@link #ambilUnik()}. */
	private ais.database.model.koperasi.PembayaranAnggotaKoperasi pembayaranAnggotaKoperasi;

	/**
	 * Referensi dokumen sumber: pembayaran anggota koperasi. <b>TIDAK ikut</b> menyusun
	 * {@link #ambilUnik()}.
	 *
	 * @return dokumen pembayaran anggota koperasi asal jurnal, atau {@code null}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pembayaran_anggota_koperasi", nullable = true)
	public ais.database.model.koperasi.PembayaranAnggotaKoperasi getPembayaranAnggotaKoperasi() {
		return pembayaranAnggotaKoperasi;
	}

	/**
	 * Menyetel referensi pembayaran anggota koperasi.
	 *
	 * @param pembayaranAnggotaKoperasi dokumen pembayaran anggota koperasi asal jurnal.
	 */
	public void setPembayaranAnggotaKoperasi(
			ais.database.model.koperasi.PembayaranAnggotaKoperasi pembayaranAnggotaKoperasi) {
		this.pembayaranAnggotaKoperasi = pembayaranAnggotaKoperasi;
	}

	/** Referensi dokumen sumber: pencairan diskon koperasi. <b>TIDAK ikut</b>
	 * {@link #ambilUnik()}. */
	private ais.database.model.koperasi.PencairanDiskon pencairanDiskon;

	/**
	 * Referensi dokumen sumber: pencairan diskon koperasi. <b>TIDAK ikut</b> menyusun
	 * {@link #ambilUnik()}.
	 *
	 * @return dokumen pencairan diskon asal jurnal, atau {@code null}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pencairan_diskon", nullable = true)
	public ais.database.model.koperasi.PencairanDiskon getPencairanDiskon() {
		return pencairanDiskon;
	}

	/**
	 * Menyetel referensi pencairan diskon koperasi.
	 *
	 * @param pencairanDiskon dokumen pencairan diskon asal jurnal.
	 */
	public void setPencairanDiskon(ais.database.model.koperasi.PencairanDiskon pencairanDiskon) {
		this.pencairanDiskon = pencairanDiskon;
	}


	/** Referensi dokumen sumber: penyesuaian saldo anggota koperasi. <b>TIDAK ikut</b>
	 * {@link #ambilUnik()}. */
	private ais.database.model.koperasi.PenyesuaianSaldoAnggota penyesuaianSaldoAnggota;

	/**
	 * Referensi dokumen sumber: penyesuaian saldo anggota koperasi. <b>TIDAK ikut</b>
	 * menyusun {@link #ambilUnik()}.
	 *
	 * @return dokumen penyesuaian saldo anggota asal jurnal, atau {@code null}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penyesuaian_saldo_anggota", nullable = true)
	public ais.database.model.koperasi.PenyesuaianSaldoAnggota getPenyesuaianSaldoAnggota() {
		return penyesuaianSaldoAnggota;
	}

	/**
	 * Menyetel referensi penyesuaian saldo anggota koperasi.
	 *
	 * @param penyesuaianSaldoAnggota dokumen penyesuaian saldo anggota asal jurnal.
	 */
	public void setPenyesuaianSaldoAnggota(
			ais.database.model.koperasi.PenyesuaianSaldoAnggota penyesuaianSaldoAnggota) {
		this.penyesuaianSaldoAnggota = penyesuaianSaldoAnggota;
	}

	/** Referensi dokumen sumber: modal penyertaan koperasi. <b>TIDAK ikut</b>
	 * {@link #ambilUnik()}. */
	private ais.database.model.koperasi.ModalPenyertaanKoperasi modalPenyertaanKoperasi;

	/**
	 * Referensi dokumen sumber: modal penyertaan koperasi. <b>TIDAK ikut</b> menyusun
	 * {@link #ambilUnik()}.
	 *
	 * @return dokumen modal penyertaan koperasi asal jurnal, atau {@code null}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "modal_penyertaan_koperasi", nullable = true)
	public ais.database.model.koperasi.ModalPenyertaanKoperasi getModalPenyertaanKoperasi() {
		return modalPenyertaanKoperasi;
	}

	/**
	 * Menyetel referensi modal penyertaan koperasi.
	 *
	 * @param modalPenyertaanKoperasi dokumen modal penyertaan koperasi asal jurnal.
	 */
	public void setModalPenyertaanKoperasi(
			ais.database.model.koperasi.ModalPenyertaanKoperasi modalPenyertaanKoperasi) {
		this.modalPenyertaanKoperasi = modalPenyertaanKoperasi;
	}


	/** Referensi dokumen sumber: pembagian SHU koperasi. <b>TIDAK ikut</b>
	 * {@link #ambilUnik()}. */
	private ais.database.model.koperasi.PembagianShu pembagianShu;

	/**
	 * Referensi dokumen sumber: pembagian SHU (sisa hasil usaha) koperasi. <b>TIDAK
	 * ikut</b> menyusun {@link #ambilUnik()}.
	 *
	 * @return dokumen pembagian SHU asal jurnal, atau {@code null}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pembagian_shu", nullable = true)
	public ais.database.model.koperasi.PembagianShu getPembagianShu() {
		return pembagianShu;
	}

	/**
	 * Menyetel referensi pembagian SHU koperasi.
	 *
	 * @param pembagianShu dokumen pembagian SHU asal jurnal.
	 */
	public void setPembagianShu(ais.database.model.koperasi.PembagianShu pembagianShu) {
		this.pembagianShu = pembagianShu;
	}


	/** Referensi dokumen sumber: nota biaya sales koperasi. <b>TIDAK ikut</b>
	 * {@link #ambilUnik()}. */
	private ais.database.model.koperasi.NotaSalesBiaya notaSalesBiaya;

	/**
	 * Referensi dokumen sumber: nota biaya sales koperasi. <b>TIDAK ikut</b> menyusun
	 * {@link #ambilUnik()}.
	 *
	 * @return dokumen nota biaya sales asal jurnal, atau {@code null}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "nota_sales_biaya", nullable = true)
	public ais.database.model.koperasi.NotaSalesBiaya getNotaSalesBiaya() {
		return notaSalesBiaya;
	}

	/**
	 * Menyetel referensi nota biaya sales koperasi.
	 *
	 * @param notaSalesBiaya dokumen nota biaya sales asal jurnal.
	 */
	public void setNotaSalesBiaya(ais.database.model.koperasi.NotaSalesBiaya notaSalesBiaya) {
		this.notaSalesBiaya = notaSalesBiaya;
	}

}
