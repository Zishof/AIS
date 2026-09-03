package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.json.JSONObject;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Row;

import ais.action.master.RencanaTahunAkademikAction;
import ais.action.master.helper.KegiatanPersistenceHelper;
import ais.action.master.helper.PembayaranUtilHelper;
import ais.action.master.helper.virtualaccount.DownloadTagihanSiswaBankOnline;
import ais.action.master.sekolah.util.PembayaranSiswaUtil;
import ais.action.report.CommonReportHelper;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.CaraPembayaranKoperasi;
import ais.database.model.kursus.PesertaPunyaProdukKursus;
import ais.database.model.sekolah.AkunPembayaranSiswa;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.KanalPembayaran;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.NominalBiaya;
import ais.database.model.sekolah.PembayaranSiswa;
import ais.database.model.sekolah.PembayaranSiswaDetail;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Tagihan;
import ais.ui.util.MyDoublebox;
import ais.ui.util.WaktuUtil;

/**
 * Entity <b>tagihan Virtual Account (VA)</b>: satu baris tabel {@code public.virtual_account_bank}
 * mewakili satu permintaan pembayaran yang diterbitkan AIS untuk dibayar lewat bank atau payment
 * gateway, lengkap dengan seluruh jejak integrasinya (request, response, notifikasi callback) dan
 * penanda hasil postingnya.
 *
 * <p>Kelas ini bukan sekadar wadah kolom. Selain state entity, di sini tinggal <b>mesin pembayaran
 * VA</b> berupa method {@code static} yang dipanggil langsung oleh servlet callback tiap bank —
 * lihat {@code ais.action.servlet.Briva}, {@code ais.action.servlet.BSI},
 * {@code ais.action.servlet.Bankaltimtara}, {@code ais.action.servlet.Bjb},
 * {@code ais.action.servlet.BCA}, {@code ais.action.servlet.Mandiri},
 * {@code ais.action.servlet.Nagari}, {@code ais.action.servlet.OcbcNisp},
 * {@code ais.action.servlet.MncBank}, {@code ais.action.servlet.Finpay},
 * {@code ais.action.servlet.Flip}, {@code ais.action.servlet.Esmartlink},
 * {@code ais.action.servlet.Otto}, {@code ais.action.servlet.Maja},
 * {@code ais.action.servlet.Jaring}, {@code ais.action.servlet.BMS},
 * {@code ais.action.servlet.Va} — serta oleh berkas penarikan tagihan di
 * {@code ais.action.master.helper.virtualaccount} (BNI/BRI/BSI/BTN/BJB/Bankaltimtara/NTT/Online).
 * Modul adaptor tiap bank ada di paket {@code ais.action.master.bni}, {@code ...bri},
 * {@code ...bsi}, {@code ...cimb}, {@code ...faspay}, {@code ...finpay}, {@code ...ipaymu},
 * {@code ...jatelindo}.</p>
 *
 * <h3>Alur hidup satu baris VA</h3>
 * <ol>
 *   <li><b>Penerbitan.</b> UI/service ({@code VirtualAccountBankAction}, helper daftar ulang,
 *       pembayaran siswa, koperasi, kursus) membuat baris baru: nominal ({@code total}), pemilik
 *       tagihan, dan token rincian biaya ({@code detailbiaya}/{@code cicilan}/{@code bulanan}).
 *       Nomor VA disimpan di {@code kode}; bila {@code otomatis} bernilai {@code true} nomor itu
 *       dibentuk sendiri oleh {@link #getKode()} dari {@code id + 1}.</li>
 *   <li><b>Penarikan oleh bank.</b> Bank/host memanggil AIS (inquiry) atau menarik berkas tagihan;
 *       baris VA dicari lewat {@link #ambilVa(String, Double, BankHost, Criterion)} atau
 *       {@link #ambilLink(String, BankHost)}.</li>
 *   <li><b>Pembayaran &amp; callback.</b> Servlet bank memanggil {@link #bayarVa(VirtualAccountBank,
 *       Date, String, Session)} (jalur mahasiswa/calon mahasiswa) atau
 *       {@link #bayarSiswa(VirtualAccountBank, Session, Date, String, boolean, String, boolean)}
 *       (jalur siswa/calon siswa sekolah). Keduanya membentuk dokumen pembayaran yang sebenarnya
 *       ({@link Kegiatan} + {@link CicilanPembayaran}, atau
 *       {@code PembayaranSiswa} + {@code PembayaranSiswaDetail}) dan/atau saldo
 *       {@link Deposit} lewat {@link #bayarTopup(VirtualAccountBank, Session, Date, String,
 *       boolean, String)}.</li>
 *   <li><b>Penandaan lunas.</b> Kolom {@code kegiatan}, {@code pembayaran}, {@code deposit}
 *       (masing-masing id dokumen hasil, bukan relasi Hibernate) plus {@code waktuBayar} diisi;
 *       id VA dimasukkan ke cache {@link #sukses} supaya callback ulang tidak dobel-posting.</li>
 * </ol>
 *
 * <h3>Siapa yang membayar</h3>
 * <p>Satu baris VA memakai <b>tepat satu</b> dari beberapa pemilik yang saling eksklusif secara
 * praktik (semuanya nullable): {@link Mahasiswa}, {@link BiodataCalonMahasiswa}, {@link Siswa},
 * {@link CalonSiswa}, {@link AnggotaKoperasi}, atau {@link PesertaPunyaProdukKursus}. Banyak method
 * bercabang persis atas dasar ini — mis. {@link #getPt()}, {@link #getNamaPemilikRingkas()},
 * {@link #getTahunAkademik()}.</p>
 *
 * <h3>Token rincian biaya (format teks, bukan relasi)</h3>
 * <p>Rincian apa saja yang dibayar TIDAK disimpan sebagai tabel anak, melainkan sebagai string
 * dipisah koma:</p>
 * <ul>
 *   <li>{@code detailbiaya} — daftar id {@link DetailBiaya}, mis. {@code "12,13,14"}.</li>
 *   <li>{@code cicilan} — token per item yang dibayar, dengan tiga bentuk berbeda yang diurai
 *       {@link #bayarVa(VirtualAccountBank, Date, String, Session)}:
 *       {@code "Bulanan-<idPengaturanPembayaranBulanan>-<nilai>"},
 *       {@code "Item-<idItemBiaya>-<nilai>-<bayarKe>-<idDetailBiaya>"},
 *       {@code "Keranjang-<idKegiatanTemporary>"}. Untuk jalur siswa, {@code cicilan} malah berisi
 *       {@code "<apa saja>-<idTagihan>[-<nilai>]"} (lihat
 *       {@link #bayarSiswa(VirtualAccountBank, Session, Date, String, boolean, String, boolean)}) —
 *       jadi <b>arti kolom yang sama berbeda antara jalur mahasiswa dan jalur siswa</b>.</li>
 * </ul>
 *
 * <h3>Pengelompokan method</h3>
 * <ul>
 *   <li><b>Penjaga idempotensi</b> — {@link #sukses},
 *       {@link #isSudahTerbayar(VirtualAccountBank)},
 *       {@link #isSudahTerbayarUntukPayment(VirtualAccountBank, boolean, boolean, boolean)}.</li>
 *   <li><b>Mesin pembayaran</b> — {@link #bayarVa(VirtualAccountBank, Date, String, Session)},
 *       {@link #bayarSiswa(VirtualAccountBank, Session, Date, String, boolean, String, boolean)},
 *       {@link #bayarSiswaLangsung(List, Session, Date, Double, Double, String, boolean, Tbmuser)},
 *       {@link #bayarTopup(VirtualAccountBank, Session, Date, String, boolean, String)},
 *       {@link #updateVa(VirtualAccountBank, Date, Kegiatan, String, String)},
 *       {@link #updateTotal(VirtualAccountBank, Double)}.</li>
 *   <li><b>Pencarian VA</b> — {@link #ambilVa(String, BankHost)} beserta overload-nya,
 *       {@link #ambilLink(String, BankHost)}, {@link #ambilByNisAja(String, Double)}.</li>
 *   <li><b>Perakit token cicilan</b> — {@link #populateCicilan(Grid)} (dari UI ZK) dan
 *       {@link #populateCicilan(Session, String, Kegiatan, Long, Date, JenisPembayaran, String)}
 *       (dipakai gateway lain untuk memposting token yang sudah jadi).</li>
 *   <li><b>Jembatan HTTP e-Smartlink</b> — {@link #curlSmartlink(String, String, String, JSONObject)}
 *       dan {@link #curlSmartlinkGet(String, String, String)}.</li>
 *   <li><b>Accessor entity</b> dan <b>getter ringkas untuk kolom grid ZK</b>
 *       ({@code *Ringkas}, {@link #getTotalAman()}).</li>
 * </ul>
 *
 * <h3>PERINGATAN: getter yang menulis balik</h3>
 * <p>Seperti entity AIS lain, banyak getter di kelas ini <b>bukan getter murni</b> — mereka
 * mengubah field (dan karenanya bisa ikut ter-flush ke database) saat dibaca. Yang paling berbahaya:</p>
 * <ul>
 *   <li>{@link #getWaktuBayar()} <b>mengosongkan</b> {@code waktuBayar} bila ketiga penanda hasil
 *       posting kosong;</li>
 *   <li>{@link #getAkunPembayaranSiswa()} <b>mengosongkan</b> field akun bila akun bertipe
 *       tabungan/manual — pembacaan biasa bisa menghapus FK saat flush;</li>
 *   <li>{@link #getKegiatan()} membaca berkas cache samping ({@code retreive}) dan dapat menyetel
 *       {@code kegiatan} menjadi {@code null} bila penanda {@code "hapus"} bernilai {@code "1"};</li>
 *   <li>{@link #getKode()}, {@link #getNama()}, {@link #getBank()}, {@link #getSemester()},
 *       {@link #getTahunAkademik()}, {@link #getBiayaAdmin()}, {@link #getAmount()},
 *       {@link #getKadaluarsa()}, {@link #getKadaluarsaWaktu()}, {@link #getPt()},
 *       {@link #getKanalPembayaran()}, {@link #getKelas()}, {@link #getChannel()} semuanya menimpa
 *       field dari sumber lain (JSON callback, relasi, konfigurasi).</li>
 * </ul>
 * <p>Konsekuensinya: jangan pernah menganggap membaca entity ini bebas efek samping, dan hati-hati
 * memanggil getter-nya di dalam session yang masih terbuka dengan flush otomatis.</p>
 *
 * <h3>PERINGATAN: kredensial &amp; konfigurasi</h3>
 * <p>{@link #curlSmartlink(String, String, String, JSONObject)} dan
 * {@link #curlSmartlinkGet(String, String, String)} menerima username/password e-Smartlink,
 * mengubahnya menjadi header {@code Authorization: Basic ...}, lalu menaruhnya sebagai
 * <b>argumen baris perintah</b> {@code curl} — dan, bila konfigurasi
 * {@code curl_e_smartlink_via_server_lain} aktif, sebagai bagian dari string perintah yang dikirim
 * lewat {@code ssh} ke host lain. Nilai default host/port/user relay tersebut ter-hardcode di kode
 * (lihat method terkait). Perhatikan hal ini saat audit governance atau rotasi kredensial;
 * {@code Common.getKonfigurasi} akan <b>menulis nilai default ke database</b> bila baris konfigurasi
 * belum ada, sehingga default yang tertulis di sini bisa menjadi nilai produksi.</p>
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap
 * dimiliki {@link GeneralValueObject} (kontrak {@code id}/{@code equals}/{@code compareTo}/
 * {@code check()}/{@code put()}/{@code retreive()}). Kelas ini hanya memuat yang khas VA. Riwayat
 * perubahan baris ditangani Envers ({@code @Audited}) dan disajikan oleh
 * {@code ais.action.master.helper.RevisiVirtualAccountBankHelper}.</p>
 *
 * @see GeneralValueObject
 * @see BankHost
 * @see Va
 * @see Kegiatan
 * @see CicilanPembayaran
 * @see Deposit
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "virtual_account_bank")
public class VirtualAccountBank extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key {@code virtual_account_bank.id}; ikut dipakai membentuk nomor VA otomatis. */
	private Long id;
	/** Nama pengguna terakhir yang menyimpan baris ini (jejak audit ringan, lihat {@link #setOleh(String)}). */
	private String oleh;
	/** Identitas (user id) pengguna terakhir yang menyimpan baris ini. */
	private String olehId;
	/** Penanda apakah tagihan ini benar-benar diterbitkan sebagai VA; default {@code true}. */
	private Boolean pakaiva;
	/** Buffer HTML sementara untuk kebutuhan tampilan/cetak; {@code @Transient}, tidak disimpan. */
	private String htmlTemporaryData;

	/**
	 * Cache <b>in-memory</b> berisi id VA yang pembayarannya sudah selesai diproses pada JVM ini.
	 *
	 * <p>Fungsinya menahan callback ganda dari bank (bank kerap mengirim notifikasi berkali-kali)
	 * agar dokumen pembayaran tidak dibuat dobel, tanpa harus menembak database dulu. Diisi oleh
	 * {@link #bayarSiswa(VirtualAccountBank, Session, Date, String, boolean, String, boolean)} dan
	 * {@link #updateVa(VirtualAccountBank, Date, Kegiatan, String, String)}; dibuang kembali oleh
	 * {@code updateVa} ketika seluruh penanda hasil posting kosong (mis. setelah pembatalan).</p>
	 *
	 * <p><b>Batas yang perlu disadari:</b> ini {@code static} dan tidak pernah dikosongkan massal,
	 * jadi (a) isinya tumbuh selama JVM hidup, (b) tidak dibagi antar node bila aplikasi berjalan
	 * lebih dari satu instance — karena itu pengecekan cache SELALU didampingi pengecekan permanen
	 * ke database pada method pemakainya. Set dibungkus {@link Collections#synchronizedSet(Set)}
	 * sehingga aman dipakai banyak thread callback.</p>
	 */
	public static Set<Long> sukses = Collections.synchronizedSet(new HashSet<Long>());

	/**
	 * Guard umum supaya callback/payment VA yang sama tidak diproses ulang.
	 * Dicek dari dua sumber:
	 * 1) cache in-memory sukses, untuk callback berulang dalam JVM yang sama;
	 * 2) penanda permanen di database: kegiatan, pembayaran siswa, atau deposit/topup.
	 *
	 * <p>Urutan pemeriksaan: {@code null} &rarr; {@code false}; id ada di {@link #sukses} &rarr;
	 * {@code true}; nilai {@code total} tidak lebih dari 0,1 &rarr; {@code false} (VA bernilai nol
	 * dianggap belum bisa disebut terbayar); selebihnya {@code true} bila salah satu dari
	 * {@link #getKegiatan()}, {@link #getPembayaran()}, atau {@link #getDeposit()} sudah terisi.</p>
	 *
	 * <p>Perhatikan bahwa {@link #getKegiatan()} bukan getter murni (membaca cache berkas samping),
	 * jadi pemanggilan method ini ikut menyentuh mekanisme itu.</p>
	 *
	 * @param virtualAccountBank baris VA yang diperiksa; boleh {@code null}
	 * @return {@code true} bila pembayaran VA ini sudah pernah diproses
	 * @see #isSudahTerbayarUntukPayment(VirtualAccountBank, boolean, boolean, boolean)
	 */
	public static boolean isSudahTerbayar(VirtualAccountBank virtualAccountBank) {
		if (virtualAccountBank == null) {
			return false;
		}

		Long id = virtualAccountBank.getId();
		if (id != null && sukses.contains(id)) {
			return true;
		}

		Double total = virtualAccountBank.getTotal();
		boolean punyaNilaiTagihan = total != null && total.doubleValue() > 0.1;
		if (!punyaNilaiTagihan) {
			return false;
		}

		return virtualAccountBank.getKegiatan() != null || virtualAccountBank.getPembayaran() != null
				|| virtualAccountBank.getDeposit() != null;
	}

	/**
	 * Varian {@link #isSudahTerbayar(VirtualAccountBank)} yang dipakai servlet callback bank untuk
	 * memutuskan apakah permintaan yang masuk harus ditolak sebagai duplikat.
	 *
	 * <p>Tiga bendera pertama mematikan penjagaan, karena jenis permintaan tersebut memang boleh
	 * datang berkali-kali pada VA yang sudah lunas:</p>
	 * <ul>
	 *   <li>{@code inquery} — bank hanya menanyakan detail tagihan, tidak memposting apa pun;</li>
	 *   <li>{@code reversal} — bank membatalkan pembayaran yang sudah dikirim;</li>
	 *   <li>{@code chek} — pengecekan/rekonsiliasi ulang dari sisi operator.</li>
	 * </ul>
	 *
	 * @param virtualAccountBank baris VA yang diperiksa; boleh {@code null}
	 * @param inquery            {@code true} bila permintaan hanya inquiry
	 * @param reversal           {@code true} bila permintaan berupa pembatalan
	 * @param chek               {@code true} bila permintaan berupa cek ulang
	 * @return {@code true} hanya bila permintaan benar-benar payment DAN VA-nya sudah terbayar
	 */
	public static boolean isSudahTerbayarUntukPayment(VirtualAccountBank virtualAccountBank, boolean inquery,
			boolean reversal, boolean chek) {
		return !inquery && !reversal && !chek && isSudahTerbayar(virtualAccountBank);
	}

	/**
	 * Menyegarkan cache daftar cicilan milik satu {@link Kegiatan} setelah pembayarannya berubah.
	 *
	 * <p>{@code KegiatanPersistenceHelper.ambilCicilan(kegiatan, true)} dipanggil dengan bendera
	 * "paksa muat ulang", supaya tampilan/laporan yang membaca cicilan berikutnya tidak menyajikan
	 * data lama. Nilai kembalian helper sengaja diabaikan — yang dibutuhkan hanya efek sampingnya.</p>
	 *
	 * <p>Kegagalan ditelan dan hanya ditampilkan kepada admin ({@code Common.tampilErrorJikaAdmin}),
	 * karena kegagalan menyegarkan cache tidak boleh membatalkan pembayaran yang sudah tercatat.</p>
	 *
	 * @param kegiatan kegiatan yang baru saja dibayar; {@code null} atau belum ber-id diabaikan
	 */
	private static void sinkronkanKegiatanSetelahBayar(Kegiatan kegiatan) {
		if (kegiatan == null || kegiatan.getId() == null) {
			return;
		}
		try {
			KegiatanPersistenceHelper.ambilCicilan(kegiatan, true);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menerjemahkan prefix referensi gateway menjadi nama <i>validator</i> yang dicatat pada
	 * {@link CicilanPembayaran#setValidator(String)} — yaitu teks yang muncul di bukti pembayaran
	 * sebagai "dibayar melalui ...".
	 *
	 * <p>Tiga bank punya nama resmi yang dapat disetel operator lewat konfigurasi:</p>
	 * <ul>
	 *   <li>{@code "bsi"} &rarr; konfigurasi {@code default_validator_bsi} (default
	 *       {@code "Bank Syariah Indonesia"});</li>
	 *   <li>{@code "bri"} &rarr; konfigurasi {@code default_validator_bri} (default {@code "BRI"});</li>
	 *   <li>{@code "bni"} &rarr; konfigurasi {@code default_validator_bni} (default {@code "BNI"}).</li>
	 * </ul>
	 * <p>Prefix lain dikembalikan apa adanya dalam huruf kapital; prefix kosong/{@code null} menjadi
	 * {@code "BANK"}.</p>
	 *
	 * <p><b>Catatan konfigurasi:</b> {@code Common.getKonfigurasi(nama, default)} akan menuliskan
	 * nilai default ke database bila baris konfigurasinya belum ada. Mengubah literal default di
	 * sini karena itu berdampak pada instalasi yang belum pernah memiliki baris tersebut.</p>
	 *
	 * @param prefix prefix referensi gateway (mis. {@code "bri"}), tidak peka huruf besar/kecil
	 * @return nama validator yang layak ditampilkan; tidak pernah {@code null}
	 * @see #populateCicilan(Session, String, Kegiatan, Long, Date, JenisPembayaran, String)
	 */
	private static String ambilDefaultValidatorBank(String prefix) {
		String bank = prefix == null ? "" : prefix.trim().toLowerCase();
		if ("bsi".equals(bank)) {
			return Common.getKonfigurasi("default_validator_bsi", "Bank Syariah Indonesia").getNilai();
		}
		if ("bri".equals(bank)) {
			return Common.getKonfigurasi("default_validator_bri", "BRI").getNilai();
		}
		if ("bni".equals(bank)) {
			return Common.getKonfigurasi("default_validator_bni", "BNI").getNilai();
		}
		return prefix == null || prefix.trim().length() == 0 ? "BANK" : prefix.trim().toUpperCase();
	}

	/**
	 * Memproses bagian <b>topup saldo</b> dari sebuah pembayaran VA: membuat atau memperbarui satu
	 * {@link Deposit} senilai {@code topup}, lalu menautkannya kembali ke baris VA.
	 *
	 * <p>Dipanggil sebagai langkah pertama oleh
	 * {@link #bayarSiswa(VirtualAccountBank, Session, Date, String, boolean, String, boolean)}
	 * sehingga semua kanal (BRI/BNI/BSI/BTN dan gateway online lain) menambah saldo lewat satu
	 * pintu yang sama.</p>
	 *
	 * <p><b>Idempoten.</b> Bila kolom {@code deposit} pada VA sudah berisi id, deposit lama
	 * dimuat ulang dan ditimpa nilainya — bukan dibuat baru. Callback ganda karenanya tidak
	 * menggandakan saldo.</p>
	 *
	 * <p><b>Efek samping:</b>
	 * <ul>
	 *   <li>menyimpan/memperbarui {@link Deposit} (mahasiswa, calon mahasiswa, siswa, calon siswa,
	 *       atau anggota koperasi — diambil apa adanya dari VA);</li>
	 *   <li>menyetel {@code waktuBayar} dan {@code notif} pada VA;</li>
	 *   <li>membuat baris {@link Va} baru bila VA belum punya (nomor VA dipakai sebagai kodenya);</li>
	 *   <li>menyimpan VA itu sendiri melalui {@code Common.refreshSaveOrUpdate}.</li>
	 * </ul>
	 * Setiap penyimpanan memakai transaksi pendek tersendiri pada {@code session} yang diberikan;
	 * bila terjadi kegagalan, transaksi yang sedang aktif di-rollback dan error hanya ditampilkan
	 * kepada admin sehingga alur pembayaran di pemanggil tidak ikut gagal.</p>
	 *
	 * <p>Method langsung keluar tanpa melakukan apa pun bila {@code inquery} bernilai {@code true}
	 * atau nilai {@code topup} tidak lebih dari 0,1.</p>
	 *
	 * @param virtualAccountBankNtt baris VA yang sedang dibayar (dimutasi oleh method ini)
	 * @param session               session Hibernate aktif milik pemanggil
	 * @param tanggal               waktu pembayaran menurut bank
	 * @param bank                  nama bank/kanal pembayar; saat ini tidak dipakai di sini,
	 *                              dipertahankan agar tanda tangan seragam dengan method bayar lain
	 * @param inquery               {@code true} bila permintaan hanya inquiry (tidak memposting)
	 * @param notif                 payload notifikasi mentah dari bank, disimpan ke kolom {@code notif}
	 * @see Deposit
	 */
	public static void bayarTopup(VirtualAccountBank virtualAccountBankNtt, Session session, Date tanggal, String bank,
			boolean inquery, String notif) {
		if (!inquery && virtualAccountBankNtt.getTopup() != null && virtualAccountBankNtt.getTopup() > 0.1) {
			Transaction tx = null;
			try {
				Deposit deposit = (Deposit) (virtualAccountBankNtt.getDeposit() == null ? null
						: session.createCriteria(Deposit.class)
								.add(Restrictions.idEq(virtualAccountBankNtt.getDeposit())).uniqueResult());

				if (deposit == null || deposit.getId() == null) {
					deposit = new Deposit();
				}

				deposit.setMahasiswa(virtualAccountBankNtt.getMahasiswa());
				deposit.setAnggotaKoperasi(virtualAccountBankNtt.getAnggotaKoperasi());
				deposit.setSiswa(virtualAccountBankNtt.getSiswa());
				deposit.setCalonSiswa(virtualAccountBankNtt.getCalonSiswa());
				deposit.setBiodataCalonMahasiswa(virtualAccountBankNtt.getBiodataCalonMahasiswa());
				deposit.setNominal(virtualAccountBankNtt.getTopup());
				deposit.setWaktu(tanggal);

				tx = session.beginTransaction();
				Common.refreshSaveOrUpdate(session, deposit);
				tx.commit();

				virtualAccountBankNtt.setWaktuBayar(tanggal);
				virtualAccountBankNtt.setNotif(notif);

				if (virtualAccountBankNtt.getVa() == null) {
					Va va = new Va();
					va.setKode(virtualAccountBankNtt.getKode());
					tx = session.beginTransaction();
					session.save(va);
					tx.commit();
					virtualAccountBankNtt.setVa(va);
				}

				virtualAccountBankNtt.setDeposit(deposit.getId());
				tx = session.beginTransaction();
				Common.refreshSaveOrUpdate(session, virtualAccountBankNtt);
				tx.commit();

			} catch (Exception e) {
				if (tx != null && tx.isActive())
					tx.rollback();
				Common.tampilErrorJikaAdmin(e);
			}
		}
	}


	/**
	 * Menuliskan hasil pembayaran siswa ke baris VA dengan <b>HQL bulk update</b>, hanya menyentuh
	 * empat kolom: {@code waktuBayar}, {@code pembayaran}, {@code notif}, {@code va}.
	 *
	 * <p>Alasan memakai bulk update alih-alih {@code session.update(entity)}: entity
	 * {@code VirtualAccountBank} punya banyak getter yang menulis balik ke field (lihat catatan di
	 * Javadoc kelas). Bila entity di-flush utuh, nilai-nilai hasil "getter cerdas" itu ikut
	 * tersimpan dan dapat merusak kolom lain. Karena itu {@link FlushMode#MANUAL} dipasang selama
	 * eksekusi dan dikembalikan ke mode semula di blok {@code finally}.</p>
	 *
	 * <p>Setelah update berhasil di-commit, keempat nilai yang sama juga disetel pada object
	 * {@code va} di memori agar pemanggil melihat state yang konsisten.</p>
	 *
	 * @param session     session Hibernate aktif
	 * @param va          baris VA yang diperbarui; {@code null} atau belum ber-id diabaikan
	 * @param tanggal     waktu pembayaran
	 * @param pembayaranId id {@code PembayaranSiswa} hasil posting
	 * @param notif       payload notifikasi mentah dari bank
	 * @throws Exception bila update gagal — sengaja dilempar ulang setelah rollback supaya pemanggil
	 *                   tahu bahwa penandaan lunas tidak tersimpan
	 */
	private static void updateVirtualAccountSiswaMinimal(Session session, VirtualAccountBank va, Date tanggal,
			Long pembayaranId, String notif) throws Exception {
		if (session == null || va == null || va.getId() == null) {
			return;
		}
		FlushMode oldFlushMode = null;
		Transaction tx = null;
		try {
			oldFlushMode = session.getFlushMode();
			session.setFlushMode(FlushMode.MANUAL);
			tx = session.beginTransaction();
			Query query = session.createQuery("update VirtualAccountBank set waktuBayar = :waktuBayar, "
					+ "pembayaran = :pembayaran, notif = :notif, va = :va where id = :id");
			query.setParameter("waktuBayar", tanggal);
			query.setParameter("pembayaran", pembayaranId);
			query.setParameter("notif", notif);
			query.setParameter("va", va.getVa());
			query.setParameter("id", va.getId());
			query.executeUpdate();
			tx.commit();
			va.setWaktuBayar(tanggal);
			va.setPembayaran(pembayaranId);
			va.setNotif(notif);
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:224");}
			throw e;
		} finally {
			try { if (oldFlushMode != null) session.setFlushMode(oldFlushMode); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:227");}
		}
	}

	/**
	 * Jalur posting pembayaran VA untuk <b>siswa / calon siswa sekolah</b>: mengubah satu callback
	 * bank menjadi dokumen {@code PembayaranSiswa} beserta rincian
	 * {@code PembayaranSiswaDetail}, sekaligus menandai {@link Tagihan} yang terlunasi.
	 *
	 * <p>Pendamping method ini untuk jalur perguruan tinggi adalah
	 * {@link #bayarVa(VirtualAccountBank, Date, String, Session)}.</p>
	 *
	 * <h4>Urutan kerja</h4>
	 * <ol>
	 *   <li><b>Topup dulu.</b> {@link #bayarTopup(VirtualAccountBank, Session, Date, String,
	 *       boolean, String)} dipanggil tanpa syarat agar seluruh kanal menambah saldo lewat satu
	 *       pintu. Bila VA ternyata topup murni (ada nilai {@code topup} tetapi {@code cicilan}
	 *       kosong), method langsung mengembalikan map kosong supaya tidak lahir
	 *       {@code PembayaranSiswa} tanpa isi.</li>
	 *   <li><b>Menentukan akun kas.</b> Dipakai {@link #getAkunPembayaranSiswa()} milik VA; bila
	 *       kosong, dicari {@code AkunPembayaranSiswa} pertama milik sekolah yang aktif dan bukan
	 *       akun manual.</li>
	 *   <li><b>Menguraikan token {@code cicilan}.</b> Pada jalur siswa setiap token berbentuk
	 *       {@code "<apa saja>-<idTagihan>[-<nilai>]"}. Tagihan yang sudah tidak ada (atau tanpa
	 *       {@code NominalBiaya}) dilewati dan dicatat ke {@code System.out}. Tagihan yang valid
	 *       dikelompokkan ke dalam map hasil dengan kunci
	 *       {@code "<idJenisBiayaSekolah>_<tahunbulan>"}.</li>
	 *   <li><b>Penjagaan duplikat</b> (hanya bila {@code inquery} {@code false}): cek cache
	 *       {@link #sukses}, lalu hitung {@code PembayaranSiswa} yang FK-nya menunjuk VA ini —
	 *       sengaja lewat FK, bukan lewat kolom {@code pembayaran} pada VA yang bisa basi. Ada
	 *       pemeriksaan lapis ketiga berbasis kolom {@code pembayaran} di dalam blok penyimpanan.</li>
	 *   <li><b>Posting.</b> {@code PembayaranSiswa} disimpan, deposit disinkronkan
	 *       ({@code saveOrUpdateDeposit}), VA ditandai lunas, {@link Va} dibuat bila belum ada, lalu
	 *       tiap {@link Tagihan} memperoleh satu {@code PembayaranSiswaDetail} dan ditautkan balik.
	 *       Nominal per rincian diambil dari {@code nominal - diskon + denda}, kecuali bila token
	 *       {@code cicilan} membawa nilai eksplisit pada posisi ketiga. Diskon yang tidak memotong
	 *       tagihan diteruskan ke
	 *       {@code DaftarPengajuanTransfer.simpanDiskonPembayaran}.</li>
	 *   <li><b>Penutup.</b> Penandaan lunas ditulis ulang secara minimal lewat
	 *       {@link #updateVirtualAccountSiswaMinimal(Session, VirtualAccountBank, Date, Long, String)},
	 *       bukti pembayaran dicetak ({@code PembayaranSiswaUtil.cetakVa}), id VA masuk ke cache
	 *       {@link #sukses}, dan ringkasan pembayaran siswa/calon siswa dimuat ulang
	 *       ({@code populatePembayaran()}).</li>
	 * </ol>
	 *
	 * <p><b>Efek samping berat:</b> banyak transaksi pendek berturut-turut pada {@code session}
	 * pemanggil (bukan satu transaksi besar), pencetakan berkas bukti, dan penulisan cache statis.
	 * Kegagalan pada satu rincian tagihan hanya me-rollback transaksi rincian itu; sisa proses tetap
	 * berjalan.</p>
	 *
	 * @param virtualAccountBankNtt baris VA yang sedang dibayar (dimutasi oleh method ini)
	 * @param session               session Hibernate aktif milik pemanggil
	 * @param tanggal               waktu pembayaran menurut bank
	 * @param bank                  nama bank/kanal, disimpan sebagai validator pembayaran
	 * @param inquery               {@code true} bila permintaan hanya inquiry; posting dilewati dan
	 *                              hanya daftar tagihan yang dikembalikan
	 * @param notif                 payload notifikasi mentah dari bank
	 * @param cetak                 {@code true} bila bukti pembayaran perlu langsung dicetak
	 * @return map tagihan yang tercakup callback ini, berkunci
	 *         {@code "<idJenisBiayaSekolah>_<tahunbulan>"}; kosong bila topup murni atau tidak ada
	 *         tagihan yang cocok
	 * @see #bayarSiswaLangsung(List, Session, Date, Double, Double, String, boolean, Tbmuser)
	 */
	public static Map<String, List<Tagihan>> bayarSiswa(VirtualAccountBank virtualAccountBankNtt, Session session,
			Date tanggal, String bank, boolean inquery, String notif, boolean cetak) {

		// Semua callback pembayaran siswa melewati method ini. Dengan menempatkan
		// topup di satu pintu, saldo bertambah konsisten untuk BRI/BNI/BSI/BTN dan
		// kanal online lain. bayarTopup idempotent karena menggunakan kembali id
		// Deposit yang sudah tersimpan pada VirtualAccountBank.
		bayarTopup(virtualAccountBankNtt, session, tanggal, bank, inquery, notif);

		// Topup murni tidak mempunyai token cicilan/tagihan. Setelah saldo diproses
		// oleh bayarTopup(), hentikan jalur pembayaran tagihan agar sistem tidak
		// mencoba mencari akun atau membuat PembayaranSiswa kosong. Transaksi yang
		// menggabungkan topup dan tagihan tetap diteruskan seperti biasa.
		if (virtualAccountBankNtt.getTopup() != null && virtualAccountBankNtt.getTopup() > 0.1
				&& (virtualAccountBankNtt.getCicilan() == null
						|| virtualAccountBankNtt.getCicilan().trim().length() == 0)) {
			return new HashMap<String, List<Tagihan>>();
		}

		AkunPembayaranSiswa akunPembayaranSiswa = virtualAccountBankNtt.getAkunPembayaranSiswa();
		if (akunPembayaranSiswa == null) {
			Sekolah sekolah = virtualAccountBankNtt.getSiswa() == null
					? virtualAccountBankNtt.getCalonSiswa().getSekolah()
					: virtualAccountBankNtt.getSiswa().getSekolah();

			akunPembayaranSiswa = (AkunPembayaranSiswa) ConstantValues.simpleObject(session
					.createCriteria(AkunPembayaranSiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("manual", false)).add(Restrictions.eq("sekolah", sekolah)).setMaxResults(1),
					AkunPembayaranSiswa.class);
		}

		Map<String, List<Tagihan>> maps = new HashMap<String, List<Tagihan>>();
		if (virtualAccountBankNtt.getCicilan() != null && !virtualAccountBankNtt.getCicilan().isEmpty()) {
			for (String c : virtualAccountBankNtt.getCicilan().split(",")) {
				String[] parts = c.split("-");
				if (parts.length > 1) {
					Long idTagihan = Long.parseLong(parts[1]);
					Tagihan tagihan = (Tagihan) session.createCriteria(Tagihan.class)
							.add(Restrictions.idEq(idTagihan)).uniqueResult();
					if (tagihan != null && tagihan.getNominalBiaya() != null) {
						Long id = tagihan.getPengaturanBiaya().getJenisBiayaSekolah().getId();
						String key = id + "_" + tagihan.getTahunbulan();
						if (!maps.containsKey(key)) {
							maps.put(key, new ArrayList<Tagihan>());
						}
						maps.get(key).add(tagihan);
					} else {
						System.out.println("Token cicilan VA dilewati karena tagihan sudah tidak tersedia: idTagihan="
								+ idTagihan + ", virtualAccountBankNtt id=" + virtualAccountBankNtt.getId()
								+ ", cicilan mentah='" + virtualAccountBankNtt.getCicilan() + "'");
					}
				}
			}
		}

		if (!inquery) {
			// Cek in-memory sukses set — tidak perlu query DB, paling cepat.
			if (virtualAccountBankNtt.getId() != null && sukses.contains(virtualAccountBankNtt.getId())) {
				System.out.println("Pembayaran VA " + virtualAccountBankNtt + " sudah pernah diproses (cache). Proses siswa dilewati.");
				return maps;
			}
			// Cek langsung ke PembayaranSiswa berdasarkan FK virtualAccountBank —
			// tidak andalkan kolom pembayaran di VAB yang bisa basi (mis. saat Cek Ulang
			// atau data tidak konsisten). Jika belum ada → lanjut insert.
			Double totalVaBayarSiswa = virtualAccountBankNtt.getTotal();
			if (totalVaBayarSiswa != null && totalVaBayarSiswa > 0.1 && virtualAccountBankNtt.getId() != null) {
				long countPembayaranSiswa = ((Number) session.createCriteria(PembayaranSiswa.class)
						.setProjection(Projections.rowCount())
						.add(Restrictions.eq("virtualAccountBank", virtualAccountBankNtt))
						.uniqueResult()).longValue();
				if (countPembayaranSiswa > 0) {
					System.out.println("Pembayaran VA " + virtualAccountBankNtt + " sudah ada di PembayaranSiswa. Proses siswa dilewati.");
					return maps;
				}
			}
			Transaction tx = null;
			try {
				if (maps != null && !maps.isEmpty()) {

					// Fallback: cek juga via id PembayaranSiswa yang tersimpan di kolom VAB
					if (virtualAccountBankNtt.getPembayaran() != null) {
						int count = ((Number) session.createCriteria(PembayaranSiswa.class)
								.setProjection(Projections.rowCount())
								.add(Restrictions.idEq(virtualAccountBankNtt.getPembayaran())).uniqueResult())
								.intValue();
						if (count > 0) {
							System.out
									.println("Pembayaran dengan va " + virtualAccountBankNtt.toString() + " sudah ada");
							return maps;
						}
					}

					Double tabungan = virtualAccountBankNtt.getTabungan();
					if (tabungan != null && tabungan > virtualAccountBankNtt.getTotal()) {
						tabungan = virtualAccountBankNtt.getTotal();
					}

					PembayaranSiswa pembayaranSiswa = new PembayaranSiswa();
					pembayaranSiswa.setSiswa(virtualAccountBankNtt.getSiswa());
					pembayaranSiswa.setCalonSiswa(virtualAccountBankNtt.getCalonSiswa());
					pembayaranSiswa.setTanggal(tanggal);
					pembayaranSiswa.setKeterangan(virtualAccountBankNtt.getKeterangan());
					pembayaranSiswa.setAkunPembayaranSiswa(akunPembayaranSiswa);
					pembayaranSiswa.setNominal(virtualAccountBankNtt.getTotal());
					pembayaranSiswa.setVirtualAccountBank(virtualAccountBankNtt);
					pembayaranSiswa.setDariTabungan(tabungan);
					pembayaranSiswa.setDariTabunganManual(tabungan);
					pembayaranSiswa.setTambahanDeposit(virtualAccountBankNtt.getTotal());
					pembayaranSiswa.setValidator(bank);

					tx = session.beginTransaction();
					if (pembayaranSiswa.getId() == null)
						session.save(pembayaranSiswa);
					else
						Common.refreshSaveOrUpdate(session, pembayaranSiswa);
					tx.commit();

					tx = session.beginTransaction();
					pembayaranSiswa.saveOrUpdateDeposit(session);
					tx.commit();

					virtualAccountBankNtt.setWaktuBayar(tanggal);
					virtualAccountBankNtt.setPembayaran(pembayaranSiswa.getId());
					virtualAccountBankNtt.setNotif(notif);

					if (virtualAccountBankNtt.getVa() == null) {
						Va va = new Va();
						va.setKode(virtualAccountBankNtt.getKode());
						tx = session.beginTransaction();
						session.save(va);
						tx.commit();
						virtualAccountBankNtt.setVa(va);
					}

					for (List<Tagihan> details : maps.values()) {
						for (Tagihan tagihan : details) {
							try {
								NominalBiaya nominalBiaya = tagihan.getNominalBiaya();
								if (nominalBiaya != null) {
									Double nominal = tagihan.getNominal() - tagihan.getDiskon() + tagihan.getDenda();

									if (virtualAccountBankNtt.getCicilan() != null) {
										for (String s : virtualAccountBankNtt.getCicilan().split(",")) {
											String[] ss = s.split("-");
											if (ss.length > 2 && tagihan.getId().equals(Long.parseLong(ss[1].trim()))) {
												nominal = Double.parseDouble(ss[2].trim());
												break;
											}
										}
									}

									PembayaranSiswaDetail pembayaranSiswaDetail = new PembayaranSiswaDetail(tagihan);
									pembayaranSiswaDetail.setItemBiayaSekolah(nominalBiaya.getItemBiayaSekolah());
									pembayaranSiswaDetail.setNominalBiaya(nominalBiaya);
									pembayaranSiswaDetail.setNominal(nominal);
									pembayaranSiswaDetail.setNominalManual(nominal);
									pembayaranSiswaDetail.setPembayaranSiswa(pembayaranSiswa);

									tx = session.beginTransaction();
									session.save(pembayaranSiswaDetail);
									tx.commit();

									session.refresh(tagihan);
									tagihan.setPembayaranSiswaDetail(pembayaranSiswaDetail);

									tx = session.beginTransaction();
									session.update(tagihan);
									tx.commit();

									if (tagihan.getDiskonSiswa() != null
											&& !tagihan.getDiskonSiswa().getMemotongTagihan()) {
										DaftarPengajuanTransfer.simpanDiskonPembayaran(tagihan);
									}
								}
							} catch (Exception e) {
								if (tx != null && tx.isActive())
									tx.rollback();
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:388");
							}
						}
					}

					updateVirtualAccountSiswaMinimal(session, virtualAccountBankNtt, tanggal, pembayaranSiswa.getId(), notif);

					PembayaranSiswaUtil.cetakVa(pembayaranSiswa, virtualAccountBankNtt, cetak);
				}
				sukses.add(virtualAccountBankNtt.getId());

			} catch (Exception e) {
				if (tx != null && tx.isActive())
					tx.rollback();
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:402");
			}

			if (virtualAccountBankNtt != null) {
				if (virtualAccountBankNtt.getCalonSiswa() != null
						&& virtualAccountBankNtt.getCalonSiswa().getId() != null) {
					virtualAccountBankNtt.getCalonSiswa().populatePembayaran();
				}
				if (virtualAccountBankNtt.getSiswa() != null && virtualAccountBankNtt.getSiswa().getId() != null) {
					virtualAccountBankNtt.getSiswa().populatePembayaran();
				}
			}
		}
		return maps;
	}

	/**
	 * Memposting pembayaran tagihan siswa <b>tanpa melalui VA</b> — dipakai saat pelunasan dilakukan
	 * langsung oleh petugas, umumnya memotong saldo tabungan/deposit siswa.
	 *
	 * <p>Bedanya dengan
	 * {@link #bayarSiswa(VirtualAccountBank, Session, Date, String, boolean, String, boolean)}:</p>
	 * <ul>
	 *   <li>daftar {@link Tagihan} diberikan langsung sebagai parameter, tidak diurai dari token
	 *       {@code cicilan};</li>
	 *   <li>akun kas dicari dengan prioritas akun bertanda {@code dariTabungan}, baru jatuh ke akun
	 *       non-manual bila tidak ada;</li>
	 *   <li>{@code tambahanDeposit} sengaja disetel {@code 0.0} supaya tidak lahir record
	 *       {@code DepositSiswa} baru — uangnya memang berasal dari deposit yang sudah ada;</li>
	 *   <li>petugas yang memvalidasi ikut dicatat: {@code tbmuser} dimuat ulang dari session aktif
	 *       agar FK-nya sah, dan menjadi {@code null} bila user tidak ditemukan.</li>
	 * </ul>
	 *
	 * <p>Nilai {@code tabungan} dipangkas agar tidak melebihi {@code total}. Setiap tagihan dimuat
	 * ulang dari {@code session} (menghindari entity detached), diberi satu
	 * {@code PembayaranSiswaDetail} senilai {@code nominal - diskon + denda}, lalu ditautkan balik.
	 * Diskon yang tidak memotong tagihan diteruskan ke
	 * {@code DaftarPengajuanTransfer.simpanDiskonPembayaran}. Bukti pembayaran dicetak lewat
	 * {@code PembayaranSiswaUtil.cetakVa} dengan argumen VA {@code null}.</p>
	 *
	 * <p>Seluruh badan method dibungkus satu {@code try}: bila gagal di tengah, transaksi terakhir
	 * di-rollback dan object {@code PembayaranSiswa} yang mungkin sudah tersimpan sebagian tetap
	 * dikembalikan — pemanggil wajib memeriksa hasilnya.</p>
	 *
	 * @param tagihans daftar tagihan yang dilunasi; {@code null}/kosong &rarr; {@code null}
	 * @param session  session Hibernate aktif
	 * @param tanggal  tanggal pembayaran
	 * @param total    nominal total pembayaran
	 * @param tabungan porsi yang diambil dari tabungan/deposit; dipangkas ke {@code total}
	 * @param bank     teks validator (nama kanal/petugas) yang dicatat pada pembayaran
	 * @param cetak    {@code true} bila bukti pembayaran perlu langsung dicetak
	 * @param tbmuser  petugas yang memvalidasi; boleh {@code null}
	 * @return dokumen {@code PembayaranSiswa} yang terbentuk, atau {@code null} bila daftar tagihan
	 *         kosong
	 */
	public static PembayaranSiswa bayarSiswaLangsung(List<Tagihan> tagihans, Session session, Date tanggal,
			Double total, Double tabungan, String bank, boolean cetak, Tbmuser tbmuser) {

		if (tagihans == null || tagihans.isEmpty())
			return null;
		if (tabungan != null && tabungan > total)
			tabungan = total;

		Tagihan tagihanD = tagihans.get(0);
		Sekolah sekolah = tagihanD.getSiswa() == null ? tagihanD.getCalonSiswa().getSekolah()
				: tagihanD.getSiswa().getSekolah();

		AkunPembayaranSiswa akunPembayaranSiswa = (AkunPembayaranSiswa) ConstantValues.simpleObject(session
				.createCriteria(AkunPembayaranSiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.eq("dariTabungan", true)).add(Restrictions.eq("sekolah", sekolah)).setMaxResults(1),
				AkunPembayaranSiswa.class);

		if (akunPembayaranSiswa == null) {
			akunPembayaranSiswa = (AkunPembayaranSiswa) ConstantValues.simpleObject(session
					.createCriteria(AkunPembayaranSiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("manual", false)).add(Restrictions.eq("sekolah", sekolah)).setMaxResults(1),
					AkunPembayaranSiswa.class);
		}

		PembayaranSiswa pembayaranSiswa = null;
		Transaction tx = null;
		try {
			pembayaranSiswa = new PembayaranSiswa();
			pembayaranSiswa.setSiswa(tagihanD.getSiswa());
			pembayaranSiswa.setCalonSiswa(tagihanD.getCalonSiswa());
			pembayaranSiswa.setTanggal(tanggal);
			pembayaranSiswa.setKeterangan(tagihanD.getInformasi());
			pembayaranSiswa.setAkunPembayaranSiswa(akunPembayaranSiswa);
			pembayaranSiswa.setNominal(total);
			pembayaranSiswa.setDariTabungan(tabungan);
			// bayar via deposit: tambahanDeposit = 0 agar tidak membuat record DepositSiswa baru
			pembayaranSiswa.setTambahanDeposit(0.0);
			pembayaranSiswa.setValidator(bank);
			pembayaranSiswa.setDariTabunganManual(tabungan);
			// Reload tbmuser dari session aktif (attached) agar FK valid saat session.save().
			// Jika user tidak ada di DB → null → kolom nullable → tidak ada FK violation.
			if (tbmuser != null && tbmuser.getUserId() != null) {
				Tbmuser attachedUser = (Tbmuser) session.get(Tbmuser.class, tbmuser.getUserId());
				pembayaranSiswa.setValidatorUser(attachedUser);
			}

			tx = session.beginTransaction();
			if (pembayaranSiswa.getId() == null)
				session.save(pembayaranSiswa);
			else
				Common.refreshSaveOrUpdate(session, pembayaranSiswa);
			tx.commit();

			tx = session.beginTransaction();
			pembayaranSiswa.saveOrUpdateDeposit(session);
			tx.commit();

			for (Tagihan tagihan : tagihans) {
				// Reload dalam session baru agar tidak ada masalah detached entity
				Tagihan tagihanPersisten = (Tagihan) session.get(Tagihan.class, tagihan.getId());
				if (tagihanPersisten == null) continue;
				NominalBiaya nominalBiaya = tagihanPersisten.getNominalBiaya();
				if (nominalBiaya != null) {
					Double nominal = tagihanPersisten.getNominal() - tagihanPersisten.getDiskon() + tagihanPersisten.getDenda();

					PembayaranSiswaDetail pembayaranSiswaDetail = new PembayaranSiswaDetail(tagihanPersisten);
					pembayaranSiswaDetail.setItemBiayaSekolah(nominalBiaya.getItemBiayaSekolah());
					pembayaranSiswaDetail.setNominalBiaya(nominalBiaya);
					pembayaranSiswaDetail.setNominal(nominal);
					pembayaranSiswaDetail.setNominalManual(nominal);
					pembayaranSiswaDetail.setPembayaranSiswa(pembayaranSiswa);

					tx = session.beginTransaction();
					session.save(pembayaranSiswaDetail);
					tx.commit();

					tagihanPersisten.setPembayaranSiswaDetail(pembayaranSiswaDetail);

					tx = session.beginTransaction();
					session.update(tagihanPersisten);
					tx.commit();

					if (tagihanPersisten.getDiskonSiswa() != null && !tagihanPersisten.getDiskonSiswa().getMemotongTagihan()) {
						DaftarPengajuanTransfer.simpanDiskonPembayaran(tagihanPersisten);
					}
				}
			}

			PembayaranSiswaUtil.cetakVa(pembayaranSiswa, null, cetak);

		} catch (Exception e) {
			if (tx != null && tx.isActive())
				tx.rollback();
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:513");
		}

		return pembayaranSiswa;
	}

	/**
	 * Mencari VA yang belum terbayar untuk sekumpulan tagihan tertentu, atau membuatkannya bila
	 * belum ada. Penolong khusus {@link #ambilByNisAja(String, Double)}.
	 *
	 * <p>VA baru dibuat dengan nomor {@link Va} berformat {@code "<kode>-<tag>"}, membawa siswa dan
	 * tahun ajaran dari tagihan pertama, id perguruan tinggi sekolahnya, serta {@code amount} =
	 * nilai setoran. Kolom {@code cicilan} diisi dengan {@code tag}, yaitu daftar id tagihan yang
	 * dicakup.</p>
	 *
	 * <p><b>Kuirk yang perlu diketahui:</b> kriteria pencarian memakai properti
	 * {@code "refTagihan"}, sedangkan entity ini <b>tidak memiliki properti bernama itu</b> (yang
	 * ada {@code cicilan}). Akibatnya Hibernate melempar {@code QueryException} pada baris pertama,
	 * sebelum blok {@code try} — exception itu ditangkap oleh {@code try} milik
	 * {@link #ambilByNisAja(String, Double)} sehingga jalur pembuatan VA otomatis dari NIS praktis
	 * tidak pernah berhasil. Dicatat apa adanya di sini; jangan diperbaiki tanpa pengujian, karena
	 * perbaikan justru MENGAKTIFKAN pembuatan VA otomatis yang selama ini mati.</p>
	 *
	 * @param sessionBaru session Hibernate yang dibuka pemanggil
	 * @param kode        kode dasar (NIS siswa) untuk membentuk nomor VA
	 * @param nilai       nominal setoran
	 * @param tag         daftar id tagihan yang dicakup, dipisah koma/titik koma
	 * @param tagihan     tagihan-tagihan terkait; VA hanya dibuat bila minimal satu diberikan
	 * @return VA yang ditemukan atau baru dibuat; {@code null} bila keduanya gagal
	 */
	private static VirtualAccountBank buatAtauChekTagihan(Session sessionBaru, String kode, Double nilai, String tag,
			Tagihan... tagihan) {
		VirtualAccountBank virtualAccountBank = (VirtualAccountBank) sessionBaru
				.createCriteria(VirtualAccountBank.class).add(Restrictions.eq("refTagihan", tag))
				.add(Restrictions.isNull("waktuBayar")).setMaxResults(1).uniqueResult();

		if (virtualAccountBank == null && tagihan != null && tagihan.length > 0) {
			Transaction tx = null;
			try {
				String code = kode + "-" + tag;
				Va va = new Va();
				va.setKode(code);
				tx = sessionBaru.beginTransaction();
				sessionBaru.save(va);
				tx.commit();

				virtualAccountBank = new VirtualAccountBank();
				virtualAccountBank.setCicilan(tag);
				virtualAccountBank.setVa(va);
				virtualAccountBank.setSiswa(tagihan[0].getSiswa());
				virtualAccountBank.setTahunAkademik(tagihan[0].getTahunAjaran());
				virtualAccountBank.setPt(tagihan[0].getSekolah().getPerguruanTinggi() == null ? null
						: tagihan[0].getSekolah().getPerguruanTinggi().getId());
				virtualAccountBank.setAmount(nilai);

				tx = sessionBaru.beginTransaction();
				sessionBaru.save(virtualAccountBank);
				tx.commit();
			} catch (Exception e) {
				if (tx != null && tx.isActive())
					tx.rollback();
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:550");
			}
		}
		return virtualAccountBank;
	}

	/**
	 * Jalur cadangan pencarian VA: bila nomor yang disetor bank ternyata bukan nomor VA melainkan
	 * <b>NIS siswa</b>, coba tebak tagihan mana yang dimaksud dari nominal setorannya.
	 *
	 * <p>Dipanggil oleh {@link #ambilVa(String, Double, BankHost, Criterion)} sebagai upaya terakhir
	 * ketika pencarian berdasarkan {@code kode} tidak menghasilkan apa pun dan nominal diketahui.</p>
	 *
	 * <p>Heuristiknya: untuk {@code i} = 1..4, cari {@code i} tagihan siswa yang belum terbayar
	 * (belum punya {@code pembayaranSiswaDetail}, atau tenggat pembayarannya sudah lewat) dengan
	 * nominal masing-masing {@code nilai / i}, diurutkan menaik menurut {@code tahunbulan}. Iterasi
	 * pertama yang menghasilkan tepat {@code i} tagihan dipakai, lalu VA dibuat/diambil lewat
	 * {@link #buatAtauChekTagihan(Session, String, Double, String, Tagihan...)}.</p>
	 *
	 * <p><b>Kuirk delimiter:</b> daftar id tagihan dirangkai dengan koma, KECUALI pada kasus
	 * {@code i == 4} di posisi ketiga yang memakai titik koma. Ini dipertahankan sengaja agar
	 * sama persis dengan bentuk data lama di produksi.</p>
	 *
	 * <p><b>Efek samping:</b> membuka session Hibernate baru sendiri
	 * ({@code HibernateUtil.openSession()}) dan menutupnya di {@code finally}, serta dapat menyimpan
	 * baris {@link Va} dan {@code VirtualAccountBank} baru. Semua kegagalan ditelan (hanya
	 * di-{@code printStackTrace} + dicatat audit) dan menghasilkan {@code null}.</p>
	 *
	 * @param kode  NIS siswa
	 * @param nilai nominal setoran yang diterima bank
	 * @return VA yang cocok/baru dibuat, atau {@code null} bila siswa tidak ketemu atau tidak ada
	 *         kombinasi tagihan yang pas
	 * @see #buatAtauChekTagihan(Session, String, Double, String, Tagihan...)
	 */
	public static VirtualAccountBank ambilByNisAja(String kode, Double nilai) {
		VirtualAccountBank virtualAccountBank = null;
		Siswa siswa = ConstantValues.ambilByNis(kode);

		if (siswa != null) {
			Session sessionBaru = null;
			try {
				sessionBaru = HibernateUtil.openSession();

				// REFACTOR: Loop 1 to 4 to dynamically divide and search instead of
				// copy-pasting 4 blocks
				for (int i = 1; i <= 4; i++) {
					@SuppressWarnings("unchecked")
					List<Tagihan> tagihanList = sessionBaru.createCriteria(Tagihan.class)
							.add(Restrictions.eq("siswa", siswa))
							.add(Restrictions.or(
									Restrictions.and(Restrictions.isNotNull("pembayaranBerakhirPada"),
											Restrictions.sqlRestriction(
													"this_.pembayaran_berakhir_pada < CURRENT_TIMESTAMP")),
									Restrictions.isNull("pembayaranSiswaDetail")))
							.add(Restrictions.eq("nominal", nilai / i)).addOrder(Order.asc("tahunbulan"))
							.setMaxResults(i).list();

					if (tagihanList != null && tagihanList.size() == i) {
						StringBuilder tagBuilder = new StringBuilder();
						for (int j = 0; j < tagihanList.size(); j++) {
							tagBuilder.append(tagihanList.get(j).getId());
							// Mempertahankan quirk delimiter asli (koma atau titik koma)
							if (j < tagihanList.size() - 1) {
								tagBuilder.append((i == 4 && j == 2) ? ";" : ",");
							}
						}

						Tagihan[] tagArray = tagihanList.toArray(new Tagihan[0]);
						virtualAccountBank = buatAtauChekTagihan(sessionBaru, kode, nilai, tagBuilder.toString(),
								tagArray);
						break; // Stop loop once match is found and created
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:596");
			} finally {
				if (sessionBaru != null) {
					KegiatanPersistenceHelper.closeNativeSession(sessionBaru);
				}
			}
		}
		return virtualAccountBank;
	}

	/**
	 * Memaksa lima relasi lazy yang paling sering dipakai pemanggil agar ter-inisialisasi
	 * <b>selagi session masih terbuka</b>.
	 *
	 * <p>{@link #ambilVa(String, Double, BankHost, Criterion)} dan
	 * {@link #ambilLink(String, BankHost)} menutup session-nya sendiri sebelum mengembalikan
	 * entity. Tanpa langkah ini, servlet callback yang kemudian membaca {@code getBankHost()},
	 * {@code getJenisKegiatan()}, {@code getMahasiswa()}, {@code getBiodataCalonMahasiswa()}, atau
	 * {@code getJadwalPembayaran()} akan menemui {@code LazyInitializationException}.</p>
	 *
	 * <p>Caranya sederhana: memanggil {@code getId()} pada tiap relasi supaya proxy-nya terhidrasi.
	 * Setiap relasi dibungkus {@code try} sendiri agar satu relasi yang bermasalah (mis. baris
	 * induk sudah dihapus) tidak menggagalkan yang lain.</p>
	 *
	 * @param va entity yang akan diserahkan ke pemanggil; {@code null} diabaikan
	 */
	private static void initialiseVirtualAccountForCaller(VirtualAccountBank va) {
		if (va == null) {
			return;
		}
		try {
			if (va.getBankHost() != null) {
				va.getBankHost().getId();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:614");
		}
		try {
			if (va.getJenisKegiatan() != null) {
				va.getJenisKegiatan().getId();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:620");
		}
		try {
			if (va.getMahasiswa() != null) {
				va.getMahasiswa().getId();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:626");
		}
		try {
			if (va.getBiodataCalonMahasiswa() != null) {
				va.getBiodataCalonMahasiswa().getId();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:632");
		}
		try {
			if (va.getJadwalPembayaran() != null) {
				va.getJadwalPembayaran().getId();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:638");
		}
		/* Callback multi-tenant Online BMT memerlukan pemilik sekolah dan kanal sesudah
		 * session lookup ditutup. Hidrasi sampai sekolah agar resolver credential tidak
		 * jatuh kembali ke konfigurasi global hanya karena proxy sudah detached. */
		try {
			if (va.getSiswa() != null) {
				va.getSiswa().getId();
				if (va.getSiswa().getSekolah() != null) va.getSiswa().getSekolah().getId();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "VirtualAccountBank.initialise.siswaSekolah"); }
		try {
			if (va.getCalonSiswa() != null) {
				va.getCalonSiswa().getId();
				if (va.getCalonSiswa().getSekolah() != null) va.getCalonSiswa().getSekolah().getId();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "VirtualAccountBank.initialise.calonSiswaSekolah"); }
		try {
			if (va.getAnggotaKoperasi() != null) va.getAnggotaKoperasi().getId();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "VirtualAccountBank.initialise.anggotaKoperasi"); }
		try {
			if (va.getCaraPembayaranKoperasi() != null) {
				va.getCaraPembayaranKoperasi().getId();
				if (va.getCaraPembayaranKoperasi().getKoperasi() != null) {
					va.getCaraPembayaranKoperasi().getKoperasi().getId();
					if (va.getCaraPembayaranKoperasi().getKoperasi().getYayasan() != null) {
						va.getCaraPembayaranKoperasi().getKoperasi().getYayasan().getId();
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "VirtualAccountBank.initialise.caraPembayaranKoperasi"); }
		try {
			if (va.getKanalPembayaran() != null) {
				va.getKanalPembayaran().getId();
				if (va.getKanalPembayaran().getSekolah() != null) va.getKanalPembayaran().getSekolah().getId();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "VirtualAccountBank.initialise.kanalPembayaran"); }
	}

	/**
	 * Pintasan {@link #ambilVa(String, Double, BankHost, Criterion)} tanpa nominal dan tanpa
	 * kriteria tambahan — dipakai pada inquiry yang hanya membawa nomor VA.
	 *
	 * @param kode     nomor VA yang dikirim bank
	 * @param bankHost host bank pemanggil; boleh {@code null}
	 * @return VA yang cocok atau {@code null}
	 * @see #ambilVa(String, Double, BankHost, Criterion)
	 */
	public static VirtualAccountBank ambilVa(String kode, BankHost bankHost) {
		return ambilVa(kode, null, bankHost, null);
	}

	/**
	 * Pintasan {@link #ambilVa(String, Double, BankHost, Criterion)} tanpa kriteria tambahan, tetapi
	 * dengan nominal sehingga jalur cadangan {@link #ambilByNisAja(String, Double)} tetap aktif.
	 *
	 * @param kode     nomor VA yang dikirim bank
	 * @param nominalP nominal setoran; dipakai bila pencarian berdasarkan kode gagal
	 * @param bankHost host bank pemanggil; boleh {@code null}
	 * @return VA yang cocok atau {@code null}
	 * @see #ambilVa(String, Double, BankHost, Criterion)
	 */
	public static VirtualAccountBank ambilVa(String kode, Double nominalP, BankHost bankHost) {
		return ambilVa(kode, nominalP, bankHost, null);
	}

	/**
	 * Mencari satu baris VA berdasarkan nomornya — pintu masuk utama semua servlet callback bank.
	 *
	 * <h4>Urutan pencarian</h4>
	 * <ol>
	 *   <li>VA dengan {@code kode} sama, yang {@code bankHost}-nya kosong ATAU sama dengan host
	 *       pemanggil (VA "netral" boleh dibayar lewat bank mana saja), diambil yang id-nya
	 *       terbesar;</li>
	 *   <li>bila belum ketemu dan {@code crit} diberikan, ulangi pencarian hanya dengan kriteria
	 *       khusus bank tersebut (mis. kolom {@code link} atau format nomor tersendiri);</li>
	 *   <li>bila masih kosong dan {@code nominalP} lebih dari 0,1, coba tebak lewat
	 *       {@link #ambilByNisAja(String, Double)}.</li>
	 * </ol>
	 *
	 * <h4>Efek samping (penting)</h4>
	 * <p>Method ini <b>bukan sekadar query</b>. Bila VA yang ditemukan sudah punya penanda hasil
	 * posting ({@code kegiatan} atau {@code pembayaran}) tetapi {@code waktuBayar} masih kosong —
	 * gejala data setengah jadi dari callback yang terputus — method akan <b>memperbaiki baris itu
	 * di database</b>: mengambil tanggal dari {@link CicilanPembayaran} yang mereferensi VA ini,
	 * mengisi {@code bankHost} bila masih kosong, membuat baris {@link Va} bila belum ada, lalu
	 * menyimpan VA tersebut.</p>
	 *
	 * <p>Session Hibernate dibuka sendiri dan <b>ditutup</b> di blok {@code finally}
	 * ({@code KegiatanPersistenceHelper.closeNativeSession}). Karena itu relasi yang dibutuhkan
	 * pemanggil sudah dihidrasi lebih dulu oleh
	 * {@link #initialiseVirtualAccountForCaller(VirtualAccountBank)}; relasi lain akan detached.</p>
	 *
	 * @param kode     nomor VA yang dikirim bank
	 * @param nominalP nominal setoran; {@code null} mematikan jalur tebakan lewat NIS
	 * @param bankHost host bank pemanggil; boleh {@code null}
	 * @param crit     kriteria pencarian alternatif khusus bank tertentu; boleh {@code null}
	 * @return VA yang cocok, atau {@code null} bila tidak ada
	 * @see #ambilLink(String, BankHost)
	 */
	public static VirtualAccountBank ambilVa(String kode, Double nominalP, BankHost bankHost, Criterion crit) {
		Session sessionBaru = null;
		VirtualAccountBank virtualAccountBankNtt = null;
		Transaction tx = null;
		try {
			sessionBaru = HibernateUtil.openSession();
			virtualAccountBankNtt = (VirtualAccountBank) sessionBaru.createCriteria(VirtualAccountBank.class)
					.add(Restrictions.or(Restrictions.isNull("bankHost"), Restrictions.eq("bankHost", bankHost)))
					.add(Restrictions.eq("kode", kode)).addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();

			if (virtualAccountBankNtt == null && crit != null) {
				virtualAccountBankNtt = (VirtualAccountBank) sessionBaru.createCriteria(VirtualAccountBank.class)
						.add(crit).addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
			}

			if (virtualAccountBankNtt != null) {
				initialiseVirtualAccountForCaller(virtualAccountBankNtt);
			}

			if (virtualAccountBankNtt != null
					&& (virtualAccountBankNtt.getKegiatan() != null || virtualAccountBankNtt.getPembayaran() != null)
					&& virtualAccountBankNtt.getWaktuBayar() == null) {

				Date tanggal = (Date) sessionBaru.createCriteria(CicilanPembayaran.class)
						.add(Restrictions.eq("refVa", virtualAccountBankNtt.getId()))
						.setProjection(Projections.property("tanggal")).setMaxResults(1).uniqueResult();
				virtualAccountBankNtt.setWaktuBayar(tanggal);

				if (bankHost != null && virtualAccountBankNtt.getBankHost() == null) {
					virtualAccountBankNtt.setBankHost(bankHost);
				}

				if (virtualAccountBankNtt.getVa() == null) {
					Va va = new Va();
					va.setKode(virtualAccountBankNtt.getKode());
					tx = sessionBaru.beginTransaction();
					sessionBaru.save(va);
					tx.commit();
					virtualAccountBankNtt.setVa(va);
				}

				tx = sessionBaru.beginTransaction();
				sessionBaru.update(virtualAccountBankNtt);
				tx.commit();
			}
		} catch (Exception e) {
			if (tx != null && tx.isActive())
				tx.rollback();
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:698");
		} finally {
			if (sessionBaru != null) {
				KegiatanPersistenceHelper.closeNativeSession(sessionBaru);
			}
		}

		try {
			if (virtualAccountBankNtt == null && nominalP != null && nominalP > 0.1) {
				virtualAccountBankNtt = ambilByNisAja(kode, nominalP);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:710");
		}

		return virtualAccountBankNtt;
	}

	/**
	 * Varian {@link #ambilVa(String, Double, BankHost, Criterion)} yang mencari VA berdasarkan
	 * <b>tautan pembayaran</b> ({@code link}), bukan nomor VA.
	 *
	 * <p>Dipakai gateway berbasis <i>payment link</i> (mis. e-Smartlink, Flip) yang mengembalikan
	 * URL unik alih-alih nomor rekening virtual. Pencocokan memakai {@code ilike} dengan
	 * {@link MatchMode#END} — cukup potongan akhir tautan yang sama — dan tetap menghormati aturan
	 * "{@code bankHost} kosong atau sama".</p>
	 *
	 * <p>Efek sampingnya identik dengan {@code ambilVa}: perbaikan otomatis baris yang sudah punya
	 * penanda posting tetapi belum punya {@code waktuBayar}, pembuatan baris {@link Va} bila belum
	 * ada, hidrasi relasi lewat
	 * {@link #initialiseVirtualAccountForCaller(VirtualAccountBank)}, dan penutupan session sendiri
	 * di {@code finally}. Bedanya, di sini {@code bankHost} tidak pernah diisikan ke VA.</p>
	 *
	 * @param link     potongan akhir tautan pembayaran
	 * @param bankHost host bank pemanggil; boleh {@code null}
	 * @return VA yang cocok, atau {@code null} bila tidak ada
	 */
	public static VirtualAccountBank ambilLink(String link, BankHost bankHost) {
		Session sessionBaru = null;
		VirtualAccountBank virtualAccountBankNtt = null;
		Transaction tx = null;
		try {
			sessionBaru = HibernateUtil.openSession();
			virtualAccountBankNtt = (VirtualAccountBank) sessionBaru.createCriteria(VirtualAccountBank.class)
					.add(Restrictions.or(Restrictions.isNull("bankHost"), Restrictions.eq("bankHost", bankHost)))
					.add(Restrictions.ilike("link", link, MatchMode.END)).addOrder(Order.desc("id")).setMaxResults(1)
					.uniqueResult();

			if (virtualAccountBankNtt != null) {
				initialiseVirtualAccountForCaller(virtualAccountBankNtt);
			}

			if (virtualAccountBankNtt != null
					&& (virtualAccountBankNtt.getKegiatan() != null || virtualAccountBankNtt.getPembayaran() != null)
					&& virtualAccountBankNtt.getWaktuBayar() == null) {

				Date tanggal = (Date) sessionBaru.createCriteria(CicilanPembayaran.class)
						.add(Restrictions.eq("refVa", virtualAccountBankNtt.getId()))
						.setProjection(Projections.property("tanggal")).setMaxResults(1).uniqueResult();
				virtualAccountBankNtt.setWaktuBayar(tanggal);

				if (virtualAccountBankNtt.getVa() == null) {
					Va va = new Va();
					va.setKode(virtualAccountBankNtt.getKode());
					tx = sessionBaru.beginTransaction();
					sessionBaru.save(va);
					tx.commit();
					virtualAccountBankNtt.setVa(va);
				}

				tx = sessionBaru.beginTransaction();
				sessionBaru.update(virtualAccountBankNtt);
				tx.commit();
			}
		} catch (Exception e) {
			if (tx != null && tx.isActive())
				tx.rollback();
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:756");
		} finally {
			if (sessionBaru != null) {
				KegiatanPersistenceHelper.closeNativeSession(sessionBaru);
			}
		}
		return virtualAccountBankNtt;
	}

	// Metode bayarVa terlalu besar, sudah saya kelompokkan Transaction
	// boundaries-nya
	/**
	 * Jalur posting pembayaran VA untuk <b>mahasiswa / calon mahasiswa</b>: mengubah satu callback
	 * bank menjadi {@link Kegiatan} (dokumen pembayaran per semester) beserta rincian
	 * {@link CicilanPembayaran}.
	 *
	 * <p>Pendamping method ini untuk jalur sekolah adalah
	 * {@link #bayarSiswa(VirtualAccountBank, Session, Date, String, boolean, String, boolean)}.
	 * Dipanggil oleh servlet callback tiap bank dan oleh {@code PembayaranGatewayHelper}.</p>
	 *
	 * <h4>Penjagaan duplikat</h4>
	 * <ol>
	 *   <li>id VA sudah ada di cache {@link #sukses} &rarr; langsung keluar;</li>
	 *   <li>bila VA punya total dan token {@code cicilan}, jumlahkan {@code nilai} seluruh
	 *       {@link CicilanPembayaran} yang {@code refVa}-nya menunjuk VA ini. Bila jumlah itu sudah
	 *       &ge; total VA, callback dianggap duplikat. Sengaja memakai SUM, bukan COUNT: bila
	 *       sebagian cicilan pernah terhapus, COUNT tetap &gt; 0 padahal data belum lengkap,
	 *       sehingga sisa cicilan yang kurang tetap perlu di-insert.</li>
	 * </ol>
	 *
	 * <h4>Pembentukan dokumen</h4>
	 * <p>Kegiatan dicari berurutan: lewat kolom {@code kegiatan} pada VA, lalu lewat kombinasi
	 * (mahasiswa/calon mahasiswa + jenis kegiatan + semester); bila tetap nihil, dibuat baru.
	 * Kegiatan diisi dari VA (nama, amount, tahun akademik, semester, jadwal pembayaran,
	 * {@code kodeUnikLain}), ditandai {@code validated = 1}, dan validatornya diisi nama bank.</p>
	 *
	 * <p>Token pada kolom {@code cicilan} kemudian diurai per jenis:</p>
	 * <ul>
	 *   <li><b>angka polos atau {@code "Bulanan-<id>-...-<nilai>"}</b> &rarr; satu
	 *       {@link CicilanPembayaran} yang terhubung ke {@code PengaturanPembayaranBulanan};</li>
	 *   <li><b>{@code "Item-<idItemBiaya>-<nilai>-<bayarKe>-<idDetailBiaya>"}</b> &rarr; cicilan
	 *       lepas yang terhubung ke {@link ItemBiaya} + {@link DetailBiaya};</li>
	 *   <li><b>{@code "Keranjang-<idKegiatanTemporary>"}</b> &rarr; jalur keranjang belanja: membuka
	 *       <b>session Hibernate lokal tersendiri</b>, memindahkan seluruh cicilan milik
	 *       {@code KegiatanTemporary} ke {@link Kegiatan} yang sesungguhnya, lalu menghitung ulang
	 *       {@code amount} dan {@code amountTerhutang} kegiatan tersebut. Session lokal ini ditutup
	 *       di {@code finally}.</li>
	 * </ul>
	 * <p>Setiap cicilan memakai referensi unik {@code "ntt-<idKegiatan>-<token>-<idVa>"} sehingga
	 * pemrosesan ulang meng-<i>update</i> baris yang sama, bukan menambah baris baru (idempoten).
	 * Item biaya dengan penghitungan {@code DIKALI_NILAI_MINUS} nilainya dibalik menjadi negatif.
	 * Jenis pembayaran diambil dari {@code bankHost}, jatuh ke {@code ConstantValues.TUNAI} bila
	 * tidak tersedia.</p>
	 *
	 * <h4>Penutup</h4>
	 * <p>Total dan denda kegiatan dihitung ulang lewat
	 * {@code PembayaranUtil.getTotalDanDendaFromCicilan}, {@code amountTerhutang} diisi selisih
	 * tagihan seharusnya dengan yang sudah dibayar, kegiatan disimpan, cache cicilannya disegarkan,
	 * lalu {@link #updateVa(VirtualAccountBank, Date, Kegiatan, String, String)} menandai VA lunas.</p>
	 *
	 * <p><b>Perhatian:</b> seluruh proses hanya berjalan bila {@code detailbiaya} ATAU {@code cicilan}
	 * tidak kosong. Semua kegagalan ditelan di level method (rollback + audit), jadi pemanggil tidak
	 * pernah menerima exception — periksa kolom hasil posting pada VA untuk memastikan keberhasilan.</p>
	 *
	 * @param virtualAccountBankNtt baris VA yang sedang dibayar (dimutasi oleh method ini)
	 * @param tanggal               waktu pembayaran menurut bank
	 * @param data                  payload notifikasi mentah, disimpan ke kolom {@code notif}
	 * @param session               session Hibernate aktif milik pemanggil
	 */
	@SuppressWarnings("unchecked")
	public static void bayarVa(final VirtualAccountBank virtualAccountBankNtt, Date tanggal, String data,
			Session session) {
		Transaction tx = null;
		try {
			// Cek in-memory sukses set (tidak perlu query DB, paling cepat).
			if (virtualAccountBankNtt.getId() != null && sukses.contains(virtualAccountBankNtt.getId())) {
				System.out.println("Pembayaran VA " + virtualAccountBankNtt + " sudah pernah diproses (cache). Proses mahasiswa dilewati.");
				return;
			}
			// Cek langsung ke CicilanPembayaran berdasarkan refVa = VAB.id —
			// tidak andalkan kolom kegiatan di VAB yang bisa basi (mis. saat Cek Ulang
			// atau data tidak konsisten). Jika belum ada → lanjut insert.
			// Kode di bawah sudah idempoten: Kegiatan→refreshSaveOrUpdate;
			// CicilanPembayaran→cek ref sebelum insert, update jika sudah ada.
			Double totalVaBayarVa = virtualAccountBankNtt.getTotal();
			boolean adaCicilan = virtualAccountBankNtt.getCicilan() != null
					&& !virtualAccountBankNtt.getCicilan().trim().isEmpty();
			if (totalVaBayarVa != null && totalVaBayarVa > 0.1
					&& virtualAccountBankNtt.getId() != null && adaCicilan) {
				// Bandingkan SUM(nilai) cicilan yang ada dengan total VA —
				// count saja tidak cukup karena mungkin hanya sebagian cicilan yang terhapus
				// (mis. dari 2 cicilan, 1 masih ada → count > 0 tapi data belum lengkap).
				Double sumCicilanVa = (Double) session.createCriteria(CicilanPembayaran.class)
						.setProjection(Projections.sum("nilai"))
						.add(Restrictions.eq("refVa", virtualAccountBankNtt.getId()))
						.uniqueResult();
				if (sumCicilanVa != null) {
					long sumBulat = Math.round(sumCicilanVa);
					long totalBulat = Math.round(totalVaBayarVa);
					if (sumBulat >= totalBulat) {
						System.out.println("Pembayaran VA " + virtualAccountBankNtt
								+ " sum CicilanPembayaran (" + sumBulat + ") >= total (" + totalBulat
								+ "). Proses mahasiswa dilewati.");
						return;
					}
					System.out.println("Pembayaran VA " + virtualAccountBankNtt
							+ " CicilanPembayaran belum lengkap (sum=" + sumBulat
							+ " < total=" + totalBulat + "). Lanjut insert cicilan yang kurang.");
				}
			}

			JenisKegiatan jenisKegiatan = virtualAccountBankNtt.getJenisKegiatan();
			Mahasiswa mahasiswa = virtualAccountBankNtt.getMahasiswa();
			BiodataCalonMahasiswa biodataCalonMahasiswa = virtualAccountBankNtt.getBiodataCalonMahasiswa();
			Integer semester = virtualAccountBankNtt.getSemester();
			String nama = mahasiswa == null ? biodataCalonMahasiswa.getNama() : mahasiswa.getNama();

			if (!virtualAccountBankNtt.getDetailbiaya().isEmpty() || !virtualAccountBankNtt.getCicilan().isEmpty()) {

				Kegiatan kegiatan = (Kegiatan) (virtualAccountBankNtt.getKegiatan() == null ? null
						: session.createCriteria(Kegiatan.class)
								.add(Restrictions.idEq(virtualAccountBankNtt.getKegiatan())).uniqueResult());

				if (kegiatan == null || kegiatan.getId() == null) {
					kegiatan = (Kegiatan) session.createCriteria(Kegiatan.class).addOrder(Order.asc("id"))
							.add(biodataCalonMahasiswa != null
									? Restrictions.eq("calonMahasiswa", biodataCalonMahasiswa)
									: Restrictions.eq("mahasiswa", mahasiswa))
							.add(Restrictions.eq("jenisKegiatan", virtualAccountBankNtt.getJenisKegiatan()))
							.add(Restrictions.eq("semster", semester)).setMaxResults(1).uniqueResult();
				}

				if (kegiatan == null || kegiatan.getId() == null)
					kegiatan = new Kegiatan();

				kegiatan.setKodeUnikLain(virtualAccountBankNtt.getKodeUnikLain());
				kegiatan.setJadwalPembayaran(virtualAccountBankNtt.getJadwalPembayaran());
				kegiatan.setNama(nama);
				kegiatan.setAmount(virtualAccountBankNtt.getTotal());
				kegiatan.setCalonMahasiswa(biodataCalonMahasiswa);
				kegiatan.setMahasiswa(mahasiswa);
				kegiatan.setTahunAkademik(virtualAccountBankNtt.getTahunAkademik());
				kegiatan.setSemster(semester);
				kegiatan.setJenisKegiatan(jenisKegiatan);
				kegiatan.setTanggal(tanggal);
				kegiatan.setValidated(1);
				kegiatan.setValidator(virtualAccountBankNtt.getBank());

				tx = session.beginTransaction();
				Common.refreshSaveOrUpdate(session, kegiatan);
				tx.commit();

				List<Long> detailBiayasId = new ArrayList<Long>();
				for (String id : StringUtils.split(virtualAccountBankNtt.getDetailbiaya(), ",")) {
					try {
						detailBiayasId.add(Long.parseLong(id.trim()));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:854");
					}
				}

				Collection<DetailBiaya> detailBiayas = session.createCriteria(DetailBiaya.class)
						.add(detailBiayasId.isEmpty() ? Restrictions.sqlRestriction("false")
								: Restrictions.in("id", detailBiayasId))
						.list();

				Double nilaiBiayaHarusDiBayars = 0.0;
				for (DetailBiaya detailBiaya : detailBiayas) {
					nilaiBiayaHarusDiBayars += Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya);
				}

				if (virtualAccountBankNtt.getCicilan() != null && !virtualAccountBankNtt.getCicilan().isEmpty()) {
					for (String idPemBul : StringUtils.split(virtualAccountBankNtt.getCicilan(), ",")) {

						if (Common.isNumber(idPemBul) || idPemBul.startsWith("Bulanan-")) {
							Long idToSearch = Common.isNumber(idPemBul) ? Long.parseLong(idPemBul)
									: Long.parseLong(idPemBul.split("-")[1]);
							PengaturanPembayaranBulanan ppBln = (PengaturanPembayaranBulanan) session
									.createCriteria(PengaturanPembayaranBulanan.class)
									.add(Restrictions.idEq(idToSearch)).uniqueResult();

							if (ppBln != null) {
								String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
										+ virtualAccountBankNtt.getId();
								ItemBiaya itemBiaya = ppBln.getDetailBiaya().getItemBiaya();
								Double subtotal = 0.0;
								try {
									String[] spl = idPemBul.split("-");
									subtotal = Double.parseDouble(spl[spl.length - 1]);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:886");
								}

								if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS))
									subtotal = 0.0 - subtotal;

								CicilanPembayaran cp = (CicilanPembayaran) session
										.createCriteria(CicilanPembayaran.class).add(Restrictions.eq("ref", ref))
										.setMaxResults(1).uniqueResult();

								if (cp == null)
									cp = new CicilanPembayaran(ppBln.getDetailBiaya());

								cp.setRef(ref);
								cp.setValidator(virtualAccountBankNtt.getBank());
								cp.setKegiatan(kegiatan);
								cp.setItemBiaya(itemBiaya);
								cp.setPengaturanPembayaranBulanan(ppBln);
								cp.setRefVa(virtualAccountBankNtt.getId());
								cp.setNilai(subtotal);
								cp.setNilaiAsli(subtotal);
								cp.setTanggal(tanggal);
								cp.setJenisPembayaran(virtualAccountBankNtt.getBankHost() == null
										|| virtualAccountBankNtt.getBankHost().getJenisPembayaran() == null
												? ConstantValues.TUNAI
												: virtualAccountBankNtt.getBankHost().getJenisPembayaran());
								cp.setDenda(0.0);

								tx = session.beginTransaction();
								if (cp.getId() == null)
									session.save(cp);
								else
									Common.refreshUpdate(session, cp);
								tx.commit();
							}
						} else if (idPemBul.startsWith("Keranjang-")) {
							// Logic ini cukup panjang dan membuka session lokal, mari kita proteksi
							Session sessionLocalKeg = null;
							Transaction txLocal = null;
							try {
								Long idKeranjang = Long.parseLong(idPemBul.split("-")[1]);
								KegiatanTemporary kegTemp = (KegiatanTemporary) session
										.createCriteria(KegiatanTemporary.class).add(Restrictions.idEq(idKeranjang))
										.uniqueResult();

								if (kegTemp != null) {
									String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
											+ virtualAccountBankNtt.getId();
									sessionLocalKeg = HibernateUtil.openSession();
									kegTemp = (KegiatanTemporary) sessionLocalKeg.createCriteria(KegiatanTemporary.class)
											.add(Restrictions.idEq(idKeranjang)).uniqueResult();
									if (kegTemp == null) {
										continue;
									}

									mahasiswa = kegTemp.getMahasiswa();
									Integer smt = kegTemp.getSemster();
									jenisKegiatan = kegTemp.getJenisKegiatan();
									if (mahasiswa == null || smt == null || jenisKegiatan == null) {
										continue;
									}
									Kegiatan kegLocal = mahasiswa.ambilKegiatans(smt, jenisKegiatan);

									if (kegLocal == null || kegLocal.getId() == null)
										kegLocal = new Kegiatan();
									else
										kegLocal = (Kegiatan) sessionLocalKeg.createCriteria(Kegiatan.class)
												.add(Restrictions.idEq(kegLocal.getId())).uniqueResult();

									kegLocal.setJenisKegiatan(jenisKegiatan);
									kegLocal.setJadwalPembayaran(kegTemp.getJadwalPembayaran());
									kegLocal.setMahasiswa(mahasiswa);
									kegLocal.setSemster(smt);
									kegLocal.setStatusMahasiswa(kegTemp.getStatusMahasiswa());
									kegLocal.setTahunAkademik(kegTemp.getTahunAkademik());
									kegLocal.setTanggal(tanggal);
									kegLocal.setValidated(1);
									kegLocal.setValidator(virtualAccountBankNtt.getBank());
									kegLocal.setKeterangan(kegTemp.getKeterangan());

									txLocal = sessionLocalKeg.beginTransaction();
									Common.refreshSaveOrUpdate(sessionLocalKeg, kegLocal);
									txLocal.commit();
									sinkronkanKegiatanSetelahBayar(kegLocal);

									List<CicilanPembayaran> cps = sessionLocalKeg
											.createCriteria(CicilanPembayaran.class)
											.add(Restrictions.eq("kegiatanTemporary", kegTemp)).list();
									for (CicilanPembayaran cp : cps) {
										cp.setRef(ref);
										cp.setKegiatan(kegLocal);
										cp.setValidator(virtualAccountBankNtt.getBank());
										cp.setTanggal(tanggal);
										cp.setJenisPembayaran(virtualAccountBankNtt.getBankHost() == null
												|| virtualAccountBankNtt.getBankHost().getJenisPembayaran() == null
														? ConstantValues.TUNAI
														: virtualAccountBankNtt.getBankHost().getJenisPembayaran());

										txLocal = sessionLocalKeg.beginTransaction();
										Common.refreshSaveOrUpdate(sessionLocalKeg, cp);
										txLocal.commit();
									}

									if (kegTemp.getKegiatan() == null || kegTemp.getKegiatan().getId() == null
											|| !kegTemp.getKegiatan().getId().equals(kegLocal.getId())) {
										kegTemp.setKegiatan(kegLocal);
										txLocal = sessionLocalKeg.beginTransaction();
										Common.refreshSaveOrUpdate(sessionLocalKeg, kegTemp);
										txLocal.commit();
									}

									Number jumlah = (Number) sessionLocalKeg.createCriteria(CicilanPembayaran.class)
											.add(Restrictions.isNotNull("itemBiaya"))
											.add(Restrictions.eq("kegiatan", kegLocal))
											.setProjection(Projections.sum("nilai")).uniqueResult();

									Double amountTotal = jumlah == null ? 0.0 : jumlah.doubleValue();
									Map<Long, DetailBiaya> map = new HashMap<Long, DetailBiaya>();
									Collection<DetailBiaya> myDBs = PembayaranUtilHelper
											.getDetailBiayaMahasiswa(mahasiswa, smt, jenisKegiatan, false);
									for (Object o : myDBs) {
										if (o instanceof DetailBiaya)
											map.put(((DetailBiaya) o).getId(), (DetailBiaya) o);
										else if (o instanceof PengaturanPembayaranBulanan)
											map.put(((PengaturanPembayaranBulanan) o).getDetailBiaya().getId(),
													((PengaturanPembayaranBulanan) o).getDetailBiaya());
									}

									Double nHarusDibayar = 0.0;
									for (DetailBiaya db : map.values())
										nHarusDibayar += Kegiatan.ambilJumlahTagihan(kegLocal, db);

									kegLocal.setAmountTerhutang(nHarusDibayar - amountTotal);
									kegLocal.setAmount(amountTotal);
									txLocal = sessionLocalKeg.beginTransaction();
									Common.refreshSaveOrUpdate(sessionLocalKeg, kegLocal);
									txLocal.commit();
									sinkronkanKegiatanSetelahBayar(kegLocal);
								}
							} catch (Exception e) {
								if (txLocal != null && txLocal.isActive())
									txLocal.rollback();
								Common.tampilErrorJikaAdmin(e);
							} finally {
								if (sessionLocalKeg != null) {
									KegiatanPersistenceHelper.closeNativeSession(sessionLocalKeg);
								}
							}
						} else if (idPemBul.startsWith("Item-")) {
							ItemBiaya itemBiaya = (ItemBiaya) ConstantValues.simpleObject(
									session.createCriteria(ItemBiaya.class)
											.add(Restrictions.idEq(Long.parseLong(idPemBul.split("-")[1]))),
									ItemBiaya.class);

							if (itemBiaya != null) {
								String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
										+ virtualAccountBankNtt.getId();
								Double subtotal = 0.0;
								Long detailBiayaId = null;
								try {
									String[] spl = idPemBul.split("-");
									subtotal = Double.parseDouble(spl[2]);
									detailBiayaId = Long.parseLong(spl[4]);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:1049");
								}

								if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS))
									subtotal = 0.0 - subtotal;

								CicilanPembayaran cp = (CicilanPembayaran) session
										.createCriteria(CicilanPembayaran.class).add(Restrictions.eq("ref", ref))
										.setMaxResults(1).uniqueResult();

								if (cp == null)
									cp = new CicilanPembayaran(
											DetailBiaya.muatRefAman(session, detailBiayaId));
								cp.setDetailBiaya(DetailBiaya.muatRefAman(session, detailBiayaId));
								cp.setRef(ref);
								cp.setValidator(virtualAccountBankNtt.getBank());
								cp.setKegiatan(kegiatan);
								cp.setItemBiaya(itemBiaya);
								cp.setPengaturanPembayaranBulanan(null);
								cp.setRefVa(virtualAccountBankNtt.getId());
								cp.setNilai(subtotal);
								cp.setNilaiAsli(subtotal);
								cp.setTanggal(tanggal);
								cp.setJenisPembayaran(virtualAccountBankNtt.getBankHost() == null
										|| virtualAccountBankNtt.getBankHost().getJenisPembayaran() == null
												? ConstantValues.TUNAI
												: virtualAccountBankNtt.getBankHost().getJenisPembayaran());
								cp.setDenda(0.0);

								tx = session.beginTransaction();
								if (cp.getId() == null)
									session.save(cp);
								else
									Common.refreshUpdate(session, cp);
								tx.commit();
							}
						}
					}
				}

				Double[] d = PembayaranUtil.getInstance().getTotalDanDendaFromCicilan(session, kegiatan);
				kegiatan.setDenda(d[1]);
				kegiatan.setAmountTerhutang(nilaiBiayaHarusDiBayars - (d[0] - d[1]));
				kegiatan.setAmount(d[0] > 0.1 ? d[0] : virtualAccountBankNtt.getTotal());
				kegiatan.setValidator(virtualAccountBankNtt.getBank());

				tx = session.beginTransaction();
				Common.refreshUpdate(session, kegiatan);
					tx.commit();
					sinkronkanKegiatanSetelahBayar(kegiatan);

				VirtualAccountBank.updateVa(virtualAccountBankNtt, tanggal, kegiatan, data,
						virtualAccountBankNtt.getBank());
			}
		} catch (Exception e) {
			if (tx != null && tx.isActive())
				tx.rollback();
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:1106");
		}
	}


	/**
	 * Menandai satu VA sebagai lunas dan menjalankan tindak lanjut pasca-pembayaran mahasiswa.
	 *
	 * <p>Dipanggil di akhir {@link #bayarVa(VirtualAccountBank, Date, String, Session)} dan juga
	 * langsung oleh beberapa gateway.</p>
	 *
	 * <h4>Yang dikerjakan</h4>
	 * <ol>
	 *   <li><b>Menyetel object di memori</b> milik pemanggil: {@code kegiatan}, {@code waktuBayar},
	 *       {@code notif}, dan {@code bank} (hanya bila nama bank tidak kosong).</li>
	 *   <li><b>Menyimpan ke database lewat session baru.</b> Baris VA dimuat ulang dari database
	 *       berdasarkan id (agar tidak menyimpan entity detached); bila baris tidak ditemukan,
	 *       object pemanggil sendiri yang disimpan. Baris {@link Va} dibuat bila belum ada.</li>
	 *   <li><b>Menyegarkan cache {@link #sukses}</b>: id ditambahkan bila ada salah satu penanda
	 *       hasil posting, dan justru <b>dihapus</b> bila ketiganya kosong — inilah yang membuat VA
	 *       bisa dibayar ulang setelah pembayarannya dibatalkan.</li>
	 *   <li><b>Mencetak bukti pembayaran</b> mahasiswa atau calon mahasiswa lewat
	 *       {@code CommonReportHelper}, lalu menyegarkan cache cicilan kegiatan.</li>
	 *   <li><b>Generate NIM otomatis</b>: bila pembayar masih calon mahasiswa tanpa mahasiswa, dan
	 *       konfigurasi
	 *       {@code calon_mahasiswa_baru_otomatis_mendapatkan_nim_saat_mahasiswa_membayar_persen_pembayaran_daftar_ulang}
	 *       atau
	 *       {@code calon_mahasiswa_baru_otomatis_mendapatkan_nim_saat_mahasiswa_melunasi_pembayaran_pembayaran_daftar_ulang}
	 *       aktif, dan kegiatan yang dibayar memang pendaftaran ulang mahasiswa baru, maka
	 *       {@code CommonReportHelper.checkGenNim} dijalankan.</li>
	 * </ol>
	 *
	 * <p>Session yang dibuka di langkah 2 ditutup di {@code finally}. Setiap tindak lanjut (langkah
	 * 4 dan 5) dibungkus {@code try} tersendiri sehingga kegagalan cetak atau generate NIM tidak
	 * membatalkan penandaan lunas yang sudah tersimpan.</p>
	 *
	 * @param virtualAccountBankNtt baris VA yang ditandai lunas; {@code null} diabaikan
	 * @param waktuBayar            waktu pembayaran menurut bank
	 * @param kegiatan              kegiatan hasil posting; boleh {@code null} (mis. saat pembatalan)
	 * @param notif                 payload notifikasi mentah dari bank
	 * @param bank                  nama bank/kanal; diabaikan bila kosong
	 */
	public static void updateVa(final VirtualAccountBank virtualAccountBankNtt, final Date waktuBayar,
			final Kegiatan kegiatan, String notif, String bank) {

		if (virtualAccountBankNtt == null) {
			return;
		}

		final Long idVa = virtualAccountBankNtt.getId();
		final Long idKegiatan = kegiatan == null ? null : kegiatan.getId();
		final String kodeVa = virtualAccountBankNtt.getKode();
		final String notifFinal = notif;
		final String bankFinal = bank;

		virtualAccountBankNtt.setKegiatan(idKegiatan);
		virtualAccountBankNtt.setWaktuBayar(waktuBayar);
		virtualAccountBankNtt.setNotif(notifFinal);
		if (bankFinal != null && bankFinal.trim().length() > 0) {
			virtualAccountBankNtt.setBank(bankFinal);
		}

		Session sessionBaru = null;
		Transaction tx = null;
		try {
			sessionBaru = HibernateUtil.openSession();
			VirtualAccountBank vaDb = idVa == null ? null
					: (VirtualAccountBank) sessionBaru.get(VirtualAccountBank.class, idVa);
			if (vaDb == null) {
				vaDb = virtualAccountBankNtt;
			}

			vaDb.setKegiatan(idKegiatan);
			vaDb.setWaktuBayar(waktuBayar);
			vaDb.setNotif(notifFinal);
			if (bankFinal != null && bankFinal.trim().length() > 0) {
				vaDb.setBank(bankFinal);
			}

			if (vaDb.getVa() == null) {
				Va va = new Va();
				va.setKode(kodeVa);
				tx = sessionBaru.beginTransaction();
				sessionBaru.save(va);
				tx.commit();
				tx = null;
				vaDb.setVa(va);
			}

			tx = sessionBaru.beginTransaction();
			Common.refreshSaveOrUpdate(sessionBaru, vaDb);
			tx.commit();
			tx = null;

			if (idVa != null) {
				if (idKegiatan != null || vaDb.getPembayaran() != null || vaDb.getDeposit() != null) {
					sukses.add(idVa);
				} else {
					sukses.remove(idVa);
				}
			}
		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				try {
					tx.rollback();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:1174");
				}
			}
			Common.tampilErrorJikaAdmin(e);
		} finally {
			KegiatanPersistenceHelper.closeNativeSession(sessionBaru);
		}

		try {
			if (kegiatan != null && kegiatan.getId() != null) {
				try {
					Mahasiswa mhs = virtualAccountBankNtt.getMahasiswa();
					BiodataCalonMahasiswa bio = virtualAccountBankNtt.getBiodataCalonMahasiswa();
					if (mhs != null) {
						CommonReportHelper.cetakBuktipembayaranMahasiswa(kegiatan, true);
					} else if (bio != null) {
						CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(kegiatan, true);
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				List<CicilanPembayaran> cicilanPembayarans = KegiatanPersistenceHelper.ambilCicilan(kegiatan, true);
				System.out.println("virtualAccountBank -> " + virtualAccountBankNtt + " kegiatan " + kegiatan
						+ " jumlah angsuran " + cicilanPembayarans.size());
				cicilanPembayarans.clear();
				cicilanPembayarans = null;
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		try {
			BiodataCalonMahasiswa biodataCalonMahasiswa = virtualAccountBankNtt.getBiodataCalonMahasiswa();
			if (biodataCalonMahasiswa != null && biodataCalonMahasiswa.getMahasiswa() == null) {
				boolean wajibBayarPersen = Common.bolehKonfigurasi("calon_mahasiswa_baru_otomatis_mendapatkan_nim_saat_mahasiswa_membayar_persen_pembayaran_daftar_ulang", Konfigurasi.TIDAK_AKTIF);
				boolean wajibBayar = Common.bolehKonfigurasi("calon_mahasiswa_baru_otomatis_mendapatkan_nim_saat_mahasiswa_melunasi_pembayaran_pembayaran_daftar_ulang", Konfigurasi.TIDAK_AKTIF);

				if (wajibBayarPersen || wajibBayar) {
					JenisKegiatan jenisKegiatan = ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU;
					if (jenisKegiatan != null && kegiatan != null && kegiatan.getId() != null
							&& kegiatan.getJenisKegiatan() != null
							&& kegiatan.getJenisKegiatan().getId().equals(jenisKegiatan.getId())) {
						CommonReportHelper.checkGenNim(kegiatan);
					}
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}


	/**
	 * Mengubah nominal tagihan sebuah VA yang sudah terbit, mis. ketika rincian biaya mahasiswa
	 * berubah sebelum dibayar.
	 *
	 * <p>Bila total baru sama dengan total lama (dibandingkan sebagai {@code int}), method langsung
	 * keluar setelah mencetak catatan ke {@code System.out}.</p>
	 *
	 * <p>Selain menyetel {@code total}, nama VA diperbarui menjadi
	 * {@code "<nama pemilik>-<barcode baru>"} ({@code Common.getGeneratedBarCode()}) supaya
	 * tampilan/berkas tagihan yang lama tidak tertukar dengan yang baru.</p>
	 *
	 * <p><b>Kuirk penting:</b> penulisan ke database dilakukan pada <b>thread terpisah</b> yang
	 * tidur 500&nbsp;ms lebih dulu, memakai SQL native
	 * {@code update virtual_account_bank set total = ? where id = ?} pada session Hibernate
	 * tersendiri. Akibatnya:</p>
	 * <ul>
	 *   <li>method ini kembali SEBELUM database benar-benar berubah — jangan langsung membaca ulang
	 *       nilai total dari database sesudah memanggilnya;</li>
	 *   <li>kegagalan penulisan tidak pernah sampai ke pemanggil (hanya tercatat di log/audit);</li>
	 *   <li>jeda 500&nbsp;ms itu tampaknya untuk memberi kesempatan transaksi pemanggil melakukan
	 *       commit lebih dulu, sehingga tidak saling mengunci baris yang sama.</li>
	 * </ul>
	 *
	 * @param virtualAccountBankNtt baris VA yang diubah; nama dan total di memori ikut disetel
	 * @param total                 nominal tagihan yang baru
	 */
	public static void updateTotal(final VirtualAccountBank virtualAccountBankNtt, final Double total) {
		if (virtualAccountBankNtt.getTotal() != null && total != null
				&& total.intValue() == virtualAccountBankNtt.getTotal().intValue()) {
			System.out.println("virtualAccountBankNtt -> " + virtualAccountBankNtt + " total sudah sama");
			return;
		}

		try {
			Mahasiswa mahasiswa = virtualAccountBankNtt.getMahasiswa();
			BiodataCalonMahasiswa biodataCalonMahasiswa = virtualAccountBankNtt.getBiodataCalonMahasiswa();
			String nama = mahasiswa == null ? biodataCalonMahasiswa.getNama() : mahasiswa.getNama();

			virtualAccountBankNtt.setNama(nama + "-" + Common.getGeneratedBarCode());
			virtualAccountBankNtt.setTotal(total);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:1242");
		}

		final Long idV = virtualAccountBankNtt.getId();
		new Thread(new Runnable() {
			/**
			 * Menulis nominal baru ke kolom {@code total} secara asinkron.
			 *
			 * <p>Tidur 500&nbsp;ms lebih dulu agar transaksi pemanggil sempat commit, lalu membuka
			 * session Hibernate sendiri dan mengeksekusi SQL native. Session ditutup di
			 * {@code finally}; kegagalan hanya di-rollback dan dicatat ke audit karena tidak ada
			 * pemanggil yang bisa menerima exception dari thread ini.</p>
			 */
			@Override
			public void run() {
				try {
					Thread.sleep(500);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:1251");
				}

				Session sessionBaru = null;
				Transaction tx = null;
				try {
					sessionBaru = HibernateUtil.openSession();
					tx = sessionBaru.beginTransaction();
					sessionBaru.createSQLQuery("update virtual_account_bank set total = :total where id = :id")
								.setParameter("total", total)
								.setParameter("id", idV)
								.executeUpdate();
					tx.commit();
				} catch (Exception e) {
					if (tx != null && tx.isActive())
						tx.rollback();
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:1267");
				} finally {
					if (sessionBaru != null) {
							KegiatanPersistenceHelper.closeNativeSession(sessionBaru);
						}
				}
			}
		}).start();
	}

	/**
	 * Merakit isi kolom {@code cicilan} dari grid rincian pembayaran di layar (ZK), yaitu daftar
	 * item yang dicentang/diisi nominalnya oleh petugas saat menerbitkan VA.
	 *
	 * <p>Setiap {@link Row} diperiksa lewat atribut yang dipasang layar pembuat grid:
	 * {@code "jumlahCicilan"} ({@link MyDoublebox}), {@code "cicilanPembayaran"}
	 * ({@link CicilanPembayaran}), dan {@code "itemBiaya"} ({@link Combobox}). Baris diikutkan hanya
	 * bila nominalnya bukan nol (toleransi &plusmn;0,01) DAN cicilannya belum tersimpan
	 * ({@code getId() == null}) — yang sudah tersimpan berarti sudah pernah dibayar.</p>
	 *
	 * <p>Bentuk token yang dihasilkan (dipisah koma) sesuai yang diurai
	 * {@link #bayarVa(VirtualAccountBank, Date, String, Session)}:</p>
	 * <ul>
	 *   <li>{@code "Bulanan-<idPengaturanPembayaranBulanan>-<nilai>"} bila baris berasal dari
	 *       pengaturan pembayaran bulanan;</li>
	 *   <li>{@code "Item-<idItemBiaya>-<nilai>-<bayarKe>-<idDetailBiaya>"} untuk item biaya lepas.
	 *       Bila detail biaya tidak terpilih, dipakai {@code bayarKe} = 1 dan id literal
	 *       {@code "null"}.</li>
	 * </ul>
	 *
	 * <p>Kegagalan pada satu baris (atribut hilang, komponen bertipe lain) ditelan dan dicatat ke
	 * audit; baris itu sekadar dilewati sehingga petugas tidak melihat error di tengah pengisian.</p>
	 *
	 * @param gridCicilan grid ZK berisi baris rincian biaya
	 * @return string token siap simpan ke kolom {@code cicilan}; string kosong bila tidak ada baris
	 *         yang memenuhi syarat
	 * @see #populateCicilan(Session, String, Kegiatan, Long, Date, JenisPembayaran, String)
	 */
	public static String populateCicilan(Grid gridCicilan) {
		@SuppressWarnings("unchecked")
		List<Row> mycicilanrows = gridCicilan.getRows().getChildren();
		StringBuilder cicilanBuilder = new StringBuilder();

		for (Row row : mycicilanrows) {
			try {
				MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");
				if (jumlahCicilan.getValue() != null
						&& (jumlahCicilan.getValue() > 0.01 || jumlahCicilan.getValue() < -0.01)) {
					CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) row.getAttribute("cicilanPembayaran");

					if (cicilanPembayaran.getId() == null) {
						PengaturanPembayaranBulanan biaya = cicilanPembayaran.getPengaturanPembayaranBulanan();
						Double nilai = jumlahCicilan.getValue();

						if (biaya != null) {
							if (cicilanBuilder.length() > 0)
								cicilanBuilder.append(",");
							cicilanBuilder.append("Bulanan-").append(biaya.getId()).append("-").append(nilai);
						} else {
							Combobox myItemBiaya = (Combobox) row.getAttribute("itemBiaya");
							DetailBiaya detailBiaya = (DetailBiaya) (myItemBiaya.getSelectedItem() == null ? null
									: myItemBiaya.getSelectedItem().getValue());
							ItemBiaya itemBiaya = (cicilanPembayaran.getItemBiaya() != null
									&& cicilanPembayaran.getItemBiaya().getId() != null)
											? cicilanPembayaran.getItemBiaya()
											: detailBiaya.getItemBiaya();
							detailBiaya = cicilanPembayaran.getDetailBiaya() != null
									? cicilanPembayaran.getDetailBiaya()
									: detailBiaya;

							if (cicilanBuilder.length() > 0)
								cicilanBuilder.append(",");
							cicilanBuilder.append("Item-").append(itemBiaya.getId()).append("-").append(nilai)
									.append("-").append(detailBiaya == null ? 1 : detailBiaya.getBayarKe()).append("-")
									.append(detailBiaya == null ? "null" : detailBiaya.getId());
						}
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:1317");
			}
		}
		return cicilanBuilder.toString();
	}

	// --- SISA GETTER SETTER SAMA PERSIS SEPERTI SEBELUMNYA ---
	// Demi menyingkat respon teks agar tidak terpotong namun tetap lengkap

	/**
	 * @return id pengguna terakhir yang menyimpan baris ini; boleh {@code null}
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna penyimpan.
	 *
	 * <p><b>Catatan:</b> nilai {@code null} atau kosong <b>diabaikan</b> — nilai lama dipertahankan.
	 * Jadi field ini tidak bisa dikosongkan lewat setter.</p>
	 *
	 * @param olehId id pengguna; nilai kosong/{@code null} tidak mengubah apa pun
	 */
	public void setOlehId(String olehId) {
		if (olehId != null && !olehId.trim().isEmpty())
			this.olehId = olehId;
	}

	/**
	 * @return nama pengguna terakhir yang menyimpan baris ini; boleh {@code null}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menyetel nama pengguna penyimpan.
	 *
	 * <p><b>Catatan:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong
	 * diabaikan sehingga field ini tidak bisa dikosongkan lewat setter.</p>
	 *
	 * @param oleh nama pengguna; nilai kosong/{@code null} tidak mengubah apa pun
	 */
	public void setOleh(String oleh) {
		if (oleh != null && !oleh.trim().isEmpty())
			this.oleh = oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: menyegel stempel waktu audit tepat sebelum baris di-update.
	 *
	 * <p>Pekerjaan sesungguhnya dilakukan
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}, yang mengisi
	 * {@link #setTanggal_dirubah(Date)} beserta {@link #setOleh(String)}/{@link #setOlehId(String)}
	 * dari sesi pengguna aktif. Jangan dipanggil manual — dipicu oleh provider persistence.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Stempel waktu perubahan terakhir; diinisialisasi ke waktu server saat object dibuat. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return stempel waktu perubahan terakhir baris ini
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Ringkasan baris VA untuk keperluan log dan pesan konsol mesin pembayaran.
	 *
	 * <p>Bentuknya
	 * {@code "<id>-<kode>-<jenisKegiatan><siswa>-<mahasiswa> - <calonSiswa>-<detailbiaya>-<tahunAkademik>-<semester>"}.
	 * Bila salah satu relasi gagal dimuat (mis. entity sudah detached), method jatuh ke bentuk
	 * pendek {@code "<id>-<kode>"} sehingga logging tidak pernah melempar exception.</p>
	 *
	 * <p><b>Perhatian:</b> method ini <b>bukan operasi baca murni</b> — ia menugaskan kembali field
	 * {@code jenisKegiatan} dari {@link #getJenisKegiatan()} dan memanggil beberapa getter relasi
	 * lain yang juga menulis balik ke field. Memasukkan entity ini ke dalam string log karenanya
	 * dapat memicu query lazy-load.</p>
	 *
	 * @return representasi teks baris VA
	 */
	public String toString() {
		try {
			jenisKegiatan = getJenisKegiatan();
			return id + "-" + kode + "-" + jenisKegiatan + getSiswa() + "-" + getMahasiswa() + " - " + getCalonSiswa()
					+ "-" + getDetailbiaya() + "-" + tahunAkademik + "-" + semester;
		} catch (Exception e) {
			return id + "-" + kode;
		}
	}

	/** Nomor VA yang dibayarkan pembayar; kolom {@code updatable = false} (lihat {@link #getKode()}). */
	private String kode;
	/** Nama/keterangan pemilik tagihan yang dikirim ke bank; ikut membawa barcode saat total diubah. */
	private String nama;
	/** Nama bank/kanal pembayaran; ditimpa dari {@link BankHost} bila relasinya terisi. */
	private String bank;
	/** Barcode/QR pembayaran bila kanal memakainya. */
	private String barcode;
	/** Keterangan bebas tagihan, ikut disalin ke dokumen pembayaran siswa. */
	private String keterangan;
	/** Payload permintaan mentah yang dikirim AIS ke gateway (JSON), untuk penelusuran. */
	private String request;
	/** Payload balasan mentah dari gateway (JSON), untuk penelusuran. */
	private String response;
	/** Payload notifikasi/callback mentah dari bank; sumber nilai {@code amount}/{@code biayaAdmin} pada beberapa kanal. */
	private String notif;
	/** Tautan pembayaran untuk gateway berbasis payment link; lihat {@link #ambilLink(String, BankHost)}. */
	private String link;
	/** Token pembayaran bulanan (jarang dipakai, dipertahankan untuk data lama). */
	private String bulanan;
	/** Token rincian yang dibayar — formatnya berbeda antara jalur mahasiswa dan jalur siswa (lihat Javadoc kelas). */
	private String cicilan;
	/** Channel pembayaran yang dipilih pembayar; untuk e-Smartlink dibaca dari JSON {@code request}. */
	private String channel;
	/** Daftar id {@link DetailBiaya} yang ditagihkan, dipisah koma. */
	private String detailbiaya;
	/** Jenis kegiatan/pos pembayaran (mis. daftar ulang, pendaftaran calon mahasiswa). */
	private JenisKegiatan jenisKegiatan;
	/** Baris nomor VA yang dipesan; relasi unik, dibuat otomatis oleh mesin pembayaran bila belum ada. */
	private Va va;
	/** Pembayar dari sisi sekolah (siswa aktif). */
	private Siswa siswa;
	/** Pembayar dari sisi sekolah (calon siswa/PPDB). */
	private CalonSiswa calonSiswa;
	/** Pembayar dari sisi perguruan tinggi (mahasiswa aktif). */
	private Mahasiswa mahasiswa;
	/** Pembayar dari sisi perguruan tinggi (calon mahasiswa/PMB). */
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	/** Jadwal pembayaran yang menaungi tagihan ini; menentukan tanggal kadaluarsa default. */
	private JadwalPembayaran jadwalPembayaran;
	/** Semester yang ditagihkan; dipaksa 0 untuk pendaftaran calon mahasiswa. */
	private Integer semester;
	/** Tahun akademik tagihan; diisi otomatis dari rencana tahun akademik bila kosong. */
	private String tahunAkademik;
	/** Waktu pembayaran menurut bank; dikosongkan getter bila tidak ada penanda hasil posting. */
	private Date waktuBayar;
	/** Nominal yang benar-benar ditarik bank (total + biaya admin), atau nilai dari notifikasi gateway. */
	private Double amount;
	/** Nominal tagihan pokok yang harus dibayar. */
	private Double total;
	/** Biaya admin bank/gateway di luar {@code total}. */
	private Double biayaAdmin;
	/** Id {@link Kegiatan} hasil posting (jalur mahasiswa) — id mentah, bukan relasi Hibernate. */
	private Long kegiatan;
	/** Id {@link Deposit} hasil topup — id mentah, bukan relasi Hibernate. */
	private Long deposit;
	/** Id {@code PembayaranSiswa} hasil posting (jalur sekolah) — id mentah, bukan relasi Hibernate. */
	private Long pembayaran;
	/** Id perguruan tinggi pemilik data, untuk pemisahan multi-tenant; diturunkan otomatis oleh {@link #getPt()}. */
	private Long pt;
	/** Bila {@code true}, nomor VA dibentuk sendiri oleh {@link #getKode()} dari id baris. */
	private Boolean otomatis;
	/** Penanda pemakaian skema kode unik alternatif; diteruskan ke {@link Kegiatan}. */
	private Boolean kodeUnikLain;
	/** Penanda bahwa penerbitan/pembayaran VA ini bermasalah; memengaruhi {@link #getStatusVirtualAccountRingkas()}. */
	private Boolean terjadiKendala;
	/** Tanggal kadaluarsa tagihan (tanpa jam); default diambil dari akhir jadwal pembayaran. */
	private Date kadaluarsa;
	/** Kadaluarsa lengkap dengan jam; disetel otomatis dari {@link #setKadaluarsa(Date)}. */
	private Date kadaluarsaWaktu;
	/** Kadaluarsa khusus barcode/QR, bila kanalnya memberi masa berlaku terpisah. */
	private Date kadaluarsaBarcode;
	/** Host bank/gateway penerbit VA; sumber nama bank dan jenis pembayaran. */
	private BankHost bankHost;
	/** Kelas siswa/mahasiswa saat tagihan dibuat; nilai turunan, diisi {@link #getKelas()}. */
	private String kelas;
	/** Pembayar dari modul kursus (peserta yang membeli produk kursus). */
	private PesertaPunyaProdukKursus pesertaPunyaProdukKursus;
	/** Akun kas sekolah tujuan pembayaran; getter menolak akun tabungan/manual. */
	private AkunPembayaranSiswa akunPembayaranSiswa;
	/** Kanal pembayaran; ditimpa oleh kanal milik {@link JenisKegiatan} bila ada. */
	private KanalPembayaran kanalPembayaran;
	/** Porsi pembayaran yang diambil dari tabungan/deposit siswa. */
	private Double tabungan;
	/** Porsi setoran yang masuk sebagai penambahan saldo, bukan pelunasan tagihan. */
	private Double topup;
	/** Pembayar dari modul koperasi. */
	private AnggotaKoperasi anggotaKoperasi;
	/** Cara pembayaran koperasi yang dipilih. */
	private CaraPembayaranKoperasi caraPembayaranKoperasi;

	/**
	 * Membersihkan teks sebelum disimpan ke kolom database.
	 *
	 * <p>Karakter NUL ({@code '\0'}) diganti spasi lalu teks di-{@code trim}. Ini bukan kosmetik:
	 * PostgreSQL menolak menyimpan {@code \0} di kolom {@code text}/{@code varchar}, sedangkan
	 * payload mentah dari bank kadang membawanya. Dipakai oleh hampir semua setter bertipe
	 * {@code String} di kelas ini.</p>
	 *
	 * @param value teks asal; boleh {@code null}
	 * @return teks yang sudah bersih, atau {@code null} bila masukannya {@code null}
	 */
	private static String bersihkanTextDb(String value) {
		if (value == null) {
			return null;
		}
		return value.replace('\0', ' ').trim();
	}

	/**
	 * Konstruktor kosong wajib Hibernate/JPA. Seluruh field dibiarkan {@code null} kecuali
	 * {@link #getTanggal_dirubah()} yang sudah terisi waktu server.
	 */
	public VirtualAccountBank() {
	}

	/**
	 * Konstruktor pintasan untuk membuat baris VA yang sejak awal terikat pada satu perguruan
	 * tinggi (pemisahan multi-tenant).
	 *
	 * @param pt id perguruan tinggi pemilik data
	 */
	public VirtualAccountBank(Long pt) {
		this.pt = pt;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Identity kolom {@code id}; nilainya juga menjadi bahan pembentuk nomor VA otomatis pada
	 * {@link #getKode()}.</p>
	 *
	 * @return primary key baris ini, {@code null} bila belum tersimpan
	 * @see GeneralValueObject
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Umumnya hanya dipanggil Hibernate.
	 *
	 * @param id primary key baris ini
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nomor VA yang harus dibayar — <b>bukan getter murni</b>: nilai field
	 * {@code kode} bisa dihitung ulang setiap kali method ini dipanggil.
	 *
	 * <p>Tiga sumber, berurutan:</p>
	 * <ol>
	 *   <li><b>Nomor otomatis.</b> Bila {@link #getOtomatis()} {@code true} dan baris sudah ber-id,
	 *       nomor dibentuk dari tiga digit terakhir kode {@link JenisKegiatan} (tanda titik dan koma
	 *       dibuang) ditambah lima digit {@code id + 1} yang dipadkan nol. Bila jenis kegiatan tidak
	 *       tersedia, dipakai cadangan berupa delapan digit {@code id + 1} saja.</li>
	 *   <li><b>e-Smartlink.</b> Bila kolom {@code request} terisi dan nama bank {@code "Esmartlink"},
	 *       nomor diambil dari {@code order_id} pada JSON request.</li>
	 *   <li><b>Nomor yang sudah dipesan.</b> Selain kasus e-Smartlink, bila relasi {@link Va} punya
	 *       kode tidak kosong, kode itulah yang menang.</li>
	 * </ol>
	 *
	 * <p>Kolom dipetakan {@code updatable = false}, sehingga hasil perhitungan ulang di atas TIDAK
	 * ikut tersimpan ke database pada operasi update — nomor VA di database tetap sebagaimana
	 * dituliskan saat insert. Namun field di memori tetap berubah, dan nilai itulah yang dipakai
	 * pemanggil berikutnya.</p>
	 *
	 * @return nomor VA; boleh {@code null} bila baris belum pernah punya kode
	 */
	@Column(name = "kode", updatable = false, nullable = false)
	public String getKode() {
		if (getOtomatis() && id != null) {
			try {
				String digitKetiga = "000000000000" + (id + 1);
				digitKetiga = digitKetiga.substring(digitKetiga.length() - 5);
				String kd = org.apache.commons.lang3.StringUtils.replace(
						org.apache.commons.lang3.StringUtils.replace(jenisKegiatan.getKode(), ".", ""), ",", "");
				if (kd.length() > 3)
					kd = kd.substring(kd.length() - 3);
				kode = kd + digitKetiga;
			} catch (Exception e) {
				String digitKetiga = "000000000000" + (id + 1);
				kode = digitKetiga.substring(digitKetiga.length() - 8);
			}
		}
		if (getRequest() != null && !getRequest().isEmpty() && getBank() != null
				&& getBank().equalsIgnoreCase("Esmartlink")) {
			try {
				JSONObject jsonObject = new JSONObject(getRequest());
				if (!jsonObject.isNull("order_id"))
					kode = jsonObject.getString("order_id").trim();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:1464");
			}
		} else {
			try {
				if (va != null && va.getKode() != null && !va.getKode().trim().isEmpty())
					kode = getVa().getKode();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:1470");
			}
		}
		return kode;
	}

	/**
	 * Menyetel nomor VA, <b>hanya sekali</b>.
	 *
	 * <p>Nilai baru diterima hanya bila tidak kosong DAN field lama masih kosong. Nomor VA yang
	 * sudah pernah terbit karenanya tidak bisa ditimpa lewat setter — konsisten dengan pemetaan
	 * kolom {@code updatable = false}.</p>
	 *
	 * @param kode nomor VA baru; diabaikan bila kosong atau bila sudah ada nomor sebelumnya
	 */
	public void setKode(String kode) {
		if (kode != null && !kode.trim().isEmpty() && (this.kode == null || this.kode.trim().isEmpty()))
			this.kode = kode;
	}

	/**
	 * Mengembalikan nama/identitas tagihan yang dikirim ke bank.
	 *
	 * <p><b>Bukan getter murni.</b> Khusus kanal {@code "flip"} yang sudah punya notifikasi, field
	 * {@code nama} ditimpa dengan nilai {@code id} dari JSON notifikasi (id transaksi Flip).</p>
	 *
	 * <p>Bila field masih {@code null}, yang dikembalikan adalah barcode baru hasil
	 * {@code Common.getGeneratedBarCode()} — <b>nilai acak yang tidak disimpan</b>, sehingga dua
	 * pemanggilan berturut-turut pada entity yang sama bisa memberi hasil berbeda.</p>
	 *
	 * @return nama tagihan; tidak pernah {@code null}
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if (getBank() != null && getBank().equalsIgnoreCase("flip") && getNotif() != null
				&& !getNotif().trim().isEmpty()) {
			try {
				nama = new JSONObject(getNotif()).get("id") + "";
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:1487");
			}
		}
		return this.nama == null ? Common.getGeneratedBarCode() : this.nama.trim();
	}

	/**
	 * Menyetel nama tagihan; teks dibersihkan lebih dulu oleh {@link #bersihkanTextDb(String)}.
	 *
	 * @param nama nama tagihan
	 */
	public void setNama(String nama) {
		this.nama = bersihkanTextDb(nama);
	}

	/**
	 * @return keterangan bebas tagihan; boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan tagihan; teks dibersihkan oleh {@link #bersihkanTextDb(String)}.
	 *
	 * @param keterangan keterangan bebas
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = bersihkanTextDb(keterangan);
	}

	/**
	 * Mengembalikan nominal tagihan pokok, dengan {@code null} dinormalkan menjadi {@code 0.0}
	 * sehingga aman dipakai langsung dalam perhitungan.
	 *
	 * @return nominal tagihan; tidak pernah {@code null}
	 */
	public Double getTotal() {
		return total == null ? 0.0 : total;
	}

	/**
	 * Menyetel nominal tagihan pokok.
	 *
	 * <p>Untuk mengubah nominal VA yang sudah terbit, pakai
	 * {@link #updateTotal(VirtualAccountBank, Double)} agar perubahannya juga tertulis ke database.</p>
	 *
	 * @param total nominal tagihan
	 */
	public void setTotal(Double total) {
		this.total = total;
	}

	/**
	 * @return token pembayaran bulanan, {@code ""} bila kosong (tidak pernah {@code null})
	 */
	public String getBulanan() {
		return bulanan == null ? "" : bulanan.trim();
	}

	/**
	 * Menyetel token pembayaran bulanan.
	 *
	 * <p>Berbeda dari setter teks lain di kelas ini, nilai di sini <b>tidak</b> dilewatkan
	 * {@link #bersihkanTextDb(String)}.</p>
	 *
	 * @param bulanan token pembayaran bulanan
	 */
	public void setBulanan(String bulanan) {
		this.bulanan = bulanan;
	}

	/**
	 * Mengembalikan daftar id {@link DetailBiaya} yang ditagihkan (dipisah koma), {@code ""} bila
	 * kosong sehingga pemanggil bisa langsung memakai {@code isEmpty()}/{@code split()}.
	 *
	 * @return daftar id detail biaya; tidak pernah {@code null}
	 */
	public String getDetailbiaya() {
		return detailbiaya == null ? "" : detailbiaya.trim();
	}

	/**
	 * Menyetel daftar id detail biaya yang ditagihkan.
	 *
	 * @param detailbiaya daftar id dipisah koma
	 */
	public void setDetailbiaya(String detailbiaya) {
		this.detailbiaya = bersihkanTextDb(detailbiaya);
	}

	/**
	 * Jenis kegiatan/pos pembayaran yang ditagihkan (mis. pendaftaran calon mahasiswa, daftar
	 * ulang). Kodenya ikut membentuk nomor VA otomatis pada {@link #getKode()}, dan kanal
	 * pembayarannya dapat menimpa {@link #getKanalPembayaran()}.
	 *
	 * <p>Mengikuti pola getter relasi standar AIS: hasil {@code check()} ditugaskan kembali ke
	 * field, sehingga proxy lazy yang sudah detached tetap dapat dipakai.</p>
	 *
	 * @return jenis kegiatan, atau {@code null}
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_kegiatan", nullable = true)
	public JenisKegiatan getJenisKegiatan() {
		return jenisKegiatan = check(jenisKegiatan);
	}

	/**
	 * @param jenisKegiatan jenis kegiatan/pos pembayaran yang ditagihkan
	 */
	public void setJenisKegiatan(JenisKegiatan jenisKegiatan) {
		this.jenisKegiatan = jenisKegiatan;
	}

	/**
	 * Mahasiswa aktif pemilik tagihan (jalur perguruan tinggi).
	 *
	 * @return mahasiswa, atau {@code null} bila pembayarnya bukan mahasiswa aktif
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		return mahasiswa = check(mahasiswa);
	}

	/**
	 * @param mahasiswa mahasiswa pemilik tagihan
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Calon mahasiswa (PMB) pemilik tagihan.
	 *
	 * <p>Bila calon mahasiswa ini belum punya {@link Mahasiswa}, pelunasan tagihan daftar ulang
	 * dapat memicu pembuatan NIM otomatis — lihat
	 * {@link #updateVa(VirtualAccountBank, Date, Kegiatan, String, String)}.</p>
	 *
	 * @return biodata calon mahasiswa, atau {@code null}
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "biodata_calon_mahasiswa", nullable = true)
	public BiodataCalonMahasiswa getBiodataCalonMahasiswa() {
		return biodataCalonMahasiswa = check(biodataCalonMahasiswa);
	}

	/**
	 * @param biodataCalonMahasiswa calon mahasiswa pemilik tagihan
	 */
	public void setBiodataCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	/**
	 * Jadwal pembayaran yang menaungi tagihan ini; tanggal akhirnya menjadi kadaluarsa default VA
	 * (lihat {@link #getKadaluarsa()}).
	 *
	 * @return jadwal pembayaran, atau {@code null}
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jadwal_pembayaran", nullable = true)
	public JadwalPembayaran getJadwalPembayaran() {
		return jadwalPembayaran = check(jadwalPembayaran);
	}

	/**
	 * @param jadwalPembayaran jadwal pembayaran penaung tagihan
	 */
	public void setJadwalPembayaran(JadwalPembayaran jadwalPembayaran) {
		this.jadwalPembayaran = jadwalPembayaran;
	}

	/**
	 * Mengembalikan semester yang ditagihkan.
	 *
	 * <p><b>Bukan getter murni:</b> bila jenis kegiatannya adalah pendaftaran calon mahasiswa
	 * ({@code ConstantValues.PENDAFTARAN_CALON_MAHASISWA}), field {@code semester} <b>dipaksa
	 * menjadi 0</b> — calon mahasiswa memang belum punya semester. Nilai paksaan ini ikut tersimpan
	 * bila entity kemudian di-flush.</p>
	 *
	 * @return semester yang ditagihkan; boleh {@code null}
	 */
	public Integer getSemester() {
		jenisKegiatan = getJenisKegiatan();
		if (jenisKegiatan != null && ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
				&& jenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId())) {
			semester = 0;
		}
		return semester;
	}

	/**
	 * @param semester semester yang ditagihkan
	 */
	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	/**
	 * Mengembalikan tahun akademik tagihan, <b>mengisinya sendiri bila masih kosong</b>.
	 *
	 * <p>Penentuan dilakukan {@code RencanaTahunAkademikAction.getCurrentRencanaTahunAkademik(...)}
	 * berdasarkan konteks pemilik tagihan: fakultas/jurusan/status awal/angkatan/program bila
	 * pembayarnya mahasiswa, atau yayasan/sekolah bila pembayarnya siswa atau calon siswa. Titik
	 * waktu acuannya adalah {@link #getWaktuBayar()}, jatuh ke {@link #getTanggal_dirubah()} bila
	 * VA belum dibayar.</p>
	 *
	 * <p><b>Perhatian dua hal.</b> Pertama, ini getter yang menulis balik ke field, sehingga nilai
	 * hasil tebakan bisa ikut tersimpan. Kedua, argumen fakultas ditulis
	 * {@code mahasiswa == null || mahasiswa.getJurusan() != null ? null : mahasiswa.getJurusan().getFakultas()}
	 * — perhatikan bahwa cabang "else" hanya tercapai ketika jurusan justru {@code null}, sehingga
	 * pemanggilan {@code getFakultas()} di situ pasti melempar {@code NullPointerException}.
	 * Kondisinya tampak terbalik (mestinya {@code getJurusan() == null ? null : ...}). Dicatat apa
	 * adanya; risikonya nyata untuk mahasiswa tanpa jurusan, dan method ini tidak menangkap
	 * exception.</p>
	 *
	 * @return nama tahun akademik, atau {@code null} bila tidak ada rencana tahun akademik yang cocok
	 */
	public String getTahunAkademik() {
		if (tahunAkademik == null) {
			RencanaTahunAkademik s = RencanaTahunAkademikAction.getCurrentRencanaTahunAkademik(
					mahasiswa == null || mahasiswa.getJurusan() != null ? null : mahasiswa.getJurusan().getFakultas(),
					mahasiswa == null ? null : mahasiswa.getJurusan(),
					siswa != null ? siswa.getYayasan() : (calonSiswa != null ? calonSiswa.getYayasan() : null),
					siswa != null ? siswa.getSekolah() : (calonSiswa != null ? calonSiswa.getSekolah() : null),
					mahasiswa == null ? null : mahasiswa.getStatusAwalMahasiswa(),
					mahasiswa == null ? null : mahasiswa.getTahunangkatan(),
					mahasiswa == null ? null : mahasiswa.getProgram(),
					getWaktuBayar() == null ? getTanggal_dirubah() : getWaktuBayar(), null, null);
			if (s != null)
				tahunAkademik = s.getNama();
		}
		return tahunAkademik;
	}

	/**
	 * @param tahunAkademik nama tahun akademik tagihan
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengembalikan nama bank/kanal pembayaran.
	 *
	 * <p><b>Bukan getter murni:</b> bila relasi {@link BankHost} terisi dan punya nama, field
	 * {@code bank} <b>ditimpa</b> dengan nama host tersebut. Artinya {@link BankHost} adalah sumber
	 * kebenaran; kolom {@code bank} hanya salinan yang menyusul.</p>
	 *
	 * <p>Nilai inilah yang dibandingkan (tanpa peka huruf besar/kecil) dengan {@code "Esmartlink"}
	 * dan {@code "flip"} oleh {@link #getKode()}, {@link #getNama()}, {@link #getAmount()},
	 * {@link #getBiayaAdmin()}, dan {@link #getChannel()}.</p>
	 *
	 * @return nama bank/kanal; boleh {@code null}
	 */
	public String getBank() {
		if (getBankHost() != null && getBankHost().getNama() != null)
			bank = getBankHost().getNama();
		return bank;
	}

	/**
	 * Menyetel nama bank/kanal; teks dibersihkan oleh {@link #bersihkanTextDb(String)}.
	 *
	 * <p>Nilai ini akan tertimpa lagi oleh {@link #getBank()} bila relasi {@link BankHost} terisi.</p>
	 *
	 * @param bank nama bank/kanal
	 */
	public void setBank(String bank) {
		this.bank = bersihkanTextDb(bank);
	}

	/**
	 * Mengembalikan id {@link Kegiatan} hasil posting pembayaran (jalur mahasiswa).
	 *
	 * <p><b>Getter paling tidak biasa di kelas ini.</b> Nilainya tidak diambil dari kolom database
	 * saja, melainkan dari <i>cache berkas samping</i> milik {@link GeneralValueObject}:</p>
	 * <ul>
	 *   <li>{@code retreive("k")} — bila berisi angka, angka itulah yang dipakai sebagai id
	 *       kegiatan, menimpa nilai kolom;</li>
	 *   <li>{@code retreive("hapus")} — bila bernilai {@code "1"}, id kegiatan <b>dikosongkan</b>.</li>
	 * </ul>
	 * <p>Kedua penanda ditulis oleh {@link #setKegiatan(Long)}. Mekanisme ini membuat pembatalan
	 * pembayaran tetap "menempel" pada object meski kolom database belum sempat diperbarui — tetapi
	 * juga berarti membaca getter ini bisa MENGHAPUS id kegiatan di memori, dan penghapusan itu
	 * ikut tersimpan bila entity di-flush. Perlakukan sebagai operasi bermutasi.</p>
	 *
	 * @return id kegiatan hasil posting, atau {@code null} bila belum/tidak lagi terbayar
	 * @see GeneralValueObject#retreive(String)
	 * @see #setKegiatan(Long)
	 */
	public Long getKegiatan() {
		String k = retreive("k");
		String hapus = retreive("hapus");
		if (k != null && !k.isEmpty())
			kegiatan = Long.parseLong(k);
		if (hapus != null && hapus.equalsIgnoreCase("1"))
			kegiatan = null;
		return kegiatan;
	}

	/**
	 * Menyetel id {@link Kegiatan} hasil posting, sekaligus menulis penanda ke cache berkas samping
	 * yang dibaca {@link #getKegiatan()}.
	 *
	 * <p>Bila {@code kegiatan} tidak {@code null}: {@code "k"} diisi id dan {@code "hapus"} diisi
	 * {@code "0"}. Bila {@code null}: {@code "hapus"} diisi {@code "1"} dan {@code "k"} dikosongkan
	 * — inilah cara pembatalan pembayaran ditandai.</p>
	 *
	 * <p><b>Efek samping:</b> setiap pemanggilan menulis berkas cache (dilewati saat startup
	 * aplikasi, lihat {@link GeneralValueObject#put(String, String)}).</p>
	 *
	 * @param kegiatan id kegiatan hasil posting; {@code null} berarti pembayaran dibatalkan
	 * @see GeneralValueObject#put(String, String)
	 */
	public void setKegiatan(Long kegiatan) {
		if (kegiatan != null) {
			put(kegiatan.toString(), "k");
			put("0", "hapus");
		} else {
			put("1", "hapus");
			put(null, "k");
		}
		this.kegiatan = kegiatan;
	}

	/**
	 * Penanda pemakaian skema kode unik alternatif; diteruskan ke {@link Kegiatan} saat posting.
	 *
	 * @return {@code true} bila skema kode unik lain dipakai; {@code null} dinormalkan ke
	 *         {@code false}
	 */
	public Boolean getKodeUnikLain() {
		return kodeUnikLain == null ? false : kodeUnikLain;
	}

	/**
	 * @param kodeUnikLain penanda pemakaian skema kode unik alternatif
	 */
	public void setKodeUnikLain(Boolean kodeUnikLain) {
		this.kodeUnikLain = kodeUnikLain;
	}

	/**
	 * Mengembalikan token rincian yang dibayar, {@code ""} bila kosong.
	 *
	 * <p>Format token berbeda antara jalur mahasiswa
	 * ({@code Bulanan-} / {@code Item-} / {@code Keranjang-}) dan jalur siswa
	 * ({@code <apa saja>-<idTagihan>[-<nilai>]}) — lihat Javadoc kelas.</p>
	 *
	 * @return token rincian; tidak pernah {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getCicilan() {
		return cicilan == null ? "" : cicilan.trim();
	}

	/**
	 * @param cicilan token rincian yang dibayar, dipisah koma
	 */
	public void setCicilan(String cicilan) {
		this.cicilan = bersihkanTextDb(cicilan);
	}

	/**
	 * Payload permintaan mentah yang dikirim AIS ke gateway.
	 *
	 * <p>Untuk e-Smartlink, JSON ini menjadi sumber {@code order_id} pada {@link #getKode()} dan
	 * {@code channel} pada {@link #getChannel()}. Isinya bisa memuat data pribadi pembayar —
	 * perlakukan sebagai data sensitif saat menampilkan atau mengekspor.</p>
	 *
	 * @return payload request mentah; boleh {@code null}
	 */
	@Column(name = "request", nullable = true, columnDefinition = "text")
	public String getRequest() {
		return request;
	}

	/**
	 * @param request payload permintaan mentah ke gateway
	 */
	public void setRequest(String request) {
		this.request = bersihkanTextDb(request);
	}

	/**
	 * @return payload balasan mentah dari gateway; boleh {@code null}
	 */
	@Column(name = "response", nullable = true, columnDefinition = "text")
	public String getResponse() {
		return response;
	}

	/**
	 * @param response payload balasan mentah dari gateway
	 */
	public void setResponse(String response) {
		this.response = bersihkanTextDb(response);
	}

	/**
	 * Penanda bahwa nomor VA dibentuk otomatis oleh sistem.
	 *
	 * <p>Bila {@code true}, {@link #getKode()} menghitung ulang nomor VA dari id baris setiap kali
	 * dipanggil.</p>
	 *
	 * @return {@code true} bila nomor VA otomatis; {@code null} dinormalkan ke {@code false}
	 */
	public Boolean getOtomatis() {
		return otomatis == null ? false : otomatis;
	}

	/**
	 * @param otomatis {@code true} bila nomor VA dibentuk otomatis
	 */
	public void setOtomatis(Boolean otomatis) {
		this.otomatis = otomatis;
	}

	/**
	 * Penanda bahwa penerbitan atau pembayaran VA ini bermasalah; membuat
	 * {@link #getStatusVirtualAccountRingkas()} melaporkan "Terjadi Kendala".
	 *
	 * @return {@code true} bila bermasalah; {@code null} dinormalkan ke {@code false}
	 */
	public Boolean getTerjadiKendala() {
		return terjadiKendala == null ? false : terjadiKendala;
	}

	/**
	 * @param terjadiKendala penanda VA bermasalah
	 */
	public void setTerjadiKendala(Boolean terjadiKendala) {
		this.terjadiKendala = terjadiKendala;
	}

	/**
	 * Total yang harus ditransfer pembayar: tagihan pokok ditambah biaya admin, dibulatkan ke
	 * bilangan bulat (pembulatan ke bawah karena memakai {@code intValue()}).
	 *
	 * <p>Dipakai kanal yang mensyaratkan nominal bulat dalam rupiah. Perhatikan bahwa nilainya
	 * dihitung dari {@link #getBiayaAdmin()} dan {@link #getTotal()} — keduanya getter yang bisa
	 * menulis balik ke field.</p>
	 *
	 * @return total tagihan + biaya admin sebagai {@code int}
	 */
	public int totalBiaya() {
		VirtualAccountBank virtualAccountBankNtt = this;
		return (virtualAccountBankNtt.getBiayaAdmin().intValue() + virtualAccountBankNtt.getTotal().intValue());
	}

	/**
	 * Mengembalikan biaya admin bank/gateway di luar tagihan pokok.
	 *
	 * <p><b>Bukan getter murni</b> — nilainya dihitung ulang dari notifikasi bank untuk dua kanal:</p>
	 * <ul>
	 *   <li><b>e-Smartlink</b>: bila {@code amount} yang ditagihkan lebih besar dari {@code total},
	 *       selisihnya dianggap biaya admin;</li>
	 *   <li><b>Flip</b>: diambil dari {@code fee_amount} pada JSON notifikasi. Field itu tidak
	 *       selalu dikirim (mis. notifikasi lama atau berstatus pending), karena itu dibaca dengan
	 *       {@code optDouble} setelah dipastikan ada — memakai getter wajib bisa melempar exception
	 *       tepat saat Hibernate memanggil property getter dalam proses flush.</li>
	 * </ul>
	 *
	 * <p>Notifikasi yang tidak dapat diurai hanya dicatat ke {@code System.err} dan nilai lama
	 * dipertahankan; ini disengaja agar kegagalan parsing tidak menggagalkan flush entity.</p>
	 *
	 * @return biaya admin; {@code null} dinormalkan ke {@code 0.0}
	 */
	public Double getBiayaAdmin() {
		if (getNotif() != null && !getNotif().isEmpty() && getBank() != null
				&& getBank().equalsIgnoreCase("Esmartlink")) {
			try {
				if (amount != null && amount > getTotal())
					biayaAdmin = amount - getTotal();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:1697");
			}
		} else if (getBank() != null && getBank().equalsIgnoreCase("flip") && getNotif() != null
				&& !getNotif().trim().isEmpty()) {
			try {
				JSONObject notifJson = new JSONObject(getNotif());
				/* fee_amount tidak selalu dikirim Flip (mis. notifikasi lama/pending).
				 * Nilai ini opsional, jadi jangan memakai getter wajib yang melempar saat
				 * Hibernate memanggil property getter pada proses flush. */
				if (notifJson.has("fee_amount") && !notifJson.isNull("fee_amount")) {
					biayaAdmin = notifJson.optDouble("fee_amount", biayaAdmin == null ? 0.0 : biayaAdmin);
				}
			} catch (Exception e) {
				/* Notifikasi tidak valid tidak boleh menggagalkan flush entity. Pertahankan
				 * biaya admin yang sudah tersimpan (atau nol melalui return di bawah). */
				System.err.println("[VirtualAccountBank.getBiayaAdmin] notif Flip tidak dapat dibaca: "
						+ e.getMessage());
			}
		}
		return biayaAdmin == null ? 0.0 : biayaAdmin;
	}

	/**
	 * @param biayaAdmin biaya admin bank/gateway
	 */
	public void setBiayaAdmin(Double biayaAdmin) {
		this.biayaAdmin = biayaAdmin;
	}

	/**
	 * Mengembalikan tanggal kadaluarsa tagihan (tanpa jam).
	 *
	 * <p><b>Bukan getter murni:</b> bila field masih kosong sementara {@link JadwalPembayaran}
	 * punya {@code endDate}, tanggal itu <b>disalin ke field</b> dan menjadi kadaluarsa VA. Field
	 * {@code jadwalPembayaran} sendiri ikut ditugaskan ulang dari
	 * {@link #getJadwalPembayaran()}.</p>
	 *
	 * @return tanggal kadaluarsa, atau {@code null} bila tagihan tidak berbatas waktu
	 */
	@Temporal(TemporalType.DATE)
	public Date getKadaluarsa() {
		jadwalPembayaran = getJadwalPembayaran();
		if (jadwalPembayaran != null && kadaluarsa == null && jadwalPembayaran.getEndDate() != null)
			kadaluarsa = jadwalPembayaran.getEndDate();
		return kadaluarsa;
	}

	/**
	 * Menyetel tanggal kadaluarsa tagihan, sekaligus <b>merawat</b> {@code kadaluarsaWaktu}.
	 *
	 * <p>Aturannya: {@code kadaluarsaWaktu} ikut disetel bila belum pernah diisi, atau bila tanggal
	 * baru membawa jam yang bukan {@code 00.00} (artinya pemanggil memang bermaksud menentukan jam
	 * batas, bukan sekadar tanggal). Dengan begitu jam batas yang sudah disetel khusus tidak
	 * tertimpa oleh pembaruan tanggal biasa.</p>
	 *
	 * @param kadaluarsa tanggal (dan mungkin jam) kadaluarsa tagihan
	 * @see #getKadaluarsaWaktu()
	 */
	public void setKadaluarsa(Date kadaluarsa) {
		if (kadaluarsaWaktu == null
				|| (kadaluarsa != null && !Common.timeFormat2.get().format(kadaluarsa).equals("00.00")))
			this.kadaluarsaWaktu = kadaluarsa;
		this.kadaluarsa = kadaluarsa;
	}

	/**
	 * Host bank/gateway penerbit VA ini.
	 *
	 * <p>Menjadi sumber {@link #getBank()} (nama) dan jenis pembayaran yang dicatat pada
	 * {@link CicilanPembayaran}. Pencarian VA menghormati aturan "{@code bankHost} kosong atau
	 * sama", sehingga VA tanpa host dapat dibayar lewat bank mana pun — lihat
	 * {@link #ambilVa(String, Double, BankHost, Criterion)}.</p>
	 *
	 * @return host bank, atau {@code null}
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "bank_host", nullable = true)
	public BankHost getBankHost() {
		return bankHost = check(bankHost);
	}

	/**
	 * @param bankHost host bank/gateway penerbit VA
	 */
	public void setBankHost(BankHost bankHost) {
		this.bankHost = bankHost;
	}

	/**
	 * Siswa aktif pemilik tagihan (jalur sekolah).
	 *
	 * @return siswa, atau {@code null} bila pembayarnya bukan siswa aktif
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa", nullable = true)
	public Siswa getSiswa() {
		return siswa = check(siswa);
	}

	/**
	 * @param siswa siswa pemilik tagihan
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Calon siswa (PPDB) pemilik tagihan.
	 *
	 * @return calon siswa, atau {@code null}
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "calon_siswa", nullable = true)
	public CalonSiswa getCalonSiswa() {
		return calonSiswa = check(calonSiswa);
	}

	/**
	 * @param calonSiswa calon siswa pemilik tagihan
	 */
	public void setCalonSiswa(CalonSiswa calonSiswa) {
		this.calonSiswa = calonSiswa;
	}

	/**
	 * Mengembalikan waktu pembayaran menurut bank.
	 *
	 * <p><b>Getter yang MENGHAPUS data.</b> Baris pertama method ini <b>mengosongkan</b> field
	 * {@code waktuBayar} bila ketiga penanda hasil posting ({@link #getKegiatan()},
	 * {@link #getPembayaran()}, {@link #getDeposit()}) kosong. Tujuannya menjaga konsistensi —
	 * VA tidak boleh tampak "sudah dibayar" tanpa dokumen pembayaran — tetapi konsekuensinya
	 * sekadar membaca getter ini dapat menghapus waktu bayar di memori, dan penghapusan itu ikut
	 * tersimpan bila entity di-flush. Inilah salah satu alasan
	 * {@link #updateVirtualAccountSiswaMinimal(Session, VirtualAccountBank, Date, Long, String)}
	 * memilih bulk update HQL alih-alih menyimpan entity utuh.</p>
	 *
	 * <p>Selain itu, tanggal bertahun 1970 (gejala timestamp nol dari bank) diperbaiki menjadi
	 * tahun berjalan.</p>
	 *
	 * @return waktu pembayaran, atau {@code null} bila VA belum terbayar
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuBayar() {
		waktuBayar = getKegiatan() == null && getPembayaran() == null && getDeposit() == null ? null : waktuBayar;
		try {
			if (waktuBayar != null) {
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(waktuBayar);
				if (calendar.get(Calendar.YEAR) == 1970) {
					calendar.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
					waktuBayar = calendar.getTime();
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:1770");
		}
		return waktuBayar;
	}

	/**
	 * @param waktuBayar waktu pembayaran menurut bank
	 */
	public void setWaktuBayar(Date waktuBayar) {
		this.waktuBayar = waktuBayar;
	}

	/**
	 * Peserta kursus pemilik tagihan (modul kursus).
	 *
	 * <p>Berbeda dari relasi lain di kelas ini, getter ini <b>tidak</b> memanggil {@code check()};
	 * relasinya di-fetch dengan {@link FetchMode#SELECT} tanpa {@code FetchType.LAZY} eksplisit,
	 * sehingga tidak menghadapi masalah proxy detached yang sama.</p>
	 *
	 * @return peserta kursus, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "peserta_punya_produk_kursus", nullable = true)
	public PesertaPunyaProdukKursus getPesertaPunyaProdukKursus() {
		return pesertaPunyaProdukKursus;
	}

	/**
	 * @param pesertaPunyaProdukKursus peserta kursus pemilik tagihan
	 */
	public void setPesertaPunyaProdukKursus(PesertaPunyaProdukKursus pesertaPunyaProdukKursus) {
		this.pesertaPunyaProdukKursus = pesertaPunyaProdukKursus;
	}

	/**
	 * Mengembalikan akun kas sekolah tujuan pembayaran.
	 *
	 * <p><b>Getter yang MENGHAPUS data.</b> Setelah {@code check()}, bila akun yang terpasang
	 * bertipe tabungan ({@code dariTabungan}) atau manual, field <b>disetel menjadi {@code null}</b>
	 * dan {@code null} itulah yang dikembalikan. Alasannya masuk akal secara bisnis — setoran VA
	 * tidak boleh masuk ke akun tabungan atau akun yang dikelola manual, sehingga
	 * {@link #bayarSiswa(VirtualAccountBank, Session, Date, String, boolean, String, boolean)} akan
	 * mencari akun non-manual milik sekolah sebagai gantinya.</p>
	 *
	 * <p><b>Risikonya nyata:</b> pembacaan biasa (mis. saat merender grid) membuang relasi di
	 * memori, dan bila entity kemudian di-flush, kolom {@code akun_pembayaran_siswa_id} di database
	 * ikut dikosongkan. Dicatat apa adanya.</p>
	 *
	 * @return akun kas yang sah untuk pembayaran VA, atau {@code null}
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun_pembayaran_siswa_id", nullable = true)
	public AkunPembayaranSiswa getAkunPembayaranSiswa() {
		akunPembayaranSiswa = check(akunPembayaranSiswa);
		if (akunPembayaranSiswa != null && (akunPembayaranSiswa.getDariTabungan() || akunPembayaranSiswa.getManual()))
			akunPembayaranSiswa = null;
		return akunPembayaranSiswa;
	}

	/**
	 * @param akunPembayaranSiswa akun kas sekolah tujuan pembayaran
	 */
	public void setAkunPembayaranSiswa(AkunPembayaranSiswa akunPembayaranSiswa) {
		this.akunPembayaranSiswa = akunPembayaranSiswa;
	}

	/**
	 * Id {@code PembayaranSiswa} hasil posting (jalur sekolah) — id mentah, bukan relasi Hibernate,
	 * jadi tidak ada jaminan referensinya masih ada.
	 *
	 * <p>Dipakai sebagai salah satu penanda "sudah terbayar" oleh
	 * {@link #isSudahTerbayar(VirtualAccountBank)} dan {@link #getWaktuBayar()}.</p>
	 *
	 * @return id dokumen pembayaran siswa, atau {@code null}
	 */
	public Long getPembayaran() {
		return pembayaran;
	}

	/**
	 * @param pembayaran id {@code PembayaranSiswa} hasil posting
	 */
	public void setPembayaran(Long pembayaran) {
		this.pembayaran = pembayaran;
	}

	/**
	 * Mengembalikan id perguruan tinggi pemilik data (pemisahan multi-tenant).
	 *
	 * <p><b>Bukan getter murni:</b> nilai field selalu <b>dihitung ulang</b> dari pemilik tagihan
	 * bila jalurnya tersedia, dengan prioritas berurutan: siswa &rarr; calon siswa &rarr; mahasiswa
	 * (lewat jurusan &rarr; fakultas) &rarr; calon mahasiswa (prodi lulus) &rarr; calon mahasiswa
	 * (prodi pilihan pertama). Bila tidak satu pun jalur lengkap, nilai lama dipertahankan.</p>
	 *
	 * <p>Karena menelusuri banyak relasi lazy, pemanggilan getter ini bisa memicu beberapa query.</p>
	 *
	 * @return id perguruan tinggi, atau {@code null}
	 */
	public Long getPt() {
		if (getSiswa() != null && getSiswa().getSekolah() != null
				&& getSiswa().getSekolah().getPerguruanTinggi() != null)
			pt = getSiswa().getSekolah().getPerguruanTinggi().getId();
		else if (getCalonSiswa() != null && getCalonSiswa().getSekolah() != null
				&& getCalonSiswa().getSekolah().getPerguruanTinggi() != null)
			pt = getCalonSiswa().getSekolah().getPerguruanTinggi().getId();
		else if (getMahasiswa() != null && getMahasiswa().getJurusan() != null
				&& getMahasiswa().getJurusan().getFakultas() != null
				&& getMahasiswa().getJurusan().getFakultas().getPerguruanTinggi() != null)
			pt = getMahasiswa().getJurusan().getFakultas().getPerguruanTinggi().getId();
		else if (getBiodataCalonMahasiswa() != null && getBiodataCalonMahasiswa().getProdiLulus() != null
				&& getBiodataCalonMahasiswa().getProdiLulus().getFakultas() != null
				&& getBiodataCalonMahasiswa().getProdiLulus().getFakultas().getPerguruanTinggi() != null)
			pt = getBiodataCalonMahasiswa().getProdiLulus().getFakultas().getPerguruanTinggi().getId();
		else if (getBiodataCalonMahasiswa() != null && getBiodataCalonMahasiswa().getProdi1() != null
				&& getBiodataCalonMahasiswa().getProdi1().getFakultas() != null
				&& getBiodataCalonMahasiswa().getProdi1().getFakultas().getPerguruanTinggi() != null)
			pt = getBiodataCalonMahasiswa().getProdi1().getFakultas().getPerguruanTinggi().getId();
		return pt;
	}

	/**
	 * @param pt id perguruan tinggi pemilik data; akan dihitung ulang oleh {@link #getPt()}
	 */
	public void setPt(Long pt) {
		this.pt = pt;
	}

	/**
	 * Mengembalikan tautan pembayaran yang sudah dinormalkan.
	 *
	 * <p>Tautan kosong menjadi {@code ""}; tautan yang belum diawali {@code "https"} diberi awalan
	 * {@code "https://"}. Perhatikan bahwa penambahan awalan itu terjadi <b>di getter saja</b> dan
	 * tidak dikembalikan dalam bentuk yang di-{@code trim} (trim hanya diterapkan pada cabang
	 * tautan yang sudah benar), sehingga nilai yang disimpan di database tetap seperti aslinya.</p>
	 *
	 * @return tautan pembayaran siap pakai; tidak pernah {@code null}
	 * @see #ambilLink(String, BankHost)
	 */
	@Column(columnDefinition = "text")
	public String getLink() {
		return link == null || link.isEmpty() ? "" : !link.startsWith("https") ? "https://" + link : link.trim();
	}

	/**
	 * @param link tautan pembayaran dari gateway
	 */
	public void setLink(String link) {
		this.link = bersihkanTextDb(link);
	}

	/**
	 * Mengembalikan batas kadaluarsa lengkap dengan jam.
	 *
	 * <p><b>Bukan getter murni:</b> bila field masih kosong, diisi dari {@link #getKadaluarsa()}
	 * (yang sendirinya bisa mengambil dari jadwal pembayaran). Jadi memanggil getter ini bisa
	 * merambat menulis dua field sekaligus.</p>
	 *
	 * @return batas kadaluarsa berikut jam, atau {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getKadaluarsaWaktu() {
		if (kadaluarsaWaktu == null)
			kadaluarsaWaktu = getKadaluarsa();
		return kadaluarsaWaktu;
	}

	/**
	 * @param kadaluarsaWaktu batas kadaluarsa berikut jam
	 */
	public void setKadaluarsaWaktu(Date kadaluarsaWaktu) {
		this.kadaluarsaWaktu = kadaluarsaWaktu;
	}

	/**
	 * Payload notifikasi/callback mentah dari bank.
	 *
	 * <p>Menjadi sumber nilai bagi {@link #getNama()} (Flip), {@link #getAmount()}, dan
	 * {@link #getBiayaAdmin()}. Isinya dapat memuat data transaksi pembayar — perlakukan sebagai
	 * data sensitif.</p>
	 *
	 * @return payload notifikasi mentah; boleh {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getNotif() {
		return notif;
	}

	/**
	 * @param notif payload notifikasi mentah dari bank
	 */
	public void setNotif(String notif) {
		this.notif = bersihkanTextDb(notif);
	}

	/**
	 * @return data barcode/QR pembayaran; boleh {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getBarcode() {
		return barcode;
	}

	/**
	 * Menyetel data barcode/QR pembayaran.
	 *
	 * <p>Berbeda dari setter teks lain, nilai di sini <b>tidak</b> dilewatkan
	 * {@link #bersihkanTextDb(String)} — payload QR memang tidak boleh diubah karakternya.</p>
	 *
	 * @param barcode data barcode/QR
	 */
	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	/**
	 * @return batas berlaku barcode/QR, atau {@code null} bila kanal tidak memakainya
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getKadaluarsaBarcode() {
		return kadaluarsaBarcode;
	}

	/**
	 * @param kadaluarsaBarcode batas berlaku barcode/QR
	 */
	public void setKadaluarsaBarcode(Date kadaluarsaBarcode) {
		this.kadaluarsaBarcode = kadaluarsaBarcode;
	}

	/**
	 * Buffer HTML sementara untuk kebutuhan tampilan/cetak.
	 *
	 * <p>Ditandai {@link Transient} — tidak dipetakan ke kolom mana pun dan hilang begitu entity
	 * dimuat ulang.</p>
	 *
	 * @return potongan HTML sementara, atau {@code null}
	 */
	@Transient
	public String getHtmlTemporaryData() {
		return htmlTemporaryData;
	}

	/**
	 * @param htmlTemporaryData potongan HTML sementara (tidak disimpan ke database)
	 */
	public void setHtmlTemporaryData(String htmlTemporaryData) {
		this.htmlTemporaryData = htmlTemporaryData;
	}

	// Metode ini juga di-copy dari aslinya, karena sangat spesifik bisnis logic
	/**
	 * Memposting token cicilan menjadi baris {@link CicilanPembayaran} pada satu {@link Kegiatan} —
	 * versi "lepas" yang bisa dipakai gateway lain tanpa memiliki baris
	 * {@code VirtualAccountBank}.
	 *
	 * <p>Isinya paralel dengan bagian penguraian token di
	 * {@link #bayarVa(VirtualAccountBank, Date, String, Session)}, dengan tiga perbedaan:</p>
	 * <ul>
	 *   <li>token dan referensi kegiatan diberikan sebagai parameter, bukan dibaca dari VA;</li>
	 *   <li>prefix referensi dapat ditentukan pemanggil ({@code prefix}), sehingga referensi
	 *       menjadi {@code "<prefix>-<idKegiatan>-<token>-<reqId>"}. Validator cicilan pun diambil
	 *       dari prefix itu lewat {@link #ambilDefaultValidatorBank(String)};</li>
	 *   <li>bentuk token {@code "Keranjang-"} <b>tidak</b> didukung di sini — hanya
	 *       {@code "Bulanan-"} dan {@code "Item-"}.</li>
	 * </ul>
	 *
	 * <p>Sifatnya idempoten: cicilan dicari dulu berdasarkan referensi uniknya, lalu di-update bila
	 * sudah ada. Item biaya berpenghitungan {@code DIKALI_NILAI_MINUS} dijadikan negatif. Tanggal
	 * kosong diganti waktu server. Di akhir, cache cicilan kegiatan disegarkan lewat
	 * {@link #sinkronkanKegiatanSetelahBayar(Kegiatan)}. Kegagalan ditelan (rollback + audit).</p>
	 *
	 * @param session           session Hibernate aktif
	 * @param cicilan           token rincian dipisah koma
	 * @param kegiatan          kegiatan tujuan posting
	 * @param reqId             id permintaan/VA yang dicatat pada {@code refVa} tiap cicilan
	 * @param tanggalTransaksi  tanggal transaksi; {@code null} diganti waktu server
	 * @param jenisPembayaran   jenis pembayaran yang dicatat pada cicilan
	 * @param prefix            prefix referensi sekaligus penentu nama validator bank
	 * @see #populateCicilan(Grid)
	 */
	public static void populateCicilan(Session session, String cicilan, Kegiatan kegiatan, Long reqId,
			Date tanggalTransaksi, JenisPembayaran jenisPembayaran, String prefix) {
		Transaction tx = null;
		try {
			for (String idPemBul : StringUtils.split(cicilan, ",")) {
				if (idPemBul != null && idPemBul.startsWith("Bulanan-")) {
					PengaturanPembayaranBulanan ppBln = (PengaturanPembayaranBulanan) session
							.createCriteria(PengaturanPembayaranBulanan.class)
							.add(Restrictions.idEq(Long.parseLong(idPemBul.split("-")[1]))).uniqueResult();
					if (ppBln != null) {
						String ref = prefix + "-" + kegiatan.getId() + "-" + idPemBul + "-" + reqId;
						ItemBiaya itemBiaya = ppBln.getDetailBiaya().getItemBiaya();
						Double subtotal = 0.0;
						try {
							String[] spl = idPemBul.split("-");
							subtotal = Double.parseDouble(spl[spl.length - 1]);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:1910");
						}
						if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS))
							subtotal = 0.0 - subtotal;
						CicilanPembayaran cp = (CicilanPembayaran) session.createCriteria(CicilanPembayaran.class)
								.add(Restrictions.eq("ref", ref)).setMaxResults(1).uniqueResult();
						if (cp == null)
							cp = new CicilanPembayaran(ppBln.getDetailBiaya());
						cp.setRef(ref);
						cp.setValidator(ambilDefaultValidatorBank(prefix));
						cp.setKegiatan(kegiatan);
						cp.setItemBiaya(itemBiaya);
						cp.setPengaturanPembayaranBulanan(ppBln);
						cp.setRefVa(reqId);
						cp.setNilai(subtotal);
						cp.setNilaiAsli(cp.getNilai());
						cp.setTanggal(tanggalTransaksi == null ? WaktuUtil.getDate() : tanggalTransaksi);
						cp.setJenisPembayaran(jenisPembayaran);
						cp.setDenda(0.0);

						tx = session.beginTransaction();
						if (cp.getId() == null)
							session.save(cp);
						else
							Common.refreshUpdate(session, cp);
						tx.commit();
					}
				} else if (idPemBul != null && idPemBul.startsWith("Item-")) {
					ItemBiaya itemBiaya = (ItemBiaya) ConstantValues
							.simpleObject(
									session.createCriteria(ItemBiaya.class)
											.add(Restrictions.idEq(Long.parseLong(idPemBul.split("-")[1]))),
									ItemBiaya.class);
					if (itemBiaya != null) {
						String ref = prefix + "-" + kegiatan.getId() + "-" + idPemBul + "-" + reqId;
						Double subtotal = 0.0;
						Long detailBiayaId = null;
						try {
							String[] spl = idPemBul.split("-");
							subtotal = Double.parseDouble(spl[2]);
							detailBiayaId = Long.parseLong(spl[4]);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:1951");
						}
						if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS))
							subtotal = 0.0 - subtotal;
						CicilanPembayaran cp = (CicilanPembayaran) session.createCriteria(CicilanPembayaran.class)
								.add(Restrictions.eq("ref", ref)).setMaxResults(1).uniqueResult();
						if (cp == null)
							cp = new CicilanPembayaran(DetailBiaya.muatRefAman(session, detailBiayaId));
						cp.setDetailBiaya(DetailBiaya.muatRefAman(session, detailBiayaId));
						cp.setRef(ref);
						cp.setValidator(ambilDefaultValidatorBank(prefix));
						cp.setKegiatan(kegiatan);
						cp.setItemBiaya(itemBiaya);
						cp.setPengaturanPembayaranBulanan(null);
						cp.setRefVa(reqId);
						cp.setNilai(subtotal);
						cp.setNilaiAsli(cp.getNilai());
						cp.setTanggal(tanggalTransaksi == null ? WaktuUtil.getDate() : tanggalTransaksi);
						cp.setJenisPembayaran(jenisPembayaran);
						cp.setDenda(0.0);

						tx = session.beginTransaction();
						if (cp.getId() == null)
							session.save(cp);
						else
							Common.refreshUpdate(session, cp);
						tx.commit();
					}
				}
			}
			sinkronkanKegiatanSetelahBayar(kegiatan);
		} catch (Exception e) {
			if (tx != null && tx.isActive())
				tx.rollback();
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:1985");
		}
	}

	/**
	 * Mengembalikan nominal yang benar-benar ditarik dari pembayar (tagihan pokok + biaya admin).
	 *
	 * <p><b>Bukan getter murni</b> — nilainya dihitung ulang setiap kali dipanggil:</p>
	 * <ul>
	 *   <li><b>e-Smartlink</b> (ada {@code notif}): bila status pada JSON {@code data} bernilai
	 *       {@code "SUCCESS"}, dipakai {@code data.amount} dari notifikasi.</li>
	 *   <li><b>Flip</b> (ada {@code notif}): dipakai {@code amount} dari JSON notifikasi.</li>
	 *   <li><b>Selain itu</b>: {@code biayaAdmin + total}.</li>
	 * </ul>
	 *
	 * <p><b>Kuirk yang dicurigai bug:</b> pada cabang e-Smartlink yang statusnya bukan
	 * {@code "SUCCESS"}, nilai dihitung sebagai {@code total + total} padahal syarat cabangnya
	 * justru memeriksa {@code biayaAdmin != null} — hampir pasti yang dimaksud
	 * {@code total + biayaAdmin}, sebagaimana cabang terakhir. Dicatat, tidak diperbaiki.</p>
	 *
	 * <p>Semua kegagalan parsing JSON ditelan sehingga nilai lama dipertahankan.</p>
	 *
	 * @return nominal yang ditagihkan ke pembayar; {@code null} dinormalkan ke {@code 0.0}
	 */
	public Double getAmount() {
		if (getNotif() != null && !getNotif().isEmpty() && getBank() != null
				&& getBank().equalsIgnoreCase("Esmartlink")) {
			try {
				JSONObject jsonObject = new JSONObject(getNotif());
				JSONObject data = jsonObject.getJSONObject("data");
				if (data.getString("status").trim().equalsIgnoreCase("SUCCESS"))
					amount = data.getDouble("amount");
				else if (total != null && biayaAdmin != null)
					amount = total + total;
				else
					amount = 0.0;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:2001");
			}
		} else if (getBank() != null && getBank().equalsIgnoreCase("flip") && getNotif() != null
				&& !getNotif().trim().isEmpty()) {
			try {
				amount = new JSONObject(getNotif()).getDouble("amount");
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:2007");
			}
		} else if (total != null && biayaAdmin != null)
			amount = biayaAdmin + total;
		return amount == null ? 0.0 : amount;
	}

	/**
	 * @param amount nominal yang ditagihkan ke pembayar; akan dihitung ulang oleh {@link #getAmount()}
	 */
	public void setAmount(Double amount) {
		this.amount = amount;
	}

	/**
	 * Baris nomor VA yang dipesan untuk tagihan ini.
	 *
	 * <p>Relasi ini {@code unique} — satu {@link Va} hanya boleh dipakai satu baris VA. Bila masih
	 * kosong saat pembayaran masuk, mesin pembayaran membuatkannya otomatis dengan kode = nomor VA
	 * (lihat {@link #bayarTopup(VirtualAccountBank, Session, Date, String, boolean, String)},
	 * {@link #updateVa(VirtualAccountBank, Date, Kegiatan, String, String)}, dan
	 * {@link #ambilVa(String, Double, BankHost, Criterion)}).</p>
	 *
	 * <p>Kode pada relasi inilah yang menang atas field {@code kode} di {@link #getKode()}.</p>
	 *
	 * @return baris nomor VA, atau {@code null} bila belum pernah dipesan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "va", nullable = true, unique = true)
	public Va getVa() {
		return va;
	}

	/**
	 * @param va baris nomor VA yang dipesan
	 */
	public void setVa(Va va) {
		this.va = va;
	}

	/**
	 * Mengembalikan kanal pembayaran tagihan ini.
	 *
	 * <p><b>Bukan getter murni:</b> bila {@link JenisKegiatan} punya kanal pembayaran sendiri, kanal
	 * itu <b>menimpa</b> field — konfigurasi di jenis kegiatan dianggap lebih berwenang daripada
	 * nilai yang tersimpan di baris VA. Bila tidak, dipakai nilai field setelah {@code check()}.</p>
	 *
	 * @return kanal pembayaran, atau {@code null}
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kanal_pembayaran", nullable = true)
	public KanalPembayaran getKanalPembayaran() {
		if (getJenisKegiatan() != null && getJenisKegiatan().getKanalPembayaran() != null)
			kanalPembayaran = getJenisKegiatan().getKanalPembayaran();
		else
			kanalPembayaran = check(kanalPembayaran);
		return kanalPembayaran;
	}

	/**
	 * @param kanalPembayaran kanal pembayaran; dapat tertimpa oleh kanal milik {@link JenisKegiatan}
	 */
	public void setKanalPembayaran(KanalPembayaran kanalPembayaran) {
		this.kanalPembayaran = kanalPembayaran;
	}

	/**
	 * Anggota koperasi pemilik tagihan (modul koperasi); ikut menerima saldo pada topup.
	 *
	 * @return anggota koperasi, atau {@code null}
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "anggota_koperasi", nullable = true)
	public AnggotaKoperasi getAnggotaKoperasi() {
		return anggotaKoperasi = check(anggotaKoperasi);
	}

	/**
	 * @param anggotaKoperasi anggota koperasi pemilik tagihan
	 */
	public void setAnggotaKoperasi(AnggotaKoperasi anggotaKoperasi) {
		this.anggotaKoperasi = anggotaKoperasi;
	}

	/**
	 * Cara pembayaran koperasi yang dipilih untuk tagihan ini.
	 *
	 * @return cara pembayaran koperasi, atau {@code null}
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "cara_pembayaran_koperasi", nullable = true)
	public CaraPembayaranKoperasi getCaraPembayaranKoperasi() {
		return caraPembayaranKoperasi = check(caraPembayaranKoperasi);
	}

	/**
	 * @param caraPembayaranKoperasi cara pembayaran koperasi
	 */
	public void setCaraPembayaranKoperasi(CaraPembayaranKoperasi caraPembayaranKoperasi) {
		this.caraPembayaranKoperasi = caraPembayaranKoperasi;
	}

	/**
	 * Penanda apakah tagihan ini benar-benar diterbitkan sebagai virtual account.
	 *
	 * <p>Perhatikan default-nya: {@code null} dinormalkan menjadi <b>{@code true}</b> (berbeda dari
	 * boolean lain di kelas ini yang default-nya {@code false}), sehingga data lama yang belum punya
	 * kolom ini tetap diperlakukan sebagai tagihan ber-VA.</p>
	 *
	 * @return {@code true} bila tagihan diterbitkan sebagai VA
	 */
	public Boolean getPakaiva() {
		return pakaiva == null ? true : pakaiva;
	}

	/**
	 * @param pakaiva penanda penerbitan sebagai VA
	 */
	public void setPakaiva(Boolean pakaiva) {
		this.pakaiva = pakaiva;
	}

	/**
	 * Mengembalikan kelas pembayar untuk kebutuhan tampilan/laporan.
	 *
	 * <p><b>Bukan getter murni dan berpotensi mahal:</b> nilainya dihitung ulang tiap pemanggilan —
	 * untuk siswa lewat {@code Siswa.ambilKelas(siswa, tahunAkademik)} (satu query, dan
	 * {@link #getTahunAkademik()} sendiri bisa memicu pencarian rencana tahun akademik), untuk
	 * mahasiswa langsung dari {@code Mahasiswa.getKelas()}. Siswa tanpa kelas menghasilkan
	 * {@code "-"}.</p>
	 *
	 * <p>Kegagalan ditelan (audit) dan nilai lama dipertahankan. Hindari memanggilnya di dalam loop
	 * render grid berukuran besar.</p>
	 *
	 * @return nama kelas pembayar, atau {@code null} bila tidak dapat ditentukan
	 */
	public String getKelas() {
		try {
			if (getSiswa() != null) {
				KelasSiswa kelasSiswa = Siswa.ambilKelas(siswa, getTahunAkademik());
				kelas = kelasSiswa == null ? "-" : kelasSiswa.getNama();
			} else if (getMahasiswa() != null) {
				kelas = getMahasiswa().getKelas();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:2080");
		}
		return kelas;
	}

	/**
	 * @param kelas nama kelas pembayar; akan dihitung ulang oleh {@link #getKelas()}
	 */
	public void setKelas(String kelas) {
		this.kelas = kelas;
	}

	/**
	 * Mengembalikan channel pembayaran yang dipilih pembayar.
	 *
	 * <p><b>Bukan getter murni:</b> khusus kanal {@code "Esmartlink"}, nilai diambil dari elemen
	 * pertama array {@code channel} pada JSON {@code request} dan menimpa field. Untuk kanal lain,
	 * nilai field dikembalikan apa adanya.</p>
	 *
	 * @return nama channel pembayaran, atau {@code null}
	 */
	public String getChannel() {
		if (getRequest() != null && !getRequest().isEmpty() && getBank() != null
				&& getBank().equalsIgnoreCase("Esmartlink")) {
			try {
				JSONObject jsonObject = new JSONObject(getRequest());
				if (!jsonObject.isNull("channel"))
					channel = jsonObject.getJSONArray("channel").getString(0);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:2096");
			}
		}
		return channel;
	}

	/**
	 * @param channel nama channel pembayaran
	 */
	public void setChannel(String channel) {
		this.channel = channel;
	}

	/**
	 * Porsi pembayaran yang diambil dari tabungan/deposit siswa.
	 *
	 * <p>Saat posting, nilai ini dipangkas agar tidak melebihi {@link #getTotal()} dan disalin ke
	 * {@code dariTabungan}/{@code dariTabunganManual} pada {@code PembayaranSiswa}.</p>
	 *
	 * @return porsi dari tabungan; {@code null} dinormalkan ke {@code 0.0}
	 */
	public Double getTabungan() {
		return tabungan == null ? 0.0 : tabungan;
	}

	/**
	 * @param tabungan porsi pembayaran yang diambil dari tabungan/deposit
	 */
	public void setTabungan(Double tabungan) {
		this.tabungan = tabungan;
	}

	/**
	 * Id {@link Deposit} hasil topup — id mentah, bukan relasi Hibernate.
	 *
	 * <p>Dipakai {@link #bayarTopup(VirtualAccountBank, Session, Date, String, boolean, String)}
	 * untuk memastikan topup bersifat idempoten, dan menjadi salah satu penanda "sudah terbayar".</p>
	 *
	 * @return id deposit, atau {@code null}
	 */
	public Long getDeposit() {
		return deposit;
	}

	/**
	 * @param deposit id {@link Deposit} hasil topup
	 */
	public void setDeposit(Long deposit) {
		this.deposit = deposit;
	}

	/**
	 * Porsi setoran yang masuk sebagai penambahan saldo, bukan pelunasan tagihan.
	 *
	 * <p>Bila nilainya di atas 0,1 sedangkan {@code cicilan} kosong, setoran dianggap topup murni
	 * dan jalur pembayaran tagihan dilewati — lihat
	 * {@link #bayarSiswa(VirtualAccountBank, Session, Date, String, boolean, String, boolean)}.</p>
	 *
	 * @return nominal topup; {@code null} dinormalkan ke {@code 0.0}
	 */
	public Double getTopup() {
		return topup == null ? 0.0 : topup;
	}

	/**
	 * @param topup nominal setoran yang masuk sebagai penambahan saldo
	 */
	public void setTopup(Double topup) {
		this.topup = topup;
	}

	/**
	 * Melakukan HTTP GET ke endpoint e-Smartlink dengan menjalankan proses {@code curl} eksternal.
	 *
	 * <p>Alasan memakai proses eksternal alih-alih klien HTTP Java: sertifikat/TLS host e-Smartlink
	 * pernah bermasalah, dan {@code curl -k} memberi jalan pintas yang dapat dikonfigurasi
	 * operator. Bila konfigurasi {@code curl_e_smartlink_via_server_lain} bernilai aktif, perintah
	 * {@code curl} tidak dijalankan di server ini melainkan <b>di-relay lewat {@code ssh}</b> ke
	 * host lain — berguna ketika hanya satu IP yang di-whitelist gateway.</p>
	 *
	 * <p><b>PERINGATAN KEAMANAN.</b> Username dan password diubah menjadi header
	 * {@code Authorization: Basic ...} lalu dititipkan sebagai <b>argumen baris perintah</b>; pada
	 * mode relay, header itu menjadi bagian dari string perintah yang dieksekusi shell di host
	 * tujuan. Artinya kredensial dapat terbaca lewat daftar proses ({@code ps}) dan riwayat shell,
	 * dan tautan {@code linkPost} yang mengandung kutip tunggal berpotensi merusak/menyisipi
	 * perintah remote. Selain itu, host/port/user relay memiliki nilai default yang ter-hardcode
	 * pada pemanggilan {@code Common.getKonfigurasi} di dalam method ini — dan
	 * {@code getKonfigurasi} <b>menuliskan default tersebut ke database</b> bila baris
	 * konfigurasinya belum ada. Catat ini saat audit governance/rotasi kredensial; jangan mengubah
	 * nilai default tanpa memeriksa isi tabel konfigurasi produksi.</p>
	 *
	 * <p>Stream keluaran dan error dibaca sampai habis, lalu {@code reader}, {@code errorReader},
	 * dan proses ditutup di {@code finally}. Bila proses hanya menghasilkan error, dikembalikan
	 * JSON buatan sendiri {@code {"status":"error","message":"Gagal koneksi CURL/SSH proxy"}};
	 * exception juga dibungkus menjadi JSON {@code status=error} sehingga pemanggil selalu menerima
	 * teks yang bisa diurai.</p>
	 *
	 * @param linkPost                 URL endpoint yang di-GET
	 * @param username_va_e_smartlink  username Basic Auth e-Smartlink
	 * @param password_va_e_smartlink  password Basic Auth e-Smartlink
	 * @return isi respons apa adanya, atau JSON {@code status=error} bila gagal
	 * @see #curlSmartlink(String, String, String, JSONObject)
	 */
	public static String curlSmartlinkGet(String linkPost, String username_va_e_smartlink,
			String password_va_e_smartlink) {
		String hasil = "";
		Process p = null;
		BufferedReader reader = null;
		BufferedReader errorReader = null;
		try {
			String screet_key = DownloadTagihanSiswaBankOnline.getBasicAuthenticationHeader(username_va_e_smartlink,
					password_va_e_smartlink);
			if (Common.getKonfigurasi("curl_e_smartlink_via_server_lain", Konfigurasi.TIDAK_AKTIF).getNilai().trim()
					.equals(Konfigurasi.AKTIF)) {
				String ipServerLain = Common.getKonfigurasi("curl_e_smartlink_via_IP", "38.47.178.46").getNilai()
						.trim();
				String portServerLain = Common.getKonfigurasi("curl_e_smartlink_via_PORT", "22031").getNilai().trim();
				String userServerLain = Common.getKonfigurasi("curl_e_smartlink_via_USER", "zishof").getNilai().trim();

				String remoteCurlCmd = "curl -s -k --location --request GET '" + linkPost + "' "
						+ "--header 'Content-Type: application/json' --header 'Authorization: Basic " + screet_key
						+ "'";
				String[] command = { "ssh", "-p", portServerLain, "-o", "StrictHostKeyChecking=no",
						userServerLain + "@" + ipServerLain, remoteCurlCmd };

				p = new ProcessBuilder(command).start();
			} else {
				String[] command = { "curl", "-s", "-k", "--location", "--request", "GET", linkPost, "--header",
						"Content-Type: application/json", "--header", "Authorization: Basic " + screet_key };
				p = new ProcessBuilder(command).start();
			}

			reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null)
				builder.append(line).append(System.getProperty("line.separator"));
			hasil = builder.toString();

			errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			StringBuilder errorBuilder = new StringBuilder();
			while ((line = errorReader.readLine()) != null)
				errorBuilder.append(line);

			if (errorBuilder.length() > 0 && hasil.trim().isEmpty()) {
				System.out.println("ERROR CURL: " + errorBuilder.toString());
				hasil = "{\"status\":\"error\",\"message\":\"Gagal koneksi CURL/SSH proxy\"}";
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:2176");
			hasil = "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
		} finally {
			try {
				if (reader != null)
					reader.close();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:2182");
			}
			try {
				if (errorReader != null)
					errorReader.close();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:2187");
			}
			if (p != null)
				p.destroy();
		}
		return hasil;
	}

	/**
	 * Melakukan HTTP POST JSON ke endpoint e-Smartlink dengan menjalankan proses {@code curl}
	 * eksternal — pasangan {@link #curlSmartlinkGet(String, String, String)} untuk permintaan yang
	 * membawa badan data (mis. penerbitan tagihan/payment link).
	 *
	 * <p>Mekanisme, mode relay {@code ssh}, penanganan stream, dan bentuk hasil error persis sama
	 * dengan {@link #curlSmartlinkGet(String, String, String)} — termasuk seluruh
	 * <b>PERINGATAN KEAMANAN</b> yang tertulis di sana tentang kredensial pada baris perintah dan
	 * nilai default konfigurasi relay yang ter-hardcode.</p>
	 *
	 * <p>Tambahan risiko khusus method ini: pada mode relay, {@code postData.toString()} disisipkan
	 * ke dalam string perintah remote di antara kutip tunggal ({@code --data-raw '...'}). Payload
	 * yang mengandung kutip tunggal akan memutus kutipan itu, sehingga isi JSON bisa terbaca sebagai
	 * potongan perintah shell. Perlakukan sebagai temuan keamanan; dicatat, tidak diperbaiki di
	 * sini.</p>
	 *
	 * @param strURL                   URL endpoint yang di-POST
	 * @param username_va_e_smartlink  username Basic Auth e-Smartlink
	 * @param password_va_e_smartlink  password Basic Auth e-Smartlink
	 * @param postData                 badan permintaan JSON
	 * @return isi respons apa adanya, atau JSON {@code status=error} bila gagal
	 */
	public static String curlSmartlink(String strURL, String username_va_e_smartlink, String password_va_e_smartlink,
			JSONObject postData) {
		String hasil = "";
		Process p = null;
		BufferedReader reader = null;
		BufferedReader errorReader = null;
		try {
			String screet_key = DownloadTagihanSiswaBankOnline.getBasicAuthenticationHeader(username_va_e_smartlink,
					password_va_e_smartlink);
			if (Common.getKonfigurasi("curl_e_smartlink_via_server_lain", Konfigurasi.TIDAK_AKTIF).getNilai().trim()
					.equals(Konfigurasi.AKTIF)) {
				String ipServerLain = Common.getKonfigurasi("curl_e_smartlink_via_IP", "38.47.178.46").getNilai()
						.trim();
				String portServerLain = Common.getKonfigurasi("curl_e_smartlink_via_PORT", "22031").getNilai().trim();
				String userServerLain = Common.getKonfigurasi("curl_e_smartlink_via_USER", "zishof").getNilai().trim();

				String remoteCurlCmd = "curl -s -k -X POST '" + strURL + "' -H 'Content-Type: application/json' "
						+ "-H 'Accept: application/json' -H 'Authorization: Basic " + screet_key + "' --data-raw '"
						+ postData.toString() + "'";
				String[] command = { "ssh", "-p", portServerLain, "-o", "StrictHostKeyChecking=no",
						userServerLain + "@" + ipServerLain, remoteCurlCmd };
				p = new ProcessBuilder(command).start();
			} else {
				String[] command = { "curl", "-s", "-k", "-H", "Content-Type: application/json", "-H",
						"Accept: application/json", "-H", "Authorization: Basic " + screet_key, "-X", "POST", strURL,
						"--data-raw", postData.toString() };
				p = new ProcessBuilder(command).start();
			}

			reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null)
				builder.append(line).append(System.getProperty("line.separator"));
			hasil = builder.toString();

			errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			StringBuilder errorBuilder = new StringBuilder();
			while ((line = errorReader.readLine()) != null)
				errorBuilder.append(line);

			if (errorBuilder.length() > 0 && hasil.trim().isEmpty()) {
				System.out.println("ERROR CURL: " + errorBuilder.toString());
				hasil = "{\"status\":\"error\",\"message\":\"Gagal koneksi CURL/SSH proxy\"}";
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VirtualAccountBank.java:2241");
			hasil = "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
		} finally {
			try {
				if (reader != null)
					reader.close();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:2247");
			}
			try {
				if (errorReader != null)
					errorReader.close();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VirtualAccountBank.java:2252");
			}
			if (p != null)
				p.destroy();
		}
		return hasil;
	}


	/**
	 * Penanda ringkas "sudah terbayar" untuk kolom grid/laporan.
	 *
	 * <p>Lebih longgar daripada {@link #isSudahTerbayar(VirtualAccountBank)}: nilai {@code total}
	 * tidak diperiksa, dan {@code waktuBayar} ikut dihitung sebagai bukti. Karena itu jangan dipakai
	 * sebagai penjaga idempotensi pembayaran — gunakan hanya untuk tampilan.</p>
	 *
	 * <p>Perhatikan bahwa method ini memanggil {@link #getKegiatan()} dan {@link #getWaktuBayar()}
	 * yang keduanya dapat mengubah field.</p>
	 *
	 * @return {@code true} bila ada salah satu penanda pembayaran
	 */
	@Transient
	public boolean getTerbayarRingkas() {
		return getKegiatan() != null || getPembayaran() != null || getDeposit() != null || getWaktuBayar() != null;
	}

	/**
	 * Penanda ringkas "kadaluarsa" untuk kolom grid/laporan: batas kadaluarsa sudah lewat
	 * <b>dan</b> tagihan belum terbayar.
	 *
	 * @return {@code true} bila VA sudah kadaluarsa dan belum dibayar
	 * @see #getTerbayarRingkas()
	 */
	@Transient
	public boolean getKadaluarsaRingkas() {
		Date kadaluarsa = getKadaluarsa();
		return kadaluarsa != null && kadaluarsa.before(WaktuUtil.getDate()) && !getTerbayarRingkas();
	}

	/**
	 * Status VA dalam satu kata untuk ditampilkan di grid.
	 *
	 * <p>Urutan prioritasnya penting: {@code "Terjadi Kendala"} &rarr; {@code "Telah Bayar"} &rarr;
	 * {@code "Kadaluarsa"} &rarr; {@code "Belum Bayar"}. Artinya VA bermasalah tetap dilaporkan
	 * bermasalah walau sudah dibayar, dan VA yang sudah dibayar tidak pernah dilaporkan kadaluarsa.</p>
	 *
	 * @return salah satu dari {@code "Terjadi Kendala"}, {@code "Telah Bayar"},
	 *         {@code "Kadaluarsa"}, {@code "Belum Bayar"}
	 */
	@Transient
	public String getStatusVirtualAccountRingkas() {
		if (getTerjadiKendala()) {
			return "Terjadi Kendala";
		}
		if (getTerbayarRingkas()) {
			return "Telah Bayar";
		}
		if (getKadaluarsaRingkas()) {
			return "Kadaluarsa";
		}
		return "Belum Bayar";
	}

	/**
	 * Identitas pemilik tagihan dalam satu baris teks, untuk kolom grid dan cetakan.
	 *
	 * <p>Bentuknya {@code "<nomor induk> - <nama>"} menurut jalur pemilik yang pertama ditemukan:
	 * mahasiswa (NIM) &rarr; calon mahasiswa (nomor registrasi) &rarr; siswa (nomor induk) &rarr;
	 * calon siswa (nomor induk). Bagian yang kosong dilewati tanpa menyisakan pemisah menggantung.</p>
	 *
	 * <p>Bila tidak satu pun jalur terisi — atau terjadi kesalahan saat memuat relasi, yang hanya
	 * ditampilkan kepada admin — dipakai {@link #getNama()} sebagai cadangan.</p>
	 *
	 * @return identitas pemilik tagihan; tidak pernah {@code null}
	 */
	@Transient
	public String getNamaPemilikRingkas() {
		try {
			if (getMahasiswa() != null) {
				return (getMahasiswa().getNim() == null ? "" : getMahasiswa().getNim() + " - ")
						+ (getMahasiswa().getNama() == null ? "" : getMahasiswa().getNama());
			}
			if (getBiodataCalonMahasiswa() != null) {
				return (getBiodataCalonMahasiswa().getNoRegistrasi() == null ? ""
						: getBiodataCalonMahasiswa().getNoRegistrasi() + " - ")
						+ (getBiodataCalonMahasiswa().getNama() == null ? ""
								: getBiodataCalonMahasiswa().getNama());
			}
			if (getSiswa() != null) {
				return (getSiswa().getNomorInduk() == null ? "" : getSiswa().getNomorInduk() + " - ")
						+ (getSiswa().getNama() == null ? "" : getSiswa().getNama());
			}
			if (getCalonSiswa() != null) {
				return (getCalonSiswa().getNomorInduk() == null ? "" : getCalonSiswa().getNomorInduk() + " - ")
						+ (getCalonSiswa().getNama() == null ? "" : getCalonSiswa().getNama());
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return getNama() == null ? "" : getNama();
	}

	/**
	 * Varian {@link #getTotal()} untuk pemakaian di ekspresi EL/ZK.
	 *
	 * <p>Secara praktis <b>redundan</b>: {@link #getTotal()} sudah menormalkan {@code null} menjadi
	 * {@code 0.0}, sehingga pemeriksaan {@code null} di sini tidak akan pernah terpenuhi.
	 * Dipertahankan karena sudah dirujuk berkas tampilan.</p>
	 *
	 * @return nominal tagihan; tidak pernah {@code null}
	 */
	@Transient
	public Double getTotalAman() {
		Double total = getTotal();
		return total == null ? 0.0 : total;
	}

}
