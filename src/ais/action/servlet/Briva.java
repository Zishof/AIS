package ais.action.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
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
import ais.database.model.DetailKegiatan;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.LogHostToHost;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.VirtualAccountBank;
import ais.ui.util.WaktuUtil;

/**
 * Servlet implementation class CheckISBN
 */
public class Briva extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	public static DateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Briva() {
		super();
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
	public static JSONObject doProcess(double nominalP, String tanggalP, String va, String bank, BankHost bankHost,
			HttpServletRequest request, String data, boolean chekLagi, boolean inquery, boolean reversal, boolean chek,
			JSONObject virtualAccountData) throws Exception {

		System.out.println("chekLagi -> " + chekLagi + ", inquery -> " + inquery + ", reversal -> " + reversal
				+ ", chek -> " + chek);

		JSONObject response = new JSONObject(data);
		response.put("responseCode", "2003400");
		response.put("responseMessage", "Successful");

		response.put("virtualAccountData", virtualAccountData);

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
					response = new JSONObject();
					response.put("responseCode", "4003400");
					response.put("statusDescription", "Bad Request");
				}

				else if (!reversal && !chek && virtualAccountBankNtt != null
						&& virtualAccountBankNtt.getKadaluarsa().before(WaktuUtil.getDate())) {
					response = new JSONObject();
					response.put("responseCode", "4003400");
					response.put("statusDescription", "Bad Request");

				} else {

					if (reversal && virtualAccountBankNtt != null && virtualAccountBankNtt.getTotal() > 0.1
							&& virtualAccountBankNtt.getKegiatan() == null
							&& virtualAccountBankNtt.getPembayaran() == null) {
						response = new JSONObject();
						response.put("responseCode", "4003400");
						response.put("statusDescription", "Bad Request");
					} else

					if (VirtualAccountBank.isSudahTerbayarUntukPayment(virtualAccountBankNtt, inquery, reversal, chek)) {
						response = new JSONObject();
						response.put("responseCode", "4003400");
						response.put("statusDescription", "Bad Request");
					}

					else if (virtualAccountBankNtt != null && virtualAccountBankNtt.getTotal() > 0.1) {

						String TahunID = "";
						try {
							TahunID = virtualAccountBankNtt.getTahunAkademik().split("/")[0];
							response.put("TahunID", TahunID);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Briva.java:158");
							// TODO: handle exception
						}

						Session session = HibernateUtil.getSessionFactory().openSession();
						try {

							Double nominal = nominalP;

							if (!inquery && nominal.intValue() != virtualAccountBankNtt.totalBiaya()) {
								response = new JSONObject();
								response.put("responseCode", "4003400");
								response.put("statusDescription", "Bad Request");
							} else {

								Date tanggal = ais.ui.util.WaktuUtil.getDate();
								try {
									tanggal = dateFormat1.parse(tanggalP);
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Briva.java:177");
								}

								if (virtualAccountBankNtt.getSiswa() != null
										|| virtualAccountBankNtt.getCalonSiswa() != null) {

									if (virtualAccountBankNtt.getSiswa() != null) {
										nim = virtualAccountBankNtt.getSiswa().getNomorInduk();
										nama = virtualAccountBankNtt.getSiswa().getNama();

									} else if (virtualAccountBankNtt.getCalonSiswa() != null) {
										nim = virtualAccountBankNtt.getCalonSiswa().getNomorInduk();
										nama = virtualAccountBankNtt.getCalonSiswa().getNama();

									}

									VirtualAccountBank.bayarSiswa(virtualAccountBankNtt, session, tanggal, bank,
											inquery, data, false);

								} else {

									JenisKegiatan jenisKegiatan = virtualAccountBankNtt.getJenisKegiatan();
									Mahasiswa mahasiswa = virtualAccountBankNtt.getMahasiswa();
									BiodataCalonMahasiswa biodataCalonMahasiswa = virtualAccountBankNtt
											.getBiodataCalonMahasiswa();

									Integer semester = virtualAccountBankNtt.getSemester();

									nim = mahasiswa == null ? biodataCalonMahasiswa.getNoRegistrasi()
											: mahasiswa.getNim();
									nama = mahasiswa == null ? biodataCalonMahasiswa.getNama() : mahasiswa.getNama();

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
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Briva.java:261");

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
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Briva.java:339");

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
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Briva.java:384");
														}

														if (itemBiaya.getPenghitungan()
																.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
															subtotal = 0.0 - subtotal;
														}

														if (inquery) {

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
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Briva.java:461");
														}

														if (itemBiaya.getPenghitungan()
																.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
															subtotal = 0.0 - subtotal;
														}

														if (inquery) {

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
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Briva.java:533");
														}

														Long detailBiayaId = null;
														try {
															String[] spl = idPemBul.split("-");
															detailBiayaId = Long.parseLong(spl[4]);
														} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Briva.java:540");
														}

														if (itemBiaya.getPenghitungan()
																.equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
															subtotal = 0.0 - subtotal;
														}

														if (inquery) {

														} else {

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
													// menjadi Kegiatan+Cicilan nyata - pemroses terpusat yang sama dengan Esmartlink.
													if (!inquery && !reversal) {
														ais.action.ws.util.PembayaranGatewayHelper.prosesSatuTokenKeranjang(session, idPemBul,
																virtualAccountBankNtt, false, bank, bankHost, tanggal, data, null);
													}
												}
											}
										}

										if (reversal) {
											response = new JSONObject();
											response.put("errorCode", "00");
											response.put("statusDescription", "Success");
										}

										else if (inquery) {

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

											VirtualAccountBank.updateVa(virtualAccountBankNtt, tanggal, kegiatan, data,
													bank);

										}

									}
								}
							}
						} finally {
							if (session != null) {
								try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Briva.java:644");}
								try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Briva.java:645");}
								try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Briva.java:646");}
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

	private static List<String> accessTokens = new ArrayList<String>();

	public static String doProses(String data, HttpServletRequest request, BankHost bankHost, String bank, boolean chek)
			throws Exception {
		JSONObject req = data == null ? null : new JSONObject(data);

		String va = req == null || req.isNull("virtualAccountNo")
				? (request == null ? "" : request.getParameter("virtualAccountNo"))
				: req.getString("virtualAccountNo").trim();

		double nominalP = 0.0;
		try {
			nominalP = req == null || req.isNull("additionalInfo") ? 0.0
					: Double.parseDouble(req.getJSONObject("additionalInfo").get("paymentAmount") + "");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/Briva.java:689");
			// TODO: handle exception
		}

		boolean reversal = false;

		String body;
		try {
			// 05, request tidak diizinkan
			String grantType = req == null || req.isNull("grantType") ? null : req.get("grantType").toString();

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

			if (grantType != null) {

				String strISOTimeStamp = jsonObjectHeader.isNull("X-Timestamp") ? null
						: jsonObjectHeader.get("X-Timestamp").toString();

				if (!jsonObjectHeader.isNull("X-TIMESTAMP")) {
					strISOTimeStamp = jsonObjectHeader.get("X-TIMESTAMP").toString();
				}

				String strToSign = strClientId + "|" + strISOTimeStamp;
				String strPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0zd5K3uxlMHUdsWYalyxwefDycD8DYdiRAGByLcd6Zlgudt1bA3HjuFoE9AeSTxx+ggN3ucr21XbfuVMV0QnuNr6/d/C1U2FRE3MS4Ma1ybT0dYOb5Y6ZkwlMtHJ+3CD7Z33INEQorcd+8pBB/p+0f3Z0lScpQebN+njiWcHG4SoN9B1Tdy3yJFFscAeBiSf8aNvjEoAQM50sM9OHAUz33LCUfu16mu22a+9TkH/1Y5B65kHsOQNMDdYVgdEij4Ayvue2TnUj7asaOPlLvDAIplJBzdLQND6nLuWgCOhDUPcClqY2I6BhsIp6eGpvvQEHGCDmgdzDZcVhVC3lUUyFwIDAQAB";

				strPublicKey = Common.getKonfigurasi("strPublicKey_bri", strPublicKey).getNilai();

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

					int expiresIn = 1000 * 15;

					String token = UUID.randomUUID().toString();
					accessTokens.add(token);
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("accessToken", token);
					jsonObject.put("tokenType", "BearerToken");
					jsonObject.put("expiresIn", expiresIn);

					body = jsonObject.toString();
				} else {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("responseCode", "4017300");
					jsonObject.put("responseMessage", "Unathorized. [Signature]");

					body = jsonObject.toString();
				}
			} else {

				JSONObject virtualAccountData = new JSONObject();

				virtualAccountData.put("partnerServiceId", req.get("partnerServiceId"));
				virtualAccountData.put("customerNo", req.get("customerNo"));
				virtualAccountData.put("virtualAccountNo", req.get("virtualAccountNo"));
				virtualAccountData.put("paymentRequestId", req.get("paymentRequestId"));
				virtualAccountData.put("trxDateTime", req.get("trxDateTime"));
				virtualAccountData.put("paymentStatus", "Success");

				String tanggalP = (req.get("trxDateTime") + "").replaceAll("\\+07:00", "");

				System.out.println("==> tanggalP " + tanggalP);

				body = Briva.doProcess(nominalP, tanggalP, va, bank, bankHost, request, data, true, false, reversal,
						chek, virtualAccountData).toString();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Briva.java:790");
			body = errorDb(req.isNull("amount"), data);

		}

		if (Common.bolehKonfigurasi("otto_va_sleep", Konfigurasi.TIDAK_AKTIF)) {
			Thread.sleep(3 * 1000);
			body = timeoutDb(req.isNull("amount"), data);
		}

		System.out.println("response->" + body);

		return body;
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

		String bank = "Briva";
		BankHost bankHost = pembayaranUtil.getBankHost(request.getRemoteAddr(), bank);

		String body = doProses(data, request, bankHost, bank, true);

		response.setHeader("length", body.length() + "");
		response.setHeader("Content-Type", "application/json");
		PrintWriter writer = response.getWriter();

		writer.write(body);

	}

	private static String timeoutDb(boolean inquery, String data) throws Exception {

		JSONObject jsonObjectResponse = new JSONObject(data);
		jsonObjectResponse.put("errorCode", "68");
		jsonObjectResponse.put("statusDescription", "Connection Timeout");
		return jsonObjectResponse.toString();
	}

	private static String errorDb(boolean inquery, String data) throws Exception {

		JSONObject jsonObjectResponse = new JSONObject(data);
		jsonObjectResponse.put("errorCode", "91");
		jsonObjectResponse.put("statusDescription", "Link Down");
		return jsonObjectResponse.toString();
	}
}

