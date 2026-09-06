package ais.action.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.helper.KegiatanPersistenceHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.report.CommonReportHelper;
import ais.action.ws.PembayaranAction;
import ais.action.ws.model.Response;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankHost;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.LogHostToHost;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.VirtualAccountBank;
import ais.database.model.sekolah.PembayaranSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Tagihan;
import ais.ui.util.WaktuUtil;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.criteria.AuditCriterion;

/**
 * Servlet Host-to-Host (H2H) Virtual Account: satu-satunya pintu masuk HTTP mentah tempat host
 * bank menanyakan tagihan, mengonfirmasi setoran, dan membatalkan setoran mahasiswa/siswa.
 *
 * <h2>Pemetaan URL</h2>
 * <p>Kelas ini terdaftar dua kali di {@code WEB-INF/web.xml}: sebagai servlet {@code Va} yang
 * dipetakan ke {@code /Va}, dan sebagai servlet {@code notif} (display-name {@code Va/notif}).
 * Kedua entri menunjuk kelas yang sama sehingga perilakunya identik.</p>
 *
 * <h2>Kontrak protokol</h2>
 * <p>Permintaan boleh datang sebagai badan JSON (dibaca utuh di {@link #process}) atau sebagai
 * parameter query biasa; {@link #getSafeParam} membaca JSON lebih dulu lalu jatuh ke parameter
 * request. Kunci yang dikenali: {@code action}, {@code va}, {@code kode}, {@code smt},
 * {@code bank}, {@code nominal}, {@code tanggal}, dan array {@code bayar}. Nilai {@code action}
 * yang didukung:</p>
 * <ul>
 *   <li>{@code tagihan} &mdash; kembalikan seluruh rincian tagihan satu NIM/no-registrasi pada
 *       satu semester (BLOK 1 di {@link #doProses});</li>
 *   <li>{@code simpan_tagihan} &mdash; buat/perbarui baris {@link VirtualAccountBank} dari
 *       keranjang item yang dikirim pemanggil, lalu kembalikan nomor VA-nya (BLOK 2);</li>
 *   <li>{@code inquiry}, {@code payment}, {@code reversal} &mdash; alur H2H standar atas satu
 *       nomor VA (BLOK 3).</li>
 * </ul>
 *
 * <p>Dua bank memakai skema payload sendiri yang dideteksi dari bentuk JSON-nya, bukan dari
 * field {@code bank}: kehadiran {@code revflag}/{@code reserve} menandai <b>Bank BTN</b>, dan
 * kehadiran {@code va_acc_no} menandai <b>Bank BJBS</b>. Untuk kedua bank ini badan balasan
 * ditulis ulang di akhir {@link #doProses} memakai format respons khas bank tersebut
 * ({@code ref}/{@code rsp}/{@code spdesc} untuk BTN, {@code rc}/{@code rm} untuk BJBS).</p>
 *
 * <h2>Kode status balasan</h2>
 * <table border="1" summary="Arti kode status pada JSON balasan">
 *   <tr><th>status</th><th>arti</th></tr>
 *   <tr><td>{@code 00}</td><td>sukses (inquiry/payment/reversal berhasil, atau tagihan ditemukan)</td></tr>
 *   <tr><td>{@code 01}</td><td>nomor pembayaran tidak ditemukan (nilai awal sebelum diproses)</td></tr>
 *   <tr><td>{@code 02}</td><td>gagal / tagihan belum dibayar / data mahasiswa pada VA tidak ada</td></tr>
 *   <tr><td>{@code 03}</td><td>request salah ({@code action} tidak dikenali)</td></tr>
 *   <tr><td>{@code 04}</td><td>nominal pembayaran tidak sesuai total tagihan</td></tr>
 *   <tr><td>{@code 05}</td><td>tagihan telah dibayar / kadaluarsa / sistem sedang sibuk</td></tr>
 *   <tr><td>{@code 06}</td><td>tagihan tidak ditemukan</td></tr>
 *   <tr><td>{@code 08}</td><td>kesalahan tak terduga (pesan exception disertakan)</td></tr>
 * </table>
 *
 * <h2>Jejak audit</h2>
 * <p>Setiap permintaan yang masuk BLOK 3 dicatat ke {@link LogHostToHost} dari blok
 * {@code finally} &mdash; lihat {@link #doProses} &mdash; sehingga tercatat walau prosesnya
 * gagal. Kelas ini mengisi {@code keterangan} (badan permintaan mentah), {@code ip},
 * {@code bankHost}, {@code nim}, {@code kode}, {@code nama}, {@code tanggal},
 * {@code responseDescription}, {@code nominal}, dan {@code item}; penyimpanannya didelegasikan
 * ke {@code PembayaranGatewayHelper.simpanLogHostToHost} yang memakai session terdedikasi
 * berikut commit dan retry sendiri. Perlu dicatat bahwa {@code LogHostToHost} juga punya kolom
 * penampung header HTTP (termasuk {@code Authorization}); kolom-kolom itu <b>tidak</b> diisi
 * dari kelas ini, melainkan dari jalur pencatatan lain.</p>
 *
 * <h2>Catatan keamanan (FAKTA arsitektur, sudah terdaftar sebagai temuan)</h2>
 * <p><b>Endpoint ini tidak punya gerbang otentikasi sama sekali.</b> Tidak ada
 * {@code doCheckSecurity()}, tidak ada pemeriksaan Basic/Bearer, tidak ada verifikasi tanda
 * tangan payload, dan tidak ada pemeriksaan sesi. {@code applicationContext-security.xml}
 * memetakan pola penadah {@code /**} ke {@code IS_AUTHENTICATED_ANONYMOUSLY} dan {@code /Va}
 * tidak muncul di satu pun pola yang mensyaratkan otentikasi, sehingga siapa pun di jaringan
 * yang bisa menjangkau aplikasi dapat memanggilnya.</p>
 *
 * <p>Satu-satunya kontrol yang tersisa adalah pencocokan IP: {@link #process} memanggil
 * {@code PembayaranUtil.getBankHost(request.getRemoteAddr(), bank)} untuk mencari baris
 * {@link BankHost} yang IP-nya sama dengan alamat pemanggil. Kontrol itu <b>lunak</b>, bukan
 * gerbang: bila tidak ada yang cocok, helper tersebut mencoba membuat {@link BankHost} baru
 * secara otomatis (bila konfigurasi
 * {@code apabila_bank_host_tidak_ditemukan_buat_data_bank_otomatis} aktif) lalu jatuh ke baris
 * wildcard ber-IP {@code 0.0.0.0}; dan bila akhirnya tetap {@code null}, {@link #doProses}
 * tetap berjalan karena {@code VirtualAccountBank.ambilVa} mencocokkan VA "netral"
 * ({@code bankHost IS NULL}) untuk host apa pun. Jadi {@code bankHost} yang kosong <b>tidak</b>
 * menolak permintaan.</p>
 *
 * <p>Konsekuensi yang perlu diketahui pembaca kode ini:</p>
 * <ul>
 *   <li>{@code action=tagihan} adalah <b>orakel data</b>: cukup satu NIM/no-registrasi dan
 *       nomor semester untuk memperoleh nama, fakultas, program studi, dan seluruh rincian
 *       tagihan beserta nominalnya, tanpa syarat apa pun;</li>
 *   <li>{@code action=inquiry} melakukan hal serupa berdasarkan nomor VA;</li>
 *   <li>{@code action=simpan_tagihan} adalah jalur <b>tulis</b> anonim &mdash; pemanggil dapat
 *       membuat baris {@link VirtualAccountBank} baru untuk mahasiswa mana pun;</li>
 *   <li>{@code action=payment} dan {@code action=reversal} mengubah status keuangan: yang
 *       pertama membuat {@link Kegiatan} tervalidasi berikut {@link CicilanPembayaran}, yang
 *       kedua menghapus baris {@code cicilan_pembayaran} milik VA tersebut.</li>
 * </ul>
 *
 * <p>Karena itu perlindungan nyata endpoint ini bergantung sepenuhnya pada pembatasan jaringan
 * di depan aplikasi (firewall/allowlist IP host bank). Jangan menambah cabang baru di kelas ini
 * dengan asumsi pemanggilnya sudah tepercaya.</p>
 *
 * @see VirtualAccountBank
 * @see LogHostToHost
 * @see BankHost
 * @see PembayaranUtil
 */
public class Va extends HttpServlet {
	/** Versi serial standar {@link HttpServlet}; tidak dipakai untuk logika apa pun. */
	private static final long serialVersionUID = 1L;

	/**
	 * Singleton util pembayaran, dipakai untuk memetakan IP pemanggil menjadi {@link BankHost}
	 * dan untuk menghitung total serta denda dari daftar {@link CicilanPembayaran}.
	 */
	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/** Konstruktor bawaan kontainer servlet; tidak melakukan inisialisasi tambahan. */
	public Va() {
		super();
	}

	/**
	 * Menangani permintaan GET dengan mendelegasikannya ke {@link #process}.
	 *
	 * <p>GET dan POST diperlakukan <b>sama persis</b> &mdash; keduanya membaca badan permintaan
	 * dan parameter query lalu memanggil {@link #doProses}. Tidak ada pemeriksaan otentikasi di
	 * sini; lihat catatan keamanan pada dokumentasi kelas.</p>
	 *
	 * <p>Semua exception ditelan dan hanya ditampilkan kepada admin lewat
	 * {@code Common.tampilErrorJikaAdmin}, sehingga kegagalan tidak pernah bocor sebagai HTTP
	 * 500 ke host bank.</p>
	 *
	 * @param request  permintaan HTTP masuk
	 * @param response balasan HTTP yang akan diisi JSON hasil proses
	 * @throws ServletException tidak pernah dilempar oleh implementasi ini
	 * @throws IOException      bila penulisan balasan gagal
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menangani permintaan POST &mdash; jalur yang sesungguhnya dipakai host bank &mdash; dengan
	 * mendelegasikannya ke {@link #process}.
	 *
	 * <p>Implementasinya identik dengan {@link #doGet}: tanpa otentikasi, tanpa verifikasi
	 * tanda tangan payload, dan menelan seluruh exception.</p>
	 *
	 * @param request  permintaan HTTP masuk; badannya berisi JSON dari host bank
	 * @param response balasan HTTP yang akan diisi JSON hasil proses
	 * @throws ServletException tidak pernah dilempar oleh implementasi ini
	 * @throws IOException      bila penulisan balasan gagal
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Pintasan {@link #doProses(String, HttpServletRequest, BankHost, boolean)} dengan
	 * {@code tetapberhasil} bernilai {@code false}, yaitu mode normal tempat nominal setoran
	 * <b>wajib</b> sama dengan total tagihan VA.
	 *
	 * <p>Dipakai oleh servlet callback bank lain yang meneruskan payload-nya ke mesin H2H ini.</p>
	 *
	 * @param data     badan permintaan mentah berformat JSON; boleh {@code null}/kosong sehingga
	 *                 seluruh parameter dibaca dari query string
	 * @param request  permintaan HTTP asal, dipakai untuk membaca parameter dan alamat IP
	 * @param bankHost host bank pemanggil hasil pencocokan IP; boleh {@code null}
	 * @return badan balasan JSON siap kirim
	 * @throws Exception bila terjadi kegagalan yang tidak tertangani di dalam mesin H2H
	 */
	public static String doProses(String data, HttpServletRequest request, BankHost bankHost) throws Exception {
		return doProses(data, request, bankHost, false);
	}

