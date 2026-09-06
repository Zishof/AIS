package ais.action.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.LogHostToHost;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.VirtualAccountBank;
import ais.database.model.sekolah.Tagihan;

/**
 * Servlet gateway <b>host-to-host (H2H)</b> Bank OCBC NISP — pintu masuk callback bank untuk
 * inquiry tagihan, posting pembayaran, dan pembatalan (reversal) Virtual Account.
 *
 * <h3>Rute yang dilayani (web.xml)</h3>
 * <p>Satu kelas dipetakan ke empat {@code servlet-name} berbeda yang semuanya menunjuk kelas ini;
 * pembedaan alur dilakukan dengan memeriksa akhiran {@link HttpServletRequest#getRequestURI()}:</p>
 * <ul>
 *   <li>{@code /OcbcNisp} — kanal SNAP standar (BI-SNAP): pengambilan token, inquiry
 *       ({@code 2002400}) dan payment ({@code 2002500}) untuk VA intra-produk;</li>
 *   <li>{@code /OcbcNisp/transfer-va/inquiry-intrabank} — inquiry antar bank ({@code 2003200}),
 *       memaksa {@code inquiryRequestId = "123"};</li>
 *   <li>{@code /OcbcNisp/transfer-va/payment-intrabank} — payment antar bank ({@code 2003300});</li>
 *   <li>{@code /OcbcNisp/transfer-va/notify-payment-intrabank} — notifikasi pembatalan yang
 *       diperlakukan sebagai {@code reversal = true} sekaligus {@code intrabank = true}.</li>
 * </ul>
 *
 * <h3>Bentuk respons</h3>
 * <p>Seluruh jawaban berupa JSON SNAP dengan {@code responseCode} tujuh digit. Tiga digit pertama
 * dipakai ulang sebagai status HTTP ({@code Integer.parseInt(responseCode.substring(0, 3))}),
 * sehingga {@code 4042414} menghasilkan HTTP 404 dan {@code 5000001} menghasilkan HTTP 500.</p>
 *
 * <h3>Otentikasi H2H — FAKTA ARSITEKTUR YANG WAJIB DIPAHAMI</h3>
 * <p>Verifikasi tanda tangan RSA (SHA256withRSA atas {@code X-Client-Key|X-Timestamp}) <b>hanya
 * dijalankan pada cabang penerbitan token</b> di {@link #process(HttpServletRequest,
 * HttpServletResponse)}, yaitu ketika body memuat {@code grantType} atau header memuat
 * {@code X-Client-Key}. Cabang transaksi — inquiry, payment, dan reversal, yakni jalur yang benar
 * benar memutasi data keuangan — <b>tidak memeriksa tanda tangan maupun bearer token</b>. Token
 * yang diterbitkan disimpan ke {@link #accessTokens} tetapi tidak pernah dibaca kembali oleh kelas
 * ini. Satu-satunya penyaring yang tersisa pada jalur transaksi adalah:</p>
 * <ol>
 *   <li>pemetaan IP penelepon ke {@link BankHost} lewat
 *       {@link PembayaranUtil#getBankHost(String, String)} — dan itu pun <b>tidak fail-closed</b>:
 *       {@code bankHost} boleh {@code null}, sementara
 *       {@link VirtualAccountBank#ambilVa(String, Double, BankHost)} menerima VA yang kolom
 *       {@code bankHost}-nya kosong untuk host pemanggil mana pun;</li>
 *   <li>penyaring awalan nomor VA lewat konfigurasi
 *       {@code prefix_wajib_diterima_pembayaran_ocbc} dan
 *       {@code bukan_prefix_wajib_diterima_pembayaran_ocbc} — keduanya default kosong, artinya
 *       nonaktif kecuali diisi operator.</li>
 * </ol>
 * <p>Konsekuensinya, pengamanan efektif endpoint ini bertumpu pada pembatasan jaringan/firewall di
 * luar aplikasi, bukan pada kode. Catatan ini adalah dokumentasi keadaan terkini; jangan diubah
 * tanpa koordinasi dengan pihak bank karena perubahan kontrak H2H berdampak langsung ke setelmen.
 * Perlu dicatat pula bahwa nilai {@code X-Timestamp} tidak divalidasi kesegarannya, sehingga
 * permintaan token yang sudah pernah ditandatangani dapat diputar ulang.</p>
 *
 * <h3>Hubungan dengan pencatatan log</h3>
 * <p>Setiap pemanggilan {@link #doProcess} — berhasil maupun gagal — menulis satu baris
 * {@link LogHostToHost} melalui {@code PembayaranGatewayHelper.catatLogHostToHost} di blok
 * {@code finally}. Baris itu menyimpan payload mentah dari bank, sehingga tabel log H2H memuat
 * data transaksi perbankan sungguhan dan harus diperlakukan sebagai data sensitif.</p>
 *
 * @see ais.action.ws.util.PembayaranGatewayHelper
 * @see VirtualAccountBank
 * @see LogHostToHost
 * @see BankHost
 */
