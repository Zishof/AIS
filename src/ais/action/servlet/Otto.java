package ais.action.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

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
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.LogHostToHost;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.VirtualAccountBank;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Tagihan;
import ais.ui.util.WaktuUtil;

/**
 * Servlet gateway <i>host-to-host</i> (H2H) penyedia pembayaran <b>OTTO</b> untuk skema Virtual
 * Account: melayani <b>inquiry</b> (tanya tagihan), <b>payment</b> (pelunasan), dan
 * <b>reversal</b> (pembatalan pembayaran) atas baris {@link VirtualAccountBank}.
 *
 * <p>Terpasang di {@code web.xml} sebagai servlet {@code Otto} pada URL {@code /Otto}, dengan
 * {@link FilterOtto} terpasang pada pola URL yang sama. Filter tersebut hanya membungkus
 * <i>response</i> (mencatat setiap header yang disetel dan menahan header
 * {@code Authorization}); ia <b>bukan</b> penyaring alamat IP dan tidak melakukan otentikasi
 * apa pun atas permintaan yang masuk.</p>
 *
 * <p>Identitas penyedianya dipastikan dari kode, bukan dari namanya saja: payload memakai field
 * {@code trxRef} dan {@code amount}, URI pembatalan berakhiran {@code "OttoReversal"}, dan
 * simulasi timeout memakai kunci konfigurasi {@code otto_va_sleep} — pola khas gerbang pembayaran
 * OTTO, bukan lembaga pengawas.</p>
 *
 * <h4>Asal-usul: salinan dari {@link BMS}</h4>
 * <p>Berkas ini adalah <b>salinan struktural {@link BMS}</b>. Penyesuaian yang sudah dilakukan
 * meliputi nama field JSON ({@code trxRef} dan {@code amount} menggantikan {@code nim} dan
 * {@code paymentAmount}), URI reversal, kunci konfigurasi, serta penambahan {@link #doHead}.
 * Waktu transaksi tidak dikirim penyedia ini, sehingga {@link #doProses} meneruskan
 * {@code tanggalP} bernilai {@code null} dan {@link #doProcess} jatuh ke waktu server saat ini.</p>
 *
 * <p><b>Satu penanda milik BMS masih tertinggal</b>: {@link #process} menetapkan label bank
 * {@code "BMS"}. Karena label itulah yang disimpan sebagai {@code validator} pada {@link Kegiatan}
 * dan {@link CicilanPembayaran}, pembayaran lewat kanal ini tercatat atas nama BMS. Bahwa URI dan
 * kunci konfigurasi sudah bernama Otto sementara label bank belum menunjukkan penyesuaian yang
 * belum tuntas. Dicatat apa adanya; perbaikannya di luar cakupan berkas ini.</p>
 *
 * <h4>Model autentikasi (tidak ada tanda tangan digital)</h4>
 * <p>Sama seperti {@link BMS}, {@link Nagari}, dan {@link BSI}, kanal ini <b>tidak memverifikasi
 * tanda tangan, HMAC, token, maupun kata sandi</b> — baik pada cabang inquiry, pembayaran, maupun
 * reversal. Satu-satunya pengenal pemanggil adalah pencocokan alamat IP lewat
 * {@link PembayaranUtil#getBankHost(String, String)}, dan hasilnya <b>tidak dipakai sebagai
 * gerbang</b>: {@code bankHost} bernilai {@code null} untuk IP tak dikenal, namun pemrosesan tetap
 * berlanjut. Bandingkan dengan {@link Bsiresponse} yang payload-nya harus dibuka memakai kunci
 * rahasia bersama sehingga verifikasi terjadi pada jalur transaksi. IP dibaca dari
 * {@link HttpServletRequest#getRemoteAddr()}, bukan dari header {@code X-Forwarded-For} yang dapat
 * dipalsukan klien.</p>
 *
 * @see BMS
 * @see Nagari
 * @see FilterOtto
 * @see VirtualAccountBank
 */
public class Otto extends HttpServlet {
	/** Versi serialisasi bawaan {@link HttpServlet}; tidak dipakai untuk logika apa pun. */
	private static final long serialVersionUID = 1L;