	/**
	 * Mesin utama servlet: mengurai payload bank, menjalankan salah satu dari tiga blok
	 * pemrosesan, mencatat jejak {@link LogHostToHost}, lalu mengembalikan badan balasan JSON.
	 *
	 * <h4>Tahap 1 &mdash; normalisasi payload</h4>
	 * <p>Parameter {@code kode}, {@code smt}, {@code va}, {@code action}, {@code bank},
	 * {@code nominal}, dan {@code tanggal} dibaca lewat {@link #getSafeParam} (JSON dulu, lalu
	 * query string), sementara array {@code bayar} hanya dibaca dari JSON. Sesudah itu dua
	 * bentuk payload khusus bank dinormalkan menjadi bentuk baku:</p>
	 * <ul>
	 *   <li><b>Bank BTN</b> &mdash; dikenali dari kehadiran kunci {@code revflag} atau
	 *       {@code reserve}. {@code bank} dipaksa {@code "Bank BTN"}, {@code action} dipaksa
	 *       {@code "payment"}, nominal diambil dari {@code terbayar}, dan tanggal disusun dari
	 *       gabungan {@code paymentdate} dan {@code paymenttime};</li>
	 *   <li><b>Bank BJBS</b> &mdash; dikenali dari kehadiran kunci {@code va_acc_no}. Nomor VA
	 *       diambil dari kunci itu, {@code bank} dipaksa {@code "Bank BJBS"}, {@code action}
	 *       dipaksa {@code "payment"}, dan nominal diambil dari {@code amount}.</li>
	 * </ul>
	 *
	 * <h4>Tahap 2 &mdash; pemilihan blok</h4>
	 * <ol>
	 *   <li><b>BLOK 1 ({@code action=tagihan})</b> &mdash; syarat: {@code kode} terisi dan
	 *       {@code smt} berupa angka. Mencari {@link Mahasiswa} aktif berdasarkan NIM; bila tidak
	 *       ada, mencari {@link BiodataCalonMahasiswa} aktif berdasarkan nomor registrasi. Rincian
	 *       tagihan diisi oleh {@code CommonReportHelper.populateTagihanMahasiswa} atau
	 *       {@code populateTagihanCalonMahasiswa} (jenis kegiatan pendaftaran ulang bila calon
	 *       sudah punya prodi lulus, jenis pendaftaran calon bila belum). Balasan memuat NIM,
	 *       nama, fakultas, prodi, semester, {@code total}, dan array {@code rincian};</li>
	 *   <li><b>BLOK 2 ({@code action=simpan_tagihan})</b> &mdash; syarat tambahan: array
	 *       {@code bayar} tidak kosong. Item pembayaran dikelompokkan per {@code jenis_id},
	 *       lalu untuk tiap kelompok dibuat/diperbarui satu {@link VirtualAccountBank}. Kolom
	 *       {@code cicilan} diisi token {@code Bulanan-<id>-<nilai>} atau
	 *       {@code Item-<idItem>-<nilai>-<detailBiaya>-<idDetailBiaya>}, kolom {@code keterangan}
	 *       diisi pasangan {@code kode,deskripsi;}. Nomor VA hasilnya dikembalikan di
	 *       {@code rincian};</li>
	 *   <li><b>BLOK 3 (selain keduanya)</b> &mdash; alur H2H {@code inquiry}, {@code payment},
	 *       dan {@code reversal} atas satu nomor VA; lihat rincian di bawah.</li>
	 * </ol>
	 *
	 * <h4>Tahap 3 &mdash; alur H2H pada BLOK 3</h4>
	 * <p>VA dicari lewat {@code VirtualAccountBank.ambilVa(va, nominal, bankHost)}. Bila VA sudah
	 * berstatus terbayar, balasan langsung berstatus {@code 05}. Bila VA tidak ketemu tetapi
	 * konfigurasi {@code pembayaran_via_va_bisa_berdasarkan_nim} aktif, nomor yang dikirim
	 * diperlakukan sebagai NIM dan diproses lewat {@code PembayaranAction.inquiryByNim} atau
	 * {@code payByNim}. Selain itu {@code inquiry}, {@code payment}, dan {@code reversal}
	 * diteruskan ke {@link #prosesH2HInquiry}, {@link #prosesH2HPayment}, dan
	 * {@link #prosesH2HReversal}. Kedaluwarsa VA diperiksa lewat {@link #isExpired} untuk
	 * {@code inquiry} dan {@code payment} &mdash; <b>tidak</b> untuk {@code reversal}.</p>
	 *
	 * <p>Kegagalan basis data sementara (mis. {@code lock timeout} saat banyak notifikasi bank
	 * masuk bersamaan) ditangkap di dalam BLOK 3 dan diubah menjadi balasan JSON berstatus
	 * {@code 05} "sistem sedang sibuk", supaya host bank menerima kontrak API yang sah dan bisa
	 * mengulang, bukan HTTP 500 mentah.</p>
	 *
	 * <h4>Tahap 4 &mdash; jejak audit dan balasan khusus bank</h4>
	 * <p>Blok {@code finally} BLOK 3 selalu menyimpan {@link LogHostToHost} lewat
	 * {@code PembayaranGatewayHelper.simpanLogHostToHost}. Sesudahnya, bila {@code bank} bernilai
	 * {@code "Bank BTN"} atau {@code "Bank BJBS"}, badan balasan <b>ditimpa seluruhnya</b> dengan
	 * format ringkas khas bank tersebut, sehingga status rinci di atas tidak terlihat oleh kedua
	 * bank itu.</p>
	 *
	 * <h4>Catatan keamanan</h4>
	 * <p>Method ini tidak melakukan otentikasi apa pun; lihat dokumentasi kelas. Selain itu perlu
	 * disadari bahwa {@code bank} berasal dari payload yang dikirim pemanggil, sedangkan
	 * {@code tetapberhasil} di {@link #process} diturunkan dari nilai {@code bank} tersebut.
	 * Artinya jalur yang melewati pencocokan nominal (lihat parameter {@code tetapberhasil})
	 * dipilih oleh data pemanggil, bukan oleh identitas pemanggil yang terverifikasi.</p>
	 *
	 * @param data          badan permintaan mentah berformat JSON; boleh {@code null}/kosong
	 * @param request       permintaan HTTP asal; boleh {@code null} bila seluruh parameter ada di
	 *                      dalam {@code data}
	 * @param bankHost      host bank pemanggil hasil pencocokan IP; boleh {@code null}, dan nilai
	 *                      {@code null} <b>tidak</b> menghentikan pemrosesan
	 * @param tetapberhasil bila {@code true}, pemeriksaan "nominal setoran harus sama dengan total
	 *                      tagihan VA" pada {@code action=payment} <b>dilewati</b> sehingga
	 *                      pembayaran tetap diposting; dipakai untuk protokol Bank BTN yang
	 *                      mengirim konfirmasi tanpa nominal yang sepadan
	 * @return badan balasan JSON siap kirim; tidak pernah {@code null}
	 * @throws Exception bila terjadi kegagalan yang tidak tertangani di luar blok yang dilindungi
	 * @see #prosesH2HInquiry
	 * @see #prosesH2HPayment
	 * @see #prosesH2HReversal
	 */
	public static String doProses(String data, HttpServletRequest request, BankHost bankHost, boolean tetapberhasil)
			throws Exception {
		System.out.println("[VA Servlet] ================= MEMULAI PROSES REQUEST =================");
		LogHostToHost logHostToHost = new LogHostToHost();

		// Mencegah NullPointerException jika data kosong
		JSONObject req = (data != null && !data.trim().isEmpty()) ? new JSONObject(data) : null;

		String kode = getSafeParam(req, request, "kode");
		String smt = getSafeParam(req, request, "smt");
		JSONArray bayar = (req != null && !req.isNull("bayar")) ? req.getJSONArray("bayar") : null;

		String va = getSafeParam(req, request, "va");
		String action = getSafeParam(req, request, "action");
		String bank = getSafeParam(req, request, "bank");
		String nominalP = getSafeParam(req, request, "nominal");
		String tanggalP = getSafeParam(req, request, "tanggal");

		// Handle khusus BTN
		if (req != null && (!req.isNull("revflag") || !req.isNull("reserve"))) {
			System.out.println("[VA Servlet] Request terdeteksi sebagai Bank BTN (revflag/reserve).");
			bank = "Bank BTN";
			action = "payment";
			try {
				nominalP = String.valueOf(req.get("terbayar"));
			} catch (Exception e) {
				nominalP = String.valueOf(req.optInt("terbayar", 0));
			}
			if (!req.isNull("paymentdate") && !req.isNull("paymenttime")) {
				tanggalP = req.optString("paymentdate", "") + req.optString("paymenttime", "");
			}
		}

		// Handle khusus BJBS
		if (req != null && !req.isNull("va_acc_no")) {
			System.out.println("[VA Servlet] Request terdeteksi sebagai Bank BJBS.");
			va = req.optString("va_acc_no", va);
			bank = "Bank BJBS";
			action = "payment";
			nominalP = String.valueOf(req.optInt("amount", 0));
		}

		System.out.println("[VA Servlet] Data Parsed -> Action: " + action + " | VA: " + va + " | Kode: " + kode
				+ " | Bank: " + bank);

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("status", "01");
		jsonObject.put("description", "Nomor Pembayaran Tidak Ditemukan");
		String body = jsonObject.toString();

		String nim = "";
		String nama = "";
		JSONArray rincian = new JSONArray();
		VirtualAccountBank virtualAccountBankNtt = null;

		Session session = null;

		// =========================================================================================
		// BLOK 1: ACTION TAGIHAN
		// =========================================================================================
		if (action != null && action.trim().equalsIgnoreCase("tagihan") && kode != null && !kode.trim().isEmpty()
				&& smt != null && !smt.trim().isEmpty() && Common.isNumber(smt)) {

			System.out.println("[VA Servlet] Memproses action: TAGIHAN untuk kode: " + kode);
			List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
			Double total = 0.0;
			Integer semester = Integer.parseInt(smt.trim());

			try {
				session = HibernateUtil.openSession();

				Mahasiswa mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.ilike("nim", kode.trim(), MatchMode.EXACT)).setMaxResults(1).uniqueResult();

				if (mahasiswa != null) {
					CommonReportHelper.populateTagihanMahasiswa(mahasiswa, semester, maps);
					jsonObject.put("nim", mahasiswa.getNim());
					jsonObject.put("nama", mahasiswa.getNama());
					jsonObject.put("fakultas", getSafeFakultas(mahasiswa));
					jsonObject.put("prodi", getSafeProdi(mahasiswa));
				} else {
					BiodataCalonMahasiswa calonMahasiswa = (BiodataCalonMahasiswa) session
							.createCriteria(BiodataCalonMahasiswa.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.ilike("noRegistrasi", kode.trim(), MatchMode.EXACT)).setMaxResults(1)
							.uniqueResult();

					if (calonMahasiswa != null) {
						jsonObject.put("nim", calonMahasiswa.getNoRegistrasi());
						jsonObject.put("nama", calonMahasiswa.getNama());

						if (calonMahasiswa.getProdiLulus() != null) {
							jsonObject.put("fakultas", getSafeFakultasLulus(calonMahasiswa));
							jsonObject.put("prodi", getSafeProdiLulus(calonMahasiswa));
							Kegiatan kegiatan = calonMahasiswa.ambilKegiatans(semester,
									ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU);
							CommonReportHelper.populateTagihanCalonMahasiswa(calonMahasiswa,
									ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU, kegiatan, semester, maps);
						} else if (calonMahasiswa.getProdi1() != null) {
							jsonObject.put("fakultas",
									calonMahasiswa.getProdi1().getFakultas() != null
											? calonMahasiswa.getProdi1().getFakultas().getNama()
											: "");
							jsonObject.put("prodi", calonMahasiswa.getProdi1().getNama());
							Kegiatan kegiatan = calonMahasiswa.ambilKegiatans(semester,
									ConstantValues.PENDAFTARAN_CALON_MAHASISWA);
							CommonReportHelper.populateTagihanCalonMahasiswa(calonMahasiswa,
									ConstantValues.PENDAFTARAN_CALON_MAHASISWA, kegiatan, semester, maps);
						}
					}
				}

				jsonObject.put("status", maps.isEmpty() ? "06" : "00");
				jsonObject.put("description", maps.isEmpty() ? "Tagihan Tidak Ditemukan" : "Tagihan Ditemukan");
				jsonObject.put("semester", semester);

				for (Map<String, Object> map : maps) {
					PengaturanPembayaranBulanan ppBulan = (PengaturanPembayaranBulanan) map
							.get("pengaturanPembayaranBulanan");
					DetailBiaya detailBiaya = (DetailBiaya) map.get("detailBiaya");
					Double subtotal = (Double) map.get("nilai");

					JSONObject jsonObjectRinci = new JSONObject();
					if (ppBulan != null) {
						jsonObjectRinci.put("id", ppBulan.getId());
						jsonObjectRinci.put("jenis_id", ppBulan.getDetailBiaya().getJenisKegiatan().getId());
						jsonObjectRinci.put("item_id", ppBulan.getDetailBiaya().getItemBiaya().getId());
						jsonObjectRinci.put("jenis_nama",
								ppBulan.getDetailBiaya().getJenisKegiatan().getNamaKegiatan());
						jsonObjectRinci.put("nama", ppBulan.getDetailBiaya().getItemBiaya().getNama());
						jsonObjectRinci.put("kode", ppBulan.getDetailBiaya().getItemBiaya().getKode());
						jsonObjectRinci.put("bulan", ppBulan.getNamaBulan());
						jsonObjectRinci.put("bulanan", "ya");
						jsonObjectRinci.put("boleh_diubah",
								ppBulan.getDetailBiaya().getItemBiaya().getNilaiBisaDiubah());
					} else if (detailBiaya != null) {
						jsonObjectRinci.put("id", detailBiaya.getId());
						jsonObjectRinci.put("jenis_id", detailBiaya.getJenisKegiatan().getId());
						jsonObjectRinci.put("item_id", detailBiaya.getItemBiaya().getId());
						jsonObjectRinci.put("jenis_nama", detailBiaya.getJenisKegiatan().getNamaKegiatan());
						jsonObjectRinci.put("nama", detailBiaya.getItemBiaya().getNama());
						jsonObjectRinci.put("kode", detailBiaya.getItemBiaya().getKode());
						jsonObjectRinci.put("bulan", "");
						jsonObjectRinci.put("bulanan", "tidak");
						jsonObjectRinci.put("boleh_diubah", detailBiaya.getItemBiaya().getNilaiBisaDiubah());
					}

					if (ppBulan != null || detailBiaya != null) {
						jsonObjectRinci.put("label", map.get("label"));
						total += subtotal;
						jsonObjectRinci.put("nominal", subtotal);
						rincian.put(jsonObjectRinci);
					}
				}

				jsonObject.put("total", total);
				jsonObject.put("rincian", rincian);
				body = jsonObject.toString();

			} catch (Exception e) {
				HibernateUtil.rollbackTransaction();
				jsonObject.put("status", "08");
				jsonObject.put("description", "Terjadi kesalahan " + e.getMessage());
				body = jsonObject.toString();
				Common.tampilErrorJikaAdmin(e);
			} finally {
				KegiatanPersistenceHelper.closeNativeSession(session);
			}

			// =========================================================================================
			// BLOK 2: ACTION SIMPAN TAGIHAN
			// =========================================================================================
		} else if (action != null && action.trim().equalsIgnoreCase("simpan_tagihan") && kode != null
				&& !kode.trim().isEmpty() && bayar != null && bayar.length() > 0 && smt != null && !smt.trim().isEmpty()
				&& Common.isNumber(smt)) {

			System.out.println("[VA Servlet] Memproses action: SIMPAN_TAGIHAN untuk kode: " + kode);
			Integer semester = Integer.parseInt(smt.trim());

			try {
				session = HibernateUtil.openSession();
				Map<Long, List<JSONObject>> dataBerdasarJenisPembayaran = new HashMap<Long, List<JSONObject>>();

				for (int index = 0; index < bayar.length(); index++) {
					JSONObject obj = bayar.getJSONObject(index);
					Long jenis_id = ais.common.CommonJSONUtil.ambilLong(obj, "jenis_id");
					if (dataBerdasarJenisPembayaran.containsKey(jenis_id)) {
						dataBerdasarJenisPembayaran.get(jenis_id).add(obj);
					} else {
						List<JSONObject> jsonObjects = new ArrayList<JSONObject>();
						jsonObjects.add(obj);
						dataBerdasarJenisPembayaran.put(jenis_id, jsonObjects);
					}
				}

				Mahasiswa mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.ilike("nim", kode.trim(), MatchMode.EXACT)).setMaxResults(1).uniqueResult();

				BiodataCalonMahasiswa calonMahasiswa = null;

				if (mahasiswa != null) {
					jsonObject.put("nim", mahasiswa.getNim());
					jsonObject.put("nama", mahasiswa.getNama());
					jsonObject.put("fakultas", getSafeFakultas(mahasiswa));
					jsonObject.put("prodi", getSafeProdi(mahasiswa));
				} else {
					calonMahasiswa = (BiodataCalonMahasiswa) session.createCriteria(BiodataCalonMahasiswa.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.ilike("noRegistrasi", kode.trim(), MatchMode.EXACT)).setMaxResults(1)
							.uniqueResult();
					if (calonMahasiswa != null) {
						jsonObject.put("nim", calonMahasiswa.getNoRegistrasi());
						jsonObject.put("nama", calonMahasiswa.getNama());
						if (calonMahasiswa.getProdiLulus() != null) {
							jsonObject.put("fakultas", getSafeFakultasLulus(calonMahasiswa));
							jsonObject.put("prodi", getSafeProdiLulus(calonMahasiswa));
						} else if (calonMahasiswa.getProdi1() != null) {
							jsonObject.put("fakultas",
									calonMahasiswa.getProdi1().getFakultas() != null
											? calonMahasiswa.getProdi1().getFakultas().getNama()
											: "");
							jsonObject.put("prodi", calonMahasiswa.getProdi1().getNama());
						}
					}
				}

				for (Long key : dataBerdasarJenisPembayaran.keySet()) {
					List<JSONObject> jsonObjects = dataBerdasarJenisPembayaran.get(key);

					// Gunakan StringBuilder untuk efisiensi memori (Penting!)
					StringBuilder pemb = new StringBuilder();
					StringBuilder cicilan = new StringBuilder();
					StringBuilder detailbiaya = new StringBuilder();
					Double total = 0.0;

					for (JSONObject o : jsonObjects) {
						Long itemId = ais.common.CommonJSONUtil.ambilLong(o, "item_id");
						if (detailbiaya.length() == 0)
							detailbiaya.append(itemId);
						else
							detailbiaya.append(",").append(itemId);

						Long id = ais.common.CommonJSONUtil.ambilLong(o, "id");

						if (o.getString("bulanan").equalsIgnoreCase("ya")) {
							PengaturanPembayaranBulanan biaya = (PengaturanPembayaranBulanan) session
									.createCriteria(PengaturanPembayaranBulanan.class).add(Restrictions.idEq(id))
									.uniqueResult();

							if (biaya != null) {
								Double nilai = o.getDouble("nominal");
								if (cicilan.length() > 0)
									cicilan.append(",");
								cicilan.append("Bulanan-").append(biaya.getId()).append("-").append(nilai);

								Double hasilDenda = biaya.checkDenda(nilai, ais.ui.util.WaktuUtil.getDate(), null,
										null);
								String desc = biaya.getKeterangan();
								desc = (desc.isEmpty() ? biaya.getDetailBiaya().getItemBiaya().getNama() : desc)
										+ ", Rp. " + Common.numberFormat.get().format(nilai)
										+ (hasilDenda.intValue() > nilai.intValue() ? biaya.getInfoDenda() : "");

								pemb.append(biaya.getDetailBiaya().getItemBiaya().getKode().trim()).append(",")
										.append(desc).append(";");
								total += nilai;
							}
						} else {
							DetailBiaya detailBiaya = (DetailBiaya) session.createCriteria(DetailBiaya.class)
									.setMaxResults(1).add(Restrictions.idEq(id)).uniqueResult();

							if (detailBiaya != null) {
								Double nilai = o.getDouble("nominal");
								if (cicilan.length() > 0)
									cicilan.append(",");
								cicilan.append("Item-").append(detailBiaya.getItemBiaya().getId()).append("-")
										.append(nilai).append("-").append(detailBiaya).append("-")
										.append(detailBiaya.getId());

								ItemBiaya itemBiaya = detailBiaya.getItemBiaya();
								String desc = itemBiaya.getNama() + ", Rp. " + Common.numberFormat.get().format(nilai);
								pemb.append(itemBiaya.getKode().trim()).append(",").append(desc).append(";");
								total += nilai;
							}
						}
					}

					VirtualAccountBank vaBaru = (VirtualAccountBank) session.createCriteria(VirtualAccountBank.class)
							.add(Restrictions.or(Restrictions.isNull("bankHost"),
									Restrictions.eq("bankHost", bankHost)))
							.add(Restrictions.eq("keterangan", pemb.toString()))
							.add(Restrictions.eq("mahasiswa", mahasiswa)).add(Restrictions.eq("semester", semester))
							.add(Restrictions.eq("jenisKegiatan.id", key)).add(Restrictions.isNull("kegiatan"))
							.setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();

					if (vaBaru == null) {
						PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
						vaBaru = new VirtualAccountBank(perguruanTinggi.getId());
					}

					vaBaru.setCicilan(cicilan.toString());
					vaBaru.setJenisKegiatan(new JenisKegiatan(key));
					vaBaru.setKeterangan(pemb.toString());
					vaBaru.setTotal(total);
					vaBaru.setBulanan("");
					vaBaru.setDetailbiaya(detailbiaya.toString());
					vaBaru.setBiodataCalonMahasiswa(calonMahasiswa);
					vaBaru.setMahasiswa(mahasiswa);
					vaBaru.setJadwalPembayaran(null);
					vaBaru.setSemester(semester);
					vaBaru.setBank("Bank");

					session.getTransaction().begin();
					session.saveOrUpdate(vaBaru);
					session.getTransaction().commit();

					JSONObject jsonObjectRinci = new JSONObject();
					jsonObjectRinci.put("va", vaBaru.getKode());
					rincian.put(jsonObjectRinci);
				}

				jsonObject.put("rincian", rincian);
				body = jsonObject.toString();

			} catch (Exception e) {
				HibernateUtil.rollbackTransaction();
				jsonObject.put("status", "08");
				jsonObject.put("description", "Terjadi kesalahan " + e.getMessage());
				body = jsonObject.toString();
				Common.tampilErrorJikaAdmin(e);
			} finally {
				KegiatanPersistenceHelper.closeNativeSession(session);
			}

