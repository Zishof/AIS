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
 * Servlet implementation class Va
 */
public class Va extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	public Va() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public static String doProses(String data, HttpServletRequest request, BankHost bankHost) throws Exception {
		return doProses(data, request, bankHost, false);
	}

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

	private static String getSafeParam(JSONObject req, HttpServletRequest request, String key) {
		if (req != null && !req.isNull(key)) {
			return req.optString(key, "");
		}
		return request != null ? request.getParameter(key) : "";
	}

	private static String getSafeFakultas(Mahasiswa m) {
		if (m != null && m.getJurusan() != null && m.getJurusan().getFakultas() != null)
			return m.getJurusan().getFakultas().getNama();
		return "";
	}

	private static String getSafeProdi(Mahasiswa m) {
		if (m != null && m.getJurusan() != null)
			return m.getJurusan().getNama();
		return "";
	}

	private static String getSafeFakultasLulus(BiodataCalonMahasiswa cm) {
		if (cm != null && cm.getProdiLulus() != null && cm.getProdiLulus().getFakultas() != null)
			return cm.getProdiLulus().getFakultas().getNama();
		return "";
	}

	private static String getSafeProdiLulus(BiodataCalonMahasiswa cm) {
		if (cm != null && cm.getProdiLulus() != null)
			return cm.getProdiLulus().getNama();
		return "";
	}

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

	private static boolean isExpired(VirtualAccountBank vaBank) {
		boolean checkAktif = Common.bolehKonfigurasi("chek_kadaluarsa");
		return checkAktif && vaBank != null && vaBank.getKadaluarsa() != null
				&& vaBank.getKadaluarsa().before(WaktuUtil.getDate());
	}

	private static JSONObject generateExpiredResponse() throws Exception {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("status", "05");
		jsonObject.put("description", "Pembayaran telah kadaluarsa");
		return jsonObject;
	}

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
				nilaiBiayaHarusDiBayars += detailBiaya.hitungTotalKegiatan(kegiatan, session);
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
						Double subtotal = detailBiaya.hitungTotalKegiatan(kegiatan, session);
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

			nilaiBiayaHarusDiBayars += biaya;
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
