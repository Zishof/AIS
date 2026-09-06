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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * Servlet gateway <i>host-to-host</i> (H2H) untuk skema Virtual Account bank mitra: melayani
 * <b>inquiry</b> (tanya tagihan), <b>payment</b> (pelunasan), dan <b>reversal</b> (pembatalan
 * pembayaran) atas baris {@link VirtualAccountBank}.
 *
 * <p>Terpasang di {@code web.xml} pada dua URL yang keduanya menunjuk kelas ini: {@code /BMS}
 * untuk inquiry dan pembayaran, serta {@code /BMSReversal} untuk pembatalan. Pembedaannya
 * dilakukan {@link #doProses} dengan memeriksa akhiran URI, bukan lewat parameter.</p>
 *
 * <h4>Berkas acuan bagi salinan-salinannya</h4>
 * <p>Kelas ini adalah <b>bentuk paling mutakhir</b> dari sebuah pola gateway yang juga dipakai
 * {@link Nagari} dan {@link Otto}. Beberapa pengaman sudah ditambahkan di sini dan
 * <b>belum tersalin</b> ke kedua berkas itu:</p>
 * <ul>
 *   <li><b>pemeriksaan bentuk payload</b> di awal {@link #doProses} — badan kosong atau yang tidak
 *       diawali <code>{</code> ditolak dengan {@code errorCode=30}, dan setiap pembacaan field
 *       memakai pola {@code req == null || req.isNull(...)} yang aman terhadap {@code null};</li>
 *   <li><b>urutan aman simulasi timeout</b> — balasan {@code errorCode=68} dikembalikan
 *       <i>sebelum</i> {@link #doProcess} dipanggil, sehingga tidak mungkin lagi ada pembayaran
 *       yang telanjur tercatat lunas padahal bank diberi tahu transaksinya gagal; pengaktifannya
 *       juga menuntut JVM flag {@code ais.bms.allowTimeoutSimulation} di samping konfigurasi
 *       {@code bms_va_sleep};</li>
 *   <li><b>{@link #POLA_ITEM_PEMBAYARAN}</b> — validasi ketat format komponen {@code "Item-"}
 *       sebelum diurai.</li>
 * </ul>
 *
 * <h4>Model autentikasi (tidak ada tanda tangan digital)</h4>
 * <p>Kanal ini <b>tidak memverifikasi tanda tangan, HMAC, token, maupun kata sandi</b> — baik pada
 * cabang inquiry, pembayaran, maupun reversal. Satu-satunya pengenal pemanggil adalah pencocokan
 * alamat IP lewat {@link PembayaranUtil#getBankHost(String, String)} di {@link #process}, dan
 * hasilnya <b>tidak dipakai sebagai gerbang</b>: bila IP tidak dikenal, {@code bankHost} bernilai
 * {@code null} namun pemrosesan tetap berlanjut. Karena
 * {@link VirtualAccountBank#ambilVa(String, Double, BankHost)} mencocokkan baris dengan syarat
 * <i>{@code bankHost} kosong ATAU sama dengan pemanggil</i>, VA "netral" tetap ditemukan dalam
 * keadaan itu. Bandingkan dengan {@link Bsiresponse} yang payload-nya harus dibuka memakai kunci
 * rahasia bersama sehingga verifikasi kepemilikan kunci benar-benar terjadi pada jalur transaksi.
 * Sifat ini didokumentasikan apa adanya; penanganannya di luar cakupan berkas ini.</p>
 * <p>Sisi baiknya, IP dibaca dari {@link HttpServletRequest#getRemoteAddr()} — alamat TCP
 * sebenarnya — <b>bukan</b> dari header {@code X-Forwarded-For}/{@code X-Real-IP} yang dapat
 * dipalsukan klien.</p>
 *
 * @see Nagari
 * @see Otto
 * @see VirtualAccountBank
 */
public class BMS extends HttpServlet {
	/** Versi serialisasi bawaan {@link HttpServlet}; tidak dipakai untuk logika apa pun. */
	private static final long serialVersionUID = 1L;

	/**
	 * Singleton utilitas pembayaran; dipakai untuk memetakan alamat IP pemanggil menjadi
	 * {@link BankHost} dan untuk menghitung total serta denda dari cicilan.
	 */
	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();
	/**
	 * Pola validasi komponen pembayaran berawalan {@code "Item-"} pada kolom {@code cicilan} milik
	 * {@link VirtualAccountBank}, berbentuk
	 * {@code Item-<idItemBiaya>-<nominal>-<opsional>-<idDetailBiaya>}.
	 *
	 * <p>Keempat gugus tangkapnya berturut-turut adalah id {@link ItemBiaya}, nominal (boleh
	 * negatif dan boleh berdesimal), sebuah gugus opsional yang boleh kosong, dan id
	 * {@link DetailBiaya}.</p>
	 *
	 * <p>Pola ini menggantikan pemecahan {@code split("-")} yang dipakai gateway sejenis: bila
	 * format tidak lengkap, komponen tersebut <b>dilewati</b> dan dicatat ke
	 * {@code ErrorAuditUtil}, alih-alih terurai sebagian menjadi nominal atau id yang keliru.
	 * Karena bersifat statis dan {@code final}, biaya kompilasi pola hanya dibayar sekali;
	 * {@link Pattern} sendiri aman dipakai bersama antar-thread.</p>
	 */
	private static final Pattern POLA_ITEM_PEMBAYARAN = Pattern
			.compile("^Item-([0-9]+)-(-?[0-9]+(?:\\.[0-9]+)?)-([0-9]*)-([0-9]+)$");

	/**
	 * Pemformat tanggal {@code yyyyMMddHHmmss} untuk field {@code trxDateTime} yang dikirim bank.
	 *
	 * <p>Dibungkus {@link ThreadLocal} karena {@link SimpleDateFormat} <b>tidak aman untuk dipakai
	 * bersama antar-thread</b>, sedangkan servlet dilayani banyak thread sekaligus; satu instance
	 * bersama akan menghasilkan tanggal kacau atau exception acak di bawah beban.</p>
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
	public BMS() {
		super();
	}

	/**
	 * Menangani permintaan HTTP GET dengan meneruskannya ke {@link #process}; perlakuannya sama
	 * dengan {@link #doPost}.
	 *
	 * <p>Exception apa pun ditelan di sini dan hanya diteruskan ke
	 * {@code Common.tampilErrorJikaAdmin}, sehingga kegagalan tak terduga tidak menjadi HTTP 500
	 * melainkan balasan kosong bertatus 200. Jalur kegagalan yang normal sudah ditangani lebih
	 * dulu di {@link #doProses} yang menjawab dengan {@link #errorDb}.</p>
	 *
	 * @param request  permintaan dari sisi bank
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
	 * Menangani permintaan HTTP POST — jalur yang dipakai bank — dengan meneruskannya ke
	 * {@link #process}.
	 *
	 * @param request  permintaan dari sisi bank, badan JSON-nya dibaca di {@link #process}
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
	 *       {@code billDetails}; tidak ada penyimpanan, sehingga pengecekan tagihan oleh bank
	 *       tidak pernah tercatat sebagai pembayaran;</li>
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
	 *       (dibandingkan sebagai {@code int}) dengan {@code getTotal() + getBiayaAdmin()}; bila
	 *       tidak, {@code errorCode=04} "Nominal Tagihan tidak sesuai";</li>
	 *   <li>kegagalan tak terduga &rarr; {@code errorCode=91} "Link Down", atau
	 *       {@code errorCode=05} bila sedang reversal.</li>
	 * </ol>
	 *
	 * <h4>Dua cabang pembayar</h4>
	 * <p>VA milik siswa/calon siswa diproses {@link VirtualAccountBank#bayarSiswa}; VA milik
	 * mahasiswa/calon mahasiswa diproses di tempat dengan membuat atau memperbarui
	 * {@link Kegiatan} dan baris {@link CicilanPembayaran} untuk tiap komponen pada kolom
	 * {@code cicilan}. Komponen dikenali dari awalannya: angka murni dan {@code "Bulanan-"}
	 * merujuk {@link PengaturanPembayaranBulanan}, {@code "Item-"} merujuk {@link ItemBiaya} dan
	 * divalidasi lebih dulu oleh {@link #POLA_ITEM_PEMBAYARAN}, sedangkan {@code "Keranjang-"}
	 * didelegasikan ke {@code PembayaranGatewayHelper.prosesSatuTokenKeranjang}.</p>
	 *
	 * <p>Nama dan identitas pembayar yang dikirim balik disaring
	 * {@code replaceAll("[^a-zA-Z0-9]", "")} agar hanya berisi huruf dan angka, mengikuti batasan
	 * format di sisi bank.</p>
	 *
	 * <p>Blok {@code finally} <b>selalu</b> memanggil
	 * {@code PembayaranGatewayHelper.catatLogHostToHost} tanpa syarat {@code bankHost}, sehingga
	 * permintaan dari IP tak dikenal pun tetap meninggalkan jejak audit. Ini keputusan arsitektur
	 * yang disengaja, bukan kelalaian pemeriksaan.</p>
	 *
	 * @param nominalP nominal setoran dari bank; diabaikan saat inquiry
	 * @param tanggalP waktu transaksi berformat {@code yyyyMMddHHmmss}; bila gagal diurai, dipakai
	 *                 waktu server saat ini
	 * @param va       nomor Virtual Account (field {@code nim} pada payload)
	 * @param bank     label bank yang disimpan sebagai {@code validator} pada Kegiatan dan
	 *                 CicilanPembayaran
	 * @param bankHost host bank hasil pencocokan IP; boleh {@code null} dan tidak menjadi gerbang
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
			Session session = null;
			try {

				virtualAccountBankNtt = VirtualAccountBank.ambilVa(va, nominalP,  bankHost);

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
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/BMS.java:155");
							// TODO: handle exception
						}

						session = HibernateUtil.getSessionFactory().openSession();
						{

							Double nominal = nominalP;

							if (!inquery && nominal.intValue() != (virtualAccountBankNtt.getBiayaAdmin().intValue()
									+ virtualAccountBankNtt.getTotal().intValue())) {
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
									response.put("paymentAmount", virtualAccountBankNtt.getTotal().intValue()
											+ virtualAccountBankNtt.getBiayaAdmin().intValue());
								}

								Date tanggal = ais.ui.util.WaktuUtil.getDate();
								try {
									tanggal = dateFormat.get().parse(tanggalP);
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/BMS.java:198");
								}

								if (virtualAccountBankNtt.getSiswa() != null
										|| virtualAccountBankNtt.getCalonSiswa() != null) {

									Sekolah sekolah = null;
									if (virtualAccountBankNtt.getSiswa() != null) {
										sekolah = virtualAccountBankNtt.getSiswa().getSekolah();
										nim = virtualAccountBankNtt.getSiswa().getNomorInduk()
												.replaceAll("[^a-zA-Z0-9]", "");
										nama = virtualAccountBankNtt.getSiswa().getNama().replaceAll("[^a-zA-Z0-9]",
												"");

									} else if (virtualAccountBankNtt.getCalonSiswa() != null) {
										sekolah = virtualAccountBankNtt.getCalonSiswa().getSekolah();
										nim = virtualAccountBankNtt.getCalonSiswa().getNomorInduk()
												.replaceAll("[^a-zA-Z0-9]", "");
										nama = virtualAccountBankNtt.getCalonSiswa().getNama()
												.replaceAll("[^a-zA-Z0-9]", "");

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

									response.put("Nama", nama.replaceAll("[^a-zA-Z0-9]", ""));
									response.put("FormID", nim.replaceAll("[^a-zA-Z0-9]", ""));
									response.put("Gelombang",
											biodataCalonMahasiswa == null
													|| biodataCalonMahasiswa.getGelombangPendaftaran() == null ? ""
															: biodataCalonMahasiswa.getGelombangPendaftaran().getNama()
																	.replaceAll("[^a-zA-Z0-9]", ""));
									response.put("ProdiNama",
											jurusan == null ? "" : jurusan.getNama().replaceAll("[^a-zA-Z0-9]", ""));
									response.put("FakultasNama", jurusan == null ? ""
											: jurusan.getFakultas().getNama().replaceAll("[^a-zA-Z0-9]", ""));

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
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/BMS.java:352");

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
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/BMS.java:430");

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
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/BMS.java:475");
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
															if (cicilanPembayaran.getId() == null)
																session.save(cicilanPembayaran);
															else
																Common.refreshUpdate(session, cicilanPembayaran);
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
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/BMS.java:565");
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
															if (cicilanPembayaran.getId() == null)
																session.save(cicilanPembayaran);
															else
																Common.refreshUpdate(session, cicilanPembayaran);
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
													Matcher dataItem = POLA_ITEM_PEMBAYARAN.matcher(idPemBul);
													if (!dataItem.matches()) {
														ais.common.ErrorAuditUtil.record(new IllegalArgumentException(
																"Format item pembayaran bank NTT tidak lengkap: " + idPemBul),
																"BMS.doProcess:skip-item-format-tidak-valid");
														continue;
													}
													ItemBiaya itemBiaya = (ItemBiaya) ConstantValues
															.simpleObject(
																	session.createCriteria(ItemBiaya.class)
																					.add(Restrictions.idEq(Long.parseLong(
																							dataItem.group(1)))),
																	ItemBiaya.class);

													if (itemBiaya != null) {
														String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
																+ virtualAccountBankNtt.getId();
														Double subtotal = 0.0;
														try {
																	subtotal = Double.parseDouble(dataItem.group(2));
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/BMS.java:650");
														}

														Long detailBiayaId = null;
														try {
																	detailBiayaId = Long.parseLong(dataItem.group(4));
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/BMS.java:657");
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
																// KE-1: jangan referensikan DetailBiaya transient (new DetailBiaya(id))
																// karena id hasil parse idPemBul bisa tidak ada di tabel detail_biaya
																// -> insert melanggar FK constraint. Muat dari DB null-safe (helper bersama).
																cicilanPembayaran = new CicilanPembayaran(
																		DetailBiaya.muatRefAman(session, detailBiayaId));
															}
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
															if (cicilanPembayaran.getId() == null)
																session.save(cicilanPembayaran);
															else
																Common.refreshUpdate(session, cicilanPembayaran);
															session.getTransaction().commit();

															JSONObject jsonObjectRinci = new JSONObject();
															jsonObjectRinci.put("nama", itemBiaya.getNama());
															jsonObjectRinci.put("bulan", "");
															jsonObjectRinci.put("nominal", subtotal);

															rincian.put(jsonObjectRinci);
														}

													}
												} else if (idPemBul != null && idPemBul.startsWith("Keranjang-")) {
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
				if (session != null) {
					try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/BMS.java:795");}
					try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/BMS.java:796");}
					try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/BMS.java:797");}
				}
				ais.action.ws.util.PembayaranGatewayHelper.catatLogHostToHost(request, bankHost, data, nim, va, nama,
						response.toString(), nominalP, rincian.toString(), h2hStackTrace);
			}
		}
		return response;
	}

	/**
	 * Mengurai payload permintaan bank menjadi parameter terpisah, menentukan mode inquiry maupun
	 * reversal, lalu memanggil {@link #doProcess}.
	 *
	 * <p>Diawali <b>pemeriksaan bentuk payload</b>: badan yang kosong atau tidak diawali
	 * <code>{</code> langsung ditolak dengan {@code errorCode=30} "Format request tidak valid",
	 * sebelum {@code new JSONObject(data)} sempat melempar exception. Setiap pembacaan field
	 * selanjutnya memakai pola {@code req == null || req.isNull(...)} sehingga aman terhadap
	 * {@code null}.</p>
	 *
	 * <p>Nomor VA diambil dari field {@code nim} dan nominal dari {@code paymentAmount}, masing-
	 * masing dengan cadangan berupa parameter query bernama sama. <b>Ketiadaan</b>
	 * {@code paymentAmount} itu sendiri yang menandai permintaan sebagai inquiry — bank
	 * mengirimkannya hanya saat benar-benar menyetor. Mode reversal ditentukan dari URI yang
	 * berakhiran {@code "BMSReversal"}, yaitu url-pattern kedua yang menunjuk kelas ini.</p>
	 *
	 * <h4>Simulasi timeout dan alasan urutannya</h4>
	 * <p>Simulasi timeout menuntut <b>dua</b> syarat sekaligus: JVM flag
	 * {@code -Dais.bms.allowTimeoutSimulation=true} dan konfigurasi {@code bms_va_sleep}. Gerbang
	 * ganda ini mencegah konfigurasi basis data yang tidak sengaja aktif di produksi menjalankan
	 * simulasi dengan sendirinya.</p>
	 * <p>Yang lebih penting, balasan {@code errorCode=68} dikembalikan <b>sebelum</b>
	 * {@link #doProcess} dipanggil. Implementasi lama mencatat pembayaran lebih dulu baru
	 * mengirim kode 68; bank lalu me-refund transaksi sementara eCampus telanjur mencatatnya
	 * lunas. Dengan urutan sekarang, jalur simulasi tidak pernah menyentuh pembukuan, dan
	 * kejadiannya tetap dicatat ke log H2H.</p>
	 *
	 * <p>Bila {@link #doProcess} melempar exception, balasan diganti {@link #errorDb} sehingga
	 * bank menerima {@code errorCode=91} "Link Down" dan tidak menganggap tagihan lunas.</p>
	 *
	 * @param data     badan permintaan mentah
	 * @param request  permintaan asli, sumber cadangan parameter dan penentu mode reversal
	 * @param bankHost host bank hasil pencocokan IP; boleh {@code null}
	 * @param bank     label bank yang akan tercatat sebagai {@code validator}
	 * @param chek     mode pemeriksaan internal; nilai {@code true} juga mematikan simulasi
	 *                 timeout
	 * @return teks JSON balasan
	 * @throws Exception bila payload bukan JSON yang sah, atau {@link Thread#sleep} pada simulasi
	 *                   timeout terinterupsi
	 */
	public static String doProses(String data, HttpServletRequest request, BankHost bankHost, String bank, boolean chek)
			throws Exception {
		if (data == null || data.trim().length() == 0 || !data.trim().startsWith("{")) {
			JSONObject response = new JSONObject();
			response.put("errorCode", "30");
			response.put("statusDescription", "Format request tidak valid");
			return response.toString();
		}
		JSONObject req = new JSONObject(data);

		String va = req == null || req.isNull("nim") ? (request == null ? "" : request.getParameter("nim"))
				: req.getString("nim");

		double nominalP = 0.0;
		try {
			nominalP = req == null || req.isNull("paymentAmount") ? 0.0
					: Double.parseDouble(req.get("paymentAmount") + "");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/BMS.java:817");
			// TODO: handle exception
		}

		boolean reversal = false;
		if (request != null && request.getRequestURI().endsWith("BMSReversal")) {
			reversal = true;
		}

		String tanggalP = req == null || req.isNull("trxDateTime") ? dateFormat.get().format(new Date())
				: req.getString("trxDateTime");

		/*
		 * Simulasi timeout tidak boleh berjalan hanya karena konfigurasi database
		 * tidak sengaja aktif di produksi. Selain harus diaktifkan lewat konfigurasi,
		 * operator pengujian wajib memberi JVM flag berikut:
		 * -Dais.bms.allowTimeoutSimulation=true
		 *
		 * Yang paling penting, timeout dikembalikan SEBELUM doProcess dipanggil.
		 * Implementasi lama meng-commit pembayaran terlebih dahulu lalu mengirim kode
		 * 68. Bank kemudian me-refund transaksi, tetapi eCampus telanjur mencatat lunas.
		 */
		boolean simulasiTimeout = !chek && Boolean.getBoolean("ais.bms.allowTimeoutSimulation")
				&& Common.bolehKonfigurasi("bms_va_sleep", Konfigurasi.TIDAK_AKTIF);
		if (simulasiTimeout) {
			Thread.sleep(3 * 1000);
			String bodyTimeout = timeoutDb(req == null || req.isNull("paymentAmount"), data);
			ais.action.ws.util.PembayaranGatewayHelper.catatLogHostToHost(request, bankHost, data, va, va, "",
					"Connection Timeout (simulasi aman sebelum pencatatan pembayaran)", nominalP, "[]", null,
					data, bodyTimeout, "68", null);
			System.out.println("response->" + bodyTimeout);
			return bodyTimeout;
		}

		String body;
		try {
			// 05, request tidak diizinkan
			body = BMS.doProcess(nominalP, tanggalP, va, bank, bankHost, request, data, true,
					req == null || req.isNull("paymentAmount"), reversal, chek).toString();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/BMS.java:835");
			body = errorDb(req == null || req.isNull("paymentAmount"), data);

		}

		System.out.println("response->" + body);

		return body;
	}

	/**
	 * Membaca badan permintaan, menetapkan {@link BankHost} dari alamat IP pemanggil, memanggil
	 * {@link #doProses}, lalu menuliskan hasilnya sebagai JSON.
	 *
	 * <p>Badan permintaan dibaca baris demi baris lalu digabung <b>tanpa pemisah</b>, sehingga
	 * JSON yang dikirim bank dalam beberapa baris tetap menjadi satu dokumen utuh.</p>
	 *
	 * <p>Identifikasi pemanggil memakai {@link PembayaranUtil#getBankHost(String, String)} dengan
	 * {@link HttpServletRequest#getRemoteAddr()} — alamat TCP sebenarnya, <b>bukan</b> header
	 * {@code X-Forwarded-For} atau sejenisnya yang dapat dipalsukan klien. Nilai yang dihasilkan
	 * hanya dilekatkan pada log dan pencarian VA; ia <b>tidak</b> dipakai untuk menolak
	 * permintaan (lihat javadoc {@link BMS kelas ini}).</p>
	 *
	 * <p>Method ini melayani kedua url-pattern kelas ini; pembedaan inquiry/pembayaran dan
	 * reversal seluruhnya dilakukan {@link #doProses} berdasarkan payload dan akhiran URI.</p>
	 *
	 * @param request  permintaan dari sisi bank
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
	 * bank tetap ada dan hanya status yang diganti.</p>
	 *
	 * <p>Pada berkas ini pemanggilnya di {@link #doProses} berjalan <b>sebelum</b>
	 * {@link #doProcess}, sehingga balasan timeout ini tidak pernah mewakili transaksi yang sudah
	 * telanjur tercatat lunas — lihat pembahasan urutan pada javadoc {@link #doProses}.</p>
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
	 * tetap dikenali pengurai di sisi bank. Kode 91 menandakan gangguan sementara, sehingga bank
	 * memperlakukan tagihan sebagai <b>belum</b> lunas dan dapat mengulang permintaan — perilaku
	 * yang diinginkan, karena kegagalan tak terduga membuat status pembukuan tidak pasti.</p>
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