public class OcbcNisp extends HttpServlet {
	/** Versi serial standar {@link java.io.Serializable} untuk kontrak servlet. */
	private static final long serialVersionUID = 1L;

	/**
	 * Singleton helper pembayaran bersama, dipakai untuk memetakan alamat IP penelepon menjadi
	 * {@link BankHost} dan untuk menghitung total serta denda cicilan.
	 *
	 * <p>Field sengaja {@code public static} agar servlet bank lain memakai instance yang sama;
	 * karena itu jangan menugasinya ulang saat runtime.</p>
	 */
	public static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/**
	 * Format tanggal {@code MMddHHmmss} yang dipakai bank pada field tanggal transaksi.
	 *
	 * <p>Dibungkus {@link ThreadLocal} karena {@link SimpleDateFormat} tidak aman dipakai banyak
	 * thread sekaligus, sedangkan servlet ini dilayani oleh worker thread container. Pola format
	 * ini <b>tidak memuat tahun</b>; tanggal hasil parse karenanya jatuh pada tahun 1970 dan
	 * dipakai apa adanya sebagai penanda waktu setoran.</p>
	 */
	public static final ThreadLocal<SimpleDateFormat> dateFormat = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("MMddHHmmss");
		}
	};

	/**
	 * Konstruktor default yang dibutuhkan container servlet.
	 *
	 * <p>Tidak melakukan inisialisasi apa pun selain memanggil konstruktor {@link HttpServlet}.
	 * Seluruh state per-permintaan dibentuk di dalam
	 * {@link #process(HttpServletRequest, HttpServletResponse)}.</p>
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public OcbcNisp() {
		super();
	}

	/**
	 * Menerima permintaan HTTP GET dan meneruskannya apa adanya ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}.
	 *
	 * <p>GET, POST, dan PUT diperlakukan identik: kelas ini tidak membedakan metode HTTP sama
	 * sekali, karena beberapa kanal bank mengirim inquiry lewat GET sementara payment lewat POST
	 * atau PUT. Pembedaan alur sepenuhnya bersandar pada akhiran URI dan isi body JSON.</p>
	 *
	 * <p>Exception apa pun ditelan oleh {@link Common#tampilErrorJikaAdmin(Exception)} agar tidak
	 * menghasilkan halaman error container ke arah bank. Efek sampingnya, ketika kegagalan terjadi
	 * sebelum {@code process} sempat menulis body, bank menerima respons kosong dengan status 200
	 * bawaan container.</p>
	 *
	 * @param request  permintaan dari bank
	 * @param response respons yang akan diisi JSON SNAP
	 * @throws ServletException bila container melaporkan kegagalan servlet
	 * @throws IOException      bila penulisan respons gagal
	 * @see HttpServlet#doGet(HttpServletRequest, HttpServletResponse)
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
	 * Menerima permintaan HTTP POST — metode utama yang dipakai bank untuk inquiry, payment, dan
	 * permintaan token — lalu meneruskannya ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}.
	 *
	 * <p>Perilaku dan penanganan error identik dengan
	 * {@link #doGet(HttpServletRequest, HttpServletResponse)}.</p>
	 *
	 * @param request  permintaan dari bank
	 * @param response respons yang akan diisi JSON SNAP
	 * @throws ServletException bila container melaporkan kegagalan servlet
	 * @throws IOException      bila penulisan respons gagal
	 * @see HttpServlet#doPost(HttpServletRequest, HttpServletResponse)
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
	 * Menerima permintaan HTTP PUT dan meneruskannya ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}.
	 *
	 * <p>Override ini ada karena sebagian kanal OCBC NISP mengirim notifikasi pembatalan
	 * (reversal) memakai PUT. Perilaku dan penanganan error identik dengan
	 * {@link #doGet(HttpServletRequest, HttpServletResponse)}.</p>
	 *
	 * @param req  permintaan dari bank
	 * @param resp respons yang akan diisi JSON SNAP
	 * @throws ServletException bila container melaporkan kegagalan servlet
	 * @throws IOException      bila penulisan respons gagal
	 */
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			process(req, resp);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static JSONObject doProcess(Double nominalP, String tanggalP, String va, String bank, BankHost bankHost,
			HttpServletRequest request, String data, String partnerServiceId, String inquiryRequestId,
			String paymentRequestId, String trxId, String customer_number, String partnerReferenceNo, boolean chekLagi,
			boolean inquery, boolean reversal, boolean antarbank) throws Exception {

		JSONObject virtualAccountData = new JSONObject();

		JSONObject jsonObjectResponse = new JSONObject();

		if (antarbank) {
			jsonObjectResponse.put("responseCode", inquery ? "2003200" : "2003300");
		} else {
			jsonObjectResponse.put("responseCode", inquery ? "2002400" : "2002500");
		}

		jsonObjectResponse.put("responseMessage", "Successful");
		jsonObjectResponse.put("virtualAccountData", virtualAccountData);

		if (partnerReferenceNo != null) {
			virtualAccountData.put("partnerReferenceNo", partnerReferenceNo);
		}

		if (trxId != null) {
			jsonObjectResponse.put(trxId, trxId);
		}

		virtualAccountData.put("partnerServiceId", partnerServiceId);

		JSONObject feeAmount = new JSONObject();
		virtualAccountData.put("feeAmount", feeAmount);
		feeAmount.put("value", "0.00");
		feeAmount.put("currency", "IDR");

		if (!reversal) {
			if (!inquery) {
				jsonObjectResponse.put("payment_amount", 0);
				virtualAccountData.put("paymentRequestId", paymentRequestId);

				JSONObject paidAmount = new JSONObject();
				virtualAccountData.put("paidAmount", paidAmount);
				paidAmount.put("value", nominalP.toString());
				paidAmount.put("currency", "IDR");

			} else {
				virtualAccountData.put("inquiryRequestId", inquiryRequestId);
			}
		}

		JSONObject totalAmount = new JSONObject();
		virtualAccountData.put("totalAmount", totalAmount);
		totalAmount.put("value", nominalP.toString());
		totalAmount.put("currency", "IDR");

		JSONArray billDetails = new JSONArray();
		virtualAccountData.put("billDetails", billDetails);

		System.out.println("bankHost->" + bankHost + ", inquiryRequestId " + inquiryRequestId + ", paymentRequestId "
				+ paymentRequestId + ", partnerServiceId " + partnerServiceId + " va " + va + " customer_number "
				+ customer_number + " inquery " + inquery + " nominalP " + nominalP + " partnerReferenceNo "
				+ partnerReferenceNo);

		{ // TANPA syarat bankHost: request bank apa pun WAJIB tercatat ke log H2H (lihat finally)
			String nim = "";
			String nama = "";
			JSONArray rincian = new JSONArray();
			// Jejak stack trace bila inquiry/pembayaran error; disimpan ke kolom log H2H.
			String h2hStackTrace = null;

			Date tanggal = ais.ui.util.WaktuUtil.getDate();
			try {
				tanggal = dateFormat.get().parse(tanggalP);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/OcbcNisp.java:175");
			}

			VirtualAccountBank virtualAccountBankNtt = null;
			try {

				if ((va == null || va.trim().isEmpty()) && customer_number != null
						&& !customer_number.trim().isEmpty()) {
					va = customer_number.trim();
				}

				// 1. Pastikan VA tidak null untuk mencegah NPE saat memanggil va.startsWith()
				if (va == null || va.trim().isEmpty()) {
					if (inquery) {
						jsonObjectResponse.put("responseCode", "4043212");
						jsonObjectResponse.put("responseMessage", "Bill not found");
					} else {
						jsonObjectResponse.put("responseCode", "4043312");
						jsonObjectResponse.put("responseMessage", "Bill not found");
					}

				} else {
					// 2. Ambil konfigurasi dengan aman (Safe check)
					String prefixWajib = "";
					String prefixTerlarang = "";

					// Asumsi getKonfigurasi bisa null, atau getNilai bisa null
					Konfigurasi configWajib = Common.getKonfigurasi("prefix_wajib_diterima_pembayaran_ocbc", "");
					if (configWajib != null && configWajib.getNilai() != null) {
						prefixWajib = configWajib.getNilai().trim();
					}

					Konfigurasi configTerlarang = Common.getKonfigurasi("bukan_prefix_wajib_diterima_pembayaran_ocbc",
							"");
					if (configTerlarang != null && configTerlarang.getNilai() != null) {
						prefixTerlarang = configTerlarang.getNilai().trim();
					}

					// 3. Logika pengecekan dengan tanda kurung yang memperjelas scope
					boolean isMissingRequiredPrefix = !prefixWajib.isEmpty() && !va.startsWith(prefixWajib);
					boolean hasForbiddenPrefix = !prefixTerlarang.isEmpty() && va.startsWith(prefixTerlarang);

					if (isMissingRequiredPrefix || hasForbiddenPrefix) {
						if (inquery) {
							jsonObjectResponse.put("responseCode", "4043212");
							jsonObjectResponse.put("responseMessage", "Bill not found");
						} else {
							jsonObjectResponse.put("responseCode", "4043312");
							jsonObjectResponse.put("responseMessage", "Bill not found");
						}

					} else {

						virtualAccountBankNtt = VirtualAccountBank.ambilVa(customer_number, nominalP, bankHost);

						if (virtualAccountBankNtt == null) {
							String subVa;
							try {
								subVa = va.substring(partnerServiceId.length());
							} catch (Exception e) {
								subVa = customer_number;
							}
							virtualAccountBankNtt = VirtualAccountBank.ambilVa(subVa, nominalP, bankHost);
						}

						if (virtualAccountBankNtt == null) {
							virtualAccountBankNtt = VirtualAccountBank.ambilVa(va, nominalP, bankHost);
						}

						if (virtualAccountBankNtt == null) {

							if (antarbank) {

								if (inquery) {
									jsonObjectResponse.put("responseCode", "4043212");
									jsonObjectResponse.put("responseMessage", "Bill not found");
								} else {
									jsonObjectResponse.put("responseCode", "4043312");
									jsonObjectResponse.put("responseMessage", "Bill not found");
								}

							} else {
								if (inquery) {
									jsonObjectResponse.put("responseCode", "4042412");
									jsonObjectResponse.put("responseMessage", "Bill not found");
								} else {
									jsonObjectResponse.put("responseCode", "4042512");
									jsonObjectResponse.put("responseMessage", "Bill not found");
								}
							}

						}

						else {
							totalAmount.put("value", virtualAccountBankNtt.getTotal().toString());

							if (!inquery) {
								jsonObjectResponse.put("payment_amount", nominalP);

								JSONObject bill_number = new JSONObject();
								bill_number.put("bill_number", virtualAccountBankNtt.getKode());
								jsonObjectResponse.put("additionalInfo", bill_number);
							}
						}

						if (virtualAccountBankNtt != null && virtualAccountBankNtt.getKadaluarsa() != null) {
							System.out.println("Kadaluara "
									+ Common.databaseDateFormat.get().format(virtualAccountBankNtt.getKadaluarsa()));
						}

//				if (!reversal && virtualAccountBankNtt != null
//						&& virtualAccountBankNtt.getKadaluarsa().before(WaktuUtil.getDate())) {
//
//					if (antarbank) {
//						if (inquery) {
//							jsonObjectResponse.put("responseCode", "4043212");
//							jsonObjectResponse.put("responseMessage", "Bill Expired");
//						} else {
//							jsonObjectResponse.put("responseCode", "4043312");
//							jsonObjectResponse.put("responseMessage", "Bill Expired");
//						}
//					} else {
//						jsonObjectResponse.put("responseCode", "4042419");
//						jsonObjectResponse.put("responseMessage", "Bill Expired");
//					}
//				} else {

						if (!reversal && !chekLagi && virtualAccountBankNtt != null && VirtualAccountBank.isSudahTerbayar(virtualAccountBankNtt)) {

							if (antarbank) {
								if (inquery) {
									jsonObjectResponse.put("responseCode", "4043214");
									jsonObjectResponse.put("responseMessage", "Bill has been paid");
								} else {
									jsonObjectResponse.put("responseCode", "4043314");
									jsonObjectResponse.put("responseMessage", "Bill has been paid");
								}
							} else {
								jsonObjectResponse.put("responseCode", "4042414");
								jsonObjectResponse.put("responseMessage", "Bill has been paid");
							}
						}

						else if (virtualAccountBankNtt != null && virtualAccountBankNtt.getTotal() > 0.1) {
							Session session = HibernateUtil.getSessionFactory().openSession();
							try {

								Double nominal = nominalP;

								if (!reversal && (nominalP != null && nominalP > 0.1)
										&& nominal.intValue() != virtualAccountBankNtt.totalBiaya()) {

									if (antarbank) {
										jsonObjectResponse.put("responseCode", "4043313");
										jsonObjectResponse.put("responseMessage", "Invalid Amount");
									} else {
										jsonObjectResponse.put("responseCode", "4042513");
										jsonObjectResponse.put("responseMessage", "Invalid Amount");
									}

								} else {
									int totalTabungan = 0;
									if (virtualAccountBankNtt.getBiayaAdmin() > 0.1) {
										JSONObject bill = new JSONObject();
										bill.put("billSubCompany", "Biaya admin");

										JSONObject billAmount = new JSONObject();
										bill.put("billAmount", billAmount);

										bill.put("currency", "IDR");
										billAmount.put("value",
												virtualAccountBankNtt.getBiayaAdmin().intValue() + ".00");

										if (virtualAccountBankNtt.getTabungan() > 0.1) {
											totalTabungan += virtualAccountBankNtt.getBiayaAdmin().intValue();
										} else {
											billDetails.put(bill);
										}

									}

									if (virtualAccountBankNtt.getSiswa() != null
											|| virtualAccountBankNtt.getCalonSiswa() != null) {

										if (virtualAccountBankNtt.getSiswa() != null) {
											nim = virtualAccountBankNtt.getSiswa().getNomorInduk();
											nama = virtualAccountBankNtt.getSiswa().getNama();
											if (customer_number == null || customer_number.isEmpty()) {
												customer_number = nim;
											}
										} else if (virtualAccountBankNtt.getCalonSiswa() != null) {
											nim = virtualAccountBankNtt.getCalonSiswa().getNomorInduk();
											nama = virtualAccountBankNtt.getCalonSiswa().getNama();
											if (customer_number == null || customer_number.isEmpty()) {
												customer_number = nim;
											}
										}

										virtualAccountData.put("customerNo", customer_number);
										virtualAccountData.put("virtualAccountNo", va);
										virtualAccountData.put("virtualAccountName", nama);

										Map<String, List<Tagihan>> map = VirtualAccountBank.bayarSiswa(
												virtualAccountBankNtt, session, tanggal, bank, inquery, data, false);
										
										for (List<Tagihan> tagihans : map.values()) {

											for (Tagihan tagihan : tagihans) {

												JSONObject bill = new JSONObject();
												bill.put("billSubCompany", tagihan.getItemBiayaSekolah().getNama());

												JSONObject billAmount = new JSONObject();
												bill.put("billAmount", billAmount);

												bill.put("currency", "IDR");
												billAmount.put("value", tagihan.getNominal().intValue() + ".00");

												if (virtualAccountBankNtt.getTabungan() > 0.1) {
													totalTabungan += tagihan.getNominal().intValue();
												} else {
													billDetails.put(bill);
												}

												if (tagihan.getDenda() > 0.1) {
													bill = new JSONObject();
													bill.put("billSubCompany",
															"Denda " + tagihan.getItemBiayaSekolah().getNama());

													billAmount = new JSONObject();
													bill.put("billAmount", billAmount);

													bill.put("currency", "IDR");
													billAmount.put("value", tagihan.getDenda().intValue() + ".00");
													if (virtualAccountBankNtt.getTabungan() > 0.1) {
														totalTabungan += tagihan.getNominal().intValue();
													} else {
														billDetails.put(bill);
													}
												}

											}

										}

										if (virtualAccountBankNtt.getTabungan() > 0.1) {

											JSONObject bill = new JSONObject();
											bill.put("billSubCompany", "Total Item Biaya Dipotong Tabungan");

											JSONObject billAmount = new JSONObject();
											bill.put("billAmount", billAmount);

											bill.put("currency", "IDR");
											billAmount.put("value",
													(totalTabungan - virtualAccountBankNtt.getTabungan().intValue())
															+ ".00");

											billDetails.put(bill);
										}

										if (reversal) {
											VirtualAccountBank.updateVa(virtualAccountBankNtt, tanggal, null, data,
													bank);

											if (antarbank) {
												jsonObjectResponse.put("responseCode", "2003400");
											} else {
												jsonObjectResponse.put("responseCode", "2002900");
											}

											jsonObjectResponse.put("responseMessage", "UPDATE STATUS SUCCESS");
										}

									} else {

										JenisKegiatan jenisKegiatan = virtualAccountBankNtt.getJenisKegiatan();
										Mahasiswa mahasiswa = virtualAccountBankNtt.getMahasiswa();
										BiodataCalonMahasiswa biodataCalonMahasiswa = virtualAccountBankNtt
												.getBiodataCalonMahasiswa();

										Integer semester = virtualAccountBankNtt.getSemester();

										nim = mahasiswa == null ? biodataCalonMahasiswa.getNoRegistrasi()
												: mahasiswa.getNim();
										nama = mahasiswa == null ? biodataCalonMahasiswa.getNama()
												: mahasiswa.getNama();

										if (customer_number == null || customer_number.isEmpty()) {
											customer_number = nim;
										}

										virtualAccountData.put("customerNo", customer_number);
										virtualAccountData.put("virtualAccountNo", va);
										virtualAccountData.put("virtualAccountName", nama);

										Kegiatan kegiatan = (Kegiatan) (virtualAccountBankNtt.getKegiatan() == null
												? null
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
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/OcbcNisp.java:520");

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
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/OcbcNisp.java:565");
														}

														if (itemBiaya.getPenghitungan()
																.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
															subtotal = 0.0 - subtotal;
														}

														JSONObject bill = new JSONObject();
														bill.put("billSubCompany", itemBiaya.getNama());

														JSONObject billAmount = new JSONObject();
														bill.put("billAmount", billAmount);

														bill.put("currency", "IDR");
														billAmount.put("value", subtotal.intValue() + ".00");

														billDetails.put(bill);

														if (reversal) {
															session.getTransaction().begin();
															session.createSQLQuery(
																	"delete from cicilan_pembayaran where ref = " + ref)
																	.executeUpdate();
															session.getTransaction().commit();

															jsonObjectResponse.put("responseCode", "2002900");
															jsonObjectResponse.put("responseMessage",
																	"UPDATE STATUS SUCCESS");

														}

														else if (!inquery) {
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
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/OcbcNisp.java:663");
														}

														if (itemBiaya.getPenghitungan()
																.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
															subtotal = 0.0 - subtotal;
														}

														JSONObject bill = new JSONObject();
														bill.put("billSubCompany", itemBiaya.getNama());

														JSONObject billAmount = new JSONObject();
														bill.put("billAmount", billAmount);

														bill.put("currency", "IDR");
														billAmount.put("value", subtotal.intValue() + ".00");

														billDetails.put(bill);

														if (reversal) {
															session.getTransaction().begin();
															session.createSQLQuery(
																	"delete from cicilan_pembayaran where ref = " + ref)
																	.executeUpdate();
															session.getTransaction().commit();

															jsonObjectResponse.put("responseCode", "2002900");
															jsonObjectResponse.put("responseMessage",
																	"UPDATE STATUS SUCCESS");

														}

														else if (!inquery) {
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
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/OcbcNisp.java:757");
														}

														Long detailBiayaId = null;
														try {
															String[] spl = idPemBul.split("-");
															detailBiayaId = Long.parseLong(spl[4]);
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/OcbcNisp.java:764");
														}

														if (itemBiaya.getPenghitungan()
																.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
															subtotal = 0.0 - subtotal;
														}

														JSONObject bill = new JSONObject();
														bill.put("billSubCompany", itemBiaya.getNama());

														JSONObject billAmount = new JSONObject();
														bill.put("billAmount", billAmount);

														bill.put("currency", "IDR");
														billAmount.put("value", subtotal.intValue() + ".00");

														billDetails.put(bill);

														if (!inquery) {
															CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
																	.createCriteria(CicilanPembayaran.class)
																	.add(Restrictions.eq("ref", ref)).setMaxResults(1)
																	.uniqueResult();

															if (cicilanPembayaran == null) {
																cicilanPembayaran = new CicilanPembayaran(
																		DetailBiaya.muatRefAman(session, detailBiayaId));
															}
															cicilanPembayaran
																	.setDetailBiaya(DetailBiaya.muatRefAman(session, detailBiayaId));
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
													if (!reversal) {
														ais.action.ws.util.PembayaranGatewayHelper.prosesSatuTokenKeranjang(session, idPemBul,
																virtualAccountBankNtt, inquery, bank, bankHost, tanggal, data, null);
													}
												}
											}
										}

										if (inquery) {
//								status.put("isError", "false");
//								status.put("errorCode", "00");
//								status.put("statusDescription", "Inquiry success");
										} else {
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

											if (reversal) {
												VirtualAccountBank.updateVa(virtualAccountBankNtt, tanggal, null, data,
														bank);

												if (antarbank) {
													jsonObjectResponse.put("responseCode", "2003400");
												} else {
													jsonObjectResponse.put("responseCode", "2002900");
												}

												jsonObjectResponse.put("responseMessage", "UPDATE STATUS SUCCESS");
											} else {
												VirtualAccountBank.updateVa(virtualAccountBankNtt, tanggal, kegiatan,
														data, bank);
											}

										}

									}

								}
							} finally {
								if (session != null) {
									try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/OcbcNisp.java:884");}
									try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/OcbcNisp.java:885");}
									try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/OcbcNisp.java:886");}
								}
							}
						}
					}
				}
			} catch (Exception e) {
				HibernateUtil.rollbackTransaction();

				jsonObjectResponse.put("responseCode", "5000001");
				jsonObjectResponse.put("responseMessage",
						"Unknown Internal Server Failure, Please retry the process again");

				h2hStackTrace = ais.action.ws.util.PembayaranGatewayHelper.ambilStackTrace(e);
				Common.tampilErrorJikaAdmin(e);
			} finally {
				// JAMINAN: log H2H SELALU dicatat & tercommit walau terjadi error/exception
				// (helper: session terdedikasi + commit + retry, tak pernah gagal).
				ais.action.ws.util.PembayaranGatewayHelper.catatLogHostToHost(request, bankHost, data, nim, va, nama,
						jsonObjectResponse.toString(), nominalP, rincian.toString(), h2hStackTrace);
			}
		}
		return jsonObjectResponse;
	}

	private List<String> accessTokens = new ArrayList<String>();
	private Map<String, String> customerNoAndPartner = new HashMap<String, String>();

	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {

		// Read from request

		boolean intrabank = false;
		boolean reversal = false;

		if (request.getRequestURI().endsWith("notify-payment-intrabank")) {
			reversal = true;
			intrabank = true;
		} else if (request.getRequestURI().endsWith("intrabank")) {
			intrabank = true;
		}

		StringBuilder buffer = new StringBuilder();
		BufferedReader reader = request.getReader();
		String line;
		while ((line = reader.readLine()) != null) {
			buffer.append(line);
		}
		String data = buffer.toString();
		Enumeration<String> header = request.getHeaderNames();
		JSONObject jsonObjectHeader = new JSONObject();
		while (header.hasMoreElements()) {
			String key = header.nextElement();
			jsonObjectHeader.put(key, request.getHeader(key));
		}

		System.out.println("==> VA data => " + data + " request.getRequestURI() " + request.getRequestURI());
		System.out.println("==> VA header => " + jsonObjectHeader);

		String strClientId = jsonObjectHeader.isNull("X-Client-Key") ? null
				: jsonObjectHeader.get("X-Client-Key").toString();

		if (!jsonObjectHeader.isNull("X-CLIENT-KEY")) {
			strClientId = jsonObjectHeader.get("X-CLIENT-KEY").toString();
		}

		String responseMessage = "Successful";
		String responseCode = "200";
		String body = "";
		if (data != null) {

			JSONObject req = data == null || data.isEmpty() ? null : new JSONObject(data);

			String grantType = req == null || req.isNull("grantType") ? null : req.get("grantType").toString();

			if (grantType != null || strClientId != null) {

				String strISOTimeStamp = jsonObjectHeader.isNull("X-Timestamp") ? null
						: jsonObjectHeader.get("X-Timestamp").toString();

				if (!jsonObjectHeader.isNull("X-TIMESTAMP")) {
					strISOTimeStamp = jsonObjectHeader.get("X-TIMESTAMP").toString();
				}

				String strToSign = strClientId + "|" + strISOTimeStamp;
				String strPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAot1QSKMcrF/nWBypLw0Hzoaz+0/WqujbR1RAeyjVOLGqufSE2JG290QeYj3aw0yZJcur5XM/+3kTeP3NjOpCgQrMRSn/fmScRj34jx/ashX0C8JHozLipLuOvYhnzpjphGbKncp87Ye1TSQMrQMXmVGHabxk5XbxSePazdorRq4DvlEio2cj/uYUEZ7X3EI3VZOnsIRrSqqXGjoy+zj8ArylZJp9sd139uQmyrz1w2EdawdBfW9WiC1hCucFJENjK5xZEHcb3WdtuT5Z8nRH70T2shs3nEHFW93beFd2Yb7OT9EKqewJUAxFNKl5dl/va0Ruk6FlNv+Stlria2o7zwIDAQAB";

				strPublicKey = Common.getKonfigurasi("strPublicKey_ocbc", strPublicKey).getNilai();

				String strSignature = jsonObjectHeader.isNull("X-Signature") ? null
						: jsonObjectHeader.get("X-Signature").toString();

				if (!jsonObjectHeader.isNull("X-SIGNATURE")) {
					strSignature = jsonObjectHeader.get("X-SIGNATURE").toString();
				}

				byte[] signDecode = Base64.getDecoder().decode(strSignature);

				byte[] bPublicKey = Base64.getDecoder().decode(strPublicKey);
				X509EncodedKeySpec specPublic = new X509EncodedKeySpec(bPublicKey);
				KeyFactory kf = KeyFactory.getInstance("RSA");

				Signature privateSignature = Signature.getInstance("SHA256withRSA");
				privateSignature.initVerify(kf.generatePublic(specPublic));
				privateSignature.update(strToSign.getBytes("UTF8"));

				Boolean validateSignature = privateSignature.verify(signDecode);

				System.out.println("==> validateSignature " + validateSignature);

				if (validateSignature) {

					int expiresIn = 1000 * 60 * 24;

					String token = UUID.randomUUID().toString();
					accessTokens.add(token);
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("accessToken", token);
					jsonObject.put("tokenType", "bearer");
					jsonObject.put("expiresIn", expiresIn);

					String tokenOcbcResponseCode = Common.getKonfigurasi("token_ocbc_response_code", "2007300")
							.getNilai();
					String tokenOcbcResponseMessage = Common.getKonfigurasi("token_ocbc_response_message", "Success")
							.getNilai();

					jsonObject.put("responseCode", tokenOcbcResponseCode);
					jsonObject.put("responseMessage", tokenOcbcResponseMessage);

					responseMessage = jsonObject.isNull("responseMessage") ? "Fail"
							: jsonObject.getString("responseMessage");
					responseCode = jsonObject.isNull("responseCode") ? "401" : jsonObject.getString("responseCode");

					body = jsonObject.toString();
				} else {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("responseCode", "4017300");
					jsonObject.put("responseMessage", "Unathorized. [Signature]");

					responseMessage = jsonObject.isNull("responseMessage") ? "Fail"
							: jsonObject.getString("responseMessage");
					responseCode = jsonObject.isNull("responseCode") ? "401" : jsonObject.getString("responseCode");

					body = jsonObject.toString();
				}
			} else {

				String va = req == null || req.isNull("virtualAccountNo") ? null
						: req.getString("virtualAccountNo").trim();

				String trxId = req == null || req.isNull("trxId") ? null : req.getString("trxId").trim();

				String inquiryRequestId = req == null || req.isNull("inquiryRequestId") ? null
						: req.getString("inquiryRequestId").trim();

				String paymentRequestId = req == null || req.isNull("paymentRequestId") ? null
						: req.getString("paymentRequestId").trim();

				String partnerServiceId = req == null || req.isNull("partnerServiceId") ? null
						: req.getString("partnerServiceId").trim();

				String customer_number = req == null || req.isNull("customer_number") ? null
						: req.getString("customer_number").trim();

				if (req != null && !req.isNull("customerNo")) {
					customer_number = req.getString("customerNo").trim();
				}

				String bank = "OCBC NISP";
				double nominalP = 0.0;
				try {
					nominalP = req == null || req.isNull("paidAmount") ? 0.0
							: Double.parseDouble(req.getJSONObject("paidAmount").get("value") + "");
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/OcbcNisp.java:1060");

				}
				try {
					if (req != null && !req.isNull("payment_amount")) {
						Double c = Double.parseDouble(req.get("payment_amount") + "");
						if (c > 0.1) {
							nominalP = c;
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/OcbcNisp.java:1070");

				}

				try {
					if (req != null && !req.isNull("amount")) {
						Double c = Double.parseDouble(req.getJSONObject("amount").get("value") + "");
						if (c > 0.1) {
							nominalP = c;
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/OcbcNisp.java:1081");

				}

				String tanggalP = dateFormat.get().format(new Date());

				String paidStatus = req == null || req.isNull("paidStatus") ? null : req.getString("paidStatus").trim();

				// 05, request tidak diizinkan
				BankHost bankHost = pembayaranUtil.getBankHost(request.getRemoteAddr(), bank);

				try {
					if (inquiryRequestId == null) {
						if (!req.isNull("amount")) {
							inquiryRequestId = "123";
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/OcbcNisp.java:1098");

				}

				if (request.getRequestURI().endsWith("inquiry-intrabank")) {
					inquiryRequestId = "123";
				}

				String partnerReferenceNo = null;

				try {

					String customerNo = req == null || req.isNull("customerNo") ? null
							: req.getString("customerNo").trim();

					if (customerNo != null && req != null && !req.isNull("partnerReferenceNo")
							&& req.getString("partnerReferenceNo") != "null") {
						partnerReferenceNo = req.getString("partnerReferenceNo").trim();
						customerNoAndPartner.put(customerNo, partnerReferenceNo);
					}

					else if (customerNo != null && customerNoAndPartner.containsKey(customerNo)) {
						partnerReferenceNo = customerNoAndPartner.get(req.getString("customerNo"));
					}

				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/OcbcNisp.java:1123");

				}

				JSONObject o = OcbcNisp.doProcess(nominalP, tanggalP, va, bank, bankHost, request, data,
						partnerServiceId, inquiryRequestId, paymentRequestId, trxId, customer_number,
						partnerReferenceNo, false, req.isNull("paidAmount") && inquiryRequestId != null,
						(paidStatus != null && paidStatus.equalsIgnoreCase("n")) || reversal, intrabank);

				responseMessage = o.isNull("responseMessage") ? "Fail" : o.getString("responseMessage");
				responseCode = o.isNull("responseCode") ? "401" : o.getString("responseCode");

				body = o.toString();
			}
		}
		int status = Integer.parseInt(responseCode.substring(0, 3));
		System.out.println("status-> " + status);
		System.out.println("responseMessage-> " + responseMessage);
		System.out.println("body->\n" + body);

		response.setStatus(status);

		response.setHeader("length", body.length() + "");
		response.setHeader("Content-Type", "application/json");
		PrintWriter writer = response.getWriter();

		writer.write(body);

	}

}

