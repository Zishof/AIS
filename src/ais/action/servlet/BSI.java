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
import org.hibernate.Transaction;
import org.hibernate.TransactionException;
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
 * Servlet implementation class CheckISBN
 */
public class BSI extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	private static final ThreadLocal<SimpleDateFormat> dateFormat = new ThreadLocal<SimpleDateFormat>() {
		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("yyyyMMddHHmmss");
		}
	};

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public BSI() {
		super();
	}

	/**
	 * Commit transaksi HANYA jika transaksi tersebut masih benar-benar aktif.
	 *
	 * <p><b>Root cause yang dijaga di sini.</b> {@code Common.refreshSaveOrUpdate}/{@code refreshUpdate}
	 * bisa menelan exception (mis. pelanggaran constraint NOT NULL yang bukan "unique constraint") lalu
	 * diam-diam melakukan rollback transaksi tanpa melempar ulang exception-nya (lihat
	 * {@code CommonHibernateHelper}). Jika kode pemanggil di sini langsung memanggil
	 * {@code session.getTransaction().commit()} setelah itu, Hibernate melempar
	 * {@code TransactionException: Transaction not successfully started} yang membingungkan --
	 * gejala jauh dari akar masalah aslinya (pola sama seperti bug kodeunik null di Kegiatan/Mandiri).</p>
	 *
	 * <p>Dengan guard ini: bila transaksi ternyata sudah tidak aktif saat hendak commit, JANGAN commit
	 * (mencegah TransactionException generic), catat detail lokasi ke {@code ErrorAuditUtil}, lalu
	 * lempar ulang RuntimeException yang jelas supaya ditangkap oleh catch umum method pemanggil
	 * (yang sudah mengisi rc=ERR-DB & mencatat log H2H) -- bukan silently melanjutkan seolah sukses.</p>
	 */
	private static void commitTransaksiAman(Session session, String lokasi) {
		Transaction tx = session == null ? null : session.getTransaction();
		if (tx != null && tx.isActive()) {
			tx.commit();
		} else {
			String pesan = "Transaksi tidak aktif saat hendak commit di BSI.java:" + lokasi
					+ " -- kemungkinan sudah di-rollback akibat error sebelumnya (mis. pelanggaran constraint) yang tertelan sebelum commit.";
			ais.common.ErrorAuditUtil.record(new TransactionException(pesan),
					"auto-audit src/ais/action/servlet/BSI.java:" + lokasi);
			throw new RuntimeException(pesan);
		}
	}

	/**
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

	@SuppressWarnings("unchecked")
	public static JSONObject doProcess(double nominalP, String tanggalP, String va, String companyCode,
			String RequestID, String bank, BankHost bankHost, HttpServletRequest request, String data, boolean chekLagi,
			boolean inquery, boolean chek) throws Exception {

		JSONObject response = new JSONObject();

		response.put("rc", "OK");
		response.put("msg", inquery ? "Inquiry Succeeded" : "Payment Succeeded");
		response.put("nomorPembayaran", va);
		response.put("idPelanggan", va);

		response.put("nama", "");
		response.put("totalNominal", 0.0);

		JSONArray arr_informasi = new JSONArray();
		response.put("informasi", arr_informasi);

		JSONArray rincian = new JSONArray();
		response.put("rincian", rincian);

		String idTagihan = Common.getGeneratedBarCode();

		response.put("idTagihan", idTagihan);

		{ // TANPA syarat bankHost: request bank apa pun WAJIB tercatat ke log H2H (lihat finally)
			String nim = "";
			String nama = "";
			// Jejak stack trace bila inquiry/pembayaran error; disimpan ke kolom log H2H.
			String h2hStackTrace = null;

			VirtualAccountBank virtualAccountBankNtt = null;
			Session session = null;
			try {

				virtualAccountBankNtt = VirtualAccountBank.ambilVa(va, nominalP, bankHost);

				if (virtualAccountBankNtt == null) {
					response.put("rc", "ERR-NOT-FOUND");
					response.put("msg", "Error nomor VA tidak ditemukan");
				}

				else if (VirtualAccountBank.isSudahTerbayarUntukPayment(virtualAccountBankNtt, inquery, false, chek)) {

					response.put("rc", "ERR-ALREADY-PAID");
					response.put("msg", "Sudah Terbayar");

				}

				else if (virtualAccountBankNtt != null && virtualAccountBankNtt.getTotal() > 0.1) {
					session = HibernateUtil.openSession();
					{

						int mytotal = virtualAccountBankNtt.totalBiaya();

						boolean biaya_admin_jangan_dimasukkan_saat_simpan_va = Common.bolehKonfigurasi("biaya_admin_jangan_dimasukkan_saat_simpan_va", Konfigurasi.TIDAK_AKTIF);

						if (biaya_admin_jangan_dimasukkan_saat_simpan_va) {
							mytotal = virtualAccountBankNtt.getTotal().intValue();
						}

						response.put("totalNominal", mytotal);

						Double nominal = nominalP;

						if (!inquery && nominal.intValue() != mytotal) {
							response.put("rc", "ERR-PAYMENT-WRONG-AMOUNT");
							response.put("msg", "Terdapat kesalahan nilai pembayaran " + nominal.intValue()
									+ " tidak sama dengan tagihan " + mytotal);
						} else {

							Date tanggal = ais.ui.util.WaktuUtil.getDate();
							try {
								tanggal = dateFormat.get().parse(tanggalP);
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/BSI.java:161");
							}

							if (virtualAccountBankNtt.getSiswa() != null
									|| virtualAccountBankNtt.getCalonSiswa() != null) {

								if (!biaya_admin_jangan_dimasukkan_saat_simpan_va) {
									if (virtualAccountBankNtt.getBiayaAdmin() > 0.1) {
										JSONObject bill = new JSONObject();
										bill.put("kode_rincian", "Biaya admin");
										bill.put("deskripsi", "Biaya admin");
										bill.put("nominal", virtualAccountBankNtt.getBiayaAdmin().intValue());
										rincian.put(bill);

									}
								}

								String hp = "";
								String email = "";
								if (virtualAccountBankNtt.getSiswa() != null) {

									nim = virtualAccountBankNtt.getSiswa().getNomorInduk();
									nama = virtualAccountBankNtt.getSiswa().getNama();
									hp = virtualAccountBankNtt.getSiswa().getTeleponSiswa();
									email = virtualAccountBankNtt.getSiswa().getAlamatEmail();
								} else if (virtualAccountBankNtt.getCalonSiswa() != null) {

									nim = virtualAccountBankNtt.getCalonSiswa().getNomorInduk();
									nama = virtualAccountBankNtt.getCalonSiswa().getNama();
									hp = virtualAccountBankNtt.getCalonSiswa().getTeleponSiswa();
									email = virtualAccountBankNtt.getCalonSiswa().getAlamatEmail();
								}

								hp = hp.startsWith("08") ? "+62" + hp.substring(1) : hp;
								hp = hp.startsWith("0") ? "+62" + hp.substring(1) : hp;
								hp = !hp.startsWith("+") ? "+62" + hp : hp;

								response.put("nomorHP", hp);

								response.put("email", email);

								response.put("nama", nama + " " + nim);
								response.put("totalNominal", mytotal);

								Map<String, List<Tagihan>> map = VirtualAccountBank.bayarSiswa(virtualAccountBankNtt,
										session, tanggal, bank, inquery, data, false);

								int infoke = 1;
								for (List<Tagihan> tagihans : map.values()) {

									for (Tagihan tagihan : tagihans) {

										JSONObject jsonObjectInfo = new JSONObject();
										jsonObjectInfo.put("label_key", "INFO" + (infoke++));
										jsonObjectInfo.put("label_value",
												tagihan.getItemBiayaSekolah() + " "
														+ (tagihan.getBulan() != null && tagihan.getBulan() > 0
																? " bulan " + tagihan.getBulan()
																: "")
														+ (tagihan.getTahun() != null && tagihan.getTahun() > 0
																? " tahun " + tagihan.getTahun()
																: ""));
										arr_informasi.put(jsonObjectInfo);

										if (inquery) {
											JSONObject bill = new JSONObject();
											bill.put("kode_rincian", tagihan.getItemBiayaSekolah().getKode());
											bill.put("deskripsi", tagihan.getItemBiayaSekolah().getNama());
											bill.put("nominal", tagihan.getNominal().intValue());
											rincian.put(bill);

											if (tagihan.getDenda() > 0.1) {
												bill = new JSONObject();

												bill.put("kode_rincian", "D" + tagihan.getItemBiayaSekolah().getKode());
												bill.put("deskripsi",
														"Denda " + tagihan.getItemBiayaSekolah().getNama());
												bill.put("nominal", tagihan.getDenda().intValue());
												rincian.put(bill);

											}

										}
									}

								}

								if (inquery) {
									response.put("PaymentFlagStatus", "00");
								}

							} else {

								JenisKegiatan jenisKegiatan = virtualAccountBankNtt.getJenisKegiatan();

								if (jenisKegiatan != null && jenisKegiatan.getPrefixKodePembayaran() != null
										&& companyCode != null && !companyCode.trim().isEmpty()
										&& !jenisKegiatan.getPrefixKodePembayaran().equalsIgnoreCase(companyCode)) {
									response.put("rc", "ERR-NOT-FOUND");
									response.put("msg", "Error nomor VA tidak ditemukan");
								} else {

									Mahasiswa mahasiswa = virtualAccountBankNtt.getMahasiswa();
									BiodataCalonMahasiswa biodataCalonMahasiswa = virtualAccountBankNtt
											.getBiodataCalonMahasiswa();

									Integer semester = virtualAccountBankNtt.getSemester();

									nim = mahasiswa == null ? biodataCalonMahasiswa.getNoRegistrasi()
											: mahasiswa.getNim();
									nama = mahasiswa == null ? biodataCalonMahasiswa.getNama() : mahasiswa.getNama();

									String hp = mahasiswa == null ? biodataCalonMahasiswa.getHp() : mahasiswa.getTelp();
									if (hp != null) {
										hp = hp.startsWith("08") ? "+62" + hp.substring(1) : hp;
										hp = hp.startsWith("0") ? "+62" + hp.substring(1) : hp;
										hp = !hp.startsWith("+") ? "+62" + hp : hp;
									}
									if (hp == null) {
										hp = "";
									}
									response.put("nomorHP", hp);

									String email = mahasiswa == null ? biodataCalonMahasiswa.getEmail()
											: mahasiswa.getEmail();
									response.put("email", email);

									response.put("nama", nama + " " + nim);
									response.put("totalNominal", mytotal);

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
									commitTransaksiAman(session, "331");

									List<Long> detailBiayasId = new ArrayList<Long>();
									for (String id : StringUtils.split(virtualAccountBankNtt.getDetailbiaya(), ",")) {
										try {
											detailBiayasId.add(Long.parseLong(id.trim()));
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/BSI.java:337");

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
									System.out.println(
											"cicilanPembayaran total -> " + total + " totalTagihan " + totalTagihan);

									if (!biaya_admin_jangan_dimasukkan_saat_simpan_va) {
										if (virtualAccountBankNtt.getBiayaAdmin() > 0.1) {
											JSONObject bill = new JSONObject();
											bill.put("kode_rincian", "Biaya admin");
											bill.put("deskripsi", "Biaya admin");
											bill.put("nominal", virtualAccountBankNtt.getBiayaAdmin().intValue());
											rincian.put(bill);

										}
									}

									int infoke = 1;
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

													ItemBiaya itemBiaya = pengaturanPembayaranBulanan.getDetailBiaya()
															.getItemBiaya();
													Double subtotal = 0.0;
													try {
														String[] spl = idPemBul.split("-");
														subtotal = Double.parseDouble(spl[spl.length - 1]);
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/BSI.java:393");
													}

													if (itemBiaya.getPenghitungan()
															.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
														subtotal = 0.0 - subtotal;
													}

													JSONObject jsonObjectInfo = new JSONObject();
													jsonObjectInfo.put("label_key", "INFO" + (infoke++));
													jsonObjectInfo.put("label_value",
															itemBiaya.getNama() + " " + " bulan "
																	+ pengaturanPembayaranBulanan.getNamaBulan() + " "
																	+ pengaturanPembayaranBulanan.getDetailBiaya()
																			.getTahunAkademik());
													arr_informasi.put(jsonObjectInfo);

													JSONObject bill = new JSONObject();

													bill.put("kode_rincian", itemBiaya.getKode());
													bill.put("deskripsi", itemBiaya.getNama());
													bill.put("nominal", subtotal.intValue());

													rincian.put(bill);

													if (!inquery) {

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
														cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
														cicilanPembayaran.setTanggal(tanggal);
														cicilanPembayaran.setJenisPembayaran(bankHost == null
																|| bankHost.getJenisPembayaran() == null
																		? ConstantValues.TUNAI
																		: bankHost.getJenisPembayaran());
														cicilanPembayaran.setDenda(0.0);
														cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());

														session.getTransaction().begin();
														if (cicilanPembayaran.getId() == null)
															session.save(cicilanPembayaran);
														else
															Common.refreshUpdate(session, cicilanPembayaran);
														commitTransaksiAman(session, "452");

														JSONObject jsonObjectRinci = new JSONObject();
														jsonObjectRinci.put("nama", pengaturanPembayaranBulanan
																.getDetailBiaya().getItemBiaya().getNama());
														jsonObjectRinci.put("bulan",
																pengaturanPembayaranBulanan.getNamaBulan());
														jsonObjectRinci.put("nominal", pengaturanPembayaranBulanan
																.ambilNominalModifikasi(mahasiswa, semester));

														rincian.put(jsonObjectRinci);
													}
//											}
												}
											} else if (idPemBul != null && idPemBul.startsWith("Bulanan-")) {
												PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) session
														.createCriteria(PengaturanPembayaranBulanan.class)
														.add(Restrictions.idEq(Long.parseLong(idPemBul.split("-")[1])))
														.uniqueResult();

												if (pengaturanPembayaranBulanan != null) {
													String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
															+ virtualAccountBankNtt.getId();

													ItemBiaya itemBiaya = pengaturanPembayaranBulanan.getDetailBiaya()
															.getItemBiaya();

													JSONObject jsonObjectInfo = new JSONObject();
													jsonObjectInfo.put("label_key", "INFO" + (infoke++));
													jsonObjectInfo.put("label_value",
															itemBiaya.getNama() + " " + " bulan "
																	+ pengaturanPembayaranBulanan.getNamaBulan() + " "
																	+ pengaturanPembayaranBulanan.getDetailBiaya()
																			.getTahunAkademik());
													arr_informasi.put(jsonObjectInfo);

													Double subtotal = 0.0;
													try {
														String[] spl = idPemBul.split("-");
														subtotal = Double.parseDouble(spl[spl.length - 1]);
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/BSI.java:492");
													}

													if (itemBiaya.getPenghitungan()
															.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
														subtotal = 0.0 - subtotal;
													}

													if (inquery) {
														JSONObject bill = new JSONObject();
														bill.put("kode_rincian", itemBiaya.getKode());
														bill.put("deskripsi", itemBiaya.getNama());
														bill.put("nominal", subtotal.intValue());

														rincian.put(bill);

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
														cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
														cicilanPembayaran.setTanggal(tanggal);
														cicilanPembayaran.setJenisPembayaran(bankHost == null
																|| bankHost.getJenisPembayaran() == null
																		? ConstantValues.TUNAI
																		: bankHost.getJenisPembayaran());
														cicilanPembayaran.setDenda(0.0);
														cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
														session.getTransaction().begin();
														if (cicilanPembayaran.getId() == null)
															session.save(cicilanPembayaran);
														else
															Common.refreshUpdate(session, cicilanPembayaran);
														commitTransaksiAman(session, "540");

														JSONObject bill = new JSONObject();
														bill.put("kode_rincian", itemBiaya.getKode());
														bill.put("deskripsi", itemBiaya.getNama());
														bill.put("nominal", subtotal.intValue());

														rincian.put(bill);

														JSONObject jsonObjectRinci = new JSONObject();
														jsonObjectRinci.put("nama", pengaturanPembayaranBulanan
																.getDetailBiaya().getItemBiaya().getNama());
														jsonObjectRinci.put("bulan",
																pengaturanPembayaranBulanan.getNamaBulan());
														jsonObjectRinci.put("nominal", subtotal);

														rincian.put(jsonObjectRinci);
//												}
													}
												}
											} else if (idPemBul != null && idPemBul.startsWith("Item-")) {
												ItemBiaya itemBiaya = (ItemBiaya) ConstantValues
														.simpleObject(
																session.createCriteria(ItemBiaya.class)
																		.add(Restrictions.idEq(Long
																				.parseLong(idPemBul.split("-")[1]))),
																ItemBiaya.class);

												if (itemBiaya != null) {
													String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
															+ virtualAccountBankNtt.getId();

													JSONObject jsonObjectInfo = new JSONObject();
													jsonObjectInfo.put("label_key", "INFO" + (infoke++));
													jsonObjectInfo.put("label_value", itemBiaya.getNama() + " "
															+ virtualAccountBankNtt.getTahunAkademik());
													arr_informasi.put(jsonObjectInfo);

													Double subtotal = 0.0;
													try {
														String[] spl = idPemBul.split("-");
														subtotal = Double.parseDouble(spl[2]);
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/BSI.java:582");
													}

													Long detailBiayaId = null;
													try {
														String[] spl = idPemBul.split("-");
														detailBiayaId = Long.parseLong(spl[4]);
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/BSI.java:589");
													}

													if (itemBiaya.getPenghitungan()
															.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
														subtotal = 0.0 - subtotal;
													}

													if (inquery) {
														JSONObject bill = new JSONObject();
														bill.put("kode_rincian", itemBiaya.getKode());
														bill.put("deskripsi", itemBiaya.getNama());
														bill.put("nominal", subtotal.intValue());

														rincian.put(bill);

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
														cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
														cicilanPembayaran.setTanggal(tanggal);
														cicilanPembayaran.setJenisPembayaran(bankHost == null
																|| bankHost.getJenisPembayaran() == null
																		? ConstantValues.TUNAI
																		: bankHost.getJenisPembayaran());
														cicilanPembayaran.setDenda(0.0);
														cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
														session.getTransaction().begin();
														if (cicilanPembayaran.getId() == null)
															session.save(cicilanPembayaran);
														else
															Common.refreshUpdate(session, cicilanPembayaran);
														commitTransaksiAman(session, "638");

														JSONObject bill = new JSONObject();
														bill.put("kode_rincian", itemBiaya.getKode());
														bill.put("deskripsi", itemBiaya.getNama());
														bill.put("nominal", subtotal.intValue());

														rincian.put(bill);

														JSONObject jsonObjectRinci = new JSONObject();
														jsonObjectRinci.put("nama", itemBiaya.getNama());
														jsonObjectRinci.put("bulan", "");
														jsonObjectRinci.put("nominal", subtotal);

														rincian.put(jsonObjectRinci);

													}
												}
											} else if (idPemBul != null && idPemBul.startsWith("Keranjang-")) {
												// Pembayaran Keranjang Belanja (multi jenis / KegiatanTemporary): konversi draf
												// menjadi Kegiatan+Cicilan nyata - pemroses terpusat yang sama dengan Esmartlink.
												if (!inquery) {
													ais.action.ws.util.PembayaranGatewayHelper.prosesSatuTokenKeranjang(session, idPemBul,
															virtualAccountBankNtt, false, bank, bankHost, tanggal, data, null);
												}
											}
										}
									}

									if (inquery) {
										response.put("PaymentFlagStatus", "00");

									} else {
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
										commitTransaksiAman(session, "685");

										VirtualAccountBank.updateVa(virtualAccountBankNtt, tanggal, kegiatan, data,
												bank);

									}
								}
							}
						}

					}

				}

			} catch (Exception e) {
					try {
						if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
							session.getTransaction().rollback();
						}
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/servlet/BSI.java:704");
					}

					response.put("rc", "ERR-DB");
				response.put("msg", "Error saat Update Transaksi");

				h2hStackTrace = ais.action.ws.util.PembayaranGatewayHelper.ambilStackTrace(e);
				Common.tampilErrorJikaAdmin(e);
			} finally {
				Common.closeNativeSessionQuietly(session);
				// JAMINAN: log H2H SELALU dicatat & tercommit walau terjadi error/exception, dan
				// TANPA syarat bankHost (request dari IP tak dikenal pun WAJIB tercatat).
				ais.action.ws.util.PembayaranGatewayHelper.catatLogHostToHost(request, bankHost, data, nim, va, nama,
						response.toString(), nominalP, rincian.toString(), h2hStackTrace);
			}

			}

		if (Common.bolehKonfigurasi("bsi_paksa_gagal", Konfigurasi.TIDAK_AKTIF)) {
			response.put("rc", Common.getKonfigurasi("bsi_paksa_gagal_rc", "ERR-DB").getNilai());
			response.put("msg", Common.getKonfigurasi("bsi_paksa_gagal_msg", "Error saat Update Transaksi").getNilai());
		}

		return response;
	}

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
		String bank = "BSI";
		BankHost bankHost = pembayaranUtil.getBankHost(request.getRemoteAddr(), bank);

		String body;
		try {
			// 05, request tidak diizinkan
			body = BSI.doProses(data, request, bankHost, bank, false).toString();
		} catch (Exception e) {
			JSONObject req = data == null ? null : new JSONObject(data);

			String va = req == null || req.isNull("nomorPembayaran") ? request.getParameter("nomorPembayaran")
					: req.getString("nomorPembayaran");

			body = errorDB(va);
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/BSI.java:760");

		}

		response.setHeader("length", body.length() + "");
		response.setHeader("Content-Type", "application/json");
		PrintWriter writer = response.getWriter();

		writer.write(body);

	}

	public static String errorDB(String va) {
		JSONObject response = new JSONObject();
		try {

			response.put("rc", Common.getKonfigurasi("bsi_paksa_gagal_rc", "ERR-DB").getNilai());
			response.put("msg", Common.getKonfigurasi("bsi_paksa_gagal_msg", "Error saat Update Transaksi").getNilai());
			response.put("nomorPembayaran", va);
			response.put("idPelanggan", va);

			response.put("nama", "");
			response.put("totalNominal", 0.0);

			JSONArray arr_informasi = new JSONArray();
			response.put("informasi", arr_informasi);

			JSONArray rincian = new JSONArray();
			response.put("rincian", rincian);

			String idTagihan = Common.getGeneratedBarCode();

			response.put("idTagihan", idTagihan);
		} catch (Exception e) {

			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/BSI.java:795");

		}
		return response.toString();
	}

	public static String doProses(String data, HttpServletRequest request, BankHost bankHost, String bank, boolean chek)
			throws Exception {
		JSONObject req = data == null ? null : new JSONObject(data);

		String action = req == null || req.isNull("action") ? request.getParameter("action") : req.getString("action");

		String va = req == null || req.isNull("nomorPembayaran") ? request.getParameter("nomorPembayaran")
				: req.getString("nomorPembayaran");

		String companyCode = req == null || req.isNull("kodeBiller") ? request.getParameter("kodeBiller")
				: req.getString("kodeBiller");

		String requestID = req == null || req.isNull("idTransaksi") ? request.getParameter("idTransaksi")
				: req.getString("idTransaksi");

		double nominalP = 0.0;
		try {
			nominalP = req == null || req.isNull("totalNominal") ? 0.0
					: Double.parseDouble(req.get("totalNominal").toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/BSI.java:820");
			// TODO: handle exception
		}

		String tanggalP = req == null || req.isNull("tanggalTransaksi") ? dateFormat.get().format(new Date())
				: req.getString("tanggalTransaksi");

		// 05, request tidak diizinkan

		System.out.println("action " + action);

		String body = BSI.doProcess(nominalP, tanggalP, va, companyCode, requestID, bank, bankHost, request, data, true,
				action != null && action.equalsIgnoreCase("inquiry"), chek).toString();
		return body;
	}

}

