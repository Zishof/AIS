package ais.database.model.payroll;
/* ENHANCED_PENGGUNAAN_ANGGARAN_MEMORY_SAFE_2026_06_03 - Java 1.6/1.7 compatible. */

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

import org.hibernate.Session;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.StandingInstruction;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.asset.NomorSuratAlurPengadaan;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.Workspace;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;

/**
 * Dokumen <b>batch pembayaran gaji pegawai</b> untuk satu periode &mdash; kepala (header) dari
 * seluruh rantai penggajian AIS. Satu baris entity ini mewakili satu perintah bayar gaji untuk
 * <b>sekelompok pegawai sekaligus</b>, dibatasi oleh kombinasi bulan/tahun
 * ({@link #getBulan()}/{@link #getTahun()}), cara pembayaran
 * ({@link #getCaraPembayaranGaji()}), dan satuan kerja ({@link #getSatuanKerja()}). Tabelnya
 * {@code payroll.pembayaran_gaji}, di-audit penuh oleh Hibernate Envers ({@code @Audited} &rarr;
 * tabel bayangan {@code pembayaran_gaji_aud}), serta memakai {@code dynamicInsert}/
 * {@code dynamicUpdate} sehingga hanya kolom yang benar-benar berubah yang masuk ke pernyataan SQL.
 *
 * <h3>Posisi dalam rantai penggajian (TERVERIFIKASI dari pemanggil)</h3>
 * <p>
 * Struktur datanya berlapis tiga, dari kepala ke daun:
 * </p>
 * <ol>
 * <li><b>{@code PembayaranGaji}</b> (kelas ini) &mdash; kepala dokumen: periode, cara bayar,
 * satuan kerja, total nominal, alur persetujuan, dan status posting jurnal.</li>
 * <li><b>{@code PembayaranGajiPunyaPegawai}</b> &mdash; satu baris <b>per pegawai</b> dalam batch
 * ini; memegang {@code pegawai}, {@code nilai} (take-home pay pegawai tersebut),
 * {@code formatItemGaji}, serta rentang {@code mulai}/{@code sampai}. Relasinya ke kelas ini
 * adalah {@code @ManyToOne} bernama {@code pembayaranGaji} pada sisi anak; <b>kelas ini tidak
 * memegang koleksi balik</b>, jadi seluruh pembacaan anak dilakukan lewat
 * {@code Criteria(PembayaranGajiPunyaPegawai).add(eq("pembayaranGaji", ...))} &mdash; pola yang
 * dipakai konsisten oleh {@link #hitungUlang(PembayaranGaji)} dan kedua mesin posting.</li>
 * <li><b>{@code PembayaranItemGajiPegawai}</b> &mdash; satu baris <b>per komponen gaji per
 * pegawai</b> (gaji pokok, tunjangan, potongan, dst). Di sinilah akun buku besar sesungguhnya
 * tinggal: {@code akun} (sisi kredit) dan {@code akunDebet} (sisi debet). Tanpa kedua akun ini
 * terisi, dokumen gaji <b>tidak dapat dijurnal sama sekali</b> &mdash; mesin posting melewatinya
 * diam-diam tanpa menandainya terposting.</li>
 * </ol>
 *
 * <h3>Ringkasan alur pembayaran gaji (untuk pembaca masa depan)</h3>
 * <ol>
 * <li><b>Penyusunan.</b> Operator membuka layar {@code PembayaranGajiAction} (ZUL
 * {@code pembayaran_gaji.zul}) atau {@code BayarGajiPegawaiAction}, memilih bulan/tahun/cara
 * bayar/satuan kerja, lalu menarik daftar pegawai beserta komponen gajinya. {@code onSave()} pada
 * Action itu menolak duplikat: kombinasi (tahun, bulan, caraPembayaranGaji, satuanKerja) yang
 * sudah ada di basis data ditolak dengan pesan peringatan &mdash; itulah satu-satunya penjaga
 * "satu batch per periode per unit".</li>
 * <li><b>Penomoran surat.</b> Kode dokumen dibangkitkan
 * {@code PembayaranGajiAction.generateCode()} dari template
 * {@link NomorSuratAlurPengadaan#PEMBAYARAN_GAJI_PEGAWAI} &mdash; katalog yang sama yang dipakai
 * rantai pengadaan. {@link #getNomorSuratAlurPengadaan()} pada kelas ini mengembalikan konstanta
 * global itu sebagai bawaan bila kolomnya kosong.</li>
 * <li><b>Total dihitung ulang.</b> {@link #hitungUlang(PembayaranGaji)} menjumlahkan
 * {@code nilai} seluruh {@code PembayaranGajiPunyaPegawai} milik dokumen ini dan menuliskannya ke
 * {@link #setNilai(Double)}. Nilai kepala dokumen karena itu adalah <b>hasil agregasi</b>, bukan
 * angka yang diketik manusia.</li>
 * <li><b>Perintah transfer terbit.</b> Bila konfigurasi
 * {@code gaji_masuk_standing_instruction_baru} menyala,
 * {@code StandingInstruction.simpanPembayaranGajiPunyaPegawai(PembayaranGaji)} menerbitkan satu
 * baris {@link StandingInstruction} baru untuk dokumen ini, dan hasilnya disimpan balik ke
 * {@link #setStandingInstruction(StandingInstruction)}. SI itulah yang kemudian dikumpulkan ke
 * dalam dokumen batch {@code ProsesTransferStandingInstruction}, disetujui, direalisasikan, dan
 * dicetak sebagai surat perintah transfer ke bank. Perlu ditegaskan: <b>tidak ada mesin yang
 * mengirim uang</b> &mdash; realisasi transfer adalah tindakan manusia di luar aplikasi, aplikasi
 * hanya menerbitkan kertas perintahnya.</li>
 * <li><b>Persetujuan SOP.</b> Dokumen ini {@code extends} {@link DataSop}, jadi alur
 * persetujuannya dititipkan sepenuhnya ke {@link DisposisiSop}: siapa mengajukan
 * ({@link #getDibuatOleh()}), siapa menyetujui ({@link #getDisetujuiOleh()}), kapan
 * ({@link #getTanggalPersetujuan()}) &mdash; semuanya <b>diturunkan dari langkah disposisi</b>,
 * bukan diisi langsung oleh layar.</li>
 * <li><b>Posting jurnal.</b> Dua mesin memposting dokumen ini, keduanya menghasilkan
 * {@link PostingHistory} berjenis {@code JENIS_PENGGAJIAN}:
 * <ul>
 * <li>{@code PostingTransaksiPembayaranGajiAction} &mdash; baris dasbor <b>"Gaji"</b>. Kredit:
 * akun tiap {@code PembayaranItemGajiPegawai} ditambah akun bank rekening pegawai (atau akun
 * {@link CaraPembayaranGaji#getAkun()} bila pegawai tak punya bank); debet: {@code akunDebet}
 * tiap item. Jurnal ditulis lewat {@code CommonAkunting.saveTransaksi(...)} yang membuat
 * {@code GrupTransaksi} + baris-baris {@code Transaksi} &mdash; mesin double-entry yang sama
 * dengan seluruh modul akunting.</li>
 * <li>{@code PostingTransaksiPenggajianAction} &mdash; baris dasbor <b>"Penggajian Pegawai"</b>,
 * membaca {@link StandingInstruction#getProsesStanding()} milik dokumen ini.</li>
 * </ul>
 * Setelah berhasil, {@link #setPostingHistory(PostingHistory)} distempel; itulah satu-satunya
 * penanda "sudah dijurnal".</li>
 * <li><b>Realisasi anggaran.</b> {@code rab.PenggunaanAnggaran} memegang FK {@code pembayaranGaji}
 * dan memperlakukan dokumen ini sebagai salah satu sumber penyerapan anggaran (label
 * {@code "GAJI " + kode}). Bendera {@link #getTanpaAnggaran()} membebaskan dokumen dari
 * pembebanan anggaran.</li>
 * <li><b>Pelaporan.</b> Sekitar delapan laporan JasperReports membaca rantai ini
 * ({@code LaporanRekapPembayaranGaji}, {@code LaporanRekapPembayaranGajiPerSatuanKerja},
 * {@code LaporanSlipGajiPegawaiPerOrang}, {@code LaporanSlipGajiRealPegawaiPerOrang},
 * {@code LaporanItemGajiPegawai}, {@code employ.LaporanPembayaranGaji}, dst), ditambah dasbor
 * {@code DasborAnalisisPenggajian}/{@code DasborPenggajianDetailHelper}.</li>
 * </ol>
 *
 * <h3>Pengelompokan method</h3>
 * <ul>
 * <li><b>Identitas &amp; jejak audit:</b> {@link #getId()}, {@link #getKodeUnik()},
 * {@link #getOleh()}, {@link #getOlehId()}, {@link #getTanggal_dirubah()}, {@link #onUpdate()},
 * {@link #toString()}.</li>
 * <li><b>Periode &amp; deskripsi:</b> {@link #getKeterangan()}, {@link #getWaktuBayar()},
 * {@link #getBulan()}, {@link #getTahun()}, {@link #getTanggalTransaksi()}.</li>
 * <li><b>Nominal:</b> {@link #getNilai()}, {@link #hitungUlang(PembayaranGaji)}.</li>
 * <li><b>Cara bayar &amp; perintah transfer:</b> {@link #getCaraPembayaranGaji()},
 * {@link #getStandingInstruction()}.</li>
 * <li><b>Alur persetujuan (SOP):</b> {@link #getDisposisiSop()}, {@link #getDibuatOleh()},
 * {@link #getTanggalPembuatan()}, {@link #getDisetujuiOleh()}, {@link #getTanggalPersetujuan()},
 * {@link #getDitolakOleh()}, {@link #getTanggalDitolak()}, {@link #getAktif()}.</li>
 * <li><b>Cakupan organisasi &amp; anggaran:</b> {@link #getSatuanKerja()},
 * {@link #getWorkspace()}, {@link #getTanpaAnggaran()},
 * {@link #getNomorSuratAlurPengadaan()}.</li>
 * <li><b>Status jurnal:</b> {@link #getPostingHistory()}.</li>
 * </ul>
 *
 * <h3>Hal non-obvious yang WAJIB diketahui sebelum menyunting</h3>
 * <ol>
 * <li><b>Deklarasi ulang field itu KEHARUSAN TEKNIS, bukan duplikasi keliru.</b> Rantai
 * warisannya {@code PembayaranGaji extends} {@link DataSop} {@code extends}
 * {@link ais.database.model.GeneralValueObject}, dan {@code GeneralValueObject} <b>bukan</b>
 * {@code @Entity} maupun {@code @MappedSuperclass} &mdash; ia POJO abstrak biasa yang propertinya
 * <b>tidak</b> dipetakan Hibernate. Karena itu {@code id}, {@code keterangan}, {@code aktif},
 * {@code oleh}, {@code olehId} sengaja dideklarasikan ulang di sini; jangan "dirapikan". Method
 * bantu {@code check(...)} yang dipakai banyak getter di bawah juga berasal dari kelas induk
 * tersebut dan berfungsi meresolusi proxy lazy lewat {@code EntityIdentityMap}.</li>
 * <li><b>Konsekuensi lanjutan yang mudah terlewat: {@code kode} TIDAK IKUT DIDEKLARASIKAN
 * ULANG.</b> Berbeda dari {@code keterangan} dan {@code aktif}, properti {@code kode} milik
 * {@code GeneralValueObject} tidak dideklarasikan ulang di kelas ini, sehingga
 * <b>tidak dipetakan ke kolom mana pun</b> pada {@code payroll.pembayaran_gaji}. Nomor surat yang
 * dibangkitkan {@code PembayaranGajiAction.generateCode()} lewat {@code setKode(...)} karena itu
 * hanya hidup selama objeknya masih di memori; begitu dokumen dimuat ulang dari basis data,
 * {@code getKode()} kembali {@code null}. Efek berantainya terlihat langsung pada
 * {@link #getKodeUnik()} (menghasilkan {@code "null_<id>"}) dan pada
 * {@code PenggunaanAnggaran.getNama()} (menghasilkan {@code "GAJI null"}). Ini bug data, bukan
 * keamanan &mdash; dan memperbaikinya berarti menambah kolom, jadi <b>di luar cakupan
 * dokumentasi ini</b>.</li>
 * <li><b>Banyak getter di kelas ini bersifat DESTRUKTIF (write-back).</b> Karena Hibernate
 * memetakan kelas ini dengan <i>property access</i> (anotasi dipasang di getter) dan
 * {@code dynamicUpdate} aktif, cukup <b>membaca</b> objek ini di dalam sesi yang kemudian
 * di-flush/commit untuk menerbitkan {@code UPDATE} sungguhan. Tujuh getter menimpa field
 * persisten: {@link #getBulan()}, {@link #getTahun()}, {@link #getTanggalTransaksi()},
 * {@link #getSatuanKerja()}, {@link #getKodeUnik()}, {@link #getNomorSuratAlurPengadaan()},
 * {@link #getDisetujuiOleh()}/{@link #getTanggalPersetujuan()} (kedua terakhir bahkan dapat
 * <b>mengosongkan</b> persetujuan yang sudah sah). Rinciannya ada di Javadoc masing-masing.</li>
 * <li><b>Kelayakan posting hanya diperiksa dari DUA kolom.</b> Kriteria kedua mesin posting
 * ({@code kriteriaPostingStatic()} dan {@code initCriteria()}) adalah
 * {@code standingInstruction IS NOT NULL AND disetujuiOleh IS NOT NULL}. Tidak ada pemeriksaan
 * bahwa persetujuan itu benar-benar berasal dari langkah SOP, tidak ada pemisahan tugas
 * (pembuat boleh sama dengan penyetuju), dan tidak ada penyaring satuan kerja sama sekali.
 * Lihat catatan pada {@link #getDisetujuiOleh()}.</li>
 * <li><b>Pembatalan posting tidak simetris dengan posting.</b> Kedua jalur pembatalan menghapus
 * jurnal dengan {@code delete from akunting.grup_transaksi where pembayaran_gaji=<id> and closing
 * is null}. Baris jurnal yang periodenya sudah ditutup ({@code closing} terisi) <b>tetap
 * tinggal</b>, tetapi {@code postingHistory} tetap dikosongkan &mdash; dokumen jadi layak
 * diposting ulang dan menerbitkan set jurnal KEDUA yang utuh untuk gaji yang sama. Ini pola yang
 * sama dengan temuan pencairan dana di modul akunting; lihat
 * {@link #getPostingHistory()}.</li>
 * <li><b>Cascade basis data bekerja DUA ARAH dengan {@link StandingInstruction}.</b> Menurut
 * {@code cascade.sql}: {@code standing_instruction.pembayaran_gaji} &rarr;
 * {@code payroll.pembayaran_gaji(id)} {@code ON DELETE CASCADE}, <b>dan</b>
 * {@code payroll.pembayaran_gaji.standing_instruction} &rarr;
 * {@code akunting.standing_instruction(id)} {@code ON DELETE CASCADE}. Artinya menghapus satu
 * baris SI akan <b>ikut menghapus dokumen gajinya</b> (beserta seluruh baris per pegawai dan per
 * item di bawahnya), bukan sekadar memutus tautannya. Jangan pernah menghapus SI penggajian
 * sebagai "pembersihan".</li>
 * <li><b>Ada kolom warisan yang tidak lagi dipetakan.</b> {@code cascade.sql} masih memasang
 * constraint {@code pembayaran_gaji.daftar_pengajuan_transfer} &rarr;
 * {@code akunting.daftar_pengajuan_transfer(id)}, padahal kelas ini tidak lagi mendeklarasikan
 * properti tersebut. Kolomnya masih ada di basis data lama; jangan bingung saat membaca skema.</li>
 * <li><b>Data yang disentuh kelas ini adalah data pribadi pegawai.</b> Nominal gaji per pegawai
 * tinggal di {@code PembayaranGajiPunyaPegawai}, dan rekening bank tujuan diambil dari
 * {@code Pegawai.ambilBank(...)} saat posting. Setiap perubahan pada gerbang akses rantai ini
 * berdampak privasi, bukan sekadar finansial.</li>
 * </ol>
 *
 * <h3>Catatan keamanan &amp; integritas hasil audit (per 3 September 2026)</h3>
 * <p>
 * Bagian ini merekam hasil verifikasi terhadap kode pemanggil <b>apa adanya</b>. Tidak ada
 * perilaku yang diubah oleh dokumentasi ini.
 * </p>
 * <ol>
 * <li><b>Gerbang tombol "Posting Data" &mdash; TIDAK ADA.</b> Pada
 * {@code PostingTransaksiPembayaranGajiAction}, tombol <i>Batalkan Posting</i> dipagari
 * {@code adminLain} ({@code Common.getApakahAdmin() ||
 * CommonPrivilages.checkPrevilages(APPROVE)}), tetapi tombol <i>Posting Data</i> hanya dipagari
 * keadaan data ({@code getPostingHistory() == null}) &mdash; <b>nol pemeriksaan hak</b>.
 * Asimetrinya terbalik dari yang diharapkan: membatalkan jurnal butuh admin, sedangkan
 * <b>menerbitkan</b> jurnal gaji ke buku besar tidak butuh apa pun selain bisa membuka layarnya.
 * Ini pola yang sama dengan tombol "Realisasikan" di rantai pencairan dana akunting.</li>
 * <li><b>Jalur REST massal fail-open pada peran {@code null}.</b>
 * {@code DraftJurnalApiHelper.bolehAksi()} berbunyi {@code if (peran == null) return true;}
 * sebelum memanggil {@code EbisnisMenuKatalog.bolehAksiAkuntansi(...)}. Gerbang itulah yang
 * menjaga ketiga baris payroll ({@code "Gaji"}, {@code "Penggajian Pegawai"},
 * {@code "Transaksi Pegawai"}, semuanya dipetakan ke kunci menu {@code gaji}) untuk memanggil
 * {@code postingSemua(...)}/{@code batalkanPostingSemua(...)}. Pengguna tanpa peran terpasang
 * lolos sepenuhnya. Ini instansiasi pola fail-open yang sama yang sudah tercatat di sembilan
 * helper API modul akunting.</li>
 * <li><b>Cakupan tenant fail-open pada mesin posting.</b> Baik {@code kriteriaPostingStatic()}
 * (jalur REST) maupun {@code initCriteria()} (jalur ZK) <b>tidak memasang satu pun restriksi
 * {@code satuanKerja}</b>. Satu penekanan tombol memposting &mdash; atau membatalkan &mdash;
 * dokumen gaji <b>seluruh satuan kerja pada instalasi</b>, bukan hanya milik penggunanya.</li>
 * <li><b>Generic CRUD v2 menjangkau rantai ini, dan {@code pegawai} bukan sumbu cakupan.</b>
 * Terverifikasi konkret: {@code webapp/WEB-INF/new/payroll/services/pembayaran_gaji_service.jsp}
 * adalah <i>scaffold</i> yang meneruskan ke {@code _shared/services/dispatcher.jsp}, yang
 * memanggil {@code GenericCrudDefinitionRegistry.tryAutoRegister("payroll", "pembayaran_gaji",
 * {"CaraPembayaranGaji", "PembayaranGaji", "PembayaranGajiPunyaPegawai", "Pegawai"}, ...)}.
 * Nama kelas ini tidak mengandung satu pun token {@code BLOCKED_CLASS_TOKENS}, jadi definisinya
 * terbit dalam mode {@code FULL_CRUD}. Penyaring cakupannya
 * ({@code GenericCrudAutoEntityAdapter.scopeBindings()}) hanya mengenal properti
 * {@code yayasan}, {@code sekolah}, {@code program}, {@code fakultas}, {@code jurusan},
 * {@code satuanKerja}, ditambah {@code mahasiswa}/{@code siswa}/{@code dosen}/{@code guru}/
 * {@code orangTua}/{@code anggotaKoperasi} yang bersyarat peran. <b>Properti {@code pegawai}
 * tidak ada di daftar itu.</b> Akibatnya berbeda untuk dua tingkat rantai ini:
 * <ul>
 * <li>kelas ini <b>punya</b> {@code satuanKerja}, jadi cakupannya berlaku &mdash; tetapi hanya
 * bila {@code Tbmuser.getSatuanKerja()} tidak {@code null}; {@code addScope()} langsung
 * {@code return} pada nilai {@code null}, sehingga pengguna tanpa satuan kerja melihat
 * semuanya;</li>
 * <li>{@code PembayaranGajiPunyaPegawai} <b>tidak punya</b> satu pun properti yang dikenali
 * daftar itu (hanya {@code pembayaranGaji} dan {@code pegawai}), sehingga baris nominal gaji
 * <b>per pegawai</b> tidak tersaring sama sekali &mdash; tanpa syarat, lintas seluruh tenant.</li>
 * </ul>
 * Gerbang <i>privilege</i>-nya sendiri ({@code GenericCrudPrivilegeGuard}) justru fail-closed dan
 * benar; yang bocor adalah cakupan datanya.</li>
 * <li><b>Validasi cakupan objek dapat dikalahkan oleh getter write-back.</b>
 * {@code GenericCrudAutoEntityAdapter.validateObjectScope()} membaca nilai properti lewat
 * {@code ClassMetadata.getPropertyValue(object, "satuanKerja", POJO)} &mdash; yang pada pemetaan
 * <i>property access</i> berarti memanggil {@link #getSatuanKerja()}. Getter itu <b>menimpa</b>
 * satuan kerja dokumen dengan satuan kerja {@link CaraPembayaranGaji} yang dipilih. Jadi cakupan
 * yang diperiksa bukan cakupan yang tersimpan, melainkan cakupan yang baru saja ditulis ulang
 * oleh pilihan cara bayar pada request yang sedang diperiksa.</li>
 * <li><b>Persetujuan dapat berdiri tanpa langkah SOP.</b> Lihat
 * {@link #getDisetujuiOleh()}: seluruh logika penurunan dan pembatalan di dalamnya dijaga
 * {@code if (getDisposisiSop() != null ...)}. Ketika {@code disposisiSop} bernilai {@code null},
 * <b>tidak satu pun cabang berjalan</b> dan nilai kolom mentah diteruskan apa adanya. Karena
 * {@link #setDisposisiSop(DisposisiSop)} menolak argumen {@code null}/tanpa id, dokumen yang lahir
 * di luar layar SOP tidak akan pernah punya disposisi &mdash; dan kriteria kelayakan posting
 * (butir 4 di bagian sebelumnya) hanya menuntut kolom {@code disetujui_oleh} terisi.</li>
 * <li><b>Pembatalan posting luput dari Envers.</b> Penghapusan {@code grup_transaksi}/
 * {@code transaksi} dilakukan lewat {@code createSQLQuery(...)} mentah, sehingga tidak
 * menghasilkan revisi Envers. Riwayat audit hanya memperlihatkan {@code postingHistory} berubah
 * jadi {@code null}, bukan jurnal apa yang lenyap. (Id yang dirangkai ke dalam SQL bertipe
 * {@code Long}, jadi ini bukan celah injeksi.)</li>
 * </ol>
 *
 * @see PembayaranGajiPunyaPegawai
 * @see CaraPembayaranGaji
 * @see StandingInstruction
 * @see PostingHistory
 * @see DisposisiSop
 * @see DataSop
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "payroll", name = "pembayaran_gaji")
public class PembayaranGaji extends DataSop {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna (user id) terakhir yang mengubah baris ini.
	 *
	 * @return user id pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel identitas pengguna pengubah terakhir.
	 *
	 * <p><b>Menolak nilai kosong secara diam-diam:</b> argumen {@code null}, string kosong, atau
	 * hanya berisi spasi diabaikan &mdash; method langsung {@code return} tanpa menyentuh field.
	 * Konsekuensinya jejak audit tidak pernah bisa dikosongkan atau "dinetralkan" lewat setter
	 * ini, tetapi juga berarti pemanggil tidak akan pernah tahu bahwa penyetelannya gagal.
	 * Diisi otomatis oleh {@code AuditTimestampInterceptor}.</p>
	 *
	 * @param olehId user id pengubah; nilai kosong/{@code null} diabaikan
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks dokumen: <b>hanya {@link #getKeterangan()}</b>.
	 *
	 * <p>Sengaja tidak memakai {@code toString()} bawaan {@code GeneralValueObject} (yang
	 * merangkai {@code kode - nama}), karena {@code kode} tidak dipetakan pada entity ini (lihat
	 * catatan kelas) sehingga hasilnya akan selalu berawalan {@code "null - "}. Nilai balik dapat
	 * berupa {@code null} bila keterangan belum diisi &mdash; pemanggil yang merangkainya ke
	 * dalam label harus siap menerima {@code null}.</p>
	 *
	 * @return isi kolom {@code keterangan}, bisa {@code null}
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Menyetel nama tampil pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong diabaikan diam-diam.</p>
	 *
	 * @param oleh nama pengubah; nilai kosong/{@code null} diabaikan
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama tampil pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: menstempel jejak audit sebelum setiap {@code UPDATE}.
	 *
	 * <p>Mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)}, yang mengisi
	 * {@link #setOleh(String)}, {@link #setOlehId(String)}, dan
	 * {@link #setTanggal_dirubah(Date)} dari pengguna sesi aktif. Dipanggil oleh penyedia
	 * persistence, <b>bukan</b> oleh kode aplikasi &mdash; jangan panggil manual.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan; biasanya diisi {@code AuditTimestampInterceptor}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir.
	 *
	 * <p>Diinisialisasi ke waktu server saat objek dibuat, jadi dokumen yang belum pernah
	 * disimpan pun sudah memiliki nilai.</p>
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	private String keterangan;
	private Date waktuBayar = ais.ui.util.WaktuUtil.getDate();
	private Integer bulan;
	private Integer tahun;
	private CaraPembayaranGaji caraPembayaranGaji;
	private StandingInstruction standingInstruction;
	private Double nilai;
	private Date tanggalTransaksi;

	private Date tanggalPembuatan;
	private Date tanggalPersetujuan;
	private Tbmuser dibuatOleh;
	private Tbmuser disetujuiOleh;

	private NomorSuratAlurPengadaan nomorSuratAlurPengadaan;
	private Workspace workspace;
	private SatuanKerja satuanKerja;
	private DisposisiSop disposisiSop;

	private PostingHistory postingHistory;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate.
	 *
	 * <p>Field {@code waktuBayar} dan {@code tanggal_dirubah} sudah terisi waktu server lewat
	 * inisialisasi field, sehingga dokumen baru langsung punya periode bawaan (bulan/tahun
	 * berjalan) tanpa perlu disetel pemanggil.</p>
	 */
	public PembayaranGaji() {
	}

	/**
	 * Mengembalikan kunci primer dokumen.
	 *
	 * @return id dokumen, {@code null} bila belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci primer dokumen. Hanya untuk Hibernate dan kode migrasi.
	 *
	 * @param id kunci primer
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan keterangan bebas dokumen; juga menjadi hasil {@link #toString()}.
	 *
	 * @return keterangan dokumen, bisa {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas dokumen. Tanpa validasi.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan bulan periode gaji (1&ndash;12), menurunkannya dari
	 * {@link #getWaktuBayar()} bila kolomnya masih kosong.
	 *
	 * <p><b>Getter write-back.</b> Bila {@code bulan} bernilai {@code null}, method ini
	 * <b>menulis</b> hasil turunan ke field. Karena pemetaan kelas ini memakai <i>property
	 * access</i> dan {@code dynamicUpdate} aktif, membaca dokumen lama di dalam sesi yang
	 * kemudian di-flush akan mengisi kolom {@code bulan} di basis data secara permanen. Ini
	 * disengaja: dokumen warisan yang belum punya kolom periode "disembuhkan" saat pertama kali
	 * dibaca. Efek sampingnya, kolom {@code bulan} tidak pernah bisa sengaja dibiarkan kosong.</p>
	 *
	 * <p><b>Kasus tepi:</b> nilai yang <i>sudah</i> tersimpan tidak pernah dikoreksi. Bila
	 * {@link #setWaktuBayar(Date)} mengubah tanggal bayar ke bulan lain, {@code bulan} tetap
	 * memegang nilai lama sampai ada yang menyetelnya ulang &mdash; dan penyaring layar posting
	 * ({@code Restrictions.eq("bulan", ...)}) memakai kolom ini, bukan tanggalnya.</p>
	 *
	 * @return bulan periode gaji, 1 untuk Januari sampai 12 untuk Desember
	 */
	public Integer getBulan() {
		if (bulan == null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(getWaktuBayar());
			bulan = calendar.get(Calendar.MONTH) + 1;
		}
		return bulan;
	}

	/**
	 * Menyetel bulan periode gaji.
	 *
	 * @param bulan bulan 1&ndash;12; tidak divalidasi
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Mengembalikan tahun periode gaji, menurunkannya dari {@link #getWaktuBayar()} bila
	 * kolomnya masih kosong.
	 *
	 * <p><b>Getter write-back</b> dengan sifat yang sama persis seperti {@link #getBulan()}:
	 * mengisi field bila {@code null}, tidak pernah mengoreksi nilai yang sudah ada, dan dapat
	 * menerbitkan {@code UPDATE} hanya karena dibaca.</p>
	 *
	 * @return tahun periode gaji (empat digit)
	 */
	public Integer getTahun() {
		if (tahun == null) {
			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(getWaktuBayar());
			tahun = calendar.get(Calendar.YEAR);
		}
		return tahun;
	}

	/**
	 * Menyetel tahun periode gaji.
	 *
	 * @param tahun tahun empat digit; tidak divalidasi
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengembalikan tanggal/waktu pembayaran gaji &mdash; <b>sumbu waktu utama dokumen ini</b>.
	 *
	 * <p>Kolomnya bernama {@code waktubayar} di basis data (dipakai apa adanya oleh
	 * {@code Restrictions.sqlRestriction("date(waktubayar) between ...")} pada kedua mesin
	 * posting). Nilai ini juga menjadi sumber {@link #getBulan()}, {@link #getTahun()},
	 * {@link #getTanggalTransaksi()}, dan tanggal jurnal saat posting.</p>
	 *
	 * <p><b>Getter write-back ringan:</b> bila field bernilai {@code null} ia diisi waktu server
	 * saat ini. Jadi dokumen tidak pernah tampil tanpa tanggal bayar &mdash; termasuk dokumen
	 * lama yang kolomnya memang kosong, yang akan "mendapat" tanggal hari ini alih-alih
	 * memperlihatkan bahwa datanya hilang.</p>
	 *
	 * @return tanggal/waktu pembayaran gaji, tidak pernah {@code null}
	 */
	public Date getWaktuBayar() {
		if (waktuBayar == null) {
			waktuBayar = ais.ui.util.WaktuUtil.getDate();
		}
		return waktuBayar;
	}

	/**
	 * Menyetel tanggal/waktu pembayaran gaji.
	 *
	 * <p><b>Perhatikan:</b> mengubah nilai ini <b>tidak</b> menyelaraskan {@link #getBulan()} dan
	 * {@link #getTahun()} yang sudah tersimpan; keduanya hanya diturunkan saat masih
	 * {@code null}. Bila periode dokumen benar-benar dipindah, ketiganya harus disetel bersama.</p>
	 *
	 * @param waktuBayar tanggal bayar baru
	 */
	public void setWaktuBayar(Date waktuBayar) {
		this.waktuBayar = waktuBayar;
	}

	/**
	 * Mengembalikan cara pembayaran gaji yang dipakai batch ini (mis. Tunai, Transfer Bank).
	 *
	 * <p>Selain sebagai label, master ini memasok dua hal yang berdampak nyata:</p>
	 * <ul>
	 * <li>{@link CaraPembayaranGaji#getAkun()} &mdash; akun kas/bank yang <b>dikredit</b> saat
	 * posting jurnal untuk pegawai yang tidak punya rekening bank terdaftar;</li>
	 * <li>{@link CaraPembayaranGaji#getSatuanKerja()} &mdash; yang <b>menimpa</b> satuan kerja
	 * dokumen ini setiap kali {@link #getSatuanKerja()} dibaca (lihat catatan di sana).</li>
	 * </ul>
	 *
	 * <p>Memanggil {@code check(...)} lebih dulu untuk meresolusi proxy lazy; nilai balik bisa
	 * berupa instance berbeda dari yang tersimpan di field sebelumnya.</p>
	 *
	 * @return master cara pembayaran, atau {@code null} bila belum dipilih
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "cara_pembayaran_gaji", nullable = true)
	public CaraPembayaranGaji getCaraPembayaranGaji() {
		caraPembayaranGaji = check(caraPembayaranGaji);
		return caraPembayaranGaji;
	}

	/**
	 * Menyetel cara pembayaran gaji batch ini.
	 *
	 * @param caraPembayaranGaji master cara pembayaran
	 */
	public void setCaraPembayaranGaji(CaraPembayaranGaji caraPembayaranGaji) {
		this.caraPembayaranGaji = caraPembayaranGaji;
	}

	/**
	 * Mengembalikan perintah transfer ({@link StandingInstruction}) yang diterbitkan untuk
	 * dokumen ini.
	 *
	 * <p><b>Kolom ini adalah separuh dari gerbang kelayakan posting.</b> Kedua mesin posting
	 * menyaring {@code standingInstruction IS NOT NULL}; dokumen tanpa SI tidak pernah dijurnal.
	 * SI-nya sendiri diterbitkan {@code StandingInstruction.simpanPembayaranGajiPunyaPegawai(...)}
	 * saat dokumen disimpan, dan <b>hanya bila</b> konfigurasi
	 * {@code gaji_masuk_standing_instruction_baru} bernilai benar. Bila konfigurasi itu mati,
	 * dokumen gaji tidak akan pernah bisa diposting lewat kedua mesin tersebut.</p>
	 *
	 * <p>Memakai {@code FetchMode.SELECT} (bukan join) sehingga relasi ini selalu dimuat lewat
	 * query terpisah; ini yang membuat layar daftar tidak membawa serta seluruh kolom SI.</p>
	 *
	 * <p><b>Hati-hati saat menghapus:</b> constraint basis datanya
	 * {@code ON DELETE CASCADE} ke arah {@code akunting.standing_instruction} &mdash; menghapus
	 * baris SI akan ikut menghapus dokumen gaji ini beserta seluruh turunannya.</p>
	 *
	 * @return perintah transfer terkait, atau {@code null} bila belum/tidak diterbitkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "standing_instruction", nullable = true)
	public StandingInstruction getStandingInstruction() {
		return standingInstruction;
	}

	/**
	 * Menyetel perintah transfer untuk dokumen ini.
	 *
	 * <p>Dipanggil dari {@code PembayaranGajiAction}/{@code BayarGajiPegawaiAction} setelah
	 * {@code StandingInstruction.simpanPembayaranGajiPunyaPegawai(...)} menerbitkan barisnya.
	 * Menyetel {@code null} secara efektif membuat dokumen tidak layak diposting.</p>
	 *
	 * @param standingInstruction perintah transfer terkait
	 */
	public void setStandingInstruction(StandingInstruction standingInstruction) {
		this.standingInstruction = standingInstruction;
	}

	/**
	 * Mengembalikan total nominal batch gaji ini.
	 *
	 * <p><b>Angka ini adalah hasil agregasi, bukan masukan manusia:</b> diisi
	 * {@link #hitungUlang(PembayaranGaji)} dari jumlah {@code nilai} seluruh
	 * {@code PembayaranGajiPunyaPegawai} milik dokumen ini. Nilai yang sama dibaca
	 * {@code StandingInstruction.getNominal()} sebagai nominal perintah transfer, sehingga
	 * perubahan pada baris per pegawai <b>merambat ke surat perintah transfer</b> selama SI-nya
	 * belum dicetak.</p>
	 *
	 * <p><b>Perhatikan asimetri {@code null}:</b> getter mengembalikan {@code 0.0} untuk field
	 * {@code null}, tetapi kolomnya di basis data tetap {@code NULL}. Query agregat SQL yang
	 * menjumlahkan kolom ini langsung (di luar Hibernate) karena itu bisa memberi hasil berbeda
	 * dari penjumlahan lewat getter.</p>
	 *
	 * @return total nominal batch, {@code 0.0} bila belum pernah dihitung
	 */
	public Double getNilai() {
		return nilai == null ? 0.0 : nilai;
	}

	/**
	 * Menyetel total nominal batch gaji.
	 *
	 * <p>Umumnya jangan dipanggil langsung &mdash; pakai {@link #hitungUlang(PembayaranGaji)}
	 * agar angkanya tetap konsisten dengan baris per pegawai.</p>
	 *
	 * @param nilai total nominal batch
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Menghitung ulang total {@link #getNilai()} dari seluruh baris per pegawai, lalu
	 * <b>menyimpannya</b> ke basis data.
	 *
	 * <p><b>Tujuan.</b> Menjaga agar total pada kepala dokumen selalu sama dengan jumlah
	 * {@code nilai} seluruh {@code PembayaranGajiPunyaPegawai} yang menunjuk dokumen ini.
	 * Dipanggil setiap kali baris pegawai ditambah, diubah, atau dihapus dari layar penyusunan
	 * gaji ({@code PembayaranGajiAction}, {@code BayarGajiPegawaiAction},
	 * {@code PembayaranGajiPunyaPegawaiAction}, dan helper terkait).</p>
	 *
	 * <p><b>Langkahnya, berurutan:</b></p>
	 * <ol>
	 * <li>membuka sesi Hibernate native lewat {@code HibernateUtil.currentNativeSession()};</li>
	 * <li>{@code session.refresh(pembayaranGaji)} &mdash; memuat ulang dokumen dari basis data
	 * sehingga perubahan yang belum ter-flush pada objek di memori <b>dibuang</b>;</li>
	 * <li>menjumlahkan proyeksi {@code sum("nilai")} atas {@code PembayaranGajiPunyaPegawai}
	 * dengan {@code eq("pembayaranGaji", pembayaranGaji)};</li>
	 * <li>menuliskan hasilnya ({@code 0.0} bila tidak ada baris sama sekali) lewat
	 * {@link #setNilai(Double)};</li>
	 * <li>membuka transaksi, menyimpan lewat {@code Common.refreshSaveOrUpdate(...)}, dan
	 * commit.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping yang harus disadari pemanggil:</b></p>
	 * <ul>
	 * <li><b>Menyimpan ke basis data.</b> Ini bukan sekadar penghitung &mdash; ia commit. Jangan
	 * panggil di tengah transaksi lain yang belum siap dipersist.</li>
	 * <li><b>Menutup sesi.</b> Blok penutup melakukan {@code disconnect()} + {@code close()} +
	 * {@code HibernateUtil.closeSession()} <b>tanpa syarat</b>, termasuk terhadap sesi yang
	 * <i>bukan</i> dibuka oleh method ini. Pemanggil yang masih memegang objek lazy dari sesi
	 * yang sama akan menemuinya sudah terputus sesudahnya.</li>
	 * <li><b>Menelan kegagalan.</b> Seluruh langkah 2&ndash;5 dibungkus {@code try/catch} yang
	 * hanya mencatat lewat {@code ErrorAuditUtil.record(...)}. Bila penyimpanan gagal, method
	 * tetap kembali normal dan total dokumen diam-diam tertinggal basi &mdash; pemanggil tidak
	 * punya cara mengetahuinya (nilai baliknya {@code void}).</li>
	 * <li><b>Tanpa gerbang otorisasi maupun cakupan.</b> Method statis ini menerima dokumen
	 * <i>mana pun</i> dan menulis ulang nominalnya; tidak ada pemeriksaan peran, satuan kerja,
	 * status persetujuan, maupun status posting. Dokumen yang <b>sudah diposting</b> pun akan
	 * berubah totalnya tanpa jurnalnya ikut disesuaikan.</li>
	 * </ul>
	 *
	 * @param pembayaranGaji dokumen batch yang totalnya akan dihitung ulang dan disimpan;
	 *                       harus sudah punya id (sudah tersimpan), karena {@code refresh(...)}
	 *                       akan gagal untuk objek transien
	 */
	public static void hitungUlang(PembayaranGaji pembayaranGaji) {
		Session session = HibernateUtil.currentNativeSession();

		try {
			session.refresh(pembayaranGaji);
			Number total = (Number) session.createCriteria(PembayaranGajiPunyaPegawai.class)
					.setProjection(Projections.sum("nilai")).add(Restrictions.eq("pembayaranGaji", pembayaranGaji))
					.uniqueResult();

			pembayaranGaji.setNilai(total == null ? 0.0 : total.doubleValue());
			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, pembayaranGaji);
			session.getTransaction().commit();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/payroll/PembayaranGaji.java:224");
			// TODO: handle exception
		}

		// session.disconnect();
		if (session.isOpen()) {
			session.disconnect();
			session.close();
		}
		HibernateUtil.closeSession();
	}

	/**
	 * Mengembalikan tanggal transaksi dokumen &mdash; <b>selalu sama dengan
	 * {@link #getWaktuBayar()}</b>.
	 *
	 * <p><b>Getter destruktif tanpa syarat.</b> Baris pertamanya menugaskan
	 * {@code tanggalTransaksi = getWaktuBayar()} <i>setiap kali dipanggil</i>, tanpa memeriksa
	 * apakah field sudah terisi. Karena kelas ini dipetakan dengan <i>property access</i> dan
	 * {@code dynamicUpdate} aktif, sekadar <b>membaca</b> dokumen di dalam sesi yang kemudian
	 * di-flush sudah cukup menimpa kolom {@code tanggal_transaksi} di basis data.</p>
	 *
	 * <p>Akibat praktisnya: {@link #setTanggalTransaksi(Date)} <b>tidak pernah bertahan</b> &mdash;
	 * kolom ini secara efektif hanyalah salinan {@code waktubayar}. Bila suatu saat tanggal
	 * transaksi perlu berbeda dari tanggal bayar (mis. jurnal ditanggalkan di akhir bulan
	 * sementara transfernya awal bulan berikutnya), penugasan tanpa syarat inilah yang harus
	 * dibongkar lebih dulu.</p>
	 *
	 * <p>{@code try/catch}-nya melindungi dari {@code LazyInitializationException} yang
	 * secara teori bisa muncul dari {@link #getWaktuBayar()}; bila terjadi, nilai lama
	 * dipertahankan dan kegagalannya hanya dicatat.</p>
	 *
	 * @return tanggal transaksi, praktis selalu bernilai sama dengan tanggal bayar
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_transaksi")
	public Date getTanggalTransaksi() {
		try {
			tanggalTransaksi = getWaktuBayar();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/payroll/PembayaranGaji.java:241");
			// TODO: handle exception
		}
		return tanggalTransaksi;
	}

	/**
	 * Menyetel tanggal transaksi dokumen.
	 *
	 * <p><b>Praktis tidak berguna:</b> {@link #getTanggalTransaksi()} menimpa nilainya dengan
	 * {@link #getWaktuBayar()} pada pembacaan berikutnya. Disediakan demi kelengkapan kontrak
	 * JavaBean dan agar Hibernate bisa memuat kolomnya.</p>
	 *
	 * @param tanggalTransaksi tanggal transaksi yang akan segera tertimpa
	 */
	public void setTanggalTransaksi(Date tanggalTransaksi) {
		this.tanggalTransaksi = tanggalTransaksi;
	}

	/**
	 * Menyetel pengguna pembuat dokumen.
	 *
	 * <p>Nilainya akan tertimpa {@link #getDibuatOleh()} bila dokumen memiliki
	 * {@link DisposisiSop} dengan langkah awal yang lengkap.</p>
	 *
	 * @param dibuatOleh pengguna pembuat dokumen
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * Mengembalikan pengguna yang mengajukan/membuat dokumen ini.
	 *
	 * <p><b>Sumber kebenarannya adalah alur SOP, bukan kolomnya sendiri.</b> Bila dokumen punya
	 * {@link DisposisiSop} yang langkah awalnya ({@code getDisposisiStart()}) memiliki pengaju,
	 * nilai kolom <b>ditimpa</b> dengan pengaju tersebut. Karena pemetaan <i>property access</i>,
	 * penimpaan itu ikut tersimpan saat sesi di-flush &mdash; getter ini destruktif.</p>
	 *
	 * <p><b>Bila {@code disposisiSop} bernilai {@code null}</b> (dokumen yang lahir di luar layar
	 * SOP), tidak ada penimpaan sama sekali dan nilai kolom mentah diteruskan apa adanya.</p>
	 *
	 * <p>Seluruh penurunan dibungkus {@code try/catch} yang sengaja menelan
	 * {@code LazyInitializationException}: {@code disposisiSop} bisa berupa instance kanonik yang
	 * proxy-nya terikat ke sesi lain yang sudah tertutup, dan getter ini tidak boleh membuat
	 * layar gagal dirender hanya karena itu. Bila terjadi, nilai kolom mentah dipertahankan
	 * sebagai fallback.</p>
	 *
	 * @return pengguna pembuat dokumen, atau {@code null} bila tidak diketahui
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = true)
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);

		try {
			// FIX LazyInitializationException: disposisiSop bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
					&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
				dibuatOleh = getDisposisiSop().getDisposisiStart().getDiajukanOleh();
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/payroll/PembayaranGaji.java:getDibuatOleh-lazy");
		}

		return dibuatOleh;
	}

	/**
	 * Menyetel pengguna penyetuju dokumen.
	 *
	 * <p><b>Kolom yang disetel di sini adalah salah satu dari dua syarat kelayakan posting
	 * jurnal</b> (yang lain {@link #getStandingInstruction()}). Baca peringatan lengkapnya di
	 * {@link #getDisetujuiOleh()} sebelum memanggil setter ini dari kode baru.</p>
	 *
	 * @param disetujuiOleh pengguna penyetuju
	 */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * Mengembalikan pengguna yang menyetujui dokumen ini &mdash; <b>gerbang yang menentukan
	 * apakah gaji boleh dijurnal</b>.
	 *
	 * <p><b>Aturan penurunannya, berurutan:</b></p>
	 * <ol>
	 * <li>nilai kolom diresolusi lewat {@code check(...)};</li>
	 * <li><b>bila</b> {@link #getDisposisiSop()} tidak {@code null} dan langkah persetujuannya
	 * ({@code getDisposisiSetuju()}) punya pengaju, kolom <b>ditimpa</b> dengan pengaju
	 * tersebut;</li>
	 * <li><b>bila</b> {@link #getDisposisiSop()} tidak {@code null} tetapi langkah
	 * persetujuannya belum ada / belum berpengaju, kolom <b>dikosongkan menjadi {@code null}</b>;</li>
	 * <li>terakhir, di luar {@code try/catch}: bila {@link #getDitolakOleh()} terisi, persetujuan
	 * <b>dicabut</b> menjadi {@code null}.</li>
	 * </ol>
	 *
	 * <p><b>PERINGATAN 1 &mdash; getter ini bisa MENCABUT persetujuan sah hanya dengan dibaca.</b>
	 * Langkah 3 dan 4 menulis {@code null} ke field persisten. Dengan pemetaan <i>property
	 * access</i> + {@code dynamicUpdate}, membaca dokumen di dalam sesi yang kemudian di-flush
	 * cukup untuk mengosongkan kolom {@code disetujui_oleh} di basis data secara permanen.
	 * Dokumen gaji yang sudah disetujui lalu langkah SOP-nya berubah/hilang akan kehilangan
	 * status persetujuannya tanpa jejak niat manusia, dan mesin posting langsung berhenti
	 * mengenalinya sebagai layak dijurnal.</p>
	 *
	 * <p><b>PERINGATAN 2 &mdash; tanpa disposisi, kolom ini murni data mentah.</b> Ketika
	 * {@code disposisiSop} bernilai {@code null}, <b>tidak satu pun</b> dari langkah 2&ndash;3
	 * berjalan; apa pun yang tersimpan di kolom diteruskan apa adanya. Karena
	 * {@link #setDisposisiSop(DisposisiSop)} menolak argumen {@code null}/tanpa id, dokumen yang
	 * dibuat di luar layar SOP tidak akan pernah memperoleh disposisi. Padahal kelayakan posting
	 * hanya memeriksa {@code disetujuiOleh IS NOT NULL} &mdash; tidak ada pemeriksaan bahwa
	 * persetujuannya berasal dari langkah SOP yang sah, dan tidak ada pemisahan tugas antara
	 * pembuat dan penyetuju.</p>
	 *
	 * @return pengguna penyetuju, atau {@code null} bila belum disetujui atau sudah ditolak
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujuiOleh = check(disetujuiOleh);

		try {
			// FIX LazyInitializationException: disposisiSop bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
					&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
				disetujuiOleh = getDisposisiSop().getDisposisiSetuju().getDiajukanOleh();
			}

			if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
					|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
				disetujuiOleh = null;
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/payroll/PembayaranGaji.java:getDisetujuiOleh-lazy");
		}

		if (getDitolakOleh() != null) {
			disetujuiOleh = null;
		}

		return disetujuiOleh;
	}

	/**
	 * Menyetel tanggal persetujuan dokumen.
	 *
	 * <p>Akan tertimpa {@link #getTanggalPersetujuan()} bila dokumen memiliki
	 * {@link DisposisiSop}.</p>
	 *
	 * @param tanggalPersetujuan waktu persetujuan
	 */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * Mengembalikan waktu persetujuan dokumen, diturunkan dari langkah persetujuan
	 * {@link DisposisiSop}.
	 *
	 * <p>Aturannya <b>berpasangan persis</b> dengan {@link #getDisetujuiOleh()}: diisi dari
	 * {@code getDisposisiSetuju().getWaktu()} bila langkah persetujuannya lengkap, dikosongkan
	 * bila disposisi ada tetapi langkah persetujuannya belum, dan dikosongkan lagi bila dokumen
	 * sudah ditolak. Ketika {@code disposisiSop} bernilai {@code null}, nilai kolom mentah
	 * diteruskan apa adanya.</p>
	 *
	 * <p><b>Getter destruktif</b> dengan risiko yang sama: pengosongan pada langkah 2 dan 3 ikut
	 * tersimpan saat sesi di-flush.</p>
	 *
	 * @return waktu persetujuan, atau {@code null} bila belum disetujui atau sudah ditolak
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {

		try {
			// FIX LazyInitializationException: disposisiSop bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
					&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
				tanggalPersetujuan = getDisposisiSop().getDisposisiSetuju().getWaktu();
			}

			if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
					|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
				tanggalPersetujuan = null;
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/payroll/PembayaranGaji.java:getTanggalPersetujuan-lazy");
		}

		if (getDitolakOleh() != null) {
			tanggalPersetujuan = null;
		}

		return tanggalPersetujuan;
	}

	/**
	 * Menyetel tanggal pembuatan dokumen.
	 *
	 * @param tanggalPembuatan waktu pembuatan
	 */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * Mengembalikan waktu pembuatan/pengajuan dokumen.
	 *
	 * <p>Bila dokumen punya {@link DisposisiSop} dengan langkah awal berpengaju, kolom
	 * <b>ditimpa</b> dengan waktu langkah awal tersebut &mdash; getter destruktif, sama seperti
	 * {@link #getDibuatOleh()}.</p>
	 *
	 * <p><b>Berbeda dari pasangan persetujuan:</b> di sini <b>tidak ada</b> cabang pengosongan.
	 * Dokumen tanpa langkah awal tidak kehilangan tanggal pembuatannya. Nilai baliknya pun punya
	 * jaring pengaman: bila {@code tanggalPembuatan} masih {@code null}, yang dikembalikan adalah
	 * {@link #getTanggalTransaksi()} &mdash; yang pada gilirannya sama dengan
	 * {@link #getWaktuBayar()}. Jadi method ini tidak pernah mengembalikan {@code null}, tetapi
	 * <b>fallback-nya tidak ikut tersimpan</b>: yang dikembalikan bukan nilai field.</p>
	 *
	 * @return waktu pembuatan dokumen, jatuh ke tanggal transaksi bila kolomnya kosong
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		try {
			// FIX LazyInitializationException: disposisiSop bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
					&& getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
				tanggalPembuatan = getDisposisiSop().getDisposisiStart().getWaktu();
			}
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/payroll/PembayaranGaji.java:getTanggalPembuatan-lazy");
		}

		return tanggalPembuatan == null ? getTanggalTransaksi() : tanggalPembuatan;
	}

	/**
	 * Mengembalikan satuan kerja (unit organisasi/tenant) pemilik dokumen ini.
	 *
	 * <p><b>Getter write-back dengan dampak lintas tenant.</b> Bila
	 * {@link #getCaraPembayaranGaji()} terisi <b>dan</b> master itu punya satuan kerja, kolom
	 * {@code satuan_kerja} dokumen <b>ditimpa</b> dengan satuan kerja master tersebut. Karena
	 * pemetaan <i>property access</i> + {@code dynamicUpdate}, penimpaan itu ikut tersimpan saat
	 * sesi di-flush. Konsekuensinya:</p>
	 * <ul>
	 * <li>satuan kerja dokumen <b>tidak dapat berbeda</b> dari satuan kerja cara bayarnya;
	 * mengubah master cara bayar akan memindahkan seluruh dokumen gaji yang memakainya ke unit
	 * lain, secara diam-diam, saat dokumen itu dibaca;</li>
	 * <li>penyaring cakupan Generic CRUD v2 (
	 * {@code GenericCrudAutoEntityAdapter.validateObjectScope()}) membaca properti ini lewat
	 * {@code ClassMetadata.getPropertyValue(...)} &mdash; artinya lewat getter ini. Cakupan yang
	 * divalidasi karena itu adalah cakupan yang <b>baru saja ditulis ulang</b> oleh pilihan cara
	 * bayar pada request yang sedang diperiksa, bukan cakupan yang tersimpan sebelumnya.</li>
	 * </ul>
	 *
	 * <p><b>Berbeda dari {@code StandingInstruction.getSatuanKerja()}</b> (yang sudah diperbaiki
	 * agar hanya menimpa dengan nilai bukan-{@code null}), di sini penimpaan hanya terjadi saat
	 * sumbernya memang ada &mdash; jadi cakupan yang sudah tersimpan tidak bisa terhapus menjadi
	 * {@code null} lewat jalur ini. Yang bisa terjadi adalah <b>berpindah</b>, bukan hilang.</p>
	 *
	 * <p>Kedua mesin posting ({@code kriteriaPostingStatic()} dan {@code initCriteria()})
	 * <b>tidak</b> menyaring berdasarkan properti ini sama sekali; nilainya hanya dipakai sebagai
	 * dimensi jurnal dan label laporan.</p>
	 *
	 * @return satuan kerja pemilik dokumen, atau {@code null} bila tidak ditetapkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);

		if (getCaraPembayaranGaji() != null && getCaraPembayaranGaji().getSatuanKerja() != null) {
			satuanKerja = getCaraPembayaranGaji().getSatuanKerja();
		}

		return satuanKerja;
	}

	/**
	 * Menyetel satuan kerja pemilik dokumen.
	 *
	 * <p>Nilai yang disetel akan tertimpa {@link #getSatuanKerja()} pada pembacaan berikutnya bila
	 * cara pembayaran dokumen punya satuan kerja sendiri.</p>
	 *
	 * @param satuanKerja unit organisasi pemilik dokumen
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	private String kodeUnik;
	private Boolean tanpaAnggaran;
	private Date tanggalDitolak;
	private Tbmuser ditolakOleh;
	private Boolean aktif;

	/**
	 * Mengembalikan kunci unik gabungan dokumen, dirakit dari kode dokumen dan id disposisi
	 * (atau id dokumen bila belum berdisposisi).
	 *
	 * <p><b>Getter write-back tanpa syarat:</b> nilainya dihitung ulang dan ditulis ke field
	 * <i>setiap kali dipanggil</i>, jadi {@link #setKodeUnik(String)} tidak pernah bertahan.
	 * Formatnya {@code <kode>_<idDisposisi>} bila dokumen sudah punya {@link DisposisiSop}, dan
	 * {@code <kode>_<idDokumen>} bila belum.</p>
	 *
	 * <p><b>Dua kuirk penting:</b></p>
	 * <ol>
	 * <li>{@code getKode()} berasal dari {@code GeneralValueObject} dan <b>tidak dipetakan</b>
	 * pada entity ini (lihat catatan kelas), sehingga sesudah dimuat ulang dari basis data
	 * nilainya {@code null} &mdash; kunci unik yang tersimpan berbentuk {@code "null_123"}. Isi
	 * kolom ini karena itu tidak dapat dipakai untuk menelusuri nomor surat dokumen.</li>
	 * <li>Kolomnya {@code @Column(unique = true)}, tetapi nilainya <b>berubah di tengah hidup
	 * dokumen</b>: begitu disposisi terpasang, sufiksnya berpindah dari id dokumen ke id
	 * disposisi. Perubahan itu tersimpan saat sesi di-flush, dan secara teori dapat berbenturan
	 * dengan baris lain yang kebetulan menghasilkan rakitan yang sama &mdash; kegagalannya akan
	 * muncul sebagai pelanggaran constraint pada saat commit, jauh dari tempat pembacaannya.</li>
	 * </ol>
	 *
	 * @return kunci unik gabungan dokumen; tidak pernah {@code null} (bagian kode bisa berupa
	 *         teks {@code "null"})
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		kodeUnik = getKode() + "" + (getDisposisiSop() == null ? "_" + getId() : "_" + getDisposisiSop().getId());
		return kodeUnik;
	}

	/**
	 * Menyetel kunci unik gabungan dokumen.
	 *
	 * <p>Praktis tidak berguna: {@link #getKodeUnik()} merakit ulang nilainya pada pembacaan
	 * berikutnya. Disediakan agar Hibernate dapat memuat kolomnya.</p>
	 *
	 * @param kodeUnik kunci unik yang akan segera tertimpa
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * Mengembalikan disposisi SOP yang menaungi dokumen ini &mdash; <b>satu-satunya sumber
	 * kebenaran alur persetujuan</b>.
	 *
	 * <p>Implementasi kontrak abstrak {@link DataSop#getDisposisiSop()}. Dari sinilah
	 * {@link #getDibuatOleh()}, {@link #getTanggalPembuatan()}, {@link #getDisetujuiOleh()},
	 * {@link #getTanggalPersetujuan()}, dan sebagian {@link #getAktif()} menurunkan nilainya.
	 * Memanggil {@code check(...)} untuk meresolusi proxy lazy.</p>
	 *
	 * @return disposisi SOP dokumen, atau {@code null} bila dokumen tidak melewati alur SOP
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menautkan dokumen ke satu disposisi SOP.
	 *
	 * <p><b>Penjaga di baris pertama membuat tautan ini praktis sekali pasang:</b> argumen
	 * {@code null} atau disposisi yang belum punya id ditolak diam-diam (method langsung
	 * {@code return}). Artinya disposisi yang sudah terpasang <b>tidak dapat dilepas</b> lewat
	 * setter ini, dan dokumen yang lahir di luar alur SOP tidak akan pernah memperoleh disposisi
	 * dari jalur ini.</p>
	 *
	 * <p>Ekspresi ternary di bawah penjaga tersebut <b>tidak pernah memilih cabang kiri</b>:
	 * kondisinya menuntut {@code disposisiSop == null || disposisiSop.getId() == null}, persis
	 * keadaan yang sudah disaring habis oleh penjaga di atasnya. Efektifnya method ini setara
	 * dengan penugasan biasa. Bentuknya dipertahankan apa adanya &mdash; dokumentasi ini tidak
	 * mengubah logika.</p>
	 *
	 * @param disposisiSop disposisi SOP yang sudah tersimpan (punya id); {@code null} atau
	 *                     disposisi transien diabaikan
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
	 * Mengembalikan template penomoran surat untuk dokumen ini, dengan bawaan global
	 * {@link NomorSuratAlurPengadaan#PEMBAYARAN_GAJI_PEGAWAI}.
	 *
	 * <p><b>Getter write-back:</b> bila kolomnya kosong, konstanta statis global itu ditugaskan
	 * ke field &mdash; dan karena pemetaan <i>property access</i>, tautan itu ikut tersimpan saat
	 * sesi di-flush. Dokumen yang semula tidak menunjuk template mana pun jadi permanen menunjuk
	 * template bawaan hanya karena dibaca.</p>
	 *
	 * <p><b>Kasus tepi:</b> konstanta global tersebut disemai
	 * {@code NomorSuratAlurPengadaan.reloadDefault()} saat startup. Bila penyemaian itu belum
	 * berjalan, nilai baliknya {@code null} &mdash; dan {@code PembayaranGajiAction.generateCode()}
	 * memang menyiapkan jalur cadangan ({@code Common.getGeneratedBarCode()}) untuk keadaan itu.</p>
	 *
	 * <p>Bila kolomnya sudah terisi, hanya {@code check(...)} yang dijalankan untuk meresolusi
	 * proxy lazy.</p>
	 *
	 * @return template penomoran surat dokumen, bisa {@code null} bila konstanta global belum
	 *         disemai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat_alur_pengadaan", nullable = true)
	public NomorSuratAlurPengadaan getNomorSuratAlurPengadaan() {
		if (nomorSuratAlurPengadaan == null) {
			nomorSuratAlurPengadaan = NomorSuratAlurPengadaan.PEMBAYARAN_GAJI_PEGAWAI;
		} else {
			nomorSuratAlurPengadaan = check(nomorSuratAlurPengadaan);
		}
		return nomorSuratAlurPengadaan;
	}

	/**
	 * Menyetel template penomoran surat dokumen.
	 *
	 * @param nomorSuratAlurPengadaan template penomoran; {@code null} akan diganti bawaan global
	 *                                pada pembacaan berikutnya
	 */
	public void setNomorSuratAlurPengadaan(NomorSuratAlurPengadaan nomorSuratAlurPengadaan) {
		this.nomorSuratAlurPengadaan = nomorSuratAlurPengadaan;
	}

	/**
	 * Mengembalikan ruang kerja/kegiatan anggaran ({@code rab.Workspace}) yang membebani dokumen
	 * ini.
	 *
	 * <p>Dibaca {@code rab.PenggunaanAnggaran} untuk menentukan apakah pembayaran gaji ini
	 * menyerap anggaran suatu kegiatan, dan dengan label apa. Bila {@code null}, dokumen tidak
	 * terhubung ke pos anggaran mana pun. Bandingkan dengan {@link #getTanpaAnggaran()} yang
	 * membebaskan dokumen dari pembebanan secara eksplisit.</p>
	 *
	 * @return ruang kerja anggaran, atau {@code null} bila tidak dibebankan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace", nullable = true)
	public Workspace getWorkspace() {
		workspace = check(workspace);
		return workspace;
	}

	/**
	 * Menyetel ruang kerja/kegiatan anggaran dokumen.
	 *
	 * @param workspace ruang kerja anggaran
	 */
	public void setWorkspace(Workspace workspace) {
		this.workspace = workspace;
	}

	/**
	 * Menyatakan apakah dokumen ini dikecualikan dari pembebanan anggaran.
	 *
	 * <p>Bernilai {@code true} berarti pembayaran gaji ini <b>tidak</b> menyerap pagu anggaran
	 * mana pun, sehingga tidak perlu {@link #getWorkspace()} maupun pemeriksaan sisa pagu.
	 * Bawaannya {@code false} ({@code null} diperlakukan sebagai {@code false}), yaitu tetap
	 * dibebankan.</p>
	 *
	 * @return {@code true} bila dokumen dikecualikan dari anggaran; {@code false} bila dibebankan
	 */
	public Boolean getTanpaAnggaran() {
		return tanpaAnggaran == null ? false : tanpaAnggaran;
	}

	/**
	 * Menyetel bendera pengecualian anggaran.
	 *
	 * @param tanpaAnggaran {@code true} untuk mengecualikan dokumen dari pembebanan anggaran
	 */
	public void setTanpaAnggaran(Boolean tanpaAnggaran) {
		this.tanpaAnggaran = tanpaAnggaran;
	}

	/**
	 * Mengembalikan pengguna yang menolak dokumen ini.
	 *
	 * <p>Berbeda dari pasangan persetujuan, kolom ini <b>tidak</b> diturunkan dari
	 * {@link DisposisiSop} &mdash; ia data mentah yang hanya diresolusi proxy-nya lewat
	 * {@code check(...)}. Meski begitu pengaruhnya besar: begitu terisi,
	 * {@link #getDisetujuiOleh()} dan {@link #getTanggalPersetujuan()} mengembalikan (dan
	 * menuliskan) {@code null}, sehingga dokumen langsung berhenti layak diposting.</p>
	 *
	 * @return pengguna penolak, atau {@code null} bila dokumen tidak ditolak
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ditolak_oleh", nullable = true)
	public Tbmuser getDitolakOleh() {
		ditolakOleh = check(ditolakOleh);
		return ditolakOleh;
	}

	/**
	 * Menyetel pengguna yang menolak dokumen.
	 *
	 * <p>Menyetel nilai bukan-{@code null} di sini <b>mencabut persetujuan</b> dokumen secara
	 * efektif (lihat {@link #getDisetujuiOleh()}). Menyetelnya kembali ke {@code null} tidak
	 * memulihkan persetujuan yang sudah terlanjur ditulis {@code null} ke basis data.</p>
	 *
	 * @param ditolakOleh pengguna penolak
	 */
	public void setDitolakOleh(Tbmuser ditolakOleh) {
		this.ditolakOleh = ditolakOleh;
	}

	/**
	 * Mengembalikan waktu penolakan dokumen.
	 *
	 * <p>Getter murni tanpa efek samping &mdash; satu-satunya kolom tanggal alur pada kelas ini
	 * yang tidak diturunkan dari {@link DisposisiSop}.</p>
	 *
	 * @return waktu penolakan, atau {@code null} bila dokumen tidak ditolak
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_ditolak")
	public Date getTanggalDitolak() {
		return tanggalDitolak;
	}

	/**
	 * Menyetel waktu penolakan dokumen.
	 *
	 * @param tanggalDitolak waktu penolakan
	 */
	public void setTanggalDitolak(Date tanggalDitolak) {
		this.tanggalDitolak = tanggalDitolak;
	}

	/**
	 * Menyatakan apakah dokumen masih aktif (belum dibatalkan/ditolak oleh alur SOP).
	 *
	 * <p><b>Getter write-back satu arah.</b> Nilai {@code aktif} dipaksa menjadi {@code false}
	 * dalam dua keadaan:</p>
	 * <ol>
	 * <li>{@link DisposisiSop} dokumen sendiri sudah tidak aktif ({@code !disposisiSop.getAktif()});</li>
	 * <li>langkah akhir disposisi ({@code getDisposisiEnd()}) menunjuk alur SOP yang ditandai
	 * sebagai titik penolakan ({@code getAlurSop().getPenolakanAdaDiSini()}).</li>
	 * </ol>
	 *
	 * <p>Tidak ada cabang yang mengembalikan nilai ke {@code true}: sekali ditulis {@code false}
	 * dan tersimpan, dokumen tidak dapat "hidup lagi" lewat getter ini sekalipun disposisinya
	 * kemudian diaktifkan kembali. Bawaannya {@code true} bila field masih {@code null}, jadi
	 * dokumen baru selalu dianggap aktif.</p>
	 *
	 * <p>Perhatikan bahwa baris pertama menugaskan hasil {@link #getDisposisiSop()} ke field
	 * {@code disposisiSop} secara langsung (bukan lewat setter), sehingga penjaga anti-{@code null}
	 * pada {@link #setDisposisiSop(DisposisiSop)} tidak berlaku di sini.</p>
	 *
	 * <p><b>Catatan cakupan:</b> bendera ini <b>tidak</b> dipakai sebagai penyaring oleh kedua
	 * mesin posting; dokumen yang sudah tidak aktif tetap dapat dijurnal selama
	 * {@code standingInstruction} dan {@code disetujuiOleh} terisi.</p>
	 *
	 * @return {@code true} bila dokumen masih aktif; {@code false} bila dibatalkan/ditolak alur SOP
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
	 * Menyetel status aktif dokumen.
	 *
	 * @param aktif {@code true} bila dokumen aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan riwayat posting jurnal dokumen &mdash; <b>satu-satunya penanda "gaji ini
	 * sudah dibukukan"</b>.
	 *
	 * <p>Terisi {@link PostingHistory} berjenis {@code JENIS_PENGGAJIAN} setelah salah satu mesin
	 * posting berhasil menulis {@code GrupTransaksi} + baris {@code Transaksi} lewat
	 * {@code CommonAkunting.saveTransaksi(...)}. Bernilai {@code null} berarti "belum diposting",
	 * dan itulah tepat yang dipakai kedua layar posting untuk memutuskan tombol mana yang
	 * ditampilkan serta dokumen mana yang masuk daftar draft.</p>
	 *
	 * <p>Memakai {@code FetchMode.SELECT} sehingga dimuat lewat query terpisah.</p>
	 *
	 * <p><b>Integritas &mdash; dokumen dapat terjurnal berlipat.</b> Pembatalan posting (baik
	 * lewat tombol per baris, tombol "batalkan semua", maupun jalur REST) selalu menghapus jurnal
	 * dengan {@code delete from akunting.grup_transaksi where pembayaran_gaji=<id> and closing is
	 * null}, tetapi mengosongkan {@code postingHistory} <b>tanpa syarat</b>. Baris jurnal yang
	 * periodenya sudah ditutup ({@code closing} terisi) selamat dari penghapusan, sementara
	 * dokumennya kembali berstatus "belum diposting" dan layak dijurnal ulang &mdash;
	 * menghasilkan set jurnal kedua yang utuh untuk gaji yang sama. Penghapusan itu juga memakai
	 * SQL mentah sehingga <b>tidak menghasilkan revisi Envers</b>: riwayat audit hanya
	 * memperlihatkan kolom ini berubah jadi {@code null}, bukan jurnal apa yang lenyap.</p>
	 *
	 * @return riwayat posting jurnal, atau {@code null} bila dokumen belum dijurnal
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * Menyetel riwayat posting jurnal dokumen.
	 *
	 * <p>Dipanggil mesin posting setelah jurnal tersimpan, dan dipanggil dengan {@code null} oleh
	 * seluruh jalur pembatalan posting. Menyetel {@code null} <b>tidak</b> menghapus jurnal yang
	 * sudah terbit &mdash; penghapusan jurnal dilakukan terpisah lewat SQL mentah oleh
	 * pemanggilnya (lihat {@link #getPostingHistory()}).</p>
	 *
	 * @param postingHistory riwayat posting, atau {@code null} untuk menandai dokumen belum
	 *                       diposting
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

}
