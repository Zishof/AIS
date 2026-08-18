package ais.action.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
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
import ais.ui.util.WaktuUtil;

/**
 * Servlet implementation class CheckISBN
 */
public class MncBank extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public MncBank() {
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

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPut(HttpServletRequest request, HttpServletResponse response)
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
	protected void doTrace(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static String doProcess(Double nominalP, String tanggalP, String va, String bank, BankHost bankHost,
			HttpServletRequest request, String data, boolean chekLagi, boolean inquery, boolean chek) throws Exception {

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("referenceNo", Common.getGeneratedBarCode());
		jsonObject.put("resultCd", "0000");
		jsonObject.put("resultMsg", "SUCCESS");
		jsonObject.put("amt", "0");
		{ // TANPA syarat bankHost: request bank apa pun WAJIB tercatat ke log H2H (lihat finally)
			String nim = "";
			String goodsNm = "";
			String nama = "";
			// Jejak stack trace bila inquiry/pembayaran error; disimpan ke kolom log H2H.
			String h2hStackTrace = null;
			VirtualAccountBank virtualAccountBankNtt = null;
			try {
				Session session = HibernateUtil.getSessionFactory().openSession();
				try {
				virtualAccountBankNtt = VirtualAccountBank.ambilVa(va,  nominalP, bankHost);

				if (virtualAccountBankNtt == null) {
					jsonObject.put("resultCd", "9613");
					jsonObject.put("resultMsg", "kode pembayaran tidak ditemukan");
				} else if (!chek
						&& Common.bolehKonfigurasi("chek_kadaluarsa")
						&& virtualAccountBankNtt != null
						&& virtualAccountBankNtt.getKadaluarsa().before(WaktuUtil.getDate())) {
					jsonObject.put("resultCd", "9612");
					jsonObject.put("resultMsg", "expired");
				} else if (virtualAccountBankNtt != null && !inquery
						&& (nominalP.intValue() != (virtualAccountBankNtt.getBiayaAdmin().intValue()
								+ virtualAccountBankNtt.getTotal().intValue()))) {
					jsonObject.put("resultCd", "9613");
					jsonObject.put("resultMsg", "nominal tidak sesuai");
				} else if (VirtualAccountBank.isSudahTerbayarUntukPayment(virtualAccountBankNtt, inquery, false, chek)) {
					jsonObject.put("resultCd", "9611");
					jsonObject.put("resultMsg", "paid");
				} else if (virtualAccountBankNtt != null && virtualAccountBankNtt.getTotal() > 0.1) {

					Date tanggal = ais.ui.util.WaktuUtil.getDate();
					try {
						tanggal = Common.databaseDateFormat1.get().parse(tanggalP);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/MncBank.java:154");
					}

					if (virtualAccountBankNtt.getSiswa() != null || virtualAccountBankNtt.getCalonSiswa() != null) {

						if (virtualAccountBankNtt.getSiswa() != null) {

							nim = virtualAccountBankNtt.getSiswa().getNomorInduk();
							nama = virtualAccountBankNtt.getSiswa().getNama();

						} else if (virtualAccountBankNtt.getCalonSiswa() != null) {

							nim = virtualAccountBankNtt.getCalonSiswa().getNomorInduk();
							nama = virtualAccountBankNtt.getCalonSiswa().getNama();

						}

						Map<String, List<Tagihan>> map = VirtualAccountBank.bayarSiswa(virtualAccountBankNtt, session,
								tanggal, bank, inquery, data, false);
						goodsNm = "";
						Double amt = 0.0;
						for (List<Tagihan> tagihans : map.values()) {

							for (Tagihan tagihan : tagihans) {

								String ket = tagihan.getNominalBiaya().getItemBiayaSekolah().getNama()
										+ (tagihan.getNominalBiaya().getDibayarSebayak() > 1
												? " (ke " + tagihan.getBayarKe() + ")"
												: "");
								if (tagihan.getBulan() != null && tagihan.getBulan() > 0 && tagihan.getBulan() <= 12) {
									ket += ", Bulan " + Common.BULAN[tagihan.getBulan() - 1];
								}
								if (tagihan.getTahun() != null && tagihan.getTahun() > 1900) {
									ket += ", Tahun " + tagihan.getTahun();
								}
								Double denda = tagihan.getDenda();
								if (denda > 0.01) {
									ket += ", Denda " + Common.numberFormat.get().format(tagihan.getDenda());
								}
								Date tglDeadline = tagihan.getTanggalDeadline();
								if (tglDeadline != null) {
									ket += ", Deadline " + Common.dateFormat4.get().format(tglDeadline);
								}

								ket += (tagihan.getDiskonSiswa() != null ? " - " + tagihan.getDiskonSiswa().getNama()
										: "");

								goodsNm += goodsNm.isEmpty() ? ket : ";" + ket;
								amt += tagihan.getNominal();
							}

						}

						jsonObject.put("amt", amt.intValue() + "");
						jsonObject.put("goodsNm", goodsNm);
						jsonObject.put("referenceNo", nim);
						jsonObject.put("billingNm", nama);
					} else {

						JenisKegiatan jenisKegiatan = virtualAccountBankNtt.getJenisKegiatan();
						Mahasiswa mahasiswa = virtualAccountBankNtt.getMahasiswa();
						BiodataCalonMahasiswa biodataCalonMahasiswa = virtualAccountBankNtt.getBiodataCalonMahasiswa();

						Integer semester = virtualAccountBankNtt.getSemester();

						nim = mahasiswa == null ? biodataCalonMahasiswa.getNoRegistrasi() : mahasiswa.getNim();
						nama = mahasiswa == null ? biodataCalonMahasiswa.getNama() : mahasiswa.getNama();

						Kegiatan kegiatan = (Kegiatan) (virtualAccountBankNtt.getKegiatan() == null ? null
								: session.createCriteria(Kegiatan.class)
										.add(Restrictions.idEq(virtualAccountBankNtt.getKegiatan())).uniqueResult());

						if (kegiatan == null || kegiatan.getId() == null) {

							kegiatan = (Kegiatan) session.createCriteria(Kegiatan.class)

									.addOrder(Order.asc("id"))

									.add(biodataCalonMahasiswa != null
											? Restrictions.eq("calonMahasiswa", biodataCalonMahasiswa)
											: Restrictions.eq("mahasiswa", mahasiswa))
									.add(Restrictions.eq("jenisKegiatan", virtualAccountBankNtt.getJenisKegiatan()))
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
						for (String id : StringUtils.split(virtualAccountBankNtt.getDetailbiaya(), ",")) {
							try {
								detailBiayasId.add(Long.parseLong(id.trim()));
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/MncBank.java:267");

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

						if (inquery) {

							Double total = 0.0;
							if (virtualAccountBankNtt.getCicilan() != null
									&& !virtualAccountBankNtt.getCicilan().isEmpty()) {
								for (String idPemBul : StringUtils.split(virtualAccountBankNtt.getCicilan(), ",")) {
									if (Common.isNumber(idPemBul)) {
										PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) session
												.createCriteria(PengaturanPembayaranBulanan.class)
												.add(Restrictions.idEq(Long.parseLong(idPemBul))).uniqueResult();

										if (pengaturanPembayaranBulanan != null) {

											Double subtotal = pengaturanPembayaranBulanan
													.ambilNominalModifikasi(mahasiswa, semester);
											total += subtotal;

											String t = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya()
													.getNama() + " bulan " + pengaturanPembayaranBulanan.getNamaBulan();
											goodsNm += goodsNm.isEmpty() ? t : ";" + t;

										}
									} else if (idPemBul != null && idPemBul.startsWith("Bulanan-")) {
										PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) session
												.createCriteria(PengaturanPembayaranBulanan.class)
												.add(Restrictions.idEq(Long.parseLong(idPemBul.split("-")[1])))
												.uniqueResult();

										if (pengaturanPembayaranBulanan != null) {

											Double subtotal = 0.0;
											try {
												String[] spl = idPemBul.split("-");
												subtotal = Double.parseDouble(spl[spl.length - 1]);
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/MncBank.java:319");
											}

											if (pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya()
													.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
												subtotal = 0.0 - subtotal;
											}
											total += subtotal;
											String t = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya()
													.getNama() + " bulan " + pengaturanPembayaranBulanan.getNamaBulan();
											goodsNm += goodsNm.isEmpty() ? t : ";" + t;
										}
									} else if (idPemBul != null && idPemBul.startsWith("Item-")) {
										ItemBiaya itemBiaya = (ItemBiaya) session.createCriteria(ItemBiaya.class)
												.add(Restrictions.idEq(Long.parseLong(idPemBul.split("-")[1])))
												.uniqueResult();

										if (itemBiaya != null) {

											Double subtotal = 0.0;
											try {
												String[] spl = idPemBul.split("-");
												subtotal = Double.parseDouble(spl[2]);
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/MncBank.java:342");
											}

											@SuppressWarnings("unused")
											Integer bayarke = 1;
											try {
												String[] spl = idPemBul.split("-");
												bayarke = Integer.parseInt(spl[3]);
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/MncBank.java:350");
											}

											if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
												subtotal = 0.0 - subtotal;
											}
											total += subtotal;

											String t = itemBiaya.getNama();
											goodsNm += goodsNm.isEmpty() ? t : ";" + t;
										}
									}
								}

							}

							jsonObject.put("amt", total.intValue() + "");
							jsonObject.put("goodsNm", goodsNm);
							jsonObject.put("referenceNo", nim);
							jsonObject.put("billingNm", nama);
						}

						else {

//							Double[] totalCicilan = kegiatan.hitungTotalDanDendaFromCicilan();
//							Double total = totalCicilan[0];
//							Double totalTagihan = kegiatan.getAmount() + kegiatan.getAmountTerhutang();
//							System.out.println("cicilanPembayaran total -> " + total + " totalTagihan " + totalTagihan);

							if (virtualAccountBankNtt.getCicilan() != null
									&& !virtualAccountBankNtt.getCicilan().isEmpty()) {
								for (String idPemBul : StringUtils.split(virtualAccountBankNtt.getCicilan(), ",")) {
									if (Common.isNumber(idPemBul)) {
										PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) session
												.createCriteria(PengaturanPembayaranBulanan.class)
												.add(Restrictions.idEq(Long.parseLong(idPemBul))).uniqueResult();

										if (pengaturanPembayaranBulanan != null) {
											String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
													+ virtualAccountBankNtt.getId();

											ItemBiaya itemBiaya = pengaturanPembayaranBulanan.getDetailBiaya()
													.getItemBiaya();

											Double subtotal = 0.0;
											try {
												String[] spl = idPemBul.split("-");
												subtotal = Double.parseDouble(spl[spl.length - 1]);
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/MncBank.java:398");
											}

											if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
												subtotal = 0.0 - subtotal;
											}

											CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
													.createCriteria(CicilanPembayaran.class)
													.add(Restrictions.eq("ref", ref)).setMaxResults(1).uniqueResult();

											if (cicilanPembayaran == null) {
												cicilanPembayaran = new CicilanPembayaran(
														pengaturanPembayaranBulanan.getDetailBiaya());
											}
											cicilanPembayaran.setRef(ref);
											cicilanPembayaran.setValidator(bank);
											cicilanPembayaran.setKegiatan(kegiatan);
											cicilanPembayaran.setItemBiaya(
													pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya());
											cicilanPembayaran
													.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
											cicilanPembayaran.setRefVa(virtualAccountBankNtt.getId());
											cicilanPembayaran.setNilai(subtotal);
											cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
											cicilanPembayaran.setTanggal(tanggal);
											cicilanPembayaran.setJenisPembayaran(
													bankHost == null || bankHost.getJenisPembayaran() == null
															? ConstantValues.TUNAI
															: bankHost.getJenisPembayaran());
											cicilanPembayaran.setDenda(0.0);
											cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());

											session.getTransaction().begin();
											if(cicilanPembayaran.getId()==null)session.save(cicilanPembayaran);else Common.refreshUpdate(session, cicilanPembayaran);
											session.getTransaction().commit();

											String t = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya()
													.getNama() + " bulan " + pengaturanPembayaranBulanan.getNamaBulan();
											goodsNm += goodsNm.isEmpty() ? t : ";" + t;

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
											Double subtotal = 0.0;
											try {
												String[] spl = idPemBul.split("-");
												subtotal = Double.parseDouble(spl[spl.length - 1]);
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/MncBank.java:457");
											}

											if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
												subtotal = 0.0 - subtotal;
											}

											CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
													.createCriteria(CicilanPembayaran.class)
													.add(Restrictions.eq("ref", ref)).setMaxResults(1).uniqueResult();

											if (cicilanPembayaran == null) {
												cicilanPembayaran = new CicilanPembayaran(
														pengaturanPembayaranBulanan.getDetailBiaya());
											}
											cicilanPembayaran.setRef(ref);
											cicilanPembayaran.setValidator(bank);
											cicilanPembayaran.setKegiatan(kegiatan);
											cicilanPembayaran.setItemBiaya(itemBiaya);
											cicilanPembayaran
													.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
											cicilanPembayaran.setRefVa(virtualAccountBankNtt.getId());

											cicilanPembayaran.setNilai(subtotal);
											cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
											cicilanPembayaran.setTanggal(tanggal);
											cicilanPembayaran.setJenisPembayaran(
													bankHost == null || bankHost.getJenisPembayaran() == null
															? ConstantValues.TUNAI
															: bankHost.getJenisPembayaran());
											cicilanPembayaran.setDenda(0.0);
											cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
											session.getTransaction().begin();
											if(cicilanPembayaran.getId()==null)session.save(cicilanPembayaran);else Common.refreshUpdate(session, cicilanPembayaran);
											session.getTransaction().commit();

											String t = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya()
													.getNama() + " bulan " + pengaturanPembayaranBulanan.getNamaBulan();
											goodsNm += goodsNm.isEmpty() ? t : ";" + t;

										}

									} else if (idPemBul != null && idPemBul.startsWith("Item-")) {
										ItemBiaya itemBiaya = (ItemBiaya) ConstantValues
												.simpleObject(
														session.createCriteria(ItemBiaya.class)
																.add(Restrictions
																		.idEq(Long.parseLong(idPemBul.split("-")[1]))),
														ItemBiaya.class);

										if (itemBiaya != null) {
											String ref = "ntt-" + kegiatan.getId() + "-" + idPemBul + "-"
													+ virtualAccountBankNtt.getId();
											Double subtotal = 0.0;
											try {
												String[] spl = idPemBul.split("-");
												subtotal = Double.parseDouble(spl[2]);
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/MncBank.java:514");
											}

											Long detailBiayaId = null;
											try {
												String[] spl = idPemBul.split("-");
												detailBiayaId = Long.parseLong(spl[4]);
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/MncBank.java:521");
											}

											if (itemBiaya.getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
												subtotal = 0.0 - subtotal;
											}

											CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
													.createCriteria(CicilanPembayaran.class)
													.add(Restrictions.eq("ref", ref)).setMaxResults(1).uniqueResult();

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
											cicilanPembayaran.setJenisPembayaran(
													bankHost == null || bankHost.getJenisPembayaran() == null
															? ConstantValues.TUNAI
															: bankHost.getJenisPembayaran());
											cicilanPembayaran.setDenda(0.0);
											cicilanPembayaran.setNilaiAsli(cicilanPembayaran.getNilai());
											session.getTransaction().begin();
											if(cicilanPembayaran.getId()==null)session.save(cicilanPembayaran);else Common.refreshUpdate(session, cicilanPembayaran);
											session.getTransaction().commit();

											String t = itemBiaya.getNama();
											goodsNm += goodsNm.isEmpty() ? t : ";" + t;
										}

									} else if (idPemBul != null && idPemBul.startsWith("Keranjang-")) {
										// Pembayaran Keranjang Belanja (multi jenis / KegiatanTemporary): konversi draf
										// menjadi Kegiatan+Cicilan nyata â€” pemroses terpusat yang sama dengan Esmartlink.
										ais.action.ws.util.PembayaranGatewayHelper.prosesSatuTokenKeranjang(session, idPemBul,
												virtualAccountBankNtt, false, bank, bankHost, tanggal, data, null);
									}
								}
							}

							Double[] d = PembayaranUtil.getInstance().getTotalDanDendaFromCicilan(session, kegiatan);
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

							VirtualAccountBank.updateVa(virtualAccountBankNtt, tanggal, kegiatan, data, bank);

						}
					}

				}

				} finally {
					if (session != null) {
						try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/MncBank.java:594");}
						try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/MncBank.java:595");}
						try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/MncBank.java:596");}
					}
				}
			} catch (Exception e) {
				HibernateUtil.rollbackTransaction();

				h2hStackTrace = ais.action.ws.util.PembayaranGatewayHelper.ambilStackTrace(e);
				Common.tampilErrorJikaAdmin(e);
			} finally {
				// JAMINAN: log H2H SELALU dicatat & tercommit walau terjadi error/exception
				// (helper: session terdedikasi + commit + retry, tak pernah gagal).
				try {
					jsonObject.put("goodsNm", goodsNm);
				} catch (Exception eGoods) { ais.common.ErrorAuditUtil.record(eGoods, "auto-audit(empty-catch) src/ais/action/servlet/MncBank.java:609");
				}
				ais.action.ws.util.PembayaranGatewayHelper.catatLogHostToHost(request, bankHost, data, nim, va, nama,
						jsonObject.toString(), nominalP, goodsNm, h2hStackTrace);
			}
		}
		return jsonObject.toString();
	}

	public static String doProses(String data, HttpServletRequest request, BankHost bankHost, String bank, boolean chek)
			throws Exception {
		JSONObject req = new JSONObject(data);

		String va = req.get("prefix") + "" + req.get("customerId") + "";

		double nominalP = 0.0;
		try {
			nominalP = Double.parseDouble(req.get("amt") + "");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/MncBank.java:627");
			// TODO: handle exception
		}

		String tanggalP = Common.databaseDateFormat1.get().format(WaktuUtil.getDate());

		String body;
		try {
			body = MncBank
					.doProcess(nominalP, tanggalP, va, bank, bankHost, request, data, true, req.isNull("amt"), chek)
					.toString();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/MncBank.java:639");
			body = "ERROR";
		}

		return body;
	}

	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {

		Enumeration<String> headerNames = request.getHeaderNames();

		while (headerNames.hasMoreElements()) {

			String headerName = headerNames.nextElement();

			Enumeration<String> headers = request.getHeaders(headerName);
			while (headers.hasMoreElements()) {
				String headerValue = headers.nextElement();
				System.out.println("==> headerName => " + headerName + " headerValue " + headerValue);
			}

		}

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

		String bank = "MNC Bank";
		BankHost bankHost = pembayaranUtil.getBankHost(request.getRemoteAddr(), bank);

		String body = data == null || data.isEmpty() ? "" : doProses(data, request, bankHost, bank, false);

		response.setHeader("length", body.length() + "");
		response.setHeader("Content-Type", "application/json");
		PrintWriter writer = response.getWriter();

		writer.write(body);

	}

}

