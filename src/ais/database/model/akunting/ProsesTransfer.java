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

import ais.database.model.Tbmuser;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * Entity <b>Proses Transfer</b> pada tabel <code>akunting.proses_transfer</code> — satu
 * <b>batch pencairan dana lewat bank</b>.
 *
 * <h3>Peran dalam mekanisme pencairan dana</h3>
 * <p>Class ini adalah <b>langkah ketiga</b> (dan terakhir sebelum jurnal) dari rantai
 * pencairan dana lembaga. Rantai lengkapnya, seluruhnya terverifikasi dari kode:</p>
 * <ol>
 *   <li><b>Dokumen sumber disetujui</b> — uang muka pegawai, kas besar, kas kecil,
 *   penggantian kas kecil, pertanggungjawaban (pengembalian sisa panjar), dana talangan,
 *   reimbursement pegawai, pajak, pengadaan/termin/DP aset, transaksi koperasi, payroll,
 *   hutang supplier, diskon siswa.</li>
 *   <li><b>Satu baris {@link DaftarPengajuanTransfer} dibuat</b> (DPT, di layar disebut
 *   <b>DPC</b> — "Daftar Pengajuan Cair"). Tabel itu adalah <i>kolam antrean pembayaran</i>
 *   terpusat: 17 tipe dokumen sumber bermuara ke sana, satu baris per dokumen, lengkap
 *   dengan nominal, bank/rekening tujuan, dan akun debitnya.</li>
 *   <li><b>Petugas keuangan memilih beberapa baris DPT dan membungkusnya menjadi SATU
 *   {@code ProsesTransfer}</b> — inilah class ini. Batch itu punya satu
 *   {@link CaraPembayaranTransfer} (yang membawa <i>akun kredit</i>-nya, yaitu rekening
 *   bank sumber di bagan akun), satu nomor {@link #getKode() kode}, satu total
 *   {@link #getNilai() nilai}, dan satu alur persetujuan.</li>
 *   <li><b>Batch disetujui</b> → {@link #getDisetujuiOleh()} terisi.</li>
 *   <li><b>Batch direalisasikan</b> (uang benar-benar keluar dari rekening bank) →
 *   {@link #getRealisasikanOleh()} terisi. Inilah <b>penanda kanonik "dana sudah cair"</b>
 *   di seluruh aplikasi; lihat {@code DpcTransferStatusHelper} dan seluruh filter
 *   <code>prosesTransfer.realisasikanOleh IS NOT NULL</code> di modul akunting.</li>
 *   <li><b>Jurnal umum diposting</b> — {@code PostingProsesTransferAction} membentuk
 *   {@link PostingHistory} + sejumlah {@link GrupTransaksi}, dan
 *   {@code DaftarPengajuanTransfer.transfer} menjadi {@code true}.</li>
 * </ol>
 *
 * <h3>Relasi ke {@code DaftarPengajuanTransfer} — TERVERIFIKASI</h3>
 * <p>Relasinya <b>satu-arah dari sisi DPT</b>: tabel <code>daftar_pengajuan_transfer</code>
 * memiliki kolom FK <code>proses_transfer</code>
 * ({@code DaftarPengajuanTransfer.getProsesTransfer()}), sedangkan class ini
 * <b>tidak</b> memegang koleksi balik. Untuk mengambil isi sebuah batch, kode selalu
 * mengkueri sisi DPT (mis. {@code Restrictions.eq("prosesTransfer", pt)}). Konsekuensinya:
 * menghapus/mengubah batch <b>tidak</b> di-cascade ke baris-baris DPT-nya — pembebasan
 * baris harus dilakukan manual, dan memang itulah yang dilakukan jalur "batal setuju"
 * di {@code ProsesTransferApiHelper} maupun {@code NewUiTransferWorkflowService}.</p>
 * <p>Kardinalitas: <b>1 ProsesTransfer : N DaftarPengajuanTransfer</b>. Penjaga
 * idempotensinya ada di sisi DPT ({@code d.getProsesTransfer() != null} → tolak, "sudah
 * berada dalam proses transfer"), bukan di sini.</p>
 *
 * <h3>Apa yang TIDAK ada di entity ini (koreksi asumsi yang mudah keliru)</h3>
 * <ul>
 *   <li><b>Tidak ada bank/rekening tujuan.</b> Field {@code bankSumber} dan
 *   {@code noRekSumber} ada di file ini tetapi <b>dikomentari mati</b> sejak lama; yang
 *   hidup adalah {@code DaftarPengajuanTransfer.getBankSumber()}/{@code getNoRekSumber()}
 *   per baris. Satu batch bisa memuat baris-baris dengan rekening tujuan berbeda-beda.</li>
 *   <li><b>Tidak ada akun jurnal.</b> Field {@code akun} juga dikomentari mati; akun
 *   kreditnya diambil dari {@link CaraPembayaranTransfer#getAkun()} (atau
 *   {@code getAkunTransitori()} untuk baris bertanda transitori).</li>
 *   <li><b>Tidak ada kolom status transfer.</b> Status bukan enum/string melainkan
 *   <i>diturunkan</i> dari kombinasi tiga field: {@code disetujuiOleh == null} →
 *   WAITING_APPROVAL, {@code != null} tapi {@code realisasikanOleh == null} → APPROVED,
 *   {@code realisasikanOleh != null} → REALIZED (lihat
 *   {@code NewUiTransferWorkflowService.processRow}).</li>
 *   <li><b>Tidak ada kolom tenant.</b> Tidak ada {@code sekolah}, {@code yayasan}, maupun
 *   {@code satuanKerja} — lihat bagian "Cakupan tenant" di bawah.</li>
 *   <li><b>Tidak ada integrasi API bank / host-to-host.</b> Realisasi di sini adalah
 *   <i>pencatatan manual</i> oleh petugas ("saya sudah transfer lewat internet banking"),
 *   bukan pemicu transfer otomatis. Nominal riil berpindah di luar sistem.</li>
 * </ul>
 *
 * <h3>HAL PALING NON-OBVIOUS: enam getter menulis balik ke database</h3>
 * <p>Anotasi Hibernate menempel di <b>getter</b> ({@code @Id} ada di {@link #getId()}),
 * jadi class ini memakai <b>property access</b>. Digabung dengan
 * {@code dynamicUpdate = true}, setiap penugasan field yang terjadi <i>di dalam getter</i>
 * ikut terbaca oleh <i>dirty checking</i> dan <b>tersimpan permanen</b> pada flush
 * berikutnya. Getter yang berperilaku begitu — sekadar <b>membaca</b> entity yang masih
 * terpasang di session sudah cukup untuk mengubah isi database:</p>
 * <ul>
 *   <li>{@link #getAktif()} — hanya bisa menulis {@code false}, tidak pernah memulihkan
 *   {@code true} (pola "flag satu-arah", sama seperti di {@code DaftarPengajuanTransfer}).</li>
 *   <li>{@link #getKodeUnik()} — menghitung ulang kolom ber-{@code unique} constraint
 *   setiap kali dibaca.</li>
 *   <li>{@link #getDisetujuiOleh()} dan {@link #getTanggalPersetujuan()} — <b>menimpa
 *   penyetuju dan tanggal persetujuan dari alur SOP</b>, termasuk memaksanya menjadi
 *   {@code null}. Ini yang membuat {@code setDisetujuiOleh(null)} bisa tidak berefek;
 *   lihat "Pembatalan persetujuan bisa tidak menempel" di bawah.</li>
 *   <li>{@link #getTanggalPembuatan()} — menimpa dari waktu pengajuan disposisi SOP.</li>
 *   <li>{@link #getTanggalRealisasikan()} — <b>menstempel waktu SEKARANG</b> bila pelaksana
 *   realisasi sudah ada tetapi tanggalnya masih kosong, dan <b>memaksa {@code null}</b>
 *   bila pelaksananya kosong.</li>
 *   <li>{@link #getNomorSuratAlurKeuangan()} — mengisi default {@code DPC} bila kosong.</li>
 *   <li>{@link #getTahun()} dan {@link #getBulan()} — mengisi tahun/bulan <b>saat dibaca</b>,
 *   bukan saat batch dibuat.</li>
 * </ul>
 * <p><b>Verifikasi negatif yang menenangkan:</b> {@link #getNilai()} — satu-satunya getter
 * yang menyentuh nominal uang — <b>TIDAK</b> menulis balik. Ia mengembalikan {@code 0.0}
 * untuk {@code null} tanpa menugaskannya ke field, jadi membaca nominal transfer tidak
 * pernah mengubahnya. Demikian pula {@link #getNama()} (mem-{@code trim} salinan yang
 * dikembalikan saja) dan {@link #getCaraPembayaranTransfer()}/{@link #getRealisasikanOleh()}
 * (hanya me-resolve proxy lewat {@code check(...)}, tanpa mengubah nilai).</p>
 *
 * <h3>Kuirk: pembatalan persetujuan bisa tidak menempel</h3>
 * <p>Dua jalur pembatalan persetujuan — {@code ProsesTransferApiHelper.batalSetuju} (REST)
 * dan {@code NewUiTransferWorkflowService.reject} — memanggil
 * {@code setDisetujuiOleh(null)} lalu membebaskan seluruh baris DPT dan menolkan
 * {@link #getNilai() nilai}. Untuk batch yang <b>tidak</b> memakai SOP
 * ({@code disposisiSop == null}) itu bekerja. Namun untuk batch yang <b>memakai SOP</b>,
 * pembacaan berikutnya atas {@link #getDisetujuiOleh()} akan <b>memulihkan kembali</b>
 * penyetujunya dari {@code disposisiSop.getDisposisiSetuju().getDiajukanOleh()} — dan
 * karena itu getter properti, pemulihan tersebut ikut tersimpan. Hasilnya batch kembali
 * berstatus "APPROVED" padahal isinya sudah kosong dan nilainya nol, sementara tombol
 * "Realisasikan" (yang hanya mensyaratkan {@code disetujuiOleh != null &&
 * realisasikanOleh == null}) kembali muncul. Ditambah {@link #setDisposisiSop(DisposisiSop)}
 * yang <b>menolak</b> argumen {@code null}, tautan ke alur SOP praktis tidak bisa dilepas
 * dari kode Java. Dicatat sebagai kuirk; tidak diubah di sini.</p>
 *
 * <h3>Cakupan tenant: tidak ada sama sekali di level batch</h3>
 * <p>Entity ini <b>tidak punya kolom pembatas tenant</b> (tidak ada {@code sekolah},
 * {@code yayasan}, maupun {@code satuanKerja}), dan tiga konsumen utamanya —
 * {@code ProsesTransferAction}, {@code ProsesTransferApiHelper}, dan
 * {@code PostingProsesTransferAction} — <b>tidak menyebut kata sekolah/yayasan sama
 * sekali</b>. Jadi daftar batch transfer bersifat <b>global lintas tenant</b>: setiap
 * pengguna yang bisa membuka layar Proses Transfer melihat (dan dapat menyetujui,
 * merealisasikan, membatalkan, atau menghapus) batch milik tenant mana pun. Satu-satunya
 * sumbu pembatas ada di lapisan bawahnya, {@code DaftarPengajuanTransferAction}, dan itu
 * pun <b>fail-open</b> dalam dua arah sekaligus: baris ber-{@code satuanKerja} {@code NULL}
 * lolos untuk semua orang, dan bila {@code SekolahUtil.ambilSatuanKerjas()} mengembalikan
 * himpunan kosong penyaringnya berubah menjadi {@code sqlRestriction("1=1")} — yakni
 * seluruh baris seluruh tenant.</p>
 * <p>Di jalur REST tidak ada pembatas sama sekali: seluruh method yang mengambil batch
 * bekerja langsung dari id yang dikirim klien
 * ({@code session.get(ProsesTransfer.class, id)}) tanpa memverifikasi bahwa batch itu
 * milik tenant/satuan kerja pemanggil, dan kueri {@code daftar}/{@code kandidat} tidak
 * punya predikat tenant apa pun.</p>
 *
 * <h3>Keterjangkauan lewat endpoint — HASIL VERIFIKASI</h3>
 * <p>Selain layar ZK, batch transfer dapat dikendalikan penuh lewat servlet REST
 * {@code /PosApi} dan {@code /Api_eBisnis}, yang mendispatch seluruh aksi berawalan
 * {@code proses_transfer_} ke {@code ProsesTransferApiHelper} — termasuk
 * {@code _simpan}, {@code _hapus}, {@code _setujui}, {@code _batal_setuju},
 * {@code _realisasikan}, dan {@code _batal_realisasi}. Aksi {@code _realisasikan} adalah
 * yang paling berdampak: ia mengisi {@link #getRealisasikanOleh()} lalu langsung memicu
 * {@code PostingProsesTransferAction.postingSatu(...)} sehingga jurnal umum terbentuk
 * dalam satu panggilan.</p>
 * <p><b>Verifikasi negatif:</b> jalur ini <b>bukan</b> anonim — token {@code Bearer} wajib
 * ada. Jadi entity ini tidak terjangkau tanpa kredensial, berbeda dari servlet H2H bank.
 * Diverifikasi pula bahwa <b>tidak ada satu pun servlet H2H/payment gateway</b> (BNI, BSI,
 * BRI, Flip, Finpay, E-Smartlink, Online BMT, Otto, dsb.) yang menyentuh
 * {@code ProsesTransfer} maupun {@link CaraPembayaranTransfer}; persinggungan mereka dengan
 * modul ini hanya lewat {@code DaftarPengajuanTransfer.simpanDiskonPembayaran(...)} untuk
 * diskon tagihan siswa, bukan lewat batch pencairan.</p>
 * <p><b>Namun otorisasinya lemah berlapis</b>, dan ini yang perlu diketahui siapa pun yang
 * menyentuh entity ini:</p>
 * <ol>
 *   <li>Kunci menu {@code "proses_transfer"} terdaftar di {@code KUNCI_DEFAULT_NONAKTIF}
 *   milik {@code EbisnisMenuKatalog} — niat penulisnya jelas <i>fail-closed</i> — tetapi
 *   gerbang {@code PosApi.bolehAksesActionKantin} <b>tidak pernah menulis cabang untuk
 *   awalan {@code proses_transfer_}</b>, sehingga eksekusi jatuh ke {@code return true}
 *   di ujung method. Bandingkan awalan {@code si_} yang justru fail-closed.</li>
 *   <li>{@code ProsesTransferApiHelper.bolehAksi(...)} mengembalikan {@code true} bila
 *   {@code tbmuser.hakAkses()} bernilai {@code null} — peran yang tidak terbaca diberi izin
 *   penuh, bukan ditolak. Ini pola fail-open yang sama yang sudah tercatat di beberapa
 *   helper API modul keuangan lain.</li>
 *   <li>{@code EbisnisMenuKatalog.bolehAksi(...)} mengembalikan default
 *   {@code aksiLegacy = true} untuk peran yang grid CRUD-nya belum pernah disimpan admin.
 *   Helper akuntansi lain ({@code DraftJurnalApiHelper}) sengaja memakai varian
 *   {@code bolehAksiAkuntansi(...)} yang membedakan "belum diatur" dari "sengaja
 *   dimatikan"; helper ini tidak.</li>
 * </ol>
 * <p>Efek gabungannya: satu kredensial AIS yang sah — peran apa pun, tenant mana pun —
 * cukup untuk menyetujui dan merealisasikan batch pencairan milik tenant lain. Aksi baca
 * ({@code _daftar}, {@code _detail}, {@code _kandidat}, {@code _dasbor}) bahkan tidak
 * memanggil gerbang sama sekali.</p>
 *
 * <h3>Gerbang tombol "Realisasikan" di layar ZK</h3>
 * <p>Di {@code ProsesTransferAction.init(...)}, tombol "Realisasikan" hanya disyaratkan oleh
 * keadaan <i>data</i> — {@code getDisetujuiOleh() != null &amp;&amp; getRealisasikanOleh() == null}
 * — <b>tanpa memeriksa hak {@code APPROVE}/{@code UPDATE}</b>, dan tanpa menghormati flag
 * {@code edit}/{@code viewOnly} milik controller. Padahal
 * {@code ProsesTransferAction.onAddExternal(...)} adalah entry point <b>publik statis</b>
 * yang dipanggil dari <b>25+ layar berbeda</b> di modul akunting, aset, koperasi, dan
 * payroll (mis. {@code PostingKasKecilAction}, {@code PostingUangMukaAction},
 * {@code TransaksiKoperasiAction}, {@code PostingPembayaranAction}), bahkan dari entity
 * {@link DaftarPengajuanTransfer#tampilStatus} dan {@link GrupTransaksi} sendiri — semuanya
 * lewat overload dua-argumen yang menyetel {@code edit = false}. Karena flag itu tidak
 * membungkus blok tombol realisasi, membuka tautan status transfer dari layar mana pun
 * menampilkan tombol pencairan yang berfungsi. Ini instans pola "pewarisan hak lewat menu
 * induk" yang sudah berulang di repo ini, tetapi yang diwariskan di sini adalah
 * <b>pencairan dana</b>, bukan CRUD master.</p>
 *
 * <h3>Integritas: bisa satu batch terjurnal dua kali?</h3>
 * <p>Realisasi sendiri <b>idempoten</b>: {@code realisasikan(...)} di jalur REST memeriksa
 * {@code getRealisasikanOleh() != null} dan memperlakukan pemanggilan ulang sebagai
 * pemulihan (tidak menstempel ulang), dan mesin posting hanya mengambil baris DPT dengan
 * {@code postingHistory IS NULL}. Namun <b>jalur batal-posting membuka celah</b>: baik
 * {@code onBatalkanPostingSemua} maupun tombol "Batalkan Posting Data" per baris
 * mengosongkan {@code postingHistory} lalu menjalankan
 * <code>delete from akunting.grup_transaksi where daftar_pengajuan_transfer=? and closing is null</code>.
 * Dua akibatnya:</p>
 * <ol>
 *   <li>Hanya <i>header</i> jurnal ({@code grup_transaksi}) yang dihapus — baris
 *   debit/kredit anaknya di tabel {@code transaksi} tidak ikut dihapus, sehingga posting
 *   ulang menambah baris buku besar di atas baris lama yang menggantung. Ini pola yang
 *   sama dengan yang sudah dicatat untuk siklus uang muka.</li>
 *   <li>Klausa <code>and closing is null</code> membuat jurnal yang sudah masuk periode
 *   tutup buku <b>tidak</b> dihapus, padahal {@code postingHistory}-nya tetap dikosongkan.
 *   Baris DPT itu lalu tampak "belum diposting" dan bisa diposting lagi — menghasilkan
 *   <b>jurnal ganda yang benar-benar utuh</b> untuk pencairan dana yang sama.</li>
 * </ol>
 *
 * <h3>Pengelompokan anggota class</h3>
 * <ol>
 *   <li><b>Jejak audit</b> — {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()} beserta {@code onUpdate()}.</li>
 *   <li><b>Identitas &amp; deskripsi</b> — {@link #getId()}, {@link #getKode()},
 *   {@link #getKodeUnik()}, {@link #getNama()}, {@link #getKeterangan()},
 *   {@link #toString()}.</li>
 *   <li><b>Isi batch</b> — {@link #getNilai()} (total) dan
 *   {@link #getCaraPembayaranTransfer()} (pembawa akun kredit).</li>
 *   <li><b>Periode</b> — {@link #getTahun()}, {@link #getBulan()},
 *   {@link #getTanggalPembuatan()}.</li>
 *   <li><b>Alur SOP &amp; penomoran surat</b> — {@link #getDisposisiSop()},
 *   {@link #getNomorSuratAlurKeuangan()}, {@link #getAktif()}.</li>
 *   <li><b>Tahap persetujuan</b> — {@link #getDisetujuiOleh()},
 *   {@link #getTanggalPersetujuan()}, {@link #getCatatanPersetujuan()}.</li>
 *   <li><b>Tahap realisasi (dana cair)</b> — {@link #getRealisasikanOleh()},
 *   {@link #getTanggalRealisasikan()}, {@link #getCatatanRealisasi()}.</li>
 * </ol>
 *
 * <h3>Catatan teknis pewarisan</h3>
 * <p>Class ini {@code extends} {@link DataSop} (abstrak, mewajibkan pasangan
 * {@code getDisposisiSop}/{@code setDisposisiSop}), yang pada gilirannya
 * {@code extends} {@link ais.database.model.GeneralValueObject}.
 * <b>{@code GeneralValueObject} bukan {@code @Entity} maupun
 * {@code @MappedSuperclass}</b> — ia POJO abstrak biasa, sehingga Hibernate <b>tidak</b>
 * memetakan properti induknya. Karena itu field {@code id}, {@code oleh}, {@code olehId},
 * dan {@code tanggal_dirubah} <b>sengaja dideklarasikan ulang</b> di sini; itu keharusan
 * teknis, bukan duplikasi yang keliru. Method utilitas warisan yang dipakai adalah
 * {@code check(...)} (resolusi proxy lazy) — lihat penjelasan lengkapnya di
 * {@link ais.database.model.GeneralValueObject#check(Object)}.</p>
 * <p>Catatan kembar salin-tempel: {@code serialVersionUID} di sini identik dengan milik
 * {@link CaraPembayaranTransfer} ({@code 2463821577548439808L}) — penanda khas bahwa
 * keduanya lahir dari satu template generator yang sama. Sepupu terdekat class ini adalah
 * {@link ProsesTransferStandingInstruction} (batch transfer berulang/standing instruction),
 * yang dilayani {@code ProsesTransferStandingInstructionAction}.</p>
 *
 * @see DaftarPengajuanTransfer
 * @see CaraPembayaranTransfer
 * @see NomorSuratAlurKeuangan
 * @see DisposisiSop
 * @see DataSop
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "akunting", name = "proses_transfer")
public class ProsesTransfer extends DataSop {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** PK batch transfer; di-generate {@code IDENTITY} oleh basis data saat INSERT. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (jejak audit, diisi interceptor). */
	private String oleh;
	/** User id pengguna terakhir yang mengubah baris ini (jejak audit, diisi interceptor). */
	private String olehId;

	/**
	 * Mengembalikan user id pencatat perubahan terakhir.
	 *
	 * @return user id pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan user id pencatat perubahan terakhir, <b>dengan penolakan nilai kosong</b>.
	 *
	 * <p><b>Efek samping / kasus tepi:</b> argumen {@code null}, string kosong, atau
	 * hanya spasi <b>diabaikan diam-diam</b> (method langsung {@code return}) sehingga nilai
	 * lama dipertahankan. Ini disengaja: jejak audit tidak boleh terhapus oleh proses yang
	 * kebetulan tidak punya konteks pengguna (mis. job latar atau pemanggilan REST tanpa
	 * sesi). Konsekuensinya {@code oleh_id} tidak pernah bisa dikosongkan lewat setter.</p>
	 *
	 * @param olehId user id pengubah; nilai {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pencatat perubahan terakhir, <b>dengan penolakan nilai kosong</b>.
	 *
	 * <p><b>Efek samping / kasus tepi:</b> sama persis dengan
	 * {@link #setOlehId(String)} — {@code null}/kosong/spasi diabaikan diam-diam agar
	 * jejak audit yang sudah ada tidak tertimpa nilai hampa.</p>
	 *
	 * @param oleh nama pengubah; nilai {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pencatat perubahan terakhir.
	 *
	 * @return nama pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA <b>dan</b> deklarasi field stempel waktu, keduanya pada satu baris
	 * fisik (bentuk warisan dari penyisipan otomatis; jangan dipisah tanpa alasan).
	 *
	 * <p><b>{@code onUpdate()}:</b> dipanggil Hibernate <i>sebelum</i> setiap {@code UPDATE}
	 * atas baris ini dan mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}, yang mengisi
	 * {@link #setOleh(String) oleh}/{@link #setOlehId(String) olehId} dari pengguna aktif
	 * serta memperbarui {@link #getTanggal_dirubah() tanggal_dirubah}. Tidak dipanggil pada
	 * {@code INSERT} — karena itu field {@code tanggal_dirubah} sudah diinisialisasi ke
	 * waktu sekarang di deklarasinya.</p>
	 *
	 * <p><b>Perhatian:</b> karena getter-getter di class ini menulis balik ke field
	 * (lihat Javadoc class), sebuah baris bisa ter-{@code UPDATE} — dan karenanya
	 * stempel auditnya ikut berubah — <i>hanya karena dibaca</i>, tanpa ada perubahan
	 * yang disengaja pengguna.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk baris baru karena
	 *         field-nya diinisialisasi {@code WaktuUtil.getDate()} saat objek dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat batch, dipakai combobox/label ZK dan pesan log.
	 *
	 * <p><b>Kasus tepi:</b> membaca <b>field</b> {@code nama} secara langsung, bukan
	 * {@link #getNama()} — jadi hasilnya <b>tidak</b> di-{@code trim}. Untuk baris yang
	 * belum tersimpan {@code id} masih {@code null}, sehingga keluarannya berbentuk
	 * {@code "null-<nama>"}.</p>
	 *
	 * @return gabungan {@code id + "-" + nama}
	 */
	public String toString() {
		return id + "-" + nama;
	}

	// Rekening bank sumber pernah dirancang melekat di batch, tetapi ditinggalkan:
	// bank/rekening yang dipakai ditentukan per baris di DaftarPengajuanTransfer, dan akun
	// kreditnya diambil dari CaraPembayaranTransfer. Dibiarkan sebagai jejak rancangan.
//	private Bank bankSumber;
//	private String noRekSumber;

	/** Cara/metode transfer yang dipakai batch ini; pembawa akun kredit jurnalnya. */
	private CaraPembayaranTransfer caraPembayaranTransfer;
	/** Nomor batch yang tampil di layar dan dipakai pada pesan notifikasi. */
	private String kode;
	/** Tahun periode batch; diisi otomatis oleh getter bila kosong. */
	private Integer tahun;
	/** Bulan periode batch (1-12); diisi otomatis oleh getter bila kosong. */
	private Integer bulan;
	/** Judul/nama batch yang diketik petugas keuangan. */
	private String nama;
	/** Keterangan bebas untuk batch. */
	private String keterangan;
	/** Total nominal batch = penjumlahan nominal seluruh baris DPT yang menempel. */
	private Double nilai;
	/** Bendera aktif; hanya bisa dipadamkan oleh getter, tidak pernah dinyalakan ulang. */
	private Boolean aktif;

	/** Tautan ke alur disposisi SOP yang mengawal persetujuan batch ini. */
	private DisposisiSop disposisiSop;
	/** Tanggal batch dibuat; ditimpa dari waktu pengajuan disposisi bila ada. */
	private Date tanggalPembuatan;
	/** Pengguna yang menyetujui batch; diturunkan dari disposisi SOP bila ada. */
	private Tbmuser disetujuiOleh;
	/** Tanggal persetujuan; diturunkan dari disposisi SOP bila ada. */
	private Date tanggalPersetujuan;
	/** Catatan bebas penyetuju (maks. 2000 karakter). */
	private String catatanPersetujuan;
	/** Kategori penomoran surat alur keuangan; default {@code DPC} bila kosong. */
	private NomorSuratAlurKeuangan nomorSuratAlurKeuangan;
	/** Pengguna yang mencatat realisasi — <b>penanda kanonik "dana sudah cair"</b>. */
	private Tbmuser realisasikanOleh;
	/** Tanggal dana benar-benar keluar dari rekening bank. */
	private Date tanggalRealisasikan;
	/** Catatan bebas pelaksana realisasi (maks. 2000 karakter). */
	private String catatanRealisasi;

	// Akun jurnal pernah dirancang melekat di batch, tetapi ditinggalkan: akun kreditnya
	// diambil dari CaraPembayaranTransfer.getAkun()/getAkunTransitori().
//	private Akun akun;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/ZK.
	 *
	 * <p>Tidak melakukan inisialisasi apa pun selain default field. Perlu diketahui bahwa
	 * instance kosong hasil konstruktor ini <b>tidak</b> netral saat dibaca: memanggil
	 * {@link #getTahun()}/{@link #getBulan()}/{@link #getNomorSuratAlurKeuangan()}
	 * langsung mengisi field dengan nilai default.</p>
	 */
	public ProsesTransfer() {
	}

	/**
	 * Mengembalikan PK batch.
	 *
	 * @return id batch, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel PK batch. Hanya dipakai Hibernate; jangan dipanggil dari kode aplikasi.
	 *
	 * @param id PK batch
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nomor batch transfer.
	 *
	 * <p>Nilainya dibuat sekali saat batch dibentuk (lihat
	 * {@code NewUiTransferWorkflowService.uniqueProcessCode} dan jalur ZK padanannya) dan
	 * dipakai di layar, pesan notifikasi realisasi, serta pada
	 * {@link #getKodeUnik()}.</p>
	 *
	 * @return nomor batch, atau {@code null} bila belum dibentuk
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Menyetel nomor batch transfer.
	 *
	 * @param kode nomor batch
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan judul batch, sudah di-{@code trim}.
	 *
	 * <p><b>Kasus tepi:</b> {@code trim()} hanya diterapkan pada nilai yang
	 * <i>dikembalikan</i>; field-nya tidak diubah, jadi getter ini <b>tidak</b> destruktif
	 * (beda dari getter-getter lain di class ini). Karena itu {@link #toString()}, yang
	 * membaca field mentah, bisa menampilkan spasi tepi yang tidak terlihat di layar.</p>
	 *
	 * @return judul batch tanpa spasi tepi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel judul batch.
	 *
	 * <p>Kolomnya {@code nullable = false} di basis data, jadi menyimpan batch dengan
	 * judul {@code null} akan gagal pada level SQL, bukan pada validasi Java.</p>
	 *
	 * @param nama judul batch
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas batch.
	 *
	 * @return keterangan, atau {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas batch.
	 *
	 * @param keterangan keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan bendera aktif batch, sekaligus <b>memadamkannya</b> bila alur SOP-nya
	 * sudah mati atau berakhir di titik penolakan.
	 *
	 * <p><b>Tujuan:</b> menyembunyikan batch yang alur persetujuannya ditolak/dibatalkan
	 * dari daftar kerja, tanpa perlu ada proses batch tersendiri yang menyapu tabel.</p>
	 *
	 * <p><b>Cara kerja:</b> me-refresh field {@code disposisiSop} dari
	 * {@link #getDisposisiSop()}, lalu menugaskan {@code aktif = false} pada dua kondisi:
	 * (a) disposisinya sendiri tidak aktif; atau (b) alur SOP-nya berhenti di simpul yang
	 * ditandai {@code getPenolakanAdaDiSini()}. Nilai {@code null} diperlakukan sebagai
	 * <b>aktif</b> ({@code return aktif == null ? true : aktif}).</p>
	 *
	 * <p><b>EFEK SAMPING PENTING:</b> ini getter properti Hibernate, sehingga penugasan
	 * {@code false} di atas <b>tersimpan permanen ke basis data</b> pada flush berikutnya —
	 * membaca batch saja sudah cukup untuk menonaktifkannya. Sifatnya <b>satu arah</b>:
	 * tidak ada satu pun cabang yang mengembalikan nilai menjadi {@code true}. Bila sebuah
	 * batch ditolak lalu alurnya diajukan ulang dengan disposisi baru yang sehat, getter ini
	 * memang berhenti menulis {@code false}, tetapi nilai {@code false} lama tetap
	 * dikembalikan apa adanya — batch itu tidak pulih dengan sendirinya. Pemulihan hanya
	 * bisa lewat {@link #setAktif(Boolean)} eksplisit (mis. dari layar) atau lewat SQL.</p>
	 *
	 * <p><b>Kasus tepi:</b> {@code getDisposisiSop()} dapat melempar
	 * {@code LazyInitializationException} bila proxy disposisinya terikat ke session yang
	 * sudah tertutup. Berbeda dari {@link #getTanggalPersetujuan()}, method ini
	 * <b>tidak</b> memasang penangkap exception, jadi kegagalan lazy di sini akan
	 * merambat naik ke pemanggil.</p>
	 *
	 * @return {@code true} bila batch masih aktif (termasuk saat kolomnya {@code NULL});
	 *         {@code false} bila alur SOP-nya mati atau ditolak
	 */
	public Boolean getAktif() {
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
	 * Menyetel bendera aktif batch secara eksplisit.
	 *
	 * <p>Ini satu-satunya jalan menghidupkan kembali batch yang sudah dipadamkan
	 * {@link #getAktif()}; pemanggilnya antara lain jalur pembuatan batch di
	 * {@code NewUiTransferWorkflowService.createProcess} ({@code setAktif(Boolean.TRUE)}).
	 * Perlu diingat penugasan {@code true} di sini bisa <b>ditimpa lagi</b> oleh
	 * {@link #getAktif()} pada pembacaan berikutnya bila kondisi SOP-nya masih memenuhi
	 * salah satu cabang pemadam.</p>
	 *
	 * @param aktif bendera aktif; {@code null} berarti "aktif" menurut konvensi getter-nya
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	// Bank sumber & nomor rekening sumber: lihat catatan pada deklarasi field yang
	// dikomentari di atas. Fungsinya kini dipenuhi DaftarPengajuanTransfer per baris.
//	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
//	@JoinColumn(name = "bank_sumber_id", nullable = false)
//	public Bank getBankSumber() {
//		bankSumber = check(bankSumber);
//		return bankSumber;
//	}
//
//	public void setBankSumber(Bank bankSumber) {
//		this.bankSumber = bankSumber;
//	}
//
//	public String getNoRekSumber() {
//		return noRekSumber;
//	}
//
//	public void setNoRekSumber(String noRekSumber) {
//		this.noRekSumber = noRekSumber;
//	}

	/**
	 * Mengembalikan total nominal batch (jumlah nominal seluruh baris DPT yang menempel).
	 *
	 * <p><b>VERIFIKASI PENTING:</b> berbeda dari mayoritas getter di class ini, method ini
	 * <b>tidak menulis balik</b>. Substitusi {@code 0.0} untuk {@code null} hanya berlaku
	 * pada nilai yang dikembalikan; field {@code nilai} tidak disentuh, sehingga
	 * <b>membaca nominal transfer tidak pernah mengubah nominal yang tersimpan</b>. Kolom
	 * {@code NULL} tetap {@code NULL} di basis data.</p>
	 *
	 * <p><b>Siapa yang mengisinya:</b> nilainya <i>tidak</i> dihitung ulang oleh entity,
	 * melainkan di-maintain oleh lapisan layanan setiap kali komposisi batch berubah —
	 * {@code createProcess} (jumlah awal), {@code addItems} (tambah), {@code removeItem}
	 * (kurang, dengan lantai {@code Math.max(0, ...)}), serta {@code reject}/{@code batalSetuju}
	 * yang menolkannya. Karena itu nilai di kolom ini bisa melenceng dari penjumlahan riil
	 * baris DPT bila ada baris yang dilepas lewat jalur lain.</p>
	 *
	 * @return total nominal batch; {@code 0.0} bila kolomnya masih kosong
	 */
	public Double getNilai() {
		return nilai == null ? 0.0 : nilai;
	}

	/**
	 * Menyetel total nominal batch.
	 *
	 * @param nilai total nominal; boleh {@code null} (dibaca sebagai {@code 0.0})
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Mengembalikan cara/metode transfer yang dipakai batch ini.
	 *
	 * <p><b>Mengapa penting:</b> {@link CaraPembayaranTransfer} adalah pembawa
	 * <b>akun kredit</b> jurnal pencairan — {@code getAkun()} untuk baris bertanda
	 * "Transfer" dan {@code getAkunTransitori()} untuk baris bertanda "Transitori". Jadi
	 * relasi inilah yang menentukan rekening/akun mana yang berkurang di buku besar saat
	 * batch diposting.</p>
	 *
	 * <p><b>Cara kerja:</b> memanggil {@code check(...)} warisan untuk meng-<i>unwrap</i>
	 * proxy lazy Hibernate menjadi instance nyata. Penugasan ulang di sini menulis nilai
	 * yang setara (proxy → objek yang sama), jadi tidak mengubah data.</p>
	 *
	 * @return cara pembayaran transfer, atau {@code null} bila belum dipilih (kolomnya
	 *         {@code nullable}, sehingga batch bisa tersimpan tanpa metode dan akibatnya
	 *         tidak punya akun kredit saat posting)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "cara_pembayaran_transfer", nullable = true)
	public CaraPembayaranTransfer getCaraPembayaranTransfer() {
		caraPembayaranTransfer = check(caraPembayaranTransfer);
		return caraPembayaranTransfer;
	}

	/**
	 * Menyetel cara/metode transfer batch.
	 *
	 * <p>Jalur pembuatan batch memvalidasi lebih dulu bahwa metodenya ada dan
	 * {@code aktif} bernilai {@code true}; setter ini sendiri tidak memvalidasi apa pun.</p>
	 *
	 * @param caraPembayaranTransfer metode transfer; boleh {@code null}
	 */
	public void setCaraPembayaranTransfer(CaraPembayaranTransfer caraPembayaranTransfer) {
		this.caraPembayaranTransfer = caraPembayaranTransfer;
	}

	/** Kunci unik turunan; selalu dihitung ulang oleh getter-nya, tidak pernah dipercaya. */
	private String kodeUnik;

	/**
	 * Mengembalikan kunci unik batch, <b>dihitung ulang setiap kali dibaca</b>.
	 *
	 * <p><b>Tujuan:</b> memberi basis data satu kolom ber-{@code unique} constraint yang
	 * mencegah dua batch berbagi identitas yang sama — pola yang sama dipakai
	 * {@code GrupTransaksi.kodeUnik} dan {@code DaftarPengajuanTransfer.getKodeUnik()}
	 * sebagai penjaga duplikasi.</p>
	 *
	 * <p><b>Cara kerja:</b> merangkai {@code getKode()} + {@code "_"} + id disposisi SOP,
	 * atau + {@code "_"} + {@link #getId()} bila batch belum terikat SOP.</p>
	 *
	 * <p><b>EFEK SAMPING:</b> hasilnya ditugaskan ke field {@code kodeUnik} — dan karena
	 * ini getter properti Hibernate, nilainya <b>tersimpan permanen</b> pada flush
	 * berikutnya, menimpa apa pun yang ada di kolom itu sebelumnya.</p>
	 *
	 * <p><b>Kasus tepi yang perlu diketahui:</b></p>
	 * <ul>
	 *   <li>Pada saat {@code INSERT} batch baru non-SOP, {@link #getId()} masih
	 *   {@code null} (id {@code IDENTITY} baru terbit setelah INSERT), sehingga nilai yang
	 *   pertama kali tersimpan berbentuk <code>"&lt;kode&gt;_null"</code>. Nilai itu baru
	 *   terkoreksi saat baris dibaca ulang dalam session hidup.</li>
	 *   <li>Bila {@link #getKode()} masih {@code null}, hasilnya diawali literal
	 *   {@code "null"} — bukan {@code NullPointerException}, karena perangkaian string.</li>
	 *   <li>Batch yang berpindah alur SOP akan berganti kunci unik, sehingga kunci lama
	 *   tidak lagi menghalangi pembuatan batch lain dengan kode yang sama.</li>
	 * </ul>
	 *
	 * @return kunci unik batch, tidak pernah {@code null}
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		kodeUnik = getKode() + "" + (getDisposisiSop() == null ? "_" + getId() : "_" + getDisposisiSop().getId());
		return kodeUnik;
	}

	/**
	 * Menyetel kunci unik batch.
	 *
	 * <p><b>Praktis tidak berguna:</b> nilai apa pun yang disetel di sini akan ditimpa
	 * pada pembacaan {@link #getKodeUnik()} berikutnya. Setter ini ada semata agar
	 * Hibernate bisa memuat kolomnya saat hidrasi objek.</p>
	 *
	 * @param kodeUnik kunci unik; akan ditimpa oleh getter
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * Mengembalikan disposisi alur SOP yang mengawal persetujuan batch ini.
	 *
	 * <p>Implementasi dari kontrak abstrak {@link DataSop#getDisposisiSop()}. Berbeda dari
	 * getter relasi lain di class ini, method ini <b>tidak</b> memanggil {@code check(...)},
	 * sehingga yang dikembalikan bisa berupa proxy Hibernate yang belum terinisialisasi.
	 * Itulah sumber {@code LazyInitializationException} yang ditangkap secara khusus di
	 * {@link #getTanggalPersetujuan()}.</p>
	 *
	 * <p>Nilai {@code null} berarti batch dibuat tanpa alur SOP — jalur yang sah, dan
	 * satu-satunya jalur di mana pembatalan persetujuan benar-benar menempel (lihat
	 * Javadoc class).</p>
	 *
	 * @return disposisi SOP, atau {@code null} bila batch tidak memakai alur SOP
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		return disposisiSop;
	}

	/**
	 * Menautkan batch ke sebuah disposisi alur SOP, <b>dengan penolakan nilai kosong</b>.
	 *
	 * <p><b>Cara kerja &amp; kasus tepi (penting):</b></p>
	 * <ul>
	 *   <li>Penjaga di awal membuat argumen {@code null} <i>atau</i> disposisi yang belum
	 *   tersimpan ({@code getId() == null}) <b>diabaikan diam-diam</b>. Akibatnya
	 *   <b>tautan SOP yang sudah terpasang tidak dapat dilepas dari kode Java</b> —
	 *   {@code setDisposisiSop(null)} tidak berefek apa pun. Ini yang membuat kuirk
	 *   "pembatalan persetujuan bisa tidak menempel" (lihat Javadoc class) tidak punya
	 *   jalan keluar sederhana lewat setter.</li>
	 *   <li>Ekspresi ternary di baris kedua adalah <b>sisa kode yang sudah tidak pernah
	 *   menyala</b>: setelah lolos penjaga, kondisi
	 *   {@code (disposisiSop == null || disposisiSop.getId() == null)} pasti bernilai
	 *   {@code false}, sehingga cabang yang mempertahankan nilai lama tidak pernah dipilih
	 *   dan hasilnya selalu argumen yang baru. Dibiarkan apa adanya.</li>
	 * </ul>
	 *
	 * @param disposisiSop disposisi SOP yang akan ditautkan; {@code null} atau disposisi
	 *                     tanpa id diabaikan
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {if(disposisiSop==null||disposisiSop.getId()==null) {return;}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;
	}

	/**
	 * Menyetel penyetuju batch secara eksplisit.
	 *
	 * <p><b>PERINGATAN:</b> setter ini <i>tidak</i> berpasangan simetris dengan
	 * {@link #getDisetujuiOleh()}. Untuk batch yang memakai alur SOP, nilai yang disetel di
	 * sini — termasuk {@code null} pada pembatalan persetujuan — akan <b>ditimpa kembali</b>
	 * dari disposisi pada pembacaan berikutnya. Hanya batch tanpa SOP yang mematuhi setter
	 * ini sepenuhnya.</p>
	 *
	 * @param disetujuiOleh pengguna penyetuju; {@code null} untuk membatalkan persetujuan
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Mengembalikan penyetuju batch, <b>diselaraskan paksa dengan alur SOP</b>.
	 *
	 * <p><b>Tujuan:</b> menjadikan disposisi SOP sebagai satu-satunya sumber kebenaran
	 * tentang siapa yang menyetujui pencairan, sehingga kolom {@code disetujui_oleh} tidak
	 * bisa menyimpang dari riwayat alur.</p>
	 *
	 * <p><b>Cara kerja:</b> setelah meng-<i>unwrap</i> proxy dengan {@code check(...)},
	 * ada dua cabang penimpa:</p>
	 * <ol>
	 *   <li>bila disposisi punya simpul "setuju" yang berpengaju
	 *   ({@code getDisposisiSetuju().getDiajukanOleh() != null}), penyetujunya
	 *   <b>diganti</b> dengan pengaju simpul itu — menimpa siapa pun yang tersimpan
	 *   sebelumnya;</li>
	 *   <li>bila disposisi ada tetapi simpul "setuju"-nya belum ada (atau pengajunya
	 *   kosong), penyetuju <b>dipaksa menjadi {@code null}</b>.</li>
	 * </ol>
	 *
	 * <p><b>EFEK SAMPING PENTING:</b> ini getter properti Hibernate, jadi kedua penimpaan
	 * di atas <b>tersimpan ke basis data</b>. Ini sekaligus <b>gerbang de facto tombol
	 * "Realisasikan"</b> — baik layar ZK maupun jalur REST hanya memeriksa
	 * {@code getDisetujuiOleh() != null} sebelum mengizinkan pencairan ditandai, sehingga
	 * hasil getter inilah yang menentukan boleh-tidaknya dana dicairkan.</p>
	 *
	 * <p><b>Kasus tepi:</b> untuk batch tanpa SOP ({@code disposisiSop == null}) kedua
	 * cabang tidak menyala dan nilai tersimpan dikembalikan apa adanya. Akses lazy di sini
	 * <b>tidak</b> dibungkus penangkap exception, berbeda dari
	 * {@link #getTanggalPersetujuan()}.</p>
	 *
	 * @return pengguna penyetuju batch, atau {@code null} bila batch belum disetujui
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
	 * Menyetel tanggal persetujuan secara eksplisit.
	 *
	 * <p>Sama seperti {@link #setDisetujuiOleh(Tbmuser)}, nilai ini bisa ditimpa kembali
	 * oleh {@link #getTanggalPersetujuan()} untuk batch ber-SOP.</p>
	 *
	 * @param tanggalPersetujuan tanggal persetujuan; boleh {@code null}
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengembalikan tanggal persetujuan, <b>diselaraskan dengan alur SOP</b>.
	 *
	 * <p><b>Cara kerja:</b> pasangan logika {@link #getDisetujuiOleh()}, dengan dua
	 * perbedaan halus: cabang pengisian hanya menyala bila tanggalnya <i>masih kosong</i>
	 * ({@code tanggalPersetujuan == null}) — jadi tanggal yang sudah ada tidak diubah —
	 * sedangkan cabang penolan tetap agresif dan memaksa {@code null} kapan pun simpul
	 * "setuju"-nya belum lengkap. Sumber tanggalnya adalah
	 * {@code disposisiSop.getDisposisiSetuju().getWaktu()}.</p>
	 *
	 * <p><b>Penanganan error:</b> seluruh blok dibungkus {@code try/catch} yang menelan
	 * exception dan mencatatnya ke {@code ErrorAuditUtil}. Alasannya terdokumentasi di
	 * komentar inline: {@link #getDisposisiSop()} bisa mengembalikan proxy milik session
	 * yang sudah tertutup (instance kanonik dari {@code AuditTimestampInterceptor}),
	 * sehingga tanpa penangkap ini pembacaan sederhana bisa menggagalkan seluruh halaman.
	 * Konsekuensinya: bila lazy gagal, <b>nilai fallback yang tersimpan dikembalikan</b>
	 * tanpa penyelarasan — tanggal persetujuan bisa tampil meski simpul SOP-nya sudah
	 * tidak mendukungnya lagi.</p>
	 *
	 * <p><b>EFEK SAMPING:</b> penugasan di kedua cabang tersimpan permanen (getter
	 * properti Hibernate).</p>
	 *
	 * @return tanggal persetujuan, atau {@code null} bila belum/tidak lagi disetujui
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {

		try {
			// FIX LazyInitializationException: getDisposisiSop() bisa berupa instance
			// canonical/shared (AuditTimestampInterceptor) yang proxy-nya terikat ke
			// Session lain yang sudah closed -> jangan biarkan getter ini crash, cukup
			// lewati bagian ini (nilai fallback dipertahankan).
			if (tanggalPersetujuan == null && getDisposisiSop() != null
					&& getDisposisiSop().getDisposisiSetuju() != null
					&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
				tanggalPersetujuan = getDisposisiSop().getDisposisiSetuju().getWaktu();
			}

			if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
					|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
				tanggalPersetujuan = null;
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/akunting/ProsesTransfer.java:getTanggalPersetujuan-lazy");
		}
		return tanggalPersetujuan;
	}

	/**
	 * Mengembalikan catatan bebas yang ditulis penyetuju.
	 *
	 * <p>Getter polos, tanpa penyelarasan SOP. Dikosongkan bersamaan dengan
	 * {@code disetujuiOleh}/{@code tanggalPersetujuan} pada jalur "batal setuju" REST.</p>
	 *
	 * @return catatan persetujuan (maks. 2000 karakter), atau {@code null}
	 */
	@Column(name = "catatan_persetujuan", nullable = true, length = 2000)
	public String getCatatanPersetujuan() {
		return catatanPersetujuan;
	}

	/**
	 * Menyetel catatan persetujuan.
	 *
	 * <p>Batas 2000 karakter ditegakkan di lapisan pemanggil (jalur REST menolak lebih dari
	 * itu dengan pesan galat), bukan di setter ini; melebihinya akan gagal di level SQL.</p>
	 *
	 * @param catatanPersetujuan catatan; boleh {@code null}
	 */
	public void setCatatanPersetujuan(String catatanPersetujuan) {
		this.catatanPersetujuan = catatanPersetujuan;
	}

	/**
	 * Mengembalikan tanggal pembuatan batch, <b>diselaraskan dengan awal alur SOP</b>.
	 *
	 * <p><b>Cara kerja:</b> bila disposisi punya simpul awal ({@code getDisposisiStart()})
	 * yang berpengaju, tanggal pembuatan <b>ditimpa</b> dengan waktu pengajuan simpul itu —
	 * tanpa syarat "hanya bila masih kosong", jadi penimpaan terjadi pada setiap pembacaan.
	 * Bila hasil akhirnya masih {@code null}, yang dikembalikan adalah
	 * {@code WaktuUtil.getDate()} (waktu sekarang).</p>
	 *
	 * <p><b>EFEK SAMPING &amp; kasus tepi:</b> penimpaan dari SOP tersimpan permanen.
	 * Sebaliknya, substitusi "waktu sekarang" pada cabang terakhir <b>tidak</b> ditugaskan
	 * ke field, jadi ia hanya kosmetik: kolomnya tetap {@code NULL} di basis data dan
	 * pembacaan besok akan mengembalikan tanggal besok. Artinya batch tanpa SOP dan tanpa
	 * tanggal pembuatan eksplisit akan tampak "dibuat hari ini" selamanya.</p>
	 *
	 * @return tanggal pembuatan batch; tidak pernah {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
				&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
			tanggalPembuatan = getDisposisiSop().getDisposisiStart().getWaktu();
		}

		return tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan;
	}

	/**
	 * Menyetel tanggal pembuatan batch.
	 *
	 * <p>Diisi jalur pembuatan batch dari tanggal yang diketik petugas (atau waktu sekarang
	 * bila dikosongkan). Untuk batch ber-SOP nilainya akan ditimpa
	 * {@link #getTanggalPembuatan()}.</p>
	 *
	 * @param tanggalPembuatan tanggal pembuatan; boleh {@code null}
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Mengembalikan kategori penomoran surat alur keuangan batch ini, <b>dengan pengisian
	 * default otomatis</b>.
	 *
	 * <p><b>Tujuan:</b> setiap dokumen keuangan mendapat nomor surat dari deret yang sesuai
	 * jenisnya; untuk batch transfer, deret yang benar adalah <b>DPC</b> ("Daftar Pengajuan
	 * Cair", kode {@code "006"}).</p>
	 *
	 * <p><b>Cara kerja:</b> bila field masih kosong, ditugaskan konstanta statis
	 * {@link NomorSuratAlurKeuangan#DPC}; bila sudah terisi, proxy-nya di-<i>unwrap</i>
	 * dengan {@code check(...)}.</p>
	 *
	 * <p><b>EFEK SAMPING &amp; kasus tepi:</b> penugasan default tersimpan permanen (getter
	 * properti Hibernate). Konstanta {@code DPC} adalah field <b>statis</b> yang baru terisi
	 * setelah {@code NomorSuratAlurKeuangan.reloadDefault()} dijalankan; sebelum itu
	 * nilainya {@code null}, sehingga getter ini bisa "mengisi default" dengan {@code null}
	 * dan mengulang percobaan itu pada setiap pembacaan. Selain itu instance statis tersebut
	 * dimuat lewat session-nya sendiri, jadi merupakan objek lintas-session yang bisa
	 * menjadi sumber {@code LazyInitializationException} bila relasinya ditelusuri lebih
	 * dalam.</p>
	 *
	 * @return kategori penomoran surat; berpotensi {@code null} bila konstanta statisnya
	 *         belum dimuat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat_alur_keuangan", nullable = true)
	public NomorSuratAlurKeuangan getNomorSuratAlurKeuangan() {
		if (nomorSuratAlurKeuangan == null) {
			nomorSuratAlurKeuangan = NomorSuratAlurKeuangan.DPC;
		} else {
			nomorSuratAlurKeuangan = check(nomorSuratAlurKeuangan);
		}
		return nomorSuratAlurKeuangan;
	}

	/**
	 * Menyetel kategori penomoran surat alur keuangan batch.
	 *
	 * @param nomorSuratAlurKeuangan kategori penomoran; {@code null} akan diganti default
	 *                               {@code DPC} pada pembacaan berikutnya
	 */
	public void setNomorSuratAlurKeuangan(NomorSuratAlurKeuangan nomorSuratAlurKeuangan) {
		this.nomorSuratAlurKeuangan = nomorSuratAlurKeuangan;
	}

	/**
	 * Mengembalikan tahun periode batch, <b>mengisinya dengan tahun berjalan bila kosong</b>.
	 *
	 * <p><b>EFEK SAMPING PENTING:</b> nilainya ditugaskan ke field dan karena ini getter
	 * properti Hibernate, <b>tersimpan permanen</b>. Yang tercatat adalah tahun <i>saat baris
	 * dibaca</i>, bukan tahun saat batch dibuat atau direalisasikan. Untuk batch yang
	 * kolomnya masih {@code NULL} dan baru dibuka setelah pergantian tahun, periode yang
	 * terstempel adalah tahun yang salah — dan karena kolom ini dipakai sebagai penyaring
	 * periode di layar, batch tersebut berpindah ke periode yang keliru secara permanen.</p>
	 *
	 * @return tahun periode batch; tidak pernah {@code null}
	 */
	public Integer getTahun() {
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	/**
	 * Menyetel tahun periode batch.
	 *
	 * @param tahun tahun periode; {@code null} akan diisi otomatis oleh getter
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengembalikan bulan periode batch (1-12), <b>mengisinya dengan bulan berjalan bila
	 * kosong</b>.
	 *
	 * <p><b>Cara kerja:</b> {@code Calendar.MONTH} berbasis nol, jadi ditambah 1 agar
	 * Januari bernilai {@code 1}.</p>
	 *
	 * <p><b>EFEK SAMPING PENTING:</b> sama persis dengan {@link #getTahun()} — nilainya
	 * tersimpan permanen dan mencerminkan bulan <i>saat dibaca</i>, bukan bulan batch
	 * dibuat.</p>
	 *
	 * @return bulan periode batch dalam rentang 1-12; tidak pernah {@code null}
	 */
	public Integer getBulan() {
		if (bulan == null) {
			bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		}
		return bulan;
	}

	/**
	 * Menyetel bulan periode batch.
	 *
	 * @param bulan bulan periode (1-12); {@code null} akan diisi otomatis oleh getter
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Mengembalikan pengguna yang mencatat realisasi — <b>penanda kanonik bahwa dana batch
	 * ini sudah benar-benar cair</b>.
	 *
	 * <p><b>Mengapa penting:</b> field inilah, bukan sebuah kolom status, yang dipakai
	 * seluruh aplikasi untuk membedakan "sudah diajukan" dari "sudah ditransfer". Contoh
	 * pemakaiannya: penyaring daftar DPC
	 * ({@code prosesTransfer.realisasikanOleh IS NULL} vs {@code IS NOT NULL}), penentu
	 * status di {@code DpcTransferStatusHelper}, syarat kelayakan uang muka/kas besar untuk
	 * dipertanggungjawabkan ({@code AmbilDataUangMukaBanbox},
	 * {@code AmbilDataKasBesarBanbox}), serta penentu {@code status} pada layanan UI baru.</p>
	 *
	 * <p><b>Cara kerja:</b> hanya meng-<i>unwrap</i> proxy lazy lewat {@code check(...)};
	 * <b>tidak</b> ada logika penyelarasan SOP dan <b>tidak</b> destruktif — berbeda dari
	 * pasangan persetujuannya. Realisasi memang dicatat manual oleh petugas, bukan
	 * diturunkan dari alur.</p>
	 *
	 * <p><b>Siapa yang mengisinya:</b> tombol "Realisasikan" di {@code ProsesTransferAction}
	 * (mengisi dengan {@code Common.getCurrentUser()}) dan
	 * {@code ProsesTransferApiHelper.realisasikan} (mengisi dengan pemanggil REST).
	 * Pengosongannya dibatasi: hanya pelaksana realisasinya sendiri (atau admin) yang boleh
	 * membatalkan, dan jalur REST menolak pembatalan bila jurnalnya sudah terbentuk.</p>
	 *
	 * @return pengguna pelaksana realisasi, atau {@code null} bila dana belum cair
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "realisasikan_oleh", nullable = true)
	public Tbmuser getRealisasikanOleh() {
		realisasikanOleh = check(realisasikanOleh);
		return realisasikanOleh;
	}

	/**
	 * Menyetel pelaksana realisasi batch.
	 *
	 * <p>Menyetel nilai non-{@code null} sama artinya dengan <b>menyatakan dana sudah
	 * cair</b>; menyetel {@code null} membatalkan pernyataan itu. Perhatikan bahwa
	 * {@link #getTanggalRealisasikan()} akan menyesuaikan diri terhadap perubahan ini pada
	 * pembacaan berikutnya (mengisi tanggal bila baru diisi, mengosongkannya bila
	 * dibatalkan).</p>
	 *
	 * @param realisasikanOleh pelaksana realisasi; {@code null} untuk membatalkan realisasi
	 */
	public void setRealisasikanOleh(Tbmuser realisasikanOleh) {
		this.realisasikanOleh = realisasikanOleh;
	}

	/**
	 * Mengembalikan tanggal dana benar-benar keluar dari rekening bank, <b>sambil
	 * menyelaraskannya dengan {@link #getRealisasikanOleh()}</b>.
	 *
	 * <p><b>Cara kerja:</b> dua cabang saling melengkapi:</p>
	 * <ol>
	 *   <li>bila pelaksana realisasi sudah ada tetapi tanggalnya masih kosong, tanggal
	 *   <b>distempel dengan waktu SEKARANG</b> ({@code WaktuUtil.getDate()});</li>
	 *   <li>bila pelaksananya kosong, tanggal <b>dipaksa menjadi {@code null}</b> —
	 *   membersihkan sisa tanggal setelah pembatalan realisasi.</li>
	 * </ol>
	 *
	 * <p><b>EFEK SAMPING PENTING:</b> ini getter properti Hibernate, jadi kedua penugasan
	 * <b>tersimpan permanen</b>. Cabang pertama berarti <b>tanggal pencairan dana bisa
	 * terbentuk hanya karena baris dibaca</b>, dengan nilai = waktu pembacaan, bukan waktu
	 * transfer sesungguhnya. Kondisi itu tercapai kapan pun {@code realisasikan_oleh} terisi
	 * sementara {@code tanggal_realisasikan} masih {@code NULL} — mis. baris hasil impor,
	 * hasil pembaruan SQL langsung, atau jalur ZK yang gagal mengisi tanggalnya karena
	 * komponen {@code tanggalRealisasikan} bernilai {@code null}. Tanggal itu selanjutnya
	 * diteruskan ke mesin posting sebagai tanggal jurnal
	 * ({@code PostingProsesTransferAction.postingSatu}), sehingga <b>tanggal jurnal
	 * pencairan bisa ikut melenceng ke tanggal pembacaan</b>.</p>
	 *
	 * <p><b>Kasus tepi:</b> karena cabang kedua agresif, menyetel tanggal secara eksplisit
	 * tanpa mengisi pelaksana realisasi tidak akan bertahan — pembacaan berikutnya
	 * menghapusnya lagi.</p>
	 *
	 * @return tanggal realisasi, atau {@code null} bila dana belum cair
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_realisasikan")
	public Date getTanggalRealisasikan() {
		if (getRealisasikanOleh() != null && tanggalRealisasikan == null) {
			tanggalRealisasikan = WaktuUtil.getDate();
		} else if (getRealisasikanOleh() == null) {
			tanggalRealisasikan = null;
		}
		return tanggalRealisasikan;
	}

	/**
	 * Menyetel tanggal realisasi batch.
	 *
	 * <p>Diisi dari komponen tanggal di layar ZK atau dari field {@code tanggalRealisasikan}
	 * pada permintaan REST (yang mewajibkannya untuk realisasi baru). Nilai ini akan
	 * dikosongkan kembali oleh {@link #getTanggalRealisasikan()} bila
	 * {@link #getRealisasikanOleh()} kosong.</p>
	 *
	 * @param tanggalRealisasikan tanggal dana cair; boleh {@code null}
	 */
	public void setTanggalRealisasikan(Date tanggalRealisasikan) {
		this.tanggalRealisasikan = tanggalRealisasikan;
	}

	/**
	 * Mengembalikan catatan bebas yang ditulis pelaksana realisasi.
	 *
	 * <p>Getter polos. Biasanya dipakai mencatat nomor referensi transfer bank atau
	 * keterangan pencairan.</p>
	 *
	 * @return catatan realisasi (maks. 2000 karakter), atau {@code null}
	 */
	@Column(name = "catatan_realisasi", nullable = true, length = 2000)
	public String getCatatanRealisasi() {
		return catatanRealisasi;
	}

	/**
	 * Menyetel catatan realisasi.
	 *
	 * <p>Batas 2000 karakter ditegakkan lapisan pemanggil (jalur REST menolak yang lebih
	 * panjang, dan menyimpan {@code null} bila catatannya kosong), bukan di setter ini.</p>
	 *
	 * @param catatanRealisasi catatan realisasi; boleh {@code null}
	 */
	public void setCatatanRealisasi(String catatanRealisasi) {
		this.catatanRealisasi = catatanRealisasi;
	}
}
