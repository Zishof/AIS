package ais.action.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.action.report.CommonReportHelper;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankHost;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisPembayaran;
import ais.database.model.Kegiatan;
import ais.database.model.KegiatanTemporary;
import ais.database.model.LogHostToHost;
import ais.database.model.LogPembayaran;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.jatelindo.JatelindoRequest;
import ais.database.model.jatelindo.JatelindoRequestDetail;
import ais.database.model.jatelindo.JatelindoResponse;

/**
 * Servlet implementation class CheckISBN
 */
public class JatelindoCallback extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public JatelindoCallback() {
		super();

		// TODO Auto-generated constructor stub
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

	public static Kegiatan createKegiatan(JatelindoRequest jatelindoRequest, Session session) {
		Kegiatan kegiatan = null;
		Double nilaiBiayaHarusDiBayars = jatelindoRequest.getNilaiBiayaHarusDiBayars();

		Mahasiswa mhs = jatelindoRequest.getMahasiswa();
		BiodataCalonMahasiswa bio = jatelindoRequest.getBiodataCalonMahasiswa();

		Integer semester = jatelindoRequest.getSemester();
		JadwalPembayaran jadwalPembayaran = jatelindoRequest.getJadwalPembayaran();
		JenisKegiatan jenisKegiatan = jatelindoRequest.getJenisKegiatan();
		if (mhs != null) {
			kegiatan = mhs.ambilKegiatans(semester, jenisKegiatan);
		} else if (bio != null) {
			kegiatan = bio.ambilKegiatans(semester, jenisKegiatan);
			if (kegiatan == null && semester <= 1) {
				kegiatan = bio.ambilKegiatans(jenisKegiatan);
			}
		}

		System.out.println("mhs==>" + mhs + ",bio==>" + bio + ", semester==>" + semester + ",jadwalPembayaran==>"
				+ jadwalPembayaran + ",jenisKegiatan==>" + jenisKegiatan + ",kegiatan==>" + kegiatan
				+ ",nilaiBiayaHarusDiBayars==>" + nilaiBiayaHarusDiBayars + ",pengurangan==>"
				+ jatelindoRequest.getPengurangan() + ", hapusCicilanSebelumnya ==> "
				+ jatelindoRequest.getHapusCicilanSebelumnya());

		if (kegiatan == null || kegiatan.getId() == null) {
			kegiatan = new Kegiatan();
			kegiatan.setJenisKegiatan(jenisKegiatan);
			kegiatan.setJadwalPembayaran(jadwalPembayaran);
			kegiatan.setMahasiswa(mhs);
			kegiatan.setCalonMahasiswa(bio);
			kegiatan.setSemster(semester);
			if (mhs != null) {
				kegiatan.setStatusMahasiswa(Common
						.currentStatus(mhs, kegiatan.getTahunAkademik(), kegiatan.getSemster()).getStatusMahasiswa());
			} else if (bio != null) {
				kegiatan.setStatusMahasiswa(ConstantValues.AKTIF);
			}
			kegiatan.setTahunAkademik(jatelindoRequest.getTahunAkademik());
			kegiatan.setTanggal(ais.ui.util.WaktuUtil.getDate());
			kegiatan.setValidated(1);
			kegiatan.setValidator(jatelindoRequest.getMerchant());
			kegiatan.setPengurangan(jatelindoRequest.getPengurangan());
			kegiatan.setKeterangan(jatelindoRequest.getKeterangan());
			kegiatan.setAmount(nilaiBiayaHarusDiBayars);

			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, kegiatan);
			session.getTransaction().commit();
		} else {
			session.refresh(kegiatan); 
		}
		return kegiatan;
	}


	private static boolean isRequestSudahDiproses(JatelindoRequest jatelindoRequest) {
		if (jatelindoRequest == null) {
			return false;
		}
		try {
			Collection temporarys = jatelindoRequest.getKegiatanTemporarys();
			if (temporarys != null && !temporarys.isEmpty()) {
				boolean semuaSudahTerhubungKeKegiatan = true;
				for (Object object : temporarys) {
					KegiatanTemporary kegiatanTemporary = (KegiatanTemporary) object;
					if (kegiatanTemporary == null || kegiatanTemporary.getKegiatan() == null
							|| kegiatanTemporary.getKegiatan().getId() == null) {
						semuaSudahTerhubungKeKegiatan = false;
						break;
					}
				}
				return semuaSudahTerhubungKeKegiatan;
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return false;
	}

	@SuppressWarnings({ "unchecked", "static-access" })
	public static void prosesResponse(JatelindoResponse jatelindoResponse) {
		Session session = null;
		try {
		session = HibernateUtil.getSessionFactory().openSession();
		JatelindoRequest jatelindoRequest = (JatelindoRequest) session.createCriteria(JatelindoRequest.class)
				.add(Restrictions.eq("trxId", jatelindoResponse.getTrxId())).setMaxResults(1).uniqueResult();

		if (jatelindoRequest != null && jatelindoResponse.getKodeStatus().toString().trim().equalsIgnoreCase("2")) {

			if (isRequestSudahDiproses(jatelindoRequest)) {
				System.out.println("Callback jatelindoRequest sudah pernah diproses, proses pembayaran dilewati: " + jatelindoRequest);
				return;
			}

			jatelindoRequest.setJatelindoResponse(jatelindoResponse);
			jatelindoRequest.setStatus("Payment Sukses");
			jatelindoRequest.setKodeStatus(jatelindoResponse.getKodeStatus());
			session.getTransaction().begin();
			session.update(jatelindoRequest);
			session.getTransaction().commit();

			String kodeAkun = Common.getKonfigurasi("kode_akun_jatelindo", "").getNilai();
			JenisPembayaran jenisPembayaran = JenisPembayaran.ambilJenisPembayaranBerdasarkanKodeAkun(session,
					kodeAkun);

			if (!jatelindoRequest.getKegiatanTemporarys().isEmpty()) {
				Kegiatan kegiatan = null;
				for (KegiatanTemporary temporary : jatelindoRequest.getKegiatanTemporarys()) {

					Session sessionLocalKeg = null;
					try {
					sessionLocalKeg = HibernateUtil.getSessionFactory().openSession();
						KegiatanTemporary kegiatanTemporary = (KegiatanTemporary) sessionLocalKeg
								.createCriteria(KegiatanTemporary.class).add(Restrictions.idEq(temporary.getId()))
								.uniqueResult();
						if (kegiatanTemporary != null) {

							try {
								Mahasiswa mahasiswa = kegiatanTemporary.getMahasiswa();
								Integer smt = kegiatanTemporary.getSemster();
								JenisKegiatan jenisKegiatan = kegiatanTemporary.getJenisKegiatan();
								kegiatan = mahasiswa.ambilKegiatansRefresh(smt, jenisKegiatan);

								if (kegiatan == null || kegiatan.getId() == null) {
									kegiatan = new Kegiatan();
								} else {
									kegiatan = (Kegiatan) sessionLocalKeg.createCriteria(Kegiatan.class)
											.add(Restrictions.idEq(kegiatan.getId())).uniqueResult();
								}

								kegiatan.setJenisKegiatan(jenisKegiatan);
								kegiatan.setJadwalPembayaran(kegiatanTemporary.getJadwalPembayaran());
								kegiatan.setMahasiswa(mahasiswa);
								kegiatan.setSemster(smt);
								kegiatan.setStatusMahasiswa(kegiatanTemporary.getStatusMahasiswa());
								kegiatan.setTahunAkademik(kegiatanTemporary.getTahunAkademik());
								kegiatan.setTanggal(kegiatanTemporary.getTanggal());
								kegiatan.setValidated(1);
								kegiatan.setJenisKegiatan(jenisKegiatan);
								kegiatan.setValidator(jatelindoRequest.getMerchant());
								kegiatan.setKeterangan(kegiatanTemporary.getKeterangan());

								sessionLocalKeg.getTransaction().begin();
								Common.refreshSaveOrUpdate(sessionLocalKeg, kegiatan);
								sessionLocalKeg.getTransaction().commit();

								List<CicilanPembayaran> cicilanPembayarans = sessionLocalKeg
										.createCriteria(CicilanPembayaran.class)
										.add(Restrictions.eq("kegiatanTemporary.id", temporary.getId())).list();

								System.out.println("cicilanPembayarans==>" + cicilanPembayarans.size());
								for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
									Session sessionLocal = null;
									try {
										sessionLocal = HibernateUtil.getSessionFactory().openSession();
										try {
											sessionLocal.refresh(cicilanPembayaran);
											cicilanPembayaran.setKegiatan(kegiatan);
											cicilanPembayaran.setValidator(jatelindoRequest.getMerchant());
											cicilanPembayaran.setJenisPembayaran(jenisPembayaran);
											sessionLocal.getTransaction().begin();
											Common.refreshSaveOrUpdate(sessionLocal, cicilanPembayaran);
											sessionLocal.getTransaction().commit();
										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/JatelindoCallback.java:251");
										}
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/JatelindoCallback.java:254");
									} finally {
										if (sessionLocal != null) {
											try { sessionLocal.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:257");}
											try { sessionLocal.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:258");}
											try { sessionLocal.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:259");}
										}
									}
								}

								try {
									if (kegiatanTemporary.getKegiatan() == null
											|| (kegiatanTemporary.getKegiatan() != null && !kegiatanTemporary
													.getKegiatan().getId().equals(kegiatan.getId()))) {
										sessionLocalKeg.getTransaction().begin();
										kegiatanTemporary.setKegiatan(kegiatan);
										Common.refreshSaveOrUpdate(sessionLocalKeg, kegiatanTemporary);
										sessionLocalKeg.getTransaction().commit();
									}
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:273");
									// TODO: handle exception
								}

								Number jumlah = (Number) sessionLocalKeg.createCriteria(CicilanPembayaran.class)
										.add(Restrictions.isNotNull("itemBiaya"))
										.add(Restrictions.eq("kegiatan", kegiatan))
										.setProjection(Projections.sum("nilai")).uniqueResult();
								Double nilaiBiayaHarusDiBayars = 0.0;
								Double amountTotal = jumlah == null ? 0.0 : jumlah.doubleValue();

								try {

									Map<Long, DetailBiaya> map = new java.util.HashMap<Long, DetailBiaya>();
									Collection<DetailBiaya> mydetailBiayas = pembayaranUtil
											.getDetailBiayaMahasiswa(mahasiswa, smt, jenisKegiatan, false);
									for (Object o : mydetailBiayas) {
										if (o instanceof DetailBiaya) {
											DetailBiaya detailBiaya = (DetailBiaya) o;
											map.put(detailBiaya.getId(), detailBiaya);
										} else if (o instanceof PengaturanPembayaranBulanan) {
											PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
											DetailBiaya detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();
											map.put(detailBiaya.getId(), detailBiaya);
										}
									}

									for (DetailBiaya detailBiaya : map.values()) {
										nilaiBiayaHarusDiBayars += Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya);
									}

									kegiatan.setAmountTerhutang(nilaiBiayaHarusDiBayars - amountTotal);
									kegiatan.setAmount(amountTotal);
									sessionLocalKeg.getTransaction().begin();
									Common.refreshSaveOrUpdate(sessionLocalKeg, kegiatan);
									sessionLocalKeg.getTransaction().commit();
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}

							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/JatelindoCallback.java:315");
							}
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					} finally {
						if (sessionLocalKeg != null) {
							try { sessionLocalKeg.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:322");}
							try { sessionLocalKeg.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:323");}
							try { sessionLocalKeg.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:324");}
						}
					}
				}

				if (jatelindoRequest.getAmount() > 0.1) {
					LogPembayaran logPembayaran = (LogPembayaran) session.createCriteria(LogPembayaran.class)
							.add(Restrictions.eq("jatelindoRequest", jatelindoRequest)).setMaxResults(1).uniqueResult();
					if (logPembayaran == null) {
						logPembayaran = new LogPembayaran();
					}
					double biayaAdministrasi = 0.0;
					try {
						biayaAdministrasi = Double
								.parseDouble(Common.getKonfigurasi("jatelindo_biaya_administrasi", "0.0").getNilai());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:339");

					}
					logPembayaran.setBiayaAdministrasi(biayaAdministrasi);
					logPembayaran.setJatelindoRequest(jatelindoRequest);
					logPembayaran.setNominal(jatelindoRequest.getAmount());
					logPembayaran.setKeterangan(jatelindoRequest.getRequest());
					logPembayaran.setKegiatan(kegiatan);
					logPembayaran.setValidator(jatelindoRequest.getMerchant());
					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, logPembayaran);
					session.getTransaction().commit();
				}

			} else {

				Kegiatan kegiatan = JatelindoCallback.createKegiatan(jatelindoRequest, session);
				Double nilaiBiayaHarusDiBayars = jatelindoRequest.getNilaiBiayaHarusDiBayars();

				List<JatelindoRequestDetail> jatelindoRequestDetails = session
						.createCriteria(JatelindoRequestDetail.class).add(Restrictions.isNull("idCicilan"))
						.add(Restrictions.eq("jatelindoRequest", jatelindoRequest)).add(Restrictions.gt("ke", 0))
						.addOrder(Order.asc("ke")).add(Restrictions.gt("ke", 0)).list();
				System.out.println("jatelindoRequestDetails==>" + jatelindoRequestDetails);
				if (!jatelindoRequestDetails.isEmpty()) {

					for (JatelindoRequestDetail jatelindoRequestDetail : jatelindoRequestDetails) {

						String ref = "jatelindoRequestDetail-" + jatelindoRequestDetail.getId();

						CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
								.createCriteria(CicilanPembayaran.class).add(Restrictions.eq("ref", ref))
								.setMaxResults(1).uniqueResult();
						if (cicilanPembayaran == null) {
							cicilanPembayaran = new CicilanPembayaran(jatelindoRequestDetail.getDetailBiaya());

						}
						cicilanPembayaran.setRef(ref);
						cicilanPembayaran.setValidator(jatelindoRequest.getMerchant());
						cicilanPembayaran.setKe(jatelindoRequestDetail.getKe());
						cicilanPembayaran.setKegiatan(kegiatan);
						cicilanPembayaran.setItemBiaya(jatelindoRequestDetail.getItemBiaya());
						cicilanPembayaran.setPengaturanPembayaranBulanan(
								jatelindoRequestDetail.getPengaturanPembayaranBulanan());
						cicilanPembayaran.setNilai(jatelindoRequestDetail.getNilai());
						cicilanPembayaran.setTanggal(jatelindoRequestDetail.getTanggal());
						cicilanPembayaran.setJenisPembayaran(jenisPembayaran);
						cicilanPembayaran.setDenda(jatelindoRequestDetail.getDenda());
						cicilanPembayaran.setNilaiAsli(jatelindoRequestDetail.getNilaiAsli());
						session.getTransaction().begin();
						if(cicilanPembayaran.getId()==null)session.save(cicilanPembayaran);else Common.refreshUpdate(session, cicilanPembayaran);
						session.getTransaction().commit();

						jatelindoRequestDetail.setCicilanref(cicilanPembayaran.getId());
						session.getTransaction().begin();
						session.update(jatelindoRequestDetail);
						session.getTransaction().commit();
					}

					Double[] d = PembayaranUtil.getInstance().getTotalDanDendaFromCicilan(session, kegiatan);
					Double jumlah = d[0];
					Double denda = d[1];
					kegiatan.setDenda(denda.doubleValue());
					kegiatan.setAmountTerhutang(nilaiBiayaHarusDiBayars - (jumlah.doubleValue() - denda.doubleValue()));
					kegiatan.setAmount(jumlah.doubleValue());

					kegiatan.setValidator(jatelindoRequest.getMerchant());

					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, kegiatan);
					session.getTransaction().commit();
				}

				if (jatelindoRequest.getAmount() > 0.1) {
					LogPembayaran logPembayaran = (LogPembayaran) session.createCriteria(LogPembayaran.class)
							.add(Restrictions.eq("jatelindoRequest", jatelindoRequest)).setMaxResults(1).uniqueResult();
					if (logPembayaran == null) {
						logPembayaran = new LogPembayaran();
					}
					double biayaAdministrasi = 0.0;
					try {
						biayaAdministrasi = Double
								.parseDouble(Common.getKonfigurasi("jatelindo_biaya_administrasi", "0.0").getNilai());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:422");

					}
					logPembayaran.setBiayaAdministrasi(biayaAdministrasi);
					logPembayaran.setJatelindoRequest(jatelindoRequest);
					logPembayaran.setKegiatan(kegiatan);
					logPembayaran.setNominal(jatelindoRequest.getAmount());
					logPembayaran.setKeterangan(jatelindoRequest.getRequest());
					logPembayaran.setValidator(jatelindoRequest.getMerchant());
					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, logPembayaran);
					session.getTransaction().commit();
				}

				pembayaranUtil.updateTunggakan(kegiatan, session);

				Mahasiswa mhs = jatelindoRequest.getMahasiswa();
				BiodataCalonMahasiswa bio = jatelindoRequest.getBiodataCalonMahasiswa();
				if (mhs != null) {
					CommonReportHelper.cetakBuktipembayaranMahasiswa(kegiatan, true);
				} else if (bio != null) {
					CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(kegiatan, true);
				}
			}
		}
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:449");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:450");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:451");}
			}
		}
	}

	public static JatelindoResponse prosesTransaksi(JSONObject jatelindo) throws Exception {
		String bit48Request = jatelindo.getString("bit48");
		String trx_id = bit48Request.split(" ")[0].trim();
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			JatelindoResponse jatelindoResponse = (JatelindoResponse) session.createCriteria(JatelindoResponse.class)
					.add(Restrictions.eq("trxId", trx_id)).setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();
			if (jatelindoResponse == null) {
				jatelindoResponse = new JatelindoResponse();
			}
			jatelindoResponse.setKeterangan(jatelindo.toString());
			jatelindoResponse.setNama(trx_id);
			jatelindoResponse.setStatus(JatelindoResponse.SEDANG_DIPROSES);
			jatelindoResponse.setTrxId(trx_id);
			session.getTransaction().begin();
			session.save(jatelindoResponse);
			session.getTransaction().commit();
			return jatelindoResponse;
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:477");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:478");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:479");}
			}
		}
	}

	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		BankHost bankHost = pembayaranUtil.getBankHost(request.getRemoteAddr(), "Jatelindo");
		// Read from request
		StringBuilder buffer = new StringBuilder();
		BufferedReader reader = request.getReader();
		String line;
		while ((line = reader.readLine()) != null) {
			buffer.append(line);
		}
		String data = buffer.toString();

		String hasil = JatelindoCallback.process(data, request, bankHost, false);

		response.setHeader("Content-Type", "application/json");
		PrintWriter writer = response.getWriter();
		writer.write(hasil);
	}

	@SuppressWarnings({})
	public static String process(String data, HttpServletRequest request, BankHost bankHost, boolean tetaplanjut)
			throws Exception {

		System.out.println("==> JatelindoCallback data => " + data);

		JatelindoRequest jatelindoRequest = null;
		String trx_id = "-0000";
		JSONObject jatelindo = new JSONObject();
		String hasil = "";
		// Jejak stack trace bila pemrosesan callback/pembayaran error; disimpan ke kolom log H2H.
		String h2hStackTrace = null;
		try {
			jatelindo = new JSONObject(data);
			Long nilaiTransaksi = Long.parseLong(jatelindo.getString("bit4").trim());
			Session session = HibernateUtil.getSessionFactory().openSession();
			try {

			String bit48Request = jatelindo.getString("bit48");
			if (jatelindo.getString("bit3").equals("380000")) {
				trx_id = bit48Request.split(" ")[0].trim();
			} else if (jatelindo.getString("bit3").equals("170000")) {
				trx_id = bit48Request.substring(10, 26).trim();
			}

			jatelindoRequest = (JatelindoRequest) session.createCriteria(JatelindoRequest.class)
					.add(Restrictions.eq("trxId", trx_id)).setMaxResults(1).uniqueResult();

			System.out.println(
					"trx_id = " + trx_id + " jatelindoRequest = " + jatelindoRequest + " bankHost " + bankHost);
			if (jatelindoRequest != null && bankHost != null) {

				if (jatelindoRequest.getKodeStatus().equals("2") && !tetaplanjut) {
					jatelindo.put("bit39", "34");
				} else {

					Double amn = jatelindoRequest.getAmount() + jatelindoRequest.getBiayaAdministrasi();
					if (nilaiTransaksi.equals(amn.longValue())) {

						String trx = "000000000000000000000" + jatelindoRequest.getId();
						trx = trx.substring(trx.length() - 8);

						Mahasiswa mahasiswa = jatelindoRequest.getMahasiswa();
						BiodataCalonMahasiswa biodataCalonMahasiswa = jatelindoRequest.getBiodataCalonMahasiswa();

						// String merchant_id =
						// Common.getKonfigurasi("jatelindo_merchant_id",
						// "129").getNilai().trim();
						String no_rek_alias = Common.getKonfigurasi("jatelindo_no_rek_alias", "ecamp").getNilai()
								.trim();

						String label_universitas = Common.getKonfigurasi("label_universitas", "-").getNilai().trim();

						String Norek_alias = Common.maxPanjangSpace(no_rek_alias, 10);
						String No_VA = Common.maxPanjangSpace(trx_id, 16);
						String Nama = mahasiswa != null ? Common.maxPanjangSpace(mahasiswa.getNama(), 30)
								: Common.maxPanjangSpace(
										biodataCalonMahasiswa == null ? "" : biodataCalonMahasiswa.getNama(), 30);
						String Jenis_trx = Common.maxPanjangSpace("TOPUP", 30);
						String Keterangan = Common
								.maxPanjangSpace("Bayar " + (jatelindoRequest.getJenisKegiatan() == null ? ""
										: jatelindoRequest.getJenisKegiatan().getNama()), 30);
						String Id_trx = Common.maxPanjangSpace(trx, 30);
						String amount = Common.maxPanjangNol(amn.intValue() + "", 12);
						String admin = Common.maxPanjangNol("0", 12);
						String hp = mahasiswa != null ? Common.maxPanjangSpace(mahasiswa.getTelp(), 30)
								: Common.maxPanjangSpace(
										biodataCalonMahasiswa == null ? "" : biodataCalonMahasiswa.getHp(), 30);
						String email = mahasiswa != null ? Common.maxPanjangSpace(mahasiswa.getEmail(), 30)
								: Common.maxPanjangSpace(
										biodataCalonMahasiswa == null ? "" : biodataCalonMahasiswa.getEmail(), 30);

						if (jatelindo.getString("bit3").equals("380000")) {

							JatelindoResponse jatelindoResponse = prosesTransaksi(jatelindo);
							String payment_status_desc = "Sedang diproses";
							String payment_status_code = "1";

							session.refresh(jatelindoResponse);
							jatelindoResponse.setKeterangan(jatelindo.toString());
							jatelindoResponse.setNama(jatelindoRequest.getTrxId());
							jatelindoResponse.setStatus(
									payment_status_desc == null ? "Belum diproses" : payment_status_desc.toString());
							jatelindoResponse.setTrxId(jatelindoRequest.getTrxId());
							jatelindoResponse.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
							jatelindoResponse
									.setKodeStatus(payment_status_code == null ? "0" : payment_status_code.toString());
							session.getTransaction().begin();
							Common.refreshSaveOrUpdate(session, jatelindoResponse);
							session.getTransaction().commit();

							session.refresh(jatelindoRequest);
							jatelindoRequest
									.setStatus(payment_status_desc == null ? null : payment_status_desc.toString());
							jatelindoRequest
									.setKodeStatus(payment_status_code == null ? null : payment_status_code.toString());
							session.getTransaction().begin();
							Common.refreshSaveOrUpdate(session, jatelindoRequest);
							session.getTransaction().commit();

							System.out.println("Request " + jatelindo);

							String bit48 = Norek_alias + No_VA + Nama + Jenis_trx + Keterangan + Id_trx + amount + admin
									+ hp + email;
							jatelindo.put("bit48", bit48);
							jatelindo.put("bit39", "00");

							String bit62 = Common.maxPanjangSpace("No.VA", 10)
									+ Common.maxPanjangSpace(no_rek_alias + "-" + No_VA, 30)
									+ Common.maxPanjangSpace("Nama", 10) + Nama + Common.maxPanjangSpace("Jenis", 10)
									+ Jenis_trx + Common.maxPanjangSpace("Nominal", 10)
									+ Common.maxPanjangSpace(
											"Rp. " + Common.numberFormat.get().format(Long.parseLong(amount)), 30)
									+ Common.maxPanjangSpace("Instansi", 10)
									+ Common.maxPanjangSpace(label_universitas, 30);
							jatelindo.put("bit62", bit62);

						} else if (jatelindo.getString("bit3").equals("170000")) {

							JatelindoResponse jatelindoResponse = prosesTransaksi(jatelindo);
							String payment_status_desc = "Payment Sukses";
							String payment_status_code = "2";

							session.refresh(jatelindoResponse);
							jatelindoResponse.setKeterangan(jatelindo.toString());
							jatelindoResponse.setNama(jatelindoRequest.getTrxId());
							jatelindoResponse.setStatus(
									payment_status_desc == null ? "Belum diproses" : payment_status_desc.toString());
							jatelindoResponse.setMerchant(
									jatelindo.isNull("merchant_id") ? "" : jatelindo.get("merchant_id").toString());
							jatelindoResponse.setTrxId(jatelindoRequest.getTrxId());
							jatelindoResponse.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
							jatelindoResponse
									.setKodeStatus(payment_status_code == null ? "0" : payment_status_code.toString());
							session.getTransaction().begin();
							Common.refreshSaveOrUpdate(session, jatelindoResponse);
							session.getTransaction().commit();

							session.refresh(jatelindoRequest);
							jatelindoRequest
									.setStatus(payment_status_desc == null ? null : payment_status_desc.toString());
							jatelindoRequest
									.setKodeStatus(payment_status_code == null ? null : payment_status_code.toString());
							session.getTransaction().begin();
							Common.refreshSaveOrUpdate(session, jatelindoRequest);
							session.getTransaction().commit();

							JatelindoCallback.prosesResponse(jatelindoResponse);

							System.out.println("Payment " + jatelindo);
							String reffNum = Common.maxPanjangSpace(Common.getGeneratedBarCode(), 30);
							String bit48 = Norek_alias + No_VA + Nama + Jenis_trx + Keterangan + Id_trx + amount + admin
									+ hp + email + reffNum;
							jatelindo.put("bit48", bit48);
							jatelindo.put("bit39", "00");

							String bit62 = Common.maxPanjangSpace("No.VA", 10)
									+ Common.maxPanjangSpace(no_rek_alias + "-" + No_VA, 30)
									+ Common.maxPanjangSpace("Nama", 10) + Nama + Common.maxPanjangSpace("Jenis", 10)
									+ Jenis_trx + Common.maxPanjangSpace("Refnum", 10)
									+ Common.maxPanjangSpace(Id_trx, 30) + Common.maxPanjangSpace("Instansi", 10)
									+ Common.maxPanjangSpace(label_universitas, 30);
							jatelindo.put("bit62", bit62);

						}
					} else {
						jatelindo.put("bit39", "51");
					}
				}
			} else {
				jatelindo.put("bit39", "14");
			}
			} finally {
				if (session != null) {
					try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:677");}
					try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:678");}
					try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:679");}
				}
			}
		} catch (Exception e) {
			h2hStackTrace = ais.action.ws.util.PembayaranGatewayHelper.ambilStackTrace(e);
			// jatelindo.put("bit39", "14");
			Common.tampilErrorJikaAdmin(e);
		} finally {
			// JAMINAN: log H2H SELALU dicatat & tercommit walau terjadi error/exception
			// (helper: session terdedikasi + commit + retry, tak pernah gagal).
			try {
				jatelindo.put("mti", "0210");
			} catch (Exception eMti) { ais.common.ErrorAuditUtil.record(eMti, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:691");
			}
			hasil = jatelindo.toString();
			System.out.println("response " + hasil);
			try {
				LogHostToHost logHostToHost = new LogHostToHost();
				logHostToHost.setKeterangan(data);
				try {
					if (request != null && request.getHeader("Cf-Connecting-Ip") != null) {
						logHostToHost.setIp(request.getHeader("Cf-Connecting-Ip"));
					} else if (request != null && request.getHeader("CF-Connecting-IP") != null) {
						logHostToHost.setIp(request.getHeader("CF-Connecting-IP"));
					} else if (request != null && request.getHeader("X-Forwarded-For") != null) {
						logHostToHost.setIp(request.getHeader("X-Forwarded-For"));
					} else if (request != null && request.getHeader("X-Real-IP") != null) {
						logHostToHost.setIp(request.getHeader("X-Real-IP"));
					} else {
						logHostToHost.setIp(
								request != null ? request.getRemoteAddr() : (bankHost != null ? bankHost.getIp() : ""));
					}
				} catch (Exception e) {
					logHostToHost
							.setIp(request != null ? request.getRemoteAddr() : (bankHost != null ? bankHost.getIp() : ""));
				}
				logHostToHost.setBankHost(bankHost);
				logHostToHost.setNim(jatelindoRequest == null ? ""
						: (jatelindoRequest.getMahasiswa() != null ? jatelindoRequest.getMahasiswa().getNim()
								: jatelindoRequest.getBiodataCalonMahasiswa() != null
										? jatelindoRequest.getBiodataCalonMahasiswa().getNoRegistrasi()
										: ""));
				logHostToHost.setKode(trx_id);
				logHostToHost.setNama(jatelindoRequest == null ? ""
						: (jatelindoRequest.getMahasiswa() != null ? jatelindoRequest.getMahasiswa().getNama()
								: jatelindoRequest.getBiodataCalonMahasiswa() != null
										? jatelindoRequest.getBiodataCalonMahasiswa().getNama()
										: ""));
				logHostToHost.setTanggal(ais.ui.util.WaktuUtil.getDate());
				logHostToHost.setResponseDescription(hasil);
				logHostToHost.setNominal(jatelindoRequest == null ? 0.0
						: (jatelindoRequest.getAmount() + jatelindoRequest.getBiayaAdministrasi()));
				logHostToHost.setItem(jatelindo == null ? "" : jatelindo.toString());
				logHostToHost.setStackTrace(h2hStackTrace);
				ais.action.ws.util.PembayaranGatewayHelper.simpanLogHostToHost(logHostToHost);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		return hasil;

	}

}
