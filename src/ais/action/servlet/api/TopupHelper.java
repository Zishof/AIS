package ais.action.servlet.api;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Rows;

import ais.action.master.helper.virtualaccount.DownloadTagihanAnggotaKoperasiBankOnline;
import ais.action.master.helper.virtualaccount.DownloadTagihanMahasiswaBankBtn;
import ais.action.master.helper.virtualaccount.DownloadTagihanMahasiswaBankOnline;
import ais.action.master.helper.virtualaccount.DownloadTagihanSiswaBankOnline;
import ais.action.ws.util.PembayaranUtil;
import ais.common.BniCommon;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankHost;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.VirtualAccountBank;
import ais.database.model.asset.Lokasi;
import ais.database.model.bni.BniRequest;
import ais.database.model.inventory.Pembelian;
import ais.database.model.inventory.Toko;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.koperasi.CaraPembayaranKoperasi;
import ais.database.model.koperasi.DraftPembelianAnggotaKoperasi;
import ais.database.model.koperasi.KodePembayaranOnline;
import ais.database.model.koperasi.PembelianAnggotaKoperasi;
import ais.database.model.sekolah.AkunPembayaranSiswa;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.WaktuUtil;

public class TopupHelper {

	// Fungsi pembantu agar tidak ada try-catch berulang yang kosong
	private static Double parseDoubleSafe(String value, Double fallback) {
		if (value == null || value.trim().isEmpty()) {
			return fallback;
		}
		try {
			return Double.parseDouble(value.trim());
		} catch (Exception e) {
			return fallback;
		}
	}

	// Fungsi pembantu untuk menutup session di blok finally
	private static void closeSessionSafely(Session session) {
		if (session != null && session.isOpen()) {
			try {
				try { session.clear(); } catch (Exception ignoreClear) { ais.common.ErrorAuditUtil.record(ignoreClear, "auto-audit(empty-catch) src/ais/action/servlet/api/TopupHelper.java:73");}
				session.disconnect();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/TopupHelper.java:75");
			}
			try {
				session.close();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/TopupHelper.java:79");
			}
		}
		try {
			HibernateUtil.closeSession();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/TopupHelper.java:84");
		}
	}