	/**
	 * Singleton utilitas pembayaran; dipakai untuk memetakan alamat IP pemanggil menjadi
	 * {@link BankHost} dan untuk menghitung total serta denda dari cicilan.
	 */
	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/**
	 * Pemformat tanggal {@code yyyyMMddHHmmss}, warisan dari {@link BMS}.
	 *
	 * <p>Penyedia OTTO tidak mengirimkan waktu transaksi, sehingga {@link #doProses} meneruskan
	 * {@code null} dan penguraian di {@link #doProcess} selalu gagal lalu jatuh ke waktu server
	 * saat ini. Dengan demikian pemformat ini praktis tidak terpakai pada alur normal, dan tetap
	 * dipertahankan mengikuti bentuk berkas asalnya.</p>
	 *
	 * <p>Dibungkus {@link ThreadLocal} karena {@link SimpleDateFormat} tidak aman dipakai bersama
	 * antar-thread, sedangkan servlet dilayani banyak thread sekaligus.</p>
	 */
	private static final ThreadLocal<SimpleDateFormat> dateFormat = new ThreadLocal<SimpleDateFormat>() {
		/** Membuat pemformat baru untuk setiap thread yang pertama kali memakainya. */
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("yyyyMMddHHmmss");
		}
	};

	/**
	 * Konstruktor bawaan yang dipanggil kontainer servlet; tidak ada inisialisasi khusus karena
	 * seluruh keadaan bersifat statis.
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public Otto() {
		super();
	}

	/**
	 * Menangani permintaan HTTP GET dengan meneruskannya ke {@link #process}; perlakuannya sama
	 * dengan {@link #doPost}.
	 *
	 * <p>Exception apa pun ditelan di sini dan hanya diteruskan ke
	 * {@code Common.tampilErrorJikaAdmin}, sehingga kegagalan tak terduga tidak menjadi HTTP 500
	 * melainkan balasan kosong bertatus 200.</p>
	 *
	 * @param request  permintaan dari sisi penyedia pembayaran
	 * @param response tujuan penulisan balasan JSON
	 * @throws ServletException diwariskan dari kontrak {@link HttpServlet}; tidak dilempar sendiri
	 * @throws IOException      bila penulisan balasan gagal
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
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
	 * Menangani permintaan HTTP HEAD dengan meneruskannya sepenuhnya ke implementasi bawaan
	 * {@link HttpServlet#doHead}.
	 *
	 * <p>Method ini tidak menambahkan perilaku apa pun — implementasi bawaanlah yang menjalankan
	 * {@link #doGet} dan membuang badan balasannya sehingga hanya header yang terkirim. Kehadiran
	 * penimpaan kosong ini kemungkinan besar sisa kerangka yang dihasilkan IDE (komentar
	 * {@code TODO Auto-generated method stub} masih tertinggal), namun aman dipertahankan karena
	 * perilakunya identik dengan tanpa penimpaan.</p>
	 *
	 * <p>Perlu dicatat: karena bawaan {@code doHead} menjalankan {@link #doGet}, sebuah permintaan
	 * HEAD tetap melewati seluruh alur pemrosesan termasuk pembacaan badan permintaan dan
	 * kemungkinan pencatatan pembayaran — bukan sekadar pemeriksaan ketersediaan layanan.</p>
	 *
	 * @param req  permintaan HEAD dari klien
	 * @param resp tujuan penulisan header balasan
	 * @throws ServletException diteruskan dari implementasi bawaan
	 * @throws IOException      diteruskan dari implementasi bawaan
	 * @see HttpServlet#doHead(HttpServletRequest, HttpServletResponse)
	 */
	@Override
	protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO Auto-generated method stub

		super.doHead(req, resp);

	}

	/**
	 * Menangani permintaan HTTP POST — jalur yang dipakai penyedia pembayaran — dengan
	 * meneruskannya ke {@link #process}.
	 *
	 * @param request  permintaan dari sisi penyedia, badan JSON-nya dibaca di {@link #process}
	 * @param response tujuan penulisan balasan JSON
	 * @throws ServletException diwariskan dari kontrak {@link HttpServlet}; tidak dilempar sendiri
	 * @throws IOException      bila penulisan balasan gagal
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
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
	 * Inti gateway: mencari VA, menyusun balasan JSON, dan — sesuai mode — mencatat pelunasan atau
	 * membatalkannya.
	 *
	 * <h4>Tiga mode</h4>
	 * <ul>
	 *   <li><b>inquiry</b> ({@code inquery=true}) — hanya membaca tagihan dan mengisi
	 *       {@code billDetails}; tidak ada penyimpanan, sehingga pengecekan tagihan tidak pernah
	 *       tercatat sebagai pembayaran;</li>
	 *   <li><b>payment</b> — mencatat {@link Kegiatan} beserta {@link CicilanPembayaran} lalu
	 *       memanggil {@link VirtualAccountBank#updateVa};</li>
	 *   <li><b>reversal</b> ({@code reversal=true}) — membatalkan pembayaran dengan memutus
	 *       kaitan VA ke kegiatannya ({@code setKegiatan(null)}) sehingga VA kembali terbuka.</li>
	 * </ul>
	 *
	 * <h4>Urutan pemeriksaan dan kode galat</h4>
	 * <ol>
	 *   <li>VA tidak ditemukan &rarr; {@code errorCode=01} "Nomor VA salah";</li>
	 *   <li>VA kedaluwarsa &rarr; {@code errorCode=02} "Tagihan tidak tersedia" (dilewati pada
	 *       mode reversal, karena pembatalan justru wajar terjadi setelah masa berlaku habis);</li>
	 *   <li>reversal atas VA yang belum pernah dibayar &rarr; {@code errorCode=05}
	 *       "Reversal gagal";</li>
	 *   <li>{@code VirtualAccountBank.isSudahTerbayarUntukPayment} &rarr; {@code errorCode=03}
	 *       "Tagihan sudah terbayar", pencegah pembayaran ganda;</li>
	 *   <li><b>pencocokan nominal</b> — pada mode pembayaran {@code nominalP} harus sama persis
	 *       (sebagai {@code int}) dengan {@code getTotal() + getBiayaAdmin()}; bila tidak,
	 *       {@code errorCode=04} "Nominal Tagihan tidak sesuai";</li>
	 *   <li>kegagalan tak terduga &rarr; {@code errorCode=91} "Link Down", atau
	 *       {@code errorCode=05} bila sedang reversal.</li>
	 * </ol>
	 *
	 * <h4>Waktu transaksi</h4>
	 * <p>Penyedia ini tidak mengirim waktu transaksi, sehingga {@link #doProses} meneruskan
	 * {@code tanggalP} bernilai {@code null}. Penguraiannya dijamin gagal dan tertangkap, lalu
	 * seluruh pembukuan memakai waktu server saat permintaan diterima.</p>
	 *
	 * <h4>Dua cabang pembayar</h4>
	 * <p>VA milik siswa/calon siswa diproses {@link VirtualAccountBank#bayarSiswa}; VA milik
	 * mahasiswa/calon mahasiswa diproses di tempat dengan membuat atau memperbarui
	 * {@link Kegiatan} dan baris {@link CicilanPembayaran} untuk tiap komponen pada kolom
	 * {@code cicilan}, yang dikenali dari awalannya ({@code "Bulanan-"}, {@code "Item-"},
	 * {@code "Keranjang-"}, atau angka murni).</p>
	 *
	 * <p>Blok {@code finally} <b>selalu</b> memanggil
	 * {@code PembayaranGatewayHelper.catatLogHostToHost} tanpa syarat {@code bankHost}, sehingga
	 * permintaan dari IP tak dikenal pun tetap meninggalkan jejak audit. Ini keputusan arsitektur
	 * yang disengaja, bukan kelalaian pemeriksaan.</p>
	 *
	 * @param nominalP nominal setoran dari penyedia; diabaikan saat inquiry
	 * @param tanggalP waktu transaksi; pada alur normal selalu {@code null} sehingga dipakai waktu
	 *                 server saat ini
	 * @param va       penanda transaksi yang membawa nomor Virtual Account (field {@code trxRef})
	 * @param bank     label bank yang disimpan sebagai {@code validator}; saat ini bernilai
	 *                 {@code "BMS"} warisan salinan {@link BMS}
	 * @param bankHost host hasil pencocokan IP; boleh {@code null} dan tidak menjadi gerbang
	 * @param request  permintaan asli, dipakai untuk pencatatan log H2H
	 * @param data     badan permintaan mentah; juga menjadi kerangka objek balasan
	 * @param chekLagi penanda dari pemanggil; pada alur servlet selalu {@code true}
	 * @param inquery  {@code true} untuk mode tanya tagihan yang bersifat baca-saja
	 * @param reversal {@code true} untuk pembatalan pembayaran
	 * @param chek     mode pemeriksaan internal; melonggarkan pemeriksaan kedaluwarsa dan
	 *                 "sudah terbayar"
	 * @return objek JSON balasan berisi {@code errorCode}, {@code statusDescription}, identitas
	 *         pembayar, dan — pada inquiry — {@code billDetails}
	 * @throws Exception bila penyusunan JSON gagal; kegagalan basis data ditangkap di dalam dan
	 *                   diubah menjadi {@code errorCode=91}
	 */
	@SuppressWarnings("unchecked")
	public static JSONObject doProcess(double nominalP, String tanggalP, String va, String bank, BankHost bankHost,
			HttpServletRequest request, String data, boolean chekLagi, boolean inquery, boolean reversal, boolean chek)
			throws Exception {

		JSONObject response = new JSONObject(data);
		response.put("errorCode", "00");
		response.put("statusDescription", "Success");

		JSONArray billDetails = new JSONArray();

		if (inquery) {
			response.put("billDetails", billDetails);
		}

		{ // TANPA syarat bankHost: request bank apa pun WAJIB tercatat ke log H2H (lihat finally)
			String nim = "";
			String nama = "";
			JSONArray rincian = new JSONArray();
			// Jejak stack trace bila inquiry/pembayaran error; disimpan ke kolom log H2H.
			String h2hStackTrace = null;

			VirtualAccountBank virtualAccountBankNtt = null;
			try {

				virtualAccountBankNtt = VirtualAccountBank.ambilVa(va,  nominalP, bankHost);

				if (virtualAccountBankNtt != null && virtualAccountBankNtt.getKadaluarsa() != null) {
					System.out.println(
							"Kadaluara " + Common.databaseDateFormat.get().format(virtualAccountBankNtt.getKadaluarsa()));
				}

				if (virtualAccountBankNtt == null) {
					response.put("errorCode", "01");
					response.put("statusDescription", "Nomor VA salah");
				}

				else if (!reversal && !chek && virtualAccountBankNtt != null
						&& virtualAccountBankNtt.getKadaluarsa().before(WaktuUtil.getDate())) {

					response.put("errorCode", "02");
					response.put("statusDescription", "Tagihan tidak tersedia");

				} else {

					if (reversal && virtualAccountBankNtt != null && virtualAccountBankNtt.getTotal() > 0.1
							&& virtualAccountBankNtt.getKegiatan() == null
							&& virtualAccountBankNtt.getPembayaran() == null) {
						response = new JSONObject();
						response.put("errorCode", "05");
						response.put("statusDescription", "Reversal gagal");
					} else

					if (VirtualAccountBank.isSudahTerbayarUntukPayment(virtualAccountBankNtt, inquery, reversal, chek)) {
						response.put("errorCode", "03");
						response.put("statusDescription", "Tagihan sudah terbayar");
					}

					else if (virtualAccountBankNtt != null && virtualAccountBankNtt.getTotal() > 0.1) {

						String TahunID = "";
						try {
							TahunID = virtualAccountBankNtt.getTahunAkademik().split("/")[0];
							response.put("TahunID", TahunID);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Otto.java:162");
							// TODO: handle exception
						}

						Session session = HibernateUtil.getSessionFactory().openSession();
						try {

							Double nominal = nominalP;

							if (!inquery && nominal.intValue() != virtualAccountBankNtt.totalBiaya()) {
								response.put("errorCode", "04");
								response.put("statusDescription", "Nominal Tagihan tidak sesuai");
							} else {

								if (inquery) {

									response.put("billAmount", virtualAccountBankNtt.getTotal().intValue()
											+ virtualAccountBankNtt.getBiayaAdmin().intValue());

									if (virtualAccountBankNtt.getBiayaAdmin() > 0.1) {
										JSONObject bill = new JSONObject();
										bill.put("billID", "0010");
										bill.put("billName", "Biaya admin");
										bill.put("billNameID", "0");
										bill.put("billAmount", virtualAccountBankNtt.getBiayaAdmin().intValue());

										bill.put("billAmountBayar", 0);
										bill.put("billAmountPay", 0);
										bill.put("TahunID", TahunID);

										billDetails.put(bill);

									}
								} else {
									response.put("amount", virtualAccountBankNtt.getTotal().intValue()
											+ virtualAccountBankNtt.getBiayaAdmin().intValue());
								}

								Date tanggal = ais.ui.util.WaktuUtil.getDate();
								try {
									tanggal = dateFormat.get().parse(tanggalP);
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Otto.java:204");
								}

								if (virtualAccountBankNtt.getSiswa() != null
										|| virtualAccountBankNtt.getCalonSiswa() != null) {

									Sekolah sekolah = null;
									if (virtualAccountBankNtt.getSiswa() != null) {
										sekolah = virtualAccountBankNtt.getSiswa().getSekolah();
										nim = virtualAccountBankNtt.getSiswa().getNomorInduk();
										nama = virtualAccountBankNtt.getSiswa().getNama();

									} else if (virtualAccountBankNtt.getCalonSiswa() != null) {
										sekolah = virtualAccountBankNtt.getCalonSiswa().getSekolah();
										nim = virtualAccountBankNtt.getCalonSiswa().getNomorInduk();
										nama = virtualAccountBankNtt.getCalonSiswa().getNama();

									}

									response.put("Nama", nama);
									response.put("FormID", nim);
									response.put("ProdiNama", sekolah == null ? "" : sekolah.getNama());

									Map<String, List<Tagihan>> map = VirtualAccountBank
											.bayarSiswa(virtualAccountBankNtt, session, tanggal, bank, inquery, data, false);

									for (List<Tagihan> tagihans : map.values()) {

										for (Tagihan tagihan : tagihans) {

											if (inquery) {
												JSONObject bill = new JSONObject();

												bill.put("billID", tagihan.getItemBiayaSekolah().getKode());
												bill.put("billName", tagihan.getItemBiayaSekolah().getNama());
												bill.put("billNameID", tagihan.getItemBiayaSekolah().getId() + "");

												bill.put("billAmount", tagihan.getNominal().intValue());

												bill.put("billAmountBayar", 0);
												bill.put("billAmountPay", 0);
												bill.put("TahunID", TahunID);

												billDetails.put(bill);

												if (tagihan.getDenda() > 0.1) {
													bill = new JSONObject();
													bill.put("billID", "D" + tagihan.getItemBiayaSekolah().getKode());
													bill.put("billName",
															"Denda " + tagihan.getItemBiayaSekolah().getNama());
													bill.put("billNameID", "0" + tagihan.getItemBiayaSekolah().getId());
													bill.put("billAmount", tagihan.getDenda().intValue());

													bill.put("billAmountBayar", 0);
													bill.put("billAmountPay", 0);
													bill.put("TahunID", TahunID);

													billDetails.put(bill);
												}

											}
										}

									}

								} else {

									JenisKegiatan jenisKegiatan = virtualAccountBankNtt.getJenisKegiatan();
									Mahasiswa mahasiswa = virtualAccountBankNtt.getMahasiswa();
									BiodataCalonMahasiswa biodataCalonMahasiswa = virtualAccountBankNtt
											.getBiodataCalonMahasiswa();

									Integer semester = virtualAccountBankNtt.getSemester();

									nim = mahasiswa == null ? biodataCalonMahasiswa.getNoRegistrasi()
											: mahasiswa.getNim();
									nama = mahasiswa == null ? biodataCalonMahasiswa.getNama() : mahasiswa.getNama();
									Jurusan jurusan = mahasiswa == null ? null : mahasiswa.getJurusan();
									if (biodataCalonMahasiswa != null
											&& biodataCalonMahasiswa.getProdiLulus() != null) {
										jurusan = biodataCalonMahasiswa.getProdiLulus();
									} else if (biodataCalonMahasiswa != null
											&& biodataCalonMahasiswa.getProdi1() != null) {
										jurusan = biodataCalonMahasiswa.getProdi1();
									}

									response.put("Nama", nama);
									response.put("FormID", nim);
									response.put("Gelombang", biodataCalonMahasiswa == null
											|| biodataCalonMahasiswa.getGelombangPendaftaran() == null ? ""
													: biodataCalonMahasiswa.getGelombangPendaftaran().getNama());
									response.put("ProdiNama", jurusan == null ? "" : jurusan.getNama());
									response.put("FakultasNama",
											jurusan == null ? "" : jurusan.getFakultas().getNama());

									Kegiatan kegiatan = (Kegiatan) (virtualAccountBankNtt.getKegiatan() == null ? null
											: session.createCriteria(Kegiatan.class)
													.add(Restrictions.idEq(virtualAccountBankNtt.getKegiatan()))
													.uniqueResult());

									if (kegiatan == null || kegiatan.getId() == null) {

										kegiatan = (Kegiatan) session.createCriteria(Kegiatan.class)

												.addOrder(Order.asc("id"))

												.add(biodataCalonMahasiswa != null
														? Restrictions.eq("calonMahasiswa", biodataCalonMahasiswa)
														: Restrictions.eq("mahasiswa", mahasiswa))
												.add(Restrictions.eq("jenisKegiatan",
														virtualAccountBankNtt.getJenisKegiatan()))
												.add(Restrictions.eq("semster", semester))

												.setMaxResults(1).uniqueResult();
									}

									if (kegiatan == null || kegiatan.getId() == null) {
										kegiatan = new Kegiatan();
									}

									if (reversal) {

										kegiatan.setKodeUnikLain(virtualAccountBankNtt.getKodeUnikLain());
										kegiatan.setJadwalPembayaran(virtualAccountBankNtt.getJadwalPembayaran());
										kegiatan.setNama(nama);
										kegiatan.setUploadVirtualAccount(null);
										kegiatan.setAmount(virtualAccountBankNtt.getTotal());
										kegiatan.setCalonMahasiswa(biodataCalonMahasiswa);
										kegiatan.setMahasiswa(mahasiswa);
										kegiatan.setTahunAkademik(virtualAccountBankNtt.getTahunAkademik());
										kegiatan.setSemster(semester);
										kegiatan.setJenisKegiatan(jenisKegiatan);
										kegiatan.setTanggal(tanggal);
										kegiatan.setValidated(1);
										kegiatan.setValidator(bank);

										session.getTransaction().begin();
										Common.refreshSaveOrUpdate(session, kegiatan);
										session.getTransaction().commit();

										virtualAccountBankNtt.setKegiatan(null);

										List<Long> detailBiayasId = new ArrayList<Long>();
										for (String id : StringUtils.split(virtualAccountBankNtt.getDetailbiaya(),
												",")) {
											try {
												detailBiayasId.add(Long.parseLong(id.trim()));
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Otto.java:351");

											}
										}

										Collection<DetailBiaya> detailBiayas = session.createCriteria(DetailBiaya.class)
												.add(detailBiayasId.isEmpty() ? Restrictions.sqlRestriction("false")
														: Restrictions.in("id", detailBiayasId))
												.list();

										Double nilaiBiayaHarusDiBayars = 0.0;
										for (DetailBiaya detailBiaya : detailBiayas) {

											DetailKegiatan detailKegiatan = kegiatan
													.ambilSatuDetailKegiatan(detailBiaya, session);
											if (detailKegiatan == null) {
												detailKegiatan = new DetailKegiatan();
											}

											Double biaya = detailBiaya.hitungTotalKegiatan(kegiatan, session);

											detailKegiatan.setBiaya(biaya);

											detailKegiatan.setDetailBiaya(detailBiaya);
											detailKegiatan.setKeterangan(detailBiaya.getKeterangan());
											detailKegiatan.setKegiatan(kegiatan);

											nilaiBiayaHarusDiBayars += Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya);

										}

										session.getTransaction().begin();
										session.createSQLQuery("delete from cicilan_pembayaran where ref_va = "
												+ virtualAccountBankNtt.getId()).executeUpdate();
										session.getTransaction().commit();

										Double[] d = PembayaranUtil.getInstance().getTotalDanDendaFromCicilan(session,
												kegiatan);
										Double jumlah = d[0];
										Double denda = d[1];
										kegiatan.setDenda(denda.doubleValue());
										kegiatan.setAmountTerhutang(
												nilaiBiayaHarusDiBayars - (jumlah.doubleValue() - denda.doubleValue()));

										kegiatan.setAmount(jumlah.doubleValue() > 0.1 ? jumlah.doubleValue()
												: virtualAccountBankNtt.getTotal());
										kegiatan.setValidator(bank);

										session.getTransaction().begin();
										Common.refreshUpdate(session, kegiatan);
										session.getTransaction().commit();

										VirtualAccountBank.updateVa(virtualAccountBankNtt, tanggal, null, data, bank);
									} else {

										kegiatan.setKodeUnikLain(virtualAccountBankNtt.getKodeUnikLain());
										kegiatan.setJadwalPembayaran(virtualAccountBankNtt.getJadwalPembayaran());
										kegiatan.setNama(nama);
										kegiatan.setUploadVirtualAccount(null);
										kegiatan.setAmount(virtualAccountBankNtt.getTotal());
										kegiatan.setCalonMahasiswa(biodataCalonMahasiswa);
										kegiatan.setMahasiswa(mahasiswa);
										kegiatan.setTahunAkademik(virtualAccountBankNtt.getTahunAkademik());
										kegiatan.setSemster(semester);
										kegiatan.setJenisKegiatan(jenisKegiatan);
										kegiatan.setTanggal(tanggal);
										kegiatan.setValidated(1);
										kegiatan.setValidator(bank);

										session.getTransaction().begin();
										Common.refreshSaveOrUpdate(session, kegiatan);
										session.getTransaction().commit();

										List<Long> detailBiayasId = new ArrayList<Long>();
										for (String id : StringUtils.split(virtualAccountBankNtt.getDetailbiaya(),
												",")) {
											try {
												detailBiayasId.add(Long.parseLong(id.trim()));
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Otto.java:429");

											}
										}

										Collection<DetailBiaya> detailBiayas = session.createCriteria(DetailBiaya.class)
												.add(detailBiayasId.isEmpty() ? Restrictions.sqlRestriction("false")
														: Restrictions.in("id", detailBiayasId))
												.list();

										Double nilaiBiayaHarusDiBayars = 0.0;
										for (DetailBiaya detailBiaya : detailBiayas) {
											Double biaya = detailBiaya.hitungTotalKegiatan(kegiatan, session);

											nilaiBiayaHarusDiBayars += Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya);

										}

										Double[] totalCicilan = kegiatan.hitungTotalDanDendaFromCicilan();
										Double total = totalCicilan[0];
										Double totalTagihan = kegiatan.getAmount() + kegiatan.getAmountTerhutang();
										System.out.println("cicilanPembayaran total -> " + total + " totalTagihan "
												+ totalTagihan);

										if (virtualAccountBankNtt.getCicilan() != null
												&& !virtualAccountBankNtt.getCicilan().isEmpty()) {
											for (String idPemBul : StringUtils.split(virtualAccountBankNtt.getCicilan(),
													",")) {
												if (Common.isNumber(idPemBul)) {
													PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) session
															.createCriteria(PengaturanPembayaranBulanan.class)
															.add(Restrictions.idEq(Long.parseLong(idPemBul)))
															.uniqueResult();

													if (pengaturanPembayaranBulanan != null) {
														String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
																+ virtualAccountBankNtt.getId();

														ItemBiaya itemBiaya = pengaturanPembayaranBulanan
																.getDetailBiaya().getItemBiaya();

														Double subtotal = 0.0;
														try {
															String[] spl = idPemBul.split("-");
															subtotal = Double.parseDouble(spl[spl.length - 1]);
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Otto.java:474");
														}

														if (itemBiaya.getPenghitungan()
																.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
															subtotal = 0.0 - subtotal;
														}

														if (inquery) {
															JSONObject bill = new JSONObject();

															bill.put("billID", itemBiaya.getKode());
															bill.put("billName", itemBiaya.getNama());
															bill.put("billNameID", itemBiaya.getId() + "");

															bill.put("billAmount", subtotal.intValue());

															bill.put("billAmountBayar", 0);
															bill.put("billAmountPay", 0);
															bill.put("TahunID", TahunID);

															billDetails.put(bill);

														} else {

															CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
																	.createCriteria(CicilanPembayaran.class)
																	.add(Restrictions.eq("ref", ref)).setMaxResults(1)
																	.uniqueResult();

															if (cicilanPembayaran == null) {
																cicilanPembayaran = new CicilanPembayaran(
																		pengaturanPembayaranBulanan.getDetailBiaya());
															}
															cicilanPembayaran.setRef(ref);
															cicilanPembayaran.setValidator(bank);
															cicilanPembayaran.setKegiatan(kegiatan);
															cicilanPembayaran.setItemBiaya(pengaturanPembayaranBulanan
																	.getDetailBiaya().getItemBiaya());
															cicilanPembayaran.setPengaturanPembayaranBulanan(
																	pengaturanPembayaranBulanan);
															cicilanPembayaran.setRefVa(virtualAccountBankNtt.getId());
															cicilanPembayaran.setNilai(subtotal);
															cicilanPembayaran
																	.setNilaiAsli(cicilanPembayaran.getNilai());
															cicilanPembayaran.setTanggal(tanggal);
															cicilanPembayaran.setJenisPembayaran(bankHost == null
																	|| bankHost.getJenisPembayaran() == null
																			? ConstantValues.TUNAI
																			: bankHost.getJenisPembayaran());
															cicilanPembayaran.setDenda(0.0);
															cicilanPembayaran
																	.setNilaiAsli(cicilanPembayaran.getNilai());

															session.getTransaction().begin();
															if(cicilanPembayaran.getId()==null)session.save(cicilanPembayaran);else Common.refreshUpdate(session, cicilanPembayaran);
															session.getTransaction().commit();

															JSONObject jsonObjectRinci = new JSONObject();
															jsonObjectRinci.put("nama", pengaturanPembayaranBulanan
																	.getDetailBiaya().getItemBiaya().getNama());
															jsonObjectRinci.put("bulan",
																	pengaturanPembayaranBulanan.getNamaBulan());
															jsonObjectRinci.put("nominal", pengaturanPembayaranBulanan
																	.ambilNominalModifikasi(mahasiswa, semester));

															rincian.put(jsonObjectRinci);

														}
													}
												} else if (idPemBul != null && idPemBul.startsWith("Bulanan-")) {
													PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) session
															.createCriteria(PengaturanPembayaranBulanan.class)
															.add(Restrictions
																	.idEq(Long.parseLong(idPemBul.split("-")[1])))
															.uniqueResult();

													if (pengaturanPembayaranBulanan != null) {
														String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
																+ virtualAccountBankNtt.getId();

														ItemBiaya itemBiaya = pengaturanPembayaranBulanan
																.getDetailBiaya().getItemBiaya();
														Double subtotal = 0.0;
														try {
															String[] spl = idPemBul.split("-");
															subtotal = Double.parseDouble(spl[spl.length - 1]);
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Otto.java:561");
														}

														if (itemBiaya.getPenghitungan()
																.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
															subtotal = 0.0 - subtotal;
														}

														if (inquery) {
															JSONObject bill = new JSONObject();

															bill.put("billID", itemBiaya.getKode());
															bill.put("billName", itemBiaya.getNama());
															bill.put("billNameID", itemBiaya.getId() + "");

															bill.put("billAmount", subtotal.intValue());

															bill.put("billAmountBayar", 0);
															bill.put("billAmountPay", 0);
															bill.put("TahunID", TahunID);

															billDetails.put(bill);

														} else {
															CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
																	.createCriteria(CicilanPembayaran.class)
																	.add(Restrictions.eq("ref", ref)).setMaxResults(1)
																	.uniqueResult();

															if (cicilanPembayaran == null) {
																cicilanPembayaran = new CicilanPembayaran(
																		pengaturanPembayaranBulanan.getDetailBiaya());
															}
															cicilanPembayaran.setRef(ref);
															cicilanPembayaran.setValidator(bank);
															cicilanPembayaran.setKegiatan(kegiatan);
															cicilanPembayaran.setItemBiaya(itemBiaya);
															cicilanPembayaran.setPengaturanPembayaranBulanan(
																	pengaturanPembayaranBulanan);
															cicilanPembayaran.setRefVa(virtualAccountBankNtt.getId());

															cicilanPembayaran.setNilai(subtotal);
															cicilanPembayaran
																	.setNilaiAsli(cicilanPembayaran.getNilai());
															cicilanPembayaran.setTanggal(tanggal);
															cicilanPembayaran.setJenisPembayaran(bankHost == null
																	|| bankHost.getJenisPembayaran() == null
																			? ConstantValues.TUNAI
																			: bankHost.getJenisPembayaran());
															cicilanPembayaran.setDenda(0.0);
															cicilanPembayaran
																	.setNilaiAsli(cicilanPembayaran.getNilai());
															session.getTransaction().begin();
															if(cicilanPembayaran.getId()==null)session.save(cicilanPembayaran);else Common.refreshUpdate(session, cicilanPembayaran);
															session.getTransaction().commit();

															JSONObject jsonObjectRinci = new JSONObject();
															jsonObjectRinci.put("nama", pengaturanPembayaranBulanan
																	.getDetailBiaya().getItemBiaya().getNama());
															jsonObjectRinci.put("bulan",
																	pengaturanPembayaranBulanan.getNamaBulan());
															jsonObjectRinci.put("nominal", subtotal);

															rincian.put(jsonObjectRinci);
														}
													}

												} else if (idPemBul != null && idPemBul.startsWith("Item-")) {
													ItemBiaya itemBiaya = (ItemBiaya) ConstantValues
															.simpleObject(
																	session.createCriteria(ItemBiaya.class)
																			.add(Restrictions.idEq(Long.parseLong(
																					idPemBul.split("-")[1]))),
																	ItemBiaya.class);

													if (itemBiaya != null) {
														String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
																+ virtualAccountBankNtt.getId();
														Double subtotal = 0.0;
														try {
															String[] spl = idPemBul.split("-");
															subtotal = Double.parseDouble(spl[2]);
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Otto.java:643");
														}

														Long detailBiayaId = null;
														try {
															String[] spl = idPemBul.split("-");
															detailBiayaId = Long.parseLong(spl[4]);
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Otto.java:650");
														}

														if (itemBiaya.getPenghitungan()
																.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
															subtotal = 0.0 - subtotal;
														}

														if (inquery) {
															JSONObject bill = new JSONObject();

															bill.put("billID", itemBiaya.getKode());
															bill.put("billName", itemBiaya.getNama());
															bill.put("billNameID", itemBiaya.getId() + "");

															bill.put("billAmount", subtotal.intValue());

															bill.put("billAmountBayar", 0);
															bill.put("billAmountPay", 0);
															bill.put("TahunID", TahunID);

														} else {

															CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
																	.createCriteria(CicilanPembayaran.class)
																	.add(Restrictions.eq("ref", ref)).setMaxResults(1)
																	.uniqueResult();

															if (cicilanPembayaran == null) {
																cicilanPembayaran = new CicilanPembayaran(
																		DetailBiaya.muatRefAman(session, detailBiayaId));
															}
															cicilanPembayaran.setDetailBiaya(DetailBiaya.muatRefAman(session, detailBiayaId));
															cicilanPembayaran.setRef(ref);
															cicilanPembayaran.setValidator(bank);
															cicilanPembayaran.setKegiatan(kegiatan);
															cicilanPembayaran.setItemBiaya(itemBiaya);
															cicilanPembayaran.setPengaturanPembayaranBulanan(null);
															cicilanPembayaran.setRefVa(virtualAccountBankNtt.getId());

															cicilanPembayaran.setNilai(subtotal);
															cicilanPembayaran
																	.setNilaiAsli(cicilanPembayaran.getNilai());
															cicilanPembayaran.setTanggal(tanggal);
															cicilanPembayaran.setJenisPembayaran(bankHost == null
																	|| bankHost.getJenisPembayaran() == null
																			? ConstantValues.TUNAI
																			: bankHost.getJenisPembayaran());
															cicilanPembayaran.setDenda(0.0);
															cicilanPembayaran
																	.setNilaiAsli(cicilanPembayaran.getNilai());
															session.getTransaction().begin();
															if(cicilanPembayaran.getId()==null)session.save(cicilanPembayaran);else Common.refreshUpdate(session, cicilanPembayaran);
															session.getTransaction().commit();

															JSONObject jsonObjectRinci = new JSONObject();
															jsonObjectRinci.put("nama", itemBiaya.getNama());
															jsonObjectRinci.put("bulan", "");
															jsonObjectRinci.put("nominal", subtotal);

															rincian.put(jsonObjectRinci);
														}

													}
												} else if (idPemBul.startsWith("Keranjang-")) {
													// Pembayaran Keranjang Belanja (multi jenis / KegiatanTemporary): konversi draf
													// menjadi Kegiatan+Cicilan nyata â€” pemroses terpusat yang sama dengan Esmartlink.
													ais.action.ws.util.PembayaranGatewayHelper.prosesSatuTokenKeranjang(session, idPemBul,
															virtualAccountBankNtt, inquery, bank, bankHost, tanggal, data, null);
												}
											}
										}

										response.put("numBill", billDetails.length());

										if (reversal) {
											response = new JSONObject();
											response.put("errorCode", "00");
											response.put("statusDescription", "Success");
										}

										else if (inquery) {

										} else {

											response.put("UserLogin", "");
											response.put("Password", "");

											Double[] d = PembayaranUtil.getInstance()
													.getTotalDanDendaFromCicilan(session, kegiatan);
											Double jumlah = d[0];
											Double denda = d[1];
											kegiatan.setDenda(denda.doubleValue());
											kegiatan.setAmountTerhutang(nilaiBiayaHarusDiBayars
													- (jumlah.doubleValue() - denda.doubleValue()));

											kegiatan.setAmount(jumlah.doubleValue() > 0.1 ? jumlah.doubleValue()
													: virtualAccountBankNtt.getTotal());
											kegiatan.setValidator(bank);

											session.getTransaction().begin();
											Common.refreshUpdate(session, kegiatan);
											session.getTransaction().commit();

											VirtualAccountBank.updateVa(virtualAccountBankNtt, tanggal, kegiatan, data,
													bank);

										}

									}
								}
							}
						} finally {
							if (session != null) {
								try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Otto.java:764");}
								try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Otto.java:765");}
								try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Otto.java:766");}
							}
						}
					}
				}
			} catch (Exception e) {
				HibernateUtil.rollbackTransaction();

				if (reversal) {
					response = new JSONObject();
					response.put("errorCode", "05");
					response.put("statusDescription", "Reversal gagal");
				} else {

					response.put("errorCode", "91");
					response.put("statusDescription", "Link Down");
				}
				h2hStackTrace = ais.action.ws.util.PembayaranGatewayHelper.ambilStackTrace(e);
				Common.tampilErrorJikaAdmin(e);
			} finally {
				// JAMINAN: log H2H SELALU dicatat & tercommit walau terjadi error/exception
				// (helper: session terdedikasi + commit + retry, tak pernah gagal).
				ais.action.ws.util.PembayaranGatewayHelper.catatLogHostToHost(request, bankHost, data, nim, va, nama,
						response.toString(), nominalP, rincian.toString(), h2hStackTrace);
			}
		}
		return response;
	}

	/**
	 * Mengurai payload permintaan penyedia menjadi parameter terpisah, memanggil
	 * {@link #doProcess}, dan menentukan mode inquiry maupun reversal.
	 *
	 * <p>Penanda transaksi diambil dari field {@code trxRef} dan nominal dari {@code amount},
	 * masing-masing dengan cadangan berupa parameter query bernama sama. <b>Ketiadaan</b>
	 * {@code amount} itu sendiri yang menandai permintaan sebagai inquiry — penyedia
	 * mengirimkannya hanya saat benar-benar menyetor. Mode reversal ditentukan dari URI yang
	 * berakhiran {@code "OttoReversal"}. Waktu transaksi tidak dikirim penyedia, sehingga
	 * {@code tanggalP} sengaja diteruskan sebagai {@code null}.</p>
	 *
	 * <p>Bila {@link #doProcess} melempar exception, balasan diganti {@link #errorDb} sehingga
	 * penyedia menerima {@code errorCode=91} "Link Down" dan tidak menganggap tagihan lunas.</p>
	 *
	 * <p><b>Perbedaan dengan {@link BMS#doProses}.</b> Dua pengaman yang sudah ada di {@link BMS}
	 * belum tersalin ke sini:</p>
	 * <ul>
	 *   <li>tidak ada pemeriksaan bentuk payload di awal ({@link BMS} menolak badan kosong atau
	 *       yang tidak diawali <code>{</code> dengan {@code errorCode=30}), dan {@code req} yang
	 *       bernilai {@code null} langsung dipakai memanggil {@code req.isNull(...)};</li>
	 *   <li>simulasi timeout di bawah masih dijalankan <b>setelah</b> {@link #doProcess} selesai
	 *       dan pembayaran tercatat, serta cukup diaktifkan lewat konfigurasi
	 *       {@code otto_va_sleep} saja — sedangkan {@link BMS} kini mengembalikan balasan timeout
	 *       <b>sebelum</b> pembayaran dicatat dan mensyaratkan JVM flag
	 *       {@code ais.bms.allowTimeoutSimulation}.</li>
	 * </ul>
	 * <p>Kedua hal itu dicatat apa adanya; perbaikannya di luar cakupan berkas ini.</p>
	 *
	 * @param data     badan permintaan mentah
	 * @param request  permintaan asli, sumber cadangan parameter dan penentu mode reversal
	 * @param bankHost host hasil pencocokan IP; boleh {@code null}
	 * @param bank     label bank yang akan tercatat sebagai {@code validator}
	 * @param chek     mode pemeriksaan internal, diteruskan ke {@link #doProcess}
	 * @return teks JSON balasan
	 * @throws Exception bila payload bukan JSON yang sah, atau {@link Thread#sleep} pada simulasi
	 *                   timeout terinterupsi
	 */
	public static String doProses(String data, HttpServletRequest request, BankHost bankHost, String bank, boolean chek)
			throws Exception {
		JSONObject req = data == null ? null : new JSONObject(data);

		String va = req == null || req.isNull("trxRef") ? (request == null ? "" : request.getParameter("trxRef"))
				: req.getString("trxRef");

		double nominalP = 0.0;
		try {
			nominalP = req == null || req.isNull("amount") ? 0.0 : Double.parseDouble(req.get("amount") + "");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Otto.java:805");
			// TODO: handle exception
		}

		boolean reversal = false;
		if (request != null && request.getRequestURI().endsWith("OttoReversal")) {
			reversal = true;
		}

		String tanggalP = null;

		String body;
		try {
			// 05, request tidak diizinkan
			body = Otto.doProcess(nominalP, tanggalP, va, bank, bankHost, request, data, true, req.isNull("amount"),
					reversal, chek).toString();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Otto.java:822");
			body = errorDb(req.isNull("amount"), data);

		}

		if (Common.bolehKonfigurasi("otto_va_sleep", Konfigurasi.TIDAK_AKTIF)) {
			Thread.sleep(3 * 1000);
			body = timeoutDb(req.isNull("amount"), data);
		}

		System.out.println("response->" + body);

		return body;
	}

	/**
	 * Membaca badan permintaan, menetapkan {@link BankHost} dari alamat IP pemanggil, memanggil
	 * {@link #doProses}, lalu menuliskan hasilnya sebagai JSON.
	 *
	 * <p>Badan permintaan dibaca baris demi baris lalu digabung <b>tanpa pemisah</b>, sehingga
	 * JSON yang terpecah beberapa baris tetap menjadi satu dokumen utuh.</p>
	 *
	 * <p>Identifikasi pemanggil memakai {@link PembayaranUtil#getBankHost(String, String)} dengan
	 * {@link HttpServletRequest#getRemoteAddr()} — alamat TCP sebenarnya, bukan header yang dapat
	 * dipalsukan klien. Hasilnya hanya dilekatkan pada log dan pencarian VA; ia <b>tidak</b>
	 * dipakai untuk menolak permintaan.</p>
	 *
	 * <p>Label bank yang ditetapkan di sini adalah {@code "BMS"} — penanda warisan salinan
	 * {@link BMS} yang belum diganti menjadi Otto, padahal nilainya tersimpan sebagai
	 * {@code validator} pada Kegiatan dan CicilanPembayaran (lihat javadoc
	 * {@link Otto kelas ini}).</p>
	 *
	 * <p>Berbeda dari {@link BMS#process} dan {@link Nagari#process}, tidak ada penanganan
	 * exception tambahan di sini: kegagalan {@link #doProses} merambat ke {@link #doPost} yang
	 * menelannya, sehingga penyedia menerima balasan kosong alih-alih JSON bertanda gagal.</p>
	 *
	 * @param request  permintaan dari sisi penyedia pembayaran
	 * @param response tujuan penulisan balasan JSON
	 * @throws Exception bila pembacaan badan permintaan, pemrosesan, atau penulisan balasan gagal
	 */
	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {

		// Read from request

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

		String bank = "BMS";
		BankHost bankHost = pembayaranUtil.getBankHost(request.getRemoteAddr(), bank);

		String body = doProses(data, request, bankHost, bank, false);

		response.setHeader("length", body.length() + "");
		response.setHeader("Content-Type", "application/json");
		PrintWriter writer = response.getWriter();

		writer.write(body);

	}

	/**
	 * Menyusun balasan bertanda <i>timeout</i> ({@code errorCode=68}, "Connection Timeout") untuk
	 * keperluan simulasi pengujian.
	 *
	 * <p>Balasan dibangun dari payload permintaan itu sendiri, sehingga seluruh field yang dikirim
	 * penyedia tetap ada dan hanya status yang diganti.</p>
	 *
	 * <p><b>Peringatan.</b> Pada berkas ini pemanggilnya di {@link #doProses} berjalan
	 * <b>setelah</b> {@link #doProcess} selesai mencatat pembayaran, sehingga balasan timeout ini
	 * dapat terkirim untuk transaksi yang sebenarnya sudah tersimpan lunas. {@link BMS} sudah
	 * mengubah urutan ini; lihat javadoc {@link #doProses}.</p>
	 *
	 * @param inquery penanda mode inquiry; saat ini tidak memengaruhi hasil
	 * @param data    badan permintaan mentah yang dipakai sebagai kerangka balasan
	 * @return teks JSON balasan timeout
	 * @throws Exception bila {@code data} bukan JSON yang sah
	 */
	private static String timeoutDb(boolean inquery, String data) throws Exception {

		JSONObject jsonObjectResponse = new JSONObject(data);
		jsonObjectResponse.put("errorCode", "68");
		jsonObjectResponse.put("statusDescription", "Connection Timeout");
		return jsonObjectResponse.toString();
	}

	/**
	 * Menyusun balasan bertanda gagal ({@code errorCode=91}, "Link Down"), dipakai saat
	 * {@link #doProcess} melempar exception.
	 *
	 * <p>Seperti {@link #timeoutDb}, balasan dibangun dari payload permintaan sehingga bentuknya
	 * tetap dikenali pengurai di sisi penyedia. Kode 91 menandakan gangguan sementara, sehingga
	 * penyedia memperlakukan tagihan sebagai <b>belum</b> lunas dan dapat mengulang permintaan.</p>
	 *
	 * @param inquery penanda mode inquiry; saat ini tidak memengaruhi hasil
	 * @param data    badan permintaan mentah yang dipakai sebagai kerangka balasan
	 * @return teks JSON balasan gagal
	 * @throws Exception bila {@code data} bukan JSON yang sah
	 */
	private static String errorDb(boolean inquery, String data) throws Exception {

		JSONObject jsonObjectResponse = new JSONObject(data);
		jsonObjectResponse.put("errorCode", "91");
		jsonObjectResponse.put("statusDescription", "Link Down");
		return jsonObjectResponse.toString();
	}
}