			// =========================================================================================
			// BLOK 3: ACTION INQUIRY, PAYMENT, REVERSAL
			// =========================================================================================
		} else {
			System.out.println("[VA Servlet] Memproses H2H Transaksi. Action: " + action + " | VA: " + va);
			try {
				Double nominal = -1.0;
				try {
					if (nominalP != null && !nominalP.isEmpty())
						nominal = Double.parseDouble(nominalP);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Va.java:442");
				}

				virtualAccountBankNtt = VirtualAccountBank.ambilVa(va, nominal, bankHost);

				if (virtualAccountBankNtt != null
						&& VirtualAccountBank.isSudahTerbayar(virtualAccountBankNtt)) {
					System.out.println("[VA Servlet] Tagihan VA " + va + " telah terbayar sebelumnya.");
					jsonObject = new JSONObject();
					jsonObject.put("status", "05");
					jsonObject.put("description", "Tagihan Telah Dibayar");
					body = jsonObject.toString();
				} else {
					Response response = null;
					boolean viaNimAktif = Common.bolehKonfigurasi("pembayaran_via_va_bisa_berdasarkan_nim", Konfigurasi.TIDAK_AKTIF);

					// LOGIKA BAYAR VIA NIM
					if (virtualAccountBankNtt == null && viaNimAktif) {
						System.out.println("[VA Servlet] Mencoba transaksi H2H By NIM. VA/NIM: " + va);
						if (action != null && action.equalsIgnoreCase("inquiry")) {
							response = PembayaranAction.inquiryByNim(va, bankHost, nama, logHostToHost);
							jsonObject = mapResponseToJson(response, rincian, "Tagihan Semester",
									Double.parseDouble(response.getTotal_amount()));
							body = jsonObject.toString();
						} else if (action != null && action.equalsIgnoreCase("payment")) {
							response = PembayaranAction.payByNim(va, bankHost, nama, logHostToHost, nominalP);
							jsonObject = mapResponseToJson(response, rincian, "Tagihan Semester",
									Double.parseDouble(nominalP));
							body = jsonObject.toString();
						}
					}

					// LOGIKA STANDAR VIRTUAL ACCOUNT
					if (response == null) {
						if (virtualAccountBankNtt != null && virtualAccountBankNtt.getTotal() > 0.1) {

							try {
								session = HibernateUtil.openSession();

								if (action != null && action.equalsIgnoreCase("inquiry")) {
									System.out.println("[VA Servlet] H2H Inquiry untuk VA: " + va);
									if (isExpired(virtualAccountBankNtt)) {
										jsonObject = generateExpiredResponse();
										body = jsonObject.toString();
									} else {
										body = prosesH2HInquiry(virtualAccountBankNtt, session, bank, data, jsonObject,
												rincian, nim, nama);
									}

								} else if (action != null && action.equalsIgnoreCase("payment")) {
									System.out.println("[VA Servlet] H2H Payment untuk VA: " + va);
									if (isExpired(virtualAccountBankNtt)) {
										jsonObject = generateExpiredResponse();
										body = jsonObject.toString();
									} else if (!tetapberhasil
											&& (nominal.intValue() != virtualAccountBankNtt.totalBiaya())) {
										jsonObject = new JSONObject();
										jsonObject.put("status", "04");
										jsonObject.put("description", "Nominal Pembayaran Tidak Sesuai");
										body = jsonObject.toString();
									} else {
										body = prosesH2HPayment(virtualAccountBankNtt, session, bank, data, jsonObject,
												rincian, nim, nama, tanggalP, bankHost, tetapberhasil, nominal);
									}

								} else if (action != null && action.equalsIgnoreCase("reversal")) {
									System.out.println("[VA Servlet] H2H Reversal untuk VA: " + va);
									body = prosesH2HReversal(virtualAccountBankNtt, session, bank, data, jsonObject,
											rincian, nim, nama, tanggalP);
								} else {
									jsonObject = new JSONObject();
									jsonObject.put("status", "03");
									jsonObject.put("description", "Request Salah");
									body = jsonObject.toString();
								}
							} catch (Exception exH2H) {
								// FIX: error DB transient (mis. "canceling statement due to lock timeout" saat
								// banyak notifikasi H2H bank masuk bersamaan/menabrak baris Kegiatan yang sama)
								// sebelumnya lolos tanpa ditangkap sampai ke doProses (yang cuma "throws
								// Exception") -> body tak pernah ditulis -> respons H2H ke bank jadi
								// exception/500 mentah alih-alih JSON status seperti kontrak API H2H. Tangkap di
								// sini, catat ke audit, dan tetap kembalikan JSON valid supaya host bank bisa
								// retry sesuai kontraknya, bukan menerima respons rusak.
								ais.common.ErrorAuditUtil.record(exH2H,
										"Va.doProses: gagal proses H2H (kemungkinan DB sibuk/lock timeout)");
								jsonObject = new JSONObject();
								jsonObject.put("status", "05");
								jsonObject.put("description", "Sistem sedang sibuk, silakan coba lagi beberapa saat.");
								body = jsonObject.toString();
							} finally {
								KegiatanPersistenceHelper.closeNativeSession(session);
							}
						}
					}
				}
			} catch (Exception e) {
				jsonObject.put("status", "08");
				jsonObject.put("description", "Terjadi kesalahan " + e.getMessage());
				body = jsonObject.toString();
				logHostToHost.setStackTrace(ais.action.ws.util.PembayaranGatewayHelper.ambilStackTrace(e));
				Common.tampilErrorJikaAdmin(e);
			} finally {
				// ================== PENCATATAN LOG HOST TO HOST (JAMINAN) ==================
				// Disimpan di blok finally + via helper bersama (session terdedikasi + commit +
				// retry, tak pernah gagal) agar log request bank PASTI tercatat walau ada error.
				System.out.println("[VA Servlet] Mencatat Log H2H ke database...");
				try {
					logHostToHost.setKeterangan(data);
					logHostToHost.setIp(getIpClient(request, bankHost));
					logHostToHost.setBankHost(bankHost);
					logHostToHost.setNim(nim);
					logHostToHost.setKode(va);
					logHostToHost.setNama(nama);
					logHostToHost.setTanggal(ais.ui.util.WaktuUtil.getDate());
					logHostToHost.setResponseDescription(body);
					logHostToHost.setNominal(virtualAccountBankNtt == null ? 0.0 : virtualAccountBankNtt.getTotal());
					logHostToHost.setItem(rincian.toString());
					ais.action.ws.util.PembayaranGatewayHelper.simpanLogHostToHost(logHostToHost);
				} catch (Exception eLog) {
					Common.tampilErrorJikaAdmin(eLog);
				}
			}

			// ================== CUSTOM RESPONSE BANK BTN / BJBS ==================
			if (bank != null && bank.equalsIgnoreCase("Bank BTN")) {
				JSONObject responseBtn = new JSONObject();
				if (virtualAccountBankNtt == null
						|| virtualAccountBankNtt.getKadaluarsa().before(WaktuUtil.getDate())) {
					responseBtn.put("ref", "");
					responseBtn.put("rsp", "002");
					responseBtn.put("spdesc", "Transction Failed.");
				} else {
					responseBtn.put("ref", virtualAccountBankNtt.getKode());
					responseBtn.put("rsp", "000");
					responseBtn.put("spdesc", "Transction Success.");
				}
				body = responseBtn.toString();
			}

			if (bank != null && bank.equalsIgnoreCase("Bank BJBS")) {
				JSONObject responseBtn = new JSONObject();
				if (virtualAccountBankNtt == null
						|| virtualAccountBankNtt.getKadaluarsa().before(WaktuUtil.getDate())) {
					responseBtn.put("rc", "442");
					responseBtn.put("rm", "Virtual Account doesn't exist !");
				} else {
					responseBtn.put("rc", "000");
					responseBtn.put("rm", "Success");
				}
				body = responseBtn.toString();
			}
		}

		System.out.println("[VA Servlet] Selesai proses. Response Bank: " + bank + " | Body Length: " + body.length());
		return body;
	}

	/**
	 * Membaca permintaan HTTP, menentukan host bank pemanggil, menjalankan {@link #doProses},
	 * lalu menulis hasilnya sebagai balasan JSON.
	 *
	 * <p>Badan permintaan dibaca baris demi baris lalu <b>digabung tanpa pemisah</b>, sehingga
	 * JSON multi-baris tetap terurai dengan benar tetapi nilai string yang mengandung baris baru
	 * akan menempel. Isi badan dan query string dicetak ke {@code System.out} apa adanya &mdash;
	 * termasuk seluruh payload bank &mdash; sehingga log aplikasi memuat data transaksi mentah.</p>
	 *
	 * <p>Nama bank dibaca dari kunci JSON {@code bank} atau, bila tidak ada, dari parameter
	 * request {@code bank}; kemudian ditimpa menjadi {@code "Bank BTN"} bila payload memuat
	 * {@code revflag}/{@code reserve}, atau {@code "Bank BJBS"} bila memuat {@code va_acc_no}.</p>
	 *
	 * <p>{@link BankHost} dicari lewat {@code PembayaranUtil.getBankHost} memakai
	 * {@code request.getRemoteAddr()} &mdash; alamat soket sesungguhnya, bukan header
	 * {@code X-Forwarded-For} yang bisa dipalsukan. Hasilnya boleh {@code null}; nilai
	 * {@code null} diteruskan apa adanya ke {@link #doProses} dan tidak menolak permintaan.</p>
	 *
	 * <p>Bendera {@code tetapberhasil} yang diteruskan ke {@link #doProses} bernilai {@code true}
	 * bila dan hanya bila nama bank hasil langkah di atas sama dengan {@code "Bank BTN"}. Karena
	 * nama bank itu berasal dari payload pemanggil, pemilihan mode tersebut ditentukan oleh isi
	 * permintaan, bukan oleh identitas pemanggil yang terverifikasi.</p>
	 *
	 * <p>Balasan dikirim dengan header {@code length} dan {@code Content-Type: application/json};
	 * status HTTP dibiarkan pada nilai bawaan 200 apa pun kode status di dalam JSON.</p>
	 *
	 * @param request  permintaan HTTP masuk
	 * @param response balasan HTTP yang akan diisi
	 * @throws Exception bila pembacaan badan permintaan atau pemrosesan H2H gagal
	 */
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		StringBuilder buffer = new StringBuilder();
		BufferedReader reader = request.getReader();
		String line;
		while ((line = reader.readLine()) != null) {
			buffer.append(line);
		}
		String data = buffer.toString();
		String querystring = request.getQueryString();

		System.out.println("==> VA data => " + data);
		System.out.println("==> VA querystring => " + querystring);

		JSONObject req = (data != null && !data.trim().isEmpty()) ? new JSONObject(data) : null;
		String bank = (req == null || req.isNull("bank")) ? request.getParameter("bank") : req.getString("bank");

		if (req != null) {
			if (!req.isNull("revflag") || !req.isNull("reserve"))
				bank = "Bank BTN";
			if (!req.isNull("va_acc_no"))
				bank = "Bank BJBS";
		}

		BankHost bankHost = pembayaranUtil.getBankHost(request.getRemoteAddr(), bank);
		boolean isBtn = bank != null && bank.equalsIgnoreCase("Bank BTN");

		String body = Va.doProses(data, request, bankHost, isBtn);

		response.setHeader("length", body.length() + "");
		response.setHeader("Content-Type", "application/json");
		PrintWriter writer = response.getWriter();
		writer.write(body);
	}

	// ====================================================================================================
	// HELPER METHODS (Dibuat untuk modularitas dan mempermudah pembacaan logic
	// tanpa mengubah bisnis proses)
	// ====================================================================================================


	/**
	 * Mengembalikan session Hibernate yang dijamin terbuka: session yang diberikan bila masih
	 * hidup, atau session ThreadLocal pengganti bila sudah tertutup.
	 *
	 * <p>Diperlukan karena {@link #prosesH2HPayment} melakukan beberapa {@code commit} berturutan;
	 * pada sebagian jalur session bisa tertutup di tengah proses sehingga operasi berikutnya
	 * gagal.</p>
	 *
	 * <p>Session pengganti <b>sengaja</b> memakai {@code HibernateUtil.currentNativeSession()}
	 * (ThreadLocal), bukan {@code openSession()}: hasil method ini di-assign ke variabel lokal
	 * pemanggil sehingga lolos dari blok {@code finally} milik session asli. Session ThreadLocal
	 * dijamin ditutup oleh teardown {@code FilterJSP} di akhir request, sedangkan
	 * {@code openSession()} mentah di sini akan bocor tanpa ada yang menutup.</p>
	 *
	 * @param session session yang akan diperiksa; boleh {@code null}
	 * @return session yang siap dipakai; tidak pernah {@code null}
	 */
	private static Session pastikanSessionAktif(Session session) {
		try {
			if (session != null && session.isOpen()) {
				return session;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Va.java:630");
		}
		/*
		 * Session pengganti SENGAJA memakai currentNativeSession() (ThreadLocal), bukan
		 * openSession(): hasil method ini di-assign ke parameter lokal pemanggil sehingga
		 * lolos dari blok finally pemilik session asli. Session ThreadLocal dijamin
		 * tertutup oleh teardown FilterJSP di akhir request, sedangkan openSession()
		 * mentah di sini akan bocor tanpa ada yang menutup.
		 */
		return HibernateUtil.currentNativeSession();
	}

	/**
	 * Membaca satu parameter permintaan dengan mendahulukan badan JSON lalu jatuh ke query string.
	 *
	 * <p>Urutannya: bila {@code req} ada dan kuncinya tidak {@code null} di dalam JSON, nilai JSON
	 * dipakai; selain itu dibaca dari {@code request.getParameter(key)}. Bila {@code request} juga
	 * {@code null}, dikembalikan string kosong.</p>
	 *
	 * <p>Perhatikan bahwa jalur JSON mengembalikan {@code ""} untuk kunci yang ada tapi kosong,
	 * sedangkan jalur query string dapat mengembalikan {@code null} untuk parameter yang tidak
	 * ada. Karena itu pemanggil di {@link #doProses} selalu memeriksa {@code null} sebelum
	 * memakai hasilnya.</p>
	 *
	 * @param req     badan permintaan yang sudah diurai; boleh {@code null}
	 * @param request permintaan HTTP asal; boleh {@code null}
	 * @param key     nama kunci JSON sekaligus nama parameter query
	 * @return nilai parameter, string kosong, atau {@code null} bila parameter query tidak ada
	 */
	private static String getSafeParam(JSONObject req, HttpServletRequest request, String key) {
		if (req != null && !req.isNull(key)) {
			return req.optString(key, "");
		}
		return request != null ? request.getParameter(key) : "";
	}

	/**
	 * Mengambil nama fakultas seorang mahasiswa dengan aman terhadap relasi yang kosong.
	 *
	 * <p>Jalur relasinya {@code Mahasiswa -> jurusan -> fakultas -> nama}; setiap simpul
	 * diperiksa lebih dulu sehingga tidak pernah melempar {@code NullPointerException}.</p>
	 *
	 * @param m mahasiswa yang ditanyakan; boleh {@code null}
	 * @return nama fakultas, atau string kosong bila salah satu relasi tidak ada
	 */
	private static String getSafeFakultas(Mahasiswa m) {
		if (m != null && m.getJurusan() != null && m.getJurusan().getFakultas() != null)
			return m.getJurusan().getFakultas().getNama();
		return "";
	}

	/**
	 * Mengambil nama program studi (jurusan) seorang mahasiswa dengan aman terhadap relasi kosong.
	 *
	 * @param m mahasiswa yang ditanyakan; boleh {@code null}
	 * @return nama jurusan, atau string kosong bila mahasiswa atau jurusannya tidak ada
	 */
	private static String getSafeProdi(Mahasiswa m) {
		if (m != null && m.getJurusan() != null)
			return m.getJurusan().getNama();
		return "";
	}

	/**
	 * Mengambil nama fakultas dari <b>prodi lulus</b> seorang calon mahasiswa dengan aman.
	 *
	 * <p>Prodi lulus adalah program studi hasil kelulusan seleksi, berbeda dari
	 * {@code prodi1} yang merupakan pilihan saat mendaftar. Dipakai untuk calon yang sudah
	 * dinyatakan lulus dan sedang membayar daftar ulang.</p>
	 *
	 * @param cm calon mahasiswa yang ditanyakan; boleh {@code null}
	 * @return nama fakultas prodi lulus, atau string kosong bila salah satu relasi tidak ada
	 */
	private static String getSafeFakultasLulus(BiodataCalonMahasiswa cm) {
		if (cm != null && cm.getProdiLulus() != null && cm.getProdiLulus().getFakultas() != null)
			return cm.getProdiLulus().getFakultas().getNama();
		return "";
	}

	/**
	 * Mengambil nama <b>prodi lulus</b> seorang calon mahasiswa dengan aman terhadap relasi kosong.
	 *
	 * @param cm calon mahasiswa yang ditanyakan; boleh {@code null}
	 * @return nama prodi lulus, atau string kosong bila calon atau prodi lulusnya tidak ada
	 */
	private static String getSafeProdiLulus(BiodataCalonMahasiswa cm) {
		if (cm != null && cm.getProdiLulus() != null)
			return cm.getProdiLulus().getNama();
		return "";
	}

	/**
	 * Menentukan alamat IP pemanggil <b>untuk keperluan pencatatan</b> {@link LogHostToHost}.
	 *
	 * <p>Urutan sumber yang dicoba: header {@code Cf-Connecting-Ip}, {@code CF-Connecting-IP},
	 * {@code X-Forwarded-For}, {@code X-Real-IP}, lalu {@code request.getRemoteAddr()} sebagai
	 * cadangan. Bila {@code request} sendiri {@code null}, dipakai IP yang tercatat pada
	 * {@link BankHost}.</p>
	 *
	 * <p><b>Penting:</b> keempat header di atas dikirim oleh klien dan karena itu dapat
	 * dipalsukan. Method ini hanya dipakai untuk mengisi kolom {@code ip} pada log, <b>bukan</b>
	 * untuk mengambil keputusan otorisasi &mdash; pencocokan {@link BankHost} di {@link #process}
	 * memakai {@code request.getRemoteAddr()} secara langsung. Konsekuensinya, kolom {@code ip}
	 * pada log H2H tidak boleh diperlakukan sebagai bukti asal permintaan yang tepercaya.</p>
	 *
	 * @param request  permintaan HTTP asal; boleh {@code null}
	 * @param bankHost host bank yang dipakai sebagai cadangan; boleh {@code null}
	 * @return alamat IP untuk dicatat, atau string kosong bila tidak ada sumber sama sekali
	 */
	private static String getIpClient(HttpServletRequest request, BankHost bankHost) {
		if (request == null)
			return bankHost != null ? bankHost.getIp() : "";
		String ip = request.getHeader("Cf-Connecting-Ip");
		if (ip == null)
			ip = request.getHeader("CF-Connecting-IP");
		if (ip == null)
			ip = request.getHeader("X-Forwarded-For");
		if (ip == null)
			ip = request.getHeader("X-Real-IP");
		return ip != null ? ip : request.getRemoteAddr();
	}

	/**
	 * Memeriksa apakah sebuah VA sudah melewati tanggal kedaluwarsanya.
	 *
	 * <p>Pemeriksaan hanya berlaku bila konfigurasi {@code chek_kadaluarsa} aktif; bila
	 * konfigurasi itu mati, method selalu mengembalikan {@code false} sehingga VA lama tetap
	 * bisa dibayar. VA tanpa tanggal kedaluwarsa juga dianggap belum kedaluwarsa.</p>
	 *
	 * <p>Dipanggil untuk {@code action=inquiry} dan {@code action=payment} saja &mdash; jalur
	 * {@code action=reversal} tidak memeriksa kedaluwarsa.</p>
	 *
	 * @param vaBank VA yang diperiksa; boleh {@code null}
	 * @return {@code true} bila pemeriksaan aktif dan tanggal kedaluwarsa sudah lewat
	 */
	private static boolean isExpired(VirtualAccountBank vaBank) {
		boolean checkAktif = Common.bolehKonfigurasi("chek_kadaluarsa");
		return checkAktif && vaBank != null && vaBank.getKadaluarsa() != null
				&& vaBank.getKadaluarsa().before(WaktuUtil.getDate());
	}

	/**
	 * Membangun balasan baku untuk VA yang sudah kedaluwarsa: status {@code 05} dengan deskripsi
	 * "Pembayaran telah kadaluarsa".
	 *
	 * @return objek JSON balasan yang siap diubah menjadi string
	 * @throws Exception bila pembentukan JSON gagal
	 */
	private static JSONObject generateExpiredResponse() throws Exception {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("status", "05");
		jsonObject.put("description", "Pembayaran telah kadaluarsa");
		return jsonObject;
	}

	/**
	 * Menerjemahkan {@link Response} milik layanan pembayaran berbasis NIM menjadi bentuk JSON
	 * balasan H2H yang sama dengan jalur VA biasa.
	 *
	 * <p>Dipakai hanya pada jalur "bayar via NIM" (konfigurasi
	 * {@code pembayaran_via_va_bisa_berdasarkan_nim}), yaitu ketika nomor yang dikirim bank
	 * ternyata bukan nomor VA melainkan NIM mahasiswa.</p>
	 *
	 * <h4>Penguraian rincian</h4>
	 * <p>Field {@code amount} pada {@link Response} berisi daftar item yang dipisahkan tanda
	 * {@code |}, dan setiap item dipisahkan lagi dengan garis miring terbalik. Nama item diambil
	 * dari bagian kedua dan nominalnya dari bagian terakhir. Item yang gagal diurai dilewati.
	 * Bila tidak ada satu pun item yang berhasil ditambahkan, dibuat satu baris rincian
	 * pengganti memakai {@code namaRinci} dan {@code nominalRinci}.</p>
	 *
	 * @param response     hasil panggilan {@code inquiryByNim}/{@code payByNim}
	 * @param rincian      array rincian yang akan diisi; dimodifikasi di tempat
	 * @param namaRinci    nama item pengganti bila rincian asli tidak terurai
	 * @param nominalRinci nominal item pengganti bila rincian asli tidak terurai
	 * @return objek JSON balasan lengkap berikut status, identitas, dan rincian
	 * @throws Exception bila pembentukan JSON atau penguraian {@code total_amount} gagal
	 */
	private static JSONObject mapResponseToJson(Response response, JSONArray rincian, String namaRinci,
			Double nominalRinci) throws Exception {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("status", response.getResponse_code());
		jsonObject.put("description", response.getResponse_description());
		jsonObject.put("nim", response.getNim());
		jsonObject.put("nama", response.getNama());
		jsonObject.put("semester", response.getSemester_ke());
		jsonObject.put("tahun_akademik", Common.getCurrentTahunAkademik());
		jsonObject.put("jenis", "Pembayaran");

		boolean rincianAsliDitambahkan = false;
		String amount = response.getAmount();
		if (amount != null && !amount.trim().isEmpty()) {
			String[] barisRincian = StringUtils.split(amount, '|');
			if (barisRincian != null) {
				for (String baris : barisRincian) {
					String[] bagian = StringUtils.splitPreserveAllTokens(baris, '\\');
					if (bagian != null && bagian.length >= 3) {
						try {
							Double nominalItem = Double.parseDouble(bagian[bagian.length - 1].trim());
							String namaItem = bagian[1] == null ? "" : bagian[1].trim();
							if (!namaItem.isEmpty()) {
								JSONObject item = new JSONObject();
								item.put("nama", namaItem);
								item.put("nominal", nominalItem);
								rincian.put(item);
								rincianAsliDitambahkan = true;
							}
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
					}
				}
			}
		}
		if (!rincianAsliDitambahkan) {
			JSONObject jsonObjectRinci = new JSONObject();
			jsonObjectRinci.put("nama", namaRinci);
			jsonObjectRinci.put("nominal", nominalRinci);
			rincian.put(jsonObjectRinci);
		}

		jsonObject.put("nominal", Double.parseDouble(response.getTotal_amount()));
		jsonObject.put("keterangan", response.getResponse_description());
		jsonObject.put("fakultas", response.getFakultas());
		jsonObject.put("prodi", response.getProdi());
		jsonObject.put("rincian", rincian);
		return jsonObject;
	}

	// CATATAN: Method prosesH2HInquiry, prosesH2HPayment, prosesH2HReversal memuat
	// logic core H2H yang panjang.
	// Kami memisahkannya agar struktur try-catch-finally utamanya rapi. Dikarenakan
	// kompleksitas yang sama,
	// saya sertakan perbaikan logic session dan memory optimization.

	// =========================================================================================
    // IMPLEMENTASI METHOD H2H INQUIRY
    // =========================================================================================
    /**
     * Menjalankan {@code action=inquiry}: mengembalikan identitas pemilik VA berikut rincian
     * tagihannya kepada host bank, tanpa memposting pembayaran.
     *
     * <h4>Dua skenario</h4>
     * <ol>
     *   <li><b>Siswa sekolah</b> &mdash; dipilih bila VA punya relasi {@code siswa} atau
     *       {@code calonSiswa}. Identitas diambil dari nomor induk dan nama siswa, sedangkan
     *       {@code fakultas} diisi nama yayasan dan {@code prodi} diisi nama sekolah. Rincian
     *       tagihan diambil dengan memanggil {@code VirtualAccountBank.bayarSiswa} dalam
     *       <b>mode simulasi</b> (argumen ke-lima bernilai {@code true}), yang mengembalikan
     *       daftar {@link Tagihan} per kelompok tanpa memposting pembayaran. Balasan memuat
     *       {@code noref} berupa gabungan {@code id.kodeVA};</li>
     *   <li><b>Mahasiswa/calon mahasiswa</b> &mdash; jalur selain di atas. Bila VA tidak menunjuk
     *       {@link Mahasiswa} maupun {@link BiodataCalonMahasiswa}, balasan langsung berstatus
     *       {@code 02}. Selain itu status ditentukan oleh kolom {@code kegiatan} pada VA:
     *       {@code null} berarti belum dibayar sehingga status {@code 00} "Inquiry Sukses",
     *       sedangkan yang sudah terisi berarti status {@code 02} "Tagihan Telah Dibayar".</li>
     * </ol>
     *
     * <h4>Penguraian token cicilan</h4>
     * <p>Kolom {@code cicilan} pada VA berisi daftar token yang dipisahkan koma. Tiga bentuk
     * token dikenali:</p>
     * <ul>
     *   <li><b>angka polos</b> &mdash; id {@link PengaturanPembayaranBulanan}; nominalnya dihitung
     *       ulang secara dinamis lewat {@code ambilNominalModifikasi(mahasiswa, semester)};</li>
     *   <li>{@code Bulanan-<id>-<nilai>} &mdash; id pembayaran bulanan dengan nominal yang sudah
     *       dipatok pada token; nilai dibaca dari potongan terakhir;</li>
     *   <li>{@code Item-<idItem>-<nilai>-...} &mdash; id {@link ItemBiaya} dengan nominal pada
     *       potongan ketiga.</li>
     * </ul>
     * <p>Untuk dua bentuk terakhir, item ber-{@code penghitungan} {@link ItemBiaya#DIKALI_NILAI_MINUS}
     * dibalik tandanya menjadi negatif (potongan/diskon). Nominal yang gagal diurai dicatat ke
     * audit error dan dianggap nol.</p>
     *
     * <p>Setelah seluruh token dijumlahkan, {@code VirtualAccountBank.updateTotal(vaNtt, total)}
     * dipanggil &mdash; jadi method ini <b>menulis ke basis data</b> meskipun namanya "inquiry".
     * Nilai {@code nominal} pada balasan diambil dari {@code vaNtt.getTotal()} sesudah pembaruan
     * itu, bukan dari variabel {@code total} lokal.</p>
     *
     * @param vaNtt      VA yang ditanyakan; dijamin tidak {@code null} oleh pemanggil
     * @param session    session Hibernate aktif milik {@link #doProses}
     * @param bank       nama bank pemanggil, diteruskan ke {@code bayarSiswa}
     * @param data       badan permintaan mentah, disimpan sebagai jejak pada jalur siswa
     * @param jsonObject objek balasan awal; selalu diganti objek baru di dalam method ini
     * @param rincian    array rincian yang akan diisi; dimodifikasi di tempat
     * @param nim        penampung NIM; diisi ulang dari data VA
     * @param nama       penampung nama; diisi ulang dari data VA
     * @return badan balasan JSON dalam bentuk string
     * @throws Exception bila query atau pembentukan JSON gagal
     */
    private static String prosesH2HInquiry(VirtualAccountBank vaNtt, Session session, String bank, String data, 
            JSONObject jsonObject, JSONArray rincian, String nim, String nama) throws Exception {
        
        System.out.println("[VA Servlet - Inquiry] Memulai proses H2H Inquiry. VA: " + vaNtt.getKode());

        // ---------------------------------------------------------------------------------
        // SKENARIO 1: INQUIRY SISWA SEKOLAH
        // ---------------------------------------------------------------------------------
        if (vaNtt.getSiswa() != null || vaNtt.getCalonSiswa() != null) {
            Sekolah sekolah = null;
            if (vaNtt.getSiswa() != null) {
                nim = vaNtt.getSiswa().getNomorInduk();
                nama = vaNtt.getSiswa().getNama();
                sekolah = vaNtt.getSiswa().getSekolah();
            } else {
                nim = vaNtt.getCalonSiswa().getNomorInduk();
                nama = vaNtt.getCalonSiswa().getNama();
                sekolah = vaNtt.getCalonSiswa().getSekolah();
            }

            jsonObject = new JSONObject();
            boolean isSukses = VirtualAccountBank.isSudahTerbayar(vaNtt);
            jsonObject.put("status", isSukses ? "00" : "02");
            jsonObject.put("description", isSukses ? "Pembayaran Sukses" : "Pembayaran Gagal");
            jsonObject.put("nim", nim);
            jsonObject.put("nama", nama);
            jsonObject.put("semester", vaNtt.getSemester());
            jsonObject.put("jenis", vaNtt.getAkunPembayaranSiswa() == null ? "" : vaNtt.getAkunPembayaranSiswa().getNama());
            jsonObject.put("nominal", vaNtt.getTotal());
            jsonObject.put("keterangan", vaNtt.getKeterangan());
            jsonObject.put("noref", vaNtt.getId() + "." + vaNtt.getKode());
            jsonObject.put("tahun_akademik", vaNtt.getTahunAkademik());
            jsonObject.put("fakultas", sekolah != null ? sekolah.getYayasan().getNama() : "");
            jsonObject.put("prodi", sekolah != null ? sekolah.getNama() : "");

            Map<String, List<Tagihan>> map = VirtualAccountBank.bayarSiswa(vaNtt, session, vaNtt.getWaktuBayar() == null ? WaktuUtil.getDate() : vaNtt.getWaktuBayar(), bank, true, data, false);
            
            for (List<Tagihan> tagihans : map.values()) {
                for (Tagihan tagihan : tagihans) {
                    JSONObject jr = new JSONObject();
                    jr.put("nama", tagihan.getItemBiayaSekolah().getNama());
                    jr.put("bulan", tagihan.getTahunbulan() + "");
                    jr.put("nominal", tagihan.getNominal());
                    rincian.put(jr);
                }
            }
            jsonObject.put("rincian", rincian);

        // ---------------------------------------------------------------------------------
        // SKENARIO 2: INQUIRY MAHASISWA KAMPUS
        // ---------------------------------------------------------------------------------
        } else {
            JenisKegiatan jenisKegiatan = vaNtt.getJenisKegiatan();
            Mahasiswa mahasiswa = vaNtt.getMahasiswa();
            BiodataCalonMahasiswa bcm = vaNtt.getBiodataCalonMahasiswa();
            Integer semester = vaNtt.getSemester();

            if (mahasiswa == null && bcm == null) {
                jsonObject = new JSONObject();
                jsonObject.put("status", "02");
                jsonObject.put("description", "Data mahasiswa/calon mahasiswa pada VA tidak ditemukan");
                jsonObject.put("nim", "");
                jsonObject.put("nama", "");
                jsonObject.put("semester", semester);
                jsonObject.put("tahun_akademik", vaNtt.getTahunAkademik());
                jsonObject.put("jenis", jenisKegiatan == null ? "" : jenisKegiatan.getNamaKegiatan());
                jsonObject.put("nominal", 0.0);
                jsonObject.put("keterangan", vaNtt.getKeterangan());
                jsonObject.put("fakultas", "");
                jsonObject.put("prodi", "");
                jsonObject.put("rincian", rincian);
                return jsonObject.toString();
            }

            nim = mahasiswa == null ? bcm.getNoRegistrasi() : mahasiswa.getNim();
            nama = mahasiswa == null ? bcm.getNama() : mahasiswa.getNama();
            String fakultas = mahasiswa == null ? (bcm.getProdi1() == null || bcm.getProdi1().getFakultas() == null ? "" : bcm.getProdi1().getFakultas().getNama()) : getSafeFakultas(mahasiswa);
            String prodi = mahasiswa == null ? (bcm.getProdi1() == null ? "" : bcm.getProdi1().getNama()) : getSafeProdi(mahasiswa);

            if (bcm != null && bcm.getProdiLulus() != null) {
                prodi = getSafeProdiLulus(bcm);
                fakultas = getSafeFakultasLulus(bcm);
            }

            jsonObject = new JSONObject();
            jsonObject.put("status", vaNtt.getKegiatan() == null ? "00" : "02");
            jsonObject.put("description", vaNtt.getKegiatan() == null ? "Inquiry Sukses" : "Tagihan Telah Dibayar");
            jsonObject.put("nim", nim);
            jsonObject.put("nama", nama);
            jsonObject.put("semester", semester);
            jsonObject.put("tahun_akademik", vaNtt.getTahunAkademik());
            jsonObject.put("jenis", jenisKegiatan == null ? "" : jenisKegiatan.getNamaKegiatan());

            Double total = 0.0;
            
            // ================== BLOK LOGIC RINCIAN CICILAN ==================
            if (vaNtt.getCicilan() != null && !vaNtt.getCicilan().isEmpty()) {
                System.out.println("[VA Servlet - Inquiry] Memproses rincian cicilan tagihan mahasiswa...");
                
                for (String idPemBul : StringUtils.split(vaNtt.getCicilan(), ",")) {
                    
                    if (Common.isNumber(idPemBul)) {
                        PengaturanPembayaranBulanan ppb = (PengaturanPembayaranBulanan) session
                                .createCriteria(PengaturanPembayaranBulanan.class)
                                .add(Restrictions.idEq(Long.parseLong(idPemBul)))
                                .uniqueResult();

                        if (ppb != null) {
                            JSONObject jsonObjectRinci = new JSONObject();
                            jsonObjectRinci.put("nama", ppb.getDetailBiaya().getItemBiaya().getNama());
                            jsonObjectRinci.put("bulan", ppb.getNamaBulan());

                            Double subtotal = ppb.ambilNominalModifikasi(mahasiswa, semester);
                            total += subtotal;
                            jsonObjectRinci.put("nominal", subtotal);
                            rincian.put(jsonObjectRinci);
                        }
                    } else if (idPemBul != null && idPemBul.startsWith("Bulanan-")) {
                        PengaturanPembayaranBulanan ppb = (PengaturanPembayaranBulanan) session
                                .createCriteria(PengaturanPembayaranBulanan.class)
                                .add(Restrictions.idEq(Long.parseLong(idPemBul.split("-")[1])))
                                .uniqueResult();

                        if (ppb != null) {
                            JSONObject jsonObjectRinci = new JSONObject();
                            jsonObjectRinci.put("nama", ppb.getDetailBiaya().getItemBiaya().getNama());
                            jsonObjectRinci.put("bulan", ppb.getNamaBulan());

                            Double subtotal = 0.0;
                            try {
                                String[] spl = idPemBul.split("-");
                                subtotal = Double.parseDouble(spl[spl.length - 1]);
                            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Va.java:847");}

                            if (ppb.getDetailBiaya().getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
                                subtotal = 0.0 - subtotal;
                            }
                            total += subtotal;
                            jsonObjectRinci.put("nominal", subtotal);
                            rincian.put(jsonObjectRinci);
                        }
                    } else if (idPemBul != null && idPemBul.startsWith("Item-")) {
                        ItemBiaya itemBiaya = (ItemBiaya) session
                                .createCriteria(ItemBiaya.class)
                                .add(Restrictions.idEq(Long.parseLong(idPemBul.split("-")[1])))
                                .uniqueResult();

                        if (itemBiaya != null) {
                            JSONObject jsonObjectRinci = new JSONObject();
                            jsonObjectRinci.put("nama", itemBiaya.getNama());
                            jsonObjectRinci.put("bulan", "");

                            Double subtotal = 0.0;
                            try {
                                String[] spl = idPemBul.split("-");
                                subtotal = Double.parseDouble(spl[2]);
                            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Va.java:871");}

                            // Abaikan field bayarke seperti logic aslinya (supaya tidak unused variable warning)

                            if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
                                subtotal = 0.0 - subtotal;
                            }
                            total += subtotal;
                            jsonObjectRinci.put("nominal", subtotal);
                            rincian.put(jsonObjectRinci);
                        }
                    }
                }
                
                // Update nilai total Virtual Account secara optimis jika diperlukan
                VirtualAccountBank.updateTotal(vaNtt, total);
            }
            // ================================================================

            jsonObject.put("nominal", vaNtt.getTotal());
            jsonObject.put("keterangan", vaNtt.getKeterangan());
            jsonObject.put("fakultas", fakultas);
            jsonObject.put("prodi", prodi);
            jsonObject.put("rincian", rincian);
        }

        return jsonObject.toString();
    }

	// =========================================================================================
	// IMPLEMENTASI METHOD H2H PAYMENT
	// =========================================================================================
	/**
	 * Menjalankan {@code action=payment}: memposting setoran bank menjadi data keuangan nyata
	 * &mdash; {@link Kegiatan} tervalidasi berikut baris-baris {@link CicilanPembayaran} &mdash;
	 * lalu menandai VA sebagai terbayar.
	 *
	 * <p>Ini adalah method paling berdampak di kelas ini: keluarannya bukan sekadar balasan JSON,
	 * melainkan perubahan permanen pada pembukuan tagihan mahasiswa/siswa.</p>
	 *
	 * <h4>Penentuan tanggal transaksi</h4>
	 * <p>Tanggal diambil dari parameter {@code tanggal} milik bank lewat
	 * {@link #parseTanggalPayment}. Khusus jalur siswa, bila VA sudah punya relasi
	 * {@code pembayaran}, tanggal ditimpa dengan tanggal revisi Envers pertama entitas
	 * {@code PembayaranSiswa} yang menunjuk VA ini &mdash; supaya posting ulang tidak menggeser
	 * tanggal pembayaran yang sudah tercatat. Kegagalan membaca jejak audit tidak menghentikan
	 * proses, hanya menghasilkan peringatan di konsol.</p>
	 *
	 * <h4>Skenario 1 &mdash; siswa sekolah</h4>
	 * <p>Dipilih bila VA punya relasi {@code siswa} atau {@code calonSiswa}. Seluruh posting
	 * didelegasikan ke {@code VirtualAccountBank.bayarSiswa} dalam mode nyata (argumen ke-lima
	 * {@code false}), yang mengembalikan daftar {@link Tagihan} yang berhasil dilunasi untuk
	 * disusun menjadi array {@code rincian}.</p>
	 *
	 * <h4>Skenario 2 &mdash; mahasiswa/calon mahasiswa</h4>
	 * <p>Berjalan dalam beberapa transaksi berturutan:</p>
	 * <ol>
	 *   <li>{@link Kegiatan} dicari dari kolom {@code kegiatan} pada VA; bila kosong, dicari
	 *       berdasarkan kombinasi mahasiswa/calon, jenis kegiatan, dan semester; bila tetap tidak
	 *       ada, dibuat baru. Kegiatan diisi identitas, tahun akademik, tanggal, {@code validated=1},
	 *       dan {@code validator} berisi nama bank, lalu disimpan dan di-commit. Sesudah commit,
	 *       {@link #pastikanSessionAktif} dipakai supaya session tetap hidup;</li>
	 *   <li>daftar {@link DetailBiaya} dimuat dari kolom {@code detailbiaya} VA untuk menghitung
	 *       {@code nilaiBiayaHarusDiBayars};</li>
	 *   <li>satu transaksi dibuka untuk seluruh cicilan. Token pada kolom {@code cicilan} diurai
	 *       sama seperti pada {@link #prosesH2HInquiry} &mdash; angka polos, {@code Bulanan-},
	 *       {@code Item-} &mdash; dan setiap token disimpan lewat {@link #saveCicilan} dengan
	 *       referensi unik berpola {@code ntt-<idKegiatan>-<token>-<idVA>}. Ada satu bentuk token
	 *       tambahan yang tidak dikenal jalur inquiry, yaitu {@code Keranjang-}: token keranjang
	 *       belanja multi-jenis yang diproses oleh
	 *       {@code PembayaranGatewayHelper.prosesSatuTokenKeranjang}; karena helper itu mengelola
	 *       transaksinya sendiri, transaksi batch ditutup lalu dibuka kembali di sekitarnya.
	 *       Bila kolom {@code cicilan} kosong, cicilan dibuat langsung dari daftar
	 *       {@link DetailBiaya};</li>
	 *   <li>total dan denda dihitung ulang dari seluruh cicilan lewat
	 *       {@code PembayaranUtil.getTotalDanDendaFromCicilan}, lalu {@code amount},
	 *       {@code denda}, dan {@code amountTerhutang} pada {@link Kegiatan} diperbarui dan
	 *       di-commit;</li>
	 *   <li>terakhir {@code VirtualAccountBank.updateVa} menandai VA sebagai terbayar dan
	 *       mengaitkannya dengan kegiatan yang baru dibuat. Bila VA belum punya {@link BankHost},
	 *       host pemanggil dipasang di sini.</li>
	 * </ol>
	 *
	 * <p>Status balasan tidak diasumsikan sukses: ia dibaca ulang dari
	 * {@code VirtualAccountBank.isSudahTerbayar(vaNtt)} sesudah posting, sehingga bernilai
	 * {@code 00} hanya bila VA benar-benar tercatat lunas.</p>
	 *
	 * <h4>Catatan tentang {@code tetapberhasil}</h4>
	 * <p>Pemeriksaan kesesuaian nominal <b>tidak</b> dilakukan di sini melainkan di
	 * {@link #doProses} sebelum method ini dipanggil, dan pemeriksaan itu dilewati ketika
	 * {@code tetapberhasil} bernilai {@code true}. Parameter {@code tetapberhasil} dan
	 * {@code nominal} diterima method ini demi kelengkapan konteks, tetapi tidak lagi
	 * memengaruhi jalur posting di dalamnya.</p>
	 *
	 * @param vaNtt         VA yang dibayar; dijamin tidak {@code null} oleh pemanggil
	 * @param session       session Hibernate aktif milik {@link #doProses}
	 * @param bank          nama bank pemanggil; disimpan sebagai {@code validator} kegiatan
	 * @param data          badan permintaan mentah, disimpan sebagai jejak
	 * @param jsonObject    objek balasan awal; selalu diganti objek baru di dalam method ini
	 * @param rincian       array rincian yang akan diisi; dimodifikasi di tempat
	 * @param nim           penampung NIM; diisi ulang dari data VA
	 * @param nama          penampung nama; diisi ulang dari data VA
	 * @param tanggalP      tanggal setoran versi bank dalam bentuk string mentah
	 * @param bankHost      host bank pemanggil; dipakai menentukan jenis pembayaran cicilan
	 * @param tetapberhasil bendera lewati-pemeriksaan-nominal; hanya informatif di sini
	 * @param nominal       nominal setoran hasil parsing; hanya informatif di sini
	 * @return badan balasan JSON dalam bentuk string
	 * @throws Exception bila salah satu transaksi atau query gagal
	 * @see #saveCicilan
	 * @see #parseTanggalPayment
	 */
	@SuppressWarnings("unchecked")
	private static String prosesH2HPayment(VirtualAccountBank vaNtt, Session session, String bank, String data,
			JSONObject jsonObject, JSONArray rincian, String nim, String nama, String tanggalP, BankHost bankHost,
			boolean tetapberhasil, Double nominal) throws Exception {

		System.out.println(
				"[VA Servlet - Payment] Memulai proses H2H Payment. VA: " + vaNtt.getKode() + " | Bank: " + bank);

		Date tanggal = parseTanggalPayment(tanggalP, bank);

		// ---------------------------------------------------------------------------------
		// SKENARIO 1: PEMBAYARAN SISWA SEKOLAH
		// ---------------------------------------------------------------------------------
		if (vaNtt.getSiswa() != null || vaNtt.getCalonSiswa() != null) {
			System.out.println("[VA Servlet - Payment] Kategori: Pembayaran Siswa Sekolah");
			Sekolah sekolah = null;
			if (vaNtt.getSiswa() != null) {
				nim = vaNtt.getSiswa().getNomorInduk();
				nama = vaNtt.getSiswa().getNama();
				sekolah = vaNtt.getSiswa().getSekolah();
			} else {
				nim = vaNtt.getCalonSiswa().getNomorInduk();
				nama = vaNtt.getCalonSiswa().getNama();
				sekolah = vaNtt.getCalonSiswa().getSekolah();
			}

			jsonObject = new JSONObject();
			boolean isSukses = VirtualAccountBank.isSudahTerbayar(vaNtt);
			jsonObject.put("status", isSukses ? "00" : "02");
			jsonObject.put("description", isSukses ? "Pembayaran Sukses" : "Pembayaran Gagal");
			jsonObject.put("nim", nim);
			jsonObject.put("nama", nama);
			jsonObject.put("semester", vaNtt.getSemester());
			jsonObject.put("jenis",
					vaNtt.getAkunPembayaranSiswa() == null ? "" : vaNtt.getAkunPembayaranSiswa().getNama());
			jsonObject.put("nominal", vaNtt.getTotal());
			jsonObject.put("keterangan", vaNtt.getKeterangan());
			jsonObject.put("noref", vaNtt.getId() + "." + vaNtt.getKode());
			jsonObject.put("tahun_akademik", vaNtt.getTahunAkademik());
			jsonObject.put("fakultas", sekolah != null ? sekolah.getYayasan().getNama() : "");
			jsonObject.put("prodi", sekolah != null ? sekolah.getNama() : "");

			try {
				if (vaNtt.getPembayaran() != null) {
					AuditReader reader = AuditReaderFactory.get(session);
					AuditQuery query = reader.createQuery().forRevisionsOfEntity(PembayaranSiswa.class, true, true);
					query.addOrder(AuditEntity.revisionNumber().asc());
					query.add(AuditEntity.property("virtualAccountBank").eq(vaNtt));
					Serializable serializable = (Serializable) query.setMaxResults(1).getSingleResult();

					if (serializable != null) {
						ClassMetadata classMetadata = HibernateUtil.getClassMetadata(PembayaranSiswa.class);
						tanggal = (Date) classMetadata.getPropertyValue(serializable, "tanggal", EntityMode.POJO);
						System.out.println("[VA Servlet - Payment] Tanggal revisi siswa ditemukan -> "
								+ Common.dateFormat3.get().format(tanggal));
					}
				}
			} catch (Exception e) {
				System.out.println("[VA Servlet - Payment] Peringatan: Gagal mendapatkan audit trail PembayaranSiswa");
			}

			Map<String, List<Tagihan>> map = VirtualAccountBank.bayarSiswa(vaNtt, session, tanggal, bank, false, data,
					false);
			for (List<Tagihan> tagihans : map.values()) {
				for (Tagihan tagihan : tagihans) {
					JSONObject jr = new JSONObject();
					jr.put("nama", tagihan.getItemBiayaSekolah().getNama());
					jr.put("bulan", tagihan.getTahunbulan() + "");
					jr.put("nominal", tagihan.getNominal());
					rincian.put(jr);
				}
			}
			jsonObject.put("rincian", rincian);

			// ---------------------------------------------------------------------------------
			// SKENARIO 2: PEMBAYARAN MAHASISWA
			// ---------------------------------------------------------------------------------
		} else {
			System.out.println("[VA Servlet - Payment] Kategori: Pembayaran Mahasiswa Kampus");
			JenisKegiatan jenisKegiatan = vaNtt.getJenisKegiatan();
			Mahasiswa mahasiswa = vaNtt.getMahasiswa();
			BiodataCalonMahasiswa bcm = vaNtt.getBiodataCalonMahasiswa();
			Integer semester = vaNtt.getSemester();

			nim = mahasiswa == null ? bcm.getNoRegistrasi() : mahasiswa.getNim();
			nama = mahasiswa == null ? bcm.getNama() : mahasiswa.getNama();

			Kegiatan kegiatan = vaNtt.getKegiatan() != null
					? (Kegiatan) session.createCriteria(Kegiatan.class).add(Restrictions.idEq(vaNtt.getKegiatan()))
							.uniqueResult()
					: null;

			String fakultas = mahasiswa == null
					? (bcm.getProdi1() == null ? "" : bcm.getProdi1().getFakultas().getNama())
					: getSafeFakultas(mahasiswa);
			String prodi = mahasiswa == null ? (bcm.getProdi1() == null ? "" : bcm.getProdi1().getNama())
					: getSafeProdi(mahasiswa);

			if (bcm != null && bcm.getProdiLulus() != null) {
				prodi = getSafeProdiLulus(bcm);
				fakultas = getSafeFakultasLulus(bcm);
			}

			if (kegiatan == null || kegiatan.getId() == null) {
				kegiatan = (Kegiatan) session.createCriteria(Kegiatan.class).addOrder(Order.asc("id"))
						.add(bcm != null ? Restrictions.eq("calonMahasiswa", bcm)
								: Restrictions.eq("mahasiswa", mahasiswa))
						.add(Restrictions.eq("jenisKegiatan", vaNtt.getJenisKegiatan()))
						.add(Restrictions.eq("semster", semester)).setMaxResults(1).uniqueResult();
			}

			if (kegiatan == null || kegiatan.getId() == null)
				kegiatan = new Kegiatan();

			kegiatan.setKodeUnikLain(vaNtt.getKodeUnikLain());
			kegiatan.setJadwalPembayaran(vaNtt.getJadwalPembayaran());
			kegiatan.setNama(nama);
			kegiatan.setAmount(vaNtt.getTotal());
			kegiatan.setCalonMahasiswa(bcm);
			kegiatan.setMahasiswa(mahasiswa);
			kegiatan.setTahunAkademik(vaNtt.getTahunAkademik());
			kegiatan.setSemster(semester);
			kegiatan.setJenisKegiatan(jenisKegiatan);
			kegiatan.setTanggal(tanggal);
			kegiatan.setValidated(1);
			kegiatan.setValidator(bank);

			// Simpan Kegiatan
			session.getTransaction().begin();
			if (kegiatan.getId() == null)
				session.save(kegiatan);
			else
				Common.refreshSaveOrUpdate(session, kegiatan);
			session.getTransaction().commit();
			session = pastikanSessionAktif(session);

			List<Long> detailBiayasId = new ArrayList<Long>();
			for (String id : StringUtils.split(vaNtt.getDetailbiaya(), ",")) {
				try {
					detailBiayasId.add(Long.parseLong(id.trim()));
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Va.java:1043");
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

			boolean checkCicilanLama = !Common.bolehKonfigurasi("ngakUsahCheckCicilanLama", Konfigurasi.TIDAK_AKTIF);

			// Buka satu transaksi untuk semua cicilan agar lebih efisien
			session.getTransaction().begin();
			System.out.println("[VA Servlet - Payment] Memproses daftar cicilan...");

			if (vaNtt.getCicilan() != null && !vaNtt.getCicilan().isEmpty()) {
				for (String idPemBul : StringUtils.split(vaNtt.getCicilan(), ",")) {
					// Logic pemetaan Bulanan & Item Biaya
					Double subtotal = 0.0;
					String namaCicilan = "";
					String bulanCicilan = "";

					if (Common.isNumber(idPemBul)) {
						PengaturanPembayaranBulanan ppb = (PengaturanPembayaranBulanan) session
								.createCriteria(PengaturanPembayaranBulanan.class)
								.add(Restrictions.idEq(Long.parseLong(idPemBul))).uniqueResult();
						if (ppb != null) {
							String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-" + vaNtt.getId();
							subtotal = ppb.ambilNominalModifikasi(mahasiswa, semester);
							saveCicilan(session, checkCicilanLama, ref, kegiatan, ppb.getDetailBiaya(),
									ppb.getDetailBiaya().getItemBiaya(), ppb, vaNtt.getId(), subtotal, tanggal, bank,
									bankHost);
							namaCicilan = ppb.getDetailBiaya().getItemBiaya().getNama();
							bulanCicilan = ppb.getNamaBulan();
						}
					} else if (idPemBul != null && idPemBul.startsWith("Bulanan-")) {
						PengaturanPembayaranBulanan ppb = (PengaturanPembayaranBulanan) session
								.createCriteria(PengaturanPembayaranBulanan.class)
								.add(Restrictions.idEq(Long.parseLong(idPemBul.split("-")[1]))).uniqueResult();
						if (ppb != null) {
							String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-" + vaNtt.getId();
							try {
								subtotal = Double.parseDouble(idPemBul.split("-")[idPemBul.split("-").length - 1]);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Va.java:1091");
							}
							if (ppb.getDetailBiaya().getItemBiaya().getPenghitungan()
									.equals(ItemBiaya.DIKALI_NILAI_MINUS))
								subtotal = 0.0 - subtotal;

							saveCicilan(session, checkCicilanLama, ref, kegiatan, ppb.getDetailBiaya(),
									ppb.getDetailBiaya().getItemBiaya(), ppb, vaNtt.getId(), subtotal, tanggal, bank,
									bankHost);
							namaCicilan = ppb.getDetailBiaya().getItemBiaya().getNama();
							bulanCicilan = ppb.getNamaBulan();
						}
					} else if (idPemBul != null && idPemBul.startsWith("Item-")) {
						ItemBiaya itemBiaya = (ItemBiaya) session.createCriteria(ItemBiaya.class)
								.add(Restrictions.idEq(Long.parseLong(idPemBul.split("-")[1]))).uniqueResult();
						if (itemBiaya != null) {
							String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-" + vaNtt.getId();
							try {
								subtotal = Double.parseDouble(idPemBul.split("-")[2]);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Va.java:1110");
							}
							Long detailBiayaId = null;
							try {
								detailBiayaId = Long.parseLong(idPemBul.split("-")[4]);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Va.java:1115");
							}

							if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS))
								subtotal = 0.0 - subtotal;
							DetailBiaya db = DetailBiaya.muatRefAman(session, detailBiayaId);

							saveCicilan(session, checkCicilanLama, ref, kegiatan, db, itemBiaya, null, vaNtt.getId(),
									subtotal, tanggal, bank, bankHost);
							namaCicilan = itemBiaya.getNama();
							bulanCicilan = "";
						}
					} else if (idPemBul.startsWith("Keranjang-")) {
						// Pembayaran Keranjang Belanja (multi jenis / KegiatanTemporary): konversi draf
						// menjadi Kegiatan+Cicilan nyata — pemroses terpusat yang sama dengan Esmartlink.
						// Helper mengelola transaksinya sendiri, jadi transaksi batch cicilan yang sedang
						// terbuka ditutup dulu lalu dibuka lagi agar commit batch di bawah tetap valid.
						if (session.getTransaction() != null && session.getTransaction().isActive()) {
							session.getTransaction().commit();
						}
						ais.action.ws.util.PembayaranGatewayHelper.prosesSatuTokenKeranjang(session, idPemBul,
								vaNtt, false, bank, bankHost, tanggal, data, null);
						session.getTransaction().begin();
					}

					if (!namaCicilan.isEmpty()) {
						JSONObject jr = new JSONObject();
						jr.put("nama", namaCicilan);
						jr.put("bulan", bulanCicilan);
						jr.put("nominal", subtotal);
						rincian.put(jr);
					}
				}
			} else {
				for (DetailBiaya detailBiaya : detailBiayas) {
					ItemBiaya itemBiaya = detailBiaya.getItemBiaya();
					if (itemBiaya != null) {
						String ref = "ntt-" + kegiatan.getId() + "-" + detailBiaya.getId() + "-" + vaNtt.getId();
						Double subtotal = Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya);
						if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS))
							subtotal = 0.0 - subtotal;

						saveCicilan(session, checkCicilanLama, ref, kegiatan, detailBiaya, itemBiaya, null,
								vaNtt.getId(), subtotal, tanggal, bank, bankHost);

						JSONObject jr = new JSONObject();
						jr.put("nama", itemBiaya.getNama());
						jr.put("bulan", "");
						jr.put("nominal", subtotal);
						rincian.put(jr);
					}
				}
			}
			session.getTransaction().commit(); // Selesaikan transaksi cicilan

			// Hitung Denda & Update Kegiatan
			Double[] d = PembayaranUtil.getInstance().getTotalDanDendaFromCicilan(session, kegiatan);
			Double jumlah = d[0];
			Double denda = d[1];
			kegiatan.setDenda(denda);
			kegiatan.setAmountTerhutang(nilaiBiayaHarusDiBayars - (jumlah - denda));
			kegiatan.setAmount(jumlah > 0.1 ? jumlah : vaNtt.getTotal());
			kegiatan.setValidator(bank);

			if (bankHost != null && vaNtt.getBankHost() == null)
				vaNtt.setBankHost(bankHost);

			session.getTransaction().begin();
			Common.refreshUpdate(session, kegiatan);
			session.getTransaction().commit();

			System.out.println("[VA Servlet - Payment] Mengupdate status VA menjadi Terbayar");
			VirtualAccountBank.updateVa(vaNtt, tanggal, kegiatan, data, bank);

			// Bangun JSON Balasan
			jsonObject = new JSONObject();
			boolean isSukses = VirtualAccountBank.isSudahTerbayar(vaNtt);
			jsonObject.put("status", isSukses ? "00" : "02");
			jsonObject.put("description", isSukses ? "Pembayaran Sukses" : "Pembayaran Gagal");
			jsonObject.put("nim", nim);
			jsonObject.put("nama", nama);
			jsonObject.put("semester", semester);
			jsonObject.put("jenis", jenisKegiatan.getNamaKegiatan());
			jsonObject.put("nominal", vaNtt.getTotal());
			jsonObject.put("keterangan", vaNtt.getKeterangan());
			jsonObject.put("noref", kegiatan.getRefNumber() + "." + vaNtt.getKode());
			jsonObject.put("tahun_akademik", vaNtt.getTahunAkademik());
			jsonObject.put("fakultas", fakultas);
			jsonObject.put("prodi", prodi);
			jsonObject.put("rincian", rincian);
		}

		return jsonObject.toString();
	}

	// =========================================================================================
	// IMPLEMENTASI METHOD H2H REVERSAL (PEMBATALAN TRANSAKSI)
	// =========================================================================================
	/**
	 * Menjalankan {@code action=reversal}: membatalkan setoran yang sebelumnya sudah diposting
	 * atas sebuah VA, sehingga tagihan kembali berstatus belum dibayar.
	 *
	 * <h4>Prasyarat</h4>
	 * <p>Pembatalan hanya berjalan bila kolom {@code kegiatan} pada VA sudah terisi &mdash; yaitu
	 * VA memang pernah dibayar. Bila masih kosong, method langsung mengembalikan balasan berstatus
	 * {@code 02} "Tagihan Belum Dibayar" tanpa menyentuh basis data.</p>
	 *
	 * <p>Berbeda dari {@code inquiry} dan {@code payment}, jalur ini <b>tidak</b> memeriksa
	 * kedaluwarsa VA lewat {@link #isExpired} dan tidak membandingkan nominal apa pun.</p>
	 *
	 * <h4>Langkah pembatalan</h4>
	 * <ol>
	 *   <li>{@link Kegiatan} yang bersangkutan dimuat &mdash; dari kolom {@code kegiatan} pada VA,
	 *       atau dicari ulang berdasarkan mahasiswa/calon, jenis kegiatan, dan semester &mdash;
	 *       lalu atribut identitasnya ditulis ulang dan disimpan;</li>
	 *   <li>relasi {@code kegiatan} pada objek VA di memori dikosongkan;</li>
	 *   <li>daftar {@link DetailBiaya} dimuat dari kolom {@code detailbiaya} untuk menghitung
	 *       ulang {@code nilaiBiayaHarusDiBayars}, sekaligus menyegarkan {@link DetailKegiatan}
	 *       terkait;</li>
	 *   <li><b>seluruh baris {@code cicilan_pembayaran} yang menunjuk VA ini dihapus</b> lewat
	 *       satu perintah SQL native {@code delete from cicilan_pembayaran where ref_va = ...}.
	 *       Nilai yang disisipkan berasal dari {@code vaNtt.getId()}, yaitu {@code Long} hasil
	 *       pembacaan basis data, bukan masukan pemanggil, sehingga tidak membuka celah injeksi
	 *       SQL &mdash; tetapi tetap jangan mengubah pola perangkaian string ini untuk nilai lain;</li>
	 *   <li>total dan denda dihitung ulang dari cicilan yang tersisa, lalu {@link Kegiatan}
	 *       diperbarui;</li>
	 *   <li>{@code VirtualAccountBank.updateVa} dipanggil dengan kegiatan bernilai {@code null}
	 *       untuk mengembalikan VA ke status belum dibayar.</li>
	 * </ol>
	 *
	 * <p>Status balasan dibaca ulang dari kondisi VA sesudah pembatalan: {@code 00} "Reversal
	 * Sukses" bila kolom {@code kegiatan} benar-benar sudah kosong, {@code 02} bila belum.</p>
	 *
	 * <p>Nilai {@code nominal} pada balasan awalnya diisi total VA, lalu ditimpa dengan jumlah
	 * seluruh komponen cicilan yang berhasil diurai bila kolom {@code cicilan} tidak kosong.
	 * Penguraian tokennya mengikuti aturan yang sama dengan {@link #prosesH2HInquiry}, kecuali
	 * bentuk {@code Keranjang-} yang tidak ditangani di sini.</p>
	 *
	 * @param vaNtt      VA yang dibatalkan; dijamin tidak {@code null} oleh pemanggil
	 * @param session    session Hibernate aktif milik {@link #doProses}
	 * @param bank       nama bank pemanggil; disimpan sebagai {@code validator}
	 * @param data       badan permintaan mentah, disimpan sebagai jejak
	 * @param jsonObject objek balasan awal; selalu diganti objek baru di dalam method ini
	 * @param rincian    array rincian yang akan diisi; dimodifikasi di tempat
	 * @param nim        penampung NIM; diisi ulang dari data VA
	 * @param nama       penampung nama; diisi ulang dari data VA
	 * @param tanggalP   tanggal pembatalan versi bank dalam bentuk string mentah
	 * @return badan balasan JSON dalam bentuk string
	 * @throws Exception bila salah satu transaksi atau query gagal
	 */
	@SuppressWarnings("unchecked")
	private static String prosesH2HReversal(VirtualAccountBank vaNtt, Session session, String bank, String data,
			JSONObject jsonObject, JSONArray rincian, String nim, String nama, String tanggalP) throws Exception {

		System.out.println("[VA Servlet - Reversal] Memulai proses H2H Reversal. VA: " + vaNtt.getKode());

		JenisKegiatan jenisKegiatan = vaNtt.getJenisKegiatan();
		Mahasiswa mahasiswa = vaNtt.getMahasiswa();
		BiodataCalonMahasiswa bcm = vaNtt.getBiodataCalonMahasiswa();
		Integer semester = vaNtt.getSemester();

		nim = mahasiswa == null ? bcm.getNoRegistrasi() : mahasiswa.getNim();
		nama = mahasiswa == null ? bcm.getNama() : mahasiswa.getNama();
		Date tanggal = parseTanggalPayment(tanggalP, bank);

		Kegiatan kegiatan = vaNtt.getKegiatan() != null
				? (Kegiatan) session.createCriteria(Kegiatan.class).add(Restrictions.idEq(vaNtt.getKegiatan()))
						.uniqueResult()
				: null;

		String fakultas = mahasiswa == null ? (bcm.getProdi1() == null ? "" : bcm.getProdi1().getFakultas().getNama())
				: getSafeFakultas(mahasiswa);
		String prodi = mahasiswa == null ? (bcm.getProdi1() == null ? "" : bcm.getProdi1().getNama())
				: getSafeProdi(mahasiswa);

		if (bcm != null && bcm.getProdiLulus() != null) {
			prodi = getSafeProdiLulus(bcm);
			fakultas = getSafeFakultasLulus(bcm);
		}

		if (vaNtt.getKegiatan() == null) {
			System.out.println("[VA Servlet - Reversal] Reversal ditolak. Tagihan belum dibayar.");
			jsonObject = new JSONObject();
			jsonObject.put("status", "02");
			jsonObject.put("description", "Tagihan Belum Dibayar");
			jsonObject.put("nim", nim);
			jsonObject.put("nama", nama);
			jsonObject.put("semester", semester);
			jsonObject.put("tahun_akademik", vaNtt.getTahunAkademik());
			jsonObject.put("jenis", jenisKegiatan.getNamaKegiatan());
			jsonObject.put("nominal", vaNtt.getTotal());
			jsonObject.put("keterangan", vaNtt.getKeterangan());
			jsonObject.put("fakultas", fakultas);
			jsonObject.put("prodi", prodi);
			return jsonObject.toString();
		}

		// Proses Pembatalan Kegiatan
		if (kegiatan == null || kegiatan.getId() == null) {
			kegiatan = (Kegiatan) session.createCriteria(Kegiatan.class).addOrder(Order.asc("id"))
					.add(bcm != null ? Restrictions.eq("calonMahasiswa", bcm) : Restrictions.eq("mahasiswa", mahasiswa))
					.add(Restrictions.eq("jenisKegiatan", vaNtt.getJenisKegiatan()))
					.add(Restrictions.eq("semster", semester)).setMaxResults(1).uniqueResult();
		}

		if (kegiatan == null)
			kegiatan = new Kegiatan();

		kegiatan.setKodeUnikLain(vaNtt.getKodeUnikLain());
		kegiatan.setJadwalPembayaran(vaNtt.getJadwalPembayaran());
		kegiatan.setNama(nama);
		kegiatan.setAmount(vaNtt.getTotal());
		kegiatan.setCalonMahasiswa(bcm);
		kegiatan.setMahasiswa(mahasiswa);
		kegiatan.setTahunAkademik(vaNtt.getTahunAkademik());
		kegiatan.setSemster(semester);
		kegiatan.setJenisKegiatan(jenisKegiatan);
		kegiatan.setTanggal(tanggal);
		kegiatan.setValidated(1);
		kegiatan.setValidator(bank);

		session.getTransaction().begin();
		Common.refreshSaveOrUpdate(session, kegiatan);
		session.getTransaction().commit();

		vaNtt.setKegiatan(null);

		List<Long> detailBiayasId = new ArrayList<Long>();
		for (String id : StringUtils.split(vaNtt.getDetailbiaya(), ",")) {
			try {
				detailBiayasId.add(Long.parseLong(id.trim()));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Va.java:1294");
			}
		}

		Collection<DetailBiaya> detailBiayas = session.createCriteria(DetailBiaya.class).add(
				detailBiayasId.isEmpty() ? Restrictions.sqlRestriction("false") : Restrictions.in("id", detailBiayasId))
				.list();

		Double nilaiBiayaHarusDiBayars = 0.0;
		for (DetailBiaya detailBiaya : detailBiayas) {
			DetailKegiatan detailKegiatan = kegiatan.ambilSatuDetailKegiatan(detailBiaya, session);
			if (detailKegiatan == null)
				detailKegiatan = new DetailKegiatan();

			Double biaya = detailBiaya.hitungTotalKegiatan(kegiatan, session);
			detailKegiatan.setBiaya(biaya);
			detailKegiatan.setDetailBiaya(detailBiaya);
			detailKegiatan.setKeterangan(detailBiaya.getKeterangan());
			detailKegiatan.setKegiatan(kegiatan);

			nilaiBiayaHarusDiBayars += Kegiatan.ambilJumlahTagihan(detailKegiatan, kegiatan, detailBiaya, false);
		}

		System.out.println("[VA Servlet - Reversal] Menghapus cicilan pembayaran di DB...");
		session.getTransaction().begin();
		session.createSQLQuery("delete from cicilan_pembayaran where ref_va = " + vaNtt.getId()).executeUpdate();
		session.getTransaction().commit();

		Double[] d = PembayaranUtil.getInstance().getTotalDanDendaFromCicilan(session, kegiatan);
		Double jumlah = d[0];
		Double denda = d[1];
		kegiatan.setDenda(denda);
		kegiatan.setAmountTerhutang(nilaiBiayaHarusDiBayars - (jumlah - denda));
		kegiatan.setAmount(jumlah > 0.1 ? jumlah : vaNtt.getTotal());
		kegiatan.setValidator(bank);

		session.getTransaction().begin();
		Common.refreshUpdate(session, kegiatan);
		session.getTransaction().commit();

		VirtualAccountBank.updateVa(vaNtt, tanggal, null, data, bank);

		jsonObject = new JSONObject();
		jsonObject.put("status", vaNtt.getKegiatan() == null ? "00" : "02");
		jsonObject.put("description", vaNtt.getKegiatan() == null ? "Reversal Sukses" : "Reversal Gagal");
		jsonObject.put("nim", nim);
		jsonObject.put("nama", nama);
		jsonObject.put("semester", semester);
		jsonObject.put("jenis", jenisKegiatan.getNamaKegiatan());
		jsonObject.put("nominal", vaNtt.getTotal());

		Double total = 0.0;
		if (vaNtt.getCicilan() != null && !vaNtt.getCicilan().isEmpty()) {
			for (String idPemBul : StringUtils.split(vaNtt.getCicilan(), ",")) {
				// Pembuatan rincian pembatalan
				if (Common.isNumber(idPemBul) || idPemBul.startsWith("Bulanan-")) {
					Long parseId = Common.isNumber(idPemBul) ? Long.parseLong(idPemBul)
							: Long.parseLong(idPemBul.split("-")[1]);
					PengaturanPembayaranBulanan ppb = (PengaturanPembayaranBulanan) session
							.createCriteria(PengaturanPembayaranBulanan.class).add(Restrictions.idEq(parseId))
							.uniqueResult();
					if (ppb != null) {
						Double subtotal = 0.0;
						if (Common.isNumber(idPemBul))
							subtotal = ppb.ambilNominalModifikasi(mahasiswa, semester);
						else {
							try {
								subtotal = Double.parseDouble(idPemBul.split("-")[idPemBul.split("-").length - 1]);
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Va.java:1362");
							}
							if (ppb.getDetailBiaya().getItemBiaya().getPenghitungan()
									.equals(ItemBiaya.DIKALI_NILAI_MINUS))
								subtotal = 0.0 - subtotal;
						}
						total += subtotal;
						JSONObject jr = new JSONObject();
						jr.put("nama", ppb.getDetailBiaya().getItemBiaya().getNama());
						jr.put("bulan", ppb.getNamaBulan());
						jr.put("nominal", subtotal);
						rincian.put(jr);
					}
				} else if (idPemBul.startsWith("Item-")) {
					ItemBiaya itemBiaya = (ItemBiaya) session.createCriteria(ItemBiaya.class)
							.add(Restrictions.idEq(Long.parseLong(idPemBul.split("-")[1]))).uniqueResult();
					if (itemBiaya != null) {
						Double subtotal = 0.0;
						try {
							subtotal = Double.parseDouble(idPemBul.split("-")[2]);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Va.java:1382");
						}
						if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS))
							subtotal = 0.0 - subtotal;
						total += subtotal;
						JSONObject jr = new JSONObject();
						jr.put("nama", itemBiaya.getNama());
						jr.put("bulan", "");
						jr.put("nominal", subtotal);
						rincian.put(jr);
					}
				}
			}
			jsonObject.put("nominal", total);
		}

		jsonObject.put("keterangan", vaNtt.getKeterangan());
		jsonObject.put("noref", kegiatan.getRefNumber() + "." + vaNtt.getKode());
		jsonObject.put("tahun_akademik", vaNtt.getTahunAkademik());
		jsonObject.put("fakultas", fakultas);
		jsonObject.put("prodi", prodi);
		jsonObject.put("rincian", rincian);

		return jsonObject.toString();
	}

	// =========================================================================================
	// HELPER METHODS UNTUK PAYMENT
	// =========================================================================================

	// Method Helper untuk menghemat baris penulisan logic Date
	/**
	 * Mengurai string tanggal setoran menjadi {@link Date} memakai format yang berbeda-beda
	 * menurut bank pengirim.
	 *
	 * <p>Pemetaan format dipilih dari potongan nama bank (huruf kecil):</p>
	 * <ul>
	 *   <li>mengandung {@code ntt} &rarr; {@code Common.datetimeFormat2s};</li>
	 *   <li>mengandung {@code btn} &rarr; {@code Common.datetimeFormat1s};</li>
	 *   <li>mengandung {@code bmi} &rarr; {@code Common.dateFormat9};</li>
	 *   <li>selain itu &rarr; {@code Common.datetimeFormat1s}.</li>
	 * </ul>
	 *
	 * <p>Bila {@code tanggalP} kosong atau penguraiannya gagal, dipakai waktu sekarang dari
	 * {@code WaktuUtil.getDate()} &mdash; jadi format yang salah tidak menggagalkan posting,
	 * melainkan diam-diam mengubah tanggal transaksi menjadi waktu server.</p>
	 *
	 * @param tanggalP string tanggal dari payload bank; boleh {@code null}/kosong
	 * @param bank     nama bank pengirim; menentukan format yang dipakai
	 * @return tanggal hasil penguraian, atau waktu sekarang bila tidak dapat diurai
	 */
	private static Date parseTanggalPayment(String tanggalP, String bank) {
		Date tanggal = ais.ui.util.WaktuUtil.getDate();
		if (tanggalP == null || tanggalP.isEmpty())
			return tanggal;
		try {
			if (bank != null && bank.toLowerCase().contains("ntt"))
				return Common.datetimeFormat2s.get().parse(tanggalP);
			if (bank != null && bank.toLowerCase().contains("btn"))
				return Common.datetimeFormat1s.get().parse(tanggalP);
			if (bank != null && bank.toLowerCase().contains("bmi"))
				return Common.dateFormat9.get().parse(tanggalP);
			return Common.datetimeFormat1s.get().parse(tanggalP);
		} catch (Exception e) {
			return tanggal;
		}
	}

	// Method Helper untuk menyimpan Cicilan ke Database (Mencegah duplikasi baris
	// logic panjang)
	/**
	 * Menyimpan satu baris {@link CicilanPembayaran} untuk sebuah komponen tagihan yang dibayar.
	 *
	 * <p>Bila {@code checkCicilanLama} bernilai {@code true}, cicilan dengan {@code ref} yang sama
	 * dicari lebih dulu; bila ditemukan, method <b>tidak melakukan apa pun</b>. Inilah penjaga
	 * anti-dobel-posting ketika host bank mengirim ulang notifikasi yang sama. Penjaga tersebut
	 * dimatikan bila konfigurasi {@code ngakUsahCheckCicilanLama} diaktifkan.</p>
	 *
	 * <p>Referensi {@code ref} disusun pemanggil dengan pola
	 * {@code ntt-<idKegiatan>-<tokenCicilan>-<idVA>} sehingga unik per kombinasi kegiatan, item,
	 * dan VA.</p>
	 *
	 * <p>Jenis pembayaran diambil dari {@link BankHost}; bila host atau jenis pembayarannya tidak
	 * ada, dipakai {@code ConstantValues.TUNAI}. Kolom {@code denda} selalu diisi nol di sini
	 * &mdash; denda dihitung belakangan oleh {@code PembayaranUtil.getTotalDanDendaFromCicilan}.
	 * Baris baru selalu disimpan lewat {@code session.save}; cabang {@code refreshUpdate} secara
	 * praktis tak terjangkau karena objek yang baru dibuat pasti belum punya id.</p>
	 *
	 * @param session          session Hibernate aktif dengan transaksi yang sudah dibuka pemanggil
	 * @param checkCicilanLama bila {@code true}, lewati penyimpanan saat {@code ref} sudah ada
	 * @param ref              kunci idempotensi cicilan
	 * @param kegiatan         kegiatan pembayaran induk
	 * @param db               detail biaya terkait; boleh {@code null} pada token tertentu
	 * @param ib               item biaya yang dibayar
	 * @param ppb              pengaturan pembayaran bulanan bila komponen ini bersifat bulanan;
	 *                         {@code null} untuk komponen non-bulanan
	 * @param vaId             id VA sumber, disimpan pada kolom {@code refVa} agar bisa dihapus
	 *                         massal saat reversal
	 * @param subtotal         nominal komponen ini; sudah bertanda negatif untuk potongan
	 * @param tanggal          tanggal transaksi
	 * @param bank             nama bank pemanggil, disimpan sebagai {@code validator}
	 * @param bankHost         host bank pemanggil; boleh {@code null}
	 */
	private static void saveCicilan(Session session, boolean checkCicilanLama, String ref, Kegiatan kegiatan,
			DetailBiaya db, ItemBiaya ib, PengaturanPembayaranBulanan ppb, Long vaId, Double subtotal, Date tanggal,
			String bank, BankHost bankHost) {

		CicilanPembayaran cicilan = null;
		if (checkCicilanLama) {
			cicilan = (CicilanPembayaran) session.createCriteria(CicilanPembayaran.class)
					.add(Restrictions.eq("ref", ref)).setMaxResults(1).uniqueResult();
		}

		if (cicilan == null) {
			cicilan = new CicilanPembayaran(db);
			cicilan.setRef(ref);
			cicilan.setValidator(bank);
			cicilan.setKegiatan(kegiatan);
			cicilan.setItemBiaya(ib);
			cicilan.setPengaturanPembayaranBulanan(ppb);
			cicilan.setRefVa(vaId);
			cicilan.setNilai(subtotal);
			cicilan.setNilaiAsli(subtotal);
			cicilan.setTanggal(tanggal);
			cicilan.setJenisPembayaran(bankHost == null || bankHost.getJenisPembayaran() == null ? ConstantValues.TUNAI
					: bankHost.getJenisPembayaran());
			cicilan.setDenda(0.0);

			if (cicilan.getId() == null)
				session.save(cicilan);
			else
				Common.refreshUpdate(session, cicilan);
		}
	}
}