	public static JSONObject topup(JSONObject request, HttpServletRequest httpServletRequest) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, httpServletRequest);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
				return jsonObject;
			}

			if (tbmuser.getSiswa() != null || tbmuser.getCalonSiswa() != null) {
				jsonObject = topupSiswa(request, httpServletRequest);
			} else if (tbmuser.getMahasiswa() != null) {
				jsonObject = topup_mahasiswa(request, httpServletRequest);
			} else if (tbmuser.getAnggotaKoperasi() != null) {
				jsonObject = topupAnggotaKoperasi(request, httpServletRequest, tbmuser);
			} else {
				jsonObject.put("status", "01");
				jsonObject.put("description", "Pengambilan data gagal, tipe user tidak dikenali");
			}
		} catch (Exception e) {
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", Common.tampilErrorJikaAdmin(e));
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/TopupHelper.java:113");
			}
		}
		return jsonObject;
	}

	public static JSONObject topup(JSONObject request, HttpServletRequest httpServletRequest, Tbmuser tbmuser) {
		JSONObject jsonObject = new JSONObject();
		try {

			if (tbmuser.getSiswa() != null || tbmuser.getCalonSiswa() != null) {
				jsonObject = topupSiswa(request, httpServletRequest, tbmuser);
			} else if (tbmuser.getMahasiswa() != null) {
				jsonObject = topup_mahasiswa(request, httpServletRequest, tbmuser);
			} else if (tbmuser.getAnggotaKoperasi() != null) {
				jsonObject = topupAnggotaKoperasi(request, httpServletRequest, tbmuser);
			} else {
				jsonObject.put("status", "01");
				jsonObject.put("description", "Pengambilan data gagal, tipe user tidak dikenali");
			}
		} catch (Exception e) {
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", Common.tampilErrorJikaAdmin(e));
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/TopupHelper.java:138");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject riwayatPembelian(JSONObject request, HttpServletRequest httpServletRequest) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, httpServletRequest);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
				return jsonObject;
			}

			Integer max = request.isNull("max") ? 5 : Integer.parseInt((request.get("max") + "").trim());
			Integer halaman = request.isNull("halaman") ? 0 : Integer.parseInt((request.get("halaman") + "").trim());

			String cari = request.isNull("cari") ? "" : (request.get("cari") + "").trim();

			Session session = HibernateUtil.openSession();
			try {
			try {
				Criterion criterion = Restrictions.sqlRestriction("false");

				if (tbmuser.getSiswa() != null) {
					criterion = Restrictions.or(criterion, Restrictions.eq("siswa", tbmuser.getSiswa()));
				} else if (tbmuser.getCalonSiswa() != null) {
					criterion = Restrictions.or(criterion, Restrictions.eq("calonSiswa", tbmuser.getCalonSiswa()));
				} else if (tbmuser.getBiodataCalonMahasiswa() != null) {
					criterion = Restrictions.or(criterion,
							Restrictions.eq("biodataCalonMahasiswa", tbmuser.getBiodataCalonMahasiswa()));
				} else if (tbmuser.getMahasiswa() != null) {
					criterion = Restrictions.or(criterion, Restrictions.eq("mahasiswa", tbmuser.getMahasiswa()));
				} else {
					criterion = Restrictions.or(criterion, Restrictions.eq("tbmuser", tbmuser));
				}

				if (!cari.isEmpty()) {
					criterion = Restrictions.and(criterion, Restrictions.ilike("nama", cari, MatchMode.ANYWHERE));
				}

				Criteria cCount = session.createCriteria(Pembelian.class).add(criterion);
				Number countData = (Number) cCount.setProjection(Projections.rowCount()).uniqueResult();

				List<Pembelian> pembelians = session.createCriteria(Pembelian.class).setMaxResults(max)
						.setFirstResult(halaman * max).add(criterion).addOrder(Order.desc("id")).list();
				JSONArray arrayTransaksi = new JSONArray();
				for (Pembelian pembelian : pembelians) {
					JSONObject data = new JSONObject();
					Common.insertProperty(Pembelian.class, pembelian, data, "", 1, "siswa", "calonSiswa", "mahasiswa",
							"biodataCalonMahasiswa", "pembelianAnggotaKoperasi", "tbmuser");
					arrayTransaksi.put(data);
				}
				pembelians.clear();
				pembelians = null;
				jsonObject.put("count", countData.intValue());
				jsonObject.put("data", arrayTransaksi);
				jsonObject.put("status", "00");
				jsonObject.put("description", "Pengambilan data sukses");

			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/TopupHelper.java:202");
			} finally {
				closeSessionSafely(session);
			}

			} finally {
				HibernateUtil.closeSessionQuietly(session);
			}
		} catch (Exception e) {
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", Common.tampilErrorJikaAdmin(e));
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/TopupHelper.java:215");
			}
		}
		return jsonObject;
	}

	public static JSONObject checkBayar(JSONObject request, HttpServletRequest httpServletRequest) {
		JSONObject jsonObject = new JSONObject();
		try {

			if (!request.isNull("kodeUnik")) {

				String kodeToko = !request.isNull("kodeToko") ? (request.getString("kodeToko")).trim() : "";
				String kodeUnik = !request.isNull("kodeUnik") ? (request.getString("kodeUnik")).trim() : "";
				String waktu = !request.isNull("waktu") ? (request.getString("waktu")).trim() : "";
				Date currentWaktu = WaktuUtil.getDate();
				try {
					if (waktu != null && !waktu.trim().isEmpty()) {
						currentWaktu = Common.dateFormat3.get().parse(waktu);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/TopupHelper.java:235");
					// TODO: handle exception
				}
				JSONArray transaksi = !request.isNull("transaksi") ? request.getJSONArray("transaksi") : null;

				System.out.println("kodeToko -> " + kodeToko + ", kodeUnik -> " + kodeUnik + " transaksi " + transaksi);

				if (!kodeToko.trim().isEmpty()) {
					if (!kodeUnik.isEmpty()) {
						if (kodeUnik.length() >= 50) {

							if (transaksi != null && transaksi.length() > 0) {

								Toko toko = null;

								Session session = HibernateUtil.openSession();
								try {
								try {
									toko = (Toko) ConstantValues.simpleObject(
											session.createCriteria(Toko.class).add(Restrictions.eq("kode", kodeToko)),
											Toko.class);
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/TopupHelper.java:257");
								} finally {
									closeSessionSafely(session);
								}

								if (toko != null) {

									HibernateUtil.closeSessionQuietly(session); // tutup session lama sebelum buka ulang (konversi openSession)
									session = HibernateUtil.openSession();
									try {
										KodePembayaranOnline kodePembayaranOnline = (KodePembayaranOnline) session
												.createCriteria(KodePembayaranOnline.class)
												.add(Restrictions.eq("kode", kodeUnik)).uniqueResult();

										if (kodePembayaranOnline != null) {

											session.createSQLQuery(
													"delete from koperasi.pembelian where kode_pembayaran_online="
															+ kodePembayaranOnline.getId())
													.executeUpdate();

											session.createSQLQuery(
													"delete from koperasi.pembelian_anggota_koperasi where kode_pembayaran_online="
															+ kodePembayaranOnline.getId())
													.executeUpdate();

											CaraPembayaranKoperasi caraPembayaranKoperasiOnline = (CaraPembayaranKoperasi) ConstantValues
													.simpleObject(session.createCriteria(CaraPembayaranKoperasi.class)
															.add(Restrictions.eq("nama", "Online (Topup)"))
															.setMaxResults(1), CaraPembayaranKoperasi.class);
											if (caraPembayaranKoperasiOnline == null) {
												caraPembayaranKoperasiOnline = new CaraPembayaranKoperasi();
												caraPembayaranKoperasiOnline.setNama("Online (Topup)");
												caraPembayaranKoperasiOnline.setKode("Topup");
												session.getTransaction().begin();
												session.save(caraPembayaranKoperasiOnline);
												session.getTransaction().commit();
											}

											Lokasi lokasi = (Lokasi) ConstantValues.simpleObject(
													session.createCriteria(Lokasi.class)
															.add(Restrictions.eq("toko", toko)).setMaxResults(1),
													Lokasi.class);
											if (lokasi == null) {
												lokasi = new Lokasi();
												lokasi.setNama(toko.getNama());
												lokasi.setKode(toko.getKode());
												lokasi.setToko(toko);
												session.getTransaction().begin();
												session.save(lokasi);
												session.getTransaction().commit();
											}

											Double total = 0.0;
											Double totalDiskon = 0.0;
											Double totalCashback = 0.0;
											for (int i = 0; i < transaksi.length(); i++) {
												try {
													JSONObject objectTransaksi = transaksi.getJSONObject(i);
													Double hargaBarang = objectTransaksi.isNull("harga") ? 1.0
															: Double.parseDouble(
																	(objectTransaksi.get("harga") + "").trim());
													Double jumlahBarang = objectTransaksi.isNull("jumlah") ? 1.0
															: Double.parseDouble(
																	(objectTransaksi.get("jumlah") + "").trim());
													Double diskonBarang = objectTransaksi.isNull("diskon") ? 0.0
															: Double.parseDouble(
																	(objectTransaksi.get("diskon") + "").trim());
													Double cashbackBarang = objectTransaksi.isNull("cashback") ? 0.0
															: Double.parseDouble(
																	(objectTransaksi.get("cashback") + "").trim());
													total += (hargaBarang * jumlahBarang) - diskonBarang;

													totalDiskon += diskonBarang;
													totalCashback += cashbackBarang;

												} catch (Exception e) {
													e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/TopupHelper.java:334");
												}
											}

											Long iddraftPembelianAnggotaKoperasi = jsonObject
													.isNull("draftPembelianAnggotaKoperasi")
															? null
															: Long.parseLong(
																	(jsonObject.get("draftPembelianAnggotaKoperasi")
																			+ "").trim());
											DraftPembelianAnggotaKoperasi draftPembelianAnggotaKoperasi = (iddraftPembelianAnggotaKoperasi == null
													? null
													: (DraftPembelianAnggotaKoperasi) session
															.createCriteria(DraftPembelianAnggotaKoperasi.class)
															.add(Restrictions.idEq(iddraftPembelianAnggotaKoperasi))
															.uniqueResult());

											PembelianAnggotaKoperasi pembelianAnggotaKoperasi = new PembelianAnggotaKoperasi();
											pembelianAnggotaKoperasi.setTotalDiskon(totalDiskon);
											pembelianAnggotaKoperasi.setTotalCashback(totalCashback);
											pembelianAnggotaKoperasi.setKode(kodePembayaranOnline.getKode());
											pembelianAnggotaKoperasi.setAnggotaKoperasi(
													kodePembayaranOnline.buatAtauAmbilAnggotaKoperasi(session));
											pembelianAnggotaKoperasi.setTanggalPembayaran(currentWaktu);
											pembelianAnggotaKoperasi.setKodePembayaranOnline(kodePembayaranOnline);
											pembelianAnggotaKoperasi
													.setCaraPembayaranKoperasi(caraPembayaranKoperasiOnline);
											pembelianAnggotaKoperasi.setTotalBiaya(total);
											pembelianAnggotaKoperasi.setBiaya(total);
											pembelianAnggotaKoperasi.setLokasi(lokasi);
											pembelianAnggotaKoperasi.setTbmuser(kodePembayaranOnline.getTbmuser());
											pembelianAnggotaKoperasi.setToko(toko);
											session.getTransaction().begin();
											session.save(pembelianAnggotaKoperasi);
											session.getTransaction().commit();

											JSONArray arrayTransaksi = pembelianAnggotaKoperasi.simpanRinci(session,
													transaksi, kodeUnik, currentWaktu, toko, kodePembayaranOnline,
													draftPembelianAnggotaKoperasi);

											if (draftPembelianAnggotaKoperasi != null
													&& draftPembelianAnggotaKoperasi.getId() != null) {
												draftPembelianAnggotaKoperasi.setLunas(pembelianAnggotaKoperasi);
												session.getTransaction().begin();
												session.update(draftPembelianAnggotaKoperasi);
												session.getTransaction().commit();
											}

											JSONObject data = new JSONObject();
											Common.insertProperty(KodePembayaranOnline.class, kodePembayaranOnline,
													data, "", 1);
											jsonObject.put("transaksi", arrayTransaksi);
											jsonObject.put("data", data);
											jsonObject.put("status", "00");
											jsonObject.put("description", "Pembayaran berhasil");
										} else {
											jsonObject.put("status", "01");
											jsonObject.put("description",
													"KOde kodeUnik " + kodeUnik + " tidak ditemukan");
										}
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/TopupHelper.java:395");
									} finally {
										closeSessionSafely(session);
									}

								} else {
									jsonObject.put("status", "01");
									jsonObject.put("description",
											"Toko dengan kode \"" + kodeToko + "\" tidak ditemukan");
								}
								} finally {
									HibernateUtil.closeSessionQuietly(session);
								}
							} else {
								jsonObject.put("status", "01");
								jsonObject.put("description", "Data transaksi harus ada");
							}
						} else {
							jsonObject.put("status", "01");
							jsonObject.put("description", "Parameter kodeUnik minimal 50 digit");
						}
					} else {
						jsonObject.put("status", "01");
						jsonObject.put("description", "Parameter kodeUnik tidak ditemukan");
					}
				} else {
					jsonObject.put("status", "01");
					jsonObject.put("description", "Parameter kodeToko tidak ditemukan");
				}
			} else {
				jsonObject.put("status", "01");
				jsonObject.put("description", "Parameter kode tidak ditemukan");
			}
		} catch (Exception e) {
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", Common.tampilErrorJikaAdmin(e));
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/TopupHelper.java:433");
			}
		}
		return jsonObject;
	}

	public static JSONObject bayarOnline(JSONObject request, HttpServletRequest httpServletRequest) {
		JSONObject jsonObject = new JSONObject();
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, httpServletRequest);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				jsonObject.put("status", "97");
				jsonObject.put("description", "Token tidak sesuai");
				return jsonObject;
			} else {
				jsonObject = bayarOnline(request, httpServletRequest, tbmuser);
			}
		} catch (Exception e) {
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", Common.tampilErrorJikaAdmin(e));
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/TopupHelper.java:455");
			}
		} finally {

		}

		return jsonObject;
	}

	public static JSONObject bayarOnline(JSONObject request, HttpServletRequest httpServletRequest, Tbmuser tbmuser) {
		JSONObject jsonObject = new JSONObject();
		try {

			if (!request.isNull("kode")) {

				String kodeJ = (request.getString("kode")).trim();
				System.out.println("kodeJ -> " + kodeJ);
				JSONObject jsonObjectKode = null;
				try {
					jsonObjectKode = new JSONObject(kodeJ);
				} catch (Exception e) {
					String jsonBersih = kodeJ.replace("\\\"", "\"");
					String bersih = jsonBersih.replaceAll("^\"|\"$", "");
					System.out.println("jsonBersih -> " + bersih);
					jsonObjectKode = new JSONObject(bersih);
				}

				String kodeToko = !jsonObjectKode.isNull("kodeToko") ? (jsonObjectKode.getString("kodeToko")).trim()
						: "";
				String kodeUnik = !jsonObjectKode.isNull("kodeUnik") ? (jsonObjectKode.getString("kodeUnik")).trim()
						: "";
				String idMember = !jsonObjectKode.isNull("idMember") ? (jsonObjectKode.getString("idMember")).trim()
						: "";

				System.out.println("kodeToko -> " + kodeToko + ", kodeUnik -> " + kodeUnik + " tbmuser " + tbmuser
						+ " mhs " + tbmuser.getMahasiswa() + " siswa " + tbmuser.getSiswa() + ", anggota "
						+ tbmuser.getAnggotaKoperasi());

				if (!kodeToko.trim().isEmpty()) {
					if (!kodeUnik.isEmpty()) {
						if (kodeUnik.length() >= 50) {
							Toko toko = (Toko) GeneralValueObject.ambilData(Toko.class, kodeToko, true);

							if (toko == null) {
								Session session = HibernateUtil.openSession();
								try {

								try {
									toko = (Toko) ConstantValues.simpleObject(
											session.createCriteria(Toko.class).add(Restrictions.eq("kode", kodeToko)),
											Toko.class);
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/TopupHelper.java:507");
								} finally {
									closeSessionSafely(session);
								}
								} finally {
									HibernateUtil.closeSessionQuietly(session);
								}
							}

							if (toko != null) {
								if (tbmuser.getMahasiswa() != null || tbmuser.getSiswa() != null
										|| tbmuser.getAnggotaKoperasi() != null
										|| (idMember != null && !idMember.trim().isEmpty())) {
									String nominal = !jsonObjectKode.isNull("nominal")
											? jsonObjectKode.get("nominal") + ""
											: null;
									System.out.println("nominal -> " + nominal);
									Double nominalNumber = parseDoubleSafe(nominal, 0.0);
									Double tabungan = 0.0;

									AnggotaKoperasi anggotaKoperasi = null;
									Session session = HibernateUtil.openSession();
									try {
									try {
										if (idMember != null && !idMember.trim().isEmpty()) {
											anggotaKoperasi = (AnggotaKoperasi) ConstantValues.simpleObject(
													session.createCriteria(AnggotaKoperasi.class)
															.add(Restrictions.idEq(Long.parseLong(idMember.trim()))),
													AnggotaKoperasi.class);
										} else if (tbmuser.getAnggotaKoperasi() != null) {
											anggotaKoperasi = tbmuser.getAnggotaKoperasi();
										}

										if (anggotaKoperasi != null) {
											tabungan = ais.action.master.sekolah.util.DepositHelper
													.hitungDeposit(anggotaKoperasi);
										} else if (tbmuser.getSiswa() != null) {
											tabungan = tbmuser.getSiswa().hitungSisaDeposit(WaktuUtil.getDate());

										} else if (tbmuser.getMahasiswa() != null) {

											tabungan = ais.action.master.sekolah.util.DepositHelper
													.hitungDeposit(tbmuser.getMahasiswa());
										}
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/TopupHelper.java:552");
									} finally {
										closeSessionSafely(session);
									}

									if (tabungan < nominalNumber) {
										jsonObject.put("status", "01");
										jsonObject.put("description",
												"Saldo tidak mencukupi untuk melakukan pembayaran");
									} else {

										HibernateUtil.closeSessionQuietly(session); // tutup session lama sebelum buka ulang (konversi openSession)
										session = HibernateUtil.openSession();
										try {

											if (anggotaKoperasi == null) {
												Criterion criterion = Restrictions.sqlRestriction("false");

												if (tbmuser.getSiswa() != null) {
													criterion = Restrictions.or(criterion,
															Restrictions.eq("siswa", tbmuser.getSiswa()));
												} else if (tbmuser.getMahasiswa() != null) {
													criterion = Restrictions.or(criterion,
															Restrictions.eq("mahasiswa", tbmuser.getMahasiswa()));
												} else if (tbmuser != null) {
													criterion = Restrictions.or(criterion,
															Restrictions.eq("tbmuser", tbmuser));
												}

												anggotaKoperasi = (AnggotaKoperasi) ConstantValues
														.simpleObject(
																session.createCriteria(AnggotaKoperasi.class)
																		.add(criterion).setMaxResults(1),
																AnggotaKoperasi.class);
												if (anggotaKoperasi == null) {
													anggotaKoperasi = new AnggotaKoperasi();
													anggotaKoperasi.setMahasiswa(tbmuser.getMahasiswa());
													anggotaKoperasi.setDosen(tbmuser.getDosen());
													anggotaKoperasi.setGuru(tbmuser.getGuru());
													anggotaKoperasi.setSiswa(tbmuser.getSiswa());
													anggotaKoperasi.setPegawai(tbmuser.getPegawai());
													anggotaKoperasi.setTbmuser(tbmuser);
													session.getTransaction().begin();
													session.save(anggotaKoperasi);
													session.getTransaction().commit();
												}
											}
											KodePembayaranOnline kodePembayaranOnline = (KodePembayaranOnline) session
													.createCriteria(KodePembayaranOnline.class)
													.add(Restrictions.eq("kode", kodeUnik)).uniqueResult();
											if (kodePembayaranOnline == null) {
												kodePembayaranOnline = new KodePembayaranOnline();
											}
											kodePembayaranOnline.setToko(toko);
											kodePembayaranOnline.setKode(kodeUnik);
											kodePembayaranOnline.setMahasiswa(tbmuser.getMahasiswa());
											kodePembayaranOnline.setSiswa(tbmuser.getSiswa());
											kodePembayaranOnline.setCalonSiswa(kodePembayaranOnline.getCalonSiswa());
											kodePembayaranOnline.setBiodataCalonMahasiswa(
													kodePembayaranOnline.getBiodataCalonMahasiswa());
											kodePembayaranOnline.setTbmuser(tbmuser);
											kodePembayaranOnline.setLogPembayaran(jsonObjectKode.toString());
											kodePembayaranOnline.setAnggotaKoperasi(anggotaKoperasi);
											session.getTransaction().begin();
											Common.refreshSaveOrUpdate(session, kodePembayaranOnline);
											session.getTransaction().commit();

											JSONObject data = new JSONObject();
											Common.insertProperty(KodePembayaranOnline.class, kodePembayaranOnline,
													data, "", 1);
											jsonObject.put("data", data);
											jsonObject.put("status", "00");
											jsonObject.put("description", "Pembayaran berhasil");
										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/api/TopupHelper.java:626");
										} finally {
											closeSessionSafely(session);
										}
									}
									} finally {
										HibernateUtil.closeSessionQuietly(session);
									}
								} else {
									jsonObject.put("status", "01");
									jsonObject.put("description",
											"Pengguna harus terdaftar sebagai mahasiswa atau siswa");
								}
							} else {
								jsonObject.put("status", "01");
								jsonObject.put("description", "Toko atau pedagang ini belum terdaftar");
							}
						} else {
							jsonObject.put("status", "01");
							jsonObject.put("description", "Parameter kodeUnik minimal 50 digit");
						}
					} else {
						jsonObject.put("status", "01");
						jsonObject.put("description", "Parameter kodeUnik tidak ditemukan");
					}
				} else {
					jsonObject.put("status", "01");
					jsonObject.put("description", "Parameter kodeToko tidak ditemukan");
				}
			} else {
				jsonObject.put("status", "01");
				jsonObject.put("description", "Parameter kode tidak ditemukan");
			}
		} catch (Exception e) {
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", Common.tampilErrorJikaAdmin(e));
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/TopupHelper.java:664");
			}
		}
		return jsonObject;
	}

	@SuppressWarnings({})
	public static JSONObject topupSiswa(JSONObject request, HttpServletRequest httpServletRequest) throws Exception {
		Tbmuser tbmuser = ApiUtil.currentUser(request, httpServletRequest);
		if (tbmuser == null || tbmuser.getUserId() == null) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("status", "97");
			jsonObject.put("description", "Token tidak sesuai");
			return jsonObject;
		} else {
			return topupSiswa(request, httpServletRequest, tbmuser);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static JSONObject topupSiswa(JSONObject request, HttpServletRequest httpServletRequest, Tbmuser tbmuser) {
		JSONObject jsonObject = new JSONObject();
		Session session = null;

		try {

			String bank = request.has("bank") && !request.isNull("bank") ? request.getString("bank").trim() : "";
			if (bank.isEmpty()) {
				jsonObject.put("status", "03");
				jsonObject.put("description", "Pengambilan data gagal, bank tidak ditemukan");
				return jsonObject;
			}

			String topupStr = request.has("topup") && !request.isNull("topup") ? request.getString("topup") : null;
			Double topupNumber = parseDoubleSafe(topupStr, 0.0);

			Siswa siswa = tbmuser.getSiswa();
			CalonSiswa calonSiswa = tbmuser.getCalonSiswa();

			if (siswa == null && calonSiswa == null) {
				jsonObject.put("status", "01");
				jsonObject.put("description", "Pengambilan data gagal, token harus sebagai siswa / calon siswa");
				return jsonObject;
			}

			Sekolah sekolah = siswa != null ? siswa.getSekolah() : calonSiswa.getSekolah();
			session = HibernateUtil.openSession();
			try {

			AkunPembayaranSiswa akunPembayaranSiswa = (AkunPembayaranSiswa) ConstantValues.simpleObject(session
					.createCriteria(AkunPembayaranSiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("manual", false)).add(Restrictions.eq("sekolah", sekolah)).setMaxResults(1),
					AkunPembayaranSiswa.class);

			double biayaAdministrasi = 0.0;
			List<String> warnings = new ArrayList<String>();
			Double amn = topupNumber;
			String billExpired = "";
			String va = "";
			String link = "";
			VirtualAccountBank virtualAccountBank = null;

			boolean isBNI = bank.equalsIgnoreCase("BNI") || Konfigurasi.AKTIF
					.equals(Common.getKonfigurasi("masih_hanya_pakai_bni", Konfigurasi.TIDAK_AKTIF).getNilai());
			BankHost bankHost = null;

			if (!isBNI) {
				bankHost = PembayaranUtil.getInstance()
						.getBankHost(Common.getKonfigurasi("online_bank_host_ip", "").getNilai(), "Bank Host");
			}

			if (isBNI) {
				biayaAdministrasi = parseDoubleSafe(Common.getKonfigurasi("bni_biaya_administrasi", "0.0").getNilai(),
						0.0);

				BniRequest bniRequest = BniCommon.onSaveBni(siswa, calonSiswa, null, amn, false, topupNumber);
				if (bniRequest != null) {
					va = bniRequest.getVa() == null ? "" : bniRequest.getVa();
					billExpired = bniRequest.getBillExpired() == null ? ""
							: Common.dateFormat5.get().format(bniRequest.getBillExpired());
				}
			} else {
				Map param = new HashMap();

				if (bank.equalsIgnoreCase("BTN")) {
					biayaAdministrasi = parseDoubleSafe(
							Common.getKonfigurasi("btn_biaya_administrasi", "0.0").getNilai(), 0.0);
					virtualAccountBank = DownloadTagihanMahasiswaBankBtn.downloadData(siswa, calonSiswa, null, false,
							biayaAdministrasi, bankHost, akunPembayaranSiswa, sekolah);

				} else if (bank.equalsIgnoreCase("Flip")) {
					biayaAdministrasi = sekolah.getBiayaAdminFlip();
					param.put("flip", true);
					virtualAccountBank = DownloadTagihanSiswaBankOnline.downloadData(siswa, calonSiswa, null, param,
							biayaAdministrasi, null, topupNumber, bankHost, akunPembayaranSiswa, sekolah, warnings);

				} else if (bank.equalsIgnoreCase("Smartlink")) {
					biayaAdministrasi = sekolah.getBiayaAdminEsmartlink();
					if (sekolah.getVariableBiayaAdminEsmartlink() == null
							|| sekolah.getVariableBiayaAdminEsmartlink().isEmpty()) {
						param.put("esmartlink", true);
						virtualAccountBank = DownloadTagihanSiswaBankOnline.downloadData(siswa, calonSiswa, null, param,
								biayaAdministrasi, null, topupNumber, bankHost, akunPembayaranSiswa, sekolah, warnings);
					} else {
						va = Common.getGeneratedBarCode();
						link = Common.getRequestHostWithProtocol(httpServletRequest)
								+ "/common/smartlink/no_va.zul?siswa=" + (siswa == null ? "-1" : siswa.getId())
								+ "&calonSiswa=" + (calonSiswa == null ? "-1" : calonSiswa.getId()) + "&topup="
								+ topupNumber.intValue() + "&tag=&bankHost="
								+ (bankHost == null ? "-1" : bankHost.getId()) + "&akunPembayaranSiswa="
								+ (akunPembayaranSiswa == null ? "-1" : akunPembayaranSiswa.getId());
					}

				} else if (bank.equalsIgnoreCase("Finpay")) {
					biayaAdministrasi = sekolah.getBiayaAdminFlip();
					param.put("finpay", true);
					virtualAccountBank = DownloadTagihanSiswaBankOnline.downloadData(siswa, calonSiswa, null, param,
							biayaAdministrasi, null, topupNumber, bankHost, akunPembayaranSiswa, sekolah, warnings);

				} else if (bank.equalsIgnoreCase("Maja")) {
					biayaAdministrasi = parseDoubleSafe(Common.getKonfigurasi("online_biaya_maja", "0.0").getNilai(),
							0.0);
					param.put("maja", true);
					virtualAccountBank = DownloadTagihanSiswaBankOnline.downloadData(siswa, calonSiswa, null, param,
							biayaAdministrasi, null, topupNumber, bankHost, akunPembayaranSiswa, sekolah, warnings);

				} else {
					biayaAdministrasi = parseDoubleSafe(
							Common.getKonfigurasi("online_biaya_administrasi", "0.0").getNilai(), 0.0);
					virtualAccountBank = DownloadTagihanSiswaBankOnline.downloadData(siswa, calonSiswa, null, param,
							biayaAdministrasi, null, topupNumber, bankHost, akunPembayaranSiswa, sekolah);
				}

				if (virtualAccountBank != null) {
					if (va.isEmpty())
						va = virtualAccountBank.getKode() == null ? "" : virtualAccountBank.getKode();
					if (link.isEmpty())
						link = virtualAccountBank.getLink() == null ? "" : virtualAccountBank.getLink();
					if (billExpired.isEmpty()) {
						billExpired = virtualAccountBank.getKadaluarsaWaktu() == null ? ""
								: Common.dateFormat5.get().format(virtualAccountBank.getKadaluarsaWaktu());
					}
				}
			}

			if (!warnings.isEmpty()) {
				jsonObject.put("status", "05");
				jsonObject.put("description", warnings.get(0));
			} else if (va == null || va.isEmpty()) {
				jsonObject.put("status", "05");
				jsonObject.put("description", "Pengambilan data gagal, nomor VA tidak bisa dibuat");
			} else {
				String kodebankLainOnline = "";
				Konfigurasi confBankLain = Common.getKonfigurasi("prefix_kode_bank_lain_online", "");
				if (confBankLain != null && confBankLain.getNilai() != null) {
					kodebankLainOnline = confBankLain.getNilai().trim();
				}

				if (!kodebankLainOnline.isEmpty()) {
					String prefix = "";
					if (virtualAccountBank != null && virtualAccountBank.getKanalPembayaran() != null
							&& virtualAccountBank.getKanalPembayaran().getBsiUsername() != null
							&& !virtualAccountBank.getKanalPembayaran().getBsiUsername().isEmpty()) {
						prefix = virtualAccountBank.getKanalPembayaran().getBsiUsername();
					} else if (sekolah != null && sekolah.getBsiUsername() != null
							&& !sekolah.getBsiUsername().isEmpty()) {
						prefix = sekolah.getBsiUsername();
					}

					va = prefix + va;
					kodebankLainOnline += va;
				}

				JSONArray jsonArray = new JSONArray();
				JSONObject jsonObjecttopup = new JSONObject();
				jsonObjecttopup.put("nama", "Topup");
				jsonObjecttopup.put("nilai", topupNumber);
				jsonArray.put(jsonObjecttopup);

				jsonObject.put("billExpired", billExpired);
				jsonObject.put("topup", jsonObjecttopup);
				jsonObject.put("biayaAdministrasi", biayaAdministrasi);
				jsonObject.put("total", amn);
				jsonObject.put("va", va);
				jsonObject.put("link", link);

				if (!kodebankLainOnline.isEmpty()) {
					jsonObject.put("va_bank_lain", kodebankLainOnline);
				}

				jsonObject.put("data", jsonArray);
				jsonObject.put("status", "00");
				jsonObject.put("description", "Pengambilan data berhasil");
			}

			} finally {
				HibernateUtil.closeSessionQuietly(session);
			}
		} catch (Exception e) {
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", Common.tampilErrorJikaAdmin(e));
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/TopupHelper.java:868");
			}
		} finally {
			closeSessionSafely(session);
		}

		return jsonObject;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static JSONObject topupAnggotaKoperasi(JSONObject request, HttpServletRequest httpServletRequest,
			Tbmuser tbmuser) {
		JSONObject jsonObject = new JSONObject();
		Session session = null;

		try {

			String bank = request.has("bank") && !request.isNull("bank") ? request.getString("bank").trim() : "";
			if (bank.isEmpty()) {
				jsonObject.put("status", "03");
				jsonObject.put("description", "Pengambilan data gagal, bank tidak ditemukan");
				return jsonObject;
			}

			String caraPembayaranKoperasidata = request.has("caraPembayaranKoperasi")
					&& !request.isNull("caraPembayaranKoperasi") ? request.getString("caraPembayaranKoperasi") : null;
			String topupStr = request.has("topup") && !request.isNull("topup") ? request.getString("topup") : null;
			Double topupNumber = parseDoubleSafe(topupStr, 0.0);

			AnggotaKoperasi anggotaKoperasi = tbmuser.getAnggotaKoperasi();

			if (anggotaKoperasi == null) {
				jsonObject.put("status", "01");
				jsonObject.put("description", "Pengambilan data gagal, token harus sebagai anggota koperasi");
				return jsonObject;
			}

			CaraPembayaranKoperasi caraPembayaranKoperasi = (CaraPembayaranKoperasi) (caraPembayaranKoperasidata == null
					? null
					: GeneralValueObject.ambilData(CaraPembayaranKoperasi.class, caraPembayaranKoperasidata));
			if (caraPembayaranKoperasi == null) {
				jsonObject.put("status", "01");
				jsonObject.put("description", "Cara Pembayaran pembayaran harus dipilih");
				return jsonObject;
			}
			BankHost bankHost = null;
			session = HibernateUtil.openSession();
			try {

			double biayaAdministrasi = 0.0;
			List<String> warnings = new ArrayList<String>();
			Double amn = topupNumber;
			String billExpired = "";
			String va = "";
			String link = "";
			VirtualAccountBank virtualAccountBank = null;

			Map param = new HashMap();

			param.put("esmartlink", true);
			virtualAccountBank = DownloadTagihanAnggotaKoperasiBankOnline.downloadData(anggotaKoperasi, topupNumber,
					0.0, param, bankHost, caraPembayaranKoperasi);

			if (virtualAccountBank != null) {
				if (va.isEmpty())
					va = virtualAccountBank.getKode() == null ? "" : virtualAccountBank.getKode();
				if (link.isEmpty())
					link = virtualAccountBank.getLink() == null ? "" : virtualAccountBank.getLink();
				if (billExpired.isEmpty()) {
					billExpired = virtualAccountBank.getKadaluarsaWaktu() == null ? ""
							: Common.dateFormat5.get().format(virtualAccountBank.getKadaluarsaWaktu());
				}
			}

			if (!warnings.isEmpty()) {
				jsonObject.put("status", "05");
				jsonObject.put("description", warnings.get(0));
			} else if (va == null || va.isEmpty()) {
				jsonObject.put("status", "05");
				jsonObject.put("description", "Pengambilan data gagal, nomor VA tidak bisa dibuat");
			} else {
				String kodebankLainOnline = "";
				Konfigurasi confBankLain = Common.getKonfigurasi("prefix_kode_bank_lain_online", "");
				if (confBankLain != null && confBankLain.getNilai() != null) {
					kodebankLainOnline = confBankLain.getNilai().trim();
				}

				JSONArray jsonArray = new JSONArray();
				JSONObject jsonObjecttopup = new JSONObject();
				jsonObjecttopup.put("nama", "Topup");
				jsonObjecttopup.put("nilai", topupNumber);
				jsonArray.put(jsonObjecttopup);

				jsonObject.put("billExpired", billExpired);
				jsonObject.put("topup", jsonObjecttopup);
				jsonObject.put("biayaAdministrasi", biayaAdministrasi);
				jsonObject.put("total", amn);
				jsonObject.put("va", va);
				jsonObject.put("link", link);

				if (!kodebankLainOnline.isEmpty()) {
					jsonObject.put("va_bank_lain", kodebankLainOnline);
				}

				jsonObject.put("data", jsonArray);
				jsonObject.put("status", "00");
				jsonObject.put("description", "Pengambilan data berhasil");
			}

			} finally {
				HibernateUtil.closeSessionQuietly(session);
			}
		} catch (Exception e) {
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", Common.tampilErrorJikaAdmin(e));
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/TopupHelper.java:985");
			}
		} finally {
			closeSessionSafely(session);
		}

		return jsonObject;
	}

	public static JSONObject topup_mahasiswa(JSONObject request, HttpServletRequest httpServletRequest)
			throws Exception {
		Tbmuser tbmuser = ApiUtil.currentUser(request, httpServletRequest);
		if (tbmuser == null || tbmuser.getUserId() == null) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("status", "97");
			jsonObject.put("description", "Token tidak sesuai");
			return jsonObject;
		} else {
			return topup_mahasiswa(request, httpServletRequest, tbmuser);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static JSONObject topup_mahasiswa(JSONObject request, HttpServletRequest httpServletRequest,
			Tbmuser tbmuser) {
		JSONObject jsonObject = new JSONObject();
		Session session = null;

		try {

			Mahasiswa mahasiswa = tbmuser.getMahasiswa();
			if (mahasiswa == null) {
				jsonObject.put("status", "01");
				jsonObject.put("description", "Pengambilan data gagal, token harus sebagai mahasiswa");
				return jsonObject;
			}

			String bank = request.has("bank") && !request.isNull("bank") ? request.getString("bank").trim() : "";
			if (bank.isEmpty()) {
				jsonObject.put("status", "03");
				jsonObject.put("description", "Pengambilan data gagal, bank tidak ditemukan");
				return jsonObject;
			}

			String topupStr = request.has("topup") && !request.isNull("topup") ? request.getString("topup") : null;
			Double topupNumber = parseDoubleSafe(topupStr, 0.0);

			BiodataCalonMahasiswa calonMahasiswa = mahasiswa.getBiodataCalonMahasiswaData();

			if (bank.equalsIgnoreCase("MANDIRI") || bank.equalsIgnoreCase("Online")
					|| bank.equalsIgnoreCase("Smartlink") || bank.equalsIgnoreCase("BSI")
					|| bank.equalsIgnoreCase("BCA") || bank.equalsIgnoreCase("MNC Bank")
					|| bank.equalsIgnoreCase("Finpay") || bank.equalsIgnoreCase("Flip")) {

				Integer smt = 0;
				List detailBiayas = new ArrayList();
				Grid gridCicilan = new Grid();
				Rows rows = new Rows();
				rows.setParent(gridCicilan);

				Double biayaAdministrasi = parseDoubleSafe(
						Common.getKonfigurasi("online_biaya_administrasi", "0.0").getNilai(), 0.0);
				String ket = "";
				String pemb = "";
				String cicilan = "";
				Double total = topupNumber;
				JSONArray items = new JSONArray();

				if (biayaAdministrasi.intValue() > 0) {
					JSONObject jsonObjectitems = new JSONObject();
					jsonObjectitems.put("description", "Biaya Admin");
					jsonObjectitems.put("unitPrice", biayaAdministrasi.intValue());
					jsonObjectitems.put("qty", 1);
					jsonObjectitems.put("amount", biayaAdministrasi.intValue());
					items.put(jsonObjectitems);
				}

				String tahunAkademik = Common.getCurrentTahunAkademik();
				BankHost bankHost = PembayaranUtil.getInstance()
						.getBankHost(Common.getKonfigurasi("online_bank_host_ip", "").getNilai(), "Bank Host");

				Map param = new HashMap();
				param.put("otto", bank.equalsIgnoreCase("Otto"));
				param.put("finpay", bank.equalsIgnoreCase("Finpay"));
				param.put("flip", bank.equalsIgnoreCase("Flip"));
				param.put("smartlink", bank.equalsIgnoreCase("Smartlink"));

				if (bank.equalsIgnoreCase("Smartlink")) {
					param.put("smartlink_direct", true);
					String link = Common.getRequestHostWithProtocol(httpServletRequest)
							+ "/common/smartlink/no_va2.zul?mahasiswa=" + (mahasiswa == null ? "-1" : mahasiswa.getId())
							+ "&calonMahasiswa=" + (calonMahasiswa == null ? "-1" : calonMahasiswa.getId()) + "&ket="
							+ URLEncoder.encode(ket, "UTF-8") + "&topup=" + topupNumber.intValue() + "&items="
							+ URLEncoder.encode(items.toString(), "UTF-8") + "&pemb=" + URLEncoder.encode(pemb, "UTF-8")
							+ "&cicilan=" + URLEncoder.encode(cicilan, "UTF-8") + "&total=" + total + "&smt=" + smt
							+ "&bankHost=" + (bankHost == null ? "-1" : bankHost.getId());

					param.put("payment_url", link);
				}
				param.put("tahunAkademik", tahunAkademik);

				VirtualAccountBank virtualAccountBank = DownloadTagihanMahasiswaBankOnline.downloadData(mahasiswa, smt,
						null, detailBiayas, gridCicilan, param, biayaAdministrasi, null, topupNumber, bankHost);

				if (virtualAccountBank == null || virtualAccountBank.getKode() == null
						|| virtualAccountBank.getKode().isEmpty()) {
					jsonObject.put("status", "05");
					jsonObject.put("description", "Pengambilan data gagal, nomor VA tidak bisa dibuat");
				} else {
					JSONArray jsonArray = new JSONArray();
					JSONObject jsonObjecttopup = new JSONObject();
					jsonObjecttopup.put("nama", "Topup");
					jsonObjecttopup.put("nilai", topupNumber);
					jsonArray.put(jsonObjecttopup);

					if (virtualAccountBank.getLink() != null && !virtualAccountBank.getLink().isEmpty()) {
						jsonObject.put("link", virtualAccountBank.getLink());
					}

					jsonObject.put("va", virtualAccountBank.getKode());
					jsonObject.put("biayaAdministrasi", biayaAdministrasi);
					jsonObject.put("data", jsonArray);
					jsonObject.put("status", "00");
					jsonObject.put("description", "Pengambilan data berhasil");
				}
			} else {
				jsonObject.put("status", "03");
				jsonObject.put("description", "Pengambilan data gagal, bank tidak ditemukan");
			}

		} catch (Exception e) {
			try {
				jsonObject.put("status", "90");
				jsonObject.put("description", Common.tampilErrorJikaAdmin(e));
			} catch (Exception ee) {
				ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/action/servlet/api/TopupHelper.java:1120");
			}
		} finally {
			closeSessionSafely(session);
		}

		return jsonObject;
	}
}